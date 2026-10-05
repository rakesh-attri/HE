package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Complaint
import com.example.data.model.MaintenanceRecord
import com.example.data.repository.SocietyRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SocietyRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun resident_submitMaintenance_success(): Unit = runBlocking {
    val uid = signInTestUser("resident_test@society.com")
    val repo = SocietyRepository(firestore)

    val record = MaintenanceRecord(
      userId = uid,
      userName = "Ravi Kumar",
      flatNumber = "Plot 114, B-302",
      monthYear = "October 2026",
      amount = 1000.0,
      utrNumber = "428901234567",
      status = "pending"
    )

    val result = withTimeout(5000L) { repo.submitMaintenance(record) }
    assertTrue(result.isSuccess)
    val docId = result.getOrThrow()
    assertNotNull(docId)
    assertTrue(docId.isNotEmpty())
  }

  @Test
  fun resident_submitComplaint_success(): Unit = runBlocking {
    val uid = signInTestUser("complaint_user@society.com")
    val repo = SocietyRepository(firestore)

    val complaint = Complaint(
      userId = uid,
      userName = "Sunita Verma",
      flatNumber = "Flat 102",
      title = "Water supply motor timing",
      description = "Morning water pump was turned on late today.",
      status = "open"
    )

    val result = withTimeout(5000L) { repo.submitComplaint(complaint) }
    assertTrue(result.isSuccess)
  }

  @Test
  fun crossUser_cannotReadOthersMaintenance(): Unit = runBlocking {
    val aliceUid = signInTestUser("alice_maint@society.com")
    val aliceRepo = SocietyRepository(firestore)

    val record = MaintenanceRecord(
      userId = aliceUid,
      userName = "Alice",
      flatNumber = "Flat 101",
      monthYear = "October 2026",
      amount = 1000.0,
      utrNumber = "112233445566",
      status = "pending"
    )
    val aliceDocId = withTimeout(5000L) { aliceRepo.submitMaintenance(record).getOrThrow() }

    // Bob signs in and attempts to directly get Alice's record
    signInTestUser("bob_maint@society.com")
    try {
      withTimeout(5000L) {
        firestore.collection("maintenance_records").document(aliceDocId).get().await()
      }
      fail("Expected PERMISSION_DENIED when Bob accesses Alice's maintenance record")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun unauthenticated_cannotCreateMaintenance(): Unit = runBlocking {
    auth.signOut()
    try {
      withTimeout(5000L) {
        firestore.collection("maintenance_records").add(
          mapOf(
            "userId" to "anon_user",
            "userName" to "Anon",
            "flatNumber" to "Flat 000",
            "monthYear" to "October 2026",
            "amount" to 1000.0,
            "utrNumber" to "000000000000",
            "status" to "pending"
          )
        ).await()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated create")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }
}
