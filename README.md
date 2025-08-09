# WebRTC Video Calling SDK for Android

A comprehensive Android SDK for implementing real-time video calling functionality using WebRTC technology. This SDK provides a simple and robust solution for adding video calling capabilities to Android applications.

## 🚀 Features

- **Real-time Video Calling**: High-quality video and audio communication
- **WebRTC Technology**: Industry-standard peer-to-peer communication
- **Signaling Server**: Built-in WebSocket-based signaling for connection establishment
- **Cross-Platform**: Works between Android devices and emulators
- **Easy Integration**: Simple API for quick implementation
- **Error Handling**: Robust error handling and graceful failure recovery
- **Customizable UI**: Flexible video view integration
- **Network Adaptation**: Automatic handling of different network conditions

## 📋 Requirements

- Android API Level 21 (Android 5.0) or higher
- Internet connection for signaling and STUN/TURN servers
- Camera and microphone permissions
- WebRTC library: `io.github.webrtc-sdk:android:114.5735.08`

## 🛠️ Installation

### 1. Add the SDK to your project

Include the SDK as a dependency in your `build.gradle` file:

```gradle
dependencies {
    implementation project(':app') // If using the SDK as a module
    // Or include the AAR file directly
    implementation files('libs/videocalling-sdk.aar')
}
```

### 2. Add required permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

### 3. Request runtime permissions

Use the provided permission handling or implement your own:

```kotlin
// Request camera and microphone permissions
Dexter.withContext(this)
    .withPermissions(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    .withListener(object : MultiplePermissionsListener {
        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
            if (report?.areAllPermissionsGranted() == true) {
                // Permissions granted, proceed with video call
            }
        }
        override fun onPermissionRationaleShouldBeShown(
            permissions: MutableList<PermissionRequest>?,
            token: PermissionToken?
        ) {
            token?.continuePermissionRequest()
        }
    }).check()
```

## 🚀 Quick Start

### 1. Initialize the SDK

```kotlin
// Initialize the SDK with your API key
VideoCallingSDK.initialize(
    apiKey = "your-api-key", // Optional, defaults to "demo-api-key"
    signalingUrl = "ws://your-signaling-server:8080" // Optional, auto-detected
)
```

### 2. Set up video views

```kotlin
// Create video view wrappers
val localVideoView = VideoViewWrapper(localVideoSurfaceView)
val remoteVideoView = VideoViewWrapper(remoteVideoSurfaceView)

// Initialize video views
localVideoView.initialize(EglBase.create().eglBaseContext)
remoteVideoView.initialize(EglBase.create().eglBaseContext)
```

### 3. Start a video call

```kotlin
// Create and start a video call
val videoCall = VideoCallingSDK.createVideoCall(
    roomId = "room-123",
    userId = "user-456",
    localVideoView = localVideoView,
    remoteVideoView = remoteVideoView
)

// Set up call event listener
videoCall.setCallEventListener(object : CallEventListener {
    override fun onCallStateChanged(state: CallState) {
        when (state) {
            CallState.CONNECTING -> Log.d("Call", "Connecting...")
            CallState.CONNECTED -> Log.d("Call", "Connected!")
            CallState.DISCONNECTED -> Log.d("Call", "Disconnected")
            CallState.ERROR -> Log.e("Call", "Error occurred")
        }
    }
    
    override fun onRemoteUserJoined(userId: String) {
        Log.d("Call", "Remote user joined: $userId")
    }
    
    override fun onRemoteUserLeft(userId: String) {
        Log.d("Call", "Remote user left: $userId")
    }
})

// Start the call
videoCall.start()
```

### 4. End the call

```kotlin
// End the call when done
videoCall.end()

// Release video views
localVideoView.release()
remoteVideoView.release()
```

## 📱 Complete Example

Here's a complete example of implementing video calling in an Activity:

```kotlin
class VideoCallActivity : AppCompatActivity() {
    private lateinit var videoCall: VideoCall
    private lateinit var localVideoView: VideoViewWrapper
    private lateinit var remoteVideoView: VideoViewWrapper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)
        
        // Initialize SDK
        VideoCallingSDK.initialize()
        
        // Set up video views
        setupVideoViews()
        
        // Start video call
        startVideoCall()
    }
    
    private fun setupVideoViews() {
        val localSurfaceView = findViewById<SurfaceViewRenderer>(R.id.localVideoView)
        val remoteSurfaceView = findViewById<SurfaceViewRenderer>(R.id.remoteVideoView)
        
        localVideoView = VideoViewWrapper(localSurfaceView)
        remoteVideoView = VideoViewWrapper(remoteSurfaceView)
        
        val eglBase = EglBase.create()
        localVideoView.initialize(eglBase.eglBaseContext)
        remoteVideoView.initialize(eglBase.eglBaseContext)
    }
    
    private fun startVideoCall() {
        val roomId = intent.getStringExtra("room_id") ?: "default-room"
        val userId = intent.getStringExtra("user_id") ?: "user-${System.currentTimeMillis()}"
        
        videoCall = VideoCallingSDK.createVideoCall(
            roomId = roomId,
            userId = userId,
            localVideoView = localVideoView,
            remoteVideoView = remoteVideoView
        )
        
        videoCall.setCallEventListener(object : CallEventListener {
            override fun onCallStateChanged(state: CallState) {
                runOnUiThread {
                    updateCallState(state)
                }
            }
            
            override fun onRemoteUserJoined(userId: String) {
                runOnUiThread {
                    showMessage("Remote user joined: $userId")
                }
            }
            
            override fun onRemoteUserLeft(userId: String) {
                runOnUiThread {
                    showMessage("Remote user left: $userId")
                }
            }
        })
        
        videoCall.start()
    }
    
    private fun updateCallState(state: CallState) {
        val statusText = when (state) {
            CallState.CONNECTING -> "Connecting..."
            CallState.CONNECTED -> "Connected"
            CallState.DISCONNECTED -> "Disconnected"
            CallState.ERROR -> "Error"
        }
        findViewById<TextView>(R.id.statusText).text = statusText
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        videoCall.end()
        localVideoView.release()
        remoteVideoView.release()
    }
}
```

