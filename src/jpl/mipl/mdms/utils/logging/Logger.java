/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.utils.logging;

/**
 * The common logging service class. This class uses plugin architecture to
 * bridge custom logging strategy. The plugin class can be specified through the
 * property <b>jpl.mipl.mdms.utils.logging.pluginClass </b>. By default this
 * class uses log4j plugin where log format and appenders can be dynamically
 * configured through a properties/xml configuration file.
 * 
 * @see http://logging.apache.org/log4j
 * 
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: Logger.java,v 1.10 2005/08/25 17:12:05 txh Exp $
 */
public class Logger {

   // name of the logger
   private final String _name;

   // the VM property for logger plugin
   protected static String _PLUGIN_CLASS_PROP = "jpl.mipl.mdms.utils.logging.pluginClass";

   // the default logger plugin is log4j
   protected static String _LOG4J_PLUGIN_CLASS = "jpl.mipl.mdms.utils.logging.Log4JPlugin";

   // the plugin class name
   protected static String _pluginClassName = null;

   // the plugin class reference
   protected static Class _pluginClass = null;

   // the plugin object reference
   protected LoggerPlugin _plugin = null;

   // log file rolling flag values
   public static final int ROLLING_NEVER = 0;
   public static final int ROLLING_DAILY = 1;
   public static final int ROLLING_WEEKLY = 2;
   public static final int ROLLING_MONTHLY = 3;
   public static final int ROLLING_HOURLY = 4;
   public static final int ROLLING_MINUTELY = 5;
   public static final int ROLLING_HALF_DAILY = 6;

   // first initialize the logger to load the plugin class
   static {
      Logger.init();
   }

   /**
    * The singleton factory method to create and access the Logger object
    * instance
    * 
    * @param name the name of the logger, typically the name of the class
    * @return a Logger object instance
    */
   public static Logger getLogger(String name) {
      return new Logger(name);
   }

   /**
    * Hidden constructor to initialize the logger plugin object
    * 
    * @param name the name of the logger
    */
   protected Logger(final String name) {
      this._name = name;
      this._plugin = Logger._getPlugin(this._name);
   }

   /**
    * Hidden method to initialize the logger plugin object
    * 
    * @param name name of the logger instance
    * @return the LoggerPlugin object reference
    */
   protected static LoggerPlugin _getPlugin(String name) {
      LoggerPlugin plugin = null;
      try {
         plugin = (LoggerPlugin) Logger._pluginClass.newInstance();
      } catch (Throwable e) {
         plugin = new NullPlugin();
      }

      try {
         plugin.init(name);
      } catch (Throwable e) {
         System.err.println("Unable to initialize logger plugin " + name);
         plugin = new NullPlugin();
      }
      return plugin;
   }

   /**
    * Method to load and initialize the LoggerPlugin strategy class
    * 
    */
   protected static void init() {
      try {
         if (Logger._pluginClassName == null) {
            Logger._pluginClassName = System.getProperty(
                  Logger._PLUGIN_CLASS_PROP, Logger._LOG4J_PLUGIN_CLASS);
         }

         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         Logger._pluginClass = cl.loadClass(Logger._pluginClassName);
      } catch (ClassNotFoundException e) {
         Logger._pluginClass = jpl.mipl.mdms.utils.logging.NullPlugin.class;
      }
   }

   /**
    * Method to log a severe message. This level designates very severe error
    * events that will presumbly lead the application to abort
    * 
    * @param message the message object
    */
   public void severe(Object message) {
      this._plugin.severe(message);
   }

   /**
    * Method to log a severe message with assocated throwable object (e.g.
    * exception). This level designates very severe error events that will
    * presumbly lead the application to abort
    * 
    * @param message the message object
    * @param t the throwable object, typically an Exception instance
    */
   public void severe(Object message, Throwable t) {
      this._plugin.severe(message, t);
   }

   /**
    * Method to log an error rmessage. This level designates error events that
    * might still allow the application to continue running.
    * 
    * @param message the error message object
    */
   public void error(Object message) {
      this._plugin.error(message);
   }

   /**
    * Method to log an error message with a throwable. This level designates
    * error events that might still allow the application to continue running.
    * 
    * @param message the error message object
    * @param t the throwable object
    */
   public void error(Object message, Throwable t) {
      this._plugin.error(message, t);
   }

   /**
    * Method to log a warning message
    * 
    * @param message the message object
    */
   public void warn(Object message) {
      this._plugin.warn(message);
   }

   /**
    * Method to log a warning message with throwable object (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable object, typically an Exception instance
    */
   public void warn(Object message, Throwable t) {
      this._plugin.warn(message, t);
   }

