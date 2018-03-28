/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

/**
 * Session Exception class.  Note: All code throwing this exception must
 * supply errno and a message.
 *
 *  @author J. Jacobson
 *  @version $Id: SessionException.java,v 1.2 2003/09/04 23:13:00 rap Exp $
 */
public class SessionException extends Exception {
   private int _errno;

   /**
    * Constructor
    *
    * @param message string describing the exception
    * @param errno The error number for the exception
    */
   public SessionException(String message, int errno) {
      super(message);
      this._errno = errno;
   }

   /**
    * Method to return the current error number
    *
    * @return errno The error number associated with the exception.
    */
   public final int getErrno() {
      return this._errno;
   }
}
