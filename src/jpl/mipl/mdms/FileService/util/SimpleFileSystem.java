/****************************************************************************
 * Copyright (C) 2001 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ***************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import jpl.mipl.mdms.FileService.io.BoundedBufferedReader;

/**
 * This class implements the simple file system to support the required FEI
 * operations.  This class should be portable to any file systems.
 *
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: SimpleFileSystem.java,v 1.7 2013/10/14 17:29:23 ntt Exp $
 */
public class SimpleFileSystem implements FileSystem {

    /**
     * Implement the method to create a softlink.  Since the Unix symbolic
     * link feature is not portable on other file system, this implementation
     * creates a text file to store the actual file location.
     *
     * @param target the actual target file name to link to
     * @param link the name of the link file
     * @return an Errno object reference.
     */
    public Errno createSoftLink(String target, String link) {
        Errno errno = null;
        try {
            File linkfile = new File(link);
            if (linkfile.exists())
                errno = new Errno(-1, "Can't create softlink <" + link + ">");
            else {
                BufferedWriter writer =
                    new BufferedWriter(new FileWriter(linkfile));
                writer.write(target);
                writer.close();
                errno =
                    new Errno(0, "Softlink <" + link + "> has been created.");
            }
        } catch (IOException e) {
            errno = new Errno(-1, e.getMessage());
        }
        return errno;
    }

    /**
     * Implement the method to obtain a softlink.  Since this implementation
     * uses a text file to store the physical target location, this method
     * returns the contents within the link.
     *
     * @param link the link file name
     * @return the contents within the link file or null
     */
    public String getSoftLink(String link) {
        String linkValue = null;
        try {
            File file = new File(link);
            if (file.isDirectory())
                return linkValue;

            //Security code review prompted this change to prevent DOS attack (nttoole 08.27.2013)
            //BufferedReader linkfile = new BufferedReader(new FileReader(file));
            BufferedReader linkfile = new BoundedBufferedReader(new FileReader(file));
            
            linkValue = linkfile.readLine();
            linkfile.close();
        } catch (IOException e) {
            // no-op;
        }
        return linkValue;
    }

    /**
     * Implement the method to remove a softlink file.  This implementation
     * deletes the link file.
     *
     * @param link the link file
     * @return an Errno object reference.
     */
    public Errno removeSoftLink(String link) {
        Errno errno = null;

        File linkfile = new File(link);
        if (!linkfile.exists() || linkfile.isDirectory())
            errno =
                new Errno(
                    -1,
                    "Reference "
                        + this.getSoftLink(link)
                        + " deleted from vft."
                        + "Warning: link \""
                        + link
                        + "\", could not be deleted.");
        else {
            if (linkfile.delete() == false) {
                errno =
                    new Errno(
                        -1,
                        "Can't delete old reference location \"" + link + "\"");
            } else {
                errno =
                    new Errno(0, "Reference link \"" + link + "\" unlinked.");
            }
        }
        return errno;
    }

    /**
     * Implement the method to lock a file from group and other access.
     * This is a do-nothing method since 'chmod' is not a portable feature.
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno lockGroupOther(String filename) {
        return new Errno(0, "Locked group|other on " + filename);
    }

    /**
     * Implement the method to lock a file from user, group, and other access.
     * This is a do-nothing method since 'chmod' is not a portable feature.
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno lockUserGroupOther(String filename) {
        return new Errno(0, "Locked user|group|other on " + filename);
    }

    /**
     * Implement the method to unlock a file for user and group access.
     * This is a do-nothting method since 'chmod' is not a portable feature.
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno unlockUserGroup(String filename) {
        return new Errno(0, "Unlocked user|group on " + filename);
    }

    /**
     * Implement the method to unlock a file for user access.  This is a
     * do-nothing method since 'chmod' is not a portable feature.
     *
     * @param filename the target file name
     * @return the Errno object reference
     */
    public Errno unlockUser(String filename) {
        return new Errno(0, "Unlocked user on " + filename);
    }

    /**
     * Implement the method to move/rename a file.  This implementation
     * simply uses the File.renameTo command.
     */
    public Errno moveFile(String srcFileName, String dstFileName) {
        File srcFile = new File(srcFileName),
             dstFile = new File(dstFileName);

        if (srcFile.renameTo(dstFile))
            return new Errno(0,"Moved "+srcFileName+" to "+dstFileName+".");
        else
            return new Errno(-1,"Unable to move "+srcFileName+" to "+dstFileName+".");
    }
}
