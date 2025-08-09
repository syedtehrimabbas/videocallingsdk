package com.videocalling.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.videocalling.demo.databinding.ActivityMainBinding
import com.videocalling.sdk.VideoCallingSDK
import com.videocalling.sdk.callback.CallEventListener
import com.videocalling.sdk.model.CallState

class MainActivity : AppCompatActivity(), CallEventListener {
    private lateinit var binding: ActivityMainBinding
    private var currentCall: com.videocalling.sdk.VideoCall? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        requestPermissions()
    }

    private fun setupUI() {
        binding.btnStartCall.setOnClickListener {
            if (currentCall == null) {
                startCall()
            } else {
                endCall()
            }
        }

        binding.btnEndCall.setOnClickListener {
            endCall()
        }

        binding.btnToggleVideo.setOnClickListener {
            currentCall?.let { call ->
                val newState = !call.isVideoEnabled()
                call.setVideoEnabled(newState)
                updateVideoButton(newState)
            }
        }

        binding.btnToggleAudio.setOnClickListener {
            currentCall?.let { call ->
                val newState = !call.isAudioEnabled()
                call.setAudioEnabled(newState)
                updateAudioButton(newState)
            }
        }

        binding.btnToggleSpeaker.setOnClickListener {
            currentCall?.let { call ->
                val newState = !call.isSpeakerMode()
                call.setSpeakerMode(newState)
                updateSpeakerButton(newState)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (checkPermissions(permissions)) {
            initializeSDK()
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeSDK()
            } else {
                Toast.makeText(this, "Permissions required for video calling", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeSDK() {
        try {
            VideoCallingSDK.initialize(this) // No API key required for demo
            VideoCallingSDK.setDebugMode(true) // Enable debug logging
            Log.d(TAG, "SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SDK", e)
            Toast.makeText(this, "Failed to initialize SDK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCall() {
        try {
            val roomId = binding.roomIdInput.text.toString().trim()
            val userId = binding.userIdInput.text.toString().trim()
            
            if (roomId.isEmpty()) {
                Toast.makeText(this, "Please enter a room ID", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (userId.isEmpty()) {
                Toast.makeText(this, "Please enter a user ID", Toast.LENGTH_SHORT).show()
                return
            }

            currentCall = VideoCallingSDK.createCall()
            currentCall?.setOnCallEventListener(this)
            
            // Set video views first
            currentCall?.setLocalVideoView(binding.localVideoView)
            currentCall?.setRemoteVideoView(binding.remoteVideoView)
            
            // Initialize video views through SDK
            currentCall?.initializeVideoViews()

            currentCall?.start(roomId, userId)

            // Update UI state
            binding.callSetupPanel.visibility = android.view.View.GONE
            binding.callControlsLayout.visibility = android.view.View.VISIBLE
            binding.statusText.text = "Connecting..."

            Log.d(TAG, "Starting call in room: $roomId with user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call", e)
            Toast.makeText(this, "Failed to start call: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun endCall() {
        currentCall?.end()
        
        // Release video views through SDK
        currentCall?.releaseVideoViews()
        
        currentCall = null

        // Update UI state
        binding.callSetupPanel.visibility = android.view.View.VISIBLE
        binding.callControlsLayout.visibility = android.view.View.GONE
        binding.statusText.text = "Ready to call"

        Log.d(TAG, "Call ended")
    }

    private fun updateVideoButton(enabled: Boolean) {
        binding.btnToggleVideo.setImageResource(
            if (enabled) android.R.drawable.ic_menu_camera
            else android.R.drawable.ic_menu_close_clear_cancel
        )
    }

    private fun updateAudioButton(enabled: Boolean) {
        binding.btnToggleAudio.setImageResource(
            if (enabled) android.R.drawable.ic_lock_silent_mode_off
            else android.R.drawable.ic_lock_silent_mode
        )
    }

    private fun updateSpeakerButton(enabled: Boolean) {
        binding.btnToggleSpeaker.setImageResource(
            if (enabled) android.R.drawable.ic_lock_silent_mode_off
            else android.R.drawable.ic_lock_silent_mode
        )
    }

    override fun onCallStateChanged(state: CallState) {
        runOnUiThread {
            binding.statusText.text = "Call State: $state"
            Log.d(TAG, "Call state changed: $state")
        }
    }

    override fun onCallConnected() {
        runOnUiThread {
            binding.statusText.text = "Connected"
            Toast.makeText(this, "Call connected!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCallDisconnected() {
        runOnUiThread {
            binding.statusText.text = "Disconnected"
            Toast.makeText(this, "Call disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRemoteUserJoined(userId: String) {
        runOnUiThread {
            binding.statusText.text = "Remote user joined: $userId"
            Toast.makeText(this, "Remote user joined: $userId", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRemoteUserLeft(userId: String) {
        runOnUiThread {
            binding.statusText.text = "Remote user left: $userId"
            Toast.makeText(this, "Remote user left: $userId", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onVideoStateChanged(enabled: Boolean) {
        runOnUiThread {
            updateVideoButton(enabled)
        }
    }

    override fun onAudioStateChanged(enabled: Boolean) {
        runOnUiThread {
            updateAudioButton(enabled)
        }
    }

    override fun onSpeakerStateChanged(enabled: Boolean) {
        runOnUiThread {
            updateSpeakerButton(enabled)
        }
    }

    override fun onCallError(error: String) {
        runOnUiThread {
            binding.statusText.text = "Error: $error"
            Toast.makeText(this, "Call error: $error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Release video views when app goes to background
        currentCall?.releaseVideoViews()
    }

    override fun onDestroy() {
        currentCall?.end()
        currentCall?.releaseVideoViews()
        super.onDestroy()
    }
}
