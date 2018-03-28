/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.services.query.api;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.api.ClientRestartCache;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;

/**
 * Request class maintains information about query requests. This is
 * an internal package class. Not part of the interface.
 * 
 * @author Nicholas Toole
 * @version $Id: QRequest.java,v 1.6 2009/08/07 01:00:48 ntt Exp $
 */
public class QRequest 
{

   //Request parameters
   private String _command;
   private char _modifier = Constants.NOMODIFIER;
   private String[] _fileNames;
   private String[] _cmdArgs;
   private Date[] _dates;
   private String _regexp;
   private String _serverGroup;
   
   private boolean _subscribe;
   
   private QueryList _queryList;
   private QueryConstraints _systemQuery;
   
   private ClientRestartCache _restartCache = null;
   private Map<String,ClientRestartCache> _restartCacheMap;
   
   //Transaction parameters
   private int _transactionId = -1;
   private int _options;
   

   // = USER request constructors

   public QRequest(String serverGroup, String fileType) {
       this._serverGroup = serverGroup;
       this._command = Constants.QUIT;
    }
   
   
   /**
    * Constructor used to close file type connection <br>
    * The command is internally set to QUIT.
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    */
   QRequest(String command, String serverGroup, QueryList query) {
      this._serverGroup = serverGroup;
      this._command = command;
      this._queryList = query;
   }
   
   public QRequest(String command, String serverGroup, QueryList query, QueryConstraints sQuery) 
   {
       this(command, serverGroup, query);
       this._systemQuery = sQuery;
    }

   /**
    * Constructor for regular expression requests <br>
    * The command modifier is internally set to REGEXP.
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param directory the client's current working directory
    * @param command the command's shortcut (a, g, s, n,....)
    * @param regexp a regular expression
    */
   public QRequest(String serverGroup, String command, String regexp) {
      this._serverGroup = serverGroup;
      this._command = command;
      this._regexp = regexp;      
      this._modifier = Constants.REGEXP;
   }
   
   public QRequest(String command, String[] args) {
       this._command = command;
       this._cmdArgs = args;      
    }

   /**
    * Constructor for regular expression requests, where dates are used to
    * further discriminate the selected set. Note: An empty Data array implies
    * latest. Otherwise one Date means "since", and two means "between". <br>
    * the command modifier is internally set to REGEXP.
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param directory the client's current working directory
    * @param command the command's shortcut (a, g, s, n,....)
    * @param modifier the command modifier (e.g. regular expression, latest)
    * @param regexp a regular expression
    * @param dates the list of date filters
    */
   public QRequest(String serverGroup, String fileType, String directory, String command,
         char modifier, String regexp, Date[] dates) {
      this._serverGroup = serverGroup;
      this._command = command;
      this._regexp = regexp;

      this._dates = dates;
      //if (dates.length > 0)
      //   this._modifier = Constants.REGEXP;
      //else
      this._modifier = modifier;
   }

   
   public QueryList getUserQuery()
   {
       return this._queryList;
   }
   
   public QueryConstraints getSystemQuery()
   {
       return this._systemQuery;
   }


   /**
    * Accessor method to get command
    * 
    * @return command string (a, l, n,...)
    */
   public final String getCommand() {

      return this._command;
   }

   /**
    * Method to get command modifier
    * 
    * @return command modifier (0 = none, x = regexp, s = since, b = between, l =
    *         latest for regexp, a = at)
    */
   public final char getModifier() {

      return this._modifier;
   }

   /**
    * Method to set command modifier
    * 
    * @param modifier the command modifier.
    */
   public final void setModifier(char modifier) {

      this._modifier = modifier;
   }



   /**
    * Method to get regular expression
    * 
    * @return string representation of regular expression
    */
   public final String getRegExp() {

      return this._regexp;
   }

   /**
    * Method to get dates for 'since' and 'between' modified commands
    * 
    * @return array of Dates objects for 'since' and 'between' modified commands
    */
   public final Date[] getDates() {

      return this._dates;
   }

   /**
    * Method to get array of file names command is operating on
    * 
    * @return array of file names command is operating on
    */
   public final String[] getFileNames() {

      return this._fileNames;
   }

