/*
    Copyright (c) 1999-2002 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;

import java.util.Vector;

/**
    The classic "filter file by extension filter. <br>
    This class will filter files by their extensions.  It has the
    capabilities of including a list of extensions or excluding
    a list of extensions.  These capabilities do <b>not</b> work
    together.  If any "inclusion" extension is specified, all
    other extensions are considered to be excluded.  The inclusion
    list has priority (if it is specified the exclusion list
    is ignored).
    <p>
    The extensions are case insensitive by default and can be made
    sensitive
*/
public class FileInfoExtensionFilter implements FileInfoFilter
{
    private Vector m_includes;
    private Vector m_excludes;
    private boolean m_sensitive;

    //-------------------------------------------------------------------------
    public FileInfoExtensionFilter ()
    {
        m_includes = new Vector();
        m_excludes = new Vector();
        m_sensitive = false;
    }

    //-------------------------------------------------------------------------
    /**
        Constructor for standard "filter one extension" filter. <br>
    */
    public FileInfoExtensionFilter (String includeExt)
    {
        this();
        addInclusion(includeExt);
    }

    //-------------------------------------------------------------------------
    public void setSensitive (boolean sens)
    {
        m_sensitive = sens;
    }

    //-------------------------------------------------------------------------
    public boolean isSensitive ()
    {
        return m_sensitive;
    }

    //-------------------------------------------------------------------------
    public void addInclusion (String ext)
    {
        m_includes.addElement(ext);
    }

    //-------------------------------------------------------------------------
    public void addExclusion (String ext)
    {
        m_excludes.addElement(ext);
    }

    //-------------------------------------------------------------------------
    public boolean filterDirectory (int filterId, FileInfo filei)
    {
        return true; // we include all directories
    }

    //-------------------------------------------------------------------------
    public boolean filterFile (int filterId, FileInfo filei)
    {
        // get the extension
        String extension = null;
        int dotIndex = filei.getName().lastIndexOf('.');
        if (dotIndex < 0)
        {
            // no extension at all
            extension = "";
        }
        else {
            extension = filei.getName().substring(dotIndex+1);
        }

        // if we have inclusions, they have precedence
        if (m_includes.size() > 0) {
            for (int ii=0; ii<m_includes.size(); ii++) {
                String ext = (String) m_includes.elementAt(ii);
                if (m_sensitive) {
                    if (ext.equals(extension)) {
                        return true;
                    }
                }
                else {
                    if (ext.equalsIgnoreCase(extension)) {
                        return true;
                    }
                }
            }

            // no match, therefore it fails
            return false;
        }

        // otherwise we try to exclude
        for (int jj=0; jj<m_excludes.size(); jj++) {
            String ext2 = (String) m_excludes.elementAt(jj);
            if (m_sensitive) {
                if (ext2.equals(extension)) {
                    return false;
                }
            }
            else {
                if (ext2.equalsIgnoreCase(extension)) {
                    return false;
                }
            }
        }

        // we passed through exclusions, so succeeds
        return true;
    }
}

