package com.ucl.energygrid

import android.content.Context
import org.webrtc.SurfaceViewRenderer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class CallingViewModel(
    private val context: Context,
    private val isCaller: Boolean,
) : ViewModel() {

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    private var callingClient: CallingClient? = null

    lateinit var localView: SurfaceViewRenderer
    lateinit var remoteView: SurfaceViewRenderer

    fun init(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer) {
        this.localView = localView
        this.remoteView = remoteView
        callingClient = CallingClient(context, localView, remoteView, isCaller)
        callingClient?.init()
    }

    fun startCall() {
        if (_isStarted.value) return
        callingClient?.let {
            if (isCaller) {
                it.startCall()
            }
            _isStarted.value = true
        }
    }

    fun endCall() {
        callingClient?.endCall()
        _isStarted.value = false
    }

    override fun onCleared() {
        super.onCleared()
        endCall()
    }
}