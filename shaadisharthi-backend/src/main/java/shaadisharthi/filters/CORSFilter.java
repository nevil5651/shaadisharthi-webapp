package shaadisharthi.filters;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import shaadisharthi.utils.ConfigUtil;

/**
 * CORSFilter - Cross-Origin Resource Sharing configuration with path-based origins
 * 
 * Provides flexible CORS configuration for different application modules:
 * - Admin endpoints: Separate allowed origins
 * - Service Provider endpoints: Separate allowed origins  
 * - Customer endpoints: Separate allowed origins
 * - Dynamic configuration from environment variables/config files
 * - Preflight request handling
 * 
 * Applied to all endpoints ("/*") with path-based origin matching
 * 
 * @category Security & Web Standards
 */
@WebFilter("/*")
public class CORSFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(CORSFilter.class.getName());
    // Stores allowed origins mapped by base path
    private Map<String, List<String>> allowedOriginsByPath;

    /**
     * Initialize CORS filter with configuration from environment/config
     * 
     * Loads allowed origins for three application modules:
     * - Admin interfaces
     * - Service Provider portals  
     * - Customer applications
     * 
     * @param filterConfig Filter configuration
     * @throws ServletException If configuration loading fails
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialize map to store allowed origins for different paths
        allowedOriginsByPath = new HashMap<>();
        
        try {
            // Load admin origins from configuration
            String adminOrigins = ConfigUtil.get("admin.allowed.origins", "ADMIN_ALLOWED_ORIGINS");
            if (adminOrigins == null) {
                adminOrigins = ""; // Fallback to empty string if missing
            }
            allowedOriginsByPath.put("/admin", Arrays.asList(adminOrigins.split(",")));
            
            // Load service provider origins from configuration
            String providerOrigins = ConfigUtil.get("serviceprovider.allowed.origins", "SERVICEPROVIDER_ALLOWED_ORIGINS");
            if (providerOrigins == null) {
                providerOrigins = ""; // Fallback to empty string if missing
            }
            allowedOriginsByPath.put("/ServiceProvider", Arrays.asList(providerOrigins.split(",")));
            
            // Load customer origins from configuration
            String customerOrigins = ConfigUtil.get("customer.allowed.origins", "CUSTOMER_ALLOWED_ORIGINS");
            if (customerOrigins == null) {
                customerOrigins = ""; // Fallback to empty string if missing
            }
            allowedOriginsByPath.put("/Customer", Arrays.asList(customerOrigins.split(",")));
            
            LOGGER.info("Loaded admin origins: " + adminOrigins);
            LOGGER.info("Loaded service provider origins: " + providerOrigins);
            LOGGER.info("Loaded customer origins: " + customerOrigins);
        } catch (Exception e) {
            throw new ServletException("Failed to initialize CORSFilter configs", e);
        }
    }

    /**
     * Apply CORS headers based on request path and origin
     * 
     * Determines allowed origins by request path prefix:
     * - /admin paths → admin allowed origins
     * - /ServiceProvider paths → service provider allowed origins  
     * - /Customer paths → customer allowed origins
     * 
     * Handles preflight OPTIONS requests and applies appropriate CORS headers
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

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String origin = httpRequest.getHeader("Origin");
        
        LOGGER.info("Request URI: " + requestURI + ", Origin: " + origin);

        List<String> allowedOrigins = null;
        String relativePath = requestURI.substring(contextPath.length());
        
        // Determine allowed origins based on request URI path prefix
        if (relativePath.startsWith("/admin")) {
            allowedOrigins = allowedOriginsByPath.get("/admin");
            LOGGER.info("Matched admin path, allowed origins: " + allowedOrigins);
        } else if (relativePath.startsWith("/ServiceProvider")) {
            allowedOrigins = allowedOriginsByPath.get("/ServiceProvider");
            LOGGER.info("Matched service provider path, allowed origins: " + allowedOrigins);
        } else if (relativePath.startsWith("/Customer")) {
            allowedOrigins = allowedOriginsByPath.get("/Customer");
            LOGGER.info("Matched customer path, allowed origins: " + allowedOrigins);
        } else {
            LOGGER.warning("No matching path for URI: " + requestURI);
        }

        // Apply CORS headers if origin is allowed for this path
        if (allowedOrigins != null && origin != null && allowedOrigins.contains(origin.trim())) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            LOGGER.info("CORS headers set for origin: " + origin);
        } else {
            LOGGER.warning("CORS headers not set. Origin: " + origin + ", Allowed: " + allowedOrigins);
        }

        // Handle OPTIONS requests (preflight) - return immediately
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            LOGGER.info("Handled OPTIONS preflight request");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Clean up filter resources
     */
    @Override
    public void destroy() {
        // Cleanup if needed
        allowedOriginsByPath.clear();
        LOGGER.info("CORSFilter destroyed");
    }
}