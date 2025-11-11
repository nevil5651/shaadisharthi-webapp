package shaadisharthi.security;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.Claims;
import shaadisharthi.utils.ConfigUtil;

/**
 * JwtFilter - JWT authentication and authorization for Admin and Service Provider endpoints
 * 
 * Provides comprehensive security for protected endpoints:
 * - JWT token validation from Authorization header
 * - Role-based access control (RBAC)
 * - Public endpoint whitelisting
 * - Dynamic role configuration from environment/config
 * - Token expiration handling
 * 
 * Applied to /admin/* and /ServiceProvider/* endpoints
 * 
 * @category Security & Authentication
 */
@WebFilter(urlPatterns = {"/admin/*", "/ServiceProvider/*"})
public class JwtFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Override
    public void init(javax.servlet.FilterConfig filterConfig) throws ServletException {
        logger.info("JwtFilter initialized");
        // ConfigUtil handles loading config.properties and prefers environment variables
    }

    /**
     * Filter requests for JWT authentication and authorization
     * 
     * Process flow:
     * 1. Check if endpoint is public (skip authentication)
     * 2. Extract and validate JWT from Authorization header
     * 3. Parse token and check expiration
     * 4. Verify user role against endpoint requirements
     * 5. Set claims attribute for downstream processing
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

        String authHeader = httpRequest.getHeader("Authorization");
        String servletPath = httpRequest.getServletPath();
        String contextPath = httpRequest.getContextPath();
        String fullPath = contextPath + servletPath;
        String method = httpRequest.getMethod();

        logger.debug("Processing request for fullPath: {}, servletPath: {}, method: {}", fullPath, servletPath, method);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(servletPath, method)) {
            logger.debug("Skipping authentication for public endpoint: {}", servletPath);
            chain.doFilter(request, response);
            return;
        }

        // Validate Authorization header presence and format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", fullPath);
            sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Authorization header missing or invalid");
            return;
        }

        // Extract and validate JWT token
        String token = authHeader.substring(7);
        logger.debug("Extracted token: {}", token);
        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
            if (claims == null) {
                logger.error("JwtUtil.parseToken returned null for path: {}", fullPath);
                sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
            logger.debug("Token parsed successfully for subject: {}", claims.getSubject());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.error("Token expired for path {}: {}", fullPath, e.getMessage(), e);
            sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
            return;
        } catch (Exception e) {
            logger.error("Token parsing failed for path {}: {}", fullPath, e.getMessage(), e);
            sendError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Check role-based authorization
        List<String> allowedRoles = getAllowedRoles(servletPath, method);
        if (allowedRoles == null) {
            logger.warn("No roles defined for path: {}, method: {}", fullPath, method);
            sendError(httpResponse, HttpServletResponse.SC_FORBIDDEN, "No roles configured for this endpoint");
            return;
        }
        if (!hasRequiredRole(claims, allowedRoles, httpResponse)) {
            return;
        }

        // Token valid and authorized - pass to application
        logger.debug("Setting claims attribute for subject: {}", claims.getSubject());
        httpRequest.setAttribute("claims", claims);

        chain.doFilter(request, response);
    }

    /**
     * Check if endpoint is publicly accessible (no authentication required)
     * 
     * @param servletPath Request servlet path
     * @param method HTTP method
     * @return true if endpoint is public, false otherwise
     */
    private boolean isPublicEndpoint(String servletPath, String method) {
        return Set.of(
            "/admin/adminlogin",
            "/ServiceProvider/login",
            "/ServiceProvider/reset-password",
            "/ServiceProvider/forgot-password",
            "/ServiceProvider/verify-email",
            "/ServiceProvider/email-verification",
            "/ServiceProvider/register",
            "/admin/admin-auth",
            "/Customer/signin",
            "/Customer/services/*",
            "/Customer/services"
        ).contains(servletPath);
    }

    /**
     * Get allowed roles for specific endpoint and method from configuration
     * 
     * Configuration keys follow pattern: endpoint.method.roles
     * Example: "dashboard.get.roles" = "admin,super_admin"
     * 
     * @param servletPath Request servlet path
     * @param method HTTP method
     * @return List of allowed roles, or null if no roles configured
     */
    private List<String> getAllowedRoles(String servletPath, String method) {
        String endpoint;
        if (servletPath.startsWith("/admin/")) {
            endpoint = servletPath.replaceFirst("^/admin/", "");
        } else if (servletPath.startsWith("/ServiceProvider/")) {
            endpoint = servletPath.replaceFirst("^/ServiceProvider/", "");
        } else {
            return null;
        }
        String key = endpoint + "." + method.toLowerCase() + ".roles";
        String roles = ConfigUtil.get(key, key.toUpperCase().replace(".", "_"));
        if (roles == null) {
            roles = ""; // Fallback to empty string if missing
        }
        logger.debug("Roles for key {}: {}", key, roles);
        return roles.isEmpty() ? null : Arrays.asList(roles.split(","));
    }

    /**
     * Verify user has required role for endpoint access
     * 
     * @param claims JWT claims containing user role
     * @param allowedRoles List of roles permitted for this endpoint
     * @param response HTTP response for error handling
     * @return true if role authorized, false otherwise
     * @throws IOException If error response writing fails
     */
    private boolean hasRequiredRole(Claims claims, List<String> allowedRoles, HttpServletResponse response)
            throws IOException {
        String role = claims.get("role", String.class);
        if (role == null || !allowedRoles.contains(role)) {
            logger.warn("Access denied for role: {} on endpoint, allowed roles: {}", role, allowedRoles);
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Insufficient Role");
            return false;
        }
        logger.debug("Role {} authorized for endpoint", role);
        return true;
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
        logger.info("JwtFilter destroyed");
    }
}