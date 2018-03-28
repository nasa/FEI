/*
 * Created on Feb 17, 2005
 */
package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Properties;


/**
 * <b>Purpose:</b>
 * Default implementation for some methods of the FileEventHandler.  
 * Subclasses must implement the <code>handleEvent()</code> and
 * <code>handleError()</code> methods.
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
 * 07/21/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: AbstractFileEventHandler.java,v 1.5 2011/06/01 00:03:05 ntt Exp $
 *
 */

public abstract class AbstractFileEventHandler implements FileEventHandler
{   
    
    protected FileEventHandlerInfo _metadata;
    
    /** Name of this handler */
    protected String        _name    = null;
    
    /** Id of this handler */
    protected String        _id = null;
    
    /** Class name of this handler */
    protected String        _classname   = null;
    
    /** Properties for this handler */
    protected Properties    _properties;
    
    /** Context reference */
    protected FileEventsContext _context;
    
    //---------------------------------------------------------------------
    
//    /**
//     * Handle method performs operation of the handler.  Implementors
//     * can use the event type as a filter so that handler is only
//     * invoked for specific event types.
//     * @param taskType Type of task
//     * @param results List of Result objects 
//     */
//    
//    public abstract void eventOccurred(FileResultEvent event);
//    
//    
//    public abstract void errorOccurred(FileResultError error);
//    
    //---------------------------------------------------------------------
    
    public void initialize(FileEventsContext context, FileEventHandlerInfo metadata) 
                                                            throws HandlerException
    {
        this._metadata = metadata;
        this._context  = context;
        
        this._name = metadata.getName();
        this._classname = metadata.getImplementation();
        this._id = metadata.getId();
        
        this._properties = new Properties();
        Properties infoProps = metadata.getProperties();
        this._properties.putAll(infoProps);        
    }

    //---------------------------------------------------------------------

    /**
     * Returns the name of this handler 
     * @return Handler name
     */
    
    public String getName()
    {
        return this._name;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the id of this handler 
     * @return Handler id
     */
    
    public String getId()
    {
        return this._id;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the description of this handler 
     * @return Handler description
     */
    
    public String getClassname()
    {
        return this._classname;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns the description of this handler 
     * @return Handler description
     */
    
    public String getDescription()
    {
        return this._metadata.getDescription();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the version of this handler 
     * @return Handler version
     */
    
    public String getVersion()
    {
        return this._metadata.getVersion();
    }
    
    //---------------------------------------------------------------------
    
    public String getProperty(String propertyName)
    {
        return this._properties.getProperty(propertyName);
    }

    
    //---------------------------------------------------------------------
    
    public String toString()
    {
        return this.getClass().getName()+"::"+ this._name+
               "(id "+this._id+") "+super.toString();
    }
    
    //---------------------------------------------------------------------
    
    public void close()
    {
        this._properties.clear();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Implementation of Comparable interface, uses natural ordering
     * of the handler ids.
     */
    
    public int compareTo(Object o)
    {
        if (!(o instanceof FileEventHandler))
            return 0;
        
        FileEventHandler other = (FileEventHandler) o;
        String myId = this.getId();
        String otherId = other.getId();
        
        return myId.compareTo(otherId);
    }
    
    //---------------------------------------------------------------------
    
}
