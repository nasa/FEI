package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.net.MalformedURLException;
import java.net.URL;

import jpl.mipl.mdms.utils.logging.Logger;


/**
 * <b>Purpose:</b>
 * Constants interface for the Savannah subscription package.
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
 * 01/13/2005        Nick             Initial Release
 * 09/22/2005        Nick             Added support for push/pull
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionConstants.java,v 1.21 2013/08/21 22:10:01 ntt Exp $
 *
 */

public abstract class SubscriptionConstants
{
    
    //---------------------------------------------------------------------
    //keywords for the MetaParameter class
    
    public static final String KEY_FILETYPE            = "filetype";
    public static final String KEY_OUTPUT_DIRECTORY    = "outputDirectory";
    public static final String KEY_OPTIONS_FILENAME    = "optionsFilename";
    public static final String KEY_RESTART             = "restart";
    public static final String KEY_INVOKE_COMMAND      = "invokeCommand";
    public static final String KEY_INVOKE_EXIT_ON_ERROR = "invokeExitOnError";
    public static final String KEY_INVOKE_ASYNC        = "invokeAsync";
    public static final String KEY_LOG_FILENAME        = "logFilename";
    public static final String KEY_MAIL_MESSAGE_FROM   = "mailMessageFrom";
    public static final String KEY_MAIL_MESSAGE_TO     = "mailMessageTo";
    public static final String KEY_MAIL_REPORT_TO      = "mailReportTo";
    public static final String KEY_MAIL_REPORT_AT      = "mailReportAt";
    public static final String KEY_MAIL_SMTP_HOST      = "mailSmtpHost";
    public static final String KEY_MAIL_SILENT_RECONN  = "mailSilentReconn";
    
    //public static final String KEY_DOMAIN_FILENAME   = "domainFilename";
    public static final String KEY_DOMAIN_FILE_URL   = "domainFileURL";
    public static final String KEY_USERNAME          = "username";
    public static final String KEY_PASSWORD          = "password";
    public static final String KEY_SERVER_GROUP      = "serverGroup";
    public static final String KEY_CRC               = "crc";
    public static final String KEY_RECEIPT           = "receipt";
    public static final String KEY_SAFEREAD          = "saferead";
    public static final String KEY_VERSION           = "version";
    public static final String KEY_REPLACE           = "replace";
    public static final String KEY_LOGFILE_ROLLING   = "logfileRolling";
    public static final String KEY_PULL              = "pull";
    public static final String KEY_PUSH              = "push";
    public static final String KEY_STAYALIVE         = "keepalive";
    public static final String KEY_TASK_TYPE         = "tasktype";
    public static final String KEY_DIFF              = "diff";
       
    
    //---------------------------------------------------------------------
    
