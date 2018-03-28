/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.io;

/**
 * <p>Verify Exception class.  Note: All code throwing this exception must
 * supply errno and a message.</p>
 * <br>
 * <p>Implements the Komodo Verify Exception class.</p>
 *
 *  @author J. Jacobson
 *  @version $Id: VerifyException.java,v 1.3 2005/02/15 01:47:57 txh Exp $
 */
public class VerifyException extends Exception {

   /**
    * constructor
    *
    * @param message string describing the exception
    */
   public VerifyException(String message) {
      super(message);
   }
}
