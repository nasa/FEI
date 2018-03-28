/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;

/**
 * This class hides all the complexity of creating a secure socket (be it
 * server or client)
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 2018-02-01        William             originally client secure sockets only have same cipher suites as server sockets.
 *                                          The inconvenience arose when cipher suites are updated.
 *                                          All clients need to update their config file to match server suites.
 *                                          Disabling this will ensure that created socket has all available suites from Java.
 */
public class SecureSocketsUtil {
   // using SSL version 3.
   //private String _algorithm = "SSLv3";
   private String _algorithm = "TLSv1.2";
   private SSLContext _context;
   private final Object _contextLock = new Object();
   private TrustManager[] _tms;

   //private SSLSocketFactory _clientFactory;


   private static final String CIPHER_KEY = "komodo.net.ciphers";
   private static final String PROTOCOL_KEY = "komodo.net.protocol";

   // Enable only 128 bit cypher suites. For version 6, put this
   // into DBMS server parameters.
   private static String[] _STRONGSUITES =
         {
//            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
//            "SSL_RSA_WITH_RC4_128_MD5",
//            "SSL_RSA_WITH_RC4_128_SHA",
//            "SSL_RSA_WITH_3DES_EDE_CBC_SHA"
             "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
             "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
             "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
             "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
             "TLS_RSA_WITH_AES_128_CBC_SHA"
         };




   /**
    * Constructor to initialize security providers.
    */
   public SecureSocketsUtil() {
      //Dynamic loading of security providers. Should be in
      //$JAVA_HOME/jre/lib/security/java.security, but never know
      // Commenting these out as they become problematic if using
      // a non Sun/Oracle JVM (e.g. IBM jvm).  Will rely on
      // the java.security file to define which providers are loaded
      //  - awt 11/19/14
      //Security.addProvider(new sun.security.provider.Sun());
      //Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

      // Have to manually load the trustStore in case it is bundled
      // in a jar file (as in the case for webstart).
      try {
        //get the domain file
          ConfigFileURLResolver resolver = new ConfigFileURLResolver();
          URL trustStoreURL = resolver.getKeyStoreFile();
          //String trustStore = System.getProperty("javax.net.ssl.trustStore");
          if (trustStoreURL != null) {
	          TrustManagerFactory tmFactory = TrustManagerFactory
	                         .getInstance(TrustManagerFactory.getDefaultAlgorithm());
	          KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
	          InputStream ksStream = trustStoreURL.openStream();

	          ks.load(ksStream,null);
	          tmFactory.init(ks);
	          this._tms = tmFactory.getTrustManagers();
	          this._context = SSLContext.getInstance(this._algorithm);
	          this._context.init(null, this._tms, this._getSecureRandom());
          } else {
        	  this._context = SSLContext.getInstance(this._algorithm);
	          this._context.init(null, null, this._getSecureRandom());
          }


          // Get cipher suites override from properties
          // If specified, expecting a csv list of ciphers
          String cipherOverride = System.getProperty(SecureSocketsUtil.CIPHER_KEY);
          String[] cipherList = null;
          if (cipherOverride != null) {
              cipherList = cipherOverride.split(",");
          }

          if (cipherList != null && cipherList.length > 0) {
              SecureSocketsUtil._STRONGSUITES = cipherList;
          }

          // Get protocol override from properties
          String protocolOverride = System.getProperty(SecureSocketsUtil.PROTOCOL_KEY);
          if (protocolOverride != null) {
              this._algorithm = protocolOverride;
          }
      } catch (Exception e) {
          e.printStackTrace();
      }


   }

   /**
    * Constructor to initialize security providers and initialize SSL context.
    *
    * @param passphrase password to keystore
    * @param keys absolute path to keys file
    * @throws IOException       when keystore access fail
    * @throws SecurityException when context initialization fail
    */
   public SecureSocketsUtil(String keys, String passphrase)
         throws IOException, SecurityException {
      this();
      this._context = getSSLContext(passphrase, keys);
   }

