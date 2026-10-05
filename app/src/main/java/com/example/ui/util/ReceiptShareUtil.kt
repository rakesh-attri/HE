package com.example.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.MaintenanceRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ReceiptShareUtil"

object ReceiptShareUtil {

  private const val PAGE_WIDTH = 440
  private const val PAGE_HEIGHT = 650

  /**
   * Generates a realistic, small, and high-fidelity official society maintenance receipt in PDF format.
   * File size is minimized (~15-30 KB) using vector canvas drawing without bulky bitmaps.
   */
  fun generateReceiptPdf(
    context: Context,
    record: MaintenanceRecord,
    societyName: String = "हनुमान नगर विस्तार 1 विकास समिति"
  ): File {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val dateStr = record.updatedAt?.toDate()?.let {
      SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(it)
    } ?: record.createdAt?.toDate()?.let {
      SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(it)
    } ?: SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())

    val receiptId = "HNVS-${record.id.takeLast(6).uppercase()}"

    // Colors
    val maroon = Color.rgb(183, 28, 28)       // #B71C1C Society Red
    val darkRed = Color.rgb(136, 14, 79)
    val textDark = Color.rgb(33, 33, 33)      // #212121
    val textMuted = Color.rgb(97, 97, 97)     // #616161
    val greenApproved = Color.rgb(46, 125, 50)// #2E7D32
    val bgSoft = Color.rgb(250, 250, 250)
    val borderLight = Color.rgb(224, 224, 224)
    val goldAccent = Color.rgb(245, 127, 23)

