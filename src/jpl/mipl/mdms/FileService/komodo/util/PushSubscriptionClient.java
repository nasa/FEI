package jpl.mipl.mdms.FileService.komodo.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.client.CMD;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Creates and maintains client connection with server, listening for
 * new file events as part of the server's push event mechanism.
 * 
 * Listeners can be attached by implementing the SubscriptionEventListener
 * interface.  
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
 * 05/27/2005        Nick             Initial Release
 * 09/16/2005        Nick             Generalized to allow for subscription
 *                                    and notification.  Also includes read
 *                                    or read/write state for cache file.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: PushSubscriptionClient.java,v 1.10 2009/08/07 01:00:48 ntt Exp $
 *
 */

public class PushSubscriptionClient implements Runnable
{
    private static final long _MINUTE_MS = 60000;
    
    protected long    _lastActiveTime;
    protected long    _keepAliveTimeout = Constants.KEEPALIVETIMEDEFAULT;
    protected Client  _client;
    protected URL     _domainFile;
    protected Map     _clientOptions;
    protected int     _errorCount = 0;
    protected boolean _canCommit = false;
    protected String  _operation;
    protected boolean _continue = true;
    
    /** Event queue */
    protected final List _queue;
    
    /** Listener list */
    protected final List _listeners;
    
    /** Logger instance */
    private final Logger _logger = Logger.getLogger(this.getClass().getName());
    
    //blocking times for client creation
    protected final long DEFAULT_WAIT_TIME = 1000 * 60 * 30; //30 minutes
    public static final long BLOCK_INDEFINITELY = -1L;
    public static final long BLOCK_NEVER        = -2L;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param domainFile Path of domain file
     * @param clientOptions Map of client options
     * @throws SessionException if clientOptions does not contain
     *         required options.
     */
    
    public PushSubscriptionClient(URL domainFile, Map clientOptions, 
                                  String operation) throws SessionException
    {
        this(domainFile, clientOptions, operation, false);
    }

    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param domainFile Path of domain file
     * @param clientOptions Map of client options
     * @param Flag indicating whether result information will be persisted
    *         to restart cache
     * @throws SessionException if clientOptions does not contain
     *         required options.
     */
    
