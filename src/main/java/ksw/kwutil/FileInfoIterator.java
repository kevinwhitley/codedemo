/*
    Copyright (c) 2002 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
    An iterator over a FileInfo tree. <br>
    This class allows familiar iteration operations over a FileInfo tree.  It
    allows both depth-first (return the children first) & depth-last iterations
    (return the parent directory first).
    <p>
    The remove method of Iterator is not supported.
*/
public class FileInfoIterator implements Iterator
{
    private boolean m_depthFirst;
    private FileInfo m_top;
    private FileInfo m_current;
    
    private static final int STACKSIZE = 100;
    private int[] m_stack; // stack of indices into files arrays
    private int m_stackIndex; //  where we are in stack

    private boolean m_haveNext;
    private FileInfo m_theNext;

    //-------------------------------------------------------------------------
    /**
        Create an iterator - it will be depthFirst
    */
    public FileInfoIterator (FileInfo fi)
    {
        this(fi, true);
    }

    //-------------------------------------------------------------------------
    /**
        Create an iterator, with depthFirst optional. <br>
    */
    public FileInfoIterator (FileInfo fi, boolean depthFirst)
    {
        m_top = fi;
        m_depthFirst = depthFirst;
        m_current = m_top;
        m_stack = new int[STACKSIZE];
        m_stackIndex = 0;
        m_stack[0] = -1; // indicates we haven't done anything with current yet

        m_haveNext = false;
        m_theNext = null;
    }

    //-------------------------------------------------------------------------
    public boolean hasNext ()
    {
        /*
            We discover whether we have a next element by calculating it,
            which requires us to remember the calculated element
        */
        if (!m_haveNext) {
            m_theNext = calculateNextFileInfo();
            m_haveNext = true;
        }

        return m_theNext != null;
    }

    //-------------------------------------------------------------------------
    public Object next ()
    {
        return nextFileInfo();
    }

    //-------------------------------------------------------------------------
    public FileInfo nextFileInfo ()
    {
        if (!m_haveNext) {
            m_theNext = calculateNextFileInfo();
            m_haveNext = true;
        }

        m_haveNext = false;
        if (m_theNext == null) {
            throw new NoSuchElementException("FileInfoIterator");
        }

        return m_theNext;
    }

    //-------------------------------------------------------------------------
    private FileInfo calculateNextFileInfo ()
    {
        //System.out.print("itr: " + ((m_current != null) ? m_current.getName() : "no parent"));
        //System.out.println(" " + m_stackIndex + " " + ((m_stackIndex >= 0) ? m_stack[m_stackIndex] : -99));
        if (m_stackIndex < 0) {
            return null;  // we've gone through the whole tree already
        }

        int index = m_stack[m_stackIndex];

        if (index >= 0) {
            // normal case, we're in the file list
            FileInfo[] infos = m_current.getFiles();
            if (infos != null && index < infos.length) {
                m_stack[m_stackIndex]++; // advance to next
                if (infos[index].isDirectory()) {
                    // we need to go down to next level
                    m_stackIndex++;
                    m_stack[m_stackIndex] = -1; // we need to start
                    m_current = infos[index];
                    return calculateNextFileInfo(); // recurse
                }
                else {
                    // just return the file
                    return infos[index];
                }
            }
            else {
                // we've run off end
                m_stackIndex--;
                FileInfo oldCurrent = m_current;
                m_current = m_current.getParent();
                if (m_depthFirst) {
                    // if we are depth-first, we still need to return current
                    return oldCurrent;
                }
                return calculateNextFileInfo(); // recurse
            }
        }
        else {
            // we haven't started traversing the current fileinfo yet
            if (!m_depthFirst) {
                // return the current directory first
                m_stack[m_stackIndex] = 0;
                return m_current;
            }
            else {
                // start through files
                m_stack[m_stackIndex] = 0;
                return calculateNextFileInfo(); // recurse
            }
        }
    }

    //-------------------------------------------------------------------------
    public void remove ()
    {
        throw new UnsupportedOperationException("FileInfoIterator doesn't support remove");
    }

}

