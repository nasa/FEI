/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Wrapper class to define an endpoint of a connection to encapsulate the host
 * and the port.
 *
 * @author T. Huang
 * @version $Id: INETAddr.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class INETAddr {

    /**
     * The internal Inet Address object.
     */
    private InetAddress _address;

    /**
     * The internal port number.
     */
    private int _port = 0;

    /**
     * Default constructor to initialize the INETAddr object.
     */
    public INETAddr() {
        // no-op.
    }

    /**
     * Constructor to initialize an INETAddr with a port and a host name.
     * 
     * @param port The input port number.
     * @param hostname The input host name.
     * @throws UnknownHostException when host lookup failure
     */
    public INETAddr(int port, String hostname) throws UnknownHostException {
        super();
        this._port = port;
        this._address = InetAddress.getByName(hostname);
    }

    /**
     * Constructor to initialize the INETAddr with a string address.
     * 
     * @param address The address string.
     * @throws UnknownHostException when host lookup failure
     */
    public INETAddr(String address) throws UnknownHostException {
        super();
        // typical address format: hostname.foo.com:port
        int delimiter = address.indexOf(':');
        if (delimiter != 0) {
            this._address =
                InetAddress.getByName(address.substring(0, delimiter));
            address = address.substring(delimiter + 1);
        }
        this._port = Integer.parseInt(address);
    }

    /**
     * Access method to the host name.
     * 
     * @return The host name string.
     */
    public String getHostName() {
        return this._address.getHostName();
    }

    /**
     * Access method to the host address.
     * @return The host address string.
     */
    public String getHostAddress() {
        return this._address.toString();
    }

    /**
     * Access method to the port number.
     * @return The port number.
     */
    public int getPortNumber() {
        return this._port;
    }

    /**
     * Override the part toString method to return formated address string.
     * @return The formated address string.
     */
    public String toString() {
        return this.getHostAddress() + Integer.toString(this._port);
    }

}
