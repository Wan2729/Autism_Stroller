package com.example.autismstroller.functional

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ForumHandlerTest {

    private val mockAuth: FirebaseAuth = mock()
    private val mockFirestore: FirebaseFirestore = mock()
    private val mockUser: FirebaseUser = mock()

    // Mocks for Firestore Chain
    private val mockCollection: CollectionReference = mock()
    private val mockDocument: DocumentReference = mock()
    private val mockVoidTask: Task<Void> = mock() // 'set' returns Task<Void>

    private val handler = ForumHandler(mockAuth, mockFirestore)

    @Test
    fun `create adds document to forums collection`() {
        // GIVEN: User is logged in
        whenever(mockAuth.currentUser).thenReturn(mockUser)
        whenever(mockUser.uid).thenReturn("user123")

        // --- FIX: Setup the COMPLETE Firestore Chain ---
        // 1. collection("forums") -> returns mockCollection
        whenever(mockFirestore.collection("forums")).thenReturn(mockCollection)

        // 2. document(any) -> returns mockDocument
        whenever(mockCollection.document(any())).thenReturn(mockDocument)

        // 3. set(any) -> returns mockVoidTask
        whenever(mockDocument.set(any())).thenReturn(mockVoidTask)

        // 4. orderBy(...) -> returns mockCollection (Query is a superclass of CollectionReference)
        // This is the missing link that caused the crash!
        whenever(mockCollection.orderBy(anyString(), any())).thenReturn(mockCollection)

        // 5. get() -> returns a Task (We need a new mock for the QuerySnapshot task)
        val mockQueryTask: Task<com.google.firebase.firestore.QuerySnapshot> = mock()
        whenever(mockCollection.get()).thenReturn(mockQueryTask)

        // Mock the listeners so they don't crash
        whenever(mockVoidTask.addOnSuccessListener(any())).thenReturn(mockVoidTask)
        whenever(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask)

        // Mock listeners for the get() call inside getForum()
        whenever(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask)
        whenever(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask)

        // WHEN
        handler.create("Topic A", "Body Text")

        // THEN: Verify the save call happened
        verify(mockDocument).set(any())

        // Trigger success to prove the chain works
        val captor = ArgumentCaptor.forClass(OnSuccessListener::class.java) as ArgumentCaptor<OnSuccessListener<Void>>
        verify(mockVoidTask).addOnSuccessListener(captor.capture())
        captor.value.onSuccess(null)
    }
}