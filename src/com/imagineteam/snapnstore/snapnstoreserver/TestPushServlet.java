package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.mysql.jdbc.Statement;

@WebServlet("/testPush")
public class TestPushServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public TestPushServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			
				if (!(inputJSON.has("sessionid") && inputJSON.has("recipientuserid")  && inputJSON.has("notificationtext") ))
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
					    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				  	    }
				  	    else if (userscount.equals(1))
				  	    {
				  	    	rs.beforeFirst();
				  	    	rs.next();
				  	    	
				  	    	Integer userid = rs.getInt("userid");
				  	    	
				  	    	if (!(userid.equals(113)))
				  	    	{
								result.put("success", false);
								result.put("error", "Only QPI Team can call test push.");
						    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				  	    	}
				  	    	else
				  	    	{
				  	    		
																    	  // Find all the recipients of the message.
																    	  String recipient_statement = "SELECT " +
																  	    		  	" name," +
																  	    		  	" userid, " +
																		  	  		" androidpushtoken, " +
																		  	  		" iphonepushtoken " +
																	  	    		" FROM users" +
																	  	    		" WHERE userid = ? ";
																			
																	  	    PreparedStatement r_stmt = conn.prepareStatement(recipient_statement);
																	  	    i=1;
																	  	     
																	  	    r_stmt.setInt(i++, inputJSON.getInt("recipientuserid"));
																	  	    
																	  	    ResultSet recipient_rs = r_stmt.executeQuery();
																	  	    
																	  	    ArrayList<String> androiddevices = new ArrayList<String>();
																	  	    ArrayList<String> iphonedevices = new ArrayList<String>();
																	  	
																  	    	ArrayList<Integer> androiduids = new ArrayList<Integer>();
																  	    	ArrayList<Integer> iphoneuids = new ArrayList<Integer>();
																  	    	
																	  	    while(recipient_rs.next())
																	  	    {
																		  	    	if (recipient_rs.getString("androidpushtoken") !=null)
																		  	    	{
																		  	    		androiddevices.add(recipient_rs.getString("androidpushtoken"));
																		  	    		androiduids.add(recipient_rs.getInt("userid"));
																		  	    		IO.p ("Pushing Android: " + recipient_rs.getString("androidpushtoken"));
																		  	    	}
																		  	    	
																		  	    	if (recipient_rs.getString("iphonepushtoken") !=null)
																		  	    	{
																		  	    		iphonedevices.add(recipient_rs.getString("iphonepushtoken"));
																		  	    		iphoneuids.add(recipient_rs.getInt("userid"));
																		  	    		IO.p ("Pushing iPhone: " + recipient_rs.getString("iphonepushtoken"));
																		  	    	}
																	  	    }
																	  	    
																	  	    // Push notify Android devices.
																	  	    PushToDevices.pushToAndroids(getServletContext(), androiddevices,androiduids, inputJSON.getString("notificationtext") ,"invitepromotion", "blank", 1);
																	  	    
																	  	    // Push notify iPhone devices
																	  	   	PushToDevices.pushToiPhones(getServletContext(), iphonedevices,iphoneuids,  inputJSON.getString("notificationtext")  ,"invitepromotion", "blank", 1);
																    	  
																	  	   	result.put("success", true);
				  	    	}
				  	    }
				}
			}

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

	
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Email.sendAlertEmail(getServletContext(), "WARNING", "WARNING in " + this.getServletName() + " doPost was called.");
	}

}
