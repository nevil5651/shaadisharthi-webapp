package shaadisharthi.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.utils.ConfigUtil;

/**
 * ForgotPassword - Service provider password reset initiation servlet
 * 
 * Handles secure password reset workflow:
 * - Rate limiting to prevent abuse (5 attempts per hour per email)
 * - JWT token generation for secure reset links
 * - Email delivery with reset instructions
 * - Security-conscious responses (generic messages for privacy)
 * - Database transaction safety
 * 
 * Endpoint: /api/forgot-password
 * Method: POST
 * 
 * @category Authentication & Security
 */
@WebServlet("/api/forgot-password")
public class ForgotPassword extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPassword.class);
    // Token expiration time (1 hour)
    private static final long TOKEN_EXPIRY_MS = 60 * 60 * 1000; // 1 hour
    // Rate limiting: maximum reset attempts per hour
    private static final String APP_BASE_URL = ConfigUtil.get("app.base.url", "APP_BASE_URL");
    private static final int MAX_ATTEMPTS_PER_HOUR = 5;

    /**
     * Send standardized JSON response
     * 
     * @param response HTTP response
     * @param status HTTP status code
     * @param json JSON content
     * @throws IOException If response writing fails
     */
    private void sendResponse(HttpServletResponse response, int status, JSONObject json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Check if email is rate limited for password reset attempts
     * 
     * @param con Database connection
     * @param email User email address
     * @param ipAddress Client IP for logging
     * @return true if rate limited, false otherwise
     * @throws SQLException If database query fails
     */
    private boolean isRateLimited(Connection con, String email, String ipAddress) throws SQLException {
        String query = "SELECT COUNT(*) FROM reset_attempts WHERE email = ? AND attempt_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.debug("Reset attempts for email {} in last hour: {}", email, count);
                    return count >= MAX_ATTEMPTS_PER_HOUR;
                }
            }
        }
        return false;
    }

    /**
     * Log password reset attempt for rate limiting and audit
     * 
     * @param con Database connection
     * @param email User email address
     * @param ipAddress Client IP address
     * @throws SQLException If database insert fails
     */
    private void logAttempt(Connection con, String email, String ipAddress) throws SQLException {
        String query = "INSERT INTO reset_attempts (email, ip_address) VALUES (?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, ipAddress);
            pstmt.executeUpdate();
            logger.debug("Logged reset attempt for email: {}, IP: {}", email, ipAddress);
        }
    }

    /**
     * POST /api/forgot-password - Initiate password reset process
     * 
     * Process flow:
     * 1. Validate email format
     * 2. Check rate limiting
     * 3. Verify email exists in service_providers table
     * 4. Generate secure JWT reset token
     * 5. Store token in database with expiration
     * 6. Send reset email with secure link
     * 7. Return generic response for security
     * 
     * @param request HTTP request with email parameter
     * @param response JSON response with operation result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /api/forgot-password");
        String email = request.getParameter("email");
        JSONObject responseJson = new JSONObject();

        // Validate email format
        if (email == null || email.trim().isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            logger.warn("Invalid email format received: {}", email);
            // Return generic response for security (don't reveal if email exists)
            responseJson.put("message", "If the email is registered, a reset link has been sent");
            sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            return;
        }

        String ipAddress = request.getRemoteAddr();
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false);
            try {
                // Check rate limiting
                if (isRateLimited(con, email, ipAddress)) {
                    logger.warn("Rate limit exceeded for email: {}, IP: {}", email, ipAddress);
                    responseJson.put("error", "Too many reset attempts. Please try again later.");
                    sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
                    return;
                }

                // Check if email exists in service_providers table
                String query = "SELECT provider_id FROM service_providers WHERE email = ?";
                try (PreparedStatement pstmt = con.prepareStatement(query)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            logger.info("No service provider found for email: {}. Returning generic response.", email);
                            logAttempt(con, email, ipAddress);
                            con.commit();
                            // Generic response for security (don't reveal if email exists)
                            responseJson.put("message", "If the email is registered, a reset link has been sent");
                            sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                            return;
                        }
                    }
                }

                // Decode Base64 JWT reset secret from configuration
                String jwtResetSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
                if (jwtResetSecretBase64 == null) {
                    logger.error("Missing JWT reset secret in environment variables or config.properties");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }
                byte[] jwtResetSecret;
                try {
                    jwtResetSecret = Base64.getDecoder().decode(jwtResetSecretBase64);
                    if (jwtResetSecret.length < 32) {
                        logger.error("JWT reset secret is too short after decoding");
                        responseJson.put("error", "Server configuration error");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid Base64-encoded JWT reset secret: {}", e.getMessage(), e);
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }

                // Generate secure JWT reset token
                String token = Jwts.builder()
                        .setSubject(email)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MS))
                        .signWith(Keys.hmacShaKeyFor(jwtResetSecret))
                        .compact();

                // Store token in database with 1-hour expiration
                String insertTokenQuery = "INSERT INTO password_reset_tokens (email, token, expiry) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 1 HOUR))";
                try (PreparedStatement pstmt = con.prepareStatement(insertTokenQuery)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    pstmt.executeUpdate();
                    logger.debug("Stored reset token for email: {}", email);
                }

                // Log the reset attempt
                logAttempt(con, email, ipAddress);

                // Prepare and send reset email
                String subject = "ShaadiSharthi Password Reset";
                if (APP_BASE_URL == null) {
                    logger.error("Application base URL is not configured. Please set app.base.url or APP_BASE_URL.");
                    con.rollback();
                    responseJson.put("message", "If the email is registered, a reset link has been sent");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }
                String body = "Dear Service Provider,\n\nClick the following link to reset your password:\n" + APP_BASE_URL + "/provider/reset-password?token=" + token + "\n\nThis link expires in 1 hour.\n\nBest regards,\nShaadiSarthi Team";
                
                String from = ConfigUtil.get("email.from", "EMAIL_FROM");
                String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
                if (from == null || password == null) {
                    logger.error("Missing email credentials in environment variables or config.properties");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }

                // Configure SMTP properties
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                // Create authenticated mail session
                Session mailSession = Session.getInstance(props,
                        new Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(from, password);
                            }
                        });

                try {
                    // Create and send email message
                    Message message = new MimeMessage(mailSession);
                    message.setFrom(new InternetAddress(from));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                    message.setSubject(subject);
                    message.setText(body);

                    Transport.send(message);
                    logger.info("Password reset email sent successfully to: {}", email);

                    // Return generic success response for security
                    responseJson.put("message", "If the email is registered, a reset link has been sent");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                } catch (MessagingException e) {
                    logger.error("Failed to send email to {}: {}", email, e.getMessage(), e);
                    // Still return generic response even if email fails (security)
                    responseJson.put("message", "If the email is registered, a reset link has been sent");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error processing forgot-password for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to connect to database for email {}: {}", email, e.getMessage(), e);
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}