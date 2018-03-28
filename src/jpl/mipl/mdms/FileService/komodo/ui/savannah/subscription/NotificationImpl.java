package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.util.InvocationCommandUtil;
import jpl.mipl.mdms.FileService.komodo.util.PushFileEventQueue;
import jpl.mipl.mdms.FileService.komodo.util.PushSubscriptionClient;
import jpl.mipl.mdms.FileService.komodo.util.SubscriptionEvent;
import jpl.mipl.mdms.FileService.komodo.util.SubscriptionEventListener;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.Errno;
import jpl.mipl.mdms.FileService.util.SystemProcess;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Notification implementation for new files.  Constructor takes
 * a reference to a SavannahModel for handling transfer of 
 * files.
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
 * 01/14/2005        Nick             Initial Release
 * 09/28/2005        Nick             Added push capability.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: NotificationImpl.java,v 1.14 2017/01/13 00:59:19 ntt Exp $
 *
 */

public class NotificationImpl extends DefaultMetaSubscription
{
    private final String __classname = this.getClass().getName();
    
    protected SavannahModel _model;
    protected String        _fileType;
    
    //shadow _parameters from super class
    protected NotificationParameters _parameters = null;
    
    private Logger _logger = Logger.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------
    
    /**
     * Constructor using a parameters argument to set state.
     * @param parameters Instance of NotificationParameters
     */
    
