package jpl.mipl.mdms.FileService.util;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.util.AuthenticationType;

/**
 * <B>Purpose:<B>
 * Password utility class.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: PasswordUtil.java,v 1.1 2013/03/30 00:06:21 ntt Exp $
 *
 */
public class PasswordUtil
{

    public static final String PROMPT_PASSWORD = "Password";
    public static final String PROMPT_PASSCODE = "Passcode";
    
    public static final String DEFAULT_PROMPT  = PROMPT_PASSWORD;
    
    
    public static String getPrompt(AuthenticationType authType)
    {        
        String prompt = DEFAULT_PROMPT;
        
     
        String authCat  = authType.getCategory();
        
        if (authCat != null)
        {
            if (authCat.equals(Constants.AUTH_INSTITUTIONAL_PASSC))
            {
                prompt = PROMPT_PASSCODE;
            }
            else if (authCat.equals(Constants.AUTH_INTERNAL_PASSC))
            {
                prompt = PROMPT_PASSCODE;
            }
            else if (authCat.equals(Constants.AUTH_INSTITUTIONAL_PASSW))
            {
                prompt = PROMPT_PASSWORD;
            }
            if (authCat.equals(Constants.AUTH_INTERNAL_PASSW))
            {
                prompt = PROMPT_PASSWORD;
            }
        }
        
        return prompt;
            
    }
 
    
}
