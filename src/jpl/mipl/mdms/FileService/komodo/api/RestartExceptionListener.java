/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

import java.beans.ExceptionListener;

/**
 * Implements the ExceptionListener interface which is used for
 * catching any recoverable exceptions during construction of
 * XMLDecoder object.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: RestartExceptionListener.java,v 1.3 2003/07/18 00:49:15 txh Exp $
 */
public class RestartExceptionListener implements ExceptionListener {
   // flag which can be checked by the calling object.
   private boolean _caught = false;

   /**
   * Implements the excetionThrown method
   *
   * @param e Any recoverable exception that is thrown.
   */
   public void exceptionThrown(Exception e) {
      //e.printStackTrace();
      this._caught = true;
   }

   /**
   * Accessor to return the exception caught flag.
   *
   * @return true if an exception was thrown.
   */
   public boolean isCaught() {
      return this._caught;
   }
}
