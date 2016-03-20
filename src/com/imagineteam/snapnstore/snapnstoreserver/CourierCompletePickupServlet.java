package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.json.JSONObject;

@WebServlet("/cCompletePickup")
@MultipartConfig
public class CourierCompletePickupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public CourierCompletePickupServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		IO.alertDodgyGetRequest(getServletName(), getServletContext(), request);
	}

    private static final int THRESHOLD_SIZE     = 1024 * 1024 * 3;  // 3MB
    private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
    private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB
 
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
		
		try {
				  	    	
						  	   // checks if the request actually contains upload file
						  	        if (!ServletFileUpload.isMultipartContent(request)) {
						  	        	/*
						  	            PrintWriter writer = response.getWriter();
						  	            writer.println("Request does not contain upload data");
						  	            writer.flush();*/
						  	        	result.put("error", "You'll need to specify encoding type as multipart.");
						  	        	result.put("success", false);
						  	            return;
						  	        }
						  	        else
						  	        {
						  	        	IO.p("Is multipart.");
						  	        }
						  	         
						  	        String upload_directory = "catchall"; 
						  	        
						  	      Class.forName("com.mysql.jdbc.Driver");

									conn = DriverManager
											.getConnection("jdbc:mysql://localhost/wp_snapnstore?"
													+ "user=wpsnapuser&password=wpsnappassword");
						  	    		  
						  	        String findgroupid_sql = "SELECT max(itemgroupid) + 1 AS newitemgroup FROM itemgroups";
							  	    
							  	    PreparedStatement fg_stmt = conn.prepareStatement(findgroupid_sql);
							  	    int i=1;
							  	    
							  	    Integer newitemgroupid = null;
							  	    ResultSet fg_rs = fg_stmt.executeQuery();
							  	    
							  	    if(fg_rs.next())
							  	    {
							  	    	newitemgroupid = fg_rs.getInt("newitemgroup");
							  	    	upload_directory = "itemgroup_" + newitemgroupid.toString();
							  	    }
							  	    
						  	        // constructs the directory path to store upload file
						  	        String uploadPath = getServletContext().getRealPath("")
						  	            + File.separator + upload_directory;
						  	        // creates the directory if it does not exist
						  	        File uploadDir = new File(uploadPath);
						  	        if (!uploadDir.exists()) 
						  	        {
						  	            uploadDir.mkdir();
						  	            IO.p("Created directory.");
						  	        }
						  	        else
						  	        {
						  	        	IO.p("Directory existed, didn't create it.");
						  	        }
						  	         
						  	        IO.p("About to hit parts.");
						  	        //request.get
						  	        
					  	            String sessionid = null;
					  	            Integer pickupid = null;
					  	            Boolean zipfilefound = false;
					  	            String zipfilename = "pickup_" + System.currentTimeMillis() + ".zip";
					  	        
						          for (Part part : request.getParts()) 
						          {
						        	  IO.p("\tHit part!");
						        	  
						              String name = part.getName();
						              
						              IO.p("\t\tname=" + name);
						              
						              if (name.equals("sessionid"))
						              {
						            	  InputStream is = request.getPart(name).getInputStream();
						            	  
						            	  sessionid = getStringFromInputStream(is);
						              }
						              /*
						              if (name.equals("storagelocation"))
						              {
						            	  InputStream is = request.getPart(name).getInputStream();
						            	  
						            	  storagelocation = getStringFromInputStream(is);
						              }
						              */
						              
						              if (name.equals("pickupid"))
						              {
						            	  InputStream is = request.getPart(name).getInputStream();
						            	  
						            	  pickupid = Integer.parseInt(getStringFromInputStream(is));
						              }
						              
						              if (name.equals("imagezip"))
						              {
						              
							              String contentType = part.getContentType();
							              if (null!= contentType)
							              {
							            	  
								              if(!contentType.equals("application/octet-stream")) {
								                  //IO.p("Only ZIP files are supported!");
								                  //result.put("fileerror","Only ZIP files are supported, you gave '" + contentType + "'");
								                  //break; // TODO Uncomment
								              }
							              }
							              
							              InputStream is = request.getPart(name).getInputStream();
							             // File uploadDir = new File("C:\\path\\to\\dir");
							              //File file = File.createTempFile("img", ".png", uploadDir);
							              
							              File file = new File(uploadDir, zipfilename);
							              FileOutputStream fos = new FileOutputStream(file);

							              /*
							              int data = 0;
							              while ((data = is.read()) != -1) {
							                  fos.write(data);
							              }
							              */
							              
							              IO.p("!Reading first bytes");
							              int read = 0;
							      		  byte[] bytes = new byte[1024];
							       
							      			while ((read = is.read(bytes)) != -1) 
							      			{
							      				fos.write(bytes, 0, read);
							      			}
							      			IO.p("!Finished Reading first bytes");
							      		
	
							              fos.close();
							              IO.p("File uploaded.");
							              zipfilefound = true;
						              }
						              
						          } // End of for-loop
						          
						          if ( null!=sessionid && null!=pickupid && null!=zipfilefound && zipfilefound )
						          {
												// Check if sessionid is valid
										
									    	  	String sessionidstatement = "SELECT " +
										  	  		"sessionid, " +
										  	  		"userid, " +
										  	  		"name, " +
										  	  		"email " + 
										  	    		" FROM appusers WHERE sessionid = ?";
										  	    
										  	    PreparedStatement sessionidstmt = conn.prepareStatement(sessionidstatement);
										  	    i=1;
										  	    
										  	    sessionidstmt.setString(i++, sessionid);
										  	    
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
												  	  
										  	    	  // Check if the pickup is already closed.
										  	    	  
										  	    	  String checkcompleted_sql = "SELECT " +
												  	  		"completed, " +
												  	  		"ownerid, " +
												  	  		"completedby " +
												  	    		" FROM pickups WHERE pickupid = ?";
												  	    
												  	    PreparedStatement cc_stmt = conn.prepareStatement(checkcompleted_sql);
												  	    i=1;
												  	    
												  	    cc_stmt.setInt(i++, pickupid);
												  	    
												  	    ResultSet cc_rs = cc_stmt.executeQuery();
												  	    
												  	    Boolean alreadyclosed = false;
												  	    
												  	    Integer ownerid = null;
												  	    
												  	    if(!cc_rs.next())
												  	    {
												  	    	result.put("success", false);
												  	    	result.put("error", "Couldn't find pickupid " + pickupid);
												  	    	result.put("usererror", "Couldn't find the pickup to be completed: pickupid_" + pickupid);
												  	    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
												  	    }
												  	    else
												  	    {
												  	    	cc_rs.beforeFirst();
												  	    	cc_rs.next();
												  	    	
												  	    	ownerid = cc_rs.getInt("ownerid");
												  	    	
												  	    	if (1==cc_rs.getInt("completed"))
												  	    	{
												  	    		alreadyclosed = true;

												  	    		result.put("success",false);
												  	    		
													  	    	if (cc_rs.getInt("completedby") == userid)
													  	    	{
													  	    		result.put("usererror", "You already completed this job.");
													  	    	}
													  	    	else
													  	    	{
													  	    		result.put("usererror", "This job was already completed by another courier.");
													  	    	}
												  	    	}
												  	    	
												  	    }
												  	    
										  	    	  
												  	    if (!alreadyclosed)
												  	    {
												  	    	  
												  	    	  // Check if there are any storage spaces left.
												  	    	String findspace_sql = "SELECT " +
														  	  		"locationid, " +
														  	  		"description " +
														  	    		" FROM storagelocations WHERE available = 1 LIMIT 1";
														  	    
														  	    PreparedStatement fs_stmt = conn.prepareStatement(findspace_sql);
														  	    i=1;
														  	    
														  	    ResultSet fs_rs = fs_stmt.executeQuery();
														  	    
														  	    if(!fs_rs.next())
														  	    {
														  	    	result.put("success", false);
														  	    	result.put("error", "Couldn't find free storage");
														  	    	result.put("usererror", "No storage is available, please contact support.");
														  	    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
														  	    }
														  	    else
														  	    {
														  	    	
														  	    	  Integer locationid = fs_rs.getInt("locationid");
														  	    	  String locationdescription = fs_rs.getString("description");
														  	    	  
														  	    	  result.put("storagelocation", locationdescription);
														  	    	  
														  	    	  // Now go and grab hold of one storage space
														  	    	  String grab_location_statement = "UPDATE storagelocations SET " + 
																  	  	    " available = 0 " +
															    	 		" WHERE available = 1 AND locationid = ?" + 
															    	 		" LIMIT 1";
																      
																      PreparedStatement gl_stmt = conn.prepareStatement(grab_location_statement,Statement.RETURN_GENERATED_KEYS);
																      
																      i=1;

															  	      gl_stmt.setInt(i++, locationid);
																  	    
																      int grab_success = 2;
																      grab_success = gl_stmt.executeUpdate();
																      
																      if (grab_success == 0)
																      {
																    	  result.put("success", false);
																    	  result.put("usererror", "Could not access list of storage locations. Please contact support.");
																    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("usererror"));
																      }
																      else if(grab_success == 1)
																      {

																	  	  String update_statement = "UPDATE pickups SET " + 
																	  	  	    "completedby = ? , " +
																	  	  	    "completed = 1 " +
																    	 		" WHERE pickupid = ?";
																	      
																	      PreparedStatement u_stmt = conn.prepareStatement(update_statement,Statement.RETURN_GENERATED_KEYS);
																	      
																	      i=1;
																	      
																	      u_stmt.setInt(i++, userid);
																	      u_stmt.setInt(i++, pickupid);
																	      
																	      int updatesuccess = 2;
																	      updatesuccess = u_stmt.executeUpdate();
																	      
																	      if (updatesuccess == 0) 
																	      {
																	    	  result.put("success", false);
																	    	  result.put("usererror", "Could not update pickup as completed.");
																	    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("usererror"));
																	      }
																	      else if(updatesuccess == 1) 
																	      {
																    	  	  // Unzip the file and go through the files.
																	    	  
																	    	  byte[] buffer = new byte[1024];
																	    	  
																	    	     try{
																	    	    	 String outputfolder = "/var/www/www.imagineteamsolutions.com/htdocs/www.snapnstore.com.au/htdocs/uploadedimages/groupid_" + newitemgroupid;
																			  	        
																	    	    	//create output directory is not exists
																	    	    	File folder = new File(outputfolder);
																	    	    	if(!folder.exists()){
																	    	    		folder.mkdir();
																	    	    		IO.p("Creating output folder:" + outputfolder);
																	    	    	}
																	    	    	else
																	    	    	{
																	    	    		IO.p("Output folder already exists:" + outputfolder);
																	    	    	}
																	    	 
																	    	    	IO.p("Now opening zipfile at:" + uploadDir + File.separator + zipfilename);
																	    	    	
																	    	    	//get the zip file content
																	    	    	ZipInputStream zis = new ZipInputStream(new FileInputStream(uploadDir + File.separator + zipfilename)); 
																	    	    	//get the zipped file list entry
																	    	    	ZipEntry ze = zis.getNextEntry();
																	    	 
																	    	    	IO.p("Starting zip loop:");
																	    	    	while(ze!=null){
																	    	 
																	    	    	   String filename = ze.getName();
																	    	    	   
																	    	    	   if (filename.toLowerCase().endsWith(".png") || filename.toLowerCase().endsWith(".jpeg") || filename.toLowerCase().endsWith(".jpg"))
																	    	    	   {
																	    	    		   String nopathfilename = System.currentTimeMillis() + "_" + filename;
																		    	           File newFile = new File(outputfolder + File.separator + nopathfilename);
																		    	 
																		    	           System.out.println("file unzip : "+ newFile.getAbsoluteFile());
																		    	 
																		    	            //create all non exists folders
																		    	            //else you will hit FileNotFoundException for compressed folder
																		    	            new File(newFile.getParent()).mkdirs();
																		    	 
																		    	            FileOutputStream fos = new FileOutputStream(newFile);             
																		    	            
																		    	            IO.p("\t#Unzipping file by byte");
																		    	            int len;
																		    	            while ((len = zis.read(buffer)) > 0) {
																		    	       		fos.write(buffer, 0, len);
																		    	            }
																		    	            IO.p("\t#Finished unzipping file by byte");
																		    	            
																		    	            // Insert item

																							  java.util.Date nowdate= new java.util.Date();
																							  Timestamp timestamp = new Timestamp(nowdate.getTime());
																								
																					    	  String insert_sql = "INSERT INTO items ("
																					    	  		+ "ownerid, "
																					    	  		+ "location, "
																					    	  		+ "imageurl, "
																					    	  		+ "created"
																					    	  		+ ") VALUES " + 
																							  	  	    "( "
																							  	  	    + " ? ,"
																							  	  	    + " ? , "
																							  	  	    + " ? , "
																							  	  	    + " ? "
																							  	  	    + ")";
																							      
																							      PreparedStatement i_stmt = conn.prepareStatement(insert_sql,Statement.RETURN_GENERATED_KEYS);
																							      
																							      i=1;
																							     
																							      i_stmt.setInt(i++, ownerid);
																							      i_stmt.setString(i++, locationdescription);
																							      i_stmt.setString(i++, "http://snapnstore.imagineteamsolutions.com/www.snapnstore.com.au/htdocs/uploadedimages/groupid_" + newitemgroupid + "/" + nopathfilename);
																							      i_stmt.setTimestamp(i++, timestamp);
																							      
																							      int insertsuccess = 2;
																							      insertsuccess = i_stmt.executeUpdate();
																							      
																							      if (insertsuccess == 0) 
																							      {
																							    	  result.put("success", false);
																							    	  result.put("error", "Could not insert as completed.");
																							    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
																							      }
																							      else if (insertsuccess == 1) 
																							      {
																							    	  // Get new itemid

																							    	  ResultSet insert_rs = i_stmt.getGeneratedKeys();
																							    	  
																							    	  insert_rs.next();
																							    	  Integer newitemid=insert_rs.getInt(1);
																							    	  
																							    	  // Insert item into group
																							    	  
																						    	  	  String insertgroup_sql = "INSERT INTO itemgroups (itemgroupid, itemid) VALUES " + 
																								  	  	    "( ? , ? )";
																								      
																								      PreparedStatement ig_stmt = conn.prepareStatement(insertgroup_sql,Statement.RETURN_GENERATED_KEYS);
																								      
																								      i=1;
																								     
																								      ig_stmt.setInt(i++, newitemgroupid);
																								      ig_stmt.setInt(i++, newitemid);
																								      
																								      int insertgroupsuccess = 2;
																								      insertgroupsuccess = ig_stmt.executeUpdate();
																								      
																								      if (insertgroupsuccess == 0) 
																								      {
																								    	  result.put("success", false);
																								    	  result.put("error", "Could not insert as completed.");
																								    	  Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
																								      }
																								      else if(insertgroupsuccess == 1) 
																								      {
																								    	//  result.put("success", true);
																								    	  IO.p("File uploaded.");
																								      }
																							      }
																		    	            
																							      fos.close();   
																	    	    	   }
																	    	    	   else
																	    	    	   {
																	    	    		   IO.p("Unzipping: File was not an image: " + filename + ", so not unzipped.");
																	    	    	   }

																	    	            // Now done with this file.
																	    	            ze = zis.getNextEntry();
																	    	    	}
																	    	 
																	    	        zis.closeEntry();
																	    	    	zis.close();
																	    	 
																	    	    	result.put("success", true);
																	    	 
																	    	    }catch(IOException ex){
																	    	       ex.printStackTrace(); 
																	    	    }
																	    	     
																    	      
																			      
																    	  	  
																	      }
																      }
														  	    	
														  	    }
												  	    	
												  	    }
										  	    }
						          }
						          else
						          {
						        	  result.put("error", "Missing mandatory parameters.");
						        	  result.put("success", false);
						          }
		//	}}
				//} // End of check JSON parameters
		
			/*
			} // End of check get parameters.	


		}// End of input parameter check
		catch (SQLException e) 
		{
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("error", "Problem with SQL:" + e.getMessage());
	    	Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
	    	*/
		} 
		catch (Exception e) 
		{
		e.printStackTrace();
        result.put("success", false);
        result.put("error", "Exception:" + e.getMessage());
    	//Email.sendAlertEmail(getServletContext(), "ERROR", "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
        
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
	
	

	// convert InputStream to String
	private static String getStringFromInputStream(InputStream is) {
 
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
 
		String line;
		try {
 
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
 
		return sb.toString();
 
	}
 
	
	

}
