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

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/uGetPlansPurchased")
public class CustomerGetPlansPurchasedServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public CustomerGetPlansPurchasedServlet() {
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
				result.put("error", "Missing GET parameter.");
			}
			else
			{
			
			JSONObject inputJSON = new JSONObject(json);
			inputString = inputJSON.toString(1);
			IO.p(this.getServletName() + " INPUT:" + inputJSON.toString());
			
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
				  	  		"userid," +
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

				  	    	// Get the plans that the user has purchased
				  	    	
				  	    	Integer userid = rs.getInt("userid");
				  	    	
				  	    	String plans_sql = "SELECT " +
				  	    			" planpurchases.userid," +
				  	    		  	" planpurchases.planid," +
						  	  		" planpurchases.proratacharge, " +
						  	  		" planpurchases.monthlycharge, " +
						  	  		" planpurchases.qpisubscriptionid, " + 
						  	  		" planpurchases.stopped, " +
						  	  		" plans.description " + 
					  	    		" FROM planpurchases" +
						  	  		" LEFT JOIN plans on planpurchases.planid = plans.planid" + 
					  	    		" WHERE planpurchases.userid = ? AND planpurchases.stopped != 1 ";
							
					  	    PreparedStatement plans_stmt = conn.prepareStatement(plans_sql);
					  	    i=1;
					  	    
					  	    plans_stmt.setInt(i++, userid);
					  	    
					  	    ResultSet plans_rs = plans_stmt.executeQuery();

					  	    JSONArray plans = new JSONArray();
					  	    
					  	    while(plans_rs.next())
					  	    {
					  	    	JSONObject thisjo = new JSONObject();
					  	    	thisjo.put("planid", plans_rs.getInt("planid"));
					  	    	thisjo.put("qpisubscriptionid", plans_rs.getInt("qpisubscriptionid"));
					  	    	thisjo.put("monthlycharge", plans_rs.getString("monthlycharge"));
					  	    	thisjo.put("description", plans_rs.getString("description"));
					  	    	plans.put(thisjo);
						    }
					  	    
					  	    result.put("planspurchased", plans);
					  	    
					  	    // Get plans available
					  	    
					  	  String available_plans_sql = "SELECT " +
				  	    		  	" planid, " +
						  	  		" description, " +
						  	  		" monthlycharge " +
					  	    		" FROM plans" +
					  	    		" WHERE enabled = 1 ";
							
					  	    PreparedStatement ap_stmt = conn.prepareStatement(available_plans_sql);
					  	    i=1;
					  	    
//					  	    ap_stmt.setInt(i++, userid);
					  	    
					  	    ResultSet ap_rs = ap_stmt.executeQuery();

					  	    JSONArray availableplans = new JSONArray();
					  	    
					  	    while(ap_rs.next())
					  	    {
					  	    	JSONObject thisjo = new JSONObject();
					  	    	thisjo.put("planid", ap_rs.getInt("planid"));
					  	    	thisjo.put("description", ap_rs.getString("description"));
					  	    	thisjo.put("monthlycharge", ap_rs.getString("monthlycharge"));
					  	    	availableplans.put(thisjo);
						    }
					  	    
					  	    result.put("availableplans", availableplans);
					  	    
					  	    result.put("success", true);
					  	    
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
			IO.p(this.getServletName()  + " OUTPUT:" + result.toString());
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
