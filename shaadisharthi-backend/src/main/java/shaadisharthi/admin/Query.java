package shaadisharthi.admin;

import java.sql.Timestamp;

/**
 * Query Data Model Class
 * 
 * Represents a support query/ticket in the system with comprehensive tracking
 * Used for customer and service provider support requests with admin response tracking
 * 
 * Fields track:
 * - Query creation and assignment details
 * - Response history and timing
 * - Escalation status and admin assignment
 * - User context (customer vs service provider)
 */
public class Query {
    private int queryId;
    private int userId;
    private String userType;          // 'customer' or 'service_provider'
    private int adminId;              // Admin who responded
    private String subject;
    private String message;
    private Timestamp timestamp;      // When query was created
    private String queryStatus;       // 'pending', 'resolved', 'escalated'
    private String responseMsg;       // Admin's response
    private Timestamp responseTime;   // When admin responded
    private boolean escalated;        // Whether query was escalated
    private int assignedAdminId;      // Admin currently assigned
    private Timestamp assignedTime;   // When assigned to admin

    // Comprehensive getters and setters for all fields
    public int getQueryId() { return queryId; }
    public void setQueryId(int queryId) { this.queryId = queryId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getQueryStatus() { return queryStatus; }
    public void setQueryStatus(String queryStatus) { this.queryStatus = queryStatus; }

    public String getResponseMsg() { return responseMsg; }
    public void setResponseMsg(String responseMsg) { this.responseMsg = responseMsg; }

    public Timestamp getResponseTime() { return responseTime; }
    public void setResponseTime(Timestamp responseTime) { this.responseTime = responseTime; }

    public boolean isEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }

    public int getAssignedAdminId() { return assignedAdminId; }
    public void setAssignedAdminId(int assignedAdminId) { this.assignedAdminId = assignedAdminId; }

    public Timestamp getAssignedTime() { return assignedTime; }
    public void setAssignedTime(Timestamp assignedTime) { this.assignedTime = assignedTime; }
}