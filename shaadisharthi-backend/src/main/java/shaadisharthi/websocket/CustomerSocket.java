package shaadisharthi.websocket;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.servlet.http.Cookie;
import java.util.*;
import io.jsonwebtoken.Claims;
import shaadisharthi.security.JwtUtil;
import shaadisharthi.utils.NotificationService;
import shaadisharthi.utils.AsyncExecutor;

/**
 * WebSocket endpoint for customer real-time communication
 * Handles WebSocket connections, authentication, and message routing for customers
 * 
 * Security Features:
 * - JWT token validation from cookies
 * - Automatic session cleanup on connection close
 * - Authentication failure handling with proper close codes
 * 
 * @ServerEndpoint Configured with custom SocketConfigurator for header access
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@ServerEndpoint(value = "/CustomerSocket", configurator = shaadisharthi.utils.SocketConfigurator.class)
public class CustomerSocket {

    /**
     * Handles new WebSocket connection establishment
     * Performs JWT authentication and session registration
     * 
     * @param session WebSocket session object
     * @param config Endpoint configuration containing HTTP headers
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        try {
            // Extract headers from the WebSocket handshake request
            Map<String, List<String>> headers = (Map<String, List<String>>) config.getUserProperties().get("headers");
            List<String> cookies = headers.get("cookie");

            // Validate presence of cookies
            if (cookies == null || cookies.isEmpty()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Missing cookies"));
                return;
            }

            // Extract JWT token from cookies
            String token = extractTokenFromCookies(cookies);
            if (token == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Missing session token"));
                return;
            }

            // Validate JWT token
            Claims claims = JwtUtil.parseToken(token);
            if (claims == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
                return;
            }

            // Extract customer ID from token and register session
            int customerId = Integer.parseInt(claims.getSubject());
            CustomerSocketManager.addSession(customerId, session);
            
            // Send welcome message and notification (commented out notification creation)
            //session.getBasicRemote().sendText("Welcome to ShaadiSharthi!");
            CustomerSocket.notifyCustomer(customerId, "Welcome to ShaadiSharthi!", customerId);

        } catch (Exception e) {
            // Handle authentication failures gracefully
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Auth failed"));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Handles incoming messages from client
     * Currently logs messages - can be extended for bidirectional communication
     * 
     * @param message Message content from client
     * @param session WebSocket session object
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        // Optional: you can handle client messages here if needed
        System.out.println("Message from client: " + message);
    }

    /**
     * Handles WebSocket connection closure
     * Cleans up session from CustomerSocketManager
     * 
     * @param session WebSocket session object
     * @param reason Reason for connection closure
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        CustomerSocketManager.removeSession(getCustomerIdFromSession(session));
    }

    /**
     * Handles WebSocket errors
     * Performs session cleanup and logs the error
     * 
     * @param session WebSocket session object
     * @param throwable Exception that occurred
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        CustomerSocketManager.removeSession(getCustomerIdFromSession(session));
        throwable.printStackTrace();
    }

    /**
     * Extracts customer ID from session user properties
     * 
     * @param session WebSocket session object
     * @return customer ID if found, -1 otherwise
     */
    private int getCustomerIdFromSession(Session session) {
        try {
            return session.getUserProperties().containsKey("customerId") ?
                   (int) session.getUserProperties().get("customerId") : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Extracts JWT token from cookie headers
     * Looks for "session" cookie containing the authentication token
     * 
     * @param cookies List of cookie headers from WebSocket handshake
     * @return JWT token string if found, null otherwise
     */
    private String extractTokenFromCookies(List<String> cookies) {
        for (String cookieHeader : cookies) {
            String[] cookiePairs = cookieHeader.split(";");
            for (String pair : cookiePairs) {
                String[] keyValue = pair.trim().split("=");
                if (keyValue.length == 2 && keyValue[0].equals("session")) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    /**
     * Utility method to send notifications to customers asynchronously
     * Uses AsyncExecutor for non-blocking message delivery
     * 
     * @param customerId Target customer identifier
     * @param message Notification message content
     * @param relatedId Related entity ID (for notification context)
     */
    public static void notifyCustomer(int customerId, String message, int relatedId) {
        AsyncExecutor.runAsync(() -> {
            try {
                // Notification service creation commented out - can be enabled when needed
                //NotificationService.createNotification(customerId, "customer", message, relatedId);
                
                // Send real-time message via WebSocket
                CustomerSocketManager.sendMessage(customerId, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}