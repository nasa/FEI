/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This is a wrapper class to the java socket class.  It addes additional
 * useful methods to simplify sending and receiving of data stream.
 *
 * @author T. Huang
 * @version $Id: SocketStream.java,v 1.5 2003/09/09 22:55:01 rap Exp $
 */
public class SocketStream {

    /**
     * The internal socket object.
     */
    private Socket _socket;

    /**
     * input stream.
     */
    private DataInputStream _inStream;

    /**
     * output stream.
     */
    private DataOutputStream _outStream;

    /**
     * Default constructor to initialize the socket stream object.
     */
    public SocketStream() {
        // no-op
    }

    /**
     * Constructor to initialize the socket stream object with a Socket object.
     * 
     * @param s The socket object.
     * @throws IOException when I/O failure
     */
    public SocketStream(Socket s) throws IOException {
        this.setSocket(s);
    }

    /**
     * Sets the socket object and initializes the internal I/O stream objects.
     * 
     * @param s The socket object.
     * @throws IOException when I/O failure
     */
    public void setSocket(Socket s) throws IOException {
        this._socket = s;

        this._inStream =
            new DataInputStream(
                new BufferedInputStream(this._socket.getInputStream()));

        this._outStream =
            new DataOutputStream(
                new BufferedOutputStream(this._socket.getOutputStream()));
    }

    /**
     * To return the internal socket object.
     * 
     * @return The internal socket object.
     */
    public Socket getSocket() {
        return this._socket;
    }

    /**
     * Closes the socket connection, if it exists.
     * 
     * @throws IOException when I/O failure
     */
    public void close() throws IOException {
        if (this._socket != null)
            this._socket.close();
        this._socket = null;
    }

    /**
     * Sends a String Buffer through the socket.
     * 
     * @param sb The input string buffer.
     * @return The number of bytes sent.
     * @throws IOException when I/O failure
     */
    public int send(StringBuffer sb) throws IOException {
        String buf = sb.toString();
        return this.send(buf);
    }

    /**
     * Sends a string through the socket.
     * 
     * @param s The input string to be sent.
     * @return The number of bytes sent.
     * @throws IOException when I/O failure
     */
    public int send(String s) throws IOException {
        this._outStream.writeChars(s.toString());
        this._outStream.writeChar('\n');
        this._outStream.flush();
        return s.length();
    }

    /**
     * Sends an int through the socket.
     * 
     * @param val the integer value
     * @return The value sent
     * @throws IOException when I/O failure
     */
    public int send(int val) throws IOException {
        this._outStream.writeInt(val);
        this._outStream.flush();
        return val;
    }

    /**
     * Sends an array of bytes through the socket.
     * 
     * @param b The input byte array.
     * @param offset The input offset.
     * @param length The length of the byte array.
     * @return The number of bytes sent.
     * @throws IOException when I/O failure
     */
    public int sendNBytes(byte[] b, int offset, int length)
        throws IOException {
        this._outStream.write(b, offset, length);
        this._outStream.flush();
        return length;
    }

    /**
     * Receives a string buffer from the socket.
     * 
     * @param sb The output string buffer.
     * @return The number of bytes received.
     * @throws IOException when I/O failure
     */
    public int recv(StringBuffer sb) throws IOException {
        int len = 0;
        char in = (char) this._inStream.readByte();

        while (in != '\n') {
            sb.append(in);
            in = (char) this._inStream.readByte();
            ++len;
        }
        return len;
    }

    /**
     * Reads an int from a socket
     * 
     * @return the int value
     * @throws IOException when I/O failure
     */
    public int recv() throws IOException {
        return this._inStream.readInt();
    }

    /**
     * Receives an array of bytes.
     * 
     * @param b The received byte array.
     * @param offset The returned offset value.
     * @param n The number of bytes received.
     * @return The number of bytes received.
     * @throws IOException when I/O failure
     */
    public int recvNBytes(byte[] b, int offset, int n) throws IOException {
        this._inStream.readFully(b, offset, n);
        return n;
    }

    /**
     * Sets the internal data output stream.
     * 
     * @param os The input output stream object.
     */
    public void setDataOutputStream(OutputStream os) {
        this._outStream = new DataOutputStream(new BufferedOutputStream(os));
    }

    /**
     * Access method to the internal output data stream object.
     * 
     * @return The output data stream object.
     */
    public DataOutputStream getDataOutputStream() {
        return this._outStream;
    }

    /**
     * Sets the internal input data stream object.
     * 
     * @param is The input stream object.
     */
    public void setDataInputStream(InputStream is) {
        this._inStream = new DataInputStream(new BufferedInputStream(is));
    }

    /**
     * Access method to the internal data input stream object.
     * 
     * @return The data input stream object.
     */
    public DataInputStream getDataInputStream() {
        return this._inStream;
    }

    /**
     * Cleanup method execute by the garbage collector.
     * 
     * @throws Throwable when finalizing
     */
    protected void finalize() throws Throwable {
        super.finalize();
        this.close();
    }
}
