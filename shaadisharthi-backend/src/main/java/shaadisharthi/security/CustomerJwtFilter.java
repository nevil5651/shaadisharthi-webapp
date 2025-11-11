package shaadisharthi.security;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import io.jsonwebtoken.Claims;
import shaadisharthi.utils.ConfigUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CustomerJwtFilter - JWT authentication and authorization for Customer endpoints
 * 
 * Specialized filter for customer-facing endpoints with cookie-based token storage:
 * - Extracts JWT from "session" cookie instead of Authorization header
 * - Different public endpoint whitelist for customer operations
 * - Role-based access control for customer-specific roles
 * - Handles customer authentication flows (signin, register, password reset)
 * 
 * Applied to /Customer/* endpoints
 * 
 * @category Security & Authentication
 */
@WebFilter(urlPatterns = {"/Customer/*"})
public class CustomerJwtFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CustomerJwtFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CustomerJwtFilter initialized");
        // ConfigUtil handles loading config.properties and prefers environment variables
    }

    /**
     * Filter requests for customer JWT authentication and authorization
     * 
     * Customer-specific flow:
     * 1. Check if customer endpoint is public (authentication skipped)
     * 2. Extract JWT from "session" cookie (customer-specific storage)
     * 3. Parse and validate token
     * 4. Verify customer role against endpoint requirements
     * 5. Set claims for downstream processing
     * 
     * @param request Servlet request
     * @param response Servlet response
     * @param chain Filter chain
     * @throws IOException If response writing fails
     * @throws ServletException If filter processing fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String servletPath = httpRequest.getServletPath();
        String method = httpRequest.getMethod();

        logger.info("CustomerJwtFilter: Incoming request - Path: {}, Method: {}", servletPath, method);

        // Skip authentication for public customer endpoints
        if (isPublicEndpoint(servletPath, method)) {
            logger.info("CustomerJwtFilter: Public endpoint detected, skipping authentication - Path: {}", servletPath);
            chain.doFilter(request, response);
            return;
        }

        // Extract token from customer session cookie
        String token = extractTokenFromCookies(httpRequest.getCookies());
        if (token == null) {
            logger.warn("CustomerJwtFilter: Missing session token - Path: {}", servletPath);
            sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Session token is missing");
            return;
        }

        logger.debug("CustomerJwtFilter: Extracted session token: {}", token);
        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
            if (claims == null) {
                logger.error("CustomerJwtFilter: JwtUtil.parseToken returned null - Path: {}", servletPath);
                sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
            logger.info("CustomerJwtFilter: Token parsed successfully - Subject: {}", claims.getSubject());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.error("CustomerJwtFilter: Token expired - Path: {} - {}", servletPath, e.getMessage(), e);
            sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
            return;
        } catch (Exception e) {
            logger.error("CustomerJwtFilter: Token parsing failed - Path: {} - {}", servletPath, e.getMessage(), e);
            sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Check role authorization for customer endpoints
        List<String> allowedRoles = getAllowedRoles(servletPath, method);
        if (allowedRoles == null) {
            logger.warn("CustomerJwtFilter: No roles configured for endpoint - Path: {}, Method: {}", servletPath, method);
            sendError(httpResponse, HttpServletResponse.SC_FORBIDDEN, "No roles configured for this endpoint");
            return;
        }

        String role = claims.get("role", String.class);
        if (role == null || !allowedRoles.contains(role)) {
            logger.warn("CustomerJwtFilter: Access denied - Role: {}, Path: {}, Allowed: {}", role, servletPath, allowedRoles);
            sendError(httpResponse, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Insufficient Role");
            return;
        }

        logger.info("CustomerJwtFilter: Role {} authorized for endpoint {}", role, servletPath);
        logger.debug("CustomerJwtFilter: Setting claims attribute for downstream processing");
        httpRequest.setAttribute("claims", claims);

        logger.info("CustomerJwtFilter: Passing request down the filter chain");
        chain.doFilter(request, response);
    }

    /**
     * Extract JWT token from "session" cookie (customer-specific storage)
     * 
     * @param cookies HTTP cookies from request
     * @return JWT token string, or null if not found
     */
    private String extractTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("session".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Check if customer endpoint is publicly accessible
     * 
     * Includes customer authentication, registration, and support endpoints
     * 
     * @param servletPath Request servlet path
     * @param method HTTP method
     * @return true if endpoint is public, false otherwise
     */
    private boolean isPublicEndpoint(String servletPath, String method) {
        return Set.of(
            "/Customer/signin",
            "/Customer/register",
            "/Customer/cstmr-verify-email",
            "/Customer/cstmr-email-verification",
            "/Customer/cstmr-rgt",
            "/Customer/cstmr-forgot-password",
            "/Customer/cstmr-reset-password",
            "/Customer/AddGuestQuery"
        ).contains(servletPath);
    }

    /**
     * Get allowed roles for customer endpoint from configuration
     * 
     * @param servletPath Request servlet path
     * @param method HTTP method
     * @return List of allowed roles, or null if no roles configured
     */
    private List<String> getAllowedRoles(String servletPath, String method) {
        String endpoint = servletPath.replaceFirst("^/Customer/", "");
        String key = endpoint + "." + method.toLowerCase() + ".roles";
        String roles = ConfigUtil.get(key, key.toUpperCase().replace(".", "_"));
        if (roles == null) {
            roles = ""; // Fallback to empty string if missing
        }
        logger.debug("Roles for key {}: {}", key, roles);
        return roles.isEmpty() ? null : Arrays.asList(roles.split(","));
    }

    /**
     * Send standardized error response
     * 
     * @param response HTTP response
     * @param status HTTP status code
     * @param message Error description
     * @throws IOException If response writing fails
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        JSONObject json = new JSONObject();
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    @Override
    public void destroy() {
        logger.info("CustomerJwtFilter destroyed");
    }
}