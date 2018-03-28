/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.concurrent;

import jpl.mipl.mdms.concurrent.Consumer;
import jpl.mipl.mdms.concurrent.MessageQueue;
import jpl.mipl.mdms.concurrent.Producer;
import jpl.mipl.mdms.concurrent.Queue;
import jpl.mipl.mdms.concurrent.Task;
import jpl.mipl.mdms.concurrent.TaskThread;
import jpl.mipl.mdms.utils.MDMS;
import junit.framework.TestCase;

/**
 * JUnit test case of Message Queue
 *
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: MessageQueueTest.java,v 1.5 2003/09/10 00:19:14 rap Exp $
 */

public class MessageQueueTest extends TestCase {
    private MessageQueue _queue = null;
    private long _timeout = 1000;

	/**
	 * Constructor
	 *
	 * @param name the test suite name
	 */
    public MessageQueueTest(String name) {
        super(name);
    }

	/**
	 * Override the TestCase setUp method to initialize test environment.
	 *
	 * @throws Exception when general failure
	 */
    public void setUp() throws Exception {
        this._queue = new MessageQueue();
    }

	/**
	 * Override parent tearDown method to cleanup after testing.
	 */
    public void tearDown() {
        this._queue = null;
    }

    /**
     * Test the message queue
     * 
     * @throws Exception when general failure
     */
    public void testMessageQueue() throws Exception {
        try {
            TaskThread[] producers = new TaskThread[10];

            TaskThread consumer =
                new TaskThread(new TestConsumer(this._queue, this._timeout));

            for (int i = 0; i < producers.length; ++i) {
                producers[i] = new TaskThread(new TestProducer(this._queue));
                producers[i].setName(new String("Producer " + i));
            }

            this._queue.send(
                new String("<HTML>\n<TITLE>\nTest Producer\n</TITLE>\n<BODY>\n<P>"));

            for (int i = 0; i < producers.length; ++i)
                producers[i].start();

            consumer.start();

            for (int i = 0; i < producers.length; ++i)
                producers[i].join();

            consumer.join();

            this._queue.send(new String("</P>\n</BODY>\n</HTML>"));

            //consumer.join();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }

    }
}

/**
 * Utility test consumer class
 */
class TestConsumer extends Task implements Consumer {
    /** The internal timeout value in milliseconds. */
    private long _timeout;

    /**
     * Creates a TestConsumer object.
     * @param channel The input message channel.
     * @param timeout The timeout value in milliseconds.
     */
    public TestConsumer(Queue channel, long timeout) {
        super(channel);
        this._timeout = timeout;
    }

    /**
     * Method to implement the recv() method, to receive data from the internal
     * message channel.
     * @return The data object.
     * @throws InterruptedException when thread is interrupted
     */
    public Object recv() throws InterruptedException {
        return this.getq();
    }

    /**
     * Method to implement the recv(long) method, to recieve data from the
     * internal message channel within the specified time value.
     * @param msec The input timeout value in milliseconds.
     * @return The data object (may be 'null').
     * @throws InterruptedException when thread is interrupted
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
                msg = (String) this.recv(this._timeout);
                if (msg == null)
                    return;
                //MDMS.DEBUG(msg);
            }
        } catch (Exception e) {
            MDMS.DEBUG("Exception: " + e);
        }
    }

}

/**
 * Utility test producer class
 */
class TestProducer extends Task implements Producer {

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
     * @throws InterruptedException when thread is interrupted
     */
    public void send(Object item) throws InterruptedException {
        this.putq(item);
    }

    /**
     * Method to send an object to the channel within the specified time.
     * @param item The data object.
     * @param msec The timeout value in milliseconds.
     * @return true if successful.
     * @throws InterruptedException when thread is interrupted
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
                    new String(
                        this.getSelf().getName()
                            + ": <"
                            + i
                            + "> Pattern Languages<BR>"));
            }
        } catch (Exception e) {
            MDMS.DEBUG(new String("Exception: " + e));
        }
    }
}
