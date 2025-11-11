package shaadisarthi.booking;

import com.google.gson.Gson;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SERVLET: BookingServlet (Main Booking Creation Endpoint)
 * 
 * PURPOSE: Handles new booking creation by customers
 * Comprehensive booking system with:
 * - Multi-table transactional integrity (bookings, booking_details, booking_services)
 * - Real-time WebSocket notifications to customers
 * - Automated provider email notifications
 * - Date/time validation and business rules
 * - Notification system integration
 * 
 * SECURITY: JWT-based customer authentication
 * ARCHITECTURE: RESTful POST endpoint with async email processing
 * DATABASE: Atomic transaction across 3 tables
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
@WebServlet(urlPatterns = {"/process-bookings"})
public class BookingServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(BookingServlet.class);
    private static final Gson GSON = new Gson();
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    private final BookingDAO bookingDAO = new BookingDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    /**
     * Handles POST requests for new booking creation
     * Creates booking across multiple tables and triggers notifications
     * 
     * @param request HTTP request containing booking details in JSON body
     * @param response JSON response with booking ID and status
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
            logger.warn("Unauthorized request: Missing JWT claims at {}", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized: Missing JWT claims");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException e) {
            logger.warn("Invalid customer ID in JWT: {} at {}", e.getMessage(), LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid customer ID in JWT");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Parse JSON request body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        } catch (Exception ex) {
            logger.error("Invalid request body: {} at {}", ex.getMessage(), LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid request body");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        JsonObject body;
        try {
            body = JsonParser.parseString(sb.toString()).getAsJsonObject();
        } catch (Exception ex) {
            logger.error("Malformed JSON: {} at {}", ex.getMessage(), LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Malformed JSON");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Extract and validate required booking fields
        Integer serviceId = body.has("service_id") && !body.get("service_id").isJsonNull() ? body.get("service_id").getAsInt() : null;
        String serviceName = body.has("service_name") ? body.get("service_name").getAsString().trim() : "";
        String customerName = body.has("customer_name") ? body.get("customer_name").getAsString().trim() : "";
        String phone = body.has("phone") ? body.get("phone").getAsString().trim() : "";
        String email = body.has("email") ? body.get("email").getAsString().trim() : "";
        Double servicePrice = body.has("service_price") ? body.get("service_price").getAsDouble() : null;
        String eventAddress = body.has("event_address") ? body.get("event_address").getAsString().trim() : "";
        String startDateStr = body.has("event_start_date") ? body.get("event_start_date").getAsString().trim() : null;
        String endDateStr = body.has("event_end_date") ? body.get("event_end_date").getAsString().trim() : null;
        String eventTimeStr = body.has("event_time") ? body.get("event_time").getAsString().trim() : null;
        String notes = body.has("notes") ? body.get("notes").getAsString().trim() : "";

        // Validate all required fields are present
        if (serviceId == null || servicePrice == null || serviceName.isEmpty() || customerName.isEmpty() ||
                phone.isEmpty() || startDateStr == null || eventTimeStr == null || eventAddress.isEmpty()) {
            logger.warn("Missing required fields in request at {}", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Missing required fields");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Parse and validate date/time formats
        LocalDate startDate, endDate;
        LocalTime eventTime;
        try {
            startDate = LocalDate.parse(startDateStr);
            endDate = (endDateStr == null || endDateStr.isEmpty()) ? startDate : LocalDate.parse(endDateStr);
            eventTime = LocalTime.parse(eventTimeStr);
        } catch (DateTimeParseException ex) {
            logger.error("Invalid date or time format: {} at {}", ex.getMessage(), LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid date or time format. Use yyyy-MM-dd and HH:mm");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        LocalDateTime startDateTime = LocalDateTime.of(startDate, eventTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate, eventTime);

        // Business rule: Cannot book in the past
        if (startDateTime.isBefore(LocalDateTime.now())) {
            logger.warn("Event start date/time cannot be in the past: {} at {}", startDateTime, LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Event start date/time cannot be in the past");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Database transaction for atomic booking creation
        try (Connection conn = DbConnection.getCon()) {
            try {
                conn.setAutoCommit(false);

                // Find provider info for notification and relationship mapping
                Map<String, Object> providerInfo = bookingDAO.findProviderInfoByServiceId(conn, serviceId);
                Integer providerId = (Integer) providerInfo.get("providerId");
                String providerEmail = (String) providerInfo.get("email");
                if (providerId == null || providerEmail == null) {
                    logger.error("Service not found or provider missing for service_id: {} at {}", serviceId, LocalDateTime.now());
                    conn.rollback();
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject errorResponse = new JsonObject();
                    errorResponse.addProperty("error", "Service not found or provider missing for service_id: " + serviceId);
                    out.println(GSON.toJson(errorResponse));
                    return;
                }

                // Create main booking record
                int bookingId = bookingDAO.createBooking(
                        conn,
                        customerId,
                        serviceId,
                        eventAddress,
                        Timestamp.valueOf(startDateTime),
                        Timestamp.valueOf(endDateTime),
                        servicePrice,
                        notes,
                        eventTimeStr
                );

                // Insert booking details for line item tracking
                bookingDAO.insertBookingDetails(conn, bookingId, serviceId, 1, servicePrice);

                // Create booking-service relationship for provider access
                bookingDAO.insertBookingService(conn, bookingId, providerId, serviceId);

                // Create provider notification
                String providerMessage = String.format("New booking request for \"%s\" by %s (%s). BookingId: %d",
                        serviceName, customerName, phone, bookingId);
                notificationDAO.createNotification(conn, providerId, "PROVIDER", bookingId, providerMessage);

                conn.commit();
                
                // ✅ Instant customer notification via WebSocket
                CustomerSocket.notifyCustomer(
                    customerId,
                    "Your booking request for \"" + serviceName + "\" has been placed successfully.",
                    bookingId
                );

                // Asynchronous email notification to provider
                executor.submit(() -> {
                    try {
                        new EmailService().sendProviderNotification(providerEmail, serviceName, customerName, phone,notes, bookingId, startDateTime, servicePrice);
                        logger.info("Email sent to provider for booking_id: {} at {}", bookingId, LocalDateTime.now());
                    } catch (Exception e) {
                        logger.error("Failed to send email for booking_id {}: {} at {}", bookingId, e.getMessage(), LocalDateTime.now());
                    }
                });

                // Success response to customer
                JsonObject success = new JsonObject();
                success.addProperty("booking_id", bookingId);
                success.addProperty("status", "Pending");
                success.addProperty("message", "Booking request sent to provider");
                response.setStatus(HttpServletResponse.SC_CREATED);
                out.println(GSON.toJson(success));
                logger.info("Booking created successfully for booking_id: {} at {}", bookingId, LocalDateTime.now());

            } catch (SQLException e) {
                logger.error("Database error for booking: {} at {}", e.getMessage(), LocalDateTime.now(), e);
                conn.rollback();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Database error: " + e.getMessage());
                out.println(GSON.toJson(errorResponse));
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("DB connection error: {} at {}", e.getMessage(), LocalDateTime.now(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "DB connection error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        }
    }

    /**
     * Cleanup method to shutdown executor service when servlet is destroyed
     */
    @Override
    public void destroy() {
        executor.shutdown();
    }
}