package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.util.UrlInputStreamLoader;

import org.dom4j.Element;

/**
 * <b>Purpose:</b>
 * Factory that builds instances of configuration info based on the 
 * extension point attribute of an extension Element.
 * 
 * A lookup is performed to ascertain the class to be used for
 * a particular extension.  This information must be loaded from
 * an extension info properties file with extension point as key
 * and info class as the value.
 * 
 * A default extensions info file is provided.  However, it
 * can be overriden by specifying the new extensions info file
 * as the value of the <b>komodo.extensions.info</b> property.
 * 
 *   <PRE>
 *   Copyright 2005, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2005.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 03/01/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ExtensionFactory.java,v 1.5 2012/03/15 23:07:05 ntt Exp $
 *
 */

public class ExtensionFactory
{
    
    /**
     * Name of extensions file property.  If this property is defined, 
     * then the associated file will be used to map extension points
     * to extension classes.
     */
    protected static final String PROPERTY_EXTENSIONS_INFO = 
                                    "komodo.extensions.info";
    
    /**
     * Name of extensions file from external contributors.  
     * If this file found via resource searching mechanism, 
     * then the associated file will be used to map added 
     * extension points to extension classes.
     */
    protected static final String FILENAME_EXTENSIONS_INFO = 
                                    "komodo.extensions.info";
    
    
    protected static final String DEFAULT_EXTENSIONS_INFO = 
                                    "resources/extensions.info";
    
    
    /**
     * Contains lookup from extension point to associated configuration
     * info object.
     */
    
    private static Properties _extensionClasses;
    
    //---------------------------------------------------------------------
    
    static {
        loadExtensionInfo();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Loads extension class information.  Reads from a file that
     * associates extension points with specified instances of
     * configuration info classes.
     */
    
    protected static void loadExtensionInfoNew()
    {
        _extensionClasses = new Properties();
        
        List<URL> urlList = new ArrayList<URL>();
        
        //-------------------------
        
        URL primaryLocation = null;
        
        //attempt to load override file if specified
        String overrideExtFile = System.getProperty(PROPERTY_EXTENSIONS_INFO);
        if (overrideExtFile != null)
        {
            try {
                File tempFile = new File(overrideExtFile);
                if (tempFile.canRead())
                    primaryLocation = tempFile.toURL();               
            } catch (Exception ex) {
            }
        }
        
        //attempt to load default file if nothing yet laoded
        if (primaryLocation == null)
        {
            primaryLocation = ExtensionFactory.class.getResource(
                                         DEFAULT_EXTENSIONS_INFO);
        }
        
        //read in contents from input stream
        if (primaryLocation != null)
        {
            urlList.add(primaryLocation);              
        }
        
        
        //-------------------------
        //Look for external extension info

        try {
            Enumeration<URL> thirdPartyURLs = ExtensionFactory.class.getClassLoader().
                                                getResources(FILENAME_EXTENSIONS_INFO);
            
            while (thirdPartyURLs.hasMoreElements())
            {
                urlList.add(thirdPartyURLs.nextElement());
            }
            
        } catch (IOException ioEx) {    
            ioEx.printStackTrace();
        }
        
        //-------------------------
        
        Iterator<URL> urlIt = urlList.iterator();
        while (urlIt.hasNext())
        {
            loadExtensionInfoFromUrl(urlIt.next());
        }
        
        //-------------------------           
    }
    
    protected static void loadExtensionInfoFromUrl(URL extensionFileUrl)
    {
        InputStream inStream = null;
        Properties tempProps = new Properties();
        
        if (extensionFileUrl != null)
        {
            try {                
                //inStream = extensionFileUrl.openStream();                 
                inStream = UrlInputStreamLoader.open(extensionFileUrl);
            } catch (Exception ex) {
                inStream = null;
            }
        }
        
        //read in contents from input stream
        if (inStream != null)
        {
            try {
                tempProps.load(inStream);
                _extensionClasses.putAll(tempProps);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    inStream.close();
                } catch (IOException ioEx) {
                    ioEx.printStackTrace();
                }
            }
            
        }
            
    }
    
    protected static void loadExtensionInfo()
    {
        _extensionClasses = new Properties();
        InputStream inStream = null;
        
        
        //attempt to load override file if specified
        String overrideExtFile = System.getProperty(PROPERTY_EXTENSIONS_INFO);
        if (overrideExtFile != null)
        {
            try {
                inStream = new FileInputStream(overrideExtFile);
            } catch (Exception ex) {
                inStream = null;
            }
        }
        
        //attempt to load default file if nothing yet laoded
        if (inStream == null)
        {
            inStream = ExtensionFactory.class.getResourceAsStream(
                                         DEFAULT_EXTENSIONS_INFO);
        }
        
        //read in contents from input stream
        if (inStream != null)
        {
            try {
                _extensionClasses.load(inStream);
            } catch (Exception ex) {
                _extensionClasses.clear();
            }
        }
    }

    //---------------------------------------------------------------------
    
    public static Class getClassForPoint(String point) throws Exception
    {
        Class clazz;
        
        String pointClassname = _extensionClasses.getProperty(point);
        if (pointClassname == null)
            throw new Exception("Unrecognized extension point: "+point);
        
        try {
            clazz = Class.forName(pointClassname);
        } catch (ClassNotFoundException cnfEx) {
            throw new Exception("Could not locate class for extension "
                    + point +", class = " + pointClassname);
        }
        
        return clazz;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Parses components of an extension element based upon the point
     * attribute of the extension info obejct, which is passed in as 
     * the parent  parameter. Returns a list of associated 
     * ConfigurationInfo components.
     * @param element JDOM element of the extension node
     * @param parent Instance of ExtensionInfo 
     * @return List of ConfigurationInfo
     */
    
    public static List buildExtensions(Element element, 
                                       ExtensionInfo parent)
                                       throws Exception
    {
        String point = parent.getExtensionPoint();
        List list = new ArrayList();
        Class clazz;
        
        //get the class associated with point
        clazz = getClassForPoint(point);
        
        ConfigurationInfo newInfo;
        
        List children = element.elements();
        int numChildren = children.size();
        for (int i = 0; i < numChildren; ++i)
        {
            Element child = (Element) children.get(i);
            
            newInfo = (ConfigurationInfo) clazz.newInstance();
            newInfo.initialize(child, parent);
            list.add(newInfo);
        }
        
        return list;
    }
    
    //---------------------------------------------------------------------
    
}
