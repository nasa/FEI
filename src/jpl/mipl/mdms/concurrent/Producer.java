/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Defines base interface for all channel producer object.
 * This interface defines the base interface for all channel producer object.
 * This interface is designed as a generatic interface to implement
 * publish-and-subscribe frameworks.
 *
 * @author T. Huang
 * @version $Id: Producer.java,v 1.5 2003/09/09 22:55:01 rap Exp $
 */
public interface Producer {
    /**
     * Method to send a data object.
     * @param item The data object.
     * @throws InterruptedException when thread is interrupted
     */
    void send(Object item) throws InterruptedException;

    /**
     * Method to send a data object within the specified time value.
     * @param item The data object.
     * @param msec The input time value in milliseconds.
     * @return true if successful.
     * @throws InterruptedException when thread is interrupted
     */
    boolean send(Object item, long msec) throws InterruptedException;

}
