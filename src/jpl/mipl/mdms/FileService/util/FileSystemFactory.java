package jpl.mipl.mdms.FileService.util;


/**
 * <b>Purpose: </b> Factory that creates new instances of FileSystem 
 * interface based on the current environment.
 * If the native platform can be determined and is supported, then the 
 * returned implementation may utilize native techniques for behavior. 
 * 
 *    <PRE>
 *    Copyright 2008, California Institute of Technology. 
 *    ALL RIGHTS RESERVED. 
 *    U.S. Government Sponsorship acknowledge. 2008.
 *    </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History : </B> 
 * ----------------------
 * 
 * <B>Date              Who          What </B>
 * ----------------------------------------------------------------------------
 * 06/24/2008        Nick          Initial release
 * ============================================================================
 * </PRE>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileSystemFactory.java,v 1.2 2008/06/26 00:09:21 ntt Exp $
 *  
 */

public class FileSystemFactory
{
    protected String osName;

    //---------------------------------------------------------------------
    
    /**
     * Constructor
     */
    
    public FileSystemFactory()
    {    
        this.osName = System.getProperty("os.name");
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns a newly instantiated implementation of FileSystem
     * @return Instance of FileSystem
     */
    
    public FileSystem createFileSystem()
    {
        FileSystem fs = null;

        if (osName.equalsIgnoreCase("sunos") || 
            osName.equalsIgnoreCase("Linux") ||
            osName.equalsIgnoreCase("Mac OS X"))
           fs = new UnixFileSystem();
        else
           fs = new SimpleFileSystem();
        
        return fs;
    }

    //---------------------------------------------------------------------
}
