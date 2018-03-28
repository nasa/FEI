package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.Session;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.logging.LogMessagePublisher;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.logging.LogMessagePublisherSingleton;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.logging.SavannahLogModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.DefaultMetaSubscriptionManager;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaSubscriptionManager;
import jpl.mipl.mdms.FileService.komodo.util.AuthenticationType;
import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;
import jpl.mipl.mdms.FileService.komodo.util.UserAuthenticator;
import jpl.mipl.mdms.FileService.komodo.util.UserToken;
import jpl.mipl.mdms.FileService.util.DateTimeFormatter;
import jpl.mipl.mdms.FileService.util.FileUtil;
import jpl.mipl.mdms.FileService.util.PasswordUtil;
import jpl.mipl.mdms.FileService.util.WildcardRegexUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose: </b> 
 * Application model for the Savannah application.
 * 
 * <PRE>
 * Copyright 2004, California Institute of Technology. 
 * ALL RIGHTS RESERVED. 
 * U.S. Government Sponsorship acknowledge. 2004.
 * </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History : </B> 
 * ---------------------- 
 * <B>Date             Who         What </B>
 * ----------------------------------------------------------------------------
 * 06/02/2004       Nick        Initial Release
 * 08/22/2005       Nick        Added shutdown handler to close session and
 *                              terminate subscription manager.
 * ============================================================================
 * </PRE>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahModel.java,v 1.101 2016/10/05 22:08:04 ntt Exp $
 */

public class SavannahModel implements PropertyChangeListener
{


    private final String __classname = "SavannahModel";

    /** application version id */
    public static final String      VERSION_ID          = "1.0.10"; //10/05/16

    /** application title */
    public static final String      APPLICATION_TITLE   = "File Exchange Interface 5";

    /** client version string */
    public static final String      KOMODO_VERSION      = Constants.CLIENTVERSIONSTR;

    /** copyright string */
    public static final String      COPYRIGHT           = 
            "(c) Copyright 2002-2013 California Institute of Technology.\n" +
    		"ALL RIGHTS RESERVED. U.S. Government sponsorship acknowledged.\n" +
    		"Any commercial use must be negotiated with the Office of \n"+
    		"Technology Transfer at the California Institute of Technology.\n\n" +
    		"This software may be subject to U.S. export control laws and \n"+
    		"regulations. " +
    		"By accepting this document, the user agrees to comply \nwith " +
    		"all applicable U.S. Export Laws and regulations.\n\n" +
    		"User has the responsibility to obtain export licenses \n" +
    		"or other export authority as may be required before exporting \n"+
    		"such information to foreign countries or providing access to \n"+
    		"foreign persons.";

    /** application icon */
    public static final String      ICON_PATH           = "resources/komodo.jpg";

    /** animated icon */
    public static final String      ABOUT_ICON_PATH     = "resources/komodo_a.gif";

    /** Maximum login attempt count */
    public static final int         LOGIN_ATTEMPT_COUNT = 3;

    /** Reference to logger object */
    private Logger _logger = Logger.getLogger(SavannahModel.class.getName());

    //-----------------------------
    //instance fields

    /** FEI Domain file property key */
    protected String                _domainFileKey      = Constants.PROPERTY_DOMAIN_FILE;

    /** reference to FEI Session object */
    protected Session               _session;

    /** reference to FEI filetype connection */
    protected FileType              _fileType;

    /** lock for synchronization */
    protected final Object          _syncLock           = new Object();

    /** FEI username */
    protected String                _username           = null;

    /** FEI password */
    protected String                _password           = null;

    /** FEI server identifier */
    protected String                _currentFeiGroup    = null;

    /** FEI file type */
    protected String                _currentFeiType     = null;
    
    /** status message */
    protected String                _statusMessage;

    /** location of keystore file */
    //protected String                _keystore;

    /** list of FEI server groups */
    protected List                  _serverGroups       = null;

    /** filename of FEI domain file */
    //protected String                _domainFilename;
    protected URL                _domainFileURL;

    /** flag indicating whether application is busy */
    protected boolean               _isBusy;

    /** Enables bean property change event handling */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(this);

    /** Component used as reference for error messages, can be null */
    protected Component             _relativeComponent;
    
    /** Date/time formatter */
    protected DateTimeFormatter     _dateTimeFormatter;
    
    //-----------------------------
    //Listing refresh 
    
    public final static int TARGET_FEI    = 1;

    public final static int TARGET_LOCAL  = 2;
    
    public final static int DEFAULT_MAX_REFRESH_RATE = 20;
    
    protected int _maxListingRefreshRate = DEFAULT_MAX_REFRESH_RATE;
    
    /** 
     * Property to control how many files are processed before a refresh
     * is performed, positive integer.
     */
    public static final String PROPERTY_MAX_REFRESH_RATE = "savannah.max.refresh"; 
    
    //-----------------------------
    //Models and managers
    
    /** transfer history model */
    protected SavannahTransferModel _transferModel;

    /** Regular expression filename filter for local files */
    protected SavannahFilterModel   _filterModel;

    /** Logging publisher, log model is added as a listener */
    protected LogMessagePublisher   _logPublisher;

    /** log history model */
    protected SavannahLogModel      _logModel;

    /** subscription manager field */
    protected MetaSubscriptionManager _subscriptionManager;
    
    /** model for maintaining receival history */
    protected ReceivalHistoryModel  _receivalModel;

    /** model for maintaining session settings */
    protected SavannahSessionModel  _sessionModel;
    
    /** user authenticator instance */
    protected UserAuthenticator _authenticator;
    
    /** Date filter model */
    protected SavannahDateFilterModel _dateFilterModel;
    
    
    //---------------------------------------------------------------------

    //Interface and class used to communicate user GUI options
    //from GUI thread to worker thread.

    static interface RunnableWithUserReply extends Runnable {
        public int getUserReply();
    }
    static abstract class AbstractRunnableWithUserReply implements
                                             RunnableWithUserReply {
        protected int _userReply;
        public int getUserReply() {
            return this._userReply;
        }
    }

    //---------------------------------------------------------------------

    /**
     * Constructor.
     */

    public SavannahModel()
    {
        init();
    }

    //---------------------------------------------------------------------

    /**
     * Initializes model, loads properties, establishes server list
     */

    protected void init()
    {
        //add shutdown handler
        Runtime.getRuntime().addShutdownHook(new ShutDownHandler());
        
        //init sub models
        this._receivalModel = new ReceivalHistoryModel(this);
        this._transferModel = new SavannahTransferModel();
        this._logModel      = new SavannahLogModel();
        this._sessionModel  = new SavannahSessionModel();
        this._sessionModel.addPropertyChangeListener(this);
                
        // init data members
        this._serverGroups = new ArrayList();
        this._isBusy = false;        
        this._statusMessage = "";
        this._filterModel = new SavannahFilterModel(); 
        this._filterModel.addFilter(new SavannahListFilter("LOCAL_FILTER"));
        this._filterModel.addFilter(new SavannahListFilter("FEI_FILTER"));
        this._filterModel.addFilter(new SavannahListFilter("FEI_FT_FILTER")); 

        //connect log model to publisher
        this._logPublisher = LogMessagePublisherSingleton.instance();
        this._logPublisher.addLogMessageListener(this._logModel);

        loadProperties();
        //cache FEI username password if defined
        this._username = System.getProperties().getProperty("fei.username");
        this._password = System.getProperties().getProperty("fei.password");

        //try using the user.name property
        if (this._username == null)
            _username = System.getProperties().getProperty("user.name");

        this._subscriptionManager = new DefaultMetaSubscriptionManager(this);
        
        this._dateTimeFormatter = new DateTimeFormatter();
        
        _dateFilterModel = new SavannahDateFilterModel();
        
        
        initSession();
        
        loadAuthenticator();        
    }
    
    //---------------------------------------------------------------------
    
    protected void loadProperties()
    {
        //cache FEI username password if defined
        this._username = System.getProperties().getProperty("fei.username");
        this._password = System.getProperties().getProperty("fei.password");

        //try using the user.name property
        if (this._username == null)
            _username = System.getProperties().getProperty("user.name");
        
        //refresh rate
        String refreshRateStr = System.getProperty(PROPERTY_MAX_REFRESH_RATE);
        if (refreshRateStr != null)
        {
            int rate;
            try {
                rate = Integer.parseInt(refreshRateStr);
                if (rate < 1)
                {
                    _logger.warn("Value for property '"+
                            PROPERTY_MAX_REFRESH_RATE+"' must be positive "+
                            "integer. Using default value.");
                    rate = DEFAULT_MAX_REFRESH_RATE;
                }
            } catch (NumberFormatException nfEx) {
                _logger.warn("Value for property '"+
                        PROPERTY_MAX_REFRESH_RATE+"' could not be parsed: "+
                        refreshRateStr+". Using default value.");
                rate = DEFAULT_MAX_REFRESH_RATE;
            }
            this._maxListingRefreshRate = rate;
        }
    }
    
    //---------------------------------------------------------------------

    /**
     * Initializes client information including SSL keystore and FEI domain
     * lookup service.
     */
    
    protected void initSession()
    {
        //establish keystore location
        /*
        String keystore = System.getProperties().getProperty(_keystoreKey);
        if (keystore == null)
        {
            throw new RuntimeException(__classname + "::initSession(): "
                    + "Could not find value for property: " + _keystoreKey);
        }
        this._keystore = keystore;

        //test to make sure keystore file exists
        if (!(new File(_keystore)).canRead())
        {
            throw new RuntimeException(__classname + "::initSession(): "
                    + "Keystore file '" + _keystore + "' does not exist or "
                    + "cannot be read.");
        }
        */

        boolean loadSession = true;

        //get the domain file
        ConfigFileURLResolver resolver = new ConfigFileURLResolver();
        try {
            this._domainFileURL = resolver.getDomainFile();
        } catch (SessionException sesEx) {
            throw new RuntimeException(__classname + "::initSession(): "
                    + "Session error occurred " + sesEx.getMessage());
        }
        
        
        if (this._domainFileURL == null)
        {
            throw new RuntimeException(__classname + "::initSession(): "
                    + "Could not find value for property: " + _domainFileKey);
        }
        
        if (!resolver.canReadURL(this._domainFileURL))
        {
            JOptionPane.showMessageDialog(_relativeComponent, "Domain file '"
                    + this._domainFileURL
                    + "' \ndoes not exist or cannot be read.\n\n"
                    + "Use the Options Menu to load an exising domain file.",
                    "Session Error", JOptionPane.ERROR_MESSAGE);
            loadSession = false;    
        }
        
        //-------------------------
        
        if (loadSession)
        {
            //construct the session object
            boolean success = loadSession(this._domainFileURL);

            if (success)
            {
                //get listing of server groups and load em
                loadFeiServerGroups();
            }
            else
            {
                JOptionPane.showMessageDialog(_relativeComponent,
                        "Could not create a session object with domain file\n"
                        + "'" + this._domainFileURL + "'.\n\n" +
                        "Use the Options Menu to load an exising domain file.",
                        "Session Error", JOptionPane.ERROR_MESSAGE);
            }
        }        
        
        //-------------------------
        
        String dateTimeFormat = this._sessionModel.getDateTimeFormat();
        try {
            new SimpleDateFormat(dateTimeFormat);
        } catch (Exception ex) {
            dateTimeFormat = DateTimeFormatter.DEFAULT_FORMAT;
        }
        this._dateTimeFormatter = new DateTimeFormatter(dateTimeFormat);        
        
        //-------------------------
    }
    
    //---------------------------------------------------------------------

