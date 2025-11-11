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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;
import io.jsonwebtoken.Claims;

/**
 * Servlet for handling service deletion with associated media cleanup
 * Provides secure deletion of services and their Cloudinary media assets
 * 
 * Features:
 * - JWT-authenticated service deletion
 * - Transaction-safe database operations
 * - Cloudinary media cleanup integration
 * - Comprehensive ownership validation
 * 
 * @WebServlet Maps to "/deleteservice/*" endpoint with service ID path parameter
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/deleteservice/*")
public class DeleteService extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DeleteService.class);
    private static final Properties config = new Properties();

    // Static initialization block for loading configuration
    static {
        try {
            config.load(DeleteService.class.getClassLoader().getResourceAsStream("config.properties"));
            logger.info("Successfully loaded config.properties");
        } catch (IOException e) {
            logger.error("Failed to load config.properties: {}", e.getMessage());
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
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Handles DELETE requests for service deletion
     * Performs comprehensive cleanup including database records and Cloudinary media
     * 
     * @param request HttpServletRequest with JWT claims and service ID path parameter
     * @param response HttpServletResponse with deletion result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing DELETE request for deleting service");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Validate JWT authentication
        Claims claims = (Claims) request.getAttribute("claims");
        Integer providerId;
        try {
            providerId = Integer.valueOf(claims.getSubject());
            logger.debug("Authenticated provider ID: {}", providerId);
        } catch (NumberFormatException | NullPointerException e) {
            logger.error("Invalid or missing JWT token: {}", claims != null ? claims.getSubject() : "null");
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Extract serviceId from URL path parameter
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid service ID");
            return;
        }
        int serviceId;
        try {
            serviceId = Integer.parseInt(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid service ID");
            return;
        }

        // Load Cloudinary credentials for media cleanup
        String cloudName = ConfigUtil.get("cloudinary.cloud_name", "CLOUDINARY_CLOUD_NAME");
        String apiKey = ConfigUtil.get("cloudinary.api_key", "CLOUDINARY_API_KEY");
        String apiSecret = ConfigUtil.get("cloudinary.api_secret", "CLOUDINARY_API_SECRET");
        if (cloudName == null || apiKey == null || apiSecret == null) {
            logger.error("Missing Cloudinary credentials for provider ID: {}", providerId);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false); // Start transaction for atomic operations
            try {
                // Check if service exists and belongs to authenticated provider
                String checkServiceQuery = "SELECT service_id FROM services WHERE service_id = ? AND provider_id = ?";
                try (PreparedStatement checkPstmt = con.prepareStatement(checkServiceQuery)) {
                    checkPstmt.setInt(1, serviceId);
                    checkPstmt.setInt(2, providerId);
                    try (ResultSet rs = checkPstmt.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            logger.warn("Service ID: {} not found or does not belong to provider ID: {}", serviceId, providerId);
                            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service not found");
                            return;
                        }
                    }
                }

                // Fetch all media associated with the service for Cloudinary cleanup
                String mediaQuery = "SELECT media_id, media_url, media_type FROM media WHERE service_id = ? AND status = 'Active'";
                try (PreparedStatement mediaPstmt = con.prepareStatement(mediaQuery)) {
                    mediaPstmt.setInt(1, serviceId);
                    try (ResultSet mediaRs = mediaPstmt.executeQuery()) {
                        // Initialize Cloudinary client for media deletion
                        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                            "cloud_name", cloudName,
                            "api_key", apiKey,
                            "api_secret", apiSecret,
                            "secure", true,
                            "timeout", 10000
                        ));
                        while (mediaRs.next()) {
                            int mediaId = mediaRs.getInt("media_id");
                            String mediaUrl = mediaRs.getString("media_url");
                            String mediaType = mediaRs.getString("media_type");

                            // Delete from Cloudinary if URL exists
                            if (mediaUrl != null) {
                                // Extract public_id from Cloudinary URL for deletion
                                String publicId = mediaUrl.replaceFirst("https://res.cloudinary.com/" + cloudName + "/" + mediaType + "/upload/v\\d+/", "").replaceFirst("\\." + "[^\\.]+$", "");
                                logger.debug("Extracted public_id: {} for media_url: {}", publicId, mediaUrl);
                                try {
                                    Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", mediaType));
                                    logger.info("Deleted media from Cloudinary: media_id={}, public_id={}, type={}, result={}", mediaId, publicId, mediaType, result);
                                } catch (Exception e) {
                                    // Log warning but continue with database deletion
                                    logger.warn("Failed to delete media from Cloudinary for media ID: {}, public_id: {}. Continuing with database deletion.", mediaId, publicId, e);
                                }
                            }
                        }
                    }
                }

                // Delete all media records from database
                String deleteMediaQuery = "DELETE FROM media WHERE service_id = ?";
                try (PreparedStatement deleteMediaPstmt = con.prepareStatement(deleteMediaQuery)) {
                    deleteMediaPstmt.setInt(1, serviceId);
                    int mediaRowsAffected = deleteMediaPstmt.executeUpdate();
                    logger.debug("Deleted {} media records for service ID: {}", mediaRowsAffected, serviceId);
                }

                // Delete service record from database
                String deleteServiceQuery = "DELETE FROM services WHERE service_id = ? AND provider_id = ?";
                try (PreparedStatement deleteServicePstmt = con.prepareStatement(deleteServiceQuery)) {
                    deleteServicePstmt.setInt(1, serviceId);
                    deleteServicePstmt.setInt(2, providerId);
                    int serviceRowsAffected = deleteServicePstmt.executeUpdate();
                    if (serviceRowsAffected == 0) {
                        con.rollback();
                        logger.warn("Failed to delete service ID: {} for provider ID: {}", serviceId, providerId);
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service not found");
                        return;
                    }
                }

                // Commit transaction if all operations succeed
                con.commit();
                logger.info("Deleted service ID: {} and associated media for provider ID: {}", serviceId, providerId);

                // Send success response
                response.setStatus(HttpServletResponse.SC_OK);
                JSONObject responseJson = new JSONObject();
                responseJson.put("status", "success");
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(responseJson.toString());
                    writer.flush();
                }
            } catch (SQLException e) {
                // Rollback transaction on database error
                con.rollback();
                logger.error("Database error deleting service ID: {} for provider ID: {}", serviceId, providerId, e);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to connect to database for provider ID: {}", providerId, e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}