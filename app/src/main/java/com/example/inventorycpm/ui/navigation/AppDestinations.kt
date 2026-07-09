package com.example.inventorycpm.ui.navigation

/**
 * Rutas de navegación de la app. Constantes centralizadas para evitar strings mágicos.
 */
object AppDestinations {
    const val HOME = "home"
    const val SCAN = "scan"
    const val FACTURA_REVIEW = "factura_review/{facturaId}"
    const val CIERRE_DIA = "cierre_dia"
    const val HISTORIAL = "historial"
    const val HISTORIAL_DIA = "historial_dia/{fecha}"
    const val FACTURA_DETALLE = "factura_detalle/{facturaId}"

    fun facturaReview(facturaId: Long) = "factura_review/$facturaId"
    fun historialDia(fecha: String) = "historial_dia/$fecha"
    fun facturaDetalle(facturaId: Long) = "factura_detalle/$facturaId"
}
