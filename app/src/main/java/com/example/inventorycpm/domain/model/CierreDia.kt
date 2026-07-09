package com.example.inventorycpm.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Modelo de dominio para el cierre del día.
 * diferencia = efectivoContado - totalFacturasConfirmadas
 *   > 0 → sobra efectivo
 *   < 0 → falta efectivo
 *   = 0 → cuadra exacto
 */
data class CierreDia(
    val id: Long = 0L,
    val fecha: LocalDate,
    val totalFacturasConfirmadas: BigDecimal = BigDecimal.ZERO,
    val efectivoContado: BigDecimal = BigDecimal.ZERO,
    val diferencia: BigDecimal = BigDecimal.ZERO,
    /** Notas libres opcionales del usuario. */
    val notas: String? = null
)
