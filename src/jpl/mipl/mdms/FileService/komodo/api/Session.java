/**
 * @copyright Copyright 2004, California Institute of Technology. ALL RIGHTS
 *            RESERVED. U.S. Government Sponsorship acknowledge. 29-6-2000. MIPL
 *            Data Management System (MDMS).
 */
package jpl.mipl.mdms.FileService.komodo.api;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.util.Closable;
import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * Implements the Session session class. The constructor takes a user name and
 * password, security mode, and domain?
 * 
 * @author J. Jacobson
 * @version $Id: Session.java,v 1.63 2013/06/24 20:36:16 ntt Exp $
 */
public class Session {
    
   private String _userName, _password, _registry = null, _serverGroup = null,
                  _currDir = System.getProperty("user.dir");
   private String _replicationRoot;
   
   private URL _domainFile;
   private Domain _domain;
   private int _transactionCount = 0;
   private int _transactionId = 0;
   private int _connTimeout = Constants.TIMEOUT_NONE;
   private int _closeTranId = Constants.NOT_SET;
   private int _securityModel = Constants.INSECURE;
   private int _tcpStartPort;
   private int _tcpEndPort;
   int _onOffOptions; // Options in effect when commands are issued.
   final LinkedList _adminClients = new LinkedList(); // Administration clients.
   final LinkedList _capabilities = new LinkedList();   
   final LinkedList _qServices = new LinkedList();   
   final LinkedList _vftCapabilities = new LinkedList();
   final Hashtable _openClients = new Hashtable();
   int _userAccess; // admin = 3, write_all = 2, read_all = 1, none = 0.
   
   boolean _addVFT; // If not admin, is this session allowed to add VFT?
   private Vector _results;   
   private final Logger _logger = Logger.getLogger(Session.class.getName());
   private Hashtable _sgClients = new Hashtable();  
   
   //---------------------------------------------------------------------
   
   /**
    * optional initialization <br>
    * 
    * Call to get a head start on loading classes. This is because just loading
    * the classes needed by SSLSecureSocket factory takes ~20 seconds.
    *  
    */
   public static void init() {
      Connection.init();
   }

   //---------------------------------------------------------------------
   
