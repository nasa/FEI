package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.util.List;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler.SubscriptionHandler;

/**
 * <b>Purpose:</b>
 * Virual proxy for implemenations of the SubscriptionHandler interface.
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
 * 03/01/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionHandlerProxy.java,v 1.4 2005/03/31 22:02:06 ntt Exp $
 *
 */

public class SubscriptionHandlerProxy implements SubscriptionHandler
{
    SubscriptionHandler _delegate = null;
    boolean             _invoked  = false;
    HandlerInfo         _info     = null;
    Client              _client   = null;
    SavannahModel       _model    = null;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Creates a virtual proxy to a SubscriptionHandler.
     * Delegate is not instatiated until the first invocation of the
     * handleEvent method.
     * @param info Handler info used to initialize handler
     */
    
    SubscriptionHandlerProxy(HandlerInfo info)
    {
        this._info = info;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns handler name
     * @return handler name
     */
    
    public String getName()
    {
        return this._info.getName();
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns handler id
     * @return handler id
     */
    
    public String getId()
    {
        return this._info.getId();
    }

    //---------------------------------------------------------------------
    
    /** 
     * Returns the classname of the handler
     * @return Handler class name
     */
    
    public String getClassname()
    {
        return this._info.getClassname();
    }

    //---------------------------------------------------------------------
    
    /**
     * This method forces the creation of the delegate instance.  Delegate
     * is created and initialized with state maintained by the proxy.
     * Method call is then dispatched to it.
     * @param taskType Task type (one of TASK_{SUBSCRIPTION|NOTIFICATION}
     * @param results List of Result instances
     */
    
    public void handleEvent(int taskType, List results)
    {
        try {
            getDelegate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        this._delegate.handleEvent(taskType, results);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates the delegate using the HandlerRegistry static
     * factory method.  Sets the application model and client
     * based on its own state.
     * @return Delegate instance of type SubscriptionHandler
     */
    
    private final SubscriptionHandler getDelegate() throws Exception 
    {
        if (this._invoked) 
        {
            return this._delegate;
        }
        
        this._invoked = true;
        
        this._delegate = HandlerRegistry.loadHandler(this._info);
        this._delegate.setAppModel(this._model);
        this._delegate.setClient(this._client);
        
        return this._delegate;
    }

    //---------------------------------------------------------------------

    /**
     * Proxy maintains reference to the configuration info instance.
     * @param hInfo Handler info
     */
    
    public void initialize(HandlerInfo hInfo) throws Exception
    {
        this._info = hInfo;
    }

    //---------------------------------------------------------------------
    
    /** 
     * Set client used by handler.
     * @param client New client instance
     */
    
    public void setClient(Client client)
    {
        if (this._delegate != null)
            this._delegate.setClient(client);
        else
            this._client = client;
    }

    //---------------------------------------------------------------------
    
    /**
     * Sets reference to the application model.
     * @param model Instance of SavannahModel 
     */
    
    public void setAppModel(SavannahModel model)
    {
        if (this._delegate != null)
            this._delegate.setAppModel(model);
        else
            this._model = model;
    }

    //---------------------------------------------------------------------
    
    /**
     * Nullifies this instance.  Do not use this object once this method has 
     * been called.  
     */
    
    public void destroy()
    {
        if (this._delegate != null)
            this._delegate.destroy();
        else
        {
            this._model  = null;
            this._client = null;
        }
        
        this._delegate = null;
    }

    //---------------------------------------------------------------------
    
}
