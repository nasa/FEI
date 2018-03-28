/**
 * @copyright Copyright 2003, California Institute of Technology. ALL RIGHTS
 *            RESERVED. U.S. Government Sponsorship acknowledged. 29-6-2000.
 *            MIPL Data Management System (MDMS).
 */
package jpl.mipl.mdms.FileService.komodo.api;

import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * A simplified wrapper class for all general user operations defined in the
 * APIs.
 * 
 * @author Rich Pavlovsky, Thomas Huang
 * @version $Id: Client.java,v 1.105 2010/09/01 17:29:01 ntt Exp $
 */
public class Client {

   /** Abort option */
   public static final String OPTION_ABORT = "abort";

   /** Safe read option */
   public static final String OPTION_SAFEREAD = "safeRead";

   /** Compute checksum option */
   public static final String OPTION_COMPUTECHECKSUM = "computeChecksum";

   /** Auto delete option */
   public static final String OPTION_AUTODELETE = "autoDelete";

   /** Receipt option */
   public static final String OPTION_RECEIPT = "receipt";

   /** Replace file option */
   public static final String OPTION_REPLACEFILE = "replaceFile";

   /** Version file option */
   public static final String OPTION_VERSIONFILE = "versionFile";

   /** Restart option */
   public static final String OPTION_RESTART = "restart";

   /** Resume option */
   public static final String OPTION_RESUME = "resume";

   /** Replicate option */
   public static final String OPTION_REPLICATE = "replicate";
   
   /** Diff option */
   public static final String OPTION_DIFF = "diff";
   
   private URL _domainFile = null;
   private String _trustStore = null;
   private String _username = null;
   private String _password = null;
   private String _fileType = null;
   private String _serverGroup = null;
   private String _currentDir = null;
   //private String _replicationRoot = null;
   private FileType _ft = null;
   private Session _session = null;
   private VFT _vftAccess = null;
   private int _securityType = Constants.SSL;
   private boolean _checksum = false;
   private boolean _verbose = false;
   private boolean _veryVerbose = false;
   private Vector _results = new Vector();
   
   private Logger _logger = Logger.getLogger(Client.class.getName());

   /* *
    * Constructor to initialize to the komodo server connection properties. This
    * wrapper class requires SSL encryption.
    * 
    * @param domainFile the komodo domain file
    * @param trustStore the SSL keystore location
    * @throws SessionException when connection fails
    */
   /* *
    * public Client(String domainFile, String trustStore) throws
    * SessionException { this(domainFile, trustStore, false); }
    */

   /**
    * Constructor to initialize to the komodo server connection properties. This
    * wrapper class requires SSL encryption.
    * 
    * @param domainFile the komodo domain file
    * @param trustStore the SSL keystore location
    * @throws SessionException when connection fails
    */
   public Client(String domainFile, String trustStore) throws SessionException 
   {      
      ConfigFileURLResolver resolver = new ConfigFileURLResolver();
      this._domainFile = resolver.getFileURL(domainFile);
      
      this._trustStore = trustStore;
      this._currentDir = System.getProperty("user.dir");
      System.setProperty(Constants.PROPERTY_SSL_TRUSTSTORE, this._trustStore);
      // Optional initialization. Pre-load classes needed by the Komodo
      // API to obfuscate the 20 second delay on the first "use" attempt.
      Session.init();
      this._session = new Session(this._domainFile, this._securityType);
      this._session.setOption(Constants.CHECKSUM, this._checksum);
   }

   /**
    * Constructor to initialize to the komodo server connection properties.
    * Location of domain file is a URL. This wrapper class requires SSL
    * encryption.
    * 
    * @param domainFile URL location of komodo domain file
    * @param trustStore String location of SSL keystore
    * @throws SessionException when connection fails
    */
   public Client(URL domainFile, String trustStore) throws SessionException 
   {
      this._domainFile = domainFile;
      this._trustStore = trustStore;
      this._currentDir = System.getProperty("user.dir");
      System.setProperty(Constants.PROPERTY_SSL_TRUSTSTORE, this._trustStore);
      
      // Optional initialization. Pre-load classes needed by the Komodo
      // API to obfuscate the 20 second delay on the first "use" attempt.
      Session.init();
      this._session = new Session(this._domainFile, this._securityType);
      this._session.setOption(Constants.CHECKSUM, this._checksum);
   }

   /**
    * Another constructor to initialize to the komodo server connection
    * properties. Location of the domain file is a URL. This wrapper class
    * requires SSL encryption.
    * 
    * @param domainFile URL location of komodo domain file
    * @throws SessionException when connection fails
    */
   public Client(URL domainFile) throws SessionException {
      this._domainFile = domainFile;
      this._currentDir = System.getProperty("user.dir");
      // Optional initialization. Pre-load classes needed by the Komodo
      // API to obfuscate the 20 second delay on the first "use" attempt.
      Session.init();
      this._session = new Session(this._domainFile, this._securityType);
      this._session.setOption(Constants.CHECKSUM, this._checksum);
   }

   /**
    * Method to set the domain file reference.
    * 
    * @param domainFile the domain file
    */
   public void setDomainFile(URL domainFile) {
      this._domainFile = domainFile;
   }

   /**
    * Method to set the SSL trust keystore location.
    * 
    * @param trustStore the keystore location.
    */
   public void setTrustStore(String trustStore) {
      this._trustStore = trustStore;
   }
   
