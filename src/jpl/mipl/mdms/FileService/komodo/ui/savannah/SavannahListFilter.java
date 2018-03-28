/*
 * Created on Aug 8, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

/**
 * <b>Purpose:</b>
 * Filter class for Savannah file lists, both local and remote.
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
 * 08/08/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahListFilter.java,v 1.2 2006/05/06 00:21:31 ntt Exp $
 *
 */

public class SavannahListFilter
{
    public static final String FILTER_ALL = "*";
    
    protected String  _filter;    
    protected boolean _enabled;
    protected String  _name;
    
    //---------------------------------------------------------------------
    
    public SavannahListFilter(String name)
    {    
        this(name, FILTER_ALL, false);
    }
    
    //---------------------------------------------------------------------
    
    public SavannahListFilter(String name, String filter, boolean enabled)
    {
        this._name    = name;
        this._filter  = filter;
        this._enabled = enabled;
    }
    
    //---------------------------------------------------------------------
    
    public String getPattern()
    {
        return this._filter;
    }
    
    //---------------------------------------------------------------------
    
    public void setPattern(String pattern)
    {
        if (pattern == null)
            pattern = FILTER_ALL;
        this._filter = pattern;
    }
    
    //---------------------------------------------------------------------
    
    public boolean isEnabled()
    {
        return this._enabled;
    }
    
    //---------------------------------------------------------------------
    
    public void setEnabled(boolean enable)
    {
        this._enabled = enable;
    }
    
    //---------------------------------------------------------------------
    
    public String getName()
    {
        return this._name;
    }
    
    //---------------------------------------------------------------------
}