    protected boolean loadAuthenticator()
    {        
        boolean loaded = false;
        
        //-------------------------       
        
        //make sure we have a domain file
        if (this._domainFileURL == null)
        {
            //get the domain file
            ConfigFileURLResolver resolver = new ConfigFileURLResolver();
            
            try {
                this._domainFileURL = resolver.getDomainFile();
            } catch (SessionException sesEx) {
                this._domainFileURL = null;
            }
            
            if (this._domainFileURL == null)
            {
                throw new RuntimeException(__classname + "::initSession(): "
                        + "Could not find value for property: " + _domainFileKey);
            }
            
            if (!resolver.canReadURL(this._domainFileURL))
            {
                JOptionPane.showMessageDialog(_relativeComponent, "Domain file '"
                        + this._domainFileURL
                        + "' \ndoes not exist or cannot be read.\n\n"
                        + "Use the Options Menu to load an exising domain file.",
                        "Session Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        //-------------------------

        if (this._domainFileURL != null)
        {
            try {
                
                this._authenticator = new UserAuthenticator(_domainFileURL);
                loaded = true;  
                
            } catch (SessionException sesEx) {
                _logger.error(sesEx.getMessage()); 
                _logger.debug(null, sesEx);
                JOptionPane.showMessageDialog(_relativeComponent,
                        "Error occurred while " +
                        "initialing authenticator.\n\n" +
                        "ERROR DETAILS\n-MESSAGE: " + sesEx.getMessage() +
                        "\n-CODE: " + sesEx.getErrno(),
                        "Authenticator Error", JOptionPane.ERROR_MESSAGE);
            }
            
            //--------------
            
//            try {
//                
//                this._tokenGenerator = new UserTokenGenerator(_domainFileURL);                
//           
//            } catch (SessionException sesEx) {
//                _logger.error(sesEx.getMessage()); 
//                _logger.debug(null, sesEx);
//                JOptionPane.showMessageDialog(_relativeComponent,
//                        "Error occurred while " +
//                        "initialing token generator.\n\n" +
//                        "ERROR DETAILS\n-MESSAGE: " + sesEx.getMessage() +
//                        "\n-CODE: " + sesEx.getErrno(),
//                        "Authenticator Error", JOptionPane.ERROR_MESSAGE);
//            }
            
        }
        
        //-------------------------
        
        return loaded;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns reference to the transfer model used to track file 
     * transaction history.
     * @return Reference to model's SavannahTransferModel
     */

    public SavannahTransferModel getTransferModel()
    {
        return this._transferModel;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns reference to the session model used to maintain session
     * state between session instances.
     * @return Session model
     */

    public SavannahSessionModel getSessionModel()
    {
        return this._sessionModel;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns reference to the file filter model.
     * @return Filter model
     */
    
    public SavannahFilterModel getFilterModel()
    {
        return this._filterModel;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns reference to the file filter model.
     * @return Filter model
     */
    
    public SavannahDateFilterModel getDateFilterModel()
    {
        return this._dateFilterModel;
    }
    
    

    //---------------------------------------------------------------------

    /**
     * Returns reference to the receival model.
     * @return Receival model
     */
    
    public ReceivalHistoryModel getReceivalModel()
    {
        return this._receivalModel;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns reference to the logging model used by this object
     * @return Reference to log model
     */
     
    public SavannahLogModel getLoggingModel()
    {
        return this._logModel;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns reference to the subscription manager used by this object
     * @return reference to MetaSubscriptionManager
     */
    
    public MetaSubscriptionManager getSubscriptionManager()
    {
        return this._subscriptionManager;
    }
    
    //---------------------------------------------------------------------

    /**
     * Adds listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            added
     */

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        _changes.addPropertyChangeListener(l);
    }

    //---------------------------------------------------------------------

    /**
     * Removes listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            removed
     */

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        _changes.removePropertyChangeListener(l);
    }
    
    //---------------------------------------------------------------------

    /**
     * Closes all sessions and resets all references for sub-components.
     * Once this method is called, instance should no longer receive
     * method invocation requests.
     */
    
    public void destroy()
    {
        synchronized (_syncLock)
        {
            if (_session != null)
            {
                printDebug("Closing session...");
                _session.closeImmediate();
                this._session = null;
                printDebug("Session closed.");
            }
        }

        this._domainFileURL = null;
        this._username = null;
        this._password = null;
        this._transferModel = null;
        this._sessionModel.removePropertyChangeListener(this);
        this._sessionModel = null;
        this._logPublisher.removeLogMessageListener(this._logModel);
        this._logPublisher = null;
        this._logModel = null;
        this._filterModel = null;

    }

    //---------------------------------------------------------------------

    /**
     * Retrieves a list of server group names from the session object
     * @return True if load was successful, false otherwise.
     */

    protected boolean loadFeiServerGroups()
    {
        boolean loaded = false;

        synchronized (_syncLock)
        {
            if (_session != null)
            {
                List serverGroups = null;

                /*
                 * try { serverGroups = _session.getGroupList();
                 * setAvailableFeiServers(serverGroups); loaded = true; } catch
                 * (SessionException sesEx) {
                 * JOptionPane.showMessageDialog(_relativeComponent, "Error
                 * occurred while retrieving file types\n"+ "from FEI.\n\n"+
                 * "ERROR DETAILS\n-MESSAGE: "+ sesEx.getMessage()+ "\n-CODE:
                 * "+sesEx.getErrno(), "Group List Error",
                 * JOptionPane.ERROR_MESSAGE); }
                 */

                //TODO - repace with commented code above when SesEx's are
                // added
                serverGroups = _session.getGroupList();
                if (serverGroups.isEmpty())
                {
                    _logger.error("Session returned an empty server group list.  " +
                    		    "Domain file is "+this._domainFileURL.toString());
                    JOptionPane.showMessageDialog(_relativeComponent,
                            "Current session returned an empty list of server groups.  Please\n"
                            + "check '" + this._domainFileURL + "'.\n\n" +
                            "Use the Options Menu to load an exising domain file.",
                            "Session Error", JOptionPane.ERROR_MESSAGE);
                   
                }
                else
                {
                    setAvailableFeiServers(serverGroups);
                    loaded = true;
                }
            }
        }

        return loaded;
    }

    //---------------------------------------------------------------------

    /**
     * Requests refresh of FEI server group listing
     */

    public void refreshFeiServerGroups()
    {
        setDomainFile(this._domainFileURL, true);
        loadFeiServerGroups();
    }
    
    //---------------------------------------------------------------------

    /**
     * Sets username.
     * @param username New username
     */

    public void setUsername(String username)
    {
        if (this._username == null && username == null)
            return;
        if (this._username != null && this._username.equals(username))
            return;

        String oldUsername = this._username;
        this._username = username;

        login();

        _changes.firePropertyChange("FEI_USERNAME", this._username,
                                    oldUsername);
    }

    //---------------------------------------------------------------------

    /**
     * Returns current username for session
     * @return current username
     */

    public String getUsername()
    {
        return this._username;
    }

    //---------------------------------------------------------------------

    /**
     * Sets password for connection to FEI server groups
     * @param password New password string, or null to reset
     */

    public void setPassword(String password)
    {
        if (this._password == null && password == null)
            return;
        if (this._password != null && this._password.equals(password))
            return;

        this._password = password;
        login();
    }

    //---------------------------------------------------------------------

    /**
     * Attempts to login to Client using username, password fields.
     */

    protected void login()
    {
        synchronized (_syncLock)
        {
            if (_session == null)
                return;

            if (_username != null && _password != null)
            {
                _session.setLoginInfo(_username, _password);
            }
        }
    }

    //---------------------------------------------------------------------

    /**
     * Returns the formatter used for date/time rendering
     * @return current date/time formatter
     */

    public DateTimeFormatter getDateTimeFormatter()
    {
        return this._dateTimeFormatter;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns the format string used for date/time rendering
     * @return current date/time format expression
     */

    public String getDateTimeFormat()
    {
        return this._sessionModel.getDateTimeFormat();
    }
    
    
    //---------------------------------------------------------------------

    /**
     * Sets the date/time format for this model.  If the parameter
     * is null or not well-formed, then it will be replaced by the
     * default format expression as declared by <code>
     * DateTimeFormatter.DEFAULT_FORMAT</code>
     * @param format Date/time format expression string 
     */

    public void setDateTimeFormat(String format)
    {
        if (format != null)
        {
            try {
                new SimpleDateFormat(format);
            } catch (Exception ex) {
                format = null;
            }            
        }
        if (format == null)
        {
            format = DateTimeFormatter.DEFAULT_FORMAT;
        }
        
        
        if (!this._sessionModel.getDateTimeFormat().equals(format))
        {
            
            //test format, if legal, proceed, else return
            try {
                new SimpleDateFormat(format);
            } catch (Exception ex) {
                return;
            }
            
            this._sessionModel.setDateTimeFormat(format);
            DateTimeFormatter d = new DateTimeFormatter(format);
            this._dateTimeFormatter = d;
            
            this.requestRefresh(TARGET_LOCAL);
            this.requestRefresh(TARGET_FEI);
        }
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns the name of the current fei server group
     * @return current FEI server
     */

    public String getCurrentFeiServer()
    {
        return this._currentFeiGroup;
    }
    //---------------------------------------------------------------------
    
    /**
     * Unsets the current FEI server group by assigning null to it.  This
     * has the effect of "disconnecting" to the server.  
     */
    public void unsetCurrentFeiGroup() 
    {      
        this.setCurrentFeiGroup(null);
    }
    
    
    //---------------------------------------------------------------------

    /**
     * Sets the current FEI server group according to the paraemeter
     * @param groupName The name of the new FEI server group, or null to reset
     */

    public void setCurrentFeiGroup(String groupName)
                                   throws IllegalArgumentException
    {
        printDebug(__classname + "::setCurrentFeiGroup(): " + "Entered...");

        //if serverName == _currentFeiServer, do nothing
        if (this._currentFeiGroup == null && groupName == null)
            return;
        if (this._currentFeiGroup != null
                && this._currentFeiGroup.equals(groupName))
            return;

        //if servername not null but not recognized, error
        if (groupName != null && !_serverGroups.contains(groupName))
        {
            throw new IllegalArgumentException(__classname
                    + "::setCurrentFeiGroup(): Group name not recogized: "
                    + groupName);
        }

        //try getting some login data now
        if (groupName != null)
        {
            boolean proceed = true, success = false;
            int attemptCount = 0;
            if (this._username == null || this._password == null)
            {
                proceed = getLoginInfo(groupName);                   
            }
            
            while (proceed && !success && attemptCount < LOGIN_ATTEMPT_COUNT)
            {
                boolean canConn = false;
                
                try {
                    canConn = canUserConnect(this._username, this._password,
                                             groupName, null);
                } catch (SessionException sesEx) {
                    if (sesEx.getErrno() == Constants.CONN_FAILED)
                    {
                        setStatusMessage("Login aborted. (network error)");
                        JOptionPane.showMessageDialog(_relativeComponent,
                              "Unable to connect to server group '" +
                              groupName+"'.\nPlease check network status and "+
                              "FEI domain file\nconfiguration.",
                              "Connection Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }                    
                    else
                    {
                        setStatusMessage("Login aborted.");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Unable to connect to server group '" +
                                groupName+"'.\nMessage: "+sesEx.getMessage(),
                                "Login Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                if (canConn)
                {
                    success = true;
                }
                else
                {
                    ++attemptCount;
                    setStatusMessage("Login failed.");
                    if (attemptCount < LOGIN_ATTEMPT_COUNT)
                    {
                        JOptionPane.showMessageDialog(_relativeComponent,
                            "Invalid login.  Please re-enter "+
                            "username and password in login window.",
                            "Login Error", JOptionPane.ERROR_MESSAGE);
                        proceed = getLoginInfo(groupName);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Invalid login.  Max attempt count ("+
                                SavannahModel.LOGIN_ATTEMPT_COUNT+") reached!",
                                "Login Error", JOptionPane.ERROR_MESSAGE); 
                    }
                }
                
            }
      
            if (!success)
            {
                setStatusMessage("Login aborted.");
                setPassword(null);
                return;
            }
            
            
            //inform session of login info
            synchronized (_syncLock)
            {
                _session.setLoginInfo(this._username, this._password);
            }
        } 
        else
        {
            //  Clearing password if disconnecting
            this.setPassword(null);
        }

        Object oldValue = this._currentFeiGroup;
        this._currentFeiGroup = groupName;
        _changes.firePropertyChange("CURRENT_FEI_GROUP", oldValue,
                                    this._currentFeiGroup);
    }

    //---------------------------------------------------------------------
    
    protected boolean tryAuthenticating(String groupName)
    {
        boolean proceed = true, success = false;
        int attemptCount = 0;
        if (this._username == null || this._password == null)
        {
            proceed = getLoginInfo(groupName);                   
        }
        
        while (proceed && !success && attemptCount < LOGIN_ATTEMPT_COUNT)
        {
            boolean canConn = false;
            
            try {
                attemptCount++;
                canConn = canUserConnect(this._username, this._password,
                                         groupName, null);
                
                if (canConn)
                    success = true;    
                else
                {
                    setStatusMessage("Login failed.");
                    if (attemptCount < LOGIN_ATTEMPT_COUNT)
                    {
                        JOptionPane.showMessageDialog(_relativeComponent,
                            "Invalid login.  Please re-enter "+
                            "username and password in login window.",
                            "Login Error", JOptionPane.ERROR_MESSAGE);
                        proceed = getLoginInfo(groupName);
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Invalid login.  Max attempt count ("+
                                SavannahModel.LOGIN_ATTEMPT_COUNT+") reached!",
                                "Login Error", JOptionPane.ERROR_MESSAGE); 
                    }
                }
                
            } catch (SessionException sesEx) {
                if (sesEx.getErrno() == Constants.CONN_FAILED)
                {
                    setStatusMessage("Login aborted. (network error)");
                    JOptionPane.showMessageDialog(_relativeComponent,
                          "Unable to connect to server group '" +
                          groupName+"'.\nPlease check network status and "+
                          "FEI domain file\nconfiguration.",
                          "Connection Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }                    
                else
                {
                    setStatusMessage("Login aborted.");
                    JOptionPane.showMessageDialog(_relativeComponent,
                            "Unable to connect to server group '" +
                            groupName+"'.\nMessage: "+sesEx.getMessage(),
                            "Login Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        return success;
    }
    
    //---------------------------------------------------------------------

    /**
     * Loads new session object and replaces old session object if necessarry.
     * @param domainFileURL URL of the FEI domain file
     * @return True if connection was successful, false otherwise.
     */

    protected boolean loadSession(URL domainFileURL)
    {
        Session newSession;
        
        //set session options
        try {
            newSession = this._sessionModel.createSession(domainFileURL);
        } catch (SessionException sesEx) {
            _logger.error(sesEx.getMessage()); 
            _logger.debug(null, sesEx);
            JOptionPane.showMessageDialog(_relativeComponent,
                    "Error occurred while " +
                    "initialing settings\nof new session.\n\n" +
                    "ERROR DETAILS\n-MESSAGE: " + sesEx.getMessage() +
                    "\n-CODE: " + sesEx.getErrno(),
                    "Session Init Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception ex) {
            _logger.error(ex.getMessage(), ex); //ex.stackTrace();
            JOptionPane.showMessageDialog(_relativeComponent,
                    "Error occurred while " +
                    "initializing settings\nof new session.\n\n" +
                    "ERROR DETAILS\n-MESSAGE: " + ex.getMessage(),
                    "Session Init Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        //------------------------------

        //set the _session reference to the created session object
        synchronized (_syncLock)
        {
            if (this._session != null)
            {
                //logout of current client if it exists
                printDebug(__classname + "::createSession(): "
                        + "Calling _session close()");
                _session.closeImmediate();
                setPassword(null);
            }
            this._session = newSession;
            this._session.setLoginInfo(this._username, this._password);
        }

        //------------------------------
        

        
        return true;
    }
    
    //---------------------------------------------------------------------

    /**
     * Displays dialog for user to enter username and password
     */

    public boolean getLoginInfo(String groupName)
    {
        boolean proceed = false;
        
        String connFailedMesg = "Unable to authenticate for group '"+groupName+
                                "' due to connection failure.  Possible network issue.";

        connFailedMesg = "Unable to connect to server group '" +  groupName+
                "'.\nPlease check network status and FEI domain file configuration.";
        
        //create login dialog
        LoginDialog loginDialog = new EncryptedLoginDialog("FEI Login", _username);
        
        //-------------------------
        
        //does server group want a password or passcode?        
        try {
            AuthenticationType authType = 
                        _authenticator.getAuthenticationType(groupName);
            String passPrmpt = PasswordUtil.getPrompt(authType);
            loginDialog.setPasswordPrompt(passPrmpt);
        } catch (SessionException sesEx) {
            if (sesEx.getErrno() == Constants.CONN_FAILED)
            {                
                JOptionPane.showMessageDialog(_relativeComponent,
                        connFailedMesg, "Authentication Error",
                        JOptionPane.ERROR_MESSAGE);           
                return false;
            }
            
            _logger.error("Error occured while attempting to query '"+
                    "auth method for server group '"+groupName+"': "+
                    sesEx.getMessage()+" ("+sesEx.getErrno()+")");
            _logger.trace(null, sesEx);
        }
        
        //-------------------------
        
//        TokenLoginDialog loginDialog = new TokenLoginDialog("FEI Login", _username,
//                                                    this._domainFileURL, groupName);

        int rVal = loginDialog.showDialog(_relativeComponent);

        if (rVal == JOptionPane.OK_OPTION)
        {
            String username = loginDialog.getUsername();
            String password = loginDialog.getPassword();
            
            //get an authentication token
            UserToken token = null;
            String errMesg  = null;
                    
            try {
                token = getAuthenticationToken(username, 
                                               password, groupName);
            } catch (SessionException sesEx) {
                
                if (sesEx.getErrno() == Constants.CONN_FAILED)
                {
                    errMesg = connFailedMesg;
                }                
            }
            
            if (token != null && token.isValid())
            {
                setUsername(username);            
                setPassword(token.getToken());
                proceed = true;
            }
            else
            {
                if (errMesg == null)
                {
                    errMesg = "Could not generate authentication token for user '"+
                               username+"'.  Please check login credentials.\n";
                }
                
                JOptionPane.showMessageDialog(_relativeComponent,
                                 errMesg, "Authentication Error",
                                 JOptionPane.ERROR_MESSAGE);
            }
        }

        return proceed;
    }

    //---------------------------------------------------------------------

    /**
     * Returns authentication token for login credentials.  If the
     * authentication cannot be performed (i.e. invalid login,
     * server group not defined) then null will be returned.
     * @param username Username
     * @param password Password
     * @param serverGroup Server group
     */
    
    protected UserToken getAuthenticationToken(String username, String password,
                                               String serverGroup) throws SessionException
    {
        UserToken token = null;
        try {
            
            token = this._authenticator.authenticate(username, 
                                         password, serverGroup);
            
        } catch (SessionException sesEx) {
            _logger.error("Error occured while attempting to authenticate '"+
                    username+"' for server group '"+serverGroup+"'");
            _logger.trace(null, sesEx);
            throw sesEx;
        }
        return token;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets list of available FEI server groups.
     * @param serverGroups List of FEI server groups
     */

    public void setAvailableFeiServers(List serverGroups)
    {
        if (serverGroups == null)
            serverGroups = new ArrayList();

        if (true)
        { //!this._serverGroups.equals(serverGroups)) {
            Object oldValue = this._serverGroups;
            this._serverGroups = new ArrayList(serverGroups);
            _changes.firePropertyChange("AVAILABLE_FEI_SERVERS", oldValue,
                    this._serverGroups);
        }
    }

    //---------------------------------------------------------------------

    /**
     * Returns list of available FEI server groups.
     * @return List of FEI server groups
     */

    public List getAvailableFeiServers()
    {
        return new ArrayList(_serverGroups);
    }

    //---------------------------------------------------------------------

    /**
     * Returns a List of FEI types from the current FEI server
     * @return List of available FEI file types
     */

    public List getFeiTypes()
    {
        List formatTypes = new ArrayList();
        List groupTypes = null;
        boolean error = false;
        String statusMessage = "OK";
        int statusValue = Constants.OK;
        SavannahListFilter filter = this._filterModel.getFilter(
                                               "FEI_FT_FILTER");
        String filterPattern = filter.isEnabled() ? filter.getPattern() : 
                                           SavannahListFilter.FILTER_ALL;
        //filterPattern = SavannahFileFilter.regularize(filterPattern);        
        filterPattern = WildcardRegexUtil.wildCardToRegex(filterPattern, true);
        
        try
        {
            synchronized (_syncLock)
            {
                if (_session != null)
                {
                    if (this._currentFeiGroup != null)
                    {
                        groupTypes = this._session.getFileTypeList(
                                            this._currentFeiGroup);
                        while (this._session.getTransactionCount() > 0)
                        {
                            Result r = this._session.result();
                            if (r == null)
                                continue;
                            if (r.getErrno() != Constants.OK)
                            {
                                statusValue = r.getErrno();
                                statusMessage = r.getMessage();
                            }
                        }
                    }
                    else
                    {
                        groupTypes = new ArrayList();
                    }
                }
                else
                {
                    statusValue = Constants.NO_SERVERS;
                    statusMessage = "Cannot retrieve filetypes. " +
                                    "No session exists.";
                }
            }
        } catch (SessionException sesEx) {       
            _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
            statusValue = sesEx.getErrno();
            statusMessage = sesEx.getMessage();
        } catch (Exception ex) {        
            _logger.error(ex.getMessage(), ex); //ex.stackTrace();
            statusValue = Constants.EXCEPTION;
            statusMessage = ex.getMessage();
        }

        if (statusValue != Constants.OK)
        {
            groupTypes = new ArrayList();
            JOptionPane.showMessageDialog(_relativeComponent,
                    "Error occurred while retrieving file types\n"
                            + "from FEI.\n\n" + "ERROR DETAILS\n-MESSAGE: "
                            + statusMessage + "\n-CODE: " + statusValue,
                    "File Type Error", JOptionPane.ERROR_MESSAGE);
        }

        //---------------------------------

        //format types to remove group info

        int numEntries = groupTypes.size();
        int index;
        String entry;
        for (int i = 0; i < numEntries; ++i)
        {
            entry = (String) groupTypes.get(i);
            if (FileType.isFullFiletype(entry))
                entry = FileType.extractFiletype(entry);
            
            if (entry.matches(filterPattern))
                formatTypes.add(entry);
        }

        //---------------------------------

        return formatTypes;
    }

    //---------------------------------------------------------------------
    // Session Behavior
    //---------------------------------------------------------------------
    
    /**
     * Method to add a list of files to the current server
     * @param filenames String array of filenames to be added
     */
    
    public void addToFei(final String[] filenames)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _addToFei(filenames);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
    }

    //---------------------------------------------------------------------

    /**
     * Method to add a list of files to the current server
     * @param filenames String array of filenames to be added
     */

    protected void _addToFei(final String[] filenames)
    {
        if (filenames == null || filenames.length == 0)
            return;

        final boolean wasBusy = _isBusy;

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Adding " + filenames.length
                        + " files to FEI.  Please wait...");
            }
        });

        int statusValue = Constants.IO_ERROR;
        int userReply = 0;
        String statusMesg = "No reply from server.";
        boolean cont = true;
        SavannahTransferRecord record = null;

        for (int i = 0; i < filenames.length && cont; ++i)
        {
            final String filename = filenames[i];
            final int fileIndex = i;

            //set status message
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    setStatusMessage("Adding " + filename + ". Please wait...");
                }
            });

            try
            {
                synchronized (_syncLock)
                {
                    final int xactId = this._fileType.add(
                            new String[] { filename }, null);

                    //make record
                    final long sTime = System.currentTimeMillis();
                    File tmpFile = new File(filename);
                    record = new SavannahTransferRecord(filename,
                            _currentFeiType, xactId, tmpFile.length(),
                            SavannahTransferRecord.TRANSACTION_TYPE_ADD);

                    //add record to model
                    final SavannahTransferRecord fRecord = record;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            _transferModel.addTransferRecord(fRecord);
                            fRecord.setStartTime(sTime);
                            fRecord.setState(SavannahTransferRecord.
                                             STATE_TRANSFERRING);
                        }
                    });

                    while (this._session.getTransactionCount() > 0)
                    {
                        Result r = this._session.result();
                        if (r == null)
                        {
                            Thread.yield();
                            continue;
                        }
                        statusValue = r.getErrno();
                        statusMesg = r.getMessage();
                    }
                }
            } catch (SessionException sesEx) {
                _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
                statusValue = sesEx.getErrno();
                statusMesg = sesEx.getMessage();
            } catch (Exception ex) {
                _logger.error(ex.getMessage(), ex); //ex.stackTrace();
                statusValue = Constants.EXCEPTION;
                statusMesg = ex.getMessage();
            } finally {
                final int fStatVal = statusValue;
                final String fStatMsg = statusMesg;
                final SavannahTransferRecord fRecord = record;
                if (fStatVal == Constants.OK)
                {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            setStatusMessage("Added " + filename + ".");
                            fRecord.setEndTime(System.currentTimeMillis());
                            fRecord
                                    .setState(SavannahTransferRecord.STATE_COMPLETE);
                            File file = new File(filename);
                            _receivalModel.addToFei("/" + _currentFeiGroup
                                    + "/" + _currentFeiType + "/"
                                    + file.getName());
                            
                            
                            //refresh if fileIndex matches refresh rate or if index 
                            //is a boundary value
                            if (shouldRequestRefresh(fileIndex, filenames.length))
                            {
                                requestRefresh(TARGET_FEI);
                            }
                        }
                    });
                }
//                else if (fStatVal == Constants.FILE_EXISTS ||
//                        (fStatVal == Constants.LOCKEDERR && 
//                         fStatMsg.indexOf("File already exists") != -1))
                else if (fStatVal == Constants.FILE_EXISTS ||
                        fStatVal == Constants.FILEALREADYEXISTS)                           
                {
                    //what does user want to do?
                    final Object[] buttons = new Object[] { "Yes",
                            "Yes to All", "No", "Cancel" };

                    //create runnable to get user option from dialog
                    RunnableWithUserReply rwa = new AbstractRunnableWithUserReply() {
                        public void run()
                        {
                            setStatusMessage(filename + " exists...");
                            this._userReply = JOptionPane.showOptionDialog(
                                    _relativeComponent, "File '" + filename
                                            + "'\n already exists in FEI.\n"
                                            + "\nReplace file?",
                                    "Replace Option",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.WARNING_MESSAGE, null, buttons,
                                    "Yes");
                        }
                    };

                    try {
                        SwingUtilities.invokeAndWait(rwa);
                        userReply = rwa.getUserReply();
                    } catch (Exception e) {
                        _logger.error(e.getMessage(), e); //e.stackTrace();
                        userReply = JOptionPane.CLOSED_OPTION;
                    }

                    if (userReply == JOptionPane.CLOSED_OPTION || userReply < 0
                            || userReply >= buttons.length
                            || buttons[userReply].equals("No"))
                    {
                        //"No", then skip current file and continue
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                printDebug("Aborting file " + filename);
                                fRecord
                                        .setState(SavannahTransferRecord.STATE_ABORTED);
                                setStatusMessage("'Add " + filename
                                        + "' aborted.");
                            }
                        });
                    }
                    else if (buttons[userReply].equals("Yes"))
                    {
                        //if "Yes", replace this file only
                        String[] curFileArray = new String[] { filename };
                        _transferModel.removeTransferRecord(fRecord);
                        _replaceToFei(curFileArray);
                    }
                    else if (buttons[userReply].equals("Yes to All"))
                    {
                        //if "Yes to All", replace remaining files
                        int numRemaining = filenames.length - i;
                        String[] restFileArray = new String[numRemaining];
                        for (int j = 0; j < numRemaining; ++j)
                        {
                            restFileArray[j] = filenames[j + i];
                        }
                        _transferModel.removeTransferRecord(fRecord);
                        _replaceToFei(restFileArray);
                        cont = false;
                    }
                    else if (buttons[userReply].equals("Cancel"))
                    {
                        RunnableWithUserReply rwa2 = new AbstractRunnableWithUserReply() {
                            public void run()
                            {
                                this._userReply = JOptionPane
                                        .showConfirmDialog(_relativeComponent,
                                                "Cancel transferring "
                                                        + "remaining files?",
                                                "Cancel Transfer",
                                                JOptionPane.YES_NO_OPTION);
                            }
                        };

                        try
                        {
                            SwingUtilities.invokeAndWait(rwa2);
                            userReply = rwa2.getUserReply();
                        } catch (Exception e)
                        {
                            _logger.error(e.getMessage(), e); //e.stackTrace();
                            userReply = JOptionPane.NO_OPTION;
                        }

                        //if yes, then set cont flag to false.
                        if (userReply == JOptionPane.YES_OPTION)
                        {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run()
                                {
                                    setStatusMessage("Aborted "
                                            + "remaining transfer.");
                                    fRecord.setState(SavannahTransferRecord.
                                                     STATE_ABORTED);
                                }
                            });
                            cont = false;
                        }
                        else
                        {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run()
                                {
                                    setStatusMessage("Aborted " + "adding '"
                                            + filename + "'");
                                    fRecord.setState(SavannahTransferRecord.
                                                     STATE_ABORTED);
                                }
                            });
                        }
                    } //end_user_select_CANCEL
                } //end_if_FILE_EXISTS
                else
                {
                  try{
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run()
                        {
                            setStatusMessage("'Add " + filename + "' aborted.");
                            fRecord.setState(SavannahTransferRecord.STATE_ERROR);
                            JOptionPane.showMessageDialog(
                                            _relativeComponent,
                                            "Error occurred while adding '"
                                                    + filename
                                                    + "'\nto FEI.\n\n"
                                                    + "ERROR DETAILS\n-MESSAGE: "
                                                    + fStatMsg + "\n-CODE: "
                                                    + fStatVal, "Add Error",
                                            JOptionPane.ERROR_MESSAGE);
                        }
                    });
                  } catch (Exception ex) { 
                      _logger.error("Error occurred while showing dialog", ex);
                  }
                }
            } //end_finally
        } //end_for

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }

