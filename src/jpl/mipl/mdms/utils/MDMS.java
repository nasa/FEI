/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.text.MessageFormat;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * This is a general utility class for MDMS Java developments. It provides
 * simple utility methods to simplify development.
 * 
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: MDMS.java,v 1.6 2008/12/02 23:00:24 awt Exp $
 */
public abstract class MDMS {

   /**
    * The global MDMS logger.
    */
   private static Logger _logger;

   /**
    * The default logger name.
    */
   public static final String LOGGER_NAME = "jpl.mipl.mdms";

   /**
    * debug flag
    */
   private static boolean _debug = false;

   /**
    * Need to have this initialize block to make sure our logger is configured
    * to at least to print something out.
    */
   static {
      MDMS._logger = Logger.getLogger(MDMS.LOGGER_NAME);
      Handler[] handlers = MDMS._logger.getParent().getHandlers();
      // if logging to the terminal, we want to remove all that
      // time formated outputs and just output the raw message.
      for (int i = 0; i < handlers.length; ++i) {
         if (handlers[i] instanceof ConsoleHandler) {
            handlers[i].setLevel(Level.SEVERE);
            handlers[i].setFormatter(new RawFormatter());
         }
      }
      Handler stdoutHandler = new StreamHandler(System.out, new RawFormatter());
      stdoutHandler.setLevel(Level.INFO);
      MDMS._logger.addHandler(stdoutHandler);
   }

   /**
    * The global OK return status.
    */
   public static final int OK = 0;

   /**
    * The global failure return status.
    */
   public static final int FAILED = -1;

   /**
    * The global DONE return flag.
    */
   public static final int DONE = 0;

   /**
    * The default thread pool size.
    */
   public static final int DEFAULT_POOL_SIZE = 10;

   /**
    * The default port number.
    */
   public static final int DEFAULT_PORT = 5555;

   /**
    * The default timeout: 2 seconds.
    */
   public static final int DEFAULT_TIMEOUT = 2000;

   /**
    * The default buffer size.
    */
   public static final int BUFSIZ = 8192;

   /* time constants in unit of milliseconds */

   /**
    * One second in milliseconds
    */
   public static final long ONE_SECOND = 1000;

   /**
    * One minute in milliseconds
    */
   public static final long ONE_MINUTE = 60 * MDMS.ONE_SECOND;

   /**
    * One hour in milliseconds
    */
   public static final long ONE_HOUR = 60 * MDMS.ONE_MINUTE;

   /**
    * One day in milliseconds
    */
   public static final long ONE_DAY = 24 * MDMS.ONE_HOUR;

   /**
    * One week in milliseconds
    */
   public static final long ONE_WEEK = 7 * ONE_DAY;

   /**
    * One year in milliseconds. The same formula is used in
    * java.util.GregorianCalandar class.
    */
   public static final long ONE_YEAR = (long) (365.2425 * MDMS.ONE_DAY);

   /**
    * One century in milliseconds.
    */
   public static final long ONE_CENTURY = 100 * MDMS.ONE_YEAR;

   /**
    * The default MemoryHandler buffer size.
    */
   public static final int HANDLER_BUF_SIZE = 1000;

   /**
    * Hidden constructor to prevent instance creation on this class.
    */
   private MDMS() {
      // no-op.
   }

   /**
    * Method to register a log handler to the global logger.
    * 
    * @param handler reference to a Log Handler.
    * @see java.util.logging.Handler
    */
   public static final void registerLogHandler(Handler handler) {
      MDMS._logger.addHandler(handler);
   }

   /**
    * Method to remove a registered log handler.
    * 
    * @param handler reference to a Log Handler.
    * @see java.util.logging.Handler
    */
   public static final void removeLogHandler(Handler handler) {
      MDMS._logger.removeHandler(handler);
   }

   /**
    * Method to access the internal logger.
    * 
    * @return the logger reference.
    * @see java.util.logging.Logger
    */
   public static final Logger getLogger() {
      return MDMS._logger;
   }

   /**
    * Activates MDMS.DEBUG to output all debug messages.
    */
   public static final void enableDebug() {
      MDMS._debug = true;
      MDMS._logger.setLevel(Level.FINEST);
   }

   /**
    * De-activates MDMS.DEBUG, so all calls to MDMS.DEBUG would not output any
    * messages.
    */
   public static final void disableDebug() {
      MDMS.FLUSH();
      MDMS._logger.setLevel(Level.INFO);
      MDMS._debug = false;
   }

   /**
    * De-activates all logging.
    *  
    */
   public static final void disableLogger() {
      MDMS.FLUSH();
      MDMS._logger.setLevel(Level.OFF);
   }

   /**
    * Wrapper method to output exception messages.
    * 
    * @param e The input exception.
    */
   public static final void ERROR(Exception e) {
      MDMS._logger.severe(e.toString());
   }

   /**
    * Wrapper to output error messages.
    * 
    * @param s The input message string.
    */
   public static final void ERROR(String s) {
      MDMS._logger.severe(s);
   }

   /**
    * Wrapper to output error messages and return the proper error value.
    * 
    * @param s The input message string.
    * @param errorVal The return value.
    * @return The error value.
    */
   public static final int ERROR_RETURN(String s, int errorVal) {
      MDMS._logger.severe(s);
      return errorVal;
   }

   /**
    * Wrapper to log the input message. This method will out any input string
    * messages even without enableing debug.
    * 
    * @param s The input message string.
    */
   public static final void LOG(String s) {
      MDMS._logger.info(s);
   }

   /**
    * Wrapper to output debug message if debug is enabled.
    * 
    * @param s The input message string.
    */
   public static final void DEBUG(String s) {
      if (MDMS._debug)
         MDMS._logger.fine(s);
   }

   /**
    * Wrapper to flush all buffered outputs.
    */
   public static final void FLUSH() {
      Handler[] handlers = MDMS._logger.getHandlers();
      for (int i = 0; i < handlers.length; ++i)
         handlers[i].flush();
   }

   /**
    * Wrapper to set the input bit(s) in the input bit mask.
    * 
    * @param WORD The input bit mask.
    * @param BITS The input bits to be set.
    * @return The result mask after setting the input bits.
    */
   public static final long SET_BITS(long WORD, long BITS) {
      return WORD | BITS;
   }

   /**
    * Wrapper to clear the input bit(s) in the input bit mask.
    * 
    * @param WORD The input bit mask.
    * @param BITS The input bits to be cleared.
    * @return The result mask after clearing the input bits.
    */
   public static final long CLR_BITS(long WORD, long BITS) {
      return WORD & ~BITS;
   }

   /**
    * Wrapper to check to see if the input bit(s) are set in the input bit mask.
    * 
    * @param WORD The input bit mask.
    * @param BIT The input bits to be verified within the mask.
    * @return true, if the input bits are enabled within the mask.
    */
   public static final boolean BIT_ENABLED(long WORD, long BIT) {
      return (WORD & BIT) != 0;
   }

   /**
    * Wrapper to check to see if the input bit(s) are unset in the input bit
    * mask.
    * 
    * @param WORD The input bit mask.
    * @param BIT The input bits to be verified within the mask.
    * @return true, if the input bits are disabled within the mask.
    */
   public static final boolean BIT_DISABLED(long WORD, long BIT) {
      return (WORD & BIT) == 0;
   }
   
}