package com.example.inventorycpm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inventorycpm.data.local.entity.CierreDiaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CierreDiaDao {

    /** Upsert — reemplaza si ya existe un cierre para esa fecha (unique index). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceCierre(cierre: CierreDiaEntity): Long

    @Update
    suspend fun updateCierre(cierre: CierreDiaEntity)

    @Query("SELECT * FROM cierres_dia WHERE fecha = :fecha LIMIT 1")
    fun getCierreDiaByFecha(fecha: java.time.LocalDate): Flow<CierreDiaEntity?>

    @Query("SELECT * FROM cierres_dia WHERE fecha = :fecha LIMIT 1")
    suspend fun getCierreDiaByFechaOnce(fecha: java.time.LocalDate): CierreDiaEntity?

    @Query("SELECT * FROM cierres_dia ORDER BY fecha DESC")
    fun getAllCierres(): Flow<List<CierreDiaEntity>>
}
