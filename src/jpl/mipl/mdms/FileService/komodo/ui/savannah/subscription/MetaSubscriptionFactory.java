package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.util.ArrayList;
import java.util.List;

import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler.SubscriptionHandler;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.HandlerInfo;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.HandlerRegistry;

/**
 * <b>Purpose:</b>
 * Factory class for the creation of subscription instances with the Savannah
 * supscription package.  Supply parameters, task type, and a new instance
 * of metasubscription will be created and configured with handlers.
 * 
 *   <PRE>
 *   Copyright 2004, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2004.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 09/24/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MetaSubscriptionFactory.java,v 1.6 2008/10/28 19:00:34 ntt Exp $
 *
 */

public class MetaSubscriptionFactory
{
    //---------------------------------------------------------------------
    
    /**
     * Creates an instance of Subscription based on parameter.
     * @param object Object used to determine instance class of subscription 
     */
    
    public static MetaSubscription createInstance(int taskType,
                                                  MetaParameters parameters) 
                                                  throws SessionException
    {
        MetaSubscription subscription;
        
        switch(taskType)
        {
            case SubscriptionConstants.TASK_NOTIFICATION:
                subscription = createNotification(parameters);
                break;
            case SubscriptionConstants.TASK_SUBSCRIPTION:
                subscription = createSubscription(parameters);
                break;      
            default:
                subscription = new NullMetaSubscription(parameters);
                break;
        }
        
        if (subscription != null)
            configureMetaSubscription(subscription, parameters);
        
        return subscription;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates an instance of Subscription based on parameter.
     * @param object Object used to determine instance class of subscription 
     */
    
    public static MetaSubscription createInstance(MetaParameters parameters) 
                                                  throws SessionException
    {
        MetaSubscription subscription;
        
        Integer taskType = (Integer) parameters.get(SubscriptionConstants.KEY_TASK_TYPE);
        int taskTypeId = taskType == null ? SubscriptionConstants.TASK_UNKNOWN :
                                            taskType.intValue();        
        switch(taskType)
        {
            case SubscriptionConstants.TASK_NOTIFICATION:
                subscription = createNotification(parameters);
                break;
            case SubscriptionConstants.TASK_SUBSCRIPTION:
                subscription = createSubscription(parameters);
                break;      
            default:
                subscription = new NullMetaSubscription(parameters);
                break;
        }
        
        if (subscription != null)
            configureMetaSubscription(subscription, parameters);
        
        return subscription;
    }
    
    //---------------------------------------------------------------------
    
    protected static MetaSubscription createNotification(
                                                MetaParameters parameters)
    {
        NotificationImpl ms = null;
        
        if (!(parameters instanceof NotificationParameters))
            return null;
        
        NotificationParameters np = (NotificationParameters) parameters;
        ms = new NotificationImpl(np);

        return ms;
    }
    
    //---------------------------------------------------------------------
    
    protected static MetaSubscription createSubscription(
                        MetaParameters parameters) throws SessionException
    {
        SubscriptionImpl ms = null;
        
        if (!(parameters instanceof SubscriptionParameters))
            return null;
        
        SubscriptionParameters sp = (SubscriptionParameters) parameters;
        ms = new SubscriptionImpl(sp);
        
        return ms;   
    }
    
    //---------------------------------------------------------------------
    
    protected static void configureMetaSubscription(MetaSubscription ms, 
                                                    MetaParameters mp) 
    {
        setHandlers(ms);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Load, instantiate, and set subscription handlers for the
     * meta subscription parameter.
     * @param ms Instance of MetaSubscription
     */
    
    protected static void setHandlers(MetaSubscription ms) 
    {
        //get list of handlers for success
        HandlerInfo[] hInfos = null;
        List handlerList     = null;
        HandlerInfo hInfo    = null;
        SubscriptionHandler handler = null;
        
        int      curEventType     = -1;
        String   curEventString   = null;
        String[] eventStringArray = new String[] { 
                SubscriptionConstants.EVENT_SUCCESS_STR ,
                SubscriptionConstants.EVENT_FAILURE_STR}; 
        
        for (int i = 0; i < eventStringArray.length; ++i)
        {

            handlerList = new ArrayList();
            curEventString = eventStringArray[i];
            curEventType   = SubscriptionConstants.eventStringToType(
                                                      curEventString);
            
            try {
                hInfos = HandlerRegistry.getHandlers(curEventString);
            } catch (Exception ex) {
                hInfos = new HandlerInfo[0];
            }
            
            for (int j = 0; j < hInfos.length; ++j)
            {
                hInfo = hInfos[j];
                try {
                    handler = HandlerRegistry.loadHandler(hInfo);
                } catch (Exception ex) {
                    handler = null;
                }
                
                if (handler != null)
                    handlerList.add(handler);
            }
            
            ms.getHandlerTable().addHandlers(curEventType, handlerList);
        }
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
}
