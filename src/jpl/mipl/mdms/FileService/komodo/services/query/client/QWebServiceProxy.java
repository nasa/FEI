package jpl.mipl.mdms.FileService.komodo.services.query.client;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.xml.ws.BindingProvider;

import jpl.mipl.mdms.FileService.komodo.api.ClientRestartCache;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QConstants;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QLoginInfo;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QRequest;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QResult;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QueryConstraints;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QueryList;
import jpl.mipl.mdms.FileService.komodo.services.query.server.QueryFault;
import jpl.mipl.mdms.FileService.komodo.services.query.server.QueryFault_Exception;
import jpl.mipl.mdms.FileService.komodo.services.query.server.QueryService;
import jpl.mipl.mdms.FileService.komodo.services.query.server.QueryServicePortType;
import jpl.mipl.mdms.FileService.komodo.services.query.util.QResultUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * This class implements the Komodo query server proxy protocol. 
 * It handles requests queued to query service muliplexed onto 
 * this proxy. Must be public to implement runnable.
 * 
 *   <PRE>
 *   Copyright 2007, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2007.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 05/17/2007        Nick             Initial release.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QWebServiceProxy.java,v 1.6 2009/08/07 01:00:48 ntt Exp $
 *
 */

public class QWebServiceProxy implements Runnable, QServiceProxy 
{    
   
    enum ProcessResultsReturnCode
    {
        NO_MORE_RESULTS, GET_MORE_RESULTS, DONE, ERROR
    };

    // flag for server proxy lifecycle
    private boolean _alive = true;

    private final Vector<QRequest> _requests; // Dispatcher queues requests
                                                // here.
    private final Vector<QRequest> _controlReqs; // Dispatcher queues
                                                    // interrupts here.

    private static final String PROXY_THREAD_SUFFIX = "Service_Thread";
    private static final String CONTROL_THREAD_SUFFIX = "Control_Thread";
    private static final String PULSE_THREAD_SUFFIX = "Pulse_Thread";
    private Thread _proxyThread; // Thread services for command processing.
    private Thread _controlThread; // Thread for dispatching from init Q
    // private Thread _pulseThread; //Thread for dispatching heartbeat ops

    private QRequest _currentCmd; // The command we're now executing.
    private QRequest _currentCntlCmd; // The cntl command we're now executing.

    private QueryClient _client;

    // useful information for the web service interaction
    private QueryServicePortType _service;
    private String _sessionId = QConstants.EMPTY_ID;
    private long _querySessionId = QConstants.NO_ID;
    private String _serviceUrl;
    private String _groupName;
    private String[] _filetypes;
    private int _refCount = 1; // Initialize ref count to 1 on creation.

    private final Logger _logger = Logger.getLogger(
                                    QWebServiceProxy.class.getName());

    // ------------------------------

    // delta used to determine if retrieved file has modtime < client req time
    public static final long DB_DELTA_MS = 4;

    // flag indicating that DB workaround behavior is enabled
    // private static boolean DB_ACCURRACY_FIX_ENABLED = true;

    // ------------------------------

    // interval at which client will send no-ops to server to keep connection
    // alive iff non-zero. Value must be communicated to server
    private int _heartbeatIntervalSec = 0;

    protected final int _id;
    protected static int __nextId = 0;

    // internal commands do not get unique transaction ids, instead
    // they will all be assigned this value
    protected final int INTERNAL_TRANSACTION_ID = -1;

    protected long _queryPeriod = 10000; // 30 seconds
   
   //----------------------------------------------------------------------
   
   /**
    * Constructor. 
    * @throws SessionException when connection fails
    */
   
