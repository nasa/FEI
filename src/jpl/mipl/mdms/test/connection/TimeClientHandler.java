/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import jpl.mipl.mdms.connection.ServiceHandler;
import jpl.mipl.mdms.connection.SocketConnector;
import jpl.mipl.mdms.utils.MDMS;

/**
 * TimeClientHander.java
 * 
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TimeClientHandler.java,v 1.3 2003/09/10 19:33:37 rap Exp $
 */
public class TimeClientHandler implements ServiceHandler {
    private SocketChannel _handle = null;
    private ByteBuffer _input = ByteBuffer.allocate(1024);

    /**
     * Constructor
     * @throws IOException when I/O failure
     */
    public TimeClientHandler() throws IOException {
        this._handle = SocketChannel.open();
        this._handle.socket().setKeepAlive(true);
        this._handle.socket().setReuseAddress(true);
    }
    /**
     * @see ServiceHandler#open()
     */
    public void open() {
        try {
            this._handle.read(this._input);
            this._input.flip();

            Charset charset = Charset.forName("US-ASCII");
            CharsetDecoder decoder = charset.newDecoder();
            MDMS.DEBUG(decoder.decode(this._input).toString());
            this._handle.socket().close();
            this._handle.close();
        } catch (Exception e) {
            MDMS.ERROR(e.getMessage());
        }
    }

    /**
     * @see ServiceHandler#activate(SelectionKey)
     */
    public boolean activate(SelectionKey key) {
        return false;
    }

    /**
     * @see ServiceHandler#deactivate()
     */
    public boolean deactivate() {
        return false;
    }

    /**
     * @see ServiceHandler#getKey()
     */
    public SelectionKey getKey() {
        return null;
    }

    /**
     * @see ServiceHandler#setKey(SelectionKey)
     */
    public void setKey(SelectionKey key) {
    }

    /**
     * @see ServiceHandler#getHandle()
     */
    public SelectableChannel getHandle() {
        return this._handle;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {
    }

    /**
     * Main method
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            MDMS.ERROR("Usage: TestClient <hostname>");
            return;
        }
        try {
            InetSocketAddress addr =
                new InetSocketAddress(InetAddress.getByName(args[0]), 25);
            TimeClientHandler handler = new TimeClientHandler();
            SocketConnector connector = new SocketConnector();
            connector.connect(handler, addr);
        } catch (IOException ex) {
            MDMS.ERROR(ex.getMessage());
        }
    }
}
