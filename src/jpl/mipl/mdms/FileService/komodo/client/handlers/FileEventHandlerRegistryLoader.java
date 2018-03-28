package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jpl.mipl.mdms.FileService.komodo.client.UtilClient;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * Loader scans classpath for file event handler plugin descriptors (as
 * an XML file) and attempts to load into handler info instances.
 * 
 * Classpath location of individual descriptors is: 
 * /META-INF/services/komodo.filehandler.xml,
 * such that no two handlers are packaged within the same Jar file. 
 * 
 *   <PRE>
 *   Copyright 2008, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2008.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 08/15/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileEventHandlerRegistryLoader.java,v 1.8 2011/05/31 22:43:50 ntt Exp $
 *
 */

public class FileEventHandlerRegistryLoader
{
    //public static final String FILENAME_EXTENSIONS_INFO = "/META-INF/services/komodo/handler.xml";
    //public static final String FILENAME_EXTENSIONS_INFO = "jpl/mipl/mdms/FileService/komodo/client/handlers/handler.xml";

    
    //public static final String IMPL_PROP_NAME = "jpl.mipl.mdms.FileService.komodo.FileEventHandler";
    
    protected Set<String> implNameSet;
    protected List<FileEventHandlerInfo> handlerInfoList;
    protected boolean loaded;
    
    //Loggers
    private Logger _logger = Logger.getLogger(FileEventHandlerRegistryLoader.class.getName());
    
    //---------------------------------------------------------------------
    
    public FileEventHandlerRegistryLoader()    
    {
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        this.implNameSet = new HashSet<String>();
        this.handlerInfoList = new ArrayList<FileEventHandlerInfo>();
        this.loaded = false;
    }
    

    //---------------------------------------------------------------------
    
    protected void loadHandlers()
    {
        loadHandlersInfo();  
    }
    
    //---------------------------------------------------------------------
    
    protected void loadHandlersInfo()
    {
        List<URL> urlList = new ArrayList<URL>();
        
        //-------------------------
        //Look for external extension info

        try {
            Enumeration<URL> thirdPartyURLs;
            thirdPartyURLs = FileEventHandlerRegistryLoader.
                                         class.getClassLoader().getResources(
                                         Constants.HANDLER_DESCRIPTOR_PATH);
            
            while (thirdPartyURLs.hasMoreElements())
            {
                urlList.add(thirdPartyURLs.nextElement());
            }
            
        } catch (IOException ioEx) {    
            _logger.error("Exception occurred while collecting URLs of handler" +
            		      " descriptors: "+ioEx.getMessage());
            _logger.trace(null, ioEx);
        }
        
        //-------------------------
        
        Iterator<URL> urlIt = urlList.iterator();
        while (urlIt.hasNext())
        {
            loadHandlerInfoFromUrl(urlIt.next());
        }
        
        //-------------------------           
    }
    
    //---------------------------------------------------------------------
  
    protected void loadHandlerInfoFromUrl(URL url)
    {
        this._logger.trace("Loading file event handler from URL "+url.toString());
        FileEventHandlerInfoLoader infoLoader = new FileEventHandlerInfoLoader(url);
        FileEventHandlerInfo info = infoLoader.getInfo();
        if (info != null)
            this.handlerInfoList.add(info);       
    }
    
    //---------------------------------------------------------------------
    
    
    public List<FileEventHandlerInfo> getHandlerInfos()
    {       
        synchronized(this)
        {
            if (!this.loaded)
            {
                loadHandlers();
                this.loaded = true;
            }
        }
        
        List<FileEventHandlerInfo> classes = new ArrayList<FileEventHandlerInfo>();
        classes.addAll(this.handlerInfoList);
        return classes;
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
}
