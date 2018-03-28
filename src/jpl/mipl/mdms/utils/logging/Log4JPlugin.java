/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.utils.logging;

import java.net.URL;

import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.net.SMTPAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;
import org.apache.log4j.varia.LevelRangeFilter;

/**
 * This class implements the Log4J plugin inteface.
 * 
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @author N. Toole {Nicholas.T.Toole@jpl.nasa.gov}
 * @author A. Tinio {Adrian.Tinio@jpl.nasa.gov}
 * @version $Id: Log4JPlugin.java,v 1.17 2009/09/21 17:46:44 ntt Exp $
 */
public class Log4JPlugin implements LoggerPlugin 
{
    
   private transient org.apache.log4j.Logger _logger;

   private String _logFilename = null;
   private int _rolling = Logger.ROLLING_NEVER;

   private FileAppender _fileAppender = null;
   private SMTPAppender _emailAppender = null;
   private final String _LAYOUT = "%m%n";
   
   private final String BASIC_LAYOUT   = "%m%n";
   private final String COMPLEX_LAYOUT = "%-5p [%t] (%F:%L) - %m%n";

   private final String _name = Logger.class.getName();
   
   private static boolean __configured = false; 

   //----------------------------------------------------------------------
   
   /**
    * Examines properties for the log configuration and delay.
    * Parses configuration file setting the Log4J framework.
    * Also examines MDMS debug property and will set logger
    * accordingly.
    */
   
   private synchronized static void loadConfiguration()
   {
       String configDelay = System.getProperty(
                            LoggerPlugin.LOG_CONFIG_DELAY_PROPERTY);
       ConfigFileURLResolver resolver = new ConfigFileURLResolver();
       URL cfgURL = null;

       try {
           cfgURL = resolver.getLoggingConfigFile();
       } catch (SessionException se) {
           System.err.println("[LOG_CONF_ERR] Error accessing log config file: "+se.getMessage());
       }
       
       if (cfgURL != null) 
       {
            if (configDelay != null) 
            {
                Long delay = null;
                try {
                    delay = new Long(configDelay);
                } catch (NumberFormatException e) {
                    System.err.println("[LOG_CONF_ERR] Invalid delay value");
                }

                if (delay == null || delay.longValue() < 1)
                {
                    org.apache.log4j.xml.DOMConfigurator.configure(cfgURL);
                }
                else
                {
                    org.apache.log4j.xml.DOMConfigurator.configureAndWatch(
                            cfgURL.getFile(), delay.longValue() * 60);
                }
            }
            else
            {
                org.apache.log4j.xml.DOMConfigurator.configure(cfgURL);
            }
        }

        if (System.getProperty(LoggerPlugin.ENABLE_DEBUG_PROPERTY) != null)
        {
            org.apache.log4j.Logger rootLogger = LogManager.getRootLogger();
            if (rootLogger.getLevel().isGreaterOrEqual(Level.DEBUG))
                rootLogger.setLevel(Level.DEBUG);
        }      
        
        Log4JPlugin.__configured = true;
   }
   
   /**
    * Returns true if the logger has already been configured.
    * @return True if configured, false otherwise
    */
   
   private synchronized static boolean isConfigured()
   {
       return Log4JPlugin.__configured;
   }
   
   //----------------------------------------------------------------------
   
   /**
    * Method to initialize the Log4J logger using its DOMConfigurator to parse
    * the log4j XML configuration file
    * @param name the name of the logger.
    * @param reinit if true, the configuration will be reloaded, else
    *        configuration is only loaded if not already.
    */

   public synchronized void init(String name, boolean reinit)
   {

       //check if the plugin configuration has been loaded.
       //if not, or if reinit is true, then load the configuration
       if (!Log4JPlugin.isConfigured() || reinit)
       {                
           Log4JPlugin.loadConfiguration();           
       }
       
       //--------------------------
       
       this._logger = LogManager.getLogger(name);       
   }
   
   /**
    * Method to init an instance of the Log4J logger.  This is a
    * convenience method that is the same as calling init(name, false).
    * 
    * @param name the name of the logger.
    */
   
   public void init(String name) 
   {       
       init(name, false);
   }

   public void severe(Object message) {
      this._logger.log(this._name, Level.FATAL, message, null);
   }