   /**
    * Constructor to initialize providers, context, and use the input
    * algorithm.
    *
    * @param key absolute path to keys file
    * @param passphrase password to the keystore
    * @param algorithm the algorithm to be used (defaults to SSLv3)
    * @throws IOException       when keystore access fail
    * @throws SecurityException when context initialization fail
    */
   public SecureSocketsUtil(String key, String passphrase, String algorithm)
         throws IOException, SecurityException {
      this(key, passphrase);
      this._algorithm = algorithm;
   }

   /**
    * Gets a secure server socket and return it to the calling program. The
    * method throws a generic exception which contains the message of the
    * actual exception (one of 6 possible)
    *
    * @param port the port number (between 0 and 65536)
    * @return a secure server socket object
    * @throws IOException when network problem occurs
    */
   public SSLServerSocket getSecureServerSocket(int port) throws IOException {
      SSLServerSocket newSocket;
      SSLServerSocketFactory factory = this._context.getServerSocketFactory();
      newSocket = (SSLServerSocket) factory.createServerSocket(port);
      newSocket.setEnabledCipherSuites(SecureSocketsUtil._STRONGSUITES);
      return newSocket;
   }

   /**
    * Gets a secure client socket on the specified host and port number.
    *
    * @param host the remote host name
    * @param port the remote port number (between 0 and 65536)
    * @return a secure client socket object
    * @throws IOException when network problem occurs
    */
   public synchronized SSLSocket getSecureClientSocket(String host, int port)
         throws IOException {
       return this.getSecureClientSocket(host, port, 0);
   }


   /**
    * Gets a secure client socket on the specified host and port number.
    *
    * @param host the remote host name
    * @param port the remote port number (between 0 and 65536)
    * @return a secure client socket object
    * @throws IOException when network problem occurs
    */
   public synchronized SSLSocket getSecureClientSocket(String host, int port,
                                             int timeout) throws IOException {

       //put the getInstance() and init in block to avoid sync issues
      try {
          synchronized (_contextLock) {
              this._context = SSLContext.getInstance(this._algorithm);
              this._context.init(null, this._tms, this._getSecureRandom());
          }
      } catch (NoSuchAlgorithmException e) {
         throw new IOException(e.getMessage());
      } catch (KeyManagementException e) {
         throw new IOException(e.getMessage());
      }

      SSLSocketFactory factory =
            (SSLSocketFactory) this._context.getSocketFactory(); //SSLSocketFactory.getDefault();

      // If port = 0, return null. This allows the dynamic loading of
      // socket classes before attempting a real connection.
      if (port == 0)
         return null;

      SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

      // Just enable the strong cypher suites.
      /* NOTE: updated on 2018/02/01
         originally client secure sockets only have same cipher suites as server sockets.
         The inconvenience arose when cipher suites are updated.
         All clients need to update their config file to match server suites.
         Disabling this will ensure that created socket has all available suites from Java.
       */
      //socket.setEnabledCipherSuites(SecureSocketsUtil._STRONGSUITES);

      /*
       * If timeout non-zero, set SoTimeout value.  When set, a read() call
       * on the InputStream associated with socket will block for this time.
       * If timeout expires, java.net.SocketTimeoutException is thrown.
       */
      if (timeout > 0)
          socket.setSoTimeout(timeout);

      return socket;
   }

   /**
    * Gets a secure client socket on the specified host and port from the
    * specified local INET address and port.
    *
    * @param host the remote host name
    * @param port the remote port number (between 0 and 65536)
    * @param localAddr the InetAdress of local network interface
    * @param localPort the local port for full-duplex connection.
    * @param timeout If non-zero, enables the SO_TIMEOUT with the timeout
    *                value.
    * @return the secure client socket object
    * @throws IOException when network problem occurs
    */
   public SSLSocket getSecureClientSocket(String host,
                                          int port,
                                          InetAddress localAddr,
                                          int localPort,
                                          int timeout)
         throws IOException {
      SSLSocketFactory factory = this._context.getSocketFactory();
            //(SSLSocketFactory) SSLSocketFactory.getDefault();
      SSLSocket socket =
            (SSLSocket) factory.createSocket(host, port, localAddr, localPort);

      /*
         * * enable all the suites. * String[] supported =
         * socket.getSupportedCipherSuites(); *
         * socket.setEnabledCipherSuites(supported); * Just enable the strong
         * cypher suites.
         */
      /* NOTE: updated on 2018/02/01
         originally client secure sockets only have same cipher suites as server sockets.
         The inconvenience arose when cipher suites are updated.
         All clients need to update their config file to match server suites.
         Disabling this will ensure that created socket has all available suites from Java.
       */
      //socket.setEnabledCipherSuites(SecureSocketsUtil._STRONGSUITES);

      /*
       * If timeout non-zero, set SoTimeout value.  When set, a read() call
       * on the InputStream associated with socket will block for this time.
       * If timeout expires, java.net.SocketTimeoutException is thrown.
       */
      if (timeout > 0)
          socket.setSoTimeout(timeout);

      return socket;
   }

