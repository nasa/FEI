package jpl.mipl.mdms.FileService.komodo.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.regex.PatternSyntaxException;

import jpl.mipl.mdms.FileService.io.BoundedBufferedReader;
import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Domain;
import jpl.mipl.mdms.FileService.komodo.api.DomainFactory;
import jpl.mipl.mdms.FileService.komodo.api.DomainFactoryIF;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerManager;
import jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerSet;
import jpl.mipl.mdms.FileService.komodo.client.handlers.FileResultError;
import jpl.mipl.mdms.FileService.komodo.client.handlers.FileResultEvent;
import jpl.mipl.mdms.FileService.komodo.client.handlers.HandlerException;
import jpl.mipl.mdms.FileService.komodo.services.query.client.QueryClient;
import jpl.mipl.mdms.FileService.komodo.services.query.util.QueryClientUtil;
import jpl.mipl.mdms.FileService.komodo.services.query.util.QueryResultsCollector;
import jpl.mipl.mdms.FileService.komodo.services.query.util.Utils;
import jpl.mipl.mdms.FileService.komodo.util.AuthenticationType;
import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;
import jpl.mipl.mdms.FileService.komodo.util.InvocationCommandUtil;
import jpl.mipl.mdms.FileService.komodo.util.LoginFile;
import jpl.mipl.mdms.FileService.komodo.util.PublicKeyEncrypter;
import jpl.mipl.mdms.FileService.komodo.util.PushFileEventQueue;
import jpl.mipl.mdms.FileService.komodo.util.PushSubscriptionClient;
import jpl.mipl.mdms.FileService.komodo.util.ReconnectThrottle;
import jpl.mipl.mdms.FileService.komodo.util.SubscriptionEvent;
import jpl.mipl.mdms.FileService.komodo.util.SubscriptionEventListener;
import jpl.mipl.mdms.FileService.komodo.util.UserAuthenticator;
import jpl.mipl.mdms.FileService.komodo.util.UserToken;
import jpl.mipl.mdms.FileService.util.ConsolePassword;
import jpl.mipl.mdms.FileService.util.DateTimeFormatter;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.Errno;
import jpl.mipl.mdms.FileService.util.ExpressionEvaluator;
import jpl.mipl.mdms.FileService.util.FileUtil;
import jpl.mipl.mdms.FileService.util.PasswordUtil;
import jpl.mipl.mdms.FileService.util.PrintfFormat;
import jpl.mipl.mdms.FileService.util.SystemProcess;

import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Handler for FEI utility client scripts. Each script defines value for
 * property 'mdms.user.operation' to select which action the client will use. 
 * 
 *   <PRE>
 *   Copyright 2006, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2006.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 00/00/2003        Thomas           Initial release. 
 * 07/13/2005        Nick             Initial documentation.
 * 09/16/2005        Nick             Added push version of notify
 * 03/09/2006        Nick             Adding date formatter for querying and
 *                                    reporting file times.
 * ============================================================================
 * </PRE>
 *
 * @author Thomas Huang     (Thomas.Huang@jpl.nasa.gov)
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: UtilClient.java,v 1.194 2017/01/13 00:58:59 ntt Exp $
 *
 */

public class UtilClient 
{
   private static final long _MINUTE_MS = 60000;
   private static final long _DAY_MS = 86400000;

   // reference to client object
   private Client _client = null;
   
   // utility client used for push metadata subscriptions
   private PushSubscriptionClient _subscriptionClient;
   
   // utility client used for the feiQ Query service
   private QueryClient _queryClient;
   
   //utility registry for file event handling
   private FileEventHandlerSet _fileEventHandlerSet;
   
   // Action id
   private String _actionId = Constants.NOOPERATION;
   

   // Reference ot the argument array
   private String[] _args = null;

   private boolean _using = false;
   private UtilCmdParser _parser;
   private Hashtable _argTable = new Hashtable();
   private int _resultIdx = 0;
   private int _errorCount = 0;
   private DateTimeFormatter _dateFormatter;
   private ReconnectThrottle _throttle;
   
   //property values
   private final String _userOperation;
   private final String _domainFilename;
   //private final String _restartDir;
   private final String _userScript;
   //private final String _loginFile;
   private final String _queryInterval;

   private URL _domainFile;
   
   private LoginFile _loginReader;

   private PublicKeyEncrypter _encrypter;
   private UserAuthenticator _userAuthenticator; 
   
   
   private final static String ERROR_TAG = "FEI_ERROR::";

   //mail property values
   private String _mailFrom         = null;
   private String _mailTo           = null;
   private String _smtpHost         = null;
   private String _mailReportTo     = null;
   private String _mailReportAt     = null;
   private boolean _mailSilentRecon = false;   
   
   
   // buffer used to collect report information
   private StringBuffer _reportmessage = new StringBuffer();

   //Loggers
   private Logger _logger = Logger.getLogger(UtilClient.class.getName());
   private Logger _emailMessageLogger = Logger
         .getLogger("UtilClient.Email.Message");
   private Logger _emailReportLogger = Logger
         .getLogger("UtilClient.Email.Report");
   private Timer _reportScheduler = null;
   
   // Mail flags
   private boolean _mailmessage = false;
   private boolean _mailreport = false;
    
   


   //---------------------------------------------------------------------

   /**
    * Constructor, accepts string array object which contains the command line
    * arguments.
    * 
    * @param args The command line arguments
    */
   public UtilClient(String[] args) 
   {
       
      //set reference to args parameter
      this._args = args;
      this._parser = new UtilCmdParser();

            
      //set the restart directory
//      String restartdir = System.getProperty(Constants.PROPERTY_RESTART_DIR);
//      if (restartdir == null) 
//      {
//          restartdir = System.getProperty(Constants.PROPERTY_USER_HOME);
//         if (restartdir == null)
//            restartdir = System.getProperty("user.home") + File.separator
//                  + Constants.RESTARTDIR;
//      }
//      this._restartDir = restartdir + File.separator + Constants.RESTARTDIR;
//      this._loginFile = this._restartDir + File.separator + Constants.LOGINFILE;
      
      
      //set the values of properties
      //this._domainFile = System.getProperty(Constants.PROPERTY_DOMAIN_FILE);
      
      this._domainFilename = System.getProperty(Constants.PROPERTY_DOMAIN_FILE);
      this._userScript     = System.getProperty(Constants.PROPERTY_USER_APPLICATION);
      this._userOperation  = System.getProperty(Constants.PROPERTY_USER_OPERATION);
      this._queryInterval  = System.getProperty(Constants.PROPERTY_QUERY_INTERVAL);
      
      //get the action id based on the operation name
      //this._actionId = ActionTable.toId(this._userOperation);
      this._actionId = ActionTable.toId(this._userOperation);
           
   }

   //---------------------------------------------------------------------

   /**
    * Method to activate the util client to begin processing user inputs
    *  
    */

   public void run() {

       ConfigFileURLResolver resolver = new ConfigFileURLResolver();      
       
       try {
           this._domainFile = resolver.getFileURL(this._domainFilename);
       } catch (SessionException sesEx) {
           this._logger.error(ERROR_TAG + "Error occurred while loading login file: "+sesEx.getMessage());
           ++this._errorCount;
           return;
       }
       
       //--------------------------
       
       //try to create login file instance
       try {
           _loginReader = new LoginFile();
       } catch (IOException ioEx) {
           this._logger.error(ERROR_TAG + "Error occurred while loading login file");
           ++this._errorCount;
           return;
       }
       
       //--------------------------
       
       //try to create encrypter instance
       try {
           _encrypter = new PublicKeyEncrypter();
       } catch (IOException ioEx) {
           this._logger.error(ERROR_TAG + "Error occurred while loading encrypter");
           //ioEx.printStackTrace();
           this._logger.trace(null, ioEx);
           ++this._errorCount;
           return;
       }
       
       //--------------------------
       
       //create reconnect throttle 
       
       _throttle = new ReconnectThrottle();
      
       //--------------------------
       
      //install shutdown handler to hook
      //Runtime.getRuntime().addShutdownHook(new ShutDownHandler());
       
      //check action id
      if (this._actionId.equals(Constants.NOOPERATION)) {
         this._logger.error(ERROR_TAG + "Unrecognized user operation: "
               + this._userOperation);
         ++this._errorCount;
         return;
      }

      boolean parseError = false;

      try {
         //construct parser and parser
         this._parser.parse(this._actionId, this._args);
      } catch (ParseException pEx) {
         parseError = true;

         //if no arguments were provided and exception is missing args,
         //then just send message to debug and rely on help message
         if (this._args.length == 0 && 
             pEx.getErrorOffset() == UtilCmdParser.CODE_MISSING_ARGUMENTS)
            this._logger.debug(ERROR_TAG + pEx.getMessage());
         else
            this._logger.error(ERROR_TAG + pEx.getMessage());
         this._logger.debug(null, pEx);
      } catch (Exception ex) {
         parseError = true;
         this._logger.error(ERROR_TAG + ex.getMessage());
         this._logger.debug(null, ex);
      }

      if (parseError) {
         //attempt to logout
         try {
            this._logout();
         } catch (Exception le) {
            this._logger.debug(null, le);
         }
         ++this._errorCount;
      }

      //-------------------------

      boolean help = parseError || 
                     (this._parser == null) || this._parser.printHelp();

      //if help is true, print usage and return
      if (help) {
         this._logger.info(this._getUsage());
         return;
      }

      
      //-------------------------

//      FileEventHandlerManager fehManager = new FileEventHandlerManager(
//                                           this._argTable, this._actionId);      
//      this._fileEventHandlerSet = fehManager.getFileEventHandlers();
      
      //-------------------------

      this._using = (this._parser.getOptionsFilename() != null);

      try {
         //iterate over number of argument sets, processing each

         Hashtable argTable;
         this._parser.reset();
         int iterations = this._parser.iterations();
         for (int i = 0; i < iterations; ++i) {
            argTable = (Hashtable) this._parser.getCurrentArguments();
            
            try {
               _process(argTable);
            } catch (SessionException sesEx) {
               this._logger.error(ERROR_TAG + sesEx.getMessage());
               this._logger.debug(null, sesEx);
               try {
                  this._logout();
               } catch (Exception le) {
                  this._logger.debug(null, le);
               }
               ++this._errorCount;
               //return;
            }
            this._parser.advance();
         }

         //-------------------------

         //logout successfully
         this._logout();

         //-------------------------

      } catch (Exception ex) {
         this._logger.error(ERROR_TAG + ex.getMessage());
         this._logger.debug(null, ex);
         try {
            this._logout();
         } catch (Exception le) {
            this._logger.debug(null, le);
         }
         ++this._errorCount;
         return;
      }

   }

   //---------------------------------------------------------------------

   /**
    * Method to return the total number of error counts. Since a single
    * UtilClient command can translate into multiple transactions. The error
    * count keeps track of the number of unsuccessful transactions.
    * 
    * @return the number of error encountered during program execution
    */
   public int getErrorCount() {
      return this._errorCount;
   }

   //---------------------------------------------------------------------
   
   /**
    * Instantiates new file handler set, passing this
    * session's argument table and action id.  If a set pre-exists,
    * it will be destroyed.
    */
   
   protected void _loadHandlers() throws SessionException
   {
       if (this._fileEventHandlerSet != null)
       {
           this._logger.trace("Releasing old set of file event handlers.");    
           this._fileEventHandlerSet.destroy();
       }
       
       //--------------------------
       
       this._logger.trace("Creating file event handler manager.");
       
       //create new manager, and get a (possible empty) handle set from it
       FileEventHandlerManager fehManager = new FileEventHandlerManager(
                                                        this._argTable, 
                                                        this._actionId);      
       this._fileEventHandlerSet = fehManager.getFileEventHandlers();
       
       //check for error
       if (this._fileEventHandlerSet.hasError())
       {
           throw new SessionException("Error occured while loading file handlers",
                                      Constants.EXCEPTION);
       }
       
       //dump list of ids if debug enabled
       if (this._logger.isDebugEnabled())
       {
           String[] ids = _fileEventHandlerSet.getHandlerIds();
           StringBuffer buf = new StringBuffer("Handler set ids: { ");
           if (ids.length > 0)
               buf.append(ids[0]);
           if (ids.length > 1)
               for (int i = 1; i < ids.length; ++i)
                   buf.append(", ").append(ids[i]);
           buf.append("}");
           this._logger.debug(buf.toString());    
           
       }
   }
   
   //---------------------------------------------------------------------
   
   protected void _unloadHandlers()
   {
       if (this._fileEventHandlerSet != null)
           this._fileEventHandlerSet.destroy();
   }
   
   //---------------------------------------------------------------------

   /**
    * Main process method, check action and call appropriate method
    * 
    * @param argTable The argument hashtable
    * @return boolean true if success, false if failure
    * @throws SessionException when operation fails
    * @throws IOException if IO error occurs
    */

   private boolean _process(Hashtable argTable) throws SessionException,
                                                       IOException 
   {
      this._argTable = argTable;

      
      boolean success = true;
      
      //----------------------------
      
      
     //if server communication expected, ensure that necessary info 
     //is set.  Also, load the handler framework
     final boolean isLocalOperation = _isLocalOperation(this._actionId);
     final boolean filetypeRequired = _isFileTypeOperation(this._actionId);
     final boolean checkHandlers    = filetypeRequired;
     if (! isLocalOperation)
     {     
         this._checkConfig(filetypeRequired);
         
         this._login();
         
         if (checkHandlers)
             this._loadHandlers();
     }
     
     //----------------------------
     
     if (this._actionId.equals(Constants.ACCEPT))
     {
         success = this._accept();
     }
     else if (this._actionId.equals(Constants.ADDFILE))
     {
         success = this._add();
     }
     else if (this._actionId.equals(Constants.CHECK))
     {
         success = this._check();
     }
     else if (this._actionId.equals(Constants.GETFILES))
     {
         success = this._get();
     }
     else if (this._actionId.equals(Constants.AUTOGETFILES))
     {
         success = this._autoGet();
     }     
     else if (this._actionId.equals(Constants.AUTOSHOWFILES))
     {
         success = this._autoShow();
     }
     else if (this._actionId.equals(Constants.COMPUTECHECKSUM))
     {
         success = this._crc();
     }
     else if (this._actionId.equals(Constants.CHECKFILES))
     {
         success = this._checkFiles();
     }
     else if (this._actionId.equals(Constants.DELETEFILE))
     {
         success = this._delete();
     }
     else if (this._actionId.equals(Constants.MAKECLEAN))
     {
         success = this._delete();
     }
     else if (this._actionId.equals(Constants.DISPLAY))
     {
         success = this._display();
     }
     else if (this._actionId.equals(Constants.SHOWFILES))
     {
         success = this._list();
     }
     else if (this._actionId.equals(Constants.SETREFERENCE))
     {
         success = this._reference();
     }
     else if (this._actionId.equals(Constants.RENAMEFILE))
     {
         success = this._rename();
     }
     else if (this._actionId.equals(Constants.REPLACEFILE))
     {
         success = this._replace();
     }
     else if (this._actionId.equals(Constants.COMMENTFILE))
     {
         success = this._comment();
     }
     else if (this._actionId.equals(Constants.CREDLIST))
     {
         success = this._credList();
     }
     else if (this._actionId.equals(Constants.CREDLOGIN))
     {
         success = this._credInit();
     }
     else if (this._actionId.equals(Constants.CREDLOGOUT))
     {
         success = this._credDestroy();
     }
     else if (this._actionId.equals(Constants.SHOWTYPES))
     {
         success = this._showTypes();
     }
     else if (this._actionId.equals(Constants.REGISTERFILE))
     {
         success = this._register();
     }     
     else if (this._actionId.equals(Constants.UNREGISTERFILE))
     {
         success = this._unregister();
     }
     else if (this._actionId.equals(Constants.LOCKFILETYPE))
     {
         success = this._lockType();
     }
     else if (this._actionId.equals(Constants.UNLOCKFILETYPE))
     {
         success = this._unlockType();
     }
     else if (this._actionId.equals(Constants.CHANGEPASSWORD))
     {
         success = this._changePassword();
     }
     else
     {
         throw new SessionException("Invalid operation.", -1);
     }
     
      
      this._unloadHandlers();
      this._logout();
      
      return success;
   }

   //---------------------------------------------------------------------
   
   /**
    * Returns true if operation is local only, and does not
    * communicate with the server.  Called by process().
    * @param actionId The action id
    * @return True if local only action, false otherwise.
    */
   
