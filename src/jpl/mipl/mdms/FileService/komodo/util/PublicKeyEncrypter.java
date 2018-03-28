package jpl.mipl.mdms.FileService.komodo.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.utils.CipherUtil;

/**  
 * <B>Purpose:<B>
 * Wrapper that extracts the public key file property and 
 * instantiates an encrypt-only cipher util.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: PublicKeyEncrypter.java,v 1.1 2009/12/09 22:43:53 ntt Exp $
 */

public class PublicKeyEncrypter
{
    CipherUtil cipherUtil;
    ConfigFileURLResolver resourceResolver;
    URL publicKeyUrl;

    //---------------------------------------------------------------------
    
    /**
     * Constructor
     */
    
    public PublicKeyEncrypter() throws FileNotFoundException, IOException
    {
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init() throws IOException
    {
        resourceResolver = new ConfigFileURLResolver();
        URL tempUrl = null;
        
        try {
            tempUrl = resourceResolver.getFileURLViaProperty(
                                            Constants.PROPERTY_PUBLIC_KEY);
        } catch (SessionException sesEx) {
            throw new IOException("Error occurring while resolving "+
                                  "URL for public key file: "+
                                   sesEx.getMessage());                    
        }
        
        if (tempUrl == null)
        {
            throw new IOException("Cannot find URL for public key file");
        }
        else if (!resourceResolver.canReadURL(tempUrl))
        {
            throw new IOException("Resource not found at URL for property '"+
                    Constants.PROPERTY_PUBLIC_KEY + "', URL = "+tempUrl);
        }
        
        
        this.publicKeyUrl = tempUrl;
         
        
        try {
            this.cipherUtil = new CipherUtil(null, this.publicKeyUrl);
        } catch (IOException ioEx) {
            throw ioEx;
//            throw new SessionException("IO error occurred while constructing "+
//                    "cypher utility instance: "+ioEx.getMessage(), 
//                    Constants.IO_ERROR);
        } catch (URISyntaxException usEx) {
            throw new IOException("URI Syntax error: "+this.publicKeyUrl+
                                  ", "+usEx.getMessage());
        }
    }
    
    //---------------------------------------------------------------------
    
    public String encrypt(String message) throws Exception
    {
        String encrMsg = null;
        
        
        try {
            encrMsg = this.cipherUtil.encryptToHex(message);
        } catch (Exception ex) {
            throw ex;
            //throw new SessionException("Error occurred while attempting to"+
            //        " encrypt message: "+ex.getMessage(), Constants.EXCEPTION);
        }
        
        return encrMsg;
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------

}
