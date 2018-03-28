package jpl.mipl.mdms.FileService.komodo.api;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Client restart cache JavaBean is used to cache file download and query
 * information. The cached data can be persisted in XML format to assist
 * restarting of user query or file download session. The cache data is 
 * stored with file name: 
 * &lt;servergroup&gt;.&lt;filetype&gt;.&lt;subtype&gt;.&lt;restart|notify&gt;. 
 * It is created per file type.
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
 * 01/01/0001        Thomas           Initial Release
 * 01/10/2006        Nick             Initial documentation.
 * 01/10/2006        Nick             Added backup capability.  When committing
 *                                    a backup of existing file is made.  When
 *                                    restoring, if cannot restore from file,
 *                                    the backup is checked.
 * 06/25/2008        Nick             Added persisted location to accommodate
 *                                    replication
 * ============================================================================
 * </PRE>
 *
 * @author Thomas   Huang   (Thomas.Huang@jpl.nasa.gov)
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ClientRestartCache.java,v 1.16 2009/08/07 01:00:47 ntt Exp $
 *
 */

public class ClientRestartCache {

   private static final Object _syncLock = new Object();    
    
   //the command (e.g.subscribe,notify,get)
   private String _command = null; 
   
   //the name of the file this object will be stored
   private String _cacheFilename = null; 
   
   //only for subscribe, get, and vft
   private String _outputDir = null; 
   
   //the servergroup
   private String _servergroup = null; 
   
   //the filetype
   private String _filetype = null; 
   
   //the subfiletype
   private String _subtype = null;

   // since a VFT can contain many files. Use a properties object to store
   // file resume information is a more general storage solution.
   // mapping (file name, resume data)
   private Hashtable _filesToPersist = new Hashtable();

   // references:
   
   //the local CCSDS time of the last query time
   private String _lastQueryTime = null;
   
   //the query expression queried.
   private String _lastQueryExpression = null;  

   //private File _classicFile = null;
   //private File _cacheFile = null;

   private static final String VFT_KEY      = "vft";
   private static final String SIZE_KEY     = "size";
   private static final String TIME_KEY     = "time";
   private static final String LOCATION_KEY = "location";

   private boolean _backupCommits = true;
   
   private Logger _logger = Logger.getLogger(
                                   ClientRestartCache.class.getName());

    //---------------------------------------------------------------------
   
   /**
    * Factory method to create a client restart cache. This method also
    * migrates any existing legacy restart file into the new format
    * 
    * @param servergroup server group name
    * @param filetype file tye name
    * @param subtype file sub-type name
    * @param command the command (add,get,list..)
    * @param queryExpression the file name query expression
    * @param classicRegistry the classic registry locaion
    * @param outputDir the file output location. For get or autoget, this should
    *           be the output directory where new files will be stored. For show
    *           or autoshow, this should be the user cache directory (i.e.
    *           $HOME/.komdo)
    * @return ClientRestartCache built from existing cache file, legacy file,
    *         or a new instance
    */
   
