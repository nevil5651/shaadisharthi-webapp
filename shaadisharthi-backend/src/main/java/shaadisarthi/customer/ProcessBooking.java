package shaadisarthi.customer;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Claims;

import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.NotificationService;
import shaadisharthi.websocket.CustomerSocket;

/**
 * Servlet implementation for processing customer bookings
 * Handles booking creation with validation and real-time notifications
 * 
 * @WebServlet Maps to "/process-bookioiliukngs" endpoint
 * @version 1.0
 * @description Processes POST requests for creating service bookings with comprehensive validation
 */
@WebServlet("/process-bookioiliukngs")
public class ProcessBooking extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    /**
     * Processes POST requests for booking creation
     * Validates JWT claims, parses booking data, and creates booking records
     * 
     * @param request HttpServletRequest containing booking parameters
     * @param response HttpServletResponse for sending booking creation result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract customerId from JWT claims for authentication
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized: Missing or invalid JWT claims");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid customer ID in JWT");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Parse and validate booking request parameters
        BookingRequest bookingRequest;
        try {
            bookingRequest = parseBookingRequest(request);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", e.getMessage());
            out.println(GSON.toJson(errorResponse));
            return;
        }

        try (Connection conn = DbConnection.getCon()) {
            // Insert booking into database and get generated booking ID
            int bookingId = createBooking(conn, customerId, bookingRequest);

            // Send real-time notification to customer via WebSocket
            CustomerSocket.notifyCustomer(
            	    customerId,
            	    "Your booking has been placed successfully! Please wait for provider approval.",
            	    bookingId
            	);
            System.out.println("Message from client: ");

            // Return success response with booking ID
            response.setStatus(HttpServletResponse.SC_CREATED);
            JsonObject successResponse = new JsonObject();
            successResponse.addProperty("message", "Booking request submitted successfully");
            successResponse.addProperty("bookingId", bookingId);
            out.println(GSON.toJson(successResponse));

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Database error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unexpected error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        }
    }

    /**
     * Parses and validates booking request parameters from HTTP request
     * 
     * @param request HttpServletRequest containing booking parameters
     * @return BookingRequest object with validated data
     * @throws IllegalArgumentException if validation fails
     */
    private BookingRequest parseBookingRequest(HttpServletRequest request) {
        try {
            int serviceId = Integer.parseInt(request.getParameter("service_id"));
            int providerId = Integer.parseInt(request.getParameter("provider_id"));
            String serviceName = request.getParameter("service_name");
            double servicePrice = Double.parseDouble(request.getParameter("service_price"));
            String customerName = request.getParameter("customer_name");
            String phone = request.getParameter("phone");
            String eventAddress = request.getParameter("event_address");
            String eventStartDate = request.getParameter("event_start_date");
            String eventTime = request.getParameter("event_time");
            String eventEndDate = request.getParameter("event_end_date");
            String notes = request.getParameter("notes");

            // Comprehensive validation of all booking parameters
            if (serviceId <= 0) {
                throw new IllegalArgumentException("Invalid service ID");
            }
            if (providerId <= 0) {
                throw new IllegalArgumentException("Invalid provider ID");
            }
            if (serviceName == null || serviceName.trim().isEmpty()) {
                throw new IllegalArgumentException("Service name is required");
            }
            if (servicePrice < 0) {
                throw new IllegalArgumentException("Service price cannot be negative");
            }
            if (customerName == null || customerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer name is required");
            }
            if (phone == null || !phone.matches("^[0-9]{10}$")) {
                throw new IllegalArgumentException("Invalid phone number (must be 10 digits)");
            }
            if (eventAddress == null || eventAddress.trim().isEmpty()) {
                throw new IllegalArgumentException("Event address is required");
            }
            if (eventStartDate == null || !eventStartDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                throw new IllegalArgumentException("Invalid event start date (YYYY-MM-DD)");
            }
            if (eventTime == null || !eventTime.matches("^\\d{2}:\\d{2}$")) {
                throw new IllegalArgumentException("Invalid event time (HH:MM)");
            }
            // eventEndDate and notes are optional, no strict validation needed

            return new BookingRequest(
                serviceId, providerId, serviceName, servicePrice, customerName,
                phone, eventAddress, eventStartDate, eventTime, eventEndDate, notes
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric input: " + e.getMessage());
        }
    }

    /**
     * Creates booking record in database
     * 
     * @param conn Database connection
     * @param customerId ID of the customer making the booking
     * @param request BookingRequest object containing booking details
     * @return Generated booking ID
     * @throws SQLException if database operation fails
     */
    private int createBooking(Connection conn, int customerId, BookingRequest request) throws SQLException {
        String sql = "INSERT INTO bookings (customer_id, provider_id, service_id, event_address, " +
                     "event_start_date, event_end_date, booking_date, request_status, total_amount, notes, event_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, CURDATE(), 'Pending', ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, customerId);
            stmt.setInt(2, request.providerId);
            stmt.setInt(3, request.serviceId);
            stmt.setString(4, request.eventAddress);
            stmt.setString(5, request.eventStartDate);
            stmt.setString(6, request.eventEndDate);
            stmt.setDouble(7, request.servicePrice);
            stmt.setString(8, request.notes);
            stmt.setString(9, request.eventTime);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to create booking, no rows affected");
            }

            // Retrieve auto-generated booking ID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve booking ID");
                }
            }
        }
    }

    /**
     * Inner class representing booking request data structure
     * Used for parameter validation and data transfer
     */
    private static class BookingRequest {
        final int serviceId;
        final int providerId;
        final String serviceName;
        final double servicePrice;
        final String customerName;
        final String phone;
        final String eventAddress;
        final String eventStartDate;
        final String eventTime;
        final String eventEndDate;
        final String notes;

        BookingRequest(int serviceId, int providerId, String serviceName, double servicePrice,
                       String customerName, String phone, String eventAddress,
                       String eventStartDate, String eventTime, String eventEndDate, String notes) {
            this.serviceId = serviceId;
            this.providerId = providerId;
            this.serviceName = serviceName;
            this.servicePrice = servicePrice;
            this.customerName = customerName;
            this.phone = phone;
            this.eventAddress = eventAddress;
            this.eventStartDate = eventStartDate;
            this.eventTime = eventTime;
            this.eventEndDate = eventEndDate;
            this.notes = notes;
        }
    }
}