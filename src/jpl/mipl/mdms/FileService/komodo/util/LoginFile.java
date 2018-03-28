package jpl.mipl.mdms.FileService.komodo.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import jpl.mipl.mdms.FileService.io.BoundedBufferedReader;
import jpl.mipl.mdms.FileService.io.FileIO;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.client.CMD;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Login file that handles loading, committing, and deleting of 
 * komodo login files for usernames and passwords.
 * 
 *   <PRE>
 *   Copyright 2010, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2010.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 * 
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 04/14/2008        Nick             Initial Release
 * 01/19/2010        Nick             Restructured to include namespaces for
 *                                    login credentials.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: LoginFile.java,v 1.11 2013/10/14 17:25:37 ntt Exp $
 *
 */

public class LoginFile
{
    //private static final Object _syncLock = new Object();    
    
    public static final String DEFAULT_NAMESPACE = "*";
    
    public static final String PLAIN_NAMESPACE = "originalNamespace";
    
    protected LoginFileIO loginFileIO;
    protected File        loginFile;
    protected String      restartdir;   

    /** Data structure containing credentials */
    protected Hashtable<String, Properties> namespaceMap;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor, using default login file location
     * @throws IOExceptio if IO error occurs
     */
    
    public LoginFile() throws IOException
    {   
        this.init(null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor, using parameter as login file location
     * @param Location of login file
     * @throws IOExceptio if IO error occurs
     */
    
    public LoginFile(File loginFile) throws IOException
    {        
        this.init(loginFile);
    }

    //---------------------------------------------------------------------
    
    /**
     * Initializes this instance
     * @param loginFile File reference passed to constuctor, can be null
     * @throws IOExceptio if IO error occurs
     */
    
    protected void init(File loginFile) throws IOException
    {
        this.namespaceMap = new Hashtable<String, Properties>();
        this.loginFileIO  = new LoginFileIO();
        
        if (loginFile != null)
        {
            if (!loginFile.canRead())
                throw new FileNotFoundException("Cannot read file: "+
                                        loginFile.getAbsolutePath());
            else
            {
                this.loginFile = loginFile;
                this.loginFileIO.readFile(this);
                
            }
        }
        else
        {
            initDefault();
            if (this.loginFile.canRead())
            {
                this.loginFileIO.readFile(this);
            }
        }            
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Examines the restart directory property and uses it if set.  
     * Otherwise, assumes containing directory is user.home
     * concatenated by the restart subdir string.  
     */
    
    protected void initDefault() //throws IOException
    {        
        //set the restart directory
        if (this.restartdir == null)
        {   
            restartdir = System.getProperty(Constants.PROPERTY_RESTART_DIR);
            
            if (restartdir == null) 
            {
               restartdir = System.getProperty(Constants.PROPERTY_USER_HOME);
               if (restartdir == null)
                  restartdir = System.getProperty("user.home");
            }
            
            restartdir = restartdir + File.separator + Constants.RESTARTDIR;
        }

        String loginFilePath = restartdir + File.separator + Constants.LOGINFILE;
        this.loginFile = new File(loginFilePath);       
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get the username for this instance, using default namespace
     * @return Default username, or null if not found
     */
    
    public String getUsername()
    {
        return this.getUsername(DEFAULT_NAMESPACE, false);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get the username for this instance using namespace.  If namespace
     * not found, then the default (empty namespace) credential is returned.
     * @param namespace The username/password namespace
     * @return Username associated with namespace
     */
    
    public String getUsername(String namespace)
    {
        return getUsername(namespace, true);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get the username for this instance using namespace.  If namespace
     * not found, then the default (empty namespace) credential is returned
     * if useDefault parameter is true
     * @param namespace The username/password namespace
     * @param useDefault Use default login info if namespace not found
     * @return Username
     */
    
    public String getUsername(String namespace, boolean useDefault)
    {
        String username = null;
        
        
        if (namespace != null)
        {            
            String key = translateKey(namespace);            
                     
            Properties props = this.namespaceMap.get(key);
            if (props != null)
                username = props.getProperty(CMD.USER);
        }
        
        if (username == null && useDefault)
        {
            Properties props = this.namespaceMap.get(DEFAULT_NAMESPACE);
            if (props != null)
                username = props.getProperty(CMD.USER);
        }
        
        return username;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get the password for this instance, using default namespace
     * @return Default password
     */
    
    public String getPassword()
    {
        return this.getPassword(DEFAULT_NAMESPACE, false);
    }

    //---------------------------------------------------------------------
    
    /**
     * Get the password for this instance using namespace.  If namespace
     * not found, then the default (empty namespace) credential is returned.
     * @param namespace The username/password namespace
     * @return Password
     */
    
    public String getPassword(String namespace)
    {
        return this.getPassword(namespace, true);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get the expiration time for this instance using namespace.  
     * @param namespace The namespace
     * @return expiration time, -1 is not set
     */
    
    public long getExpiry(String namespace)
    {
        long expiry = Constants.NO_EXPIRATION;
        
        if (namespace != null)
        {
            String expirationStr = null;
            
            String key = translateKey(namespace);            
                         
            Properties props = this.namespaceMap.get(key);
            if (props != null)
                expirationStr = props.getProperty(CMD.EXPIRY);
            
            if (expirationStr != null)
            {
                try {
                    expiry = Long.parseLong(expirationStr);
                } catch (NumberFormatException nfEx) {
                    expiry = Constants.NO_EXPIRATION;
                }
            }
            
        }
        
        return expiry;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Set the username for this instance
     * @param user Username
     */
    
    public void setExpiry(String namespace, long expiration)
    {
        if (namespace == null)
            return;
        
        String expiryStr = expiration+"";
        setExpiry(namespace, expiryStr);
    
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Set the username for this instance
     * @param user Username
     */
    
    public void setExpiry(String namespace, String expiration)
    {
        if (namespace == null)
            return;
                
        if (expiration != null)
        {
            boolean isNumber = true;
            try {
                Long.parseLong(expiration);
            } catch (NumberFormatException nfEx) {
                isNumber = false;
            }
            
            if (!isNumber)
            {
                return;
            }
        }

        String key = translateKey(namespace);
        
        Properties props = this.namespaceMap.get(key);
        if (props == null)
        {
            props = new Properties();
            props.put(PLAIN_NAMESPACE, namespace);
                    
            this.namespaceMap.put(key, props);            
        }
        
        if (expiration != null)
            props.setProperty(CMD.EXPIRY, expiration);
        else
            props.remove(CMD.EXPIRY);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get the password for this instance using namespace.  If namespace
     * not found, then the default (empty namespace) credential is returned
     * if useDefault parameter is true
     * @param namespace The username/password namespace
     * @param useDefault Use default login info if namespace not found
     * @return Password
     */
    
    public String getPassword(String namespace, boolean useDefault)
    {
        String password = null;
        
        if (namespace != null)
        {
            String key = translateKey(namespace);
            
            Properties props = this.namespaceMap.get(key);
            if (props != null)
                password = props.getProperty(CMD.PASSWORD);
        }
        if (password == null && useDefault)
        {
            Properties props = this.namespaceMap.get(DEFAULT_NAMESPACE);
            if (props != null)
                password = props.getProperty(CMD.PASSWORD);
        }
        
        return password;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Set the username for this instance
     * @param user Username
     */
    
    public void setUsername(String name)
    {
        this.setUsername(DEFAULT_NAMESPACE, name);        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Set the username for this instance
     * @param user Username
     */
    
    public void setUsername(String namespace, String name)
    {
        if (namespace == null)
            namespace = DEFAULT_NAMESPACE;
        
        String key = translateKey(namespace);
        
        Properties props = this.namespaceMap.get(key);
        if (props == null)
        {
            props = new Properties();
            props.put(PLAIN_NAMESPACE, namespace);
                    
            this.namespaceMap.put(key, props);            
        }
        if (name != null)
            props.setProperty(CMD.USER, name);
        else
            props.remove(CMD.USER);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Set the password for this instance
     * @param pass Password
     */
    
    public void setPassword(String pass)
    {
        this.setPassword(DEFAULT_NAMESPACE, pass);
    }    
    
    //---------------------------------------------------------------------
    
    /**
     * Set the password for this instance
     * @param pass Password
     */
    
    public void setPassword(String namespace, String pass)
    {
        if (namespace == null)
            namespace = DEFAULT_NAMESPACE;
        
        String key = translateKey(namespace);            
        
        Properties props = this.namespaceMap.get(key);
        if (props == null)
        {
            props = new Properties();
            props.put(PLAIN_NAMESPACE, namespace);
            
            this.namespaceMap.put(key, props);            
        }
        if (pass != null)
            props.setProperty(CMD.PASSWORD, pass);
        else
            props.remove(CMD.PASSWORD);
        
        
    }  

    //---------------------------------------------------------------------
    
    /**
     * Returns array of declared namespaces, including the default
     * namespace if entry found.  
     * Note: Check if entry is the default namepsace by comparing it to 
     * LoginFile.DEFAULT_NAMESPACE string.
     * @return Array of declared namespaces
     */
    
    public String[] getNamespaces()
    {
        String[] values = new String[0];
        
        List<String> tempList = new ArrayList<String>();        
        Iterator<String> it = this.namespaceMap.keySet().iterator();
        
        while (it.hasNext())
        {
            String key = it.next();
            Properties props = this.namespaceMap.get(key);
            
            String ns = props.getProperty(PLAIN_NAMESPACE);
            if (ns != null)
                tempList.add(ns);
        }
        
        values = (String[]) tempList.toArray(values);
        
        return values;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the reference to the file managed by this instance.
     * @return Login file instance
     */
    
    public File getFile()
    {
        return (this.loginFile == null) ? null : 
                                          this.loginFile;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the reference to the directory containing the login file
     * managed by this instance.
     * @return Login file directory
     */
    
    public File getDirectory()
    {
        return (this.loginFile == null) ? null : 
                    this.loginFile.getParentFile();
    }
    
    //---------------------------------------------------------------------

    /**
     * Write usernames and passwords to the file     
     */
    
    public synchronized void commit() throws IOException
    {
        delete();
        
        this.loginFileIO.writeFile(this);        
    }    
    
    //---------------------------------------------------------------------
    
    /**    
      * Deletes the underlying login file
      */
    
    public synchronized void delete() throws IOException
    {
        this.loginFileIO.deleteFile(this);
    }
    
    //---------------------------------------------------------------------
    
//    /**
//     * Returns internal reference to data structure.
//     */
//    
//    protected Hashtable<String, Properties> getNamespaceMap()
//    {
//        return this.namespaceMap;
//    }

    //---------------------------------------------------------------------
    
    /**    
      * Removes credentials for a given namespace.
      * @param namespace Namespace to be removed.  If null,
      * then default namespace is removed.
      */
    
    public void remove(String namespace)
    {        
        if (namespace == null)
            namespace = DEFAULT_NAMESPACE;
        
        String key = translateKey(namespace);
        
        if (this.namespaceMap.containsKey(key))
        {
            this.namespaceMap.remove(key);
        }
    }
    
    //-----------------------------------------------------------------
    
    public String translateKey(String inKey)
    {
        if (inKey == null)
            return null;
        
        return inKey.toUpperCase();
    }
    
    
    //-----------------------------------------------------------------
    
    //=====================================================================
    //=====================================================================    
    
    /**
     * Encapsulates the IO operations for login file: write, read, delete.
     * Read checks for old format and will parse accordingly.
     */
    
    class LoginFileIO
    {
        //protected final static String oldIntro = "#FEI5 login information (DISCREET)";
        
        protected final static String HEADER_INTRO = "FEI5 login information (DISCREET)";
        protected final static String DATE_FORMAT  = "yyyy.MM.dd' at 'HH:mm:ssZ";
        
        protected SimpleDateFormat dateFormatter;
        
        private Logger _logger = Logger.getLogger(LoginFileIO.class.getName());
        
        //-----------------------------------------------------------------
        
        /**
         * Constructor
         */
        public LoginFileIO()
        {
            dateFormatter = new SimpleDateFormat(DATE_FORMAT); 
        }
        
        //-----------------------------------------------------------------
        
        /**
         * Reads in the contents of the file returned by loginFile.getFile(),
         * and populates the loginFile parameter with entries.  During the 
         * read, checks for deprecated format.  If found, parses using
         * old technique. 
         * @param loginFile LoginFile, with an assigned file member, that will
         * be populated
         * @throws FileNotFoundException If underlying file could not be found
         * @throws IOException If IO error occurs
         */
        
        protected synchronized void readFile(LoginFile loginFile) 
                                             throws FileNotFoundException, 
                                                    IOException 

        {            
            boolean oldFormatFound = false;
            
            //Security code review prompted this change to prevent DOS attack (nttoole 08.27.2013)
            //LineNumberReader reader = new LineNumberReader(new FileReader(
            //                                      loginFile.getFile()));
            LineNumberReader reader = new LineNumberReader(
                                       new BoundedBufferedReader(
                                        new FileReader(loginFile.getFile())));            

            String line = null;            
            
            //while more lines and we have not witnessed the old format
            while (!oldFormatFound && (line = reader.readLine()) != null)
            {                
                line = line.trim();
                
                //skip empty lines and commented lines
                if (isLineSkippable(line))
                    continue;

                //check for old format
                if (isLineFromOldFormat(line))                
                {
                    oldFormatFound = true;
                    continue;
                }
                
                //line should be colon-delimited> namespace:user:password[:expiry]
                String[] parts = line.split(":");
                if (parts == null || parts.length < 3)
                    continue;

                String ns = parts[0]; // namespace
                String us = parts[1]; // username
                String pw = parts[2]; // password
                String ex = parts.length > 3 ? parts[3] : null; // expiry

                // if null or empty, set to default value
                if (ns == null || ns.equals(""))
                    ns = DEFAULT_NAMESPACE;

                // if entry already exists, overwrite. Otherwise, create and add
                loginFile.setUsername(ns, us);
                loginFile.setPassword(ns, pw);    
                loginFile.setExpiry(  ns, ex);
            }            
            reader.close();
            
            //---------------------
            
            //we witnessed the old format, so read it the old way
            if (oldFormatFound)
            {
                this._logger.warn("Login file format appears to be deprecated." +
                		          "  Will attempt to load using old format. ");
                this.readOldFile(loginFile);
            }

            //---------------------            
            
        }
        
        //-----------------------------------------------------------------
        
        /**
         * Returns true if line entry is skippable (empty or comment)
         * @param line Line to be checked
         * @return True if considered irrelevant, false otherwise
         */
        
        protected boolean isLineSkippable(String line)
        {
            if (line == null)
                return true;
            
            line = line.trim();
            if (line.equals("")      || line.startsWith("#") || 
                line.startsWith("!") || line.startsWith("//"))
                return true;
            else
                return false;
        }
        
        //-----------------------------------------------------------------
        
        /**
         * Returns true if line entry has the old file format
         * @param line Line to be checked
         * @return True if in old login file format, false otherwise
         */
        
        protected boolean isLineFromOldFormat(String line)
        {
            boolean isOld = false;
            line = line.trim();
            
            if (line.startsWith(CMD.USER+"=") || 
                line.startsWith(CMD.PASSWORD+"="))
            {
                isOld = true;            
            }
            
            return isOld;
        }
        
        //-----------------------------------------------------------------
        
        /**
         * Reads deprecated login file (only one user and password entry)
         */
        protected synchronized void readOldFile(LoginFile loginFile) 
                            throws FileNotFoundException, IOException
        {
            File file = loginFile.getFile();
            
            Properties tmpProps = new Properties();
            FileIO.readConfiguration(tmpProps, file);
            
            String user = tmpProps.getProperty(CMD.USER);
            if (user != null) 
            {
                loginFile.setUsername(DEFAULT_NAMESPACE, user);         
            }

            String pass = tmpProps.getProperty(CMD.PASSWORD);
            if (pass != null) 
            {
                loginFile.setPassword(DEFAULT_NAMESPACE, pass);
            }
        }
        
        //-----------------------------------------------------------------
        
        /**
         * Writes the entries found in the loginFile parameter to
         * a file, as specified by the loginFile.getFile() location.
         * @param loginFile Instance of LoginFile, with file member
         * set, that will be written to disk.
         * @throws FileNotFoundException If underlying file could not be found
         * @throws IOException If IO error occurs
         */
        protected synchronized void writeFile(LoginFile loginFile) 
                                    throws FileNotFoundException, 
                                           IOException 

        {
            
            //---------------------
            
            File outFile = loginFile.getFile();
            
            //ensure directory exist before attempting to create file
            File outFileParent = null;
            try {
                outFileParent = outFile.getParentFile();
                
                //if it exist but is not a dir, then we will catch that
                //in the FileOutStream portion
                if (!outFileParent.exists())
                {
                    boolean createdDir = outFileParent.mkdirs();
                    if (!createdDir)
                    {
                        throw new IOException("Could not create directory " + 
                                              outFileParent.getAbsolutePath() +".");
                    }
                }
            } catch (SecurityException se) {
                throw new IOException("Could not create directory " + 
                         outFileParent.getAbsolutePath() +".  Reason: " + 
                         se.getMessage());
            }
        
            //---------------------
            
            FileWriter writer = new FileWriter(outFile, false);
            
            //write a header
            String header = this.getHeader();            
            writer.write(header);

            String[] namespaces = loginFile.getNamespaces();
            
            for (String ns : namespaces)
            {
                String us = loginFile.getUsername(ns, false);
                String pw = loginFile.getPassword(ns, false);
                String ex = loginFile.getExpiry(ns) + "";
                
                if (us != null && pw != null)
                {
                    String line = ns + ":" + us + ":" + pw;
                    
                    //if expiry was included
                    if (ex != null)
                        line = line + ":" + ex; 
                    
                    line = line + "\n";
                    
                    writer.write(line);
                }                
            }    
            
            writer.close();
        }
        
        //-----------------------------------------------------------------
        
        /**
         * Deletes file referenced by loginFile parameter.
         * @param loginFile Instance of LoginFile with file member set
         * @return True if file was deleted, false otherwise
         * @throws IOException If IO error occurs
         */
        
        protected synchronized boolean deleteFile(LoginFile loginFile) 
                                       throws IOException 
        {
            boolean wasDeleted = false;
            
            File file = loginFile.getFile();
            if (file.exists()) 
            {
                wasDeleted = file.delete();
            }
            
            return wasDeleted;
        }
        
        //-----------------------------------------------------------------
        
        
        /**
         * Returns header, including description, version and timestamp
         * @return Login file header info
         */
        
        protected String getHeader()
        {
            StringBuffer buffer = new StringBuffer("");
            buffer.append("# ").append(LoginFileIO.HEADER_INTRO).append("\n").
                   append("# ").append(Constants.CLIENTVERSIONSTR).append("\n").
                   //append("# Created on ").append(dateFormatter.format(new Date()))
                   append("# Created on ").append((new Date()).toString())
                   .append("\n\n");
            return buffer.toString();
        }
        
        //-----------------------------------------------------------------
        
        
        
        //-----------------------------------------------------------------
        
    }
    
    //=====================================================================
    //=====================================================================
    
    //---------------------------------------------------------------------
    
    /**
     * Test main.
     */
    
    public static void main(String[] args)
    {
        LoginFile lf = null;
        
        String filepath = null;
        if (args.length > 0)
        {
            filepath = args[0];
        }
        
        
        try {
            if (filepath == null)
                lf = new LoginFile();
            else
                lf = new LoginFile(new File(filepath));
            
            System.out.println("File = "+lf.getFile().getAbsolutePath());
            System.out.println("Dir  = "+lf.getDirectory().getAbsolutePath());

            
            String line = "Default entry>> username = " + lf.getUsername() + ", password = " + lf.getPassword();
            System.out.println(line);
            
            String[] namespaces = lf.getNamespaces();
            for (String ns : namespaces)
            {
                line = ns + ":" + lf.getUsername(ns) + ":" + lf.getPassword(ns);
                System.out.println("Entry: "+line);
            }
            
            lf.setUsername("cyclops", "scott");
            lf.setPassword("cyclops", "summers");
            lf.setExpiry( "cyclops", System.currentTimeMillis());
            lf.setUsername("pyro", "john");
            lf.setPassword("pyro", "allerdyce");
            
            lf.commit();
            
            System.out.print("Check the file, hit a key when ready to continue...");
            System.in.read();
            
            //---------------
            
            lf.remove("cyclops");
            namespaces = lf.getNamespaces();
            for (String ns : namespaces)
            {
                line = ns + ":" + lf.getUsername(ns) + ":" + lf.getPassword(ns) 
                          + ":" + lf.getExpiry(ns);
                System.out.println("Entry: "+line);
            }
            
            lf.commit();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
         
    }
    
    //---------------------------------------------------------------------
}
