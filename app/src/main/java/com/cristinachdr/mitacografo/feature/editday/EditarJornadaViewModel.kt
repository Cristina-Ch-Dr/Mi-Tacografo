package com.cristinachdr.mitacografo.feature.editday

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.PreferenciasPaises
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.cruceDeSemana
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.usecase.AnadirJornada
import com.cristinachdr.mitacografo.domain.usecase.BorrarJornada
import com.cristinachdr.mitacografo.domain.usecase.EditarJornada
import com.cristinachdr.mitacografo.domain.usecase.ObtenerPreferenciasPaises
import com.cristinachdr.mitacografo.feature.startday.regionPara
import com.cristinachdr.mitacografo.ui.ErrorCampo
import com.cristinachdr.mitacografo.ui.casillasConduccion
import com.cristinachdr.mitacografo.ui.errorMinutos
import com.cristinachdr.mitacografo.ui.leerConduccion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class EditarJornadaUiState(
    /** La jornada tal y como está guardada (o la propuesta, si es nueva). `null` mientras carga o si ya no existe. */
    val original: Jornada? = null,
    /** Una jornada pasada que se añade a mano (MT-47): siempre terminada y no se puede borrar. */
    val nueva: Boolean = false,
    val inicio: LocalDateTime? = null,
    val kmInicio: String = "",
    val paisInicio: String = "",
    /** Comunidad autónoma, si el país es España (MT-55). */
    val regionInicio: String = "",
    val ciudadInicio: String = "",
    /** Solo si la jornada está terminada: una jornada en curso se termina desde Inicio. */
    val fin: LocalDateTime? = null,
    val kmFin: String = "",
    val paisFin: String = "",
    val regionFin: String = "",
    val ciudadFin: String = "",
    val conduccionHoras: String = "",
    val conduccionMinutos: String = "",
    /** La zona de Ajustes: en ella se mira la medianoche del domingo, como el motor de reglas. */
    val zonaReferencia: ZoneId? = null,
    /** El lunes a las 00:00 que cruza la jornada (MT-50); `null` si no cruza ninguno. */
    val medianoche: ZonedDateTime? = null,
    val antesHoras: String = "",
    val antesMinutos: String = "",
    val errorMinutosAntes: ErrorCampo? = null,
    val observaciones: String = "",
    val errorKmInicio: ErrorCampo? = null,
    val errorKmFin: ErrorCampo? = null,
    val errorMinutos: ErrorCampo? = null,
    val errores: Set<ErrorJornada> = emptySet(),
    val kmRecorridos: Int? = null,
    val duracion: Duration? = null,
    val paises: PreferenciasPaises = PreferenciasPaises(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    /** Se está mostrando «¿Borrar esta jornada?». */
    val confirmandoBorrado: Boolean = false,
    /** Cuando pasa a `true` (guardada o borrada), la pantalla se cierra. */
    val terminado: Boolean = false,
) {
    val noExiste: Boolean get() = !cargando && original == null
    val terminada: Boolean get() = original?.abierta == false

    val puedeGuardar: Boolean
        get() =
            original != null &&
                !guardando &&
                errorKmInicio == null &&
                errorKmFin == null &&
                errorMinutos == null &&
                errorMinutosAntes == null &&
                errores.isEmpty()
}

/**
 * Detalle de una jornada del historial: se puede corregir cualquier dato o borrarla.
 * Con id 0, añadir a mano una jornada pasada (MT-47), con los mismos campos.
 *
 * Las horas se editan en la zona donde ocurrieron (la de inicio y la de fin), como las vio el conductor.
 */
@HiltViewModel
class EditarJornadaViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: JornadaRepository,
        private val editarJornada: EditarJornada,
        private val anadirJornada: AnadirJornada,
        private val borrarJornada: BorrarJornada,
        private val obtenerPreferenciasPaises: ObtenerPreferenciasPaises,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) : ViewModel() {
        /** El id viene de la ruta `RutaJornada(id)`; 0 = nueva. */
        private val id: Long = savedStateHandle[CLAVE_ID] ?: Jornada.SIN_ID

        private val _uiState = MutableStateFlow(EditarJornadaUiState())
        val uiState: StateFlow<EditarJornadaUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val jornada = if (id == Jornada.SIN_ID) propuestaNueva() else repository.obtenerPorId(id)
                val paises = obtenerPreferenciasPaises()
                val zonaReferencia = settings.observar().first().zonaReferencia
                _uiState.update {
                    if (jornada == null) {
                        it.copy(cargando = false)
                    } else {
                        desde(jornada)
                            .copy(paises = paises, nueva = id == Jornada.SIN_ID, zonaReferencia = zonaReferencia)
                            .validado()
                    }
                }
            }
        }

        fun onInicio(valor: LocalDateTime) = actualizar { copy(inicio = valor) }

        fun onKmInicio(valor: String) = actualizar { copy(kmInicio = valor) }

        fun onPaisInicio(valor: String) =
            actualizar {
                copy(paisInicio = valor, regionInicio = regionPara(valor, regionInicio))
            }

        fun onRegionInicio(valor: String) = actualizar { copy(regionInicio = valor) }

        fun onCiudadInicio(valor: String) = actualizar { copy(ciudadInicio = valor) }

        fun onFin(valor: LocalDateTime) = actualizar { copy(fin = valor) }

        fun onKmFin(valor: String) = actualizar { copy(kmFin = valor) }

        fun onPaisFin(valor: String) = actualizar { copy(paisFin = valor, regionFin = regionPara(valor, regionFin)) }

        fun onRegionFin(valor: String) = actualizar { copy(regionFin = valor) }

        fun onCiudadFin(valor: String) = actualizar { copy(ciudadFin = valor) }

        fun onConduccionHoras(valor: String) = actualizar { copy(conduccionHoras = valor.take(2)) }

        fun onConduccionMinutos(valor: String) = actualizar { copy(conduccionMinutos = valor.take(2)) }

        fun onAntesHoras(valor: String) = actualizar { copy(antesHoras = valor.take(2)) }

        fun onAntesMinutos(valor: String) = actualizar { copy(antesMinutos = valor.take(2)) }

        fun onObservaciones(valor: String) = actualizar { copy(observaciones = valor) }

        fun onGuardar() {
            val estado = _uiState.value
            val jornada = estado.borrador()
            if (!estado.puedeGuardar || jornada == null) return

            _uiState.update { it.copy(guardando = true) }
            viewModelScope.launch {
                val resultado = if (estado.nueva) anadirJornada(jornada) else editarJornada(jornada)
                when (resultado) {
                    is ResultadoJornada.Ok -> {
                        _uiState.update { it.copy(terminado = true) }
                    }

                    is ResultadoJornada.Invalida -> {
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
                when (val resultado = borrarJornada(id)) {
                    is ResultadoJornada.Ok -> {
                        _uiState.update { it.copy(terminado = true) }
                    }

                    is ResultadoJornada.Invalida -> {
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

        /**
         * Lo que se propone al añadir una jornada: de 06:00 a 16:00 (en la zona del móvil) del último día antes
         * de hoy sin ninguna jornada. Así, al ir pasando la libreta hacia atrás, propone cada vez el día que falta.
         * Los km se dejan en blanco para que se copien del tacógrafo.
         */
        private suspend fun propuestaNueva(): Jornada {
            val zona = clock.zone
            val jornadas = repository.observarTodas().first()
            val ahora = clock.instant()
            val ayer = LocalDate.now(clock).minusDays(1)
            val dia =
                generateSequence(ayer) { it.minusDays(1) }
                    .take(DIAS_BUSCANDO_HUECO)
                    .firstOrNull { dia ->
                        val desde = dia.atStartOfDay(zona).toInstant()
                        val hasta = dia.plusDays(1).atStartOfDay(zona).toInstant()
                        jornadas.none { it.inicio < hasta && (it.fin ?: ahora) > desde }
                    } ?: ayer
            return Jornada(
                inicio = dia.atTime(HORA_INICIO_PROPUESTA, 0).atZone(zona).toInstant(),
                zonaInicio = zona,
                kmInicio = 0,
                fin = dia.atTime(HORA_FIN_PROPUESTA, 0).atZone(zona).toInstant(),
                zonaFin = zona,
            )
        }

        private fun desde(jornada: Jornada): EditarJornadaUiState {
            val (horas, minutos) = casillasConduccion(jornada.conduccionTacografo)
            val (antesHoras, antesMinutos) = casillasConduccion(jornada.conduccionAntesMedianoche)
            return EditarJornadaUiState(
                original = jornada,
                inicio = jornada.inicio.atZone(jornada.zonaInicio).toLocalDateTime(),
                kmInicio = if (jornada.id == Jornada.SIN_ID) "" else jornada.kmInicio.toString(),
                paisInicio = jornada.paisInicio.orEmpty(),
                regionInicio = jornada.regionInicio.orEmpty(),
                ciudadInicio = jornada.ciudadInicio.orEmpty(),
                fin = jornada.fin?.atZone(jornada.zonaFin ?: jornada.zonaInicio)?.toLocalDateTime(),
                kmFin = jornada.kmFin?.toString().orEmpty(),
                paisFin = jornada.paisFin.orEmpty(),
                regionFin = jornada.regionFin.orEmpty(),
                ciudadFin = jornada.ciudadFin.orEmpty(),
                conduccionHoras = horas,
                conduccionMinutos = minutos,
                antesHoras = antesHoras,
                antesMinutos = antesMinutos,
                observaciones = jornada.observaciones.orEmpty(),
                cargando = false,
            )
        }

        private fun actualizar(cambio: EditarJornadaUiState.() -> EditarJornadaUiState) {
            _uiState.update { it.cambio().validado() }
        }

        /** Errores de cada casilla, reglas del dominio y km y duración en vivo. */
        private fun EditarJornadaUiState.validado(): EditarJornadaUiState {
            val errorKmInicio = errorKm(kmInicio)
            val errorKmFin = if (terminada) errorKm(kmFin) else null
            val errorMinutos = errorMinutos(conduccionMinutos)
            // Primero se mira si cruza el domingo: de eso depende que cuente la conducción antes de medianoche.
            val medianoche =
                borrador()?.cruceDeSemana(zonaReferencia)?.atZone(zonaReferencia ?: original?.zonaInicio)
            val conMedianoche = copy(medianoche = medianoche)
            val errorMinutosAntes = errorMinutos(antesMinutos).takeIf { medianoche != null }
            val borrador =
                conMedianoche.borrador().takeIf {
                    errorKmInicio == null && errorKmFin == null && errorMinutos == null && errorMinutosAntes == null
                }
            return conMedianoche.copy(
                errorKmInicio = errorKmInicio,
                errorKmFin = errorKmFin,
                errorMinutos = errorMinutos,
                errorMinutosAntes = errorMinutosAntes,
                errores =
                    borrador
                        ?.let {
                            validarJornada(
                                it,
                                clock.instant(),
                                zonaReferencia = zonaReferencia,
                            )
                        }.orEmpty(),
                kmRecorridos = borrador?.kmRecorridos,
                duracion = borrador?.duracion?.takeUnless { it.isNegative },
            )
        }

        private companion object {
            const val CLAVE_ID = "id"
            const val HORA_INICIO_PROPUESTA = 6
            const val HORA_FIN_PROPUESTA = 16
            const val DIAS_BUSCANDO_HUECO = 366
        }
    }

private fun errorKm(km: String): ErrorCampo? =
    when {
        km.isBlank() -> ErrorCampo.OBLIGATORIO
        km.toIntOrNull() == null -> ErrorCampo.NO_ES_NUMERO
        else -> null
    }

/** La jornada con lo que hay escrito, o `null` si falta algo para poder construirla. */
internal fun EditarJornadaUiState.borrador(): Jornada? {
    val original = original ?: return null
    val inicio = inicio ?: return null
    val kmInicio = kmInicio.toIntOrNull() ?: return null
    val base =
        original.copy(
            inicio = inicio.atZone(original.zonaInicio).toInstant(),
            kmInicio = kmInicio,
            paisInicio = paisInicio,
            regionInicio = regionInicio,
            ciudadInicio = ciudadInicio,
            observaciones = observaciones,
        )
    if (original.abierta) return base
    val zonaFin = original.zonaFin ?: original.zonaInicio
    return base.copy(
        fin = fin?.atZone(zonaFin)?.toInstant(),
        kmFin = kmFin.toIntOrNull() ?: return null,
        paisFin = paisFin,
        regionFin = regionFin,
        ciudadFin = ciudadFin,
        conduccionTacografo = leerConduccion(conduccionHoras, conduccionMinutos),
        conduccionAntesMedianoche = if (medianoche == null) null else leerConduccion(antesHoras, antesMinutos),
    )
}
