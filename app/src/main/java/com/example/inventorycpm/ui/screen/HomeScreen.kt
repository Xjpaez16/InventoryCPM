package com.example.inventorycpm.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventorycpm.domain.model.EstadoFactura
import com.example.inventorycpm.domain.model.Factura
import com.example.inventorycpm.ui.component.EmptyState
import com.example.inventorycpm.ui.theme.*
import com.example.inventorycpm.ui.viewmodel.HomeViewModel
import com.example.inventorycpm.util.CurrencyFormatter
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToFactura: (Long) -> Unit,
    onNavigateToCierre: () -> Unit,
    onNavigateToHistorial: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = uiState.fecha

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "InventoryCPM",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM"))
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Botón historial
                    IconButton(onClick = onNavigateToHistorial) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Historial",
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = OnSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToScan,
                icon = {
                    Icon(
                        Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = OnPrimary
                    )
                },
                text = {
                    Text(
                        "Escanear Factura",
                        color = OnPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                containerColor = PrimaryBlue,
                contentColor = OnPrimary
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 100.dp // espacio para el FAB
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Card resumen del día ─────────────────────────────────────
            item {
                DaySummaryCard(
                    totalAjustado = CurrencyFormatter.format(uiState.totalAjustadoDelDia),
                    facturaCount = uiState.facturas.size,
                    confirmadasCount = uiState.facturas.count { it.estado == EstadoFactura.CONFIRMADA },
                    hayFacturasConfirmadas = uiState.hayFacturasConfirmadas,
                    cierreDiaHecho = uiState.cierreDiaHecho,
                    onCerrarDia = onNavigateToCierre
                )
            }

            // ── Título lista facturas ────────────────────────────────────
            item {
                Text(
                    text = "Facturas del día (${uiState.facturas.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.facturas.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "Sin facturas hoy",
                        subtitle = "Toca el botón para escanear la primera factura del día",
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(
                    items = uiState.facturas,
                    key = { it.id }
                ) { factura ->
                    FacturaCard(
                        factura = factura,
                        onClick = { onNavigateToFactura(factura.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    totalAjustado: String,
    facturaCount: Int,
    confirmadasCount: Int,
    hayFacturasConfirmadas: Boolean,
    cierreDiaHecho: Boolean,
    onCerrarDia: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            PrimaryBlueDark.copy(alpha = 0.3f),
                            SurfaceDark
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Total del día",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = totalAjustado,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }

                    // Indicador circular de progreso facturas
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariantDark)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$confirmadasCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ConfirmadaColor
                            )
                            Text(
                                text = "/ $facturaCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                if (hayFacturasConfirmadas) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCerrarDia,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cierreDiaHecho)
                                SurfaceVariantDark
                            else
                                PrimaryBlue
                        )
                    ) {
                        Icon(
                            if (cierreDiaHecho) Icons.Default.Edit else Icons.Default.Calculate,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (cierreDiaHecho) "Ver/Editar Cierre" else "Cerrar el Día",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FacturaCard(
    factura: Factura,
    onClick: () -> Unit
) {
    val isConfirmada = factura.estado == EstadoFactura.CONFIRMADA
    val estadoColor by animateColorAsState(
        targetValue = if (isConfirmada) ConfirmadaColor else PendienteColor,
        label = "estado_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = CardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Indicador de estado (barra lateral de color)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(estadoColor)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = factura.nombreClienteOpcional ?: "Sin nombre de cliente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isConfirmada) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = estadoColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isConfirmada) "Confirmada" else "Pendiente",
                        style = MaterialTheme.typography.labelMedium,
                        color = estadoColor
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(factura.valorTotalAjustado),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                if (factura.valorTotalOriginal.compareTo(factura.valorTotalAjustado) != 0) {
                    Text(
                        text = CurrencyFormatter.format(factura.valorTotalOriginal),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = OnSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp)
            )
        }
    }
}
