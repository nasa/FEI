/*
 * Created on Oct 12, 2005
 */
package jpl.mipl.mdms.FileService.komodo.util;

import java.net.URL;
import java.util.Date;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.ServerGroup;
import jpl.mipl.mdms.FileService.komodo.api.Session;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Utility class that performs user authentication for a filetype or, 
 * more generally, a server group.  Prior to this class, the only way
 * to verify a user has access was to perform a connection to a filetype.
 * This utility offers to ability to confirm authentication prior to
 * that step.
 * 
 *   <PRE>
 *   Copyright 2005, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2005.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 10/12/2005        Nick             Initial Release
 * 10/19/2005        Nick             Added unknwnCmdOp field to handle older
 *                                    servers oblivious to  authentication API 
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: UserAuthenticator.java,v 1.8 2013/03/30 00:06:21 ntt Exp $
 *
 */

public class UserAuthenticator
{
    /**
     * Results with errno representing unknown/unrecognized commands
     * will be treated as if the authentication succeeded.  This
     * is used when communicating with older server that doesn't know
     * about pre-authentication.  In which case, the convention was to
     * allow user to proceed and rely on any errors to occur later.
     */
    
    public static final int UNKNOWN_COMMAND_WILL_PASS = 0;
    
    /**
     * Results with errno representing unknown/unrecognized commands
     * will be treated as if the authentication failed.
     */
    public static final int UNKNOWN_COMMAND_WILL_FAIL = 1;
    
    //---------------------------------------------------------------------
    
    protected URL _domainFile;
    protected int    _security;
    protected String _username, _password;
    protected int _unknownCmdOperation;
    private final Logger _logger = Logger.getLogger(UserAuthenticator.class.getName());
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor. Assumes SSL security option and always pass for
     * unknown command replies from servere.
     * @param domainFile Path of FEI domain file
     */
    
