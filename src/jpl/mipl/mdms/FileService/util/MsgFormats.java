/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights re served
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 *****************************************************************************/

package jpl.mipl.mdms.FileService.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Properties;

import jpl.mipl.mdms.FileService.io.FileIO;

/**
 * Builds an array of MsgFormat from statements found in a configuration
 * file.  The array statements are identified by an array of keywords, the
 * values of which are read from the configuration file.  The constructor
 * creates an array of compiled statements for keywords found in the array
 * of Strings.  Calls to makeString return formatted strings by argument
 * substitution.  See java.txt.MsgFormat for more information.
 *
 * @author J. Jacobson
 * @version $Id: MsgFormats.java,v 1.5 2003/09/26 00:12:10 txh Exp $
 */
public class MsgFormats {

   private Hashtable _formDictionary;
   private static boolean _debug = false;

   /**
    * Reads a message format file of the format key = value, to populate
    * an array of compiled message formats.
    *
    * @param pattern array of patterns...
    * @param confFileName - fully qualified path of MessageFormats file
    * @throws IOException when I/O failure
    * @throws ParseException when XML parsing or validating failure
    * @throws FileNotFoundException when file not found
    */
   public MsgFormats(String[] pattern, String confFileName)
      throws IOException, ParseException, FileNotFoundException {
      Properties propHandle = new Properties();
      File fileHandle = new File(confFileName);

      // Create lookup table for compiled format statements.
      FileIO.readConfiguration(propHandle, fileHandle);
      try {
         this._compile(propHandle, pattern);
      } catch (ParseException e) {
         throw new ParseException(
            e.getMessage() + " in file \"" + confFileName + "\".",
            e.getErrorOffset());
      }
   }

   /**
    * Read a message format input stream.  This is the prefered method to
    * access resource files within a JAR file.  The stream format consists
    * of key = value, to populate an array of compiled message formats.
    *
    * @param pattern array of patterns...
    * @param is the input stream to the properties.
    * @throws IOException when I/O failure
    * @throws ParseException when XML parsing or validating failure
    */
   public MsgFormats(String[] pattern, InputStream is)
      throws IOException, ParseException {

      Properties propHandle = new Properties();
      propHandle.load(is);

      // Create lookup table for compiled format statements;
      this._compile(propHandle, pattern);
   }

   /**
    * @param pattern The pattern derived from the regular expression
    * @param props The Properties
    * @throws IOException when I/O failure
    * @throws ParseException when XML parsing or validating failure
    */
   public MsgFormats(String[] pattern, Properties props)
      throws IOException, ParseException {

      // Create lookup table for compiled format statements;
      this._compile(props, pattern);
   }

   /**
    * private method to compile the message strings
    * 
    * @param propHandle Properties handle
    * @param pattern The pattern array
    * @throws ParseException when XML parsing or validating failure
    */
   private void _compile(Properties propHandle, String[] pattern)
      throws ParseException {

      MessageFormat tmp;
      String nextPattern;

      this._formDictionary = new Hashtable(pattern.length);

      // Loop through the statement array and compile the message
      // pattern strings.
      for (int i = 0; i < pattern.length; i++) {
         nextPattern = propHandle.getProperty(pattern[i]);
         if (nextPattern == null) {
            throw new ParseException(
               "Statement \"" + pattern[i] + "\" not found" + "\".",
               i);
         }
         if (MsgFormats._debug)
            System.err.println("Pattern: " + nextPattern);
         tmp = new MessageFormat(nextPattern);
         if (tmp == null) {
            throw new ParseException(
               "Statement \"" + pattern[i] + "\" did not compile.",
               i);
         }
         // Put the parsed format statement into our lookup table.
         this._formDictionary.put(pattern[i], tmp);
      }
   }

   /**
    * Format a string, using a pre-compiled format statement.
    * @param key the index for lookup
    * @param args an array of objects to format
    * @return the associated string format
    */
   public String makeString(String key, Object[] args) {
      MessageFormat mf = (MessageFormat) this._formDictionary.get(key);
      Format[] formats = mf.getFormats();

      for (int i = 0; i < formats.length; ++i) {
         if (formats[i] == null && args[i] instanceof Number) {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setGroupingUsed(false);
            formats[i] = nf;
         }
      }
      mf.setFormats(formats);
      return mf.format(args);
      // If this fails, it is a programmer error.
      //return ((MessageFormat) this._formDictionary.get(key)).format(args);
   }

   /**
    * Driver program.  Get patterns from a configuration file, and test them.
    *
    * @param args the optional command line arguments
    */
   public static void main(String[] args) {

      String[] keys =
         {
            "db.stmt.setRole",
            "db.stmt.getServerFileTypes",
            "db.stmt.addFile",
            "db.stmt.lockFile",
            "db.stmt.checkForUser",
            "db.stmt.releaseFileLock",
            "db.stmt.updateComment",
            "db.stmt.updateArchive",
            "db.stmt.deleteFile",
            "db.stmt.listFilesForTypeShort",
            "db.stmt.listFilesForTypeShortByRegExp",
            "db.stmt.createDomainFile",
            };

      try {
         // Find the expected keys in the file, and compile their patterns.
         MsgFormats formats = new MsgFormats(keys, "patterns.txt");

         // Now, try some.
         String[] roleArgs = { "JeffsRole" };
         System.err.println(
            "setRole: " + formats.makeString(keys[0], roleArgs));

         Object[] sftArgs = { "ServerName" };
         System.err.println(
            "getServerFileTypes: " + formats.makeString(keys[1], sftArgs));

         Object[] addArgs =
            {
               new Integer(12),
               "fileName",
               "Joe Astro",
               new Integer(512),
               new Integer(1),
               new Integer(1232134),
               "This is a note for the file." };
         System.err.println("addFile: " + formats.makeString(keys[2], addArgs));

         Object[] lockArgs = { "FileName", new Integer(12), new Integer(5)};
         System.err.println(
            "lockFile: " + formats.makeString("db.stmt.lockFile", lockArgs));
         // Another way to do it.

         Object[] userArgs = { "jdj", "jdjpasswd" };
         System.err.println(
            "checkForUser: " + formats.makeString(keys[4], userArgs));

         Object[] releaseArgs = { new Integer(12)};
         System.err.println(
            "releaseFileLock: " + formats.makeString(keys[5], releaseArgs));

         Object[] commentArgs =
            { new Integer(121), "filename", "This is a comment." };
         System.err.println(
            "updateComment: " + formats.makeString(keys[6], commentArgs));

         Object[] archiveArgs =
            { new Integer(121), "filename", "Archived to /dev/null." };
         System.err.println(
            "updateArchive: " + formats.makeString(keys[7], archiveArgs));

         Object[] delArgs = { new Integer(121)};
         System.err.println(
            "deleteFile: " + formats.makeString(keys[8], delArgs));

         Object[] listArgs = { new Integer(2), "regular expression" };
         System.err.println(
            "listFilesForTypeShort: " + formats.makeString(keys[9], listArgs));
         System.err.println(
            "listFilesForTypeShortRegExp: "
               + formats.makeString(keys[10], listArgs));

         System.err.println(
            "createDomainFile: " + formats.makeString(keys[11], null));
      } catch (IOException e) {
         e.printStackTrace();
      } catch (ParseException pe) {
         pe.printStackTrace();
      }
   }
}
