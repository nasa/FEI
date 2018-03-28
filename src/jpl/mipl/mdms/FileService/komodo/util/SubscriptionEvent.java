/*
 * Created on Jun 3, 2005
 */
package jpl.mipl.mdms.FileService.komodo.util;

import java.util.EventObject;

/**
 * <b>Purpose:</b>
 * Subscription event object.
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
 * 06/03/2005        Nick             Initial Release
 * 07/12/2005        Nick             Added source, id, and timestamp.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionEvent.java,v 1.1 2005/09/23 00:16:10 ntt Exp $
 *
 */

public class SubscriptionEvent extends EventObject
{
    public static final int ID_NULL     = -1;
    public static final int ID_NEW_FILE =  1;
    
    //---------------------------------------------------------------------
    
    /** Object associated with event */
    protected Object object;
    
    /** Timestamp of event */
    protected long   timestamp;
    
    /** Event type */
    protected int    id;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param src Source of event
     * @param obj Object associated with event
     */
    
    public SubscriptionEvent(Object src, Object obj)
    {
        this(src, SubscriptionEvent.ID_NULL, obj);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param src Source of event
     * @param id  Event type
     * @param obj Object associated with event
     */
    
    public SubscriptionEvent(Object src, int id, Object obj)
    {
        super(src);
        this.timestamp = System.currentTimeMillis();
        this.object    = obj;
        this.id        = id;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns object associated with event if specified during construction,
     * otherwise null is returned.
     * @return Object associated with event, or null.
     */
    
    public Object getObject()
    {
        return this.object;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the event type id of this instance.  If id was never set,
     * then <code>SubscriptionEvent.ID_NULL</code> is returned.
     * @return Event type, ID_NULL if not specified.
     */
    
    public int getId()
    {
        return this.id;
    }
    
    //---------------------------------------------------------------------
}
