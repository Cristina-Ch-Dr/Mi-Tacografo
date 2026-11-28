package com.cristinachdr.mitacografo.feature.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristinachdr.mitacografo.domain.model.ConfigSalario
import com.cristinachdr.mitacografo.domain.model.Gasto
import com.cristinachdr.mitacografo.domain.model.ModoCobro
import com.cristinachdr.mitacografo.domain.model.ResultadoSalario
import com.cristinachdr.mitacografo.domain.usecase.AnadirGasto
import com.cristinachdr.mitacografo.domain.usecase.BorrarGasto
import com.cristinachdr.mitacografo.domain.usecase.CambiarConfigSalario
import com.cristinachdr.mitacografo.domain.usecase.ObtenerSalario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Lo que se escribe en «Cómo cobras»: texto, para dejar escribir «0,» sin que salte nada. */
data class CamposSalario(
    val diaInicio: String = "",
    val nomina: String = "",
    val porKm: String = "",
    val porDia: String = "",
)

/** El gasto que se está apuntando. */
data class GastoNuevo(
    val fecha: LocalDate,
    val descripcion: String = "",
    val importe: String = "",
) {
    val valido: Boolean get() = leerImporte(importe)?.let { it > BigDecimal.ZERO } == true
}

data class SalarioUiState(
    val resultado: ResultadoSalario? = null,
    /** 0 = el periodo de hoy, -1 = el anterior… No se pasa del de hoy. */
    val desplazamiento: Int = 0,
    val campos: CamposSalario = CamposSalario(),
    val gasto: GastoNuevo,
) {
    val puedeAvanzar: Boolean get() = desplazamiento < 0
}

/**
 * Pantalla Salario (MT-58): lo que gana en un periodo, con el desglose, sus viajes y sus gastos, y cómo cobra.
 * Lo que se escribe en «Cómo cobras» se guarda al momento (si tiene sentido).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SalarioViewModel
    @Inject
    constructor(
        obtenerSalario: ObtenerSalario,
        private val cambiarConfig: CambiarConfigSalario,
        private val anadirGasto: AnadirGasto,
        private val borrarGasto: BorrarGasto,
        private val clock: Clock,
    ) : ViewModel() {
        private val desplazamiento = MutableStateFlow(0)
        private val _uiState = MutableStateFlow(SalarioUiState(gasto = GastoNuevo(LocalDate.now(clock))))
        val uiState: StateFlow<SalarioUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                var primeraVez = true
                desplazamiento.flatMapLatest { obtenerSalario(it) }.collect { resultado ->
                    _uiState.update { estado ->
                        val campos = if (primeraVez) camposDe(resultado.config) else estado.campos
                        primeraVez = false
                        estado.copy(resultado = resultado, desplazamiento = desplazamiento.value, campos = campos)
                    }
                }
            }
        }

        fun onAnterior() = desplazamiento.update { it - 1 }

        fun onSiguiente() = desplazamiento.update { if (it < 0) it + 1 else it }

        fun onModo(modo: ModoCobro) = guardar { it.copy(modo = modo) }

        fun onDiaInicio(valor: String) {
            _uiState.update { it.copy(campos = it.campos.copy(diaInicio = valor.take(2))) }
            valor.toIntOrNull()?.takeIf { it in ConfigSalario.DIAS_INICIO }?.let { dia ->
                guardar { it.copy(diaInicio = dia) }
            }
        }

        fun onNomina(valor: String) = importe(valor, { copy(nomina = it) }) { config, v -> config.copy(nomina = v) }

        fun onPorKm(valor: String) = importe(valor, { copy(porKm = it) }) { config, v -> config.copy(porKm = v) }

        fun onPorDia(valor: String) = importe(valor, { copy(porDia = it) }) { config, v -> config.copy(porDia = v) }

        fun onFechaGasto(fecha: LocalDate) = _uiState.update { it.copy(gasto = it.gasto.copy(fecha = fecha)) }

        fun onDescripcionGasto(valor: String) = _uiState.update { it.copy(gasto = it.gasto.copy(descripcion = valor)) }

        fun onImporteGasto(valor: String) =
            _uiState.update { it.copy(gasto = it.gasto.copy(importe = soloImporte(valor))) }

        fun onAnadirGasto() {
            val nuevo = _uiState.value.gasto
            val importe = leerImporte(nuevo.importe) ?: return
            viewModelScope.launch {
                if (anadirGasto(Gasto(fecha = nuevo.fecha, descripcion = nuevo.descripcion, importe = importe))) {
                    _uiState.update { it.copy(gasto = GastoNuevo(nuevo.fecha)) }
                }
            }
        }

        fun onBorrarGasto(id: Long) {
            viewModelScope.launch { borrarGasto(id) }
        }

        /** Un importe de «Cómo cobras»: se ve lo escrito y, si es un número, se guarda. Vacío = 0. */
        private fun importe(
            valor: String,
            campo: CamposSalario.(String) -> CamposSalario,
            cambio: (ConfigSalario, BigDecimal) -> ConfigSalario,
        ) {
            val limpio = soloImporte(valor)
            _uiState.update { it.copy(campos = it.campos.campo(limpio)) }
            val numero = if (limpio.isBlank()) BigDecimal.ZERO else leerImporte(limpio) ?: return
            guardar { cambio(it, numero) }
        }

        private fun guardar(cambio: (ConfigSalario) -> ConfigSalario) {
            viewModelScope.launch { cambiarConfig(cambio) }
        }

        private fun camposDe(config: ConfigSalario) =
            CamposSalario(
                diaInicio = config.diaInicio.toString(),
                nomina = textoImporte(config.nomina),
                porKm = textoImporte(config.porKm),
                porDia = textoImporte(config.porDia),
            )
    }

/** «12,50» o «12.50» → 12.50. `null` si no es un número. */
internal fun leerImporte(texto: String): BigDecimal? =
    texto.trim().replace(',', '.').toBigDecimalOrNull()?.takeIf {
        it.signum() >=
            0
    }

/** Solo cifras y una coma (el teclado puede poner punto o coma). */
internal fun soloImporte(texto: String): String {
    val conComa = texto.replace('.', ',').filter { it.isDigit() || it == ',' }
    val primera = conComa.indexOf(',')
    return if (primera <
        0
    ) {
        conComa
    } else {
        conComa.substring(0, primera + 1) + conComa.substring(primera + 1).replace(",", "")
    }
}

/** 0 → vacío; 0.045 → «0,045»; 1200.00 → «1200». */
internal fun textoImporte(valor: BigDecimal): String =
    if (valor.signum() == 0) "" else valor.stripTrailingZeros().toPlainString().replace('.', ',')
