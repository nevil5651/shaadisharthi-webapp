package shaadisharthi.utils;

import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.*;

/**
 * SocketConfigurator - WebSocket handshake configuration utility
 * 
 * Captures HTTP headers during WebSocket handshake for use in WebSocket sessions.
 * This enables access to authentication headers, cookies, and other HTTP request
 * data within WebSocket message handlers.
 * 
 * Used for real-time features like:
 * - Live notifications
 * - Chat functionality  
 * - Real-time booking updates
 * 
 * @category WebSocket & Real-time Communication
 */
public class SocketConfigurator extends ServerEndpointConfig.Configurator {
    
    /**
     * Modify WebSocket handshake to capture HTTP headers
     * 
     * Stores HTTP headers from the handshake request in the WebSocket
     * session's user properties for later access during message handling.
     * 
     * @param config WebSocket endpoint configuration
     * @param request WebSocket handshake request
     * @param response WebSocket handshake response
     */
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, javax.websocket.HandshakeResponse response) {
        // Store HTTP headers in WebSocket session user properties
        config.getUserProperties().put("headers", request.getHeaders());
    }
}