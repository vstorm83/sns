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

@WebServlet("/payByInvoice")
public class PayByInvoice extends HttpServlet {
  private static final long serialVersionUID = -3232369325226440391L;

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

        if (!(inputJSON.has("sessionid") && inputJSON.has("billingName") && inputJSON.has("billingAddress")
            && inputJSON.has("billingSurbub") && inputJSON.has("billingPostCode") && inputJSON.has("billingState")
            && inputJSON.has("billingContactNumber"))) {
          result.put("success", false);
          result.put("error", "Missing mandatory parameters");
        } else {

          Class.forName("com.mysql.jdbc.Driver");

          conn = DriverManager.getConnection("jdbc:mysql://localhost/wp_snapnstore?" + "user=wpsnapuser&password=wpsnappassword");

          // Check if sessionid is valid
          String sessionidstatement = "SELECT userid, name, email, phone FROM appusers WHERE sessionid = ?";

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

            String billing_sql = "UPDATE appusers SET billing_name=?, billing_address=?, "
                + "billing_surbub=?, billing_postcode=?, billing_state=?, billing_contact_number=? WHERE sessionid=?";

            PreparedStatement billing_stmt = conn.prepareStatement(billing_sql);
            i = 1;

            billing_stmt.setString(i++, inputJSON.getString("billingName"));
            billing_stmt.setString(i++, inputJSON.getString("billingAddress"));
            billing_stmt.setString(i++, inputJSON.getString("billingSurbub"));
            billing_stmt.setString(i++, inputJSON.getString("billingPostCode"));
            billing_stmt.setString(i++, inputJSON.getString("billingState"));
            billing_stmt.setString(i++, inputJSON.getString("billingContactNumber"));
            billing_stmt.setString(i++, inputJSON.getString("sessionid"));

            billing_stmt.executeUpdate();

            // Get customer's plan detail
            rs.beforeFirst();
            rs.next();
            String plans_sql = "SELECT planpurchases.monthlycharge, plans.description FROM planpurchases" 
                + " LEFT JOIN plans on planpurchases.planid = plans.planid WHERE planpurchases.userid = ?";

            PreparedStatement plans_stmt = conn.prepareStatement(plans_sql);
            plans_stmt.setInt(1, rs.getInt("userid"));
            ResultSet plans_rs = plans_stmt.executeQuery();
            
            String mCharge = plans_rs.getString("monthlycharge");
            String description = plans_rs.getString("description");
            
            StringBuilder content = new StringBuilder("Customer details:<br/>");
            content.append("Name: ").append(rs.getString("name"));
            content.append(" email: ").append(rs.getString("email"));
            content.append(" phone: ").append(rs.getString("phone"));
            content.append("<br/>Plan details:<br/>");
            content.append("Description: ").append(description);
            content.append("<br/>").append("Monthly charge: ").append(mCharge);
            Email.sendAlertEmail(getServletContext(), "A new user has registered via the app with the pay by invoice option", content.toString());
            
            result.put("success", true);
          }
        } // End of check JSON parameters
      } // End of check get parameters.

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
