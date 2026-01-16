package com.example.autismstroller.utilities

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.autismstroller.models.Child
import com.example.autismstroller.models.Song
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfUtility {

    fun generateAndSavePdf(context: Context, child: Child, allSongs: List<Song>) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // 1. Create Page (A4 size: 595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // 2. Define Styling
        titlePaint.textSize = 24f
        titlePaint.isFakeBoldText = true
        titlePaint.color = Color.BLACK
        titlePaint.textAlign = Paint.Align.CENTER

        paint.textSize = 12f
        paint.color = Color.BLACK

        // 3. Draw Header
        canvas.drawText("Sensory Stroller Report", 297f, 50f, titlePaint) // Center

        paint.isFakeBoldText = true
        canvas.drawText("Child Name: ${child.name}", 50f, 90f, paint)
        canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 50f, 110f, paint)

        // 4. Draw Overview Box
        drawSectionHeader(canvas, "Session Overview", 140f, paint)
        val stats = child.childStats
        paint.isFakeBoldText = false
        var y = 160f
        canvas.drawText("• Total Distance: ${"%.2f".format(stats.totalDistanceTravelled)} meters", 60f, y, paint)
        canvas.drawText("• Total Time: ${stats.totalTimeInStrollerMinutes} minutes", 300f, y, paint)
        y += 20f

        val avgTemp = if(stats.sensorReadingCount > 0) stats.tempExposureSum / stats.sensorReadingCount else 0.0
        val avgCo = if(stats.sensorReadingCount > 0) stats.coExposureSum / stats.sensorReadingCount else 0.0

        canvas.drawText("• Avg Temperature: ${"%.1f".format(avgTemp)}°C", 60f, y, paint)
        canvas.drawText("• Avg CO Level: ${"%.1f".format(avgCo)} ppm", 300f, y, paint)

        // 5. Draw Music Stats
        y += 40f
        drawSectionHeader(canvas, "Top Music Preferences", y, paint)
        y += 20f
        val topSongs = stats.musicPreferences.entries.sortedByDescending { it.value }.take(5)
        if (topSongs.isEmpty()) {
            canvas.drawText("(No music data recorded)", 60f, y, paint)
        } else {
            topSongs.forEach { (url, count) ->
                val songName = allSongs.find { it.url == url }?.name ?: "Unknown Song"
                canvas.drawText("- $songName: Played $count times", 60f, y, paint)
                y += 18f
            }
        }

        // 6. Draw Light Stats
        y += 30f
        drawSectionHeader(canvas, "Color Preferences", y, paint)
        y += 20f
        stats.colorPreferences.forEach { (color, count) ->
            canvas.drawText("- $color Light: Used $count times", 60f, y, paint)
            y += 18f
        }

        // 7. Draw Time Stats
        y += 30f
        drawSectionHeader(canvas, "Active Times", y, paint)
        y += 20f
        val times = listOf("Morning", "Afternoon", "Evening", "Night")
        times.forEach { time ->
            val count = stats.timeOfDayUsage[time] ?: 0
            if (count > 0) {
                canvas.drawText("- $time: $count sessions", 60f, y, paint)
                y += 18f
            }
        }

        // Finish Page
        pdfDocument.finishPage(page)

        // 8. Save File
        savePdfToStorage(context, pdfDocument, "Stroller_Report_${child.name}_${System.currentTimeMillis()}.pdf")
    }

    private fun drawSectionHeader(canvas: Canvas, text: String, y: Float, paint: Paint) {
        val originalSize = paint.textSize
        val originalBold = paint.isFakeBoldText

        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = 0xFF239CA1.toInt() // Teal Color

        canvas.drawText(text, 50f, y, paint)

        // Reset
        paint.textSize = originalSize
        paint.isFakeBoldText = originalBold
        paint.color = Color.BLACK
    }

    private fun savePdfToStorage(context: Context, document: PdfDocument, fileName: String) {
        try {
            val outputStream: OutputStream?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                // Android 9 and below
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                outputStream = FileOutputStream(file)
            }

            if (outputStream != null) {
                document.writeTo(outputStream)
                outputStream.close()
                Toast.makeText(context, "PDF Saved to Downloads!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            document.close()
        }
    }
}