package com.example.inventorycpm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.inventorycpm.data.local.AppDatabase
import com.example.inventorycpm.data.repository.CierreDiaRepository
import com.example.inventorycpm.data.repository.FacturaRepository
import com.example.inventorycpm.domain.model.CierreDia
import com.example.inventorycpm.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

data class CierreDiaUiState(
    val fecha: LocalDate = LocalDate.now(),
    val totalEsperado: BigDecimal = BigDecimal.ZERO,
    val efectivoInput: String = "",
    val efectivoContado: BigDecimal = BigDecimal.ZERO,
    val diferencia: BigDecimal = BigDecimal.ZERO,
    val notas: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val cierreExistente: Boolean = false,
    val error: String? = null
)

class CierreDiaViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val facturaRepo = FacturaRepository(db.facturaDao(), db.itemFacturaDao())
    private val cierreRepo = CierreDiaRepository(db.cierreDiaDao())

    // Fecha puede venir por parámetro de navegación (para re-ver/re-hacer cierre)
    private val fechaStr: String? = savedStateHandle["fecha"]
    private val fecha: LocalDate = fechaStr?.let { LocalDate.parse(it) } ?: LocalDate.now()

    private val _uiState = MutableStateFlow(CierreDiaUiState(fecha = fecha))
    val uiState: StateFlow<CierreDiaUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val total = facturaRepo.getTotalAjustadoConfirmadasDelDia(fecha)
            val cierreExistente = cierreRepo.getCierreDiaOnce(fecha)

            _uiState.update { state ->
                state.copy(
                    totalEsperado = total,
                    isLoading = false,
                    cierreExistente = cierreExistente != null,
                    efectivoInput = cierreExistente?.efectivoContado?.toPlainString() ?: "",
                    efectivoContado = cierreExistente?.efectivoContado ?: BigDecimal.ZERO,
                    diferencia = cierreExistente?.diferencia ?: BigDecimal.ZERO,
                    notas = cierreExistente?.notas ?: ""
                )
            }
        }
    }

    fun setEfectivoInput(input: String) {
        val parsed = CurrencyFormatter.parse(input) ?: BigDecimal.ZERO
        _uiState.update { state ->
            val diferencia = parsed.subtract(state.totalEsperado)
            state.copy(
                efectivoInput = input,
                efectivoContado = parsed,
                diferencia = diferencia
            )
        }
    }

    fun setNotas(notas: String) {
        _uiState.update { it.copy(notas = notas) }
    }

    fun confirmarCierre() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val cierre = CierreDia(
                    fecha = fecha,
                    totalFacturasConfirmadas = state.totalEsperado,
                    efectivoContado = state.efectivoContado,
                    diferencia = state.diferencia,
                    notas = state.notas.takeIf { it.isNotBlank() }
                )
                cierreRepo.insertOrUpdateCierre(cierre)
                _uiState.update { it.copy(isSaving = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Error al guardar: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
