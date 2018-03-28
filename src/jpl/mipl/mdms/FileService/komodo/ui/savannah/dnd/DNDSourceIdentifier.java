package jpl.mipl.mdms.FileService.komodo.ui.savannah.dnd;

/**
 * <b>Purpose:</b>
 *  DNDSourceIdentifier is merely a wrapper around a string
 *  to be used to identify the source of a DND transfer.
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
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 06/02/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DNDSourceIdentifier.java,v 1.1 2004/09/21 22:11:28 ntt Exp $
 *
 */

public class DNDSourceIdentifier 
{
    public static final String HUMAN_READABLE = "DNDSourceIdentifier";
    
    protected String _name;
    
    public DNDSourceIdentifier(String name)
    {
        _name = name;    
    }
    
    public String getName()
    {       
        return _name;
    }
    
    public boolean equals(Object obj)
    {
        DNDSourceIdentifier other;
        
        if (obj instanceof DNDSourceIdentifier)
        {
            other = (DNDSourceIdentifier) obj;
            return _name.equals(other.getName());
        }
        return false;
    }
}

