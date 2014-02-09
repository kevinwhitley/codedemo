/*
    Copyright (c) 1999-2002 Kevin S. Whitley
    All rights reserved.
*/

package ksw.kwutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
/* -- requires Java7
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
*/
import java.util.StringTokenizer;

/**
    A collection of file & directory utility functions. <br>
    These are all static methods, and perform various things.
    <br>
    There is a test method "main" which is used to drive a few tests.
*/
public class FileUtil
{
    // scratch buffer used for work within a single routine
    // we allocate it once, to save garbage collection
    private static int BUFFERSIZE = 16*1024;

    /**
     * Get the extension of a File - the last string after a period in the name
     * @return the extension which may be an empty string
     */
    public static String getExtension(File fl, boolean greedy)
    {
        String fname = fl.getName();
        return getExtension(fname, greedy);
    }

    public static String getExtension(String fname)
    {
        return getExtension(fname, true);
    }

    // get the file name extension
    // given a name aa.bb.cc - if greedy is true we'll return bb.cc, and if false, just cc
    public static String getExtension(String fname, boolean greedy)
    {
        int indx = greedy ? fname.indexOf('.') : fname.lastIndexOf('.');
        if (indx >= 0) {
            return fname.substring(indx+1);
        }
        else {
            return "";
        }
    }
    
    /* -- requires Java7
    public static Path getDefaultPath(File ff)
    {
        return FileSystems.getDefault().getPath(ff.getAbsolutePath());
    }
    
    public static boolean createLink(File source, File target)
    {
        Path newLink = getDefaultPath(target);
        Path existingFile = getDefaultPath(source);
        try {
            Files.createLink(newLink, existingFile);
            return true;
        } catch (IOException x) {
            System.err.println(x);
        } catch (UnsupportedOperationException x) {
            // Some file systems do not
            // support adding an existing
            // file to a directory.
            System.err.println(x);
        }
        return false;
    }
    */

    //-------------------------------------------------------------------------
    /**
        Read a file, putting its contents into a String.
        
        @param filePath A string containing a platform-acceptable
        file path.
        
        @return The file contents, converted to a String, using
        the default locale.  Returns null if there are any
        problems.
    */
    public static String fileAsString (String filePath)
    {
        String retVal = null;

        if (filePath == null)
        {
            return null;
        }
        
        try
        {
            FileInputStream fin = new FileInputStream(filePath);
            return fileAsString(fin);
        }
        catch(Exception exc)
        {
            // just go ahead and return the null
        }

        return retVal;
    }

    //-------------------------------------------------------------------------
    /**
        Read a file, putting its contents into a String.
        
        @param dir Directory where filepath is rooted
        @param filepath The file we want to read
        
        @return The file contents, converted to a String, using
        the default locale.  Returns null if there are any
        problems.
    */
    public static String fileAsString (File dir, String filepath)
    {
        File theF = new File(dir, filepath);
        return fileAsString(theF);
    }

    //-------------------------------------------------------------------------
    /**
        Read a file, putting its contents into a String.
        
        @param infile The file we want to read
        
        @return The file contents, converted to a String, using
        the default locale.  Returns null if there are any
        problems.
    */
    public static String fileAsString (File infile)
    {
        String retVal = null;

        try
        {
            FileInputStream fin = new FileInputStream(infile);
            return fileAsString(fin);
        }
        catch(Exception exc)
        {
            // just go ahead and return the null
        }

        return retVal;
    }

    //-------------------------------------------------------------------------
    /**
        Read a file, putting its contents into a String.
        
        @param fin An input stream.  This method closes the stream
        
        @return The file contents, converted to a String, using
        the default locale.  Returns null if there are any
        problems.
    */
    public static String fileAsString (FileInputStream fin)
    {
        String retVal = null;

        try
        {
            int avail = fin.available();
            byte[] bb = new byte[avail];
            fin.read(bb);
            fin.close();

            retVal = new String(bb);
        }
        catch(Exception exc)
        {
            // just go ahead and return the null
        }

        return retVal;
    }

    //-------------------------------------------------------------------------
    /**
        Write a String to a file.
        
        @param filePath the path of the file we are creating or
        overwriting.
        @param str the string we are writing out
        @throws FileNotFoundException if the file can't be opened
        @throws IOException if there is trouble writing
    */
    public static void writeStringToFile (String filePath, String str)
      throws FileNotFoundException, IOException
    {
        File outf = new File(filePath);
        writeStringToFile(outf, str);
        
        return;
    }
    
