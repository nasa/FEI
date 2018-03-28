/**
 * Tests implementation of producer interfce and task runnable class
 */

package jpl.mipl.mdms.test.concurrent;

import jpl.mipl.mdms.utils.*;
import jpl.mipl.mdms.concurrent.*;

/**
 * This class tests implementation of Producer interface and the Task Runnable
 * class.
 *
 * <p>
 * @copyright Copyright 2001, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  25-09-2001.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestProducer.java,v 1.8 2002/05/20 18:41:11 txh Exp $
 */
public class TestProducer extends Task implements Producer {

   /**
    * Creates TestProducer with message channel.
    * @param channel The input message Channel.
    */
   public TestProducer(Queue channel) {
      super(channel);
   }

   /**
    * Method to send an object to the channel.
    * @param item The data object.
    * @throws InterruptedException.
    */
   public void send(Object item) throws InterruptedException {
      this.putq(item);
   }

   /**
    * Method to send an object to the channel within the specified time.
    * @param item The data object.
    * @param msec The timeout value in milliseconds.
    * @return true if successful.
    * @throws InterruptedException.
    */
   public boolean send(Object item, long msec) throws InterruptedException {
      return this.putq(item, msec);
   }

   /**
    * Method to turn this producer into an active object.
    */
   public void run() {
      try {
         for (int i = 0; i < 10; ++i) {
            this.send(
               new String(this.thr_self().getName() + ": <" + i + "> Pattern Languages<BR>"));
         }
      } catch (Exception e) {
         MDMS.DEBUG(new String("Exception: " + e));
      }
   }

}