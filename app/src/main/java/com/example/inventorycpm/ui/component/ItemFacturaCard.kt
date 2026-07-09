package com.example.inventorycpm.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.inventorycpm.domain.model.ItemFactura
import com.example.inventorycpm.ui.theme.CardBackground
import com.example.inventorycpm.ui.theme.CardBorder
import com.example.inventorycpm.ui.theme.ConfirmadaColor
import com.example.inventorycpm.ui.theme.OnSurfaceVariant
import com.example.inventorycpm.util.CurrencyFormatter

/**
 * Card reutilizable para mostrar y editar un ítem de factura.
 *
 * @param item El ítem a mostrar.
 * @param readOnly Si true, deshabilita checkbox y stepper (vista de historial/detalle).
 * @param onToggleIncluido Callback cuando el usuario marca/desmarca el checkbox.
 * @param onCantidadChange Callback cuando cambia la cantidad entregada.
 * @param onNombreChange Callback cuando edita el nombre del producto.
 */
@Composable
fun ItemFacturaCard(
    item: ItemFactura,
    readOnly: Boolean = false,
    onToggleIncluido: (() -> Unit)? = null,
    onCantidadChange: ((Int) -> Unit)? = null,
    onNombreChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (item.incluido) 1f else 0.45f,
        label = "item_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (item.incluido) CardBorder else CardBorder.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── Fila superior: Checkbox + Nombre ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!readOnly) {
                    Checkbox(
                        checked = item.incluido,
                        onCheckedChange = { onToggleIncluido?.invoke() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ConfirmadaColor,
                            uncheckedColor = OnSurfaceVariant
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!readOnly && onNombreChange != null) {
                    OutlinedTextField(
                        value = item.nombreProducto,
                        onValueChange = onNombreChange,
                        placeholder = { Text("Nombre del producto", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier
                            .weight(1f)
                            .alpha(alpha),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (!item.incluido) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ConfirmadaColor,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                } else {
                    Text(
                        text = item.nombreProducto.ifBlank { "Producto sin nombre" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (!item.incluido) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .alpha(alpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Fila inferior: Precio + Stepper + Subtotal ───────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Precio unitario
                Column {
                    Text(
                        text = "Precio unit.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(item.precioUnitario),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Stepper de cantidad
                if (!readOnly) {
                    StepperControl(
                        value = item.cantidadEntregada,
                        maxValue = item.cantidadOriginal,
                        enabled = item.incluido,
                        onDecrement = {
                            onCantidadChange?.invoke(
                                (item.cantidadEntregada - 1).coerceAtLeast(0)
                            )
                        },
                        onIncrement = {
                            onCantidadChange?.invoke(
                                (item.cantidadEntregada + 1).coerceAtMost(item.cantidadOriginal)
                            )
                        }
                    )
                } else {
                    // Vista de auditoría: muestra cantidadEntregada / cantidadOriginal
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Entregado",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                        val cantidadText = if (item.cantidadEntregada != item.cantidadOriginal) {
                            "${item.cantidadEntregada} / ${item.cantidadOriginal}"
                        } else {
                            "${item.cantidadEntregada}"
                        }
                        Text(
                            text = cantidadText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.cantidadEntregada != item.cantidadOriginal)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Subtotal de línea
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(item.valorLineaAjustado),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (item.incluido)
                            ConfirmadaColor
                        else
                            OnSurfaceVariant
                    )
                }
            }

            // Indicador si la cantidad es parcial
            if (!readOnly && item.incluido && item.cantidadEntregada < item.cantidadOriginal && item.cantidadEntregada > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠ Entrega parcial: ${item.cantidadEntregada} de ${item.cantidadOriginal} unidades",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

/**
 * Control stepper +/- para ajustar cantidad entregada.
 */
@Composable
private fun StepperControl(
    value: Int,
    maxValue: Int,
    enabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        FilledIconButton(
            onClick = onDecrement,
            enabled = enabled && value > 0,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Disminuir cantidad",
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .widthIn(min = 36.dp)
                .wrapContentWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        FilledIconButton(
            onClick = onIncrement,
            enabled = enabled && value < maxValue,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Aumentar cantidad",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
