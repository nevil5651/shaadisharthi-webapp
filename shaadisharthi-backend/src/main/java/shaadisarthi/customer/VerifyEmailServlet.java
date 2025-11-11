package shaadisarthi.customer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;

/**
 * Servlet implementation for customer email verification
 * Handles email verification token validation and account activation
 * 
 * @WebServlet Maps to "/cstmr-email-verification" endpoint
 * @version 1.0
 * @description Processes GET requests to verify customer email addresses using JWT tokens
 */
@WebServlet("/cstmr-email-verification")
public class VerifyEmailServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(VerifyEmailServlet.class);

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
     * Processes GET requests for email verification
     * Validates JWT token and updates email verification status in database
     * 
     * @param request HttpServletRequest containing verification token
     * @param response HttpServletResponse for sending verification result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for /email-verification");
        String token = request.getParameter("token");
        JSONObject responseJson = new JSONObject();

        // Validate token presence
        if (token == null || token.trim().isEmpty()) {
            logger.warn("No token provided");
            responseJson.put("error", "Invalid token");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Retrieve and validate JWT secret from configuration
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
                        if ("used".equals(status)) {
                            logger.info("Token already used for email: {}", email);
                            responseJson.put("error", "Token already used");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                        // Update token status to 'verified' if currently pending
                        if (!"verified".equals(status)) {
                            String updateQuery = "UPDATE email_verification_tokens SET status = 'verified' WHERE email = ? AND token = ?";
                            try (PreparedStatement updatePstmt = con.prepareStatement(updateQuery)) {
                                updatePstmt.setString(1, email);
                                updatePstmt.setString(2, token);
                                int rows = updatePstmt.executeUpdate();
                                if (rows == 0) {
                                    logger.error("Failed to update token status for email: {}", email);
                                    responseJson.put("error", "Server error");
                                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                                    return;
                                }
                            }
                        }
                    }
                }

                con.commit();
                responseJson.put("email", email);
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