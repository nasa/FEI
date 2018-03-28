/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.client;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;

import jpl.mipl.mdms.FileService.komodo.api.Admin;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.help.ClientHelp;
import jpl.mipl.mdms.FileService.komodo.util.CapsUtil;
import jpl.mipl.mdms.FileService.komodo.util.FileLocksUtil;
import jpl.mipl.mdms.FileService.util.Command;
import jpl.mipl.mdms.FileService.util.ConsolePassword;
import jpl.mipl.mdms.pwdclient.PWDClient;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * This class implements the Administrator client side command line interpreter
 * 
 * @author G. Turek
 * @version $Id: ACLProcessor.java,v 1.39 2013/03/30 00:06:20 ntt Exp $
 */
public class ACLProcessor extends CLProcessor {
   private Admin _admin;
   private String[] _tmp;
   private Logger _logger = Logger.getLogger(ACLProcessor.class.getName());

   /**
    * Constructor
    * 
    * @param ssl SSL flag set to true to enable SSL support
    * @param batchFileName batch file name
    * @param silent true if client invoked with -b
    * @throws Exception command processing failure
    */
   public ACLProcessor(boolean ssl, String batchFileName, boolean silent,
                       boolean exitAfterBatch) throws Exception
   {
      super(true, ssl, batchFileName, silent, exitAfterBatch);
      
      this._mapUserCommands();

      if (batchFileName == null) 
      {
         // Removed login from startup sequence on 9/29/03
         //this.login();
         this.processInput();
      } 
      else 
      {
         if (batchFileName.indexOf(File.separator) < 0)
            batchFileName = this._currentDir + File.separator + batchFileName;
         this._startBatch(batchFileName);
         this.processInput();
      }
   }

