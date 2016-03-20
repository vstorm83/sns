package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/getAccountDetails")
public class GetAccountDetailsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public GetAccountDetailsServlet() {
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
			
				if (!(inputJSON.has("sessionid") ))
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
				  	  		"userid," +
				  	  		"name, " +
				  	  		"usertype, " +
				  	  		"email " + 
				  	    		" FROM appusers WHERE sessionid = ?";
				  	    
				  	    PreparedStatement sessionidstmt = conn.prepareStatement(sessionidstatement);
				  	    
				  	    int i=1;
				  	    
				  	    sessionidstmt.setString(i++, inputJSON.getString("sessionid"));
				  	    
				  	    ResultSet rs = sessionidstmt.executeQuery();
				  	    
				  	    Integer userid = null;
				  	    Integer userscount=0;
				  	    while(rs.next())
				  	    {
				  	    	userid = rs.getInt("userid");
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
				  	    	

							Calendar aCalendar = Calendar.getInstance();
							aCalendar.set(Calendar.DATE, 1);

							Date firstDateOfMonth = aCalendar.getTime();
							Timestamp firstDateOfMonth_timestamp = new Timestamp(firstDateOfMonth.getTime());
						
							aCalendar.set(Calendar.DATE,     aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
							
							Date lastDateOfMonth = aCalendar.getTime();
							Timestamp lastDateOfMonth_timestamp = new Timestamp(lastDateOfMonth.getTime());

							String pickups_statement = "SELECT COUNT(*) as pickupcount FROM pickups WHERE timestamp > ? AND timestamp < ? AND ownerid = ?";
						  	    
					  	    PreparedStatement p_stmt = conn.prepareStatement(pickups_statement);
					  	    i=1;
					  	    
					  	    p_stmt.setTimestamp(i++, firstDateOfMonth_timestamp);
					  	    p_stmt.setTimestamp(i++, lastDateOfMonth_timestamp);
					  	    p_stmt.setInt(i++, userid);
					  	    
					  	    ResultSet p_rs = p_stmt.executeQuery();
					  	    
					  	    Integer pickups_count=0;
					  	    
					  	    if(p_rs.next())
					  	    {
					  	    	pickups_count = p_rs.getInt("pickupcount");
					  	    }
					  	    
					  	    //  Count deliveries

							String deliveries_statement = "SELECT COUNT(*) as deliverycount FROM deliveries WHERE timestamp > ? AND timestamp < ? AND ownerid = ?";
						  	    
					  	    PreparedStatement d_stmt = conn.prepareStatement(deliveries_statement);
					  	    i=1;
					  	    
					  	    d_stmt.setTimestamp(i++, firstDateOfMonth_timestamp);
					  	    d_stmt.setTimestamp(i++, lastDateOfMonth_timestamp);
					  	    d_stmt.setInt(i++, userid);
					  	    
					  	    ResultSet d_rs = d_stmt.executeQuery();
					  	    
					  	    Integer deliveries_count=0;
					  	    
					  	    if(d_rs.next())
					  	    {
					  	    	deliveries_count = d_rs.getInt("deliverycount");
					  	    }
					  	    	
							//result.put("btpubkey", Finances.getBTPubKey());
							
							JSONArray items = new JSONArray();
							JSONObject thisitem = new JSONObject();
							
							thisitem.put("name","Account Type");
							thisitem.put("value", rs.getString("usertype"));
							items.put(thisitem);
							
							if (rs.getString("usertype").equalsIgnoreCase("courier"))
							{
							
							}
							else
							{
								thisitem = new JSONObject();
								thisitem.put("name","Account status");
								thisitem.put("value","Unknown");
								items.put(thisitem);
								
								thisitem = new JSONObject();
								thisitem.put("name","Free trips left");
								thisitem.put("value", Math.max(0, 2 - (pickups_count + deliveries_count)));
								items.put(thisitem);
								
								thisitem = new JSONObject();
								thisitem.put("name","Credit card attached");
								thisitem.put("value","Unknown");
								items.put(thisitem);
							}
							
							thisitem = new JSONObject();
							thisitem.put("name","Email");
							thisitem.put("value",rs.getString("email"));
							items.put(thisitem);
							
							result.put("items", items);
							
				  	    } // End of participant-check
					  	    
				} // End of check JSON parameters
					
			} // End of check GET parameters.	

		}
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
