package com.example.inventorycpm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventorycpm.data.local.AppDatabase
import com.example.inventorycpm.data.repository.CierreDiaRepository
import com.example.inventorycpm.data.repository.FacturaRepository
import com.example.inventorycpm.domain.model.Factura
import com.example.inventorycpm.domain.model.EstadoFactura
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

data class HomeUiState(
    val facturas: List<Factura> = emptyList(),
    val totalAjustadoDelDia: BigDecimal = BigDecimal.ZERO,
    val hayFacturasConfirmadas: Boolean = false,
    val cierreDiaHecho: Boolean = false,
    val isLoading: Boolean = false,
    val fecha: LocalDate = LocalDate.now()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val facturaRepo = FacturaRepository(db.facturaDao(), db.itemFacturaDao())
    private val cierreRepo = CierreDiaRepository(db.cierreDiaDao())

    private val hoy = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = combine(
        facturaRepo.getFacturasDelDia(hoy),
        cierreRepo.getCierreDia(hoy)
    ) { facturas, cierre ->
        val total = facturas
            .filter { it.estado == EstadoFactura.CONFIRMADA }
            .fold(BigDecimal.ZERO) { acc, f -> acc.add(f.valorTotalAjustado) }

        HomeUiState(
            facturas = facturas,
            totalAjustadoDelDia = total,
            hayFacturasConfirmadas = facturas.any { it.estado == EstadoFactura.CONFIRMADA },
            cierreDiaHecho = cierre != null,
            fecha = hoy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
