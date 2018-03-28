/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * This abstract class implements the base class for the Active Object
 * design pattern (POSA2).  Objects of this type contain an message queue as
 * communication channel between them when running in multi-threaded
 * environment.
 *
 * @author Thomas Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: Task.java,v 1.6 2003/09/09 22:55:01 rap Exp $
 */
public abstract class Task implements Runnable {
    /** 
     * The internal message channel 
     */
    private Queue _queue = null;

    /**
     * The parent thread reference.  Since Thread only references to Runnable's
     * run method, a reference to parent thread will help delegate some of the
     * userful thread functions from this object.
     */
    private Thread _self = Thread.currentThread();

    /**
     * Creates new Task object with its own message queue.
     * 
     * @see MessageQueue
     */
    public Task() {
        this(new MessageQueue());
    }

    /**
     * Create a new Task object with an input message queue.  This is the common
     * usage of objects of this type by have all instances of this class to
     * subscribe to the same channel.
     * 
     * @param queue The input message channel.
     */
    public Task(Queue queue) {
        this._queue = queue;
    }

    /**
     * Method to implement the start() method in Thread.  If this Runnable class
     * is running in a single-threaded environment, then instances of this class
     * will not be attached to any threads.  Therefore, it needs a way to start
     * the execution.
     */
    public void start() {
        this.run();
    }

    /**
     * Abstract method that childen of this class must implement their own
     * application logics.
     */
    public abstract void run();

    /**
     * Method to implement the join() method in Thread.  If this Runnable class
     * is running in a single-threaded environment, then instances of this class
     * will not be attached to any threads.  Therefore, it needs a way to join
     * the execution.
     */
    public void join() {
        // no-op.
    }

    /**
     * Method to allow the owner thread to register with this Runnable object.
     * 
     * @param thread The owner thread.
     */
    public void register(Thread thread) {
        this._self = thread;
    }

    /**
     * Method to obtain reference to the internal message channel object.
     * 
     * @return The internal channel reference, usually Message Queue.
     */
    public Queue getMsgQueue() {
        return this._queue;
    }

    /**
     * Method to insert an item to the message channel.
     * 
     * @param item The data object.
     * @throws InterruptedException when thread is interrupted
     */
    public void putq(Object item) throws InterruptedException {
        this._queue.send(item);
    }

    /**
     * Method to insert an object onto the message channel within the
     * specified timeout value.  This method returns 'true' if the operation
     * was successful.
     * 
     * @param item The data object.
     * @param msec The timeout value in milliseconds.
     * @return true if successful
     * @throws InterruptedException when thread is interrupted
     */
    public boolean putq(Object item, long msec) throws InterruptedException {
        return this._queue.send(item, msec);
    }

    /**
     * Method to obtain an object from the message channel.  This is a blocking
     * call until an object is available on the message channel.
     * 
     * @return The data object.
     * @throws InterruptedException when thread is interrupted
     */
    public Object getq() throws InterruptedException {
        return this._queue.recv();
    }

    /**
     * Method to attempt to object an object from the message channel within the
     * specified timeout value.  This method returns 'null' if it failed to
     * obtain a data object from the message channel within the specified time.
     * 
     * @param msec The timeout value in milliseconds.
     * @return The data object (may be 'null').
     * @throws InterruptedException when thread is interrupted
     */
    public Object getq(long msec) throws InterruptedException {
        return this._queue.recv(msec);
    }

    /**
     * Method to obtain reference to the self thread.  It is a helper method
     * to allow this Runnable object to delegate some of its operations to the
     * owner thread.
     * 
     * @return The owner thread.
     */
    public Thread getSelf() {
        return this._self;
    }
}
