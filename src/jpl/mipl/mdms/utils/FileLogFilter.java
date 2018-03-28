/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.utils;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * This Logger Filter is designed to filter out messages with just a simple '\n'.
 * The fact is the standard logger inserts timestamp for every messages that is 
 * being logged.  When the developer only wants to write a '\n' to the console, 
 * the message should not be logged to the log file.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: FileLogFilter.java,v 1.3 2003/09/04 23:13:04 rap Exp $
 */
public class FileLogFilter implements Filter {

    /**
     * Method to override the Filter's isLoggable method to remove any messages with 
     * just a single '\n'.
     * @param record The LogRecord
     * @return false when the log message only has a single '\n' character.
     * @see java.util.logging.LogRecord
     * @see Filter#isLoggable(LogRecord)
     */
    public boolean isLoggable(LogRecord record) {
        if (record.getMessage().equals("\n"))
            return false;
        return true;
    }
}
