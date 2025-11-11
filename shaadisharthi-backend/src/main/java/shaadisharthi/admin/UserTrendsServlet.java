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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * UserTrendsServlet - Analytics and reporting for user growth trends
 * 
 * Provides monthly user registration data for:
 * - Customer registrations (joining trends)
 * - Service provider registrations (joining trends)
 * - Estimated user churn/leaving patterns
 * 
 * Used for admin dashboard analytics and business intelligence
 * 
 * Endpoint: /usertrends
 * Method: GET
 * 
 * @category Analytics & Reporting
 * @security JWT Token required (admin role)
 */
@WebServlet("/usertrends")
public class UserTrendsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(UserTrendsServlet.class);

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
     * GET /usertrends - Retrieve user growth and churn analytics
     * 
     * Returns monthly data arrays for:
     * - customerJoining: New customer registrations per month
     * - customerLeaving: Estimated customer churn per month
     * - providerJoining: New provider registrations per month
     * - providerLeaving: Estimated provider churn per month
     * 
     * @param request HttpServletRequest (no parameters required)
     * @param response JSON with four arrays of monthly data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for user trends");
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
            
            // Get monthly customer registrations from database
            List<Integer> customerJoining = getMonthlyRegistrations(con, "customers", "created_at");
            // Get monthly provider registrations from database
            List<Integer> providerJoining = getMonthlyRegistrations(con, "service_providers", "created_at");
            
            // Estimate leaving users based on historical data (simulated churn)
            List<Integer> customerLeaving = estimateLeavingUsers(customerJoining);
            List<Integer> providerLeaving = estimateLeavingUsers(providerJoining);
            
            // Build JSON response with all trend data
            JSONObject json = new JSONObject();
            json.put("customerJoining", new JSONArray(customerJoining));
            json.put("customerLeaving", new JSONArray(customerLeaving));
            json.put("providerJoining", new JSONArray(providerJoining));
            json.put("providerLeaving", new JSONArray(providerLeaving));
            
            logger.info("Successfully fetched user trends for admin ID: {}", adminId);
            out.print(json.toString());
            out.flush();
        } catch (SQLException e) {
            logger.error("Database error while fetching user trends: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Retrieve monthly registration counts from specified table
     * 
     * @param con Database connection
     * @param table Table name to query (customers or service_providers)
     * @param dateColumn Date column for registration timestamp
     * @return List of 12 integers representing monthly counts (January to December)
     * @throws SQLException If database query fails
     */
    private List<Integer> getMonthlyRegistrations(Connection con, String table, String dateColumn) throws SQLException {
        List<Integer> monthlyData = new ArrayList<>();
        
        // Query to get monthly registration counts for current year
        String query = "SELECT MONTH(" + dateColumn + ") as month, COUNT(*) as count " +
                       "FROM " + table + " " +
                       "WHERE YEAR(" + dateColumn + ") = YEAR(CURDATE()) " +
                       "GROUP BY MONTH(" + dateColumn + ") " +
                       "ORDER BY month";
        
        try (PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            // Initialize array with 12 months (0 values for months with no data)
            for (int i = 0; i < 12; i++) {
                monthlyData.add(0);
            }
            
            // Fill in actual values from query results
            while (rs.next()) {
                int month = rs.getInt("month") - 1; // Convert to 0-based index for array
                int count = rs.getInt("count");
                monthlyData.set(month, count);
            }
        }
        
        return monthlyData;
    }
    
    /**
     * Estimate user churn based on historical registration data
     * 
     * Uses simplified churn model:
     * - Assumes 20-30% of users who joined 3 months ago are no longer active
     * - No churn data for first 3 months (insufficient history)
     * - Random variation to simulate real-world patterns
     * 
     * Note: In production, this would use actual activity data
     * 
     * @param joiningData Monthly registration data
     * @return List of 12 integers representing estimated monthly churn
     */
    private List<Integer> estimateLeavingUsers(List<Integer> joiningData) {
        // Simple estimation: assume 20-30% of users who joined 3 months ago are no longer active
        List<Integer> leavingData = new ArrayList<>();
        
        for (int i = 0; i < 12; i++) {
            if (i >= 3) {
                // For months where we have historical data, estimate churn
                int churnRate = 20 + (int)(Math.random() * 11); // 20-30% random churn rate
                int leavingCount = (int)(joiningData.get(i-3) * churnRate / 100);
                leavingData.add(leavingCount);
            } else {
                // For first 3 months, no churn data yet (users haven't been around long enough)
                leavingData.add(0);
            }
        }
        
        return leavingData;
    }
}