/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.connection;

import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;

/**
 * Abstarct interfaces for the service handlers that are required by the 
 * Reactor and Acceptor-Connector framework.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: ServiceHandler.java,v 1.4 2003/09/09 00:32:36 rap Exp $
 */
public interface ServiceHandler extends Runnable {

    /**
     * Method invoked by the acceptor and connector to notify 
     * service objects to initialize it self.  This method is invoked
     * when the communication channel is established and the service 
     * object may 'decide' to register for additional I/O events on 
     * the established channel using the Reactor.
     * @see jpl.mipl.mdms.connection.Reactor
     */
    void open();

    /**
     * Method invoked by the Reactor to dispatch the subscribed 
     * I/O event.  For server-side objects, the input SelectionKey 
     * contains reference to an established communication channel (handle)
     * that the service object should store.
     * @param key The input selection key from Reactor.
     * @return true when activation was successful.
     * @see java.nio.channels.SelectionKey
     */
    boolean activate(SelectionKey key);

    /**
     * Method invoked by the Reactor during deactivation of the service object.
     * This is the hook method for the service object to implement its own 
     * cleanup process.
     * @return true when deactivation was successful.
     */
    boolean deactivate();

    /**
     * Access method to return reference to the internal selection key.
     * @return The selection key object reference (may also be null).
     * @see java.nio.channels.SelectionKey
     */
    SelectionKey getKey();

    /**
     * Method invoked by the Reactor during event registration phase.
     * @param key The input selection key reference.
     * @see java.nio.channels.SelectionKey
     */
    void setKey(SelectionKey key);

    /**
     * Access method to return the internal communication channel (handle).
     * @return The selectable channel object reference.
     * @see java.nio.channels.SelectableChannel
     */
    SelectableChannel getHandle();
}