   protected boolean _isLocalOperation(String actionId)
   {
       if (this._actionId.equals(Constants.COMPUTECHECKSUM) ||
           this._actionId.equals(Constants.CHECK)           ||
           this._actionId.equals(Constants.CREDLIST)        ||
           this._actionId.equals(Constants.CREDLOGIN)       ||
           this._actionId.equals(Constants.CREDLOGOUT)      ||
           this._actionId.equals(Constants.SHOWTYPES))
           return true;
       else
           return false;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Returns true if operation is filetype operation. This calls
    * _isLocalOperation first, then checks for other non-filetype
    * specific ops.  Called by process().
    * @param actionId The action id
    * @return True if filetype action, false otherwise.
    */
   
   protected boolean _isFileTypeOperation(String actionId)
   {
       boolean isLocal = _isLocalOperation(actionId);
       if (isLocal)
       {
           return false;
       }
       
       if (this._actionId.equals(Constants.CHANGEPASSWORD))
       {
           return false;
       }
       else
           return true;  //ntt-Jun102013: this was return false
    }
   
   //---------------------------------------------------------------------

   /**
    * Log into FEI only if not already logged in
    * 
    * @throws SessionException when operation fails
    */
   private void _login() throws SessionException {

      
         String servergroup = (String) this._argTable.get(CMD.SERVERGROUP);
         String filetype = (String) this._argTable.get(CMD.FILETYPE);
         String user = (String) this._argTable.get(CMD.USER);
         String password = (String) this._argTable.get(CMD.PASSWORD);
         this._client = new Client(this._domainFile);
         
         //if (servergroup == null) {
         //   servergroup = this._client.getDefaultGroup();
         //   if (servergroup != null)
         //      this._argTable.put(CMD.SERVERGROUP, servergroup);
         //}
         if (servergroup != null)
         {
             if (!this._client.isGroupDefined(servergroup))
                 throw new SessionException("Group " + servergroup
                       + " not found in domain!", Constants.DOMAINLOOKUPERR);             
         }
         
         //if (servergroup != null && !l.contains(servergroup))
         //   throw new SessionException("Group " + servergroup
         //         + " not found in domain!", Constants.DOMAINLOOKUPERR);
         if (filetype != null)
         {
        	 this._client.login(user, password, servergroup, filetype);
         }
         else
         {
        	 this._client.login(user, password);
        	 if (servergroup != null)
        	     this._client.setDefaultGroup(servergroup);
         }
         
         if (this._argTable.get(CMD.SERVERGROUP) == null)
        	 this._argTable.put(CMD.SERVERGROUP, 
        	                    this._client.getDefaultGroup());
             
   }

   //---------------------------------------------------------------------

   /**
    * Log out of FEI if logged in, otherwise do nothing.
    * 
    * @throws SessionException when operation fails.
    */
   private void _logout() throws SessionException {

      if (this._client != null && this._client.isLoggedOn())
         this._client.logout();
   }

   //---------------------------------------------------------------------

   /**
    * Create user credential cache file
    * 
    * @throws FileNotFoundException if cache file is not found
    * @throws IOException if cache file is not readable
    */
   private boolean _credInit() throws FileNotFoundException, IOException 
   {

      String user = (String) this._argTable.get(CMD.USER);
      String svgp = (String) this._argTable.get(CMD.SERVERGROUP);
      String password = null;

      //get the server group first
      if (svgp == null) 
      {
          while (svgp == null)
          {
              //if servergroup is null, query.If empty, set to null (which is default)              
              System.out.print("Server group>> ");
              svgp = this._readTTYLine();
              //System.out.println("");
              svgp = svgp.trim();
              if (svgp.equals(""))
                  svgp = null;
               
              if (svgp == null)
              {
                  System.out.println("Server group must be specified.  Enter 'abort' to quit.");
              }
              else if (svgp.equalsIgnoreCase("abort") || svgp.equalsIgnoreCase("exit") ||
                       svgp.equalsIgnoreCase("quit")  || svgp.equalsIgnoreCase("bye")) 
              {
                  System.exit(0);
              }
          }
      }
      else
      {
          System.out.println("Server group>> "+svgp);          
      }
      
      //---------------------------

      //request the authentication method from the servergroup
      AuthenticationType authType = null;
      try {
          authType = getAuthenticationType(svgp);
      } catch (SessionException sesEx) {
          
          if (sesEx.getErrno() == Constants.CONN_FAILED)
          {
              this._logger.error("Unable to connect to server group '" +
                      svgp+"'.\nPlease check network status and "+
                      "FEI domain file configuration.");
              
//              this._logger.severe("Unable to authenticate for group '"+svgp+
//                      "' due to connection failure.  Possible network issue.");
          }
          else
          {
              this._logger.severe("Authentication Error! Unable to query server "+
                      "authentication method for servergroup '"+svgp + "'. ("+
                      sesEx.getErrno()+")");
          }
          _logger.trace(null,sesEx);
          return false;
      }
      
      final String pwdPrompt = PasswordUtil.getPrompt(authType);

      //---------------------------
      
      if (user == null) 
      {
         System.out.print("User name>> ");
         user = this._readTTYLine();
         user = user.trim();
      }     
      else
      {
          System.out.print("User name>> "+user);
      }
      
      boolean ok = false;      
      while (!ok)
      {      
          password = ConsolePassword.getPassword(pwdPrompt+">> ");
          //System.out.println("");
          password = password.trim();
          if (!password.equals(""))
              ok = true;              
      }
      
      //System.out.println("");
      //password = password.trim();
      
      if (password.equalsIgnoreCase("abort")   || password.equalsIgnoreCase("exit") ||
          password.equalsIgnoreCase("quit") || password.equalsIgnoreCase("bye")) 
      {
         System.exit(0);
      }
      
      
      //create the encrypter for passwords
      String encrypted = null;
      try {
          encrypted = this._encrypter.encrypt(password);
      } catch (Exception ex) {
          this._logger.severe("Password encrypt Error! Could not write login credentials.");
          _logger.trace(null,ex);
          return false;
      }
      

          
      UserToken userToken = null;
      String authToken = encrypted;
      try {
          userToken = getAuthenticationToken(svgp, user, encrypted);
      } catch (SessionException sesEx) {
          this._logger.severe("Authentication Error! Could not authenticate "+
                  "user '"+user+"' for servergroup '"+svgp + "'.");                  
          _logger.trace(null,sesEx);
          return false;
      }
      
      
      //user was not authenticated
      if (userToken == null || !userToken.isValid())
      {
          this._logger.severe("Authentication Error: Could not authenticate "+
                  "user '"+user+"' for servergroup '"+svgp + "'.");          
          return false;
      }
      
      this._loginReader.setUsername(svgp, user);
      this._loginReader.setPassword(svgp, userToken.getToken());
      this._loginReader.setExpiry(  svgp, userToken.getExpiry());
      
      
      boolean success = true;
      try {
          this._loginReader.commit();
      } catch (IOException ioEx) {
          this._logger.severe("Login File Error! Could not write login credentials.");
          _logger.trace(null, ioEx);
          success = false;
          ++this._errorCount;
      }
      return success;
     
   }
   
   //---------------------------------------------------------------------
   
   private UserAuthenticator getAuthenticator()
   {
       if (this._userAuthenticator == null)
       {
           try {
               this._userAuthenticator = new UserAuthenticator(this._domainFile);
           } catch (SessionException sesEx) {
               this._logger.severe("Authentication Token Generator Error! Could not create generator.");
               _logger.trace(null, sesEx);
               ++this._errorCount;
               return null;
           }
       }
       
       return this._userAuthenticator;
   }
   
   //---------------------------------------------------------------------
   
   private UserToken getAuthenticationToken(String servergroup, 
                     String user, String password) throws SessionException
   {
       UserToken token = null;
       
       UserAuthenticator auth = getAuthenticator();
       
       token = auth.authenticate(user, password, servergroup);
       
       return token;
   }
   
   //---------------------------------------------------------------------
   
   private AuthenticationType getAuthenticationType(String servergroup) 
                                       throws SessionException
   {
       AuthenticationType authType = null;

       UserAuthenticator auth = getAuthenticator();

        authType = auth.getAuthenticationType(servergroup);

        return authType;
   }
   
   //---------------------------------------------------------------------
   
   private boolean _credInitOld() throws FileNotFoundException, IOException 
   {

      String user = (String) this._argTable.get(CMD.USER);
      String svgp = (String) this._argTable.get(CMD.SERVERGROUP);
      String password = null;

      if (user == null) 
      {
         System.out.print("User name>> ");
         user = this._readTTYLine();
         user = user.trim();
      }     
      
      boolean ok = false;      
      while (!ok)
      {      
          password = ConsolePassword.getPassword("Password>> ");
          //System.out.println("");
          password = password.trim();
          if (!password.equals(""))
              ok = true;              
      }
      
      //System.out.println("");
      //password = password.trim();
      
      if (password.equalsIgnoreCase("abort") || user.equalsIgnoreCase("exit")
          || user.equalsIgnoreCase("quit") || user.equalsIgnoreCase("bye")) 
      {
         System.exit(0);
      }
      
      
      //create the encrypter for passwords
      String encrypted = null;
      try {
          encrypted = this._encrypter.encrypt(password);
      } catch (Exception ex) {
          this._logger.severe("Password encrypt Error! Could not write login credentials.");
          _logger.trace(null,ex);
          return false;
      }
      
      //if servergroup is null, query.If empty, set to null (which is default)
      if (svgp == null) 
      {
         System.out.print("Server group [ENTER for default]>> ");
         svgp = this._readTTYLine();
         System.out.println("");
         svgp = svgp.trim();
         if (svgp.equals(""))
             svgp = null;
      }
      else
      {
          System.out.println("Setting login for server group '"+svgp+"'"); 
          System.out.println("");
      }
      
      
      this._loginReader.setUsername(svgp, user);
      this._loginReader.setPassword(svgp, encrypted);
      
      boolean success = true;
      try {
          this._loginReader.commit();
      } catch (IOException ioEx) {
          this._logger.severe("Login File Error! Could not write login credentials.");
          _logger.trace(null, ioEx);
          success = false;
          ++this._errorCount;
      }
      return success;
     
   }

   //---------------------------------------------------------------------

   /**
    * Destroy user credential cache file.
    */
   private boolean _credDestroy() 
   {
//       String loginFileLocation = this._loginReader.getLoginFileLocation();
//       
//       File file = new File(loginFileLocation);
//       if (file.exists()) {
//           file.delete();
//       }
       String svgp = (String) this._argTable.get(CMD.SERVERGROUP);
       boolean success = true;
       
       if (svgp != null)
       {
           boolean sgFound = false;
           String[] sgs = this._loginReader.getNamespaces();
           for (int i = 0; !sgFound && i < sgs.length; ++i)
           {
               if (svgp.equals(sgs[i]))
                   sgFound = true;
           }
           if (!sgFound)
           {
               this._logger.error(ERROR_TAG + "No credentials found for server group '"+svgp+"'");
               success = false;
           }
           else
           {               
               this._loginReader.remove(svgp);
               System.out.println("Removed entry for server group '"+svgp+"'");
               try {
                   _loginReader.commit();
               } catch (IOException ioEx) {
                   success = false;
               }
           }
       }
       else
       {
           try {
               _loginReader.delete();
           } catch (IOException ioEx) {
               success = false;
           }       
       }
       
       return success;
   }

   //---------------------------------------------------------------------

   /**
    * Display user credential cache file information (except password).
    */
   private boolean _credList() 
   {

      //File file = new File(loginFileLocation);
      File file = this._loginReader.getFile();
      String fileLocation = (file == null) ? null : file.getAbsolutePath();
       
      if (!file.canRead())
      {
          this._logger.info("No credentials exist.");
          //this._logger.severe("Login Error! Please acquire credentials "
          //        + "with login utility.");
          System.exit(-1);
      }
      
      String format1 = "EEE, d MMM yyyy HH:mm:ss Z";
      String format2 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
      DateFormat dateFormat = new SimpleDateFormat(format2);
      
      Date timeStamp = new Date(file.lastModified());
      this._logger.info(Constants.COPYRIGHT);
      this._logger.info(Constants.CLIENTVERSIONSTR);
      this._logger.info(Constants.APIVERSIONSTR);
      this._logger.info(""); // empty line
      this._logger.info("Credential cache file: " + fileLocation);
      this._logger.info("File modified on: " + timeStamp.toString());
      String defUser = this._loginReader.getUsername();
      if (defUser != null)
          this._logger.info("Default principal: " + defUser);
      else
          this._logger.info("No default principal specified.");
      
      String[] sgroups = this._loginReader.getNamespaces();
      for (int i = 0; i < sgroups.length; ++i)
      {
          String ns = sgroups[i];
          if (!ns.equals(LoginFile.DEFAULT_NAMESPACE))
          {
              String user = this._loginReader.getUsername(ns, false);
              long expiry = this._loginReader.getExpiry(ns);
              if (user != null)
              {
                  String line = "Principal for group '"+ns+"' : " + user;
                  if (expiry != Constants.NO_EXPIRATION)
                  {
                      String date = dateFormat.format(new Date(expiry));
                      if (expiry < System.currentTimeMillis())
                      {
                          line = line + " (expired "+date+")";   
                      }
                      else
                      {
                          line = line + " (expires "+date+")";                             
                      }
                  }
                  this._logger.info(line);
              }
          }
      }

      return true;
   }

//   private boolean _credListOld() 
//   {
//
//      //File file = new File(loginFileLocation);
//      File file = this._loginReader.getFile();
//      String fileLocation = (file == null) ? null : file.getAbsolutePath();
//       
//      if (!file.canRead())
//      {
//          this._logger.severe("Login Error! Please acquire credentials "
//                  + "with login utility.");
//          System.exit(-1);
//      }
//      
//      try {
//         this._getLoginInfo();
//      } catch (FileNotFoundException fnf) {
//         this._logger.severe("Login Error! Please acquire credentials "
//               + "with login utility.");
//         System.exit(-1);
//      } catch (IOException io) {
//         this._logger.severe("Login Error! Could not read login credentials.");
//         System.exit(-1);
//      }
//      Date timeStamp = new Date(file.lastModified());
//      this._logger.info(Constants.COPYRIGHT);
//      this._logger.info(Constants.CLIENTVERSIONSTR);
//      this._logger.info(Constants.APIVERSIONSTR);
//      this._logger.info(""); // empty line
//      this._logger.info("Credential cache file: " + fileLocation);
//      this._logger.info("File modified on: " + timeStamp.toString());
//      this._logger.info("Default principal: " + this._argTable.get(CMD.USER));
//
//      return true;
//   }
   
   //---------------------------------------------------------------------

   /**
    * Get user credential cache information, throw exception if error
    * 
    * @throws FileNotFoundException if credential cache file doesn't exist
    * @throws IOException if credential cache file is not readable
    */
   private void _getLoginInfo() throws FileNotFoundException, IOException 
   {
//
//      File file = new File(this._loginFile);
//      Properties props = new Properties();
//      FileIO.readConfiguration(props, file);
//      String user = props.getProperty(CMD.USER);
//      if (user != null) {
//         this._argTable.put(CMD.USER, user);
//      }
//
//      String password = props.getProperty(CMD.PASSWORD);
//      if (password != null) {
//         this._argTable.put(CMD.PASSWORD, password);
//      }
       
       //String user = this._loginReader.getUsername();
       //String pass = this._loginReader.getPassword();
       
       String serverGroup = this._getServerGroup(true);
       String user = this._loginReader.getUsername(serverGroup);
       String pass = this._loginReader.getPassword(serverGroup);
       
       if (user != null)
           this._argTable.put(CMD.USER, user);
       if (pass != null)
           this._argTable.put(CMD.PASSWORD, pass);
   }

   //---------------------------------------------------------------------

   /**
    * Util method to set up proper alignment in output
    * 
    * @param fmt number format
    * @param n the number
    * @param sp spacing
    * @return a properly aligned string
    */
   protected String _align(NumberFormat fmt, int n, int sp) {

      StringBuffer buf = new StringBuffer();
      FieldPosition fpos = new FieldPosition(NumberFormat.INTEGER_FIELD);
      fmt.format(n, buf, fpos);
      for (int i = 0; i < sp - fpos.getEndIndex(); ++i) {
         buf.insert(0, ' ');
      }
      return buf.toString();

   }

   //---------------------------------------------------------------------

   /**
    * Add Action
    * 
    * @return boolean true if operation is successful, false othewise
    * @throws SessionException when operation fails.
    */
   private boolean _add() throws SessionException {

       //prepare file list
      String[] tmpFileList = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);
      if (tmpFileList == null) {
         throw new SessionException("Invalid/missing file name expression.", -1);
      }
      ArrayList filesArray = new ArrayList(tmpFileList.length);

      try {
         for (int i = 0; i < tmpFileList.length; ++i) {
            if (tmpFileList[i].indexOf('*') == -1) {
               filesArray.add(tmpFileList[i]);
            } else {
               String nameExp = tmpFileList[i].replaceAll("\\*", ".*");

               //user dir may be a "junction point", which is a dir
               //the returns null from listing request.  So now check
               //if dir and returned array is not null
               String userDirStr = System.getProperty("user.dir");
               File userDirFile = new File(userDirStr);
               String[] dirList = null;
               if (userDirFile.isDirectory())
                   dirList = userDirFile.list();
   
               if (dirList != null)
               {
                   for (int j = 0; j < dirList.length; ++j) {
                      if (dirList[j].matches(nameExp)) {
                         filesArray.add(dirList[j]);
                      }
                   }
               }
            }
         }
      } catch (PatternSyntaxException e) {
         throw new SessionException(e.getMessage(), -1);
      }

      filesArray.trimToSize();
      String[] files = new String[filesArray.size()];
      for (int i = 0; i < filesArray.size(); ++i) {
         files[i] = (String) filesArray.get(i);
      }
      
      //---------------------------
      
      if (files != null && files.length > 0) {
         if (this._argTable.get(CMD.AUTODELETE) != null) {
            this._client.set(Client.OPTION_AUTODELETE, true);
         }
         if (this._argTable.get(CMD.CRC) != null) {
            this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
         }
         if (this._argTable.get(CMD.RECEIPT) != null) {
             this._client.set(Client.OPTION_RECEIPT, true);
         }
         String comment = (String) this._argTable.get(CMD.COMMENT);
         String before = (String) this._argTable.get(CMD.BEFORE);
         String after = (String) this._argTable.get(CMD.AFTER);
         String between = (String) this._argTable.get(CMD.BETWEEN);
         String and = (String) this._argTable.get(CMD.AND);
         String format = (String) this._argTable.get(CMD.FORMAT);
         
         this._dateFormatter = new DateTimeFormatter(format);
         
         int transId;

         try {
            if (before != null) {
               files = this._filesBefore(this._dateFormatter.parseDate(before),
                                         files);
            } else if (after != null) {
               files = this._filesAfter(this._dateFormatter.parseDate(after),
                                        files);
            } else if (between != null && and != null) {
               files = this._filesBetween(this._dateFormatter.parseDate(between),
                                          this._dateFormatter.parseDate(and), 
                                          files);
            }
         } catch (ParseException e) {
            throw new SessionException(e.getMessage(), -1);
         }

         if (files == null || files.length < 1) {
            this._logger.info("No file to be added");
            return true;
         } else
            transId = this._client.add(files, comment);

         boolean success = true;
         NumberFormat fmt = NumberFormat.getInstance();
         String currentType = "";
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            
            if (result.getErrno() != Constants.OK) {
                
               this._logger.error(ERROR_TAG + result.getMessage());
               ++this._errorCount;
               success = false;
               this._triggerFileResultError(Constants.ADDFILE, result); 
               continue;
            }
            
            if (!currentType.equals(result.getType()) && this._using) {
               currentType = result.getType();
               this._logger.info("\nAdding to file type \"" + currentType
                     + "\":");
            }
            String msg = this._align(fmt, ++this._resultIdx, 6) +
                         ". Added \"" + result.getName() + "\".";
            if (result.getChecksumStr() != null)
                msg += "  Checksum: " + result.getChecksumStr() + ".";
            if (result.getReceiptId() != Constants.NOT_SET)
                msg += "  Receipt Id: " + result.getReceiptId() + ".";            
            this._logger.info(msg);
            
            //trigger handler event 
            this._triggerFileResultEvent(Constants.ADDFILE, result);
            
            
            //this._logger.info(this._align(fmt, ++this._resultIdx, 6)
            //      + ". Added \"" + result.getName() + "\".");
            //if (result.getErrno() != Constants.OK) {                                           
            //    success = false;
            //}
            
         }
         return success;
      }
      return false;
   }

   //---------------------------------------------------------------------

   /**
    * Get a list of files modified before a date
    * 
    * @param before the before date
    * @param files Vector containing file names
    * @return String array of files that where modified before a date
    */
   private String[] _filesBefore(Date before, String[] files) {

      ArrayList list = new ArrayList(files.length);

      for (int i = 0; i < files.length; ++i) {
         File f = new File(files[i]);
         Date date = new Date(f.lastModified());
         if (date.before(before)) {
            list.add(files[i]);
         }
      }
      list.trimToSize();

      if (list.size() == 0)
         return null;

      String[] newlist = new String[0];
      newlist = (String[]) list.toArray(newlist); 

      return newlist;
   }

   //---------------------------------------------------------------------

   /**
    * Get a list of files modified after a date
    * 
    * @param after the after date
    * @param files Vector containing file names
    * @return String array of files that where modified after a date
    */
   private String[] _filesAfter(Date after, String[] files) {

      ArrayList list = new ArrayList(files.length);

      for (int i = 0; i < files.length; ++i) {
         File f = new File(files[i]);
         Date date = new Date(f.lastModified());
         if (date.after(after)) {
            list.add(files[i]);
         }
      }
      list.trimToSize();

      if (list.size() == 0)
         return null;

      String[] newlist = new String[0];
      newlist = (String[]) list.toArray(newlist); 

      return newlist;
   }

   //---------------------------------------------------------------------

   /**
    * Get a list of files modified between two dates
    * 
    * @param begin begin date
    * @param end end date
    * @param files Vector containing file names
    * @return String array of files that where modified between beginning and
    *         ending dates
    */
   private String[] _filesBetween(Date begin, Date end, String[] files) {

      ArrayList list = new ArrayList(files.length);

      for (int i = 0; i < files.length; ++i) {
         File f = new File(files[i]);
         Date date = new Date(f.lastModified());
         if (date.before(end) && date.after(begin)) {
            list.add(files[i]);
         }
      }
      list.trimToSize();

      if (list.size() == 0)
         return null;

      String[] newlist = new String[0];
      newlist = (String[]) list.toArray(newlist); 

      return newlist;
   }

   //---------------------------------------------------------------------

   /**
    * Get Action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails
    */
   private boolean _get() throws SessionException 
   {

      String[] files = (String[]) this._argTable.get(UtilCmdParser.KEYWORD_FILES);
      
      if (files == null || files.length == 0)
         files = new String[] { "*" };

      try {
         String outputDir = (String) this._argTable.get(CMD.OUTPUT);
         if (outputDir == null)
             outputDir = System.getProperty("user.dir");
         this._client.changeDir(outputDir);
         

         Object replace = this._argTable.get(CMD.REPLACE);
         Object version = this._argTable.get(CMD.VERSION);

         if (replace != null && version != null)
         {
            if (!this._using) 
            {
               this._logger.info(this._getUsage());
            }
            ++this._errorCount;
            return false;
         }

         if (replace != null) 
         {
            this._client.set(Client.OPTION_REPLACEFILE, true);
         }

         if (version != null) 
         {
            this._client.set(Client.OPTION_VERSIONFILE, true);
         }

         if (this._argTable.get(CMD.CRC) != null) 
         {
            this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
            this._client.set(Client.OPTION_RESTART, true);
            this._logger.info("File resume transfer enabled");
         }

         if (this._argTable.get(CMD.SAFEREAD) != null) 
         {
            this._client.set(Client.OPTION_SAFEREAD, true);
         }

         if (this._argTable.get(CMD.RECEIPT) != null) 
         {
            this._client.set(Client.OPTION_RECEIPT, true);
         }

         if (this._argTable.get(CMD.REPLICATE) != null) 
         {
             this._client.set(Client.OPTION_REPLICATE, true);
         }

         if (this._argTable.get(CMD.REPLICATEROOT) != null)
         {
             String rootStr = (String) this._argTable.get(CMD.REPLICATEROOT);
             try {             
                 this._client.setReplicationRoot(rootStr);
             } catch (SessionException sesEx) {
                 this._logger.error(ERROR_TAG + sesEx.getMessage());
                 ++this._errorCount;
                 return false;
             }
         }
         
         if (this._argTable.get(CMD.DIFF) != null) {
             this._client.set(Client.OPTION_DIFF, true);
         }
       
         String before  = (String) this._argTable.get(CMD.BEFORE);
         String after   = (String) this._argTable.get(CMD.AFTER);
         String between = (String) this._argTable.get(CMD.BETWEEN);
         String and     = (String) this._argTable.get(CMD.AND);
         String format  = (String) this._argTable.get(CMD.FORMAT);

         //------------------------
         
         //get the invoke command string
         String invoke = (String) this._argTable.get(CMD.INVOKE);
         if (invoke != null) 
         {
            invoke.trim();
            if (invoke.length() == 0)
               invoke = null;             
         }

         //set exit on error flag
         boolean exitOnError = false;
         if (invoke != null && this._argTable.get(CMD.INVOKEEXITONERROR) != null)
            exitOnError = true;   
         
         //set invoke async flag
         boolean invokeAsync = false;
         if (invoke != null && this._argTable.get(CMD.INVOKEASYNC) != null)
             invokeAsync = true;
         
         //------------------------
         
         this._dateFormatter = new DateTimeFormatter(format);
         
         int transId;

         if (before != null) 
         {
            transId = this._client.getBefore(
                            this._dateFormatter.parseDate(before), 
                            files[0]);
         } 
         else if (after != null)
         {
            transId = this._client.getAfter(
                            this._dateFormatter.parseDate(after),
                            files[0]);
         } 
         else if (between != null && and != null) 
         {
            transId = this._client.getBetween(
                            this._dateFormatter.parseDate(between), 
                            this._dateFormatter.parseDate(and),
                            files[0]);
         } 
         else 
         {
            if (files[0].indexOf("*") != -1)
               transId = this._client.get(files[0]);
            else
               transId = this._client.get(files);
         }
         
         
         boolean success = true;
         NumberFormat fmt = NumberFormat.getInstance();
         String currentType = "";
         
         while (this._client.getTransactionCount() > 0) 
         {
            Result result = this._client.getResult();
            if (result == null) 
            {
               continue;
            }
            
            if (result.getErrno() == Constants.NO_FILES_MATCH) 
            {
               this._logger.info(result.getMessage());
               continue;
            } 
            else if (result.getErrno() == Constants.FILE_EXISTS) 
            {
               this._logger.error(ERROR_TAG + result.getMessage());
               ++this._errorCount;
               success = false;
               
               //invoke error handlers
               this._triggerFileResultError(Constants.GETFILES, result); 
               
               if (this._argTable.get(CMD.CRC) != null)
                  result.commit();
               continue;
            } 
            else if (result.getErrno() != Constants.OK) 
            {
               this._logger.error(ERROR_TAG + result.getMessage());
               ++this._errorCount;
               success = false;
               
               //invoke error handlers
               this._triggerFileResultError(Constants.GETFILES, result); 
               
               continue;
            }
            
            if (!currentType.equals(result.getType()) && this._using) 
            {
               currentType = result.getType();
               this._logger.info("\nGetting from file type \"" + currentType
                     + "\":");
            }
            
            String msg = this._align(fmt, ++this._resultIdx, 6) + ". Got \""
                                     + result.getName() + "\".";

            if (result.getChecksumStr() != null)
               msg += ("  CRC: " + result.getChecksumStr()+".");

            if (result.getReceiptId() != Constants.NOT_SET)
                msg += ("  Receipt Id: " + result.getReceiptId()+".");
            
            //if replication was enabled, print the location
            if ((this._argTable.get(CMD.REPLICATE) != null) &&
                 result.getLocalLocation() != null)
                msg += ("  Location: " + result.getLocalLocation()+".");
            
            this._logger.info(msg);
            
            // if invoke is supported, this is the place to check and
            // execute it. If it fails, then it should commit the
            // restart cache info.
            if (invoke != null)
            {
                boolean proceed = _performInvocation(result, outputDir,  
                                                     invoke, exitOnError, 
                                                     invokeAsync,
                                                     Constants.GETFILES);
                if (!proceed)
                {
                    success = false;
                    break;
                }
            }
            
            //invoke event handlers
            this._triggerFileResultEvent(Constants.GETFILES, result);
            
            if (this._argTable.get(CMD.CRC) != null)
               result.commit();
         }
                  
         return success;
         
      } catch (ParseException e) {
         throw new SessionException(e.getMessage(), -1);
      }
   }

   //---------------------------------------------------------------------

   /**
    * Application method to read as list of file names from standard-input and
    * issues add|replace|get|delete request on each file.
    * 
    * @return true if success
    * @throws SessionException
    */
   private boolean _accept() throws SessionException {

      String[] files = (String[]) this._argTable
                       .get(UtilCmdParser.KEYWORD_FILES);
      if (files == null) {
          this._logger.error(ERROR_TAG+"no files specified.");
          ++this._errorCount;
          return false;
      }
      else if (files.length == 0){
         this._logger.info("No files found.");
         //++this._errorCount;
         return false;
      }

      String op = (String) this._argTable.get(CMD.FOR);
      if (op.equals("add"))
         return this._add();
      else if (op.equals("replace"))
         return this._replace();
      else if (op.equals("get"))
         return this._get();
      else if (op.equals("delete") || op.equals("remove"))
         return this._delete();
      else {
         this._logger.info(this._getUsage());
         return false;
      }
   }

   //---------------------------------------------------------------------

   private Client _createAutoQueryClient() {

      Client client = null;
      while (client == null) {
          
          //check with throttler if we need to hold our
          //horses before creating a new client..
          
          if (_throttle != null)
          {
              _throttle.pollWait();              
          }
          
         try {
             
            client = new Client(this._domainFile);
            client.login((String) this._argTable.get(CMD.USER),
                  (String) this._argTable.get(CMD.PASSWORD),
                  (String) this._argTable.get(CMD.SERVERGROUP),
                  (String) this._argTable.get(CMD.FILETYPE));
            client.set("restart", true);

         } catch (SessionException e) {
            String msg = "FEI5 Information on "
                  + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                  + ERROR_TAG + "Unable to restart session to ["
                  + this._argTable.get(CMD.SERVERGROUP) + ":"
                  + this._argTable.get(CMD.FILETYPE) + "].  Throttling next attempt.\n";
            
            if (this._mailmessage)
               this._emailMessageLogger.error(msg);
            this._logger.error(msg);
            this._logger.debug(e);
            client = null;
         }

       
      }
      return client;
   }

   private Client _createAutoQueryClientOriginal() {
              
      long minSleepTime = 60 * 1000;
      long maxSleepTime = minSleepTime * 16;
      long sleepTime = minSleepTime;

      Client client = null;
      while (client == null) {
         try {
             
            client = new Client(this._domainFile);
            client.login((String) this._argTable.get(CMD.USER),
                  (String) this._argTable.get(CMD.PASSWORD),
                  (String) this._argTable.get(CMD.SERVERGROUP),
                  (String) this._argTable.get(CMD.FILETYPE));
            client.set("restart", true);

         } catch (SessionException e) {
            String msg = "FEI5 Information on "
                  + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                  + ERROR_TAG + "Unable to restart session to ["
                  + this._argTable.get(CMD.SERVERGROUP) + ":"
                  + this._argTable.get(CMD.FILETYPE) + "].  Next attempt in "
                  + (sleepTime / _MINUTE_MS) + " minute(s).\n";
            if (this._mailmessage)
               this._emailMessageLogger.error(msg);
            this._logger.error(msg);
            this._logger.debug(e);
            client = null;
         }

         if (client == null) {
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
    * Convenience method that prepares loggers, email settings,
    * and timers for external notifications.
    * @return True if loggers were successfully initialized,
    *         false otherwise.
    */
   
   private boolean _initSessionLogging(String messageType, String reportType)
   {
       String titleRoot = "Re: FEI5 [" + FileType.toFullFiletype(
                            (String) this._argTable.get(CMD.SERVERGROUP), 
                            (String) this._argTable.get(CMD.FILETYPE)) + "] ";
       String messageTitle = titleRoot + messageType;
       String reportTitle  = titleRoot + reportType;
       
       //--------------------------
       
       //prepare log file
       String logfile = (String) this._argTable.get(CMD.LOGFILE);
       if (logfile != null) 
       {
          String rolling = (String) this._argTable.get(CMD.LOGFILEROLLING);
          if (rolling != null) 
          {
             if (rolling.equalsIgnoreCase("DAILY")) {
                this._logger.enableLogToFile(logfile, Logger.ROLLING_DAILY);
             } else if (rolling.equalsIgnoreCase("WEEKLY")) {
                this._logger.enableLogToFile(logfile, Logger.ROLLING_WEEKLY);
             } else if (rolling.equalsIgnoreCase("MONTHLY")) {
                this._logger.enableLogToFile(logfile, Logger.ROLLING_MONTHLY);
             } else if (rolling.equalsIgnoreCase("HOURLY")) {
                this._logger.enableLogToFile(logfile, Logger.ROLLING_HOURLY);
             } else if (rolling.equalsIgnoreCase("MINUTELY")) {
                this._logger.enableLogToFile(logfile, Logger.ROLLING_MINUTELY);
             } else if (rolling.equalsIgnoreCase("HALFDAILY")) {
                this._logger.enableLogToFile(logfile, Logger.ROLLING_HALF_DAILY);
             } else {
                this._logger.enableLogToFile(logfile);
             }
          } else
             this._logger.enableLogToFile(logfile);
                    
       }
       
       //--------------------------
       
       //retrieve mail settings from parser
       this._mailFrom        = (String)  this._argTable.get(CMD.MAILMESSAGEFROM);
       this._mailTo          = (String)  this._argTable.get(CMD.MAILMESSAGETO);
       this._smtpHost        = (String)  this._argTable.get(CMD.MAILSMTPHOST);
       this._mailReportTo    = (String)  this._argTable.get(CMD.MAILREPORTTO);
       this._mailReportAt    = (String)  this._argTable.get(CMD.MAILREPORTAT);
       this._mailSilentRecon = this._argTable.containsKey(CMD.MAILSILENTRECONN) ?
                               ((Boolean) this._argTable.get(CMD.MAILSILENTRECONN)).
                               booleanValue() : false;
                               
                               

                               
       //--------------------------
       
       //check if we are sending messages
       if (this._mailFrom != null && this._mailTo != null &&
           this._smtpHost != null) 
       {
          this._mailmessage = true;
          
          this._emailMessageLogger.setMail(this._mailFrom, this._mailTo,
                this._smtpHost, messageTitle);
          this._emailMessageLogger.enableSendMail();   
         
       }

       //check that mail-message settings are complete
       if (!this._mailmessage) 
       {
          if (this._mailTo != null && 
              (this._mailFrom == null || this._smtpHost == null)) 
          {
             this._logger.error(ERROR_TAG
                   + "Incomplete mailMessageTo setting.  "
                   + "Values for 'mailMessageFrom', 'mailMessageTo', "
                   + "and 'mailSMTPHost' must be sepcified.");
             ++this._errorCount;
             return false;
          }
       }
       else
       {
           _throttle.addLogger(_emailMessageLogger);
       }
       

       //--------------------------
       
       // create mail report scheduler
       this._reportScheduler = new Timer();
       
       //check if we are sending reports
       if (this._mailFrom != null && this._mailReportTo != null &&
           this._smtpHost != null && this._mailReportAt != null) 
       {
          this._mailreport = true;
          this._emailReportLogger.setMail(this._mailFrom, this._mailReportTo,
                                          this._smtpHost, reportTitle);
          this._emailReportLogger.enableSendMail();

          Date[] dateList = UtilCmdParser.parseTimeList(this._mailReportAt);
          if (dateList == null || dateList.length == 0) 
          {
             this._logger.error(ERROR_TAG + "Invalid time formate for \"" +
                                CMD.MAILREPORTAT + "\".");
             this._logger.error("Ignore report mailing request.  Please use " +
                                "\"hh:mm am|pm, hh:mm am|pm, ...\"");
             this._reportScheduler.cancel();
             this._mailreport = false;
          } 
          else 
          {
             for (int i = 0; i < dateList.length; ++i) 
             {
                this._logger.debug("scheduling task for " + dateList[i]);
                _reportScheduler.scheduleAtFixedRate(new ScheduledMailer(),
                                                     dateList[i], _DAY_MS);
             }
          }
       }

       //check that mail-report settings are complete
       if (!this._mailreport) 
       {
          if (this._mailReportTo != null && 
             (this._mailFrom == null || this._mailReportAt == null ||
              this._smtpHost == null)) 
          {
             this._logger.error(ERROR_TAG + "Incomplete mailReportTo " +
                    "setting.  Values for 'mailMessageFrom', " +
                    "'mailReportTo', 'mailReportAt', and 'mailSMTPHost' " +
                    "must be sepcified.");
             ++this._errorCount;
             return false;
          }
       }
       
       //--------------------------
       
       //all is good, send true to signal no errors
       return true;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Dispatches subscription based on user parameters.  Current options
    * for subscription sessions are pulling and pushing, or the pull-query.
    * @return true iff success.
    * @throws SessionException
    */
   
   private boolean _autoGet() throws SessionException
   {
       if (this._argTable.get(CMD.PUSH) != null)
           return this._autoGetPush();
       else if (this._argTable.get(CMD.QUERY) != null)
           return this._autoGetQuery();
       else          
           return this._autoGetPull();
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Dispatches subscription based on user parameters.  Current options
    * for subscription sessions are pulling and pushing.
    * @return true iff success.
    * @throws SessionException
    */
   
   private boolean _autoShow() throws SessionException
   {
       if (this._argTable.get(CMD.PUSH) != null)
           return this._autoShowPush();
       else if (this._argTable.get(CMD.QUERY) != null)
           return this._autoShowQuery();
       else          
           return this._autoShowPull();
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method implement the file auto get operation. The method supports continue
    * auto get file with restart capability. It also supports automatic
    * reconnection after server reboot.
    * 
    * @return true if success.
    * @throws SessionException
    */
   
   private boolean _autoGetPull() throws SessionException {
               
      long queryInterval = this._queryInterval == null ? _MINUTE_MS : 
                            Long.parseLong(this._queryInterval) *
                            _MINUTE_MS;

      String ftString = FileType.toFullFiletype(
              (String) this._argTable.get(CMD.SERVERGROUP),
              (String) this._argTable.get(CMD.FILETYPE));
      
      long sleeptime = queryInterval;

      //---------------------------
      
      boolean loggerInitError = !_initSessionLogging(
                                                "Subscription Notification",
                                                "Subscription Report");
      if (loggerInitError)   
          return false;
      
      //---------------------------
      
      //set output dir
      String outputDir = (String) this._argTable.get(CMD.OUTPUT);
      if (outputDir == null)
         outputDir = System.getProperty("user.dir");
      this._client.changeDir(outputDir);

      //---------------------------
      
      //get settings from parser
      Object replace = this._argTable.get(CMD.REPLACE);
      Object version = this._argTable.get(CMD.VERSION);

      //check consistent state
      if (replace != null && version != null) {
         if (!this._using) {
            this._logger.info(this._getUsage());
         }
         ++this._errorCount;
         return false;
      }

      //set client according to parameters
      if (replace != null)
         this._client.set(Client.OPTION_REPLACEFILE, true);
      if (version != null)
         this._client.set(Client.OPTION_VERSIONFILE, true);
      if (this._argTable.get(CMD.SAFEREAD) != null)
         this._client.set(Client.OPTION_SAFEREAD, true);
      if (this._argTable.get(CMD.RECEIPT) != null)
         this._client.set(Client.OPTION_RECEIPT, true);

      //---------------------------
      
      //check CRC, enable if set
      if (this._argTable.get(CMD.CRC) != null) {
         this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
         this._logger.info("File resume transfer enabled.\n");
      }

      //get the invoke command string
      String invoke = (String) this._argTable.get(CMD.INVOKE);
      if (invoke != null) {
         invoke.trim();
         if (invoke.length() == 0)
            invoke = null;
      }

      //set exit on error flag
      boolean exitOnError = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEEXITONERROR) != null)
         exitOnError = true;

      //set invoke async flag
      boolean invokeAsync = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEASYNC) != null)
          invokeAsync = true;
           
      //---------------------------
      
      //check format option, and build new formatter
      String format = (String) this._argTable.get(CMD.FORMAT);
      this._dateFormatter = new DateTimeFormatter(format);      
      
      //---------------------------
      
      //check replicate, enable if set
      if (this._argTable.get(CMD.REPLICATE) != null) {
         this._client.set(Client.OPTION_REPLICATE, true);
         this._logger.info("File replication enabled.\n");
      }
         
      if (this._argTable.get(CMD.REPLICATEROOT) != null) {
          String rootStr = (String) this._argTable.get(CMD.REPLICATEROOT);
                
          try {             
              this._client.setReplicationRoot(rootStr);
          } catch (SessionException sesEx) {
              this._logger.error(ERROR_TAG + sesEx.getMessage());
              ++this._errorCount;
              return false;
          }
      }
      
      //---------------------------
      
      //check diff
      if (this._argTable.get(CMD.DIFF) != null) {
          this._client.set(Client.OPTION_DIFF, true);
      }

      //---------------------------
      
      /*
       * Since we are relying on client to automatic query, restart (in the
       * client API) should be always enabled. The 'restart' command option
       * translates to restart from the latest queried time. If the user did not
       * specify this option, then it should use the default restart time, that
       * is the current time, and persist the time for each file received.
       */
      Date queryTime = null;
      if (this._argTable.get(CMD.RESTART) == null) {
         queryTime = new Date();
      }
      this._client.set(Client.OPTION_RESTART, true);

      String msg = "FEI5 Information on "
            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
            + "Subscribing to [" + ftString + "] file type.\n";

      if (this._mailmessage)
         this._emailMessageLogger.info(msg);
      this._logger.info(msg);

      boolean success = true;
      boolean newconnection = false;

      //enter loop
      while (true) {
         long issueTime = System.currentTimeMillis();
         int tranId = this._client.getAfter(queryTime);

         newconnection = false;

         // reset the epoch to null to trigger client API to
         // use the last queried time.
         queryTime = null;

         boolean shouldExit = false;

         //handle resulting files
         while (this._client.getTransactionCount() > 0) {
             
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            } else if (result.getErrno() == Constants.NO_FILES_MATCH) {
               // no new files at this time
               continue;
            } else if (result.getErrno() == Constants.OK) {
                
                boolean proceed =  _handleNewFile(result, outputDir,  
                                                  invoke, exitOnError, 
                                                  invokeAsync,
                                                  Constants.AUTOGETFILES);
                if (!proceed)
                {
                    shouldExit = true;
                    break;
                }  
                
                //invoke event handlers
                this._triggerFileResultEvent(Constants.AUTOGETFILES, result);
                
               result.commit();               
               continue;
            } else if (result.getErrno() == Constants.FILE_EXISTS) {
               this._logger.info(result.getMessage());
               
               this._triggerFileResultError(Constants.AUTOGETFILES, result);
               
               result.commit();
               continue;
            } else if (result.getErrno() == Constants.IO_ERROR) {
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                     + ERROR_TAG + "Lost connection to ["
                     + ftString + "].  Attempting restart\n";
               if (this._mailmessage && !this._mailSilentRecon)
                  this._emailMessageLogger.error(msg);
               this._logger.error(msg);
               
               //invoke error handlers
               this._triggerFileResultError(Constants.AUTOGETFILES, result);
               
               this._client.logout();
               
               
               this._client = this._createAutoQueryClient();
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString()
                     + "\nRestored subscription session to ["
                     + ftString + "].\n";
               if (this._mailmessage && !this._mailSilentRecon)
                  this._emailMessageLogger.info(msg);
               this._logger.info(msg);

               if (outputDir != null)
                  this._client.changeDir(outputDir);
               if (replace != null)
                  this._client.set(Client.OPTION_REPLACEFILE, true);
               if (version != null)
                  this._client.set(Client.OPTION_VERSIONFILE, true);
               if (this._argTable.get(CMD.CRC) != null)
                  this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
               if (this._argTable.get(CMD.SAFEREAD) != null)
                  this._client.set(Client.OPTION_SAFEREAD, true);
               
               //fix as part of AR118105 -------------------
               //check replicate, enable if set
               if (this._argTable.get(CMD.REPLICATE) != null) 
                  this._client.set(Client.OPTION_REPLICATE, true);               
               if (this._argTable.get(CMD.REPLICATEROOT) != null) 
               {
                   String rootStr = (String) this._argTable.get(CMD.REPLICATEROOT);                         
                   try {             
                       this._client.setReplicationRoot(rootStr);
                   } catch (SessionException sesEx) {
                       this._logger.error(ERROR_TAG + sesEx.getMessage());
                   }
               }       
               
               //check diff
               if (this._argTable.get(CMD.DIFF) != null) {
                   this._client.set(Client.OPTION_DIFF, true);
               }
               //end of fix --------------------------------------
               
               newconnection = true;
               break;
            } else {
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                     + ERROR_TAG + result.getMessage() + "\n";
               if (this._mailmessage)
                  this._emailMessageLogger.error(msg);
               this._logger.error(msg);
               this._logger.debug("ERRNO = " + result.getErrno());
               this._triggerFileResultError(Constants.AUTOGETFILES, result);
               continue;
            }
         }

         if (shouldExit)
            break;

         if (!newconnection) {
            long processTime = System.currentTimeMillis() - issueTime;
            sleeptime = processTime > queryInterval ? 0 : queryInterval
                  - processTime;
            this._logger.debug("sleep time = " + sleeptime);
            try {
               Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
               // exist the infinite loop and return
               break;
            }
         }
      }

      return success;
   }

   //---------------------------------------------------------------------

   /**
    * Method implement the file auto get operation. The method supports continue
    * auto get file with restart capability. It also supports automatic
    * reconnection after server reboot.
    * 
    * @return true if success.
    * @throws SessionException
    */
   private boolean _autoGetPush() throws SessionException {
      
      long sleeptime = 1000; //1 second
      String ftString = FileType.toFullFiletype(
                                 (String) this._argTable.get(CMD.SERVERGROUP),
                                 (String) this._argTable.get(CMD.FILETYPE));
      
      //---------------------------
      
      boolean loggerInitError = !_initSessionLogging(
                                          "Subscription Notification",
                                          "Subscription Report");
      if (loggerInitError)       
          return false;
      
      //---------------------------
      
      //set output dir
      String outputDir = (String) this._argTable.get(CMD.OUTPUT);
      if (outputDir == null)
         outputDir = System.getProperty("user.dir");
      this._client.changeDir(outputDir);

      //get settings from parser
      Object replace = this._argTable.get(CMD.REPLACE);
      Object version = this._argTable.get(CMD.VERSION);

      //check consistent state
      if (replace != null && version != null) {
         if (!this._using) {
            this._logger.info(this._getUsage());
         }
         ++this._errorCount;
         return false;
      }

      //set client according to parameters
      if (replace != null)
         this._client.set(Client.OPTION_REPLACEFILE, true);
      if (version != null)
         this._client.set(Client.OPTION_VERSIONFILE, true);
      if (this._argTable.get(CMD.SAFEREAD) != null)
         this._client.set(Client.OPTION_SAFEREAD, true);
      if (this._argTable.get(CMD.RECEIPT) != null)
         this._client.set(Client.OPTION_RECEIPT, true);
      if (this._argTable.get(CMD.CRC) != null) {
         this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
         this._logger.info("File resume transfer enabled.\n");
      }

      //get the invoke command string
      String invoke = (String) this._argTable.get(CMD.INVOKE);
      if (invoke != null) {
         invoke.trim();
         if (invoke.length() == 0)
            invoke = null;
      }

      //set exit on error flag
      boolean exitOnError = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEEXITONERROR) != null)
         exitOnError = true;

      //set invoke async flag
      boolean invokeAsync = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEASYNC) != null)
          invokeAsync = true;
      
      //---------------------------
          
      this._client.set(Client.OPTION_RESTART, true);      
      
      //---------------------------
      
      //check format option, and build new formatter
      String format = (String) this._argTable.get(CMD.FORMAT);
      this._dateFormatter = new DateTimeFormatter(format);      

      //---------------------------
      
      //check replicate, enable if set
      if (this._argTable.get(CMD.REPLICATE) != null) {
         this._client.set(Client.OPTION_REPLICATE, true);
         this._logger.info("File replication enabled.\n");
      }
      
      if (this._argTable.get(CMD.REPLICATEROOT) != null) {
          String rootStr = (String) this._argTable.get(CMD.REPLICATEROOT);
          try {             
              this._client.setReplicationRoot(rootStr);
          } catch (SessionException sesEx) {
              this._logger.error(ERROR_TAG + sesEx.getMessage());
              ++this._errorCount;
              return false;
          }
      }

      //---------------------------
      
      //check diff, enable if set
      if (this._argTable.get(CMD.DIFF) != null) {
         this._client.set(Client.OPTION_DIFF, true);
      }
      
      //---------------------------
      
      String msg = "FEI5 Information on "
            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
            + "Subscribing to [" + ftString + "] file type.\n";

      if (this._mailmessage)
         this._emailMessageLogger.info(msg);
      this._logger.info(msg);

      //---------------------------
      
      boolean success = true;
      boolean newconnection = false;
      boolean timeToExit = false;

      //construct a subscription client that will generate new file events
      //final List newFiles = new Vector();   
      final PushFileEventQueue<String> newFileQueue = 
                            new PushFileEventQueue<String>(); 
      final Map clientOptions = (Map) this._argTable.clone();      
      if (!clientOptions.containsKey(CMD.OUTPUT))
          clientOptions.put(CMD.OUTPUT, ".");    
      
      try {
          _subscriptionClient = new PushSubscriptionClient(
                                    this._domainFile, clientOptions, 
                                    Constants.AUTOGETFILES); 
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
                  }
              }
          }          
      };
      
      //add anon. listener to client
      _subscriptionClient.addSubscriptionEventListener(subListener);
      
      //---------------------------
      
      //launch subscription client on own thread
      Thread subThread = new Thread(_subscriptionClient);
      subThread.setName("Subscription_Thread_"+ftString);
      subThread.start();
      
      //---------------------------
      
      //enter loop
      while (_subscriptionClient.isAlive()) {
         long issueTime = System.currentTimeMillis();
         
         //---------------------------
         
         //lock newFiles collection, create array of filenames from
         //contents. 
         String[] files = null;
         synchronized(newFileQueue)
         {
             newFileQueue.advanceQueue();
             List<String> filenameList = newFileQueue.getItemsInProcess();
             files = filenameList.toArray(new String[0]);
             
//             int numFiles = newFiles.size();
//             if (numFiles > 0)
//             {
//                 files = new String[numFiles];
//                 for (int i = 0; i < numFiles; ++i)
//                 {
//                     
//                     files[i] = (String) newFiles.get(i);
//                 }
//             }
         }
         
         // call 'get' using the filename array.
         if (files != null && files.length > 0)
         {
             this._client.get(files);
         }
       
         //---------------------------

         // reset the epoch to null to trigger client API to
         // use the last queried time.
         //queryTime = null;
         timeToExit = false;
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
                boolean proceed =  _handleNewFile(result, outputDir,  
                                                  invoke, exitOnError,  
                                                  invokeAsync,
                                                  Constants.AUTOGETFILES);
                if (!proceed)
                {
                    timeToExit = true;
                    break;
                }
                
                //invoke event handlers
                this._triggerFileResultEvent(Constants.AUTOGETFILES, result);
                
               result.commit();
               //newFiles.remove(result.getName());  //synch'ed Vector
               newFileQueue.removeItem(result.getName());
               continue;
            } 
            else if (result.getErrno() == Constants.FILE_EXISTS) 
            {
               this._logger.info(result.getMessage());
               this._triggerFileResultError(Constants.AUTOGETFILES, result);
               
               result.commit();
               //newFiles.remove(result.getName());  //synch'ed Vector
               newFileQueue.removeItem(result.getName());
               continue;
            } 
            else if (result.getErrno() == Constants.IO_ERROR) 
            {
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                     + ERROR_TAG + "Lost connection to ["
                     + ftString + "].  Attempting restart\n";
               if (this._mailmessage && !this._mailSilentRecon)
                  this._emailMessageLogger.error(msg);
               this._logger.error(msg);

               //invoke error handlers
               this._triggerFileResultError(Constants.AUTOGETFILES, result);
               
               this._client.logout();
               
               this._client = this._createAutoQueryClient();
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString()
                     + "\nRestored subscription session to ["
                     + ftString + "].\n";
               if (this._mailmessage && !this._mailSilentRecon)
                  this._emailMessageLogger.info(msg);
               this._logger.info(msg);

               if (outputDir != null)
                  this._client.changeDir(outputDir);
               if (replace != null)
                  this._client.set(Client.OPTION_REPLACEFILE, true);
               if (version != null)
                  this._client.set(Client.OPTION_VERSIONFILE, true);
               if (this._argTable.get(CMD.CRC) != null)
                  this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
               if (this._argTable.get(CMD.SAFEREAD) != null)
                  this._client.set(Client.OPTION_SAFEREAD, true);
               
               //fix as part of AR118105 -------------------
               //check replicate, enable if set
               if (this._argTable.get(CMD.REPLICATE) != null) 
                  this._client.set(Client.OPTION_REPLICATE, true);               
               if (this._argTable.get(CMD.REPLICATEROOT) != null) 
               {
                   String rootStr = (String) this._argTable.get(CMD.REPLICATEROOT);                         
                   try {             
                       this._client.setReplicationRoot(rootStr);
                   } catch (SessionException sesEx) {
                       this._logger.error(ERROR_TAG + sesEx.getMessage());
                   }
               }            
               
               //check diff
               if (this._argTable.get(CMD.DIFF) != null) {
                   this._client.set(Client.OPTION_DIFF, true);
               }
               //end of fix --------------------------------------
               
               
               newconnection = true;
               break;
            } 
            else if (result.getErrno() == Constants.FILE_NOT_FOUND)
            {
                msg = "FEI5 Information on "
                    + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                    + ERROR_TAG + result.getMessage() + 
                    ", file not found on server.  Skipping file." + "\n";
                if (this._mailmessage)
                    this._emailMessageLogger.error(msg);
                this._logger.error(msg);
                
                this._triggerFileResultError(Constants.AUTOGETFILES, result);
                
                result.commit();
                //newFiles.remove(result.getName());  //synch'ed Vector
                newFileQueue.removeItem(result.getName());
                continue;
            }
            else 
            {
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                     + ERROR_TAG + result.getMessage() + "\n";
               if (this._mailmessage)
                  this._emailMessageLogger.error(msg);
               this._logger.error(msg);
               this._logger.debug("ERRNO = " + result.getErrno());
               
               this._triggerFileResultError(Constants.AUTOGETFILES, result);
               
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
                      msg += ERROR_TAG + "Subscription ["
                            + ftString + "]: Aborted.\n";
                      if (this._mailmessage)
                         this._emailMessageLogger.error(msg);
                      this._logger.error(msg);

                      ++this._errorCount;
                      timeToExit = true;
                      break;
                   }
                   //otherwise, just skip this file and log message
                   else                       
                   {                       
                       //newFiles.remove(filename);
                       newFileQueue.removeItem(result.getName());
                       msg = "FEI5 Information on "
                           + DateTimeUtil.getCurrentDateCCSDSAString()
                           + "\n";
                       msg += ERROR_TAG + "Subscription ["
                           + ftString + "]: Skipping '"+filename+"'\n";
                       if (this._mailmessage)
                           this._emailMessageLogger.warn(msg);
                       this._logger.warn(msg);
                   }
               }
               continue;
            }
         } //end_while_transactions_exist

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
      
      //logout of client, close subscription client
      if (this._client != null && this._client.isLoggedOn())
          this._client.logout();
          
      if (_subscriptionClient != null && _subscriptionClient.isAlive())
          _subscriptionClient.close();
      
      //---------------------------
      
      return success;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method implement the file auto get operation. The method supports continue
    * auto get file with restart capability. It also supports automatic
    * reconnection after server reboot.
    * 
    * @return true if success.
    * @throws SessionException
    */
   private boolean _autoGetQuery() throws SessionException 
   {
      
      long sleeptime = 5000; //5 seconds //1 minute
      
      //get options sg, ft - remember ft might be null
      String serverGroup = (String) this._argTable.get(CMD.SERVERGROUP);
      //String filetype    = (String) this._argTable.get(CMD.FILETYPE);
      String outputDir   = (String) this._argTable.get(CMD.OUTPUT);
      
      if (outputDir == null)
      {
          outputDir = System.getProperty("user.dir");
          this._argTable.put(CMD.OUTPUT, outputDir);
       }
      
      //---------------------------
      
      boolean loggerInitError = !_initSessionLogging(
                                      "FeiQ Subscription Notification",
                                      "FeiQ Subscription Report");
      if (loggerInitError)       
          return false;
      
      //---------------------------
      
      //set output dir
      this._client.changeDir(outputDir);

      //get settings from parser
      Object replace = this._argTable.get(CMD.REPLACE);
      Object version = this._argTable.get(CMD.VERSION);

      //check consistent state
      if (replace != null && version != null) {
         if (!this._using) {
            this._logger.info(this._getUsage());
         }
         ++this._errorCount;
         return false;
      }

      //set client according to parameters
      if (replace != null)
         this._client.set(Client.OPTION_REPLACEFILE, true);
      if (version != null)
         this._client.set(Client.OPTION_VERSIONFILE, true);
      if (this._argTable.get(CMD.SAFEREAD) != null)
         this._client.set(Client.OPTION_SAFEREAD, true);
      if (this._argTable.get(CMD.RECEIPT) != null)
         this._client.set(Client.OPTION_RECEIPT, true);
      if (this._argTable.get(CMD.CRC) != null) {
         this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
         this._logger.info("File resume transfer enabled.\n");
      }

      //get the invoke command string
      String invoke = (String) this._argTable.get(CMD.INVOKE);
      if (invoke != null) {
         invoke.trim();
         if (invoke.length() == 0)
            invoke = null;
      }

      //set exit on error flag
      boolean exitOnError = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEEXITONERROR) != null)
         exitOnError = true;

      //set invoke async flag
      boolean invokeAsync = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEASYNC) != null)
          invokeAsync = true;
      
      //---------------------------
          
      this._client.set(Client.OPTION_RESTART, true);      
      
      //---------------------------
      
      //check format option, and build new formatter, using default if
      //format value is null
      String format = (String) this._argTable.get(CMD.FORMAT);
      this._dateFormatter = new DateTimeFormatter(format);      

      //---------------------------
      
      //check replicate, enable if set
      if (this._argTable.get(CMD.REPLICATE) != null) {
         this._client.set(Client.OPTION_REPLICATE, true);
         this._logger.info("File replication enabled.\n");
      }
      
      if (this._argTable.get(CMD.REPLICATEROOT) != null) {
          String rootStr = (String) this._argTable.get(CMD.REPLICATEROOT);
          try {             
              this._client.setReplicationRoot(rootStr);
          } catch (SessionException sesEx) {
              this._logger.error(ERROR_TAG + sesEx.getMessage());
              ++this._errorCount;
              return false;
          }
      }

      //---------------------------
      
      //check diff, enable if set
      if (this._argTable.get(CMD.DIFF) != null) {
         this._client.set(Client.OPTION_DIFF, true);
      }
      //---------------------------
      
      String msg = "FEI5 Information on "
            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
            + "Subscribing to [" + serverGroup + "] server group.\n";

      if (this._mailmessage)
         this._emailMessageLogger.info(msg);
      this._logger.info(msg);

      //---------------------------
      
      boolean success = true;
      boolean newconnection = false;
      boolean timeToExit = false;

      
      //create copy of options for query client
      final Map qOptions = (Map) this._argTable.clone(); 
      QueryResultsCollector starScream = createClientCollector(qOptions,
    		  								    Constants.AUTOGETFILES);
      this._queryClient = starScream.getClient();
      
      //start the query subscription
      this._logger.debug("Starting subscription on query client");
      int xid = this._queryClient.subscribeQuery();
      
      //---------------------------
      
      //launch subscription client on own thread
      Thread subThread = new Thread(starScream);
      subThread.setName("Query_Result_Collector:"+serverGroup);
      subThread.start();
      
      //---------------------------
      
      //check to see if we bundle results so that they are retrieved
      //per filetype, or all files in results in sequential time order
      boolean bundleResults = Utils.bundleResultsByFiletype();
      
      //enter loop, while collector has work to do, keep going
      while (starScream.isActive()) 
      {
         long issueTime = System.currentTimeMillis();
         
         //---------------------------
         
         //if results are available, then process 'em
         //else go to sleep
         
         if (starScream.isResultAvailable())
         {
             List<String> filetypes  = starScream.getResultKeys();
             List<Result> allResults = starScream.getAllResults();
                     
             int iterations = bundleResults ? filetypes.size() : 
                                              allResults.size();
                          
             for (int i = 0; i < iterations; ++i)
             {
                 String ft = null;
                 if (bundleResults)
                 {
                     //collect a list of all files for the filetype to retrieve
                     //and call get() for that list
                     ft = filetypes.get(i);
                     List<Result> resultList = starScream.getResultsForFiletype(ft);
                     String[] filenames = Utils.getResultNames(resultList);
                     if (filenames != null && filenames.length > 0)
                     {
                    	 _setType(serverGroup, ft);
                         this._client.get(filenames);
                     }
                 }
                 else
                 {
                     //single file at a time
                     Result res = allResults.get(i);
                     ft = res.getType();
                     String filename = res.getName();
                     if (ft != null && filename != null)
                     {
                    	 _setType(serverGroup, ft);                         
                         this._client.get(new String[] {filename});
                     }                     
                 }                 
                 
                 timeToExit = false;
                 newconnection = false;
                 
                 //---------------------------
                 
                 //handle resulting files
                 while (this._client.getTransactionCount() > 0) 
                 {
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
                        boolean proceed =  _handleNewFile(result, outputDir,  
                                                          invoke, exitOnError,  
                                                          invokeAsync,
                                                          Constants.AUTOGETFILES);
                        if (!proceed)
                        {
                            timeToExit = true;
                            break;
                        }
                        
                        //invoke event handlers
                        this._triggerFileResultEvent(Constants.AUTOGETFILES, result);
                        
                        result.commit();
                        starScream.remove(ft, result.getName()); 
                        continue;
                    } 
                    else if (result.getErrno() == Constants.FILE_EXISTS) 
                    {
                       this._logger.info(result.getMessage());
                       this._triggerFileResultError(Constants.AUTOGETFILES, result);
                       result.commit();
                       starScream.remove(ft, result.getName());
                       continue;
                    } 
                    else if (result.getErrno() == Constants.IO_ERROR) 
                    {
                       msg = "FEI5 Information on "
                             + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                             + ERROR_TAG + "Lost connection to ["
                             + ft + "].  Attempting restart\n";
                       if (this._mailmessage && !this._mailSilentRecon)
                          this._emailMessageLogger.error(msg);
                       this._logger.error(msg);

                       //invoke error handlers
                       this._triggerFileResultError(Constants.AUTOGETFILES, result);
                       
                       this._client.logout();
                       this._client = this._createAutoQueryClient();
                       msg = "FEI5 Information on "
                             + DateTimeUtil.getCurrentDateCCSDSAString()
                             + "\nRestored subscription session to ["
                             + ft + "].\n";
                       if (this._mailmessage && !this._mailSilentRecon)
                          this._emailMessageLogger.info(msg);
                       this._logger.info(msg);

                       if (outputDir != null)
                          this._client.changeDir(outputDir);
                       if (replace != null)
                          this._client.set(Client.OPTION_REPLACEFILE, true);
                       if (version != null)
                          this._client.set(Client.OPTION_VERSIONFILE, true);
                       if (this._argTable.get(CMD.CRC) != null)
                          this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
                       if (this._argTable.get(CMD.SAFEREAD) != null)
                          this._client.set(Client.OPTION_SAFEREAD, true);
                       
                       //fix as part of AR118105 -------------------
                       //check replicate, enable if set
                       if (this._argTable.get(CMD.REPLICATE) != null) 
                          this._client.set(Client.OPTION_REPLICATE, true);               
                       if (this._argTable.get(CMD.REPLICATEROOT) != null) 
                       {
                           String rootStr = (String) this._argTable.get(CMD.REPLICATEROOT);                         
                           try {             
                               this._client.setReplicationRoot(rootStr);
                           } catch (SessionException sesEx) {
                               this._logger.error(ERROR_TAG + sesEx.getMessage());
                           }
                       }  
                       
                       //check diff
                       if (this._argTable.get(CMD.DIFF) != null) {
                           this._client.set(Client.OPTION_DIFF, true);
                       }
                       //end of fix --------------------------------------
                       
                       newconnection = true;
                       break;
                    } 
                    else if (result.getErrno() == Constants.FILE_NOT_FOUND)
                    {
                        msg = "FEI5 Information on "
                            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                            + ERROR_TAG + result.getMessage() + 
                            ", file not found on server.  Skipping file." + "\n";
                        if (this._mailmessage)
                            this._emailMessageLogger.error(msg);
                        this._logger.error(msg);
                        this._triggerFileResultError(Constants.AUTOGETFILES, result);
                        result.commit();
                        starScream.remove(ft, result.getName());
                        continue;
                    }
                    else 
                    {
                       msg = "FEI5 Information on "
                             + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                             + ERROR_TAG + result.getMessage() + "\n";
                       if (this._mailmessage)
                          this._emailMessageLogger.error(msg);
                       this._logger.error(msg);
                       this._logger.debug("ERRNO = " + result.getErrno());
                       this._triggerFileResultError(Constants.AUTOGETFILES, result);
                       
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
                              msg += ERROR_TAG + "Subscription ["
                                    + ft + "]: Aborted.\n";
                              if (this._mailmessage)
                                 this._emailMessageLogger.error(msg);
                              this._logger.error(msg);

                              ++this._errorCount;
                              timeToExit = true;
                              break;
                           }
                           //otherwise, just skip this file and log message
                           else                       
                           {
                               starScream.remove(ft, result.getName());
                               msg = "FEI5 Information on "
                                   + DateTimeUtil.getCurrentDateCCSDSAString()
                                   + "\n";
                               msg += ERROR_TAG + "Subscription ["
                                   + ft + "]: Skipping '"+filename+"'\n";
                               if (this._mailmessage)
                                   this._emailMessageLogger.warn(msg);
                               this._logger.warn(msg);
                           }
                       }
                       continue;
                    }
                 } //end_while_transactions_exist
                 
             } //end_for_iterations
             
             
             // if timeToExit was set, break out of outer loop
             if (timeToExit)
                break;


             
         } //end_if_results         
         
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
                    shouldSleep = !starScream.isResultAvailable();
                    this._logger.debug("Checking for query results...");
                    shouldSleep = shouldSleep && this._queryClient.isAlive();
                    if (shouldSleep)
                        Thread.sleep(sleeptime);
                }
            } catch (InterruptedException e) {
               break; // exit the infinite loop and return
            }
         }
         
      } //end_while_starscream_active
             
             
      //---------------------------
      
      //logout of client, close query client
      if (this._client != null && this._client.isLoggedOn())
          this._client.logout();
          
      if (this._queryClient != null && this._queryClient.isAlive())
          this._queryClient.close();
      
      //---------------------------
      
      return success;
   }

   //---------------------------------------------------------------------
   
   protected void _setType(String servergroup, String type) 
   												  throws SessionException
   {
	   this._client.setType(servergroup, type);
       while (this._client.getTransactionCount() > 0) {
           Result r = this._client.getResult();
           if (r == null) {
               continue;
           }
           if (r.getErrno() != Constants.OK) {
        	   throw new SessionException("Error occurred while setting type "+
        			   "to "+FileType.toFullFiletype(servergroup,type)+
        			   ". Message: "+r.getMessage(),
        			   r.getErrno());
           }
       }
   }
   
   //---------------------------------------------------------------------
   //---------------------------------------------------------------------
   
   /**
    * Convenience method that creates a new QueryClient instance and uses
    * that to create a new result collector instance.  The collector is
    * returned, and has a public method, getClient(), that returns
    * the underlying query client.
    * @param qOptions Options map used to create query client
    * @param operation Operation code
    * @return New instance of QueryResultsCollector with new underlying
    * query client.
    * @throws SessionException if error occurs.
    */
   
   protected QueryResultsCollector createClientCollector(Map qOptions, 
		                       String operation) throws SessionException
   {
       String msg;
       QueryClient queryClient;
       
       try {
           //use utility to collect required data to built a query client
           QueryClientUtil barricade = new QueryClientUtil(qOptions, operation);
           queryClient = barricade.getQueryClient();          
       } catch (SessionException sesEx) {
           msg = "Unable to construct query client.  Aborting...";
           this._logger.error(msg);
           this._logger.debug(null, sesEx);
           throw sesEx;
       }
       
       //---------------------------
       
       //construct result collector, which will retrieve from qClient
       QueryResultsCollector starScream = new QueryResultsCollector(queryClient);
       
       //---------------------------
       
       return starScream;
   }
   
   
   
   //---------------------------------------------------------------------
   //---------------------------------------------------------------------

   /**
    * Application method to subscribe to new file available event from the
    * server
    * 
    * @return true if success
    * @throws SessionException
    */
   private boolean _autoShowPull() throws SessionException 
   {
      long queryInterval = this._queryInterval == null ? _MINUTE_MS : 
                           Long.parseLong(this._queryInterval)
                           * _MINUTE_MS;
      String ftString = FileType.toFullFiletype(
              (String) this._argTable.get(CMD.SERVERGROUP),
              (String) this._argTable.get(CMD.FILETYPE));
      long sleeptime = queryInterval;

      //---------------------------
      
      boolean loggerInitError = !_initSessionLogging("Notification", 
                                                     "Notification Report");
      if (loggerInitError)       
          return false;
      
      //---------------------------
      
      //set output dir
      String outputDir = (String) this._argTable.get(CMD.OUTPUT);
      if (outputDir == null)
         outputDir = System.getProperty("user.dir");
      this._client.changeDir(outputDir);

      //load invoke string
      String invoke = (String) this._argTable.get(CMD.INVOKE);
      if (invoke != null) {
         invoke.trim();
         if (invoke.length() == 0)
            invoke = null;
      }

      //set exitOnError flag
      boolean exitOnError = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEEXITONERROR) != null)
         exitOnError = true;

      //set invoke async flag
      boolean invokeAsync = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEASYNC) != null)
          invokeAsync = true;
      
      //---------------------------

      //check format option, and build new formatter
      String format = (String) this._argTable.get(CMD.FORMAT);
      this._dateFormatter = new DateTimeFormatter(format);  
      
      //---------------------------
      
      /*
       * here is the algorithm - Since we are relying on client to automatic
       * query, restart (in the client API) should be always enabled. The
       * 'restart' command option translates to restart from the latest queried
       * time. If the user did not specify this option, then it should use the
       * default restart time, that is the current time, and persist the time
       * for each file received.
       */
      Date queryTime = null;
      if (this._argTable.get(CMD.RESTART) == null) {
         queryTime = new Date();
      }
      this._client.set(Client.OPTION_RESTART, true);

      String msg = "FEI5 Information on "
            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
            + "Subscribing to [" + ftString + "] file type.\n";

      if (this._mailmessage)
         this._emailMessageLogger.info(msg);
      this._logger.info(msg);

      boolean success = true;
      boolean newconnection = false;

      //enter loop
      while (true) 
      {

         long issueTime = System.currentTimeMillis();
         int tranId = this._client.showAfter(queryTime);

         newconnection = false;
         queryTime = null;

         boolean shouldExit = false;

         //handle files
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            } else if (result.getErrno() == Constants.NO_FILES_MATCH) {
               // no new files at this time
               continue;
            } else if (result.getErrno() == Constants.OK) {
                boolean proceed =  _handleNewFile(result, outputDir,  
                                                  invoke, exitOnError,  
                                                  invokeAsync,
                                                  Constants.AUTOSHOWFILES);
                if (!proceed)
                {
                    shouldExit = true; 
                    break;
                }
                
                //invoke event handlers
                this._triggerFileResultEvent(Constants.AUTOSHOWFILES, result);
                
                result.commit();
                continue;
            } else if (result.getErrno() == Constants.IO_ERROR) {
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                     + ERROR_TAG + "Lost connection to ["
                     + ftString + "].  Attempting restart\n";
               if (this._mailmessage && !this._mailSilentRecon)
                  this._emailMessageLogger.error(msg);
               this._logger.error(msg);
               
               this._triggerFileResultError(Constants.AUTOSHOWFILES, result);
               
               this._client.logout();

               this._client = this._createAutoQueryClient();
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString()
                     + "\nRestored notification session to ["
                     + ftString + "].\n";

               if (this._mailmessage && !this._mailSilentRecon)
                  this._emailMessageLogger.info(msg);
               this._logger.info(msg);
               newconnection = true;
               break;
            } else {
               msg = "FEI5 Information on "
                     + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                     + ERROR_TAG + result.getMessage() + "\n";
               if (this._mailmessage)
                  this._emailMessageLogger.error(msg);
               this._logger.error(msg);
               this._logger.debug("ERRNO = " + result.getErrno());
               this._triggerFileResultError(Constants.AUTOSHOWFILES, result);
               continue;
            }
         }

         if (shouldExit)
            break;

         if (!newconnection) {
            long processTime = System.currentTimeMillis() - issueTime;
            sleeptime = processTime > queryInterval ? 0 : queryInterval
                  - processTime;
            this._logger.debug("sleep time = " + sleeptime);
            try {
               Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
               // exist the infinite loop and return
               break;
            }
         }
      }

      return success;
   }
   
   //---------------------------------------------------------------------

   /**
    * Method implement the file auto get operation. The method supports continue
    * auto get file with restart capability. It also supports automatic
    * reconnection after server reboot.
    * 
    * @return true if success.
    * @throws SessionException
    */
   private boolean _autoShowPush() throws SessionException {
      
      long sleeptime = 1000; //1 second
      String ftString = FileType.toFullFiletype(
                                 (String) this._argTable.get(CMD.SERVERGROUP),
                                 (String) this._argTable.get(CMD.FILETYPE));     
      
      //---------------------------
      
      //we don't use this client any more, so get rid of it
      if (this._client != null && this._client.isLoggedOn())
          this._client.logout();
      
      //---------------------------
      
      boolean loggerInitError = !_initSessionLogging("Notification", 
                                                     "Notification Report");
      if (loggerInitError)       
          return false;
      
      //---------------------------
      
      //set output dir
      String outputDir = (String) this._argTable.get(CMD.OUTPUT);
      if (outputDir == null)
         outputDir = System.getProperty("user.dir");
      //this._client.changeDir(outputDir);  //no longer using client


      //get the invoke command string
      String invoke = (String) this._argTable.get(CMD.INVOKE);
      if (invoke != null) {
         invoke.trim();
         if (invoke.length() == 0)
            invoke = null;
      }

      //set exit on error flag
      boolean exitOnError = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEEXITONERROR) != null)
         exitOnError = true;   
      
      //set invoke async flag
      boolean invokeAsync = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEASYNC) != null)
          invokeAsync = true;
      
      //---------------------------
      
      //check format option, and build new formatter
      String format = (String) this._argTable.get(CMD.FORMAT);
      this._dateFormatter = new DateTimeFormatter(format);  
      
      //---------------------------
      
      String msg = "FEI5 Information on "
            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
            + "Subscribing to [" + ftString + "] file type.\n";

      if (this._mailmessage)
         this._emailMessageLogger.info(msg);
      this._logger.info(msg);

      //---------------------------
      
      boolean success = true;
      boolean timeToExit = false;

      //construct a notification client that will generate new file events
      //final List newFileResults = new Vector();
      final PushFileEventQueue<Result> newResultQueue = 
                            new PushFileEventQueue<Result>();
      final Map clientOptions = (Map) this._argTable.clone();      
      if (!clientOptions.containsKey(CMD.OUTPUT))
          clientOptions.put(CMD.OUTPUT, ".");    
      
      try {
          _subscriptionClient = new PushSubscriptionClient(
                                    this._domainFile, clientOptions, 
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
//                      if (!newFileResults.contains(result))
//                          newFileResults.add(result);                    
                  }
              }
          }          
      };
      
      //add anon. listener to client
      _subscriptionClient.addSubscriptionEventListener(subListener);
      
      //---------------------------
      
      //launch subscription client on own thread
      Thread subThread = new Thread(_subscriptionClient);
      subThread.setName("Notification_Thread_"+ftString);
      subThread.start();
      
      //---------------------------
      
      //enter loop
      while (_subscriptionClient.isAlive()) {
         long issueTime = System.currentTimeMillis();
         
         Result[] resultArray;
         
         //---------------------------
         
         //lock newFiles collection, create array of Results from
         //contents. 
         synchronized(newResultQueue)
         {
             newResultQueue.advanceQueue();
             List<Result> resultList = newResultQueue.getItemsInProcess();
             resultArray = resultList.toArray(new Result[0]);
//             int size = newFileResults.size();
//             resultArray = new Result[0];
//             resultArray = (Result[]) newFileResults.toArray(resultArray);
         } //end synch
         
             
         for (int i = 0; i < resultArray.length; ++i)
         {
             Result result = resultArray[i];
          
             boolean proceed =  _handleNewFile(result, outputDir,  
                                               invoke, exitOnError, 
                                               invokeAsync,
                                               Constants.AUTOSHOWFILES);
                          
             if (!proceed)
             {
                 timeToExit = true;
                 break;
             }
               
             //invoke event handlers
             this._triggerFileResultEvent(Constants.AUTOSHOWFILES, result);
             
            result.commit();
            newResultQueue.removeItem(result);
            //newFileResults.remove(result);  //synch'ed Vector
            
         } //end_for_loop
             
         if (timeToExit)
             break;
         
         //---------------------------
      
         //if we have the same connection, check if we should nap
         this._logger.debug("Waiting for new files...");
         try {
             //assume we are gonna sleep...
             boolean shouldSleep = true;
             while(shouldSleep)
             {
                 //check to see if we really should sleep
                 synchronized(newResultQueue) {
                     shouldSleep = newResultQueue.isEmpty();
                 }
                 shouldSleep = shouldSleep && _subscriptionClient.isAlive();
                 if (shouldSleep)
                     Thread.sleep(sleeptime);
             }
          } catch (InterruptedException e) {
              break; //exit the infinite loop and return
          }
       }
   
      //---------------------------
      
      //logout of client, close subscription client (moved to beginning)
      //if (this._client != null && this._client.isLoggedOn())
      //    this._client.logout();
          
      if (_subscriptionClient != null && _subscriptionClient.isAlive())
          _subscriptionClient.close();
      
      //---------------------------
      
      return success;
   }

   
   //---------------------------------------------------------------------

   /**
    * Method implement the file auto show operation uing the FeiQ client.
    * The method supports continue auto show file with restart capability. 
    * It also supports automatic reconnection after server reboot.
    * 
    * @return true if success.
    * @throws SessionException If error occurs
    */
   
   private boolean _autoShowQuery() throws SessionException 
   {
      
	   long sleeptime = 5000; //5 seconds
      
      //---------------------------
      
      //we don't use this client any more, so get rid of it
      if (this._client != null && this._client.isLoggedOn())
          this._client.logout();
      
      //---------------------------
      
      //get options sg, ft - remember ft might be null
      String serverGroup = (String) this._argTable.get(CMD.SERVERGROUP);
      String filetype    = (String) this._argTable.get(CMD.FILETYPE);
      String outputDir   = (String) this._argTable.get(CMD.OUTPUT);
      
      if (outputDir == null)
      {
          outputDir = System.getProperty("user.dir");
          this._argTable.put(CMD.OUTPUT, outputDir);
       }
      
      //---------------------------
      
      boolean loggerInitError = !_initSessionLogging(
                                      "FeiQ Notification",
                                      "FeiQ Notification Report");
      if (loggerInitError)       
          return false;
      
      //---------------------------     

      //get the invoke command string
      String invoke = (String) this._argTable.get(CMD.INVOKE);
      if (invoke != null) {
         invoke.trim();
         if (invoke.length() == 0)
            invoke = null;
      }

      //set exit on error flag
      boolean exitOnError = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEEXITONERROR) != null)
         exitOnError = true;

      //set invoke async flag
      boolean invokeAsync = false;
      if (invoke != null && this._argTable.get(CMD.INVOKEASYNC) != null)
          invokeAsync = true;
      
      //---------------------------
      
      //check format option, and build new formatter, using default if
      //format value is null
      String format = (String) this._argTable.get(CMD.FORMAT);
      this._dateFormatter = new DateTimeFormatter(format);      

      //---------------------------
      
      String msg = "FEI5 Information on "
            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
            + "Subscribing to [" + serverGroup + "] server group.\n";

      if (this._mailmessage)
         this._emailMessageLogger.info(msg);
      this._logger.info(msg);

      //---------------------------
      
      boolean success = true;
      boolean newconnection = false;
      boolean timeToExit = false;

      //---------------------------
      
      //create copy of options for query client
      final Map qOptions = (Map) this._argTable.clone(); 
      QueryResultsCollector starScream = createClientCollector(qOptions, Constants.AUTOSHOWFILES);
      this._queryClient = starScream.getClient();
      
      //start the query subscription
      this._logger.debug("Starting subscription on query client");
      int xid = this._queryClient.subscribeQuery();
      
      //---------------------------
      
      //launch subscription client on own thread
      Thread subThread = new Thread(starScream);
      subThread.setName("Query_Result_Collector:"+serverGroup);
      subThread.start();
     
      //---------------------------
      
      //check to see if we bundle results so that they are retrived
      //per filetype, or all files in results in seqential time order
      boolean bundleResults = Utils.bundleResultsByFiletype();
      
      //enter loop, while collector has work to do, keep going
      while (starScream.isActive()) 
      {
         long issueTime = System.currentTimeMillis();
         
         //---------------------------
         
         //if no results available, then go to sleep
         if (starScream.isResultAvailable())
         {
             List<String> filetypes  = starScream.getResultKeys();
             List<Result> allResults = starScream.getAllResults();
             
             if (bundleResults)
             {
            	 
            	 //if bundle, then create a reordered list that combines
            	 //same filetype'd results
                 List<Result> reorderedResults = new ArrayList<Result>();
                 for (int i = 0; i < filetypes.size(); ++i)
                 {
                     String ft = filetypes.get(i);
                     List<Result> resultList = starScream.getResultsForFiletype(ft);
                     reorderedResults.addAll(resultList);                    
                 }
                 
                 //now we can process the reordered results
                 allResults = reorderedResults;                 
             }
             
             
             for (Result result : allResults)
             {                 
                 String ft = result.getType();
                 String filename = result.getName();
                     
                 boolean proceed =  _handleNewFile(result, outputDir,  
                                                   invoke, exitOnError,  
                                                   invokeAsync,
                                                   Constants.AUTOSHOWFILES);
                 if (!proceed)
                 {
                     timeToExit = true;
                     break;
                 }
                 
                 //invoke event handlers
                 this._triggerFileResultEvent(Constants.AUTOSHOWFILES, result);
                 
                 result.commit();
                 starScream.remove(ft, result.getName());
             }
             
             // if timeToExit was set, break out of outer loop
             if (timeToExit)
                break;
         } //end_if_results         
         
         //if we have the same connection, check if we should nap
         this._logger.debug("Waiting for new files...");
         try {
             //assume we are gonna sleep...
             boolean shouldSleep = true;
             while(shouldSleep)
             {
                 //check to see if we really should sleep
                 this._logger.debug("Checking for results from query");            	 
                 shouldSleep = !starScream.isResultAvailable();
                 shouldSleep = shouldSleep && this._queryClient.isAlive();
                 if (shouldSleep)
                     Thread.sleep(sleeptime);
             }
         } catch (InterruptedException e) {
             break; // exit the infinite loop and return
         }
         
      } //end_while_starscream_active
             
             
      //---------------------------
      
      //close query client
      if (this._queryClient != null && this._queryClient.isAlive())
          this._queryClient.close();
      
      //---------------------------
      
      return success;
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
    * @param invokeAsync Flag indicating that invocation will proceed
    *        asynchronously
    * @param operation Operation id, one of Constants.AUTO(GET|SHOW)FILES.
    * @return False iff file was unsuccessfully processed and exit should 
    *         occur.
    */
   
   private boolean _handleNewFile(Result result, String outputDir,
                                  String invoke, boolean exitOnError,
                                  boolean invokeAsync, String operation)
   {       
       boolean canProceed = true;
       String name = result.getName();
       String ftString = FileType.toFullFiletype(result.getServerGroup(),
                                                 result.getType());
       String state = (operation.equals(Constants.AUTOGETFILES)) ? 
                      "received" : "available";
       
       //create log message
       String msg = "FEI5 Information on "
                      + DateTimeUtil.getCurrentDateCCSDSAString() 
                      + "\n";
       msg += "File type [" + ftString + "]: "+state+" \""
               + name + "\" " + result.getSize() + " bytes "
               + this._dateFormatter.formatDate(
                              result.getFileModificationTime());
        if (result.getChecksumStr() != null)
            msg += (" CRC:" + result.getChecksumStr());
        msg += "\n";
        if (this._mailreport) {
            synchronized (this._reportmessage) {
                this._reportmessage.append(msg + "\n");
            }
        }
        this._logger.info(msg);

        //-------------------------
        
        // handle invoke
        if (invoke != null) 
        {
            //returns false if we should exit
            canProceed = _performInvocation(result, outputDir, invoke,  
                                            exitOnError, invokeAsync,
                                            operation);                              
        }
        
        //-------------------------
        
        //_triggerFileResultEvent(operation, result);

        //-------------------------
        
        
        return canProceed;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Performs the result invocation for a new file for notification,
    * subscription, and get
    * @param result Result object representing new file state
    * @param outputDir Output directory path
    * @param invoke Invocation command string
    * @param exitOnError Flag indicating that invocation was not
    *        successful and thus processing should be aborted.
    * @param invokeAsync Flag indicating that invocation will proceed
    *        asynchronously
    * @param operation Operation constant (Constants.{AUTOGETFILES,
    *        AUTOSHOWFILES,GETFILES}
    * @return False if error occurred and exitOnError was set,
    *         true otherwise.
    */

   private boolean _performInvocation(Result result, String outputDir,
                                      String invoke, boolean exitOnError,
                                      boolean invokeAsync, String operation)
   {
       boolean canProceed = true;
       String name = result.getName();
       String ftString = FileType.toFullFiletype(result.getServerGroup(),
                                                 result.getType());
       String opName = null;
       
       if (operation.equals(Constants.AUTOGETFILES))
           opName = "Subscription";
       else if (operation.equals(Constants.AUTOSHOWFILES))
           opName = "Notification";
       else if (operation.equals(Constants.GETFILES))
           opName = "Get";
       
       
       //prepare actual invocation command    
       String cmdStr = InvocationCommandUtil.buildCommand(
                                invoke, outputDir, result);

       //log invocation attempt
       String msg = "FEI5 Information on " +
                        DateTimeUtil.getCurrentDateCCSDSAString() + 
                        "\n";
       msg += "Invoke command \"" + cmdStr.trim() + "\"\n";
       if (this._mailreport) {
           synchronized (this._reportmessage) {
               this._reportmessage.append(msg + "\n");
           }
       }
       this._logger.info(msg);

       //execute the command
       Errno errno;
       if (invokeAsync)
           errno = SystemProcess.executeAsync(cmdStr, this._logger);
       else
           errno = SystemProcess.execute(cmdStr, this._logger);

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
           msg += ERROR_TAG + "File type [" + ftString +
                     "]: invoke process \"" + cmdStr +
                     "\" failed. "+errnoReport+"\n";
           
           if (this._mailmessage)
              this._emailMessageLogger.error(msg);
           if (this._mailreport)
           {
               synchronized (this._reportmessage) {
                   this._reportmessage.append(msg + "\n");
               }
           }
           this._logger.error(msg);
        
           //----------------------
           
           //if exitOnErr set, write message and break loop
           if (exitOnError) 
           {
              msg = "FEI5 Information on "
                    + DateTimeUtil.getCurrentDateCCSDSAString()
                    + "\n";
              msg += ERROR_TAG + opName + " ["
                    + ftString + "]: Aborted.\n";
              if (this._mailmessage)
                 this._emailMessageLogger.error(msg);
              this._logger.error(msg);
        
              ++this._errorCount;
              canProceed = false;
           }
       }
       else
       {
           msg =  "FEI5 Information on " + 
                              DateTimeUtil.getCurrentDateCCSDSAString() + "\n";
           msg += "Message from filetype [" + ftString +
                              "]: invoke process \"" + cmdStr +
                              "\" (errno = "+errno.getId()+"): " + 
                              errno.getMessage()+"\n";
           this._logger.debug(msg);
       }
       
       return canProceed;
   }

   //---------------------------------------------------------------------
   
   protected boolean _triggerFileResultEvent(String operation, Result result)
                                                      throws SessionException
   {
       boolean success = true;       

       if (this._fileEventHandlerSet != null)
       {
           FileResultEvent event = new FileResultEvent(operation, result);

           try {
               this._fileEventHandlerSet.eventOccurred(event);
           } catch (HandlerException hEx) {
               this._logger.error(hEx.getMessage());
               this._logger.debug(null, hEx);
               success = false;
               
               throw new SessionException(hEx.getMessage(), 
                                          Constants.EXCEPTION);
           }
       }     
       
       return success;
   }
   
   //---------------------------------------------------------------------
   
   protected boolean _triggerFileResultError(String operation, Result result) 
                                                   throws SessionException
   {
       boolean success = true;
       FileResultError error = new FileResultError(operation, result);
       
       if (this._fileEventHandlerSet != null)
       {
           try {
               this._fileEventHandlerSet.errorOccurred(error);
           } catch (HandlerException hEx) {
               
               this._logger.error(hEx.getMessage());
               this._logger.debug(null, hEx);
               success = false;
               
               throw new SessionException(hEx.getMessage(), 
                                          Constants.EXCEPTION);
           }
       }      
       
       return success;
   }
   
   //---------------------------------------------------------------------

   /**
    * Delete action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails
    */
   private boolean _delete() throws SessionException {

      String[] files;

      if (this._userOperation.equalsIgnoreCase("komodo.util.makeclean")) {
         files = new String[] { "*" };
      } else {
         files = (String[]) this._argTable.get(UtilCmdParser.KEYWORD_FILES);
      }
      boolean success = true;

      if (files != null && files.length > 0) {
         for (int i = 0; i < files.length; ++i) {
            this._client.delete(files[i]);
         }

         String currentType = "";
         NumberFormat fmt = NumberFormat.getInstance();
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() != Constants.OK) {
               this._logger.error(result.getMessage());
               ++this._errorCount;
               this._triggerFileResultError(Constants.DELETEFILE, result);
               continue;
            }
            if (!currentType.equals(result.getType()) && this._using) {
               currentType = result.getType();
               this._logger.info("\nDeleting from file type \"" + currentType
                     + "\":");
            }
            this._logger.info(this._align(fmt, ++this._resultIdx, 6)
                  + ". Deleted \"" + result.getName() + "\".");
            
            //invoke event handlers
            this._triggerFileResultEvent(Constants.DELETEFILE, result);
            
         }
      }
      return success;
   }

   //---------------------------------------------------------------------

   /**
    * Comment action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails.
    */
   private boolean _comment() throws SessionException {

      String[] files = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);
      String comment = (String) this._argTable.get(CMD.COMMENT);

      boolean success = false;
      if (comment == null || comment.equalsIgnoreCase("")) {
         this._getUsage();
         ++this._errorCount;
         return success;
      }

      if (files != null && files.length > 0) {
         this._client.comment(files[0], comment);

         NumberFormat fmt = NumberFormat.getInstance();
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() != Constants.OK) {
               this._logger.error(result.getMessage());
               ++this._errorCount;
               this._triggerFileResultError(Constants.COMMENTFILE, result);
               continue;
            }
            
            this._logger.info(this._align(fmt, ++this._resultIdx, 6)
                  + ". Commented \"" + result.getName() + "\".");
            this._triggerFileResultEvent(Constants.COMMENTFILE, result);
         }
      }
      return success;
   }

   //---------------------------------------------------------------------

   
   private StringBuffer loadQueryFile(File queryFile) throws SessionException
   {
       StringBuffer buffer = new StringBuffer();       
       BufferedReader reader = null;
       
       try { 
           
           //Security code review prompted this change to prevent DOS attack (nttoole 08.27.2013)
           //reader = new BufferedReader(new FileReader(queryFile));
           reader = new BoundedBufferedReader(new FileReader(queryFile));
           
           String line, separator = System.getProperty("line.separator");
           while ((line = reader.readLine()) != null)
           {
               buffer.append(line).append(separator);
           }
       } catch (FileNotFoundException fnfEx) {
           throw new SessionException("Query file '" + 
                           queryFile.getAbsolutePath() + 
                           "' does not exist.", Constants.FILE_NOT_FOUND);
       } catch (Exception ex) {
           throw new SessionException("Query file '" + 
                           queryFile.getAbsolutePath() + 
                           "' could not be read: "+ex.getMessage(), 
                           Constants.IO_ERROR);
       } finally {
           if (reader != null)
           {    
               try { reader.close(); } catch (IOException ioEx) {}
           }
       }
       
       return buffer;
   }
   
   
   /**
    * List action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails
    */
   private boolean _list() throws SessionException {
       
       if (this._argTable.get(CMD.QUERY) == null)
           return _listCommon();
       else
       {
           return _listQuery();
       }              
   }
   
   
   private boolean _listCommon() throws SessionException {
      String[] files = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);
      if (files == null) {
         files = new String[] { "*" };

      }
      boolean longList = false;
      boolean verylongList = false;

      if (this._argTable.get(CMD.LONG) != null) {
         this._client.setVerbose(true);
         longList = true;
      }
      if (this._argTable.get(CMD.VERYLONG) != null) {
         this._client.setVeryVerbose(true);
         verylongList = true;
      }

      String before = (String) this._argTable.get(CMD.BEFORE);
      String after = (String) this._argTable.get(CMD.AFTER);
      String between = (String) this._argTable.get(CMD.BETWEEN);
      String and = (String) this._argTable.get(CMD.AND);
      String format = (String) this._argTable.get(CMD.FORMAT);
      String query = (String) this._argTable.get(CMD.QUERY);
      
      this._dateFormatter = new DateTimeFormatter(format);

      int transId;

      try {
         if (before != null) {
            transId = this._client.showBefore(
                            this._dateFormatter.parseDate(before), 
                            files[0]);
         } else if (after != null) {
            transId = this._client.showAfter(
                            this._dateFormatter.parseDate(after),
                            files[0]);
         } else if (between != null && and != null) {
            transId = this._client.showBetween(
                            this._dateFormatter.parseDate(between), 
                            this._dateFormatter.parseDate(and),
                            files[0]);
         } else if (before == null && after == null && between == null
               && and == null) {
            transId = this._client.show(files[0]);
         }
      } catch (ParseException e) {
         throw new SessionException(e.getMessage(), -1);
      }

      int count = 0;
      while (this._client.getTransactionCount() > 0) {
         Result r = this._client.getResult();
         if (r == null) {
            continue;
         }

         if (r.getErrno() == Constants.NO_FILES_MATCH) {
            this._logger.info(r.getMessage());
            continue;
         } else if (r.getErrno() == Constants.OK) {
            count++;
            _listResult(r, longList, verylongList, count);   
            this._triggerFileResultEvent(Constants.SHOWFILES, r);            
         } else {
             this._logger.error(ERROR_TAG + r.getMessage());           
            ++this._errorCount;
            
            //invoke error handlers
            this._triggerFileResultError(Constants.SHOWFILES, r);
            
            continue;
         }
      }
      return true;
   }

   
   private boolean _listQuery() throws SessionException 
   {
	  boolean newconnection = false;
      boolean timeToExit = false;
      long sleeptime = 5000; //5 seconds
      boolean longList = false;
      boolean verylongList = false;
      int count = 0;
      String msg;
      boolean sortFileTypes = true;
      
      //---------------------------
      
      //we don't use this client any more, so get rid of it
//      if (this._client != null && this._client.isLoggedOn())
//          this._client.logout();
      
      //---------------------------
      
      //get options sg, ft - remember ft might be null
      String serverGroup = (String) this._argTable.get(CMD.SERVERGROUP);
      String filetype    = (String) this._argTable.get(CMD.FILETYPE);
      String outputDir   = (String) this._argTable.get(CMD.OUTPUT);
      
      if (outputDir == null)
      {
          outputDir = System.getProperty("user.dir");
          this._argTable.put(CMD.OUTPUT, outputDir);
       }

      if (this._argTable.get(CMD.LONG) != null) 
      {
         this._client.setVerbose(true);
         longList = true;
      }
      
      if (this._argTable.get(CMD.VERYLONG) != null) 
      {
         this._client.setVeryVerbose(true);
         verylongList = true;
      }
      
      //---------------------------     
      
      //check format option, and build new formatter, using default if
      //format value is null
      String format = (String) this._argTable.get(CMD.FORMAT);
      this._dateFormatter = new DateTimeFormatter(format);      

      //---------------------------
      
      boolean success = true;
      
      //create copy of options for query client
      final Map qOptions = (Map) this._argTable.clone(); 
      QueryResultsCollector starScream;
      
      //use utility to collect required data to built a query client
      QueryClientUtil barricade  = new QueryClientUtil(qOptions, Constants.SHOWFILES);
      this._queryClient = barricade.getQueryClient();
      this._logger.debug("Starting session on query client");
      int xid = this._queryClient.sendQuery();
      
      //---------------------------
      
      //construct result collector, which will retrieve from qClient
      starScream = new QueryResultsCollector(this._queryClient);
      
      //---------------------------
      
      //launch subscription client on own thread
      Thread subThread = new Thread(starScream);
      subThread.setName("Query_Result_Collector_"+serverGroup);
      subThread.start();
      
      //---------------------------
      
      //check to see if we bundle results so that they are retrieved
      //per filetype, or all files in results in sequential time order
      boolean bundleResults = Utils.bundleResultsByFiletype();
      
      //enter loop, while collector has work to do, keep going
    //enter loop, while collector has work to do, keep going
      while (starScream.isActive()) 
      {
         long issueTime = System.currentTimeMillis();
         
         //---------------------------
         
         //if results are available, then process 'em
         //else go to sleep
         
         if (starScream.isResultAvailable())
         {
             List<String> filetypes  = starScream.getResultKeys();
             List<Result> allResults = starScream.getAllResults();
                     
             if (sortFileTypes)
            	 Collections.sort(filetypes);
             
             int iterations = bundleResults ? filetypes.size() : 
                                              allResults.size();
                          
             for (int i = 0; i < iterations; ++i)
             {
                 String ft = null;
                 if (bundleResults)
                 {                	 
                     //collect a list of all files for the filetype to retrieve
                     //and call get() for that list
                     ft = filetypes.get(i);
                     List<Result> resultList = starScream.getResultsForFiletype(ft);
                     String[] filenames = Utils.getResultNames(resultList);
                     if (filenames != null && filenames.length > 0)
                     {
                    	 _setType(serverGroup, ft);
                    	 for (String filename : filenames)
                    		 this._client.show(filename);
                     }
                 }
                 else
                 {
                     //single file at a time
                     Result res = allResults.get(i);
                     ft = res.getType();
                     String filename = res.getName();
                     if (ft != null && filename != null)
                     {
                    	 _setType(serverGroup, ft);                         
                         this._client.show(filename);
                     }                     
                 }                 
                 
                 timeToExit = false;
                 newconnection = false;
                 
                 //---------------------------
                 
                 //handle resulting files
                 while (this._client.getTransactionCount() > 0) 
                 {
                    Result r = this._client.getResult();
                    if (r == null) 
                    {
                       continue;
                    } 
                    else if (r.getErrno() == Constants.NO_FILES_MATCH) 
                    {
                       // no new files at this time
                       continue;
                    } 
                    else if (r.getErrno() == Constants.OK) 
                    {
                        count++;
                        _listResult(r, longList, verylongList, true, count);    
                        this._triggerFileResultEvent(Constants.SHOWFILES, r);     
                        r.commit();
                        starScream.remove(ft, r.getName()); 
                        continue;
                    }                    
                    else if (r.getErrno() == Constants.IO_ERROR) 
                    {
                        this._logger.error(ERROR_TAG + r.getMessage());           
                        ++this._errorCount;
                        this._triggerFileResultError(Constants.SHOWFILES, r);     
                        timeToExit = true;
                        break;
                        //continue;
                    } 
                    else if (r.getErrno() == Constants.FILE_NOT_FOUND)
                    {
                        msg = "FEI5 Information on "
                            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                            + ERROR_TAG + r.getMessage() + 
                            ", file not found on server. Skipping file." + "\n";
                   
                        this._logger.error(msg);
                        this._triggerFileResultError(Constants.SHOWFILES, r);
                        r.commit();
                        starScream.remove(ft, r.getName());
                        continue;
                    }
                    else 
                    {
                       msg = "FEI5 Information on "
                             + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                             + ERROR_TAG + r.getMessage() + "\n";

                       this._logger.error(msg);
                       this._logger.debug("ERRNO = " + r.getErrno());
                       this._triggerFileResultError(Constants.SHOWFILES, r);     
                       
                       //is this error associated with a file?  If so, more processing
                       //is needed.
                       String filename = r.getName();
                       if (filename != null)
                       {                          
                           //otherwise, just skip this file and log message 
                    	   starScream.remove(ft, r.getName());
                               msg = "FEI5 Information on "
                               + DateTimeUtil.getCurrentDateCCSDSAString()
                               + "\n";
                           msg += ERROR_TAG + "Subscription ["
                               + ft + "]: Skipping '"+filename+"'\n";

                           this._logger.warn(msg);
                       }
                       continue;
                    }
                 } //end_while_transactions_exist
                 
             } //end_for_iterations
             
             
             // if timeToExit was set, break out of outer loop
             if (timeToExit)
                break;

         } //end_if_results         
         
         //if we have the same connection, check if we should nap
         if (true) 
         {            
            this._logger.debug("Waiting for new files...");
            try {
                //assume we are gonna sleep...
                boolean shouldSleep = true;
                while(shouldSleep)
                {
                    //check to see if we really should sleep
                    shouldSleep = !starScream.isResultAvailable();
                    this._logger.debug("Checking for query results...");
                    shouldSleep = shouldSleep && this._queryClient.isAlive();
                    if (shouldSleep)
                        Thread.sleep(sleeptime);
                }
            } catch (InterruptedException e) {
               e.printStackTrace();            	
               break; // exit the infinite loop and return
            }
         }
         
      } //end_while_starscream_active
             
             
      //---------------------------
      
      //close query client
      if (this._queryClient != null && this._queryClient.isAlive())
          this._queryClient.close();
     
      //close client
      if (this._client != null && this._client.isLoggedOn())
          this._client.logout();
      
      //---------------------------
      
      return success;
   }
    
   //---------------------------------------------------------------------
   
   private void _listResult(Result r, boolean longList, boolean verylongList,
		                    int count)
   {
	   _listResult(r, longList, verylongList, false, count);
   }
   
   //---------------------------------------------------------------------
   
   private void _listResult(Result r, boolean longList, boolean verylongList,
		   			        boolean printType, int count) 
   {
	   String type = (printType) ? r.getType() : null;
	   
	   if (!longList && !verylongList) 
       {
		   if (printType)
			   this._logger.info(r.getType()+" "+r.getName());
		   else
			   this._logger.info(r.getName());
       } 
       else 
       {
    	   if (printType)
    		   this._logger.info(new PrintfFormat("%5d. ").sprintf(count) +
    				r.getType() + ", "
   	                + new PrintfFormat("%22s, %12ld, %.125s")
   	                      .sprintf(new Object[] {
   	                              this._dateFormatter.formatDate(
   	                                      r.getFileModificationTime()),
   	                              new Long(r.getSize()), r.getName() }));
    	   else
              this._logger.info(new PrintfFormat("%5d. ").sprintf(count)
                + new PrintfFormat("%22s, %12ld, %.125s")
                      .sprintf(new Object[] {
                              this._dateFormatter.formatDate(
                                      r.getFileModificationTime()),
                            new Long(r.getSize()), r.getName() }));
    	   
          if (verylongList) 
          {
             this._logger.info("       "
                   + new PrintfFormat("[Contributor] %s").sprintf(
                         r.getFileContributor()));

             this._logger.info("       "
                   + new PrintfFormat("[Created] %s").sprintf(
                           this._dateFormatter.formatDate(
                                   r.getFileCreationTime())));

             if (r.getChecksum() != null) 
             {
                this._logger.info("       "
                      + new PrintfFormat("[Checksum] %s").sprintf(r
                            .getChecksumStr()));
             }
             if (r.getComment() != null) 
             {
                this._logger.info("       "
                      + new PrintfFormat("[Comment] \"%s\"").sprintf(r
                            .getComment()));
             }
             if (r.getArchiveNote() != null) 
             {
                this._logger.info("       "
                      + new PrintfFormat("[Archive note] \"%s\"")
                            .sprintf(r.getArchiveNote()));
             }
             if (r.getRemoteLocation() != null) 
             {
                this._logger.info("       "
                      + new PrintfFormat("[Remote location] %s")
                            .sprintf(r.getRemoteLocation()));
             }
             this._logger.info("");
          }
       }
   }
   
   
   //---------------------------------------------------------------------

   /**
    * Application method to check user connection to all the file types in each
    * server group defined by the user's local domain file
    * 
    * @return true if success
    * @throws SessionException
    */
   private boolean _check() throws SessionException {

      this._client = new Client(this._domainFile, 
                         System.getProperty(Constants.PROPERTY_SSL_TRUSTSTORE));
      

      //get a list of server groups
      LinkedList groups = this._client.getGroupList();
      boolean success = true;
      
      //for each group, get a list of filetypes
      while (groups.size() != 0) 
      {
         String group = (String) groups.getFirst();
         LinkedList filetypes = this._client.getFileTypeList(group);
         this._argTable.put(CMD.SERVERGROUP, group);
         
        
         //we will need to re-set the login info based on the 
         //current server group
         this._argTable.remove(CMD.USER);
         this._argTable.remove(CMD.PASSWORD);
        
         
         String username, password;
         //get the username and password for current active servergroup
         try {
             username = this._loginReader.getUsername(group);
             password = this._loginReader.getPassword(group);
          } catch (Exception e) {
             this._logger.error(ERROR_TAG + "Unable to locate user login file.");
             ++this._errorCount;
             return false;
          }
         

          //check that we have credentials, if not, then skip group
          if (username == null || password  == null)
          {
              String msg = "No login credentials associated with server group '"
                           + group + "'.  Skipping the server group.";
              this._logger.error(ERROR_TAG + msg);
              ++this._errorCount;
              success = false;
              groups.removeFirst();
              continue;
          }
          else
          {
              this._argTable.put(CMD.USER, username);
              this._argTable.put(CMD.PASSWORD, password);
          }
          
         
          while (filetypes.size() != 0) 
          {
            String filetype = (String) filetypes.getFirst();
            this._argTable.put(CMD.FILETYPE, filetype);
            
            String fullFiletype = FileType.toFullFiletype(group, filetype);
            
            this._logger.info("Trying connection to file type \"" + 
                    fullFiletype + "\"");
            try {
               this._client.login(username, password, group, filetype);
               this._logger.info("OK");
            } catch (SessionException e) {
               this._logger.error(ERROR_TAG + e.getMessage());
               ++this._errorCount;
               success = false;
            }
            filetypes.removeFirst();
         }
         groups.removeFirst();
      }
      return success;
   }

  
   //---------------------------------------------------------------------

   /**
    * Application method to check files available on server and compare to files
    * on the user's local disk
    * 
    * @return true if success
    * @throws SessionException
    */
   private boolean _checkFiles() throws SessionException {
      String[] files = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);
      if (files == null) {
         files = new String[] { "*" };

      }
      boolean longList = false;
      boolean verylongList = false;

      if (this._argTable.get(CMD.LONG) != null) {
         this._client.setVerbose(true);
         longList = true;
      }
      if (this._argTable.get(CMD.VERYLONG) != null) {
         this._client.setVeryVerbose(true);
         verylongList = true;
      }

      String before = (String) this._argTable.get(CMD.BEFORE);
      String after = (String) this._argTable.get(CMD.AFTER);
      String between = (String) this._argTable.get(CMD.BETWEEN);
      String and = (String) this._argTable.get(CMD.AND);
      String format = (String) this._argTable.get(CMD.FORMAT);
      
      this._dateFormatter = new DateTimeFormatter(format);

      int transId;

      try {
         if (before != null) {
            transId = this._client.showBefore(
                            this._dateFormatter.parseDate(before), 
                            files[0]);
         } else if (after != null) {
            transId = this._client.showAfter(
                            this._dateFormatter.parseDate(after),
                            files[0]);
         } else if (between != null && and != null) {
            transId = this._client.showBetween(
                            this._dateFormatter.parseDate(between), 
                            this._dateFormatter.parseDate(and),
                            files[0]);
         } else if (before == null && after == null && between == null
               && and == null) {
            transId = this._client.show(files[0]);
         }
      } catch (ParseException e) {
         throw new SessionException(e.getMessage(), -1);
      }

      int count = 0;
      while (this._client.getTransactionCount() > 0) {
         Result r = this._client.getResult();
         if (r == null) {
            continue;
         }

         if (r.getErrno() == Constants.NO_FILES_MATCH) {
            this._logger.info(r.getMessage());
            continue;
         } else if (r.getErrno() == Constants.OK) {
            count++;
            if (!longList && !verylongList) {
               this._logger.info(new PrintfFormat("%5d. ").sprintf(count)
                     + new PrintfFormat("%.125s").sprintf(r.getName()));
            } else {
               this._logger.info(new PrintfFormat("%5d. ").sprintf(count)
                     + new PrintfFormat("%22s, %12d, %.125s")
                           .sprintf(new Object[] {
                                   this._dateFormatter.formatDate(
                                       r.getFileModificationTime()),
                                 new Long(r.getSize()), r.getName() }));
               if (verylongList) {
                  this._logger.info("       "
                        + new PrintfFormat("[Contributor] %s").sprintf(r
                              .getFileContributor()));

                  this._logger.info("       "
                        + new PrintfFormat("[Created] %s").sprintf(
                                this._dateFormatter.formatDate(
                                        r.getFileCreationTime())));

                  if (r.getChecksum() != null) {
                     this._logger.info("       "
                           + new PrintfFormat("[Checksum] %s").sprintf(r
                                 .getChecksumStr()));
                  }
                  if (r.getComment() != null) {
                     this._logger.info("       "
                           + new PrintfFormat("[Comment] \"%s\"").sprintf(r
                                 .getComment()));
                  }
                  if (r.getArchiveNote() != null) {
                     this._logger.info("       "
                           + new PrintfFormat("[Archive note] \"%s\"")
                                 .sprintf(r.getArchiveNote()));
                  }
               }
            }
            File file = new File(r.getName());
            if (!file.exists() || file.isDirectory()) {
               this._logger.info("       - No such file.");
            }
         } else {
            ++this._errorCount;
         }
      }
      return true;
   }

   //---------------------------------------------------------------------

   private boolean _display() throws SessionException {
      String[] files = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);

      /**
       * if (this._argTable.get(CMD.CRC) != null)
       * this._client.set("computeChecksum", true);
       */

      if (files[0].indexOf('*') != -1) {
         this._logger.error(ERROR_TAG + "Invalid file name \"" + files[0]
               + "\"");
         this._getUsage();
         ++this._errorCount;
         return false;
      }
      this._client.get(files[0], System.out);

      boolean success = true;
      NumberFormat fmt = NumberFormat.getInstance();
      String currentType = "";
      while (this._client.getTransactionCount() > 0) {
         Result result = this._client.getResult();
         if (result == null) {
            this._logger.debug("result is null");
            continue;
         }
         if (result.getErrno() == Constants.NO_FILES_MATCH) {
            this._logger.info("no matching files found");
            this._logger.debug(result.getMessage());
            continue;
         } else if (result.getErrno() == Constants.FILE_NOT_FOUND) {
            this._logger.info("no files found");
            this._logger.debug(result.getMessage());
            continue;
         } else if (result.getErrno() != Constants.OK) {
            this._logger.debug("got an error, output error message...");
            this._logger.error(ERROR_TAG + result.getMessage());
            ++this._errorCount;
            success = false;
            continue;
         } else if (result.getErrno() == Constants.OK
               && result.getName() != null)
            this._logger.error(result.getName());
      }
      return success;
   }

   //---------------------------------------------------------------------

   /**
    * Reference action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails.
    */
   private boolean _reference() throws SessionException {

      String[] files = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);

      String vft = (String) this._argTable.get(CMD.VFT);
      String reference = (String) this._argTable.get(CMD.REFERENCE);
      String filetype = (String) this._argTable.get(CMD.FILETYPE);

      boolean success = true;

      if (files == null || files.length < 1 || vft == null || reference == null
            || filetype == null) {
         ++this._errorCount;
         success = false;
      } else {
         this._client.setReference(vft, reference, filetype, files[0]);
         NumberFormat fmt = NumberFormat.getInstance();
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() != Constants.OK) {
               this._logger.error(ERROR_TAG + result.getMessage());
               ++this._errorCount;
               continue;
            }
            this._logger.info(this._align(fmt, ++this._resultIdx, 6)
                  + ". Reference \"" + result.getRefName()
                  + "\" is added to VFT \"" + result.getVFTName() + "\"");
         }
      }
      return success;
   }

   //---------------------------------------------------------------------

   /**
    * Rename action
    * 
    * @return boolean true if operation is successful, false otherwise
    * @throws SessionException when operation fails.
    */
   private boolean _rename() throws SessionException {

      String[] files = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);

      if (files != null && files.length == 2) {
         this._client.rename(files[0], files[1]);

         NumberFormat fmt = NumberFormat.getInstance();
         boolean success = true;
         String currentType = "";
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() != Constants.OK) {
               this._logger.error(ERROR_TAG + result.getMessage());
               success = false;
               ++this._errorCount;
               this._triggerFileResultError(Constants.RENAMEFILE, result);
               continue;
            }
            if (!currentType.equals(result.getType()) && this._using) {
               currentType = result.getType();
               this._logger.info("\nRenaming in file type \"" + currentType
                     + "\":");
            }
            this._logger.info(this._align(fmt, ++this._resultIdx, 6)
                  + ". Renamed \"" + result.getName() + " to \"" + files[1]
                  + "\".");
            
            
            this._triggerFileResultEvent(Constants.RENAMEFILE, result);
            
         }
      } else {
         this._getUsage();
         ++this._errorCount;
      }
      return false;
   }

   //---------------------------------------------------------------------

   /**
    * Application method to compute and output checksum value of local file
    * 
    * @return true if success
    * @throws SessionException
    */
   private boolean _crc() throws SessionException
    {
        boolean success = true;
        String[] files = (String[]) this._argTable.get(UtilCmdParser.
                                                       KEYWORD_FILES);

        //check for no match
        if (files.length == 0)
        {
            this._logger.info("No files found");
        }
        else  //iterate over files reporting CRC
        {
            String filename;
            for (int i = 0; i < files.length; ++i)
            {
                filename = files[i];
                File file = new File(filename);
                
                if (!file.exists())
                {
                    this._logger.error(ERROR_TAG + "'" +filename + "'" +
                                       " does not exist.");
                    continue;
                }
                else if (file.isDirectory())
                {
                    this._logger.error(ERROR_TAG + "'" +filename + "'" +
                                       " is a directory.");
                    continue;
                }
                
                try {
                    this._logger.info("File:\"" + filename + "\"  Checksum:\""
                            + FileUtil.getStringChecksum(filename) + "\"");
                } catch (IOException e) {
                    this._logger.error(ERROR_TAG + "Unable to process file \""
                            + filename + "\"", e);
                    success = false;
                    ++this._errorCount;
                }
            }
        }
        return success;
    }

   //---------------------------------------------------------------------

   /**
    * Replace action
    * 
    * @return boolean true of operation is successful, false otherwise
    * @throws SessionException when operation fails.
    */
   private boolean _replace() throws SessionException {

      String[] tmpFileList = (String[]) this._argTable
            .get(UtilCmdParser.KEYWORD_FILES);
      if (tmpFileList == null) {
         throw new SessionException("Invalid/missing file name expression.", -1);
      }

      ArrayList filesArray = new ArrayList(tmpFileList.length);

      try {
         for (int i = 0; i < tmpFileList.length; ++i) {
            if (tmpFileList[i].indexOf('*') == -1) {
               filesArray.add(tmpFileList[i]);
            } else {
               String nameExp = tmpFileList[i].replaceAll("\\*", ".*");
               
               final String userDirStr = System.getProperty("user.dir");
               final File userDirFile = new File(userDirStr);
               String[] dirList = null;
               if (userDirFile.isDirectory())
                   dirList = userDirFile.list();
               
               if (dirList != null)
               {
                   for (int j = 0; j < dirList.length; ++j) {
                      if (dirList[j].matches(nameExp)) {
                         filesArray.add(dirList[j]);
                      }
                   }
               }
            }
         }
      } catch (PatternSyntaxException e) {
         throw new SessionException(e.getMessage(), -1);
      }

      filesArray.trimToSize();
      String[] files = new String[filesArray.size()];
      for (int i = 0; i < filesArray.size(); ++i) {
         files[i] = (String) filesArray.get(i);

      }
      if (files != null && files.length > 0) {
         String comment = (String) this._argTable.get(CMD.COMMENT);
         String before = (String) this._argTable.get(CMD.BEFORE);
         String after = (String) this._argTable.get(CMD.AFTER);
         String between = (String) this._argTable.get(CMD.BETWEEN);
         String and = (String) this._argTable.get(CMD.AND);
         String format = (String) this._argTable.get(CMD.FORMAT);
         
         this._dateFormatter = new DateTimeFormatter(format);

         if (this._argTable.get(CMD.AUTODELETE) != null) {
            this._client.set(Client.OPTION_AUTODELETE, true);
         }
         if (this._argTable.get(CMD.CRC) != null) {
            this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
         }
         if (this._argTable.get(CMD.RECEIPT) != null) {
             this._client.set(Client.OPTION_RECEIPT, true);
         }
         if (this._argTable.get(CMD.DIFF) != null) {
             this._client.set(Client.OPTION_DIFF, true);
         }

         //Vector v = null;
         int transId;

         try {
            if (before != null) {
               files = this._filesBefore(
                             this._dateFormatter.parseDate(before),
                             files);
            } else if (after != null) {
               files = this._filesAfter(
                             this._dateFormatter.parseDate(after),
                             files);
            } else if (between != null && and != null) {
               files = this._filesBetween(
                       this._dateFormatter.parseDate(between),
                       this._dateFormatter.parseDate(and), 
                       files);
            }
         } catch (ParseException e) {
            throw new SessionException(e.getMessage(), -1);
         }

         if (files == null || files.length < 1) {
            this._logger.info("No file to be replaced");
            return true;
         } else
            transId = this._client.replace(files, comment);

         boolean success = true;
         NumberFormat fmt = NumberFormat.getInstance();
         String currentType = "";
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() == Constants.FILE_EXISTS) 
            {
                currentType = result.getType();
                this._logger.info("Skipping identical file " + result.getName()+".");     
                this._triggerFileResultError(Constants.REPLACEFILE, result);
                continue;
            }
            else if (result.getErrno() != Constants.OK) 
            {
               this._logger.error(ERROR_TAG + result.getMessage());
               ++this._errorCount;
               success = false;
               this._triggerFileResultError(Constants.REPLACEFILE, result);
               continue;
            }
            if (!currentType.equals(result.getType()) && this._using) {
               currentType = result.getType();
               this._logger.info("\nReplacing file type \"" + currentType
                     + "\":");
            }
            String msg = this._align(fmt, ++this._resultIdx, 6) +
                            ". Replaced \"" + result.getName() + "\".";
            if (result.getChecksumStr() != null)
                msg += "  Checksum: " + result.getChecksumStr() + ".";
            if (result.getReceiptId() != Constants.NOT_SET)
                msg += "  Receipt Id: " + result.getReceiptId() + ".";            
            this._logger.info(msg);
            
            this._triggerFileResultEvent(Constants.REPLACEFILE, result);
            
            //this._logger.info(this._align(fmt, ++this._resultIdx, 6)
            //      + ". Replaced \"" + result.getName() + "\".");            
         }
         return success;
      }
      return false;

   }

   //---------------------------------------------------------------------

   /**
    * Show file types
    * 
    * @throws SessionException
    */
   private boolean _showTypes() throws SessionException {
      Domain domain;
      DomainFactoryIF factory = new DomainFactory();
      String defaultGroup = null;

      this._logger.debug("Domain file to parse: " + this._domainFile);
      String key;
      for (Iterator it = this._argTable.keySet().iterator(); it.hasNext();) {
         key = (String) it.next();
         this._logger.debug("Argument: " + key + "=" + this._argTable.get(key));
      }

      // Parse the domain file and get a domain object

      domain = factory.getDomain(this._domainFile);
      
      //  Check if a query for server groups was issued.
      //  
      Boolean queryGroups = (Boolean) this._argTable.get(CMD.SERVERGROUPS);
      if (queryGroups != null) {
         _printServerGroups(domain.getGroupNames(), domain.getDefaultGroup());
         return true;
      }

      String serverGroup = (String) this._argTable.get(CMD.SERVERGROUP);
      String fileTypeExpression = (String) this._argTable.get(CMD.FILETYPE);

      boolean foundTypes = false;
      Map fileTypesToPrint = new TreeMap();

      //  Check if servergroup argument was specified
      if (serverGroup != null) {
         if (fileTypeExpression != null
               && !fileTypeExpression.trim().equals("")) {
            //  Condition where both servergroup
            //  and filetype expression were specified
            List list = domain.getFileTypeNames(serverGroup);
            LinkedList listToPrint = new LinkedList();
            for (ListIterator lit = list.listIterator(); lit.hasNext();) {
               String fileType = (String) lit.next();
               if (ExpressionEvaluator.isMatch(fileTypeExpression, fileType))
                  listToPrint.add(fileType);
            }
            if (listToPrint.size() > 0) {
               //this._printFileTypes(listToPrint,serverGroup);
               fileTypesToPrint.put(serverGroup, listToPrint);
               foundTypes = true;
            }
         } else {
            //  Condition where only servergroup
            //  was specified
            LinkedList listToPrint = domain.getFileTypeNames(serverGroup);
            if (listToPrint.size() > 0) {
               fileTypesToPrint.put(serverGroup, listToPrint);
               //this._printFileTypes(listToPrint,serverGroup);
               foundTypes = true;
            }
         }
         //  Check if filetype expression argument was specified
      } else if (fileTypeExpression != null
            && !fileTypeExpression.trim().equals("")) {
         //  Condition where only a filetype expression
         //  was specified.
         List groupNames = domain.getGroupNames();
         String group = null;
         LinkedList listToPrint = null;
         for (ListIterator lit = groupNames.listIterator(); lit.hasNext();) {
            group = (String) lit.next();
            List fileTypes = domain.getFileTypeNames(group);
            String fileType = null;
            listToPrint = new LinkedList();
            for (ListIterator lit2 = fileTypes.listIterator(); lit2.hasNext();) {
               fileType = (String) lit2.next();
               if (ExpressionEvaluator.isMatch(fileTypeExpression, fileType))
                  listToPrint.add(fileType);
            }
            if (listToPrint.size() > 0) {
               foundTypes = true;
               //this._printFileTypes(listToPrint,group);
               fileTypesToPrint.put(group, listToPrint);
            }
         }
      } else {

         //  If no argument has been given, show all file types
         List groupNames = domain.getGroupNames();
         String group = null;
         for (ListIterator lit = groupNames.listIterator(); lit.hasNext();) {
            group = (String) lit.next();
            LinkedList fileTypes = domain.getFileTypeNames(group);
            if (fileTypes.size() > 0) {
               //this._printFileTypes(fileTypes,group);
               fileTypesToPrint.put(group, fileTypes);
               foundTypes = true;
            }
         }
      }
      if (foundTypes) {
         this._printFileTypes(fileTypesToPrint, domain.getDefaultGroup());
      } else
         this._logger.info("No file types found that match criteria.");

      return foundTypes;
   }

   //---------------------------------------------------------------------

   /**
    *  
    */
   private void _printServerGroups(List serverGroups, String defaultGroup) {
      //this._logger.info(Constants.COPYRIGHT);
      //this._logger.info(Constants.APIVERSIONSTR + "\n");
      //this._logger.info("Server groups in \""
      //        + new File(this._domainFile).getName() + "\"");
      if (defaultGroup == null)
          defaultGroup = "";
      
      this._logger.info("Domain file: "+this._domainFile);

      for (ListIterator lit = serverGroups.listIterator(); lit.hasNext();)
      {
          String group = lit.next().toString();
          String line = "  " + group;
          if (group.equals(defaultGroup))
          {
              line = line + " (default)";
          }
          this._logger.info(line);
      }
   }

   /**
    * Convenience method to print the file types stored in a map for which the
    * key is the servergroup and the value is a list of filetypes associated
    * with the servergroup.
    * 
    * @param fileTypes the map of servergroup to filetype names to be printed.
    */
   private void _printFileTypes(Map fileTypes, String defaultGroup) {
      //  if printing fileTypes for only 1 server group
      //  there is no need to qualify the filetypes.
       
      if (defaultGroup == null)
          defaultGroup = "";
      boolean qualifyFileTypes = !(fileTypes.size() == 1);
      Set keys = fileTypes.keySet();
      String key = null;
      int maxFileTypeLength = 0;
      String fileType = null, fileType2 = null;

      this._logger.info("Domain file: "+this._domainFile);
      
      /*
      this._logger.info(Constants.COPYRIGHT);
      this._logger.info(Constants.APIVERSIONSTR + "\n");
      this._logger.info("File types in \""
            + new File(this._domainFile).getName()
            + "\""
            + (qualifyFileTypes ? "" : " belonging to server group \""
                  + keys.iterator().next() + "\""));
      */
      
      // Check if "classic" display is requested
      if (this._argTable.get(CMD.CLASSIC) != null) {
         List listToPrint = new LinkedList();

         //  First find the longest servergroup:filetype string.
         //  This is necessary for proper formatting.
         //  Then, store each servergroup:filetype string in a list
         for (Iterator it = keys.iterator(); it.hasNext();) {
            key = (String) it.next();
            for (ListIterator lit = ((List) fileTypes.get(key)).listIterator(); lit
                  .hasNext();) {
               fileType = (String) lit.next();
               maxFileTypeLength = Math.max(maxFileTypeLength, key.length()
                     + fileType.length());
               listToPrint.add((qualifyFileTypes ? key + ":" : "") + fileType);
            }
         }

         PrintfFormat col1Format = new PrintfFormat("%-"
               + (maxFileTypeLength + 4) + "s%s");
         for (ListIterator lit = listToPrint.listIterator(); lit.hasNext();) {
            fileType = (String) lit.next();
            if (lit.hasNext()) {
               fileType2 = (String) lit.next();
               this._logger.info(col1Format.sprintf(new Object[] { fileType,
                     fileType2 }));
            } else
               this._logger.info(fileType);
         }
      } else {
         for (Iterator it = keys.iterator(); it.hasNext();) {
            key = (String) it.next();
            String line = "Server Group: " + key;
            if (key.equals(defaultGroup))
                line = line + " (default)";            
            this._logger.info(line);
            for (ListIterator lit = ((List) fileTypes.get(key)).listIterator(); lit
                  .hasNext();) {
               this._logger.info("   " + lit.next());

            }
         }
      }
      //       this._logger.info("File types in group: "+group);
      //       if (list.size() > 0) {
      //           for (ListIterator lit = list.listIterator(); lit.hasNext();) {
      //               this._logger.info(" "+lit.next());
      //           }
      //       } else
      //           this._logger.info(" No file types found that match criteria.");
   }

   //---------------------------------------------------------------------

   /**
    * Register action
    * 
    * @return boolean true of operation is successful, false otherwise
    * @throws SessionException when operation fails.
    */
   private boolean _register() throws SessionException {

      String[] tmpFileList = (String[]) this._argTable.get(
                                        UtilCmdParser.KEYWORD_FILES);
      if (tmpFileList == null) {
         throw new SessionException("Invalid/missing file name expression.", -1);
      }

      ArrayList filesArray = new ArrayList(tmpFileList.length);

      try {
         for (int i = 0; i < tmpFileList.length; ++i) {
            if (tmpFileList[i].indexOf('*') == -1) {
               filesArray.add(new File(tmpFileList[i]).getAbsolutePath());
            } else {
               String nameExp = tmpFileList[i].replaceAll("\\*", ".*");               
               //File[] dirList = new File(System.getProperty("user.dir")).listFiles();
               
               String userDirStr = System.getProperty("user.dir");
               File userDirFile = new File(userDirStr);
               File[] dirList = null;
               if (userDirFile.isDirectory())
                   dirList = userDirFile.listFiles();
               
               if (dirList != null)
               {
                   for (int j = 0; j < dirList.length; ++j) {
                      if (dirList[j].getName().matches(nameExp)) {
                         filesArray.add(dirList[j].getAbsolutePath());
                      }
                   }
               }
            }
         }
      } catch (PatternSyntaxException e) {
         throw new SessionException(e.getMessage(), -1);
      }

      String[] files = new String[0];
      files = (String[]) filesArray.toArray(files); 

      if (files == null || files.length == 0)
          this._logger.info("No file to be registered");
      else
      {
         String comment = (String) this._argTable.get(CMD.COMMENT);
         String before = (String) this._argTable.get(CMD.BEFORE);
         String after = (String) this._argTable.get(CMD.AFTER);
         String between = (String) this._argTable.get(CMD.BETWEEN);
         String and = (String) this._argTable.get(CMD.AND);
         String format = (String) this._argTable.get(CMD.FORMAT);
         
         this._dateFormatter = new DateTimeFormatter(format);

         boolean replace = (this._argTable.get(CMD.REPLACE) != null);
         boolean force   = (this._argTable.get(CMD.FORCE) != null);
         boolean receipt = (this._argTable.get(CMD.RECEIPT) != null);
         
         if (receipt)
             this._client.set(Client.OPTION_RECEIPT, true);
         
         //Vector v = null;
         int transId;

         try {
            if (before != null) {
               files = this._filesBefore(
                             this._dateFormatter.parseDate(before),
                             files);
            } else if (after != null) {
               files = this._filesAfter(
                             this._dateFormatter.parseDate(after),
                             files);
            } else if (between != null && and != null) {
               files = this._filesBetween(
                       this._dateFormatter.parseDate(between),
                       this._dateFormatter.parseDate(and), 
                       files);
            }
         } catch (ParseException e) {
            throw new SessionException(e.getMessage(), -1);
         }

         if (files == null || files.length < 1) {
            this._logger.info("No file to be registered");
            return true;
         } else
            transId = this._client.register(files, replace, force, comment);

         boolean success = true;
         NumberFormat fmt = NumberFormat.getInstance();
         String currentType = "";
         while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() != Constants.OK) {
               this._logger.error(ERROR_TAG + result.getMessage());
               ++this._errorCount;
               success = false;
               this._triggerFileResultError(Constants.REGISTERFILE, result);
               continue;
            }
            if (!currentType.equals(result.getType()) && this._using) {
               currentType = result.getType();
               this._logger.info("\nRegistering file type \"" + currentType
                     + "\":");
            }
            String msg = this._align(fmt, ++this._resultIdx, 6) +
                            ". Registered \"" + result.getName() + "\".";
            if (result.getChecksumStr() != null)
                msg += "  Checksum: " + result.getChecksumStr() + ".";
            if (result.getReceiptId() != Constants.NOT_SET)
                msg += "  Receipt Id: " + result.getReceiptId() + ".";    
            
            
//            //message will contain checksum and receit id if included
//            String msg = this._align(fmt, ++this._resultIdx, 6) +
//                         ". " + result.getMessage() + "\".";
            
            this._logger.info(msg);
            this._triggerFileResultEvent(Constants.REGISTERFILE, result);
            
            //this._logger.info(this._align(fmt, ++this._resultIdx, 6)
            //      + ". Replaced \"" + result.getName() + "\".");            
         }
         return success;
      }
      return false;

   }

   //---------------------------------------------------------------------
   
   /**
    * Unregister action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails
    */
   private boolean _unregister() throws SessionException {

      String[] files = (String[]) this._argTable.get(UtilCmdParser.KEYWORD_FILES);
      
      boolean success = true;

      if (files != null && files.length > 0) 
      {
         for (int i = 0; i < files.length; ++i) 
         {
            this._client.unregister(files[i]);
         }

         String currentType = "";
         NumberFormat fmt = NumberFormat.getInstance();
         while (this._client.getTransactionCount() > 0) 
         {
            Result result = this._client.getResult();
            if (result == null) 
            {
               continue;
            }
            if (result.getErrno() != Constants.OK) 
            {
               this._logger.error(result.getMessage());
               ++this._errorCount;
               this._triggerFileResultError(Constants.UNREGISTERFILE, result);
               continue;
            }
            if (!currentType.equals(result.getType()) && this._using) 
            {
               currentType = result.getType();
               this._logger.info("\nUnregistering from file type \"" + currentType
                     + "\":");
            }
            this._logger.info(this._align(fmt, ++this._resultIdx, 6)
                  + ". Unregistered \"" + result.getName() + "\".");
            this._triggerFileResultEvent(Constants.UNREGISTERFILE, result);
         }
      }
      return success;
   }
      
   //---------------------------------------------------------------------
   
   /**
    * Lock filetype action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails    
    */
   
   private boolean _lockType() throws SessionException 
   {
       
       String mode = null;
       
       boolean owner = this._argTable.containsKey(CMD.OWNER) &&
                      ((Boolean) this._argTable.get(CMD.OWNER)).booleanValue();
       boolean group = this._argTable.containsKey(CMD.GROUP) &&
                      ((Boolean) this._argTable.get(CMD.GROUP)).booleanValue();
       
       if (owner && group) 
       {
           this._logger.info(this._getUsage());
           
           ++this._errorCount;
           return false;
       }
       else if (owner)
       {
           mode = CMD.OWNER;
       }
       else if (group)
       {
           mode = CMD.GROUP;
       }
       
       //--------------------------
       
       int transId = this._client.lockType(mode);          

       while (this._client.getTransactionCount() > 0) 
       {
          Result r = this._client.getResult();
          if (r == null) 
          {
             continue;
          }
          
          String fullFt = FileType.toFullFiletype(r.getServerGroup(), r.getType());
       
          if (r.getErrno() == Constants.OK) 
          {
              this._logger.info("Filetype '"+fullFt+"' locked.");              
          } 
          else 
          {
              this._logger.error(ERROR_TAG + "Unable to lock filetype " + fullFt + ".");
              this._logger.error(ERROR_TAG + r.getMessage());   
                                    
             ++this._errorCount;
                    
             continue;
          }
       }
       return true;
    }
      
   //---------------------------------------------------------------------
   

   /**
    * Unlock filetype action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails    
    */
   
   private boolean _unlockType() throws SessionException 
   {
       
       String mode = null;
       
       boolean owner = this._argTable.containsKey(CMD.OWNER) &&
                      ((Boolean) this._argTable.get(CMD.OWNER)).booleanValue();
       boolean group = this._argTable.containsKey(CMD.GROUP) &&
                      ((Boolean) this._argTable.get(CMD.GROUP)).booleanValue();
       
       if (owner && group) 
       {
           this._logger.info(this._getUsage());
           
           ++this._errorCount;
           return false;
       }
       else if (owner)
       {
           mode = CMD.OWNER;
       }
       else if (group)
       {
           mode = CMD.GROUP;
       }
       
       //--------------------------
       
       int transId = this._client.unlockType(mode);          

       while (this._client.getTransactionCount() > 0) 
       {
          Result r = this._client.getResult();
          if (r == null) {
             continue;
          }
          
          String fullFt = FileType.toFullFiletype(r.getServerGroup(), r.getType());
       
          if (r.getErrno() == Constants.OK) 
          {
              this._logger.info("Filetype '"+fullFt+"' unlocked.");              
          } 
          else 
          {
              this._logger.error(ERROR_TAG + "Unable to unlock filetype " + fullFt + ".");
              this._logger.error(ERROR_TAG + r.getMessage());   
                                    
             ++this._errorCount;
                    
             continue;
          }
       }
       return true;
    }
   
   //---------------------------------------------------------------------
   

   /**
    * Change password action
    * 
    * @return boolean true when operation is successful, false otherwise
    * @throws SessionException when operation fails    
    */
   
   private boolean _changePassword() throws SessionException 
   {
       
       String mode = null;
       
       
       String oldPassword = null;
       String newPassword = null;
       String verify = null;
       String user = null;
       boolean interactive = false;
       
       
       if (this._argTable.get(CMD.USER) == null ||
           this._argTable.get(CMD.PASSWORD) == null) 
       {
           try {
               this._getLoginInfo();
           } catch (FileNotFoundException fnf) {
               throw new SessionException(
                       "Please acquire credentials with login utility.", -1);
           } catch (IOException io) {
               throw new SessionException("Could not read login credentials.", -1);
           }

           // Check again to see if we got login data
           if (this._argTable.get(CMD.USER) == null || 
               this._argTable.get(CMD.PASSWORD) == null) 
           {
               throw new SessionException("Missing user login information.", -1);
           }
       }
       
       user = (String) this._argTable.get(CMD.USER);
       oldPassword = (String) this._argTable.get(CMD.PASSWORD);
       this._logger.info("Changing password for user '"+user+"'");
       
       try {
          
           boolean ok = false;
           while (!ok) 
           {
               newPassword = ConsolePassword.getPassword(
                               "Enter new password (or type \"abort\" to quit)>> ");
               if (newPassword.equalsIgnoreCase("abort"))
               {
                   return false;
               } 
               if (newPassword.equals(""))
               {
                   this._logger.error(ERROR_TAG + "Password cannot be empty, try again");
                   continue;                  
               }
               
               verify = ConsolePassword.getPassword("Re-enter new password>> ");
               if (newPassword.equals(verify))
                   ok = true;
               else
               {
                   this._logger.error(ERROR_TAG + "Password does not match, try again");   
               }               
           }
       } catch (IOException ioEx) {
           throw new SessionException("PasswIO error occurred during password entry: "+
                                      ioEx.getMessage(), Constants.IO_ERROR);
       }
           
       String encrOldPassword = null, encrNewPassword = null;
       
       try {
           encrOldPassword = oldPassword; //old password should already by encrypted at this point
           encrNewPassword = this._encrypter.encrypt(newPassword);
       } catch (Exception ex) {
           throw new SessionException("Error occurred while encrypting passwords.", Constants.EXCEPTION);
       }
       
       /* Now, change the password for our Komodo user. */
       this._client.changePassword(encrOldPassword, encrNewPassword);
       
       while (this._client.getTransactionCount() > 0) 
       {
           Result r = this._client.getResult();
           if (r == null) 
           {
               continue;
           }  
          
           if (r.getErrno() == Constants.OK) 
           {
               this._logger.info("Password for user '"+user+"' changed.");              
           } 
           else 
           {
               this._logger.error(ERROR_TAG + "Unable to change password for '" + user + "'");
               this._logger.error(ERROR_TAG + r.getMessage());   
                                       
               ++this._errorCount;
                       
               continue;
           }
       }
              
       return true;
    }
   
   //---------------------------------------------------------------------

   /**
    * Check configuration
    * @param checkFiletype Indicates we 
    * @throws SessionException when unable to obtain user login information.
    */
   
   private void _checkConfig(boolean checkFiletype) throws SessionException {



      //---------------------------
      
    
      if (this._domainFile == null) {
         throw new SessionException("Missing domain file.", -1);
      }
      
      //---------------------------
      
      _checkUserCredentials();
      
      
      //---------------------------
      
      if (this._argTable.get(CMD.SERVERGROUP) == null) {
          String defaultGroup = this._getServerGroup(true);
          if (defaultGroup != null)
              this._argTable.put(CMD.SERVERGROUP, defaultGroup);
      }
      
      //---------------------------
      
      if (checkFiletype)
      {
          _checkFiletype();           
      }
      
      //---------------------------
   }

   private void _checkFiletype() throws SessionException {

       if (this._argTable.get(CMD.FILETYPE) == null) {
           
           //if QUERY set, then missing filetype is ok, else error
           if (this._argTable.get(CMD.QUERY) != null)
               ; //do nothing
           else if (this._actionId.equals(Constants.CHANGEPASSWORD))
               ; //do nothing
           else
               throw new SessionException("Missing file type.", -1);
       }
       
       //---------------------------
    }
   
   //---------------------------------------------------------------------
   
   private void _checkUserCredentials() throws SessionException
   {
       if (this._argTable.get(CMD.USER) == null ||
           this._argTable.get(CMD.PASSWORD) == null) 
       {
          try {
             this._getLoginInfo();
          } catch (FileNotFoundException fnf) {
             throw new SessionException(
                   "Please acquire credentials with login utility.", -1);
          } catch (IOException io) {
             throw new SessionException("Could not read login credentials.", -1);
          }

          // Check again to see if we got login data
          if (this._argTable.get(CMD.USER) == null ||
              this._argTable.get(CMD.PASSWORD) == null) {
             throw new SessionException("Missing user login information.", -1);
          }
       }
   }
   
   //---------------------------------------------------------------------

   /**
    * Creates the usage statement for the various utility commands
    * 
    * @return Usage statement for command
    */
   private String _getUsage() {

      StringBuffer buf = new StringBuffer();
      
      // Add FEI version number string and copyright statement
      buf.append(Constants.COPYRIGHT + "\n");
      buf.append(Constants.CLIENTVERSIONSTR + "\n");
      buf.append(Constants.APIVERSIONSTR + "\n\n");

      // All usuage statements begin with Usage: command name
      buf.append("Usage:\t" + this._userScript + " ");

      if (this._actionId.equals(Constants.CREDLOGIN))
      {
          buf.append("[<user name> [<server group>] ] [help]");
      }
      else if (this._actionId.equals(Constants.CREDLOGOUT))
      {
          buf.append("[<server group>] [help]");
      }
      else if (this._actionId.equals(Constants.CREDLIST))
      {
          buf.append("[help]");
      }
      else if (this._actionId.equals(Constants.ADDFILE))
      {
          buf.append("[<server group>:]<file type> <file name expression>\n")
          .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
          .append("\t[format \'<date format>\'] [comment \'<comment text>\'] [crc] [receipt]\n")
          .append("\t[autodelete] [filehandler] [help]\n\n")
          .append("Usage:\t").append(this._userScript)
          .append(" using <option file>\n");
          buf.append("Option File Format (per line):\n")
          .append("\t[<server group>:]<file type> <file name>...\n")
          .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
          .append("\t[format \'<date format>\'] [comment \'<comment text>\'] \n")
          .append("\t[autodelete] [crc] [receipt]");
      }
      else if (this._actionId.equals(Constants.REPLACEFILE)) 
      {
          buf.append("[<server group>:]<file type> <file name expression>\n")
          .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
          .append("\t[format \'<date format>\'] [comment \'<comment text>\'] [crc] [receipt]\n")
          .append("\t[autodelete] [diff] [filehandler] [help]\n\n")
          .append("Usage:\t").append(this._userScript)
          .append(" using <option file>\n");
          buf.append("Option File Format (per line):\n")
          .append("\t[<server group>:]<file type> <file name>...\n")
          .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
          .append("\t[format \'<date format>\'] [comment \'<comment text>\'] \n")
          .append("\t[autodelete] [crc] [receipt] [diff]");
      }
      else if (this._actionId.equals(Constants.GETFILES))
      {
          buf.append("[<server group>:]<file type> [\'<file name expression>\'] \n")
          .append("\t[output <path>] {[before|after <datetime>] | \n")
          .append("\t[between <datetime1> and <datetime2>]} \n")
          .append("\t[format \'<date format>\'] [crc] [saferead] [receipt] \n")
          .append("\t{[replace|version]} [diff] [query <queryfile>] [replicate]\n")
          .append("\t[replicateroot <rootdir>] [filehandler] [help]\n\n")
          .append("Usage:\t").append(this._userScript)
          .append(" using <option file>\n");
           buf.append("Option File Format (per line):\n")
          .append("\t[<server group>:]<file type> [\'<file name expression>\']\n")
          .append("\t[output <path>] {[before|after <date-time>] | \n")
          .append("\t[between <date-time1> and <date-time2>]} \n")
          .append("\t[format \'<date format>\'] [crc] [saferead] [receipt] \n")
          .append("\t{[replace|version]} [diff]");
      }
      else if (this._actionId.equals(Constants.AUTOGETFILES))
      {
          buf.append("[<server group:]<file type> \n")
          .append("\t[output <path>] [restart] [using <option file>] {[pull|push]} \n")
          .append("\t{[replace|version]} [format \'<date format>\']  [query <queryfile>]\n")
          .append("\t[replicate] [replicateroot <rootdir>] [filehandler] [diff] [help]\n");
          buf.append("Option File Format:\n")
          .append("\tcrc\n")
          .append("\tdiff\n")
          .append("\tinvoke              <command>\n")
          .append("\tinvokeExitOnError\n")
          .append("\tinvokeAsync\n")
          .append("\tlogFile             <file name>\n")
          .append("\tlogFileRolling      {monthly|weekly|daily|hourly|minutely|halfdaily}\n")
          .append("\tmailMessageFrom     <email address>\n")
          .append("\tmailMessageTo       <email address, email address, ...>\n")
          .append("\tmailReportAt        <hh:mm am|pm, hh:mm am|pm, ...>\n")
          .append("\tmailReportTo        <email address, email address, ...>\n")
          .append("\tmailSMTPHost        <host name>\n")
          .append("\tmailSilentReconnect\n")
          .append("\treceipt\n").append("\treplace\n")
          .append("\tsaferead\n").append("\tversion");
      }
      else if (this._actionId.equals(Constants.AUTOSHOWFILES))
      {
          buf.append("[<server group:]<file type> \n")
          .append("\t[output <path>] [restart] [using <option file>] {[pull|push]}\n")
          .append("\t[format \'<date format>\']  [query <queryfile>] [filehandler]\n")
          .append("\t[help]\n");
          buf.append("Option File Format:\n")
          .append("\tinvoke              <command>\n")
          .append("\tinvokeExitOnError\n")
          .append("\tinvokeAsync\n")
          .append("\tlogFile             <file name>\n")
          .append("\tlogFileRolling      {monthly|weekly|daily|hourly|minutely|halfdaily}\n")
          .append("\tmailMessageFrom     <email address>\n")
          .append("\tmailMessageTo       <email address, email address, ...>\n")
          .append("\tmailReportAt        <hh:mm am|pm, hh:mm am|pm, ...>\n")
          .append("\tmailReportTo        <email address, email address, ...>\n")
          .append("\tmailSMTPHost        <host name>\n")
          .append("\tmailSilentReconnect\n");
      }
      else if (this._actionId.equals(Constants.MAKECLEAN))
      {
          buf.append("[<server group>:]<file type> [help]");
      }
      else if (this._actionId.equals(Constants.DELETEFILE))
      {
          buf.append("[<server group>:]<file type> \'<file name expression>\' \n")
          .append("\t[filehandler] [help]\n\n")
          .append("Usage:\t").append(this._userScript)
          .append(" using <option file>\n");
          buf.append("Option File Format (per line):\n")
          .append("\t[<server group>:]<file type> \'<file name expression>\'");
      }
      else if (this._actionId.equals(Constants.SHOWFILES))
      {
          buf.append("[<server group>:]<file type> [\'<file name expression>\'] \n")
          .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
          .append("\t[format \'<date format>\'] {[long | verylong]} \n")
          .append("\t[query <queryfile>] [filehandler] [help]");
      }
      else if (this._actionId.equals(Constants.SETREFERENCE))
      {
          buf.append("[<server group>:]<file type> <file name> \n")
          .append("\tvft <VFT name> reference <ref name> [help]");
      }
      else if (this._actionId.equals(Constants.RENAMEFILE))
      {
          buf.append("[<server group>:]<file type> <old file name> <new file name>\n")
          .append("\t[filehandler] [help]\n\n")
          .append("Usage:\t")
          .append(this._userScript)
          .append(" using <option file>\n")
          .append("Option File Format (per line):\n")
          .append("\t[<server group>:]<file type> <old file name> <new file name>");
      }
      else if (this._actionId.equals(Constants.COMMENTFILE))
      {
          buf.append("[<server group>:]<file type> <file name> comment \n")
          .append("\t\'<comment text>\' [filehandler] [help]");
      }
      else if (this._actionId.equals(Constants.COMPUTECHECKSUM))
      {
          buf.append("<file name expression> [help]");
      }
      else if (this._actionId.equals(Constants.CHECKFILES))
      {
          buf.append("[<server group>:]<file type> [\'<file name expression>\']\n")
          .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
          .append("\t[format \'<date format>\'] {[long | verylong]} [help]\n\n")
          .append("Usage:\t")
          .append(this._userScript + " using <option file>\n");
          buf.append("Option File Format (per line):\n")
          .append("\t[<server group>:]<file type> \'<file name expression>\'\n")
          .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
          .append("\t[format \'<date format>\'] {[long | verylong]}");
      }
      else if (this._actionId.equals(Constants.DISPLAY))
      {
          buf.append("[<server group>:]<file type> <file name> [help]");
      }
      else if (this._actionId.equals(Constants.ACCEPT))
      {
          buf.append("[<server group>:]<file type> for <add|replace|get|delete>\n")
          .append("\t[output <path>] [crc] [saferead] [autodelete] {[replace|version]}\n")
          .append("\t[diff] [help]");
      }
      else if (this._actionId.equals(Constants.SHOWTYPES))
      {
          buf.append("{\'[<server group>:][<file type expression>]\'|[srvgroups]}\n")
          .append("\t[classic] [help]\n");
      }
      else if (this._actionId.equals(Constants.CHECK))
      {
          buf.append("[help]");
      }
      else if (this._actionId.equals(Constants.REGISTERFILE))
      {
          buf.append("[<server group>:]<file type> <file name expression>\n")
          .append("\t[replace] [force] [comment \'<comment text>\']\n")
         .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
         .append("\t[format \'<date format>\'] [receipt] [filehandler] [help]\n\n")
         .append("Usage:\t").append(this._userScript)
         .append(" using <option file>\n");
         buf.append("Option File Format (per line):\n")
         .append("\t[<server group>:]<file type> <file name>... [replace] [force]\n")
         .append("\t{[before|after <date-time>] | [between <date-time1> and <date-time2>]}\n")
         .append("\t[format \'<date format>\'] [comment \'<comment text>\'] [receipt]");
      }
      else if (this._actionId.equals(Constants.UNREGISTERFILE))
      {
          buf.append("[<server group>:]<file type> \'<file name expression>\' \n")
          .append("\t[filehandler] [help]\n\n")
          .append("Usage:\t").append(this._userScript)
          .append(" using <option file>\n");
          buf.append("Option File Format (per line):\n")
          .append("\t[<server group>:]<file type> \'<file name expression>\'");
      }
      else if (this._actionId.equals(Constants.LOCKFILETYPE))
      {
          buf.append("[<server group>:]<file type> [owner | group] [help]\n"); 
      }
      else if (this._actionId.equals(Constants.UNLOCKFILETYPE))
      {
          buf.append("[<server group>:]<file type> [owner | group] [help]\n");   
      }
      else if (this._actionId.equals(Constants.CHANGEPASSWORD))
      {
          buf.append("<server group> [help]\n"); 
      }
      else
      {
      }
     
  
      
      return buf.toString();
   }

   //---------------------------------------------------------------------

   /**
    * Gets input from the command line Note: must look for '\r' for it to work
    * properly in DOS
    * 
    * @return the TTY input string
    */
   protected final String _readTTYLine() {

      StringBuffer buf = new StringBuffer(80);
      int c = 0;
      try {
         while ((c = System.in.read()) != -1) {
            char ch = (char) c;
            if (ch == '\r') {
               ch = (char) System.in.read();
               if (ch == '\n') {
                  break;
               } else {
                  continue;
               }
            } else if (ch == '\n') {
               break; // Unix flavors.
            }
            buf.append(ch);
         }
      } catch (IOException e) {
         this._logger.error(ERROR_TAG + e.getMessage());
      }
      return buf.toString();
   }

   //---------------------------------------------------------------------

   /**
    * Replaces all instances of token with repl in string,
    * 
    * @param str Original string
    * @param token String to be replaced
    * @param repl Replacement string
    * @return String with replacements
    */

   private final String _strReplace(String str, String token, String repl) {
      StringBuffer sb = new StringBuffer(str);
      int idx = sb.indexOf(token);
      while (idx > -1) {
         sb.replace(idx, idx + token.length(), repl);
         idx = sb.indexOf(token);
      }
      return sb.toString();
   }

   //---------------------------------------------------------------------
   
   protected String _getServerGroup(boolean useDefault)
   {
       String sg = (String) this._argTable.get(CMD.SERVERGROUP);
       if (sg == null && useDefault)
       {
           if (this._client != null)
               sg = this._client.getDefaultGroup();
           
           if (sg == null)
           {
               DomainFactoryIF factory = new DomainFactory();
               try {
                   Domain tmpDomain = factory.getDomain(this._domainFile);
                   sg = tmpDomain.getDefaultGroup();
               } catch (Exception ex) {
                   sg = null;
                   this._logger.error("Error occurred while creating " +
                   		              "temporary domain: "+ex.getMessage());
                   this._logger.trace(null, ex);
               }
           }
       }
       
       return sg;
   }
   
   //---------------------------------------------------------------------

   protected class ScheduledMailer extends TimerTask {
      public void run() {

         String message = null;
         synchronized (UtilClient.this._reportmessage) {             
            message = UtilClient.this._reportmessage.toString();            
            UtilClient.this._reportmessage = new StringBuffer();            
         }
         
         if (message == null)
         {
            return;
         }
         if (message.trim().length() == 0)
         {             
            return;
         }

         message = "[SUBSCRIPTION REPORT]" + "\n" + message;                
         UtilClient.this._emailReportLogger.info(message);         
      }
   }

   //---------------------------------------------------------------------
   //---------------------------------------------------------------------

   /**
    * Shutdown hook implementation to logout of clients prior
    * to JVM exiting.
    */
   
   class ShutDownHandler extends Thread
   {
       public ShutDownHandler()
       {
           _logger.debug("Instantiated shutdown handler.");
       }
              
       //------------------------------------------------------------------
       
       public void run() {

           _logger.debug("Invoking shutdown handler...");
           
           //kill subscription client connection
           if (_subscriptionClient != null)
           {
               _subscriptionClient.close();    
               _logger.debug("Subscription client closed.");
           }
           
           //----------------------
           
           //kill client connection
           if (_client != null)
           {
               try {
                   _client.logoutImmediate();
                   _logger.debug("Client connection closed.");
               } catch (SessionException sesEx) {
                   _logger.error("Session exception occurred while " +
                                 "attepting final client logout.  Message: " +
                                 sesEx.getMessage());
               }               
           }
           
           //----------------------
       }
       
       //------------------------------------------------------------------
   }
   
   //---------------------------------------------------------------------
   //---------------------------------------------------------------------

   /**
    * Main method, accepts string array object which contains the command line
    * arguments.
    * 
    * @param args The command line arguments
    */
   public static void main(String[] args) {
      UtilClient client = new UtilClient(args);
      client.run();

      // exit the program with the number of errors as the exit status
      System.exit(client.getErrorCount());
   }

   //---------------------------------------------------------------------

}
