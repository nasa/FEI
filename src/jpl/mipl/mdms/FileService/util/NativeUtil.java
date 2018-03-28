/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

/**
 * NativeUtil provides static methods calls to native utility
 * functions. The shared library libntvutil.so must be loaded
 * by an application in order to use this utility
 * To use must include this code clock in app.
 *
 * @author G. Turek
 * @version $Id: NativeUtil.java,v 1.2 2003/09/09 00:32:35 rap Exp $
 */

public class NativeUtil {
    /**
     * init - Force load of dynamic library.  Use to force configuration errors
     * at startup, to avoid failure later.
     */
    public static void init() {
    }

    /**
     * Create symbolic link
     * Calls native function int symlink(const char *name1, const char *name2);
     *
     * @param s1 pathname of file
     * @param s2 link pathname
     * @return an Errno object containing error information
     */
    public static native Errno softLink(String s1, String s2);

    /**
     * JNI wrapper for chmod
     *
     * @param s pathname of file
     * @param mode the mode to be changed to
     * @return Errno object containing error information
     */
    public static native Errno chMod(String s, int mode);

    /**
     * JNI wrapper for recursiveLock
     * Recurse through a directory struction, and lock each file
     * and directory in it.  Locking is defined as turning off the
     * permissions specified in the mode mask.  Usually, we would
     * specify 0222, to turn off all write access.  This method
     * is serialized because the underlying JNI call is not
     * re-entrant.
     *
     * @param s pathname of directory
     * @param modeMask to be changed to
     * @return Errno object containing error information
     */
    public static synchronized native Errno recursiveLock(
        String s,
        int modeMask);

    /**
     * JNI wrapper for recursiveUnlock
     * Recurse through a directory struction, and unlock each file
     * and directory in it.  Unlocking is defined as turning on the
     * permissions specified in the mode mask.  Usually, we would
     * specify 0220, to turn on all write access for user and group.
     * This method is serialized because the underlying JNI call is
     * not re-entrant.
     *
     * @param s pathname of directory
     * @param modeMask to be changed to
     * @return Errno object containing error information
     */
    public static synchronized native Errno recursiveUnlock(
        String s,
        int modeMask);
}
