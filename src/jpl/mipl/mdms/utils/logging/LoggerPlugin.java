/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.utils.logging;

import jpl.mipl.mdms.utils.Constants;


/**
 * This interface specifies the required methods for a logging strategy. It is
 * the bridge interface used by the master Logger to bridge application message
 * to the specific logging implementation.
 * 
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: LoggerPlugin.java,v 1.8 2007/02/12 19:21:00 ntt Exp $
 */
public interface LoggerPlugin {

   // VM property used to specify logger-spcific configuration file.
   public static final String LOG_CONFIG_PROPERTY = Constants.PROPERTY_LOGGING_CONFIG;

   // VM property used to configure logging service to periodically check for
   // changes to the configuration file.
   public static final String LOG_CONFIG_DELAY_PROPERTY = Constants.PROPERTY_LOG_CONFIG_DELAY;

   // the global debug property
   public static final String ENABLE_DEBUG_PROPERTY = Constants.PROPERTY_ENABLE_DEBUG;

   /**
    * Method called by the master Logger at startup to initialize the plugin
    * Same as calling <code>init(name, <b>false<b>);</code>.
    * @param name name of the logger
    */
   public void init(String name);

   /**
    * Method called by the master Logger at startup to initialize the plugin
    * or to reinitialize it (reloading configuration, etc)
    * @param name name of the logger
    * @param reinitialize If true, then logger plugin is reinitialized from
    *        its configuration.  If false, then plugin is only init'ed if
    *        not already done so. 
    */
   public void init(String name, boolean reinitialize);
   
   /**
    * Method to log a severe message. This level is designated for very severe
    * error that will presumably lead the application to abort
    * 
    * @param message the severe message object
    */
   public void severe(Object message);

   /**
    * Method to log a severe message with a throwable (e.g. exception). This
    * level is designated for very severe error that will preseumably lead the
    * application to abort
    * 
    * @param message the severe message object
    * @param t the throwable, typically an Exception
    */
   public void severe(Object message, Throwable t);

   /**
    * Method to log an error message. This level is designated for error events
    * that might still allow the application to continue running.
    * 
    * @param message the error message
    */
   public void error(Object message);

   /**
    * Method to log an error message with throwable. This level is designated
    * for error events that might still allow the application to continue
    * running.
    * 
    * @param message the error message object
    * @param t the throwable, typically an Exception
    */
   public void error(Object message, Throwable t);

   /**
    * Method to log a warning message. This level is designated for potentially
    * harmful situations
    * 
    * @param message the message object
    */
   public void warn(Object message);

   /**
    * Method to log a warning message with a throwable (e.g. exception). This
    * level is designated for potentially harmful situations
    * 
    * @param message the message object
    * @param t the throwable, typically an Exception
    */
   public void warn(Object message, Throwable t);

   /**
    * Method to check if log level Info is enabled
    * 
    * @return true if log Info level is enabled
    */
   public boolean isInfoEnabled();

   /**
    * Method to log an info message
    * 
    * @param message the message object
    */
   public void info(Object message);

   /**
    * Method to log an info message with a throwable (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable, typically an Exception
    */
   public void info(Object message, Throwable t);

   /**
    * Method to check if log level Debug is enabled
    * 
    * @return true if log Debug level is enabled
    */
   public boolean isDebugEnabled();

   /**
    * Method to log a debug message
    * 
    * @param message the message object
    */
   public void debug(Object message);

   /**
    * Method to log a debug message with throwable (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable, typically an Exception
    */
   public void debug(Object message, Throwable t);

   /**
    * Method to return true if tracing is enabled.
    * 
    * @return true if trace is enabled.
    */
   public boolean isTraceEnabled();

   /**
    * Method to trace program execution.
    * 
    * @param message the message object
    */
   public void trace(Object message);

   /**
    * Method to trace program execution with throwable (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable, typically an Exception
    */
   public void trace(Object message, Throwable t);

   /**
    * Method to return true if bench is eanbled.
    * 
    * @return true if bench is enabled
    */
   public boolean isBenchEnabled();

   /**
    * Method to load benchmark message
    * 
    * @param message the message object.
    */
   public void bench(Object message);

   /**
    * Method to log benchmark message with throwable (e.g. exception)
    * 
    * @param message the message object
    * @param t the throwable, typically an Exception
    */
   public void bench(Object message, Throwable t);

   /**
    * Method to set the log file name
    * 
    * @param filename the log file name
    */
   public void setLogFileName(String filename);

   /**
    * Method to set the log file rollover value ROLLING_NEVER, ROLLING_DAILY,
    * ROLLING_WEEKLY, ROLLING_MONTHLY, ROLLING_HOURLY, ROLLING_MINUTELY,
    * ROLLING_HALF_DAILY
    * 
    * @param rolling the rolling value
    */
   public void setLogFileRolling(int rolling);

   /**
    * Method to enable log to a file
    * 
    */
   public void enableLogToFile();

   /**
    * Method to enable log to a file on the specified file name
    * 
    * @param filename the log file name
    */
   public void enableLogToFile(String filename);

   /**
    * Method to enable log to a file on the specified file name. The log file
    * will be rolling daily/weekly/monthly
    * 
    * @param filename the log file name
    * @param rolling the rolling value ROLLING_NEVER, ROLLING_DAILY,
    *           ROLLING_WEEKLY, ROLLING_MONTHLY, ROLLING_HOURLY,
    *           ROLLING_MINUTELY, ROLLING_HALF_DAILY
    */
   public void enableLogToFile(String filename, int rolling);

   /**
    * Method to configure email settings, but it will not begin sending emails
    * until enableSendMail is called.
    * 
    * @param from the sender email address
    * @param to the receiver email address
    * @param smtpserver the SMTP host name
    * @param subject the email subject
    */
   public void setMail(String from, String to, String smtpserver, String subject);

   /**
    * Method to start sending email on all level of logging.
    */
   public void enableSendMail();

   /*
    * Method to enable sending email.
    */
   public void disableSendMail();

   /**
    * Method to send an email message
    * 
    * @param from the sender email address
    * @param to the receiver email address
    * @param smtpserver the smtp host name
    * @param subject the message subject
    * @param message the message object
    */
   public void sendMail(String from, String to, String smtpserver,
         Object subject, Object message);

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
   public void sendMail(String from, String to, String smtpserver,
         Object subject, Object message, Throwable t);

}