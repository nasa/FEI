package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;


/**
 * <b>Purpose:</b>
 * Interface for log message publishers, used as an interface between a log entry
 * source and a log entry target.
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
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: LogMessagePublisher.java,v 1.2 2005/01/25 00:16:51 ntt Exp $
 *
 */

public interface LogMessagePublisher
{
    
    //---------------------------------------------------------------------
    
    /**
     *  Adds listener for property change of model.
     *  @param l Object implementing the LogMessageListener interface 
     *           to be added
     */
    
    public void addLogMessageListener(LogMessageListener l);
    
    //---------------------------------------------------------------------
    
    /**
     *  Removes listener for property change of model.
     *  @param l Object implementing the LogMessageListener interface
     *           to be removed
     */
    
    public void removeLogMessageListener(LogMessageListener l); 
        
    //---------------------------------------------------------------------
    
    /**
     * Adds a new entry to the queue.  If new size exceeds the limit, then
     * oldest message (by order of receival) is discarded.
     * @param entry Log entry to be added
     */
    
    public void publish(LogEntry entry);
    
    //---------------------------------------------------------------------
    
    /**
     * Removes all queue entries and resets limit to default.
     */
    
    public void reset();
    
    //---------------------------------------------------------------------
}
