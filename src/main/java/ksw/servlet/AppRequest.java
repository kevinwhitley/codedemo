package ksw.servlet;

import ksw.kwutil.JSONReader;
import ksw.kwutil.JSONWriter;
import ksw.kwutil.JSONReader.JSONParseException;
import ksw.servlet.AppCookie;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Packaging up of httprequest, response and cookies for the AppServlet
 */
public class AppRequest
{
    private HttpServletRequest _request;
    private HttpServletResponse _response;
    private AppServlet.Application _app;
    private Map<String, AppCookie> _cookies;
    private Object _sessionData;

    public AppRequest (HttpServletRequest request, HttpServletResponse response, AppServlet.Application app)
    {
        _request = request;
        _response = response;
        _app = app;
        _cookies = null;
        _sessionData = null;
    }

    public void takeCookiesFromRequest (Map<String, Class> cookieClasses)
    {
        Cookie[] cookies = _request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cname = cookie.getName();
                Class cookieClass = cookieClasses.get(cname);
                AppCookie ck = null;
                if (cookieClass != null) {
                    try {
                        ck = (AppCookie)cookieClass.newInstance();
                        ck.initializeFromRequest(cookie);
                    }
                    catch (Exception exc) {
                        System.out.println("Exception instantiating cookie: " + exc);
                    }
                }

                if (ck != null) {
                    if (_cookies == null) {
                        _cookies = new HashMap<String, AppCookie>();
                    }
                    AppCookie other = _cookies.put(cname, ck);
                    if (other != null) {
                        System.out.println("Multiple cookies in request of type " + cname);
                    }
                }
            }
        }
    }
    
    public void setSessionData(Object data)
    {
        _sessionData = data;
    }
    
    public Object getSessionData()
    {
        return _sessionData;
    }

    public void setResponseType(String mime)
    {
        _response.setContentType(mime);
    }

    public void setHtmlResponse ()
    {
        setResponseType("text/html");        
    }

    public void setTextResponse ()
    {
        setResponseType("text/plain");
    }
    
    public void setJSONResponse()
    {
        setResponseType("application/json");
    }

    public HttpServletRequest getRequest ()
    {
        return _request;
    }

    public HttpServletResponse getResponse ()
    {
        return _response;
    }

    public PrintWriter getWriter ()
    {
        try {
            return _response.getWriter();
        }
        catch (IOException exc) {
            System.out.println("IOException getting print writer" + exc);
            return null;
        }
    }

    /**
     * Write out a simple text response.
     * Useful mainly for actions.
     * @param message - the message to be written
     */
    public void writeTextResponse (String message)
    {
        setTextResponse();
        PrintWriter writer = getWriter();
        writer.print(message);
        writer.close();
    }
    
    // write the content of the JSONWriter to the response
    // the jWriter is assummed to have already closed the outermost context
    public void writeJSONResponse(JSONWriter jWriter)
    {
        setJSONResponse();
        PrintWriter writer = getWriter();
        writer.print(jWriter.toString());
        writer.close();
    }

    public void writeXmlResponse (String message)
    {
        setResponseType("text/xml");
        PrintWriter writer = getWriter();
        writer.print(message);
        writer.close();
    }
    
    public void writeSuccessJSON(Object msg)
    {
        setJSONResponse();
        PrintWriter pwriter = getWriter();
        pwriter.print(makeSuccessResponse(msg));
        pwriter.close();
    }

    public void writeFailureJSON(Object msg)
    {
        setJSONResponse();
        PrintWriter pwriter = getWriter();
        pwriter.print(makeFailureResponse(msg));
        pwriter.close();
    }
    
    public void writeRedirectJSON(String url)
    {
        setJSONResponse();
        PrintWriter pwriter = getWriter();
        pwriter.print(redirectResponse(url));
        pwriter.close();
    }

    // standard JSON success response
    public static String makeSuccessResponse(Object msg)
    {
        JSONWriter writer = new JSONWriter();
        writer.startObject();
        writer.addItem("action", "success");
        if (msg instanceof JSONWriter) {
            writer.addItem("message", (JSONWriter)msg);
        }
        else {
            writer.addItem("message", ((msg != null) ? msg.toString() : ""));
        }
        writer.endObject();
        return writer.toString();
    }

    public static String makeFailureResponse(Object msg)
    {
        JSONWriter writer = new JSONWriter();
        writer.startObject();
        writer.addItem("action", "error");
        if (msg instanceof JSONWriter) {
            writer.addItem("message", (JSONWriter)msg);
        }
        else {
            writer.addItem("message", msg.toString());
        }
        writer.endObject();
        return writer.toString();
    }

    public static String redirectResponse(String url)
    {
        JSONWriter writer = new JSONWriter();
        writer.startObject();
        writer.addItem("action", "redirect");
        writer.addItem("url", url);
        writer.endObject();
        return writer.toString();
    }

    public AppServlet.Application getApp ()
    {
        return _app;
    }

    public AppCookie getCookie (String cname)
    {
        return (_cookies != null) ? _cookies.get(cname) : null;
    }

    public void addCookie (String cname, AppCookie cookie)
    {
        Cookie browserCookie = new Cookie(cname, cookie.makeCookieString());
        browserCookie.setPath("/");
        _response.addCookie(browserCookie);
    }

    public String getParameter (String pname)
    {
        return _request.getParameter(pname);
    }
    
    public String[] getParameterValues(String pname)
    {
        return _request.getParameterValues(pname);
    }

    public int getParameterInt (String pname)
    {
        return getParameterInt(pname, 0);
    }
    
    public int getParameterInt(String pname, int defaultValue)
    {
        int result = defaultValue;

        String val = getParameter(pname);
        if (val == null) {
            return result;
        }
        try {
            result = Integer.parseInt(val);
        }
        catch(NumberFormatException nfe) {
            result = defaultValue;
        }

        return result;
    }

    // read the request body (as in a POST) as a JSON string, and return the resulting map
    public Map readRequestAsJSON()
    {
        HttpServletRequest hreq = getRequest();
        try {
            // read the request body
            String encoding = hreq.getCharacterEncoding();
            if (encoding == null) {
                throw new JSONParseException("no encoding", null);
            }
            Charset cset = Charset.forName(encoding);
            ServletInputStream instream = hreq.getInputStream();
            InputStreamReader reader = new InputStreamReader(instream, cset);
            char[] buffer = new char[128];
            StringBuilder builder = new StringBuilder();
            while(true) {
                int nread = reader.read(buffer, 0, buffer.length);
                if (nread < 0) {
                    break;
                }
                builder.append(buffer, 0, nread);
            }
            
            // parse that as JSON (maybe we could modify our JSON parser to read directly from the stream...)
            if (builder.length() > 0) {
                JSONReader jreader = new JSONReader();
                Object jsonObj = jreader.parse(builder.toString());
                if (jsonObj instanceof Map) {
                    return (Map)jsonObj;
                }
            }
            
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (JSONParseException exc) {
            // just leave albumData null
            System.err.println("error parsing json data: " + exc);
        }

        return null;
    }
}
