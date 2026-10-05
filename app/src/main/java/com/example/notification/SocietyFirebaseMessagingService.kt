package com.example.notification

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SocietyFirebaseMessagingService : FirebaseMessagingService() {

  companion object {
    private const val TAG = "SocietyFCMService"
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "New FCM Token received: $token")
    val currentUser = Firebase.auth.currentUser
    if (currentUser != null) {
      Firebase.firestore.collection("users")
        .document(currentUser.uid)
        .update("fcmToken", token)
        .addOnSuccessListener {
          Log.d(TAG, "FCM token updated in Firestore for user: ${currentUser.uid}")
        }
        .addOnFailureListener { e ->
          Log.w(TAG, "Failed to update FCM token in Firestore: ${e.message}")
        }
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.d(TAG, "Push message received from: ${remoteMessage.from}")

    // Extract title and body from notification payload or data payload
    val title = remoteMessage.notification?.title
      ?: remoteMessage.data["title"]
      ?: "Society Notification"

    val body = remoteMessage.notification?.body
      ?: remoteMessage.data["body"]
      ?: remoteMessage.data["message"]
      ?: "You have a new update from Hanuman Nagar Vikas Samiti."

    NotificationHelper.showNotification(
      context = applicationContext,
      title = title,
      body = body
    )
  }
}
