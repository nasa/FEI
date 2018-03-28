/*
 * Created on Mar 22, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaParameters;

/**
 * <b>Purpose:</b>
 * Interface for the I/O class the reads and writes meta-subscription
 * parameters from and to files, respectively.
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
 * 03/22/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MetaParameterIO.java,v 1.2 2008/10/22 00:03:46 ntt Exp $
 *
 */

public interface MetaParameterIO
{
    public static final int FORMAT_PLAIN = 0;
    public static final int FORMAT_XML   = 1;
    
    /**
     * Write contents of parameter instance to output stream
     * @param params metaparameter instance to be written
     * @param os Output stream instance 
     * @param format Format flag (FORMAT_{PLAIN,XML})
     * @throws IOException if error occurs
     */
    
    public void write(MetaParameters params, 
                      OutputStream os, 
                      int format)  throws IOException;
    
    //---------------------------------------------------------------------
    
    /**
     * Read in parameter values from input stream 
     * @param params metaparameter instance to be set
     * @param is Input stream instance 
     * @param format Format flag (FORMAT_{PLAIN,XML})
     * @throws IOException if error occurs
     */
    
    public void read(MetaParameters params,
                     InputStream is, 
                     int format) throws IOException;
}
