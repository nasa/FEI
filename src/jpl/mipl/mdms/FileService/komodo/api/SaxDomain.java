/**
 * @copyright Copyright 2004, California Institute of Technology. ALL RIGHTS
 *            RESERVED. U.S. Government Sponsorship acknowledge. 29-6-2000. MIPL
 *            Data Management System (MDMS).
 */
package jpl.mipl.mdms.FileService.komodo.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import jpl.mipl.mdms.FileService.komodo.util.UrlInputStreamLoader;
import jpl.mipl.mdms.FileService.komodo.xml.SaxXmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX XML parsing implementation of Domain object
 * 
 * @author R. Pavlovsky {rich.pavlovsky@jpl.nasa.gov}
 * @version $Id: SaxDomain.java,v 1.32 2012/06/12 23:09:54 ntt Exp $
 */
public class SaxDomain extends Domain {
    
    //------------------------------
    
    // Keys to access hashtable values
    protected static final String URI_KEY = "globalURI", GROUPS_KEY = "groups",
                                  SERVERS_KEY = "servers", TYPES_KEY = "types",
                                  DEFGRP_KEY = "defaultGroup";
    protected static final String DYNAMIC_LOOKUP_KEY = "dynamicTypes";
    protected static final String DYNAMIC_LOOKUP_ALL_KEY = "globalDynamicTypes";
   

    //------------------------------
   
    /* 
     Structure of _hash=
       
         Hashtable():
         
            URI_KEY -----------------> String URL
          
            DEFGRP_KEY --------------> GroupName
          
            GROUPS_KEY --------------> Hashtable():  
                                       
                                          GroupName ----------------> String URL
          
            SERVERS_KEY -------------> Hashtable():  
                                       
                                          GroupName:ServerName -----> ServerInfo
                                    
            TYPES_KEY ---------------> Hashtable():  
                                       
                                          GroupName:TypeName -------> FileTypeInfo
                                    
            DYNAMIC_LOOKUP_KEY ------> Hashtable():  
                                       
                                          GroupName ----------------> Boolean
                                            
            DYNAMIC_LOOKUP_ALL_KEY --> Boolean                                                                         
     */
    protected Hashtable _hash = new Hashtable();
   
   
   
    /**
     * URL to the domain file
     */
    protected URL _url;

   /**
    * Constructor, location of domain file. Parses XML and loads it into a
    * hashtable. See class Javadoc comment for hashtable format.
    * 
    * @param uri the full path to a domain file.
    * @throws SessionException when other failures
    */
   public SaxDomain(String uri) throws SessionException 
   {
       try {
           this._url = new File(uri).toURL();
           //this._url = new File(uri).toURI().toURL();
       } catch (MalformedURLException e) {
           throw new SessionException(e.getMessage(), Constants.MALFORMEDENTRY);
       }
       this._load();
   }

   //----------------------------------------------------------------------
   
   /**
    * Constructor, location of domain file. Parses XML and loads it into a
    * hashtable. See class Javadoc comment for hashtable format.
    * 
    * @param url URL location of domainFile
    * @throws SessionException when session failure
    */
   public SaxDomain(URL url) throws SessionException {
      this._url = url;
      this._load();
   }

    //----------------------------------------------------------------------
   
