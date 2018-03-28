/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

import java.util.Date;

import jpl.mipl.mdms.FileService.komodo.util.Closable;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * All actions that can be take on a file type
 * 
 * @author J. Jacobson, G. Turek
 * @version $Id: Admin.java,v 1.23 2009/09/03 21:31:50 ntt Exp $
 */
public class Admin implements Closable {
   private ServerInfo _serverInfo;
   private ServerProxy _proxy;
   private String _serverName;
   private final Logger _logger = Logger.getLogger(Admin.class.getName());

   /**
    * Constructor
    * 
    * @param session a transfer session, for maintining file types and
    *           connections.
    * @param serverName the Komodo server name
    * @param useAdminPort the admin port uses port + 1 as admin port.
    * @throws SessionException when failed to initialize
    */
   public Admin(Session session, String serverName, boolean useAdminPort)
         throws SessionException {
      this._serverName = serverName;

      this._logger.trace("Admin - get server info for \"" + serverName + "\"");
      this._serverInfo = session.getServerInfo(null, serverName);
      if (this._serverInfo == null) {
         throw new SessionException("Server \"" + serverName
               + "\" not found in domain.", Constants.NO_SUCH_SERVER);
      }

      // If there is no server proxy, create one.
      this._proxy = this._serverInfo.getProxy();
      if (this._proxy == null) {
         this._logger.trace("No proxy, so make one.");
         // Blocks.
         this._serverInfo.setProxy(new ServerProxy(session, this._serverInfo,
               useAdminPort));
         this._proxy = this._serverInfo.getProxy();
      } else {
         // Increment the reference count on the server proxy. Note: the
         // server proxy refenence count is initialized to 1 in its
         // constructor.
         this._proxy.incrementRefCount();
      }
   }

   /**
    * Accessor method to get the server name
    * 
    * @return the server name
    */
   public final String getServerName() {
      return this._serverName;
   }

