package com.example.autismstroller.functional

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UserHandlerTest {

    // 1. Mocks
    private val mockFirestore: FirebaseFirestore = mock()
    private val mockCollection: CollectionReference = mock()
    private val mockDocument: DocumentReference = mock()
    private val mockSnapshot: DocumentSnapshot = mock()

    private val mockTask: Task<Void> = mock()
    private val mockGetTask: Task<DocumentSnapshot> = mock()

    private val mockTransaction: Transaction = mock()
    private val mockTransactionTask: Task<Any> = mock()

    // 2. Declare Handler (Do not initialize yet!)
    private lateinit var handler: UserHandler

    @Before
    fun setup() {
        // 3. Train the Mock FIRST
        // This prevents the NullPointerException because the mock now knows what to return
        whenever(mockFirestore.collection("users")).thenReturn(mockCollection)

        // 4. Initialize Handler SECOND
        handler = UserHandler(mockFirestore)
    }

    @Test
    fun `addFriend adds uid to list if not present`() {
        // GIVEN
        val myUid = "Me"
        val newFriendUid = "B"
        val currentFriends = listOf("A")

        // Setup Document Mock
        whenever(mockCollection.document(myUid)).thenReturn(mockDocument)

        // Mock the get() call
        whenever(mockDocument.get()).thenReturn(mockGetTask)
        whenever(mockGetTask.addOnSuccessListener(any())).thenReturn(mockGetTask)

        // Mock the update() call
        whenever(mockDocument.update(eq("friends"), any())).thenReturn(mockTask)
        whenever(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)

        // Mock snapshot data
        whenever(mockSnapshot.exists()).thenReturn(true)
        whenever(mockSnapshot.get("friends")).thenReturn(currentFriends)

        // WHEN
        val getCaptor = ArgumentCaptor.forClass(OnSuccessListener::class.java) as ArgumentCaptor<OnSuccessListener<DocumentSnapshot>>

        handler.addFriend(myUid, newFriendUid) { count ->
            assertEquals(2, count)
        }

        // Trigger the 'get' success manually
        verify(mockGetTask).addOnSuccessListener(getCaptor.capture())
        getCaptor.value.onSuccess(mockSnapshot)

        // THEN
        verify(mockDocument).update("friends", listOf("A", "B"))
    }

    @Test
    fun `increment updates stat via Transaction`() = runTest {
        // GIVEN
        val uid = "user1"
        val statName = "likeCount"

        whenever(mockCollection.document(uid)).thenReturn(mockDocument)

        // --- FIX: Make the Mock Task look "Finished" ---
        // This stops await() from hanging!
        whenever(mockTransactionTask.isComplete).thenReturn(true)
        whenever(mockTransactionTask.isCanceled).thenReturn(false)
        whenever(mockTransactionTask.exception).thenReturn(null)
        // -----------------------------------------------

        // Mock Transaction Execution
        whenever(mockFirestore.runTransaction(any<Transaction.Function<Any>>())).thenAnswer { invocation ->
            val transactionFunction = invocation.arguments[0] as Transaction.Function<Any>
            transactionFunction.apply(mockTransaction)
            mockTransactionTask // Return our "Finished" task
        }

        // Mock Transaction Reads
        whenever(mockTransaction.get(mockDocument)).thenReturn(mockSnapshot)
        whenever(mockSnapshot.getLong("userStats.$statName")).thenReturn(10L)

        // WHEN
        handler.increment(uid, statName, 1)

        // THEN
        verify(mockTransaction).update(mockDocument, "userStats.$statName", 11L)
    }
}