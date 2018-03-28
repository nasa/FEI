/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import jpl.mipl.mdms.concurrent.Mutex;
import jpl.mipl.mdms.connection.Reactor;
import jpl.mipl.mdms.connection.SocketAcceptor;
import jpl.mipl.mdms.connection.SocketConnector;
import jpl.mipl.mdms.utils.FileLogFilter;
import jpl.mipl.mdms.utils.GetOpt;
import jpl.mipl.mdms.utils.MDMS;
import junit.framework.TestCase;

/**
 * This is the main test driver for the acceptor-connector famework. It looks
 * very much like a simple copy program, but with a client/server layer. This
 * program creates both a client and a server object on the same host. The
 * client sends a request for a file to the server object. The service object
 * accepts the request and creates a service handler that will be serve in a
 * service pool to transfer the file from the server to the client. The client
 * takes the incoming data stream and write it to a new file location. All event
 * dispatching and communication management are handled by the
 * acceptor-connector and reactor framework.
 * <p>
 * 
 * @copyright Copyright 2002, California Institute of Technology. ALL RIGHTS
 *            RESERVED. U.S. Government Sponsorship acknowledge. 22-05-2002.
 *            MIPL Data Management System (MDMS).
 *            <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: AcceptorConnectorTest.java,v 1.5 2004/12/23 23:25:12 ntt Exp $
 */
public class AcceptorConnectorTest extends TestCase {
    private Properties _props = System.getProperties();

    // the hostname (localhost or the actual host name).
    private String _hostname;

    // the TCP port to be used.
    private int _port;

    private String _portStr;

    // the input file name, which will be shipped to the server.
    private String _input;

    // the output file name, which the client uses for writing.
    private String _output;

    /**
     * Constructor
     * 
     * @param name the test suite name
     */
    public AcceptorConnectorTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     * 
     * @throws Exception when general failure
     */
    public void setUp() throws Exception {
        this._hostname = this._props.getProperty("conn.acceptorhost");
        this._portStr = this._props.getProperty("conn.acceptorport");
        this._port = new Integer(this._portStr).intValue();
        this._input = this._props.getProperty("conn.acceptorinput");
        this._output = this._props.getProperty("conn.acceptoroutput");
        String[] args = { "-h", this._hostname, "-p", this._portStr, "-i",
                this._input, "-o", this._output };
        TestConfigurator.activate(args, "TestAcceptorConnector");
    }

    /**
     * Override parent tearDown method to cleanup after testing.
     */
    public void tearDown() {
        TestConfigurator.deactivate();
    }

    /**
     * Method to start the test.
     * 
     * @throws Exception when general failure
     */
    public void testAcceptor() throws Exception {
        try {
            // creates a Reactor with an input timeout. That is, the Reactor
            // will terminate will no event occurs of the input time unit.
            Reactor reactor = new Reactor(MDMS.DEFAULT_TIMEOUT * 10);
            MDMS.DEBUG("<Main>Service Timeout: " + MDMS.DEFAULT_TIMEOUT * 10
                    + "ms.</Main>");

            // creates our acceptor object (a.k.a. the server).
            SocketAcceptor acceptor = new SocketAcceptor(reactor,
                    TestAcceptFactory.instance(), this._port);

            // now create the client.
            InetSocketAddress addr = new InetSocketAddress(InetAddress
                    .getByName(this._hostname), this._port);
            TestConnectHandler handler = new TestConnectHandler(reactor,
                    this._input, this._output);

            SocketConnector connector = new SocketConnector(reactor);

            // register our client to the connector.
            connector.connect(handler, addr);

            // activate the infinite loop until no event occurs for the
            // specified timeout value.
            reactor.run();
        } catch (IOException ex) {
            MDMS.ERROR("<Main>" + ex.getMessage() + "</Main>");
        }
    }
}

/**
 * MDMS Test Configurator
 * 
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: AcceptorConnectorTest.java,v 1.5 2004/12/23 23:25:12 ntt Exp $
 */

class TestConfigurator {

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
     */
    protected TestConfigurator(String[] args, String name) {
        this._getOpt = new GetOpt(args, "d:o:");
        this._parseArgs();
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
                    TestConfigurator._instance = new TestConfigurator(args,
                            name);

                MDMS.DEBUG("Performing <" + name + "> test");
                TestConfigurator._startTime = System.currentTimeMillis();
                MDMS.DEBUG(new Date(TestConfigurator._startTime).toString()
                        + "\n");
                TestConfigurator._lock.release();
            } catch (InterruptedException e) {
                TestConfigurator._lock.release();
                System.err.println(e);
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
            MDMS.DEBUG("\n" + "Elapsed Time: "
                    + (stopTime - TestConfigurator._startTime) + "ms.");
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
     */
    private void _parseArgs() {
        String str;
        while ((str = this._getOpt.nextArg()) != null) {
            switch (str.charAt(0)) {
            case 'd':
                this._debug = true;
                MDMS.enableDebug();
                break;
            case 'o':
                try {
                    Handler fhandler = new FileHandler(this._getOpt
                            .getArgValue());
                    fhandler.setFormatter(new SimpleFormatter());
                    fhandler.setFilter(new FileLogFilter());
                    this._handler = new MemoryHandler(fhandler,
                            MDMS.HANDLER_BUF_SIZE, Level.ALL);
                    MDMS.registerLogHandler(this._handler);
                } catch (IOException e) {
                    this._handler = new StreamHandler(System.out,
                            new SimpleFormatter());
                    MDMS.registerLogHandler(new MemoryHandler(this._handler,
                            MDMS.HANDLER_BUF_SIZE, Level.ALL));
                    MDMS.ERROR("Failed to create File Handler");
                } catch (ParseException pEx) {
                    this._handler = new StreamHandler(System.out,
                                         new SimpleFormatter());
                    MDMS.registerLogHandler(new MemoryHandler(this._handler,
                            MDMS.HANDLER_BUF_SIZE, Level.ALL));
                    MDMS.ERROR("Could not retrieve argument value for 'o'."+
                               "Failed to create File Handler");
                }
                break;
            default:
                MDMS.ERROR("Failed to parse arg");
            }
        }
    }

    public static void main(String args[]) {
        junit.swingui.TestRunner.run(AcceptorConnectorTest.class);
    }
}