package shaadisharthi.admin;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;

/**
 * ServiceProviderStatusServlet - Handles service provider approval/rejection workflow
 * 
 * Manages provider status updates with email notifications:
 * - Approve pending providers with welcome email
 * - Reject providers with reason notification
 * - Transaction-safe database operations
 * - SMTP email integration for status notifications
 * 
 * Endpoint: /update-status
 * Method: POST
 * 
 * @category Service Provider Management
 * @security JWT Token required (admin role)
 */
@WebServlet("/update-status")
public class ServiceProviderStatusServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderStatusServlet.class);
    private static final String APP_BASE_URL = ConfigUtil.get("app.base.url", "APP_BASE_URL");
    
    /**
     * Send standardized JSON response
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param json JSON content to send
     * @throws IOException If response writing fails
     */
    private void sendResponse(HttpServletResponse response, int status, JSONObject json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (var writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Validate input parameters for status update
     * 
     * @param providerId Must be numeric and valid
     * @param status Must be 'approved' or 'rejected'
     * @param rejectionReason Required when status is 'rejected'
     * @param response For sending error responses
     * @return true if validation passes, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateInput(String providerId, String status, String rejectionReason, HttpServletResponse response) throws IOException {
        JSONObject responseJson = new JSONObject();
        if (providerId == null || !providerId.matches("\\d+")) {
            responseJson.put("error", "Invalid or missing provider ID");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return false;
        }
        if (status == null || (!status.equals("approved") && !status.equals("rejected"))) {
            responseJson.put("error", "Invalid or missing status. Must be 'approved' or 'rejected'");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return false;
        }
        if (status.equals("rejected") && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
            responseJson.put("error", "Rejection reason is required for status 'rejected'");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return false;
        }
        return true;
    }

    /**
     * Send status notification email to service provider
     * 
     * Uses SMTP with TLS for secure email delivery
     * Sends different email templates for approval vs rejection
     * 
     * @param email Recipient email address
     * @param status New status (approved/rejected)
     * @param rejectionReason Reason for rejection (if applicable)
     * @return true if email sent successfully, false otherwise
     */
    private boolean sendEmail(String email, String status, String rejectionReason) {
    	// Get email credentials from configuration
    	String from = ConfigUtil.get("email.from", "EMAIL_FROM");
    	String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        if (from == null || password == null) {
            logger.error("Missing email credentials in config.properties");
            return false;
        }

        // Configure SMTP properties
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

        // Prepare email content based on status
        String subject;
        String body;
        if (status.equals("approved")) {
            subject = "ShaadiSarthi Application Approved";
            if (APP_BASE_URL == null) {
                logger.error("Application base URL is not configured. Please set app.base.url or APP_BASE_URL. Cannot send approval email.");
                return false;
            }
            String loginLink = APP_BASE_URL + "/provider";
            body = "Dear Service Provider,\n\n" +
                   "We are pleased to inform you that your application to join ShaadiSarthi has been approved!\n" +
                   "Welcome to the board of ShaadiSarthi. You can now log in to your account and start offering your services.\n" +
                   "Login here: " + loginLink + "\n\n" +
                   "Best regards,\nShaadiSarthi Team";
        } else {
            subject = "ShaadiSarthi Application Status Update";
            body = "Dear Service Provider,\n\n" +
                   "We regret to inform you that your application to join ShaadiSarthi has been rejected.\n" +
                   "Reason: " + rejectionReason + "\n\n" +
                   "If you believe this was an error or wish to reapply, please contact our support team.\n" +
                   "Best regards,\nShaadiSarthi Team";
        }

        try {
            // Create and send email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Email sent to {} for status: {}", email, status);
            return true;
        } catch (MessagingException e) {
            logger.error("Failed to send email to {} for status {}: {}", email, status, e.getMessage(), e);
            return false;
        }
    }

    /**
     * POST /update-status - Update service provider approval status
     * 
     * Performs:
     * 1. JWT authentication and validation
     * 2. Input parameter validation
     * 3. Database status update in transaction
     * 4. Email notification to provider
     * 5. Comprehensive error handling and rollback
     * 
     * @param request Contains provider_id, status, and rejection_reason parameters
     * @param response JSON response with operation result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /admin/service-providers/update-status");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Authenticate admin using JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        String adminId;
        try {
            adminId = claims.getSubject();
            logger.debug("Authenticated admin ID: {}", adminId);
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null");
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Invalid token");
            sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
            return;
        }

        // Read request parameters
        String providerId = request.getParameter("provider_id");
        String status = request.getParameter("status");
        String rejectionReason = request.getParameter("rejection_reason");

        // Validate inputs
        if (!validateInput(providerId, status, rejectionReason, response)) {
            return;
        }

        try (Connection conn = DbConnection.getCon()) {
            conn.setAutoCommit(false);
            try {
                // Check if provider exists and get email
                String email = null;
                String checkQuery = "SELECT email FROM service_providers WHERE provider_id = ? AND status = 'pending_approval'";
                try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                    pstmt.setInt(1, Integer.parseInt(providerId));
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            email = rs.getString("email");
                        } else {
                            logger.warn("Provider ID {} not found or not in pending_approval status", providerId);
                            JSONObject responseJson = new JSONObject();
                            responseJson.put("error", "Provider not found or not pending approval");
                            sendResponse(response, HttpServletResponse.SC_NOT_FOUND, responseJson);
                            return;
                        }
                    }
                }

                // Update provider status in database
                String updateQuery = "UPDATE service_providers SET status = ? WHERE provider_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                    pstmt.setString(1, status);
                    pstmt.setInt(2, Integer.parseInt(providerId));
                    int rows = pstmt.executeUpdate();
                    if (rows == 0) {
                        logger.warn("Failed to update status for provider ID {}", providerId);
                        conn.rollback();
                        JSONObject responseJson = new JSONObject();
                        responseJson.put("error", "Failed to update provider status");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                }

                // Send status notification email
                boolean emailSent = sendEmail(email, status, rejectionReason);
                if (!emailSent) {
                    logger.warn("Email sending failed for provider ID {}, status: {}", providerId, status);
                    conn.rollback();
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("message", "Status updated but email sending failed");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }

                // Commit transaction if all operations successful
                conn.commit();
                logger.info("Provider ID {} status updated to {} by admin ID {}", providerId, status, adminId);
                JSONObject responseJson = new JSONObject();
                responseJson.put("message", "Provider status updated successfully");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                // Rollback on any database error
                conn.rollback();
                logger.error("Database error updating provider ID {}: {}", providerId, e.getMessage(), e);
                JSONObject responseJson = new JSONObject();
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            }
        } catch (SQLException e) {
            logger.error("Connection error for provider ID {}: {}", providerId, e.getMessage(), e);
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}