   public static ClientRestartCache restoreFromCache(String servergroup,
                                     String filetype, String subtype, 
                                     String command, String queryExpression,
                                     String classicRegistry, String outputDir) 
   {
      Logger logger = Logger.getLogger(ClientRestartCache.class.getName());
      boolean corruptCacheFlag = false;
      ClientRestartCache crc = null;
      
      //retrieve filepath for cache file
      String cacheFilename = buildCacheFilePath(servergroup, filetype, 
                                                subtype, command, outputDir);

      //verify that directory containing cache exists, if not, make it so.
      File cacheFile = new File(cacheFilename);      
      cacheFile.getParentFile().mkdirs();
      cacheFilename = cacheFile.getAbsolutePath();
            
      // if the cache file already exists then just restore and return
      if (cacheFile.canRead())
      {
          crc = ClientRestartCache.restoreFromCache(cacheFilename);
          if (crc == null)
          {
              corruptCacheFlag = true;          
              logger.warn("Unable to restore from existing cache file.");
          }
          else
              logger.trace("Restored from cache.");     
      } 
      
      //---------------------------
      
      //if null cache, try the backup file
      if (crc == null)      
      {
          //try loading from backup cache
          String bakFilename = cacheFilename + Constants.BACKUPEXTENSION;
          cacheFile = new File(bakFilename);
          
          if (cacheFile.canRead())
          {          
              logger.trace("Trying to restore from backup cache.");     
              crc = ClientRestartCache.restoreFromCache(bakFilename);              
              if (crc == null)
              {
                  corruptCacheFlag = true;              
                  logger.warn("Unable to restore from existing backup cache file.");
              }
              else
              {
                  if (corruptCacheFlag)
                      logger.warn("Original cache file corrupted. " +
                                   "Restored from backup cache.");
                  else
                      logger.warn("Restored from backup cache.");
              }
          }
      }
      
      //---------------------------
      
      //if we've got a cache, set state and return it
      if (crc != null)
      {
          crc.setOutputDir(outputDir);
          crc.setCacheFilename(cacheFilename);
          return crc;
      }

      //---------------------------
      
      //retrieve filepath for classic restart file
      String classicRestartFile = buildClassicRestartFilePath(classicRegistry,
                                                              filetype);
      
      // otherwise, if classic cache exists then restore
      logger.trace("Trying to restore from classic cache: "
                   + classicRestartFile);
      File classicFile = new File(classicRestartFile);
      if (!classicFile.exists() || !classicFile.isFile()) 
      {
          if (!corruptCacheFlag)
              logger.trace("Classic cache file does not exist. Creating " +
                           "a new cache object.");
          else
              logger.warn("Due to cache file corruption, creating " +
                          "new cache object.");
          
         crc = new ClientRestartCache(servergroup, filetype, subtype, command,
                                      queryExpression, cacheFilename);
         try {
            crc.commit();
         } catch (SessionException e) {
            logger.error("Unable to persist session restart info to " +
                         cacheFilename);
         }
         return crc;
      }

      //---------------------------
      
      logger.trace("Found classic restart cache.  Migrating to " +
                   "new cache object");
      crc = new ClientRestartCache(servergroup, filetype, subtype, command,
                                   queryExpression, cacheFilename);
      RestartInfo restartInfo = ClientRestartCache.restoreFromClassicCache(
                                                        classicRestartFile);

      crc.setOutputDir(outputDir);
      crc.setCacheFilename(cacheFilename);

      if (restartInfo == null) 
      {
         logger.trace("Unable to restore from classic cache..." +
                      "just return a new cache object");
         return crc;
      }

      crc.setFileType(restartInfo.getFileType());
      if (command.equals(Constants.SHOWFILES) || 
          command.equals(Constants.AUTOSHOWFILES))
      {
          crc.setLastQueryTime(DateTimeUtil.getDateCCSDSAString(
                               new Date(restartInfo.getListRestartTime())));
      }
      else if (command.equals(Constants.GETFILES) || 
               command.equals(Constants.AUTOGETFILES))
      {
          crc.setLastQueryTime(DateTimeUtil.getDateCCSDSAString(
                               new Date(restartInfo.getRestartTime())));
      }
     
      // retrieve the internal resume file data structure.
      Properties fileList = restartInfo.getFileList();
      Properties vftList  = restartInfo.getVFTList();
      if (fileList != null) {
         logger.trace("Migrating classic restart data structure to new " +
                      "data structure");
         Hashtable filesToResume = new Hashtable();
         Enumeration filenames = fileList.keys();
         while (filenames.hasMoreElements()) {
            String filename = (String) filenames.nextElement();
            Hashtable valueTable = new Hashtable();
            String vft = vftList.getProperty(filename);
            if (vft != null) {
               valueTable.put(ClientRestartCache.VFT_KEY, vft);
            }
            String value = fileList.getProperty(filename);
            if (value == null)
               continue;
            String[] values = value.split(" ");
            valueTable.put(ClientRestartCache.SIZE_KEY, new Long(values[0]));
            valueTable.put(ClientRestartCache.TIME_KEY, 
                           DateTimeUtil.getDateCCSDSAString(
                                   new Date(Long.parseLong(values[1]))));
            filesToResume.put(filename, valueTable);
         }
         crc.setFilesToResume(filesToResume);
      }
            
      try {
         logger.trace("Completed migration.  " +
                      "Now persist the new cache object.");
         crc.commit();
      } catch (SessionException e) {
         logger.error("Unable to write restart file: " + cacheFilename, e);
      }
      return crc;
   }

   //---------------------------------------------------------------------
   
