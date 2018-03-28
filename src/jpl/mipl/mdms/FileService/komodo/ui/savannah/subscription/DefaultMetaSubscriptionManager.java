package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.SwingUtilities;

import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.MetaParametersParseUtil;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.SessionCacheRegistry;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Default manager class for FEI subscription and event scheduling.
 * Maintains a hashtable of of subscription lists.  Index is the
 * subscription's source, using the <code>Subscription.getSource()
 * </code> method.
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
 * 09/22/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DefaultMetaSubscriptionManager.java,v 1.10 2008/11/03 19:30:40 ntt Exp $
 *
 */

public class DefaultMetaSubscriptionManager implements MetaSubscriptionManager
{
    private final String __classname = this.getClass().getName();
    
    /** Subscription table */
    protected final Hashtable _subscriptionTable = new Hashtable();
    
    /** Interruption flag */
    protected boolean  _interrupted;
    
    /** Termination flag */
    protected boolean  _terminated;
    
    /** synchronization lock */
    protected final Object _syncLock = new Object();
    
    /** Enables bean property change event handling */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(this);
    
    /** Listener to individual metasubscription properties */
    protected PropertyChangeListener _msListener;
    
    //unique id, one per subscription entity
    private int _uniqueSubscriptionId = 0;
    
    /** Reference to application model */
    protected SavannahModel _appModel;
    
    /** Flag indicating whether duplicate, equivalent subscriptions 
     * are permitted */
    protected boolean _allowDuplicates = false;
    
    /** Property of this manager's set of metasubscriptions */
    public static final String PROPERTY_META_SUBSCRIPTION_SET = 
                           "MetaSubscriptionManager.meta.subscription.set";
    
    /** Property signifying that specific metasubscription property changed */
    public static final String PROPERTY_META_SUBSCRIPTION_ENTRY = 
                         "MetaSubscriptionManager.meta.subscription.entry";
    
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    protected String _name = "Subscription Manager";
    
    SessionCacheRegistry _sessionCache;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param appModel Application model
     */
    
    public DefaultMetaSubscriptionManager(SavannahModel appModel)
    {
        this._interrupted     = false;
        this._terminated      = false;
        this._appModel        = appModel;
        this._msListener      = new MSPropertyChangeListener();
        this._sessionCache    = SessionCacheRegistry.restoreFromCache();
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Adds listener for property change of model.
     *  @param l Object implementing the PropertyChangeListener 
     *           interface to be added
     */
    
    public void addPropertyChangeListener(PropertyChangeListener l)
    {        
        _changes.addPropertyChangeListener(l);  
    }
    
    //---------------------------------------------------------------------
     
    /**
     *  Removes listener for property change of model.
     *  @param l Object implementing the PropertyChangeListener 
     *           interface to be removed
     */
    
    public void removePropertyChangeListener(PropertyChangeListener l)
    {     
        _changes.removePropertyChangeListener(l);   
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a meta-subscription.
     * @param subscription Instance of meta-subscription to be added 
     * @throws IllegalArgumentException if subscription parameter is
     *         null or if subscription.getSource() returns null.
     *         Also thrown if an active metasubscription with the
     *         same source and target is found.
     */
    
    public void addMetaSubscription(MetaSubscription subscription) 
                                            throws IllegalArgumentException
    {
        if (this._terminated)
            throw new IllegalStateException(__classname+
                    "::addMetaSubscription()"+
                    ": Cannot call method while in terminated state.");
        
        if (subscription == null)
            throw new IllegalArgumentException(__classname+"::"+
                    "addMetaSubscription(): Cannot add null");
        
        if (subscription.getSource() == null)
            throw new IllegalArgumentException(__classname+"::"+
                    "addMetaSubscription(): Cannot add subscription with "+
                    "null source.");
        
        synchronized(_syncLock)
        {
            //assign id to subscription, set initial interrupt state
            subscription.setId(this.getNextSubscriptionId());
            subscription.setInterrupted(this._interrupted);
            
            List list = (List) _subscriptionTable.get(subscription.getSource());
            if (list == null)
            {
                list = new ArrayList();
                _subscriptionTable.put(subscription.getSource(), list);
            }
            
            //check for active ms's with same source,target pair
            if (!_allowDuplicates)
            {
                int listSize = list.size();
                MetaSubscription curMs;
                for (int i = 0; i < listSize; ++i)
                {
                    curMs = (MetaSubscription) list.get(i);
                    if (!curMs.isTerminated() && 
                               curMs.equivalentTo(subscription))
                    {
                        throw new IllegalArgumentException("Equivalent " +
                              "active subscription found.");
                    }
                }
            }
            
            list.add(subscription);
            subscription.addPropertyChangeListener(_msListener);
            this._logger.info(getName()+": Subscription added - "
                                           + subscription.getName());
            
            //---------------------
            
            attemptPersistAdd(subscription);              
        }
        
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            _changes.firePropertyChange(PROPERTY_META_SUBSCRIPTION_SET, 
                                        null, null);
        }});
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Removes a subscription.
     * @param subscription Instance of subscription to be removed 
     * @throws IllegalArgumentException if subscription paraemeter is
     *         null or if subscription.getSource() returns null.
     */
    
