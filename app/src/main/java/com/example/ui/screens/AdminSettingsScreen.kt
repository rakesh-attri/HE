package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import coil.compose.AsyncImage
import com.example.ui.SocietyViewModel
import com.example.ui.components.SimulatedQrCanvas
import com.example.ui.components.SocietyGreen
import com.example.ui.components.SocietyLightGreen
import com.example.ui.components.SocietyRed
import com.example.ui.util.ImageUtils

@Composable
fun AdminSettingsScreen(
  viewModel: SocietyViewModel
) {
  val context = LocalContext.current
  val settings by viewModel.societySettings.collectAsState()
  val isSubmitting by viewModel.isSubmitting.collectAsState()

  var upiIdInput by remember(settings.upiId) { mutableStateOf(settings.upiId) }
  var societyNameInput by remember(settings.societyName) { mutableStateOf(settings.societyName) }
  var qrUrlInput by remember(settings.qrCodeImageUrl) { mutableStateOf(settings.qrCodeImageUrl) }
  var amountInput by remember(settings.monthlyMaintenanceAmount) {
    mutableStateOf(settings.monthlyMaintenanceAmount.toInt().toString())
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      val base64 = ImageUtils.uriToBase64(context, uri)
      if (base64 != null) {
        qrUrlInput = base64
        Toast.makeText(context, "QR code image uploaded! Tap Save to apply.", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Failed to process selected image", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        Icons.Default.Settings,
        contentDescription = null,
        tint = SocietyRed,
        modifier = Modifier.size(28.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "QR & Society Settings",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = SocietyRed
        )
        Text(
          text = "Manage society UPI ID, QR code image, and fee amount",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Live QR Code Preview
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Live Resident QR Preview",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        val previewBitmap = remember(qrUrlInput) {
          if (qrUrlInput.startsWith("data:image") || (!qrUrlInput.startsWith("http") && qrUrlInput.length > 50)) {
            ImageUtils.base64ToBitmap(qrUrlInput)
          } else null
        }

        Box(
          modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(10.dp),
          contentAlignment = Alignment.Center
        ) {
          if (previewBitmap != null) {
            Image(
              bitmap = previewBitmap.asImageBitmap(),
              contentDescription = "QR Code Preview",
              modifier = Modifier.size(140.dp),
              contentScale = ContentScale.Fit
            )
          } else if (qrUrlInput.startsWith("http://") || qrUrlInput.startsWith("https://") || qrUrlInput.startsWith("content://")) {
            AsyncImage(
              model = qrUrlInput,
              contentDescription = "QR Code Preview",
              modifier = Modifier.size(140.dp),
              contentScale = ContentScale.Fit
            )
          } else {
            SimulatedQrCanvas(
              modifier = Modifier.size(140.dp),
              text = "upi://pay?pa=$upiIdInput&pn=$societyNameInput&am=$amountInput&cu=INR"
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (qrUrlInput.isNotBlank()) "Previewing custom uploaded QR image" else "Rendering fixed vector QR for: $upiIdInput",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }

    // Settings Input Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Society Payment Configuration",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = upiIdInput,
          onValueChange = { upiIdInput = it.trim() },
          label = { Text("Society Official UPI ID *") },
          placeholder = { Text("e.g. hanumannagar@upi or 9829012345@paytm") },
          leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_upi_id_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = societyNameInput,
          onValueChange = { societyNameInput = it },
          label = { Text("Society Registered Name *") },
          placeholder = { Text("हनुमान नगर विस्तार 1 विकास समिति") },
          leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_society_name_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // QR Code Upload Section (Replaced URL textfield with Photo Picker)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth().testTag("qr_upload_section")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Society Payment QR Code",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (qrUrlInput.isNotBlank()) "Custom uploaded QR code active" else "Default fixed society QR active",
                  fontSize = 12.sp,
                  color = if (qrUrlInput.isNotBlank()) SocietyGreen else Color(0xFF616161)
                )
              }
              if (qrUrlInput.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = SocietyLightGreen
                ) {
                  Text(
                    text = "CUSTOM UPLOADED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SocietyGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("upload_qr_button")
              ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (qrUrlInput.isNotBlank()) "Change QR Image" else "Upload QR Image", fontSize = 12.sp)
              }

              if (qrUrlInput.isNotBlank()) {
                OutlinedButton(
                  onClick = {
                    qrUrlInput = ""
                    Toast.makeText(context, "Reset to default fixed vector QR", Toast.LENGTH_SHORT).show()
                  },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.testTag("reset_qr_button")
                ) {
                  Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Reset to Fixed QR", fontSize = 12.sp)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = amountInput,
          onValueChange = { amountInput = it.filter { char -> char.isDigit() } },
          label = { Text("Standard Monthly Maintenance (₹)") },
          placeholder = { Text("1000") },
          leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_amount_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            val amount = amountInput.toDoubleOrNull() ?: 1000.0
            viewModel.updateSocietySettings(
              upiId = upiIdInput,
              societyName = societyNameInput,
              qrUrl = qrUrlInput,
              monthlyAmount = amount
            )
          },
          enabled = !isSubmitting && upiIdInput.isNotBlank() && societyNameInput.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("save_settings_button")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Saving...")
          } else {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save QR & Payment Settings")
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
