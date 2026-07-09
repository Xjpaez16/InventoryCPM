package com.example.inventorycpm.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.inventorycpm.domain.model.ItemFactura
import com.example.inventorycpm.ui.component.EmptyState
import com.example.inventorycpm.ui.component.ItemFacturaCard
import com.example.inventorycpm.ui.theme.*
import com.example.inventorycpm.ui.viewmodel.FacturaReviewViewModel
import com.example.inventorycpm.ui.viewmodel.FacturaReviewViewModelFactory
import com.example.inventorycpm.util.CurrencyFormatter
import java.io.File
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturaReviewScreen(
    facturaId: Long,
    onNavigateBack: () -> Unit,
    onConfirmado: () -> Unit,
    viewModel: FacturaReviewViewModel = viewModel(
        factory = FacturaReviewViewModelFactory(facturaId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Navegar tras guardar exitosamente
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onConfirmado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isConfirmada) "Factura Confirmada" else "Revisar Factura",
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
        },
        bottomBar = {
            // ── Barra inferior con total y botón de confirmar ──────────────
            Surface(
                color = SurfaceDark,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total ajustado:",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(uiState.totalAjustado),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ConfirmadaColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!uiState.isConfirmada) {
                        Button(
                            onClick = { showConfirmDialog = true },
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ConfirmadaColor)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    color = OnPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    modifier = Modifier.size(22.dp),
                                    tint = OnPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Confirmar Factura",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimary
                                )
                            }
                        }
                    } else {
                        // Factura ya confirmada — badge de estado
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = ConfirmadaColor.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = ConfirmadaColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Factura confirmada",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ConfirmadaColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {

            // ── Imagen de la factura (miniatura) ──────────────────────────
            item {
                uiState.factura?.imagenPath?.let { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Foto de la factura",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // ── Campo nombre del cliente ───────────────────────────────────
            item {
                OutlinedTextField(
                    value = uiState.nombreCliente,
                    onValueChange = viewModel::setNombreCliente,
                    label = { Text("Nombre del cliente (opcional)") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, null, tint = OnSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    enabled = !uiState.isConfirmada,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Encabezado lista de productos ─────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Productos (${uiState.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!uiState.isConfirmada) {
                        TextButton(onClick = { viewModel.addItemManual() }) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Añadir")
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = CardBorder
                )
            }

            // ── Lista de ítems ────────────────────────────────────────────
            if (uiState.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Inventory2,
                        title = "Sin productos",
                        subtitle = "La IA no detectó ítems. Agrega productos manualmente.",
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            } else {
                items(
                    items = uiState.items,
                    key = { item -> item.id }
                ) { item ->
                    ItemFacturaCard(
                        item = item,
                        readOnly = uiState.isConfirmada,
                        onToggleIncluido = { viewModel.toggleIncluido(item.id) },
                        onCantidadChange = { cantidad ->
                            viewModel.setCantidadEntregada(item.id, cantidad)
                        },
                        onNombreChange = { nombre ->
                            viewModel.setNombreProducto(item.id, nombre)
                        }
                    )
                }
            }
        }

        // ── Diálogo de confirmación ─────────────────────────────────────
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                icon = { Icon(Icons.Default.CheckCircle, null, tint = ConfirmadaColor) },
                title = { Text("Confirmar Factura") },
                text = {
                    Column {
                        Text(
                            "¿Confirmas que los productos y cantidades son correctos?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Total a confirmar: ${CurrencyFormatter.format(uiState.totalAjustado)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ConfirmadaColor
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.confirmarFactura()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ConfirmadaColor)
                    ) {
                        Text("Confirmar", color = OnPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
