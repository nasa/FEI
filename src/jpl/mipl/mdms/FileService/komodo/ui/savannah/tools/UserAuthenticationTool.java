package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.Component;
import java.net.URL;

import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.EncryptedLoginDialog;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.LoginDialog;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.SubscriptionConstants;
import jpl.mipl.mdms.FileService.komodo.util.AuthenticationType;
import jpl.mipl.mdms.FileService.komodo.util.PublicKeyEncrypter;
import jpl.mipl.mdms.FileService.komodo.util.UserAuthenticator;
import jpl.mipl.mdms.FileService.komodo.util.UserToken;
import jpl.mipl.mdms.FileService.util.PasswordUtil;

/**
 * <B>Purpose:<B>
 * This tool authenticates user credentials with the intended
 * filetype.  If unsuccessful, the tool queries the user for
 * updated credentials.  This loop can be set for a number
 * of iterations before it returns indicating no success.
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: UserAuthenticationTool.java,v 1.5 2016/05/24 22:22:05 ntt Exp $
 *
 */
public class UserAuthenticationTool
{
    public static final String DEFAULT_TITLE       = "Login";
    public static final int    DEFAULT_MAX_ATTEMPT = 3;
    
    Component parent;
    SavannahModel appModel;
    
    PublicKeyEncrypter encrypter;    
    UserAuthenticator userAuthenticator;
    
   //---------------------------------------------------------------------
    
    public UserAuthenticationTool(SavannahModel appModel,
                                  Component parent) 
                                  throws SessionException
    {
        this.appModel = appModel;
        this.parent   = parent;
        
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init() throws SessionException
    {
        URL domainFile = this.appModel.getDomainFile();
        
        if (domainFile == null)
        {
            throw new SessionException("Token generator requires a domain file.",
                                       Constants.DOMAINIOERR);
        }
        
        userAuthenticator = new UserAuthenticator(domainFile);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Attempts to authenticate user for a specific filetype.  Will attempt
     * at most maxAttempts times before aborting.  
     * @param title Title to be displayed for input panels
     * @param username Initial username  Can be null.
     * @param password Initial password. Can be null.
     * @param fullfiletype Full filetype to which user is attempting to 
     *        connect.
     * @param maxAttempts Maximum number of attempts to authenticate before
     *        aborting.
     * @return String[] of length two where first entry is username,
     *        and second entry is password if successful.  Null otherwise.
     */
    
    public String[] authenticateUser(String title, 
                                     String username,
                                     String password,
                                     String fullfiletype,
                                     int maxAttempts)
                                     throws SessionException
    {
        String[] info = null;
        boolean success = false;
        int attemptCount = 0;
        String servergroup = FileType.extractServerGroup(fullfiletype);
        String filetype = FileType.extractFiletype(fullfiletype);
                
        //do we already have username/pwd?  If not, get it
        if (username == null || password == null)
        {            
            info = getLoginInfo(title,       username,
                                servergroup, fullfiletype); 
            
            if (info == null)
                return null;
            
            username = info[0];
            password = info[1];
        }
        else
        {
            info = new String[] {username, password};
        }
        
        while (info != null && !success && attemptCount < maxAttempts)
        {
            if (appModel.canUserConnect(info[0],     info[1],
                                        servergroup, filetype))
            {
                success = true;
            }
            else
            {
                ++attemptCount;
                if (attemptCount < SubscriptionConstants.MAX_LOGIN_ATTEMPT_COUNT)
                {
                    JOptionPane.showMessageDialog(this.parent,
                        "Invalid login for user '"+username+"' to '"+fullfiletype
                        +"'.\nPlease re-enter "+
                        "username and password in login window.",
                        "Login Error", JOptionPane.ERROR_MESSAGE);
                    info = getLoginInfo(title, username, 
                             servergroup, fullfiletype);
                }
                else
                {
                    JOptionPane.showMessageDialog(this.parent,
                            "Invalid login.  Max attempt count ("+
                            SubscriptionConstants.MAX_LOGIN_ATTEMPT_COUNT+
                            ") reached!", "Login Error", 
                            JOptionPane.ERROR_MESSAGE); 
                }
            }            
        }
  
        if (!success)
        {            
            info = null;
        }
        
        return info;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Overloaded version that uses a default title and number
     * of iterations.
     */
    
    public String[] authenticateUser(String username,
                                     String password,
                                     String fullfiletype)
                                     throws SessionException
    {
        return authenticateUser(DEFAULT_TITLE, username, password,
                                fullfiletype, DEFAULT_MAX_ATTEMPT);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get username and password from user via dialog.
     */
    
    protected String[] getLoginInfo(String title, String username,
                                    String servergroup, String message)
                                    throws SessionException
    {
        String[] info = null;
        
        LoginDialog ld = new EncryptedLoginDialog(title, username, message);
        
        //-------------------------
        
        //does server group want a password or passcode?        
        try {
            AuthenticationType authType = 
                        userAuthenticator.getAuthenticationType(servergroup);
            String passPrmpt = PasswordUtil.getPrompt(authType);
            ld.setPasswordPrompt(passPrmpt);
        } catch (SessionException sesEx) {
            if (sesEx.getErrno() == Constants.CONN_FAILED)
            {
                throw sesEx;
            }
        }
        
        //-------------------------
//        TokenLoginDialog ld = new TokenLoginDialog(title, username, message,
//                                 appModel.getDomainFile(), servergroup);
        
        int reply = ld.showDialog(this.parent);
        if (reply == JOptionPane.OK_OPTION)
        {            
            String theUsername = ld.getUsername();
            String thePassword = ld.getPassword();
            
            UserToken theToken = getAuthenticationToken(theUsername, 
                                          thePassword, servergroup);
            
            if (theToken != null && theToken.isValid())
            {             
                info = new String[2];
                info[0] = theUsername;
                info[1] = theToken.getToken();
            }
        }
        
        return info;
    }

    //---------------------------------------------------------------------
    
    
    
    //---------------------------------------------------------------------

    /**
     * Returns authentication token for login credentials.  If the
     * authentication cannot be performed (i.e. invalid login,
     * server group not defined) then null will be returned.
     * @param username Username
     * @param password Password
     * @param serverGroup Server group
     * @returns UserToken or null
     */
    
    protected UserToken getAuthenticationToken(String username, String password,
                                               String serverGroup)
    {
        UserToken token = null;
        try {

            token = userAuthenticator.authenticate(username, 
                                      password, serverGroup);
                
        } catch (SessionException sesEx) {
            JOptionPane.showMessageDialog(this.parent,
                    "Error occured while attempting to authenticate '"+
                    username+"' for server group '"+serverGroup+"'",
                    "Login Error", 
                    JOptionPane.ERROR_MESSAGE); 
            token = null;
        }
        return token;
    }
    
    //---------------------------------------------------------------------
    
}
