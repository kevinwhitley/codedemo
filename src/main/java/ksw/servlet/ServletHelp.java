/*
    Copyright (c) 1998-2003 Kevin S. Whitley
    All rights reserved.
*/

package ksw.servlet;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

import ksw.kwutil.FileUtil;

/**
    This class contains several helper routines that are useful when writing
    servlets for servicing HTTP requests.
*/
public class ServletHelp
{
    // string which preceeds names of buttons
    // (so we can tell which button was pressed)
    static final String ActionPrefix = "!";
    
    // standard cookie parameter names
    public static final String VersionParameter = "v";

    public static Cookie getCookie (HttpServletRequest request, String name)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie ck : cookies) {
            if (ck.getName().equals(name)) {
                return ck;
            }
        }

        return null;
    }

    // convert a Map into our cookie serialization
    public static String getCookieValue (Map<String, Object> values)
    {
        StringBuffer result = new StringBuffer("a");  // 'a' is version mark
        for (String key : values.keySet()) {
            result.append(key);
            result.append("=");
            result.append(values.get(key));
            result.append(";");
        }

        return result.toString();
    }

    // convert our cookie serialization into a map
    public static Map<String, String> getCookieMap (Cookie ck)
    {
        Map<String, String> result = new HashMap<String, String>();
        try {
            String ckvalue = ck.getValue();
            if (ckvalue == null || ckvalue.length() < 1 || ckvalue.charAt(0) != 'a') {
                // bad value or version
                return result;
            }
            String[] keyvals = ckvalue.substring(1).split(";");
            for (String keyval : keyvals) {
                int eqindx = keyval.indexOf('=');
                if (eqindx > 0) {
                    String key = keyval.substring(0, eqindx);
                    String val = keyval.substring(eqindx+1);
                    result.put(key, val);
                }
            }
        }
        catch(PatternSyntaxException exc) {
            System.out.println("Internal error in ServletHelp.getCookieMap");
        }

        return result;
    }
    
    // get the saved version out of the cookie map
    public static int getVersion(Map<String, String> cookieMap)
    {
        String versionS = (cookieMap != null) ? cookieMap.get(VersionParameter) : "0";
        int version = 0;
        try {
            version = Integer.parseInt(versionS);
        }
        catch (NumberFormatException exc) {
            ;
        }
        
        return version;
    }

    //-------------------------------------------------------------
    public static void writeDocType (PrintWriter pw)
    {
        pw.println(
          "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
    }

    //-------------------------------------------------------------
    public static void writeHeader (PrintWriter pw, String title)
    {
        pw.println("<HTML>\n" +
                   "<HEAD><TITLE>" + title +"</TITLE></HEAD>");
    }

    //-------------------------------------------------------------
    public static void writeHeader (PrintWriter pw, String title, String styles)
    {
        pw.println("<HTML>\n");
        pw.print("<HEAD><TITLE>");
        pw.print(title);
        pw.println("</TITLE>");
        if (styles != null) {
            pw.println("<style type=\"text/css\">");
            pw.println(styles);
            pw.println("</style>");
        }
        pw.println("</HEAD>");

        pw.println("<BODY>");
    }

    //-------------------------------------------------------------
    /**
       Write a standard header, but with tell the browser the page has expired.
       This should stop the browser from caching the page.
       @param pw The printwriter for the page
       @param title Title for the page
    */
    public static void writeExpiredHeader (PrintWriter pw, String title)
    {
        pw.println("<HTML>\n" +
                   "<HEAD><TITLE>" + title +"</TITLE>");
        pw.println("<META HTTP-EQUIV=\"expires\" CONTENT=\"Sun, 06 Aug 2000 01:01:00 GMT\">");
        pw.println("</HEAD>");
    }

    //-------------------------------------------------------------
    public static void writeFooter (PrintWriter pw)
    {
        pw.println("</BODY></HTML>");
    }

    //-------------------------------------------------------------
    public static void writeButton (PrintWriter pw, String action, String label)
    {
        pw.print("<input type=submit name=\"");
        pw.print(ActionPrefix);
        pw.print(action);
        pw.print("\" value=\"");
        pw.print(label);
        pw.println("\">");
    }

    //-------------------------------------------------------------
    public static void errorReply(PrintWriter out, String title, String msg)
    {
        out.println("<HEAD><TITLE>"+title+"</TITLE></HEAD><BODY>");
        out.println("<h1>"+title+"</h1>");
        out.println("<P>"+msg);
        out.println("<hr>");
        out.println("</BODY>");
        out.close();
    }

    public static void writeFileToResponse (File readFile, HttpServletResponse response)
            throws IOException
    {
        String state = "outside";
        try {
            if (!readFile.exists()) {
                response.sendError(404);
                return;
            }
            
            String mimeType = mimeFromFile(readFile);
            if (mimeType != null) {
                //System.out.println("sent content type for " + readFile.getName() + " to " + mimeType);
                response.setContentType(mimeType);
            }
            else {
                System.out.println("no content type for " + readFile.getName());
            }

            // set the content length
            response.setContentLength((int)readFile.length());

            int buffsize = 1024 * 62;
            byte[] mybuff = new byte[buffsize];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(readFile), buffsize+2);
            BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream(), buffsize+2);
            while (true) {
                state = "reading";
                int nread = in.read(mybuff, 0, buffsize);
                state = "writing";
                if (nread > 0) {
                    out.write(mybuff, 0, nread);
                }
                state = "inloop";
                if (nread < buffsize) {
                    break;
                }
            }
            out.flush();
            in.close();

        }
        catch (Exception exc) {
        	System.out.println("(" + state + ") Exception writing file to response: " + exc);
            response.sendError(404, "internal error");
        }

    }
    
    // figure a mime type based on the file extension
    private static String mimeFromFile(File theFile)
    {
        String ext = FileUtil.getExtension(theFile, false).toLowerCase();
        if ("html".equals(ext) || "htm".equals(ext)) {
            return "text/html";
        }
        else if ("js".equals(ext)) {
            return "application/javascript";
        }
        else if ("css".equals(ext)) {
            return "text/css";
        }
        else if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return "image/jpeg";
        }
        else if ("png".equals(ext)) {
            return "image/png";
        }
        else if ("gif".equals(ext)) {
            return "image/gif";
        }
        else if ("mov".equals(ext)) {
            return "video/quicktime";
        }
        else if ("mpg".equals(ext)) {  // this is ambiguous - could be audio or video
            return "audio/mpeg";
        }
        else if ("mpeg".equals(ext)) {  // this is ambiguous? - could be audio or video
            return "video/mpeg";
        }
        else if ("ogv".equals(ext)) {  // not sure that this is standard
            return "video/ogg";
        }
        else {
            // unknown - don't set the mime type
            return null;
        }
    }

}


