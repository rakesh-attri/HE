package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Complaint
import com.example.ui.SocietyViewModel
import com.example.ui.components.SocietyGreen
import com.example.ui.components.SocietyRed
import com.example.ui.components.StatusBadge
import com.example.ui.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ComplaintsScreen(
  viewModel: SocietyViewModel
) {
  val complaints by viewModel.complaints.collectAsState()
  val isSubmitting by viewModel.isSubmitting.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()
  val effectiveRole = viewModel.effectiveRole()

  var showNewForm by remember { mutableStateOf(false) }
  var titleInput by remember { mutableStateOf("") }
  var descInput by remember { mutableStateOf("") }
  var filterStatus by remember { mutableStateOf("All") }

  // Real-time camera capture only (No gallery uploads - Item 2)
  var capturedPhotoBase64 by remember { mutableStateOf("") }
  val cameraLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicturePreview()
  ) { bitmap ->
    if (bitmap != null) {
      // Scale down to max 420px, JPEG quality 65 to minimize space
      capturedPhotoBase64 = ImageUtils.bitmapToBase64(bitmap, maxDimension = 420, quality = 65) ?: ""
    }
  }

  // Selected complaint for Chat / Discussion Activity (Item 10)
  var activeDiscussionComplaintId by remember { mutableStateOf<String?>(null) }
  val activeDiscussionComplaint = remember(complaints, activeDiscussionComplaintId) {
    complaints.firstOrNull { it.id == activeDiscussionComplaintId }
  }

  var fullImageViewBase64 by remember { mutableStateOf<String?>(null) }

  val filteredComplaints = remember(complaints, filterStatus) {
    when (filterStatus) {
      "Open" -> complaints.filter { it.status.equals("open", ignoreCase = true) }
      "In Progress" -> complaints.filter { it.status.equals("in_progress", ignoreCase = true) }
      "Resolved" -> complaints.filter { it.status.equals("resolved", ignoreCase = true) }
      else -> complaints
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
            text = "Society Complaints & Redressal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SocietyRed
          )
          Text(
            text = if (effectiveRole == "admin") "Review complaints, chat with residents & resolve issues" else "Lodge issues with live camera proof & chat with committee",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Button(
          onClick = { showNewForm = !showNewForm },
          colors = ButtonDefaults.buttonColors(containerColor = if (showNewForm) MaterialTheme.colorScheme.surfaceVariant else SocietyRed),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("lodge_complaint_toggle_button")
        ) {
          Icon(
            if (showNewForm) Icons.Default.Close else Icons.Default.Add,
            contentDescription = null,
            tint = if (showNewForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (showNewForm) "Close" else "Lodge",
            color = if (showNewForm) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
          )
        }
      }
    }

    // New Complaint Form Card
    if (showNewForm) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Lodge a Complaint",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Lodge as: ${userProfile?.name ?: viewModel.currentUserName} (House No. ${userProfile?.flatNumber ?: "N/A"})",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = titleInput,
              onValueChange = { titleInput = it },
              label = { Text("Issue Title *") },
              placeholder = { Text("e.g. Streetlight flickering near Pole 3") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("complaint_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = descInput,
              onValueChange = { descInput = it },
              label = { Text("Description & Location Details *") },
              placeholder = { Text("Describe the issue clearly for the maintenance team...") },
              minLines = 3,
              maxLines = 6,
              modifier = Modifier.fillMaxWidth().testTag("complaint_description_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-Time Camera Capture Section (Item 2: Camera only, no uploads, reduced size)
            if (capturedPhotoBase64.isNotBlank()) {
              val bmp = remember(capturedPhotoBase64) { ImageUtils.base64ToBitmap(capturedPhotoBase64) }
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (bmp != null) {
                    Image(
                      bitmap = bmp.asImageBitmap(),
                      contentDescription = "Captured Issue Photo",
                      modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp)),
                      contentScale = ContentScale.Crop
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Live Camera Photo Captured", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Compressed to save cloud storage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  IconButton(onClick = { capturedPhotoBase64 = "" }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Photo", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
            } else {
              OutlinedButton(
                onClick = { cameraLauncher.launch(null) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("capture_photo_button")
              ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = SocietyRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capture Photo Proof (Camera Only)", color = SocietyRed, fontWeight = FontWeight.SemiBold)
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = {
                viewModel.submitComplaint(
                  title = titleInput,
                  description = descInput,
                  photoUrl = capturedPhotoBase64
                )
                titleInput = ""
                descInput = ""
                capturedPhotoBase64 = ""
                showNewForm = false
              },
              enabled = !isSubmitting && titleInput.isNotBlank() && descInput.isNotBlank(),
              colors = ButtonDefaults.buttonColors(containerColor = SocietyRed),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_complaint_button")
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting...")
              } else {
                Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Complaint")
              }
            }
          }
        }
      }
    }

    // Filter Chips Row
    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        val filters = listOf("All", "Open", "In Progress", "Resolved")
        items(filters) { filter ->
          val count = when (filter) {
            "Open" -> complaints.count { it.status == "open" }
            "In Progress" -> complaints.count { it.status == "in_progress" }
            "Resolved" -> complaints.count { it.status == "resolved" }
            else -> complaints.size
          }
          FilterChip(
            selected = filterStatus == filter,
            onClick = { filterStatus = filter },
            label = { Text("$filter ($count)") }
          )
        }
      }
    }

    if (filteredComplaints.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (filterStatus == "All") "No complaints recorded yet." else "No $filterStatus complaints found.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(filteredComplaints) { item ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("complaint_card_${item.id}")
            .clickable { activeDiscussionComplaintId = item.id }
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.title,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "House No.: ${item.flatNumber} • ${item.userName}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = item.description,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 18.sp
            )

            // Photo Proof Thumbnail (Item 2)
            if (item.photoUrl.isNotBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              val bmp = remember(item.photoUrl) { ImageUtils.base64ToBitmap(item.photoUrl) }
              if (bmp != null) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { fullImageViewBase64 = item.photoUrl }
                    .padding(6.dp)
                ) {
                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Photo Proof",
                    modifier = Modifier
                      .size(44.dp)
                      .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text("Live Camera Proof Attached", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Tap to view full photo", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }

            // Summary of chat replies
            val messageCount = item.messages.size
            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              item.createdAt?.toDate()?.let { date ->
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(date)
                Text(
                  text = "Logged: $dateStr",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.outline
                )
              }

              // Open Chat Activity Button (Item 10)
              Button(
                onClick = { activeDiscussionComplaintId = item.id },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (effectiveRole == "admin") SocietyRed else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp).testTag("open_chat_${item.id}")
              ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (messageCount > 0) "Chat ($messageCount updates)" else "Chat / Respond",
                  fontSize = 11.sp
                )
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

  // -------------------------------------------------------------------
  // FULL-SCREEN PHOTO PREVIEW DIALOG
  // -------------------------------------------------------------------
  fullImageViewBase64?.let { base64 ->
    val bmp = remember(base64) { ImageUtils.base64ToBitmap(base64) }
    AlertDialog(
      onDismissRequest = { fullImageViewBase64 = null },
      title = { Text("Complaint Photo Proof") },
      text = {
        if (bmp != null) {
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Full Complaint Photo",
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 380.dp),
            contentScale = ContentScale.Fit
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { fullImageViewBase64 = null }) {
          Text("Close")
        }
      }
    )
  }

  // -------------------------------------------------------------------
  // COMPLAINT DISCUSSION & CHAT DIALOG (Item 10)
  // -------------------------------------------------------------------
  activeDiscussionComplaint?.let { complaint ->
    ComplaintChatDialog(
      complaint = complaint,
      effectiveRole = effectiveRole,
      onDismiss = { activeDiscussionComplaintId = null },
      onSendMessage = { text ->
        viewModel.sendComplaintMessage(complaint.id, text)
      },
      onUpdateStatus = { status, note ->
        viewModel.respondToComplaint(complaint.id, note, status)
      },
      onViewPhoto = { photo -> fullImageViewBase64 = photo }
    )
  }
}

