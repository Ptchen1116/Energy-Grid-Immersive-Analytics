package com.ucl.energygrid

import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.firebase.database.*
import org.webrtc.*
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.widget.Toast
import android.content.Context
import android.media.AudioManager


class CallingActivity : ComponentActivity() {
    companion object {
        const val REQUEST_CAMERA_CODE = 1001
    }

    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var startCallBtn: Button
    private lateinit var endCallBtn: Button

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private lateinit var localVideoSource: VideoSource
    private lateinit var localAudioSource: AudioSource
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private var videoCapturer: VideoCapturer? = null

    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance("https://ucl---energy-data-analytics-default-rtdb.europe-west1.firebasedatabase.app")
            .reference.child("calls").child("test-call")
    }

    private val eglBase: EglBase by lazy { EglBase.create() }
    private var isCaller = false
    private val pendingCandidates = mutableListOf<IceCandidate>() // 暫存 ICE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = ConstraintLayout(this).apply {
            id = ConstraintLayout.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        localView = SurfaceViewRenderer(this).apply {
            id = ViewGroup.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(300, 400)
        }

        remoteView = SurfaceViewRenderer(this).apply {
            id = ViewGroup.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        startCallBtn = Button(this).apply {
            id = ViewGroup.generateViewId()
            text = "Start Call"
        }

        endCallBtn = Button(this).apply {
            id = ViewGroup.generateViewId()
            text = "End Call"
        }

        rootLayout.addView(remoteView)
        rootLayout.addView(localView)
        rootLayout.addView(startCallBtn)
        rootLayout.addView(endCallBtn)
        setContentView(rootLayout)

        val set = ConstraintSet()
        set.clone(rootLayout)
        set.connect(remoteView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        set.connect(remoteView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.connect(remoteView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(remoteView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        set.constrainWidth(remoteView.id, ConstraintSet.MATCH_CONSTRAINT)
        set.constrainHeight(remoteView.id, ConstraintSet.MATCH_CONSTRAINT)

        set.connect(localView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 16)
        set.connect(localView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.constrainWidth(localView.id, 300)
        set.constrainHeight(localView.id, 400)

        set.connect(startCallBtn.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 16)
        set.connect(startCallBtn.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(endCallBtn.id, ConstraintSet.TOP, startCallBtn.id, ConstraintSet.BOTTOM, 16)
        set.connect(endCallBtn.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.applyTo(rootLayout)

        setupViews()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_CODE)
        } else {
            initWebRTC()
        }

        startCallBtn.setOnClickListener {
            isCaller = true
            startCall()
        }

        endCallBtn.setOnClickListener { endCall() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initWebRTC()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViews() {
        localView.init(eglBase.eglBaseContext, null)
        localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        localView.setMirror(true)

        remoteView.init(eglBase.eglBaseContext, null)
        remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        remoteView.setMirror(false)
    }

    private fun initWebRTC() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options().apply {
            disableEncryption = false
            disableNetworkMonitor = false
        }

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        videoCapturer = createCameraCapturer()
        if (videoCapturer == null) {
            Log.e("WebRTC", "Failed to create video capturer")
            return
        }

        localVideoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
        videoCapturer!!.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            this,
            localVideoSource.capturerObserver
        )
        videoCapturer!!.startCapture(640, 480, 30)

        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localVideoTrack = peerConnectionFactory.createVideoTrack("videoTrack", localVideoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("audioTrack", localAudioSource)

        localVideoTrack.addSink(localView)

        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val config = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    val path = if (isCaller) "callerCandidates" else "calleeCandidates"
                    database.child(path).push().setValue(IceCandidateData(it.sdpMid, it.sdpMLineIndex, it.sdp))
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.addSink(remoteView)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.i("WebRTC", "Connection state: $newState")
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

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        listenForOffer()
        listenForRemoteSDP()
        listenForRemoteCandidates()
    }

    private fun startCall() {
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
            override fun onCreateFailure(error: String?) { Log.e("WebRTC", "Offer failed: $error") }
            override fun onSetFailure(error: String?) { Log.e("WebRTC", "SetLocalDesc failed: $error") }
        }, MediaConstraints())
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
            override fun onCreateFailure(error: String?) { Log.e("WebRTC", "Answer failed: $error") }
            override fun onSetFailure(error: String?) { Log.e("WebRTC", "SetLocalDesc failed: $error") }
        }, MediaConstraints())
    }

    private fun listenForOffer() {
        database.child("offer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && !isCaller) {
                    val sdp = snapshot.getValue(String::class.java) ?: return
                    val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            createAnswer()
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
        })
    }

    private fun listenForRemoteSDP() {
        database.child("answer").addValueEventListener(object : ValueEventListener {
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
        })
    }

    private fun listenForRemoteCandidates() {
        val candidatesRef = if (isCaller) database.child("calleeCandidates") else database.child("callerCandidates")
        candidatesRef.addChildEventListener(object : ChildEventListener {
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
        })
    }

    private fun endCall() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
        videoCapturer?.dispose()
        localVideoSource.dispose()
        localAudioSource.dispose()

        peerConnection?.close()
        peerConnection = null
        database.removeValue()
        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(this)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return enumerator.deviceNames.firstOrNull()?.let { enumerator.createCapturer(it, null) }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { videoCapturer?.stopCapture() } catch (_: Exception) {}
        videoCapturer?.dispose()
        localVideoSource.dispose()
        localAudioSource.dispose()
        peerConnection?.close()
        peerConnectionFactory.dispose()
        localView.release()
        remoteView.release()
        eglBase.release()
    }

    data class IceCandidateData(
        val sdpMid: String? = null,
        val sdpMLineIndex: Int = 0,
        val sdp: String? = null
    )
}