/*
 * Created on Jul 18, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.api;

/**
 * 
 * <b>Purpose:</b>
 * Constants used by the FeiQ Service API
 * 
 *   <PRE>
 *   Copyright 2007, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2007.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 07/18/2007        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QConstants.java,v 1.5 2007/09/13 17:50:59 ntt Exp $
 *
 */

public class QConstants
{
    public static final long NO_ID = 0L;
    
    /** Null id */
    public static final String EMPTY_ID = "";
    
    //---------------------------------------------------------------------
    //Client-side properties
    
    /** Specifies the implementation of QServiceProxy the client should use */
    public static final String PROP_SERVICE_PROXY_IMPL = "komodo.query.service.proxy.impl";
    
    /** Specifies the location of the Fei5 Query Service, as a URL */
    public static final String PROP_SERVICE_LOCATION = "komodo.query.service.location";
    
    /** Specifies the location query file containing user query */
    public static final String PROP_USER_QUERY_FILE = "komodo.query.user.file";
    
    /** If set, client query client will bundle results by filetype */
    public static final String PROP_BUNDLE_RESULTS = "komodo.query.bundle.results";
    
    //---------------------------------------------------------------------
    //Server-side properties
    
    /** Specifies the location of the server-side configuration file */
    public static final String QUERY_SERVICE_CONFIG_FILE = "query-service.properties";
    
    /** Specifies the location of the server-side configuration file */
    public static final String PROP_QUERY_SERVICE_CONFIG_PATH = "komodo.query.service.config";
    
    /** Specifies the max number of results retrieved from search service */
    public static final String PROP_MAX_RESULT_SIZE = "komodo.query.service.max.result.sizes";    
    
    /** Default max result size */
    public static final int DEFAULT_MAX_RESULT_SIZE = 100;      
    
    /** Specifies the implementation of QSearchService the server should use */
    public static final String PROP_SEARCH_SERVICE_IMPL = "komodo.query.service.search.impl";
    
    /** Specifies the location of the QSearchService, as a URL */
    public static final String PROP_SEARCH_SERVICE_LOCATION = "komodo.query.service.search.location";
    
    /** Specifies the location of the QSearchService configuration file */
    public static final String PROP_SEARCH_SERVICE_CONFIG = "komodo.query.service.search.config";
    
    /** Specifies the number of seconds to wait between subscription queries */
    public static final String PROP_SLEEP_PERIOD = "komodo.query.service.sleep.period";

    /** Specifies the location of the Authentication service, as a URL */
    public static final String PROP_AA_SERVICE_LOCATION = "komodo.query.service.aa.location";
    
    //---------------------------------------------------------------------
    //Service-side,Http-Session properties
    
    /** Attribute name for the session transaction id, maintained by HttpSession */
    public static final String PROP_SESSION_ID = "komodo.query.session.id";
    
    /** Attribute name for the reference to main application, maintained by HttpSession */
    public static final String PROP_SERVICE_REFERENCE = "komodo.query.session.service.main.ref";
    
    /** Attribute name for the user name, maintained by HttpSession */
	public static final String PROP_SESSION_USER = "komodo.query.session.user";
	
	/** Attribute name for the server group, maintained by HttpSession */
	public static final String PROP_SESSION_SERVERGROUP = "komodo.query.session.servergroup";
	
	/** Attribute name for the user ip, maintained by HttpSession */
	public static final String PROP_SESSION_USER_IP = "komodo.query.session.user.ip";
	
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
    
    
}

