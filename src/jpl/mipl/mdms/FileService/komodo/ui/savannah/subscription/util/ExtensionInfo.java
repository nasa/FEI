/*
 * Created on Mar 1, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * <b>Purpose:</b>
 * Wrapper info object for plugin extensions.  Defined for an extension
 * point, maintains a list of extension behavior.
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
 * @version $Id: ExtensionInfo.java,v 1.3 2008/09/05 19:09:42 ntt Exp $
 *
 */

public class ExtensionInfo implements ConfigurationInfo
{
    public static final String ATTR_POINT = "point";
    
    protected String _extensionPoint;
    protected List   _extensions;
    protected ConfigurationInfo _parent;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    
    public ExtensionInfo()
    {
        this._extensions = new ArrayList();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initialize instance from JDOM Element.
     * @param element JDOM element containing state
     * @param parent Reference to parent config info instance
     */
    
    public void initialize(Element element, ConfigurationInfo parent)
                           throws Exception
    {
        this._parent = parent;
        
        //get extension point
        Attribute attr = element.attribute(ATTR_POINT);
        if (attr == null)
            throw new Exception("No "+ATTR_POINT+" attribute found in '" + 
                                element.getName() + "' element.");
        this._extensionPoint = attr.getValue();
        
        this._extensions = ExtensionFactory.buildExtensions(element, this);
    }
    
    //---------------------------------------------------------------------
    
    public ConfigurationInfo getParent()
    {
        return this._parent;
    }
    
    //---------------------------------------------------------------------
    
    public String getExtensionPoint()
    {
        return this._extensionPoint;
    }
    
    //---------------------------------------------------------------------
    
    public ConfigurationInfo[] getExtensions()
    {
        int size = this._extensions.size();
        ConfigurationInfo[] infos = new ConfigurationInfo[size];
        
        for (int i = 0; i < size; ++i)
            infos[i] = (ConfigurationInfo) this._extensions.get(i);
        
        return infos;   
    }
    
    //---------------------------------------------------------------------
    
    public Object get(Object key)
    {
        return null;
    }
    
    //---------------------------------------------------------------------
    
    public int getType()
    {
        return ConfigurationInfo.TYPE_EXTENSION;
    }

    //---------------------------------------------------------------------
    
    /**
     * Convenience method that traverses parent tree starting with 
     * parameter searching for an instance of ExtensionInfo.  If one
     * is found, it is returned.  If root is reached with no
     * match, null is returned.
     * @param child Current info instance whose plugin info we are
     * searching for.
     * @return Matching ExtensionInfo if found, else false
     */
    
    public static ExtensionInfo getExtensionInfo(ConfigurationInfo child)
    {
        ConfigurationInfo current = child;
        
        while (current != null)
        {
            if (current instanceof ExtensionInfo)
                return (ExtensionInfo) current;
            current = current.getParent();
        }
        
        return null;
    }
    
    //---------------------------------------------------------------------
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("Extension Info:\n").append("  point  = ").
               append(this._extensionPoint);
        
        if (!this._extensions.isEmpty())
        {
            buffer.append("\n  extensions = ");
            Iterator it = this._extensions.iterator();
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
    
}
