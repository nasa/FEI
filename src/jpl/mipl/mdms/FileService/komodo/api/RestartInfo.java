/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Date;

/**
 * This JavaBean keeps track of restart information that can be presisted on the
 * client local disk to assist restart transfer of data file.
 * 
 * @deprecated
 * 
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: RestartInfo.java,v 1.6 2004/10/27 00:52:33 txh Exp $
 */
public class RestartInfo {

   // the hash table used to keep track of resume transfer information
   // for each file that was partically transfered.
   private Properties _fileList = new Properties();

   private Properties _vftList = new Properties();

   // this timestamp is ued for restart getAfter session that is used to
   // pull the server for new files available.
   private long _restartTime = new Date().getTime();

   // this timestamp is used for restart showAfter session that is used to
   // query for file metadata on new files available.
   private long _listRestartTime = new Date().getTime();

   // that shall be an associated file type for each instance of this class.
   private String _fileType = null;

   // name of the JavaBean persistent file.
   private String _persistFile = null;

   /**
    * A default constructor to help serialization of class instance.
    */
   public RestartInfo() {
      // no-op
   }

   /**
    * Constructor used by the komodo client to create new restart objects
    * 
    * @param fileType the associated file type
    * @param persistFile the persistent file name
    */
   public RestartInfo(String fileType, String persistFile) {
      this._fileType = fileType;
      this._persistFile = persistFile;
   }

   /**
    * Accessor method to obtain the name of the persistent file.
    * 
    * @return the name of the persistent file.
    */
   public String getPersistFile() {
      return this._persistFile;
   }

   /**
    * Method to set the name of the persistent file.
    * 
    * @param persistFile name of the persistent file.
    */
   public void setPersistFile(String persistFile) {
      this._persistFile = persistFile;
   }

   /**
    * Accessor method to obtain the file type name.
    * 
    * @return the file type name
    */
   public String getFileType() {
      return this._fileType;
   }

   /**
    * Method to set the associated file type.
    * 
    * @param fileType the file type name
    */
   public void setFileType(String fileType) {
      this._fileType = fileType;
   }

   /**
    * Accessor method to obtain the hash table for file names.
    * 
    * @return the file names hash table.
    */
   public Properties getFileList() {
      return this._fileList;
   }

   /**
    * Method to set the file list hash table.
    * 
    * @param fileList the hash table reference.
    */
   public void setFileList(Properties fileList) {
      this._fileList = fileList;
   }
   
   public Properties getVFTList() {
      return this._vftList;
   }
   
   public void setVFTList(Properties vftList) {
      this._vftList = vftList;
   }

   /**
    * Accessor method to obtain the restart time.
    * 
    * @return the restart timestamp
    */
   public long getRestartTime() {
      return this._restartTime;
   }

   /**
    * Method to set the restart time
    * 
    * @param restartTime the restart timestamp.
    */
   public void setRestartTime(long restartTime) {
      this._restartTime = restartTime;
   }

   /**
    * Method to obtain the file query restart time.
    * 
    * @return the file query restart time stamp
    */
   public long getListRestartTime() {
      return this._listRestartTime;
   }

   /**
    * Method to set the file query restart time
    * 
    * @param restartTime the file query restart time.
    */
   public void setListRestartTime(long restartTime) {
      this._listRestartTime = restartTime;
   }

   /**
    * Method to create a new entry to the file name hash table.
    * 
    * @param filename the file name
    * @param size the file size
    * @param datetime the file registred time on the server.
    */
   public void addResume(String filename, long size, String datetime) {
      this._fileList
            .setProperty(filename, Long.toString(size) + " " + datetime);
   }

   /**
    * Method to create a new entry to the file name hash table.
    * 
    * @param filename the file name
    * @param vft the name of the VFT
    * @param size the file size
    * @param datetime the file registered time on the server
    */
   public void addResume(String filename, String vft, long size, String datetime) {
      this.addResume(filename, size, datetime);
      this._vftList.setProperty(filename, vft);
   }

   /**
    * Method to remove an entry from the hash table.
    * 
    * @param filename the file name to be removed
    * @return the value string for the associated file name
    */
   public String removeResume(String filename) {

      Enumeration e = this._fileList.propertyNames();
      String value = null;

      while (e.hasMoreElements()) {
         String name = (String) e.nextElement();
         if (filename.equals(name)) {
            value = (String) this._fileList.getProperty(name);
            this._fileList.remove(name);
            break;
         }
      }

      // walk through the VFT list. If found, append the vft name
      // with the actual file name.
      e = this._vftList.propertyNames();
      while (e.hasMoreElements()) {
         String name = (String) e.nextElement();
         if (filename.equals(name)) {
            value += "\t" + (String) this._vftList.getProperty(name);
            this._vftList.remove(name);
            break;
         }
      }

      return value;
   }

   /**
    * Method to retrieve the resume string for the associated file.
    * 
    * @param filename the file name
    * @param filepath the location of the file on the client side.
    * @return the formated restart string for the file.
    */

   public String getResume(String filename, String filepath) {
      long offset = 0;

      String value = this._fileList.getProperty(filename);
      String vft = this._vftList.getProperty(filename);

      /**
       * search cache tree for filename info. if file found in cache tree, then
       * find out the current file size.
       */
      if (value != null) {
         File file;
         if (vft != null)
            file = new File(filepath + vft);
         else
            file = new File(filepath + filename);
         if (!file.isDirectory() && file.exists()) {
            offset = file.length();
         }
         return offset + " " + value;
      }
      return offset + " 0 0";
   }

   /**
    * Method to persist the RestartInfo object
    * 
    * @throws SessionException when unable to cache the object instance.
    */
   public void commit() throws SessionException {
      try {
         XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(
               new FileOutputStream(this._persistFile)));
         encoder.writeObject(this);
         encoder.close();
      } catch (IOException ioe) {
         throw new SessionException(
               "IOException while writing to restart file",
               Constants.RESTARTFILEERR);
      }
   }
}