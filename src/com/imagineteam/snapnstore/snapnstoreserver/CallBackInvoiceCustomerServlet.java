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

@WebServlet("/invoiceCustomer")
public class CallBackInvoiceCustomerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public CallBackInvoiceCustomerServlet() {
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
		
		Connection conn = null;
		
		try {

			String qpisubscriptionid = request.getParameter("qpisubscriptionid");
			String qpitransactionid = request.getParameter("qpitransactionid");
			String qpicustomerid = request.getParameter("qpicustomerid");
			String qpichargeamount = request.getParameter("qpichargeamount");
			
			if (!(qpisubscriptionid != null && qpitransactionid != null && null != qpicustomerid && null != qpichargeamount))
			{
				result.put("success", false);
				result.put("error", "Missing/incorrect mandatory parameters");
				Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " error was :" + result.getString("error"));
			}
			else
			{
						IO.p("QPI TransactionID was: " + qpitransactionid);
						IO.p("QPI SubscriptionID was: " + qpisubscriptionid);
						IO.p("QPI CustomerID was: " + qpicustomerid);
						IO.p("QPI Charge Amount was: " + qpichargeamount);
			
						Class.forName("com.mysql.jdbc.Driver");
						
						conn = DriverManager
							.getConnection("jdbc:mysql://localhost/wp_snapnstore?"
									+ "user=wpsnapuser&password=wpsnappassword");
				  	    	
					    	  String details_sql = "SELECT " +
						  	  		"planpurchases.planid, " +
						  	  		"planpurchases.qpicustomerid, " +
						  	  		"planpurchases.qpisubscriptionid, " +
						  	  		"appusers.name, " +
						  	  		"appusers.email, " +
						  	  		"plans.description " +
					  	    		"FROM planpurchases " + 
					  	    		"LEFT JOIN appusers ON planpurchases.userid = appusers.userid " +
					  	    		"LEFT JOIN plans ON planpurchases.planid = plans.planid " +
					  	    	 	"WHERE planpurchases.qpicustomerid = ? AND planpurchases.qpisubscriptionid = ?";
						  	    
						  	    PreparedStatement d_stmt = conn.prepareStatement(details_sql);
						  	    
						  	    int i=1;
						  	    
						  	    d_stmt.setString(i++, qpicustomerid);
						  	    d_stmt.setString(i++, qpisubscriptionid);
						  	    
						  	    ResultSet d_rs = d_stmt.executeQuery();
						  	    d_rs.beforeFirst();
						  	    
						  	    if(!d_rs.next())
						  	    {
						  	    	  result.put("success", false);
							    	  result.put("error", "Callback failed fetching details for qpisubscriptionid: '" + qpisubscriptionid + "', customer was not emailed.");
							    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " error was :" + result.getString("error"));
						  	    }
						  	    else
						  	    {
						  	    
						  	    	String name = d_rs.getString("name");
						  	    	String email = d_rs.getString("email");
						  	    	String plandesc = d_rs.getString("description");
						  	    	
						  	    	String qpi_subscriptionid = d_rs.getString("qpisubscriptionid");

						  	    	
						  	    	 Email.sendCustomerInvoiceEmail(getServletContext(),
						  	    			 name,
						  	    			email,
						  	    			 "<b>Item</b>: Storage plan per month <br/>" +
				  	    					 "<b>Description</b>: " + plandesc + " <br/>" +
						  	    			 "<b>Amount (AUD)</b>: $" + qpichargeamount + "<br/>" +
						  	    			 "(GST Inclusive)<br>" +
						  	    			 "<b>Subscription ID</b>: " + qpi_subscriptionid + "<br/>"
						  	    			 );
						  	    }
						  	    
				    	 result.put("success", true);
																				    	  
			} // End of check get parameters.	


		}// End of input parameter check
		catch (SQLException e) 
		{
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("error", "Problem with SQL:" + e.getMessage());
	        result.put("usererror", "Issue recording payment. Please contact support");
	    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " " + result.getString("error"));
		} 
		catch (Exception e) 
		{
		e.printStackTrace();
        result.put("success", false);
        result.put("error", "Exception:" + e.getMessage());
    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " " + result.getString("error"));
        
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
