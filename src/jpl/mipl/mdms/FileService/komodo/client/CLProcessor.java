/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.io.BoundedBufferedReader;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.Session;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.help.ClientHelp;
import jpl.mipl.mdms.FileService.komodo.util.AuthenticationType;
import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;
import jpl.mipl.mdms.FileService.komodo.util.InvocationCommandUtil;
import jpl.mipl.mdms.FileService.komodo.util.LoginFile;
import jpl.mipl.mdms.FileService.komodo.util.PublicKeyEncrypter;
import jpl.mipl.mdms.FileService.komodo.util.UrlInputStreamLoader;
import jpl.mipl.mdms.FileService.komodo.util.UserAuthenticator;
import jpl.mipl.mdms.FileService.komodo.util.UserToken;
import jpl.mipl.mdms.FileService.util.Command;
import jpl.mipl.mdms.FileService.util.ConsolePassword;
import jpl.mipl.mdms.FileService.util.DateTimeFormatter;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.Errno;
import jpl.mipl.mdms.FileService.util.Interpreter;
import jpl.mipl.mdms.FileService.util.PasswordUtil;
import jpl.mipl.mdms.FileService.util.SessionLogger;
import jpl.mipl.mdms.FileService.util.Switch;
import jpl.mipl.mdms.FileService.util.SystemProcess;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * This class defines the client side command line interpreter objects
 * 
 * @author G. Turek, T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: CLProcessor.java,v 1.89 2016/09/29 23:24:56 ntt Exp $
 */
public class CLProcessor {
   protected String _version = Constants.CLIENTVERSIONSTR;
   protected String _currentDir;
   protected String _startTcpPort;
   protected String _endTcpPort;
   protected URL _domainFile;
   protected String _newConn;
   protected Session _session;
   protected Hashtable _connections;
   protected String _prompt = "";
   protected String _batchFileName;
   protected BufferedReader _currentBatchFile;
   protected Stack _batchFileReaders;
   protected PrintWriter _pw;
   protected Interpreter _interpreter;
   protected String _commandsFile;
   protected String _logFile;
   protected Timer _batchTimer;
   protected boolean _repeatFlag = false;
   protected boolean _exitAfterBatch = false;
   protected String _loginFileType = null;
   protected final List _cmdHistory = new Vector();
   protected int _cmdHistoryMax = 10;
   protected DateTimeFormatter _dateFormatter;
   protected PublicKeyEncrypter _encrypter;
   protected UserAuthenticator _userAuthenticator;
   
   //Processing variables
   protected StringTokenizer _tokenizer;
   protected int _tokenCount;
   protected String _ttyInput;
   protected boolean _supportOS = false;
   protected long _startTime;

   //Regular expressions
   protected Matcher _noArg;
   protected Matcher _noArgWithInvoke;
   protected Matcher _noArgWithQuotedString;
   protected Matcher _oneArg;
   protected Matcher _twoArg;
   protected Matcher _threeArg;
   protected Matcher _fourArg;
   protected Matcher _fiveArg;
   protected Matcher _oneDate;
   protected Matcher _twoDate;
   protected Matcher _twoDateWithInvoke;
   protected Matcher _oneArgWithDate;
   protected Matcher _oneArgWithInvoke;
   protected Matcher _oneDateWithInvoke;
   protected Matcher _oneArgWithQuotedString;
   protected Matcher _twoArgWithQuotedString;
   protected Matcher _threeArgWithQuotedString;
   protected Matcher _fourArgWithQuotedString;
   protected Matcher _eightArgWithQuotedString;
   protected Matcher _nineArgWithQuotedString;
   protected Matcher _tenArgWithQuotedString;

   //Session settings
   protected boolean _abort = true;
   protected boolean _safeRead = false;
   protected boolean _checksum = false;
   protected boolean _autocommit = true;
   protected boolean _autoDel = false;
   protected boolean _replaceFile = false;
   protected boolean _versionFile = false;
   protected boolean _restart = false;
   protected boolean _log = true;
   protected boolean _echo = true;
   protected boolean _secure = false;
   protected boolean _signed = false;
   protected boolean _test = false;
   protected boolean _timer = false;
   protected boolean _verbose = false;
   protected boolean _veryverbose = false;
   protected boolean _receipt = false;
   protected boolean _done = false;
   protected boolean _init = true;
   protected boolean _preserve = true;
   protected boolean _replicate = false;
   protected boolean _diff = false;
   
   //BaseClient settings
   protected boolean _admin = false;
   protected boolean _toBatch = false;
   //protected boolean _debugEnabled = false;
   protected boolean _batch = false;
   protected boolean _silent = false;
   protected int _securityType = Constants.INSECURE;
   protected final int _cmdCharCount = 8;

   private Logger _logger = Logger.getLogger(CLProcessor.class.getName());

   /**
    * Constructor
    * 
    * @param admin if true, then this is an admin client
    * @param ssl if true, then SSL is enabled
    * @param batchFileName the name of the batch file
    * @param silent if true, then client activated the silent mode
    * @throws Exception general operation failure
    */
   public CLProcessor(boolean admin, boolean ssl, String batchFileName,
         boolean silent, boolean exitAfterBatch) throws Exception {
      this._admin = admin;
      this._batchFileName = batchFileName;
      this._exitAfterBatch = exitAfterBatch;
      this._silent = silent;

      this._batchFileReaders = new Stack();
      this._connections = new Hashtable();
      this._dateFormatter = new DateTimeFormatter();

      this._encrypter = new PublicKeyEncrypter();
      
      //add shutdown handler
      //Runtime.getRuntime().addShutdownHook(new ShutDownHandler());
      
      this.createRExp();

      String os = System.getProperty("os.name");
      if (os.equalsIgnoreCase("sunos") || os.equalsIgnoreCase("Linux")
            || os.equalsIgnoreCase("Mac OS X"))
         this._supportOS = true;

      this._currentDir = System.getProperty("user.dir");
      //this._domainFile = System.getProperty(Constants.PROPERTY_DOMAIN_FILE);
      this._startTcpPort = System.getProperty("komodo.tcp.startPort");
      this._endTcpPort = System.getProperty("komodo.tcp.endPort");
      
      ConfigFileURLResolver resolver = new ConfigFileURLResolver();
      this._domainFile = resolver.getDomainFile();      

      if (ssl)
         this._securityType = Constants.SSL;

      this._writeTTYLine("Domain file: " + this._domainFile);
      this._writeTTYLine(this._version);
      this._writeTTYLine(Constants.APIVERSIONSTR);
      this._writeTTYLine(Constants.COPYRIGHT);
      // Optional initialization. Pre-load classes needed by the Komodo
      // API to obfuscate the 20 second delay on the first "ct" attempt.
      Session.init();

      // Create a command interpreter, loaded with the BaseClient Komodo
      // commands.
      this._interpreter = new Interpreter(this._cmdCharCount, _mapCommands());

      //Create a session
      try {
         this._session = new Session(this._domainFile, this._securityType);
         this._session.setOption(Constants.AUTOCOMMIT, this._autocommit);
         this._session.setOption(Constants.CHECKSUM, this._checksum);
         this._session.setOption(Constants.DIFF, this._diff);
         // set the prompt for the default group
         // this._prompt = this._session.getDefaultGroup() + ":";
         // If client properties call for limiting tcp return port
         // ranges, set them in our Session instance.
         if (this._startTcpPort != null) {
            if (this._endTcpPort == null)
               this._endTcpPort = this._startTcpPort;
            this._logger.debug("startTcpPort = " + this._startTcpPort);
            this._logger.debug("endTcpPort = " + this._endTcpPort);

            this._session.setTcpPortRange(Integer.parseInt(this._startTcpPort),
                  Integer.parseInt(this._endTcpPort));
         }
         String settingsFile = this._session.getRegistry();
         File f = new File(settingsFile);
         if (f.exists()) {
            if (f.isDirectory())
               settingsFile += File.separator + Constants.SETTINGDIR;
            if ((new File(settingsFile)).exists()) {
               if ((new File(settingsFile)).canRead()) {
                  this._startBatch(settingsFile);
                  this.processInput();
                  this._logger.debug("Processed " + settingsFile + " file");
               } else
                  this._logger
                        .info("Don't have read permission for setting file "
                              + settingsFile);
            }
         }
      } catch (SessionException se) {
         this._logger.error(se.getMessage());
         this._logger.debug(null, se);
         System.exit(1);
      }
      
      
      //Create a authentication token generator
      try {
          _userAuthenticator = new UserAuthenticator(this._domainFile);
      } catch (SessionException se) {
         this._logger.error(se.getMessage());
         this._logger.debug(null, se);
         System.exit(1);
      }
      
      
   }

