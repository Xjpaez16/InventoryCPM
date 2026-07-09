package com.example.inventorycpm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Entidad Room para un ítem de factura.
 * FK hacia FacturaEntity con cascade delete — al borrar una factura se borran sus ítems.
 */
@Entity(
    tableName = "items_factura",
    foreignKeys = [
        ForeignKey(
            entity = FacturaEntity::class,
            parentColumns = ["id"],
            childColumns = ["facturaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["facturaId"])]
)
data class ItemFacturaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val facturaId: Long,
    val nombreProducto: String,
    /** Precio por unidad en centavos (via BigDecimalConverter). */
    val precioUnitario: BigDecimal = BigDecimal.ZERO,
    val cantidadOriginal: Int = 1,
    /** Unidades realmente entregadas. Default = cantidadOriginal al crear. */
    val cantidadEntregada: Int = 1,
    /** false → el producto completo se excluye; valorLineaAjustado = 0. */
    val incluido: Boolean = true,
    /** precioUnitario × cantidadEntregada si incluido, sino 0. En centavos. */
    val valorLineaAjustado: BigDecimal = BigDecimal.ZERO
)
