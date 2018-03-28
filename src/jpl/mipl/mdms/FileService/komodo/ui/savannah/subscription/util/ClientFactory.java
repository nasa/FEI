/*
 * Created on Jan 12, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

import java.net.URL;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.MetaParameters;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.SubscriptionConstants;
import jpl.mipl.mdms.FileService.komodo.util.ReconnectThrottle;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Factory for Client instances.
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
 * 01/12/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ClientFactory.java,v 1.7 2016/03/30 21:24:56 ntt Exp $
 *
 */

public class ClientFactory
{
    protected static final String ERROR_TAG = "FEI_ERROR::";
    protected static final long _MINUTE_MS = 60000;
    private static final Logger _logger = Logger.getLogger(
                                              ClientFactory.class.getName());
    
    /** 
     * Indicates that caller is willing to block until request
     * is fulfilled. 
     */
    public static final long BLOCK_INDEFINITELY = -1L;
    
    //---------------------------------------------------------------------
    
    /**
     * Creates and returns new Client object.  If SessionException
     * occurs IO during construction, then method sleeps for an
     * increasing amount of time and another attempt is made.
     * This process repeats indefinitely.
     * @param params Instance of MetaParameters with the required
     *               parameters set (domain file, username, 
     *               password, servergroup, filetype)
     */
    
    public static Client createClient(MetaParameters params)
    {
        return createClient(params, BLOCK_INDEFINITELY);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates and returns new Client object.  If SessionException
     * for IO occurs during construction, then method sleeps for an
     * increasing amount of time and another attempt is made.
     * This process repeats indefinitely until next sleep time 
     * would be greater than quit time. If any other error occurs,
     * then a null reference will be returned.
     * @param params Instance of MetaParameters with the required
     *               parameters set (domain file, username, 
     *               password, servergroup, filetype)
     * @param waitTime Number of milliseconds to wait before giving
     *               up construction of Client.  Can block indefinitely
     *               if BLOCK_INDEFINITELY is supplied.
     * @return Newly constructed client if successful, else null.
     */
     
    
    public static Client createClient(MetaParameters params, long waitTime)
    {
        return createClient(params, waitTime, null);
    }
    //---------------------------------------------------------------------
    
    /**
     * Creates and returns new Client object.  If SessionException
     * for IO occurs during construction, then method sleeps for an
     * increasing amount of time and another attempt is made.
     * This process repeats indefinitely until next sleep time 
     * would be greater than quit time. If any other error occurs,
     * then a null reference will be returned.
     * @param params Instance of MetaParameters with the required
     *               parameters set (domain file, username, 
     *               password, servergroup, filetype)
     * @param waitTime Number of milliseconds to wait before giving
     *               up construction of Client.  Can block indefinitely
     *               if BLOCK_INDEFINITELY is supplied.
     * @return Newly constructed client if successful, else null.
     */
     
    
    public static Client createClient(MetaParameters params, long waitTime,
                                      ReconnectThrottle throttle)
    {
        //set quitTime if positive value supplied
        boolean block = true;
        long quitTime = BLOCK_INDEFINITELY;
        
        if (waitTime >= 0)
        {
            block = false;
            quitTime = System.currentTimeMillis() + waitTime;
        }
        
        //get necessary parameters
        URL domainFile;
        String user, password, serverGroup, fullFiletype, filetype;
        domainFile   = (URL)    params.get(SubscriptionConstants.
                                           KEY_DOMAIN_FILE_URL);
        user         = (String) params.get(SubscriptionConstants.
                                           KEY_USERNAME);
        password     = (String) params.get(SubscriptionConstants.
                                           KEY_PASSWORD);
        fullFiletype = (String) params.get(SubscriptionConstants.
                                           KEY_FILETYPE);
        
        serverGroup = FileType.extractServerGroup(fullFiletype);
        filetype    = FileType.extractFiletype(fullFiletype);
        
        //try-block below will handle cases where values weren't specified
        if (domainFile == null)
            throw new IllegalArgumentException("Factory cannot create Client "
                    +"without domain file parameter");           
        if (user == null)
            throw new IllegalArgumentException("Factory cannot create Client "
                    +"without username parameter");
        if (password == null)
            throw new IllegalArgumentException("Factory cannot create Client "
                    +"without password parameter");
        if (filetype == null)
            throw new IllegalArgumentException("Factory cannot create Client "
                    +"without filetype parameter");
        
//        long minSleepTime = 60 * 1000;
//        long maxSleepTime = minSleepTime * 16;
//        long sleepTime    = minSleepTime;
    
        Client client = null;
        
        while (client == null) {
            
            
            throttle.pollWait();
            
           try {
              client = new Client(domainFile);
              client.login(user, password, serverGroup, filetype);
              client.set("restart", true);
           } catch (SessionException sesEx) {
              String msg;
              if (sesEx.getErrno() == Constants.INVALID_LOGIN)
              {
                  msg = "FEI5 Information on "
                      + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                      + ERROR_TAG + "Invalid login.  Could not connect to ["
                      + serverGroup + ":" + filetype + "].  Aborting...\n";                  
                  _logger.error(msg);
                  _logger.debug(sesEx.getMessage(), sesEx);
                  client = null;
                  break;
              }
              else
              {               
                 msg = "FEI5 Information on "
                    + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                    + ERROR_TAG + "Unable to restart session to ["
                    + serverGroup + ":" + filetype + "].";  
                    //Next attempt in "+ (sleepTime / _MINUTE_MS) + " minute(s).\n";
                            
                 _logger.error(msg);
                 _logger.debug(sesEx.getMessage()+" "+sesEx.getErrno(), sesEx);
                 client = null;
              }
           }
    
           if (!block && System.currentTimeMillis() > quitTime)
               break;
           
           //no longer needed, we use throttle now
//           if (client == null) {
//              try {
//                 Thread.sleep(sleepTime);
//                 if (sleepTime < maxSleepTime)
//                    sleepTime *= 2;
//              } catch (InterruptedException e) {
//                 break;
//              }
//           }
        }
        
        return client;
    }
 
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
}
