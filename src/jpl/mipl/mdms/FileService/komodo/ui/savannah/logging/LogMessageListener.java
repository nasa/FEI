/*
 * Created on Nov 30, 2004
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;


/**
 * <b>Purpose:</b>
 * Interface for log message queue listeners.
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
 * @version $Id: LogMessageListener.java,v 1.1 2004/12/04 02:13:39 ntt Exp $
 *
 */

public interface LogMessageListener
{
    /**
     * Called when a new log entry is to be published to listener
     * @param logEntry Entry being published
     */
    
    public void newLogEntry(LogEntry logEntry);
}
