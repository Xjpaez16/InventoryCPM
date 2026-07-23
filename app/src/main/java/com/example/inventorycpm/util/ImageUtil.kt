package com.example.inventorycpm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Utilidades para manipulación de imágenes antes de enviarlas a Gemini.
 */
object ImageUtil {

    /**
     * Redimensiona una imagen si excede el tamaño máximo y la comprime.
     * Útil para no saturar la memoria y el ancho de banda con la IA.
     */
    fun resizeAndCompressImage(file: File, maxDimension: Int = 1024): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
