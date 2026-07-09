package com.example.inventorycpm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inventorycpm.data.local.entity.ItemFacturaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemFacturaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemFacturaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemFacturaEntity): Long

    @Update
    suspend fun updateItem(item: ItemFacturaEntity)

    @Update
    suspend fun updateItems(items: List<ItemFacturaEntity>)

    @Query("SELECT * FROM items_factura WHERE facturaId = :facturaId ORDER BY id ASC")
    fun getItemsByFacturaId(facturaId: Long): Flow<List<ItemFacturaEntity>>

    @Query("SELECT * FROM items_factura WHERE facturaId = :facturaId ORDER BY id ASC")
    suspend fun getItemsByFacturaIdOnce(facturaId: Long): List<ItemFacturaEntity>

    @Query("DELETE FROM items_factura WHERE facturaId = :facturaId")
    suspend fun deleteItemsByFacturaId(facturaId: Long)
}
