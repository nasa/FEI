/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */
package jpl.mipl.mdms.test.FileService.komodo.api;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import junit.framework.TestCase;

/**
 * JUnit test case for Komodo (FEI5) Client API (Get Commands)
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: GetCmdsTest.java,v 1.9 2004/09/08 00:07:44 rap Exp $
 */
public class GetCmdsTest extends TestCase {
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

    private String _username = null;

    private String _password = null;

    private String _domainFile = null;

    private String _trustStore = null;

    private Date _date = null;

    /**
     * Constructor
     * 
     * @param name
     *            the test suite name
     */
    public GetCmdsTest(String name) {
        super(name);
        // Get the current datetime
        this._date = new Date();
        // Subtract 1 day from today's date.
        this._date.setTime(this._date.getTime() - 86400000);
        // Get the FEI username, password and fileType from system properties
        this._username = this._props.getProperty("fei.username");
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
        this._linkPrefix = this._props.getProperty("fei.vft.link.prefix");
        if (this._verbose) {
            System.out.println("FEI username: " + this._username);
            System.out.println("FEI file type: " + this._fileType);
            System.out.println("FEI server group: " + this._serverGroup);
            System.out.println("FEI domain file: " + this._domainFile);
            System.out.println("FEI trust store:  " + this._trustStore);
            System.out.println("FEI VFT server link: " + this._linkPrefix);
            System.out.println("Test file 1: " + this._testFile1);
            System.out.println("Test file 2: " + this._testFile2);
            System.out.println("Test file 3: " + this._testFile3);
            System.out.println("Test file 4: " + this._testFile4);
        }
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     * 
     * @throws Exception
     *             when general failure
     */
    public void setUp() throws Exception {
        try {
            // Open a client connection
            this._client = new Client(this._domainFile, this._trustStore);
            this._client.login(this._username, this._password,
                    this._serverGroup, this._fileType);
            // Change directories to redirect the output
            this._client.changeDir(this._props.getProperty("output.dir"));
            // Set replacefile option so we don't get spurious "File Already
            // Exists" errors
            this._client.set("replacefile", true);

            // Async call to add a single file to a file type
            this._client.add(this._testFile1, "Adding test.file.1");
            // Get results from file add command
            while (this._client.getTransactionCount() > 0) {
                Result r = this._client.getResult();
                if (r == null) {
                    continue;
                }
                assertEquals(Constants.OK, r.getErrno());
            }
        } catch (SessionException se) {
            System.out.println("Caught Session Exception: " + se.getMessage());
        } catch (Exception e) {
            System.out.println("Caught Exception: " + e.getMessage());
            throw new Exception(e.getMessage());
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
                // Async call to delete file from file type
                this._client.delete(this._testFilename1);
                // Get results from delete command
                while (this._client.getTransactionCount() > 0) {
                    Result r = this._client.getResult();
                    if (r == null) {
                        continue;
                    }
                    assertEquals(Constants.OK, r.getErrno());
                }

                // Delete local filesystem file
                this.removeFile(this._props.getProperty("output.dir") + "/"
                        + this._testFilename1);
                // Close client connection
                this._client.logout();
                this._client = null;
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    /**
     * Test case on client 'get' operation
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGet() throws Exception {
        // Async call to get a single file from a file type
        int xactId = this._client.get(this._testFilename1);
        // Get results from file get command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'get' operation with the restart option set.
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetRestart() throws Exception {
        this._client.set("restart", true);
        // Async call to get a single file from a file type, restart on
        int xactId = this._client.get(this._testFilename1);
        // Get results from file get command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'get' operation with the restart and computeChecksum
     * options set.
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetRestartChecksum() throws Exception {
        this._client.set("restart", true);
        this._client.set("computechecksum", true);
        // Async call to get a single file from a file type,
        // restart and computechecksum options enabled
        int xactId = this._client.get(this._testFilename1);
        // Get results from file get command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getAfter' operation
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetAfter() throws Exception {
        System.out.println("getAfter " + this._date.toString());
        // Async call to get all files after a specified date
        int xactId = this._client.getAfter(this._date);
        // Get results from getAfter command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getAfter' operation with restart option set
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetAfterRestart() throws Exception {
        this._client.set("restart", true);
        System.out.println("getAfter " + this._date.toString());
        // Async call to get all files after a specified date, restart
        // option is enabled
        int xactId = this._client.getAfter(this._date);
        // Get results from getAfter command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getAfter' operation with restart and computeChecksum
     * options set
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetAfterRestartChecksum() throws Exception {
        this._client.set("restart", true);
        this._client.set("computeChecksum", true);
        // Async call to get all files after a specified date, restart
        // and computeChecksum options are enabled
        System.out.println("getAfter " + this._date.toString());
        // Get results from getAfter command
        int xactId = this._client.getAfter(this._date);
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getBetween' operation
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetBetween() throws Exception {
        // Get the current datetime
        Date tomorrow = new Date();
        // Add 1 day to today's date.
        tomorrow.setTime(tomorrow.getTime() + 86400000);
        System.out.println("getBetween " + this._date.toString() + " and "
                + tomorrow.toString());
        // Async call to get all files between a date range
        int xactId = this._client.getBetween(this._date, tomorrow);
        // Get results from getBetween commmand
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getBetween' operation with restart option set
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetBetweenRestart() throws Exception {
        this._client.set("restart", true);
        // Get the current datetime
        Date tomorrow = new Date();
        // Add 1 day to today's date.
        tomorrow.setTime(tomorrow.getTime() + 86400000);
        System.out.println("getBetween " + this._date.toString() + " and "
                + tomorrow.toString());
        // Async call to get all files between a date range, restart
        // option enabled
        int xactId = this._client.getBetween(this._date, tomorrow);
        // Get results from getBetween command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getBetween' operation with restart and
     * computeChecksum options set
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetBetweenRestartChecksum() throws Exception {
        this._client.set("restart", true);
        this._client.set("computeChecksum", true);
        // Get the current datetime
        Date tomorrow = new Date();
        // Add 1 day to today's date.
        tomorrow.setTime(tomorrow.getTime() + 86400000);
        System.out.println("getBetween " + this._date.toString() + " and "
                + tomorrow.toString());
        // Async call to get all files between a date range, restart
        // and computeChecksum options are enabled
        int xactId = this._client.getBetween(this._date, tomorrow);
        // Get results from getBetween command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getLatest' operation
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetLatest() throws Exception {
        // Async call to get the latest file in a file type
        int xactId = this._client.getLatest(this._testFilename1);
        // Get results from getLatest command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getLatest' operation with restart option set
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetLatestRestart() throws Exception {
        this._client.set("restart", true);
        // Async call to get the latest file in a file type, restart
        // option is enabled
        int xactId = this._client.getLatest(this._testFilename1);
        // Get results from getLatest command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Test case on client 'getLatest' operation with restart and
     * computeChecksum options set
     * 
     * @throws Exception
     *             when general failure
     */
    public void testGetLatestRestartChecksum() throws Exception {
        this._client.set("restart", true);
        this._client.set("computechecksum", true);
        // Async call to get the latest file in a file type, restart
        // and computeChecksum options are enabled
        int xactId = this._client.getLatest(this._testFilename1);
        // Get results from getLatest command
        while (this._client.getTransactionCount() > 0) {
            Result r = this._client.getResult();
            if (r == null) {
                continue;
            }
            assertEquals(Constants.OK, r.getErrno());
        }
    }

    /**
     * Utility method to delete a file.
     * 
     * @param name
     *            filename to be deleted
     * @throws Exception
     *             when general failure
     */
    private void removeFile(String name) throws Exception {
        File f = new File(name);
        if (!f.exists() || f.isDirectory()) {
            System.out.println("File " + name + " not found");
            return;
        } else if (f.isDirectory()) {
            System.out.println("Cannot delete, " + name + " is a directory");
            return;
        } else {
            f.delete();
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
                + "   -Dfei.domain.file=<domain file> \\\n"
                + "   -Dfei.trust.store=<keystore> \\\n"
                + "   -Dfei.vft.server.link=<server-side symbolic link> \\\n"
                + "   -Dfei.test.file.1=<test file1> \\\n"
                + "   -Dfei.test.file.2=<test file2> \\\n"
                + "   -Dfei.test.file.3=<test file3> \\\n"
                + "   -Dfei.test.file.4=<test file4> \\\n"
                + "   -Doutput.dir=<output directory> \\\n" + "   "
                + GetCmdsTest.class.getName() + " <gui|text>";
        if (args.length != 1) {
            System.out.println(usage);
            return;
        }
        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(GetCmdsTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(GetCmdsTest.class);
        } else
            System.out.println(usage);
    }
}