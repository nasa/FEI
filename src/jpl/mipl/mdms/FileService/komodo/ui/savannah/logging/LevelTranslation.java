package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;

import jpl.mipl.mdms.utils.logging.L4JCustomLevel;

import org.apache.log4j.Level;

/**
 * <b>Purpose:</b>
 * Translates priority levels from one framework to that
 * used by the <code>LogEntry</code> class.
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
 * 12/03/2004        Nick             Initial Release
 * 09/27/2005        Nick             Added Benchmark level
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: LevelTranslation.java,v 1.3 2007/05/03 22:39:53 awt Exp $
 *
 */

public class LevelTranslation
{
    public static int translate(int level)
    {
        int tLevel = LogEntry.LEVEL_UNKNOWN;
        
        switch (level)
        {
            case Level.FATAL_INT:
                tLevel = LogEntry.LEVEL_FATAL;
                break;
            case Level.ERROR_INT:
                tLevel = LogEntry.LEVEL_ERROR;
                break;
            case Level.WARN_INT:
                tLevel = LogEntry.LEVEL_WARN;
                break;
            case Level.INFO_INT:
                tLevel = LogEntry.LEVEL_INFO;
                break;
            case L4JCustomLevel.BENCH_INT:
                tLevel = LogEntry.LEVEL_BENCHMARK;
                break;
            case Level.DEBUG_INT:
                tLevel = LogEntry.LEVEL_DEBUG;
                break;
            case Level.TRACE_INT:
                tLevel = LogEntry.LEVEL_TRACE;
                break;      
        
        }
        
        return tLevel;
    }
    
    
}
