package com.videocalling.sdk.callback

import com.videocalling.sdk.model.CallState

/**
 * Interface for handling call events
 */
interface CallEventListener {
    /**
     * Called when the call is connected
     */
    fun onCallConnected() {}
    
    /**
     * Called when the call is disconnected
     */
    fun onCallDisconnected() {}
    
    /**
     * Called when a remote user joins the call
     * @param userId ID of the user who joined
     */
    fun onRemoteUserJoined(userId: String) {}
    
    /**
     * Called when a remote user leaves the call
     * @param userId ID of the user who left
     */
    fun onRemoteUserLeft(userId: String) {}
    
    /**
     * Called when the call state changes
     * @param state New call state
     */
    fun onCallStateChanged(state: CallState) {}
    
    /**
     * Called when video state changes
     * @param enabled true if video is enabled
     */
    fun onVideoStateChanged(enabled: Boolean) {}
    
    /**
     * Called when audio state changes
     * @param enabled true if audio is enabled
     */
    fun onAudioStateChanged(enabled: Boolean) {}
    
    /**
     * Called when speaker mode changes
     * @param enabled true if speaker mode is enabled
     */
    fun onSpeakerStateChanged(enabled: Boolean) {}
    
    /**
     * Called when an error occurs during the call
     * @param error Error message
     */
    fun onCallError(error: String) {}
    
    /**
     * Called when the connection quality changes
     * @param quality Connection quality (0-100)
     */
    fun onConnectionQualityChanged(quality: Int) {}
} 