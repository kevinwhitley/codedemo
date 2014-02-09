package ksw.servlet;

import ksw.servlet.AppRequest;
import ksw.servlet.AppCookie;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 */
public class AppServlet extends AbstractHandler
{
    private Application _app;
    private Map<String, Class> _pageClasses;
    private Map<String, Method> _actionMethods;
    private Map<String, Class> _cookieClasses;

    public AppServlet (Application app)
    {
        _app = app;
        _pageClasses = new HashMap<String, Class>();
        _actionMethods = new HashMap<String, Method>();
        _cookieClasses = new HashMap<String, Class>();
    }

    public void registerPage (String name, Class pageClass)
    {
        // the page class must be a ServletPage
        if (!ServletPage.class.isAssignableFrom(pageClass)) {
            System.out.println("attempted to register non-ServletPage class " + pageClass.getName());
            return;
        }
        _pageClasses.put(name, pageClass);
    }

    public void registerActions (Class actionClass)
    {
        // the action class will have various methods in it that are static and have the naming convention
        // xxxAction.  The "xxx" then becomes the name of the action expected on the url
        Class[] targetParameters = new Class[1];
        targetParameters[0] = AppRequest.class;

        Method[] methods = actionClass.getMethods();
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String methodName = method.getName();
            if (!methodName.endsWith("Action")) {
                continue;
            }
            Class[] parameters = method.getParameterTypes();
            if (parameters.length != targetParameters.length) {
                continue;
            }
            boolean match = true;
            for (int ii=0; ii<targetParameters.length; ii++) {
                if (parameters[ii] != targetParameters[ii]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                String name = methodName.substring(0, methodName.length()-6); // strip off "Action"
                _actionMethods.put(name, method);
            }
        }
    }

    public void registerCookie (String name, Class cookieClass)
    {
        // the cookie class must be an AppCookie
        if (!AppCookie.class.isAssignableFrom(cookieClass)) {
            System.out.println("attempted to register non-AppCookie class " + cookieClass.getName());
            return;
        }
        _cookieClasses.put(name, cookieClass);
    }

    /*
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        doRequest(request, response);
    }

    protected void doPost (HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        doRequest(request, response);
    }
    */

    @Override
    public void handle(String arg0, Request arg1, HttpServletRequest arg2, HttpServletResponse arg3)
            throws IOException, ServletException
    {
        doRequest(arg2, arg3);
    }
    
    private void doRequest (HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        AppRequest appRequest = new AppRequest(request, response, _app);
        
        // construct cookie objects from the incoming request
        appRequest.takeCookiesFromRequest(_cookieClasses);
        
        // let app assign per-session data
        appRequest.setSessionData(_app.getSessionData(appRequest));

        // figure out who to dispatch to
        String pagePath = request.getPathInfo();
        String[] pathPieces = pagePath.split("/");
        String targetName = "??";
        if (pathPieces.length > 1) {
            try {
            	boolean handled = false;
                targetName = pathPieces[1];
                // first check for a page class
                Class pageClass = _pageClasses.get(targetName);
                if (pageClass != null) {
                    // dispatch to page
                    ServletPage page = (ServletPage) pageClass.newInstance();
                    page.writePage(appRequest);
                    response.setStatus(200);
                    handled = true;
                }
                else {
	                Method actionMethod = _actionMethods.get(targetName);
	                if (actionMethod != null) {
	                    // dispatch to method
	                    Object args[] = new Object[1];
	                    args[0] = appRequest;
	                    actionMethod.invoke(null, args);
	                    handled = true;
	                }
                }
                
                if (handled) {
                	// give the app a chance to do any post-request processing
                	_app.afterRequest(appRequest);
                	return;
                }
            }
            catch(InvocationTargetException ite) {
            	System.out.println("Invocation exception: " + ite);
            	System.out.println("... caused by " + ite.getTargetException());
            	StackTraceElement[] stackElements = ite.getTargetException().getStackTrace();
            	for (StackTraceElement se : stackElements) {
            		System.out.print(" ");
            		System.out.println(se);
            	}
            }
            catch(Exception exc) {
                System.out.println("Exception handling request in AppServlet: " + exc);
            }
        }

        // if we're here, we didn't handle the request
        response.setContentType("text/html");
        response.setStatus(404);
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>Error</title></head><body>Unrecognized target " + targetName + "</body></html>");
    }
    
    public static class Application
    {
        // application returns session data, based on the request (cookies usually)
        public Object getSessionData(AppRequest request)
        {
            return null;
        }
        
        // post-request handling (for instance saving session data)
        public void afterRequest(AppRequest request)
        {
        	
        }
    }

    /* layer to convert between Jetty 7's abstract handler and our AppServlet */
    /*
    public static class KWServletHandler extends AbstractHandler
    {
        private AppServlet _appServlet;
        
        public KWServletHandler(AppServlet appServlet)
        {
            _appServlet = appServlet;
        }

        @Override
        public void handle(String arg0, Request arg1, HttpServletRequest arg2, HttpServletResponse arg3)
                throws IOException, ServletException
        {
            _appServlet.doRequest(arg2, arg3);
        }

    }
    */
}
