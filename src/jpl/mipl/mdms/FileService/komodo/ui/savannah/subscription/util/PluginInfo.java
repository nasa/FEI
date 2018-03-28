/*
 * Created on Mar 1, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * <b>Purpose:</b>
 * Extension information for a plugin.  Contains information on plugin name,
 * id, and sub-components.
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
 * @version $Id: PluginInfo.java,v 1.6 2008/09/05 19:09:42 ntt Exp $
 *
 */

public class PluginInfo implements ConfigurationInfo
{
    
    public static final String PROPERTY_NAME    = "name"; //required
    public static final String PROPERTY_ID      = "id";   //required 
    public static final String PROPERTY_VERSION = "version"; //required
    public static final String PROPERTY_DESC    = "desc"; //optional
    public static final String PROPERTY_ORG     = "org"; //optional
    
    
    protected String _name, _id, _version, _org, _desc;
    protected File   _location;
    
    public static final String ELEMENT_EXTENSION = "extension";
    public static final String ELEMENT_RUNTIME   = "runtime";
    
    //---------------------------------------------------------------------
 
    /** Reference to parent info node */
    protected ConfigurationInfo _parent = null;
    
    /** Reference to the plugin runtime object */
    protected RuntimeInfo _runtime = null;
    
    /** Contains plugin properties */
    protected Properties _properties;
    
    /**
     * Map from extension point to extension.
     * Plugin extension points are assumed to be unique.  Hence, there
     * should never be duplicate extension points defined for a plugin.
     * When there is more than one behavior to be added to a point,
     * then the underlying point element should be a set type containing
     * those.
     */
    protected Map _extensions;  //entension point name -> extension array
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Creates empty info object.
     */
    
