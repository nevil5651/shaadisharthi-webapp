package shaadisharthi;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;

/**
 * Servlet for handling service provider registration
 * Provides secure user registration with email verification requirement
 * 
 * Features:
 * - Email verification token validation
 * - BCrypt password hashing
 * - Duplicate email prevention
 * - Transaction-safe registration process
 * 
 * @WebServlet Maps to "/register" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/register") // Adjusted to match deployment context
public class Register extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Register.class);

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
     * Handles POST requests for service provider registration
     * Validates email verification and creates new provider accounts
     * 
     * @param request HttpServletRequest with JSON registration data
     * @param response HttpServletResponse with registration result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /ShaadiSharthi/api/register");
        JSONObject responseJson = new JSONObject();
        String name = null;
        String email = null;
        String password = null;

        // Read and parse JSON payload from request
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
            JSONObject jsonPayload = new JSONObject(payload.toString());
            name = jsonPayload.getString("name");
            email = jsonPayload.getString("email");
            password = jsonPayload.getString("password");
        } catch (IOException e) {
            logger.error("Failed to read request payload: {}", e.getMessage(), e);
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        } catch (Exception e) {
            logger.warn("Invalid JSON payload: {}", payload);
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Validate required fields
        if (name == null || email == null || password == null || name.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            logger.warn("Missing or empty registration data: name={}, email={}, password={}", name, email, password);
            responseJson.put("error", "All fields are required");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false);
            try {
                // Verify email token exists and is verified
                String verifyQuery = "SELECT 1 FROM email_verification_tokens WHERE email = ? AND status = 'verified' LIMIT 1";
                try (PreparedStatement pstmt = con.prepareStatement(verifyQuery)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            logger.info("Email not verified or token invalid for email: {}", email);
                            responseJson.put("error", "Email not verified. Please verify your email first.");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                    }
                }

                // Check if email already registered to prevent duplicates
                String checkQuery = "SELECT 1 FROM service_providers WHERE email = ? LIMIT 1";
                try (PreparedStatement pstmt = con.prepareStatement(checkQuery)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            logger.info("Email already registered: {}", email);
                            responseJson.put("error", "Email already in use");
                            sendResponse(response, HttpServletResponse.SC_CONFLICT, responseJson);
                            return;
                        }
                    }
                }

                // Hash password for secure storage
                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                // Insert new service provider with basic_registered status
                String insertQuery = "INSERT INTO service_providers (name, email, password, status, created_at) VALUES (?, ?, ?, 'basic_registered', NOW())";
                try (PreparedStatement pstmt = con.prepareStatement(insertQuery)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, email);
                    pstmt.setString(3, hashedPassword);
                    int rows = pstmt.executeUpdate();
                    if (rows == 0) {
                        logger.error("Failed to register user: {}", email);
                        responseJson.put("error", "Registration failed");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                }

                // Mark verification token as used to prevent reuse
                String updateQuery = "UPDATE email_verification_tokens SET status = 'used' WHERE email = ?";
                try (PreparedStatement pstmt = con.prepareStatement(updateQuery)) {
                    pstmt.setString(1, email);
                    pstmt.executeUpdate();
                }

                // Commit transaction on success
                con.commit();
                logger.info("Registered user: {}", email);
                responseJson.put("message", "Registration successful");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                // Rollback transaction on error
                con.rollback();
                logger.error("Database error for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            }
        } catch (SQLException e) {
            logger.error("Connection error for email {}: {}", email, e.getMessage(), e);
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}