    public void removeMetaSubscription(MetaSubscription subscription)
                                            throws IllegalArgumentException
    {
        if (this._terminated)
            throw new IllegalStateException(__classname+
                    "::removeMetaSubscription()"+
                    ": Cannot call method while in terminated state.");
        
        if (subscription == null)
            throw new IllegalArgumentException(__classname+"::"+
                    "removeMetaSubscription(): Cannot remove null");
        
        if (subscription.getSource() == null)
            throw new IllegalArgumentException(__classname+"::"+
                    "addSubscription(): Cannot remove subscription with "+
                    "null source.");
        
        synchronized(_syncLock)
        {
            List list = (List)_subscriptionTable.get(subscription.getSource());
            if (list == null)
                return;
            list.remove(subscription);
            subscription.removePropertyChangeListener(_msListener);
            this._logger.info(getName()+": Subscription removed - "
                                       + subscription.getName());
            
            attemptPersistRemove(subscription);
        }
        
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            _changes.firePropertyChange(PROPERTY_META_SUBSCRIPTION_SET, 
                                        null, null);
        }});
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Removes all current subscription entries from this manager that are
     * not set with STAY_ALIVE active.
     */
    
    public void clear()
    {
        clear(false);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Removes all current subscription entries from this manager if parameter
     * is true, otherwise only remove those without STAY_ALIVE set.
     * @param clearStayAliveSessions True to clear ALL sessions, false to clear
     * only non-persisted.
     */
    
    public void clear(boolean clearStayAliveSessions)
    {
        synchronized(_syncLock)
        {        
            MetaSubscription[] msArray = getMetaSubscriptions();
            for (int i = 0; i < msArray.length; ++i)
            {
                boolean removeSession = false;
                if (clearStayAliveSessions)
                {
                    removeSession = true;
                }
                else
                {
                    MetaParameters mp = msArray[i].getParameters();
                    Boolean stayAlive = (Boolean) mp.get(SubscriptionConstants.
                                                     KEY_STAYALIVE); 
                    if (stayAlive == null || stayAlive.equals(Boolean.FALSE))
                    {                
                        removeSession = true;
                    }
                }
                
                if (removeSession)
                    this.removeMetaSubscription(msArray[i]);
            }
            
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                _changes.firePropertyChange(PROPERTY_META_SUBSCRIPTION_SET, 
                                            null, null);
            }});
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns a list of subscriptions registered with the source parameter.
     * @param source Source of subscriptions to be returned.  Used to index 
     *               internal data structure. 
     * @return Array of all subscriptions registered to manager.
     * @throws IllegalArgumentException if source paraemeter is
     *         null.
     */
    
    public MetaSubscription[] getMetaSubscriptions(Object source)
    {   
        if (this._terminated)
            throw new IllegalStateException(__classname+
                    "::getMetaSubscriptions()"+
                    ": Cannot call method while in terminated state.");
        
        MetaSubscription[] subscriptions;
        
        if (source == null)
            throw new IllegalArgumentException(__classname+"::"+
                    "getMetaSubscriptions(): Source cannot be null");
        List list;
        
        synchronized(_syncLock)
        {
            list = (List) this._subscriptionTable.get(source);
        }
        
        if (list == null)
            list = new ArrayList();
        else
            list = new ArrayList(list);
        
        Collections.sort(list);
        int listSize = list.size();
        subscriptions = new MetaSubscription[listSize];
        for (int i = 0; i < listSize; ++i)
        {
            subscriptions[i] = (MetaSubscription) list.get(i);
        }
        
        return subscriptions;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of all subscriptions.
     * @return Array of all subscriptions registered to manager.
     */
    
    public MetaSubscription[] getMetaSubscriptions()
    {   
        if (this._terminated)
            throw new IllegalStateException(__classname+
                    "::getMetaSubscriptions()"+
                    ": Cannot call method while in terminated state.");
        
        MetaSubscription[] subscriptions;
        List list1, list2 = new ArrayList();
        
        synchronized(_syncLock)
        {
            list1 = new ArrayList(_subscriptionTable.values());
        }
        
        int list1Size = list1.size();
        for (int i = 0; i < list1Size; ++i)
        {
            list2.addAll((List) list1.get(i));
        }
        
        Collections.sort(list2);
        int list2Size = list2.size();
        subscriptions = new MetaSubscription[list2Size];
        for (int i = 0; i < list2Size; ++i)
        {
            subscriptions[i] = (MetaSubscription) list2.get(i);
        }
        
        return subscriptions;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns a subscription associated with the subscription id 
     * parameter.
     * @subscriptionId Subscription id of interest.
     * @return Subscriptions associated with subscription id, null 
     *         if no match.
     * @throws IllegalArgumentException if id is negative.
     */
    
    public MetaSubscription getMetaSubscription(int subId)
    {
        MetaSubscription sub = null;
        MetaSubscription[] subscriptions = getMetaSubscriptions();
        
        for (int i = 0; i < subscriptions.length && sub == null; ++i)
        {
            MetaSubscription current = subscriptions[i];
            if (current.getId() == subId)
                sub = current;
        }
        return sub;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns an array of sources with at least one subscription established.
     * @return Array of subscribed sources. 
     */
    
    public Object[] getMetaSubscriptionSources()
    {
        if (this._terminated)
            throw new IllegalStateException(__classname+
                    "::getSubscriptionSources(): "+
                    "Cannot call method while in terminated state.");
        
        Object[] sources;
        List list = new ArrayList();
        
        synchronized(_syncLock) {
            Enumeration keys = this._subscriptionTable.keys();
            while (keys.hasMoreElements())
                list.add(keys.nextElement());
        }
        
        int listSize = list.size();
        sources = new Object[listSize];
        for (int i = 0; i < listSize; ++i)
        {
            sources[i] = list.get(i);
        }
        
        return sources;
    }

    //---------------------------------------------------------------------
    
    /**
     * Cleans up resources during termination.
     */
    
    protected void nullify()
    {
        clear();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Terminates this manager.  Once a manager is terminated, it should
     * clean up resources and set itself to a final terminated state.
     * Method calls that would otherwise change the state will result
     * in an IllegalStateException being thrown.
     */
    
    public void terminate()
    {
        clear();
        this._terminated = true;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the interruption state of the manager.  True to enter interrupt
     * state, false to leave it.
     * @param flag Interruption state
     */
    
    public void setInterrupted(boolean flag)
    {
        if (this._terminated)
            throw new IllegalStateException(__classname 
                    + "::setInterrupted()"
                    + ": Cannot call method while in terminated state.");
        
        this._interrupted = flag;
        
        MetaSubscription[] ms = getMetaSubscriptions();
        for (int i = 0; i < ms.length; ++i)
        {
            ms[i].setInterrupted(flag);
            _changes.firePropertyChange(PROPERTY_META_SUBSCRIPTION_ENTRY,
                                        null, new Integer(ms[i].getId()));
        }        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if manager has been interrupted, false otherwise.
     * @return Interruption state. 
     */
    
    public boolean isInterrupted()
    {
        if (this._terminated)
            throw new IllegalStateException(__classname+"::isInterrupted()"+
                    ": Cannot call method while in terminated state.");
        
        return this._interrupted;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if manager has been terminated, false otherwise.  Once
     * true, this method should always return true.
     * @return Termination state. 
     */
    
    public boolean isTerminated()
    {
        return this._terminated;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns next sequential subscription id.  
     */
    /*  Performs a check with
     *  the cache to ensure that new id does not already exist
     */
    
    protected synchronized int getNextSubscriptionId()
    {
        int newId = this._uniqueSubscriptionId++;
        
        return newId;  
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the count of subscriptions under management.
     * @return subscription count
     */
    
    public int getSize()
    {
        return this._subscriptionTable.size();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns reference to the application model.
     * @return Application model reference
     */
    
    public SavannahModel getAppModel()
    {
        return this._appModel;
    }

    //---------------------------------------------------------------------
    
    /**
     * Property change listener associated with meta subscriptions.
     * Some properties should be relayed to listeners of this manager
     * (ie updating a subscription state)
     */
    
    class MSPropertyChangeListener implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent pce)
        {
            Object source = pce.getSource();
            if (!(source instanceof MetaSubscription))
                return;
            
            MetaSubscription ms = (MetaSubscription) source;
            String propertyName = pce.getPropertyName();
            
            //metasubscription state has changed
            if (propertyName.equals(MetaSubscription.PROPERTY_STATE))
            {
                int id = ms.getId();
                
                //alert listeners that metasubscription has changed
                _changes.firePropertyChange(PROPERTY_META_SUBSCRIPTION_ENTRY,
                                            null, new Integer(id));
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * Checks the session parameters to see if STAYALIVE flag is
     * set.  If so, then the parameters for the session are 
     * persisted in the session cache registry.
     * @param session Instance of metasubscription to be persisted
     * @return True if session was persisted, false otherwise
     */
    
    protected boolean attemptPersistAdd(MetaSubscription session)
    {
        boolean success = false;
        
        MetaParameters params = session.getParameters();
        Boolean shouldPersist = (Boolean) params.get(SubscriptionConstants.
                                                     KEY_STAYALIVE);        
        
        if (shouldPersist != null && shouldPersist)
        {            
            String id = getIdForSession(session);
            
            Hashtable valueTable = new Hashtable(params.getAll());  
            Properties props = MetaParametersParseUtil.toStrings(valueTable);
            
            this._sessionCache.addSessionSettings(id, props);
            try {
                this._sessionCache.commit();
                this._logger.trace("Persisted subscription session: "+id);
                success = true;
            } catch (SessionException sesEx) {
                this._logger.error("Error occurred while persisting new " +
                                   "subscription session "+id+". Message: "+  
                                   sesEx.getMessage());
                this._logger.trace(null, sesEx);
            }
        }
        
        return success;
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * Checks the session parameters are stored in cache.  If so, 
     * then the parameters for the session are 
     * removed from the session cache registry.
     * @param session Instance of metasubscription to be removed 
     *        from cache persisted
     * @return True if session was removed and cache committed, false otherwise
     */
    
    protected boolean attemptPersistRemove(MetaSubscription session)
    {
        String id = getIdForSession(session);
        boolean removed = this._sessionCache.removeSessionSettings(id);
        if (removed)
        {
            try {         
                this._sessionCache.commit();
            } catch (SessionException sesEx) {
                this._logger.error("Error occurred while committing " +
                     "subscription session cache after an entry deletion: "+
                     sesEx.getMessage());
                this._logger.trace(null, sesEx);
                removed = false;
            }
        }
        
        return removed;       
    }
               
    //---------------------------------------------------------------------
    
    
    /**
     * Returns the manager name.  Used for logging messages and reporting.
     * @return manager name
     */
    
    public String getName()
    {
        return this._name;
    }
    
    //---------------------------------------------------------------------
    
    public SessionCacheRegistry getSessionCacheRegistry()
    {
        return this._sessionCache;
    }
    
    //---------------------------------------------------------------------
    
    protected String getIdForSession(MetaSubscription session)
    {           
        MetaParameters params = session.getParameters();
        
        String  ft = (String)  params.get(SubscriptionConstants.KEY_FILETYPE);
        Integer tt = (Integer) params.get(SubscriptionConstants.KEY_TASK_TYPE);
        String  wd = (String)  params.get(SubscriptionConstants.KEY_OUTPUT_DIRECTORY);
        
        if (ft == null)
            ft = "NULL";
        if (wd == null)
            wd = System.getenv("user.dir");
                    
        String id = tt == null ? "NULL" : tt.toString();
        id += "." + ft + "." + wd;
        return id;
    }
    //---------------------------------------------------------------------
}
