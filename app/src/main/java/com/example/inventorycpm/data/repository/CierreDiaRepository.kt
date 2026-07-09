package com.example.inventorycpm.data.repository

import com.example.inventorycpm.data.local.dao.CierreDiaDao
import com.example.inventorycpm.data.local.entity.CierreDiaEntity
import com.example.inventorycpm.domain.model.CierreDia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repositorio del cierre de día. Único punto de acceso a CierreDia en la app.
 */
class CierreDiaRepository(
    private val cierreDiaDao: CierreDiaDao
) {

    fun getCierreDia(fecha: LocalDate): Flow<CierreDia?> =
        cierreDiaDao.getCierreDiaByFecha(fecha)
            .map { it?.toDomain() }

    suspend fun getCierreDiaOnce(fecha: LocalDate): CierreDia? =
        cierreDiaDao.getCierreDiaByFechaOnce(fecha)?.toDomain()

    fun getAllCierres(): Flow<List<CierreDia>> =
        cierreDiaDao.getAllCierres().map { list -> list.map { it.toDomain() } }

    /** Upsert — puede re-hacer el cierre del día si ya existía. */
    suspend fun insertOrUpdateCierre(cierre: CierreDia): Long =
        cierreDiaDao.insertOrReplaceCierre(cierre.toEntity())

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun CierreDiaEntity.toDomain(): CierreDia = CierreDia(
        id = id,
        fecha = fecha,
        totalFacturasConfirmadas = totalFacturasConfirmadas,
        efectivoContado = efectivoContado,
        diferencia = diferencia,
        notas = notas
    )

    private fun CierreDia.toEntity(): CierreDiaEntity = CierreDiaEntity(
        id = id,
        fecha = fecha,
        totalFacturasConfirmadas = totalFacturasConfirmadas,
        efectivoContado = efectivoContado,
        diferencia = diferencia,
        notas = notas
    )
}