   /**
    * Private method to do the XML parsing and load the hashtable. See class
    * Javadoc entry for hashtable format.
    * 
    * @throws SessionException when session failure
    */
   protected void _load() throws SessionException {
      InputStream xml = null;
      String schema = null;
      DomainXmlParser parser;
      
      try {
          
         //xml = this._url.openStream();         
         xml = UrlInputStreamLoader.open(_url);
          
         //schema = System.getProperty("schema.file");
         URL schemaUrl = this.getClass().
                 getResource("/jpl/mipl/mdms/FileService/komodo/api/resources/domain.xsd");
         
         if (schemaUrl != null) {
            schema = schemaUrl.toString();
//         if (schema != null) {
            parser = new DomainXmlParser("http://mdms.jpl.nasa.gov/schema/fei "
                  + schema);
         } else
            parser = new DomainXmlParser();

         this._hash = parser.getHashtable(xml);
         
         // Check to see if document URI is set
         // NOTE: a document URI will override the calling domain
         // file data
         String docUri = this._hash.get(URI_KEY).toString();
         if (!docUri.equalsIgnoreCase("")) 
         {
            // if document URI is set, load into hash
             Hashtable hashFromUri = null;
             try {                 
                 //xml = new URL(docUri).openStream();
                 xml = UrlInputStreamLoader.open(docUri);
                 
                 hashFromUri = parser.getHashtable(xml);
             } catch (IOException ioEx) {
                 hashFromUri = null;                 
             } finally {
                 if (xml != null)
                     xml.close();
             }
             
             if (hashFromUri == null || hashFromUri.isEmpty())
             {
                 //we could try reporting an error, or just seamlessly
                 //use the original domain file.  If the domain file
                 //is empty, then we will get an error. 
             }
             else
             {
                 this._hash = hashFromUri;   
             }            
         }

         // Check to see if any group URI's are set
         Hashtable h = (Hashtable) this._hash.get(GROUPS_KEY);
         Enumeration e = h.keys();
         while (e.hasMoreElements()) {
            String group = e.nextElement().toString();
            String uri = h.get(group).toString();
            if (!uri.equalsIgnoreCase("")) {
               //xml = new URL(uri).openStream();
               xml = UrlInputStreamLoader.open(uri);
               Hashtable gh = parser.getHashtable(xml);
               this.addToHash(gh);
            }
         }

      } catch (IOException ioe) {
         throw new SessionException(ioe.getMessage(), Constants.DOMAINIOERR);

      } catch (ParserConfigurationException pce) {
         throw new SessionException(pce.getMessage(), Constants.DOMAINPARSEERR);

      } catch (SAXException se) {
         throw new SessionException(se.getMessage(), Constants.DOMAINPARSEERR);
      }
   }
   
    //----------------------------------------------------------------------

   /**
    * Private method to add data from one hashtable to the Domain data hash
    * 
    * @param h Hashtable data to add to Domain hash
    */
   private void addToHash(Hashtable h) {
      // Add servers data
      Hashtable servers = (Hashtable) this._hash.get(SERVERS_KEY);
      Hashtable sh = (Hashtable) h.get(SERVERS_KEY);
      servers.putAll(sh);
      this._hash.put(SERVERS_KEY, servers);

      // Add filetypes data
      Hashtable types = (Hashtable) this._hash.get(TYPES_KEY);
      Hashtable th = (Hashtable) h.get(TYPES_KEY);
      types.putAll(th);
      this._hash.put(TYPES_KEY, types);
   }

    //----------------------------------------------------------------------
   
   /**
    * Utility method to output the Domain hash to a string
    * 
    * @return String contents of domain hash
    */
   public String toString() {
      StringBuffer buf = new StringBuffer();
      LinkedList groups = this.getGroupNames();
      try {
         for (int i = 0; i < groups.size(); i++) {
            String group = groups.get(i).toString();
            buf.append("Server group " + group
                  + " has the following file types:\n");
            LinkedList types = this.getFileTypeNames(group);
            for (int j = 0; j < types.size(); j++) {
               buf.append(types.get(j).toString());
               if (j <= types.size() - 2)
                  buf.append(", ");
            }
            buf.append("\n");
         }
      } catch (SessionException se) {
         System.out.println("Session Exception caught: " + se.getMessage());
      }

      return buf.toString();
   }

    //----------------------------------------------------------------------
   
   /**
    * Get the default server group specified in domain file. Returns an empty
    * string if default group not specified in domain file.
    * 
    * @return default server group name
    */
   public String getDefaultGroup() {
      return (String) this._hash.get(DEFGRP_KEY);
   }

    //----------------------------------------------------------------------
   
   /**
    * Get a list of sorted server group names
    * 
    * @return list of server group names
    */
   public LinkedList getGroupNames() {
      LinkedList l = new LinkedList();
      Hashtable h = (Hashtable) this._hash.get(GROUPS_KEY);
      Enumeration e = h.keys();
      while (e.hasMoreElements())
         l.add(e.nextElement().toString());

      //Sort server groups alphabetically by name
      Collections.sort(l);

      return l;
   }
   
    //----------------------------------------------------------------------
   
   /**
    * Get a list of sorted server group names
    * 
    * @return list of server group names
    */
   public LinkedList getServerNames(String group) {
      LinkedList l = new LinkedList();
      Hashtable h = (Hashtable) this._hash.get(SERVERS_KEY);
      Enumeration e = h.keys();
      while (e.hasMoreElements())
      {
          String key = e.nextElement().toString();
          String keyGroup  = FileType.extractServerGroup(key);
          String keyServer = FileType.extractFiletype(key);

          if (group.equalsIgnoreCase(keyGroup))
              l.add(keyServer);
      }
      
      //Sort server groups alphabetically by name
      Collections.sort(l);

      return l;
   }
   
