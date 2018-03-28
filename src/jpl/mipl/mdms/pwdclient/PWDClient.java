package jpl.mipl.mdms.pwdclient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import jpl.mipl.mdms.utils.GetOpt;
import jpl.mipl.mdms.utils.logging.Logger;
import jpl.mipl.mdms.utils.logging.LoggerPlugin;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/**
 * Pure Java implementation of MDMS Password Server BaseClient.
 * <p>
 * MDMS Password Server BaseClient interfaces with Password Server to request
 * and get a password securely. The Password Server client is written using Java
 * GSSAPI (Generic Security Services) and JAAS (Java Authentication and
 * Authorization Service). More information on GSSAPI and JAAS is available at
 * <a href="http://java.sun.com/products/jaas/">
 * http://java.sun.com/products/jaas/ </a>.
 * <p>
 * Usage:
 * <p>
 * <code>// import statement</code><br>
 * <code>import jpl.mipl.mdms.pwdclient.PWDClient;</code>
 * <p>
 * <code>// username is defined as the MIPL Kerberos username (i.e. rap)</code>
 * <br>
 * <code>// servername is defined as the MIPL Database servername (i.e. miplDev)</code>
 * <br>
 * <code>PWDClient p = new PWDClient("username", "servername");</code><br>
 * <code>try {<br>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;String password = p.getPassword();</code><br>
 * <code>} catch (Exception e) {</code><br>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;System.err.println(e.getMessage);</code><br>
 * <code>}
 * <p>
 * Environment Variables:
 * <p>
 * To use the above code fragment, you must set the following
 * environmental variables:<br>
 * <i>NOTE: The MIPL select system will set these variables for you.</i>
 * <ul><li>PWDSERVER - This variable points to the directory that contains the
 * pwdinterface and pwdclient.conf MDMS PWDServer configuration files.
 * <li>KRB5_CONFIG - This variable points to the krb5.conf Kerberos
 * configuration file.</ul>
 * <p>
 * @author N. Toole, {Nicholas.Toole@jpl.nasa.gov}
 * @version $Id: PWDClient.java,v 1.33 2006/07/25 00:14:50 ntt Exp $
 */

public class PWDClient
{

    //---------------------------------------------------------------------

    public static final String PWD_INTERFACE_FILENAME = "pwdinterface";
    public static final String PWD_CLIENT_FILENAME    = "pwdclient.conf";

    public static final String SERVER_NAME   = "SERVERNAME";
    public static final String SERVER_REALM  = "SERVERREALM";
    public static final String SERVER_HOST   = "SERVERHOST";
    public static final String SERVER_PORT   = "SERVERPORT";
    public static final String COMMENT_START = "#";

    public static final String DEF_KRB5_CONFIG    = "/etc/krb5.conf";
    public static final String KRB5_CONFIG_ENV    = "KRB5_CONFIG";
    public static final String KRB5_OID           = "1.2.840.113554.1.2.2";
    public static final String KRB5_PRINCIPAL_OID = "1.2.840.113554.1.2.2.1";
    public static final String PWD_SERVER_ENV     = "PWDSERVER";
    public static final char   PWD_REQUEST_TOKEN  = 'p';
    public static final int    PWDPACKETLEN       = 1024;
    public static final String CHARACTER_SET      = "US-ASCII";

    //patterns for expressions
    protected final String PATTERN_REALM = 
                "^\\s*([\\w\\-\\_\\.]+)\\s+([=]+)\\s+([{]+)\\s*$";
    protected final String PATTERN_KDC   = 
                "^\\s*([kdc]+)\\s+([=]+)\\s+([\\w\\-\\_\\.]+[:]+[\\d]+)\\s*$";
    protected final String PATTERN_END   = 
                "^\\s*([}]+)\\s*$";


    private byte[] _reqPkt = new byte[PWDPACKETLEN];
    private Properties _props = System.getProperties();
    
    private String _dbUsrName;
    private String _dbSrvName;
    private String _dbPwd;
    private String _krbRealm;
    private String _krbPrincipal = null;
    private String _krbPwd;
    private String _pwdSrvName;
    private String _pwdSrvHost;
    private int    _pwdSrvPort;
    private String _confPath;
    private String _krbKdc;
    
    private int _bufsize = 0;

    // Regular expressions for parsing Kerberos krb5.conf file. We only want
    // the hostname for the KDC of the realm we are interested in.
    private Matcher _krbRealmExpr; // Match Kerberos realm.
    private Matcher _krbKdcExpr; // Match Kerberos KDC.
    private Matcher _krbEndDefExpr; // Match end of Kerberos realm definition.

    /** Logger instance */
    private final Logger _logger;
    protected final String LOG_CONFIG_PROP = LoggerPlugin.LOG_CONFIG_PROPERTY;
    protected final String PLUGIN_CLASS_PROP = 
                                "jpl.mipl.mdms.utils.logging.pluginClass";
    protected final String PLUGIN_CLASS_DEFAULT = 
                                "jpl.mipl.mdms.utils.logging.NullPlugin";

    //---------------------------------------------------------------------

    /**
     * Two argument constructer for PWDClient class. This constructor expects
     * the following system environment variables to be set: PWDSERVER which
     * points to the directory that contains the pwdinterface and pwdclient.conf
     * files, KRB5_CONFIG which point to the krb5.conf file. The MIPL select
     * system will set these variables. This constructor takes two String
     * arguments: 1) A MIPL Kerberos username, 2) The MIPL Database servername
     * (i.e. miplDev or MIPS1). Note: environment variables are read in the
     * getPassword() method
     * @param username A MIPL username (i.e. Kerberos account username)
     * @param server The name of the MDMS database server you're connecting to
     */

