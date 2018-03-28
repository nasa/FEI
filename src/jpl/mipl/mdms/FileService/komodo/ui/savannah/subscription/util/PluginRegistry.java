package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler.SubscriptionHandler;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Registry for Savannah subscription plugin extensions plugins.  
 * Plugins are loaded using the PluginLoader.
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
 * @version $Id: PluginRegistry.java,v 1.5 2008/09/05 19:09:42 ntt Exp $
 *
 */

//NOTE - public methods must first call checkInit to ensure initialization

public class PluginRegistry
{   
    //---------------------------------------------------------------------
    
    /** Map from unique plugin id to plugin info */
    protected static Map plugins;
    
    /** Map from extension names to Lists of extension info objects */
    protected static Map extensions;
    
    /* logger */
    private static Logger logger = Logger.getLogger(PluginRegistry.class.getName());
    
    /** Flag indicating that instance has been instantiated */
    private static boolean initialized = false;
    
    //---------------------------------------------------------------------
    
    /**
     * Checks that registry has been initialized.  If not, then the init
     * method is called.  If so, then returns immediately.
     */
    
    protected static void checkInit()
    {
        if (!initialized)
            init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes the registry by loading plugins via plugin loader.
     */
    
    protected static void init()
    {
        //create maps
        plugins = new Hashtable();
        extensions = new Hashtable();
        
        loadPlugins();
        loadExtensions();
        
        initialized = true;
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * Loads plugins via a plugin loader instance.
     * Adds plugin info objects to plugins map using the id as key.
     * If a duplicate id is found, an error message is printed and
     * the latter is ignored.
     */
    
    protected static void loadPlugins()
    {
        PluginLoader loader = PluginLoader.createLoader();
        
        List plist = loader.getPlugins();
        int numPlugins = plist.size();
        
        for (int i = 0; i < numPlugins; ++i )
        {
            PluginInfo pInfo = (PluginInfo) plist.get(i);
            String id = pInfo.getId();
            if (id == null)
                continue;
            
            // check for duplicates here
            if (plugins.containsKey(id))
            {
                PluginInfo other = (PluginInfo) plugins.get(id);
            
                logger.error("Multiple plugins found with id '" + id
                             + "'.\n Location 1 = "
                             + other.getLocation().getAbsolutePath() + "\n "
                             + "Location 2 = "
                             + pInfo.getLocation().getAbsolutePath() + "\n"
                             + "Ignoring the latter...");
            }
            else
            {
                logger.debug("Loaded plugin '"+id+"'");
                plugins.put(pInfo.getId(), pInfo);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Preprocessing step that occurs after plugins have been loaded.
     * Iterates through plugins searching for declared extension points.
     * Extensions are then added to the extensions map which maps from
     * extension point name to a list of extensions.
     */
    
    protected static void loadExtensions()
    {
        Iterator ids = plugins.keySet().iterator();
        String id;
        PluginInfo pInfo;
        
        //for each plugin
        while (ids.hasNext())
        {
            id = (String) ids.next();
            pInfo = (PluginInfo) plugins.get(id);
            
            //for each extension point declared in plugin, add extension
            String[] extenPts = pInfo.getExtensionPoints();
            for (int i = 0; i < extenPts.length; ++i)
            {
                String extensionPoint = extenPts[i];
                ExtensionInfo eInfo = pInfo.getExtension(extensionPoint);
                addToExtensionPoint(extensionPoint, eInfo);
            }
        }
    }
       
    //---------------------------------------------------------------------
    
    /**
     * Adds extension info object to list of extensions associated with
     * the extension point if not already present.
     * @param extensionPoint Name of extension point
     * @param extension Extension info object to be added
     */
    
    protected static void addToExtensionPoint(String extensionPoint, 
                                              ExtensionInfo extension)   
    {
        List list = (List) extensions.get(extensionPoint);
        if (list == null)
        {
            list = new ArrayList();
            extensions.put(extensionPoint, list);
        }
        
        if (!list.contains(extension))
            list.add(extension);
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns array of extension info objects associated with extension
     * point.
     * @param extensionPoint Name of extension point
     * @return Array of ExtensionInfo
     */
    
    protected static ExtensionInfo[] getFromExtensionPoint(String extensionPoint)
    {
        ExtensionInfo[] eInfos;
        
        List list = (List) extensions.get(extensionPoint);
        if (list != null)
        {
            int numExts = list.size();
            eInfos = new ExtensionInfo[numExts];
            for (int i = 0; i < numExts; ++i)
                eInfos[i] = (ExtensionInfo) list.get(i);
        }
        else
        {
            eInfos = new ExtensionInfo[0];
        }

        return eInfos;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieves plugin info object based on plugin id.
     * @param Plugin id
     * @return Plugin info associated with id, null if none found.
     */
    
    public static PluginInfo getPluginInfo(String id)
    {
        checkInit();
        
        PluginInfo pInfo = (PluginInfo) plugins.get(id);
        return pInfo;
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * Returns array of extensions associated with extension point
     * @param Extension point name
     * @return Array of extensions associated with extension point from
     * registry.
     */ 
    public static ExtensionInfo[] getExtensions(String point)
    {
        checkInit();
        
        ExtensionInfo[] extensions = getFromExtensionPoint(point);
        return extensions;
    }
   
    //---------------------------------------------------------------------
    
    /**
     * Collects listing of jar files from the runtime info objects of
     * the plugin info parameter. 
     * @param pInfo Plugin info for which Jars are required
     * @return File array of jar files required for plugin.
     */
    
    public static File[] getLibraries(PluginInfo pInfo) throws IOException
    {   
        List list = getLibraries(pInfo, new ArrayList());
        
        int numJars = list.size();
        File[] jars = new File[numJars];
        for (int i = 0; i < numJars; ++i)
        {
            jars[i] = (File) list.get(i);
        }
        
        return jars;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Recursively collects listing of jar files from the runtime info 
     * objects of the plugin info parameter and all other plugins that
     * parameter is dependent upon (from PluginInfo.getImports().  
     * @param pInfo Plugin info for which Jars are required
     * @param previous List of previous plugin id's, used to detect
     *        loops 
     * @return List of File, jar files required for plugin.
     */
    
    protected static List getLibraries(PluginInfo pInfo, List previous) 
                                                        throws IOException
    {
        //check for dependency loop
        String curId = pInfo.getId();
        if (previous.contains(curId))
            throw new IOException("Loop detected in import dependency for '"
                                  + curId+"'");
        previous.add(curId);
        
        //create empty list for jar files
        List list = new ArrayList();
        
        String[] libs = pInfo.getRuntime().getLibraries();
        String[] plugins = pInfo.getRuntime().getImports();
        File parentDir = pInfo.getLocation();
        
        //add current plugin libraries
        for (int i = 0; i < libs.length; ++i)
        {
            File jar = new File(parentDir, libs[i]);
            if (!jar.canRead())
                throw new IOException("Could not locate library '"
                                      + jar.getAbsolutePath() + "'");
            if (!list.contains(jar))
                list.add(jar);
        }
        
        //add libraries from imported plugins
        for (int i = 0; i < plugins.length; ++i)
        {
            PluginInfo curPInfo = getPluginInfo(plugins[i]);
            List curList = getLibraries(curPInfo, previous);
            list.addAll(curList);
        }
        
        return list;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Loads and initializes an instance of SubscriptionHanlder using
     * the handler info parameter.
     * @param hInfo handler info instance describing handler
     * @return Initialized instantiation of handler if successful,
     *         null otherwise
     * @throws Exception if error occurs
     */
    public static SubscriptionHandler loadHandler(HandlerInfo hInfo) 
                                                    throws Exception
    {
        checkInit();
        
        SubscriptionHandler handler = null;
        String classname = hInfo.getClassname();
        PluginInfo pInfo = PluginInfo.getPluginInfo(hInfo);
        File[] jars = getLibraries(pInfo);
        
       Object obj = instantiateClass(jars, classname);
        if (obj instanceof SubscriptionHandler)
        {
            handler = (SubscriptionHandler) obj;
            handler.initialize(hInfo);
        }
        
        return handler;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Loads and instatiates class using the array of jars as a 
     * classpath.
     * TODO - I think the system classpath might be referenced
     *        by this loader.  Check to see if this will cause
     *        any conflicts.
     * @param jars Array of jar files used for classpath
     * @param name Classname to load and instantiate (must have a 
     *        zero-argument constructor)
     * @return Instantiation of classname
     * @throws Exception if error occurred.
     */
    
    public static Object instantiateClass(File[] jars, String name) 
                                                      throws Exception
    {
        Class clazz = null;
        
        URL[] urls = new URL[jars.length];
        
        for (int i = 0; i < jars.length; ++i)
            urls[i] = jars[i].toURL();
        
        URLClassLoader cLoader = new URLClassLoader(urls);
        clazz = cLoader.loadClass(name);
        
        Object o = clazz.newInstance();
        
        return o;
    }

    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
}
