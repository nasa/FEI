/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Defines object format for the message queue.
 * This class defines the object format for the Message Queue.  Additional
 * fields will be added in the future.
 *
 * @author T. Huang
 * @version $Id: MessageBlock.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class MessageBlock {
    /** The actual data. */
    private Object _data;

    /** The reference to the next message block. */
    private MessageBlock _next = null;

    /**
     * Creates new MessageBlock
     * @param data The actual data to store.
     */
    public MessageBlock(Object data) {
        this._data = data;
    }

    /**
     * Creates a new MessageBlock
     * @param data The actual data to store.
     * @param next The next neighbor MessageBlock.
     */
    public MessageBlock(Object data, MessageBlock next) {
        this._data = data;
        this._next = next;
    }

    /**
     * Method to access the internal stored data object.
     * @return The object data
     */
    public Object getData() {
        return this._data;
    }

    /**
     * Method to set the internal store data object.
     * @param data The object data
     */
    public void setData(Object data) {
        this._data = data;
    }

    /**
     * Method to access the next message block.
     * @return The message block.
     */
    public MessageBlock getNext() {
        return this._next;
    }

    /**
     * Method to set the next message block.
     * @param next The next message block.
     */
    void setNext(MessageBlock next) {
        this._next = next;
    }

}
