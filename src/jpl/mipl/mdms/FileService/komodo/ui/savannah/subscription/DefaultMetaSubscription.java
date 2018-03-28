package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.ClientFactory;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.SubscriptionHandlerTable;
import jpl.mipl.mdms.FileService.komodo.util.InvocationCommandUtil;
import jpl.mipl.mdms.FileService.komodo.util.PushSubscriptionClient;
import jpl.mipl.mdms.FileService.komodo.util.ReconnectThrottle;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.Errno;
import jpl.mipl.mdms.FileService.util.SystemProcess;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Default implementation of the MetaSubscription interface with some
 * basic behavior.  
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
 * 09/28/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DefaultMetaSubscription.java,v 1.23 2018/02/15 02:14:56 ntt Exp $
 *
 */

public class DefaultMetaSubscription implements MetaSubscription, Comparable
{
    private final String __classname = this.getClass().getName();
    
    /** List of SubscriptionEventListeners */
    protected List      _listeners;
     
    /** Event handler table */
    protected SubscriptionHandlerTable _handlerTable;
    
    /** Busy / Processing flag */
    protected boolean   _isBusy;
    
    /** Reference to source object */
    protected Object    _source;
    
    /** Reference to target object */
    protected Object    _target;
    
    /** Reference to Client object */
    protected Client    _client;
    
    /** Reference to Push subscription client */
    protected PushSubscriptionClient _subscriptionClient;
    
    /** Reference to parameters */
    protected MetaParameters _parameters;
    
    /** Synchronization lock */
    protected final Object _syncLock = new Object();
    
    /** Subscription id */
    protected int    _subscriptionId;
    
    /** Query interval */
    protected String _queryInterval;
    
    protected final long _BLOCK_TIME = 180000L;
    
    /** state of the subscription */
    protected int _state = SubscriptionConstants.STATE_INITIALIZED;
    
    /** Reference to the property change support */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(this);
    
    /** Reference to the history object */
    protected List _history;
    
    /** Paused by external source flag */
    protected boolean _externalPause = false;
    
    /** Time at which this subscription was started */
    protected long _startTime = -1L;
    
    /** Time at which this subscription was created */
    protected long _creationTime  = -1L;
    
    /** Time at which this subscription terminated */
    protected long _endTime  = -1L;
    
    /** Name of metasubscription */
    protected String _name = null;
    
    //invocation variable regex's
    protected static final String REGEX_FILENAME_NO_PATH = "\\$(f|F)(i|I)(l|L)(e|E)(n|N)(a|A)(m|M)(e|E)(n|N)(o|O)(p|P)(a|A)(t|T)(h|H)";
    protected static final String REGEX_FILENAME = "\\$(f|F)(i|I)(l|L)(e|E)(n|N)(a|A)(m|M)(e|E)";
    protected static final String REGEX_FILEPATH = "\\$(f|F)(i|I)(l|L)(e|E)(p|P)(a|A)(t|T)(h|H)";
    protected static final String REGEX_FILETYPE = "\\$(f|F)(i|I)(l|L)(e|E)(t|T)(y|Y)(p|P)(e|E)";
    protected static final String REGEX_SERVER_GROUP = "\\$(s|S)(e|E)(r|R)(v|V)(e|E)(r|R)(g|G)(r|R)(o|O)(u|U)(p|P)";
    protected static final String REGEX_COMMENT = "\\$(c|C)(o|O)(m|M)(m|M)(e|E)(n|N)(t|T)";

    /** Logger instance */
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    //mail objects and state
    protected Logger _emailMessageLogger = null;
    protected Logger _emailReportLogger  = null;
    protected StringBuffer _reportBuffer = new StringBuffer();
    protected boolean _mailMessage      = false;
    protected boolean _mailReport       = false;
    protected boolean _mailSilentReconn = false;
    
    protected Timer   _reportScheduler = new Timer();
    
    protected ReconnectThrottle _throttle;

    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param parameterList Instance of ParameterList with all
     *        parameters required by the subscription instance
     *        defined.
     */
    
