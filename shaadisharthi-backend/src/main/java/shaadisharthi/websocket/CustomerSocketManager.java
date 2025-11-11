package shaadisharthi.websocket;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.Session;

/**
 * Manages WebSocket sessions for customer real-time communication
 * Provides centralized session management for customer connections
 * 
 * Features:
 * - Thread-safe session storage using ConcurrentHashMap
 * - Individual and broadcast message delivery
 * - Session lifecycle management
 * 
 * @author ShaadiSharthi Team
 * @version 1.0
 */
public class CustomerSocketManager {
    // Thread-safe storage for customer sessions: customerId -> WebSocket session
    private static final ConcurrentHashMap<Integer, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Adds a customer session to the active sessions map
     * 
     * @param customerId Unique identifier for the customer
     * @param session WebSocket session object
     */
    public static void addSession(int customerId, Session session) {
        sessions.put(customerId, session);
    }

    /**
     * Removes a customer session from active sessions
     * Typically called when connection is closed
     * 
     * @param customerId Unique identifier for the customer
     */
    public static void removeSession(int customerId) {
        sessions.remove(customerId);
    }

    /**
     * Retrieves a specific customer's WebSocket session
     * 
     * @param customerId Unique identifier for the customer
     * @return WebSocket session if exists, null otherwise
     */
    public static Session getSession(int customerId) {
        return sessions.get(customerId);
    }

    /**
     * Sends a message to a specific customer via WebSocket
     * Uses synchronized block to ensure thread-safe message delivery
     * 
     * @param customerId Target customer identifier
     * @param message Message content to send
     * @throws IOException If message delivery fails
     */
    public static void sendMessage(int customerId, String message) throws IOException {
        Session session = sessions.get(customerId);
        if (session != null && session.isOpen()) {
            // Synchronize on session to prevent concurrent writes
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } else {
            System.out.println("No active session for customer " + customerId);
        }
    }

    /**
     * Broadcasts a message to all connected customers
     * Uses asynchronous sending for better performance with multiple clients
     * 
     * @param message Message content to broadcast
     */
    public static void broadcast(String message) {
        // Iterate through all active sessions and send message asynchronously
        sessions.values().forEach(s -> {
            if (s.isOpen()) {
                s.getAsyncRemote().sendText(message);
            }
        });
    }
}