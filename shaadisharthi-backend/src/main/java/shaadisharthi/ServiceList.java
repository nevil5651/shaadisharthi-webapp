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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * Servlet for retrieving service provider's service listings
 * Provides comprehensive service data including associated media assets
 * 
 * Features:
 * - JWT-authenticated service retrieval
 * - Complete service details with media
 * - Efficient database queries with joins
 * 
 * @WebServlet Maps to "/providerservices" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/providerservices")
public class ServiceList extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);

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
     * Handles GET requests for retrieving provider services
     * Returns complete service listings with associated media assets
     * 
     * @param request HttpServletRequest with JWT claims
     * @param response HttpServletResponse with service array
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for fetching provider services");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract provider ID from JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        Integer providerId;
        try {
            providerId = Integer.valueOf(claims.getSubject());
            logger.debug("Authenticated provider ID: {}", providerId);
        } catch (NumberFormatException | NullPointerException e) {
            logger.error("Invalid or missing provider ID in token: {}", claims != null ? claims.getSubject() : "null");
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        JSONArray servicesArray = new JSONArray();
        try (Connection con = DbConnection.getCon()) {
            // Fetch all services for the authenticated provider
            String serviceQuery = "SELECT service_id, service_name, category, price, description FROM services WHERE provider_id = ?";
            try (PreparedStatement pstmt = con.prepareStatement(serviceQuery)) {
                pstmt.setInt(1, providerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        JSONObject service = new JSONObject();
                        int serviceId = rs.getInt("service_id");
                        service.put("id", serviceId);
                        service.put("providerId", providerId);
                        service.put("name", rs.getString("service_name"));
                        service.put("description", rs.getString("description"));
                        service.put("category", rs.getString("category"));
                        service.put("price", rs.getDouble("price"));

                        // Fetch associated media assets for each service
                        JSONArray mediaArray = new JSONArray();
                        String mediaQuery = "SELECT media_id, media_url, media_type FROM media WHERE service_id = ?";
                        try (PreparedStatement mediaPstmt = con.prepareStatement(mediaQuery)) {
                            mediaPstmt.setInt(1, serviceId);
                            try (ResultSet mediaRs = mediaPstmt.executeQuery()) {
                                while (mediaRs.next()) {
                                    JSONObject media = new JSONObject();
                                    media.put("id", mediaRs.getInt("media_id"));
                                    media.put("url", mediaRs.getString("media_url"));
                                    media.put("type", mediaRs.getString("media_type"));
                                    mediaArray.put(media);
                                }
                            }
                        }
                        service.put("media", mediaArray);
                        servicesArray.put(service);
                    }
                }
            }
            logger.info("Fetched {} services for provider ID: {}", servicesArray.length(), providerId);
        } catch (SQLException e) {
            logger.error("Database error while fetching services for provider ID {}: {}", providerId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return;
        }

        // Send success response with services array
        try (PrintWriter writer = response.getWriter()) {
            writer.write(servicesArray.toString());
            writer.flush();
        }
    }
}