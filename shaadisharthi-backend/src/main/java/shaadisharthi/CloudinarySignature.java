package shaadisharthi;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.jsonwebtoken.Claims;
import shaadisharthi.utils.ConfigUtil;

/**
 * Servlet for generating Cloudinary upload signatures for secure media uploads
 * Provides authenticated signature generation for image and video uploads
 * 
 * Features:
 * - JWT-authenticated signature generation
 * - Secure folder-based upload restrictions
 * - Cloudinary API integration for media management
 * - Configurable upload destinations
 * 
 * Security: Restricts uploads to predefined folders only
 * 
 * @WebServlet Maps to "/cloudinarysignature" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/cloudinarysignature")
public class CloudinarySignature extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CloudinarySignature.class);
    private static final Properties config = new Properties();
    
    // Predefined allowed folders for security - prevents arbitrary upload locations
    private static final String[] ALLOWED_FOLDERS = {"shaadisharthi/images", "shaadisharthi/videos"};

    // Static initialization block for loading configuration
    static {
        try {
            config.load(CloudinarySignature.class.getClassLoader().getResourceAsStream("config.properties"));
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
     * Handles POST requests for Cloudinary signature generation
     * Authenticates user and generates secure upload signatures
     * 
     * @param request HttpServletRequest with JSON containing upload parameters
     * @param response HttpServletResponse with signature data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for Cloudinary signature");
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

        // Load Cloudinary credentials from configuration
        String cloudName = ConfigUtil.get("cloudinary.cloud_name", "CLOUDINARY_CLOUD_NAME");
        String apiKey = ConfigUtil.get("cloudinary.api_key", "CLOUDINARY_API_KEY");
        String apiSecret = ConfigUtil.get("cloudinary.api_secret", "CLOUDINARY_API_SECRET");

        // Validate Cloudinary configuration
        if (cloudName == null || apiKey == null || apiSecret == null) {
            logger.error("Missing Cloudinary credentials for provider ID: {}", providerId);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
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

            // Parse JSON request body
            JSONObject requestBody;
            try {
                requestBody = new JSONObject(sb.toString());
            } catch (Exception e) {
                logger.warn("Invalid JSON request body for provider ID: {}", providerId);
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
                return;
            }

            // Validate folder parameter against allowed list for security
            String folder = requestBody.optString("folder", null);
            boolean isValidFolder = false;
            for (String allowedFolder : ALLOWED_FOLDERS) {
                if (allowedFolder.equals(folder)) {
                    isValidFolder = true;
                    break;
                }
            }
            if (!isValidFolder) {
                logger.warn("Invalid or missing folder parameter for provider ID: {}. Received: {}", providerId, folder);
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Folder must be 'shaadisharthi/images' or 'shaadisharthi/videos'");
                return;
            }

            // Prepare parameters for signature generation
            Map<String, Object> paramsToSign = new HashMap<>();
            paramsToSign.put("folder", folder);
            // Include all other parameters from request (except folder)
            for (String key : requestBody.keySet()) {
                if (!key.equals("folder")) {
                    paramsToSign.put(key, requestBody.get(key));
                }
            }

            // Initialize Cloudinary client with secure connection
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
            ));

            // Generate timestamp and signature using Cloudinary API
            long timestamp = System.currentTimeMillis() / 1000L;
            paramsToSign.put("timestamp", timestamp);
            Map<String, Object> sortedParams = new TreeMap<>(paramsToSign);
            String signature;
            try {
                signature = cloudinary.apiSignRequest(sortedParams, apiSecret);
            } catch (Exception e) {
                logger.error("Failed to generate signature for provider ID: {}, folder: {}", providerId, folder, e);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Signature generation failed");
                return;
            }

            // Prepare success response with signature data
            JSONObject responseJson = new JSONObject();
            responseJson.put("signature", signature);
            responseJson.put("timestamp", timestamp);
            responseJson.put("apiKey", apiKey);

            logger.info("Generated Cloudinary signature for provider ID: {}, folder: {}", providerId, folder);

            // Send response
            try (PrintWriter writer = response.getWriter()) {
                writer.write(responseJson.toString());
                writer.flush();
            }
        } catch (Exception e) {
            logger.error("Error processing signature request for provider ID: {}", providerId, e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}