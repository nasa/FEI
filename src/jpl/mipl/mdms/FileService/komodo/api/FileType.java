/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

import java.io.OutputStream;
import java.util.Date;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.util.Closable;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose: </b>
 * This class defines all operations that can be performed on a file type.
 * 
 * <PRE>
 *   Copyright 2005, California Institute of Technology. 
 *   ALL RIGHTS RESERVED. U.S.
 *   Government Sponsorship acknowledge. 2005.
 * </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History : </B> ----------------------
 * 
 * <B>Date             Who         What </B>
 * ----------------------------------------------------------------------------
 * 01/01/0001       Turek       Initial release.
 * 06/01/2005       Nick        Initial documentation.
 * 06/02/2005       Nick        Added subscribePush() method.  
 * 07/15/2005       Nick        get(String) and get(String[]) required CRC
 *                              and RESTART to create restart file.  Now, 
 *                              RESTART alone is sufficient.
 * 07/25/2005       Nick        subscribe() uses date param, restart time if
 *                              RESTART, otherwise, current time.
 * ============================================================================
 * </PRE>
 * 
 * @author Thomas Huang (Thomas.Huang@jpl.nasa.gov)
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileType.java,v 1.58 2010/09/01 17:29:01 ntt Exp $
 *  
 */

public class FileType implements Closable 
{
   private String _fileType, _serverGroup;
   private FileTypeInfo _ftInfo;
   private ServerInfo _serverInfo;
   private ServerProxy _proxy = null;
   private Session _session;
   private ClientRestartCache _restartCache = null;
   private final Logger _logger = Logger.getLogger(FileType.class.getName());

   
   /**
    * Constructor
    * 
    * @param session a transfer session, for maintaining file types and
    *           connections.
    * @param ftInfo a file type
    * @throws SessionException when session failure.
    *         If unable to connect to server, errno = CONN_FAILED.  If 
    *         authentication error, errno = INVALID_LOGIN.
    */
   public FileType(Session session, FileTypeInfo ftInfo) throws SessionException
   {

      this._session     = session;
      this._ftInfo      = ftInfo;
      this._fileType    = ftInfo.getName();
      this._serverGroup = ftInfo.getGroup();
      
      Vector v = ftInfo.getServers();
      int serverCount = v.size();
      int serverIndex = 0;     //index of current server info object
      String serverName = null;
      
      this._logger.trace("New FileType : " + this._fileType);
      
      
      while (this._proxy == null && serverIndex < serverCount)
      {
          // get serverInfo for the next element in servers vector
          serverName = v.get(serverIndex).toString();
          this._serverInfo = ftInfo.getServerInfo(serverName);
          this._logger.trace("Trying server " + serverName + "...");

          try {
    
              // If there is no server proxy for this file type, create one.
              this._proxy = this._serverInfo.getProxy();              
              if (this._proxy == null) {
                  this._logger.trace("No proxy, so make one.");
                  // Blocks.
                  ServerProxy sp = new ServerProxy(session, this._serverInfo, 
                                                   false);
                  this._serverInfo.setProxy(sp);
                  this._proxy = this._serverInfo.getProxy();
              } else {
                  // Increment the reference count on the server proxy. 
                  // Note: the server proxy reference count is initialized 
                  // to 1 in its constructor.
                  this._proxy.incrementRefCount();
              }
              
          } catch (SessionException sesEx) {
              //only retry if error was conn failed, else pass ses ex up
              if (sesEx.getErrno() == Constants.CONN_FAILED)
              {
                  this._logger.trace("Connection attempt failed: " 
                                     + serverName, sesEx);
                  ++serverIndex;
              }
              else
                  throw sesEx;
          }
          
      } //end_while

      //check that proxy exists. if not, throw exception
      if (this._proxy != null)
      {
          serverName = this._proxy.getServerInfo().getName();
          this._logger.trace("Connected to " + serverName + " .");
      }
      else
      {
          throw new SessionException("Unable to connect to filetype '" 
                      + toFullFiletype(this._serverGroup, this._fileType)
                      + "' server(s).", Constants.CONN_FAILED);
      } 
      
      /**
       * String restartFileName = this._session.getRegistory() + File.separator +
       * this._fileType + Constants.RESTARTEXTENSION; try {
       * RestartExceptionListener listener = new RestartExceptionListener();
       * XMLDecoder decoder = new XMLDecoder(new BufferedInputStream( new
       * FileInputStream(restartFileName)), this, listener);
       * 
       * this._restartInfo = (RestartInfo) decoder.readObject();
       * decoder.close();
       * 
       * if (listener.isCaught()) { this._restartInfo = new
       * RestartInfo(this._fileType, restartFileName); } } catch (Exception e) {
       * this._restartInfo = new RestartInfo(this._fileType, restartFileName); }
       * 
       * this._proxy.setRestartInfo(this._restartInfo);
       */

      // If there are no outstanding transactions, set this file type on the
      // server side. This will verify that the file type really does exist
      // on the server before returning the file type instance. If there are
      // transactions, then the server proxy servicing multi-plexed file types
      // is busy, so don't set the file type to perform early file type
      // checking, just allow the first command on that file type to fail.
      if (this._session.getTransactionCount() == 0) 
      {
         Request cmd = new Request(this._serverGroup, this._fileType,
                                  Constants.CHANGETYPE, Constants.NOMODIFIER);
         this._proxy.put(cmd);
         Result result = this._session.result();
         if (result.getErrno() != Constants.OK) {
            // Close this file type, and throw an exception.
            this.close();
            this._session.result(); // Throw away result of close.
            throw new SessionException(result.getMessage(), result.getErrno());
         }
      }
   }
   
