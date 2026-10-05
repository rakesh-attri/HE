package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.util.ImageUtils

val SocietyRed = Color(0xFFC62828)
val SocietyDarkRed = Color(0xFF8E0000)
val SocietyLightRed = Color(0xFFFFEBEE)
val SocietyGreen = Color(0xFF2E7D32)
val SocietyLightGreen = Color(0xFFE8F5E9)
val SocietyAmber = Color(0xFFF57F17)
val SocietyLightAmber = Color(0xFFFFF8E1)

@Composable
fun SocietyEmblem(
  modifier: Modifier = Modifier,
  size: Dp = 80.dp
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(Color.White)
      .border(2.dp, Color(0xFFFFB300), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_hanuman_circular_logo),
      contentDescription = "श्री हनुमान जी महाराज - हनुमान नगर विस्तार 1 समिति",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Fit
    )
  }
}

@Composable
fun StatusBadge(
  status: String,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor, icon) = when (status.lowercase()) {
    "approved", "paid" -> Triple(SocietyLightGreen, SocietyGreen, Icons.Default.CheckCircle)
    "pending" -> Triple(SocietyLightAmber, SocietyAmber, Icons.Default.HourglassTop)
    "resolved" -> Triple(SocietyLightGreen, SocietyGreen, Icons.Default.CheckCircle)
    "open" -> Triple(SocietyLightAmber, SocietyAmber, Icons.Default.Error)
    else -> Triple(SocietyLightRed, SocietyRed, Icons.Default.Error)
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = bgColor,
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = status.replaceFirstChar { it.uppercase() },
        color = textColor,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
      )
    }
  }
}

fun getCategoryIcon(category: String): ImageVector {
  return when (category.lowercase()) {
    "security" -> Icons.Default.Security
    "electricity" -> Icons.Default.Bolt
    "water", "water supply" -> Icons.Default.WaterDrop
    "cleaning", "sanitation" -> Icons.Default.CleaningServices
    "garden", "gardening" -> Icons.Default.LocalFlorist
    "maintenance", "repairs" -> Icons.Default.Build
    "events", "festivals" -> Icons.Default.Celebration
    else -> Icons.Default.AccountBalance
  }
}

@Composable
fun UpiQrCard(
  upiId: String,
  qrCodeUrl: String,
  societyName: String,
  amount: Double,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(20.dp)
    ) {
      Text(
        text = "Scan & Pay via any UPI App",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "BHIM • Google Pay • PhonePe • Paytm",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      // QR Code Display with safe Base64 decode and vector fallback
      val decodedBitmap = remember(qrCodeUrl) {
        if (qrCodeUrl.startsWith("data:image") || (!qrCodeUrl.startsWith("http") && qrCodeUrl.length > 50)) {
          ImageUtils.base64ToBitmap(qrCodeUrl)
        } else null
      }

      Box(
        modifier = Modifier
          .size(200.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color.White)
          .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        if (decodedBitmap != null) {
          Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = "Society UPI QR Code",
            modifier = Modifier.size(176.dp),
            contentScale = ContentScale.Fit
          )
        } else if (qrCodeUrl.startsWith("http://") || qrCodeUrl.startsWith("https://") || qrCodeUrl.startsWith("content://")) {
          AsyncImage(
            model = qrCodeUrl,
            contentDescription = "Society UPI QR Code",
            modifier = Modifier.size(176.dp),
            contentScale = ContentScale.Fit
          )
        } else {
          // Dynamic styled vector QR Canvas fallback
          SimulatedQrCanvas(
            modifier = Modifier.size(176.dp),
            text = "upi://pay?pa=$upiId&pn=${Uri.encode(societyName)}&am=$amount&cu=INR"
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // UPI ID display box
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Society UPI ID",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = upiId,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("UPI ID", upiId)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("copy_upi_button")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy")
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Open in UPI App button
      Button(
        onClick = {
          val uri = Uri.parse("upi://pay?pa=$upiId&pn=${Uri.encode(societyName)}&am=$amount&cu=INR")
          val intent = Intent(Intent.ACTION_VIEW, uri)
          try {
            context.startActivity(Intent.createChooser(intent, "Pay via UPI"))
          } catch (e: Exception) {
            Toast.makeText(context, "No UPI app found on device", Toast.LENGTH_SHORT).show()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("open_upi_app_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Pay ₹${amount.toInt()} with UPI App")
      }
    }
  }
}

@Composable
fun SimulatedQrCanvas(
  modifier: Modifier = Modifier,
  text: String
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val dark = Color(0xFF1E1E1E)
    val light = Color(0xFFFFFFFF)

    // Background
    drawRect(light)

    // Finder pattern helper
    fun drawFinder(x: Float, y: Float, boxSize: Float) {
      val inner1 = boxSize * (5f / 7f)
      val inner2 = boxSize * (3f / 7f)
      drawRoundRect(
        color = dark,
        topLeft = Offset(x, y),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = boxSize / 7f)
      )
      drawRoundRect(
        color = dark,
        topLeft = Offset(x + (boxSize - inner2) / 2f, y + (boxSize - inner2) / 2f),
        size = Size(inner2, inner2),
        cornerRadius = CornerRadius(4f, 4f)
      )
    }

    val finderSize = w * 0.28f
    drawFinder(w * 0.05f, h * 0.05f, finderSize)
    drawFinder(w * 0.67f, h * 0.05f, finderSize)
    drawFinder(w * 0.05f, h * 0.67f, finderSize)

    // Alignment pattern
    val alignSize = finderSize * 0.6f
    val ax = w * 0.70f
    val ay = h * 0.70f
    drawRect(
      color = dark,
      topLeft = Offset(ax, ay),
      size = Size(alignSize, alignSize),
      style = Stroke(width = 4f)
    )
    drawRect(
      color = dark,
      topLeft = Offset(ax + alignSize * 0.35f, ay + alignSize * 0.35f),
      size = Size(alignSize * 0.3f, alignSize * 0.3f)
    )

    // Dot grid based on string hash for deterministic authentic pattern
    val cols = 18
    val rows = 18
    val cellW = w / cols
    val cellH = h / rows
    val hash = text.hashCode()

    for (r in 0 until rows) {
      for (c in 0 until cols) {
        // Skip finder zones
        val inTopLeft = r < 7 && c < 7
        val inTopRight = r < 7 && c > 10
        val inBottomLeft = r > 10 && c < 7
        val inBottomRight = r > 11 && c > 11
        val inCenter = r in 8..9 && c in 8..9

        if (!inTopLeft && !inTopRight && !inBottomLeft && !inBottomRight && !inCenter) {
          val bit = ((hash xor (r * 31 + c * 17)) and (1 shl ((r + c) % 16))) != 0
          if (bit) {
            drawRoundRect(
              color = dark,
              topLeft = Offset(c * cellW + cellW * 0.1f, r * cellH + cellH * 0.1f),
              size = Size(cellW * 0.8f, cellH * 0.8f),
              cornerRadius = CornerRadius(3f, 3f)
            )
          }
        }
      }
    }

    // Center badge with Society Red dot
    val centerSize = w * 0.18f
    drawRoundRect(
      color = light,
      topLeft = Offset((w - centerSize) / 2f, (h - centerSize) / 2f),
      size = Size(centerSize, centerSize),
      cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
      color = SocietyRed,
      topLeft = Offset((w - centerSize * 0.8f) / 2f, (h - centerSize * 0.8f) / 2f),
      size = Size(centerSize * 0.8f, centerSize * 0.8f),
      cornerRadius = CornerRadius(6f, 6f)
    )
  }
}
