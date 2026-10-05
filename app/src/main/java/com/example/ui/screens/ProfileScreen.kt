package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyEmblem
import com.example.ui.components.SocietyRed
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
  viewModel: SocietyViewModel,
  onSignedOut: () -> Unit,
  onNavigateToComplaints: () -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val userProfile by viewModel.userProfile.collectAsState()
  val isSubmitting by viewModel.isSubmitting.collectAsState()
  val effectiveRole = viewModel.effectiveRole()

  var nameInput by remember(userProfile?.name) { mutableStateOf(userProfile?.name ?: viewModel.currentUserName) }
  var flatInput by remember(userProfile?.flatNumber) { mutableStateOf(userProfile?.flatNumber ?: "Flat 101") }
  var mobileInput by remember(userProfile?.mobileNumber) { mutableStateOf(userProfile?.mobileNumber ?: "") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    // Society Emblem
    SocietyEmblem(size = 90.dp)

    Text(
      text = nameInput,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = if (effectiveRole == "admin") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
      Text(
        text = if (effectiveRole == "admin") "ADMINISTRATOR" else "SOCIETY RESIDENT",
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (effectiveRole == "admin") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
      )
    }

    // Role switcher card (Admin only)
    if (userProfile?.isAdmin == true) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Active App Mode: ${if (effectiveRole == "admin") "Admin" else "Resident"}",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
            Text(
              text = "Toggle between Resident and Committee Admin views",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          OutlinedButton(
            onClick = { viewModel.toggleRoleOverride() },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("profile_role_toggle_button")
          ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (effectiveRole == "admin") "Switch to Resident" else "Switch to Admin", fontSize = 11.sp)
          }
        }
      }
    }

    // Admin Complaints Management Card (Requested: Complaints in profile section for Admin only)
    if (effectiveRole == "admin") {
      val complaints by viewModel.complaints.collectAsState()
      val openCount = complaints.count { it.status == "open" }

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateToComplaints() }
          .testTag("admin_complaints_profile_card")
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFE65100)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.QuestionAnswer,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Society Complaints",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Color(0xFFBF360C)
                )
                if (openCount > 0) {
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFBF360C)
                  ) {
                    Text(
                      text = "$openCount OPEN",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.ExtraBold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Text(
                text = "${complaints.size} Total logged • Tap to review & chat",
                fontSize = 12.sp,
                color = Color(0xFF5D4037)
              )
            }
          }

          Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFBF360C),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Edit Resident Info Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Resident Information",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Your Full Name") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = flatInput,
          onValueChange = { input ->
            flatInput = input.filter { it.isDigit() }
          },
          label = { Text("House No. (Numbers Only) *") },
          placeholder = { Text("e.g. 114, 101, 204") },
          leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          supportingText = { Text("Enter house number (digits only). Must be unique.") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("profile_flat_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = mobileInput,
          onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            if (digits.length <= 10) mobileInput = digits
          },
          label = { Text("WhatsApp / Mobile Number *") },
          placeholder = { Text("10-digit mobile e.g. 9876543210") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          supportingText = { Text("Mandatory for receiving official WhatsApp maintenance receipts") },
          modifier = Modifier.fillMaxWidth().testTag("profile_mobile_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = viewModel.currentUserEmail,
          onValueChange = {},
          readOnly = true,
          label = { Text("Google Account Email") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            viewModel.updateProfile(
              name = nameInput,
              flatNumber = flatInput,
              role = userProfile?.role ?: "resident",
              mobileNumber = mobileInput
            )
          },
          enabled = !isSubmitting && nameInput.isNotBlank() && flatInput.isNotBlank() && mobileInput.length == 10,
          colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Saving...")
          } else {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Update Resident Profile")
          }
        }
      }
    }

    // Society Registration & Legal Info Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "हनुमान नगर विस्तार 1 विकास समिति",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = SocietyRed,
          textAlign = TextAlign.Center
        )
        Text(
          text = "Reg. No. COOP/2023/JAIPUR/205538",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 2.dp)
        )
        Text(
          text = "पता: प्लॉट न. 114, हनुमान नगर विस्तार 1, नियर बी.एड कॉलेज, निवार रोड, झोटवाड़ा, जयपुर (राज.)",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 16.sp,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }

    // App Version & Designer / Developer Credits Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = SocietyRed.copy(alpha = 0.12f)
        ) {
          Text(
            text = "Ver- 0.1",
            color = SocietyRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Developed and maintained by",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Rakesh Sharma",
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
          letterSpacing = 0.5.sp
        )
      }
    }

    // Google Sign Out Button
    OutlinedButton(
      onClick = {
        performSignOut(
          context = context,
          scope = coroutineScope,
          onComplete = onSignedOut
        )
      },
      shape = RoundedCornerShape(10.dp),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
    ) {
      Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Sign out of Society Portal")
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

private fun performSignOut(
  context: Context,
  scope: CoroutineScope,
  onComplete: () -> Unit
) {
  Firebase.auth.signOut()
  val credentialManager = CredentialManager.create(context)
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (_: Exception) {
    } finally {
      onComplete()
    }
  }
}
