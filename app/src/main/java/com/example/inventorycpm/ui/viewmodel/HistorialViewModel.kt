package com.example.inventorycpm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventorycpm.data.local.AppDatabase
import com.example.inventorycpm.data.repository.CierreDiaRepository
import com.example.inventorycpm.data.repository.FacturaRepository
import com.example.inventorycpm.domain.model.CierreDia
import com.example.inventorycpm.domain.model.Factura
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HistorialUiState(
    val cierres: List<CierreDia> = emptyList(),
    val diasConFacturas: List<LocalDate> = emptyList(),
    val availableMonths: List<YearMonth> = emptyList(),
    val selectedMonth: YearMonth? = null,
    val isLoading: Boolean = true
)

data class HistorialDiaUiState(
    val fecha: LocalDate = LocalDate.now(),
    val facturas: List<Factura> = emptyList(),
    val isCerrado: Boolean = false,
    val isLoading: Boolean = true
)

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val facturaRepo = FacturaRepository(db.facturaDao(), db.itemFacturaDao())
    private val cierreRepo = CierreDiaRepository(db.cierreDiaDao())

    private val _selectedMonth = MutableStateFlow<YearMonth?>(null)

    val historialState: StateFlow<HistorialUiState> =
        kotlinx.coroutines.flow.combine(
            cierreRepo.getAllCierres(),
            facturaRepo.getDiasConFacturas(),
            _selectedMonth
        ) { cierres, dias, month ->
            val allMonths = (cierres.map { YearMonth.from(it.fecha) } + dias.map { YearMonth.from(it) })
                .distinct()
                .sortedDescending()

            val filteredCierres = if (month != null) cierres.filter { YearMonth.from(it.fecha) == month } else cierres
            val filteredDias = if (month != null) dias.filter { YearMonth.from(it) == month } else dias

            HistorialUiState(
                cierres = filteredCierres,
                diasConFacturas = filteredDias,
                availableMonths = allMonths,
                selectedMonth = month,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistorialUiState()
        )

    private val _diaState = MutableStateFlow(HistorialDiaUiState())
    val diaState: StateFlow<HistorialDiaUiState> = _diaState.asStateFlow()

    fun setMonthFilter(month: YearMonth?) {
        _selectedMonth.value = month
    }

    fun loadFacturasDelDia(fecha: LocalDate) {
        viewModelScope.launch {
            val cierre = cierreRepo.getCierreDiaOnce(fecha)
            facturaRepo.getFacturasDelDia(fecha).collect { facturas ->
                _diaState.update { state ->
                    state.copy(
                        fecha = fecha,
                        facturas = facturas,
                        isCerrado = cierre != null,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteFactura(facturaId: Long) {
        viewModelScope.launch {
            facturaRepo.deleteFacturaConItems(facturaId)
        }
    }
}
