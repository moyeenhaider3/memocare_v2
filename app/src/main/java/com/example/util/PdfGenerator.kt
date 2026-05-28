package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.ConfirmationLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun generateAndSharePdf(context: Context, logs: List<ConfirmationLog>) {
        if (logs.isEmpty()) {
            return
        }

        // Calculations
        val doneCount = logs.count { it.action.uppercase(Locale.getDefault()) == "DONE" }
        val skippedCount = logs.count { it.action.uppercase(Locale.getDefault()) == "SKIPPED" }
        val snoozedCount = logs.count { it.action.uppercase(Locale.getDefault()) == "SNOOZED" }
        val totalActionable = doneCount + skippedCount
        val complianceRate = if (totalActionable > 0) {
            (doneCount.toFloat() / totalActionable * 100).toInt()
        } else 100

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        
        // Define paints
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A3A5C")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#4A5568")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#1A3A5C")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#2D3748")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val boldTextPaint = Paint().apply {
            color = Color.parseColor("#2D3748")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#718096")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val fillPrimaryPaint = Paint().apply {
            color = Color.parseColor("#1A3A5C")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val bannerPaint = Paint().apply {
            color = Color.parseColor("#F0F4F8")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val rowAltPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        // Status pill paints
        val doneFillPaint = Paint().apply { color = Color.parseColor("#DCFCE7") } // light green
        val doneTextPaint = Paint().apply {
            color = Color.parseColor("#15803D")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val skipFillPaint = Paint().apply { color = Color.parseColor("#FEE2E2") } // light red
        val skipTextPaint = Paint().apply {
            color = Color.parseColor("#B91C1C")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val snoozeFillPaint = Paint().apply { color = Color.parseColor("#FEF3C7") } // light orange/yellow
        val snoozeTextPaint = Paint().apply {
            color = Color.parseColor("#B45309")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val defaultFillPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        val defaultTextPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy HH:mm", Locale.getDefault())
        val generatedAtString = sdf.format(Date())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        // We can draw a beautiful logo shield:
        val logoX = 50f
        val logoY = 60f
        
        fun drawHeaderAndMetaData(canvas: Canvas, pageNum: Int) {
            // Draw a beautiful circular logo banner
            canvas.drawRoundRect(25f, 30f, 415f, 90f, 8f, 8f, bannerPaint)
            
            val iconPaint = Paint().apply {
                color = Color.parseColor("#27AE60")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(logoX + 10f, logoY, 15f, iconPaint)
            
            val iconCheckPaint = Paint().apply {
                color = Color.WHITE
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(logoX + 4f, logoY, logoX + 9f, logoY + 5f, iconCheckPaint)
            canvas.drawLine(logoX + 9f, logoY + 5f, logoX + 17f, logoY - 5f, iconCheckPaint)

            canvas.drawText("MEMOCARE COMPLIANCE JOURNAL", logoX + 40f, logoY - 4f, titlePaint.apply { textSize = 13f })
            canvas.drawText("Generated at: $generatedAtString", logoX + 40f, logoY + 14f, subTitlePaint)
            
            // Draw page number
            canvas.drawText("Page $pageNum", pageWidth - 75f, logoY + 5f, subTitlePaint)
            canvas.drawLine(35f, 100f, pageWidth - 35f, 100f, borderPaint)
        }

        fun drawFooter(canvas: Canvas) {
            canvas.drawLine(35f, pageHeight - 65f, pageWidth - 35f, pageHeight - 65f, borderPaint)
            
            val disclaimer2 = "MemoCare supports seamless routine tracking. Communicate with your care provider."
            canvas.drawText("MemoCare Wellness Companion - Confidential Report", 35f, pageHeight - 50f, footerPaint)
            canvas.drawText(disclaimer2, 35f, pageHeight - 38f, footerPaint)
        }

        // Draw page 1 headers
        drawHeaderAndMetaData(canvas, pageNumber)

        // Draw Summary Section
        var yPos = 125f
        
        // Draw standard summary block
        canvas.drawRoundRect(35f, yPos, pageWidth - 35f, yPos + 80f, 12f, 12f, fillPrimaryPaint)
        
        val summaryTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val summaryTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        canvas.drawText("PERFORMANCE INSIGHTS", 55f, yPos + 25f, summaryTitlePaint)
        canvas.drawText("Compliance Score: $complianceRate%", 55f, yPos + 45f, summaryTextPaint)
        canvas.drawText("Completed: $doneCount  |  Skipped: $skippedCount  |  Snoozed: $snoozedCount  |  Total Logs: ${logs.size}", 55f, yPos + 62f, summaryTextPaint)
        
        // Draw visual indicator: nice green progress ring representing success or star matching rate
        val ratingPaint = Paint().apply {
            color = Color.parseColor("#27AE60")
            textSize = 24f
        }
        canvas.drawText("🏆", pageWidth - 85f, yPos + 50f, ratingPaint)
        
        yPos += 105f

        // Table headers info
        canvas.drawText("CHRONOLOGICAL RECORD", 35f, yPos, headerPaint.apply { textSize = 11f })
        yPos += 12f
        canvas.drawLine(35f, yPos, pageWidth - 35f, yPos, borderPaint)
        yPos += 18f

        // Headers
        canvas.drawText("#", 45f, yPos, headerPaint.apply { textSize = 9f })
        canvas.drawText("Reminder Name", 85f, yPos, headerPaint)
        canvas.drawText("Action / Status", 325f, yPos, headerPaint)
        canvas.drawText("Logged Timestamp", 435f, yPos, headerPaint)

        yPos += 8f
        canvas.drawLine(35f, yPos, pageWidth - 35f, yPos, borderPaint)
        yPos += 18f

        val logSdf = SimpleDateFormat("EEEE, MMM dd, hh:mm a", Locale.getDefault())

        for (index in logs.indices) {
            val log = logs[index]
            
            // Check page overflow
            if (yPos > pageHeight - 95f) {
                drawFooter(canvas)
                pdfDocument.finishPage(currentPage)
                
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                
                // Draw header on new page
                drawHeaderAndMetaData(canvas, pageNumber)
                
                // Table header
                yPos = 130f
                canvas.drawText("#", 45f, yPos, headerPaint)
                canvas.drawText("Reminder Name", 85f, yPos, headerPaint)
                canvas.drawText("Action / Status", 325f, yPos, headerPaint)
                canvas.drawText("Logged Timestamp", 435f, yPos, headerPaint)
                yPos += 8f
                canvas.drawLine(35f, yPos, pageWidth - 35f, yPos, borderPaint)
                yPos += 18f
            }

            // Draw alternate rows
            if (index % 2 == 1) {
                canvas.drawRect(35f, yPos - 12f, pageWidth - 35f, yPos + 10f, rowAltPaint)
            }

            // Row values
            canvas.drawText((index + 1).toString(), 45f, yPos, boldTextPaint)
            
            // Handle long reminder names cleanly using truncation
            val displayName = if (log.reminderName.length > 32) {
                log.reminderName.substring(0, 29) + "..."
            } else {
                log.reminderName
            }
            canvas.drawText(displayName, 85f, yPos, textPaint)

            // Status Pill Draw Box
            val pillLeft = 325f
            val pillTop = yPos - 10f
            val pillRight = 385f
            val pillBottom = yPos + 4f
            
            val logAction = log.action.uppercase(Locale.getDefault())
            val (fillP, textP) = when (logAction) {
                "DONE" -> Pair(doneFillPaint, doneTextPaint)
                "SKIPPED" -> Pair(skipFillPaint, skipTextPaint)
                "SNOOZED" -> Pair(snoozeFillPaint, snoozeTextPaint)
                else -> Pair(defaultFillPaint, defaultTextPaint)
            }
            
            canvas.drawRoundRect(pillLeft, pillTop, pillRight, pillBottom, 4f, 4f, fillP)
            
            // Center status text inside pill
            val textWidth = textP.measureText(logAction)
            val pillWidth = pillRight - pillLeft
            val xOffset = (pillWidth - textWidth) / 2
            canvas.drawText(logAction, pillLeft + xOffset, yPos - 1f, textP)

            // Dynamic date/timestamp formatted
            val logTimeFormatted = logSdf.format(Date(log.actionedAt))
            canvas.drawText(logTimeFormatted, 435f, yPos, textPaint)

            yPos += 22f
        }

        drawFooter(canvas)
        pdfDocument.finishPage(currentPage)

        // Save PDF to App Cache directory and share it
        try {
            val cachePath = File(context.cacheDir, "compliance_pdfs")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            
            val pdfFile = File(cachePath, "memocare_compliance_export.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            
            // Share intent
            val pdfUri = FileProvider.getUriForFile(context, "io.github.moyeenhaider3.memocare.fileprovider", pdfFile)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TITLE, "MemoCare Compliance Report.pdf")
                putExtra(Intent.EXTRA_SUBJECT, "MemoCare Compliance Report Export")
                putExtra(Intent.EXTRA_TEXT, "Hello! Please find attached the professional MemoCare Medical Compliance Report detailing scheduled reminders, completed items, skipped tasks, and compliance rating.")
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Medical Report (PDF)"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error compiling report: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}
