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
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mysql.jdbc.Statement;

@WebServlet("/uRequestDelivery")
public class CustomerRequestDeliveryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
    public CustomerRequestDeliveryServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
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
		
		String qpitransactionid = request.getParameter("qpitransactionid");
		String qpiparams = request.getParameter("qpiparams");
		Boolean paid=null;
		
		// Would look up the transaction here.
		
		if (qpitransactionid != null && qpiparams != null)
		{
			json = qpiparams;
			IO.p("QPI TransactionID was: " + qpitransactionid);
			paid = true;
			
		}
		else
		{
			paid = false;
		}
		
		
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
			
				if (!(inputJSON.has("sessionid") && inputJSON.has("address") && inputJSON.has("items") && inputJSON.has("deliverytime")))
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
				  	  		"name, " +
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
				  	    	String email = rs.getString("email");
				  	    	String name = rs.getString("name");
				  	    	
				  	    	//Integer planid = rs.getInt("planid");

					        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					        Date parsedDate = dateFormat.parse(inputJSON.getString("deliverytime"));
					        IO.p("Parsed date is : " + parsedDate.toString() + " and milis = " + parsedDate.getTime());
					        Timestamp deliverytime = new java.sql.Timestamp(parsedDate.getTime());
						    IO.p("Timestamp is then:" + deliverytime.toString());
					        
						    //java.util.Date nowdate= new java.util.Date();
							//Timestamp timestamp = new Timestamp(nowdate.getTime());
				  	    	
						    Date date = new Date();
							  Timestamp timestamp = new Timestamp(date.getTime());
							  
							IO.p("Passed timestamp.");
				  	    	// Check how many free pickups / deliveries left
							
							// Count pickups
							
							Calendar aCalendar = Calendar.getInstance();
							aCalendar.set(Calendar.DATE, 1);

							Date firstDateOfMonth = aCalendar.getTime();
							Timestamp firstDateOfMonth_timestamp = new Timestamp(firstDateOfMonth.getTime());
						
							aCalendar.set(Calendar.DATE,     aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
							
							Date lastDateOfMonth = aCalendar.getTime();
							Timestamp lastDateOfMonth_timestamp = new Timestamp(lastDateOfMonth.getTime());
							
							
							IO.p("About to run pickupcount query.");
							
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
					  	    IO.p("About to run deliverycount query.");
					  	  

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
						  	
						 
					  	    
					  	    Boolean free_ride = ( paid || ((pickups_count + deliveries_count) < 2));
					  	  

					  	    if ( userid.equals(20) || userid.equals(21) )
					  	    {
					  	    	free_ride=true;
					  	    }
					  	    
					  	    // Go and check the list of all items
					  	    
					  	    JSONArray items = inputJSON.getJSONArray("items");
					  	    
					  	    Boolean allvalid = true;
					  	    
					  	    IO.p("About to go through items: " + items.toString());
					  	    
					  	    for (int j=0; j < items.length(); j++)
					  	    {
					  	    	/*
					  	    	JSONObject thisitem = items.getJSONObject(i);
					  	    	
					  	    	if (!thisitem.has("itemid"))
					  	    	{
					  	    		result.put("error", "Error parsing itemid.");
					  	    		allvalid = false;
					  	    		break;
					  	    	}
					  	    	*/
					  	    	
					  	    	Integer thisitemid = items.getInt(j); //thisitem.getInt("itemid");
					  	    	
					  	    	IO.p("Handling item:" + thisitemid);
					  	    	
								String owner_sql = "SELECT COUNT(*) as itemcount FROM items WHERE ownerid = ? AND itemid = ? AND returning != 1 AND delivered != 1";
							  	
						  	    PreparedStatement o_stmt = conn.prepareStatement(owner_sql);
						  	    i=1;
						  	    
						  	    o_stmt.setInt(i++, userid);
						  	    o_stmt.setInt(i++, thisitemid);
						  	    
						  	    ResultSet o_rs = o_stmt.executeQuery();
						  	    
						  	    if(o_rs.next())
						  	    {
						  	    	if (0 == o_rs.getInt("itemcount"))
						  	    	{
						  	    		result.put("error", "Not all those items exist, or you don't own all those items.");
						  	    		allvalid = false;
						  	    		break;
						  	    	}
						  	    	else
						  	    	{
						  	    		
						  	    	}
						  	    }
					  	    }
					  	    
					  	    IO.p("Finished checking item validity");
					  	    
					  	    if (!allvalid)
					  	    {
					  	    	result.put("success", false);
					  	    }
					  	    else
					  	    {
						  	    
					  	    	IO.p("Items all valid.");
					  	    	
						  	    if (!free_ride)
						  	    {
						  	    	// Tell the user to go and pay.
						  	    	
						  	    	result.put("mustpay", true);
						  	    	
						  	    	String charge_statement = "SELECT returncharge FROM plans LIMIT 1";
						  	    	
						  	    	PreparedStatement c_stmt = conn.prepareStatement(charge_statement);
						  	    	
						  	    	//c_stmt.setInt(1, planid);
						  	    	
						  	    	ResultSet c_rs = c_stmt.executeQuery();
						  	    	
						  	    	if(c_rs.next())
						  	    	{
						  	    		result.put("chargeamount", c_rs.getString("chargeamount"));
						  	    	}
						  	    	
						  	    	result.put("success", true);
						  	    	
						  	    }
						  	    else
						  	    {
						  	    		if (paid)
						  	    		{
						  	    			IO.p("Emailing");
									    	  Email.sendCustomerInvoiceEmail(getServletContext(), name, email,
									    			  "Item: Delivery Request<br/>" + 
						    					  	  "Amount (AUD) : $25.00 <br/>" + 
						    					  	  "GST Included.<br/>" +
						    					  	  "Transaction ID: " + qpitransactionid + "<br/>"
									    			  );
							  	    			IO.p("Finished emailing");
						  	    		}
						  	    		else
						  	    		{
						  	    			IO.p("Didn't pay; didn't email");
						  	    		}
						  	    
						  	    		result.put("mustpay", false);
						  	    	
						  	    		Integer newgroupid = null;
							  	   
							  	    	// Generate a new groupid
							  	    	
							  	    	String findgroupid_sql = "SELECT max(itemgroupid) + 1 AS newitemgroup FROM itemgroups";
								  	    
								  	    PreparedStatement fg_stmt = conn.prepareStatement(findgroupid_sql);
								  	    i=1;
								  	    
								  	    ResultSet fg_rs = fg_stmt.executeQuery();
								  	    
								  	    Boolean newgroup_success = true;
								  	  
								  	    if(fg_rs.next())
								  	    {
								  	    	newgroupid = fg_rs.getInt("newitemgroup");
								  	    	
								  	    	// All are valid.
								  	    	for (int j=0; j < items.length(); j++)
									  	    {
								  	    		
										  	    //	JSONObject thisitem = items.getJSONObject(i);
											  	   
										  	    	Integer thisitemid = items.getInt(j);
										  	    	IO.p("Updating itemgroup for item" + thisitemid);
										  	    	
										  	    	String regroup_sql = "UPDATE itemgroups SET itemgroupid = ? WHERE itemid = ?";
									  	    	
										  	    	PreparedStatement rg_stmt = conn.prepareStatement(regroup_sql,Statement.RETURN_GENERATED_KEYS);
											      
										  	    	rg_stmt.setInt(1, newgroupid);
										  	    	rg_stmt.setInt(2, thisitemid);
											      
										  	    	int regroupsuccess = 2;
										  	    	regroupsuccess = rg_stmt.executeUpdate();
											      
										  	    	if (regroupsuccess == 0) 
										  	    	{
										  	    		result.put("success", false);
										  	    		result.put("error", "Could not move item into new group for return");
										  	    		Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
										  	    		newgroup_success = false;
										  	    		break;
										  	    	}
										  	    	else if(regroupsuccess == 1) 
										  	    	{
										  	    		String return_sql = "UPDATE items SET returning = 1 WHERE itemid = ?";
											  	    	
											  	    	PreparedStatement r_stmt = conn.prepareStatement(return_sql,Statement.RETURN_GENERATED_KEYS);
												      
											  	    	r_stmt.setInt(1, thisitemid);
												      
											  	    	int updatesuccess = 2;
											  	    	updatesuccess = r_stmt.executeUpdate();
												      
											  	    	if (updatesuccess == 0) 
											  	    	{
											  	    		result.put("success", false);
											  	    		result.put("error", "Could not mark item as returning");
											  	    		Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
											  	    		newgroup_success = false;
											  	    		break;
											  	    	}
											  	    	else if(updatesuccess == 1) 
											  	    	{
												    	  
											  	    	}
										  	    	}
									  	    }
								  	    }
						  	    	
								  	    IO.p("Checking newgroup success.");
								  	  if (newgroup_success)
								  	  {
								  		IO.p("Newgroup success!");
						  	    	 	  String insert_statement = "INSERT INTO deliveries (" +
									  			"ownerid, " +
									  			"itemgroupid, " +
									  	  		"deliveryaddress, " +
									  	  		"deliverytime, " +
									  	  		"cashcharged, " +
									  	  		"created " +
									  	  		") " +
									  	  		"VALUES " +
								  			  	"( ? , ? , ? , ? , ? , ? )";
									      
									      PreparedStatement i_stmt = conn.prepareStatement(insert_statement,Statement.RETURN_GENERATED_KEYS);
									      
									      i=1;
									      i_stmt.setInt(i++, userid);
									      i_stmt.setInt(i++, newgroupid);
									      i_stmt.setString(i++, inputJSON.getString("address"));
									      i_stmt.setTimestamp(i++, deliverytime);
								    	  i_stmt.setString(i++, "0.0");
									      i_stmt.setTimestamp(i++, timestamp);
									      
									      int insertsuccess = 2;
									      insertsuccess = i_stmt.executeUpdate();
									      
									      if (insertsuccess == 0) 
									      {
									    	  result.put("success", false);
									    	  result.put("error", "Could not create delivery");
									    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
									      }
									      else if(insertsuccess == 1) 
									      {
									    	  result.put("success", true);
									    	  
									    	  ResultSet insert_rs = i_stmt.getGeneratedKeys();
									    	  
									    	  insert_rs.next();
									    	  Integer newpickupid=insert_rs.getInt(1);
									    	  result.put("deliveryid", newpickupid);
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
