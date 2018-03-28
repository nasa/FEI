/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.services.query.client;

import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.ClientRestartCache;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.client.CMD;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QConstants;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QLoginInfo;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QRequest;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QResult;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QueryConstraints;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QueryList;
import jpl.mipl.mdms.FileService.komodo.services.query.util.QueryClientUtil;
import jpl.mipl.mdms.FileService.komodo.util.Closable;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * 
 * Client end to the FeiQ web service
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QueryClient.java,v 1.8 2015/01/30 01:52:14 awt Exp $
 */

public class QueryClient implements Closable 
{
    private QServiceProxy _proxy;
    
    private QLoginInfo _loginInfo;
    private String _serviceUrlString;
    private QueryList _userQuery = null;
    private QueryConstraints _systemQuery = null;
        
    private int _transactionId;
    private int _transactionCount = 0;
    
    private String _serverGroup;     
    private List<QResult> _results;
    private List<String> _accessibleFiletypes;
    private Map _optionsMap;
    private String _outputDirectory;
    private String _singleType;
    private boolean _restartEnabled = false;
    
    /** Logger instance */
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    protected int max_size = 250;
    protected Random _random = new Random(System.currentTimeMillis());
    
    //---------------------------------------------------------------------
    
    public QueryClient(QLoginInfo loginInfo, Map options) throws SessionException 
    {
         this(System.getProperty(QConstants.PROP_SERVICE_LOCATION), 
              loginInfo, options);
    }
    
    //---------------------------------------------------------------------
    
   /**
    * Constructor
    * 
    * @param serviceUrl The query service URL
    * @param loginInfo Session login information for service
    * @param options Map of options, can contain output, restart,
    *        date filters, date format, file expressions
    * @throws SessionException when failed to initialize
    */
    
   public QueryClient(String serviceUrl, QLoginInfo loginInfo, Map options)
                                                    throws SessionException
    {
        if (serviceUrl == null)
            throw new SessionException("serviceUrl parameter cannot be null",
                                       Constants.NOT_SET);
        if (loginInfo == null)
            throw new SessionException("loginInfo parameter cannot be null",
                                       Constants.NOT_SET);
        if (options == null)
            throw new SessionException("options parameter cannot be null",
                                       Constants.NOT_SET);

        this._serviceUrlString = serviceUrl;
        this._loginInfo = loginInfo;
        this._serverGroup = this._loginInfo.getServerGroup();
        this._optionsMap = options;

        init();
    }

   //---------------------------------------------------------------------
   
   protected void init() throws SessionException
   {
       this._transactionId = 0;
       
       //--------------------------
       
       //init collections
       this._results = new Vector<QResult>();
       this._accessibleFiletypes = new Vector<String>();
       
       //--------------------------
       
       //extract options (OUTPUT, RESTART)
       if (this._optionsMap != null)
       {
           this._outputDirectory = (String) this._optionsMap.get(CMD.OUTPUT);
           this._restartEnabled = this._optionsMap.containsKey(CMD.RESTART);
           this._singleType = (String) this._optionsMap.get(CMD.FILETYPE);
       }
       if (this._outputDirectory == null)
           this._outputDirectory = System.getProperty("user.dir");
       
       
       //--------------------------
       
       //built the service proxy
       this._proxy = QServiceProxyFactory.createProxy(this,
                                                      this._serviceUrlString,
                                                      this._loginInfo);
       
       if (this._proxy == null)
           throw new SessionException("Could not create an instance of QServiceProxy",
                                      Constants.CONN_FAILED);

       //--------------------------
       
       //we can generate a basic sys query from options map
       initSystemQuery();
       
       //--------------------------       
   }

   //---------------------------------------------------------------------
   
   protected void initSystemQuery() throws SessionException
   {
       QueryConstraints sysQuery = null;
       
       //create sys query containing file expression and global date info
       try {
           sysQuery = QueryClientUtil.createSystemQuery(this._optionsMap);
       } catch (SessionException sesEx) {
           throw sesEx;
       }
       
       this._systemQuery = sysQuery;
   }
   
   
   //---------------------------------------------------------------------
   
   /**
    * Accessor method to get the service URL as string
    * 
    * @return the service URL
    */
   
   public final String getServiceUrlString() 
   {
       return this._serviceUrlString;
   }
   
   //---------------------------------------------------------------------
   
   public final String getServerGroup()
   {
       return this._serverGroup;
   }

   //---------------------------------------------------------------------
   
   public final QLoginInfo getLoginInfo()
   {
       return this._loginInfo;
   }

   //---------------------------------------------------------------------
   
   public void setUserQuery(QueryList query)
   {
       this._userQuery = query;
   }
   
   //---------------------------------------------------------------------
   
   void setSystemQuery(QueryConstraints query)
   {
       this._systemQuery = query;
   }

   //---------------------------------------------------------------------
   
