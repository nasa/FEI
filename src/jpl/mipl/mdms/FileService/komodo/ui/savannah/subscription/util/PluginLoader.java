/*
 * Created on Feb 16, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.utils.logging.Logger;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


/**
 * <b>Purpose:</b>
 * Loads and initializes plug-ins for the Savannah application.
 * Searches global and local plugin paths, examining directories
 * containing a plugin.xml manifest.  
 * <BR>
 * 
 * Search paths include: <PRE>
 * if system property <i>komodo.global.plugins<\i> set and exists
 *      value of property ( ${komodo.global.plugins} )
 * else
 *      default global dir ( ${komodo.config.dir}/plugins/ )
 * 
 * if system property <i>komodo.local.plugins<\i> set and exists
 *      value of property ( ${komodo.local.plugins} )
 * else
 *     default local dir ( ${user.home}/.komodo/plugins/ )
 * </PRE>
 * 
 * See <code>PluginInfo</code> for manifest structure.
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
 * 02/16/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: PluginLoader.java,v 1.6 2016/09/29 23:30:25 ntt Exp $
 *
 */

public class PluginLoader
{
    public static final String PLUGIN_MANIFEST = "plugin.xml";
    
    public static final String PROPERTY_LOCAL_PLUGINS  = "komodo.local.plugins";
    public static final String PROPERTY_GLOBAL_PLUGINS = "komodo.global.plugins";
    public static final String PROPERTY_CONFIG_DIR     = Constants.PROPERTY_CONFIG_DIR;
    
    public static final int ALL_PLUGINS    = 0;
    public static final int LOCAL_PLUGINS  = 1;
    public static final int GLOBAL_PLUGINS = 2;
    
    public static final String ELEMENT_PROPERTIES = "properties";
    public static final String ELEMENT_PROPERTY   = "property";
    public static final String ATTR_NAME          = "name";
    public static final String ATTR_VALUE         = "value";
    
    private Logger _logger = Logger.getLogger(PluginLoader.class.getName());
    
    private static PluginLoader _instance;
    
    //---------------------------------------------------------------------
    
    private PluginLoader() {}
    
    //---------------------------------------------------------------------
    
    /**
     * Returns instance of loader.
     */
    
