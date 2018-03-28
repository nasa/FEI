/****************************************************************************
 * Copyright (C) 2001 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ***************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import jpl.mipl.mdms.FileService.io.FileIO;

/**
 * This class implements the Unix file system to support the required FEI
 * operations.  At this time this class uses Runtime to invoke Unix system
 * utilites to perform the access and symbolic modifications.
 *
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: UnixFileSystem.java,v 1.6 2008/12/24 16:30:08 awt Exp $
 */
public class UnixFileSystem extends SimpleFileSystem {

    /**
     * This method implement the FileSystem createSoftLink interface.
     *
     * @param target the target file reference
     * @param link the link to be created
     * @return Errno to store any error returned from stderror
     * @see jpl.mipl.mdms.FileService.util.FileSystem#createSoftLink(java.lang.String, java.lang.String)
     */
    public Errno createSoftLink(String target, String link) {
        String command = "ln -s " + target + " " + link;
        return SystemProcess.execute(command);
    }

    /**
     * This method implements the FileSystem getSoftLink interface
     *
     * @param link the link value to be retrieve from.
     * @return the value of the link.
     * @see jpl.mipl.mdms.FileService.util.FileSystem#getSoftLink(java.lang.String)
     */
    public String getSoftLink(String link) {
        return link;
    }

    /**
     * Method to lock a file from group and other access.
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno lockGroupOther(String filename) {
        String command = "chmod -R go-rw " + filename;
        return SystemProcess.execute(command);
    }

    /**
     * Method to lock a file from user, group, and other access.
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno lockUserGroupOther(String filename) {
        String command = "chmod -R ugo-rw " + filename;
        return SystemProcess.execute(command);
    }

    /**
     * Method to unlock a file for user and group access
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno unlockUserGroup(String filename) {
        String command = "chmod -R ug+rw " + filename;
        return SystemProcess.execute(command);
    }

    /**
     * Method to unlock a file for user access.
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno unlockUser(String filename) {
        String command = "chmod -R u+rw " + filename;
        return SystemProcess.execute(command);
    }
    
    
    /**
     * Implentation of the moveFile method for a
     * Unix type filesystem.  This attempts to move
     * the file by first using the java File.renameTo
     * method.  If that fails, uses the "mv" unix command.
     * If the mv command fails, it will result to reading
     * the source file, writing the destination file, and 
     * finally, deleting the source file.
     * 
     * @param srcFileName the name and path of file being moved
     * @param dstFileName the destination name and path of file 
     */
    public Errno moveFile(String srcFileName, String dstFileName) {        
        Errno errno = super.moveFile(srcFileName,dstFileName);
        if (errno.getId() != 0) {            
            String command = "mv "+ srcFileName+" "+dstFileName;            
            errno = SystemProcess.execute(command);
            
            if (errno.getId() != 0) {                
                FileChannel  srcChannel = null,
                             dstChannel = null;
                File srcFile = new File(srcFileName),
                     dstFile = new File(dstFileName);
                try {
                    srcChannel = new FileInputStream(srcFile).getChannel();
                    dstChannel = new FileOutputStream(dstFile).getChannel();
                       
                    long bytesToRead = srcChannel.size();
                    long offset = 0;
                    while (bytesToRead > 0) {
                
                        if (bytesToRead > FileIO.BUFFSIZE) {
                            dstChannel.transferFrom(srcChannel,offset,FileIO.BUFFSIZE);
                            bytesToRead -= FileIO.BUFFSIZE;
                            offset += FileIO.BUFFSIZE;
                        } else {
                            dstChannel.transferFrom(srcChannel,offset,bytesToRead);
                            bytesToRead = 0;
                        }
                    }
                } catch (IOException ioe) {
                    return new Errno(-1,ioe.getMessage());
                } finally {
                    // close channels
                    try {
                        if (srcChannel != null)
                            srcChannel.close();
                        if (dstChannel != null)
                            dstChannel.close();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
                
                // sanity check
                if (!dstFile.exists())
                    return new Errno(-1,"Unable to move file.");

                //  At this point, the file has been copied to the new location.
                //  Attempt to delete the old file.  If deletion of original file fails
                //  rollback by deleting copy of file in new location.
                if (!srcFile.delete()) {
                    dstFile.delete();
                    return new Errno(-1, "Unable to move file: "+
                            srcFile.getAbsoluteFile() +" can not be deleted.");
                }
            }             
        }
        
        return new Errno(0,"Moved "+ srcFileName + " to " + dstFileName + ".");
    }
}
