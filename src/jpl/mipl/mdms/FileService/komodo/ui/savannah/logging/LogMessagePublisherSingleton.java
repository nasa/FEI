package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <b>Purpose:</b>
 * Singleton message publisher class for logging messages.  
 * Implementors of the LogMessageListener interface register themselves as
 * listeners, and are notified of any new log entries received by this
 * object.
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
 * 11/30/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: LogMessagePublisherSingleton.java,v 1.2 2004/12/10 03:46:10 ntt Exp $
 *
 */

public class LogMessagePublisherSingleton implements LogMessagePublisher
{
    private final String __classname = LogMessagePublisherSingleton.class.toString();
    
    public static final int DEFAULT_LIMIT = 300;
    private static LogMessagePublisherSingleton _instance = null;
    
    private final Object _syncLock  = new Object();
    private final Set    _listeners = new HashSet();
    
    //---------------------------------------------------------------------
    
    /**
     * Private constructor.
     */
    
    private LogMessagePublisherSingleton()
    {
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns instance of this class.
     */
    
    public static LogMessagePublisherSingleton instance()
    {
        if (_instance == null)
        {
            _instance = new LogMessagePublisherSingleton();
        }
        
        return _instance;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Adds listener to consumers list.
     *  @param l Object implementing the LogMessageQueueListener interface 
     *           to be added
     */
    
    public void addLogMessageListener(LogMessageListener l)
    {       
        synchronized(_syncLock) {
            this._listeners.add(l);  
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Removes listener from consumers list.
     *  @param l Object implementing the LogMessageQueueListener interface
     *           to be removed
     */
    
    public void removeLogMessageListener(LogMessageListener l)
    {       
        synchronized(_syncLock) {
            this._listeners.remove(l);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a new entry to the queue.  If new size exceeds the limit, then
     * oldest message (by order of receival) is discarded.
     * @param entry Log entry to be added
     */
    
    public void publish(LogEntry entry)
    {
        synchronized(_syncLock) {
            Iterator it = this._listeners.iterator();
        
            while (it.hasNext())
            {
                LogMessageListener l = (LogMessageListener) it.next();
                l.newLogEntry(entry);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Removes all queue entries and resets limit to default.
     */
    
    public void reset()
    {
        synchronized(_syncLock)
        {
            this._listeners.clear();
        }
    }
        
    //---------------------------------------------------------------------
    
}
