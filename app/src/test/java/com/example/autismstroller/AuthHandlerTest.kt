package com.example.autismstroller.functional

import com.example.autismstroller.functional.AuthState
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AuthHandlerTest {

    // 1. Create Mocks
    private val mockAuth: FirebaseAuth = mock()
    private val mockFirestore: FirebaseFirestore = mock()
    private val mockUser: FirebaseUser = mock()

    // Mocks for the complicated "Task" chains
    private val mockAuthTask: Task<AuthResult> = mock()

    // 2. Initialize Handler with mocks
    private val handler = AuthHandler(mockAuth, mockFirestore)

    @Test
    fun `checkAuthStatus sets Authenticated when user exists`() {
        // GIVEN: A user is logged in
        whenever(mockAuth.currentUser).thenReturn(mockUser)

        // WHEN
        handler.checkAuthStatus()

        // THEN
        assertEquals(AuthState.Authenticated, handler.authstate.value)
    }

    @Test
    fun `checkAuthStatus sets Unauthenticated when user is null`() {
        // GIVEN: No user
        whenever(mockAuth.currentUser).thenReturn(null)

        // WHEN
        handler.checkAuthStatus()

        // THEN
        assertEquals(AuthState.Unauthenticated, handler.authstate.value)
    }

    @Test
    fun `login updates state to Authenticated on success`() {
        // GIVEN
        val email = "test@test.com"
        val pass = "123456"

        // Setup the specific mock chain for signIn
        whenever(mockAuth.signInWithEmailAndPassword(email, pass)).thenReturn(mockAuthTask)

        // Setup the Task to be "Successful"
        whenever(mockAuthTask.isSuccessful).thenReturn(true)

        // WHEN: We call login
        handler.login(email, pass)

        // THEN: We must capture the "Listener" that the code attached to the task
        // Because the real Firebase isn't running, we have to manually "click" the listener
        val captor = ArgumentCaptor.forClass(OnCompleteListener::class.java) as ArgumentCaptor<OnCompleteListener<AuthResult>>
        verify(mockAuthTask).addOnCompleteListener(captor.capture())

        // Trigger the callback manually!
        captor.value.onComplete(mockAuthTask)

        // Verify state changed
        assertEquals(AuthState.Authenticated, handler.authstate.value)
    }
}