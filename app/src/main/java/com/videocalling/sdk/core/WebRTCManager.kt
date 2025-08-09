package com.videocalling.sdk.core

import android.content.Context
import android.util.Log
import com.videocalling.sdk.config.SDKConfig
import com.videocalling.sdk.network.SignalingListener
import com.google.gson.Gson
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SdpObserver
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages WebRTC functionality for video calls
 */
class WebRTCManager(private val context: Context) {
    private val TAG = "WebRTCManager"

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteAudioTrack: AudioTrack? = null
    private var localVideoCapturer: VideoCapturer? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var isVideoEnabled = true
    private var isAudioEnabled = true
    private var isSpeakerMode = false
    private var signalingListener: SignalingListener? = null
    private var signalingClient: com.videocalling.sdk.network.SignalingClient? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private val iceServers = mutableListOf<String>()
    private val eglBase: EglBase = EglBase.create()
    private val gson = Gson()

    init {
        iceServers.addAll(SDKConfig.DEFAULT_ICE_SERVERS)
    }

    /**
     * Initialize WebRTC components
     */
    fun initialize() {
        scope.launch {
            try {
                // Initialize WebRTC
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions()
                )

                // Create peer connection factory
                val options = PeerConnectionFactory.Options()
                peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .createPeerConnectionFactory()

                // Create video capturer and video track
                val videoSource = createAndStartVideoSource()
                localVideoTrack = peerConnectionFactory?.createVideoTrack("VIDEO_TRACK", videoSource)

                // Create audio source
                val audioSource = createAudioSource()
                localAudioTrack = peerConnectionFactory?.createAudioTrack("AUDIO_TRACK", audioSource)

                // Create peer connection
                createPeerConnection()

                Log.d(TAG, "WebRTC initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WebRTC", e)
                throw e
            }
        }
    }

    /**
     * Set ICE servers for NAT traversal
     * @param servers List of STUN/TURN server URLs
     */
    fun setIceServers(servers: List<String>) {
        iceServers.clear()
        iceServers.addAll(servers)
        // Recreate peer connection with new ICE servers
        createPeerConnection()
    }
    
    /**
     * Set signaling listener for handling remote user events
     * @param listener Signaling listener
     */
    fun setSignalingListener(listener: SignalingListener) {
        Log.d(TAG, "Setting signaling listener")
        signalingListener = listener
    }
    
    /**
     * Set signaling client for sending offers/answers
     * @param client Signaling client
     */
    fun setSignalingClient(client: com.videocalling.sdk.network.SignalingClient) {
        signalingClient = client
    }

    /**
     * Initialize video views with EGL context
     * @param localView Local video view
     * @param remoteView Remote video view
     */
    fun initializeVideoViews(localView: org.webrtc.SurfaceViewRenderer, remoteView: org.webrtc.SurfaceViewRenderer) {
        try {
            localView.init(eglBase.eglBaseContext, null)
            remoteView.init(eglBase.eglBaseContext, null)
            Log.d(TAG, "Video views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing video views", e)
            throw e
        }
    }

    /**
     * Release video views
     * @param localView Local video view
     * @param remoteView Remote video view
     */
    fun releaseVideoViews(localView: org.webrtc.SurfaceViewRenderer, remoteView: org.webrtc.SurfaceViewRenderer) {
        try {
            localView.release()
            remoteView.release()
            Log.d(TAG, "Video views released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing video views", e)
        }
    }

    /**
     * Set local video renderer
     */
    fun setLocalVideoView(renderer: org.webrtc.SurfaceViewRenderer) {
        localRenderer = renderer
        // Don't initialize here - let the calling app handle initialization
        localVideoTrack?.addSink(renderer)
        // Start video capture immediately to show preview
        startVideoCapture()
    }

    /**
     * Set remote video renderer
     */
    fun setRemoteVideoView(renderer: org.webrtc.SurfaceViewRenderer) {
        remoteRenderer = renderer
        // Don't initialize here - let the calling app handle initialization
        remoteVideoTrack?.addSink(renderer)
    }

    /**
     * Enable or disable video
     */
    fun setVideoEnabled(enabled: Boolean) {
        isVideoEnabled = enabled
        localVideoTrack?.setEnabled(enabled)
        localVideoCapturer?.let { capturer ->
            try {
                if (enabled) {
                    capturer.startCapture(
                        SDKConfig.DEFAULT_VIDEO_WIDTH,
                        SDKConfig.DEFAULT_VIDEO_HEIGHT,
                        SDKConfig.DEFAULT_VIDEO_FPS
                    )
                } else {
                    capturer.stopCapture()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error toggling capture", e)
            }
        }
    }

    /**
     * Enable or disable audio
     */
    fun setAudioEnabled(enabled: Boolean) {
        isAudioEnabled = enabled
        localAudioTrack?.setEnabled(enabled)
    }

    /**
     * Enable or disable speaker mode (routing will be implemented by host app)
     */
    fun setSpeakerMode(enabled: Boolean) {
        isSpeakerMode = enabled
        Log.d(TAG, "Speaker mode ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Handle incoming offer
     */
    fun handleOffer(offer: String) {
        scope.launch {
            try {
                val sdp = SessionDescription(SessionDescription.Type.OFFER, offer)
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(s: SessionDescription?) {}
                    override fun onSetSuccess() { 
                        Log.d(TAG, "Remote offer set, creating answer")
                        createAnswer() 
                    }
                    override fun onCreateFailure(p0: String?) { Log.e(TAG, "onCreateFailure $p0") }
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "onSetFailure $p0") }
                }, sdp)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling offer", e)
            }
        }
    }

    /**
     * Handle incoming answer
     */
    fun handleAnswer(answer: String) {
        scope.launch {
            try {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, answer)
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(s: SessionDescription?) {}
                    override fun onSetSuccess() { Log.d(TAG, "Remote description set") }
                    override fun onCreateFailure(p0: String?) { Log.e(TAG, "onCreateFailure $p0") }
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "onSetFailure $p0") }
                }, sdp)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling answer", e)
            }
        }
    }
    


    /**
     * Handle incoming ICE candidate (expects JSON: {"sdpMid":"0","sdpMLineIndex":0,"candidate":"..."})
     */
    fun handleIceCandidate(candidateJson: String) {
        scope.launch {
            try {
                Log.d(TAG, "Handling ICE candidate: $candidateJson")
                
                // Try to parse as the expected format first
                try {
                    data class IceMsg(val sdpMid: String?, val sdpMLineIndex: Int, val candidate: String)
                    val msg = gson.fromJson(candidateJson, IceMsg::class.java)
                    val ice = IceCandidate(msg.sdpMid, msg.sdpMLineIndex, msg.candidate)
                    peerConnection?.addIceCandidate(ice)
                    Log.d(TAG, "ICE candidate added successfully")
                } catch (e: Exception) {
                    // If that fails, try to parse the WebRTC format
                    Log.d(TAG, "Failed to parse as standard format, trying WebRTC format")
                    try {
                        data class WebRTCCandidate(val sdpMid: String?, val sdpMLineIndex: Int, val sdp: String)
                        val msg = gson.fromJson(candidateJson, WebRTCCandidate::class.java)
                        Log.d(TAG, "Parsed WebRTC candidate - sdpMid: ${msg.sdpMid}, sdpMLineIndex: ${msg.sdpMLineIndex}, sdp: ${msg.sdp}")
                        
                        if (msg.sdp.isNotEmpty()) {
                            // Create IceCandidate with proper null handling
                            val sdpMid = msg.sdpMid ?: ""
                            val sdpMLineIndex = msg.sdpMLineIndex
                            val sdp = msg.sdp
                            
                            Log.d(TAG, "Creating IceCandidate with sdpMid: '$sdpMid', sdpMLineIndex: $sdpMLineIndex, sdp: '$sdp'")
                            
                            // Try to create IceCandidate with explicit null checks
                            if (sdpMid.isNotEmpty() && sdp.isNotEmpty()) {
                                try {
                                    // Use a simpler approach - create IceCandidate with minimal parameters
                                    val ice = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                                    peerConnection?.addIceCandidate(ice)
                                    Log.d(TAG, "ICE candidate added successfully (WebRTC format)")
                                } catch (iceException: Exception) {
                                    Log.e(TAG, "Error creating IceCandidate", iceException)
                                    // Try alternative approach
                                    try {
                                        val ice = IceCandidate("", sdpMLineIndex, sdp)
                                        peerConnection?.addIceCandidate(ice)
                                        Log.d(TAG, "ICE candidate added successfully (alternative format)")
                                    } catch (altException: Exception) {
                                        Log.e(TAG, "Alternative ICE candidate creation also failed", altException)
                                        // Try one more approach - skip this candidate entirely
                                        Log.w(TAG, "Skipping problematic ICE candidate")
                                        // Don't crash the app, just log and continue
                                        return@launch
                                    }
                                }
                            } else {
                                Log.e(TAG, "Invalid ICE candidate parameters - sdpMid: '$sdpMid', sdp: '$sdp'")
                            }
                        } else {
                            Log.e(TAG, "ICE candidate sdp is empty")
                        }
                    } catch (parseException: Exception) {
                        Log.e(TAG, "Error parsing WebRTC candidate format", parseException)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling ICE candidate", e)
            }
        }
    }

    /** Create and send an offer (signaling send should be wired by host) */
    fun createOffer() {
        Log.d(TAG, "createOffer() called")
        scope.launch {
            try {
                Log.d(TAG, "Creating offer...")
                peerConnection?.createOffer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        Log.d(TAG, "Offer created successfully")
                        sdp?.let { local ->
                            Log.d(TAG, "Setting local description...")
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() { 
                                    Log.d(TAG, "Offer set locally, sending through signaling")
                                    // Send offer through signaling client
                                    signalingClient?.sendOffer(local.description)
                                    Log.d(TAG, "Offer sent through signaling client")
                                }
                                override fun onCreateFailure(p0: String?) { Log.e(TAG, "create local fail $p0") }
                                override fun onSetFailure(p0: String?) { Log.e(TAG, "set local fail $p0") }
                            }, local)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) { Log.e(TAG, "createOffer fail $p0") }
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "setOffer fail $p0") }
                }, MediaConstraints())
            } catch (e: Exception) {
                Log.e(TAG, "Error creating offer", e)
            }
        }
    }
    
    /** Create and send an answer */
    fun createAnswer() {
        scope.launch {
            try {
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        sdp?.let { local ->
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() { 
                                    Log.d(TAG, "Answer set locally")
                                    // Send answer through signaling client
                                    signalingClient?.sendAnswer(local.description)
                                }
                                override fun onCreateFailure(p0: String?) { Log.e(TAG, "create local fail $p0") }
                                override fun onSetFailure(p0: String?) { Log.e(TAG, "set local fail $p0") }
                            }, local)
                        }
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) { Log.e(TAG, "createAnswer fail $p0") }
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "setAnswer fail $p0") }
                }, MediaConstraints())
            } catch (e: Exception) {
                Log.e(TAG, "Error creating answer", e)
            }
        }
    }

    /**
     * Clean up WebRTC resources
     */
    fun cleanup() {
        scope.launch {
            try {
                try { localVideoCapturer?.stopCapture() } catch (_: Exception) {}
                localVideoCapturer?.dispose()
                localVideoTrack?.dispose()
                localAudioTrack?.dispose()
                remoteVideoTrack?.dispose()
                remoteAudioTrack?.dispose()
                peerConnection?.dispose()
                peerConnectionFactory?.dispose()
                localRenderer?.release()
                remoteRenderer?.release()
                eglBase.release()
                Log.d(TAG, "WebRTC resources cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up WebRTC", e)
            }
        }
    }

    private fun createAndStartVideoSource(): VideoSource {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        // Prefer front camera
        var capturer: VideoCapturer? = null
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                capturer = enumerator.createCapturer(name, null)
                break
            }
        }
        if (capturer == null && deviceNames.isNotEmpty()) {
            capturer = enumerator.createCapturer(deviceNames[0], null)
        }
        requireNotNull(capturer) { "No camera capturer available" }
        localVideoCapturer = capturer

        val videoSource = peerConnectionFactory!!.createVideoSource(false)
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        try {
            capturer.startCapture(
                SDKConfig.DEFAULT_VIDEO_WIDTH,
                SDKConfig.DEFAULT_VIDEO_HEIGHT,
                SDKConfig.DEFAULT_VIDEO_FPS
            )
        } catch (e: Exception) {
            Log.w(TAG, "startCapture failed", e)
        }
        return videoSource
    }

    private fun createAudioSource(): AudioSource {
        val audioConstraints = MediaConstraints()
        audioConstraints.optional.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
        audioConstraints.optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        audioConstraints.optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        audioConstraints.optional.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        return peerConnectionFactory!!.createAudioSource(audioConstraints)
    }

    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(
            iceServers.map { url -> PeerConnection.IceServer.builder(url).createIceServer() }
        )
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                    Log.d(TAG, "Signaling state: $newState")
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "ICE connection: $newState")
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "ICE receiving: $receiving")
                }

                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                    Log.d(TAG, "ICE gathering: $newState")
                }

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { ice ->
                        Log.d(TAG, "Local ICE candidate: ${ice.sdp}")
                        // Send ICE candidate through signaling client
                        val candidateJson = gson.toJson(ice)
                        signalingClient?.sendIceCandidate(candidateJson)
                    }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                }

                override fun onAddStream(stream: MediaStream?) {
                    stream?.videoTracks?.firstOrNull()?.let { track ->
                        remoteVideoTrack = track
                        remoteRenderer?.let { track.addSink(it) }
                    }
                    stream?.audioTracks?.firstOrNull()?.let { track ->
                        remoteAudioTrack = track
                        track.setEnabled(true)
                    }
                }

                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
                override fun onTrack(transceiver: RtpTransceiver?) {}
                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
                override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            }
        )

        // Attach local tracks
        localVideoTrack?.let { track -> peerConnection?.addTrack(track, listOf("ARDAMS")) }
        localAudioTrack?.let { track -> peerConnection?.addTrack(track, listOf("ARDAMS")) }

        // Render local preview
        localRenderer?.let { localVideoTrack?.addSink(it) }
    }



    private fun startVideoCapture() {
        localVideoCapturer?.let { capturer ->
            try {
                capturer.startCapture(
                    SDKConfig.DEFAULT_VIDEO_WIDTH,
                    SDKConfig.DEFAULT_VIDEO_HEIGHT,
                    SDKConfig.DEFAULT_VIDEO_FPS
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start video capture for preview", e)
            }
        }
    }
} 