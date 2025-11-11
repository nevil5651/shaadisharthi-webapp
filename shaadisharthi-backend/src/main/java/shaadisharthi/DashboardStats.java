package shaadisharthi;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
 * Servlet for providing comprehensive dashboard statistics for service providers
 * Delivers business intelligence and performance metrics for provider dashboards
 * 
 * Features:
 * - Real-time business metrics (orders, revenue, customers)
 * - Multi-dimensional analytics (booking analysis, performance ratings)
 * - Financial reporting and trend analysis
 * - Calendar-based booking insights
 * 
 * @WebServlet Maps to "/providerdashboardstats" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/providerdashboardstats")
public class DashboardStats extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DashboardStats.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    /**
     * Sends standardized error responses in JSON format
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
     * Handles GET requests for provider dashboard statistics
     * Aggregates multiple metrics and analytics data into comprehensive dashboard view
     * 
     * @param request HttpServletRequest with JWT claims
     * @param response HttpServletResponse with JSON dashboard data
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for provider dashboard stats");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Extract provider ID from JWT token
        Claims claims = (Claims) request.getAttribute("claims");
        Integer providerId;
        try {
            providerId = Integer.valueOf(claims.getSubject());
            logger.debug("Authenticated provider ID: {}", providerId);
        } catch (NumberFormatException e) {
            logger.error("Invalid provider ID in token: {}", claims.getSubject());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
            return;
        }

        try (Connection con = DbConnection.getCon();
             PrintWriter out = response.getWriter()) {
            
            // Aggregate all dashboard metrics from various data sources
            int upcomingOrders = getUpcomingOrders(con, providerId);
            int totalOrdersThisMonth = getTotalOrdersThisMonth(con, providerId);
            double revenueThisMonth = getRevenueThisMonth(con, providerId);
            int customersThisYear = getCustomersThisYear(con, providerId);
//            double walletBalance = getWalletBalance(con, providerId); // Commented out - potentially deprecated
            double totalEarnings = getTotalEarningsThisYear(con, providerId);
            
            // Retrieve analytical data for charts and visualizations
            JSONObject bookingAnalysis = getBookingAnalysisData(con, providerId);
            JSONObject performanceRating = getPerformanceRatingData(con, providerId);
            JSONObject bookingCalendar = getBookingCalendarData(con, providerId);
            JSONObject financialData = getFinancialData(con, providerId);

            // Build comprehensive dashboard response
            JSONObject json = new JSONObject();
            
            // Add core business metrics
            json.put("upcomingOrders", upcomingOrders);
            json.put("totalOrdersThisMonth", totalOrdersThisMonth);
            json.put("revenueThisMonth", revenueThisMonth);
            json.put("customersThisYear", customersThisYear);
//            json.put("walletBalance", walletBalance); // Commented out
            json.put("totalEarnings", totalEarnings);
            
            // Add analytical data for visualization components
            json.put("bookingAnalysis", bookingAnalysis);
            json.put("performanceRating", performanceRating);
            json.put("bookingCalendar", bookingCalendar);
            json.put("financialData", financialData);

            logger.info("Successfully fetched dashboard stats for provider ID: {}", providerId);
            out.print(json.toString());
            out.flush();
        } catch (SQLException e) {
            logger.error("Database error while fetching provider dashboard stats: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Retrieves count of upcoming orders (future events not cancelled/completed)
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return Count of upcoming orders
     * @throws SQLException If database query fails
     */
    private int getUpcomingOrders(Connection con, int providerId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT b.booking_id) " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "AND b.event_start_date >= CURDATE() " +
                     "AND b.status NOT IN ('Cancelled', 'Completed', 'Rejected')";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves total orders booked in current month
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return Count of monthly orders
     * @throws SQLException If database query fails
     */
    private int getTotalOrdersThisMonth(Connection con, int providerId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT b.booking_id) " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "AND MONTH(b.booking_date) = MONTH(CURDATE()) " +
                     "AND YEAR(b.booking_date) = YEAR(CURDATE())";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Calculates total revenue for current month (excluding cancelled/rejected orders)
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return Monthly revenue amount
     * @throws SQLException If database query fails
     */
    private double getRevenueThisMonth(Connection con, int providerId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(b.total_amount), 0) " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "AND MONTH(b.booking_date) = MONTH(CURDATE()) " +
                     "AND YEAR(b.booking_date) = YEAR(CURDATE()) " +
                     "AND b.status NOT IN ('Cancelled', 'Rejected')";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0;
    }

    /**
     * Counts unique customers served in current year
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return Count of unique customers
     * @throws SQLException If database query fails
     */
    private int getCustomersThisYear(Connection con, int providerId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT b.customer_id) " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "AND YEAR(b.booking_date) = YEAR(CURDATE()) " +
                     "AND b.status NOT IN ('Cancelled', 'Rejected')";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