   /**
    * Method to get array of command arguments
    * 
    * @return array of command arguments
    */
   public final String[] getCommandArgs() {

      return this._cmdArgs;
   }


   /**
    * Method to get array of file names command is operating on
    * 
    * @param fileNames the list of file names
    */
   public final void setFileNames(String[] fileNames) {

      this._fileNames = fileNames;
   }

   /**
    * Method to set transaction id.
    * 
    * @param transactionId the transaction Id
    */
   public final void setTransactionId(int transactionId) {

      this._transactionId = transactionId;
   }

   public final void setClientRestartCache(ClientRestartCache restartCache) {
      this._restartCache = restartCache;
   }

   
   public final void setClientRestartCache(String filetype, ClientRestartCache restartCache) 
    {
        synchronized(this)
        {
            if (this._restartCacheMap == null)
                this._restartCacheMap = new Hashtable<String, ClientRestartCache>();
            
            this._restartCacheMap.put(filetype, restartCache);
        }
    }
    
    public final void setClientRestartCaches(Map<String, ClientRestartCache> map) 
    {
        synchronized(this)
        {            
            if (this._restartCacheMap == null)
                this._restartCacheMap = new Hashtable<String, ClientRestartCache>();
         
            for (String filetype : map.keySet())
            {
                ClientRestartCache restartCache = map.get(filetype);
                setClientRestartCache(filetype, restartCache);
            }            
        }
    }
    
    public final ClientRestartCache getClientRestartCache(String filetype)
    {
        ClientRestartCache cache = null;
        synchronized(this)
        {
            if (this._restartCacheMap != null)
                cache = this._restartCacheMap.get(filetype);
        }
        return cache;
    }
    
   
   /**
    * Method to perform client restart cache object checkout. Once the object is
    * returned, the internal cache object reference is set to null. The caller
    * must call setClientRestartCache to return the object to this class.
    * 
    * @return the client restart cache object reference.
    * /
   final synchronized ClientRestartCache getClientRestartCache() {
      return this._restartCache;
      /**
       * ClientRestartCache restartCache = this._restartCache;
       * this._restartCache = null; return restartCache;
       * /
   }
   */



   /**
    * Method to get transaction id
    * 
    * @return the transaction id of the command. If this returns -1, and error
    *         has occurred.
    */
   public final int getTransactionId() {

      return this._transactionId;
   }

   /**
    * Method to set transaction option.
    * 
    * @param options the options mask
    */
   public final void setOptions(int options) {

      this._options = options;
   }

   /**
    * Method to return true if the input option is enabled in the options mask
    * 
    * @param option the option value
    * @return true if the option is enabled in the options mask
    */
   public boolean getOption(int option) {

      return (this._options & option) > 0 ? true : false;
   }

   /**
    * Method to get the options mask
    * 
    * @return all currently set options.
    */
   public int getOptions() {

      return (this._options);
   }


   /**
    * Method to return the server grou pname
    * 
    * @return the server group name
    */
   public final String getServerGroup() {
      return this._serverGroup;
   }

   public final boolean isSubscribe() {
       return this._subscribe;
    }
   
   public final void setSubscribe(boolean enable) {
       this._subscribe = enable;
    }
   
   public final void setSystemQuery(QueryConstraints systemQuery)
   {
       this._systemQuery = systemQuery;
   }

   /**
    * General command marshalling method.
    * 
    * @return entire command string ready to ship to server
    */
   public final String getCommandString() {

      StringBuffer sb = new StringBuffer();
      sb.append(this._command);
      if (this._modifier != Constants.NOMODIFIER)
         sb.append(this._modifier);
      if (this._cmdArgs != null) {
         for (int i = 0; i < this._cmdArgs.length; i++) {
            if (this._cmdArgs[i] == null)
               sb.append(" null");
            else
               sb.append(" " + this._cmdArgs[i]);
         }
      }
      if (this._dates != null) {
         for (int i = 0; i < this._dates.length; i++) {
            if (this._dates[i] != null)
               sb.append(" " + DateTimeUtil.getDateDBString(this._dates[i]));
         }
      }
      return sb.toString();
   }
}