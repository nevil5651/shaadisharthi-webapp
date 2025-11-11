package shaadisarthi.booking;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.websocket.CustomerSocket;

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
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SERVLET: CustomerBookingActionServlet
 * 
 * PURPOSE: Handles customer-initiated actions on their bookings
 * Supports customer-side booking management with:
 * - Booking cancellation with reason tracking
 * - Marking bookings as complete
 * - Payment initiation (placeholder for future implementation)
 * - Real-time WebSocket notifications
 * - Provider notification for cancellations
 * 
 * SECURITY: JWT-based customer authentication with ownership validation
 * ARCHITECTURE: RESTful endpoint with path parameters
 * BUSINESS RULES: Status validation for allowed actions
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
@WebServlet("/api/customer/bookings/{bookingId}/action")
public class CustomerBookingActionServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CustomerBookingActionServlet.class);
    private static final Gson GSON = new Gson();
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final BookingDAO bookingDAO = new BookingDAO();

    /**
     * Handles POST requests for customer booking actions
     * 
     * @param request HTTP request with action in JSON body
     * @param response JSON response with action result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract bookingId from path parameter
        String bookingIdStr = request.getPathInfo().substring(1);
        int bookingId;
        try {
            bookingId = Integer.parseInt(bookingIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid booking ID");
            out.print(GSON.toJson(errorResponse));
            return;
        }

        // Extract customerId from JWT for authentication
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized");
            out.print(GSON.toJson(errorResponse));
            return;
        }
        int customerId = Integer.parseInt(claims.getSubject());

        // Parse request body for action and reason
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JsonObject body = GSON.fromJson(sb.toString(), JsonObject.class);
        String action = body.get("action").getAsString();
        String reason = body.has("reason") ? body.get("reason").getAsString() : "";

        Connection conn = null;
        try {
            conn = DbConnection.getCon();
            conn.setAutoCommit(false);

            // Validate customer ownership of the booking
            if (!bookingDAO.isCustomerOwner(conn, bookingId, customerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Not your booking");
                out.print(GSON.toJson(errorResponse));
                return;
            }

            String currentDetailsStatus = bookingDAO.getBookingDetailsStatus(conn, bookingId);
            
            // Handle cancellation action (only for Pending or Confirmed bookings)
            if ("cancel".equals(action) && ("Pending".equals(currentDetailsStatus) || "Confirmed".equals(currentDetailsStatus))) {
                // Update to Rejected/Cancelled status
                String bookingsStatus = "Rejected";
                String detailsStatus = "Cancelled";
                updateStatus(conn, bookingId, bookingsStatus, detailsStatus, reason);
                
                // ✅ Instant customer notification via WebSocket
                CustomerSocket.notifyCustomer(customerId, 
                    "Your booking with Id. #" + bookingId + " has been cancelled successfully.", bookingId);

                // Send email to provider for cancellation notification
                String providerEmail = bookingDAO.getProviderEmail(conn, bookingId);
                executor.submit(() -> new EmailService().sendProviderCancellation(providerEmail, bookingId, reason, currentDetailsStatus));
            } else if ("markComplete".equals(action)) {
                // Update to Completed status
                String bookingsStatus = "Accepted";
                String detailsStatus = "Completed";
                updateStatus(conn, bookingId, bookingsStatus, detailsStatus, null);
                
                // ✅ Instant customer notification via WebSocket
                CustomerSocket.notifyCustomer(customerId, 
                    "Your booking #" + bookingId + " has been marked as completed. Thank you!", bookingId);

            } else if ("pay".equals(action)) {
                // Placeholder for future payment integration
                response.setStatus(HttpServletResponse.SC_OK);
                JsonObject successResponse = new JsonObject();
                successResponse.addProperty("message", "Payment initiated (coming soon)");
                out.print(GSON.toJson(successResponse));
                return;
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Invalid action or status");
                out.print(GSON.toJson(errorResponse));
                return;
            }

            conn.commit();
            response.setStatus(HttpServletResponse.SC_OK);
            JsonObject successResponse = new JsonObject();
            successResponse.addProperty("success", "Succesfully done");
            out.print(GSON.toJson(successResponse));

        } catch (SQLException e) {
            logger.error("Action error at {}", LocalDateTime.now(), e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Rollback failed at {}", LocalDateTime.now(), rollbackEx);
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Database error");
            out.print(GSON.toJson(errorResponse));
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection at {}", LocalDateTime.now(), e);
                }
            }
        }
    }

    /**
     * Updates booking status across multiple tables
     * 
     * @param conn Database connection
     * @param bookingId Booking ID to update
     * @param bookingsStatus Status for bookings table
     * @param detailsStatus Status for booking_details table
     * @param reason Cancellation reason (if applicable)
     * @throws SQLException if database update fails
     */
    private void updateStatus(Connection conn, int bookingId, String bookingsStatus, String detailsStatus, String reason) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingsStatus);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }

        sql = "UPDATE booking_details SET status = ? WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, detailsStatus);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }
}