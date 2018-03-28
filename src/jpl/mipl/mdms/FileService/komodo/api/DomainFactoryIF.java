/**
 *  @copyright Copyright 2004, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */
package jpl.mipl.mdms.FileService.komodo.api;

import java.net.URL;

/**
 * Factory interface for Domain objects
 * 
 * @author R. Pavlovsky {rich.pavlovsky@jpl.nasa.gov}
 * @version $Id: DomainFactoryIF.java,v 1.3 2004/09/29 22:02:19 rap Exp $
 */
public interface DomainFactoryIF {

    /**
     * Returns a singleton Domain object to parse and validate the FEI Domain
     * information.
     * 
     * @param url location of Domain information
     * @return Domain object containing lookup table of information
     * @throws SessionException when domain parse error
     */
    Domain getDomain(URL url) throws SessionException;
}