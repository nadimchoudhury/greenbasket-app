package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Product
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class GroceryRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndObserveProducts_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GroceryRepository(firestore, auth)
        val uniqueId = "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val newProduct = Product(
            id = uniqueId,
            userId = aliceUid,
            name = "Organic Honeycrisp Apples",
            barcode = "8901234999",
            category = "Produce",
            unit = "kg",
            costPrice = 2.0,
            sellingPrice = 3.8,
            discountPercent = 10.0,
            stockQuantity = 40L,
            reorderThreshold = 10L,
            reorderQuantity = 25L
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.upsertProduct(newProduct) }
        assertTrue("Expected product creation to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProducts(aliceUid).first { list -> list.any { it.id == uniqueId } }
        }
        assertTrue(emitted.any { it.id == uniqueId && it.name == "Organic Honeycrisp Apples" })
    }

    @Test
    fun getProductById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GroceryRepository(firestore, auth)
        val uniqueId = "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val product = Product(
            id = uniqueId,
            userId = aliceUid,
            name = "Almond Milk",
            barcode = "8901234888",
            category = "Dairy",
            unit = "bottle",
            costPrice = 1.8,
            sellingPrice = 3.2,
            discountPercent = 0.0,
            stockQuantity = 15L,
            reorderThreshold = 5L,
            reorderQuantity = 20L
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.upsertProduct(product).getOrThrow() }

        // Switch to Bob and attempt to read Alice's product
        signInTestUser(BOB_EMAIL)
        val bobRepo = GroceryRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProductById(aliceUid, uniqueId)
        }
        assertTrue("Expected cross-user read to fail", crossReadResult.isFailure)
        val fsEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeProducts_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = GroceryRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProducts(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected unauthenticated observation to throw", caught)
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_store@test.com"
        const val BOB_EMAIL = "bob_store@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
