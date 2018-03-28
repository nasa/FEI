package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

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

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.util.DateTimeFormatter;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Session cache JavaBean is used to cache session settings
 * information. The cached data can be persisted in XML format to assist
 * restarting of session. The cache data is 
 * stored with file name: 
 * &lt;servergroup&gt;.&lt;filetype&gt;.&lt;subtype&gt;.&lt;restart|notify&gt;. 
 * It is created per file type.
 * 
 *   <PRE>
 *   Copyright 2008, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2008.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 09/05/0008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SessionCache.java,v 1.8 2016/09/29 23:30:37 ntt Exp $
 *
 */

public class SessionCache 
{

   private static final Object _syncLock = new Object();    

   private String currentDirectory;

   private int options;
   
   private String sessionOnOffOptionsStr;
   
   private String cacheFilename;
   
   private String dateTimeFormat;
   
   
   //the name of the file this object will be stored
   private static String CACHE_FILENAME = ".savannah.session"; 
      
   //the local CCSDS time of the last query time
   private String _lastQueryTime = null;

   private boolean _backupCommits = true;
   
   private Logger _logger = Logger.getLogger(
                                   SessionCache.class.getName());

    //---------------------------------------------------------------------
   
   /**
    * Factory method to create a session cache.
    //* @param outputDir the file output location. 
    //* @param options Int representing all options off or on
    * @return SessionCache built from existing cache file, 
    *         or a new instance
    */
   
