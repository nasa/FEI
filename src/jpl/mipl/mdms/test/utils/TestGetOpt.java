/**
 * Test program for GetOpt and GetOptLong utility classes
 */

package jpl.mipl.mdms.test.utils;

import jpl.mipl.mdms.utils.*;

/**
 * This is a test program for the GetOpt and GetOptLong utility classes.
 *
 * @copyright Copyright 2001, California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government Sponsorship acknowledge.  25-09-2001.
 * MIPL Data Management System (MDMS).
 * <p>
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: TestGetOpt.java,v 1.5 2002/05/20 18:41:11 txh Exp $ 
 */
public class TestGetOpt {
   /** The internal reference to the GetOpt object. */
   private GetOpt getOpt__;

   /**
    * Creates a TestGetOpt object with an input GetOpt object reference.
    * @param getOpt the GetOpt object reference.
    */
   public TestGetOpt(GetOpt getOpt) {
      this.getOpt__ = getOpt;
   }

   /**
    * Method to start the test.
    */
   public void run() {
      String str;
      while ((str = this.getOpt__.nextArg()) != null) {
         // it's so happened that all the test arguments begin with different
         // letter, so we can just check by that.  In general, we can also
         // use String.equals() method to do the comparison.
         switch (str.charAt(0)) {
            case 'h' :
               MDMS.DEBUG("[help    ]: on");
               break;
            case 'u' :
               MDMS.DEBUG("[username]: " + this.getOpt__.getArgValue());
               break;
            case 'p' :
               MDMS.DEBUG("[password]: " + this.getOpt__.getArgValue());
               break;
            case 's' :
               MDMS.DEBUG("[server  ]: " + this.getOpt__.getArgValue());
               break;
            case 'd' :
               MDMS.DEBUG("[database]: " + this.getOpt__.getArgValue());
               break;
            default :
               MDMS.ERROR("Invalue input argument");
         }
      }
   }

   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {
      TestConfigurator.activate(args, "GetOpt");

      GetOpt getOpt = null;
      TestGetOpt tstGetOpt = null;

      // Test 1
      MDMS.DEBUG("Standard UNIX GetOpt test");
      String[] argv =
         new String[] {
            "-u",
            "tux",
            "-p",
            "v2.4",
            "-s",
            "dbServer",
            "-d",
            "mydb",
            "-h" };
      MDMS.DEBUG("Arguments: -u tux -p v2.4 -s dbServer -d mydb -h");

      getOpt = new GetOpt(argv, "u:p:s:d:h");
      tstGetOpt = new TestGetOpt(getOpt);
      tstGetOpt.run();

      MDMS.DEBUG("\n");

      // Test 2
      MDMS.DEBUG("Extended long keyword test");
      argv =
         new String[] {
            "-username",
            "tux",
            "-password",
            "v2.4",
            "-server",
            "dbServer",
            "-database",
            "mydb",
            "-help" };
      MDMS.DEBUG(
         "Arguments: "
            + "-username tux -password v2.4 -server dbServer -database mydb -help");

      getOpt =
         new GetOptLong(argv, "help|username:password:server:database:", true, "-");
      tstGetOpt = new TestGetOpt(getOpt);
      tstGetOpt.run();

      MDMS.DEBUG("\n");

      // Test 3
      MDMS.DEBUG("Another Extended long keyword test");
      argv =
         new String[] {
            "username",
            "tux",
            "password",
            "v2.4",
            "server",
            "dbServer",
            "database",
            "mydb",
            "help" };
      MDMS.DEBUG(
         "Arguments: "
            + "username tux password v2.4 server dbServer database mydb help");

      getOpt =
         new GetOptLong(argv, "help|username:password:server:database:", true, null);
      tstGetOpt = new TestGetOpt(getOpt);
      tstGetOpt.run();
      MDMS.DEBUG("Test 'rewind' capability");
      getOpt.rewind();
      tstGetOpt.run();

      MDMS.DEBUG("\n");

      TestConfigurator.deactivate();
   }

}