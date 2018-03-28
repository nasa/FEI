/*
 * Main test class for testing acceptor-connector framework using service pool.
 */

package jpl.mipl.mdms.test.connection;

import jpl.mipl.mdms.connection.Reactor;
import jpl.mipl.mdms.connection.SocketAcceptor;
import jpl.mipl.mdms.connection.SocketConnector;
import jpl.mipl.mdms.utils.MDMS;
import jpl.mipl.mdms.utils.GetOpt;
import jpl.mipl.mdms.utils.TestConfigurator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * This is the main test driver for the acceptor-connector famework.  It looks 
 * very much like a simple copy program, but with a client/server layer.  This
 * program creates both a client and a server object on the same host.  The 
 * client sends a request for a file to the server object.  The service object 
 * accepts the request and creates a service handler that will be serve 
 * in a service pool to transfer the file from the server to the client.
 * The client takes the incoming data stream and write it to a new file 
 * location.  All event dispatching and communication management are handled 
 * by the acceptor-connector and reactor framework.
 * <p>
 * @copyright Copyright 2002, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  22-05-2002.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestAcceptorConnector.java,v 1.6 2002/06/01 00:12:40 txh Exp $
 */
public class TestAcceptorConnector {

   // the hostname (localhost or the actual host name).
   private String hostname__;

   // the TCP port to be used.
   private int port__;

   // the input file name, which will be shipped to the server.
   private String input__;

   // the output file name, which the client uses for writing.
   private String output__;

   /**
    * Constructor to parse user commands.
    * @param args the input command line arguments.
    */
   public TestAcceptorConnector(String[] args) {
      // if the parsing failed, the the program terminates.
      if (!this.parseArgs__(args))
         System.exit(0);
   }

   /**
    * Method to start the test.
    */
   public void run() {
      try {
         // creates a Reactor with an input timeout.  That is, the Reactor 
         // will terminate will no event occurs of the input time unit.
         Reactor reactor = new Reactor(MDMS.DEFAULT_TIMEOUT*10);
         MDMS.DEBUG("<Main>Service Timeout: " + MDMS.DEFAULT_TIMEOUT*10 + "ms.</Main>");

         // creates our acceptor object (a.k.a. the server).
         SocketAcceptor acceptor =
            new SocketAcceptor(reactor, TestAcceptFactory.instance(), this.port__);

         // now create the client.
         InetSocketAddress addr =
            new InetSocketAddress(InetAddress.getByName(this.hostname__), this.port__);
         TestConnectHandler handler =
            new TestConnectHandler(reactor, this.input__, this.output__);

         SocketConnector connector = new SocketConnector(reactor);

         // register our client to the connector.
         connector.connect(handler, addr);
         
         // activate the infinite loop until no event occurs for the 
         // specified timeout value.
         reactor.run();
      } catch (IOException ex) {
         MDMS.ERROR("<Main>" + ex.getMessage() + "</Main>");
      }
   }

   /**
    * Internal method to parse user command line argument using GetOpt 
    * utility class.
    * @param args The command line argument list.
    * @return true if parsing was successful.
    */
   private boolean parseArgs__(String[] args) {

      if (args.length == 0 || args.length > 9) {
         this.usage__();
         return false;
      }

      GetOpt getOpt = new GetOpt(args, "h:p:i:t:H");

      String str;
      while ((str = getOpt.nextArg()) != null) {
         switch (str.charAt(0)) {
            case 'H' :
               this.usage__();
               return false;
            case 'h' :
               this.hostname__ = getOpt.getArgValue();
               break;
            case 'p' :
               this.port__ = Integer.parseInt(getOpt.getArgValue());
               break;
            case 'i' :
               this.input__ = getOpt.getArgValue();
               break;
            case 't' :
               this.output__ = getOpt.getArgValue();
               break;
         }
      }

      return true;
   }

   /**
    * Method to display program useage.
    */
   private void usage__() {
      MDMS.DEBUG("Usage: " + this.getClass().getName()
         + " -h hostname -p port -i <input file> -t <target file> -H");
   }

   /**
    * Main method to activate the test.
    */
   public static void main(String[] args) {
      TestConfigurator.activate(args, "TestAcceptorConnector");
      TestAcceptorConnector test = new TestAcceptorConnector(args);
      test.run();
      TestConfigurator.deactivate();
   }
}