    //----------------------------------------------------------------------
   
   /**
    * Returns true if servergroup passed in as parameter is defined
    * @param servergroup Name of server group to query, case-insensitive
    * @return True if servergroup exists, false otherwise
    */
   
   public boolean isGroupDefined(String servergroup)
   {
       boolean found = false;
       List l = getGroupNames();
       
       Iterator it = l.iterator();
       while (it.hasNext())
       {
           String entry = it.next().toString();
           if (entry.equalsIgnoreCase(servergroup))
               found = true;
       }
       
       return found;
   }
   
    //----------------------------------------------------------------------
   
   /**
    * Get a list of sorted file type names, does not respect server group
    * namespace.
    * 
    * @return list of file type names
    * @throws SessionException when operation fails
    * @deprecated doesn't respect server group namespace, use
    *             getFileTypeNames(String groupName) instead
    */
   public LinkedList getFileTypeNames() throws SessionException {
      LinkedList l = new LinkedList();
      Hashtable h = (Hashtable) this._hash.get(TYPES_KEY);
      Enumeration e = h.keys();
      while (e.hasMoreElements()) {
         String key = e.nextElement().toString();
         String type = key.substring(key.indexOf(":") + 1);
         if (!l.contains(type))
            l.add(type);
      }

      //Sort server groups alphabetically by name.
      Collections.sort(l);

      return l;
   }

    //----------------------------------------------------------------------
   
   /**
    * Get a list of sorted file type names for a specified server group
    * 
    * @param groupName the name of the server group
    * @return list of file type names
    * @throws SessionException when operation fails
    */
   public LinkedList getFileTypeNames(String groupName) throws SessionException {
      if (groupName == null)
         throw new SessionException("Group name cannot be null!",
               Constants.DOMAINLOOKUPERR);
      LinkedList l = new LinkedList();
      Hashtable h = (Hashtable) this._hash.get(TYPES_KEY);
      Enumeration e = h.keys();
      while (e.hasMoreElements()) {
         
         String key = e.nextElement().toString();
         String group = FileType.extractServerGroup(key);
         String type  = FileType.extractFiletype(key);
         
         if (group.equalsIgnoreCase(groupName))
            l.add(type);
      }

      //Sort file types alphabetically by name
      Collections.sort(l);

      return l;
   }

    //----------------------------------------------------------------------
   
   /**
    * Returns the server infomation object for input servername parameter.
    * Returns the first serverName match found (regardless of server groups)!
    * Please note that the same server name can be defined in multiple server
    * groups. Returns the first server defined in domain structure if serverName
    * is null. Throws session exception if a match is not found in domain hash.
    * 
    * @param serverName the server name
    * @return the server info object reference
    * @throws SessionException if match not found
    */
   public ServerInfo getServerInfo(String serverName) throws SessionException {
      return this.getServerInfo(null, serverName);
   }
   
    //----------------------------------------------------------------------

   /**
    * Returns the server infomation object for input server name and group name
    * parameters. If servername is null, returns the first server name match
    * found (regardless of server groups)! Please note that the same server name
    * can be defined in multiple server groups. Returns the first servername
    * match (regardless of server groups) if group name is null. Throws session
    * exception if a match is not found in domain hash.
    * 
    * @param serverName the server name
    * @param groupName the server group name
    * @return the server info object reference
    * @throws SessionException if match not found
    */
   public ServerInfo getServerInfo(String groupName, String serverName)
         throws SessionException {
      Hashtable h = (Hashtable) this._hash.get(SERVERS_KEY);
      Enumeration e = h.keys();
      while (e.hasMoreElements()) {
          
         String key    = e.nextElement().toString();
         String group  = FileType.extractServerGroup(key);
         String server = FileType.extractFiletype(key);
         
         if (groupName == null && serverName != null) {
            //return first server name match regardless of server group
            if (server.equalsIgnoreCase(serverName))
               return (ServerInfo) h.get(key);

         } else if (serverName == null
               || (group.equalsIgnoreCase(groupName) && server
                     .equalsIgnoreCase(serverName))) {
            // If serverName is null or if serverName and groupName match,
            // return ServerInfo object
            return (ServerInfo) h.get(key);
         }
      }

      // throw exception if no match
      throw new SessionException("Unable to find " + serverName
            + " host information", Constants.DOMAINLOOKUPERR);
   }

    //----------------------------------------------------------------------
   