   /**
    * Internal method to initialize Regular expressions
    * 
    * @throws PatternSyntaxException when regular expression matching fails
    */
   private void createRExp() throws PatternSyntaxException {
       
       //dates can either be enclosed in quotes or a single string
      String dateExpr = "(\"[^\"]*\"|'[^']*'|\\S*)";
      
      this._noArg = Pattern.compile("^\\s*(\\w+)\\s*$").matcher("");
      this._noArgWithInvoke = Pattern.compile(
            "^\\s*(\\w+)\\s+invoke\\s+(\"(.*)\"|'(.*)')\\s*$").matcher("");
      // Enforce no args.
      this._oneArg = Pattern.compile("^\\s*(\\w+)\\s+(\\S+)\\s*$").matcher("");
      this._oneArgWithInvoke = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+invoke\\s+(\"(.*)\"|'(.*)')\\s*$")
            .matcher("");
      this._noArgWithQuotedString = Pattern.compile(
                                  "^\\s*(\\w+)\\s+\"(.*)\"\\s*$").matcher("");
      
      this._threeArg = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*$").matcher("");
      this._fourArg = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*$")
            .matcher("");
      this._fiveArg = Pattern
            .compile(
                  "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*$")
            .matcher("");
      this._twoArg = Pattern.compile("^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s*$")
            .matcher("");
      
      //this._oneArgWithDate = Pattern.compile(
      //      "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s*$").matcher("");
      this._oneArgWithDate = Pattern.compile(
              "^\\s*(\\w+)\\s+(\\S+)\\s+"+dateExpr+"\\s*$").matcher("");
      
//      this._oneDate = Pattern.compile("^\\s*(\\w+)\\s+(\\S+)\\s*$").matcher("");
//      this._twoDate = Pattern.compile(
//            "^\\s*(\\w+)\\s+(\\S+)\\s+and\\s+(\\S+)\\s*$").matcher("");
//
//      this._twoDateWithInvoke = Pattern
//            .compile(
//                  "^\\s*(\\w+)\\s+(\\S+)\\s+and\\s+(\\S+)\\s+invoke\\s+(\"(.*)\"|'(.*)')\\s*$")
//            .matcher("");

      this._oneDate = Pattern.compile("^\\s*(\\w+)\\s+"+dateExpr+"\\s*$").matcher("");
      this._twoDate = Pattern.compile(
            "^\\s*(\\w+)\\s+"+dateExpr+"\\s+and\\s+"+dateExpr+"\\s*$").matcher("");

      this._twoDateWithInvoke = Pattern
            .compile(
                  "^\\s*(\\w+)\\s+"+dateExpr+"\\s+and\\s+"+dateExpr+"\\s+invoke\\s+(\"(.*)\"|'(.*)')\\s*$")
            .matcher("");
      
      this._oneArgWithQuotedString = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+\"(.*)\"\\s*$").matcher("");

      // to handle <yyy-MM-ddThh:mm:ss.SSS> invoke "system command"
//      this._oneDateWithInvoke = Pattern.compile(
//            "^\\s*(\\w+)\\s+(\\S+)\\s+invoke\\s+(\"(.*)\"|'(.*)')\\s*$")
//            .matcher("");
      this._oneDateWithInvoke = Pattern.compile(
      "^\\s*(\\w+)\\s+"+dateExpr+"\\s+invoke\\s+(\"(.*)\"|'(.*)')\\s*$")
      .matcher("");
      
