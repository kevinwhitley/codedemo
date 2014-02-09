/*
    Copyright (c) 2002 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;

/**
    A set of standard logging output streams. <br>
    Using the members of Owt allows us to print messages to streams that
    can be redirected to different PrintStreams
    @see ksw.kwutil.Logger
*/
public class Owt
{
    public static Logger err = new Logger();
    public static Logger out = new Logger();
}

