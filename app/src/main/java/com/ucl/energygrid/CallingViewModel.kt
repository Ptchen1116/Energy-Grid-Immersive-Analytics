package com.ucl.energygrid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucl.energygrid.data.repository.WebRtcRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer


class CallingViewModel(
    private val repo: WebRtcRepository
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = repo.isConnected

    fun init(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, isCaller: Boolean) {
        repo.init(localView, remoteView, isCaller)
    }

    fun startCall() = viewModelScope.launch { repo.startCall() }
    fun endCall() = viewModelScope.launch { repo.endCall() }

    override fun onCleared() {
        super.onCleared()
        repo.endCall()
    }
}