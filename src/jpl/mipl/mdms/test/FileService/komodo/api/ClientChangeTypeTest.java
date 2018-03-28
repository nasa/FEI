/**
 * JUnit test case for Komodo (FEI5) Client API (changeType())
 */
package jpl.mipl.mdms.test.FileService.komodo.api;

import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import junit.framework.TestCase;

/**
 * JUnit test case for Komodo (FEI5) Client API (changeType())
 * <p>
 * Copyright 2004, California Institute of Technology. <br>
 * ALL RIGHTS RESERVED. <br>
 * U.S. Government Sponsorship acknowledge. 6/15/2004. <br>
 * MIPL Data Management System (MDMS).
 * <p>
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @author Nicholas Toole, {Nicholas.Toole@jpl.nasa.gov}
 * @version $Id: ClientChangeTypeTest.java,v 1.5 2017/06/05 16:48:29 awt Exp $
 */
public class ClientChangeTypeTest extends TestCase {
    private Properties _props = System.getProperties();

    private Client _client = null;

    private boolean _verbose = true;

    private String _fileType = null;

    private String _fileType2 = null;

    private String _serverGroup = null;

    private String _domainFile = null;

    private String _trustStore = null;

    private String _userName = null;

    private String _password = null;

    /**
     * Constructor
     * 
     * @param name
     *            the test suite name
     */
    public ClientChangeTypeTest(String name) {
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
        this._fileType2 = this._props.getProperty("fei.file.type2");
        this._serverGroup = this._props.getProperty("fei.group.name");
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

    /**
     * Test case on client changeType(). We change type from original to file
     * type 2, then back to original.
     * 
     * @throws Exception
     *             when general failure
     */
    public void testChangeType() {
        try {
            assertEquals(this._serverGroup+":"+this._fileType, this._client.getType());

            this._client.setType(this._serverGroup, this._fileType2);
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

            assertEquals(this._serverGroup+":"+this._fileType2, this._client.getType());

            //change back to original file type
            this._client.setType(this._serverGroup, this._fileType);
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
            assertEquals(this._serverGroup+":"+this._fileType, this._client.getType());
        } catch (SessionException se) {
            System.out.println("SessionException: " + se.getMessage());
        }
    }

    public void testNewLoginChangeType() {
        try {
            Client c = new Client(this._domainFile, this._trustStore);
            c.login(this._userName, this._password);
            assertNull(c.getType());

            c.setType(this._serverGroup, this._fileType);
            while (c.getTransactionCount() > 0) {
                Result r = c.getResult();
                if (r == null) {
                    continue;
                }
                if (r.getErrno() == Constants.OK) {
                    // Get the return code for each operation
                    assertEquals(Constants.OK, r.getErrno());
                }
            }
            assertEquals(this._serverGroup+":"+this._fileType, c.getType());

            c.setType(this._serverGroup, this._fileType2);
            while (c.getTransactionCount() > 0) {
                Result r = c.getResult();
                if (r == null) {
                    continue;
                }
                if (r.getErrno() == Constants.OK) {
                    // Get the return code for each operation
                    assertEquals(Constants.OK, r.getErrno());
                }
            }
            assertEquals(this._serverGroup+":"+this._fileType2, c.getType());
            c.logout();
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
                + "   -Dfei.file.type2=<file type2> \\\n"
                + "   -Dfei.group.name=<server group> \\\n"
                + "   -Dfei.domain.file=<domain file> \\\n"
                + "   -Dfei.trust.store=<keystore> \\\n"
                + ClientChangeTypeTest.class.getName() + " <gui|text>";
        if (args.length != 1) {
            System.out.println(usage);
            return;
        }
        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(ClientChangeTypeTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(ClientChangeTypeTest.class);
        } else
            System.out.println(usage);
    }
}