    public NotificationImpl(NotificationParameters parameters) 
    {
        this(parameters, null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor using a parameters argument to set state.
     * @param parameters Instance of NotificationParameters
     * @param model Instance of SavannahModel.  Used to notify model
     *              of transfer activity.  Can be <code>null</code>. 
     */
    
    public NotificationImpl(NotificationParameters parameters, 
                            SavannahModel model) 
    {
        super(parameters);
        
        //shadow super's parameters
        this._parameters = parameters;
        
        //super() should have init'd source
        if (!(this._source instanceof String))
        {
            throw new IllegalArgumentException(__classname+"::constructor::"+
                                           "Source must be of type String.");
        }
        this._fileType = (String) this._source;
        
        //set model reference
        this._model = model;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Implementation of the run() method.
     */
    
    public void run()
    {
        //we're off and running, make it heard
        String msg;
        setState(SubscriptionConstants.STATE_RUNNING);
        this._startTime = System.currentTimeMillis();
        
        try {
            //do the work
            if (this._parameters.getPush())
                runNotifyPush();
            else
                runNotifyPull();
            
            //we're done, set state accordingly
            setState(SubscriptionConstants.STATE_TERMINATED);
            msg = getName()+": Terminated normally.";
            this._logger.info(msg);
            if (this._mailMessage && this._emailMessageLogger != null)
                this._emailMessageLogger.info(msg);
            
        } catch (SessionException sesEx) {
            //session error
            setState(SubscriptionConstants.STATE_ERROR);
            msg = getName()+": Session error occurred.  "
                            + "Message = " + sesEx.getMessage();
            this._logger.error(msg);
            this._logger.debug(sesEx.getMessage(), sesEx);
            if (this._mailMessage && this._emailMessageLogger != null)
                this._emailMessageLogger.error(msg);
        } catch (Exception ex) {
            //general error
            setState(SubscriptionConstants.STATE_ERROR);
            msg = getName()+": Error occurred.  "
                            + "Message = " + ex.getMessage();
            this._logger.error(msg);
            this._logger.debug(ex.getMessage(), ex);
            if (this._mailMessage && this._emailMessageLogger != null)
                this._emailMessageLogger.error(msg);
        }
        
        //kill reporter and record end time
        this.cleanupMailLoggers();
        this._endTime = System.currentTimeMillis();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Application method to subscribe to new file available event from the
     * server.
     * @return true if success, false otherwise
     * @throws SessionException if error occurs during processing
     */
    
    protected boolean runNotifyPull() throws SessionException
    {
        long queryInterval = this._queryInterval == null ? 
                             SubscriptionConstants.MINUTE_MS :
                             Long.parseLong(this._queryInterval)
                             * SubscriptionConstants.MINUTE_MS;
        String serverGroup = FileType.extractServerGroup(
                                    _parameters.getFiletype());
        long sleeptime = queryInterval;

        //-------------------------
        
        //initialize client 
        this._logger.debug(getName() + ": Initializing client...");
        initClient();
        
        if (this._client == null)
            throw new SessionException("Cannot establish connection " +
                                       "to server", Constants.IO_ERROR);
        else
            this._logger.debug(getName() + ": Client initialized.");
        
        //-------------------------
        
        //prepare log file
        String logfile = _parameters.getLogFilename();
        if (logfile != null)
        {
            String rolling = _parameters.getLogfileRolling();
            int rollType = SubscriptionConstants.logRollStringToType(rolling);
            this._logger.enableLogToFile(logfile, rollType);            
        }

        //-------------------------
        
        //prepare mail loggers
        initMailLoggers("Notification.Email.Message."+this._subscriptionId, 
                        "Re: FEI5 Notification Message ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "Notification.Email.Report."+this._subscriptionId,
                        "Re: FEI5 Notification Report ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "[NOTIFICATION REPORT]");
        
        //-------------------------
        
        //load invoke string
        String invoke = _parameters.getInvokeCommand();
        if (invoke != null)
        {
            invoke.trim();
            if (invoke.length() == 0)
                invoke = null;
        }

        //set exitOnError flag
        boolean exitOnError = false;
        if (invoke != null && _parameters.getInvokeExitOnError())
            exitOnError = true;
        
        //-------------------------
        
        /*
         * here is the algorithm - Since we are relying on client to automatic
         * query, restart (in the client API) should be always enabled. The
         * 'restart' command option translates to restart from the latest
         * queried time. If the user did not specify this option, then it should
         * use the default restart time, that is the current time, and persist
         * the time for each file received.
         */
        Date queryTime = null;
        if (_parameters.getRestart())
        {
            queryTime = new Date();
        }

        String msg = getName() + ": Subscribing to [" + 
                     _parameters.getFiletype() + "] file type.";

        //log message that subscription is started
        this._logger.info(msg);
        if (this._mailMessage)
            this._emailMessageLogger.info(msg);
        
        boolean success = true;
        boolean newconnection = false;

        this._logger.debug(getName() + ": Entering polling loop...");
        
        //enter loop
        while (true)
        {
            if (!canProceed())
                break;
            
            this._logger.debug(getName() +": Checking for new files...");             
            this.setState(SubscriptionConstants.STATE_BUSY);            
            long issueTime = System.currentTimeMillis();
            
            int tranId = this._client.showAfter(queryTime);
            newconnection = false;
            
            // reset the epoch to null to trigger client API to
            // use the last queried time.
            queryTime = null;
            boolean timeToExit = false;

            //handle files
            while (this._client.getTransactionCount() > 0)
            {
                Result result = this._client.getResult(SubscriptionConstants.
                                                       GET_RESULT_TIME_OUT_MS);
                if (result == null)
                {
                    if (isTerminated())
                        break;
                    else
                        continue;
                }
                else if (result.getErrno() == Constants.NO_FILES_MATCH)
                {
                    // no new files at this time
                    continue;
                }
                else if (result.getErrno() == Constants.OK)
                {
                    msg = getName()
                            + ": available \""
                            + result.getName()
                            + "\" "
                            + result.getSize()
                            + " bytes "
                            + DateTimeUtil.getDateCCSDSAString(result
                                    .getFileModificationTime());
                    if (result.getChecksumStr() != null)
                        msg += (" CRC:" + result.getChecksumStr());

                    this._logger.info(msg);
                    logToReport(msg);
                    
                    // handle invoke
                    if (invoke != null)
                    {
                        String name = result.getName();
                        String cmdStr = InvocationCommandUtil.buildCommand(
                                        invoke, this._client.getDir(), result);
                        
                        msg = getName() + ": Invoke command \"" + 
                               cmdStr.trim() + "\"";
                        this._logger.info(msg);
                        logToReport(msg);
                        
                        Errno errno = SystemProcess.execute(cmdStr, 
                                                            this._logger);
                        if (errno.getId() != Constants.OK)
                        {
                                                        
                            String errnoReport = "(errno = "+errno.getId();
                            if (errno.getMessage() != null)
                                errnoReport += "; errmsg = "+errno.getMessage();
                            errnoReport += ")";                           
                            
                            msg = SubscriptionConstants.ERROR_TAG + " "
                                    + getName() + ": invoke process \""
                                    + cmdStr + "\" failed. "+errnoReport;
                            this._logger.error(msg);
                            if (this._mailMessage)
                                this._emailMessageLogger.error(msg);
                            
                            
                            if (exitOnError)
                            {
                                this._client.logout();
                                msg = SubscriptionConstants.ERROR_TAG + " "
                                        + getName() + ": Exiting.";
                                this._logger.error(msg);
                                if (this._mailMessage)
                                    this._emailMessageLogger.error(msg);
                                
                                timeToExit = true;
                                break;
                            }
                        }
                    }
                    
                    //run handlers
                    List resultList = new ArrayList();
                    resultList.add(result);
                    
                    try {
                        invokeHandlers(
                             SubscriptionConstants.EVENT_SUCCESS, 
                             SubscriptionConstants.TASK_NOTIFICATION,
                             resultList);
                    } catch (Exception ex) {
                        msg = SubscriptionConstants.ERROR_TAG + " "
                            + getName() + ": Handlers failed with \""
                            + " message: "+ ex.getMessage() + ".";
                        this._logger.error(msg);    
                        if (this._mailMessage)
                            this._emailMessageLogger.error(msg);
                    }                    
                    result.commit();
                    continue;
                }
                else if (result.getErrno() == Constants.IO_ERROR)
                {
                    int preRepairState = this.getState();
                    this.setState(SubscriptionConstants.STATE_REPAIR);
                    
                    msg = SubscriptionConstants.ERROR_TAG + " "
                            + getName() + ": Lost connection to server.  "
                            + "Attempting restart...";
                    this._logger.error(msg);
                    if (this._mailMessage && !this._mailSilentReconn)
                        this._emailMessageLogger.error(msg);
                    
                    initClient();
                    
                    if (this._client == null)
                    {
                        msg = SubscriptionConstants.ERROR_TAG + " " 
                                + getName() + ": ";
                        String ms2 = "Unable to restart connection to ["
                                        + serverGroup + ":"
                                        + result.getType() + "].";
                        msg += ms2;
                        this._logger.error(msg);
                        if (this._mailMessage)
                            this._emailMessageLogger.error(msg);
                        throw new SessionException(ms2, Constants.IO_ERROR);
                    }
                    
                    this.setState(preRepairState); //return to state
                    msg = getName() + ": Restored notification session to ["
                            + serverGroup + ":" + result.getType() + "].";
                    this._logger.info(msg);  
                    if (this._mailMessage && !this._mailSilentReconn)
                        this._emailMessageLogger.info(msg);
                    newconnection = true;
                    break;
                }
                else
                {
                    msg = SubscriptionConstants.ERROR_TAG + " "
                            + getName() + ": "
                            + result.getMessage();
                    this._logger.error(msg);
                    this._logger.debug("ERRNO = " + result.getErrno());
                    if (this._mailMessage)
                        this._emailMessageLogger.error(msg);
                    
                    //run handlers
                    List resultList = new ArrayList();
                    resultList.add(result);                    
                    try {
                        invokeHandlers(
                             SubscriptionConstants.EVENT_FAILURE, 
                             SubscriptionConstants.TASK_NOTIFICATION,
                             resultList);
                    } catch (Exception ex) {
                        msg = SubscriptionConstants.ERROR_TAG + " "
                            + getName() + ": Handlers failed with \""
                            + " message: "+ ex.getMessage() + ".";
                        this._logger.error(msg);    
                        if (this._mailMessage)
                            this._emailMessageLogger.error(msg);
                    }                    
                    result.commit();
                    continue;
                }
            } //end_while_trans_count

            if (timeToExit)
                break;
            
            //done processing, set to running state
            this.setState(SubscriptionConstants.STATE_RUNNING);
            
            if (!newconnection)
            {
                long processTime = System.currentTimeMillis() - issueTime;
                sleeptime = processTime > queryInterval ? 0 : 
                            queryInterval - processTime;
                long wakeupTime = System.currentTimeMillis() + sleeptime;
                this._logger.trace("wakeup time = " + wakeupTime);
                while (System.currentTimeMillis() < wakeupTime  
                        && !isTerminated())
                    Thread.yield();
            }
        }
        
        //-------------------------
        
        //logout of client
        if (this._client != null && this._client.isLoggedOn())
        {
            this._client.logoutImmediate();
            while (this._client.getTransactionCount() > 0)
                this._client.getResult();
        }

        //-------------------------
        
        return success;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Push implemenation of subscription.  
     */
    
    protected boolean runNotifyPush() throws SessionException
    {
       long sleeptime = 1000; //1 second
       String ftString = this._fileType;
       boolean success = true;
        
       try {
                
        //---------------------------
        
        //prepare log file
        String logfile = _parameters.getLogFilename();
        if (logfile != null)
        {
            String rolling = _parameters.getLogfileRolling();
            int rollType = SubscriptionConstants.logRollStringToType(rolling);
            this._logger.enableLogToFile(logfile, rollType);
        }
        
        //prepare mail logging
        initMailLoggers("Notification.Email.Message."+this._subscriptionId, 
                        "Re: FEI5 Notification Message ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "Notification.Email.Report."+this._subscriptionId,
                        "Re: FEI5 Notification Report ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "[NOTIFICATION REPORT]");
        
        //---------------------------
        
        //get the invoke command string
        String invoke = this._parameters.getInvokeCommand();
        if (invoke != null) {
           invoke.trim();
           if (invoke.length() == 0)
              invoke = null;
        }

        //set exit on error flag
        boolean exitOnError = false;
        if (invoke != null && this._parameters.getInvokeExitOnError())
           exitOnError = true;
        
        //---------------------------
        
        //set output dir
        String outputDir = this._parameters.getOutputDirectory();

        //---------------------------
        
        String msg = "FEI5 Information on "
              + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
              + "Subscribing to [" + ftString + "] file type.\n";

        if (this._mailMessage)
           this._emailMessageLogger.info(msg);
        else
           this._logger.info(msg);

        //---------------------------
        //construct a subscription client that will generate new file events
        //final List newFileResults = new Vector();     
        final PushFileEventQueue<Result> newResultQueue = 
                                    new PushFileEventQueue<Result>();
        final Map clientOptions = translatePreferences();
                
        try {
            _subscriptionClient = new PushSubscriptionClient(
                                      this._parameters.getDomainFile(), 
                                      clientOptions, 
                                      Constants.AUTOSHOWFILES, true);
        } catch (SessionException sesEx) {
            msg = "Unable to construct subscription client.  Aborting...";
            this._logger.error(msg);
            this._logger.debug(null, sesEx);
            throw sesEx;
        }
        
        //---------------------------
    
        //construct subscription event listener that adds new files to local
        //file collection
        SubscriptionEventListener subListener = new SubscriptionEventListener() {          
            public void eventOccurred(SubscriptionEvent event)
            {
                _logger.trace("Received new notification event");              
                Object obj = event.getObject();
                
                if (obj instanceof Result)
                {
                    //get the filename and add it to our queue!
                    Result result = (Result) obj;
                             
                    //test if this file should be retrieve or not
                    String filename = result.getName();    
                    synchronized(newResultQueue)
                    {
                        newResultQueue.addItem(result);
//                        if (!newFileResults.contains(result))
//                            newFileResults.add(result);                    
                    }
                }
            }          
        };
        
        //add anon. listener to client
        _subscriptionClient.addSubscriptionEventListener(subListener);
        
        //---------------------------
        
        //When termination state is set, listener will invoke close
        //on the push subscription client, closing that connection.
        PropertyChangeListener killReactor = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pce)
            {
                String propName = pce.getPropertyName();
                if (propName.equals(MetaSubscription.PROPERTY_STATE))
                {
                    int newState = ((Integer) pce.getNewValue()).intValue();                    
                    if (newState == SubscriptionConstants.STATE_TERMINATED)
                    {
                        if (_subscriptionClient.isAlive())
                        {                           
                            _logger.debug("Closing push subscription client...");
                            _subscriptionClient.close();
                        }
                    }
                }                
            }
        };
        this.addPropertyChangeListener(killReactor);
        
        //---------------------------
        
        //launch subscription client on own thread
        Thread subThread = new Thread(_subscriptionClient);
        subThread.setName("Notification_Thread_"+ftString+"_"
                          +this._subscriptionId);
        subThread.start();
        
        //---------------------------
        
        //enter loop
        while (_subscriptionClient.isAlive()) 
        {
          
           //---------------------------
            
           if (!canProceed())
               break;
            
           //---------------------------
            
           long issueTime = System.currentTimeMillis();
                                 
           //lock newFiles collection, create array of filenames from
           //contents. 
           Result[] resultArray = null;
           synchronized(newResultQueue)
           {
               newResultQueue.advanceQueue();
               List<Result> resultList = newResultQueue.getItemsInProcess();
               resultArray = resultList.toArray(new Result[0]);
//               int size = newFileResults.size();
//               resultArray = new Result[0];
//               resultArray = (Result[]) newFileResults.toArray(resultArray);
           } //end synch
           
           // we're gonna be busy !
           if (resultArray != null && resultArray.length > 0)
           {
               this.setState(SubscriptionConstants.STATE_BUSY);
           }

           for (int i = 0; i < resultArray.length; ++i)
           {
               Result result = resultArray[i];
            
               boolean proceed =  handleNewFile(result, outputDir,  
                                                invoke, exitOnError, 
                                                Constants.AUTOSHOWFILES);
               if (!proceed)
                   break;
                   
              result.commit();
              newResultQueue.removeItem(result);
              //newFileResults.remove(result);  //synch'ed Vector
              
              
           } //end_for_loop
                      
           //---------------------------
           
           //done for now, revert to running state
           this.setState(SubscriptionConstants.STATE_RUNNING);

           //---------------------------

           // check if we should nap
           this._logger.debug("Waiting for new files...");
           try {
              //assume we are gonna sleep...
              boolean shouldSleep = true;
              while(shouldSleep)
              {
                  //check to see if we really should sleep
                  synchronized(newResultQueue) {shouldSleep = newResultQueue.isEmpty();}
                  shouldSleep = shouldSleep && _subscriptionClient.isAlive();
                  if (shouldSleep)
                      Thread.sleep(sleeptime);
              }
          } catch (InterruptedException e) {
             break; // exit the infinite loop and return
          }
           
        } //end_while_subclient_alive

        //---------------------------
        
      } finally {
          //logout of client
          if (this._client != null && this._client.isLoggedOn())
          {
              this._client.logout();
          }
          
          // close subscription client
          if (_subscriptionClient != null && _subscriptionClient.isAlive())
          {
              _subscriptionClient.close();
          }
      }
        
      //---------------------------
      
      return success;
        
    }
    
    //---------------------------------------------------------------------
    
    protected void applyClientParameters() throws SessionException
    {
        super.applyClientParameters();
        if (this._client == null)
            return;
        
        try {                 
            this._client.set(Client.OPTION_RESTART, true);
        } catch (SessionException sesEx) {
            //sesEx.printStackTrace();
            throw sesEx;
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the task type of this metasubscription.  Specifically,
     * returns the value SubscriptionConstants.TASK_NOTIFICATION.
     * @return SubscriptionConstants.TASK_NOTIFICATION
     */
    
    public int getTaskType()
    {
        return SubscriptionConstants.TASK_NOTIFICATION;
    }
    
    //=====================================================================
    //=====================================================================
    
}
