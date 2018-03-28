package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.DefaultMetaParameters;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaParameters;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.SubscriptionConstants;

/**
 * <b>Purpose:</b>
 * Class that handles IO for subscription parameter classes.
 * Loads and stores parameter values from and to streams. 
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
 * @version $Id: DefaultMetaParameterIO.java,v 1.7 2009/12/10 16:28:17 ntt Exp $
 *
 */

public class DefaultMetaParameterIO implements MetaParameterIO
{
    
    protected final static String HIDDEN_STRING = "*****";
    
    public DefaultMetaParameterIO()
    {
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Write contents of parameter instance to output stream
     * @param params metaparameter instance to be written
     * @param os Output stream instance 
     * @param format Format flag (FORMAT_{PLAIN,XML})
     * @throws IOException if error occurs
     */
    
    public void write(MetaParameters params,
                      OutputStream os, 
                      int format) throws IOException
     {
        if (!(params instanceof DefaultMetaParameters))
            throw new UnsupportedOperationException("Expecting instance of "+
                                     DefaultMetaParameters.class.getName());
        DefaultMetaParameters dParams = (DefaultMetaParameters) params;   
        write(dParams, os, format);
     }
    
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
                     int format) throws IOException
    {
        if (!(params instanceof DefaultMetaParameters))
            throw new UnsupportedOperationException("Expecting instance of "+
                                     DefaultMetaParameters.class.getName());
        DefaultMetaParameters dParams = (DefaultMetaParameters) params;     
        read(dParams, is, format);
    }

    //---------------------------------------------------------------------
    
    /**
     * Write contents of parameter instance to output stream
     * @param params metaparameter instance to be written
     * @param os Output stream instance 
     * @param format Format flag (FORMAT_{PLAIN,XML})
     * @throws IOException if error occurs
     */
    
    public void write(DefaultMetaParameters params,
                      OutputStream os, 
                      int format) throws IOException
    {
        if (format != FORMAT_PLAIN)
            throw new UnsupportedOperationException("This method supports "+
                    "FORMAT_PLAIN format only");
        
        StringBuffer buffer = new StringBuffer();
        buffer.append("# Written to file by ").
               append(this.getClass().getName()).
               append("\n# Date: ").append(new Date()).append("\n");
        
        Iterator it = params.getAll().keySet().iterator();
        while (it.hasNext())
        {
            String key = (String) it.next();

            if (canWriteOption(key))
            {
                Object val = params.get(key);
                buffer.append(key).append(" = ").append(val.toString()).
                       append("\n");
            }
            else
            {
                buffer.append(key).append(" = ").append(HIDDEN_STRING).
                append("\n");
            }
        }
        
        os.write(buffer.toString().getBytes());
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Checks keyword to determine if option should be printed. 
     * @param keyword Keyword to test
     * @return true if option is allowed to be written, false otherwis
     */
    
    protected boolean canWriteOption(String keyword)
    {
        if (keyword == null)
            return false;
        if (keyword.equalsIgnoreCase(SubscriptionConstants.KEY_PASSWORD))
            return false;
        
        return true;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Read in parameter values from input stream 
     * @param params metaparameter instance to be set
     * @param is Input stream instance 
     * @param format Format flag (FORMAT_{PLAIN,XML})
     * @throws IOException if error occurs
     */
    
    public void read(DefaultMetaParameters params,
                     InputStream is, 
                     int format) throws IOException
    {
        if (format != FORMAT_PLAIN)
            throw new UnsupportedOperationException("This method supports "+
                    "FORMAT_PLAIN format only");
        
        LineNumberReader reader = new LineNumberReader(
                                     new InputStreamReader(is));
        
        Properties readInProps = new Properties();
        readInProps.load(is);
        Enumeration e = readInProps.keys();
        String key, val;
        Object value;
        
        params.reset();
               
        while (e.hasMoreElements())
        {
            key = (String) e.nextElement();
            val = readInProps.getProperty(key);
            value = SubscriptionConstants.wrapValue(key, val);
            params.set(key, value);
        }
    }
}
