package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.SubscriptionConstants;

/**
 * <B>Purpose:</B>
 * Utility class that converts between maps of strings and maps
 * of objects based on knowledge of type via property name.
 * 
 * Conversion from string to object entails iterating over pairs
 * and calling <code>SubscriptionConstants.wrapValue()</code> to
 * instantiate the appropriate type.
 * 
 * Conversion from object to string relies on the implementation
 * of <code>toString</code> for the object.
 * 
 * <PRE>
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
 * 10/26/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MetaParametersParseUtil.java,v 1.3 2008/11/03 19:30:40 ntt Exp $
 */

public class MetaParametersParseUtil
{
    
    //---------------------------------------------------------------------
    
    public static Map<String, Object> fromStrings(Properties stringToStringMap)
    {
        Map<String, Object> valuesMap = new Hashtable<String, Object>();
        String key;
        String val;
        Object value;
        
        Iterator it = stringToStringMap.keySet().iterator();
        while (it.hasNext())
        {
            key = (String) it.next();
            val = (String) stringToStringMap.get(key);
            value = SubscriptionConstants.wrapValue(key, val);
            valuesMap.put(key, value);
        }        
        return valuesMap;        
    }
    
    //---------------------------------------------------------------------    

    public static Properties toStrings(Map stringToObjectMap)
    {
        Properties stringMap = new Properties();

        String key;
        Object val;
        String value;
        Iterator it = stringToObjectMap.keySet().iterator();
        while (it.hasNext())
        {
            key = (String) it.next();
            val = stringToObjectMap.get(key);
            value = val.toString();
            stringMap.put(key, value);
        }        
        return stringMap;        
    }
    
    //---------------------------------------------------------------------
}
