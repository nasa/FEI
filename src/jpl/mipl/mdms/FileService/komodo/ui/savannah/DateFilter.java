package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.util.Calendar;
import java.util.Date;

/**
 * <B>Purpose:<B>
 * Data structure for date filters.  Handles explicit bounds
 * for BEFORE, AFTER, and BETWEEN, as well as implicit bounds
 * via OFFSET.  
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DateFilter.java,v 1.1 2013/04/04 20:48:50 ntt Exp $
 *
 */
public class DateFilter
{
    public static final int MODE_OFF         = 0;
    public static final int MODE_BETWEEN     = 1;
    public static final int MODE_AFTER       = 2;
    public static final int MODE_BEFORE      = 3;
    public static final int MODE_OFFSET      = 4;
    
    protected int   mode;
    protected long  date;
    protected long  endDate;
    protected long  offset;
    
    public static final long NO_VALUE = -1L;
    
    static final long referenceTime = System.currentTimeMillis();
    
    public DateFilter()
    {
        this.mode    = MODE_OFF;
        this.offset  = NO_VALUE;
         
        this.date    = referenceTime;
        this.endDate = referenceTime;
    }
    
    public DateFilter(int mode, long timeMs)
    {
        this();
        
        if (mode == MODE_AFTER || mode == MODE_BEFORE)
        {
            this.mode    = mode;
        }
        this.date    = timeMs;        
    }
    
    public DateFilter(long startMs, long endMs)
    {
        this();
//        if (startMs > endMs)
//        {
//            long temp = startMs;
//            startMs = endMs;
//            endMs = temp;
//        }
            
        this.date = startMs;
        this.endDate = endMs;
        this.mode = MODE_BETWEEN;            
                         
    }
    
    public DateFilter(long offsetMs)
    {
        this();
        if (offsetMs > 0)
        {
            this.mode = MODE_OFFSET;
            this.offset = offsetMs;
        }          
    }
    
    public boolean equals(Object o)
    {
        if (!(o instanceof DateFilter))
            return false;
        DateFilter other = (DateFilter) o;
        if (this.mode != other.mode)
            return false;
        if (this.date != other.date)
            return false;
        if (this.endDate != other.endDate)
            return false;
        if (this.offset != other.offset)
            return false;
        return true;
    }
    
    public int getMode()
    {
        return this.mode;
    }
    public long getDate()
    {
        return this.date;
    }
    public long getEndDate()
    {
        return this.endDate;
    }
    public long getOffset()
    {
        return this.offset;
    }
    
    public void setDateBounds(long start, long end)
    {
        this.date = start;
        this.endDate = end;
    }
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        if (this.mode == DateFilter.MODE_OFF)
        {
            buffer.append("Non active");
        }
        else if (this.mode == DateFilter.MODE_OFFSET)
        {
            buffer.append("Offset of ").append(this.offset).append(" ms");
        }
        else if (this.mode == DateFilter.MODE_AFTER)
        {
            buffer.append("After ").append(new Date(this.date)).append("");
        }
        else if (this.mode == DateFilter.MODE_BEFORE)
        {
            buffer.append("Before ").append(new Date(this.date)).append("");
        }
        else if (this.mode == DateFilter.MODE_BETWEEN)
        {
            buffer.append("Between ").append(new Date(this.date)).append(" and ").
                   append(new Date(this.endDate));
        }
        
        
        return buffer.toString();
    }
}

