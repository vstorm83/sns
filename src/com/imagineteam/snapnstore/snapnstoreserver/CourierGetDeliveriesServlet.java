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

@WebServlet("/cGetDeliveries")
public class CourierGetDeliveriesServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public CourierGetDeliveriesServlet() {
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
				  	    		
					  	    	String deliveries_sql = "SELECT " +
					  	    		  	" deliveryid," +
					  	    		  	" deliverytime," +
					  	    		  	" deliveryaddress," +
					  	    		  	" itemgroupid, " +
					  	    		  	" appusers.name as customername " +
						  	    		" FROM deliveries" +
						  	    		" LEFT JOIN appusers ON deliveries.ownerid = appusers.userid" +
						  	    		" WHERE completed != 1";
							  	    
							  	    PreparedStatement du_stmt = conn.prepareStatement(deliveries_sql);
							  	    
							  	    ResultSet deliveries_rs = du_stmt.executeQuery();
							  	    
							  	    JSONArray deliveries = new JSONArray();
							  	    
							  	    while(deliveries_rs.next())
							  	    {
							  	    	Integer itemcount = 0;
							  	    	
						  	    		String itemcount_sql = "SELECT " +
						  	    		  	" count(*) as itemcount" +
							  	    		" FROM itemgroups" +
							  	    		" WHERE itemgroupid = ?";
								  	    
								  	    PreparedStatement ic_stmt = conn.prepareStatement(itemcount_sql);
								  	    i=1;
								  	    
								  	    ic_stmt.setInt(i++, deliveries_rs.getInt("itemgroupid"));
								  	    
								  	    ResultSet ic_rs = ic_stmt.executeQuery();
								  	    
								  	    if (ic_rs.next())
								  	    {
								  	    	itemcount = ic_rs.getInt("itemcount");
								  	    }
								  	    
							  	    	JSONObject thisdelivery = new JSONObject();
							  	    	
							  	    	thisdelivery.put("deliveryid", deliveries_rs.getInt("deliveryid"));
							  	    	
							  	    	Timestamp dts = deliveries_rs.getTimestamp("deliverytime");
							  	    	
							  	    	String deliverytime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(dts);
							  	    	
							  	    	thisdelivery.put("deliverytime", deliverytime);
							  	    	thisdelivery.put("itemcount", itemcount);
							  	    	thisdelivery.put("customername", deliveries_rs.getString("customername"));
							  	    	thisdelivery.put("customeraddress", deliveries_rs.getString("deliveryaddress"));

							  	    	IO.p("Showing delivery: " + thisdelivery.toString());
							  	    	
							  	    	JSONArray items = new JSONArray();
							  	    	
							  	    	String items_sql = "SELECT " +
							  	    		  	" items.itemid, " +
							  	    		  	" items.imageurl, " +
							  	    		  	" items.location " +
								  	    		" FROM itemgroups" + 
								  	    		" LEFT JOIN items ON itemgroups.itemid = items.itemid " +
								  	    		" WHERE itemgroups.itemgroupid = ?";
									  	    
									  	    PreparedStatement i_stmt = conn.prepareStatement(items_sql);
									  	    i=1;
									  	    
									  	    i_stmt.setInt(i++, deliveries_rs.getInt("itemgroupid"));
									  	    
									  	    ResultSet i_rs = i_stmt.executeQuery();
									  	    
									  	    while(i_rs.next())
									  	    {
									  	    	JSONObject item = new JSONObject();
									  	    	
									  	    	item.put("itemid", i_rs.getString("itemid"));
									  	    	item.put("imageurl", i_rs.getString("imageurl"));
									  	    	item.put("location", i_rs.getString("location"));
									  	    	
									  	    	items.put(item);
									  	    	IO.p("Showing items: " + item.toString());
									  	    }
									  	    
									  	  thisdelivery.put("items", items);
							  	    	
							  	    	deliveries.put(thisdelivery);
							  	    }
							  	    		
							  	    result.put("deliveries", deliveries);
							  	    
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
