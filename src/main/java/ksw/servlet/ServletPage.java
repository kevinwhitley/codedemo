package ksw.servlet;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.IOException;

import ksw.servlet.AppRequest;

/**
 * Abstraction for writing out a page
 */
public abstract class ServletPage
{
    private AppRequest _request;

    public void writePage (AppRequest req)
            throws IOException
    {
        _request = req;

        req.setResponseType(getResponseType());

        PrintWriter out = req.getResponse().getWriter();
        try {
            start(req);
            write(out);
            out.close();
        }
        finally {
            finish();
        }
    }
    
    /* by default our response type is standard html */
    public String getResponseType()
    {
        return "text/html";
    }
    
    /**
     * Place for subclasses to put any logic that should be invoked before the page is written.
     * Subclasses should ALWAYS call super.
     */
    public void start (AppRequest request)
    {
    }

    public abstract void write(PrintWriter writer);

    /**
     * Place for subclasses to put any logic that should be invoked after the page is written.
     * Subclasses should ALWAYS call super.
     * Note that finish is called even when an exception is thrown while writing the page.
     */
    public void finish()
    {

    }

    /**
     * Get the original http request, generally for retrieving parameters.
     * @return the request
     */
    protected HttpServletRequest getRequestzz()
    {
        return _request.getRequest();
    }

}