   /**
    * Method to add file type to role
    * 
    * @param args arguments to command
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int addFileTypeToRole(String[] args) throws SessionException {
      Request cmd = new Request(Constants.ADDFTTOROLE, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add user to role
    * 
    * @param args arguments to command
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int addUserToRole(String[] args) throws SessionException {
      Request cmd = new Request(Constants.ADDUSERTOROLE, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to synchronize file system with data in database
    * 
    * @param fileType the file type name
    * @param datetime the data time stamp
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int dSync(String fileType, Date datetime)
         throws SessionException {
      Request cmd;
      if (fileType == null && datetime == null) {
         cmd = new Request(Constants.DSYNC);
      } else {
         cmd = new Request(Constants.DSYNC, new String[] { fileType },
               new Date[] { datetime });
      }
      return (this._proxy.put(cmd));
   }

   /**
    * Method to synchronize database with file system
    * 
    * @param fileType the file type
    * @param datetime the time stamp
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int fSync(String fileType, Date datetime)
         throws SessionException {
      Request cmd;
      if (fileType == null && datetime == null) {
         cmd = new Request(Constants.FSYNC);
      } else {
         cmd = new Request(Constants.FSYNC, new String[] { fileType },
               new Date[] { datetime });
      }
      return (this._proxy.put(cmd));
   }

   /**
    * Method to issue a hotboot request to the server.
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int hotboot() throws SessionException {
      Request cmd = new Request(Constants.HOTBOOT);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to create a domain file
    * 
    * @param fileName the domain file name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int makeDomainFile(String fileName) throws SessionException {
      Request cmd = new Request(Constants.MAKEDOMAIN, new String[] { fileName });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to create file type.
    * 
    * @param args arguments to command
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int addFileType(String[] args) throws SessionException {
      Request cmd = new Request(Constants.ADDFT, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add new role access
    * 
    * @param args arguments to command
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int addRole(String[] args) throws SessionException {
      Request cmd = new Request(Constants.ADDROLE, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add new user
    * 
    * @param args arguments to command
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int addUser(String[] args) throws SessionException {
      Request cmd = new Request(Constants.ADDUSER, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to change a user's password, without verifying old password.
    * 
    * @param userName the user name
    * @param password the user's new password
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int changePassword(String userName, String password)
         throws SessionException {
      Request cmd = new Request(Constants.CHANGEPASSWORD, new String[] {
            userName, password });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to change a user's password, verifying old password.
    * 
    * @param userName the user name
    * @param oldPassword the user's old password
    * @param newPassword the user's new password
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int changePassword(String userName, String oldPassword,
         String newPassword) throws SessionException {
      Request cmd = new Request(Constants.CHANGEPASSWORD, new String[] {
            userName, newPassword, oldPassword });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to remove user role
    * 
    * @param role the role name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int delRole(String role) throws SessionException {
      String[] tmp = { new String(role) };
      Request cmd = new Request(Constants.REMOVEROLE, tmp);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to remove user
    * 
    * @param name the user name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int delUser(String name) throws SessionException {
      String[] tmp = { new String(name) };
      Request cmd = new Request(Constants.REMOVEUSER, tmp);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to remove file type
    * 
    * @param fileType the file type name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int delFileType(String fileType) throws SessionException {
      String[] tmp = { new String(fileType) };
      Request cmd = new Request(Constants.REMOVEFT, tmp);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to remove file type from role
    * 
    * @param args type and role
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int delFileTypeFromRole(String[] args) throws SessionException {
      Request cmd = new Request(Constants.RMFTFROMROLE, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to remove user
    * 
    * @param args the name and role
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int delUserFromRole(String[] args) throws SessionException {
      Request cmd = new Request(Constants.RMUSERFROMROLE, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to count number of users connected to server
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showConnections() throws SessionException {
      Request cmd = new Request(Constants.SHOWCONN);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show roles associated with a file type
    * 
    * @param args the name and role
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showRolesForFileType(String[] args) throws SessionException {
      Request cmd = new Request(Constants.SHOWROLESFORFT, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show filetypes associated with a role
    * 
    * @param args the role and optinal type
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showFiletypesForRole(String[] args) throws SessionException {
      Request cmd = new Request(Constants.SHOWTYPESFORROLE, args);   
      //cmd.setModifier(Constants.INVERSE);
      return (this._proxy.put(cmd));
   }
   
   /**
    * Method to show roles associated with a user
    * 
    * @param args the user name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showRolesForUser(String[] args) throws SessionException {
      Request cmd = new Request(Constants.SHOWROLESFORUSER, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show users associated with a role
    * 
    * @param args the role name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showUsersForRoles(String[] args) throws SessionException {
       
      Request cmd = new Request(Constants.SHOWUSERSFORROLE, args);
      //cmd.setModifier(Constants.INVERSE);
      return (this._proxy.put(cmd));
      
   }
   
   /**
    * Method to return list of file types in db or info about specific file type
    * 
    * @param name the file type name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showFileTypes(String name) throws SessionException {
      Request cmd;
      if (name == null)
         cmd = new Request(Constants.SHOWFT);
      else
         cmd = new Request(Constants.SHOWFT, new String[] { name });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show memory usage
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showMemory() throws SessionException {
      Request cmd = new Request(Constants.SHOWMEM);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show user records found in the db
    * 
    * @param name the server name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showServerParameters(String name) throws SessionException {
      Request cmd;
      if (name == null)
         cmd = new Request(Constants.SHOWPARAMS);
      else
         cmd = new Request(Constants.SHOWPARAMS, new String[] { name });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show servers
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showServers() throws SessionException {
      Request cmd = new Request(Constants.SHOWSERVERS);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show user records found in the db
    * 
    * @param role the role name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showRoles(String role) throws SessionException {
      Request cmd;
      if (role == null)
         cmd = new Request(Constants.SHOWROLES);
      else
         cmd = new Request(Constants.SHOWROLES, new String[] { role });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show locks of records found in the db
    * 
    * @param args Array of arguments in position
    * @param typeExpr the name or wildcard expression for filetype
    * @param nameExpr the name or wildcard expression for files
    * @param lockValue value of file lock, acts a filter
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showLocks(String[] args) throws SessionException {
      Request cmd;
      
      String typeExpr  = "*";
      String fileExpr  = "*"; 
      String lockValue = "*";
      
      if (args.length > 0)
      {
          typeExpr = args[0];
      }
      if (args.length > 1)
      {
          fileExpr = args[1];
      }
      if (args.length > 2)
      {
          lockValue = args[2];
      }
      
      cmd = new Request(Constants.SHOWLOCKS, new String[] { typeExpr, 
                                              fileExpr, lockValue});
      
      return (this._proxy.put(cmd));
   }
   
   /**
    * Method to show user records found in the db
    * 
    * @param name the user name
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int showUsers(String name) throws SessionException {
      Request cmd;
      if (name == null)
         cmd = new Request(Constants.SHOWUSERS);
      else
         cmd = new Request(Constants.SHOWUSERS, new String[] { name });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to shutdown server
    * 
    * @param timeout the timeout in seconds
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int shutdown(String timeout) throws SessionException {
      Request cmd;
      cmd = new Request(Constants.SHUTDOWN, new String[] { timeout });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to close this admin channel. Do this by appending the close command
    * at the head of the requests queue. The ServerProxy will then remove any
    * admin requests for from the request queue. If all referneces, file types
    * and admin for this server have been closed, then the connection to the
    * server will be gracefully closed.
    * 
    * @return the transaction id for tracking this command.
    */
   public final int close() {
      Request cmd = new Request(); // Default command is quit.
      this._logger.trace("Queuing requested command " + cmd.getCommand());
      return (this._proxy.putExpedited(cmd));
   }
   
