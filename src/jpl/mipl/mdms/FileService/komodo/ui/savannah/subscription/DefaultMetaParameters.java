/*
 * Created on Jan 10, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


/**
 * <b>Purpose:</b>
 * Abstract class containing shared parameters for notification/
 * subscription, otherwise known as meta-subscription.  This
 * class is used by meta-subscriptions for settings and also
 * to create Client objects.
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
 * 01/10/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DefaultMetaParameters.java,v 1.13 2013/08/21 22:10:01 ntt Exp $
 *
 */

public class DefaultMetaParameters implements MetaParameters
{
    protected final Map _parameters;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Initial set of parameters is empty.
     */
    
    public DefaultMetaParameters()
    {
        this._parameters = new Hashtable();
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Initial set of parameters is set to the
     * clone of the argument's parameters.
     * @param instance of DefaultMetaParameters
     */
    
    public DefaultMetaParameters(DefaultMetaParameters mp)
    {
        //getOptions returns a copy, so we are safe using the return value
        this._parameters = mp.getAll();
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Initial set of parameters is set based on the
     * contents of the input map.
     * @param parameterMap Instance of map containing initial parameters
     */
    
    public DefaultMetaParameters(Map parameterMap)
    {
        this._parameters = new Hashtable(parameterMap);                 
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Performs initialization of the base class.
     */
    
    protected void init()
    {
        //by default, output dir is the user directory
        if (getOutputDirectory() == null)
            setOutputDirectory(System.getProperty("user.dir"));
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns a copy of the map of options used by this instance.
     * @return Copy of new map of options parameters
     */
    
    public Map getAll()
    {
        return new Hashtable(this._parameters);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Clear current options.
     */
    
    public void reset()
    {
        this._parameters.clear();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns value bound to parameter 
     * @param parameterName Parameter name
     * @return Object associated as parameter value, null if not bound.
     */
    
    public Object get(String parameterName)
    {
        return this._parameters.get(parameterName);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets value bound to parameter 
     * @param parameterName Parameter name
     * @param value Object to be associated as parameter value
     */
    
    public void set(String parameterName, Object value)
    {
        if (parameterName != null)
        {
            if (value != null)
                this._parameters.put(parameterName, value);
            else
                this._parameters.remove(parameterName);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets values based on contents of parameter 
     * @param mappedParameters Map of parameter names and values
     */
    
    public void setAll(Map mappedParameters)
    {
        Iterator it = mappedParameters.keySet().iterator();
        while (it.hasNext())
        {
            String parameterName  = (String) it.next();
            Object parameterValue = mappedParameters.get(parameterName);
            
            set(parameterName, parameterValue);            
        }
    }
    
    //---------------------------------------------------------------------
    
    public void setFiletype(String filetype)
    {
        this.set(SubscriptionConstants.KEY_FILETYPE, filetype);   
    }
    
    //---------------------------------------------------------------------
    
    public String getFiletype()
    {
        return (String) this.get(SubscriptionConstants.KEY_FILETYPE);
    }
    //---------------------------------------------------------------------
    
    public void setOutputDirectory(String outputDir)
    {
        this.set(SubscriptionConstants.KEY_OUTPUT_DIRECTORY, outputDir);
    }
    
    //---------------------------------------------------------------------
    
    public String getOutputDirectory()
    {
        return (String) this.get(SubscriptionConstants.KEY_OUTPUT_DIRECTORY);
    }
    //---------------------------------------------------------------------
    
    public void setOptionsFilename(String optionsFile)
    {
        this.set(SubscriptionConstants.KEY_OPTIONS_FILENAME, optionsFile);
    }
    
    //---------------------------------------------------------------------
    
    public String getOptionsFilename()
    {
        return (String) this.get(SubscriptionConstants.KEY_OPTIONS_FILENAME);
    }
    
    //---------------------------------------------------------------------
    
    public void setRestart(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_RESTART, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getRestart()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_RESTART);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setSessionStayAlive(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_STAYALIVE, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getSessionStayAlive()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_STAYALIVE);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setInvokeCommand(String command)
    {
        this.set(SubscriptionConstants.KEY_INVOKE_COMMAND, command);
    }
    
    //---------------------------------------------------------------------
    
    public String getInvokeCommand()
    {
        return (String) this.get(SubscriptionConstants.KEY_INVOKE_COMMAND);
    }
    
    //---------------------------------------------------------------------
    
    public void setInvokeExitOnError(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_INVOKE_EXIT_ON_ERROR, 
                                             new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getInvokeExitOnError()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.
                                          KEY_INVOKE_EXIT_ON_ERROR);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setInvokeAsync(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_INVOKE_ASYNC, 
                                             new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getInvokeAsync()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.
                                          KEY_INVOKE_ASYNC);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setLogFilename(String filename)
    {
        this.set(SubscriptionConstants.KEY_LOG_FILENAME, filename);
    }
    
    //---------------------------------------------------------------------
    
    public String getLogFilename()
    {
        return (String) this.get(SubscriptionConstants.KEY_LOG_FILENAME);
    }
    
    //---------------------------------------------------------------------
    
    public void setMailMessageFrom(String from)
    {
        this.set(SubscriptionConstants.KEY_MAIL_MESSAGE_FROM, from);
    }
    
    //---------------------------------------------------------------------
    
    public String getMailMessageFrom()
    {
        return (String) this.get(SubscriptionConstants.KEY_MAIL_MESSAGE_FROM);
    }
    
    //---------------------------------------------------------------------
    
    public void setMailMessageTo(String to)
    {
        this.set(SubscriptionConstants.KEY_MAIL_MESSAGE_TO, to);
    }
    
    //---------------------------------------------------------------------
    
    public String getMailMessageTo()
    {
        return (String) this.get(SubscriptionConstants.KEY_MAIL_MESSAGE_TO);
    }
    
    //---------------------------------------------------------------------
    
    public void setMailReportTo(String to)
    {
        this.set(SubscriptionConstants.KEY_MAIL_REPORT_TO, to);
    }
    
    //---------------------------------------------------------------------
    
    public String getMailReportTo()
    {
        return (String) this.get(SubscriptionConstants.KEY_MAIL_REPORT_TO);
    }
    
    //---------------------------------------------------------------------
    
    public void setMailReportAt(String at)
    {
        this.set(SubscriptionConstants.KEY_MAIL_REPORT_AT, at);
    }
    
    //---------------------------------------------------------------------
    
    public String getMailReportAt()
    {
        return (String) this.get(SubscriptionConstants.KEY_MAIL_REPORT_AT);
    }
    
    //---------------------------------------------------------------------
    
    public void setMailSMTPHost(String host)
    {
        this.set(SubscriptionConstants.KEY_MAIL_SMTP_HOST, host);
    }
    
    //---------------------------------------------------------------------
    
    public String getMailSMTPHost()
    {
        return (String) this.get(SubscriptionConstants.KEY_MAIL_SMTP_HOST);
    }
    
    //---------------------------------------------------------------------
    
    public void setDomainFile(URL domainFile)
    {
        this.set(SubscriptionConstants.KEY_DOMAIN_FILE_URL, domainFile);
    }
    
    //---------------------------------------------------------------------
    
    public URL getDomainFile()
    {
        return (URL) this.get(SubscriptionConstants.KEY_DOMAIN_FILE_URL);
    }
    
    //---------------------------------------------------------------------
    
    public void setUsername(String user)
    {
        this.set(SubscriptionConstants.KEY_USERNAME, user);
    }
    
    //---------------------------------------------------------------------
    
    public String getUsername()
    {
        return (String) this.get(SubscriptionConstants.KEY_USERNAME);
    }
    
    //---------------------------------------------------------------------
    
    public void setPassword(String password)
    {
        this.set(SubscriptionConstants.KEY_PASSWORD, password);
    }
    
    //---------------------------------------------------------------------
    
    public String getPassword()
    {
        return (String) this.get(SubscriptionConstants.KEY_PASSWORD);
    }

    //---------------------------------------------------------------------
    
    /*
    public void setServerGroup(String group)
    {
        this.set(SubscriptionConstants.KEY_SERVER_GROUP, group);
    }
    
    //---------------------------------------------------------------------
    
    public String getServerGroup()
    {
        return (String) this.get(SubscriptionConstants.KEY_SERVER_GROUP);
    }
    */
    
    //---------------------------------------------------------------------
    
    public String getLogfileRolling()
    {
        return (String) this.get(SubscriptionConstants.KEY_LOGFILE_ROLLING);
    }
    
    //---------------------------------------------------------------------
    
    public void setLogfileRolling(String period)
    {
        this.set(SubscriptionConstants.KEY_LOGFILE_ROLLING, period);        
    }
    
    //---------------------------------------------------------------------
    
    public boolean getPull()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_PULL);
        return (flag != null && flag.booleanValue());
    }

    //---------------------------------------------------------------------
    
    public void setPull(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_PULL, new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getPush()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.KEY_PUSH);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setPush(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_PUSH, new Boolean(flag));
    }
    

    //---------------------------------------------------------------------
    
    public int getTaskType()
    {
        Integer value = (Integer) this.get(SubscriptionConstants.KEY_TASK_TYPE);        
        return (value == null ? SubscriptionConstants.TASK_UNKNOWN : value.intValue());
    }
    
    //---------------------------------------------------------------------
    
    public void setTaskType(int taskType)
    {
        this.set(SubscriptionConstants.KEY_TASK_TYPE, new Integer(taskType));
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
    
    public void setMailSilentReconn(boolean flag)
    {
        this.set(SubscriptionConstants.KEY_MAIL_SILENT_RECONN, 
                                             new Boolean(flag));
    }
    
    //---------------------------------------------------------------------
    
    public boolean getMailSilentReconn()
    {
        Boolean flag = (Boolean) this.get(SubscriptionConstants.
                                    KEY_MAIL_SILENT_RECONN);
        return (flag != null && flag.booleanValue());
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
}
