package shaadisarthi.customer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shaadisharthi.DbConnection.DbConnection;

/**
 * Servlet implementation for handling guest query submissions
 * Allows non-authenticated users to submit support queries without registration
 * 
 * @WebServlet Maps to "/AddGuestQuery" endpoint
 * @version 1.0
 * @description Processes POST requests to store guest queries in database with PENDING status
 */
@WebServlet("/AddGuestQuery")
public class AddGuestQuery extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AddGuestQuery.class);

    /**
     * Processes POST requests for guest query submissions
     * Validates input data and stores queries in database with PENDING status
     * 
     * @param request HttpServletRequest containing JSON payload with query details
     * @param response HttpServletResponse for sending submission result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.debug("Processing POST request for adding guest query at {} IST", 
                     LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Parse request body JSON
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            logger.error("Failed to read request body: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body");
            return;
        }

        JSONObject jsonPayload;
        try {
            jsonPayload = new JSONObject(sb.toString());
        } catch (Exception e) {
            logger.error("Invalid JSON payload: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format");
            return;
        }

        // Extract and validate required fields from JSON payload
        String name = jsonPayload.optString("name", "").trim();
        String email = jsonPayload.optString("email", "").trim();
        String subject = jsonPayload.optString("subject", "").trim();
        String message = jsonPayload.optString("message", "").trim();

        // Input validation - all fields are mandatory
        if (name.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "All fields are required");
            return;
        }

        // Insert guest query into database with PENDING status
        String insertSQL = "INSERT INTO guest_queries (name, email, subject, message, status, created_at) " +
                           "VALUES (?, ?, ?, ?, 'PENDING', NOW())";

        try (Connection con = DbConnection.getCon();
             PreparedStatement ps = con.prepareStatement(insertSQL)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, subject);
            ps.setString(4, message);

            int result = ps.executeUpdate();

            if (result > 0) {
                logger.info("Guest query submitted successfully from email: {} at {}", 
                            email, LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                JSONObject responseJson = new JSONObject();
                responseJson.put("success", true);
                responseJson.put("message", "Query submitted successfully!");
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(responseJson.toString());
                    writer.flush();
                }
            } else {
                logger.warn("Failed to submit guest query from email: {}", email);
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit query");
            }

        } catch (SQLException e) {
            logger.error("Database error submitting guest query: {}", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                      "Database error: " + e.getMessage().replace("'", ""));
        }
    }

    /**
     * Sends standardized error response in JSON format
     * 
     * @param response HttpServletResponse object
     * @param status HTTP status code
     * @param message Error message description
     * @throws IOException if response writing fails
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JSONObject json = new JSONObject();
        json.put("success", false);
        json.put("error", message);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json.toString());
            writer.flush();
        }
    }
}