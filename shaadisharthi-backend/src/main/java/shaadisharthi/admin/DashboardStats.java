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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;
import io.jsonwebtoken.Claims;

/**
 * Dashboard Stats Servlet - Platform Analytics and Metrics
 * 
 * Provides high-level platform statistics for admin dashboard including:
 * - Total number of registered customers
 * - Total number of service providers
 * 
 * Use Cases:
 * - Admin dashboard overview
 * - Platform growth tracking
 * - User base analytics
 * 
 * Security: JWT-protected endpoint for admin access only
 * Performance: Simple COUNT queries optimized for dashboard loading
 */
@WebServlet("/dashboardstats")
public class DashboardStats extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DashboardStats.class);

    /**
     * Standardized error response handler
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param message Error message for client
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
     * GET endpoint - Retrieves platform statistics for admin dashboard
     * 
     * Returns JSON with:
     * - totalCustomers: Count of all registered customers
     * - totalProviders: Count of all registered service providers
     * 
     * Future Enhancements:
     * - Active vs inactive user counts
     * - Registration trends over time
     * - Geographic distribution
     * 
     * @param request HttpServletRequest with JWT token
     * @param response HttpServletResponse with JSON statistics
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for dashboard stats");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract and validate admin ID from JWT claims
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
            // Fetch platform statistics
            int totalCustomers = getTotalCustomers(con);
            int totalProviders = getTotalProviders(con);

            // Build statistics response
            JSONObject json = new JSONObject();
            json.put("totalCustomers", totalCustomers);
            json.put("totalProviders", totalProviders);

            logger.info("Successfully fetched dashboard stats for admin ID: {}", adminId);
            out.print(json.toString());
            out.flush();
        } catch (SQLException e) {
            logger.error("Database error while fetching dashboard stats: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Retrieves total count of registered customers
     * 
     * @param con Database connection
     * @return Total number of customers in the system
     * @throws SQLException if database query fails
     */
    private static int getTotalCustomers(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM customers");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Retrieves total count of registered service providers
     * 
     * @param con Database connection
     * @return Total number of service providers in the system
     * @throws SQLException if database query fails
     */
    private static int getTotalProviders(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM service_providers");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}