   /**
    * Gets a secure client socket on the specified host and port from the
    * specified local INET address and port.
    *
    * @param host the remote host name
    * @param port the remote port number (between 0 and 65536)
    * @param localAddr the InetAdress of local network interface
    * @param localPort the local port for full-duplex connection.
    * @return the secure client socket object
    * @throws IOException when network problem occurs
    */
   public SSLSocket getSecureClientSocket(String host,
                                          int port,
                                          InetAddress localAddr,
                                          int localPort)
         throws IOException {
      return this.getSecureClientSocket(host, port, localAddr, localPort, 0);
   }

   /**
    * Gets a secure client socket with authorization. The client machine is
    * assumed to be authorized to connect to the remote server host, therefore
    * store clpher suites is not enabled for this socket.
    *
    * @param host the remote host name
    * @param port the remote port number (between 0 and 65536)
    * @return the secure client socket object
    * @throws IOException when I/O failure
    */
   public SSLSocket getSecureClientSocketWithAuth(String host, int port)
         throws IOException {
      SSLSocketFactory factory = this._context.getSocketFactory();
      return (SSLSocket) factory.createSocket(host, port);
   }

   /**
    * Sets up SSL context for server sockets and client sockets with
    * authorization
    *
    * @param passphrase password to keystore
    * @param keys absolute path to keys file
    * @return the SSL context object for the server socket.
    * @throws IOException       when keystore access fail
    * @throws SecurityException when context initialization fail
    */
   private SSLContext getSSLContext(String passphrase, String keys)
         throws SecurityException, IOException {
      SSLContext context;
      try {

         context = SSLContext.getInstance(this._algorithm);
         context.init(null, this._tms, this._getSecureRandom());

         //The reference implementation only supports X.509 keys
         KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

         //Sun's default kind of key store
         KeyStore ks = KeyStore.getInstance("JKS");

         //Use the keys and certs which came with the JSSE1.0.2 examples
         char[] passwd = passphrase.toCharArray();
         ks.load(new FileInputStream(keys), passwd);
         kmf.init(ks, passwd);

         context.init(kmf.getKeyManagers(), null, null);
      } catch (KeyManagementException e) {
         throw new SecurityException(e.getMessage());
      } catch (KeyStoreException e) {
         throw new SecurityException(e.getMessage());
      } catch (NoSuchAlgorithmException e) {
         throw new SecurityException(e.getMessage());
      } catch (UnrecoverableKeyException e) {
         throw new SecurityException(e.getMessage());
      } catch (CertificateException e) {
         throw new SecurityException(e.getMessage());
      }
      return context;
   }

   /**
    * Methdo to create our own SecrueRandom seed object.  By default, the JVM looks
    * for local random generator method which will impact the performance.  For example,
    * on Sun and Linux, it requires to read from device /dev/random which could be blocked
    * if not enough entropy have been collected
    *
    * @return a SecureRandom object
    */
   private SecureRandom _getSecureRandom() {
      long baseSeed = (new java.util.Date()).getTime();
      SecureRandom srBase = new SecureRandom();
      srBase.setSeed(baseSeed);

      byte[] seed = new byte[30];
      srBase.nextBytes(seed);

      SecureRandom sr = new SecureRandom();
      sr.setSeed(seed);

      return sr;
   }
}
