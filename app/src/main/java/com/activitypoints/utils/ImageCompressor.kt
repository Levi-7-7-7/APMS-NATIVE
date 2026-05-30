package com.activitypoints.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compresses an image URI to a temporary JPEG file ≤ MAX_SIZE_MB.
 * Mirrors the RN app's ImageResizer logic (RESIZE_WIDTH=1200, RESIZE_HEIGHT=1600).
 */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val MAX_WIDTH    = 1200
        private const val MAX_HEIGHT   = 1600
        private const val MAX_SIZE_MB  = 3L
        private const val QUALITY_STEP = 10
    }

    fun compress(uri: Uri): File? = try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original    = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Scale down while keeping aspect ratio
        val scaled = scaleBitmap(original, MAX_WIDTH, MAX_HEIGHT)

        val outFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        var quality = 90

        // Reduce quality until under MAX_SIZE_MB
        do {
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
            quality -= QUALITY_STEP
        } while (outFile.length() > MAX_SIZE_MB * 1024 * 1024 && quality > 10)

        scaled.recycle()
        original.recycle()
        outFile
    } catch (e: Exception) {
        null
    }

    private fun scaleBitmap(src: Bitmap, maxW: Int, maxH: Int): Bitmap {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        if (srcW <= maxW && srcH <= maxH) return src
        val scale = minOf(maxW / srcW, maxH / srcH)
        return Bitmap.createScaledBitmap(src, (srcW * scale).toInt(), (srcH * scale).toInt(), true)
    }
}
