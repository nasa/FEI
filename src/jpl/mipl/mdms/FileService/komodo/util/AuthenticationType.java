package jpl.mipl.mdms.FileService.komodo.util;

import jpl.mipl.mdms.FileService.komodo.api.Constants;

/**
 * <B>Purpose:<B>
 * Data structure capturing the category and method
 * for authentication.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: AuthenticationType.java,v 1.2 2013/10/15 17:26:27 ntt Exp $
 *
 */
public class AuthenticationType
{
    protected String serverGroup;
    
    protected String authImplementation;
    protected String authCategory;
    
    public AuthenticationType(String servergroup)
    {
        this.serverGroup = servergroup;
    }
    
    public void setImpl(String impl)
    {
        this.authImplementation = impl;
    }
    
    public void setCategory(String cat)
    {
        this.authCategory = cat;
    }
    
    public String getCategory()
    {
        return this.authCategory;
    }
    
    public String getImpl()
    {
        return this.authImplementation;
    }
    
    public boolean isValid()
    {
        if (this.serverGroup == null)
            return false;
//        if (this.authImplementation == null)
//            return false;
        if (this.authCategory == null)
            return false;
        
        if (!(authCategory.equals(Constants.AUTH_INSTITUTIONAL_PASSC) || 
              authCategory.equals(Constants.AUTH_INSTITUTIONAL_PASSW) ||
              authCategory.equals(Constants.AUTH_INTERNAL_PASSC) ||
              authCategory.equals(Constants.AUTH_INTERNAL_PASSW)))
        {
            return false;
        }
        
        return true;
    }
    
    public boolean isMaintainedInternally()
    {        
        if (isValid() &&
            (authCategory.equals(Constants.AUTH_INTERNAL_PASSC) ||
             authCategory.equals(Constants.AUTH_INTERNAL_PASSW)) )
        {
              return true;
        }

        return false;
    }
}
