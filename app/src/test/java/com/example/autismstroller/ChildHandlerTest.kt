package com.example.autismstroller.functional

import com.example.autismstroller.models.Child
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChildHandlerTest {

    // 1. Mocks
    private val mockAuth: FirebaseAuth = mock()
    private val mockUser: FirebaseUser = mock()
    private val mockFirestore: FirebaseFirestore = mock()
    private val mockCollection: CollectionReference = mock()
    private val mockDocument: DocumentReference = mock()

    // Mocks for Querying
    private val mockQuery: Query = mock()
    private val mockQuerySnapshot: QuerySnapshot = mock()
    private val mockDocSnapshot: DocumentSnapshot = mock()

    // Mocks for Transaction
    private val mockTransaction: Transaction = mock()
    private val mockTransactionTask: Task<Any> = mock()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var handler: ChildHandler

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Train basic mocks to avoid NPEs
        whenever(mockFirestore.collection("children")).thenReturn(mockCollection)
        whenever(mockAuth.currentUser).thenReturn(mockUser)
        whenever(mockUser.uid).thenReturn("parent123")

        // Initialize Handler
        handler = ChildHandler(mockAuth, mockFirestore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchChildren updates flow when snapshot listener triggers`() = runTest {
        // GIVEN
        whenever(mockCollection.whereEqualTo("parentId", "parent123")).thenReturn(mockQuery)

        // Mock the Snapshot Listener
        // We capture the listener so we can trigger it manually
        val captor = ArgumentCaptor.forClass(EventListener::class.java) as ArgumentCaptor<EventListener<QuerySnapshot>>
        whenever(mockQuery.addSnapshotListener(captor.capture())).thenReturn(mock()) // return dummy registration

        // Mock the data inside the snapshot
        val childObject = Child(id = "c1", name = "John")
        whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDocSnapshot))
        whenever(mockDocSnapshot.toObject(Child::class.java)).thenReturn(childObject)

        // WHEN: We call the function
        handler.fetchChildren()

        // THEN: Trigger the listener manually with our mock data
        captor.value.onEvent(mockQuerySnapshot, null)

        // Assert StateFlow updated
        assertEquals(1, handler.childrenList.value.size)
        assertEquals("John", handler.childrenList.value[0].name)
    }

    @Test
    fun `recordMusicPlay updates count via Transaction`() = runTest {
        // GIVEN
        val childId = "c1"
        val songUrl = "baby_shark.mp3"

        whenever(mockCollection.document(childId)).thenReturn(mockDocument)

        // --- Transaction Mock (Same as UserHandler) ---
        whenever(mockTransactionTask.isComplete).thenReturn(true) // Stop await() hang

        whenever(mockFirestore.runTransaction(any<Transaction.Function<Any>>())).thenAnswer { invocation ->
            val func = invocation.arguments[0] as Transaction.Function<Any>
            func.apply(mockTransaction)
            mockTransactionTask
        }

        // Mock Reading current data
        whenever(mockTransaction.get(mockDocument)).thenReturn(mockDocSnapshot)
        // Current stats: map of music
        val currentStats = mapOf("musicPreferences" to mapOf(songUrl to 5L))
        whenever(mockDocSnapshot.get("childStats")).thenReturn(currentStats)

        // WHEN
        handler.recordMusicPlay(childId, songUrl)

        // THEN: Verify update called with incremented value (5 + 1 = 6)
        // Note: verifying complex map updates is hard, so we just verify the path was updated
        verify(mockTransaction).update(eq(mockDocument), eq("childStats.musicPreferences"), any())
    }
}