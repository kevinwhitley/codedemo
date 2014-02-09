package ksw.servlet;

import javax.servlet.http.Cookie;

/**
 */
public abstract class AppCookie
{
    // used by AppRequest to build the object from the incoming cookie value
    public abstract void initializeFromRequest (Cookie cookie);

    // used by AppRequest to convert the object into a cookie string
    public abstract String makeCookieString ();
}
