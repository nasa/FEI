package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import jpl.mipl.mdms.FileService.util.WildcardRegexUtil;


/**
 * <b>Purpose:</b>
 *  File filter for the local file system.  Uses regex-like string
 *  to filter non-directory files.
 *
 *   <PRE>
 *   Copyright 2004, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2004.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who                        What</B>
 * ----------------------------------------------------------------------------
 * 09/04/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahFileFilter.java,v 1.3 2009/07/28 19:22:29 ntt Exp $
 *
 */

public class SavannahFileFilter implements FileFilter
{
    private final String __classname = "SavannahFileFilter";
    
    protected String  _expression;
    private   String  _regExpression;
    protected Pattern _pattern;

    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    
    public SavannahFileFilter()
    {
        setExpression(null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param expression Initial expression used as file filter
     */
    
    public SavannahFileFilter(String expression)
    {
        this();
        setExpression(expression);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the value of the filter expression.
     * @param expression New filter expression, can be null
     */
    
    public void setExpression(String expression)
    {
        if (this._expression == null && expression == null)
            return;
        if (this._expression != null && this._expression.equals(expression))
            return;
        
        this._expression    = expression;
        //this._regExpression = regularize(this._expression);
        this._regExpression = WildcardRegexUtil.wildCardToRegex(
                                        this._expression, true);
        this._pattern       = Pattern.compile(this._regExpression);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the value of the filter expression.
     * @return Filter expression.
     */
    
    public String getExpression()
    {
        return this._expression;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Converts file system wildcard expression to regular expression.
     * Periods (.) are replaced with (\.).
     * Stars (*) are replaced with (.*)
     * Question marks are replaced with (.)
     * Null or empty string is replaced with .*
     * @param string Instance of wildcard expression, can be null
     * @return Associated regular expression.
     */
    
//    public static String regularize(String string)
//    {
//        String regEx  = string;
//        
//        if (string == null || string.equals(""))
//        {
//            regEx = ".*";
//        }
//        else
//        {
//            //replace . with \.
//            regEx = regEx.replaceAll("\\.","\\\\.");
//            
//            //replace * with .*
//            regEx = regEx.replaceAll("\\*","\\.\\*");
//            
//            //replace ? with .
//            regEx = regEx.replaceAll("\\?","\\.");
//        }
//        
//        return regEx;
//    }
    
    //---------------------------------------------------------------------
    
    /**
     * Tests whether or not the specified abstract pathname should be  
     * included in a pathname list.
     * @param pathname The abstract pathname to be tested
     * @return True if and only if pathname should be included
     */
    
    public boolean accept(File pathname)
    {
        if (pathname.isDirectory())
            return true;
        
        if (_expression == null)
            return true;
        
        if (_pattern.matcher(pathname.getName()).matches())
            return true;
        
        return false;
    }

    //---------------------------------------------------------------------
}
