package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.awt.Component;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaParameters;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaSubscription;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaSubscriptionFactory;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaSubscriptionManager;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.NotificationParameters;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.SubscriptionConstants;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.SubscriptionParameters;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.UserAuthenticationTool;
import jpl.mipl.mdms.utils.logging.Logger;

public class PersistedSessionsChecker implements Runnable
{
    SavannahModel appModel;
    MetaSubscriptionManager manager; 
    Component parent;
    SessionCacheRegistry registry;
    
    private Logger _logger = Logger.getLogger(
                            PersistedSessionsChecker.class.getName());
    
    
    //---------------------------------------------------------------------
    
    public PersistedSessionsChecker(SavannahModel appModel, 
                                    Component parent)
    {
        this.appModel = appModel;
        this.manager  = appModel.getSubscriptionManager();
        this.parent  = parent;
        
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        registry = this.manager.getSessionCacheRegistry();
    }
    
    //---------------------------------------------------------------------
    
    public void run()
    {
        List<String> ids = registry.getSessionIds();
        
        //if no ids, then there is nothing to do
        if (ids == null || ids.isEmpty())
            return;
        
        //pop up dialog asking user how to proceed
        int size = ids.size();        
        String message = size+" persisted sessions found. Do you wish \n"+
                         "to restart all; skip restart;\n"+
                         "or clear all persisted entries?";
        String[] options  = new String[] {"Restart", 
                                          "Skip", 
                                          "Clear"};
        int reply = JOptionPane.showOptionDialog(this.parent, message, 
                                     "Restart sessions", 
                                     JOptionPane.YES_NO_CANCEL_OPTION, 
                                     JOptionPane.INFORMATION_MESSAGE, null, 
                                     options, options[0]);
        
        if (reply == JOptionPane.YES_OPTION)
        {
            reactivatePersistedSessions();
        }
        else if (reply == JOptionPane.NO_OPTION)
        {
            return;
        }
        else if (reply == JOptionPane.CANCEL_OPTION)
        {
            clearPersistedSessions();
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Iterates over the cached session options from the registry
     * and resumes them.
     * An internal re-authentication step takes place, then ensures
     * that user/password can still access the type.  If not,
     * then new credentials are requested of the user.  If these
     * are successful, the session is resumed.  Otherwise, the user
     * is given the option to skip resuming until the next application
     * lifecycle, or to remove it completely.
     */
    
    protected void reactivatePersistedSessions()
    {
        List<String> ids = registry.getSessionIds();
        
        if (ids == null || ids.isEmpty())
            return;
        
        int size = ids.size();        
        
        Iterator<String> it = ids.iterator();
        while (it.hasNext())
        {
            String id = it.next();
            
            //get the string map from the registry
            Properties persistedOptions = this.registry.getSessionSettings(id);
                        
            //---------------------
            
            //Ensure that user auth info still applies
            boolean success = false;
            String errMesg  = null;
            try {
                success = reauthenticateSession(id, persistedOptions);
            } catch (SessionException sesEx) {
                this._logger.error("Error occurred while reactivating " +
                        "session: "+sesEx.getMessage()+" ("+
                        sesEx.getErrno()+")");
                this._logger.trace(null, sesEx);
                success = false;
                
                if (sesEx.getErrno() == Constants.CONN_FAILED)
                {                   
                    errMesg = "Error message: Please check network status and FEI domain file configuration.";
                }
                else
                {
                    errMesg = "Error message: "+sesEx.getMessage();
                }
            }
            
            if (!success)
            {
                //give user option to skip or remove persisted session
                handleNonSuccessfulAuthentication(id, persistedOptions, errMesg);
                continue;
            }
            
            //---------------------
                       
            //convert string values to correct instance valus
            Map objectOptions = MetaParametersParseUtil.fromStrings(
                                                      persistedOptions);
            
            //determine the task type, and create the appropriate task 
            //params object
            MetaParameters mp;
            Integer taskType = (Integer) objectOptions.get(
                                          SubscriptionConstants.KEY_TASK_TYPE);
            if (taskType == null)
                continue;
                
            if (taskType.intValue() == SubscriptionConstants.TASK_NOTIFICATION)
            {
                mp = new NotificationParameters();
                mp.setAll(objectOptions);
                
            }
            else if (taskType.intValue() == SubscriptionConstants.TASK_SUBSCRIPTION)
            {
                mp = new SubscriptionParameters();
                mp.setAll(objectOptions);
            }
            else
            {
                //we dont handle this case now, so skip it
                continue;
            }
            
            //---------------------
            
            {             
                //create a new subscription session from the parameters
                MetaSubscription ms;
                try {
                    ms = MetaSubscriptionFactory.createInstance(mp);                
                } catch (SessionException sesEx) {
                    this._logger.error("Error occurred while reactivating " +
                    		"session: "+sesEx.getMessage());
                    this._logger.trace(null, sesEx);
                    ms  = null;
                }      
                
                if (ms != null)
                {
                    this.manager.addMetaSubscription(ms);
                    
                    //start the session
                    if (true)
                    {
                        final MetaSubscription fMetaSub = ms;
                        Thread msThread = new Thread(ms);
                        msThread.setName("Thread_"+ms.getName());
                        int lowPriority = (Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2;
                        msThread.setPriority(lowPriority);
                        msThread.start();
                    }
                }     
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void clearPersistedSessions()
    {
        //clear all and continue
        this.registry.removeAllSessionSettings();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Ensures that the login settings still work since the last 
     * invocation.  If they do not, then the user is queried for
     * updated credentials.  This exchange will occur a max 
     * number of times.  If successful, the updated login
     * info is stored in the persistedOptions parameter and
     * the registry is updated and committed.
     * @param entryId Session id as retrieved from the registry
     * @param persistedOptions Options to be tested for authentication,
     * this parameter may be updated by this method if new credentials
     * are required.
     * @return True if success authentication, despite repeated attempts.
     * False otherwise. 
     */
    
    protected boolean reauthenticateSession(String entryId,
                                            Properties persistedOptions) 
                                            throws SessionException
    {
        
        String username = persistedOptions.getProperty(SubscriptionConstants.KEY_USERNAME);
        String password = persistedOptions.getProperty(SubscriptionConstants.KEY_PASSWORD);        
        String fullType = persistedOptions.getProperty(SubscriptionConstants.KEY_FILETYPE);
        String svrgroup = FileType.extractServerGroup(fullType);
        String filetype = FileType.extractFiletype(fullType);
        
        boolean success    = false;
        boolean canConnect = false;
        
        canConnect = this.appModel.canUserConnect(username, password, svrgroup, filetype);
        
        if (canConnect)
        {
            success = true;
        }
        else
        {
            UserAuthenticationTool authUtil = new UserAuthenticationTool(
                                               this.appModel,this.parent);
            String[] loginInfo = authUtil.authenticateUser("Filetype Login", 
                                          username, password,
                                          fullType, 
                                          UserAuthenticationTool.
                                          DEFAULT_MAX_ATTEMPT);
            
            //we ended up with legit login info, save to the options and persist
            if (loginInfo != null && loginInfo[0] != null && loginInfo[1] != null)
            {
                persistedOptions.setProperty(SubscriptionConstants.KEY_USERNAME, 
                                             loginInfo[0]);
                persistedOptions.setProperty(SubscriptionConstants.KEY_PASSWORD,
                                            loginInfo[1]);
                
                this.registry.addSessionSettings(entryId, persistedOptions);   
                try {
                    this.registry.commit();
                    this._logger.trace("Persisted subscription session: "+entryId);
                    success = true;
                } catch (SessionException sesEx) {
                    this._logger.error("Error occurred while persisting new " +
                                       "subscription session "+entryId+". Message: "+  
                                       sesEx.getMessage());
                    this._logger.trace(null, sesEx);
                }
            }     
        }
        
        return success;
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Offers user the option to skip a session that cannot be resumed
     * for a later application invocation or to remove it from the 
     * session registry cache so that it will never be run again.
     * @param id Entry id of registry
     * @param persistedOptions Options properties
     */
    
    protected void handleNonSuccessfulAuthentication(String id, 
                                                     Properties persistedOptions,
                                                     String errorMessage)
    {
        String fullFiletype = FileType.toFullFiletype(
                persistedOptions.getProperty(SubscriptionConstants.KEY_SERVER_GROUP),
                persistedOptions.getProperty(SubscriptionConstants.KEY_FILETYPE));
        
        this._logger.error("Could not resume session for filetype "+
                           fullFiletype+" (id = "+id+").");
                        
        
        if (errorMessage == null)
            errorMessage = "";
        else
            errorMessage = "\n" + errorMessage + "\n\n";
        
        String message = "Could not resume persisted session for '" +
                          fullFiletype+"'. \n"+errorMessage+
                          "Do you wish "+
                         "to skip session or remove it completely?";
        
        
        String[] options  = new String[] {"Skip", 
                                          "Remove"};
        int reply = JOptionPane.showOptionDialog(this.parent, message, 
                        "Restart sessions", 
                        JOptionPane.YES_NO_OPTION, 
                        JOptionPane.INFORMATION_MESSAGE, null, 
                        options, options[0]);

        if (reply == JOptionPane.YES_OPTION) //SKIP
        {
            //do nothing
            this._logger.trace("Skipping subscription session: "+id);
        }
        else if (reply == JOptionPane.NO_OPTION) //REMOVE
        {
            //remove it from cache            
            this.registry.removeSessionSettings(id);   
            try {
                this.registry.commit();
                this._logger.trace("Removed subscription session: "+id);
            } catch (SessionException sesEx) {
                this._logger.error("Error occurred while removing new " +
                                   "subscription session "+id+". Message: "+  
                                   sesEx.getMessage());
                this._logger.trace(null, sesEx);
            }
        }       
    }
    
    //---------------------------------------------------------------------
}
