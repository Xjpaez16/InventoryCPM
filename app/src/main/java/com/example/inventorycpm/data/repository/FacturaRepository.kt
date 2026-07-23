package com.example.inventorycpm.data.repository

import com.example.inventorycpm.data.local.dao.FacturaDao
import com.example.inventorycpm.data.local.dao.ItemFacturaDao
import com.example.inventorycpm.data.local.entity.FacturaEntity
import com.example.inventorycpm.data.local.entity.ItemFacturaEntity
import com.example.inventorycpm.domain.model.EstadoFactura
import com.example.inventorycpm.domain.model.Factura
import com.example.inventorycpm.domain.model.ItemFactura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Repositorio de Facturas. Única fuente de verdad para datos de facturas e ítems.
 * Convierte entre entidades Room y modelos de dominio.
 */
class FacturaRepository(
    private val facturaDao: FacturaDao,
    private val itemFacturaDao: ItemFacturaDao
) {
    // ── Queries reactivos ────────────────────────────────────────────────────

    fun getFacturasDelDia(fecha: LocalDate): Flow<List<Factura>> =
        facturaDao.getFacturasByFecha(fecha)
            .map { list -> list.map { it.toDomain() } }

    fun getFacturaById(facturaId: Long): Flow<Factura?> =
        facturaDao.getFacturaById(facturaId).map { it?.toDomain() }

    fun getDiasConFacturas(): Flow<List<LocalDate>> =
        facturaDao.getDiaConFacturas()

    suspend fun getFacturasDelDiaOnce(fecha: LocalDate): List<Factura> =
        facturaDao.getFacturasByFechaOnce(fecha)
            .map { it.toDomain() }

    fun getItemsDeFactura(facturaId: Long): Flow<List<ItemFactura>> =
        itemFacturaDao.getItemsByFacturaId(facturaId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun getItemsDeFacturaOnce(facturaId: Long): List<ItemFactura> =
        itemFacturaDao.getItemsByFacturaIdOnce(facturaId).map { it.toDomain() }

    // ── Operaciones de escritura ─────────────────────────────────────────────

    /**
     * Inserta una factura nueva junto a sus ítems de forma atómica.
     * @return id de la factura insertada.
     */
    suspend fun insertFacturaConItems(factura: Factura, items: List<ItemFactura>): Long {
        val facturaEntity = factura.toEntity()
        val facturaId = facturaDao.insertFactura(facturaEntity)

        val itemEntities = items.map { it.toEntity(facturaId) }
        itemFacturaDao.insertItems(itemEntities)

        return facturaId
    }

    /**
     * Actualiza la factura y reemplaza todos sus ítems.
     * Usado al confirmar la factura con los ajustes finales.
     */
    suspend fun updateFacturaConItems(factura: Factura, items: List<ItemFactura>) {
        facturaDao.updateFactura(factura.toEntity())
        // Borrar ítems existentes y reinsertar con valores actualizados
        itemFacturaDao.deleteItemsByFacturaId(factura.id)
        itemFacturaDao.insertItems(items.map { it.toEntity(factura.id) })
    }

    suspend fun updateFactura(factura: Factura) {
        facturaDao.updateFactura(factura.toEntity())
    }

    suspend fun deleteFacturaConItems(facturaId: Long) {
        itemFacturaDao.deleteItemsByFacturaId(facturaId)
        facturaDao.deleteFactura(facturaId)
    }

    suspend fun getTotalAjustadoConfirmadasDelDia(fecha: LocalDate): BigDecimal {
        val centavos = facturaDao.getTotalAjustadoConfirmadasByFecha(fecha)
        return BigDecimal(centavos).divide(BigDecimal(100))
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun FacturaEntity.toDomain(): Factura = Factura(
        id = id,
        fecha = fecha,
        nombreClienteOpcional = nombreClienteOpcional,
        valorTotalOriginal = valorTotalOriginal,
        valorTotalAjustado = valorTotalAjustado,
        estado = EstadoFactura.valueOf(estado),
        imagenPath = imagenPath,
        fechaHoraCreacion = fechaHoraCreacion
    )

    private fun Factura.toEntity(): FacturaEntity = FacturaEntity(
        id = id,
        fecha = fecha,
        nombreClienteOpcional = nombreClienteOpcional,
        valorTotalOriginal = valorTotalOriginal,
        valorTotalAjustado = valorTotalAjustado,
        estado = estado.name,
        imagenPath = imagenPath,
        fechaHoraCreacion = fechaHoraCreacion
    )

    private fun ItemFacturaEntity.toDomain(): ItemFactura = ItemFactura(
        id = id,
        facturaId = facturaId,
        nombreProducto = nombreProducto,
        precioUnitario = precioUnitario,
        cantidadOriginal = cantidadOriginal,
        cantidadEntregada = cantidadEntregada,
        incluido = incluido,
        valorLineaAjustado = valorLineaAjustado
    )

    private fun ItemFactura.toEntity(overridFacturaId: Long = facturaId): ItemFacturaEntity =
        ItemFacturaEntity(
            id = id,
            facturaId = overridFacturaId,
            nombreProducto = nombreProducto,
            precioUnitario = precioUnitario,
            cantidadOriginal = cantidadOriginal,
            cantidadEntregada = cantidadEntregada,
            incluido = incluido,
            valorLineaAjustado = valorLineaAjustado
        )
}
