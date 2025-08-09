# Keep SDK public API
-keep class com.videocalling.sdk.VideoCallingSDK { *; }
-keep class com.videocalling.sdk.VideoCall { *; }
-keep interface com.videocalling.sdk.callback.CallEventListener { *; }
-keep class com.videocalling.sdk.model.CallState { *; }
-keep class com.videocalling.sdk.ui.VideoView { *; }
-keep class com.videocalling.sdk.ui.CallControls { *; }

# Keep public methods and fields
-keep public class com.videocalling.sdk.** {
    public <methods>;
    public <fields>;
}

# Keep callback methods
-keepclassmembers class * implements com.videocalling.sdk.callback.CallEventListener {
    public void onCallConnected();
    public void onCallDisconnected();
    public void onRemoteUserJoined(java.lang.String);
    public void onRemoteUserLeft(java.lang.String);
    public void onCallStateChanged(com.videocalling.sdk.model.CallState);
    public void onVideoStateChanged(boolean);
    public void onAudioStateChanged(boolean);
    public void onSpeakerStateChanged(boolean);
    public void onCallError(java.lang.String);
    public void onConnectionQualityChanged(int);
} 