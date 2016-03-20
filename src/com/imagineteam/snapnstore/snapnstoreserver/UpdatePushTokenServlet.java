package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.mysql.jdbc.Statement;

@WebServlet("/updatePushToken")
public class UpdatePushTokenServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public UpdatePushTokenServlet() {
        super();
    }
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	IO.alertDodgyGetRequest(getServletName(), getServletContext(), request);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		IO.p(this.getServletName() + ":");
		
		response.setContentType("application/json");
		Writer writer = response.getWriter();
		JSONObject result = new JSONObject();
		
		String json = request.getParameter("details");
		String inputString = "(No input read)";
		Connection conn = null;
		
		try {

			if (null==json)
			{
				result.put("success", false);
				result.put("error", "Missing GET parameter.");
			}
			else
			{
				
			JSONObject inputJSON = new JSONObject(json);
			inputString = inputJSON.toString(1);
			IO.p(this.getServletName() + " INPUT:" + inputJSON.toString(1));
			
				if (!(inputJSON.has("sessionid") && inputJSON.has("androidpushtoken")))
				{
					result.put("success", false);
					result.put("error", "Missing mandatory parameters");
					Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				}
				else
				{
					
				Class.forName("com.mysql.jdbc.Driver");

				conn = DriverManager
						.getConnection("jdbc:mysql://localhost/wp_snapnstore?"
								+ "user=wpsnapuser&password=wpsnappassword");
				
			    	  String sessionidstatement = "SELECT " +
				  	  		" sessionid, " +
				  	  		" userid," +
				  	  		" name" + 
				  	    	" FROM users WHERE sessionid = ?";
				  	    
				  	    PreparedStatement sessionidstmt = conn.prepareStatement(sessionidstatement);
				  	    int i=1;
				  	    
				  	    sessionidstmt.setString(i++, inputJSON.getString("sessionid"));
				  	    
				  	    ResultSet rs = sessionidstmt.executeQuery();
				  	    
				  	    Integer userscount=0;
				  	    while(rs.next())
				  	    {
				  	    	userscount++;
				  	    }
				  	    
				  	    if (userscount.equals(0))
				  	    {
					    	  result.put("success", false);
					    	  result.put("error", "Invalid sessionid.");
					    	  Email.sendAlertEmail(getServletContext(), "WARNING", "WARNING in " + this.getServletName() + result.getString("error"));
				  	    }
				  	  	else if (userscount.equals(1))
				  	    {
				  	    	rs.beforeFirst();
				  	    	rs.next();
				  	    	
							// Update the android push notification token

				  	    		String tokenstatement = "UPDATE users SET androidpushtoken = ?" +
							    	 		" WHERE sessionid = ?";
							      
							      PreparedStatement tokenstmt = conn.prepareStatement(tokenstatement,Statement.RETURN_GENERATED_KEYS);
							      
							      i=1;
							      
							      tokenstmt.setString(i++, inputJSON.getString("androidpushtoken"));
							      tokenstmt.setString(i++, inputJSON.getString("sessionid"));
							      
							      int success = 2;
							      success = tokenstmt.executeUpdate();
							      
							      if (success == 0) 
							      {
							    	  result.put("success", false);
							    	  result.put("error", "No device-token updated.");
							      }
							      else if(success == 1) 
							      {
							    	  result.put("success", true);
							      }
							      else
							      {
							    	  result.put("success", false);
							    	  result.put("error", "More than one device token updated.");
							      }
							
							/*
							// Update the iPhone push notification token
							if (inputJSON.has("iphonepushtoken"))
							{

				  	    		String tokenstatement = "UPDATE users SET iphonepushtoken = ?" +
							    	 		" WHERE sessionid = ?";
							      
							      PreparedStatement tokenstmt = conn.prepareStatement(tokenstatement,Statement.RETURN_GENERATED_KEYS);
							      
							      i=1;
							      
							      tokenstmt.setString(i++, inputJSON.getString("iphonepushtoken"));
							      tokenstmt.setString(i++, rs.getString("sessionid"));
							      
							      int success = 2;
							      success = tokenstmt.executeUpdate();
							      
							      if (success == 0) 
							      {
							    	  result.put("success", false);
							    	  result.put("error", "No device-token updated.");
							      }
							      else if(success == 1) 
							      {
							    	  result.put("success", true);
							      }
							      else
							      {
							    	  result.put("success", false);
							    	  result.put("error", "More than one device token updated.");
							      }
							}
							*/
							
							
				  	    }
				  	    
					    	  
					      
				  	    
				} // End of check JSON parameters
					
			} // End of check get parameters.	


		}// End of input parameter check
		catch (SQLException e) 
		{
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("error", "Problem with SQL:" + e.getMessage());
		} 
		catch (Exception e) 
		{
		e.printStackTrace();
        result.put("success", false);
        result.put("error", "Exception:" + e.getMessage());
        
		}	
		finally 
		{
			IO.p(this.getServletName()  + " OUTPUT:" + result.toString(1));
			writer.write(result.toString(1));
			writer.close();
			
        if (conn != null) 
          try { 
            conn.close();
            } catch (SQLException ignore) {
         }
        
      }
		
	}
	
}
