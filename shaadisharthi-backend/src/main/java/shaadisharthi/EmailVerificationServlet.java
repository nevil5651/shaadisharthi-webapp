package shaadisharthi;

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
 * Servlet for handling email verification during service provider registration
 * Provides secure email verification with rate limiting and JWT token generation
 * 
 * Features:
 * - Rate limiting to prevent abuse
 * - JWT-based verification tokens
 * - SMTP email integration
 * - Security-focused email enumeration prevention
 * 
 * @WebServlet Maps to "/verify-email" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/verify-email")
public class EmailVerificationServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationServlet.class);
    private static final Properties config = new Properties();
    
    // Token expiry set to 15 minutes for security
    private static final long TOKEN_EXPIRY_MS = 15 * 60 * 1000;
    
    // Rate limiting: maximum 5 attempts per hour per email
    private static final int MAX_ATTEMPTS_PER_HOUR = 5;
    private static final String APP_BASE_URL = ConfigUtil.get("app.base.url", "APP_BASE_URL");

    // Static initialization for configuration loading
    static {
        try {
            config.load(EmailVerificationServlet.class.getClassLoader().getResourceAsStream("config.properties"));
            logger.info("Loaded config.properties for email verification");
        } catch (IOException e) {
            logger.error("Failed to load config.properties: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends standardized HTTP responses with JSON content
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
     * Checks if the request is rate limited for the given email and IP address
     * 
     * @param con Database connection
     * @param email Email address to check
     * @param ipAddress Client IP address
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
                    logger.debug("Attempts for email {} in last hour: {}", email, count);
                    return count >= MAX_ATTEMPTS_PER_HOUR;
                }
            }
        }
        return false;
    }

    /**
     * Logs an attempt for rate limiting purposes
     * 
     * @param con Database connection
     * @param email Email address
     * @param ipAddress Client IP address
     * @throws SQLException If database operation fails
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
     * Handles POST requests for email verification initiation
     * Processes email verification requests with security measures
     * 
     * @param request HttpServletRequest with JSON containing email address
     * @param response HttpServletResponse with operation result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /ShaadiSharthi/api/verify-email");
        StringBuilder payload = new StringBuilder();
        String email = null;

        // Read and parse JSON payload from request
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
            JSONObject jsonPayload = new JSONObject(payload.toString());
            email = jsonPayload.getString("email");
        } catch (IOException e) {
            logger.error("Failed to read request payload: {}", e.getMessage(), e);
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        } catch (Exception e) {
            logger.warn("Invalid JSON payload: {}", payload);
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        JSONObject responseJson = new JSONObject();

        // Validate email format - always return success to prevent email enumeration
        if (email == null || email.trim().isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            logger.warn("Invalid email format: {}", email);
            // Security: Always return success to prevent email enumeration attacks
            responseJson.put("message", "If the email is valid, a verification link has been sent");
            sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            return;
        }

        String ipAddress = request.getRemoteAddr();
        
        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false);
            try {
                // Check rate limiting before processing
                if (isRateLimited(con, email, ipAddress)) {
                    logger.warn("Rate limit exceeded for email: {}, IP: {}", email, ipAddress);
                    responseJson.put("error", "Too many requests. Try again later.");
                    sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
                    return;
                }

                // Check if email already exists in system (prevent duplicate registration)
                String checkQuery = "SELECT 1 FROM service_providers WHERE email = ? LIMIT 1";
                try (PreparedStatement pstmt = con.prepareStatement(checkQuery)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            logger.info("Email already registered: {}", email);
                            logAttempt(con, email, ipAddress);
                            con.commit();
                            // Security: Always return success to prevent email enumeration
                            responseJson.put("message", "If the email is valid, a verification link has been sent");
                            sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                            return;
                        }
                    }
                }

                // Generate JWT token for email verification
                String jwtSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
                if (jwtSecretBase64 == null) {
                    logger.error("Missing JWT secret in config.properties");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }
                
                // Decode JWT secret from Base64
                byte[] jwtSecret = Base64.getDecoder().decode(jwtSecretBase64);
                if (jwtSecret.length < 32) {
                    logger.error("JWT secret too short");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }

                // Create JWT token with email as subject and 15-minute expiry
                String token = Jwts.builder()
                        .setSubject(email)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRY_MS))
                        .signWith(Keys.hmacShaKeyFor(jwtSecret))
                        .compact();

                // Store verification token in database
                String insertQuery = "INSERT INTO email_verification_tokens (email, token, expiry) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))";
                try (PreparedStatement pstmt = con.prepareStatement(insertQuery)) {
                    pstmt.setString(1, email);
                    pstmt.setString(2, token);
                    pstmt.executeUpdate();
                    logger.debug("Stored verification token for email: {}", email);
                }

                // Log attempt for rate limiting
                logAttempt(con, email, ipAddress);

                // Send verification email via SMTP
                String from = ConfigUtil.get("email.from", "EMAIL_FROM");
                String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
                if (from == null || password == null) {
                    logger.error("Missing email credentials");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }

                // Configure SMTP properties for Gmail
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

                // Prepare email content
                String subject = "ShaadiSarthi Email Verification";
                if (APP_BASE_URL == null) {
                    logger.error("Application base URL is not configured. Please set app.base.url or APP_BASE_URL.");
                    responseJson.put("error", "Server configuration error");
                    sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                    return;
                }
                String link = APP_BASE_URL + "/provider/register?token=" + token;
                String body = "Dear User,\n\nClick the following link to verify your email and complete registration:\n" + link
                        + "\n\nThis link expires in 15 minutes.\n\nBest regards,\nShaadiSarthi Team";

                try {
                    // Create and send email message
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(from));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                    message.setSubject(subject);
                    message.setText(body);
                    Transport.send(message);
                    logger.info("Verification email sent to: {}", email);
                } catch (MessagingException e) {
                    logger.error("Failed to send email to {}: {}", email, e.getMessage(), e);
                    con.rollback();
                    // Security: Always return success even on email failure
                    responseJson.put("message", "If the email is valid, a verification link has been sent");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }

                con.commit();
                // Security: Always return generic success message
                responseJson.put("message", "If the email is valid, a verification link has been sent");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                con.rollback();
                logger.error("Database error for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            }
        } catch (SQLException e) {
            logger.error("Connection error for email {}: {}", email, e.getMessage(), e);
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}