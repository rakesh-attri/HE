package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.SocietyViewModel
import com.example.ui.UiMessage
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.NotificationCenterDialog
import com.example.ui.components.SocietyRed

enum class SocietyTab(
  val label: String,
  val icon: ImageVector
) {
  HOME("Home", Icons.Default.Dashboard),
  PAY("Pay Dues", Icons.Default.Payment),
  APPROVALS("Approvals", Icons.Default.AssignmentTurnedIn),
  MEMBERS("Members", Icons.Default.Group),
  EXPENSES("Expenses", Icons.Default.ReceiptLong),
  COMPLAINTS("Complaints", Icons.Default.ReportProblem),
  QR_SETTINGS("QR Sett.", Icons.Default.QrCode),
  PROFILE("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  viewModel: SocietyViewModel,
  onSignedOut: () -> Unit
) {
  val userProfile by viewModel.userProfile.collectAsState()
  val pendingMaintenance by viewModel.pendingMaintenance.collectAsState()
  val allUsers by viewModel.allUsers.collectAsState()
  val uiMessage by viewModel.uiMessage.collectAsState()
  val effectiveRole = viewModel.effectiveRole()
  val snackbarHostState = remember { SnackbarHostState() }

  // Check if non-admin user registration is pending approval
  if (userProfile != null && !userProfile!!.isApproved && effectiveRole != "admin") {
    RegistrationPendingScreen(
      viewModel = viewModel,
      onSignedOut = onSignedOut
    )
    return
  }

  var selectedTab by remember { mutableStateOf(SocietyTab.HOME) }
  val notifications by viewModel.notifications.collectAsState()
  val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()
  var showNotificationsDialog by remember { mutableStateOf(false) }

  // Runtime Permission request for POST_NOTIFICATIONS (Android 13+)
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ ->
    // Granted or dismissed
  }

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  LaunchedEffect(uiMessage) {
    uiMessage?.let { msg ->
      when (msg) {
        is UiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
        is UiMessage.Error -> snackbarHostState.showSnackbar("Error: ${msg.message}")
      }
      viewModel.clearMessage()
    }
  }

  val pendingApprovalsCount = pendingMaintenance.size
  val pendingMembersCount = allUsers.count { it.approvalStatus == "pending" }
  val complaints by viewModel.complaints.collectAsState()
  val openComplaintsCount = complaints.count { it.status == "open" }

  // Define tabs dynamically based on effectiveRole
  val tabs = if (effectiveRole == "admin") {
    listOf(
      SocietyTab.HOME,
      SocietyTab.APPROVALS,
      SocietyTab.MEMBERS,
      SocietyTab.EXPENSES,
      SocietyTab.QR_SETTINGS,
      SocietyTab.PROFILE
    )
  } else {
    listOf(
      SocietyTab.HOME,
      SocietyTab.PAY,
      SocietyTab.COMPLAINTS,
      SocietyTab.EXPENSES,
      SocietyTab.PROFILE
    )
  }

  // Ensure selected tab is valid for current role (Allow Complaints for admin when accessed via Profile or Home)
  if (!tabs.contains(selectedTab) && selectedTab != SocietyTab.COMPLAINTS) {
    selectedTab = SocietyTab.HOME
  }

  BackHandler(enabled = selectedTab == SocietyTab.COMPLAINTS && effectiveRole == "admin") {
    selectedTab = SocietyTab.PROFILE
  }

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (selectedTab == SocietyTab.COMPLAINTS && effectiveRole == "admin") {
            IconButton(onClick = { selectedTab = SocietyTab.PROFILE }) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Profile",
                tint = Color.White
              )
            }
          }
        },
        title = {
          Text(
            text = when (selectedTab) {
              SocietyTab.HOME -> "HN Vistar 1 Samiti"
              SocietyTab.PAY -> "Pay Maintenance"
              SocietyTab.APPROVALS -> "Payment Approvals"
              SocietyTab.MEMBERS -> "Members & Roles"
              SocietyTab.EXPENSES -> "Transparency Ledger"
              SocietyTab.COMPLAINTS -> "Society Complaints"
              SocietyTab.QR_SETTINGS -> "UPI & QR Settings"
              SocietyTab.PROFILE -> "Resident Profile"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
          )
        },
        actions = {
          NotificationBellButton(
            unreadCount = unreadNotificationsCount,
            onClick = { showNotificationsDialog = true }
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SocietyRed,
          titleContentColor = Color.White,
          actionIconContentColor = Color.White
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
      ) {
        tabs.forEach { tab ->
          val isSelected = selectedTab == tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTab = tab },
            icon = {
              if (tab == SocietyTab.APPROVALS && pendingApprovalsCount > 0) {
                BadgedBox(
                  badge = {
                    Badge(containerColor = SocietyRed) {
                      Text("$pendingApprovalsCount")
                    }
                  }
                ) {
                  Icon(tab.icon, contentDescription = tab.label)
                }
              } else if (tab == SocietyTab.COMPLAINTS && openComplaintsCount > 0) {
                BadgedBox(
                  badge = {
                    Badge(containerColor = Color(0xFFE65100)) {
                      Text("$openComplaintsCount")
                    }
                  }
                ) {
                  Icon(tab.icon, contentDescription = tab.label)
                }
              } else if (tab == SocietyTab.MEMBERS && pendingMembersCount > 0) {
                BadgedBox(
                  badge = {
                    Badge(containerColor = Color(0xFFE65100)) {
                      Text("$pendingMembersCount")
                    }
                  }
                ) {
                  Icon(tab.icon, contentDescription = tab.label)
                }
              } else {
                Icon(tab.icon, contentDescription = tab.label)
              }
            },
            label = {
              Text(
                text = tab.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = SocietyRed,
              selectedTextColor = SocietyRed,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
          )
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        SocietyTab.HOME -> HomeScreen(
          viewModel = viewModel,
          onNavigateToPay = { selectedTab = SocietyTab.PAY },
          onNavigateToComplaints = { selectedTab = SocietyTab.COMPLAINTS },
          onNavigateToExpenses = { selectedTab = SocietyTab.EXPENSES },
          onNavigateToApprovals = { selectedTab = SocietyTab.APPROVALS },
          onNavigateToMembers = { selectedTab = SocietyTab.MEMBERS }
        )
        SocietyTab.PAY -> PayMaintenanceScreen(viewModel = viewModel)
        SocietyTab.APPROVALS -> AdminApprovalsScreen(viewModel = viewModel)
        SocietyTab.MEMBERS -> AdminMembersScreen(viewModel = viewModel)
        SocietyTab.EXPENSES -> ExpensesScreen(viewModel = viewModel)
        SocietyTab.COMPLAINTS -> ComplaintsScreen(viewModel = viewModel)
        SocietyTab.QR_SETTINGS -> AdminSettingsScreen(viewModel = viewModel)
        SocietyTab.PROFILE -> ProfileScreen(
          viewModel = viewModel,
          onSignedOut = onSignedOut,
          onNavigateToComplaints = { selectedTab = SocietyTab.COMPLAINTS }
        )
      }
    }
  }

  val isSubmitting by viewModel.isSubmitting.collectAsState()

  // Mandatory Mobile Registration Dialog (Requirement 1)
  if (userProfile != null && userProfile!!.mobileNumber.isBlank()) {
    MandatoryMobileRegistrationDialog(
      currentName = userProfile!!.name.ifBlank { viewModel.currentUserName },
      currentFlat = userProfile!!.flatNumber,
      isSubmitting = isSubmitting,
      onSave = { name, flat, mobile ->
        viewModel.updateProfile(
          name = name,
          flatNumber = flat,
          role = userProfile?.role ?: "resident",
          mobileNumber = mobile
        )
      }
    )
  }

  if (showNotificationsDialog) {
    NotificationCenterDialog(
      notifications = notifications,
      onDismiss = {
        showNotificationsDialog = false
        viewModel.markAllNotificationsAsRead()
      },
      onMarkAsRead = { id -> viewModel.markNotificationAsRead(id) }
    )
  }
}

