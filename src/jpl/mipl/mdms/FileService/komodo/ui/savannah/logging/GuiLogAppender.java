/*
 * Created on Nov 23, 2004
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * <b>Purpose:</b>
 * Appender to be used with LogMessagePublisherSingleton to display 
 * log messages.
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
 * 12/10/2004        Nick             Added logger to allow polling for
 *                                    current debug or trace level.  In such a
 *                                    case, throwables are attached to log 
 *                                    entries for display.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: GuiLogAppender.java,v 1.2 2004/12/11 00:11:18 ntt Exp $
 *
 */

public class GuiLogAppender extends AppenderSkeleton
{

    protected LogMessagePublisherSingleton _publisher;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    
    public GuiLogAppender()
    {
        super();
        
        //create reference to the message queue
        _publisher = LogMessagePublisherSingleton.instance();
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Behavior for a new log event.  Creates a new log entry and sneds it
     * to a publisher where listeners will be notified.
     * @param LoggingEvent New log event
     */
    
    protected void append(LoggingEvent logEvent)
    {
        String message, levelStr;
        int level;
        long timestamp;
        LogEntry entry;
        
        //extract information of event to data struct
        message  = logEvent.getRenderedMessage();
        timestamp = logEvent.timeStamp;
        String lfjLevelStr = logEvent.getLevel().toString();
        int l4jLevel = logEvent.getLevel().toInt();
        level = LevelTranslation.translate(l4jLevel);
        
        //create new log entry
        entry    = new LogEntry(message, timestamp, level);

        //set entry location info
        String locInfo = logEvent.getLocationInformation().fullInfo;
        
        if (locInfo != null)
        {
            entry.setLocation(locInfo);
        }
        else
        {
            entry.setLocation(LocationInfo.NA);
        }
        

        ThrowableInformation throwInfo = logEvent.getThrowableInformation();
        if (throwInfo != null)
            entry.setThrowable(throwInfo.getThrowable());
        
        //add entry to queue
        this._publisher.publish(entry);
        
    }

    //---------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see org.apache.log4j.Appender#close()
     */
    public void close()
    {
        //release reference to queue
        this._publisher = null;
    }
    
    //---------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.apache.log4j.Appender#requiresLayout()
     */
    public boolean requiresLayout()
    {
        return false;
    }

    //---------------------------------------------------------------------
       
}
