package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.mysql.jdbc.Statement;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    public RegisterServlet() {
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
			
				if (!(inputJSON.has("email") && inputJSON.has("phonenumber") && inputJSON.has("passwordhash") && inputJSON.has("name")))
				{
					result.put("success", false);
					result.put("error", "Missing mandatory parameters");
					Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				}
				else
				{
					
				String statement = null;

				Class.forName("com.mysql.jdbc.Driver");

				conn = DriverManager
						.getConnection("jdbc:mysql://localhost/wp_snapnstore?"
								+ "user=wpsnapuser&password=wpsnappassword");
				
				String generatedsessionid=null;
				
	      		String rawuserphonenumber = inputJSON.getString("phonenumber");
	      		String userphonenumber = rawuserphonenumber.replaceAll("[^\\d]","");
	      		if (userphonenumber.startsWith("61"))
	      		{
	      			userphonenumber = "0" + userphonenumber.substring(2, userphonenumber.length());
	      		}
	      		
	      		String name = inputJSON.getString("name");
	      		String email = inputJSON.getString("email");
		
				// Check if the user currently exists in the database
			
	    	  	String sessionidstatement = "SELECT " +
		  	  		"sessionid, " +
		  	  		"userid " +
		  	    		" FROM appusers WHERE email = ?";
		  	    
		  	    PreparedStatement sessionidstmt = conn.prepareStatement(sessionidstatement);
		  	    int i=1;
		  	    
		  	    sessionidstmt.setString(i++, email);
		  	    
		  	    ResultSet rs = sessionidstmt.executeQuery();
		  	    
		  	    Integer userscount=0;
		  	    while(rs.next())
		  	    {
		  	    	userscount++;
		  	    }
		  	    
		  	    rs.beforeFirst();
		  	    rs.next();
		  	    
		  	    Integer finaluserid=null;
		  	    
			    Date date = new Date();
				Timestamp timestamp = new Timestamp(date.getTime());
		  	    
		  	    if (userscount.equals(1))
		  	    {
					// User's phone number already exists.
		  	    	
		  	    	finaluserid = rs.getInt("userid");
		  	    	
		  	    	result.put("success", false);
			    	result.put("usererror", "This email is already registered.");
				   
		  	    }
		  	    else if (userscount.equals(0))
		  	    {
		  	    	// No current user exists, create them.
		  	    	// This user is registering for the first time
		  	    	
			    	 statement = "INSERT INTO appusers " +
			    			 		"( " +
			    			 		" sessionid, " +
			    			 		" email, " +
			    			 		" usertype, " +
			    			 		" phonenumber, " +
			    			 		" name, " +
			    			 		" businessname, " +
			    			 		(inputJSON.has("iphonepushtoken") ? " iphonepushtoken, " : "") +
			    			 		(inputJSON.has("androidpushtoken") ? " androidpushtoken, " : "") +
			    			 		" passwordhash," +
			    			 		" created ) " +
						      		"VALUES " + 
						      		"( ? ," +
						      		" ? ," +
						      		" ? ," +
						      		" ? ," +
						      		" ? ," +
						      		" ? ," +
						      		(inputJSON.has("iphonepushtoken") ? "? ," : "") +
						      		(inputJSON.has("androidpushtoken") ? "? ," : "") +
						      		" ? ," +
						      		" ? )";
			      
			      PreparedStatement stmt = conn.prepareStatement(statement,Statement.RETURN_GENERATED_KEYS);
			      
			      i=1;
			      
			      generatedsessionid = UUID.randomUUID().toString();
			      stmt.setString(i++, generatedsessionid);
			      
			      stmt.setString(i++, inputJSON.getString("email").toLowerCase());
			      stmt.setString(i++, "customer");
			      stmt.setString(i++, userphonenumber);
			      stmt.setString(i++, inputJSON.getString("name"));
			      String business = null;
			      if (inputJSON.has("businessName")) {
			    	  business = inputJSON.getString("businessName");
			      }
			      stmt.setString(i++, business);
			      
			      if (inputJSON.has("iphonepushtoken"))
			      {
			    	  stmt.setString(i++, inputJSON.getString("iphonepushtoken"));
			      }
			      
			      if (inputJSON.has("androidpushtoken"))
			      {
			    	  stmt.setString(i++, inputJSON.getString("androidpushtoken"));
			      }
			      
			      stmt.setString(i++, inputJSON.getString("passwordhash").toLowerCase());
			      
			      stmt.setTimestamp(i++, timestamp);
			      
			      int success = 2;
			      success = stmt.executeUpdate();
			      
			      if (success == 0) 
			      {
			    	  result.put("success", false);
			    	  result.put("error", "No user created.");
			    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
			      }
			      else if(success == 1) 
			      {
				    	  ResultSet insert_rs = stmt.getGeneratedKeys();
				    	  
				    	  insert_rs.next();
				    	  finaluserid=insert_rs.getInt(1);
				    	  
				    	  result.put("sessionid", generatedsessionid);
			    	  
		    	  	  // Check if the user's email exists in Wordpress
		    	  		String email_sql = "SELECT ID FROM wp_users WHERE user_email = ?";
				  	    
				  	    PreparedStatement e_stmt = conn.prepareStatement(email_sql);
				  	    i=1;
				  	    
				  	    e_stmt.setString(i++, email);
				  	    
				  	    ResultSet e_rs = e_stmt.executeQuery();
				  	    
				  	    Integer wpuserid = null;
				  	    
				  	    
				  	    Boolean updatetocourier = false;
				  	  
				  	  
				  	    Integer emailcount=0;
				  	    while(e_rs.next())
				  	    {
				  	    	emailcount++;
				  	    }
				  	    
				  	    if (emailcount > 1)
				  	    {
				  	    		result.put("success", false);
				  	    		result.put("error", "Email already exists more than once.");
				  	    		Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
				  	    }
				  	    else if ( 1 == emailcount )
				  	    {
					  	    	e_rs.beforeFirst();
					  	    	e_rs.next();
					  	    	wpuserid = e_rs.getInt("ID");
					  	    	
					  	    	//
					    	  	// Check if the user's email exists in Wordpress
				    	  		String courier_sql = "SELECT meta_value FROM wp_usermeta WHERE meta_key = \"wp_capabilities\" AND user_id = ?";
						  	    
						  	    PreparedStatement c_stmt = conn.prepareStatement(courier_sql);
						  	    i=1;
						  	    
						  	    c_stmt.setInt(i++, wpuserid);
						  	    
						  	    ResultSet c_rs = c_stmt.executeQuery();
						  	    
						  	    while(c_rs.next())
						  	    {
						  	    	String tmpstring = c_rs.getString("meta_value");
						  	    	if (tmpstring.toLowerCase().contains("courier"))
						  	    	{
						  	    		// Update them to be a courier, later.
						  	    		updatetocourier = true;
						  	    		result.put("usertype","courier");
						  	    	}
						  	    }
					  	    	
				  	    }
				  	    else if ( 0 == emailcount )
				  	    {
				  	    	/*
				  	    		// Should register them.
				  	    		updatetocourier = false;
			    	  
				  	    		HttpURLConnection con = null;
								BufferedReader in = null;
								
								
								HttpURLConnection r_con = null;
								BufferedReader r_in = null;
								
								
								try
								{
									
									// Get a nonce from wordpress
										
				  			    	String url = "http://snapnstore.imagineteamsolutions.com/www.snapnstore.com.au/htdocs/?json=get_nonce&controller=user&method=register";
					  				URL obj = new URL(url);
					  				//con =  (HttpsURLConnection) obj.openConnection();
					  				 con = (HttpURLConnection) obj.openConnection();
					  		 
					  				con.setRequestMethod("GET");
					  				con.setRequestProperty("User-Agent", "Tomcat");
					  				con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
					  				con.setRequestProperty("Content-Type", "application/json");
					  				
					  				//con.setRequestProperty("Authorization", "Basic " + encoding);
					  				
					  				// Send post request
					  				con.setDoOutput(true);
					  		 
					  				int responseCode = con.getResponseCode();
					  				System.out.println("\tSending 'GET' request to URL : " + url);
					  				System.out.println("\tResponse Code : " + responseCode);
					  		 
					  				in = new BufferedReader(
					  				        new InputStreamReader(con.getInputStream()));
					  				String inputLine;
					  				StringBuffer getresponse = new StringBuffer();
					  		 
					  				while ((inputLine = in.readLine()) != null) {
					  					getresponse.append(inputLine);
					  				}
					  				in.close();
					  		 
					  				IO.p("Response was:\n\t" + getresponse.toString());
					  				
					  				JSONObject resultjo = new JSONObject(getresponse.toString());
					  				
									if(!resultjo.has("nonce"))
									{
										Email.sendAlertEmail(getServletContext(), "PARSE ERROR", "Missing nonce in: '" + getresponse.toString() + "'.");
									}
									else
									{
										String nonce =  resultjo.getString("nonce");
										
										String username = name.replace("[^\\w]", "");
										
										String generatedpassword = UUID.randomUUID().toString().replace("[^\\w]", "");
										
										wpuserid = 9001;
										
										// Call server to register them as a wordpress user
										
										String register_url = "http://snapnstore.imagineteamsolutions.com/www.snapnstore.com.au/htdocs/?json=user/register&username=" + username + 
												"&nonce=" + nonce + "&display_name=" + username + "&email=" + email + "&password=" + generatedpassword;
										
										
										
										IO.p("Register URL is : " + register_url);
										
						  				URL r_obj = new URL(register_url);
						  				r_con = (HttpURLConnection) r_obj.openConnection();
						  		 
						  				r_con.setRequestMethod("GET");
						  				r_con.setRequestProperty("User-Agent", "Tomcat");
						  				r_con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
						  				r_con.setRequestProperty("Content-Type", "application/json");
						  				
						  				// Send post request
						  				r_con.setDoOutput(true);
						  		 
						  				int r_responseCode = r_con.getResponseCode();
						  				System.out.println("\tSending 'GET' request to URL : " + r_obj.toString());
						  				System.out.println("\tResponse Code : " + r_responseCode);
						  		 
						  				r_in = new BufferedReader(
						  				        new InputStreamReader(r_con.getInputStream()));
						  				inputLine = "";
						  				StringBuffer r_getresponse = new StringBuffer();
						  		 
						  				while ((inputLine = r_in.readLine()) != null) {
						  					r_getresponse.append(inputLine);
						  				}
						  				r_in.close();
						  		 
						  				IO.p("Response was:\n\t" + r_getresponse.toString());
						  				
						  				JSONObject r_resultjo = new JSONObject(r_getresponse.toString());
						  				
										if( r_resultjo.has("user_id") && (null!=r_resultjo.get("user_id") ))
										{
											wpuserid = r_resultjo.getInt("user_id");
										}
										else
										{
											Email.sendAlertEmail(getServletContext(), "PARSE ERROR", "Missing new wp_userid in: '" + r_getresponse.toString() + "'.");
										}
									}
				  	    	 	
									
								}
								catch (Exception e)
								{
									result.put("success", false);
									result.put("error", "Creating nonce/user failed");
									e.printStackTrace();
								}
								finally
								{
									
									if ( con != null )
									{
										con.disconnect();
									}
									
									if ( in != null )
									{
										try 
										{ 
											in.close(); 
										} 
										catch (Exception ex) 
										{
											ex.printStackTrace();
										}
									}
									
									if ( r_con != null )
									{
										r_con.disconnect();
									}
									
									if ( r_in != null )
									{
										try 
										{ 
											r_in.close(); 
										} 
										catch (Exception ex) 
										{
											ex.printStackTrace();
										}
									}
									
								}
								  	  */  
			  	    } // End of no-existing user
				  	
			  	    String usertypestring = (updatetocourier ? "courier" : "customer");
			  	    
			  	    result.put("usertype", usertypestring);

			  	    if (!updatetocourier)
			  	    {
			  	    	// No wordpress user as of yet.
			  	    	result.put("success", true);
			  	    }
			  	    else
			  	    {
				  	      if (null==wpuserid)
					      {
				  	    	  // TODO Error
					      }
				  	      else
				  	      {
							  String tokenstatement = "UPDATE appusers SET wpuserid = ?, usertype = ? WHERE userid = ?";
						      
						      PreparedStatement tokenstmt = conn.prepareStatement(tokenstatement,Statement.RETURN_GENERATED_KEYS);
						      
						      i=1;
						      
						      tokenstmt.setInt(i++, wpuserid);
						      tokenstmt.setString(i++, usertypestring );
						      
						      tokenstmt.setInt(i++, finaluserid);
						      
						      int tokensuccess = 2;
						      tokensuccess = tokenstmt.executeUpdate();
						      
						      if (tokensuccess == 0) 
						      {
						    	  result.put("success", false);
						    	  result.put("error", "Could not store wpuserid.");
						      }
						      else if(tokensuccess == 1) 
						      {
						    	  result.put("success", true);
						      }
					      }
			  	    }
		      }
		      else
		      {
		    	  result.put("success", false);
		    	  result.put("error", "More than one user found.");
		    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
		      }
		  	 }	      
			} 
					
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
