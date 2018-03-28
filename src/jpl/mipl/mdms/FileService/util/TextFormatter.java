/******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights re served
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 *****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.text.MessageFormat;
import java.util.Vector;

/**
 * This class formats string lists in columns
 *
 * @author G.Turek
 * @version $Id: TextFormatter.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class TextFormatter {
    /**
     * Formats a list of strings in <numcol> columns
     * Column width defaults to longest string length + 2
     *
     * @param numcol number of columns data should be formatted in
     * @param data vector of strings to be formatted
     * @return formatted string
     */
    public static String formatText(int numcol, Vector data) {
        String fmt;
        int nc = data.size();

        // Find longest command string
        int max = 0;
        int len;
        for (int i = 0; i < nc; i++) {
            len = ((String) data.elementAt(i)).length();
            if (len > max)
                max = len;
        }
        max += 2;

        // Constructor vector with equal length command strings
        Vector ndata = new Vector(nc);
        for (int i = 0; i < nc; i++) {
            fmt = (String) data.elementAt(i);
            while (fmt.length() < max) {
                fmt += " ";
            }
            ndata.add(fmt);
        }

        // Format output
        fmt = "";
        int ncol = numcol - 1;
        int j = 0;
        for (int i = 0; i < nc; i++) {
            fmt += "{" + Integer.toString(i) + "}";
            if (j == ncol)
                fmt += "\n";
            j++;
            if (j > ncol)
                j = 0;
        }
        return MessageFormat.format(fmt, ndata.toArray()).trim();
    }

    /**
     * Formats a list of strings in <numcol> columns
     *
     * @param numcol number of columns data should be formatted in
     * @param colwidth width of columns. If width less than longest string,
     * default to longest string length + 2
     * @param data vector of strings to be formatted
     * @return formatted string
     */
    public static String formatText(int numcol, int colwidth, Vector data) {
        String fmt;
        int nc = data.size();

        // Find longest command string
        int max = 0;
        int len;
        for (int i = 0; i < nc; i++) {
            len = ((String) data.elementAt(i)).length();
            if (len > max)
                max = len;
        }
        max += 2;

        if (colwidth > max)
            max = colwidth;

        // Constructor vector with equal length command strings
        Vector ndata = new Vector(nc);
        for (int i = 0; i < nc; i++) {
            fmt = (String) data.elementAt(i);
            while (fmt.length() < max) {
                fmt += " ";
            }
            ndata.add(fmt);
        }

        // Format output
        fmt = "";
        int ncol = numcol - 1;
        int j = 0;
        for (int i = 0; i < nc; i++) {
            fmt += "{" + Integer.toString(i) + "}";
            if (j == ncol)
                fmt += "\n";
            j++;
            if (j > ncol)
                j = 0;
        }
        return MessageFormat.format(fmt, ndata.toArray()).trim();
    }
}
