/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.FileService.util;

import java.util.Date;

import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import junit.framework.TestCase;

/**
 * A simple test program for the DateFormatUtil class.
 *
 * @author R. Pavlovsky {rich.pavlovsky@jpl.nasa.gov}
 * @version $Id: DateFormatTest.java,v 1.3 2003/09/09 22:55:02 rap Exp $
 */
public class DateFormatTest extends TestCase {
    private Date _d = null;
    private Date _gmt = null;

	/**
	 * Constructor
	 *
	 * @param name the test suite name
	 */
    public DateFormatTest(String name) {
        super(name);
    }

	/**
	 * Override the TestCase setUp method to initialize test environment.
	 *
	 * @throws Exception when general failure
	 */
    public void setUp() throws Exception {
        String date = "2001-11-23T12:12:1.000";
        this._d = DateTimeUtil.getCCSDSADate(date);
        this._gmt = DateTimeUtil.getCCSDSADateGMT(date);
    }
    
	/**
	 * Override parent tearDown method to cleanup after testing.
	 */
	public void tearDown() {
		this._d = null;
		this._gmt = null;
	}

    /**
     * Test the DateTimeUtil class
     * 
     * @throws Exception when general failure
     */
    public void testDateFormat() throws Exception {
        assertEquals("Fri Nov 23 12:12:01 PST 2001", this._d.toString());
        assertEquals("Fri Nov 23 04:12:01 PST 2001", this._gmt.toString());
    }
}
