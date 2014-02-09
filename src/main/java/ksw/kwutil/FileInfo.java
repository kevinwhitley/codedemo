/*
    Copyright (c) 1999-2002 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;

import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

/**
    Class to capture information about files & directories. <br>
    This class will represent in memory the information that can
    be obtained from the java.io.File class.  By capturing the information
    in a class it saves us from having to hit the disk over and over
    while examining a directory/file tree.
    <p>
    The class provides a number of operations which will "mark" various elements
    of the directory tree.  Only one mark is available, so before using a tree
    in several ways the marks should be cleared (elements are created unmarked).
*/
public class FileInfo implements Comparable
{
    private boolean m_error; // true when the fileInfo has some problem
    private boolean m_dir;
    private File m_theFile;
    private String m_name;
    private long m_lastModifiedTime;
    private FileInfo[] m_files;
    private FileInfo m_parent;
    private boolean m_mark;

    //-------------------------------------------------------------------------
    /**
        Construct a FileInfo for a root directory. <br>
    */
    public FileInfo (String fullpath)
    {
        this(new File(fullpath), null);
    }
    
    public FileInfo(File rootDir)
    {
    	this(rootDir, null);
    }

    //-------------------------------------------------------------------------
    /**
        Construct a FileInfo for a file or directory. <br>
    */
    public FileInfo (File theFile, FileInfo parent)
    {
        m_parent = parent;
        m_error = false;
        m_name = theFile.getName();
        m_lastModifiedTime = theFile.lastModified();
        m_mark = false;
        m_files = null;
        m_theFile = theFile;

        if (theFile.isDirectory()) {
            m_dir = true;
            m_files = new FileInfo[0];
        }
        else if (theFile.isFile()) {
            m_dir = false;
        }
        else {
            m_error = true;
        }
    }

    //-------------------------------------------------------------------------
    /**
        Ask a directory info to build a tree of information. <br>
    */
    public void buildTree (boolean recursive, FileInfoFilter filter, int filterId)
    {
        if (m_error || !m_dir) {
            return; // we only run on a valid directory
        }

        doListing(recursive, filter, filterId);

        return;
    }

