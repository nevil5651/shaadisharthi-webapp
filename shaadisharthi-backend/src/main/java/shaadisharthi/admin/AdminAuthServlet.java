package shaadisharthi.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shaadisharthi.AuditLogger.AuditUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.security.JwtUtil;
import shaadisharthi.utils.ConfigUtil;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;

/**
 * Admin Authentication Servlet
 * 
 * Comprehensive authentication system for admin users providing:
 * - Login with email/password + optional TOTP 2FA
 * - Logout functionality with token invalidation
 * - Password reset flow with email verification
 * - TOTP (Time-based One-Time Password) setup and verification
 * - Rate limiting and account lockout mechanisms
 * - Audit logging for security tracking
 * 
 * Security Features:
 * - BCrypt password hashing
 * - JWT tokens for session management
 * - Progressive account lockout after failed attempts
 * - TOTP-based two-factor authentication
 * - Secure password reset tokens with expiry
 */
@WebServlet("/admin-auth/*")
public class AdminAuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthServlet.class);
    
    // Security configuration constants
    private static final long RESET_TOKEN_EXPIRY_MS = 15 * 60 * 1000; // 15 minutes for password reset tokens
    private static final int LOGIN_MAX_ATTEMPTS = 10; // Maximum failed login attempts before hard lockout
    private static final int LOGIN_WINDOW_SECONDS = 300; // 5 minutes window for rate limiting
    private static final int RESET_MAX_ATTEMPTS_PER_HOUR = 5; // Max password reset attempts per hour

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
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Rate limiting mechanism to prevent brute force attacks
     * 
     * Tracks attempts per email address within defined time windows:
     * - Login attempts: 5-minute window with max 10 attempts
     * - Password reset: 1-hour window with max 5 attempts
     * 
     * @param con Database connection
     * @param email User email to check
     * @param ipAddress Client IP for logging
     * @param action Type of action ("login" or "reset")
     * @return true if rate limited, false otherwise
     * @throws SQLException if database query fails
     */
    private boolean isRateLimited(Connection con, String email, String ipAddress, String action) throws SQLException {
        String query = action.equals("login")
            ? "SELECT COUNT(*) FROM reset_attempts WHERE email = ? AND attempt_time > DATE_SUB(NOW(), INTERVAL ? SECOND)"
            : "SELECT COUNT(*) FROM reset_attempts WHERE email = ? AND attempt_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            if (action.equals("login")) {
                pstmt.setInt(2, LOGIN_WINDOW_SECONDS);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.debug("Rate limit check for {} (email: {}, IP: {}): {}", action, email, ipAddress, count);
                    return count >= (action.equals("login") ? LOGIN_MAX_ATTEMPTS : RESET_MAX_ATTEMPTS_PER_HOUR);
                }
            }
        }
        return false;
    }
    
    /**
     * Retrieves admin ID from email address
     * 
     * @param con Database connection
     * @param email Admin email address
     * @return Admin ID string or null if not found/inactive
     * @throws SQLException if database query fails
     */
    private String getAdminIdFromEmail(Connection con, String email) throws SQLException {
        String query = "SELECT admin_id FROM admin WHERE email = ? AND is_active = TRUE";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("admin_id") : null;
            }
        }
    }

    /**
     * Retrieves email address from admin ID
     * 
     * @param con Database connection
     * @param adminId Admin identifier
     * @return Email address or "unknown" if not found
     * @throws SQLException if database query fails
     */
    private String getEmailFromAdminId(Connection con, String adminId) throws SQLException {
        String query = "SELECT email FROM admin WHERE admin_id = ? AND is_active = TRUE";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, adminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("email") : "unknown";
            }
        }
    }

    /**
     * Sends password reset email to admin user
     * 
     * Constructs and sends email with secure reset link containing JWT token
     * Reset link expires in 15 minutes for security
     * 
     * @param email Recipient email address
     * @param token JWT reset token
     * @param ipAddress Client IP for logging
     */
    private void sendResetEmail(String email, String token, String ipAddress) {
        String subject = "ShaadiSharthi Admin Password Reset";
        String link = "https://shaadisharthi.theworkpc.com/admin/adminresetpassword?token=" + token;
        String body = "Dear Admin,\n\nClick the following link to reset your password:\n" + link + "\n\nThis link expires in 15 minutes.\n\nBest regards,\nShaadiSharthi Team";

        // Get email configuration from environment/config
        String from = ConfigUtil.get("email.from", "EMAIL_FROM");
        String password = ConfigUtil.get("email.password", "EMAIL_PASSWORD");
        
        // Configure SMTP properties for Gmail
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
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            logger.info("Password reset email sent successfully to: {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", email, e.getMessage(), e);
        }
    }

    /**
     * Main request handler - routes to appropriate method based on URL path
     * 
     * Supported endpoints:
     * - /login: User authentication
     * - /logout: Session termination  
     * - /forgot-password: Password reset initiation
     * - /reset-password: Password reset completion
     * - /totp: TOTP setup and verification
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() != null ? request.getPathInfo().substring(1) : "login";
        String ipAddress = request.getRemoteAddr();
        JSONObject responseJson = new JSONObject();

        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false); // Use transaction for database operations

            // Route request to appropriate handler based on path
            switch (path) {
                case "login":
                    handleLogin(request, response, con, ipAddress, responseJson);
                    break;
                case "logout":
                    handleLogout(request, response, con, ipAddress, responseJson);
                    break;
                case "forgot-password":
                    handleForgotPassword(request, response, con, ipAddress, responseJson);
                    break;
                case "reset-password":
                    handleResetPassword(request, response, con, ipAddress, responseJson);
                    break;
                case "totp":
                    handleTotp(request, response, con, ipAddress, responseJson);
                    break;
                default:
                    responseJson.put("error", "Invalid endpoint");
                    sendResponse(response, HttpServletResponse.SC_NOT_FOUND, responseJson);
            }

            con.commit(); // Commit transaction if all operations succeed
        } catch (SQLException e) {
            logger.error("Database error for {}: {}", path, e.getMessage(), e);
            responseJson.put("error", "Database error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            logger.error("Security error for {}: {}", path, e.getMessage(), e);
            responseJson.put("error", "Security processing error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }

    /**
     * Handles admin login with comprehensive security checks
     * 
     * Security features:
     * - Rate limiting to prevent brute force
     * - Progressive account lockout (30s after 3 attempts, 2min after 4 attempts)
     * - Hard lockout after 10 failed attempts
     * - Automatic reset of failed attempts after 1 hour
     * - BCrypt password verification
     * - TOTP two-factor authentication
     * - JWT token generation on successful login
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse  
     * @param con Database connection
     * @param ipAddress Client IP for logging
     * @param responseJson Response JSON object
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response, Connection con, String ipAddress, JSONObject responseJson)
            throws IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException {
        
        logger.debug("Processing login request from IP: {}", ipAddress);
        
        // Read and parse request body
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String requestBody = sb.toString();
        logger.debug("Login request body: {}", requestBody);

        JSONObject json = new JSONObject(requestBody);
        String email = json.getString("email");
        String password = json.getString("password");
        String totpCode = json.optString("totpCode");
        String ip = request.getRemoteAddr();

        // Validate required fields
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            logger.warn("Login attempt with missing email or password from IP: {}", ipAddress);
            responseJson.put("error", "Email and password are required");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        logger.info("Login attempt for email: {} from IP: {}", email, ipAddress);

        // Check rate limiting
        if (isRateLimited(con, email, ipAddress, "login")) {
            logger.warn("Rate limited login attempt for email: {} from IP: {}", email, ipAddress);
            responseJson.put("error", "Too many login attempts. Try again later.");
            sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
            return;
        }

        // Query admin details with row locking to prevent race conditions
        String query = "SELECT admin_id, role, password, totp_secret, is_active, failed_login_attempts, last_failed_attempt FROM admin WHERE email = ? FOR UPDATE";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    logger.warn("Login failed - email not found: {} from IP: {}", email, ipAddress);
                    responseJson.put("message", "Login failed. Check credentials.");
                    sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
                    return;
                }

                // Extract admin details from result set
                String adminId = rs.getString("admin_id");
                String role = rs.getString("role");
                String hashedPassword = rs.getString("password");
                String totpSecret = rs.getString("totp_secret");
                boolean isActive = rs.getBoolean("is_active");
                int failedAttempts = rs.getInt("failed_login_attempts");
                Timestamp lastFailedAttempt = rs.getTimestamp("last_failed_attempt");

                logger.debug("Admin found: ID={}, role={}, isActive={}, failedAttempts={}", adminId, role, isActive, failedAttempts);

                // Reset failed attempts if 1 hour has passed since last failure
                if (failedAttempts >= LOGIN_MAX_ATTEMPTS && lastFailedAttempt != null) {
                    long timeSinceLastFailure = System.currentTimeMillis() - lastFailedAttempt.getTime();
                    long oneHourInMs = 60 * 60 * 1000;
                    
                    if (timeSinceLastFailure > oneHourInMs) {
                        // Reset failed attempts counter after 1 hour
                        String resetQuery = "UPDATE admin SET failed_login_attempts = 0, last_failed_attempt = NULL WHERE email = ?";
                        try (PreparedStatement resetStmt = con.prepareStatement(resetQuery)) {
                            resetStmt.setString(1, email);
                            int updated = resetStmt.executeUpdate();
                            if (updated > 0) {
                                logger.info("Reset failed attempts for email: {} after 1 hour", email);
                                failedAttempts = 0; // Update local variable
                            } else {
                                logger.error("Failed to reset attempts for email: {}", email);
                            }
                        }
                    }
                }

                // Check if account is active
                if (!isActive) {
                    logger.warn("Login attempt for inactive account: {} from IP: {}", email, ipAddress);
                    responseJson.put("error", "Account is inactive");
                    sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
                    return;
                }

                // PROGRESSIVE LOCKING MECHANISM - Temporary locks after multiple failures
                if (failedAttempts >= 3 && lastFailedAttempt != null) {
                    long timeSinceLastFailure = System.currentTimeMillis() - lastFailedAttempt.getTime();
                    
                    // Progressive delays: 30s after 3 attempts, 2min after 4 attempts
                    long delayMs = 0;
                    if (failedAttempts == 3) {
                        delayMs = 30 * 1000; // 30 seconds
                    } else if (failedAttempts == 4) {
                        delayMs = 2 * 60 * 1000; // 2 minutes
                    }
                    
                    // Check if account is still in temporary lock period
                    if (timeSinceLastFailure < delayMs) {
                        long secondsLeft = (delayMs - timeSinceLastFailure) / 1000;
                        logger.warn("Account temporarily locked: {} from IP: {} ({} seconds remaining)", 
                                   email, ipAddress, secondsLeft);
                        responseJson.put("error", "Too many failed attempts. Please try again in " + secondsLeft + " seconds.");
                        sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
                        return;
                    }
                }

                // HARD LOCKOUT after maximum allowed attempts
                if (failedAttempts >= LOGIN_MAX_ATTEMPTS) {
                    logger.warn("Account locked due to too many failed attempts: {} from IP: {}", email, ipAddress);
                    responseJson.put("error", "Account locked due to too many failed attempts. Try again after some time or contact support.");
                    sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
                    return;
                }

                // Verify password using BCrypt
                boolean passwordCorrect = BCrypt.checkpw(password, hashedPassword);
                
                if (!passwordCorrect) {
                    logger.warn("Invalid password for email: {} from IP: {}", email, ipAddress);
                    
                    // Increment failed attempts counter
                    String updateQuery = "UPDATE admin SET failed_login_attempts = failed_login_attempts + 1, last_failed_attempt = NOW() WHERE admin_id = ?";
                    try (PreparedStatement updateStmt = con.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, adminId);
                        int rowsUpdated = updateStmt.executeUpdate();
                        if (rowsUpdated > 0) {
                            logger.debug("Successfully incremented failed login attempts for admin: {}", adminId);
                        } else {
                            logger.error("Failed to update login attempts for admin: {}", adminId);
                        }
                    }
                    
                    int newFailedAttempts = failedAttempts + 1;
                    
                    // Progressive error messages based on attempt count
                    if (newFailedAttempts >= LOGIN_MAX_ATTEMPTS) {
                        logger.warn("Account now locked after maximum failed attempts: {} from IP: {}", email, ipAddress);
                        responseJson.put("error", "Account locked due to too many failed attempts. Try again after some time.");
                    } else if (newFailedAttempts == 4) {
                        responseJson.put("error", "Login failed. 1 attempt remaining. Next failed attempt will lock your account for some time.");
                    } else if (newFailedAttempts == 3) {
                        responseJson.put("error", "Login failed. 2 attempts remaining. Please wait 30 seconds before next attempt.");
                    } else {
                        int attemptsLeft = LOGIN_MAX_ATTEMPTS - newFailedAttempts;
                        responseJson.put("message", "Login failed. " + attemptsLeft + " attempts remaining.");
                    }  
                    
                    sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
                    return;
                }

                // PASSWORD IS CORRECT - Continue with TOTP checks
                
                // Case 1: TOTP not set up yet - require setup
                if (totpSecret == null) {
                    logger.info("Initial login successful for email: {}, TOTP setup required", email);
                    
                    // Reset failed attempts on successful password verification
                    String resetQuery = "UPDATE admin SET failed_login_attempts = 0, last_failed_attempt = NULL, last_login = NOW() WHERE admin_id = ?";
                    try (PreparedStatement resetStmt = con.prepareStatement(resetQuery)) {
                        resetStmt.setString(1, adminId);
                        int rowsUpdated = resetStmt.executeUpdate();
                        if (rowsUpdated > 0) {
                            logger.debug("Reset failed attempts and updated last login for admin: {}", adminId);
                        }
                    }
                    
                    // Generate JWT token and indicate TOTP setup required
                    String token = JwtUtil.generateToken(Integer.parseInt(adminId), role);
                    responseJson.put("token", token);
                    responseJson.put("adminId", adminId);
                    responseJson.put("setupTotp", true);
                    responseJson.put("email", email);
                    logger.info("Login successful, TOTP setup required for admin: {}", adminId);
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }

                // Case 2: TOTP is set up but code not provided in request
                if (totpSecret != null && totpCode == null) {
                    logger.info("TOTP code required for admin: {}", adminId);
                    
                    // Reset failed attempts since password was correct
                    String resetQuery = "UPDATE admin SET failed_login_attempts = 0, last_failed_attempt = NULL WHERE admin_id = ?";
                    try (PreparedStatement resetStmt = con.prepareStatement(resetQuery)) {
                        resetStmt.setString(1, adminId);
                        resetStmt.executeUpdate();
                    }
                    
                    responseJson.put("totpRequired", true);
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }

                // Case 3: TOTP is set up and code provided - validate it
                if (totpSecret != null) {
                    logger.debug("Validating TOTP code for admin: {}", adminId);
                    TimeBasedOneTimePasswordGenerator totpGenerator = new TimeBasedOneTimePasswordGenerator();
                    Base32 base32 = new Base32();
                    byte[] secretBytes = base32.decode(totpSecret);
                    if (secretBytes == null || secretBytes.length == 0) {
                        logger.error("Invalid TOTP secret for email: {}", email);
                        responseJson.put("error", "Invalid Credentials");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                    SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA1");
                    Instant now = Instant.now();
                    int generatedCode = totpGenerator.generateOneTimePassword(secretKey, now);
                    
                    // Verify TOTP code
                    if (!String.valueOf(generatedCode).equals(totpCode)) {
                        logger.warn("Invalid TOTP code for admin: {} from IP: {}", adminId, ipAddress);
                        
                        // Increment failed attempts for TOTP failure too
                        String updateQuery = "UPDATE admin SET failed_login_attempts = failed_login_attempts + 1, last_failed_attempt = NOW() WHERE admin_id = ?";
                        try (PreparedStatement updateStmt = con.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, adminId);
                            int rowsUpdated = updateStmt.executeUpdate();
                            if (rowsUpdated > 0) {
                                logger.debug("Incremented failed login attempts due to invalid TOTP for admin: {}", adminId);
                            }
                        }
                        
                        responseJson.put("error", "Invalid Credentials");
                        sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
                        return;
                    }
                    logger.debug("TOTP validation successful for admin: {}", adminId);
                }

                // SUCCESSFUL LOGIN - All checks passed
                logger.info("Successful login for admin: {} from IP: {}", adminId, ipAddress);
                
                // Reset failed attempts and update last login timestamp
                String resetQuery = "UPDATE admin SET failed_login_attempts = 0, last_failed_attempt = NULL, last_login = NOW() WHERE admin_id = ?";
                try (PreparedStatement resetStmt = con.prepareStatement(resetQuery)) {
                    resetStmt.setString(1, adminId);
                    int rowsUpdated = resetStmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        logger.debug("Reset failed attempts and updated last login for admin: {}", adminId);
                    }
                }
                
                // Generate JWT token and log successful login
                String token = JwtUtil.generateToken(Integer.parseInt(adminId), role);
                AuditUtil.logAudit(adminId, "LOGIN_SUCCESS", adminId, "ADMIN", "Login for email: " + email, null, ip);
                responseJson.put("token", token);
                responseJson.put("adminId", adminId);
                logger.info("Login completed successfully for admin: {}", adminId);
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            }
        }
    }
    
    /**
     * Handles admin logout with token validation and audit logging
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param con Database connection
     * @param ipAddress Client IP for logging
     * @param responseJson Response JSON object
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response, Connection con, String ipAddress, JSONObject responseJson)
            throws IOException, SQLException {
        // Extract JWT token from Authorization header
        String token = request.getHeader("Authorization");
        String ip = request.getRemoteAddr();
        if (token != null) {
            token = token.replace("Bearer ", "");
        }
        if (token == null) {
            responseJson.put("error", "Authorization token required");
            sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
            return;
        }

        try {
            // Validate token and extract admin details
            String adminId = JwtUtil.getAdminIdFromToken(token);
            String email = getEmailFromAdminId(con, adminId);

            // Log logout event for audit trail
            AuditUtil.logAudit(adminId, "LOGOUT", adminId, "ADMIN", "Logout for email: " + email, null, ip);
            
            responseJson.put("message", "Logged out successfully");
            sendResponse(response, HttpServletResponse.SC_OK, responseJson);
        } catch (Exception e) {
            // Log failed logout attempt with invalid token
            responseJson.put("error", "Invalid token");
            sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
        }
    }

    /**
     * Handles forgot password flow - generates reset token and sends email
     * 
     * Security features:
     * - Rate limiting to prevent email spam
     * - JWT-based secure reset tokens with 15min expiry
     * - Generic responses to prevent email enumeration
     * - Audit logging for security tracking
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param con Database connection
     * @param ipAddress Client IP for logging
     * @param responseJson Response JSON object
     */
    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response, Connection con, String ipAddress, JSONObject responseJson)
            throws IOException, SQLException {
        // Read and parse request body
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String requestBody = sb.toString();
        String ip = request.getRemoteAddr();
        
        JSONObject json = new JSONObject(requestBody);
        String email = json.getString("email");

        // Validate email format - return generic success regardless to prevent enumeration
        if (email == null || email.trim().isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            responseJson.put("message", "If the email is registered, a reset link has been sent");
            sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            return;
        }

        // Check rate limiting for password reset attempts
        if (isRateLimited(con, email, ipAddress, "reset")) {
            AuditUtil.logAudit("unknown", "PASSWORD_RESET_REQUEST", null, "ADMIN", "Reset requested for email: " + email, "Too many attempts", ip);
            responseJson.put("error", "Too many reset attempts. Please try again later.");
            sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
            return;
        }

        // Verify email exists and account is active
        String query = "SELECT admin_id FROM admin WHERE email = ? AND is_active = TRUE";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    // Log non-existent email attempt but return generic success
                    AuditUtil.logAudit(null, "PASSWORD_RESET_REQUEST", null, "ADMIN", "Reset requested for email: " + email, "Email not found", ip);
                    
                    responseJson.put("message", "If the email is registered, a reset link has been sent");
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    return;
                }
            }
        }

        // Generate JWT reset token
        String jwtResetSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
        byte[] jwtResetSecret = Base64.getDecoder().decode(jwtResetSecretBase64);
        String adminId = getAdminIdFromEmail(con, email);

        // Create JWT token with 15min expiry
        String token = Jwts.builder()
            .setSubject(adminId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + RESET_TOKEN_EXPIRY_MS))
            .signWith(Keys.hmacShaKeyFor(jwtResetSecret))
            .compact();

        // Store reset token in database with expiry
        String insertTokenQuery = "INSERT INTO admin_password_reset_tokens (token, admin_id, expiry) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 15 MINUTE))";
        try (PreparedStatement pstmt = con.prepareStatement(insertTokenQuery)) {
            pstmt.setString(1, token);
            pstmt.setString(2, adminId);
            pstmt.executeUpdate();
        }

        // Send reset email and log successful request
        sendResetEmail(email, token, ipAddress);
        AuditUtil.logAudit(adminId, "PASSWORD_RESET_REQUEST", adminId, "ADMIN", "Reset requested for email: " + email, null, ip);
        
        responseJson.put("message", "If the email is registered, a reset link has been sent");
        sendResponse(response, HttpServletResponse.SC_OK, responseJson);
    }

    /**
     * Handles password reset with token validation and security checks
     * 
     * Validates:
     * - Reset token validity and expiry
     * - Password strength requirements
     * - Token single-use prevention
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param con Database connection
     * @param ipAddress Client IP for logging
     * @param responseJson Response JSON object
     */
    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response, Connection con, String ipAddress, JSONObject responseJson)
            throws IOException, SQLException {
        // Read request payload
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) payload.append(line);
        }
        JSONObject jsonPayload = new JSONObject(payload.toString());
        String token = jsonPayload.getString("token");
        String password = jsonPayload.getString("password");
        String ip = request.getRemoteAddr();

        // Validate password meets security requirements
        if (password == null || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            responseJson.put("error", "Password must be at least 8 characters with one uppercase, lowercase, digit, and special character");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Validate JWT reset token
        String jwtResetSecretBase64 = ConfigUtil.get("jwt.reset.secret.key", "JWT_RESET_SECRET_KEY");
        byte[] jwtResetSecret = Base64.getDecoder().decode(jwtResetSecretBase64);
        String adminId;
        try {
            adminId = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(jwtResetSecret)).build().parseClaimsJws(token).getBody().getSubject();
        } catch (Exception e) {
            responseJson.put("error", "Invalid or expired token");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Verify token exists in database and is not used/expired
        String query = "SELECT expiry, used FROM admin_password_reset_tokens WHERE admin_id = ? AND token = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, adminId);
            pstmt.setString(2, token);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next() || rs.getBoolean("used") || rs.getTimestamp("expiry").before(new Date())) {
                    AuditUtil.logAudit(adminId, "PASSWORD_RESET_ATTEMPT", adminId, "ADMIN", "Reset attempt for admin_id: " + adminId, "Invalid token", ip);
                     
                    responseJson.put("error", "Invalid or expired token");
                    sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                    return;
                }
            }
        }

        // Hash new password and update database
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        query = "UPDATE admin SET password = ?, failed_login_attempts = 0 WHERE admin_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, adminId);
            if (pstmt.executeUpdate() == 0) throw new SQLException("Update failed");
        }

        // Mark token as used to prevent reuse
        query = "UPDATE admin_password_reset_tokens SET used = TRUE WHERE admin_id = ? AND token = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, adminId);
            pstmt.setString(2, token);
            pstmt.executeUpdate();
        }

        // Log successful password reset
        AuditUtil.logAudit(adminId, "PASSWORD_RESET_CONFIRM", adminId, "ADMIN", "Password reset confirmed for admin_id: " + adminId, null, ip);
        responseJson.put("message", "Password reset successfully");
        sendResponse(response, HttpServletResponse.SC_OK, responseJson);
    }

    /**
     * Handles TOTP (Time-based One-Time Password) operations:
     * - Setup: Generates new TOTP secret and QR code URL
     * - Verify: Validates TOTP code against stored secret
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param con Database connection
     * @param ipAddress Client IP for logging
     * @param responseJson Response JSON object
     */
    private void handleTotp(HttpServletRequest request, HttpServletResponse response, Connection con, String ipAddress, JSONObject responseJson)
            throws IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException {
        
        // Read and parse request body
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String requestBody = sb.toString();

        JSONObject json = new JSONObject(requestBody);
        String action = json.getString("action");
        String adminId = json.getString("adminId");
        String totpCode = json.optString("totpCode");

        // Extract and validate JWT token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) 
            ? authHeader.substring(7) 
            : null;
        

        if (token == null) {
            responseJson.put("error", "Authorization token required");
            sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
            logger.warn("TOTP request without token from IP: {}", ipAddress);
            return;
        }

        // Validate token and extract adminId from it
        String tokenAdminId;
        try {
            tokenAdminId = JwtUtil.getAdminIdFromToken(token);
            if (tokenAdminId == null) {
                responseJson.put("error", "Invalid token");
                sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
                logger.warn("Invalid token for TOTP request from IP: {}", ipAddress);
                return;
            }
        } catch (Exception e) {
            responseJson.put("error", "Token validation failed");
            sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
            logger.error("Token validation error for TOTP request: {}", e.getMessage());
            return;
        }

        // Verify adminId from request matches token adminId for security
        if (adminId == null || !tokenAdminId.equals(adminId)) {
            responseJson.put("error", "Invalid adminId or token mismatch");
            sendResponse(response, HttpServletResponse.SC_FORBIDDEN, responseJson);
            logger.warn("AdminId mismatch: requested {}, token {}, IP: {}", adminId, tokenAdminId, ipAddress);
            return;
        }

        // Validate required parameters based on action
        if (action == null || adminId == null || (action.equals("verify") && totpCode == null)) {
            responseJson.put("error", "Invalid TOTP request");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Retrieve current TOTP secret from database
        String query = "SELECT totp_secret FROM admin WHERE admin_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, tokenAdminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    responseJson.put("error", "Admin not found");
                    sendResponse(response, HttpServletResponse.SC_NOT_FOUND, responseJson);
                    return;
                }
                String totpSecret = rs.getString("totp_secret");

                // TOTP SETUP - Generate new secret and QR code
                if (action.equals("setup")) {
                    if (totpSecret != null) {
                        responseJson.put("error", "TOTP already setup");
                        sendResponse(response, HttpServletResponse.SC_CONFLICT, responseJson);
                        return;
                    }
                    
                    // Generate cryptographically secure random secret
                    byte[] secretBytes = new byte[20];
                    new SecureRandom().nextBytes(secretBytes);
                    Base32 base32 = new Base32();
                    String totpSecretEncoded = new String(base32.encode(secretBytes), StandardCharsets.UTF_8).replaceAll("=+$", "");
                    
                    // Get email for QR code generation
                    String email = getEmailFromAdminId(con, tokenAdminId);
                    
                    // Store new TOTP secret in database
                    query = "UPDATE admin SET totp_secret = ? WHERE admin_id = ?";
                    try (PreparedStatement updateStmt = con.prepareStatement(query)) {
                        updateStmt.setString(1, totpSecretEncoded);
                        updateStmt.setString(2, tokenAdminId);
                        updateStmt.executeUpdate();
                    }
                    
                    // Generate QR code URL for authenticator apps
                    String qrCodeUrl = "otpauth://totp/ShaadiSharthi:" + email + "?secret=" + totpSecretEncoded + "&issuer=ShaadiSharthi";
                    responseJson.put("totpSecret", totpSecretEncoded);
                    responseJson.put("qrCodeUrl", qrCodeUrl);
                    sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    
                } 
                // TOTP VERIFICATION - Validate provided code
                else if (action.equals("verify")) {
                    if (totpSecret == null) {
                        responseJson.put("error", "TOTP not setup");
                        sendResponse(response, HttpServletResponse.SC_PRECONDITION_FAILED, responseJson);
                        return;
                    }
                    
                    // Generate current TOTP code for comparison
                    TimeBasedOneTimePasswordGenerator totpGenerator = new TimeBasedOneTimePasswordGenerator();
                    Base32 base32 = new Base32();
                    byte[] secretBytes = base32.decode(totpSecret);
                    if (secretBytes == null || secretBytes.length == 0) {
                        logger.error("Invalid TOTP secret for adminId: {}", tokenAdminId);
                        responseJson.put("error", "Invalid TOTP secret");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                    SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA1");
                    Instant now = Instant.now();
                    int generatedCode = totpGenerator.generateOneTimePassword(secretKey, now);
                    
                    // Compare generated code with user input
                    if (String.valueOf(generatedCode).equals(totpCode)) {
                        responseJson.put("message", "2FA verified.");
                        sendResponse(response, HttpServletResponse.SC_OK, responseJson);
                    } else {
                        responseJson.put("error", "Invalid TOTP code");
                        sendResponse(response, HttpServletResponse.SC_UNAUTHORIZED, responseJson);
                    }
                }
            }
        }
    }
}