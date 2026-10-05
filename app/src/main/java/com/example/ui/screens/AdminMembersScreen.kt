package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyGreen
import com.example.ui.components.SocietyLightAmber
import com.example.ui.components.SocietyLightGreen
import com.example.ui.components.SocietyLightRed
import com.example.ui.components.SocietyRed
import com.example.ui.components.StatusBadge
import com.example.ui.util.ReceiptShareUtil
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminMembersScreen(
  viewModel: SocietyViewModel
) {
  val context = LocalContext.current
  val allUsers by viewModel.allUsers.collectAsState()
  val allMaintenance by viewModel.allMaintenance.collectAsState()
  val settings by viewModel.societySettings.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var filterCategory by remember { mutableStateOf("All") }
  var showFirebaseInfoDialog by remember { mutableStateOf(false) }

  // Selected user for payment details inspection dialog (Requested by user)
  var selectedUserForDetails by remember { mutableStateOf<UserProfile?>(null) }
  var userToDelete by remember { mutableStateOf<UserProfile?>(null) }

  val filteredUsers = remember(allUsers, searchQuery, filterCategory) {
    allUsers.filter { user ->
      val matchesSearch = searchQuery.isBlank() ||
        user.name.contains(searchQuery, ignoreCase = true) ||
        user.flatNumber.contains(searchQuery, ignoreCase = true) ||
        user.email.contains(searchQuery, ignoreCase = true)

      val matchesCategory = when (filterCategory) {
        "Pending Approvals" -> user.approvalStatus.equals("pending", ignoreCase = true)
        "Blocked" -> user.isBlocked
        "Admins" -> user.isAdmin
        "Residents" -> !user.isAdmin
        else -> true
      }

      matchesSearch && matchesCategory
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Society Members & Roles",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SocietyRed
          )
          Text(
            text = "Tap on any member to inspect their complete payment ledger & UTRs",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF616161)
          )
        }

        OutlinedButton(
          onClick = { showFirebaseInfoDialog = true },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("admin_guide_button")
        ) {
          Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Admin Guide", fontSize = 11.sp)
        }
      }
    }

    // Search Box
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by name, flat (e.g. 101, Plot 114), or email...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("member_search_input")
      )
    }

    // Filter Chips
    item {
      val pendingCount = allUsers.count { it.approvalStatus == "pending" }
      val blockedCount = allUsers.count { it.isBlocked }
      val adminCount = allUsers.count { it.isAdmin }
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        val filters = listOf(
          "All (${allUsers.size})",
          "Pending Approvals ($pendingCount)",
          "Blocked ($blockedCount)",
          "Admins ($adminCount)",
          "Residents (${allUsers.size - adminCount})"
        )
        items(filters) { f ->
          val key = f.substringBefore(" (")
          FilterChip(
            selected = filterCategory == key,
            onClick = { filterCategory = key },
            label = { Text(f) }
          )
        }
      }
    }

    if (filteredUsers.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFFF9F9F9),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = SocietyRed, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (filterCategory == "Pending Approvals") "No pending resident registrations! All caught up." else "No society members matching filter.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF616161)
            )
          }
        }
      }
    } else {
      items(filteredUsers) { user ->
        val isSelf = user.userId == viewModel.currentUserId

        // User maintenance stats
        val userRecords = remember(allMaintenance, user.userId) {
          allMaintenance.filter { it.userId == user.userId }
        }
        val userPaidCount = userRecords.count { it.status == "approved" }
        val userPendingCount = userRecords.count { it.status == "pending" }

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = CardDefaults.outlinedCardBorder(),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${user.userId}")
            .clickable {
              // Open detailed payment ledger for this user (Requested by user)
              selectedUserForDetails = user
            }
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = user.name.ifBlank { "Resident" },
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF212121)
                )
                Text(
                  text = "House No.: ${user.flatNumber} • ${if (user.mobileNumber.isNotBlank()) "📱 ${user.mobileNumber}" else user.email}",
                  fontSize = 12.sp,
                  color = Color(0xFF616161)
                )
                Text(
                  text = "Payments: $userPaidCount Paid • $userPendingCount Pending (Tap for ledger)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = SocietyRed,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }

              // Badges
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (user.isAdmin) SocietyLightRed else Color(0xFFEDE7F6)
                ) {
                  Text(
                    text = if (user.isAdmin) "ADMIN" else "RESIDENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (user.isAdmin) SocietyRed else Color(0xFF4527A0),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = when {
                    user.isBlocked -> Color(0xFFFFCDD2)
                    user.approvalStatus.equals("approved", true) -> SocietyLightGreen
                    user.approvalStatus.equals("rejected", true) -> SocietyLightRed
                    else -> SocietyLightAmber
                  }
                ) {
                  Text(
                    text = if (user.isBlocked) "BLOCKED" else user.approvalStatus.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                      user.isBlocked -> Color(0xFFB71C1C)
                      user.approvalStatus.equals("approved", true) -> SocietyGreen
                      user.approvalStatus.equals("rejected", true) -> SocietyRed
                      else -> Color(0xFFE65100)
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // View Ledger button
              OutlinedButton(
                onClick = { selectedUserForDetails = user },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(38.dp).testTag("view_ledger_${user.userId}")
              ) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Ledger", fontSize = 11.sp)
              }

              // Approval / Block / Delete Actions
              if (user.approvalStatus.equals("pending", ignoreCase = true)) {
                Button(
                  onClick = { viewModel.approveResidentMembership(user.userId, user.name) },
                  colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1.1f).height(38.dp).testTag("approve_member_${user.userId}")
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Approve", fontSize = 11.sp)
                }

                OutlinedButton(
                  onClick = { viewModel.rejectResidentMembership(user.userId, user.name) },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).height(38.dp).testTag("reject_member_${user.userId}")
                ) {
                  Text("Reject", fontSize = 11.sp)
                }
              } else {
                if (!isSelf) {
                  if (user.isBlocked) {
                    Button(
                      onClick = { viewModel.unblockUser(user.userId, user.name) },
                      colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1.1f).height(38.dp).testTag("unblock_user_${user.userId}")
                    ) {
                      Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text("Unblock", fontSize = 11.sp)
                    }
                  } else {
                    OutlinedButton(
                      onClick = { viewModel.blockUser(user.userId, user.name) },
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f).height(38.dp).testTag("block_user_${user.userId}")
                    ) {
                      Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text("Block", fontSize = 11.sp)
                    }
                  }

                  if (user.isAdmin) {
                    OutlinedButton(
                      onClick = { viewModel.makeUserResident(user.userId, user.name) },
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f).height(38.dp).testTag("demote_admin_${user.userId}")
                    ) {
                      Text("Demote", fontSize = 11.sp)
                    }
                  } else if (!user.isBlocked) {
                    Button(
                      onClick = { viewModel.makeUserAdmin(user.userId, user.name) },
                      colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f).height(38.dp).testTag("promote_admin_${user.userId}")
                    ) {
                      Text("Admin", fontSize = 11.sp)
                    }
                  }
                }
              }

              // Permanent Delete Button for Admin (Requested by user)
              if (!isSelf) {
                IconButton(
                  onClick = { userToDelete = user },
                  modifier = Modifier.size(36.dp).testTag("delete_user_${user.userId}")
                ) {
                  Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = "Delete Member",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // -------------------------------------------------------------
  // USER PAYMENT DETAILS & LEDGER DIALOG (Requested by User)
  // -------------------------------------------------------------
  selectedUserForDetails?.let { user ->
    val userRecords = remember(allMaintenance, user.userId) {
      allMaintenance.filter { it.userId == user.userId }
    }
    val totalPaid = userRecords.filter { it.status == "approved" }.sumOf { it.amount }
    val totalPending = userRecords.filter { it.status == "pending" }.sumOf { it.amount }

    AlertDialog(
      onDismissRequest = { selectedUserForDetails = null },
      title = {
        Column {
          Text(
            text = user.name.ifBlank { "Resident Details" },
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
          Text(
            text = "Flat: ${user.flatNumber} • ${user.email}${if (user.mobileNumber.isNotBlank()) " • 📱 ${user.mobileNumber}" else ""}",
            fontSize = 12.sp,
            color = Color(0xFF616161)
          )
        }
      },
      text = {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(380.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item {
            // Summary Card
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("Total Paid", fontSize = 11.sp, color = Color(0xFF757575), textAlign = TextAlign.Center)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("₹${totalPaid.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SocietyGreen)
                }
                Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE0E0E0)))
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("Pending Dues", fontSize = 11.sp, color = Color(0xFF757575), textAlign = TextAlign.Center)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("₹${totalPending.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFE65100))
                }
                Box(modifier = Modifier.height(28.dp).width(1.dp).background(Color(0xFFE0E0E0)))
                Column(
                  modifier = Modifier.weight(1f),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("Total Records", fontSize = 11.sp, color = Color(0xFF757575), textAlign = TextAlign.Center)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("${userRecords.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF212121))
                }
              }
            }
          }

          if (userRecords.isEmpty()) {
            item {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFAFAFA),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
              ) {
                Text(
                  text = "No payment records submitted by this resident yet.",
                  fontSize = 12.sp,
                  color = Color(0xFF757575),
                  modifier = Modifier.padding(16.dp)
                )
              }
            }
          } else {
            items(userRecords) { record ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(record.monthYear, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      Text("₹${record.amount.toInt()} • UTR: ${record.utrNumber}", fontSize = 12.sp, color = Color(0xFF616161))
                    }
                    StatusBadge(status = record.status)
                  }

                  if (record.adminRemarks.isNotBlank()) {
                    Text("Remarks: ${record.adminRemarks}", fontSize = 11.sp, color = SocietyRed, modifier = Modifier.padding(top = 2.dp))
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Actions on this specific record
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    // Share Receipt if approved
                    if (record.status.equals("approved", ignoreCase = true)) {
                      Button(
                        onClick = {
                          ReceiptShareUtil.shareReceiptToWhatsApp(context, record, user.mobileNumber, settings.societyName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                      ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send PDF Receipt", fontSize = 11.sp)
                      }
                    } else if (record.status.equals("pending", ignoreCase = true)) {
                      Button(
                        onClick = {
                          viewModel.approveMaintenancePayment(
                            recordId = record.id,
                            remarks = "Approved from resident ledger",
                            residentName = record.userName,
                            flat = record.flatNumber
                          )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                      ) {
                        Text("Approve", fontSize = 11.sp)
                      }
                      OutlinedButton(
                        onClick = {
                          viewModel.declineMaintenancePayment(
                            recordId = record.id,
                            remarks = "Declined from resident ledger",
                            residentName = record.userName,
                            flat = record.flatNumber
                          )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp)
                      ) {
                        Text("Decline", fontSize = 11.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedUserForDetails = null }) {
          Text("Close")
        }
      }
    )
  }

  // Admin Guide Dialog
  if (showFirebaseInfoDialog) {
    AlertDialog(
      onDismissRequest = { showFirebaseInfoDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Shield, contentDescription = null, tint = SocietyRed)
          Spacer(modifier = Modifier.width(8.dp))
          Text("How to Manage Admins", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Method 1: Inside This App (Instant)",
            fontWeight = FontWeight.Bold,
            color = SocietyRed
          )
          Text(
            text = "As the Master Admin (${viewModel.currentUserEmail}), you can simply tap 'Make Admin' on any registered member in this list to grant them full admin privileges!",
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Method 2: Firebase Web Console",
            fontWeight = FontWeight.Bold,
            color = SocietyRed
          )
          Text(
            text = "1. Open Firebase Console (https://console.firebase.google.com)\n2. Select project 'gen-lang-client-0613838084'\n3. Go to Firestore Database > 'users' collection\n4. Select the user document and change field 'role' from 'resident' to 'admin'.",
            fontSize = 12.sp,
            color = Color(0xFF616161)
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showFirebaseInfoDialog = false }) {
          Text("Got it")
        }
      }
    )
  }

  // Delete User Confirmation Dialog (Requested by user)
  userToDelete?.let { user ->
    AlertDialog(
      onDismissRequest = { userToDelete = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Delete User Record?", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Text("Are you sure you want to permanently delete \"${user.name}\" (House No. ${user.flatNumber}) from Hanuman Nagar Vikas Samiti database? All associated user account data will be removed.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteUser(user.userId, user.name)
            userToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Permanently Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { userToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
