package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

@WebServlet("/cCompleteDelivery")
public class CourierCompleteDeliveryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public CourierCompleteDeliveryServlet() {
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
			
				if (!(inputJSON.has("sessionid") && inputJSON.has("deliveryid")))
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
				  	    		
				  	    			Integer deliveryid = inputJSON.getInt("deliveryid");
				  	    		
				  	    			// Check that the delivery isn't already complete
				  	    		
					  	    		String delivery_sql = "SELECT " +
					  	    		  	" completed, " +
					  	    		  	" itemgroupid " +
						  	    		" FROM deliveries" +
						  	    		" WHERE deliveryid = ?";
							  	    
							  	    PreparedStatement d_stmt = conn.prepareStatement(delivery_sql);
							  	    i=1;
							  	    
							  	    d_stmt.setInt(i++, deliveryid);
							  	    
							  	    ResultSet d_rs = d_stmt.executeQuery();
							  	    
							  	    Boolean okay_to_close = false;
							  	    
							  	    Integer itemgroupid = null;
							  	    if(d_rs.next())
							  	    {
							  	    	
							  	    	if (0 == d_rs.getInt("completed"))
							  	    	{
							  	    		okay_to_close = true;
							  	    	}
							  	    	
							  	    	itemgroupid = d_rs.getInt("itemgroupid");
							  	    	
							  	    }
							  	    
							  	    if (!okay_to_close)
							  	    {
							  	    	result.put("success", false);
							  	    	result.put("usererror", "This delivery does not exist or is already closed.");
							  	    	okay_to_close = false;
							  	    }
							  	    else
							  	    {
							  	    	
							  	    	// Mark all the items in 'itemgroupid' as returned.
							  	    	
							  	    	  String finditems_sql = "SELECT items.itemid as theitemid " +
							  	    	  		 "FROM itemgroups " +
							  	    	  		 "LEFT JOIN items ON itemgroups.itemid = items.itemid " + 
							  	    	  		 "WHERE itemgroups.itemgroupid = ?";
							  	    	
							  	    	  PreparedStatement fi_stmt = conn.prepareStatement(finditems_sql);
									  	  i=1;
									  	  
									  	  fi_stmt.setInt(i++, itemgroupid);
									  	  
									  	  ResultSet fi_rs = fi_stmt.executeQuery();
									  	  
									  	  Boolean all_delivered = true;
									  	  while(fi_rs.next())
									  	  {
								  	    	  String return_sql = "UPDATE items SET delivered = 1 WHERE itemid = ?";
								  	    	
							  	    		  PreparedStatement r_stmt = conn.prepareStatement(return_sql,Statement.RETURN_GENERATED_KEYS);
										      
										      i=1;
										      
										      r_stmt.setInt(i++, fi_rs.getInt("theitemid"));
										      
										      int returnsuccess = 2;
										      returnsuccess = r_stmt.executeUpdate();
										      
										      if (returnsuccess == 0) 
										      {
										    	  all_delivered = false;
										      }
									  	  }
							  	    	
									  	  if (!all_delivered)
									  	  {
									  		  result.put("success", false);
									    	  result.put("error", "Could not mark all items as delivered.");
									    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
									  	  }
									  	  else
									  	  {
								  	    	  String update_statement = "UPDATE deliveries SET " + 
										  	  	    "completedby = ? , " +
										  	  	    "completed = 1 " +
									    	 		" WHERE deliveryid = ?";
										      
										      PreparedStatement u_stmt = conn.prepareStatement(update_statement,Statement.RETURN_GENERATED_KEYS);
										      
										      i=1;
										      
										      u_stmt.setInt(i++, userid);
										      u_stmt.setInt(i++, deliveryid);
										      
										      int updatesuccess = 2;
										      updatesuccess = u_stmt.executeUpdate();
										      
										      if (updatesuccess == 0) 
										      {
										    	  result.put("success", false);
										    	  result.put("error", "Could not update delivery as completed.");
										    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
										      }
										      else if(updatesuccess == 1) 
										      {
										    	  result.put("success",true);
										      }
									  	  }
							  	    }
							  	    
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