    public PWDClient(String username, String server)
    {
        setUser(username);
        setServer(server);
        this._krbPwd = "";
        this._confPath = "";

        //create logger.  Either (1) it was externally specified or 
        // (2) a default is used.
        String value = System.getProperty(LOG_CONFIG_PROP);
        if (value == null)
            System.setProperty(PLUGIN_CLASS_PROP, PLUGIN_CLASS_DEFAULT);
        this._logger = Logger.getLogger(this.getClass().getName());
    }

    //---------------------------------------------------------------------

    /**
     * Nine argument constructer for PWDClient class. This constructor is
     * provided for maximum portability and does not require environment
     * @param usrName A MIPL username (i.e. Kerberos account username)
     * @param dbSrvName Name of the MDMS database server you're connecting to
     * variables to be set.
     * @param pwdSrvName The name of the MDMS Password Server (ie. DEVPwdSrv)
     * @param pwdSrvHost The hostname of the MDMS Password Server
     * @param pwdSrvPort The port in which the MDMS Password Server is listening
     * @param krbRealm The Kerberos realm of the MDMS Password Server
     * @param krbKdc The KDC hostname for the Kerberos realm
     * @param krbPwd The password for Kerberos authentication
     * @param confFile The location of the PWDClient configuration file
     */

    public PWDClient(String usrName, String dbSrvName, String pwdSrvName,
                     String pwdSrvHost, int pwdSrvPort, String krbRealm, 
                     String krbKdc, String krbPwd, String confFile)
    {
        this(usrName, dbSrvName);
        configure(pwdSrvName, pwdSrvHost, pwdSrvPort, krbRealm, 
                  krbKdc, krbPwd, confFile);
    }

    //---------------------------------------------------------------------

    /**
     * Private method to get and parse the contents of the krb5.conf Kerberos
     * configuration file. Looks for the kdc value in the krb5.conf file pointed
     * at by the realm parameter. If KRB5_CONFIG env. variable is not set,
     * defaults to /etc/krb5.conf
     * @param realm The Kerberos realm of the MDMS PWDServer
     * @throws PatternSyntaxException when errors are encountered parsing and
     *             matching lines to defined regular expression patterns
     * @throws IOException when I/O failure
     */

