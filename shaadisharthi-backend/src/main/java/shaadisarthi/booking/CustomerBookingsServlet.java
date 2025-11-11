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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * SERVLET: CustomerBookingsServlet
 * 
 * PURPOSE: Provides comprehensive booking history and management for customers
 * Advanced booking management with:
 * - Multi-filter capability (status, search, date range)
 * - Paginated results with configurable page sizes
 * - Unified status mapping between database and frontend
 * - Search across service and provider names
 * - Date range filtering for event dates
 * 
 * SECURITY: JWT-based customer authentication
 * ARCHITECTURE: RESTful GET endpoint with multiple query parameters
 * UI INTEGRATION: Designed for customer dashboard with advanced filtering
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
@WebServlet("/cstmr-bookings")
public class CustomerBookingsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CustomerBookingsServlet.class);
    private static final Gson GSON = new Gson();

    /**
     * Handles GET requests for customer booking history with advanced filtering
     * 
     * @param request HTTP request with query parameters: status, search, dateFrom, dateTo, page, limit
     * @param response JSON response with bookings array and pagination metadata
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract customerId from JWT for authentication
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            logger.warn("Unauthorized request at {}", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized");
            out.print(GSON.toJson(errorResponse));
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException e) {
            logger.warn("Invalid customer ID at {}", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid customer ID");
            out.print(GSON.toJson(errorResponse));
            return;
        }

        // Parse and validate query parameters
        String status = request.getParameter("status"); // e.g., "pending", "confirmed", "cancelled", "completed"
        String search = request.getParameter("search"); // Service or provider name search
        String dateFrom = request.getParameter("dateFrom"); // Event start date range
        String dateTo = request.getParameter("dateTo"); // Event end date range
        int page = request.getParameter("page") != null ? Integer.parseInt(request.getParameter("page")) : 1;
        int limit = request.getParameter("limit") != null ? Integer.parseInt(request.getParameter("limit")) : 10;
        int offset = (page - 1) * limit;

        try (Connection conn = DbConnection.getCon()) {
            // Build base SQL query with joins for comprehensive booking data
            StringBuilder sql = new StringBuilder(
                "SELECT b.booking_id, b.event_start_date, b.event_end_date, b.total_amount, b.status AS booking_status, bd.status AS details_status, " +
                "s.service_name, s.service_id, sp.name AS provider_name, b.event_time, b.notes, bd.price " +
                "FROM bookings b " +
                "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                "JOIN booking_services bs ON b.booking_id = bs.booking_id " +
                "JOIN services s ON b.service_id = s.service_id " +
                "JOIN service_providers sp ON bs.provider_id = sp.provider_id " +
                "WHERE b.customer_id = ?"
            );

            // Build count query for pagination
            StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(DISTINCT b.booking_id) " +
                "FROM bookings b " +
                "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                "JOIN booking_services bs ON b.booking_id = bs.booking_id " +
                "JOIN services s ON b.service_id = s.service_id " +
                "JOIN service_providers sp ON bs.provider_id = sp.provider_id " +
                "WHERE b.customer_id = ?"
            );

            // Add status filtering with unified logic mapping
            if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
                String normalizedStatus = status.toLowerCase();
                if ("confirmed".equals(normalizedStatus)) {
                    sql.append(" AND (bd.status = 'Confirmed' OR b.status = 'Accepted')");
                    countSql.append(" AND (bd.status = 'Confirmed' OR b.status = 'Accepted')");
                } else if ("cancelled".equals(normalizedStatus)) {
                    sql.append(" AND (bd.status = 'Cancelled' OR b.status = 'Rejected')");
                    countSql.append(" AND (bd.status = 'Cancelled' OR b.status = 'Rejected')");
                } else if ("completed".equals(normalizedStatus)) {
                    sql.append(" AND bd.status = 'Completed'");
                    countSql.append(" AND bd.status = 'Completed'");
                } else if ("pending".equals(normalizedStatus)) {
                    sql.append(" AND bd.status NOT IN ('Confirmed', 'Cancelled', 'Completed') AND b.status = 'Pending'");
                    countSql.append(" AND bd.status NOT IN ('Confirmed', 'Cancelled', 'Completed') AND b.status = 'Pending'");
                }
            }

            // Add search filtering across service and provider names
            if (search != null && !search.isEmpty()) {
                String searchLike = "%" + search + "%";
                sql.append(" AND (s.service_name LIKE ? OR sp.name LIKE ?)");
                countSql.append(" AND (s.service_name LIKE ? OR sp.name LIKE ?)");
            }

            // Add date filtering with DATE() for consistency
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sql.append(" AND DATE(b.event_start_date) >= ?");
                countSql.append(" AND DATE(b.event_start_date) >= ?");
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                sql.append(" AND DATE(b.event_end_date) <= ?");
                countSql.append(" AND DATE(b.event_end_date) <= ?");
            }

            // Add ORDER BY and LIMIT to main query
            sql.append(" ORDER BY b.booking_date DESC LIMIT ? OFFSET ?");

            // Execute count query for pagination metadata
            try (PreparedStatement countPs = conn.prepareStatement(countSql.toString())) {
                int paramIndex = 1;
                countPs.setInt(paramIndex++, customerId);
                if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
                    // No parameters needed for status due to direct conditions
                }
                if (search != null && !search.isEmpty()) {
                    countPs.setString(paramIndex++, "%" + search + "%");
                    countPs.setString(paramIndex++, "%" + search + "%");
                }
                if (dateFrom != null && !dateFrom.isEmpty()) {
                    countPs.setString(paramIndex++, dateFrom);
                }
                if (dateTo != null && !dateTo.isEmpty()) {
                    countPs.setString(paramIndex++, dateTo);
                }

                ResultSet countRs = countPs.executeQuery();
                int total = countRs.next() ? countRs.getInt(1) : 0;
                int totalPages = (int) Math.ceil((double) total / limit);

                // Execute main query with filtering and pagination
                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    paramIndex = 1;
                    ps.setInt(paramIndex++, customerId);
                    if (search != null && !search.isEmpty()) {
                        ps.setString(paramIndex++, "%" + search + "%");
                        ps.setString(paramIndex++, "%" + search + "%");
                    }
                    if (dateFrom != null && !dateFrom.isEmpty()) {
                        ps.setString(paramIndex++, dateFrom);
                    }
                    if (dateTo != null && !dateTo.isEmpty()) {
                        ps.setString(paramIndex++, dateTo);
                    }
                    ps.setInt(paramIndex++, limit);
                    ps.setInt(paramIndex++, offset);

                    ResultSet rs = ps.executeQuery();
                    JsonArray bookingsArray = new JsonArray();
                    while (rs.next()) {
                        JsonObject booking = new JsonObject();
                        booking.addProperty("id", rs.getInt("booking_id"));
                        booking.addProperty("serviceName", rs.getString("service_name"));
                        booking.addProperty("providerName", rs.getString("provider_name"));
                        booking.addProperty("date", rs.getString("event_start_date"));
                        booking.addProperty("time", rs.getString("event_time"));
                        booking.addProperty("amount", rs.getDouble("total_amount"));
                        booking.addProperty("status", getUnifiedStatus(rs.getString("booking_status"), rs.getString("details_status")));
                        booking.addProperty("paymentStatus", "unpaid"); // Placeholder for future payment integration
                        booking.addProperty("serviceImage", "/img/default-service.jpg"); // Placeholder for service images
                        bookingsArray.add(booking);
                    }

                    // Build comprehensive response with data and metadata
                    JsonObject result = new JsonObject();
                    result.add("bookings", bookingsArray);
                    result.addProperty("currentPage", page);
                    result.addProperty("totalPages", totalPages);
                    result.addProperty("totalItems", total);
                    out.print(GSON.toJson(result));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching bookings at {}", LocalDateTime.now(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Database error");
            out.print(GSON.toJson(errorResponse));
        }
    }

    /**
     * Unifies status from multiple database fields to frontend-standardized values
     * Maps database status combinations to simplified frontend statuses
     * 
     * @param bookingStatus Status from bookings table
     * @param detailsStatus Status from booking_details table
     * @return Unified status for frontend consumption
     */
    private String getUnifiedStatus(String bookingStatus, String detailsStatus) {
        // Map to frontend-normalized statuses
        if ("Confirmed".equals(detailsStatus) || "Accepted".equals(bookingStatus)) {
            return "confirmed";
        }
        if ("Cancelled".equals(detailsStatus) || "Rejected".equals(bookingStatus)) {
            return "cancelled";
        }
        if ("Completed".equals(detailsStatus)) {
            return "completed";
        }
        return "pending";
    }
}