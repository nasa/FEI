/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.FileService.komodo.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Komodo XML parser error handler.  Catches SAXParseExceptions and 
 * rethrows them as SAXExceptions with an informative message.
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: ErrorCatcher.java,v 1.4 2003/09/09 00:32:34 rap Exp $
 */
class ErrorCatcher implements ErrorHandler {
    private boolean _isValid;

    /**
     * Constructor
     */
    public ErrorCatcher() {
        this._isValid = true;
    }

    /**
     * This method is called when the XML parser encounters a warning condition.
     * The document can still be considered valid if only warnings are encountered.
     * 
     * @throws SAXException
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException exception) throws SAXException {
        throw new SAXException(
            "Warning: "
                + exception.getMessage()
                + " at line "
                + exception.getLineNumber()
                + ", column "
                + exception.getColumnNumber()
                + " in entity "
                + exception.getSystemId());
    }

    /**
     * This method is called when the XML parser encounters an error condition.
     * The document does not validate successfully.
     * 
     * @throws SAXException
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException exception) throws SAXException {
        this._isValid = false;

        throw new SAXException(
            "Error: "
                + exception.getMessage()
                + " at line "
                + exception.getLineNumber()
                + ", column "
                + exception.getColumnNumber()
                + " in entity "
                + exception.getSystemId());
    }

    /**
     * This method is called when the XML parser encounters a fatal condition.
     * The document does not validate successfully.
     * 
     * @throws SAXException
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException exception) throws SAXException {
        this._isValid = false;

        throw new SAXException(
            "Error: "
                + exception.getMessage()
                + " at line "
                + exception.getLineNumber()
                + ", column "
                + exception.getColumnNumber()
                + " in entity "
                + exception.getSystemId());
    }

    /**
     * Utility method to check if XML document validated successfully
     * 
     * @return boolean true if validation was successful, false otherwise
     */
    public boolean isValid() {
        return this._isValid;
    }
}
