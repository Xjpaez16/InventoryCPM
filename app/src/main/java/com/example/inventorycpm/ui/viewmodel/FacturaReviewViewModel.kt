package com.example.inventorycpm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.inventorycpm.data.local.AppDatabase
import com.example.inventorycpm.data.repository.CierreDiaRepository
import com.example.inventorycpm.data.repository.FacturaRepository
import com.example.inventorycpm.domain.model.EstadoFactura
import com.example.inventorycpm.domain.model.Factura
import com.example.inventorycpm.domain.model.ItemFactura
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class FacturaReviewUiState(
    val factura: Factura? = null,
    val items: List<ItemFactura> = emptyList(),
    val totalAjustado: BigDecimal = BigDecimal.ZERO,
    val nombreCliente: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isConfirmada: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

class FacturaReviewViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val facturaRepo = FacturaRepository(db.facturaDao(), db.itemFacturaDao())
    private val cierreRepo = CierreDiaRepository(db.cierreDiaDao())

    private val facturaId: Long = checkNotNull(savedStateHandle["facturaId"])

    private val _uiState = MutableStateFlow(FacturaReviewUiState())
    val uiState: StateFlow<FacturaReviewUiState> = _uiState.asStateFlow()

    init {
        loadFactura()
    }

    private fun loadFactura() {
        viewModelScope.launch {
            val factura = facturaRepo.getItemsDeFacturaOnce(facturaId).let { items ->
                val f = facturaRepo.getFacturasDelDiaOnce(
                    java.time.LocalDate.now()
                ).firstOrNull { it.id == facturaId }
                    ?: run {
                        // Buscar en todos los días (historial)
                        null
                    }
                f to items
            }

            // Observar reactivamente
            facturaRepo.getFacturaById(facturaId).collect { f ->
                if (f != null) {
                    val items = facturaRepo.getItemsDeFacturaOnce(f.id)
                    _uiState.update { state ->
                        state.copy(
                            factura = f,
                            items = items,
                            nombreCliente = f.nombreClienteOpcional ?: "",
                            totalAjustado = calcularTotal(items),
                            isConfirmada = f.estado == EstadoFactura.CONFIRMADA,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun setNombreCliente(nombre: String) {
        _uiState.update { it.copy(nombreCliente = nombre) }
    }

    /**
     * Cambia el flag incluido de un ítem y recalcula el total en tiempo real.
     */
    fun toggleIncluido(itemId: Long) {
        _uiState.update { state ->
            val updatedItems = state.items.map { item ->
                if (item.id == itemId) {
                    val toggled = item.copy(incluido = !item.incluido)
                    toggled.copy(valorLineaAjustado = toggled.calcularValorAjustado())
                } else item
            }
            state.copy(
                items = updatedItems,
                totalAjustado = calcularTotal(updatedItems)
            )
        }
    }

    /**
     * Ajusta las unidades entregadas de un ítem y recalcula el total en tiempo real.
     */
    fun setCantidadEntregada(itemId: Long, cantidad: Int) {
        _uiState.update { state ->
            val updatedItems = state.items.map { item ->
                if (item.id == itemId) {
                    val updated = item.copy(
                        cantidadEntregada = cantidad.coerceAtLeast(0)
                    )
                    updated.copy(valorLineaAjustado = updated.calcularValorAjustado())
                } else item
            }
            state.copy(
                items = updatedItems,
                totalAjustado = calcularTotal(updatedItems)
            )
        }
    }

    fun setNombreProducto(itemId: Long, nombre: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) item.copy(nombreProducto = nombre) else item
                }
            )
        }
    }

    fun setPrecioUnitario(itemId: Long, precio: BigDecimal) {
        _uiState.update { state ->
            val updatedItems = state.items.map { item ->
                if (item.id == itemId) {
                    val updated = item.copy(precioUnitario = precio)
                    updated.copy(valorLineaAjustado = updated.calcularValorAjustado())
                } else item
            }
            state.copy(
                items = updatedItems,
                totalAjustado = calcularTotal(updatedItems)
            )
        }
    }

    /**
     * Añade un ítem manual vacío para que el usuario lo complete.
     */
    fun addItemManual() {
        _uiState.update { state ->
            val newItem = ItemFactura(
                id = -(System.currentTimeMillis()), // id temporal negativo hasta guardar
                facturaId = facturaId,
                nombreProducto = "",
                precioUnitario = BigDecimal.ZERO,
                cantidadOriginal = 1,
                cantidadEntregada = 1,
                incluido = true,
                valorLineaAjustado = BigDecimal.ZERO
            )
            state.copy(items = state.items + newItem)
        }
    }

    fun removeItem(itemId: Long) {
        _uiState.update { state ->
            val updatedItems = state.items.filter { it.id != itemId }
            state.copy(
                items = updatedItems,
                totalAjustado = calcularTotal(updatedItems)
            )
        }
    }

    /**
     * Confirma la factura: guarda los valores ajustados finales en Room y marca como CONFIRMADA.
     * Requiere confirmación explícita del usuario (el botón en UI).
     */
    fun confirmarFactura() {
        val state = _uiState.value
        val factura = state.factura ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val itemsFinales = state.items
                .filter { it.nombreProducto.isNotBlank() } // Descartar ítems vacíos
                .map { item ->
                    // Asegurar que valorLineaAjustado está actualizado
                    item.copy(valorLineaAjustado = item.calcularValorAjustado())
                }

            val totalOriginal = itemsFinales.fold(BigDecimal.ZERO) { acc, item ->
                acc.add(item.precioUnitario.multiply(BigDecimal(item.cantidadOriginal)))
            }

            val facturaActualizada = factura.copy(
                nombreClienteOpcional = state.nombreCliente.takeIf { it.isNotBlank() },
                valorTotalOriginal = totalOriginal,
                valorTotalAjustado = state.totalAjustado,
                estado = EstadoFactura.CONFIRMADA
            )

            try {
                facturaRepo.updateFacturaConItems(facturaActualizada, itemsFinales)
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Error al guardar: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun calcularTotal(items: List<ItemFactura>): BigDecimal =
        items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.calcularValorAjustado()) }
}
