/*
 * Implementation of active acceptor handler object
 */

package jpl.mipl.mdms.test.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.StringTokenizer;

import jpl.mipl.mdms.connection.Reactor;
import jpl.mipl.mdms.connection.ServiceHandler;
import jpl.mipl.mdms.utils.MDMS;
import EDU.oswego.cs.dl.util.concurrent.Executor;

/**
 * This class implements the active acceptor handler object.  This handler 
 * is created by the acceptor factory per acceptor request.  It is created 
 * when a client service connection is established.  This handler will 
 * first reads in the client request on getting a file.  If the file 
 * exists, the handler sends a response with the size of the file and
 * followed by file transfer using the new Java I/O package.
 * <p>
 * @copyright Copyright 2001, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  25-09-2001.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestAcceptHandler.java,v 1.3 2003/09/10 19:33:36 rap Exp $
 */
public final class TestAcceptHandler implements ServiceHandler {

    // the pluggable concurrency control object.
    private final Executor _pool;

    // the communication handle that this service handler must bind to.
    private final SocketChannel _handle;

    // the selection key that is used by the Reactor of non-blocking I/O.
    private SelectionKey _key;

    // the reactor reference.
    private Reactor _reactor;

    // the input file stream used for opening the input file.
    private FileInputStream _fileStream = null;

    // the file channel used for file transfering.
    private FileChannel _fileChannel = null;

    // since this is non-blocking I/O, the file transfer method will 
    // be dispatched multiple time for large files.  Therefore, it is 
    // important to keep track of the total number of bytes sent so far.
    private long _bytesSent = 0;

    // name of the file to be sent.
    private String _filename = null;

    // the file size.
    private long _fileSize = 0;

    // the client request option.
    private char _option = '\0';

    // a cheap state machine's predefined state.
    private static final int READ = 0, SEND = 1, PROCESS = 2, SHIP = 3;

    // initial state is to first read client request.
    private int _state = READ;

    // the reusable file data buffer.  For now, let's assume it is 64k.
    private ByteBuffer _fileBuffer = ByteBuffer.allocate(64 * 1024);

    /**
     * Constructor for non-blocking I/O handling.
     * @param reactor The Reactor to be register to.
     * @param handle The communication handle.
     * @param pool The concurrency strategy.
     * @throws IOException when I/O failure
     */
    public TestAcceptHandler(
        Reactor reactor,
        SelectableChannel handle,
        Executor pool)
        throws IOException {
        this._reactor = reactor;
        this._handle = (SocketChannel) handle;

        // configure the communication to be non-blocking.
        this._handle.configureBlocking(false);
        this._pool = pool;
    }

    /**
     * Hook method invoked by the acceptor to allow the service handler to 
     * initialize itself and perhaps register more interested events 
     * to the Reactor.
     */
    public void open() {
        // this handler needs to first handle read event for 
        // incoming client request.
        this._reactor.register(this, SelectionKey.OP_READ);
    }

    /**
     * Hook method invoked by the Reactor when the registered event occurs 
     * on the given communication channel.
     * @param key The selection key used by the Reactor.
     * @return boolean true if activation was successfull
     */
    public boolean activate(SelectionKey key) {
        this._key = key;
        this.run();
        return true;
    }

    /**
     * Hook method invoked by the Reactor duing shutdown process.
     * @return true when successful.
     */
    public boolean deactivate() {
        return true;
    }

    /**
     * Acces method to retrieve the internal selection key.
     * @return The selection key, may be null.
     */
    public SelectionKey getKey() {
        return this._key;
    }

    /**
     * Method to set the selection key
     * @param key The input selection key.
     */
    public void setKey(SelectionKey key) {
        this._key = key;
    }

    /**
     * Access method to the internal communication handle.
     * @return The communication handle, may be null.
     */
    public SelectableChannel getHandle() {
        return this._handle;
    }

    /**
     * Hook method used by Runnable and the concurrency strategy to 
     * handle any event.  It depends on the object's current state.
     */
    public void run() {
        try {
            if (this._state == READ)
                this._read();
            else if (this._state == SEND)
                this._send();
            else if (this._state == SHIP)
                this._ship();
            else
                MDMS.ERROR("<Server>Invalid event received.</Server>");
        } catch (IOException ex) {
            ex.printStackTrace();
            MDMS.ERROR(
                "<Server>" + this._state + " " + ex.getMessage() + "</Server>");
        }
    }

