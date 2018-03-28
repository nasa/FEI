/**
 * JUnit test case for Komodo (FEI5) Client API (changeType())
 */
package jpl.mipl.mdms.test.FileService.komodo.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.client.CMD;
import jpl.mipl.mdms.FileService.komodo.client.UtilCmdParser;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * JUnit test case for Komodo (FEI5) Client Util Parsing
 * <p>
 * Copyright 2004, California Institute of Technology. <br>
 * ALL RIGHTS RESERVED. <br>
 * U.S. Government Sponsorship acknowledge. 6/15/2004. <br>
 * MIPL Data Management System (MDMS).
 * <p>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: UtilCmdParserTest.java,v 1.6 2005/01/16 09:17:40 txh Exp $
 */
public class UtilCmdParserTest extends TestCase {
   private final static String OPTIONS_FILENAME = "ucptest";

   private Properties _props = System.getProperties();

   private UtilCmdParser _parser = null;
   private boolean _verbose = false;

   private File _optionsFile = null;
   private FileWriter _fileWriter = null;
   private int _testId = -1;

   String[][] options = new String[][] {
         {},

         {},

         { "dev:type1", OPTIONS_FILENAME, "\n", "dev:type1", OPTIONS_FILENAME,
               "comment", "\"this is number 2\"", "\n", "dev:type1",
               OPTIONS_FILENAME, "crc" },

         {
               "invoke",
               "ps",
               "-elf",
               "\n",
               "invokeExitOnError",
               "\n",
               "logFile",
               File.separator + "some" + File.separator + "random"
                     + File.separator + "file.poo", "\n", "mailMessageFrom",
               "me@joeblow.com", "\n", "mailMessageTo",
               "him@there.com, her@here.com", "\n", "mailReportAt",
               "12:34 am, 11:42:31 pm", "\n", "mailReportTo",
               "them@hereNthere.com", "\n", "mailSMTPHost",
               "neutino.jpl.nasa.gov:30402", "\n", },

         {},

         {} };

   String[][] args = new String[][] {
         { "help" },

         { "dev:type1", "myFile.ext", "comment", "this is a comment", "crc",
               "autodelete" },

         { "using", OPTIONS_FILENAME },

         { "restart", "output", "/output/dir", "mailsmtphost",
               "sonicboom.jpl.nasa.gov", "using", OPTIONS_FILENAME },

         {},

         { "dev:type1", "myFile.ext", "replace", "version" } };

   //----------------------------------------------------------------------

   /**
    * Constructor
    * 
    * @param name the test suite name
    */
   public UtilCmdParserTest(String name) {
      super(name);
   }

   //----------------------------------------------------------------------

   /**
    * Override the TestCase setUp method to initialize test environment.
    * 
    * @throws Exception when general failure
    */
   public void setUp() throws Exception {
      this._optionsFile = null;
      this._fileWriter = null;
      this._parser = new UtilCmdParser();
   }

   //----------------------------------------------------------------------

   /**
    * Override parent tearDown method to cleanup after testing. Removes
    * temporary files if existing.
    * 
    * @throws Exception when general operation fail
    */
   public void tearDown() throws Exception {

      //reset file writer
      if (this._fileWriter != null) {
         this._fileWriter.close();
         this._fileWriter = null;
      }

      //reset options file
      if (this._optionsFile != null && this._optionsFile.exists()) {
         this._optionsFile.delete();
         this._optionsFile = null;
      }
      this._parser = null;
   }

   //----------------------------------------------------------------------

   public void testAddWithHelp() {

      System.out.println("Running testAddWithHelp():");
      _testId = 0;
      Object value;

      for (int i = 0; i < args[_testId].length; ++i)
         System.out.println(">>> " + args[_testId][i]);

      try {
         this._parser.parse(Constants.ADDFILE, args[_testId]);
      } catch (ParseException pEx) {
         pEx.printStackTrace();
         System.out.print(this._parser);
         Assert.fail();
      }

      //if (parser.hasValue(CMD.FILETYPE))
      Assert.assertNull(this._parser.getValue(CMD.FILETYPE));
      //if (parser.hasValue(CMD.FILETYPE))
      Assert.assertNotNull(this._parser.getValue(CMD.HELP));
   }

   //----------------------------------------------------------------------

   public void testEmpty() {
      System.out.println("Running testEmpty():");
      _testId = 1;
   }

   //----------------------------------------------------------------------