//    /**
//     * Retrieves current wallet balance for service provider
//     * Currently commented out - potentially deprecated feature
//     */
//    private double getWalletBalance(Connection con, int providerId) throws SQLException {
//        String sql = "SELECT wallet_balance FROM service_providers WHERE provider_id = ?";
//        
//        try (PreparedStatement ps = con.prepareStatement(sql)) {
//            ps.setInt(1, providerId);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    return rs.getDouble(1);
//                }
//            }
//        }
//        return 0;
//    }

    /**
     * Calculates total earnings for current year (excluding cancelled/rejected orders)
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return Year-to-date earnings
     * @throws SQLException If database query fails
     */
    private double getTotalEarningsThisYear(Connection con, int providerId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(b.total_amount), 0) " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "AND YEAR(b.booking_date) = YEAR(CURDATE()) " +
                     "AND b.status NOT IN ('Cancelled', 'Rejected')";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0;
    }

    /**
     * Generates booking analysis data grouped by status for pie/bar charts
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return JSON object with labels (status) and series (counts)
     * @throws SQLException If database query fails
     */
    private JSONObject getBookingAnalysisData(Connection con, int providerId) throws SQLException {
        String sql = "SELECT b.status, COUNT(DISTINCT b.booking_id) as count " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "GROUP BY b.status";
        
        JSONObject result = new JSONObject();
        JSONArray labels = new JSONArray();
        JSONArray series = new JSONArray();
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    labels.put(rs.getString("status"));
                    series.put(rs.getInt("count"));
                }
            }
        }
        
        result.put("labels", labels);
        result.put("series", series);
        return result;
    }

    /**
     * Retrieves performance rating data per service for quality metrics
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return JSON object with service categories and average ratings
     * @throws SQLException If database query fails
     */
    private JSONObject getPerformanceRatingData(Connection con, int providerId) throws SQLException {
        String sql = "SELECT s.service_id, s.service_name, COALESCE(AVG(r.rating), 0) as avg_rating " +
                     "FROM services s " +
                     "LEFT JOIN reviews r ON s.service_id = r.service_id " +
                     "WHERE s.provider_id = ? " +
                     "GROUP BY s.service_id, s.service_name " +
                     "ORDER BY avg_rating DESC";
        
        JSONObject result = new JSONObject();
        JSONArray categories = new JSONArray();
        JSONArray ratings = new JSONArray();
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.put(rs.getString("service_name"));
                    ratings.put(rs.getDouble("avg_rating"));
                }
            }
        }
        
        result.put("categories", categories);
        result.put("ratings", ratings);
        return result;
    }

    /**
     * Generates booking calendar data for time-series analysis (last 12 months)
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return JSON object with monthly categories and booking counts
     * @throws SQLException If database query fails
     */
    private JSONObject getBookingCalendarData(Connection con, int providerId) throws SQLException {
        String sql = "SELECT DATE_FORMAT(b.booking_date, '%Y-%m') as month, COUNT(DISTINCT b.booking_id) as count " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "AND b.booking_date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                     "GROUP BY DATE_FORMAT(b.booking_date, '%Y-%m') " +
                     "ORDER BY month";
        
        JSONObject result = new JSONObject();
        JSONArray categories = new JSONArray();
        JSONArray bookings = new JSONArray();
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.put(rs.getString("month"));
                    bookings.put(rs.getInt("count"));
                }
            }
        }
        
        result.put("categories", categories);
        result.put("bookings", bookings);
        return result;
    }

    /**
     * Generates financial data for revenue trend analysis (last 12 months)
     * 
     * @param con Database connection
     * @param providerId Service provider identifier
     * @return JSON object with monthly categories and revenue amounts
     * @throws SQLException If database query fails
     */
    private JSONObject getFinancialData(Connection con, int providerId) throws SQLException {
        String sql = "SELECT DATE_FORMAT(b.booking_date, '%Y-%m') as month, COALESCE(SUM(b.total_amount), 0) as revenue " +
                     "FROM bookings b " +
                     "JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                     "WHERE bd.service_id IN (SELECT service_id FROM services WHERE provider_id = ?) " +
                     "AND b.status NOT IN ('Cancelled', 'Rejected') " +
                     "AND b.booking_date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                     "GROUP BY DATE_FORMAT(b.booking_date, '%Y-%m') " +
                     "ORDER BY month";
        
        JSONObject result = new JSONObject();
        JSONArray categories = new JSONArray();
        JSONArray revenue = new JSONArray();
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.put(rs.getString("month"));
                    revenue.put(rs.getDouble("revenue"));
                }
            }
        }
        
        result.put("categories", categories);
        result.put("revenue", revenue);
        return result;
    }
}