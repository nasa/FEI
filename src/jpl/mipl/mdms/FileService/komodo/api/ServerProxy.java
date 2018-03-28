package jpl.mipl.mdms.FileService.komodo.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jpl.mipl.mdms.FileService.io.BufferedStreamIO;
import jpl.mipl.mdms.FileService.io.MessagePkg;
import jpl.mipl.mdms.FileService.io.VerifyException;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.DirectoryUtil;
import jpl.mipl.mdms.FileService.util.Errno;
import jpl.mipl.mdms.FileService.util.FileSystem;
import jpl.mipl.mdms.FileService.util.FileSystemFactory;
import jpl.mipl.mdms.FileService.util.FileUtil;
import jpl.mipl.mdms.FileService.util.GeneralFileFilter;
import jpl.mipl.mdms.FileService.util.PrintfFormat;
import jpl.mipl.mdms.utils.logging.Logger;


/**
 * <b>Purpose:</b>
 * The sever proxy class that implements the Komodo server proxy class. 
 * This class handles requests queued to file types muliplexed onto this 
 * proxy. Must be public to implement runnable.
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
 * 00/00/2000        Thomas           Initial release. 
 * 07/13/2005        Nick             Initial documentation.
 * 10/11/2005        Nick             Introducted new constructor that accepts
 *                                    a login flag that, if set, performs the
 *                                    original login sequence.
 *                                    Added _authenticateServerGroupUser() to
 *                                    perform user authentication without 
 *                                    connecting to specific filetype.
 *                                    Updated _closeConn to check if filetype 
 *                                    is associated with the request.
 * 06/28/2006        Nick             New pulse thread to keep connection 
 *                                    alive.
 * ============================================================================
 * </PRE>
 *
 * @author Thomas Huang     (Thomas.Huang@jpl.nasa.gov)
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ServerProxy.java,v 1.112 2018/02/15 02:25:08 ntt Exp $
 *
 */

public class ServerProxy implements Runnable {
    
   //flag for server proxy lifecycle
   private boolean _alive = true; 
    
   private final Vector _requests; // Dispatcher queues requests here.
   private final Vector _controlReqs; // Dispatcher queues interrupts here.
   
   private static final String PROXY_THREAD_SUFFIX   = "Service_Thread";
   private static final String CONTROL_THREAD_SUFFIX = "Control_Thread";
   private static final String PULSE_THREAD_SUFFIX   = "Pulse_Thread";
   private Thread _proxyThread;    // Thread services for command processing.
   private Thread _controlThread;  //Thread for dispatching from init Q
   private Thread _pulseThread;  //Thread for dispatching heartbeat ops
   
   private Request _currentCmd;     // The command we're now executing.
   private Request _currentCntlCmd; // The cntl command we're now executing.
   
   private Session _session; // Reference to the FEI. We need it to queue   
                             // results.
   
   private ServerInfo _serverInfo;
   private Connection _conn = null; // Connection to Fei server.
   private String _currentType = null; // Current type being serviced.
   private int _refCount = 1; // Initialize ref count to 1 on creation.
   private boolean _admin = false;
   //private RestartInfo _restartInfo = null;
   private MessagePkg _srvReply; // Last package message reply seen from server.
   private final Logger _logger = Logger.getLogger(ServerProxy.class.getName());  
   
   private FileSystem _fileSystem;
   
   //delta used to determine if retrieved file has modtime < client req time
   public static final long DB_DELTA_MS = 4;
   
   //flag indicating that DB workaround behavior is enabled
   private static boolean DB_ACCURRACY_FIX_ENABLED = true;
   
   //termination handler reference
   private RequestTerminationHandler _reqTermHandler;
   //TODO - perhaps another set of methods could be added to automatically
   //register and de-register these handlers as part of the proxy service
   //loop
   
   //interval at which client will send no-ops to server to keep connection
   //alive iff non-zero.  Value must be communicated to server
   private int _heartbeatIntervalSec = 0;
   
   protected final int _id;
   protected static int __nextId = 0;
   
   //internal commands do not get unique transaction ids, instead
   //they will all be assigned this value
   protected final int INTERNAL_TRANSACTION_ID = -1;
   
   //----------------------------------------------------------------------
   
   /**
    * Constructor. Estabishes a connection to the server associated with
    * serverInfo and performs a login sequence.
    * @param session the reference to the FEI session
    * @param serverInfo the server info reference to connection management
    *        information.
    * @param admin if true, add 1 to Port number to get admin port connection
    * @throws SessionException when connection fails
    */
   
   ServerProxy(Session session, ServerInfo serverInfo, boolean admin)
                                              throws SessionException 
   {
       this(session, serverInfo, admin, true);
   }   
   
   //----------------------------------------------------------------------
   
   /**
    * Constructor. Estabishes a connection to the server associated with
    * the serverInfo parameter.
    * @param session the reference to the FEI session
    * @param serverInfo the server info reference to connection management
    *           information.
    * @param admin if true, add 1 to Port number to get admin port connection
    * @param login if true, perform login using user/password info from 
    *        session object.  Otherwise, perform no login.
    * @throws SessionException when connection fails
    */
   
   ServerProxy(Session session, ServerInfo serverInfo, boolean admin, 
               boolean login) throws SessionException 
   {
      this._id = nextId();
      this._session = session;
      this._serverInfo = serverInfo;
      this._admin = admin;
      this._requests   = new Vector(Constants.REQUESTCAPACITY,
                                    Constants.REQUESTCAPINCR);
      this._controlReqs = new Vector(Constants.REQUESTCAPACITY,
                                    Constants.REQUESTCAPINCR);
      
      //---------------------------
      
      try {
         // Now, establish the connection to the server.
         int port = serverInfo.getPort();
         if (admin)
            port += 1;
         
         // TODO - What is an appropriate socket timeout value. Make it configurable
         this._conn = new Connection(serverInfo.getHostName(), port, 
                                     session.getSecurityModel(), 
                                     session.getTcpStartPort(), 
                                     session.getTcpEndPort(),
                                     session.getConnectionTimeout());
         this._logger.trace(this + " Got connection " + this._conn);
         
         //if login flag is set, perform a login using session username
         //and password settings.  Also, perform a show capabilities 
         //for that user.  Otherwise, skip this step.
         if (login)
         {
             // First, before we start the service thread, log in to the server.
             // This way, we can throw an exception back to the Session.open
             // call.
             String logincmd = Constants.PROTOCOLVERSION + " " + Constants.LOGIN +
                               " " + session.getUserName() + " " + 
                               session.getPassword();
             this._logger.trace(this + " proxy.run () logincmd: " + logincmd);
    
             BufferedStreamIO io = this._conn.getIO();
    
             io.writeLine(logincmd);
             this._srvReply = io.readMessage();
             if (this._srvReply.getErrno() != Constants.OK) 
             {
                this._logger.trace(this + " logincmd reply: " + _srvReply.getMessage());
                throw new SessionException("Invalid login", 
                                           Constants.INVALID_LOGIN);
             }
             // To greatly simplify the serialization of the Session-specific
             // user capablities, load capabilities for this session before
             // starting our service thread. Only load them once.
    
             synchronized (session) {
                 
                String groupName = this._serverInfo.getGroupName();
                 
                if (!session.isCapabilitiesLoaded(groupName)) 
                {
                   String capCmd = Constants.PROTOCOLVERSION + " "
                                   + Constants.SHOWCAPS;
                   Capability tmp;
                   String reply;
                   
                   io.writeLine(capCmd);
                   // Get user capability strings.
                   this._srvReply = io.readMessage();
                   if (this._srvReply.getErrno() != Constants.OK) 
                   {
                      throw new SessionException(this._srvReply.getMessage(),
                                                 this._srvReply.getErrno());
                   }
                   do 
                   {
                      reply = io.readLine();
                      if (reply.length() == 0)
                         throw new SessionException("Unexpected eof from server",
                                                    Constants.UNEXPECTED_EOF);
    
                      StringTokenizer st = new StringTokenizer(reply, "\t");
                      st.nextToken();
                      if (reply.charAt(0) == 'a') 
                      {
                         // Save user capabilitity.
                         String tmp1 = st.nextToken();
                         this._logger.trace(this + " komodo.userAccess string = " + tmp1);
                         session._userAccess = Integer.parseInt(tmp1.trim());
                         this._logger.trace(this + " komodo.userAccess = "
                               + session._userAccess);
                      } 
                      else if (reply.charAt(0) == 'P') 
                      {
                         session.setAddVFT(true);
                      } 
                      else if (reply.charAt(0) == 'i') 
                      {
                         // Add capability to list.
                          
                         String typeName = st.nextToken();
                         this._logger.trace(this + " Adding type = " + typeName);
                         tmp = new Capability(FileType.toFullFiletype(groupName, typeName),
                                                  Integer.parseInt(st.nextToken().trim()));
                         /*
                          * Append to list. These entries come from the server
                          * sorted by file type name.
                          */
                         if (!session._capabilities.contains(tmp))
                             session._capabilities.add(tmp);
                      } 
                      else if (reply.charAt(0) == 'p') 
                      {
                         // Add capability to list.
                         String vftName = st.nextToken();
                         this._logger.trace(this + " Adding vft = " + vftName);
                         tmp = new Capability(vftName, Integer.parseInt(
                                                 st.nextToken().trim()));
                         /*
                          * Append to list. These entries come from the server
                          * sorted by file type name.
                          */
                         session._vftCapabilities.add(tmp);
                      }
                      this._logger.trace(this + " getcaps reply: " + reply);
                   } while (!reply.startsWith("eol"));
                }
             }
         } //end_if_login
         
         // Create a service thread, and start it.
         String threadPrefix = "Proxy_"+this._id+"_";
         this._proxyThread = new Thread(this);
         this._proxyThread.setName(threadPrefix + PROXY_THREAD_SUFFIX);
         this._controlThread = new Thread(this);
         this._controlThread.setName(threadPrefix + CONTROL_THREAD_SUFFIX);
         this._pulseThread = new Thread(this);
         this._pulseThread.setName(threadPrefix + PULSE_THREAD_SUFFIX);
         
         this._proxyThread.start();
         this._controlThread.start();
         this._pulseThread.start();
         
      } catch (IOException e) {
         this._logger.trace(null, e);
         throw new SessionException("Connection attempt failed: "
               + e.getMessage(), Constants.CONN_FAILED);
      } catch (SecurityException e) {
         this._logger.trace(null, e);
         throw new SessionException("Connection attempt failed: "
               + e.getMessage(), Constants.CONN_FAILED);
      }
   }
   
   //----------------------------------------------------------------------
   
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

   //----------------------------------------------------------------------
   
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
   
