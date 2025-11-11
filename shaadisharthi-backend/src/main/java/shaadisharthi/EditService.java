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
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;
import io.jsonwebtoken.Claims;

/**
 * Servlet for comprehensive service management including updates and media operations
 * Handles service modifications, media additions, and media deletions with Cloudinary integration
 * 
 * Features:
 * - Dynamic service field updates with validation
 * - Media management with Cloudinary integration
 * - Transaction-safe database operations
 * - Comprehensive input validation and error handling
 * 
 * @WebServlet Maps to "/editservice/*" endpoint with flexible path patterns
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/editservice/*")
public class EditService extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(EditService.class);
    private static final Properties config = new Properties();

    // Static initialization for configuration loading
    static {
        try {
            config.load(EditService.class.getClassLoader().getResourceAsStream("config.properties"));
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
     * Fetches complete service information including media assets
     * 
     * @param con Database connection
     * @param serviceId Service identifier
     * @param providerId Provider identifier for ownership validation
     * @return JSONObject containing service details and media array
     * @throws SQLException If database query fails
     */
    private JSONObject fetchService(Connection con, int serviceId, int providerId) throws SQLException {
        String query = "SELECT service_id, provider_id, service_name, description, price, category FROM services WHERE service_id = ? AND provider_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, serviceId);
            pstmt.setInt(2, providerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JSONObject service = new JSONObject();
                    service.put("id", rs.getInt("service_id"));
                    service.put("providerId", rs.getInt("provider_id"));
                    service.put("name", rs.getString("service_name"));
                    service.put("description", rs.getString("description"));
                    service.put("price", rs.getDouble("price"));
                    service.put("category", rs.getString("category"));

                    // Fetch associated media assets
                    JSONArray mediaArray = new JSONArray();
                    String mediaQuery = "SELECT media_id, media_type, media_url FROM media WHERE service_id = ? AND status = 'Active'";
                    try (PreparedStatement mediaPstmt = con.prepareStatement(mediaQuery)) {
                        mediaPstmt.setInt(1, serviceId);
                        try (ResultSet mediaRs = mediaPstmt.executeQuery()) {
                            while (mediaRs.next()) {
                                JSONObject media = new JSONObject();
                                media.put("id", mediaRs.getInt("media_id"));
                                media.put("type", mediaRs.getString("media_type"));
                                media.put("url", mediaRs.getString("media_url"));
                                mediaArray.put(media);
                            }
                        }
                    }
                    service.put("media", mediaArray);
                    return service;
                }
            }
        }
        return null;
    }

    /**
     * Validates service input parameters for updates
     * 
     * @param name Service name
     * @param category Service category
     * @param price Service price
     * @param description Service description
     * @param response HttpServletResponse for error handling
     * @return true if all validations pass, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateServiceInput(String name, String category, Double price, String description, HttpServletResponse response) throws IOException {
        // Validate service name: optional but must be valid if provided
        if (name != null && (name.trim().isEmpty() || name.length() > 100)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid service name (max 100 characters)");
            return false;
        }
        // Validate category: optional but must be valid if provided
        if (category != null && (category.trim().isEmpty() || category.length() > 50)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid category (max 50 characters)");
            return false;
        }
        // Validate price: must be non-negative if provided
        if (price != null && price < 0) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid price (must be non-negative)");
            return false;
        }
        // Validate description: optional but must be valid if provided
        if (description != null && (description.trim().isEmpty() || description.length() > 500)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid description (max 500 characters)");
            return false;
        }
        return true;
    }

    /**
     * Validates media input parameters for uploads
     * 
     * @param media JSON object containing media details
     * @param response HttpServletResponse for error handling
     * @return true if media validation passes, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateMediaInput(JSONObject media, HttpServletResponse response) throws IOException {
        // Check for required media fields
        if (!media.has("url") || !media.has("type") || !media.has("fileSize") || !media.has("fileExtension")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid media format: missing required fields");
            return false;
        }
        
        String url = media.getString("url");
        String type = media.getString("type");
        long fileSize;
        try {
            fileSize = media.getLong("fileSize");
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid fileSize");
            return false;
        }
        String fileExtension = media.getString("fileExtension");
        
        // Validate media type and URL format
        if (!type.matches("image|video") || !url.startsWith("https://res.cloudinary.com")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid media type or URL");
            return false;
        }
        // Validate file size
        if (fileSize < 0) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid fileSize: must be non-negative");
            return false;
        }
        // Validate file extension
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid fileExtension: must be non-empty");
            return false;
        }
        return true;
    }

    /**
     * Handles PUT requests for updating service information
     * Supports partial updates with dynamic query building
     * 
     * @param request HttpServletRequest with JSON payload containing update fields
     * @param response HttpServletResponse with updated service data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing PUT request for updating service");
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

        // Read and parse JSON request body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject requestBody;
        try {
            requestBody = new JSONObject(sb.toString());
        } catch (Exception e) {
            logger.warn("Invalid JSON request body for provider ID: {}", providerId);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        // Extract and validate input parameters (all optional for partial updates)
        String name = requestBody.has("name") ? requestBody.optString("name", null) : null;
        String category = requestBody.has("category") ? requestBody.optString("category", null) : null;
        Double price = requestBody.has("price") ? requestBody.optDouble("price", -1) : null;
        String description = requestBody.has("description") ? requestBody.optString("description", null) : null;

        if (!validateServiceInput(name, category, price, description, response)) {
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false); // Start transaction
            try {
                // Verify service exists and belongs to provider
                JSONObject existingService = fetchService(con, serviceId, providerId);
                if (existingService == null) {
                    con.rollback();
                    logger.warn("Service ID: {} not found or does not belong to provider ID: {}", serviceId, providerId);
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service not found");
                    return;
                }

                // Build dynamic UPDATE query based on provided fields
                StringBuilder query = new StringBuilder("UPDATE services SET ");
                boolean hasUpdates = false;
                if (name != null) {
                    query.append("service_name = ?, ");
                    hasUpdates = true;
                }
                if (description != null) {
                    query.append("description = ?, ");
                    hasUpdates = true;
                }
                if (price != null) {
                    query.append("price = ?, ");
                    hasUpdates = true;
                }
                if (category != null) {
                    query.append("category = ?, ");
                    hasUpdates = true;
                }
                
                // Check if any fields were provided for update
                if (!hasUpdates) {
                    con.rollback();
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "No fields provided to update");
                    return;
                }
                
                query.setLength(query.length() - 2); // Remove trailing comma and space
                query.append(" WHERE service_id = ? AND provider_id = ?");

                // Execute dynamic update
                try (PreparedStatement pstmt = con.prepareStatement(query.toString())) {
                    int paramIndex = 1;
                    if (name != null) pstmt.setString(paramIndex++, name.trim());
                    if (description != null) pstmt.setString(paramIndex++, description.trim());
                    if (price != null) pstmt.setDouble(paramIndex++, price);
                    if (category != null) pstmt.setString(paramIndex++, category.trim());
                    pstmt.setInt(paramIndex++, serviceId);
                    pstmt.setInt(paramIndex, providerId);
                    
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected == 0) {
                        con.rollback();
                        logger.warn("Failed to update service ID: {} for provider ID: {}", serviceId, providerId);
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service not found");
                        return;
                    }
                }

                // Fetch updated service information
                JSONObject updatedService = fetchService(con, serviceId, providerId);
                con.commit();

                logger.info("Updated service ID: {} for provider ID: {}", serviceId, providerId);

                // Send success response with updated service data
                response.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(updatedService.toString());
                    writer.flush();
                }
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error updating service ID: {} for provider ID: {}", serviceId, providerId, e);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to connect to database for provider ID: {}", providerId, e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handles POST requests for adding media to existing services
     * 
     * @param request HttpServletRequest with JSON media details
     * @param response HttpServletResponse with created media data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for adding media to service");
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

        // Extract serviceId from URL path pattern: /{serviceId}/media
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+/media")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid service ID or path");
            return;
        }
        int serviceId;
        try {
            serviceId = Integer.parseInt(pathInfo.split("/")[1]);
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid service ID");
            return;
        }

        // Read and parse JSON request body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject requestBody;
        try {
            requestBody = new JSONObject(sb.toString());
        } catch (Exception e) {
            logger.warn("Invalid JSON request body for provider ID: {}", providerId);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        // Validate media input parameters
        if (!validateMediaInput(requestBody, response)) {
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false);
            try {
                // Verify service exists and belongs to provider
                JSONObject existingService = fetchService(con, serviceId, providerId);
                if (existingService == null) {
                    con.rollback();
                    logger.warn("Service ID: {} not found or does not belong to provider ID: {}", serviceId, providerId);
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service not found");
                    return;
                }

                // Insert new media record
                String mediaQuery = "INSERT INTO media (service_id, media_type, media_url, file_size, file_extension, status) VALUES (?, ?, ?, ?, ?, 'Active')";
                int mediaId;
                try (PreparedStatement mediaPstmt = con.prepareStatement(mediaQuery, Statement.RETURN_GENERATED_KEYS)) {
                    mediaPstmt.setInt(1, serviceId);
                    mediaPstmt.setString(2, requestBody.getString("type"));
                    mediaPstmt.setString(3, requestBody.getString("url"));
                    mediaPstmt.setLong(4, requestBody.getLong("fileSize"));
                    mediaPstmt.setString(5, requestBody.getString("fileExtension"));
                    mediaPstmt.executeUpdate();

                    // Retrieve generated media ID
                    try (ResultSet mediaKeys = mediaPstmt.getGeneratedKeys()) {
                        if (mediaKeys.next()) {
                            mediaId = mediaKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated media ID");
                        }
                    }
                }

                // Prepare response with new media details
                JSONObject mediaResponse = new JSONObject();
                mediaResponse.put("id", mediaId);
                mediaResponse.put("url", requestBody.getString("url"));
                mediaResponse.put("type", requestBody.getString("type"));
                mediaResponse.put("fileSize", requestBody.getLong("fileSize"));
                mediaResponse.put("fileExtension", requestBody.getString("fileExtension"));

                con.commit();

                logger.info("Added media ID: {} to service ID: {} for provider ID: {}", mediaId, serviceId, providerId);

                // Send success response with created media data
                response.setStatus(HttpServletResponse.SC_CREATED);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(mediaResponse.toString());
                    writer.flush();
                }
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error adding media to service ID: {} for provider ID: {}", serviceId, providerId, e);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to connect to database for provider ID: {}", providerId, e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handles DELETE requests for removing media from services
     * Performs cleanup in both database and Cloudinary
     * 
     * @param request HttpServletRequest with service and media ID in path
     * @param response HttpServletResponse with updated service data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing DELETE request for removing media from service");
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

        // Extract serviceId and mediaId from URL path pattern: /{serviceId}/media/{mediaId}
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+/media/\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid service ID or media ID");
            return;
        }
        int serviceId, mediaId;
        try {
            String[] parts = pathInfo.split("/");
            serviceId = Integer.parseInt(parts[1]);
            mediaId = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid service ID or media ID");
            return;
        }

        // Load Cloudinary credentials for media deletion
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
            con.setAutoCommit(false);
            try {
                // Verify service exists and belongs to provider
                JSONObject existingService = fetchService(con, serviceId, providerId);
                if (existingService == null) {
                    con.rollback();
                    logger.warn("Service ID: {} not found or does not belong to provider ID: {}", serviceId, providerId);
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service not found");
                    return;
                }

                // Fetch media details for Cloudinary deletion
                String mediaQuery = "SELECT media_url, media_type FROM media WHERE media_id = ? AND service_id = ?";
                String mediaUrl = null;
                String mediaType = null;
                try (PreparedStatement mediaPstmt = con.prepareStatement(mediaQuery)) {
                    mediaPstmt.setInt(1, mediaId);
                    mediaPstmt.setInt(2, serviceId);
                    try (ResultSet mediaRs = mediaPstmt.executeQuery()) {
                        if (mediaRs.next()) {
                            mediaUrl = mediaRs.getString("media_url");
                            mediaType = mediaRs.getString("media_type");
                        } else {
                            con.rollback();
                            logger.warn("Media ID: {} not found for service ID: {}", mediaId, serviceId);
                            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Media not found");
                            return;
                        }
                    }
                }

                // Delete from Cloudinary if URL exists
                if (mediaUrl != null) {
                    String publicId = mediaUrl.replaceFirst("https://res.cloudinary.com/" + cloudName + "/" + mediaType + "/upload/v\\d+/", "").replaceFirst("\\." + "[^\\.]+$", "");
                    logger.debug("Extracted public_id: {} for media_url: {}", publicId, mediaUrl);
                    Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true,
                        "timeout", 10000
                    ));
                    try {
                        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", mediaType));
                        logger.info("Deleted media from Cloudinary: public_id={}, type={}, result={}", publicId, mediaType, result);
                    } catch (Exception e) {
                        logger.warn("Failed to delete media from Cloudinary for media ID: {}, public_id: {}. Continuing with database deletion.", mediaId, publicId, e);
                    }
                }

                // Delete media record from database
                String deleteQuery = "DELETE FROM media WHERE media_id = ? AND service_id = ?";
                try (PreparedStatement deletePstmt = con.prepareStatement(deleteQuery)) {
                    deletePstmt.setInt(1, mediaId);
                    deletePstmt.setInt(2, serviceId);
                    int rowsAffected = deletePstmt.executeUpdate();
                    if (rowsAffected == 0) {
                        con.rollback();
                        logger.warn("Media ID: {} not found for service ID: {}", mediaId, serviceId);
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Media not found");
                        return;
                    }
                }

                // Fetch updated service information
                JSONObject updatedService = fetchService(con, serviceId, providerId);
                con.commit();

                logger.info("Deleted media ID: {} from service ID: {} for provider ID: {}", mediaId, serviceId, providerId);

                // Send success response with updated service data
                response.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(updatedService.toString());
                    writer.flush();
                }
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error deleting media ID: {} from service ID: {} for provider ID: {}", mediaId, serviceId, providerId, e);
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