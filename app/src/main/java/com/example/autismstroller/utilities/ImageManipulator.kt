package com.example.autismstroller.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.autismstroller.R
import java.io.ByteArrayOutputStream

object ImageManipulator{
    fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if(base64String == null){return null}
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    fun vectorToBitmap(context: Context, imageId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, imageId) ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    fun imageToBitmap(context: Context, imageId: Int) : Bitmap{
        val drawable = ContextCompat.getDrawable(context, imageId) as BitmapDrawable
        return drawable.bitmap
    }

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resizeBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (source.width <= maxWidth && source.height <= maxHeight) return source
        val aspectRatio = source.width.toDouble() / source.height.toDouble()
        val targetWidth: Int
        val targetHeight: Int
        if (source.width > source.height) {
            targetWidth = maxWidth
            targetHeight = (maxWidth / aspectRatio).toInt()
        } else {
            targetHeight = maxHeight
            targetWidth = (maxHeight * aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }
}