   /**
    * Query proxy will notify this instance of the accessible filetypes.
    * @param filetypes Array of filetype names that user can access
    */
   
   void setAccessibleFiletypes(String[] filetypes)
   {
       synchronized (this._accessibleFiletypes)
       {
           this._accessibleFiletypes.clear();
           
           if (filetypes != null)
           {
               for (String filetype : filetypes)
               {
                   this._accessibleFiletypes.add(filetype);
               }
           }           
       }
   }
   
   //---------------------------------------------------------------------
   //---------------------------------------------------------------------
   
   /**
    * Sends single query. 
    * @return proxy transaction id
    * @throws SessionException if error occurs
    */
   public int sendQuery() throws SessionException
   {           
       if (this._userQuery == null)
           throw new SessionException("No query set",
                                      Constants.NOT_SET);       
       
       Map<String, ClientRestartCache> crcMap = attemptLoadCrcMap(false);
       completeSystemQuery(crcMap);
       
       QRequest cmd = new QRequest(this._loginInfo.getOperation(),
    		   					   this._serverGroup, this._userQuery, 
    		   					   this._systemQuery);
       cmd.setClientRestartCaches(crcMap);
       
       //--------------------------
       
       return this._proxy.put(cmd);       
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Subscribes a query.  
    * @return proxy transaction id
    * @throws SessionException if error occurs
    */
   
   public int subscribeQuery() throws SessionException
   {       
       if (this._userQuery == null)
           throw new SessionException("No query set",
                                      Constants.NOT_SET);

       Map<String, ClientRestartCache> crcMap = attemptLoadCrcMap(true);
       completeSystemQuery(crcMap);
       
       //--------------------------
       
       //create the command and set it up for subscription-like session
       QRequest cmd = new QRequest(this._loginInfo.getOperation(),
                                   this._serverGroup, this._userQuery, 
                                   this._systemQuery);
       cmd.setSubscribe(true);
       cmd.setClientRestartCaches(crcMap);
       
       //--------------------------
       
       return this._proxy.put(cmd);  
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Kills a query subscription.  
    * @return proxy transaction id
    * @throws SessionException if error occurs
    */
   
   public int unsubscribeQuery() throws SessionException
   {
       QRequest cmd = new QRequest(this._loginInfo.getOperation(),
                                   this._serverGroup, null, null);
       cmd.setSubscribe(true);
       cmd.setModifier(Constants.KILLSUBSCRIPTION);
       
       return this._proxy.put(cmd);
   }  
   
   //---------------------------------------------------------------------
   
   
   /**
    * Method to post a result to the result queue. 
    * @param result the file profile
    */
   
   void postResult(QResult result) {
      this._logger.trace("Posting result.");
      while (this._results.size() >= max_size)
      {
          try {
              Thread.sleep(this._random.nextLong() % 250);
          } catch (InterruptedException iEx) {     
        	  iEx.printStackTrace();
          }
      }
      
      synchronized (this._results) 
      {  
          //TODO - do we want to add a size check here and WAIT until size decreases?
          
         this._results.add(result);
         this._logger.trace("Posting result notify.");
         this._results.notify();
         this._logger.trace("Posting result notified.");
      }
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Read next result from this client. Can block until result
    * is available.  Return value may be null, so caller should
    * quit when <code>getTransactionCount()</code> returns 0.
    * @return Next Result instance
    * @throws SessionException if error occurs
    */
   
   public Result result() throws SessionException
   {
       QResult qResult = null;
       synchronized (this._results) 
       {
          if (this.getTransactionCount() > 0) 
          {
             while (this._results.isEmpty()) 
             {
                try {
                   this._results.wait();
                } catch (InterruptedException e) {
                   this._logger.trace(null, e);
                   throw new SessionException("Unexpected interrupt.",
                         Constants.INTERRUPTED);
                }
             }
             
             qResult = (QResult) this._results.get(0);
             this._results.remove(0);
             
//System.out.println("DEBUG::QueryCLient::result: Result errno = "+qResult.getErrno());
//System.out.println("DEBUG::QueryCLient::result: Result NO SHOW USER = "+qResult.isDoNotShowUser());
//System.out.println("DEBUG::QueryCLient::result: Result EOT = "+qResult.isEoT());

             if (qResult.isEoT()) 
             {
                 _decrementTransactionCount();
             }
          }
          
          if (qResult.isDoNotShowUser())
              return null;
          
          Result result;          
          int errno = qResult.getErrno();
          
          //if errno suggests that we have some file information,
          //then create a file Result and pass it on; else treat 
          //as an error Result
          if (errno == Constants.OK)
          { 
        	  result = new Result(null, qResult.getName(), qResult.getSize(),
                                  qResult.getFileModificationTime());
          }
          else
          {
        	  result = new Result(null, qResult.getErrno(), 
        			  			  qResult.getMessage());
          }
          
          result.setServerGroup(this._serverGroup);
          result.setType(qResult.getType());          
    	  result.setClientRestartCache(qResult.getClientRestartCache());
          
          return result;          
       }
   }
   
   //---------------------------------------------------------------------
   
   public boolean isQueryActive()
   {
       return this._proxy.isQueryRunning();
   }
   
   //---------------------------------------------------------------------

   /**
    * Method to close this admin channel. Do this by appending the close command
    * at the head of the requests queue. The ServerProxy will then remove any
    * admin requests for from the request queue. If all referneces, file types
    * and admin for this server have been closed, then the connection to the
    * server will be gracefully closed.
    * 
    * @return the transaction id for tracking this command.
    */
   public final int close() 
   {
      QRequest cmd = new QRequest((String) null,(String)null); // Default command is quit.
      this._logger.trace("Queuing requested command " + cmd.getCommand());
      return (this._proxy.putExpedited(cmd));
   }
   
   
   
   /**
    * Get new transaction id. Also increments the transaction count.
    * 
    * @return the transaction id
    */
   public synchronized int getTransactionId() 
   {
      this._incrementTransactionCount();
      return ++this._transactionId;
   }
   
   private void _incrementTransactionCount() {
       synchronized (this) {
          this._transactionCount++;
       }
    }
   
   private void _decrementTransactionCount() {
       synchronized (this) {
          this._transactionCount--;
       }
    }
   
   public int getTransactionCount() {
       synchronized (this) {
          return (this._transactionCount);
       }
    }
   
   
   //----------------------------------------------------------------------
   
   public boolean isAlive()
   {
       return (this.getTransactionCount() > 0);
   }
   
   //----------------------------------------------------------------------
   
   boolean isRestartEnabled()
   {
       return this._restartEnabled;
   }
   
   //----------------------------------------------------------------------
   
   String getOutputDirectory()
   {
       return this._outputDirectory;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Will attempt to load the map of CRC's based on whether the RESTART 
    * option appears in the options map.  Caller can force loading by 
    * passing true as the parameter.
    * @param forceLoad Forces caches to be loaded  
    * @returns Map of CRCs with filetype as key
    * @throws SessionException if error occurs
    */
   
    protected Map<String, ClientRestartCache> attemptLoadCrcMap(boolean forceLoad)
                                                         throws SessionException
    {
        Map<String, ClientRestartCache> restartCacheMap;
        if (forceLoad || this.isRestartEnabled())
            restartCacheMap = QueryClientUtil.loadRestartCaches(
                                          this._serverGroup,
                                          this._loginInfo.getOperation(),
                                          this._accessibleFiletypes, 
                                          this._outputDirectory);
        else
            restartCacheMap = new Hashtable<String, ClientRestartCache>();
	    
        return restartCacheMap;
   	}
   
   
   //---------------------------------------------------------------------- 
   
   /**
    * Extracts the Date associated with a filetype from the map if found.
    * Otherwise null will be returned.
    * @param filetype Name of the file type for which a date is requested
    * @param restartCacheMap Map of ClientRestartCache instances that contain
    * restart info
    * @return Date of last query, if defined for filetype
    * @throws SessionException if error occurs
    */

    protected Date getDateFromCacheMap(String filetype,
            Map<String, ClientRestartCache> restartCacheMap)
            throws SessionException
    {
        Date date = null;

        ClientRestartCache crc = restartCacheMap.get(filetype);
        if (crc != null)
        {

            try {
                date = DateTimeUtil.getCCSDSAWithLocalTimeZoneDate(crc
                        .getLastQueryTime());
            } catch (ParseException pEx) {
                throw new SessionException(pEx.getMessage(),
                        Constants.EXCEPTION);
            }
        }

        return date;
    }

   //----------------------------------------------------------------------
   
   /**
    * Called prior to a query creation.  This method examines any filetype
    * specific filters, such as last mod times for subscriptions.
    * @param crcMap Map of client restart caches for filetypes
    * @throws SessionException if error occurs
    */
   
   protected void completeSystemQuery(Map<String, ClientRestartCache> crcMap)
            throws SessionException
    {
        // ---------------------------

        for (String curFiletype : this._accessibleFiletypes)
        {
            if (this._singleType != null
                    && !this._singleType.equals(curFiletype))
                continue;

            String fullFiletype = FileType.toFullFiletype(this._serverGroup,
                    curFiletype);
            Date date = getDateFromCacheMap(curFiletype, crcMap);

            if (date != null)
                this._systemQuery.setFiletypeTime(fullFiletype, date.getTime());
            else
                this._systemQuery.setFiletypeTime(fullFiletype, null);
        }

        // ---------------------------

        // check for date conflicts
        try {
            this._systemQuery.sanityCheck();
        } catch (SessionException sesEx) {
            throw sesEx;
        }
    }
   
   
   //----------------------------------------------------------------------
}