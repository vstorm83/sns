package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javapns.Push;
import javapns.notification.PayloadPerDevice;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.ResponsePacket;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONObject;

public class PushToDevices {
	
	
	public static boolean pushToAndroids(ServletContext servletContext, ArrayList<String> androiddevices,  final ArrayList<Integer> userids,  String userNotificationText, String type, String idtype, Integer id)
	{
		// Project ID: 128179036264
		
		// API Key for servers: AIzaSyAwEOBz28p1GTJes5ab_a74GPmPwxgpy2A
		// TODO GCM
		
		/*
		curl --header "Authorization: key=$api_key" --header Content-Type:"application/json" https://android.googleapis.com/gcm/send   -d "{\"registration_ids\":[\"APA91bFqBwGI9KQVBdVnWqqjAxSuwGN-myqpZaLUaecNdxiTxCecMAG6zIqwAJ9TcVxqfYq19SRCGO4WYBsjd7vrAOc4SWezPo8l_pyBVljUgn2LZLLbIp0MbOVdLUMAYa6IAHQ1vcoP5rO95pEVDOUCYIbEWvs-cXuZ4mfTQ0DqM1BFj82I2xQ\"],\"data\":{\"what\":\"winner\"}}"
		{"multicast_id":6883148048748576468,"success":1,"failure":0,"canonical_ids":0,"results":[{"message_id":"0:1386735489895700%9b899701f9fd7ecd"}]}
		*/
		try 
		{
			Class.forName("com.mysql.jdbc.Driver");

			Connection conn;
			conn = DriverManager
					.getConnection("jdbc:mysql://localhost/wp_snapnstore?"
							+ "user=wpsnapuser&password=wpsnappassword");
			
			
			IO.p("Beginning Android push loop. Pushing " + androiddevices.size() + " devices.");
			
			if (userids.size()!=androiddevices.size())
			{
				IO.p("PushToDevices: Array mismatch.");
				Email.sendAlertEmail(servletContext, "ERROR", "ERROR in PushToDevices. userids.length=" +userids.size() +  " is not equal to androiddevices.length=" + androiddevices.size());
			}
			else
			{
				int i=0;
				for (i=0; i< androiddevices.size(); i++)
				{
					IO.p("\tPushing device userid '" + userids.get(i) + "' at device '" + androiddevices.get(i) + "'");
					
					String chats_statement = "SELECT " +
							" participants.chatid," +
							" participants.lastseenmessageid," +
							" participants.amountowing, " +
							" participants.paid, " +
							" chats.recipientid, " +
							" chats.createdby, " +
							" chats.title, " +
							" chats.totalbill, " +
							" chats.closed " +
							" FROM participants" +
							" LEFT JOIN chats ON participants.chatid = chats.chatid" +
							" WHERE userid = ?";
						
			        		PreparedStatement chats_stmt = conn.prepareStatement(chats_statement);
			        		
			    	  	    chats_stmt.setInt(1, userids.get(i));
			    	  	    
			    	  	    ResultSet chats_rs = chats_stmt.executeQuery();
			    	  	    
			    	  	    Integer unreadtotal = 0;
			    	  	    
			    	  	    while(chats_rs.next())
			    	  	    {
			    	  	    	IO.p("\t\t\t Querying chat '" + chats_rs.getInt("chatid") + "'");
			    	  	    	// Figure out if we have any unread messages
			    	  	    	String messagecount_statement = "SELECT max(messageid) AS latestmessage, max(timestamp) as latestmessagetimestamp FROM messages WHERE chatid = ?";
			    				
			    		  	    PreparedStatement messagecount_stmt = conn.prepareStatement(messagecount_statement);
			    		  	    
			    		  	    messagecount_stmt.setInt(1, chats_rs.getInt("chatid"));
			    		  	    
			    		  	    ResultSet messagecount_rs = messagecount_stmt.executeQuery();
			    		  	    
			    		  	    Integer latestmessage = 0;
			    		  	    
			    		  	    if(messagecount_rs.next())
			    		  	    {	
			    		  	    	latestmessage = messagecount_rs.getInt("latestmessage");
			    		  	    }
			    		  	    
			    		  	    if (null!=latestmessage)
			    		  	    {
			    		  	    	if (latestmessage > chats_rs.getInt("lastseenmessageid"))
			    		  	    	{
			    		  	    		unreadtotal += (latestmessage - chats_rs.getInt("lastseenmessageid"));
			    		  	    		IO.p("\t\t\t\t and this chat has " + (latestmessage - chats_rs.getInt("lastseenmessageid")) +  " unread.");
			    		  	    	}
			    		  	    	else
			    		  	    	{
			    		  	    		//badge_array.add(0); 
			    		  	    	}
			    		  	    }
			    		  	    else
			    		  	    {
			    		  	    	//badge_array.add(0);  	 
			    		  	    }
			    		  	    
			    		  	  
			    	  	    } // End of each-chat while.
			    	  	    
			    	  	    if (0==unreadtotal)
			    	  	    {
			    	  	    	Email.sendAlertEmail(servletContext, "WARNING", "WARNING in Push: ZERO Badge sent. Calling user is:" +  userids.get(i));
			    	  	    }
			    	  	    
			    	  	    
			    	  	    
					
					String url = "https://android.googleapis.com/gcm/send";
					URL obj = new URL(url);
					HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
			 
					con.setRequestMethod("POST");
					con.setRequestProperty("User-Agent", "Tomcat");
					con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
					con.setRequestProperty("Authorization", "key=AIzaSyAwEOBz28p1GTJes5ab_a74GPmPwxgpy2A");
					con.setRequestProperty("Content-Type", "application/json");
					
					JSONObject post = new JSONObject(); 
					JSONArray regids = new JSONArray();
					
					JSONObject data = new JSONObject();
					data.put("notificationtext", userNotificationText);
					data.put("unread", unreadtotal);
					data.put("type", type);
					data.put("idtype", idtype);
					data.put("id", id);
					
					regids.put(androiddevices.get(i));
					
					post.put("registration_ids", regids);
					post.put("data", data);
					
					//con.set
					//String urlParameters = "{\"registration_ids\":[\"" + regid + "\"],\"data\":{\"what\":\"ZAKisanegroid\"}}";
			 
					// Send post request
					con.setDoOutput(true);
					DataOutputStream wr = new DataOutputStream(con.getOutputStream());
					wr.writeBytes(post.toString());
					wr.flush();
					wr.close();
			 
					int responseCode = con.getResponseCode();
					System.out.println("\tSending 'POST' request to URL : " + url);
					//System.out.println("Post parameters : " + urlParameters);
					System.out.println("\tResponse Code : " + responseCode);
			 
					BufferedReader in = new BufferedReader(
					        new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();
			 
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
			 
					//print result
					System.out.println("\tPush response: " + response.toString());
				}
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return true;
	}

	public static void pushToiPhones(final ServletContext servletContext, final ArrayList<String> iphonedevices,  final ArrayList<Integer> userids, final String userNotificationText, final String type, final String idtype, final Integer id)
	{
		
			Thread gogogo = new Thread() {
			    public void run() {
			    	System.out.println("Starting push thread");
			    	
			    	int j=0;
			    	
					try
					{
						Class.forName("com.mysql.jdbc.Driver");

						Connection conn;
						conn = DriverManager
								.getConnection("jdbc:mysql://localhost/wp_snapnstore?"
										+ "user=wpsnapuser&password=wpsnappassword");
						
					if (iphonedevices.size() != userids.size())
					{
						// Panic and freak out.
						IO.p("Push: iphonedevices and userids arrays don't match in size.");
					}
					else
					{
								List<PayloadPerDevice> payloadDevicePairs = new Vector<PayloadPerDevice>();
						
								String chats_statement = "SELECT " +
									" participants.chatid," +
									" participants.lastseenmessageid," +
									" participants.amountowing, " +
									" participants.paid, " +
									" chats.recipientid, " +
									" chats.createdby, " +
									" chats.title, " +
									" chats.totalbill, " +
									" chats.closed " +
									" FROM participants" +
									" LEFT JOIN chats ON participants.chatid = chats.chatid" +
									" WHERE userid = ?";

								IO.p("\t iphonedevices array:" + iphonedevices + "");
								
						    	for (j=0;j<iphonedevices.size();j++)
						    	{
								
						    		IO.p("\t\t iphonedevices[" + j + "] = " + iphonedevices.get(j));
						    		
					        		PreparedStatement chats_stmt = conn.prepareStatement(chats_statement);
					        		
					    	  	    int i=1;
					    	  	    
					    	  	    chats_stmt.setInt(i++, userids.get(j));
					    	  	    
					    	  	    ResultSet chats_rs = chats_stmt.executeQuery();
					    	  	    
					    	  	    Integer unreadtotal = 0;
					    	  	    
					    	  	    int chatcount = 0;
					    	  	    
					    	  	    IO.p("\t\tQuerying for chats.");
					    	  	    
					    	  	    while(chats_rs.next())
					    	  	    {
					    	  	    	IO.p("\t\t\t Querying chat '" + chats_rs.getInt("chatid") + "' and lastseenmessage id is " + chats_rs.getInt("lastseenmessageid"));
					    	  	    	chatcount++;
					    	  	    	// Figure out if we have any unread messages
					    	  	    	String messagecount_statement = "SELECT max(messageid) AS latestmessage, max(timestamp) as latestmessagetimestamp FROM messages WHERE chatid = ?";
					    				
					    		  	    PreparedStatement messagecount_stmt = conn.prepareStatement(messagecount_statement);
					    		  	    i=1;
					    		  	    
					    		  	    messagecount_stmt.setInt(i++, chats_rs.getInt("chatid"));
					    		  	    
					    		  	    ResultSet messagecount_rs = messagecount_stmt.executeQuery();
					    		  	    
					    		  	    Integer latestmessage = 0;
					    		  	    
					    		  	    if(messagecount_rs.next())
					    		  	    {	
					    		  	    	latestmessage = messagecount_rs.getInt("latestmessage");
					    		  	    	IO.p("\t\t\t The largest messageid in chat " + chats_rs.getInt("chatid") + " is " + latestmessage);
					    		  	    }
					    		  	    
					    		  	    if (null!=latestmessage)
					    		  	    {
					    		  	    	if (latestmessage > chats_rs.getInt("lastseenmessageid"))
					    		  	    	{
					    		  	    		unreadtotal = unreadtotal + (latestmessage - chats_rs.getInt("lastseenmessageid"));
					    		  	    		IO.p("\t\t\t and this chat has " + (latestmessage - chats_rs.getInt("lastseenmessageid")) +  " unread.");
					    		  	    	}
					    		  	    	else
					    		  	    	{
					    		  	    		IO.p("\t\t\t and this chat was up to date.");
					    		  	    		//badge_array.add(0); 
					    		  	    	}
					    		  	    }
					    		  	    else
					    		  	    {
					    		  	    	IO.p("\t\t\t latestmessage was null.");
					    		  	    	//badge_array.add(0);  	 
					    		  	    }
					    		  	    
					    		  	  IO.p("\t\t\t End of Querying chat '" + chats_rs.getInt("chatid") + "'");
					    		  	  
					    	  	    } // End of each-chat while.
					    	  	    
					    	  	    IO.p("\t\t End of querying chats.");
					    	  	    
					    	  	    if (unreadtotal.equals(0))
					    	  	    {
					    	  	    	IO.p("\t\t unreadtotal was ZERO for userid " + userids.get(j));
					    	  	    	Email.sendAlertEmail(servletContext, "WARNING", "WARNING in Push: ZERO Badge sent counting " + chatcount + " chats and calling user is:" +  userids.get(j));
					    	  	    }

						    		//Build a blank payload to customize
						            PushNotificationPayload payload = PushNotificationPayload.complex();

						            // Customize the payload
						            payload.addAlert(userNotificationText);
						            payload.addCustomDictionary("type", type);
						            payload.addCustomDictionary(idtype, id);
						            
						            //if (0==unreadtotal)
						            //{
						            	payload.addBadge(unreadtotal);
						            //}
						            
						    		payloadDevicePairs.add(new PayloadPerDevice(payload, iphonedevices.get(j)));
						    		IO.p("\t\t Built notification with payload with badge=" + unreadtotal);
						    	}
						    	
						    	IO.p("\tFinally, payloadDevicePairs is =" + payloadDevicePairs);
						    	
						    	System.out.println("\tFinished chat query, starting push");
						    	
						    	URL cert = servletContext.getResource("/WEB-INF/ProductionAppServerTwo.p12");
					    		
					    		InputStream cert_is = cert.openStream();
					    		
						    	Boolean development=false;
						    	Boolean production=true;
						    	List<PushedNotification> notifications  = Push.payloads(cert_is, "quicklypay.it", production, payloadDevicePairs);
				            
				            	IO.p("Number of notifications sent : " + notifications.size());
				            
					    		for (PushedNotification notification : notifications) 
					    		{
					                 if (notification.isSuccessful()) 
					                 {
					                         /* Apple accepted the notification and should deliver it */  
					                         System.out.println("Push notification sent successfully to: " + 
					                                                         notification.getDevice().getToken());
					                         /* Still need to query the Feedback Service regularly */  
					                         //return true;
					                 } 
					                 else 
					                 {
					                     System.out.println("Push notification FAILED. Token was: " + 
					                             notification.getDevice().getToken());
					                         //String invalidToken = notification.getDevice().getToken();
					                         /* Add code here to remove invalidToken from your database */  
		
					                         /* Find out more about what the problem was */  
					                         Exception theProblem = notification.getException();
					                         theProblem.printStackTrace();
		
					                         /* If the problem was an error-response packet returned by Apple, get it */  
					                         ResponsePacket theErrorResponse = notification.getResponse();
					                         if (theErrorResponse != null) {
					                                 System.out.println(theErrorResponse.getMessage());
					                         }
					                         
					                 
					                 }
					    		}
							}
				        } 
						catch (SQLException e1) 
						{
							e1.printStackTrace();
						} 
						catch (ClassNotFoundException e1) 
						{
							e1.printStackTrace();
						}
						catch(Exception e) 
						{
							e.printStackTrace();
						}
					
					System.out.println("Finished push thread");
					
			        }
			        
			};

			gogogo.start();

		return;
	}

	
}
