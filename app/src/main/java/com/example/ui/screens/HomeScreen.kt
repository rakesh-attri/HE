package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyAmber
import com.example.ui.components.SocietyDarkRed
import com.example.ui.components.SocietyEmblem
import com.example.ui.components.SocietyGreen
import com.example.ui.components.SocietyLightAmber
import com.example.ui.components.SocietyLightGreen
import com.example.ui.components.SocietyLightRed
import com.example.ui.components.SocietyRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  viewModel: SocietyViewModel,
  onNavigateToPay: () -> Unit,
  onNavigateToComplaints: () -> Unit,
  onNavigateToExpenses: () -> Unit,
  onNavigateToApprovals: () -> Unit,
  onNavigateToMembers: () -> Unit
) {
  val userProfile by viewModel.userProfile.collectAsState()
  val residentMaintenance by viewModel.residentMaintenance.collectAsState()
  val pendingMaintenance by viewModel.pendingMaintenance.collectAsState()
  val expenses by viewModel.societyExpenses.collectAsState()
  val complaints by viewModel.complaints.collectAsState()
  val notices by viewModel.notices.collectAsState()
  val allUsers by viewModel.allUsers.collectAsState()
  val settings by viewModel.societySettings.collectAsState()
  val effectiveRole = viewModel.effectiveRole()

  val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date())

  val currentMonthRecord = residentMaintenance.firstOrNull {
    it.monthYear.equals(currentMonthYear, ignoreCase = true)
  }

  val currentMonthPaymentStatus = when {
    currentMonthRecord?.status?.equals("approved", ignoreCase = true) == true -> "Paid"
    currentMonthRecord?.status?.equals("pending", ignoreCase = true) == true -> "Pending"
    else -> "Unpaid"
  }

  var showAddNoticeDialog by remember { mutableStateOf(false) }
  var noticeTitle by remember { mutableStateOf("") }
  var noticeDesc by remember { mutableStateOf("") }
  var noticePriority by remember { mutableStateOf("normal") }

  val pendingMemberApprovalsCount = allUsers.count { it.approvalStatus == "pending" }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Society Header Card (Clean Logo & Title)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = CardDefaults.outlinedCardBorder(),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      modifier = Modifier.fillMaxWidth().testTag("society_header_card")
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        SocietyEmblem(size = 56.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = settings.societyName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SocietyRed
          )
          Text(
            text = "हनुमान नगर विस्तार 1 • झोटवाड़ा, जयपुर (राज.)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF616161)
          )
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFFFEBEE),
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Text(
              text = "Reg. No. COOP/2023/JAIPUR/205538",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = SocietyRed,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Resident / Flat Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
      border = CardDefaults.outlinedCardBorder(),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Welcome Resident",
            fontSize = 11.sp,
            color = Color(0xFF616161)
          )
          Text(
            text = userProfile?.name?.ifBlank { viewModel.currentUserName } ?: viewModel.currentUserName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFFFEBEE)
            ) {
              Text(
                text = if (userProfile?.flatNumber.isNullOrBlank()) "House No. Pending" else "House No. ${userProfile?.flatNumber}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SocietyRed,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (effectiveRole == "admin") SocietyLightRed else Color(0xFFEDE7F6)
            ) {
              Text(
                text = if (effectiveRole == "admin") "ADMIN" else "RESIDENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (effectiveRole == "admin") SocietyRed else Color(0xFF4527A0),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }

        if (userProfile?.isAdmin == true) {
          OutlinedButton(
            onClick = { viewModel.toggleRoleOverride() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("role_switch_button")
          ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (effectiveRole == "admin") "View as Res." else "View as Admin", fontSize = 11.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Current Month Maintenance Status Card
    when (currentMonthPaymentStatus) {
      "Paid" -> {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SocietyLightGreen),
          modifier = Modifier.fillMaxWidth().testTag("maintenance_status_paid")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SocietyGreen),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$currentMonthYear Maintenance",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SocietyGreen
              )
              Text(
                text = "PAID & VERIFIED (₹${currentMonthRecord?.amount?.toInt() ?: 1000})",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SocietyGreen
              )
              Text(
                text = "UTR: ${currentMonthRecord?.utrNumber ?: ""}",
                fontSize = 11.sp,
                color = Color(0xFF424242)
              )
            }
          }
        }
      }
      "Pending" -> {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SocietyLightAmber),
          modifier = Modifier.fillMaxWidth().testTag("maintenance_status_pending")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFE65100)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$currentMonthYear Maintenance",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFBF360C)
              )
              Text(
                text = "PAYMENT PENDING APPROVAL",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFBF360C)
              )
              Text(
                text = "UTR: ${currentMonthRecord?.utrNumber ?: ""} • Verification in progress",
                fontSize = 11.sp,
                color = Color(0xFF424242)
              )
            }
          }
        }
      }
      else -> {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SocietyLightRed),
          modifier = Modifier.fillMaxWidth().testTag("maintenance_status_unpaid")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(SocietyRed),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "$currentMonthYear Maintenance",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = SocietyRed
                )
                Text(
                  text = "MAINTENANCE DUE: ₹${settings.monthlyMaintenanceAmount.toInt()}",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = SocietyDarkRed
                )
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onNavigateToPay,
              colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("pay_maintenance_cta_button")
            ) {
              Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Pay Maintenance Now via UPI")
            }
          }
        }
      }
    }

    // Admin Alerts (if Admin)
    if (effectiveRole == "admin") {
      if (pendingMaintenance.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToApprovals() }
            .testTag("admin_pending_alert_card")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Payment Verification Alert",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SocietyRed
              )
              Text(
                text = "${pendingMaintenance.size} Pending Maintenance Payments",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SocietyDarkRed
              )
              Text(
                text = "Tap to review UTR numbers, approve or decline",
                fontSize = 12.sp,
                color = Color(0xFF616161)
              )
            }
            Icon(
              Icons.Default.HourglassTop,
              contentDescription = null,
              tint = SocietyRed,
              modifier = Modifier.size(28.dp)
            )
          }
        }
      }

      if (pendingMemberApprovalsCount > 0) {
        Spacer(modifier = Modifier.height(10.dp))
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SocietyLightAmber),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToMembers() }
            .testTag("admin_member_pending_card")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Member Approval Gate",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBF360C)
              )
              Text(
                text = "$pendingMemberApprovalsCount New Resident Registration(s)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBF360C)
              )
              Text(
                text = "Tap to review flat details and grant access",
                fontSize = 12.sp,
                color = Color(0xFF424242)
              )
            }
            Icon(
              Icons.Default.Group,
              contentDescription = null,
              tint = Color(0xFFBF360C),
              modifier = Modifier.size(28.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Quick Action Grid
    Text(
      text = "Quick Actions",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF212121)
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      QuickActionCard(
        title = "Pay Dues",
        subtitle = "UPI & QR",
        icon = Icons.Default.Payment,
        color = SocietyRed,
        onClick = onNavigateToPay,
        modifier = Modifier.weight(1f).testTag("quick_action_pay")
      )
      QuickActionCard(
        title = "Complaints",
        subtitle = "${complaints.count { it.status == "open" }} open",
        icon = Icons.Default.ReportProblem,
        color = Color(0xFFE65100),
        onClick = onNavigateToComplaints,
        modifier = Modifier.weight(1f).testTag("quick_action_complaints")
      )
      QuickActionCard(
        title = "Expenses",
        subtitle = "₹${expenses.sumOf { it.amount }.toInt()}",
        icon = Icons.Default.ReceiptLong,
        color = Color(0xFF1565C0),
        onClick = onNavigateToExpenses,
        modifier = Modifier.weight(1f).testTag("quick_action_expenses")
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Society Official Notice Board (with Admin Add Notice functionality)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      border = CardDefaults.outlinedCardBorder(),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Campaign, contentDescription = null, tint = SocietyRed, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Society Notice Board",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF212121)
            )
          }

          if (effectiveRole == "admin") {
            Button(
              onClick = { showAddNoticeDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(34.dp).testTag("admin_add_notice_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Post Notice", fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (notices.isEmpty()) {
          NoticeCard(
            title = "Next General Body Meeting (GBM)",
            desc = "Scheduled for 2nd Sunday of this month at 10:00 AM at Community Park. All residents are requested to attend.",
            date = "01 Oct 2026",
            isUrgent = false,
            canDelete = false,
            onDelete = {}
          )
          NoticeCard(
            title = "Daily Door-to-Door Garbage Collection",
            desc = "Daily waste collection is at 8:00 AM. Please keep segregated dry and wet bins ready outside your door.",
            date = "01 Oct 2026",
            isUrgent = false,
            canDelete = false,
            onDelete = {}
          )
        } else {
          notices.forEach { notice ->
            NoticeCard(
              title = notice.title,
              desc = notice.description,
              date = notice.date,
              isUrgent = notice.priority.equals("urgent", ignoreCase = true),
              canDelete = effectiveRole == "admin",
              onDelete = { viewModel.deleteNotice(notice.id) }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Society Management Contacts
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
      border = CardDefaults.outlinedCardBorder(),
      modifier = Modifier.fillMaxWidth().testTag("society_contacts_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.ContactPhone, contentDescription = null, tint = SocietyRed, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Society Contacts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OfficialContactRow(
          role = "Chairman (अध्यक्ष)",
          name = "Sh. Charan Sing Gill",
          phone = "+91 98290 12345"
        )
        OfficialContactRow(
          role = "Secretary (सचिव)",
          name = "Sh. Deepak Pareek",
          phone = "+91 94140 67890"
        )
        OfficialContactRow(
          role = "Treasurer (कोषाध्यक्ष)",
          name = "Sh. Bhanwar Shekhawat",
          phone = "+91 99280 54321"
        )
        ContactRow(role = "Society Office", desc = "Plot No. 114, Hanuman Nagar Vistar 1")
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  // Add Notice Dialog (Admin)
  if (showAddNoticeDialog) {
    AlertDialog(
      onDismissRequest = { showAddNoticeDialog = false },
      title = {
        Text("Publish Society Notice", fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = noticeTitle,
            onValueChange = { noticeTitle = it },
            label = { Text("Notice Title *") },
            placeholder = { Text("e.g. Water Tank Maintenance") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("notice_title_input")
          )

          OutlinedTextField(
            value = noticeDesc,
            onValueChange = { noticeDesc = it },
            label = { Text("Notice Description *") },
            placeholder = { Text("Write details of the announcement for all residents...") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("notice_desc_input")
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = noticePriority == "normal",
              onClick = { noticePriority = "normal" },
              label = { Text("Normal Priority") }
            )
            FilterChip(
              selected = noticePriority == "urgent",
              onClick = { noticePriority = "urgent" },
              label = { Text("Urgent Alert") }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())
            viewModel.addNotice(noticeTitle, noticeDesc, dateStr, noticePriority)
            noticeTitle = ""
            noticeDesc = ""
            showAddNoticeDialog = false
          },
          enabled = noticeTitle.isNotBlank() && noticeDesc.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = SocietyRed)
        ) {
          Text("Publish Notice")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddNoticeDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun NoticeCard(
  title: String,
  desc: String,
  date: String,
  isUrgent: Boolean,
  canDelete: Boolean,
  onDelete: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isUrgent) SocietyLightRed else Color(0xFFF9F9F9),
    border = if (isUrgent) CardDefaults.outlinedCardBorder() else null,
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isUrgent) SocietyDarkRed else Color(0xFF212121)
          )
          if (isUrgent) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = SocietyRed
            ) {
              Text(
                text = "URGENT",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        if (canDelete) {
          IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Notice", tint = Color(0xFF757575), modifier = Modifier.size(16.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = desc,
        fontSize = 12.sp,
        color = Color(0xFF424242),
        lineHeight = 17.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Date: $date",
        fontSize = 10.sp,
        color = Color(0xFF757575)
      )
    }
  }
}

@Composable
private fun QuickActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = modifier.clickable { onClick() }
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
      Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF616161))
    }
  }
}

@Composable
private fun OfficialContactRow(role: String, name: String, phone: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = role, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF212121))
      Text(
        text = name,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = SocietyRed
      )
    }
    Text(text = phone, fontSize = 12.sp, color = Color(0xFF616161))
  }
}

@Composable
private fun ContactRow(role: String, desc: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = role, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF212121))
    Text(text = desc, fontSize = 12.sp, color = Color(0xFF616161))
  }
}
