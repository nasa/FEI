/**
 * JUnit test case for Komodo (FEI5) Client API (Basic Session)
 */
package jpl.mipl.mdms.test.FileService.komodo.api;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import junit.framework.TestCase;

/**
 * JUnit test case for FEI client API using one-parameter URL constructor
 * <p>
 * Copyright 2003, California Institute of Technology. <br>
 * ALL RIGHTS RESERVED. <br>
 * U.S. Government Sponsorship acknowledge. 6/29/2000. <br>
 * MIPL Data Management System (MDMS).
 * <p>
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: ClientURLDomainTest.java,v 1.5 2017/06/05 16:48:29 awt Exp $
 */
public class ClientURLDomainTest extends TestCase {
    private Properties _props = System.getProperties();

    private Client _client = null;

    private boolean _verbose = false;

    private String _linkPrefix = null;

    private String _fileType = null;

    private String _serverGroup = null;

    private String _testFile1 = null;

    private String _testFile2 = null;

    private String _testFile3 = null;

    private String _testFile4 = null;

    private String _testFilename1 = null;

    private String _testFilename2 = null;

    private String _testFilename3 = null;

    private String _testFilename4 = null;

    private URL _domainURL = null;

    private String _trustStore = null;

    private String _userName = null;

    private String _password = null;

    /**
     * Constructor
     * 
     * @param name
     *            the test suite name
     */
    public ClientURLDomainTest(String name) {
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
        // Get resource files needed by Komodo (FEI5) client
        try {
            this._domainURL = new URL(this._props.getProperty("fei.domain.url"));
        } catch (MalformedURLException me) {
            System.out.println(me.getMessage());
        }
        this._trustStore = this._props.getProperty("fei.trust.store");
        System.setProperty(Constants.PROPERTY_SSL_TRUSTSTORE, this._trustStore);
        // Get some files to test file transfers
        this._testFile1 = this._props.getProperty("fei.test.file.1");
        this._testFile2 = this._props.getProperty("fei.test.file.2");
        this._testFile3 = this._props.getProperty("fei.test.file.3");
        this._testFile4 = this._props.getProperty("fei.test.file.4");
        this._testFilename1 = this._testFile1.substring(this._testFile1
                .lastIndexOf(File.separatorChar) + 1);
        this._testFilename2 = this._testFile2.substring(this._testFile2
                .lastIndexOf(File.separatorChar) + 1);
        this._testFilename3 = this._testFile3.substring(this._testFile3
                .lastIndexOf(File.separatorChar) + 1);
        this._testFilename4 = this._testFile4.substring(this._testFile4
                .lastIndexOf(File.separatorChar) + 1);
        this._linkPrefix = this._props.getProperty("fei.vft.link.prefix");
        if (this._verbose) {
            System.out.println("FEI username: " + this._userName);
            System.out.println("FEI file type: " + this._fileType);
            System.out.println("FEI server group: " + this._serverGroup);
            System.out.println("FEI trust store:  " + this._trustStore);
            System.out.println("FEI VFT server link: " + this._linkPrefix);
            System.out.println("Test file 1: " + this._testFile1);
            System.out.println("Test file 2: " + this._testFile2);
            System.out.println("Test file 3: " + this._testFile3);
            System.out.println("Test file 4: " + this._testFile4);
        }
        try {
            this._client = new Client(this._domainURL);
            this._client.login(this._userName, this._password,
                    this._serverGroup, this._fileType);
            // Change directories to redirect the output
            this._client.changeDir(this._props.getProperty("output.dir"));
            System.out.println("Changed dir");
            // Add a single file to use in testing
            this._client.add(this._testFile1, "Adding test.file.1");
            System.out.println("Added file");
            // Get results from add command
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
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
                // Delete the test file from FEI server
                this._client.delete(this._testFilename1);
                // Get results from delete command
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
                // Close client connection
                this._client.logout();
                this._client = null;
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    /**
     * Test case on client 'show' operation
     * 
     * @throws Exception
     *             when general failure
     */
    public void testShow() throws Exception {
        int xactId = this._client.show(this._testFilename1);
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
    }

    /**
     * Test case to show the list of file types
     * 
     * @throws Exception
     *             when general failure
     */
    public void testShowTypes() throws Exception {
        this._client.showTypes();
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
    }

    /**
     * Test case to add and delete multiple files
     * 
     * @throws Exception
     *             when operation failure
     */
    public void testAddDeleteMultipleFiles() throws Exception {
        String[] files = { this._testFile2, this._testFile3, this._testFile4 };
        int xactId = this._client.add(files, "Adding three files to fileType");
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
        // Delete test files
        xactId = this._client.delete("test*");
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
    }

    /**
     * @param args
     */
    public void testShowNoResults() throws Exception {
        Vector v = this._client.showNoResults("*");
        Enumeration e = v.elements();
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement().toString());
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
                + "   -Dfei.file.type=<filetype> \\\n"
                + "   -Dfei.group.name=<server group> \\\n"
                + "   -Dfei.domain.file=<domain file> \\\n"
                + "   -Dfei.trust.store=<keystore> \\\n"
                + "   -Dfei.vft.server.link=<server-side symbolic link> \\\n"
                + "   -Dfei.test.file.1=<test file1> \\\n"
                + "   -Dfei.test.file.2=<test file2> \\\n"
                + "   -Dfei.test.file.3=<test file3> \\\n"
                + "   -Dfei.test.file.4=<test file4> \\\n" + "   "
                + ClientURLDomainTest.class.getName() + " <gui|text>";
        if (args.length != 1) {
            System.out.println(usage);
            return;
        }
        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(ClientURLDomainTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(ClientURLDomainTest.class);
        } else
            System.out.println(usage);
    }
}