package shaadisarthi.booking;

import java.sql.*;

/**
 * DATA ACCESS OBJECT: NotificationDAO
 * 
 * PURPOSE: Handles database operations for notification system
 * Simple CRUD operations for:
 * - Creating notifications for providers and customers
 * - Tracking notification status and creation timestamps
 * - Supporting real-time notification features
 * 
 * ARCHITECTURE: Data Access Object pattern
 * INTEGRATION: Used by booking system for event notifications
 * 
 * @author ShaadiSarthi Team
 * @version 1.0
 * @since 2024
 */
public class NotificationDAO {
    
    /**
     * Creates a new notification record in the database
     * 
     * @param conn Database connection
     * @param receiverId ID of the user receiving notification
     * @param type Type of notification (PROVIDER/CUSTOMER)
     * @param bookingId Associated booking ID
     * @param message Notification message content
     * @return Generated notification ID
     * @throws SQLException if insert operation fails
     */
    public int createNotification(Connection conn, int receiverId, String type, int bookingId, String message) throws SQLException {
        String sql = "INSERT INTO notifications (receiver_id, type, booking_id, message, status, created_at) VALUES (?, ?, ?, ?, 'UNREAD', NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, receiverId);
            ps.setString(2, type);
            ps.setInt(3, bookingId);
            ps.setString(4, message);
            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Creating notification failed, no rows affected.");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Creating notification failed, no ID obtained.");
    }
}