   /**
    * Map command aliases, regular expressions into methods.
    * 
    * @throws Exception general exception when failed to create command map
    */
   private void _mapUserCommands() throws Exception {
      Command[] commands = {
            new Command(this, new String[] { "addFileType" }, "addFileType",
                  ClientHelp.getUsage("addFileType"), null,
                  new Matcher[] { this._tenArgWithQuotedString }),
            new Command(this, new String[] { "addFileTypeToRole" },
                  "addFileTypeToRole",
                  ClientHelp.getUsage("addFileTypeToRole"), null,
                  new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "addRole" }, "addRole", ClientHelp
                  .getUsage("addRole"), null, 
                  new Matcher[] { this._twoArg, this._threeArg }),
            new Command(this, new String[] { "addUser" }, "addUser", ClientHelp
                  .getUsage("addUser"), null, null),
            new Command(this, new String[] { "addUserToRole" },
                  "addUserToRole", ClientHelp.getUsage("addUserToRole"), null,
                  new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "changePassword" },
                  "changePassword", ClientHelp.getUsage("changePassword"),
                  null, new Matcher[] { this._noArg }),
            new Command(this, new String[] { "connect" }, "connect", ClientHelp
                  .getUsage("connect"), null, new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "connections" }, "connections",
                  ClientHelp.getUsage("connections"), null,
                  new Matcher[] { this._noArg }),
            new Command(this, new String[] { "delFileType" }, "delFileType",
                  ClientHelp.getUsage("delFileType"), null,
                  new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "delFileTypeFromRole" },
                  "delFileTypeFromRole", ClientHelp
                        .getUsage("delFileTypeFromRole"), null,
                  new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "delRole" }, "delRole", ClientHelp
                  .getUsage("delRole"), null, new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "delUser" }, "delUser", ClientHelp
                  .getUsage("delUser"), null, new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "delUserFromRole" },
                  "delUserFromRole", ClientHelp.getUsage("delUserFromRole"),
                  null, new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "dSync" }, "dSync", ClientHelp
                  .getUsage("dSync"), null, new Matcher[] { this._noArg,
                  this._oneArg, this._oneArgWithDate }),
            new Command(this, new String[] { "exit", "lo", "quit", "bye" },
                  "exit", ClientHelp.getUsage("exit"), null,
                  new Matcher[] { this._noArg }),
            new Command(this, new String[] { "focus" }, "focus", ClientHelp
                  .getUsage("focus"), null, new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "fSync" }, "fSync", ClientHelp
                  .getUsage("fSync"), null, new Matcher[] { this._noArg,
                  this._oneArg, this._oneArgWithDate }),
            new Command(this, new String[] { "hotboot" }, "hotboot", ClientHelp
                  .getUsage("hotboot"), null, null),
            new Command(this, new String[] { "login" }, "login", ClientHelp
                  .getUsage("login"), null, null),
                  
            new Command(this, new String[] { "modifyFileType" }, "modifyFileType", ClientHelp
                  .getUsage("modifyFileType"), null, new Matcher[] { this._threeArg }),
                  
            new Command(this, new String[] { "move" }, "move", ClientHelp
                  .getUsage("move"), null, new Matcher[] { this._threeArg }),
            new Command(this, new String[] { "showConnections" },
                  "showConnections", ClientHelp.getUsage("showConnections"),
                  null, new Matcher[] { this._noArg }),
            new Command(this, new String[] { "showFileTypes" },
                  "showFileTypes", ClientHelp.getUsage("showFileTypes"), null,
                  new Matcher[] { this._noArg, this._oneArg }),
            new Command(this, new String[] { "showMemory" }, "showMemory",
                  ClientHelp.getUsage("showMemory"), null,
                  new Matcher[] { _noArg }),
            new Command(this, new String[] { "showServerParameters" },
                  "showServerParameters", ClientHelp
                        .getUsage("showServerParameters"), null, new Matcher[] {
                        this._noArg, this._oneArg }),
            new Command(this, new String[] { "showServers" }, "showServers",
                  ClientHelp.getUsage("showServers"), null,
                  new Matcher[] { this._noArg }),
            new Command(this, new String[] { "showRoles" }, "showRoles",
                  ClientHelp.getUsage("showRoles"), null, new Matcher[] {
                        this._noArg, this._oneArg }),
            new Command(this, new String[] { "showRolesForFileType" },
                  "showRolesForFileType", ClientHelp
                        .getUsage("showRolesForFileType"), null, new Matcher[] {
                        this._oneArg, this._twoArg }),
            new Command(this, new String[] { "showFiletypesForRole" },
                   "showFiletypesForRole", ClientHelp
                        .getUsage("showFiletypesForRole"), null, new Matcher[] {
                        this._oneArg, this._twoArg }),
            new Command(this, new String[] { "showRolesForUser" },
                  "showRolesForUser", ClientHelp.getUsage("showRolesForUser"),
                  null, new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "showUsersForRole" },
                  "showUsersForRole", ClientHelp.getUsage("showUsersForRole"),
                  null, new Matcher[] { this._noArg, this._oneArg }),
            new Command(this, new String[] { "showUsers" }, "showUsers",
                  ClientHelp.getUsage("showUsers"), null, new Matcher[] {
                        this._noArg, this._oneArg }),
            new Command(this, new String[] { "shutdown" }, "shutdown",
                  ClientHelp.getUsage("shutdown"), null,
                  new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "showLocks" }, "showLocks",
                  ClientHelp.getUsage("showLocks"), null,
                   new Matcher[] { this._noArg,   this._oneArg, 
                                 this._twoArg,  this._threeArg }) ,
            new Command(this, new String[] { "logMessage" }, "logMessage",
                  ClientHelp.getUsage("logmessage"), null,
                  new Matcher[] { this._noArgWithQuotedString }),
            new Command(this, new String[] { "modifyRole" }, "modifyRole",
                  ClientHelp.getUsage("modifyRole"), null,
                  new Matcher[] { this._threeArg }),
            new Command(this, new String[] { "modifyUserAccess" }, "modifyUserAccess",
                  ClientHelp.getUsage("modifyUserAccess"), null,
                  new Matcher[] { this._threeArg }),
            new Command(this, new String[] { "setLock" }, "setLock",
                  ClientHelp.getUsage("setLock"), null,
                  new Matcher[] { this._threeArg })};
      this._interpreter.setMaxCharMatch(14);
      this._interpreter.loadCommands(commands);
   }

   /**
    * Method to processes "addFileTypeToRole" command
    * 
    * @throws SessionException when add file type role session fail
    */
   public final void addFileTypeToRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._tmp = new String[2];
         this._tmp[0] = this._twoArg.group(2); //type
         this._tmp[1] = this._twoArg.group(3); //role
         this._admin.addFileTypeToRole(this._tmp);
         this._getResults();
      }
   }

   /**
    * Processes "addUser" command. If in batch mode parses line for arguments,
    * but otherwise prompts for each argument
    * 
    * @throws SessionException when processing session fail
    */
   public final void addUser() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (this._batch) {
         if (this._tokenCount < 3) {
            this
                  ._handleError("Usage: addUser <name> <password> [<privilege>] [\"p\"]");
            return;
         }

         this._tmp = new String[4];
         this._tmp[0] = _tokenizer.nextToken(); //name
         this._tmp[1] = _tokenizer.nextToken(); //passwd
         this._tmp[2] = null; // admin, writeAll, or readAll
         this._tmp[3] = ""; // addVFT

         if (this._tokenizer.hasMoreTokens()) {
            String token = this._tokenizer.nextToken();
            if (token.equals("a") || token.equals("r") || token.equals("w"))
               this._tmp[2] = token;
            else if (token.equals("p"))
               this._tmp[3] = token;
            else {
               this._writeTTYLine("Unrecognized privilege: " + token);
               this
                     ._writeTTYLine("Must be one of \"a\", \"r\", \"p\" or \"w\"");
               return;
            }
         }

         if (this._tokenizer.hasMoreTokens()) {
            String token = this._tokenizer.nextToken();
            if (token.equals("p"))
               this._tmp[3] = token;
            else {
               this._writeTTYLine("Unrecognized vft privilege: " + token);
               this._writeTTYLine("Must be \"p\"");
               return;
            }
         }

         if (!this._test) {
            this._admin.addUser(this._tmp);
            _getResults();
         }
      } else {
         String n, p, p1, p2, p3;
         boolean end = false;
         while (!end) {
            int icnt = 3;
            n = null;
            p = null;
            p1 = null;
            p2 = null;
            p3 = null;

            System.out.print("User name: ");
            System.out.flush();
            n = this._readTTYLine(); //name

            boolean ok = false;
            while (!ok) 
            {               
               try {
                  p1 = ConsolePassword.getPassword("Enter password: ");
                  if (p1.equals(""))
                  {
                      this._handleError("Password cannot be empty, try again");
                      continue;
                  }                  
                  p2 = ConsolePassword.getPassword("Re-enter password: ");
                  if (p1.equals(p2))
                     ok = true;
                  else
                     this._handleError("Password does not match, try again");
               } catch (IOException e) {
                  this._handleError("Unable to read input.  try again");
               }
            }
            String encryptedPass = null;
            try { 
                encryptedPass = this._encrypter.encrypt(p1);
            } catch (Exception ex) {
                this._handleError("Unable to encrypt password.  try again");
            }
            

            ok = false;
            while (!ok) {
               System.out.print("User privileges: ");
               System.out.flush();
               p = this._readTTYLine(); //privileges
               if (p.equals("")) {
                  icnt = 2;
                  ok = true;
               } else if (!p.equals("a") && !p.equals("r") && !p.equals("w")) {
                  this._writeTTYLine("Unrecognized privilege: " + p);
                  this._writeTTYLine("Must be one of \"a\", \"r\" or \"w\"");
               } else
                  ok = true;
            }

            ok = false;
            while (!ok) {
               System.out.print("Add vft privilege [p|<cr>]: ");
               System.out.flush();
               p3 = this._readTTYLine(); //privileges
               if (p3.equalsIgnoreCase("p")) {
                  ok = true;
                  icnt = 4;
               } else if (!p3.equals("")) {
                  this._writeTTYLine("Invalid reply.  Must be <cr>, p.");
               } else
                  ok = true;
            }
            this._tmp = new String[icnt];
            this._tmp[0] = n;
            //this._tmp[1] = p1;
            this._tmp[1] = encryptedPass; 
            if (icnt >= 3)
               this._tmp[2] = p;
            if (icnt == 4)
               this._tmp[3] = p3;
            if (!n.equals("") && !p1.equals("")) {
               this._admin.addUser(_tmp);
               this._getResults();
            }
            System.out.print("Add another user? (Y/N) ");
            System.out.flush();
            String reply = this._readTTYLine();
            if (reply.equals("") || !reply.equalsIgnoreCase("Y"))
               end = true;
         }
      }
   }

   /**
    * Processes "addUserToRole" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void addUserToRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._tmp = new String[2];
         this._tmp[0] = this._twoArg.group(2); //name
         this._tmp[1] = this._twoArg.group(3); //role
         this._admin.addUserToRole(this._tmp);
         this._getResults();
      }
   }

   /**
    * Change a user's password. Used by Komodo administrator to reset a password
    * for a user who has forgotten it.
    * 
    * @throws SessionException when processing session fail
    */
   public void changePassword() throws SessionException {
      String userName = null;
      String newPassword = null;
      String verify = null;

      if (this._admin == null) {
         this._handleError("Not connected.");
         return;
      }

      System.out.print("Enter user name: ");
      userName = this._readTTYLine();
      boolean ok = false;
      try {
         while (!ok) 
         {
            newPassword = ConsolePassword
                  .getPassword("Enter new password (or type \"abort\" to quit): ");
            if (newPassword.equalsIgnoreCase("abort"))
               return;
            if (newPassword.equals(""))
            {
                this._handleError("Password cannot be empty, try again");
                continue;
            }
                
            verify = ConsolePassword.getPassword("Re-enter password: ");
            if (newPassword.equals(verify))
               ok = true;
            else
               this._handleError("Password does not match, try again");
         }
         
         String encryptedPass = null;
         try { 
             encryptedPass = this._encrypter.encrypt(newPassword);
         } catch (Exception ex) {
             this._handleError("Unable to encrypt password.  try again");
         }
         
         /* Now, change the password for our Komodo user. */
         this._admin.changePassword(userName, encryptedPass);
         this._getResults();
      } catch (IOException e) {
         this._handleError("Failed to change password. " + e.getMessage());
      }
   }

   /**
    * Processes "connect" command
    */
   public final void connect() {
      this._logger.debug("process connect");
      //Connect to a [new] server
      this._newConn = this._oneArg.group(2); //the server name

      String group = null;
      
      try {
          group = this._session.getServerGroup(this._newConn);
      } catch (SessionException se) {
         this._writeTTYLine(se.getMessage());
      }
      
      if (!this._session.isLoggedOn())
          this.loginUsingCache(group);
      if (!this._session.isLoggedOn())
          this.login(group);
      
      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      try {
         
         if (group == null) {
            this._writeTTYLine("ERROR: Server [" + this._newConn
                  + "] not found.");
            return;
         }
         this._checkAuth(group);
      } catch (SessionException se) {
         this._writeTTYLine(se.getMessage());
      }

      this._logger.debug("Connect to server " + this._newConn);
      if (this._connections.containsKey(this._newConn)) {
         this._logger.debug("new connection");
         this._admin = (Admin) this._connections.get(this._newConn);
         this._prompt = this._newConn;
         this._writeTTYLine("Admin connection set to : " + this._newConn);
      } else {
         this._logger.debug("new connection");
         try {
             if (!this._session.isLoggedOn())
                 this.loginUsingCache(group);
            this._admin = this._session.openAdmin(this._newConn);
            this._connections.put(this._newConn, this._admin);
            this._prompt = this._newConn;
            this._writeTTYLine("Admin connection set to : " + this._newConn);
         } catch (SessionException e) {
            this._writeTTYLine(e.getMessage());
         }
      }
   }

   /**
    * Processes "connections" command
    */
   public final void connections() {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }
      this._writeTTYLine("Currently connected to:");
      for (Enumeration e = this._connections.keys(); e.hasMoreElements();) {
         this._writeTTYLine("    " + (String) e.nextElement());
      }
   }

   /**
    * Processes "dSync" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void dSync() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      Date date = null;
      String type = null;
      String tmp1;

      if (this._oneArgWithDate.matches()
            && this._oneArgWithDate.group(1) != null) {
         type = this._oneArgWithDate.group(2);
         tmp1 = this._oneArgWithDate.group(3);
         // We must verify the date format
         // otherwise, syntax checking is comprimised.
         try {
            date = this._dateFormatter.parseDate(tmp1);  
                //DateTimeUtil.getCCSDSADate(tmp1);
         } catch (ParseException e) {
            this._handleError(e.getMessage());
            return;
         }
      } else if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         type = this._oneArg.group(2);
      }

      if (!this._test) {
         this._admin.dSync(type, date);
         this._getResults();
      }
   }

   /**
    * Processes "focus" command
    */
   public final void focus() {
      //Change server
      this._newConn = this._oneArg.group(2);
      if (this._connections.containsKey(this._newConn)) {
         this._logger.debug("old connection");

         this._admin = (Admin) this._connections.get(this._newConn);
         this._prompt = this._newConn;
         this._writeTTYLine("Admin connection set to : " + this._newConn);
      } else {
         this._logger.debug("new connection");
         this._writeTTYLine("Not connected to : " + this._newConn);
         
         String group = null;
         try {
             group = this._session.getServerGroup(this._newConn);
         } catch (SessionException sesEx) {
             
         }
         
         //try cache first, then use manual approach
         this.loginUsingCache(group);
         if (!this._session.isLoggedOn())
             this.login(group);
      }
   }

   /**
    * Processes "fSync" command. The file sync command to make sure files are
    * being written to disk and not being buffered in memory by in network.
    * 
    * @throws SessionException when processing session fail
    */
   public final void fSync() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      Date date = null;
      String type = null;
      String tmp1;

      if (this._oneArgWithDate.matches()
            && this._oneArgWithDate.group(1) != null) {
         type = this._oneArgWithDate.group(2);
         tmp1 = this._oneArgWithDate.group(3);
         // We must verify the date format
         // otherwise, syntax checking is comprimised.
         try {
            date = this._dateFormatter.parseDate(tmp1); 
                   //DateTimeUtil.getCCSDSADate(tmp1);
         } catch (ParseException e) {
            this._handleError(e.getMessage());
            return;
         }
      } else if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         type = this._oneArg.group(2);
      }
      if (!this._test) {
         this._admin.fSync(type, date);
         this._getResults();
      }
   }

   /**
    * Processes "hotboot" command similar to SIG HUP in UNIX.
    * 
    * @throws SessionException when processing session fail
    */
   public final void hotboot() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._admin.hotboot();
         this._getResults();
      }
   }

   /**
    * Process admin login and reinitialize internal admin object reference.
    */
   public final void loginHiddenToUncoverCallers() {
      super.loginHiddenToUncoverCallers();
      this._admin = null;
   }

   /**
    * Process admin login and reinitialize internal admin object reference.
    */
   public final void login(String servergroup) {
      super.login(servergroup);
      this._admin = null;
   }
   
   /**
    * Process admin login using login file and reinitialize internal 
    * admin object reference.
    */
   public final void loginUsingCache(String servergroup) {
       try {
           super.loginUsingCache(servergroup);
           this._admin = null;
       } catch (Exception e) {
          //this._writeTTYLine("Unable to process user login.  Use manual login.");
       }       
    }
   
   /**
    * Processes "addFileType" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void addFileType() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      this._tmp = new String[10];
      this._tmp[0] = this._tenArgWithQuotedString.group(2); //file type
      this._tmp[1] = this._tenArgWithQuotedString.group(3); //directory
      this._tmp[2] = "\"" + this._tenArgWithQuotedString.group(4) + "\"";
      //comment
      this._tmp[3] = this._tenArgWithQuotedString.group(5); //reserved
      this._tmp[4] = this._tenArgWithQuotedString.group(6); //threshold
      this._tmp[5] = this._tenArgWithQuotedString.group(7); //qainterval
      this._tmp[6] = this._tenArgWithQuotedString.group(8).toLowerCase();
      //checksum
      this._tmp[7] = this._tenArgWithQuotedString.group(9).toLowerCase();
      //logDeleteRecord      
      this._tmp[8] = this._tenArgWithQuotedString.group(10).toLowerCase();
      //receipt
      this._tmp[9] = this._tenArgWithQuotedString.group(11).toLowerCase();
      //xmlschema
      
      this._logger.debug(this._tmp[0]);
      this._logger.debug(this._tmp[1]);
      this._logger.debug(this._tmp[2]);
      this._logger.debug(this._tmp[3]);
      this._logger.debug(this._tmp[4]);
      this._logger.debug(this._tmp[5]);
      this._logger.debug(this._tmp[6]);
      this._logger.debug(this._tmp[7]);
      this._logger.debug(this._tmp[8]);
      this._logger.debug(this._tmp[9]);

      if (!this._tmp[3].equalsIgnoreCase("null")) {
         try {
            Integer.parseInt(this._tmp[3]);
         } catch (NumberFormatException nfe) {
            this._handleError("Invalid integer value: " + this._tmp[3]);
            return;
         }
      }

      if (!this._tmp[4].equalsIgnoreCase("null")) {
         try {
            Integer.parseInt(_tmp[4]);
         } catch (NumberFormatException nfe) {
            this._handleError("Invalid integer value: " + this._tmp[4]);
            return;
         }
      }

      if (!this._tmp[5].equalsIgnoreCase("null")) {
         try {
            Integer.parseInt(this._tmp[5]);
         } catch (NumberFormatException nfe) {
            this._handleError("Invalid integer value: " + this._tmp[4]);
            return;
         }
      }

      if (!this._tmp[6].equals("t") && !this._tmp[6].equals("f")
            && !this._tmp[6].equals("null")) {
         this
               ._writeTTYLine("Checksum entry can only be \"t\" or \"f\" or \"null\"");
         return;
      }

      if (!this._tmp[7].equals("t") && !this._tmp[7].equals("f")
              && !this._tmp[7].equals("null")) {
           this._writeTTYLine("logDeleteRecord entry can only be \"t\" or \"f\" or \"null\"");
           return;
      }
      
      if (!this._tmp[8].equals("t") && !this._tmp[8].equals("f")
              && !this._tmp[8].equals("null")) {
           this._writeTTYLine("receipt entry can only be \"t\" or \"f\" or \"null\"");
           return;
      }
      
      if (!this._tmp[9].equals("t") && !this._tmp[9].equals("f")
            && !this._tmp[9].equals("null")) {
         this._writeTTYLine("xmlSchema entry can only be \"t\" or \"f\" or \"null\"");
         return;
      }

      if (!this._test) {
         this._admin.addFileType(this._tmp);
         this._getResults();
      }
   }

   /**
    * Processes "addRole" command to add a new user role
    * 
    * @throws SessionException when processing session fail
    */
   public final void addRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not logged in");
         return;
      }

      if (this._tokenCount < 3) {
         this._handleError("Usage: addRole <access role> <capabilities list> [<external role>]");
         return;
      }

      this._tmp = new String[3];
      this._tmp[0] = this._tokenizer.nextToken(); //role
      this._logger.debug("Role : " + this._tmp[0]);

      StringTokenizer captoken = new StringTokenizer(this._tokenizer
            .nextToken(), ",");

      Vector caps = new Vector();
      while (captoken.hasMoreTokens()) {
         caps.add(captoken.nextToken());
      }
      
    
      int icaps = -1;
      try {
         icaps = CapsUtil.getCapabilitiesAsInt(caps);
         this._tmp[1] = Integer.toString(icaps); //capabilities
         this._logger.debug("Capabilities : " + this._tmp[1]);
      } catch (NoSuchFieldException nsf) {
         this._writeTTYLine(nsf.getMessage());
         this._writeTTYLine("Allowed: add,archive,delete,get,locktype,offline,");
         this._writeTTYLine("         push-subscribe,vft,qaaccess,receipt,register,");
         this._writeTTYLine("         rename,replace,replicate,subtype");
         return;
      }

      
      if (this._tokenizer.hasMoreTokens())
      {
          String[] newTmp = new String[3];
          newTmp[0] = this._tmp[0];
          newTmp[1] = this._tmp[1];
          newTmp[2] = this._tokenizer.nextToken();
          this._tmp = newTmp;
          this._logger.debug("External Role : " + this._tmp[2]);
      }
      
      
      if (!this._test) {
         this._admin.addRole(this._tmp);
         this._getResults();
      }
   }

   /**
    * Processes "delRole" command to remove a user role
    * 
    * @throws SessionException when processing session fail
    */
   public final void delRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         String tmp = this._oneArg.group(2); //role
         this._admin.delRole(tmp);
         this._getResults();
      }
   }

   /**
    * Processes "delUser" command to remove a user.
    * 
    * @throws SessionException when processing session fail
    */
   public final void delUser() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not logged in");
         return;
      }

      if (!this._test) {
         String tmp = this._oneArg.group(2); //name
         this._admin.delUser(tmp);
         this._getResults();
      }
   }

   /**
    * Processes "delFileType" command to remove file type
    * 
    * @throws SessionException when processing session fail
    */
   public final void delFileType() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         String tmp = this._oneArg.group(2); //type
         this._admin.delFileType(tmp);
         this._getResults();
      }
   }

   /**
    * Processes "delFileTypeFromRole" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void delFileTypeFromRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._tmp = new String[2];
         this._tmp[0] = this._twoArg.group(2); //type
         this._tmp[1] = this._twoArg.group(3); //role
         this._admin.delFileTypeFromRole(this._tmp);
         this._getResults();
      }
   }

   /**
    * Processes "delUserFromRole" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void delUserFromRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._tmp = new String[2];
         this._tmp[0] = this._twoArg.group(2); //name
         this._tmp[1] = this._twoArg.group(3); //role
         this._admin.delUserFromRole(this._tmp);
         this._getResults();
      }
   }

   /**
    * Processes "showConnections" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showConnections() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._admin.showConnections();
         this._getResults();
      }
   }

   /**
    * Processes "showConnections" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showFileTypes() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         if (this._oneArg.matches() && this._oneArg.group(1) != null) {
            String tmp1 = this._oneArg.group(2); // file type
            this._admin.showFileTypes(tmp1);
         } else if (this._noArg.matches() && this._noArg.group(1) != null) {
            this._admin.showFileTypes(null);
         }
         this._getResults();
      }
   }

   /**
    * Processes "showMemory" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showMemory() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._admin.showMemory();
         this._getResults();
      }
   }
   
   /**
    * Processes "showLocks" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showLocks() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) 
      {
          String[] args = null;
          
          if (this._oneArg.matches() && this._oneArg.group(1) != null) 
          {
              String filetypeExpr = this._oneArg.group(2); // file type
              args = new String[] {filetypeExpr};
          } 
          else if (this._twoArg.matches() && this._twoArg.group(1) != null
                  && _twoArg.group(2) != null)              
          {
              String filetypeExpr = this._twoArg.group(2); // file type
              String fileExpr     = this._twoArg.group(3); // file
              args = new String[] {filetypeExpr,fileExpr};
          }
          else if (this._threeArg.matches() && this._threeArg.group(1) != null
                   && _threeArg.group(2) != null && _threeArg.group(3) != null)              
          {
              String filetypeExpr = this._threeArg.group(2); // file type
              String fileExpr     = this._threeArg.group(3); // file
              String lockValue    = this._threeArg.group(4); // lock value
              args = new String[] {filetypeExpr,fileExpr,lockValue};
          }
          else if (this._noArg.matches() && this._noArg.group(1) != null) 
          {
              args = new String[] {};
          }
          
          this._admin.showLocks(args);
          this._getResults();
       }
      
   }

   /**
    * Processes "showServerParameters" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showServerParameters() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         if (this._oneArg.matches() && this._oneArg.group(1) != null) {
            String tmp1 = this._oneArg.group(2); // server name
            this._admin.showServerParameters(tmp1);
         } else if (this._noArg.matches() && this._noArg.group(1) != null) {
            this._admin.showServerParameters(null);
         }
         this._getResults();
      }
   }

   /**
    * Processes "showServers" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showServers() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }
      if (!this._test) {
         this._admin.showServers();
         this._getResults();
      }
   }

   /**
    * Processes "showRoles" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showRoles() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         if (this._oneArg.matches() && this._oneArg.group(1) != null) {
            String tmp1 = this._oneArg.group(2); // role
            this._admin.showRoles(tmp1);
         } else if (this._noArg.matches() && this._noArg.group(1) != null) {
            this._admin.showRoles(null);
         }
         this._getResults();
      }
   }

   /**
    * Processes "showRolesForFileType" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showRolesForFileType() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         if (this._twoArg.matches() && this._twoArg.group(1) != null) {
            this._tmp = new String[2];
            this._tmp[0] = this._twoArg.group(2); // file type
            this._tmp[1] = this._twoArg.group(3); // role
         } else if (this._oneArg.matches() && this._oneArg.group(1) != null) {
            this._tmp = new String[1];
            this._tmp[0] = this._oneArg.group(2); // file type
         }
         this._admin.showRolesForFileType(this._tmp);
         this._getResults();
      }
   }

   /**
    * Processes "showFiletypesForRoles" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showFiletypesForRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         if (this._twoArg.matches() && this._twoArg.group(1) != null) {
            this._tmp = new String[2];
            this._tmp[0] = this._twoArg.group(2); // role
            this._tmp[1] = this._twoArg.group(3); // filetype
         } else if (this._oneArg.matches() && this._oneArg.group(1) != null) {
            this._tmp = new String[1];
            this._tmp[0] = this._oneArg.group(2); // role
         }
         this._admin.showFiletypesForRole(this._tmp);
         this._getResults();
      }
   }
   
   /**
    * Processes "showRolesForUser" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showRolesForUser() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         this._tmp = new String[1];
         this._tmp[0] = this._oneArg.group(2); // User
         this._admin.showRolesForUser(this._tmp);
         this._getResults();
      }
   }
   
   /**
    * Processes "showUsersForRole" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showUsersForRole() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) 
      {
          if (this._oneArg.matches() && this._oneArg.group(1) != null) 
          {
              this._tmp = new String[1];
              this._tmp[0] = this._oneArg.group(2); // Role
              this._admin.showUsersForRoles(this._tmp);
          }
          else if (this._noArg.matches() && this._noArg.group(1) != null) 
          {
              this._admin.showUsersForRoles(null);
          }       
          this._getResults();
      }
   }

   /**
    * Processes "showUsers" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void showUsers() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         if (this._oneArg.matches() && this._oneArg.group(1) != null) {
            String tmp1 = this._oneArg.group(2); // name
            this._admin.showUsers(tmp1);
         } else if (this._noArg.matches() && this._noArg.group(1) != null) {
            this._admin.showUsers(null);
         }
         this._getResults();
      }
   }

   /**
    * Processes "shutdown" command
    * 
    * @throws SessionException when processing session fail
    */
   public final void shutdown() throws SessionException {
      if (this._admin == null) {
         this._handleError("Not connected");
         return;
      }

      if (!this._test) {
         String tmp1 = this._oneArg.group(2); // timeout
         this._admin.shutdown(tmp1);
         this._getResults();
      }
   }

   /**
    * Checks the user authentication in session object, contacts MDMS PWDServer
    * if password is null
    * 
    * @param group Name of FEI server group
    * @throws SessionException when invalid login
    */
   private void _checkAuth(String group) throws SessionException {
      String password = this._session.getPassword();

      //if (group.equalsIgnoreCase("vft")) {
      //   group = this._session.getFTServerGroup("vft");
      //}

      // Check if password is not set, and check MDMS PWDSERVER
      if (password == null || password.equalsIgnoreCase("")) {
         this._logger.debug("Password was null, contacting PWDSERVER.");
         String user = this._session.getUserName();
         this._logger.debug("Server group: " + group);
         PWDClient pc = new PWDClient(user, group);
         try {
            this._session.setLoginInfo(user, pc.getPassword());
         } catch (Exception e) {
            throw new SessionException(e.getMessage(), Constants.INVALID_LOGIN);
         }
      }
   }
   
   /**
    * Checks the user authentication in session object, contacts MDMS 
    * PWDServer if password is null
    * 
    * @param group Name of FEI server group
    * @throws SessionException when invalid login
    */
   public final void move() throws SessionException 
   {
       if (this._admin == null) {
           this._handleError("Not connected");
           return;
       }
       
       //prepare string arguments
       String srcFt = this._threeArg.group(2);
       String dstFt = this._threeArg.group(3);
       String fExpr = this._threeArg.group(4);       
       
       if (srcFt == null || dstFt == null || fExpr == null) {
           this._handleError("Missing required arguments");
           return;
       }       
       String[] args = new String[] {srcFt, dstFt, fExpr};      
       
       //make request
       if (!this._test) {
           this._admin.moveBetweenFileTypes(args, 
                                            this._preserve,
                                            this._replaceFile);
           this._getResults();
       }       
   }
   
   /**
    * Modifies a setting for a filetype.
    * @throws SessionException when invalid login
    */
   public final void modifyFileType() throws SessionException 
   {
       if (this._admin == null) {
           this._handleError("Not connected");
           return;
       }
       
       //prepare string arguments
       String filetype = this._threeArg.group(2);
       String field    = this._threeArg.group(3);
       String value    = this._threeArg.group(4);       
       
       if (filetype == null || field == null || value == null) {
           this._handleError("Missing required arguments");
           return;
       }       
       String[] args = new String[] {filetype, field, value};      
       
       //make request
       if (!this._test) {
           this._admin.modifyFileType(args);
           this._getResults();
       }       
   }
   
   /**
    * Writes a message to server log
    * @throws SessionException when invalid login
    */
   public final void logMessage() throws SessionException 
   {
       if (this._admin == null) {
           this._handleError("Not connected");
           return;
       }
       
       //prepare string arguments
       String message = this._noArgWithQuotedString.group(2);
       
       if (message == null)
           message = ""; 
       
       //make request
       if (!this._test) {
           this._admin.logMessage(message);
           this._getResults();
       }       
   }
   
   /**
    * Modify capabilities associated with a role
    * @throws SessionException when invalid login
    */
   public final void modifyRole() throws SessionException 
   {
       if (this._admin == null) {
           this._handleError("Not connected");
           return;
       }
       
       //prepare string arguments
       String roleName  = this._threeArg.group(2);
       String operation = this._threeArg.group(3);
       String capList   = this._threeArg.group(4);
       
       if (roleName == null || operation == null || capList == null) {
           this._handleError("Missing required arguments");
           return;
       } 
       
       int capsAsInt;
       try {
           capsAsInt = CapsUtil.getCapabilitiesAsInt(capList, ',');
       } catch (NoSuchFieldException nsfEx) {
           String message = "Could not parse capabilities list: "+capList+
                            "\nMessage: "+nsfEx.getMessage();
           throw new SessionException(message, Constants.EXCEPTION);
       }
            
       
       //make request
       if (!this._test) {
           String[] args = new String[] {roleName, operation, Integer.toString(capsAsInt)};
           this._admin.modifyRole(args);
           this._getResults();
       }       
   }
   
   /**
    * Modifies access level associated with a user name
    * @throws SessionException when invalid login
    */
   public final void modifyUserAccess() throws SessionException 
   {
       if (this._admin == null) {
           this._handleError("Not connected");
           return;
       }
       
       //prepare string arguments
       String user   = this._threeArg.group(2);
       String level  = this._threeArg.group(3);
       String flag   = this._threeArg.group(4);
       
       if (user == null || level == null || flag == null) {
           this._handleError("Missing required arguments");
           return;
       } 
       
       if (! (level.equalsIgnoreCase("admin") || level.equalsIgnoreCase("read") ||
              level.equalsIgnoreCase("write") || level.equalsIgnoreCase("vft")))
       {
           this._handleError("Access level should be one of: admin,write,read,vft");
           return;
       }
       
       flag = flag.toLowerCase();
       if (! (flag.equals("on") || flag.equals("off")) )
       {
           if (flag.equals("y") || flag.equals("yes") ||
               flag.equals("true") || flag.equals("t"))
           {
               flag = "on";
           }
           else if (flag.equals("n") || flag.equals("no") || 
                    flag.equals("false") || flag.equals("f"))
           {
               flag = "off";
           }
           else 
           {
               this._handleError("Switch value should be one of: on, off");
               return;
           }               
       }
       
       //make request
       if (!this._test) {
           String[] args = new String[] {user, level, flag};
           this._admin.modifyUserAccess(args);
           this._getResults();
       }       
   }
   
   /**
    * Modifies access level associated with a user name
    * @throws SessionException when invalid login
    */
   public final void setLock() throws SessionException 
   {
       if (this._admin == null) {
           this._handleError("Not connected");
           return;
       }
       
       String filetype = null;
       String filename = null;
       String newLock  = null;

       
       String[] args   = new String[0];
       
       if (this._threeArg.matches() && this._threeArg.group(1) != null)
       {
           filetype = this._threeArg.group(2);
           filename = this._threeArg.group(3);
           newLock  = this._threeArg.group(4);
           
           args = new String[] {filetype, filename, 
                                newLock};
       }
       
       if (filetype == null || filename == null || newLock == null)
       {
           this._handleError("Missing required arguments");
           return;
       }
       
       if (FileLocksUtil.getLockAsInt(newLock) == -1)
           this._handleError("Unknown new lock type: "+newLock);
       
       //make request
       if (!this._test) {
           this._admin.setLocks(args);
           this._getResults();
       }       
   }
   
}