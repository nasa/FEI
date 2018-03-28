package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import jpl.mipl.mdms.FileService.io.BoundedBufferedReader;
import jpl.mipl.mdms.utils.logging.Logger;

/**
* Loads a file handler manifest list from various locations.
* 1) In local output directory, hidden file .komodo.filehandler.manifest
* 2) Examine property 'komodo.filehandler.manifest' for location
* 3) Assume highest filter level.
*  
*   <PRE>
*   Copyright 2011, California Institute of Technology.
*   ALL RIGHTS RESERVED.
*   U.S. Government Sponsorship acknowledge. 2011.
*   </PRE>
*
* <PRE>
* ============================================================================
* <B>Modification History :</B>
* ----------------------
*
* <B>Date              Who              What</B>
* ----------------------------------------------------------------------------
* 05/31/2011        Nick             Initial Release
* ============================================================================
* </PRE>
*
* @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
* @version $Id: FileEventHandlerFiltersLoader.java,v 1.4 2013/10/14 17:24:27 ntt Exp $
*
*/

public class FileEventHandlerFiltersLoader
{
    protected FileEventsContext context; 
    FileEventHandlerFilters filters;
    
    //Loggers
    private Logger _logger = Logger.getLogger(FileEventHandlerFiltersLoader.class.getName());
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param context Instance of File events context
     */
    public FileEventHandlerFiltersLoader(FileEventsContext context)
    {
        this.context = context;
        
        load();        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Performs loading of filter
     */
    protected void load()
    {        
        this.filters = new FileEventHandlerFilters(); 
        
        //get stream
        InputStream is = getLocalStream();
        
        if (is == null)
        {
            is = getPropertyStream();            
        }
        
        //-------------------------
        
        //if a stream was found, parse it
        
        if (is != null)
        {
            LineNumberReader reader = null;
            
        
            //Security code review prompted this change to prevent DOS attack (nttoole 08.27.2013)
            //reader = new LineNumberReader(new InputStreamReader(is));
            reader = new LineNumberReader(new BoundedBufferedReader(
                                          new InputStreamReader(is)));
            
            String line = null;
            
            try {
                while ((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    if (!line.equals(""))
                        this.filters.addFilterId(line);                    
                }
                
                reader.close();
                
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
                try {
                    reader.close();
                } catch (IOException ioEx2) {
                    ioEx2.printStackTrace();
                }
            }
        }        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Checks output director (via context) and for file following
     * particular name.
     * @see Constants.HANDLER_FILTERS_LIST_NAME
     * @return InputStream to local file, null if file does not exist
     * or is unreadable.
     */
    protected InputStream getLocalStream()
    {
        InputStream is = null;
        
        //first check for local file
        String output = this.context.getDirectory();
        String localFileStr = output + File.separator + Constants.HANDLER_FILTERS_LIST_NAME;
        File localFile = new File(localFileStr);
        
        if (localFile.isFile() && localFile.canRead())
        {
            _logger.trace("Handler filter manifest file in output directory "+
                          localFile.getAbsolutePath()+". Attempting to load.");
                        
            try {
                is = new FileInputStream(localFile);               
            } catch (FileNotFoundException fnfEx) {
                is = null;
            }
            
            if (is != null)
                _logger.trace("Local handler filter manifest file loaded from "+localFile.getAbsolutePath());
        }
        
        return is;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Checks system property, if defined, attempts to load input stream from
     * that resource.
     * @see Constants.HANDLER_FILTERS_LIST_PROPERTY
     * @return InputStream to resource, null if value does not exist
     * or is unreadable.
     */
    
    protected InputStream getPropertyStream()
    {
        InputStream is = null;
        
        String value = System.getProperty(Constants.HANDLER_FILTERS_LIST_PROPERTY);
        
        if (value != null)
        {
            _logger.trace("Handler filter manifest file specified by property at "+value+". Attempting to load.");
            
            
            //check to see if its a local file on the file system
            File file = new File(value);
            if (file.canRead())
            {
                try {
                    is = new FileInputStream(file);
                } catch (IOException ioEx) {
                    is = null;
                }
            }
            else //otherwise assume its a resource on classpath
            {
                is = this.getClass().getClassLoader().getResourceAsStream(value);                   
            }
            
            if (is != null)
                _logger.trace("Handler filter manifest file loaded from "+file.getAbsolutePath());
        }
                
        return is;
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Returns instance of FileEventHandlerFilters
     */
    
    public FileEventHandlerFilters getFilters()
    {
        return this.filters;
    }

   
    //---------------------------------------------------------------------
}
