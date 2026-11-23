package com.cristinachdr.mitacografo.feature.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ErrorViaje
import com.cristinachdr.mitacografo.domain.model.ResultadoViaje
import com.cristinachdr.mitacografo.domain.model.ResumenViaje
import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.domain.model.solapaCon
import com.cristinachdr.mitacografo.domain.model.validarViaje
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import com.cristinachdr.mitacografo.domain.usecase.AnadirViaje
import com.cristinachdr.mitacografo.domain.usecase.BorrarViaje
import com.cristinachdr.mitacografo.domain.usecase.GuardarViaje
import com.cristinachdr.mitacografo.domain.usecase.ObtenerViajes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class EditarViajeUiState(
    /** El viaje tal y como está guardado (o el propuesto, si es nuevo). `null` mientras carga o si ya no existe. */
    val original: Viaje? = null,
    /** El viaje con sus jornadas, para el detalle. `null` en uno nuevo. */
    val resumen: ResumenViaje? = null,
    /** Un viaje pasado que se añade a mano (MT-52): siempre terminado, sin jornadas a la vista y sin «Borrar». */
    val nuevo: Boolean = false,
    val nombre: String = "",
    /** `true` cuando el conductor escribe el nombre: desde entonces ya no se propone solo. */
    val nombreTocado: Boolean = false,
    val inicio: LocalDateTime? = null,
    val fin: LocalDateTime? = null,
    val observaciones: String = "",
    val errores: Set<ErrorViaje> = emptySet(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val confirmandoBorrado: Boolean = false,
    /** Cuando pasa a `true` (guardado o borrado), la pantalla se cierra. */
    val cerrar: Boolean = false,
) {
    val noExiste: Boolean get() = !cargando && original == null
    val puedeGuardar: Boolean get() = original != null && !guardando && errores.isEmpty()
}

/**
 * Detalle de un viaje: sus jornadas y km, y corregir el nombre, las fechas o borrarlo.
 * Con id 0, añadir a mano un viaje pasado (MT-52), con los mismos campos.
 */
@HiltViewModel
class EditarViajeViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        obtenerViajes: ObtenerViajes,
        private val repository: ViajeRepository,
        private val guardarViaje: GuardarViaje,
        private val anadirViaje: AnadirViaje,
        private val borrarViaje: BorrarViaje,
        private val clock: Clock,
    ) : ViewModel() {
        /** El id viene de la ruta `RutaViaje(id)`; 0 = nuevo. */
        private val id: Long = savedStateHandle[CLAVE_ID] ?: Viaje.SIN_ID

        private val _uiState = MutableStateFlow(EditarViajeUiState())
        val uiState: StateFlow<EditarViajeUiState> = _uiState.asStateFlow()

        private var otros: List<Viaje> = emptyList()

        /** Cómo se llama un viaje nuevo según su fecha de inicio («Viaje del 2 nov 2026»); lo pone la pantalla. */
        private var nombrePropuesto: ((LocalDate) -> String)? = null

        init {
            viewModelScope.launch {
                otros = repository.obtenerTodos().filter { it.id != id }
                if (id == Viaje.SIN_ID) {
                    // La pantalla puede haber dado ya el nombre propuesto mientras cargaba: se aplica aquí.
                    _uiState.update {
                        it
                            .desde(propuestaNueva())
                            .copy(nuevo = true, cargando = false)
                            .conNombrePropuesto()
                            .validado()
                    }
                    return@launch
                }
                var primeraVez = true
                // Las jornadas del viaje se siguen actualizando (si se corrige una desde aquí, cambian los km).
                obtenerViajes.uno(id).collect { resumen ->
                    _uiState.update { estado ->
                        val base = estado.copy(resumen = resumen, original = resumen?.viaje, cargando = false)
                        if (primeraVez && resumen != null) {
                            primeraVez = false
                            base.desde(resumen.viaje).validado()
                        } else {
                            base
                        }
                    }
                }
            }
        }

        /** La pantalla dice cómo proponer el nombre (con los textos y el formato de fecha del idioma de la app). */
        fun usarNombrePropuesto(propuesta: (LocalDate) -> String) {
            nombrePropuesto = propuesta
            actualizar { conNombrePropuesto() }
        }

        fun onNombre(valor: String) = actualizar { copy(nombre = valor, nombreTocado = true) }

        fun onInicio(valor: LocalDateTime) = actualizar { copy(inicio = valor).conNombrePropuesto() }

        fun onFin(valor: LocalDateTime) = actualizar { copy(fin = valor) }

        fun onObservaciones(valor: String) = actualizar { copy(observaciones = valor) }

        fun onGuardar() {
            val estado = _uiState.value
            val viaje = estado.borrador() ?: return
            if (!estado.puedeGuardar) return
            _uiState.update { it.copy(guardando = true) }
            viewModelScope.launch {
                val resultado = if (estado.nuevo) anadirViaje(viaje) else guardarViaje(viaje)
                when (resultado) {
                    is ResultadoViaje.Ok -> {
                        _uiState.update { it.copy(cerrar = true) }
                    }

                    is ResultadoViaje.Invalido -> {
                        _uiState.update {
                            it.copy(
                                guardando = false,
                                errores = resultado.errores,
                            )
                        }
                    }
                }
            }
        }

        fun onPedirBorrar() = _uiState.update { it.copy(confirmandoBorrado = true) }

        fun onCancelarBorrar() = _uiState.update { it.copy(confirmandoBorrado = false) }

        fun onConfirmarBorrar() {
            _uiState.update { it.copy(confirmandoBorrado = false, guardando = true) }
            viewModelScope.launch {
                borrarViaje(id)
                _uiState.update { it.copy(cerrar = true) }
            }
        }

        /**
         * Lo que se propone al añadir un viaje pasado: de lunes a las 06:00 a viernes a las 18:00 (en la zona
         * del móvil) de la semana anterior al viaje más antiguo, o a hoy si no hay ninguno. Así, al ir pasando
         * los viajes hacia atrás, no pisa a ninguno. Las fechas se cambian; el nombre sigue a la de inicio.
         */
        private fun propuestaNueva(): Viaje {
            val zona = clock.zone
            val primero = otros.minByOrNull { it.inicio }?.inicio
            val ancla = primero?.atZone(zona)?.toLocalDate() ?: LocalDate.now(clock)
            val lunes = ancla.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val viernes =
                lunes
                    .plusDays(DIAS_HASTA_VIERNES)
                    .atTime(HORA_VUELTA, 0)
                    .atZone(zona)
                    .toInstant()
            return Viaje(
                nombre = "",
                inicio = lunes.atTime(HORA_SALIDA, 0).atZone(zona).toInstant(),
                zonaInicio = zona,
                fin = if (primero != null) minOf(viernes, primero) else viernes,
                zonaFin = zona,
            )
        }

        private fun EditarViajeUiState.desde(viaje: Viaje): EditarViajeUiState =
            copy(
                original = viaje,
                nombre = viaje.nombre,
                inicio = viaje.inicio.atZone(viaje.zonaInicio).toLocalDateTime(),
                fin = viaje.fin?.atZone(viaje.zonaFin ?: viaje.zonaInicio)?.toLocalDateTime(),
                observaciones = viaje.observaciones.orEmpty(),
            )

        /** En un viaje nuevo, mientras no se haya escrito el nombre, se llama como su fecha de inicio. */
        private fun EditarViajeUiState.conNombrePropuesto(): EditarViajeUiState {
            val propuesta = nombrePropuesto
            val dia = inicio?.toLocalDate()
            if (!nuevo || nombreTocado || propuesta == null || dia == null) return this
            return copy(nombre = propuesta(dia))
        }

        private fun actualizar(cambio: EditarViajeUiState.() -> EditarViajeUiState) =
            _uiState.update { it.cambio().validado() }

        private fun EditarViajeUiState.validado(): EditarViajeUiState {
            val viaje = borrador() ?: return copy(errores = emptySet())
            val ahora = clock.instant()
            val errores = validarViaje(viaje, otros, ahora).toMutableSet()
            if (nuevo && otros.any { it.solapaCon(viaje, ahora) }) errores += ErrorViaje.SOLAPA_CON_OTRO_VIAJE
            return copy(errores = errores)
        }

        private companion object {
            const val CLAVE_ID = "id"
            const val HORA_SALIDA = 6
            const val HORA_VUELTA = 18
            const val DIAS_HASTA_VIERNES = 4L
        }
    }

/** El viaje con lo que hay escrito, o `null` si aún no ha cargado. */
internal fun EditarViajeUiState.borrador(): Viaje? {
    val original = original ?: return null
    val inicio = inicio ?: return null
    return original.copy(
        nombre = nombre,
        inicio = inicio.atZone(original.zonaInicio).toInstant(),
        fin = fin?.atZone(original.zonaFin ?: original.zonaInicio)?.toInstant(),
        observaciones = observaciones,
    )
}
