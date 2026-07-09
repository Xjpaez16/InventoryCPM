package com.example.inventorycpm.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Entidad Room para el cierre del día.
 * Se permite un único cierre por fecha (índice unique en fecha).
 *
 * diferencia = efectivoContado - totalFacturasConfirmadas
 *   > 0 → sobra efectivo
 *   < 0 → falta efectivo
 */
@Entity(
    tableName = "cierres_dia",
    indices = [Index(value = ["fecha"], unique = true)]
)
data class CierreDiaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fecha: LocalDate,
    /** Suma de valorTotalAjustado de todas las facturas CONFIRMADAS ese día. En centavos. */
    val totalFacturasConfirmadas: BigDecimal = BigDecimal.ZERO,
    /** Valor que el usuario ingresó manualmente al contar el efectivo. En centavos. */
    val efectivoContado: BigDecimal = BigDecimal.ZERO,
    /** efectivoContado - totalFacturasConfirmadas. En centavos. */
    val diferencia: BigDecimal = BigDecimal.ZERO,
    /** Notas libres opcionales. */
    val notas: String? = null
)
