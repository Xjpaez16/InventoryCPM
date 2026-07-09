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
import com.example.inventorycpm.domain.model.CierreDia
import com.example.inventorycpm.ui.component.EmptyState
import com.example.inventorycpm.ui.theme.*
import com.example.inventorycpm.ui.viewmodel.HistorialViewModel
import com.example.inventorycpm.util.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDia: (String) -> Unit,
    viewModel: HistorialViewModel = viewModel()
) {
    val uiState by viewModel.historialState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.cierres.isEmpty() && uiState.diasConFacturas.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = "Sin historial aún",
                    subtitle = "Los días con facturas y cierres aparecerán aquí",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Combinar cierres y días con facturas para el listado
                val diasConCierre = uiState.cierres.map { it.fecha }.toSet()
                val todosLosDias = (diasConCierre + uiState.diasConFacturas).sortedDescending()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(todosLosDias) { fecha ->
                        val cierre = uiState.cierres.find { it.fecha == fecha }
                        DiaHistorialCard(
                            fecha = fecha,
                            cierre = cierre,
                            onClick = { onNavigateToDia(fecha.toString()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaHistorialCard(
    fecha: LocalDate,
    cierre: CierreDia?,
    onClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM")
    val isToday = fecha == LocalDate.now()
    val isYesterday = fecha == LocalDate.now().minusDays(1)

    val diaLabel = when {
        isToday -> "Hoy"
        isYesterday -> "Ayer"
        else -> fecha.format(dateFormatter)
            .replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = diaLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) PrimaryBlue else OnSurface
                    )
                    Text(
                        text = fecha.format(DateTimeFormatter.ofPattern("yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                // Badge estado del cierre
                if (cierre != null) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "Cerrado",
                                style = MaterialTheme.typography.labelSmall,
                                color = ConfirmadaColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = ConfirmadaColor,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ConfirmadaColor.copy(alpha = 0.1f),
                            leadingIconContentColor = ConfirmadaColor
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = ConfirmadaColor.copy(alpha = 0.3f))
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "Sin cierre",
                                style = MaterialTheme.typography.labelSmall,
                                color = PendienteColor
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PendienteColor.copy(alpha = 0.1f)
                        ),
                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = PendienteColor.copy(alpha = 0.3f))
                    )
                }
            }

            if (cierre != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = CardBorder
                )

                // Fila de cifras: Total facturas | Efectivo | Diferencia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CifraColumn(
                        label = "Total facturas",
                        value = CurrencyFormatter.format(cierre.totalFacturasConfirmadas)
                    )
                    CifraColumn(
                        label = "Efectivo",
                        value = CurrencyFormatter.format(cierre.efectivoContado)
                    )
                    val dif = cierre.diferencia
                    val difColor = when {
                        dif.compareTo(BigDecimal.ZERO) == 0 -> OnSurface
                        dif > BigDecimal.ZERO -> SuccessGreen
                        else -> ErrorRed
                    }
                    val difPrefix = when {
                        dif.compareTo(BigDecimal.ZERO) == 0 -> ""
                        dif > BigDecimal.ZERO -> "+"
                        else -> "-"
                    }
                    CifraColumn(
                        label = "Diferencia",
                        value = "$difPrefix${CurrencyFormatter.format(dif.abs())}",
                        valueColor = difColor
                    )
                }
            }

            // Indicador de navegación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (cierre != null) 8.dp else 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Ver detalle",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue
                )
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CifraColumn(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = OnSurface
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
