package com.example.shop.viewModels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State
import com.example.shop.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AuthenticationViewModel : ViewModel() {
    private val _email = mutableStateOf("")
    val email: State<String> get() = _email

    private val _password = mutableStateOf("")
    val password: State<String> get() = _password

    private val _message = mutableStateOf("")
    val message: State<String> get() = _message

    private val _isLogin = mutableStateOf(true)
    val isLogin: State<Boolean> get() = _isLogin

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onToggleLogin() {
        _isLogin.value = !_isLogin.value
    }

    fun onAuthenticate(auth: FirebaseAuth, onSuccess: (FirebaseAuth) -> Unit) {
        if (_isLogin.value) {
            login(auth, _email.value, _password.value) { success, error ->
                if (success) {
                    onSuccess(auth)
                } else {
                    _message.value = error ?: "Ошибка входа"
                }
            }
        } else {
            register(auth, _email.value, _password.value) { success, error ->
                if (success) {
                    _message.value = "Регистрация успешна!"
                    _isLogin.value = true
                } else {
                    _message.value = error ?: "Ошибка регистрации"
                }
            }
        }
    }

    private fun login(
        auth: FirebaseAuth,
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    private fun register(
        auth: FirebaseAuth,
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val firestore = Firebase.firestore

                    val user = UserProfile(
                        name = "",
                        email = email,
                        registrationDate = System.currentTimeMillis().toString(),
                        surname = "",
                        country = "",
                        city = "",
                        birthDate = "",
                        sex = "",
                        phone = "",
                        status = ""
                    )

                    firestore.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            callback(true, null)
                        }
                        .addOnFailureListener { exception ->
                            callback(false, exception.message)
                        }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }
}
