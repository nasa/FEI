/*
 * Created on Mar 9, 2006
 */
package jpl.mipl.mdms.FileService.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <b>Purpose:</b>
 * Date time formatter is responsible for parsing and formatting between 
 * strings and dates.  During instantiation, a format string is compared
 * with known format ids.  If a match is found, then the corresponding 
 * format pattern will be used.  If no match is found, then the string 
 * is assumed to be a format pattern and is used accordingly.  If the
 * format string is null, then a default pattern is used.
 * <BR>
 * Currently, the default format is CCSDSA (yyyy-MM-ddTHH:mm:ss.SSS).
 * 
 *   <PRE>
 *   Copyright 2006, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2006.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 03/09/2006        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DateTimeFormatter.java,v 1.5 2006/08/23 19:02:14 ntt Exp $
 *
 */

public class DateTimeFormatter
{
    public static final String FORMAT_UTC_ID         = "utc";
    public static final String FORMAT_CCSDSA_ID      = "ccsdsa";
    public static final String FORMAT_UTC_PATTERN    = "yyyy'-'DDD'T'HH:mm:ss.SSS";
    public static final String FORMAT_CCSDSA_PATTERN = "yyyy'-'MM'-'dd'T'HH:mm:ss.SSS";
    public static final String DEFAULT_FORMAT        = FORMAT_CCSDSA_ID;
    
    protected boolean _hackEnabled = true;
    protected boolean _stripQuotes = true;
    protected String  _formatString = null;
    protected SimpleDateFormat _format = null;
    
    //---------------------------------------------------------------------
    
    /**
     * Instatiates new formatter using default format pattern.
     */
    
    public DateTimeFormatter()
    {
        this(null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Instantiates new formatter using format parameter.  This parameter
     * will be checked with known ids before applied as a pattern.  Null
     * value implies that default should be used.
     * @param format Date format string
     */
    
    public DateTimeFormatter(String format)
    {
        this._formatString = (format != null) ? format : DEFAULT_FORMAT;
        this._format = generateFormatter(this._formatString);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Performs the comparison of the format parameter to prepare 
     * a new simple date format instance.
     * @param formatStr Format string (pattern or id)
     * @return SimpleDateFormat created based on parameter
     */
    
    protected SimpleDateFormat generateFormatter(String formatStr)
    {
        SimpleDateFormat format;
    
        //if null, then use ccsdas as default
        if (formatStr == null)
        {
            formatStr = DEFAULT_FORMAT;
        }
        
        //check for string identifiers first, else assume string is format
        if (formatStr.equalsIgnoreCase(FORMAT_UTC_ID))
        {
            format = new SimpleDateFormat(FORMAT_UTC_PATTERN);
        }
        else if (formatStr.equalsIgnoreCase(FORMAT_CCSDSA_ID))
        {
            format = new SimpleDateFormat(FORMAT_CCSDSA_PATTERN);
        }
        else
        {
            format = new SimpleDateFormat(formatStr);
        }            
        format.setLenient(true);
    
        return format;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Converts string representation of date to a Date object using
     * the formatters pattern.
     * @param dateAsString String representation of date
     * @return Date corresponding to parameter.
     * @throws ParseException if string could not be parsed.
     */
    
    public Date parseDate(String dateAsString) throws ParseException
    {
        if (this._stripQuotes)
            dateAsString = stripQuotes(dateAsString);    
        
        //hack relies on DateTimeUtil which attempts to complete 
        //a date fragment with the remaining time portion
        if (_hackEnabled) 
        {
            if (this._formatString.equalsIgnoreCase(FORMAT_CCSDSA_ID))
            {
                return DateTimeUtil.getCCSDSADate(dateAsString);
            }
            else if (this._formatString.equalsIgnoreCase(FORMAT_UTC_ID))
            {
                return DateTimeUtil.getUTCDate(dateAsString);
            }
        }
        return this._format.parse(dateAsString);        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Converts Date representation to a string using the
     * formatters pattern.
     * @param date Date to be converted to String representation
     * @return String corresponding to parameter.
     */
    
    public String formatDate(Date date)
    {
        return this._format.format(date);
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the format string of this formatter.
     * @return Format string or id, null if not set.
     */
    
    public String getFormatString()
    {
        return this._formatString;
    }
    

    //---------------------------------------------------------------------
    
    /**
     * Strips quotes off of entry if found
     */
    
    protected String stripQuotes(String entry)
    {
        String value = entry;
        
        if ((entry.startsWith("\"") && entry.endsWith("\"")) ||
            (entry.startsWith("'") && entry.endsWith("'")))
        {
            value = entry.substring(1, entry.length()-1);
        }
        
        return value;
    }
    
    //---------------------------------------------------------------------
}
