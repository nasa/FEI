package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;

/**
 * <b>Purpose:</b>
 * Model for recording and maintaining log history.  Accepts
 * property change listeners which will receive a message each
 * time the state of the model changes.  
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
 * 12/10/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahLogModel.java,v 1.4 2005/10/06 00:07:53 ntt Exp $
 *
 */

public class SavannahLogModel implements LogMessageListener
{

    private final String __classname = "SavannahLogModel";
    
    /** enables bean property change event handling */
    protected final PropertyChangeSupport _changes = new PropertyChangeSupport(
                                                                          this);
    
    /** Queue of log entries / events */
    protected final List _queue;
    
    /** Max number of message entries that can be queued */
    protected int _limit;
    
    public static final int DEFAULT_LIMIT = 600;
    
    public static final double DEFAULT_DELTA = .35;
    
    public final static String LOG_QUEUE_PROPERTY = "LOG_QUEUE";
    
    public final static String LOG_LIMIT_PROPERTY = "LOG_LIMIT";
    
    public final static String LOG_NEW_ENTRY = "LOG_NEW_ENTRY";
    
    //---------------------------------------------------------------------
    
    /**
     *  Constructor.
     */
    
    public SavannahLogModel()
    {
        this._limit = DEFAULT_LIMIT;
        this._queue = new Vector();
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes this class.  Called by constructor only. 
     */
    
    protected void init()
    {
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Adds listener for property change of model.
     *  Property names: "LOG_QUEUE", "LOG_LIMIT"
     *  @param l Object implementing the PropertyChangeListener interface 
     *           to be added
     */
    
    public void addPropertyChangeListener(PropertyChangeListener l)
    {        
        this._changes.addPropertyChangeListener(l);  
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Removes listener for property change of model.
     *  @param l Object implementing the PropertyChangeListener interface
     *           to be removed
     */
    
    public void removePropertyChangeListener(PropertyChangeListener l)
    {     
        this._changes.removePropertyChangeListener(l);   
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     *  Clears all entries from the transfer list. 
     */
    
    public void resetLogQueue()
    {
        this._queue.clear();
        fireQueueChange();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Alerts listerns of change to queue property.
     */
    
    protected void fireQueueChange()
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                _changes.firePropertyChange(LOG_QUEUE_PROPERTY, 
                                            null, _queue);
            }
        });
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Alerts listerns of change to log limit property.
     * @param oldLimit Old limit value
     * @param newLimit New limit value
     */
    
    protected void fireLimitChange(final int oldLimit, final int newLimit)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                _changes.firePropertyChange(LOG_LIMIT_PROPERTY, 
                                            new Integer(oldLimit), 
                                            new Integer(newLimit));
            }
        });
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Alerts listerns of new entry.
     * @param entry New log entry
     */
    
    protected void fireNewEntry(final LogEntry entry)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                _changes.firePropertyChange(LOG_NEW_ENTRY, null, entry);
            }
        });
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns copy of log message queue
     * @return List of LogEntry.
     */
    
    public List getLogEntries()
    {
        return this._queue;
        //return (List) ((Vector) this._queue).clone();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns count of log entries.
     * @return Log entry count
     */
    
    public int getNumEntries()
    {
        return this._queue.size();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the current message limit.
     * @return Current limit.
     */
    
    public int getMessageLimit()
    {
        return this._limit;
    }   
    
    //---------------------------------------------------------------------
    
    /**
     * Change the message limit allowed by the queue.  
     * @param limit Non-negative integer representing new limit
     * @throws IllegalArgumentException if limit is negative
     */
    
    public void setMessageLimit(int limit)
    {
        if (this._limit != limit)
        {
            if (limit < 0)
                throw new IllegalArgumentException(__classname
                        + "::setMessageLimit(): "
                        + " Limit must be non-negative integer.  Value = "
                        + limit);
            
            int oldLimit = this._limit;
            this._limit = limit;
            
            maintainLimit();
            this.fireLimitChange(oldLimit, this._limit);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Discards oldest entries until size is within limit.
     */
    
    protected void maintainLimit()
    {
        int numEntries = this._queue.size();
        int numDiscards = numEntries - this._limit;
        if (numDiscards > 0)
        {
            numDiscards += (int) (DEFAULT_DELTA * _limit);
            while (numDiscards > 0)
            {
                this._queue.remove(0);
                --numDiscards;
            }
            //fireQueueChange();
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Implementation of the LogMessageListener interface, alerts this
     * instance of new log entry.
     * @param new LogEntry instance
     */
    
    public void newLogEntry(LogEntry entry)
    {
        this._queue.add(entry);
        maintainLimit();
        fireNewEntry(entry);
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
}
