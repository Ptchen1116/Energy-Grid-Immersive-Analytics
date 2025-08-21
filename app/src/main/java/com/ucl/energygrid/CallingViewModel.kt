package com.ucl.energygrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucl.energygrid.data.repository.WebRtcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer


class CallingViewModel(
    private val repo: WebRtcRepository
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = repo.isConnected

    private val _callActive = MutableStateFlow(false)
    val callActive: StateFlow<Boolean> get() = _callActive

    init {
        viewModelScope.launch {
            repo.isConnected.collect { connected ->
                _callActive.value = connected
            }
        }
    }

    fun init(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, isCaller: Boolean) {
        repo.init(localView, remoteView, isCaller)
    }

    fun startCall() = viewModelScope.launch { repo.startCall() }

    fun endCall() = viewModelScope.launch {
        repo.endCall()
    }

    val incomingCall: StateFlow<Boolean> = repo.incomingCall

    fun acceptCall() = viewModelScope.launch {
        repo.acceptIncomingCall()
    }

    fun rejectCall() = viewModelScope.launch {
        repo.rejectIncomingCall()
    }

    override fun onCleared() {
        super.onCleared()
        repo.endCall()
    }
}