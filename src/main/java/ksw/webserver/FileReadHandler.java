package ksw.webserver;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ksw.servlet.ServletHelp;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

// handler to read files for Jetty 7 - can be used as an alternative to their ResourceHandler (when it has bugs)
public class FileReadHandler extends AbstractHandler
{
    private File _fileDir;

    public FileReadHandler (File fileDir)
    {
        _fileDir = fileDir;
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        // what kind of request do we have?
        String method = request.getMethod();
        if ("GET".equals(method)) {
            doGet(request, response);
        }
        else if ("HEAD".equals(method)) {
            doHead(request, response);
        }
        else {
            // we don't handle anything else
            response.setStatus(404);
            response.sendError(404);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.charAt(0) != '/') {
            // can't find it
            response.sendError(404);
            return;
        }
    
        File readFile = new File(_fileDir, pathInfo);
        ServletHelp.writeFileToResponse(readFile, response);
    }
    
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        System.out.println("Unhandled head request in FileReadHandler!!");
    }

}
