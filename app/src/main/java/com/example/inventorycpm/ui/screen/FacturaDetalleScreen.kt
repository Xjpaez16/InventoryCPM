package com.example.inventorycpm.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.inventorycpm.data.local.AppDatabase
import com.example.inventorycpm.data.repository.FacturaRepository
import com.example.inventorycpm.domain.model.Factura
import com.example.inventorycpm.domain.model.ItemFactura
import com.example.inventorycpm.ui.component.ItemFacturaCard
import com.example.inventorycpm.ui.theme.*
import com.example.inventorycpm.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.format.DateTimeFormatter

// ─── ViewModel local ──────────────────────────────────────────────────────────

data class FacturaDetalleUiState(
    val factura: Factura? = null,
    val items: List<ItemFactura> = emptyList(),
    val isLoading: Boolean = true
)

class FacturaDetalleViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val facturaRepo = FacturaRepository(db.facturaDao(), db.itemFacturaDao())
    private val facturaId: Long = checkNotNull(savedStateHandle["facturaId"])

    private val _uiState = MutableStateFlow(FacturaDetalleUiState())
    val uiState: StateFlow<FacturaDetalleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            facturaRepo.getFacturaById(facturaId).collect { factura ->
                val items = if (factura != null) {
                    facturaRepo.getItemsDeFacturaOnce(facturaId)
                } else emptyList()
                _uiState.update { it.copy(factura = factura, items = items, isLoading = false) }
            }
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturaDetalleScreen(
    facturaId: Long,
    onNavigateBack: () -> Unit,
    viewModel: FacturaDetalleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val factura = uiState.factura

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        factura?.nombreClienteOpcional ?: "Detalle de Factura",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (factura == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Factura no encontrada", color = OnSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Imagen de la factura ──────────────────────────────────────
            item {
                factura.imagenPath?.let { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Foto de la factura",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // ── Info de la factura ────────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Fecha",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    factura.fecha.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // Estado chip
                            val estadoColor = if (factura.estado.name == "CONFIRMADA")
                                ConfirmadaColor else PendienteColor
                            AssistChip(
                                onClick = {},
                                label = { Text(factura.estado.name, color = estadoColor) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = estadoColor.copy(alpha = 0.12f)
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = estadoColor.copy(alpha = 0.3f)
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = CardBorder
                        )

                        // Totales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Total original",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    CurrencyFormatter.format(factura.valorTotalOriginal),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (factura.valorTotalOriginal != factura.valorTotalAjustado)
                                        TextDecoration.LineThrough else TextDecoration.None,
                                    color = OnSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Total ajustado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    CurrencyFormatter.format(factura.valorTotalAjustado),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ConfirmadaColor
                                )
                            }
                        }
                    }
                }
            }

            // ── Encabezado productos ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Productos (${uiState.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = CardBorder
                )
            }

            // ── Ítems en modo read-only (auditoría) ──────────────────────
            items(uiState.items) { item ->
                ItemFacturaCard(
                    item = item,
                    readOnly = true
                )
            }
        }
    }
}
