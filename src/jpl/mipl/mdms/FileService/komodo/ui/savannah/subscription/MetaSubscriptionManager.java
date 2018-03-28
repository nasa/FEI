package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.SessionCacheRegistry;

/**
 * <b>Purpose:</b>
 * Interface for meta-subscription managers.  Implementations of this
 * interface manage subscription sets.
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
 * 09/27/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MetaSubscriptionManager.java,v 1.7 2008/10/28 19:00:34 ntt Exp $
 *
 */

public interface MetaSubscriptionManager
{
    //---------------------------------------------------------------------
    
    /**
     * Adds a meta-subscription.
     * @param subscription Instance of meta-subscription to be added 
     * @throws IllegalArgumentException if subscription parameter is
     *         null or if subscription.getSource() returns null.
     */
    
    public void addMetaSubscription(MetaSubscription subscription) 
                                          throws IllegalArgumentException;
    
    //---------------------------------------------------------------------
    
    /**
     * Removes a meta-subscription.
     * @param subscription Instance of subscription to be removed 
     * @throws IllegalArgumentException if subscription paraemeter is
     *         null or if subscription.getSource() returns null.
     */
    
    public void removeMetaSubscription(MetaSubscription subscription)
                                            throws IllegalArgumentException;
  
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of meta-subscriptions registered with the source parameter.
     * @param source Source of subscriptions to be returned. 
     * @return Array of subscriptions associated with source parameter.
     * @throws IllegalArgumentException if source paraemeter is
     *         null.
     */
    
    public MetaSubscription[] getMetaSubscriptions(Object source);
   
    //---------------------------------------------------------------------
    
    /**
     * Returns a meta-subscription associated with the subscription id 
     * parameter.
     * @subscriptionId Subscription id of interest.
     * @return Subscriptions associated with subscription id, null 
     *         if no match.
     * @throws IllegalArgumentException if id is negative.
     */
    
    public MetaSubscription getMetaSubscription(int subscriptionId);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of registered meta-subscriptions.
     * @return Array of all register meta-subscriptions.
     */
    
    public MetaSubscription[] getMetaSubscriptions();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of sources with at least one meta-subscription 
     * established.
     * @return Array of meta-subscribed sources. 
     */
    
    public Object[] getMetaSubscriptionSources();
    
    //---------------------------------------------------------------------
    
    /**
     * Terminates this manager.  Once a manager is terminated, it should
     * clean up resources and set itself to a final terminated state.
     * Method calls that would otherwise change the state will result
     * in an IllegalStateException being thrown.
     */
    
    public void terminate();
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the interruption state of the manager.  True to enter interrupt
     * state, false to leave it.
     * @param flag Interruption state
     */
    
    public void setInterrupted(boolean flag);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if manager has been interrupted, false otherwise.
     * @return Interruption state. 
     */
    
    public boolean isInterrupted();

    //---------------------------------------------------------------------
    
    /**
     * Returns true if manager has been terminated, false otherwise.  Once
     * true, this method should always return true.
     * @return Termination state. 
     */
    
    public boolean isTerminated();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the count of subscriptions under management.
     * @return subscription count
     */
    
    public int getSize();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns reference to application model, SavannahModel
     */
    
    public SavannahModel getAppModel();
    
    //---------------------------------------------------------------------

    /**
     * Returns the manager name.  Used for logging messages and reporting.
     * @return manager name
     */
    
    public String getName();
    
    //---------------------------------------------------------------------
    
    public SessionCacheRegistry getSessionCacheRegistry();

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

}
