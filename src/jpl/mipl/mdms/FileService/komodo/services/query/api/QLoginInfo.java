/*
 * Created on Jun 25, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.api;

/**
 * <b>Purpose:</b> 
 * Data structure that maintains session login information, including
 * user info, server group, and operation id.
 * 
 *   <PRE>
 *   Copyright 2007, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2007.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 08/08/2007        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QLoginInfo.java,v 1.3 2009/08/07 01:00:48 ntt Exp $
 *
 */

public class QLoginInfo
{
    protected String username;
    protected String password;
    protected String serverGroup;
    protected String operation;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param username User name
     * @param password User password
     * @param serverGroup Server group name
     * @param operationId Identifer of operation (from Komodo API Constants)
     */
    
    public QLoginInfo(String username, String password,
                      String serverGroup, String operationId)
    {
        this.username = username;
        this.password = password;
        this.serverGroup = serverGroup;
        this.operation = operationId;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the operation id
     * @return the operation
     */
    
    public String getOperation()
    {
        return operation;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the user password
     * @return the password
     */
    
    public String getPassword()
    {
        return password;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the server group
     * @return the serverGroup
     */
    
    public String getServerGroup()
    {
        return serverGroup;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the username
     * @return the username
     */
    
    public String getUsername()
    {
        return username;
    }
    
    //---------------------------------------------------------------------
    
}
