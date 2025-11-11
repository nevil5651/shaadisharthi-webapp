package shaadisharthi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;

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

/**
 * ResetPassword - Service provider password reset completion servlet
 * 
 * Handles secure password reset completion:
 * - JWT token validation and expiration checking
 * - Strong password policy enforcement
 * - BCrypt password hashing
 * - Token invalidation after use
 * - Transaction-safe database operations
 * 
 * Endpoint: /reset-password
 * Method: POST
 * 
 * @category Authentication & Security
 */
@WebServlet("/reset-password")
public class ResetPassword extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ResetPassword.class);
    // Strong password policy regex
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    /**
     * Send standardized JSON response
     * 
     * @param response HTTP response
     * @param status HTTP status code
     * @param json JSON content
     * @throws IOException If response writing fails
     */
    private void sendResponse(HttpServletResponse response, int status, JSONObject json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Log password reset attempt for audit purposes
     * 
     * @param con Database connection
     * @param email User email address
     * @param ipAddress Client IP address
     * @param success Whether the reset attempt was successful
     * @throws SQLException If database insert fails
     */
    private void logAttempt(Connection con, String email, String ipAddress, boolean success) throws SQLException {
        String query = "INSERT INTO reset_attempts (email, ip_address, attempt_time) VALUES (?, ?, NOW())";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, ipAddress);
            pstmt.executeUpdate();
            logger.debug("Logged reset attempt for email: {}, IP: {}, Success: {}", email, ipAddress, success);
        }
    }

    /**
     * POST /reset-password - Complete password reset process
     * 
     * Process flow:
     * 1. Parse JSON payload with token and new password
     * 2. Validate password against strong policy
     * 3. Decode and verify JWT reset token
     * 4. Check token validity and expiration in database
     * 5. Hash new password with BCrypt
     * 6. Update user password in database
     * 7. Mark token as used to prevent reuse
     * 8. Log successful reset attempt
     * 
     * @param request HTTP request with JSON payload
     * @param response JSON response with operation result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /api/reset-password");
        JSONObject responseJson = new JSONObject();
        String ipAddress = request.getRemoteAddr();

        // Parse JSON payload from request body
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
        } catch (IOException e) {
            logger.error("Failed to read request payload: {}", e.getMessage(), e);
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        JSONObject jsonPayload;
        String token;
        String password;
        try {
            jsonPayload = new JSONObject(payload.toString());
            token = jsonPayload.getString("token");
            password = jsonPayload.getString("password");
        } catch (Exception e) {
            logger.warn("Invalid JSON payload: {}", payload);
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Validate password against strong policy
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            logger.warn("Invalid password format for token: {}", token);
            responseJson.put("error", "Password must be at least 8 characters, with one uppercase, one lowercase, one digit, and one special character");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Decode Base64 JWT reset secret from configuration
        String jwtResetSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
        if (jwtResetSecretBase64 == null) {
            logger.error("Missing JWT reset secret in environment variables or config.properties");
            responseJson.put("error", "Server configuration error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            return;
        }
        byte[] jwtResetSecret;
        try {
            jwtResetSecret = Base64.getDecoder().decode(jwtResetSecretBase64);
            if (jwtResetSecret.length < 32) {
                logger.error("JWT reset secret is too short after decoding");
                responseJson.put("error", "Server configuration error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                return;
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid Base64-encoded JWT reset secret: {}", e.getMessage(), e);
            responseJson.put("error", "Server configuration error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            return;
        }

        // Verify JWT token and extract email
        String email;
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtResetSecret))
                    .build()
                    .parseClaimsJws(token);
            email = jws.getBody().getSubject();
            if (email == null || email.isEmpty()) {
                logger.warn("JWT token has no subject: {}", token);
                responseJson.put("error", "Invalid or expired token");
                sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                return;
            }
        } catch (Exception e) {
            logger.warn("Invalid or expired JWT token: {}", e.getMessage(), e);
            responseJson.put("error", "Invalid or expired token");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false);
            try {
                // Check if token exists and is valid in database
                String query = "SELECT expiry, used FROM password_reset_tokens WHERE email = ? AND token = ?";
                try (PreparedStatement pstmt = con.prepareStatement(query)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            logger.info("No valid token found for email: {}", email);
                            logAttempt(con, email, ipAddress, false);
                            con.commit();
                            responseJson.put("error", "Invalid or expired token");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                        boolean used = rs.getBoolean("used");
                        Date expiry = rs.getTimestamp("expiry");
                        if (used || expiry.before(new Date())) {
                            logger.info("Token is used or expired for email: {}", email);
                            logAttempt(con, email, ipAddress, false);
                            con.commit();
                            responseJson.put("error", "Invalid or expired token");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                    }
                }

                // Verify email exists in service_providers table
                query = "SELECT provider_id FROM service_providers WHERE email = ?";
                try (PreparedStatement pstmt = con.prepareStatement(query)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            logger.info("No service provider found for email: {}", email);
                            logAttempt(con, email, ipAddress, false);
                            con.commit();
                            responseJson.put("error", "Invalid or expired token");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                    }
                }

                // Hash new password using BCrypt with salt
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                // Update password in service_providers table
                query = "UPDATE service_providers SET password = ? WHERE email = ?";
                try (PreparedStatement pstmt = con.prepareStatement(query)) {
                    pstmt.setString(1, hashedPassword);
                    pstmt.setString(2, email);
                    int rows = pstmt.executeUpdate();
                    if (rows == 0) {
                        logger.error("Failed to update password for email: {}", email);
                        logAttempt(con, email, ipAddress, false);
                        con.rollback();
                        responseJson.put("error", "Failed to reset password");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                }

                // Mark token as used to prevent reuse
                query = "UPDATE password_reset_tokens SET used = TRUE WHERE email = ? AND token = ?";
                try (PreparedStatement pstmt = con.prepareStatement(query)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    pstmt.executeUpdate();
                }

                // Log successful reset attempt
                logAttempt(con, email, ipAddress, true);

                con.commit();
                logger.info("Password reset successfully for email: {}", email);
                responseJson.put("message", "Password reset successfully");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error processing reset-password for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to connect to database for email {}: {}", email, e.getMessage(), e);
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}