   /**
    * Method to check if the log level Info is enabled
    * 
    * @return true if log level Info is enabled
    */
   public boolean isInfoEnabled() {
      return this._plugin.isInfoEnabled();
   }

   /**
    * Method to log an info message
    * 
    * @param message the message object
    */
   public void info(Object message) {
      this._plugin.info(message);
   }

   /**
    * Method to log an info message with throwable object (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable object, typically an Exception instance
    */
   public void info(Object message, Throwable t) {
      this._plugin.info(message, t);
   }

   /**
    * Method to check if the log level Debug is enabled
    * 
    * @return true if log level Debug is enabled
    */
   public boolean isDebugEnabled() {
      return this._plugin.isDebugEnabled();
   }

   /**
    * Method to log a debug message
    * 
    * @param message the message object
    */
   public void debug(Object message) {
      this._plugin.debug(message);
   }

   /**
    * Method to log a debug message with throwable object (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable object, typically an Exception instance
    */
   public void debug(Object message, Throwable t) {
      this._plugin.debug(message, t);
   }

   /**
    * Method to return true if execution trace is enabled
    * 
    * @return true if execution trace is enabled
    */
   public boolean isTraceEnabled() {
      return this._plugin.isTraceEnabled();
   }

   /**
    * Method to log a program execution trace message
    * 
    * @param message the message object
    */
   public void trace(Object message) {
      this._plugin.trace(message);
   }

   /**
    * Method to log a program exeuction trace message with throwable object
    * (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable object, typically an Exception instance
    */
   public void trace(Object message, Throwable t) {
      this._plugin.trace(message, t);
   }

   /**
    * Method to return true if benchmark is enabled
    * 
    * @return true if benchmark is enabled
    */
   public boolean isBenchEnabled() {
      return this._plugin.isBenchEnabled();
   }

   /**
    * Method to log a program execution benchmark message
    * 
    * @param message the message object.
    */
   public void bench(Object message) {
      this._plugin.bench(message);
   }

   /**
    * Method to log a program execution benchmark message with throwable object
    * (e.g. exception)
    * 
    * @param message the message object.
    * @param t the throwable object, typically an Exception instance.
    */
   public void bench(Object message, Throwable t) {
      this._plugin.bench(message, t);
   }

   /**
    * Method to set the log file name to direct log messages to a file. If log4j
    * plugin is used, log to a file can be configured via a log4j.properties or
    * an XML configuration file
    * 
    * @param filename the name of the log file
    */
   public void setLogFileName(String filename) {
      this._plugin.setLogFileName(filename);
   }

   /**
    * Method to set log file rolling value
    * 
    * @param rolling the rolling value
    */
   public void setLogFileRolling(int rolling) {
      this._plugin.setLogFileRolling(rolling);
   }

   /**
    * Method to enable log to a file. If a file name is not set, then no file
    * will be written to.
    * 
    */
   public void enableLogToFile() {
      this._plugin.enableLogToFile();
   }

   /**
    * Method to enable log to a file with a log file name
    * 
    * @param filename the log file name
    */
   public void enableLogToFile(String filename) {
      this._plugin.enableLogToFile(filename);
   }

   /**
    * Method to enable log to a file with rollover
    * 
    * @param filename the log file name
    * @param rolling the rolling value
    */
   public void enableLogToFile(String filename, int rolling) {
      this._plugin.enableLogToFile(filename, rolling);
   }

   /**
    * Method to configure email setting, but it will not start sending emails
    * until enableSendMail is called.
    * 
    * @param from the sender email address
    * @param to the receiver email address
    * @param smtpHost the SMTP host name
    * @param subject the email subject
    */
   public void setMail(String from, String to, String smtpHost, String subject) {
      this._plugin.setMail(from, to, smtpHost, subject);
   }

   /**
    * Method to start sending email for each log message for ALL log levels
    */
   public void enableSendMail() {
      this._plugin.enableSendMail();
   }

   /**
    * Method to disable sending email
    */
   public void disableSendMail() {
      this._plugin.disableSendMail();
   }

   /**
    * public void sendMail(String from, String to, String smtpserver, Object
    * subject, Object message) { this._plugin.sendMail(from, to, smtpserver,
    * subject, message); }
    */

   /**
    * Method to send an email message
    * 
    * @param from the sender email address
    * @param to the receiver email address
    * @param smtpserver the smtp host name
    * @param subject the message subject
    * @param message the message object
    * @param t the throwable object, typically an Exception
    */
   /**
    * public void sendMail(String from, String to, String smtpserver, Object
    * subject, Object message, Throwable t) { this._plugin.sendMail(from, to,
    * smtpserver, subject, message, t); }
    */

}