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
 * ServiceProviderServlet - Comprehensive service provider management system
 * 
 * Handles CRUD operations, search, and listing of service providers with advanced features:
 * - Paginated listing with status filtering
 * - Multi-criteria search with validation
 * - Provider profile editing
 * - Cascading deletion with transaction management
 * - JWT-protected admin access
 * 
 * Endpoint: /service-providers/*
 * Methods: GET, POST (with sub-paths for specific operations)
 * 
 * @category Service Provider Management
 * @security JWT Token required (admin role)
 */
@WebServlet("/service-providers/*")
public class ServiceProviderServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderServlet.class);
    private ServiceProviderService serviceProviderService;

    @Override
    public void init() throws ServletException {
        serviceProviderService = new ServiceProviderService();
        logger.info("ServiceProviderServlet initialized");
    }

    /**
     * Send standardized error responses as JSON
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param message Error message description
     * @throws IOException If response writing fails
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
     * Validate service provider data for editing operations
     * 
     * @param providerId Provider ID (must be numeric)
     * @param name Provider name (max 50 chars, required)
     * @param phone Primary phone (10 digits, required)
     * @param altPhone Alternate phone (10 digits, optional)
     * @param address Address (required)
     * @param city City (required)
     * @param state State (required)
     * @param response HttpServletResponse for error handling
     * @return true if validation passes, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateProviderData(String providerId, String name, String phone, String altPhone, String address, String city, String state, HttpServletResponse response)
            throws IOException {
        if (providerId == null || !providerId.matches("\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing provider ID");
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
        if (city == null || city.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing city");
            return false;
        }
        if (state == null || state.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing state");
            return false;
        }
        return true;
    }

    /**
     * Validate provider ID format
     * 
     * @param providerId Provider ID to validate
     * @param response HttpServletResponse for error handling
     * @return true if valid numeric ID, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateProviderId(String providerId, HttpServletResponse response) throws IOException {
        if (providerId == null || !providerId.matches("\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing provider ID");
            return false;
        }
        return true;
    }

    /**
     * Validate search parameters with comprehensive format checking
     * 
     * @param providerId Numeric provider ID
     * @param name Provider name
     * @param email Valid email format
     * @param phoneNo 10-digit phone number
     * @param altPhone 10-digit alternate phone
     * @param city City name
     * @param state State name
     * @param businessName Business name
     * @param gstNumber GST number format validation
     * @param aadharNumber 12-digit Aadhar number
     * @param panNumber PAN card format validation
     * @param response HttpServletResponse for error handling
     * @return true if all parameters are valid, false otherwise
     * @throws IOException If error response needs to be sent
     */
    private boolean validateSearchParams(String providerId, String name, String email, String phoneNo, String altPhone, String city, String state, String businessName, String gstNumber, String aadharNumber, String panNumber, HttpServletResponse response)
            throws IOException {
        if (providerId != null && !providerId.isEmpty() && !providerId.matches("\\d+")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid provider ID (must be numeric)");
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
        if (gstNumber != null && !gstNumber.isEmpty() && !gstNumber.matches("\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[1-9A-Z]{1}Z\\d{1}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid GST number format");
            return false;
        }
        if (aadharNumber != null && !aadharNumber.isEmpty() && !aadharNumber.matches("\\d{12}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Aadhar number (must be 12 digits)");
            return false;
        }
        if (panNumber != null && !panNumber.isEmpty() && !panNumber.matches("[A-Z]{5}\\d{4}[A-Z]{1}")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid PAN number format");
            return false;
        }
        return true;
    }

    /**
     * GET /service-providers - Retrieve paginated list of service providers
     * 
     * Supports:
     * - Pagination with page and limit parameters
     * - Status filtering (basic_registered, pending_approval, approved, rejected)
     * - JWT authentication
     * 
     * @param request HttpServletRequest with optional page, limit, status parameters
     * @param response JSON response with providers array and totalCount
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing GET request for service providers");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract and validate JWT claims
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

        // Parse pagination parameters with defaults
        int page = 1;
        int limit = 15;
        String status = request.getParameter("status"); // Get status filter
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

        // Validate pagination values
        if (page < 1 || limit < 1) {
            logger.warn("Page and limit must be positive: page={}, limit={}", page, limit);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Page and limit must be positive integers");
            return;
        }

        // Validate status parameter against allowed values
        if (status != null && !status.isEmpty() && 
            !status.equals("basic_registered") && 
            !status.equals("pending_approval") && 
            !status.equals("approved") && 
            !status.equals("rejected")) {
            logger.warn("Invalid status parameter: {}", status);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid status parameter");
            return;
        }

        try {
            // Fetch providers from service layer
            ServiceProviderService.ProviderResult result = serviceProviderService.getLatestServiceProviders(page, limit, status);
            JSONObject responseJson = createProviderListJson(result.providers, result.totalCount);
            logger.info("Fetched {} service providers for admin ID: {}, status: {}", result.providers.size(), adminId, status);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(responseJson.toString());
                writer.flush();
            }
        } catch (SQLException e) {
            logger.error("Database error fetching service providers: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * POST /service-providers/* - Handle various service provider operations
     * 
     * Supported sub-paths:
     * - /edit: Update provider profile information
     * - /delete: Remove provider and associated data
     * - /search: Advanced multi-criteria search
     * 
     * @param request HttpServletRequest with operation-specific parameters
     * @param response JSON response with operation result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for service providers");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Extract and validate JWT claims
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
                handleEditServiceProvider(request, response, adminId);
                break;
            case "/delete":
                handleDeleteServiceProvider(request, response, adminId);
                break;
            case "/search":
                handleSearchServiceProviders(request, response, adminId);
                break;
            default:
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Invalid endpoint");
                break;
        }
    }

    /**
     * Handle service provider profile editing
     * 
     * @param request Contains JSON with provider data to update
     * @param response JSON response with update result
     * @param adminId Admin ID for audit logging
     * @throws IOException If response writing fails
     */
    private void handleEditServiceProvider(HttpServletRequest request, HttpServletResponse response, String adminId)
            throws IOException {
        logger.debug("Processing service provider edit for admin ID: {}", adminId);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        JSONObject json = new JSONObject(sb.toString());
        String providerId = json.optString("providerId", null);
        String name = json.optString("name", null);
        String phone = json.optString("phoneNumber", null);
        String altPhone = json.optString("alternatePhoneNumber", null);
        String address = json.optString("address", null);
        String city = json.optString("city", null);
        String state = json.optString("state", null);

        // Validate input data
        if (!validateProviderData(providerId, name, phone, altPhone, address, city, state, response)) {
            return;
        }

        try {
            // Perform update through service layer
            boolean updated = serviceProviderService.editServiceProvider(providerId, name, phone, altPhone, address, city, state);
            if (updated) {
                logger.info("Service provider ID {} updated by admin ID: {}", providerId, adminId);
                response.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(new JSONObject().put("message", "Service provider updated successfully").toString());
                    writer.flush();
                }
            } else {
                logger.warn("Service provider ID {} not found or not updated", providerId);
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service provider not found or not updated");
            }
        } catch (SQLException e) {
            logger.error("Database error updating service provider ID {}: {}", providerId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handle service provider deletion with cascading cleanup
     * 
     * @param request Contains provider_id parameter
     * @param response JSON response with deletion result
     * @param adminId Admin ID for audit logging
     * @throws IOException If response writing fails
     */
    private void handleDeleteServiceProvider(HttpServletRequest request, HttpServletResponse response, String adminId)
            throws IOException {
        logger.debug("Processing service provider delete for admin ID: {}", adminId);
        String providerId = request.getParameter("provider_id");
        if (!validateProviderId(providerId, response)) {
            return;
        }

        try {
            // Perform deletion through service layer (includes cascading deletes)
            boolean deleted = serviceProviderService.deleteServiceProvider(Integer.parseInt(providerId));
            if (deleted) {
                logger.info("Service provider ID {} deleted by admin ID: {}", providerId, adminId);
                response.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(new JSONObject().put("message", "Service provider deleted successfully").toString());
                    writer.flush();
                }
            } else {
                logger.warn("Service provider ID {} not found", providerId);
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Service provider not found");
            }
        } catch (SQLException e) {
            logger.error("Database error deleting service provider ID {}: {}", providerId, e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Handle advanced service provider search with multiple criteria
     * 
     * @param request Contains search parameters for filtering
     * @param response JSON response with matching providers
     * @param adminId Admin ID for audit logging
     * @throws IOException If response writing fails
     */
    private void handleSearchServiceProviders(HttpServletRequest request, HttpServletResponse response, String adminId)
            throws IOException {
        logger.debug("Processing service provider search for admin ID: {}", adminId);
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

        if (page < 1 || limit < 1) {
            logger.warn("Page and limit must be positive: page={}, limit={}", page, limit);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Page and limit must be positive integers");
            return;
        }

        // Extract search parameters
        String providerId = request.getParameter("provider_id");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phoneNo = request.getParameter("phone_no");
        String altPhone = request.getParameter("alternate_phone");
        String city = request.getParameter("city");
        String state = request.getParameter("state");
        String businessName = request.getParameter("business_name");
        String gstNumber = request.getParameter("gst_number");
        String aadharNumber = request.getParameter("aadhar_number");
        String panNumber = request.getParameter("pan_number");

        // Validate search parameters
        if (!validateSearchParams(providerId, name, email, phoneNo, altPhone, city, state, businessName, gstNumber, aadharNumber, panNumber, response)) {
            return;
        }

        try {
            // Perform search through service layer
            ServiceProviderService.ProviderResult result = serviceProviderService.searchServiceProviders(
                providerId, name, email, phoneNo, altPhone, city, state, businessName, gstNumber, aadharNumber, panNumber, page, limit);
            JSONObject responseJson = createProviderListJson(result.providers, result.totalCount);
            logger.info("Fetched {} service providers for search by admin ID: {}", result.providers.size(), adminId);
            try (PrintWriter writer = response.getWriter()) {
                writer.write(responseJson.toString());
                writer.flush();
            }
        } catch (SQLException e) {
            logger.error("Database error searching service providers: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Create JSON response from provider list and total count
     * 
     * @param providers List of ServiceProvider objects
     * @param totalCount Total number of providers (for pagination)
     * @return JSONObject containing providers array and totalCount
     */
    private JSONObject createProviderListJson(List<ServiceProvider> providers, int totalCount) {
        JSONArray jsonArray = new JSONArray();
        for (ServiceProvider provider : providers) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("providerId", provider.getProviderId());
            jsonObj.put("name", provider.getName());
            jsonObj.put("email", provider.getEmail());
            jsonObj.put("createdAt", provider.getCreatedAt());
            jsonObj.put("phoneNumber", provider.getPhoneNumber());
            jsonObj.put("alternatePhoneNumber", provider.getAlternatePhoneNumber());
            jsonObj.put("address", provider.getAddress());
            jsonObj.put("city", provider.getCity());
            jsonObj.put("state", provider.getState());
            jsonObj.put("businessName", provider.getBusinessName());
            jsonObj.put("gstNumber", provider.getGstNumber());
            jsonObj.put("aadharNumber", provider.getAadharNumber());
            jsonObj.put("panNumber", provider.getPanNumber());
            jsonObj.put("status", provider.getStatus()); // Added status to JSON response
            jsonArray.put(jsonObj);
        }
        JSONObject responseJson = new JSONObject();
        responseJson.put("providers", jsonArray);
        responseJson.put("totalCount", totalCount);
        return responseJson;
    }
}

/**
 * ServiceProviderService - Business logic layer for service provider operations
 * 
 * Handles database interactions for:
 * - Paginated provider retrieval with filtering
 * - Provider profile updates
 * - Cascading deletion with transaction management
 * - Advanced multi-criteria search
 */
class ServiceProviderService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceProviderService.class);

    /**
     * ProviderResult - Data transfer object for paginated provider results
     */
    static class ProviderResult {
        List<ServiceProvider> providers;
        int totalCount;

        ProviderResult(List<ServiceProvider> providers, int totalCount) {
            this.providers = providers;
            this.totalCount = totalCount;
        }
    }

    /**
     * Retrieve paginated list of service providers with optional status filtering
     * 
     * @param page Page number (1-based)
     * @param limit Number of items per page
     * @param status Filter by provider status (basic_registered, pending_approval, approved, rejected)
     * @return ProviderResult containing providers list and total count
     * @throws SQLException If database operation fails
     */
    public ProviderResult getLatestServiceProviders(int page, int limit, String status) throws SQLException {
        List<ServiceProvider> providerList = new ArrayList<>();
        int totalCount = 0;

        try (Connection conn = DbConnection.getCon()) {
            // Build count query with optional status filter
            String countSql = "SELECT COUNT(*) FROM service_providers";
            if (status != null && !status.isEmpty()) {
                countSql += " WHERE status = ?";
            }
            try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                if (status != null && !status.isEmpty()) {
                    countPs.setString(1, status);
                }
                try (ResultSet countRs = countPs.executeQuery()) {
                    if (countRs.next()) {
                        totalCount = countRs.getInt(1);
                    }
                }
            }

            // Build main query with pagination and optional status filter
            int offset = (page - 1) * limit;
            String sql = "SELECT * FROM service_providers";
            if (status != null && !status.isEmpty()) {
                sql += " WHERE status = ?";
            }
            sql += " ORDER BY created_at DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int paramIndex = 1;
                if (status != null && !status.isEmpty()) {
                    ps.setString(paramIndex++, status);
                }
                ps.setInt(paramIndex++, limit);
                ps.setInt(paramIndex++, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ServiceProvider sp = new ServiceProvider();
                        sp.setProviderId(rs.getInt("provider_id"));
                        sp.setName(rs.getString("name"));
                        sp.setEmail(rs.getString("email"));
                        sp.setCreatedAt(rs.getString("created_at"));
                        sp.setPhoneNumber(rs.getString("phone_no"));
                        sp.setAlternatePhoneNumber(rs.getString("alternate_phone"));
                        sp.setAddress(rs.getString("address"));
                        sp.setCity(rs.getString("city"));
                        sp.setState(rs.getString("state"));
                        sp.setBusinessName(rs.getString("business_name"));
                        sp.setGstNumber(rs.getString("gst_number"));
                        sp.setAadharNumber(rs.getString("aadhar_number"));
                        sp.setPanNumber(rs.getString("pan_number"));
                        sp.setStatus(rs.getString("status")); // Added status retrieval
                        providerList.add(sp);
                    }
                }
            }
        }
        logger.debug("Fetched {} service providers, total count: {}, status: {}", providerList.size(), totalCount, status);
        return new ProviderResult(providerList, totalCount);
    }

    /**
     * Update service provider profile information
     * 
     * @param providerId Provider ID to update
     * @param name Updated provider name
     * @param phone Updated primary phone
     * @param altPhone Updated alternate phone
     * @param address Updated address
     * @param city Updated city
     * @param state Updated state
     * @return true if update successful, false otherwise
     * @throws SQLException If database operation fails
     */
    public boolean editServiceProvider(String providerId, String name, String phone, String altPhone, String address, String city, String state)
            throws SQLException {
        try (Connection conn = DbConnection.getCon();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE service_providers SET name=?, phone_no=?, alternate_phone=?, address=?, city=?, state=? WHERE provider_id=?"
             )) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, altPhone);
            stmt.setString(4, address);
            stmt.setString(5, city);
            stmt.setString(6, state);
            stmt.setString(7, providerId);
            int rows = stmt.executeUpdate();
            logger.debug("Updated service provider ID {}: {} rows affected", providerId, rows);
            return rows > 0;
        }
    }

    /**
     * Delete service provider with cascading cleanup of related data
     * 
     * Performs transaction-safe deletion of:
     * - Provider media files
     * - Provider services
     * - Provider main record
     * 
     * @param providerId Provider ID to delete
     * @return true if deletion successful, false otherwise
     * @throws SQLException If database operation fails
     */
    public boolean deleteServiceProvider(int providerId) throws SQLException {
        try (Connection conn = DbConnection.getCon()) {
            conn.setAutoCommit(false);
            try {
                // Step 1: Get all service IDs for this provider
                List<Integer> serviceIds = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT service_id FROM services WHERE provider_id = ?")) {
                    stmt.setInt(1, providerId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            serviceIds.add(rs.getInt("service_id"));
                        }
                    }
                }

                // Step 2: If services exist, handle media cleanup
                if (!serviceIds.isEmpty()) {
                    List<Integer> mediaIds = new ArrayList<>();
                    StringBuilder servicePlaceholders = new StringBuilder();
                    for (int i = 0; i < serviceIds.size(); i++) {
                        servicePlaceholders.append("?");
                        if (i < serviceIds.size() - 1) servicePlaceholders.append(",");
                    }
                    // Get media IDs for all services
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT media_id FROM media WHERE service_id IN (" + servicePlaceholders.toString() + ")")) {
                        for (int i = 0; i < serviceIds.size(); i++) {
                            stmt.setInt(i + 1, serviceIds.get(i));
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                mediaIds.add(rs.getInt("media_id"));
                            }
                        }
                    }

                    // Step 3: Delete media records if they exist
                    if (!mediaIds.isEmpty()) {
                        StringBuilder mediaPlaceholders = new StringBuilder();
                        for (int i = 0; i < mediaIds.size(); i++) {
                            mediaPlaceholders.append("?");
                            if (i < mediaIds.size() - 1) mediaPlaceholders.append(",");
                        }
                        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM media WHERE media_id IN (" + mediaPlaceholders.toString() + ")")) {
                            for (int i = 0; i < mediaIds.size(); i++) {
                                stmt.setInt(i + 1, mediaIds.get(i));
                            }
                            stmt.executeUpdate();
                        }
                    }

                    // Step 4: Delete service records
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM services WHERE service_id IN (" + servicePlaceholders.toString() + ")")) {
                        for (int i = 0; i < serviceIds.size(); i++) {
                            stmt.setInt(i + 1, serviceIds.get(i));
                        }
                        stmt.executeUpdate();
                    }
                }

                // Step 5: Delete main provider record
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM service_providers WHERE provider_id = ?")) {
                    stmt.setInt(1, providerId);
                    int rows = stmt.executeUpdate();
                    conn.commit();
                    logger.debug("Deleted service provider ID {}: {} rows affected", providerId, rows);
                    return rows > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Advanced search for service providers with multiple criteria
     * 
     * @param providerId Exact provider ID match
     * @param name Partial name match
     * @param email Partial email match
     * @param phoneNo Partial phone number match
     * @param altPhone Partial alternate phone match
     * @param city Partial city match
     * @param state Partial state match
     * @param businessName Partial business name match
     * @param gstNumber Partial GST number match
     * @param aadharNumber Partial Aadhar number match
     * @param panNumber Partial PAN number match
     * @param page Page number for pagination
     * @param limit Items per page
     * @return ProviderResult with matching providers and total count
     * @throws SQLException If database operation fails
     */
    public ProviderResult searchServiceProviders(String providerId, String name, String email, String phoneNo,
                                                String altPhone, String city, String state, String businessName,
                                                String gstNumber, String aadharNumber, String panNumber, int page, int limit)
            throws SQLException {
        List<ServiceProvider> providerList = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM service_providers WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM service_providers WHERE 1=1");
        List<String> params = new ArrayList<>();

        // Dynamically build WHERE clause based on provided parameters
        if (providerId != null && !providerId.trim().isEmpty()) {
            sql.append(" AND provider_id = ?");
            countSql.append(" AND provider_id = ?");
            params.add(providerId.trim());
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
        if (city != null && !city.trim().isEmpty()) {
            sql.append(" AND city LIKE ?");
            countSql.append(" AND city LIKE ?");
            params.add("%" + city.trim() + "%");
        }
        if (state != null && !state.trim().isEmpty()) {
            sql.append(" AND state LIKE ?");
            countSql.append(" AND state LIKE ?");
            params.add("%" + state.trim() + "%");
        }
        if (businessName != null && !businessName.trim().isEmpty()) {
            sql.append(" AND business_name LIKE ?");
            countSql.append(" AND business_name LIKE ?");
            params.add("%" + businessName.trim() + "%");
        }
        if (gstNumber != null && !gstNumber.trim().isEmpty()) {
            sql.append(" AND gst_number LIKE ?");
            countSql.append(" AND gst_number LIKE ?");
            params.add("%" + gstNumber.trim() + "%");
        }
        if (aadharNumber != null && !aadharNumber.isEmpty()) {
            sql.append(" AND aadhar_number LIKE ?");
            countSql.append(" AND aadhar_number LIKE ?");
            params.add("%" + aadharNumber.trim() + "%");
        }
        if (panNumber != null && !panNumber.isEmpty()) {
            sql.append(" AND pan_number LIKE ?");
            countSql.append(" AND pan_number LIKE ?");
            params.add("%" + panNumber.trim() + "%");
        }

        // Add pagination to main query
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        int offset = (page - 1) * limit;
        int totalCount = 0;

        try (Connection conn = DbConnection.getCon()) {
            // Execute count query first
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
                for (int i = 0; i < params.size(); i++) {
                    ps.setString(i + 1, params.get(i));
                }
                ps.setInt(params.size() + 1, limit);
                ps.setInt(params.size() + 2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ServiceProvider sp = new ServiceProvider();
                        sp.setProviderId(rs.getInt("provider_id"));
                        sp.setName(rs.getString("name"));
                        sp.setEmail(rs.getString("email"));
                        sp.setPhoneNumber(rs.getString("phone_no"));
                        sp.setAlternatePhoneNumber(rs.getString("alternate_phone"));
                        sp.setAddress(rs.getString("address"));
                        sp.setCity(rs.getString("city"));
                        sp.setState(rs.getString("state"));
                        sp.setBusinessName(rs.getString("business_name"));
                        sp.setGstNumber(rs.getString("gst_number"));
                        sp.setAadharNumber(rs.getString("aadhar_number"));
                        sp.setPanNumber(rs.getString("pan_number"));
                        sp.setCreatedAt(rs.getString("created_at"));
                        sp.setStatus(rs.getString("status")); // Added status retrieval
                        providerList.add(sp);
                    }
                }
            }
        }
        logger.debug("Searched {} service providers, total count: {}", providerList.size(), totalCount);
        return new ProviderResult(providerList, totalCount);
    }
}