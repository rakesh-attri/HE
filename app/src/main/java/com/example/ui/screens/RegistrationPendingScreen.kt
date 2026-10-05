package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyEmblem
import com.example.ui.components.SocietyLightAmber
import com.example.ui.components.SocietyRed
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RegistrationPendingScreen(
  viewModel: SocietyViewModel,
  onSignedOut: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by viewModel.userProfile.collectAsState()

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      SocietyEmblem(size = 96.dp)

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "हनुमान नगर विस्तार 1 विकास समिति",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = SocietyRed,
        textAlign = TextAlign.Center
      )
      Text(
        text = "Reg. No. COOP/2023/JAIPUR/205538",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
      )

      Spacer(modifier = Modifier.height(24.dp))

      val isBlocked = userProfile?.isBlocked == true

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isBlocked) Color(0xFFFFEBEE) else SocietyLightAmber),
        modifier = Modifier.fillMaxWidth().testTag("pending_approval_notice_card")
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(if (isBlocked) Color(0xFFC62828) else Color(0xFFE65100)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              if (isBlocked) Icons.Default.Block else Icons.Default.HourglassTop,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = if (isBlocked) "Account Blocked" else "Membership Under Verification",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (isBlocked) Color(0xFFB71C1C) else Color(0xFFBF360C),
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = if (isBlocked) {
              "Hello ${userProfile?.name ?: viewModel.currentUserName}!\nYour account for House No. ${userProfile?.flatNumber ?: ""} has been blocked by society administration. All payment processing and society features are locked."
            } else {
              "Hello ${userProfile?.name ?: viewModel.currentUserName}!\nYour registration for House No. ${userProfile?.flatNumber ?: ""} has been submitted to the Society Management Committee."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = if (isBlocked) {
              "Please visit the society office or contact the admin committee to reactivate your resident account."
            } else {
              "To ensure maximum safety & transparency for Hanuman Nagar Vikas Samiti, new resident profiles must be approved by society admins to enable payments and all services."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SocietyRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Office: Plot No. 114, Near B.Ed College, Niwar Road, Jhotwara, Jaipur",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = SocietyRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Helpline: +91 98290 12345 (Secretary)",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Sign Out / Try again
      OutlinedButton(
        onClick = {
          Firebase.auth.signOut()
          val cm = CredentialManager.create(context)
          coroutineScope.launch {
            try {
              cm.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
            onSignedOut()
          }
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
      ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out / Switch Account")
      }
    }
  }
}
