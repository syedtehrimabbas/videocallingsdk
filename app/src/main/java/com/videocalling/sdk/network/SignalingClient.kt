package com.videocalling.sdk.network

import android.util.Log
import com.google.gson.Gson
import com.videocalling.sdk.model.SignalingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

/**
 * WebSocket client for signaling server communication
 */
class SignalingClient(
    private val signalingUrl: String,
    private val apiKey: String
) {
    private val TAG = "SignalingClient"
    
    private var webSocketClient: WebSocketClient? = null
    private var signalingListener: SignalingListener? = null
    private var isConnected = false
    private var roomId: String? = null
    private var userId: String? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    
    /**
     * Connect to signaling server
     * @param roomId Room identifier
     * @param userId User identifier
     */
    fun connect(roomId: String, userId: String) {
        this.roomId = roomId
        this.userId = userId
        
        scope.launch {
            try {
                val uri = URI("$signalingUrl?room=$roomId&user=$userId&apiKey=$apiKey")
                Log.d(TAG, "Attempting to connect to: $uri")
                
                webSocketClient = object : WebSocketClient(uri) {
                    override fun onOpen(handshakedata: ServerHandshake?) {
                        Log.d(TAG, "Connected to signaling server")
                        isConnected = true
                        signalingListener?.onConnected()
                        
                        // Send join room message
                        sendJoinRoomMessage()
                    }
                    
                    override fun onMessage(message: String?) {
                        message?.let { msg ->
                            handleMessage(msg)
                        }
                    }
                    
                    override fun onClose(code: Int, reason: String?, remote: Boolean) {
                        Log.d(TAG, "Disconnected from signaling server: $reason")
                        isConnected = false
                        signalingListener?.onDisconnected()
                    }
                    
                    override fun onError(ex: Exception?) {
                        Log.e(TAG, "WebSocket error", ex)
                        signalingListener?.onError(ex?.message ?: "WebSocket error")
                    }
                }
                
                webSocketClient?.connect()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to signaling server", e)
                signalingListener?.onError(e.message ?: "Failed to connect")
            }
        }
    }
    
    /**
     * Disconnect from signaling server
     */
    fun disconnect() {
        scope.launch {
            try {
                webSocketClient?.close()
                isConnected = false
                Log.d(TAG, "Disconnected from signaling server")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from signaling server", e)
            }
        }
    }
    
    /**
     * Send offer to remote peer
     * @param offer SDP offer
     */
    fun sendOffer(offer: String) {
        sendSignalingMessage(SignalingMessage.Type.OFFER, offer)
    }
    
    /**
     * Send answer to remote peer
     * @param answer SDP answer
     */
    fun sendAnswer(answer: String) {
        sendSignalingMessage(SignalingMessage.Type.ANSWER, answer)
    }
    
    /**
     * Send ICE candidate to remote peer
     * @param candidate ICE candidate
     */
    fun sendIceCandidate(candidate: String) {
        sendSignalingMessage(SignalingMessage.Type.ICE_CANDIDATE, candidate)
    }
    
    /**
     * Send join room message
     */
    private fun sendJoinRoomMessage() {
        if (isConnected && roomId != null && userId != null) {
            val message = SignalingMessage(
                type = SignalingMessage.Type.JOIN_ROOM,
                data = "",
                roomId = roomId,
                userId = userId
            )
            
            val jsonMessage = gson.toJson(message)
            webSocketClient?.send(jsonMessage)
            Log.d(TAG, "Sent JOIN_ROOM message for room: $roomId, user: $userId")
        }
    }
    
    /**
     * Set signaling listener
     * @param listener Signaling listener
     */
    fun setSignalingListener(listener: SignalingListener) {
        signalingListener = listener
    }
    
    /**
     * Check if connected to signaling server
     * @return true if connected
     */
    fun isConnected(): Boolean = isConnected
    
    private fun handleMessage(message: String) {
        try {
            Log.d(TAG, "Received message: $message")
            
            // Parse as generic message first
            val signalingMessage = gson.fromJson(message, SignalingMessage::class.java)
            
            when (signalingMessage.type) {
                SignalingMessage.Type.OFFER -> {
                    Log.d(TAG, "Received OFFER")
                    // Try to extract offer from the raw JSON
                    val offerSdp = extractFieldFromJson(message, "offer") ?: signalingMessage.data
                    signalingListener?.onOfferReceived(offerSdp)
                }
                SignalingMessage.Type.ANSWER -> {
                    Log.d(TAG, "Received ANSWER")
                    // Try to extract answer from the raw JSON
                    val answerSdp = extractFieldFromJson(message, "answer") ?: signalingMessage.data
                    signalingListener?.onAnswerReceived(answerSdp)
                }
                SignalingMessage.Type.ICE_CANDIDATE -> {
                    Log.d(TAG, "Received ICE_CANDIDATE")
                    Log.d(TAG, "Full ICE_CANDIDATE message: $message")
                    // Try to extract candidate from the raw JSON
                    val candidate = extractFieldFromJson(message, "candidate") ?: signalingMessage.data
                    Log.d(TAG, "Extracted candidate: $candidate")
                    if (candidate != null && candidate.isNotEmpty()) {
                        signalingListener?.onIceCandidateReceived(candidate)
                    } else {
                        Log.e(TAG, "ICE candidate is null or empty, message: $message")
                    }
                }
                SignalingMessage.Type.USER_JOINED -> {
                    // The server sends userId in the userId field, not data field
                    val remoteUserId = signalingMessage.userId ?: signalingMessage.data
                    Log.d(TAG, "Remote user joined: $remoteUserId")
                    Log.d(TAG, "Full USER_JOINED message: $message")
                    signalingListener?.onRemoteUserJoined(remoteUserId)
                }
                SignalingMessage.Type.USER_LEFT -> {
                    // The server sends userId in the userId field, not data field
                    val remoteUserId = signalingMessage.userId ?: signalingMessage.data
                    Log.d(TAG, "Remote user left: $remoteUserId")
                    signalingListener?.onRemoteUserLeft(remoteUserId)
                }
                SignalingMessage.Type.ROOM_INFO -> {
                    Log.d(TAG, "Received ROOM_INFO: $message")
                    // Extract users from the message and trigger onRemoteUserJoined for each existing user
                    val users = extractUsersFromRoomInfo(message)
                    users.forEach { userId ->
                        if (userId != this.userId) {
                            Log.d(TAG, "Found existing user in room: $userId")
                            signalingListener?.onRemoteUserJoined(userId)
                        }
                    }
                }
                SignalingMessage.Type.ERROR -> {
                    Log.e(TAG, "Received error: ${signalingMessage.data}")
                    signalingListener?.onError(signalingMessage.data)
                }
                else -> {
                    Log.w(TAG, "Unknown message type: ${signalingMessage.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing signaling message", e)
        }
    }
    
    private fun extractFieldFromJson(json: String, fieldName: String): String? {
        return try {
            val jsonObject = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            jsonObject.get(fieldName)?.asString
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractUsersFromRoomInfo(json: String): List<String> {
        return try {
            val jsonObject = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val usersArray = jsonObject.get("users")?.asJsonArray
            usersArray?.map { it.asString } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting users from ROOM_INFO", e)
            emptyList()
        }
    }
    
    private fun sendSignalingMessage(type: SignalingMessage.Type, data: String) {
        if (!isConnected) {
            Log.w(TAG, "Not connected to signaling server")
            return
        }
        
        try {
            val message = when (type) {
                SignalingMessage.Type.OFFER -> {
                    mapOf(
                        "type" to "OFFER",
                        "offer" to data,
                        "roomId" to roomId,
                        "userId" to userId
                    )
                }
                SignalingMessage.Type.ANSWER -> {
                    mapOf(
                        "type" to "ANSWER",
                        "answer" to data,
                        "roomId" to roomId,
                        "userId" to userId
                    )
                }
                SignalingMessage.Type.ICE_CANDIDATE -> {
                    mapOf(
                        "type" to "ICE_CANDIDATE",
                        "candidate" to data,
                        "roomId" to roomId,
                        "userId" to userId
                    )
                }
                else -> {
                    mapOf(
                        "type" to type.name,
                        "data" to data,
                        "roomId" to roomId,
                        "userId" to userId
                    )
                }
            }
            
            val jsonMessage = gson.toJson(message)
            webSocketClient?.send(jsonMessage)
            
            Log.d(TAG, "Sent message: $type with data length: ${data.length}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending signaling message", e)
        }
    }
} 