    public UserAuthenticator(String domainFile) throws SessionException
    {
        this(domainFile, Constants.SSL);       
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor. Assumes always pass for unknown command replies from 
     * server.
     * @param domainFile Path of FEI domain file
     * @param security Security option for connection
     */
    
    public UserAuthenticator(String domainFile, int security) throws SessionException
    {
        this(ConfigFileURLResolver.resolve(domainFile), security);       
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor. Assumes always pass for unknown command replies from 
     * server.
     * @param domainFile Path of FEI domain file
     * @param security Security option for connection
     */
    
    public UserAuthenticator(URL domainFile) throws SessionException
    {
        this(domainFile, Constants.SSL);        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor. Assumes always pass for unknown command replies from 
     * server.
     * @param domainFile Path of FEI domain file
     * @param security Security option for connection
     */
    
    public UserAuthenticator(URL domainFile, int security) throws SessionException
    {
        this._domainFile = domainFile;    
        this._security   = security;
        this._unknownCmdOperation = UNKNOWN_COMMAND_WILL_PASS;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Perform an authentication procedure for a (user,pwd) and a
     * servergroup.  If no filetype is included, then method will
     * return true if there exists a filetype the user can access.
     * If filetype was included, then method returns true iff user
     * can access the specific filetype.
     * @param user User name, cannot be null.
     * @param pwd User password, cannot be null.
     * @param groupName Name of server group, cannot be null.
     * @param ftName Name of filetype, can be null.
     * @throws SessionException if session error occurs during 
     *         authentication. 
     */
    
    public boolean authenticate(String user, String pwd, String groupName, 
                                String ftName) throws SessionException
    {
        boolean userOk = false;
        Session session = null;
        
        //TODO - decide if null args, return false or throw exception?
        
        if (user == null || pwd == null || groupName == null)
            return false;
        
        if (user == null)
            throw new SessionException("Cannot authenticate with null " +
                                       "user field", 
                                       Constants.INVALID_LOGIN);
        if (pwd == null)
            throw new SessionException("Cannot authenticate with null " +
                                       "password field", 
                                       Constants.INVALID_LOGIN);        
        try {
            session = new Session(_domainFile, _security);        
            String msg = (ftName == null) ? " server group '"+groupName+"'" :
                         " filetype '" + 
                            FileType.toFullFiletype(groupName, ftName) + "'";
            
            ServerGroup sg = session.openServerGroup(groupName);
            int transId = sg.authenticateUser(user, pwd, ftName);
            
            while (session.getTransactionCount() > 0) 
            {
                Result r = session.result();
                if (r == null) 
                {
                   continue;
                }
                else if (r.getErrno() == Constants.OK)
                {
                    userOk = true;
                    msg = "Authenticated user '" + user + "' for " + msg;
                    this._logger.trace(msg);
                }
                else if (r.getErrno() == Constants.UNKNOWNCMD &&
                        this._unknownCmdOperation == UNKNOWN_COMMAND_WILL_PASS)
                {                    
                    //this is the case of an older server without
                    //authentication API.  We will default to true
                    //and rely on FileType creation to test access.
                    this._logger.debug("Server does not recognize " +
                                       "authentication API. Will grant "+
                                       "access.");                        
                    userOk = true;
                    msg = "Authenticated user '" + user + "' for " + msg;
                    this._logger.trace(msg);
                }  
                else
                {
                    userOk = false;
                    msg = "Could not authenticate user '" + user + "' for " + msg;
                    this._logger.trace(msg);
                }
            }
        } catch (SessionException sesEx) {
            if (session != null)
                session.closeImmediate();
            this._logger.error("Session error occurred during "+
                               "authentication: " + sesEx.getMessage());
            this._logger.debug(null, sesEx);
            throw sesEx;
        } finally {
            //close the session, we're not using it anymore...
            if (session != null)
                session.closeImmediate();            
        }
                
        return userOk;
    }
    
    
   //---------------------------------------------------------------------
    
    /**
     * Perform an authentication procedure for a (user,pwd) and a
     * servergroup.  Successful authentication will result in a token
     * that will be used as the passcode for FEI operations.
     * @param user User name, cannot be null.
     * @param pwd User password, cannot be null.
     * @param groupName Name of server group, cannot be null.
     * @throws SessionException if session error occurs during 
     *         authentication. 
     */
    
    public UserToken authenticate(String user, String pwd, String groupName) 
                                                   throws SessionException
    {
        String userToken = null;
        long tokenExpiration = Constants.NO_EXPIRATION;
        
        Session session = null;
        int transId = -1;
        
        //TODO - decide if null args, return false or throw exception?
        
        if (user == null || pwd == null || groupName == null)
            return null;
        
        if (user == null)
            throw new SessionException("Cannot authenticate with null " +
                                       "user field", 
                                       Constants.INVALID_LOGIN);
        if (pwd == null)
            throw new SessionException("Cannot authenticate with null " +
                                       "password field", 
                                       Constants.INVALID_LOGIN);        
        try {
            session = new Session(_domainFile, _security);                   
            
            ServerGroup sg = session.openServerGroup(groupName);
            
            transId = sg.generateUserToken(user, pwd);
            
            while (session.getTransactionCount() > 0) 
            {
                Result r = session.result();
                if (r == null) 
                {
                   continue;
                }
                else if (r.getErrno() == Constants.OK)
                {
                    String resultString = r.getMessage();
                    
                    String[] results = resultString.split("\\s");
                    
                    //get the actual token
                    if (results.length > 0)
                        userToken = results[0];
                    
                    //get the expiration (milliseconds from epoch)
                    if (results.length > 1)
                    {
                        try {
                            tokenExpiration = Long.parseLong(results[1]);
                            this._logger.trace("expiration: "+new Date(tokenExpiration)); 
                        } catch (NumberFormatException nfEx) {
                            tokenExpiration = Constants.NO_EXPIRATION;
                        }
                        
                    }
                    this._logger.trace("Response: "+resultString);    
                    
                    String msg = "Authenticated user '" + user + "' for " + groupName;
                    
                    this._logger.debug(msg);
                }
                else if (r.getErrno() == Constants.UNKNOWNCMD &&
                        this._unknownCmdOperation == UNKNOWN_COMMAND_WILL_PASS)
                {                    
                    //this is the case of an older server without
                    //authentication API.  We will default to true
                    //and rely on FileType creation to test access.
                    this._logger.debug("Server does not recognize " +
                                       "authentication API. Will grant "+
                                       "access using password as token.");
                    userToken = pwd;
                  
                    String msg = "Authenticated user '" + user + "' for " + groupName;
                    this._logger.trace(msg);
                }  
                else if (r.getErrno() == Constants.DENIED)
                {
                    String msg = "User '"+user+"' was not authenticated for server group " + groupName;
                    this._logger.trace(msg);
                }
                else
                {
                    String msg = "Could not authenticate user '" + user + "' for " + groupName;
                    msg += "\nServer Reply: "+r.getMessage();
                    this._logger.trace(msg);
                }
            }
        } catch (SessionException sesEx) {
            if (session != null)
                session.closeImmediate();
            this._logger.error("Session error occurred during token "+
                               "generation (tansaction #"+transId+") : " + sesEx.getMessage());
            this._logger.debug(null, sesEx);
            throw sesEx;
        } finally {
            //close the session, we're not using it anymore...
            if (session != null)
                session.closeImmediate();            
        }
                
        //create return object
        UserToken returnObj = new UserToken(user, groupName);
        returnObj.setToken(userToken, tokenExpiration);
        
        return returnObj;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Queries server group about its authentication method
     * @param groupName Name of server group, cannot be null.
     * @return AuthenticationType for the group
     * @throws SessionException if session error occurs during 
     *         authentication. 
     */
    
    public AuthenticationType getAuthenticationType(String groupName) 
                                              throws SessionException
    {
        
        Session session = null;
        int transId = -1;
        
        String authCategory = null;
        String authImpl     = null;
        
        if (groupName == null)
            return null;
        
        try {
            session = new Session(_domainFile, _security);                   
            
            ServerGroup sg = session.openServerGroup(groupName);
            
            transId = sg.queryAuthenticationType();
            
            while (session.getTransactionCount() > 0) 
            {
                Result r = session.result();
                if (r == null) 
                {
                   continue;
                }
                else if (r.getErrno() == Constants.OK)
                {
                    //expected format:  authCategory  authImplementation
                    String serverReply = r.getMessage();
                    
                    String[] results = serverReply.split("\\s");
                    
                    //get the actual token
                    if (results.length > 0)
                        authCategory = results[0];
                    if (results.length > 1)
                        authImpl = results[1];
                    
                    this._logger.trace("Response: "+serverReply);    
                    
                }
                else if (r.getErrno() == Constants.UNKNOWNCMD &&
                        this._unknownCmdOperation == UNKNOWN_COMMAND_WILL_PASS)
                {                    
                    //this is the case of an older server without
                    //authentication API.  We will default to true
                    //and rely on FileType creation to test access.
                    this._logger.debug("Server does not recognize " +
                                       "authentication API. Will grant "+
                                       "access using password as token.");
                }  
                else if (r.getErrno() == Constants.DENIED)
                {
                    String msg = "Authentication-method query denied for " + groupName;  
                    this._logger.trace(msg);
                }
                else
                {
                    String msg = "Authentication-method query failed for " + groupName;  
                    this._logger.trace(msg);
                }
            }
        } catch (SessionException sesEx) {
            if (session != null)
                session.closeImmediate();
            this._logger.error("Session error occurred during token "+
                               "generation (tansaction #"+transId+") : " + sesEx.getMessage());
            this._logger.debug(null, sesEx);
            throw sesEx;
        } finally {
            //close the session, we're not using it anymore...
            if (session != null)
                session.closeImmediate();            
        }
                
        //create return object
        AuthenticationType returnObj = new AuthenticationType(groupName);
        
        returnObj.setCategory(authCategory);
        returnObj.setImpl(authImpl);
        
        return returnObj;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the operation option for case where server replies with an
     * 'unknown operation' message, which usually denotes a server running
     * an older version of the FEI protocol.  Options are
     * UNKNOWN_COMMAND_WILL_FAIL/UNKNOWN_COMMAND_WILL_PASS to automatically
     * restrict/grant access, respectively.
     * @param operationId One of UserAuthentication.UNKNOWN_COMMAND_WILL_{FAIL|PASS}.
     */
    
    public void setUnknownCommandOperation(int operationId)
    {
        if ((operationId == UNKNOWN_COMMAND_WILL_FAIL ||
             operationId == UNKNOWN_COMMAND_WILL_PASS) &&
            this._unknownCmdOperation != operationId)
        {
            this._unknownCmdOperation = operationId;
        }
    }

    //---------------------------------------------------------------------
    
    /**
     * Return ths current operation option for situation where server returns
     * with 'unknown operation' message.
     * @return Operation id.
     */
    
    public int getUnknownCommandOperation()
    {
        return this._unknownCmdOperation;        
    }
    
    //---------------------------------------------------------------------
    
}