@Composable
fun ComplaintChatDialog(
  complaint: Complaint,
  effectiveRole: String,
  onDismiss: () -> Unit,
  onSendMessage: (String) -> Unit,
  onUpdateStatus: (newStatus: String, note: String) -> Unit,
  onViewPhoto: (String) -> Unit
) {
  var messageInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val messages = complaint.parsedMessages

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 560.dp),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = complaint.title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          Text(
            text = "House No.: ${complaint.flatNumber} • ${complaint.userName}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        StatusBadge(status = complaint.status)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxSize()) {
        // Admin Quick Status Controls (Item 10)
        if (effectiveRole == "admin") {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "Admin Status Control:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SocietyRed
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                if (complaint.status != "in_progress") {
                  Button(
                    onClick = { onUpdateStatus("in_progress", "Marked in progress by Admin") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                  ) {
                    Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("In Progress", fontSize = 10.sp)
                  }
                }

                if (complaint.status != "resolved") {
                  Button(
                    onClick = { onUpdateStatus("resolved", "Marked as resolved by Admin") },
                    colors = ButtonDefaults.buttonColors(containerColor = SocietyGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Resolve", fontSize = 10.sp)
                  }
                } else {
                  OutlinedButton(
                    onClick = { onUpdateStatus("open", "Reopened by Admin") },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                  ) {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Re-open", fontSize = 10.sp)
                  }
                }
              }
            }
          }
        }

        // Chat Message Stream
        LazyColumn(
          state = listState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Original Complaint Description (Starting bubble)
          item {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFFF5F5F5),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "${complaint.userName} (Resident)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  complaint.createdAt?.toDate()?.let {
                    Text(
                      text = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(it),
                      fontSize = 9.sp,
                      color = Color.Gray
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = complaint.description, fontSize = 12.sp)

                // Optional attached photo thumbnail
                if (complaint.photoUrl.isNotBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  val bmp = remember(complaint.photoUrl) { ImageUtils.base64ToBitmap(complaint.photoUrl) }
                  if (bmp != null) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier
                        .clickable { onViewPhoto(complaint.photoUrl) }
                        .padding(top = 4.dp)
                    ) {
                      Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Photo Proof",
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("View Photo Proof", fontSize = 10.sp, color = SocietyRed, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }

          // Thread messages
          items(messages) { msg ->
            val isAdminSender = msg.senderRole.equals("admin", ignoreCase = true)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = if (isAdminSender) Arrangement.End else Arrangement.Start
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAdminSender) Color(0xFFFFEBEE) else Color(0xFFE8EAF6),
                modifier = Modifier.fillMaxWidth(0.9f)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = msg.senderName,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      color = if (isAdminSender) SocietyRed else Color(0xFF283593)
                    )
                    Text(
                      text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(msg.timeMillis)),
                      fontSize = 9.sp,
                      color = Color.Gray
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = msg.message,
                    fontSize = 12.sp,
                    color = Color(0xFF212121),
                    lineHeight = 16.sp
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = messageInput,
            onValueChange = { messageInput = it },
            placeholder = { Text("Write reply or update...", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f).testTag("complaint_chat_input")
          )
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(
            onClick = {
              if (messageInput.trim().isNotBlank()) {
                onSendMessage(messageInput.trim())
                messageInput = ""
              }
            },
            enabled = messageInput.trim().isNotBlank(),
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(21.dp))
              .background(if (messageInput.trim().isNotBlank()) SocietyRed else Color.LightGray)
              .testTag("send_complaint_chat_button")
          ) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}
