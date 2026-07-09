package com.example.inventorycpm.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Utilidades para manipulación de imágenes antes de enviarlas a Gemini.
 */
object ImageUtil {

    /**
     * Copia un Uri (puede ser content:// de galería o file:// de CameraX) a un File
     * en el directorio de archivos internos de la app.
     * Retorna null si la operación falla.
     */
    fun copyUriToFile(context: Context, uri: Uri, destFileName: String): File? {
        return try {
            val destDir = context.filesDir.resolve("facturas_images").also { it.mkdirs() }
            val destFile = File(destDir, destFileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lee un File y lo codifica como Base64 (sin saltos de línea).
     * Retorna null si el archivo no existe o hay error de lectura.
     */
    fun fileToBase64(file: File): String? {
        return try {
            val bytes = file.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Determina el MIME type de una imagen basado en su extensión.
     */
    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }
}