   /**
    * Factory method to create a classic restart data object from the classic
    * restart registry file
    * 
    * @param cachefile the classic cache file
    * @return a classic restart object
    */
   
   static RestartInfo restoreFromClassicCache(String cachefile) 
   {     
      File file = new File(cachefile);
      if (!file.canRead())
          return null;
       
      RestartInfo restartInfo = null;
      try {
         XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(
                                     new FileInputStream(file)));
         decoder.setExceptionListener(new ExceptionListener() {
            public void exceptionThrown(Exception e) {
               // no-op
            }
         });
         restartInfo = (RestartInfo) decoder.readObject();
         decoder.close();
      } catch (Exception e) {
         // unable to recover classic restart file info, so just use the
         return null;
      }
      return restartInfo;
   }
   
   //---------------------------------------------------------------------

   /**
    * Factory method to create a client restart cache object from a cache file
    * @param cachefile the cache file
    * @return the client restart cache object
    */
   
   static ClientRestartCache restoreFromCache(String cachefile) 
   {       
       //--------------------------
       
       File file = new File(cachefile);     
       ClientRestartCache crc = null;
       final boolean[] error = {false};
      
       //--------------------------
       //try reading state from cache file
       
       synchronized(_syncLock)
       {
           if (!file.canRead())
               return null;
          
           try {
               XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(
                                    new FileInputStream(cachefile)));
               decoder.setExceptionListener(new ExceptionListener() {
                   public void exceptionThrown(Exception e) {                      
                       error[0] = true;
                   }
               });
               crc = (ClientRestartCache) decoder.readObject();
               decoder.close();
           } catch (Exception e) {
               // unable to recover classic restart file info, so just use the
               error[0] = true;
               //return crc;
           }
       }
       
       //--------------------------
       //check for error, write log message if flag set.
       
       if (error[0])
       {
           Logger.getLogger(ClientRestartCache.class.getName()).warn(
                   "Error occurred while attempting to restore restart cache" +
                   " from existing file '"+ file.getAbsolutePath()+"'.  " +
                   "Ignoring file...");       
       }
       
       //--------------------------
       
       return (error[0]) ? null : crc;
    }

    //---------------------------------------------------------------------
   
   /**
    * Returns path restart cache based on the parameters. 
    * @param servergroup Servergroup name
    * @param filetype Filetype name
    * @param subtype Sub-filetype name
    * @param command String id associated with command
    * @param outputDir Output directory path
    * @return Path of restart cache file, or null if indeterminable.
    */
   
   static String buildCacheFilePath(String servergroup, String filetype,
                                    String subtype, String command, 
                                    String outputDir)
   {       
       String cacheFilename = null;
       
       String prefix = outputDir + File.separator + Constants.SHADOWDIR +
                       File.separator + "." + servergroup + "." + filetype;
       if (subtype != null)
           prefix = prefix + "." + subtype;
       
        // determin the restart file location
//        switch (command.charValue()) {
//          case Constants.SHOWFILES:
//          case Constants.AUTOSHOWFILES:
//            cacheFilename = prefix + Constants.NOTIFYEXTENSION;
//            break;
//          case Constants.GETFILES:
//          case Constants.AUTOGETFILES:
//          case Constants.GETVFT:
//            cacheFilename = prefix + Constants.RESTARTEXTENSION;
//            break;
//        }
        
       if (command.equals(Constants.SHOWFILES) || 
           command.equals(Constants.AUTOSHOWFILES))
       {
           cacheFilename = prefix + Constants.NOTIFYEXTENSION;
       }
       else if (command.equals(Constants.GETFILES) || 
               command.equals(Constants.AUTOGETFILES) || 
               command.equals(Constants.GETVFT) )
       {
           cacheFilename = prefix + Constants.RESTARTEXTENSION;
       }

        return cacheFilename;
   }
   
    //---------------------------------------------------------------------
   
   /**
    * Returns path restart cache based on the parameters. 
    * @param classicRegistry Path of directory holding restart file, 
    *        null uses default.
    * @param filetype Filetype name
    * @return Path of classic cache file, or null if indeterminable.
    */
   
   static String buildClassicRestartFilePath(String classicRegistry, 
                                             String filetype)
   {       
       String classicRestartFile = null;
       
       if (classicRegistry == null)
           classicRegistry = System.getProperty("user.home") + File.separator
                             + Constants.RESTARTDIR;

       classicRestartFile = classicRegistry + File.separator + filetype
                             + Constants.RESTARTEXTENSION;
        
        return classicRestartFile;
   }
   
    //---------------------------------------------------------------------
   
    /** 
     * Checks if a cache file associated with the parameters exists.
     * First checks for a modern restart file.  If that cannot be found,
     * then checks for backup.  If not found, finally checks for 
     * classic restart file.  If none exists, then false is returned.
     * @param servergroup Servergroup name
     * @param filetype Filetype name
     * @param subtype Sub-filetype name
     * @param command Character associated with command
     * @param outputDir Output directory path
     * @param outputDir Output directory path
     * @return True if a cache file is restorable, false otherwise.
     */
   
    public static boolean canRestoreFromCache(String servergroup,
                                              String filetype, 
                                              String subtype, 
                                              String command,
                                              String classicRegistry, 
                                              String outputDir)
    {
        
        String[] pathsToCheck = new String[3];
        String path;
        boolean canRestore = false;
        
        //-------------------------
        
        //populate the array of files to check for restore
        
        //entry 0: cache file 
        pathsToCheck[0] = buildCacheFilePath(servergroup, filetype, subtype,
                                             command, outputDir);
        //entry 1: backup cache file
        if (pathsToCheck[0] != null)
            pathsToCheck[1] = pathsToCheck[0] + Constants.BACKUPEXTENSION;
    
        //entry 2: classic restart cache
        pathsToCheck[2] = buildClassicRestartFilePath(classicRegistry, filetype);
        
        //-------------------------
        
        //iterate looking for an existing and readable file
        
        for (int i = 0; i < pathsToCheck.length && !canRestore; ++i)
        {
            path = pathsToCheck[i];            
            if (path != null)
            {
                //build file from path
                File file = new File(path);
    
                //return file existence state
                canRestore = canRestore || (file.isFile() && file.canRead());
            }
        }

        //-------------------------
        
        return canRestore;
    }
    
    //---------------------------------------------------------------------
   
   /**
    * Default constructor - required by JavaBean encoding architecture
    */
    
   public ClientRestartCache() 
   {
      // need this for JavaBean encoding and decoding
   }

   //---------------------------------------------------------------------
   
   /**
    * Restore cache information from a file
    * @param cachefile
    * /
   public ClientRestartCache(String cachefile) {
      try {
         XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(
               new FileInputStream(this._classicFile)), this,
               new ExceptionListener() {
                  public void exceptionThrown(Exception e) {
                     // no-op
                  }
               });
         ClientRestartCache crc = (ClientRestartCache) decoder.readObject();
         decoder.close();
      } catch (Exception e) {
         // unable to recover classic restart file info, so just use the
         return;
      }

   }
   */
   
   //---------------------------------------------------------------------

   /**
    * Constructor
    * @param servergroup the server group
    * @param filetype the file type
    * @param subtype the file sub-type
    * @param command the user command
    * @param queryExpression the file name query expression
    * @param cacheFilename the cache file name
    */
   
    public ClientRestartCache(String servergroup, String filetype,
                              String subtype, String command, 
                              String queryExpression, String cacheFilename) 
    {
        this._servergroup = servergroup;
        this._filetype = filetype;
        this._subtype = subtype;
        //this._command = command;
        this.setCommand(command);
        this._lastQueryExpression = queryExpression;
        this._cacheFilename = cacheFilename;
        this._lastQueryTime = DateTimeUtil.getDateCCSDSAString(
                                   DateTimeUtil.getLocalDate());
    }

   //---------------------------------------------------------------------
   
   /**
    * Method to add persist informtion used by resume transfer.
    * @param filename the file name
    * @param size the file size
    * @param utc the file modification time
    */
   
   public void addPersist(String filename, long size, long utc) 
   {
//      Hashtable vt = new Hashtable();
//      vt.put(ClientRestartCache.SIZE_KEY, new Long(size));
//      vt.put(ClientRestartCache.TIME_KEY, DateTimeUtil
//            .getDateCCSDSAString(new Date(utc)));
//      this._filesToPersist.put(filename, vt);
       addPersist(filename, size, utc, _outputDir);
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to add persist informtion used by resume transfer.
    * @param filename the file name
    * @param size the file size
    * @param utc the file modification time
    */
   
   public void addPersist(String filename, long size, long utc, String location) 
   {
      Hashtable vt = new Hashtable();
      vt.put(ClientRestartCache.SIZE_KEY, new Long(size));
      vt.put(ClientRestartCache.TIME_KEY, DateTimeUtil.getDateCCSDSAString(
                                                           new Date(utc)));
      if (location != null)
          vt.put(ClientRestartCache.LOCATION_KEY, location);
      this._filesToPersist.put(filename, vt);
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method to add persist infomation used by resume transfer on getVFT
    * @param filename the file name
    * @param vft the reference name registered under a VFT
    * @param size the file size
    * @param utc the file modification time
    */
   
   public void addPersist(String filename, String vft, long size, long utc) 
   {
//      Hashtable vt = new Hashtable();
//      vt.put(ClientRestartCache.VFT_KEY, vft);
//      vt.put(ClientRestartCache.SIZE_KEY, new Long(size));
//      vt.put(ClientRestartCache.TIME_KEY, DateTimeUtil
//            .getDateCCSDSAString(new Date(utc)));
//      this._filesToPersist.put(filename, vt);
       addPersist(filename, vft, size, utc, _outputDir);
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to add persist information used by resume transfer on getVFT
    * @param filename the file name
    * @param vft the reference name registered under a VFT
    * @param size the file size
    * @param utc the file modification time
    * @param location Path to directory containing file
    */
   
   public void addPersist(String filename, String vft, long size, long utc, 
                                                           String location) 
   {
      Hashtable vt = new Hashtable();
      vt.put(ClientRestartCache.VFT_KEY, vft);
      vt.put(ClientRestartCache.SIZE_KEY, new Long(size));
      vt.put(ClientRestartCache.TIME_KEY, DateTimeUtil.getDateCCSDSAString(
                                                       new Date(utc)));
      if (location != null)
          vt.put(ClientRestartCache.LOCATION_KEY, location);
      this._filesToPersist.put(filename, vt);
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method to remove resume transfer information. Usually afer a successful
    * transfer 
    * @param filename the file name
    */
   
   public void removePersist(String filename) 
   {
      this._filesToPersist.remove(filename);
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to get peristed resume transfer information on a VFT reference 
    * @param filename the file anme
    * @return the reference name
    */
   
   public String getPersistedVFT(String filename) 
   {
      Hashtable vt = (Hashtable) this._filesToPersist.get(filename);
      if (vt == null)
         return null;
      return (String) vt.get(ClientRestartCache.VFT_KEY);
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to get resume offset value of a file.
    * If file is declared in cache, exists and is not dir, 
    * then the file length is returned.  Otherwise, null is returned. 
    * @param filename the file name
    * @return the offset value, or null
    */
   public Long getResumeOffset(String filename) 
   {      
      if (filename == null)
          return null;
      
      Long offset = null;
      Hashtable vt = (Hashtable) this._filesToPersist.get(filename);
      if (vt == null)
         return null;

      String refName  = (String) vt.get(ClientRestartCache.VFT_KEY);
      String location = (String) vt.get(ClientRestartCache.LOCATION_KEY);
      
      if (location == null)
          location = this._outputDir;
      
      File file = null;
      
      if (refName != null)
        file = new File(location + File.separator + refName);
      else
        file = new File(location + File.separator + filename);
      
      if (!file.isDirectory() && file.exists()) 
      {
          offset = new Long(file.length());
      }
      
      return offset;
   }
//   public Long getResumeOffset(String filename) 
//   {      
//      if (filename == null)
//          return null;
//      
//      Long offset = null;
//      Hashtable vt = (Hashtable) this._filesToPersist.get(filename);
//      if (vt == null)
//         return null;
//
//      String refName = (String) vt.get(ClientRestartCache.VFT_KEY);
//      File file = null;
//      
//      if (refName != null)
//        file = new File(this._outputDir + File.separator + refName);
//      else
//        file = new File(this._outputDir + File.separator + filename);
//      if (!file.isDirectory() && file.exists()) {
//        offset = new Long(file.length());
//      }
//      
//      return offset;
//   }

   //---------------------------------------------------------------------
   
   /**
    * Method to obtain the cached file size
    * @param filename the file name
    * @return the file size
    */
   
   public Long getPersistedFileSize(String filename) 
   {
      Hashtable vt = (Hashtable) this._filesToPersist.get(filename);
      if (vt == null)
         return null;
      return (Long) vt.get(ClientRestartCache.SIZE_KEY);
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to obtain the cached file size
    * @param filename the file name
    * @return the file size
    */
   
   public String getPersistedFileLocation(String filename) 
   {
      Hashtable vt = (Hashtable) this._filesToPersist.get(filename);
      if (vt == null)
         return null;
      return (String) vt.get(ClientRestartCache.LOCATION_KEY);
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Method to return the file modification time in CCSDS formated string
    * @param filename the file name
    * @return the CCSDS-formated file modification string
    */
   
   public String getPersistedCCSDSModTimeString(String filename) 
   {
      Hashtable vt = (Hashtable) this._filesToPersist.get(filename);
      if (vt == null)
         return null;
      return (String) vt.get(ClientRestartCache.TIME_KEY);
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to return the file modification time in a Date object 
    * @param filename the file name
    * @return the Date object
    */
   
   public Date getPersistedModTime(String filename) 
   {
      String ccsds = this.getPersistedCCSDSModTimeString(filename);
      Date persistedTime = new Date();
      if (ccsds == null)
         return persistedTime;
      try {
         persistedTime = DateTimeUtil.getCCSDSADate(ccsds);
      } catch (Exception e) {
         return persistedTime;
      }
      return persistedTime;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor method to return the cache file name
    * @return the cache file name
    */
   
   public String getCacheFilename() 
   {
      return this._cacheFilename;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the cache file name
    * @param cacheFilename the cache file name
    */
   
   public void setCacheFilename(String cacheFilename) 
   {
      this._cacheFilename = cacheFilename;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor method to return the user command
    * @return the user command string
    */
   
   public String getCommand() 
   {
      return this._command;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the user command operator character 
    * @param command the command operator
    */
   
   public void setCommand(String command) 
   {
       //handle the case of older cache files that use a single char
       //for the command
       if (command.length() == 1)
       {
           
           command = DeprecatedConstants.getCommandString(command.charAt(0), 
                                                       true);
       }
       
      this._command = command;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor method to return server group name
    * @return the server group name
    */
   
   public String getServerGroup() 
   {
      return this._servergroup;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set server group name
    * @param servergroup the server group name
    */
   
   public void setServerGroup(String servergroup) 
   {
      this._servergroup = servergroup;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor method to get file type name
    * @return the file type name
    */
   
   public String getFileType() 
   {
      return this._filetype;
   }
   
   //---------------------------------------------------------------------

   /**
    * Method to set file type name
    * @param filetype the file type name
    */
   
   public void setFileType(String filetype) 
   {
      this._filetype = filetype;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor method to file sub-type name
    * @return the sub-type name
    */
   
   public String getSubType() 
   {
      return this._subtype;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set sub-type name
    * @param subtype the sub type name
    */
   
   public void setSubType(String subtype) 
   {
      this._subtype = subtype;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor method to return internal hashtable used to keep track of file
    * resume transfer information
    * @return the hashtable
    */
   
   public Hashtable getFilesToResume() 
   {
      return this._filesToPersist;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set file resume transfer internal data structure. 
    * @param filesToResume the hashtable
    */
   
   public void setFilesToResume(Hashtable filesToResume) 
   {
      this._filesToPersist = filesToResume;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor to obtain the last query time in CCSDS formated time string
    * @return the last query time
    */
   
   public String getLastQueryTime() 
   {
      return this._lastQueryTime;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the last query time in UTC long integer  
    * @param utc the UTC long integer
    */
   
   public void setLastQueryTime(long utc) 
   {
      this._lastQueryTime = DateTimeUtil.getDateCCSDSAWithTimeZoneString(new Date(utc));
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the last query time in CCSDS-formated time
    * @param ccsds the CCSDS formated time
    */
   
   public void setLastQueryTime(String ccsds) 
   {
       String temp;
       try {
           temp = DateTimeUtil.completeTimeStringWithWithRFC822TimeZone(ccsds, true, true);
       } catch (Exception ex) {
           temp = null;
       }
       
       if (temp != null)
           this._lastQueryTime = temp; 
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor for last file query expression
    * @return the last file query expression
    */
   
   public String getLastQueryExpression() 
   {
      return this._lastQueryExpression;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set last file query expression 
    * @param lastQueryExpression the last file query expression
    */
   
   public void setLastQueryExpression(String lastQueryExpression) 
   {
      this._lastQueryExpression = lastQueryExpression;
   }

   //---------------------------------------------------------------------
   
   /**
    * Accessor to file output directory
    * @return the output directory
    */
   
   public String getOutputDir() 
   {
      return this._outputDir;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the output directory 
    * @param outputDir the output directory
    */
   
   public void setOutputDir(String outputDir) 
   {
      this._outputDir = outputDir;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to persist this cache JavaBean in XML format.
    * @throws SessionException
    */
   
   public void commit() throws SessionException 
   {
       FileOutputStream fos = null;
       XMLEncoder encoder = null;
       
       synchronized(this)
       {
           //--------------------------
           
           //backup previous file
           if (_backupCommits)
               commitBackup();
           
           //--------------------------
       
           File file = new File(this._cacheFilename);
           
           try {
               fos = new FileOutputStream(file);              
               encoder = new XMLEncoder(new BufferedOutputStream(fos));
               encoder.writeObject(this);
               encoder.flush();
           } catch (IOException ioEx) {              
               throw new SessionException(
                       "IOException while writing to restart file.  Error: " +
                       ioEx.getMessage(), Constants.RESTARTFILEERR);

           } finally {
               if (encoder != null)
                   encoder.close(); 
               if (fos != null)
                   try { fos.getFD().sync(); } catch (IOException ioEx) {}               
           }
       }
    }

   //---------------------------------------------------------------------
   
   /**
    * Copies existing restart file to a backup version in the name of
    * fault tolerance.  When cache is restored from file, it will first
    * check the usual filename.  If unsuccessful, it will then check for
    * the backup version, which will be out-of-date, but not as much so 
    * as starting over.  Finally, if that is unsuccessful, a classic
    * cache file is checked. If all fails, then a cache is created
    * from no persisted state.
    */
   
   protected synchronized void commitBackup()
   {
       File file = new File(this._cacheFilename);
       File bakFile = new File(this._cacheFilename + Constants.BACKUPEXTENSION);              
       
       if (file.exists())
       {
           try {
               copyFileNIO(file, bakFile);
           } catch (IOException ioEx) {
               this._logger.warn("Could not backup cache file.  " +
                                 "Reason: "+ioEx.getMessage());
               this._logger.trace(ioEx.getMessage(), ioEx);
           }
       }
       else
       {
           this._logger.trace("No cache file to back-up.");
       }
   }

   //---------------------------------------------------------------------
   
   /**
    * Renames source file to destination file.
    * @param src Source file
    * @param dst Destination file
    */
   
   protected synchronized void moveFile(File src, File dst) throws IOException
   {                
       //backup previous file
       if (src.exists())
       {
           this._logger.trace("Restart file '" + src.getName() + 
                              "' exists.  Will attempt to backup.");
           
           if (dst.exists())
           {
               this._logger.trace("Old backup file found.  Deleting...");
               dst.delete();
           }
           
           boolean backupSuccess = src.renameTo(dst);
           String msg = backupSuccess ?
                        "Renamed restart file to " + dst.getName() :
                        "Unable to rename restart file to " + 
                         dst.getName();           
           this._logger.trace(msg);
       }     
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Copies file using standard Java I/O library.
    * @param src Source file
    * @param dst Destination file
    */
   
   protected synchronized void copyFile(File src, File dst) throws IOException
   {
       InputStream is = new FileInputStream(src);
       OutputStream os = new FileOutputStream(dst);
       
       byte[] buffer = new byte[1024];
       int length;
       while ((length = is.read(buffer)) > 0)
       {
           os.write(buffer, 0, length);
       }
       is.close();
       os.close();       
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Copies file using NIO package.
    * @param src Source file
    * @param dst Destination file
    */
   
   protected synchronized void copyFileNIO(File src, File dst) throws IOException
   {
       FileChannel srcChannel = new FileInputStream(src).getChannel();
       FileChannel dstChannel = new FileOutputStream(dst).getChannel();      
       dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
       srcChannel.close();
       dstChannel.close();
   }
   
   //---------------------------------------------------------------------
}