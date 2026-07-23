package com.example.inventorycpm.data.remote

import android.util.Log
import com.example.inventorycpm.BuildConfig
import com.example.inventorycpm.data.remote.model.ParsedInvoiceItemDto
import com.example.inventorycpm.util.ImageUtil
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
import java.io.IOException
import java.math.BigDecimal
import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class GeminiService {

    private val ipv4Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            // Prioritize IPv4 addresses to avoid IPv6 connection issues
            val ipv4 = addresses.filterIsInstance<Inet4Address>()
            return ipv4.ifEmpty { addresses }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .dns(ipv4Dns)
        .build()

    private val gson = Gson()
    private val apiKey = BuildConfig.GEMINI_API_KEY.trim()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/interactions"

    suspend fun scanInvoice(imageFile: File): Result<List<ParsedInvoiceItemDto>> =
        withContext(Dispatchers.IO) {
            val totalStartTime = System.currentTimeMillis()
            try {
                Log.d("GeminiService", "🚀 [PASO 1] Iniciando escaneo. Archivo: ${imageFile.name} (${imageFile.length() / 1024} KB)")

                if (apiKey.isBlank()) {
                    Log.e("GeminiService", "❌ API key vacía")
                    return@withContext Result.failure(
                        IllegalStateException("API key de Gemini no configurada.")
                    )
                }

                // 1. Optimización e imagen a Base64
                val imgStartTime = System.currentTimeMillis()
                Log.d("GeminiService", "🖼️ [PASO 2] Procesando e imprimiendo imagen...")
                val bytes = ImageUtil.resizeAndCompressImage(imageFile)
                    ?: imageFile.readBytes()

                val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val imgDuration = System.currentTimeMillis() - imgStartTime
                Log.d("GeminiService", "✅ Base64 completado en ${imgDuration}ms. Tamaño: ${base64Image.length / 1024} KB (${base64Image.length} caracteres)")

                val mimeType = when (imageFile.extension.lowercase()) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }

                // 2. Construcción de Payload
                val payloadStartTime = System.currentTimeMillis()
                Log.d("GeminiService", "📦 [PASO 3] Construyendo payload JSON...")
                val payload = buildGeminiPayload(base64Image, mimeType)
                val requestBody = payload.toRequestBody("application/json".toMediaType())
                val payloadDuration = System.currentTimeMillis() - payloadStartTime
                Log.d("GeminiService", "✅ Payload listo en ${payloadDuration}ms. Peso total enviado: ${requestBody.contentLength() / 1024} KB")

                // 3. Petición HTTP
                val request = Request.Builder()
                    .url(baseUrl)
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Api-Revision", "2026-05-20")
                    .post(requestBody)
                    .build()

                Log.d("GeminiService", "📡 [PASO 4] Preparando solicitud HTTP a Gemini...")
                Log.d("GeminiService", "URL: ${request.url}")
                Log.d("GeminiService", "Method: ${request.method}")
                Log.d("GeminiService", "Headers:\n${request.headers}")
                Log.d("GeminiService", "Payload Start: ${payload.take(500)}...")

                Log.d("GeminiService", "📡 [PASO 4.1] Enviando solicitud HTTP POST... (Esperando respuesta)")
                val netStartTime = System.currentTimeMillis()

                val response = client.newCall(request).execute()

                val netDuration = System.currentTimeMillis() - netStartTime
                Log.d("GeminiService", "⚡ HTTP Status recibido: ${response.code} en ${netDuration}ms (${netDuration / 1000.0}s)")

                // 4. Lectura de Respuesta
                val readStartTime = System.currentTimeMillis()
                Log.d("GeminiService", "📥 [PASO 5] Leyendo cuerpo de respuesta...")
                val responseBody = response.body?.string() ?: ""
                val readDuration = System.currentTimeMillis() - readStartTime
                Log.d("GeminiService", "✅ Respuesta leída en ${readDuration}ms. Tamaño recibido: ${responseBody.length} caracteres")

                if (!response.isSuccessful) {
                    Log.e("GeminiService", "❌ Error API (${response.code}): $responseBody")
                    return@withContext Result.failure(
                        Exception("Error de la API de Gemini (${response.code})")
                    )
                }

                // 5. Parseo JSON
                val parseStartTime = System.currentTimeMillis()
                Log.d("GeminiService", "🔍 [PASO 6] Extrayendo e interpretando JSON de la factura...")
                val items = parseGeminiResponse(responseBody)
                val parseDuration = System.currentTimeMillis() - parseStartTime

                val totalDuration = System.currentTimeMillis() - totalStartTime
                Log.d("GeminiService", "🎉 [COMPLETADO] Proceso finalizado en ${totalDuration}ms (${totalDuration / 1000.0}s). Ítems extraídos: ${items.size}")

                Result.success(items)

            } catch (e: SocketTimeoutException) {
                val elapsed = System.currentTimeMillis() - totalStartTime
                Log.e("GeminiService", "⏳ TIMEOUT de red después de ${elapsed}ms (${elapsed / 1000.0}s). La conexión tardó demasiado.", e)
                Result.failure(e)
            } catch (e: IOException) {
                Log.e("GeminiService", "🔌 Error de E/S o conectividad a la red", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e("GeminiService", "💥 Excepción inesperada durante el proceso", e)
                Result.failure(e)
            }
        }

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

        val payloadMap = mapOf(
            "model" to "gemini-3.6-flash",
            "input" to listOf(
                mapOf(
                    "type" to "text",
                    "text" to prompt
                ),
                mapOf(
                    "type" to "image",
                    "data" to base64Image,
                    "mime_type" to mimeType
                )
            )
        )

        return gson.toJson(payloadMap)
    }

    private fun parseGeminiResponse(responseBody: String): List<ParsedInvoiceItemDto> {
        return try {
            val responseJson = gson.fromJson(responseBody, JsonObject::class.java)

            val text = extractTextFromResponse(responseJson) ?: return emptyList()

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
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Error parseando la respuesta JSON", e)
            emptyList()
        }
    }

    private fun extractTextFromResponse(responseJson: JsonObject): String? {
        // Formato para /v1beta/interactions (Gemini 3.5)
        responseJson.getAsJsonArray("steps")?.let { stepsArray ->
            for (elem in stepsArray) {
                val stepObj = elem.asJsonObject
                if (stepObj.has("type") && stepObj.get("type").asString == "model_output") {
                    stepObj.getAsJsonArray("content")?.let { contentArray ->
                        for (contentElem in contentArray) {
                            val contentObj = contentElem.asJsonObject
                            if (contentObj.has("type") && contentObj.get("type").asString == "text" && contentObj.has("text")) {
                                return contentObj.get("text").asString
                            }
                        }
                    }
                }
            }
        }

        // Formatos legacy o alternativos por si acaso
        responseJson.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString?.let { return it }

        responseJson.get("text")?.asString?.let { return it }

        return null
    }

    private fun extractJsonFromText(text: String): String? {
        val markdownRegex = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```")
        markdownRegex.find(text)?.groupValues?.get(1)?.let { return it }

        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        if (startIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1)
        }
        return null
    }
}