/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Base interface for channel objects.
 * This interface deinfes the base interface for channel objects.  A channel
 * is an object that allows data/objects to flow from one point to another.
 * An example of a Channel object is a dynamic Message Queue, which allows
 * clients to insert and remove objects from its managed memory space.
 *
 * @author T. Huang
 * @version $Id: Queue.java,v 1.4 2003/09/09 22:55:01 rap Exp $
 */
public interface Queue extends Producer, Consumer {

    /**
     * Method to send an object to the channel.  This is a blocking call
     * until the object is successfully sent.  An InterruptedException may
     * be thrown to interrupt the blocked operation.
     * 
     * @param item The item to be sent.
     * @throws InterruptedException when thread is interrupted
     */
    void send(Object item) throws InterruptedException;

    /**
     * Method to send an object to the channel within the specified timeout
     * value.
     * 
     * @param item The item to be sent.
     * @param msec The timeout value in milliseconds.
     * @return true if successful.
     * @throws InterruptedException when thread is interrupted
     */
    boolean send(Object item, long msec) throws InterruptedException;

    /**
     * Method to receive an object from the channel.  This is a blocking call
     * until an object is received.  An InterruptedException may be thrown to
     * interrupt the blocked operation.
     *
     * @return A received object.
     * @throws InterruptedException when thread is interrupted
     */
    Object recv() throws InterruptedException;

    /**
     * Method to receive an object from the channel within the specified timeout
     * value.
     * 
     * @param msec The timeout value in milliseconds.
     * @return The received object or null if timedout.
     * @throws InterruptedException when thread is interrupted
     */
    Object recv(long msec) throws InterruptedException;

    /**
     * Method to look ahead into the channel without removing the object from
     * the channel.
     * 
     * @return The next item in the channl or null if empty channel.
     */
    Object lookAhead();

    /**
     * Method to return the current channel size.
     * 
     * @return The current channel size.
     */
    long getSize();

}
