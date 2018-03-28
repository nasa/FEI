/******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights re served
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 *****************************************************************************/
package jpl.mipl.mdms.FileService.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Properties;

/**
 *  This class is a generic utility class for File I/O
 *
 *  @author G. Turek
 *  @version $Id: FileIO.java,v 1.8 2008/06/18 00:32:23 ntt Exp $
 */
public class FileIO {
    
    public static final int BUFFSIZE = 32768;

    /**
     * Returns a FileChannel, that is part of the Java NIO package.
     *
     * @param fileName the file name
     * @return a FileChannel for this file
     * @throws FileNotFoundException when file not found (duh!)
     * @see java.nio.channels.FileChannel
     */
    public static FileChannel getFileChannel(String fileName)
        throws FileNotFoundException {
        File inFile = new File(fileName);
        if (inFile.exists()) {
            //Reading from disc
            return new FileInputStream(inFile).getChannel();
        } else {
            //Reading from a jar file
            String jarFilePath = "null";
            try {
                jarFilePath =
                    ClassLoader.getSystemResource(fileName).toString();
                inFile = new File(new URI(jarFilePath));
                return new FileInputStream(inFile).getChannel();
            } catch (URISyntaxException use) {
                throw new FileNotFoundException(
                    "Domain file " + jarFilePath + " not found");
            } catch (NullPointerException npe) {
                throw new FileNotFoundException(
                    "Domain file " + jarFilePath + " not found");
            }
        }
    }

    /**
     * Returns a FileChannel object, that is part of the Java NIO package.
     *
     * @param inFile a File object
     * @return a FileChannel for this file
     * @throws FileNotFoundException when file not found (duh!)
     * @see java.nio.channels.FileChannel
     */
    public static FileChannel getFileChannel(File inFile)
        throws FileNotFoundException {
        if (inFile.exists()) {
            //Reading from disc
            return new FileInputStream(inFile).getChannel();
        } else {
            //Reading from a jar file
            String jarFilePath = "null";
            try {
                jarFilePath =
                    ClassLoader.getSystemResource(inFile.getName()).toString();
                File nf = new File(new URI(jarFilePath));
                return new FileInputStream(nf).getChannel();
            } catch (URISyntaxException use) {
                throw new FileNotFoundException(
                    "Domain file " + jarFilePath + " not found");
            } catch (NullPointerException npe) {
                throw new FileNotFoundException(
                    "Domain file " + jarFilePath + " not found");
            }
        }
    }

    /**
     * Read a file from local disc into memory.  This method
     * only works for files whose size less than or equal to Integer.MAX_VALUE,
     * or an Exception will be thrown.
     * @param fileName the file name
     * @return the data byte array
     * @throws Exception the catch all exception
     * @throws IOException file I/O error
     * @throws FileNotFoundException file not found
     */
    public static byte[] readIntoMemory(String fileName)
        throws IOException, FileNotFoundException, Exception {
        long fileBufSize = (new File(fileName)).length();
        if (fileBufSize > Integer.MAX_VALUE)
            throw new Exception("File too large");
        byte[] data = new byte[(int) fileBufSize];
        FileInputStream fin = new FileInputStream(fileName);
        fin.read(data, 0, (int) fileBufSize);
        fin.close();
        return data;
    }

    /**
     * Read a file from either local disc or a jar file
     *
     * @param fileName the file name
     * @return BufferedReader object
     * @throws IOException when I/O failure
     * @throws FileNotFoundException when file not found
     */
    public static BufferedReader read(String fileName)
        throws IOException, FileNotFoundException {
        if ((new File(fileName)).exists()) {
            //Reading from disc
            return new BufferedReader(new FileReader(fileName));
        } else {
            //Reading from a jar file
            try {
                InputStream is =
                    ClassLoader.getSystemResource(fileName).openStream();
                return new BufferedReader(new InputStreamReader(is));
            } catch (NullPointerException npe) {
                throw new FileNotFoundException(
                    "Domain file " + fileName + " not found");
            }
        }
    }

    /**
     * Read a file from either local disc or a jar file
     *
     * @param url the url to read
     * @return BufferedReader object
     * @throws IOException when I/O failure
     */
    public static BufferedReader readURL(URL url)
        throws IOException {
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    /**
     * Read a file from either the local disc or a jar file
     *
     * @param inFile the File object to be read
     * @return BufferedReader object reference
     * @throws IOException when file I/O failure
     * @throws FileNotFoundException when file not found
     */
    public static BufferedReader read(File inFile)
        throws IOException, FileNotFoundException {
        if (inFile.exists()) {
            //Reading from disc
            return new BufferedReader(new FileReader(inFile.getAbsolutePath()));
        } else {
            //Reading from a jar file
            try {
                InputStream is =
                    ClassLoader
                        .getSystemResource(inFile.getName())
                        .openStream();
                return new BufferedReader(new InputStreamReader(is));
            } catch (NullPointerException npe) {
                throw new FileNotFoundException(
                    "Domain file " + inFile.getName() + " not found");
            }
        }
    }

    /**
     * Read a configuration file into a <code>Properties</code> object from
     * either the local disc or a jar file
     *
     * @param prop the properties object configuration is to be read in
     * @param inFile the configuration File
     * @throws IOException when general I/O failure
     * @throws FileNotFoundException when file not found (duh!)
     */
    public static void readConfiguration(Properties prop, File inFile)
        throws IOException, FileNotFoundException {
        if (inFile.exists()) {
            //Try reading from local disc
            FileInputStream fs = new FileInputStream(inFile.getAbsolutePath());
            prop.load(fs);
            if (fs != null)
                fs.close();
        } else {
            //Reading from a jar file
            if (ClassLoader.getSystemResource(inFile.toString()) != null) {
                InputStream is =
                    ClassLoader
                        .getSystemResource(inFile.toString())
                        .openStream();
                prop.load(is);
                is.close();
            } else {
                //File really does not exist
                throw new FileNotFoundException(
                    "Unable to find" + inFile.getAbsolutePath());
            }
        }
    }
    
	/**
	 * Write a configuration file based on a <code>Properties</code> object
	 *
	 * @param prop the properties object configuration to write out
	 * @param outFile the configuration File
	 * @param header the header comment for the configuration File
	 * @throws IOException when general I/O failure
	 * @throws FileNotFoundException when file not found (duh!)
	 */
	public static void writeConfiguration(Properties prop, File outFile, String header)
		                                 throws IOException, FileNotFoundException 
    {
	    
        //-------------------------
	    
        //ensure directory exist before attempting to create file
        File outFileParent = null;
        try {
            outFileParent = outFile.getParentFile();
            
            //if it exist but is not a dir, then we will catch that
            //in the FileOutStream portion
            if (!outFileParent.exists())
            {
                boolean createdDir = outFileParent.mkdirs();
                if (!createdDir)
                {
                    throw new IOException("Could not create directory " + 
                                          outFileParent.getAbsolutePath() +".");
                }
            }
        } catch (SecurityException se) {
            throw new IOException("Could not create directory " + 
                     outFileParent.getAbsolutePath() +".  Reason: " + 
                     se.getMessage());
        }
    
        //-------------------------
        
		FileOutputStream fs = new FileOutputStream(outFile.getAbsolutePath());
		prop.store(fs, header);
		if (fs != null)
			fs.close();
		
		//-------------------------
	}
        
}
