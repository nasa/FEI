/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.pwdclient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import jpl.mipl.mdms.pwdclient.PWDClient;
import junit.framework.TestCase;

/**
 * JUnit test case for the MDMS Password Server client.
 *
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: PWDClientTest.java,v 1.12 2003/09/04 23:13:04 rap Exp $
 */
public class PWDClientTest extends TestCase {
    private Properties _props = System.getProperties();
    private PWDClient _pc = null;
    private String _dbUsrName;
    private String _dbSrvName;
    private String _pwdSrvName;
    private String _pwdSrvHost;
    private int _pwdSrvPort;
    private String _krbRealm;
    private String _krbKdc;
    private String _krbPwd;
    private String _confFile;
    private String _jdbcUrl;
    private String _dbPwd;

    /**
     * Constructor
     *
     * @param name the test suite name
     */
    public PWDClientTest(String name) {
        super(name);
    }

    /**
     * Override the TestCase setUp method to initialize test environment.
     *
     * @throws Exception when general failure
     */
    public void setUp() throws Exception {
        this._dbUsrName = this._props.getProperty("pwd.dbusrname");
        this._dbSrvName = this._props.getProperty("pwd.dbsrvname");
        this._dbPwd = this._props.getProperty("pwd.dbpwd");
        this._pwdSrvName = this._props.getProperty("pwd.pwdsrvname");
        this._pwdSrvHost = this._props.getProperty("pwd.pwdsrvhost");
        this._pwdSrvPort =
            new Integer(this._props.getProperty("pwd.pwdsrvport")).intValue();
        this._krbRealm = this._props.getProperty("pwd.krbrealm");
        this._krbKdc = this._props.getProperty("pwd.krbkdc");
        this._krbPwd = this._props.getProperty("pwd.krbpwd");
        this._confFile = this._props.getProperty("pwd.conffile");
        this._jdbcUrl = this._props.getProperty("jdbc.url");
        this._pc =
            new PWDClient(
                this._dbUsrName,
                this._dbSrvName,
                this._pwdSrvName,
                this._pwdSrvHost,
                this._pwdSrvPort,
                this._krbRealm,
                this._krbKdc,
                this._krbPwd,
                this._confFile);
    }

    /**
     * Override parent tearDown method to cleanup after testing.
     */
    public void tearDown() {
        this._pc = null;
    }

    /**
     * Simple test of getPassword() method
     * 
     * @throws Exception when general failure
     */
    public void testGetPassword() throws Exception {
        assertEquals(this._dbPwd, this._pc.getPassword());
    }

    /**
     * Test of database connectivity with password
     * 
     * @throws Exception when general failure
     */
    public void testPasswordWithDb() throws Exception {
        Connection con = null;

        try {
            Class.forName("com.sybase.jdbc2.jdbc.SybDriver");
            con =
                DriverManager.getConnection(
                    this._jdbcUrl,
                    this._dbUsrName,
                    this._pc.getPassword());
            Statement stmt = con.createStatement();
            assertEquals(true, stmt.execute("select db_name()"));

        } catch (Exception e) {
            throw e;

        } finally {
            if (con != null)
                con.close();
        }
    }
}
