package com.ucl.energygrid.data.API

// ViewModel
import androidx.lifecycle.ViewModel

// Compose State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class AuthViewModel : ViewModel() {
    var isLoggedIn by mutableStateOf(false)
        private set

    var userToken by mutableStateOf<String?>(null)
        private set

    fun loginSuccess(token: String) {
        isLoggedIn = true
        userToken = token
    }

    fun logout() {
        isLoggedIn = false
        userToken = null
    }
}