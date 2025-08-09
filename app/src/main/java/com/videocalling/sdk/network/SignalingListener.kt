package com.videocalling.sdk.network

/**
 * Interface for handling signaling server events
 */
interface SignalingListener {
    /**
     * Called when connected to signaling server
     */
    fun onConnected() {}
    
    /**
     * Called when disconnected from signaling server
     */
    fun onDisconnected() {}
    
    /**
     * Called when a remote user joins the room
     * @param userId ID of the user who joined
     */
    fun onRemoteUserJoined(userId: String) {}
    
    /**
     * Called when a remote user leaves the room
     * @param userId ID of the user who left
     */
    fun onRemoteUserLeft(userId: String) {}
    
    /**
     * Called when an offer is received from remote peer
     * @param offer SDP offer
     */
    fun onOfferReceived(offer: String) {}
    
    /**
     * Called when an answer is received from remote peer
     * @param answer SDP answer
     */
    fun onAnswerReceived(answer: String) {}
    
    /**
     * Called when an ICE candidate is received from remote peer
     * @param candidate ICE candidate
     */
    fun onIceCandidateReceived(candidate: String) {}
    
    /**
     * Called when an error occurs
     * @param error Error message
     */
    fun onError(error: String) {}
} 