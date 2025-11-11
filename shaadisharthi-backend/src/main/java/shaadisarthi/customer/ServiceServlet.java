package shaadisarthi.customer;

import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import shaadisarthi.cache.RedisClient;
import shaadisharthi.security.JwtUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Main servlet for service-related operations
 * Handles service listing, details, reviews, and review creation with comprehensive routing
 * 
 * @WebServlet Maps to multiple endpoints: "/services", "/services/*"
 * @version 1.0
 * @description Unified servlet for all service operations with rate limiting and caching
 */
@WebServlet(urlPatterns = {"/services", "/services/*"})
public class ServiceServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceServlet.class);
    // Rate limiter to prevent abuse (100 requests per second)
    private static final RateLimiter rateLimiter = RateLimiter.create(100.0);
    private static final int CACHE_TTL_SECONDS = 300; // 5 minutes cache expiry
    private final ServiceDAO serviceDAO = new ServiceDAO();

    /**
     * Handles GET requests for service operations
     * Routes to appropriate handlers based on URL path
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        // Check rate limiting for all GET requests
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded");
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            out.write(new JSONObject().put("error", "Too many requests").toString());
            return;
        }

        Jedis jedis = null;
        try {
            if (RedisClient.getJedisPool() != null) {
                jedis = RedisClient.getJedisPool().getResource();
            }
            
            // Route based on URL path
            if (pathInfo == null || "/services".equals(pathInfo)) {
                handleGetServices(request, response, jedis, out);
            } else if (pathInfo.matches("/services/\\d+")) {
                handleGetServiceById(request, response, jedis, out);
            } else if (pathInfo.matches("/services/\\d+/reviews")) {
                handleGetServiceReviews(request, response, jedis, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(new JSONObject().put("error", "Endpoint not found").toString());
            }
        } catch (Exception e) {
            LOGGER.error("Error in doGet: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(new JSONObject().put("error", "Server error").toString());
        } finally {
            if (jedis != null) jedis.close();
        }
    }

    /**
     * Handles POST requests for service operations
     * Currently supports review creation
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        // Check rate limiting for all POST requests
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded");
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            out.write(new JSONObject().put("error", "Too many requests").toString());
            return;
        }

        Jedis jedis = null;
        try {
            if (RedisClient.getJedisPool() != null) {
                jedis = RedisClient.getJedisPool().getResource();
            }
            
            // Route based on URL path - currently only reviews creation
            if (pathInfo != null && pathInfo.matches("/services/\\d+/reviews")) {
                handleAddReview(request, response, jedis, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(new JSONObject().put("error", "Endpoint not found").toString());
            }
        } catch (Exception e) {
            LOGGER.error("Error in doPost: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(new JSONObject().put("error", "Server error").toString());
        } finally {
            if (jedis != null) jedis.close();
        }
    }

    /**
     * Handles filtered service listing with pagination
     * 
     * @param request HttpServletRequest with filter parameters
     * @param response HttpServletResponse
     * @param jedis Redis connection for caching
     * @param out PrintWriter for response output
     * @throws IOException if I/O operations fail
     */
    private void handleGetServices(HttpServletRequest request, HttpServletResponse response, Jedis jedis, PrintWriter out)
            throws IOException {
        // Extract and parse filter parameters
        String category = request.getParameter("category");
        String location = request.getParameter("location");
        Double minPrice = parseDouble(request.getParameter("minPrice"));
        Double maxPrice = parseDouble(request.getParameter("maxPrice"));
        Integer rating = parseInt(request.getParameter("rating"));
        String sortBy = request.getParameter("sortBy") != null ? request.getParameter("sortBy") : "popular";
        int page = parseInt(request.getParameter("page"), 1); // Default to page 1
        int limit = parseInt(request.getParameter("limit"), 12); // Default to 12 items

        // Validate pagination parameters
        if (page < 1 || limit < 1 || limit > 100) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new JSONObject().put("error", "Invalid page or limit").toString());
            return;
        }

        // Fetch filtered services and total count
        List<Service> services = serviceDAO.getFilteredServices(category, location, minPrice, maxPrice, rating, sortBy, page, limit);
        long totalCount = serviceDAO.getFilteredServicesCount(category, location, minPrice, maxPrice, rating);
        boolean hasMore = (page * limit) < totalCount; // Calculate pagination metadata

        // Build and send response
        JSONObject responseJson = new JSONObject();
        responseJson.put("services", new JSONArray(services));
        responseJson.put("hasMore", hasMore);
        out.write(responseJson.toString());
        
        // Set cache headers for browser caching
        response.setHeader("Cache-Control", "max-age=300, public");
    }

    /**
     * Handles individual service retrieval by ID
     * 
     * @param request HttpServletRequest with service ID in path
     * @param response HttpServletResponse
     * @param jedis Redis connection for caching
     * @param out PrintWriter for response output
     * @throws IOException if I/O operations fail
     */
    private void handleGetServiceById(HttpServletRequest request, HttpServletResponse response, Jedis jedis, PrintWriter out)
            throws IOException {
        // Extract service ID from URL path
        int serviceId = Integer.parseInt(request.getPathInfo().substring(1).replace("/services/", ""));
        Service service = serviceDAO.getServiceById(serviceId);

        if (service == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write(new JSONObject().put("error", "Service not found").toString());
        } else {
            out.write(new JSONObject(service).toString());
            // Set cache headers for browser caching
            response.setHeader("Cache-Control", "max-age=300, public");
        }
    }

    /**
     * Handles service reviews retrieval with pagination
     * 
     * @param request HttpServletRequest with service ID and pagination parameters
     * @param response HttpServletResponse
     * @param jedis Redis connection for caching
     * @param out PrintWriter for response output
     * @throws IOException if I/O operations fail
     */
    private void handleGetServiceReviews(HttpServletRequest request, HttpServletResponse response, Jedis jedis, PrintWriter out)
            throws IOException {
        // Extract service ID from URL path
        int serviceId = Integer.parseInt(request.getPathInfo().replace("/reviews", "").substring(1).replace("/services/", ""));
        int page = parseInt(request.getParameter("page"), 1); // Default to page 1
        int limit = parseInt(request.getParameter("limit"), 10); // Default to 10 items

        // Validate pagination parameters
        if (page < 1 || limit < 1 || limit > 50) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new JSONObject().put("error", "Invalid page or limit").toString());
            return;
        }

        // Fetch reviews and calculate pagination metadata
        List<Review> reviews = serviceDAO.getServiceReviews(serviceId, page, limit);
        long totalCount = serviceDAO.getServiceReviewsCount(serviceId);
        boolean hasMore = (page * limit) < totalCount;

        // Build and send response
        JSONObject responseJson = new JSONObject();
        responseJson.put("reviews", new JSONArray(reviews));
        responseJson.put("hasMore", hasMore);
        out.write(responseJson.toString());
        
        // Set cache headers for browser caching
        response.setHeader("Cache-Control", "max-age=300, public");
    }

    /**
     * Handles review creation for services
     * 
     * @param request HttpServletRequest with review data
     * @param response HttpServletResponse
     * @param jedis Redis connection for caching
     * @param out PrintWriter for response output
     * @throws IOException if I/O operations fail
     */
    private void handleAddReview(HttpServletRequest request, HttpServletResponse response, Jedis jedis, PrintWriter out)
            throws IOException {
        // Extract service ID from URL path
        int serviceId = Integer.parseInt(request.getPathInfo().replace("/reviews", "").substring(1).replace("/services/", ""));
        
        // Validate JWT token for authentication
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(new JSONObject().put("error", "Authorization header missing or invalid").toString());
            return;
        }

        // Extract customer ID from JWT token
        String token = authHeader.substring(7);
        String customerId = JwtUtil.getAdminIdFromToken(token);
        
        // Parse review data from request body
        JSONObject jsonRequest = new JSONObject(readRequestBody(request));
        String reviewText = jsonRequest.getString("reviewText");
        int rating = jsonRequest.getInt("rating");

        // Validate review data
        if (reviewText == null || reviewText.trim().isEmpty() || rating < 1 || rating > 5) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new JSONObject().put("error", "Invalid review text or rating").toString());
            return;
        }

        // Create and persist review
        Review review = new Review();
        review.setServiceId(serviceId);
        review.setCustomerId(Integer.parseInt(customerId));
        review.setReviewText(reviewText);
        review.setRating(rating);

        if (serviceDAO.addReview(review)) {
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.write(new JSONObject().put("message", "Review added successfully").toString());
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new JSONObject().put("error", "Failed to add review").toString());
        }
    }

    // Helper methods for request parsing and data conversion
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    private Integer parseInt(String value) {
        return parseInt(value, null);
    }

    private Integer parseInt(String value, Integer defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : (defaultValue != null ? defaultValue : 0);
        } catch (NumberFormatException e) {
            return defaultValue != null ? defaultValue : 0;
        }
    }

    private Double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}