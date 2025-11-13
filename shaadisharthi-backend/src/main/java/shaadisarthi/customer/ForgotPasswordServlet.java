package shaadisarthi.customer;

import java.io.BufferedReader;
import java.io.IOException;
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
 * Servlet implementation for password reset initiation
 * Handles forgot password flow with rate limiting and email sending
 * 
 * @WebServlet Maps to "/cstmr-forgot-password" endpoint
 * @version 1.0
 * @description Processes POST requests to send password reset links via email
 */
@WebServlet("/cstmr-forgot-password")
public class ForgotPasswordServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordServlet.class);
    // Token expiry set to 15 minutes
    private static final long TOKEN_EXPIRY_MS = 15 * 60 * 1000;
    private static final int MAX_ATTEMPTS_PER_HOUR = 5;
    private static final String APP_BASE_URL = ConfigUtil.get("app.base.url", "APP_BASE_URL");

    /**
     * Sends standardized JSON response to client
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param json JSONObject containing response data
     * @throws IOException if response writing fails
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
     * Checks if email is rate limited based on recent reset attempts
     * 
     * @param con Database connection
     * @param email Email address to check
     * @param ipAddress IP address of the request
     * @return true if rate limit exceeded, false otherwise
     * @throws SQLException if database operation fails
     */
    private boolean isRateLimited(Connection con, String email, String ipAddress) throws SQLException {
        String query = "SELECT COUNT(*) FROM reset_attempts WHERE email = ? AND attempt_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.debug("Attempts for email {} in last hour: {}", email, count);
                    return count >= MAX_ATTEMPTS_PER_HOUR;
                }
            }
        }
        return false;
    }

    /**
     * Logs password reset attempt for rate limiting
     * 
     * @param con Database connection
     * @param email Email address of the attempt
     * @param ipAddress IP address of the request
     * @throws SQLException if database operation fails
     */
    private void logAttempt(Connection con, String email, String ipAddress) throws SQLException {
        String query = "INSERT INTO reset_attempts (email, ip_address) VALUES (?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, ipAddress);
            pstmt.executeUpdate();
            logger.debug("Logged attempt for email: {}, IP: {}", email, ipAddress);
        }
    }

    /**
     * Processes POST requests for password reset initiation
     * Validates email, generates reset tokens, and sends reset emails
     * 
     * @param request HttpServletRequest containing email address
     * @param response HttpServletResponse for sending reset initiation result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /forgot-password");
        StringBuilder payload = new StringBuilder();
        String email = null;

        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
            JSONObject jsonPayload = new JSONObject(payload.toString());
            email = jsonPayload.getString("email");
        } catch (Exception e) {
            logger.warn("Invalid JSON payload: {}", payload);
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        JSONObject responseJson = new JSONObject();

        // Validate email format but don't reveal if email exists (security through obscurity)
        if (email == null || email.trim().isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            logger.warn("Invalid email format: {}", email);
            responseJson.put("message", "If the email exists in our system, a reset link has been sent.");
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
                    responseJson.put("message", "Please try again after some time");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    con.rollback();
                    return;
                }

                // Check if email exists but don't reveal result (security through obscurity)
                String checkQuery = "SELECT 1 FROM customers WHERE email = ? LIMIT 1";
                boolean emailExists = false;
                try (PreparedStatement pstmt = con.prepareStatement(checkQuery)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            emailExists = true;
                        }
                    }
                }

                logAttempt(con, email, ipAddress);

                // Always return success message regardless of email existence (security)
                if (!emailExists) {
                    con.commit();
                    responseJson.put("message", "If the email exists in our system, a reset link has been sent.");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }

                // Generate JWT token for password reset
                String jwtSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
                if (jwtSecretBase64 == null) {
                    logger.error("Missing JWT secret in configuration");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }
                byte[] jwtSecret = Base64.getDecoder().decode(jwtSecretBase64);
                if (jwtSecret.length < 32) {
                    logger.error("JWT secret too short");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }

                String token = Jwts.builder()
                        .setSubject(email)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MS))
                        .signWith(Keys.hmacShaKeyFor(jwtSecret))
                        .compact();

                // Store reset token in database with 15-minute expiry
                String insertQuery = "INSERT INTO reset_tokens (email, token, expiry, status) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE), 'pending')";
                try (PreparedStatement pstmt = con.prepareStatement(insertQuery)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    pstmt.executeUpdate();
                    logger.debug("Stored reset token for email: {}", email);
                }

                // Send password reset email via SMTP
                String from = ConfigUtil.get("email.from", "EMAIL_FROM");
                String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
                if (from == null || password == null) {
                    logger.error("Missing email credentials");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, password);
                    }
                });

                String subject = "ShaadiSarthi Password Reset";
                if (APP_BASE_URL == null) {
                    logger.error("Application base URL is not configured. Please set app.base.url or APP_BASE_URL.");
                    con.rollback();
                    responseJson.put("message", "If the email exists in our system, a reset link has been sent.");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }
                String link = APP_BASE_URL + "/customer/reset-password?token=" + token;
                String body = "Dear User,\n\nClick the following link to reset your password:\n" + link
                        + "\n\nThis link expires in 15 minutes.\n\nBest regards,\nShaadiSarthi Team";

                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(from));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                    message.setSubject(subject);
                    message.setText(body);
                    Transport.send(message);
                    logger.info("Reset email sent to: {}", email);
                } catch (MessagingException e) {
                    logger.error("Failed to send reset email to {}: {}", email, e.getMessage(), e);
                    con.rollback();
                    responseJson.put("message", "If the email exists in our system, a reset link has been sent.");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }

                con.commit();
                responseJson.put("message", "If the email exists in our system, a reset link has been sent.");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error for email {}: {}", email, e.getMessage(), e);
                responseJson.put("message", "If the email exists in our system, a reset link has been sent.");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            }
        } catch (SQLException e) {
            logger.error("Connection error for email {}: {}", email, e.getMessage(), e);
            responseJson.put("message", "If the email exists in our system, a reset link has been sent.");
            sendResponse(response, HttpServletResponse.SC_OK, responseJson);
        }
    }
}