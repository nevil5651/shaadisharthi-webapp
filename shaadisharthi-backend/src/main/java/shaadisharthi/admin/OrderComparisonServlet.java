package shaadisharthi.admin;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * OrderComparisonServlet - Year-over-year booking analytics by service category
 * 
 * Provides comparative order data for business intelligence:
 * - Current year vs previous year booking counts
 * - Category-wise breakdown of completed/accepted orders
 * - Data formatted for frontend chart visualization
 * - JWT-protected admin access
 * 
 * Endpoint: /ordercomparison
 * Method: GET
 * 
 * @category Analytics & Reporting
 * @security JWT Token required (admin role)
 */
@WebServlet("/ordercomparison")
public class OrderComparisonServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(OrderComparisonServlet.class);

    /**
     * Send standardized error response
     * 
     * @param response HttpServletResponse object
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

    /**
     * GET /ordercomparison - Retrieve year-over-year booking comparison by category
     * 
     * Returns structured data for chart visualization:
     * - categories: Array of service categories
     * - currentYearData: Order counts for current year per category
     * - previousYearData: Order counts for previous year per category
     * 
     * Only includes bookings with status 'Accepted' or 'Completed'
     * 
     * @param request HttpServletRequest (no parameters required)
     * @param response JSON with comparative order data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for order comparison");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Authenticate admin using JWT claims
        Claims claims = (Claims) request.getAttribute("claims");
        Integer adminId;
        try {
            adminId = Integer.valueOf(claims.getSubject());
            logger.debug("Authenticated admin ID: {}", adminId);
        } catch (NumberFormatException e) {
            logger.error("Invalid admin ID in token: {}", claims.getSubject());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
            return;
        }

        try (Connection con = DbConnection.getCon();
             PrintWriter out = response.getWriter()) {
            
            // Get current year orders by category (only Accepted or Completed status)
            Map<String, Integer> currentYearOrders = getOrdersByCategory(con, "YEAR(CURDATE())");
            // Get previous year orders by category (only Accepted or Completed status)
            Map<String, Integer> previousYearOrders = getOrdersByCategory(con, "YEAR(CURDATE()) - 1");
            
            // Get all unique categories from both years using a Set
            Set<String> allCategories = new HashSet<>();
            allCategories.addAll(currentYearOrders.keySet());
            allCategories.addAll(previousYearOrders.keySet());
            
            // Prepare arrays for chart data
            JSONArray categoriesArray = new JSONArray();
            JSONArray currentYearData = new JSONArray();
            JSONArray previousYearData = new JSONArray();
            
            // Create arrays for chart data (consistent category order)
            for (String category : allCategories) {
                categoriesArray.put(category);
                currentYearData.put(currentYearOrders.getOrDefault(category, 0));
                previousYearData.put(previousYearOrders.getOrDefault(category, 0));
            }
            
            // Build JSON response
            JSONObject json = new JSONObject();
            json.put("categories", categoriesArray);
            json.put("currentYearData", currentYearData);
            json.put("previousYearData", previousYearData);
            
            logger.info("Successfully fetched order comparison for admin ID: {}", adminId);
            out.print(json.toString());
            out.flush();
        } catch (SQLException e) {
            logger.error("Database error while fetching order comparison: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Retrieve order counts by service category for specified year
     * 
     * @param con Database connection
     * @param yearCondition SQL year condition (e.g., "YEAR(CURDATE())" or "YEAR(CURDATE()) - 1")
     * @return Map of category names to order counts
     * @throws SQLException If database query fails
     */
    private Map<String, Integer> getOrdersByCategory(Connection con, String yearCondition) throws SQLException {
        Map<String, Integer> ordersByCategory = new HashMap<>();
        
        // Query to get order counts by category for specified year
        String query = "SELECT s.category, COUNT(b.booking_id) as order_count " +
                       "FROM bookings b " +
                       "JOIN services s ON b.service_id = s.service_id " +
                       "WHERE YEAR(b.booking_date) = " + yearCondition + " " +
                       "AND b.status IN ('Accepted', 'Completed') " + // Only count Accepted or Completed bookings
                       "GROUP BY s.category";
        
        try (PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            // Process results and populate map
            while (rs.next()) {
                String category = rs.getString("category");
                if (category != null && !category.trim().isEmpty()) {
                    int orderCount = rs.getInt("order_count");
                    ordersByCategory.put(category, orderCount);
                }
            }
        }
        
        return ordersByCategory;
    }
}