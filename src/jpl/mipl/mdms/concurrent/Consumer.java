/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Interface for all channel consumer object.
 * This interface defines the base interface for all channel consumer object.
 * This interface is designed as a generatic interface to implement
 * publish-and-subscribe frameworks.
 *
 * @author T. Huang
 * @version $Id: Consumer.java,v 1.4 2003/09/09 22:55:01 rap Exp $
 */
public interface Consumer {

    /**
     * Method to receive an object from the event source.  This is designed to
     * be a blocking call until an object is returned or an exception occurs.
     * 
     * @return The received object.
     * @throws InterruptedException when thread is interrupted
     */
    Object recv() throws InterruptedException;

    /**
     * Method to receive an object from the event source within the specified
     * timeout value.  This method will either return an object or null if
     * request timed out.
     * 
     * @param msec timeout time in msec
     * @return The received object reference or null if timed out.
     * @throws InterruptedException when thread is interrupted
     */
    Object recv(long msec) throws InterruptedException;

}
