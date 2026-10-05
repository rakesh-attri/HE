package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Complaint
import com.example.data.model.ComplaintChatEntry
import com.example.data.model.MaintenanceRecord
import com.example.data.model.Notice
import com.example.data.model.SocietyExpense
import com.example.data.model.SocietyNotification
import com.example.data.model.SocietySettings
import com.example.data.model.UserProfile
import com.example.data.repository.SocietyRepository
import com.example.notification.NotificationHelper
import com.example.ui.util.SoundHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SocietyViewModel"

sealed interface UiMessage {
  data class Success(val message: String) : UiMessage
  data class Error(val message: String) : UiMessage
}

@OptIn(ExperimentalCoroutinesApi::class)
class SocietyViewModel(
  private val repository: SocietyRepository,
  val currentUserId: String,
  val currentUserEmail: String,
  val currentUserName: String
) : ViewModel() {

  private val _uiMessage = MutableStateFlow<UiMessage?>(null)
  val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  // Local role override for testing / switching between Resident and Admin views
  private val _activeRoleOverride = MutableStateFlow<String?>(null)
  val activeRoleOverride: StateFlow<String?> = _activeRoleOverride.asStateFlow()

  // Selected user for payment details inspection dialog in Admin
  private val _inspectedUser = MutableStateFlow<UserProfile?>(null)
  val inspectedUser: StateFlow<UserProfile?> = _inspectedUser.asStateFlow()

  // User Profile
  val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
    .catch { e ->
      Log.e(TAG, "Error observing user profile", e)
      emit(null)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null
    )

  // All registered society members
  val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsers()
    .catch { e ->
      Log.e(TAG, "Error observing all users", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  // Society Settings
  val societySettings: StateFlow<SocietySettings> = repository.observeSocietySettings()
    .catch { e ->
      Log.e(TAG, "Error observing settings", e)
      emit(SocietySettings())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = SocietySettings()
    )

  // Resident Maintenance Records
  val residentMaintenance: StateFlow<List<MaintenanceRecord>> = repository.observeUserMaintenance(currentUserId)
    .catch { e ->
      Log.e(TAG, "Error observing user maintenance", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  private val _approvalsRefreshTrigger = MutableStateFlow(0)

  // Admin Pending Maintenance Records (Supports live push and manual refresh)
  val pendingMaintenance: StateFlow<List<MaintenanceRecord>> = _approvalsRefreshTrigger
    .flatMapLatest { repository.observePendingMaintenance() }
    .catch { e ->
      Log.e(TAG, "Error observing pending maintenance", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  // All Maintenance (for household-wise history & auditing, supports live push & manual refresh)
  val allMaintenance: StateFlow<List<MaintenanceRecord>> = _approvalsRefreshTrigger
    .flatMapLatest { repository.observeAllMaintenance() }
    .catch { e ->
      Log.e(TAG, "Error observing all maintenance", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  // Society Expenses
  val societyExpenses: StateFlow<List<SocietyExpense>> = repository.observeSocietyExpenses()
    .catch { e ->
      Log.e(TAG, "Error observing expenses", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  // Society Complaints
  val complaints: StateFlow<List<Complaint>> = repository.observeComplaints()
    .catch { e ->
      Log.e(TAG, "Error observing complaints", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  // Society Notices
  val notices: StateFlow<List<Notice>> = repository.observeNotices()
    .catch { e ->
      Log.e(TAG, "Error observing notices", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  // Real-time Push Notifications for current user
  val notifications: StateFlow<List<SocietyNotification>> = repository.observeUserNotifications(currentUserId)
    .catch { e ->
      Log.e(TAG, "Error observing notifications", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val unreadNotificationsCount: StateFlow<Int> = notifications
    .map { list -> list.count { !it.isRead } }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = 0
    )

  init {
    viewModelScope.launch {
      val isBootstrapAdmin = currentUserEmail.equals("therakeshattri@gmail.com", ignoreCase = true)
      val initialRole = if (isBootstrapAdmin) "admin" else "resident"
      val initialApproval = if (isBootstrapAdmin) "approved" else "pending"

      val initialProfile = UserProfile(
        userId = currentUserId,
        name = currentUserName.ifBlank { "Resident" },
        email = currentUserEmail,
        flatNumber = if (isBootstrapAdmin) "Plot 114 (Admin Office)" else "",
        role = initialRole,
        approvalStatus = initialApproval
      )
      repository.saveUserProfile(initialProfile)

      // Ensure society settings doc exists
      repository.updateSocietySettings(SocietySettings())
    }

    // Initialize Firebase Cloud Messaging registration token & topics
    try {
      FirebaseMessaging.getInstance().subscribeToTopic("all_residents")
      FirebaseMessaging.getInstance().subscribeToTopic("society_notices")

      FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val token = task.result
          Log.d(TAG, "FCM registration token acquired: $token")
          viewModelScope.launch {
            repository.saveFcmToken(token)
          }
        } else {
          Log.d(TAG, "FCM token registration status: ${task.exception?.message}")
        }
      }
    } catch (e: Exception) {
      Log.d(TAG, "FCM initialization note: ${e.message}")
    }

    // Real-time push notification dispatcher with persistent deduplication
    val appLaunchTime = System.currentTimeMillis()
    viewModelScope.launch {
      val context = FirebaseApp.getInstance().applicationContext
      val prefs = context.getSharedPreferences("hnvs_notifications_seen", Context.MODE_PRIVATE)
      val seenSet = prefs.getStringSet("seen_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

      notifications.collect { list ->
        if (list.isEmpty()) return@collect

        for (notif in list) {
          val notifTime = notif.createdAt?.toDate()?.time ?: 0L

          // Ignore already read, already seen, or notifications that existed prior to app launch
          if (notif.isRead || seenSet.contains(notif.id)) continue
          if (notifTime < appLaunchTime - 15_000L) {
            // Created before this session began: mark seen silently so it never spams on app restart
            seenSet.add(notif.id)
            prefs.edit().putStringSet("seen_ids", seenSet).apply()
            continue
          }

          // Newly arrived live notification!
          seenSet.add(notif.id)
          prefs.edit().putStringSet("seen_ids", seenSet).apply()

          try {
            NotificationHelper.showNotification(
              context = context,
              title = notif.title,
              body = notif.body
            )

            // Play 3-second urgent sound if notice is urgent
            val isUrgent = notif.type == "notice_published" && (
              notif.title.contains("Urgent", ignoreCase = true) ||
              notif.body.contains("Urgent", ignoreCase = true)
            )
            if (isUrgent) {
              SoundHelper.playUrgentAlertTone(3000)
            }
          } catch (e: Exception) {
            Log.e(TAG, "Could not trigger local notification: ${e.message}")
          }
        }
      }
    }
  }

  fun markAllNotificationsAsRead() {
    viewModelScope.launch {
      val unreadList = notifications.value.filter { !it.isRead }
      for (notif in unreadList) {
        if (notif.id.isNotBlank()) {
          repository.markNotificationAsRead(notif.id)
        }
      }
    }
  }

  fun markNotificationAsRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationAsRead(notificationId)
    }
  }

  fun clearMessage() {
    _uiMessage.value = null
  }

  fun inspectUserPayments(user: UserProfile?) {
    _inspectedUser.value = user
  }

  fun toggleRoleOverride() {
    val profile = userProfile.value
    val isRealAdmin = profile?.isAdmin == true || currentUserEmail.equals("therakeshattri@gmail.com", ignoreCase = true)
    if (!isRealAdmin) {
      Log.w(TAG, "Non-admin attempted to toggle role override. Denied.")
      return
    }
    val currentRole = effectiveRole()
    val nextRole = if (currentRole == "admin") "resident" else "admin"
    _activeRoleOverride.value = nextRole
    _uiMessage.value = UiMessage.Success("Switched view to $nextRole mode")
  }

  fun effectiveRole(): String {
    _activeRoleOverride.value?.let { return it }
    val profile = userProfile.value
    if (profile != null) {
      if (profile.isAdmin) return "admin"
      return profile.role
    }
    if (currentUserEmail.equals("therakeshattri@gmail.com", ignoreCase = true)) {
      return "admin"
    }
    return "resident"
  }

  fun updateProfile(name: String, flatNumber: String, role: String, mobileNumber: String = "") {
    viewModelScope.launch {
      val houseNo = flatNumber.trim()
      if (houseNo.isNotBlank()) {
        val isTaken = repository.isHouseNoTaken(houseNo, currentUserId)
        if (isTaken) {
          _uiMessage.value = UiMessage.Error("House No. '$houseNo' is already registered by another resident. House numbers must be unique.")
          return@launch
        }
      }
      _isSubmitting.value = true
      val existingApproval = userProfile.value?.approvalStatus ?: "pending"
      val existingMobile = userProfile.value?.mobileNumber ?: ""
      val finalMobile = mobileNumber.trim().ifBlank { existingMobile }
      val updated = UserProfile(
        userId = currentUserId,
        name = name.trim(),
        email = currentUserEmail,
        flatNumber = houseNo,
        mobileNumber = finalMobile,
        role = role,
        approvalStatus = existingApproval
      )
      val result = repository.saveUserProfile(updated)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Profile updated successfully")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to update profile: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun refreshApprovals() {
    _approvalsRefreshTrigger.value += 1
    _uiMessage.value = UiMessage.Success("Approvals synced with database")
  }

  // --- MEMBER MANAGEMENT (ADMIN) ---

  fun makeUserAdmin(targetUserId: String, targetName: String) {
    viewModelScope.launch {
      val result = repository.updateUserRoleAndApproval(targetUserId, "admin", "approved")
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("$targetName promoted to Admin!")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to promote user: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun makeUserResident(targetUserId: String, targetName: String) {
    viewModelScope.launch {
      val result = repository.updateUserRoleAndApproval(targetUserId, "resident", "approved")
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("$targetName role updated to Resident")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to change role: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun approveResidentMembership(targetUserId: String, targetName: String) {
    viewModelScope.launch {
      val result = repository.updateUserRoleAndApproval(targetUserId, "resident", "approved")
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("$targetName membership approved!")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to approve membership: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun rejectResidentMembership(targetUserId: String, targetName: String) {
    viewModelScope.launch {
      val result = repository.updateUserRoleAndApproval(targetUserId, "resident", "rejected")
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("$targetName membership rejected")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to reject membership: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun blockUser(targetUserId: String, targetName: String) {
    viewModelScope.launch {
      val result = repository.updateUserRoleAndApproval(targetUserId, "resident", "blocked")
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("$targetName has been blocked from payments and all services")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to block user: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun unblockUser(targetUserId: String, targetName: String) {
    viewModelScope.launch {
      val result = repository.updateUserRoleAndApproval(targetUserId, "resident", "approved")
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("$targetName has been unblocked")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to unblock user: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun deleteUser(targetUserId: String, targetName: String) {
    viewModelScope.launch {
      val result = repository.deleteUser(targetUserId)
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("$targetName deleted from society database")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to delete user: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  // --- MAINTENANCE & PAYMENTS ---

  fun submitMaintenancePayment(
    monthYear: String,
    amount: Double,
    utrNumber: String,
    receiptImageUrl: String = ""
  ) {
    if (utrNumber.trim().length < 4) {
      _uiMessage.value = UiMessage.Error("Please enter a valid UPI UTR / Reference number")
      return
    }
    viewModelScope.launch {
      _isSubmitting.value = true
      val profile = userProfile.value
      val record = MaintenanceRecord(
        userId = currentUserId,
        userName = profile?.name?.ifBlank { currentUserName } ?: currentUserName,
        flatNumber = profile?.flatNumber?.ifBlank { "House 1" } ?: "House 1",
        userMobile = profile?.mobileNumber ?: "",
        receiptImageUrl = receiptImageUrl,
        monthYear = monthYear,
        amount = amount,
        utrNumber = utrNumber.trim(),
        status = "pending"
      )
      val result = repository.submitMaintenance(record)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Payment submitted! Admin will verify your UTR & receipt.")
      } else {
        _uiMessage.value = UiMessage.Error("Submission failed: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  /**
   * Auto-generate payment record when resident pays via QR code / UPI Intent
   */
  fun autoGenerateQrPayment(monthYear: String, amount: Double) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val profile = userProfile.value
      val autoRef = "UPI-QR-${System.currentTimeMillis().toString().takeLast(6)}"
      val record = MaintenanceRecord(
        userId = currentUserId,
        userName = profile?.name?.ifBlank { currentUserName } ?: currentUserName,
        flatNumber = profile?.flatNumber?.ifBlank { "Flat 101" } ?: "Flat 101",
        userMobile = profile?.mobileNumber ?: "",
        monthYear = monthYear,
        amount = amount,
        utrNumber = autoRef,
        status = "pending",
        adminRemarks = "Auto-generated via QR scanner"
      )
      val result = repository.submitMaintenance(record)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Payment auto-submitted! Tracking Ref: $autoRef")
      } else {
        _uiMessage.value = UiMessage.Error("Auto-submission failed: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun approveMaintenancePayment(recordId: String, remarks: String, residentName: String, flat: String) {
    viewModelScope.launch {
      val result = repository.approveMaintenance(recordId, remarks)
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Approved payment for $residentName ($flat)! Ready to share receipt.")
      } else {
        _uiMessage.value = UiMessage.Error("Approval failed: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun declineMaintenancePayment(recordId: String, remarks: String, residentName: String, flat: String) {
    viewModelScope.launch {
      val result = repository.declineMaintenance(recordId, remarks)
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Declined payment for $residentName ($flat)")
      } else {
        _uiMessage.value = UiMessage.Error("Decline failed: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  // --- NOTICES ---

  fun addNotice(title: String, description: String, date: String, priority: String) {
    if (title.isBlank() || description.isBlank()) {
      _uiMessage.value = UiMessage.Error("Please enter notice title and description")
      return
    }
    viewModelScope.launch {
      _isSubmitting.value = true
      val notice = Notice(
        title = title.trim(),
        description = description.trim(),
        date = date.trim(),
        priority = priority,
        postedBy = currentUserId
      )
      val result = repository.addNotice(notice)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Notice published to Society Board")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to publish notice: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun deleteNotice(noticeId: String) {
    viewModelScope.launch {
      val result = repository.deleteNotice(noticeId)
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Notice removed")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to delete notice: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  // --- EXPENSES ---

  fun addExpense(
    title: String,
    category: String,
    amount: Double,
    date: String
  ) {
    if (title.isBlank() || amount <= 0) {
      _uiMessage.value = UiMessage.Error("Please enter valid expense title and amount")
      return
    }
    viewModelScope.launch {
      _isSubmitting.value = true
      val expense = SocietyExpense(
        title = title.trim(),
        category = category.trim(),
        amount = amount,
        date = date.trim(),
        loggedBy = currentUserId
      )
      val result = repository.addSocietyExpense(expense)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Expense ₹$amount added to transparency ledger")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to add expense: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun deleteExpense(expenseId: String) {
    viewModelScope.launch {
      val result = repository.deleteSocietyExpense(expenseId)
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Expense deleted")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to delete expense: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  // --- SETTINGS ---

  fun updateSocietySettings(
    upiId: String,
    societyName: String,
    qrUrl: String,
    monthlyAmount: Double
  ) {
    if (upiId.isBlank() || societyName.isBlank()) {
      _uiMessage.value = UiMessage.Error("UPI ID and Society Name cannot be empty")
      return
    }
    viewModelScope.launch {
      _isSubmitting.value = true
      val updated = SocietySettings(
        upiId = upiId.trim(),
        societyName = societyName.trim(),
        qrCodeImageUrl = qrUrl.trim(),
        monthlyMaintenanceAmount = monthlyAmount
      )
      val result = repository.updateSocietySettings(updated)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Society UPI and QR settings saved")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to save settings: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  // --- COMPLAINTS & ADMIN RESPONSES ---

  fun submitComplaint(title: String, description: String, photoUrl: String = "") {
    if (title.isBlank() || description.isBlank()) {
      _uiMessage.value = UiMessage.Error("Please provide both title and description for complaint")
      return
    }
    viewModelScope.launch {
      _isSubmitting.value = true
      val profile = userProfile.value
      val complaint = Complaint(
        userId = currentUserId,
        userName = profile?.name?.ifBlank { currentUserName } ?: currentUserName,
        flatNumber = profile?.flatNumber?.ifBlank { "House 1" } ?: "House 1",
        title = title.trim(),
        description = description.trim(),
        photoUrl = photoUrl,
        status = "open"
      )
      val result = repository.submitComplaint(complaint)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Complaint logged successfully with live photo proof.")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to submit complaint: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun sendComplaintMessage(complaintId: String, messageText: String) {
    val clean = messageText.trim()
    if (clean.isBlank()) return
    viewModelScope.launch {
      val profile = userProfile.value
      val senderRole = effectiveRole()
      val senderName = if (senderRole == "admin") {
        "${profile?.name?.ifBlank { "Admin" } ?: "Admin"} (Admin)"
      } else {
        "${profile?.name?.ifBlank { "Resident" } ?: "Resident"} (House ${profile?.flatNumber ?: ""})"
      }
      val chatEntry = ComplaintChatEntry(
        senderId = currentUserId,
        senderName = senderName,
        senderRole = senderRole,
        message = clean,
        timeMillis = System.currentTimeMillis()
      )
      val result = repository.addComplaintChatMessage(complaintId, chatEntry)
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Message posted")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to send reply: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }

  fun respondToComplaint(complaintId: String, adminResponse: String, status: String) {
    if (adminResponse.isBlank()) {
      _uiMessage.value = UiMessage.Error("Please write an action/response message")
      return
    }
    viewModelScope.launch {
      _isSubmitting.value = true
      val result = repository.respondToComplaint(complaintId, adminResponse.trim(), status)
      _isSubmitting.value = false
      if (result.isSuccess) {
        _uiMessage.value = UiMessage.Success("Response posted & complaint marked as $status")
      } else {
        _uiMessage.value = UiMessage.Error("Failed to update complaint: ${result.exceptionOrNull()?.localizedMessage}")
      }
    }
  }
}
