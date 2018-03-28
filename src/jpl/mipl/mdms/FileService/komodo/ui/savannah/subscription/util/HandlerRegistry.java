package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler.SubscriptionHandler;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Registry for handler info objects.  Referenced by the PluginRegistry,
 * extracts handler information from extensions and serves as a way
 * of retrieving handlers for a given event type.
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
 * 03/09/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: HandlerRegistry.java,v 1.3 2005/04/13 21:37:28 ntt Exp $
 *
 */

public class HandlerRegistry
{
    //---------------------------------------------------------------------
    
    public static final String POINT_HANDLER_SET = 
        "jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handlers";
    
    /** Map from event names to Lists of handler info objects */
    protected static Map eventHandlers;

    protected static boolean initiated = false;
    
    private static Logger logger = Logger.getLogger(HandlerRegistry.class.getName());

    //---------------------------------------------------------------------
    
    protected static void checkInit() throws Exception
    {
        if (!initiated)
            init();
    }
    
    //---------------------------------------------------------------------
    
    protected static void init() throws Exception
    {
        eventHandlers = new Hashtable();
        ExtensionInfo[] eInfos = PluginRegistry.getExtensions(
                                                POINT_HANDLER_SET);
        loadHandlers(eInfos);
        initiated = true;
    }
    
    //---------------------------------------------------------------------
    
    protected static void loadHandlers(ExtensionInfo[] eInfos) 
                                                        throws Exception
    {
        Class clazz;
                
        //get class for extension point
        try {
            clazz = ExtensionFactory.getClassForPoint(POINT_HANDLER_SET);
        } catch (Exception ex) {
            logger.error("No class found for extension point '"
                         + POINT_HANDLER_SET + "'", ex);
            throw ex;
        }
        
        
        //for each extension, verify if handler, if so add to handler map
        try {
            for (int i = 0; i < eInfos.length; ++i)
            {
                ConfigurationInfo[] cInfos = eInfos[i].getExtensions();
                for (int j = 0; j < cInfos.length; ++j)
                {
                    ConfigurationInfo cInfo = cInfos[j];
                    
                    //if not handler set, ignore
                    if (!clazz.isInstance(cInfo))
                        continue;
                    
                    HandlerSetInfo hsetInfo = (HandlerSetInfo) cInfo;
                    HandlerInfo[] hInfos = hsetInfo.getHandlers();
                    
                    for (int k = 0; k < hInfos.length; ++k)
                    {
                        addToEventType(hsetInfo.getEventType(), hInfos[k]);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Handler registry could not load handlers");
            logger.debug(ex.getMessage(), ex);
            throw ex;
        }
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Adds handler info object to list of handlers associated with
     * the event type if not already present.
     * @param eventType Name of event type
     * @param handler Handler info object to be added
     */
    
    protected static void addToEventType(String eventType, 
                                         HandlerInfo handler)   
    {
        List list = (List) eventHandlers.get(eventType);
        if (list == null)
        {
            list = new ArrayList();
            eventHandlers.put(eventType, list);
        }
        
        if (!list.contains(handler))
        {
            logger.debug("Adding handler '"+handler.getId()+"' for event "
                         + "type '"+eventType+"'");
            list.add(handler);
        }
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns array of handler info objects associated with event
     * type.
     * @param eventType Name of event type
     * @return Array of HandlerInfo
     */
    
    protected static HandlerInfo[] getForEventType(String eventType)
    {
        HandlerInfo[] handlers;
        
        List list = (List) eventHandlers.get(eventType);
        if (list != null)
        {
            int numHandlers = list.size();
            handlers = new HandlerInfo[numHandlers];
            for (int i = 0; i < numHandlers; ++i)
                handlers[i] = (HandlerInfo) list.get(i);
        }
        else
        {
            handlers = new HandlerInfo[0];
        }

        return handlers;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of event type names that are declared from
     * the plugin registry with handlers.
     * @return Array of event type names 
     */
    
    public static String[] getEventTypes() throws Exception
    {
        checkInit();
        
        String[] types;
        
        List list = new ArrayList(eventHandlers.keySet());
        int numTypes = list.size();
        types = new String[numTypes];
        
        for(int i = 0; i < numTypes; ++i)
        {
            types[i] = (String) list.get(i);
        }
        
        return types;
    }
    
    //---------------------------------------------------------------------
 
    public static HandlerInfo[] getHandlers(String eventType) throws Exception
    {
        checkInit();
        
        HandlerInfo[] handlers;
        handlers = getForEventType(eventType);
        return handlers;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Loads and initializes an instance of SubscriptionHanlder using
     * the handler info parameter.
     * @param hInfo handler info instance describing handler
     * @return Initialized instantiation of handler if successful,
     *         null otherwise
     * @throws Exception if error occurs
     */
    public static SubscriptionHandler loadHandler(HandlerInfo hInfo) 
                                                  throws Exception
    {
        checkInit();
        
        SubscriptionHandler handler = null;
        String classname = hInfo.getClassname();
        PluginInfo pInfo = PluginInfo.getPluginInfo(hInfo);
        File[] jars = PluginRegistry.getLibraries(pInfo);
        
        Object obj = PluginRegistry.instantiateClass(jars, classname);
        if (obj instanceof SubscriptionHandler)
        {
            handler = (SubscriptionHandler) obj;
            handler.initialize(hInfo);
        }
        
        return handler;
    }
    
    //---------------------------------------------------------------------
    
}
