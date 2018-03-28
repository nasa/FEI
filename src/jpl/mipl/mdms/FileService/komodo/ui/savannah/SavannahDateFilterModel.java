/*
 * Created on Aug 8, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Model class for modeling date filter and controlling state and 
 * change notification. Application model is assumed to have a 
 * reference to an instance of this.
 * <BR>
 * Observers of date state must register themselves to this model
 * for updates.  Property name is equal to the
 * filter name, as such, care should be taken when selecting a name
 * for a new filter so that it does not conflict with any other 
 * application properties.
 * 
 *   <PRE>
 *   Copyright 2013, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2013.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 04/08/2013        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahDateFilterModel.java,v 1.2 2013/04/04 21:01:20 ntt Exp $
 *
 */

public class SavannahDateFilterModel
{            
    DateFilter _active;
    
    List<Offset> _offsetList;
    
    int _offsetId;
    Date _startDate;
    Date _endDate;
    
    protected int  DEFAULTOFFSETID = 0;
    
    protected long DEFAULTRANGEDAY = 7;
    protected long DEFAULTRANGEMS  = 1000 * 60 * 60 * 24 * DEFAULTRANGEDAY;
    protected final static long MILLISPERDAY  = 1000 * 60 * 60 * 24;
    
    
    protected final List<SavannahDateFilterListener> _listeners;
    
    
    /** Enables bean property change event handling */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(
                                                                this);
    
    /** Reference to logger object */
    private Logger _logger = Logger.getLogger(SavannahDateFilterModel.class.getName());
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  
     */
    
    public SavannahDateFilterModel()
    {
        this._listeners = new Vector<SavannahDateFilterListener>();
        
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {        

        this._active = new DateFilter();

        this._offsetList = new ArrayList<Offset>();
        
        this._offsetList.add(new Offset(" 1 day ", getOffsetForDays(1)));
        this._offsetList.add(new Offset(" 7 days", getOffsetForDays(7)));
        this._offsetList.add(new Offset("10 days", getOffsetForDays(10)));
        this._offsetList.add(new Offset("30 days", getOffsetForDays(30)));
        this._offsetList.add(new Offset("60 days", getOffsetForDays(60)));        
        this._offsetId = DEFAULTOFFSETID;
        
       
        Offset offset = this.getOffset(this._offsetId);
        long currentTime = System.currentTimeMillis();
        Date start = new Date(currentTime - offset.getOffset());
        Date end   = new Date(currentTime);
        
        this._startDate = roundDate(start, false);        
        this._endDate   = roundDate(end,   true);
        this._startDate = roundDate(end,   false);    
        
    }
    

    //---------------------------------------------------------------------

    /**
     * Returns the number of milliseconds associated with a number
     * of days.
     * @param dayCount number of days
     * @return Number of milliseconds in those days
     */
    
    public static long getOffsetForDays(int dayCount)
    {
        if (dayCount > 0)
        {
            return (MILLISPERDAY * dayCount);
        }
        else
        {
            return 0;
        }
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns the number of milliseconds associated with a number
     * of days.
     * @param dayCount number of days
     * @return Number of milliseconds in those days
     */
    
    public static int getDaysForOffset(long offsetMs)
    {
        if (offsetMs < 0L)
        {
            return 0;
        }
        else
        {
            return (int) ((offsetMs + 1) / MILLISPERDAY);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Rounds date to either midnight of the date (if rounding backward),
     * or to 23:59:59 (if rounding forward).
     * @param date Current date to round
     * @param roundForward True to round forward, false to round backward.
     * @return Rounded date.
     */
    
    protected static Date roundDate(Date date, boolean roundForward)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        if (roundForward)
        {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE,      59);
            cal.set(Calendar.SECOND,      59);
        }
        else
        {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE,      0);
            cal.set(Calendar.SECOND,      0);
        }
        
        return cal.getTime();
    }
    
    
    
    //---------------------------------------------------------------------

    /**
     * Adds listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            added
     */

    public void addFilterListener(SavannahDateFilterListener l)
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

    public void removeFilterListener(SavannahDateFilterListener l)
    {
        if (l != null) {
            synchronized(this._listeners) {
                if (this._listeners.contains(l))
                    this._listeners.remove(l);
            }
        }
    }
    
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------

    /**
     * Adds listener for property change of model.
     * @param l Object implementing the PropertyChangeListener interface to be
     *            added
     */

    public void fireFilterListenerEvent()
    {
        synchronized(this._listeners) {
            Iterator<SavannahDateFilterListener> it =
                            this._listeners.iterator();
            while (it.hasNext())
                it.next().filterChanged(this._active);
        }
    }
        
    //---------------------------------------------------------------------
    
    public DateFilter getActive()
    {
        return this._active;
    }
    
    
    public void setActive(DateFilter newFilter)
    {
        if (this._active.equals(newFilter))
            return;
        
        _active = newFilter;
        fireFilterListenerEvent();
        
        _logger.trace("Active date filter: "+_active);
  
    }
    
    
    //---------------------------------------------------------------------
    
   public int getMode()
   {
       if (this._active == null)
           return DateFilter.MODE_OFF;
       else
           return this._active.getMode();
   }
    
    //---------------------------------------------------------------------
    
    public int getOffsetId()
    {
        return this._offsetId;
    }
    
    public Offset getOffset(int id)
    {
        if (id < 0 || id >= this._offsetList.size())
            return null;
        else return this._offsetList.get(id);
    }
    
    //---------------------------------------------------------------------
    


    public Offset[] getOffsets()
    {
        Offset[] array = new Offset[0];
        array = (Offset[]) this._offsetList.toArray(array);
        return array;
    }
    
    class Offset
    {
        protected String str;
        protected long offset;
        public Offset(String str, long milliseconds)
        {
            this.str = str;
            this.offset = milliseconds;         
        }
        public long getOffset() { return this.offset; }
        public String getStr()  { return this.str; }
        
        
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
}

