package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Properties;

/**
 * This class encapsulates configuration metadata for implementations
 * of file event handlers.
 *
 *   <PRE>
 *   Copyright 2008, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2008.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 08/15/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileEventHandlerInfo.java,v 1.2 2008/08/19 23:47:22 ntt Exp $
 */

public class FileEventHandlerInfo
{
    
    protected String name;
    protected String id;
    protected String version;
    protected String org;
    protected String description;
    protected String implName;
    protected Properties properties;
    
    public FileEventHandlerInfo()
    {
        init();
    }
    
    protected void init()
    {
        this.properties = new Properties();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getImplementation()
    {
        return implName;
    }

    public void setImplementation(String implName)
    {
        this.implName = implName;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperty(String name, String value)
    {
        this.properties.setProperty(name, value);
    }

    public String getOrganization()
    {
        return org;
    }

    public void setOrganization(String org)
    {
        this.org = org;
    }    
        
}
