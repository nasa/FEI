/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.test.FileService.komodo.api;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.Session;

/**
 * Komodo driver class.
 *
 * Test driver for Komodo class.
 * @author J. Jacobson
 * @version $Id: Driver.java,v 1.2 2003/02/20 23:32:52 txh Exp $
 */
public class Driver {
   private static String _username = null;
   private static String _password = null;
   private static String _domainfile = null;
   private static String _fileToAdd = null;
   private static String _fileToDelete = null;
   private static String _fileToGet = null;
   private static String _fileToGet_2 = null;
   private static String _type = null;

   /**
    * Issues error message in case of bad start up invocation
    */
   private static void _usage() {
      System.out.println("Komodo driver options:");
      System.out.println("    -f domain file (required)");
      System.out.println("    -u user name   (required)");
      System.out.println("    -p password    (required)");
      System.out.println("    -t type");
      System.out.println("    -a fileToAdd)");
      System.out.println("    -g fileToGet)");
      System.out.println("    -z fileToGet_2)");
      System.out.println("    -d fileToDelete)");
      System.exit(1);
   }

   /**
    * Parse startup arguments (if any)
    *
    * @param args the input command line arguments
    */
   private static void _parseArgs(String[] args) {
      int numArgs = args.length;
      if (numArgs == 0)
         Driver._usage();

      int index = 0;
      while (index < args.length) {
         if (args[index].equals("-h")) {
            Driver._usage();

         } else if (args[index].equals("-f")) {
            Driver._domainfile = args[++index];
            index++;
         } else if (args[index].equals("-u")) {
            Driver._username = args[++index];
            index++;
         } else if (args[index].equals("-p")) {
            Driver._password = args[++index];
            index++;
         } else if (args[index].equals("-t")) {
            Driver._type = args[++index];
            index++;
         } else if (args[index].equals("-a")) {
            Driver._fileToAdd = args[++index];
            index++;
         } else if (args[index].equals("-d")) {
            Driver._fileToDelete = args[++index];
            index++;
         } else if (args[index].equals("-g")) {
            Driver._fileToGet = args[++index];
            index++;
         } else if (args[index].equals("-z")) {
            Driver._fileToGet_2 = args[++index];
            index++;
         }
      }
      if (Driver._domainfile == null)
         Driver._usage();
      if (Driver._username == null)
         Driver._usage();
      if (Driver._password == null)
         Driver._usage();
   }

   /**
    * main
    * @param args Command line arguments.
    * @throws java.lang.Exception general exception
    */
   public static void main(String[] args) throws Exception {
      FileType test = null;
      Result result;
      Session session = null;

      System.err.println("Test Session.INSECURE = " + Constants.INSECURE);
      // From implements constants.
      System.err.println("Test INSECURE = " + Constants.INSECURE);
      Driver._parseArgs(args);
      try {
         session =
            new Session(
               Driver._username,
               Driver._password,
               Driver._domainfile,
               Constants.INSECURE);
         session.dump();

         if (Driver._type != null) {
            System.err.println("set type = " + Driver._type);
            test = session.open(Driver._type);
            if (test == null)
               System.err.println("Open failed");
            // For fun, list the file currently in the type.
            System.err.println("Listing type = " + Driver._type);
            test.show();
            while ((result = session.result()) != null) {
               if (result.getErrno() == 0) {
                  System.out.println(
                     result.getName()
                        + "\t"
                        + result.getDate()
                        + "\t"
                        + result.getSize());
               } else
                  System.err.println("List failed: " + result.getMessage());
            }
         }
         if (_fileToAdd != null) {
            System.err.println("Adding file = " + Driver._fileToAdd);
            String[] f = new String[1];
            f[0] = new String(Driver._fileToAdd);
            test.add(f);
            result = session.result();
            System.err.println("Status of command is " + result.getErrno());
         }
         if (Driver._fileToGet != null) {
            System.err.println("Getting file = " + Driver._fileToGet);
            String[] f = new String[1];
            f[0] = new String(Driver._fileToGet);
            test.get(f);
            result = session.result();
            // Get null result.  There will always be one.
            session.result();
            System.err.println("Status of command is " + result.getErrno());
         }
         if (Driver._fileToGet_2 != null) {
            System.err.println("Getting file = " + Driver._fileToGet_2);
            String[] f = new String[1];
            f[0] = new String(Driver._fileToGet_2);
            result = session.result();
            // Get null result.  There will always be one.
            session.result();
            System.err.println("Status of command is " + result.getErrno());
         }
         if (Driver._fileToDelete != null) {
            System.err.println("Deleting file = " + Driver._fileToDelete);
            test.delete(_fileToDelete);
            result = session.result();
            System.err.println("Status of command is " + result.getErrno());
         }
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         //if (test != null)
         //  test.close ();
         if (session != null)
            session.closeImmediate();
      }
      System.exit(0);
   }
}
