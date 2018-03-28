/*
 * Created on Nov 23, 2004
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;



/**
 * <b>Purpose:</b>
 * Record object containing information about log messages.
 *
 *   <PRE>
 *   Copyright 2004, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2004.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 11/23/2004        Nick             Initial Release
 * 09/27/2005        Nick             Added Benchmark level
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: LogEntry.java,v 1.4 2005/09/28 18:24:59 ntt Exp $
 *
 */

public class LogEntry
{
    //---------------------------------------------------------------------
    
    /** Level for Fatal log messages */
    public static final int LEVEL_FATAL   =   0;
    /** Level for Error log messages */
    public static final int LEVEL_ERROR   =   3;
    /** Level for Warning log messages */
    public static final int LEVEL_WARN    =   4;
    /** Level for Information log messages */
    public static final int LEVEL_INFO    =   6;
    /** Level for Benchmark log messages */
    public static final int LEVEL_BENCHMARK =   7;
    /** Level for Debug log messages */
    public static final int LEVEL_DEBUG   =   8;
    /** Level for Trace log messages */
    public static final int LEVEL_TRACE   =   10;
    /** Level for Unknown/unidentifier log messages */
    public static final int LEVEL_UNKNOWN = 100;
    
    protected static final String[] levelNames = new String[] { 
                    "Fatal", "Error", "Warning", "Information",
                    "Benchmark", "Debug", "Trace", "Unknown"};
    
    //---------------------------------------------------------------------
    
    /**
     * Translates a level from its int value to a string.
     * @param level Log level
     * @return String of the associated level name
     */
    
    public static String levelAsString(int level)
    {
        String levelName = levelNames[6];
        switch (level)
        {
            case LEVEL_FATAL:
                levelName = levelNames[0];
                break;
            case LEVEL_ERROR:
                levelName = levelNames[1];
                break;
            case LEVEL_WARN:
                levelName = levelNames[2];
                break;
            case LEVEL_INFO:
                levelName = levelNames[3];
                break;
            case LEVEL_BENCHMARK:
                levelName = levelNames[4];
                break;
            case LEVEL_DEBUG:
                levelName = levelNames[5];
                break;
            case LEVEL_TRACE:
                levelName = levelNames[6];
                break;      
        }
        return levelName;
    }
    
    //---------------------------------------------------------------------
    
    String _message;  //getRenderedMessage
    String _levelStr;
    int    _level;    //getLevel.toString
    String _location;
    long   _timestamp;
    Throwable _throwable;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param message Log message
     * @param time Timestamp
     * @param level Log level
     */
    
    public LogEntry(String message, long time, int level)
    {
        this._message   = message;
        this._level     = level;
        this._timestamp = time;
        this._levelStr  = LogEntry.levelAsString(this._level);
        this._location  = null;
        this._throwable = null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the location information for this log entry, identifying from 
     * where the message was logged
     * @param classname Name of the class file
     * @param methodname Name of the method/procedure
     * @param filename Name of the file
     * @param lineNumber Line of the file
     */
    
    public void setLocation(String classname, String methodname,
                            String filename,  String lineNumber)
    {
        this._location = classname + "."+methodname + 
                         "(" + filename + ":" + lineNumber + ")";
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the location information for this log entry, identifying from 
     * where the message was logged.
     * @param location String representing log location.
     */
    
    public void setLocation(String location)
    {
        this._location = location;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the log level.
     * @return log level, one of LEVEL_{FATAL,ERROR,WARN,INFO,DEBUG,
     *         TRACE,UNKNOWN} as defined in this class.
     */
    
    public int getLevel()
    {
        return this._level;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the level name as a string
     * @return level name
     */
    
    public String getLevelString()
    {
        return this._levelStr;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns this log entry's message
     * @return message
     */
    
    public String getMessage()
    {
        return this._message;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns this log entry's location information
     * @return location, possibly null.
     */
    
    public String getLocation()
    {
        return this._location;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the timestamp of this log entry
     * @return timestamp
     */
    
    public long getTimestamp()
    {
        return this._timestamp;
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Override of Object.toString, prints this entry in format of
     * levelName::message
     * @return string representation of this log entry
     */
    
    public String toString()
    {
        return this._levelStr+"::"+this._message;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the Throwable associated with this log entry.  Usually used
     * with error conditions.
     * @param throwable Instance of Throwable 
     */
    
    public void setThrowable(Throwable throwable)
    {
        this._throwable = throwable;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the Throwable associated with this log entry.
     * @param throwable Instance of Throwable, or null
     */
    
    public Throwable getThrowable()
    {
        return this._throwable;
    }
    
    //---------------------------------------------------------------------
    
}
