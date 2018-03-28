/*
 * Created on Jul 16, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.client;

import java.lang.reflect.Constructor;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QConstants;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QLoginInfo;

/**
 * <b>Purpose:</b>
 * Factory class that instantiates new QServiceProxy instance. Currently, the
 * default implementation is the QWebServiceProxy which expects the location
 * parameter to be a URL referring to the Query web service.
 * 
 * Clients can override this by setting the value of property 
 * "komodo.query.service.proxy.impl" to the fully qualified classname 
 * of the intended implementation.
 * 
 *   <PRE>
 *   Copyright 2007, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2007.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 07/16/2007        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QServiceProxyFactory.java,v 1.3 2007/09/26 00:06:50 ntt Exp $
 *
 */

public class QServiceProxyFactory
{
    protected final static String DEF_IMPL_CLASSNAME = 
                "jpl.mipl.mdms.FileService.komodo.services.query.client.QWebServiceProxy";
    
    //---------------------------------------------------------------------
    
    /**
     * Returns a new instance of a QServiceProxy based on the parameters
     * and the value of the property <code>QConstants.PROP_SERVICE_PROXY_IMPL
     * </code>, which specifies the implementation class to use for the 
     * service
     * @param client Instance of QueryClient requesting service
     * @param location Service location, in URL format
     * @param loginInfo Service login information structure
     * @return Instance of QServiceProxy to communicate with FeiQ service
     * @throws SessionException if error occurs
     */
    
    public static QServiceProxy createProxy(QueryClient client,
                                            String location,
                                            QLoginInfo loginInfo)
                                            throws SessionException
    {
        QServiceProxy proxy;
        
        try {
           proxy = buildProxy(client, location, loginInfo);
        } catch (SessionException sesEx) {
            throw sesEx;
        } catch (Exception ex) {
            throw new SessionException(ex.getMessage(), Constants.EXCEPTION);
        }
        
        
        return proxy;
    }
    
    //---------------------------------------------------------------------
    
    protected static QServiceProxy  buildProxy(QueryClient client,
                                               String location,
                                               QLoginInfo loginInfo)
                                               throws SessionException
    {
        QServiceProxy proxy = null;
        
        try {
            
            //check for override
            String className = System.getProperty(
                                        QConstants.PROP_SERVICE_PROXY_IMPL,
                                        DEF_IMPL_CLASSNAME);
            
            Class clazz = Class.forName(className);
            if (clazz != null)
            {
                Class[] parameterTypes = new Class[] {QueryClient.class,
                                                      String.class, 
                                                      QLoginInfo.class};
                Object[] argList = new Object[] {client, location, loginInfo};
                Constructor constructor = clazz.getConstructor(parameterTypes);
                Object returnObject;
                
                if (constructor != null)
                {
                    returnObject = constructor.newInstance(argList);
                    
                    if (returnObject instanceof QServiceProxy)
                    {
                        proxy = (QServiceProxy) returnObject;
                    }
                }                
            }
        } catch (Throwable t) {
            proxy = null;
        }
        
        return proxy;
        
    }
    
    //---------------------------------------------------------------------
    
}
