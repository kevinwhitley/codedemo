/*
    Copyright (c) 2002 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;

import java.io.PrintStream;

/**
    Write messages to a delegated PrintStream. <br>
    This class is uses for instances of the Owt class, so
    that informational and error messages can be redirected
    to various PrintStreams.
    @see ksw.kwutil.Owt
*/
public class Logger
{
    private PrintStream m_out;

    /**
        Create a logger, sending messages to System.out
    */
    public Logger ()
    {
        m_out = System.out;
    }

    /**
        Create a logger on a PrintStream
        @param stream The PrintStream we will write to
    */
    public Logger (PrintStream stream)
    {
        m_out = stream;
    }

    /**
        Change the destination PrintStream
        @param stream the new PrintStream
    */
    public void setPrinter (PrintStream stream)
    {
        m_out = stream;
    }

    /**
        Get the current PrintStream
        @return the current output stream
    */
    public PrintStream getPrinter ()
    {
        return m_out;
    }

    /**
        Print a newline
    */
    public void println ()
    {
        m_out.println();
    }

    /**
        Print a string, as part of a line
        @param str the string we print
    */
    public void print (String str)
    {
        m_out.print(str);
    }

    /**
        Print a string on a line
        @param str the string we print
    */
    public void println (String str)
    {
        m_out.println(str);
    }
}

