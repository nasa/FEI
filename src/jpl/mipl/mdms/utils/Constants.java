/*
 * Created on Jul 24, 2006
 */
package jpl.mipl.mdms.utils;

/**
 * <b>Purpose:</b>
 * Interface to define common constants for the MDMS framework.
 * 
 *   <PRE>
 *   Copyright 2006, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2006.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 07/24/2006        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: Constants.java,v 1.2 2006/12/12 01:51:04 ntt Exp $
 *
 */

public interface Constants
{
    /** Path to the keystore file used for SSL communication */
    public static final String PROPERTY_SSL_TRUSTSTORE   = "javax.net.ssl.trustStore";
    
    /** Logging configuration file. Controls logger verbosity and targets. */
    public static final String PROPERTY_LOGGING_CONFIG   = "mdms.logging.config";
    
    /** Configures logging service to periodically check for changes to the 
        configuration file. */
    public static final String PROPERTY_LOG_CONFIG_DELAY = "mdms.logging.config.delay";
    
    /** DOM document builder factory implementation class. */
    public static final String PROPERTY_XML_DOM_FACTORY  = "javax.xml.parsers.DocumentBuilderFactoryImpl";
    
    /** SAX parser factory implementation class. */
    public static final String PROPERTY_XML_SAX_FACTORY  = "javax.xml.parsers.SAXParserFactory";
    
    /** SAX parser driver implementation class */
    public static final String PROPERTY_XML_SAX_DRIVER   = "org.xml.sax.driver";
    
    /** Identifies the launching application name for messages. */
    public static final String PROPERTY_USER_APPLICATION = "mdms.user.application";
    
    /** Identifies the launching operation */
    public static final String PROPERTY_USER_OPERATION   = "mdms.user.operation";
    
    /** Enables debug message. */
    public static final String PROPERTY_ENABLE_DEBUG     = "mdms.enable.debug";
    
    /** Sets preference for IPv4 Stack for network. */
    public static final String PROPERTY_PREFER_IPV4      = "java.net.preferIPv4Stack";
    
    /** Sets upper limit of asynchronous system process invocations */
    public static final String PROPERTY_ASYNC_INVOKE_LIMIT  = "mdms.invoke.async.limit";
}
