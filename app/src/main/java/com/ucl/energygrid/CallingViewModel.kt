package com.ucl.energygrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucl.energygrid.data.repository.WebRtcRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import kotlinx.coroutines.flow.MutableStateFlow


class CallingViewModel(
    private val repo: WebRtcRepository
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = repo.isConnected
    private val _callActive = MutableStateFlow(false)
    val callActive: StateFlow<Boolean> get() = _callActive

    fun init(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, isCaller: Boolean) {
        repo.init(localView, remoteView, isCaller)
    }

    fun startCall() = viewModelScope.launch { repo.startCall() }
    fun endCall() = viewModelScope.launch { repo.endCall() }

    override fun onCleared() {
        super.onCleared()
        repo.endCall()
    }

    val incomingCall: StateFlow<Boolean> = repo.incomingCall

    fun acceptCall() = viewModelScope.launch {
        repo.acceptIncomingCall()
        _callActive.value = true
    }

    fun rejectCall() = viewModelScope.launch {
        repo.rejectIncomingCall()
        _callActive.value = false
    }
}