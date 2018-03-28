/**
 * Test the message queue
 */

package jpl.mipl.mdms.test.concurrent;

import jpl.mipl.mdms.utils.TestConfigurator;
import jpl.mipl.mdms.utils.*;
import jpl.mipl.mdms.concurrent.*;
import java.util.*;

/**
 * Test the message queue
 *
 * @copyright Copyright 2001, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  25-09-2001.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestMessageQueue.java,v 1.7 2002/05/20 18:41:11 txh Exp $
 */
public class TestMessageQueue implements Runnable {

   private MessageQueue queue_ = null;
   private long timeout_ = 1000;

   /** Creates new TestMessageQueue */
   public TestMessageQueue() {
      this.queue_ = new MessageQueue();
   }

   public TestMessageQueue(MessageQueue queue) {
      this.queue_ = queue;
   }

   public void run() {
      try {
         TaskThread[] producers = new TaskThread[10];

         TaskThread consumer =
            new TaskThread(new TestConsumer(this.queue_, this.timeout_));

         for (int i = 0; i < producers.length; ++i) {
            producers[i] = new TaskThread(new TestProducer(this.queue_));
            producers[i].setName(new String("Producer " + i));
         }

         this.queue_.send(
            new String("<HTML>\n<TITLE>\nTest Producer\n</TITLE>\n<BODY>\n<P>"));

         for (int i = 0; i < producers.length; ++i)
            producers[i].start();

         consumer.start();

         for (int i = 0; i < producers.length; ++i)
            producers[i].join();

         consumer.join();

         this.queue_.send(new String("</P>\n</BODY>\n</HTML>"));

         //consumer.join();
      } catch (Exception e) {
         MDMS.DEBUG("Exception: " + e);
      }

   }

   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {
      TestConfigurator.activate(args, "Message Queue");

      Thread test = new Thread(new TestMessageQueue());

      try {
         test.start();
         test.join();
      } catch (Exception e) {
         MDMS.DEBUG("Exception: " + e);
      }

      TestConfigurator.deactivate();
   }

}