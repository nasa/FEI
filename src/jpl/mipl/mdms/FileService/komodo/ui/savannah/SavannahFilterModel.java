/*
 * Created on Aug 8, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.beans.PropertyChangeSupport;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Model class for registering filters and controlling state and 
 * change notification. Application model is assumed to have a 
 * reference to an instance of this.
 * <BR>
 * Observers of filter state must register themselves to this model
 * for updates on particular filters.  Property name is equal to the
 * filter name, as such, care should be taken when selecting a name
 * for a new filter so that it does not conflict with any other 
 * application properties.
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
 * @version $Id: SavannahFilterModel.java,v 1.2 2013/04/04 21:01:20 ntt Exp $
 *
 */

public class SavannahFilterModel
{

    public static final String PROPERTY_PATTERN = "FILTER_PATTERN";
    public static final String PROPERTY_ENABLED = "FILTER_ENABLED";
    public static final String SEPARATOR = ":";
    protected final Map _filters;
    protected final List _listeners;
    
    
    /** Enables bean property change event handling */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(
                                                                this);
    

    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Creates an empty filter registry.
     */
    
    public SavannahFilterModel()
    {
        this._filters = new Hashtable();
        this._listeners = new Vector();
    }
    
    //---------------------------------------------------------------------

    /**
     * Adds listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            added
     */

    public void addFilterListener(SavannahListFilterListener l)
    {
        if (l != null) {
            synchronized(this._listeners) {
                if (!this._listeners.contains(l))
                    this._listeners.add(l);
            }
        }
    }

    //---------------------------------------------------------------------

    /**
     * Removes listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            removed
     */

    public void removeFilterListener(SavannahListFilterListener l)
    {
        if (l != null) {
            synchronized(this._listeners) {
                if (this._listeners.contains(l))
                    this._listeners.remove(l);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    public void addFilter(SavannahListFilter filter)
    {
        if (filter != null && filter.getName() != null)
        {
            this._filters.put(filter.getName(), filter);
        }
    }
    
    //---------------------------------------------------------------------
    
    protected SavannahListFilter getFilter(String filterName)
    {
        return (SavannahListFilter) this._filters.get(filterName);
    }
    
    //---------------------------------------------------------------------
    
    public boolean isEnabled(String filterName)
    {
        SavannahListFilter filter = getFilter(filterName);
        if (filter == null)
            return false;
        else
            return filter.isEnabled();
    }
    
    //---------------------------------------------------------------------
    
    public String getPattern(String filterName)
    {
        SavannahListFilter filter = getFilter(filterName);
        if (filter == null)
            return null;
        else
            return filter.getPattern();
    }
    
    //---------------------------------------------------------------------
    
    public void setEnabled(String filterName, boolean enabled)
    {
        SavannahListFilter filter = getFilter(filterName);
        if (filter != null && filter.isEnabled() != enabled)
        {            
            filter.setEnabled(enabled);
            fireFilterChange(filter);          
        }        
    }
    
    //---------------------------------------------------------------------
    
    public void setPattern(String filterName, String pattern)
    {
        SavannahListFilter filter = getFilter(filterName);
        if (filter != null && !filter.getPattern().equals(pattern))
        {            
            filter.setPattern(pattern);
            fireFilterChange(filter);    
        }        
    }
    
    //---------------------------------------------------------------------
    
    protected void fireFilterChange(SavannahListFilter filter)
    {
        synchronized(this._listeners)
        {
            Iterator it = this._listeners.iterator();
            while (it.hasNext())
            {
                ((SavannahListFilterListener) it.next()).filterChange(filter);
            }
        }
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    interface SavannahListFilterListener {
        public void filterChange(SavannahListFilter filter);
    }
        
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
}
