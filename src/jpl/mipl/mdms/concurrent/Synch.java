/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Interface for Locks, mutex, and condition waits.
 *
 * @author T. Huang
 * @version $Id: Synch.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */

public interface Synch {

    /**
     * This method blocks (may be forever) until it has successfully obtain
     * a lock.  The only other way for this method to have early exit is by
     * an interruption.  If the caller receives an exception during this
     * method call, it means the attempt to obtain the lock failed.
     * 
     * @throws InterruptedException when thread is interrupted
     */
    void acquire() throws InterruptedException;

    /**
     * Timed acquire.  This method tries to obtain a lock within the
     * specified milliseconds time.  The time variable does not guarantee
     * the method will retun at the exact time, due to the architectural
     * limitation of the Java language.  In addition, the internal timers in
     * Java does not stop during garbage collection, so timeout can occur
     * because of GC intervended.
     * 
     * @param msec the number of milliseconds to wait.  If a value of zero or
     * negative is assigned, then the method will not wait.
     * @return true if acquire.
     * @throws InterruptedException when thread is interrupted
     */
    boolean acquire(long msec) throws InterruptedException;

    /**
     * This method releases the acquired lock to allow other thread(s) to
     * have access to the shared resource.
     */
    void release();

}
