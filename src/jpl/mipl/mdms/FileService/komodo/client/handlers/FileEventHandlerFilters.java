package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Data structure for filtering file handlers.
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
* @version $Id: FileEventHandlerFilters.java,v 1.2 2011/06/01 20:52:16 ntt Exp $
*
*/

public class FileEventHandlerFilters
{
    /**
     * No filtering applied, all entries pass
     */
    public static final int FILTER_NONE = 0;
    
    /**
     * Ultimate filtering applied, entries shall not pass!
     */
    public static final int FILTER_ALL  = 1;
    
    /**
     * Only entries existing in id list may pass
     */
    public static final int FILTER_LIST = 2;
    
    //---------------------------------------------------------------------
    
    protected List<String> _ids;
    protected int _filterMode;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor, defaults to filter everything
     */
    public FileEventHandlerFilters()
    {
        this(FILTER_ALL);
    }

    //---------------------------------------------------------------------
    
    /**
     * Constructor, using mode pass in as argument
     * @param mode One of FILTER_NONE, FILTER_ALL, or FILTER_LIST
     */
    public FileEventHandlerFilters(int mode)
    {
        this._filterMode = mode;
    } 
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor which takes a list of ids as initial list, sets
     * mode to FILTER_LIST
     */
    public FileEventHandlerFilters(List<String> list)
    {        
        this(FILTER_LIST);
        
        this._ids = new ArrayList<String>();
        if (list != null)
            this._ids.addAll(list);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Given a list of discovered ids, apply filter and return new 
     * list of ids that are considered to have passed.
     * @param input Incoming ids list
     * @return Filtered id list
     */
    public List<String> filterList(List<String> input)
    {
        if (input == null)
            return null;
        
        List<String> output = new ArrayList<String>();
        
        if (this._filterMode == FILTER_NONE)
        {
            output.addAll(input);
        }
        else if (this._filterMode == FILTER_LIST)
        {
            if (this._ids != null)
            {
                Iterator<String> inIt = input.iterator();
                while (inIt.hasNext())
                {
                    String entry = inIt.next();                   
                    if (this._ids.contains(entry))
                        output.add(entry);
                }
            }
        }
        else //FILTER_ALL
        {
            //do nothing, leave output empty
        }
        
        return output;
    }

    //---------------------------------------------------------------------
    
    /**
     * Tests if id parameter passes the filter (true) or not (false).
     * @param id Id to be tested
     * @return True if passes, false otherwise
     */
    
    public boolean doesIdPass(String id)
    {
        boolean passes = false;
        
        if (this._filterMode == FILTER_NONE)
            passes = true;
        else if (this._filterMode == FILTER_LIST && this._ids != null)
            passes = this._ids.contains(id);
        
        return passes;            
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns a list of ids contained within the filter list
     * @return List of filter ids
     */
    public List<String> getFilterIds()
    {
        List list = new ArrayList();
        list.addAll(this._ids);
        
        return list;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a filter id to the set.  If mode was not FILTER_LIST, it will be
     * after calling this method.
     * @param id New handler id to be added.
     */
    
    public void addFilterId(String id)
    {
        if (id != null && !id.equals(""))
        {
            if (this._ids == null)
            {
                this._ids = new ArrayList();
                this._filterMode = FILTER_LIST;
            }
            
            if (!this._ids.contains(id))
            {
                this._ids.add(id);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Set the filter mode for this instance.  Note: Setting to 
     * FILTER_LIST mode without specifying any ids via addFilterId()
     * may result in same behavior as FILTER_ALL.
     * @param mode One of FILTER_NONE, FILTER_ALL, FILTER_LIST
     */
    
    public void setMode(int mode)
    {
        if (mode == this._filterMode)
            return;
        
        if (mode == FILTER_NONE || mode == FILTER_ALL ||
            mode == FILTER_LIST )
        {
            this._filterMode = mode;
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the filter mode of this instance
     * @return filter mode
     */
    
    public int getMode()
    {
        return this._filterMode;
    }
    
    //---------------------------------------------------------------------
    
}