   /**
    * Method to set the replication root directory location.
    * 
    * @param rootPath the root location.
    */
   public void setReplicationRoot(String rootPath) throws SessionException {
      //this._replicationRoot = rootPath;
       if (this._session != null)
           this._session.setReplicationRoot(rootPath);
   }

   /**
    * Method to enable/disable verbose to output file metadata.
    * 
    * @param verbose the verbose flag
    */
   public void setVerbose(boolean verbose) {
      this._verbose = verbose;
   }

   /**
    * Method to enable/disable very verbose to output detailed file metadata.
    * 
    * @param veryVerbose the very verbose flag
    */
   public void setVeryVerbose(boolean veryVerbose) {
      this._veryVerbose = veryVerbose;
   }

   /**
    * Method to establish connection to the komodo server on the specified file
    * type.
    * 
    * @param username the user name
    * @param password the password
    * @param fileType the target file type
    * @throws SessionException when connection failed.
    * @deprecated Please use the login(username, password, groupName, typeName)
    *             method. This method does not respect server group namespaces
    *             in FEI!
    */
   public void login(String username, String password, String fileType)
                       throws SessionException 
   {
       login(username, password);
       this._fileType = fileType;      
       this._ft = this._session.open(this._fileType);
   }

   /**
    * Method to establish connection to the komodo server on the specified file
    * type.
    * 
    * @param username the user name
    * @param password the password
    * @param groupName the server group for chosen file type
    * @param typeName the target file type
    * @throws SessionException when connection failed.
    */
   public void login(String username, String password, String groupName,
                     String typeName) throws SessionException {
      login(username, password);
      this._serverGroup = groupName;
      this._fileType = typeName;
      setType(this._serverGroup, this._fileType);
      
   }

   /**
    * Method to establish connection to the set login information for FEI
    * server, but not connect to a file type.
    * 
    * @param username the user name
    * @param password the password
    * @throws SessionException when session object allocation failure.
    */
   public void login(String username, String password) throws SessionException {
      this._username = username;
      this._password = password;
      this._session.setLoginInfo(this._username, this._password);
   }

   /**
    * Utiity method to check if currently logged on.
    *  
    */
   public boolean isLoggedOn() {
      if (this._session == null)
         return false;
      return this._session.isLoggedOn();
   }

   /**
    * Method to disconnect from the server.
    * 
    * @throws SessionException when disconnect failed
    */
   public void logout() throws SessionException {
      if (this._session != null)
            this._session.close();
   }
   
   public void logout (String group, String filetype) throws SessionException {
      if (this._ft != null) {
         if (this._ft.getGroup() == group && this._ft.getName() == filetype)
            this._session.close(this._ft);
         else
            this.logout();
      } else
         this.logout();
   }
   

   /**
    * Method to disconnect from the server immediately
    * 
    * @throws SessionException when disconnect failed
    */
   public void logoutImmediate() throws SessionException {
      if (this._session != null)
         this._session.closeImmediate();
   }

   /**
    * Get the default server group.  This differs from the domain 
    * concept of default server group in that if the
    * current server group is defined, it is returned.  Else
    * if there is a current filetype, its server group is returned.
    * Else return the default server group from the session domain. 
    * Returns an empty string if default server group is not defined 
    * in domain file.
    * 
    * @return default server group name
    */
   public String getDefaultGroup() {
      if (this._serverGroup == null) {
         if (this._ft != null)
            this._serverGroup = this._ft.getGroup();
         else
            this._serverGroup = this._session.getDefaultGroup();
      }
      return this._serverGroup;
   }

   /**
    * Set the default server group.
    * 
    * @param groupName the server group name
    * @throws SessionException when groupName not defined in domain
    */
   public void setDefaultGroup(String groupName) throws SessionException {
       
       if (!this._session.isGroupDefined(groupName))
           throw new SessionException("Group " + groupName
                   + " not found in domain!", Constants.DOMAINLOOKUPERR);
       
      this._serverGroup = groupName;
      this._session.setDefaultGroup(groupName);
   }

   /**
    * Get a sorted list of server groups in the domain.
    * 
    * @return sorted linked list of server group names
    */
   public LinkedList getGroupList() {
      return this._session.getGroupList();
   }

   /**
    * Returns true if servergroup passed in as parameter is defined
    * @param servergroup Name of server group to query
    * @return True if servergroup exists, false otherwise
    */
   public boolean isGroupDefined(String groupName) {
       return this._session.isGroupDefined(groupName);
   }
   
   /**
    * Get a sorted list of file types in a server group
    * 
    * @param groupName the name of the server group
    * @return sorted linked list of server group names
    * @throws SessionException when operation fails
    */
   public LinkedList getFileTypeList(String groupName) throws SessionException {
      return this._session.getFileTypeList(groupName);
   }

   /**
    * Method to query the komodo server for a list of files on the connected
    * file type. Shows all files in the file type.
    * 
    * @return a vector containing error codes of operations.
    * @throws SessionException when operation failed
    */
   public int show() throws SessionException {
      return this.show("*");
   }

