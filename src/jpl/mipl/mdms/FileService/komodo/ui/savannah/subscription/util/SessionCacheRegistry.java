package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;


/**
 * <b>Purpose:</b>
 * Session cache JavaBean is used to cache session settings
 * information. The cached data can be persisted in XML format to assist
 * restarting of session. The cache data is 
 * stored with file name:  ${USERHOME}/.komodo/.savannah.subscriptions
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
 * 09/05/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SessionCacheRegistry.java,v 1.3 2008/11/03 19:30:40 ntt Exp $
 *
 */

public class SessionCacheRegistry 
{

   private static final Object _syncLock = new Object();    

   private String cacheFilename;
   
   //maps string to a map-of-string-to-string
   private Hashtable<String, Properties> sessionsToPersist = 
                        new Hashtable<String, Properties>();
   
   
   //the name of the file this object will be stored
   private static String CACHE_FILENAME = ".savannah.subscriptions"; 
      
   //the local CCSDS time of the last query time
   private String _lastQueryTime = null;

   private boolean _backupCommits = true;
   
   private Logger _logger = Logger.getLogger(
                                   SessionCacheRegistry.class.getName());

    //---------------------------------------------------------------------
   
   /**
    * Factory method to create a session cache.
    * @return SessionCache built from existing cache file, 
    *         or a new instance
    */
   
   public static SessionCacheRegistry restoreFromCache() 
   {
      Logger logger = Logger.getLogger(SessionCacheRegistry.class.getName());
      boolean corruptCacheFlag = false;
      SessionCacheRegistry sesCache = null;
      
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
          sesCache = SessionCacheRegistry.restoreFromCache(cacheFilepath);
          if (sesCache == null)
          {
              corruptCacheFlag = true;          
              logger.warn("Unable to restore from existing subscription cache file.");
          }
          else
              logger.trace("Restored from cache.");     
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
              logger.trace("Trying to restore from backup subscription cache.");     
              sesCache = SessionCacheRegistry.restoreFromCache(bakFilename);              
              if (sesCache == null)
              {
                  corruptCacheFlag = true;              
                  logger.warn("Unable to restore from existing backup subscription cache file.");
              }
              else
              {
                  if (corruptCacheFlag)
                      logger.warn("Original subscription cache file corrupted. " +
                                   "Restored from backup cache.");
                  else
                      logger.warn("Restored from backup subscription cache.");
              }
          }         
      }
      
      //---------------------------
      
      // if cache still null after checking backup, create a new one
      if (sesCache == null)
      {
          logger.warn("Unable to restore from backup subscription cache. Creating a new " +
          		      "cache object.");
          sesCache = new SessionCacheRegistry(cacheFilepath);
      }
      
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
   
   public void addSessionSettings(String id, Properties options)
   {
       this.sessionsToPersist.put(id, options);
   }

   //---------------------------------------------------------------------
   
   public Properties getSessionSettings(String id)
   {
       Properties map = null;
       
       map = this.sessionsToPersist.get(id);
       
       return map;
   }

   //---------------------------------------------------------------------
   
   public boolean removeSessionSettings(String id)
   {
       return (this.sessionsToPersist.remove(id) != null);
   }
   
   //---------------------------------------------------------------------
   
   public void removeAllSessionSettings()
   {
       this.sessionsToPersist.clear();
   }
   
   //---------------------------------------------------------------------

   
   /**
    * Returns true if registry has no entries, false otherwise.
    * @return True if empty, false otherwise
    */
   
   public boolean isEmpty()
   {
       return this.sessionsToPersist.isEmpty();
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Returns a list of session ids managed by this registry.
    * @return List of session ids, can be empty.
    */
   public List<String> getSessionIds()
   {
       List<String> ids = new ArrayList<String>();
       
       Set<String> set = this.sessionsToPersist.keySet();
       ids.addAll(set);
           
       return ids;
   }
   
   
   
   //---------------------------------------------------------------------
    

   
   //---------------------------------------------------------------------

   /**
    * Factory method to create a client restart cache object from a cache file
    * @param cachefile the cache file
    * @return the client restart cache object
    */
   
   static SessionCacheRegistry restoreFromCache(String cachefile) 
   {       
       //--------------------------
       
       File file = new File(cachefile);     
       SessionCacheRegistry scr = null;
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
               scr = (SessionCacheRegistry) decoder.readObject();
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
           Logger.getLogger(SessionCacheRegistry.class.getName()).warn(
                   "Error occurred while attempting to restore session cache" +
                   " from existing file '"+ file.getAbsolutePath()+"'.  " +
                   "Ignoring file...");       
       }
       
       //--------------------------
       
       return (error[0]) ? null : scr;
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
    
   public SessionCacheRegistry() 
   {
      // need this for JavaBean encoding and decoding
       //this.currentDirectory = System.getProperty("user.dir");
       this.cacheFilename = null;
   }

   //---------------------------------------------------------------------
   
   /**
    * Constructor
    * @param outputDir Output directory
    * @param options Session options
    * @param cacheFilename the cache file name
    */
   
    public SessionCacheRegistry(String cacheFilename) 
    {
        this.cacheFilename = cacheFilename;
    }

   //---------------------------------------------------------------------
   
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

   public void setSessionsToPersist(Hashtable<String, Properties>  allSettings)
   {
       this.sessionsToPersist = allSettings;
   }
  
   public Hashtable<String, Properties> getSessionsToPersist()
   {
       return this.sessionsToPersist;
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