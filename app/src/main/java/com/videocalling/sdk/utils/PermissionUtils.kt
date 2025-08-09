package com.videocalling.sdk.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility class for handling permissions
 */
object PermissionUtils {
    
    /**
     * Check if camera permission is granted
     * @param context Application context
     * @return true if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if microphone permission is granted
     * @param context Application context
     * @return true if microphone permission is granted
     */
    fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if all required permissions are granted
     * @param context Application context
     * @return true if all permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasCameraPermission(context) && hasMicrophonePermission(context)
    }
    
    /**
     * Get list of required permissions
     * @return Array of required permission strings
     */
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
} 