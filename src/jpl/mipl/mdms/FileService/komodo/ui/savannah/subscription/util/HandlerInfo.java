package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.util.Enumeration;
import java.util.Properties;


import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * <b>Purpose:</b>
 * Extension information for a handler.  Contains information on handler
 * name, id, and associated class.  Can also store properties that will
 * be used during handler initialization.
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
 * @version $Id: HandlerInfo.java,v 1.3 2008/09/05 19:09:42 ntt Exp $
 *
 */

public class HandlerInfo implements ConfigurationInfo
{       
    public static final String PROPERTY_CLASS = "class"; //required
    public static final String PROPERTY_NAME  = "name";  //required
    public static final String PROPERTY_ID    = "id";    //required 
 
    protected String _name, _classname, _id;
    
    /** Reference to parent info object */
    protected ConfigurationInfo _parent;
    
    /** Handler properties */
    protected Properties _properties;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     */
    
    public HandlerInfo()
    {
        this._properties = new Properties();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initialize instance from JDOM Element.
     * @param element JDOM element containing state
     * @param parent Reference to parent config info instance
     */
    
    public void initialize(Element element, ConfigurationInfo parent) throws Exception
    {
        this._parent = parent;
        
        //get name (required)
        Attribute attr = element.attribute(PROPERTY_NAME);
        if (attr == null)
            throw new Exception("No "+PROPERTY_NAME+" attribute found in '" + 
                                element.getName() + "' element.");
        this._name = attr.getValue();
        
        //get class (required)
        attr = element.attribute(PROPERTY_CLASS);
        if (attr == null)
            throw new Exception("No "+PROPERTY_CLASS+" attribute found in '" + 
                                element.getName() + "' element.");
        this._classname = attr.getValue();
        
        //check for id (required)
        attr = element.attribute(PROPERTY_ID);
        if (attr == null)
            throw new Exception("No "+PROPERTY_ID+" attribute found in '" + 
                                element.getName() + "' element.");
        this._id = attr.getValue();
        
        //load properties for this handler info
        this._properties = PluginLoader.extractProperties(element);
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Return reference to plugin info for this handler.
     * @return Instance of PluginInfo
     */

    public ConfigurationInfo getParent()
    {
        return this._parent;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get value associated with property
     * @param name Property name
     * @return Value of property, null if no value.
     */
    
    public String getProperty(String name)
    {
        return this._properties.getProperty(name, null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Empty implementation.
     * @param key Lookup object
     * @return null
     */
    
    public Object get(Object key)
    {
        return null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the type of this instance of ConfigurationInfo
     * @return ConfigurationInfo.TYPE_HANDLER
     */
    
    public int getType()
    {
        return ConfigurationInfo.TYPE_HANDLER;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Return class name of handler.
     * @return class name
     */
    
    public String getClassname()
    {
        return this._classname;
    }

    //---------------------------------------------------------------------
    
    /**
     * Return name of handler
     * @return handler name
     */
    
    public String getName()
    {
        return this._name;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Return id of handler
     * @return handler id
     */
    
    public String getId()
    {
        return this._id;
    }
    
    //---------------------------------------------------------------------
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("Handler Info:\n").append("  name  = ").
               append(this._name).append("\n  id    = ").append(this._id).
               append("\n  class = ").append(this._classname);
        
        if (!this._properties.isEmpty())
        {
            buffer.append("\n  properties = ");
            Enumeration e = this._properties.propertyNames();
            while (e.hasMoreElements())
            {
                String prop = (String) e.nextElement();
             
                buffer.append("\n   ").append(prop).append(" = ").
                       append(this._properties.getProperty(prop));
            }
        }
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
}