      this._twoArgWithQuotedString = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+\"(.*)\"\\s*$").matcher("");
      this._threeArgWithQuotedString = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+\"(.*)\"\\s*$")
            .matcher("");
      this._fourArgWithQuotedString = Pattern
            .compile(
                  "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+\"(.*)\"\\s*$")
            .matcher("");
      this._eightArgWithQuotedString = Pattern
            .compile(
                  "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+\"(.*)\"\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*$")
            .matcher("");
      this._nineArgWithQuotedString = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+\"(.*)\"\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*$")
            .matcher("");
      this._tenArgWithQuotedString = Pattern.compile(
            "^\\s*(\\w+)\\s+(\\S+)\\s+(\\S+)\\s+\"(.*)\"\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s*$")
            .matcher("");
   }

   /**
    * Map command aliases, regular expressions into methods.
    * 
    * @return an array of mapped commands.
    * @throws Exception general operation failure
    */
   protected Command[] _mapCommands() throws Exception {
      Command[] commandTable = {
            new Command(this, new String[] { "batch" }, "batch", ClientHelp
                  .getUsage("batch"), null, null),
            new Command(this, new String[] { "cd" }, "changeDir", ClientHelp
                  .getUsage("cd"), null, null),
            new Command(this, new String[] { "exit", "lo", "quit", "bye" },
                  "exit", ClientHelp.getUsage("exit"), null, null),
            new Command(this, new String[] { "help", "?", "h" }, "help",
                  ClientHelp.getUsage("help"), null, null),
            new Command(this, new String[] { "pause" }, "pause", ClientHelp
                  .getUsage("pause"), null, null),
            new Command(this, new String[] { "ls" }, "ls", ClientHelp
                  .getUsage("ls"), null, null),
            new Command(this, new String[] { "logFile", "log" }, "logFile",
                  ClientHelp.getUsage("logFile"), null,
                  new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "logCmds" }, "logCmds", ClientHelp
                  .getUsage("logCmds"), null, null),
            new Command(this, new String[] { "login" }, "login", ClientHelp
                  .getUsage("login"), null, new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "showDomainFile" },
                  "showDomainFile", ClientHelp.getUsage("showDomainFile"),
                  null, null),
            new Command(this, new String[] { "set" }, "set", ClientHelp
                  .getUsage("set"), null, null),
            new Command(this, new String[] { "pwd" }, "pwd", ClientHelp
                  .getUsage("pwd"), null, null),
            new Command(this, new String[] { "version", "v" }, "version",
                  ClientHelp.getUsage("version"), null, null),
            new Command(this, new String[] { "history" }, "history",
                          ClientHelp.getUsage("history"), null, null),
            new Command(this, new String[] { "dateFormat" }, "dateFormat",
                          ClientHelp.getUsage("dateFormat"), null, 
                          new Matcher[] { this._noArg, 
                                          this._noArgWithQuotedString }) 
            };
      return commandTable;
   }

   /**
    * Display the current domain file on standard display
    */
   public void showDomainFile() {
      try {                    
 
//         BufferedReader infile = new BufferedReader(new InputStreamReader(
//                                     this._domainFile.openStream()));

         InputStream is = UrlInputStreamLoader.open(this._domainFile);
         
         //Security code review prompted this change to prevent DOS attack (nttoole 08.27.2013)
         //BufferedReader infile = new BufferedReader(new InputStreamReader(is));       
         BufferedReader infile = new BoundedBufferedReader(new InputStreamReader(is));
         
         String line = infile.readLine();
         while (line != null) {
            this._writeTTYLine(line);
            line = infile.readLine();
         }
         infile.close();
      } catch (FileNotFoundException e) {
         this._logger
               .error("Domain file : " + this._domainFile + " not found.");
      } catch (IOException e) {
         this._logger.error("IO error while reading " + this._domainFile
               + " :\n" + e.getMessage());
      }
   }

   /**
    * Login sequence. Checks if this is a solaris system, if so hides passwd.
    */
   public void loginHiddenToUncoverCallers() {
      this._startTime = 0; // Don't time this command.
      try {
         String user = null, passwd = null;
         String encryptedPass = null;
         
         if (!this._batch) 
         {
            System.out.print("User name>> ");
            System.out.flush();
            if (this._log && this._logFile != null)
               SessionLogger.logPartialEntry("User name>> ");
            user = this._readTTYLine();
            if (this._log && this._logFile != null)
               SessionLogger.logEntry(user);
            if (user.equalsIgnoreCase("abort")) 
            {
               this._writeTTYLine("Type \"login\" to return to login sequence");
               return;
            } 
            else if (user.equalsIgnoreCase("exit")
                  || user.equalsIgnoreCase("quit")
                  || user.equalsIgnoreCase("bye")) 
            {
               this.exit(); // Does not return.
            }

            if (this._log && this._logFile != null)
               SessionLogger.logEntry("Password>> ");

            passwd = ConsolePassword.getPassword("Password>> ");

            if (passwd.equalsIgnoreCase("abort")) 
            {
               this._writeTTYLine("Type \"login\" to return to login sequence");
               return;
            } 
            else if (user.equalsIgnoreCase("exit") ||
                       user.equalsIgnoreCase("quit")   ||
                       user.equalsIgnoreCase("bye")) 
            {
               this.exit(); // Does not return.
            }
           
           
         } 
         else 
         {

            // Else condition handled in batch login sequence
            // Minimum of two tokens accepted (i.e. login commnad and
            // username)
            // The password is optional since PWDClient will get password if
            // Kerberos ticket is valid.
            if (this._tokenCount < 2) {
               this._handleError("Usage: login <name> [<password>]");
               return;
            }

            user = this._tokenizer.nextToken(); // user

            if (this._tokenCount > 2)
               passwd = this._tokenizer.nextToken(); // password
         }

         try { 
             encryptedPass = this._encrypter.encrypt(passwd);
         } catch (Exception ex) {
             this._handleError("Unable to encrypt password.");
             return;
         }
    
         
         if (this._connections.size() > 0)
         {
            this._writeTTYLine("Closing open sessions.");
            this._session.closeImmediate();           
         }
         /* Remove all references to any previous file types. */
         this._connections = new Hashtable();
         this._session.setLoginInfo(user, encryptedPass);

      } catch (Exception e) {
         this._logger.debug(null, e);
         if (e instanceof FileNotFoundException) {
            this._writeTTYLine("ERROR!: " + e.getMessage());
            System.exit(1);
         } else
            this._writeTTYLine(e.getMessage());
      }
   }
   
   public void login() {
       login(null);
   }
   public void login(String serverGroup) {
       
       this._startTime = 0; // Don't time this command.
       try {
          String user = null, passwd = null;
          
          String encryptedPass = null;
          UserToken authToken = null;
          
          if (!this._batch) 
          {
              
              while (serverGroup == null)
              {
                  System.out.print("Server group>> ");
                  System.out.flush();
                  if (this._log && this._logFile != null)
                     SessionLogger.logPartialEntry("Server group>> ");
                  serverGroup = this._readTTYLine();
                  if (this._log && this._logFile != null)
                     SessionLogger.logEntry(serverGroup);
                  if (serverGroup.equalsIgnoreCase("abort")) 
                  {
                     this._writeTTYLine("Type \"login\" to return to login sequence");
                     return;
                  } 
                  else if (serverGroup.equalsIgnoreCase("exit")
                        || serverGroup.equalsIgnoreCase("quit")
                        || serverGroup.equalsIgnoreCase("bye")) 
                  {
                     this.exit(); // Does not return.
                  }
                  else if (serverGroup.isEmpty())
                  {
                      this._writeTTYLine("Server group must be specified. Enter 'abort' to abort.");
                      serverGroup = null;
                  }
              }
              
              //---------------------------

              //request the authentication method from the servergroup
              AuthenticationType authType = null;
              try {
                  authType = _userAuthenticator.getAuthenticationType(serverGroup);
              } catch (SessionException sesEx) {
                  //default error message
                  String errMsg = "Authentication Error! Unable to query server "+
                          "authentication method for servergroup '"+serverGroup + "'.";
                  
                  //if conn failed, provide potentially more useful message
                  if (sesEx.getErrno() == Constants.CONN_FAILED)
                  {                
                      errMsg = "Unable to connect to server group '" +
                               serverGroup+"'.\nPlease check network " +
                               "status and FEI domain file configuration.";                      
                  }
                  
                  this._logger.severe(errMsg);                  
                  _logger.trace(null,sesEx);
                  return;
              }
              
              final String pwdPrompt = PasswordUtil.getPrompt(authType);
              
              
              //---------------------------
              
              
              
              
              
             System.out.print("User name>> ");
             System.out.flush();
             if (this._log && this._logFile != null)
                SessionLogger.logPartialEntry("User name>> ");
             user = this._readTTYLine();
             if (this._log && this._logFile != null)
                SessionLogger.logEntry(user);
             if (user.equalsIgnoreCase("abort")) 
             {
                this._writeTTYLine("Type \"login\" to return to login sequence");
                return;
             } 
             else if (user.equalsIgnoreCase("exit")
                   || user.equalsIgnoreCase("quit")
                   || user.equalsIgnoreCase("bye")) 
             {
                this.exit(); // Does not return.
             }

             if (this._log && this._logFile != null)
                SessionLogger.logEntry(pwdPrompt+">> ");

             passwd = ConsolePassword.getPassword(pwdPrompt+">> ");

             if (passwd.equalsIgnoreCase("abort")) 
             {
                this._writeTTYLine("Type \"login\" to return to login sequence");
                return;
             } 
             else if (passwd.equalsIgnoreCase("exit") ||
                      passwd.equalsIgnoreCase("quit")   ||
                      passwd.equalsIgnoreCase("bye")) 
             {
                this.exit(); // Does not return.
             }
          } 
          else 
          {

             // Else condition handled in batch login sequence
             // Minimum of two tokens accepted (i.e. login commnad and
             // username)
             // The password is optional since PWDClient will get password if
             // Kerberos ticket is valid.
             if (this._tokenCount < 2) {
                this._handleError("Usage: login <name> [<password>] [<servergroup>]");
                return;
             }

             user = this._tokenizer.nextToken(); // user

             if (this._tokenCount > 2)
                 passwd = this._tokenizer.nextToken(); // password
             
             if (this._tokenCount > 3)
                serverGroup = this._tokenizer.nextToken(); // servergroup
             
          }

          //encrypt user password
          try { 
              encryptedPass = this._encrypter.encrypt(passwd);
          } catch (Exception ex) {
              this._handleError("Unable to encrypt password.");
              return;
          }
          
          
          //perform authentication token generation
          try { 
              authToken = this._userAuthenticator.authenticate
                               (user, encryptedPass, serverGroup);
          } catch (Exception ex) {
              this._handleError("Unable to generate authentication token.");
              return;
          }
     
          if (authToken == null || !authToken.isValid())
          {
              String errMesg = "User '"+user+"' was not authenticated for " +
              		"group '"+serverGroup+"'";
              this._handleError(errMesg);
              return;
          }
          
          
          if (this._connections.size() > 0)
          {
             this._writeTTYLine("Closing open sessions.");
             this._session.closeImmediate();           
          }
          /* Remove all references to any previous file types. */
          this._connections = new Hashtable();
          this._session.setLoginInfo(user, authToken.getToken());

       } catch (Exception e) {
          this._logger.debug(null, e);
          if (e instanceof FileNotFoundException) {
             this._writeTTYLine("ERROR!: " + e.getMessage());
             System.exit(1);
          } else
             this._writeTTYLine(e.getMessage());
       }
    }

   
   /**
    * Method to load user login information from the cached login file
    */
   public void loginUsingCache() throws Exception {       
           this.loginUsingCache(null);
   }
   
   public void loginUsingCache(String serverGroup) throws Exception {
       
       LoginFile loginFile = new LoginFile();
       
       String username = loginFile.getUsername(serverGroup);
       String password = loginFile.getPassword(serverGroup);
       
       this._session.setLoginInfo(username, password);
   }
   
