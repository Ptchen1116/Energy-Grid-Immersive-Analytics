package com.ucl.energygrid.ui.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.ucl.energygrid.data.API.RetrofitInstance
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.auth0.android.jwt.JWT
import com.ucl.energygrid.data.model.LoginRequest
import com.ucl.energygrid.data.model.RegisterRequest


class AuthViewModel : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _userToken = MutableStateFlow<String?>(null)
    val userToken: StateFlow<String?> = _userToken.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.userApi
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val token = loginResponse?.access_token

                    if (token != null) {
                        val jwt = JWT(token)
                        val userId = jwt.getClaim("sub").asString() ?: jwt.getClaim("id").asString() ?: ""
                        val userIdInt = userId.toIntOrNull() ?: -1

                        if (userIdInt > 0) {
                            _userToken.value = token
                            _userId.value = userId
                            _isLoggedIn.value = true
                            _uiMessage.emit("Login success! UserId: $userId")
                        } else {
                            _isLoggedIn.value = false
                            _uiMessage.emit("Login failed: Invalid userId")
                        }
                    } else {
                        _uiMessage.emit("Login failed: No token received")
                    }
                } else {
                    _uiMessage.emit("Login failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _uiMessage.emit("Login error: ${e.message}")
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitInstance.userApi
                val response = api.register(RegisterRequest(username, email, password))
                if (response.isSuccessful) {
                    _uiMessage.emit("Register successful!")
                } else {
                    _uiMessage.emit("Register failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _uiMessage.emit("Register error: ${e.message}")
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _userId.value = null
        _userToken.value = null
    }
}