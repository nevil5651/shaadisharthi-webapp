package shaadisharthi.admin;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;

import javax.mail.*;
import javax.mail.internet.*;

/**
 * Guest Query Handler Servlet - Support System for Non-Registered Users
 * 
 * Handles support queries from guests (non-authenticated users) who haven't registered on the platform.
 * Provides similar functionality to QueryHandlerServlet but for guest users without accounts.
 * 
 * Key Features:
 * - Guest query management with assignment logic
 * - Email responses to guest users
 * - Assignment timeout protection (10-minute expiration)
 * - Admin authentication and authorization
 * 
 * Differences from QueryHandlerServlet:
 * - Guests don't have user accounts, so simpler user management
 * - No user_type differentiation (all guests treated equally)
 * - Direct email communication without user profiles
 * 
 * Security: Uses JWT for admin authentication, same assignment protection rules
 */
@WebServlet("/GuestQueryHandler/*")
public class GuestQueryHandlerServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(GuestQueryHandlerServlet.class);

    /**
     * GET endpoint - Retrieves paginated list of pending guest queries
     * 
     * Smart assignment logic similar to registered user queries:
     * - Shows unassigned guest queries
     * - Shows queries assigned to current admin
     * - Shows queries with expired assignments (>10 minutes)
     * - Orders by creation date (newest first)
     * 
     * @param request HttpServletRequest with optional page/limit parameters
     * @param response HttpServletResponse with JSON query list
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Fetching guest queries at {}", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        logger.info("Sending the queries");

        // Extract and validate admin authentication from JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        String adminIdStr;
        try {
            adminIdStr = claims.getSubject();
            logger.debug("Authenticated admin ID: {} at {}", adminIdStr, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null at {}", LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        int adminId = Integer.parseInt(adminIdStr);

        // Parse pagination parameters with safe defaults
        int page = parseIntOrDefault(request.getParameter("page"), 1);
        int limit = parseIntOrDefault(request.getParameter("limit"), 10);
        int offset = (page - 1) * limit;

        JSONObject resultJson = new JSONObject();
        JSONArray queryArray = new JSONArray();

        // ✅ Smart assignment query: includes unassigned, expired, and self-assigned queries
        String sql = "SELECT id, name, subject, message, created_at " +
                     "FROM guest_queries " +
                     "WHERE status = 'PENDING' AND " +
                     "(assigned_admin_id IS NULL " +
                     " OR assigned_at < (NOW() - INTERVAL 10 MINUTE) " +
                     " OR assigned_admin_id = ?) " +
                     "ORDER BY created_at DESC LIMIT ? OFFSET ?";

        String countSql = "SELECT COUNT(*) FROM guest_queries " +
                          "WHERE status = 'PENDING' AND " +
                          "(assigned_admin_id IS NULL " +
                          " OR assigned_at < (NOW() - INTERVAL 10 MINUTE) " +
                          " OR assigned_admin_id = ?)";

        try (Connection con = DbConnection.getCon();
             PreparedStatement ps = con.prepareStatement(sql);
             PreparedStatement countPs = con.prepareStatement(countSql)) {

            // Set parameters for main query
            ps.setInt(1, adminId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            // Execute query and build JSON response
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject queryJson = new JSONObject();
                queryJson.put("id", rs.getInt("id"));
                queryJson.put("name", rs.getString("name"));
                queryJson.put("subject", rs.getString("subject"));
                queryJson.put("message", rs.getString("message"));
                queryJson.put("created_at", rs.getTimestamp("created_at").toString());
                queryArray.put(queryJson);
            }

            // Get total count for pagination metadata
            countPs.setInt(1, adminId);
            ResultSet countRs = countPs.executeQuery();
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }

            // Construct final response
            resultJson.put("queries", queryArray);
            resultJson.put("totalCount", totalCount);

            try (PrintWriter out = response.getWriter()) {
                out.print(resultJson.toString());
                out.flush();
            }

        } catch (SQLException e) {
            logger.error("Database error in GET guest queries: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    /**
     * POST endpoint - Handles guest query assignment and responses
     * 
     * Supported actions:
     * - "assign" - Assign guest query to current admin
     * - "reply" - Submit response to assigned guest query
     * 
     * @param request HttpServletRequest with action and required parameters
     * @param response HttpServletResponse with operation result
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Validate required action parameter
        String action = request.getParameter("action");
        if (action == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Action parameter is required");
            return;
        }
        
        // Extract and validate admin authentication
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

        // Route to appropriate handler based on action
        switch (action) {
            case "assign":
                handleAssign(request, response, Integer.parseInt(adminId));
                break;
            case "reply":
                handleReply(request, response, Integer.parseInt(adminId));
                break;
            default:
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
        }
    }

    /**
     * Handles guest query assignment with concurrency protection
     * 
     * Assignment Rules:
     * - Can assign unassigned queries
     * - Can reassign queries with expired assignments (>10 minutes)
     * - Cannot steal queries assigned to other admins within timeout
     * - Allows reopening queries already assigned to same admin
     * 
     * @param request HttpServletRequest with query_id parameter
     * @param response HttpServletResponse with assigned query details
     * @param adminId Authenticated admin ID for assignment
     */
    private void handleAssign(HttpServletRequest request, HttpServletResponse response, Integer adminId) throws IOException {
        int queryId = parseIntOrDefault(request.getParameter("query_id"), -1);

        // Validate input parameters
        if (queryId <= 0 || adminId <= 0) {
            logger.info("Admin {} and Query id {}", adminId, queryId);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid query_id or admin_id");
            return;
        }

        try (Connection conn = DbConnection.getCon()) {
            // Step 1: Check current assignment status
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT assigned_admin_id, assigned_at FROM guest_queries WHERE id = ?")) {
                ps.setInt(1, queryId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found");
                        return;
                    }

                    // Analyze current assignment state
                    Integer assignedAdmin = rs.getInt("assigned_admin_id");
                    boolean isAssignedNull = rs.wasNull(); // Check if NULL in database
                    Timestamp assignedAt = rs.getTimestamp("assigned_at");

                    // Calculate if assignment has expired (10-minute timeout)
                    boolean isExpired = assignedAt != null &&
                            java.time.temporal.ChronoUnit.MINUTES.between(
                                assignedAt.toLocalDateTime(),
                                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                            ) > 10;

                    // Prevent query hijacking from other admins
                    if (!isAssignedNull && !isExpired && assignedAdmin != adminId) {
                        sendError(response, HttpServletResponse.SC_FORBIDDEN, "Query already assigned to another admin");
                        return;
                    }

                    // If already assigned to same admin and not expired → allow reopening without reassignment
                    if (!isAssignedNull && assignedAdmin == adminId && !isExpired) {
                        logger.info("Admin {} reopening already assigned query {}", adminId, queryId);
                    } else {
                        // Otherwise (unassigned or expired), assign to this admin
                        try (PreparedStatement assign = conn.prepareStatement(
                            "UPDATE guest_queries SET assigned_admin_id = ?, assigned_at = NOW() WHERE id = ?")) {
                            assign.setInt(1, adminId);
                            assign.setInt(2, queryId);
                            assign.executeUpdate();
                            logger.info("Admin {} newly assigned query {}", adminId, queryId);
                        }
                    }
                }
            }

            // Step 2: Return full query details for admin to respond
            try (PreparedStatement detail = conn.prepareStatement(
                "SELECT id, name, email, subject, message, created_at FROM guest_queries WHERE id = ?")) {
                detail.setInt(1, queryId);
                try (ResultSet detailRs = detail.executeQuery()) {
                    if (detailRs.next()) {
                        JSONObject queryJson = new JSONObject();
                        queryJson.put("id", detailRs.getInt("id"));
                        queryJson.put("name", detailRs.getString("name"));
                        queryJson.put("email", detailRs.getString("email")); // Critical for email response
                        queryJson.put("subject", detailRs.getString("subject"));
                        queryJson.put("message", detailRs.getString("message"));
                        queryJson.put("created_at", detailRs.getTimestamp("created_at").toString());

                        try (PrintWriter out = response.getWriter()) {
                            out.print(queryJson.toString());
                            out.flush();
                        }
                    } else {
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found after assignment");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error assigning guest query: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    /**
     * Handles guest query resolution with email notification
     * 
     * Process Flow:
     * 1. Validate query assignment to current admin
     * 2. Send email response to guest user
     * 3. Update query status to 'REPLIED' in database
     * 
     * Note: Unlike registered users, guests don't have accounts to check responses in-app
     * 
     * @param request HttpServletRequest with query_id and reply_msg
     * @param response HttpServletResponse with operation result
     * @param adminId Authenticated admin ID for authorization
     */
    
    private void handleReply(HttpServletRequest request, HttpServletResponse response, Integer adminId) throws IOException {
        int queryId = parseIntOrDefault(request.getParameter("query_id"), -1);
        String replyMsg = request.getParameter("reply_msg");

        // Comprehensive input validation
        if (queryId <= 0 || adminId <= 0 || replyMsg == null || replyMsg.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid input for reply");
            return;
        }

        // SQL queries for verification and update
        String selectSql = "SELECT email, subject, assigned_admin_id FROM guest_queries WHERE id = ? AND status = 'PENDING'";
        String updateSql = "UPDATE guest_queries SET reply_message = ?, status = 'REPLIED', replied_by_admin_id = ?, replied_at = NOW() WHERE id = ?";

        try (Connection con = DbConnection.getCon();
             PreparedStatement selectPs = con.prepareStatement(selectSql);
             PreparedStatement updatePs = con.prepareStatement(updateSql)) {

            // Verify query exists and is assigned to current admin
            selectPs.setInt(1, queryId);
            ResultSet rs = selectPs.executeQuery();

            if (!rs.next()) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Query not found or already replied");
                return;
            }

            int assignedAdminId = rs.getInt("assigned_admin_id");
            if (assignedAdminId != adminId) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "You are not assigned to this query");
                return;
            }

            // Extract guest details for email communication
            String email = rs.getString("email");
            String subject = rs.getString("subject");

            // Send email notification to guest
            sendEmail(email, subject, replyMsg);

            // Update database with response and status change
            updatePs.setString(1, replyMsg);
            updatePs.setInt(2, adminId);
            updatePs.setInt(3, queryId);
            updatePs.executeUpdate();

            // Return success response
            JSONObject respJson = new JSONObject();
            respJson.put("success", true);
            respJson.put("message", "Reply sent successfully");
            try (PrintWriter out = response.getWriter()) {
                out.print(respJson.toString());
                out.flush();
            }

        } catch (SQLException e) {
            logger.error("Database error replying to guest query: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to send email");
        }
    }

    /**
     * Sends email notification to guest user when their query is resolved
     * 
     * @param email Guest's email address
     * @param subject Original query subject
     * @param replyMsg Admin's response message
     * @return true if email sent successfully, false otherwise
     */
    private boolean sendEmail(String email, String subject, String replyMsg) {
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

        // Construct email content for guest users
        String emailSubject = "Your ShaadiSarthi Query Has Been Resolved";
        String salutation = "Guest User";
        String body = "Dear " + salutation + ",\n\n" +
                      "Your query with Subject '" + subject + "' has been resolved.\n" +
                      "Response: " + replyMsg + "\n\n" +
                      "Best regards,\nShaadiSarthi Team";

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(emailSubject);
            message.setText(body);
            Transport.send(message);
            logger.info("Email sent to {} ({}) for query response: {} at {} IST", email, subject, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            return true;
        } catch (MessagingException e) {
            logger.error("Failed to send email to {} ({}) for query response {} at {} IST: {}", email, subject, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Safe integer parsing with default value fallback
     * 
     * @param val String value to parse
     * @param def Default value if parsing fails
     * @return Parsed integer or default value
     */
    private int parseIntOrDefault(String val, int def) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }
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
        JSONObject json = new JSONObject();
        json.put("success", false);
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }
}