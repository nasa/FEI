/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Implementation of latch synchronization strategy.
 * A Latch is an implementation of the synchronization strategy that is only
 * need to be enabled once.  A good example of the latch usage is to allow the
 * main thread to have control when the service threads get activated.
 * <pre>
 * public class ServiceHandle extends Task
 * {
 *    protected final Latch _lock;
 *
 *    public ServiceHandle (Latch lock)
 *    {
 *       this._lock = lock;
 *    }
 *
 *    public void run ()
 *    {
 *       try
 *       {
 *          this._lock.acquire();
 *          this._service();
 *
 *       }
 *       catch (InterruptedException e)
 *       {
 *          MDMS.DEBUG ("Interrupted");
 *       }
 *    }
 *
 *    protected void _service() { ... }
 * }
 *
 * public class Server
 * {
 *    public static void main (String args[])
 *    {
 *       Latch lock = new Latch();
 *
 *       for (int i=0; i<10; ++i)
 *          new TaskThread (new ServiceHandle(lock)).start();
 *
 *       // additional setups...
 *
 *       lock.release();  // release lock to activate all service handles.
 *    }
 * }
 * </pre>
 * 
 * @author T. Huang
 * @version $Id: Latch.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 * @see jpl.mipl.mdms.concurrent.Task
 * @see jpl.mipl.mdms.concurrent.TaskThread
 */
public class Latch implements Synch {
    /**
     * The latch switch.
     */
    protected boolean _latched = false;

    /**
     * Creates new Latch
     */
    public Latch() {
        // no-op.
    }

    /**
     * Method to implement the qcquire method in the Synch interface.  This
     * method blocks forever until the latch switch is enabled or an
     * interrupt exception occurs.
     * @throws InterruptedException when thread is interrupted
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized (this) {
            // blocked until latch is switched on.
            while (!this._latched)
                this.wait();
        }
    }

    /**
     * Method implements the timeout acquire method in the Synch interface.
     * This method blocks until timedout or the latch switch is enabled.  An
     * InterruptedException can also be thrown.  This method returns false when
     * the input timeout is 0 or negative.
     * 
     * @param msec The timeout value in milliseconds.
     * @return true if acquire was successful.
     * @throws InterruptedException when thread is interrupted
     */
    public boolean acquire(long msec) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized (this) {
            if (this._latched)
                return true;
            else if (msec <= 0)
                return false;
            else {
                long startTime = System.currentTimeMillis();
                while (true) {
                    // block for the specified time duration.
                    this.wait(msec);
                    if (this._latched)
                        return true;
                    else {
                        // when wakeup, first check the time duration.
                        msec -= (System.currentTimeMillis() - startTime);
                        if (msec <= 0)
                            return false;
                    }
                }
            }
        }
    }

    /**
     * Method implements the release method in Synch interface.  This method
     * is usually called by the parent thread to signal all waiting tasks to
     * begin working.
     */
    public synchronized void release() {
        this._latched = true;
        this.notifyAll();
    }

}
