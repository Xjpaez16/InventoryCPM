package com.example.inventorycpm.data.remote

import com.example.inventorycpm.BuildConfig
import com.example.inventorycpm.data.remote.model.ParsedInvoiceItemDto
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * Servicio de escaneo de facturas usando Gemini 1.5 Flash (Vision).
 *
 * Responsabilidades:
 *  1. Codificar la imagen en Base64.
 *  2. Construir el payload JSON para la API de Gemini.
 *  3. Parsear la respuesta de texto libre a lista de [ParsedInvoiceItemDto].
 *  4. Manejar errores de parseo de forma robusta (JSON malformado, campos faltantes).
 *
 * NOTA: La API key se lee de BuildConfig (originada en local.properties, nunca hardcodeada).
 */
class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val apiKey = BuildConfig.GEMINI_API_KEY

    /** Endpoint de Gemini 1.5 Flash con soporte de visión (imágenes). */
    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    /**
     * Procesa una imagen de factura y retorna los ítems extraídos.
     *
     * @param imageFile Archivo de imagen en almacenamiento interno de la app.
     * @return [Result] con lista de [ParsedInvoiceItemDto] (puede incluir ítems con campos nulos
     *         si la IA no pudo leerlos con certeza — la UI permite edición manual en ese caso).
     */
    suspend fun scanInvoice(imageFile: File): Result<List<ParsedInvoiceItemDto>> =
        withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("API key de Gemini no configurada. Agrega GEMINI_API_KEY en local.properties.")
                    )
                }

                val base64Image = android.util.Base64.encodeToString(
                    imageFile.readBytes(),
                    android.util.Base64.NO_WRAP
                )

                val mimeType = when (imageFile.extension.lowercase()) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }

                val payload = buildGeminiPayload(base64Image, mimeType)
                val requestBody = payload.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Error de la API de Gemini (${response.code}): $responseBody")
                    )
                }

                val items = parseGeminiResponse(responseBody)
                Result.success(items)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Construye el JSON de solicitud para Gemini con imagen + prompt de extracción.
     * El prompt pide explícitamente JSON estructurado sin texto adicional.
     */
    private fun buildGeminiPayload(base64Image: String, mimeType: String): String {
        val prompt = """
            Analiza esta imagen de una factura comercial o recibo de venta.
            Extrae TODOS los productos o ítems listados y responde ÚNICAMENTE con un JSON válido,
            sin texto adicional, sin markdown, sin bloques de código, solo el JSON puro con este formato exacto:
            {
              "items": [
                {
                  "nombre_producto": "string (nombre del producto o descripción)",
                  "precio_unitario": number (precio por unidad, sin símbolos de moneda),
                  "cantidad": number (cantidad de unidades),
                  "valor_total_linea": number (precio_unitario × cantidad)
                }
              ]
            }
            Si no puedes leer un campo con certeza, usa null para ese campo.
            Si la imagen no contiene una factura legible, responde: {"items": []}
        """.trimIndent()

        return """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "inline_data": {
                        "mime_type": "$mimeType",
                        "data": "$base64Image"
                      }
                    },
                    {
                      "text": ${gson.toJson(prompt)}
                    }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.1,
                "topP": 0.8,
                "maxOutputTokens": 2048
              }
            }
        """.trimIndent()
    }

    /**
     * Parsea la respuesta de texto de Gemini para extraer la lista de ítems.
     *
     * Estrategia robusta:
     *  1. Extrae el texto de la respuesta.
     *  2. Busca el bloque JSON usando regex (maneja casos donde Gemini agrega texto extra).
     *  3. Parsea con Gson, captura excepciones de parseo.
     *  4. Ítems con campos nulos se convierten a defaults editables por el usuario.
     */
    private fun parseGeminiResponse(responseBody: String): List<ParsedInvoiceItemDto> {
        return try {
            val responseJson = gson.fromJson(responseBody, JsonObject::class.java)

            // Extraer texto de la primera candidata
            val text = responseJson
                .getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
                ?: return emptyList()

            // Buscar el JSON dentro del texto (Gemini a veces agrega explicaciones)
            val jsonText = extractJsonFromText(text) ?: return emptyList()

            val parsedJson = gson.fromJson(jsonText, JsonObject::class.java)
            val itemsArray = parsedJson.getAsJsonArray("items") ?: return emptyList()

            itemsArray.mapNotNull { element ->
                try {
                    val item = element.asJsonObject
                    ParsedInvoiceItemDto(
                        nombreProducto = item.get("nombre_producto")?.takeIf { !it.isJsonNull }?.asString,
                        precioUnitario = item.get("precio_unitario")?.takeIf { !it.isJsonNull }
                            ?.asDouble?.let { BigDecimal(it.toString()) },
                        cantidad = item.get("cantidad")?.takeIf { !it.isJsonNull }?.asInt,
                        valorTotalLinea = item.get("valor_total_linea")?.takeIf { !it.isJsonNull }
                            ?.asDouble?.let { BigDecimal(it.toString()) }
                    )
                } catch (e: Exception) {
                    // Si un ítem individual falla el parseo, lo saltamos pero no fallamos todo
                    null
                }
            }
        } catch (e: JsonSyntaxException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Extrae el primer bloque JSON válido de un texto que puede contener
     * markdown u otro texto alrededor.
     */
    private fun extractJsonFromText(text: String): String? {
        // Intentar extraer de bloques ```json ... ``` o ``` ... ```
        val markdownRegex = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```")
        markdownRegex.find(text)?.groupValues?.get(1)?.let { return it }

        // Buscar el primer { que abre el JSON de items y el último }
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        if (startIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1)
        }
        return null
    }
}
