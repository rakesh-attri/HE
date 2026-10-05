package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {
  fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 600): String? {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val originalBitmap = BitmapFactory.decodeStream(inputStream)
      inputStream.close()
      if (originalBitmap == null) return null

      val width = originalBitmap.width
      val height = originalBitmap.height
      val scale = max(1f, max(width.toFloat() / maxDimension, height.toFloat() / maxDimension))
      val targetW = (width / scale).toInt().coerceAtLeast(1)
      val targetH = (height / scale).toInt().coerceAtLeast(1)

      val resized = Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
      val outputStream = ByteArrayOutputStream()
      resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
      val bytes = outputStream.toByteArray()
      val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
      "data:image/jpeg;base64,$base64"
    } catch (e: Exception) {
      null
    }
  }

  fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = 480, quality: Int = 70): String? {
    return try {
      val width = bitmap.width
      val height = bitmap.height
      val scale = max(1f, max(width.toFloat() / maxDimension, height.toFloat() / maxDimension))
      val targetW = (width / scale).toInt().coerceAtLeast(1)
      val targetH = (height / scale).toInt().coerceAtLeast(1)

      val resized = if (targetW != width || targetH != height) {
        Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
      } else {
        bitmap
      }
      val outputStream = ByteArrayOutputStream()
      resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      val bytes = outputStream.toByteArray()
      val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
      "data:image/jpeg;base64,$base64"
    } catch (e: Exception) {
      null
    }
  }

  fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
      val pureBase64 = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
      val clean = pureBase64.trim().replace("\n", "").replace("\r", "")
      val bytes = Base64.decode(clean, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
      null
    }
  }
}
