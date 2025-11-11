package shaadisharthi.admin;

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
 * Account Servlet - Admin Profile Management
 * 
 * Handles admin user profile operations including:
 * - Retrieving admin profile information (GET)
 * - Updating admin profile details (POST)
 * 
 * Profile fields managed:
 * - Name, phone number, email, address
 * - Email is read-only (primary identifier)
 * 
 * Security Features:
 * - JWT token validation for authentication
 * - Input validation and sanitization
 * - Comprehensive error handling and logging
 * 
 * Note: Email cannot be changed as it's the primary admin identifier
 */
@WebServlet("/account")
public class Account extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    /**
     * Standardized error response handler
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param message Error message for client
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
     * Validates profile update input parameters
     * 
     * Validation Rules:
     * - Name: Required, max 100 characters
     * - Phone: Required, exactly 10 digits
     * - Address: Required, max 255 characters
     * 
     * @param name Admin's full name
     * @param phone Admin's phone number
     * @param address Admin's address
     * @param response HttpServletResponse for error handling
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateInput(String name, String phone, String address, HttpServletResponse response)
            throws IOException {
        if (name == null || name.trim().isEmpty() || name.length() > 100) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing name");
            return false;
        }
        if (phone == null || !phone.matches("\\d{10}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid phone number (must be 10 digits)");
            return false;
        }
        if (address == null || address.trim().isEmpty() || address.length() > 255) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing address");
            return false;
        }
        return true;
    }

    /**
     * GET endpoint - Retrieves current admin's profile information
     * 
     * Returns:
     * - name, phone_no, email, address
     * - Email is read-only and serves as primary identifier
     * 
     * @param request HttpServletRequest with JWT token
     * @param response HttpServletResponse with JSON profile data
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for account");
        response.setContentType("application/json");

        // Extract and validate admin ID from JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        Integer adminId;
        try {
            adminId = Integer.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            logger.error("Invalid admin ID in token: {}", claims.getSubject());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
            return;
        }

        JSONObject json = new JSONObject();
        try (Connection con = DbConnection.getCon();
             PreparedStatement pstmt = con.prepareStatement("SELECT name, phone_no, email, address FROM admin WHERE admin_id = ?")) {
            pstmt.setInt(1, adminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Build profile response with all admin details
                    json.put("name", rs.getString("name"));
                    json.put("phone", rs.getString("phone_no"));
                    json.put("email", rs.getString("email")); // Read-only field
                    json.put("address", rs.getString("address"));
                } else {
                    logger.warn("Admin not found for ID: {}", adminId);
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Admin not found");
                    return;
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while fetching profile for admin ID {}: {}", adminId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return;
        }

        // Return profile data as JSON
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * POST endpoint - Updates admin profile information
     * 
     * Updateable fields:
     * - name, phone_no, address
     * - Email cannot be updated (primary identifier)
     * 
     * Process:
     * 1. Validate JWT token and extract admin ID
     * 2. Parse and validate request body
     * 3. Update database with new profile information
     * 4. Return updated profile data
     * 
     * @param request HttpServletRequest with JSON body containing update fields
     * @param response HttpServletResponse with update result
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for account update");
        response.setContentType("application/json");

        // Extract and validate admin ID from JWT
        Claims claims = (Claims) request.getAttribute("claims");
        Integer adminId;
        try {
            adminId = Integer.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            logger.error("Invalid admin ID in token: {}", claims.getSubject());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
            return;
        }

        JSONObject json = new JSONObject();
        try {
            // Read and parse request body
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject requestBody = new JSONObject(sb.toString());
            String name = requestBody.optString("name", null);
            String phone = requestBody.optString("phone", null);
            String address = requestBody.optString("address", null);

            // Validate input parameters
            if (!validateInput(name, phone, address, response)) {
                return;
            }

            // Update admin profile in database
            try (Connection con = DbConnection.getCon();
                 PreparedStatement pstmt = con.prepareStatement("UPDATE admin SET name = ?, phone_no = ?, address = ? WHERE admin_id = ?")) {
                pstmt.setString(1, name.trim());
                pstmt.setString(2, phone);
                pstmt.setString(3, address.trim());
                pstmt.setInt(4, adminId);

                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    // Return success response with updated data
                    json.put("success", true);
                    json.put("name", name);
                    json.put("phone", phone);
                    json.put("address", address);
                    response.setStatus(HttpServletResponse.SC_OK);
                    logger.info("Profile updated successfully for admin ID: {}", adminId);
                } else {
                    logger.warn("Profile update failed for admin ID: {}", adminId);
                    json.put("success", false);
                    json.put("error", "Update failed");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (SQLException e) {
                logger.error("Database error while updating profile for admin ID {}: {}", adminId, e.getMessage());
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }
        } catch (Exception e) {
            logger.error("Error processing POST request: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        // Return update result
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }
}