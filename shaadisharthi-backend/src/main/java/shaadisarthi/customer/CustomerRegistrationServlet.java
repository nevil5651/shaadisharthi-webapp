package shaadisarthi.customer;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
 * Servlet implementation for customer registration with email verification
 * Handles complete customer registration flow with token validation
 * 
 * @WebServlet Maps to "/cstmr-rgt" endpoint
 * @version 1.0
 * @description Processes POST requests for customer registration with email verification token validation
 */
@WebServlet("/cstmr-rgt")
public class CustomerRegistrationServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRegistrationServlet.class);

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
     * Processes POST requests for customer registration
     * Validates email verification token and creates customer account
     * 
     * @param request HttpServletRequest containing registration data and verification token
     * @param response HttpServletResponse for sending registration result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /Customer/cstmr-rgt");
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

        // Extract registration fields from JSON payload
        String name = jsonPayload.optString("name", null);
        String email = jsonPayload.optString("email", null);
        String phone = jsonPayload.optString("phone", null);
        String altPhone = jsonPayload.optString("altPhone", "");
        String password = jsonPayload.optString("password", null);
        String token = jsonPayload.optString("token", null);

        JSONObject responseJson = new JSONObject();

        // Validate required fields
        if (name == null || email == null || phone == null || password == null || token == null) {
            responseJson.put("error", "Missing required fields");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Validate phone number format (10 digits)
        if (phone.length() != 10 || !phone.matches("\\d+")) {
            responseJson.put("error", "Invalid phone number");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }
        if (!altPhone.isEmpty() && (altPhone.length() != 10 || !altPhone.matches("\\d+"))) {
            responseJson.put("error", "Invalid alternate phone number");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false);
            try {
                // Retrieve JWT secret for token validation
                String jwtSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
                if (jwtSecretBase64 == null) {
                    logger.error("Missing JWT secret");
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

                // Validate JWT token and ensure it matches the provided email
                Jws<Claims> jws;
                try {
                    jws = Jwts.parserBuilder()
                            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret))
                            .build()
                            .parseClaimsJws(token);
                    String subject = jws.getBody().getSubject();
                    if (!email.equals(subject)) {
                        logger.warn("Token subject mismatch for email: {}", email);
                        responseJson.put("error", "Invalid token");
                        sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Invalid or expired token for email {}: {}", email, e.getMessage());
                    responseJson.put("error", "Invalid or expired token");
                    sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                    return;
                }

                // Verify token exists and is valid in database
                String tokenQuery = "SELECT expiry, status FROM email_verification_tokens WHERE email = ? AND token = ? AND expiry > NOW()";
                try (PreparedStatement pstmt = con.prepareStatement(tokenQuery)) {
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
                        if (!"verified".equals(status)) {
                            logger.info("Token not verified for email: {}", email);
                            responseJson.put("error", "Token not verified");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                    }
                }

                // Check if email already exists in customers table
                String checkQuery = "SELECT 1 FROM customers WHERE email = ? LIMIT 1";
                try (PreparedStatement pstmt = con.prepareStatement(checkQuery)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            logger.warn("Email already exists during registration: {}", email);
                            responseJson.put("error", "Email already in use");
                            sendResponse(response, HttpServletResponse.SC_CONFLICT, responseJson);
                            return;
                        }
                    }
                }

                // Hash password with BCrypt before storage
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                // Insert new customer record
                String insertQuery = "INSERT INTO customers (name, email, password, created_at, phone_no, alternate_phone, address) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = con.prepareStatement(insertQuery)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, email);
                    pstmt.setString(3, hashedPassword); // Store hashed password
                    pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    pstmt.setString(5, phone);
                    pstmt.setString(6, altPhone.isEmpty() ? null : altPhone);
                    pstmt.setString(7, null); // Address not provided
                    int rows = pstmt.executeUpdate();
                    if (rows == 0) throw new SQLException("Failed to insert customer");
                }

                // Mark token as used to prevent reuse
                String updateTokenQuery = "UPDATE email_verification_tokens SET status = 'used' WHERE email = ? AND token = ?";
                try (PreparedStatement pstmt = con.prepareStatement(updateTokenQuery)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    pstmt.executeUpdate();
                }

                con.commit();
                responseJson.put("message", "Registration successful");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error during registration for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            } catch (Exception e) {
                con.rollback();
                logger.error("Error during registration for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Internal server error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            }
        } catch (SQLException e) {
            logger.error("Connection error during registration: {}", e.getMessage(), e);
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}