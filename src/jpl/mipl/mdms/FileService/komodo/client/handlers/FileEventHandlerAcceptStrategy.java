package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Map;

/**
 * <B>Purpose:<B>
 * Interface for classes that determine what handlers should
 * be loaded based upon the implementations accept()
 * strategy.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileEventHandlerAcceptStrategy.java,v 1.3 2009/08/07 15:53:55 ntt Exp $
 *
 */
public interface FileEventHandlerAcceptStrategy
{

    /**
     * Initializes instance of handler factory accept strategy using argument 
     * options and actionId.
     * @param options Argument options from client
     * @param actionId Id of operation
     * @throws IllegalArgumentException if a required argument is
     * missing from options map.
     */

    public abstract void initialize(Map options, String actionId)
                                   throws IllegalArgumentException;

    
    
    /**
     * Returns true if strategy determined that handlers should be loaded.
     * @return True to load, false otherwise
     */

    public abstract boolean accept();

}