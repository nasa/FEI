/*
 * Created on Jul 16, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.client;

import jpl.mipl.mdms.FileService.komodo.services.query.api.QRequest;

public interface QServiceProxy
{

    /**
     * The server proxy run method. <br>
     * Dispatches based on thread name for servicing general requests and
     * control requests, respectively.
     */

    public void run();

    /**
     * Method to put a request to the request queue
     * 
     * @param profile the profile record specifies a request command.
     * @return the transaction id
     */
    public int put(QRequest profile);

    /**
     * Method to insert a request to the head of the request queue. Used for
     * commands such as close file type.
     * 
     * @param profile the profile record specifies a request command.
     * @return the transaction id
     */
    public int putExpedited(QRequest profile);

    /**
     * Returns string representing this server proxy, including its id.
     * @return This object's string representation.
     */

    public String toString();

    /**
     * Returns true if current running request is a query request.
     * @return True if query is running, false otherwise
     */

    public boolean isQueryRunning();

}