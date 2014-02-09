package ksw.webserver;

import java.io.IOException;

import ksw.servlet.AppServlet;

// version of WebServer built so that it can also run on Heroku
public class HerokuServer extends WebServer
{
    public static final int DefaultPortNumber = 8060;

    protected HerokuServer()
    {
        
    }
    
    protected void initialize()
    {
        int portNumber = figurePort();
        //initialize(null, portNumber, null, null);
        lowInit(portNumber, "");
    }

    private int figurePort()
    {
        // if we're on heroku, figure the port number from the environment
        // otherwise, use the default
        String portS = System.getenv("PORT");
        int portNumber = DefaultPortNumber;
        if (portS != null && portS.length() > 0) {
            portNumber = Integer.valueOf(portS);
        }
        
        return portNumber;
    }
    
    protected void setup(String eclipseContentPath, AppServlet.Application application,
                       String apiPath, Class apiClass, String cookieName, Class cookieClass) throws IOException
    {
        String inEclipseVar = System.getenv("INECLIPSE");
        boolean inEclipse = inEclipseVar != null && inEclipseVar.length() > 0;
        
        // the content root varies, depending on whether we're in eclipse or not
        String contentPath = null;
        String libPath = null;
        if (inEclipse) {
            contentPath = eclipseContentPath;
            libPath = "website";
        }
        else {
            contentPath = "src/main/webapp";
            libPath = "src/main/webapp";
        }

        addResourceHandler(contentPath, "");
        addResourceHandler(libPath, "");
        
        // set up the servlet, used to handle actions
        AppServlet appServlet = new AppServlet(application);
        addContextHandler(appServlet, apiPath);
        // set actions and cookies here
        if (apiClass != null) {
            appServlet.registerActions(apiClass);
            //AppServlet.KWServletHandler sh = new AppServlet.KWServletHandler(actionServlet);
            //addContextHandler(sh, apiPath);
        }
        if (cookieClass != null) {
            appServlet.registerCookie(cookieName, cookieClass);
        }
        
    }
}
