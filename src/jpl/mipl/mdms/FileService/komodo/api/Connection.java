/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

import jpl.mipl.mdms.FileService.io.BufferedStreamIO;
import jpl.mipl.mdms.FileService.net.SecureSocketsUtil;
import jpl.mipl.mdms.utils.logging.Logger;

import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Make connection to serverHost and serverPort specified
 * 
 * @author G. Turek, J. Jacobson
 * @version $Id: Connection.java,v 1.6 2005/06/14 01:30:24 ntt Exp $
 */
class Connection implements Runnable {
    
   private static int NO_TIMEOUT = 0;
   
   private String _serverHost;
   private int _serverPort;
   private int _startPort;
   private int _endPort;
   private int _bufferSize = 32256;
   private int _timeout = NO_TIMEOUT;

   private int _securityType = Constants.SSL;
   private SSLSocket _sslClient;
   private Socket _client;


   
   private static final SecureSocketsUtil _SECURESOCKETUTIL = new SecureSocketsUtil();
   private BufferedOutputStream _bos;
   private BufferedInputStream _bis;
   private BufferedStreamIO _io;

   private final Logger _logger = Logger.getLogger(Connection.class.getName());

   /**
    * Simple constructor
    */
   Connection() {
   }

   /**
    * Constructor <br>
    * Make a connection to a server, identified by serverHost and serverPort.
    * 
    * @param serverHost the server serverHost name
    * @param serverPort the server serverPort number
    * @param securityType the security type, usually Session.SSL.
    * @param startPort client side return port range, low end
    * @param endPort client side return port range, high end
    * @throws IOException when network I/O failure
    * @throws SecurityException when security check failed
    */
   Connection(String serverHost, int serverPort, int securityType,
              int startPort, int endPort) throws IOException, 
                                                 SecurityException {

       this(serverHost, serverPort, securityType, 
            startPort, endPort, NO_TIMEOUT);
   }

   /**
    * Constructor <br>
    * Make a connection to a server, identified by serverHost and serverPort.
    * 
    * @param serverHost the server serverHost name
    * @param serverPort the server serverPort number
    * @param securityType the security type, usually Session.SSL.
    * @param startPort client side return port range, low end
    * @param endPort client side return port range, high end
    * @param timeout Timeout, in milliseconds, that socket will wait while
    *        reading.  If timeout elapses, then a <code> 
    *        java.net.SocketTimeoutException</code> will be raised.
    * @throws IOException when network I/O failure
    * @throws SecurityException when security check failed
    */
   Connection(String serverHost, int serverPort, int securityType,
              int startPort, int endPort, int timeout) throws IOException, 
                                                          SecurityException {
       this._serverHost = serverHost;
       this._serverPort = serverPort;
       this._securityType = securityType;
       this._startPort = startPort;
       this._endPort = endPort;
       this._timeout = timeout;
       this._logger.trace("Now, make the connection.");
       this._makeConnection();
   }
   /**
    * Method to force loading of security classes, to avoid latency on 1st
    * connect.
    */
   public static void init() {
      Connection dummy = new Connection();
      // Create a service thread, and start it.
      Thread temp = new Thread(dummy);
      temp.start();
   }

