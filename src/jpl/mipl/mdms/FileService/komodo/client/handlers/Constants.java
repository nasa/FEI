package jpl.mipl.mdms.FileService.komodo.client.handlers;

/**
* Constants for the KOMODO Hile Handler framework.
*  
*   <PRE>
*   Copyright 2011, California Institute of Technology.
*   ALL RIGHTS RESERVED.
*   U.S. Government Sponsorship acknowledge. 2011.
*   </PRE>
*
* <PRE>
* ============================================================================
* <B>Modification History :</B>
* ----------------------
*
* <B>Date              Who              What</B>
* ----------------------------------------------------------------------------
* 05/31/2011        Nick             Initial Release
* ============================================================================
* </PRE>
*
* @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
* @version $Id: Constants.java,v 1.1 2011/05/31 22:43:50 ntt Exp $
*
*/

public class Constants
{
    /**
     * Location of file handler descriptor file within Jar package
     */
    public static final String HANDLER_DESCRIPTOR_PATH = "META-INF/services/komodo.filehandler.xml";
    
    /**
     * Filename of a file handler manifest listing the selected 
     * file handlers to run.
     */
    public static final String HANDLER_FILTERS_LIST_NAME = ".komodo.filehandler.manifest";
    
    /**
     * Property name for which the value will be a file handler manifest 
     * listing the selected file handlers to run.
     */
    public static final String HANDLER_FILTERS_LIST_PROPERTY = "komodo.filehandler.manifest";
}