   /**
    * Returns the server at the specified index, or null if index is out of
    * bounds.
    * 
    * @param index the index entry to the server list
    * @return the server info object reference
    */
   public ServerInfo getServerInfoByIndex(int index) {
      Hashtable h = (Hashtable) this._hash.get(SERVERS_KEY);
      Object[] o = (Object[]) h.values().toArray();
      if (index < 0 || index >= o.length)
         return null;
      return (ServerInfo) o[index];
   }

    //----------------------------------------------------------------------
   
   /**
    * Method to return a LinkedList of ServerInfo objects for a given file type
    * name and server group
    * 
    * @param groupName the server group name
    * @param typeName the file type name
    * @return LinkedList of server info objects
    * @throws SessionException when operation fails
    */
   public LinkedList getServerInfoFromFileType(String groupName, String typeName)
         throws SessionException {
      LinkedList l = new LinkedList();
      
      if (typeName != null)
      {
          Hashtable h = (Hashtable) this._hash.get(TYPES_KEY);
          Enumeration e = h.keys();
          while (e.hasMoreElements()) {
             String key   = e.nextElement().toString();
             String group = FileType.extractServerGroup(key);
             String type  = FileType.extractFiletype(key);

             //if match of group and filetype, add all serverInfo's
             if ((groupName == null || group.equalsIgnoreCase(groupName)) &&
                  type.equalsIgnoreCase(typeName))  
             {
                 FileTypeInfo ftInfo = (FileTypeInfo) h.get(key);
                 List serverNames = ftInfo.getServers();
                 for (int i = 0; i < serverNames.size(); ++i)
                 {                     
                     String serverName = (String) serverNames.get(i);
                     ServerInfo sInfo = ftInfo.getServerInfo(serverName);
                     if (sInfo != null && !l.contains(sInfo))
                         l.add(sInfo);
                 }
             } 
          }
      }
      
      if (l.size() == 0)
         // throw exception if no match
         throw new SessionException("Unable to find server information for "
               + groupName + ":" + typeName, Constants.DOMAINLOOKUPERR);

      return l;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Method to return a LinkedList of ServerInfo objects for a given server
    * group
    * 
    * @param groupName the server group name
    * @return LinkedList of server info objects
    * @throws SessionException when operation fails 
    */
   public LinkedList getServerInfoFromGroup(String groupName)
                                              throws SessionException {
      LinkedList l = new LinkedList();
      
      if (groupName != null)
      {
          Hashtable h = (Hashtable) this._hash.get(TYPES_KEY);
          Enumeration e = h.keys();
          while (e.hasMoreElements()) {
             String key   = e.nextElement().toString();
             String group = FileType.extractServerGroup(key);
             String type  = FileType.extractFiletype(key);

             //if match of group and filetype, add all serverInfo's
             if (group.equalsIgnoreCase(groupName))  
             {
                 FileTypeInfo ftInfo = (FileTypeInfo) h.get(key);
                 List serverNames = ftInfo.getServers();
                 for (int i = 0; i < serverNames.size(); ++i)
                 {                     
                     String serverName = (String) serverNames.get(i);
                     ServerInfo sInfo = ftInfo.getServerInfo(serverName);
                     if (sInfo != null && !l.contains(sInfo))
                         l.add(sInfo);
                 }
             } 
          }
      }
      
      if (l.size() == 0)
         // throw exception if no match
         throw new SessionException("Unable to find server information for "
               + "server group " + groupName, Constants.DOMAINLOOKUPERR);

      return l;
   }
   
    //----------------------------------------------------------------------
   
   /**
    * Method to return a LinkedList of ServerInfo objects for a given file type
    * name and server group
    * 
    * @param groupName the server group name
    * @param typeName the file type name
    * @return LinkedList of server info objects
    * @throws SessionException when operation fails
    */
   public LinkedList getServerInfoFromFileType2(String groupName, String typeName)
         throws SessionException {
      LinkedList l = new LinkedList();
      Hashtable h = (Hashtable) this._hash.get(SERVERS_KEY);
      Enumeration e = h.keys();
      while (e.hasMoreElements()) {
         String key   = e.nextElement().toString();
         String group = FileType.extractServerGroup(key);
         String type  = FileType.extractFiletype(key);
         if (groupName == null && typeName != null) {
            // add server info for typename
            if (type.equalsIgnoreCase(typeName))
               l.add((ServerInfo) h.get(key));

         } else if (group.equalsIgnoreCase(groupName)
               && type.equalsIgnoreCase(typeName)) {
            // If group and type names match, add ServerInfo
            // object to LinkedList
            l.add((ServerInfo) h.get(key));
         }
      }

      if (l.size() == 0)
         // throw exception if no match
         throw new SessionException("Unable to find server information for "
               + groupName + ":" + typeName, Constants.DOMAINLOOKUPERR);

      return l;
   }

   //----------------------------------------------------------------------
   
   /**
    * Returns the file type information object for input file type name
    * parameter. If type name is null, returns the first type name found
    * (regardless of server groups)! Please note that the same file type name
    * can be defined in multiple server groups. Returns the first type name
    * match (regardless of server groups) if group name is null. Throws session
    * exception if a match is not found in domain hash.
    * 
    * @param typeName the file type name
    * @return the file type info object reference
    * @deprecated please use the getFileType(String groupName, String typeName)
    *             method
    * @throws SessionException if match is not found
    */
   public FileTypeInfo getFileType(String typeName) throws SessionException {
      return this.getFileType(null, typeName);
   }

   /**
    * Returns the file type information object for input file type name
    * parameter. 
    * 
    * If the group is specified, then a filetype info object is only
    * returned if that group contains that filetype.
    * 
    * If group name is null, checks if filetype is defined for
    * the default group. If not found, it checks all other groups
    * and makes the following decision.  If single group matches, then
    * that group is returned.  If multiple groups match, then method
    * throws an exception with ambiguity message.
    * 
    * Please note that the same file type name can be defined in multiple 
    * server groups. Throws session exception if a match is not found 
    * in domain hash.
    * 
    * @param groupName the server group name
    * @param typeName the file type name
    * @return the file type object
    * @throws SessionException if unique match is not found
    */
   public FileTypeInfo getFileType(String groupName, String typeName)
                                            throws SessionException {
      Hashtable h = (Hashtable) this._hash.get(TYPES_KEY);
      Enumeration e = h.keys();
      Hashtable matches = new Hashtable();
      Vector fileTypes = new Vector();
      
      while (e.hasMoreElements()) {
         String key = e.nextElement().toString();
         String group = FileType.extractServerGroup(key);
         String type  = FileType.extractFiletype(key);

         if (type.equalsIgnoreCase(typeName)) {
            FileTypeInfo fti = (FileTypeInfo) h.get(key);
            String groupLowerKey = fti.getGroup().toLowerCase(); 
            matches.put(groupLowerKey, fti);
         }
      }

      // check the specified server group for first match
      Object value = null;
      if (groupName != null)
      {
         String groupLowerLookup = groupName.toLowerCase();
         value = matches.get(groupLowerLookup);
         if (value != null)
             return (FileTypeInfo) value;
      }
      else
      {
          //try default group
          String defGroupName = getDefaultGroup();
          if (defGroupName != null)
          {
              String groupLowerLookup = defGroupName.toLowerCase();
              value = matches.get(groupLowerLookup);
              if (value != null)
                  return (FileTypeInfo) value;
          }
          
          // if the file type is in another group and it is unique within the
          // the domain file, then just let the caller have that reference
          if (matches.size() == 1) {
             e = matches.keys();
             return (FileTypeInfo) matches.get(e.nextElement());
          }
          
          // we have problem here. the file type is not found within the
          // specified server group and we have more than one match. It is
          // time to report the problem.
          if (matches.size() > 1) {
             String msg = "Multiple matches for file type name:\n";
             e = matches.elements();
             while (e.hasMoreElements()) {
                FileTypeInfo ft = (FileTypeInfo) e.nextElement();
                msg += "\"" + ft.getGroup() + ":" + ft.getName() + "\"\n";
             }
             throw new SessionException(msg, Constants.DOMAINLOOKUPERR);
          }
      }

      groupName = (groupName == null) ? groupName = "" : groupName + ":";

      throw new SessionException("Unable to find file type \"" + groupName
            + typeName + "\" in Domain", Constants.DOMAINLOOKUPERR);
   }

   //----------------------------------------------------------------------
   
   /**
    * Method to return the domain file's URI
    * 
    * @return the domain file URI
    */
   public String getUri() {
      return this._url.toString();
   }
   
   //----------------------------------------------------------------------
   
   protected ContentHandler getParserContentHandler()
   {
       return new DomainHandler();
   }
   
   //----------------------------------------------------------------------
   
   public boolean isGroupDynamic(String groupname)
   {
       boolean isDynamic = false;
       
       //check the global setting first
       isDynamic = (Boolean) this._hash.get(SaxDomain.DYNAMIC_LOOKUP_ALL_KEY);
       
       
       //if global not enabled, check group level
       if (!isDynamic)
       {
           Hashtable h = (Hashtable) this._hash.get(SaxDomain.DYNAMIC_LOOKUP_KEY);
           
           String lowGroup = groupname.toLowerCase();
           Boolean groupDynamic = (Boolean) h.get(lowGroup);
           isDynamic = (groupDynamic != null && groupDynamic.booleanValue());               
       }
       
       return isDynamic;
   }
   
}

//=========================================================================
//=========================================================================

/**
 * Implementation class for FEI Domain SAX XML parser. We're using Apache
 * Xerces-J for our SAX parser, but the code should be portable enough to
 * plug-in other parsers. NOTE: To use this parser class, please pass in the
 * following System parameter:
 * <PRE>
 *    -Dorg.xml.sax.driver=org.apache.xerces.parsers.SAXParser
 *    -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl
 * </PRE>
 * SAX Parsing uses a ContentHandler class to capture the XML parsing events.
 * NOTE: does not return an XML DOM Tree like DOM. The Domain XML Parser uses
 * the DomainHandler class to capture and store XML data in a hashtable.
 */

class DomainXmlParser extends SaxXmlParser {

