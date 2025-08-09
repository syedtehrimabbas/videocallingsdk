package com.videocalling.sdk.config

import android.os.Build

/**
 * SDK configuration constants
 */
object SDKConfig {
    // Signaling server URLs for different environments
    const val EMULATOR_SIGNALING_URL = "ws://10.0.2.2:8080" // Android emulator
    const val REAL_DEVICE_SIGNALING_URL = "ws://192.168.2.100:8080" // Real device (your MacBook's IP)
    
    // WebRTC video configuration
    const val DEFAULT_VIDEO_WIDTH = 640
    const val DEFAULT_VIDEO_HEIGHT = 480
    const val DEFAULT_VIDEO_FPS = 30
    
    // WebRTC audio configuration
    const val DEFAULT_AUDIO_SAMPLE_RATE = 48000
    const val DEFAULT_AUDIO_CHANNELS = 1
    
    // Default ICE servers (STUN servers for NAT traversal)
    val DEFAULT_ICE_SERVERS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302"
    )
    
    // Connection timeouts
    const val CONNECTION_TIMEOUT_MS = 30000L
    const val RECONNECTION_DELAY_MS = 5000L
    const val MAX_RECONNECTION_ATTEMPTS = 5
    
    // Video quality settings
    const val VIDEO_BITRATE_KBPS = 1000
    const val VIDEO_FRAME_RATE = 30
    const val VIDEO_WIDTH = 640
    const val VIDEO_HEIGHT = 480
    
    // Audio quality settings
    const val AUDIO_BITRATE_KBPS = 64
    const val AUDIO_SAMPLE_RATE = 48000
    const val AUDIO_CHANNELS = 1
    
    /**
     * Get the appropriate signaling URL based on the device environment
     */
    fun getSignalingUrl(): String {
        return if (isEmulator()) {
            EMULATOR_SIGNALING_URL
        } else {
            REAL_DEVICE_SIGNALING_URL
        }
    }
    
    /**
     * Check if the app is running on an emulator
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }
} 