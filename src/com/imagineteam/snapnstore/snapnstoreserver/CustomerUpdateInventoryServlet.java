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

@WebServlet("/uUpdateInventory")
public class CustomerUpdateInventoryServlet extends HttpServlet {

  private static final long serialVersionUID = -6236939698333145467L;

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

        if (!(inputJSON.has("sessionid") && inputJSON.has("itemid") && 
            (inputJSON.has("quantity") || inputJSON.has("imageurl") || inputJSON.has("note")))) {
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
            String updateSql = "UPDATE items SET";
            if (inputJSON.has("quantity")) {
              updateSql += " quantity=" + inputJSON.getInt("quantity") + ",";
            }
            if (inputJSON.has("imageurl")) {
              updateSql += "imageurl='" + inputJSON.getString("imageurl") + "',";
            }
            if (inputJSON.has("note")) {
              updateSql += " note='" + inputJSON.getString("note") + "',";
            }
            updateSql = updateSql.substring(0, updateSql.length() - 1);
            updateSql += " WHERE ownerid = ? AND itemid=?";

            PreparedStatement items_stmt = conn.prepareStatement(updateSql);            
            
            i = 1;
            items_stmt.setInt(i++, userid);
            items_stmt.setInt(i++, inputJSON.getInt("itemid"));
            int updated = items_stmt.executeUpdate();
            
            if (updated > 0) {
              result.put("success", true);
            } else {
              result.put("success", false);
              result.put("error", "not found itemid: " + inputJSON.getInt("itemid") + "of userid: " + userid + "to update");
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
