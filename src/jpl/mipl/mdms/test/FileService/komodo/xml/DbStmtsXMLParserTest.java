/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.FileService.komodo.xml;

import java.io.IOException;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.server.DbStmtsXMLParser;
import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * JUnit test case for Komodo XML Parser/Validator.
 *
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: DbStmtsXMLParserTest.java,v 1.5 2017/06/05 16:48:29 awt Exp $
 */
public class DbStmtsXMLParserTest extends TestCase {
    private Properties _stmts = null;
    private DbStmtsXMLParser _xmlParser;

    /**
     * Constructor
     *
     * @param name the test suite name
     */
    public DbStmtsXMLParserTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     *
     * @throws IOException when I/O failure
     * @throws SAXException when XML parsing failure
     */
    public void setUp() throws IOException, SAXException {
        Properties props = System.getProperties();
        String xml = props.getProperty("stmts.xml.file");
        String schema = props.getProperty("stmts.xsd.file");
        if (schema != null)
            this._xmlParser = new DbStmtsXMLParser(schema);
        else
            this._xmlParser = new DbStmtsXMLParser();

        this._stmts = this._xmlParser.getProperties(xml);
    }

    /**
     * Override the TestCase trerDown method to destroy test environment.
     * Beware!  This is called between test cases.
     *
     * @throws Exception when general failure
     */
    public void tearDown() throws Exception {
        this._xmlParser = null;
        this._stmts = null;
    }

    /**
     * Test case on xml schema validation.  Checks to see if XML document is valid.
     *
     * @throws Exception when general failure
     */
    public void testParseDbStmts() throws Exception {
        assertTrue(this._xmlParser.isValid());
    }

    /**
     * Test case for Sybase specific DBMS stmts.
     *
     * @throws Exception when general failure
     */
    public void testSybStmts() throws Exception {

        if (this._stmts != null) {
            if (this._stmts.getProperty("dbName").equalsIgnoreCase("Sybase")) {
                // Perform some Sybase specific assertion tests
                assertEquals(
                    "exec addVFTReader ''{0}'',''{1}''",
                    this._stmts.getProperty("db.stmt.addVFTReader").trim());
                assertEquals(
                    "exec releaseFileLock {0}",
                    this._stmts.getProperty("db.stmt.releaseFileLock").trim());
                assertEquals(
                    "exec getRefInfoAt ''{0}'',''{1}''",
                    this._stmts.getProperty("db.stmt.getRefInfoAt").trim());
                assertEquals(
                    "exec addFileType ''{0}'',''{1}'',''{2}'',{3},{4},{5},{6},''{7}'',''{8}'',''{9}'',{10}",
                    this._stmts.getProperty("db.stmt.addFileType").trim());
                assertEquals(
                    "exec listFilesForTypeSince {0},''{1}''",
                    this._stmts.getProperty("db.stmt.listFilesForTypeSince").trim());
                assertEquals(
                    "exec addUserRole ''{0}'',''{1}''",
                    this._stmts.getProperty("db.stmt.addUserRole").trim());
            }
        }
    }

    /**
     * The main method to launch the JUnit TestRunner
     *
     * @param args gui|text
     */
    public static void main(String[] args) {
        String usage =
            "Usage: java [-classpath ...] \\\n"
                + "   -Dstmts.xml.file=<xml.file> \\\n"
                + "   -Dstmts.xml.schema=<xsd.file> \\\n"
                + "   "
                + DbStmtsXMLParserTest.class.getName()
                + " <gui|text>";

        if (args.length != 1) {
            System.out.println(usage);
            return;
        }

        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(DbStmtsXMLParserTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(DbStmtsXMLParserTest.class);
        } else
            System.out.println(usage);
    }
}