//   /**
//    * Method to load user login information from the cached login file
//    */
//   public void loginUsingCache() throws Exception {
//       
//      String restartDir = System.getProperty(Constants.PROPERTY_RESTART_DIR);
//      if (restartDir == null)
//         restartDir = System.getProperty("user.home") + File.separator
//               + Constants.RESTARTDIR;
//      else    
//         restartDir = restartDir + File.separator + Constants.RESTARTDIR;
//
//       String loginFilename = restartDir + File.separator + Constants.LOGINFILE;
//       File loginFile = new File(loginFilename);
//       Properties prop = new Properties();
//       prop.load(new FileInputStream(loginFile));
//       String user = prop.getProperty("user").trim();
//       String password = prop.getProperty("password").trim();
//       
//       this._session.setLoginInfo(user, password);
//   }
   
   /**
    * Method to process user input
    */
   public void processInput() {
      this._logger.debug("Processing input");
      this._startTime = 0;

      while (!this._done) {
         this._readInput();
         
         processTtyInput();
         
         /*
         this._tokenizer = new StringTokenizer(this._ttyInput);
         this._tokenCount = this._tokenizer.countTokens();
         if (this._tokenCount > 0) {
            cmnd = this._tokenizer.nextToken();
            if (this._batch && !cmnd.equalsIgnoreCase("pause"))
               this._writeTTYLine(this._prompt + ">> " + this._ttyInput);
            try {
               if (this._timer)
                  this._startTime = System.currentTimeMillis();
               else
                  this._startTime = 0;

               maxCmdSize = this._cmdCharCount > cmnd.length() ? cmnd.length()
                     : this._cmdCharCount;

               if (cmnd.length() > 0 && cmnd.charAt(0) == '#') {
                  // Skip comment line.
                  // Don't print time for comment.
                  this._startTime = 0;
               } else {
                  // See if this command is in our lookup table. If it is,
                  // execute it.
                  // If not echo error message
                  try {
                     this._interpreter.exec(this, this._ttyInput);
                  } catch (InvocationTargetException ite) {
                     // Starting with jdk 1.4: throw (Exception)
                     // ite.getCause();
                     throw (Exception) ite.getTargetException();
                  }
               }
            } catch (Exception e) {
               if (e instanceof SessionException) {
                  this._handleError(e.getMessage());
                  if (((SessionException) e).getErrno() == Constants.INVALID_LOGIN) {
                     this.login();
                  }
               } else if (e instanceof ConnectException) {
                  this._handleError(_newConn + " unavailable");
               } else if (e instanceof java.text.ParseException) {
                  this._handleError(e.getMessage());
               } else {
                  this._abort = true;
                  this._silent = false;
                  this._echo = true;
                  this._handleError(e.getMessage());
                  this._logger.debug(null, e);
               }
            }
         }
         */
      }
      // We're done.
      if (this._batchTimer == null && !this._init)
         this.exit();
      if (this._init) {
         // We're not really done if we just processed the registory file.
         this._init = false;
         this._done = false;
      }
   }

   public void processTtyInput()
   {
       String cmnd;
       int maxCmdSize;
       Command cmndInfo;
       
       this._logger.debug("Tokenizing _ttyInput: " + this._ttyInput);
       
       addToCommandHistory(this._ttyInput);
       
       this._tokenizer = new StringTokenizer(this._ttyInput);
       this._tokenCount = this._tokenizer.countTokens();
       if (this._tokenCount > 0) {
          cmnd = this._tokenizer.nextToken();
          if (this._batch && !cmnd.equalsIgnoreCase("pause"))
             this._writeTTYLine(this._prompt + ">> " + this._ttyInput);
          try {
             if (this._timer)
                this._startTime = System.currentTimeMillis();
             else
                this._startTime = 0;

             maxCmdSize = this._cmdCharCount > cmnd.length() ? cmnd.length()
                   : this._cmdCharCount;

             if (cmnd.length() > 0 && cmnd.charAt(0) == '#') {
                // Skip comment line.
                // Don't print time for comment.
                this._startTime = 0;
             } else {
                // See if this command is in our lookup table. If it is,
                // execute it.
                // If not echo error message
                try {
                   this._interpreter.exec(this, this._ttyInput);
                } catch (InvocationTargetException ite) {
                   // Starting with jdk 1.4: throw (Exception)
                   // ite.getCause();
                   throw (Exception) ite.getTargetException();
                }
             }
          } catch (Exception e) {
             if (e instanceof SessionException) {
                this._handleError(e.getMessage());
                if (((SessionException) e).getErrno() == Constants.INVALID_LOGIN) {
                   this.login(null);
                }
             } else if (e instanceof ConnectException) {
                this._handleError(_newConn + " unavailable");
             } else if (e instanceof java.text.ParseException) {
                this._handleError(e.getMessage());
             } else {
                this._abort = true;
                this._silent = false;
                this._echo = true;
                this._handleError(e.getMessage());
                this._logger.debug(null, e);
             }
          }
       }
   }
   
   /**
    * Method to display current client version
    */
   public final void version() {
      this._startTime = 0; // Don't time this command.
      this._writeTTYLine(this._version);
      if (this._verbose)
         this._writeTTYLine(Session.getApiVersionString());
   }

   /**
    * Method to display current working directory
    */
   public final void pwd() {
      this._startTime = 0; // Don't time this command.
      this._writeTTYLine(this._currentDir);
   }

   /**
    * Method to list files in the current working directory
    * 
    * @throws java.io.IOException when local directory access fail
    */
   public final void ls() throws IOException {
      this._startTime = 0; // Don't time this command.
      File currentDir = new File(this._currentDir);
      File[] list = currentDir.listFiles();
      if (list == null)
          list = new File[0];
      
      for (int i = 0; i < list.length; ++i) {
          String path = list[i].getName();
          if (list[i].isDirectory())
              path = path + File.separator;
          this._writeTTYLine(path);
      }
   }

   /**
    * Method to display "Command not implemented" message
    */
   public final void notImplemented() {
      this._handleError("Command not implemented.");
   }

   /**
    * Method to process log file input
    */
   public final void logFile() {
      this._startTime = 0; // Don't time this command.
      if (!this._tokenizer.hasMoreTokens()) {
         if (this._logFile != null) {
            SessionLogger.closeLogFile();
            this._writeTTYLine("Closed log file " + this._logFile);
            this._logFile = null;
         } else {
            this._handleError("Missing log file name");
         }
         return;
      }

      String tmp = this._tokenizer.nextToken();
      if (!tmp.equals(this._logFile) && this._logFile != null) {
         SessionLogger.closeLogFile();
         this._logFile = null;
      }

      try {
         SessionLogger.openLogFile("BaseClient", tmp);
         this._logFile = tmp;
      } catch (IOException ioe) {
         this._handleError("Unable to open log file " + tmp);
      }
   }
   
   /**
    * Method to process date formatting.
    */
   
   public final void dateFormat() {
      this._startTime = 0; // Don't time this command.
      
      String currentFmt = this._dateFormatter != null ?
                          this._dateFormatter.getFormatString() :
                          null;
      
      //--------------------------- 
                          
      //if no value, then print the current pattern
      if (!this._tokenizer.hasMoreTokens()) 
      {
         String msg = "Current date/time format: ";
         msg += (currentFmt != null) ? "\"" + currentFmt + "\"" : "N/A";
         this._writeTTYLine(msg);
         return;
      }
      
      //--------------------------- 

      //value provided, use as new format if different
      String tmp = "";
      while (this._tokenizer.hasMoreTokens())
          tmp += tmp + " " + this._tokenizer.nextToken();      
      tmp = tmp.trim();
      
      tmp = this._noArgWithQuotedString.group(2);
            
      //String tmp = this._tokenizer.nextToken();
      if (!tmp.equals(currentFmt)) {
          //create new formatter
          this._dateFormatter = new DateTimeFormatter(tmp);
      
          //-----------------------
          //Print message with exmaple formatted time
          
          if (false)
          {
              String fNow = "", msg;
              fNow = this._dateFormatter.formatDate(new Date());
              
              if (fNow != null)
                  msg = "New format set: '"+tmp+"'.  Now = "+ fNow;
              else
                  msg = "Warning - Could not successfully parse using pattern";
              this._writeTTYLine(msg);
          }
      }
      
      //---------------------------      
   }

   /**
    * Log commands and output to file.
    */
   public void logCmds() {
      this._startTime = 0; // Don't time this command.
      if (!this._tokenizer.hasMoreTokens()) {
         //If no argument assume close existing file
         if (this._toBatch) {
            this._pw.close();
            this._toBatch = false;
            this._writeTTYLine("Closed commands file " + this._commandsFile);
            return;
         } else {
            this._handleError("Missing commands file name");
            return;
         }
      } else {
         try {
            this._commandsFile = this._tokenizer.nextToken();
            /*
             * * Close the old log file if creating a new one.
             */
            if (this._pw != null) {
               this._pw.close();
               this._writeTTYLine("Closed command file \"" + this._commandsFile
                     + "\".");
               this._pw = null;
            }
            this._pw = new PrintWriter(new FileWriter(this._commandsFile),
                  false);
            this._toBatch = true;
            return;
         } catch (IOException ioe) {
            this._toBatch = false;
            this._handleError("Unable to open new commands file "
                  + this._commandsFile);
         }
      }
   }

   /**
    * Method to start batch processin
    */
   public final void batch() {
      if (!this._tokenizer.hasMoreTokens()) {
         this._handleError("Usage: batch <file name> [repeatAt hh:mm AM|PM |" +
                           " repeatEvery hh:mm [hh:mm AM|PM]]");
         return;
      } else if (this._tokenCount == 2) {
         String fileName = this._tokenizer.nextToken();
         //If no separator caracters are found, then assume it's in the
         //current directory
         if (fileName.indexOf(File.separator) < 0)
            fileName = this._currentDir + File.separator + fileName;
         this._startBatch(fileName);
      } else {
         String fileName = this._tokenizer.nextToken();
         //If no separator caracters are found, then assume it's in the
         //current directory
         if (fileName.indexOf(File.separator) < 0)
            fileName = this._currentDir + File.separator + fileName;
         String modifier = this._tokenizer.nextToken();
         String time1 = this._tokenizer.nextToken();

         if (modifier.equalsIgnoreCase("repeatAt")) {
            this._repeatFlag = true;
            time1 = time1.toLowerCase();
            String ampm = null;

            if (this._tokenizer.hasMoreTokens()) {
               ampm = this._tokenizer.nextToken();
               if (!ampm.equalsIgnoreCase("AM") && !ampm.equalsIgnoreCase("PM")) {
                  this._handleError("Usage: batch <file name> [repeatAt hh:mm AM|PM |" +
                                    " repeatEvery hh:mm [hh:mm AM|PM]]");
                  return;
               }
               time1 += " " + ampm;
            } else if (time1.endsWith("am") || time1.endsWith("pm")) {
               StringBuffer sb = new StringBuffer(time1);
               sb = sb.insert(time1.indexOf('m') - 1, ' ');
               time1 = sb.toString();
            } else {
               this._handleError("Usage: batch <file name> [repeatAt hh:mm AM|PM |" +
                                 " repeatEvery hh:mm [hh:mm AM|PM]]");
               return;
            }

            this._logger.debug("time1 now " + time1);
            Date schedTime = this._getDate(time1);
            if (schedTime == null)
               return;
            this._batchTimer = new Timer();
            this._batchTimer.scheduleAtFixedRate(new ScheduledBatch(fileName),
                                                 schedTime, 86400000);
            // 24 hours
            //Exit processInput() loop so can't issue any more commands
            this._done = true;
         } else if (modifier.equalsIgnoreCase("repeatEvery")) {
            this._repeatFlag = true;
            int indx = time1.indexOf(':');
            int hour = 0;
            int min = 0;
            if (indx > -1) {
               hour = Integer.parseInt(time1.substring(0, indx));
               min = Integer.parseInt(time1.substring(indx + 1));
            } else {
               hour = Integer.parseInt(time1);
               time1 = time1 + ":00";
            }

            this._batchTimer = new Timer();
            long interval = ((hour * 60 * 60) + (min * 60)) * 1000;

            if (this._tokenizer.hasMoreTokens()) {
               String time2 = this._tokenizer.nextToken().toLowerCase();
               String ampm = null;
               if (this._tokenizer.hasMoreTokens()) {
                  ampm = this._tokenizer.nextToken();
                  if (!ampm.equalsIgnoreCase("AM")
                        && !ampm.equalsIgnoreCase("PM")) {
                     this._handleError("Usage: batch <file name> " +
                                       "[repeatAt hh:mm AM|PM |" +
                                       " repeatEvery hh:mm [hh:mm AM|PM]]");
                     return;
                  }
                  time2 += " " + ampm;
               } else if (time2.endsWith("am") || time2.endsWith("pm")) {
                  StringBuffer sb = new StringBuffer(time2);
                  sb = sb.insert(time2.indexOf('m') - 1, ' ');
                  time2 = sb.toString();
               } else {
                  this._handleError("Usage: batch <file name> [repeatAt hh:mm AM|PM |" +
                                    " repeatEvery hh:mm [hh:mm AM|PM]]");
                  return;
               }

               this._logger.debug("time2 now " + time2);
               Date schedTime = this._getDate(time2);
               if (schedTime == null)
                  return;
               this._batchTimer.scheduleAtFixedRate(
                     new ScheduledBatch(fileName), schedTime, interval);
            } else {
               this._batchTimer.scheduleAtFixedRate(
                     new ScheduledBatch(fileName), 0, interval);
            }

            this._writeTTYLine("Batch file will be executed every " + 
                               interval + " (ms)");
            //Exit processInput() loop so can't issue any more commands
            this._done = true;
         } else {
            this._handleError("Usage: batch <file name> [repeatAt hh:mm AM|PM |" +
                              " repeatEvery hh:mm [hh:mm AM|PM]]");
         }
      }
   }

   /**
    * Process change directory command
    * 
    * @throws SessionException when general failure
    */
   public final void changeDir() throws SessionException {
      File f;
      String newDir;
      String cdup = ".." + File.separator;
      String usrhome = "~" + File.separator;

      this._startTime = 0; // Don't time this command.
      if (this._tokenCount < 2)
         newDir = System.getProperty("user.home");
      else
         newDir = this._tokenizer.nextToken();

      if (newDir.equals("..") || newDir.equals(cdup)) {
         this._currentDir = this._currentDir.substring(0, this._currentDir
               .lastIndexOf(File.separator));
      } else if (newDir.startsWith("..")) {
         String tmp = this._currentDir;
         while (newDir.startsWith("..")) {
            if (newDir.equals("..") || newDir.equals(cdup))
               newDir = "";
            else
               newDir = newDir.substring(newDir.indexOf(File.separator) + 1);
            tmp = tmp.substring(0, tmp.lastIndexOf(File.separator));
         }

         if (newDir.equals(""))
            newDir = tmp;
         else
            newDir = tmp + File.separator + newDir;

         f = new File(newDir);
         if (!f.exists() || !f.isDirectory()) {
            this._echo = true;
            this._abort = true;
            this._handleError("Directory " + newDir + " not found");
            return;
         } else {
            this._currentDir = newDir;
         }
      } else if (newDir.equals("~") || newDir.equals(usrhome)) {
         this._currentDir = System.getProperty("user.home");

      } else if (newDir.length() > 0 && newDir.charAt(0) == '~') {
         if (newDir.indexOf(File.separator) > -1)
            this._currentDir = System.getProperty("user.home") + File.separator
                  + newDir.substring(2);
         else {
            String tmp = System.getProperty("user.home");
            tmp = tmp.substring(0, tmp.lastIndexOf(File.separator) + 1);
            this._currentDir = tmp + newDir.substring(1);
         }
      } else {
         if (newDir.indexOf(File.separator) != 0)//== -1)
            newDir = _currentDir + File.separator + newDir;
         f = new File(newDir);
         if (!f.exists() || !f.isDirectory()) {
            this._handleError("Directory " + newDir + " not found");
            return;
         } else {
            this._currentDir = newDir;
         }
      }
      // Set the current directory for the session. This changes
      // the directory for all sub classes too. For example, CCL
      // Processor new file type commands will use this new directory
      if (this._session != null)
         this._session.setDirectory(_currentDir);
      this._writeTTYLine("Current directory set to " + this._currentDir);
   }

   
   /**
    * Process history command
    * 
    * @throws SessionException when general failure
    */
   public final void history() throws SessionException {

      this._startTime = 0; // Don't time this command.
      String numberString = null;
      int number;
      String histCmd = null;
     
      if (this._tokenCount > 2)
      {
          this._handleError("Too many arguments.");
          return;
      }
      else if (this._tokenCount == 2)
          numberString = this._tokenizer.nextToken();
      
      synchronized (this._cmdHistory)
      {
          //try to retrieve historial command and execute it
          if (numberString != null)
          { 
              try {
                  number = Integer.parseInt(numberString);
              } catch (NumberFormatException nfEx) {
                  this._handleError("Parameter " + numberString + 
                                    " not an integer");
                  return;
              }
            
              if (number == 0) {
                  this._handleError("Argument out of range");
                  return;
              }
              

              int index = 0;  
              if (number > 0)
              {
                  index = number - 1;
              }
              else if (number < 0)
              {
                  index = this._cmdHistory.size() + number;
              }
              
              if (index < 0 || index >= this._cmdHistory.size())
              {
                  this._handleError("Argument out of range");
                  return;
              }
              
              this._logger.trace("Retrieving command at index "+(index+1));
              histCmd = (String) this._cmdHistory.get(index);
              this._writeTTYLine(this._prompt + ">> " + histCmd);
              this._ttyInput = histCmd;
              processTtyInput();
          }
          else
          {
              //print history
              int size = this._cmdHistory.size();
              this._logger.trace("Printing commands from history");
              for (int i = 0; i < size; ++i)
              {
                  histCmd = (String) this._cmdHistory.get(i);
                  if (histCmd == null)
                      histCmd = "N/A";
                  this._writeTTYLine("---" + (i+1) + "\n" + histCmd + "\n");
              }
          }
      }
   }
   
   /**
    * Appends command string to end of command history list.
    * If command parameter is a history command, then it will
    * not be added.  All other commands will be added.
    * If a max was set of history length, then older commands
    * will be discarded.
    * 
    * @param cmdString Command string to be added
    * @return True if parameter was added, false otherwise
    */
   
   protected boolean addToCommandHistory(String cmdString)
   {
       boolean added = false;
       
       if (cmdString == null || cmdString.equals(""))
           return added;
       
       //only add if not a history call
       String cmdlow = cmdString.toLowerCase();
       if (!cmdlow.startsWith("history") && 
           !cmdlow.startsWith("history "))
       {
           synchronized (this._cmdHistory)
           {
               this._logger.trace("Adding \"" + cmdString 
                                  + "\" to command history");
               this._cmdHistory.add(cmdString);
               added = true;
               
               //remove older commands if max is set
               if (this._cmdHistoryMax >= 0)
               {
                   while (this._cmdHistoryMax < this._cmdHistory.size())
                       this._cmdHistory.remove(0);
               }
           }
       }
       return added;
   }
   
   /**
    * Process batch command. Set up file reader. Note, if this is a startUp
    * file, registory, then file not found is not an error.
    * 
    * @param fileName the batch file name
    */
   protected final void _startBatch(String fileName) {
      this._batch = true;
      this._logger.debug("Reading batch file \"" + fileName + "\".");
      try {
         File f = new File(fileName);

         this._logger.debug("batch file: " + fileName);
//         this._currentBatchFile = new BufferedReader(new FileReader(fileName));
         this._currentBatchFile = new BoundedBufferedReader(new FileReader(fileName));
         
         this._batchFileReaders.push(this._currentBatchFile);

         this._logger.debug("Stack size = " + this._batchFileReaders.size());
         this._writeTTYLine("Executing batch file " + fileName);
         return;
      } catch (FileNotFoundException e) {
         this._abort = true;
         this._silent = false;
         this._echo = true;
         this._handleError("Batch file \"" + fileName + "\" not found.");
         return;
      }
   }

   /**
    * Method to handle change session settings (set command)
    */
   public final void set() {
      this._startTime = 0; // Don't time this command.
      if (!this._tokenizer.hasMoreTokens()) {
         // Switch class, inherits from Boolean,
         // w/ toString () -> "on" | "off"???
         String s = "abort                "
               + (new Switch(this._abort)).toString() + "\n"
               + "autoDelete           "
               + (new Switch(this._autoDel)).toString() + "\n"
               + "computeChecksum      "
               + (new Switch(this._checksum)).toString() + "\n"
               + "diff                 " 
               + (new Switch(this._diff)).toString() + "\n" 
               + "echo                 " 
               + (new Switch(this._echo)).toString() + "\n" 
               + "log                  "
               + (new Switch(this._log)).toString() + "\n"
               + "preserve             "
               + (new Switch(this._preserve)).toString() + "\n"
               + "receipt              "
               + (new Switch(this._receipt)).toString() + "\n"
               + "replaceFile          "
               + (new Switch(this._replaceFile)).toString() + "\n"
               + "replicate            "
               + (new Switch(this._replicate)).toString() + "\n"
               + "restart              "
               + (new Switch(this._restart)).toString() + "\n"
               + "safeRead             "
               + (new Switch(this._safeRead)).toString() + "\n"
               + "test                 " + (new Switch(this._test)).toString()
               + "\n" + "timer                "
               + (new Switch(this._timer)).toString() + "\n"
               + "verbose              "
               + (new Switch(this._verbose)).toString() + "\n"
               + "veryverbose          "
               + (new Switch(this._veryverbose)).toString() + "\n"
               + "versionFile          "
               + (new Switch(this._versionFile)).toString();
         this._writeTTYLine(s);
         return;
      }

      if (this._tokenCount < 3) {
         this._handleError("Incomplete command.");
         return;
      }

      String name = this._tokenizer.nextToken();
      String value = this._tokenizer.nextToken();
      boolean flag;

      if (value.equalsIgnoreCase("on"))
         flag = true;
      else if (value.equalsIgnoreCase("off"))
         flag = false;
      else {
         this._handleError("Value must be on | off.");
         return;
      }

      if (name.equalsIgnoreCase("abort")) {
         this._abort = flag;
         if (this._session != null)
            this._session.setOption(Constants.ABORTALLONERR, flag);
      } else if (name.equalsIgnoreCase("safeRead")) {
         this._safeRead = flag;
         if (this._session != null)
            this._session.setOption(Constants.SAFEREAD, flag);
      } else if (name.equalsIgnoreCase("computeChecksum")) {
         this._checksum = flag;
         if (this._session != null)
            this._session.setOption(Constants.CHECKSUM, flag);
      } else if (name.equalsIgnoreCase("autoDelete")) {
         this._autoDel = flag;
         if (this._session != null)
            this._session.setOption(Constants.AUTODEL, flag);
      } else if (name.equalsIgnoreCase("log")) {
         this._log = flag;
      } else if (name.equalsIgnoreCase("receipt")) {
         this._receipt = flag;
         if (this._session != null)
            this._session.setOption(Constants.RECEIPTONXFR, flag);
      } else if (name.equalsIgnoreCase("replaceFile")) {
         this._replaceFile = flag;
         if (this._versionFile && this._replaceFile)
             this._versionFile = false;
         
         if (this._session != null)
         {
             //set replace
            this._session.setOption(Constants.FILEREPLACE, flag);
            
            //unset version if necessary
            if (this._session.getOption(Constants.FILEVERSION) != this._versionFile)
            {
                this._writeTTYLine("Resetting 'versionFile' off");
                this._session.setOption(Constants.FILEVERSION, this._versionFile);
            }
         }
      } else if (name.equalsIgnoreCase("versionFile")) {
         this._versionFile = flag;
         
         if (this._versionFile && this._replaceFile)
             this._replaceFile = false;
         
         if (this._session != null)
         {
             //set version
            this._session.setOption(Constants.FILEVERSION, flag);
            
            //unset replace if necessary
            if (this._session.getOption(Constants.FILEREPLACE) != this._replaceFile)
            {
                this._writeTTYLine("Resetting 'replaceFile' off");
                this._session.setOption(Constants.FILEREPLACE, this._replaceFile);
            }
         }
      } else if (name.equalsIgnoreCase("restart")) {
         this._restart = flag;
         if (this._session != null)
            this._session.setOption(Constants.RESTART, flag);
      } else if (name.equalsIgnoreCase("test")) {
         this._test = flag;
      } else if (name.equalsIgnoreCase("timer")) {
         this._timer = flag;
      } else if (name.equalsIgnoreCase("verbose")) {
         this._verbose = flag;
         this._veryverbose = false;
      } else if (name.equalsIgnoreCase("veryverbose")) {
         this._veryverbose = flag;
      } else if (name.equalsIgnoreCase("echo")) {
         this._echo = flag;
      } else if (name.equalsIgnoreCase("preserve")) {
          this._preserve = flag;
      } else if (name.equalsIgnoreCase("replicate")) {
          this._replicate = flag;
          if (this._session != null)
              this._session.setOption(Constants.REPLICATE, flag);
      } else if (name.equalsIgnoreCase("diff")) {
          this._diff = flag;
          if (this._session != null)
              this._session.setOption(Constants.DIFF, flag);
      } else {
         this._handleError("Incorrect command " + this._ttyInput);
         return;
      }
   }

   /**
    * Check existance and validity of a "file"
    * 
    * @param fileName the input file name
    * @return true if file OK. Handles error.
    */
   protected final boolean _checkFile(String fileName) {
      File ftest = new File(this._currentDir, fileName);
      if (!ftest.exists()) {
         this._handleError("File: " + this._currentDir + File.separator
               + fileName + " does not exist.");
         return false;
      }

      if (ftest.isDirectory()) {
         this._handleError("File: " + this._currentDir + File.separator
               + fileName + " is a directory.");
         return false;
      }
      return true;
   }

   /**
    * Reads input, either from command line or batch input NOTE: this is a
    * recursive function, watch for cleanup.
    */
   protected final void _readInput() {
      // If we've just executed a timed command, write out timing information.
      if (this._startTime > 0) {
         this._writeTTYLine("Elapsed time: "
               + (System.currentTimeMillis() - this._startTime) / 1000.0
               + " seconds.");
      }
      this._logger.debug("Reading input");

      if (!this._batch) {
         // Command line input
         this._handlePrompt();
         this._ttyInput = this._readTTYLine();
         //Echo to batch file
         if (this._toBatch)
            this._pw.println(this._ttyInput);
         if (this._log && this._logFile != null)
            SessionLogger.logEntry(this._ttyInput);

      } else {
         // Batch file input
         this._logger.debug("current batch file " + this._currentBatchFile);
         this._logger.debug("repeat flag: " + this._repeatFlag);
         // Check to see if repeatFlag has been set. It is set by a batch
         // command with repeatEvery or repeatAfter. If it is set, set
         // exitAfterBatch to false because we want to block until repeat
         // time.
         if (this._repeatFlag)
            this._exitAfterBatch = false;

         try {
            this._ttyInput = this._currentBatchFile.readLine();
            this._logger.debug("* ttyInput " + this._ttyInput);

            if (this._ttyInput == null) {
               this._logger.debug("Batch file finished");
               if (this._test)
                  this._writeTTYLine("Batch file test completed.");

               // batchTimer gets set when batch ... repeatEvery|repeatAt
               // commands are issued. We don't want to close current
               // batch file or check the stack if we're repeating this
               // file.
               if (this._batchTimer != null)
                  this._done = true;

               // We're not running a repeating or scheduled command so
               // close the current batch file when done reading and pop
               // the
               // stack if more batch files exist
               this._currentBatchFile.close();
               if (!this._batchFileReaders.empty())
                  this._batchFileReaders.pop();
               if (this._batchFileReaders.empty()) {
                  this._logger.debug("batch file is empty");
                  this._batch = false;

                  // exit FEI5 client, this should only occur when the
                  // client
                  // is passed the '-b' option which is non-interactive
                  // batch
                  // mode.
                  if (this._exitAfterBatch) {
                     this._logger
                           .debug("*** Batch processing is over, exiting client! ***");
                     this.exit();
                  } else {
                     this._ttyInput = "\n";
                  }

               } else {
                  this._logger.debug("batch file is NOT empty");
                  this._currentBatchFile = (BufferedReader) this._batchFileReaders
                        .peek();
                  this._logger.debug(" ****** Stack size = "
                        + this._batchFileReaders.size());
                  this._readInput();

                  //Must force exit from processInput() in order for
                  // timer to
                  //finish one run
                  if (this._batchTimer != null)
                     this._done = true;
               }
            }
         } catch (IOException e) {
            //e.printStackTrace();
            if (this._batchFileReaders.empty())
               this._batch = false;
            try {
               this._currentBatchFile.close();
               this._batchFileReaders.pop();

               this._batch = false;
               if (this._exitAfterBatch) {
                  this.exit();
               } else {
                  this._ttyInput = "\n";
               }
            } catch (IOException ie) {
               this._batchFileReaders.pop();
               this._logger.error(ie.getMessage());
            }
         }
      }
   }

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
               // This is a temporary fix. XXXX We really need lookahead.
               //if (_os.equalsIgnoreCase("macos"))
               /**
                * if (this._supportOS) break; else { System.in.read(); break; }
                */
            } else if (ch == '\n')
               break; // Unix flavors.
            buf.append(ch);
         }
      } catch (IOException e) {
         this._logger.error(e.getMessage());
      }
      return buf.toString();
   }

   /**
    * Prints a line to standard out
    * 
    * @param msg a string to be printed to STDOUT
    */
   protected final void _writeTTYLine(String msg) {
      if (this._echo) {
         if (!this._silent)
            System.out.println(msg);
         if (this._log && this._logFile != null)
            SessionLogger.logEntry(msg);
      }
   }

   /**
    * Command line prompt to standard out. Note it uses print rather than
    * println because we don't want an EOL. However then we must flush.
    */
   protected final void _handlePrompt() {
      System.out.print(this._prompt + ">> ");
      System.out.flush();
      if (this._log && this._logFile != null)
         SessionLogger.logPartialEntry(this._prompt + ">> ");
   }

   /**
    * Handles results for each transaction
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when network
    *            communication fail
    */
   protected final void _getResults() throws SessionException {
      this._getResults(null);
   }

   /**
    * Handles results for each transaction. If the input system command argument
    * is not null, then it executes the system command for each result object with
    * errno equal to Constants.OK.
    * 
    * @param cmd the input command string
    * @throws SessionException when network communicate fail.
    */
   protected final void _getResults(String cmd) throws SessionException {
      Result result;
      while ((result = this._session.result()) != null) {
         this._writeTTYLine(result.getMessage());
         if (result.getErrno() == Constants.OK)
         {
             //if cmd provided and result has name, substitute then execute
             if (cmd != null && result.getName() != null) {
                String cmdStr = InvocationCommandUtil.buildCommand( 
                                    cmd, this._currentDir, result);                                     
                this._writeTTYLine("Invoke command \'" + cmdStr + "\'");
                Errno error = SystemProcess.execute(cmdStr);
                this._writeTTYLine(error.getMessage());
             }
         }
         result.commit();
      }
   }

   /**
    * Process "help" command
    */
   public final void help() {
      this._startTime = 0; // Don't time this command.
      if (!this._tokenizer.hasMoreTokens())
         this._writeTTYLine(ClientHelp.getInfo(this._admin, null));
      else
         this._writeTTYLine(ClientHelp.getInfo(this._admin, this._tokenizer
               .nextToken()));
   }

   /**
    * Process pause command. This command is used just for demos.
    */
   public final void pause() {
      this._startTime = 0; // Don't time this command.
      if (this._batch)
         this._readTTYLine();
   }

   /**
    * Method to close Komodo fileTypes and exit.
    */
   public void exit() {
      this._done = true;
      try {
         if (this._session != null)
            this._session.close();
      } catch (SessionException se) {
         se.printStackTrace(); // Should never happen, so just show stack.
      }

      if (this._toBatch) {
         this._pw.close();
         this._toBatch = false;
         this._writeTTYLine("Closed commands file " + this._commandsFile);
      }

      if (this._logFile != null)
         SessionLogger.closeLogFile();

      System.exit(0);
   }

   /**
    * Handles errors while taking into account batching
    * 
    * @param msg error message to user
    */
   protected final void _handleError(String msg) {
      this._logger.debug("Handling error");
      if (msg != null) {
         this._logger.debug("error: " + msg);
         this._writeTTYLine(msg);
      }
      this._startTime = 0; // Don't print out timing info on error.
      if (this._batch && this._abort) {
         this._batch = false;
         this._writeTTYLine("Batch execution aborted");
         try {
            if (this._currentBatchFile != null)
               this._currentBatchFile.close();
         } catch (IOException ie) {
            this._logger.error(ie.getMessage());
         }
      }
   }

   /**
    * Set version string
    * 
    * @param version the new version string
    */
   protected final void _setVersion(String version) {
      this._version = version;
   }

   /**
    * Constructs a date from hh:mm AM|PM format
    * 
    * @param datetime input datetime string
    * @return a new converted Date object
    */
   protected final Date _getDate(String datetime) {
      try {
         //Get current date
         Date now = new Date();

         //Set up date formatting
         //TimeZone tz = TimeZone.getDefault();
         //this._writeTTYLine("Client TimeZone: " + tz.getDisplayName());
         
         TimeZone tz = TimeZone.getDefault();
         String tzName = tz.getDisplayName(true, DateFormat.LONG);
         this._writeTTYLine("Client Time Zone: " + tzName);
         SimpleDateFormat longFormat = new SimpleDateFormat(
                                       "yyyy-MM-dd'T'HH:mm:ss.SSS z", 
                                       new Locale("en", "US"));
         longFormat.setTimeZone(tz);
         SimpleDateFormat shortFormat = new SimpleDateFormat("yyyy-MM-dd",
                                        new Locale("en", "US"));
         shortFormat.setTimeZone(tz);         

         // Output current date on FEI server hardware
         this._writeTTYLine("The current date is: " + longFormat.format(now));

         // Parse datetime input
         int hrs = (new Integer(datetime.substring(0, datetime.indexOf(':')))
               .intValue());
         int mins = (new Integer(datetime.substring(datetime.indexOf(':') + 1,
               datetime.indexOf(':') + 3)).intValue());

         boolean isPm = (datetime.endsWith("PM") || datetime.endsWith("pm"));
         
         if (isPm && hrs < 12)
                 hrs += 12;
         if (!isPm  && hrs == 12)         
                 hrs = 0;
         
         Date then = longFormat.parse(shortFormat.format(now) + "T" + hrs + ":"
                     + mins + ":00.000 " + tzName);
         this._logger.debug("Possible planned date: "+longFormat.format(then));
         //Check if this date's already passed
         if (then.before(now)) {
            //If yes, add 24 hours
            long ll = then.getTime() + 86400000;
            then = new Date(ll);
            this._logger.debug("Correct planned date: " +
                               longFormat.format(then));
         }
         this._writeTTYLine("Batch file will first execute on " +
                            longFormat.format(then));
         return then;
      } catch (ParseException pe) {
         this._handleError("Date parsing exception " + pe.getMessage());
         return null;
      }
   }

   /**
    * Inner class to handle timed batch jobs
    */
   protected class ScheduledBatch extends TimerTask {
      private String _fileName;

      /**
       * Constructor
       * 
       * @param fileName The file name
       */
      ScheduledBatch(String fileName) {
         this._fileName = fileName;
      }

      /**
       * Run method
       * 
       * @see java.lang.Runnable#run()
       */
      public void run() {
         CLProcessor.this._writeTTYLine("[" + DateTimeUtil.getTimestamp()
               + "] Scheduled batch execution start");
         //We had set "done" to true to force exit from processInput()
         //in order for timer to finish one run. Now we must reset it
         //in order to loop
         CLProcessor.this._done = false;
         CLProcessor.this._startBatch(this._fileName);
         CLProcessor.this.processInput();
         CLProcessor.this._writeTTYLine("[" + DateTimeUtil.getTimestamp()
               + "] Scheduled batch execution end");
      }
   }
    
   //---------------------------------------------------------------------
   //---------------------------------------------------------------------
  
   /**
    * Shutdown hook implementation to logout of session prior
    * to JVM exiting.
    */
   
   protected class ShutDownHandler extends Thread
   {
       public ShutDownHandler()
       {
           _logger.debug("Instantiated shutdown handler.");
       }
              
       //-----------------------------------------------------------------
       
       public void run() {

           //----------------------
           
           //kill session
           if (_session != null)
           {
               _connections.clear();
               _session.closeImmediate();               
               _logger.debug("Session closed.");
           }
           
           //----------------------
       }        
   }
   
   //---------------------------------------------------------------------
   //---------------------------------------------------------------------
  
}
