/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * MDMS logger class to log messages to a file and/or standard out.
 * The main logging function <PRE>logMessage</PRE> is overloaded to take
 * either an object that represents the caller or a string that represents
 * the class name of the caller (if the caller is a static method). <BR>
 * The methods <UL> <LI>startTime</LI> <LI>stopTime</LI>
 * <LI>printElapsedTime</LI> </UL> are used for timing.
 * These methods should be called in sequence or invalid timing results will
 * be  generated.  Note: Derived from Joe Chavez's util/SysLogger class.
 *
 * @author Jeff Jacobson
 * @version $Id: Logger.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class Logger {

    //Set default to false, unless -D "LOG__toFile".
    private static boolean _toFile = Boolean.getBoolean("LOG__toFile");

    //Set default to false, unless -D "STDERR_DISABLE".
    private static boolean _toStdErr = Boolean.getBoolean("STDERR_DISABLE");

    //Default name of log file path.
    private static String _logFileName = "./system.log";

    //Define severity levels.
    public static final int INFO = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;
    public static final int FATAL = 4;

    /**
     * Enable/disable output to file.
     *
     * @param enable if true then enable output to file.
     * if false then disable output to file.
     */
    public static synchronized void enableToFile(boolean enable) {
        Logger._toFile = enable;
    }

    /**
     * Accessor method to return log to file flag
     *
     * @return true if log to file is enabled
     */
    public static synchronized boolean getEnableToFile() {
        return Logger._toFile;
    }

    /**
     * Enable/disable output to stderr.
     *
     * @param enable if true then output to stderr.
     * if false then disable output to file.
     */
    public static synchronized void enableToStdErr(boolean enable) {
        Logger._toStdErr = enable;
    }

    /**
     * Accessor method to return log to standard erro flag
     *
     * @return true if log to standard error is enabled
     */
    public static synchronized boolean getEnableToStdErr() {
        return Logger._toStdErr;
    }

    /**
     * Set the output file name. Only valid when _toFile is set to true.
     *
     * @param name the name of output file.
     */
    public static synchronized void setLogFilePath(String name) {
        Logger._logFileName = name;
    }

    /**
     * Return the text for the severity level.
     *
     * @param level the severity level associated with the message
     * @return Severity level text.
     */
    public static String getSeverityName(int level) {
        switch (level) {
            case INFO :
                return "INFORMATION";
            case WARNING :
                return "WARNING";
            case 3 :
                return "ERROR";
            case 4 :
                return "FATAL";
            default :
                return "UNKNOWN LEVEL: " + level;
        }
    }

    // Stop time.
    private static long _msStop = 0;

    // Start time.
    private static long _msStart = 0;

    /**
     * Logs a single message. Pass the 'this' reference to add the class
     * name to the output message.
     *
     * @param caller the class of the caller, pass 'this' to have the caller
     * @param type an arbitrary message type, e.g., "SECURITY", "Program", "Trace".
     * @param level the severity levels: Logger.INFO, Logger.WARNING,
     * Logger.ERROR, Logger.FATAL.
     * @param msg Message to be logged.
     */
    public static synchronized void logMessage(
        Object caller,
        String type,
        int level,
        String msg) {
        Logger.logMessage(caller.getClass().getName(), type, level, msg);
    }

    /**
     * Logs a single message. Use this method call within static methods
     * that do not have a 'this' reference.
     *
     * @param reporter the string that identifies the reporting entity.
     * @param type an arbitrary message type, e.g., "SECURITY", "Program", "Trace".
     * @param level the severity levels: Logger.INFO, Logger.WARNING,
     * Logger.ERROR, Logger.FATAL.
     * @param msg Message to be logged.
     */
    public static synchronized void logMessage(
        String reporter,
        String type,
        int level,
        String msg) {
        Date theTime = new Date(System.currentTimeMillis());
        String header =
            reporter
                + " "
                + type
                + " "
                + getSeverityName(level)
                + " "
                + theTime
                + "\n";
        try {
            if (Logger._toFile) {
                // XXXX Do I really want to open this file each time????
                FileWriter fileWriter;
                fileWriter = new FileWriter(Logger._logFileName, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(header + msg);
                bufferedWriter.newLine();
                bufferedWriter.close();
            }

            if (Logger._toStdErr)
                System.err.println(header + msg);
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }

    /**
     * Record to the log devices (file, standard error) the string contained
     * in <em>msg</em>.
     * The message is timestamped with the current system time. The class
     * name of the <em>caller</em> is also recorded in the final message. The
     * message and stack trace from <em>ex</em> is appended to the final messge.
     *
     * @param caller the <em>this</em> pointer of the caller.
     * @param type an arbitrary message type, e.g., "SECURITY", "Program", "Trace".
     * @param level the severity levels: Logger.INFO, Logger.WARNING,
     * Logger.ERROR, Logger.FATAL.
     * @param msg Message to log.
     * @param ex the current Exception object of the caller.
     */
    public static synchronized void logMessage(
        Object caller,
        String type,
        int level,
        String msg,
        Exception ex) {
        if (msg == null) {
            return;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.close();
        String s = sw.toString();
        Logger.logMessage(
            caller.getClass().getName(),
            type,
            level,
            msg + ": " + ex.getMessage() + s);
    }

    /**
     * Record to the enabled log devices (file, standard ouput) the string
     * contained in <em>msg</em>.  The message is timestamped with the current
     * system time. The class name of the <em>caller</em> is also recorded in the
     * final message. The message and stack trace from <em>ex</em> is
     * appended to the final messge.
     *
     * @param type an arbitrary message type,
     * e.g., "SECURITY", "Program", "Trace".
     * @param level the severity levels: Logger.INFO, Logger.WARNING,
     * Logger.ERROR, Logger.FATAL.
     * @param msg the message to log.
     * @param ex The current Exception object of the caller.
     */
    public static synchronized void logMessage(
        String type,
        int level,
        String msg,
        Exception ex) {
        if (msg == null) {
            return;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.close();
        String s = sw.toString();
        Logger.logMessage(
            "Exception",
            type,
            level,
            msg + ": " + ex.getMessage() + s);
    }

    /**
     * Logs the start time in milliseconds including a custom message.
     *
     * @param reporter the string that identifies the reporting entity.
     * @param msg the custom message to be added to the log entry.
     */
    public static void startTime(String reporter, String msg) {
        Logger._msStart = System.currentTimeMillis();
        if (msg.compareTo("") != 0) {
            Logger.logMessage(
                reporter,
                "Timer",
                1,
                msg + ", time = " + Logger._msStart);
        }
    }

    /**
     * Logs the stop time of an activity including a custom message.
     *
     * @param reporter the string that identifies the reporting entity.
     * @param msg the custom message to be recorded with the log entry.
     */
    public static void stopTime(String reporter, String msg) {
        Logger._msStop = System.currentTimeMillis();
        if (msg.compareTo("") != 0) {
            Logger.logMessage(
                reporter,
                "Timer",
                1,
                msg + ", time = " + Logger._msStop);
        }
    }

    /**
     * Calculates the delta between the start and stop times and enters
     * that information in to the log.
     *
     * @param reporter the string that identifies the reporting entity.
     * @param msg the custom message to be added to the log entry.
     * @return the value containing the elapsed time.
     */
    public static long printElapsedTime(String reporter, String msg) {
        Logger.logMessage(
            reporter,
            "Timer",
            1,
            msg
                + ", elapsed time =, "
                + (Logger._msStop - Logger._msStart)
                + ", ms");
        return Logger._msStop - Logger._msStart;
    }
}
