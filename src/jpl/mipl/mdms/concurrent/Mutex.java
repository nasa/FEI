/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Implementation of conventional mutual exclusive synch strategy.
 * This class implements the conventional mutual exclusive synchronization
 * strategy.  Mutex behaves similar to the Java synchronized block, except that
 * it requires explicit release of the obtained lock.  This allows developer to
 * have synchronized block behavior in a method without scope limitation.
 *
 * @author T. Huang
 * @version $Id: Mutex.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class Mutex implements Synch {
    /** The boolean lock object. */
    protected boolean _locked = false;

    /**
     * Creates new Mutex.  The lock is not obtained during creation.
     */
    public Mutex() {
        // no-op.
    }

    /**
     * Method to obtain the internal lock.  This is a blocking call until the
     * lock is successfully obtained or exit by interrupt.
     * 
     * @throws InterruptedException when thread is interrupted
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        // first obtain the object lock.
        synchronized (this) {
            try {
                // if the internal lock is not available, then first give up the
                // object lock and wait.
                while (this._locked)
                    this.wait();

                // obtain the lock.
                this._locked = true;
            } catch (InterruptedException e) {
                this.notify();
                throw e;
            }
        }
    }

    /**
     * Method to attempt to obtain the internal lock within the specified time
     * value.  If the attempt failed, then the method returns 'false'.
     * 
     * @param msec The input timeout value.
     * @return true if successfully obtained the internal lock.
     * @throws InterruptedException when thread is interrupted
     */
    public boolean acquire(long msec) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized (this) {
            if (!this._locked) { // obtain the lock if it is available.
                this._locked = true;
                return true;
            } else if (msec <= 0) // return if invlid input time.
                return false;
            else {
                long startTime = System.currentTimeMillis();
                try {
                    while (true) {
                        // wait for the specified time value.
                        this.wait(msec);

                        // when waks up, and the lock is available, then obtain the
                        // lock.
                        if (!this._locked) {
                            this._locked = true;
                            return true;
                        } else { // if wait was canceled by notify, check time.
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
     * Method to release the obtained lock.
     */
    public synchronized void release() {
        this._locked = false;
        this.notify();
    }

}