   public static SessionCache restoreFromCache() 
   {
      Logger logger = Logger.getLogger(SessionCache.class.getName());
      boolean corruptCacheFlag = false;
      SessionCache sesCache = null;
      
      //retrieve filepath for cache file
      String cacheFilepath = buildCacheFilePath();
      File cacheFile = new File(cacheFilepath);
      
      
      //verify that directory containing cache exists, if not, make it so.
      File directory = cacheFile.getParentFile();
      if (!directory.exists())
      {
          directory.mkdirs();
      }
              
      // if the cache file already exists then just restore and return
      if (cacheFile.canRead())
      {
          sesCache = SessionCache.restoreFromCache(cacheFilepath);
          if (sesCache == null)
          {
              corruptCacheFlag = true;          
              logger.warn("Unable to restore from existing cache file.");
          }
          else
          {
              sesCache.setCacheFilename(cacheFilepath);              
          }
          
          if (sesCache != null && !sesCache.isWellformed())
          {
              corruptCacheFlag = true;   
              sesCache         = null;
              logger.warn("Unable to restore from existing cache file. " +
              		      "File is missing required information.");
          }
          else
          {
              logger.trace("Restored from cache.");
          }
      } 
      
      //---------------------------
      
      //if null cache, try the backup file
      if (sesCache == null)      
      {
          //try loading from backup cache
          String bakFilename = cacheFilepath + Constants.BACKUPEXTENSION;
          cacheFile = new File(bakFilename);
          
          if (cacheFile.canRead())
          {          
              logger.trace("Trying to restore from backup cache.");     
              sesCache = SessionCache.restoreFromCache(bakFilename);              
              if (sesCache == null)
              {
                  corruptCacheFlag = true;              
                  logger.warn("Unable to restore from existing backup cache file.");
              }
              else
              {
                  sesCache.setCacheFilename(cacheFilepath);
              }
              
              if (!sesCache.isWellformed())
              {
                  corruptCacheFlag = true;   
                  sesCache         = null;
                  logger.warn("Unable to restore from existing backup cache file. " +
                              "File is missing required information.");                  
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
      
      // if cache still null after checking backup, create a new one
      if (sesCache == null)
      {
          logger.warn("Unable to restore from backup session cache. Creating a new " +
          		      "cache object.");
          sesCache = new SessionCache(cacheFilepath);
      }
      
      
//      //if we've got a cache, set state and return it
//      if (sesCache != null)
//      {
//          sesCache.setOutputDir(outputDir);
//          sesCache.setCacheFilename(cacheFilepath);
//          sesCache.setOptions(options);
//          return sesCache;
//      }

      //---------------------------
      
//      if (!corruptCacheFlag)
//          logger.trace("Classic cache file does not exist. Creating " +
//                       "a new cache object.");
//      else {
//          logger.warn("Due to cache file corruption, creating " +
//                      "new cache object.");
//      
//          sesCache = new SessionCache(cacheFilepath);
//      }
      
     try {
         sesCache.commit();
      } catch (SessionException e) {
         logger.error("Unable to persist session restart info to " +
                      cacheFilepath);
      }
      return sesCache;

      //---------------------------
      
   }

   //---------------------------------------------------------------------
   
   public void setOptions(int options)
   {
       this.options = options;
       this.sessionOnOffOptionsStr = this.options + "";
   }
   
   //---------------------------------------------------------------------
   
   public int getOptions()
   {
       return this.options;
   }
   
   //---------------------------------------------------------------------
   
   public void setOption(int option, boolean value) 
   {
       //boolean oldValue = this.getOption(option);
       if (value)
          this.options |= option;
       else
          this.options &= ~option;   
       
       this.sessionOnOffOptionsStr = this.options + "";
    }

    //---------------------------------------------------------------------
    
    /**
     * Method to return the option value
     * 
     * @param option the option
     * @return boolean value for option.
     */
    public boolean getOption(int option) 
    {
       return (this.options & option) > 0 ? true : false;
    }
   
   //---------------------------------------------------------------------

   /**
    * Factory method to create a client restart cache object from a cache file
    * @param cachefile the cache file
    * @return the client restart cache object
    */
   
   static SessionCache restoreFromCache(String cachefile) 
   {       
       //--------------------------
       
       File file = new File(cachefile);     
       SessionCache crc = null;
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
               crc = (SessionCache) decoder.readObject();
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
           Logger.getLogger(SessionCache.class.getName()).warn(
                   "Error occurred while attempting to restore session cache" +
                   " from existing file '"+ file.getAbsolutePath()+"'.  " +
                   "Ignoring file...");       
       }
       
       //--------------------------
       
       return (error[0]) ? null : crc;
    }

    //---------------------------------------------------------------------
   
   /**
    * Returns path session cache based on the parameters. 
    * @return Path of session cache file, or null if indeterminable.
    */
   
   static String buildCacheFilePath()
   {       
       String cacheFilename = null;
       
       String userHome = System.getProperty("user.home");
       
       cacheFilename = userHome + File.separator + Constants.RESTARTDIR + File.separator + CACHE_FILENAME; 

       return cacheFilename;
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
   
    public static boolean canRestoreFromCache()
    {        
        String[] pathsToCheck = new String[2];
        String path;
        boolean canRestore = false;
        
        //-------------------------
        
        //populate the array of files to check for restore
        
        //entry 0: cache file 
        pathsToCheck[0] = buildCacheFilePath();
        //entry 1: backup cache file
        if (pathsToCheck[0] != null)
            pathsToCheck[1] = pathsToCheck[0] + Constants.BACKUPEXTENSION;
    
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
    
   public SessionCache() 
   {
      // need this for JavaBean encoding and decoding
       this.currentDirectory = System.getProperty("user.dir");
       this.setOptions(0);
       this.cacheFilename = null;
       this.dateTimeFormat = DateTimeFormatter.DEFAULT_FORMAT;
       //this.cacheFilename = buildCacheFilePath();        
   }

   //---------------------------------------------------------------------
   
   /**
    * Constructor
    * @param outputDir Output directory
    * @param options Session options
    * @param cacheFilename the cache file name
    */
   
    public SessionCache(String cacheFilename) 
    {
        this.cacheFilename = cacheFilename;
        this.dateTimeFormat = DateTimeFormatter.DEFAULT_FORMAT;
    }
    
   //---------------------------------------------------------------------

   /**
    * Constructor
    * @param outputDir Output directory
    * @param options Session options
    * @param cacheFilename the cache file name
    */
   
    public SessionCache(String outputDir, int options, String cacheFilename) 
    {
        this.currentDirectory = outputDir;
        this.setOptions(options);
        this.cacheFilename = cacheFilename;
        this.dateTimeFormat = DateTimeFormatter.DEFAULT_FORMAT;
        //this._lastQueryTime = DateTimeUtil.getDateCCSDSAString(
        //                           DateTimeUtil.getLocalDate());
    }

   //---------------------------------------------------------------------
   
    //---------------------------------------------------------------------

    /**
     * Constructor
     * @param outputDir Output directory
     * @param options Session options
     * @param cacheFilename the cache file name
     * @param dateTimeFormat the date format string
     */
    
     public SessionCache(String outputDir, int options, String cacheFilename,
                         String dateTimeFormat) 
     {
         this.currentDirectory = outputDir;
         this.setOptions(options);
         this.cacheFilename = cacheFilename;
         this.dateTimeFormat = dateTimeFormat;
         if (this.dateTimeFormat == null)
             this.dateTimeFormat = DateTimeFormatter.DEFAULT_FORMAT;
         //this._lastQueryTime = DateTimeUtil.getDateCCSDSAString(
         //                           DateTimeUtil.getLocalDate());
     }
     
   //---------------------------------------------------------------------
   
   public boolean isWellformed()
   {
       boolean passes = true;
       
       //sometimes, the serialization messes up the cache filename
       //field, so we check that it is valid.
       if (this.cacheFilename == null)
           passes = false;
       
       return passes;
   }
     
   //---------------------------------------------------------------------
   
   /**
    * Accessor method to return the cache file name
    * @return the cache file name
    */
   
   public String getCacheFilename() 
   {
      return this.cacheFilename;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the cache file name
    * @param cacheFilename the cache file name
    */
   
   public void setCacheFilename(String cacheFilename) 
   {
      this.cacheFilename = cacheFilename;
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
    * Accessor to obtain the date/time format expression
    * @return the date/time format
    */
   
   public String getDateTimeFormat() 
   {
      return this.dateTimeFormat;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the expression pattern for date/time format  
    * @param dateTimeFormat the date/time expression
    */
   
   public void setDateTimeFormat(String dateTimeFormat) 
   {
      this.dateTimeFormat = dateTimeFormat;
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
   
   //---------------------------------------------------------------------
   
   /**
    * Accessor to file output directory
    * @return the output directory
    */
   
   public String getCurrentDirectory() 
   {
      return this.currentDirectory;
   }

   //---------------------------------------------------------------------
   
   /**
    * Method to set the output directory 
    * @param outputDir the output directory
    */
   
   public void setCurrentDirectory(String outputDir) 
   {
      this.currentDirectory = outputDir;
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
       
           if (!isWellformed())
           {
               this._logger.warn("Cache file is not well-formed. Aborting commit.");
               throw new SessionException(
                       "Cache file was not well-formed. Commit aborted.",
                       Constants.RESTARTFILEERR);
           }
           
           //--------------------------
           
           File file = new File(this.cacheFilename);
           
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
//               if (encoder != null)
//                   encoder.close(); 
               if (fos != null)
               {
                   try { 
                       fos.getFD().sync(); 
                   } catch (IOException ioEx) {
                       this._logger.warn("Error occurred while attempting to sync " +
                          		         "file "+cacheFilename+": "+ioEx.getMessage());
                     
                   }
               }
               if (encoder != null)
                   encoder.close(); 
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
       File file = new File(this.cacheFilename);
       File bakFile = new File(this.cacheFilename + Constants.BACKUPEXTENSION);              
       
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