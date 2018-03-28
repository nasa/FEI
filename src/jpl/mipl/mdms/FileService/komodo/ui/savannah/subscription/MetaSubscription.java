package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.beans.PropertyChangeListener;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.SubscriptionHandlerTable;

/**
 * <b>Purpose:</b>
 *  Interface for FEI meta-subscription classes.  Includes methods for
 *  event listeners, event recognizer, and activity state. 
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
 * <B>Date              Who                        What</B>
 * ----------------------------------------------------------------------------
 * 09/22/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MetaSubscription.java,v 1.9 2008/09/10 23:10:14 awt Exp $
 *
 */

public interface MetaSubscription extends Runnable
{
    public final static int NULL_ID = -1;
    
    public final static String PROPERTY_STATE   = "metaSubscriptionState";
    public final static String PROPERTY_CLIENT  = "metaSubscriptionClient";
    public final static String PROPERTY_HISTORY = "metaSubscriptionHistory";
    
    //---------------------------------------------------------------------

    /**
     * Returns flag indicating whether or not subscription has been
     * interrupted or paused.
     * @return Interrupt status.
     */
    
    public boolean isInterrupted();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns flag indicating whether or not subscription has been
     * terminated.
     * @return Termination status.
     */
    
    public boolean isTerminated();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns flag indicating whether or not subscription is busy 
     * orocessing.
     * @return Busy status.
     */
    
    public boolean isBusy();
    
    //---------------------------------------------------------------------
    
    /**
     * Terminates the subscription so that events are no longer processed.
     */
    
    public void terminate();
    
    //---------------------------------------------------------------------
    
    /**
     * Sets external interrupt flag.  Can be set by a managing 
     * component to pause this subscription without changing its
     * state specifically.
     * @param interrupted True to interrupt subscription, false
     *        to reset interrupt.
     */
    
    public void setInterrupted(boolean interrupted);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns object being used as the source of the subscription.
     * This might be a FEI filetype, or another object that produces
     * subscription information.
     * @return Source object of the subscription
     */
    
    public Object getSource();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns object being used as the target of the subscription.
     * This might be a destination file directory, or another object
     * that will process events.
     * @return Target object of the subscription
     */
    
    public Object getTarget();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the session object associated with the subscription.
     * @return Session object associated with subscription.
     */
    
    public Client getClient();
        
    //---------------------------------------------------------------------
    
    /**
     * Returns id associated with this subscription.
     * @return subscription id
     */
    
    public int getId();
    
    //---------------------------------------------------------------------
    
    /**
     * Sets subscription id.
     * @param id New subscription id
     */
    
    public void setId(int id);
    
    //---------------------------------------------------------------------
    
    /**
     * Initiates the main processing loop.
     */
    
    public void run();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns refernce to the handler table used to link events to
     * handlers.
     * @return Instance's SubscriptionHandlerTable
     */
    
    public SubscriptionHandlerTable getHandlerTable();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns history object that records what files have been
     * handled by the meta-subscription.
     * @return History object 
     */
    
    public Object getHistory();
    
    //---------------------------------------------------------------------
    
    /**
     * Clears history object of records what files have been
     * handled by the meta-subscription.
     */
    
    public void clearHistory();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the current state of the subscription.
     * @return state
     */
    
    public int getState();

    //---------------------------------------------------------------------
    
    /**
     * Set current state of subscription.  The update will only be made
     * if the state transition from current state to new state is legal
     * based on the implicit state machine.  In order to check, the
     * state is returned from the method.  If the return value does not
     * equal that of the parameter, then the transition was not allowed.
     * @param newState New state of this subscription. One of
     * STATE_{INITIALIZED,RUNNING,BUSY,PAUSED,ERROR,TERMINATED}
     * @return Current state of subscription after method finishes
     */
    
    public int setState(int newState);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the task type of this metasubscription.  
     * @return task type
     */
    
    public int getTaskType();

    //---------------------------------------------------------------------
    /**
     * Returns the parameters of this metasubscription.
     * @return parameters
     */
    public MetaParameters getParameters();
    
    //---------------------------------------------------------------------
    
    /**
     *  Adds listener for property change of model.
     *  @param l Object implementing the PropertyChangeListener 
     *           interface to be added
     */
    
    public void addPropertyChangeListener(PropertyChangeListener l);
    
    //---------------------------------------------------------------------
     
    /**
     *  Removes listener for property change of model.
     *  @param l Object implementing the PropertyChangeListener 
     *           interface to be removed
     */
    
    public void removePropertyChangeListener(PropertyChangeListener l);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true iff this and other have same task type, source,
     * and target.
     * @param other Another instance of MetaSubscription to compare
     *              equivalence with.
     * @return True iff this and other are equivalent, false otherwise
     */
    
    public boolean equivalentTo(MetaSubscription other);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns name of metasubscription.  Usually includes the id, source,
     * and target, when defined.
     * @return Name of metasubscription
     */
    
    public String getName();
    
    //---------------------------------------------------------------------
    
}