   /**
    * Constructor, references super class constructor and passes a new
    * DomainHandler class. NOTE: XML validation is disabled.
    */
   public DomainXmlParser() {
      super(new DomainHandler(), false);
   }

   /**
    * Constructor, references super class constructor and passes DomainHandler
    * and XML schema file location. NOTE: XML validation is enabled.
    * 
    * @param schema String location of XML schema file.  This string should follow
    *   the convention used to specify the xsi:schemaLocation attribute
    *   of an xml instance document.  Target namespace should be 
    *   included with the schema location and a space should separate the two. 
    *   Eg: "http://mdms.jpl.nasa.gov/schema/fei http://mdms.jpl.nasa.gov/schema/fei/domain.xsd" 
    * 
    */
   public DomainXmlParser(String schema) {
      super(new DomainHandler(), schema, true);
   }

   /**
    * Parses and returns a hashtable of XML Domain data.
    * 
    * @param uri String location of XML file
    * @return Hashtable containing Domain data
    * @throws IOException when general I/O failure
    * @throws SAXException when parsing or XML validation error
    */
   public Hashtable getHashtable(String uri) throws IOException,
         ParserConfigurationException, SAXException {
      if (uri == null && uri == "")
         throw new IOException("XML uri cannot be null.");

      return this.getHashtable(new FileInputStream(new File(uri)));
   }

