package com.videocalling.sdk

import android.app.Activity
import android.content.Context
import android.util.Log
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.videocalling.sdk.config.SDKConfig
import com.videocalling.sdk.core.WebRTCManager
import com.videocalling.sdk.network.SignalingClient
import com.videocalling.sdk.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main SDK class for video calling functionality
 */
object VideoCallingSDK {
    private const val TAG = "VideoCallingSDK"
    
    private var isInitialized = false
    private var apiKey: String? = null
    private var signalingUrl: String? = null
    private var debugMode = false
    private lateinit var context: Context
    private lateinit var webRTCManager: WebRTCManager
    private lateinit var signalingClient: SignalingClient
    
    /**
     * Initialize the SDK with API key
     * @param context Application context
     * @param apiKey Your API key (optional for demo/testing)
     */
    fun initialize(context: Context, apiKey: String? = null) {
        initialize(context, apiKey, null)
    }
    
    /**
     * Initialize the SDK with API key and custom signaling server
     * @param context Application context
     * @param apiKey Your API key (optional for demo/testing)
     * @param signalingUrl Custom signaling server URL
     */
    fun initialize(context: Context, apiKey: String?, signalingUrl: String?) {
        this.context = context.applicationContext
        this.apiKey = apiKey ?: "demo-api-key"
        this.signalingUrl = signalingUrl ?: SDKConfig.getSignalingUrl()
        
        webRTCManager = WebRTCManager(context)
        signalingClient = SignalingClient(this.signalingUrl!!, this.apiKey!!)
        
        isInitialized = true
        
        if (debugMode) {
            Log.d(TAG, "SDK initialized with API key: ${this.apiKey}")
            Log.d(TAG, "Using signaling URL: ${this.signalingUrl}")
        }
    }
    
    /**
     * Create a new video call instance
     * @return VideoCall instance
     */
    fun createCall(): VideoCall {
        if (!isInitialized) {
            throw IllegalStateException("SDK not initialized. Call VideoCallingSDK.initialize() first.")
        }
        
        return VideoCall(webRTCManager, signalingClient)
    }
    
    /**
     * Request camera and microphone permissions
     * @param activity Current activity
     */
    fun requestPermissions(activity: Activity) {
        Dexter.withContext(activity)
            .withPermissions(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        Log.d(TAG, "All permissions granted")
                    } else {
                        Log.w(TAG, "Some permissions denied")
                    }
                }
                
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }
    
    /**
     * Enable debug mode for detailed logging
     * @param enabled Whether to enable debug mode
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        Log.d(TAG, "Debug mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if SDK is initialized
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean = isInitialized
} 