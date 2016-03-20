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

@WebServlet("/upgradePlan")
public class UpgradePlanServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public UpgradePlanServlet() {
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
		
		String json = request.getParameter("qpiparams");
		String inputString = "(No input read)";
		Connection conn = null;
		
		
		String qpicustomerid= request.getParameter("qpicustomerid");
		String qpisubscriptionid= request.getParameter("qpisubscriptionid");
		String qpitransactionid = request.getParameter("qpichargenowtransactionid");
		String qpichargenowamount = request.getParameter("qpichargenowamount");
		
		// Would look up the transaction here.
		Boolean paid = false;
		
		if (qpisubscriptionid != null)
		{
			paid = true;
		}
		else
		{
			paid = false;
		}
		
		try {

			if ( null==json || null==qpitransactionid || null==qpichargenowamount )
			{
				result.put("success", false);
				result.put("error", "Missing POST parameter(s).");
			}
			else
			{
				
				JSONObject inputJSON = new JSONObject(json);
				inputString = inputJSON.toString(1);
				IO.p(this.getServletName() + " INPUT:" + inputJSON.toString(1));
			
				if (!(inputJSON.has("sessionid") && inputJSON.has("oldplan") && inputJSON.has("newplan") ))
				{
					result.put("success", false);
					result.put("error", "Missing mandatory parameters");
					Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				}
				else if (!paid)
				{
					result.put("success", false);
					result.put("error", "Unsuccessful transaction.");
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
				  	  		" email," + 
				  	  		" name" + 
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
					    	  Email.sendAlertEmail(getServletContext(), "WARNING", "WARNING in " + this.getServletName() + result.getString("error"));
				  	    }
				  	  	else if (userscount.equals(1))
				  	    {
					  	      rs.beforeFirst();
					  	      rs.next();
					  	      
					  	      String email = rs.getString("email");
					  	      String name = rs.getString("name");
				  	    	
					  	      // Close off ONE old plan
					    	  String closepurchase_sql = "UPDATE planpurchases " +
					    			  "SET stopped = 1, " +
					    			  "stoppedtime = now() " +
					    			  "WHERE planid = ? AND userid = ? LIMIT 1";
						      
						      PreparedStatement cp_stmt = conn.prepareStatement(closepurchase_sql,Statement.RETURN_GENERATED_KEYS);
						      
						      i=1;
						      cp_stmt.setInt(i++, inputJSON.getInt("oldplan"));
						      cp_stmt.setInt(i++, rs.getInt("userid"));

						      int closepurchasesuccess = 2;
						      closepurchasesuccess = cp_stmt.executeUpdate();
						      
						      if(closepurchasesuccess != 1) 
						      {
						    	  result.put("success", false);
						    	  result.put("error", "Could not mark the old plan as closed.");
						    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
						      }
						      else
						      {
						    	  
					    	  		// Find new plan monthlycharge
					    	  		String newplan_sql = "SELECT " +
							  	  		" monthlycharge " +
						  	    		" FROM plans" +
						  	    		" WHERE planid = ?";
						  	      
							  	    PreparedStatement newplan_stmt = conn.prepareStatement(newplan_sql);
							  	    i=1;
							  	    
							  	    newplan_stmt.setInt(i++, inputJSON.getInt("newplan"));
							  	    
							  	    ResultSet newplan_rs = newplan_stmt.executeQuery();
							  	    
							  	    if(newplan_rs.next())
							  	    {
							  	    	  String monthlycharge = newplan_rs.getString("monthlycharge");
							  	    
						  	    		  String tokenstatement = "INSERT INTO planpurchases ( planid , userid, qpicustomerid,  proratacharge, monthlycharge, qpisubscriptionid, qpitransactionid, created ) " +
									    	 		" VALUES ( ? , ? , ? , ? , ? , ? , ? , now() )";
									      
									      PreparedStatement tokenstmt = conn.prepareStatement(tokenstatement,Statement.RETURN_GENERATED_KEYS);
									      
									      i=1;
									      
									      tokenstmt.setInt(i++, inputJSON.getInt("newplan"));
									      tokenstmt.setInt(i++, rs.getInt("userid"));
									      tokenstmt.setString(i++, qpicustomerid);
									      tokenstmt.setString(i++, qpichargenowamount);
									      tokenstmt.setString(i++, monthlycharge);
									      tokenstmt.setString(i++, qpisubscriptionid);
									      tokenstmt.setString(i++, qpitransactionid);
									      
									      int success = 2;
									      success = tokenstmt.executeUpdate();
									      
									      if (success == 0) 
									      {
									    	  result.put("success", false);
									    	  result.put("error", "Could not record purchase of new plan.");
									    	  result.put("usererror", "Could not set/change plans, please contact support.");
									      }
									      else if (1 != success)
									      {
									    	  result.put("success", false);
									    	  result.put("error", "More than one plan purchased.");
									    	  Email.sendAlertEmail(getServletContext(), "WARNING", "WARNING in " + this.getServletName() + result.getString("error"));
									      }
									      else if(success == 1) 
									      {
									    	  result.put("success", true);
									    	  
									    	   String planstatement = "SELECT " +
										  	  		" description" +
										  	    	" FROM plans WHERE planid = ?";
										  	    
									    	   
									    	   
										  	    PreparedStatement plan1stmt = conn.prepareStatement(planstatement);
										  	    
										  	    plan1stmt.setInt(1, inputJSON.getInt("oldplan"));
										  	    
										  	    ResultSet p1rs = plan1stmt.executeQuery();
		
										  	    p1rs.beforeFirst();
										  	    
										  	    String plan1desc = "Plan ID" +  inputJSON.getInt("oldplan");
										  	    
										  	    if(p1rs.next())
										  	    {
										  	    	plan1desc = p1rs.getString("description");
										  	    }

										  	    PreparedStatement plan2stmt = conn.prepareStatement(planstatement);
										  	    
										  	    plan2stmt.setInt(1, inputJSON.getInt("newplan"));
										  	    
										  	    ResultSet p2rs = plan2stmt.executeQuery();
		
										  	    p2rs.beforeFirst();
										  	    
										  	    String plan2desc = "Plan ID" +  inputJSON.getInt("newplan");
										  	    
										  	    if(p2rs.next())
										  	    {
										  	    	plan2desc = p2rs.getString("description");
										  	    }
										  	    
										  	    Double tempdouble = new Double(qpichargenowamount);
								  	    
									    	  Email.sendCustomerInvoiceEmail(getServletContext(), name, email,
									    			  "Item: Plan upgrade from '" + plan1desc + "' plan to '" + plan2desc +  "' plan<br/>" + 
									    		      "Pro-rata amount charged (AUD) : $" + IO.round(tempdouble,2) + "<br/>" + 
						    					  	  "GST Included.<br/>" +
						    					  	  "Transaction ID: " + qpitransactionid + "<br/>"
									    			  );
									      }
							  	    }
							  	    else
							  	    {
							  	    	 result.put("success", false);
								    	 result.put("error", "Could not find new plan price.");
								    	 Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
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