   /**
    * Constructor
    * 
    * @param domainFile the full path to a domain file.
    * @param securityModel Session.SSL, Session.kerberos, or Session.INSECURE
    * @throws SessionException when session init failure
    */
   public Session(String domainFile, int securityModel) throws SessionException 
   {        
       this(ConfigFileURLResolver.resolve(domainFile), securityModel);            
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Another constructor
    * 
    * @param domainFile URL location of domainFile
    * @param securityModel Session.SSL, Session.kerberos, or Session.INSECURE
    * @throws SessionException when session failure
    */
   public Session(URL domainFile, int securityModel) throws SessionException 
   {
      this._domainFile = domainFile;
      this._securityModel = securityModel;
      this._configure();
   }

   //---------------------------------------------------------------------
   
   /**
    * Internal configuration method to setup a session.
    * 
    * @throws SessionException when configuration failed.
    */
   
   private void _configure() throws SessionException 
   {
      this._tcpStartPort = 0;
      this._tcpEndPort = 0;
      this._onOffOptions = 0;
      this._connTimeout = _getInitialConnectionTimeout();
      
      if (this._connTimeout == Constants.TIMEOUT_NONE)
          this._logger.trace("Session not setting an initial timeout.");
      else
          this._logger.trace("Session is using initial timeout of " + 
                             this._connTimeout + "ms.");

      //---------------------------
      
      // Load the domain information. If there is a problem with the
      // parse, or a problem with the domain file content, the Domain
      // constructor will throw a SessionException.
      DomainFactoryIF factory = new DomainFactory();
      
      //get domain object from factory
      this._domain = factory.getDomain(this._domainFile);
      
      //---------------------------
      
      //create results queue
      this._results = new Vector(Constants.RESULTCAPACITY,
                                 Constants.RESULTCAPINCR);
      
      this._userAccess = Constants.NOT_SET;
      this._addVFT = false; // Default is no add vft.
      
      //---------------------------
      
      // Nothing more to do. Login and file type access are handled
      // when file types are opened.
      boolean ok = true;
      this._registry = System.getProperty(Constants.PROPERTY_RESTART_DIR);
      if (this._registry == null)
         this._registry = System.getProperty("user.home") + File.separator
                          + Constants.RESTARTDIR;
      else
         this._registry = this._registry + File.separator
                          + Constants.RESTARTDIR;
      File f = new File(this._registry);
      if (!f.exists())
         ok = f.mkdir();
      if (!ok)
         throw new SessionException("Unable to create restart directory \""
                                    + this._registry + "\".", 
                                    Constants.RESTARTFILEERR);
   }
   
   //---------------------------------------------------------------------

   /**
    * Constructor
    * 
    * @param userName the user name to connect to the service.
    * @param password the user password to connect to the service.
    * @param domainFile the full path to a domain file.
    * @param securityModel Session.SSL, Session.kerberos, or Session.INSECURE
    * @throws SessionException when session failure
    */
   public Session(String userName, String password, String domainFile,
                  int securityModel) throws SessionException 
   {
               
      this._domainFile = ConfigFileURLResolver.resolve(domainFile);
      this._securityModel = securityModel;
      this._configure();
      
      this._userName = userName;
      this._password = password;
      this._registry = System.getProperty(Constants.PROPERTY_RESTART_DIR);
      if (this._registry == null)
         this._registry = System.getProperty("user.home") + File.separator
               + Constants.RESTARTDIR;
      else
         this._registry = this._registry + File.separator
               + Constants.RESTARTDIR;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get the api version.
    * 
    * @return apiVersion The current vervsion represented as a decimal number.
    */
   public static final float getApiVersion() {
      return Constants.APIVERSION;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get the api version string.
    * 
    * @return apiVersionString current version represented as a decimal number.
    */
   public static final String getApiVersionString() {
      return Constants.APIVERSIONSTR;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get restart directory.
    * 
    * @return restart file directory location.
    */
   public String getRegistry() {
      return this._registry;
   }

   //---------------------------------------------------------------------
   
   /**
    * Set login information. Used to reset login name and password. This is used
    * for invalid login recovery.
    * 
    * @param userName the user name
    * @param password the user password
    */
   public final void setLoginInfo(String userName, String password) {
      this._userName = userName;
      this._password = password;
   }
   
   //---------------------------------------------------------------------

   /**
    * Checks if login information is set. Returns true if set, false otherwise.
    * 
    * @return true if login info is set, false otherwise
    */
   public boolean isLoggedOn() {
      if (this._userName == null)
         return false;
      if (this._password == null)
         return false;
      return true;
   }
   
   //---------------------------------------------------------------------

   /**
    * Get the type of security for this session, e.g.: Kerberos, SSL, none.
    * 
    * @return an integer value, represting the type of security.
    */
   public final int getSecurityModel() {
      return this._securityModel;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to enable/disable an option
    * 
    * @param option the option
    * @param value the on/off flag
    * @return previous values of option
    */
   public boolean setOption(int option, boolean value) {
      boolean oldValue = this.getOption(option);
      if (value)
         this._onOffOptions |= option;
      else
         this._onOffOptions &= ~option;
      return oldValue;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to return the option value
    * 
    * @param option the option
    * @return boolean value for option.
    */
   public boolean getOption(int option) {
      return (this._onOffOptions & option) > 0 ? true : false;
   }

   //---------------------------------------------------------------------
   
   /**
    * setDirectory for the session. This applies to functions, like getChecksum
    * (), that operate on file independent of file type.
    * 
    * @param path the string directory
    * @return the string old directory
    * @throws SessionException when session failure
    */
   public String setDirectory(String path) throws SessionException {
      String oldDirectory = this._currDir;
      // Make sure this is a directory. If not, throw an exeception.
      File f = new File(path);
      if (!f.isDirectory())
         throw new SessionException(
               "Path \"" + path + "\" is not a directory.",
               Constants.FILE_NOT_FOUND);
      else
         this._currDir = path;
      return oldDirectory;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get directory associated with this session.
    * 
    * @return String directory.
    */
   public String getDir() {
      return this._currDir;
   }

   //---------------------------------------------------------------------
   
   /**
    * Sets replication root for the session. 
    * @param path the string directory
    * @return the string old directory
    * @throws SessionException when session failure
    */
   public String setReplicationRoot(String path) throws SessionException {
      String oldDirectory = this._replicationRoot;
      // Make sure this is a directory. If not, throw an exeception.
      File f = new File(path);
      if (!f.isDirectory())
         throw new SessionException(
               "Replication root path \"" + path + "\" is not a directory.",
               Constants.FILE_NOT_FOUND);
      else
         this._replicationRoot = path;
      return oldDirectory;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get directory associated with this session.
    * 
    * @return String directory.
    */
   public String getReplicationRoot() {
      return this._replicationRoot;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method to return capabilities specific to the input file type
    * 
    * @param fileType the file type name
    * @return Capabilities for a specific file type.
    * @throws SessionException when session failure
    */
   public Capability getCapabilities(String fileType) throws SessionException {
      Capability cap = null;
      Capability tmp;
      
      String sgName = FileType.extractServerGroup(fileType);
      String ftName = FileType.extractFiletype(fileType);
      String curName, curSgName, curFtName;
      
//      commented out 4/5/2010 for AR116318
      if (true || this._userAccess == Constants.NOT_SET)
         this._loadCapabilities(sgName);

      
      for (int i = 0; i < this._capabilities.size(); i++) {
         tmp = (Capability) this._capabilities.get(i);
         curName = tmp.getName();
         curSgName = FileType.extractServerGroup(curName);
         curFtName = FileType.extractFiletype(curName);
 
         if (ftName.equals(curFtName) && 
             (
              (sgName != null && sgName.equals(curSgName)) ||
              (sgName == null && curSgName.equals(this._serverGroup))         
             )
            )
         {
             cap = tmp;
             break;
         }
         
//         if (tmp.getName().equals(fileType)) {
//            cap = tmp;
//            break;
//         }
      }
      return cap;
   }
   
   /**
    * Method to return capabilities specific to the input file type
    * 
    * @param fileType the file type name
    * @return Capabilities for a specific file type.
    * @throws SessionException when session failure
    */
   public LinkedList getCapabilitiesForGroup(String group) throws SessionException {
       
       LinkedList list = new LinkedList();
       
       if (group == null)
           return list;
           
       Capability tmp;
      
       String curName, curSgName, curFtName;
      
       if (true || this._userAccess == Constants.NOT_SET)
           this._loadCapabilities(group);       
      
       for (int i = 0; i < this._capabilities.size(); i++) 
       {
           tmp = (Capability) this._capabilities.get(i);
           curName = tmp.getName();
           curSgName = FileType.extractServerGroup(curName);
 
           if (group.equals(curSgName))
           {
               list.add(tmp);
           }
         
//         if (tmp.getName().equals(fileType)) {
//            cap = tmp;
//            break;
//         }
      }
      return list;
   }
   
   //---------------------------------------------------------------------

   /**
    * method to return a list of capabilities for all file types
    * 
    * @return a list of Capabilities for all file types where a user has
    *         capabilities.
    * @throws SessionException when session failure
    */
   public LinkedList getCapabilities() throws SessionException {
      // Maybe we should re-load each time we're called ?
      if (true || this._userAccess == Constants.NOT_SET)
          this._loadCapabilities();
      
      return _capabilities;
   }

   //---------------------------------------------------------------------
   
   /**
    * getVFTCapabilities
    * 
    * @return a list of Capabilities all VFT where a user has capabilities.
    * @throws SessionException when session failure
    */
   public LinkedList getVFTCapabilities() throws SessionException {
      // Maybe we should re-load each time we're called ?
      if (true || this._userAccess == Constants.NOT_SET)
         this._loadCapabilities();
      
      return this._vftCapabilities;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to return a capability object specific to the input vft
    * 
    * @param vft the virtual file type
    * @return Capabilities for a specific VFT
    * @throws SessionException when session failure
    */
   public Capability getVFTCapabilities(String vft) throws SessionException {
      Capability cap = null;
      Capability tmp;
      String sgName = FileType.extractServerGroup(vft);
      String ftName = FileType.extractFiletype(vft);
      String curName, curSgName, curFtName;
      
      if (true || this._userAccess == Constants.NOT_SET)
         this._loadCapabilities(sgName);
      
      for (int i = 0; i < this._vftCapabilities.size(); i++) {
         tmp = (Capability) this._vftCapabilities.get(i);         
         curName = tmp.getName();
         curSgName = FileType.extractServerGroup(curName);
         curFtName = FileType.extractFiletype(curName);
 
         if (ftName.equals(curFtName) && 
             (
              (sgName != null && sgName.equals(curSgName)) ||
              (sgName == null && curSgName.equals(this._serverGroup))         
             )
            )
         {
             cap = tmp;
             break;
         }
         
//         if (tmp.getName().equals(vft)) {
//            cap = tmp;
//            break;
//         }
      }
      return cap;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get user access, Session.ADMIN, Session.READALL, Session.WRITEALL. Returns
    * Session.NOT_SET if not initialized.
    * 
    * @return user access value
    * @throws SessionException when session failure
    */
   public int getUserAccess() throws SessionException {
      if (true || this._userAccess == Constants.NOT_SET)
         this._loadCapabilities();
      
      return this._userAccess;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to check if user has add vft priv
    * 
    * @return true if user has the ability to add VFT.
    * @throws SessionException when session failure
    */
   public boolean getAddVFT() throws SessionException {
      return this._addVFT;
   }

   //---------------------------------------------------------------------
   
   /**
    * Used to set add VFT priv to true or false.
    * 
    * @param okToDoIt the flag to set add vft
    * @throws SessionException when session failure.
    */
   void setAddVFT(boolean okToDoIt) throws SessionException {
      this._addVFT = okToDoIt;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get user access, Session.ADMIN, Session.READALL, Session.WRITEALL.
    * 
    * @return user access string value
    */
   public String getUserAccessStr() {
      String display;
      switch (this._userAccess) {
      case Constants.ADMIN:
         display = "admin";
         break;
      case Constants.WRITE_ALL:
         display = "write_all";
         break;
      case Constants.READ_ALL:
         display = "read_all";
         break;
      default:
         display = "none";
         break;
      }
      return display;
   }

   //---------------------------------------------------------------------
   
   /**
    * Change password. To be safe, only let the user to change passwords when
    * there are no outstanding transactions. This way, we can avoid problems
    * tracking down the correct result before closing our utility connection.
    * 
    * @param oldPassword the old password
    * @param newPassword the new password
    * @return Transaction id.
    * @throws SessionException when session failure
    */
   public int changePassword(String oldPassword, String newPassword)
                                           throws SessionException 
   {
      if (this.getTransactionCount() > 0)
         throw new SessionException(
               "Cannot change password while transactions are pending.",
               Constants.INTRANSACTION);
      
      //throws exception if Admin instance could not be created
      Admin ac = this._utilityConn();
      
      int transactionId = ac.changePassword(this._userName, oldPassword,
                                            newPassword);
      Result result = this.result(); // Wait to hear from the server proxy.
      // Close our temporary utility instance.
      ac.close();
      while (this.getTransactionCount() > 0)
         this.result(); // Wait for the result of the close.
      ac = null; // Hint
      // Restore transaction count to 1, and repost the result the user
      // wants to see.
      this._incrementTransactionCount();
      result.setEoT();
      this.postResult(result);
      return transactionId; // Return the transaction id for result pickup.
   }

   //---------------------------------------------------------------------
   
   /**
    * Make a domain file.
    * 
    * @param domainFilePath the new domain file path.
    * @param serverGroup Name of server group to be written to domain file
    * @return Transaction id.
    * @throws SessionException when session failure
    */
   public int makeDomainFile(String domainFilePath, String serverGroup) 
                                              throws SessionException {
      if (this.getTransactionCount() > 0)
         throw new SessionException(
               "Cannot make domain file while transactions are pending.",
               Constants.INTRANSACTION);
      
      //list is known to be non-empty, else exception is thrown
      List sList = this._domain.getServerInfoFromGroup(serverGroup);
      ServerInfo sInfo = (ServerInfo) sList.get(0);
      String serverName = sInfo.getName();
      Admin ac = getAdminConnection(serverName, false);
      
      int transactionId = ac.makeDomainFile(domainFilePath);
      Result result = this.result(); // Wait to hear from the server proxy.
      // Close our temporary uitility connection instance.
      ac.close();
      while (this.getTransactionCount() > 0)
         this.result(); // Wait for the result of the close.
      ac = null; // Hint
      // Restore transaction count to 1, and repost the result the user
      // wants to see.
      this._incrementTransactionCount();
      result.setEoT();
      this.postResult(result);
      return transactionId; // Return the transaction id for result pickup.
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set TCP port range
    * 
    * @param start the start value
    * @param end the end value
    * @throws SessionException when session failure
    */
   public void setTcpPortRange(int start, int end) throws SessionException {
      if (start < Constants.MINTCPPORT && end > Constants.MAXTCPPORT) {
         throw new SessionException("TCP port number not between "
               + Constants.MINTCPPORT + " and " + Constants.MAXTCPPORT + ".",
               Constants.TCP_PORT_RANGE);
      }
      if (start > end) {
         throw new SessionException("TCP start port greater than end port.",
               Constants.TCP_PORT_RANGE);
      }
      this._tcpStartPort = start;
      this._tcpEndPort = end;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to get TCP port start value
    * 
    * @return tcp lower port limit for client side port
    */
   public int getTcpStartPort() {
      return this._tcpStartPort;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to get TCP port end value
    * 
    * @return tcp upper port limit for client side port
    */
   public int getTcpEndPort() {
      return this._tcpEndPort;
   }
   
   //---------------------------------------------------------------------

   /**
    * Open a file type
    * 
    * @param fileTypeName file type to open.
    * @return a file type object reference
    * @throws SessionException when session failure
    */
   public FileType open(String fileTypeName) throws SessionException {
      return this.open(null, fileTypeName);
   }

   //---------------------------------------------------------------------
   
   /**
    * Open a file type
    * 
    * @param groupName the server group name
    * @param typeName the file type name
    * @return a file type object reference
    * @throws SessionException when session failure
    */
   public FileType open(String groupName, String typeName)
                                            throws SessionException {
       
      FileTypeInfo ftInfo = this._domain.getFileType(groupName, typeName);
     
      // @TODO not soure if we should throw an exception here or simply
      // return the existing object reference....
      if (ftInfo.getFt() != null)
         return ftInfo.getFt();

      /**
       * if (ft.getFt() != null) { // This file type is already open. throw new
       * SessionException( "File type \"" + typeName + " already open. Create
       * another session for second open on type.", Constants.TYPE_OPEN); }
       */

      // Create a new FileType class. This may involve establishing a
      // connection to a server. The server will be queried for the ftId 
      // for the file type, which serves to both "ping" the server, as 
      // well as to provide a more efficient way to refer to a type. The 
      // file type constructor blocks.
      
      FileType ft = new FileType(this, ftInfo);
      ftInfo.setFt(ft);
      this._openClients.put(ftInfo.getGroup() + ":" + typeName, ftInfo);
      return ftInfo.getFt();
   }

   //---------------------------------------------------------------------
   
   /**
    * Opens a server group.  
    * @param groupName Name of the server group to open
    * @return ServerGroup instance
    * @throws SessionException if error occurs.
    */
   
   public ServerGroup openServerGroup(String groupName) throws SessionException
   {
       ServerGroup sgClient;
       
       if (this._sgClients.containsKey(groupName))
           return (ServerGroup) this._sgClients.get(groupName);
       
       
       List sgInfos = this._domain.getServerInfoFromGroup(groupName);       
       sgClient = new ServerGroup(this, sgInfos);
       this._sgClients.put(groupName, sgClient);
       
       return sgClient;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method to get a server group name from a file type name.
    * 
    * @param fileTypeName the name of the filetype
    * @return The server group name
    * @throws SessionException when session failure
    * @deprecated method not server group namespace aware
    */
   public String getFTServerGroup(String fileTypeName) throws SessionException {
      if (fileTypeName.equalsIgnoreCase("vft")) {
         return this._domain.getServerInfo("vft").getGroupName();
      }
      FileTypeInfo ft = this._domain.getFileType(fileTypeName);
      if (ft == null)
         throw new SessionException("Invalid file type \"" + fileTypeName
               + "\".", Constants.INVALID_TYPE);
      Vector v = ft.getServers();
      ServerInfo s = ft.getServerInfo((String) v.elementAt(0));
      return s.getGroupName();
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to get a server group name from a server name.
    * 
    * @param serverName the name of the filetype
    * @return The server group name
    * @throws SessionException when session failure
    */
   public String getServerGroup(String serverName) throws SessionException {
      ServerInfo s = this._domain.getServerInfo(serverName);
      return (s == null) ? null : s.getGroupName();
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to open an admin session
    * 
    * @param serverName open a server w/o a file type, enrolls connection into
    *           list.
    * @return an admin object reference
    * @throws SessionException when session failure
    */
   public Admin openAdmin(String serverName) throws SessionException {
      Admin ac;
      // Get new admin port on admin port (server's port + 1).
      ac = getAdminConnection(serverName, true);
      // Put new admin client into list for close all.
      this._adminClients.add(ac);
      return ac;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method to open an admin session
    * 
    * @param serverName open a server w/o a file type, enrolls connection into
    *           list.
    * @return an admin object reference
    * @throws SessionException when session failure
    * /
   public QueryService openQueryService(String serverName) throws SessionException {
      QueryService qs;
      // Get new admin port on admin port (server's port + 1).
      qs = getQServiceConnection(serverName, true);
      
      // Put new qclient into list for close all.  
      this._qServices.add(qs);
      
      return qs;
   }
   */
   

   //---------------------------------------------------------------------
   
   /**
    * Method to connection to an admin session
    * 
    * @param serverName open a server w/o a file type.
    * @param useAdminPort flag to indicate to use an defined admin port
    * @return an admin object reference
    * @throws SessionException when session failure.
    */
   public Admin getAdminConnection(String serverName, boolean useAdminPort)
         throws SessionException {
      Admin ac = null;
      
      // Make sure there isn't already an admin client for this server.
      for (int i = 0; i < this._adminClients.size(); i++) {
         ac = (Admin) this._adminClients.get(i);
         if (ac.getServerName().compareTo(serverName) == 0) {
            // This file type is already open.
            throw new SessionException("Admin client for server \""
                  + serverName + "\" already open.", Constants.TYPE_OPEN);
         }
      }
      ac = new Admin(this, serverName, useAdminPort);
      
      return ac;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to connection to an query service session
    * 
    * @param serverName open a server w/o a file type.
    * @param useAdminPort flag to indicate to use an defined admin port
    * @return QueryService object reference
    * @throws SessionException when session failure.
    *b/
   public QueryService getQServiceConnection(String serverName, boolean useAdminPort)
         throws SessionException {
      QueryService qs = null;
      
      // Make sure there isn't already an admin client for this server.
      for (int i = 0; i < this._qServices.size(); i++) {
         qs = (QueryService) this._qServices.get(i);
         if (qs.getServerName().compareTo(serverName) == 0) {
            // This file type is already open.
            throw new SessionException("Query client for server \""
                  + serverName + "\" already open.", Constants.TYPE_OPEN);
         }
      }
      qs = new QueryService(this, serverName, useAdminPort);
      return qs;
   }
   */
   
   //---------------------------------------------------------------------
   
   /**
    * Get server info: port, host.
    * 
    * @param groupName the server group name
    * @param serverName the server name
    * @return a Komodo server info object reference
    */
   ServerInfo getServerInfo(String groupName, String serverName) {
      this._logger.trace("Getting serverInfo for group: " + groupName
            + " ,server: " + serverName);
      try {
         return this._domain.getServerInfo(groupName, serverName);
      } catch (SessionException se) {
         System.out.println(se.getMessage());
      }
      return (ServerInfo) null;
   }
   //---------------------------------------------------------------------
   
   /**
    * Get query server info: port, host.
    * 
    * @param groupName the server group name
    * @param serverName the query server name
    * @return a Komodo query server info object reference
    * /
   ServerInfo getQueryServerInfo(String groupName, String serverName) {
      this._logger.trace("Getting serverInfo for group: " + groupName
            + " , query server: " + serverName);
      try {
         return this._domain.getQueryServerInfo(groupName, serverName);
      } catch (SessionException se) {
         System.out.println(se.getMessage());
      }
      return (ServerInfo) null;
   }
   */
   
   //---------------------------------------------------------------------
   
   /**
    * Method to post a result to the result queue.
    * 
    * @param result the file profile
    */
   public void postResult(Result result) {
      this._logger.trace("Posting result.");
      synchronized (this._results) {         
         this._results.add(result);
         this._logger.trace("Posting result notify.");
         this._results.notify();
         this._logger.trace("Posting result notified.");
      }
   }

   //---------------------------------------------------------------------
   
   /**
    * Get the current transaction count.
    * 
    * @return transaction count
    */
   public int getTransactionCount() {
      synchronized (this) {
         return (this._transactionCount);
      }
   }
   
   //---------------------------------------------------------------------

   /**
    * close all file types, disconnect from all servers, wait for close to
    * complete and throw away all pending results.
    */
   public void closeImmediate() {
      try {
         this.close();
         while (this.getTransactionCount() > 0) {
            this.result();
         }
      } catch (SessionException se) {
         se.printStackTrace();
         return;
      }
   }
   
   //---------------------------------------------------------------------

   /**
    * Close all clients. If the application program wants to synch-up on this
    * close, then wait on the Komodo result queue for the close command result.
    * The close request goes into the queue as a transaction. The result of the
    * close will appear on the results queue.
    * 
    * @return transaction id.
    * @throws SessionException when session failure
    */
   public int close() throws SessionException {
      this.close((FileType) null);
      return this._closeTranId;
   }

   //---------------------------------------------------------------------
   
   /**
    * Start closing all file types, disconnect from all servers. The application
    * may then synch up by calling result until the transaction count goes to
    * zero. If flushResults is true, wait for close to complete and throw away
    * all pending results. Also, close all admin clients.
    * 
    * @param type the file type to close. If null, close all file types.
    * @throws SessionException when session failure
    */
   public void close(FileType type) throws SessionException {
      int connectionCount = this._openClients.size();

      if (type != null) 
      {
    	  String fullFiletype = FileType.toFullFiletype(type.getGroup(), 
    			                                        type.getName());
          FileTypeInfo ftInfo = (FileTypeInfo) this._openClients.remove(
        		 							             fullFiletype);
          if (ftInfo != null && ftInfo.getFt() != null) 
          {
        	  this._logger.trace("Closing " + fullFiletype);
        	  this._closeTranId = type.close();
        	  ftInfo.setFt(null);
        	  this._openClients.remove(fullFiletype);
        	  --connectionCount;
          }
      } 
      else 
      {
         Enumeration tokens = this._openClients.keys();
         while (tokens.hasMoreElements()) {
            FileTypeInfo ftInfo = (FileTypeInfo) this._openClients
                  .remove((String) tokens.nextElement());
            if (ftInfo != null) {
               this._logger.trace("Closing " + ftInfo.getGroup() + ":"
                     + ftInfo.getName());
               FileType ft = ftInfo.getFt();
               if (ft != null) {
                  this._closeTranId = ft.close();
                  ftInfo.setFt(null);
                  --connectionCount;
               }
            }
         }
      }
      /**
       * // Close all open file types in session LinkedList g =
       * this._domain.getGroupNames(); for (int i = 0; i < g.size(); i++) {
       * String group = g.get(i).toString(); LinkedList t =
       * this._domain.getFileTypeNames(group); for (int j = 0; j < t.size();
       * j++) { FileTypeInfo ftInfo = this._domain.getFileType(group, t.get(j)
       * .toString()); if (ftInfo != null) { FileType ft = ftInfo.getFt(); if
       * (ft != null) { connectionCount++; if (type == null || type == ft) {
       * this._closeTranId = ft.close(); ftInfo.setFt(null); connectionCount--; } } } } } }
       */

      Admin ac;
      /* If there are no more file types open, clear capabilities. */
      if (connectionCount == 0) 
      {
         this._userAccess = Constants.NOT_SET;
         this._addVFT = false;
         this._capabilities.clear();
         this._vftCapabilities.clear();
      }
      
      // Close all admin clients open on this session.
      for (int i = 0; i < this._adminClients.size(); i++) {
         ac = (Admin) this._adminClients.get(i);
         ac.close();
      }
      // Remove all reference from admin clients.
      this._adminClients.clear();
      
      //----------------------
      
      Closable c;
      Iterator it = _sgClients.values().iterator();
      while (it.hasNext())
      {
          c = (Closable) it.next();
          c.close();
      }      
      // Remove all reference from server group clients.
      this._sgClients.clear();
   }

   //---------------------------------------------------------------------
   
   /**
    * Close server group instance.  
    * @param sg ServerGroup instance to close.  If null, method returns
    *        immediately.
    */
   public void closeServerGroup(ServerGroup sg)
   {
       if (sg == null)
           return;
       
       String sgName = sg.getName();
       this._logger.trace("Closing " + sgName + "...");
       this._closeTranId = sg.close();
       
       if (this._sgClients.get(sgName) == sg)
       {
           this._sgClients.remove(sgName);           
       }             
   }
   
   //---------------------------------------------------------------------
   
   /**
    * See if a file type already has a server proxy connection.
    * 
    * @param fileTypeName the file type name
    * @return true if is connected
    * @throws SessionException when session failure
    * @deprecated does not respect server group namespace!
    */
   public boolean isConnected(String fileTypeName) throws SessionException {
      // Make sure this is a valid file type.
      FileTypeInfo ft = this._domain.getFileType(fileTypeName);
      if (ft == null) {
         throw new SessionException("File type \"" + fileTypeName
               + "\" not found in domain.", Constants.INVALID_TYPE);
      }
      // Return true if we have a connection.
      return (ft.getFt() != null ? true : false);
   }

   //---------------------------------------------------------------------
   
   /**
    * See if a file type already has a server proxy connection.
    * 
    * @param fileTypeName the file type name
    * @return true if is connected
    * @throws SessionException when session failure
    */
   public boolean isConnected(String groupName, String typeName)
                                         throws SessionException 
   {
      // Make sure this is a valid file type.
      FileTypeInfo ft = this._domain.getFileType(groupName, typeName);
      
      if (ft == null) {
         throw new SessionException("File type \"" + groupName + ":" + typeName
               + "\" not found in domain.", Constants.INVALID_TYPE);
      }
      
      // Return true if we have a connection.
      return (ft.getFt() != null ? true : false);
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Get the next profile representing a command result. Wait forever for the
    * result. Decrements transaction count on EoT. Returns the EoT profile on
    * some commands (add, for example). On commands that can return multiple
    * results, such as get file by regular expression, do not return the EoT
    * profile.
    * 
    * @return a result object. null profile if the transaction count is already
    *         zero.
    * @throws SessionException when session failure
    *  
    */
   public Result result() throws SessionException {
      Result result = null;
      synchronized (this._results) {
         if (this.getTransactionCount() > 0) {
            while (this._results.isEmpty()) {
               try {
                  this._results.wait();
               } catch (InterruptedException e) {
                  this._logger.trace(null, e);
                  throw new SessionException("Unexpected interrupt.",
                        Constants.INTERRUPTED);
               }
            }
            result = (Result) this._results.get(0);
            this._results.remove(0);
            if (result.isEoT()) {
               synchronized (this) {
                  this._transactionCount--;
               }
            }
         }
         
         return (result == null || result.isDoNotShowUser() ? null : result);
      }
   }

   //---------------------------------------------------------------------
   
   /**
    * Get the next profile representing a command result. Block until a
    * specified number of milleseconds for a result.
    * 
    * @param timeDelay time in seconds.
    * @return the result object
    * @throws SessionException when session failure
    */
   public Result result(int timeDelay) throws SessionException {
      Result result = null;
      if (timeDelay < Constants.RESULTMINTIMEOUT
            || timeDelay > Constants.RESULTMAXTIMEOUT) {
         throw new SessionException("Time delay '"+timeDelay+"' not in range ("
               + Constants.RESULTMINTIMEOUT + "," + Constants.RESULTMAXTIMEOUT
               + ")" + ".", Constants.TIMEOUT_RANGE);
      }
      synchronized (this._results) {
         if (this.getTransactionCount() > 0) {
            if (this._results.isEmpty()
                  && timeDelay > Constants.RESULTNOTIMEOUT) {
               try {
                  this._results.wait((long) timeDelay);
               } catch (InterruptedException e) {
                  this._logger.trace(null, e);
                  throw new SessionException("Unexpected interrupt.",
                        Constants.INTERRUPTED);
               }
            }
         }
         
         if (!this._results.isEmpty())
             result = (Result) this._results.get(0);
         
         if (result != null) {
            this._results.remove(0);
            if (result.isEoT()) {
               synchronized (this) {
                  this._transactionCount--;
               }
            }
         }           
         return (result == null || result.isDoNotShowUser() ? null : result);
      }
   }

   //---------------------------------------------------------------------
   
   /**
    * Get new transaction id. Also increments the transaction count.
    * 
    * @return the transaction id
    */
   public synchronized int getTransactionId() {
      this._incrementTransactionCount();
      return ++this._transactionId;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Get user name for this Komodo session.
    * 
    * @return user name
    */
   public final String getUserName() {
      return this._userName;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get password for this Komodo session.
    * 
    * @return the password
    */
   public final String getPassword() {
      return this._password;
   }

   //---------------------------------------------------------------------
   
   /**
    * Get a hashed list of file type names, indexed by server groups
    * 
    * @return the hashed list of file type names
    * @throws SessionException when operation fails
    * @deprecated does not respect server group namespace!
    */
   public LinkedList getFileTypeList() throws SessionException {
      return this._domain.getFileTypeNames();
   }

   //---------------------------------------------------------------------
   
   /**
    * Get a sorted list of file types from a server group.
    * 
    * @param groupName the server group name
    * @return the list of file types
    * @throws SessionException when operation fails
    */
   public LinkedList getFileTypeList(String groupName) throws SessionException {
       return this._domain.getFileTypeNames(groupName);
       //return this._domainManager.getFileTypeList(groupName);
   }

   //---------------------------------------------------------------------
   
   /**
    * Get a sorted list of server group names.
    * 
    * @return the list of file types
    */
   public LinkedList getGroupList() {
      return this._domain.getGroupNames();
   }

   //---------------------------------------------------------------------
   
   /**
    * Returns true if servergroup passed in as parameter is defined
    * @param servergroup Name of server group to query
    * @return True if servergroup exists, false otherwise
    */
   public boolean isGroupDefined(String groupName) {
      return this._domain.isGroupDefined(groupName);
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Get the default server group. Returns an empty string if default server
    * group is not defined in domain file.
    * 
    * @return default server group name
    */
   public String getDefaultGroup() {
      if (this._serverGroup == null)
         this._serverGroup = this._domain.getDefaultGroup();
      return this._serverGroup;
   }

   //---------------------------------------------------------------------
   
   /**
    * Set the default server group.
    * 
    * @param groupName the server group name
    * @throws SessionException when groupName not defined in domain
    */
   public void setDefaultGroup(String groupName) throws SessionException {
      
       if (!this._domain.isGroupDefined(groupName))
           throw new SessionException("Group " + groupName
                   + " not found in domain!", Constants.DOMAINLOOKUPERR);
       
//      LinkedList l = this._domain.getGroupNames();
//      
//      if (!l.contains(groupName))
//         throw new SessionException("Group " + groupName
//               + " not found in domain!", Constants.DOMAINLOOKUPERR);
      
       this._serverGroup = groupName;
   }

   //---------------------------------------------------------------------
   
   /**
    * Dump Komodo class to stderr, and any interesting has-a classes.
    * 
    * @throws SessionException when session failure
    */
   public void dump() throws SessionException {
      System.err.println("Dump The domain class");
      System.err.println(this._domain.toString());
   }

   //---------------------------------------------------------------------
   
   /**
    * Internal function to load capabilities from the first server connection we
    * can find. If no connection has been made yet, use the first server in the
    * domain. Also, be sure to get the close type result off the session queue.
    * Note: This function is only called when no file type has been created for
    * this session.
    * 
    * @throws SessionException when sessionf ailure
    */
   private void _loadCapabilities(String servergroup) throws SessionException {
      // Establishing this connection will load capabilities.
      Admin ac = this._utilityConn(servergroup);
      ac.close();
      // Just flush the result of the close. No other results will
      // have been left on the queue, since if we're loading caps,
      // then there can be no other results, since this is the
      // first connection.
      this.result();
   }
   
   private void _loadCapabilities() throws SessionException
   {
       _loadCapabilities(null);
   }

   //---------------------------------------------------------------------
   
   /*
   * No longer tru:
   * First, a server from the spe default group is attempted.  If unsuccessful,
   * then an arbitrary server (from any group) is attempted.
   */
   
   /**
    * Get temporary "admin" connection. Use this for any non-FT specific
    * operation, such as loading capabilities, or changing password.
    * Servers for a servergroup are attempted until successful. If
    * the parameter is null, then either the Session's server group field
    * is used, OR the default server group.
    * @param servergroup Server group
    * @return an admin object reference
    * @throws SessionException when session failure
    */
   private Admin _utilityConn(String servergroup) throws SessionException {
       
      Admin ac = null;
        
      if (servergroup == null)
      {
          if (this._serverGroup != null)
              servergroup = this._serverGroup;
          else
              servergroup = this._domain.getDefaultGroup();    
      }
                
      //get a list of servers for this group
      List list = this._domain.getServerInfoFromGroup(servergroup);
      
      if (list.isEmpty())
      {
          throw new SessionException("No servers in domain file for '"+
                                     servergroup+"'", Constants.NO_SERVERS);
      }
      
      //we may be iterating, so get size
      final int serverCount = list.size();
      int serverIndex = 0;
  
      //while connection is null and we have more servers to try...
      while (ac == null && serverIndex < serverCount )
      {
          ServerInfo serverInfo = (ServerInfo) list.get(serverIndex);          
          
//          if (serverInfo == null)          
//              serverInfo = this._domain.getServerInfoByIndex(0);           
          
          if (serverInfo != null) {
              try {
                  // Establishing this connection will load capabilities.
                  ac = getAdminConnection(serverInfo.getName(), false);
              } catch (SessionException sesEx) {
                  if (sesEx.getErrno() == Constants.CONN_FAILED)
                  {
                      ac = null;
                      boolean lastOne = serverIndex == serverCount-1;
                      String host = serverInfo.getHostName() + ":" +
                                    serverInfo.getPort();
                      
                      String errMsg = "Connection to server " + host +
                              " failed.  ";
                      if (lastOne)
                          errMsg += "No more servers are available.";
                      else
                          errMsg += "Will attempt next server for group '"+
                              servergroup + "'.";
                      _logger.debug(errMsg);
                      
                      
                      
                  }
                  else
                  {
                      ac = null;
                      throw sesEx;    
                  }                  
              }
          } else {
              throw new SessionException("No servers in domain file.",
                          Constants.NO_SERVERS);              
          }
          
          ++serverIndex;
      }
      
      if (ac == null)
      {
          throw new SessionException("Could not create a connection to " +
          		                     "server group '" + servergroup+ "' ",
          		                     Constants.CONN_FAILED);
      }
      
      return ac;
   }
   
   //---------------------------------------------------------------------
   
   private Admin _utilityConn() throws SessionException {
       return _utilityConn(null);
   }

   //---------------------------------------------------------------------
   
   /**
    * Called by server proxy when new requests are queued.
    */
   private void _incrementTransactionCount() {
      synchronized (this) {
         this._transactionCount++;
      }
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Returns the value of the connection timeout.  
    */
   
   public int getConnectionTimeout()
   {
       return this._connTimeout;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Checks property <code>Constants.PROPERTY_CLIENT_TIMEOUT</code> to see
    * if property is set.  If so, then it attempts to parse that value.  If 
    * successful, that value is used; else no timeout is used.  If property 
    * is not set, then a default is used, <code>Constants.TIMEOUT_DEFAULT
    * </code>.
    * @return Initial timeout value to use by session
    */
   
   private int _getInitialConnectionTimeout()
   {
       int timeout;
       
       //check to see if property is set
       String timeoutStr = System.getProperty(Constants.PROPERTY_CLIENT_TIMEOUT);
       if (timeoutStr != null)
       {
           try {
               timeout = Integer.parseInt(timeoutStr);
           } catch (NumberFormatException nfEx) {
               timeout = Constants.TIMEOUT_NONE;
           }
       }
       else
       {
           timeout = Constants.TIMEOUT_DEFAULT;
       }
       
       if (timeout < 0)
           timeout = Constants.TIMEOUT_NONE;
       
       return timeout;
   }

   //---------------------------------------------------------------------
   
   boolean isCapabilitiesLoaded(String servergroup)
   {
       
       if (servergroup == null)
           return false;
       
       boolean set = false;
       Iterator it = this._capabilities.iterator();
       while (it.hasNext() && !set)
       {
           Capability cap = (Capability) it.next();
           String capName = cap.getName();
           String capServerGroup = FileType.extractServerGroup(capName);
           if (servergroup.equals(capServerGroup))
               set = true;
       }
       
       return set;
   }
   
   //---------------------------------------------------------------------
}