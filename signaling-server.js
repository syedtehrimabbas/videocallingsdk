const WebSocket = require('ws');
const http = require('http');

const server = http.createServer();
const wss = new WebSocket.Server({ server });

// Store connected clients and rooms
const clients = new Map();
const rooms = new Map();

console.log('🚀 WebRTC Signaling Server Starting...');

wss.on('connection', (ws) => {
    const clientId = generateClientId();
    clients.set(clientId, ws);
    
    console.log(`📱 Client connected: ${clientId}`);
    
    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            handleMessage(clientId, data);
        } catch (error) {
            console.error('❌ Error parsing message:', error);
        }
    });
    
    ws.on('close', () => {
        console.log(`📱 Client disconnected: ${clientId}`);
        handleClientDisconnect(clientId);
    });
});

function handleMessage(clientId, data) {
    console.log(`📨 Message from ${clientId}:`, data.type);
    
    switch (data.type) {
        case 'JOIN_ROOM':
            handleJoinRoom(clientId, data.roomId, data.userId);
            break;
        case 'OFFER':
            handleOffer(clientId, data);
            break;
        case 'ANSWER':
            handleAnswer(clientId, data);
            break;
        case 'ICE_CANDIDATE':
            handleIceCandidate(clientId, data);
            break;
        case 'LEAVE_ROOM':
            handleLeaveRoom(clientId, data.roomId);
            break;
        default:
            console.log(`❓ Unknown message type: ${data.type}`);
    }
}

function handleJoinRoom(clientId, roomId, userId) {
    if (!rooms.has(roomId)) {
        rooms.set(roomId, new Map());
    }
    
    const room = rooms.get(roomId);
    room.set(clientId, { userId, ws: clients.get(clientId) });
    
    console.log(`🏠 User ${userId} joined room ${roomId}`);
    
    // Notify other users in the room
    broadcastToRoom(roomId, clientId, {
        type: 'USER_JOINED',
        userId: userId,
        roomId: roomId
    });
    
    // Send room info to the joining user
    const roomUsers = Array.from(room.values()).map(client => client.userId);
    sendToClient(clientId, {
        type: 'ROOM_INFO',
        roomId: roomId,
        users: roomUsers
    });
}

function handleOffer(clientId, data) {
    const room = findRoomByClientId(clientId);
    if (room) {
        broadcastToRoom(room.roomId, clientId, {
            type: 'OFFER',
            offer: data.offer,
            roomId: room.roomId
        });
    }
}

function handleAnswer(clientId, data) {
    const room = findRoomByClientId(clientId);
    if (room) {
        broadcastToRoom(room.roomId, clientId, {
            type: 'ANSWER',
            answer: data.answer,
            roomId: room.roomId
        });
    }
}

function handleIceCandidate(clientId, data) {
    const room = findRoomByClientId(clientId);
    if (room) {
        broadcastToRoom(room.roomId, clientId, {
            type: 'ICE_CANDIDATE',
            candidate: data.candidate,
            roomId: room.roomId
        });
    }
}

function handleLeaveRoom(clientId, roomId) {
    const room = rooms.get(roomId);
    if (room && room.has(clientId)) {
        const userInfo = room.get(clientId);
        room.delete(clientId);
        
        console.log(`👋 User ${userInfo.userId} left room ${roomId}`);
        
        // Notify other users
        broadcastToRoom(roomId, clientId, {
            type: 'USER_LEFT',
            userId: userInfo.userId,
            roomId: roomId
        });
        
        // Clean up empty rooms
        if (room.size === 0) {
            rooms.delete(roomId);
            console.log(`🗑️ Room ${roomId} deleted (empty)`);
        }
    }
}

function handleClientDisconnect(clientId) {
    // Find and clean up all rooms this client was in
    for (const [roomId, room] of rooms.entries()) {
        if (room.has(clientId)) {
            handleLeaveRoom(clientId, roomId);
        }
    }
    
    clients.delete(clientId);
}

function broadcastToRoom(roomId, excludeClientId, message) {
    const room = rooms.get(roomId);
    if (room) {
        for (const [clientId, client] of room.entries()) {
            if (clientId !== excludeClientId) {
                sendToClient(clientId, message);
            }
        }
    }
}

function sendToClient(clientId, message) {
    const client = clients.get(clientId);
    if (client && client.readyState === WebSocket.OPEN) {
        client.send(JSON.stringify(message));
    }
}

function findRoomByClientId(clientId) {
    for (const [roomId, room] of rooms.entries()) {
        if (room.has(clientId)) {
            return { roomId, room };
        }
    }
    return null;
}

function generateClientId() {
    return Math.random().toString(36).substr(2, 9);
}

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => {
    console.log(`🎯 Signaling server running on port ${PORT}`);
    console.log(`🌐 Connect to: ws://localhost:${PORT}`);
    console.log(`📱 For testing, use room IDs like: "test-room-123"`);
});

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\n🛑 Shutting down signaling server...');
    wss.close(() => {
        server.close(() => {
            console.log('✅ Server stopped');
            process.exit(0);
        });
    });
});
