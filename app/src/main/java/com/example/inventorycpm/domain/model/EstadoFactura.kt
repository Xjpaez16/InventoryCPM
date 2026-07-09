package com.example.inventorycpm.domain.model

/**
 * Representa el estado de una factura en el proceso de entrega.
 */
enum class EstadoFactura {
    /** Factura escaneada pero aún no confirmada por el usuario. */
    PENDIENTE,

    /** El usuario ha confirmado qué se entregó y qué no. Valores bloqueados. */
    CONFIRMADA
}
