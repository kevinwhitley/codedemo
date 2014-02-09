/*
    Copyright (c) 1999-2002 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;

public interface FileInfoFilter
{
    /**
        Filter a directory. <br>
        Decide whether a directory should be included in a FileInfo
        listing tree.  The FileInfo for the directory itself will
        be completed at the call, but the contained tree will not
        be filled out.
        @return true if the directory should be included, false if not
    */
    public boolean filterDirectory (int filterId, FileInfo filei);

    /**
        Filter a file. <br>
        Decide whether a file should be included in a FileInfo listing
        tree.  The FileInfo for the file will be completed at the call.
        @return true if the file should be included, false if not
    */
    public boolean filterFile      (int filterId, FileInfo filei);
}