    public QWebServiceProxy(QueryClient client, String serviceUrl,
                        QLoginInfo loginInfo) throws SessionException 
    {
        this._id = nextId();
        this._serviceUrl = serviceUrl;
        this._client = client;
        this._groupName = client.getServerGroup();

        this._requests = new Vector<QRequest>(Constants.REQUESTCAPACITY,
                Constants.REQUESTCAPINCR);
        this._controlReqs = new Vector<QRequest>(Constants.REQUESTCAPACITY,
                Constants.REQUESTCAPINCR);

        // ---------------------------

        try {

            // create the service
            this._service = new QueryService().getQueryServicePort();

            // set the service address property
            ((BindingProvider) this._service).getRequestContext()
                    .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            this._serviceUrl);
            // enable sessions
            ((BindingProvider) this._service).getRequestContext().put(
                    BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

            // login
            String sessionId = this._service.login(loginInfo.getUsername(),
                    loginInfo.getPassword(), this._groupName);

            if (sessionId.equals(""))
            {
                throw new SessionException(
                        "Could not connect login to query service",
                        Constants.INVALID_LOGIN);
            }
            else
            {
                this._sessionId = sessionId;
            }

            // -----------------------

            List<String> ftList = this._service.getAccessibleFiletypes(
                    sessionId, loginInfo.getOperation());
            if (ftList.isEmpty())
            {
                this._service.logout(this._sessionId);
                throw new SessionException("Cannot authorize user "
                        + " authorization for server group",
                        Constants.INVALID_LOGIN);
            }

            this._filetypes = ftList.toArray(new String[0]);

            // notify query client of the filetypes
            this._client.setAccessibleFiletypes(this._filetypes);

            // -----------------------

            // Create a service thread, and start it.
            String threadPrefix = "Query_Proxy_" + this._id + "_";
            this._proxyThread = new Thread(this);
            this._proxyThread.setName(threadPrefix + PROXY_THREAD_SUFFIX);
            this._controlThread = new Thread(this);
            this._controlThread.setName(threadPrefix + CONTROL_THREAD_SUFFIX);
            // this._pulseThread = new Thread(this);
            // this._pulseThread.setName(threadPrefix + PULSE_THREAD_SUFFIX);

            this._proxyThread.start();
            this._controlThread.start();
            // this._pulseThread.start();

        } catch (SecurityException e) {
            this._logger.trace(null, e);
            throw new SessionException("Connection attempt failed: "
                    + e.getMessage(), Constants.CONN_FAILED);
        } catch (QueryFault_Exception qfEx) {
            QueryFault fInfo = qfEx.getFaultInfo();
            throw new SessionException(fInfo.getMessage(), fInfo.getErrno());
        }
    }
   
   
   // ----------------------------------------------------------------------
   
   /**
     * Method to increment reference count.
     * 
     * @return new reference count.
     */
   int incrementRefCount() {
      synchronized (this._requests) {
         return ++this._refCount;
      }
   }

   // ----------------------------------------------------------------------
   
   /**
    * Method to decrement reference count
    * 
    * @return new reference count.
    */
   int decrementRefCount() {
      synchronized (this._requests) {
         return --this._refCount;
      }
   }

   //----------------------------------------------------------------------
   
   /* (non-Javadoc)
 * @see jpl.mipl.mdms.FileService.komodo.services.query.api.QServiceProxy#run()
 */
   