   public void severe(Object message, Throwable t) {
      this._logger.log(this._name, Level.FATAL, message, t);
   }

   public void error(Object message) {
      this._logger.log(this._name, Level.ERROR, message, null);
   }

   public void error(Object message, Throwable t) {
      this._logger.log(this._name, Level.ERROR, message, t);
   }

   public void warn(Object message) {
      this._logger.log(this._name, Level.WARN, message, null);
   }

   public void warn(Object message, Throwable t) {
      this._logger.log(this._name, Level.WARN, message, t);
   }

   public boolean isInfoEnabled() {
      return this._logger.isEnabledFor(Level.INFO);
   }

   public void info(Object message) {
      this._logger.log(this._name, Level.INFO, message, null);
   }

   public void info(Object message, Throwable t) {
      this._logger.log(this._name, Level.INFO, message, t);
   }

   public boolean isDebugEnabled() {
      return this._logger.isEnabledFor(Level.DEBUG);
   }

   public void debug(Object message) {
      this._logger.log(this._name, Level.DEBUG, message, null);
   }

   public void debug(Object message, Throwable t) {
      this._logger.log(this._name, Level.DEBUG, message, t);
   }

   public boolean isTraceEnabled() {
       return this._logger.isEnabledFor(Level.TRACE);
//      if (this._logger.isEnabledFor(Level.TRACE) == false)
//         return false;
//      return Level.TRACE.isGreaterOrEqual(this._logger
//            .getEffectiveLevel());
   }

   public void trace(Object message) {
      this._logger.log(this._name, Level.TRACE, message, null);
   }

   public void trace(Object message, Throwable t) {
      this._logger.log(this._name, Level.TRACE, message, t);
   }

   public boolean isBenchEnabled() {
      if (this._logger.isEnabledFor(L4JCustomLevel.BENCH) == false)
         return false;
      return L4JCustomLevel.BENCH.isGreaterOrEqual(this._logger
            .getEffectiveLevel());
   }

   public void bench(Object message) {
      this._logger.log(this._name, L4JCustomLevel.BENCH, message, null);
   }

   public void bench(Object message, Throwable t) {
      this._logger.log(this._name, L4JCustomLevel.BENCH, message, t);
   }

   public synchronized void setLogFileName(String filename) {
      this._logFilename = filename;
   }

   public synchronized void setLogFileRolling(int rolling) {
      this._rolling = rolling;
   }

   /**
    * Method to log message to an external log file. This implementation does
    * not provide any message formatting to the input message. For server
    * processes that needs to log data to a file, it should be done using the
    * external log4j configuration file to customize log message layout.
    */
   public synchronized void enableLogToFile() {
      
       if (this._logFilename == null)
         return;
      
       try {   
           
           org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
           
           //if file appender doesn't exist, then create it
           if (this._fileAppender == null)
           {
               //if log level of root is INFO or higher, then use standard
               //simple layout.  Otherwise, use the more informative layout
               //commonly used for DEBUG and TRACE
               
               String layoutPattern = BASIC_LAYOUT;
               if (!root.getLevel().isGreaterOrEqual(Level.INFO))
                   layoutPattern = COMPLEX_LAYOUT;
                   
               
               switch (this._rolling) {
               case Logger.ROLLING_NEVER:
                   _fileAppender = new FileAppender(new PatternLayout(layoutPattern),
                                       this._logFilename);
                  break;
               case Logger.ROLLING_DAILY:
                   _fileAppender = new DailyRollingFileAppender(new PatternLayout(
                                       layoutPattern), this._logFilename, 
                                       "'.'yyyy-MM-dd");
                  break;
               case Logger.ROLLING_WEEKLY:
                   _fileAppender = new DailyRollingFileAppender(new PatternLayout(
                                       layoutPattern), this._logFilename, 
                                       "'.'yyyy-ww");
                  break;
               case Logger.ROLLING_MONTHLY:
                   _fileAppender = new DailyRollingFileAppender(new PatternLayout(
                                       layoutPattern), this._logFilename, 
                                       "'.'yyyy-MM");
                  break;
               case Logger.ROLLING_HOURLY:
                   _fileAppender = new DailyRollingFileAppender(new PatternLayout(
                                       layoutPattern), this._logFilename, 
                                       "'.'yyyy-MM-dd-HH");
                  break;
               case Logger.ROLLING_MINUTELY:
                   _fileAppender = new DailyRollingFileAppender(new PatternLayout(
                                       layoutPattern), this._logFilename, 
                                       "'.'yyyy-MM-dd-HH-mm");
                  break;
               case Logger.ROLLING_HALF_DAILY:
                   _fileAppender = new DailyRollingFileAppender(new PatternLayout(
                                       layoutPattern), this._logFilename, 
                                       "'.'yyyy-MM-dd-a");
                  break;
               default:
                   _fileAppender = new FileAppender(new PatternLayout(layoutPattern),
                                                    this._logFilename);
                  break;
               }
               
               //prepare state of appender
               _fileAppender.setImmediateFlush(true);
               _fileAppender.setAppend(true);
//               if (System.getProperty(LoggerPlugin.ENABLE_DEBUG_PROPERTY) != null)         
//                   _fileAppender.setThreshold(Level.DEBUG);
//               else
//                   _fileAppender.setThreshold(Level.INFO);
               
               //allow it to activate
               _fileAppender.activateOptions();
           }
           
//           //theory - file appender needs to be attached to root so that everything goes there
//           //maybe consider creating itermediary logger 'jpl.mipl.mdms' as a common parent
//           org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
           
           //logger adds appender only if not already present
           root.addAppender(_fileAppender);
           
           
                   
           //this._logger.addAppender(appender);
           
       } catch (java.io.IOException e) {
         this.severe("Unable to create log file [" + this._logFilename + "]",
                     e);
      }
   }

