package shaadisharthi.filters;

import com.google.common.util.concurrent.RateLimiter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * RateLimitFilter - Application-level rate limiting with dual-layer protection
 * 
 * Implements comprehensive rate limiting strategy:
 * - Global rate limiting (500 requests/second across all clients)
 * - Per-client rate limiting (5 requests/second per client)
 * - Uses Guava RateLimiter for efficient token bucket algorithm
 * - Protects against DDoS attacks and API abuse
 * 
 * Applied to all endpoints ("/*") for comprehensive protection
 * 
 * @category Security & Performance
 * @threading Thread-safe with ConcurrentHashMap for client tracking
 */
@WebFilter("/*")  
public class RateLimitFilter implements Filter {

    // Global limit (optional) - protects overall system capacity
    private final RateLimiter globalRateLimiter = RateLimiter.create(500.0); // 500 req/sec total

    // Per-client limiters - tracks individual client rates
    private final ConcurrentHashMap<String, RateLimiter> clientLimiters = new ConcurrentHashMap<>();

    /**
     * Get or create rate limiter for specific client
     * 
     * @param clientId Unique client identifier (IP address)
     * @return RateLimiter instance for the client (5 requests/second)
     */
    private RateLimiter getClientLimiter(String clientId) {
        return clientLimiters.computeIfAbsent(clientId, id -> RateLimiter.create(5.0)); 
        // each client 5 req/sec
    }

    /**
     * Filter incoming requests and apply rate limiting checks
     * 
     * Two-tier rate limiting:
     * 1. Global rate limit check (500 req/sec)
     * 2. Per-client rate limit check (5 req/sec)
     * 
     * Returns HTTP 429 (Too Many Requests) when limits exceeded
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

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String clientId = getClientIdentifier(httpReq);

        // 1. Global check - protect overall system capacity
        if (!globalRateLimiter.tryAcquire(1, 10, TimeUnit.MILLISECONDS)) {
            reject(httpResp, "Too many requests (global limit)");
            return;
        }

        // 2. Per-client check - prevent individual client abuse
        RateLimiter clientLimiter = getClientLimiter(clientId);
        if (!clientLimiter.tryAcquire(1, 10, TimeUnit.MILLISECONDS)) {
            reject(httpResp, "Too many requests (per-client limit)");
            return;
        }

        // Passed both rate limiting checks → continue to application
        chain.doFilter(request, response);
    }

    /**
     * Extract client identifier for rate limiting
     * 
     * Uses client IP address as identifier (fallback when no API key/JWT available)
     * In production, consider using API keys or JWT user IDs for more accurate identification
     * 
     * @param request HTTP request
     * @return Client identifier string (IP address)
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Best: use API key / JWT userId
        // Fallback: client IP
        String ip = request.getRemoteAddr();
        return ip;
    }

    /**
     * Send rate limit exceeded response
     * 
     * @param resp HTTP response
     * @param message Error message describing the limit exceeded
     * @throws IOException If response writing fails
     */
    private void reject(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(429); // Too Many Requests
        resp.setContentType("application/json");
        resp.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}