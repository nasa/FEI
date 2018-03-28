/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.utils;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * The Logger Formatter class is used by the console logger to remove all the 
 * timestamps and simply writes the log message to STDERR followed by a newline
 * character.  This is designed as part of the standard logger integration to 
 * enable formatting of log message per log handler.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: RawFormatter.java,v 1.3 2003/09/04 23:13:05 rap Exp $
 */
public class RawFormatter extends Formatter {

    /**
     * Method to override the existing Formatter's format method to 
     * intercept log message and remove insertion of timestamps for 
     * console logging.
     * @param record The input LogRecord.
     * @return The raw formatted string.
     * @see Formatter#format(LogRecord)
     */
    public String format(LogRecord record) {
        return record.getMessage() + "\n";
    }
}