   public synchronized void enableLogToFile(String filename) {
      this.enableLogToFile(filename, Logger.ROLLING_NEVER);
   }

   public synchronized void enableLogToFile(String filename, int rolling) {
      this._logFilename = filename;
      this._rolling = rolling;
      
      
      this.enableLogToFile();
   }

   public synchronized void setMail(String from, String to, String smtpserver,
         String subject) {
      this._emailAppender = new SMTPAppender(new EmailEvaluator());
      this._emailAppender.setFrom(from);
      this._emailAppender.setTo(to);
      this._emailAppender.setSubject(subject);
      this._emailAppender.setSMTPHost(smtpserver);
      this._emailAppender.setBufferSize(512);
      this._emailAppender.setLayout(new org.apache.log4j.PatternLayout(
                                    this._LAYOUT));      
      this._emailAppender.activateOptions();
   }

   public synchronized void enableSendMail() {
      if (this._emailAppender == null)
         return;
      this._logger.addAppender(this._emailAppender);
      this._logger.setAdditivity(false);
   }

   public synchronized void disableSendMail() {
      if (this._emailAppender == null)
         return;
      this._logger.removeAppender(this._emailAppender);
      this._logger.setAdditivity(true);
   }

   public void sendMail(String from, String to, String smtpserver,
         Object subject, Object message) {
      SMTPAppender emailAppender = new SMTPAppender(new EmailEvaluator());
      emailAppender.setFrom(from);
      emailAppender.setTo(to);
      emailAppender.setSubject(subject.toString());
      emailAppender.setSMTPHost(smtpserver);
      emailAppender.setBufferSize(512);
      emailAppender.setLayout(new org.apache.log4j.PatternLayout(this._LAYOUT));
      LevelRangeFilter filter = new LevelRangeFilter();
      filter.setLevelMin(Level.INFO);
      filter.setLevelMax(Level.INFO);
      emailAppender.addFilter(filter);
      emailAppender.activateOptions();
      this.error(message);
      this._logger.removeAppender(emailAppender);
   }

   public void sendMail(String from, String to, String smtpserver,
         Object subject, Object message, Throwable t) {
      SMTPAppender emailAppender = new SMTPAppender(new EmailEvaluator());
      emailAppender.setFrom(from);
      emailAppender.setTo(to);
      emailAppender.setSubject(subject.toString());
      emailAppender.setSMTPHost(smtpserver);
      emailAppender.setBufferSize(512);
      emailAppender.setLayout(new org.apache.log4j.PatternLayout(this._LAYOUT));
      emailAppender.activateOptions();
      this.info(message, t);
      this._logger.removeAppender(emailAppender);
   }

   class EmailEvaluator implements TriggeringEventEvaluator {
      public boolean isTriggeringEvent(LoggingEvent event) {
         return true;
      }
   }

}