    //-------------------------------------------------------------------------
    /**
        Write a String to a file.
        
        @param outf the file we are creating or overwriting.
        @param str the string we are writing out
        @throws FileNotFoundException if the file can't be opened
        @throws IOException if there is trouble writing
    */
    public static void writeStringToFile (File outf, String str)
      throws FileNotFoundException, IOException
    {
        if (outf == null || str == null)
        {
            return;
        }
        
        // make sure the destination directory exists
        File dirf = outf.getParentFile();
        if (dirf != null && !dirf.exists()) {
            // parent directory doesn't exist, create it
            dirf.mkdirs();
        }

        FileOutputStream fout = new FileOutputStream(outf);
        byte[] bb = str.getBytes();
        fout.write(bb);
        fout.close();
        
        return;
    }
    
    //-------------------------------------------------------------------------
    /**
        Convert a file path to a URL string
        @param path filepath to convert
        @return the proper URL
    */
    public static String filepathToURLString (String path)
    {
        try {
            URI furi = (new File(path)).toURI();
            URL xx = furi.toURL();
            return xx.toString();
        }
        catch (MalformedURLException mue) {
            return path;
        }
    }

    //-------------------------------------------------------------------------
    /**
        Delete every file within a directory.
        If there are any problems, the routine silently returns.
        
        @param dir A directory, which exists
        @param doTree If true, then we will clear all subdirectories
    */
    public static void clearDirectory (File dir, boolean doTree)
    {
        // get a list of the files
        String[] fileList = dir.list();
        if (fileList == null)
        { // this isn't a directory
            return;
        }
        
        for (int ii=0; ii<fileList.length; ii++)
        {
            File subFile = new File(dir, fileList[ii]);
            if (subFile.isDirectory())
            {
                if (doTree)
                {
                    clearDirectory(subFile, true);
                    subFile.delete();
                }
            }
            else
            {
                subFile.delete();
            }
        }
        
        return;
    }
    
