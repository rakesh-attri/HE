package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MaintenanceRecord
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyGreen
import com.example.ui.components.SocietyRed
import com.example.ui.components.StatusBadge
import com.example.ui.util.ReceiptShareUtil
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalsScreen(
  viewModel: SocietyViewModel
) {
  val context = LocalContext.current
  val settings by viewModel.societySettings.collectAsState()
  val pendingRecords by viewModel.pendingMaintenance.collectAsState()
  val allRecords by viewModel.allMaintenance.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  var householdSearch by remember { mutableStateOf("") }
  var householdStatusFilter by remember { mutableStateOf("All") }

  // Filter household records
  val filteredHouseholdRecords = remember(allRecords, householdSearch, householdStatusFilter) {
    allRecords.filter { record ->
      val matchesSearch = householdSearch.isBlank() ||
        record.flatNumber.contains(householdSearch, ignoreCase = true) ||
        record.userName.contains(householdSearch, ignoreCase = true) ||
        record.utrNumber.contains(householdSearch, ignoreCase = true)

      val matchesStatus = when (householdStatusFilter) {
        "Approved" -> record.status.equals("approved", ignoreCase = true)
        "Pending" -> record.status.equals("pending", ignoreCase = true)
        "Declined" -> record.status.equals("declined", ignoreCase = true) || record.status.equals("rejected", ignoreCase = true)
        else -> true
      }

      matchesSearch && matchesStatus
    }
  }

  // Group by Flat Number for Household-wise breakdown
  val recordsByFlat = remember(filteredHouseholdRecords) {
    filteredHouseholdRecords.groupBy { it.flatNumber.ifBlank { "Unassigned" } }
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
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = SocietyRed,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Society Payments & Approvals",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SocietyRed
          )
          Text(
            text = "Verify UTRs, approve/decline dues & share official receipts",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF616161)
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (selectedTab == 0) "Pending Requests: ${pendingRecords.size}" else "Audit Records: ${allRecords.size}",
          fontSize = 12.sp,
          color = Color(0xFF616161)
        )
        OutlinedButton(
          onClick = { viewModel.refreshApprovals() },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.testTag("refresh_approvals_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Refresh Live Approvals", fontSize = 12.sp)
        }
      }
    }

    item {
      PrimaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Pending (${pendingRecords.size})", fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Household History (${allRecords.size})") }
        )
      }
    }

    if (selectedTab == 0) {
      // TAB 0: PENDING APPROVALS
      if (pendingRecords.isEmpty()) {
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
              Icon(Icons.Default.DoneAll, contentDescription = null, tint = SocietyGreen, modifier = Modifier.size(40.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "All Caught Up!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SocietyGreen
              )
              Text(
                text = "There are no pending maintenance payments awaiting verification.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
              )
            }
          }
        }
      } else {
        items(pendingRecords) { record ->
          var remarksInput by remember { mutableStateOf("") }
          var isActionDone by remember { mutableStateOf(false) }

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().testTag("pending_approval_card_${record.id}")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "House No. ${record.flatNumber}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SocietyRed
                  )
                  Text(
                    text = record.userName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121)
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "₹${record.amount.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = SocietyRed
                  )
                  Text(
                    text = record.monthYear,
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // UTR Box with Copy button
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5F5F5),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(text = "12-Digit UTR / Transaction Reference", fontSize = 10.sp, color = Color(0xFF757575))
                    Text(
                      text = record.utrNumber,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp,
                      color = Color(0xFF212121)
                    )
                  }

                  IconButton(
                    onClick = {
                      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                      val clip = ClipData.newPlainText("UTR", record.utrNumber)
                      clipboard.setPrimaryClip(clip)
                      Toast.makeText(context, "UTR copied: ${record.utrNumber}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp).testTag("copy_utr_${record.id}")
                  ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy UTR", modifier = Modifier.size(16.dp))
                  }
                }
              }

              if (record.receiptImageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                var showReceiptDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                  onClick = { showReceiptDialog = true },
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth().testTag("view_receipt_${record.id}")
                ) {
                  Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("View Attached Payment Screenshot", fontSize = 12.sp)
                }

                if (showReceiptDialog) {
                  val bmp = remember(record.receiptImageUrl) { com.example.ui.util.ImageUtils.base64ToBitmap(record.receiptImageUrl) }
                  AlertDialog(
                    onDismissRequest = { showReceiptDialog = false },
                    title = { Text("Payment Screenshot (House ${record.flatNumber})") },
                    text = {
                      if (bmp != null) {
                        Image(
                          bitmap = bmp.asImageBitmap(),
                          contentDescription = "Payment Screenshot",
                          modifier = Modifier.fillMaxWidth().height(350.dp),
                          contentScale = ContentScale.Fit
                        )
                      } else {
                        Text("No valid image data available")
                      }
                    },
                    confirmButton = {
                      TextButton(onClick = { showReceiptDialog = false }) {
                        Text("Close")
                      }
                    }
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = remarksInput,
                onValueChange = { remarksInput = it },
                label = { Text("Receipt / Admin Remarks (Optional)") },
                placeholder = { Text("e.g. Verified in SBI bank account") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("remarks_input_${record.id}")
              )

              Spacer(modifier = Modifier.height(12.dp))

              record.createdAt?.toDate()?.let { date ->
                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(date)
                Text(
                  text = "Submitted: $dateStr",
                  fontSize = 11.sp,
                  color = Color(0xFF757575)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              // APPROVE AND DECLINE BUTTON ROW (Both options provided)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // DECLINE BUTTON
                OutlinedButton(
                  onClick = {
                    viewModel.declineMaintenancePayment(
                      recordId = record.id,
                      remarks = remarksInput.ifBlank { "Declined by Admin" },
                      residentName = record.userName,
                      flat = record.flatNumber
                    )
                  },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f).height(44.dp).testTag("decline_button_${record.id}")
                ) {
                  Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Decline", fontSize = 13.sp)
                }

                // APPROVE BUTTON
                Button(
                  onClick = {
                    viewModel.approveMaintenancePayment(
                      recordId = record.id,
                      remarks = remarksInput,
                      residentName = record.userName,
                      flat = record.flatNumber
                    )
                    isActionDone = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1.4f).height(44.dp).testTag("approve_button_${record.id}")
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Approve Payment", fontSize = 13.sp)
                }
              }

              // SEND PDF RECEIPT SHORTCUT (WhatsApp Direct)
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = {
                  val updatedRecord = record.copy(status = "approved", adminRemarks = remarksInput)
                  val allUsersList = viewModel.allUsers.value
                  val residentMobile = allUsersList.find { it.userId == record.userId }?.mobileNumber ?: record.userMobile
                  ReceiptShareUtil.shareReceiptToWhatsApp(context, updatedRecord, residentMobile, settings.societyName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp).testTag("share_receipt_quick_${record.id}")
              ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Official PDF Receipt (WhatsApp)", fontSize = 12.sp)
              }
            }
          }
        }
      }
    } else {
      // TAB 1: HOUSEHOLD-WISE HISTORY & AUDIT
      item {
        OutlinedTextField(
          value = householdSearch,
          onValueChange = { householdSearch = it },
          placeholder = { Text("Search by Flat (e.g. 101, Plot 114) or Name...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("household_search_input")
        )
      }

      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val filters = listOf("All", "Approved", "Pending", "Declined")
          items(filters) { f ->
            FilterChip(
              selected = householdStatusFilter == f,
              onClick = { householdStatusFilter = f },
              label = { Text(f) }
            )
          }
        }
      }

      if (recordsByFlat.isEmpty()) {
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
              Icon(Icons.Default.Info, contentDescription = null, tint = SocietyRed)
              Spacer(modifier = Modifier.height(8.dp))
              Text("No payments found matching criteria.", color = Color(0xFF616161))
            }
          }
        }
      } else {
        // Group by Household Flat Cards
        recordsByFlat.forEach { (flatNumber, records) ->
          item(key = flatNumber) {
            val totalPaid = records.filter { it.status == "approved" }.sumOf { it.amount }
            val totalPending = records.filter { it.status == "pending" }.sumOf { it.amount }

            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              border = CardDefaults.outlinedCardBorder(),
              modifier = Modifier.fillMaxWidth().testTag("household_group_$flatNumber")
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Apartment, contentDescription = null, tint = SocietyRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = flatNumber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = SocietyRed
                      )
                      Text(
                        text = "Resident: ${records.firstOrNull()?.userName ?: "Resident"}",
                        fontSize = 12.sp,
                        color = Color(0xFF616161)
                      )
                    }
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "Paid: ₹${totalPaid.toInt()}",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold,
                      color = SocietyGreen
                    )
                    if (totalPending > 0) {
                      Text(
                        text = "Pending: ₹${totalPending.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List individual transaction records under this household
                records.forEach { record ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF9F9F9),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = record.monthYear,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF212121)
                        )
                        Text(
                          text = "₹${record.amount.toInt()} • UTR: ${record.utrNumber}",
                          fontSize = 12.sp,
                          color = Color(0xFF616161)
                        )
                        if (record.adminRemarks.isNotBlank()) {
                          Text(
                            text = "Note: ${record.adminRemarks}",
                            fontSize = 11.sp,
                            color = SocietyRed
                          )
                        }
                      }

                      Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(status = record.status)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                          onClick = {
                            ReceiptShareUtil.viewReceiptPdf(context, record, settings.societyName)
                          },
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(Icons.Default.PictureAsPdf, contentDescription = "View PDF Receipt", tint = SocietyRed, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                          onClick = {
                            ReceiptShareUtil.shareReceipt(context, record, settings.societyName)
                          },
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(Icons.Default.Share, contentDescription = "Send PDF Receipt", tint = Color(0xFF757575), modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }
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
}
