/*
 * Created on Aug 8, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Session;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.SessionCache;
import jpl.mipl.mdms.FileService.util.DateTimeFormatter;
import jpl.mipl.mdms.utils.logging.Logger;


/**
 * <b>Purpose:</b>
 * Model of the session settings.
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
 * 08/08/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahSessionModel.java,v 1.10 2009/09/22 21:58:19 ntt Exp $
 *
 */

public class SavannahSessionModel
{
    //-----------------------------
    
    public static final String FILE_RECEIPT_ENABLED = "FILE_RECEIPT_ENABLED";

    public static final String RESUME_TRANSACTION_ENABLED = "RESUME_TRANSACTION_ENABLED";

    public static final String RESTART_TRANSACTION_ENABLED = "RESTART_TRANSACTION_ENABLED";

    public static final String COMPUTE_CHECKSUM_ENABLED = "COMPUTE_CHECKSUM_ENABLED";
    
    public static final String FILE_DIFF_ENABLED = "FILE_DIFF_ENABLED";

    public static final String FILE_VERSION_ENABLED = "FILE_VERSION_ENABLED";

    public static final String FILE_REPLACE_ENABLED = "FILE_REPLACE_ENABLED";

    public static final String LOCAL_DIRECTORY = "LOCAL_DIRECTORY";

    public static final String FILE_SAFEREAD_ENABLED = "FILE_SAFEREAD_ENABLED";

    public static final String DATE_TIME_FORMAT = "DATE_TIME_FORMAT";
    
    //-----------------------------
    
    /** Flag used to indicate if checksum should be computed */
    protected boolean               _computeChecksumEnabled;

    /** Flag used to indicate if restart is enabled */
    protected boolean               _restartTransferEnabled;

    /** Flag used to indicate if resume transfer is enabled */
    protected boolean               _resumeTransferEnabled;
    
    /** Flag used to indicate if file replace is enabled */
    protected boolean               _fileReplaceEnabled;
    
    /** Flag used to indicate if file version is enabled */
    protected boolean               _fileVersionEnabled;

    /** Flag used to indicate if file saferead is enabled */
    protected boolean               _fileSafereadEnabled;

    /** Flag used to indicate if file diff is enabled */
    protected boolean               _fileDiffEnabled;
    
    /** Flag used to indicate if file receipt is enabled */
    protected boolean               _fileReceiptEnabled;
    
    /** Path to current domain file */
    //protected String                _domainFilename;
    
    /** Keystore path */
    protected String                _keystore;
    
    /** Reference to session local directory */
    protected File                  _localDirectory;
    
    /** Reference to session date format string */
    protected String                _dateTimeFormat;
    
    
    /** private logger instance */
    private static Logger _logger = Logger.getLogger(SavannahSessionModel.class.getName());
    
    /** Enables bean property change event handling */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(this);
    
    //private Session _currentSession;
    protected SessionCache sessionCache;
    
    //---------------------------------------------------------------------

    
    //---------------------------------------------------------------------
    
    /**
     * Constructor. 
     */
    
    public SavannahSessionModel()
    {
        this._localDirectory = new File(System.getProperty("user.dir"));
        this._computeChecksumEnabled = false;
        this._restartTransferEnabled = false;
        this._resumeTransferEnabled  = false;
        this._fileReplaceEnabled     = false;
        this._fileVersionEnabled     = false;
        this._fileReceiptEnabled     = false;
        this._fileSafereadEnabled    = false;
        this._fileDiffEnabled        = false;
        this._dateTimeFormat         = null;
        
        loadSessionCache();
    }

    //---------------------------------------------------------------------
    
    /**
     * Loads session cache if found, or creates new one.
     * Sets fields according to cached values.
     * Adds listening to this class to update cache as fields
     * are updated.
     */
    
