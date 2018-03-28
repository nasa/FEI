/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * BaseClient-side Komodo internal file type class. <br>
 * <br>
 * 
 * Methods are provided for returning file type and server information.
 * 
 * @author J. Jacobson, R. Pavlovsky
 * @version $Id: FileTypeInfo.java,v 1.5 2005/03/09 00:15:50 ntt Exp $
 */
public class FileTypeInfo {
    private String _groupName, _typeName;

    private Hashtable _servers = new Hashtable();

    private FileType _fileType = null; // Place to remember user's file type.

    /**
     * Constructor, takes one string argument for the name of the file type.
     * 
     * @param name The file type name
     */
    public FileTypeInfo(String groupName, String typeName) {
        this._groupName = groupName;
        this._typeName = typeName;
    }

    /**
     * Method to set the file type
     * 
     * @param ft the file type id received from server.
     */
    final void setFt(FileType ft) {
        this._fileType = ft;
    }

    /**
     * Method to return the file type name
     * 
     * @return the file type name
     */
    final String getName() {
        return this._typeName;
    }

    /**
     * Method to return the server group name
     * 
     * @return the server group name
     */
    final String getGroup() {
        return this._groupName;
    }

    /**
     * Method to return a vector of server names, sorted according
     * to priorities of server info objects.
     * @return Vector of String
     */
    final Vector getServers() {
        
        //sort the server info objects by priority
        Vector v1 = new Vector();
        v1.addAll(this._servers.values());
        int count = v1.size();
        Collections.sort(v1);
        
        //create vector of names in same order
        Vector v2 = new Vector();
        for (int i = 0; i < count; ++i)
        {
            ServerInfo sInfo = (ServerInfo) v1.get(i);
            v2.add(sInfo.getName());
        }

        return v2;
    }

    /**
     * Checks to see if input server parameter is in list of servers for this
     * file type. Returns true if it is in the list, false otherwise.
     * 
     * @param serverName The name of the FEI server
     * @return boolean true if serverName is in list of servers, false otherwise
     */
    final boolean isServedBy(String serverName) {
        Enumeration e = this._servers.keys();
        while (e.hasMoreElements()) {
            if (e.nextElement().toString().equalsIgnoreCase(serverName))
                return true;
        }
        return false;
    }

    /**
     * Method to return the file type object
     * 
     * @return the file type object reference
     */
    final FileType getFt() {
        return this._fileType;
    }

    /**
     * Method to return server info object
     * 
     * @param serverName Logical name of an FEI server
     * @return the server info object reference.
     */
    final ServerInfo getServerInfo(String serverName) {
        return (ServerInfo) this._servers.get(serverName);
    }

    /**
     * Bind the server information (i.e. host and port) to the server name entry
     * in the servers hashtable. The FEI server name is the key to the
     * hashtable.
     * 
     * @param serverName Logical name of an FEI server
     * @param serverInfo Bind server info to a server name
     */
    final void setServerInfo(String serverName, ServerInfo serverInfo) {
        this._servers.put(serverName, serverInfo);
    }

    /**
     * Get a string representation of class
     * 
     * @return String representation of class
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("FILE TYPE - group: " + this._groupName + " name: "
                + this._typeName + "\n");
        buf.append("\t" + this.getServers().toString() + "\n");
        return buf.toString();
    }
}