    public PushSubscriptionClient(URL domainFile, Map clientOptions,
                                  String operation, boolean commitResults)
                                                throws SessionException
    {
        this._queue            = new Vector();
        this._listeners        = new Vector();
        this._domainFile       = domainFile;
        this._clientOptions    = clientOptions;
        this._canCommit        = commitResults;
        this._operation        = operation;
        this._keepAliveTimeout = Constants.KEEPALIVETIMEDEFAULT;
        
        //verify that we have the options we need
        if (!checkOptions())
            throw new SessionException("Client options missing required " +
                                       "values", Constants.NOT_SET);            
        
        //check system properties for potential overrides
        checkOverrides();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Checks that all required options for creating a client are set.
     * Returns false if one more more are missing, true otherwise.
     * @return True if all options set, false otherwise.
     */
    
    protected boolean checkOptions()
    {
        return (this._clientOptions.get(CMD.USER)        != null &&
                this._clientOptions.get(CMD.PASSWORD)    != null &&
                this._clientOptions.get(CMD.SERVERGROUP) != null &&
                this._clientOptions.get(CMD.FILETYPE)    != null &&
                this._clientOptions.get(CMD.OUTPUT)      != null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Checks overrides to default values.
     */
    
    protected void checkOverrides()
    {
        //-------------------------
        
        //check keep alive time (in seconds)
        String value = System.getProperty(Constants.PROPERTY_KEEP_ALIVE);
        if (value != null)
        {
            try {
                int intVal = Integer.parseInt(value) * 1000;
                setKeepAliveTime(intVal);
            } catch (Exception ex) {
                //number format or session ex, do nothing
                _logger.warn("Unable to use keep alive override: "+value);
                _logger.debug(null, ex);
            }
        }
        
        //-------------------------
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a subscription listener to this client.
     * @param l New instance of SubscriptionEventListener
     */
    
    public void addSubscriptionEventListener(SubscriptionEventListener l)
    {
        synchronized(this._listeners)
        {
            if (l != null && !this._listeners.contains(l))
                this._listeners.add(l);
        }
    }
    
    //---------------------------------------------------------------------
     
    /**
     * Removes a subscription listener from this client.
     * @param l Instance of SubscriptionEventListener
     */
    
    public void removeSubscriptionEventListener(SubscriptionEventListener l)
    {
        synchronized(this._listeners)
        {
            if (l != null && this._listeners.contains(l))
                this._listeners.remove(l);
        }
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * Set the keep alive time, which is the maximum amount of time client
     * will wait for a reply from server.  If that time elaspses with no
     * reply, a ping message will be sent to server to preserve connection.
     * @param timeout New timeout value (in milliseconds), between range of 
     *        KEEPALIVETIMEMIN and KEEPALIVETIMEMAX as defined in Constants.
     * @throws SessionException if timeout parameter is not without range
     */
    
    public void setKeepAliveTime(long timeout) throws SessionException
    {
        if (timeout < Constants.KEEPALIVETIMEMIN &&
            timeout > Constants.KEEPALIVETIMEMAX)
            throw new SessionException("Timeout must be between " + 
                                       Constants.KEEPALIVETIMEMIN + "and " + 
                                       Constants.KEEPALIVETIMEMAX, 
                                       Constants.INVALIDTIMEOUT);
            
        this._keepAliveTimeout = timeout;
    }

    //---------------------------------------------------------------------

    /**
     * Returns the keep alive time value, in milliseconds
     * @return Keep alive time
     */
    
    public long getKeepAliveTime()
    {            
        return this._keepAliveTimeout;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Fires subscription events for list of listeners
     * @param obj Instance of object to wrap with event
     */
    
    protected void fireNewFileEvent(Object obj)
    {
        SubscriptionEvent event = null;
        
        synchronized(this._listeners)
        {
            int numListeners = this._listeners.size();
            this._logger.trace("Firing subscription event for "
                               + numListeners + " listener(s)...");
            
            for (int i = 0; i < numListeners; ++i)
            {
                event = new SubscriptionEvent(this, SubscriptionEvent.
                                              ID_NEW_FILE, obj);
                ((SubscriptionEventListener) this._listeners.get(i)).
                                                eventOccurred(event);
            }
        }
    }

    //---------------------------------------------------------------------
    
    /**
     * Creates a new client reference using the client options.
     * This method can block until a client instance is created
     * @param waitTime If positive, the number maximum number of 
     * milliseconds to wait for client. Otherwise, can be 
     * BLOCK_INDEFINITELY or BLOCK_NONE.
     * @return New client instance, or null if max sleep time reached
     */
    
    protected Client createClient(long waitTime)
    {
        //set quitTime if positive value supplied
        boolean block = true;
        long quitTime = BLOCK_INDEFINITELY;
        
        if (waitTime >= 0L)
        {
            block = false;
            quitTime = System.currentTimeMillis() + waitTime;
        }
        else if (waitTime == BLOCK_NEVER)
        {
            block = false;
            quitTime = System.currentTimeMillis();
        }
        
        long minSleepTime = 60 * 1000; //one minute
        long maxSleepTime = minSleepTime * 16;
        long sleepTime = minSleepTime;

        Client client = null;
        while (client == null)
        {
            try {
                client = new Client(this._domainFile);
                client.login((String) this._clientOptions.get(CMD.USER),
                        (String) this._clientOptions.get(CMD.PASSWORD),
                        (String) this._clientOptions.get(CMD.SERVERGROUP),
                        (String) this._clientOptions.get(CMD.FILETYPE));
                
                String outputDir = (String) this._clientOptions.get(CMD.OUTPUT);                
                if (outputDir != null)
                    client.changeDir(outputDir);
                if (this._clientOptions.get(CMD.REPLACE) != null)
                    client.set(Client.OPTION_REPLACEFILE, true);
                if (this._clientOptions.get(CMD.VERSION) != null)
                    client.set(Client.OPTION_VERSIONFILE, true);
                if (this._clientOptions.get(CMD.CRC) != null)
                    client.set(Client.OPTION_COMPUTECHECKSUM, true);
                if (this._clientOptions.get(CMD.SAFEREAD) != null)
                    client.set(Client.OPTION_SAFEREAD, true);
                if (this._clientOptions.get(CMD.RESTART) != null)
                    client.set(Client.OPTION_RESTART, true);    
                if (this._clientOptions.get(CMD.REPLICATE) != null)
                    client.set(Client.OPTION_REPLICATE, true);
                
                //---------------------------
                
            } catch (SessionException e) {
                String msg = "FEI5 Information on "
                        + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                        + "ERROR: " + "Unable to restart metadata " 
                        + "subscription session to ["
                        + this._clientOptions.get(CMD.SERVERGROUP) + ":"
                        + this._clientOptions.get(CMD.FILETYPE)
                        + "].  Next attempt in " + (sleepTime / _MINUTE_MS)                   
                        + " minute(s).\n";
                this._logger.debug(e);
                this._logger.info(msg);
                client = null;
            }

            if (client == null)
            {
                if (!block && System.currentTimeMillis() + sleepTime > quitTime)
                    break;
                
                try {
                    Thread.sleep(sleepTime);
                    if (sleepTime < maxSleepTime)
                        sleepTime *= 2;
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return client;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates and maintains client subscription session.  Initially, a new
     * is constructed.  If this attempt fails, operation is aborted.  
     * Otherwise, a subscription session is initialized between client
     * and server via requests and results.
     * 
     * If an error occurs during the session, the client is killed and a new
     * client session is created.
     */
    
    public void run()
    {
        //initially, do not block when creating client
        long waitTime = BLOCK_NEVER; 
        boolean inited = false;
        String msg;
            
        while (this._continue)
        {
            try {
                //create new client - since we are set not to block, 
                //only one attempt should be made.  If that fails,
                //either because of user login error or network,
                //then subscription is aborted.
                this._client = createClient(waitTime);

                if (this._client == null)
                {
                    ++this._errorCount;
                    this._continue = false;
                    break;
                }
                
                if (inited)
                    this._logger.info("New client created for metadata subscription.");
                
                long issueTime = System.currentTimeMillis();

                //allow blocking period in future
                waitTime = DEFAULT_WAIT_TIME;
                inited = true;
                
                //client subscribes to server
                int tranId = this._client.subscribe(this._operation, 
                                                    this._canCommit);
                this._lastActiveTime = System.currentTimeMillis();
                
                //-------------------------
                
                while (this._client.getTransactionCount() > 0 && this._continue)
                {
                    //wait max of time out period for a result
                    int resWaitTime = (int)Math.min(Constants.RESULTMAXTIMEOUT,
                                                    this._keepAliveTimeout);  
                    Result result = this._client.getResult(resWaitTime);
                    
                    //-------------------------
                    
                    //time out occurred, check if ping needs to occur
                    if (result == null) 
                    {                        
                        if ((System.currentTimeMillis() - this._lastActiveTime)
                                                     >= this._keepAliveTimeout)
                        {                        
                            long diffSec = (System.currentTimeMillis() - 
                                           this._lastActiveTime) / 1000;
                            
                            //connection may be dead, 
                            //consider buildin' a NEW ONE! HAHAHA!
                            msg = "Timeout expired.  Client received no data " + 
                                  "from server for last " + diffSec + " seconds." +
                                  "  Will create new metadata subscription session.";
                            this._logger.error(msg);
                            this._client.logout();
                            this._client = null;
                            break;
                        }    
                        else
                            continue;
                    }
                    else //non-null, record active time
                    {
                        this._lastActiveTime = System.currentTimeMillis();
                    }
                              
                    //-------------------------

                    //handle result according to errno value
                    if (result.getErrno() == Constants.OK) 
                    {
                        handleNewFile(result);
                    }
                    else if (result.getErrno() == Constants.PING_RECVD)
                    {
                        //do nothing, we already updated the lastActiveTime
                        msg = "Received ping result.";
                        this._logger.trace(msg);
                    }
                    else if (result.getErrno() == Constants.IO_ERROR) 
                    {
                        //IO error, logout of this client      
                        msg = "Metadata subscription client received " +
                              "IO error: " + result.getMessage() + 
                              ".\nLogging out...";
                        this._logger.error(msg);
                        this._client.logout();
                        this._client = null;
                        ++this._errorCount;
                        break;
                    } 
                    else if (result.getErrno() == Constants.NACKED)
                    {
                        msg = "Metadata subscription client could not initialize "+
                              " session.  Reason: "+result.getMessage() +
                              "\nAborting...";
                        this._logger.error(msg);
                        this._continue = false;
                        ++this._errorCount;
                        break;
                    }
                    else 
                    {
                        //other error, end this subscription
                        msg = "Metadata subscription received error result.  " +
                              "Message: " + result.getMessage() + ".\n" +
                              "";
                        this._logger.error(msg);
                        ++this._errorCount;
                        break;
                    }
                    
                    //-------------------------
                    
                } //end_inner_while
            } catch (SessionException sesEx) {
                msg = "Session exception occurred during metadata subscription.  "+
                      "Message: "+sesEx.getMessage();
                this._logger.error(msg);
                
                //Should only occur if we could not establish subscription
                //session.  In this case, we abort.  Otherwise, try again.
                if (sesEx.getErrno() == Constants.NACKED)
                {
                    msg = "Aborting metadata subscription session.";
                    this._logger.error(msg);
                    this._continue = false;
                }   
                else
                {
                    msg = "Will attempt to create new metadata subscription session.";
                    this._logger.error(msg);
                }
            }            
        } //end_while_cont
        
        //-------------------------
        
        //logout of client if still logged in
        if (this._client != null && this._client.isLoggedOn())
        {
            try {
                this._client.logoutImmediate();
            } catch (SessionException sesEx) {
                msg = "Session exception occurred while attepting " +
                      "final client logout for metadata subscription.  " +
                      "Message: " + sesEx.getMessage();
                this._logger.error(msg);
            }
        }
        this._client = null;
        
        //-------------------------
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Invokes behavior to create new file event for listeners.
     * @param result Result encapsulating a new file
     */
    
    protected void handleNewFile(Result result)
    {
        //create event object, notify all listeners of it
        fireNewFileEvent(result);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sends subscription termination request and clears list of listeners. 
     * After calling this method, <code>isAlive()</code> will return false.
     */
    
    public void close()
    {
        this._continue = false;
        
        //send quit request to server to initiate shutdown
        if (this._client != null)
        {         
            try {
                this._client.stopSubscribe();
            } catch (SessionException sesEx) {
                this._logger.error("Error occurred while attempting to " +
                                   "terminate metadata subscription");
                this._logger.debug(null, sesEx);
            }
        }
        
        //remove all listeners
        synchronized (this._listeners)
        {
            this._listeners.clear();
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if this instance is still processing and session has 
     * not been closed.
     * @return True if client is still active, false otherwise.
     */
    
    public boolean isAlive()
    {
        return this._continue;
    }    
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if session requests have restart caches set.  This would
     * mean that an associated Result object would persist its state to the
     * restart cache when commit occurs. 
     * @return True if commit is enabled, false otherwise.
     */
    
    public boolean isCommitEnabled()
    {
        return this._canCommit;
    }
    
    //---------------------------------------------------------------------
}


