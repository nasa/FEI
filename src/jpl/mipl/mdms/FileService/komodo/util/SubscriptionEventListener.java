/*
 * Created on Jun 3, 2005
 */
package jpl.mipl.mdms.FileService.komodo.util;

/**
 * <b>Purpose:</b>
 * Interface for subscription listener.
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
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionEventListener.java,v 1.1 2005/09/23 00:16:10 ntt Exp $
 *
 */

public interface SubscriptionEventListener
{
    /**
     * Event notification method.
     * @param event Instance of SubscriptionEvent associated with the event
     */
    public void eventOccurred(SubscriptionEvent event);
}
