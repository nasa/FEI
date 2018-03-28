/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Implementation of conventional semaphore synch strategy.
 * This class implements the conventional semaphore synchronization
 * strategy.  This class avoides application programs from creating too many
 * locks by using pre-initialized lock count value.  Each call to
 * acquire will decrement the internal count.  If the  internal lock count is
 * zero, then the acquire call will be blocked.
 *
 * @author T. Huang
 * @version $Id: Semaphore.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class Semaphore implements Synch {
    /** The internal lock count */
    protected long _count;

    /** The maximum number of locks */
    protected final long _maxCount;

    /**
     * Creates a Semaphore object with input lock count value.
     * @param count The input count value.
     */
    public Semaphore(long count) {
        this._count = count;
        this._maxCount = count;
    }

    /**
     * Method to acquire the lock.  The internal count value will be decremented
     * if more locks are avaiable.  if the interal count value is zero, then
     * this method will be blocked until more locks are available.
     * @throws InterruptedException when thread is interrupted
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized (this) {
            try {
                while (this._count <= 0)
                    this.wait();
                --this._count;
            } catch (InterruptedException e) {
                this.notify();
                throw e;
            }
        }
    }

    /**
     * Method to acquire the lock within the specified timeout value.  This
     * method returns 'false' if it failed to obtain the lock within the input
     * time.
     * @param msec The timeout value in milliseconds.
     * @return true if successful.
     * @throws InterruptedException when thread is interrupted
     */
    public boolean acquire(long msec) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        // obtain object lock before attempt to check and update the internal
        // lock count.
        synchronized (this) {
            if (this._count > 0) {
                --this._count;
                return true;
            } else if (msec <= 0)
                return false;
            else {
                try {
                    long startTime = System.currentTimeMillis();

                    while (true) {
                        // gives up the object lock and wait.
                        this.wait(msec);
                        if (this._count > 0) {
                            --this._count;
                            return true;
                        } else {
                            msec -= (System.currentTimeMillis() - startTime);
                            if (msec <= 0)
                                return false;
                        }
                    }
                } catch (InterruptedException e) {
                    this.notify();
                    throw e;
                }
            }
        }
    }

    /**
     * Method to release an obtained lock.  For Sempahore, this is very efficient
     * since it only has to increment the available number of locks and let
     * all waiting threads know.
     */
    public synchronized void release() {
        if (this._count < this._maxCount) {
            // this requires object lock to allow atomic update of internal count.
            ++this._count;
            this.notify();
        }
    }

    /**
     * Method to release a list of locks.  Instead of calling release() to
     * release one lock at a time, the method release a number of lock at once.
     * @param count The input lock count.
     */
    public synchronized void release(long count) {
        if (count < 0 || ((count + this._count) > this._maxCount))
            throw new IllegalArgumentException("Invalid Semaphore count argument");

        this._count += count;
        for (long i = 0; i < count; ++i)
            this.notify();
    }

    /**
     * Method to obtain the current number of available locks to obtain.
     * @return The current available lock(s).
     */
    public synchronized long getCount() {
        return this._count;
    }

}
