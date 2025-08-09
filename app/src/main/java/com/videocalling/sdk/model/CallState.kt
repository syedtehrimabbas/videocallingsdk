package com.videocalling.sdk.model

/**
 * Represents the state of a video call
 */
enum class CallState {
    /**
     * Call is idle (not started)
     */
    IDLE,
    
    /**
     * Call is connecting to signaling server
     */
    CONNECTING,
    
    /**
     * Call is connected and active
     */
    CONNECTED,
    
    /**
     * Call is disconnecting
     */
    DISCONNECTING,
    
    /**
     * Call has ended
     */
    ENDED,
    
    /**
     * Call has encountered an error
     */
    ERROR,
    
    /**
     * Call is reconnecting after connection loss
     */
    RECONNECTING
} 