   /**
    * Accessor method to get the name of this file type.
    * 
    * @return file type name
    */
   public final String getName() {

      return this._fileType;
   }

   /**
    * Accessor method to get the name of this file type.
    * 
    * @return Server Group name
    */
   public final String getGroup() {

      return this._serverGroup;
   }

   /**
    * Method to remove comment from a file
    * 
    * @param fileName a file name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int comment(String fileName) throws SessionException 
   {

      String[] fileNames = new String[1];
      fileNames[0] = fileName;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.COMMENTFILE, fileNames);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add comment to a file
    * 
    * @param fileName a file name
    * @param comment the comment that goes along with file(s)
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int comment(String fileName, String comment) throws SessionException 
   {

      String[] fileNames = new String[1];
      fileNames[0] = fileName;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.COMMENTFILE, fileNames, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to remove archive notation from a file
    * 
    * @param fileName a file name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int archive(String fileName) throws SessionException 
   {

      String[] fileNames = new String[1];
      fileNames[0] = fileName;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.ARCHIVENOTE, fileNames);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to archive a file
    * 
    * @param fileName a file name
    * @param comment the optional comment for the file
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int archive(String fileName, String comment) throws SessionException 
   {

      String[] fileNames = new String[1];
      fileNames[0] = fileName;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.ARCHIVENOTE, fileNames, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add with regular expression
    * 
    * @param regexp a regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int add(String regexp) throws SessionException 
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.ADDFILE, regexp);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add file with regular expression
    * 
    * @param regexp a regular expression
    * @param comment a comment to associated with each file
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int add(String regexp, String comment) throws SessionException
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.ADDFILE, regexp, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add a list of files
    * 
    * @param fileNames an array of file names
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int add(String[] fileNames) throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.ADDFILE, fileNames);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add a list of files with comment
    * 
    * @param fileNames an array of file names
    * @param comment a comment to associated with each file
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int add(String[] fileNames, String comment) throws SessionException
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.ADDFILE, fileNames, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add and reference files with regular expression. Server makes
    * the file name unique, and sets a reference to it. The name of the
    * reference is the same as the file name. If linkDirectory is not null,
    * causes Komodo to create a link of the form <linkDirectory>/ <ref name>,
    * where ref name is the name of the local file. Note: References and file
    * system links take effect at the next vft update. If reference does not
    * exist for the vft, create one. If it does exist, schedule the reference
    * for change at the next VFT update.
    * 
    * @param regexp a regular expression
    * @param vft virtual file type in created reference.
    * @param linkDirectory where file system soft link directory.
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int addAndRef(String regexp, String vft, String linkDirectory)
                                                 throws SessionException
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.ADDFILEANDREF, regexp, vft,
            linkDirectory);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add in-memory file.
    * 
    * @param fileName a file name
    * @param buffer in-memory file contents
    * @param length amount of data in the buffer to be sent as a file
    * @param comment a comment to associated with each file, or null
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int add(String fileName, byte[] buffer, long length, String comment)
                                                       throws SessionException 
   {

      String[] fileNames = new String[1];
      fileNames[0] = fileName;
      Request cmd = new Request(this._serverGroup, this._fileType,
            Constants.ADDFILE, fileNames, buffer, length, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Replace with regular expression
    * 
    * @param regexp a regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int replace(String regexp) throws SessionException 
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.REPLACEFILE, regexp);
      return (this._proxy.put(cmd));
   }

   /**
    * Replace with regular expression
    * 
    * @param regexp a regular expression
    * @param comment a comment to associated with each file
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int replace(String regexp, String comment) throws SessionException
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.REPLACEFILE, regexp, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to replace a file
    * 
    * @param fileNames an array of file names
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int replace(String[] fileNames) throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.REPLACEFILE, fileNames);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to replace a file
    * 
    * @param fileNames an array of file names
    * @param comment a comment to associated with each file
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int replace(String[] fileNames, String comment) 
                                                   throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.REPLACEFILE, fileNames, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to replace in-memory file.
    * 
    * @param fileName a file name
    * @param buffer in-memory file contents
    * @param length amount of data in the buffer to be sent as a file
    * @param comment a comment to associated with each file, or null
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int replace(String fileName, byte[] buffer, long length,
                         String comment) throws SessionException 
   {

      String[] fileNames = new String[1];
      fileNames[0] = fileName;
      Request cmd = new Request(this._serverGroup, this._fileType,
            Constants.REPLACEFILE, fileNames, buffer, length, comment);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to delete a file
    * 
    * @param regexp The regular expression file selector
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int delete(String regexp) throws SessionException 
   {

      //String[] fileNames = new String[1];
      //fileNames[0] = regexp;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.DELETEFILE, regexp);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to ignore a file
    * 
    * @param fileNames an array of file names
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int ignore(String[] fileNames) throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.IGNOREFILE, fileNames);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to rename a file
    * 
    * @param oldFile the old file name
    * @param newFile the new file name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int rename(String oldFile, String newFile)  throws SessionException
   {

      String[] f = new String[2];
      f[0] = oldFile;
      f[1] = newFile;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.RENAMEFILE, f);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show all the user's file type capabilities on the server
    * associated with this file type.
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showCapabilities() throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.SHOWCAPS, "*");
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show all files of this file type
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int show() throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.SHOWFILES, "*");
      return (this._proxy.put(cmd));
   }   

   /**
    * Method to show files according to regular expression
    * 
    * @param regexp a regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int show(String regexp) throws SessionException 
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.SHOWFILES, regexp);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show given files in the server
    * 
    * @param fileNames an array of file names
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int show(String[] fileNames) throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.SHOWFILES, fileNames);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show files of this file type since a given date
    * 
    * @param datetime the date cutoff for file show
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showSince(Date datetime) throws SessionException
   {
      return this.showAfter(datetime);
   }

   /**
    * Method to show files of this file type after a given date
    * 
    * @param datetime the date cutoff for file show
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showAfter(Date datetime)  throws SessionException 
   {
      return this.showAfter(datetime, "*");
   }

   /**
    * Method to show files of this file type after a given date
    * 
    * @param datetime the date cutoff for file show
    * @param regexp the file name regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showAfter(Date datetime, String regexp) throws SessionException 
   {

      Date[] d = new Date[1];
      ClientRestartCache restartCache = null;

      if (datetime != null) {
         // Rather than add a new command to the protocol, just increment
         // the start time by a millesecond.
         datetime.setTime(datetime.getTime() + 1);
         d[0] = datetime;
         if (this._session.getOption(Constants.RESTART)) {
            this._logger
                  .trace("Got a non-null datetime value... Persist it first.");
            restartCache = ClientRestartCache.restoreFromCache(
                  this._serverGroup, this._fileType, null, 
                          Constants.AUTOSHOWFILES, regexp, 
                          this._session.getRegistry(), 
                          this._session.getDir());
            restartCache.setLastQueryTime(datetime.getTime());
            restartCache.commit();
         }
      } else {
         if (!this._session.getOption(Constants.RESTART)) {
            throw new SessionException(
                  "Ambiguous command, must specify date or set restart option",
                  Constants.MISSINGARG);
         } else {             
             try {
                 this._logger.trace("Restart an auto show operation");
                 restartCache = ClientRestartCache.restoreFromCache(
                         this._serverGroup, this._fileType, null,                          
                               Constants.AUTOSHOWFILES, regexp, 
                               this._session.getRegistry(), this._session.getDir());

                 //Make sure this is local time!
                 d[0] = DateTimeUtil.getCCSDSAWithLocalTimeZoneDate(restartCache
                                                               .getLastQueryTime());
                 /*
                  * * Since this is the time of the last file received, bump the *
                  * time by one millesecond. This avoids adding a new command * to
                  * the wire protocol.
                  */
                 d[0].setTime(d[0].getTime() + 1);
              } catch (Exception e) {
                 d[0] = new Date(0);
              }
            
             /*//comment this out, use getAfter() impl instead.
            restartCache = ClientRestartCache.restoreFromCache(
                  this._serverGroup, this._fileType, null, new Character(
                        Constants.AUTOSHOWFILES), regexp, this._session
                        .getRegistry(), this._session.getDir());
            try {
               d[0] = DateTimeUtil.getCCSDSADate(restartCache
                     .getLastQueryTime());
               d[0].setTime(d[0].getTime() + 1);
            } catch (ParseException e) {
               this._logger.trace("Unable to get last show time.", e);
            }
            */
         }
      }

      Request cmd = new Request(this._serverGroup, this._fileType,
                                this._session.getDir(), Constants.SHOWFILES,
                                Constants.FILESSINCE, regexp, d);
      cmd.setClientRestartCache(restartCache);
      return (this._proxy.put(cmd));
   }

   
   /**
    * Method to subscribe to files of this file type.  If cache file for
    * operation exists, persisted time is used.  Otherwise current time
    * will be used.
    * @param operation Char constant of operation, one of AUTO{GET|SHOW}FILES.
    `* @param commit Flag indicating whether Result state will be 
    *        persisted to restart cache
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
    public int subscribe(String operation, boolean commit) throws SessionException
    {
        return this.subscribe(null, "*", operation, commit);
    }
   
   /**
    * Method to subscribe to files of this file type after a given date.  If
    * datetime parameter is specified, it is used.  Otherwise, retrieves
    * last query time from the restart cache if it exists and RESTART is 
    * enabled for the session.  If not, then current system time is used.
    * @param datetime the date cutoff for file show, can be null
    * @param regexp the file name regular expression
    * @param operation Char constant of operation, one of AUTO{GET|SHOW}FILES.
    * @param commit Flag indicating whether Result state will be 
    *        persisted to restart cache
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
    public int subscribe(Date datetime, String regexp, String operation, 
                         boolean commit) throws SessionException
    {
        Date[] d = new Date[1];
        ClientRestartCache restartCache = null;       
        boolean restart = this._session.getOption(Constants.RESTART);
        
        //query if cache already exists
        boolean cacheExists = ClientRestartCache.canRestoreFromCache(
                                    this._serverGroup,
                                    this._fileType, null, operation, 
                                    this._session.getRegistry(), 
                                    this._session.getDir());
        
        //force a cache to exists if committing or [restart enabled and it 
        //already exists], get reference to it
        
        // If restart is enabled, then we should always have a restartcache,
        // regardless of whether it already existed (ntt 03.05.09):
        //if (commit || (restart && cacheExists))       
        
        if (commit || restart)
        {
            this._logger.trace("Retrieving restart cache ...");
            restartCache = ClientRestartCache.restoreFromCache(
                              this._serverGroup,
                              this._fileType, null, operation, 
                              regexp, this._session.getRegistry(), 
                              this._session.getDir());
        }
        
        //caller supplied date override
        if (datetime != null)
        {
            // Rather than add a new command to the protocol, just increment
            // the start time by a millesecond.
            datetime.setTime(datetime.getTime() + 1);
            d[0] = datetime;
        }
        // only use cache time if restart enabled
        else if (restart && restartCache != null) 
        {
            this._logger.trace("Retrieving cached get time...");
            
            //Make sure this is local time!
            try {
                d[0] = DateTimeUtil.getCCSDSAWithLocalTimeZoneDate(
                                            restartCache.getLastQueryTime());

                /*
                 * Since this is the time of the last file received, bump
                 * the time by one millesecond. This avoids adding a new
                 * command to the wire protocol.
                 */
                 d[0].setTime(d[0].getTime() + 1);
            } catch (Exception ex) {
                d[0] = null;
            }
        }

        //if date array wasn't assigned, use current date
        if (d[0] == null)
            d[0] = new Date();
        
        Request cmd = new Request(this._serverGroup, this._fileType,
                                  this._session.getDir(), 
                                  Constants.SUBSCRIBEPUSH,
                                  Constants.FILESSINCE, regexp, d);
        
        //if commit is true, then associate cache with request so that
        //Result may persist its state
        if (commit)
        {
            if (restartCache != null)
            {
                this._logger.trace("Attaching restart cache to request " +
                                   "command.");
                cmd.setClientRestartCache(restartCache);
            }
        }
        
        //calling this method adds replication data to command, if enabled.
        //otherwise command is unaffected
        if (this._session.getOption(Constants.REPLICATE))
            _configureForReplication(cmd);
        
        
        return (this._proxy.put(cmd));
    }

    //---------------------------------------------------------------------
    
    /**
     * Method to subscribe to files of this file type after a given date.  If
     * datetime parameter is specified, it is used.  Otherwise, retrieves
     * last query time from the restart cache if it exists and RESTART is 
     * enabled for the session.  If not, then current system time is used.
     * @param datetime the date cutoff for file show, can be null
     * @param regexp the file name regular expression
     * @return the transaction id for tracking this command.
     * @throws SessionException when session failure
     */
     public int stopSubscribe() throws SessionException
     {
         Date[] d = new Date[] {new Date()};
         
         Request cmd = new Request(this._serverGroup, this._fileType,
                                   this._session.getDir(), 
                                   Constants.SUBSCRIBEPUSH,
                                   Constants.KILLSUBSCRIPTION, 
                                   null, d);     
         return (this._proxy.put(cmd));
     }
     
   //---------------------------------------------------------------------
   
   /**
    * Method to show files of this file type between two dates
    * 
    * @param begDate the beginning Date
    * @param endDate the last Date
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showBetween(Date begDate, Date endDate)  throws SessionException 
   {
      return this.showBetween(begDate, endDate, "*");
   }

   /**
    * Method to show files of this file type between two dates
    * 
    * @param begDate the beginning Date
    * @param endDate the last Date
    * @param regexp the file name regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showBetween(Date begDate, Date endDate, String regexp)
                                             throws SessionException
   {

      if (begDate.after(endDate))
         throw new SessionException("Begin date is after end date",
               Constants.DATE_RANGE);
      Date[] d = new Date[2];
      d[0] = begDate;
      d[1] = endDate;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.SHOWFILES,
            Constants.FILESBETWEEN, regexp, d);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show the latest file for this type.
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showLatest() throws SessionException 
   {

      Date[] d = new Date[0];
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.SHOWFILES, Constants.LATEST, "*",
            d);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show files according to regular expression
    * 
    * @param regexp a regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int showLatest(String regexp) throws SessionException 
   {

      Date[] d = new Date[0];
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.SHOWFILES, Constants.LATEST,
            regexp, d);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get a file from a Komodo server based on an absolute path.
    * 
    * @param filePath the location to files
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getFileFromPath(String filePath) throws SessionException
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.GETFILEFROMFS,
            new String[] { filePath });
      if (this._session.getOption(Constants.RESTART)
            && this._session.getOption(Constants.CHECKSUM)) {
         this._logger.trace("Resume transfer is enabled.");
         ClientRestartCache restartCache = ClientRestartCache.restoreFromCache(
               this._serverGroup, this._fileType, null,                
                     Constants.GETFILEFROMFS, null, this._session
                     .getRegistry(), this._session.getDir());
         cmd.setClientRestartCache(restartCache);
      }
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get files according to a regular expression
    * 
    * @param regexp a regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int get(String regexp) throws SessionException 
   {

      Request cmd = new Request(this._serverGroup, this._fileType,
                                this._session.getDir(), Constants.GETFILES,
                                regexp);
      //if (this._session.getOption(Constants.RESTART) &&
      //    this._session.getOption(Constants.CHECKSUM)) 
      if (this._session.getOption(Constants.RESTART)) 
      {
          if (this._session.getOption(Constants.CHECKSUM))
              this._logger.trace("Resume transfer is enabled.");
          ClientRestartCache restartCache = ClientRestartCache.restoreFromCache(
                                  this._serverGroup, this._fileType, null, 
                                  Constants.GETFILES, regexp, 
                                  this._session.getRegistry(),
                                  this._session.getDir());
          cmd.setClientRestartCache(restartCache);
      }
      
      
      //calling this method adds replication data to command, if enabled.
      //otherwise command is unaffected
      if (this._session.getOption(Constants.REPLICATE))
          _configureForReplication(cmd);
      
      
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get a file
    * 
    * @param fileNames an array of file names
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int get(String[] fileNames) throws SessionException 
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
                                this._session.getDir(), Constants.GETFILES, 
                                fileNames);
      //if (this._session.getOption(Constants.RESTART) &&
      //    this._session.getOption(Constants.CHECKSUM)) 
      if (this._session.getOption(Constants.RESTART)) 
      {
          if (this._session.getOption(Constants.CHECKSUM))
              this._logger.trace("Resume transfer is enabled.");
          ClientRestartCache restartCache = ClientRestartCache.restoreFromCache(
                                     this._serverGroup, this._fileType, null, 
                                     Constants.GETFILES, null, 
                                     this._session.getRegistry(),
                                     this._session.getDir());
          cmd.setClientRestartCache(restartCache);
      }
      
      
      //calling this method adds replication data to command, if enabled.
      //otherwise command is unaffected
      if (this._session.getOption(Constants.REPLICATE))
          _configureForReplication(cmd);
      
      
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get a file and return it on the output stream specified.
    * 
    * @param fileNames an array of file names
    * @param out the output stream to return the data on. Stream left 
    *        open on return.
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int get(String[] fileNames, OutputStream out) throws SessionException
   {
      Request cmd = new Request(this._serverGroup, this._fileType, out,
                                Constants.GETFILEOUTPUTSTREAM, fileNames);
      if (this._session.getOption(Constants.RESTART) && 
          this._session.getOption(Constants.CHECKSUM)) 
      {
         this._logger.trace("Resume transfer is enabled.");
         ClientRestartCache restartCache = ClientRestartCache.restoreFromCache(
               this._serverGroup, this._fileType, null, 
                     Constants.GETFILEOUTPUTSTREAM, null,
                     this._session.getRegistry(), this._session.getDir());
         cmd.setClientRestartCache(restartCache);
      }
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get files of this file type after a given date.
    * 
    * @param datetime the date overrides restart file date.
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getAfter(Date datetime) throws SessionException
   {
      return this.getAfter(datetime, "*");
   }

   /**
    * Method to get files of this file type after a given date.
    * 
    * @param datetime the date overrides restart file date.
    * @param regexp the file name regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getAfter(Date datetime, String regexp) throws SessionException
   {
      ClientRestartCache restartCache = null;

      Date[] d = new Date[1];
      // If date is null and Session option RESTART is set, then get date
      // from restart file. If no restart file, start one. In any case,
      // if RESTART is set, we maintain the date of the last file in
      // restart directory.
      if (datetime != null) {
         // Rather than add a new command to the protocol, just increment
         // the start time by a millesecond.
         datetime.setTime(datetime.getTime() + 1);
         d[0] = datetime;
         if (this._session.getOption(Constants.RESTART)) {
            restartCache = ClientRestartCache.restoreFromCache(
                  this._serverGroup, this._fileType, null, 
                        Constants.AUTOGETFILES, regexp, this._session
                        .getRegistry(), this._session.getDir());
            restartCache.setLastQueryTime(datetime.getTime());
            restartCache.commit();
         }
      } else {
         if (!this._session.getOption(Constants.RESTART)) {
            throw new SessionException(
                  "Ambiguous command, must specify date or set restart option",
                  Constants.MISSINGARG);
         } else {
            try {
               this._logger.trace("Restart an auto get operation");
               restartCache = ClientRestartCache.restoreFromCache(
                     this._serverGroup, this._fileType, null, 
                           Constants.AUTOGETFILES, regexp, this._session
                           .getRegistry(), this._session.getDir());

               //Make sure this is local time!
               d[0] = DateTimeUtil.getCCSDSAWithLocalTimeZoneDate(
                                            restartCache.getLastQueryTime());
                           
               /*
                * * Since this is the time of the last file received, bump the *
                * time by one millesecond. This avoids adding a new command * to
                * the wire protocol.
                */
               d[0].setTime(d[0].getTime() + 1);
            } catch (Exception e) {
               d[0] = new Date(0);
            }
         }
      }
      Request cmd;
      cmd = new Request(this._serverGroup, this._fileType, 
                        this._session.getDir(), Constants.GETFILES, 
                        Constants.FILESSINCE, regexp, d);
      cmd.setClientRestartCache(restartCache);
      
      //calling this method adds replication data to command, if enabled.
      //otherwise command is unaffected
      if (this._session.getOption(Constants.REPLICATE))
          _configureForReplication(cmd);
      
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get files of this file type since a given date
    * 
    * @param datetime to date cutoff for file get
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getSince(Date datetime) throws SessionException 
   {
      return this.getAfter(datetime, "*");
   }

   /**
    * Method to get files of this file type since a given date
    * 
    * @param datetime to date cutoff for file get
    * @param regexp the file name regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getSince(Date datetime, String regexp) throws SessionException
   {
      return this.getAfter(datetime, regexp);
   }

   /**
    * Method to get files of this file type between two dates
    * 
    * @param begDate the beginning date
    * @param endDate the last date
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getBetween(Date begDate, Date endDate) throws SessionException 
   {
      return this.getBetween(begDate, endDate, "*");
   }

   /**
    * Method to get files of this file type between two dates
    * 
    * @param begDate the beginning date
    * @param endDate the last date
    * @param regexp the file name regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getBetween(Date begDate, Date endDate, String regexp)
                                            throws SessionException 
   {

      if (begDate.after(endDate))
         throw new SessionException("Begin date is after end date",
               Constants.DATE_RANGE);
      Date[] d = new Date[2];
      d[0] = begDate;
      d[1] = endDate;
      Request cmd;
      if (regexp == null)
         cmd = new Request(this._serverGroup, this._fileType, this._session
               .getDir(), Constants.GETFILES, d);
      else
         cmd = new Request(this._serverGroup, this._fileType, this._session
               .getDir(), Constants.GETFILES, Constants.FILESBETWEEN, regexp, d);
      if (this._session.getOption(Constants.RESTART)
            && this._session.getOption(Constants.CHECKSUM)) {
         this._logger.trace("Resume transfer is enabled.");
         ClientRestartCache restartCache = ClientRestartCache.restoreFromCache(
               this._serverGroup, this._fileType, null,
                     Constants.GETFILES, regexp, 
                     this._session.getRegistry(),
                     this._session.getDir());
         cmd.setClientRestartCache(restartCache);
      }
      
      
      //calling this method adds replication data to command, if enabled.
      //otherwise command is unaffected
      if (this._session.getOption(Constants.REPLICATE))
          _configureForReplication(cmd);
      
      
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get the latest file
    * 
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getLatest() throws SessionException 
   {

      Date[] d = new Date[0];
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.GETFILES, Constants.LATEST, "*",
            d);
      if (this._session.getOption(Constants.RESTART)
            && this._session.getOption(Constants.CHECKSUM)) {
         this._logger.trace("Resume transfer is enabled.");
         ClientRestartCache restartCache = ClientRestartCache.restoreFromCache(
                     this._serverGroup, this._fileType, null, 
                     Constants.GETFILES, "*", this._session.getRegistry(),
                     this._session.getDir());
         cmd.setClientRestartCache(restartCache);
      }
      
      //calling this method adds replication data to command, if enabled.
      //otherwise command is unaffected
      if (this._session.getOption(Constants.REPLICATE))
          _configureForReplication(cmd);
      
      
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get latest file according to a regular expression
    * 
    * @param regexp a regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int getLatest(String regexp) throws SessionException
   {
      Date[] d = new Date[0];
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.GETFILES, Constants.LATEST,
            regexp, d);
      if (this._session.getOption(Constants.RESTART)
            && this._session.getOption(Constants.CHECKSUM)) {
         this._logger.trace("Resume transfer is enabled.");
         ClientRestartCache restartCache = ClientRestartCache.restoreFromCache(
                 this._serverGroup, this._fileType, null, 
                     Constants.GETFILES, regexp, this._session.getRegistry(),
                     this._session.getDir());
         cmd.setClientRestartCache(restartCache);
      }
      
      //calling this method adds replication data to command, if enabled.
      //otherwise command is unaffected
      if (this._session.getOption(Constants.REPLICATE))
          _configureForReplication(cmd);
      

      return (this._proxy.put(cmd));
   }

   /**
    * Method to get files according to a regular expression into memory
    * 
    * @param regexp a regular expression
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int memGet(String regexp) throws SessionException 
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.GETFILES, regexp);
      cmd.setInMem(true);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get file in memory
    * 
    * @param fileNames - an array of file names
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int memGet(String[] fileNames) throws SessionException 
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.GETFILES, fileNames);
      cmd.setInMem(true);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get files of this file type since a given date
    * 
    * @param datetime the date cutoff for file get
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int memGetSince(Date datetime) throws SessionException 
   {
      Date[] d = new Date[1];
      d[0] = datetime;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.GETFILES, d);
      cmd.setInMem(true);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get files of this file type between two dates into memory
    * 
    * @param begDate the beginning date
    * @param endDate the last date
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int memGetBetween(Date begDate, Date endDate) throws SessionException
   {
      if (begDate.after(endDate))
         throw new SessionException("Begin date is after end date",
               Constants.DATE_RANGE);
      Date[] d = new Date[2];
      d[0] = begDate;
      d[1] = endDate;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.GETFILES, d);
      cmd.setInMem(true);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to lock file type
    * 
    * @param mode the lock mode
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int lock(String mode) throws SessionException 
   {
      Request cmd;
      if (mode == null)
         cmd = new Request(this._serverGroup, this._fileType,
               Constants.LOCKFILETYPE, Constants.NOOPMOD);
      else if (mode.equalsIgnoreCase("group"))
         cmd = new Request(this._serverGroup, this._fileType,
               Constants.LOCKFILETYPE, Constants.GROUPMOD);
      else if (mode.equalsIgnoreCase("owner"))
         cmd = new Request(this._serverGroup, this._fileType,
               Constants.LOCKFILETYPE, Constants.OWNERMOD);
      else
         throw new SessionException("Invalid lock mode",
               Constants.INVALID_LOCK_MODE);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to unlock file type
    * 
    * @param mode the lock mode
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int unlock(String mode) throws SessionException 
   {
      Request cmd;
      if (mode == null)
         cmd = new Request(this._serverGroup, _fileType,
               Constants.UNLOCKFILETYPE, Constants.NOOPMOD);
      else if (mode.equalsIgnoreCase("group"))
         cmd = new Request(this._serverGroup, _fileType,
               Constants.UNLOCKFILETYPE, Constants.GROUPMOD);
      else if (mode.equalsIgnoreCase("owner"))
         cmd = new Request(this._serverGroup, this._fileType,
               Constants.UNLOCKFILETYPE, Constants.OWNERMOD);
      else
         throw new SessionException("Invalid lock mode",
               Constants.INVALID_LOCK_MODE);
      return (this._proxy.put(cmd));
   }
   
   /**
    * Method to register a file
    * 
    * @param fileNames an array of file names
    * @param replace flag indicating that file metadata should be re-registered
    * @param force flag indicating that all file metadata should be re-registered, 
    *        including location
    * @param comment a comment to associated with each file
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int register(String[] fileNames, boolean replace, boolean force, 
                       String comment) throws SessionException 
   {
      Request cmd = new Request(this._serverGroup, this._fileType,
                                this._session.getDir(), Constants.REGISTERFILE, 
                                fileNames, comment);
      
      //set modifier
      char modifier = Constants.NOMODIFIER;
      if (replace)
          modifier = Constants.REREGISTER;
      if (force)
          modifier = Constants.REREGISTERALL;
      
      cmd.setModifier(modifier);    
      
      return (this._proxy.put(cmd));
   }

   /**
    * Method to unregister a file
    * 
    * @param regexp The regular expression file selector
    * @return the transaction id for tracking this command.
    * @throws SessionException when session
    *            failure
    */
   public int unregister(String regexp) throws SessionException 
   {

      //String[] fileNames = new String[1];
      //fileNames[0] = regexp;
      Request cmd = new Request(this._serverGroup, this._fileType,
            this._session.getDir(), Constants.UNREGISTERFILE, regexp);
      cmd.setModifier(Constants.NOMODIFIER);
      return (this._proxy.put(cmd));
   }
   
   /**
    * Method to close this file type. Do this by appending the close command at
    * the head of the requests queue. The ServerProxy will then remove any
    * requests for the file type from the queue. If all file types for this
    * server have been closed, then the connection to the server will be
    * gracefully closed.
    * 
    * @return the transaction id for tracking this command.
    */
   public int close() {

      Request cmd = new Request(this._serverGroup, this._fileType); // Default
      // command
      // is quit.
      this._logger.trace("Queing requested command " + cmd.getCommand());
      return (this._proxy.putExpedited(cmd));
   }
   
    //---------------------------------------------------------------------
   
   /**
    * If command has replication enabled, then the session is queried
    * for a replication root directory.  If no directory is specified,
    * then the command is unaffected.
    * @param cmd The Request to be configured for replication
    */
   
   protected void _configureForReplication(Request cmd)
   {
       //if replication is enabled AND a replication root was specified,
       //then set the rep root for the request
       if (cmd != null && this._session != null && 
                     this._session.getOption(Constants.REPLICATE))
       {
           String replicationRoot = this._session.getReplicationRoot();
           if (replicationRoot != null)
               cmd.setReplicationRoot(replicationRoot);            
       }       
   }
   
   //---------------------------------------------------------------------
   
    /**
     * Extracts server group from a full filetype.  For example, if the
     * argument is "dev:type3", then "dev" is returned.  Also handles
     * case of "dev:", where "dev" would be returned.  If no server
     * group can be found, then null is returned
     * @param fullFiletype Full filetype name
     * @return server group if extracted from argument, null otherwise
     */
   
    public static String extractServerGroup(String fullFiletype)
    {
       String sg = null;
       if (fullFiletype != null)
       {
           int index = fullFiletype.indexOf(":");
           if (index != -1 && index > 0)
           {
               sg = fullFiletype.substring(0, index);
           }
       }
       return sg;
    }
   
    //---------------------------------------------------------------------
  
    /**
     * Extracts filetype from a full filetype.  For example, if the
     * argument is "dev:type3", then "type3" is returned.  If no filetype
     * can be found, i.e. not a full filetype, then null is returned
     * @param fullFiletype Full filetype name
     * @return filetype if extracted from argument, null otherwise
     */
    
    public static String extractFiletype(String fullFiletype)
    {
       String ft = null;
       
       if (fullFiletype != null)
       {
           int index = fullFiletype.indexOf(":");
           if (index != -1 && index < fullFiletype.length() - 1)
           {
               ft = fullFiletype.substring(index+1);
           }
       }
       return ft;
    }
   
    //---------------------------------------------------------------------
  
    /**
     * Forms full filetype name using the servergroup and filetype 
     * arguments.  For example, toFullFiletype("dev", "type3") returns
     * "dev:type3".  Server group can be null, but filetype must be
     * a string of positive-length.
     * @param servergroup Server group, can be null
     * @param filetype Filetype name
     * @return the full file type name from the arguments
     */
    
    public static String toFullFiletype(String servergroup, String filetype)
    {
       String fullft = null;
       if (filetype != null && !filetype.equals(""))
       {
           if (servergroup != null && !servergroup.equals(""))
               fullft = servergroup + ":" + filetype;
           else
               fullft = filetype;
       }
       return fullft;
    }
    
    //---------------------------------------------------------------------
    
      /**
       * Examines argument string and determines if it satisfies the rules
       * of a full filetype.  Rules are:
       * (1) Cannot be null or emptry string.
       * (2) Must be in form [sg]:[ft], where [sg] and [ft] are non-empty 
       * strings not starting and ending with ":" respectively.
       * @param filetype Potential full filetype string
       * @return True if filetype is considered a full filetype, false 
       *         otherwise.
       */
      
      public static boolean isFullFiletype(String filetype)
      {
          boolean rVal = false;
          int index = filetype.indexOf(":");
          if (filetype != null && index > 0 && index < filetype.length() - 2)
              rVal = true;
         return rVal;
      }
   
    //---------------------------------------------------------------------
      
    public static void main(String[] args)
    {
    	String fullft = "dev:type";
    	String sgOnly = "fev:";
    	String ftOnly = "rype";

    	System.out.println(FileType.extractServerGroup(fullft));
    	System.out.println(FileType.extractFiletype(fullft));

    	System.out.println(FileType.extractServerGroup(sgOnly));
    	System.out.println(FileType.extractFiletype(sgOnly));
    	
    	System.out.println(FileType.extractServerGroup(ftOnly));
    	System.out.println(FileType.extractFiletype(ftOnly));
    	
    }
      
    //---------------------------------------------------------------------
}