    private void _parseKRB5Conf(String realm) throws PatternSyntaxException,
                                                     IOException
    {
        BufferedReader df = null;

        // If KRB5_CONFIG is not set, point to /etc/krb5.conf
        String env = this._getEnvVariable(KRB5_CONFIG_ENV);
        String path = (env == null) ? DEF_KRB5_CONFIG : env.substring(
                                   env.indexOf('=') + 1, env.length());

        _logger.debug("In parseKRB5Conf() method");
        _logger.debug("Kerberos config is: " + path);

        try {
            this._krbRealmExpr = Pattern.compile(PATTERN_REALM).matcher("");
            this._krbKdcExpr = Pattern.compile(PATTERN_KDC).matcher("");
            this._krbEndDefExpr = Pattern.compile(PATTERN_END).matcher("");

            df = this._createReader(path);
            String linebuf = null;

            // Read line by line to get Kerberos KDC information
            while ((linebuf = df.readLine()) != null)
            {
                this._krbRealmExpr.reset(linebuf);
                if (this._krbRealmExpr.find())
                {
                    if (!realm.equalsIgnoreCase(this._krbRealmExpr.group(1)))
                        continue;

                    // Read line by line until _krbEndDefExpr is encountered
                    while ((linebuf = df.readLine()) != null)
                    {
                        this._krbKdcExpr.reset(linebuf);
                        this._krbEndDefExpr.reset(linebuf);
                        if (this._krbKdcExpr.find())
                        {
                            String kdc = this._krbKdcExpr.group(3);
                            kdc = kdc.substring(0, kdc.indexOf(':'));
                            setKDC(kdc);
                            _logger.debug("Kerberos kdc: " + this._krbKdc);
                        }
                        else if (this._krbEndDefExpr.find())
                        {
                            break;
                        }
                    }
                }
            }
        } catch (PatternSyntaxException pse) {
            throw pse;
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (df != null)
            {
                try {
                    df.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //---------------------------------------------------------------------

    /**
     * Private method to get and parse the contents of the pwdinterface MDMS
     * PWDServer configuration file. Looks for the SERVERNAME, SERVERREALM,
     * SERVERHOST and SERVERPORT entiried in the pwdinterface file. The values
     * of those entries are used to set the name, realm, host and port of the
     * MDMS PWDServer that this client connects to get the password.
     * @throws PatternSyntaxException when pattern matching failure
     * @throws IOException when I/O failure
     * @throws Exception when PWDSERVER env. variable is not set
     */

    private void _parsePWDInterface() throws PatternSyntaxException,
                                             IOException, Exception
    {
        String env = null, content = null, path = null, linebuf = null;

        Properties props;
        InputStream is;

        env = this._getEnvVariable(PWD_SERVER_ENV);
        if (env == null)
            throw new Exception(PWD_SERVER_ENV + " environment variable "
                                + "is not set!");

        path = env.substring(env.indexOf('=') + 1, env.length()).trim();
        if (!path.endsWith(File.separator))
            path = path + File.separator;

        // Set the path to where pwdinterface and pwdclient.conf files are kept
        this._confPath = path;

        _logger.debug("In parsePWDInterface() method");
        _logger.debug("pwdInterface: " + path + PWD_INTERFACE_FILENAME);

        //load interface file to properties object
        is = _createStream(path + PWD_INTERFACE_FILENAME);
        props = new Properties();
        props.load(is);

        //set configuration from values of keys
        int index;
        String val;
        String[] keys = {SERVER_NAME, SERVER_REALM, SERVER_HOST, SERVER_PORT};
        String[] vals = new String[keys.length];

        for (int i = 0; i < keys.length; ++i)
        {
            val = props.getProperty(keys[i]);
            if (val == null)
                continue;
            if ((index = val.indexOf(COMMENT_START)) > 0)
                val = val.substring(0, index).trim();
            vals[i] = val;
        }

        setPWDServer(vals[0]);
        setRealm(vals[1]);
        setHost(vals[2]);
        try {
            setPort(Integer.parseInt(vals[3]));
        } catch (NumberFormatException nfEx) {
            _logger.error("Could not parse port number from "
                    + PWD_INTERFACE_FILENAME + ": " + vals[3]);
        }

        _logger.debug("In parsePWDInterface(): Server = " + getPWDServer());
        _logger.debug("In parsePWDInterface(): Host   = " + getHost());
        _logger.debug("In parsePWDInterface(): Realm  = " + getRealm());
        _logger.debug("In parsePWDInterface(): Port   = " + getPort());
    }

    //---------------------------------------------------------------------

    /*
     * private void _parsePWDInterface2() throws PatternSyntaxException,
     * IOException, Exception { String env = null, content = null, path = null,
     * linebuf = null; BufferedReader df = null; env =
     * this._getEnvVariable("PWDSERVER"); if (env == null) throw new
     * Exception("PWDSERVER environment variable is not set!"); path =
     * env.substring(env.indexOf('=') + 1, env.length()).trim(); if
     * (!path.endsWith(File.separator)) path = path + File.separator;
     *  // Set the path to where pwdinterface and pwdclient.conf files are kept
     * this._confPath = path;
     * 
     * MDMS.ERROR("In parsePWDInterface() method"); MDMS.ERROR("pwdInterface: " +
     * path + "pwdinterface"); try { this._srvNameExpr = Pattern .compile(
     * "^\\s*([SERVERNAME]+)\\s+([=]+)\\s+([\\w\\-\\_]+)\\s*+([#.*])")
     * .matcher(""); this._srvRealmExpr = Pattern .compile(
     * "^\\s*([SERVERREALM]+)\\s+([=]+)\\s+([\\w\\-\\_\\.]+)\\s*+([#.*])")
     * .matcher(""); this._srvHostExpr = P1382attern .compile(
     * "^\\s*([SERVERHOST]+)\\s+([=]+)\\s+([\\w\\-\\_\\.]+)\\s*+([#.*])")
     * .matcher(""); this._srvPortExpr = Pattern.compile(
     * "^\\s*([SERVERPORT]+)\\s+([=]+)\\s+(\\d+)\\s*+([#.*])") .matcher("");
     * this._srvFkPortExpr = Pattern.compile(
     * "^\\s*([FKSERVERPORT]+)\\s+([=]+)\\s+(\\d+)\\s*+([#.*])") .matcher("");
     * this._commentExpr = Pattern.compile("^\\s*#.*").matcher("");
     * this._emptyLineExpr = Pattern.compile("^\\s*$").matcher(""); df =
     * this._createReader(path + "pwdinterface"); // Read line by line to get
     * password server connection information while ((linebuf = df.readLine()) !=
     * null) { this._srvNameExpr.reset(linebuf);
     * this._srvRealmExpr.reset(linebuf); this._srvHostExpr.reset(linebuf);
     * this._srvPortExpr.reset(linebuf); this._srvFkPortExpr.reset(linebuf);
     * this._commentExpr.reset(linebuf); this._emptyLineExpr.reset(linebuf); if
     * (this._srvNameExpr.find()) { this._pwdSrvName =
     * this._srvNameExpr.group(3); MDMS.ERROR("PwdSrvName: " +
     * this._pwdSrvName); } else if (this._srvRealmExpr.find()) { this._krbRealm =
     * this._srvRealmExpr.group(3); MDMS.ERROR("krbRealm: " + this._krbRealm); }
     * else if (this._srvHostExpr.find()) { this._pwdSrvHost =
     * this._srvHostExpr.group(3); MDMS.ERROR("PwdSrvHost: " +
     * this._pwdSrvHost); } else if (this._srvPortExpr.find()) {
     * this._pwdSrvPort = (new Integer(this._srvPortExpr.group(3))) .intValue();
     * MDMS.ERROR("PwdSrvPort: " + this._pwdSrvPort); } else if
     * (this._commentExpr.find() == false && this._emptyLineExpr.find() == false &&
     * this._srvFkPortExpr.find() == false) { throw new Exception("Mal-formed
     * expression: \"" + linebuf + "\"."); } } } catch (PatternSyntaxException
     * pse) { throw pse; } catch (IOException ioe) { throw ioe; } finally { if
     * (df != null) { try { df.close(); } catch (IOException e) {
     * e.printStackTrace(); } } } }
     */

    //---------------------------------------------------------------------
    /**
     * Read a file from either local disc or a jar file
     * @param fileName the file name
     * @return BufferedReader object
     * @throws java.io.IOException when I/O failure
     */

    private BufferedReader _createReader(String fileName) throws IOException
    {
        InputStream inStream = _createStream(fileName);
        return new BufferedReader(new InputStreamReader(inStream));
    }

    //---------------------------------------------------------------------

    /**
     * Read a file from either local disc or a jar file
     * @param fileName the file name
     * @return InputStream object
     * @throws java.io.IOException when I/O failure
     */

    private InputStream _createStream(String fileName) throws IOException
    {
        InputStream inStream = null;

        //try reading from disc
        if ((new File(fileName)).exists())
            inStream = new FileInputStream(fileName);
        else //Reading from a jar file
            inStream = this.getClass().getResourceAsStream(fileName);

        if (inStream == null)
            throw new IOException("File '" + fileName + "' not found");

        return inStream;
    }

    //---------------------------------------------------------------------

    /**
     * Private method to get the environmental variable specified by the key
     * parameter. Returns the variable if it is set, null if it is not.
     * @param key the string value of the environmental variable that is
     *            searched for.
     * @return the value of the env variable
     * @throws IOException when I/O failure
     */

    private String _getEnvVariable(String key) throws IOException
    {
        String env   = null;
        String line  = new String();
        Runtime run  = Runtime.getRuntime();
        Process proc = run.exec("env");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                                               proc.getInputStream()));
        while ((line = in.readLine()) != null)
        {
            if (line.startsWith(key))
                env = line;
        }
        return env;
    }

    //---------------------------------------------------------------------

    /**
     * Private internal method to construct the JAAS authentication framework
     * and retieve the Kerberos ticket from the ticket cache or requests a new
     * ticket. Builds a request packet for the MDMS Password Server if
     * successfull. Culls credentials from the ticket and calls a privileged
     * method to transmit the packet to the password server.
     * @throws PrivilegedActionException when credentials are invalid
     * @throws LoginException when authentication framework is incomplete or
     *             password and/or username are incorrect
     * @throws Exception when ticket is expired or if ticket is for the wrong
     *             realm
     */

    private void _authenticate() throws PrivilegedActionException,
            LoginException, Exception
    {
        //Basic class framework used to authenticate subjects
        LoginContext lc = null;

        _logger.debug("*** DEBUG: _authenticate: Creating login "
                + "context with username " + this._dbUsrName 
                + " and password " + this._krbPwd);

        try {
            //Will catch exceptions generated by incorrect logins
            //Create a new LoginContext using PWDCallbackHandler class
            lc = new LoginContext(this.getClass().getName(),
                                  new PWDCallbackHandler(this._dbUsrName, 
                                                         this._krbPwd));
            lc.login();
            Iterator credIterator = lc.getSubject().getPrivateCredentials()
                                                                .iterator();
            while (credIterator.hasNext())
            {
                KerberosTicket tkt = (KerberosTicket) credIterator.next();
                if (!tkt.isCurrent())
                {
                    throw new Exception("Kerberos TGT has expired");
                }
                String tktRealm = tkt.getServer().getRealm();

                _logger.debug("*** DEBUG: _authenticate: Ticket realm = "
                               + tktRealm);
                if (!tktRealm.equals(this._krbRealm))
                {
                    throw new Exception("Realm mismatch between Kerberos "
                                        + "TGT and PWDServer");
                }
                String principal = tkt.getClient().toString();

                _logger.debug("*** DEBUG: _authenticate: Principal = "
                               + principal);
                this._krbPrincipal = principal.substring(0, 
                                              principal.indexOf('@'));
                _logger.debug("*** DEBUG: _authenticate: Principal (prefix) = "
                              + _krbPrincipal);
            }

            if (this._krbPrincipal == null)
            {
                throw new Exception("Unable to retrieve Kerberos principal"
                                    + " for user "+this._dbUsrName);
            }

            // build the request packet to send to MDMS Password Server.
            this._buildPacket(PWD_REQUEST_TOKEN, this._dbUsrName,
                              this._dbSrvName, this._krbPrincipal);
            Subject subject = lc.getSubject();
            Subject.doAsPrivileged(subject, new GSSAuthentication(), null);

        } catch (PrivilegedActionException pae) {
            throw pae;
        } catch (LoginException le) {
            throw le;
        }
    }

    //---------------------------------------------------------------------

    /**
     * Checks to see if parameters for authentication have been configured.
     * @return True if parameters are configured for authentication, false
     *         otherwise
     */

    private boolean _isConfigured()
    {
        if (this._confPath == null || this._confPath.equals(""))
            return false;
        else if (this._krbRealm == null || this._krbRealm.equals(""))
            return false;
        else if (this._krbKdc == null || this._krbKdc.equals(""))
            return false;
        else if (this._pwdSrvName == null || this._pwdSrvName.equals(""))
            return false;
        else if (this._pwdSrvHost == null || this._pwdSrvHost.equals(""))
            return false;
        else if (this._pwdSrvPort == 0)
            return false;
        else if (this._dbUsrName == null || this._dbUsrName.equals(""))
            return false;
        else if (this._dbSrvName == null || this._dbSrvName.equals(""))
            return false;
        else
            return true;
    }

    //---------------------------------------------------------------------

    /**
     * Configuration of parameters required for authentication and communication
     * with MDMS Password Server. Use this method if the two argument
     * constructor was used but PWDSERVER and KRB5_CONFIG env. variables are not
     * set.
     * @param pwdSrvName The name of the MDMS Password Server
     * @param pwdSrvHost The hostname where the password server is running
     * @param pwdSrvPort The port the password server is listening to
     * @param krbRealm The Kerberos realm of the password server
     * @param krbKdc The KDC hostname for the Kerberos realm
     * @param krbPwd The password for Kerberos authentication
     * @param confFile The location of the PWDClient configuration file
     */

    public void configure(String pwdSrvName, String pwdSrvHost, int pwdSrvPort,
            String krbRealm, String krbKdc, String krbPwd, String confFile)
    {
        setPWDServer(pwdSrvName);
        setHost(pwdSrvHost);
        setPort(pwdSrvPort);
        setRealm(krbRealm);
        setKDC(krbKdc);
        this._krbPwd = krbPwd;
        this._confPath = confFile;
    }

    //---------------------------------------------------------------------

    /**
     * Gets the password from the MDMS Password Server. This method wll request
     * a Kerberos ticket or read from a Kerberos cache file. If valid
     * credentials are obtained, the password will be requested from the
     * password server.
     * 
     * If the two argument constructor is used, an exception will be thrown
     * when:
     * <ul>
     * <li>A Kerberos cache file is not found
     * <li>A Kerberos cache file is found, but Kerberos TGT is expired
     * <li>The PWDSERVER environment variable is not set
     * <li>The KRB5_CONFIG environment variable is set to an incorrect
     * configuration file (i.e. pointing to the wrong realm)
     * <li>The MDMS PWDServer is down
     * <li>Password Server doesn't contain an entry for supplied username and
     * servername
     * <li>Realm mismatch between Kerberos TGT and PWDServer
     * </ul>
     * 
     * If the nine argument constructor is used, an exception will be thrown
     * when:
     * <ul>
     * <li>A Kerberos ticket cannot be obtained (i.e. wrong password and/or
     * username)
     * <li>PWDClient configuration file is missing and/or incorrect
     * <li>The MDMS PWDServer is down
     * <li>Password Server doesn't contain an entry for supplied username and
     * servername
     * <li>Realm mismatch between Kerberos TGT and PWDServer
     * </ul>
     * 
     * @return string containing the password
     * @throws PatternSyntaxException if parse error occurred
     * @throws IOException if I/O error occurred
     * @throws PrivilegedActionException if authorization error occurred
     * @throws LoginException if authentication error occurred
     * @throws GSSException if kerberos GSS error occurred
     * @throws Exception if other error occurred
     */

    public String getPassword() throws Exception
    {
        try {

            if (this._isConfigured())
            {
                this._props.put("java.security.auth.login.config",
                                this._confPath);
            }
            else
            {
                this._parsePWDInterface();
                this._parseKRB5Conf(this._krbRealm);
                this._props.put("java.security.auth.login.config",
                                this._confPath + PWD_CLIENT_FILENAME);
            }
            this._props.put("java.security.krb5.realm", this._krbRealm);
            this._props.put("java.security.krb5.kdc", this._krbKdc);
            this._authenticate();
        } catch (PatternSyntaxException pse) {
            //throw new Exception("Parse Error: " + pse.getMessage());
            throw pse;
        } catch (IOException ioe) {
            //throw new Exception("I/O Error: " + ioe.getMessage());
            throw ioe;
        } catch (PrivilegedActionException pae) {
            //throw new Exception("Authorization Error: " + pae.getMessage());
            throw pae;
        } catch (LoginException le) {
            //throw new Exception("Authentication Error: " + le.getMessage());
            throw le;
        } catch (GSSException gsse) {
            //throw new Exception("Kerberos GSS Error: " + gsse.getMessage());
            throw gsse;
        }

        if (this._dbPwd == null || this._dbPwd.equalsIgnoreCase(""))
        {
            throw new Exception("Password is null");
        }
        else if (this._dbPwd.equalsIgnoreCase("Instance not found."))
        {
            this._dbPwd = null;
            throw new Exception("Instance not found for principal "
                    + this._krbPrincipal + ", username " + this._dbUsrName
                    + ", servername " + this._dbSrvName + ".\n"
                    + "Did you specify the correct server?\n"
                    + "Are you in the correct select?\n"
                    + "Do you have an entry in the MDMS PWDServer?");
        }
        return this._dbPwd;
    }

    //---------------------------------------------------------------------

    /**
     * Accessor Method to get the Kerberos realm.
     * @return string containing the Kerberos realm
     */

    public String getRealm()
    {
        return this._krbRealm;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the Kerberos realm
     * @param realm Sets the Kerberos realm to parameter input
     */

    public void setRealm(String realm)
    {
        this._krbRealm = realm;
    }

    //---------------------------------------------------------------------

    /**
     * Accessor Method to get the KDC hostname for the Kerberos realm.
     * @return String containing the KDC hostname for the Kerberos realm.
     */

    public String getKDC()
    {
        return this._krbKdc;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the hostname of the Key Distribution Center
     * (KDC) for the Kerberos realm. The KDC is the central Kerberos service
     * that kinit interfaces with.
     * @param kdc Sets the hostname of the KDC for the Kerberos realm
     */

    public void setKDC(String kdc)
    {
        this._krbKdc = kdc;
    }

    //---------------------------------------------------------------------

    /**
     * Accessor Method to get the hostname of the MDMS PWDServer.
     * @return string containing the hostname of the PWDServer
     */

    public String getHost()
    {
        return this._pwdSrvHost;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the hostname of the MDMS PWDServer
     * @param host Sets the PWDServer host to parameter input
     */

    public void setHost(String host)
    {
        this._pwdSrvHost = host;
    }

    //---------------------------------------------------------------------

    /**
     * Accessor Method to get the port of the MDMS PWDServer.
     * @return int containing the port of the MDMS PWDServer
     */

    public int getPort()
    {
        return this._pwdSrvPort;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the port of the MDMS PWDServer
     * @param port Sets the PWDServer port to parameter input
     */

    public void setPort(int port)
    {
        this._pwdSrvPort = port;
    }

    //---------------------------------------------------------------------

    /**
     * Accessor Method to get the name of the MDMS PWDServer.
     * @return String containing the name of the MDMS PWDServer.
     */

    public String getPWDServer()
    {
        return this._pwdSrvName;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the name of the MDMS PWDServer
     * @param pwdServer Sets the name of the MDMS PWDServer
     */

    public void setPWDServer(String pwdServer)
    {
        this._pwdSrvName = pwdServer;
    }

    //---------------------------------------------------------------------

    /**
     * Accessor Method to get the username.
     * @return string containing the username
     */

    public String getUser()
    {
        return this._dbUsrName;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the username
     * @param username Sets the _dbUsrName to username input
     */

    public void setUser(String username)
    {
        this._dbUsrName = username;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the default Kerberos cachefile
     * @param cacheFile Sets the system property for Kerberos cachefile to
     *            cacheFile
     * @deprecated
     */

    public void setCacheFile(String cacheFile)
    {
        // Set the system property krb.cache so that the JRE knows where to
        // get the Kerberos ticket cache file.
        this._props.put("krb.cache", cacheFile);
    }

    //---------------------------------------------------------------------

    /**
     * Accessor method to get the server name of the MDMS database server.
     * @return string containing the server name of the database server.
     */

    public String getServer()
    {
        return this._dbSrvName;
    }

    //---------------------------------------------------------------------

    /**
     * Mutator method to change the server name of the MDMS database server.
     * @param server Sets the server to the parameter input
     */

    public void setServer(String server)
    {
        this._dbSrvName = server;
    }

    //---------------------------------------------------------------------

    /**
     * Accessor method to get the error message.
     * @return string containing the error message
     * @deprecated
     */

    public String getErrMsg()
    {
        return "Please get error message from thrown exception in getPassword()";
    }

    //---------------------------------------------------------------------

    /**
     * (Deprecated) Utility method to check if an error occurred when getting
     * the password. The getPassword() method will throw exceptions if errors
     * occur.
     * @return boolean value T or F depending upon if an error occurred.
     * @deprecated
     */

    public boolean isError()
    {
        return false;
    }

    //---------------------------------------------------------------------

    /**
     * Builds a request packet which will be sent the the MDMS Password Server.
     * MDMS Password Server Packet Format (CONFIDENTIAL)
     * 
     * The MDMS Password Server requires a packet of max length 1024 bytes. The
     * packet must be initialized to all null char's. The first four bytes of
     * the packet must be skipped, these bytes will hold the integer size of the
     * packet. The fifth byte will contain a token (char) that will tell the
     * password server which command the user wants to execute. The command for
     * a password request is a 'p'. The sixth to nth byte will contain the
     * username string where the username is n-bytes long. The n+1 byte will be
     * skipped so it is still set to a null char. The dbusername will be put
     * into to the packet next where the dbusername is n-bytes long. The n+1
     * byte will be skipped so it is still set to a null char. The dbservername
     * will be put into to the packet next where the dbservername is n-bytes
     * long. The n+1 byte will be skipped so it is still set to a null char. The
     * Kerberos principal name will be put into the packet next where the
     * principal name is n-bytes long. The n+1 byte will be skipped so it is
     * still set to a null char. A null character is put into the packet next to
     * hold the position of the password buffer. The current position of the
     * packet after the above data has been stored in it is saved as an int and
     * put into the first four bytes of the packet. This is important or the
     * password server won't know how long the packet is and it'll core dump
     * with a malloc error (very bad).
     * @param token Char token used to tell the password server which opertion
     *              to perform.
     * @param username The Kerberos username
     * @param server The MIPL Database server
     * @param principal Principal name of user
     * @throws IOException when I/O failure
     */

    private void _buildPacket(char token, String username, String server,
                              String principal) throws IOException
    {

        _logger.debug("_buildPacket: Token = "+token+"; user = "+username +
                      "; server = "+server+"; principal = "+principal);

        //using a buffer to build packet
        StringBuffer buffer = new StringBuffer();

        //server expects prefix of slash
        String principalWoSub = principal;
        int index = principalWoSub.indexOf('/');
        if (index > 0)
            principalWoSub = principal.substring(0, index);

        //allocate first four bytes for length
        buffer.append('\0').append('\0').append('\0').append('\0');

        //append token char
        buffer.append(token);

        //append principal without sub-principal part
        buffer.append(principalWoSub).append('\0');

        //append username
        buffer.append(username).append('\0');

        //append server name
        buffer.append(server).append('\0');

        //append principal without sub-principal part
        //Only works with first part of principal (5.16.05)
        buffer.append(principalWoSub).append('\0');

        //append null char for empty password
        buffer.append('\0');

        //get the length of the buffer, dont include the length (first 4 bytes)
        int bufLength = buffer.length() - 4;

        //fill in the rest with null chars
        for (index = bufLength + 4; index < PWDPACKETLEN; ++index)
            buffer.append('\0');

        //convert buffer to bytes
        byte[] bufferBytes = buffer.toString().getBytes(CHARACTER_SET);

        //set pkt to point to new byte array
        this._reqPkt = bufferBytes;

        //write length of buffer to first four bytes of byte array
        byte[] pktLenArray = intToByteArray(bufLength);
        for (index = 0; index < 4; ++index)
        {
            this._reqPkt[index] = pktLenArray[index];
        }

        //DEBUG_START
        /*
         File newFile = new File("newPacket.dat");
        _logger.info("*** DEBUG: Writing packet to "
                + newFile.getAbsolutePath() + ". Length = " + _reqPkt.length);
        _logger.debug("*** DEBUG: Buf size = " + bufLength);
        for (int i = 0; i < bufLength + 4; ++i)
            _logger.debug("*** DEBUG packet[" + i + "] = " + this._reqPkt[i]);
        FileOutputStream fos = new FileOutputStream(newFile);
        fos.write(this._reqPkt);
        fos.flush();
        fos.close();
        */
        //DEBUG_END

    }

    //---------------------------------------------------------------------

    /** 
     * Converts big-endian integer to byte array.
     * @param integer Interger value (4-bytes)
     * @return Byte array of length 4 created from parameter
     */
    
    protected static byte[] intToByteArray(final int integer)
    {
        byte[] byteArray = new byte[4];

        for (int i = 0; i < 4; ++i)
            byteArray[3 - i] = (byte) (integer >>> (i * 8));

        return byteArray;
    }

    //---------------------------------------------------------------------

    /** 
     * Converts byte-array to big-endian integer.
     * @param Byte array of length 4bytes 
     * @return Integer (4-bytes) created from byte-array parameter
     */
    
    protected static int byteArrayToInt(final byte[] bytes)
    {
        int integer = 0;
        
        if (bytes != null || bytes.length == 4)
        {
            for (int i = 0; i < 4; ++i)
                integer += (bytes[3 - i] & 0xFF) << (i * 8);
        }

        return integer;
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    /**
     * main method for testing
     * 
     * @param args Command line arguments.
     * @throws Exception general exception.
     */
    public static void main(String[] args) throws Exception
    {
        GetOpt getOpt = new GetOpt(args, "u:s:vh");
        String usage = "Usage: pwdclient -u <user name> -s <server name> " +
                "[-v -h]\n\nMDMS Password Server client utility queries and " +
                "decrypts the\npassword for the associated user. The -v " +
                "option displays verbose\noutput.  The -h option displays " +
                "this message.";
        String arg = null, user = null, server = null;
        boolean verbose = false;
        while ((arg = getOpt.nextArg()) != null)
        {
            switch (arg.charAt(0))
            {
            case 'h':
            case 'H':
                System.err.println(usage);
                System.exit(-1);
                break;
            case 'v':
            case 'V':
                verbose = true;
                break;
            case 'u':
            case 'U':
                user = getOpt.getArgValue();
                break;
            case 's':
            case 'S':
                server = getOpt.getArgValue();
                break;
            default:
                System.err.println("Invalid input argument: " + arg);
                System.exit(-1);
            }
        }
        if (user == null || server == null)
        {
            System.err.println(usage);
            System.exit(-1);
        }
        PWDClient pc = new PWDClient(user, server);
        if (verbose)
        {
            try {
                System.out.println("The password is: " + pc.getPassword());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            System.out.println("\nUsing MDMS PWDServer " + pc.getPWDServer());
            System.out.println(" on host " + pc.getHost() + ":" + pc.getPort());
            System.out.println("\nUsing Kerberos Realm " + pc.getRealm());
            System.out.println(" on KDC host " + pc.getKDC());
        }
        else
        {
            try {
                System.out.println(pc.getPassword());
            } catch (Exception e) {
                //e.printStackTrace();
                System.err.println(e.getMessage());
            }
        }
        System.exit(0);
    }

    //=====================================================================
    //=====================================================================

    /**
     * Utility class to do login Authentication and get a Kerberos TGT.
     */
    class PWDCallbackHandler implements CallbackHandler
    {
        private String _username = null;
        private char[] _password = null;

        //-----------------------------------------------------------------

        /**
         * Constructor
         * @param username the username
         * @param password the password
         */

        PWDCallbackHandler(String username, String password)
        {
            this._username = username;
            this._password = password.toCharArray();
        }

        //-----------------------------------------------------------------

        /**
         * Handle the callback
         * @param callbacks Array of callbacks
         * @throws IOException
         * @throws UnsupportedCallbackException when callback not supported
         */

        public void handle(Callback[] callbacks) throws IOException,
                                               UnsupportedCallbackException
        {
            for (int i = 0; i < callbacks.length; i++)
            {
                if (callbacks[i] instanceof NameCallback)
                {
                    // Set username of kerberos login
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(this._username);
                }
                else if (callbacks[i] instanceof PasswordCallback)
                {
                    // set password of kerberos login
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(this._password);
                }
                else
                {
                    throw new UnsupportedCallbackException(callbacks[i],
                                               "Unrecognized Callback");
                }
            }
        }

        //-----------------------------------------------------------------
    }

    //=====================================================================

    /**
     * Utility class to do Kerberos service ticket authentication
     */
    class GSSAuthentication implements PrivilegedExceptionAction
    {
        private static final int MAXSTRLEN = 256;
        private static final int DEFINBUFSIZE  = PWDClient.PWDPACKETLEN;
        private static final int DEFOUTBUFSIZE = PWDClient.PWDPACKETLEN;
        private byte[] _data;
        private Socket _socket;
        private DataInputStream _inStream;
        private DataOutputStream _outStream;
        private int _inBufSize;
        private int _outBufSize;
        private boolean _debug = false;

        /** Position of reply packet where reply token is found*/
        private static final int REPLY_CODE_INDEX = 4;

        /** Position of reply packet where password string begins */
        private static final int PWD_START_INDEX = 5;

        /** 
         * Abandons attempt at setting a secure context between
         * server and client if true.
         */
        private boolean _dumbHandshake = true;

        //-----------------------------------------------------------------

        /**
         * Constructor
         * @throws GSSException when GSS framework not enabled
         */

        public GSSAuthentication() throws GSSException
        {
            _logger.debug("In the GSSAuth constructor");
            this._inBufSize = GSSAuthentication.DEFINBUFSIZE;
            this._outBufSize = GSSAuthentication.DEFOUTBUFSIZE;
            this._data = new byte[GSSAuthentication.DEFINBUFSIZE];
        }

        //-----------------------------------------------------------------

        /**
         * Preform the GSS authentication
         * @return Boolean true when successfull
         * @throws Exception when general failure
         */

        public Object run() throws IOException, GSSException
        {
            int buffersize;
            long bytesToRead, bytesToWrite;
            _logger.debug("About to open Input/Output Streams...");
            
            //create the socket and get refernces to the I/O streams
            try {
                _logger.debug("*** DEBUG: run: New socket(" + _pwdSrvHost + 
                             "," + _pwdSrvPort + ")");
                this._socket = new Socket(_pwdSrvHost, _pwdSrvPort);
                this._inStream = new DataInputStream(
                                            this._socket.getInputStream());
                this._outStream = new DataOutputStream(
                                            this._socket.getOutputStream());
            } catch (Exception e) {
                throw new IOException("Unable to create and retrieve "+
                        "streams from socket.  Message: "+e.getMessage());
            }

            // create secure context 
            _logger.debug("Creating the GSS Context...");
            Oid krb5Oid = new Oid(KRB5_OID);
            Oid krb5PrincipalNameType = new Oid(KRB5_PRINCIPAL_OID);
            GSSManager manager = GSSManager.getInstance();
            GSSName serverName = manager.createName(_pwdSrvName + "/"
                                        + _pwdSrvHost + "@" + _krbRealm, 
                                        krb5PrincipalNameType);
            _logger.debug("*** DEBUG: run: Servername =" + serverName);

            //Identify who the client wishes to be
            GSSName userName = manager.createName(_krbPrincipal, null);
            GSSCredential userCreds = manager.createCredential(userName,
                    GSSCredential.DEFAULT_LIFETIME, krb5Oid,
                    GSSCredential.INITIATE_ONLY);
            GSSContext context = manager.createContext(
                                                  serverName, krb5Oid,
                                                  userCreds, 
                                                  GSSContext.DEFAULT_LIFETIME);
            context.requestMutualAuth(true); // Mutual authentication
            context.requestConf(true);       // Will use confidentiality later
            context.requestInteg(true);      // Will use integrity later

            /*
            //Commented: We do not do anything with the security token!
            byte[] token = new byte[0];
            _logger.debug("Initiating security context...");
            token = context.initSecContext(token, 0, token.length);
            */
          
            bytesToRead = this._inBufSize;

            if (_dumbHandshake)
            {
                try {
                    //We are not honoring the security context with the server.
                    //Instead, we are simply writing an empty byte and
                    //reading a byte response from GSS layer.
                    byte[] trashByte = new byte[1];
                    this._outStream.write(trashByte, 0, trashByte.length);
                    while (this._inStream.read(trashByte, 0, 1) == 0);
                } catch (IOException ioEx) {
                    _logger.error("I/O error in creating context with " +
                                  "password server.");
                    throw ioEx;
                }
            }

            try {
                // Write PWDServer protocol packet
                _logger.debug("Start sending the request packet...");
                this._outStream.write(_reqPkt, 0, _reqPkt.length);
                this._outStream.flush();

                /* 
                // Commented: Not using token...
                // Write GSS token
                _logger.debug("send GSS token of length " + token.length + " ");
                this._outStream.write(token);
                this._outStream.flush();
                */

                _logger.debug("Finished sending the request packet.");
            } catch (IOException ioe) {
                _logger.error("I/O error sending request to password server.");
                throw ioe; // rethrow the exception.
            }

            try {
                // Read Responce packet
                _logger.debug("Start reading the response packet...");

                int curIndex = 0;
                buffersize = this._inBufSize;
                int retVal; 
                while (curIndex < buffersize) 
                {
                    retVal = this._inStream.read(this._data, curIndex,
                                                 buffersize - curIndex);
                    if (retVal  < 1)
                        throw new IOException("Unexpected EOF from " + 
                                              "network peer.");
                    curIndex += retVal;
                }
                _logger.debug("Finished reading the responce packet.");
            } catch (IOException ioe) {  
                _logger.error("I/O error reading response from " + 
                              "password server.");
                throw ioe; // rethrow the exception.
            } finally {
                // clean up
                context.dispose();
                try {
                    this._outStream.close();
                    this._inStream.close();
                } catch (IOException e) {
                    throw e;
                }
            }

            // DEBUG: create byte array to store the password 
            byte[] pwd = new byte[this._inBufSize];
            for (int i = 0; i < pwd.length; i++)
                pwd[i] = 0;

            // skip the first five bytes of packet
            int index = PWD_START_INDEX;
            while (this._data[index] != (byte) 0)
            {
                // The responce packet contains multiple items of
                // information.  The password begins at byte location 
                // 5 and is followed by null characters.
                char c = (char) this._data[index];
                pwd[index - PWD_START_INDEX] = this._data[index];
                index++;
            }

            //DEBUG_START
            String debugStr = new String(pwd);
            byte[] intBytes = new byte[] {this._data[0], this._data[1],
                                          this._data[2], this._data[3]};
            int sizeOfData = byteArrayToInt(intBytes);
            _logger.debug(" *** DEBUG: Reply size = " + sizeOfData);
            _logger.debug(" *** DEBUG: Reply code = " + 
                                       (char) this._data[REPLY_CODE_INDEX]);
            _logger.debug(" *** DEBUG: String = " + debugStr);
            //DEBUG_END

            //subtract token and null char
            int expectedPwdSize = sizeOfData - 2;

            //convert the password in String format
            String passwd = new String(_data, PWD_START_INDEX, 
                                       expectedPwdSize, CHARACTER_SET);
            
            if (passwd.length() != expectedPwdSize)
            {
                _logger.error("Password length does not match reported size!" +
                              "  Expected: " + expectedPwdSize + ", Actual: " +
                              passwd.length() + ".  This might indicate a "   +
                              "protocol error.");
            }
            
            _logger.debug("Password String is: " + passwd +
                          " ("+ passwd.length() +" chars)");
            _dbPwd = passwd;
            
            //successful completion
            return Boolean.TRUE;
        }

        //-----------------------------------------------------------------
    }

    //=====================================================================
    //=====================================================================
}
