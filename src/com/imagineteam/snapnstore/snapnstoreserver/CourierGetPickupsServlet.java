package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/cGetPickups")
public class CourierGetPickupsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public CourierGetPickupsServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		IO.alertDodgyGetRequest(getServletName(), getServletContext(), request);
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	IO.p(this.getServletName() + ":");
		
		response.setCharacterEncoding("UTF-8");
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
				result.put("error", "Missing POST parameter.");
			}
			else
			{
				
			JSONObject inputJSON = new JSONObject(json);
			inputString = inputJSON.toString(1);
			IO.p(this.getServletName() + " INPUT:" + inputJSON.toString(1));
			
				if (!(inputJSON.has("sessionid")))
				{
					result.put("success", false);
					result.put("error", "Missing mandatory parameters");
				}
				else
				{
					
				Class.forName("com.mysql.jdbc.Driver");

				conn = DriverManager
						.getConnection("jdbc:mysql://localhost/wp_snapnstore?"
								+ "user=wpsnapuser&password=wpsnappassword");
				
					// Check if sessionid is valid
					
			    	  String sessionidstatement = "SELECT " +
				  	  		"sessionid, " +
				  	  		"userid, " +
				  	  		"usertype, " +
				  	  		"email " +
				  	    		" FROM appusers WHERE sessionid = ?";
				  	    
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
					    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				  	    }
				  	    else if (userscount.equals(1))
				  	    {
				  	    	rs.beforeFirst();
				  	    	rs.next();
				  	    	
				  	    	Integer userid = rs.getInt("userid");
				  	    	if (!rs.getString("usertype").equals("courier"))
				  	    	{
				  	    		result.put("success", false);
								result.put("error", "This user is not a courier.");
								result.put("usererror", "You are not registered as a courier. Please contact support.");
						    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				  	    	}
				  	    	else
				  	    	{
				  	    		
					  	        // Go and get all the chats that the user is participating in
					  	        
					  	    	String pickups_sql = "SELECT " +
					  	    		  	" pickupid," +
					  	    		  	" pickuptime," +
					  	    		  	" pickupaddress," +
					  	    		  	" itemcount, " +
					  	    		  	" difficultflag, " +
					  	    		  	" appusers.name as customername " +
						  	    		" FROM pickups" +
						  	    		" LEFT JOIN appusers ON pickups.ownerid = appusers.userid" +
						  	    		" WHERE completed != 1";
							  	    
							  	    PreparedStatement pu_stmt = conn.prepareStatement(pickups_sql);
							  	    //i=1;
							  	    
							  	    //pu_stmt.setInt(i++, userid);
							  	    
							  	    ResultSet pickups_rs = pu_stmt.executeQuery();
							  	    
							  	    JSONArray pickups = new JSONArray();
							  	    
							  	    while(pickups_rs.next())
							  	    {
								  	    
							  	    	JSONObject thispickup = new JSONObject();
							  	    	

							  	    	Timestamp pts = pickups_rs.getTimestamp("pickuptime");
							  	    	String pickuptime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(pts);
							  	    	
							  	    	thispickup.put("pickupid", pickups_rs.getInt("pickupid"));
							  	    	thispickup.put("pickuptime", pickuptime);
							  	    	thispickup.put("itemcount", pickups_rs.getInt("itemcount"));
							  	    	thispickup.put("customername", pickups_rs.getString("customername"));
							  	    	thispickup.put("customeraddress", pickups_rs.getString("pickupaddress"));
							  	    	thispickup.put("difficultyflag", pickups_rs.getInt("difficultflag"));
							  	    	
							  	    	pickups.put(thispickup);
							  	    }
							  	    		
							  	    result.put("pickups", pickups);
							  	    
							  	    result.put("success",true);
					  	        
					  	    }
				  	    }	    
				  	    
				} // End of check JSON parameters
					
			} // End of check get parameters.	


		}// End of input parameter check
		catch (SQLException e) 
		{
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("error", "Problem with SQL:" + e.getMessage());
	    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
		} 
		catch (Exception e) 
		{
		e.printStackTrace();
        result.put("success", false);
        result.put("error", "Exception:" + e.getMessage());
    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
        
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
