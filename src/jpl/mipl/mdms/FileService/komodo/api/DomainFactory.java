/**
 *  @copyright Copyright 2004, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */
package jpl.mipl.mdms.FileService.komodo.api;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * Factory class to provide access to all Domain objects.
 * 
 * @author R. Pavlovsky {rich.pavlovsky@jpl.nasa.gov}
 * @version $Id: DomainFactory.java,v 1.7 2010/08/11 16:08:36 awt Exp $
 */
public class DomainFactory implements DomainFactoryIF {

    //---------------------------------------------------------------------
    
    /**
     * Returns a Domain object to parse and validate the FEI Domain
     * information.
     * 
     * @param url location of Domain information
     * @return Domain implementation object
     * @throws SessionException if Domain parse error
     */
    public Domain getDomain(URL url) throws SessionException {
        
        Domain domain = new SaxDomain(url);

        //if domain contains dynamic group, return appropriate wrapper
        if (containsDynamicGroup(domain))
        {
            domain = new DynamicDomainWrapper(domain);
        }
        
        return domain;
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     * Checks all groups declared in Domain instance and checks if
     * any of them are considered by the domain to be dynamic.
     * @param domain Domain instance to be checked
     * @return True if at least one group is dynamic, false otherwise.
     */
    
    protected boolean containsDynamicGroup(Domain domain) throws SessionException
    {
        boolean isDynamic = false;
        List groups = domain.getGroupNames();
        
        for (Iterator it = groups.iterator(); !isDynamic && it.hasNext(); )
        {
            String group = (String) it.next();
            
            if (domain.isGroupDynamic(group))
                isDynamic = true;
        }

        return isDynamic;
        
    }
    
    //---------------------------------------------------------------------
    
//    public Domain getDomain(URL url) throws SessionException {
//        return new SaxDomain(url);
//    }
}