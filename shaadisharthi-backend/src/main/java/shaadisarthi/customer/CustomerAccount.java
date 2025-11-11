package shaadisarthi.customer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import io.jsonwebtoken.Claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;

/**
 * Servlet implementation for customer account management
 * Handles retrieval and updating of customer profile information
 * 
 * @WebServlet Maps to "/cstmr-acc" endpoint
 * @version 1.0
 * @description Processes GET requests for profile retrieval and POST requests for profile updates
 */
@WebServlet("/cstmr-acc")
public class CustomerAccount extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerAccount.class);

    /**
     * Handles GET requests for retrieving customer profile information
     * Validates JWT claims and returns complete customer profile data
     * 
     * @param request HttpServletRequest with JWT claims attribute
     * @param response HttpServletResponse containing profile data in JSON format
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract and validate JWT claims from request
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            LOGGER.warn("Unauthorized access: Missing or invalid JWT claims");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized: Missing or invalid JWT claims");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(claims.getSubject());
            LOGGER.debug("Processing GET request for customer ID: {}", customerId);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid customer ID in JWT: {}", claims.getSubject(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid customer ID in JWT");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        try (Connection conn = DbConnection.getCon()) {
            Profile profile = fetchProfile(conn, customerId);
            if (profile == null) {
                LOGGER.warn("Profile not found for customer ID: {}", customerId);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Profile not found");
                out.println(GSON.toJson(errorResponse));
                return;
            }

            LOGGER.info("Successfully fetched profile for customer ID: {}", customerId);
            out.println(GSON.toJson(profile));
        } catch (SQLException e) {
            LOGGER.error("Database error while fetching profile for customer ID: {}", customerId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Database error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        } catch (Exception e) {
            LOGGER.error("Unexpected error while fetching profile for customer ID: {}", customerId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unexpected error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        }
    }

    /**
     * Handles POST requests for updating customer profile information
     * Validates input data and updates customer record in database
     * 
     * @param request HttpServletRequest containing JSON payload with profile updates
     * @param response HttpServletResponse confirming update operation
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            LOGGER.warn("Unauthorized access: Missing or invalid JWT claims");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unauthorized: Missing or invalid JWT claims");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        int customerId;
        try {
            customerId = Integer.parseInt(claims.getSubject());
            LOGGER.debug("Processing POST request for customer ID: {}", customerId);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid customer ID in JWT: {}", claims.getSubject(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid customer ID in JWT");
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Read the entire request body
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read request body for customer ID: {}", customerId, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unable to read request body");
            out.println(GSON.toJson(errorResponse));
            return;
        }
        String jsonPayload = sb.toString();
        LOGGER.debug("Received raw JSON payload: {}", jsonPayload);

        // Parse JSON payload
        JsonObject jsonObject;
        try {
            jsonObject = GSON.fromJson(jsonPayload, JsonObject.class);
            if (jsonObject == null) {
                throw new JsonSyntaxException("Empty JSON payload");
            }
            LOGGER.debug("Parsed JSON payload into JsonObject for customer ID: {}", customerId);
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Invalid JSON payload for customer ID: {}, Error: {}, Raw payload: {}", customerId, e.getMessage(), jsonPayload);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Invalid JSON format: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
            return;
        }

        // Extract parameters from JSON with null safety
        String name = jsonObject.get("name") != null ? jsonObject.get("name").getAsString() : null;
        String phoneNo = jsonObject.has("phone_no") && !jsonObject.get("phone_no").isJsonNull()
                ? jsonObject.get("phone_no").getAsString()
                : null;
        String address = jsonObject.has("address") && !jsonObject.get("address").isJsonNull()
                ? jsonObject.get("address").getAsString()
                : null;

        // Handle "null" string as null value
        if ("null".equalsIgnoreCase(address)) {
            address = null;
            LOGGER.debug("Converted address 'null' to null for customer ID: {}", customerId);
        }

        try {
            validateName(name);
            if (phoneNo != null) validatePhoneNo(phoneNo);
            if (address != null) validateAddress(address);
            LOGGER.debug("Validated input data for customer ID: {}", customerId);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid input data for customer ID: {}, Error: {}", customerId, e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", e.getMessage());
            out.println(GSON.toJson(errorResponse));
            return;
        }

        try (Connection conn = DbConnection.getCon()) {
            updateProfile(conn, customerId, name, phoneNo, address);

            Profile updatedProfile = fetchProfile(conn, customerId);
            if (updatedProfile == null) {
                LOGGER.error("Failed to fetch updated profile for customer ID: {}", customerId);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Failed to fetch updated profile");
                out.println(GSON.toJson(errorResponse));
                return;
            }

            LOGGER.info("Successfully updated profile for customer ID: {}", customerId);
            response.setStatus(HttpServletResponse.SC_OK);
            JsonObject successResponse = new JsonObject();
            successResponse.addProperty("message", "Profile updated successfully");
            successResponse.add("data", GSON.toJsonTree(updatedProfile));
            out.println(GSON.toJson(successResponse));
        } catch (SQLException e) {
            LOGGER.error("Database error while updating profile for customer ID: {}", customerId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Database error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        } catch (Exception e) {
            LOGGER.error("Unexpected error while updating profile for customer ID: {}", customerId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "Unexpected error: " + e.getMessage());
            out.println(GSON.toJson(errorResponse));
        }
    }

    /**
     * Fetches customer profile from database
     * 
     * @param conn Database connection
     * @param customerId ID of the customer to fetch
     * @return Profile object containing customer data, or null if not found
     * @throws SQLException if database operation fails
     */
    private Profile fetchProfile(Connection conn, int customerId) throws SQLException {
        String sql = "SELECT customer_id, name, email, phone_no, address, created_at FROM customers WHERE customer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp createdAtTs = rs.getTimestamp("created_at");
                    String createdAt = null;
                    if (createdAtTs != null) {
                        LocalDateTime ldt = createdAtTs.toLocalDateTime();
                        createdAt = ldt.format(ISO_FORMATTER) + "Z";
                    }

                    return new Profile(
                        "user-" + rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone_no"),
                        rs.getString("address"),
                        createdAt
                    );
                }
                return null;
            }
        }
    }

    /**
     * Updates customer profile in database
     * 
     * @param conn Database connection
     * @param customerId ID of the customer to update
     * @param name Customer's full name
     * @param phoneNo Customer's phone number
     * @param address Customer's address
     * @throws SQLException if database operation fails
     */
    private void updateProfile(Connection conn, int customerId, String name, String phoneNo, String address) throws SQLException {
        String sql = "UPDATE customers SET name = ?, phone_no = ?, address = ? WHERE customer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, phoneNo);
            stmt.setString(3, address); // Will be null if address was "null"
            stmt.setInt(4, customerId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Failed to update profile, no rows affected");
            }
        }
    }

    /**
     * Validates customer name
     * 
     * @param name Name to validate
     * @throws IllegalArgumentException if name is invalid
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Name exceeds maximum length of 255 characters");
        }
    }

    /**
     * Validates phone number format
     * 
     * @param phoneNo Phone number to validate
     * @throws IllegalArgumentException if phone number is invalid
     */
    private void validatePhoneNo(String phoneNo) {
        if (phoneNo == null || !phoneNo.matches("^\\+?\\d{10}$")) {
            throw new IllegalArgumentException("Phone number must be 10 digits, optionally prefixed with +");
        }
    }

    /**
     * Validates address length
     * 
     * @param address Address to validate
     * @throws IllegalArgumentException if address is too long
     */
    private void validateAddress(String address) {
        if (address != null && address.length() > 250) {
            throw new IllegalArgumentException("Address exceeds maximum length of 250 characters");
        }
        // Address is optional, so null or empty is allowed
    }

    /**
     * Inner class representing customer profile structure
     * Used for JSON serialization of profile data
     */
    private static class Profile {
        String id;
        String name;
        String email;
        String phone_no;
        String address;
        String created_at;

        Profile(String id, String name, String email, String phone_no, String address, String created_at) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phone_no = phone_no;
            this.address = address;
            this.created_at = created_at;
        }
    }
}