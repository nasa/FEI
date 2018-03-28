package jpl.mipl.mdms.FileService.komodo.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;


/**
 * <b>Purpose:</b>
 * Utility class that resolves the URL of the domain file.
 * 
 * Caller can directly provide a path value, or it can be
 * automatically retrieved from the system property.
 * 
 * If a path is determined, this class will first check for
 * a file with the given path.  If found, the corresponding URL
 * is returned.  Otherwise, the path is considered a resource
 * location, and the URL of the first matching resource will
 * be returned.
 * 
 *   <PRE>
 *   Copyright 2008, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2008.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 05/29/2008        Nick             Initial release. 
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @author Adrian   Tinio   (Adrian.W.Tinio@jpl.nasa.gov)
 * @version $Id: ConfigFileURLResolver.java,v 1.3 2012/03/15 22:42:40 ntt Exp $
 *
 */

public class ConfigFileURLResolver
{
    //private Logger _logger = Logger.getLogger(ConfigFileURLResolver.class.getName());
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     */
    
    public ConfigFileURLResolver()
    {        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieve the URL of the configuration file as specified by the corresponding
     * system property.
     * @return Configuration file URL, if found.  Else null.
     * @throws SessionException If error occurs
     */
    
    public URL getFileURLViaProperty(String propertyName) throws SessionException
    {
        String propertyValue = getValueFromSystemProperty(propertyName);
        
        return getFileURL(propertyValue);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieve the URL of the domain file as specified by the corresponding
     * system property.
     * @return Domain file URL, if found
     * @throws SessionException If error occurs
     */
    
    public URL getDomainFile() throws SessionException
    {
        return getFileURLViaProperty(Constants.PROPERTY_DOMAIN_FILE);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieve the URL of the keystore file as specified by the corresponding
     * system property.
     * @return Keystore file URL, if found
     * @throws SessionException If error occurs
     */
    
    public URL getKeyStoreFile() throws SessionException
    {
        return getFileURLViaProperty(Constants.PROPERTY_SSL_TRUSTSTORE);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieve the URL of the log configuration file as specified by the corresponding
     * system property.
     * @return Log config file URL, if found
     * @throws SessionException If error occurs
     */
    
    public URL getLoggingConfigFile() throws SessionException
    {
        return getFileURLViaProperty(Constants.PROPERTY_LOGGING_CONFIG);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieve the URL of the domain file as specified by the method
     * parameter
     * @param configFilePath File or resource path to the domain file
     * @return Corresponding domain file URL, if found
     * @throws SessionException If error occurs
     */
    
    public URL getFileURL(String configFilePath) throws SessionException
    {
        URL configFileUrl = null;
    
        if (configFilePath == null)
            return null;
        
        if (configFilePath == null)
            throw new SessionException("No configuration file was specified",
                                       Constants.MISSINGARG);
        
        File configFile = new File(configFilePath);
        if (configFile.canRead())
        {
            try {
                configFileUrl = configFile.toURL();
            } catch (MalformedURLException muEx) {
                
                throw new SessionException("Error occured while retrieving " +
                		            "URL from domain file: "+muEx.getMessage(),
                		            Constants.DOMAINIOERR);
            }
        }
        else
        {
            configFileUrl = this.getClass().getResource(configFilePath);
            if (configFileUrl == null)
            {
                throw new SessionException("Could not find resource " +
                                configFilePath, Constants.DOMAINIOERR);
            }
            configFileUrl = ConfigFileURLResolver.fixJarURL(configFileUrl);
        }
        
        
        return configFileUrl;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Attempts to open stream from URL to determine if the
     * resource exists.
     * @param url The URL to be read
     * @return True if resource is readable, false otherwise
     */
    
    public static boolean canReadURL(URL url)
    {
        boolean canRead = true;
        
        InputStream is = null;        
        try {
            //is = url.openStream();
            is = UrlInputStreamLoader.open(url);
        } catch (IOException ioEx) {
            canRead = false;
        } finally {
            if (is != null)
            {
                try {
                    is.close();
                } catch (IOException ioEx) {                                        
                }
            }
        }
        
        return canRead;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the value associated with the system domain file
     * property, if defined.
     * @return Value of domain file system property
     */
    
//    protected String getDomainFromSystem()
//    {        
//        return getValueFromSystemProperty(Constants.PROPERTY_DOMAIN_FILE);        
//    }
    
    //---------------------------------------------------------------------
    
    protected String getValueFromSystemProperty(String propertyName)
    {
        String entry;
        
        entry = System.getProperty(propertyName);
        
        return entry;
    }
    
    //---------------------------------------------------------------------
    
    public static URL resolve(String configFilePath) throws SessionException
    {
        ConfigFileURLResolver resolver = new ConfigFileURLResolver();
        return resolver.getFileURL(configFilePath);
    }
    
    /**
     * Workaround method to fix java 1.5.0_16 bug (bug: 6746185)
     * @param url
     * @return returns full URL
     */
    public static URL fixJarURL(URL url) {
        String originalURLProtocol = url.getProtocol();
   
        if ("jar".equalsIgnoreCase(originalURLProtocol) == false)
            return url;
     

        String originalURLString = url.toString();
        int bangSlashIndex = originalURLString.indexOf("!/");
        if (bangSlashIndex > -1)
            return url;
     
        String originalURLPath = url.getPath();
     
        URLConnection urlConnection;
        try {
            urlConnection = url.openConnection();
            if (urlConnection == null)
                throw new IOException("urlConnection is null");
        }
        catch (IOException e) {
            return url;
        }
     
        Permission urlConnectionPermission;
        try {
            urlConnectionPermission = urlConnection.getPermission();
            if (urlConnectionPermission == null)
                throw new IOException("urlConnectionPermission is null");
        }
        catch (IOException e) {
            return url;
        }
     
        String urlConnectionPermissionName = urlConnectionPermission.getName();
        if (urlConnectionPermissionName == null)
            return url;
   
     
        File file = new File(urlConnectionPermissionName);
        if (file.exists() == false)
            return url;
     
     
        String newURLStr;
        try {
            newURLStr = "jar:" + file.toURL().toExternalForm() + "!/" + originalURLPath;
        } catch (MalformedURLException e) {
            return url;
        }
     
        try {
            url = new URL(newURLStr);
        } catch (MalformedURLException e) {
            return url;
        }
     
        return url;
    }
    
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.err.println("Required parameter: domain file path");
            System.exit(1);
        }
        
        String arg = args[0];
        
        ConfigFileURLResolver urlResolver = new ConfigFileURLResolver();
        
        URL domainFile = null;
        try {
            domainFile = urlResolver.getDomainFile();
            System.out.println("URL = "+domainFile);
        } catch (SessionException sesEx) {
            sesEx.printStackTrace();
        }
        
        try {
            domainFile = urlResolver.getFileURL(arg);
            System.out.println("URL = "+domainFile);
        } catch (SessionException sesEx) {
            sesEx.printStackTrace();
        }
        
        
    }
}
