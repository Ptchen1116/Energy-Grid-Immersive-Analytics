package com.ucl.energygrid.ui.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class AuthViewModel : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userToken = MutableStateFlow<String?>(null)
    val userToken: StateFlow<String?> = _userToken

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId

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