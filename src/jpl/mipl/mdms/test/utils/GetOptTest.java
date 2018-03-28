/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.utils;

import jpl.mipl.mdms.utils.GetOpt;
import jpl.mipl.mdms.utils.MDMS;
import junit.framework.TestCase;

/**
 * JUnit test case for the GetOpt and GetOptLong utility classes.
 *
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: GetOptTest.java,v 1.2 2003/09/09 22:55:02 rap Exp $
 */
public class GetOptTest extends TestCase {
    private GetOpt _getOpt = null;

    /**
     * Constructor
     *
     * @param name the test suite name
     */
    public GetOptTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     *
     * @throws Exception when general failure
     */
    public void setUp() throws Exception {
        String[] argv =
            new String[] {
                "-u",
                "tux",
                "-p",
                "v2.4",
                "-s",
                "dbServer",
                "-d",
                "mydb",
                "-h" };
        this._getOpt = new GetOpt(argv, "u:p:s:d:h");
    }

    /**
     * Override parent tearDown method to cleanup after testing.
     */
    public void tearDown() {
        this._getOpt = null;
    }

	/**
	 * Simple test of the GetOpt class
	 * 
	 * @throws Exception when general failure
	 */
    public void testGetOpt() throws Exception {
        String str;
        while ((str = this._getOpt.nextArg()) != null) {
            // it's so happened that all the test arguments begin with different
            // letter, so we can just check by that.  In general, we can also
            // use String.equals() method to do the comparison.
            switch (str.charAt(0)) {
                case 'h' :
                    break;
                case 'u' :
                    assertEquals("tux", this._getOpt.getArgValue());
                    break;
                case 'p' :
                    assertEquals("v2.4", this._getOpt.getArgValue());
                    break;
                case 's' :
                    assertEquals("dbServer", this._getOpt.getArgValue());
                    break;
                case 'd' :
                    assertEquals("mydb", this._getOpt.getArgValue());
                    break;
                default :
                    MDMS.ERROR("Invalue input argument");
            }
        }
    }
}
