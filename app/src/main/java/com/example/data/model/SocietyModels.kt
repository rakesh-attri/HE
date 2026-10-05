package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class UserProfile(
  val userId: String = "",
  val name: String = "",
  val email: String = "",
  val flatNumber: String = "",
  val mobileNumber: String = "",
  val role: String = "resident",
  val approvalStatus: String = "approved",
  val fcmToken: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val isAdmin: Boolean
    get() = role.equals("admin", ignoreCase = true) || email.equals("therakeshattri@gmail.com", ignoreCase = true)

  val isApproved: Boolean
    get() = (approvalStatus.equals("approved", ignoreCase = true) || isAdmin) && !isBlocked

  val isBlocked: Boolean
    get() = approvalStatus.equals("blocked", ignoreCase = true)

  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "userId" to userId,
      "name" to name,
      "email" to email,
      "flatNumber" to flatNumber,
      "role" to role,
      "approvalStatus" to approvalStatus,
      "createdAt" to FieldValue.serverTimestamp()
    )
    if (mobileNumber.isNotBlank()) {
      map["mobileNumber"] = mobileNumber
    }
    if (fcmToken.isNotBlank()) {
      map["fcmToken"] = fcmToken
    }
    return map
  }

  fun toUpdateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "name" to name,
      "flatNumber" to flatNumber,
      "role" to role,
      "approvalStatus" to approvalStatus,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (mobileNumber.isNotBlank()) {
      map["mobileNumber"] = mobileNumber
    }
    if (fcmToken.isNotBlank()) {
      map["fcmToken"] = fcmToken
    }
    return map
  }
}

data class SocietySettings(
  val upiId: String = "hanumannagar@upi",
  val qrCodeImageUrl: String = "",
  val societyName: String = "हनुमान नगर विस्तार 1 विकास समिति",
  val monthlyMaintenanceAmount: Double = 1000.0,
  val updatedAt: Timestamp? = null
) {
  fun toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "upiId" to upiId,
      "societyName" to societyName,
      "monthlyMaintenanceAmount" to monthlyMaintenanceAmount,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (qrCodeImageUrl.isNotBlank()) {
      map["qrCodeImageUrl"] = qrCodeImageUrl
    }
    return map
  }
}

data class MaintenanceRecord(
  val id: String = "",
  val userId: String = "",
  val userName: String = "",
  val flatNumber: String = "",
  val userMobile: String = "",
  val receiptImageUrl: String = "",
  val monthYear: String = "",
  val amount: Double = 1000.0,
  val utrNumber: String = "",
  val status: String = "pending",
  val adminRemarks: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "userId" to userId,
      "userName" to userName,
      "flatNumber" to flatNumber,
      "monthYear" to monthYear,
      "amount" to amount,
      "utrNumber" to utrNumber,
      "status" to "pending",
      "createdAt" to FieldValue.serverTimestamp()
    )
    if (userMobile.isNotBlank()) {
      map["userMobile"] = userMobile
    }
    if (receiptImageUrl.isNotBlank()) {
      map["receiptImageUrl"] = receiptImageUrl
    }
    if (adminRemarks.isNotBlank()) {
      map["adminRemarks"] = adminRemarks
    }
    return map
  }
}

data class SocietyExpense(
  val id: String = "",
  val title: String = "",
  val category: String = "General",
  val amount: Double = 0.0,
  val date: String = "",
  val loggedBy: String = "",
  val createdAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "title" to title,
      "category" to category,
      "amount" to amount,
      "date" to date,
      "createdAt" to FieldValue.serverTimestamp()
    )
    if (loggedBy.isNotBlank()) {
      map["loggedBy"] = loggedBy
    }
    return map
  }
}

data class ComplaintChatEntry(
  val senderId: String = "",
  val senderName: String = "",
  val senderRole: String = "resident",
  val message: String = "",
  val timeMillis: Long = System.currentTimeMillis()
) {
  fun toMap(): Map<String, Any> = mapOf(
    "senderId" to senderId,
    "senderName" to senderName,
    "senderRole" to senderRole,
    "message" to message,
    "timeMillis" to timeMillis
  )

  companion object {
    fun fromMap(map: Map<String, Any?>): ComplaintChatEntry {
      return ComplaintChatEntry(
        senderId = map["senderId"] as? String ?: "",
        senderName = map["senderName"] as? String ?: "",
        senderRole = map["senderRole"] as? String ?: "resident",
        message = map["message"] as? String ?: "",
        timeMillis = (map["timeMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
      )
    }
  }
}

data class Complaint(
  val id: String = "",
  val userId: String = "",
  val userName: String = "",
  val flatNumber: String = "",
  val title: String = "",
  val description: String = "",
  val photoUrl: String = "",
  val status: String = "open",
  val adminResponse: String = "",
  val messages: List<Map<String, Any>> = emptyList(),
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val parsedMessages: List<ComplaintChatEntry>
    get() = messages.map { ComplaintChatEntry.fromMap(it) }

  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "userId" to userId,
      "userName" to userName,
      "flatNumber" to flatNumber,
      "title" to title,
      "description" to description,
      "status" to "open",
      "createdAt" to FieldValue.serverTimestamp()
    )
    if (photoUrl.isNotBlank()) {
      map["photoUrl"] = photoUrl
    }
    if (adminResponse.isNotBlank()) {
      map["adminResponse"] = adminResponse
    }
    if (messages.isNotEmpty()) {
      map["messages"] = messages
    }
    return map
  }
}

data class Notice(
  val id: String = "",
  val title: String = "",
  val description: String = "",
  val date: String = "",
  val priority: String = "normal",
  val postedBy: String = "",
  val createdAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "title" to title,
      "description" to description,
      "date" to date,
      "priority" to priority,
      "createdAt" to FieldValue.serverTimestamp()
    )
    if (postedBy.isNotBlank()) {
      map["postedBy"] = postedBy
    }
    return map
  }
}

data class SocietyNotification(
  val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val type: String = "general",
  val referenceId: String = "",
  val isRead: Boolean = false,
  val createdAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "userId" to userId,
      "title" to title,
      "body" to body,
      "type" to type,
      "isRead" to isRead,
      "createdAt" to FieldValue.serverTimestamp()
    )
    if (referenceId.isNotBlank()) {
      map["referenceId"] = referenceId
    }
    return map
  }
}

