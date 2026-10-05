package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Job
import com.example.data.model.UserProfile
import java.io.File
import java.io.FileOutputStream

object PdfInvoiceGenerator {

    fun generateAndShareInvoicePdf(
        context: Context,
        job: Job,
        userProfile: UserProfile?
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width at 72 dpi
            val pageHeight = 842 // Standard A4 height at 72 dpi
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val darkText = Color.parseColor("#0F172A")
            val grayText = Color.parseColor("#64748B")
            val greenColor = Color.parseColor("#15803D")
            val redColor = Color.parseColor("#C2410C")

            // Background white
            canvas.drawColor(Color.WHITE)

            // Top Header Banner (Deep Indigo) - Extended slightly (-2f to 597f) to prevent any subpixel right-side gap
            val bannerPaint = Paint().apply {
                color = Color.parseColor("#1E3A8A")
            }
            canvas.drawRect(-2f, 0f, 597f, 125f, bannerPaint)

            // Accent stripe (Vibrant Blue)
            val accentPaint = Paint().apply {
                color = Color.parseColor("#0284C7")
            }
            canvas.drawRect(-2f, 125f, 597f, 133f, accentPaint)

            paint.color = Color.WHITE
            paint.textSize = 26f
            paint.isFakeBoldText = true
            canvas.drawText("TAX INVOICE", 40f, 55f, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            val bizName = userProfile?.businessName?.takeIf { it.isNotBlank() && it != "My Business" } ?: "Time Bill Business Services"
            val bizMobile = userProfile?.mobile?.takeIf { it.isNotBlank() } ?: ""
            canvas.drawText("Issued by: $bizName ${if (bizMobile.isNotEmpty()) "• $bizMobile" else ""}", 40f, 85f, paint)

            val invoiceNo = "INV-${job.jobId.takeLast(6).uppercase()}"
            canvas.drawText("Invoice No: $invoiceNo", 380f, 55f, paint)
            canvas.drawText("Date: ${job.date}", 380f, 75f, paint)
            val isPaid = job.pendingAmount <= 0
            canvas.drawText("Status: ${if (isPaid) "PAID" else "PENDING"}", 380f, 95f, paint)

            var currentY = 175f

            // Customer Details Card Box
            val boxPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val strokePaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(40f, currentY, 555f, currentY + 110f, 10f, 10f, boxPaint)
            canvas.drawRoundRect(40f, currentY, 555f, currentY + 110f, 10f, 10f, strokePaint)

            paint.color = Color.parseColor("#1E3A8A")
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("BILLED TO:", 60f, currentY + 28f, paint)

            paint.textSize = 13f
            paint.isFakeBoldText = false
            paint.color = darkText
            canvas.drawText("Customer Name: ${job.customerName}", 60f, currentY + 55f, paint)
            canvas.drawText("Service Performed: ${job.serviceName}", 60f, currentY + 80f, paint)
            if (job.notes.isNotBlank()) {
                canvas.drawText("Notes: ${job.notes}", 60f, currentY + 102f, paint)
            }

            currentY += 150f

            // Table Header Bar (Full width between 40f and 555f)
            val tableBg = Paint().apply { color = Color.parseColor("#E2E8F0") }
            canvas.drawRect(40f, currentY, 555f, currentY + 35f, tableBg)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("SERVICE DESCRIPTION", 55f, currentY + 22f, paint)
            canvas.drawText("DURATION / RATE", 280f, currentY + 22f, paint)
            canvas.drawText("AMOUNT (₹)", 440f, currentY + 22f, paint)

            currentY += 45f

            // Table Row 1
            paint.color = darkText
            paint.isFakeBoldText = false
            paint.textSize = 13f
            canvas.drawText(job.serviceName, 55f, currentY + 15f, paint)
            val hours = job.billableDurationMinutes / 60
            val mins = job.billableDurationMinutes % 60
            canvas.drawText("${hours}h ${mins}m @ ₹${formatIndianCurrency(job.rate)}/hr", 280f, currentY + 15f, paint)
            paint.isFakeBoldText = true
            canvas.drawText("₹${formatIndianCurrency(job.finalAmount)}", 440f, currentY + 15f, paint)

            currentY += 40f
            // Divider line
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 1f
            canvas.drawLine(40f, currentY, 555f, currentY, paint)

            currentY += 35f

            // Totals Section (Right aligned)
            val rightX = 330f
            val valX = 440f

            paint.color = grayText
            paint.textSize = 13f
            paint.isFakeBoldText = false
            canvas.drawText("Total Invoiced:", rightX, currentY, paint)
            paint.color = darkText
            paint.isFakeBoldText = true
            canvas.drawText("₹${formatIndianCurrency(job.finalAmount)}", valX, currentY, paint)

            currentY += 28f
            paint.color = grayText
            paint.isFakeBoldText = false
            canvas.drawText("Amount Paid:", rightX, currentY, paint)
            paint.color = greenColor
            paint.isFakeBoldText = true
            canvas.drawText("₹${formatIndianCurrency(job.paidAmount)}", valX, currentY, paint)

            currentY += 28f
            paint.color = grayText
            paint.isFakeBoldText = false
            canvas.drawText("Pending Balance:", rightX, currentY, paint)
            paint.color = if (job.pendingAmount > 0) redColor else greenColor
            paint.isFakeBoldText = true
            canvas.drawText("₹${formatIndianCurrency(job.pendingAmount)}", valX, currentY, paint)

            currentY += 45f

            // Status Badge Box
            val badgePaint = Paint().apply {
                color = if (isPaid) Color.parseColor("#DCFCE7") else Color.parseColor("#FFEDD5")
            }
            canvas.drawRoundRect(40f, currentY, 220f, currentY + 40f, 8f, 8f, badgePaint)
            paint.color = if (isPaid) Color.parseColor("#15803D") else Color.parseColor("#C2410C")
            paint.textSize = 13f
            paint.isFakeBoldText = true
            canvas.drawText(if (isPaid) "✔ PAYMENT COMPLETED" else "⏳ PENDING DUES", 55f, currentY + 25f, paint)

            // Footer
            val footerY = 770f
            paint.color = grayText
            paint.textSize = 11f
            paint.isFakeBoldText = false
            canvas.drawText("Thank you for your business! Generated via Time Bill App.", 40f, footerY, paint)
            val ownerName = userProfile?.name?.takeIf { it.isNotBlank() } ?: "Authorized Signatory"
            canvas.drawText(ownerName, 420f, footerY, paint)
            canvas.drawLine(415f, footerY - 15f, 555f, footerY - 15f, Paint().apply { color = Color.parseColor("#94A3B8"); strokeWidth = 1f })

            pdfDocument.finishPage(page)

            // Save PDF file to cache with customer name
            val sanitizedCustomerName = job.customerName.takeIf { it.isNotBlank() }?.replace(Regex("[^A-Za-z0-9_]"), "_") ?: "Customer"
            val file = File(context.cacheDir, "Invoice_${sanitizedCustomerName}_${job.jobId.takeLast(6)}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            // Share PDF via Intent
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${job.customerName}")
                putExtra(Intent.EXTRA_TEXT, "Hello ${job.customerName},\nHere is your professional invoice PDF for ${job.serviceName} amounting to ₹${formatIndianCurrency(job.finalAmount)}. Thank you!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
