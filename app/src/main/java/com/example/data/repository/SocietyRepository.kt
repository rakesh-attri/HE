package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.Complaint
import com.example.data.model.ComplaintChatEntry
import com.example.data.model.MaintenanceRecord
import com.example.data.model.Notice
import com.example.data.model.SocietyExpense
import com.example.data.model.SocietyNotification
import com.example.data.model.SocietySettings
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "SocietyRepository"

class SocietyRepository(val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- USER PROFILE & MEMBERS ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val docRef = db.collection("users").document(userId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            val user = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            user?.copy(userId = snapshot.id)
          } else {
            null
          }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
          throw error
        }
    )
  }

  fun observeAllUsers(): Flow<List<UserProfile>> = flow {
    val colRef = db.collection("users")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(userId = doc.id)
          }.sortedBy { it.flatNumber }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
          emit(emptyList())
        }
    )
  }

  suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        val existingRole = existing.getString("role") ?: "resident"
        val existingApproval = existing.getString("approvalStatus") ?: "approved"
        val existingMobile = existing.getString("mobileNumber") ?: ""
        val updated = profile.copy(
          role = if (existingRole == "admin") "admin" else profile.role,
          approvalStatus = existingApproval,
          mobileNumber = profile.mobileNumber.ifBlank { existingMobile }
        )
        docRef.update(updated.toUpdateMap()).await()
      } else {
        docRef.set(profile.toCreateMap()).await()
      }
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserRoleAndApproval(
    targetUserId: String,
    newRole: String,
    newApprovalStatus: String
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("users").document(targetUserId)
    return try {
      docRef.update(
        mapOf(
          "role" to newRole,
          "approvalStatus" to newApprovalStatus,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteUser(targetUserId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("users").document(targetUserId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- SOCIETY SETTINGS ---

  fun observeSocietySettings(): Flow<SocietySettings> = flow {
    val docRef = db.collection("society_settings").document("current")
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            val upiId = snapshot.getString("upiId") ?: "hanumannagar@upi"
            val qr = snapshot.getString("qrCodeImageUrl") ?: ""
            val name = snapshot.getString("societyName") ?: "हनुमान नगर विस्तार 1 विकास समिति"
            val amount = snapshot.getDouble("monthlyMaintenanceAmount") ?: 1000.0
            val updatedAt = snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            SocietySettings(
              upiId = upiId,
              qrCodeImageUrl = qr,
              societyName = name,
              monthlyMaintenanceAmount = amount,
              updatedAt = updatedAt
            )
          } else {
            SocietySettings()
          }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
          emit(SocietySettings())
        }
    )
  }

  suspend fun updateSocietySettings(settings: SocietySettings): Result<Unit> {
    requireUserId()
    val docRef = db.collection("society_settings").document("current")
    return try {
      docRef.set(settings.toMap(), SetOptions.merge()).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  // --- MAINTENANCE RECORDS ---

  fun observeUserMaintenance(userId: String): Flow<List<MaintenanceRecord>> = flow {
    val colRef = db.collection("maintenance_records")
      .whereEqualTo("userId", userId)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(MaintenanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
          }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, "maintenance_records")
          throw error
        }
    )
  }

  fun observePendingMaintenance(): Flow<List<MaintenanceRecord>> = flow {
    val colRef = db.collection("maintenance_records")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(MaintenanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
          }.filter { it.status.equals("pending", ignoreCase = true) }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, "maintenance_records")
          emit(emptyList())
        }
    )
  }

  fun observeAllMaintenance(): Flow<List<MaintenanceRecord>> = flow {
    val colRef = db.collection("maintenance_records")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(MaintenanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
          }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, "maintenance_records")
          emit(emptyList())
        }
    )
  }

  suspend fun submitMaintenance(record: MaintenanceRecord): Result<String> {
    val uid = requireUserId()
    val colRef = db.collection("maintenance_records")
    return try {
      val payload = record.copy(userId = uid, status = "pending").toCreateMap()
      val doc = colRef.add(payload).await()
      Result.success(doc.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun approveMaintenance(recordId: String, remarks: String = ""): Result<Unit> {
    requireUserId()
    val docRef = db.collection("maintenance_records").document(recordId)
    return try {
      val existing = docRef.get().await()
      val updates = mutableMapOf<String, Any>(
        "status" to "approved",
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (remarks.isNotBlank()) {
        updates["adminRemarks"] = remarks
      }
      docRef.update(updates).await()

      // Send real-time notification to the resident
      val recipientUid = existing.getString("userId") ?: ""
      val monthYear = existing.getString("monthYear") ?: "Maintenance"
      val amount = existing.getDouble("amount") ?: 1000.0
      if (recipientUid.isNotBlank()) {
        createNotification(
          SocietyNotification(
            userId = recipientUid,
            title = "Payment Approved! ₹${amount.toInt()}",
            body = "Your maintenance payment of ₹${amount.toInt()} for $monthYear has been verified and approved by Admin.",
            type = "payment_approved",
            referenceId = recordId
          )
        )
      }

      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun declineMaintenance(recordId: String, remarks: String = ""): Result<Unit> {
    requireUserId()
    val docRef = db.collection("maintenance_records").document(recordId)
    return try {
      val existing = docRef.get().await()
      val updates = mutableMapOf<String, Any>(
        "status" to "declined",
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (remarks.isNotBlank()) {
        updates["adminRemarks"] = remarks
      }
      docRef.update(updates).await()

      // Send real-time notification to the resident
      val recipientUid = existing.getString("userId") ?: ""
      val monthYear = existing.getString("monthYear") ?: "Maintenance"
      if (recipientUid.isNotBlank()) {
        createNotification(
          SocietyNotification(
            userId = recipientUid,
            title = "Payment Verification Alert",
            body = "Your payment for $monthYear was not approved. Remarks: ${remarks.ifBlank { "Please re-check UTR" }}",
            type = "payment_declined",
            referenceId = recordId
          )
        )
      }

      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- SOCIETY EXPENSES ---

  fun observeSocietyExpenses(): Flow<List<SocietyExpense>> = flow {
    val colRef = db.collection("society_expenses")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            val title = doc.getString("title") ?: ""
            val category = doc.getString("category") ?: "General"
            val amount = doc.getDouble("amount") ?: 0.0
            val date = doc.getString("date") ?: ""
            val loggedBy = doc.getString("loggedBy") ?: ""
            val createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            SocietyExpense(
              id = doc.id,
              title = title,
              category = category,
              amount = amount,
              date = date,
              loggedBy = loggedBy,
              createdAt = createdAt
            )
          }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
          emit(emptyList())
        }
    )
  }

  suspend fun addSocietyExpense(expense: SocietyExpense): Result<String> {
    val uid = requireUserId()
    val colRef = db.collection("society_expenses")
    return try {
      val payload = expense.copy(loggedBy = uid).toCreateMap()
      val doc = colRef.add(payload).await()
      Result.success(doc.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteSocietyExpense(expenseId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("society_expenses").document(expenseId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- COMPLAINTS ---

  fun observeComplaints(): Flow<List<Complaint>> = flow {
    val colRef = db.collection("complaints")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
          }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
          emit(emptyList())
        }
    )
  }

  fun observeUserComplaints(userId: String): Flow<List<Complaint>> = flow {
    val colRef = db.collection("complaints")
      .whereEqualTo("userId", userId)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
          }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, "complaints")
          emit(emptyList())
        }
    )
  }

  suspend fun submitComplaint(complaint: Complaint): Result<String> {
    val uid = requireUserId()
    val colRef = db.collection("complaints")
    return try {
      val payload = complaint.copy(userId = uid, status = "open").toCreateMap()
      val doc = colRef.add(payload).await()
      Result.success(doc.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun respondToComplaint(complaintId: String, adminResponse: String, status: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("complaints").document(complaintId)
    return try {
      val existing = docRef.get().await()
      val updates = mutableMapOf<String, Any>(
        "status" to status,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (adminResponse.isNotBlank()) {
        updates["adminResponse"] = adminResponse
      }
      docRef.update(updates).await()

      // Send real-time notification to the resident who raised the complaint
      val recipientUid = existing.getString("userId") ?: ""
      val complaintTitle = existing.getString("title") ?: "Society Complaint"
      val statusDesc = if (status.equals("resolved", ignoreCase = true)) "RESOLVED" else "UPDATED"
      if (recipientUid.isNotBlank()) {
        createNotification(
          SocietyNotification(
            userId = recipientUid,
            title = "Complaint $statusDesc: $complaintTitle",
            body = if (adminResponse.isNotBlank()) "Admin response: $adminResponse" else "Your complaint has been marked as $status.",
            type = "complaint_updated",
            referenceId = complaintId
          )
        )
      }

      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addComplaintChatMessage(
    complaintId: String,
    chatEntry: ComplaintChatEntry
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("complaints").document(complaintId)
    return try {
      val existing = docRef.get().await()
      docRef.update(
        mapOf(
          "messages" to FieldValue.arrayUnion(chatEntry.toMap()),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      // Notify the recipient
      val residentId = existing.getString("userId") ?: ""
      val complaintTitle = existing.getString("title") ?: "Complaint"
      val isSenderAdmin = chatEntry.senderRole.equals("admin", ignoreCase = true)
      val recipientId = if (isSenderAdmin) residentId else ""
      if (recipientId.isNotBlank() && recipientId != chatEntry.senderId) {
        createNotification(
          SocietyNotification(
            userId = recipientId,
            title = "Reply on Complaint: $complaintTitle",
            body = "${chatEntry.senderName}: ${chatEntry.message}",
            type = "complaint_updated",
            referenceId = complaintId
          )
        )
      }
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun isHouseNoTaken(houseNo: String, excludeUserId: String): Boolean {
    val clean = houseNo.trim()
    if (clean.isBlank()) return false
    return try {
      val snapshot = db.collection("users").get().await()
      snapshot.documents.any { doc ->
        val docUid = doc.getString("userId") ?: doc.id
        val docFlat = (doc.getString("flatNumber") ?: "").trim()
        docUid != excludeUserId && docFlat.equals(clean, ignoreCase = true)
      }
    } catch (e: Exception) {
      false
    }
  }

  // --- NOTIFICATIONS & FCM ---

  suspend fun saveFcmToken(token: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.success(Unit)
    val docRef = db.collection("users").document(uid)
    return try {
      docRef.update("fcmToken", token).await()
      Result.success(Unit)
    } catch (e: Exception) {
      // Non-fatal if document is not yet initialized
      Result.failure(e)
    }
  }

  suspend fun createNotification(notification: SocietyNotification): Result<String> {
    val colRef = db.collection("notifications")
    return try {
      val doc = colRef.add(notification.toCreateMap()).await()
      Result.success(doc.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, colRef.path)
      Result.failure(e)
    }
  }

  fun observeUserNotifications(userId: String): Flow<List<SocietyNotification>> = flow {
    val colRef = db.collection("notifications")
      .whereEqualTo("userId", userId)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(SocietyNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
          }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, "notifications")
          emit(emptyList())
        }
    )
  }

  suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
    val docRef = db.collection("notifications").document(notificationId)
    return try {
      docRef.update("isRead", true).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- NOTICES ---

  fun observeNotices(): Flow<List<Notice>> = flow {
    val colRef = db.collection("notices")
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(Notice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.copy(id = doc.id)
          }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
          emit(emptyList())
        }
    )
  }

  suspend fun addNotice(notice: Notice): Result<String> {
    val uid = requireUserId()
    val colRef = db.collection("notices")
    return try {
      val payload = notice.copy(postedBy = uid).toCreateMap()
      val doc = colRef.add(payload).await()
      val noticeId = doc.id

      // Send real-time notifications to all users (same as amount approval)
      try {
        val usersSnapshot = db.collection("users").get().await()
        for (userDoc in usersSnapshot.documents) {
          val recipientUid = userDoc.id
          if (recipientUid.isNotBlank()) {
            createNotification(
              SocietyNotification(
                userId = recipientUid,
                title = "New Notice: ${notice.title}",
                body = notice.description.take(150),
                type = "notice_published",
                referenceId = noticeId
              )
            )
          }
        }
      } catch (ne: Exception) {
        Log.w(TAG, "Failed sending notice notifications: ${ne.message}")
      }

      Result.success(noticeId)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, colRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteNotice(noticeId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("notices").document(noticeId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }
}
