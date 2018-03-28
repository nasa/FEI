/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Debug class for debug messages to a file and/or STDERR
 *
 * @author G. Turek
 * @version $Id: Debug.java,v 1.3 2003/09/09 00:32:34 rap Exp $
 */
public class Debug {
    private static boolean toFile = false;
    private static boolean toStdErr = true;
    private static String _debugFile = "./debug.log";
    private static long _msStop = 0;
    private static long _msStart = 0;
    private static BufferedWriter _writer = null;

    /**
     * Enable/disable output to file.
     *
     * @param flag if true enable output to file
     */
    public static synchronized void toFile(boolean flag) {
        Debug.toFile = flag;
        if (Debug.toFile) {
            try {
                Debug._writer =
                    new BufferedWriter(new FileWriter(Debug._debugFile, true));
            } catch (IOException e) {
                Debug.toFile = false;
                Debug.toStdErr = true;
                System.err.println(
                    "Debug: Couldn't open " + Debug._debugFile + " for write.");
            }
        } else {
            try {
                if (Debug._writer != null)
                    Debug._writer.close();
            } catch (IOException e) {
                System.err.println("Debug: Couldn't close " + Debug._debugFile);
            }
        }
    }

    /**
     * Accessor method to return true if write to file is enabled.
     *
     * @return true if write to file
     */
    public static synchronized boolean toFile() {
        return Debug.toFile;
    }

    /**
     * Enable/disable output to stderr.
     *
     * @param flag if true enable output to stderr
     */
    public static synchronized void toStdErr(boolean flag) {
        Debug.toStdErr = flag;
    }

    /**
     * Accessor method for write to standard error flag.
     *
     * @return true if write to standard error is enabled.
     */
    public static synchronized boolean toStdErr() {
        return Debug.toStdErr;
    }

    /**
     * Set the output file name. Only valid when toFile is set to true.
     *
     * @param fileName Name of output file.
     */
    public static synchronized void setDebugFileName(String fileName) {
        Debug._debugFile = fileName;
    }

    /**
     * Logs a single debug message.
     *
     * @param msg Message to be logged.
     */
    public static synchronized void message(String msg) {
        try {
            if (Debug.toFile)
                Debug._writer.write(msg + "\n");
            if (Debug.toStdErr)
                System.err.println(msg);
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        }
    }

    /**
     * Method to log a message with associated exception.
     *
     * @param msg debug
     * @param ex exception object
     */
    public static synchronized void message(String msg, Exception ex) {
        if (msg == null)
            return;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.close();
        Debug.message(msg + ": " + ex.getMessage() + sw.toString());
    }

    /**
     * Logs the start time in milliseconds including a custom message.
     *
     * @param msg Custom message to be added to the log entry.
     */
    public static void startTime(String msg) {
        Debug._msStart = System.currentTimeMillis();
        if (msg.compareTo("") != 0) {
            Debug.message("Timer start time " + Debug._msStart + " - " + msg);
        }
    }

    /**
     * Logs the stop time of an activity including a custom message.
     *
     * @param msg Custom message to be recorded with the log entry.
     */
    public static void stopTime(String msg) {
        Debug._msStop = System.currentTimeMillis();
        if (msg.compareTo("") != 0) {
            Debug.message("Timer stop time " + Debug._msStop + " - " + msg);
        }
    }

    /**
     * Calculates the delta between the start and stop times and enters
     * that information in to the log.
     *
     * @param msg Custom message to be added to the log entry.
     * @return Value containing the elapsed time.
     */
    public static long printElapsedTime(String msg) {
        Debug.message(
            "Timer elapsed time = "
                + (Debug._msStop - Debug._msStart)
                + " - "
                + msg);
        return Debug._msStop - Debug._msStart;
    }
}
