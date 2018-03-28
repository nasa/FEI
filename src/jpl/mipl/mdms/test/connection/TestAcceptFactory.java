package jpl.mipl.mdms.test.connection;

import java.nio.channels.SelectableChannel;

import jpl.mipl.mdms.connection.Reactor;
import jpl.mipl.mdms.connection.ServiceHandler;
import jpl.mipl.mdms.connection.ServiceHandlerFactory;
import jpl.mipl.mdms.utils.MDMS;

import EDU.oswego.cs.dl.util.concurrent.DirectExecutor;

import java.io.IOException;

/**
 * Implementation of the concrete factory used by the acceptor 
 * design pattern to create service handlers.  It instantiates a 
 * service executor that is shared by all service handlers created 
 * by the acceptor.  This enables the server to have pluggable 
 * concurrency control.
 * <p>
 * @copyright Copyright 2002, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  15-04-2002.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestAcceptFactory.java,v 1.1.1.1 2003/01/25 02:18:52 txh Exp $
 */
public class TestAcceptFactory implements ServiceHandlerFactory {

   // a simple service executor that is shared by all service handlers.
   private DirectExecutor _pool = null;

   // this is an implementation of a concrete factory that is part 
   // the abstract factory design pattern, which implies that 
   // there shall be only one factory per system.
   private static TestAcceptFactory _instance = new TestAcceptFactory();

   /**
    * Access method to the single instance of this factory class.
    * @return Reference to the single factory object.
    */
   public static TestAcceptFactory instance() {
      return TestAcceptFactory._instance;
   }

   /**
    * Implements the hook method that is used by the acceptor to create 
    * new service handler to handle new client connection request.
    * @param reactor The Reactor object reference.
    * @param handle The service handle that the handler must bind to.
    * @return A service handler object reference, may be null.
    * @see ServiceHandlerFactory#createServiceHandler(Reactor, SelectableChannel)
    */
   public ServiceHandler createServiceHandler(
      Reactor reactor,
      SelectableChannel handle) {
      try {
         return new TestAcceptHandler(reactor, handle, this._pool);
      } catch (IOException ex) {
         MDMS.ERROR(ex.getMessage());
         return null;
      }
   }

   /**
    * A do-nothing run method to make the Runnable interface happy.
    * @see Runnable#run()
    */
   public void run() {
      // no-op.
   }

   /**
    * Method to shutdown the factory to cleanup any resource allocated 
    * by this factory.  This includes shutting down the service pool object.
    */
   public void shutdown() {
      // no-op
   }

   /**
    * Hidden constructor for singleton design pattern.
    */
   private TestAcceptFactory() {
      this._pool = new DirectExecutor();
   }
}
