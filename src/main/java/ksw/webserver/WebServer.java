package ksw.webserver;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.Velocity;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import ksw.kwutil.ArgsToMap;
import ksw.servlet.AppServlet;
import ksw.servlet.AppServlet.Application;
import ksw.webserver.page.SamplePage;

// straightforward webserver built on top of Jetty 7
// handles AppServlet, file serving and Velocity templates (within AppServlet)
public class WebServer
{
    private Server _server = null;
    private String _contextPath;
    private String _rootPath;
    private boolean _inited;
    
    private List<Handler> _handlerList;

    public static final String AppServletName = "ws";
    public static final int DefaultPortNumber = 9001;

    // argument tokens
    public static final String FilesArg = "files";
    public static final String TemplatesArg = "templates";


    
    /**
     * Main entry point to run the webserver.
     * The program takes three arguments:
     * 0: the full path to the directory which is used for the root of files served up by the fileread servlet
     * 1: the full path to the directory which contains the Velocity templates.  This directory will have subdirectories
     * 2: the full path to the directory for client files (jslib, csslib, etc)
     *    corresponding to class pathnames.
     * @param args - the arguments
     */
    public static void main(String[] args)
    {
        ArgsToMap argProcessor = new ArgsToMap(
                // path to templates (root of a classpath)
                TemplatesArg, ArgsToMap.StringArg,
                // list of additional file servlets - the servlet name and the path
                FilesArg, ArgsToMap.DoubleListArg);
        Map<String,Object> processedArgs = argProcessor.processArgs(args);
        if (processedArgs == null) {
            System.out.println("Program arguments invalid");
            System.exit(1);
        }
        // create app servlet, register pages and add the servlet
        WebServer server = new WebServer();
        AppServlet appS = new AppServlet(new TestApplication());
        appS.registerPage(SamplePage.PageName, SamplePage.class);

        server.initialize(processedArgs, 8060, AppServletName, appS);
        
        
        
        server.run();
    }
    
    private static class TestApplication extends Application
    {
        
    }

    public WebServer()
    {
        _inited = false;
        _handlerList = new ArrayList<Handler>(40);
    }

    public void initialize(Map<String, Object> arguments, int portNumber, String appServletName, AppServlet appS)
    {
        String templatesArg = (arguments != null) ? (String)arguments.get(TemplatesArg) : null;
        if (templatesArg != null) {
            initializeVelocity(templatesArg);
        }

        lowInit(portNumber, "");
        // add test servlets
        //_sserver.addTestServlets();

        if (arguments != null) {
            // add file-reading handlers - hands back requested files
            List<String[]> fileServlets = (List<String[]>)arguments.get(FilesArg);
            for (String[] servletInfo : fileServlets) {
                //_sserver.addResourceHandler(servletInfo[1], servletInfo[0]);
                addFileHandler(servletInfo[1], servletInfo[0]);
                System.out.println("use " + servletInfo[0] + " to access files in " + servletInfo[1]);
            }
        }

        if (appServletName != null && appS != null) {
            addContextHandler(appS, "/"+appServletName);
            //addAppServletHandler(appS, "/"+appServletName);
        }
    }
    
    /**
     * Initialize the server.
     * @param portNumber the port number to listen for http requests
     * @param contextPath context path of the servlet, usually "" or "/blah"
     */
    protected void lowInit (int portNumber, String contextPath)
    {
        if (_inited) {
            // we only allow initialization once
            return;
        }
        _server = new Server(portNumber);
        _rootPath = getLocalAddress();
        if (portNumber != 80) {
            // this is kind of funky
            // we want the root path to include the port number when running on a home server
            // but when running in heroku - we don't want the port number
            // so see if we're at home:
            if (isLocal()) {
                _rootPath += ":" + portNumber;
            }

        }
        _contextPath = contextPath;

        // set up the connector
        Connector connector = new SocketConnector();
        connector.setPort(portNumber);
        _server.setConnectors(new Connector[]{connector});

        // set up the internal context, used for internal servlets
        //_internalContext = new ServletContextHandler(_server, _contextPath, ServletContextHandler.SESSIONS);

        _inited = true;
    }

    // heuristic approach to figuring out whether we are running a local instance
    // or in some hosting
    public boolean isLocal()
    {
        // see if our artificial "running in eclipse" variable is set
        String inEclips = System.getenv("INECLIPSE");
        if (inEclips != null && inEclips.length() > 0) {
            return true;
        }
        
        // otherwise, check a mac development environment variable
        String terminal = System.getenv("TERM_PROGRAM");
        return (terminal != null && terminal.length() > 0);
    }
    