   /**
    * Parses and returns a hashtable of XML Domain data.
    * 
    * @param is String location of XML file
    * @return Hashtable containing Domain data
    * @throws IOException when general I/O failure
    * @throws SAXException when parsing or XML validation error
    */
   public Hashtable getHashtable(InputStream is) throws IOException,
         SAXException, ParserConfigurationException {
      if (is == null)
         throw new IOException("XML input stream cannot be null.");

      this.parse(is);
      DomainHandler handler = (DomainHandler) this.getHandler();
      return handler.getHashtable();
   }
}

//=========================================================================
//=========================================================================

/**
 * ContentHandler implementation class for FEI Domain XML file parsing. SAX
 * parsing triggers events that are captured by this ContentHandler class. The
 * XML Domain data is validated and parsed and the data is put in a Hashtable.
 * The Hashtable has the following format:
 *
 * <pre>
 * 
 *        KEY      VALUE 
 *        uri      String URI location or null (if not defined in XML doc) 
 *        groups   Hashtable 
 *                 KEY     VALUE 
 *                 name    String URI location or null (if not defined) 
 *                 ...
 *        servers  Hashtable 
 *                 KEY     VALUE 
 *                 group:name  ServerInfo object 
 *                 ... 
 *        types    Hashtable
 *                 KEY     VALUE 
 *                 group:name  FileTypeInfo object
 *  
 * </pre>
 * 
 * The ServerInfo object wraps up data about a server like hostname, port,
 * encryption scheme used, etc. The FileTypeInfo object encapsulates a komodo
 * FileType object and ServerInfo lookup.  
 * 
 * During processing, a priority is set for each server info object.  
 * This handler uses the ordering index of servers of a filetype as 
 * the priority, starting at 0.
 */

class DomainHandler extends DefaultHandler {

   private Stack _stack = null;

   private Hashtable _hash = null;

   private ServerInfo _serverInfo = null;

