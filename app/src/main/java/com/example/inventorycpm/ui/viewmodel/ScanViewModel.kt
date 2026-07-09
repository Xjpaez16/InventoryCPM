package com.example.inventorycpm.ui.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventorycpm.data.local.AppDatabase
import com.example.inventorycpm.data.remote.GeminiService
import com.example.inventorycpm.data.remote.model.ParsedInvoiceItemDto
import com.example.inventorycpm.data.repository.FacturaRepository
import com.example.inventorycpm.domain.model.EstadoFactura
import com.example.inventorycpm.domain.model.Factura
import com.example.inventorycpm.domain.model.ItemFactura
import com.example.inventorycpm.util.ImageUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    /** Imagen seleccionada pero aún no procesada. */
    data class ImageSelected(val imagePath: String) : ScanUiState()
    /** IA procesó la imagen; muestra ítems en pantalla de revisión. */
    data class Success(val facturaId: Long) : ScanUiState()
    data class Error(val message: String, val imagePath: String? = null) : ScanUiState()
    /** Sin conexión a internet. Se permite ingreso manual. */
    data class NoNetwork(val imagePath: String?) : ScanUiState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val facturaRepo = FacturaRepository(db.facturaDao(), db.itemFacturaDao())
    private val geminiService = GeminiService()

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var selectedImagePath: String? = null

    /**
     * Guarda la imagen seleccionada (desde galería o CameraX) en almacenamiento interno.
     * @param sourceUri URI de la imagen (content:// o file://)
     */
    fun onImageSelected(sourceUri: android.net.Uri) {
        viewModelScope.launch {
            val fileName = "factura_${UUID.randomUUID()}.jpg"
            val file = ImageUtil.copyUriToFile(getApplication(), sourceUri, fileName)
            if (file != null) {
                selectedImagePath = file.absolutePath
                _uiState.value = ScanUiState.ImageSelected(file.absolutePath)
            } else {
                _uiState.value = ScanUiState.Error("No se pudo guardar la imagen seleccionada.")
            }
        }
    }

    /**
     * Procesa la imagen actual con Gemini.
     * Si no hay red, pasa a estado NoNetwork para ingreso manual.
     */
    fun processImageWithAI() {
        val imagePath = selectedImagePath
        if (imagePath == null) {
            _uiState.value = ScanUiState.Error("Primero selecciona una imagen.")
            return
        }

        if (!hasNetworkConnection()) {
            _uiState.value = ScanUiState.NoNetwork(imagePath)
            return
        }

        viewModelScope.launch {
            _uiState.value = ScanUiState.Loading

            val imageFile = File(imagePath)
            val result = geminiService.scanInvoice(imageFile)

            result.fold(
                onSuccess = { items ->
                    val facturaId = createFacturaWithItems(imagePath, items)
                    _uiState.value = ScanUiState.Success(facturaId)
                },
                onFailure = { error ->
                    _uiState.value = ScanUiState.Error(
                        error.message ?: "Error al procesar la imagen con IA.",
                        imagePath
                    )
                }
            )
        }
    }

    /**
     * Crea una factura vacía (para ingreso manual cuando no hay red).
     */
    fun createManualFactura() {
        viewModelScope.launch {
            val imagePath = selectedImagePath
            val facturaId = createFacturaWithItems(imagePath, emptyList())
            _uiState.value = ScanUiState.Success(facturaId)
        }
    }

    /**
     * Guarda la factura y sus ítems en Room. Retorna el id de la factura.
     */
    private suspend fun createFacturaWithItems(
        imagePath: String?,
        parsedItems: List<ParsedInvoiceItemDto>
    ): Long {
        val totalOriginal = parsedItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.valorTotalLinea ?: BigDecimal.ZERO)
        }

        val factura = Factura(
            fecha = LocalDate.now(),
            valorTotalOriginal = totalOriginal,
            valorTotalAjustado = totalOriginal,
            estado = EstadoFactura.PENDIENTE,
            imagenPath = imagePath
        )

        val items = parsedItems.mapIndexed { _, dto ->
            val precioUnit = dto.precioUnitario ?: BigDecimal.ZERO
            val cantidad = dto.cantidad ?: 1
            ItemFactura(
                facturaId = 0L, // será sobreescrito por el repositorio
                nombreProducto = dto.nombreProducto ?: "",
                precioUnitario = precioUnit,
                cantidadOriginal = cantidad,
                cantidadEntregada = cantidad,
                incluido = true,
                valorLineaAjustado = precioUnit.multiply(BigDecimal(cantidad))
            )
        }

        return facturaRepo.insertFacturaConItems(factura, items)
    }

    fun resetState() {
        selectedImagePath = null
        _uiState.value = ScanUiState.Idle
    }

    private fun hasNetworkConnection(): Boolean {
        val cm = getApplication<Application>()
            .getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
