package shaadisharthi.admin;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.json.JSONObject;

import java.io.IOException;

public class LogoutAdmin extends HttpServlet {

  
 protected void doPost(HttpServletRequest request, HttpServletResponse response)
        	      throws ServletException, IOException {
        	    response.setContentType("application/json");
        	    response.setCharacterEncoding("UTF-8");

        	    try {
        	      

        	      // Optional: Blacklist JWT token (if you maintain a server-side blacklist)
        	      String authHeader = request.getHeader("Authorization");
        	      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        	        String token = authHeader.substring(7);
        	        // Add token to blacklist (e.g., store in DB or Redis)
        	        // Example: blacklistToken(token);
        	        System.out.println("JWT token received for logout: " + token);
        	      }

        	      // Send success response
        	      JSONObject responseJson = new JSONObject()
        	          .put("message", "Logged out successfully");
        	      response.setStatus(HttpServletResponse.SC_OK);
        	      response.getWriter().write(responseJson.toString());
        	      //System.out.println("Logout response: " + responseJson.toString());
        	    } catch (Exception e) {
        	      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	      response.getWriter().write(
        	          new JSONObject()
        	              .put("message", "Logout error: " + e.getMessage())
        	              .toString()
        	      );
        	      System.out.println("Logout error: " + e.getMessage());
        	    }
        	  }
}
