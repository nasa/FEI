/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.client;

/**
 * Make simple connection to serverHost and serverPort specified starting point
 * for a network client.
 * 
 * @author G. Turek
 * @version $Id: Administrator.java,v 1.4 2004/10/27 00:52:34 txh Exp $
 */
public class Administrator extends BaseClient {

   /**
    * Constructor, accepts string array object with command line arguments
    * 
    * @param args The command line arguments
    * @throws Exception when general failure
    */
   public Administrator(String[] args) throws Exception {
      super(args);
      ACLProcessor cli = new ACLProcessor(this._ssl, this._batchFile,
            this._silent, this._exitAfterBatch);
   }

   /**
    * Main method of FEI Administrator client.
    * 
    * @param args The command line arguments
    * @throws Exception when general failure
    */
   public static void main(String[] args) throws Exception {
      try {
         Administrator client = new Administrator(args);
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
}