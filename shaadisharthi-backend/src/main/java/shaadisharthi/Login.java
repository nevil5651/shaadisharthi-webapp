package shaadisharthi;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import shaadisharthi.DbConnection.DbConnection;
import shaadisharthi.security.JwtUtil;

/**
 * Servlet for handling service provider authentication
 * Provides secure login functionality with JWT token generation
 * 
 * Features:
 * - Email and password authentication
 * - BCrypt password verification
 * - JWT token generation for session management
 * - Provider status validation
 * 
 * @WebServlet Maps to "/login" endpoint
 * @author ShaadiSharthi Team
 * @version 1.0
 */
@WebServlet("/login")
public class Login extends HttpServlet {

    /**
     * Handles POST requests for service provider login
     * Authenticates credentials and generates JWT tokens for valid users
     * 
     * @param request HttpServletRequest with JSON credentials
     * @param response HttpServletResponse with authentication result
     * @throws ServletException If servlet processing fails
     * @throws IOException If response writing fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // Read JSON request body
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // Parse JSON credentials
            JSONObject json = new JSONObject(sb.toString());
            String email = json.getString("email").trim();
            String password = json.getString("password").trim();

            // Database query to find provider by email
            Connection con = DbConnection.getCon();
            String query = "SELECT provider_id, password, status FROM service_providers WHERE email = ?";
            PreparedStatement pstmt = con.prepareStatement(query);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbHashedPassword = rs.getString("password");
                int providerId = rs.getInt("provider_id");
                String status = rs.getString("status").toUpperCase(); // Convert to uppercase for consistency
                String role = "SERVICE_PROVIDER"; // Fixed role for service providers

                // Verify password using BCrypt
                if (BCrypt.checkpw(password, dbHashedPassword)) {
                    // Generate JWT token for authenticated session
                    String token = JwtUtil.generateToken(providerId, role);

                    // Construct success response with token and provider details
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("token", token);
                    responseJson.put("providerId", providerId);
                    responseJson.put("status", status);
                    responseJson.put("role", role);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(responseJson.toString());
                } else {
                    // Invalid password - return unauthorized
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"message\":\"Wrong Credentials.\"}");
                }
            } else {
                // User not found - return not found
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"message\":\"Wrong Credentials.\"}");
            }

            // Clean up database resources
            rs.close();
            pstmt.close();
            con.close();

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\":\"Database error.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\":\"Server error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

}