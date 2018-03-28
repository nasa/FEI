package jpl.mipl.mdms.FileService.komodo.client.handlers;

/**
 * <B>Purpose:<B>
 * Interface for file event handler registrys.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileEventHandlerRegistryIF.java,v 1.2 2009/08/07 15:53:55 ntt Exp $
 *
 */
public interface FileEventHandlerRegistryIF
{

    /**
     * Returns a FileEventHandlerSet containing newly instantiated
     * handlers described in registry.
     * @return FileEventHandlerSet containing handlers
     */

    public FileEventHandlerSet getFileEventHandlers();

}