package com.example.inventorycpm.data.local.converter

import androidx.room.TypeConverter
import java.math.BigDecimal

/**
 * Room TypeConverter para BigDecimal ↔ Long (centavos).
 * Estrategia: almacenar centavos como Long para máxima precisión sin Float/Double.
 * 1 peso COP = 100 centavos → $150.000 COP = 15_000_000L centavos.
 */
class BigDecimalConverter {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): Long? {
        return value?.multiply(BigDecimal(100))?.toLong()
    }

    @TypeConverter
    fun toBigDecimal(value: Long?): BigDecimal? {
        return value?.let { BigDecimal(it).divide(BigDecimal(100), 2, java.math.RoundingMode.HALF_UP) }
    }
}
