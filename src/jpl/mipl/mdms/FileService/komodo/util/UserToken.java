package jpl.mipl.mdms.FileService.komodo.util;

import jpl.mipl.mdms.FileService.komodo.api.Constants;

/**
 * <B>Purpose:<B>
 * Data structure for user authentication token.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: UserToken.java,v 1.1 2013/03/30 00:06:21 ntt Exp $
 *
 */
public class UserToken
{  
    protected String username;
    protected String servergroup;
    protected String token;
    protected long   expiry;
    
    //---------------------------------------------------------------------
    
    public UserToken(String user, String serverGroup)
    {
        this.username = user;
        this.servergroup = serverGroup;
        this.expiry = Constants.NO_EXPIRATION;
        this.token = null;
    }
    
    //---------------------------------------------------------------------
    
    public void setToken(String token)
    {
        setToken(token, Constants.NO_EXPIRATION);        
        
    }

    //---------------------------------------------------------------------
    
    public void setToken(String token, long expiry)
    {
        this.token = token;
        this.expiry = expiry;
    }

    //---------------------------------------------------------------------    
    
    public String getUsername()
    {
        return this.username;
    }

    //---------------------------------------------------------------------
    
    public String getServerGroup()
    {
        return this.servergroup;
    }

    //---------------------------------------------------------------------
    
    public String getToken()
    {
        return this.token;
    }

    //---------------------------------------------------------------------
    
    public long getExpiry()
    {
        return this.expiry;
    }

    //---------------------------------------------------------------------
    
    public boolean isValid()
    {
        if (username == null || username.isEmpty())
            return false;
        if (servergroup == null || servergroup.isEmpty())
            return false;
        if (token == null)
            return false;
        if (this.expiry != Constants.NO_EXPIRATION && 
            this.expiry < System.currentTimeMillis())
            return false;
        return true;
    }

    //---------------------------------------------------------------------
}
