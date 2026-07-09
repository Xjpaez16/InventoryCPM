package com.example.inventorycpm.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Formateador de moneda COP (pesos colombianos).
 * Usa enteros — los pesos colombianos normalmente no usan centavos en transacciones comerciales.
 *
 * Ejemplo: 150000.00 → "$ 150.000"
 */
object CurrencyFormatter {

    private val copLocale = Locale("es", "CO")
    private val numberFormat = NumberFormat.getNumberInstance(copLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    /**
     * Formatea un BigDecimal como precio COP.
     * @return String formateado, ej: "$ 150.000"
     */
    fun format(amount: BigDecimal): String {
        return "$ ${numberFormat.format(amount)}"
    }

    /**
     * Formatea un Long en centavos como precio COP.
     */
    fun formatCentavos(centavos: Long): String {
        val pesos = BigDecimal(centavos).divide(BigDecimal(100))
        return format(pesos)
    }

    /**
     * Parsea un String de entrada del usuario a BigDecimal.
     * Acepta formatos: "150000", "150.000", "150,000".
     * Retorna null si no es parseable.
     */
    fun parse(input: String): BigDecimal? {
        return try {
            // Eliminar puntos de miles y posibles espacios, reemplazar coma decimal por punto
            val cleaned = input
                .replace("\\s".toRegex(), "")
                .replace("$", "")
                .replace(".", "")
                .replace(",", ".")
            BigDecimal(cleaned)
        } catch (e: NumberFormatException) {
            null
        }
    }
}
