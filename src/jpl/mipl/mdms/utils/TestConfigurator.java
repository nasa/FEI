/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.utils;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import jpl.mipl.mdms.concurrent.Mutex;

/**
 * MDMS Test Configurator
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestConfigurator.java,v 1.5 2004/12/23 23:24:59 ntt Exp $
 */
public class TestConfigurator {

    private GetOpt _getOpt = null;
    private boolean _debug = false;
    private Handler _handler = null;
    private static TestConfigurator _instance = null;
    private static long _startTime = 0;
    private static Mutex _lock = new Mutex();

	/**
	 * Default constructor
	 */
	protected TestConfigurator() {
		System.out.println("Created");
	}
	
	/**
	 * Constructor 
	 * 
	 * @param args command line arguments
	 * @param name the name
     * @throws ParseException if unable to parse arguments
	 */
	protected TestConfigurator(String[] args, String name) 
                                                    throws ParseException {
		this._getOpt = new GetOpt(args, "d:o:");
        try {
            this._parseArgs();
        } catch (ParseException pEx) {
            throw pEx;
            //throw new RuntimeException("Unable to parse arguments", pEx);
        }
		System.out.println("name: " + name);
	}

    /**
     * Activate the tests
     * 
     * @param args the command line args
     * @param name the name
     */
    public static void activate(String[] args, String name) {
        if (TestConfigurator._instance == null) {
            try {
                TestConfigurator._lock.acquire();

                if (TestConfigurator._instance == null)
                    TestConfigurator._instance =
                        new TestConfigurator(args, name);

                MDMS.DEBUG("Performing <" + name + "> test");
                TestConfigurator._startTime = System.currentTimeMillis();
                MDMS.DEBUG(
                    new Date(TestConfigurator._startTime).toString() + "\n");
                TestConfigurator._lock.release();
            } catch (InterruptedException e) {
                TestConfigurator._lock.release();
                System.err.println(e);
                return;
            } catch (ParseException pEx) {
                TestConfigurator._lock.release();
                System.err.println(pEx);
                return;
            }
        }
    }

    /**
     * Deactivate
     */
    public static void deactivate() {
        TestConfigurator._instance._shutdown();
    }

    /**
     * shutdown tests
     */
    protected void _shutdown() {
        try {
            TestConfigurator._lock.acquire();

            long stopTime = System.currentTimeMillis();
            MDMS.DEBUG(
                "\n"
                    + "Elapsed Time: "
                    + (stopTime - TestConfigurator._startTime)
                    + "ms.");
            MDMS.DEBUG(new Date(stopTime).toString() + "\n");

            TestConfigurator._lock.release();
        } catch (InterruptedException e) {
            TestConfigurator._lock.release();
            System.err.println(e);
        }

        if (TestConfigurator._instance._handler != null) {
            try {
                TestConfigurator._instance._handler.flush();
                //TestConfigurator._instance.logger__.close();
            } catch (Exception e) {
                System.err.println("Unable to close logger");
                System.err.println(e);
            }
        }
        if (TestConfigurator._instance._debug) {
            MDMS.disableDebug();
            TestConfigurator._instance._debug = false;
        }
    }

    /**
     * Finalize
     * 
     * @throws Throwable when ?
     */
    protected void finalize() throws Throwable {
        this._shutdown();
    }

    /**
     * Parse arguments
     * @throws ParseException if unable to parse arguments.
     */
    private void _parseArgs() throws ParseException {
        String str;
        while ((str = this._getOpt.nextArg()) != null) {
            switch (str.charAt(0)) {
                case 'd' :
                    this._debug = true;
                    MDMS.enableDebug();
                    break;
                case 'o' :
                    try {
                        Handler fhandler =
                            new FileHandler(this._getOpt.getArgValue());
                        fhandler.setFormatter(new SimpleFormatter());
                        fhandler.setFilter(new FileLogFilter());
                        this._handler =
                            new MemoryHandler(
                                fhandler,
                                MDMS.HANDLER_BUF_SIZE,
                                Level.INFO);
                        MDMS.registerLogHandler(this._handler);
                    } catch (IOException e) {
                        this._handler =
                            new StreamHandler(
                                System.out,
                                new SimpleFormatter());
                        MDMS.registerLogHandler(
                            new MemoryHandler(
                                this._handler,
                                MDMS.HANDLER_BUF_SIZE,
                                Level.INFO));
                        MDMS.ERROR("Failed to create File Handler");
                    }
                    break;
            }
        }
    }
}
