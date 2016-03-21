package com.imagineteam.snapnstore.snapnstoreserver;

import java.util.Calendar;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;



public class Email {
	
	static final Boolean QUIET = false;
	
	public static boolean sendAlertEmail(ServletContext context, String subject, String content)
	{
	    
		if (QUIET)
		{
			IO.p("Quiet mode. Would otherwise have emailed with :'" + content + "'.");
			return true;
		}
		else
		{
			
			// Send an email.
			
			String fromemail = "mailserver@imagineteamsolutions.com";
			
//			String toemail = "connect+snapnstore@imagineteamsolutions.com";
			String toemail = "loweblue@gmail.com";
		
			IO.p("SendingAlertEmail (" + subject + "):" + content);
			
			/*
			InputStream resourceContent = context.getResourceAsStream("/WEB-INF/SharedCycle.html");
			
			String htmlcontent = getStringFromInputStream(resourceContent);
			
			htmlcontent = htmlcontent.replace("*|FROMEMAIL|*", fromemail);
			htmlcontent = htmlcontent.replace("*|TOEMAIL|*", toemail);
			*/
			
			java.util.Properties p = new Properties(); 
			javax.mail.Session s = javax.mail.Session.getInstance(p); 
			Message message = new MimeMessage(s);
			
			try {
				message.setFrom(new InternetAddress(fromemail));
				InternetAddress to[] = new InternetAddress[1];
				to[0] = new InternetAddress(toemail);
				message.setRecipients(Message.RecipientType.TO, to);

				InternetAddress replyto[] = new InternetAddress[1];
				replyto[0] = new InternetAddress("andrew@imagineteamsolutions.com");
				message.setReplyTo(replyto);
				
				message.setSubject("SnapNStore: " + subject);
				message.setContent(content, "text/html;charset=UTF-8");
				Transport.send(message);
				return true;
			} 
			catch (Exception ex) 
			{
				IO.p("sendAlertEmail error:" + ex.getMessage());
			}
		}
			return false;
						  	    
						  	    
						  	    	
	}

	public static boolean sendCustomerInvoiceEmail(ServletContext context, String name, String toemail, String content)
	{
	    
		if (QUIET)
		{
			IO.p("Quiet mode. Would otherwise have emailed customer with :'" + content + "'.");
			return true;
		}
		else
		{
			
			// Send an email.
			
			String fromemail = "mailserver@imagineteamsolutions.com";
			
			String tmptoemail = "connect+snscustomer@imagineteamsolutions.com";
		
			// IO.p("SendingDPSInvoiceEmail (" + subject + "):" + content);
			
//			InputStream resourceContent = context.getResourceAsStream("/WEB-INF/SharedCycle.html");
//			
//			String htmlcontent = getStringFromInputStream(resourceContent);
//			
//			htmlcontent = htmlcontent.replace("*|FROMEMAIL|*", fromemail);
//			htmlcontent = htmlcontent.replace("*|TOEMAIL|*", toemail);
			
			String tempcontent = "";
			tempcontent += "Dear " + name + " ,<br/>";
			tempcontent += "Below is your Tax Invoice from DPS Snap N Store Pty Ltd<br/>";
			tempcontent += "<br/>";
			tempcontent += "<u>Tax Invoice (PAID):</u><br/>";
			tempcontent += "ABN 13 169 122 543<br/>";
			tempcontent += "DPS Snap N Store Pty Ltd<br/>"; 
			tempcontent += "<br/>";
			tempcontent += content;
			tempcontent += "<br/>";
			 tempcontent += "<u>Contact details</u><br/>";
			 tempcontent += "<br/>";
			 tempcontent += "Office hours: 7:30am - 5:30pm<br/>"; 
			 tempcontent += "Monday - Friday<br/>";
			 tempcontent += "<br/>";
			 tempcontent += "Address: PO Box 4176<br/>"; 
			 tempcontent += "Kingston ACT 2604<br/>";
			 tempcontent += "<br/>";
			 tempcontent += "Phone: 0418 626 586<br/>";
			 tempcontent += "http://dpssnapnstore.com.au<br/>";
			 
			 
			java.util.Properties p = new Properties(); 
			javax.mail.Session s = javax.mail.Session.getInstance(p); 
			Message message = new MimeMessage(s);
			
			try {
				message.setFrom(new InternetAddress(fromemail));
				InternetAddress to[] = new InternetAddress[1];
				to[0] = new InternetAddress(tmptoemail); // TODO 
				message.setRecipients(Message.RecipientType.TO, to);

				InternetAddress replyto[] = new InternetAddress[1];
				replyto[0] = new InternetAddress("snap@dpscouriers.com.au");
				message.setReplyTo(replyto);
				
				// Get the time
				Calendar cal = Calendar.getInstance();
				
				message.setSubject("DPS SnapNStore Invoice " + cal.getTime().toString());
				message.setContent(tempcontent, "text/html;charset=UTF-8");
				Transport.send(message);
				IO.p("Successfully emailed customer.");
				return true;
			} 
			catch (Exception ex) 
			{
				IO.p("sendAlertEmail error:" + ex.getMessage());
			}
		}
			return false;
						  	    
						  	    
						  	    	
	}
	
	
/*

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
	*/
}
