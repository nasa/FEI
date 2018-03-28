/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Thread-safe implementation of a message queue channel.
 * This is a thread-safe implementation of a message queue channel.  It is
 * similar to the standard Java List collection object except that it is
 * defined based on the Channel interface and it has better control on
 * multi-thread access.
 *
 * @author T. Huang
 * @version $Id: MessageQueue.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class MessageQueue implements Queue {
    /** The head of the queue. */
    protected MessageBlock _head;

    /** The tail of the queue. */
    protected MessageBlock _tail;

    /** The internal access lock */
    protected final Object _lock = new Object();

    /** The counter use for kee track of waiting thread count. */
    protected int _waitCount;

    /** The internal size */
    private long _size = 0;

    /**
     * Creates new MessageQueue
     */
    public MessageQueue() {
        // first insert a blank list head.
        this._head = new MessageBlock(null);
        this._tail = this._head;
    }

    /**
     * Internal method to insert an object to the end of the queue.  This method
     * does not require to lock the entire object.  It first locks the process
     * of creating the message block and access the waiting thread count.  The
     * then, apply exclusive lock to the tail to ensure thread-safe insertion of
     * the new item.
     * @param item The data object.
     */
    protected void _enqueue(Object item) {
        synchronized (this._lock) {
            MessageBlock mb = new MessageBlock(item);
            // first lock the end of the queue, since it will be modifed.
            synchronized (this._tail) {
                this._tail.setNext(mb);
                this._tail = mb;
                ++this._size;
            }
            // if there are waiting threads, then signal them to wake up.
            if (this._waitCount > 0)
                this._lock.notify();
        }
    }

    /**
     * Internal method to remove an item from the head of the queue.  This
     * method is synchronized to ensure only one thread at any given time to
     * be able to remove item from the queue.
     * @return The first non-null object on the queue.
     */
    protected synchronized Object _degueue() {
        // first lock the access to the head of the queue, since another thread
        // may try to read the head while this method is trying to remove the
        // head :-).
        synchronized (this._head) {
            Object item = null;
            MessageBlock first = this._head.getNext();
            if (first != null) {
                item = first.getData();
                first.setData(null);
                this._head = first;
                --this._size;
            }
            return item;
        }
    }

    /**
     * Method to implement the Channel send(Object) method.  Again, this is a
     * blocking call until the end of the list is available for insert.
     * @param item The object to be inserted.
     * @throws InterruptedException when thread is interrupted
     * @throws IllegalArgumentException when argument failure
     */
    public void send(Object item)
        throws InterruptedException, IllegalArgumentException {
        // the processes of verifying the input item and interrupt detection
        // do not have to be synchronized.  Only the action of physically
        // insert the object needs to be synchronized.
        if (item == null)
            throw new IllegalArgumentException("Null Object");
        if (Thread.interrupted())
            throw new InterruptedException();
        this._enqueue(item);
    }

    /**
     * Method to implement the Channel's send(Object, long) method with
     * specified timeout value.  Since this message queue is implemented such
     * that the process of inserting an item does not require a lot of wait time,
     * the timeout value is ignored.
     * @param item The data object.
     * @param msec The timeout value in milliseconds.  (ignored).
     * @return The status of the timed send operation.
     * @throws InterruptedException when thread is interrupted
     */
    public boolean send(Object item, long msec) throws InterruptedException {
        this.send(item);
        return true;
    }

    /**
     * Method to impelement the Channel's recv() method, to remove a non-null
     * object from the queue.  This is a blocking call until a non-null data
     * is successfully removed from the queue.
     * @return The non-null data object.
     * @throws InterruptedException when thread is interrupted
     */
    public Object recv() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        Object item = this._degueue();

        // if a null-object is returned from _degueue, then this thread will wait.
        if (item != null)
            return item;
        else {
            // first obtain the lock to have access to update the wait counter.
            synchronized (this._lock) {
                try {
                    ++this._waitCount;
                    while (true) {
                        item = this._degueue();
                        // if a non-null object is returned, then decrement the
                        // wait count and return the object.
                        if (item != null) {
                            --this._waitCount;
                            return item;
                        } else {
                            // otherwise, this thread gives up the global lock and
                            // enter the wait state.
                            this._lock.wait();
                        }
                    }
                } catch (InterruptedException e) {
                    --this._waitCount;
                    this._lock.notify();
                    throw e;
                }
            }
        }
    }

    /**
     * Mehtod to impelement the Channel's recv(long) method to remove an item
     * from the queue with timed wait.  This method will try to remove an item
     * from the queue within the specified time.  If time expired, then a null
     * object is returned.
     * @param msec The timeout value.
     * @return The data object, but it may be null.
     * @throws InterruptedException when thread is interrupted
     */
    public Object recv(long msec) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        Object item = this._degueue();
        if (item != null)
            return item;
        else {
            synchronized (this._lock) {
                try {
                    long startTime =
                        (msec <= 0) ? 0 : System.currentTimeMillis();
                    ++this._waitCount;
                    while (true) {
                        item = this._degueue();
                        if (item != null || msec <= 0) {
                            --this._waitCount;
                            return item;
                        } else {
                            // gives up the globl lock and enter a timed wait state.
                            this._lock.wait(msec);

                            // calculate the total wait time.
                            msec -= (System.currentTimeMillis() - startTime);
                        }
                    }
                } catch (InterruptedException e) {
                    --this._waitCount;
                    this._lock.notify();
                    throw e;
                }
            }
        }
    }

    /**
     * Method to impelement the Channel's lookAhead() method to look at the head
     * of the queue without removing it from the queue.
     * 
     * @return The data object at the head of the list.  It could be null.
     */
    public Object lookAhead() {
        // lock access to the head node.
        synchronized (this._head) {
            MessageBlock first = this._head.getNext();
            if (first != null)
                return first.getData();
            else
                return null;
        }
    }

    /**
     * Method to check if the queue is empty.
     * @return true if it is empty.
     */
    public boolean isEmpty() {
        synchronized (this._head) {
            return this._head.getNext() == null;
        }
    }

    /**
     * Method to impelement the Channel's getSize() method to return the current
     * queue size.
     * @return the current queue size.
     */
    public long getSize() {
        // need to lock access to the head of the list.
        synchronized (this._head) {
            return this._size;
        }
    }
}
