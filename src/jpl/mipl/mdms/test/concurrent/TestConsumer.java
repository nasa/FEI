/**
 * Tests implementation of consumer interface and task runnable class
 */

package jpl.mipl.mdms.test.concurrent;

import jpl.mipl.mdms.concurrent.*;
import jpl.mipl.mdms.utils.*;

/**
 * This class tests implementation of Consumer interface and the Task Runnable
 * class.
 *
 * <p>
 * @copyright Copyright 2001, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  25-09-2001.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestConsumer.java,v 1.8 2002/05/20 18:41:11 txh Exp $
 */
public class TestConsumer extends Task implements Consumer {
   /** The internal timeout value in milliseconds. */
   private long timeout__;

   /**
    * Creates a TestConsumer object.
    * @param channel The input message channel.
    * @param timeout The timeout value in milliseconds.
    */
   public TestConsumer(Queue channel, long timeout) {
      super(channel);
      this.timeout__ = timeout;
   }

   /**
    * Method to implement the recv() method, to receive data from the internal
    * message channel.
    * @return The data object.
    * @throws InterruptedException.
    */
   public Object recv() throws InterruptedException {
      return this.getq();
   }

   /**
    * Method to implement the recv(long) method, to recieve data from the
    * internal message channel within the specified time value.
    * @param msec The input timeout value in milliseconds.
    * @return The data object (may be 'null').
    * @throws InterruptedException.
    */
   public Object recv(long msec) throws InterruptedException {
      return this.getq(msec);
   }

   /**
    * Method to start the Active Object required by the Task Runnable class.
    */
   public void run() {
      try {
         String msg = null;
         while (true) {
            msg = (String) this.recv(this.timeout__);
            if (msg == null)
               return;
            MDMS.DEBUG(msg);
         }
      } catch (Exception e) {
         MDMS.DEBUG("Exception: " + e);
      }
   }

}