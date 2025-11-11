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
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * Change Password Servlet - Admin Password Security Management
 * 
 * Handles secure password changes for admin users with:
 * - Current password verification
 * - New password validation and hashing
 * - BCrypt secure password storage
 * 
 * Security Features:
 * - Current password must be verified before change
 * - New password must meet complexity requirements
 * - Prevents setting same password as current
 * - Uses BCrypt with salt for secure storage
 * 
 * Password Requirements:
 * - Minimum 8 characters
 * - Must be different from current password
 */
@WebServlet("/changepassword")
public class ChangePasswordServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ChangePasswordServlet.class);

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
     * Validates password change input parameters
     * 
     * Validation Rules:
     * - Current password: Required, non-empty
     * - New password: Required, minimum 8 characters
     * - New password must be different from current password
     * 
     * @param currentPassword User's current password for verification
     * @param newPassword New password to set
     * @param response HttpServletResponse for error handling
     * @return true if inputs are valid, false otherwise
     */
    private boolean validateInput(String currentPassword, String newPassword, HttpServletResponse response)
            throws IOException {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Current password is required");
            return false;
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "New password is required");
            return false;
        }
        if (newPassword.length() < 8) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "New password must be at least 8 characters");
            return false;
        }
        if (currentPassword.equals(newPassword)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "New password must be different from current password");
            return false;
        }
        return true;
    }

    /**
     * POST endpoint - Handles admin password change requests
     * 
     * Process Flow:
     * 1. Validate JWT token and extract admin ID
     * 2. Parse and validate request parameters
     * 3. Verify current password matches stored hash
     * 4. Hash new password with BCrypt
     * 5. Update database with new password hash
     * 
     * Security: Uses BCrypt with automatic salt generation for secure storage
     * 
     * @param request HttpServletRequest with currentPassword and newPassword
     * @param response HttpServletResponse with operation result
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for password change");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract and validate admin ID from JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        String adminId;
        try {
            adminId = claims.getSubject();
            logger.debug("Authenticated admin ID: {}", adminId);
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null");
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

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
            String currentPassword = requestBody.optString("currentPassword", null);
            String newPassword = requestBody.optString("newPassword", null);

            // Validate input parameters
            if (!validateInput(currentPassword, newPassword, response)) {
                return;
            }

            // Verify current password matches stored hash
            String storedPassword = getStoredPassword(adminId);
            if (storedPassword == null || !BCrypt.checkpw(currentPassword, storedPassword)) {
                logger.warn("Incorrect current password for admin ID: {}", adminId);
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Incorrect current password");
                return;
            }

            // Hash new password and update database
            String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            updatePassword(adminId, hashedNewPassword);

            // Return success response
            logger.info("Password changed successfully for admin ID: {}", adminId);
            response.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(new JSONObject().put("message", "Password changed successfully").toString());
                writer.flush();
            }
        } catch (Exception e) {
            logger.error("Error changing password for admin ID {}: {}", adminId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Retrieves stored password hash for admin user
     * 
     * @param adminId Admin identifier
     * @return BCrypt hashed password or null if admin not found
     * @throws SQLException if database error occurs
     */
    private String getStoredPassword(String adminId) throws SQLException {
        try (Connection conn = DbConnection.getCon();
             PreparedStatement ps = conn.prepareStatement("SELECT password FROM admin WHERE admin_id = ?")) {
            ps.setString(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
            }
        }
        logger.warn("No password found for admin ID: {}", adminId);
        return null;
    }

    /**
     * Updates admin password with new hashed value
     * 
     * @param adminId Admin identifier
     * @param hashedPassword New BCrypt hashed password
     * @throws SQLException if database update fails
     */
    private void updatePassword(String adminId, String hashedPassword) throws SQLException {
        try (Connection conn = DbConnection.getCon();
             PreparedStatement ps = conn.prepareStatement("UPDATE admin SET password = ? WHERE admin_id = ?")) {
            ps.setString(1, hashedPassword);
            ps.setString(2, adminId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                logger.warn("No admin found for ID: {}", adminId);
            }
        }
    }
}