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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * Servlet for creating new services with associated media assets
 * Handles comprehensive service creation with validation and media management
 * 
 * Features:
 * - JWT-authenticated service creation
 * - Comprehensive input validation
 * - Media asset association
 * - Transaction-safe database operations
 * 
 * @WebServlet Maps to "/createservice" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/createservice")
public class ServiceCreate extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCreate.class);

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
     * Validates all input parameters for service creation
     * 
     * @param name Service name
     * @param category Service category
     * @param price Service price
     * @param description Service description
     * @param mediaArray Array of media objects
     * @param response HttpServletResponse for error handling
     * @return true if all validations pass, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateInput(String name, String category, Double price, String description, JSONArray mediaArray, HttpServletResponse response) throws IOException {
        // Validate service name: required, non-empty, max 100 characters
        if (name == null || name.trim().isEmpty() || name.length() > 100) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing service name (max 100 characters)");
            return false;
        }
        // Validate category: required, non-empty, max 50 characters
        if (category == null || category.trim().isEmpty() || category.length() > 50) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing category (max 50 characters)");
            return false;
        }
        // Validate price: required, non-negative
        if (price == null || price < 0) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing price (must be non-negative)");
            return false;
        }
        // Validate description: required, non-empty, max 500 characters
        if (description == null || description.trim().isEmpty() || description.length() > 500) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing description (max 500 characters)");
            return false;
        }
        // Validate media array if provided
        if (mediaArray != null) {
            for (int i = 0; i < mediaArray.length(); i++) {
                JSONObject media = mediaArray.optJSONObject(i);
                // Check required media fields
                if (media == null || !media.has("url") || !media.has("type") || !media.has("fileSize") || !media.has("fileExtension")) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid media format at index " + i + ": missing required fields");
                    return false;
                }
                String url = media.getString("url");
                String type = media.getString("type");
                double fileSize;
                try {
                    fileSize = media.getDouble("fileSize");
                } catch (Exception e) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid fileSize at index " + i);
                    return false;
                }
                String fileExtension = media.getString("fileExtension");
                // Validate media type and Cloudinary URL format
                if (!type.matches("image|video") || !url.startsWith("https://res.cloudinary.com")) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid media type or URL at index " + i);
                    return false;
                }
                // Validate file size is non-negative
                if (fileSize < 0) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid fileSize at index " + i + ": must be non-negative");
                    return false;
                }
                // Validate file extension presence
                if (fileExtension == null || fileExtension.trim().isEmpty()) {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid fileExtension at index " + i + ": must be non-empty");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Handles POST requests for service creation
     * Creates new service with optional media assets in transaction-safe manner
     * 
     * @param request HttpServletRequest with JSON service data
     * @param response HttpServletResponse with created service details
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for service creation");
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

        // Extract and validate input parameters
        String name = requestBody.optString("name", null);
        String category = requestBody.optString("category", null);
        Double price = requestBody.has("price") ? requestBody.optDouble("price", -1) : null;
        String description = requestBody.optString("description", null);
        JSONArray mediaArray = requestBody.optJSONArray("media");

        if (!validateInput(name, category, price, description, mediaArray, response)) {
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false); // Start transaction for atomic operations
            try {
                // Insert service record
                String serviceQuery = "INSERT INTO services (provider_id, service_name, description, price, category, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
                int serviceId;
                try (PreparedStatement pstmt = con.prepareStatement(serviceQuery, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, providerId);
                    pstmt.setString(2, name.trim());
                    pstmt.setString(3, description.trim());
                    pstmt.setDouble(4, price);
                    pstmt.setString(5, category.trim());
                    pstmt.executeUpdate();

                    // Retrieve generated service ID
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            serviceId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated service ID");
                        }
                    }
                }

                // Insert media records if provided
                JSONArray responseMediaArray = new JSONArray();
                if (mediaArray != null && mediaArray.length() > 0) {
                    String mediaQuery = "INSERT INTO media (service_id, media_type, media_url, file_size, file_extension, status) VALUES (?, ?, ?, ?, ?, 'Active')";
                    try (PreparedStatement mediaPstmt = con.prepareStatement(mediaQuery, Statement.RETURN_GENERATED_KEYS)) {
                        for (int i = 0; i < mediaArray.length(); i++) {
                            JSONObject media = mediaArray.getJSONObject(i);
                            mediaPstmt.setInt(1, serviceId);
                            mediaPstmt.setString(2, media.getString("type"));
                            mediaPstmt.setString(3, media.getString("url"));
                            mediaPstmt.setDouble(4, media.getDouble("fileSize"));
                            mediaPstmt.setString(5, media.getString("fileExtension"));
                            mediaPstmt.executeUpdate();

                            // Retrieve generated media IDs for response
                            try (ResultSet mediaKeys = mediaPstmt.getGeneratedKeys()) {
                                if (mediaKeys.next()) {
                                    JSONObject mediaResponse = new JSONObject();
                                    mediaResponse.put("id", mediaKeys.getInt(1));
                                    mediaResponse.put("type", media.getString("type"));
                                    mediaResponse.put("url", media.getString("url"));
                                    responseMediaArray.put(mediaResponse);
                                }
                            }
                        }
                    }
                }

                // Commit transaction on success
                con.commit();

                // Prepare comprehensive response with created service details
                JSONObject responseJson = new JSONObject();
                responseJson.put("id", serviceId);
                responseJson.put("providerId", providerId);
                responseJson.put("name", name);
                responseJson.put("description", description);
                responseJson.put("price", price);
                responseJson.put("category", category);
                responseJson.put("media", responseMediaArray);

                logger.info("Created service ID: {} with {} media entries for provider ID: {}", serviceId, responseMediaArray.length(), providerId);

                // Send success response with created service data
                response.setStatus(HttpServletResponse.SC_CREATED);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(responseJson.toString());
                    writer.flush();
                }
            } catch (SQLException e) {
                // Rollback transaction on database error
                con.rollback();
                logger.error("Database error creating service for provider ID: {}", providerId, e);
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