    /**
     * Wraps value parameter in another object based on the type
     * associated with the keyword.  For example, if the keyword
     * is KEY_CRC and value is "true", then Boolean.TRUE would
     * be returned because CRC is a boolean option.  If keyword
     * was KEY_USERNAME and value "true", then the string "true"
     * would be returned.
     * Currently only Boolean and String are returned.
     * @param keyword Option keyword
     * @param value Option value
     * @return Wrapped object of type associated with keyword
     * corresponding to value.
     */
    public static Object wrapValue(String keyword, String value)
    {
        Object obj = value;
        
        if (keyword.equals(KEY_RESTART) || keyword.equals(KEY_CRC) || 
            keyword.equals(KEY_INVOKE_EXIT_ON_ERROR) || 
            keyword.equals(KEY_RECEIPT) || 
            keyword.equals(KEY_SAFEREAD) || keyword.equals(KEY_VERSION) || 
            keyword.equals(KEY_REPLACE) || keyword.equals(KEY_PULL) ||
            keyword.equals(KEY_PUSH) || keyword.equals(KEY_STAYALIVE) ||
            keyword.equals(KEY_DIFF) || keyword.equals(KEY_MAIL_SILENT_RECONN))
            
        {
            //true iff value != null && value = "true" (case insensitive)
            obj = new Boolean(value);
        }  
        else if (keyword.equals(KEY_TASK_TYPE))
        {
            obj = new Integer(value);
        }
            
        else if (keyword.equals(KEY_DOMAIN_FILE_URL))
        {
            try {
                obj = new URL(value);
            } catch (MalformedURLException muEx) {
                obj = value;
            }
        }
        return obj;
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
    
    
    //---------------------------------------------------------------------
    //event types
    public static final int EVENT_UNKNOWN            = 100;
    public static final int EVENT_SUCCESS            = 101;
    public static final int EVENT_FAILURE            = 102;
    
    public static final String EVENT_UNKNOWN_STR     = "unknown";
    public static final String EVENT_SUCCESS_STR     = "success";
    public static final String EVENT_FAILURE_STR     = "failure";
    
    //-----------------------------
    
    /**
     * Translates from event type to event type name
     * @param type Event type
     * @return one of {"success", "failure", "Unknown"}
     */
    
    public static String eventTypeToString(int type)
    {
        String name = EVENT_UNKNOWN_STR;
        if (type == EVENT_SUCCESS)
            name = EVENT_SUCCESS_STR;
        else if (type == EVENT_FAILURE)
            name = EVENT_FAILURE_STR;

        return name;
    }
    
    //-----------------------------
    
    /**
     * Translates from event name to event id
     * @param eventType Event type name
     * @return one of EVENT_{SUCCESS,FAILURE}
     */
    
    public static int eventStringToType(String eventType)
    {
        int typeVal = EVENT_UNKNOWN;
        if (eventType.equalsIgnoreCase(EVENT_SUCCESS_STR))
            typeVal = EVENT_SUCCESS;
        else if (eventType.equalsIgnoreCase(EVENT_FAILURE_STR))
            typeVal = EVENT_FAILURE;
        return typeVal;
    }
    
    
    //---------------------------------------------------------------------
    //system properties
    
    //---------------------------------------------------------------------
    //useful constants
    public static final int    MINUTE_MS = 60000;
    public static final long   DAY_MS    = 86400000L;
    public static final String ERROR_TAG = "FEI_ERROR::";
    public static final String CACHE_FILE_PREFIX = ".savannah.cache";
    public static final int    GET_RESULT_TIME_OUT_MS = 5000;
    
    //---------------------------------------------------------------------
    //meta subscription types
    public static final int TASK_UNKNOWN      = -1;
    public static final int TASK_SUBSCRIPTION =  0;
    public static final int TASK_NOTIFICATION =  1;
    public static final int[] TASK_TYPES = new int[] {
                 TASK_SUBSCRIPTION, TASK_NOTIFICATION};      
    
    public static final String TASK_UNKNOWN_STR      = "Unknown";
    public static final String TASK_SUBSCRIPTION_STR = "Subscription";
    public static final String TASK_NOTIFICATION_STR = "Notification";
    public static final String[] TASK_TYPES_STR = new String[] {
                 TASK_SUBSCRIPTION_STR, TASK_NOTIFICATION_STR};        
    
    //-----------------------------
    
    /**
     * Translates from task id to task name
     * @param type Task id
     * @return one of {"Notification", "Subscription", "Unknown"}
     */
    
    public static String taskTypeToString(int type)
    {
        if (type >=0 && type < TASK_TYPES_STR.length)
            return TASK_TYPES_STR[type];
        else
            return TASK_UNKNOWN_STR;
    }
    
    //-----------------------------
    
    /**
     * Translates from task name to task id
     * @param type Task name
     * @return TASK_{SUBSCRIPTION,NOTIFICATION,UNKNOWN}
     */
    
    public static int taskStringToType(String type)
    {
        int typeVal = TASK_UNKNOWN;
        for (int i = 0; i < TASK_TYPES_STR.length &&
                                   typeVal == TASK_UNKNOWN; i++)
        {
            if (TASK_TYPES_STR[i].equalsIgnoreCase(type))
                typeVal = i;
        }
        return typeVal;
    }
    
    //---------------------------------------------------------------------
    // States
    
    /** Subscription is initialized but not running */
    public static final int STATE_INITIALIZED = 0;
    
    /** Subscription is running (active) */
    public static final int STATE_RUNNING     = 1;
    
    /** Subscription is paused (not terminated) */
    public static final int STATE_PAUSED      = 2;
    
    /** Subscription is terminated */
    public static final int STATE_TERMINATED  = 3;
    
    /** Subscription is in error state */
    public static final int STATE_ERROR       = 4;
   
    /** Subscription is running and processing */ 
    public static final int STATE_BUSY        = 5;

    /** Subscription is repairing itself (ie lost connection) */ 
    public static final int STATE_REPAIR      = 6;
    
    /** Array of state names */
    public static final String[] STATE_STRING = new String[] {
              "Initialized", "Running", "Paused", "Terminated",
              "Error", "Busy", "Repairing"};
    
    /** Array of transition names, based on the index of next state */
    public static final String[] STATE_TRANSITION_STRING = new String[] {
            "Initialize", "Run", "Pause", "Terminate",
            "", "", ""};
    
    /**
     * Retrieve an array of legal next states given the current state
     * of a metasubscription.  If the current state is not recognized
     * or an end state, then return array will be of zero length.
     * @param currentState Current state of the metasubscription
     * @return Array of legal next states
     */
    
    public static int[] getNextStates(int currentState)
    {
        int[] destStates;
        
        switch(currentState)
        {
            case STATE_INITIALIZED:
                destStates = new int[] { STATE_RUNNING, STATE_TERMINATED};
                break;

            case STATE_RUNNING:
            //case STATE_BUSY: 
                destStates = new int[] { STATE_PAUSED, STATE_TERMINATED};
                break;
                
            case STATE_PAUSED:
                destStates = new int[] { STATE_RUNNING, STATE_TERMINATED};
                break;
                
            case STATE_ERROR:
                destStates = new int[] { STATE_TERMINATED};
                break;
                
            default:
                destStates = new int[0];
                break;
        }
        
        return destStates;
    }
    
    //---------------------------------------------------------------------
    
    public static final String LOG_ROLL_NEVER     = "NEVER";
    public static final String LOG_ROLL_MINUTELY  = "MINUTELY";
    public static final String LOG_ROLL_HOURLY    = "HOURLY";
    public static final String LOG_ROLL_HALFDAILY = "HALFDAILY";
    public static final String LOG_ROLL_DAILY     = "DAILY";
    public static final String LOG_ROLL_WEEKLY    = "WEEKLY";
    public static final String LOG_ROLL_MONTHLY   = "MONTHLY";
    public static final String[] LOG_ROLL_OPTIONS = new String[] {
            LOG_ROLL_NEVER, LOG_ROLL_MINUTELY, LOG_ROLL_HOURLY,
            LOG_ROLL_HALFDAILY, LOG_ROLL_DAILY, LOG_ROLL_WEEKLY,
            LOG_ROLL_MONTHLY};
    
    /**
     * Converts from log rolling string to associated log rolling
     * type as defined by the Logger class in the utility logging
     * packge.
     * @param logRollString One of LOG_ROLL_{NEVER,MINUTELY,HOURLY,
     *        HALFDAILY,DAILY,WEEKLY,MONTHLY}
     * @return Associated logger constant, ROLLING_*, or ROLLING_NEVER
     *        if parameter is not recognized or null.
     */
    
    public static final int logRollStringToType(String logRollString)
    {
        int logRollType = Logger.ROLLING_NEVER;
                
        if (logRollString != null)
        {           
            if (logRollString.equals(LOG_ROLL_MINUTELY))
                logRollType = Logger.ROLLING_MINUTELY;
            else if (logRollString.equals(LOG_ROLL_HOURLY))
                logRollType = Logger.ROLLING_HOURLY;
            else if (logRollString.equals(LOG_ROLL_HALFDAILY))
                logRollType = Logger.ROLLING_HALF_DAILY;
            else if (logRollString.equals(LOG_ROLL_DAILY))
                logRollType = Logger.ROLLING_DAILY;
            else if (logRollString.equals(LOG_ROLL_WEEKLY))
                logRollType = Logger.ROLLING_WEEKLY;
            else if (logRollString.equals(LOG_ROLL_MONTHLY))
                logRollType = Logger.ROLLING_MONTHLY;
        }
        
        return logRollType;
    }
    
    //---------------------------------------------------------------------
    
    public static final int MAX_LOGIN_ATTEMPT_COUNT = 3;
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
                                                   
}
