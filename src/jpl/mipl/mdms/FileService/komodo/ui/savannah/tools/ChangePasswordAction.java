package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.util.AuthenticationType;
import jpl.mipl.mdms.FileService.komodo.util.PublicKeyEncrypter;
import jpl.mipl.mdms.FileService.komodo.util.UserAuthenticator;
import jpl.mipl.mdms.FileService.komodo.util.UserToken;
import jpl.mipl.mdms.utils.logging.Logger;

public class ChangePasswordAction extends AbstractAction
{
    SavannahModel model;
    Component parent;
    
    private Logger logger = Logger.getLogger(ChangePasswordAction.class.getName());
    
    static String ACTION_NAME = "Change Password";
    
    private UserAuthenticator authenticator; 
    
    //---------------------------------------------------------------------
    
    public ChangePasswordAction(Component parent, SavannahModel model)
    {
        super(ChangePasswordAction.ACTION_NAME);
        this.parent = parent;
        this.model = model;
        
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        //Create a authentication token generator
        final URL domainFile = model.getDomainFile();
        
        try {
            authenticator = new UserAuthenticator(domainFile);
        } catch (SessionException se) {
           logger.error(se.getMessage());
           logger.debug(null, se);           
        }
    }
    
    //---------------------------------------------------------------------
    
    
    public void run()
    {
        List groupList = model.getAvailableFeiServers();
        String[] groups = new String[groupList.size()];
        for (int i = 0; i < groups.length; ++i)
            groups[i] = (String) groupList.get(i);
        
        String curGroup = model.getCurrentFeiServer();
        
        String user = model.getUsername();
        //String pass = model.getPassword(user);
        
        //ChangePasswordPanel cpPanel = new ChangePasswordPanel(groups, curGroup, user, pass);
        //we're doing encryption, so old password is encrypted
        ChangePasswordPanel cpPanel = new ChangePasswordPanel(groups, curGroup, user, null);
        
        Object[] options = new Object[] {"Apply", "Cancel"};
        int opt = JOptionPane.showOptionDialog(this.parent, cpPanel, 
                                               ChangePasswordAction.ACTION_NAME,
                                               JOptionPane.YES_NO_OPTION, 
                                               JOptionPane.PLAIN_MESSAGE,
                                               null, options, options[0]);
        if (opt == JOptionPane.CLOSED_OPTION || options[opt].equals("Cancel"))
        {
            return;
        }
        
        String username    = cpPanel.getUsername();
        String oldPassword = cpPanel.getOldPassword();
        String newPassword = cpPanel.getNewPassword();
        String servergroup = cpPanel.getGroup();
                
        changePassword(servergroup, username, oldPassword, newPassword);    
    }
    
    //---------------------------------------------------------------------
    
    protected void changePassword(String servergroup, String username,
                                  String oldPassword, String newPassword)
    {
        
        String reason = this.canChangePassword(servergroup); 
        if (reason != null)
        {
            String errMesg = "Authentication credentials cannot  "+
                             "be changed for server group '"+
                             servergroup+"' via FEI5 client.";            
            errMesg +=  "\nPotential reason(s): "+reason;
            errMesg += "\n\nContact server administrator for more details.";
            
            JOptionPane.showMessageDialog(this.parent,
                                          errMesg, 
                                          ChangePasswordAction.ACTION_NAME, 
                                          JOptionPane.ERROR_MESSAGE);     
            
            return;
        }
        
        
        PublicKeyEncrypter encrypter = null;
        String oldEncrypted = null;
        String newEncrypted = null;
        
        try {
            encrypter   = new PublicKeyEncrypter();            
            oldEncrypted = encrypter.encrypt(oldPassword);
            newEncrypted = encrypter.encrypt(newPassword);
        } catch (Exception ex) {
            logger.error("Could not encrypt passwords: "+ex.getMessage());
            logger.debug(null, ex);
            JOptionPane.showMessageDialog(this.parent,
                            "Could not encrypt password.  Aborting.", 
                            ChangePasswordAction.ACTION_NAME, 
                            JOptionPane.ERROR_MESSAGE); 
            return;
        }
        
        
        //-------------
        
        //perform authentication token generation
        UserToken authToken = null;
        try { 
            authToken = this.authenticator.authenticate(
                        username, oldEncrypted, servergroup);
        } catch (Exception ex) {
            logger.error("Unable to generate authentication token");
            logger.debug(null, ex);
            JOptionPane.showMessageDialog(this.parent,
                            "Unable to generate authentication token.  Aborting.", 
                            ChangePasswordAction.ACTION_NAME, 
                            JOptionPane.ERROR_MESSAGE);           
            return;
        }
   
        if (authToken == null || !authToken.isValid())
        {
            String errMesg = "User '"+username+"' was not authenticated for " +
                  "group '"+servergroup+"'";
            logger.error(errMesg);
            JOptionPane.showMessageDialog(this.parent,
                             errMesg, 
                            ChangePasswordAction.ACTION_NAME, 
                            JOptionPane.ERROR_MESSAGE);             
            return;
        }
        
        //------------
        
        
        
        
        this.model.changePassword(servergroup, 
                                  username, 
                                  authToken.getToken(), 
                                  newEncrypted);
        
    }

    //---------------------------------------------------------------------

    public void actionPerformed(ActionEvent arg0)
    {
        run();        
    }
    
    //---------------------------------------------------------------------
    
    protected String canChangePassword(String serverGroup)
    {
        String errMsg = null;
        
        //request the authentication method from the servergroup
        AuthenticationType authType = null;
        try {
            authType = this.authenticator.getAuthenticationType(serverGroup);
        } catch (SessionException sesEx) {       
            
            //if conn failed, provide potentially more useful message
            if (sesEx.getErrno() == Constants.CONN_FAILED)
            {                
                errMsg = "Unable to connect to server group '" +
                         serverGroup+"'.\nPlease check network " +
                         "status and FEI domain file configuration.";                      
            }            
        }
        
        if (errMsg == null)
        {
            if (authType == null)
            {
                errMsg = "authentication info was not returned from server";
            }
            else if (!authType.isMaintainedInternally())
            {
                errMsg = "authentication may be tied to institutional service";
            }
        }
        
        return errMsg;
        
        
    }
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
}
