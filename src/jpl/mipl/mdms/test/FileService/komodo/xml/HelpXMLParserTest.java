/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.FileService.komodo.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.client.HelpXMLParser;
import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * JUnit test case for Komodo Help XML Parser/Validator.
 *
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: HelpXMLParserTest.java,v 1.5 2003/10/07 17:57:30 rap Exp $
 */
public class HelpXMLParserTest extends TestCase {
    private Properties _help = null;
    private HelpXMLParser _xmlParser;

    /**
     * Constructor
     *
     * @param name the test suite name
     */
    public HelpXMLParserTest(String name) {
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
        String xml = props.getProperty("help.xml.file");
        String schema = props.getProperty("help.xsd.file");
        if (schema != null) {
            URL schemaURL = new File(schema).toURL();
            this._xmlParser = new HelpXMLParser(schemaURL.openStream());

        } else {
            this._xmlParser = new HelpXMLParser();
        }

        this._help = this._xmlParser.getProperties(xml);
    }

    /**
     * Override the TestCase trerDown method to destroy test environment.
     * Beware!  This is called between test cases.
     *
     * @throws Exception when general failure
     */
    public void tearDown() throws Exception {
        this._xmlParser = null;
        this._help = null;
    }

    /**
     * Test case on xml schema validation.  Checks to see if XML document is valid.
     *
     * @throws Exception when general failure
     */
    public void testParseHelp() throws Exception {
        assertTrue(this._xmlParser.isValid());
    }

    /**
     * Test case for command help verification.
     *
     * @throws Exception when general failure
     */
    public void testCmdHelp() throws Exception {
        Properties info;

        // Test delete command help
        info = (Properties) this._help.get("delete");
        assertEquals(
            "Delete registered file(s)",
            info.getProperty("description"));

        // Test use command help
        info = (Properties) this._help.get("use");
        assertEquals("Change file type", info.getProperty("description"));

        // Test use command help
        info = (Properties) this._help.get("add");
        assertEquals(
            "Add files matching file name expression to current file type",
            info.getProperty("description"));

        // Test show command help
        info = (Properties) this._help.get("show");
        assertEquals(
            "Show registered files in current file type",
            info.getProperty("description"));

        // Test get command help
        info = (Properties) this._help.get("get");
        assertEquals(
            "Get one or more files from current file type.  If external system process "
                + "invocation is specified, then it executes the system command for each "
                + "file received.",
            info.getProperty("description"));
    }

    /**
     * The main method to launch the JUnit TestRunner
     *
     * @param args gui|text
     */
    public static void main(String[] args) {
        String usage =
            "Usage: java [-classpath ...] \\\n"
                + "   -Dhelp.xml.file=<xml.file> \\\n"
                + "   -Dhelp.xml.schema=<xsd.file> \\\n"
                + "   "
                + HelpXMLParserTest.class.getName()
                + " <gui|text>";

        if (args.length != 1) {
            System.out.println(usage);
            return;
        }

        if (args[0].compareToIgnoreCase("gui") == 0) {
            junit.swingui.TestRunner.run(HelpXMLParserTest.class);
        } else if (args[0].compareToIgnoreCase("text") == 0) {
            junit.textui.TestRunner.run(HelpXMLParserTest.class);
        } else
            System.out.println(usage);
    }
}
