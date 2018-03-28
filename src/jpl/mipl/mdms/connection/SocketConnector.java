/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.connection;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;

import jpl.mipl.mdms.utils.MDMS;

/**
 * Implementation of the Connector design pattern [POSA2] for initializing new
 * Service Handler objects.  This is a complete reimplemention of the original 
 * SocketConnector class due to New I/O support in JDK1.4 with multiplex 
 * non-blocking communication establishment.  This implements the
 * ServiceHandler interface that is required by the Reactor framework.
 * <p>
 * Service handlers that are registered to this Connector will not be 
 * dispatched directy by the Reactor on connection establishment.  Instead,
 * the Connector will be dispatched and it will look up and notify the 
 * correct service handler object to initialize it self by invoking the 
 * service handler's open() method.  After the initial connection notification, 
 * the service handler will be unregistered from the Connector's dispatch list.
 * Any future events that the service handler 'wishes' to be notified is 
 * between the service handler and it's internal Reactor reference.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: SocketConnector.java,v 1.4 2003/09/09 00:32:36 rap Exp $
 */
public class SocketConnector implements ServiceHandler {
    // the internal communication channel (handle).
    private SocketChannel _handle = null;

    // the associated selection key.
    private SelectionKey _key = null;

    // the internal Reactor reference.
    private Reactor _reactor = null;

    // the hash map managed by the connector to lookup selection key with 
    // the service handler object reference.
    private Hashtable _handlerMap = new Hashtable();

    /**
     * Default constructor.
     */
    public SocketConnector() {
        // no-op.
    }

    /**
     * Constructor to initialize this connector with an associated Reactor.
     * @param reactor The Reactor object reference.
     * @see jpl.mipl.mdms.connection.Reactor
     */
    public SocketConnector(Reactor reactor) {
        this._reactor = reactor;
    }

    /**
     * Constructor to initialize the connector with an associated Reactor, and 
     * the first service handler and socket address to start the connection
     * request.
     * 
     * @param reactor The Reactor object reference.
     * @param handler The service handler object reference.
     * @param addr The socket address object reference.
     * @throws IOException when I/O failure
     * @see jpl.mipl.mdms.connection.Reactor
     * @see jpl.mipl.mdms.connection.ServiceHandler
     * @see java.net.SocketAddress
     */
    public SocketConnector(
        Reactor reactor,
        ServiceHandler handler,
        SocketAddress addr)
        throws IOException {
        this._reactor = reactor;
        this.connect(handler, addr);
    }

    /**
     * Method to establish a connection request with the input serivce handler 
     * and a socket address.  If the handler's internal handle is configured 
     * to be non-blocking, then the service handler will be added to the 
     * Connector's internal hash map.  If the handler is configured to be 
     * a simple blocking object, then the handler's open() will be triggered 
     * when connection is established.
     * 
     * @param handler The service handler object.
     * @param addr The socket address object.
     * @throws IOException when I/O failure
     */
    public void connect(ServiceHandler handler, SocketAddress addr)
        throws IOException {
        SocketChannel handle = (SocketChannel) handler.getHandle();

        // if the handler is configured to be blocking, then the connect() 
        // call will return true when the connection is established.
        if (handle.connect(addr))
            handler.open();
        else {
            // for non-blocking service handler, the connecto will register 
            // itself to the handler's communication channel with 
            // a connect event.  And it addes the service handler to its 
            // internal hash map with it's own selection key.
            SelectionKey key =
                this._reactor.register(handle, this, SelectionKey.OP_CONNECT);
            if (key != null && key.isValid())
                this._handlerMap.put(key, handler);
        }
    }

    /**
     * Implements the required open() method as a service handler object.  
     * This is a do-nothing method, since this object will be dispatched directly 
     * by the Reactor.
     */
    public void open() {
        // no-op
    }

    /**
     * Implements the required activate(key) method as a service handler object.  
     * Again this method is dispatched by the Reactor.
     * 
     * @param key The selection key reference from the Reactor.
     * @return true always.
     */
    public boolean activate(SelectionKey key) {
        // search for the associated service object with the input key.
        ServiceHandler handler = (ServiceHandler) this._handlerMap.remove(key);
        if (handler != null) {
            // associate the service handler to the selection key, so the 
            // connector will not get dispatched by the Reactor on the same 
            // service handler's internal handle again.
            key.attach(handler);

            // sets the handler's internal key reference.
            handler.setKey(key);

            // notify the service handler to initialize itself.
            handler.open();
        } else
            MDMS.ERROR("ServiceHandler is null");
        return true;
    }

    /**
     * Implements the required deactivate() method as a service handler object.  
     * 
     * @return true when successful.
     */
    public boolean deactivate() {
        return true;
    }

    /**
     * Implements the required run() method as a Runnable object.
     */
    public void run() {
        // no-op;
    }

    /**
     * Implements the required getKey() method as a service handler object.
     * 
     * @return The selection key reference that is generated during 
     * event registration.
     */
    public SelectionKey getKey() {
        return this._key;
    }

    /**
     * Implements the required setKey() method as a service handler object.
     * 
     * @param key The selection key reference.
     */
    public void setKey(SelectionKey key) {
        this._key = key;
    }

    /**
     * Implements the required getHandle() method as a service handler object.
     * 
     * @return The internal communication channel (handle) reference.
     */
    public SelectableChannel getHandle() {
        return this._handle;
    }
}
