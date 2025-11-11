package shaadisharthi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import shaadisharthi.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JwtUtil - JWT token generation and validation utilities
 * 
 * Centralized JWT handling for the entire application:
 * - Token generation with role claims and expiration
 * - Token parsing and validation
 * - Secure key management with environment configuration
 * - HMAC-SHA256 signing for token integrity
 * 
 * Used by all JWT filters and authentication services
 * 
 * @category Security & Authentication
 * @threading Thread-safe with lazy key initialization
 */
public class JwtUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);
    
    // Secret key for JWT signing/verification - lazy initialized
    private static SecretKey secretKey;

    /**
     * Get JWT secret key with lazy initialization
     * 
     * Loads secret key from environment variables or config.properties
     * Uses ConfigUtil for flexible configuration management
     * 
     * @return SecretKey for JWT operations
     * @throws RuntimeException If key is missing or too short
     */
    private static SecretKey getSecretKey() {
        if (secretKey == null) {
            try {
                String keyString = ConfigUtil.get("jwt.secret.key", "JWT_SECRET_KEY");
                LOGGER.info("Key = {}", keyString);
                if (keyString == null || keyString.length() < 32) {
                    throw new RuntimeException("JWT secret key is missing or too short");
                }
                secretKey = Keys.hmacShaKeyFor(keyString.getBytes());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load JWT secret key", e);
            }
        }
        return secretKey;
    }

    /**
     * Generate JWT token for authenticated user
     * 
     * @param adminId User identifier (subject)
     * @param adminRole User role for authorization
     * @return Signed JWT token string
     * @throws RuntimeException If token generation fails
     */
    public static String generateToken(int adminId, String adminRole) {
        try {
            SecretKey key = getSecretKey();
            String token = Jwts.builder()
                    .claim("role", adminRole) // Custom claim for role-based access
                    .setSubject(String.valueOf(adminId)) // User identifier
                    .setIssuedAt(new Date()) // Token creation time
                    .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 6)) // 6 hours expiration
                    .signWith(key, SignatureAlgorithm.HS256) // HMAC-SHA256 signing
                    .compact();
            return token;
        } catch (Exception e) {
            LOGGER.error("Token generation error: {}", e.getMessage(), e);
            throw new RuntimeException("Token generation error", e);
        }
    }

    /**
     * Parse and validate JWT token
     * 
     * @param token JWT token string to parse
     * @return Claims object containing token payload
     * @throws io.jsonwebtoken.ExpiredJwtException If token has expired
     * @throws Exception If token is invalid or malformed
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            LOGGER.error("Token parsing error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Extract user ID from JWT token
     * 
     * Convenience method for getting subject without full claims parsing
     * 
     * @param token JWT token string
     * @return User ID as string
     * @throws Exception If token is invalid or malformed
     */
    public static String getAdminIdFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody().getSubject();
        } catch (Exception e) {
            LOGGER.error("Error extracting admin ID from token: {}", e.getMessage(), e);
            throw e;
        }
    }
}