    //---------------------------------------------------------------------

    /**
     * This method replaces a list of files to the current filetype
     * @param filenames Array of filenames to copy to FEI
     */

    public void replaceToFei(final String[] filenames)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _replaceToFei(filenames);
            }
        };

        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
    }

    //---------------------------------------------------------------------

    /**
     * Method to replace a list of files to the current server. This is the
     * synchronous version of copyToFei. Called by asynchronous method on a new
     * thread.
     * @param filenames Array of filenames to copy to FEI
     */

    protected void _replaceToFei(final String[] filenames)
    {
        if (filenames == null || filenames.length == 0)
            return;

        final boolean wasBusy = _isBusy;

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Copying " + filenames.length
                        + " files to FEI.  Please wait...");
            }
        });

        boolean cont = true;
        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        SavannahTransferRecord record = null;

        for (int i = 0; i < filenames.length && cont; ++i)
        {
            final int fileIndex = i;
            final String filename = filenames[i];
            if (filename == null)
                continue;

            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    setStatusMessage("Copying " + filename + ". Please wait...");
                }
            });

            try
            {
                synchronized (_syncLock)
                {
                    int xactId = this._fileType.replace(
                            new String[] { filename }, null);
                    final long sTime = System.currentTimeMillis();
                    File tmpFile = new File(filename);
                    record = new SavannahTransferRecord(filename,
                            _currentFeiType, xactId, tmpFile.length(),
                            SavannahTransferRecord.TRANSACTION_TYPE_REPLACE);

                    //add record to model
                    final SavannahTransferRecord fRecord = record;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            _transferModel.addTransferRecord(fRecord);
                            fRecord.setStartTime(sTime);
                            fRecord.setState(SavannahTransferRecord.STATE_TRANSFERRING);
                        }
                    });

                    while (this._session.getTransactionCount() > 0)
                    {
                        Result r = this._session.result();
                        if (r == null)
                        {
                            Thread.yield();
                            continue;
                        }
                        statusValue = r.getErrno();
                        statusMesg = r.getMessage();
                    }
                } //end_sync
            } catch (SessionException sesEx)
            {
                _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
                statusValue = sesEx.getErrno();
                statusMesg = sesEx.getMessage();
            } catch (Exception ex)
            {
                _logger.error(ex.getMessage(), ex); //ex.stackTrace();
                statusValue = Constants.EXCEPTION;
                statusMesg = ex.getMessage();
            } finally
            {
                final int fStatVal = statusValue;
                final String fStatMsg = statusMesg;
                final SavannahTransferRecord fRecord = record;

                if (fStatVal == Constants.OK)
                {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            setStatusMessage("Copied " + filename + ".");
                            fRecord.setEndTime(System.currentTimeMillis());
                            fRecord.setState(SavannahTransferRecord.STATE_COMPLETE);
                            File file = new File(filename);
                            _receivalModel.addToFei("/" + _currentFeiGroup
                                    + "/" + _currentFeiType + "/"
                                    + file.getName());
                            
                            //refresh if fileIndex matches refresh rate or if index 
                            //is a boundary value
                            if (shouldRequestRefresh(fileIndex, filenames.length))
                            {
                                requestRefresh(TARGET_FEI);
                            }
                        }
                    });
                }
                else if (fStatVal == Constants.FILE_EXISTS)
                {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            File file = new File(filename);
                            setStatusMessage("Diffed file '"+file.getName()+"' unchanged.  Skipping copy.");
                            fRecord.setEndTime(System.currentTimeMillis());
                            fRecord.setState(SavannahTransferRecord.STATE_COMPLETE);             
//                            File file = new File(filename);
//                            _receivalModel.addToFei("/" + _currentFeiGroup
//                                    + "/" + _currentFeiType + "/"
//                                    + file.getName());
//                            
//                            //refresh if fileIndex matches refresh rate or if index 
//                            //is a boundary value
//                            if (shouldRequestRemoteRefresh(fileIndex, filenames.length))
//                            {
//                                requestRefresh(TARGET_FEI);
//                            }
                        }
                    });
                }
                else
                {
                  try{
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run()
                        {
                            setStatusMessage("'Copy " + filename + "' aborted.");
                            fRecord.setState(SavannahTransferRecord.STATE_ERROR);
                            JOptionPane.showMessageDialog(
                                            _relativeComponent,
                                            "Error occurred while copying '"
                                                    + filename
                                                    + "'\nto FEI.\n\n"
                                                    + "ERROR DETAILS\n-MESSAGE: "
                                                    + fStatMsg + "\n-CODE: "
                                                    + fStatVal, "Copy Error",
                                            JOptionPane.ERROR_MESSAGE);
                        }
                    });
                  } catch (Exception ex) { 
                      _logger.error("Error occurred while showing dialog", ex);
                  }
                }
            } //end_finally
        } //end_for

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }

    //---------------------------------------------------------------------

    /**
     * This method gets files from the server to local directory
     * @param filenames Array of filenames to be retreived
     * @param destination Absolute path to destination directory
     */

    public void getFromFei(final String[] filenames, final File destination)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _getFromFei(filenames, destination);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * This method gets files from the server to local directory
     * @param filenames Array of filenames to be retreived
     */

    public void getFromFei(final String[] filenames)
    {
        getFromFei(filenames, _sessionModel.getLocalDirectory());
    }

    //---------------------------------------------------------------------

    /**
     * This method gets files from the server to local directory This is the
     * synchronous version of copyFromFei. Called by asynchronous method on a
     * new thread.
     * @param filenames Array of filenames to be retreived
     * @param dest Destination directory of files.  If null, then local
     *             directory will be used.  If different from local dir,
     *             session will use parameter to copy files, then reset
     *             to local directory before exiting. 
     */

    protected void _getFromFei(final String[] filenames, final File dest)
    {
        if (filenames == null || filenames.length == 0)
            return;

        //set flag indicating if we are using temp dir or local dir
        final boolean useLocalDir = (dest == null || 
                      this._sessionModel.getLocalDirectory().equals(dest));
        
        //set busy flag and print message
        final boolean wasBusy = _isBusy;
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Retrieving " + filenames.length
                        + " files from FEI.  Please wait...");
            }
        });

        //init this flag to the value of the member flag
        boolean replaceRestFlag = this._sessionModel.isFileReplaceEnabled();
        boolean versionRestFlag = this._sessionModel.isFileVersionEnabled();
        boolean skipRestFlag    = false;

        //status information
        boolean cont = true;
        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        SavannahTransferRecord record = null;
        final long[] fileLen = new long[] {SavannahTransferRecord.NULL_SIZE};        
        final String[] fileChecksum = new String[] {null};
        
        //change session directory silently to destination
        try {
            if (!useLocalDir)
                changeDirectory(dest);
        } catch (SessionException sesEx) {
            this._logger.error(sesEx.getMessage());
        }
        
        //iterate over filenames and call get()...
        for (int i = 0; i < filenames.length && cont; ++i)
        {
            final String filename  = filenames[i];
            final int    fileIndex = i;
            if (filename == null)
                continue;
            boolean currentCont = true;
            int userReply = 0;
            boolean replaceCurrentFile = replaceRestFlag;
            boolean versionCurrentFile = versionRestFlag;
           
            
            //loop over the same file to handle replace/versioning
            while (currentCont)
            {
                //disable immediately, enable later if necessary
                currentCont = false;

                //set status message
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Copying " + filename +
                                         ". Please wait...");
                    }
                });

                try
                {
                    synchronized (_syncLock)
                    {
                        //set replacement policy
                        _session.setOption(Constants.FILEREPLACE,
                                           replaceCurrentFile);
                        _session.setOption(Constants.FILEVERSION,
                                           versionCurrentFile);
                        final long sTime = System.currentTimeMillis();

                        //call get() and wait for result
                        final int xactId = this._fileType.get(
                                     new String[] { filename });
                        
                        record = new SavannahTransferRecord(filename,
                                _currentFeiType, xactId,
                                SavannahTransferRecord.NULL_SIZE,
                                SavannahTransferRecord.TRANSACTION_TYPE_GET);

                        //add record to model
                        final SavannahTransferRecord fRecord = record;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                _transferModel.addTransferRecord(fRecord);
                                fRecord.setStartTime(sTime);
                                fRecord.setState(SavannahTransferRecord.
                                                 STATE_TRANSFERRING);
                            }
                        });

                        while (this._session.getTransactionCount() > 0)
                        {
                            Result r = this._session.result();
                            if (r == null)
                            {
                                Thread.yield();
                                continue;
                            }
                            statusValue = r.getErrno();
                            statusMesg = r.getMessage();
                            if (statusValue == Constants.OK)
                            {
                                fileLen[0] = r.getSize();                                  
                            }
                          //will need checksum in case of diff
                            if (statusValue == Constants.FILE_EXISTS ||
                                statusValue == Constants.FILEALREADYEXISTS)
                            {
                                fileChecksum[0] = r.getChecksumStr();                                
                            }
                            r.commit();
                        }
                    }
                } catch (SessionException sesEx) {
                    _logger.error(sesEx.getMessage(), sesEx); 
                    statusValue = sesEx.getErrno();
                    statusMesg = sesEx.getMessage();
                } catch (Exception ex) {
                    _logger.error(ex.getMessage(), ex); 
                    statusValue = Constants.EXCEPTION;
                    statusMesg = ex.getMessage();
                } finally {

                    final int fStatVal = statusValue;
                    final String fStatMsg = statusMesg;
                    final SavannahTransferRecord fRecord = record;
                    final String fPath = (new File(this._session.getDir(),
                                             filename)).getAbsolutePath();
                    
                    if (fStatVal == Constants.OK)
                    {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                setStatusMessage("Retrieved " + filename + ".");
                                fRecord.setEndTime(System.currentTimeMillis());
                                fRecord.setState(SavannahTransferRecord.
                                                 STATE_COMPLETE);
                                fRecord.setFileSize(fileLen[0]);
                                printDebug("MODEL::FEI GET done.");
                                _receivalModel.addToLocal(fPath);
                                
                                if (shouldRequestRefresh(fileIndex, filenames.length))
                                {
                                    requestRefresh(TARGET_LOCAL);
                                }
                                
                            }
                        });
                    }
                    else if (fStatVal == Constants.FILEALREADYEXISTS ||
                             fStatVal == Constants.FILE_EXISTS)
                    {
                        //user may want to ignore all existing files
                        boolean skipExisitingFileEnabled = skipRestFlag;
                        
                        
                        boolean sessionFileReplaceEnabled;
                        boolean sessionFileVersionEnabled;
                        boolean sessionFileDiffEnabled;
                       
                        synchronized (_syncLock) 
                        {
                            sessionFileReplaceEnabled = _session
                                    .getOption(Constants.FILEREPLACE);
                            sessionFileVersionEnabled = _session
                                    .getOption(Constants.FILEVERSION);
                            sessionFileDiffEnabled = _session
                                    .getOption(Constants.DIFF);
                        }
                        
                        boolean diffSaysEqual = false;
                        
                        //check if checksums are actually the same
                        if (sessionFileDiffEnabled)
                        {
                            diffSaysEqual = true;
                            String remoteCrc = fileChecksum[0];
                            
                            if (remoteCrc == null)
                                diffSaysEqual = false;
                            else
                            {
                                String localCrc  = null;
                                try {
                                    localCrc = FileUtil.getStringChecksum(fPath);
                                } catch (IOException ioEx) {
                                    localCrc = null;
                                }
                                
                                if (localCrc == null || !remoteCrc.equals(localCrc))
                                    diffSaysEqual = false;                                
                            }                            
                        }
                        
                        if (diffSaysEqual)
                        {         
                            //if there are remaining files, give user option of
                            //cancelling transfer.  Otherwise, just notify them
                            //and be done with it.
                            
                            final Object[] buttons = (fileIndex == filenames.length - 1) ?
                                    new Object[] { "OK" } : new Object[] {  "OK",  "Cancel Remaining" };                                  
                            final Object defaultButton = buttons[0];
                
                            RunnableWithUserReply rwa = new AbstractRunnableWithUserReply() {
                                public void run()
                                {
                                    setStatusMessage("Identical file '" + filename
                                            + "' aborted.");
                                    this._userReply = JOptionPane.showOptionDialog(
                                            _relativeComponent, "Retrieval of \'" +
                                            filename + "\' " +
                                            "aborted.\nFiles exist and are identical with "+
                                            "diff enabled.",
                                            "Files Identical",
                                            JOptionPane.DEFAULT_OPTION,
                                            JOptionPane.WARNING_MESSAGE, null, buttons,
                                            defaultButton);
                                }
                            };

                            try {
                                SwingUtilities.invokeAndWait(rwa);
                                userReply = rwa.getUserReply();
                            } catch (Exception e) {
                                _logger.error(e.getMessage(), e); //e.stackTrace();
                                userReply = JOptionPane.CLOSED_OPTION;
                            }

                            if (userReply == 1) 
                            {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        setStatusMessage("Aborted "
                                                + "remaining transfer.");
                                    }
                                });
                                cont = false;
                            }
                            
                            
