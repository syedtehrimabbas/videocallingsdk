# WebRTC SDK Testing Guide

## 🚀 Quick Start

### 1. Start the Signaling Server

```bash
# Install dependencies
npm install

# Start the server
npm start
```

You should see:
```
🚀 WebRTC Signaling Server Starting...
🎯 Signaling server running on port 8080
🌐 Connect to: ws://localhost:8080
📱 For testing, use room IDs like: "test-room-123"
```

### 2. Configure Network Settings

#### For Android Emulator:
- Use `ws://10.0.2.2:8080` (already configured in SDK)

#### For Real Devices:
1. Find your computer's IP address:
   ```bash
   # On Mac/Linux
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # On Windows
   ipconfig
   ```

2. Update the SDK config:
   ```kotlin
   // In SDKConfig.kt
   const val DEFAULT_SIGNALING_URL = "ws://YOUR_IP_ADDRESS:8080"
   ```

### 3. Test with Two Devices

#### Device 1 (Caller):
1. Install the demo app
2. Start a call with room ID: `test-room-123`
3. Wait for connection

#### Device 2 (Callee):
1. Install the demo app
2. Start a call with the same room ID: `test-room-123`
3. Both devices should connect and see each other's video

## 📱 Testing Scenarios

### Basic Video Call
- ✅ Both devices join same room
- ✅ Video streams appear
- ✅ Audio works
- ✅ Call controls work (mute, camera, speaker)

### Network Testing
- ✅ Works on same WiFi
- ✅ Works on different networks (via STUN servers)
- ✅ Handles connection drops gracefully

### Device Testing
- ✅ Android Emulator ↔ Real Device
- ✅ Real Device ↔ Real Device
- ✅ Different Android versions

## 🔧 Troubleshooting

### "Connection Failed"
1. Check if signaling server is running
2. Verify IP address in SDK config
3. Check firewall settings
4. Ensure both devices are on same network (or use TURN servers)

### "No Video"
1. Grant camera permissions
2. Check if camera is being used by another app
3. Verify video initialization in logs

### "No Audio"
1. Grant microphone permissions
2. Check device audio settings
3. Verify audio track creation

## 📊 Server Logs

The signaling server shows real-time connection status:

```
📱 Client connected: abc123def
🏠 User user-123 joined room test-room-123
📨 Message from abc123def: OFFER
📨 Message from def456ghi: ANSWER
📨 Message from abc123def: ICE_CANDIDATE
👋 User user-456 left room test-room-123
```

## 🎯 Next Steps

### For Production:
1. **Deploy Signaling Server** to cloud (AWS, Google Cloud, etc.)
2. **Add TURN Servers** for NAT traversal
3. **Implement Authentication** (API keys, user management)
4. **Add Room Management** (create/join rooms, user limits)
5. **Add Recording** functionality
6. **Add Screen Sharing** support

### For Development:
1. **Add More Features** (chat, file sharing)
2. **Improve UI** (better video layouts, controls)
3. **Add Analytics** (call quality, connection stats)
4. **Add Testing** (unit tests, integration tests)

## 📝 Room IDs for Testing

Use these room IDs for different test scenarios:
- `test-room-123` - Basic video call
- `test-room-audio` - Audio-only testing
- `test-room-quality` - High-quality video testing
- `test-room-multi` - Multi-party call testing

## 🔍 Debug Mode

Enable debug logging in the demo app:
```kotlin
VideoCallingSDK.setDebugMode(true)
```

This will show detailed WebRTC connection logs in Android Studio's Logcat.
