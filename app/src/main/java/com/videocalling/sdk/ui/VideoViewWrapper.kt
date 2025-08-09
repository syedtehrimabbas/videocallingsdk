package com.videocalling.sdk.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

/**
 * Wrapper for SurfaceViewRenderer that handles WebRTC video rendering
 */
class VideoViewWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val surfaceViewRenderer: SurfaceViewRenderer
    
    init {
        surfaceViewRenderer = SurfaceViewRenderer(context)
        addView(surfaceViewRenderer)
    }
    
    /**
     * Get the underlying SurfaceViewRenderer
     */
    fun getSurfaceViewRenderer(): SurfaceViewRenderer = surfaceViewRenderer
    
    /**
     * Initialize the video view
     */
    fun initialize(eglBase: EglBase) {
        try {
            surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        } catch (e: Exception) {
            // Already initialized, ignore
        }
    }
    
    /**
     * Release the video view
     */
    fun release() {
        try {
            surfaceViewRenderer.release()
        } catch (e: Exception) {
            // Already released, ignore
        }
    }
}
