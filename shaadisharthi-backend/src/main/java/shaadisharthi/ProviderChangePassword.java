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
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * Servlet for handling service provider password changes
 * Provides secure password updates with BCrypt hashing and validation
 * 
 * Features:
 * - JWT-authenticated password changes
 * - BCrypt password hashing for security
 * - Comprehensive input validation
 * - Current password verification
 * 
 * @WebServlet Maps to "/provider-change-password" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/provider-change-password")
public class ProviderChangePassword extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ProviderChangePassword.class);

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
     * @param currentPassword Current password for verification
     * @param newPassword New password to set
     * @param response HttpServletResponse for error handling
     * @return true if validation passes, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateInput(String currentPassword, String newPassword, HttpServletResponse response)
            throws IOException {
        // Validate current password presence
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Current password is required");
            return false;
        }
        // Validate new password presence
        if (newPassword == null || newPassword.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "New password is required");
            return false;
        }
        // Validate new password minimum length
        if (newPassword.length() < 8) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "New password must be at least 8 characters");
            return false;
        }
        // Prevent setting same password
        if (currentPassword.equals(newPassword)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "New password must be different from current password");
            return false;
        }
        return true;
    }

    /**
     * Handles POST requests for password changes
     * Verifies current password and updates to new hashed password
     * 
     * @param request HttpServletRequest with JSON containing current and new passwords
     * @param response HttpServletResponse with operation result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for service provider password change");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract provider ID from JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        String providerId;
        try {
            providerId = claims.getSubject();
            logger.debug("Authenticated service provider ID: {}", providerId);
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null");
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

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
            String currentPassword = requestBody.optString("currentPassword", null);
            String newPassword = requestBody.optString("newPassword", null);

            // Validate input parameters
            if (!validateInput(currentPassword, newPassword, response)) {
                return;
            }

            // Retrieve stored hashed password for verification
            String storedPassword = getStoredPassword(providerId);
            if (storedPassword == null || !BCrypt.checkpw(currentPassword, storedPassword)) {
                logger.warn("Incorrect current password for service provider ID: {}", providerId);
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Incorrect current password");
                return;
            }

            // Hash new password and update in database
            String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            updatePassword(providerId, hashedNewPassword);

            logger.info("Password changed successfully for service provider ID: {}", providerId);
            response.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(new JSONObject().put("message", "Password changed successfully").toString());
                writer.flush();
            }
        } catch (Exception e) {
            logger.error("Error changing password for service provider ID {}: {}", providerId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Retrieves the stored hashed password for a service provider
     * 
     * @param providerId Service provider identifier
     * @return Hashed password string, or null if not found
     * @throws SQLException If database query fails
     */
    private String getStoredPassword(String providerId) throws SQLException {
        try (Connection conn = DbConnection.getCon();
             PreparedStatement ps = conn.prepareStatement("SELECT password FROM service_providers WHERE provider_id = ?")) {
            ps.setString(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
            }
        }
        logger.warn("No password found for service provider ID: {}", providerId);
        return null;
    }

    /**
     * Updates the password for a service provider
     * 
     * @param providerId Service provider identifier
     * @param hashedPassword New hashed password
     * @throws SQLException If database update fails
     */
    private void updatePassword(String providerId, String hashedPassword) throws SQLException {
        try (Connection conn = DbConnection.getCon();
             PreparedStatement ps = conn.prepareStatement("UPDATE service_providers SET password = ? WHERE provider_id = ?")) {
            ps.setString(1, hashedPassword);
            ps.setString(2, providerId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                logger.warn("No service provider found for ID: {}", providerId);
            }
        }
    }
}