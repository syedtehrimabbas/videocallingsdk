package com.videocalling.sdk.model

/**
 * Represents a signaling message for WebSocket communication
 */
data class SignalingMessage(
    val type: Type,
    val data: String,
    val roomId: String? = null,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Types of signaling messages
     */
    enum class Type {
        JOIN_ROOM,
        OFFER,
        ANSWER,
        ICE_CANDIDATE,
        USER_JOINED,
        USER_LEFT,
        ROOM_INFO,
        ERROR,
        PING,
        PONG
    }
}

/**
 * Specific message for OFFER
 */
data class OfferMessage(
    val type: SignalingMessage.Type = SignalingMessage.Type.OFFER,
    val offer: String,
    val roomId: String? = null,
    val userId: String? = null
)

/**
 * Specific message for ANSWER
 */
data class AnswerMessage(
    val type: SignalingMessage.Type = SignalingMessage.Type.ANSWER,
    val answer: String,
    val roomId: String? = null,
    val userId: String? = null
)

/**
 * Specific message for ICE_CANDIDATE
 */
data class IceCandidateMessage(
    val type: SignalingMessage.Type = SignalingMessage.Type.ICE_CANDIDATE,
    val candidate: String,
    val roomId: String? = null,
    val userId: String? = null
) 