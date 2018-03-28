/*
 * Created on Jul 12, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.util;

import java.util.LinkedList;
import java.util.List;

import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QConstants;

public class Utils
{
    //---------------------------------------------------------------------

    /**
     * Converts a list of Result objects to an array of associated
     * filenames by using the result.getName() method.  Ordering
     * between list and array is maintained.
     * @param results Results list
     * @return Array of result filenames 
     */

    public static String[] getResultNames(List<Result> results)
    {
        String[] names = new String[0];
        List<String> nameList = new LinkedList<String>();
        for (Result result : results)
        {
            String name = result.getName();
            if (name != null)
                nameList.add(name);
        }

        names = (String[]) nameList.toArray(names);

        return names;
    }

    //---------------------------------------------------------------------

    /**
     * Convenience method that returns true if the "bundle results"
     * property has been set.
     * @return True if the associated property has been set, false otherwise.
     */

    public static boolean bundleResultsByFiletype()
    {
        boolean bundle = (System.getProperty(QConstants.PROP_BUNDLE_RESULTS) != null);
        return bundle;
    }
    
    // ---------------------------------------------------------------------

}
