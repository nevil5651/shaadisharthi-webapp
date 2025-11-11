package shaadisharthi.utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;

import shaadisharthi.DbConnection.DbConnection;

/**
 * NotificationService - Comprehensive notification management system
 * 
 * Provides complete CRUD operations for user notifications:
 * - Create notifications for providers and customers
 * - Retrieve notifications with pagination
 * - Mark notifications as read
 * - Count unread notifications
 * - Clear notification history
 * 
 * Supports both provider and customer user types with validation
 * 
 * @category Utilities & Business Logic
 */
public class NotificationService {

    // Database table name for notifications
    private static final String NOTIFICATION_TABLE = "notifications";
    // Maximum message length for validation
    private static final int MAX_MESSAGE_LENGTH = 500;
    // Default limit for notification retrieval
    private static final int DEFAULT_LIMIT = 5;

    /**
     * Creates a new notification for a user.
     *
     * @param userId    The ID of the user receiving the notification
     * @param userType  The type of user (e.g., "provider", "customer")
     * @param message   The notification message
     * @param relatedId The ID of the related entity (e.g., booking ID)
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static void createNotification(int userId, String userType, String message, int relatedId)
            throws SQLException {
        validateUserId(userId);
        validateUserType(userType);
        validateMessage(message);

        String sql = String.format(
                "INSERT INTO %s (user_id, user_type, message, related_id, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                NOTIFICATION_TABLE);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userType);
            stmt.setString(3, message);
            stmt.setInt(4, relatedId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to create notification, no rows affected");
            }
        }
    }

    /**
     * Retrieves the most recent notifications for a user, limited to a default number.
     *
     * @param userId   The ID of the user
     * @param userType The type of user
     * @return List of notifications
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static List<Notification> getNotifications(int userId, String userType) throws SQLException {
        return getNotifications(userId, userType, DEFAULT_LIMIT);
    }

    /**
     * Retrieves notifications for a user with a specified limit.
     *
     * @param userId   The ID of the user
     * @param userType The type of user
     * @param limit    The maximum number of notifications to retrieve
     * @return List of notifications
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static List<Notification> getNotifications(int userId, String userType, int limit) throws SQLException {
        validateUserId(userId);
        validateUserType(userType);
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        List<Notification> notifications = new ArrayList<>();
        String sql = String.format(
                "SELECT * FROM %s WHERE user_id = ? AND user_type = ? ORDER BY created_at DESC LIMIT ?",
                NOTIFICATION_TABLE);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userType);
            stmt.setInt(3, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        }
        return notifications;
    }

    /**
     * Retrieves all notifications for a user.
     *
     * @param userId   The ID of the user
     * @param userType The type of user
     * @return List of all notifications
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static List<Notification> getAllNotifications(int userId, String userType) throws SQLException {
        validateUserId(userId);
        validateUserType(userType);

        List<Notification> notifications = new ArrayList<>();
        String sql = String.format(
                "SELECT * FROM %s WHERE user_id = ? AND user_type = ? ORDER BY created_at DESC",
                NOTIFICATION_TABLE);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        }
        return notifications;
    }

    /**
     * Retrieves the count of unread notifications for a user.
     *
     * @param userId   The ID of the user
     * @param userType The type of user
     * @return The number of unread notifications
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static int getUnreadCount(int userId, String userType) throws SQLException {
        validateUserId(userId);
        validateUserType(userType);

        String sql = String.format(
                "SELECT COUNT(*) AS count FROM %s WHERE user_id = ? AND user_type = ? AND is_read = FALSE",
                NOTIFICATION_TABLE);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userType);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        }
    }

    /**
     * Marks a single notification as read.
     *
     * @param notificationId The ID of the notification
     * @param userId        The ID of the user
     * @param userType      The type of user
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static void markNotificationAsRead(int notificationId, int userId, String userType) throws SQLException {
        validateNotificationId(notificationId);
        validateUserId(userId);
        validateUserType(userType);

        String sql = String.format(
                "UPDATE %s SET is_read = TRUE WHERE notification_id = ? AND user_id = ? AND user_type = ?",
                NOTIFICATION_TABLE);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            stmt.setInt(2, userId);
            stmt.setString(3, userType);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("No notification found or already marked as read");
            }
        }
    }

    /**
     * Marks all notifications as read for a user.
     *
     * @param userId   The ID of the user
     * @param userType The type of user
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static void markAllAsRead(int userId, String userType) throws SQLException {
        validateUserId(userId);
        validateUserType(userType);

        String sql = String.format(
                "UPDATE %s SET is_read = TRUE WHERE user_id = ? AND user_type = ?",
                NOTIFICATION_TABLE);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userType);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes all notifications for a user.
     *
     * @param userId   The ID of the user
     * @param userType The type of user
     * @throws IllegalArgumentException If input parameters are invalid
     * @throws SQLException            If a database error occurs
     */
    public static void clearNotifications(int userId, String userType) throws SQLException {
        validateUserId(userId);
        validateUserType(userType);

        String sql = String.format(
                "DELETE FROM %s WHERE user_id = ? AND user_type = ?",
                NOTIFICATION_TABLE);

        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, userType);
            stmt.executeUpdate();
        }
    }

    // Validation methods

    private static void validateUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: must be positive");
        }
    }

    private static void validateUserType(String userType) {
        if (userType == null || userType.trim().isEmpty() || !userType.matches("^(provider|customer)$")) {
            throw new IllegalArgumentException("Invalid user type: must be 'provider' or 'customer'");
        }
    }

    private static void validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message cannot be empty");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Notification message exceeds maximum length of " + MAX_MESSAGE_LENGTH + " characters");
        }
    }

    private static void validateNotificationId(int notificationId) {
        if (notificationId <= 0) {
            throw new IllegalArgumentException("Invalid notification ID: must be positive");
        }
    }

    /**
     * Maps ResultSet row to Notification object
     * 
     * @param rs ResultSet containing notification data
     * @return Notification object
     * @throws SQLException If database access fails
     */
    private static Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setNotificationId(rs.getInt("notification_id"));
        notification.setMessage(rs.getString("message"));
        notification.setCreatedAt(rs.getTimestamp("created_at"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setUserType(rs.getString("user_type"));
        return notification;
    }

    /**
     * Inner class to represent a Notification entity.
     */
    public static class Notification {
        private int notificationId;
        private String message;
        private Timestamp createdAt;
        private boolean isRead;
        private String userType;

        public int getNotificationId() {
            return notificationId;
        }

        public void setNotificationId(int notificationId) {
            this.notificationId = notificationId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Timestamp getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Timestamp createdAt) {
            this.createdAt = createdAt;
        }

        public boolean isRead() {
            return isRead;
        }

        public void setRead(boolean read) {
            isRead = read;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Notification that = (Notification) o;
            return notificationId == that.notificationId &&
                   isRead == that.isRead &&
                   Objects.equals(message, that.message) &&
                   Objects.equals(createdAt, that.createdAt) &&
                   Objects.equals(userType, that.userType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(notificationId, message, createdAt, isRead, userType);
        }
    }
}