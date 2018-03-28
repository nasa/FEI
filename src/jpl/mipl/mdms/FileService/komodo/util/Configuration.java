/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights re served
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 *****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Vector;

import jpl.mipl.mdms.FileService.io.FileIO;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * Reads in a configuration file, provides set/get methods for properties. The
 * following environment variable must be specified on the command line of the
 * java interpreter: -DFEI_ROOT -DFEI_VERSION. If they are not set they are
 * defaulted to the empty string
 * 
 * @author G. Turek
 * @version $Id: Configuration.java,v 1.7 2008/03/13 19:32:27 awt Exp $
 */
public class Configuration {
	
   //Subdirectories under komodo.home
   private static final String _LOGDIR = "logs";

   //Subdirectories under komodo.version
   private static final String _CONFDIR = "config";

   //Statics which represent environment variables passed to the JVM.
   public static final String ROOT_ENV = "komodo.home";

   public static final String VERSION_ENV = "komodo.version";

   private static Properties _prop = new Properties();

   private static final String _class = Configuration.class.getName();
   private static final Logger _logger = Logger.getLogger(Configuration._class);

   /**
    * Reads a configuration file of the form key = value
    * 
    * @param confFileName fully qualified path of configuration file
    * @throws IOException when file I/O fail
    * @throws FileNotFoundException when file not found
    */
   public static final void loadConfiguration(String confFileName)
         throws IOException, FileNotFoundException {
      File file = new File(confFileName);
      FileIO.readConfiguration(Configuration._prop, file);
      
   }

   /**
    * Accessor method to get the komodo.home environment variable
    * 
    * @return fully qualified path of the Komodo root directory or an empty
    *         string
    */
   public static final String getRootEnv() {
      return System.getProperty(Configuration.ROOT_ENV, "");
   }

   /**
    * This method performs a lookup of the system log file directory.
    * 
    * @return full path of the system log file directory.
    */
   public static final String getLogsDir() {
      return Configuration.getVersionEnv() + File.separator
            + Configuration._LOGDIR + File.separator;
   }

   /**
    * Get the komodo.version environment variable.
    * 
    * @return fully qualified path to the komodo.version directory or an empty
    *         string.
    */
   public static final String getVersionEnv() {
      return System.getProperty(Configuration.VERSION_ENV, "");
   }

   /**
    * Get the configuration file directory.
    * 
    * @return fully qualified path to the configuration file directory.
    */
   public static final String getConfigDir() {
      return Configuration.getVersionEnv() + File.separator
            + Configuration._CONFDIR + File.separator;
   }

   /**
    * Get a configuration property
    * 
    * @param key the name of the configuration property
    * @return the string value of the configuration property
    */
   public static final String getProperty(String key) {
      return Configuration._prop.getProperty(key);
   }

   /**
    * Get a boolean configuration property
    * 
    * @param key the name of the configuration property
    * @return the boolean value of the configuration property
    */
   public static final boolean getBooleanProperty(String key) {
      return new Boolean(Configuration.getProperty(key)).booleanValue();
   }

   /**
    * Set a configuration property
    * 
    * @param key the name of the configuration property
    * @param value the string value of the configuration property
    */
   public static final void setProperty(String key, String value) {
      Configuration._prop.setProperty(key, value);
   }

   /**
    * Method to return a string contains the list properties and values with the
    * specified output prefix.
    * 
    * @param prefix to add to each record.
    * @return sorted list of key = value formated into a string
    */
   public static final String showPropertiesWithPrefix(String prefix) {
      Vector keys = new Vector(_prop.keySet());
      Collections.sort(keys);
      int ns = keys.size();
      StringBuffer sb = new StringBuffer();
      String s;
      for (int i = 0; i < ns; i++) {
         s = (String) keys.elementAt(i);
         sb.append(prefix + s + " = " + Configuration._prop.get(s) + "\n");
      }
      return sb.toString().trim();
   }

   /**
    * Method to return a formated string with the list of properties and values.
    * 
    * @return sorted list of key = value formated into a string
    */
   public static final String showProperties() {
      Vector keys = new Vector(_prop.keySet());
      Collections.sort(keys);
      int ns = keys.size();
      StringBuffer sb = new StringBuffer();
      String s;
      for (int i = 0; i < ns; i++) {
         s = (String) keys.elementAt(i);
         sb.append(s + " = " + Configuration._prop.get(s) + "\n");
      }
      return sb.toString().trim();
   }

   /**
    * Method to print all properties (system and configuration)
    */
   public static final void printProperties() {
      Configuration._logger.info("\n-----System properties-----"
            + "\nkomodo.home = " + Configuration.getRootEnv()
            + "\nkomodo.version = " + Configuration.getVersionEnv()
            + "\nConfig Dir = " + Configuration.getConfigDir() + "\nLog Dir = "
            + Configuration.getLogsDir() + "\n"
            + Configuration.showProperties()
            + "\n---------------------------\n");
   }
}