@Composable
fun MandatoryMobileRegistrationDialog(
  currentName: String,
  currentFlat: String,
  isSubmitting: Boolean,
  onSave: (name: String, flat: String, mobile: String) -> Unit
) {
  var name by remember { mutableStateOf(currentName) }
  var flat by remember { mutableStateOf(currentFlat.ifBlank { "Flat 101" }) }
  var mobile by remember { mutableStateOf("") }
  var mobileError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = { /* Mandatory: Cannot dismiss until registered with mobile */ },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Phone, contentDescription = null, tint = SocietyRed)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Mobile Number Required",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Please enter your 10-digit WhatsApp mobile number to complete registration. Society maintenance receipts will be shared directly to your WhatsApp.",
          fontSize = 13.sp,
          color = Color(0xFF616161)
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Your Full Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
        )

        OutlinedTextField(
          value = flat,
          onValueChange = { input -> flat = input.filter { it.isDigit() } },
          label = { Text("House No. (Numbers Only) *") },
          placeholder = { Text("e.g. 114, 101, 204") },
          supportingText = { Text("Enter numbers only. Must be unique.") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("reg_flat_input")
        )

        OutlinedTextField(
          value = mobile,
          onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            if (digits.length <= 10) {
              mobile = digits
              mobileError = if (digits.length == 10) null else "Enter 10-digit mobile number"
            }
          },
          label = { Text("WhatsApp / Mobile Number *") },
          placeholder = { Text("10-digit mobile number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          isError = mobileError != null && mobile.isNotEmpty(),
          supportingText = {
            if (mobileError != null && mobile.isNotEmpty()) {
              Text(mobileError ?: "", color = MaterialTheme.colorScheme.error)
            } else {
              Text("Required to receive official WhatsApp receipts")
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("reg_mobile_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank() || flat.isBlank() || mobile.length != 10) {
            mobileError = "Valid 10-digit mobile is required"
            return@Button
          }
          onSave(name.trim(), flat.trim(), mobile.trim())
        },
        enabled = !isSubmitting && name.isNotBlank() && flat.isNotBlank() && mobile.length == 10,
        colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
        modifier = Modifier.testTag("submit_registration_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Saving...")
        } else {
          Text("Save & Continue")
        }
      }
    }
  )
}
