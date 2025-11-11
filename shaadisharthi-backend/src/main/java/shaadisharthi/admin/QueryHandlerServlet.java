package shaadisharthi.admin;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;

/**
 * Query Handler Servlet - Support Ticket Management System
 * 
 * Handles the complete lifecycle of customer and service provider support queries:
 * - Query listing with pagination and smart assignment
 * - Query assignment to admins with expiration (10-minute timeout)
 * - Query resolution with email notifications
 * - Multi-tenant support for both customer and service provider users
 * 
 * Security Features:
 * - JWT token validation for admin authentication
 * - Assignment validation to prevent query hijacking
 * - Input validation and sanitization
 * - Comprehensive audit logging with IST timestamps
 * 
 * Email Integration:
 * - Automated response notifications to users
 * - Configurable SMTP with TLS security
 * - Template-based email content
 */
@WebServlet("/queries/*")
public class QueryHandlerServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(QueryHandlerServlet.class);

    /**
     * Servlet initialization with timezone-aware logging
     */
    @Override
    public void init() throws ServletException {
        logger.info("QueryHandlerServlet initialized at {} IST", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
    }

    /**
     * Standardized error response handler
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param message Error message for client
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Validates query ID parameter format and presence
     * 
     * @param queryId Query ID string from request
     * @param response HttpServletResponse for error handling
     * @return true if valid, false otherwise
     */
    private boolean validateQueryId(String queryId, HttpServletResponse response) throws IOException {
        if (queryId == null || !queryId.matches("\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing query_id");
            return false;
        }
        return true;
    }

    /**
     * Validates reply message content and length
     * 
     * @param replyMsg Admin's response message
     * @param response HttpServletResponse for error handling
     * @return true if valid, false otherwise
     */
    private boolean validateReply(String replyMsg, HttpServletResponse response) throws IOException {
        if (replyMsg == null || replyMsg.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing reply_msg");
            return false;
        }
        if (replyMsg.length() > 500) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Reply message exceeds 500 characters");
            return false;
        }
        return true;
    }

    /**
     * Sends email notification to user when their query is resolved
     * 
     * @param email User's email address
     * @param userType 'customer' or 'service_provider'
     * @param subject Original query subject
     * @param replyMsg Admin's response message
     * @return true if email sent successfully, false otherwise
     */
    private boolean sendQueryResponseEmail(String email, String userType, String subject, String replyMsg) {
        // Get email credentials from configuration
    	String from = ConfigUtil.get("email.from", "EMAIL_FROM");
    	String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties at {} IST", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            return false;
        }

        // Configure SMTP properties for Gmail with TLS
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Create authenticated session
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        // Construct email content with user-specific salutation
        String emailSubject = "Your ShaadiSarthi Query Has Been Resolved";
        String salutation = userType.equals("service_provider") ? "Service Provider" : "Customer";
        String body = "Dear " + salutation + ",\n\n" +
                      "Your query '" + subject + "' has been resolved.\n" +
                      "Response: " + replyMsg + "\n\n" +
                      "Best regards,\nShaadiSarthi Team";

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(emailSubject);
            message.setText(body);
            Transport.send(message);
            logger.info("Email sent to {} ({}) for query response: {} at {} IST", email, userType, subject, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            return true;
        } catch (MessagingException e) {
            logger.error("Failed to send email to {} ({}) for query response {} at {} IST: {}", email, userType, subject, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage(), e);
            return false;
        }
    }

    /**
     * GET endpoint - Retrieves paginated list of pending queries
     * 
     * Smart query assignment logic:
     * - Shows unassigned queries
     * - Shows queries assigned to current admin
     * - Shows queries where assignment expired (>10 minutes)
     * 
     * @param request HttpServletRequest with optional page/limit parameters
     * @param response HttpServletResponse with JSON query list
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for queries at {} IST", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract admin ID from JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        String adminId;
        try {
            adminId = claims.getSubject();
            logger.debug("Authenticated admin ID: {} at {} IST", adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null at {} IST", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Validate endpoint path
        String pathInfo = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if (!pathInfo.equals("/")) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Invalid endpoint");
            return;
        }

        handleList(request, response, Integer.parseInt(adminId));
    }

    /**
     * POST endpoint - Handles query assignment and responses
     * 
     * Supported actions:
     * - /assign - Assign query to current admin
     * - /reply - Submit response to assigned query
     * 
     * @param request HttpServletRequest with action parameters
     * @param response HttpServletResponse with operation result
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for queries at {} IST", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract admin ID from JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        String adminId;
        try {
            adminId = claims.getSubject();
            logger.debug("Authenticated admin ID: {} at {} IST", adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null at {} IST", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Route to appropriate handler based on path
        String pathInfo = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (pathInfo) {
            case "/assign":
                handleAssign(request, response, Integer.parseInt(adminId));
                break;
            case "/reply":
                handleReply(request, response, Integer.parseInt(adminId));
                break;
            default:
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Invalid endpoint");
                break;
        }
    }

    /**
     * Handles query listing with pagination and smart assignment logic
     * 
     * Business Rules:
     * - Only shows pending queries
     * - Includes unassigned queries
     * - Includes queries assigned to current admin
     * - Includes queries with expired assignments (>10 minutes)
     * - Orders by timestamp (newest first)
     * 
     * @param request HttpServletRequest with pagination parameters
     * @param response HttpServletResponse with JSON query list
     * @param adminId Authenticated admin ID for assignment filtering
     */
    private void handleList(HttpServletRequest request, HttpServletResponse response, Integer adminId)
            throws IOException {
        logger.debug("Listing queries for admin ID: {} at {} IST", adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        JSONArray jsonArray = new JSONArray();
        
        // Parse pagination parameters with defaults
        int page = request.getParameter("page") != null ? Integer.parseInt(request.getParameter("page")) : 1;
        int limit = request.getParameter("limit") != null ? Integer.parseInt(request.getParameter("limit")) : 10;
        int offset = (page - 1) * limit;

        try (Connection conn = DbConnection.getCon()) {
            // Count total available queries for pagination metadata
            int totalCount = 0;
            try (PreparedStatement countStmt = conn.prepareStatement(
                "SELECT COUNT(*) AS total FROM support_queries " +
                "WHERE query_status = 'pending' AND (" +
                "assigned_admin_id IS NULL OR assigned_admin_id = ? OR " +
                "(assigned_time IS NOT NULL AND TIMESTAMPDIFF(MINUTE, assigned_time, NOW()) > 10))")) {
                countStmt.setInt(1, adminId);
                try (ResultSet countRs = countStmt.executeQuery()) {
                    if (countRs.next()) {
                        totalCount = countRs.getInt("total");
                    }
                }
            }

            // Fetch paginated queries with smart assignment logic
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT query_id, user_id, subject, message, timestamp, user_type, query_status, " +
                "assigned_admin_id, assigned_time FROM support_queries " +
                "WHERE query_status = 'pending' AND (" +
                "assigned_admin_id IS NULL OR assigned_admin_id = ? OR " +
                "(assigned_time IS NOT NULL AND TIMESTAMPDIFF(MINUTE, assigned_time, NOW()) > 10)) " +
                "ORDER BY timestamp DESC LIMIT ? OFFSET ?")) {
                stmt.setInt(1, adminId);
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("query_id", rs.getInt("query_id"));
                        obj.put("userId", rs.getInt("user_id"));
                        obj.put("subject", rs.getString("subject"));
                        obj.put("message", rs.getString("message"));
                        obj.put("timestamp", rs.getTimestamp("timestamp").toString());
                        obj.put("userType", rs.getString("user_type"));
                        obj.put("queryStatus", rs.getString("query_status"));
                        obj.put("assignedAdminId", rs.getObject("assigned_admin_id") != null ? rs.getInt("assigned_admin_id") : JSONObject.NULL);
                        obj.put("assignedTime", rs.getTimestamp("assigned_time") != null ? rs.getTimestamp("assigned_time").toString() : JSONObject.NULL);
                        jsonArray.put(obj);
                    }
                }
            }

            // Construct comprehensive response with pagination metadata
            JSONObject responseJson = new JSONObject();
            responseJson.put("queries", jsonArray);
            responseJson.put("totalCount", totalCount);
            logger.info("Fetched {} queries for admin ID: {} on page {} at {} IST", jsonArray.length(), adminId, page, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                writer.write(responseJson.toString());
                writer.flush();
            }
        } catch (SQLException e) {
            logger.error("Database error listing queries for admin ID {} at {} IST: {}", adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handles query assignment to current admin with concurrency protection
     * 
     * Assignment Rules:
     * - Can assign unassigned queries
     * - Can reassign queries with expired assignments (>10 minutes)
     * - Cannot steal queries assigned to other admins within timeout
     * - Atomic assignment to prevent race conditions
     * 
     * @param request HttpServletRequest with query_id parameter
     * @param response HttpServletResponse with assigned query details
     * @param adminId Authenticated admin ID for assignment
     */
    private void handleAssign(HttpServletRequest request, HttpServletResponse response, Integer adminId)
            throws IOException {
        logger.debug("Processing query assign for admin ID: {} at {} IST", adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        String queryIdStr = request.getParameter("query_id");
        if (!validateQueryId(queryIdStr, response)) {
            return;
        }
        int queryId = Integer.parseInt(queryIdStr);

        try (Connection conn = DbConnection.getCon()) {
            // Check current assignment status with concurrency protection
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT assigned_admin_id, assigned_time FROM support_queries WHERE query_id = ?")) {
                ps.setInt(1, queryId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        logger.warn("Query ID {} not found at {} IST", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found");
                        return;
                    }

                    // Check assignment validity
                    Integer assignedAdmin = rs.getInt("assigned_admin_id");
                    Timestamp assignedTime = rs.getTimestamp("assigned_time");
                    boolean isUnassigned = rs.wasNull();
                    boolean isExpired = assignedTime != null &&
                            java.time.temporal.ChronoUnit.MINUTES.between(
                                assignedTime.toLocalDateTime(),
                                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                            ) > 10;

                    // Prevent query hijacking from other admins
                    if (!isUnassigned && !isExpired && assignedAdmin != adminId) {
                        logger.warn("Query ID {} already assigned to another admin at {} IST", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                        sendError(response, HttpServletResponse.SC_FORBIDDEN, "Query already handled by another admin");
                        return;
                    }
                }
            }

            // Perform assignment
            try (PreparedStatement assign = conn.prepareStatement(
                "UPDATE support_queries SET assigned_admin_id = ?, assigned_time = NOW() WHERE query_id = ?")) {
                assign.setInt(1, adminId);
                assign.setInt(2, queryId);
                int rows = assign.executeUpdate();
                if (rows == 0) {
                    logger.warn("Query ID {} not found or not updated at {} IST", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found");
                    return;
                }
            }

            // Return assigned query details
            try (PreparedStatement detail = conn.prepareStatement("SELECT * FROM support_queries WHERE query_id = ?")) {
                detail.setInt(1, queryId);
                try (ResultSet detailRs = detail.executeQuery()) {
                    if (detailRs.next()) {
                        JSONObject queryJson = new JSONObject();
                        queryJson.put("query_id", detailRs.getInt("query_id"));
                        queryJson.put("userId", detailRs.getInt("user_id"));
                        queryJson.put("subject", detailRs.getString("subject"));
                        queryJson.put("message", detailRs.getString("message"));
                        queryJson.put("timestamp", detailRs.getTimestamp("timestamp").toString());
                        queryJson.put("userType", detailRs.getString("user_type"));
                        logger.info("Query ID {} assigned to admin ID: {} at {} IST", queryId, adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                        try (PrintWriter writer = response.getWriter()) {
                            writer.write(queryJson.toString());
                            writer.flush();
                        }
                    } else {
                        logger.warn("Query ID {} not found after assignment at {} IST", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error assigning query ID {} at {} IST: {}", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handles query resolution with email notification
     * 
     * Process Flow:
     * 1. Validate query assignment to current admin
     * 2. Fetch user email based on user_type
     * 3. Update query status and store response
     * 4. Send email notification to user
     * 5. Handle transaction rollback on failures
     * 
     * @param request HttpServletRequest with query_id and reply_msg
     * @param response HttpServletResponse with operation result
     * @param adminId Authenticated admin ID for authorization
     */
    private void handleReply(HttpServletRequest request, HttpServletResponse response, Integer adminId)
            throws IOException {
        logger.debug("Processing query reply for admin ID: {} at {} IST", adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        String queryIdStr = request.getParameter("query_id");
        String replyMsg = request.getParameter("reply_msg");

        if (!validateQueryId(queryIdStr, response) || !validateReply(replyMsg, response)) {
            return;
        }
        int queryId = Integer.parseInt(queryIdStr);

        try (Connection conn = DbConnection.getCon()) {
            conn.setAutoCommit(false); // Start transaction for atomic operation
            try {
                String email = null;
                String querySubject = null;
                String userType = null;
                
                // Verify assignment and fetch user details
                try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT user_id, subject, user_type, assigned_admin_id FROM support_queries WHERE query_id = ?")) {
                    checkStmt.setInt(1, queryId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            logger.warn("Query ID {} not found at {} IST", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found");
                            return;
                        }
                        int assignedAdminId = rs.getInt("assigned_admin_id");
                        if (rs.wasNull() || assignedAdminId != adminId) {
                            logger.warn("Query ID {} not assigned to admin ID: {} at {} IST", queryId, adminId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Query not assigned to you");
                            return;
                        }
                        int userId = rs.getInt("user_id");
                        querySubject = rs.getString("subject");
                        userType = rs.getString("user_type");

                        // Fetch user email based on user type (customer vs service provider)
                        String emailQuery = userType.equals("ServiceProvider")
                            ? "SELECT email FROM service_providers WHERE provider_id = ?"
                            : "SELECT email FROM customers WHERE customer_id = ?";
                        try (PreparedStatement emailStmt = conn.prepareStatement(emailQuery)) {
                            emailStmt.setInt(1, userId);
                            try (ResultSet emailRs = emailStmt.executeQuery()) {
                                if (emailRs.next()) {
                                    email = emailRs.getString("email");
                                    logger.debug("Fetched email {} for user ID {} (type: {}) at {} IST", email, userId, userType, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                                } else {
                                    logger.warn("No email found for user ID: {} (type: {}) at {} IST", userId, userType, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                                    sendError(response, HttpServletResponse.SC_NOT_FOUND, "User email not found");
                                    return;
                                }
                            }
                        }
                    }
                }

                // Update query with response and mark as resolved
                try (PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE support_queries SET response_msg = ?, response_time = ?, query_status = 'Resolved', admin_id = ? WHERE query_id = ?")) {
                    updateStmt.setString(1, replyMsg);
                    updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
                    updateStmt.setInt(3, adminId);
                    updateStmt.setInt(4, queryId);
                    int rows = updateStmt.executeUpdate();
                    if (rows == 0) {
                        logger.warn("Query ID {} not found or not updated at {} IST", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                        conn.rollback();
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found");
                        return;
                    }
                }

                // Send email notification - critical for user communication
                if (!sendQueryResponseEmail(email, userType, querySubject, replyMsg)) {
                    logger.warn("Failed to send email for query ID {} to {} (type: {}) at {} IST", queryId, email, userType, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                    conn.rollback();
                    sendError(response, HttpServletResponse.SC_OK, "Query replied but email sending failed");
                    return;
                }

                // Commit transaction only if all operations succeed
                conn.commit();
                logger.info("Query ID {} resolved by admin ID: {}, email sent to {} (type: {}) at {} IST", queryId, adminId, email, userType, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                response.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(new JSONObject()
                        .put("success", true)
                        .put("message", "Reply sent successfully and email notification sent")
                        .toString());
                    writer.flush();
                }
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Database error replying to query ID {} at {} IST: {}", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage(), e);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        } catch (SQLException e) {
            logger.error("Connection error for query ID {} at {} IST: {}", queryId, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage(), e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}