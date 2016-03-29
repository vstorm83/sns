package com.imagineteam.snapnstore.snapnstoreserver;

import java.io.File;
import java.io.FileOutputStream;
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

import sun.misc.BASE64Decoder;

@WebServlet("/uAddInventory")
public class CustomerAddInventoryServlet extends HttpServlet {

  private static final long serialVersionUID = -7857514172332173649L;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    IO.alertDodgyGetRequest(getServletName(), getServletContext(), request);
  }

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

      if (null == json) {
        result.put("success", false);
        result.put("error", "Missing GET parameter.");
      } else {

        JSONObject inputJSON = new JSONObject(json);
        inputString = inputJSON.toString(1);
        IO.p(this.getServletName() + " INPUT:" + inputJSON.toString());

        if (!(inputJSON.has("sessionid"))) {
          result.put("success", false);
          result.put("error", "Missing mandatory parameters");
        } else {

          Class.forName("com.mysql.jdbc.Driver");
          conn = DriverManager.getConnection("jdbc:mysql://localhost/wp_snapnstore?" + "user=wpsnapuser&password=wpsnappassword");

          // Check if sessionid is valid
          String sessionidstatement = "SELECT userid FROM appusers WHERE sessionid = ?";

          PreparedStatement sessionidstmt = conn.prepareStatement(sessionidstatement);
          int i = 1;

          sessionidstmt.setString(i++, inputJSON.getString("sessionid"));
          ResultSet rs = sessionidstmt.executeQuery();

          Integer userscount = 0;
          while (rs.next()) {
            userscount++;
          }

          if (userscount.equals(0)) {
            result.put("success", false);
            result.put("error", "Invalid sessionid.");
            Email.sendAlertEmail(getServletContext(),
                                 "ERROR",
                                 "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
          } else if (userscount.equals(1)) {
            rs.beforeFirst();
            rs.next();

            Integer userid = rs.getInt("userid");
            String insertSql = "INSERT INTO items (ownerid";
            if (inputJSON.has("quantity")) {
              insertSql += ", quantity";
            }
            if (inputJSON.has("imageurl")) {
              insertSql += ", imageurl";
            }
            if (inputJSON.has("note")) {
              insertSql += ", note";
            }
            insertSql += ") VALUES (?";
            if (inputJSON.has("quantity")) {
              insertSql += ", ?";
            }
            if (inputJSON.has("imageurl")) {
              insertSql += ", ?";
            }
            if (inputJSON.has("note")) {
              insertSql += ", ?";
            }
            insertSql += ")";
            PreparedStatement items_stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            
            i = 1;
            items_stmt.setInt(i++, userid);
            if (inputJSON.has("quantity")) {
              items_stmt.setInt(i++, inputJSON.getInt("quantity"));
            }
            if (inputJSON.has("imageurl")) {
              items_stmt.setString(i++, inputJSON.getString("imageurl"));
            }
            if (inputJSON.has("note")) {
              items_stmt.setString(i++, inputJSON.getString("note"));
            }
            int inserted = items_stmt.executeUpdate();
            if (inserted == 1) {
              ResultSet rs_key = items_stmt.getGeneratedKeys();
              rs_key.next();
              int id = rs_key.getInt(1);
              
              if (inputJSON.has("imagedata")) {
                String outputfolder = "/var/www/www.imagineteamsolutions.com/htdocs/www.snapnstore.com.au/htdocs/uploadedimages/";
                File folder = new File(outputfolder);
                if (!folder.exists()) {
                  folder.mkdir();
                  IO.p("Creating output folder:" + outputfolder);
                } else {
                  IO.p("Output folder already exists:" + outputfolder);
                }
                
                String nopathfilename = "img_" + id;
                File newFile = new File(outputfolder + File.separator + nopathfilename);
                
                String imageData = inputJSON.getString("imagedata");
                BASE64Decoder decoder = new BASE64Decoder();
                byte[] data = decoder.decodeBuffer(imageData);
                
                FileOutputStream fos = new FileOutputStream(newFile);
                try {
                  fos.write(data);
                } finally {
                  fos.close();
                }
                
                String updateImg = "UPDATE items SET imageurl=? WHERE itemid=?";
                PreparedStatement updateImg_stmt = conn.prepareStatement(updateImg);
                updateImg_stmt.setString(1, "http://snapnstore.imagineteamsolutions.com/www.snapnstore.com.au/htdocs/uploadedimages/img_" + id);
                updateImg_stmt.setInt(2, id);
                updateImg_stmt.executeUpdate();
                
                result.put("imageurl", "http://snapnstore.imagineteamsolutions.com/www.snapnstore.com.au/htdocs/uploadedimages/img_" + id);                
              }
              
              result.put("itemid", id);
            }
            

            if (inserted == 1) {
              result.put("success", true);
            } else {
              result.put("success", false);
              result.put("error", "can insert new item for ownerid: " + userid);
            }

          }
        } // End of check JSON parameters
      } // End of check get parameters.

    } // End of input parameter check
    catch (SQLException e) {
      e.printStackTrace();
      result.put("success", false);
      result.put("error", "Problem with SQL:" + e.getMessage());
      Email.sendAlertEmail(getServletContext(),
                           "ERROR",
                           "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));
    } catch (Exception e) {
      e.printStackTrace();
      result.put("success", false);
      result.put("error", "Exception:" + e.getMessage());
      Email.sendAlertEmail(getServletContext(),
                           "ERROR",
                           "ERROR in " + this.getServletName() + " input:" + inputString + " " + result.getString("error"));

    } finally {
      IO.p(this.getServletName() + " OUTPUT:" + result.toString());
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
