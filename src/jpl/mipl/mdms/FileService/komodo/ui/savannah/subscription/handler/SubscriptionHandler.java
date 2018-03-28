/*
 * Created on Feb 15, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler;

import java.util.List;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.HandlerInfo;


/**
 * <b>Purpose:</b>
 * Interface for SubscriptionHandlers.  Contains <code>handleEvent()</code>
 * for handler behavior and <code>initialize(Element)</code> to initialize
 * state from a DOM Element.  This stems from the design of the plug-in
 * architecture that specifies implementations of this interface and 
 * required attributes in an XML file.
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
 * 02/15/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionHandler.java,v 1.3 2005/03/29 03:01:52 ntt Exp $
 *
 */

public interface SubscriptionHandler
{
    /** Name of the name attribute */
    public final static String ATTR_NAME    = "name";
    
    /** Name of the version attribute */
    public final static String ATTR_VERSION = "version";
    
    /** Name of the descr (description) attribute */
    public final static String ATTR_DESC    = "desc";
    
    /** Name of the class attribute */
    public static final String ATTR_CLASS   = "class";
    
    /** Name of the element defining a handler plugin */
    public static final String ELEMENT_HANDLER_NAME    = "Handler";
    
    /** Name of the element defining a handler properties */
    public static final String ELEMENT_PROPERTIES_NAME = "properties";
    
    /** Name of the element defining a handler property */
    public static final String ELEMENT_PROPERTY_NAME = "property";
    
    /** Name of the attribute defining a handler property keyword */
    public static final String PROPERTY_KEYWORD = "keyword";
    
    /** Name of the attribute defining a handler property value */
    public static final String PROPERTY_VALUE = "value";
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the name of this handler 
     * @return Handler name
     */
    
    public String getName();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the classname of this handler 
     * @return Handler classname
     */
    
    public String getClassname();
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the id of this handler 
     * @return Handler id
     */
    
    public String getId();
    
    //---------------------------------------------------------------------
    
    /**
     * Handle method performs operation of the handler.  Implementors
     * can use the event type as a filter so that handler is only
     * invoked for specific event types.
     * @param taskType Type of task, one of 
     *            SubscriptionConstants.TASK_{SUBSCRIPTION,NOTIFICATION}
     * @param results List of Result objects 
     */
    
    public void handleEvent(int taskType, List results);
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes handler from contents of a DOM element.
     * @param into Handler configuration info object
     * @throws Exception if element is null is does not contain required
     *         attributes.
     */
    
    public void initialize(HandlerInfo info) throws Exception;

    //---------------------------------------------------------------------
    
    /**
     * Sets the reference to the client object associated with this handler.
     * @param client Current client object
     */
    
    public void setClient(Client client);
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the reference to the client object associated with this handler.
     * @param client Current client object
     */
    
    public void setAppModel(SavannahModel model);
    
    //---------------------------------------------------------------------
    
    public void destroy();
    
    //---------------------------------------------------------------------
}
