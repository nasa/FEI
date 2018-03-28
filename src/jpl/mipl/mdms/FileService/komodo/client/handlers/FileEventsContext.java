package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.io.File;

/**
 * This class encapsulates the information that comprises a file-event context.
 * This includes the working directory, filetype info, and operation id.
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
 * @version $Id: FileEventsContext.java,v 1.2 2009/08/07 01:00:48 ntt Exp $
 *
 */
public class FileEventsContext
{
    String serverGroup;
    String filetype;
    String workingDirectory;
    String operationId;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param servergroup Context severgroup
     * @param filetype Context filetype, can be null for servergroup operations
     * @param workingDirectory Path to the working directory
     * @param operationId Operation identifier
     */
    
    public FileEventsContext(String servergroup, String filetype, 
                             String workingDirectory, String operationId)
    {
        this.serverGroup = servergroup;
        this.filetype    = filetype;
     
        this.operationId = operationId;
        
        File tmpFile = new File(workingDirectory);
        if (!tmpFile.isDirectory())
            throw new IllegalArgumentException("Directory '"+workingDirectory+
                                               "' does not exist");
            
        this.workingDirectory = workingDirectory;
    }
    
    //---------------------------------------------------------------------
    
    public String getType()
    {
        return this.filetype;
    }

    //---------------------------------------------------------------------
    
    public String getServerGroup()
    {
        return this.serverGroup;
    }
    
    //---------------------------------------------------------------------
    
    public String getOperationId()
    {
        return this.operationId;
    }
    
    //---------------------------------------------------------------------
    
    public String getDirectory()
    {
        return this.workingDirectory;
    }
    
    //---------------------------------------------------------------------
    
}
