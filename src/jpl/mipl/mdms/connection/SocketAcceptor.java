/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import jpl.mipl.mdms.utils.MDMS;

/**
 * Implementation of the Acceptor design pattern [POSA2] for initializing new
 * Service Handlers.  This is a complete reimplemention of the original 
 * SocketAcceptor class due to New I/O support in JDK1.4 with multiplex 
 * non-blocking communication establishment.  This implements the
 * ServiceHandler interface that is required by the Reactor framework.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: SocketAcceptor.java,v 1.4 2003/09/09 00:32:36 rap Exp $
 */
public class SocketAcceptor implements ServiceHandler {
    // internal server socket communication channel reference.
    private ServerSocketChannel _handle = null;

    // the requried Reactor reference.
    private final Reactor _reactor;

    // reference to the service handler factory object reference.
    private final ServiceHandlerFactory _factory;

    // the associated selection key used for non-blocking communication 
    // establishment.
    private SelectionKey _key;

    /**
     * Constructor to initiate the acceptor's subscription to the reactor on the 
     * specified TCP port.
     * @param reactor The Reactor reference.
     * @param factory The factory reference used by the acceptor to create 
     * service handler objects.
     * @param port The TCP communicaiton port number.
     * @throws IOException when I/O failure
     * @see jpl.mipl.mdms.connection.Reactor
     * @see jpl.mipl.mdms.connection.ServiceHandlerFactory
     */
    public SocketAcceptor(
        Reactor reactor,
        ServiceHandlerFactory factory,
        int port)
        throws IOException {
        this._reactor = reactor;
        this._factory = factory;

        // creates the server-side communication handle.
        this._handle = ServerSocketChannel.open();
        this._handle.socket().bind(new InetSocketAddress(port));

        // we want the channel to be non-blocking.
        this._handle.configureBlocking(false);

        // register this object to the Reactor on our communication channel with 
        // the specified accept event.
        this._reactor.register(this, SelectionKey.OP_ACCEPT);
    }

    /**
     * Implements the required open() method as a service handler object.  This is 
     * a do-nothing method, since this object will be dispatched directly 
     * by the Reactor.
     */
    public void open() {
        // no-op;
    }

    /**
     * Implements the required activate(key) method as a service handler object.  Again
     * this method is dispatched by the Reactor.
     * @param key The selection key reference from the Reactor.
     * @return true always.
     */
    public boolean activate(SelectionKey key) {
        // delegate to the run() method to handle the newly arrived accept 
        // event.
        this.run();
        return true;
    }

    /**
     * Implements the required deactivate() method as a service handler object.  
     * @return true when successful.
     */
    public boolean deactivate() {
        this._factory.shutdown();
        return true;
    }

    /**
     * Implements the required run() method as a Runnable object.
     */
    public void run() {
        try {
            // accepts the connection request.
            SelectableChannel peerHandle = this._handle.accept();
            if (peerHandle != null) {
                // now, create a service handler object to serve the
                // the client.
                ServiceHandler handler =
                    this._factory.createServiceHandler(
                        this._reactor,
                        peerHandle);
                if (handler != null)
                    // give the service handler object a chance to initialize itself.
                    handler.open();
            }
        } catch (IOException ex) {
            MDMS.ERROR(ex.getMessage());
        }
    }

    /**
     * Implements the required getHandle() method as a service handler object.
     * @return The internal communication channel (handle) reference.
     */
    public SelectableChannel getHandle() {
        return this._handle;
    }

    /**
     * Implements the required getKey() method as a service handler object.
     * @return The selection key reference that is generated during 
     * event registration.
     */
    public SelectionKey getKey() {
        return this._key;
    }

    /**
     * Implements the required setKey() method as a service handler object.
     * @param key The selection key reference.
     */
    public void setKey(SelectionKey key) {
        this._key = key;
    }
}
