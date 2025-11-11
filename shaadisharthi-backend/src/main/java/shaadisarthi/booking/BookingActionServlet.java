package shaadisarthi.booking;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SERVLET: BookingActionServlet
 * 
 * PURPOSE: Handles service provider actions on bookings (accept, reject, complete, cancel)
 * Provides comprehensive booking management functionality for service providers with:
 * - Real-time WebSocket notifications to customers
 * - Automated email notifications for all actions
 * - Transactional database updates across multiple tables
 * - Provider authorization validation
 * - Business rule enforcement (e.g., cannot complete before service time)
 * 
 * SECURITY: JWT-based provider authentication with ownership validation
 * ARCHITECTURE: RESTful endpoint with asynchronous email processing
 * DATABASE TABLES: bookings, booking_details, booking_services
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
@WebServlet(urlPatterns = {"/booking-action/*"})
public class BookingActionServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(BookingActionServlet.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final BookingDAO bookingDAO = new BookingDAO();

    /**
     * Handles POST requests for booking actions by service providers
     * Supports: accept, reject, complete, cancel with reason tracking
     * 
     * @param request HTTP request containing action in JSON body
     * @param response JSON response with updated booking status
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract bookingId from path parameter and validate
        String bookingIdStr = request.getPathInfo().substring(1); 
        int bookingId;
        try {
            bookingId = Integer.parseInt(bookingIdStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid bookingId: {} at {}", bookingIdStr, LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid booking ID");
            out.println(errorResponse);
            return;
        }

        // Extract providerId from JWT claims for authentication
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            logger.warn("Unauthorized request: Missing JWT claims at {}", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized: Missing JWT claims");
            out.println(errorResponse);
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
            out.println(errorResponse);
            return;
        }

        // Parse JSON request body for action and reason
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JsonObject body;
        try {
            body = JsonParser.parseString(sb.toString()).getAsJsonObject();
        } catch (Exception ex) {
            logger.error("Malformed JSON: {} at {}", ex.getMessage(), LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Malformed JSON");
            out.println(errorResponse);
            return;
        }

        String action = body.has("action") ? body.get("action").getAsString().toLowerCase() : "";
        String reason = body.has("reason") ? body.get("reason").getAsString().trim() : "";
        
        // Validate supported actions
        if (!("accept".equals(action) || "reject".equals(action) || "complete".equals(action) || "cancel".equals(action))) {
            logger.warn("Invalid action: {} at {}", action, LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Action must be 'accept', 'reject', 'complete', or 'cancel'");
            out.println(errorResponse);
            return;
        }

        // Database transaction for booking updates
        try (Connection conn = DbConnection.getCon()) {
            // Validate provider ownership of the booking
            if (!bookingDAO.isProviderOwner(conn, bookingId, providerId)) {
                logger.warn("Provider {} not authorized for booking {} at {}", providerId, bookingId, LocalDateTime.now());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Not authorized to manage this booking");
                out.println(errorResponse);
                return;
            }

            // Business rule: Cannot complete booking before service time has passed
            if ("complete".equals(action)) {
                if (!isServiceTimePassed(conn, bookingId)) {
                    logger.warn("Cannot complete booking {}: Service time has not passed at {}", bookingId, LocalDateTime.now());
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject errorResponse = new JsonObject();
                    errorResponse.addProperty("error", "Cannot complete booking before service time has passed");
                    out.println(errorResponse);
                    return;
                }
            }

            try {
                conn.setAutoCommit(false);

                String bookingsStatus;
                String detailsStatus;
                
                // Map actions to appropriate status values for different tables
                switch (action) {
                    case "accept":
                        bookingsStatus = "Accepted"; 
                        detailsStatus = "Confirmed"; 
                        break;
                    case "reject":
                        bookingsStatus = "Rejected"; 
                        detailsStatus = "Cancelled"; 
                        break;
                    case "complete": 
                        bookingsStatus = "Completed"; 
                        detailsStatus = "Completed"; 
                        break;
                    case "cancel":
                        bookingsStatus = "Cancelled"; 
                        detailsStatus = "Cancelled"; 
                        break;
                    default:
                        bookingsStatus = "";
                        detailsStatus = "";
                        break;
                }

                // 1. Update main bookings table status
                String updateSql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, bookingsStatus);
                    ps.setInt(2, bookingId);
                    int affected = ps.executeUpdate();
                    if (affected == 0) {
                        logger.warn("No booking updated for booking_id: {} at {}", bookingId, LocalDateTime.now());
                        conn.rollback();
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        JsonObject errorResponse = new JsonObject();
                        errorResponse.addProperty("error", "Booking not found");
                        out.println(errorResponse);
                        return;
                    }
                }

                // 2. Update booking_details table status
                updateSql = "UPDATE booking_details SET status = ? WHERE booking_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, detailsStatus);
                    ps.setInt(2, bookingId);
                    ps.executeUpdate();
                }

                // Store cancellation reason for reject/cancel actions
                if (("cancel".equals(action) || "reject".equals(action)) && !reason.isEmpty()) {
                    updateSql = "UPDATE bookings SET cancellation_reason = ? WHERE booking_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, reason);
                        ps.setInt(2, bookingId);
                        ps.executeUpdate();
                    }
                }

                conn.commit();

                // Asynchronous email notifications and WebSocket updates
                String customerEmail = bookingDAO.getCustomerEmail(conn, bookingId);
                int customerId = getCustomerIdByEmail(conn, customerEmail);

                String serviceName = bookingDAO.getServiceName(conn, bookingId);
                LocalDateTime startDateTime = bookingDAO.getEventStartDate(conn, bookingId);
                double totalAmount = bookingDAO.getTotalAmount(conn, bookingId);
                
                if (customerEmail != null) {
                    switch (action) {
                        case "accept":
                        	CustomerSocket.notifyCustomer(customerId, 
                                    "Your booking for " + serviceName + " was accepted by provider. U will get Email Shortly. Thank you!", bookingId);

                            executor.submit(() -> {
                                try {
                                    new EmailService().sendCustomerAcceptance(customerEmail, serviceName, bookingId, startDateTime, totalAmount);
                                    logger.info("Acceptance email sent for booking_id: {} at {}", bookingId, LocalDateTime.now());
                                } catch (Exception e) {
                                    logger.error("Failed to send acceptance email for booking_id {}: {} at {}", bookingId, e.getMessage(), LocalDateTime.now());
                                }
                            });
                            break;
                        case "reject":
                        	CustomerSocket.notifyCustomer(customerId, 
                                    "Your booking for " + serviceName + " was not accepted by provider. U will get Email Shortly. Thank you!", bookingId);

                            executor.submit(() -> {
                                try {
                                    new EmailService().sendCustomerRejection(customerEmail, bookingId, reason);
                                    logger.info("Rejection email sent for booking_id: {} at {}", bookingId, LocalDateTime.now());
                                } catch (Exception e) {
                                    logger.error("Failed to send rejection email for booking_id {}: {} at {}", bookingId, e.getMessage(), LocalDateTime.now());
                                }
                            });
                            break;
                        case "complete":
                        	CustomerSocket.notifyCustomer(customerId, 
                                    "Your booking for " + serviceName + " was marked Completed by provider. Thank you!", bookingId);

                            executor.submit(() -> {
                                try {
                                    new EmailService().sendCustomerCompletion(customerEmail, serviceName, bookingId, startDateTime, totalAmount);
                                    logger.info("Completion email sent for booking_id: {} at {}", bookingId, LocalDateTime.now());
                                } catch (Exception e) {
                                    logger.error("Failed to send completion email for booking_id {}: {} at {}", bookingId, e.getMessage(), LocalDateTime.now());
                                }
                            });
                            break;
                        case "cancel":
                        	CustomerSocket.notifyCustomer(customerId, 
                                    "Your booking for " + serviceName + " was Cancelled by provider. U will get Email Shortly. Thank you!", bookingId);

                            executor.submit(() -> {
                                try {
                                    new EmailService().sendCustomerCancellation(customerEmail, serviceName, bookingId, startDateTime, reason);
                                    logger.info("Cancellation email sent for booking_id: {} at {}", customerEmail, bookingId, LocalDateTime.now());
                                } catch (Exception e) {
                                    logger.error("Failed to send cancellation email to {} for booking_id {}: {} at {}", customerEmail, bookingId, e.getMessage(), LocalDateTime.now());
                                }
                            });
                            break;
                    }
                }

                // Success response
                JsonObject success = new JsonObject();
                success.addProperty("booking_id", bookingId);
                success.addProperty("status", detailsStatus); // Return the detailed status
                success.addProperty("message", "Booking " + action + " successfully");
                response.setStatus(HttpServletResponse.SC_OK);
                out.println(success);
                logger.info("Booking {} updated to {} by provider {} at {}", bookingId, detailsStatus, providerId, LocalDateTime.now());

            } catch (SQLException e) {
                logger.error("Database error for booking {}: {} at {}", bookingId, e.getMessage(), LocalDateTime.now(), e);
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Failed to rollback transaction: {} at {}", ex.getMessage(), LocalDateTime.now());
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Database error: " + e.getMessage());
                out.println(errorResponse);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Failed to set auto-commit: {} at {}", e.getMessage(), LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            logger.error("DB connection error: {} at {}", e.getMessage(), LocalDateTime.now(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "DB connection error: " + e.getMessage());
            out.println(errorResponse);
        }
    }

    /**
     * Retrieves customer ID by email for WebSocket notifications
     * 
     * @param conn Database connection
     * @param email Customer email address
     * @return customer_id or -1 if not found
     * @throws SQLException if database query fails
     */
    private int getCustomerIdByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT customer_id FROM customers WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("customer_id");
                }
            }
        }
        return -1; // Return -1 if not found
    }

    /**
     * Validates if service time has passed for completion action
     * Business rule: Cannot mark booking as complete before service end time
     * 
     * @param conn Database connection
     * @param bookingId Booking ID to check
     * @return true if current time is after service end time
     * @throws SQLException if database query fails
     */
	private boolean isServiceTimePassed(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT event_end_date FROM bookings WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime endDateTime = rs.getTimestamp("event_end_date").toLocalDateTime();
                    return LocalDateTime.now().isAfter(endDateTime);
                }
            }
        }
        return false;
    }

    /**
     * Cleanup method to shutdown executor service when servlet is destroyed
     */
    @Override
    public void destroy() {
        executor.shutdown();
    }
}