   /**
    * Method to query the komodo server for a list of files on the specified
    * file name filtering regular expression.
    * 
    * @param regex the file filter regular expression
    * @return the transaction ID
    * @throws SessionException when operation failed.
    */
   public int show(String regex) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.show(regex);
   }

   /**
    * Method to query the komodo server for a list of files on the specified
    * file name filtering regular expression.
    * 
    * @param files the file name array list
    * @return the transaction ID
    * @throws SessionException when operation failed.
    */
   private int show(String[] files) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.show(files);
   }
   
   /**
    * Show files in current file type, returns file names in Vector
    * 
    * @param regex string regex to query file names
    * @return file names in vector
    * @throws SessionException when show operation fails or filetype is null
    */
   public Vector showNoResults(String regex) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      this._ft.show(regex);
      return this.getResultNames();
   }

   /**
    * Show files in current file type, returns Results objects in Vector
    * 
    * @param regex string regex to query file names
    * @return results objects in vector
    * @throws SessionException when show operation fails or filetype is null
    */
   public Vector showResults(String regex) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      this._ft.show(regex);
      Vector v = new Vector();
      while (getTransactionCount() > 0) {
         Result r = getResult();
         if (r == null) {
            continue;
         }
         // Get the return code for each operation
         if (r.getErrno() == Constants.OK) {
            // Check to see if a name exists
            if (r.getName() != null && !r.getName().equals(""))
               // Add the name to the results vector
               v.add(r);
         }
      }
      return v;
   }

   /**
    * Method to query the komodo server for a list of files added before a date.
    * 
    * @param before the before date
    * @return a vector containing error codes of operations.
    * @throws SessionException when operation fails.
    */
   public int showBefore(Date before) throws SessionException {
      return this.showBefore(before, "*");
   }

   /**
    * Method to query the komodo server for a list of files on the specified
    * file name filtering regular expression added before a date.
    * 
    * @param before the before date
    * @param regex the file filter regular expression
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int showBefore(Date before, String regex) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      Date epoch = null;
      // Get Unix Epoch time to use in datetime based queries
      try {
         epoch = DateTimeUtil.getCCSDSADate("1970-01-01T00:00:00");
      } catch (ParseException pe) {
         throw new SessionException("Error getting UNIX Epoch date",
               Constants.EXCEPTION);
      }
      return this._ft.showBetween(epoch, before, regex);
   }

   /**
    * Method to query the komodo server for a list of files added after a date.
    * 
    * @param after the after date
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int showAfter(Date after) throws SessionException {
      return this.showAfter(after, "*");
   }

   /**
    * Method to query the komodo server for a list of files on the specified
    * file name filtering regular expression added after a date.
    * 
    * @param after the after date
    * @param regex the file filter regular expression
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int showAfter(Date after, String regex) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.showAfter(after, regex);
   }

   /**
    * Method to query the komodo server for a list of files added between two
    * dates.
    * 
    * @param begin the begin date
    * @param end the end date
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int showBetween(Date begin, Date end) throws SessionException {
      return this.showBetween(begin, end, "*");
   }

   /**
    * Method to query the komodo server for a list of files on the specified
    * file name filtering regular expression added between two dates.
    * 
    * @param begin the begin date
    * @param end the end date
    * @param regex the file filter regular expression
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int showBetween(Date begin, Date end, String regex)
         throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.showBetween(begin, end, regex);
   }

   /**
    * Method to add a comment to a file
    * 
    * @param fileName the name of the file
    * @param comment the comment text
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int comment(String fileName, String comment) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.comment(fileName, comment);
   }

   /**
    * Method to show the file types
    * 
    * @return List of file types for the server
    * @throws SessionException when operation fails
    */
   public Vector showTypes() throws SessionException {
      Vector v = new Vector();
      if (this._session == null) {
         throw new SessionException("Session not initialized.",
               Constants.EXCEPTION);
      }
      LinkedList g = this._session.getGroupList();
      for (int i = 0; i < g.size(); i++) {
         String group = g.get(i).toString();
         LinkedList t = this._session.getFileTypeList(group);
         for (int j = 0; j < t.size(); j++) {
            v.add(group + ":" + t.get(j).toString());
         }
      }

      if (v.size() <= 0)
         throw new SessionException("File type lookup error",
               Constants.DOMAINFTNOTFOUND);
      return v;
   }

   /**
    * Process use command to change file type connection
    * 
    * @param fileType the file type to be connected
    * @throws SessionException when connection session fails
    * @deprecated
    */
   public void changeType(String fileType) throws SessionException {
      this.setType(fileType);
   }

   /**
    * Returns currently connected file type name with server group, returns null
    * if not connected to a file type. Returns in the following format: &lt;server
    * group&gt;:&lt;file type&gt;
    * 
    * @return current file type name with server group or null if not set
    */
   public String getType() {
      if (this._ft == null)
         return null;
      return FileType.toFullFiletype(this._ft.getGroup(), this._ft.getName());      
   }

   /**
    * Connects to the file type, closes an old connection if it already exists
    * 
    * @param fileType file type to connect to
    * @throws SessionException when file type connection fails
    * @deprecated Please use the setType(String groupName, String typeName)
    *             method. This method does not respace server group namespaces!
    */
   public void setType(String fileType) throws SessionException {
      this.setType(null, fileType);
   }

   /**
    * Connects to the file type, closes an old connection if it already exists
    * 
    * @param groupName the server group for the chosen file type
    * @param typeName file type to connect to
    * @throws SessionException when file type connection fails
    */
   public void setType(String groupName, String typeName)
         throws SessionException {
       
      if (this._session == null)
         throw new SessionException("Session is null!", Constants.INVALID_LOGIN);
      
      String currentType = this.getType();
      String group = FileType.extractServerGroup(currentType);
      String type  = FileType.extractFiletype(currentType);
      
      //if groupname and filetype name are same, then ignore change request
      if (groupName != null && groupName.equalsIgnoreCase(group) && 
          typeName  != null && typeName.equalsIgnoreCase(type))
         return;
      
      //if existing open type, close it 
      if (currentType != null && this._session.isConnected(group, type))
      {
         this._session.close(this._ft);
   
         //consume the connection closed Result
         while (this._session.getTransactionCount() > 0)
         {
        	 Result r = this._session.result();
        	 if (r == null)
        		 continue;
        	 else if (r.getErrno() != Constants.OK)
        	 {
        		 throw new SessionException("Error occurred while closing " +
        		 		   "filetype '"+currentType+"': "+r.getMessage(),
        		 		   r.getErrno());
        	 }
         }
      }    
      //open the new filetype
      this._ft = this._session.open(groupName, typeName);
   }

   /**
    * Method to add a list of files to the server.
    * 
    * @param files the file list array
    * @return thre transaction ID
    * @throws SessionException when operation fails.
    */
   public int add(String[] files) throws SessionException {
      return this.add(files, null);
   }

   /**
    * Method to add a single file to the server.
    * 
    * @param file the file name
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int add(String file) throws SessionException {
      String[] files = { file };
      return this.add(files, null);
   }

   /**
    * Method to add a single file to the server with comment.
    * 
    * @param file the file name
    * @param comment The comment for the file
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int add(String file, String comment) throws SessionException {
      String[] files = { file };
      return this.add(files, comment);
   }

   /**
    * Method to add a list of files to the server with an associated comment
    * 
    * @param files the file names
    * @param comment the comment for the files
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int add(String[] files, String comment) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.add(files, comment);
   }

   /**
    * Method to replace a list of files on the server
    * 
    * @param files the file array list
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int replace(String[] files) throws SessionException {
      return this.replace(files, null);
   }

   /**
    * Method to replace a single file on the server
    * 
    * @param file the file name
    * @return error number of operation
    * @throws SessionException when operation fails
    */
   public int replace(String file) throws SessionException {
      String[] files = { file };
      return this.replace(files, null);
   }

   /**
    * Method to replace a single file on the server with comment
    * 
    * @param file the file name
    * @param comment The comment for the file
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int replace(String file, String comment) throws SessionException {
      String[] files = { file };
      return this.replace(files, comment);
   }

   /**
    * Method to replace a list of files with an associated comment
    * 
    * @param files the file name array list
    * @param comment the associated comment
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int replace(String[] files, String comment) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.replace(files, comment);
   }
   
   /**
    * Method to register a list of files with an associated comment
    * 
    * @param files the file name array list
    * @param replace flag indicating that file metadata should be re-registered
    * @param force flag indicating that all file metadata should be re-registered, 
    *        including location
    * @param comment the associated comment
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int register(String[] files, boolean replace, boolean force,
                       String comment) throws SessionException 
    {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.register(files, replace, force, comment);
   }
   
   /**
    * Unregisters a file from a file type.
    * 
    * @param fileExpr name expression to unregister in file type
    * @return transaction ID
    * @throws SessionException when general operation failure
    */
   public int unregister(String fileExpr) throws SessionException {
      if (this._ft == null) 
      {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.unregister(fileExpr);
   }
   
   /**
    * Method to rename a file in a file type.
    * 
    * @param oldName the old name of the file
    * @param newName the new name of the file
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int rename(String oldName, String newName) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.rename(oldName, newName);
   }

   /**
    * Method to get files from a server filtered by a file expression.
    * 
    * @param fileExpr the file expression to filter (i.e. *)
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int get(String fileExpr) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.,",
               Constants.INVALID_TYPE);
      }
      return this._ft.get(fileExpr);
   }

   /**
    * Method to get a single file from the server and return contents on the
    * outputstream object.
    * 
    * @param file the file name
    * @param out the outputstream object, not closed on return
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int get(String file, OutputStream out) throws SessionException {
      String[] files = { file };
      return this._ft.get(files, out);
   }

   /**
    * Method to get a list of files from the server.
    * 
    * @param files the file name array list
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int get(String[] files) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.,",
               Constants.INVALID_TYPE);
      }
      return this._ft.get(files);
   }

   /**
    * Method to get a list of files from the server after a given date.
    * 
    * @param date Date (and optional time) to get files after.
    * @return the transaction ID
    * @throws SessionException when operation fails
    */
   public int getAfter(Date date) throws SessionException {
      return this.getAfter(date, "*");
   }

   /**
    * Method to get a list of files from the server after a given date filtered
    * by a file name expression.
    * 
    * @param date Date (and optional time) to get files after.
    * @param String fileExpr file expression to filter results
    * @return the transaction ID
    * @throws SessionException when operation fails.
    */
   public int getAfter(Date date, String fileExpr) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.,",
               Constants.INVALID_TYPE);
      }
      return this._ft.getAfter(date, fileExpr);
   }

   /**
    * Method to get a list of files from the server between two given dates.
    * 
    * @param begin Beginning date
    * @param end Ending date
    * @return Vector containing error numbers of operations
    * @throws SessionException when operation fails
    */
   public int getBetween(Date begin, Date end) throws SessionException {
      return this.getBetween(begin, end, "*");
   }

   /**
    * Method to get a list of files from the server between two given dates
    * filtered by a file name expression.
    * 
    * @param begin Beginning date
    * @param end Ending date
    * @param fileExpr file Expression to filter results
    * @return transaction ID
    * @throws SessionException when operation fails.
    */
   public int getBetween(Date begin, Date end, String fileExpr)
         throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.,",
               Constants.INVALID_TYPE);
      }
      return this._ft.getBetween(begin, end, fileExpr);
   }

   /**
    * Method to get a list of files from the server before a given date.
    * 
    * @param before Files to get BEFORE this date.
    * @return transaction ID
    * @throws SessionException when operation fails.
    */
   public int getBefore(Date before) throws SessionException {
      return this.getBefore(before, "*");
   }

   /**
    * Method to get a list of files from the server before a given date filtered
    * by a file name expression.
    * 
    * @param before Files to get BEFORE this date.
    * @param fileExpr file name expression for filtering
    * @return transaction ID
    * @throws SessionException when operation fails.
    */
   public int getBefore(Date before, String fileExpr) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.,",
               Constants.INVALID_TYPE);
      }
      Date epoch = null;
      // Get Unix Epoch time to use in datetime based queries
      try {
         epoch = DateTimeUtil.getCCSDSADate("1970-01-01T00:00:00");
      } catch (ParseException pe) {
         throw new SessionException("Error getting UNIX Epoch date",
               Constants.EXCEPTION);
      }
      return this._ft.getBetween(epoch, before, fileExpr);
   }

   /**
    * Method to get the latest file added to the fileType.
    * 
    * @param fileExpr The regular expression file selector
    * @return transaction ID
    * @throws SessionException when operation fail.
    */
   public int getLatest(String fileExpr) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.,",
               Constants.INVALID_TYPE);
      }
      return this._ft.getLatest(fileExpr);
   }

   /**
    * Deletes a file from a file type.
    * 
    * @param fileExpr name expression to delete in file type
    * @return transaction ID
    * @throws SessionException when general operation failure
    */
   public int delete(String fileExpr) throws SessionException {
      if (this._ft == null) {
         throw new SessionException("File Type not selected.",
               Constants.INVALID_TYPE);
      }
      return this._ft.delete(fileExpr);
   }

   /**
    * Command to add a virtual file type.
    * 
    * @param name the VFT name
    * @param comment the associated comment
    * @return transaction ID
    * @throws SessionException when general operation failure
    */
   public int addVFT(String name, String comment) throws SessionException {
      boolean notify = false;
      if (this._vftAccess == null)
         this._vftAccess = new VFT(this._session, false);
      return this._vftAccess.addVFT(name, comment, notify);
   }

   /**
    * Delete a virtual file type.
    * 
    * @param name the VFT name to be removed
    * @return transaction ID
    * @throws SessionException when general operation failure
    */
   public int delVFT(String name) throws SessionException {
      if (this._vftAccess == null)
         this._vftAccess = new VFT(this._session, false);
      return this._vftAccess.deleteVFT(name);
   }

   /**
    * Set a reference in a virtual file type.
    * 
    * @param vftName The VFT in which the reference belongs
    * @param refName The name of the reference
    * @param fileType The file type name in which filename belongs
    * @param fileName The name of the file
    * @return transaction ID
    * @throws SessionException when general operation failure
    */
   public int setReference(String vftName, String refName, String fileType,
         String fileName) throws SessionException {
      if (vftName == null || refName == null || fileName == null
            || fileType == null) {
         throw new SessionException("Input parameters are not valid.",
               Constants.EXCEPTION);
      }
      if (this._vftAccess == null)
         this._vftAccess = new VFT(this._session, false);
      return this._vftAccess.setReference(vftName, refName, fileType, fileName);
   }

   /**
    * Create a reference in a VFT.
    * 
    * @param vftName the VFT name
    * @param refName the reference name
    * @param linkName the server-side link
    * @return transaction ID
    * @throws SessionException when general operation failure
    */
   public int addReference(String vftName, String refName, String linkName)
         throws SessionException {
      return this.addReference(vftName, refName, linkName, null);
   }

   /**
    * Create a reference in the current vft with comment.
    * 
    * @param vftName the VFT name
    * @param refName the reference name
    * @param linkName the server-side link
    * @param comment the associated comment
    * @return transaction ID
    * @throws SessionException general operation failure
    */
   public int addReference(String vftName, String refName, String linkName,
         String comment) throws SessionException {
      if (vftName == null || refName == null || linkName == null
            || comment == null) {
         throw new SessionException("Input parameters are not valid.",
               Constants.EXCEPTION);
      }
      if (this._vftAccess == null)
         this._vftAccess = new VFT(this._session, false);
      /* Create the reference in the current vft. */
      return this._vftAccess.addRef(vftName, refName, linkName, comment);
   }

   /**
    * Delete the file system reference, and all history.
    * 
    * @param vftName the name of the virtual file type
    * @param refName the name of the reference in VFT server
    * @return transaction ID
    * @throws SessionException when general operation failure
    */
   public int delReference(String vftName, String refName)
         throws SessionException {
      if (vftName == null || refName == null) {
         throw new SessionException("Input parameters are not valid.",
               Constants.EXCEPTION);
      }
      if (this._vftAccess == null)
         this._vftAccess = new VFT(this._session, false);
      return this._vftAccess.deleteRef(vftName, refName);
   }

   /**
    * Method to update an existing virtual file type
    * 
    * @param vftName the VFT name
    * @return transaction ID
    * @throws SessionException when operation fails
    */
   public int updateVFT(String vftName) throws SessionException {
      return this.updateVFT(vftName, null);
   }

   /**
    * Method to update an existing virtual file type with comment.
    * 
    * @param vftName the VFT name
    * @param comment the associated comment
    * @return transaction ID
    * @throws SessionException when operation fails
    */
   public int updateVFT(String vftName, String comment) throws SessionException {
      if (vftName == null) {
         throw new SessionException("Input parameters are not valid.",
               Constants.EXCEPTION);
      }
      if (this._vftAccess == null)
         this._vftAccess = new VFT(this._session, false);
      return this._vftAccess.update(vftName, comment);
   }

   /**
    * Show current vft list.
    * 
    * @param vftName the VFT name
    * @throws SessionException when network or operation failures
    */
   public void showVFT(String vftName) throws SessionException {
      if (vftName == null) {
         throw new SessionException("Input parameter is not valid.",
               Constants.EXCEPTION);
      }
      Date date = null;
      if (this._vftAccess == null)
         this._vftAccess = new VFT(this._session, false);
      // First, show information about each vft.
      this._vftAccess.showVFT(vftName, date);
      this._getVFTResults();
   }

   /**
    * Internal show vft results ...
    * 
    * @return operation status. Constants.OK success.
    * @throws SessionException when network or operation failures
    */
   private int _getVFTResults() throws SessionException {
      //Now get the results
      Result result;
      int count = 0;
      int status = Constants.VFTNOTFOUND;
      while ((result = this._session.result()) != null) {
         status = result.getErrno();
         if (status == Constants.OK) {
            // Just use as separator. Also, if we're displaying information
            // about
            // a single VFT, we don't want to see the separator.
            if (count++ > 0)
                this._logger.info("\n---------------");            
               //System.out.println("\n---------------");
            
            this._logger.info(this._getVFTInfoString(result));
            //System.out.println(this._getVFTInfoString(result));
         } else
           this._logger.info(result.getMessage()); 
            //System.out.println(result.getMessage());
      }
      return status;
   }

   /**
    * Format a VFT information string.
    * 
    * @param vft the VFT result.
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
      if (this._verbose || this._veryVerbose) {
         if (vft.getTitle() != null)
            vftInfo.append("\nTitle: \"" + vft.getTitle() + "\"");
         if (vft.getComment() != null)
            vftInfo.append("\nComment: \"" + vft.getComment() + "\"");
      }
      return vftInfo.toString();
   }

   /**
    * Get current transaction count for the session
    * 
    * @return the transaction count
    * @throws SessionException when operation fails
    */
   public int getTransactionCount() throws SessionException {
      return this._session.getTransactionCount();
   }

   /**
    * Get a result object
    * 
    * @return the result object
    * @throws SessionException when operation fails
    */
   public Result getResult() throws SessionException {
      return this._session.result();
   }

   /**
    * Get a result object after time delay
    * 
    * @param timeDelay the number of milliseconds to wait to get result
    * @return the result object
    * @throws SessionException when operation fails
    */
   public Result getResult(int timeDelay) throws SessionException {
      return this._session.result(timeDelay);
   }

   /**
    * Method to retrieved queued results
    * 
    * @return Vector containing Result objects
    * @throws SessionException when operation fail
    */
   public Vector getResults() throws SessionException {
      if (this._results.isEmpty())
         this._getResults();
      return this._results;
   }

   /**
    * Internal Method to retrieved queued results
    * 
    * @throws SessionException when operation fail
    */
   private void _getResults() throws SessionException {
      Result result;
      while ((result = this._session.result()) != null) {
         this._results.add(result);
      }
   }

   /**
    * Method to clear previous results from queue
    * 
    * @throws SessionException when operation fails
    */
   public void clearResults() throws SessionException {
      this._results.clear();
   }

   /**
    * Method to retrieved error codes from operation results
    * 
    * @return Vector containing Integer result error numbers
    * @throws SessionException when operation fails
    */
   public Vector getResultCodes() throws SessionException {
      Vector v = new Vector();
      this._getResults();
      Enumeration e = this._results.elements();
      while (e.hasMoreElements()) {
         Result r = (Result) e.nextElement();
         v.add(new Integer(r.getErrno()));
      }
      return v;
   }

   /**
    * Method to retrieved result messages from operation results
    * 
    * @return Vector containing String result messages
    * @throws SessionException when operation fails
    */
   public Vector getResultMsgs() throws SessionException {
      Vector v = new Vector();
      this._getResults();
      String str = null;
      Enumeration e = this._results.elements();
      while (e.hasMoreElements()) {
         Result r = (Result) e.nextElement();
         str = r.getMessage();
         if (str != null)
            v.add(str);
      }
      return v;
   }

   /**
    * Method to retrieved result filenames from operation results
    * 
    * @return Vector containing String result filenames
    * @throws SessionException when operation fails
    */
   public Vector getResultNames() throws SessionException {
      Vector v = new Vector();
      while (getTransactionCount() > 0) {
         Result r = getResult();
         if (r == null) {
            continue;
         }
         //			 Get the return code for each operation
         if (r.getErrno() == Constants.OK) {
            // Check to see if a name exists
            if (r.getName() != null && !r.getName().equals(""))
               // Add the name to the results vector
               v.add(r.getName());
         }
      }
      return v;
   }

   /**
    * Change the directory used by the client. This is useful for redirecting
    * output or changing to a directory containing batch scripts.
    * 
    * @param dir The directory to change to
    * @throws SessionException when operation fails
    */
   public final void changeDir(String dir) throws SessionException {
      File f;
      String cdup = ".." + File.separator;
      String usrhome = "~" + File.separator;
      if (dir.equals("..") || dir.equals(cdup)) {
         this._currentDir = this._currentDir.substring(0, this._currentDir
               .lastIndexOf(File.separator));
      } else if (dir.startsWith("..")) {
         String tmp = this._currentDir;
         while (dir.startsWith("..")) {
            if (dir.equals("..") || dir.equals(cdup))
               dir = "";
            else
               dir = dir.substring(dir.indexOf(File.separator) + 1);
            tmp = tmp.substring(0, tmp.lastIndexOf(File.separator));
         }
         if (dir.equals(""))
            dir = tmp;
         else
            dir = tmp + File.separator + dir;
         f = new File(dir);
         if (!f.exists() || !f.isDirectory()) {
            throw new SessionException("Directory " + dir + " not found.",
                                       Constants.NOTADIRECTORY);
         } else {
            this._currentDir = dir;
         }
      } else if (dir.equals("~") || dir.equals(usrhome)) {
         this._currentDir = System.getProperty("user.home");
      } else if (dir.length() > 0 && dir.charAt(0) == '~') {
         if (dir.indexOf(File.separator) > -1)
            this._currentDir = System.getProperty("user.home") + File.separator
                  + dir.substring(2);
         else {
            String tmp = System.getProperty("user.home");
            tmp = tmp.substring(0, tmp.lastIndexOf(File.separator) + 1);
            this._currentDir = tmp + dir.substring(1);
         }
      } else {
         if (dir.indexOf(File.separator) == -1)
            dir = _currentDir + File.separator + dir;
         f = new File(dir);
         if (!f.exists() || !f.isDirectory()) {
             throw new SessionException("Directory " + dir + " not found.",
                                        Constants.NOTADIRECTORY);             
         } else {
            this._currentDir = dir;
         }
      }
      // Set the current directory for the session. This changes
      // the directory for all sub classes too. For example, CCL
      // Processor new file type commands will use this new directory
      if (this._session != null)
         this._session.setDirectory(_currentDir);
   }

   /**
    * Method to return the client's current directory.
    * 
    * @return the client's current directory.
    */
   public String getDir() {
      return this._session.getDir();
   }

   /**
    * Method to handle change session settings (set command). Valid commands
    * are: Client.OPTION_{ABORT,SAFEREAD,COMPUTECHECKSUM,
    * AUTODELETE,RECEIPT,REPLACEFILE,RESTART,RESUME,REPLICATE}
    * 
    * @param command the command to set
    * @param value the value of the command
    * @throws SessionException when operation fails
    */
   public void set(String command, boolean value) throws SessionException {
      if (command.equalsIgnoreCase(OPTION_ABORT)) {
         if (this._session != null)
            this._session.setOption(Constants.ABORTALLONERR, value);
      } else if (command.equalsIgnoreCase(OPTION_SAFEREAD)) {
         if (this._session != null)
            this._session.setOption(Constants.SAFEREAD, value);
      } else if (command.equalsIgnoreCase(OPTION_COMPUTECHECKSUM)) {
         if (this._session != null)
            this._session.setOption(Constants.CHECKSUM, value);
      } else if (command.equalsIgnoreCase(OPTION_AUTODELETE)) {
         if (this._session != null)
            this._session.setOption(Constants.AUTODEL, value);
      } else if (command.equalsIgnoreCase(OPTION_RECEIPT)) {
         if (this._session != null)
            this._session.setOption(Constants.RECEIPTONXFR, value);
      } else if (command.equalsIgnoreCase(OPTION_REPLACEFILE)) {
         if (this._session != null)
            this._session.setOption(Constants.FILEREPLACE, value);
      } else if (command.equalsIgnoreCase(OPTION_VERSIONFILE)) {
         if (this._session != null)
            this._session.setOption(Constants.FILEVERSION, value);
      } else if (command.equalsIgnoreCase(OPTION_RESTART)) {
         if (this._session != null)
            this._session.setOption(Constants.RESTART, value);
      } else if (command.equalsIgnoreCase(OPTION_RESUME)) {
         // TODO this should be its own option in the session instead of
         // relying on the restart and computechecksum options.
         if (this._session != null) {
            this._session.setOption(Constants.RESTART, value);
            this._session.setOption(Constants.CHECKSUM, value);
         }
      } else if (command.equalsIgnoreCase(OPTION_REPLICATE)) {
          if (this._session != null) {
             this._session.setOption(Constants.REPLICATE, value);
          }
      } else if (command.equalsIgnoreCase(OPTION_DIFF)) {
          if (this._session != null) {
              this._session.setOption(Constants.DIFF, value);
           }
      } else {
         throw new SessionException("Incorrect session option!",
                                     Constants.EXCEPTION);
      }
   }

   /**
    * Method to return session settings. Valid commands are:
    * Client.OPTION_{ABORT,SAFEREAD,COMPUTECHECKSUM,AUTODELETE,
    * RECEIPT,REPLACEFILE,RESTART,RESUME,REPLICATE,DIFF}
    * 
    * @param command the command to set
    * @return value the value of the command
    * @throws SessionException when operation fails
    */
   public boolean isSet(String command) throws SessionException {
      boolean value = false;

      if (command.equalsIgnoreCase(OPTION_ABORT)) {
         if (this._session != null)
            value = this._session.getOption(Constants.ABORTALLONERR);
      } else if (command.equalsIgnoreCase(OPTION_SAFEREAD)) {
         if (this._session != null)
            value = this._session.getOption(Constants.SAFEREAD);
      } else if (command.equalsIgnoreCase(OPTION_COMPUTECHECKSUM)) {
         if (this._session != null)
            value = this._session.getOption(Constants.CHECKSUM);
      } else if (command.equalsIgnoreCase(OPTION_AUTODELETE)) {
         if (this._session != null)
            value = this._session.getOption(Constants.AUTODEL);
      } else if (command.equalsIgnoreCase(OPTION_RECEIPT)) {
         if (this._session != null)
            value = this._session.getOption(Constants.RECEIPTONXFR);
      } else if (command.equalsIgnoreCase(OPTION_REPLACEFILE)) {
         if (this._session != null)
            value = this._session.getOption(Constants.FILEREPLACE);
      } else if (command.equalsIgnoreCase(OPTION_VERSIONFILE)) {
         if (this._session != null)
            value = this._session.getOption(Constants.FILEVERSION);
      } else if (command.equalsIgnoreCase(OPTION_RESTART)) {
         if (this._session != null)
            value = this._session.getOption(Constants.RESTART);
      } else if (command.equalsIgnoreCase(OPTION_RESUME)) {
         // TODO this should be its own option in the session instead of
         // relying on the restart and computechecksum options.
         if (this._session != null) {
            value = this._session.getOption(Constants.RESTART)
                  && this._session.getOption(Constants.CHECKSUM);
         }
      } else if (command.equalsIgnoreCase(OPTION_REPLICATE)) {
          if (this._session != null) {
             value = this._session.getOption(Constants.REPLICATE);
          }
      } else if (command.equalsIgnoreCase(OPTION_DIFF)) {
           if (this._session != null) {
               value = this._session.getOption(Constants.DIFF);
            }
       } else {
         throw new SessionException("Incorrect session option!",
                                    Constants.EXCEPTION);
      }

      return value;
   }
   
   
    //---------------------------------------------------------------------
    
    /**
     * Dispatches subscription request to filetype.
     * @param operation Operation constant.  AUTOGETFILES or AUTOSHOWFILES 
     *        from Constants, for example.
     * @param commit Flag indicating whether result information will be persisted
     *        to restart cache
     * @return Request id
     * @throws SessionException if filetype not selected or other session
     * error occurs.
     */
    
     public int subscribe(String operation, boolean commit) throws SessionException
     {
         if (this._ft == null) {
             throw new SessionException("File Type not selected.",
                                        Constants.INVALID_TYPE);
          }
          return this._ft.subscribe(operation, commit);
     }
     
    //---------------------------------------------------------------------
    
    /**
     * Dispatches subscription termination request to filetype.
     * @return Request id
     * @throws SessionException if filetype not selected or other session
     * error occurs.
     */
    
     public int stopSubscribe() throws SessionException
     {
         if (this._ft == null) {
             throw new SessionException("File Type not selected.",
                   Constants.INVALID_TYPE);
          }
          return this._ft.stopSubscribe();
     }
     
     
     //---------------------------------------------------------------------
     
     /**
      * Method to lock filetype, with optional mode parameter.
      * 
      * @param lockMode Option parameter, legal values are 'group'
      * and 'owner'
      * @return the transaction ID
      * @throws SessionException when operation fails.
      */
     public int lockType(String lockMode) throws SessionException {
        if (this._ft == null) {
           throw new SessionException("File Type not selected.,",
                 Constants.INVALID_TYPE);
        }
        return this._ft.lock(lockMode);
     }
     
     //---------------------------------------------------------------------
     
     /**
      * Method to unlock filetype, with optional mode parameter.
      * 
      * @param lockMode Option parameter, legal values are 'group'
      * and 'owner'
      * @return the transaction ID
      * @throws SessionException when operation fails.
      */
     public int unlockType(String lockMode) throws SessionException {
        if (this._ft == null) {
           throw new SessionException("File Type not selected.,",
                 Constants.INVALID_TYPE);
        }
        return this._ft.unlock(lockMode);
     }
     
     //---------------------------------------------------------------------
     
     /**
      * Method to change user password for a server group.
      * 
      * @param oldPassword Old password
      * @param newPassword New password
      * @return the transaction ID
      * @throws SessionException when operation fails.
      */
     
     public int changePassword(String oldPassword, String newPassword) throws SessionException 
     {
        if (this._session == null || !this._session.isLoggedOn()) 
        {
           throw new SessionException("Session not open.",
                                      Constants.EXCEPTION);
        }
        
        return this._session.changePassword(oldPassword, newPassword);        
     }
     
    //---------------------------------------------------------------------
}
