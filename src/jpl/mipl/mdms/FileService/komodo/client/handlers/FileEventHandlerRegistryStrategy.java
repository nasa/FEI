package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Map;

/**
 * Encapsulates the behavior used to determine whether or not
 * file event handler registry will be loaded.  Implementations
 * should check for necessary conditions in the initialize() method.
 * If conditions are met, then calls to getHandlers() should
 * return handlers associated with registry.  Else, an empty
 * handler set should be returned.
 * @author ntt
 *
 */
public interface FileEventHandlerRegistryStrategy
{
    
    //---------------------------------------------------------------------
    
    public void initialize(Map options, int actionId) 
                                throws IllegalArgumentException;    
    
    //---------------------------------------------------------------------
    
    public FileEventHandlerSet getHandlers();
    
    //---------------------------------------------------------------------
    
}
