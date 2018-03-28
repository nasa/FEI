/*
 * Created on Nov 14, 2006
 */
package jpl.mipl.mdms.FileService.util;

/**
 * <b>Purpose:</b>
 * TODO - Enter purpose.
 * 
 *   <PRE>
 *   Copyright 2006, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2006.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * mm/dd/2006        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: Semaphore.java,v 1.1 2006/11/14 23:49:40 ntt Exp $
 *
 */

public class Semaphore
{
    private int count;
    
    public Semaphore(int count)
    {
        this.count = count;
    }
    public synchronized void acquire() {
        while (this.count == 0)
        {
            try {
                wait();
            } catch (InterruptedException e) {
                //keep trying
            }
        }
        --count;
    }
    public synchronized void release()
    {
        ++count;
        notify();
    }
}
