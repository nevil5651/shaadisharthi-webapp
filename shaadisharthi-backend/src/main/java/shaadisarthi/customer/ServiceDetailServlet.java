package shaadisarthi.customer;

import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import shaadisarthi.cache.RedisClient;
import shaadisharthi.security.JwtUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet implementation for detailed service information
 * Provides comprehensive service details including media and reviews with optimized caching
 * 
 * @WebServlet Maps to "/service-detail/*" endpoint with path parameters
 * @version 1.0
 * @description Handles service detail pages with media galleries and review sections
 */
@WebServlet(urlPatterns = {"/service-detail/*"})
public class ServiceDetailServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDetailServlet.class);
    // Rate limiter to prevent abuse (100 requests per second)
    private static final RateLimiter rateLimiter = RateLimiter.create(100.0);
    private static final int CACHE_TTL_SECONDS = 300; // 5 minutes cache expiry
    private static final JedisPool jedisPool = RedisClient.getJedisPool();
    private final ServiceDAO serviceDAO = new ServiceDAO();

    /**
     * Handles GET requests for service details and reviews
     * Routes based on URL path to appropriate handlers
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        // Check rate limiting for all requests
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new JSONObject().put("error", "Too many requests").toString());
            return;
        }

        Jedis jedis = null;
        try {
            if (jedisPool != null) {
                jedis = jedisPool.getResource();
            }

            // Route based on URL path structure
            if (pathInfo == null || pathInfo.equals("/")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(new JSONObject().put("error", "Service ID is required").toString());
            } else if (pathInfo.matches("^/\\d+$")) {
                handleGetServiceDetails(request, response, jedis, out);
            } else if (pathInfo.matches("^/\\d+/reviews$")) {
                handleGetServiceReviews(request, response, jedis, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(new JSONObject().put("error", "Endpoint not found").toString());
            }
        } catch (Exception e) {
            LOGGER.error("Error processing request: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(new JSONObject().put("error", "Server error").toString());
        } finally {
            if (jedis != null) jedis.close();
        }
    }

    /**
     * Handles service detail page with comprehensive information
     * 
     * @param request HttpServletRequest with service ID in path
     * @param response HttpServletResponse
     * @param jedis Redis connection for caching
     * @param out PrintWriter for response output
     * @throws IOException if I/O operations fail
     */
    private void handleGetServiceDetails(HttpServletRequest request, HttpServletResponse response, Jedis jedis, PrintWriter out)
            throws IOException {
        int serviceId = parseServiceId(request.getPathInfo());
        if (serviceId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(new JSONObject().put("error", "Invalid service ID").toString());
            return;
        }

        String cacheKey = "service:" + serviceId;
        JSONObject serviceJson = null;
        
        // Check Redis cache first
        if (jedis != null) {
            String cached = jedis.get(cacheKey);
            if (cached != null) {
                LOGGER.info("Cache hit for service: {}", cacheKey);
                serviceJson = new JSONObject(cached);
            }
        }

        // Fetch from database if not in cache
        if (serviceJson == null) {
            Service service = serviceDAO.getServiceById(serviceId);
            if (service == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(new JSONObject().put("error", "Service not found").toString());
                return;
            }

            // Build comprehensive service JSON with media
            serviceJson = new JSONObject()
                    .put("serviceId", service.getServiceId())
                    .put("serviceName", service.getServiceName())
                    .put("description", service.getDescription())
                    .put("media", buildMediaArray(serviceDAO.getMediaByServiceId(serviceId)))
                    .put("businessName", service.getBusinessName())
                    .put("price", service.getPrice())
                    .put("email", service.getEmail())
                    .put("phone", service.getPhone())
                    .put("rating", service.getRating())
                    .put("reviewCount", service.getReviewCount());
            
            // Cache the result
            if (jedis != null) {
                jedis.setex(cacheKey, CACHE_TTL_SECONDS, serviceJson.toString());
                LOGGER.info("Cached service for key: {}", cacheKey);
            }
        }

        // Set cache headers for browser caching
        response.setHeader("Cache-Control", "max-age=" + CACHE_TTL_SECONDS + ", public");
        out.write(serviceJson.toString());
    }

    /**
     * Handles service reviews with pagination
     * 
     * @param request HttpServletRequest with service ID and pagination parameters
     * @param response HttpServletResponse
     * @param jedis Redis connection for caching
     * @param out PrintWriter for response output
     * @throws IOException if I/O operations fail
     */
    private void handleGetServiceReviews(HttpServletRequest request, HttpServletResponse response, Jedis jedis, PrintWriter out)
            throws IOException {
        int serviceId = parseServiceId(request.getPathInfo().replace("/reviews", ""));
        int page = parseInt(request.getParameter("page"), 1);
        int limit = parseInt(request.getParameter("limit"), 10);

        // Validate input parameters
        if (serviceId <= 0 || page < 1 || limit < 1 || limit > 50) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(new JSONObject().put("error", "Invalid parameters").toString());
            return;
        }

        String cacheKey = String.format("reviews:%d:%d:%d", serviceId, page, limit);
        JSONObject reviewsJson = null;
        
        // Check Redis cache first
        if (jedis != null) {
            String cached = jedis.get(cacheKey);
            if (cached != null) {
                LOGGER.info("Cache hit for reviews: {}", cacheKey);
                reviewsJson = new JSONObject(cached);
            }
        }

        // Fetch from database if not in cache
        if (reviewsJson == null) {
            List<Review> reviews = serviceDAO.getServiceReviews(serviceId, page, limit);
            long totalCount = serviceDAO.getServiceReviewsCount(serviceId);
            boolean hasMore = (page * limit) < totalCount;

            reviewsJson = new JSONObject()
                    .put("reviews", buildReviewsArray(reviews))
                    .put("totalCount", totalCount)
                    .put("hasMore", hasMore);
            
            // Cache the result
            if (jedis != null) {
                jedis.setex(cacheKey, CACHE_TTL_SECONDS, reviewsJson.toString());
                LOGGER.info("Cached reviews for key: {}", cacheKey);
            }
        }

        // Disable browser caching for dynamic review data
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        out.println(reviewsJson.toString());
    }

    // Helper methods for data parsing and conversion
    private int parseServiceId(String pathInfo) {
        try {
            return Integer.parseInt(pathInfo.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Builds JSON array from media list
     * 
     * @param mediaList List of Media objects
     * @return JSONArray containing media data
     */
    private JSONArray buildMediaArray(List<Media> mediaList) {
        JSONArray mediaArray = new JSONArray();
        for (Media media : mediaList) {
            JSONObject mediaObj = new JSONObject()
                    .put("type", media.getMediaType())
                    .put("url", media.getMediaUrl());
            mediaArray.put(mediaObj);
        }
        return mediaArray;
    }

    /**
     * Builds JSON array from reviews list
     * 
     * @param reviews List of Review objects
     * @return JSONArray containing review data
     */
    private JSONArray buildReviewsArray(List<Review> reviews) {
        JSONArray reviewsArray = new JSONArray();
        for (Review review : reviews) {
            JSONObject reviewObj = new JSONObject()
                    .put("id", review.getReviewId())
                    .put("rating", review.getRating())
                    .put("text", review.getReviewText())
                    .put("createdAt", review.getCreatedAt() != null ? review.getCreatedAt().toInstant().toString() : null)
                    .put("customerName", review.getCustomerName());
            reviewsArray.put(reviewObj);
        }
        return reviewsArray;
    }
}