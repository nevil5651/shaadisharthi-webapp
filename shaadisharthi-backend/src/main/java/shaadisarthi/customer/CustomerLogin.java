package shaadisarthi.customer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.security.JwtUtil;

/**
 * Servlet implementation for customer authentication
 * Handles customer login with email/password validation and JWT token generation
 * 
 * @WebServlet Maps to "/signin" endpoint
 * @version 1.0
 * @description Processes POST requests for customer authentication using BCrypt password verification
 */
@WebServlet("/signin")
public class CustomerLogin extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CustomerLogin.class.getName());

    /**
     * Processes POST requests for customer login
     * Validates credentials, verifies password with BCrypt, and issues JWT tokens
     * 
     * @param request HttpServletRequest containing JSON with email and password
     * @param response HttpServletResponse for sending authentication result
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        LOGGER.info("Received POST request to /signin for customer login");

        try {
            // Read JSON body from request
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            LOGGER.fine("Request body: " + sb.toString());

            // Parse JSON payload
            JSONObject json = new JSONObject(sb.toString());
            String email = json.optString("email", "").trim();
            String password = json.optString("password", "").trim();
            LOGGER.info("Processing login for email: " + email);

            // Input validation with regex for email format
            if (email.isEmpty() || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                LOGGER.warning("Invalid or missing email: " + email);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\":\"Invalid or missing email.\"}");
                return;
            }
            if (password.isEmpty()) {
                LOGGER.warning("Password is missing");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"message\":\"Password is required.\"}");
                return;
            }

            // Database query to retrieve customer credentials
            LOGGER.fine("Executing database query for email: " + email);
            Connection con = DbConnection.getCon();
            String query = "SELECT customer_id, password FROM customers WHERE email = ?";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbHashedPassword = rs.getString("password");
                int customerId = rs.getInt("customer_id");
                String role = "CUSTOMER";
                LOGGER.fine("Found customer with ID: " + customerId);

                // Verify password using BCrypt
                if (BCrypt.checkpw(password, dbHashedPassword)) {
                    LOGGER.info("Password verified for customer ID: " + customerId);
                    // Generate JWT token for authenticated session
                    String token = JwtUtil.generateToken(customerId, role);
                    LOGGER.fine("Generated JWT token for customer ID: " + customerId);

                    // Construct successful response with token and customer info
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("token", token);
                    responseJson.put("customerId", customerId);
                    //responseJson.put("role", role);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(responseJson.toString());
                    LOGGER.info("Login successful for customer ID: " + customerId);
                } else {
                    LOGGER.warning("Incorrect password for email: " + email);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"message\":\"Invalid Credentials.\"}");
                }
            } else {
                LOGGER.warning("Email not found: " + email);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"message\":\"Invalid Credentials.\"}");
            }

            // Clean up database resources
            rs.close();
            pstmt.close();
            con.close();
            LOGGER.fine("Database resources closed");

        } catch (SQLException e) {
            LOGGER.severe("Database error during login: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\":\"Database error.\"}");
        } catch (Exception e) {
            LOGGER.severe("Server error during login: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\":\"Server error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}