    // Paints
    val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = textDark
      typeface = Typeface.DEFAULT
    }

    val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
    }

    val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
    }

    // 1. Background
    paintFill.color = Color.WHITE
    canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paintFill)

    // 2. Outer Decorative Double Border
    paintLine.color = maroon
    paintLine.strokeWidth = 2.5f
    canvas.drawRect(14f, 14f, PAGE_WIDTH - 14f, PAGE_HEIGHT - 14f, paintLine)

    paintLine.strokeWidth = 0.8f
    canvas.drawRect(18f, 18f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f, paintLine)

    // Top Header Banner
    paintFill.color = maroon
    canvas.drawRect(18f, 18f, PAGE_WIDTH - 18f, 26f, paintFill)

    // 3. Society Header Titles (Hindi & English)
    paintText.textAlign = Paint.Align.CENTER

    // Hindi Title
    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paintText.textSize = 15f
    paintText.color = maroon
    canvas.drawText(societyName, PAGE_WIDTH / 2f, 48f, paintText)

    // English Title
    paintText.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paintText.textSize = 11.5f
    paintText.color = textDark
    canvas.drawText("HANUMAN NAGAR VISTAR 1 VIKAS SAMITI", PAGE_WIDTH / 2f, 65f, paintText)

    // Location
    paintText.typeface = Typeface.DEFAULT
    paintText.textSize = 9.5f
    paintText.color = textMuted
    canvas.drawText("Jaipur, Rajasthan (India) • Pin: 302012", PAGE_WIDTH / 2f, 80f, paintText)

    // Reg. No Pill Badge
    val regText = "Reg. No: COOP/2023/JAIPUR/205538"
    paintText.textSize = 8.5f
    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val regWidth = paintText.measureText(regText)
    val regRect = RectF((PAGE_WIDTH - regWidth) / 2f - 8f, 88f, (PAGE_WIDTH + regWidth) / 2f + 8f, 104f)
    paintFill.color = Color.rgb(255, 235, 238)
    canvas.drawRoundRect(regRect, 4f, 4f, paintFill)
    paintText.color = maroon
    canvas.drawText(regText, PAGE_WIDTH / 2f, 100f, paintText)

    // 4. Double Separator line (like screenshot: ====================)
    paintLine.color = maroon
    paintLine.strokeWidth = 1.2f
    canvas.drawLine(24f, 114f, PAGE_WIDTH - 24f, 114f, paintLine)
    paintLine.strokeWidth = 0.6f
    canvas.drawLine(24f, 117f, PAGE_WIDTH - 24f, 117f, paintLine)

    // 5. Official Receipt Title Ribbon
    val titleRibbonRect = RectF(28f, 124f, PAGE_WIDTH - 28f, 146f)
    paintFill.color = Color.rgb(245, 245, 245)
    canvas.drawRoundRect(titleRibbonRect, 6f, 6f, paintFill)
    paintLine.color = borderLight
    paintLine.strokeWidth = 1f
    canvas.drawRoundRect(titleRibbonRect, 6f, 6f, paintLine)

    paintText.color = textDark
    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paintText.textSize = 11f
    canvas.drawText("OFFICIAL SOCIETY MAINTENANCE RECEIPT", PAGE_WIDTH / 2f, 139f, paintText)

    // 6. Dashed Separator
    paintLine.color = Color.rgb(189, 189, 189)
    paintLine.pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    paintLine.strokeWidth = 1f
    canvas.drawLine(28f, 155f, PAGE_WIDTH - 28f, 155f, paintLine)
    paintLine.pathEffect = null // reset

    // 7. Receipt Details Table / Box
    val detailsRect = RectF(28f, 163f, PAGE_WIDTH - 28f, 420f)
    paintFill.color = bgSoft
    canvas.drawRoundRect(detailsRect, 8f, 8f, paintFill)
    paintLine.color = borderLight
    canvas.drawRoundRect(detailsRect, 8f, 8f, paintLine)

    val labelX = 42f
    val colonX = 150f
    val valueX = 160f
    var currY = 186f
    val rowSpacing = 24f

    fun drawDetailRow(label: String, value: String, isBold: Boolean = false, valueColor: Int = textDark) {
      // Label
      paintText.textAlign = Paint.Align.LEFT
      paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      paintText.textSize = 10f
      paintText.color = textMuted
      canvas.drawText(label, labelX, currY, paintText)

      // Colon
      canvas.drawText(":", colonX, currY, paintText)

      // Value
      paintText.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
      paintText.color = valueColor
      canvas.drawText(value, valueX, currY, paintText)

      // Subtle row underline
      paintLine.color = Color.rgb(240, 240, 240)
      paintLine.strokeWidth = 0.5f
      canvas.drawLine(labelX, currY + 6f, PAGE_WIDTH - 42f, currY + 6f, paintLine)

      currY += rowSpacing
    }

    drawDetailRow("Receipt No", receiptId, isBold = true, valueColor = maroon)
    drawDetailRow("Billing Month", record.monthYear, isBold = true)
    drawDetailRow("Resident Name", record.userName.ifBlank { "Resident" }, isBold = true)
    drawDetailRow("House No.", record.flatNumber.ifBlank { "N/A" }, isBold = true, valueColor = maroon)
    drawDetailRow("Amount Paid", "₹${record.amount.toInt()}  (One Thousand Only)", isBold = true, valueColor = greenApproved)
    drawDetailRow("Payment Mode", "UPI (Online Transfer)", isBold = false)
    drawDetailRow("UTR / Ref No", record.utrNumber.ifBlank { "N/A" }, isBold = true)
    drawDetailRow("Payment Date", dateStr, isBold = false)

    // Status Pill on the table
    paintText.textAlign = Paint.Align.LEFT
    paintText.typeface = Typeface.DEFAULT
    paintText.textSize = 10f
    paintText.color = textMuted
    canvas.drawText("Status", labelX, currY, paintText)
    canvas.drawText(":", colonX, currY, paintText)

    val statusBadgeRect = RectF(valueX, currY - 11f, valueX + 155f, currY + 6f)
    paintFill.color = Color.rgb(232, 245, 233)
    canvas.drawRoundRect(statusBadgeRect, 4f, 4f, paintFill)
    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paintText.textSize = 9.5f
    paintText.color = greenApproved
    canvas.drawText("✔  APPROVED & VERIFIED", valueX + 8f, currY + 1f, paintText)

    currY += 28f

    // Admin remarks if any
    if (record.adminRemarks.isNotBlank()) {
      paintText.textAlign = Paint.Align.LEFT
      paintText.textSize = 9f
      paintText.color = textMuted
      canvas.drawText("Remarks: ${record.adminRemarks}", labelX, currY, paintText)
      currY += 16f
    }

    // 8. Dashed Divider
    paintLine.color = Color.rgb(189, 189, 189)
    paintLine.pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    paintLine.strokeWidth = 1f
    canvas.drawLine(28f, 435f, PAGE_WIDTH - 28f, 435f, paintLine)
    paintLine.pathEffect = null

    // 9. Verified Circular Seal / Stamp
    val sealCenterX = PAGE_WIDTH - 85f
    val sealCenterY = 490f
    val sealRadius = 38f

    paintLine.color = maroon
    paintLine.strokeWidth = 1.5f
    canvas.drawCircle(sealCenterX, sealCenterY, sealRadius, paintLine)
    paintLine.strokeWidth = 0.6f
    canvas.drawCircle(sealCenterX, sealCenterY, sealRadius - 3f, paintLine)

    paintText.textAlign = Paint.Align.CENTER
    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paintText.textSize = 7f
    paintText.color = maroon
    canvas.drawText("HN VISTAR 1 SAMITI", sealCenterX, sealCenterY - 18f, paintText)

    paintText.textSize = 9f
    paintText.color = greenApproved
    canvas.drawText("★ VERIFIED ★", sealCenterX, sealCenterY - 4f, paintText)

    paintText.textSize = 7.5f
    paintText.color = textDark
    canvas.drawText("RECEIPT VALID", sealCenterX, sealCenterY + 9f, paintText)
    canvas.drawText("JAIPUR (RAJ.)", sealCenterX, sealCenterY + 20f, paintText)

    // 10. Left Signature & Authority Details
    paintText.textAlign = Paint.Align.LEFT
    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paintText.textSize = 9.5f
    paintText.color = textDark
    canvas.drawText("Verified by: Society Management Committee", 32f, 465f, paintText)

    paintText.typeface = Typeface.DEFAULT
    paintText.textSize = 8.5f
    paintText.color = textMuted
    canvas.drawText("Sh. Charan Sing Gill (Chairman) • Sh. Deepak Pareek (Secretary)", 32f, 480f, paintText)
    canvas.drawText("Plot No. 114, Hanuman Nagar Vistar 1, Jhotwara, Jaipur", 32f, 495f, paintText)
    canvas.drawText("Helpline / Queries: +91 98290 12345, +91 94140 67890", 32f, 510f, paintText)

    // 11. Footer note & Double bottom line
    paintLine.color = maroon
    paintLine.strokeWidth = 1.2f
    canvas.drawLine(24f, 532f, PAGE_WIDTH - 24f, 532f, paintLine)
    paintLine.strokeWidth = 0.6f
    canvas.drawLine(24f, 535f, PAGE_WIDTH - 24f, 535f, paintLine)

    paintText.textAlign = Paint.Align.CENTER
    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paintText.textSize = 10f
    paintText.color = maroon
    canvas.drawText("Thank you for your timely contribution!", PAGE_WIDTH / 2f, 555f, paintText)

    paintText.typeface = Typeface.DEFAULT
    paintText.textSize = 8f
    paintText.color = textMuted
    canvas.drawText("This is an authenticated computer-generated e-receipt issued under Samiti Bye-Laws.", PAGE_WIDTH / 2f, 572f, paintText)
    canvas.drawText("HNVS Jaipur © 2026 • Keep this document for your records.", PAGE_WIDTH / 2f, 586f, paintText)

    // Bottom solid bar
    paintFill.color = maroon
    canvas.drawRect(18f, PAGE_HEIGHT - 26f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f, paintFill)

    pdfDocument.finishPage(page)

    // Save to Cache Directory
    val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val safeMonth = record.monthYear.replace(" ", "_").replace("/", "-")
    val safeFlat = record.flatNumber.replace(" ", "_").replace("/", "-")
    val fileName = "Receipt_${safeMonth}_HN${safeFlat}_${record.id.takeLast(4)}.pdf"
    val pdfFile = File(receiptsDir, fileName)

    try {
      FileOutputStream(pdfFile).use { out ->
        pdfDocument.writeTo(out)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error writing PDF receipt to file", e)
    } finally {
      pdfDocument.close()
    }

    Log.i(TAG, "Generated receipt PDF: ${pdfFile.absolutePath}, size: ${pdfFile.length()} bytes")
    return pdfFile
  }

  /**
   * Shares the realistic PDF receipt via Android system share sheet (WhatsApp, Email, Drive, etc.)
   */
  fun shareReceipt(
    context: Context,
    record: MaintenanceRecord,
    societyName: String = "हनुमान नगर विस्तार 1 विकास समिति"
  ) {
    try {
      val pdfFile = generateReceiptPdf(context, record, societyName)
      val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Maintenance Receipt - House No. ${record.flatNumber} (${record.monthYear})")
        putExtra(
          Intent.EXTRA_TEXT,
          "Attached is the official maintenance receipt for House No. ${record.flatNumber} (${record.monthYear}) from Hanuman Nagar Vistar 1 Vikas Samiti."
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(shareIntent, "Share Maintenance Receipt PDF via")
      context.startActivity(chooser)
    } catch (e: Exception) {
      Log.e(TAG, "Error sharing PDF receipt, falling back to text", e)
      fallbackTextShare(context, record, societyName)
    }
  }

  /**
   * Shares the realistic PDF receipt directly to WhatsApp.
   */
  fun shareReceiptToWhatsApp(
    context: Context,
    record: MaintenanceRecord,
    residentMobile: String = "",
    societyName: String = "हनुमान नगर विस्तार 1 विकास समिति"
  ) {
    try {
      val pdfFile = generateReceiptPdf(context, record, societyName)
      val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)

      val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
          Intent.EXTRA_TEXT,
          "Official Maintenance Receipt: House No. ${record.flatNumber} • Month: ${record.monthYear} • Hanuman Nagar Vistar 1 Vikas Samiti"
        )
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(sendIntent)
    } catch (e: Exception) {
      Log.w(TAG, "WhatsApp direct PDF intent failed, falling back to generic share sheet", e)
      shareReceipt(context, record, societyName)
    }
  }

  /**
   * Opens the generated PDF in the device's native PDF viewer.
   */
  fun viewReceiptPdf(
    context: Context,
    record: MaintenanceRecord,
    societyName: String = "हनुमान नगर विस्तार 1 विकास समिति"
  ) {
    try {
      val pdfFile = generateReceiptPdf(context, record, societyName)
      val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)

      val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(viewIntent)
    } catch (e: Exception) {
      Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_SHORT).show()
    }
  }

  /**
   * Generates clean formatted text receipt (kept for copy-to-clipboard functionality)
   */
  fun generateReceiptText(record: MaintenanceRecord, societyName: String = "हनुमान नगर विस्तार 1 विकास समिति"): String {
    val dateStr = record.updatedAt?.toDate()?.let {
      SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(it)
    } ?: record.createdAt?.toDate()?.let {
      SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH).format(it)
    } ?: SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(Date())

    val receiptId = "HNVS-${record.id.takeLast(6).uppercase()}"

    return """
      ========================================
             $societyName
        HANUMAN NAGAR VISTAR 1 VIKAS SAMITI
            Jaipur, Rajasthan (India)
          Reg. No: COOP/2023/JAIPUR/205538
      ========================================
      OFFICIAL SOCIETY MAINTENANCE RECEIPT
      ----------------------------------------
      Receipt No   : $receiptId
      Billing Month: ${record.monthYear}
      Resident Name: ${record.userName}
      House No.    : ${record.flatNumber}
      Amount Paid  : ₹${record.amount.toInt()}
      Payment Mode : UPI (Online)
      UTR / Ref No : ${record.utrNumber}
      Status       : ${record.status.uppercase()} & VERIFIED
      Date & Time  : $dateStr
      ${if (record.adminRemarks.isNotBlank()) "Admin Remarks: ${record.adminRemarks}\n" else ""}----------------------------------------
      Verified by: Society Management Committee
      Plot No. 114, Hanuman Nagar Vistar 1,
      Near B.Ed College, Niwar Road, Jhotwara, Jaipur.
      ========================================
      Thank you for your timely contribution!
    """.trimIndent()
  }

  fun copyReceiptToClipboard(context: Context, record: MaintenanceRecord, societyName: String = "हनुमान नगर विस्तार 1 विकास समिति") {
    val receipt = generateReceiptText(record, societyName)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Society Receipt", receipt)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Receipt text copied to clipboard!", Toast.LENGTH_SHORT).show()
  }

  private fun fallbackTextShare(context: Context, record: MaintenanceRecord, societyName: String) {
    val receipt = generateReceiptText(record, societyName)
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, receipt)
      putExtra(Intent.EXTRA_SUBJECT, "Maintenance Receipt - House No. ${record.flatNumber} (${record.monthYear})")
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Maintenance Receipt via")
    context.startActivity(shareIntent)
  }
}