//                            try {
//                                SwingUtilities.invokeAndWait(new Runnable() {
//                                    public void run()
//                                    {
//                                        fRecord.setState(SavannahTransferRecord.
//                                                         STATE_ABORTED);
//                                        setStatusMessage("Identical file '" + filename
//                                                         + "' aborted.");
//                                        JOptionPane.showMessageDialog(
//                                                _relativeComponent,
//                                                "Retrieval of \'" + filename + "\' " +
//                                                "aborted.\nFiles exist and are identical with "+
//                                                "diff enabled.", 
//                                                "Files Identical",
//                                                JOptionPane.INFORMATION_MESSAGE);
//                                    }
//                                });
//                            } catch (Exception ex) {
//                                this._logger.error("Error occurred while showing dialog", ex);
//                            }
                            
                        }
                        else if (!(sessionFileReplaceEnabled || 
                                   sessionFileVersionEnabled ))
                        {
                            //user already said they do not want to be
                            //bother about exising file messages, so 
                            //if skip is enabled, just move on...
                            if (skipExisitingFileEnabled)
                            {
                                //if no then abort this file
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug("Aborting file " + filename);
                                        fRecord.setState(SavannahTransferRecord.
                                                         STATE_ABORTED);
                                        setStatusMessage("'Retrieve "
                                                + filename + "' aborted. File exists.");
                                    }
                                });
                                continue;
                            }
                            
                            //ask if they wanna turn on replace for this or all
                            final Object[] buttons = new Object[]  { 
                                                "Cancel",      //0
                                                "Skip All",    //1
                                                "Skip",        //2
                                                "Version All", //3
                                                "Version",     //4
                                                "Replace All", //5
                                                "Replace" };   //6
                            final Object defaultButton = buttons[5];
                            

                            RunnableWithUserReply rwa = new AbstractRunnableWithUserReply() {
                                public void run()
                                {
                                    this._userReply = 
                                        JOptionPane.showOptionDialog(
                                            _relativeComponent,
                                            "File '"+ filename +
                                            "' already exists.\n" +
                                            "Replace, version or skip file?",
                                            "Replace/Version/Skip Option",
                                            JOptionPane.DEFAULT_OPTION,
                                            JOptionPane.WARNING_MESSAGE,
                                            null, buttons, defaultButton);
                                }
                            };

                            try {
                                SwingUtilities.invokeAndWait(rwa);
                                userReply = rwa.getUserReply();
                            } catch (Exception e) {
                                _logger.error(e.getMessage(), e);
                                userReply = JOptionPane.CLOSED_OPTION;
                            }

                            if (userReply == JOptionPane.CLOSED_OPTION ||
                                userReply < 0 || userReply >= buttons.length ||
                                userReply == 2) //Skip
                            {
                                //if no then abort this file
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug("Aborting file " + filename);
                                        fRecord.setState(SavannahTransferRecord.
                                                         STATE_ABORTED);
                                        setStatusMessage("'Retrieve "
                                                + filename + "' aborted.");
                                    }
                                });
                            }
                            else if (userReply == 1) //Skip All
                            {
                                //if yes, try this again...but only once!!
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug(__classname
                                                + "::Skipping remaining existing files"
                                                + " ");
                                        fRecord.setState(SavannahTransferRecord.
                                                         STATE_ABORTED);
                                        _transferModel.removeTransferRecord(
                                                                    fRecord);
                                        setStatusMessage("Will ignore remaining existing files.");
                                    }
                                });
                                skipRestFlag = true;                                
                            }
                            else if (userReply == 6) //Replace
                            {
                                //if yes, try this again...but only once!!
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug(__classname
                                                + "::Enabling file"
                                                + " replacement for "
                                                + filename);
                                        fRecord.setState(SavannahTransferRecord.
                                                         STATE_ABORTED);
                                        _transferModel.removeTransferRecord(
                                                                    fRecord);
                                        setStatusMessage("Enabling file "
                                                + "replacement for '"
                                                + filename + "'.");
                                    }
                                });
                                replaceCurrentFile = true;
                                currentCont = true;
                            }
                            else if (userReply == 5)  //Replace All
                            {
                                //if yes to all, change the restFlag to true,
                                //start with current
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug(__classname
                                                + "::Enabling file"
                                                + " replacement for "
                                                + filename);
                                        fRecord
                                                .setState(SavannahTransferRecord.STATE_ABORTED);
                                        _transferModel
                                                .removeTransferRecord(fRecord);
                                        setStatusMessage("Enabling file "
                                                + "replacement for remaining "
                                                + "files");
                                    }
                                });
                                replaceCurrentFile = true;
                                replaceRestFlag = true;
                                currentCont = true;
                            }
                            else if (userReply == 4) //Version
                            {
                                //if yes, try this again...but only once!!
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug(__classname
                                                + "::Enabling file"
                                                + " versioning for "
                                                + filename);
                                        fRecord.setState(SavannahTransferRecord.
                                                         STATE_ABORTED);
                                        _transferModel.removeTransferRecord(
                                                                    fRecord);
                                        setStatusMessage("Enabling file "
                                                + "versioning for '"
                                                + filename + "'.");
                                    }
                                });
                                versionCurrentFile = true;
                                currentCont = true;
                            }
                            else if (userReply == 3)  //Version All
                            {
                                //if yes to all, change the restFlag to true,
                                //start with current
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug(__classname
                                                + "::Enabling file"
                                                + " versioing for remaining "
                                                + "files");
                                        fRecord.setState(SavannahTransferRecord.STATE_ABORTED);
                                        _transferModel.removeTransferRecord(fRecord);
                                        setStatusMessage("Enabling file "
                                                + "versioing for remaining "
                                                + "files");
                                    }
                                });
                                versionCurrentFile = true;
                                versionRestFlag = true;
                                currentCont = true;
                            }
                            else if (userReply == 0)
                            {
                                RunnableWithUserReply rwa2 = new AbstractRunnableWithUserReply() {
                                    public void run()
                                    {
                                       fRecord.setState(SavannahTransferRecord.
                                                        STATE_ABORTED);
                                        this._userReply = 
                                            JOptionPane.showConfirmDialog(
                                                     _relativeComponent,
                                                     "Cancel transferring " +
                                                     "remaining files?",
                                                     "Cancel Transfer",
                                                     JOptionPane.YES_NO_OPTION);
                                    }
                                };

                                try {
                                    SwingUtilities.invokeAndWait(rwa2);
                                    userReply = rwa2.getUserReply();
                                } catch (Exception e) {
                                    _logger.error(e.getMessage(), e);
                                    userReply = JOptionPane.NO_OPTION;
                                }

                                //if yes, then set cont flag to false.
                                if (userReply == JOptionPane.YES_OPTION)
                                {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run()
                                        {
                                            setStatusMessage("Aborted "
                                                    + "remaining transfer.");
                                        }
                                    });
                                    cont = false;
                                }
                            }
                        }
                        else
                        {
                          try{
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run()
                                {
                                    fRecord.setState(SavannahTransferRecord.
                                                     STATE_ERROR);
                                    setStatusMessage("'Retrieve " + filename
                                            + "' aborted.");
                                    JOptionPane.showMessageDialog(
                                             _relativeComponent,
                                             "Received FILE_EXISTS error" +
                                             " while attempting to replace" +
                                             "\n'" + filename + "'.\n\n" +
                                             "Aborting transfer of this file.",
                                             "Retrieval Error",
                                             JOptionPane.ERROR_MESSAGE);
                                }
                            });
                          } catch (Exception ex) { 
                              _logger.error("Error occurred while showing dialog", ex);
                          }
                        }
                    }
                    else
                    {
                      try{
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run()
                            {
                                fRecord.setState(SavannahTransferRecord.
                                                 STATE_ERROR);
                                setStatusMessage("'Retrieve " + filename
                                        + "' aborted.");
                                JOptionPane.showMessageDialog(
                                        _relativeComponent,
                                        "Error occurred while copying '" +
                                        filename + "'\nfrom FEI.\n\n" +
                                        "ERROR DETAILS\n-MESSAGE: " +
                                        fStatMsg + "\n-CODE: " +
                                        fStatVal, "Retrieval Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                      } catch (Exception ex) { 
                          _logger.error("Error occurred while showing dialog", ex);
                      }
                    }
                } //end_finally
            } //end_while
        } //end_for_each_filename

        
        
        
        //reset session file replace/version policy to application setting
        synchronized (_syncLock)
        {
            _session.setOption(Constants.FILEREPLACE, 
                               this._sessionModel.isFileReplaceEnabled());
            _session.setOption(Constants.FILEVERSION, 
                               this._sessionModel.isFileVersionEnabled());
        }

        //reset session local dir to model's local dir
        try {
            if (!useLocalDir)
                changeDirectory(this._sessionModel.getLocalDirectory());
        } catch (SessionException sesEx) {
            this._logger.error(sesEx.getMessage());
        }
        
        //set busy flag to false, since we're done
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }
    
    
    protected void _getFromFeiOriginal(final String[] filenames, final File dest)
    {
        if (filenames == null || filenames.length == 0)
            return;

        //set flag indicating if we are using temp dir or local dir
        final boolean useLocalDir = (dest == null || 
                      this._sessionModel.getLocalDirectory().equals(dest));
        
        //set busy flag and print message
        final boolean wasBusy = _isBusy;
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Retrieving " + filenames.length
                        + " files from FEI.  Please wait...");
            }
        });

        //init this flag to the value of the member flag
        boolean replaceRestFlag = this._sessionModel.isFileReplaceEnabled();
        boolean versionRestFlag = this._sessionModel.isFileVersionEnabled();

        //status information
        boolean cont = true;
        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        SavannahTransferRecord record = null;
        final long[] fileLen = new long[] {SavannahTransferRecord.NULL_SIZE};      
        final String[] fileChecksum = new String[] {null};
        
        //change session directory silently to destination
        try {
            if (!useLocalDir)
                changeDirectory(dest);
        } catch (SessionException sesEx) {
            this._logger.error(sesEx.getMessage());
        }
        
        //iterate over filenames and call get()...
        for (int i = 0; i < filenames.length && cont; ++i)
        {
            final String filename = filenames[i];
            if (filename == null)
                continue;
            boolean currentCont = true;
            int userReply = 0;
            boolean replaceCurrentFile = replaceRestFlag;
            boolean versionCurrentFile = versionRestFlag;

            //loop over the same file to handle replace/versioning
            while (currentCont)
            {
                //disable immediately, enable later if necessary
                currentCont = false;

                //set status message
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Copying " + filename +
                                         ". Please wait...");
                    }
                });

                try
                {
                    synchronized (_syncLock)
                    {
                        //set replacement policy
                        _session.setOption(Constants.FILEREPLACE,
                                           replaceCurrentFile);
                        _session.setOption(Constants.FILEVERSION,
                                           versionCurrentFile);
                        final long sTime = System.currentTimeMillis();

                        //call get() and wait for result
                        final int xactId = this._fileType.get(
                                     new String[] { filename });
                        
                        record = new SavannahTransferRecord(filename,
                                _currentFeiType, xactId,
                                SavannahTransferRecord.NULL_SIZE,
                                SavannahTransferRecord.TRANSACTION_TYPE_GET);

                        //add record to model
                        final SavannahTransferRecord fRecord = record;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                _transferModel.addTransferRecord(fRecord);
                                fRecord.setStartTime(sTime);
                                fRecord.setState(SavannahTransferRecord.
                                                 STATE_TRANSFERRING);
                            }
                        });

                        while (this._session.getTransactionCount() > 0)
                        {
                            Result r = this._session.result();
                            if (r == null)
                            {
                                Thread.yield();
                                continue;
                            }
                            statusValue     = r.getErrno();
                            statusMesg      = r.getMessage();
                            
                            //will need filesize for success
                            if (statusValue == Constants.OK)
                                fileLen[0] = r.getSize();  
                            
                            //will need checksum in case of diff
                            if (statusValue == Constants.FILE_EXISTS ||
                                statusValue == Constants.FILEALREADYEXISTS)
                                fileChecksum[0] = r.getChecksumStr();
                        }
                    }
                } catch (SessionException sesEx) {
                    _logger.error(sesEx.getMessage(), sesEx); 
                    statusValue = sesEx.getErrno();
                    statusMesg = sesEx.getMessage();
                } catch (Exception ex) {
                    _logger.error(ex.getMessage(), ex); 
                    statusValue = Constants.EXCEPTION;
                    statusMesg = ex.getMessage();
                } finally {

                    final int fStatVal = statusValue;
                    final String fStatMsg = statusMesg;
                    final SavannahTransferRecord fRecord = record;
                    final String fPath = (new File(this._session.getDir(),
                                             filename)).getAbsolutePath();
                    
                    if (fStatVal == Constants.OK)
                    {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run()
                            {
                                setStatusMessage("Retrieved " + filename + ".");
                                fRecord.setEndTime(System.currentTimeMillis());
                                fRecord.setState(SavannahTransferRecord.
                                                 STATE_COMPLETE);
                                fRecord.setFileSize(fileLen[0]);
                                printDebug("MODEL::FEI GET done.");
                                _receivalModel.addToLocal(fPath);
                                requestRefresh(TARGET_LOCAL);
                            }
                        });
                    }
                    else if (fStatVal == Constants.FILEALREADYEXISTS ||
                             fStatVal == Constants.FILE_EXISTS)
                    {
                        boolean sessionFileReplaceEnabled;
                        synchronized (_syncLock) 
                        {
                            sessionFileReplaceEnabled = _session
                                    .getOption(Constants.FILEREPLACE);
                        }

                        if (!sessionFileReplaceEnabled)
                        {
                            //ask if they wanna turn on replace for this or all
                            final Object[] buttons = new Object[] { "Cancel",
                                                 "No", "Yes to All", "Yes" };

                            RunnableWithUserReply rwa = new AbstractRunnableWithUserReply() {
                                public void run()
                                {
                                    this._userReply = 
                                        JOptionPane.showOptionDialog(
                                            _relativeComponent,
                                            "File '"+ filename +
                                            "' already exists.\n" +
                                            "Replace file?",
                                            "Replace Option",
                                            JOptionPane.DEFAULT_OPTION,
                                            JOptionPane.WARNING_MESSAGE,
                                            null, buttons, buttons[3]);
                                }
                            };

                            try {
                                SwingUtilities.invokeAndWait(rwa);
                                userReply = rwa.getUserReply();
                            } catch (Exception e) {
                                _logger.error(e.getMessage(), e);
                                userReply = JOptionPane.CLOSED_OPTION;
                            }

                            if (userReply == JOptionPane.CLOSED_OPTION ||
                                userReply < 0 || userReply >= buttons.length ||
                                userReply == 1)
                            {
                                //if no then abort this file
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug("Aborting file " + filename);
                                        fRecord.setState(SavannahTransferRecord.
                                                         STATE_ABORTED);
                                        setStatusMessage("'Retrieve "
                                                + filename + "' aborted.");
                                    }
                                });
                            }
                            else if (userReply == 3)
                            {
                                //if yes, try this again...but only once!!
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug(__classname
                                                + "::Enabling file"
                                                + " replacement for "
                                                + filename);
                                        fRecord.setState(SavannahTransferRecord.
                                                         STATE_ABORTED);
                                        _transferModel.removeTransferRecord(
                                                                    fRecord);
                                        setStatusMessage("Enabling file "
                                                + "replacement for '"
                                                + filename + "'.");
                                    }
                                });
                                replaceCurrentFile = true;
                                currentCont = true;
                            }
                            else if (userReply == 2)
                            {
                                //if yes to all, change the restFlag to true,
                                //start with current
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run()
                                    {
                                        printDebug(__classname
                                                + "::Enabling file"
                                                + " replacement for "
                                                + filename);
                                        fRecord
                                                .setState(SavannahTransferRecord.STATE_ABORTED);
                                        _transferModel
                                                .removeTransferRecord(fRecord);
                                        setStatusMessage("Enabling file "
                                                + "replacement for remaining "
                                                + "files");
                                    }
                                });
                                replaceCurrentFile = true;
                                replaceRestFlag = true;
                                currentCont = true;
                            }
                            else if (userReply == 0)
                            {
                                RunnableWithUserReply rwa2 = new AbstractRunnableWithUserReply() {
                                    public void run()
                                    {
                                       fRecord.setState(SavannahTransferRecord.
                                                        STATE_ABORTED);
                                        this._userReply = 
                                            JOptionPane.showConfirmDialog(
                                                     _relativeComponent,
                                                     "Cancel transferring " +
                                                     "remaining files?",
                                                     "Cancel Transfer",
                                                     JOptionPane.YES_NO_OPTION);
                                    }
                                };

                                try {
                                    SwingUtilities.invokeAndWait(rwa2);
                                    userReply = rwa2.getUserReply();
                                } catch (Exception e) {
                                    _logger.error(e.getMessage(), e);
                                    userReply = JOptionPane.NO_OPTION;
                                }

                                //if yes, then set cont flag to false.
                                if (userReply == JOptionPane.YES_OPTION)
                                {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run()
                                        {
                                            setStatusMessage("Aborted "
                                                    + "remaining transfer.");
                                        }
                                    });
                                    cont = false;
                                }
                            }
                        }
                        else
                        {
                          try{
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run()
                                {
                                    fRecord.setState(SavannahTransferRecord.
                                                     STATE_ERROR);
                                    setStatusMessage("'Retrieve " + filename
                                            + "' aborted.");
                                    JOptionPane.showMessageDialog(
                                             _relativeComponent,
                                             "Received FILE_EXISTS error" +
                                             " while attempting to replace" +
                                             "\n'" + filename + "'.\n\n" +
                                             "Aborting transfer of this file.",
                                             "Retrieval Error",
                                             JOptionPane.ERROR_MESSAGE);
                                }
                            });
                          } catch (Exception ex) { 
                              _logger.error("Error occurred while showing dialog", ex);
                          }
                        }
                    }
                    else
                    {
                      try{
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run()
                            {
                                fRecord.setState(SavannahTransferRecord.
                                                 STATE_ERROR);
                                setStatusMessage("'Retrieve " + filename
                                        + "' aborted.");
                                JOptionPane.showMessageDialog(
                                        _relativeComponent,
                                        "Error occurred while copying '" +
                                        filename + "'\nfrom FEI.\n\n" +
                                        "ERROR DETAILS\n-MESSAGE: " +
                                        fStatMsg + "\n-CODE: " +
                                        fStatVal, "Retrieval Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        });
                      } catch (Exception ex) { 
                          _logger.error("Error occurred while showing dialog", ex);
                      }
                    }
                } //end_finally
            } //end_while
        } //end_for_each_filename

        
        
        
        //reset session file replace/version policy to application setting
        synchronized (_syncLock)
        {
            _session.setOption(Constants.FILEREPLACE, 
                               this._sessionModel.isFileReplaceEnabled());
            _session.setOption(Constants.FILEVERSION, 
                               this._sessionModel.isFileVersionEnabled());
        }

        //reset session local dir to model's local dir
        try {
            if (!useLocalDir)
                changeDirectory(this._sessionModel.getLocalDirectory());
        } catch (SessionException sesEx) {
            this._logger.error(sesEx.getMessage());
        }
        
        //set busy flag to false, since we're done
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }
    //---------------------------------------------------------------------

    /**
     * This method deletes files from current file type.
     * @param filenames Array of filenames to be deleted
     */

    public void deleteFromFei(final String[] filenames)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _deleteFromFei(filenames);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
    }

  //---------------------------------------------------------------------

    /**
     * This method deletes files from current file type.
     * @param filenames Array of File instances to be deleted
     */

    public void deleteFromLocal(final File[] files)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _deleteFromLocal(files);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
    }
    
    //---------------------------------------------------------------------

    /**
     * This method deletes files from current file type. This is the synchronous
     * version of deleteFromFei. Called by asynchronous method on a new thread.
     * @param filenames Array of filenames to be deleted
     */

    protected void _deleteFromFei(final String[] filenames)
    {
        if (filenames == null || filenames.length == 0)
            return;

        String currentFilename = null;
        final boolean wasBusy = _isBusy;
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Deleting " + filenames.length +
                                 " files from FEI.  Please wait...");
            }
        });

        boolean cont = true;
        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        for (int i = 0; i < filenames.length && cont; ++i)
        {
            final int fileIndex = i;
            final String filename = filenames[i];
            if (filename == null)
                continue;

            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    setStatusMessage("Deleting " + filename +
                                     ". Please wait...");
                }
            });

            try
            {
                synchronized (_syncLock)
                {
                    int xactId = this._fileType.delete(filename);
                    while (this._session.getTransactionCount() > 0)
                    {
                        Result r = this._session.result();
                        if (r == null)
                        {
                            Thread.yield();
                            continue;
                        }
                        statusValue = r.getErrno();
                    }
                }
            } catch (SessionException sesEx) {
                _logger.error(sesEx.getMessage(), sesEx);
                statusValue = sesEx.getErrno();
                statusMesg = sesEx.getMessage();
            } catch (Exception ex) {
                _logger.error(ex.getMessage(), ex);
                statusValue = Constants.EXCEPTION;
                statusMesg = ex.getMessage();
            } finally {
                final int fStatVal = statusValue;
                final String fStatMsg = statusMesg;
                if (fStatVal == Constants.OK)
                {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            setStatusMessage("Deleted " + filename + ".");
                            if (shouldRequestRefresh(fileIndex, filenames.length))
                            {
                                requestRefresh(TARGET_FEI);
                            }
                        }
                    });
                }
                else
                {
                  try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run()
                        {
                            setStatusMessage("'Delete " + filename
                                    + "' aborted.");
                            JOptionPane.showMessageDialog(_relativeComponent,
                                    "Error occurred while " +
                                    "attempting to delete '" +
                                    filename + "'\n from FEI" +
                                    "\n\nERROR DETAILS\n" +
                                    "-MESSAGE: " + fStatMsg +
                                    "\n-CODE: " + fStatVal,
                                    "Delete Error", 
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                  } catch (Exception ex) { 
                      _logger.error("Error occurred while showing dialog", ex);
                  }
                }
            } //end_finally
        } //end_for

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }
    
    //---------------------------------------------------------------------

    /**
     * This method deletes files from current directory. This is the synchronous
     * version of deleteFromLocal. Called by asynchronous method on a new thread.
     * @param filenames Array of files to be deleted
     */

    protected void _deleteFromLocal(final File[] files)
    {
        
        
        
        if (files == null || files.length == 0)
            return;
        
        File file;
        final boolean wasBusy = _isBusy;
        
        for (int i = 0; i < files.length; ++i)
        {
            file = (File) files[i];
            final int fileIndex = i;
            final String filename = file.getName();
            
            if (file.isDirectory())
            {
              try{
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        setBusyFlag(true);
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Cannot remove "+filename+
                                "\nDirectories cannot be deleted",
                                "Delete Error", JOptionPane.ERROR_MESSAGE);
                        setStatusMessage("'Delete "+filename+"' aborted.");                         
                    }
                });       
              } catch (Exception ex) { 
                  _logger.error("Error occurred while showing dialog", ex);
              }
            }
            else if (!file.canWrite())
            {
              try{
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        setBusyFlag(true);
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Cannot remove '"+filename+
                                "'\nWrite access was denied",
                                "Delete Error", JOptionPane.ERROR_MESSAGE);
                         setStatusMessage("'Delete "+filename+"' aborted.");  
                                                 
                    }
                });
              } catch (Exception ex) { 
                  _logger.error("Error occurred while showing dialog", ex);
              }
                
            }
            else
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setBusyFlag(true);
                        setStatusMessage("Deleting "+filename+"...");                                                 
                    }
                });
                
            
                try {
                    
                    file.delete();  
                    
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            setStatusMessage("Deleted "+filename);
                            if (shouldRequestRefresh(fileIndex, files.length))
                            {
                                requestRefresh(TARGET_LOCAL);
                            }
                        }
                    });  
                    
                } catch (Exception ex) {
                    _logger.error(ex.getMessage(), ex); //ex.printStackTrace(); 
                    final String exMesg = ex.getMessage();
                    
                  try{
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run()
                        {
                            JOptionPane.showMessageDialog(_relativeComponent,
                                    "Error occurred while attempting to delete "+filename+
                                    "\nError message: "+exMesg,
                                    "Delete Error", JOptionPane.ERROR_MESSAGE);
                        }});
                  } catch (Exception ex2) { 
                      _logger.error("Error occurred while showing dialog", ex2);
                  }
                }
            }
        } //end_for_loop
        
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
               
    }

    //---------------------------------------------------------------------

    /**
     * Returns a list fo the files present in the current file type. NOTE: Max
     * number of files bound by Integer.MAX_VALUE
     * @return List of files present in the current file type
     */

    public List showFromFei()
    {
        setStatusMessage("Refreshing FEI files...");
        final boolean wasBusy = _isBusy;

        List files;
        setBusyFlag(true);
  
        try
        {
            synchronized (_syncLock)
            {
                int xactId;

                SavannahListFilter filter = this._filterModel.getFilter(
                                                          "FEI_FILTER");
                
                SavannahDateFilterModel dateModel = this.getDateFilterModel();
                DateFilter dateFilter = dateModel.getActive();
                

                
                String regexFilter = (this._currentFeiType != null && filter.isEnabled() &&
                                      !filter.getPattern().equals("")) ? 
                                      filter.getPattern() : null;
                
//                //use filter only if filetype, filter on, and filter defined
//                if (regexFilter != null)
//                    xactId = this._fileType.show(regexFilter);
//                else
//                    xactId = this._fileType.show();
                
                xactId = this.performShow(filter, dateFilter);               
                
                files = new Vector();
                while (_session.getTransactionCount() > 0)
                {
                    Result r = _session.result();
                    if (r == null)
                    {
                        continue;
                    }
                    // Get the return code for each operation
                    if (r.getErrno() == Constants.OK)
                    {
                        // Check to see if a name exists
                        if (r.getName() != null && !r.getName().equals(""))
                            // Add the name to the results vector
                            files.add(r);
                    }
                    else if (r.getErrno() == Constants.DENIED)
                    {
                        _logger.error("Could not list FEI files.  Reason: " +
                                       r.getMessage(), null);
                        setStatusMessage("Filetype operation access denied.");
                        JOptionPane.showMessageDialog(this._relativeComponent,
                                "Read access for this filetype is denied.\n\n" +
                                "While you are still connected to the " +
                                "filetype, no \nfiles will be listed.\n",
                                "Access Denied", JOptionPane.ERROR_MESSAGE);                    
                        files = null;
                    }
                }
                
                if (files != null)
                    setStatusMessage("Refreshed FEI file listing.");
                else
                    files = new Vector();
            }
        } catch (SessionException sEx) {
            _logger.error(sEx.getMessage(), sEx); //sEx.stackTrace();
            setStatusMessage("FEI session error occurred.");
            files = new Vector();
        } catch (Exception ex) {
            _logger.error(ex.getMessage(), ex); //ex.stackTrace();
            setStatusMessage("FEI retrieval error occurred.");
            files = new Vector();
        }

        setBusyFlag(wasBusy);
        return files;
    }

    //---------------------------------------------------------------------

    protected int performShow(SavannahListFilter filter, DateFilter dateFilter)
                                                       throws SessionException
    {
        int xactId = -1;
        
        String regex  = null;
        Date   before = null;
        Date   after  = null;
        
        if  (this._currentFeiType != null && filter.isEnabled() &&
                                 !filter.getPattern().equals(""))
        {
            regex = filter.getPattern();
        }
        else
        {
            regex = "*";    
        }
        
        
        if (dateFilter != null && dateFilter.getMode() != DateFilter.MODE_OFF)
        {
            final int dateMode = dateFilter.getMode();
            switch(dateMode)
            {
                case DateFilter.MODE_OFFSET:
                    after = new Date(System.currentTimeMillis() - 
                                     dateFilter.getOffset());
                    break;
                case DateFilter.MODE_AFTER:
                    after = new Date(dateFilter.getDate());                            
                    break;
//                case DateFilter.MODE_BEFORE:
//                    before = new Date(dateFilter.getDate());                            
//                    break;
                case DateFilter.MODE_BETWEEN:
                    after  = new Date(dateFilter.getDate());
                    before = new Date(dateFilter.getEndDate());   
                    if (after.after(before))
                    {
                        Date temp = after;
                        after = before;
                        before = temp;
                    }
                    break;                            
            }
        }
        
       
        if (after != null && before != null)
        {
            xactId = this._fileType.showBetween(after, before, regex);
        }
        else if (after == null && before != null)
        {
//            xactId = this._fileType.showBefore(before, regex);
            xactId = -1;
            _logger.warn("FEI operation showBefore() is not supported!");
        }
        else if (after != null && before == null)
        {
            xactId = this._fileType.showAfter(after, regex);
        }        
        else 
        {
            xactId = this._fileType.show(regex);
        }
            
        return xactId;
    }

    //---------------------------------------------------------------------

    /**
     * Renames an entry in FEI to a new name.
     * @param origName Original name of the entry
     * @param newName New name of the entry
     */

    public void renameInFei(final String origName, final String newName)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _renameInFei(origName, newName);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
    }

    //---------------------------------------------------------------------

    /**
     * Renames an entry in FEI to a new name. This is the synchronous version of
     * renameInFei. Called by asynchronous method on a new thread.
     * @param origName Original name of the entry
     * @param newName New name of the entry
     */

    protected void _renameInFei(final String origName, final String newName)
    {
        if (origName == null || newName == null)
            return;

        final boolean wasBusy = _isBusy;

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Renaming file.  Please wait...");
            }
        });

        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        try
        {
            synchronized (_syncLock)
            {
                int xactId = this._fileType.rename(origName, newName);
                while (this._session.getTransactionCount() > 0)
                {
                    Result r = this._session.result();
                    if (r == null)
                    {
                        Thread.yield();
                        continue;
                    }
                    statusValue = r.getErrno();
                    statusMesg = r.getMessage();
                }
            }
        } catch (SessionException sesEx)
        {
            _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
            statusValue = sesEx.getErrno();
            statusMesg = sesEx.getMessage();
        } catch (Exception ex)
        {
            _logger.error(ex.getMessage(), ex); //ex.stackTrace();
            statusValue = Constants.EXCEPTION;
            statusMesg = ex.getMessage();
        } finally
        {
            final int fStatVal = statusValue;
            final String fStatMsg = statusMesg;

            if (fStatVal == Constants.OK)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Renamed to " + newName + ".");
                        requestRefresh(TARGET_FEI);
                    }
                });
            }
            else
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Unable to rename " + origName + ".");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Error occurred while attempting to "
                                        + "rename '" + origName + "' to '"
                                        + newName + "'" + "\n\nERROR DETAILS\n"
                                        + "-MESSAGE: " + fStatMsg + "\n-CODE: "
                                        + fStatVal, "Rename Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }

    //---------------------------------------------------------------------

    /**
     * Sets or removes comment for an entry in FEI.
     * @param filename Filename of the entry
     * @param comment New comment of the entry, null resets comment.
     */

    public void commentInFei(final String filename, final String comment)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _commentInFei(filename, comment);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
    }

    //---------------------------------------------------------------------

    /**
     * Sets or removes comment for an entry in FEI. This is the synchronous
     * version of commentInFei. Called by asynchronous method on a new thread.
     * @param fileame Filename of the entry
     * @param Comment New comment of the entry, null resets comment.
     */

    protected void _commentInFei(final String filename, final String comment)
    {
        if (filename == null)
            return;

        final boolean wasBusy = _isBusy;

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Commenting file.  Please wait...");
            }
        });

        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        try
        {
            synchronized (_syncLock)
            {
                int xactId;
                if (comment != null)
                    xactId = this._fileType.comment(filename, comment);
                else
                    xactId = this._fileType.comment(filename);
                while (this._session.getTransactionCount() > 0)
                {
                    Result r = this._session.result();
                    if (r == null)
                    {
                        Thread.yield();
                        continue;
                    }
                    statusValue = r.getErrno();
                    statusMesg = r.getMessage();
                }
            }
        } catch (SessionException sesEx)
        {
            _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
            statusValue = sesEx.getErrno();
            statusMesg = sesEx.getMessage();
        } catch (Exception ex)
        {
            _logger.error(ex.getMessage(), ex); //ex.stackTrace();
            statusValue = Constants.EXCEPTION;
            statusMesg = ex.getMessage();
        } finally
        {
            final int fStatVal = statusValue;
            final String fStatMsg = statusMesg;

            if (fStatVal == Constants.OK)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Changed comment of " + filename + ".");
                        requestRefresh(TARGET_FEI);
                    }
                });
            }
            else
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Unable to change comment of "
                                + filename + ".");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Error occurred while attempting to "
                                        + "comment '" + filename + "'."
                                        + "\n\nERROR DETAILS\n" + "-MESSAGE: "
                                        + fStatMsg + "\n-CODE: " + fStatVal,
                                "Rename Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }

    //---------------------------------------------------------------------
    
    protected void _changePassword(final String servergroup,
                                   final String username, 
                                   final String oldPassword,
                                   final String newPassword)
    {
        final boolean wasBusy = _isBusy;

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Changing password.  Please wait...");
            }
        });

        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        Session tempSession = null;;
        
        try
        {
            synchronized (_syncLock)
            {
                int xactId;
                tempSession = new Session(this._domainFileURL, Constants.SSL);
                
                tempSession.setDefaultGroup(servergroup);
                tempSession.setLoginInfo(username, oldPassword);
                
                try {
                    xactId = tempSession.changePassword(oldPassword, newPassword);
                } catch (SessionException sesEx) {
                    throw sesEx;
                }
                
                Result r = tempSession.result();
                statusValue = r.getErrno();
                statusMesg  = r.getMessage();
                
//                while (this._session.getTransactionCount() > 0)
//                {
//                    Result r = this._session.result();
//                    if (r == null)
//                    {
//                        Thread.yield();
//                        continue;
//                    }
//                    statusValue = r.getErrno();
//                    statusMesg  = r.getMessage();
//                }
            }
        } catch (SessionException sesEx) {
            _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
            statusValue = sesEx.getErrno();
            statusMesg = sesEx.getMessage();
        } catch (Exception ex) {
            _logger.error(ex.getMessage(), ex); ex.printStackTrace();
            statusValue = Constants.EXCEPTION;
            statusMesg = ex.getMessage();
        } finally {
            final int fStatVal = statusValue;
            final String fStatMsg = statusMesg;

            if (fStatVal == Constants.OK)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Changed password for " + username + ".");
                    }
                });
            }
            else
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Unable to change password for "
                                + username + ".");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Error occurred while attempting to "
                                        + "change password for '" + username + "'."
                                        + "\n\nERROR DETAILS\n" + "-MESSAGE: "
                                        + fStatMsg + "\n-CODE: " + fStatVal,
                                "Change Password Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                if (tempSession != null)
                    tempSession.closeImmediate();
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });
    }
    
    //---------------------------------------------------------------------

    /**
     * Change the local directory reference by Client object.
     * @param dir The updated local directory
     */

    protected void changeDirectory(File dir) throws SessionException
    {

        synchronized (_syncLock)
        {
            if (this._session != null)
            {
                try {
                    this._session.setDirectory(dir.getAbsolutePath());
                } catch (SessionException sEx) {
                    _logger.error(sEx.getMessage(), sEx); //sEx.stackTrace();
                } catch (Exception ex)
                {
                    _logger.error(ex.getMessage(), ex); //ex.stackTrace();
                }
            }
        }
    }

    //---------------------------------------------------------------------

    /**
     * Attempts to close filetype with parameter name.
     */

    protected void closeFileType(FileType filetype)
    {
        if (filetype == null)
            return;

        String ftName = filetype.getName();
        String ftGroup = filetype.getGroup();
        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";

        synchronized (_syncLock)
        {
            boolean isConnected = false;
            try {
                isConnected = _session.isConnected(ftGroup, ftName);
            } catch (SessionException sesEx) {
                isConnected = false;
            }
            if (isConnected)
            {
                printDebug(__classname + "::closeFileType:: Closing " + ftGroup
                        + ":" + ftName);
                try {      
                    _session.close(filetype);   
                    while (_session.getTransactionCount() > 0)
                    {
                        Result r = _session.result();       
                        if (r == null)
                        {
                            Thread.yield();    
                            continue;
                        }      
                        statusValue = r.getErrno();
                        statusMesg = r.getMessage();
                    }
                } catch (SessionException sesEx) {                  
                    statusValue = sesEx.getErrno();
                    statusMesg = sesEx.getMessage();
                }

                if (statusValue != Constants.OK)
                {
                    String msg = "Error occurred while attempting to close\n"
                                 + "filetype '" + ftName + "' from session."
                                 + "\n\nERROR DETAILS\n" + "-MESSAGE: "
                                 + statusMesg + "\n-CODE: " + statusValue;
                    this._logger.error(msg);
                    JOptionPane.showMessageDialog(_relativeComponent,
                                                  msg, "Session Error", 
                                                  JOptionPane.ERROR_MESSAGE);
                }

            } //end_if_conn
        } //end_sync
    }

    //---------------------------------------------------------------------

    /**
     * Attempts to connect to filetype after closing current filetype if
     * necessary.
     * @param Filetype name, or null to reset filetype to nothing.
     */

    protected boolean connectToFileType(String type) throws SessionException
    {
        FileType newFileType;
        String currentName = null;

        synchronized (_syncLock)
        {
            if (this._fileType != null)
                currentName = this._fileType.getName();

            if (this._session == null)
                return false;

            if (type != null)
            {
                if (type.equals(""))
                    return false;
                if (type.equals(currentName))
                    return false;

                newFileType = this._session.open(this._currentFeiGroup, type);
                if (newFileType == null)
                {
                    //handle error, return
                    this._logger.error("Null filetype returned for type "
                            + type);
                    return false;
                }
                printDebug(__classname + "::New file type = "
                        + newFileType.getName());

                closeFileType(_fileType);
                this._fileType = newFileType;
                return true;
            }
            else
            {
                printDebug(__classname + "::Resetting file type = " + null);
                closeFileType(this._fileType);
                this._fileType = null;
                return true;
            }
        }
    }

    //---------------------------------------------------------------------

    /**
     * Set current status message.
     * @param statMsg Current status message.
     */

    public void setStatusMessage(String statMsg)
    {
        if (statMsg == null)
            statMsg = "";

        if (!_statusMessage.equals(statMsg))
        {
            String oldValue = _statusMessage;
            _statusMessage = statMsg;
            _changes.firePropertyChange("STATUS_MESSAGE", oldValue,
                    _statusMessage);
        }
        
        //log the status info
        _logger.info(statMsg);
    }

    //---------------------------------------------------------------------

    /**
     * Returns current status to be displayed as part of GUI.
     * @return Status message, empty string if none.
     */

    public String getStatusMessage()
    {
        return _statusMessage;
    }

    //---------------------------------------------------------------------

    /**
     * Set current local directory
     * @param localDir File pointing to new local directory
     */

    public void setLocalDirectory(File localDir)
    {
        this._sessionModel.setLocalDirectory(localDir);
    }

    //---------------------------------------------------------------------

    /**
     * Returns current status to be displayed as part of GUI.
     * @return Status message, empty string if none.
     */

    public File getLocalDirectory()
    {
        return this._sessionModel.getLocalDirectory();
    }

    //---------------------------------------------------------------------

    /**
     * Set current FEI file type
     * @param type new fiel type, null to reset
     */

    public void setCurrentFeiType(String type) throws SessionException
    {
        final boolean wasBusy = this._isBusy;
        setBusyFlag(true);
        printDebug(__classname + "::setCurrentFeiType(): " + "Type = " + type
                + ".  CurrentType = " + _currentFeiType);

        // if new type and old type are equal, then don't do anything
        if ((type == null && this._currentFeiType == null)
                || (_currentFeiType != null && _currentFeiType.equals(type)))
        {
            requestRefresh(TARGET_FEI);
            printDebug(__classname + "::setCurrentFeiType(): Exiting...");
            setBusyFlag(false);
            return;
        }

        //attempt change file type object
        boolean connChanged = false;
        try {
            connChanged = connectToFileType(type);
        } catch (SessionException sesEx) {
            setBusyFlag(wasBusy);
            throw sesEx;
        }

        if (connChanged)
        {
            String oldValue = _currentFeiType;
            this._currentFeiType = type;
            String msg = (type == null) ? "Disconnected from filetype"
                    : "Connected to filetype '" + type + "'";
            setStatusMessage(msg);

            this._changes.firePropertyChange("CURRENT_FEI_TYPE", oldValue,
                                             _currentFeiType);
        }
        else
        {
            requestRefresh(TARGET_FEI);
        }

        setBusyFlag(wasBusy);
        printDebug(__classname + "::setCurrentFeiType(): Exiting...");
    }

    //---------------------------------------------------------------------

    /**
     * Returns current FEI file type, null if none
     * @return Current file type
     */

    public String getCurrentFeiType()
    {
        return _currentFeiType;
    }

    //---------------------------------------------------------------------

    /**
     * Sets model busy flag. Indicates a transaction in progress.
     * @param isBusy True if busy, false otherwise.
     */

    public void setBusyFlag(boolean isBusy)
    {   
        if (isBusy != _isBusy)
        {
            Object oldValue = new Boolean(_isBusy);
            _isBusy = isBusy;
            _changes.firePropertyChange("IS_BUSY", oldValue, 
                                        new Boolean(_isBusy));

        }
    }

    //---------------------------------------------------------------------

    /**
     * Returns model busy flag. Indicates a transaction in progress.
     * @Return isBusy True if busy, false otherwise.
     */

    public boolean isBusy()
    {
        return _isBusy;
    }

    //---------------------------------------------------------------------

    /**
     * Assign a relative component used for placement of error/warning messages.
     * @param c Relative component to use, can be null.
     */

    public void setRelativeComponent(Component c)
    {
        if (_relativeComponent != c)
            _relativeComponent = c;
    }

    //---------------------------------------------------------------------

    /**
     * Returns reference to the relative component.
     * @return Component used for relative placement.
     */

    public Component getRelativeComponent()
    {
        return _relativeComponent;
    }

    //---------------------------------------------------------------------

    /**
     * Prints debug message to standard out if debug enabled.
     * @param msg Message to print.
     */

    public void printDebug(String msg)
    {
        _logger.debug(msg);
    }

    //---------------------------------------------------------------------
    
    /**
     * Implementation of the PropertyChangeListener interface.
     */
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        //-------------------------
        
        if (propName.equals(SavannahSessionModel.COMPUTE_CHECKSUM_ENABLED))
        {          
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.CHECKSUM, 
                                  this._sessionModel.canComputeChecksum());                
                }
            }
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.RESTART_TRANSACTION_ENABLED))
        {    
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.RESTART, 
                                  this._sessionModel.canRestart());                
                }
            }
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.RESUME_TRANSACTION_ENABLED))
        {    
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.RESUME, 
                                 _sessionModel.isResumeTransferEnabled());                
                }
            }
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.LOCAL_DIRECTORY))
        {
            File localDir = (File) pce.getNewValue();
            try {
                changeDirectory(localDir);
            } catch (Exception ex) {
                _logger.error(ex.getMessage(), ex); 
            }
            _changes.firePropertyChange(pce);
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.FILE_REPLACE_ENABLED))
        {
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.FILEREPLACE, 
                                  this._sessionModel.isFileReplaceEnabled());               
                }
            }
            _changes.firePropertyChange(pce);
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.FILE_VERSION_ENABLED))
        {
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.FILEVERSION, 
                                  this._sessionModel.isFileVersionEnabled());               
                }
            }
            _changes.firePropertyChange(pce);
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.FILE_SAFEREAD_ENABLED))
        {
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.SAFEREAD, 
                                  this._sessionModel.isSafereadEnabled());               
                }
            }
            _changes.firePropertyChange(pce);
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.FILE_DIFF_ENABLED))
        {
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.DIFF, 
                                  this._sessionModel.isDiffEnabled());               
                }
            }
            _changes.firePropertyChange(pce);
        }
        
        //-------------------------
        
        else if (propName.equals(SavannahSessionModel.FILE_RECEIPT_ENABLED))
        {
            synchronized (_syncLock)
            {
                if (this._session != null)
                {
                    this._session.setOption(Constants.RECEIPTONXFR, 
                                  this._sessionModel.isReceiptEnabled());               
                }
            }
            _changes.firePropertyChange(pce);
        }
        
        //-------------------------
        
    }


    
    //---------------------------------------------------------------------

    /**
     * Returns current FEI domain filename used for this session.
     * @return Current FEI domain filename
     */

    public URL getDomainFile()
    {
        return _domainFileURL;
    }

    //---------------------------------------------------------------------

    /**
     * Sets the domain filename according to non-null parameter. If filename is
     * different from current, then a new Session is constructed and replaces
     * the older session.
     * @param domainFilename New FEI domain file
     * @param override If domain filename is unchanged, override as true reloads
     *            current file.
     * @throws IllegalArgumentException if domainFilename is null or refers to a
     *             non-exising file.
     */

