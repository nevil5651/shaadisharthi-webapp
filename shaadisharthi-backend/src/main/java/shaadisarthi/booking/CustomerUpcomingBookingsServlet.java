package shaadisarthi.booking;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import shaadisharthi.DbConnection.DbConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

/**
 * SERVLET: CustomerUpcomingBookingsServlet
 * 
 * PURPOSE: Provides quick overview of upcoming bookings for customer dashboard
 * Lightweight endpoint for dashboard widgets with:
 * - Limited to 3 upcoming bookings for UI space constraints
 * - Total count of all upcoming bookings
 * - Focus on immediate scheduling needs
 * - Optimized for fast dashboard loading
 * 
 * SECURITY: JWT-based customer authentication
 * ARCHITECTURE: RESTful GET endpoint with minimal data
 * PERFORMANCE: Single optimized query with limit
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
@WebServlet("/cstmr-upcoming-bookings")
public class CustomerUpcomingBookingsServlet extends HttpServlet {
    private static final Gson GSON = new Gson();

    /**
     * Handles GET requests for customer's upcoming bookings
     * Returns limited set for dashboard display with total count
     * 
     * @param request HTTP request
     * @param response JSON response with upcoming bookings and total count
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
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid customer ID");
            out.print(GSON.toJson(errorResponse));
            return;
        }

        try (Connection conn = DbConnection.getCon()) {
            // Query to get upcoming bookings (limited to 3 for dashboard space)
            String sql = "SELECT b.booking_id AS id, s.service_name, sp.name AS provider_name, " +
                         "b.event_start_date AS date, b.event_time, b.status " +
                         "FROM bookings b " +
                         "JOIN services s ON b.service_id = s.service_id " +
                         "JOIN service_providers sp ON s.provider_id = sp.provider_id " +
                         "WHERE b.customer_id = ? " +
                         "AND b.event_start_date >= CURDATE() " + // Only future events
                         "AND b.status IN ('Pending', 'Accepted', 'Confirmed') " + // Active statuses only
                         "ORDER BY b.event_start_date ASC " + // Soonest first
                         "LIMIT 3";

            // Query to count total upcoming bookings for dashboard badge
            String countSql = "SELECT COUNT(*) AS total " +
                             "FROM bookings b " +
                             "WHERE b.customer_id = ? " +
                             "AND b.event_start_date >= CURDATE() " +
                             "AND b.status IN ('Pending', 'Accepted', 'Confirmed')";

            JsonObject result = new JsonObject();
            JsonArray bookingsArray = new JsonArray();

            // Fetch the bookings (up to 3)
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JsonObject booking = new JsonObject();
                        booking.addProperty("id", rs.getInt("id"));
                        booking.addProperty("serviceName", rs.getString("service_name"));
                        booking.addProperty("providerName", rs.getString("provider_name"));
                        booking.addProperty("date", rs.getString("date"));
                        booking.addProperty("time", rs.getString("event_time"));
                        booking.addProperty("status", rs.getString("status"));
                        bookingsArray.add(booking);
                    }
                }
            }

            // Fetch the total count of upcoming bookings for UI badge
            int totalBookings = 0;
            try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                countPs.setInt(1, customerId);
                try (ResultSet countRs = countPs.executeQuery()) {
                    if (countRs.next()) {
                        totalBookings = countRs.getInt("total");
                    }
                }
            }

            // Build the response with both limited data and total count
            result.add("bookings", bookingsArray);
            result.addProperty("totalBookings", totalBookings);
            out.print(GSON.toJson(result));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Database error");
            out.print(GSON.toJson(errorResponse));
        }
    }
}