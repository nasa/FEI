/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.help.ClientHelp;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * Komodo client initialization: parse command line arguments, initialize help
 * 
 * @author G. Turek, T. Huang
 * @version $Id: BaseClient.java,v 1.14 2016/09/29 23:24:56 ntt Exp $
 */
public class BaseClient {
   protected String _batchFile = null;
   protected boolean _silent = false;
   protected boolean _exitAfterBatch = false;
   protected boolean _ssl = true;
   private Logger _logger = Logger.getLogger(BaseClient.class.getName());

   /**
    * Constructor, accepts string array object which contains command line
    * arguments
    * 
    * @param args The command line arguments
    * @throws IOException when I/O failure
    * @throws Exception when general failure
    */
   public BaseClient(String[] args) throws IOException, Exception {
      this._parseArgs(args);
      this._initializeHelp();
      this._createRegistry();
   }

   /**
    * Internal method to initialize user help by processing help XML input
    * files.
    */
   private void _initializeHelp() {
      // Find help file and schema in jar file, must use
      // getSystemResourceAsStream
      // because it's more portable than getSystemResource
      InputStream xml = ClassLoader
            .getSystemResourceAsStream("jpl/mipl/mdms/FileService/komodo/client/resources/clienthelp.xml");

      InputStream schema = ClassLoader
            .getSystemResourceAsStream("jpl/mipl/mdms/FileService/komodo/client/resources/clienthelp.xsd");

      if (schema != null) {
         this._logger.debug("Using help schema from jar file.");
         ClientHelp help = new ClientHelp(xml, schema);
      } else {
         this._logger
               .info("Unable to locate help schema, will attempt to use schema URI in help file");
         ClientHelp help = new ClientHelp(xml, null);
      }
   }

   /**
    * Create dir if necessary for future user registry use.
    */
   private void _createRegistry() {
      String restartDir = System.getProperty(Constants.PROPERTY_RESTART_DIR);
      if (restartDir == null)
         restartDir = System.getProperty("user.home") + File.separator
               + Constants.RESTARTDIR;
      else
         restartDir = restartDir + File.separator + Constants.RESTARTDIR;

      File f = new File(restartDir);
      File tmp = null;

      if (f.exists()) {
         // We're done
         if (f.isDirectory())
            return;

         // Test if have old time registry file
         if (f.isFile()) {
            tmp = new File(".tmp");
            if (!f.renameTo(tmp)) {
               //Might as well
               this._logger.error("Could not rename file " + restartDir
                     + " to .tmp");
               return;
            }
         }
      }

      if (!f.mkdir()) {
         this._logger.error("Could not create " + restartDir);
         return;
      }

      // If we had renamed .tmp rename to new settings file
      if (tmp != null) {
         File f2 = new File(restartDir + File.separator + "settings");
         if (!tmp.renameTo(f2))
            this._logger.error("Could not rename .tmp to " + restartDir
                  + File.separator + "settings");
      }

      //If a legacy restart dir exists, move it under the new restart dir.
      f = new File(restartDir + Constants.LEGACY_RESTART_SUFFIX);
      if (f.exists()) {
         String s;
         File[] ff = f.listFiles();
         if (ff == null)
             ff = new File[0];
         
         for (int i = 0; i < ff.length; i++) {
            s = ff[i].getName();
            this._logger.error(s);
            if (!ff[i].renameTo(new File(restartDir + File.separator + s)))
               this._logger.error("Could not move (rename) " + s + " from "
                     + restartDir + Constants.LEGACY_RESTART_SUFFIX + " to "
                     + restartDir);
         }
      }
   }

   /**
    * Parse startup arguments (if any)
    * 
    * @param args input command line arguments
    */
   private void _parseArgs(String[] args) {
      int numArgs = args.length;
      int index = 0;
      while (index < numArgs) {
         if (args[index].equals("-h")) {
            // output help
            this._usage();
         } else if (args[index].equals("-b")) {
            // enable batch mode
            this._batchFile = args[++index];
            this._silent = true;
            this._exitAfterBatch = true;
            index++;
         } else if (args[index].equals("-u")) {
            // enable unsecure communication
            this._ssl = false;
            index++;
         } else {
            this._usage();
         }
      }
   }

   /**
    * Issues error message in case of bad start up invocation
    */
   private void _usage() {
      this._logger.info("BaseClient invocation options:");
      this._logger
            .info("  -b <batch file>  : use <batch file> (must contain login sequence)");
      this._logger.info("  -h               : this listing");
      this._logger.info("  -u               : use unsecure socket");
      System.exit(0);
   }
}