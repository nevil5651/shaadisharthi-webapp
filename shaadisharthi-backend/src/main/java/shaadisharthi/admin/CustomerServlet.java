package shaadisharthi.admin;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import shaadisharthi.DbConnection.DbConnection;

/**
 * Customer Servlet - Comprehensive Customer Management System
 * 
 * Provides complete CRUD operations for customer management in the admin panel:
 * - List customers with pagination
 * - Search customers with multiple criteria
 * - Edit customer profiles
 * - Delete customer accounts
 * 
 * Architecture: Follows MVC pattern with servlet as controller and CustomerService
 * handling business logic and data access.
 * 
 * Security Features:
 * - JWT token validation for admin authentication
 * - Comprehensive input validation and sanitization
 * - SQL injection prevention via prepared statements
 * - Audit logging of all customer operations
 * 
 * API Endpoints:
 * - GET /customers/ - List customers with pagination
 * - POST /customers/edit - Update customer profile
 * - POST /customers/delete - Remove customer account
 * - POST /customers/search - Search customers with filters
 */
@WebServlet("/customers/*")
public class CustomerServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CustomerServlet.class);
    private CustomerService customerService;

    /**
     * Servlet initialization - creates CustomerService instance
     */
    @Override
    public void init() throws ServletException {
        customerService = new CustomerService();
        logger.info("CustomerServlet initialized");
    }

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
        response.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Validates customer data for edit operations
     * 
     * Validation Rules:
     * - customerId: Required, numeric
     * - name: Required, max 50 characters
     * - phone: Required, exactly 10 digits
     * - altPhone: Optional, but must be 10 digits if provided
     * - address: Required, non-empty
     * 
     * @param customerId Customer identifier
     * @param name Customer's full name
     * @param phone Primary phone number
     * @param altPhone Alternate phone number (optional)
     * @param address Customer's address
     * @param response HttpServletResponse for error handling
     * @return true if all inputs are valid, false otherwise
     */
    private boolean validateCustomerData(String customerId, String name, String phone, String altPhone, String address, HttpServletResponse response)
            throws IOException {
        if (customerId == null || !customerId.matches("\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing customer ID");
            return false;
        }
        if (name == null || name.trim().isEmpty() || name.length() > 50) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing name (max 50 characters)");
            return false;
        }
        if (phone == null || !phone.matches("\\d{10}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing phone number (must be 10 digits)");
            return false;
        }
        if (altPhone != null && !altPhone.isEmpty() && !altPhone.matches("\\d{10}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid alternate phone number (must be 10 digits)");
            return false;
        }
        if (address == null || address.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing address");
            return false;
        }
        return true;
    }

    /**
     * Validates customer ID parameter
     * 
     * @param customerId Customer identifier to validate
     * @param response HttpServletResponse for error handling
     * @return true if valid, false otherwise
     */
    private boolean validateCustomerId(String customerId, HttpServletResponse response) throws IOException {
        if (customerId == null || !customerId.matches("\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing customer ID");
            return false;
        }
        return true;
    }

    /**
     * Validates search parameters to prevent injection and ensure data integrity
     * 
     * @param customerId Numeric customer ID
     * @param name Customer name (free text)
     * @param email Valid email format
     * @param phoneNo 10-digit phone number
     * @param altPhone 10-digit alternate phone number
     * @param response HttpServletResponse for error handling
     * @return true if all parameters are valid, false otherwise
     */
    private boolean validateSearchParams(String customerId, String name, String email, String phoneNo, String altPhone, HttpServletResponse response)
            throws IOException {
        if (customerId != null && !customerId.isEmpty() && !customerId.matches("\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid customer ID (must be numeric)");
            return false;
        }
        if (email != null && !email.isEmpty() && !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
            return false;
        }
        if (phoneNo != null && !phoneNo.isEmpty() && !phoneNo.matches("\\d{10}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid phone number (must be 10 digits)");
            return false;
        }
        if (altPhone != null && !altPhone.isEmpty() && !altPhone.matches("\\d{10}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid alternate phone number (must be 10 digits)");
            return false;
        }
        return true;
    }

    /**
     * GET endpoint - Retrieves paginated list of customers
     * 
     * Returns customers sorted by creation date (newest first) with pagination support.
     * Default: page 1, limit 15 customers per page
     * 
     * @param request HttpServletRequest with optional page/limit parameters
     * @param response HttpServletResponse with JSON customer list
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for customers");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract and validate admin authentication from JWT
        Claims claims = (Claims) request.getAttribute("claims");
        String adminId;
        try {
            adminId = claims.getSubject();
            logger.debug("Authenticated admin ID: {}", adminId);
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null");
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Validate endpoint path
        String pathInfo = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if (!pathInfo.equals("/")) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Invalid endpoint");
            return;
        }

        // Parse pagination parameters with safe defaults
        int page = 1;
        int limit = 15;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
            if (request.getParameter("limit") != null) {
                limit = Integer.parseInt(request.getParameter("limit"));
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid page or limit parameter: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid page or limit parameter");
            return;
        }

        // Validate pagination parameters
        if (page < 1 || limit < 1) {
            logger.warn("Page and limit must be positive: page={}, limit={}", page, limit);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Page and limit must be positive integers");
            return;
        }

        try {
            // Fetch customers through service layer
            CustomerService.CustomerResult result = customerService.getLatestCustomers(page, limit);
            JSONObject responseJson = createCustomerListJson(result.customers, result.totalCount);
            logger.info("Fetched {} customers for admin ID: {}", result.customers.size(), adminId);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(responseJson.toString());
                writer.flush();
            }
        } catch (SQLException e) {
            logger.error("Database error fetching customers: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * POST endpoint - Handles customer management operations
     * 
     * Supported actions:
     * - /edit - Update customer profile information
     * - /delete - Remove customer account from system
     * - /search - Find customers based on multiple criteria
     * 
     * @param request HttpServletRequest with action and parameters
     * @param response HttpServletResponse with operation result
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for customers");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract and validate admin authentication
        Claims claims = (Claims) request.getAttribute("claims");
        String adminId;
        try {
            adminId = claims.getSubject();
            logger.debug("Authenticated admin ID: {}", adminId);
        } catch (NullPointerException e) {
            logger.error("Claims attribute is null");
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Route to appropriate handler based on path
        String pathInfo = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (pathInfo) {
            case "/edit":
                handleEditCustomer(request, response, adminId);
                break;
            case "/delete":
                handleDeleteCustomer(request, response, adminId);
                break;
            case "/search":
                handleSearchCustomers(request, response, adminId);
                break;
            default:
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Invalid endpoint");
                break;
        }
    }

    /**
     * Handles customer profile updates
     * 
     * Updateable fields:
     * - name, phoneNumber, alternatePhoneNumber, address
     * - Email cannot be changed (primary identifier)
     * 
     * @param request HttpServletRequest with JSON body containing update fields
     * @param response HttpServletResponse with update result
     * @param adminId Authenticated admin ID for audit logging
     */
    private void handleEditCustomer(HttpServletRequest request, HttpServletResponse response, String adminId)
            throws IOException {
        logger.debug("Processing customer edit for admin ID: {}", adminId);
        
        // Read and parse request body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        JSONObject json = new JSONObject(sb.toString());
        String customerId = json.optString("customerId", null);
        String name = json.optString("name", null);
        String phone = json.optString("phoneNumber", null);
        String altPhone = json.optString("alternatePhoneNumber", null);
        String address = json.optString("address", null);

        // Validate input parameters
        if (!validateCustomerData(customerId, name, phone, altPhone, address, response)) {
            return;
        }

        try {
            // Perform update through service layer
            boolean updated = customerService.editCustomer(customerId, name, phone, altPhone, address);
            if (updated) {
                logger.info("Customer ID {} updated by admin ID: {}", customerId, adminId);
                response.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(new JSONObject().put("message", "Customer updated successfully").toString());
                    writer.flush();
                }
            } else {
                logger.warn("Customer ID {} not found or not updated", customerId);
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Customer not found or not updated");
            }
        } catch (SQLException e) {
            logger.error("Database error updating customer ID {}: {}", customerId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handles customer account deletion
     * 
     * Security Note: This is a hard delete operation that removes the customer
     * record from the database. Consider soft delete with status flags for
     * production systems to maintain data integrity and audit trails.
     * 
     * @param request HttpServletRequest with customerId parameter
     * @param response HttpServletResponse with deletion result
     * @param adminId Authenticated admin ID for audit logging
     */
    private void handleDeleteCustomer(HttpServletRequest request, HttpServletResponse response, String adminId)
            throws IOException {
        logger.debug("Processing customer delete for admin ID: {}", adminId);
        String customerId = request.getParameter("customerId");
        
        // Validate customer ID
        if (!validateCustomerId(customerId, response)) {
            return;
        }

        try {
            // Perform deletion through service layer
            boolean deleted = customerService.deleteCustomer(customerId);
            if (deleted) {
                logger.info("Customer ID {} deleted by admin ID: {}", customerId, adminId);
                response.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(new JSONObject().put("message", "Customer deleted successfully").toString());
                    writer.flush();
                }
            } else {
                logger.warn("Customer ID {} not found", customerId);
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Customer not found");
            }
        } catch (SQLException e) {
            logger.error("Database error deleting customer ID {}: {}", customerId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handles customer search with multiple filter criteria
     * 
     * Searchable fields:
     * - customerId (exact match)
     * - name (partial match with LIKE)
     * - email (partial match with LIKE) 
     * - phone_no (partial match with LIKE)
     * - alternate_phone (partial match with LIKE)
     * 
     * Results are paginated and sorted by creation date (newest first)
     * 
     * @param request HttpServletRequest with search parameters
     * @param response HttpServletResponse with search results
     * @param adminId Authenticated admin ID for audit logging
     */
    private void handleSearchCustomers(HttpServletRequest request, HttpServletResponse response, String adminId)
            throws IOException {
        logger.debug("Processing customer search for admin ID: {}", adminId);
        
        // Parse pagination parameters
        int page = 1;
        int limit = 15;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
            if (request.getParameter("limit") != null) {
                limit = Integer.parseInt(request.getParameter("limit"));
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid page or limit parameter: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid page or limit parameter");
            return;
        }

        // Validate pagination parameters
        if (page < 1 || limit < 1) {
            logger.warn("Page and limit must be positive: page={}, limit={}", page, limit);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Page and limit must be positive integers");
            return;
        }

        // Extract search parameters
        String customerId = request.getParameter("customer_id");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phoneNo = request.getParameter("phone_no");
        String altPhone = request.getParameter("alternate_phone");

        // Validate search parameters
        if (!validateSearchParams(customerId, name, email, phoneNo, altPhone, response)) {
            return;
        }

        try {
            // Perform search through service layer
            CustomerService.CustomerResult result = customerService.searchCustomers(customerId, name, email, phoneNo, altPhone, page, limit);
            JSONObject responseJson = createCustomerListJson(result.customers, result.totalCount);
            logger.info("Fetched {} customers for search by admin ID: {}", result.customers.size(), adminId);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(responseJson.toString());
                writer.flush();
            }
        } catch (SQLException e) {
            logger.error("Database error searching customers: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Creates standardized JSON response for customer lists
     * 
     * @param customers List of Customer objects to serialize
     * @param totalCount Total number of customers for pagination
     * @return JSONObject containing customers array and totalCount
     */
    private JSONObject createCustomerListJson(List<Customer> customers, int totalCount) {
        JSONArray jsonArray = new JSONArray();
        for (Customer customer : customers) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("customerId", customer.getCustomerId());
            jsonObj.put("name", customer.getName());
            jsonObj.put("email", customer.getEmail());
            jsonObj.put("createdAt", customer.getCreatedAt());
            jsonObj.put("phoneNumber", customer.getPhoneNumber());
            jsonObj.put("alternatePhoneNumber", customer.getAlternatePhoneNumber());
            jsonObj.put("address", customer.getAddress());
            jsonArray.put(jsonObj);
        }
        JSONObject responseJson = new JSONObject();
        responseJson.put("customers", jsonArray);
        responseJson.put("totalCount", totalCount);
        return responseJson;
    }
}

/**
 * CustomerService - Business Logic Layer for Customer Operations
 * 
 * Handles all database interactions and business logic for customer management.
 * Separates data access from controller logic for better maintainability and testability.
 * 
 * Features:
 * - Paginated customer listing
 * - Customer profile updates
 * - Customer deletion
 * - Advanced search with multiple criteria
 * - Total count calculations for pagination
 */
class CustomerService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    /**
     * CustomerResult - Data Transfer Object for paginated results
     * 
     * Contains both the list of customers and total count for pagination controls
     */
    static class CustomerResult {
        List<Customer> customers;
        int totalCount;

        CustomerResult(List<Customer> customers, int totalCount) {
            this.customers = customers;
            this.totalCount = totalCount;
        }
    }

    /**
     * Retrieves paginated list of customers sorted by creation date (newest first)
     * 
     * @param page Page number (1-based)
     * @param limit Number of customers per page
     * @return CustomerResult containing customers and total count
     * @throws SQLException if database error occurs
     */
    public CustomerResult getLatestCustomers(int page, int limit) throws SQLException {
        List<Customer> customerList = new ArrayList<>();
        int totalCount = 0;

        try (Connection conn = DbConnection.getCon()) {
            // Get total count for pagination
            String countSql = "SELECT COUNT(*) FROM customers";
            try (PreparedStatement countPs = conn.prepareStatement(countSql);
                 ResultSet countRs = countPs.executeQuery()) {
                if (countRs.next()) {
                    totalCount = countRs.getInt(1);
                }
            }

            // Calculate offset for pagination
            int offset = (page - 1) * limit;
            String sql = "SELECT * FROM customers ORDER BY created_at DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Customer c = new Customer();
                        c.setCustomerId(rs.getInt("customer_id"));
                        c.setName(rs.getString("name"));
                        c.setEmail(rs.getString("email"));
                        c.setCreatedAt(rs.getString("created_at"));
                        c.setPhoneNumber(rs.getString("phone_no"));
                        c.setAlternatePhoneNumber(rs.getString("alternate_phone"));
                        c.setAddress(rs.getString("address"));
                        customerList.add(c);
                    }
                }
            }
        }
        logger.debug("Fetched {} customers, total count: {}", customerList.size(), totalCount);
        return new CustomerResult(customerList, totalCount);
    }

    /**
     * Updates customer profile information
     * 
     * @param customerId Customer identifier
     * @param name Updated full name
     * @param phone Updated primary phone number
     * @param altPhone Updated alternate phone number
     * @param address Updated address
     * @return true if update successful, false if customer not found
     * @throws SQLException if database error occurs
     */
    public boolean editCustomer(String customerId, String name, String phone, String altPhone, String address)
            throws SQLException {
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE customers SET name=?, phone_no=?, alternate_phone=?, address=? WHERE customer_id=?"
             )) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, altPhone);
            stmt.setString(4, address);
            stmt.setString(5, customerId);
            int rows = stmt.executeUpdate();
            logger.debug("Updated customer ID {}: {} rows affected", customerId, rows);
            return rows > 0;
        }
    }

    /**
     * Deletes customer account from the system
     * 
     * Warning: This is a hard delete operation. Consider implementing soft delete
     * in production systems to maintain referential integrity and audit trails.
     * 
     * @param customerId Customer identifier to delete
     * @return true if deletion successful, false if customer not found
     * @throws SQLException if database error occurs
     */
    public boolean deleteCustomer(String customerId) throws SQLException {
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            stmt.setString(1, customerId);
            int rows = stmt.executeUpdate();
            logger.debug("Deleted customer ID {}: {} rows affected", customerId, rows);
            return rows > 0;
        }
    }

    /**
     * Searches customers based on multiple criteria with partial matching
     * 
     * Builds dynamic SQL query based on provided parameters. Uses LIKE for
     * partial matching on text fields and exact matching on numeric fields.
     * 
     * @param customerId Exact customer ID match
     * @param name Partial name match
     * @param email Partial email match
     * @param phoneNo Partial phone number match
     * @param altPhone Partial alternate phone match
     * @param page Page number for pagination
     * @param limit Results per page
     * @return CustomerResult with matching customers and total count
     * @throws SQLException if database error occurs
     */
    public CustomerResult searchCustomers(String customerId, String name, String email, String phoneNo, String altPhone,
                                         int page, int limit) throws SQLException {
        List<Customer> customerList = new ArrayList<>();
        
        // Build dynamic SQL queries based on provided parameters
        StringBuilder sql = new StringBuilder("SELECT * FROM customers WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM customers WHERE 1=1");
        List<String> params = new ArrayList<>();

        // Add conditions for each provided search parameter
        if (customerId != null && !customerId.trim().isEmpty()) {
            sql.append(" AND customer_id = ?");
            countSql.append(" AND customer_id = ?");
            params.add(customerId.trim());
        }
        if (name != null && !name.trim().isEmpty()) {
            sql.append(" AND name LIKE ?");
            countSql.append(" AND name LIKE ?");
            params.add("%" + name.trim() + "%");
        }
        if (email != null && !email.trim().isEmpty()) {
            sql.append(" AND email LIKE ?");
            countSql.append(" AND email LIKE ?");
            params.add("%" + email.trim() + "%");
        }
        if (phoneNo != null && !phoneNo.trim().isEmpty()) {
            sql.append(" AND phone_no LIKE ?");
            countSql.append(" AND phone_no LIKE ?");
            params.add("%" + phoneNo.trim() + "%");
        }
        if (altPhone != null && !altPhone.trim().isEmpty()) {
            sql.append(" AND alternate_phone LIKE ?");
            countSql.append(" AND alternate_phone LIKE ?");
            params.add("%" + altPhone.trim() + "%");
        }

        // Add pagination to main query
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        int offset = (page - 1) * limit;
        int totalCount = 0;

        try (Connection conn = DbConnection.getCon()) {
            // Get total count for pagination
            try (PreparedStatement countPs = conn.prepareStatement(countSql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    countPs.setString(i + 1, params.get(i));
                }
                try (ResultSet countRs = countPs.executeQuery()) {
                    if (countRs.next()) {
                        totalCount = countRs.getInt(1);
                    }
                }
            }

            // Execute main search query
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                // Set search parameters
                for (int i = 0; i < params.size(); i++) {
                    ps.setString(i + 1, params.get(i));
                }
                // Set pagination parameters
                ps.setInt(params.size() + 1, limit);
                ps.setInt(params.size() + 2, offset);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Customer c = new Customer();
                        c.setCustomerId(rs.getInt("customer_id"));
                        c.setName(rs.getString("name"));
                        c.setEmail(rs.getString("email"));
                        c.setCreatedAt(rs.getString("created_at"));
                        c.setPhoneNumber(rs.getString("phone_no"));
                        c.setAlternatePhoneNumber(rs.getString("alternate_phone"));
                        c.setAddress(rs.getString("address"));
                        customerList.add(c);
                    }
                }
            }
        }
        logger.debug("Searched {} customers, total count: {}", customerList.size(), totalCount);
        return new CustomerResult(customerList, totalCount);
    }
}