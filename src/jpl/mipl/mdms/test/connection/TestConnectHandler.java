/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.connection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Date;
import java.util.StringTokenizer;

import jpl.mipl.mdms.connection.Reactor;
import jpl.mipl.mdms.connection.ServiceHandler;
import jpl.mipl.mdms.utils.MDMS;

/**
 * This class uesd by the connector for non-blocking file transfer.
 * It registers itself to the connector for connection establishment.  When 
 * the connection was successful, it is being dispatched by the connector and 
 * it will registered itself to the Reactor using the established communication 
 * handle.  Objects of this class would send request to the server for 
 * file transfer.  It receives the file data stream and writes them to an 
 * output file.  All I/O are being handled using the new Java NIO package.
 *
 * <p>
 * @copyright Copyright 2001, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  25-09-2001.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestConnectHandler.java,v 1.3 2003/09/10 19:33:37 rap Exp $
 */

public class TestConnectHandler implements ServiceHandler {
    // the communication handle.
    private SocketChannel _handle = null;

    // the selection key used by the Reactor.
    private SelectionKey _key = null;

    // reference to the reactor.
    private final Reactor _reactor;

    // reference to the output file stream for file I/O.
    private FileOutputStream _fileStream = null;

    // reference to file channel for file transfer.
    private FileChannel _fileChannel = null;

    // must keep track of the number of bytes received for large file 
    // transfer.
    private long _bytesReceived = 0;

    // the size of the file to be received.
    private long _filesize = 0;

    // the input file name that will be sent to the server.
    private final String _inputfile;

    // the output file name to be written to.
    private final String _outputfile;

    // state flags for our cheap state machine.
    //private static final int READ = 0, SEND = 1, PROCESS = 2, RECEIVE = 3;
    private static final int READ = 0, SEND = 1, RECEIVE = 3;

    // initial state is to send the request.
    private int _state = SEND;

    // use for QoS calculation.
    private long _startTime = 0;

    // the reusable file buffer.  For now, let's assume it is 64k.
    //private ByteBuffer _fileBuffer = ByteBuffer.allocate(64 * 1024);

    /**
     * Constructor to initialize the client communication handle
     * @param reactor The Reactor reference.
     * @param inputfile The input file name.
     * @param outputfile The output file name.
     * @throws IOException when I/O failure
     */
    public TestConnectHandler(
        Reactor reactor,
        String inputfile,
        String outputfile)
        throws IOException {
        this._inputfile = inputfile;
        this._outputfile = outputfile;
        this._reactor = reactor;
        this._handle = SocketChannel.open();

        // make sure we have non-blocking I/O.
        this._handle.configureBlocking(false);

        // enable KEEP_ALIVE.
        this._handle.socket().setKeepAlive(true);

        // try to reduce the time on TIME_WAIT.
        this._handle.socket().setReuseAddress(true);
    }

    /**
     * Hook method invoked by the connector to notify this object 
     * that the connection has established and it is time to initialize itself.
     */
    public void open() {
        this._reactor.register(this._handle, this, SelectionKey.OP_READ);
        this.run();
    }

    /**
     * Hook method invoked by the Reactor to notify this object on event 
     * its subscribed event occurence.
     * @param key The selection key used by the reactor.
     * @return true when activation was successful.
     */
    public boolean activate(SelectionKey key) {
        this._key = key;
        this.run();
        return true;
    }

    /**
     * Hook method invoked by the Reactor to shutdown cleanup.
     * @return true when deactivation was successful.
     */
    public boolean deactivate() {
        return true;
    }

    /**
     * Access method to obtain the internal key used by the Reactor.
     * @return The internal selection key.
     */
    public SelectionKey getKey() {
        return this._key;
    }

    /**
     * Method to set the internal selection key.
     * @param key The selection key.
     */
    public void setKey(SelectionKey key) {
        this._key = key;
    }

    /**
     * Access method to return reference to the internal communication handle.
     * @return The internal communication handle.
     */
    public SelectableChannel getHandle() {
        return this._handle;
    }

    /**
     * The actual service method for state machine management.
     */
    public void run() {
        try {
            // because this is non-blocking I/O, the connection may not be 
            // offically complete until we invoke the finishConnect method.
            if (this._handle.finishConnect()) {
                if (this._state == SEND)
                    this._send();
                else if (this._state == READ)
                    this._read();
                else if (this._state == RECEIVE)
                    this._receive();
                else
                    MDMS.ERROR(
                        "<BaseClient>BaseClient invalid event</BaseClient>");
            }
        } catch (IOException ex) {
            MDMS.ERROR("<BaseClient>" + ex.getMessage() + "</BaseClient>");
        }
    }

    /**
     * Method to read service response.
     * @throws IOException when I/O failure
     */
    private void _read() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        long bytesRead = this._handle.read(buffer);
        if (bytesRead > 0) {
            buffer.flip();

            Charset charset = Charset.forName("US-ASCII");
            CharsetDecoder decoder = charset.newDecoder();

            StringTokenizer tokenizer =
                new StringTokenizer(decoder.decode(buffer).toString(), "\0");

            char response = tokenizer.nextToken().charAt(0);

            // if an error occurs on the server side, then log the error.
            if (response == 'E' || response == 'e') {
                MDMS.ERROR(
                    "<BaseClient>ERROR: "
                        + tokenizer.nextToken()
                        + "</BaseClient>");
                return;
            }

            // everything looks good, now get the file size.
            this._filesize = Long.parseLong(tokenizer.nextToken());

            // create the output file channel.
            this._fileStream = new FileOutputStream(this._outputfile);
            this._fileChannel = this._fileStream.getChannel();
            this._bytesReceived = 0;
            this._state = RECEIVE;
        }
    }

    /**
     * Method to receive the requested file.  Since this is non-blocking I/O,
     * this method will be dispatched multiple times for large file transfer.
     * @throws IOException when I/O failure
     */
    private void _receive() throws IOException {
        /**
              this.fileBuffer__.clear();
        
              this.bytesReceived__ += this.handle__.read(this.fileBuffer__);
        
              this.fileBuffer__.flip();
              this.fileChannel__.write(this.fileBuffer__);
        **/

        this._bytesReceived
            += this._fileChannel.transferFrom(
                this._handle,
                this._bytesReceived,
                this._filesize - this._bytesReceived);

        // if we have the entire file,the calculate QoS and close.
        if (this._bytesReceived >= this._filesize) {
            MDMS.DEBUG(
                "<BaseClient>Total of "
                    + this._bytesReceived
                    + " bytes received.</BaseClient>");
            MDMS.DEBUG(
                "<BaseClient>Elapsed File Transfer Time: "
                    + (new Date().getTime() - this._startTime)
                    + " ms</BaseClient>");

            this._fileChannel.close();
            this._fileStream.close();
            this._bytesReceived = 0;
            this._state = SEND;
        }
    }

    /**
     * Method to send the initial request to server.
     * @throws IOException when I/O failure
     */
    private void _send() throws IOException {
        this._startTime = new Date().getTime();
        MDMS.DEBUG(
            "<BaseClient>Request to get file: "
                + this._inputfile
                + "</BaseClient>");
        this._startTime = new Date().getTime();
        Charset charset = Charset.forName("US-ASCII");
        CharsetEncoder encoder = charset.newEncoder();

        StringBuffer sbuffer = new StringBuffer(1024);
        sbuffer.append("g" + "\0" + this._inputfile + "\0");

        ByteBuffer buffer = encoder.encode(CharBuffer.wrap(sbuffer.toString()));
        this._handle.write(buffer);
        this._state = READ;
    }
}
