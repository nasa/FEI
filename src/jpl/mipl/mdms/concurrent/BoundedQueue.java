/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Interface for channel with bounded memory size.
 * This interface defines the required interface for channel with bounded
 * memory size.  It is derived from the Channel interface.
 * 
 * @author T. Huang
 * @version $Id: BoundedQueue.java,v 1.3 2003/09/09 22:55:01 rap Exp $
 */
public interface BoundedQueue extends Queue {
    /**
     * Method to obtain the maximum capacity of the channel.
     *
     * @return the max size of the channel.
     */
    long getMaxCapacity();

}
