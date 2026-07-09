package com.example.inventorycpm.data.remote.model

import java.math.BigDecimal

/**
 * DTOs para parsear la respuesta cruda de la Gemini API.
 * Sólo se usan en la capa de datos; la UI nunca los ve directamente.
 */

data class GeminiResponseDto(
    val candidates: List<CandidateDto>? = null,
    val error: GeminiErrorDto? = null
)

data class GeminiErrorDto(
    val code: Int = 0,
    val message: String = "",
    val status: String = ""
)

data class CandidateDto(
    val content: ContentDto? = null,
    val finishReason: String? = null
)

data class ContentDto(
    val parts: List<PartDto>? = null,
    val role: String? = null
)

data class PartDto(
    val text: String? = null
)

/**
 * Resultado ya procesado y listo para convertir a ItemFactura del dominio.
 * Los campos pueden ser nulos si la IA no pudo leerlos con certeza.
 */
data class ParsedInvoiceItemDto(
    val nombreProducto: String?,
    val precioUnitario: BigDecimal?,
    val cantidad: Int?,
    val valorTotalLinea: BigDecimal?
)
