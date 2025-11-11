package shaadisharthi;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;

/**
 * Servlet for business registration and verification process
 * Handles submission of business details for service provider approval
 * 
 * Features:
 * - Comprehensive business information validation
 * - Document verification (Aadhar, PAN, GST)
 * - Database transaction management
 * - Status transition from 'basic_registered' to 'pending_approval'
 * 
 * @WebServlet Maps to "/business-register" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/business-register") // Adjusted to match deployment context
public class BusinessRegister extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(BusinessRegister.class);

    /**
     * Sends standardized HTTP responses with JSON content
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param json JSON content to send
     * @throws IOException If response writing fails
     */
    private void sendResponse(HttpServletResponse response, int status, JSONObject json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (var writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }

    /**
     * Handles POST requests for business registration
     * Processes business details submission with comprehensive validation
     * 
     * @param request HttpServletRequest with JSON business details
     * @param response HttpServletResponse with registration result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for /ShaadiSharthi/api/business-register");
        JSONObject responseJson = new JSONObject();
        
        // Initialize all business detail fields
        String email = null;
        String businessName = null;
        String phone = null;
        String alternatePhone = null;
        String address = null;
        String state = null;
        String city = null;
        String aadhar = null;
        String gstNo = null;
        String pan = null;

        // Read and parse JSON payload from request
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
            JSONObject jsonPayload = new JSONObject(payload.toString());
            
            // Extract all fields from JSON payload
            email = jsonPayload.getString("email");
            businessName = jsonPayload.getString("businessName");
            phone = jsonPayload.getString("phone");
            alternatePhone = jsonPayload.optString("alternatePhone", null); // Optional field
            address = jsonPayload.getString("address");
            state = jsonPayload.getString("state");
            city = jsonPayload.getString("city");
            aadhar = jsonPayload.getString("aadhar");
            gstNo = jsonPayload.optString("gstNo", null); // Optional field
            pan = jsonPayload.getString("pan");
        } catch (IOException e) {
            logger.error("Failed to read request payload: {}", e.getMessage(), e);
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        } catch (Exception e) {
            logger.warn("Invalid JSON payload: {}", payload);
            responseJson.put("error", "Invalid request payload");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Validate all required fields are present and non-empty
        if (email == null || businessName == null || phone == null || address == null || state == null ||
            city == null || aadhar == null || pan == null || email.trim().isEmpty() || businessName.trim().isEmpty() ||
            phone.trim().isEmpty() || address.trim().isEmpty() || state.trim().isEmpty() || city.trim().isEmpty() ||
            aadhar.trim().isEmpty() || pan.trim().isEmpty()) {
            logger.warn("Missing or empty fields for email: {}", email);
            responseJson.put("error", "All required fields are mandatory");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Validate field formats using regex patterns
        if (!phone.matches("^[0-9]{10}$") || !aadhar.matches("^[0-9]{12}$") || !pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")) {
            logger.warn("Invalid format for phone, aadhar, or pan for email: {}", email);
            responseJson.put("error", "Invalid format for phone, Aadhaar, or PAN");
            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
            return;
        }

        // Database operations with transaction management
        try (Connection con = DbConnection.getCon()) {
            con.setAutoCommit(false); // Start transaction
            try {
                // Verify user exists and is in basic_registered state
                String checkQuery = "SELECT 1 FROM service_providers WHERE email = ? AND status = 'basic_registered' LIMIT 1";
                try (PreparedStatement pstmt = con.prepareStatement(checkQuery)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            logger.info("User not found or not basic_registered for email: {}", email);
                            responseJson.put("error", "User not registered or already submitted business details");
                            sendResponse(response, HttpServletResponse.SC_BAD_REQUEST, responseJson);
                            return;
                        }
                    }
                }

                // Update business details and transition status to pending_approval
                String updateQuery = "UPDATE service_providers SET business_name = ?, phone_no = ?, alternate_phone = ?, address = ?, state = ?, city = ?, aadhar_number = ?, gst_number = ?, pan_number = ?, status = 'pending_approval' WHERE email = ?";
                try (PreparedStatement pstmt = con.prepareStatement(updateQuery)) {
                    pstmt.setString(1, businessName);
                    pstmt.setString(2, phone);
                    pstmt.setString(3, alternatePhone);
                    pstmt.setString(4, address);
                    pstmt.setString(5, state);
                    pstmt.setString(6, city);
                    pstmt.setString(7, aadhar);
                    pstmt.setString(8, gstNo);
                    pstmt.setString(9, pan);
                    pstmt.setString(10, email);
                    
                    int rows = pstmt.executeUpdate();
                    if (rows == 0) {
                        logger.error("Failed to update business details for email: {}", email);
                        responseJson.put("error", "Failed to submit business details");
                        sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
                        return;
                    }
                }

                con.commit(); // Commit transaction on success
                logger.info("Business details submitted for email: {}, status set to pending_approval", email);
                responseJson.put("message", "Business details submitted for approval");
                sendResponse(response, HttpServletResponse.SC_OK, responseJson);
            } catch (SQLException e) {
                con.rollback(); // Rollback on error
                logger.error("Database error for email {}: {}", email, e.getMessage(), e);
                responseJson.put("error", "Database error");
                sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
            }
        } catch (SQLException e) {
            logger.error("Connection error for email {}: {}", email, e.getMessage(), e);
            responseJson.put("error", "Internal server error");
            sendResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseJson);
        }
    }
}