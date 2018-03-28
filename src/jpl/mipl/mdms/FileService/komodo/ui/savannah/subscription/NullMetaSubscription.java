package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;


/**
 * <b>Purpose:</b>
 * NullSubscription extends DefaultMetaSubscription but offers
 * no client support.  Implementation of the initClient() method
 * trivially sets the client field to null.
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
 * @version $Id: NullMetaSubscription.java,v 1.4 2005/04/18 23:49:28 ntt Exp $
 *
 */

public class NullMetaSubscription extends DefaultMetaSubscription
{
    //---------------------------------------------------------------------
    
    /**
     * Constructs a NullSubscription.  Uses an empty parameter object.
     * so that source, target, and client parameters are all null.
     */
    
    public NullMetaSubscription()
    {
        this(new DefaultMetaParameters());        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructs a NullSubscription.  
     * @param Instance of MetaParameters.
     */
    
    public NullMetaSubscription(MetaParameters parameters)
    {
        super(parameters);        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Simply sets client reference to null.
     */
    
    protected void initClient()
    {
        this._client = null;
    }
    
    //---------------------------------------------------------------------
    
}
