package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.client.CMD;

/**
 * Default implementation of the registry strategy, which
 * always returns false for <code>accept()</code>.
 */

public class NullAcceptStrategy implements
                                   FileEventHandlerAcceptStrategy
{    
    //---------------------------------------------------------------------
    
    public void initialize(Map options, String actionId) throws IllegalArgumentException
    {             
    }
    
    //-----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerAcceptStrategy#accept()
     */
    
    public boolean accept()
    {
        return false;
    }

    //---------------------------------------------------------------------
}
