/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * This class provides a record of a client session
 *
 * @author G. Turek
 * @version $Id: SessionLogger.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class SessionLogger {
    private static String _fileName = null;
    private static String _appName = null;
    private static long _msStop = 0;
    private static long _msStart = 0;
    private static PrintWriter _writer = null;

    /**
     * Set application name and initialize file writer
     *
     * @param aName name of application being logged.
     * @param fName name of output file.
     * @throws java.io.IOException when fail to open log file
     */
    public static void openLogFile(String aName, String fName)
        throws IOException {
        SessionLogger._appName = aName;
        SessionLogger._fileName = fName;
        SessionLogger._msStart = System.currentTimeMillis();
        SessionLogger._writer =
            new PrintWriter(
                new FileWriter(SessionLogger._fileName, true),
                true);
        SessionLogger._writer.println(
            "Log for "
                + SessionLogger._appName
                + " started at "
                + DateTimeUtil.getDateString(new Date(SessionLogger._msStart)));
    }

    /**
     * Close log file
     */
    public static void closeLogFile() {
        SessionLogger._msStop = System.currentTimeMillis();
        long msElapsed = SessionLogger._msStop - SessionLogger._msStart;
        SessionLogger._writer.println(
            "Log for "
                + SessionLogger._appName
                + " terminated at "
                + DateTimeUtil.getDateString(new Date(SessionLogger._msStop))
                + ", time elapsed "
                + msElapsed);
        SessionLogger._writer.close();
    }

    /**
     * Logs a single line entry
     *
     * @param message to be logged
     */
    public static void logEntry(String message) {
        SessionLogger._writer.println(message);
    }

    /**
     * Logs a partial entry (no CR)
     *
     * @param message to be logged
     */
    public static void logPartialEntry(String message) {
        SessionLogger._writer.print(message);
    }

}
