package com.example.autismstroller.functional

import androidx.lifecycle.ViewModel
import com.example.autismstroller.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthHandler(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel(){
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authstate : StateFlow<AuthState> = _authState

    fun checkAuthStatus(){
        if(auth.currentUser == null){
            _authState.value = AuthState.Unauthenticated
        } else{
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email : String, password : String){
        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email or password cannot be empty")
            return;
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{task->
                if(task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                }else{
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        phoneNumber: String,
        age: Int,
        isMale: Boolean,
        profilePhoto: String
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userId = user.uid

                        val newUser = User(
                            uid = userId,
                            name = name,
                            email = email,
                            phoneNumber = phoneNumber,
                            age = age,
                            genderMale = isMale,
                            profilePicture = profilePhoto
                        )

                        firestore.collection("users").document(userId).set(newUser)
                            .addOnSuccessListener {
                                _authState.value = AuthState.Authenticated
                            }
                            .addOnFailureListener { exception ->
                                _authState.value =
                                    AuthState.Error("Failed to save user data: ${exception.message}")
                            }
                    }
                } else {
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }


    fun signOut(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun clearError() {
        _authState.value = AuthState.Unauthenticated // Reset state to prevent stale error
    }
}

sealed class AuthState{
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message : String) : AuthState()
}