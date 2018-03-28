/**
 * JUnit test case for Komodo (FEI5) Client API (changeType())
 */
package jpl.mipl.mdms.test.FileService.komodo.api;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import junit.framework.TestCase;

/**
 * JUnit test case for Komodo (FEI5) Client API (show())
 * <p>
 * Copyright 2004, California Institute of Technology. <br>
 * ALL RIGHTS RESERVED. <br>
 * U.S. Government Sponsorship acknowledge. 6/15/2004. <br>
 * MIPL Data Management System (MDMS).
 * <p>
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: ClientShowTest.java,v 1.4 2004/09/08 00:07:44 rap Exp $
 */
public class ClientShowTest extends TestCase {
    private Properties _props = System.getProperties();

    private Client _client = null;

    private boolean _verbose = false;

    private String _fileType = null, _fileType2 = null, _domainFile = null,
            _serverGroup = null, _trustStore = null, _userName = null,
            _password = null, _testFile1 = null, _testFilename1 = null;

    /**
     * Constructor
     * 
     * @param name
     *            the test suite name
     */
    public ClientShowTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     * 
     * @throws Exception
     *             when general failure
     */
    public void setUp() throws Exception {
        // Get the FEI username, password and fileType from system properties
        this._userName = this._props.getProperty("fei.username");
        this._password = this._props.getProperty("fei.password");
        this._fileType = this._props.getProperty("fei.file.type");
        this._serverGroup = this._props.getProperty("fei.group.name");
        this._fileType2 = this._props.getProperty("fei.file.type2");
        this._testFile1 = this._props.getProperty("fei.test.file.1");
        this._testFilename1 = this._testFile1.substring(this._testFile1
                .lastIndexOf(File.separatorChar) + 1);
        // Get resource files needed by Komodo (FEI5) client
        this._domainFile = this._props.getProperty("fei.domain.file");
        this._trustStore = this._props.getProperty("fei.trust.store");

        if (this._verbose) {
            System.out.println("FEI username: " + this._userName);
            System.out.println("FEI file type: " + this._fileType);
            System.out.println("FEI file type (2): " + this._fileType2);
            System.out.println("FEI server group: " + this._serverGroup);
            System.out.println("FEI domain file: " + this._domainFile);
            System.out.println("FEI trust store:  " + this._trustStore);
        }
        try {
            this._client = new Client(this._domainFile, this._trustStore);
            this._client.login(this._userName, this._password,
                    this._serverGroup, this._fileType);

        } catch (SessionException se) {
            System.out.println("SessionException: " + se.getMessage());
        }
    }

    /**
     * Override parent tearDown method to cleanup after testing.
     * 
     * @throws Exception
     *             when general operation fail
     */
    public void tearDown() throws Exception {
        if (this._client != null) {
            try {
                // Close client connection
                this._client.logout();
                this._client = null;
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    public void testShowNoResults() {
        System.out.println("testShowNoResults() method");
        try {
            this._client.add(this._testFile1);
            // Get results from add operation
            while (this._client.getTransactionCount() > 0) {
                Result r = this._client.getResult();
                if (r == null) {
                    continue;
                }
                if (r.getErrno() == Constants.OK) {
                    // Get the return code for each operation
                    assertEquals(Constants.OK, r.getErrno());
                }
            }

            // Show contents of file type using no results checking show method
            Vector v = this._client.showNoResults("*");
            System.out.println("Results vector contains " + v.size()
                    + " elements.");
            Enumeration e = v.elements();
            while (e.hasMoreElements()) {
                System.out.println(e.nextElement().toString());
            }
        } catch (SessionException se) {
            System.out.println("Session Exception: " + se.getMessage());
            se.printStackTrace();
        }
    }

    public void testShowNoResultsEmptyFileType() {
        System.out.println("testShowNoResultsEmptyFileType() method");
        try {
            this._client.delete("*");
            // Get results from delete operation
            while (this._client.getTransactionCount() > 0) {
                Result r = this._client.getResult();
                if (r == null) {
                    continue;
                }
                if (r.getErrno() == Constants.OK) {
                    // Get the return code for each operation
                    assertEquals(Constants.OK, r.getErrno());
                }
            }

            // Show contents of file type using no results checking show method
            Vector v = this._client.showNoResults("*");
            System.out.println("Results vector contains " + v.size()
                    + " elements.");
            Enumeration e = v.elements();
            while (e.hasMoreElements()) {
                System.out.println(e.nextElement().toString());
            }
        } catch (SessionException se) {
            System.out.println("Session Exception: " + se.getMessage());
            se.printStackTrace();
        }
    }

    public void testShowResults() {
        System.out.println("testShowResults() method");
        try {
            this._client.add(this._testFile1);
            // Get results from add operation
            while (this._client.getTransactionCount() > 0) {
                Result r = this._client.getResult();
                if (r == null) {
                    continue;
                }
                if (r.getErrno() == Constants.OK) {
                    // Get the return code for each operation
                    assertEquals(Constants.OK, r.getErrno());
                }
            }
            // Show contents of file type getting a vector of result objects.
            Vector v = this._client.showResults("*");
            System.out.println("Results vector contains " + v.size()
                    + " elements.");
            Enumeration e = v.elements();
            while (e.hasMoreElements()) {
                Result r = (Result) e.nextElement();
                System.out.println("[RESULT] name: " + r.getName()
                        + " contrib: " + r.getFileContributor() + " size: "
                        + r.getSize());
            }
        } catch (SessionException se) {
            System.out.println("SessionException: " + se.getMessage());
        }
    }

    public void testShowResultsEmptyFileType() {
        System.out.println("testShowResultsEmptyFileType() method");
        try {
            this._client.delete("*");
            // Get results from delete operation
            while (this._client.getTransactionCount() > 0) {
                Result r = this._client.getResult();
                if (r == null) {
                    continue;
                }
                if (r.getErrno() == Constants.OK) {
                    // Get the return code for each operation
                    assertEquals(Constants.OK, r.getErrno());
                }
            }
            // Show contents of file type getting a vector of result objects.
            Vector v = this._client.showResults("*");
            System.out.println("Results vector contains " + v.size()
                    + " elements.");
            Enumeration e = v.elements();
            while (e.hasMoreElements()) {
                Result r = (Result) e.nextElement();
                System.out.println("[RESULT] name: " + r.getName()
                        + " contrib: " + r.getFileContributor() + " size: "
                        + r.getSize());
            }
        } catch (SessionException se) {
            System.out.println("SessionException: " + se.getMessage());
        }
    }

    /**
     * The main method to launch the JUnit TestRunner
     * 
     * @param args
     *            gui|text
     */
    public static void main(String[] args) {
        String usage = "Usage: java [-classpath ...] \\\n"
                + "   -Dfei.username=<username> \\\n"
                + "   -Dfei.password=<password> \\\n"
                + "   -Dfei.file.type=<file type> \\\n"
                + "   -Dfei.server.group=<server group> \\\n"
                + "   -Dfei.test.file.1=<test file path> \\\n"
                + "   -Dfei.domain.file=<domain file> \\\n"
                + "   -Dfei.trust.store=<keystore> \\\n"
                + ClientShowTest.class.getName() + " <gui|text>";
        if (args.length != 1) {
            System.out.println(usage);
            return;
        }
        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(ClientShowTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(ClientShowTest.class);
        } else
            System.out.println(usage);
    }
}