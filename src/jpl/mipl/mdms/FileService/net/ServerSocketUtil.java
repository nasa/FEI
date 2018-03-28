/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.net;

import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A server socket utility class to enable selection between secure or
 * not secure ServerSocket configuration.
 *
 * @author G.Turek
 * @version $Id: ServerSocketUtil.java,v 1.5 2004/08/09 21:49:18 txh Exp $
 */
public class ServerSocketUtil {

   private ServerSocket _serverSocket;

   /**
    * Constructor for unsecure ServerSocket
    *
    * @param port the port number (between 0 and 65536)
    * @throws Exception when general failure
    */
   public ServerSocketUtil(int port) throws Exception {
      this(port, null, null, false, false);
   }

   /**
    * Constructor for secure ServerSocket
    *
    * @param port the port number (between 0 and 65536)
    * @param pwd password to keystore
    * @param keys absolute path to keys file
    * @param clientauth whether client authentification is required
    * @throws Exception when general failure
    */
   public ServerSocketUtil(
      int port,
      String pwd,
      String keys,
      boolean clientauth)
      throws Exception {
      this(port, pwd, keys, clientauth, true);
   }

   /**
    * Constructor for either secure or unsecure ServerSocket
    *
    * @param port the port number (between 0 and 65536)
    * @param pwd password to keystore
    * @param keys absolute path to keys file
    * @param clientauth whether client authentification is required
    * @param secure whether accept will return a secure socket
    * @throws Exception when general failure
    */
   public ServerSocketUtil(
      int port,
      String pwd,
      String keys,
      boolean clientauth,
      boolean secure)
      throws Exception {
      SecureSocketsUtil ssu = new SecureSocketsUtil(keys, pwd);
      if (secure) {
         System.err.println("Getting secure listener using keyfile " + keys);
         SSLServerSocket sock = ssu.getSecureServerSocket(port);
         //Only connections where client can authenticate itself are accepted
         sock.setNeedClientAuth(clientauth);
         this._serverSocket = sock;
      } else {
         this._serverSocket = new ServerSocket(port);
      }
   }

   /**
    * Used for access to SSL socket parameters, security provider
    * information, etc.  If the object was instantiated with unsecure
    * communication, then this method returns a null reference.
    *
    * @return a reference to the SSLServerSocket
    */
   public SSLServerSocket getSSLServerSocket() {
      if (this._serverSocket instanceof SSLServerSocket)
         return (SSLServerSocket) this._serverSocket;
      return null;
   }

   /**
    * Wrapper for socket accept method.  This method blocks until a client
    * connection is established and returns a handle to the communcation
    * channel.
    *
    * @return a secure or unsecure Socket
    * @throws IOException when communcation fail
    * @throws SecurityException when secure commucation fail
    */
   public Socket accept() throws IOException, SecurityException {
      return this._serverSocket.accept();
   }
}
