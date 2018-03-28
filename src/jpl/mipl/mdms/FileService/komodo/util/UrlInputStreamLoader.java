package jpl.mipl.mdms.FileService.komodo.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import jpl.mipl.mdms.FileService.komodo.api.Constants;

public class UrlInputStreamLoader
{
    public static final int DEFAULT_CONN_TIMEOUT =  30000;  //30 seconds
    public static final int DEFAULT_READ_TIMEOUT = 120000;  //2 minutes
    
    protected int connectionTimeoutMs;
    protected int readTimeoutMs;
    
    protected static UrlInputStreamLoader __instance;
    
    //---------------------------------------------------------------------
    
    public UrlInputStreamLoader()
    {
        initialize();
    }
    
    //---------------------------------------------------------------------
    
    protected void initialize()
    {
        this.connectionTimeoutMs = DEFAULT_CONN_TIMEOUT;
        this.readTimeoutMs       = DEFAULT_READ_TIMEOUT;
        
        String propVal;
        int   intMilliSeconds;

        //-------------------------
        
        propVal = System.getProperty(Constants.PROPERTY_URL_CONN_TIMEOUT);
        if (propVal != null)
        {
            try {
                intMilliSeconds = Integer.parseInt(propVal);
                if (intMilliSeconds >= 0)
                {
                    this.connectionTimeoutMs = intMilliSeconds;
                }
            } catch (NumberFormatException nfEx) {                
            }
        }
        
        //-------------------------
        
        propVal = System.getProperty(Constants.PROPERTY_URL_READ_TIMEOUT);
        if (propVal != null)
        {
            try {
                intMilliSeconds = Integer.parseInt(propVal);
                if (intMilliSeconds >= 0)
                {
                    this.readTimeoutMs = intMilliSeconds;
                }
            } catch (NumberFormatException nfEx) {                
            }
        }

        //-------------------------
        
    }

    //---------------------------------------------------------------------
    
    public InputStream getInputStream(URL url) throws IOException
    {
        URLConnection connection = url.openConnection();
        
        if (connection == null)
            return null;
        
        connection.setReadTimeout(readTimeoutMs);
        connection.setConnectTimeout(connectionTimeoutMs);
        
        InputStream is = connection.getInputStream();
        
        return is;
    }
    

    //---------------------------------------------------------------------
    
    public static InputStream open(URL url) throws IOException
    {
        UrlInputStreamLoader loader = new UrlInputStreamLoader();        
        return loader.getInputStream(url);
    }
    
    //---------------------------------------------------------------------
    
    public static InputStream open(String url) throws MalformedURLException,
                                                      IOException
    {
        URL theUrl = new URL(url);
        
        UrlInputStreamLoader loader = getSingleton();
        //UrlInputStreamLoader loader = new UrlInputStreamLoader();        
        return loader.getInputStream(theUrl);
    }

    //---------------------------------------------------------------------
    
    protected synchronized static UrlInputStreamLoader getSingleton()
    {
        if (__instance == null)
        {
            __instance = new UrlInputStreamLoader();
        }
        
        return __instance;
    }
    
    //---------------------------------------------------------------------
    
    public int getReadTimeout()
    {
        return this.readTimeoutMs;
    }
    
    //---------------------------------------------------------------------
    
    public int getConnectionTimeout()
    {
        return this.connectionTimeoutMs;
    }
    
    //---------------------------------------------------------------------
    
}