    private void initializeVelocity (String templatePath)
    {
        try {
            Velocity.setProperty("file.resource.loader.path", templatePath);
        }
        catch (Exception exc) {
            System.out.println("Exception initializing velocity: " + exc);
        }
    }

    /**
     * Add a servlet to the server.
     * This adds a servlet
     * <p/>
     * This method must be called before the server is started.
     *
     * @param srv  The servlet to be added
     * @param servletName the name of the servlet (if null, then servlet handles "root" requests)
     * @return an error string, or null if no error
     */
    /*
    public String addServlet (AppServlet srv, String servletName)
    {
        //Log.servlet.info("Adding internal servlet with contextPath %s", contextPath);
        String contextPath = (servletName != null) ? "/" + servletName + "/*" : "/*";
        //_internalContext.addServlet(new ServletHolder(srv), contextPath);

        return null;
    }
    */

    // resource handler - for serving up files directly from a path
    // path is the path to the directory containing the resources (files)
    // prefix is the prefix used to access those files, and should have the form "xxx" (no slash)
    public void addResourceHandler(String path, String prefix)
    {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(path);
        ContextHandler ch = new ContextHandler();
        ch.setHandler(resourceHandler);
        ch.setContextPath("/" + prefix);

        _handlerList.add(ch);
    }
    
    // rest handler
    public void addRestHandler(RestHandler rh, String context)
    {
        ContextHandler restContext = new ContextHandler();
        restContext.setContextPath(context);
        restContext.setHandler(rh);
        
        _handlerList.add(restContext);
    }

    // equivalent to addResourceHandler, but use our FileReadHandler instead of Jetty's ResourceHandler
    public void addFileHandler(String path, String prefix)
    {
        FileReadHandler fileHandler = new FileReadHandler(new File(path));
        ContextHandler ch = new ContextHandler();
        ch.setHandler(fileHandler);
        ch.setContextPath("/" + prefix);

        _handlerList.add(ch);
    }
    
    // add one of my AppServlets
    // path should be something like "/xxx
    /*
    public void addAppServletHandler(AppServlet appS, String path)
    {
        AppServlet.KWServletHandler sh = new AppServlet.KWServletHandler(appS);
        addContextHandler(sh, path);
    }
    */

    // lower-level abstract handler
    public void addContextHandler(AbstractHandler ah, String context)
    {
        ContextHandler ch = new ContextHandler();
        ch.setContextPath(context);
        ch.setHandler(ah);
        
        _handlerList.add(ch);
    }

    /*
    public void addTestServlets ()
    {
        // add the echo test servlets
        addServlet(new EchoServlet(), EchoServlet.ServletName);
        addServlet(new EchoReply(), EchoReply.ServletName);
    }
    */

    public String getAddress ()
    {
        if (_contextPath == null || _contextPath.length() < 1) {
            return _rootPath + "/";
        }
        else {
            return _rootPath + _contextPath + "/";
        }
    }

    /*
    // possibly add additional servlets
    // must be called before webserver is started
    public void addServlet(Servlet servlet, String servletName)
    {
        _sserver.addServlet(servlet, servletName);
    }
    */

    public void run()
    {
        // display the local host address, for clients to connect
        try {
            InetAddress localH = InetAddress.getLocalHost();
            System.out.println("Local host is " + localH.toString());
        }
        catch (UnknownHostException uhe) {
            System.out.println("Unknown host?");
        }

        if (!_inited) {
            // default initialization
            lowInit(80, "");
        }
        
        try
        {
            Handler[] handlers = new Handler[_handlerList.size()];
            _handlerList.toArray(handlers);
            _handlerList = null;  // indicates that we've started the server

            HandlerList hlist = new HandlerList();
            hlist.setHandlers(handlers);
            _server.setHandler(hlist);
            
            
            _server.start();
            _server.join();
        }
        catch (Exception exc)
        {
            String exception = exc.toString();
            System.out.println("Webserver startup failed with " + exception);
            _server = null;
        }

    }
    
    private static String getLocalAddress ()
    {
        String result;
        try {
            InetAddress localH = InetAddress.getLocalHost();
            String hostAddress = localH.toString();
            int indx = hostAddress.indexOf('/');
            if (indx >= 0) {
                hostAddress = hostAddress.substring(indx + 1);
            }
            result = "http://" + hostAddress;
        }
        catch (UnknownHostException uhe) {
            // just use default & hope it works
            result = "http://localhost";
        }
        return result;
    }

    public void redirect (HttpServletResponse response, String redirectTo)
            throws IOException
    {
        String addr = getAddress() + redirectTo;
        
        response.setContentType("text/plain");
        response.sendRedirect(addr);
    }
}
