/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.FileService.io;

/**
 * Message package class.  Used to return an error number and
 * a text message read from a stream.
 *
 * @author J. Jacobson, G. Turek, R. Pavlovsky
 * @version $Id: MessagePkg.java,v 1.4 2003/09/05 19:11:15 rap Exp $
 */
public class MessagePkg {
	private String _message;
    private int _errno;

    /**
     * Constructor
     *
     * @param errno the error message number.
     * @param message the error message text.
     */
    public MessagePkg(int errno, String message) {
        this._errno = errno;
        this._message = message;
    }

    /**
     * Accessor method to get the error number.
     * 
     * @return the error number of the message
     */
    public int getErrno() {
    	return this._errno;
    }

    /**
     * Accessor method to get the message text
     * 
     * @return the message text
     */
    public String getMessage() {
    	return this._message;
    }

    /**
     * Override toString() method to return a complete message. 
     * 
     * @return the message error number and text
     */
    public String toString() {
        return new String("" + _errno + ": " + _message);
    }
}