   /**
    * The server proxy run method. <br>
    * Dispatches based on thread name for servicing general requests and
    * control requests, respectively.
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
   
   protected void runProxy() {
       this._logger.trace("Server service thread started");
       try {
          // The service thread waits for a queued request. Server proxy
          // goes away when there are no file type references.
          while (true) {
             this._waitForCommand();
             if (this._currentCmd == null) {
                this._conn.close();
                this._serverInfo.setProxy(null);
                
                // Garbage collect this ServerProxy.               
                // Proxy is responsible for letting others know
                terminate(); //alert all threads that they are done                
                this._logger.trace(this + " Exiting serverproxy sevice thread.");
                return; // No more commands, ever. Terminate server proxy.
             }
             /*
              * Now, decode and execute the command. Results are placed on
              * the parent komodo results queue.
              */
             this._decodeAndExecuteRequest();
          }
       } catch (IOException e) {
          this._logger.trace(this + " ServerProxy.run ()", e);
       } catch (InterruptedException e) {
           terminate(); //alert all threads that they're done
           this._logger.trace(this + " Close immediate has been called, exiting " +
                              "serverproxy sevice thread.", e);
           return;
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
       this._logger.trace(this + " Server control thread started");
       
       try {
           // The service thread waits for a queued request. Server proxy
           // goes away when there are no file type references.
           while (true) 
           {
               this._waitForControlCommand();
               if (this._currentCntlCmd == null)
               {
                   this._logger.trace(this + " Exiting serverproxy control thread.");
                   return; // No more commands, ever. Terminate server proxy.
               }
               /*
                * Now, decode and execute the command. 
                */
               this._decodeAndExecuteControl();
           } 
       } catch (InterruptedException e) {
           this._logger.trace(this + " Close immediate has been called, exiting " +
                              "serverproxy control thread.", e);
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
       this._logger.trace(this + " Server pulse thread started");
       Request cmd;
       
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
               cmd = new Request(Constants.EXCHANGEPROPERTY, pulsePropPair);
               cmd.setModifier(Constants.SETPROPERTY);
               
               this.putInternal(cmd);           
           
               //----------------------
               
               // Wake up every time period and add no op to queue
               // TODO - can make this smarter to only add no-ops if not active
               while (this._alive) 
               {
                   //create no-op command
                   cmd = new Request(Constants.NOOPERATION, new String[0]);
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
                              "serverproxy pulse thread.");
           //this._logger.trace(null, e);
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
   private void _waitForCommand() throws InterruptedException {
      synchronized (this._requests) {
         if (this._refCount < 1) {
            this._currentCmd = null;
            return;
         }

         while (this._requests.isEmpty() && this._alive) {
            this._logger.trace(this + " Server thread waiting..");
            // This call can throw InterruptedException, the key to
            // closing. Also, jtest complains that a non-synchronized
            // method is calling wait, but we are synchronized on
            // the requests queue.
            this._requests.wait();
            this._logger.trace(this + " Server thread wait over.");
         }
         
         if (this._alive)
         {
             this._currentCmd = (Request) this._requests.get(0);
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
            this._logger.trace(this + " Server thread waiting for "
                               + "control command...");
            // This call can throw InterruptedException, the key to
            // closing. Also, jtest complains that a non-synchronized
            // method is calling wait, but we are synchronized on
            // the requests queue.
            this._controlReqs.wait();
            this._logger.trace(this + " Server control thread wait over.");
         }
         
         //if alive, then we are here because there's a control request
         if (this._alive)
         {
             this._currentCntlCmd = (Request) this._controlReqs.get(0);
             this._controlReqs.remove(0);
         }
      }
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Method to put a request to the request queue
    * 
    * @param profile the profile record specifies a request command.
    * @return the transaction id
    */
   int put(Request profile) {
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
   protected int putInternal(Request profile) {
      return this._dispatchRequest(profile, false, true);
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Method to insert a request to the head of the request queue. Used for
    * commands such as close file type.
    * 
    * @param profile the profile record specifies a request command.
    * @return the transaction id
    */
   int putExpedited(Request profile) {
      return this._dispatchRequest(profile, true, false);
   }
   
   //----------------------------------------------------------------------
   
   private int _dispatchRequest(Request profile, boolean expedited, boolean internal)
   {
       //if internal flag is set, then do not create unique xact id
       final int transactionId = (internal) ? INTERNAL_TRANSACTION_ID :
                                              this._session.getTransactionId();
       profile.setTransactionId(transactionId);       
       profile.setOptions(this._session._onOffOptions);
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

       this._logger.trace(this + " "+mesg);       
	
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
      try {
         String cmd = this._currentCmd.getCommand();
         
         
         //switch (this._currentCmd.getCommand()) {
         // Add and replace use the same method.
         if (cmd.equals(Constants.ADDFILEANDREF)) // case Constants.ADDFILEANDREF:
         {
            this._addRegexp();
         }
         else if (cmd.equals(Constants.ADDFILE) || cmd.equals(Constants.REPLACEFILE))
         {
            switch (this._currentCmd.getModifier()) 
            {
                case Constants.REGEXP:
                    this._addRegexp();
                    break;
                case Constants.MEMTRANSFER:
                    this._addInMemoryFile();
                    break;
                default:
                    this._addFiles();
                    break;
            }
         }          
         else if (cmd.equals(Constants.COMMENTFILE))
         {
             this._commentFile(); 
         }         
         else if (cmd.equals(Constants.CHANGETYPE))
         {
             this.changeType();
         }                  
         else if (cmd.equals(Constants.RENAMEFILE))
         {
             this._renameFile(); 
         }
         else if (cmd.equals(Constants.ARCHIVENOTE))
         {
             this._archiveFile(); 
         }
         else if (cmd.equals(Constants.DELETEFILE))
         {
             this._deleteFileByListOrRegexp(); 
         }
         else if (cmd.equals(Constants.GETVFT))
         {
             this._getVFT(); 
         }
         else if (cmd.equals(Constants.GETREFFILE))
         {
             this._getVFTReferencedFiles(); 
         }
         else if (cmd.equals(Constants.GETFILES))
         {
             switch (this._currentCmd.getModifier()) 
             {
                 case Constants.FILESSINCE:
                 case Constants.FILESBETWEEN:
                 case Constants.LATEST:
                    this._getFilesByDate();
                    break;
                 default:
                    this._getFilesByListOrRegexp();
                    break;
             } 
         }
         else if (cmd.equals(Constants.GETREFFILE))
         {
             this._getVFTReferencedFiles(); 
         }
         else if (cmd.equals(Constants.GETFILEFROMFS))
         {
             this._getFilesFromList(this._currentCmd.getFileNames(), null);
             this._endTransactionDoNotShowUser(Constants.OK, "");
         }
         else if (cmd.equals(Constants.GETFILEOUTPUTSTREAM))
         {
             this._getFilesWithOutStream(this._currentCmd.getFileNames(),
                                         this._currentCmd.getOutputStream());
         }
         else if (cmd.equals(Constants.SHOWFILES))
         {
             this._showFiles();
         }
         else if (cmd.equals(Constants.QUIT))
         {
             this._closeConnection();
         }
         else if (cmd.equals(Constants.UNLOCKFILETYPE) ||
                  cmd.equals(Constants.LOCKFILETYPE))
         {
             this._execCmd(Constants.NEEDTYPE);
         }
         else if (cmd.equals(Constants.MAKEDOMAIN))
         {
             this._makeDomainFile();
         }
         else if (cmd.equals(Constants.SHOWSERVERS))
         {
             this._showServers();
         }
         else if (cmd.equals(Constants.SUBSCRIBEPUSH))
         {
             switch (this._currentCmd.getModifier()) {               
                 case Constants.KILLSUBSCRIPTION:
                     this._execCmd(Constants.NEEDNOTYPE);
                    break;
                 default:
                    this._startSubscription();
                    break;
                 }
         }
         else if (cmd.equals(Constants.AUTHSERVERGROUPUSER))
         {
             this._authenticateServerGroupUser();
         }
         else if (cmd.equals(Constants.NOOPERATION))
         {
             this._noOperation();
         }
         else if (cmd.equals(Constants.EXCHANGEPROPERTY))
         {
             this._exchangeProperty();
         }
         else if (cmd.equals(Constants.REGISTERFILE))
         {
             this._registerFiles();
         }
         else if (cmd.equals(Constants.UNREGISTERFILE))
         {
             this._unregisterFiles();
         }
         else if (cmd.equals(Constants.SHOWSERVERS))
         {
             this._showServers();
         }
         else if (cmd.equals(Constants.SHOWSERVERS))
         {
             this._showServers();
         }
         else if (cmd.equals(Constants.GETAUTHTOKEN))
         {
             this._getAuthenticationToken();
         }
         else if (cmd.equals(Constants.GETAUTHTYPE))
         {
             this._getAuthenticationType();
         }         
         else
         {
             this._execCmd(Constants.NEEDNOTYPE);
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

   //---------------------------------------------------------------------
   
   /**
    * Method to decode user command and dispatch to handle the command
    */
   private void _decodeAndExecuteControl() {
      this._logger.trace(this + " decodeAndExecuteControl command "
            + this._currentCntlCmd.getCommand());
      this._logger.trace(this + " decodeAndExecuteControl transaction "
            + this._currentCntlCmd.getTransactionId());
      try {
          
          String cntrlCommand = this._currentCntlCmd.getCommand();
          
         //switch (this._currentCntlCmd.getCommand()) {
         // Add and replace use the same method.
          if (cntrlCommand.equals(Constants.SUBSCRIBEPUSH))
          {
              switch (this._currentCntlCmd.getModifier()) {               
                  case Constants.KILLSUBSCRIPTION:
                     this._stopSubscription();
                     break;
                  default:
                     this._execCmd(Constants.NEEDNOTYPE);
                     break;
                  }
          }
          else 
          {
              this._execCmd(Constants.NEEDNOTYPE);
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
      Result lastResult = new Result(this._currentCmd, errno, message);
      lastResult.setEoT(); // the transaction is complete
      this._session.postResult(lastResult); // queue the result
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
      Result lastResult = new Result(this._currentCmd, errno, message);
      lastResult.setEoT(); // the transaction is complete
      lastResult.setDoNotShowUser();
      this._session.postResult(lastResult); // queue the result
   }

   //----------------------------------------------------------------------
   
   /**
    * Method to handle regular expression add operation
    * 
    * @throws SessionException when session fail
    */
   private void _addRegexp() throws SessionException {
      this._logger.trace(this + " add <regexp> " + this._currentCmd.getRegExp());
      final Matcher regExpHandler;

      GeneralFileFilter filter = new GeneralFileFilter(this._currentCmd
            .getRegExp());

      File directory = new File(_currentCmd.getDirectory());
      String[] fileList = directory.list(filter);

      if (fileList.length < 1) {
         this._endTransaction(Constants.NO_FILES_MATCH, "No files found.");
         return;
      }

      if (this._logger.isTraceEnabled()) {
         for (int i = 0; i < fileList.length; i++) {
            this._logger.trace(this + " File " + (i + 1) + ": " + fileList[i]);
         }
      }
      /*
       * * Replace the file list, for which [0] was a regular expression, * with
       * our new list, and call addFiles.
       */
      this._currentCmd.setFileNames(fileList);
      this._addFiles();
   }

   //----------------------------------------------------------------------
   
   /**
    * Translate file expression to perl5.
    * 
    * @param fileExpr the file glob expression
    * @return Matcher for the input regular expression, null if syntax error.
    */
   private Matcher _fileExprToRegExp(String fileExpr) {
      StringBuffer regExp = new StringBuffer("^");
      Matcher regExpHandler;

      final int length = fileExpr.length();

      for (int i = 0; i < length; i++) {
         if (fileExpr.charAt(i) == '*')
            regExp.append(".*");
         else
            regExp.append(fileExpr.charAt(i));
      }
      regExp.append("$");

      try {
         regExpHandler = Pattern.compile(regExp.toString()).matcher("");
         return regExpHandler;
      } catch (PatternSyntaxException re) {
         return null;
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Method to change file type connection.
    */
   void changeType() {
      try {
         this._setType(this._currentCmd.getType());
         this._endTransaction(0, "File type set.");
      } catch (SessionException se) {
         // Force change of type on server-side, next command.
         this._currentType = null;
         this._endTransaction(se.getErrno(), se.getMessage());
      } catch (IOException io) {
         // Force change of type on server-side, next command.
         this._currentType = null;
         this._endTransaction(Constants.IO_ERROR, io.getMessage());
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Negotiate file add request with server, and ship files.
    * 
    * @throws SessionException when session fail
    */
   private void _addFiles() throws SessionException {
      String cmd;
      String reply;
      long fileSize;
      Result fileInfo;
      File f;
      String[] fileNames = this._currentCmd.getFileNames();
      String currDir = this._currentCmd.getDirectory();
      String fileName;
      byte[] checksum = null;

      // First, make sure there are any files to add.
      if (fileNames == null || fileNames.length < 1) {
         this._endTransaction(Constants.NO_FILE_SPECIFIED, "No files to add.");
         return;
      }
      for (int i = 0; i < fileNames.length; i++) {
         fileName = fileNames[i];         

         if (fileName.indexOf(File.separator) > -1) {
            f = new File(fileName);
            fileName = f.getName();
            this._logger.trace(this + " Adding fileName \"" + fileName + "\"");
         } else {
            f = new File(currDir, fileName);
            this._logger.trace(this + " Adding fileName 2 \"" + fileName + "\"");
         }

         if (!f.exists()) {
            fileInfo = new Result(this._currentCmd, Constants.FILE_NOT_FOUND,
                  "File \"" + fileName + "\" does not exist");
            if (i == fileNames.length - 1)
               fileInfo.setEoT();
            this._session.postResult(fileInfo);
            continue;
         }
         if (!f.isFile()) {
            if (f.isDirectory()) {
               fileInfo = new Result(this._currentCmd,
                     Constants.DIRECTORY_IGNORED, "File \"" + fileName
                           + "\" is a directory.");
            } else {
               fileInfo = new Result(this._currentCmd,
                     Constants.FILE_NOT_NORMAL, "File \"" + fileName
                           + "\" is not a normal file.");
            }
            if (i == fileNames.length - 1)
               fileInfo.setEoT();
            this._session.postResult(fileInfo);
            continue;
         }

         fileSize = f.length();
         fileInfo = new Result(this._currentCmd, fileName, fileSize);

         BufferedStreamIO io = this._conn.getIO();

         // Send the add command request to the server.
         try {
            // Send change type command to server, if required.
            // Change session type on server on change.
            this._setType(_currentCmd.getType());
            this._logger.trace(this + " Comment: \"" + this._currentCmd.getComment()
                  + "\"");

            // Format the add command. Send client-side checksum request to
            // server.
            // Server will use this information to override checksum for the
            // file type, if the file type does not require checksums. This
            // cannot be used to turn off mandatory validation.
            String options;
            if (this._currentCmd.getOption(Constants.CHECKSUM))
               options = " checksum";
            else
               options = " noChecksum";
            if (this._currentCmd.getOption(Constants.RECEIPTONXFR))
               options += "-receipt";
            else
               options += "";
            if (this._currentCmd.getOption(Constants.DIFF))
                options += "-diff ";
            else
                options += " ";

            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + options + fileName + " "
                  + fileSize;
            // If this is an add file with set reference, then append the
            // vft and link information.
            if (this._currentCmd.getCommand().equals(Constants.ADDFILEANDREF)) 
            {
               cmd += " " + this._currentCmd.getVFT();
               if (this._currentCmd.getLinkDir() != null)
                  cmd += " " + this._currentCmd.getLinkDir();
            }
            if (this._currentCmd.getComment() != null
                  && this._currentCmd.getComment().length() > 1) {
               cmd += " \"" + this._currentCmd.getComment(); // + "\"";
            }
            this._logger.trace(this + " add command: " + cmd);

            //-----------
            
            //write request
            io.writeLine(cmd);
            
            // Read the reply.
            this._srvReply = io.readMessage();
            
            //-----------
            //check for error.  Special case: REQUESTCHECKSUM with Replace action
            if (this._srvReply.getErrno() != Constants.OK &&
                this._srvReply.getErrno() != Constants.REQUESTCHECKSUM) 
                
            {
                fileInfo.setErrno(this._srvReply.getErrno());
                fileInfo.setMessage(this._srvReply.getMessage());
            }             
            else 
            {
                
                //track if file will be uploaded to server (default: yes)
                boolean sendFileToServer = true;
                
                //only send checksum if requested by server
                //was part of a REPLACE request
                boolean sendChecksumToServer =
                    (this._srvReply.getErrno() == Constants.REQUESTCHECKSUM);
                
                if (sendChecksumToServer)
                {
                    //if diff enabled, then server is expecting the client's
                    //calculation of the checksum on the next line.  
                    //If files differ, then server will return
                    //an OK message, meaning it wants the file uploaded.
                    //If not OK, then we return with server errno 
                    //<code>Constants.FILE_EXISTS</code>, indicating file is
                    //considered the same and can be skipped.
                    //Client should check for the <code>Constants.FILE_EXISTS</code> 
                    //errno to distinguish 'same-file' case from an error
                    
                    _sendFileChecksum(io, _currentCmd, f.getAbsolutePath());
                    
                    //get new reply from server
                    this._srvReply = io.readMessage();
                    
                    //are these cases both the same? keep them separate for now
                    if (this._srvReply.getErrno() == Constants.FILE_EXISTS) 
                    {
                       fileInfo.setErrno(this._srvReply.getErrno());
                       //fileInfo.setMessage(this._srvReply.getMessage());
                       fileInfo.setMessage("Skipping identical file " +f.getName()+".");                       
                       sendFileToServer = false;
                    } 
                    else if (this._srvReply.getErrno() != Constants.OK) 
                    {
                       fileInfo.setErrno(this._srvReply.getErrno());
                       fileInfo.setMessage(this._srvReply.getMessage());
                       sendFileToServer = false;
                    } 
                }
                       
                //true if no diff-check or if diff-check says
                //local and server versions are different
                if (sendFileToServer)
                {                    
                   //get message string from server reply
                   reply = this._srvReply.getMessage();
                   boolean isSrvrChksuming = (reply.indexOf("checksum") != -1);
                    
                   //Send READY
                   io.writeLine(Constants.PROTOCOLVERSION + " " + Constants.READY);
           
                   // Server tells us its calculating a checksum.
                   if (isSrvrChksuming) 
                   {
                      this._logger.trace(this + " server computing checksum");
                      checksum = io.writeAndVerifyFileToStream(f.getAbsolutePath(),
                                                               fileSize);
                      fileInfo.setChecksum(checksum);
                   } 
                   else 
                   {
                      this._logger.trace(this + " server not computing checksum");
                      io.writeFileToStream(f.getAbsolutePath(), fileSize);
                   }
                   
                   this._srvReply = io.readMessage();
                   reply = this._srvReply.getMessage();
                   
                   this._logger.trace(this + " Addfile reply = " + reply);
                   fileInfo.setMessage(reply);
                   
                   if (this._srvReply.getErrno() != Constants.OK)
                      fileInfo.setErrno(this._srvReply.getErrno());
                   else 
                   {
                       //--------------- 
                       //try to extract receipt id from server reply (contains: Receipt Id: ##)
                       
                       String receiptTag = "Receipt Id: ";
                       int recIdx = reply.indexOf(receiptTag);
                       if (recIdx != -1)
                       {
                           String sub = reply.substring(recIdx);                       
                           String[] entries = sub.split("\\s+");
                           if (entries.length > 2)
                           {
                               sub = entries[2];
                               try {
                                   int id = Integer.parseInt(sub);
                                   fileInfo.setReceiptId(id);
                               } catch (NumberFormatException nfEx) {
                                   this._logger.error("Could not parse receipt id " +
                                        "for \""+fileInfo.getName()+"\".");
                               }
                           }                      
                       }
                       
                      //--------------- 
                       
                      // Delete the file if the delete file on add/replace is
                      // set.
                      if (this._currentCmd.getOption(Constants.AUTODEL)) 
                      {
                         if (f.delete()) 
                         {
                            fileInfo.setErrno(Constants.OK);
                            fileInfo.setMessage("File \"" + f.getAbsolutePath()
                                  + "\" transfer completed with local delete.");
                         } 
                         else 
                         {
                            fileInfo.setErrno(Constants.LOCAL_FILE_DEL_ERR);
                            fileInfo.setMessage("File \"" + f.getAbsolutePath()
                                  + "\" added, but the local delete failed.");
                         }
                      } 
                      else
                         fileInfo.setErrno(Constants.OK);
                   }
               } //_end_of_if_send_file
            } //end_of_else_errno_is_OK
         } catch (IOException e) {
            String message = "IO exception while adding file: "+e.getMessage();
            try {
               io.writeLine("File transfer failed");
            } catch (IOException ne) {
               this._logger.trace(null, ne);
            } finally {
               this._endTransaction(Constants.IO_ERROR, message);
            }
            return;
         }
         
         // If this is the last file, tag the result with end of transaction.
         if (i == fileNames.length - 1)
            fileInfo.setEoT();
         
         this._session.postResult(fileInfo);
      }
   }
   
   //----------------------------------------------------------------------

   /**
    * Negotiate file add request with server, and ship files.
    * 
    * @throws SessionException when session failure
    */
   private void _addInMemoryFile() throws SessionException {
      String cmd;
      String reply;
      Result fileInfo;
      String[] fileNames = this._currentCmd.getFileNames();
      String fileName;
      long fileSize;
      byte[] checksum = null;

      // First, make sure there are any files to add.
      if (fileNames.length < 1) {
         this._endTransaction(Constants.NO_FILE_SPECIFIED, "No files to add.");
         return;
      }
      fileName = fileNames[0];
      if (fileName.indexOf(File.separator) > -1) {
         this._endTransaction(Constants.INVALID_FILE_NAME,
               "In memory file transfer file name contains file separator.");
         return;
      }

      BufferedStreamIO io = this._conn.getIO();

      // Send the add command request to the server.
      try {
         // Send change type command to server, if required.
         // Change session type on server on change.
         this._setType(_currentCmd.getType());
         fileSize = this._currentCmd.getFileBufferLength();
         this._logger.trace(this + " Comment: \"" + this._currentCmd.getComment()
               + "\"");

         // Format the add command. Send client-side checksum request to
         // server.
         // Server will use this information to override checksum for the
         // file
         // type, if the file type does not require checksums. This cannot be
         // used to turn off mandatory validation.
         if (this._currentCmd.getOption(Constants.CHECKSUM)) {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + " checksum " + fileName
                  + " " + fileSize;
         } else {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + " nochecksum " + fileName
                  + " " + fileSize;
         }
         if (this._currentCmd.getComment() != null) {
            cmd += " \"" + this._currentCmd.getComment();
         }
         this._logger.trace(this + " mem add command: " + cmd);
         io.writeLine(cmd);
         // Read the reply.
         this._srvReply = io.readMessage();
         reply = this._srvReply.getMessage();
         fileInfo = new Result(_currentCmd, fileName, fileSize);
         if (this._srvReply.getErrno() > 0) {
            fileInfo.setErrno(this._srvReply.getErrno());
            fileInfo.setMessage(reply);
         } else {
            io.writeLine(Constants.PROTOCOLVERSION + " " + Constants.READY);
            // Server tells us its calculating a checksum.
            if (reply.indexOf("checksum") != -1) {
               this._logger.trace(this + " checksum");
               checksum = io.writeAndVerifyBufferToStream(this._currentCmd
                     .getFileBuffer(), this._currentCmd.getFileBufferLength());
               fileInfo.setChecksum(checksum);
            } else {
               this._logger.trace(this + " No checksum");
               io.writeFileToStream(this._currentCmd.getFileBuffer(),
                     this._currentCmd.getFileBufferLength());
            }
            this._srvReply = io.readMessage();
            reply = this._srvReply.getMessage();
            this._logger.trace(this + " Addfile reply = " + reply);
            fileInfo.setErrno(this._srvReply.getErrno());
            fileInfo.setMessage(reply);
         }
      } catch (IOException e) {
         String message = "IO exception while adding file";
         try {
            io.writeLine("File transfer failed");
         } catch (IOException ne) {
            this._logger.trace(null, ne);
         } finally {
            this._endTransaction(Constants.IO_ERROR, message);
         }
         return;
      }
      fileInfo.setEoT();
      this._session.postResult(fileInfo);
   }

   //----------------------------------------------------------------------
   
   /**
    * Delete file by list or regular expression.
    * 
    * @throws SessionException when connection fails
    */
   private void _deleteFileByListOrRegexp() throws SessionException 
   {
      String cmd;
      String reply = null;
      String regexp = this._currentCmd.getRegExp();
      String[] fileNames = this._currentCmd.getFileNames();
      String type = this._currentCmd.getType();
      String task = "get by list or regexp";

      BufferedStreamIO io = this._conn.getIO();

      if (regexp != null) {
         try {
            cmd = Constants.PROTOCOLVERSION + " "
                  + Constants.GETFILES
                  + Character.toString(Constants.REGEXP) + " " + regexp;
            this._logger.trace(this + " " + task + " command = \"" + cmd
                  + "\"");
            // Change session type on server on change.
            this._setType(type);
            io.writeLine(cmd);
            
            //call the common handler to process results
            fileNames = _processResultsFromGetFilenames(io, cmd, task);

            if (fileNames == null)
            {
                return;
            }
            
            // Some times, such as with getVFT, getFilesFromList must be
            // called repeatedly w/o ending the transaction. Don't clean
            // this up by putting endTran... into getFilesFromList.
            this._deleteFilesFromList(fileNames);
            this._endTransactionDoNotShowUser(Constants.OK, "");
            
         } catch (IOException e) {
            String message = "IO exception while getting file names matching "
                  + regexp;
            try {
               io.writeLine("File transfer failed");
            } catch (IOException ne) {
               this._logger.trace(null, ne);
            } finally {
               this._endTransaction(Constants.IO_ERROR, message);
            }
            return;
         }
      } else {
         this._deleteFilesFromList(fileNames);
         this._endTransactionDoNotShowUser(Constants.OK, "");
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Request a file delete. Queue the results of the delete to the Komodo
    * results.
    * 
    * @param fileNames array of filenames to delete
    * @throws SessionException when session failure
    */
   private void _deleteFilesFromList(String[] fileNames)
         throws SessionException {
      String cmd;
      Result fileInfo;

      BufferedStreamIO io = this._conn.getIO();

      for (int i = 0; i < fileNames.length; ++i) {
         try {
            // Send change type command to server, if required.
            this._setType(this._currentCmd.getType());

            // Format the delete command.
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand()
                  + Character.toString(Constants.FILENAMES) + " "
                  + fileNames[i];
            fileInfo = new Result(this._currentCmd, fileNames[i]);
            // Send the add command request to the server.
            io.writeLine(cmd);
            // Read the reply.
            this._srvReply = io.readMessage();
            fileInfo.setErrno(this._srvReply.getErrno());
            fileInfo.setMessage(this._srvReply.getMessage());
            this._session.postResult(fileInfo);
         } catch (IOException e) {
            String message = "IO exception while deleting file";
            try {
               io.writeLine("File transfer failed");
            } catch (IOException ne) {
               this._logger.trace(null, ne);
            } finally {
               this._endTransaction(Constants.IO_ERROR, message);
            }
            return;
         }
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Request command to comment a file. Queue the results of the command to the
    * Komodo results queue.
    * 
    * @throws SessionException when session failure
    */
   private void _commentFile() throws SessionException {
      String cmd;
      String[] fileNames = this._currentCmd.getFileNames();
      Result fileInfo;
      BufferedStreamIO io = this._conn.getIO();

      try {
         // Send change type command to server, if required.
         this._setType(this._currentCmd.getType());

         // Format the comment command. Use a double quote to delimit the
         // command start. If no comment, no double quote.
         if (this._currentCmd.getComment() != null) {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + " " + fileNames[0] + " \""
                  + this._currentCmd.getComment();
         } else
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + " " + fileNames[0];

         fileInfo = new Result(this._currentCmd, fileNames[0]);
         // Send the comment file command request to the server.
         io.writeLine(cmd);
         // Read the reply.
         this._srvReply = io.readMessage();
         fileInfo.setErrno(this._srvReply.getErrno());
         fileInfo.setMessage(this._srvReply.getMessage());
      } catch (IOException e) {
         String message = "IO exception while commenting file";
         try {
            io.writeLine("File comment operation failed");
         } catch (IOException ne) {
            this._logger.trace(null, ne);
         } finally {
            this._endTransaction(Constants.IO_ERROR, message);
         }
         return;
      }
      fileInfo.setEoT();
      this._session.postResult(fileInfo);
   }

   //----------------------------------------------------------------------
   
   /**
    * Method to request file renaming
    * 
    * @throws SessionException when session failure
    */
   private void _renameFile() throws SessionException {
      String cmd;
      String[] fileNames = this._currentCmd.getFileNames();
      Result fileInfo;
      BufferedStreamIO io = this._conn.getIO();

      try {
         // Send change type command to server, if required.
         this._setType(this._currentCmd.getType());

         cmd = Constants.PROTOCOLVERSION + " " + this._currentCmd.getCommand()
               + " " + fileNames[0] + " " + fileNames[1];

         fileInfo = new Result(this._currentCmd, fileNames[0]);
         // Send the rename command request to the server.
         io.writeLine(cmd);
         // Read the reply.
         this._srvReply = io.readMessage();
         fileInfo.setErrno(this._srvReply.getErrno());
         fileInfo.setMessage(this._srvReply.getMessage());
      } catch (IOException e) {
         String message = "IO exception while renaming file";
         this._endTransaction(Constants.IO_ERROR, message);
         return;
      }
      fileInfo.setEoT();
      this._session.postResult(fileInfo);
   }

   //----------------------------------------------------------------------
   
   /**
    * Send a request to server, and bottle the reply in a result. Assumes type
    * has already been set if required. Used for generic commands and responses.
    * 
    * @param needType does the command require a file type context?
    * @throws SessionException when session failure
    */
   private void _execCmd(boolean needType) throws SessionException {
      String cmd;
      String reply;
      Result result;
      int errno;

      try {
         if (needType && this._currentCmd.getType() != null)
            this._setType(this._currentCmd.getType());
         // Format the comment command.
         cmd = Constants.PROTOCOLVERSION + " "
               + this._currentCmd.getCommandString();
         this._logger.trace(this + " Sending command " + cmd + " to server");

         BufferedStreamIO io = this._conn.getIO();

         io.writeLine(cmd);

         while (true) {
            // Read the reply packet.
            this._srvReply = io.readMessage();
            reply = this._srvReply.getMessage();
            errno = this._srvReply.getErrno();
            this._logger.trace(this + " execCmd reply = " + reply);
            if (reply.startsWith("done")) {
               this._endTransactionDoNotShowUser(Constants.OK, "");
               return;
            } else if (reply.length() == 0) {
               this._endTransactionDoNotShowUser(Constants.UNEXPECTED_EOF,
                     "Unexpected EOF from server.");
               return;
            } else {
               if (errno == Constants.OK) 
               {
                   String command = this._currentCmd.getCommand();
                   if (command.equals(Constants.SHOWVFT) ||
                       command.equals(Constants.SHOWVFTAT))
                   {
                     result = this._parseVFT(reply);
                   }
                   else if (command.equals(Constants.SHOWREF) ||
                            command.equals(Constants.SHOWREFAT))
                   {
                       result = this._parseRef(reply);
                   }
                   else
                   {
                       result = new Result(this._currentCmd, "");
                       result.setErrno(errno);
                       result.setMessage(reply);
                   }
               } 
               else 
               {
                  result = new Result(this._currentCmd, errno, reply);
               }
            }
            if (result != null)
               this._session.postResult(result);
         }
      } catch (IOException e) {
         this._logger.trace(this + " execCmd -----");
         this._endTransaction(Constants.IO_ERROR, e.getMessage());
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Create a vft result by parsing the reply by a server.
    * 
    * @param reply the reply string to be parsed
    * @return a result object. null if this is not a vft item.
    */
   private Result _parseVFT(String reply) {
      //System.err.println ("parseVFT reply = " + reply);
      if (reply.charAt(0) == 'i') {
         StringTokenizer st = new StringTokenizer(reply.substring(2), "\t");
         String vftName = st.nextToken();
         String createdBy = st.nextToken();
         String created = st.nextToken();
         if (created != null && created.length() > 1) {
            created = DateTimeUtil.getDateCCSDSAString(new Date(Long
                  .parseLong(created)));
            /**
             * try { created = DateTimeUtil.convertFromDBFormat(created); }
             * catch (ParseException e) {
             * this._endTransaction(Constants.PROTOCOL_ERROR, "Error parsing
             * created date : \"" + created + "\"."); return null; }
             */
         }
         String updatedBy = st.nextToken();
         if (updatedBy.equals("null"))
            updatedBy = null;
         String updated = st.nextToken();
         if (updated.equals("null"))
            updated = null;
         else
            updated = DateTimeUtil.getDateCCSDSAString(new Date(Long
                  .parseLong(updated)));

         String title = null;
         String comment = null;
         title = st.nextToken();
         if (title.equals("null"))
            title = null;
         comment = st.nextToken().trim();
         if (comment.equals("null"))
            comment = null;
         return (new Result(vftName, title, comment, createdBy, created,
               updatedBy, updated));
      }
      this._endTransaction(Constants.PROTOCOL_ERROR,
            "Error parsing server reply: \"" + reply + "\".");
      return null;
   }

   //----------------------------------------------------------------------
   
   /**
    * Create a reference result by parsing the reply by a server.
    * 
    * @param reply the reply string to be parsed
    * @return the result object
    */
   private Result _parseRef(String reply) {
      if (reply.charAt(0) == 'i') {
         String vftName = null;
         String refName = null;
         String refLink = null;
         String refFileType = null;
         String refFileName = null;
         String updateFileType = null;
         String updateFileName = null;
         String comment = null;
         boolean updateRef = false;

         StringTokenizer st = new StringTokenizer(reply.substring(2), "\t");
         int tokenCount = st.countTokens();
         this._logger.trace(this + " parseRef reply = " + reply);
         if (tokenCount > 3) {
            refName = st.nextToken();
            refFileType = st.nextToken();
            if (refFileType.equals("null"))
               refFileType = null;
            refFileName = st.nextToken();
            if (refFileName.equals("null"))
               refFileName = null;
            updateFileType = st.nextToken();
            if (updateFileType.equals("null"))
               updateFileType = null;
            updateFileName = st.nextToken();
            if (updateFileName.equals("null"))
               updateFileName = null;
            if (st.nextToken().trim().equals("t")) {
               updateRef = true;
            }
            int index = reply.indexOf('"');
            if (index != -1)
               comment = reply.substring(index + 1).trim();
            else
               comment = null;
            return (new Result(vftName, refName, refLink, refFileType,
                  refFileName, comment, updateFileType, updateFileName,
                  updateRef));
         }
      }
      return null;
   }

   //----------------------------------------------------------------------
   
   /**
    * Request command to archive a file. Queue the results of the command to the
    * Komodo results queue.
    * 
    * @throws SessionException when session failure
    */
   private void _archiveFile() throws SessionException {
      String cmd;
      String[] fileNames = this._currentCmd.getFileNames();
      Result fileInfo;
      BufferedStreamIO io = this._conn.getIO();

      try {
         // Send change type command to server, if required.
         this._setType(this._currentCmd.getType());

         // Format the archive command. Use a double quote to delimit the
         // command start. If no archive, no double quote.
         if (this._currentCmd.getComment() != null) {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + " "
                  + fileNames[0] + " \""
                  + this._currentCmd.getComment();
         } else
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + " " + fileNames[0];

         fileInfo = new Result(this._currentCmd, fileNames[0]);
         // Send the add command request to the server.
         io.writeLine(cmd);
         // Read the reply packet.
         this._srvReply = io.readMessage();
         fileInfo.setErrno(this._srvReply.getErrno());
         fileInfo.setMessage(this._srvReply.getMessage());
      } catch (IOException e) {
         String message = "IO exception while archiving file: "+e.getMessage();
         try {
            io.writeLine("File archive operation failed");
         } catch (IOException ne) {
            this._logger.trace(null, ne);
         } finally {
            this._endTransaction(Constants.IO_ERROR, message);
         }
         return;
      }
      fileInfo.setEoT();
      this._session.postResult(fileInfo);
   }

   //----------------------------------------------------------------------
   
   /**
    * Create a new domain file - admin function only
    */
   private void _makeDomainFile() {
      String cmd, reply;
      Result serverMsg = new Result(this._currentCmd, "");

      String[] args = this._currentCmd.getCommandArgs();
      String fileName = args[0];

      FileWriter fw = null;
      try {
         fw = new FileWriter(fileName);
         cmd = Constants.PROTOCOLVERSION + " "
               + this._currentCmd.getCommandString();
         this._logger.trace(this + " #######  "
                            + "Create domain file command: " + cmd);

         BufferedStreamIO io = this._conn.getIO();

         io.writeLine(cmd);
         this._srvReply = io.readMessage();
         if (this._srvReply.getErrno() != Constants.OK) 
         {
            this._endTransaction(this._srvReply.getErrno(), this._srvReply
                  .getMessage());
            return;
         }
         // Read the reply. As long as we get a put command, go ahead
         // and continue getting files.
         while (true) {
            reply = io.readLine();
            this._logger.trace(this + " makeDomainFile reply = " + reply);
            
            //eol, done receiving file contents
           if (reply.startsWith("eol")) {
               fw.close();
               serverMsg.setMessage("Domain file created");
               this._session.postResult(serverMsg);
               this._endTransactionDoNotShowUser(Constants.OK, "");
               break;
            }
            else  //file contents, write to file
                fw.write(reply);
         }
         
         //flush done message
         this._srvReply = io.readMessage();
         
      } catch (IOException ioe) {
         this._session.postResult(serverMsg);
         this._endTransaction(Constants.IO_ERROR, ioe.getMessage());
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Negotiate get files request with server, and receive files.
    * 
    * @throws SessionException when session failure
    */
   private void _getFilesByDate() throws SessionException {
       
      String cmd;
    
      final String type   = this._currentCmd.getType();     
      final Date[] dates  = this._currentCmd.getDates();
      final String regexp = this._currentCmd.getRegExp();
      final String task   = "get by date";
      
      BufferedStreamIO io = this._conn.getIO();

      try {
         if (dates.length == 2) {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand()
                  + Character.toString(Constants.FILESBETWEEN) + " "
                  + dates[0].getTime() + " " + dates[1].getTime() + " "
                  + regexp;
         } else if (dates.length == 0) {
            // Latest takes regular expression.
            if (regexp != null) {
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.LATEST) + " " + regexp;
            } else
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.LATEST) + " *";
         } else {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand()
                  + Character.toString(Constants.FILESSINCE) + " "
                  + dates[0].getTime() + " " + regexp;
         }

         this._logger.trace(this + " " + task + " command = \"" + cmd + "\"");
         
         // Change session type on server on change.
         this._setType(type);
         io.writeLine(cmd);
         
         //call the common handler to process results
         String[] fileNames = _processResultsFromGetFilenames(io, cmd, task);
         
         if (fileNames == null)
         {
             //presumably we have already handled the transaction 
             return;
         }
         
          
         // Some times, such as with getVFT, getFilesFromList must be
         // called repeatedly w/o ending the transaction. Don't clean 
         // this up by putting endTran... into getFilesFromList.
         this._getFilesFromList(fileNames, null);       
         
         //end the transaction 
         this._endTransactionDoNotShowUser(Constants.OK, "");
         
      } catch (IOException e) {
          String message = "IO exception while getting file names by date";
          int errorcode = Constants.IO_ERROR;
          try {
             io.writeLine("File transfer failed");
          } catch (IOException ne) {
             this._logger.trace(null, ne);
          } finally {
             this._endTransaction(errorcode, message);
          }
          return;
       }
   }

   /**
    * Common handler code for results of calling getFilenames() 
    * @param io BufferedIo instance
    * @param cmd Our command string
    * @param task Task name used for log messages
    * @return List of filenames returned from result set
    * @throws SessionException If session error occurs
    * @throws IOException If IO error occurs
    */

   protected String[] _processResultsFromGetFilenames(BufferedStreamIO io, 
                                                String cmd, String task) 
                                    throws SessionException, IOException
   {
       String reply = null;
       int curBatchIdx = 0;
       List<String> fileNameList = new ArrayList<String>();
     
       //we always fetch the first batch
       boolean fetchNextBatch = true;
  
       try {
           
           //while we are fetching the next batch...
           while (fetchNextBatch)
           {
               //assume no more fetching unless told otherwise
               fetchNextBatch = false;
               
               // Read the reply, must trim otherwise get one token too many
               // (blank).
               this._srvReply = io.readMessage();
               this._logger.trace(this + task + " reply = \"" + 
                                  this._srvReply + "\"");
               
               //check that we get an OK result initially
               final int srvReplyErrno = this._srvReply.getErrno();               
               if (srvReplyErrno != Constants.OK) 
               {                 
                  reply = io.readLine(); // Flush "done".
                  this._logger.trace(this + " " + task + " reply = \"" 
                                     + reply + "\"");
                  this._endTransaction(this._srvReply.getErrno(), 
                                       this._srvReply.getMessage());
                  return null;
               }
               
               //read the filelist line
               reply = io.readLine();               
               this._logger.trace(this + " " + task + " reply = \"" + reply +"\"");               
               reply = reply.trim();
               
               StringTokenizer st = new StringTokenizer(reply, "\t");
               if (!st.hasMoreTokens()) 
               {
                  throw new SessionException(
                        "Unexpected empty reply for get file list",
                        Constants.UNEXPECTED_EOF);            
               } 
               
               final String firstToken = st.nextToken();             
               if (!firstToken.equals("l"))
               {
                   throw new SessionException(reply, Constants.FILE_LIST_ERROR); 
               }
               
               
               while (st.hasMoreTokens()) 
               {
                   String curFilename = st.nextToken();                   
                   this._logger.trace(this + " fileNames[" + curBatchIdx
                                      +"] = " + curFilename);
                   curBatchIdx++;
                   fileNameList.add(curFilename);
               }
      
               
               //read next line to see if we are 'done' or more 
               //files to be fetched 
               final String doneOrMoreReply = io.readLine();
               this._logger.trace(this + " " + task + " reply = \"" + 
                                  doneOrMoreReply +"\""); 
               
               //now lets examine that string we retrieved before the get-call               
               if (doneOrMoreReply.startsWith("done"))
               {                 
                   //just continue and end the transaction latter based on
                   //what we have collected
               }
               else if (doneOrMoreReply.startsWith("morefilesavailable"))
               {
                   //submit the get next back subcommand and cycle back
                   fetchNextBatch = true;
                   this._logger.trace(this + " fetching next batch via "
                           + "sub-command = \"" + Constants.GET_MORE_FILES + "\"");
                   io.writeLine(Constants.GET_MORE_FILES);
               }
               else
               {
                   throw new SessionException(doneOrMoreReply, 
                             Constants.FILE_LIST_ERROR); 
               }
               
           } //end_while
               
           //At this point, we have a list of filenames, possibly empty           
           if (fileNameList.isEmpty())
           {
               this._endTransaction(Constants.NO_FILES_MATCH, "No files found.");
               return null;
           }
           else 
           {      
               String[] filenameArray = new String[0];
               filenameArray = (String[]) fileNameList.toArray(filenameArray);
               return filenameArray;              
           }
           
       } catch (SessionException sesEx) {
           throw sesEx;
       }       
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Get VFT header information and all vft references.
    * 
    * @throws SessionException when session failure
    */
   private void _getVFT() throws SessionException {
      MessagePkg srvReply; // Last package message reply seen from server.
      String reply;

      try {
         // Send the getVFT command to the server.
         String cmd = Constants.PROTOCOLVERSION + " "
               + this._currentCmd.getCommandString();
         this._logger.trace(this + " getVFT command = \"" + cmd + "\"");

         BufferedStreamIO io = this._conn.getIO();

         io.writeLine(cmd);
         // Read the reply packet, which contains all information about the
         // VFT.
         srvReply = io.readMessage();
         reply = srvReply.getMessage();
         this._logger.trace(this + " get header reply = " + reply);
         if (srvReply.getErrno() == Constants.OK) {
            Result vft = this._parseVFT(reply);
            vft.getVFTHeader();
            this._session.postResult(vft);
            // BaseClient get's message header.
         } else {
            Result messagePkt = new Result(this._currentCmd, srvReply
                  .getErrno(), reply);
            this._session.postResult(messagePkt);
         }
         // Now, call getVFTReferenceFiles to pull in the referenced files.
         this._getVFTReferencedFiles();
      } catch (IOException io) {
         String message = "IO exception while getting VFT information: "
                          + io.getMessage();
         Result messagePkt = new Result(this._currentCmd, Constants.IO_ERROR,
               message);
         this._session.postResult(messagePkt);
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Negotiate get referenced file request with server, and receive the file.
    * Note: If this function is called from getVFT, then don't send the command
    * for getting referenced files. Just process the reference files returned
    * from GETVFT.
    * 
    * @throws SessionException when session failure
    */
   private void _getVFTReferencedFiles() throws SessionException {
      String cmd;
      String reply = null;
      String fileType;
      String[] fileNameArg = new String[1];
      String[] newFileNameArg = new String[1];
      String[] fileSpec;
      LinkedList filesToGet = new LinkedList();
      LinkedList filesToDelete = new LinkedList();
      File invalidRef; // Use to maintain consistency with the VFT.

      BufferedStreamIO io = this._conn.getIO();

      try {
         // If comming from a GETVFT, then the command has already been sent.
         if (this._currentCmd.getCommand() != Constants.GETVFT) {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommandString();
            this._logger.trace(this + " getRefFile command = \"" 
                               + cmd + "\"");
            io.writeLine(cmd);
         }
         while (true) {
            // Read the reply.
            this._srvReply = io.readMessage();
            reply = this._srvReply.getMessage().trim();
            this._logger.trace(this + " getRefFile found " + reply);
            if (this._srvReply.getErrno() != Constants.OK) {
               this._endTransaction(this._srvReply.getErrno(), this._srvReply
                     .getMessage());
               // Flush "done".
               this._srvReply = io.readMessage();
               return;
            }
            StringTokenizer st = new StringTokenizer(reply, "\t");
            if (st.countTokens() < 1) {
               throw new SessionException(
                     "Unexpected incomplete reply for get referenced file",
                     Constants.UNEXPECTED_EOF);
            } else if (!st.nextToken().equals("v"))
               break; // No more files...

            // Note: Get a complete list of all the references to "get", in
            // addition to all of the references that don't apply anymore.
            // These obsoleted files will be deleted from the current
            // directory
            // if they exist.
            fileSpec = new String[3];
            // Reference name, to be the new name of the local file..
            fileSpec[0] = st.nextToken();
            if (st.hasMoreTokens()) {
               // File type.
               fileSpec[1] = st.nextToken();
               // File Name.
               fileSpec[2] = st.nextToken();
               filesToGet.addLast((Object) fileSpec);
            } else {
               filesToDelete.addLast((Object) fileSpec);
            }
         }
         // Override file replace, versioning options for this command.
         int newOptions = this._currentCmd.getOptions();
         newOptions |= Constants.FILEREPLACE;
         newOptions &= ~Constants.FILEVERSION;
         // Don't ask for a receipt on the file either. Get vft receipts are
         // on the whole VFT, and not on a file-by-file basis.
         newOptions &= ~Constants.RECEIPTONXFR;
         this._currentCmd.setOptions(newOptions);
         // Walk through our list, and get each file.
         while (filesToGet.size() > 0) {
            fileSpec = (String[]) filesToGet.removeFirst();
            // Reference name.
            newFileNameArg[0] = fileSpec[0];
            fileType = fileSpec[1];
            // Need actual file name for request.
            fileNameArg[0] = fileSpec[2];
            // Bind our referenced file's type into the current command.
            this._logger.trace(this + " Type = " + fileType 
                  + " file name = " + fileNameArg[0] + " ref name = " 
                  + newFileNameArg[0]);
            // Set new file type. Note: This only works if there is the new
            // file type is on the same server. Fix for 6.2, using new
            // Subscribe methods.
            this._currentCmd.setType(fileType);

            /*
             * Setup for getVFT resume transfer.
             */
            /**
             * String restartFileName = this._session.getRegistory() +
             * File.separator + fileType + Constants.RESTARTEXTENSION;
             * 
             * try { RestartExceptionListener listener = new
             * RestartExceptionListener(); XMLDecoder decoder = new
             * XMLDecoder(new BufferedInputStream( new
             * FileInputStream(restartFileName)), this, listener);
             * this._restartInfo = (RestartInfo) decoder.readObject();
             * decoder.close();
             * 
             * if (listener.isCaught()) { this._restartInfo = new
             * RestartInfo(fileType, restartFileName); } } catch (Exception e) {
             * this._restartInfo = new RestartInfo(fileType, restartFileName); }
             * 
             * this._currentCmd.setRestartInfo(this._restartInfo);
             */

            // Get our list of one file, renaming the file to the reference
            // name.
            this._getFilesFromList(fileNameArg, newFileNameArg);
         }
         // Now, cleanup any old referenced that are null for the VFT at the
         // specified or current point in time.
         while (filesToDelete.size() > 0) {
            fileSpec = (String[]) filesToDelete.removeFirst();
            // Get the absolute path of the file to delete.
            String filePath = this._currentCmd.getDirectory() + File.separator
                  + fileSpec[0];
            invalidRef = new File(filePath);
            if (invalidRef.exists()) {
               Result messagePkt = new Result(this._currentCmd, fileSpec[0]);
               if (invalidRef.delete()) {
                  messagePkt.setErrno(Constants.WARNING);
                  messagePkt.setMessage("VFT reference \""
                              + fileSpec[0]
                              + "\" is not valid for requested time.  Local file \""
                              + invalidRef.getAbsolutePath()
                              + "\" has been deleted.");
               } else {
                  messagePkt.setErrno(Constants.LOCAL_FILE_DEL_ERR);
                  messagePkt.setMessage("Warning: file \""
                              + invalidRef.getAbsolutePath()
                              + "\" could not be deleted.  Local copy of VFT is not consistent with RFS version.");
               }
               // Send this message on to our caller.
               this._session.postResult(messagePkt);
            }
         }

         // Some times, such as with getVFT, getFilesFromList must be called
         // repeatedly w/o ending the transaction. Don't clean this up by
         // putting endTran... into getFilesFromList. As you see, this time
         // the endTransaction is after a loop.
         this._endTransactionDoNotShowUser(Constants.OK, "");
      } catch (IOException e) {
         String message = "IO exception while getting files from a VFT.";
         int errorcode = Constants.IO_ERROR;
         try {
            io.writeLine("File transfer failed");
         } catch (IOException ne) {
            this._logger.trace(null, ne);
         } finally {
            this._endTransaction(errorcode, message);
         }
      }
   }
   
   //----------------------------------------------------------------------

   /**
    * Negotiate get files request with server, and receive files.
    * 
    * @throws SessionException when session failure
    */
   private void _getFilesByListOrRegexp() throws SessionException {
    
      String cmd;
    
      String regexp      = this._currentCmd.getRegExp();
      String[] fileNames = this._currentCmd.getFileNames();
      String type        = this._currentCmd.getType();

      String task = "get by list or regexp";
      
      BufferedStreamIO io = this._conn.getIO();

      if (regexp != null) 
      {
         try {
            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand()
                  + Character.toString(Constants.REGEXP) + " " + regexp;
            this._logger.trace(this + " "+ task + " command = \""
                               + cmd + "\"");
            // Change session type on server on change.
            this._setType(type);
            io.writeLine(cmd);

            
            //call the common handler to process results
            fileNames = _processResultsFromGetFilenames(io, cmd, task);
            
            if (fileNames == null)
            {
                //presumably we have already handled the transaction 
                return;
            }
            
             
            // Some times, such as with getVFT, getFilesFromList must be
            // called repeatedly w/o ending the transaction. Don't clean 
            // this up by putting endTran... into getFilesFromList.
            this._getFilesFromList(fileNames, null);       
            
            //end the transaction 
            this._endTransactionDoNotShowUser(Constants.OK, "");
            
         } catch (IOException e) {
            String message = "IO exception while getting file names matching "
                  + regexp;
            int errorcode = Constants.IO_ERROR;
            try {
               io.writeLine("File transfer failed");
            } catch (IOException ne) {
               this._logger.trace(null, ne);
            } finally {
               this._endTransaction(errorcode, message);
            }
            return;
         }
      } else {
         this._getFilesFromList(fileNames, null);
         this._endTransactionDoNotShowUser(Constants.OK, "");
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Constructs get command for filename parameter.  To be used by
    * get methods.
    * @param filename Filename to be retrieved from server
    * @return String of server command for get
    */
   
   private String _createGetCommand(String filename)
   {
       StringBuffer cmd = new StringBuffer(Constants.PROTOCOLVERSION);
       StringBuffer options = new StringBuffer();
       
       //checksum
       if (this._currentCmd.getOption(Constants.CHECKSUM))
           options.append("f checksum");
       else
           options.append("f noChecksum");
       
       //receipt
       if (this._currentCmd.getOption(Constants.RECEIPTONXFR))
           options.append("-receipt");
       
       //diff
       if (this._currentCmd.getOption(Constants.DIFF))
           options.append("-diff");
       
       options.append(" ");
       
       /* vft - 
        * Convert get referenced file to the GETFILEFROMVFT command, which
        * tells the server to get the file using the user capabilities
        * associated with the current vft instead of any current file type.
        */
//       char cmdTag = (this._currentCmd.getCommand() == Constants.GETREFFILE ||
//                      this._currentCmd.getCommand() == Constants.GETVFT) ?
//                      Constants.GETFILEFROMVFT : this._currentCmd.getCommand();
       String cmdStr = this._currentCmd.getCommand();
       String cmdTag = (cmdStr.equals(Constants.GETREFFILE) || 
                        cmdStr.equals(Constants.GETVFT)) ?  
                                    Constants.GETFILEFROMVFT  : 
                                    this._currentCmd.getCommand();
            
       //append command char, options, filename
       cmd.append(" ").append(cmdTag).append(options).append(filename);
       
       //--------------------------
       
       //restart
       ClientRestartCache restartCache;
       restartCache = this._currentCmd.getClientRestartCache();
       
       //reference to filename used in the cache
       String persistFilename = filename;
       if (this._currentCmd.getOption(Constants.SAFEREAD))
           persistFilename = Constants.SHADOWDIR + File.separator + filename;
           
       Long persistFilesize = new Long(0);
       Date persistModTime  = new Date();
       Long resumeoffset    = new Long(0);
       
       if (restartCache != null)
       {
           persistFilesize = (restartCache.getPersistedFileSize(persistFilename) 
                              == null) ? persistFilesize : 
                              restartCache.getPersistedFileSize(persistFilename);
           persistModTime  = restartCache.getPersistedModTime(persistFilename);
           resumeoffset    = (restartCache.getResumeOffset(persistFilename) 
                              == null) ? resumeoffset : 
                              restartCache.getResumeOffset(persistFilename);
           
           //check that current file is no larger than persisted, if
           //it is, then we cannot enable resume
           if (persistFilesize.compareTo(resumeoffset) < 0)
               resumeoffset = new Long(0);
           
           //if versioning, then cannot resume
           if (this._currentCmd.getOption(Constants.FILEVERSION))
               resumeoffset = new Long(0);
       }
       
       //append restart info
       cmd.append(" ").append(resumeoffset);
       cmd.append(" ").append(persistFilesize);
       cmd.append(" ").append(persistModTime.getTime());             
       
       //--------------------------
       
       //return command
       return cmd.toString();
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Negotiate get files request with server, and receive files though output
    * stream parameter.
    * @param fileNames the file name array.
    * @param out the output stream to receive files on
    * @throws SessionException when session failure
    */
   
   private void _getFilesWithOutStream(String[] fileNames, OutputStream out)
                                                   throws SessionException {
      String fileName = null;
      String message;
      String cmd;
      long fileSize;
      String fileTimeString;
      String fileLocation = null;
      String reply = null;
      byte[] buffer = null;
      final boolean inMem = this._currentCmd.getInMem();
      String type = this._currentCmd.getType();

      BufferedStreamIO io = this._conn.getIO();

      
      int nf = fileNames.length;
      this._logger.trace(this + " getFilesFromList tokens = " + nf);
      for (int i = 0; i < nf; i++) {
         Result fileReceived = null;
         try {
            // Change session type on server on change.
            this._setType(type);
            
            // Format the get command.
            cmd = this._createGetCommand(fileNames[i]);
            this._logger.trace(this + " #######  Get command: " + cmd);
            
            // Send the get command request to the server.
            this._logger.trace(this + " #######  Get command for file " + fileNames[i]);

            io.writeLine(cmd);
            
            // Read the reply. As long as we get a put command, go ahead
            // and continue getting files.
            while (true) 
            {
               this._logger.trace(this + "  #######  Get: "
                       + "trying to read reply from server put/done");
               this._srvReply = io.readMessage();
               reply = this._srvReply.getMessage();
               this._logger.trace(this + " #######  Got \"" + reply 
                                  + "\" from server");
               if (reply.startsWith("done"))
                  break;
               else if (!reply.startsWith("put")) 
               {
                  fileReceived = new Result(this._currentCmd, 
                                            Constants.FILE_NOT_FOUND, 
                                            reply);
                  this._session.postResult(fileReceived);
                  continue;               
               }


               String[] stArray = reply.substring(3).trim().split("\t");
               int index = 0;
               fileName = stArray[index++];
               fileSize = Long.parseLong(stArray[index++]);
               fileTimeString = stArray[index++];;
               boolean doChecksum = false;
               boolean doReceipt  = false;

               if (index < stArray.length && stArray[index++].equals("checksum"))
                   doChecksum = true;

               index+=4;  //contr,creat,comm,archiveNote               
               if (index < stArray.length) 
               {
                   String rcpt = stArray[index++];
                   if (rcpt.equalsIgnoreCase("receipt"))
                      doReceipt = true;
                   else
                      doReceipt = false;
               }
               
               //file server-side location
               if (index < stArray.length) 
               {
                   fileLocation = stArray[index++];
               }
               
               //------------------
               //TODO - write GO
               
               String getFileMesg = "Proceed transfer of '" + fileName + "'";
               _sendServerResponse(io, Constants.OK, getFileMesg);
               
               //------------------
               
               // Read file from stream.
               if (!inMem)
                  io.readFileFromStream(out, 0, fileSize, doChecksum);
               else 
               {
                  // Can't get > 2GB file in memory.
                  buffer = new byte[(int) fileSize];
                  io.readBufferFromStream(buffer, fileSize, doChecksum);
               }


               // File profile gets reference to the command. This allows
               // the for access of type, transaction id, etc.
               fileReceived = new Result(this._currentCmd, fileName, fileSize,
                                    new Date(Long.parseLong(fileTimeString)));
               
               

               message = "Got: " + fileName;

               // Acknowledge receipt of the file.
               //io.writeLine(Constants.PROTOCOLVERSION + " " + "ACK");
               //io.writeLine("ACK");
               this._sendServerResponse(io, Constants.OK, "File received.");

               //read the receipt if sent, but discard since we are not using it
               if (doReceipt)
                   io.readLine();
               
               fileReceived.setMessage(message);
               
               if (fileLocation != null)
                   fileReceived.setRemoteLocation(fileLocation);
               
               this._logger.trace(this + " ####### File " + fileName 
                                  + " received");

               /*
                * Add this received file to the results queue.
                */
               this._session.postResult(fileReceived);
               
            } //end_while_true
         } catch (IOException ie) {
            message = "Can't get file \"" + fileName + "\".  IO exception: \""
                  + ie.getMessage() + "\".";
            try {
               io.writeLine("File transfer failed");
               this._srvReply = io.readMessage();
               // Flush final done.
            } catch (IOException ne) {
               this._logger.trace(null, ne);
            } finally {
               this._endTransaction(Constants.IO_ERROR, message);
            }
            return;
         }
      }
      this._endTransaction(Constants.OK, "");
   }

   //----------------------------------------------------------------------
   
   /**
    * Negotiate get files request with server, and receive files.
    * 
    * @param fileNames the file name array.
    * @param newFileNames the new associated file names
    * @throws SessionException when session failure
    */
   private void _getFilesFromList(String[] fileNames, String[] newFileNames)
                                                   throws SessionException 
   {
      String currDir = this._currentCmd.getDirectory();
      String fileName = null;      
      String message;
      String cmd;
      long   fileSize;
      String fileTimeString;
      String fileCreationTimeString;
      String fileContributor;
      String fileComment = null;
      String fileArchiveNote = null;
      String fileLocation = null;
      String fileChecksum = null;
      String localLocation = null;
      String localLocationPrefix = null;
      String reply = null;      
      byte[] xferChecksum = null;
      byte[] buffer = null;
      boolean doXferChecksum = false;
      boolean doReceipt = false;
      final boolean inMem = this._currentCmd.getInMem();
      String type = this._currentCmd.getType();

      String destinationFilePath = null;
      String postSafeReadFilePath;
      File prevFile;
      File versionedFile = null;
      File shadowDir = null;
      String persistedFilename;
      
      boolean replicationEnabled = false;
      
      //added to handler CRC failures
      boolean retryAttempt = false; //indicates that current attempt is a retry
      int MAX_ATTEMPT_COUNT = 3;   //max number of retries before skipping
      int attemptCount = 1;        //current attempt count

      BufferedStreamIO io = this._conn.getIO();

      //---------------------------

      //If current directory was not set in command (this should not happen),
      //then default it to the current working directory.
      if (currDir == null)
          currDir = System.getProperty("user.dir");
      
      //---------------------------

      
      //check if we need shadow directory (saferead)
      if (this._currentCmd.getOption(Constants.SAFEREAD)) 
      { 

         this._logger.trace(this + " SAFEREAD = "
                  + this._currentCmd.getOption(Constants.SAFEREAD));
          
         if (currDir != null)
            shadowDir = new File(currDir + File.separator + Constants.SHADOWDIR);
         else
            shadowDir = new File(Constants.SHADOWDIR);

         if (!shadowDir.exists()) 
         {
            if (!shadowDir.mkdir()) 
            {
               this._endTransaction(Constants.MKDIRERROR,
                                    "Cannot create shadow directory.");
               return;
            }
         }
      }

      //---------------------------
      
      //check if replication is enabled
      if (this._currentCmd.getOption(Constants.REPLICATE)) 
      { 

         this._logger.trace(this + " REPLICATE = "
                  + this._currentCmd.getOption(Constants.REPLICATE));
         replicationEnabled = true;
         
         //is replication under a specified root?
         localLocationPrefix = this._currentCmd.getReplicationRoot();
         if (localLocationPrefix != null)
             this._logger.trace(this + " REPLICATE ROOT = " +
                                localLocationPrefix);
      }

      //---------------------------
      
      int nf = fileNames.length;
      this._logger.trace(this + " getFilesFromList tokens = " + nf);
      for (int i = 0; i < nf; i++) 
      {
 
         Result fileReceived = null;
         localLocation = null;
         fileLocation = null;
         
         try {
             
            // Change session type on server on change.  While this only
            // should happen once, it throws an exception that the try
             //catch-block deals with...
            this._setType(type);

            //check retry flag, if true, incr count and reset.
            //else reset count
            if (retryAttempt)
            {
                ++attemptCount;
                retryAttempt = false;
            }
            else
            {
                attemptCount = 1;
            }
            
            //---------------------------
            
             //check with client cache to see if we need
             //to resume transfer.
            
            ClientRestartCache restartCache = null;
            restartCache = this._currentCmd.getClientRestartCache();
            
            //file will be persisted in shadow dir if SAFEREAD
            persistedFilename = (this._currentCmd.getOption(Constants.SAFEREAD)) 
                                  ? Constants.SHADOWDIR + File.separator 
                                  + fileNames[i] : fileNames[i];
            
            //default values assuming no resume
            boolean hasResume = false;
            Long persistFilesize = new Long(0);
            Date persistModTime = new Date();
            Long resumeoffset = new Long(0);          
            
            if (restartCache != null) 
            {
               persistFilesize = restartCache.getPersistedFileSize(
                                                 persistedFilename);
               if (persistFilesize != null)
                  hasResume = true;
               else
                  persistFilesize = new Long(0);
               persistModTime = restartCache.getPersistedModTime(persistedFilename);
               resumeoffset = restartCache.getResumeOffset(persistedFilename);
               if (resumeoffset == null)
                  resumeoffset = new Long(0);
               
               //file been modified locally since last transfer, restart at 0
               if (persistFilesize.compareTo(resumeoffset) < 0)
                   resumeoffset = new Long(0);
               
               //if versioning, then cannot resume
               if (this._currentCmd.getOption(Constants.FILEVERSION))
                   resumeoffset = new Long(0);
            }
            
            //---------------------------
            
            // Format the get command.
            cmd = this._createGetCommand(fileNames[i]);
            this._logger.trace(this + " #######  Get command: " + cmd);
            
            // Send the get command request to the server.
            this._logger.trace(this + " #######  Get command for file " 
                               + fileNames[i]);
            io.writeLine(cmd);
            
            //---------------------------
            
            // Read the reply. As long as we get a put command, go ahead
            // and continue getting files.
            while (true) 
            {                
               this._logger.trace(this + " #######  Get: trying to "
                                  + "read reply from server put/done");
               this._srvReply = io.readMessage();
               reply = this._srvReply.getMessage();
               
               this._logger.trace(this + " #######  Got \"" + reply + 
                                  "\" from server");
               if (reply.startsWith("done"))
                  break; //exit_while_true
               else if (!reply.startsWith("put")) 
               {
                  fileReceived = new Result(this._currentCmd,
                                            Constants.FILE_NOT_FOUND, 
                                            reply);
                  
                  //try to get filename from message, set it as result.name
                  int start = reply.indexOf('"');
                  if (start != -1)
                  {
                      int end = reply.indexOf('"', start+1);
                      if (end != -1)
                      {
                          String filepath = reply.substring(start+1, end);
                          fileReceived.setRemoteLocation(filepath);
                          
                          //server is on unix, so we can assume forward slash
                          int dirIndex = filepath.lastIndexOf("/");
                          if (dirIndex != -1 && dirIndex < filepath.length() - 1)
                          {
                              String filename = filepath.substring(dirIndex+1);
                              fileReceived.setName(filename);
                          }
                      }
                  }      
                  
                  this._session.postResult(fileReceived);
                  continue;
               } //end_reply_not_put

               //---------------------------
               
               //Reading a file from server
               //Must check that we're not getting a File not found error here
               
               String[] stArray = reply.trim().split("\t");
               int index = 1;
               if (stArray.length > 1) 
               {
                  fileName = stArray[index++];
                  fileSize = Long.parseLong(stArray[index++]);
                  fileTimeString = stArray[index++];
                  String chk = stArray[index++];
                  if (chk.equalsIgnoreCase("checksum")) 
                     doXferChecksum = true;
                  else
                     doXferChecksum = false;                  
                  fileContributor = stArray[index++];
                  fileCreationTimeString = stArray[index++];
                  if (index < stArray.length) 
                  {
                     fileComment = stArray[index++];
                     fileComment = (fileComment.length() > 1) ? 
                                   fileComment.trim() : null;
                  }
                  if (index < stArray.length) 
                  {
                     fileArchiveNote = stArray[index++];
                     fileArchiveNote = (fileArchiveNote.length() > 1) ? 
                                       fileArchiveNote.trim() : null;
                  }
                  if (index < stArray.length) 
                  {
                      String rcpt = stArray[index++];
                      if (rcpt.equalsIgnoreCase("receipt"))
                         doReceipt = true;
                      else
                         doReceipt = false;
                  }
                  if (index < stArray.length) 
                  {
                      fileLocation = stArray[index++];
                      fileLocation = (fileLocation.length() > 1) ? 
                                      fileLocation.trim() : null;                      
                  }
                  if (index < stArray.length) 
                  {
                      fileChecksum = stArray[index++];
                      fileChecksum = (fileChecksum.length() > 1) ? 
                                      fileChecksum.trim() : null;                      
                  }
               } 
               else 
               {
                  // backward compatiblity mode to handle the reply from
                  // older server
                   StringTokenizer st = new StringTokenizer(reply.substring(3));
                  fileName = st.nextToken();
                  fileSize = Long.parseLong(st.nextToken());
                  fileTimeString = st.nextToken();
                  fileContributor = null;
                  fileCreationTimeString = null;
                  fileComment = null;
                  fileArchiveNote = null;
                  fileChecksum = null;
                  if (st.countTokens() > 0 && st.nextToken().equals("checksum"))
                     doXferChecksum = true;
                  else
                     doXferChecksum = false;
                  fileLocation = null;
               }          
               
               //---------------------------
               //---------------------------
               
               // if what we are getting back doesn't match the data in our
               // restart metadata, then the file on the server has been
               // modified since our last connection.
               // Server made the same comparison, so it will start from 
               // offset 0 if there is a mismatch.
              
               if (persistFilesize.longValue() != fileSize ||
                   persistModTime.getTime() != Long.parseLong(fileTimeString)) 
               {
                  this._logger.trace(this + " Persist file size/datetime "
                                     + "not equal to the server's");
                  resumeoffset = new Long(0);
               }

               // If there are renaming files in a list, then substitute the
               // new local file name. For example, used to substitute the
               // referenced files canonical name.
               String originalFilename = null;
               if (newFileNames != null && i < newFileNames.length && 
                   newFileNames[i] != null) 
               {
                  originalFilename = fileName;
                  fileName = newFileNames[i];
               }
               

               //---------------------------
               
               //If replication is enabled, then we will write the 
               //file to the same location as it was on the server 
               //file system by default.  If replication root was
               //specified, then remote location which be appended
               //to the root for final location
               if (replicationEnabled && fileLocation != null)
               {
                   //if local prefix set, then use that as the root for
                   //all files
                   if (localLocationPrefix != null)
                       localLocation = localLocationPrefix + fileLocation;
                   else
                       localLocation = fileLocation;
                   
                   destinationFilePath = localLocation + File.separator + fileName; 
                   File serverBasedLocation = null;
                   
                   try {
                       
                       serverBasedLocation = new File(localLocation);
                       
                       //if directory exists, then all should be well. 
                       //Otherwise, util will try to create it (and any)
                       //required parent directories.  If error occurs,
                       //an exception will be thrown.
                       //Question: Should utility add a hidden file to
                       //indicate it created the directory?
                       
                       DirectoryUtil.makeDirectory(serverBasedLocation);
                       
                   } catch (SessionException sesEx) {
                       this._logger.debug("Exception thrown while attempting " +
                       		              "to mkdir '"+fileLocation+"'.", sesEx);
                       
                       //we will continue on and catch error in the next step
                   }
                   
                   if (!(serverBasedLocation.isDirectory() && 
                         serverBasedLocation.canWrite()))
                   {
                       
                       String mkdirErrMsg = "File skipped.  Could not create directory.";
                       _sendServerResponse(io, Constants.FILE_SKIPPED, mkdirErrMsg);
                       
//                       // Need to tell skipFile to flush checksum if
//                       // necessary.
//                       io.skipFile(fileSize, doXferChecksum);
//                       
//                       _sendServerResponse(io, Constants.FILE_SKIPPED,
//                                           "File skipped.");                         

                       
                       Result result = new Result(this._currentCmd,
                             Constants.IO_ERROR, "Directory: \"" + 
                             serverBasedLocation.getAbsolutePath() + "\"" +
                             " does not exist or cannot be written to.");
                       
                       //currentCmd might specify multiple files, this
                       //specifically sets the name to refer to a single
                       result.setName(fileName);
                       result.setRemoteLocation(fileLocation);
                       result.setLocalLocation(localLocation);
                       result.setFileModificationTime(new Date(Long.parseLong(
                                                           fileTimeString)));
                       result.setChecksum(fileChecksum);
                       
                       if (restartCache != null) 
                       {
                          restartCache.removePersist(persistedFilename);
                          if (this._session.getOption(Constants.RESTART))
                              result.setClientRestartCache(restartCache);
                       }
                       this._session.postResult(result);
                       continue;
                   }
               }
               else //no replication, write to current dir
               {
                   localLocation = currDir;
                   destinationFilePath = localLocation + File.separator + fileName; 
               }
               
               //---------------------------
               
               //check for diff
               if (this._session.getOption(Constants.DIFF))
               {
                   this._logger.trace(this + " DIFF = "
                           + this._currentCmd.getOption(Constants.DIFF));
                   
                   //assume identical for now
                   boolean isIdentical = true;
                   
                   //don't have size or checksum from server
                   if (fileSize < 0 || fileChecksum == null)
                       isIdentical = false;
                   
                   
                   if (isIdentical)
                   {                      
                       File diffFile = new File(destinationFilePath);
                       
                       if (!diffFile.isFile() || !diffFile.canRead())
                           isIdentical = false;
                       
                       if (isIdentical)
                       {                           
                           //check filesizes
                           isIdentical = (diffFile.length() == fileSize);
                       }
                        
                       //check checksum
                       if (isIdentical)
                       {
                           String localChecksum = null;
                           try {
                               String path = destinationFilePath;
                               localChecksum = FileUtil.getStringChecksum(path);
                           } catch (IOException ioEx) {
                               localChecksum = "";
                           }
                           
                           if (localChecksum == null || localChecksum.equals("") ||
                                   !localChecksum.equalsIgnoreCase(fileChecksum))
                           {
                               isIdentical = false;                           
                           }
                       }
                   }
                   
                   //at this point, if still identical, then skip file transfer
                   if (isIdentical)
                   {
                       this._logger.trace(this + " Diffing concluded that files are identical.");
                       
                       String diffErrMsg = "File skipped. Diff enabled and files are identical.";
                       _sendServerResponse(io, Constants.FILE_SKIPPED, diffErrMsg);
                               
                       //create client result object
                       Result result = new Result(this._currentCmd,
                                           Constants.FILE_EXISTS,
                                           "File: \"" + destinationFilePath + "\"" +
                                           " already exists. Diff enabled and files " +
                                           "are identical.");
                       
                       //currentCmd might specify multiple files, this
                       //specifically sets the name to refer to a single
                       result.setName(fileName);
                       result.setRemoteLocation(fileLocation);
                       result.setLocalLocation(localLocation);
                       result.setFileModificationTime(new Date(Long.parseLong(
                                                           fileTimeString)));
                       result.setChecksum(fileChecksum);
                       
                       if (restartCache != null) 
                       {
                           restartCache.removePersist(persistedFilename);
                           if (this._session.getOption(Constants.RESTART))
                               result.setClientRestartCache(restartCache);
                       }
                       this._session.postResult(result);
                       continue;
                   }
               }
                             
               //---------------------------
               
               
               // now persist the file info for resume transfer.
               if (this._session.getOption(Constants.RESTART) &&
                   this._session.getOption(Constants.CHECKSUM)) 
               {
                   //We need to track location of file we will be writing.
                   //By default, this will be the determined localLocation.
                   //However, if SAFEREAD is enabled, then file will be written
                   //to the shadow dir, then copied, so its persisted location
                   //would then be the shadow directory, even for replication.
                   //NOTE: If SAFEREAD is enabled, then the persistFilename 
                   //already has the prepended shadow prefix, so location as
                   //current directory would be appropriate
                   String persistLocation = localLocation;
                   if (this._currentCmd.getOption(Constants.SAFEREAD))
                   {
                       persistLocation = currDir;
                   }

                   //--------------
                   
                   if (originalFilename != null)
                       restartCache.addPersist(persistedFilename, fileName, 
                                   fileSize, Long.parseLong(fileTimeString),
                                   persistLocation);
                   else
                       restartCache.addPersist(persistedFilename, fileSize, 
                                             Long.parseLong(fileTimeString),
                                             persistLocation);
                   restartCache.commit();
               }
               
               //---------------------------                                   

               // Does this file go to memory, or to disk? See if we need
               // to skip, replace or version this file.
               this._logger.trace(this + " FILEREPLACE = "
                     + this._currentCmd.getOption(Constants.FILEREPLACE));
               this._logger.trace(this + " FILEVERSION = "
                       + this._currentCmd.getOption(Constants.FILEVERSION));
               this._logger.trace(this + " inMem = " + inMem);
               
               //------------------
               
               //check if already exists as directory
               
               if (!inMem)
               {
                   File dirFile = new File(destinationFilePath);
                   
                   // If the file exists, and is not a directory, then rename
                   // the file. If the file is a directory, then report it
                   // and skip file
                   if (dirFile.isDirectory()) 
                   {
                       
                       String dirErrMsg = "File skipped. Directory with same name exists.";
                          
                       _sendServerResponse(io, Constants.FILE_SKIPPED,dirErrMsg);                         
                          
                       Result result = new Result(this._currentCmd,
                                           Constants.FILE_EXISTS, 
                                           "File: \"" + destinationFilePath + "\"" +
                                           " already exists as directory.");
                          
                       //currentCmd might specify multiple files, this
                       //specifically sets the name to refer to a single
                       result.setName(fileName);
                       result.setFileModificationTime(new Date(Long.parseLong(
                                                              fileTimeString)));
                       if (restartCache != null) 
                       {
                           restartCache.removePersist(persistedFilename);
                           if (this._session.getOption(Constants.RESTART))
                               result.setClientRestartCache(restartCache);
                       }
                       this._session.postResult(result);
                       continue;
                      
                   }
               }
               
               //------------------
               
               
               if (!inMem && !this._currentCmd.getOption(Constants.FILEREPLACE)) 
               {
                  // Check to see if the file already exists in the local
                  // directory. Never overwrite the file. If FILEVERSIONING
                  // is set, then rename the old file, otherwise, skip the
                  // file.
                   
                  prevFile = new File(destinationFilePath);
                  versionedFile = null;
                  buffer = null;
                  
                  // If the file exists, and is not a directory, then rename
                  // the file. If the file is a directory, then report it
                  // and skip file
                  if (prevFile.exists()) 
                  {                     
                      
                      //if VERSION is enabled
                     if (this._currentCmd.getOption(Constants.FILEVERSION)) 
                     {
                        int count = 0;
                        while (versionedFile == null && count++ < 10) 
                        {
                           versionedFile = new File(destinationFilePath + "."
                                 + System.currentTimeMillis());
                           if (versionedFile.exists())
                              versionedFile = null;
                        }
                        if (versionedFile != null) 
                        {
                           prevFile.renameTo(versionedFile);
                           versionedFile = null;
                        } 
                        else 
                        {
                           // Need to tell skipFile to flush checksum
                           // if necessary.
                           //io.skipFile(fileSize, doXferChecksum);
                           
                           _sendServerResponse(io, Constants.FILE_SKIPPED,
                                               "File skipped. Version file could " +
                                               "not be created.");                                                                                                        
                           //io.writeLine("File skipped.");

                           this._session.postResult(new Result(
                                 this._currentCmd, Constants.FILE_NOT_RENAMED,
                                 "File: \"" + destinationFilePath
                                       + "\" could not be renamed."));
                           continue;
                        }
                     } 
                     else if (!hasResume) 
                     {  
                        // !VERSION AND !RESUME AND FILE EXISTS
                         
                        // if the file already exists, then report the problem.
                        // if restart is enabled, then we need to register a
                        // restart object associate to the result object, so
                        // the application program can perform long transaction.
                         
                        //if autocommit - set last query time and commit
                        if (this._session.getOption(Constants.RESTART) &&
                            this._session.getOption(Constants.AUTOCOMMIT)) 
                        {
                            restartCache.setLastQueryTime(Long.parseLong(
                                                              fileTimeString));
                            restartCache.commit();    
                        }
                        
                        // Need to tell skipFile to flush checksum if
                        // necessary.
                        //io.skipFile(fileSize, doXferChecksum);
                        
                        _sendServerResponse(io, Constants.FILE_SKIPPED, 
                                            "File skipped.  Version and replace not set.");                                                                         
                        //io.writeLine("File skipped.");
                        
                        Result result = new Result(this._currentCmd,
                                                   Constants.FILE_EXISTS, 
                                                   "File: \"" + destinationFilePath
                                                   + "\" already exists.");
                        //currentCmd might specify multiple files, this
                        //specifically sets the name to refer to a single

                        result.setFileModificationTime(new Date(Long.parseLong(
                                                            fileTimeString)));
                        result.setName(fileName);
                        result.setRemoteLocation(fileLocation);
                        result.setLocalLocation(localLocation);
                        result.setChecksum(fileChecksum);
                        
                        if (restartCache != null) 
                        {
                           restartCache.removePersist(persistedFilename);
                           if (this._session.getOption(Constants.RESTART))
                               result.setClientRestartCache(restartCache);
                        }
                        this._session.postResult(result);
                        continue;
                     }
                  }
               } //end_not_mem_AND_not_replace

               

               //---------------------------
               
               // If this is an "atomic" transfer, do the actual transfer
               // in current directories .shadow.
               if (this._currentCmd.getOption(Constants.SAFEREAD)) 
               {
                  postSafeReadFilePath = destinationFilePath;
                  destinationFilePath = shadowDir + File.separator + fileName;                 
               } 
               else  //null final path means we will not rename file when done
               {
                  postSafeReadFilePath = null;
               }
               
               
               //---------------------------
               //---------------------------
               
               //go ahead and request file contents from server
               String getFileMesg = "Proceed with transfer of '" + fileName + "'";
               _sendServerResponse(io, Constants.OK, getFileMesg);
               
               //---------------------------
               //---------------------------
               
               this._logger.trace(this + " doChecksum = " + doXferChecksum);
               if (doXferChecksum) 
               {
                  try {
                     this._logger.trace(this + " Getting file "
                                 + "with checksum  "
                                 + "- filePath:"
                                 + destinationFilePath + " offset:" + resumeoffset
                                 + " size:" + fileSize);
                     if (!inMem) 
                     {
                        xferChecksum = io.readAndVerifyFileFromStream(destinationFilePath,
                                                       resumeoffset.longValue(), fileSize);
                     } 
                     else 
                     {
                        // Can't handle large files. Hence the cast.
                        buffer = new byte[(int) fileSize];
                        xferChecksum = io.readAndVerifyBufferFromStream(buffer,
                                                                 fileSize);
                     }
                  } catch (VerifyException ve) {
               
                      _sendServerResponse(io, Constants.FILE_NOT_DELIVERED,
                                          "File transfer verification failed");
                      //io.writeLine("File transfer verification failed");
                      message = "Verification error (CRC) occurred for \""
                                + fileName + "\" on ";
                      
                     //prepare to try again, decrement for-loop index
                     // and set retry flag.
                     if (attemptCount < MAX_ATTEMPT_COUNT) 
                     {
                         message += "attempt " + attemptCount 
                                    + ". Trying again...";
                         this._logger.error(message);
                         this._logger.trace(null, ve);
                         
                         //delete current attempt file
                         File badFile = new File(destinationFilePath);
                         if (badFile.canWrite() && !badFile.isDirectory())
                         {
                             if (!badFile.delete())
                             {
                                 this._endTransaction(Constants.FILE_NOT_DELETED,
                                 "Unable to delete corrupted file \"" 
                                 + destinationFilePath + "\"");
                                 return;
                             }
                         }
                         
                         //set retry flag and decr loop-index so that loop
                         //increments to the same value
                         retryAttempt = true; 
                         --i; 
                     }
                     else //reached max att. cnt., skip this file
                     {
                         message += " final attempt.  Skipping file.";
                         this._logger.error(message);
                         this._logger.trace(null, ve);
                         message = "Can't get file \"" + fileName
                            + "\".  Verify error: \"" + ve.getMessage() 
                            + "\".";
                         this._session.postResult(new Result(this._currentCmd,
                                       Constants.FILE_NOT_VERIFIED, message));
                     }
                     continue;
                  }
               } 
               else 
               {
                  // Read file from stream.
                  if (!inMem)
                     io.readFileFromStream(destinationFilePath, 0, fileSize);
                  else 
                  {
                     // Can't get > 2GB file in memory.
                     buffer = new byte[(int) fileSize];
                     io.readBufferFromStream(buffer, fileSize);
                  }
               }

               //---------------------------
               
               // Move file to final directory if we are doing an atomic
               // read.
               if (postSafeReadFilePath != null) 
               {
                  File finalFile = new File(postSafeReadFilePath);
                  File shadowFile = new File(destinationFilePath);
                  
                  FileSystem fs = getFileSystem();
                  
                  this._logger.trace("Moving file from " +
                                     shadowFile.getAbsolutePath() + " to " +
                                     finalFile.getAbsolutePath());
                  Errno moveErrno = fs.moveFile(shadowFile.getAbsolutePath(), 
                                                finalFile.getAbsolutePath());
                  
                  if (moveErrno.getId() != Constants.OK)
                  {
                      String msg = "Can't move file from shadow directory: " +
                                   moveErrno.getMessage();
                      this._logger.trace("Move file failed: "+moveErrno.getMessage());
                      this._session.postResult(new Result(this._currentCmd,
                                               Constants.FILE_NOT_MOVED, msg));                            
                      _sendServerResponse(io, Constants.FILE_NOT_DELIVERED, 
                                         "Can't move file from shadow directory.");
                      continue;                      
                  }
                  
               }

               //---------------------------
               
               // File profile gets reference to the command. This allows
               // the for access of type, transaction id, etc.
               fileReceived = new Result(this._currentCmd, fileName, fileSize,
                                    new Date(Long.parseLong(fileTimeString)));
               if (fileContributor != null)
                  fileReceived.setFileContributor(fileContributor);
               if (fileCreationTimeString != null)
                  fileReceived.setFileCreationTime(new Date(
                        Long.parseLong(fileCreationTimeString.trim())));
               if (fileComment != null)
                  fileReceived.setComment(fileComment);
               if (fileArchiveNote != null)
                  fileReceived.setArchiveNote(fileArchiveNote);
               if (fileLocation != null)
                   fileReceived.setRemoteLocation(fileLocation);
               if (localLocation != null)
                   fileReceived.setLocalLocation(localLocation);
               if (fileChecksum != null)
                   fileReceived.setChecksum(fileChecksum);
                
               // If this result is from a getVFT, patch in vft reference
               // information.
               // Note: fileName will be the name of the reference, since
               // that is what the file is known as locally.
               if (originalFilename != null) 
               {
                  fileReceived.setRefName(fileName);
                  fileReceived.setRefFileName(originalFilename);
                  fileReceived.setRefFileType(type);
               }
               if (xferChecksum != null)
                  fileReceived.setChecksum(xferChecksum);
               if (buffer != null)
                  fileReceived.setFileBuffer(buffer);

               //---------------------------
               
               // Put file version message in profile if a prev. file was
               // renamed.
               // Note. We don't use the server message here, since the
               // disposition of the file is the responsibility of the
               // client side. VFT substitution.
               String fileNameText;
               if (originalFilename != null) 
               {
                  fileNameText = fileName + " => " + type + File.separator
                        + originalFilename;
               } 
               else 
               {
                  fileNameText = fileName;
               }
               
               if (versionedFile != null) 
               {
                  message = "Got: " + fileNameText
                        + ". Saved previous file as \""
                        + versionedFile.getName();
                  versionedFile = null;
               } 
               else 
               {
                  message = "Got: " + fileNameText;
                  if (fileReceived.getChecksum() != null)
                     message += " Checksum: \"" + fileReceived.getChecksumStr()
                           + "\"";

                  // Acknowledge receipt of the file.
                  _sendServerResponse(io, Constants.OK, "File received.");
                  //io.writeLine("ACK");
               }
               
               //---------------------------
               
               // After transfer OK signal to server, get any receipt.
               if (this._currentCmd.getOption(Constants.RECEIPTONXFR) || doReceipt) 
               {
                  /* This will be the receipt */
                  this._srvReply = io.readMessage();
                  reply = this._srvReply.getMessage();
                  try {
                      fileReceived.setReceiptId(Integer.parseInt(reply.trim()));
                      message += " Receipt Id: " + fileReceived.getReceiptId();
                  } catch (NumberFormatException nfEx) {
                      this._logger.error("Could not parse receipt id: "+reply);
                  }
               }

               if (resumeoffset.longValue() > 0)
                  message += "\nTransfer was resumed for file " + fileName
                        + " at byte " + resumeoffset + ".";

               fileReceived.setMessage(message);
               this._logger.trace(this + " ####### File " 
                                  + fileName + " received");

               //---------------------------
               
               // Update restart file with file's date if commanded to do
               // so.
               if (restartCache != null) 
               {
                   
                  // if we got here, then we have received a file. Now,
                  // it is time to remove it from the resume list.
                  restartCache.removePersist(persistedFilename);
                  
                  // XXXX For long transactions, you'd check
                  // cmd.getOption(AUTOCOMMIT)
                  // here.
                  if (this._session.getOption(Constants.RESTART))
                  {
                      fileReceived.setClientRestartCache(restartCache);
                      if (this._session.getOption(Constants.AUTOCOMMIT))
                      {
                          restartCache.setLastQueryTime(fileReceived
                                       .getFileModificationTime().getTime());
                          fileReceived.commit();
                      }
                  }
               }
               
               //---------------------------
               
               // Add this received file to the results queue.
               this._session.postResult(fileReceived);
            }
         } catch (SocketTimeoutException stoEx) {
             
            message = "Can't get file \"" + fileName + "\".  IO exception: \""
                      + stoEx.getMessage() + "\".";
            this._logger.trace(message, stoEx);
            int errorcode = Constants.IO_ERROR;
            
            throw new SessionException(message, Constants.IO_ERROR);
            //this._endTransaction(errorcode, message);            
            //return;
            
         } catch (IOException ie) {
             
             message = "Can't get file \"" + fileName + "\".  IO exception: \""
                         + ie.getMessage() + "\".";
             this._logger.trace(message, ie);
             int errorcode = Constants.IO_ERROR;
             try {
                 _sendServerResponse(io, Constants.IO_ERROR, ie.getMessage());                                
               //io.writeLine("File transfer failed");
                 
               this._srvReply = io.readMessage();
               // Flush final done.
             } catch (Exception ne) {
                this._logger.trace(ne.getMessage(), ne);
             } finally {
                //throw new SessionException(message, errorcode);
                //this._endTransaction(errorcode, message);
             }
             
             throw new SessionException(message, errorcode);
             // return;
             
          }
      }
   }

   /**
    * Writes a response message to the server.  This message includes
    * an error code and a message string.
    * @param io IO Stream to which message will be written
    * @param code Error code value, Constants.OK for no error
    * @param msg Message string
    * @throws IOException If IO error occurs
    */
   
   private void _sendServerResponse(BufferedStreamIO io, int code, String msg) 
                                                            throws IOException
   {                       
       this._logger.trace(this + " #######  Sending response to server: " +
                          code + "\t"+msg);
       io.writeMessage(code, msg);          
   }
   
   
   
   
   
   //----------------------------------------------------------------------
   
   /**
    * Show all files, by date, regular expression or all. Negotiate show files
    * request with server, and ship the file.
    * 
    * @throws SessionException when session failure
    */
   private void _showFiles() throws SessionException {
      this._logger.trace(this + " show");
      String cmd;
      String reply = null;
      String comment = null;
      String archiveNote = null;
      String checksum = null;
      String fileName = null;
      String location = null;
      String date = null;
      Date[] dates;
      String regexp = this._currentCmd.getRegExp();
      String[] fileNames = this._currentCmd.getFileNames();
      long fileSize;
      Result fileInfo;
      String type = this._currentCmd.getType();
      BufferedStreamIO io = this._conn.getIO();
      

      try {
         // Format the command, for all types, date, two date, or regular
         // expression.
         switch (this._currentCmd.getModifier()) {
         //case Constants.DATETIME:
         case Constants.FILESBETWEEN:
         case Constants.FILESSINCE:
            // Note: Request constructor has already verified the
            // command semantics.
            dates = this._currentCmd.getDates();
            if (dates.length == 2) {
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.FILESBETWEEN) + " "
                     + dates[0].getTime() + " " + dates[1].getTime() + " "
                     + regexp;
            } else {
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.FILESSINCE) + " "
                     + dates[0].getTime() + " " + regexp;
            }
            break;
         case Constants.LATEST:
            if (regexp != null)
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.LATEST) + " " + regexp;
            else
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.LATEST) + " *";
            break;
         default:
            if (fileNames != null) {
               regexp = fileNames[0];
            }
            if (regexp != null)
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.REGEXP) + " " + regexp;
            else
               cmd = Constants.PROTOCOLVERSION + " "
                     + this._currentCmd.getCommand()
                     + Character.toString(Constants.FILENAMES) + " ";
            break;
         }

         // Change session type on server on change.
         this._setType(type);
         this._logger.trace(this + " show command = \"" + cmd + "\"");
         io.writeLine(cmd);
         
         //we will keep requesting batches as they are available,
         //assume true for the first time.
         boolean requestNextBatch = true;
         
         while (requestNextBatch)
         {
             //for now, assume this is the last batch
             requestNextBatch = false;
         
         
             /* If there is an error, no list will follow. */
             this._srvReply = io.readMessage();
             this._logger.trace(this + " ShowFiles reply = \"" + 
                                this._srvReply + "\"");
         
             final int srvReplyErrno = this._srvReply.getErrno();
             
             if (srvReplyErrno != Constants.OK) 
             {                 
                reply = io.readLine(); // Flush "done".
                this._logger.trace(this + " showFiles reply 2 = \"" 
                                   + reply + "\"");
                this._endTransaction(this._srvReply.getErrno(), 
                        this._srvReply.getMessage());
                return;
             }
                     
         
             // Read the reply.
             while ((reply = io.readLine()) != null) 
             {
                this._logger.trace(this + " show reply: \"" + reply + "\"");
                if (reply.length() == 0)
                   throw new SessionException("Unexpected eof from server",
                         Constants.UNEXPECTED_EOF);
                if (reply.charAt(0) != 'i')
                   break;
    
                //Reading a file from server
                //Must check that we're not getting a File not found error
                // here!
                StringTokenizer st = new StringTokenizer(reply.substring(2), "\t");
                fileName = st.nextToken();
                date = st.nextToken();
                fileSize = Long.parseLong(st.nextToken().trim());
                fileInfo = new Result(this._currentCmd, fileName, fileSize,
                      new Date(Long.parseLong(date)));
    
                reply = io.readLine();
                st = new StringTokenizer(reply, "\t");
                fileInfo.setFileContributor(st.nextToken());
                fileInfo.setFileCreationTime(new Date(Long.parseLong(
                                             st.nextToken().trim())));
    
                // See if there is a comment too.
                comment = io.readLine();
                this._logger.trace(this + " Comment = \"" + comment + "\"");
                if (comment.length() > 1)
                   fileInfo.setComment(comment.trim());
    
                // See if there is an archiveNote too.
                archiveNote = io.readLine();
                this._logger.trace(this + " ArchiveNote = \"" + 
                                                        archiveNote + "\"");
                
                if (archiveNote.length() > 1)
                   fileInfo.setArchiveNote(archiveNote.trim());
    
                // See if there is a checksum too.
                checksum = io.readLine();
                this._logger.trace(this + " Checksum = \"" + checksum + "\"");
                if (checksum.length() > 1)
                   fileInfo.setChecksum(checksum.trim());
    
                // See if there is a location too.
                location = io.readLine();
                this._logger.trace(this + " Remote Location = \"" + 
                                                            location + "\"");
                if (location.length() > 1)
                   fileInfo.setRemoteLocation(location.trim());
                
                ClientRestartCache restartCache = this._currentCmd
                                                  .getClientRestartCache();
                if (restartCache != null) 
                {
                   if (this._session.getOption(Constants.RESTART))
                   {
                       //notify file info result of cache
                       fileInfo.setClientRestartCache(restartCache);
                       
                       //check for autocommit
                       if (this._session.getOption(Constants.AUTOCOMMIT))
                       {
                           this._logger.trace(this + " Persisting lastQueryTime "
                                        + DateTimeUtil.getDateCCSDSAString(
                                          fileInfo.getFileModificationTime()));
                           restartCache.setLastQueryTime(
                                  fileInfo.getFileModificationTime().getTime());
                           restartCache.commit();
                       } //end_if_AUTOCOMMIT
                   } //end_if_RESTART
                } //end_restartCache_non_null
    
                this._session.postResult(fileInfo);
                this._logger.trace(this + " show posted result; "
                                   + "going back for more");
                
             }  //end_of_original_while_readLine
         
         
             //check if line indicates we are done or if there 
             //is another batch
             if (reply.startsWith("done")) 
             {
                 requestNextBatch = false;
                 break;                      
             }
             else if (reply.startsWith("more"))
             {
                 requestNextBatch = true;
                 this._logger.trace(this + " fetching next batch via "
                         + "sub-command = \"" + Constants.GET_MORE_FILES + "\"");
                 io.writeLine(Constants.GET_MORE_FILES); 
             }
         
         
        } //END_OF_keepReceivingRepliesFromServer_WHILE
         
         
      } catch (IOException e) {
    	 e.printStackTrace();
         String message = "IO exception while showing files: "+e.getMessage();
         try {
            io.writeLine("File transfer failed");
         } catch (IOException ne) {
            this._logger.trace(null, ne);
         } finally {
            this._endTransaction(Constants.IO_ERROR, message);
         }
         return;
      } 
      
      // Make sure we've got all of the files. If so, we should have seen
      // a "done" reply.
      this._logger.trace(this + " Exited item from server: \"" 
                         + reply + "\"");
      
      if (!reply.startsWith("done")) {
          
         this._logger.trace(this + " we are here");
         this._endTransaction(Constants.PROTOCOL_ERROR, reply);
         
      } else {
          
         this._logger.trace(this + " we are there");
         // Don't bother the caller with this profile. All results are
         // in for this call.

         this._endTransactionDoNotShowUser(Constants.OK, reply);
         
      }
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
   
   private void _startSubscription() throws SessionException {
      this._logger.trace(this + " subscribe push");
      
      String cmd;
      String reply = null;
      String comment = null;
      String archiveNote = null;
      String checksum = null;
      String fileName = null;
      String remoteLocation = null;
      String date = null;
      String regexp = this._currentCmd.getRegExp();
      String[] fileNames = this._currentCmd.getFileNames();
      long fileSize;
      Result fileInfo;
      String type = this._currentCmd.getType();
      BufferedStreamIO io = this._conn.getIO();
      
      //cmd starts with protocol version and command
      cmd = Constants.PROTOCOLVERSION + " " + this._currentCmd.getCommand();
      
      if (this._currentCmd.getModifier() != Constants.NOMODIFIER)
          cmd += Character.toString(this._currentCmd.getModifier());
      
      // Format the command, check for modifiers 


      //retrieve client date if supplied..If not, use current time
      //as init client time.
      long clientTime = System.currentTimeMillis();
      Date[] dates = this._currentCmd.getDates();
      if (dates != null && dates.length > 0 && dates[0] != null)
          clientTime = dates[0].getTime();
      
      //append timestamp and filename expression
      cmd += " " + clientTime + " " + regexp; 
      
      //reset shutdown handler (just in case one is still attached)
      this._resetShutdownHandler();
      
      //---------------------------
      
      try {
          try {
    
             // Change session type on server on change.
             this._setType(type);
             
             //------------------------
             
             //send request to server         
             this._logger.trace(this + " subscribePush command = \"" + cmd + "\"");
             io.writeLine(cmd);
    
             //------------------------
             
             //read reply from server: (1) OK, (2) error
             this._srvReply = io.readMessage();
             this._logger.trace(this + " subscribePush reply = \"" 
                                + this._srvReply + "\"");
             
             if (this._srvReply.getErrno() != Constants.OK) {
                this._endTransaction(Constants.NACKED, 
                                     this._srvReply.getMessage());
                return;
             }
             else
             {
                 this._logger.debug(this + " Subscription session initialized.");
             }
    
             //------------------------
             
             //set new shutdown handler
             synchronized(this) {
                 this._reqTermHandler = new SubscriptionShutdown(this._conn);
             }
             
             //------------------------
             
             // Read from reply loop.  Possiblities: (1) file info, (2) ping 
             //                                      request, (3) done/other
             
             while ((reply = io.readLine()) != null) 
             {
                this._logger.trace(this + " subscribePush reply: \"" 
                                   + reply + "\"");
                
                if (reply.length() == 0)
                   throw new SessionException("Unexpected eof from server",
                                              Constants.UNEXPECTED_EOF);
    
                
                //---------------------
                
                if (reply.charAt(0) == 'p') 
                {
                    //this is ping request, p <timestamp>, send ACK to server
                    clientTime = System.currentTimeMillis();
                    long serverTime = Long.parseLong(reply.substring(2).trim());
                    
                    String pingAck = "p ACK ";
                    pingAck += Long.toString(clientTime) + " ";
                    pingAck += Long.toString(serverTime);
                    this._logger.trace(this + " subscribePush acknowledging ping: \"" 
                            + pingAck + "\"");
                    io.writeLine(pingAck);
                    
                    //create result object to notify client that ping request
                    //was recv'd and not to timeout
                    fileInfo = new Result(this._currentCmd, Constants.PING_RECVD,
                                          "Ping request received from server");
                    this._session.postResult(fileInfo);                
                    continue;
                }
                
               //---------------------
                
                if (reply.charAt(0) == 'm') 
                {
                    //this is more files following notification, similar
                    //to a ping but without time stamps                    
                    String moreAck = Constants.GET_MORE_FILES;
                    this._logger.trace(this + " subscribePush requesting next file batch: \"" 
                            + moreAck + "\"");
                    io.writeLine(moreAck);
               
                    continue;
                }

                
                //---------------------
                
                else if (reply.charAt(0) == 'i')
                {
                    //file metadata
                    StringTokenizer st = new StringTokenizer(reply.substring(2), "\t");
                    fileName = st.nextToken();
                    date = st.nextToken();
                    fileSize = Long.parseLong(st.nextToken().trim());
                    fileInfo = new Result(this._currentCmd, fileName, fileSize,
                                          new Date(Long.parseLong(date)));
    
                    reply = io.readLine();
                    st = new StringTokenizer(reply, "\t");
                    fileInfo.setFileContributor(st.nextToken());
                    fileInfo.setFileCreationTime(new Date(Long.parseLong(
                                                 st.nextToken().trim())));
    
                    // See if there is a comment too.
                    comment = io.readLine();
                    this._logger.trace(this + " Comment = \"" 
                                       + comment + "\"");
                    if (comment.length() > 1)
                       fileInfo.setComment(comment.trim());
    
                    // See if there is an archiveNote too.
                    archiveNote = io.readLine();
                    this._logger.trace(this + " ArchiveNote = \"" 
                                       + archiveNote + "\"");
                    if (archiveNote.length() > 1)
                       fileInfo.setArchiveNote(archiveNote.trim());
    
                    // See if there is a checksum too.
                    checksum = io.readLine();
                    this._logger.trace(this + " Checksum = \"" 
                                       + checksum + "\"");
                    if (checksum.length() > 1)
                       fileInfo.setChecksum(checksum.trim());
                    
                     // See if there is a remote location too.
                    remoteLocation = io.readLine();
                    this._logger.trace(this + " Location = \"" 
                                       + remoteLocation + "\"");
                    if (remoteLocation.length() > 1)
                       fileInfo.setRemoteLocation(remoteLocation.trim());
                    
                    
                    //set restart cache if one was attached to the request
                    ClientRestartCache cache = _currentCmd.getClientRestartCache(); 
                    if (cache != null)
                        fileInfo.setClientRestartCache(cache);
                    
                    //-----------------
                    
                    if (DB_ACCURRACY_FIX_ENABLED)
                    {
                        //noticed that with restart enabled, we would end up 
                        //retriveing a file that we already had retrieved in 
                        //previous session. The cause was an accurracy issue 
                        //in the DB where accurracy was limited to 3/100 of a 
                        //second.  As such, we look for files that already exist 
                        //and fall within this range.  If found, then they are 
                        //ignored
                        
                        File file = new File(this._session.getDir(), fileName);                    
                        long modTime = Long.parseLong(date);
                        
                        //check if file already exists locally
                        if (file.exists())
                        {
                            long localSize = file.length();
                            
                            //now check if modtime is within delta of clientTime
                            //and file length (as it could be a resume transfer)
                            if (localSize == fileSize && (modTime < clientTime && 
                                              modTime > clientTime - DB_DELTA_MS))   
                            {
                                this._logger.trace(this + " Ignoring '"
                                           +fileName+"' . " + 
                                           "(per DB accuraccy workaround)");
                                continue;
                            }
                        }
                    }
                    
                    //-----------------

                    this._session.postResult(fileInfo);
                    this._logger.trace(this + " subscribe posted result; " +
                                       "going back for more");
                    continue;
                }
                
                //---------------------
                
                // '[' used to be the command char for push-subscribe.
                // Server will still send that character followed by
                //the KILLSUBSCRIPTION modifier to represent kill action
                else if (reply.charAt(0) == '[')
                {
                    //this a kill?
                    if (reply.charAt(1) == Constants.KILLSUBSCRIPTION) 
                    {
                        String rest = reply.substring(3);
                        //check if request was actually sent
                        
                        if (rest.startsWith("ACK "))
                        {     
                            boolean termRequested = false;
                            synchronized(this) {
                                termRequested = this._reqTermHandler == null ?
                                    false : this._reqTermHandler.isRunning();
                            }
                                
                            if (termRequested) 
                            {
                                StringTokenizer st = new StringTokenizer(rest.substring(4));
                                String sTime = st.nextToken();
                                String cTime = st.nextToken();
                                String msg = "Client request time: "+cTime+".  " +
                                             "Server ACK time: " + sTime + "."; 
                                _logger.debug(this + " Received termination"
                                              + " ACK from server.");
                                _logger.trace(this + " "+msg);
                                synchronized(this) {
                                    this._reqTermHandler.confirm();
                                }
                                continue;
                            }                    
                            else
                            {
                                _logger.error(this + " Received *UNREQUESTED* "
                                              +"termination"
                                              + " ACK from server.");
                                _logger.debug(this + " Reply = "+reply);
                                continue;
                            }
                        } //end_if_ACK
                    } //end_if_kill_modifier
                    break;                    
                }
                            
                //---------------------
                
                //unrecognized reply
                else
                {
                    break;
                }
                            
                //---------------------
                
             } //end_while
          } catch (IOException e) {
             String message = "IO exception during subscription session";
             try {
                io.writeLine("File transfer failed");
             } catch (IOException ne) {
                this._logger.trace(null, ne);
             } finally {
                this._endTransaction(Constants.IO_ERROR, message);
             }                         
             return;
          } 
          
          //---------------------------
    
          //check that we've got a 'done' mesg
          //Then check next message, if OK, then exit, else error
          
          this._logger.trace(this + " Exited item from server: \"" + reply + "\"");
          if (!reply.startsWith("done")) 
          {
             this._logger.trace(this + " Protocol Error: Expected 'done'");
             this._endTransaction(Constants.PROTOCOL_ERROR, reply);
             return;
          } 
          
          //---------------------------
          
          //read next message
          try {
              this._srvReply = io.readMessage();
              if (this._srvReply.getErrno() == Constants.OK)
              {
                  //normal termination
                  this._logger.trace(this + " Subscription session closed.");
                  this._endTransactionDoNotShowUser(Constants.OK, 
                                        this._srvReply.getMessage());
              }
              else
              {
                  //error condition
                  this._logger.error(this + " Subscription session "
                                     + "terminated abnormally.");
                  this._endTransaction(this._srvReply.getErrno(),  
                                       this._srvReply.getMessage());
              }
          } catch (IOException ioEx) {
              String message = "IO exception closing subscription session";
              try {
                 io.writeLine("File transfer failed");
              } catch (IOException ne) {
                 this._logger.trace(null, ne);
              } finally {
                 this._endTransaction(Constants.IO_ERROR, message);
              }
              return;
          } 
      } finally {
          this._resetShutdownHandler();
      }

      //---------------------------
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Sends a request to server to kill current subscription session.
    * Waits for notification from the subscription thread that it has 
    * completed.
    * @throws SessionException when session failure. 
    */
   
   private void _stopSubscription() throws SessionException {
       
       //if no current command, or if kill command does not match
       //current command, then warn and exit
       if (this._currentCmd == null || this._reqTermHandler == null ||
             (this._currentCmd.getCommand() !=
                                          this._currentCntlCmd.getCommand()))
       {
           this._logger.warn(this + " Stop subscription request valid " +
                             " only during subscription.");
           this._logger.debug(this + " Request = " +
                              _currentCntlCmd.getCommandString());
           return;
       }
       
       //invoke handler
       this._logger.trace(this + " killing subscribe push");  
       
       //returns only after another thread calls stop, (ie the proxy thread)
       this._reqTermHandler.start();
            
      //---------------------------
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Private method to show the servers
    * 
    * @throws SessionException when operation fails
    */
   private void _showServers() throws SessionException {
      String cmd;
      String reply;
      Result result;
      int errno;

      try {
         // Format the comment command.
         cmd = Constants.PROTOCOLVERSION + " "
               + this._currentCmd.getCommandString();
         this._logger.trace(this + " Sending command " + cmd 
                            + " to server");

         BufferedStreamIO io = this._conn.getIO();
         io.writeLine(cmd);
         StringBuffer resultMsg = new StringBuffer();
         PrintfFormat formatter = new PrintfFormat(
               "%-30s   %-30s   %-23s  %-23s\n");
         resultMsg.append(formatter.sprintf(new Object[] { "[Name]", "[Group]",
               "[Time Up]", "[Time Down]" }));
         resultMsg.append(formatter.sprintf(new Object[] {
               "------------------------------",
               "------------------------------", "-----------------------",
               "-----------------------" }));
         /* If there is an error, no list will follow. */
         this._srvReply = io.readMessage();
         this._logger.trace(this + " ShowServers reply = \"" 
                            + this._srvReply + "\"");
         if (this._srvReply.getErrno() != Constants.OK) {
            reply = io.readLine(); // Flush "done".
            this._endTransaction(this._srvReply.getErrno(), this._srvReply
                  .getMessage());
            return;
         }

         while ((reply = io.readLine()) != null) {
            this._logger.trace(this + " showservers reply: \"" + reply + "\"");
            if (reply.length() == 0)
               throw new SessionException("Unexpected eof from server",
                     Constants.UNEXPECTED_EOF);
            if (reply.charAt(0) != 'i')
               break;

            //Reading a file from server
            //Must check that we're not getting a File not found error
            // here!
            StringTokenizer st = new StringTokenizer(reply.substring(2), "\t");
            String servername = st.nextToken();
            String servergroup = st.nextToken();

            long starttime = Long.parseLong(st.nextToken());
            long stoptime = Long.parseLong(st.nextToken().trim());

            String starttimeStr = (starttime == -1 ? "N/A" : DateTimeUtil
                  .getDateCCSDSAString(new Date(starttime)));
            String stoptimeStr = (stoptime == -1 ? "N/A" : DateTimeUtil
                  .getDateCCSDSAString(new Date(stoptime)));

            resultMsg.append(formatter.sprintf(new Object[] { servername,
                  servergroup, starttimeStr, stoptimeStr }));
         }
         
         //read final message: 0: done
         MessagePkg doneMsg = io.readMessage(); 
         this._logger.trace(this + " ShowServers done reply = \"" + 
                            doneMsg + "\"");
         if (doneMsg.getErrno() != Constants.OK)
         {
             throw new SessionException("Protocol error occurred during " +
                     "showservers. Expected \"0: done\". Received: "+doneMsg,
                     Constants.PROTOCOL_ERROR);
         }
         
         //construct result object
         Result resultObj = new Result(this._currentCmd, 
                                       this._srvReply.getErrno(), 
                                       resultMsg.toString());
         resultObj.setEoT();
         this._session.postResult(resultObj);
      } catch (IOException e) {
         this._logger.trace(this + " showServers -----");
         this._endTransaction(Constants.IO_ERROR, e.getMessage());
      }
   }

   //----------------------------------------------------------------------
   
   /**
    * Set the file type on the server end if the current command results in a
    * change of type. Allows for multiplexing of commands over server
    * connections.
    * 
    * @param fileType the file type name
    * @throws SessionException when session failure
    * @throws IOException when network I/O failure
    */
   private void _setType(String fileType) throws SessionException, IOException {
      String cmd;

      this._logger.trace(this + " #### Current type = " + this._currentType);
      this._logger.trace(this + " #### new type = " + fileType);
      if (this._currentType == null || 
                this._currentType.compareTo(fileType) != 0) 
      {
         cmd = Constants.PROTOCOLVERSION + " "
               + Constants.CHANGETYPE + " " + fileType;
         this._logger.trace(this + " setType cmd : " + cmd);

         BufferedStreamIO io = this._conn.getIO();

         io.writeLine(cmd);
         this._srvReply = io.readMessage();
         if (this._srvReply.getErrno() != Constants.OK)
            throw (new SessionException(this._srvReply.getMessage(),
                  this._srvReply.getErrno()));
         else
            this._currentType = fileType;
      }
   }
   
   //----------------------------------------------------------------------
   
   private void _authenticateServerGroupUser() throws SessionException
   {             
       String[] args = this._currentCmd.getCommandArgs();
       
       if (args == null || args.length < 3 || args.length > 4)
       {
           this._endTransaction(Constants.MISSINGARG,
                                "Missing arguments for command.");
       }
       
       String user = args[0];
       String pwd  = args[1];
       String sg   = args[2];
       String type = (args.length == 4) ? args[3] : null;
       
       String cmd = Constants.PROTOCOLVERSION + " " +
                    Constants.AUTHSERVERGROUPUSER + " " +
                    user + " " + pwd;
       if (type != null)
           cmd += " " + type;
       
       this._logger.trace(this + " Sending command '" + cmd 
                          + "' to server");
       BufferedStreamIO io = this._conn.getIO();

       try {
           io.writeLine(cmd);
           this._srvReply = io.readMessage();
       } catch (IOException ioEx) {
           throw new SessionException(ioEx.getMessage(),
                                      Constants.IO_ERROR);
       }
       
       //if not ok, propagate error
       /*
       if (this._srvReply.getErrno() != Constants.OK &&
           this._srvReply.getErrno() != Constants.UNKNOWNCMD)
       {
           _logger.trace(this + "Authentication unsuccessful.  Message" +
                         " = "+ this._srvReply.getMessage() + " (" +
                         this._srvReply.getErrno()+")");
           throw new SessionException(this._srvReply.getMessage(),
                                      this._srvReply.getErrno());
       }
       */
       
       //post success result
       Result resultObj = new Result(_currentCmd, _srvReply.getErrno(),
                                     _srvReply.getMessage());
       resultObj.setEoT();
       this._session.postResult(resultObj);       
   }
   
   //----------------------------------------------------------------------
   
   private void _getAuthenticationToken() throws SessionException
   {             
       String[] args = this._currentCmd.getCommandArgs();
       
       if (args == null || args.length < 3 || args.length > 4)
       {
           this._endTransaction(Constants.MISSINGARG,
                                "Missing arguments for command.");
       }
       
       String user = args[0];
       String pwd  = args[1];
       String sg   = args[2];
       
       
       String cmd = Constants.PROTOCOLVERSION + " " +
                    Constants.GETAUTHTOKEN + " " +
                    user + " " + pwd;
       
       this._logger.trace(this + " Sending command '" + cmd 
                          + "' to server");
       BufferedStreamIO io = this._conn.getIO();

       try {
           io.writeLine(cmd);
           this._srvReply = io.readMessage();
       } catch (IOException ioEx) {
           throw new SessionException(ioEx.getMessage(),
                                      Constants.IO_ERROR);
       }
       
       //if not ok, propagate error
       /*
       if (this._srvReply.getErrno() != Constants.OK &&
           this._srvReply.getErrno() != Constants.UNKNOWNCMD)
       {
           _logger.trace(this + "Authentication unsuccessful.  Message" +
                         " = "+ this._srvReply.getMessage() + " (" +
                         this._srvReply.getErrno()+")");
           throw new SessionException(this._srvReply.getMessage(),
                                      this._srvReply.getErrno());
       }
       */
       
       //post success result
       Result resultObj = new Result(_currentCmd, 
                                     _srvReply.getErrno(),
                                     _srvReply.getMessage());
       resultObj.setEoT();
       this._session.postResult(resultObj);       
   }
   
   //----------------------------------------------------------------------
   
   private void _getAuthenticationType() throws SessionException
   {             
       String[] args = this._currentCmd.getCommandArgs();
       
       if (args == null || args.length < 1 || args.length > 1)
       {
           this._endTransaction(Constants.MISSINGARG,
                                "Missing arguments for command.");
       }
       
       String sg   = args[0];
       
       String cmd = Constants.PROTOCOLVERSION + " " +
                    Constants.GETAUTHTYPE + " ";
       
       this._logger.trace(this + " Sending command '" + cmd 
                          + "' to server");
       BufferedStreamIO io = this._conn.getIO();

       try {
           io.writeLine(cmd);
           this._srvReply = io.readMessage();
       } catch (IOException ioEx) {
           throw new SessionException(ioEx.getMessage(),
                                      Constants.IO_ERROR);
       }
       
       //post success result
       Result resultObj = new Result(_currentCmd, 
                                     _srvReply.getErrno(),
                                     _srvReply.getMessage());
       resultObj.setEoT();
       this._session.postResult(resultObj);       
   }
   
   
   //----------------------------------------------------------------------
   
   /**
    * Sends no-operation to server.
    */
   
   private void _noOperation() throws SessionException
   {                           
       String cmd = Constants.PROTOCOLVERSION + " " +
                    Constants.NOOPERATION;
              
       boolean quiet = (this._currentCmd.getTransactionId() == -1);
              
       this._logger.trace(this + " Sending command '" + cmd + 
                          "' to server");
       BufferedStreamIO io = this._conn.getIO();

       //write command, read reply
       try {
           io.writeLine(cmd);
           this._srvReply = io.readMessage();           
       } catch (IOException ioEx) {
           throw new SessionException(ioEx.getMessage(),
                                      Constants.IO_ERROR);
       }
       
       //if not ok, log trace error       
       if (this._srvReply.getErrno() != Constants.OK)
           //&& this._srvReply.getErrno() != Constants.UNKNOWNCMD)
       {
           _logger.trace(this + "No-op command returned with " +
                         "following message: " +
                         this._srvReply.getMessage() + " (" +
                         this._srvReply.getErrno()+")");
       }
       
       //if UNKCMD, eat up remaining 'OK done' message from server
       if (this._srvReply.getErrno() == Constants.UNKNOWNCMD)
       {
           try {
               io.readMessage();           
           } catch (IOException ioEx) {
               throw new SessionException(ioEx.getMessage(),
                                          Constants.IO_ERROR);
           }
       }
       
       if (!quiet)
       {
           this._endTransaction(this._srvReply.getErrno(),
                                this._srvReply.getMessage());        
       }
   }
   
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
       BufferedStreamIO io = this._conn.getIO();

       try {
           io.writeLine(cmd);
           this._srvReply = io.readMessage();   
       } catch (IOException ioEx) {
           throw new SessionException(ioEx.getMessage(),
                                      Constants.IO_ERROR);
       }
       
       //if not ok, log trace error       
       if (this._srvReply.getErrno() != Constants.OK)
       {
           _logger.trace(this + "Property exchange command returned with " +
                         "following message: " +
                         this._srvReply.getMessage() + " (" +
                         this._srvReply.getErrno()+")");
       }
       
       //if UNKCMD, eat up remaining 'OK done' message from server
       if (this._srvReply.getErrno() == Constants.UNKNOWNCMD)
       {
           try {
               io.readMessage();           
           } catch (IOException ioEx) {
               throw new SessionException(ioEx.getMessage(),
                                          Constants.IO_ERROR);
           }
       }
             
       if (!quiet)
       {
           this._endTransaction(this._srvReply.getErrno(),
                                this._srvReply.getMessage());        
       }
       
   }
   
   //----------------------------------------------------------------------
   
   private FileSystem getFileSystem()
   {
       if (this._fileSystem == null)
       {
           FileSystemFactory fsFactory = new FileSystemFactory();
           this._fileSystem = fsFactory.createFileSystem();
           
       }
       
       return this._fileSystem;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Negotiate file register request with server.
    * 
    * @throws SessionException when session fail
    */
   private void _registerFiles() throws SessionException 
   {
      String cmd;
      String reply;
      long fileSize;
      Result fileInfo;
      File f;
      boolean isLastFile;
      String[] fileNames = this._currentCmd.getFileNames();
      String currDir = this._currentCmd.getDirectory();
      String fileName, filePath;

      // First, make sure there are any files to add.
      if (fileNames == null || fileNames.length < 1) 
      {
         this._endTransaction(Constants.NO_FILE_SPECIFIED, "No files to register.");
         return;
      }
      for (int i = 0; i < fileNames.length; i++) 
      {          
         filePath = fileNames[i];
         isLastFile = (i == fileNames.length - 1);
         
         if (filePath.indexOf(File.separator) > -1) 
            f = new File(filePath);
         else          
            f = new File(currDir, filePath);            
         
         fileName = f.getName();
         this._logger.trace(this + " Registering fileName \"" + fileName + "\"");
         
         if (!f.exists()) 
         {
            fileInfo = new Result(this._currentCmd, Constants.FILE_NOT_FOUND,
                  "File \"" + fileName + "\" does not exist");
            if (isLastFile)
               fileInfo.setEoT();
            this._session.postResult(fileInfo);
            continue;
         }
         if (!f.isFile()) 
         {
            if (f.isDirectory()) 
            {
               fileInfo = new Result(this._currentCmd,
                     Constants.DIRECTORY_IGNORED, "File \"" + fileName
                           + "\" is a directory.");
            } 
            else 
            {
               fileInfo = new Result(this._currentCmd,
                     Constants.FILE_NOT_NORMAL, "File \"" + fileName
                           + "\" is not a normal file.");
            }
            if (isLastFile)
               fileInfo.setEoT();
            this._session.postResult(fileInfo);
            continue;
         }
         if (f.getAbsolutePath().matches("^.*\\s.*$")) 
         {
             fileInfo = new Result(this._currentCmd,
                     Constants.FILE_NOT_REGISTERED, "Absolute filepath \"" + 
                          f.getAbsolutePath() + "\" cannot contain whitespace.");
            if (isLastFile)
               fileInfo.setEoT();  
            this._session.postResult(fileInfo);
            continue;
         }

         fileSize = f.length();
         fileInfo = new Result(this._currentCmd, fileName, fileSize);

         BufferedStreamIO io = this._conn.getIO();

         // Send the add command request to the server.
         try {
            // Send change type command to server, if required.
            // Change session type on server on change.
            this._setType(_currentCmd.getType());
            this._logger.trace(this + " Comment: \"" + this._currentCmd.getComment()
                  + "\"");

            // Format the register command. 
            String options = "";
            if (this._currentCmd.getOption(Constants.RECEIPTONXFR))
                options += "receipt";
             else
                options += "noreceipt";

            cmd = Constants.PROTOCOLVERSION + " "
                  + this._currentCmd.getCommand() + this._currentCmd.getModifier()  
                  + " " + options + " " + filePath + " " + fileSize;
            
            if (this._currentCmd.getComment() != null
                  && this._currentCmd.getComment().length() > 1) 
            {
               cmd += " \"" + this._currentCmd.getComment(); // + "\"";
            }
            this._logger.trace(this + " register command: " + cmd);

            //write the command
            io.writeLine(cmd);
            
            // Read the reply.
            this._srvReply = io.readMessage();
            
            this._logger.trace(this + " #######  Got \"" + this._srvReply + 
            "\" from server");
            
            fileInfo.setErrno(this._srvReply.getErrno());
            fileInfo.setMessage(this._srvReply.getMessage());      
            
            //---------------------
            
            String localSrvReply = this._srvReply.getMessage();
            
            //get rid of quotes from string   
            localSrvReply = localSrvReply.replaceAll("\"", " ");
            
            String checkKeyString   = "Checksum:";
            String receiptKeyString = "Receipt Id:";
            
            
            //look for checksum
            if (localSrvReply.contains(checkKeyString))
            {                
               
                int index = localSrvReply.indexOf(checkKeyString);
                String substring = localSrvReply.substring(index +
                                            checkKeyString.length());
                substring = substring.trim();
                String[] parts = substring.split("\\s+");
                if (parts != null && parts.length > 0)
                {
                    String checksum = parts[0];
                    fileInfo.setChecksum(checksum);
                }
            }
            
            //look for receipt
            if (localSrvReply.contains(receiptKeyString))
            {               
                int index = localSrvReply.indexOf(receiptKeyString);
                String substring = localSrvReply.substring(index +
                                       receiptKeyString.length());
                substring = substring.trim();
                String[] parts = substring.split("\\s+");
                if (parts != null && parts.length > 0)
                {
                    String receipt = parts[0];
                    try {
                        fileInfo.setReceiptId(Integer.parseInt(receipt));    
                    } catch (NumberFormatException nfEx) {                        
                    }                    
                }
            }
            
         } catch (IOException e) {
            String message = "IO exception while registering file: "+e.getMessage();            
            this._endTransaction(Constants.IO_ERROR, message);            
            return;
         }
         
         // If this is the last file, tag the result with end of transaction.
         if (isLastFile)
            fileInfo.setEoT();
         
         this._session.postResult(fileInfo);
      }
   }
 
   //----------------------------------------------------------------------  
   
   /**
    * Unregister file by list or regular expression.
    * 
    * @throws SessionException when connection fails
    */
   
   private void _unregisterFiles() throws SessionException 
   {
       String cmd;
       String reply = null;
       String regexp = this._currentCmd.getRegExp();
       String[] fileNames = this._currentCmd.getFileNames();
       String type = this._currentCmd.getType();
       Result fileInfo;
       String task = "get by list or regexp";
       
       BufferedStreamIO io = this._conn.getIO();

       //if regex, then collect all files that match expr through an
       //intermediate getfiles command
       if (regexp != null) 
       {
           try {
               cmd = Constants.PROTOCOLVERSION + " "
                     + Constants.GETFILES
                     + Character.toString(Constants.REGEXP) + " " + regexp;
               this._logger.trace(this + " "+task+" command = \"" + cmd
                     + "\"");
               // Change session type on server on change.
               this._setType(type);
               io.writeLine(cmd);
               
               
               fileNames = _processResultsFromGetFilenames(io, cmd, task);
               
               if (fileNames == null)
               {
                   return;
               }
              
           } catch (IOException e) {
               String message = "IO exception while getting file names matching "
                   + regexp;
               try {
                   io.writeLine("File transfer failed");
               } catch (IOException ne) {
                   this._logger.trace(null, ne);
               } finally {
                   this._endTransaction(Constants.IO_ERROR, message);
               }
               return;
           }
       }             
       

       
       //--------------------------
              
       for (int i = 0; i < fileNames.length; ++i) 
       {
           try {
              // Send change type command to server, if required.
              this._setType(this._currentCmd.getType());

              // Format the delete command.
              cmd = Constants.PROTOCOLVERSION + " "
                    + this._currentCmd.getCommand()
                    + " "                  
                    + fileNames[i];
              fileInfo = new Result(this._currentCmd, fileNames[i]);
              // Send the add command request to the server.
              io.writeLine(cmd);
              // Read the reply.
              this._srvReply = io.readMessage();
              fileInfo.setErrno(this._srvReply.getErrno());
              fileInfo.setMessage(this._srvReply.getMessage());
              this._session.postResult(fileInfo);
           } catch (IOException e) {
              String message = "IO exception while unregistering file";
              this._endTransaction(Constants.IO_ERROR, message);              
              return;
           }
        }

       this._endTransactionDoNotShowUser(Constants.OK, "");
   }
   
   //----------------------------------------------------------------------  
   
   //----------------------------------------------------------------------
   
   /**
    * Service a close connection request. Note: if the file type of the close
    * request is null, then this is a close admin request.
    * 
    * @throws SessionException when session failure
    */
   private void _closeConnection() throws SessionException {
      Request nextCmd;
      String ft = this._currentCmd.getType();
      String msg; 

      try {
         msg = (ft == null) ?
                 this + " we are in close without a type" :
                 this + " we are in close type "+ft;
          
         this._logger.trace(msg);
         synchronized (this._requests) 
         {
            int size = this._requests.size();
            // Remove all commands for this type.
            for (int i = size - 1; i >= 0; --i) 
            {
               nextCmd = (Request) this._requests.get(i);
               this._logger.trace(this + " nextCmd type = " + 
                                  nextCmd.getType());
               this._logger.trace(this + " currentCmd type = " +
                                  this._currentCmd.getType());
               
               // Admin close / FileType close.
               if ( (this._currentCmd.getType() == null && 
                     nextCmd.getType() == null) ||
                    (this._currentCmd.getType() != null &&
                     nextCmd.getType() != null &&
                     nextCmd.getType().compareTo(this._currentCmd.getType()) == 0))
               {
                  this._requests.remove(i);
               }
            }
            this._logger.trace(this + " refcount first " + this._refCount);
            if (this.decrementRefCount() < 1) 
            {
               //send quit command to server
               this._logger.trace(this + " bye");
               //this._conn.getIO().writeLine("q");
               this._conn.getIO().writeLine(Constants.QUIT);
               
               //remove self as proxy for the server
               if (this._serverInfo.getProxy() == this)
                   this._serverInfo.setProxy(null);
            } 
            else 
            {
               this._logger.trace(this + " refcount now " + this._refCount);
            }
         }
      } catch (IOException e) {
         this._logger.trace(this + " ServerProxy.closeConnection.run ()", e);
      }
            
      this._logger.trace(this + " End transaction: OK Connection closed.  ");
      this._endTransaction(Constants.OK, "Connection closed.");
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
   
   private boolean isControlRequest(Request req)
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
       else
       {
           
       }
       
       return isControl;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Convenince method that resets request shutdown handler.
    */
   
   private synchronized void _resetShutdownHandler()
   {
       //reset shutdown handler reference
       synchronized(this) {
           if (this._reqTermHandler != null)
           {
               this._reqTermHandler.stop();
               this._reqTermHandler = null;
           }
       }
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Sets the state to terminated and notifies all waiting threads so
    * that they can exit.
    */
   
   private void terminate()
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
       this._pulseThread.interrupt();
   }

   //----------------------------------------------------------------------
   
   /**
    * Returns reference to server info object passed-in during construction.
    * @return Server proxy's server info reference
    */
   
   public ServerInfo getServerInfo()
   {
       return this._serverInfo;
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
   
   /**
    * Returns string representing this serverproxy, including its id.
    * @return This object's string representation.
    */
   
   public String toString()
   {
       return "[ServerProxy_"+this._id+"]";
   }
   

   
   //----------------------------------------------------------------------
   
   protected void _sendFileChecksum(BufferedStreamIO io, Request request, 
                                    String fileName) throws SessionException
   {
       String checksum = null;
       try {
           checksum = FileUtil.getStringChecksum(fileName);
       } catch (IOException ioEx) {
           checksum = "";
           if (false)
               throw new SessionException("IO error occurred while computing " +
                    "checksum for file "+fileName+": "+ioEx.getMessage(), 
                    Constants.IO_ERROR);    
       }
       
       try {
           this._logger.trace(this + " sending checksum: " + checksum);
           io.writeLine(checksum);           
       } catch (IOException ioEx) {
           throw new SessionException("IO error occurred while sending " +
           		            "checksum to server: "+ioEx.getMessage(), 
           		            Constants.IO_ERROR);           
       }
   }
   
   //======================================================================

   class SubscriptionShutdown extends RequestTerminationHandler
   {
       Connection conn;
       
       //------------------------------------------------------------------
       
       public SubscriptionShutdown(Connection conn)
       {
           super(true);
           this.conn = conn;       
       }
       
       //------------------------------------------------------------------
       
       protected void shutdown()
       {
           String cmd = "";      
           BufferedStreamIO io = this.conn.getIO();
           
           //cmd starts sub id, modified, then client date
           cmd = cmd +  Constants.SUBSCRIBEPUSH + Constants.KILLSUBSCRIPTION + 
                                             " " + System.currentTimeMillis();
           
           //---------------------------
           
           try {
              //send request to server         
              _logger.trace(ServerProxy.this +
                            " kill subscribePush command = \"" + cmd + "\"");
              io.writeLine(cmd);                       
           } catch (IOException e) {
              String message = "IO exception while terminating subscription session";
              try {
                 io.writeLine("File transfer failed");
              } catch (IOException ne) {
                 _logger.trace(null, ne);
              } finally {
                 _endTransaction(Constants.IO_ERROR, message);
              }
           }
       }      
       
       //------------------------------------------------------------------
       
       public void postShutdown()
       {
           if (this.isSuccessful())
               _logger.debug(ServerProxy.this + " Subscription shutdown " +
                             "handler completed successfully.");        
           else
               _logger.debug(ServerProxy.this + " Subscription shutdown " +
                             "handler operation aborted.");
       }
       
       
       
       //------------------------------------------------------------------
       
   } //end_class
   
   //======================================================================
   
}