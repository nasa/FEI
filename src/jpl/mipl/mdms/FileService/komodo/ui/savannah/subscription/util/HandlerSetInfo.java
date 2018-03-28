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
 * Configuration information for a handler set, which registers
 * a set of handlers for a given event type.
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
 * @version $Id: HandlerSetInfo.java,v 1.3 2008/09/05 19:09:42 ntt Exp $
 *
 */

public class HandlerSetInfo implements ConfigurationInfo
{
    public static final String ATTR_EVENT_TYPE = "eventType";
    public static final String ATTR_NAME       = "name";

    public static final String ELEMENT_HANDLER = "handler";
    
    protected String _eventType;
    protected String _name;
    
    //parent should be plugin info?
    protected ConfigurationInfo _parent;
    
    protected List _handlers;

    //---------------------------------------------------------------------
    
    public HandlerSetInfo()
    {
        this._handlers = new ArrayList();
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
        
        //get event type attribute
        Attribute attr = element.attribute(ATTR_EVENT_TYPE);
        if (attr == null)
            throw new Exception("No "+ATTR_EVENT_TYPE+" attribute found in '" + 
                                element.getName() + "' element.");
        this._eventType = attr.getValue();
        
        //get name
        attr = element.attribute(ATTR_NAME);
        if (attr == null)
            throw new Exception("No "+ATTR_NAME+" attribute found in '" + 
                                element.getName() + "' element.");
        this._name = attr.getValue();
        
        List children = element.elements(ELEMENT_HANDLER);
        int numChildren = children.size();
        
        for (int i = 0; i < numChildren; ++i)
        {
            Element child = (Element) children.get(i);
            HandlerInfo info = new HandlerInfo();
            info.initialize(child, this);
            _handlers.add(info);
        }
    }
    
    //---------------------------------------------------------------------
    
    public String getEventType()
    {
        return this._eventType;
    }
    
    //---------------------------------------------------------------------
    
    public String getName()
    {
        return this._name;
    }
    
    //---------------------------------------------------------------------
    
    public HandlerInfo[] getHandlers()
    {
        int size = this._handlers.size();
        HandlerInfo[] infos = new HandlerInfo[size];
        
        for (int i = 0; i < size; ++i)
            infos[i] = (HandlerInfo) this._handlers.get(i);
        
        return infos;   
    }
    
    //---------------------------------------------------------------------
    
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
     * Returns the type of this instance of ConfigurationInfo
     * @return ConfigurationInfo.TYPE_HANDLER_SET
     */
    
    public int getType()
    {
        return ConfigurationInfo.TYPE_HANDLER_SET;
    }
    
    //---------------------------------------------------------------------
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("Handler Set Info:\n").append("  name  = ").
               append(this._name).append("\n  event type = ").
               append(this._eventType);
        
        if (!this._handlers.isEmpty())
        {
            buffer.append("\n  handlers = ");
            int index = 0;
            
            Iterator it = this._handlers.iterator();
            while (it.hasNext())
            {
                String hand = it.next().toString();
             
                buffer.append("\n   handler ").append(index++).
                       append(" = [[").append(hand).append("]]");
            }
        }
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
}
