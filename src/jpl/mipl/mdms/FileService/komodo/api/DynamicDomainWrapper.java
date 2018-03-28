package jpl.mipl.mdms.FileService.komodo.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <B>Purpose:<B>
 * This implementation of a Domain wrapper can dynamically load file
 * type information from server if it was not included in original
 * domain file and if dynamic loading for that server group is
 * enabled. 
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DynamicDomainWrapper.java,v 1.10 2016/05/24 22:22:04 ntt Exp $
 *
 */

public class DynamicDomainWrapper extends BaseDomainWrapper
{
    //For logging events
    private final Logger _logger = Logger.getLogger(FileTypeQueryClient.class.getName());
        
    protected Session session;
    
    /** Query client is responsible for communicating with server */
    protected FileTypeQueryClient typeQueryClient;
    
    /** Set of initialized groups (non-dynamic groups are considered initialized by default) */
    protected Set<String> initializedGroupSet;   
    
    /** Map from full filetype name to FileTypeInfo instance */
    protected Map<String, FileTypeInfo> managedTypes;
    
    //---------------------------------------------------------------------
    
    
    /**
     * Constructor
     * @param domain Underlying Domain instance
     * @throws SessionException is session error occurs
     */
    
    public DynamicDomainWrapper(Domain domain) throws SessionException
    {
        this(domain, null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param domain Underlying Domain instance
     * @param session Session instance (can be null)
     * @throws SessionException is session error occurs
     */
    
    public DynamicDomainWrapper(Domain domain, Session session)
                                        throws SessionException
    {
        super(domain);
        this.session = session;     
        
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes this instance.
     */
    
    protected void init() throws SessionException
    {
        //create new client for client-server queries
        this.typeQueryClient = new FileTypeQueryClient();
        
        //init data structures
        this.initializedGroupSet = new HashSet();        
        this.managedTypes        = new Hashtable();

        //configure based on domain info
        initDomainInfo();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Polls domain object for groups that are considered fully initialized
     * (i.e. all the filetypes are included).  Groups that are not in that
     * set will be managed by this class.
     */
    
    protected void initDomainInfo() throws SessionException
    {
        //-------------------------
        
        List groups = this.domain.getGroupNames();
        
        Iterator it = groups.iterator();
        
        //go through list of groups and see which have remote
        //loading enabled.  If not enabled, then add to the set
        //of initialized groups.
        while (it.hasNext())
        {
            String servergroup = it.next().toString();
            
            if (!this.domain.isGroupDynamic(servergroup))
            {
                this.initializedGroupSet.add(servergroup.toLowerCase());
            }            
        }
        
        //-------------------------        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Wrapper function to return filetype metadata.  First, the domain
     * object will be checked.  If found, the result is returned.  Otherwise
     * the filetype is considered managed by this instance.  If the associated
     * servergroup has not yet be fully loaded, a dynamic loading of filetype
     * information will be made.  If successful, the new instance will be 
     * returned.  If already loaded, then a matching data object will be 
     * returned.  If no matching, then a session error is thrown indicating
     * that no type exists.
     * 
     * 
     * 
     * @param groupName Server group name
     * @param typeName Filetype name
     * @throws SessionException if session error occurs
     */
    
    public FileTypeInfo getFileType(String groupName, String typeName)
                                               throws SessionException
    {
        //quick check that group is defined (if specified)
        if (groupName != null && !domain.isGroupDefined(groupName))
        {
            throw new SessionException("Group '"+groupName+"' not found in domain file", 
                                       Constants.DOMAINLOOKUPERR);
        }
        
        //---------------------
        
        //the return value
        FileTypeInfo ftInfo = null;
        
        //---------------------
        
        //check if domain file already has filetype info defined
        try {
             ftInfo = this.domain.getFileType(groupName, typeName);
        } catch (SessionException sesEx) { 
            
            //if group was specified and tagged as fully initialized,
            //then there is nothing more we can do, re-throw the
            //exception
            if (groupName != null && !this.isGroupDynamic(groupName))                 
            //if (groupName != null && !this.isGroupInitialized(groupName))
            {
                throw sesEx;
            }
            
            //otherwise, we might have some work to do. Same errno 
            //associated with multi-matches and no-match from previous 
            //call, so ignore the error for now, assuming a no-match            
            ftInfo = null;
            _logger.debug("Filetype not found in underlying domain. " +
            		      " Checking dynamic results" );
//            _logger.debug(sesEx.getMessage());
            
        }

        //---------------------
        
        //check cache of managed filetypes        
        if (ftInfo == null && groupName != null)
        {                                   
            ftInfo = this.getManagedFiletypeInfo(groupName, typeName);
        }
        
        //---------------------

        //at this point, if non-null, then we are done.  Otherwise, check if
        //group is defined, and if its fully init'ed (all filetypes loaded).  
        // If not inited, then go through the process of dynamically loading filetype
        // info.

        if (ftInfo == null)
        {            
            //groupName specified, only check that group
            if (groupName != null)
            {                
                ensureGroupInitialized(groupName);
                
                //now try and get it again from internal cache            
                ftInfo = this.getManagedFiletypeInfo(groupName, typeName);             
            }
            else
            {
                //default group first
                String defGroup = domain.getDefaultGroup();
                if (defGroup != null)
                {
                    ensureGroupInitialized(defGroup);
                    
                    //now try and get it again from internal cache
                    ftInfo =  this.getManagedFiletypeInfo(defGroup, typeName);
                }
                
                //not found in default group. So now check ALL groups, 
                //but ensure no duplicates
                if (ftInfo == null)
                {
                    List matchingGroups = new ArrayList();
                    
                    //iterate over all groups
                    Iterator it = this.domain.getGroupNames().iterator();
                    while (it.hasNext())
                    {
                        String curGroup = (String) it.next();
                        
                        this.ensureGroupInitialized(curGroup);
                        FileTypeInfo curFt = getManagedFiletypeInfo(curGroup, 
                                                                    typeName);
                        if (curFt != null)
                            matchingGroups.add(curGroup);
                    }
                    
                    //check for duplicates
                    if (matchingGroups.size() > 1) 
                    {
                        String msg = "Multiple groups match for file type name:\n";
                        it = matchingGroups.iterator();
                        while (it.hasNext())
                        {
                            String curGroup = (String) it.next();                                     
                            msg += curGroup + "  ";
                        }
                        throw new SessionException(msg, Constants.DOMAINLOOKUPERR);
                    }
                    else if (matchingGroups.size() == 1)
                    {
                        //single match, yay, use it.
                        
                        String curGroup = (String) matchingGroups.get(0);
                        ftInfo =  this.getManagedFiletypeInfo(curGroup, typeName);
                    }
                    
                } //end_check_all_groups
                 
            } // end_no_group_specified
            
        } //end_dynamic_query_section
            
        //---------------------

        //if you have not found it by now, it does not exist!
        if (ftInfo == null)
        {
            String tmpGroupStr = (groupName == null) ? "" : groupName + ":";

            throw new SessionException("Unable to find file type \"" + tmpGroupStr
                  + typeName + "\" in Domain", Constants.DOMAINLOOKUPERR);
        }

        //---------------------
        
        return ftInfo;
    }

    //---------------------------------------------------------------------
    
    protected void ensureGroupInitialized(String groupName) throws SessionException
    {        
        if (groupName != null && !isGroupInitialized(groupName) )
        {      
            //this goes through the query and caching process
            fullyInitializeGroup(groupName);                    
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Magic version of group initialization.  If groupName is specified,
     * then this is the same as calling the non-magical <code>
     * ensureGroupInitialized()</code>.  If groupName is null and type 
     * is non-null, then the default group will be initialized and if
     * that group does not contain the typeName, then all groups will
     * be initialized.  If groupName is null and typeName null, then
     * only the default group will be initialized. 
     * @param groupName Server group name
     * @param typeName File type name
     * @throws SessionException if session error occurs
     */
    
    protected void ensureGroupInitializedMagic(String groupName, String typeName)
                                                         throws SessionException
    {
        if (groupName != null)
        {
            ensureGroupInitialized(groupName);
        }
        else //no group, try to match filetype
        {
            boolean initializeAllGroups = false;
            
            String defGroup = this.domain.getDefaultGroup();
            
            //if passed in or default, initialize
            if (defGroup != null)
            {
                ensureGroupInitialized(defGroup);
               
                if (typeName != null &&
                    getManagedFiletypeInfo(defGroup, typeName) == null)
                    initializeAllGroups = true;               
            }
            
            
            if (initializeAllGroups)
            {
                List groups = domain.getGroupNames();
                Iterator it = groups.iterator();
                while (it.hasNext())
                {
                    String curGroup = it.next().toString();
                    ensureGroupInitialized(curGroup);
                }
            }
            
        } //end_if_no_group
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns a list of filetypes associated with a groupname.
     * First, the domain object is checked. If no results found
     * by group is declared, then the server is queried for
     * ft info.
     * @param groupName Server group name
     * @return List of filetypes of the server group parameter
     * @throws SessionException is session error occurs
     */

    public LinkedList getFileTypeNames(String groupName) throws SessionException
    {

        if (groupName == null)
        {
            throw new SessionException("Group name cannot be null!",
                                       Constants.DOMAINLOOKUPERR);
        }

        //fix as part of AR117963        
        this.ensureGroupInitialized(groupName);
        
        //-------------
        
        //local set for removing duplicates
        HashSet ftSet     = new HashSet();
        
        //-------------
        
        List localList    = null; 
        try {
            localList = this.domain.getFileTypeNames(groupName);
        } catch (SessionException sesEx) {
            localList = null;
        }
        if (localList != null && !localList.isEmpty())
        {
            ftSet.addAll(localList);
        }
        
        
        //-------------
        
        List managedList = null;
        managedList = getManagedTypeNamesForGroup(groupName);
        if (managedList != null && !managedList.isEmpty())
        {
            //ftList = new LinkedList();
            ftSet.addAll(managedList);
        }
        
        //-------------
        
        //create sorted list to return to caller
        LinkedList ftList = new LinkedList(ftSet);
        Collections.sort(ftList);
        
        return ftList;
    }

    
    /**
     * Method to return a LinkedList of ServerInfo objects for a given file type
     * name and server group
     * 
     * @param groupName the server group name
     * @param typeName the file type name
     * @return LinkedList of server info objects
     * @throws SessionException when operation fails
     */
    public LinkedList getServerInfoFromFileType(String groupName, 
                         String typeName) throws SessionException 
    {
        
       LinkedList l = null;
       
       if (typeName == null)
       {
           String msg = "Unable to find server information for ";
           if (groupName != null)
               msg += groupName + " and ";
           msg += "unspecified filetype";
           
           throw new SessionException(msg, Constants.DOMAINLOOKUPERR);
       }
           
       //--------------------------
       
       try {
           l = this.domain.getServerInfoFromFileType(groupName, typeName);
       } catch (SessionException sesEx) {
           l = null;
       }
       
       if (l == null || l.isEmpty())
       {          
           if (groupName != null)
           {
               l = new LinkedList();
              
               this.ensureGroupInitialized(groupName);
               FileTypeInfo ftInfo = this.getManagedFiletypeInfo(groupName, typeName);
               List serverNames = ftInfo.getServers();
               for (int i = 0; i < serverNames.size(); ++i)
               {                     
                   String serverName = (String) serverNames.get(i);
                   ServerInfo sInfo = ftInfo.getServerInfo(serverName);
                   if (sInfo != null && !l.contains(sInfo))
                       l.add(sInfo);
               }
           }
           else
           {
               //get the default group and make recursive call
               String defGroup = this.getDefaultGroup();
               if (defGroup != null)
               {
                   try { 
                       l = getServerInfoFromFileType(defGroup, typeName);
                   } catch (SessionException sesEx) {
                       l = null;
                   }
               }
               
               
               if (l == null || l.isEmpty())
               {
                   //iterate over all groups and make recursive call
                   //add unique results to list
                   
                   List groupList = this.domain.getGroupNames();
                   Iterator it = groupList.iterator();
                   while (it.hasNext())
                   {
                       String curGroup = it.next().toString();
                       List curList = null;
                       
                       try { 
                           curList = getServerInfoFromFileType(curGroup, typeName);
                       } catch (SessionException sesEx) {
                           curList = null;
                       }
                       
                       if (curList != null)
                       {
                           Iterator lIt = curList.iterator();
                           while (lIt.hasNext())
                           {
                               ServerInfo curInfo = (ServerInfo) lIt.next();
                               if (!l.contains(curInfo))
                                   l.add(curInfo);
                           }
                       }
                   }
               }
           }
       }
       
       // throw exception if no match
       if (l.isEmpty())       
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
       LinkedList l = null; //new LinkedList();
       
       try {
           l = this.domain.getServerInfoFromGroup(groupName);
       } catch (SessionException sesEx) {
           l = null;
       }
         
       if (l == null || l.isEmpty())
       {
          l= new LinkedList();
       
          if (groupName != null)
          {
               
             ensureGroupInitialized(groupName);
               
             //----------------------
               
             List ftNames = getManagedTypeNamesForGroup(groupName);
             Iterator it = ftNames.iterator();
             while (it.hasNext())
             {
                 String curType = it.next().toString();
                 FileTypeInfo ftInfo = this.getManagedFiletypeInfo(groupName, curType);
                   
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
         
       if (l.isEmpty())
          // throw exception if no match
          throw new SessionException("Unable to find server information for "
                + "server group " + groupName, Constants.DOMAINLOOKUPERR);

       return l;
    }
    
    //---------------------------------------------------------------------
    
    protected List getManagedTypeNamesForGroup(String groupName)
    {
        List<String> ftList = new ArrayList<String>();
        
        
        Set<String> keySet = this.managedTypes.keySet();
        for (Iterator<String> it = keySet.iterator(); it.hasNext(); )
        {
            String fullFtNameLower = it.next();
            String curGroup = FileType.extractServerGroup(fullFtNameLower);
            //String curType  = FileType.extractFiletype(fullFtName);
            
            if (groupName.equalsIgnoreCase(curGroup))
            {
                //get the case-sensitive name from ft info
                FileTypeInfo fti = this.managedTypes.get(fullFtNameLower);                
                String caseSensitiveName = fti == null ? null : fti.getName();
                
                if (caseSensitiveName != null)
                    ftList.add(caseSensitiveName);
            }
        }       
        
        return ftList;
    }
    
    //---------------------------------------------------------------------
    
    protected boolean isGroupInitialized(String groupName)
    {
        boolean isGroupInit = false;
        
        if (domain.isGroupDefined(groupName))
        {
            isGroupInit = this.initializedGroupSet.contains(groupName.toLowerCase());            
        }
        
        return isGroupInit;
    }
    
    //---------------------------------------------------------------------
    
    protected FileTypeInfo getManagedFiletypeInfo(String groupName, String typeName)
    {
        if (groupName == null || typeName == null)
            return null;
        
        String fullType = FileType.toFullFiletype(groupName, typeName);

        return (FileTypeInfo) this.managedTypes.get(fullType.toLowerCase());
    }
    //---------------------------------------------------------------------
    
    /**
     * Will iterate over the servers associated with group until a
     * successful query is goes through and returns with 
     * a list of filetype names.  
     */
    
    protected boolean fullyInitializeGroup(String groupName) throws SessionException
    {
        boolean success = false;
        List serverNames = domain.getServerNames(groupName);
        
        //for preserving the IOERROR for SesEx's
        int ioErrorCount = 0;
        
        for (int i = 0; !success && i < serverNames.size(); ++i)
        {
            String serverName = (String) serverNames.get(i);
            ServerInfo serverInfo = domain.getServerInfo(groupName, serverName);
            
            if (serverInfo != null)
            {
                List types = null;
                
                try {
                    
                    types = this.typeQueryClient.getFileTypes(serverInfo);
                    
                } catch (SessionException sesEx) {
                    
                    if (sesEx.getErrno() == Constants.IO_ERROR)
                        ioErrorCount++;
                    
                    String target = serverName + " (" + groupName + ") ";
                    final String errMsg = "Received error while attempting " +
                    		              "to get filetype info from " +
                    		              target+":  " + sesEx.getMessage(); 
                    _logger.debug(errMsg);                    
                }

                if (types != null && !types.isEmpty())
                {                   
                    Iterator it = types.iterator();                    
                    while (it.hasNext())
                    {
                        String curType = (String) it.next();
                        addType(groupName, curType);
                    }
                    success = true;
                }                
            }
        } 
        
        if (success)
        {
            this.initializedGroupSet.add(groupName.toLowerCase());
        }
        else if (ioErrorCount > 0)
        {
            if (ioErrorCount  == serverNames.size())
            {
                this._logger.debug("Attempts to contact all servers for group '"+
                                   groupName+"' failed with IO error, suggesting " +
                                   "potential network issue.");
                
                throw new SessionException("Could not reach any of the "+
                          ioErrorCount+" server(s) for group '"+groupName+"'.", 
                          Constants.CONN_FAILED);
            }
        }
        
        return success;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates new FileTypeInfo instance, associates all the group
     * servers to it, and add it to the managed set.
     */
    
    protected void addType(String groupName, String typeName)
    {
        //construct full filetype name
        String fullTypeName = FileType.toFullFiletype(groupName, typeName);
 
        //check if filetype already being managed
        if (this.managedTypes.containsKey(fullTypeName.toLowerCase()))
            return;
        
        //create new filetype info
        FileTypeInfo ftInfo = new FileTypeInfo(groupName, typeName);
        
        //get the server names associates with this group
        List serverNames = this.domain.getServerNames(groupName);
        
        //associate all the servers of the group to that filetype
        String serverName = null;
        ServerInfo serverInfo = null;        
        Iterator it = serverNames.iterator();
        while (it.hasNext())
        {
            serverName = (String) it.next();
            
            try {
                serverInfo = domain.getServerInfo(groupName, serverName);
            } catch (SessionException sesEx) {
                _logger.trace(sesEx.getMessage(), sesEx);
                serverInfo = null;
            }
            
            if (serverInfo != null)
                ftInfo.setServerInfo(serverName, serverInfo);            
        }
        this.managedTypes.put(fullTypeName.toLowerCase(), ftInfo);                         
    }
       
    //---------------------------------------------------------------------
}
