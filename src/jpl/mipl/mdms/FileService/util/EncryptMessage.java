/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.io.IOException;

/**
 * Utility program to encrypt the input string with one-way hash
 * algorithm.  This program should be used during bootstrap configuration
 * of the Komodo service where an administration user is need to configure the
 * service.
 * <br><br>
 * Example Usage (assume CLASSPATH is set correctly):<br>
 * <code>
 * % java jpl.mipl.mdms.FileService.util.FileUtil.EncryptMessage foobar<br>
 * 8843d7f92416211de9ebb963ff4ce28125932878
 * </code>
 */
public class EncryptMessage {

    /**
     * Main method
     * 
     * @param args the command line args
     */
    public static void main(String[] args) {
        try {
            String message = ConsolePassword.getPassword("Message >> ");
            System.out.println(FileUtil.encryptMessage(message.getBytes()));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
