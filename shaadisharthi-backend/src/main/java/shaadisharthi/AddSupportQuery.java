package shaadisharthi;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import shaadisharthi.DbConnection.DbConnection;

/**
 * Servlet for submitting customer and service provider support queries
 * Handles creation of new support tickets in the system
 * 
 * Features:
 * - JWT-based user authentication and role identification
 * - Support for both Customer and ServiceProvider user types
 * - Input validation and error handling
 * - IST timestamp logging for audit trails
 * 
 * @WebServlet Maps to "/AddSupportQuery" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/AddSupportQuery")
public class AddSupportQuery extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AddSupportQuery.class);

    /**
     * Handles POST requests to create new support queries
     * Authenticates user via JWT and creates support ticket
     * 
     * @param request HttpServletRequest with JSON payload containing query details
     * @param response HttpServletResponse with operation result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Log request with IST timestamp for audit purposes
        logger.debug("Processing POST request for adding support query at {} IST", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Get JWT claims from request attribute (assumed set by a filter)
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing token");
            return;
        }

        // Extract user identity from JWT token
        String userIdStr = claims.getSubject(); // Assuming 'sub' claim holds user_id
        String role = claims.get("role", String.class); // Get role from token

        if (userIdStr == null || role == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing user details in token");
            return;
        }

        // Transform role to user_type format for database storage
        String userType;
        if ("SERVICE_PROVIDER".equals(role)) {
            userType = "ServiceProvider";
        } else {
            userType = "Customer"; // Default to Customer for other roles
        }

        // Parse request body (assuming JSON payload)
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            logger.error("Failed to read request body at {} IST: {}", LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        JSONObject jsonPayload;
        try {
            jsonPayload = new JSONObject(sb.toString());
        } catch (Exception e) {
            logger.error("Invalid JSON payload at {} IST: {}", LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format");
            return;
        }

        // Extract and validate query content
        String subject = jsonPayload.optString("subject", "").trim();
        String message = jsonPayload.optString("message", "").trim();

        // Input validation - ensure required fields are present
        if (subject.isEmpty() || message.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Subject and message cannot be empty");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            logger.error("Invalid user ID from token at {} IST: {}", LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid user ID in token");
            return;
        }

        // SQL Insert statement for creating new support query
        String insertQuerySQL = "INSERT INTO support_queries (user_id, user_type, subject, message, timestamp, query_status, escalated) " +
                               "VALUES (?, ?, ?, ?, NOW(), 'Pending', 0)";

        try (Connection con = DbConnection.getCon();
             PreparedStatement ps = con.prepareStatement(insertQuerySQL)) {

            // Set parameters for prepared statement
            ps.setInt(1, userId);
            ps.setString(2, userType);
            ps.setString(3, subject);
            ps.setString(4, message);

            int result = ps.executeUpdate();

            if (result > 0) {
                // Success response
                logger.info("Query submitted successfully for user ID: {} (type: {}) at {} IST", userId, userType, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                JSONObject responseJson = new JSONObject();
                responseJson.put("success", true);
                responseJson.put("message", "Query submitted successfully!");
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(responseJson.toString());
                    writer.flush();
                }
            } else {
                logger.warn("Failed to submit query for user ID: {} at {} IST", userId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit query");
            }

        } catch (SQLException e) {
            logger.error("Database error submitting query for user ID: {} at {} IST: {}", userId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage().replace("'", ""));
        }
    }

    /**
     * Sends standardized error responses in JSON format
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param message Error description
     * @throws IOException If response writing fails
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        json.put("success", false);
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }
}