   private String _groupName = null, _serverName = null, _groupUri = null;

   private boolean _isValid = true;
   
   private int _serverIndex;
   
   private Boolean _groupLookup = false;
   

   /**
    * Method is called when SAX parsing of XML content begins
    * 
    * @throws SAXException when SAX parsing failure
    */
   public void startDocument() throws SAXException {
      this._stack = new Stack();
      this._hash = new Hashtable();
      // init Domain URI and Default Group with an empty String
      this._hash.put(SaxDomain.URI_KEY, new String());
      //this._hash.put(SaxDomain.DEFGRP_KEY, new String());
      // init Groups, Servers and Types with empty Hashtables
      this._hash.put(SaxDomain.GROUPS_KEY, new Hashtable());
      this._hash.put(SaxDomain.SERVERS_KEY, new Hashtable());
      this._hash.put(SaxDomain.TYPES_KEY, new Hashtable());
      
      //value is a hash: groupName->Boolean
      this._hash.put(SaxDomain.DYNAMIC_LOOKUP_KEY, new Hashtable());
      this._hash.put(SaxDomain.DYNAMIC_LOOKUP_ALL_KEY, Boolean.FALSE);
   }

   /**
    * Method is called for each start tag of XML element
    * 
    * @param namespaceUri XML namespace
    * @param localName The name of the XML tag minus prefix (if specified)
    * @param qName The fully qualified XML tag name with prefix
    * @param atts XML tag attributes
    * @throws SAXException when SAX parsing failure
    */
   public void startElement(String namespaceUri, String localName,
         String qName, Attributes atts) throws SAXException {

      // Push a StringBuffer on the stack to capture XML tag
      // characters data.
      StringBuffer buf = new StringBuffer();
      this._stack.push(buf);

      // Get attributes from tags, attributes are only available
      // on the start tag of an element
      if (localName.equalsIgnoreCase("group")) {
         this._groupName = atts.getValue("name");
         this._groupUri = atts.getValue("groupURI");
         this._serverIndex = 0; //reset index
         
         String dynTypesVal = atts.getValue("dynamicTypes");
         this._groupLookup = Boolean.valueOf(dynTypesVal);
         

      } else if (localName.equalsIgnoreCase("server")) {
         this._serverName = atts.getValue("name");
         this._serverInfo = new ServerInfo(this._serverName);
         this._serverInfo.setGroupName(this._groupName);  
         //set priority from order
         this._serverInfo.setPriority(this._serverIndex++);

      } else if (localName.equalsIgnoreCase("vft")) {
         this._serverName = new String("vft");
         this._serverInfo = new ServerInfo(this._serverName);
         this._serverInfo.setGroupName(this._groupName);
         //set priority from order
         this._serverInfo.setPriority(this._serverIndex++);
      }
   }

   /**
    * Characters method is called for data within XML tags. NOTE: this method
    * can be called multiple times for each tag, so it is important to use the
    * StringBuffer to concat the data
    * 
    * @param chars Char array (including white space if not normalized)
    * @param start int start of char array
    * @param length int length of char array
    */
   public void characters(char[] chars, int start, int length) {
      StringBuffer buf = (StringBuffer) this._stack.peek();
      buf.append(chars, start, length);
   }

   /**
    * Method is called when SAX parsing of XML content ends
    * 
    * @throws SAXException when SAX parsing failure
    */
   public void endDocument() throws SAXException {
   }

