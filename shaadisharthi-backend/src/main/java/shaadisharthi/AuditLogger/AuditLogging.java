package shaadisharthi.AuditLogger;

import shaadisharthi.DbConnection.DbConnection;

import java.sql.*;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuditLogging - Asynchronous audit logging system for comprehensive activity tracking
 * 
 * Features:
 * - Background thread execution for non-blocking logging
 * - Comprehensive audit trail with timestamps and IP addresses
 * - Support for various action types and target entities
 * - Automatic connection management with HikariCP
 * - Error resilience with fail-silent behavior
 * 
 * Logs administrative actions for security compliance and operational monitoring
 * 
 * @category Audit & Security
 * @threading Uses single-threaded executor for sequential log writing
 */
public class AuditLogging {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogging.class);
	   
    // Background thread pool for async logging - single thread ensures log entry ordering
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Log audit event asynchronously - primary entry point from AuditUtil
     * 
     * Submits log entry to background thread pool for non-blocking execution
     * 
     * @param adminId Administrator ID performing the action (nullable)
     * @param action Type of action performed (e.g., "LOGIN", "UPDATE", "DELETE")
     * @param targetId ID of the target entity being acted upon
     * @param targetType Type of target entity (e.g., "CUSTOMER", "PROVIDER", "BOOKING")
     * @param details Additional context or description of the action
     * @param reason Justification or reason for the action
     * @param ip IP address of the request origin
     */
    public static void logEvent(String adminId,
                                String action,
                                String targetId,
                                String targetType,
                                String details,
                                String reason,
                                String ip) {

        // Submit task to background thread for asynchronous processing
        executor.submit(() -> {
            insertAuditLog(adminId, action, targetId, targetType, details, reason, ip);
        });
    }

    /**
     * Actual database insertion logic - runs in background thread
     * 
     * @param adminId Administrator ID (nullable)
     * @param action Action type
     * @param targetId Target entity ID
     * @param targetType Target entity type
     * @param details Action details
     * @param reason Action justification
     * @param ip Request IP address
     */
    private static void insertAuditLog(String adminId,
                                       String action,
                                       String targetId,
                                       String targetType,
                                       String details,
                                       String reason,
                                       String ip) {

        // SQL insert statement for audit_logs table
        String sql = "INSERT INTO audit_logs (actor_id, action, target_id, target_type, details, reason, timestamp, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DbConnection.getCon();
            con.setAutoCommit(true); // Always commit immediately for audit integrity

            ps = con.prepareStatement(sql);

            // Set actor_id (admin ID) - handle null values
            if (adminId == null || adminId.trim().isEmpty()) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, Integer.parseInt(adminId));
            }

            // Set remaining parameters
            ps.setString(2, action);
            ps.setString(3, targetId);
            ps.setString(4, targetType);
            ps.setString(5, details);
            ps.setString(6, reason);
            ps.setTimestamp(7, Timestamp.from(Instant.now())); // Current timestamp
            ps.setString(8, ip);

            ps.executeUpdate();

        } catch (Exception e) {
            // Log error but don't throw - fail silently to avoid disrupting main operations
            logger.error("[AsyncAuditLog] Failed: "+ e.getMessage());
            
        } finally {
            // Ensure proper resource cleanup
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            try { if (con != null) con.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Gracefully shutdown the audit logging system
     * 
     * Should be called during application shutdown to release thread resources
     */
    public static void shutdown() {
        executor.shutdown();
    }
}