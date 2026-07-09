package com.example.inventorycpm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inventorycpm.data.local.entity.FacturaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FacturaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactura(factura: FacturaEntity): Long

    @Update
    suspend fun updateFactura(factura: FacturaEntity)

    @Query("SELECT * FROM facturas WHERE fecha = :fecha ORDER BY fechaHoraCreacion ASC")
    fun getFacturasByFecha(fecha: java.time.LocalDate): Flow<List<FacturaEntity>>

    @Query("SELECT * FROM facturas WHERE id = :facturaId")
    fun getFacturaById(facturaId: Long): Flow<FacturaEntity?>

    @Query("SELECT * FROM facturas WHERE id = :facturaId")
    suspend fun getFacturaByIdOnce(facturaId: Long): FacturaEntity?

    /** Suma de valorTotalAjustado de facturas CONFIRMADAS en la fecha dada. */
    @Query("""
        SELECT COALESCE(SUM(valorTotalAjustado), 0)
        FROM facturas
        WHERE fecha = :fecha AND estado = 'CONFIRMADA'
    """)
    suspend fun getTotalAjustadoConfirmadasByFecha(fecha: java.time.LocalDate): Long

    /** Todos los días que tienen al menos una factura (para historial). */
    @Query("SELECT DISTINCT fecha FROM facturas ORDER BY fecha DESC")
    fun getDiaConFacturas(): Flow<List<java.time.LocalDate>>

    @Query("SELECT * FROM facturas WHERE fecha = :fecha ORDER BY fechaHoraCreacion ASC")
    suspend fun getFacturasByFechaOnce(fecha: java.time.LocalDate): List<FacturaEntity>

    @Query("DELETE FROM facturas WHERE id = :facturaId")
    suspend fun deleteFactura(facturaId: Long)
}
