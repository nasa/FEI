/*
 * Created on Feb 16, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler.SubscriptionHandler;

/**
 * <b>Purpose:</b>
 * Table of SubscriptionHandlers.  Key is the eventType to which handler is 
 * associated.
 *
 *   <PRE>
 *   Copyright 2005, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2005.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 02/16/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionHandlerTable.java,v 1.3 2005/03/31 22:02:06 ntt Exp $
 *
 */

public class SubscriptionHandlerTable
{
    private final String __classname = this.getClass().getName();
    
    /**
     * Maps Integer wrapper of eventType to List of SubscriptionHandler's
     */
    protected final Map _lookupTable;
    
    /**
     * XXX Not sure if you need this, delete if unused
     */
    protected Client _client;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     */
    
    public SubscriptionHandlerTable()
    {
        this._lookupTable = new Hashtable();
        this._client      = null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that retrieves List of handlers based on 
     * eventType.  If list is not found, an empty list is created, 
     * added to the table, and returned,
     * @param eventType Event type
     * @return List of handlers associated with eventType
     */
    
    protected List getListForType(int eventType)
    {
        Integer type = new Integer(eventType);
        if (!this._lookupTable.containsKey(type))
        {
            this._lookupTable.put(type, new ArrayList());
        }
        List handlers = (List) this._lookupTable.get(type);
        return handlers;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a handler for eventType.
     * @param eventType Id of event handler handles
     * @param handler Implmentation of the SubscriptionHandler
     */
    
    public void addHandler(int eventType, SubscriptionHandler handler)
    {
        List handlers = getListForType(eventType);
        if (!handlers.contains(handler))
        {
            handlers.add(handler);
            handler.setClient(this._client);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a list of handlers for eventType.
     * @param eventType Id of event handler handles
     * @param handlers List of SubscriptionHandler instances
     */
    
    public void addHandlers(int eventType, List handlers)
    {
        
        SubscriptionHandler handler = null;
        int handlerCount = handlers.size();
        List handlerList = getListForType(eventType);

        for (int i = 0; i < handlerCount; ++i)
        {
            handler = (SubscriptionHandler) handlers.get(i);
            if (handler == null)
                continue;
            
            if (!handlerList.contains(handler))
            {
                handlerList.add(handler);
                handler.setClient(this._client);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Removes a handler for eventType.
     * @param eventType Id of event handler handles
     * @param handler Implmentation of the SubscriptionHandler
     */
    
    public void removeHandler(int eventType, SubscriptionHandler handler)
    {
        List handlers = getListForType(eventType);
        if (handlers.contains(handler))
        {
            handlers.remove(handler);
            
            //if this handler is not registered anywhere else,
            //then remove the ref to client
            if (getEventTypes(handler).length == 0)
                handler.setClient(null);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of registered event types.
     * @return Array of integers
     */
    
    public int[] getEventTypes()
    {
        int[] types = new int[0];
        List typeList = new ArrayList();
        typeList.addAll(this._lookupTable.keySet());
        
        int numTypes = typeList.size();
        types = new int[numTypes];
        
        for (int i = 0; i < numTypes; ++i)
        {
            types[i] = ((Integer) typeList.get(i)).intValue();
        }
        
        return types;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of registered event types that are linked to the
     * handler argument.
     * @param handler Instance of SubscriptionHandler that may exist in 
     *        the lookup table.
     * @return Array of integers matching event types that contain handler
     */
    
    public int[] getEventTypes(SubscriptionHandler handler)
    {
        int[] types = new int[0];
        
        if (handler != null)
        {
            int[] typeArray = getEventTypes();
            List typeList = new ArrayList();
            for (int i = 0; i < typeArray.length; ++i)
            {
                List handlers = getListForType(typeArray[i]);
                if (handlers.contains(handler))
                    typeList.add(new Integer(typeArray[i]));
            }
            
            int numTypes = typeList.size();
            types        = new int[numTypes];
            
            for (int i = 0; i < numTypes; ++i)
            {
                types[i] = ((Integer) typeList.get(i)).intValue();
            }
        }
        
        return types;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns all handlers registered for a particular event type.
     * @param eventType Event type
     * @return Array of SubscriptionHandler
     */
    
    public SubscriptionHandler[] getHandlers(int eventType)
    {
        List handlerList = getListForType(eventType);
        int numHandlers = handlerList.size();
        SubscriptionHandler[] handlers = new SubscriptionHandler[numHandlers];
        for (int i = 0; i < numHandlers; ++i)
        {
            handlers[i] = (SubscriptionHandler) handlerList.get(i);
        }
        return handlers;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Resets the table by removing all entries.
     */
    
    public void reset()
    {
        this._lookupTable.clear();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Removes all handlers from the table for a given eventType.
     * @param eventType Event type
     */
    
    public void resetType(int eventType)
    {
        List handlerList = getListForType(eventType);
        handlerList.clear();
    }
    
    //---------------------------------------------------------------------  
    
    /**
     * Convenience method that dispatches handleEvent method to all
     * handlers associated with the supplied event type.
     * @param eventType Event type, used to lookup handlers
     * @param taskType Task type, TASK_{SUBSCRIPTION,NOTIFICATION}
     * @param results List of Result objects
     */
    
    public void handleEvent(int eventType, int taskType, List results) 
                                                            throws Exception
    {
        List handlerList = getListForType(eventType);
        for (int i = 0; i < handlerList.size(); ++i)
        {
           ((SubscriptionHandler) handlerList.get(i)).handleEvent(
                                               taskType, results);
        }
    }

    //---------------------------------------------------------------------  
    
    /**
     * Convenience method that dispatches setClient method to all
     * handlers.
     * @param client New Client instance
     */
    
    public void setClient(Client client) 
    {
        if (client == this._client)
            return;
        
        this._client = client;
        
        int[] types = getEventTypes();
        SubscriptionHandler handler;
        for (int i = 0; i < types.length; ++i)
        {
            List handlerList = getListForType(types[i]);
            int numHandlers = handlerList.size();
            for (int j = 0; j < numHandlers; ++j)
            {
                handler = (SubscriptionHandler) handlerList.get(j);
                handler.setClient(this._client);
            }
        }
    }
    
    //---------------------------------------------------------------------
}
