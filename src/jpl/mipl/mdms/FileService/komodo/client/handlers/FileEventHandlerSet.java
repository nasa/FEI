package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import jpl.mipl.mdms.FileService.komodo.api.Result;

/**
 * A collection of FileEventHandlers that forwards instances
 * of FileResultEvent and FileResultError events to all
 * members of the set.
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
 * @version $Id: FileEventHandlerSet.java,v 1.5 2011/06/01 00:03:05 ntt Exp $
 *
 */

public class FileEventHandlerSet
{
    final Object _lock = new Object();
    Set<FileEventHandler> handlers;
    
    Object error;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor 
     */
    
    public FileEventHandlerSet()
    {
        this.handlers = new TreeSet<FileEventHandler>();
        this.error    = null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Add a new handler instance. 
     * @param handler New handler to be added
     * @return True if handler was added to set, false otherwise 
     */
    
    public boolean addHandler(FileEventHandler handler)
    {
        synchronized(_lock)
        {
            return this.handlers.add(handler);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Iterates over handlers and invokes the <code>eventOccurred()</code>
     * method passing the parameters.
     * @param fileEvent Instance of FileResultEvent
     */
    
    public void eventOccurred(FileResultEvent fileEvent) throws HandlerException
    {
        synchronized(_lock)
        {
            Iterator<FileEventHandler> it = this.handlers.iterator();
            while (it.hasNext())
            {
               FileEventHandler handler = it.next();
                
                try {
                    handler.eventOccurred(fileEvent);
                } catch (HandlerException hEx) {
                    throw new HandlerException("Handler '" + handler.getId() +
                                  " threw exception: "+hEx.getMessage(), hEx);
                }
            }
        }        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Iterates over handlers and invokes the <code>errorOccurred()</code>
     * method passing the parameters.
     * @param fileError Instance of FileResultError
     */
    
    public void errorOccurred(FileResultError fileError) throws HandlerException
    {
        synchronized(_lock)
        {
            Iterator<FileEventHandler> it = this.handlers.iterator();
            while (it.hasNext())
            {
                FileEventHandler handler = it.next();
                
                try {
                    handler.errorOccurred(fileError);
                } catch (HandlerException hEx) {
                    throw new HandlerException("Handler '" + handler.getId() +
                                  " threw exception: "+hEx.getMessage(), hEx);
                }
            }
        }        
    }
    
    //---------------------------------------------------------------------
    
    
    /**
     * Iterates over handlers and invokes the <code>destroy()</code>
     * method passing the parameters, followed by clearing the set
     * of handlers.
     */
    
    public void destroy()
    {
        synchronized(_lock)
        {
            Iterator<FileEventHandler> it = this.handlers.iterator();
            while (it.hasNext())
                it.next().close();
        }        
        
        this.handlers.clear();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true of the error field was set, false otherwise
     */
    public boolean hasError()
    {
        return this.error != null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the error field, can be null
     */
    public Object getError()
    {
        return this.error;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the error field
     * @param error Error object, often a string
     */
    
    public void setError(Object error)
    {
        this.error = error;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of ids for the handlers in this set.
     * @return Handler ids
     */
    
    public String[] getHandlerIds()
    {
        int size = this.handlers.size();
        String[] array = new String[size];
        int index = 0;
        
        Iterator it = this.handlers.iterator();
        while (it.hasNext())
        {
            FileEventHandler h = (FileEventHandler) it.next();
            array[index] = h.getId();
            ++index;
        }
        return array;
    }
    
    //---------------------------------------------------------------------
}
