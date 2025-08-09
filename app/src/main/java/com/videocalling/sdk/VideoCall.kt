package com.videocalling.sdk

import android.util.Log
import com.videocalling.sdk.callback.CallEventListener
import com.videocalling.sdk.core.WebRTCManager
import com.videocalling.sdk.model.CallState
import com.videocalling.sdk.network.SignalingClient
import com.videocalling.sdk.network.SignalingListener
import com.videocalling.sdk.ui.VideoViewWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Represents a video call session
 */
class VideoCall(
    private val webRTCManager: WebRTCManager,
    private val signalingClient: SignalingClient
) {
    private val TAG = "VideoCall"

    private var callState = CallState.IDLE
    private var roomId: String? = null
    private var userId: String? = null
    private var isVideoEnabled = true
    private var isAudioEnabled = true
    private var isSpeakerMode = false
    private var callEventListener: CallEventListener? = null
    private var localVideoView: VideoViewWrapper? = null
    private var remoteVideoView: VideoViewWrapper? = null

    private val scope = CoroutineScope(Dispatchers.Main)

    init { setupSignalingListener() }

    fun start(roomId: String, userId: String) {
        this.roomId = roomId
        this.userId = userId

        scope.launch {
            try {
                updateCallState(CallState.CONNECTING)

                // Initialize WebRTC first
                webRTCManager.initialize()
                
                // Set signaling client in WebRTC manager
                webRTCManager.setSignalingClient(signalingClient)

                // Connect to signaling server
                signalingClient.connect(roomId, userId)

                localVideoView?.let { webRTCManager.setLocalVideoView(it.getSurfaceViewRenderer()) }
                remoteVideoView?.let { webRTCManager.setRemoteVideoView(it.getSurfaceViewRenderer()) }

                Log.d(TAG, "Call started for room: $roomId, user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start call", e)
                updateCallState(CallState.ERROR)
                callEventListener?.onCallError(e.message ?: "Failed to start call")
            }
        }
    }

    fun end() {
        scope.launch {
            try {
                updateCallState(CallState.DISCONNECTING)
                signalingClient.disconnect()
                webRTCManager.cleanup()
                updateCallState(CallState.IDLE)
                callEventListener?.onCallDisconnected()
                Log.d(TAG, "Call ended")
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call", e)
            }
        }
    }

    fun setVideoEnabled(enabled: Boolean) {
        isVideoEnabled = enabled
        webRTCManager.setVideoEnabled(enabled)
        callEventListener?.onVideoStateChanged(enabled)
        Log.d(TAG, "Video ${if (enabled) "enabled" else "disabled"}")
    }

    fun setAudioEnabled(enabled: Boolean) {
        isAudioEnabled = enabled
        webRTCManager.setAudioEnabled(enabled)
        callEventListener?.onAudioStateChanged(enabled)
        Log.d(TAG, "Audio ${if (enabled) "enabled" else "disabled"}")
    }

    fun setSpeakerMode(enabled: Boolean) {
        isSpeakerMode = enabled
        webRTCManager.setSpeakerMode(enabled)
        callEventListener?.onSpeakerStateChanged(enabled)
        Log.d(TAG, "Speaker mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun setLocalVideoView(view: VideoViewWrapper) {
        localVideoView = view
    }

    fun setRemoteVideoView(view: VideoViewWrapper) {
        remoteVideoView = view
    }

    /**
     * Initialize video views using stored references
     */
    fun initializeVideoViews() {
        localVideoView?.let { local ->
            remoteVideoView?.let { remote ->
                webRTCManager.initializeVideoViews(local.getSurfaceViewRenderer(), remote.getSurfaceViewRenderer())
            }
        }
    }

    /**
     * Release video views using stored references
     */
    fun releaseVideoViews() {
        localVideoView?.let { local ->
            remoteVideoView?.let { remote ->
                webRTCManager.releaseVideoViews(local.getSurfaceViewRenderer(), remote.getSurfaceViewRenderer())
            }
        }
    }

    fun setOnCallEventListener(listener: CallEventListener) { callEventListener = listener }

    fun getCallState(): CallState = callState
    fun isVideoEnabled(): Boolean = isVideoEnabled
    fun isAudioEnabled(): Boolean = isAudioEnabled
    fun isSpeakerMode(): Boolean = isSpeakerMode
    fun getRoomId(): String? = roomId
    fun getUserId(): String? = userId

    private fun setupSignalingListener() {
        signalingClient.setSignalingListener(object : SignalingListener {
            override fun onConnected() { Log.d(TAG, "Connected to signaling server") }
            override fun onDisconnected() { Log.d(TAG, "Disconnected from signaling server") }
            override fun onRemoteUserJoined(userId: String) {
                Log.d(TAG, "Remote user joined: $userId, creating offer")
                callEventListener?.onRemoteUserJoined(userId)
                
                // Create offer when remote user joins
                scope.launch {
                    try {
                        // Add a small delay to ensure WebRTC is fully initialized
                        kotlinx.coroutines.delay(100)
                        webRTCManager.createOffer()
                        Log.d(TAG, "createOffer() called successfully from SignalingClient listener")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating offer", e)
                    }
                }
            }
            override fun onRemoteUserLeft(userId: String) {
                Log.d(TAG, "Remote user left: $userId"); callEventListener?.onRemoteUserLeft(userId)
            }
            override fun onOfferReceived(offer: String) { webRTCManager.handleOffer(offer) }
            override fun onAnswerReceived(answer: String) { webRTCManager.handleAnswer(answer) }
            override fun onIceCandidateReceived(candidate: String) { webRTCManager.handleIceCandidate(candidate) }
            override fun onError(error: String) { Log.e(TAG, "Signaling error: $error"); callEventListener?.onCallError(error) }
        })
    }

    private fun updateCallState(newState: CallState) {
        callState = newState
        callEventListener?.onCallStateChanged(newState)
    }
} 