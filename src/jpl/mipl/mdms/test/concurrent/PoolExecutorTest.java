/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.concurrent;

import junit.framework.TestCase;
import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * Pool Executor Test
 * 
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: PoolExecutorTest.java,v 1.2 2003/09/10 18:35:54 rap Exp $
 */

public class PoolExecutorTest extends TestCase {
    private PooledExecutor _pool;

    /**
     * Constructor
     *
     * @param name the test suite name
     */
    public PoolExecutorTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     *
     * @throws Exception when general failure
     */
    public void setUp() throws Exception {
        this._pool = new PooledExecutor(new BoundedBuffer(10), 100);
        this._pool.setMinimumPoolSize(3);
    }

    /**
     * Override parent tearDown method to cleanup after testing.
     */
    public void tearDown() {
        this._pool = null;
    }

    /**
     * Simple test of getPassword() method
     * 
     * @throws Exception when general failure
     */
    public void testPoolExecutor() throws Exception {
        TestPoolExec test = new TestPoolExec(this._pool);
        this._pool.execute(test);
        this._pool.shutdownAfterProcessingCurrentlyQueuedTasks();
    }
}

/**
 * Test utility class that provides some work for the Executor pool
 */
class TestPoolExec implements Runnable {
    private Executor _pool = null;
    private int count = 5;

    /**
     * Constructor
     * 
     * @param pool the Executor pool
     */
    public TestPoolExec(Executor pool) {
        this._pool = pool;
    }

    /**
     * The work to be preformed by the test
     * 
     * @return error code
     */
    public int service() {
        System.out.println("service");

        --count;
        try {
            this._pool.execute(this);
            return 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Create a new thread and run it
     */
    public void run() {
        while (count > 0) {
            System.out.println("run");
            this.service();
        }
    }
}
