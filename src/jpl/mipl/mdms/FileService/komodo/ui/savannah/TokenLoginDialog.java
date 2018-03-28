package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.net.URL;

import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.util.UserAuthenticator;
import jpl.mipl.mdms.FileService.komodo.util.UserToken;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <B>Purpose:<B>
 * Extension of the login dialog with password encryption applied.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: TokenLoginDialog.java,v 1.1 2013/03/30 00:06:21 ntt Exp $
 *
 */
public class TokenLoginDialog extends EncryptedLoginDialog
{
    URL _domainLocation;
    UserAuthenticator _userAuthenticator;
    private Logger logger = Logger.getLogger(TokenLoginDialog.class.getName());
    
    protected String _serverGroup;
    protected String _token;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param title Optional title to be displayed with dialog.
     */
    
    public TokenLoginDialog(String title, URL domainFile, String serverGroup)
    {       
        this(title, null, domainFile, serverGroup);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Constructs a login dialog with username and password
     *  fields.
     *  @param title Optional title to be displayed with dialog.
     *  @param username Initial username to enter in field
     */
    public TokenLoginDialog(String title, String username, URL domainFile,
                            String serverGroup)                            
    {            
        this(title, username, null, domainFile, serverGroup);
    }
    
    //---------------------------------------------------------------------
    
    
    /**
     * Constructs a login dialog with username and password
     * fields.
     * @param title Optional title to be displayed with dialog.
     * @param username Initial username to enter in field
     * @param labelValue Optional message value for panel 
     */
    public TokenLoginDialog(String title, String username, String message,
                            URL domainFile,  String serverGroup)
    {        
        super(title, username, message);       
        
        this._domainLocation = domainFile;
        this._serverGroup    = serverGroup;
        
        try {
            _userAuthenticator = new UserAuthenticator(_domainLocation);
        } catch (SessionException sesEx) {   
            this.logger.error("Could not build instance of UserTokenGenerator: "+sesEx.getMessage());
            this.logger.debug(null, sesEx);
            _userAuthenticator = null;
        }     
        
    }
    
    //---------------------------------------------------------------------
    
    protected void readFields()
    {
        super.readFields();
        setTokenField();
    }

    //---------------------------------------------------------------------
    
    protected void setTokenField()
    {
        if (this._username != null && this._password != null)
        {
            UserToken userToken = generateToken();
            if (userToken != null && userToken.isValid())
                this._token = userToken.getToken();
        }
    }

    //---------------------------------------------------------------------
    
    protected boolean checkFields()
    {
        boolean fieldsOK = true;
    
        fieldsOK = super.checkFields();
        if (fieldsOK)
        {
            if (_token == null || _token.equals(""))
            {
                JOptionPane.showMessageDialog(null, "Unable to "+
                    "generate authentication token for user.", "Error",
                    JOptionPane.ERROR_MESSAGE);
                fieldsOK = false;
            }
        }
        
        return fieldsOK;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Override of password field read, where text is encrypted,
     * then assigned to the password member.
     */
    
//    protected void readPasswordField()
//    {
//        //ensure username is read in
//        this.readUsernameField();
//        
//        String passText = new String(this._passField.getPassword());
//        String encrText = null;
//        this._token = null;
//        
//        if (passText != null && !passText.equals("") && _encrypter != null)
//        {
//            try {
//                encrText = this._encrypter.encrypt(passText);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                encrText = null;
//            }
//            
//            if (encrText != null)
//            {                
//                this._token = generateToken(encrText);                
//            }
//        }
//    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the encrypted password from dialog.
     * @return User password, ecrypted using public key
     */
    
    protected UserToken generateToken()
    {
        
        UserToken token = null;
        try {
            token = this._userAuthenticator.authenticate(_username, 
                                                         _password,
                                                         _serverGroup);
        } catch (SessionException sesEx) {
            token = null;
        }
        return token;
    }
    
    //---------------------------------------------------------------------
    
   
    public String getToken()
    {
        return this._token;
    }
    
    //---------------------------------------------------------------------
    
}