    public DefaultMetaSubscription(MetaParameters parameters)
    {
        this._parameters = parameters;
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates the client, sets source and target
     */
    
    protected void init()
    {   
        this._creationTime   = System.currentTimeMillis();
        this._listeners      = new Vector();
        this._history        = new Vector();
        this._subscriptionId = MetaSubscription.NULL_ID;
        
        //source is filetype, target is outdir
        this._source = this._parameters.get(SubscriptionConstants.
                                            KEY_FILETYPE);
        this._target = this._parameters.get(SubscriptionConstants.
                                            KEY_OUTPUT_DIRECTORY);
        
        //get query interval from system if defined.
        this._queryInterval = System.getProperty(Constants.PROPERTY_QUERY_INTERVAL);
        
        //create handler table
        this._handlerTable = new SubscriptionHandlerTable();
        
        //reconnection throttle
        this._throttle = new ReconnectThrottle();
    }
   
    //---------------------------------------------------------------------

    /**
     * Adds listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            added
     */

    public void addPropertyChangeListener(final PropertyChangeListener l)
    {
        //SwingUtilities.invokeAndWait(new Runnable() {
        //    public void run() {
                _changes.addPropertyChangeListener(l);
        //    }
        //});
    }

    //---------------------------------------------------------------------

    /**
     * Removes listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            removed
     */

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        //SwingUtilities.invokeAndWait(new Runnable() {
        //    public void run() {
                _changes.removePropertyChangeListener(l);
        //    }
        //});
        
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns flag indicating whether or not subscription has been
     * interrupted or paused.
     * @return Interrupt status.
     */
    
    public boolean isInterrupted()
    {
        return (this._externalPause ||
                this._state == SubscriptionConstants.STATE_PAUSED);
    }

    //---------------------------------------------------------------------

    /**
     * Returns flag indicating whether or not subscription has been
     * terminated.
     * @return Termination status.
     */
    
    public boolean isTerminated()
    {
        return this._state == SubscriptionConstants.STATE_TERMINATED;
    }

    //---------------------------------------------------------------------

    /**
     * Sets external interrupt flag.  Can be set by a managing 
     * component to pause this subscription without changing its
     * state specifically.
     * @param interrupted True to interrupt subscription, false
     *        to reset interrupt.
     */
    
    public void setInterrupted(boolean interrupted)
    {
        if (this._externalPause != interrupted)
        {
            boolean oldState = this._externalPause;
            this._externalPause = interrupted;
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns flag indicating whether or not subscription is busy 
     * orocessing.
     * @return Busy status.
     */
    
    public boolean isBusy()
    {
        return this._state == SubscriptionConstants.STATE_BUSY;
    }

    //---------------------------------------------------------------------

    /**
     * Terminates the subscription so that events are no longer processed.
     */
    
    public synchronized void terminate()
    {
        setState(SubscriptionConstants.STATE_TERMINATED);
    }

    //---------------------------------------------------------------------

    /**
     * Returns object being used as the source of the subscription.
     * This might be a FEI filetype, or another object that produces
     * subscription information.
     * @return Source object of the subscription
     */
    
    public Object getSource()
    {
        return this._source;
    }

    //---------------------------------------------------------------------

    /**
     * Returns object being used as the target of the subscription.
     * This might be a destination file directory, or another object
     * that will process events.
     * @return Target object of the subscription
     */
    
    public Object getTarget()
    {
        return this._target;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the session object associated with the subscription.
     * @return Session object associated with subscription.
     */
    
    public Client getClient()
    {
        return this._client;
    }
    
    //---------------------------------------------------------------------
    
    protected Map translatePreferences()
    {
        Map prefMap = new Hashtable();
        Object value;
        
        value = this._parameters.get(SubscriptionConstants.KEY_USERNAME);
        if (value != null)
            prefMap.put("user", value);
        
        value = this._parameters.get(SubscriptionConstants.KEY_PASSWORD);
        if (value != null)        
            prefMap.put("password", value);
        
        //this is the full filetype, need to parse a lil
        value = this._parameters.get(SubscriptionConstants.KEY_FILETYPE);
        if (value != null)
        {
            prefMap.put("servergroup", FileType.extractServerGroup(value.toString()));
            prefMap.put("filetype", FileType.extractFiletype(value.toString()));
        }        
        
        value = this._parameters.get(SubscriptionConstants.KEY_OUTPUT_DIRECTORY);
        if (value != null)
            prefMap.put("output", value);
        else
            JOptionPane.showMessageDialog(null, "Null OUTPUT");
        
        
        //verify that output is set
        if (!prefMap.containsKey("output"))
                prefMap.put("output", ".");
        
        value = this._parameters.get(SubscriptionConstants.KEY_RESTART);
        if (value != null)
            prefMap.put("restart",  value);
            
        
        return prefMap;        
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * Convenience method that encapsulates handling of new files for
     * notification and subscription.  First, a message is printed
     * to the log, then any invocations options are executed.
     * @param result New file result
     * @param outputDir Output directory path
     * @param invoke Invocation command, can be null.
     * @param exitOnError Flag indicating invocation errors should cause
     *        immediate abort.
     * @param operation Operation char, one of Constants.AUTO(GET|SHOW)FILES.
     * @return False iff file was unsuccessfully processed and exit should 
     *         occur.
     */
    
    protected boolean handleNewFile(Result result, String outputDir,
                                   String invoke, boolean exitOnError,
                                   String operation)
    {       
        boolean canProceed = true;
        String name = result.getName();
        String ftString = FileType.toFullFiletype(result.getServerGroup(),
                                                  result.getType());
        String state = (operation.equals(Constants.AUTOGETFILES)) ? 
                       "received" : "available";
        int taskId   = (operation.equals(Constants.AUTOGETFILES)) ?
                       SubscriptionConstants.TASK_SUBSCRIPTION :
                       SubscriptionConstants.TASK_NOTIFICATION;
        
        //create log message
        String msg = "FEI5 Information on "
                       + DateTimeUtil.getCurrentDateCCSDSAString() 
                       + "\n";
        msg += "File type [" + ftString + "]: "+state+" \""
                + name + "\" " + result.getSize() + " bytes "
                + DateTimeUtil.getDateCCSDSAString(
                               result.getFileModificationTime());
         if (result.getChecksumStr() != null)
             msg += (" CRC:" + result.getChecksumStr());
         msg += "\n";
         logToReport(msg);
         this._logger.info(msg);

         //-------------------------
         
         // handle invoke
         if (invoke != null) 
         {
             //returns false if we should exit
             canProceed = performInvocation(result, outputDir, invoke,  
                                            exitOnError, operation);                              
         }

         //-------------------------
         
         //invoke handlers
         List resultList = new ArrayList();
         resultList.add(result);
         try {
             invokeHandlers(SubscriptionConstants.EVENT_SUCCESS,
                            taskId, resultList);                        
         } catch (Exception ex) {
             msg = SubscriptionConstants.ERROR_TAG + " " 
                   + getName() + ": Handler failed with message \"" 
                   + ex.getMessage() + "\".";
             this._logger.error(msg);
             this._logger.debug(null, ex);
             if (this._mailMessage)
                 this._emailMessageLogger.error(msg, ex);
         }
                  
         //-------------------------
         
         return canProceed;
    }
    
    //----------------------------------------------------------------------
    
    /**
     * Performs the result invocation for a new file for notification and
     * subscription.
     * @param result Result object representing new file state
     * @param outputDir Output directory path
     * @param invoke Invocation command string
     * @param exitOnError Flag indicating that invocation was not
     *        successful and thus processing should be aborted.
     * @return False if error occurred and exitOnError was set,
     *         true otherwise.
     */

    protected boolean performInvocation(Result result, String outputDir,
                                       String invoke, boolean exitOnError,
                                       String operation)
    {
        boolean canProceed = true;
        String name = result.getName();
        String ftString = FileType.toFullFiletype(result.getServerGroup(),
                                                  result.getType());
        String opName = (operation.equals(Constants.AUTOGETFILES)) ?
                        "Subscription" : "Notification";
        
        //prepare actual invocation command
        String cmdStr = InvocationCommandUtil.buildCommand(
                                 invoke, outputDir, result);
        
        //log invocation attempt
        String msg = "FEI5 Information on " +
                         DateTimeUtil.getCurrentDateCCSDSAString() + 
                         "\n";
        msg += "Invoke command \"" + cmdStr.trim() + "\"\n";
        logToReport(msg);
        this._logger.info(msg);

        //execute the command
        Errno errno = SystemProcess.execute(cmdStr, this._logger);

        //check for error
        if (errno.getId() != Constants.OK) 
        {
            String errnoReport = "(errno = "+errno.getId();
            if (errno.getMessage() != null)
                errnoReport += "; errmsg = "+errno.getMessage();
            errnoReport += ")";   
            
            msg = "FEI5 Information on " +
                      DateTimeUtil.getCurrentDateCCSDSAString() + 
                      "\n";
            msg += SubscriptionConstants.ERROR_TAG + "File type [" + ftString +
                      "]: invoke process \"" + cmdStr +
                      "\" failed. "+errnoReport+"\n";
            if (this._mailMessage)
               this._emailMessageLogger.error(msg);
            else
               this._logger.error(msg);
         
            //----------------------
            
            //if exitOnErr set, write message and break loop
            if (exitOnError) 
            {
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString()
                     + "\n";
               msg += SubscriptionConstants.ERROR_TAG + opName + " ["
                     + ftString + "]: Aborted.\n";
               if (this._mailMessage)
                  this._emailMessageLogger.error(msg);
               else
                  this._logger.error(msg);
         
               canProceed = false;
            }
        }
        
        return canProceed;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Resets client object based on contents of parameters.  If original
     * client is active, a logout is performed.  Once new client has
     * been created, all listeners are notified using the property name
     * of PROPERTY_CLIENT constant.
     * NOTE: This method may block until a client can be created and returned.
     * If an error occurs that prevents successful exectution, this method
     * can block indefinitely.
     */
    
    protected void initClient() throws SessionException
    {
        Client oldClient = this._client;
        
        if (oldClient != null)
        {
            try {
                if (oldClient.isLoggedOn())
                    oldClient.logout();
        
            } catch (SessionException sesEx) {
                throw sesEx;
                //sesEx.printStackTrace();
            }
            this._client = null;
        }

        //create new client
        this._client = ClientFactory.createClient(this._parameters, 
                                                  _BLOCK_TIME,
                                                  _throttle);
        
        //apply parameters
        if (this._client != null)
        {
            applyClientParameters();
        }
        
        //alert handlers of client
        this._handlerTable.setClient(this._client);
        
        //alert listeners of new client
        firePropertyChangeSync(MetaSubscription.PROPERTY_CLIENT, 
                               oldClient, this._client);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Applies parameters restart and output directory.
     */
    
    protected void applyClientParameters() throws SessionException
    {
        if (this._client == null)
            return;
        
        Object value = null;
        boolean flag = false;
        
        try {
            //restart
            value = this._parameters.get(SubscriptionConstants.
                                         KEY_RESTART);
            flag = (value != null) && ((Boolean) value).booleanValue();
            this._client.set(Client.OPTION_RESTART, flag);

            //output directory
            value = this._parameters.get(SubscriptionConstants.
                                         KEY_OUTPUT_DIRECTORY);
            if (value != null)
                this._client.changeDir((String) value);

        } catch (SessionException sesEx) {
            //sesEx.printStackTrace();
            throw sesEx;            
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns id associated with this subscription.
     * @return subscription id
     */
    
    public int getId()
    {
        return this._subscriptionId;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets subscription id iff it has not yet been set.
     * @param id New subscription id
     */
    
    public void setId(int id)
    {
        if (this._subscriptionId != NULL_ID)
            return;
        
        if (id != this._subscriptionId)
        {
            this._subscriptionId = id;
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Does nothing but wait for the termination call. Sub types should
     * override this method and not call super.run()
     */
    
    public void run()
    {
        setState(SubscriptionConstants.STATE_RUNNING);
        
        while (!this.isTerminated())
            Thread.yield();
        
        setState(SubscriptionConstants.STATE_TERMINATED);
    }   
    
    //---------------------------------------------------------------------

    /**
     * Returns reference to this metasubscriptions handler table
     * @return handler table
     */
    
    public SubscriptionHandlerTable getHandlerTable()
    {
        return this._handlerTable;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Utility method that checks interrupt and terminate flags.
     * If terminate flag is set, immediately return with value false.
     * Else while interrupt flag is set and not terminated, 
     * then pause this thread via Thread.yield().
     * Return terminate value.
     * 
     * @return True if caller should proceed normally, false if caller
     *         should initiate terminate sequence.
     */
    
    protected synchronized boolean canProceed()
    {
        while (this.isInterrupted() && !this.isTerminated())
        {
            Thread.yield();
        }
        
        return !this.isTerminated();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns history object that records what files have been
     * handled by the meta-subscription.
     * @return History object (currently a List)
     */
    
    public Object getHistory()
    {
        return this._history;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the current state of the subscription. One of
     * STATE_{INITIALIZED,RUNNING,BUSY,PAUSED,ERROR,TERMINATED}
     * @return State of subscription.
     */
    
    public int getState()
    {
        return this._state;
    }
    
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
    
    public int setState(int newState)
    {
        
        final int oldState = this._state;
        
        //Source can be: init, paused, busy
        if (newState == SubscriptionConstants.STATE_RUNNING)
        {
            if (this._state == SubscriptionConstants.STATE_INITIALIZED ||
                this._state == SubscriptionConstants.STATE_PAUSED      ||
                this._state == SubscriptionConstants.STATE_BUSY)
            {
                this._state = newState;
            }
        }
        //source can be running
        else if (newState == SubscriptionConstants.STATE_PAUSED)
        {
            if (this._state == SubscriptionConstants.STATE_RUNNING)
            {
                this._state = newState;
            }
        }
        //source can be running
        else if (newState == SubscriptionConstants.STATE_BUSY)
        {
            if (this._state == SubscriptionConstants.STATE_RUNNING ||
                this._state == SubscriptionConstants.STATE_REPAIR)
            {
                this._state = newState;
            }
        }
        else if (newState == SubscriptionConstants.STATE_REPAIR)
        {
            if (this._state == SubscriptionConstants.STATE_BUSY)
            {
                this._state = newState;
            }
        }
        //target: not init, then anything can be source
        else if (newState != SubscriptionConstants.STATE_INITIALIZED)
        {
            this._state = newState;
        }

        
        //if state change, first property change event
        if (this._state != oldState)
        {
            firePropertyChangeAsync(MetaSubscription.PROPERTY_STATE, 
                                    new Integer(oldState), 
                                    new Integer(this._state));
        }
        
        return this._state;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the task type of this metasubscription.  Specifically,
     * returns the value SubscriptionConstants.TASK_UNKNOWN.  This
     * method should be overriden by subclasses.
     * @return SubscriptionConstants.TASK_UNKNOWN
     */
    
    public int getTaskType()
    {
        return SubscriptionConstants.TASK_UNKNOWN;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the parameters used for this metasubscription
     * @return parameters
     */
    public MetaParameters getParameters() {
        return this._parameters;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Invokes handlers for a given set of results based on event type
     * and task type.
     * @param eventType Event type, one of EVENT_{SUCCESS|FAILURE}
     * @param taskType Task type, one of TASK_{NOTIFICATION|SUBSCRIPTION}
     * @param results List of result objects
     * @exception If error occurs during invocation
     */
    
    protected void invokeHandlers(int eventType, int taskType, List results)
                                                            throws Exception
    {
        this._handlerTable.handleEvent(eventType, taskType, results);
        
        //add result to history
        int count = results.size();
        for (int i = 0; i < count; ++i)
        {
            addToHistory(results.get(i));
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Add record of file involved in task.
     * @param record Record object with file information
     */
    
    protected void addToHistory(Object record)
    {
        this._history.add(record);
        firePropertyChangeAsync(MetaSubscription.PROPERTY_HISTORY, 
                                null, this._history);
    }

    //---------------------------------------------------------------------
    
    /**
     * Clears history object of records what files have been
     * handled by the meta-subscription.
     */
    
    public void clearHistory()
    {
        this._history.clear();
        firePropertyChangeAsync(MetaSubscription.PROPERTY_HISTORY, 
                                null, this._history);
    }
    
    //---------------------------------------------------------------------
    
    protected void firePropertyChangeSync(final String name, 
                                          final Object oldValue, 
                                          final Object newValue)
    {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _changes.firePropertyChange(name, oldValue, newValue);
                }
            });
        } catch (Exception ex) {
            _logger.error("Error occurred while firing property change "
                    + " for this metasubscription.  Property name: "
                    + name);
            _logger.debug(ex.getMessage(), ex);
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void firePropertyChangeAsync(final String name,
                                           final Object oldValue, 
                                           final Object newValue)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                _changes.firePropertyChange(name, oldValue, newValue);
            }
        });
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true iff this and other have same task type, source,
     * and target.
     * @param other Another instance of MetaSubscription to compare
     *              equivalence with.
     * @return True iff this and other are equivalent, false otherwise
     */
    
    public boolean equivalentTo(MetaSubscription other)
    {
        if (other == null)
            return false;
     
        //check task type
        if (this.getTaskType() != other.getTaskType())
            return false;
        
        //check source
        if ((this._source == null && other.getSource() != null) ||
            (this._source != null && !this._source.equals(other.getSource())))
                return false;
        
        //check target
        if ((this._target == null && other.getTarget() != null) ||
            (this._target != null && !this._target.equals(other.getTarget())))
            return false;
        
        return true;
    }
    
    //---------------------------------------------------------------------
    
    public int compareTo(Object object)
    {
        int val = 0;
        
        if (!(object instanceof MetaSubscription))
            return val;
        
        MetaSubscription other = (MetaSubscription) object;
        
        val = this._subscriptionId - other.getId();
        
        return val;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns name of metasubscription.  Usually includes the id, source,
     * and target, when defined.
     * @return Name of metasubscription
     */
    
    public String getName()
    {
        if (this._name == null)
        {
            this._name = SubscriptionConstants.taskTypeToString(getTaskType());
            this._name += " [Id = "+this._subscriptionId;
            if (this._source != null)
                this._name +=  ", Source = " + this._source;
            this._name += "]";
        }
        
        return this._name;
    }
    
    //---------------------------------------------------------------------
    
    protected void initMailLoggers(String mesgLoggerName, String mesgTitle,
                                   String rprtLoggerName, String rprtTitle,
                                   String rprtHeader)
    {
        // retrieve mail settings
        String mailMsgTo, mailMsgFrom, mailHost, mailRptTo, mailRptAt;
        boolean mailSilReconn;
        
        mailMsgFrom = (String) _parameters.get(SubscriptionConstants.
                                               KEY_MAIL_MESSAGE_FROM);
        mailMsgTo   = (String) _parameters.get(SubscriptionConstants.
                                               KEY_MAIL_MESSAGE_TO);
        mailHost    = (String) _parameters.get(SubscriptionConstants.
                                               KEY_MAIL_SMTP_HOST);
        mailRptTo   = (String) _parameters.get(SubscriptionConstants.
                                               KEY_MAIL_REPORT_TO);
        mailRptAt   = (String) _parameters.get(SubscriptionConstants.
                                               KEY_MAIL_REPORT_AT);
        
        Boolean tmpBool = (Boolean) _parameters.get(SubscriptionConstants.
                                             KEY_MAIL_SILENT_RECONN);
        
        mailSilReconn = tmpBool == null ? false : tmpBool.booleanValue();
        
        
        
        //both report and message require from and host
        if (mailMsgFrom != null && mailHost != null)
        {
            //check for mail setting
            if (mailMsgTo != null)
            {
                this._mailMessage = true;
                this._emailMessageLogger = Logger.getLogger(mesgLoggerName);
                this._emailMessageLogger.setMail(mailMsgFrom, mailMsgTo,
                                                 mailHost, mesgTitle);
                this._emailMessageLogger.enableSendMail();
                
                //append the our email message logger to the throttler
                //so it can notify mail listener of throttle timeouts
                if (this._throttle != null)
                    _throttle.addLogger(this._emailMessageLogger);
            }
        
            if (mailSilReconn)
            {
                this._mailSilentReconn = true;
            }
            
            // create mail report scheduler
            if (mailRptTo != null && mailRptAt != null)
            {
                this._mailReport = true;
                this._emailReportLogger = Logger.getLogger(rprtLoggerName);
                
                if (this._reportScheduler != null)
                {
                    this._reportScheduler.cancel();
                }
                _reportScheduler = new Timer();
                this._emailReportLogger.setMail(mailMsgFrom, mailRptTo,
                                                mailHost, rprtTitle);
                this._emailReportLogger.enableSendMail();

                Date[] dateList = parseTimeList(mailRptAt);
                if (dateList == null || dateList.length == 0) 
                {
                      this._logger.error(getName() + ": " 
                                  + SubscriptionConstants.ERROR_TAG 
                                  + " Invalid time format for \""
                                  + SubscriptionConstants.KEY_MAIL_REPORT_AT 
                                  + "\".");
                      this._logger.error("Ignore report mailing request. " +
                            " Please use \"hh:mm am|pm, hh:mm am|pm, ...\"");
                      _reportScheduler.cancel();
                      this._mailReport = false;
                 } 
                else 
                {                    
                    //create tasks to be run at given times per day
                    for (int i = 0; i < dateList.length; ++i) 
                    {
                        this._logger.debug("scheduling task for " + dateList[i]);
                        _reportScheduler.scheduleAtFixedRate(
                                            new ScheduledMailer(rprtHeader), 
                                            dateList[i], 
                                            SubscriptionConstants.DAY_MS);
                   }
                }
            }
        }
    }

    //---------------------------------------------------------------------
    
    protected void cleanupMailLoggers()
    {
        //cancel scheduler
        if (this._reportScheduler != null)
            this._reportScheduler.cancel();
        
        //disable mailers
        if (this._emailMessageLogger != null)
        {
            this._emailMessageLogger.disableSendMail();
            this._emailMessageLogger = null;    
        }
        if (this._emailReportLogger != null)
        {
            this._emailReportLogger.disableSendMail();        
            this._emailReportLogger = null;
        }
    }
    
    //---------------------------------------------------------------------

    protected class ScheduledMailer extends TimerTask 
    {
        protected String _title;

        public ScheduledMailer(String title)
        {
            this._title = title == null ? "" : title;
        }        
       public void run() {
          String message = null;
          synchronized (_reportBuffer) 
          {
             message = _reportBuffer.toString();
             _reportBuffer = new StringBuffer();
          }
          if (message == null)
             return;
          if (message.trim().length() == 0)
             return;

          message = _title + "\n" + message;
          _emailReportLogger.info(message);
       }
    }
    
    //---------------------------------------------------------------------
    
    protected void logToReport(String msg)
    {
        if (this._mailReport) {
            synchronized (this._reportBuffer) {
               this._reportBuffer.append(msg + "\n");
            }
         }
    }
    
    //---------------------------------------------------------------------

    /**
     * Translates from string representation of time to Date object.
     * @param time Time as string, format hh:mm
     * @param pm flag indicating time is in PM
     * @return Date object represented by parameters
     */

    public static Date getDate(String time, boolean pm) {
       Date now = new Date();

       String[] tlist = time.split(":");
       int hour = Integer.parseInt(tlist[0].trim());
       int minute = Integer.parseInt(tlist[1].trim());

       //if 12 am, convert hour to 0
       if (hour == 12 && !pm)
       {
           hour = 0;
       }
       else if (pm)
       {
           //if pm, then add 12 hours
          hour += 12;
       }

       Calendar cal = Calendar.getInstance();
       cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal
             .get(Calendar.DAY_OF_MONTH), hour, minute);

       Date then = cal.getTime();

       if (then.before(now)) {
          long ll = then.getTime() + 86400000;
          then = new Date(ll);
       }
       return then;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns an array of Date objects created from the timelist parameter.
     * @param timelist String of date list delimited by comma (,), where each
     *           entry is of the form 'hh:mm [ap]m'
     * @return Array of Date corresponding to the timelist
     */

    public static Date[] parseTimeList(String timelist) {
       timelist = timelist.toLowerCase();

       StringTokenizer st = new StringTokenizer(timelist, ",");
       Date[] dateList = new Date[st.countTokens()];
       int i = 0;
       while (st.hasMoreTokens()) {
          String time = st.nextToken();
          boolean pm = false;
          
          if (time.endsWith("pm"))
              pm = true;
          if (time.endsWith("am") || pm) {
             time = time.substring(0, time.indexOf('m') - 1).trim();
             Date d = getDate(time, pm);
             if (d != null) {
                dateList[i++] = d;
             }
          }
       }
       return dateList;
    }

    
    //---------------------------------------------------------------------
    
}
