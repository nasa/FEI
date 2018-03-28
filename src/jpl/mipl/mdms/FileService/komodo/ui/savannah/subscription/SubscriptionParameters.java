/*
 * Created on Jan 12, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

/**
 * <b>Purpose:</b>
 * Parameter class for FEI subscriptions.
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
 * 01/12/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionParameters.java,v 1.5 2010/05/13 21:23:04 ntt Exp $
 *
 */

public class SubscriptionParameters extends DefaultMetaParameters
{
    
    //---------------------------------------------------------------------
    
    public SubscriptionParameters()
    {
        super();
        this.setTaskType(SubscriptionConstants.TASK_SUBSCRIPTION);
    }
    
    //---------------------------------------------------------------------
    
    public SubscriptionParameters(DefaultMetaParameters dmp)
    {
        super(dmp);
        this.setTaskType(SubscriptionConstants.TASK_SUBSCRIPTION);
    }
    
    //---------------------------------------------------------------------
    
    public void setCrc(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_CRC, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getCrc()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_CRC);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setReceipt(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_RECEIPT, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getReceipt()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_RECEIPT);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setSaferead(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_SAFEREAD, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getSaferead()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_SAFEREAD);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    
    public boolean getDiff()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_DIFF);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setDiff(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_DIFF, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public void setVersion(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_VERSION, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getVersion()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_VERSION);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setReplace(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_REPLACE, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getReplace()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_REPLACE);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------

}
