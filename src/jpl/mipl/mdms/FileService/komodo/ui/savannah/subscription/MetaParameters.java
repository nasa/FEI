/*
 * Created on Jan 10, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.util.Map;


/**
 * <b>Purpose:</b>
 * Interface for meta-subscription parameters.
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
 * 01/10/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MetaParameters.java,v 1.3 2008/10/28 19:00:34 ntt Exp $
 *
 */

public interface MetaParameters
{
    //---------------------------------------------------------------------
    
    /**
     * Returns value bound to parameter 
     * @param parameterName
     * @return Value of parameter, null if not-set or unknown
     */
    
    public Object get(String parameterName);
    
    //---------------------------------------------------------------------
    
    /**
     * Sets value bound to parameter 
     * @param parameterName Parameter name
     * @param value Object to be associated as parameter value
     */
    
    public void set(String parameterName, Object value);
    
    //---------------------------------------------------------------------
    
    /**
     * Sets values based on contents of parameter 
     * @param mappedParameters Map of parameter names and values
     */
    
    public void setAll(Map mappedParameters);
    
    //---------------------------------------------------------------------
    
    
    /**
     * Returns a copy of the map that maintains the parameter values 
     * @param return Copy of map of parameter names and values
     */
    
    public Map getAll();
    
    //---------------------------------------------------------------------
}
