/*
 * Created on Oct 11, 2005
 */
/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

import java.util.Iterator;
import java.util.List;

import jpl.mipl.mdms.FileService.komodo.util.Closable;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose: </b>
 * This class defines all operations that can be performed on a server group.
 * 
 * <PRE>
 *   Copyright 2005, California Institute of Technology. 
 *   ALL RIGHTS RESERVED. U.S.
 *   Government Sponsorship acknowledge. 2005.
 * </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History : </B> ----------------------
 * 
 * <B>Date             Who         What </B>
 * ----------------------------------------------------------------------------
 * 10/11/2005       Nick        Initial release.
 * ============================================================================
 * </PRE>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ServerGroup.java,v 1.3 2013/03/30 00:06:20 ntt Exp $
 *  
 */

public class ServerGroup implements Closable
{
    private String _serverGroup;
    private ServerInfo _serverInfo = null;
    private ServerProxy _proxy     = null;
    private Session _session       = null;
    private ClientRestartCache _restartCache = null;
    private final Logger _logger = Logger.getLogger(ServerGroup.class.getName());

    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * 
     * @param session a transfer session, for maintining file types and
     *        connections.
     * @param sInfos List of server info instances
     * @throws SessionException when session failure.
     *         If unable to connect to server, errno = CONN_FAILED.  If 
     *         authentication error, errno = INVALID_LOGIN.
     */
    
    public ServerGroup(Session session, List sInfos) throws SessionException
    {

        if (sInfos == null || sInfos.isEmpty())
            throw new SessionException("", Constants.MISSINGARG);
               
        this._session     = session;
        Iterator it = sInfos.iterator();
        
        while (this._proxy == null && it.hasNext())
        {
            this._serverInfo = (ServerInfo) it.next();
            this._serverGroup = this._serverInfo.getGroupName();
            String serverName = this._serverInfo.getName();
            
            try {
    
                // If there is no server proxy for this file type, create one.
                this._proxy = this._serverInfo.getProxy();
                if (this._proxy == null) 
                {
                    this._logger.trace("No proxy, so make one.");
                    // Blocks.
                    ServerProxy sp = new ServerProxy(session, this._serverInfo, 
                                                     false,   false);
                    this._serverInfo.setProxy(sp);
                    this._proxy = this._serverInfo.getProxy();
                } else {
                    // Increment the reference count on the server proxy. 
                    // Note: the server proxy reference count is initialized 
                    // to 1 in its constructor.
                    this._proxy.incrementRefCount();
                }
              
            } catch (SessionException sesEx) {
                //only retry if error was conn failed, else pass ses ex up
                if (sesEx.getErrno() == Constants.CONN_FAILED)
                {
                    this._logger.trace("Connection attempt failed: " 
                                       + serverName, sesEx);                    
                }
                else
                    throw sesEx;
            }
        } //end_while

        //check that proxy exists. if not, throw exception
        if (this._proxy != null)
        {
            String serverName = this._proxy.getServerInfo().getName();
            this._logger.trace("Connected to " + serverName + " .");
        }
        else
        {
            throw new SessionException("Unable to connect to servergroup '" 
                                       + this._serverGroup + "' server(s).", 
                                       Constants.CONN_FAILED);
        } 
      
        /**
         * String restartFileName = this._session.getRegistory() + File.separator +
         * this._fileType + Constants.RESTARTEXTENSION; try {
         * RestartExceptionListener listener = new RestartExceptionListener();
         * XMLDecoder decoder = new XMLDecoder(new BufferedInputStream( new
         * FileInputStream(restartFileName)), this, listener);
         * 
         * this._restartInfo = (RestartInfo) decoder.readObject();
         * decoder.close();
         * 
         * if (listener.isCaught()) { this._restartInfo = new
         * RestartInfo(this._fileType, restartFileName); } } catch (Exception e) {
         * this._restartInfo = new RestartInfo(this._fileType, restartFileName); }
         * 
         * this._proxy.setRestartInfo(this._restartInfo);
         */

        // If there are no outstanding transactions, set this file type on the
        // server side. This will verify that the file type really does exist
        // on the server before returning the file type instance. If there are
        // transactions, then the server proxy servicing multi-plexed file types
        // is busy, so don't set the file type to perform early file type
        // checking, just allow the first command on that file type to fail.
        /*
        if (this._session.getTransactionCount() == 0) 
        {
            Request cmd = new Request(this._serverGroup, null,
                                      Constants.CHANGETYPE, 
                                      Constants.NOMODIFIER);
            this._proxy.put(cmd);
            Result result = this._session.result();
            if (result.getErrno() != Constants.OK) 
            {
                // Close this file type, and throw an exception.
                 this.close();
                 this._session.result(); // Throw away result of close.
                 throw new SessionException(result.getMessage(), result.getErrno());
            }
            
        }
        */
    }
   
