/**
 *  @copyright Copyright 2004, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */
package jpl.mipl.mdms.FileService.komodo.api;

import java.util.LinkedList;

/**
 * Abstract superclass of all Domain parsing classes.
 * 
 * Methods are provided for returning file type and server information.
 * 
 * @author R. Pavlovsky {rich.pavlovsky@jpl.nasa.gov}
 * @version $Id: Domain.java,v 1.30 2010/08/10 15:53:03 ntt Exp $
 */
public abstract class Domain {

    /**
     * Constructor
     */
    public Domain() {
        // No-op
    }

    /**
     * Utility method to output the Domain data
     * 
     * @return String contents of domain data
     */
    public abstract String toString();

    /**
     * Get the default server group specified in domain file. Returns an empty
     * string if default server group not specified in domain file.
     * 
     * @return default server group name
     */
    public abstract String getDefaultGroup();

    /**
     * Get a list of sorted server group names
     * 
     * @return list of server group names
     */
    public abstract LinkedList getGroupNames();
    
    /**
     * Get a list of sorted server names associated with a group
     * 
     * @return list of server names
     */
    public abstract LinkedList getServerNames(String group);

    /**
     * Returns true if servergroup passed in as parameter is defined
     * @param servergroup Name of server group to query, case-insensitive
     * @return True if servergroup exists, false otherwise
     */
    public abstract boolean isGroupDefined(String servergroup);
    
    /**
     * Get a list of sorted file type names, does not respect server group
     * namespace.
     * 
     * @return list of file type names
     * @deprecated doesn't respect server group namespace, use
     *             getFileTypeNames(String groupName) instead
     * @throws SessionException when lookup fails
     */
    public abstract LinkedList getFileTypeNames() throws SessionException;

    /**
     * Get a list of sorted file type names for a specified server group
     * 
     * @param groupName the server group name
     * @return list of file type names
     * @throws SessionException when lookup fails
     */
    public abstract LinkedList getFileTypeNames(String groupName)
            throws SessionException;

    /**
     * Returns the server infomation object for input servername parameter.
     * Returns the first serverName match found (regardless of server groups)!
     * Please note that the same server name can be defined in multiple server
     * groups. Returns the first server defined in domain structure if
     * serverName is null. Throws session exception if a match is not found in
     * domain hash.
     * 
     * @param serverName the server name
     * @return the server info object reference
     * @throws SessionException when lookup fails
     */
    public abstract ServerInfo getServerInfo(String serverName)
            throws SessionException;

    /**
     * Returns the server infomation object for input server name and group name
     * parameters. If servername is null, returns the first server name match
     * found (regardless of server groups)! Please note that the same server
     * name can be defined in multiple server groups. Returns the first
     * servername match (regardless of server groups) if group name is null.
     * Throws session exception if a match is not found in domain hash.
     * 
     * @param serverName the server name
     * @param groupName the server group name
     * @return the server info object reference
     * @throws SessionException when lookup fails
     */
    public abstract ServerInfo getServerInfo(String groupName, String serverName)
            throws SessionException;

    /**
     * Returns the server at the specified index, or null if index is out of
     * bounds.
     * 
     * @param index the index entry to the server list
     * @return the server info object reference
     */
    public abstract ServerInfo getServerInfoByIndex(int index);

    /**
     * Method to return a LinkedList of ServerInfo objects for a given file type
     * name and server group
     * 
     * @param groupName the server group name
     * @param typeName the file type name
     * @return linked list of server info objects
     * @throws SessionException when operation fails
     */
    public abstract LinkedList getServerInfoFromFileType(String groupName,
            String typeName) throws SessionException;

    /**
     * Method to return a LinkedList of ServerInfo objects for a given server
     * group
     * 
     * @param groupName the server group name
     * @return LinkedList of server info objects
     * @throws SessionException when operation fails
     */
    public abstract LinkedList getServerInfoFromGroup(String groupName)
                                                     throws SessionException;
        
    
    /**
     * Returns the file type infomation object for input file type name
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
    public abstract FileTypeInfo getFileType(String typeName)
            throws SessionException;

    /**
     * Returns the file type infomation object for input file type name
     * parameter. If type name is null, returns the first type name found
     * (regardless of server groups)! Please note that the same file type name
     * can be defined in multiple server groups. Returns the first type name
     * match (regardless of server groups) if group name is null. Throws session
     * exception if a match is not found in domain hash.
     * 
     * @param groupName the server group name
     * @param typeName the file type name
     * @return the file type object
     * @throws SessionException if match is not found
     */
    public abstract FileTypeInfo getFileType(String groupName, String typeName)
            throws SessionException;
    
    /**
     * Returns true of the group is tagged as dynamic, meaning the filetypes
     * that comprise the group must be explicitly retrieved from a group
     * server; false otherwise.  The switch for dynamic loading can be 
     * set at either the server group level or the domain level (all server
     * groups).
     * @param groupName Server group name
     * @return True if dynamic loading enable at servergroup or domain level.
     * @throws SessionException If session error occurs
     */
    public abstract boolean isGroupDynamic(String groupName) throws SessionException;;
}