   /**
    * Method is called for each end tag of XML element NOTE: it is even called
    * when an empty tag is used (ie. <example />)
    * 
    * @param namespaceUri XML namespace
    * @param localName The name of the XML tag minus prefix (if specified)
    * @param qName The fully qualified XML tag name with prefix
    * @throws SAXException when SAX parsing failure
    */
   public void endElement(String namespaceUri, String localName, String qName)
         throws SAXException {
      StringBuffer buf = (StringBuffer) this._stack.pop();

      if (localName.equalsIgnoreCase(SaxDomain.URI_KEY)) {
         // Global URI for entire Domain file, there
         // can only be one...
         this._hash.put(SaxDomain.URI_KEY, buf.toString());

      } else if (localName.equalsIgnoreCase(SaxDomain.DEFGRP_KEY)) {
         // Default server group for Domain file
         this._hash.put(SaxDomain.DEFGRP_KEY, buf.toString());

      } else if (localName.equalsIgnoreCase(SaxDomain.DYNAMIC_LOOKUP_ALL_KEY)) {
          
          String  lookupAllStr = buf.toString();
          Boolean lookupAll    = Boolean.valueOf(lookupAllStr);
          
          // Default server group for Domain file
          this._hash.put(SaxDomain.DYNAMIC_LOOKUP_ALL_KEY, lookupAll);

       } else if (localName.equalsIgnoreCase("group")) {
          // If groupURI is set, add to groups hash else
          // add an empty string
          Hashtable h = (Hashtable) this._hash.get(SaxDomain.GROUPS_KEY);
          if (this._groupUri != null)
             h.put(this._groupName, this._groupUri);
          else
             h.put(this._groupName, new String());

          this._hash.put(SaxDomain.GROUPS_KEY, h);
         
          //-------
         
          h = (Hashtable) this._hash.get(SaxDomain.DYNAMIC_LOOKUP_KEY);
          h.put(this._groupName.toLowerCase(), Boolean.valueOf(this._groupLookup));
         
          this._hash.put(SaxDomain.DYNAMIC_LOOKUP_KEY, h);
         
          //-------
         
          this._groupName = null;
          this._groupLookup = Boolean.FALSE;

      } else if (localName.equalsIgnoreCase("server") ||
                 localName.equalsIgnoreCase("vft")) {
          
         // serverInfo object is complete (ie. hostname, port, etc are not
         // null)
         // get servers hashtable from data hashtable
         Hashtable h = (Hashtable) this._hash.get(SaxDomain.SERVERS_KEY);
         // add serverInfo ojbect to hash indexed by <groupName>:<serverName>
         h.put(this._groupName + ':' + this._serverName, this._serverInfo);
         this._hash.put(SaxDomain.SERVERS_KEY, h);
         this._serverInfo = null;
         this._serverName = null;

      } else if (localName.equalsIgnoreCase("host")) {
         this._serverInfo.setHostName(buf.toString());

      } else if (localName.equalsIgnoreCase("port")) {
         this._serverInfo.setPort(new Integer(buf.toString()).intValue());

      } else if (localName.equalsIgnoreCase("auth")) {
         this._serverInfo.setAuth(buf.toString());

      } else if (localName.equalsIgnoreCase("communication")) {
         this._serverInfo.setCommunication(buf.toString());

      } else if (localName.equalsIgnoreCase("fileType")) {
         FileTypeInfo ftInfo = new FileTypeInfo(this._groupName, buf.toString());
         
         // if this filetype tag is not within the server tag
         // add server info of all servers in the server group
         // to the file type info
         // else only add the server info for the server
         // that is defined by ther server tag that this
         // filetype tag is within.
         if (this._serverInfo == null) {
             Hashtable sh = (Hashtable) this._hash.get(SaxDomain.SERVERS_KEY);
             Enumeration e = sh.keys();
             while (e.hasMoreElements()) {
                 String key = e.nextElement().toString();
                 String group = key.substring(0, key.indexOf(':'));
                 if (group.equalsIgnoreCase(this._groupName)) {
                     ServerInfo serverInfo = (ServerInfo) sh.get(key);
                     ftInfo.setServerInfo(serverInfo.getName(), serverInfo);
                 }
             }
         } else
             ftInfo.setServerInfo(this._serverInfo.getName(),this._serverInfo);
         
         Hashtable fh = (Hashtable) this._hash.get(SaxDomain.TYPES_KEY);
         fh.put(this._groupName + ':' + buf.toString(), ftInfo);
         this._hash.put(SaxDomain.TYPES_KEY, fh);
      }
   }

   /**
    * This method is called when the XML parser encounters an error condition.
    * The document does not validate successfully.
    * 
    * @throws SAXException
    * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
    */
   public void error(SAXParseException exception) throws SAXException {
      this._isValid = false;

      throw new SAXException("Domain File Parsing Error: "
            + exception.getMessage() + " at line " + exception.getLineNumber()
            + ", column " + exception.getColumnNumber());
   }

   /**
    * This method is called when the XML parser encounters a fatal condition.
    * The document does not validate successfully.
    * 
    * @throws SAXException
    * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
    */
   public void fatalError(SAXParseException exception) throws SAXException {
      this._isValid = false;

      throw new SAXException("Domain File Parsing Error: "
            + exception.getMessage() + " at line " + exception.getLineNumber()
            + ", column " + exception.getColumnNumber());
   }

   /**
    * Accessor method to get Hashtable of XML data
    * 
    * @return Hashtable of XML data
    */
   public Hashtable getHashtable() {
      return this._hash;
   }
   
 //=========================================================================
 //=========================================================================
}