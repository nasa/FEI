package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * <b>Purpose:</b>
 * Wrapper info object for runtime information. Contains information
 * such as which libraries are used to defined plugin.
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
 * 03/03/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: RuntimeInfo.java,v 1.4 2008/09/05 19:09:42 ntt Exp $
 *
 */

public class RuntimeInfo implements ConfigurationInfo
{

    public static final String ELEMENT_LIBRARY = "library";
    public static final String ELEMENT_IMPORT  = "import";
    public static final String ATTR_NAME       = "name";
    public static final String ATTR_PLUGIN     = "plugin";
    
    protected ConfigurationInfo _parent;
    
    protected List _libraries;
    protected List _imports;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    
    public RuntimeInfo()
    {
        this._libraries = new ArrayList();
        this._imports   = new ArrayList();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initialize instance from JDOM Element.
     * @param element JDOM element for runtime
     * @param parent Reference to parent config info instance
     */
    
    public void initialize(Element element, ConfigurationInfo parent)
                                                    throws Exception
    {
        this._parent = parent;
        
        initLibraries(element);
        initImports(element);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Extracts library information from runtime element
     * @param element Runtime JDOM element
     */
    
    protected void initLibraries(Element element)
    {
        List children = element.elements(ELEMENT_LIBRARY);
        int numChildren = children.size();
        
        Element child;
        Attribute attr;
        String lib;
        
        for (int i = 0; i < numChildren; ++i)
        {
            child = (Element) children.get(i);
            
            attr = child.attribute(ATTR_NAME);
            
            if (attr == null)
                continue;
            
            lib = attr.getValue();
            if (lib != null)
            {
                this._libraries.add(lib);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Extracts dependency information from runtime element
     * @param element Runtime JDOM element
     */
    
    protected void initImports(Element element)
    {
        List children = element.elements(ELEMENT_IMPORT);
        int numChildren = children.size();
        
        Element child;
        Attribute attr;
        String plugin;
        
        for (int i = 0; i < numChildren; ++i)
        {
            child = (Element) children.get(i);
            
            attr = child.attribute(ATTR_PLUGIN);
            
            if (attr == null)
                continue;
            
            plugin = attr.getValue();
            if (plugin != null)
            {
                this._imports.add(plugin);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Empty implementation. 
     * @return null
     */
    
    public Object get(Object key)
    {
        return null;
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
     * Returns array of library names that are required for this plugin.
     * These libraries are collected and loaded by a class loader.
     * Objects used as part of the plugin are then accessible.
     * @return String array of library names
     */
    
    public String[] getLibraries()
    {
        int numLibs = this._libraries.size();
        String[] libs = new String[numLibs];
     
        for (int i = 0; i < numLibs; ++i)
        {
            libs[i] = (String) this._libraries.get(i);
        }
        
        return libs;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns array of plugin ids that are required for this plugin.
     * These libraries are collected and loaded by a class loader.
     * @return String array of plugin ids
     */
    
    public String[] getImports()
    {
        int numPlugins = this._imports.size();
        String[] plugins = new String[numPlugins];
     
        for (int i = 0; i < numPlugins; ++i)
        {
            plugins[i] = (String) this._imports.get(i);
        }
        
        return plugins;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the type of this instance of ConfigurationInfo
     * @return ConfigurationInfo.TYPE_RUNTIME
     */
    
    public int getType()
    {
        return TYPE_RUNTIME;
    }
    
    //---------------------------------------------------------------------
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("Runtime Info:\n");
        
        if (!this._libraries.isEmpty())
        {
            buffer.append("\n  libraries = ");
            int index = 0;
            
            Iterator it = this._libraries.iterator();
            while (it.hasNext())
            {
                String lib = it.next().toString();
             
                buffer.append("\n   library ").append(index++).
                       append(" = [[").append(lib).append("]]");
            }
        }
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
}
