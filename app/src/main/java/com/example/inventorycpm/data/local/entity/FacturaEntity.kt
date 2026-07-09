package com.example.inventorycpm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Entidad Room para una factura. Los campos monetarios se almacenan como Long (centavos)
 * mediante BigDecimalConverter — nunca Float/Double.
 */
@Entity(
    tableName = "facturas",
    indices = [Index(value = ["fecha"])]
)
data class FacturaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fecha: LocalDate,
    val nombreClienteOpcional: String? = null,
    /** Suma de todos los ítems tal como vienen en la factura escaneada. En centavos. */
    val valorTotalOriginal: BigDecimal = BigDecimal.ZERO,
    /** Recalculado según lo realmente entregado. En centavos. */
    val valorTotalAjustado: BigDecimal = BigDecimal.ZERO,
    /** "PENDIENTE" o "CONFIRMADA" */
    val estado: String = "PENDIENTE",
    /** Ruta absoluta al archivo de imagen guardado localmente. */
    val imagenPath: String? = null,
    val fechaHoraCreacion: LocalDateTime = LocalDateTime.now()
)