//    public void setDomainFile(String domainFilename, boolean override)
//    {
//        
//    }
    
    public void setDomainFile(URL domainFileURL, boolean override)
    {
        if (domainFileURL == null)
            throw new IllegalArgumentException(__classname
                    + "::setDomainFile():: Cannot set domain file to null.");

               
        if (override || (!domainFileURL.equals(this._domainFileURL)))
        {

            setStatusMessage("Setting domain file as " + domainFileURL);

            //reload session object
            boolean success = loadSession(domainFileURL);

            if (success)
            {
                _receivalModel.resetAll();
                setCurrentFeiGroup(null);
                loadFeiServerGroups();
                this.setPassword(null);
                Object oldValue = this._domainFileURL;
                this._domainFileURL = domainFileURL;
                
                loadAuthenticator();                
                this._changes.firePropertyChange("FEI_DOMAIN_FILE", oldValue,
                                                 this._domainFileURL);
                setStatusMessage("Domain file change complete.");
            }
            else
            {
                setStatusMessage("Domain file change aborted.");
            }
            
        }
        else
        {
            setStatusMessage("Domain file same as previous. Change aborted.");
        }
    }

    //---------------------------------------------------------------------

    /**
     * Convenience method that requests a refresh for a file list based
     * on parameter.
     * @param target Refresh taret, one of TARGET_FEI or TARGET_LOCAL.
     */
    
    public void requestRefresh(int target)
    {
        if (target == TARGET_FEI)
            _changes.firePropertyChange("FEI_LISTING", null, null);
        else if (target == TARGET_LOCAL)
            _changes.firePropertyChange("FILE_LISTING", null, null);
        else
            throw new IllegalArgumentException(__classname + "::"
                    + "requestRefresh(): Unrecognized target");
    }

    //---------------------------------------------------------------------
    
    public boolean canUserConnect(String username, 
                                  String password,
                                  String servergroup,
                                  String filetype)
                                  throws SessionException
    {        
        boolean canConnect = false;
        
        if (this._authenticator != null)
        {
            canConnect = this._authenticator.authenticate(username, 
                                                          password, 
                                                          servergroup, 
                                                          filetype);            
        }
        
        return canConnect;        
    }
    
    //---------------------------------------------------------------------
    
    public boolean canUserConnectNoEx(String username, 
                                      String password,
                                      String servergroup,
                                      String filetype)
    {
        
        boolean canConnect = false;
        
        try {        
            canConnect = canUserConnect(username, 
                                        password, 
                                        servergroup, 
                                        filetype);
        } catch (SessionException sesEx) {
            this._logger.error("Error occurred while attempting to "+
                               "authenticate user '"+username+"'.\n" +
                               "Message = " + sesEx.getMessage());
            this._logger.debug(null, sesEx);
            canConnect = false;
        }
        
        return canConnect;        
    }
    
    //---------------------------------------------------------------------
    
    public boolean changePassword(final String servergroup,
                                  final String username, 
                                  final String oldPassword,
                                  final String newPassword)
    {                        
        Runnable run = new Runnable() {
            public void run()
            {
                _changePassword(servergroup, username, oldPassword, newPassword);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
        
        return true;
                
    }
    
    //---------------------------------------------------------------------
    
    public boolean makeDomainFile(final File outputFile, 
                                   final String serverGroup)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _makeDomainFile(outputFile, serverGroup);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
        
        return true;
    }
    
    //---------------------------------------------------------------------
    
    public void _makeDomainFile(File outputFile, String group) 
    {

        final boolean wasBusy = _isBusy;
        final String serverGroup = group;

        //ensure that we have login info        
        if (!tryAuthenticating(group))
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {                    
                    setStatusMessage("Unable to athenticate.  Aborting...");
                    JOptionPane.showMessageDialog(_relativeComponent,
                            "Could not connect to server '"+serverGroup+"'.\n" +
                            "Make domain file aborted. ",
                            "Make Domain", JOptionPane.WARNING_MESSAGE);
                    setStatusMessage("Make domain file aborted");
                }
            });                            
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Writing domain file.  Please wait...");
            }
        });

        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        Session tempSession = null;;
        
        try
        {
            int xactId;
            tempSession = new Session(this._domainFileURL, Constants.SSL);            
            tempSession.setDefaultGroup(serverGroup);
            tempSession.setLoginInfo(this._username, this._password);
            
            synchronized (_syncLock)
            {
                try {
                    xactId = tempSession.makeDomainFile(
                                outputFile.getAbsolutePath(),
                                serverGroup);
                } catch (SessionException sesEx) {
                    throw sesEx;
                }
                
                while (tempSession.getTransactionCount() > 0)
                {
                    Result r = tempSession.result();
                    if (r == null)
                    {
                        Thread.yield();
                        continue;
                    }
                    statusValue = r.getErrno();
                    statusMesg = r.getMessage();                          
                }
                
//                Result r = tempSession.result();
//                statusValue = r.getErrno();
//                statusMesg  = r.getMessage();                
            }
        } catch (SessionException sesEx) {
            _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
            statusValue = sesEx.getErrno();
            statusMesg = sesEx.getMessage();
        } catch (Exception ex) {
            _logger.error(ex.getMessage(), ex); ex.printStackTrace();
            statusValue = Constants.EXCEPTION;
            statusMesg = ex.getMessage();
        } finally {
            final int fStatVal = statusValue;
            final String fStatMsg = statusMesg;

            if (fStatVal == Constants.OK)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Domain file creation complete.");
                    }
                });
            }
            else
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Unable to create domain file for "
                                + serverGroup + ".");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Error occurred while attempting to "
                                + "create domain file for '" + serverGroup + "'."
                                + "\n\nERROR DETAILS\n" + "-MESSAGE: "
                                + fStatMsg + "\n-CODE: " + fStatVal,
                                "Make Domain Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                if (tempSession != null)
                    tempSession.closeImmediate();
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });            
    }
    
    
    
    
    
   //---------------------------------------------------------------------
    
    public boolean lockFiletype(final String filetype,  
                                final String lockMode)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _lockFiletype(filetype, lockMode);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
        
        return true;
    }
    
    //---------------------------------------------------------------------
    
    public void _lockFiletype(final String filetype, final String mode) 
    {

        final boolean wasBusy = _isBusy;

        final String group = FileType.extractServerGroup(filetype);
        final String type  = FileType.extractFiletype(filetype);
        
        //ensure that we have login info        
        if (!tryAuthenticating(group))
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {                    
                    setStatusMessage("Unable to athenticate.  Aborting...");
                    JOptionPane.showMessageDialog(_relativeComponent,
                            "Could not connect to server '"+group+"'.\n" +
                            "Filetype locking aborted. ",
                            "Type Lock", JOptionPane.WARNING_MESSAGE);
                    setStatusMessage("Lock filetype aborted");
                }
            });                            
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Attempting to lock filetype.  Please wait...");
            }
        });

        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        Session tempSession = null;;
        
        try
        {
            int xactId;
            tempSession = new Session(this._domainFileURL, Constants.SSL);
            tempSession.setLoginInfo(this._username, this._password);
            FileType ft = tempSession.open(group, type);
            
            synchronized (_syncLock)
            {
                try {
                    xactId = ft.lock(mode);
                } catch (SessionException sesEx) {
                    throw sesEx;
                }
                
                while (tempSession.getTransactionCount() > 0)
                {
                    Result r = tempSession.result();
                    if (r == null)
                    {
                        Thread.yield();
                        continue;
                    }
                    statusValue = r.getErrno();
                    statusMesg = r.getMessage();                          
                }
                
//                Result r = tempSession.result();
//                statusValue = r.getErrno();
//                statusMesg  = r.getMessage();                
            }
        } catch (SessionException sesEx) {
            _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
            statusValue = sesEx.getErrno();
            statusMesg = sesEx.getMessage();
        } catch (Exception ex) {
            _logger.error(ex.getMessage(), ex); ex.printStackTrace();
            statusValue = Constants.EXCEPTION;
            statusMesg = ex.getMessage();
        } finally {
            final int fStatVal = statusValue;
            final String fStatMsg = statusMesg;

            if (fStatVal == Constants.OK)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Filetype '"+filetype+"' locked.");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Filetype '" + filetype + "' locked.",
                                "Lock Type", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
            else
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Unable to lock filetype "
                                + filetype + ".");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Error occurred while attempting to "
                                + "lock filetype '" + filetype + "'."
                                + "\n\nERROR DETAILS\n" + "-MESSAGE: "
                                + fStatMsg + "\n-CODE: " + fStatVal,
                                "Type Lock Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                if (tempSession != null)
                    tempSession.closeImmediate();
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });            
    }
    
   //---------------------------------------------------------------------
    
    public boolean unlockFiletype(final String filetype, final String mode)
    {
        Runnable run = new Runnable() {
            public void run()
            {
                _unlockFiletype(filetype, mode);
            }
        };
        Thread thrd = new Thread(run);
        thrd.setPriority(3);
        thrd.start();
        
        return true;
    }
    
    //---------------------------------------------------------------------
    
    public void _unlockFiletype(final String filetype, final String mode) 
    {

        final boolean wasBusy = _isBusy;

        final String group = FileType.extractServerGroup(filetype);
        final String type  = FileType.extractFiletype(filetype);
        
        //ensure that we have login info        
        if (!tryAuthenticating(group))
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {                    
                    setStatusMessage("Unable to authenticate.  Aborting...");
                    JOptionPane.showMessageDialog(_relativeComponent,
                            "Could not connect to server '"+group+"'.\n" +
                            "Filetype unlocking aborted. ",
                            "Type Unlock", JOptionPane.WARNING_MESSAGE);
                    setStatusMessage("Unlock filetype aborted");
                }
            });                            
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(true);
                setStatusMessage("Attempting to unlock filetype.  Please wait...");
            }
        });

        int statusValue = Constants.IO_ERROR;
        String statusMesg = "No reply from server.";
        Session tempSession = null;;
        
        try
        {
            int xactId;
            tempSession = new Session(this._domainFileURL, Constants.SSL);
            tempSession.setLoginInfo(this._username, this._password);
            FileType ft = tempSession.open(group, type);
            
            synchronized (_syncLock)
            {
                try {
                    xactId = ft.unlock(mode);
                } catch (SessionException sesEx) {
                    throw sesEx;
                }
                
                while (tempSession.getTransactionCount() > 0)
                {
                    Result r = tempSession.result();
                    if (r == null)
                    {
                        Thread.yield();
                        continue;
                    }
                    statusValue = r.getErrno();
                    statusMesg = r.getMessage();                          
                }
                
//                Result r = tempSession.result();
//                statusValue = r.getErrno();
//                statusMesg  = r.getMessage();                
            }
        } catch (SessionException sesEx) {
            _logger.error(sesEx.getMessage(), sesEx); //sesEx.stackTrace();
            statusValue = sesEx.getErrno();
            statusMesg = sesEx.getMessage();
        } catch (Exception ex) {
            _logger.error(ex.getMessage(), ex); ex.printStackTrace();
            statusValue = Constants.EXCEPTION;
            statusMesg = ex.getMessage();
        } finally {
            final int fStatVal = statusValue;
            final String fStatMsg = statusMesg;

            if (fStatVal == Constants.OK)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Filetype '"+filetype+"' unlocked.");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Filetype '" + filetype + "' unlocked.",
                                "Lock Type", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
            else if (fStatVal == Constants.FTNOTLOCKED)
            {
                setStatusMessage("Filetype '"+filetype+"' currently unlocked.");
                JOptionPane.showMessageDialog(_relativeComponent,
                        "Filetype '"+filetype+"' already unlocked.\n"+
                        "No changes occurred.",
                        "Lock Type", JOptionPane.INFORMATION_MESSAGE);
            }                
            else
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setStatusMessage("Unable to unlock filetype "
                                + filetype + ".");
                        JOptionPane.showMessageDialog(_relativeComponent,
                                "Error occurred while attempting to "
                                + "unlock filetype '" + filetype + "'."
                                + "\n\nERROR DETAILS\n" + "-MESSAGE: "
                                + fStatMsg + "\n-CODE: " + fStatVal,
                                "Type Unlock Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                if (tempSession != null)
                    tempSession.closeImmediate();
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                setBusyFlag(wasBusy);
            }
        });            
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Returns array of all filetypes defined in the domain file of the
     * current session instance.
     * @return Sorted array of filetypes (e.g. groupname:filetype)
     */
    
    public String[] getAllFiletypes()
    {
        String[] fts = new String[0];
        if (this._session != null)
        {
            List list = new ArrayList();
            synchronized(this._session)
            {
                List gList = this._session.getGroupList();
                int gsize = gList.size();
                for (int i = 0; i < gsize; ++i)
                {
                    String groupName = (String) gList.get(i);
                    List fList;
                    try {
                        fList = this._session.getFileTypeList(groupName);
                    } catch (SessionException sesEx) {
                        continue;
                    }
                    
                    int fsize = fList.size();
                    for (int j = 0; j < fsize; ++j)
                    {
                        String ft = (String) fList.get(j);
                        String fullFt = FileType.toFullFiletype(groupName, ft);
                        list.add(fullFt);
                        
                    } //end_inner_for
                } //end_outer_for
            } //end_sync
            
            Collections.sort(list);
            int size = list.size();
            fts = new String[size];
            for (int i = 0; i < size; ++i)
            {
                fts[i] = (String) list.get(i);
            }
        }
        
        return fts;
    }

    //---------------------------------------------------------------------
    
    public String getPassword(String username)
    {
        if (username == null || !username.equals(this._username))
            return null;
        return this._password;
    }    
    
    //---------------------------------------------------------------------
    
    /**
     * Refreshing the remote listing per file takes to long when the 
     * number of files grows. So we will refresh at a rate determined
     * by this method (be sure to check for the first and last file though).
     * 
     * Note: You can set the maximum rate by setting a positive integer
     * value for property 'savannah.max.refresh'.
     * 
     * @param fileIndex Index of the file that was processed
     * @param fileTotal Total number of files being processed
     * @return Refresh rate for remote listing for adds/replaces
     */
    
    protected boolean shouldRequestRefresh(int fileIndex, int fileTotal)
    {
        if (fileIndex == 0)
            return true;
        else if (fileIndex == fileTotal - 1)
            return true;
        else
        {
            int rate;
            
            if (fileTotal < 5)
                rate = 2;
            else if (fileTotal < 10)
                rate = 3;
            else if (fileTotal < 25)
            {
                rate = 5;
            }
            else
            {                
                rate = (fileTotal / 10) + 4;        
//                if (rate == 0)
//                    rate = 1;
                if (rate > _maxListingRefreshRate)
                    rate = _maxListingRefreshRate;
            }
            
            if (fileIndex % rate == 0)
                return true;
        }
        
        return false;
    } 
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
   

    /**
     * Shutdown hook implementation to logout of session prior
     * to JVM exiting.
     */
    
    class ShutDownHandler extends Thread
    {
        public ShutDownHandler()
        {
            _logger.debug("Instantiated shutdown handler.");
        }
               
        //-----------------------------------------------------------------
        
        public void run() {

            //----------------------
            
            //kill session
            if (_session != null)
            {
                _session.closeImmediate();
                _logger.debug("Session closed.");
            }
            
            //----------------------
            
            //kill subscription manager
            if (_subscriptionManager != null) 
            {
                _subscriptionManager.terminate();
                _logger.debug("Subscription manager terminated.");
            }
            
            //----------------------
        }        
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

} //end_SavannahModel_class
