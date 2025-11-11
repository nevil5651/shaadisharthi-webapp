package shaadisarthi.customer;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;

/**
 * Servlet implementation for password reset completion
 * Handles password reset token validation and password update
 * 
 * @WebServlet Maps to "/cstmr-reset-password" endpoint
 * @version 1.0
 * @description Processes POST requests to reset passwords using valid reset tokens
 */
@WebServlet("/cstmr-reset-password")
public class ResetPasswordServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordServlet.class);

    /**
     * Sends standardized JSON response to client
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param json JSONObject containing response data
     * @throws IOException if response writing fails
     */
    private void sendResponse(HttpServletResponse response, int status, JSONObject json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (var writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Validates password strength requirements
     * 
     * @param password Password to validate
     * @param response HttpServletResponse for sending error responses
     * @return true if password is valid, false otherwise
     * @throws IOException if response writing fails
     */
    private boolean validatePassword(String password, HttpServletResponse response) throws IOException {
        JSONObject responseJson = new JSONObject();
        if (password == null || password.trim().isEmpty()) {
            responseJson.put("error", "Password is required");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return false;
        }
        if (password.length() < 8) {
            responseJson.put("error", "Password must be at least 8 characters");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return false;
        }
        // Additional strength checks if needed (e.g., regex for complexity)
        return true;
    }

    /**
     * Processes POST requests for password reset completion
     * Validates reset token and updates customer password
     * 
     * @param request HttpServletRequest containing new password and reset token
     * @param response HttpServletResponse for sending reset result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /reset-password");
        StringBuilder payload = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
        } catch (IOException e) {
            logger.error("Failed to read request payload: {}", e.getMessage(), e);
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        JSONObject jsonPayload;
        try {
            jsonPayload = new JSONObject(payload.toString());
        } catch (Exception e) {
            logger.warn("Invalid JSON payload: {}", payload);
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Extract password and token from request
        String password = jsonPayload.optString("password", null);
        String token = jsonPayload.optString("token", null);

        JSONObject responseJson = new JSONObject();

        // Validate token presence
        if (token == null || token.trim().isEmpty()) {
            responseJson.put("error", "Invalid token");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Validate password strength
        if (!validatePassword(password, response)) {
            return;
        }

        // Retrieve JWT secret for token validation
        String jwtSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
        if (jwtSecretBase64 == null) {
            logger.error("Missing JWT secret in configuration");
            responseJson.put("error", "Server configuration error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            return;
        }
        byte[] jwtSecret = Base64.getDecoder().decode(jwtSecretBase64);
        if (jwtSecret.length < 32) {
            logger.error("JWT secret too short");
            responseJson.put("error", "Server configuration error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            return;
        }

        String email;
        try {
            // Parse and validate JWT token
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret))
                    .build()
                    .parseClaimsJws(token);
            email = jws.getBody().getSubject();
            if (email == null || email.isEmpty()) {
                logger.warn("Token has no subject");
                responseJson.put("error", "Invalid token");
                sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                return;
            }
        } catch (Exception e) {
            logger.warn("Invalid or expired token: {}", e.getMessage());
            responseJson.put("error", "Invalid or expired token");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false);
            try {
                // Verify token exists and is valid in database
                String query = "SELECT expiry, status FROM reset_tokens WHERE email = ? AND token = ? AND expiry > NOW()";
                try (PreparedStatement pstmt = con.prepareStatement(query)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            logger.info("No valid token found for email: {}", email);
                            responseJson.put("error", "Invalid or expired token");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                        String status = rs.getString("status");
                        if (!"pending".equals(status)) {
                            logger.info("Token already used for email: {}", email);
                            responseJson.put("error", "Token already used");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                    }
                }

                // Hash new password with BCrypt before storage
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                // Update customer password in database
                String updatePasswordQuery = "UPDATE customers SET password = ? WHERE email = ?";
                try (PreparedStatement pstmt = con.prepareStatement(updatePasswordQuery)) {
                    pstmt.setString(1, hashedPassword);
                    pstmt.setString(2, email);
                    int rows = pstmt.executeUpdate();
                    if (rows == 0) {
                        logger.error("Failed to update password for email: {}", email);
                        responseJson.put("error", "Server error");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                }

                // Mark token as used to prevent reuse
                String updateTokenQuery = "UPDATE reset_tokens SET status = 'used' WHERE email = ? AND token = ?";
                try (PreparedStatement pstmt = con.prepareStatement(updateTokenQuery)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    pstmt.executeUpdate();
                }

                con.commit();
                responseJson.put("message", "Password reset successful");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            }
        } catch (SQLException e) {
            logger.error("Connection error for token {}: {}", token, e.getMessage(), e);
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}