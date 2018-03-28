/*
 * Created on Feb 17, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler;

import java.util.List;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.HandlerInfo;


/**
 * <b>Purpose:</b>
 * Default implementation of the methods of the SubscriptionHandler.  
 * Subclasses must implement the <code>handleEvent()</code> method.
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
 * mm/dd/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: AbstractSubscriptionHandler.java,v 1.4 2005/03/31 22:02:02 ntt Exp $
 *
 */

public abstract class AbstractSubscriptionHandler implements SubscriptionHandler
{
    
    protected Client        _client  = null;
    protected SavannahModel _model   = null;
    
    /** Name of this handler */
    protected String        _name    = null;
    
    /** Id of this handler */
    protected String        _id = null;
    
    /** Class name of this handler */
    protected String        _classname   = null;
    
    //---------------------------------------------------------------------
    
    /**
     * Handle method performs operation of the handler.  Implementors
     * can use the event type as a filter so that handler is only
     * invoked for specific event types.
     * @param taskType Type of task, one of 
     *            SubscriptionConstants.TASK_{SUBSCRIPTION,NOTIFICATION}
     * @param results List of Result objects 
     */
    
    public abstract void handleEvent(int taskType, List results);

    //---------------------------------------------------------------------
    
    public void initialize(HandlerInfo info) throws Exception
    {
        this._name = info.getName();
        this._classname = info.getClassname();
        this._id = info.getId();
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
    
    public void setClient(Client client)
    {
        if (this._client != client)
            this._client = client;
    }

    //---------------------------------------------------------------------
    
    public void setAppModel(SavannahModel model)
    {
        if (this._model != model)
            this._model = model;
    }
    
    //---------------------------------------------------------------------
    
    public String toString()
    {
        return this.getClass().getName()+"::"+ this._name+
               "(id "+this._id+") "+super.toString();
    }
    
    //---------------------------------------------------------------------
    
    public void destroy()
    {
        setAppModel(null);
        setClient(null);
    }
    
    //---------------------------------------------------------------------
    
}
