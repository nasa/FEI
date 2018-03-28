/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.connection;

import java.nio.channels.SelectableChannel;

/**
 * Defines the abstract factory interface for service handler object 
 * creation.
 * 
 * Note: This is the initial definition of this interface.  More method 
 * interfaces will be added in future releases. -- txh.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: ServiceHandlerFactory.java,v 1.4 2003/09/09 00:32:36 rap Exp $
 */
public interface ServiceHandlerFactory extends Runnable {

    /**
     * Method to create a service handler object.
     * @param reactor The input associated Reactor object reference.
     * @param handle The input selectable channel (handle) object reference.
     * @return Reference to a service handler object (may be null).
     * @see jpl.mipl.mdms.connection.Reactor
     * @see java.nio.channels.SelectableChannel
     */
    ServiceHandler createServiceHandler(
        Reactor reactor,
        SelectableChannel handle);

    /**
     * Mehtod to allow the factory object to cleanup any allocated resources.
     */
    void shutdown();

}
