package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.client.CMD;

/**
 * <B>Purpose:<B>
 * Implementation of FileEventHandlerAcceptStrategy that enables
 * handlers for replication.  The options passed in via initialize
 * is examined for all necessary properties, (servergroup, filetype,
 * output dir) and passes if the replication option is set.
 * 
 * @see CMD for names of properties.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ReplicationAcceptStrategy.java,v 1.4 2010/01/13 19:01:10 ntt Exp $
 *
 */
public class ReplicationAcceptStrategy implements FileEventHandlerAcceptStrategy
{   
    boolean accept = false;

    //-----------------------------------------------------------------------
    
    public ReplicationAcceptStrategy()
    {        
    }
    
    //-----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerAcceptStrategy#initialize(java.util.Map, int)
     */
    
    public void initialize(Map options, String actionId) throws IllegalArgumentException
    {        
        String sg  = (String) options.get(CMD.SERVERGROUP);
        if (sg == null)
            throw new IllegalArgumentException("Options parameter does not " +
                    "contain value for '"+CMD.SERVERGROUP+"'");

        
        String ft  = (String) options.get(CMD.FILETYPE);
        if (ft == null)
            throw new IllegalArgumentException("Options parameter does not " +
                    "contain value for '"+CMD.FILETYPE+"'");
        
        
        String dir = (String) options.get(CMD.OUTPUT);        
        if (dir == null)
            dir = System.getProperty("user.dir");
        
        boolean replicateEnabled = options.containsKey(CMD.REPLICATE) ? 
                                    ((Boolean) options.get(CMD.REPLICATE)).booleanValue():
                                    false;
    
        this.accept = replicateEnabled;        
    }
    
    //-----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerAcceptStrategy#accept()
     */
    
    public boolean accept()
    {
        return this.accept;
    }
    
    //-----------------------------------------------------------------------
}
