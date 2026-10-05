package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SocietyNotification
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationBellButton(
  unreadCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  IconButton(
    onClick = onClick,
    modifier = modifier.testTag("notification_bell_button")
  ) {
    BadgedBox(
      badge = {
        if (unreadCount > 0) {
          Badge(
            containerColor = SocietyRed,
            contentColor = Color.White
          ) {
            Text(text = if (unreadCount > 9) "9+" else unreadCount.toString(), fontSize = 10.sp)
          }
        }
      }
    ) {
      Icon(
        imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
        contentDescription = "Notifications",
        tint = if (unreadCount > 0) SocietyRed else Color(0xFF424242)
      )
    }
  }
}

@Composable
fun NotificationCenterDialog(
  notifications: List<SocietyNotification>,
  onDismiss: () -> Unit,
  onMarkAsRead: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Notifications, contentDescription = null, tint = SocietyRed)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Push Notifications", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
        }
      }
    },
    text = {
      if (notifications.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFF9F9F9),
          modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.DoneAll, contentDescription = null, tint = SocietyGreen, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No notifications yet",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color(0xFF212121)
            )
            Text(
              text = "You will receive real-time push alerts when an admin verifies your payment or updates your complaint.",
              fontSize = 12.sp,
              color = Color(0xFF616161),
              lineHeight = 16.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(360.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(notifications) { notif ->
            val icon = when (notif.type) {
              "payment_approved" -> Icons.Default.Payment
              "payment_declined" -> Icons.Default.Payment
              "complaint_updated" -> Icons.Default.ReportProblem
              "notice_published" -> Icons.Default.Campaign
              else -> Icons.Default.Notifications
            }

            val iconColor = when (notif.type) {
              "payment_approved" -> SocietyGreen
              "payment_declined" -> SocietyRed
              "complaint_updated" -> Color(0xFFE65100)
              "notice_published" -> Color(0xFF1565C0)
              else -> SocietyRed
            }

            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (!notif.isRead) Color(0xFFFFF8E1) else Color.White
              ),
              border = CardDefaults.outlinedCardBorder(),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (!notif.isRead) {
                    onMarkAsRead(notif.id)
                  }
                }
                .testTag("notification_item_${notif.id}")
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = notif.title,
                      fontWeight = if (!notif.isRead) FontWeight.ExtraBold else FontWeight.SemiBold,
                      fontSize = 13.sp,
                      color = Color(0xFF212121),
                      modifier = Modifier.weight(1f)
                    )
                    if (!notif.isRead) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .clip(CircleShape)
                          .background(SocietyRed)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                    text = notif.body,
                    fontSize = 12.sp,
                    color = Color(0xFF424242),
                    lineHeight = 16.sp
                  )

                  notif.createdAt?.toDate()?.let { date ->
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(date)
                    Text(
                      text = dateStr,
                      fontSize = 10.sp,
                      color = Color(0xFF757575),
                      modifier = Modifier.padding(top = 4.dp)
                    )
                  }
                }
              }
            }
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
