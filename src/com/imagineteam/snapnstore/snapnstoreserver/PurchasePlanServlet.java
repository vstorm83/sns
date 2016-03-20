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

@WebServlet("/purchasePlan")
public class PurchasePlanServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public PurchasePlanServlet() {
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
		
		
		String qpisubscriptionid = request.getParameter("qpisubscriptionid");
		String qpitransactionid = request.getParameter("qpitransactionid");
		String qpicustomerid = request.getParameter("qpicustomerid");
		String qpiparams = request.getParameter("qpiparams");
		Boolean paid=null;
		
		// Would look up the transaction here.
		
		if (qpisubscriptionid != null && qpitransactionid != null && qpiparams != null && null != qpicustomerid)
		{
			json = qpiparams;
			IO.p("QPI TransactionID was: " + qpitransactionid);
			IO.p("QPI SubscriptionID was: " + qpisubscriptionid);
			IO.p("QPI CustomerID was: " + qpicustomerid);
			paid = true;
		}
		else
		{
			paid = false;
		}
		
		try {

			if (null!=json ) IO.p("json = " + json);
			if (null!=paid ) IO.p("paid = " + paid);
			if (null!=qpitransactionid ) IO.p("qpitransactionid = " + qpitransactionid);
			if (null!=qpisubscriptionid ) IO.p("qpisubscriptionid = " + qpisubscriptionid);
			if (null!=qpicustomerid ) IO.p("qpicustomerid = " + qpicustomerid);
			
			if ( null==json || null==paid || null==qpitransactionid || null==qpisubscriptionid || null==qpicustomerid)
			{
				result.put("success", false);
				result.put("error", "Missing POST parameter(s).");
			}
			else
			{
				
				JSONObject inputJSON = new JSONObject(json);
				inputString = inputJSON.toString(1);
				IO.p(this.getServletName() + " INPUT:" + inputJSON.toString(1));
			
				if (!(inputJSON.has("sessionid") && inputJSON.has("planchosen") && inputJSON.has("proratacharge") && inputJSON.has("monthlycharge")  ))
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
				  	    	  
					  	      Integer userid = rs.getInt("userid");
					  	      String email = rs.getString("email");
					  	      String name = rs.getString("name");
					  	      
			  	    		  String tokenstatement = "INSERT INTO planpurchases ( "
			  	    		  		+ "planid, "
			  	    		  		+ "userid, "
			  	    		  		+ "qpicustomerid, "
			  	    		  		+ "proratacharge, "
			  	    		  		+ "monthlycharge, "
			  	    		  		+ "qpitransactionid, "
			  	    		  		+ "qpisubscriptionid, "
			  	    		  		+ "created ) " +
						    	 		" VALUES "
						    	 		+ "( ? , ? , ? , ? , ? , ? , ? , now() )";
						      
						      PreparedStatement tokenstmt = conn.prepareStatement(tokenstatement,Statement.RETURN_GENERATED_KEYS);
						      
						      i=1;
						      
						      tokenstmt.setInt(i++, inputJSON.getInt("planchosen"));
						      tokenstmt.setInt(i++, userid);
						      tokenstmt.setString(i++, qpicustomerid);
						      tokenstmt.setString(i++, String.valueOf(inputJSON.getDouble("proratacharge")));
						      tokenstmt.setString(i++, String.valueOf(inputJSON.getDouble("monthlycharge")));
						      tokenstmt.setString(i++, qpitransactionid);
						      tokenstmt.setString(i++, qpisubscriptionid);
						      
						      int success = 2;
						      success = tokenstmt.executeUpdate();
						      
						      if (success == 0) 
						      {
						    	  result.put("success", false);
						    	  result.put("error", "Could not update plan.");
						    	  result.put("usererror", "Could not set/change plans, please contact support.");
						      }
						      else if (1 != success)
						      {
						    	  result.put("success", false);
						    	  result.put("error", "More than one user account updated.");
						    	  Email.sendAlertEmail(getServletContext(), "WARNING", "WARNING in " + this.getServletName() + result.getString("error"));
						      }
						      else if(success == 1) 
						      {
						    	  result.put("success", true);
						    	  
						    	  
						    	  String planstatement = "SELECT " +
								  	  		" description" +
								  	    	" FROM plans WHERE planid = ?";
								  	    
								  	    PreparedStatement planstmt = conn.prepareStatement(planstatement);
								  	    
								  	    planstmt.setInt(1, inputJSON.getInt("planchosen"));
								  	    
								  	    ResultSet prs = planstmt.executeQuery();

								  	    prs.beforeFirst();
								  	    
								  	    String plandesc = "Plan ID" +  inputJSON.getInt("planchosen");
								  	    
								  	    if(prs.next())
								  	    {
								  	    	plandesc = prs.getString("description");
								  	    }
								  	    
						    	  Email.sendCustomerInvoiceEmail(getServletContext(), 
						    			  name,
						    			  email,
						    			  "Item: Plan '" + plandesc + "' purchased <br/>" + 
			    					  	  "Pro-rata amount charged (AUD) : $" + IO.round(inputJSON.getDouble("proratacharge"),2) + "<br/>" + 
			    					  	  "GST Included.<br/>" +
			    					  	  "TransactionID: " + qpitransactionid + "<br/>"
						    			  );
						    	  
						    	  
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
