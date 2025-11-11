package shaadisarthi.booking;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shaadisharthi.DbConnection.DbConnection;

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
import java.time.LocalDateTime;

/**
 * SERVLET: BookingListServlet
 * 
 * PURPOSE: Provides paginated booking list for service providers
 * Enables providers to view and manage their booking requests with:
 * - Status-based filtering (Pending, Confirmed, etc.)
 * - Pagination with configurable page sizes
 * - Customer contact information for follow-up
 * - Future-date validation for pending bookings
 * 
 * SECURITY: JWT-based provider authentication
 * ARCHITECTURE: RESTful GET endpoint with query parameters
 * UI INTEGRATION: Designed for provider dashboard integration
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
@WebServlet(urlPatterns = {"/booking-list-servlet"})
public class BookingListServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(BookingListServlet.class);
    private static final Gson GSON = new Gson();
    private final BookingDAO bookingDAO = new BookingDAO();
    
    /**
     * Used by the provider to fetch paginated booking requests
     * Supports filtering by status and pagination parameters
     * 
     * @param request HTTP request with query parameters: page, limit, status
     * @param response JSON response with bookings array and pagination metadata
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract providerId from JWT claims for authentication
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            logger.warn("Unauthorized request: Missing JWT claims at {}", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized: Missing JWT claims");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        int providerId;
        try {
            providerId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException e) {
            logger.warn("Invalid provider ID in JWT: {} at {}", e.getMessage(), LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid provider ID in JWT");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Parse query parameters with defaults
        int page = 1;
        int limit = 20;
        String status = "Pending";
        try {
            String pageStr = request.getParameter("page");
            String limitStr = request.getParameter("limit");
            String statusStr = request.getParameter("status");
            if (pageStr != null) page = Integer.parseInt(pageStr);
            if (limitStr != null) limit = Integer.parseInt(limitStr);
            if (statusStr != null) {
                if (statusStr.equals("Confirmed")) {
                    status = "Accepted"; // Map frontend status to database status
                } else {
                    status = statusStr;
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid page or limit parameter: {} at {}", e.getMessage(), LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid page or limit parameter");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        int offset = (page - 1) * limit;

        // Fetch bookings with filtering and pagination
        try (Connection conn = DbConnection.getCon()) {
            String sql = "SELECT b.booking_id, b.event_start_date, b.total_amount, s.service_name, c.name, c.phone_no " +
                         "FROM bookings b " +
                         "JOIN booking_services bs ON b.booking_id = bs.booking_id " +
                         "JOIN services s ON b.service_id = s.service_id " +
                         "JOIN customers c ON b.customer_id = c.customer_id " +
                         "WHERE bs.provider_id = ? AND b.status = ?";
            // For pending bookings, only show future events
            if (status.equals("Pending")) {
                sql += " AND b.event_start_date >= CURRENT_DATE";
            }
            sql += " ORDER BY b.event_start_date DESC " +
                   "LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, providerId);
                ps.setString(2, status);
                ps.setInt(3, limit);
                ps.setInt(4, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    JsonArray bookings = new JsonArray();
                    while (rs.next()) {
                        JsonObject booking = new JsonObject();
                        booking.addProperty("bookingId", rs.getInt("booking_id"));
                        booking.addProperty("eventStartDate", rs.getTimestamp("event_start_date").toString());
                        booking.addProperty("totalAmount", rs.getDouble("total_amount"));
                        booking.addProperty("serviceName", rs.getString("service_name"));
                        booking.addProperty("customerName", rs.getString("name"));
                        booking.addProperty("phone", rs.getString("phone_no"));
                        bookings.add(booking);
                    }

                    // Get total count for pagination metadata
                    String countSql = "SELECT COUNT(*) FROM bookings b JOIN booking_services bs ON b.booking_id = bs.booking_id WHERE bs.provider_id = ? AND b.status = ?";
                    if (status.equals("Pending")) {
                        countSql += " AND b.event_start_date >= CURRENT_DATE";
                    }
                    try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                        countPs.setInt(1, providerId);
                        countPs.setString(2, status);
                        try (ResultSet countRs = countPs.executeQuery()) {
                            int total = countRs.next() ? countRs.getInt(1) : 0;
                            JsonObject responseBody = new JsonObject();
                            responseBody.add("bookings", bookings);
                            responseBody.addProperty("total", total);
                            responseBody.addProperty("page", page);
                            responseBody.addProperty("limit", limit);
                            response.setStatus(HttpServletResponse.SC_OK);
                            out.println(GSON.toJson(responseBody));
                            logger.info("Fetched {} bookings with Status {} for provider {} at {}", bookings.size(), status, providerId, LocalDateTime.now());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error for provider {}: {} at {}", providerId, e.getMessage(), LocalDateTime.now(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Database error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        }
    }
}