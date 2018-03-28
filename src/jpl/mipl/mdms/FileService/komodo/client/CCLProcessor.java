/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import jpl.mipl.mdms.FileService.komodo.api.Capability;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.api.VFT;
import jpl.mipl.mdms.FileService.komodo.help.ClientHelp;
import jpl.mipl.mdms.FileService.komodo.util.InvocationCommandUtil;
import jpl.mipl.mdms.FileService.util.Command;
import jpl.mipl.mdms.FileService.util.ConsolePassword;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.Errno;
import jpl.mipl.mdms.FileService.util.FileUtil;
import jpl.mipl.mdms.FileService.util.PrintfFormat;
import jpl.mipl.mdms.FileService.util.SystemProcess;
import jpl.mipl.mdms.pwdclient.PWDClient;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * BaseClient side command line interpreter
 * 
 * @author G. Turek, T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: CCLProcessor.java,v 1.71 2013/03/30 00:06:20 ntt Exp $
 */
public class CCLProcessor extends CLProcessor {
   // Access to the file type.
   private FileType _fileType;

   private VFT _vftAccess = null;
   private Logger _logger = Logger.getLogger(CCLProcessor.class.getName());

   /**
    * Constructor
    * 
    * @param ssl if true, then SSL is enabled
    * @param batchFileName the batch file name
    * @param silent if true then client operates in silent mode.
    * @throws Exception general client command processing failure
    */
   public CCLProcessor(boolean ssl, String batchFileName, boolean silent,
                       boolean exitAfterBatch) throws Exception {

      super(false, ssl, batchFileName, silent, exitAfterBatch);
      this._prompt = this._session.getDefaultGroup();
      this._prompt = (this._prompt == null) ? "" : this._prompt + ":";
      this._mapUserCommands();

      if (batchFileName == null) {
         this.processInput();
      } else {
         if (batchFileName.indexOf(File.separator) < 0)
            batchFileName = this._currentDir + File.separator + batchFileName;
         this._startBatch(batchFileName);
         this.processInput();
      }
   }
   /**
    * Map command aliases, regular expressions into methods.
    * 
    * @throws Exception the general map failure
    */
   private void _mapUserCommands() throws Exception {

      Command[] commands = {
            // Note: Overlay the super class's login and exit functions
            // so we can track VFT.
            new Command(this, new String[] { "login" }, "login", ClientHelp
                  .getUsage("login"), null, null),
            new Command(this, new String[] { "changePassword" },
                  "changePassword", ClientHelp.getUsage("changePassword"),
                  null, new Matcher[] { this._noArg }),
            new Command(this, new String[] { "exit", "lo", "quit", "bye" },
                  "exit", ClientHelp.getUsage("exit"), null,
                  new Matcher[] { this._noArg }),
            new Command(this, new String[] { "add", "a" }, "add", ClientHelp
                  .getUsage("add"), null, new Matcher[] { this._oneArg,
                  this._oneArgWithQuotedString }),
            new Command(this, new String[] { "addAndRef" }, "addAndRef",
                  ClientHelp.getUsage("addAndRef"), null, null),
            new Command(this, new String[] { "archive" }, "archive", ClientHelp
                  .getUsage("archive"), null, new Matcher[] { this._oneArg,
                  this._oneArgWithQuotedString }),
            new Command(this, new String[] { "checksum" }, "checksum",
                  ClientHelp.getUsage("checksum"), null,
                  new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "comment", "c" }, "comment",
                  ClientHelp.getUsage("comment"), null,
                  new Matcher[] { this._oneArgWithQuotedString }),
            new Command(this, new String[] { "use" }, "useType", ClientHelp
                  .getUsage("use"), null, new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "delete", "d" }, "delete",
                  ClientHelp.getUsage("delete"), null,
                  new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "get", "g" }, "get", ClientHelp
                  .getUsage("get"), null, new Matcher[] { this._oneArg,
                  this._oneArgWithInvoke }),
            new Command(this, new String[] { "getAfter" }, "getAfter",
                  ClientHelp.getUsage("getAfter"), null, new Matcher[] {
                        this._noArg, this._noArgWithInvoke, this._oneDate,
                        this._oneDateWithInvoke }),
            new Command(this, new String[] { "getBetween" }, "getBetween",
                  ClientHelp.getUsage("getBetween"), null, new Matcher[] {
                        this._twoDate, this._twoDateWithInvoke }),
            new Command(this, new String[] { "getLatest" }, "getLatest",
                  ClientHelp.getUsage("getLatest"), null, new Matcher[] {
                        this._noArg, this._noArgWithInvoke, this._oneArg,
                        this._oneArgWithInvoke }),
            new Command(this, new String[] { "getSince" }, "getSince",
                  ClientHelp.getUsage("getSince"), null, new Matcher[] {
                        this._oneDate, this._oneDateWithInvoke }),
            new Command(this, new String[] { "lockFileType" }, "lockFileType",
                  ClientHelp.getUsage("lockFileType"), null, new Matcher[] {
                        this._noArg, this._oneArg }),
            new Command(this, new String[] { "rename", "n" }, "rename",
                  ClientHelp.getUsage("rename"), null,
                  new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "replace", "r" }, "replace",
                  ClientHelp.getUsage("replace"), null, new Matcher[] {
                        this._oneArg, this._oneArgWithQuotedString }),
            new Command(this, new String[] { "show", "s" }, "show", ClientHelp
                  .getUsage("show"), null, new Matcher[] { this._noArg,
                  this._noArgWithInvoke, this._oneArg, this._oneArgWithInvoke }),
            new Command(this, new String[] { "showAfter" }, "showAfter",
                  ClientHelp.getUsage("showAfter"), null, new Matcher[] {
                        this._noArg, this._noArgWithInvoke, this._oneDate,
                        this._oneDateWithInvoke }),
            new Command(this, new String[] { "showBetween" }, "showBetween",
                  ClientHelp.getUsage("showBetween"), null, new Matcher[] {
                        this._twoDate, this._twoDateWithInvoke }),
            new Command(this, new String[] { "showCapabilities", "showCaps" },
                  "showCapabilities", ClientHelp.getUsage("showCapabilities"),
                  null, new Matcher[] { this._noArg, this._oneArg }),
            new Command(this, new String[] { "showLatest" }, "showLatest",
                  ClientHelp.getUsage("showLatest"), null, new Matcher[] {
                        this._noArg, this._noArgWithInvoke, this._oneArg,
                        this._oneArgWithInvoke }),
            new Command(this, new String[] { "showSince" }, "showSince",
                  ClientHelp.getUsage("showSince"), null, new Matcher[] {
                        this._oneDate, this._oneDateWithInvoke }),
            new Command(this, new String[] { "showTypes" }, "showTypes",
                  ClientHelp.getUsage("showTypes"), null,
                  new Matcher[] { this._noArg }),
            new Command(this, new String[] { "unlockFileType", "unlock" },
                  "unlockFileType", ClientHelp.getUsage("unlockFileType"),
                  null, new Matcher[] { this._oneArg }),
            new Command(this,
                  new String[] { "setDefaultGroup", "defaultGroup" },
                  "setDefaultGroup", ClientHelp.getUsage("setDefaultGroup"),
                  null, new Matcher[] { this._oneArg }),

            /* Setup commands */

            new Command(this, new String[] { "makeDomainFile" },
                  "makeDomainFile", ClientHelp.getUsage("makeDomainFile"),
                  null, new Matcher[] { this._oneArg }),

            /* VFT Commands - */

            new Command(this, new String[] { "getVFT" }, "getVFT", ClientHelp
                  .getUsage("getVFT"), null, new Matcher[] { this._oneArg,
                  this._twoArg }),
            new Command(this, new String[] { "getReference" }, "getReference",
                  ClientHelp.getUsage("getReference"), null, new Matcher[] {
                        this._twoArg, this._threeArg }),
            new Command(this, new String[] { "updateVFT" }, "updateVFT",
                  ClientHelp.getUsage("updateVFT"), null, new Matcher[] {
                        this._oneArg, this._oneArgWithQuotedString }),
            new Command(this, new String[] { "addVFT" }, "addVFT", ClientHelp
                  .getUsage("addVFT"), null, new Matcher[] { this._oneArg,
                  this._oneArgWithQuotedString }),
            new Command(this, new String[] { "addVFTReader" }, "addVFTReader",
                  ClientHelp.getUsage("addVFTReader"), null,
                  new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "showVFT" }, "showVFT", ClientHelp
                  .getUsage("showVFTs"), null, new Matcher[] { this._noArg,
                  this._oneArg, this._oneDate, this._oneArgWithDate }),
            new Command(this, new String[] { "addReference" }, "addReference",
                  ClientHelp.getUsage("addReference"), null, new Matcher[] {
                        this._twoArg, this._twoArgWithQuotedString,
                        this._threeArg, this._threeArgWithQuotedString }),
            new Command(this, new String[] { "delVFT" }, "delVFT", ClientHelp
                  .getUsage("delVFT"), null, new Matcher[] { this._oneArg }),
            new Command(this, new String[] { "delVFTReader" }, "delVFTReader",
                  ClientHelp.getUsage("delVFTReader"), null,
                  new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "delReference" }, "delReference",
                  ClientHelp.getUsage("delReference"), null,
                  new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "cancelReference" },
                  "cancelReference", ClientHelp.getUsage("cancelReference"),
                  null, new Matcher[] { this._twoArg }),
            new Command(this, new String[] { "showVFTReaders" },
                  "showVFTReaders", ClientHelp.getUsage("showVFTReaders"),
                  null, new Matcher[] { this._oneArg, this._twoArg }),
            new Command(this, new String[] { "setReference" }, "setReference",
                  ClientHelp.getUsage("setReference"), null, new Matcher[] {
                        this._twoArg, this._fourArg }), };
      this._interpreter.loadCommands(commands);
   }

   /**
    * Method to show file types
    */
   public void showTypes() {
      try {
         this._startTime = 0; // Don't time this command.
         LinkedList g = this._session.getGroupList();

         for (int i = 0; i < g.size(); i++) {
            LinkedList t = this._session.getFileTypeList(g.get(i).toString());
            for (int j = 0; j < t.size(); j++) {
               this._writeTTYLine(g.get(i).toString() + ":"
                     + t.get(j).toString());
            }
         }
      } catch (SessionException se) {
         this._writeTTYLine("Session Exception Caught: " + se.getMessage());
      }
   }

   /**
    * Method to perform client login.
    */
   public final void loginHiddenToUncoverCallers() {

      super.loginHiddenToUncoverCallers(); // Perform the login sequence.
      this._fileType = null;
      this._newConn = null;
      this._setPrompt();

      if (this._vftAccess != null) {
         this._vftAccess.close();
         try {
            this._getResults();
         } catch (Exception e) {
            e.printStackTrace();
         }
         this._vftAccess = null;
      }
   }

   public final void login(String serverGroup) {

       super.login(serverGroup); // Perform the login sequence.
       this._fileType = null;
       this._newConn = null;
       this._setPrompt();

       if (this._vftAccess != null) {
          this._vftAccess.close();
          try {
             this._getResults();
          } catch (Exception e) {
             e.printStackTrace();
          }
          this._vftAccess = null;
       }
    }
   
   /**
    * Method to load user login information from the cached login file
    */
   public final void loginUsingCache(String servergroup) {
      try {
          super.loginUsingCache(servergroup);
          this._fileType = null;
          this._newConn  = null;
          this._setPrompt();
      } catch (Exception e) {
         this._writeTTYLine("Unable to process user login.  Use manual login.");
      }

      if (this._vftAccess != null) {
         this._vftAccess.close();
         try {
            this._getResults();
         } catch (Exception e) {
            e.printStackTrace();
         }
         this._vftAccess = null;
      }
   }

   /**
    * Method to override super class's exit method to clear VFT.
    */
   public void exit() {

      if (this._vftAccess != null) {
         this._vftAccess.close();
         this._vftAccess = null;
      }
      // Close immediate before calling super class.
      if (this._session != null)
         this._session.closeImmediate();
      this._session = null;
      // Call to actually do the exit.
      super.exit();
   }

   /**
    * Process checksum calculation.
    */
   public void checksum() {

      String fileName = null;

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileName = this._oneArg.group(2);
      }

      try {
         byte[] cksum = FileUtil.getChecksumInByte(this._currentDir
               + File.separator + fileName);
         this._writeTTYLine("Checksum: \"" + FileUtil.checksumToString(cksum)
               + "\"");
      } catch (IOException io) {
         this._handleError(io.getMessage());
      }
   }

   /**
    * Process ct command to change file type connection
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when
    *            connection session fail
    * @deprecated Use method 'useType' instead.
    */
   public void changeType() throws SessionException {
      this.useType();
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
      //    group = this._session.getFTServerGroup("vft");
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
    * Process ct command to use a file type connection
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when
    *            connection session fail
    */
   public void useType() throws SessionException {


      //Change file type
      String proposedNewConn = this._oneArg.group(2);
      String defaultGroup;

      //Check to see if group name was included with file type name
      if (FileType.isFullFiletype(proposedNewConn)) {
         // get the server group
         defaultGroup = FileType.extractServerGroup(proposedNewConn);
         try {
            this._session.setDefaultGroup(defaultGroup);
         } catch (SessionException se) {
            this._writeTTYLine(se.getMessage());
            return;
         }

         // now, strip off group from file type name
         proposedNewConn = FileType.extractFiletype(proposedNewConn);
      } else {
         //Get the default server group if not defined
         defaultGroup = this._session.getDefaultGroup();
      }
      
     if (this._fileType == null) 
     {
         if (!this._session.isLoggedOn()) {
             this.loginUsingCache(defaultGroup);
             if (!this._session.isLoggedOn())
                this.login(defaultGroup);
         }
     }
      
      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(defaultGroup);
      String fullFt = FileType.toFullFiletype(defaultGroup, proposedNewConn);
      if (this._connections.containsKey(fullFt)) {
         this._fileType = (FileType) this._connections.get(fullFt);
         this._newConn = proposedNewConn;
         this._setPrompt();
         this._writeTTYLine("Using file type : " + this._newConn);
      } else {
         try {
            this._fileType = this._session.open(defaultGroup, proposedNewConn);
            defaultGroup = this._fileType.getGroup();
            this._session.setDefaultGroup(defaultGroup);
            this._newConn = proposedNewConn;
            this._connections.put(fullFt, this._fileType);
            this._setPrompt();
            this._writeTTYLine("Using file type " + fullFt);
         } catch (SessionException e) {
            this._writeTTYLine(e.getMessage());
            //If in batch abort entirely. This is safest solution,
            //because if not the next command could be an "add" and
            //you may end up adding files to the previous file type
            if (this._batch)
               System.exit(1);
         }
      }
   }

   
//   public void useTypeOld() throws SessionException {
//
//       if (this._fileType == null) {
//          if (!this._session.isLoggedOn()) {
//             this.loginUsingCache(null);
//             if (!this._session.isLoggedOn())
//                this.login();
//          }
//       }
//
//       //Change file type
//       String proposedNewConn = this._oneArg.group(2);
//       String defaultGroup;
//
//       //Check to see if group name was included with file type name
//       if (FileType.isFullFiletype(proposedNewConn)) {
//          // get the server group
//          defaultGroup = FileType.extractServerGroup(proposedNewConn);
//          try {
//             this._session.setDefaultGroup(defaultGroup);
//          } catch (SessionException se) {
//             this._writeTTYLine(se.getMessage());
//             return;
//          }
//
//          // now, strip off group from file type name
//          proposedNewConn = FileType.extractFiletype(proposedNewConn);
//       } else {
//          //Get the default server group if not defined
//          defaultGroup = this._session.getDefaultGroup();
//       }
//
//       
//       
//       
//       // Check authentication in session object. If password is null, acquire
//       // a password from MDMS PWDServer.
//       this._checkAuth(defaultGroup);
//       String fullFt = FileType.toFullFiletype(defaultGroup, proposedNewConn);
//       if (this._connections.containsKey(fullFt)) {
//          this._fileType = (FileType) this._connections.get(fullFt);
//          this._newConn = proposedNewConn;
//          this._setPrompt();
//          this._writeTTYLine("Using file type : " + this._newConn);
//       } else {
//          try {
//             this._fileType = this._session.open(defaultGroup, proposedNewConn);
//             defaultGroup = this._fileType.getGroup();
//             this._session.setDefaultGroup(defaultGroup);
//             this._newConn = proposedNewConn;
//             this._connections.put(fullFt, this._fileType);
//             this._setPrompt();
//             this._writeTTYLine("Using file type " + fullFt);
//          } catch (SessionException e) {
//             this._writeTTYLine(e.getMessage());
//             //If in batch abort entirely. This is safest solution,
//             //because if not the next command could be an "add" and
//             //you may end up adding files to the previous file type
//             if (this._batch)
//                System.exit(1);
//          }
//       }
//    }
   
   /**
    * Process comment command
    * 
    * @throws Exception general operation exception
    */
   public void comment() throws Exception {

      String fileName = null;
      String cmnt = null;

      if (this._oneArgWithQuotedString.group(1) != null) {
         fileName = this._oneArgWithQuotedString.group(2);
         cmnt = this._oneArgWithQuotedString.group(3);
      }

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing comment command " + this._ttyInput);
      if (!this._test) {
         this._fileType.comment(fileName, cmnt);
         this._getResults();
      }
   }

   /**
    * Process rename command
    * 
    * @throws Exception general operational exception
    */
   public void rename() throws Exception {

      String oldFileName = null;
      String newFileName = null;
      Result result;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         oldFileName = this._twoArg.group(2);
         newFileName = this._twoArg.group(3);
      }

      if (!this._test) {
         this._fileType.rename(oldFileName, newFileName);
         while ((result = this._session.result()) != null) {
            this._writeTTYLine(result.getMessage());
         }
      }
   }

   /**
    * Change the current user's password. If a user login is correct, the the
    * user may change password. Password size limitations are enforced by the
    * Komodo.
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when session
    *            connection/operation fail
    */
   public void changePassword()
         throws jpl.mipl.mdms.FileService.komodo.api.SessionException {

      String oldPassword = null;
      String newPassword = null;
      String verify = null;
      String user = null;
      boolean interactive = false;
      
      try {
          //get the active server group
          String group = null;
          if (this._fileType != null)
              group = this._fileType.getGroup();
          else 
              group = this._session.getDefaultGroup();
          
         if (!this._session.isLoggedOn()) {
              this.loginUsingCache(group);
              if (!this._session.isLoggedOn())
              {
                 this.login(group);
                 interactive = true;
              }
         }
         
         //no interaction means we should remind user the 'username'
         if (!interactive)
         {
             user = this._session.getUserName();
             oldPassword = this._session.getPassword();
             if (user != null && oldPassword != null)
                 this._writeTTYLine("Changing password for user '"+user+"'");
         }

         //oldPassword = ConsolePassword.getPassword("Enter old password: ");
         oldPassword = this._session.getPassword();

         boolean ok = false;
         while (!ok) 
         {
            newPassword = ConsolePassword.getPassword(
                            "Enter new password (or type \"abort\" to quit)>> ");
            if (newPassword.equalsIgnoreCase("abort"))
               return;
            if (newPassword.equals(""))
            {
                this._handleError("Password cannot be empty, try again");
                continue;
            }
            verify = ConsolePassword.getPassword("Re-enter new password>> ");
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
         this._session.changePassword(oldPassword, encryptedPass);
         this._getResults();
      } catch (IOException e) {
         this._handleError("Failed to change password. " + e.getMessage());
      }
   }

   /**
    * Process show capabilities command.
    */
   public void showCapabilities() {

      Capability caps;
      
      String fullFiletype = null;
      String filetype = null;
      String servergroup = null;
      String cachedDefaultGroup = null;
      
      try {
                    
          //-----------------------
          
          //first stage: determine what filetype and server group is 
          //being requested, either explicitly or implicitly (via
          //current connected filetype or default server group)
          
          if (this._oneArg.matches() && this._oneArg.group(2) != null) 
          {
              String localTarget = this._oneArg.group(2);
              if (FileType.isFullFiletype(localTarget))
              {
                  fullFiletype = localTarget;
                  servergroup = FileType.extractServerGroup(fullFiletype);
                  filetype = FileType.extractFiletype(fullFiletype);
              }
              else                 
              {
                  servergroup = FileType.extractServerGroup(localTarget);
                  if (servergroup == null)
                  {
                      filetype = localTarget;           
                  }
              }
          }
         
          //servergroup not specified in argument, grab from active ft
          //or default group
          if (servergroup == null)
          {
              if (this._fileType != null)
                  servergroup = this._fileType.getGroup();
              else 
                  servergroup = this._session.getDefaultGroup();
          }
         
          
          //-----------------------
          
          //perform login only if not already logged in.  If logged
          //in to a different servergroup, user may have to
          //provide new credentials again via the 'login' command
          
          if (!this._session.isLoggedOn()) {
              this.loginUsingCache(servergroup);
              if (!this._session.isLoggedOn())
              {
                 this.login(servergroup);
              }
          }
         
         //------------------------ 
          
          if (!this._session.isGroupDefined(servergroup))
          {
              throw new SessionException("Group " + servergroup
                  + " not found in domain!", Constants.DOMAINLOOKUPERR);
          }
          
         //if (this._oneArg.matches() && this._oneArg.group(2) != null)
  
         // Show capabilities for file type.
         //if filetype specified, show info on that only (servergroup
         //should always be set by this point)
         if (filetype != null)
         {                        
            String target = FileType.toFullFiletype(servergroup, filetype);
            
            //---------------------
            
            //check that filetype exists first
            
            List groupTypes = this._session.getFileTypeList(servergroup);
            if (groupTypes == null || groupTypes.isEmpty())
            {
                throw new SessionException("Group " + servergroup
                        + " not found in domain!", Constants.DOMAINLOOKUPERR);
            }
            else if (!groupTypes.contains(filetype))
            {
                throw new SessionException("Filetype " + target
                        + " not found in domain!", Constants.DOMAINLOOKUPERR);
            }
                
             
            //---------------------
            //get capabilities for filetype
             
            caps = (Capability) this._session.getCapabilities(
                                                      target);
            
            if (caps != null)
               this._writeTTYLine(caps.getName() + " = " + 
                                  caps.getCapabilities());
            else
               this._writeTTYLine("No capabilities defined for file types.");
            
            //---------------------
            
            //handle the VFTs
            
            caps = (Capability) this._session.getVFTCapabilities(
                                        target);
                                        //this._oneArg.group(2));
            
            if (caps != null)
               this._writeTTYLine(caps.getName() + " = "
                                  + caps.getCapabilities());
            else
               this._writeTTYLine("No capabilities defined for VFTs.");
         } 
         else 
         {
             //will need to rely on default group being set
             //for user access portion.  thus, cache current default
             //value, change, and change back when done
            cachedDefaultGroup = this._session.getDefaultGroup();
            this._session.setDefaultGroup(servergroup);
             
            // Show user-level access.
            String accessDisp = null;
            int userAccess = this._session.getUserAccess();
            if (userAccess > 0)
               accessDisp = "User access: " + this._session.getUserAccessStr();
            if (this._session.getAddVFT()) 
            {
               if (accessDisp == null)
                  accessDisp = "User access: " + "VFT";
               else
                  accessDisp += ", " + "VFT";
            }
            
            if (accessDisp != null)
               this._writeTTYLine(accessDisp);
            
            if (!this._test) 
            {
                //String groupName = (this._fileType == null) ? null :
                //                   this._fileType.getGroup();
                String groupName = servergroup;
                LinkedList capabilities = (groupName == null) ? 
                        this._session.getCapabilities() :
                        this._session.getCapabilitiesForGroup(groupName);
                        
               if (capabilities.size() > 0) {
                  this._writeTTYLine("FileType Capabilities:");
                  for (int i = 0; i < capabilities.size(); i++) 
                  {
                     caps = (Capability) capabilities.get(i);
                     this._writeTTYLine(caps.getName() + " = "
                           + caps.getCapabilities());
                  }
               }
               
               capabilities = this._session.getVFTCapabilities();
               if (capabilities.size() > 0) {
                  this._writeTTYLine("VFT Capabilities:");
                  for (int i = 0; i < capabilities.size(); i++) {
                     caps = (Capability) capabilities.get(i);
                     this._writeTTYLine(caps.getName() + " = "
                           + caps.getCapabilities());
                  }
               }
            }
         }

      } catch (Exception e) {  
         this._handleError(e.getMessage());
      } finally {
          
          //always reset cached default group, if it has been set
          if (cachedDefaultGroup != null)
          {
              try {
                  this._session.setDefaultGroup(cachedDefaultGroup);
              } catch (SessionException sesEx) {
                  this._handleError(sesEx.getMessage());
              }
          }
      }
   }

   /**
    * Process archive command
    * 
    * @throws Exception general operation failure
    */
   public void archive() throws Exception {

      String fileName;
      String archiveNote = "";

      if (this._oneArgWithQuotedString.matches()
            && this._oneArgWithQuotedString.group(1) != null) {
         fileName = this._oneArgWithQuotedString.group(2);
         archiveNote = this._oneArgWithQuotedString.group(3);
      } else {
         fileName = this._oneArg.group(2);
      }
      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing archive command " + this._ttyInput);
      if (!this._test) {
         this._fileType.archive(fileName, archiveNote);
         this._getResults();
      }
   }

   /**
    * Process replace command
    * 
    * @throws Exception general operation failure
    */
   public void replace() throws Exception {
      String fileExpr = "*";
      String cmnt = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
      }

      this._logger.debug("processing replace file command: " + this._ttyInput);

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileExpr = this._oneArg.group(2);
      } else if (this._oneArgWithQuotedString.matches()
            && this._oneArgWithQuotedString.group(1) != null) {
         fileExpr = this._oneArgWithQuotedString.group(2);
         cmnt = this._oneArgWithQuotedString.group(3);
      }

      if (!this._test) {
         this._fileType.replace(fileExpr, cmnt);
         this._getResults();
      }
   }

   /**
    * Process add command
    * 
    * @throws Exception general operational failure
    */
   public void add() throws Exception {
      String fileExpr = "*";
      String cmnt = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
      }

      this._logger.debug("processing add file command: " + this._ttyInput);

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileExpr = this._oneArg.group(2);
      } else if (this._oneArgWithQuotedString.matches()
            && this._oneArgWithQuotedString.group(1) != null) {
         fileExpr = this._oneArgWithQuotedString.group(2);
         cmnt = this._oneArgWithQuotedString.group(3);
      }

      if (!this._test) {
         this._fileType.add(fileExpr, cmnt);
         this._getResults();
      }

   }

   /**
    * Add a file. Have the server rename it and set a reference to it.
    * Optionally, specifiy a link directory. Links will have the form <link
    * directory>/ <source file name>/
    * 
    * @throws Exception general operationl failure
    */
   public void addAndRef() throws Exception {

      String fileSpec;
      String vft;
      String linkDir = null;

      if (this._tokenCount < 4) {
         this._handleError("Incomplete command.");
         return;
      }
      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing add and ref command " + this._ttyInput);
      fileSpec = this._tokenizer.nextToken();
      vft = this._tokenizer.nextToken();
      linkDir = this._tokenizer.nextToken();

      //If this is not a regular expression, make sure the file exists.
      if (this._test && fileSpec.indexOf('*') == -1
            && !this._checkFile(fileSpec)) {
         return;
      }
      //Now get the results;
      if (!this._test) {
         this._fileType.addAndRef(fileSpec, vft, linkDir);
         this._getResults();
      }
   }

   /**
    * Process show command
    * 
    * @throws Exception general operation failure
    */
   public void show() throws Exception {
      String fileExpr = "*"; // by default we query for all files
      String invoke = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing show file command:  " + this._ttyInput);

      if (this._noArgWithInvoke.matches()
            && this._noArgWithInvoke.group(1) != null) {
         // skipping group(2) because it is the group for the single/double
         // quotes
         invoke = this._noArgWithInvoke.group(3);
      } else if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileExpr = this._oneArg.group(2);
      } else if (this._oneArgWithInvoke.matches()
            && this._oneArgWithInvoke.group(1) != null) {
         fileExpr = this._oneArgWithInvoke.group(2);
         invoke = this._oneArgWithInvoke.group(4);
         if (invoke == null)
            invoke = this._oneArgWithInvoke.group(5);
      }

      if (!this._test) {
         this._fileType.show(fileExpr);
         this._doShowResults(invoke);
      }
   }

   /**
    * Process show after command
    * 
    * @throws Exception general operation failure
    */
   public void showAfter() throws Exception {
      String date = null;
      Date after = null;
      String invoke = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing show after file command: "
            + this._ttyInput);

      if (this._noArgWithInvoke.matches()
            && this._noArgWithInvoke.group(1) != null) {
         invoke = this._noArgWithInvoke.group(3);
         if (invoke == null)
            invoke = this._noArgWithInvoke.group(4);
      } else if (this._oneDateWithInvoke.matches()
            && this._oneDateWithInvoke.group(1) != null) {
         date = this._oneDateWithInvoke.group(2);
         invoke = this._oneDateWithInvoke.group(4);
         if (invoke == null)
            invoke = this._oneDateWithInvoke.group(5);
      } else if (this._oneDate.matches() && this._oneDate.group(1) != null) {
         date = this._oneDate.group(2);
      }      

      try {
         if (date != null)
            after = this._dateFormatter.parseDate(date); 
                    //DateTimeUtil.getCCSDSADate(date);
         if (!this._test) {
            this._fileType.showAfter(after);
            this._doShowResults(invoke);
         }
      } catch (ParseException e) {
         this._handleError(e.getMessage());
         return;
      } catch (SessionException se) {
         this._handleError(se.getMessage());
         return;
      }
   }

   /**
    * Process show since command
    * 
    * @throws Exception general operation failure
    */
   public void showSince() throws Exception {

      String date = null;
      Date since = new Date();
      String invoke = null;

      this._writeTTYLine("Warning: deprecated command!  New command: " +
                         "'showAfter'.");

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing show file since command:  "
            + this._ttyInput);

      if (this._oneDate.matches() && this._oneDate.group(1) != null) {
         date = this._oneDate.group(2);
      } else if (this._oneDateWithInvoke.matches()
            && this._oneDateWithInvoke.group(1) != null) {
         date = this._oneDateWithInvoke.group(2);
         invoke = this._oneDateWithInvoke.group(4);
         if (invoke == null)
            invoke = this._oneDateWithInvoke.group(5);
      }

      if (date != null)
         since = this._dateFormatter.parseDate(date); 
                 //DateTimeUtil.getCCSDSADate(date);

      if (!this._test) {
         this._fileType.showSince(since);
         this._doShowResults(invoke);
      }
   }

   /**
    * Process show between command
    * 
    * @throws Exception general operation failure
    */
   public void showBetween() throws Exception {

      String date1 = null;
      String date2 = null;
      Date start = null;
      Date end = null;
      String invoke = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing show file between command:  "
            + this._ttyInput);

      if (this._twoDate.matches() && this._twoDate.group(1) != null) {
         date1 = this._twoDate.group(2);
         date2 = this._twoDate.group(3);
      } else if (this._twoDateWithInvoke.matches()
            && this._twoDateWithInvoke.group(1) != null) {
         date1 = this._twoDateWithInvoke.group(2);
         date2 = this._twoDateWithInvoke.group(3);
         invoke = this._twoDateWithInvoke.group(5);
         if (invoke == null)
            invoke = this._twoDateWithInvoke.group(6);
      }

      if (date1 != null && date2 != null) {
         start = this._dateFormatter.parseDate(date1); 
                 //DateTimeUtil.getCCSDSADate(date1);
         end  = this._dateFormatter.parseDate(date2);  
                //DateTimeUtil.getCCSDSADate(date2);
      }

      if (!this._test) {
         this._fileType.showBetween(start, end);
         this._doShowResults(invoke);
      }
   }

   /**
    * Process show latest command
    * 
    * @throws Exception general operation failure
    */
   public void showLatest() throws Exception {

      String fileExpr = "*";
      String invoke = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing show latest file command:  "
            + this._ttyInput);

      //Show all files of this type
      if (this._noArgWithInvoke.matches()
            && this._noArgWithInvoke.group(1) != null) {
         invoke = this._noArgWithInvoke.group(3);
      } else if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileExpr = this._oneArg.group(2);
      } else if (this._oneArgWithInvoke.matches()
            && this._oneArgWithInvoke.group(1) != null) {
         fileExpr = this._oneArgWithInvoke.group(2);
         invoke = this._oneArgWithInvoke.group(4);
         if (invoke == null)
            invoke = this._oneArgWithInvoke.group(5);
      }

      if (!this._test) {
         this._fileType.showLatest(fileExpr);
         this._doShowResults(invoke);
      }
   }

   /**
    * Internal method to handle show results operation
    * 
    * @param cmd the command string
    * @throws Exception general operation failure
    */
   private void _doShowResults(String cmd) throws Exception {

      //Now get the results
      Result result;
      int count = 0;

      while ((result = this._session.result()) != null) {
         this._logger.debug("Komodo.result() is not null");
         if (result.getErrno() == Constants.OK) 
         {
             //---------------------
             
            count++;
            if (!this._verbose && !this._veryverbose) 
            {
               this._writeTTYLine(result.getName());
            } 
            else 
            {
               this._writeTTYLine(new PrintfFormat("%5d. ").sprintf(count) +
                     new PrintfFormat("%22s, %12ld, %.125s").sprintf(
                             new Object[] {
                                 //DateTimeUtil.getDateCCSDSAString(
                                 this._dateFormatter.formatDate(
                                     result.getFileModificationTime()),
                                 new Long(result.getSize()),
                                 result.getName() }));

               if (this._veryverbose) {
                  this._writeTTYLine("       "
                        + new PrintfFormat("[Contributor] %s").sprintf(result
                              .getFileContributor()));

                  this._writeTTYLine("       "
                        + new PrintfFormat("[Created] %s").sprintf(
                                this._dateFormatter.formatDate(
                                      //DateTimeUtil.getDateCCSDSAString(
                                      result.getFileCreationTime())));

                  if (result.getChecksum() != null) {
                     this._writeTTYLine("       "
                           + new PrintfFormat("[Checksum] %s").sprintf(result
                                 .getChecksumStr()));
                  }
                  if (result.getComment() != null) {
                     this._writeTTYLine("       "
                           + new PrintfFormat("[Comment] \"%s\"")
                                 .sprintf(result.getComment()));
                  }
                  if (result.getArchiveNote() != null) {
                     this._writeTTYLine("       "
                           + new PrintfFormat("[Archive note] \"%s\"")
                                 .sprintf(result.getArchiveNote()));
                  }
                  if (result.getRemoteLocation() != null) {
                      this._writeTTYLine("       "
                            + new PrintfFormat("[Remote location] %s")
                                  .sprintf(result.getRemoteLocation()));
                   }
                  this._writeTTYLine("");
               }
            }
            
            //---------------------
            
            //check if cmd was provided, if so, substitute and execute
            if (cmd != null && result.getName() != null) {
                String cmdStr = InvocationCommandUtil.buildCommand(
                                     cmd, this._currentDir, result);
                this._writeTTYLine("Invoke command \'" + cmdStr + "\'");
                Errno error = SystemProcess.execute(cmdStr);
                this._writeTTYLine(error.getMessage());
             }
            
            //---------------------
            
         } else {
            this._logger.debug("reading message from result.");
            this._writeTTYLine(result.getMessage());
            this._logger.debug("done reading message from result.");
         }
      }
      this._logger.debug("no more data");
   }

   /**
    * Processes "makeDomainFile" command.  Reads argument of filepath
    * for new domain file.  Also retrieves server group from filetype
    * if set, otherwise the default group is used.  
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when network
    *            or result processing failure
    */
   public final void makeDomainFile()
         throws jpl.mipl.mdms.FileService.komodo.api.SessionException {

      if (!this._test) {
         String tmp = this._oneArg.group(2); //file name
         
         String servergroup = (this._fileType != null) ?  
                               this._fileType.getGroup() :
                               this._session.getDefaultGroup();
                               
                               
         //new_ntt grab the server info from filetype
         if (servergroup == null) {
             this._handleError("Filetype/servergroup not selected");
             return;
         }        
         this._session.makeDomainFile(tmp, servergroup);
         //end_new
         this._getResults();
      }
   }

   /**
    * Process get file command
    * 
    * @throws Exception general operation failure
    */
   public void get() throws Exception {

      String fileExpr = null;
      String cmd = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing get latest file command:  "
            + this._ttyInput);

      //Show all files of this type
      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileExpr = this._oneArg.group(2);

      } else if (this._oneArgWithInvoke.matches()
            && this._oneArgWithInvoke.group(1) != null) {
         fileExpr = this._oneArgWithInvoke.group(2);
         cmd = this._oneArgWithInvoke.group(4);
         if (cmd == null)
            cmd = this._oneArgWithInvoke.group(5);
      }

      if (!this._test) {
         this._fileType.get(fileExpr);
         this._getResults(cmd);
      }
   }

   /**
    * Process get files since a given time.
    * 
    * @throws Exception general operation failure
    */
   public void getSince() throws Exception {

      String date = null;
      Date since;
      String cmd = null;

      this._writeTTYLine("Warning: deprecated command!  New command: 'getAfter'.");

      if (this._oneDate.matches() && this._oneDate.group(1) != null) {
         date = this._oneDate.group(2);

      } else if (this._oneDateWithInvoke.matches()
            && this._oneDateWithInvoke.group(1) != null) {
         date = this._oneDateWithInvoke.group(2);
         cmd = this._oneDateWithInvoke.group(4);
         if (cmd == null)
            cmd = this._oneDateWithInvoke.group(5);
      }

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing get file command:  " + this._ttyInput);

      // We must verify the date format outside of "test",
      // otherwise, syntax checking is comprimised. Get
      // CCSDSADate will throw a parse exception.
      try {
         since = this._dateFormatter.parseDate(date); 
                 //DateTimeUtil.getCCSDSADate(date);
         if (!this._test) {
            this._fileType.getSince(since);
            this._getResults(cmd);
         }
      } catch (ParseException e) {
         this._handleError(e.getMessage());
         return;
      } catch (SessionException se) {
         this._handleError(se.getMessage());
         return;
      }
   }

   /**
    * Process get file after a given time
    * 
    * @throws Exception general operation failure
    */
   public void getAfter() throws Exception {

      String date = null;
      Date after = null;
      String invoke = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing get after file command:  "
            + this._ttyInput);

      if (this._noArgWithInvoke.matches()
            && this._noArgWithInvoke.group(1) != null) {
         invoke = this._noArgWithInvoke.group(3);
         if (invoke == null)
            invoke = this._noArgWithInvoke.group(4);
      } else if (this._oneDateWithInvoke.matches()
            && this._oneDateWithInvoke.group(1) != null) {
         date = this._oneDateWithInvoke.group(2);
         invoke = this._oneDateWithInvoke.group(4);
         if (invoke == null)
            invoke = this._oneDateWithInvoke.group(5);
      } else if (this._oneDate.matches() && this._oneDate.group(1) != null) {
         date = this._oneDate.group(2);
      }

      try {
         if (date != null)
            after = this._dateFormatter.parseDate(date); 
                    //DateTimeUtil.getCCSDSADate(date);
         if (!this._test) {
            this._fileType.getAfter(after);
            this._getResults(invoke);
         }
      } catch (ParseException e) {
         this._handleError(e.getMessage());
         return;
      } catch (SessionException se) {
         this._handleError(se.getMessage());
         return;
      }
   }

   /**
    * Process get files between a given time range
    * 
    * @throws Exception general operation failure
    */
   public void getBetween() throws Exception {

      String date1 = null;
      String date2 = null;
      Date start, end;
      String cmd = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing show file between command:  "
            + this._ttyInput);

      //Show all files of this type
      if (this._twoDate.matches() && this._twoDate.group(1) != null) {
         date1 = this._twoDate.group(2);
         date2 = this._twoDate.group(3);

      } else if (this._twoDateWithInvoke.matches()
            && this._twoDateWithInvoke.group(1) != null) {
         date1 = this._twoDateWithInvoke.group(2);
         date2 = this._twoDateWithInvoke.group(3);
         cmd = this._twoDateWithInvoke.group(5);
         if (cmd == null)
            cmd = this._twoDateWithInvoke.group(6);
      }

      // We must verify the date format outside of "test",
      // otherwise, syntax checking is comprimised. Get
      // CCSDSADate will throw a parse exception.
      try {
         start = this._dateFormatter.parseDate(date1);  
                 //DateTimeUtil.getCCSDSADate(date1);
         end   = this._dateFormatter.parseDate(date2); 
                 //DateTimeUtil.getCCSDSADate(date2);
         if (!this._test) {
            this._fileType.getBetween(start, end);
            this._getResults(cmd);
         }
      } catch (ParseException e) {
         this._handleError(e.getMessage());
         return;
      } catch (SessionException se) {
         this._handleError(se.getMessage());
         return;
      }
   }

   /**
    * Process get the latest files
    * 
    * @throws Exception general operation failure
    */
   public void getLatest() throws Exception {

      String fileExpr = null;
      String cmd = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing get latest file command:  "
            + this._ttyInput);

      //Show all files of this type
      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileExpr = this._oneArg.group(2);

      } else if (this._noArgWithInvoke.matches()
            && this._noArgWithInvoke.group(1) != null) {
         cmd = this._noArgWithInvoke.group(3);
         if (cmd == null)
            cmd = this._noArgWithInvoke.group(4);
      } else if (this._oneArgWithInvoke.matches()
            && this._oneArgWithInvoke.group(1) != null) {
         fileExpr = this._oneArgWithInvoke.group(2);
         cmd = this._oneArgWithInvoke.group(4);
         if (cmd == null)
            cmd = this._oneArgWithInvoke.group(5);
      }
      if (!this._test) {
         this._fileType.getLatest(fileExpr);
         this._getResults(cmd);
      }
   }

   /**
    * Process delete file command
    * 
    * @throws Exception general operation failure
    */
   public void delete() throws Exception {

      String fileNameExp = null;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      this._logger.debug("processing delete command " + this._ttyInput);

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         fileNameExp = this._oneArg.group(2);
      }

      if (!this._test) {
         this._fileType.delete(fileNameExp);
         this._getResults();
      }
   }

   /**
    * Process lock file type. Takes optional argument, group or owner, used to
    * change file system permissions on the server host. If "group", then remove
    * write access from all but owner. If "owner", remove write access from all.
    * 
    * @throws Exception general operation failure
    */
   public void lockFileType() throws Exception {

      String mode;

      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      // Mode is optional. If noArg was matched, then pass mode will be
      // set to null.
      mode = this._oneArg.group(2);
      if (mode != null && !mode.equalsIgnoreCase("group")
            && !mode.equalsIgnoreCase("owner")) {
         this._handleError("Incorrect command.  Mode must be owner or group.");
         return;
      }
      if (!this._test) {
         this._fileType.lock(mode);
         this._getResults();
      }
   }

   /**
    * Process setDefaultGroup command. Takes one argument for the server group
    * name.
    * 
    * @throws Exception general operation failure
    */
   public void setDefaultGroup() throws Exception {
      if (!this._test) {
         try {
            this._session.setDefaultGroup(this._oneArg.group(2));
            this._writeTTYLine("Default server group changed to "
                  + this._oneArg.group(2));
            this
                  ._writeTTYLine("Select a file type in the group with the use command.");
            this._prompt = this._oneArg.group(2) + ":";
            this._newConn = null;
            this._fileType = null;
         } catch (SessionException se) {
            this._handleError(se.getMessage());
         }
      }
   }

   /**
    * Process unlock file type.
    * 
    * @throws Exception general operation failure
    */
   public void unlockFileType() throws Exception {

      String mode;

      if (this._oneArg.matches() && this._oneArg.group(1) == null) {
         this
               ._handleError("Incorrect command.  Must specify mode, either owner or group");
         return;
      }
      if (this._fileType == null) {
         this._handleError("File type not selected");
         return;
      }
      mode = this._oneArg.group(2);
      if (!mode.equalsIgnoreCase("group") && !mode.equalsIgnoreCase("owner")) {
         this._handleError("Incorrect command.  Mode must be owner or group.");
         return;
      }
      if (!this._test) {
         this._fileType.unlock(mode);
         this._getResults();
      }
   }

   /* VFT User Commands */

   /**
    * Internal function to set prompt based on current file type and vft.
    */
   private void _setPrompt() {
      this._prompt = this._session.getDefaultGroup() == null ? ""
            : this._session.getDefaultGroup() + ":";
      if (this._newConn != null)
         this._prompt += this._newConn;
   }

   /**
    * Get file by reference.
    * 
    * @throws Exception general operation failure
    */
   public void getReference() throws Exception {

      String vft, ref;
      Date date = null;

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         ref = this._twoArg.group(3);
      } else {
         vft = this._threeArg.group(2);
         ref = this._threeArg.group(3);
         date = this._dateFormatter.parseDate(this._threeArg.group(4)); 
                //DateTimeUtil.getCCSDSADate(this._threeArg.group(4));
      }
      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         if (date != null)
            this._vftAccess.getReferenceAt(vft, ref, date);
         else
            this._vftAccess.getReference(vft, ref);
         this._getResults();
      }
   }

   /**
    * Get all the files a VFT references.
    * 
    * @throws Exception general operation failure
    */
   public void getVFT() throws Exception {

      String vft = null;
      Date date = null;
      PrintWriter manifestFile = null;

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (this._oneArg.matches() && this._oneArg.group(1) != null)
         vft = this._oneArg.group(2);
      else if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         date = this._dateFormatter.parseDate(this._twoArg.group(3)); 
                //DateTimeUtil.getCCSDSADate(this._twoArg.group(3));
      }
      if (!this._test) {
         try {
            if (this._vftAccess == null)
               this._vftAccess = new VFT(this._session, false);
            if (date != null)
               this._vftAccess.getVFTAt(vft, date);
            else
               this._vftAccess.getVFT(vft);
            // Get the first result. This will be the vft description
            // record.
            Result vftInfo = this._session.result();
            if (vftInfo.getErrno() != 0) {
               this._writeTTYLine(vftInfo.getMessage());
               this._writeTTYLine("");
               // Flush remaining results or error messages.
               this._getResults();
            } else {
               /* Write to manifest file if we've found the vft. */
               manifestFile = new PrintWriter(new FileWriter(this._currentDir
                     + File.separator + vft + ".vft"), false);
               String vftManifestString = this._getVFTManifestString(vftInfo,
                     date);
               this._writeTTYLine(vftManifestString);
               manifestFile.println(vftManifestString);
               Result refInfo;
               String refString;
               while ((refInfo = this._session.result()) != null) {
                  this._writeTTYLine(refInfo.getMessage());
                  // Only write to the manifest when a file has been
                  // brought
                  // over. Don't
                  // include any notes when files are deleted from the
                  // current
                  // directory.
                  if (refInfo.getErrno() == 0) {
                     refString = refInfo.getRefName()
                           + " "
                           + refInfo.getRefFileType()
                           + " "
                           + refInfo.getRefFileName()
                           + (refInfo.getChecksum() != null ? " "
                                 + refInfo.getChecksumStr() : "");
                     manifestFile.println(refString);
                  }
               }
            }
         } finally {
            // Make sure we close the manifest.
            if (manifestFile != null)
               manifestFile.close();
         }
      }
   }

   /**
    * Format a VFT information string for a VFT manifest file.
    * 
    * @param vft result.
    * @param date Optional date for historical request.
    * @return vft info string
    */
   private String _getVFTManifestString(Result vft, Date date) {

      StringBuffer vftInfo = new StringBuffer();
      vftInfo.append("VFT:" + vft.getVFTName() + "\nCreated by: "
            + vft.getCreatedBy());
      if (vft.getUpdatedBy() != null)
         vftInfo.append("\nUpdated by:" + vft.getUpdatedBy());
      if (vft.getTitle() != null)
         vftInfo.append("\nTitle:\"" + vft.getTitle() + "\"");
      if (vft.getComment() != null)
         vftInfo.append("\nComment:\"" + vft.getComment() + "\"");
      vftInfo.append("\ngetVFT:"
            + DateTimeUtil.getDateCCSDSAString(new Date(System
                  .currentTimeMillis())));
      if (date != null)
         vftInfo.append("\nvftHistoricalTime:"
               + DateTimeUtil.getDateCCSDSAString(date));
      if (vft.getUpdated() != null)
         vftInfo.append("\nupdateVFT:" + vft.getUpdated());
      vftInfo.append("\naddVFT:" + vft.getCreated());
      return vftInfo.toString();
   }

   /**
    * Add file reference information to a vft reference. This new references
    * takes place at the next VFT update.
    * 
    * @throws Exception general operation failure
    */
   public void setReference() throws Exception {

      String vft;
      String ref;
      String fileType;
      String fileName;

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      //this._checkAuth(this._session.getDefaultGroup());

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         ref = this._twoArg.group(3);
         fileType = null;
         fileName = null;
      } else {
         vft = this._fourArg.group(2);
         ref = this._fourArg.group(3);
         fileType = this._fourArg.group(4);
         fileName = this._fourArg.group(5);
      }
      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.setReference(vft, ref, fileType, fileName);
         this._getResults();
      }
   }

   /**
    * Cancel scheduled file reference add or delete.
    * 
    * @throws Exception general operation failure
    */
   public void cancelReference() throws Exception {

      String vft = null;
      String ref = null;

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         ref = this._twoArg.group(3);
      }

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.cancelReference(vft, ref);
         this._getResults();
      }
   }

   /**
    * Show references for a vft. Optional arguments are ref, and date.
    * 
    * @throws Exception general operation failure
    */
   public void showReference() throws Exception {

      String vft = null;
      String ref = null;
      String dateTime = null;

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         vft = this._oneArg.group(2);
         ref = null;
         dateTime = null;
      } else if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         ref = this._twoArg.group(3);
      } else {
         vft = this._threeArg.group(2);
         ref = this._threeArg.group(3);
         dateTime = this._threeArg.group(4);
      }
      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         if (dateTime != null) {
            this._vftAccess.showReference(vft, ref, DateTimeUtil
                  .getCCSDSADate(dateTime));
            this._showRefInfoHistoryResults(); // Show historical results.
         } else if (ref != null) {
            this._vftAccess.showReference(vft, ref, null);
            this._showRefInfoResults(); // Show detailed results.
         } else {
            this._vftAccess.showReference(vft, null, null);
            this._showRefResults(); // Show refname and comment.
         }
      }
   }

   /**
    * Internal method to show reference result info
    * 
    * @throws Exception general operation failure
    */
   private void _showRefInfoResults() throws Exception {

      //Now get the results
      Result result;
      String comment = "";
      while ((result = this._session.result()) != null) {
         if (result.getErrno() == 0) {
            if (result.getComment() != null && result.getComment().length() > 0)
               comment = "\t\"" + result.getComment() + "\"";
            this._writeTTYLine(result.getRefName() + comment);
         } else
            this._writeTTYLine(result.getMessage());
      }
   }

   /**
    * Show detailed information on a set of refernces.
    * 
    * @throws Exception general operation failure
    */
   private void _showRefInfoHistoryResults() throws Exception {

      //Now get the results
      Result result;
      String comment = "";
      while ((result = this._session.result()) != null) {
         if (result.getErrno() == 0) {
            if (result.getComment() != null && result.getComment().length() > 0)
               comment = "\t\"" + result.getComment() + "\"";
            this._writeTTYLine(result.getRefName() + comment);
         } else
            this._writeTTYLine(result.getMessage());
      }
   }

   /*
    * * VFT Admin commands -
    */

   /**
    * Command to add a virtual file type.
    * 
    * @throws Exception general operation failure
    */
   public void addVFT() throws Exception {

      String vft;
      String title = null;
      boolean notify = false;

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         if (this._oneArgWithQuotedString.matches()
               && this._oneArgWithQuotedString.group(1) != null) {
            vft = this._oneArgWithQuotedString.group(2);
            title = this._oneArgWithQuotedString.group(3);
         } else {
            vft = this._oneArg.group(2);
         }
         /*
          * * The server on which a VFT is to be created must be specified by *
          * the creator.
          */
         this._vftAccess.addVFT(vft, title, notify);
         this._getResults();
      }
   }

   /**
    * Create a reference in the current vft.
    * 
    * @throws Exception general operation failure
    */
   public void addReference() throws Exception {

      String vft = null;
      String ref = null;
      String link = null;
      String comment = null;

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      //this._checkAuth(this._session.getDefaultGroup());

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         ref = this._twoArg.group(3);
      } else if (this._twoArgWithQuotedString.matches()
            && this._twoArgWithQuotedString.group(1) != null) {
         vft = this._twoArgWithQuotedString.group(2);
         ref = this._twoArgWithQuotedString.group(3);
         comment = this._twoArgWithQuotedString.group(4);
      } else if (this._threeArg.matches() && this._threeArg.group(1) != null) {
         vft = this._threeArg.group(2);
         ref = this._threeArg.group(3);
         link = this._threeArg.group(4);
         comment = null;
      } else if (this._threeArgWithQuotedString.matches()
            && this._threeArgWithQuotedString.group(1) != null) {
         vft = this._threeArgWithQuotedString.group(2);
         ref = this._threeArgWithQuotedString.group(3);
         link = this._threeArgWithQuotedString.group(4);
         comment = this._threeArgWithQuotedString.group(5);
      }
      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         /* Create the reference in the current vft. */
         this._vftAccess.addRef(vft, ref, link, comment);
         this._getResults();
      }
   }

   /**
    * Update VFT to reflect all changes to file references.
    * 
    * @throws Exception general operation exception
    */
   public void updateVFT() throws Exception {

      String vft;
      String comment = null;

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         vft = this._oneArg.group(2);
      } else {
         vft = this._oneArgWithQuotedString.group(2);
         comment = this._oneArgWithQuotedString.group(3);
      }
      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.update(vft, comment);
         this._getResults();
      }
   }

   /**
    * Add a read-only user to the current VFT.
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when network
    *            or operation failed
    */
   public final void addVFTReader()
         throws jpl.mipl.mdms.FileService.komodo.api.SessionException {

      String vft = null;
      String user = null;

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         user = this._twoArg.group(3);
      }

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.addVFTReader(vft, user);
         this._getResults();
      }
   }

   /**
    * Show current vft list.
    * 
    * @throws Exception when network or operation failures
    */
   public void showVFT() throws Exception {

      Date at = null;
      String vft = null;

      if (!this._session.isLoggedOn()) {
         this.loginUsingCache(this._session.getDefaultGroup());
         if (!this._session.isLoggedOn())
            this.login(this._session.getDefaultGroup());
      }
//      if (!this._session.isLoggedOn()) {
//          this.loginUsingCache();
//          if (!this._session.isLoggedOn())
//             this.login();
//       }

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         if (this._oneArg.matches() && this._oneArg.group(1) != null) {
            vft = this._oneArg.group(2);
         } else if (this._oneDate.matches() && this._oneDate.group(1) != null) {
            at = this._dateFormatter.parseDate(this._oneDate.group(2));  
                 //DateTimeUtil.getCCSDSADate(this._oneDate.group(2));
         } else if (this._oneArgWithDate.matches()
               && this._oneArgWithDate.group(1) != null) {
            vft = this._oneArgWithDate.group(2);
            at = this._dateFormatter.parseDate(this._oneArgWithDate.group(3)); 
                 //DateTimeUtil.getCCSDSADate(this._oneArgWithDate.group(3));
         }
         // First, show information about each vft.
         this._vftAccess.showVFT(vft, at);
         if (this._showVFTResults() != 0)
            return;
         // If a single vft was specified, show its references too.
         if (vft != null) {
            String ref = null;
            this._vftAccess.showReference(vft, ref, at);
            this._showRefResults();
         }
      }
   }

   /**
    * Internal show vft results ...
    * 
    * @return operation status. -1 when failure, 0 when success
    * @throws Exception when network or operation failures
    */
   private int _showVFTResults() throws Exception {

      //Now get the results
      Result result;
      int count = 0;
      int status = -1;
      while ((result = this._session.result()) != null) {
         status = result.getErrno();
         if (status == 0) {
            // Just use as separator. Also, if we're displaying information
            // about
            // a single VFT, we don't want to see the separator.
            if (count++ > 0)
               this._writeTTYLine("\n---------------");
            this._writeTTYLine(this._getVFTInfoString(result));
         } else
            this._writeTTYLine(result.getMessage());
      }
      return status;
   }

   /**
    * Format a VFT information string.
    * 
    * @param vft result.
    * @return the retrieved vft info string
    */
   private String _getVFTInfoString(Result vft) {

      StringBuffer vftInfo = new StringBuffer();
      vftInfo.append("VFT: " + vft.getVFTName() + "\nCreated: "
            + vft.getCreated() + " by " + vft.getCreatedBy());
      if (vft.getUpdated() != null && vft.getUpdated().length() > 1) {
         vftInfo.append("\nUpdated: " + vft.getUpdated() + " by "
               + vft.getUpdatedBy());
      }
      if (this._verbose || this._veryverbose) {
         if (vft.getTitle() != null)
            vftInfo.append("\nTitle: \"" + vft.getTitle() + "\"");
         if (vft.getComment() != null)
            vftInfo.append("\nComment: \"" + vft.getComment() + "\"");
      }
      return vftInfo.toString();
   }

   /**
    * Internal show vft reference results.
    * 
    * @throws Exception general operation failure
    */
   private void _showRefResults() throws Exception {

      //Now get the results
      Result result;

      String refName;
      String fileType;
      String fileName;
      String updateFileType;
      String updateFileName;

      while ((result = this._session.result()) != null) {
         if (result.getErrno() == 0) {
            refName = result.getRefName();
            fileType = result.getRefFileType();
            fileName = result.getRefFileName();
            updateFileType = result.getUpdateFileType();
            updateFileName = result.getUpdateFileName();
            if (fileType != null)
               this._writeTTYLine("\nRef: " + refName + " => " + fileType + "/"
                     + fileName);
            else
               this._writeTTYLine("\nRef: " + result.getRefName() + " => NULL");
            if (result.updateRef() == true) {
               if (updateFileName == null)
                  this._writeTTYLine("Update to: NULL");
               else
                  this._writeTTYLine("Update to: " + updateFileType + " "
                        + updateFileName);
            }
            if (this._veryverbose && result.getComment() != null) {
               this._writeTTYLine("\"" + result.getComment() + "\"");
            }

         } else
            this._writeTTYLine(result.getMessage());
      }
   }

   /**
    * Delete a vft.
    * 
    * @throws Exception general operation failure
    */
   public void delVFT() throws Exception {

      String vft = null;

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         vft = this._oneArg.group(2);
      }

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.deleteVFT(vft);
         this._getResults();
      }
   }

   /**
    * Delete the file system reference, and all history.
    * 
    * @throws Exception general operation failure
    */
   public void delReference() throws Exception {

      String vft = null;
      String ref = null;

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         ref = this._twoArg.group(3);
      }

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.deleteRef(vft, ref);
         this._getResults();
      }
   }

   /**
    * Show the file system users allowed to read a vft.
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when network
    *            or operation failure
    */
   public final void showVFTReaders()
         throws jpl.mipl.mdms.FileService.komodo.api.SessionException {

      String vft;
      String user = null;

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (this._oneArg.matches() && this._oneArg.group(1) != null) {
         vft = this._oneArg.group(2); //
      } else {
         vft = this._twoArg.group(2); //
         user = this._twoArg.group(3); //
      }
      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.showVFTReaders(vft, user);
         this._getResults();
      }
   }

   /**
    * Delete a reader from a vft.
    * 
    * @throws jpl.mipl.mdms.FileService.komodo.api.SessionException when network
    *            or operation failure
    */
   public final void delVFTReader()
         throws jpl.mipl.mdms.FileService.komodo.api.SessionException {

      String vft = null;
      String user = null;

      if (this._twoArg.matches() && this._twoArg.group(1) != null) {
         vft = this._twoArg.group(2);
         user = this._twoArg.group(3);
      }

      // Check authentication in session object. If password is null, acquire
      // a password from MDMS PWDServer.
      this._checkAuth(this._session.getDefaultGroup());

      if (!this._test) {
         if (this._vftAccess == null)
            this._vftAccess = new VFT(this._session, false);
         this._vftAccess.delVFTReader(vft, user);
         this._getResults();
      }
   }
}