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

data class HistorialUiState(
    val cierres: List<CierreDia> = emptyList(),
    val diasConFacturas: List<LocalDate> = emptyList(),
    val isLoading: Boolean = true
)

data class HistorialDiaUiState(
    val fecha: LocalDate = LocalDate.now(),
    val facturas: List<Factura> = emptyList(),
    val isLoading: Boolean = true
)

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val facturaRepo = FacturaRepository(db.facturaDao(), db.itemFacturaDao())
    private val cierreRepo = CierreDiaRepository(db.cierreDiaDao())

    val historialState: StateFlow<HistorialUiState> =
        kotlinx.coroutines.flow.combine(
            cierreRepo.getAllCierres(),
            facturaRepo.getDiasConFacturas()
        ) { cierres, dias ->
            HistorialUiState(
                cierres = cierres,
                diasConFacturas = dias,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistorialUiState()
        )

    private val _diaState = MutableStateFlow(HistorialDiaUiState())
    val diaState: StateFlow<HistorialDiaUiState> = _diaState.asStateFlow()

    fun loadFacturasDelDia(fecha: LocalDate) {
        viewModelScope.launch {
            facturaRepo.getFacturasDelDia(fecha).collect { facturas ->
                _diaState.update { state ->
                    state.copy(
                        fecha = fecha,
                        facturas = facturas,
                        isLoading = false
                    )
                }
            }
        }
    }
}