    public static PluginLoader createLoader()
    {
        if (_instance == null)
            _instance = new PluginLoader();
        return _instance;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns List of plugins, both global and local.
     */
    
    public List getPlugins()
    {
        return getPlugins(ALL_PLUGINS);
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Checks komodo.local.plugins property.  If defined, then if value of
     * property exists as a directory, it is returned.  Otherwise, if the
     * default local source exists, it is returned.  Else null is returned.
     */
    
    protected File getLocalPluginSource()
    {
        File source = null;
        String value = System.getProperty(PROPERTY_LOCAL_PLUGINS);
        if (value != null)
        {
            source = new File(value);
            if (!source.exists())
                source = null;
        }   
        
        if (source == null)
        {
            //look for the .komodo/plugins dir
            value = System.getProperty("user.home") + File.separator
                    + Constants.RESTARTDIR + File.separator 
                    + Constants.PLUGINSDIR;
            
            source = new File(value);
            if (!source.exists())
            {
                _logger.warn("Could not locate local plugins directory.");
                source = null;
            }
        }
        
        if (source != null)
            _logger.debug("Local plugin source = "+source.getAbsolutePath());
        else
            _logger.debug("No local plugin source found.");
        
        return source;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Checks komodo.global.plugins property.  If defined, then if value of
     * property exists as a directory, it is returned.  Otherwise, if the
     * default global source exists, it is returned.  Else null is returned.
     */
    
    protected File getGlobalPluginSource()
    {
        File source = null;
        String value = System.getProperty(PROPERTY_GLOBAL_PLUGINS);
        if (value != null)
        {
            source = new File(value);
            if (!source.exists())
                source = null;
        }   
        
        if (source == null)
        {
            value = System.getProperty(PROPERTY_CONFIG_DIR);
        
            if (value != null)
            {
                source = new File(value);
                if (!source.exists())
                {
                    _logger.warn("Could not locate global plugins directory.");
                    source = null;
                }
            }  
        }
            
        if (source != null)
            _logger.debug("Global plugin source = "+source.getAbsolutePath());
        else
            _logger.debug("No global plugin source found.");
        
        return source;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns array of directories that will be examined for plugins.
     * @param flag Indicates that caller is interested in local, global,
     *             or both sources. (respectively LOCAL_PLUGINS, 
     *             GLOBAL_PLUGINS, ALL_PLUGINS)
     * @return Array of directories to search for plugins
     */
    
    protected File[] getPluginSources(int flag)
    {
        List list = new ArrayList();
        File[] sources = new File[0];
        File file;
        
        if (flag == GLOBAL_PLUGINS || flag == ALL_PLUGINS)
        {
            file = getGlobalPluginSource();
            if (file != null && !list.contains(file))
                list.add(file);
        }
        
        if (flag == LOCAL_PLUGINS || flag == ALL_PLUGINS)
        {
            file = getLocalPluginSource();
            if (file != null && !list.contains(file))
                list.add(file);
        }          
        
        int numFiles = list.size();
        sources = new File[numFiles];
        for (int i = 0; i < numFiles; ++i)
        {
            sources[i] = (File) list.get(i);
            _logger.trace("Plugin source "+i+": = "+sources[i].getAbsolutePath());
        }
        
        return sources;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Return list of plugins according to the source flag.
     * @param flag Indicates that caller is interested in local, global,
     *             or both sources. (respectively LOCAL_PLUGINS, 
     *             GLOBAL_PLUGINS, ALL_PLUGINS)
     * @return List of plugins associated with the source flag. 
     */
    
    public List getPlugins(int flag)
    {
        File[] pluginSources = getPluginSources(flag);
        
        List plugins = new ArrayList();
        
        for (int i = 0; i < pluginSources.length; ++i)
        {
            List curPlugins = getPlugins(pluginSources[i]); 
            if (curPlugins != null)
                plugins.addAll(curPlugins);
        }
    
        return plugins;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieve plugins within a specific source directory
     * @param sourceDir Directory containing plugin directories.
     * @return List of plugs within the source directory.
     */
    
    protected List getPlugins(File sourceDir)
    {
        _logger.trace("Searching for plugins in  = " + sourceDir.getPath());
        
        List plugins = new ArrayList();
        File[] children = sourceDir.listFiles();
        File dir, file;
        
        if (children == null)
            children = new File[0];
        
        for (int i = 0; i < children.length; ++i)
        {
            dir = children[i];
            if (!dir.isDirectory())
                continue;

            file = new File(dir, PLUGIN_MANIFEST);
            
            _logger.trace("Checking for manifest at "+file.getAbsolutePath());
            if (!file.canRead() || file.isDirectory())
                continue;
            _logger.trace("Confirmed manifest at "+file.getName()+". Loading...");
            
            PluginInfo plugin = loadPlugin(file);
            if (plugin != null && !plugins.contains(plugin))
                plugins.add(plugin);
        }
        
        return plugins;
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Load plugin information from a plugin manifest.
     * @param manifest Plugin manifest file
     * @return Plugin info object initialized from manifest.
     */
    
    protected PluginInfo loadPlugin(File manifest)
    {
        PluginInfo pInfo = null;
        Element root = getPluginManifestRoot(manifest);
        
        if (root != null)
        {
            try {
                pInfo = new PluginInfo();
                pInfo.initialize(root, null);
                pInfo.setLocation(manifest.getParentFile());
            } catch (Exception ex) {
                _logger.error("Could not initialize plugin info from manifest: "
                        + manifest.getAbsolutePath());
                _logger.debug(ex.getMessage(), ex);
                pInfo = null;
            }
        }
        
        return pInfo;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the root JDOM element of the plugin manifest file.  If 
     * file could not be parsed correctly, null is returned.
     * @param manifest Plugin manifest file
     * @return root element of document, null if could not be parsed.
     */
    
    protected Element getPluginManifestRoot(File manifest)
    {
        Element root = null;

        try {
            InputStream is = new FileInputStream(manifest);
            
            SAXReader reader = new SAXReader();
            Document doc = reader.read(is);
            root = doc.getRootElement();
            
        } catch (Exception ex) {
            _logger.error("Could not parse plugin manifest: "
                          + manifest.getAbsolutePath());
            _logger.debug(ex.getMessage(), ex);
            root = null;
        }
        
        return root;
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    /*
     * Load handlers.
     * Check flag for global plugins.  If enabled, goto global
     * plugin repository: FEI5/config/plugins.  Load.
     * Check if local plugin repos exists: ~/.komodo/plugins/.  Load.
     * 
     * Load:
     * For each directory in repos, check for plugin.xml file.
     * Load plugin.xml file, looking for extension point: 
     * jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handlers
     * 
     * If found, then parse underlying handler information to configuration
     * elements.  This should include name, class, properties, resource dir.
     * Question: How to include JAR file in the classpath?
     * Idea:  String[] classpath = new String[] { "../place/file.jar" ... };
     *        JarClassLoader 
     */
    
    //---------------------------------------------------------------------
    
    

    
    //---------------------------------------------------------------------
    
    /* *
     * Initialize a plug-in.
     * @param elementName The name used in the JDOM tree
     * @param clazz The Interface class
     * @param child The JDOM element holding the plug-in data 
     * @return Instance of SubscriptionHandler
     * @throws Exception if handler could not be successfully instantiated
     * / 
    
    protected SubscriptionHandler initHandler(String elementName, 
                                              Class clazz, Element child) 
                                              throws Exception
    {
        if (child == null)
            throw new Exception("No '" + elementName + 
                                "' element in parameter file.");

        // Get 'class' attribute
        Attribute classAtt = child.getAttribute("class");
        if (classAtt == null)
            throw new Exception("No 'class' attribute for the '" + elementName
                                + "' element.");

        // Make an instance of this plug-in
        String className = classAtt.getValue();
        Object o = Class.forName(className).newInstance();

        // Check for correct interface
        if (!clazz.isInstance(o))
            throw new Exception("Not an " + clazz.getName() + " class: "
                    + o.getClass().getName());
        
        SubscriptionHandler h = (SubscriptionHandler) o;
        //h.initialize(child);

        return h;
    }
    */
    
    
    //---------------------------------------------------------------------
    
    /**
     * Utility method that extracts properties from a JDOM
     * element and returns an associated map.
     * @param The element containing a properties child element.
     * @return Properties parsed from the element.
     *         If no properties were found, then an empty map is 
     *         returned.
     */
    
    public static Properties extractProperties(Element element)
    {
        Properties props = new Properties();
        
        //get the properties element
        Element propsEle = element.element(ELEMENT_PROPERTIES);
          
        if (propsEle != null)
        {
            //get the individual property elements from properties
            List children = propsEle.elements(ELEMENT_PROPERTY);
            Element propEle;
            String key, val;
            
            int numChildren = children.size();
            
            for (int i = 0; i < numChildren; ++i)
            {
                propEle = (Element) children.get(i);
                if (propEle != null)
                {
                    key = propEle.attributeValue(ATTR_NAME);
                    val = propEle.attributeValue(ATTR_VALUE);
                    if (key != null && val != null)
                        props.put(key, val);
                }
            }
        }
            
        return props;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Utility method that interprets param value as a boolean true or
     * false. 
     * @param value String value to be interpretted
     * @param def Default return value if value param cannot be interpretted
     * @return True if value maps to true, false if value maps to false,
     *         and def if neither can be decided.
     */
    
    public static boolean getBooleanValue(String value, boolean def)
    {
        if (value == null || value.equals(""))
            return def;
        
        if (value.equalsIgnoreCase("true") || value.equals("1") ||
            value.equalsIgnoreCase("on") || value.equalsIgnoreCase("y") ||
            value.equalsIgnoreCase("yes"))
        {
            return true;
        }
        
        if (value.equalsIgnoreCase("false") || value.equals("0") ||
            value.equalsIgnoreCase("off") || value.equalsIgnoreCase("n") ||
            value.equalsIgnoreCase("no"))
        {
                return false;
        }
        
        return def;
    }
    
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    /**
     * Testing only...
     */
    public static void main(String[] args)
    {
        if (args.length != 0)
        {
            System.out.println("Usage: java progname");
            System.exit(1);
        }
        
        PluginLoader loader = PluginLoader.createLoader();
        List plugins = loader.getPlugins();
        
        for (int i = 0; i < plugins.size(); ++i)
        {
            PluginInfo pInfo = (PluginInfo) plugins.get(i);
            System.out.println("Plugin #"+i+" = "+pInfo);
        }
    }
    
    //---------------------------------------------------------------------
}
