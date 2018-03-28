/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Hides the complexity of creating various streams associated with a socket.
 * Extend to other type streams as needed
 *
 * @author G.Turek
 * @version $Id: SocketUtil.java,v 1.3 2003/09/09 00:32:34 rap Exp $
 */
public class SocketUtil {

    private Socket _socket;

    /**
     * Constructor to initalize with a communcation handle
     *
     * @param sock a Socket object reference
     */
    public SocketUtil(Socket sock) {
        this._socket = sock;
    }

    /**
     * Get a buffered DataInputStream associated with a socket
     *
     * @return DataInputStream object reference
     * @throws IOException when communcation failed
     */
    public DataInputStream getBufferedDataStream() throws IOException {
        return (
            new DataInputStream(
                new BufferedInputStream(this._socket.getInputStream())));
    }

    /**
     * Get an unbuffered DataInputStream object associated with the socket
     *
     * @return DataInputStream object reference
     * @throws IOException when I/O fail
     */
    public DataInputStream getUnbufferedDataStream() throws IOException {
        return (new DataInputStream(this._socket.getInputStream()));
    }

    /**
     * Get an PrintStream object associated with the socket
     *
     * @return PrintStream object reference
     * @throws IOException when I/O fail
     */
    public PrintStream getPrintStream() throws IOException {
        return (new PrintStream(this._socket.getOutputStream()));
    }

    /**
     * Get a BufferedReader associated with the socket
     *
     * @return BufferedReader object reference
     * @throws IOException when I/O fail
     */
    public BufferedReader getBufferedReader() throws IOException {
        return (
            new BufferedReader(
                new InputStreamReader(this._socket.getInputStream())));
    }

    /**
     * Get a BufferedOutputStream associated with the socket
     *
     * @param size buffer size in bytes
     * @return BufferedOutputStream object reference
     * @throws IOException when I/O fail
     */
    public BufferedOutputStream getBufferedOutputStream(int size)
        throws IOException {
        return (new BufferedOutputStream(this._socket.getOutputStream(), size));
    }

    /**
     * Get a BufferedInputStream associated with the socket
     *
     * @param size buffer size in bytes
     * @return BufferedInputStream object reference
     * @throws IOException when I/O fail
     */
    public BufferedInputStream getBufferedInputStream(int size)
        throws IOException {
        return (new BufferedInputStream(this._socket.getInputStream(), size));
    }

    /**
     * Closes the socket
     *
     * @throws IOException when socket close fail
     */
    public void close() throws IOException {
        this._socket.close();
    }
}