    //-------------------------------------------------------------------------
    /**
        Delete all the files that are marked in a FileInfo tree
    */
    public static void deleteMarkedFiles (FileInfo fileTree)
    {
        // we need to do a depth-first delete so that we clear out directories
        // before we delete them
        FileInfoIterator iter = new FileInfoIterator(fileTree, true);
        while (iter.hasNext()) {
            FileInfo fi = iter.nextFileInfo();
            if (!fi.isMarked()) {
                continue;
            }

            Owt.out.println("Deleting " + fi.getPath());
            fi.getFile().delete();
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Copy files marked in one FileInfo tree over to another tree. <br>
        We are assuming that the two trees have the same directory structure,
        starting from the root.
    */
    public static void copyMarkedFiles (FileInfo sourceTree, FileInfo destTree)
        throws IOException
    {
        // we need to do a depth-last copy so that we create directories
        // before we use them
        FileInfoIterator iter = new FileInfoIterator(sourceTree, false);
        File destRoot = destTree.getFile();
        File sourceRoot = sourceTree.getFile();
        while (iter.hasNext()) {
            FileInfo fi = iter.nextFileInfo();
            if (!fi.isMarked()) {
                continue;
            }

            Owt.out.println("Copying " + fi.getPath());
            if (fi.isDirectory()) {
                // "copy" means create the directory
                File dir = new File(destRoot, fi.getRelativePath());
                dir.mkdirs();
            }
            else {
                // simply copy the file
                copyFile(sourceRoot, destRoot, fi.getRelativePath());
            }
        }

        return;
    }

    //-------------------------------------------------------------------------
    /**
        Copy a file from one directory to another. <br>
        This copies a file, without renaming it.  The source file is not deleted.
        @param sourceDir The directory where the file starts
        @param destDir The directory where we make a copy
        @param filename The file's name

        @throws java.io.IOException on IO error
    */
    public static void copyFile (File sourceDir, File destDir, String filename)
        throws java.io.IOException
    {
        copyFile(new File(sourceDir, filename), new File(destDir, filename));
    }

    //-------------------------------------------------------------------------
    /**
        Copy a file within a directory. <br>
        This copies a file.  The source file is not deleted.
        @param dir The directory where the files are
        @param srcFile The source file's name
        @param destFile The destination file's name

        @throws java.io.IOException on IO error
    */
    public static void copyFile (File dir, String srcFile, String destFile)
        throws java.io.IOException
    {
        copyFile(new File(dir, srcFile), new File(dir, destFile));
    }

    //-------------------------------------------------------------------------
    /**
        Copy a file. <br>
        This copies a file.  The source file is not deleted.
        @param sourceDir The directory where the file starts
        @param destDir The directory where we make a copy
        @param srcFile The source file's name
        @param destFile The destination file's name

        @throws java.io.IOException on IO error
    */
    public static void copyFile (File sourceDir, File destDir,
                                 String srcFile, String destFile)
        throws java.io.IOException
    {
        File inf = new File(sourceDir, srcFile);
        File outf = new File(destDir, destFile);

        copyFile(inf, outf);
    }
    
    public static void copyFile(File sourceFile, File destFile)
        throws java.io.IOException
    {
        // open the files, copy them by buffer
        FileInputStream fis = new FileInputStream(sourceFile);
        FileOutputStream fos = new FileOutputStream(destFile);
        copyStream(fis, fos);
        fis.close();
        fos.close();
        
        return;
    }
    
    public static void copyStream(InputStream input, OutputStream output) throws IOException
    {
    	byte[] buffer = new byte[BUFFERSIZE];
    	int nRead = 0;
    	//int total = 0;
    	while (true) {
            nRead = input.read(buffer, 0, BUFFERSIZE);
            if (nRead < 0) {
                break;
            }
            output.write(buffer, 0, nRead);
            //total += nRead;
        }
    }

    //-------------------------------------------------------------------------
    /**
        Get a File version of the current working directory
        @return a File which represents the working directory
    */
    public static File workingDirectory ()
    {
        return new File(".");
    }

    //-------------------------------------------------------------------------
    /**
        Calculate a file's relative path. <br>
        @param root The directory from which we're calculating the relative path
        @param theFile The file whose path we're calculating
        @return The relative path, or an erroneous string
    */
    public static String getRelativePath (File root, File theFile)
        throws IOException
    {
        String dirPath = root.getCanonicalPath();
        String filePath = theFile.getCanonicalPath();
        return getRelativePath(dirPath, filePath, File.separatorChar);
    }

    //-------------------------------------------------------------------------
    /**
        Calculate a file's relative path, more generally
        This method can be used on filepaths, or URLs or whatever
        @param dirPath The directory path from which we're calculating the relative path
        @param filePath The file path whose path we're calculating
        @param sepchar The separator character to use
        @return The relative path, or an erroneous string
    */
    public static String getRelativePath (String dirPath, String filePath, char sepchar)
    {
        // if the dirPath ends in the separator character, that is redundant - remove it
        if (dirPath.charAt(dirPath.length()-1) == sepchar) {
            dirPath = dirPath.substring(0, dirPath.length()-1);
        }

        // break the paths up into pieces
        String[] dirElements = StringUtil.tokenize(dirPath, sepchar);
        String[] fileElements = StringUtil.tokenize(filePath, sepchar);

        // how many elements match?
        // we ignore case because of stupid Windows case insensitivity
        int nm = 0;
        for (nm=0; nm<dirElements.length; nm++) {
            if (!dirElements[nm].equalsIgnoreCase(fileElements[nm])) {
                break;
            }
        }
        // nm now equals the number of matching elements
        // the very first token must match, or else we have no common base
        if (nm < 1) {
            Owt.out.println("getRelativePath? " + filePath + " : " +
                               dirPath);
            return("?path?");
        }

        // the number of "up" parts of the path is the number of elements
        // of dirElements that did not match
        String path = "";
        for (int ii=0; ii<dirElements.length-nm; ii++) {
            path += "..";
            path += sepchar;
        }

        // then the rest of the path are the unmatching part of fileElements
        for (int ii=nm; ii<fileElements.length-1; ii++) {
            path += fileElements[ii];
            path += sepchar;
        }
        path += fileElements[fileElements.length-1]; // the file name

        return path;
    }

    //-------------------------------------------------------------------------
    /**
        Find a file on the java classpath. <br>
        @param cpath A file path that looks like a classpath specification, namely
        a.b.zebra.txt
        @return The actual OS filepath, or null if we can't find it
    */
    public static String findFileOnClasspath (String cpath)
    {
        /*
            The name of the file should be x.y.z.filename.ext
            which we need to convert to \x\y\z\filename.ext, and look for on the
            java classpath
        */

        String cpath2 = classpathToFilepath(cpath);
        
        // get the java classpath
        String classpath = System.getProperty("java.class.path");
        StringTokenizer stok = new StringTokenizer(classpath, File.pathSeparator);
        while (stok.hasMoreTokens()) {
            String jpath = stok.nextToken();
            String testPath = jpath + cpath2;
            File testFile = new File(testPath);
            if (testFile.exists()) {
                return testPath;
            }
        }

        // couldn't find it
        return null;
    }

    //-------------------------------------------------------------------------
    /**
        Take a classpath style path and convert to filepath style
    */
    private static String classpathToFilepath (String cpath)
    {
        // change the . to directory separators, except for the last one
        int extIndex = cpath.lastIndexOf('.');
        if (extIndex >= 0) {
            cpath = cpath.substring(0,extIndex).replace('.', File.separatorChar) +
                    cpath.substring(extIndex);
        }

        String cpath2 = File.separator + cpath;
        return cpath2;
    }

    //-------------------------------------------------------------------------
    /**
        Calculate a path on the java classpath.
        This method will take a file path in the form of a classpath
        specification (like a.b.zebra.txt) and construct a full path that will
        place the file on the classpath.  It uses the first element of the
        classpath, or "." if the classpath is missing.
        @param cpath A file path that looks like a classpath specification
        @return The actual OS filepath
    */
    public static String placeFileOnClasspath (String cpath)
    {
        // get the portion of the java classpath that we want
        String classpath = System.getProperty("java.class.path");
        if (classpath == null) {
            classpath = ".";
        }
        int ind = classpath.indexOf(File.pathSeparator);
        if (ind > 0) {
            classpath = classpath.substring(0, ind); // take first element
        }
        File cpf = new File(classpath);

        // get the file's path under the classpath
        String cpath2 = classpathToFilepath(cpath);

        // put them together
        File ff = new File(cpf, cpath2);

        try {
            return ff.getCanonicalPath();
        }
        catch (IOException ioe) {
            Owt.out.println("Can't create path for " + cpath + " in placeFileOnClasspath");
            return null;
        }
    }

    //-------------------------------------------------------------------------
    /**
        Trivial test driver. <br>
        Only tests a few things.
    */
    public static void main (String[] args)
    {
        // test working directory
        Owt.out.println("Test workingDirectory");
        File dir = workingDirectory();
        // we don't print out the working directory because it changes depending on
        // where the test is run from, and therefore messes up our test scripts
        //Owt.out.println("cwd is " + dir.getAbsolutePath());
        Owt.out.println("isdir is " + dir.isDirectory());    
        Owt.out.println("Done with workingDirectory");

        // test findFileOnClasspath
        Owt.out.println("Test findFileOnClasspath");
        String filepath = findFileOnClasspath("templates.codegen.classbody.tml");
        Owt.out.println("Found class template at: " + filepath);
        Owt.out.println("Done with findFileOnClasspath");

        // test getRelativePath
        try {
            File fdir = new File("c:\\pkg\\ksw\\kwutil\\");
            File ff = new File("c:\\pkg\\ksw\\kwutil\\FileUtil.java");
            Owt.out.println("rpath to FileUtil is " + getRelativePath(fdir, ff));
            ff = new File("c:\\pkg\\ksw\\kwutil\\template\\Template.java");
            Owt.out.println("rpath to Template is " + getRelativePath(fdir, ff));
            ff = new File("c:\\pkg\\ksw\\photoalbum\\Photoalbum.java");
            Owt.out.println("rpath to Photoalbum is " + getRelativePath(fdir, ff));
            ff = new File("c:\\zkevins\\writing\\journals\\2001\\j1101.txt");
            Owt.out.println("rpath to j1101 is " + getRelativePath(fdir, ff));
        }
        catch (IOException ioe) {
            Owt.out.println("Exception in test: " + ioe);
        }
    }
}