    //-------------------------------------------------------------------------
    private void doListing (boolean recursive, FileInfoFilter filter, int filterId)
    {
        File[] fl = m_theFile.listFiles();

        int ii;
        int listLen = (fl == null) ? 0 : fl.length;
        Vector fileList = new Vector();
        for (ii=0; ii<listLen; ii++)
        {
            FileInfo filei = new FileInfo(fl[ii], this);
            if (filei.m_error) {
                continue; // skip if there is trouble
            }
            if (filei.m_dir) {
                if (filter != null && !filter.filterDirectory(filterId, filei)) {
                    continue; // skip this directory
                }
                fileList.addElement(filei);
                if (recursive) {
                    filei.doListing(recursive, filter, filterId);
                }
            }
            else {
                if (filter != null && !filter.filterFile(filterId, filei)) {
                    continue; // skip this file
                }
                fileList.addElement(filei);
            }
        }

        // sort the vectors and put them into our object arrays
        Collections.sort(fileList);
        Object[] oos = fileList.toArray();
        m_files = new FileInfo[oos.length];
        for (int nn=0; nn<oos.length; nn++) {
            m_files[nn] = (FileInfo)oos[nn];
        }
        

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Remove all directories that contain nothing. <br>
        Note that we don't trim "this"
    */
    public void trimEmptyDirectories ()
    {
        if (m_error || !m_dir) {
            return;
        }

        int compressCount = 0;
        for (int ii=m_files.length-1; ii>=0; ii--) {
            FileInfo fi = m_files[ii];
            if (!fi.m_dir) {
                continue;
            }
            fi.trimEmptyDirectories();
            if (!fi.hasChildren()) {
                m_files[ii] = null;
                compressCount++;
            }
        }

        // compressCount has the number we've trimmed out
        // reallocate the array and copy it, skipping the nulls
        FileInfo[] f2 = new FileInfo[m_files.length - compressCount];
        int kk = 0;
        for (int jj=0; jj<m_files.length; jj++) {
            if (m_files[jj] != null) {
                f2[kk++] = m_files[jj];
            }
        }

        m_files = f2;

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Tell whether directory contains anything. <br>
        Files always return false.
    */
    public boolean hasChildren ()
    {
        if (m_error || !m_dir || m_files == null) {
            return false;
        }

        return (m_files.length > 0);
    }

    //-------------------------------------------------------------------------
    public boolean isDirectory ()
    {
        return m_dir;
    }

    //-------------------------------------------------------------------------
    public String getName ()
    {
        return m_name;
    }

    //-------------------------------------------------------------------------
    public long getLastModifiedTime ()
    {
        return m_lastModifiedTime;
    }

    //-------------------------------------------------------------------------
    public void setMark (boolean mark)
    {
        m_mark = mark;
    }

    //-------------------------------------------------------------------------
    public boolean getMark ()
    {
        return m_mark;
    }

    //-------------------------------------------------------------------------
    public boolean isMarked ()
    {
        return m_mark;
    }

    //-------------------------------------------------------------------------
    /**
        Clear all the marks in the tree
    */
    public void clearMarks ()
    {
        m_mark = false;

        if (m_files != null) {
            for (int ii=0; ii<m_files.length; ii++) {
                m_files[ii].clearMarks();
            }
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Mark all the tree
    */
    public void markAll ()
    {
        m_mark = true;
        
        if (m_files != null) {
            for (int ii=0; ii<m_files.length; ii++) {
                m_files[ii].markAll();
            }
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Mark all files that have been modified more recently than a time
    */
    public void markNewer (long modifyTime, boolean markDirs)
    {
        if (!m_dir) {
            if (m_lastModifiedTime > modifyTime) {
                m_mark = true;
            }
        }
        else {
            if (markDirs && m_lastModifiedTime > modifyTime) {
                m_mark = true;
            }
            if (m_files != null) {
                for (int ii=0; ii<m_files.length; ii++) {
                    m_files[ii].markNewer(modifyTime, markDirs);
                }
            }
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Mark all items in a tree that are not in a reference tree. <br>
        Note that this routine does not clear any marks, so clearMarks()
        should probably be called before this routine.
        <p>
        All the matching is done strictly by name
    */
    public void markExtra (FileInfo reference)
    {
        // first, is this extra (doesn't match the reference)
        if (!m_name.equals(reference.m_name) || (m_dir != reference.m_dir)) {
            markAll();
            return;
        }
        if (m_files == null) {
            return; // we're done
        }

        /*
            Recurse on all the elements that match, and mark those which don't.
            If there are extra elements in the reference, we skip them
        */
        int refLen = (reference.m_files != null) ? reference.m_files.length : 0;
        int ourIndex = 0; // index in our files array
        for (int ii=0; ii<refLen && ourIndex < m_files.length; ii++) {
            int cmp = reference.m_files[ii].compareTo(m_files[ourIndex]);
            if (cmp < 0) {
                continue; // the reference item is earlier alphabetically
            }
            else if (cmp == 0) {
                // a match, recurse into the item
                m_files[ourIndex].markExtra(reference.m_files[ii]);
                ourIndex++;
            }
            else {
                // we have an extra file
                m_files[ourIndex].markAll();
                ourIndex++;
                ii--; // so we will compare against the same reference in the next iteration
            }
        }
        
        // any remaining items are extra
        for (int jj=ourIndex; jj<m_files.length; jj++) {
            m_files[jj].markAll();
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Comparison method of Comparable interface
    */
    public int compareTo (Object oo)
    {
        if (oo == this) {
            return 0;
        }
        if (oo instanceof FileInfo) {
            FileInfo fo = (FileInfo) oo;
            int cmp = m_name.compareTo(fo.m_name);
            if (cmp != 0) {
                return cmp;
            }
            // we have the same name, compare parent's name
            if (m_parent != null) {
                if (fo.m_parent == null) {
                    return -1;
                }
                else {
                    return m_parent.compareTo(fo.m_parent);
                }
            }
            else {
                if (fo.m_parent == null) {
                    // both are top
                    return 0;
                }
                else {
                    return 1;
                }
            }
        }
        else {
            if (oo == null) {
                return 1;
            }
            return toString().compareTo(oo.toString());
        }
    }

    //-------------------------------------------------------------------------
    /**
        Get the depth of this info in the tree. <br>
        The depth of the root is 0
    */
    public int getDepth ()
    {
        int depth = 0;
        FileInfo fi = this;
        while (fi.m_parent != null) {
            depth++;
            fi = fi.m_parent;
        }

        return depth;
    }

    //-------------------------------------------------------------------------
    /**
        get the File representation of this file
    */
    public File getFile ()
    {
        return m_theFile;
    }

    //-------------------------------------------------------------------------
    public String getPath ()
    {
        return m_theFile.getPath();
    }

    //-------------------------------------------------------------------------
    /**
        Get the path of this file, relative to the root of the FileInfo tree. <br>
        For the root itself, we'll return an empty string
    */
    public String getRelativePath ()
    {
        if (m_parent == null) {
            return "";
        }

        // get the root File
        FileInfo current = this;
        while (current.m_parent != null) {
            current = current.m_parent;
        }
        File rootFile = current.m_theFile;

        String rootPath = rootFile.getPath();
        String filePath = m_theFile.getPath();
        return filePath.substring(rootPath.length()+1);
    }

    //-------------------------------------------------------------------------
    /**
        Get the file array. <br>
    */
    public FileInfo[] getFiles ()
    {
        return m_files;
    }
    
    //-------------------------------------------------------------------------
    /**
     * Get a child by name
     */
    public FileInfo getChildByName(String name)
    {
    	if (m_files != null) {
    		for (FileInfo child : m_files) {
    			if (child.getName().equals(name)) {
    				return child;
    			}
    		}
    	}
    	
    	return null;
    }

    //-------------------------------------------------------------------------
    /**
        Get the info's parent. <br>
        Intended for use only by FileInfoIterator
    */
    FileInfo getParent ()
    {
        return m_parent;
    }
    
    //-------------------------------------------------------------------------
    /**
        Set the lastModified time on the file
        @param time The time (milliseconds since 1/1/70) of last modification
    */
    public void setLastModified (long time)
    {
        if (m_theFile == null || time < 0) {
            return;
        }

        try {
            // set the time
            m_theFile.setLastModified(time);
            // make sure what we have is correct
            m_lastModifiedTime = m_theFile.lastModified();
        }
        catch (IllegalArgumentException ie) {
            ; // shouldn't happen because we check for negative argument
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Obtain an iterator on the FileInfo.
        @return a FileInfoIterator
        @see FileInfoIterator
    */
    public Iterator getIterator ()
    {
        return new FileInfoIterator(this, false);
    }

    //-------------------------------------------------------------------------
    public void printTree (PrintStream out)
    {
        if (m_error) {
            out.println("!error!");
            return;
        }

        FileInfoIterator iter = new FileInfoIterator(this, false);
        while (iter.hasNext()) {
            FileInfo fi = iter.nextFileInfo();
            int depth = fi.getDepth();
            for (int ii=0; ii<depth; ii++) {
                out.print("  ");
            }
            if (fi.isDirectory()) {
                out.print("+");
            }
            out.println(fi.getName());
            //out.println(fi.getPath());
            //out.println(fi.getRelativePath());
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        A program entry for testing
    */
    public static void main (String[] args)
    {
        FileInfo root = null;

        root = new FileInfo("c:");
        root.buildTree(false, null, 0);
        root.printTree(System.out);
        System.out.println();

        root = new FileInfo("c:\\zkevins\\writing\\journals\\");
        root.buildTree(true, null, 0);
        root.printTree(System.out);
        System.out.println();

        root = new FileInfo("c:\\photography\\photos\\photos01\\");
        FileInfoExtensionFilter filter = new FileInfoExtensionFilter("txt");
        //filter.addInclusion("JBF");
        root.buildTree(true, filter, 0);
        root.printTree(System.out);
        System.out.println();

        root.trimEmptyDirectories();
        root.printTree(System.out);
        System.out.println();
        
    }

    //-------------------------------------------------------------------------
}
