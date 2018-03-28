/**
 *  @copyright Copyright 2004, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */
package jpl.mipl.mdms.test.FileService.komodo.api;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import junit.framework.TestCase;

/**
 * JUnit test case for Komodo (FEI5) Client API (Basic Session)
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: ClientTest.java,v 1.34 2004/09/30 20:42:25 rap Exp $
 */
public class ClientTest extends TestCase {
    private Properties _props = System.getProperties();

    private Client _client = null;

    private boolean _verbose = false;

    private String _fileType = null, _serverGroup = null, _testFile1 = null,
            _testFile2 = null, _testFile3 = null, _testFile4 = null,
            _testFilename1 = null, _testFilename2 = null,
            _testFilename3 = null, _testFilename4 = null, _domainFile = null,
            _trustStore = null, _userName = null, _password = null;

    /**
     * Constructor
     * 
     * @param name the test suite name
     */
    public ClientTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     * 
     * @throws Exception when general failure
     */
    public void setUp() throws Exception {
        // Get the FEI username, password and fileType from system properties
        this._userName = this._props.getProperty("fei.username");
        this._password = this._props.getProperty("fei.password");
        this._fileType = this._props.getProperty("fei.file.type");
        this._serverGroup = this._props.getProperty("fei.group.name");
        // Get resource files needed by Komodo (FEI5) client
        this._domainFile = this._props.getProperty("fei.domain.file");
        this._trustStore = this._props.getProperty("fei.trust.store");
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
        if (this._verbose) {
            System.out.println("FEI username: " + this._userName);
            System.out.println("FEI file type: " + this._fileType);
            System.out.println("FEI server group: " + this._serverGroup);
            System.out.println("FEI domain file: " + this._domainFile);
            System.out.println("FEI trust store:  " + this._trustStore);
            System.out.println("Test file 1: " + this._testFile1);
            System.out.println("Test file 2: " + this._testFile2);
            System.out.println("Test file 3: " + this._testFile3);
            System.out.println("Test file 4: " + this._testFile4);
        }
        try {
            this._client = new Client(this._domainFile, this._trustStore);
            this._client.login(this._userName, this._password,
                    this._serverGroup, this._fileType);
            // Change directories to redirect the output
            this._client.changeDir(this._props.getProperty("output.dir"));
            // Add a single file to use in testing
            this._client.add(this._testFile1, "Adding test.file.1");
            // Get results for add operation
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
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Override parent tearDown method to cleanup after testing.
     * 
     * @throws Exception when general operation fail
     */
    public void tearDown() throws Exception {
        if (this._client != null) {
            try {
                // Delete the test file from FEI server
                this._client.delete(this._testFilename1);
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
     * @throws Exception when general failure
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
     * @throws Exception when general failure
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
     * @throws Exception when operation failure
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
     * 
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
     * 
     * @param args
     */
    public void testGroupList() throws Exception {
        LinkedList list = this._client.getGroupList();
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }
    }

    /**
     * The main method to launch the JUnit TestRunner
     * 
     * @param args gui|text
     */
    public static void main(String[] args) {
        String usage = "Usage: java [-classpath ...] \\\n"
                + "   -Dfei.username=<username> \\\n"
                + "   -Dfei.password=<password> \\\n"
                + "   -Dfei.file.type=<filetype> \\\n"
                + "   -Dfei.server.group=<server group> \\\n"
                + "   -Doutput.dir=<output directory> \\\n"
                + "   -Dfei.domain.file=<domain file> \\\n"
                + "   -Dfei.trust.store=<keystore> \\\n"
                + "   -Dfei.test.file.1=<test file1> \\\n"
                + "   -Dfei.test.file.2=<test file2> \\\n"
                + "   -Dfei.test.file.3=<test file3> \\\n"
                + "   -Dfei.test.file.4=<test file4> \\\n" + "   "
                + ClientTest.class.getName() + " <gui|text>";
        if (args.length != 1) {
            System.out.println(usage);
            return;
        }
        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(ClientTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(ClientTest.class);
        } else
            System.out.println(usage);
    }
}