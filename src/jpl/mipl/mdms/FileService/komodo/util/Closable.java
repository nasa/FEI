/*
 * Created on Oct 11, 2005
 */
package jpl.mipl.mdms.FileService.komodo.util;


/**
 * <b>Purpose:</b>
 * Interface for closable objects.  
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
 * 10/11/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: Closable.java,v 1.1 2005/10/13 23:32:05 ntt Exp $
 *
 */

public interface Closable
{
    public int close();
}
