package com.example.inventorycpm.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventorycpm.ui.theme.*
import com.example.inventorycpm.ui.viewmodel.CierreDiaViewModel
import com.example.inventorycpm.util.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreDiaScreen(
    onNavigateBack: () -> Unit,
    onCierreGuardado: () -> Unit,
    viewModel: CierreDiaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) onCierreGuardado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cierre del Día") },
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Fecha ─────────────────────────────────────────────────────
            Text(
                text = uiState.fecha.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )

            // ── Card: Total esperado de facturas ──────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Receipt,
                            null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Total facturas confirmadas",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = CurrencyFormatter.format(uiState.totalEsperado),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
            }

            // ── Campo: Efectivo contado ────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Money,
                            null,
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Efectivo contado",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.efectivoInput,
                        onValueChange = viewModel::setEfectivoInput,
                        placeholder = { Text("Ej: 250000") },
                        leadingIcon = {
                            Text(
                                "$",
                                style = MaterialTheme.typography.titleLarge,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = CardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ingresa el total en pesos (sin puntos ni comas)",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                    // Mejora futura: desglose por denominación (billetes/monedas)
                    // Ver comentario en implementación plan — no bloqueante para v1
                }
            }

            // ── Card: Diferencia ─────────────────────────────────────────
            val diferencia = uiState.diferencia
            val hasEfectivo = uiState.efectivoInput.isNotBlank()

            AnimatedDiferenciaCard(
                diferencia = diferencia,
                visible = hasEfectivo
            )

            // ── Campo: Notas ──────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.notas,
                onValueChange = viewModel::setNotas,
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Observaciones del día, ajustes, etc.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = CardBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Botón guardar cierre ───────────────────────────────────────
            Button(
                onClick = { viewModel.confirmarCierre() },
                enabled = !uiState.isSaving && uiState.efectivoInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = OnPrimary,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.SaveAlt,
                        null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (uiState.cierreExistente) "Actualizar Cierre" else "Guardar Cierre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.cierreExistente) {
                Text(
                    "⚠ Ya existe un cierre para hoy. Guardarlo lo sobreescribirá.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnimatedDiferenciaCard(
    diferencia: BigDecimal,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val isPositive = diferencia > BigDecimal.ZERO
    val isExact = diferencia.compareTo(BigDecimal.ZERO) == 0

    val cardColor by animateColorAsState(
        targetValue = when {
            isExact -> SurfaceVariantDark
            isPositive -> SuccessGreen.copy(alpha = 0.15f)
            else -> ErrorRed.copy(alpha = 0.15f)
        },
        label = "card_color"
    )

    val textColor = when {
        isExact -> OnSurface
        isPositive -> SuccessGreen
        else -> ErrorRed
    }

    val icon = when {
        isExact -> Icons.Default.CheckCircle
        isPositive -> Icons.Default.TrendingUp
        else -> Icons.Default.TrendingDown
    }

    val label = when {
        isExact -> "Cuadra exacto ✓"
        isPositive -> "Sobra efectivo"
        else -> "Falta efectivo"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            textColor.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor.copy(alpha = 0.8f)
                )
                Text(
                    text = if (isExact) "$ 0" else CurrencyFormatter.format(diferencia.abs()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}
