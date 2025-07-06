package com.ucl.energygrid.data.API

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {

    private var _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> get() = _isLoggedIn

    private var _userToken = mutableStateOf<String?>(null)
    val userToken: State<String?> get() = _userToken

    private var _userId = mutableStateOf<String?>(null)
    val userId: State<String?> get() = _userId

    fun loginSuccess(token: String, id: String) {
        _isLoggedIn.value = true
        _userToken.value = token
        _userId.value = id
    }

    fun logout() {
        _isLoggedIn.value = false
        _userToken.value = null
        _userId.value = null
    }
}