   public void testAddWithOptionsFile() {
      System.out.println("Running testAddWithOptionsFile():");
      _testId = 2;
      Object value;

      createOptionsFile(_testId);
      if (this._optionsFile != null) {
         for (int i = 0; i < args[_testId].length; ++i) {
            if (OPTIONS_FILENAME.equals(args[_testId][i]))
               args[_testId][i] = this._optionsFile.getAbsolutePath();
         }
      }

      try {
         this._parser.parse(Constants.ADDFILE, args[_testId]);
      } catch (ParseException pEx) {
         pEx.printStackTrace();
         Assert.fail();
      }

      //if (parser.hasValue(CMD.FILETYPE))
      Assert.assertEquals(this._optionsFile.getAbsolutePath(), this._parser
            .getOptionsFilename());
      System.out.println("Iterations = " + this._parser.iterations());
      Assert.assertEquals(3, this._parser.iterations());

      for (int i = 0; i < 2; ++i) {
         Assert.assertNotNull(this._parser.getCurrentArguments());
         Assert.assertEquals(true, this._parser.hasNext());
         this._parser.advance();
      }
      Assert.assertNotNull(this._parser.getCurrentArguments());
      Assert.assertEquals(false, this._parser.hasNext());
      this._parser.advance();
      Assert.assertNull(this._parser.getCurrentArguments());

      this._parser.reset();
      System.out.println(this._parser);
   }

   //----------------------------------------------------------------------

   public void testSubscribeWithOptionsFile() {
      System.out.println("Running testSubscribeWithOptionsFile():");
      _testId = 3;
      Object value;

      createOptionsFile(_testId);
      if (this._optionsFile != null) {
         for (int i = 0; i < args[_testId].length; ++i) {
            if (OPTIONS_FILENAME.equals(args[_testId][i]))
               args[_testId][i] = this._optionsFile.getAbsolutePath();
         }
      }

      try {
         this._parser.parse(Constants.AUTOGETFILES, args[_testId]);
      } catch (ParseException pEx) {
         pEx.printStackTrace();
         Assert.fail();
      }

      //System.out.println("Iterations = " + this._parser.iterations());
      Assert.assertEquals(1, this._parser.iterations());
      Assert.assertEquals("sonicboom.jpl.nasa.gov", this._parser
            .getValue(CMD.MAILSMTPHOST));
      Assert.assertEquals("/output/dir", this._parser.getValue(CMD.OUTPUT));
      Assert.assertEquals("them@hereNthere.com", this._parser
            .getValue(CMD.MAILREPORTTO));

      this._parser.reset();
      //System.out.println(this._parser);

   }

   //----------------------------------------------------------------------

   public void testCredList() {
      System.out.println("Running testCredList():");
      _testId = 4;
      Object value;

      try {
         this._parser.parse(Constants.CREDLIST, args[_testId]);
      } catch (ParseException pEx) {
         pEx.printStackTrace();
         Assert.fail();
      }

      //System.out.println("Iterations = " + this._parser.iterations());
      Assert.assertEquals(1, this._parser.iterations());

      this._parser.reset();
      //System.out.println(this._parser);
   }

   //----------------------------------------------------------------------

   public void testGetWithConflictingOptions() {
      System.out.println("Running testGetWithConflictingOptions():");
      _testId = 5;
      Object value;

      try {
         this._parser.parse(Constants.GETFILES, args[_testId]);
      } catch (ParseException pEx) {
         //pEx.printStackTrace();
         //Assert.();
      }

      //System.out.println("Iterations = " + this._parser.iterations());
      Assert.assertEquals(0, this._parser.iterations());
      Assert.assertEquals(true, this._parser.printHelp());
      this._parser.reset();
      //System.out.println(this._parser);
   }

   //----------------------------------------------------------------------

   protected void createOptionsFile(int index) {
      if (index < 0 || index >= options.length)
         Assert.fail("Index out of bounds of options array");

      String[] contents = options[index];

      try {
         //create temp file
         this._optionsFile = File.createTempFile(OPTIONS_FILENAME, ".opt");

         //create writer
         this._fileWriter = new FileWriter(this._optionsFile);

         //write to the file
         for (int i = 0; i < contents.length; ++i) {
            if (OPTIONS_FILENAME.equals(contents[i]))
               this._fileWriter.write(this._optionsFile.getAbsolutePath());
            else
               this._fileWriter.write(contents[i]);

            if (!contents[i].equals("\n"))
               this._fileWriter.write(" ");
         }

         this._fileWriter.flush();
         this._fileWriter.close();
         this._fileWriter = null;

         //System.out.println("File located at
         // :"+this._optionsFile.getAbsolutePath());
         //System.exit(1);
      } catch (IOException ioEx) {
         Assert.fail(ioEx.getMessage());
      }
   }

   //----------------------------------------------------------------------

   /**
    * The main method to launch the JUnit TestRunner
    * 
    * @param args gui|text
    */
   public static void main(String[] args) {
      String usage = "Usage: java [-classpath ...] \\\n"
            + UtilCmdParserTest.class.getName() + " gui|text";
      if (args.length < 2) {
         System.out.println(usage);
         return;
      }
      if (args[0].compareToIgnoreCase("gui") == 0) {
         junit.swingui.TestRunner.run(UtilCmdParserTest.class);
      } else if (args[0].compareToIgnoreCase("text") == 0) {
         junit.textui.TestRunner.run(UtilCmdParserTest.class);
      } else
         System.out.println(usage);
   }

   //----------------------------------------------------------------------
}