    public PluginInfo()
    {
        this._properties = new Properties();
        this._extensions = new Hashtable();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes info object from JDOM element of plugin configuration
     * file.
     * @param element JDOM element containing plugin information
     * @param parent Parent configuration info instance
     * @throws Exception if initialization error occurs
     */
    
    public void initialize(Element element, ConfigurationInfo parent)
                                                   throws Exception
    {
        this._parent = parent;
        
        //get name
        Attribute attr = element.attribute(PROPERTY_NAME);
        if (attr == null)
            throw new Exception("No "+PROPERTY_NAME+" attribute found in '" + 
                                element.getName() + "' element.");
        this._name = attr.getValue();
        
        //get  version
        attr = element.attribute(PROPERTY_VERSION);
        if (attr == null)
            throw new Exception("No "+PROPERTY_VERSION+" attribute found in '" + 
                                element.getName() + "' element.");
        this._version = attr.getValue();
        
        //check for description
        attr = element.attribute(PROPERTY_DESC);
        if (attr == null)
            throw new Exception("No "+PROPERTY_DESC+" attribute found in '" + 
                                element.getName() + "' element.");
        this._desc = attr.getValue();
        
        //check for id (required)
        attr = element.attribute(PROPERTY_ID);
        if (attr == null)
            throw new Exception("No "+PROPERTY_ID+" attribute found in '" + 
                                element.getName() + "' element.");
        this._id = attr.getValue();
        
        //check for implementing organization
        attr = element.attribute(PROPERTY_ORG);
        if (attr != null)
            this._org = attr.getValue();
        
        //init sub components
        initRuntime(element);
        initExtensions(element);
    }
    

    //---------------------------------------------------------------------

    /**
     * Loads runtime info for this plugin.
     * @param element JDOM element for plugin
     */
    
    protected void initRuntime(Element element) throws Exception
    {
        this._runtime = new RuntimeInfo();
        
        Element child = element.element(ELEMENT_RUNTIME);

        this._runtime.initialize(child, this);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Loads extension info for this plugin.
     * Adds entries to the extensions map using the extension point
     * as a key.
     * @param element JDOM element for plugin
     */
    
    protected void initExtensions(Element element) throws Exception
    {
        List children = element.elements(ELEMENT_EXTENSION);
        int numChildren = children.size();
        
        for (int i = 0; i < numChildren; ++i)
        {
            Element child = (Element) children.get(i);
            
            ExtensionInfo eInfo = new ExtensionInfo();
            eInfo.initialize(child, this);
            
            if (eInfo.getExtensionPoint() != null)
                this._extensions.put(eInfo.getExtensionPoint(), eInfo);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns property from this info object. 
     * @param name Property name
     * @return Property as a String, null if not found or not String type
     */
    
    public String getProperty(String name)
    {
        Object obj = this._properties.get(name);
        
        if (obj != null && obj instanceof String)
            return (String) obj;
        else 
            return null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns all extension points declared in this plugin.
     * @return Array of extension point names
     */
    
    public String[] getExtensionPoints()
    {
        String[] pointArray = new String[0];

        Set keySet = _extensions.keySet();
        int numKeys = keySet.size();
        pointArray = new String[numKeys];
        
        int index = 0;
        Iterator it = keySet.iterator();
        
        while (it.hasNext())
        {
            pointArray[index] = (String) it.next();
            ++index;
        }
            
        return pointArray;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the extension associated with an extension point.
     * @param point Extension point name
     * @return ExtensionInfo associated with that point, or null
     * if not found.
     */
    
    public ExtensionInfo getExtension(String point)
    {
        return (ExtensionInfo) this._extensions.get(point);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the runtime info object associated with this plugin.
     * @return RuntimeInfo instance
     */
    
    public RuntimeInfo getRuntime()
    {
        return this._runtime;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns value associated with key from this info object. 
     * @param key Key object
     * @return Value associated with key, null if nothing found.
     */ 
     
    public Object get(Object key)
    {
        return this._properties.get(key);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns id of plugin.
     * @return plugin unique id
     */
    
    public String getId()
    {
        return this._id;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns parent ConfigurationInfo instance, which was passed in during
     * initialization.  Can be null.  
     * @return Instance of ConfigurationInfo or null
     */
    
    public ConfigurationInfo getParent()
    {
        return this._parent;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that traverses parent tree starting with 
     * parameter searching for an instance of PluginInfo.  If one
     * is found, it is returned.  If root is reached with no
     * match, null is returned.
     * @param child Current info instance whose plugin info we are
     * searching for.
     * @return Matching PluginInfo if found, else false
     */
    
    public static PluginInfo getPluginInfo(ConfigurationInfo child)
    {
        ConfigurationInfo current = child;
        
        while (current != null)
        {
            if (current instanceof PluginInfo)
                return (PluginInfo) current;
            current = current.getParent();
        }
        
        return null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the type of this instance of ConfigurationInfo
     * @return ConfigurationInfo.TYPE_PLUGIN
     */
    
    public int getType()
    {
        return ConfigurationInfo.TYPE_PLUGIN;
    }
    
    //---------------------------------------------------------------------
   
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("Plugin Info:\n").append("  name  = ").
               append(this._name).append("\n  id    = ").append(this._id).
               append("\n  version = ").append(this._version).
               append("\n  desc = ").append(this._desc);
        
        if (this._org != null)      
            buffer.append("\n  org = ").append(this._org);
        
        if (this._runtime != null)
        {
            String runtime = this._runtime.toString();
            buffer.append("\n  runtime = [[").append(runtime).append("]]");
        }
        
        
        if (!this._extensions.isEmpty())
        {
            buffer.append("\n  extensions = ");
            Iterator it = this._extensions.entrySet().iterator();
            int index = 0;
            
            while (it.hasNext())
            {
                String exten = it.next().toString();
                buffer.append("\n   extension ").append(index++).
                       append(" = [[").append(exten).append("]]");
            }
        }
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the location of the plugin.  
     * @param pluginLocation Path to specific plugin directory, which 
     *        must contain plugin.xml
     */
    
    public void setLocation(File pluginLocation)
    {
        this._location = pluginLocation;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns path to plugin.
     * @return Plugin location
     */
    
    public File getLocation()
    {
        return this._location;
    }
    
    //---------------------------------------------------------------------
}
