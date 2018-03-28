/**
 * @copyright 2004 California Institute of Technology. ALL RIGHTS RESERVED. U.S.
 *            Government Sponsorship acknowledge. 29-6-2000. Mission Data
 *            Management System (MDMS)
 */

package jpl.mipl.mdms.FileService.komodo.api;

/**
 * BaseClient-side Komodo server info class.
 * 
 * Manage client-side server information.
 * 
 * @author J. Jacobson, R. Pavlovsky
 * @version $Id: ServerInfo.java,v 1.10 2006/08/11 19:55:05 ntt Exp $
 */
public final class ServerInfo implements Comparable {
    
   public static final int NO_PRIORITY = Integer.MAX_VALUE;
    
   private String _name, _hostName, _groupName, _auth, _communication;
   private int _port;
   private String _securityInfo; // E.g., Kerberos realm.
   private ServerProxy _proxy = null; // Connection to server.
   
   //priority level, smaller number => higher priority
   private int _priority = NO_PRIORITY; 
   
   /**
    * Constructor
    * 
    * @param name the server name
    */
   ServerInfo(String name) {
      this._name = name;
   }

   /**
    * Constructor
    * 
    * @param name Server's name
    * @param hostName the host name
    * @param port the tcp port number
    * @param securityInfo the security info string
    */
   ServerInfo(String name, String hostName, int port, String securityInfo) {
      this._name = name;
      this._hostName = hostName;
      this._port = port;
      this._securityInfo = securityInfo;
      this._groupName = null;
   }

   /**
    * Constructor
    * 
    * @param name the server name
    * @param hostName the server hostname
    * @param port the server port
    * @param auth the authentication encryption scheme
    * @param communication the communication encryption type
    * @param groupName the server group name
    */
   ServerInfo(String name, String hostName, int port, String auth,
         String communication, String groupName) {
      this._name = name;
      this._hostName = hostName;
      this._port = port;
      this._auth = auth;
      this._communication = communication;
      //this._securityInfo = securityInfo;
      this._groupName = groupName;
   }

   /**
    * Method to return the name of the server
    * 
    * @return the server name
    */
   public String getName() {
      return this._name;
   }

   /**
    * Method to return the host name
    * 
    * @return the host name
    */
   public String getHostName() {
      return this._hostName;
   }

   /**
    * Set the servers host name
    * 
    * @param hostName the servers host name
    */
   void setHostName(String hostName) {
      this._hostName = hostName;
   }

   /**
    * Method to return the group name for the server
    * 
    * @return the group name
    */
   public String getGroupName() {
      return this._groupName;
   }

   /**
    * Set the servers group name
    * 
    * @param groupName the servers group name
    */
   void setGroupName(String groupName) {
      this._groupName = groupName;
   }

   /**
    * Method to return the tcp port number
    * 
    * @return the port number
    */
   public int getPort() {
      return this._port;
   }

   /**
    * Set the TCP port number
    * 
    * @param port the TCP port number
    */
   void setPort(int port) {
      this._port = port;
   }

   /**
    * Method to return security info string
    * 
    * @return the security info string
    */
   public String getSecurityInfo() {
      return this._securityInfo;
   }

   /**
    * Get the authentication encryption scheme
    * 
    * @return the auth. encryption scheme name
    */
   public String getAuth() {
      return this._auth;
   }

   /**
    * Set the authentication encryption scheme
    * 
    * @param auth name of authentication encryption scheme
    */
   void setAuth(String auth) {
      this._auth = auth;
   }

   /**
    * Get the communications encryption type
    * 
    * @return the communication encryption type name
    */
   public String getCommunication() {
      return this._communication;
   }

   /**
    * Set the communications encryption type
    * 
    * @param communication the encryption type name
    */
   void setCommunication(String communication) {
      this._communication = communication;
   }

   /**
    * Method to return reference to server proxy
    * 
    * @return server proxy object reference
    */
   ServerProxy getProxy() {
      return this._proxy;
   }

   /**
    * Method to set server proxy object reference
    * 
    * @param proxy the server proxy object reference
    */
   void setProxy(ServerProxy proxy) {
      this._proxy = proxy;
   }

   /**
    * toString method to enable printing of server information
    * 
    * @return String The server information
    */
   public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("Server: " + this._name + ", ");
      buf.append("Host: " + this._hostName + ", ");
      buf.append("Port: " + this._port + ", ");
      buf.append("Auth: " + this._auth + ", ");
      buf.append("Communication: " + this._communication + ", ");
      buf.append("Group: " + this._groupName);
      return buf.toString();
   }
   
   /**
    * Sets the priority of this server info object.  The smaller
    * the non-negative integer value, the higher the priority.
    * @param priority Non-negative integer denoting priority of this 
    *        object, NO_PRIORITY to disable priority for this object
    */
   public void setPriority(int priority)
   {
       if (priority >= 0)
           this._priority = priority;
   }
   
   /**
    * Returns the priority of this object.  The smaller the value, the
    * higher the priority.
    * @return Priority of this info object
    */
   public int getPriority()
   {
       return this._priority;
   }
   
   /**
    * Implementation of the Comparable interface.  Compares
    * the priorities of the objects.  Smaller values correspond to higher
    * priorities.
    * Note: this class has a natural ordering that is inconsistent with equals
    * @param obj the Object to be compared
    * @return negative integer, zero, or positive integer as this object has
    * higher, equal, or lower priority than the specified object.
    */
   public int compareTo(Object obj) {
       if (!(obj instanceof ServerInfo))
           return 0;
       
       ServerInfo other = (ServerInfo) obj;
       
       return this._priority - other.getPriority();
   }
}