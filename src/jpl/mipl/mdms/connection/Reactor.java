/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.connection;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import jpl.mipl.mdms.utils.MDMS;

/**
 * Implementation of the Reactor design pattern [POSA2] using Java 
 * NIO features for event demultiplexing and dispatching service 
 * requests that are delivered to an application from one or 
 * more clients.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: Reactor.java,v 1.4 2003/09/09 00:32:36 rap Exp $
 */
public class Reactor implements Runnable {
    // reference to a Selector that is used for I/O event dispatching.
    private final Selector _selector;

    // the timeout value in msec.  This gives the Reactor a way to 
    // return from its infinite select() loop.
    private long _timeout = 0;

    /**
     * Default constructor without timeout.  This Reactor will on block on
     * listening to events forever.
     * 
     * @throws IOException when I/O failure
     */
    public Reactor() throws IOException {
        this(0);
    }

    /**
     * Constructor that takes a timeout value.  This Reactor will block upto
     * the specified time value when no I/O event occurs.
     * 
     * @param timeout The timeout value in msec.
     * @throws IOException when I/O failure
     */
    public Reactor(long timeout) throws IOException {
        this._timeout = timeout;
        this._selector = Selector.open();
    }

    /**
     * Method to register a service handler to the Reactor with the specified 
     * event type.  This method uses 'double-dispatching' [GoF95] to obtain 
     * the service handler's internal communication handle.
     * 
     * @param handler The service handler object
     * @param eventType The event type defined in SelectionKey.
     * @return true if registration was successful.
     * @see jpl.mipl.mdms.connection.ServiceHandler
     * @see java.nio.channels.SelectionKey
     */
    public boolean register(ServiceHandler handler, int eventType) {
        // obtains the service handler's internal communcation handle.
        SelectableChannel handle = handler.getHandle();
        try {
            // register's the Selector with the specified event type
            // to the communication handle.
            SelectionKey key = handle.register(this._selector, eventType);

            // attach the service handler to the generated key.
            key.attach(handler);

            // updates the handler's internal key reference.
            handler.setKey(key);

            // notify the Selector on the newly subscribed channel and event.
            this._selector.wakeup();
            return true;
        } catch (ClosedChannelException ex) {
            MDMS.ERROR(ex.getMessage());
            return false;
        }
    }

    /**
     * Method to register a communication handle with an associated service handler.
     * 
     * @param handle The communication handle.
     * @param handler The service handler
     * @param eventType The event type as defined in SelectionKey class.
     * @return the selection key reference.
     */
    public SelectionKey register(
        SelectableChannel handle,
        ServiceHandler handler,
        int eventType) {
        try {
            // register the selector, the event type, and the service handler 
            // to the communication handle.
            SelectionKey key =
                handle.register(this._selector, eventType, handler);

            // attache the service handler to the selection key.
            key.attach(handler);

            // notify the selector on the new registration.
            this._selector.wakeup();
            return key;
        } catch (ClosedChannelException ex) {
            MDMS.ERROR(ex.getMessage());
            return null;
        }
    }

    /**
     * Method to unregister a service handler from the Reactor.
     * @param handler The service handler to be unregistered.
     * @return true when unregistration was successful.
     */
    public boolean remove(ServiceHandler handler) {
        // obtain reference to the service handler's internal key.
        SelectionKey key = handler.getKey();
        if (key != null && key.isValid()) {
            // unregister the key from the selector and 
            // close all communication channels.
            key.cancel();

            // triggers the hook method in service handler to cleanup.
            handler.deactivate();

            // notify the selector on the latest unregistration.
            this._selector.wakeup();
            return true;
        } else
            // if we already have an invalid key, then clear it
            // from the service handler.
            handler.setKey(null);
        return false;
    }

    /**
     * The hook method to activate the Reactor.  This method may block 
     * forever if timeout was initialized to zero.  This method is also
     * required by the Runnable interface.
     * @see java.lang.Runnable
     */
    public void run() {
        long timeout = this._timeout;
        boolean checkTimeout = timeout > 0 ? true : false;

        try {
            // block until we get interrupted.
            while (!Thread.interrupted()) {
                // keeps track of the starting time used by checkTimeout.
                long startTime = System.currentTimeMillis();

                // select for any I/O events.  This method will block forever
                // if timeout__ is zero.
                this._selector.select(this._timeout);

                // if the selector got waken due to a wakeup() call or 
                // an I/O event, then check to see if we need to verify the 
                // timeout.
                if (checkTimeout) {
                    timeout -= (System.currentTimeMillis() - startTime);

                    // if timeout occurs, then cleanup and shutdown the selector.
                    if (timeout <= 0) {
                        this._cleanup();
                        return;
                    }
                }

                // if not timeout, then begin dispatch the service handlers.
                Set selected = this._selector.selectedKeys();
                Iterator keys = selected.iterator();
                while (keys.hasNext())
                    this._dispatch((SelectionKey) (keys.next()));
                selected.clear();
            }
        } catch (IOException ex) {
            MDMS.ERROR(ex.getMessage());
        }
    }

    /**
     * Internal method to dispatch the service handler that is associated with 
     * the input selection key.
     * 
     * @param key The selection key that is returned by the Selector.
     */
    protected void _dispatch(SelectionKey key) {
        // obtain reference to the service handler object.
        ServiceHandler handler = (ServiceHandler) key.attachment();

        // actives the service handler by invoking its hook method.
        if (handler != null && !handler.activate(key))
            // if activation failed, then invoke the cleanup hook method.
            handler.deactivate();
    }

    /**
     * Internal method to cleanup the Reactor and shutdown the selector.
     * 
     * @throws IOException when I/O failure
     */
    protected void _cleanup() throws IOException {
        Iterator keys = this._selector.keys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = (SelectionKey) keys.next();
            keys.remove();
            ServiceHandler handler = (ServiceHandler) key.attachment();
            handler.deactivate();
        }
        this._selector.close();
    }
}
