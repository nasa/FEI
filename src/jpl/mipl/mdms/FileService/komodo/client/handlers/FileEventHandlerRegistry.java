package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Registry maintains a collection of file event handler plugin metadata
 * 
 * The registry performs a discovery scan for available plugins
 * and collects metadata that will be used for instantiation.  
 * 
 * Registry will return a set of instantiated classes to caller.
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
 * @version $Id: FileEventHandlerRegistry.java,v 1.8 2011/05/31 22:43:50 ntt Exp $
 */

public class FileEventHandlerRegistry implements FileEventHandlerRegistryIF
{
    FileEventsContext _context;
    
    FileEventHandlerRegistryLoader _loader;
    FileEventHandlerBuilder _builder;
    Map<String, FileEventHandlerInfo> _handlerInfoMap;
    
    FileEventHandlerFilters _filterSet;
    
    //---------------------------------------------------------------------
    
    public FileEventHandlerRegistry(FileEventsContext context)
    {
        this._context = context;
        init();        
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        this._handlerInfoMap = new HashMap<String, FileEventHandlerInfo>();
        
        //construct internal registry loader and hander builder
        this._loader = new FileEventHandlerRegistryLoader();
        this._builder = new FileEventHandlerBuilder();
        
        //load all plugin metdata
        loadHandlerMetadata();     
        
        loadFilters();
    }
    
    //---------------------------------------------------------------------
    
    protected void loadHandlerMetadata()
    {
        List<FileEventHandlerInfo> handlerInfos = this._loader.getHandlerInfos();
        
        //iterate over returned metadata, adding each to map using
        //its id as the key
        Iterator<FileEventHandlerInfo> it = handlerInfos.iterator();
        while (it.hasNext())
        {
            FileEventHandlerInfo metadata = it.next();
            
            this._handlerInfoMap.put(metadata.getId(), metadata);
        }        
    }
    
    //---------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerRegistryIF#getFileEventHandlers()
     */
    
    public FileEventHandlerSet getFileEventHandlers()
    {
        FileEventHandlerSet set = new FileEventHandlerSet();
        
        //iterate over the handler metadata, create new instance, add to set
        Iterator<FileEventHandlerInfo> it = _handlerInfoMap.values().iterator();
        while (it.hasNext())
        {
            FileEventHandlerInfo metadata = it.next();
            
            
            //check with filter set to see if id passes (this
            //can be because it was specified, or set accepts
            //everything by default).  Otherwise, skip instance
            if (!_filterSet.doesIdPass(metadata.getId()))
                continue;
            
            FileEventHandler handler = instantiateHandler(this._context, 
                                                          metadata);
            
            if (handler != null)
            {
                set.addHandler(handler);      
            }
            else
            {
                String handlerId = metadata.getId();
                //TODO: add 'required' tag to metadata, and fail if true
                boolean exitOnFail = true;

                if (exitOnFail)
                {
                    String errMsg = "Could not instantiate handler '"+handlerId+"'";
                    set.setError(errMsg);
                }
            }
        }
        
        return set;        
    }
    
    //---------------------------------------------------------------------
    
    FileEventHandler instantiateHandler(FileEventsContext context,
                                        FileEventHandlerInfo metadata)
    {
        FileEventHandler handler = null;
    
        try {
            handler = this._builder.build(context, metadata);
        } catch (Exception ex) {    
            System.err.println("Unable to build file handler '"+metadata.getId()
                               +"': "+ex.getMessage());
            ex.printStackTrace();
            return null;
        }
        
        return handler;
    }

    //---------------------------------------------------------------------
    
    protected void loadFilters()
    {
        FileEventHandlerFiltersLoader filterSetLoader = 
                            new FileEventHandlerFiltersLoader(this._context);
        
        this._filterSet = filterSetLoader.getFilters();
    }
    
    //---------------------------------------------------------------------
    
}
