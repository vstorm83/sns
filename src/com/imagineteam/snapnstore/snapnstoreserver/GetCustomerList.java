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

@WebServlet("/getCustomerList")
public class GetCustomerList extends HttpServlet {

  private static final long serialVersionUID = 9075038705666851568L;

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
        result.put("error", "Missing POST parameter.");
      } else {

        JSONObject inputJSON = new JSONObject(json);
        inputString = inputJSON.toString(1);
        IO.p(this.getServletName() + " INPUT:" + inputJSON.toString(1));

        if (!(inputJSON.has("sessionid"))) {
          result.put("success", false);
          result.put("error", "Missing mandatory parameters");
        } else {
          Class.forName("com.mysql.jdbc.Driver");
          conn = DriverManager.getConnection("jdbc:mysql://localhost/wp_snapnstore?" + "user=wpsnapuser&password=wpsnappassword");

          // Check if sessionid is valid
          String sessionidstatement = "SELECT usertype FROM appusers WHERE sessionid = ?";
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

            if (rs.getString("usertype").equalsIgnoreCase("courier")) {
              String customerQuery = "SELECT userid, name, businessname FROM appusers WHERE usertype='customer'";
              PreparedStatement customerstmt = conn.prepareStatement(customerQuery);
              
              JSONArray users = new JSONArray();
              ResultSet c_rs = customerstmt.executeQuery();
              
              while(c_rs.next()) {
                JSONObject user = new JSONObject();
                user.put("userid", c_rs.getLong("userid"));
                user.put("name", c_rs.getString("name"));
                user.put("businessname", c_rs.getString("businessname"));
                users.put(user);
              }
              result.put("success", true);
              result.put("customers", users);
            } else {
              result.put("success", false);
              result.put("error", "This user is not a courier.");
              result.put("usererror", "You are not registered as a courier. Please contact support.");
              Email.sendAlertEmail(getServletContext(),
                                   "ERROR",
                                   "ERROR in " + this.getServletName() + " input:" + inputString + " "
                                       + result.getString("error"));
            }
          } // End of participant-check

        } // End of check JSON parameters

      } // End of check GET parameters.

    } catch (SQLException e) {
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
      IO.p(this.getServletName() + " OUTPUT:" + result.toString(1));
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
