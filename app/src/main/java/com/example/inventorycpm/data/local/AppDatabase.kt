package com.example.inventorycpm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.inventorycpm.data.local.converter.BigDecimalConverter
import com.example.inventorycpm.data.local.converter.DateConverter
import com.example.inventorycpm.data.local.dao.CierreDiaDao
import com.example.inventorycpm.data.local.dao.FacturaDao
import com.example.inventorycpm.data.local.dao.ItemFacturaDao
import com.example.inventorycpm.data.local.entity.CierreDiaEntity
import com.example.inventorycpm.data.local.entity.FacturaEntity
import com.example.inventorycpm.data.local.entity.ItemFacturaEntity

/**
 * Base de datos Room principal de la app.
 * version = 1 → incrementar y agregar Migration si cambia el schema.
 */
@Database(
    entities = [
        FacturaEntity::class,
        ItemFacturaEntity::class,
        CierreDiaEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class, DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun facturaDao(): FacturaDao
    abstract fun itemFacturaDao(): ItemFacturaDao
    abstract fun cierreDiaDao(): CierreDiaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventorycpm_db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