   /**
    * Method to move files from one filetype to another within the same
    * server group.
    * @param args source filetype, destination filetype, options, expression
    * @param preserve Preserve flag, if true, then modification flag in 
    *        database will not be modified
    * @param replace Replaces existing file on the server if true
    * @return transaction id for tracking this command
    * @throws SessionException
    */
   public final int moveBetweenFileTypes(String[] args, boolean preserve, 
                                 boolean replace) throws SessionException 
   {
       Request cmd;
       Character modifier = null;
       
       //determine modifier based on the boolean flags
       if (!preserve && replace)
           modifier = new Character(Constants.REPLACE_NO_PRESERVE);
       else if (preserve && replace)
           modifier = new Character(Constants.REPLACE_FILE);
       else if (!preserve && !replace)
           modifier = new Character(Constants.NO_PRESERVE);
       //preserve AND !replace => no modifier
       
       //create a new move files request
       cmd = new Request(Constants.MOVEFILES, args);
       
       //if modifier non-null, set as request modifier
       if (modifier != null)
           cmd.setModifier(modifier.charValue());
       
       return (this._proxy.put(cmd));
   }
   
   /**
    * Method to modify a filetype field.
    * @param args filetype, field, value    
    * @return transaction id for tracking this command
    * @throws SessionException
    */
   public final int modifyFileType(String[] args) throws SessionException 
   {
       Request cmd;
       Character modifier = null;
       
       String ft    = args[0];
       String field = args[1];
       String value = args[2];
       
       //determine modifier based on the boolean flags
       if (field.equalsIgnoreCase("checksum"))
       {
           modifier = new Character(Constants.SETCHECKSUM);
       }
       else if (field.equalsIgnoreCase("location"))
       {
           modifier = new Character(Constants.SETLOCATION);
       }
       else if (field.equalsIgnoreCase("logdeleterecord"))
       {
           modifier = new Character(Constants.SETLOGDELRECORD);
       } 
       else if (field.equalsIgnoreCase("receipt"))
       {
           modifier = new Character(Constants.SETRECEIPT);
       }
       else if (field.equalsIgnoreCase("spaceReserved"))
       {
           modifier = new Character(Constants.SETSPACERESERVED);
       }
       else if (field.equalsIgnoreCase("threshold"))
       {
           modifier = new Character(Constants.SETTHRESHOLD);
       }
       
       String[] newArgs = new String[] { ft, value };
       
       //create a new move files request
       cmd = new Request(Constants.MODIFYFT, newArgs);
       
       //if modifier non-null, set as request modifier
       if (modifier != null)
           cmd.setModifier(modifier.charValue());
       
       return (this._proxy.put(cmd));
   }
   
   /**
    * Method to log a message from admin client to the server log.
    * 
    * @param message Message to write to log
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int logMessage(String message) throws SessionException {
      Request cmd = new Request(Constants.LOGMESSAGE, new String[] {message});
      return (this._proxy.put(cmd));
   }
   
   /**
    * Method to modify the set of capabilities associated with a role.
    * 
    * @param args Array of arguments = [roleName, operation, capString].
    * operation is one of {add, delete, set}, and capString is a
    * string of the integer interpretation.
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int modifyRole(String[] args) throws SessionException {
       
       String role  = args[0];
       String op    = args[1];
       String caps  = args[2];
      
       String[] newArgs = new String[] {role, caps};
       char modifier;
       
       if (op.equals("add"))
           modifier = Constants.ADDCAPABILITIES;
       else if (op.equals("delete"))
           modifier = Constants.DELETECAPABILITIES;
       else if (op.equals("set"))
           modifier = Constants.SETCAPABILITIES;
       else
           throw new SessionException("Unknown modifyRole operation: "+op,
                                      Constants.INVALIDMOD);
       
       Request cmd = new Request(Constants.MODIFYROLE, newArgs);
       cmd.setModifier(modifier);
       
       return (this._proxy.put(cmd));
   }
   
   
   /**
    * Method to modify user access level.
    * 
    * @param args Array of arguments = [userName, access level, switch].
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int modifyUserAccess(String[] args) throws SessionException {
       
       if (args.length != 3)
           throw new SessionException("Expected three arguments",
                                      Constants.MISSINGARG);       
       Request cmd = new Request(Constants.MODIFYUSERACCESS, args);
       
       return (this._proxy.put(cmd));
   }
   
   /**
    * Set file lock values.
    * @param args Array of arguments = [filetype expr,
    * file expr, new value] or [filetype expr, file expr, 
    * new value, old value].
    * @return the transaction id for tracking this command.
    * @throws SessionException when failed to queue request
    */
   public final int setLocks(String[] args) throws SessionException {
       
       if (args.length != 3 && args.length != 4)
           throw new SessionException("Expected 3 or 4 arguments",
                                      Constants.MISSINGARG);       
       Request cmd = new Request(Constants.SETLOCKS, args);
       
       return (this._proxy.put(cmd));
   }
   
}