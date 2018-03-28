/*
 * Created on Mar 1, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import org.dom4j.Element;

/**
 * <b>Purpose:</b>
 * Interface for configuration information objects. Implementors specify
 * attributes of plugins, handlers, and other extesions.
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
 * 08/01/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ConfigurationInfo.java,v 1.3 2008/09/05 19:09:42 ntt Exp $
 *
 */

public interface ConfigurationInfo
{
    //---------------------------------------------------------------------
    
    public static final int TYPE_PLUGIN      = 1;
    
    public static final int TYPE_EXTENSION   = 2;
    
    public static final int TYPE_HANDLER_SET = 3;
    
    public static final int TYPE_HANDLER     = 4;
    
    public static final int TYPE_RUNTIME     = 5;
    
    //---------------------------------------------------------------------
    
    /**
     * Initialize instance from JDOM Element.
     * @param element JDOM element containing state
     * @param parent Reference to parent config info instance
     */
    
    public void initialize(Element element, ConfigurationInfo parent) 
                                                    throws Exception;
    
    //---------------------------------------------------------------------
    
    
    /**
     * Get value associated with parameter key
     * @param key Key object
     * @return Object associated with key, null if nothing found.
     */
    
    public Object get(Object key);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns parent ConfigurationInfo instance, which was passed in during
     * initialization.  Can be null.  
     * @return Instance of ConfigurationInfo or null
     */
    
    public ConfigurationInfo getParent();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the type of this instance of ConfigurationInfo
     * @return configuration info type
     */
    
    public int getType();
    
    //---------------------------------------------------------------------
}
