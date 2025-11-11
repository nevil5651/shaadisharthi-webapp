package shaadisharthi;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * Servlet for managing service provider account operations
 * Handles both retrieval and update of service provider profile information
 * 
 * Features:
 * - JWT-authenticated profile retrieval (GET)
 * - Profile updates with comprehensive validation (POST)
 * - Input sanitization and error handling
 * 
 * Security: Requires valid JWT token with provider ID in subject claim
 * 
 * @author ShaadiSharthi Team
 * @version 1.0
 */
//@WebServlet("/account")
public class Account extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    /**
     * Sends standardized error responses with JSON format
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param message Error message description
     * @throws IOException If response writing fails
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        JSONObject json = new JSONObject();
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Validates all input parameters for profile updates
     * 
     * @param name Provider's full name
     * @param state Operating state
     * @param phone Primary phone number
     * @param altPhone Alternate phone number (optional)
     * @param businessName Business/organization name
     * @param address Physical business address
     * @param response HttpServletResponse for error handling
     * @return true if all validations pass, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateInput(String name, String state, String phone, String altPhone, String businessName, String address, HttpServletResponse response)
            throws IOException {
        // Validate name: required, non-empty, max 100 characters
        if (name == null || name.trim().isEmpty() || name.length() > 100) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing name");
            return false;
        }
        // Validate state: required, non-empty, max 100 characters
        if (state == null || state.trim().isEmpty() || state.length() > 100) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing state");
            return false;
        }
        // Validate primary phone: required, exactly 10 digits
        if (phone == null || !phone.matches("\\d{10}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid phone number (must be 10 digits)");
            return false;
        }
        // Validate alternate phone: optional but must be 10 digits if provided
        if (altPhone != null && !altPhone.isEmpty() && !altPhone.matches("\\d{10}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid alternate phone number (must be 10 digits)");
            return false;
        }
        // Validate business name: required, non-empty, max 100 characters
        if (businessName == null || businessName.trim().isEmpty() || businessName.length() > 100) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing business name");
            return false;
        }
        // Validate address: required, non-empty, max 255 characters
        if (address == null || address.trim().isEmpty() || address.length() > 255) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing address");
            return false;
        }
        return true;
    }

    /**
     * Handles GET requests to retrieve service provider profile information
     * Requires valid JWT token with provider ID in subject claim
     * 
     * @param request HttpServletRequest with JWT claims attribute
     * @param response HttpServletResponse with JSON profile data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for ServiceProvider account");
        response.setContentType("application/json");

        // Extract JWT claims from request attribute (set by authentication filter)
        Claims claims = (Claims) request.getAttribute("claims");
        Integer providerId;
        try {
            // Parse provider ID from JWT subject claim
            providerId = Integer.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            logger.error("Invalid provider ID in token: {}", claims.getSubject());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
            return;
        }

        JSONObject json = new JSONObject();
        try (Connection con = DbConnection.getCon();
             // Query to retrieve complete provider profile
             PreparedStatement pstmt = con.prepareStatement("SELECT provider_id, name, state, phone_no, alternate_phone, email, business_name, address FROM service_providers WHERE provider_id = ?")) {
            pstmt.setInt(1, providerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Build JSON response with profile data
                    json.put("provider_id", rs.getString("provider_id"));
                    json.put("name", rs.getString("name"));
                    json.put("state", rs.getString("state"));
                    json.put("phone", rs.getString("phone_no"));
                    json.put("alternate_phone", rs.getString("alternate_phone"));
                    json.put("email", rs.getString("email"));
                    json.put("business_name", rs.getString("business_name"));
                    json.put("address", rs.getString("address"));
                    logger.info("Profile fetched successfully for provider ID: {}", providerId);
                } else {
                    logger.warn("Service provider not found for ID: {}", providerId);
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service provider not found");
                    return;
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while fetching profile for provider ID {}: {}", providerId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return;
        }

        // Send successful response
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Handles POST requests to update service provider profile information
     * Validates all inputs and updates database record
     * 
     * @param request HttpServletRequest with JSON payload containing update fields
     * @param response HttpServletResponse with update result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for ServiceProvider account update");
        response.setContentType("application/json");

        // Extract provider ID from JWT token
        Claims claims = (Claims) request.getAttribute("claims");
        Integer providerId;
        try {
            providerId = Integer.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            logger.error("Invalid provider ID in token: {}", claims.getSubject());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
            return;
        }

        JSONObject json = new JSONObject();
        try {
            // Read and parse JSON request body
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject requestBody = new JSONObject(sb.toString());
            // Extract fields from request with null defaults
            String name = requestBody.optString("name", null);
            String state = requestBody.optString("state", null);
            String phone = requestBody.optString("phone", null);
            String altPhone = requestBody.optString("alternate_phone", null);
            String businessName = requestBody.optString("business_name", null);
            String address = requestBody.optString("address", null);

            // Validate all input parameters
            if (!validateInput(name, state, phone, altPhone, businessName, address, response)) {
                return;
            }

            // Update provider profile in database
            try (Connection con = DbConnection.getCon();
                 PreparedStatement pstmt = con.prepareStatement("UPDATE service_providers SET name = ?, state = ?, phone_no = ?, alternate_phone = ?, business_name = ?, address = ? WHERE provider_id = ?")) {
                pstmt.setString(1, name.trim());
                pstmt.setString(2, state.trim());
                pstmt.setString(3, phone);
                // Handle optional alternate phone (can be null)
                if (altPhone == null || altPhone.trim().isEmpty()) {
                    pstmt.setNull(4, java.sql.Types.VARCHAR);
                } else {
                    pstmt.setString(4, altPhone);
                }
                pstmt.setString(5, businessName.trim());
                pstmt.setString(6, address.trim());
                pstmt.setInt(7, providerId);

                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    // Build success response with updated data
                    json.put("success", true);
                    json.put("name", name);
                    json.put("state", state);
                    json.put("phone", phone);
                    json.put("alternate_phone", altPhone);
                    json.put("business_name", businessName);
                    json.put("address", address);
                    response.setStatus(HttpServletResponse.SC_OK);
                    logger.info("Profile updated successfully for provider ID: {}", providerId);
                } else {
                    logger.warn("Profile update failed for provider ID: {}", providerId);
                    json.put("success", false);
                    json.put("error", "Update failed");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (SQLException e) {
                logger.error("Database error while updating profile for provider ID {}: {}", providerId, e.getMessage());
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }
        } catch (Exception e) {
            logger.error("Error processing POST request: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        // Send response
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }
}