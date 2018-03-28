/*
 * Created on Aug 7, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.api;

public interface Closeable
{
    /**
     * Returns true if the close() method has been invoked
     * @return Closed state
     */
    public boolean isClosed();
    
    /**
     * Closes the implementation, indicating it is no longer active.
     */
    public void close();
}
