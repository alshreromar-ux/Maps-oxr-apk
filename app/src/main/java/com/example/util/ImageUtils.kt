package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {
    private const val MAX_DIMENSION_PX = 480
    private const val JPEG_QUALITY = 58
    private const val MAX_BASE64_LENGTH = 110_000

    suspend fun encodeUriToBoundedBase64(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null

                val maxSide = max(original.width, original.height)
                val scaled = if (maxSide > MAX_DIMENSION_PX) {
                    val scale = MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
                    val targetW = (original.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (original.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(original, targetW, targetH, true)
                } else {
                    original
                }

                var quality = JPEG_QUALITY
                var encoded: String
                do {
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    quality -= 10
                } while (encoded.length > MAX_BASE64_LENGTH && quality >= 20)

                if (scaled !== original) scaled.recycle()
                original.recycle()

                if (encoded.length <= MAX_BASE64_LENGTH) encoded else null
            } catch (_: Exception) {
                null
            }
        }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
