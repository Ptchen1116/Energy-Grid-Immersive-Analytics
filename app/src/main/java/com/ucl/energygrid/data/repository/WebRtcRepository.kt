package com.ucl.energygrid.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRtcRepository(private val context: Context) {
    private val eglBase = EglBase.create()
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    private lateinit var localVideoSource: VideoSource
    private lateinit var localAudioSource: AudioSource
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private var videoCapturer: VideoCapturer? = null

    private var isCaller: Boolean = false
    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer

    private val database: DatabaseReference = FirebaseDatabase.getInstance(
        "https://ucl---energy-data-analytics-default-rtdb.europe-west1.firebasedatabase.app"
    ).reference.child("calls").child("test-call")

    private val pendingCandidates = mutableListOf<IceCandidate>()
    private val _incomingCall = MutableStateFlow(false)
    val incomingCall: StateFlow<Boolean> get() = _incomingCall

    val isConnected = MutableStateFlow(false)
    private var isCallEndedByMe = false

    private var offerListener: ValueEventListener? = null
    private var answerListener: ValueEventListener? = null
    private var candidateListener: ChildEventListener? = null

    fun init(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, isCaller: Boolean) {
        if (this::peerConnectionFactory.isInitialized && peerConnection != null) {
            return
        }

        this.localView = localView
        this.remoteView = remoteView
        this.isCaller = isCaller

        setupPeerConnection()

        listenForCallEnd()
    }

    private fun setupPeerConnection() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        videoCapturer = createCameraCapturer()
        if (videoCapturer == null) {
            Log.e("WebRtcRepository", "Failed to create video capturer")
            return
        }

        localVideoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            context,
            localVideoSource.capturerObserver
        )
        videoCapturer!!.startCapture(640, 480, 30)

        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localVideoTrack = peerConnectionFactory.createVideoTrack("videoTrack", localVideoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audioTrack", localAudioSource)

        localVideoTrack.addSink(localView)

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    val path = if (isCaller) "callerCandidates" else "calleeCandidates"
                    database.child(path).push().setValue(
                        IceCandidateData(it.sdpMid, it.sdpMLineIndex, it.sdp)
                    )
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.addSink(remoteView)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    isConnected.value = true
                    Log.d("WebRtcRepository", "Call connected")
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddStream(stream: MediaStream?) {}
        })

        peerConnection?.addTrack(localVideoTrack)
        peerConnection?.addTrack(localAudioTrack)

        localView.init(eglBase.eglBaseContext, null)
        localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        localView.setMirror(true)

        remoteView.init(eglBase.eglBaseContext, null)
        remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        remoteView.setMirror(false)

        listenForOffer()
        listenForRemoteSDP()
        listenForRemoteCandidates()
    }

    fun startCall() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        database.child("offer").setValue(sdp?.description)
                    }
                    override fun onSetFailure(error: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    fun endCall(byMe: Boolean = true) {
        if (isCallEndedByMe) return
        isCallEndedByMe = true

        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
        videoCapturer?.dispose()
        if (this::localVideoSource.isInitialized) localVideoSource.dispose()
        if (this::localAudioSource.isInitialized) localAudioSource.dispose()

        peerConnection?.close()
        peerConnection = null

        offerListener?.let { database.child("offer").removeEventListener(it) }
        answerListener?.let { database.child("answer").removeEventListener(it) }
        candidateListener?.let {
            val path = if (isCaller) "calleeCandidates" else "callerCandidates"
            database.child(path).removeEventListener(it)
        }

        callEndedListener?.let { database.child("callEnded").removeEventListener(it) }

        clearSignaling()

        pendingCandidates.clear()
        isConnected.value = false

        // 只有自己主動結束才通知對方
        if (byMe) {
            database.child("callEnded").setValue(true)
        }
    }

    private fun clearSignaling() {
        database.child("offer").removeValue()
        database.child("answer").removeValue()
        database.child("callerCandidates").removeValue()
        database.child("calleeCandidates").removeValue()
        database.child("callEnded").removeValue()
    }

    private var pendingOffer: SessionDescription? = null

    private fun listenForOffer() {
        offerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && !isCaller) {
                    _incomingCall.value = true

                    val sdp = snapshot.getValue(String::class.java) ?: return
                    pendingOffer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                } else {
                    _incomingCall.value = false
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("offer").addValueEventListener(offerListener as ValueEventListener)
    }

    private fun createAnswer() {
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        database.child("answer").setValue(sdp?.description)
                    }
                    override fun onSetFailure(error: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }

    private fun listenForRemoteSDP() {
        answerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && isCaller) {
                    val sdp = snapshot.getValue(String::class.java) ?: return
                    val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            pendingCandidates.forEach { peerConnection?.addIceCandidate(it) }
                            pendingCandidates.clear()
                        }
                        override fun onSetFailure(error: String?) {}
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(error: String?) {}
                    }, desc)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("answer").addValueEventListener(answerListener as ValueEventListener)
    }

    private fun listenForRemoteCandidates() {
        val path = if (isCaller) "calleeCandidates" else "callerCandidates"
        candidateListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val data = snapshot.getValue(IceCandidateData::class.java) ?: return
                val candidate = IceCandidate(data.sdpMid, data.sdpMLineIndex, data.sdp)
                if (peerConnection?.remoteDescription != null) {
                    peerConnection?.addIceCandidate(candidate)
                } else {
                    pendingCandidates.add(candidate)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child(path).addChildEventListener(candidateListener as ChildEventListener)
    }

    fun acceptIncomingCall() {
        pendingOffer?.let { offer ->
            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    createAnswer()
                    pendingCandidates.forEach { peerConnection?.addIceCandidate(it) }
                    pendingCandidates.clear()
                }
                override fun onSetFailure(error: String?) {}
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, offer)
            pendingOffer = null
        }
        _incomingCall.value = false
    }

    fun rejectIncomingCall() {
        _incomingCall.value = false
        database.child("offer").removeValue()
        endCall()
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                val capturer = enumerator.createCapturer(name, null)
                if (capturer != null) return capturer
            }
        }
        for (name in deviceNames) {
            if (!enumerator.isFrontFacing(name)) {
                val capturer = enumerator.createCapturer(name, null)
                if (capturer != null) return capturer
            }
        }
        return null
    }

    private var callEndedListener: ValueEventListener? = null

    private fun listenForCallEnd() {
        callEndedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.getValue(Boolean::class.java) == true) {
                    endCall(byMe = false)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("callEnded").addValueEventListener(callEndedListener as ValueEventListener)
    }
}

data class IceCandidateData(
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0,
    val sdp: String? = null
)