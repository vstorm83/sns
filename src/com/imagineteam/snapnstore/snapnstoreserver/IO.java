package com.imagineteam.snapnstore.snapnstoreserver;

import java.math.BigDecimal;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

public class IO {
public static void p(String str)
{
	System.out.println(str);
}

public static String trimOffEnd(String bigstring, String tochop)
{
	String result = bigstring;
		if (result.endsWith(tochop))
		{
			result = result.substring(0, result.length() - tochop.length() );
		}
		return result;
}

public static String truncate(String bigstring, Integer maxlength)
{
	String result = bigstring;
			result = result.substring(0, Math.min(maxlength, result.length()));
		return result;
}

public static void alertDodgyGetRequest(String servletname, ServletContext ctx, HttpServletRequest req)
{
	String queryString = ((HttpServletRequest)req).getQueryString();
	
	Email.sendAlertEmail(ctx, "WARNING", "WARNING in " + servletname + " doGet was called with request '" + queryString + "'") ;
}


public static Double round(Double unrounded, int precision)
{
    BigDecimal bd = new BigDecimal(unrounded);
    BigDecimal rounded = bd.setScale(precision, BigDecimal.ROUND_HALF_UP);
    return new Double(rounded.doubleValue());
}



}
