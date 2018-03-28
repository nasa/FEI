/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.client;

import jpl.mipl.mdms.utils.logging.Logger;

/**
 * Make simple connection to serverHost and serverPort specified starting point
 * for a network client.
 * 
 * @author G. Turek
 * @version $Id: GeneralClient.java,v 1.6 2007/08/24 22:49:01 ntt Exp $
 */

public class GeneralClient extends BaseClient {

   public static final Logger logger = Logger.getLogger(GeneralClient.class
         .getName());

   /**
    * Constructor, accepts string array object which contains the command line
    * arguments.
    * 
    * @param args The command line arguments
    * @throws Exception when general failure
    */
   public GeneralClient(String[] args) throws Exception {
      super(args);
      CCLProcessor cli = new CCLProcessor(this._ssl, this._batchFile,
            this._silent, this._exitAfterBatch);
   }

   /**
    * Main method, accepts string array object which contains the command line
    * arguments.
    * 
    * @param args The command line arguments
    */
   public static void main(String[] args) {
      try {
         GeneralClient client = new GeneralClient(args);
      } catch (Exception e) {
         GeneralClient.logger.error(e.getMessage());
         GeneralClient.logger.debug(null, e);
         System.exit(1);
      }
   }
}