    protected void loadSessionCache()
    {                
        this.sessionCache = SessionCache.restoreFromCache();
        
        String cachedDir = this.sessionCache.getCurrentDirectory();
        if (cachedDir == null)
        {            
            this.sessionCache.setCurrentDirectory(this._localDirectory.getAbsolutePath());
            try {
                sessionCache.commit();
            } catch (SessionException sesEx) {
                _logger.error("Error occurred while commit " +
                              "session cache: "+sesEx.getMessage());
                _logger.trace(null, sesEx);
            }
        }
        else
        {            
            File tmpFile = new File(cachedDir);
            if (tmpFile.isDirectory())
                this._localDirectory = tmpFile;
        }

        this._computeChecksumEnabled = this.sessionCache.getOption(Constants.CHECKSUM);
        this._restartTransferEnabled = this.sessionCache.getOption(Constants.RESTART);
        this._resumeTransferEnabled  = this.sessionCache.getOption(Constants.RESUME);
        this._fileReplaceEnabled     = this.sessionCache.getOption(Constants.FILEREPLACE);
        this._fileVersionEnabled     = this.sessionCache.getOption(Constants.FILEVERSION);
        
        this._fileSafereadEnabled    = this.sessionCache.getOption(Constants.SAFEREAD);
        this._fileReceiptEnabled     = this.sessionCache.getOption(Constants.RECEIPTONXFR);
        this._fileDiffEnabled        = this.sessionCache.getOption(Constants.DIFF);

        this._dateTimeFormat         = this.sessionCache.getDateTimeFormat();
        
        //listens for updates and commits to cache
        this.addPropertyChangeListener(new SessionCacheSync());
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
     * Sets the local directory state for sessions.
     * @param localDir New directory for session directory.
     */
    
    public void setLocalDirectory(File localDir)
    {
        if (localDir == null)
            return;

        if (this._localDirectory != null && _localDirectory.equals(localDir))
            return;

        Object oldValue = this._localDirectory;
        this._localDirectory = localDir;
        this._changes.firePropertyChange(LOCAL_DIRECTORY, 
                                         oldValue, this._localDirectory);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the local directory state for sessions.
     * @param localDir New directory for session directory.
     */
    
    public void setDateTimeFormat(String dateTimeFormat)
    {
        if (dateTimeFormat == null)
            dateTimeFormat = DateTimeFormatter.DEFAULT_FORMAT;

        if (this._dateTimeFormat != null && _dateTimeFormat.equals(dateTimeFormat))
            return;

        Object oldValue = this._dateTimeFormat;
        this._dateTimeFormat = dateTimeFormat;
        this._changes.firePropertyChange(DATE_TIME_FORMAT, 
                                         oldValue, this._dateTimeFormat);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the local directory state for sessions.
     * @return Local directory.
     */
    
    public String getDateTimeFormat()
    {
        return this._dateTimeFormat;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the local directory state for sessions.
     * @return Local directory.
     */
    
    public File getLocalDirectory()
    {
        return this._localDirectory;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that returns the path of the local directory.
     * @return Absolute directory path.
     */
    
    public String getLocalDirectoryPath()
    {
        return this._localDirectory.getAbsolutePath();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Enabled/disable session file replace option.
     * @param flag Indicates desired state of option.
     */
    
    public void enableFileReplace(boolean flag)
    {
        if (flag != this._fileReplaceEnabled)
        {
            Object oldValue = new Boolean(this._fileReplaceEnabled);
            this._fileReplaceEnabled = flag;        
            this._changes.firePropertyChange(FILE_REPLACE_ENABLED, 
                          oldValue, new Boolean(_fileReplaceEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " file replace session option.";
            _logger.info(msg);
        }
    }

    //---------------------------------------------------------------------
    
    /**
     * Enabled/disable session file replace option.
     * @param flag Indicates desired state of option.
     */
    
    public void enableFileVersion(boolean flag)
    {
        if (flag != this._fileVersionEnabled)
        {
            Object oldValue = new Boolean(this._fileVersionEnabled);
            this._fileVersionEnabled = flag;        
            this._changes.firePropertyChange(FILE_VERSION_ENABLED, 
                          oldValue, new Boolean(_fileVersionEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " file version session option.";
            _logger.info(msg);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Enabled/disable session crc option.
     * @param flag Indicates desired state of option.
     */
    
    public void enableComputeChecksum(boolean flag)
    {
        if (flag != this._computeChecksumEnabled)
        {
            Object oldValue = new Boolean(this._computeChecksumEnabled);
            this._computeChecksumEnabled = flag;        
            this._changes.firePropertyChange(COMPUTE_CHECKSUM_ENABLED, 
                          oldValue, new Boolean(_computeChecksumEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " compute checksum session option.";
            _logger.info(msg);
        }
    }
    
    //---------------------------------------------------------------------

    /**
     * Enabled/disable session restart option.
     * @param flag Indicates desired state of option.
     */

    public void enableRestartTransfer(boolean flag)
    {
        if (flag != this._restartTransferEnabled)
        {
            Object oldValue = new Boolean(this._restartTransferEnabled);
            this._restartTransferEnabled = flag;
            this._changes.firePropertyChange(RESTART_TRANSACTION_ENABLED, 
                          oldValue, new Boolean(_restartTransferEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " restart transfer session option.";
            _logger.info(msg);
        }

    }
    
    //---------------------------------------------------------------------

    /**
     * Enabled/disable session option of resuming transfers. If true, then
     * checksum and restart is considered enabled.
     * @param flag Indicates desired state of option.
     */

    public void enableResumeTransfer(boolean flag)
    {
        if (flag != this._resumeTransferEnabled)
        {
            Object oldValue = new Boolean(this._resumeTransferEnabled);
            this._resumeTransferEnabled = flag;
            this._changes.firePropertyChange(RESUME_TRANSACTION_ENABLED, 
                         oldValue, new Boolean(_resumeTransferEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " resume transfer session option.";
            _logger.info(msg);
        }
    }
    
    //---------------------------------------------------------------------
    
    public void enableSaferead(boolean flag)
    {
        if (flag != this._fileSafereadEnabled)
        {
            Object oldValue = new Boolean(this._fileSafereadEnabled);
            this._fileSafereadEnabled = flag;        
            this._changes.firePropertyChange(FILE_SAFEREAD_ENABLED, 
                          oldValue, new Boolean(_fileSafereadEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " file saferead session option.";
            _logger.info(msg);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Enabled/disable session file diff option.
     * @param flag Indicates desired state of option.
     */
    
    public void enableDiff(boolean flag)
    {
        if (flag != this._fileDiffEnabled)
        {
            Object oldValue = new Boolean(this._fileDiffEnabled);
            this._fileDiffEnabled = flag;        
            this._changes.firePropertyChange(FILE_DIFF_ENABLED, 
                          oldValue, new Boolean(_fileDiffEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " file diff session option.";
            _logger.info(msg);
        }
    }
    
    //---------------------------------------------------------------------
    
    public void enableReceipt(boolean flag)
    {
        if (flag != this._fileReceiptEnabled)
        {
            Object oldValue = new Boolean(this._fileReceiptEnabled);
            this._fileReceiptEnabled = flag;        
            this._changes.firePropertyChange(FILE_RECEIPT_ENABLED, 
                          oldValue, new Boolean(_fileReceiptEnabled));
            
            String msg = (flag) ? "Enabled" : "Disabled";
            msg += " file receipt session option.";
            _logger.info(msg);
        }
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns state of session restart option.
     * @param True if option enabled, false otherwise.
     */

    public boolean isRestartTransferEnabled()
    {
        return _restartTransferEnabled;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns state of session checksum option.
     * @param True if option enabled, false otherwise.
     */

    public boolean isComputeChecksumEnabled()
    {
        return _computeChecksumEnabled;
    }

    //---------------------------------------------------------------------

    /**
     * Returns state of resume option.
     * @param True if option enabled, false otherwise.
     */

    public boolean isResumeTransferEnabled()
    {
        return _resumeTransferEnabled;
    }
    
    //---------------------------------------------------------------------
    
    public boolean isSafereadEnabled()
    {
        return this._fileSafereadEnabled;
    }
    
    //---------------------------------------------------------------------
    
    public boolean isDiffEnabled()
    {
        return this._fileDiffEnabled;
    }
    
    //---------------------------------------------------------------------
    
    public boolean isReceiptEnabled()
    {
        return this._fileReceiptEnabled;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that performs a logical OR between resume
     * and restart options.  If either one is set, then return value
     * is true.
     * @return True if restart is implicitly enabled, false otherwise.
     */
    
    public boolean canRestart()
    {
        return this._resumeTransferEnabled || this._restartTransferEnabled;
    }

    //---------------------------------------------------------------------
    
    /**
     * Convenience method that performs a logical OR between resume
     * and crc options.  If either one is set, then return value
     * is true.
     * @return True if crc is implicitly enabled, false otherwise.
     */
    
    public boolean canComputeChecksum()
    {
        return this._resumeTransferEnabled || this._computeChecksumEnabled;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Loads new session object and replaces old session object if necessarry.
     * @param domainFilename Path of the FEI domain file
     * @return True if connection was successful, false otherwise.
     */

    protected Session createSession(URL domainFileURL) throws SessionException
    {
        Session newSession = new Session(domainFileURL, Constants.SSL);
        
        if (newSession == null)
            return null;

        //set session options
        try {
            newSession.setDirectory(getLocalDirectoryPath());
            newSession.setOption(Constants.RESTART, canRestart());
            newSession.setOption(Constants.CHECKSUM, canComputeChecksum());
            newSession.setOption(Constants.RESUME, isResumeTransferEnabled());
            newSession.setOption(Constants.FILEREPLACE, isFileReplaceEnabled());
            newSession.setOption(Constants.FILEVERSION, isFileVersionEnabled());
            newSession.setOption(Constants.SAFEREAD, isSafereadEnabled());
            newSession.setOption(Constants.RECEIPTONXFR, isReceiptEnabled());
            newSession.setOption(Constants.DIFF, isDiffEnabled());  
        } catch (SessionException sesEx) {
            _logger.error(sesEx.getMessage()); 
            throw sesEx;
        } catch (Exception ex) {
            _logger.error(ex.getMessage());
            throw new SessionException("General exception: " +
                      ex.getMessage(), Constants.EXCEPTION);
        }

        //------------------------------

        //return newly constructed session
        return newSession;    
    }
    
    //---------------------------------------------------------------------

//    private void setDomainFilename(String domainfile)
//    {
//        if (this._domainFilename != null && 
//            !this._domainFilename.equalsIgnoreCase(domainfile))
//        {
//            String oldValue = this._domainFilename;
//            this._domainFilename = domainfile;
//            
//            this._changes.firePropertyChange("FEI_DOMAIN_FILE", 
//                                oldValue, _domainFilename);            
//        }
//    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true iff file replace option is enabled.
     * @return File replace option.
     */
    
    public boolean isFileReplaceEnabled()
    {
        return this._fileReplaceEnabled;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true iff file version option is enabled.
     * @return File version option.
     */
    
    public boolean isFileVersionEnabled()
    {
        return this._fileVersionEnabled;
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    class SessionCacheSync implements PropertyChangeListener
    {

        public void propertyChange(PropertyChangeEvent pce)
        {
            String propertyName = pce.getPropertyName();
            boolean cacheUpdated = false;
            
            if (propertyName.equals(LOCAL_DIRECTORY))
            {
                File file = (File) pce.getNewValue();
                sessionCache.setCurrentDirectory(file.getAbsolutePath());
                cacheUpdated = true;
            }
            else if (propertyName.equals(FILE_REPLACE_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.FILEREPLACE, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(FILE_VERSION_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.FILEVERSION, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(COMPUTE_CHECKSUM_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.CHECKSUM, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(RESTART_TRANSACTION_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.RESTART, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(RESUME_TRANSACTION_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.RESUME, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(FILE_SAFEREAD_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.SAFEREAD, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(FILE_DIFF_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.DIFF, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(FILE_RECEIPT_ENABLED))
            {
                boolean enabled = ((Boolean) pce.getNewValue()).booleanValue();
                sessionCache.setOption(Constants.RECEIPTONXFR, enabled);
                cacheUpdated = true;
            }
            else if (propertyName.equals(DATE_TIME_FORMAT))
            {
                String dateTimeFormat = ((String) pce.getNewValue());
                sessionCache.setDateTimeFormat(dateTimeFormat);
                cacheUpdated = true;
            }
            
            if (cacheUpdated)
            {
                try {
                    sessionCache.commit();
                } catch (SessionException sesEx) {
                    _logger.error("Error occurred while commit " +
                    		      "session cache: "+sesEx.getMessage());
                    _logger.trace(null, sesEx);
                }
            }
        }
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
}
