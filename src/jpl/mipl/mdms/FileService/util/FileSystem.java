/****************************************************************************
 * Copyright (C) 2001 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ***************************************************************************/
package jpl.mipl.mdms.FileService.util;

/**
 * This is a generic interface for file system operations that are required
 * by FEI.  At the present time file system support is very limited by the
 * current JDK (v1.4.1).  This generic interface serves as generic
 * file system wrapper, which will simplify support for non-Unix systems.
 *
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: FileSystem.java,v 1.5 2007/03/29 19:39:41 awt Exp $
 */
public interface FileSystem {

    /**
     * Method to create a softlink to the targeted file.
     *
     * @param target the target file to be reference to
     * @param link the symbolic link to be used
     * @return Errno object to capture any failures during link creation.
     */
    Errno createSoftLink(String target, String link);

    /**
     * Method to return the actual value referenced by the symbolic link
     *
     * @param link The symbolic link.
     * @return the string value referenced by the symbolic link or null.
     */
    String getSoftLink(String link);

    /**
     * Method to remove an existing symbolic link.
     *
     * @param link the symbolic link
     * @return Errno object to capture any failures during link removal.
     */
    Errno removeSoftLink(String link);

    /**
     * Method to lock a file/directory from group and others.
     *
     * @param filename the file/directory name
     * @return Errno object to capture any failures during file locking.
     */
    Errno lockGroupOther(String filename);

    /**
     * Method to lock a file/directory from all access.
     *
     * @param filename the file/directory name
     * @return Errno object to capture any failures during file locking
     */
    Errno lockUserGroupOther(String filename);

    /**
     * Method to unlock owner+group access to a given file/directory
     *
     * @param filename the file/directory name
     * @return Errno object to capture any failures during file locking
     */
    Errno unlockUserGroup(String filename);

    /**
     * Method to unlock owner access to a given file/directory
     *
     * @param filename the file/directory name
     * @return Errno object to capture any failures during file locking
     */
    Errno unlockUser(String filename);
    
    /**
     * Method to move/rename file.
     * 
     * @param srcFileName
     * @param dstFileName
     * @return Errno object to capture any failures during file moving
     */
    Errno moveFile(String srcFileName, String dstFileName);

}