   public void run()
   {
       String threadName = Thread.currentThread().getName();
       
       if (threadName.endsWith(CONTROL_THREAD_SUFFIX))
           runControl();
       else if (threadName.endsWith(PROXY_THREAD_SUFFIX))
           runProxy();
       else if (threadName.endsWith(PULSE_THREAD_SUFFIX))
           runPulse();
       else
           return;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Run implementation for service proxy.  Performs while loop
    * waiting for next request command.  If null, this indicates that
    * server proxy is finished, so <code>terminate</code> will
    * be invoked to alert all threads of this, and then method will 
    * return.  Otherwise, the request is decoded and executed.
    */
   
   protected void runProxy() 
   {
       this._logger.trace("Query server service thread started");
       try {
          // The service thread waits for a queued request. Server proxy
          // goes away when there are no file type references.
          while (true) 
          {
             this._waitForCommand();
             if (this._currentCmd == null) 
             {
                 //stop query if set
                 logout();
                 
                 // Garbage collect this QServiceProxy.               
                 // Proxy is responsible for letting others know
                 terminateThreads(); //alert all threads that they are done                
                 this._logger.trace(this + " Exiting query proxy sevice thread.");
                 return; // No more commands, ever. Terminate server proxy.
             }
             /*
              * Now, decode and execute the command. Results are placed on
              * the parent komodo results queue.
              */
             this._decodeAndExecuteRequest();
          }
       } catch (InterruptedException e) {
           terminateThreads(); //alert all threads that they're done
           this._logger.trace(this + " Close immediate has been called, exiting " +
                              "qserviceproxy sevice thread.", e);
           return;
       } catch (SessionException sesEx) {
           
           
           this._logger.trace(this + " QServiceProxy.run (): "+sesEx.getMessage());
       }
    }
   
   //----------------------------------------------------------------------
   
   /**
    * Run implementation for control service.  Performs while loop
    * waiting for next control command.  If null, this indicates that
    * server proxy is finished and this method should return.  Otherwise,
    * the request is decoded and executed.
    */
   
   protected void runControl()
   {
       this._logger.trace(this + " Query server control thread started");
       
       try {
           // The service thread waits for a queued request. Server proxy
           // goes away when there are no file type references.
           while (true) 
           {
               this._waitForControlCommand();
               if (this._currentCntlCmd == null)
               {
                   this._logger.trace(this + " Exiting qserviceproxy control thread.");
                   return; // No more commands, ever. Terminate server proxy.
               }
               /*
                * Now, decode and execute the command. 
                */
               this._decodeAndExecuteControl();
           } 
       } catch (InterruptedException e) {
           this._logger.trace(this + " Close immediate has been called, exiting " +
                              "qserviceproxy control thread.", e);
           return;
       } 
   }
   //----------------------------------------------------------------------
   
   /**
    * Run implementation for pulse service.  First, exchanges pulse
    * period value with server, then enters thread registering
    * no-ops to be exchanged with server to keep connection alive.
    */
   
   protected void runPulse()
   {
       this._logger.trace(this + " Query pulse thread started");
       QRequest cmd;
       
       try {
           
           //look up the heartbeat value
           String heartbeatValue = System.getProperty(
                                   Constants.PROPERTY_CLIENT_PULSE, "0");
           try {
               this._heartbeatIntervalSec = Integer.parseInt(heartbeatValue);
           } catch (NumberFormatException nfEx) {
               this._logger.warn("Invalid value of property '"+
                                 Constants.PROPERTY_CLIENT_PULSE+"': "+
                                 heartbeatValue+".  Using default value.");
               this._heartbeatIntervalSec = 0;
           }   
           
           if (this._heartbeatIntervalSec < 0)
               this._heartbeatIntervalSec = 0;
           
           //----------------------

           if (this._heartbeatIntervalSec > 0)
           {
               //set pulse property on server side (quietly)
               int serverInterval = (int) (this._heartbeatIntervalSec);
               this._logger.trace("Setting server side heartbeat value: " +
                                  serverInterval);
               String[] pulsePropPair = {Constants.PROPERTY_CLIENT_PULSE, 
                                         String.valueOf(serverInterval)}; 
               cmd = new QRequest(Constants.EXCHANGEPROPERTY, pulsePropPair);
               cmd.setModifier(Constants.SETPROPERTY);
               
               this.putInternal(cmd);           
           
               //----------------------
               
               // Wake up every time period and add no op to queue
               // TODO - can make this smarter to only add no-ops if not active
               while (this._alive) 
               {
                   //create no-op command
                   cmd = new QRequest(Constants.NOOPERATION, new String[0]);
                   cmd.setModifier(Constants.QUIET);
                   
                   //put command 
                   this.putInternal(cmd);
                   
                   //sleep   
                   if (this._alive)
                       Thread.sleep(_heartbeatIntervalSec * 1000);               
               }
           }
       } catch (InterruptedException e) {
           this._logger.trace(this + " Close immediate has been called, exiting " +
                              "queryproxy pulse thread.", e);
           return;
       }
   }
   
   //----------------------------------------------------------------------
   
   /**
    * If this method thows an InterruptedException, then throw it on up. This
    * means that we have been told to close immediately.
    * 
    * @throws InterruptedException when signed to close immediately
    */
    private void _waitForCommand() throws InterruptedException
    {
        synchronized (this._requests)
        {
        	//reset current command while waiting
        	this._currentCmd = null;
        	
            if (this._refCount < 1)
            {
                this._currentCmd = null;
                return;
            }

            while (this._requests.isEmpty() && this._alive)
            {
                this._logger.trace(this + " Query server thread waiting..");
                // This call can throw InterruptedException, the key to
                // closing. Also, jtest complains that a non-synchronized
                // method is calling wait, but we are synchronized on
                // the requests queue.
                this._requests.wait();
                this._logger.trace(this + " Query server thread wait over.");
            }

            if (this._alive)
            {
                this._currentCmd = (QRequest) this._requests.get(0);
                this._requests.remove(0);
            }
        }
    }

   //----------------------------------------------------------------------
   
   /**
    * If this method thows an InterruptedException, then throw it on up. This
    * means that we have been told to close immediately.
    * 
    * @throws InterruptedException when signed to close immediately
    */
   private void _waitForControlCommand() throws InterruptedException 
   {
      this._currentCntlCmd = null;
      
      //check for exit case
      synchronized (this._controlReqs) {
         if (this._refCount < 1) {      
            return;
         }
         
         while (this._controlReqs.isEmpty() && this._alive) {
            this._logger.trace(this + " Query server thread waiting for "
                               + "control command...");
            // This call can throw InterruptedException, the key to
            // closing. Also, jtest complains that a non-synchronized
            // method is calling wait, but we are synchronized on
            // the requests queue.
            this._controlReqs.wait();
            this._logger.trace(this + " Query server control thread wait over.");
         }
         
         //if alive, then we are here because there's a control request
         if (this._alive)
         {
             this._currentCntlCmd = (QRequest) this._controlReqs.get(0);
             this._controlReqs.remove(0);
         }
      }
   }
   
   //----------------------------------------------------------------------
   
   /* (non-Javadoc)
 * @see jpl.mipl.mdms.FileService.komodo.services.query.api.QServiceProxy#put(jpl.mipl.mdms.FileService.komodo.services.query.api.QRequest)
 */
   public int put(QRequest profile) {
      return this._dispatchRequest(profile, false, false);
   }

   //----------------------------------------------------------------------
   
   /**
    * Method to put a request to the request queue.  No transaction id
    * will be created for the request.  It is up to the caller to ensure
    * that command will not produce any results.  As such, this method
    * should only be called for requests whose transactionId has 
    * been set.
    * 
    * @param profile the profile record specifies a request command.
    * @return the transaction id
    */
   protected int putInternal(QRequest profile) {
      return this._dispatchRequest(profile, false, true);
   }
   
   //----------------------------------------------------------------------
   
   /* (non-Javadoc)
 * @see jpl.mipl.mdms.FileService.komodo.services.query.api.QServiceProxy#putExpedited(jpl.mipl.mdms.FileService.komodo.services.query.api.QRequest)
 */
   public int putExpedited(QRequest profile) {
      return this._dispatchRequest(profile, true, false);
   }
   
   //----------------------------------------------------------------------
   
   private int _dispatchRequest(QRequest profile, boolean expedited, boolean internal)
   {
       final int transactionId = (internal) ? INTERNAL_TRANSACTION_ID :
                                              this._client.getTransactionId();
       profile.setTransactionId(transactionId);
       
       //if internal flag is set, then do not create unique xact id                
       String mesg = "Adding request '"+profile.getCommandString()+"' to ";
       mesg += (expedited) ? "front" : "back";
       
       //assume request, check if control request
       List list = this._requests;
       if (isControlRequest(profile))       
       {   
           list = this._controlReqs;
           mesg += " of control queue.";
       }
       else
       {
           mesg += " of service queue.";
       }

       this._logger.trace(this + " " + mesg);
       
       //queue command
       synchronized(list) {
           
           // Queue this transaction-branded command.           
           if (expedited)
               list.add(0, profile);
           else
               list.add(profile);
           
           // Notify the associated service thread that there is something 
           // on the queue. This will "wake up" the service thread if it is
           // blocked waiting on the queue empty condition to pass. If
           // the service thread is not waiting, then the notify will have
           // no effect. There is no race condition, since the wait/notify
           // is synchronized on the requests object. Note: jtest seems
           // to want this method synched, but we really need to synch
           // on the requests object. Also, jtest recommends the use of
           // notifyAll(), but only one thread should be waiting
           // this request queue. And even if we had a work gang on these
           // requests, we only want one waiting thread to wake up to
           // to service request.
           list.notify();            
       }
       
       return transactionId;
   }
   


   //----------------------------------------------------------------------
   
   /**
    * Method to decode user command and dispatch to handle the command
    */
   private void _decodeAndExecuteRequest() {
      this._logger.trace(this + " decodeAndExecute command "
            + this._currentCmd.getCommand());
      this._logger.trace(this + " decodeAndExecute transaction "
            + this._currentCmd.getTransactionId());
      
      boolean subscribe = this._currentCmd.isSubscribe();
      char    modifier = this._currentCmd.getModifier();
      
      try {
          if (this._currentCmd.getCommand().equals(Constants.QUIT))
          {
              this.logout();           
          }
          else if (subscribe && modifier == Constants.KILLSUBSCRIPTION)
          {
              this._stopSubscription();
          }
          else
          {
              this._runQuery();
          }          
      } catch (SessionException sesEx) {
    	  this._logger.debug(null, sesEx);
          this._endTransaction(sesEx.getErrno(), sesEx.getMessage());
      }      
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to decode user command and dispatch to handle the command
    */
   private void _decodeAndExecuteControl() 
   {
      this._logger.trace(this + " decodeAndExecuteControl command "
            + this._currentCntlCmd.getCommand());
      this._logger.trace(this + " decodeAndExecuteControl transaction "
            + this._currentCntlCmd.getTransactionId());
      
      boolean subscribe = this._currentCmd.isSubscribe();
      char    modifier = this._currentCmd.getModifier();

      try {
          
          if (subscribe && modifier == Constants.KILLSUBSCRIPTION)
          {
              this._stopSubscription();
          }

         // If this is a Komodo exception, the command has failed. Load the
         // command profile with error information and queue the command
         // terminating profile. This is used by the Komodo class to
         // decrement
         // the transaction counter.
      } catch (SessionException e) {
         this._endTransaction(e.getErrno(), e.getMessage());
      }
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Signal command complete. This allows the Komodo to decrement the
    * transaction count when the calling program dequeues this result.
    * 
    * @param errno the input error number
    * @param message the message string
    */
   private void _endTransaction(int errno, String message) {
      // create the result object
      QResult lastResult = new QResult(null, errno, message);
      lastResult.setEoT(); // the transaction is complete
      this._client.postResult(lastResult); // queue the result
   }

   //----------------------------------------------------------------------
   
   /**
    * Signal command complete. This allows the Komodo to decrement the
    * transaction count when the calling program dequeues this result.
    * 
    * @param errno the input error number
    * @param message the message string
    */
   private void _endTransactionDoNotShowUser(int errno, String message) {
      // create the result object
      QResult lastResult = new QResult(null, errno, message);
      lastResult.setEoT(); // the transaction is complete
      lastResult.setDoNotShowUser();
      this._client.postResult(lastResult); // queue the result
   }

   //----------------------------------------------------------------------
    
   
   protected void _runQuery() throws SessionException
   {
       this._startQuery();
       this._readResults();
       this._endTransactionDoNotShowUser(Constants.OK, "");
   }
   
   //----------------------------------------------------------------------
   
   private void _startQuery() throws SessionException
   {
       QueryList userQuery = this._currentCmd.getUserQuery();
       QueryConstraints systemQuery = this._currentCmd.getSystemQuery();
       boolean subscribe = this._currentCmd.isSubscribe();
       
       String userBlob = userQuery.toString();
       String constraints = systemQuery.toString();
       
       //store query id in local var for now
       long querySesId = -1L;
       try {
           querySesId = this._service.startQuery(this._sessionId, userBlob,
                                                 constraints, subscribe);
       } catch (QueryFault_Exception qfEx) {
           throw new SessionException(qfEx.getMessage(), Constants.EXCEPTION);
       }
       
       //if 0, then request was denied
       if (querySesId == 0)
       {
           //log error
           this._logger.error("Session " + this._sessionId +
                              " received NACK for request");
           throw new SessionException("Query service denied request.", 
                                      Constants.NACKED);            
       }
       
       
       this._querySessionId = querySesId;
   }

   
   //----------------------------------------------------------------------
   
   
   protected void _readResults() throws SessionException
   {
       boolean stayAlive = this._currentCmd.isSubscribe();
       boolean cont = false;
       
       //enter read loop
       do   //stayAlive used for subscriptions 
       {
           cont = false;
           do {
               
               List<String> results = null;
               try {
                   results = this._service.getResults(this._sessionId, 
                                                this._querySessionId);
               } catch (QueryFault_Exception qfEx) {
            	   qfEx.printStackTrace();
                   throw new SessionException(qfEx.getMessage(), 
                                              Constants.EXCEPTION);
               }
               
               if (results == null || results.isEmpty())
                   ;//
               
               //process the results
               //int returnCode = processResults(results);
               ProcessResultsReturnCode returnCode = processResults(results);   
               switch (returnCode)
               {
                   case GET_MORE_RESULTS:
                       cont = true;
                       break;
                   case NO_MORE_RESULTS:
                       cont = false;
                       stayAlive = true;
                       break;
                   case DONE:
                   case ERROR:
                       cont = false;
                       stayAlive = false;
                       break;
                   default:
                       cont = false;
                       break;                       
               }                   
           } while (cont);
           
           if (stayAlive)
           {
               try {
                   Thread.sleep(_queryPeriod);
               } catch (InterruptedException ioEx) {
                   ioEx.printStackTrace();                 
               }
           }           
       } while (stayAlive);
   }
   
   //----------------------------------------------------------------------
   
   protected ProcessResultsReturnCode processResults(List<String> results)
   {
       ProcessResultsReturnCode returnCode = 
                               ProcessResultsReturnCode.NO_MORE_RESULTS;
       QResult fileInfo;
       if (results == null || results.isEmpty())
           return returnCode;
       
       for (String resultLine : results)
       {
           if (resultLine.startsWith("i"))
           {
               //use utility which knows how to parse string to 
               //create a QResult.  If it can't, it returns null
               fileInfo = QResultUtil.stringEntryToResult(resultLine);
               
               //post the result to the client
               if (fileInfo != null)
               {
                   //if current cmd had a restart cache, then pass it on
                   //to the result               
                   ClientRestartCache crc = this._currentCmd.getClientRestartCache(
                		   									   fileInfo.getType());
                   
                   if (crc != null)
                	   fileInfo.setClientRestartCache(crc);
                   
                   //alert client of result
                   this._client.postResult(fileInfo);
               }
           }
           else if (resultLine.startsWith("done"))
           {
               returnCode = ProcessResultsReturnCode.DONE;
           }
           else if (resultLine.startsWith("more"))
           {
               returnCode = ProcessResultsReturnCode.GET_MORE_RESULTS;
           }          
       }
       return returnCode;
   }
   
   
   //----------------------------------------------------------------------
   
   /**
    * Create subscription session with server that receives new file metadata
    * information as it becomes available.  This method returns then session
    * ends, either normally or in error.
    * @throws SessionException when session failure.  If a session cannot
    * be initialized, then errno will be Constants.NACKED.  Errors occuring
    * after initialization will have the errno associated with original 
    * error.
    */
   
  
   //----------------------------------------------------------------------
   
   /**
    * Sends a request to server to kill current subscription session.
    * Waits for notification from the subscription thread that it has 
    * completed.
    * @throws SessionException when session failure. 
    */
   
   private void _stopSubscription() throws SessionException 
   {      
       stopQuery();
   }
   
   //----------------------------------------------------------------------
   //----------------------------------------------------------------------
   

   
   //----------------------------------------------------------------------
   
   /**
    * Exchanges property with server, either setting a property (on server)
    * or getting a property (from server).  This is reflected in the required
    * modifier field of the request.  'Set's are required to have two command
    * arguments, the name and value of the property, respectively; 
    * while 'get's are required to have a single command argument, the 
    * property name.  
    * 
    * Note: Internal usage of this mechanism for setting values on the server
    * should use the SETPROPERTYQUIET modifier to prevent Results from reaching
    * user.
    */
   
   private void _exchangeProperty() throws SessionException
   {                           
       String cmd = Constants.PROTOCOLVERSION + " " +
                    _currentCmd.getCommand() +
                    _currentCmd.getModifier();
        
       //--------------------------
       
       // check modifiers
       boolean quiet = (this._currentCmd.getTransactionId() == -1);
       boolean set = (this._currentCmd.getModifier() == Constants.SETPROPERTY);       
       
       //--------------------------
       
       String[] args = this._currentCmd.getCommandArgs();
       
       if (set)
       {
           if (args.length != 2)
           {
               throw new SessionException("Set property mode requires " +
                                          "2 parameters.  Received: " +
                                          args.length, Constants.MISSINGARG); 
           }
           cmd += " " + args[0] + " " + args[1];
       }
       else
       {
           if (args.length != 1)
           {
               throw new SessionException("Get property mode requires " +
                                          "1 parameter.  Received: " + 
                                          args.length, Constants.MISSINGARG); 
           }
           cmd += " " + args[0];
       }
              
       this._logger.trace(this + " Sending command '" + cmd 
                          + "' to server");
       
       
       //... transmit message to service ...
       
             
       if (!quiet)
       {
           this._endTransaction(0, "");        
       }       
   }
   
   
   //----------------------------------------------------------------------
   
   /**
    * Filters requests that are considered control requests rather than
    * normal requests.  An example of such a request is one to stop a 
    * running subscription.  Since proxy requests are executed serially,
    * the proxy thread will never be able to get to a kill.  Thus, the 
    * control thread is required to intercept such requests and perform
    * requested operation.
    * @param req Possible control request
    * @return True if req is considered a control request, false otherwise.
    */
   
   private boolean isControlRequest(QRequest req)
   {
       boolean isControl = false;
       String cmd = req.getCommand();
       
       if (cmd.equals(Constants.SUBSCRIBEPUSH))
       {
           switch(req.getModifier())
           {
               case Constants.KILLSUBSCRIPTION:
                   isControl = true;
                   break;
               default:
                   break;
           }
       }
       

       return isControl;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Sets the state to terminated and notifies all waiting threads so
    * that they can exit.
    */
   
   private void terminateThreads()
   {
       this._alive = false;
       
       //proxy service thread
       synchronized(this._requests) {
           this._requests.notify();
       }
       
       //proxy control thread
       synchronized(this._controlReqs) {
           this._controlReqs.notify();
       }       
       
       //pulse thread
       //this._pulseThread.interrupt();
   }

   //----------------------------------------------------------------------
   
   /**
    * Sends request to service to stop current query.  
    * @throws SessionException if error occurs
    */
   
   protected void stopQuery() throws SessionException
   {
       if (!this._sessionId.equals(QConstants.EMPTY_ID))
       {           
           if (this._querySessionId != QConstants.NO_ID)
           {
               try {
                   this._service.stopQuery(this._sessionId, 
                                           this._querySessionId);
                   this._querySessionId = QConstants.NO_ID;
               } catch (QueryFault_Exception qfEx) {
                   throw new SessionException(qfEx.getMessage(),
                                              Constants.EXCEPTION);
               }
           }              
       }       
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Logout of current session.  If a query is running, it will be stopped
    * before the logout sequence is processed.  Once this method is called
    * the proxy will no longer process 
    */
   protected void logout() throws SessionException
   {
       if (!this._sessionId.equals(QConstants.EMPTY_ID))
       {
           stopQuery();
           
           try {
               this._service.logout(this._sessionId);
               this._sessionId = QConstants.EMPTY_ID;
           } catch (QueryFault_Exception qfEx) {
               throw new SessionException(qfEx.getMessage(),
                                          Constants.EXCEPTION);               
           }    
           
           this.decrementRefCount();
       }       
   }

   //----------------------------------------------------------------------
   
   /**
    * Returns true if current running request is a query request.
    * @return True if query is running, false otherwise
    */
   
   public boolean isQueryRunning()
   {
	   boolean running = false;
	   
	   synchronized(this._requests)
	   {
		   if (this._currentCmd != null)
		   {
			   running = this._currentCmd.getUserQuery() != null;
		   }
	   }
	   
	   return running;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Returns unique id.  Called by constructor, setting a final field.
    * @return Next available id
    */
   
   private synchronized static int nextId()
   {
       return __nextId++;
   }
   
   //----------------------------------------------------------------------
   
   /* (non-Javadoc)
 * @see jpl.mipl.mdms.FileService.komodo.services.query.api.QServiceProxy#toString()
 */
   
   public String toString()
   {
       return "[QServiceProxy_"+this._id+"]";
       
   }
   
}