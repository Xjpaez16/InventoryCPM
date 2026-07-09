package com.example.inventorycpm.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Modelo de dominio para una factura. Inmutable — se reemplaza, no se muta.
 * Los valores monetarios usan BigDecimal para precisión exacta.
 */
data class Factura(
    val id: Long = 0L,
    val fecha: LocalDate,
    val nombreClienteOpcional: String? = null,
    val valorTotalOriginal: BigDecimal = BigDecimal.ZERO,
    val valorTotalAjustado: BigDecimal = BigDecimal.ZERO,
    val estado: EstadoFactura = EstadoFactura.PENDIENTE,
    val imagenPath: String? = null,
    val fechaHoraCreacion: LocalDateTime = LocalDateTime.now()
)
