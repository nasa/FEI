/**
 *  Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.concurrent;

/**
 * Wrapper class to turn a task object into a threaded active object.
 * This is a simple Thread wrapper class to turn an Task object in a threaded
 * Active Object.
 *
 * @author T. Huang
 * @version $Id: TaskThread.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class TaskThread extends Thread {
    /** The associated task object. */
    protected Task _task;

    /**
     * Creates new TaskThread
     * @param task The Runnable Task object.
     */
    public TaskThread(Task task) {
        super(task);
        this._task = task;
        this._task.register(this);

    }

    /**
     * Creates new TaskThread
     * @param task The Runnable Task object.
     * @param name The name of this thread.
     */
    public TaskThread(Task task, String name) {
        super(task, name);
        this._task = task;
        this._task.register(this);
    }

    /**
     * Method to override the Thread's toString() method to delegate to the
     * Runnable Task object.
     * @return The String data of the Task object.
     */
    public String toString() {
        return this._task.toString();
    }

}
