/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Implementation of null synch strategy.
 * This class implements the null synchronization strategy.  This stragegy is
 * useful when devloping pluggable concurrent and locking model.  An example is
 * that a single threaded application requires no synchronization.
 *
 * @author T. Huang
 * @version $Id: NullSynch.java,v 1.4 2003/09/09 22:55:01 rap Exp $
 */
public class NullSynch implements Synch {
    /**
     * Method to do-nothing acquire lock.
     * @throws InterruptedException when interrupted
     */
    public synchronized void acquire() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
    }

    /**
     * Method to do-nothing timed acquire lock.
     * @param msec The input timeout value.
     * @return 'true' if lock is acquired
     * @throws InterruptedException when interrupted
     */
    public synchronized boolean acquire(long msec)
        throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        return true;
    }

    /**
     * Method to do-nothing to release lock.
     */
    public synchronized void release() {
        // no-op.
    }

}
