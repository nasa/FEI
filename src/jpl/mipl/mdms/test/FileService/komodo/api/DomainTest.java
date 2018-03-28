/**
 *  @copyright Copyright 2004, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */
package jpl.mipl.mdms.test.FileService.komodo.api;

import java.io.File;
import java.util.LinkedList;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.api.Domain;
import jpl.mipl.mdms.FileService.komodo.api.DomainFactory;
import jpl.mipl.mdms.FileService.komodo.api.DomainFactoryIF;
import junit.framework.TestCase;

/**
 * JUnit test case for FEI Domain parsing
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: DomainTest.java,v 1.5 2017/06/05 16:48:29 awt Exp $
 */
public class DomainTest extends TestCase {
    private Properties _props = System.getProperties();

    private String _domainFile = null, _fileType = null, _serverGroup = null;

    private boolean _verbose = false;

    private Domain _domain = null;

    /**
     * Constructor
     * 
     * @param name the test suite name
     */
    public DomainTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     * 
     * @throws Exception when general failure
     */
    public void setUp() throws Exception {
        // Get the FEI username, password and fileType from system properties
        this._fileType = this._props.getProperty("fei.file.type");
        this._serverGroup = this._props.getProperty("fei.group.name");
        // Get resource files needed by Komodo (FEI5) client
        this._domainFile = this._props.getProperty("fei.domain.file");

        if (this._verbose) {
            System.out.println("FEI file type: " + this._fileType);
            System.out.println("FEI server group: " + this._serverGroup);
            System.out.println("FEI domain file: " + this._domainFile);
        }

        DomainFactoryIF factory = new DomainFactory();
        this._domain = factory.getDomain(new File(this._domainFile).toURL());
    }

    /**
     * Override parent tearDown method to cleanup after testing.
     * 
     * @throws Exception when general operation fail
     */
    public void tearDown() throws Exception {
        this._domain = null;
    }

    /**
     * Test get file type names
     * 
     * @throws Exception when general failure
     */
    public void testGetFileTypeNames() throws Exception {
        System.out.println("Testing getFileTypeNames()");
        LinkedList list = this._domain.getFileTypeNames();
        for (int i = 0; i< list.size(); ++i) {
            System.out.println(list.get(i).toString());
        }
    }

    /**
     * Test get file type names
     * 
     * @throws Exception when general failure
     */
    public void testGetFileTypeNamesGroup() throws Exception {
        System.out.println("Testing getFileTypeNames(" + this._serverGroup
                + ") method");
        LinkedList l = this._domain.getFileTypeNames(this._serverGroup);
        for (int i = 0; i < l.size(); i++) {
            System.out.println("i:"+l.get(i).toString());
        }
    }

    /**
     * Test get group names
     * 
     * @throws Exception when general failure
     */
    public void testGetGroupNames() throws Exception {
        System.out.println("Testing getGroupNames() method");
        LinkedList l = this._domain.getGroupNames();
        for (int i = 0; i < l.size(); i++) {
            System.out.println(l.get(i).toString());
        }
    }

    /**
     * Test toString() method
     * 
     * @throws Exception when general failure
     */
    public void testToString() throws Exception {
        System.out.println("Testing toString() method");
        System.out.println(this._domain.toString());
    }

    /**
     * Test get default group name
     * 
     * @throws Exception when general failure
     */
    public void testGetDefaultGroup() throws Exception {
        System.out.println("Testing getDefaultGroup() method");
        System.out.println(this._domain.getDefaultGroup());
    }

    /**
     * The main method to launch the JUnit TestRunner
     * 
     * @param args gui|text
     */
    public static void main(String[] args) {
        String usage = "Usage: java [-classpath ...] \\\n"
                + "   -Dfei.file.type=<filetype> \\\n"
                + "   -Dfei.server.group=<server group> \\\n"
                + "   -Dfei.domain.file=<domain file> \\\n" + "   "
                + DomainTest.class.getName() + " <gui|text>";
        if (args.length != 1) {
            System.out.println(usage);
            return;
        }
        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(DomainTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(DomainTest.class);
        } else
            System.out.println(usage);
    }
}