package com.ucl.energygrid.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucl.energygrid.data.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userRepository: UserRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _userToken = MutableStateFlow<String?>(null)
    val userToken: StateFlow<String?> = _userToken.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage

    fun login(email: String, password: String) {
        viewModelScope.launch(dispatcher) {
            val result = userRepository.login(email, password)
            result.fold(
                onSuccess = { (userId, token) ->
                    _userId.value = userId
                    _userToken.value = token
                    _isLoggedIn.value = true
                    _uiMessage.emit("Login success! UserId: $userId")
                },
                onFailure = { error ->
                    _isLoggedIn.value = false
                    _uiMessage.emit("Login failed: ${error.message}")
                }
            )
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch(dispatcher) {
            val result = userRepository.register(username, email, password)
            result.fold(
                onSuccess = {
                    _uiMessage.emit("Register successful!")
                },
                onFailure = { error ->
                    _uiMessage.emit("Register failed: ${error.message}")
                }
            )
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _userId.value = null
        _userToken.value = null
    }
}