   /**
    * The service method <br>
    * Load SSL classes in a separate thread.
    */
   public void run() {
      try {
         Connection._SECURESOCKETUTIL.getSecureClientSocket("NoSuchHost", 0);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Close network connection to server.
    * 
    * @throws IOException when network I/O failure
    */
   public void close() throws IOException {
      if (this._sslClient != null)
         this._sslClient.close();
      if (this._client != null)
         this._client.close();
   }

   /**
    * Establishes the connection, then passes the socket to handleConnection.
    * 
    * @throws IOException when network I/O failure
    * @throws SecurityException when failed to enable security
    */
   private void _makeConnection() throws IOException, SecurityException {
      try {
         switch (this._securityType) {
         case Constants.SSL:
            this._logger.trace("SSL");
            this._logger.trace("Now getting the connection.");
            if (this._startPort != 0) {
               for (int currPort = this._startPort; currPort <= this._endPort; currPort++) {
                  try {
                     this._sslClient = Connection._SECURESOCKETUTIL
                                           .getSecureClientSocket(
                                                   this._serverHost,
                                                   this._serverPort, 
                                                   (InetAddress) null, 
                                                   currPort, this._timeout);
                     break; // If connection works, break out.
                  } catch (SocketException se) {
                     /*
                      * If it's a socket exception, parse the error * message
                      * address alreay in use. Note: This is * dangerous, but
                      * there is not other way to * test for this.
                      */
                     if (se.getMessage().indexOf("Address already in use") > -1)
                        continue;
                     else
                        throw new IOException(se.getMessage());
                  } catch (IOException io) {
                     io.printStackTrace();
                     throw io;
                  }
               }
               if (this._sslClient == null)
                  throw new IOException("Ran out of client-side tcp ports");
            } else {
               this._sslClient = Connection._SECURESOCKETUTIL
                                           .getSecureClientSocket(
                                            this._serverHost, this._serverPort,
                                            this._timeout);
            }
            this._logger.trace("Now got the connection.");
            /*
             * * Bind this SSL socket into a Bufferered input and output
             * streams.
             */
            this._bos = new BufferedOutputStream(
                                           this._sslClient.getOutputStream(), 
                                           this._bufferSize);
            this._bis = new BufferedInputStream(
                                           this._sslClient.getInputStream(), 
                                           this._bufferSize);
            this._logger.trace("Got buffered stream.");
            break;
         case Constants.KERBEROS:
            this._logger.trace("Kerberos");
            throw new SecurityException("Kerberos not implemented.");
         case Constants.INSECURE:
            this._logger.trace("No security");
            // HINT: SocketException errno 125 address already in use
            Socket client = null;
            if (this._startPort != 0) {
               for (int currPort = _startPort; currPort <= _endPort; currPort++) {
                  try {
                     client = new Socket(this._serverHost, this._serverPort,
                                         (InetAddress) null, currPort);
                     client.setSoTimeout(this._timeout);
                     break; // If connection works, break out.
                  } catch (SocketException se) {
                     /*
                      * If it's a socket exception, parse the error message. If
                      * it contains 125, then continue. That meant that the
                      * address was already in use.
                      */
                     if (se.getMessage().indexOf("125") > -1)
                        continue;
                     else
                        throw new IOException(se.getMessage());
                  } catch (IOException io) {
                     io.printStackTrace();
                     throw io;
                  }
               }
               if (client == null)
                  throw new IOException("Ran out of client-side tcp ports");
            } else {
               client = new Socket(this._serverHost, this._serverPort);
               client.setSoTimeout(this._timeout);
            }
            /*
             * Bind this SSL socket into a Bufferered input and output streams.
             */
            this._bos = new BufferedOutputStream(client.getOutputStream(),
                                                 this._bufferSize);
            this._bis = new BufferedInputStream(client.getInputStream(),
                                                this._bufferSize);
            break;
         default:
            throw new SecurityException("Unknown Security type.");
         }
         /*
          * * Bind buffered input and output stream into a full-duplex * io
          * class.
          */
         this._io = new BufferedStreamIO(this._bis, this._bos, this._bufferSize);
      } catch (IOException io) {
         this._logger.trace("Connection.makeConnection (IOException)", io);
         if (this._client != null)
            this._client.close();
         throw io;
      } catch (SecurityException se) {
         this._logger.trace("Connection.makeConnection (SecurityException)",
                            se);
         if (this._client != null)
            this._client.close();
         throw se;
      }
   }

   /**
    * Method to return the internal I/O handle.
    * 
    * @return BufferedStreamIO
    */
   public BufferedStreamIO getIO() {
      return this._io;
   }
}