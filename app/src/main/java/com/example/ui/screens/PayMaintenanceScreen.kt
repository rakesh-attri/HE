package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyGreen
import com.example.ui.components.SocietyRed
import com.example.ui.components.StatusBadge
import com.example.ui.components.UpiQrCard
import com.example.ui.util.ReceiptShareUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayMaintenanceScreen(
  viewModel: SocietyViewModel
) {
  val context = LocalContext.current
  val settings by viewModel.societySettings.collectAsState()
  val residentMaintenance by viewModel.residentMaintenance.collectAsState()
  val isSubmitting by viewModel.isSubmitting.collectAsState()

  // Months generator for dropdown
  val monthsList = remember {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    val list = mutableListOf<String>()
    for (i in 0..5) {
      val tempCal = cal.clone() as Calendar
      tempCal.add(Calendar.MONTH, -i)
      list.add(sdf.format(tempCal.time))
    }
    list
  }

  var selectedMonth by remember { mutableStateOf(monthsList.firstOrNull() ?: "October 2026") }
  var monthDropdownExpanded by remember { mutableStateOf(false) }
  var utrInput by remember { mutableStateOf("") }
  var utrError by remember { mutableStateOf<String?>(null) }
  var receiptBase64 by remember { mutableStateOf("") }

  val receiptPicker = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      receiptBase64 = com.example.ui.util.ImageUtils.uriToBase64(context, uri, maxDimension = 500) ?: ""
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = "Pay Monthly Maintenance",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = SocietyRed
      )
      Text(
        text = "Scan society UPI QR code or pay to UPI ID, then auto-submit or enter UTR.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF616161)
      )
    }

    // QR Code and UPI ID Card
    item {
      UpiQrCard(
        upiId = settings.upiId,
        qrCodeUrl = settings.qrCodeImageUrl,
        societyName = settings.societyName,
        amount = settings.monthlyMaintenanceAmount
      )
    }

    // AUTO-GENERATE PAYMENT QUICK ACTION (Requested by user)
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth().testTag("auto_generate_payment_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SocietyGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Paid via QR code already?",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SocietyGreen
              )
              Text(
                text = "1-Tap Auto-Submit will create your payment record for $selectedMonth instantly!",
                fontSize = 12.sp,
                color = Color(0xFF2E7D32)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              viewModel.autoGenerateQrPayment(
                monthYear = selectedMonth,
                amount = settings.monthlyMaintenanceAmount
              )
            },
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("auto_submit_payment_button")
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Auto-Generate & Submit Payment Record")
          }
        }
      }
    }

    // Payment Submission Form Card (Manual UTR)
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Or Enter Bank UTR Reference Number",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Month Selection Dropdown
          ExposedDropdownMenuBox(
            expanded = monthDropdownExpanded,
            onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedTextField(
              value = selectedMonth,
              onValueChange = {},
              readOnly = true,
              label = { Text("Select Billing Month") },
              leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
                .testTag("month_select_field")
            )
            ExposedDropdownMenu(
              expanded = monthDropdownExpanded,
              onDismissRequest = { monthDropdownExpanded = false }
            ) {
              monthsList.forEach { month ->
                DropdownMenuItem(
                  text = { Text(month) },
                  onClick = {
                    selectedMonth = month
                    monthDropdownExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Fixed Amount display
          OutlinedTextField(
            value = "₹${settings.monthlyMaintenanceAmount.toInt()}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Maintenance Fee Amount") },
            leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(14.dp))

          // UTR Input
          OutlinedTextField(
            value = utrInput,
            onValueChange = {
              utrInput = it.filter { char -> char.isLetterOrDigit() || char == '-' }.uppercase()
              if (utrInput.length >= 4) utrError = null
            },
            label = { Text("UPI UTR / Reference Number *") },
            placeholder = { Text("e.g. 428901234567") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            isError = utrError != null,
            supportingText = {
              if (utrError != null) {
                Text(utrError ?: "", color = MaterialTheme.colorScheme.error)
              } else {
                Text("Found in GPay, PhonePe, Paytm or BHIM payment receipt")
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("utr_input_field")
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Optional Payment Screenshot Attachment (Requested by user)
          if (receiptBase64.isNotBlank()) {
            val bmp = remember(receiptBase64) { com.example.ui.util.ImageUtils.base64ToBitmap(receiptBase64) }
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().testTag("receipt_preview_card")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (bmp != null) {
                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Payment Screenshot",
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("Screenshot Attached", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text("Admin can view before approving", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { receiptBase64 = "" }) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove screenshot", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          } else {
            OutlinedButton(
              onClick = {
                receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("attach_receipt_button")
            ) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Attach Payment Screenshot (Optional)")
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = {
              if (utrInput.trim().length < 4) {
                utrError = "Please enter valid UTR / transaction reference"
                return@Button
              }
              viewModel.submitMaintenancePayment(
                monthYear = selectedMonth,
                amount = settings.monthlyMaintenanceAmount,
                utrNumber = utrInput.trim(),
                receiptImageUrl = receiptBase64
              )
              utrInput = ""
              receiptBase64 = ""
            },
            enabled = !isSubmitting && utrInput.trim().isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("submit_maintenance_button")
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Submitting...")
            } else {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Submit Reference for Verification")
            }
          }
        }
      }
    }

    // Payment History Section
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
      ) {
        Icon(Icons.Default.History, contentDescription = null, tint = SocietyRed, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Your Payment History",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF212121)
        )
      }
    }

    if (residentMaintenance.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFF9F9F9),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = SocietyRed)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "No maintenance records submitted yet. Pay via UPI above and submit your reference.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF616161)
            )
          }
        }
      }
    } else {
      items(residentMaintenance) { record ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth().testTag("payment_history_card_${record.id}")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = record.monthYear,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Color(0xFF212121)
                )
                Text(
                  text = "₹${record.amount.toInt()} • UTR: ${record.utrNumber}",
                  fontSize = 13.sp,
                  color = Color(0xFF424242)
                )
                record.createdAt?.toDate()?.let { date ->
                  val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(date)
                  Text(
                    text = "Submitted: $dateStr",
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                  )
                }
              }

              StatusBadge(status = record.status)
            }

            // Official PDF Receipt Action for Approved payments
            if (record.status.equals("approved", ignoreCase = true)) {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    ReceiptShareUtil.viewReceiptPdf(context, record, settings.societyName)
                  },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).height(36.dp).testTag("view_pdf_${record.id}")
                ) {
                  Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = SocietyRed)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("View PDF", fontSize = 12.sp)
                }

                Button(
                  onClick = {
                    ReceiptShareUtil.shareReceipt(context, record, settings.societyName)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1.3f).height(36.dp).testTag("share_pdf_${record.id}")
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Send PDF Receipt", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
