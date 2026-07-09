package com.example.inventorycpm.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventorycpm.domain.model.EstadoFactura
import com.example.inventorycpm.domain.model.Factura
import com.example.inventorycpm.ui.component.EmptyState
import com.example.inventorycpm.ui.theme.*
import com.example.inventorycpm.ui.viewmodel.HistorialViewModel
import com.example.inventorycpm.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialDiaScreen(
    fecha: LocalDate,
    onNavigateBack: () -> Unit,
    onNavigateToDetalle: (Long) -> Unit,
    viewModel: HistorialViewModel = viewModel()
) {
    val uiState by viewModel.diaState.collectAsStateWithLifecycle()

    LaunchedEffect(fecha) {
        viewModel.loadFacturasDelDia(fecha)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM yyyy"))
                            .replaceFirstChar { it.uppercase() }
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

        if (uiState.facturas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.ReceiptLong,
                    title = "Sin facturas",
                    subtitle = "No hay facturas registradas para este día"
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Resumen del día
                val totalConfirmado = uiState.facturas
                    .filter { it.estado == EstadoFactura.CONFIRMADA }
                    .sumOf { it.valorTotalAjustado }
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "${uiState.facturas.size} facturas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                            Text(
                                "${uiState.facturas.count { it.estado == EstadoFactura.CONFIRMADA }} confirmadas",
                                style = MaterialTheme.typography.bodySmall,
                                color = ConfirmadaColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Total confirmado",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                            Text(
                                CurrencyFormatter.format(totalConfirmado),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ConfirmadaColor
                            )
                        }
                    }
                }
            }

            items(uiState.facturas) { factura ->
                FacturaHistorialCard(
                    factura = factura,
                    onClick = { onNavigateToDetalle(factura.id) }
                )
            }
        }
    }
}

@Composable
private fun FacturaHistorialCard(
    factura: Factura,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono estado
            val estadoIcon = if (factura.estado == EstadoFactura.CONFIRMADA)
                Icons.Default.CheckCircle else Icons.Default.Pending
            val estadoColor = if (factura.estado == EstadoFactura.CONFIRMADA)
                ConfirmadaColor else PendienteColor

            Icon(
                estadoIcon,
                contentDescription = null,
                tint = estadoColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = factura.nombreClienteOpcional ?: "Sin nombre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
                Text(
                    text = factura.estado.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = estadoColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(factura.valorTotalAjustado),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                if (factura.valorTotalOriginal != factura.valorTotalAjustado) {
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
                modifier = Modifier.size(18.dp).padding(start = 4.dp)
            )
        }
    }
}

// Extension para BigDecimal suma en lista (reemplaza sumOf con BigDecimal)
private fun List<Factura>.sumOf(selector: (Factura) -> java.math.BigDecimal): java.math.BigDecimal =
    fold(java.math.BigDecimal.ZERO) { acc, item -> acc.add(selector(item)) }
