package shaadisharthi.AuditLogger;

/**
 * AuditUtil - Utility wrapper for audit logging system
 * 
 * Provides simplified interface for logging audit events throughout the application
 * Handles exception wrapping and provides consistent logging pattern
 * 
 * @category Audit & Security
 * @see AuditLogging
 */
public class AuditUtil {

    /**
     * Log audit event with comprehensive context information
     * 
     * Wraps AuditLogging.logEvent with exception handling to prevent
     * audit failures from disrupting main application flow
     * 
     * @param adminId Administrator ID performing the action
     * @param action Type of action (e.g., "CREATE", "UPDATE", "DELETE")
     * @param targetId ID of the entity being acted upon
     * @param targetType Type of entity (e.g., "CUSTOMER", "SERVICE_PROVIDER")
     * @param details Detailed description of the action
     * @param reason Justification or business reason for the action
     * @param ip Client IP address for security tracking
     */
    public static void logAudit(String adminId, String action, String targetId, String targetType, String details, String reason, String ip) {
        try {
            // Delegate to async audit logging system
            AuditLogging.logEvent(adminId, action, targetId, targetType, details, reason, ip);
        } catch (RuntimeException e) {
            // Log to stderr but don't throw - maintain application stability
            System.err.printf("Audit log failed: action=%s, adminId=%s, error=%s%n", action, adminId, e.getMessage());
        }
    }
}