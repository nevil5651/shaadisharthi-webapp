package shaadisarthi.customer;

import com.google.common.util.concurrent.RateLimiter;
import io.jsonwebtoken.Claims;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Servlet implementation for customer review management
 * Handles creation of service reviews with rate limiting and validation
 * 
 * @WebServlet Maps to "/Customer/reviews/*" endpoint with path parameters
 * @version 1.0
 * @description Processes POST requests for adding customer reviews to services
 */
@WebServlet(urlPatterns = "/Customer/reviews/*")
public class AddReviewServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddReviewServlet.class);
    // Rate limiter to prevent abuse (100 requests per second)
    private static final RateLimiter rateLimiter = RateLimiter.create(100.0);
    private static final int CACHE_TTL_SECONDS = 300;
    private final ServiceDAO serviceDAO = new ServiceDAO();

    /**
     * Processes POST requests for adding customer reviews
     * Validates JWT claims, rate limits, and stores reviews in database
     * 
     * @param request HttpServletRequest containing review data and service ID in path
     * @param response HttpServletResponse for sending review creation result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Rate limiting check to prevent abuse
        if (!rateLimiter.tryAcquire()) {
            LOGGER.warn("Rate limit exceeded");
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            out.println(new JSONObject().put("error", "Too many requests").toString());
            return;
        }

        // Extract and validate JWT claims for customer authentication
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            LOGGER.warn("Missing or invalid JWT claims");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println(new JSONObject().put("error", "Unauthorized").toString());
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(claims.getSubject());
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid customer ID in JWT: {}", claims.getSubject());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.println(new JSONObject().put("error", "Invalid customer ID").toString());
            return;
        }

        // Parse service ID from URL path (e.g., /Customer/reviews/123)
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            LOGGER.warn("Invalid path: {}", pathInfo);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(new JSONObject().put("error", "Invalid service ID").toString());
            return;
        }
        int serviceId = Integer.parseInt(pathInfo.substring(1));

        // Read and parse request body
        String requestBody = readRequestBody(request);
        JSONObject jsonRequest;
        try {
            jsonRequest = new JSONObject(requestBody);
        } catch (Exception e) {
            LOGGER.warn("Invalid JSON body: {}", requestBody);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(new JSONObject().put("error", "Invalid request body").toString());
            return;
        }

        // Extract and validate review data
        String reviewText = jsonRequest.optString("reviewText", null);
        int rating = jsonRequest.optInt("rating", 0);
        if (reviewText == null || reviewText.trim().isEmpty() || reviewText.length() > 500) {
            LOGGER.warn("Invalid reviewText: length={}", reviewText != null ? reviewText.length() : 0);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(new JSONObject().put("error", "Review text must be between 1 and 500 characters").toString());
            return;
        }
        if (rating < 1 || rating > 5) {
            LOGGER.warn("Invalid rating: {}", rating);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(new JSONObject().put("error", "Rating must be between 1 and 5").toString());
            return;
        }

        // Build Review object from validated data
        Review review = new Review();
        review.setServiceId(serviceId);
        review.setCustomerId(customerId);
        review.setReviewText(reviewText.trim());
        review.setRating(rating);

        // Persist review and retrieve updated review list
        try {
            List<Review> updated = serviceDAO.addReviewAndGetPage(review, 1, 10);

            // Convert review list to JSON response
            JSONArray arr = new JSONArray();
            for (Review r : updated) {
                JSONObject o = new JSONObject();
                o.put("reviewId", r.getReviewId());
                o.put("serviceId", r.getServiceId());
                o.put("customerId", r.getCustomerId());
                o.put("customerName", r.getCustomerName());
                o.put("reviewText", r.getReviewText());
                o.put("rating", r.getRating());
                o.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().getTime() : JSONObject.NULL);
                arr.put(o);
            }

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.println(arr.toString());
        } catch (Exception e) {
            LOGGER.error("Error adding review: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(new JSONObject().put("error", "Server error: " + e.getMessage()).toString());
        }
    }

    /**
     * Reads the entire request body into a String
     * 
     * @param request HttpServletRequest to read from
     * @return String containing the complete request body
     * @throws IOException if reading the request body fails
     */
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }
        return buffer.toString();
    }
}