## 🔧 Configuration

### Signaling Server

The SDK automatically detects and configures the signaling server URL:

- **Emulator**: Uses `ws://10.0.2.2:8080`
- **Real Device**: Uses your machine's Wi-Fi IP address

You can also specify a custom signaling server:

```kotlin
VideoCallingSDK.initialize(
    signalingUrl = "ws://your-custom-server:8080"
)
```

### STUN/TURN Servers

The SDK uses Google's public STUN servers by default. For production use, consider using your own TURN servers for better connectivity:

```kotlin
// Configure custom STUN/TURN servers in WebRTCManager
val rtcConfig = PeerConnection.RTCConfiguration(
    iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:your-turn-server.com:3478")
            .setUsername("username")
            .setPassword("password")
            .createIceServer()
    )
)
```

## 🧪 Testing

### Local Testing

1. **Start the signaling server**:
   ```bash
   cd signaling-server
   npm install
   npm start
   ```

2. **Configure network**:
   - For emulator testing: Use `10.0.2.2` as the signaling server
   - For real device testing: Use your machine's Wi-Fi IP address

3. **Test scenarios**:
   - Emulator to Emulator
   - Real Device to Real Device
   - Emulator to Real Device

### Network Configuration

Find your machine's IP address:
```bash
# macOS/Linux
ifconfig | grep "inet " | grep -v 127.0.0.1

# Windows
ipconfig
```

Update the signaling server URL in `SDKConfig.kt` if needed.

## 🐛 Troubleshooting

### Common Issues

1. **App crashes on ICE candidate handling**:
   - The SDK now includes robust error handling
   - Problematic candidates are skipped gracefully
   - Check logs for "Skipping problematic ICE candidate" messages

2. **No video appearing**:
   - Ensure camera permissions are granted
   - Check that video views are properly initialized
   - Verify WebRTC connection state

3. **Connection issues**:
   - Check network connectivity
   - Verify signaling server is running
   - Ensure STUN/TURN servers are accessible

4. **Build errors**:
   - Ensure all dependencies are included
   - Check that WebRTC library version is compatible
   - Verify NDK configuration for native libraries

### Debug Logging

Enable debug logging to troubleshoot issues:

```kotlin
// Enable WebRTC logging
Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE)
```

### Log Analysis

Look for these key log messages:
- `"WebRTC initialized successfully"` - SDK initialization
- `"Connected to signaling server"` - Signaling connection
- `"ICE candidate added successfully"` - ICE candidate processing
- `"ICE connection: CONNECTED"` - WebRTC connection established
- `"Skipping problematic ICE candidate"` - Graceful error handling

## 📚 API Reference

### VideoCallingSDK

Main SDK class for initialization and video call creation.

```kotlin
object VideoCallingSDK {
    fun initialize(apiKey: String = "demo-api-key", signalingUrl: String? = null)
    fun createVideoCall(roomId: String, userId: String, localVideoView: VideoViewWrapper, remoteVideoView: VideoViewWrapper): VideoCall
}
```

### VideoCall

Manages individual video call sessions.

```kotlin
class VideoCall {
    fun start()
    fun end()
    fun setCallEventListener(listener: CallEventListener)
    fun initializeVideoViews()
    fun releaseVideoViews()
}
```

### VideoViewWrapper

Wraps WebRTC SurfaceViewRenderer for easy management.

```kotlin
class VideoViewWrapper(surfaceViewRenderer: SurfaceViewRenderer) {
    fun initialize(eglBaseContext: EglBaseContext)
    fun release()
}
```

### CallEventListener

Interface for receiving call events.

```kotlin
interface CallEventListener {
    fun onCallStateChanged(state: CallState)
    fun onRemoteUserJoined(userId: String)
    fun onRemoteUserLeft(userId: String)
}
```

### CallState

Enumeration of possible call states.

```kotlin
enum class CallState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}
```

## 🔒 Security Considerations

- Use HTTPS/WSS for production signaling servers
- Implement proper authentication for your API keys
- Use TURN servers for production deployments
- Consider implementing end-to-end encryption
- Validate all user inputs and room IDs

## 📄 License

This SDK is provided as-is for development and testing purposes. For production use, ensure compliance with WebRTC licensing and your application's requirements.

## 🤝 Support

For issues and questions:
1. Check the troubleshooting section
2. Review the debug logs
3. Test with the provided demo application
4. Ensure all dependencies are correctly configured

## 🔄 Version History

- **v1.0.0**: Initial release with basic video calling functionality
- **v1.1.0**: Added robust error handling and graceful failure recovery
- **v1.2.0**: Improved ICE candidate handling and network adaptation

---

**Note**: This SDK is designed for development and testing. For production deployments, consider implementing additional security measures, proper error handling, and performance optimizations.
