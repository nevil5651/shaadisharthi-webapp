package shaadisharthi;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import shaadisharthi.DbConnection.DbConnection;

/**
 * Servlet for handling email verification token validation
 * Completes the email verification process by validating JWT tokens
 * 
 * Features:
 * - JWT token validation and parsing
 * - Database token verification
 * - Transaction-safe token status updates
 * 
 * @WebServlet Maps to "/email-verification" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/email-verification")
public class VerifyEmailServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(VerifyEmailServlet.class);
    private static final Properties config = new Properties();

    // Static initialization for configuration loading
    static {
        try {
            config.load(VerifyEmailServlet.class.getClassLoader().getResourceAsStream("config.properties"));
            logger.info("Loaded config.properties for email verification");
        } catch (IOException e) {
            logger.error("Failed to load config.properties: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends standardized HTTP responses with JSON content
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param json JSON content to send
     * @throws IOException If response writing fails
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
     * Handles GET requests for email verification
     * Validates JWT tokens and marks email as verified in database
     * 
     * @param request HttpServletRequest with token parameter
     * @param response HttpServletResponse with verification result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for /api/verify-email");
        String token = request.getParameter("token");
        JSONObject responseJson = new JSONObject();

        // Validate token presence
        if (token == null || token.trim().isEmpty()) {
            logger.warn("No token provided");
            responseJson.put("error", "Invalid token");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Load JWT secret from configuration
        String jwtSecretBase64 = config.getProperty("jwt.reset.secret.key");
        if (jwtSecretBase64 == null) {
            logger.error("Missing JWT secret in config.properties");
            responseJson.put("error", "Server configuration error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            return;
        }
        
        // Decode JWT secret from Base64
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
                // Verify token exists in database and is not expired
                String query = "SELECT expiry, status FROM email_verification_tokens WHERE email = ? AND token = ? AND expiry > NOW()";
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
                        // Check if token is still in pending state
                        if (!"pending".equals(status)) {
                            logger.info("Token already used or verified for email: {}", email);
                            responseJson.put("error", "Invalid or expired token");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                    }
                }

                // Mark token as verified to complete verification process
                String updateQuery = "UPDATE email_verification_tokens SET status = 'verified' WHERE email = ? AND token = ?";
                try (PreparedStatement pstmt = con.prepareStatement(updateQuery)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    int rows = pstmt.executeUpdate();
                    if (rows == 0) {
                        logger.error("Failed to update token status for email: {}", email);
                        responseJson.put("error", "Server error");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                }

                // Commit transaction on success
                con.commit();
                responseJson.put("email", email);
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                // Rollback transaction on error
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