    //---------------------------------------------------------------------
    
    /**
     * Accessor method to get the name of this file type.
     * 
     * @return file type name
     */
    
    public final String getName() 
    {
        return this._serverGroup;
    }

    //---------------------------------------------------------------------
    
    /**
     * Method to authenticate user/password for a specific filetype
     * or some existing filetype without diretly accessing filetypes.
     * @param user Username
     * @param password User password
     * @param filetype Name of filetype in server group for which 
     *        authentication is being performed.  If null, then general
     *        authentication (can user access at least one filetype?)
     *        is performed.
     * @return the transaction id for tracking this command.
     */
    
    public int authenticateUser(String user, String password, String filetype) 
    {

        //Request cmd = new Request(this._serverGroup, filetype, 
        //                         Constants.AUTHSERVERGROUPUSER,
        //                          Constants.NOMODIFIER);
        String[] requestArgs = (filetype == null) ?
                  new String[] {user, password, this._serverGroup} :
                  new String[] {user, password, this._serverGroup,
                                filetype};                
        
        Request cmd = new Request(Constants.AUTHSERVERGROUPUSER,
                                  requestArgs); 
        this._logger.trace("Queing requested command " + cmd.getCommand());
        return (this._proxy.put(cmd));
    }

    //---------------------------------------------------------------------
    
    /**
     * Method to authenticate user/password for any filetype part of
     * the server group.  Uses username and password from session
     * object.
     * @return the transaction id for tracking this command.
     */
    
    public int authenticateUser()
    {
        return authenticateUser(null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Method to authenticate user/password for a specific filetype as 
     * part of the server group.  Uses username and password from 
     * session object.
     * @param filetype Name of filetype in server group for which 
     *        authentication is being performed.  If null, then general
     *        authentication ("Can user access at least one filetype?")
     *        is performed.
     * @return the transaction id for tracking this command.
     */
    
    public int authenticateUser(String filetype)
    {
        return authenticateUser(this._session.getUserName(), 
                                this._session.getPassword(),
                                filetype);
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Method to generate a user token for a user/password.
     * @param user Username
     * @param password User password
     * @return the transaction id for tracking this command.
     */
    
    public int generateUserToken(String user, String password) 
    {

        //Request cmd = new Request(this._serverGroup, filetype, 
        //                         Constants.AUTHSERVERGROUPUSER,
        //                          Constants.NOMODIFIER);
        String[] requestArgs = new String[] {user, password, this._serverGroup};
        
        Request cmd = new Request(Constants.GETAUTHTOKEN,
                                  requestArgs); 
        
        this._logger.trace("Queing requested command " + cmd.getCommand());
        
        int xactId = this._proxy.put(cmd);
        
        return xactId;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Method to generate a user token for a user/password.
     * @param user Username
     * @param password User password
     * @return the transaction id for tracking this command.
     */
    
    public int queryAuthenticationType() 
    {

        //Request cmd = new Request(this._serverGroup, filetype, 
        //                         Constants.AUTHSERVERGROUPUSER,
        //                          Constants.NOMODIFIER);
        String[] requestArgs = new String[] {this._serverGroup};
        
        Request cmd = new Request(Constants.GETAUTHTYPE,
                                  requestArgs); 
        
        this._logger.trace("Queing requested command " + cmd.getCommand());
        
        int xactId = this._proxy.put(cmd);
        
        return xactId;
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Method to close this file type. Do this by appending the close command at
     * the head of the requests queue. The ServerProxy will then remove any
     * requests for the file type from the queue. If all file types for this
     * server have been closed, then the connection to the server will be
     * gracefully closed.
     * 
     * @return the transaction id for tracking this command.
     */
    
    public int close() 
    {
        //default command is quit
        Request cmd = new Request(this._serverGroup, (String) null); 
        this._logger.trace("Queing requested command " + cmd.getCommand());
        return (this._proxy.putExpedited(cmd));
    }
   
    //---------------------------------------------------------------------
   
    //---------------------------------------------------------------------
}