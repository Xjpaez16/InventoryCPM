package com.example.inventorycpm.domain.model

import java.math.BigDecimal

/**
 * Modelo de dominio para un ítem de factura. Inmutable.
 * valorLineaAjustado se calcula en la capa de dominio/VM, no en DB directamente.
 */
data class ItemFactura(
    val id: Long = 0L,
    val facturaId: Long,
    val nombreProducto: String,
    val precioUnitario: BigDecimal = BigDecimal.ZERO,
    val cantidadOriginal: Int = 1,
    /** Unidades realmente entregadas; por defecto iguales a cantidadOriginal. */
    val cantidadEntregada: Int = cantidadOriginal,
    /** Si false, el producto completo se excluye del total (valorLineaAjustado = 0). */
    val incluido: Boolean = true,
    val valorLineaAjustado: BigDecimal = BigDecimal.ZERO
) {
    /**
     * Calcula el valor ajustado de esta línea en tiempo real.
     * Si no está incluido → 0. Si incluido → precioUnitario × cantidadEntregada.
     */
    fun calcularValorAjustado(): BigDecimal =
        if (!incluido) BigDecimal.ZERO
        else precioUnitario.multiply(BigDecimal(cantidadEntregada))
}
