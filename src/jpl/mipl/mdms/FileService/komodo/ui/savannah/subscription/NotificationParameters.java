/*
 * Created on Jan 13, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

/**
 * <b>Purpose:</b>
 * Parameters class for FEI notification.
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
 * 01/13/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: NotificationParameters.java,v 1.4 2008/11/03 19:30:40 ntt Exp $
 *
 */

public class NotificationParameters extends DefaultMetaParameters
{
    //---------------------------------------------------------------------
    
    public NotificationParameters()
    {
        super();
        this.setTaskType(SubscriptionConstants.TASK_NOTIFICATION);
    }
    
    //---------------------------------------------------------------------
    
    public NotificationParameters(DefaultMetaParameters dmp)
    {
        super(dmp);
        this.setTaskType(SubscriptionConstants.TASK_NOTIFICATION);
    }
    
    //---------------------------------------------------------------------
}
