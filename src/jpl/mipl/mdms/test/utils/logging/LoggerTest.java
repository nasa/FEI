/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.test.utils.logging;

import jpl.mipl.mdms.utils.logging.Logger;

import junit.framework.TestCase;

import java.util.Date;

/**
 * This is a logger test class that uses the junit test framework
 * 
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: LoggerTest.java,v 1.3 2004/09/23 23:08:08 txh Exp $
 */
public class LoggerTest extends TestCase {

   private Logger _logger;

   public LoggerTest() {
      this(LoggerTest.class.getName());
   }

   public LoggerTest(String name) {
      super(name);
      this._logger = Logger.getLogger(name);
   }

   protected void setUp() {
      this._logger.info("Set up started.");
   }

   protected void tearDown() {
      this._logger.info("Tear down finished.");
   }

   public void testLevels() {

      if (this._logger.isDebugEnabled())
         this._logger.debug("Debug enabled");
      this._logger.debug("A debug message on " + new Date());
      this._logger.debug("Debug exp: ", new Exception("EXCEPTION EXCEPTION"));

      if (this._logger.isInfoEnabled())
         this._logger.info("Info enabled");
      this._logger.info("An info message on " + new Date());
      this._logger.info("Info exp: ", new Exception("EXCEPTION EXCEPTION"));

      this._logger.warn("A warning message on " + new Date());
      this._logger.warn("Warn exp: ", new Exception("EXCEPTION EXCEPTION"));

      this._logger.severe("A fatal message on " + new Date());
      this._logger.severe("Severe exp: ", new Exception("EXCEPTION EXCEPTION"));
   }

   public void testLogFile() {
      String filename = System.getProperty("java.io.tmpdir")
            + System.getProperty("file.separator") + "test.log";
      this._logger.info("Test logging to file [" + filename + "]");
      this._logger.enableLogToFile(filename);
      this.testLevels();
   }

   public static void main(String[] args) {
      junit.swingui.TestRunner.run(LoggerTest.class);
   }
}