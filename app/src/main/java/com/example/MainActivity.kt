package com.example

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.SocietyRepository
import com.example.ui.SocietyViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          SocietyApp()
        }
      }
    }
  }
}

@Composable
fun SocietyApp() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

  // Listen to Auth state changes
  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  // Attempt background silent auto-sign in if authorized account exists
  LaunchedEffect(Unit) {
    if (currentUser == null) {
      attemptSilentAutoSignIn(
        context = context,
        scope = coroutineScope,
        onSuccess = { currentUser = Firebase.auth.currentUser }
      )
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = { currentUser = Firebase.auth.currentUser }
    )
  } else {
    // Authenticated user: Initialize ViewModel with custom database ID
    val databaseId = context.getString(R.string.firestore_database_id)
    val viewModel: SocietyViewModel = viewModel(
      key = user.uid,
      factory = viewModelFactory {
        initializer {
          val app = checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
          val dbId = app.getString(R.string.firestore_database_id)
          val firestore = FirebaseFirestore.getInstance(dbId)
          val repository = SocietyRepository(firestore)
          SocietyViewModel(
            repository = repository,
            currentUserId = user.uid,
            currentUserEmail = user.email ?: "",
            currentUserName = user.displayName ?: user.email?.substringBefore("@") ?: "Resident"
          )
        }
      }
    )

    MainScreen(
      viewModel = viewModel,
      onSignedOut = {
        currentUser = null
      }
    )
  }
}

private fun attemptSilentAutoSignIn(
  context: Context,
  scope: CoroutineScope,
  onSuccess: () -> Unit
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    Log.d(TAG, "default_web_client_id not found for auto sign-in")
    return
  }

  val credentialManager = CredentialManager.create(context)
  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onSuccess()
      }
    } catch (_: Exception) {
      // Silent auto sign-in failed or no prior account; user can use interactive button
    }
  }
}
