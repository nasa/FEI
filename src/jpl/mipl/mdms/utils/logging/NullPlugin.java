/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.utils.logging;

/**
 * This is a null logger plugin class that implements the required plugin
 * interface. It serves as a fallback plugin when the suggested plugin failed to
 * load.
 * 
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: NullPlugin.java,v 1.10 2009/02/03 00:28:09 ntt Exp $
 */
public class NullPlugin implements LoggerPlugin {
   private String _logFilename;
   private int _rollover = Logger.ROLLING_NEVER;

   public void init(String name) {
      // no-op
   }

   public void init(String name, boolean reinit) {
       // no-op
    }
   
   public void severe(Object message) {
      System.err.println(message);
   }

   public synchronized void severe(Object message, Throwable t) {
      System.err.println(message);
      t.printStackTrace(System.err);
   }

   public void error(Object message) {
      System.err.println(message);
   }

   public synchronized void error(Object message, Throwable t) {
      System.err.println(message);
      t.printStackTrace(System.err);
   }

   public void warn(Object message) {
      System.out.println(message);
   }

   public synchronized void warn(Object message, Throwable t) {
      System.out.println(message);
      t.printStackTrace(System.out);
   }

   public boolean isInfoEnabled() {
      return false;
   }

   public void info(Object message) {
      System.out.println(message);
   }

   public synchronized void info(Object message, Throwable t) {
      System.out.println(message);
      t.printStackTrace(System.out);
   }

   public boolean isDebugEnabled() {
      return false;
   }

   public void debug(Object message) {
      // System.out.println(message);
   }

   public synchronized void debug(Object message, Throwable t) {
      // System.out.println(message);
      // t.printStackTrace(System.out);
   }

   public boolean isTraceEnabled() {
      return false;
   }

   public void trace(Object message) {

   }

   public void trace(Object message, Throwable t) {

   }

   public boolean isBenchEnabled() {
      return false;
   }

   public void bench(Object message) {
//       if (this.isBenchEnabled())
//           System.out.println(message);
   }

   public void bench(Object message, Throwable t) {
//       if (this.isBenchEnabled())
//       {
//           System.out.println(message);
//           t.printStackTrace(System.out);
//       }
   }

   public void setLogFileName(String filename) {
      this._logFilename = filename;
   }

   public void setLogFileRolling(int rollover) {
      this._rollover = rollover;
   }

   public synchronized void enableLogToFile() {
      if (this._logFilename == null)
         return;
   }

   public synchronized void enableLogToFile(String filename) {
      this._logFilename = filename;
      this.enableLogToFile();
   }

   public synchronized void enableLogToFile(String filename, int rollover) {
      this.enableLogToFile(filename);
   }

   public void setMail(String from, String to, String smtpserver, String subject) {

   }

   public void enableSendMail() {

   }

   public void disableSendMail() {

   }

   public void sendMail(String from, String t, String smtpserver,
         Object subject, Object message) {

   }

   public void sendMail(String from, String to, String smtpserver,
         Object subject, Object message, Throwable t) {

   }
}