    /**
     * Method to read and parse user request.  If the parsing was successful, 
     * then it puts this itself nto a service pool for non-blocking response 
     * and file transfer service.
     * @throws IOException when I/O failure
     */
    private void _read() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.clear();
        long bytesRead = this._handle.read(buffer);
        if (bytesRead > 0) {
            Charset charset = Charset.forName("US-ASCII");
            CharsetDecoder decoder = charset.newDecoder();

            buffer.flip();

            StringTokenizer tokenizer =
                new StringTokenizer(decoder.decode(buffer).toString(), "\0");

            this._option = tokenizer.nextToken().charAt(0);

            this._filename = tokenizer.nextToken();

            MDMS.DEBUG(
                "<Server>Request = "
                    + this._option
                    + "; "
                    + "Filename = "
                    + this._filename
                    + "</Server>");
        }
        this._state = PROCESS;
        try {
            // put our service object into a service pool.
            this._pool.execute(new Processer());
        } catch (InterruptedException ex) {
            MDMS.ERROR("<Server>" + ex.getMessage() + "</Server>");
        }
    }

    /**
     * Method to send the requested file to the client.  If the file exists, 
     * it will be transfered using non-blocking I/O.
     * @throws IOException when I/O failure
     */
    private void _send() throws IOException {

        StringBuffer sbuffer = new StringBuffer(1024);
        Charset charset = Charset.forName("US-ASCII");
        CharsetEncoder encoder = charset.newEncoder();

        try {
            this._fileStream = new FileInputStream(this._filename);
            this._fileChannel = this._fileStream.getChannel();
        } catch (FileNotFoundException e) {
            // if the file failed to open, then it does not exists.
            // create a response packet with error code and the 
            // error message as well.

            sbuffer.append("E" + "\0" + "File not found" + "\0");
            ByteBuffer buffer =
                encoder.encode(CharBuffer.wrap(sbuffer.toString()));
            this._handle.write(buffer);
            this._bytesSent = 0;

            // shutdown the communication.
            this._reactor.remove(this);
            return;
        }

        // okay, the file exists, now send a success response along with 
        // the size of the file.
        this._fileSize = new File(this._filename).length();

        sbuffer.append("F" + "\0" + this._fileSize + "\0");

        sbuffer.setLength(1024);

        ByteBuffer buffer = encoder.encode(CharBuffer.wrap(sbuffer.toString()));

        // sends the response.  Next state is to ship the file.
        this._handle.write(buffer);

        // setup and read the first buffer first.
        //this.fileBuffer__.clear();
        //this.fileChannel__.read(this.fileBuffer__);
        //this.fileBuffer__.flip();

        MDMS.DEBUG(
            "<Server>Transfering "
                + this._fileSize
                + " bytes file."
                + "</Server>");
        this._state = SHIP;
    }

    /**
     * Method to ship the file to client.  This method may be dispatched 
     * multiple time for large file transfer.  Therefore, it is important to 
     * keep track of the total bytes sents for each execution.
     * @throws IOException when I/O failure
     */
    private void _ship() throws IOException {

        this._bytesSent
            += this._fileChannel.transferTo(
                this._bytesSent,
                this._fileSize - this._bytesSent,
                this._handle);

        /**
              // if we still have something in the buffer, then send it first before 
              // reading another one.
              if (this.fileBuffer__.hasRemaining())
                 this.bytesSent__ += this.handle__.write(this.fileBuffer__);
              else {
                 // reads another buffer from the file channel.
                 this.fileBuffer__.clear();
        
                 this.fileChannel__.read(this.fileBuffer__);
                 this.fileBuffer__.flip();
              }
        **/

        // if the entire file has been sent, the it is time to close
        // down the communication.
        if (this._bytesSent >= this._fileSize) {
            this._fileChannel.close();
            this._fileStream.close();
            this._bytesSent = 0;

            this._fileBuffer.clear();

            // unregister from Reactor.
            this._reactor.remove(this);

            // set this so run() will not throw an exception.
            this._state = READ;
        }
    }

    /**
     * Method invoked by the service pool to registered the active object 
     * to any write event occurs on its internal communicaiton channel.
     */
    public synchronized void processAndHandOff() {
        if (this._option == 'g') {
            MDMS.DEBUG(
                "<Server>BaseClient requested for file: "
                    + this._filename
                    + "</Server>");
            this._state = SEND;
            this._reactor.register(this._handle, this, SelectionKey.OP_WRITE);
        }
    }

    /**
     * Inner class as the initial point for turning our service object into an 
     * active object.
     */
    public class Processer implements Runnable {
    	
       /**
        * run the thread
        */
        public void run() {
            processAndHandOff();
        }
    }
}
