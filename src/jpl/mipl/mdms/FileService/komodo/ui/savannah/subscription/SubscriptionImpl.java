package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
 * Subscription implementation for new files.  Constructor takes
 * a reference to a SavannahModel for handling transfer of 
 * files.
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
 * 12/13/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionImpl.java,v 1.18 2017/01/13 00:59:19 ntt Exp $
 *
 */

public class SubscriptionImpl extends DefaultMetaSubscription
{ 
    
    /*
     Source  - type String (one of list of filetypes)
     Target  - type File
     */
    
    private final String __classname = this.getClass().getName();
    
    protected SavannahModel _model;
    protected String        _filetype;
    protected File          _directory;
    
    protected SubscriptionParameters _parameters = null;
    
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param parameters Instance of SubscriptionParameters
     */
    
    public SubscriptionImpl(SubscriptionParameters parameters) 
                                                    throws SessionException
    {
        this(parameters, null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param parameters Instance of SubscriptionParameters
     * @param model Instance of current application model
     */
    
    public SubscriptionImpl(SubscriptionParameters parameters,  
                            SavannahModel model) throws SessionException
    {
        super(parameters);

        //shadow supers params
        this._parameters = parameters;
        
        //source and target should be init'ed
        if (!(this._source instanceof String))
        {
            throw new IllegalArgumentException(__classname+"::constructor::"+
                    "Source must be of type String.");
        }
        this._filetype = (String) this._source;
        
        if (!(this._target instanceof String))
        {
            throw new IllegalArgumentException(__classname+"::constructor::"+
                    "Target must be of type String.");
        }
        this._directory = new File((String) this._target);
        
        //test that destination directory exists
        if (!_directory.isDirectory())
        {
            throw new IllegalArgumentException(__classname+"::constructor::"+
                    "Target must be a file directory.");
        }
        
        //set model reference
        this._model = model;
    }
    
    //---------------------------------------------------------------------
    
    protected void applyClientParameters() throws SessionException
    {
        super.applyClientParameters();
        
        if (this._client == null)
            return;       
            
        try {
            //crc
            this._client.set(Client.OPTION_COMPUTECHECKSUM, 
                             this._parameters.getCrc());
            //receipt
            this._client.set(Client.OPTION_RECEIPT, 
                             this._parameters.getReceipt());
            //saferead
            this._client.set(Client.OPTION_SAFEREAD,
                             this._parameters.getSaferead());
            //replace
            this._client.set(Client.OPTION_REPLACEFILE, 
                             this._parameters.getReplace());
            //version
            this._client.set(Client.OPTION_VERSIONFILE, 
                             this._parameters.getVersion());
            //diff
            this._client.set(Client.OPTION_DIFF, 
                             this._parameters.getDiff());
        } catch (SessionException sesEx) {
           // sesEx.printStackTrace();
            throw sesEx;
        }
    }
    
    //---------------------------------------------------------------------
    
    public void run()
    {
        String msg;
        setState(SubscriptionConstants.STATE_RUNNING);
        this._startTime = System.currentTimeMillis();
        
        try {
            if (this._parameters.getPush())
                runSubscribePush();
            else
                runSubscribePull();
            setState(SubscriptionConstants.STATE_TERMINATED);
            msg = getName()+": Terminated normally.";
            this._logger.info(msg);
            if (this._mailMessage && this._emailMessageLogger != null)
                this._emailMessageLogger.info(msg);
        } catch (SessionException sesEx) {
            setState(SubscriptionConstants.STATE_ERROR);
            msg = getName()+": Session error occurred.  "
                                + "Message = " + sesEx.getMessage();
            this._logger.error(msg);
            this._logger.debug(sesEx.getMessage(), sesEx);
            if (this._mailMessage && this._emailMessageLogger != null)
                this._emailMessageLogger.error(msg);
        } catch (Exception ex) {
            setState(SubscriptionConstants.STATE_ERROR);
            msg = getName()+": Error occurred.  "
                            + "Message = " + ex.getMessage();
            this._logger.error(msg);
            this._logger.debug(ex.getMessage(), ex);
            if (this._mailMessage && this._emailMessageLogger != null)
                this._emailMessageLogger.error(msg);
        }
        
        this.cleanupMailLoggers();
        this._endTime = System.currentTimeMillis();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Application method to subscribe to new file available event from
     * the server using a periodic pull-mechanism.
     * @return true if success
     * @throws SessionException
     */
    
    protected boolean runSubscribePull() throws SessionException
    {
    
        long queryInterval = this._queryInterval == null ? 
                             SubscriptionConstants.MINUTE_MS : 
                             Long.parseLong(this._queryInterval) 
                             * SubscriptionConstants.MINUTE_MS;
        String serverGroup = FileType.extractServerGroup(
                                _parameters.getFiletype());
        long sleeptime = queryInterval;

        //-------------------------
        
        //initialize the client object
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
        initMailLoggers("Subscription.Email.Message."+this._subscriptionId, 
                        "Re: FEI5 Subscription Message ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "Subscription.Email.Report."+this._subscriptionId,
                        "Re: FEI5 Subscription Report ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "[SUBSCRIPTION REPORT]");
        
        //-------------------------

        //get the invoke command string
        String invoke = this._parameters.getInvokeCommand();
        if (invoke != null)
        {
            invoke.trim();
            if (invoke.length() == 0)
                invoke = null;
        }

        //set exit on error flag
        boolean exitOnError = false;
        if (invoke != null && this._parameters.getInvokeExitOnError())
            exitOnError = true;

        //-------------------------
        
        //get settings from parser
        String outputDir = this._parameters.getOutputDirectory();
        boolean replace  = this._parameters.getReplace();
        boolean version  = this._parameters.getVersion();
        
        //check consistent state
        if (replace && version)
        {
            this._logger.error("Cannot enable replace AND version settings."
                               + "  Using version.");
            replace = false;
            this._parameters.setReplace(replace);
        }
        
        //-------------------------
        
        /*
         * Since we are relying on client to automatic query, restart (in the
         * client API) should be always enabled. The 'restart' command option
         * translates to restart from the latest queried time. If the user did
         * not specify this option, then it should use the default restart time,
         * that is the current time, and persist the time for each file
         * received.
         */
        
        Date queryTime = null;
        if (this._parameters.getRestart())
        {
            queryTime = new Date();
        }
        this._client.set(Client.OPTION_RESTART, true);

        String msg = getName() + ": Subscribing to [" 
                     + this._parameters.getFiletype() + "] file type.";

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
            
            int tranId = this._client.getAfter(queryTime);
            newconnection = false;

            // reset the epoch to null to trigger client API to
            // use the last queried time.
            queryTime = null;
            boolean timeToExit = false;

            //handle resulting files
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
                            + ": received \""
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
                                                invoke, outputDir, result);
                        
                        msg = getName() + ": Invoke command \"" 
                              + cmdStr.trim() + "\"";
                        this._logger.info(msg);
                        logToReport(msg);

                        //execute the command
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
                    
                    List resultList = new ArrayList();
                    resultList.add(result);
                    
                    try {
                        invokeHandlers(
                             SubscriptionConstants.EVENT_SUCCESS, 
                             SubscriptionConstants.TASK_NOTIFICATION,
                             resultList);
                    } catch (Exception ex) {
                        msg = SubscriptionConstants.ERROR_TAG + " "
                        + getName() + ": Handler failed with \""
                            + " message: "+ ex.getMessage() + "\".";
                        this._logger.error(msg);   
                        if (this._mailMessage)
                            this._emailMessageLogger.error(msg);
                    }
                    result.commit();
                    continue;
                }
                else if (result.getErrno() == Constants.FILE_EXISTS)
                {
                    this._logger.info(result.getMessage());
                    result.commit();
                    continue;
                }
                else if (result.getErrno() == Constants.IO_ERROR)
                {
                    int preRepairState = this.getState();
                    this.setState(SubscriptionConstants.STATE_REPAIR);
                    
                    msg = SubscriptionConstants.ERROR_TAG + " "
                            + getName() + ": Lost connection to server."
                            + "  Attempting restart...";
                    this._logger.error(msg);
                    if (this._mailMessage && !this._mailSilentReconn)
                        this._emailMessageLogger.error(msg);
                    
                    initClient();
                    
                    if (this._client == null)
                    {
                        msg = SubscriptionConstants.ERROR_TAG + " " +
                                getName() + ": ";
                        String ms2 = "Unable to restart connection to ["
                                     + serverGroup + ":"
                                     + result.getType() + "].";
                        msg += ms2;
                        this._logger.error(msg);
                        if (this._mailMessage)
                            this._emailMessageLogger.error(msg);
                        throw new SessionException(ms2, Constants.IO_ERROR);
                    }
                    
                    this._client.set(Client.OPTION_RESTART, true);
                    
                    this.setState(preRepairState); //return to state
                    msg = getName() + ": Restored subscription session to ["
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
                            + getName() + ": "+ result.getMessage();
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
                             SubscriptionConstants.TASK_SUBSCRIPTION,
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
    
    protected boolean runSubscribePush() throws SessionException
    {
       long sleeptime = 1000; //1 second
       String ftString = this._filetype;
       boolean success = true;
        
       try {
        
        //---------------------------
        
        //initialize the client object
        this._logger.debug(getName() + ": Initializing client...");
        initClient();
        
        if (this._client == null)
            throw new SessionException("Cannot establish connection " +
                                       "to server", Constants.IO_ERROR);
        else
            this._logger.debug(getName() + ": Client initialized.");
        
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
        initMailLoggers("Subscription.Email.Message."+this._subscriptionId, 
                        "Re: FEI5 Subscription Message ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "Subscription.Email.Report."+this._subscriptionId,
                        "Re: FEI5 Subscription Report ["+this._source
                        + ", "+this._subscriptionId+"]",
                        "[SUBSCRIPTION REPORT]");
        
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
        boolean replace  = this._parameters.getReplace();
        boolean version  = this._parameters.getVersion();

        //check consistent state
        if (replace && version) {
            this._logger.error("Cannot enable replace AND version settings."
                    + "  Using version.");
            replace = false;
            this._parameters.setReplace(replace);
        }


        //---------------------------
            
        this._client.set(Client.OPTION_RESTART, true);      

        //---------------------------
        
        String msg = "FEI5 Information on "
              + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
              + "Subscribing to [" + ftString + "] file type.\n";

        if (this._mailMessage)
           this._emailMessageLogger.info(msg);
        else
           this._logger.info(msg);

        //---------------------------
        
        boolean newconnection = false;

        //construct a subscription client that will generate new file events
        //final List newFiles = new Vector();
        final PushFileEventQueue<String> newFileQueue = 
                                    new PushFileEventQueue<String>(); 
        final Map clientOptions = translatePreferences();
        
        this._logger.trace("Creating new push subscription client...");        
        try {
            _subscriptionClient = new PushSubscriptionClient(
                                      this._parameters.getDomainFile(), 
                                      clientOptions, 
                                      Constants.AUTOGETFILES);
        } catch (SessionException sesEx) {
            msg = "Unable to construct subscription client.  Aborting...";
            this._logger.error(msg);
            this._logger.debug(null, sesEx);
            throw sesEx;
        }
        
        //---------------------------

        this._logger.trace("Creating new push subscription client listener...");        
        //construct subscription event listener that adds new files to local
        //file collection
        SubscriptionEventListener subListener = new SubscriptionEventListener() {          
            public void eventOccurred(SubscriptionEvent event)
            {
                _logger.trace("Received new subscription event");              
                Object obj = event.getObject();
                
                if (obj instanceof Result)
                {
                    //get the filename and add it to our queue!
                    Result result = (Result) obj;
                             
                    //test if this file should be retrieve or not
                    String filename = result.getName();    
                    synchronized(newFileQueue)
                    {
                        newFileQueue.addItem(filename);
//                        if (!newFiles.contains(filename))
//                            newFiles.add(filename);                    
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
//System.err.println("DEBUG:::: Killing push subscription client because state changed to TERMINATED...");                            
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
        subThread.setName("Subscription_Thread_"+ftString+"_"
                          +this._subscriptionId);
        subThread.start();
        
        //---------------------------
        
        //enter loop
        while (_subscriptionClient.isAlive()) {
          
           //---------------------------
//System.err.println("DEBUG:::: Waiting to proceed 1...");               
           if (!canProceed())
               break;
            
           //---------------------------
            
           long issueTime = System.currentTimeMillis();
                                 
           //lock newFiles collection, create array of filenames from
           //contents. 
           String[] files = null;
           synchronized(newFileQueue)
           {
               newFileQueue.advanceQueue();
               List<String> filenameList = newFileQueue.getItemsInProcess();
               files = filenameList.toArray(new String[0]);
               
//               int numFiles = newFiles.size();
//               if (numFiles > 0)
//               {
//                   files = new String[numFiles];
//                   for (int i = 0; i < numFiles; ++i)
//                   {
//                       
//                       files[i] = (String) newFiles.get(i);
//                   }
//               }
           }          
           
           // call 'get' using the filename array.
           if (files != null && files.length > 0)
           {
               this.setState(SubscriptionConstants.STATE_BUSY);
               this._client.get(files);
           }

           //---------------------------

           // reset the epoch to null to trigger client API to
           // use the last queried time.
           //queryTime = null;
           boolean timeToExit = false;
           newconnection = false;
           
           //---------------------------
           
           //handle resulting files
           while (this._client.getTransactionCount() > 0) {
              Result result = this._client.getResult();
              if (result == null) 
              {
                 continue;
              } 
              else if (result.getErrno() == Constants.NO_FILES_MATCH) 
              {
                 // no new files at this time
                 continue;
              } 
              else if (result.getErrno() == Constants.OK) 
              {
                  boolean proceed = handleNewFile(result, outputDir,  
                                                  invoke, exitOnError,  
                                                  Constants.AUTOGETFILES);                  
                  if (!proceed)
                      break;
                 result.commit();
                 newFileQueue.removeItem(result.getName());
                 //newFiles.remove(result.getName());  //synch'ed Vector
                 continue;
              } 
              else if (result.getErrno() == Constants.FILE_EXISTS) 
              {
                 this._logger.info(result.getMessage());
                 result.commit();
                 newFileQueue.removeItem(result.getName());
                 //newFiles.remove(result.getName());  //synch'ed Vector
                 continue;
              } 
              else if (result.getErrno() == Constants.IO_ERROR) 
              {
                 int preRepairState = this.getState();
                 this.setState(SubscriptionConstants.STATE_REPAIR);
                  
                 msg = "FEI5 Information on "
                       + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                       + SubscriptionConstants.ERROR_TAG + "Lost connection to"
                       + " [" + ftString + "].  Attempting restart...\n";
                 if (this._mailMessage && !this._mailSilentReconn)
                    this._emailMessageLogger.error(msg);
                 else
                    this._logger.error(msg);
//System.err.println("DEBUG:::: Re-initing client because of IO_ERROR...");
                 this._logger.trace("Reinitializing client because of IO error...");
                 initClient();
                 
                 //unable to get new connection
                 if (this._client == null) {
                     msg = SubscriptionConstants.ERROR_TAG + " " +
                           getName() + ": ";
                    String ms2 = "Unable to restart connection to ["
                          + ftString + "].";
                    msg += ms2;
                    this._logger.error(msg);
                    if (this._mailMessage)
                        this._emailMessageLogger.error(msg);
                    throw new SessionException(ms2, Constants.IO_ERROR);
                 }
                 
                 //got new connection!
                 this.setState(preRepairState);
                 msg = "FEI5 Information on "
                       + DateTimeUtil.getCurrentDateCCSDSAString()
                       + "\nRestored subscription session to ["
                       + ftString + "].\n";
                 if (this._mailMessage && !this._mailSilentReconn)
                    this._emailMessageLogger.info(msg);
                 else
                    this._logger.info(msg);
                 newconnection = true;
                 break;
              } 
              else if (result.getErrno() == Constants.FILE_NOT_FOUND)
              {
                  msg = "FEI5 Information on "
                      + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                      + SubscriptionConstants.ERROR_TAG + result.getMessage() + 
                      ", file not found on server.  Skipping file." + "\n";
                  if (this._mailMessage)
                      this._emailMessageLogger.error(msg);
                  this._logger.error(msg);
                  result.commit();
                  newFileQueue.removeItem(result.getName());
                  //newFiles.remove(result.getName());  //synch'ed Vector
                  continue;
              }
              else 
              {
                 msg = "FEI5 Information on "
                       + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                       + SubscriptionConstants.ERROR_TAG 
                       + result.getMessage() + "\n";
                 if (this._mailMessage)
                    this._emailMessageLogger.error(msg);
                 else
                    this._logger.error(msg);
                 this._logger.debug("ERRNO = " + result.getErrno());
                 
                 //invoke handlers
                 List resultList = new ArrayList();
                 resultList.add(result);
                 try {
                     invokeHandlers(SubscriptionConstants.EVENT_FAILURE,
                                    SubscriptionConstants.TASK_SUBSCRIPTION, 
                                    resultList);                        
                 } catch (Exception ex) {
                     msg = SubscriptionConstants.ERROR_TAG + " " 
                           + getName() + ": Handler failed with message \"" 
                           + ex.getMessage() + "\".";
                     this._logger.error(msg);
                     this._logger.debug(null, ex);
                     if (this._mailMessage)
                         this._emailMessageLogger.error(msg, ex);
                 }
                 
                 //is this error associated with a file?  If so, more processing
                 //is needed.
                 String filename = result.getName();
                 if (filename != null)
                 {
                     //if exitOnError set, then stop subscription
                     //to avoid losing any files
                     if (exitOnError) 
                     {
                        msg = "FEI5 Information on "
                              + DateTimeUtil.getCurrentDateCCSDSAString()
                              + "\n";
                        msg += SubscriptionConstants.ERROR_TAG + "Subscription"
                              + " [" + ftString + "]: Aborted.\n";
                        if (this._mailMessage)
                           this._emailMessageLogger.error(msg);
                        else
                           this._logger.error(msg);

                        timeToExit = true;
                        break;
                     }
                     //otherwise, just skip this file and log message
                     else                       
                     {
                         newFileQueue.removeItem(filename);
                         //newFiles.remove(filename);
                         msg = "FEI5 Information on "
                             + DateTimeUtil.getCurrentDateCCSDSAString()
                             + "\n";
                         msg += SubscriptionConstants.ERROR_TAG 
                             + "Subscription [" + ftString 
                             + "]: Skipping '"+filename+"'\n";
                         if (this._mailMessage)
                             this._emailMessageLogger.warn(msg);
                         else
                             this._logger.warn(msg);
                     }
                 }
                 continue;
              }
           } //end_while_transactions_exist
           
           this.setState(SubscriptionConstants.STATE_RUNNING);

           //---------------------------
           
           //if timeToExit was set, break out of outer loop
           if (timeToExit)
              break;

           //if we have the same connection, check if we should nap
           if (!newconnection) 
           {            
              this._logger.debug("Waiting for new files...");
              try {
                  //assume we are gonna sleep...
                  boolean shouldSleep = true;
                  while(shouldSleep)
                  {
                      //check to see if we really should sleep
                      synchronized(newFileQueue) {shouldSleep = newFileQueue.isEmpty();}
                      shouldSleep = shouldSleep && _subscriptionClient.isAlive();
                      if (shouldSleep)
                          Thread.sleep(sleeptime);
                  }
              } catch (InterruptedException e) {
                 break; // exit the infinite loop and return
              }
           }
        } //end_while_sub_alive

        //---------------------------
        
      } finally {
          //logout of client, close subscription client
          if (this._client != null && this._client.isLoggedOn())
          {
              this._logger.debug("Closing client...");
              this._client.logout();
          }
          
           
          if (_subscriptionClient != null && _subscriptionClient.isAlive())
          {
              this._logger.debug("Closing push subscription client...");   
              _subscriptionClient.close();
          }          
      }
        
      return success;
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the task type of this metasubscription.  Specifically,
     * returns the value SubscriptionConstants.TASK_SUBSCRIPTION.
     * @return SubscriptionConstants.TASK_SUBSCRIPTION
     */
    
    public int getTaskType()
    {
        return SubscriptionConstants.TASK_SUBSCRIPTION;
    }
    
    //=====================================================================
    //=====================================================================
}
