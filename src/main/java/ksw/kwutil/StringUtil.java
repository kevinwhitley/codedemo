/*
    Copyright (c) 1999-2003 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;
import java.util.Vector;

public class StringUtil
{
    public static final String ISO8859CharSetName = "ISO-8859-1";
    
    //-------------------------------------------------------------------------
    public static String upperFirst (String ss)
    {
    	return upperFirst(null, ss);
    }
    
    public static String upperFirst(String prefix, String ss)
    {
        if (ss == null || ss.length() < 1) {
        	if (prefix == null) {
	            return ss;
        	}
        	else {
        		return prefix;
        	}
        }

        StringBuffer sb = new StringBuffer();
        if (prefix != null) {
        	sb.append(prefix);
        }
        sb.append(Character.toUpperCase(ss.charAt(0)));
        sb.append(ss, 1, ss.length());

        return new String(sb);
    }

    //-------------------------------------------------------------------------
    public static String stringReplace (String template, String target, String value)
    {
        if (template == null)
        {
            return null;
        }
        
        if (target == null)
        {
            return template;
        }
        
        if (value == null)
        {
            value = "";
        }
        
        if (target.length() == 1 && value.length() == 1) {
            // simple character replacement
            return template.replace(target.charAt(0), value.charAt(0));
        }

        /*
            It is tempting to use the replaceAll method that is in String since
            java1.4.1, but then there is trouble if there are any characters
            in target or value that are special for the regular expression machine
            So, we'll keep using the bad version.
        */
        // grossly inefficient implementation...
        // should use StringBuffer for work scratch space

        String result = "";
        String whatsleft = template;
        String beginning = null;
        int tlen = target.length();

        while (true)
        {
            int targetIndex = whatsleft.indexOf(target);
            if (targetIndex < 0)
            {
                result += whatsleft;
                break;
            }

            beginning = whatsleft.substring(0,targetIndex);
            whatsleft = whatsleft.substring(targetIndex + tlen);
            result += beginning + value;
        }

        return result;
    }
    
    //-------------------------------------------------------------------------
    public static boolean isJavaIdentifier (String str)
    {
        // str must be non-null and have something in it
        if (str == null)
        {
            return false;
        }
        
        int len = str.length();
        if (len < 1)
        {
            return false;
        }
        
        // the first character needs to be a legal identifier start
        char cc = str.charAt(0);
        if (!Character.isJavaIdentifierStart(cc))
        {
            return false;
        }
        
        // all the rest must be legal identifier characters
        int ii;
        for (ii=1; ii<len; ii++)
        {
            cc = str.charAt(ii);
            if (!Character.isJavaIdentifierPart(cc))
            {
                return false;
            }
        }
        
        // everything checks out
        return true;
    }

    public static String padNumberLeft(int nn, int width)
    {
        return padNumberLeft(nn, width, ' ');
    }

    /**
     * Pad a number out to a given width.
     * If the number is already longer than the width - do nothing
     * @param nn - the number
     * @param width - minimum width we want
     * @return the formatted number
     */
    public static String padNumberLeft (int nn, int width, char padc)
    {
        String ns = Integer.toString(nn);
        if (ns.length() < width) {
            StringBuffer buff = new StringBuffer(ns);
            int add = width - ns.length();
            for (int ii=0; ii<add; ii++) {
                buff.insert(0, padc);
            }
            return buff.toString();
        }
        else {
            return ns;
        }
    }

    //-------------------------------------------------------------------------
    /**
        Convert a string into a version suitable for HTML. </br>
        This method will take the input string and perform a couple of
        transformations.  All newlines will be converted to line breaks,
        and some special characters will be converted into their HTML escape
        versions
        @param in The input string
        @return The HTML-ized version of in
    */
    public static String forHTML (String in)
    {
        if (in == null) {
            return "";
        }

        if (in.indexOf('&') < 0 &&
                in.indexOf('<') < 0 &&
                in.indexOf('>') < 0 &&
                in.indexOf('\n') < 0) {
            // no translation needed
            return in;
        }

        StringBuilder buff = new StringBuilder(in);
        int len = buff.length();
        int ii=0;
        while (ii<len) {
            char cc = buff.charAt(ii);
            switch (cc) {
            case '&':
                buff.replace(ii, ii+1, "&amp;");
                len += 4;
                ii += 5;
                break;
            case '<':
                buff.replace(ii, ii+1, "&lt;");
                len += 3;
                ii += 4;
                break;
            case '>':
                buff.replace(ii, ii+1, "&gt;");
                len += 3;
                ii += 4;
                break;
            case '\n':
                buff.replace(ii, ii+1, "</br>");
                len += 4;
                ii += 5;
                break;
            default:
                ii++;
                break;
            }
        }

        return buff.toString();
    }

    //-------------------------------------------------------------------------
    /**
        Convert a string into a version suitable for XML. <br>
        This method will take the input string and perform a couple of
        transformations.  Special characters will be converted into their
        XML escape versions
        @param in The input string
        @param escapeQuotes true if we want to escape quotes
        @return The XML-ized version of in
    */
    public static String forXML (String in, boolean escapeQuotes)
    {
        if (in == null) {
            return "";
        }

        if (in.indexOf('&') < 0 &&
                in.indexOf('<') < 0 &&
                in.indexOf('>') < 0 &&
                in.indexOf('"') < 0 &&
                in.indexOf('\'') < 0) {
            // no translation needed
            return in;
        }

        StringBuilder buff = new StringBuilder(in);
        int len = buff.length();
        int ii=0;
        while (ii<len) {
            char cc = buff.charAt(ii);
            switch (cc) {
            case '&':
                buff.replace(ii, ii+1, "&amp;");
                len += 4;
                ii += 5;
                break;
            case '<':
                buff.replace(ii, ii+1, "&lt;");
                len += 3;
                ii += 4;
                break;
            case '>':
                buff.replace(ii, ii+1, "&gt;");
                len += 3;
                ii += 4;
                break;
            case '"':
                buff.replace(ii, ii+1, "&quot;");
                len += 5;
                ii += 6;
                break;
            case '\'':
                buff.replace(ii, ii+1, "&apos;");
                len += 5;
                ii += 6;
                break;
            default:
                ii++;
                break;
            }
        }

        return buff.toString();
    }

    //-------------------------------------------------------------------------
    /**
        Break up a string into tokens
        This method will take a source string and break it into substrings.  The
        substrings are separated from each other by a single separator char in the
        source.  If there are two adjacent separator charactors, or the first or
        last character in source is a separator, then a token of an empty string
        will result.
        @param source The string we will break into tokens
        @param separator The separator character
        @return an array of tokens, or null if source is null
    */
    public static String[] tokenize (String source, char separator)
    {
        if (source == null) {
            return null;
        }

        // we'll allocate an overlarge array and then copy later
        int slen = source.length();
        String[] scratch = new String[slen+1]; // maximum possible size
        int ntokens = 0;

        int start = 0;
        while (true) {
            if (start >= slen) {
                // the last character must have been a separator
                // add an empty token
                scratch[ntokens++] = "";
                break;
            }
            int sepx = source.indexOf(separator, start);
            if (sepx < 0) {
                // we've run off the end, no more separators
                scratch[ntokens++] = source.substring(start);
                break;
            }

            // the normal case
            scratch[ntokens++] = source.substring(start, sepx);
            start = sepx + 1;
        }

        // reallocate to the size we've actually got
        String[] result = new String[ntokens];
        System.arraycopy(scratch, 0, result, 0, ntokens);

        return result;
    }

    //-------------------------------------------------------------------------
    /**
        Break up a string into tokens in Excel CSV file style
        This method will take a source string and break it into substrings.  The
        tokens are separated by a separator character.  A token may begin with
        a quote character, in which case it doesn't end until the next quote character
        is encountered.  Note that a doubled quote character doesn't count as an
        end quote in this case, it is simply a quote character.
        <br>
        The only way for the separator character to be in a token is for the token
        to be quoted.
        <br>
        Leading and trailing white space around a token is considered to be part
        of the token
        <br>
        @param source The string we will break into tokens
        @param separator The separator character
        @param quote The quote character
        @return an array of tokens, or null if source is null
    */
    public static String[] csvTokenize (String source, char separator, char quote)
    {
        if (source == null) {
            return null;
        }

        Vector scratch = new Vector();
        int slen = source.length();

        char[] buffer = new char[slen];
        int bufferind = 0;
        char addchar = '?';

        boolean inbare = false; // in unquoted token
        boolean inquote = false; // in quoted token
        boolean doaddchar = false;
        boolean doendtoken = false;
        for (int ii=0; ii<slen; ii++) {
            char cc = source.charAt(ii);
            if (inquote) {
                if (cc == quote) {
                    if (ii < slen-1 &&
                        source.charAt(ii+1) == quote) {
                        // doubled quote, not end of token
                        addchar = quote;
                        ii++; // to account for extra advancement
                        doaddchar = true;
                    }
                    else {
                        inquote = false;
                        inbare = true; // to handle the rest of the token (presumably nothing)
                    }
                }
                else {
                    addchar = cc;
                    doaddchar = true;
                }
            }
            else if (inbare) {
                if (cc == separator) {
                    doendtoken = true;
                }
                else {
                    addchar = cc;
                    doaddchar = true;
                }
            }
            else {
                // starting a token - with quote or not?
                if (cc == separator) {
                    doendtoken = true; // empty token, which is legal
                }
                else if (cc == quote) {
                    inquote = true;
                }
                else {
                    inbare = true;
                    addchar = cc;
                    doaddchar = true;
                }
            }

            if (doaddchar) {
                if (bufferind < buffer.length) {
                    buffer[bufferind++] = addchar;
                }
                doaddchar = false; // reset flag
            }
            if (doendtoken) {
                String tk = new String(buffer, 0, bufferind);
                bufferind = 0;
                scratch.addElement(tk);
                inquote = false;
                inbare = false;
                doendtoken = false; // reset flag
            }
        }

        // handle the last token
        String tk = new String(buffer, 0, bufferind);
        scratch.addElement(tk);

        Object[] sr = scratch.toArray();
        String[] retval = new String[sr.length];
        System.arraycopy(sr, 0, retval, 0, sr.length);

        return retval;
    }


    //-------------------------------------------------------------------------
    /**
        Testing program
    */
    public static void main (String[] args)
    {
        Owt.out.println(upperFirst(""));
        Owt.out.println(upperFirst("doSomething"));
        Owt.out.println(upperFirst("DoNothing"));

        Owt.out.println(stringReplace("this is a simple test", "is", "xx"));
        Owt.out.println(stringReplace("this is a simple test", "yy", "xx"));
        Owt.out.println(stringReplace("this is a simple test", "sim", "!@#$%^&**()"));

        testIdentifier("abcc99");
        testIdentifier("99_abc");
        testIdentifier("");
        testIdentifier("_a99");

        Owt.out.println(forHTML("a string for HTML"));
        Owt.out.println(forHTML("a string\n & for < HTML >"));

        testTokenize("c:/aa/bb/cc/hoho.txt");
        testTokenize("/one/two/three");
        testTokenize("alpha/beta/gamma/");
        testTokenize("//");

        testCSVTokenize("this,is,a test");
        testCSVTokenize("\"special\"\"characters\",are ' particularly,diffic\\ult");
        testCSVTokenize("something,simp!le,45,,");
        testCSVTokenize(",19,17,the end");
        testCSVTokenize("last line,  something with spaces,on the end  ");
        testCSVTokenize("A test line,\"will quotes work\",  another thing");
        testCSVTokenize("\"to include a comma, we need quotes\", I think, therefore I am");
    }

    //-------------------------------------------------------------------------
    /**
        Testing of the tokenize method
    */
    private static void testTokenize (String totok)
    {
        String[] toks = tokenize(totok, '/');
        Owt.out.println("Tokenize: " + totok);
        for (int ii=0; ii<toks.length; ii++) {
            Owt.out.println("    #" + toks[ii] + "#");
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Testing of the CSVTokenize method
    */
    private static void testCSVTokenize (String totok)
    {
        String[] toks = csvTokenize(totok, ',', '"');
        Owt.out.println("csvTokenize: " + totok);
        for (int ii=0; ii<toks.length; ii++) {
            Owt.out.println("    #" + toks[ii] + "#");
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Testing of the isJavaIdentifier method
    */
    private static void testIdentifier (String ident)
    {
        boolean isi = isJavaIdentifier(ident);
        Owt.out.print(ident);
        Owt.out.print(" is ");
        if (!isi) {
            Owt.out.print("not ");
        }
        Owt.out.println("an identifier");

        return;
    }
}
