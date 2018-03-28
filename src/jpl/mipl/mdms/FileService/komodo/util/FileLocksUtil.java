/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.util;

import jpl.mipl.mdms.FileService.komodo.api.Constants;

/**
*  File Locks utilities.
*
*  @author awt
*  @version $Id: FileLocksUtil.java,v 1.3 2009/09/18 16:43:21 awt Exp $
*/
public class FileLocksUtil
{    
    public static final String NOLOCKSTR       = "none";
    public static final String GETLOCKSTR      = "get";
    public static final String ADDLOCKSTR      = "add";
    public static final String REPLACELOCKSTR  = "replace";
    public static final String DELETELOCKSTR   = "delete";
    public static final String RESERVEDLOCKSTR = "reserved";
    public static final String LINKLOCKSTR     = "link";
    public static final String RENAMELOCKSTR   = "rename";
    public static final String DELETEDLOCKSTR  = "logdelete";
    public static final String MOVELOCKSTR     = "move";
    public static final String MOVEPRSTLOCKSTR = "movepersist";
    
    /** 
     * Convert integer lock value to string. 
     * 
     * @param lockInt
     * @return string representation of the lock value; null if not valid
     */
    public static String getLockAsString(int lockInt) 
    {
        switch (lockInt) 
        {
            case Constants.NOLOCK:
                return NOLOCKSTR;
            case Constants.GETLOCK:
                return GETLOCKSTR;
            case Constants.ADDLOCK:
                return ADDLOCKSTR;
            case Constants.REPLACELOCK:
                return REPLACELOCKSTR;
            case Constants.DELETELOCK:
                return DELETELOCKSTR;
            case Constants.RESERVEDLOCK:
                return RESERVEDLOCKSTR;
            case Constants.LINKLOCK:
                return LINKLOCKSTR;
            case Constants.RENAMELOCK:
                return RENAMELOCKSTR;
            case Constants.DELETEDLOCK:
                return DELETEDLOCKSTR;
            case Constants.MOVELOCK:
                return MOVELOCKSTR;
            case Constants.MOVEPRSTLOCK:
                return MOVEPRSTLOCKSTR;
            default:
                return null;           
        }
    }
    
    /**
     * Converts string lock to integer value.
     * Case-insensitive.
     * 
     * @param lockStr
     * @return integer value of lock; -1 if not valid
     */
    public static int getLockAsInt(String lockStr) 
    {
        if (lockStr.equalsIgnoreCase(NOLOCKSTR))
            return Constants.NOLOCK;
        if (lockStr.equalsIgnoreCase(GETLOCKSTR))
            return Constants.GETLOCK;
        if (lockStr.equalsIgnoreCase(ADDLOCKSTR))
            return Constants.ADDLOCK;
        if (lockStr.equalsIgnoreCase(REPLACELOCKSTR))
            return Constants.REPLACELOCK;
        if (lockStr.equalsIgnoreCase(DELETELOCKSTR))
            return Constants.DELETELOCK;
        if (lockStr.equalsIgnoreCase(RESERVEDLOCKSTR))
            return Constants.RESERVEDLOCK;
        if (lockStr.equalsIgnoreCase(LINKLOCKSTR))
            return Constants.LINKLOCK;
        if (lockStr.equalsIgnoreCase(RENAMELOCKSTR))
            return Constants.RENAMELOCK;
        if (lockStr.equalsIgnoreCase(DELETEDLOCKSTR))
            return Constants.DELETEDLOCK;
        if (lockStr.equalsIgnoreCase(MOVELOCKSTR))
            return Constants.MOVELOCK;
        if (lockStr.equalsIgnoreCase(MOVEPRSTLOCKSTR))
            return Constants.MOVEPRSTLOCK;
        return -1;
    }
    
    
    

}
