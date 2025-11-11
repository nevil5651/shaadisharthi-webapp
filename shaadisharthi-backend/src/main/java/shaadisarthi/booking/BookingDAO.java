package shaadisarthi.booking;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DATA ACCESS OBJECT: BookingDAO
 * 
 * PURPOSE: Centralized data access layer for all booking-related database operations
 * Provides comprehensive CRUD operations for booking management across multiple tables:
 * - bookings, booking_details, booking_services table operations
 * - Provider and customer relationship validation
 * - Booking information retrieval for notifications
 * 
 * ARCHITECTURE: Data Access Object pattern with connection parameterization
 * REUSABILITY: Used by multiple servlets for consistent database interactions
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
public class BookingDAO {
    
    /**
     * Retrieves provider information for a given service ID
     * Used for notification routing and authorization
     * 
     * @param conn Database connection
     * @param serviceId Service ID to lookup
     * @return Map containing providerId and email, or null if not found
     * @throws SQLException if database query fails
     */
    public Map<String, Object> findProviderInfoByServiceId(Connection conn, int serviceId) throws SQLException {
        String sql = "SELECT s.provider_id, sp.email " +
                     "FROM services s " +
                     "JOIN service_providers sp ON s.provider_id = sp.provider_id " +
                     "WHERE s.service_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("providerId", rs.getInt("provider_id"));
                    info.put("email", rs.getString("email"));
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Creates a new booking record in the database
     * 
     * @param conn Database connection
     * @param customerId ID of the customer making booking
     * @param serviceId Service being booked
     * @param eventAddress Venue/location of the event
     * @param eventStart Service start timestamp
     * @param eventEnd Service end timestamp
     * @param totalAmount Total booking amount
     * @param notes Additional customer notes
     * @param eventTime Event time string
     * @return Generated booking_id for the new booking
     * @throws SQLException if insert operation fails
     */
    public int createBooking(Connection conn, int customerId, int serviceId, String eventAddress, 
                           Timestamp eventStart, Timestamp eventEnd, double totalAmount, 
                           String notes, String eventTime) throws SQLException {
        String sql = "INSERT INTO bookings (customer_id, service_id, event_address, event_start_date, event_end_date, booking_date, status, total_amount, total_services, notes, event_time) " +
                     "VALUES (?, ?, ?, ?, ?, NOW(), 'Pending', ?, 1, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, serviceId);
            ps.setString(3, eventAddress);
            ps.setTimestamp(4, eventStart);
            ps.setTimestamp(5, eventEnd);
            ps.setDouble(6, totalAmount);
            ps.setString(7, notes);
            ps.setString(8, eventTime);
            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Creating booking failed, no rows affected.");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Creating booking failed, no ID obtained.");
    }

    /**
     * Inserts booking details record for service line items
     * 
     * @param conn Database connection
     * @param bookingId Parent booking ID
     * @param serviceId Service ID being booked
     * @param quantity Number of service units (default 1)
     * @param price Unit price of the service
     * @throws SQLException if insert operation fails
     */
    public void insertBookingDetails(Connection conn, int bookingId, int serviceId, int quantity, double price) throws SQLException {
        String sql = "INSERT INTO booking_details (booking_id, service_id, quantity, price, status) VALUES (?, ?, ?, ?, 'Pending')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, serviceId);
            ps.setInt(3, quantity);
            ps.setDouble(4, price);
            ps.executeUpdate();
        }
    }

    /**
     * Creates booking-service relationship for provider access
     * 
     * @param conn Database connection
     * @param bookingId Booking ID
     * @param providerId Service provider ID
     * @param serviceId Service ID
     * @throws SQLException if insert operation fails
     */
    public void insertBookingService(Connection conn, int bookingId, int providerId, int serviceId) throws SQLException {
        String sql = "INSERT INTO booking_services (booking_id, provider_id, service_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, providerId);
            ps.setInt(3, serviceId);
            ps.executeUpdate();
        }
    }

    /**
     * Validates if provider owns/is associated with the booking
     * Security measure to prevent unauthorized access
     * 
     * @param conn Database connection
     * @param bookingId Booking ID to validate
     * @param providerId Provider ID to check
     * @return true if provider is associated with booking
     * @throws SQLException if database query fails
     */
    public boolean isProviderOwner(Connection conn, int bookingId, int providerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM booking_services WHERE booking_id = ? AND provider_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Retrieves customer email for a booking
     * Used for notification and communication
     * 
     * @param conn Database connection
     * @param bookingId Booking ID
     * @return Customer email address or null if not found
     * @throws SQLException if database query fails
     */
    public String getCustomerEmail(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT c.email FROM bookings b JOIN customers c ON b.customer_id = c.customer_id WHERE b.booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("email") : null;
            }
        }
    }

    /**
     * Retrieves service name for a booking
     * Used in notifications and UI display
     * 
     * @param conn Database connection
     * @param bookingId Booking ID
     * @return Service name or "Unknown Service" if not found
     * @throws SQLException if database query fails
     */
    public String getServiceName(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT s.service_name FROM bookings b JOIN services s ON b.service_id = s.service_id WHERE b.booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("service_name") : "Unknown Service";
            }
        }
    }

    /**
     * Retrieves event start date for a booking
     * Used in notifications and scheduling
     * 
     * @param conn Database connection
     * @param bookingId Booking ID
     * @return Event start datetime or current time if not found
     * @throws SQLException if database query fails
     */
    public LocalDateTime getEventStartDate(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT event_start_date FROM bookings WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp("event_start_date").toLocalDateTime() : LocalDateTime.now();
            }
        }
    }

    /**
     * Retrieves total amount for a booking
     * Used in notifications and financial tracking
     * 
     * @param conn Database connection
     * @param bookingId Booking ID
     * @return Total booking amount or 0.0 if not found
     * @throws SQLException if database query fails
     */
    public double getTotalAmount(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT total_amount FROM bookings WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total_amount") : 0.0;
            }
        }
    }
    
    /**
     * Validates if customer owns the booking
     * Security measure for customer actions
     * 
     * @param conn Database connection
     * @param bookingId Booking ID to validate
     * @param customerId Customer ID to check
     * @return true if customer owns the booking
     * @throws SQLException if database query fails
     */
    public boolean isCustomerOwner(Connection conn, int bookingId, int customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE booking_id = ? AND customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Retrieves provider email for a booking
     * Used for customer-initiated cancellation notifications
     * 
     * @param conn Database connection
     * @param bookingId Booking ID
     * @return Provider email address or null if not found
     * @throws SQLException if database query fails
     */
    public String getProviderEmail(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT sp.email FROM booking_services bs JOIN service_providers sp ON bs.provider_id = sp.provider_id WHERE bs.booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("email") : null;
            }
        }
    }
    
    /**
     * Retrieves booking details status
     * Used for status validation in customer actions
     * 
     * @param conn Database connection
     * @param bookingId Booking ID
     * @return Current status from booking_details table
     * @throws SQLException if database query fails
     */
    public String getBookingDetailsStatus(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT status FROM booking_details WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }
}