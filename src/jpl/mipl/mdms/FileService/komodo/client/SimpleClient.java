/*
 * Created on Sep 14, 2006
 */
package jpl.mipl.mdms.FileService.komodo.client;

import java.io.File;
import java.net.URL;
import java.text.NumberFormat;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;
import jpl.mipl.mdms.FileService.komodo.util.LoginFile;

/**
 * <b>Purpose:</b>
 * Ultra simple version of FEI5 client 
 * 
 *   <PRE>
 *   Copyright 2006, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2006.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 09/14/2006        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SimpleClient.java,v 1.7 2012/02/28 23:29:26 awt Exp $
 *
 */

public class SimpleClient
{
    /** Reference to the client */
    Client _client;
    
    /** Domain file reference */
    URL _domainFile;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param domainFile Path to a domain file
     */
    
    public SimpleClient(String domainFile)
    {     
        ConfigFileURLResolver resolver = new ConfigFileURLResolver();
        try {
            this._domainFile = resolver.getFileURL(domainFile);
        } catch (SessionException sesEx) {
            throw new RuntimeException("Session exception occurred while " +
            		        "resolving domain file: " + sesEx.getMessage());
        }
        
        //this._domainFile = new File(domainFile);
    }
  
    //---------------------------------------------------------------------
    
    /**
     * Login to filetype
     * @param servergroup Server group name
     * @param filetype Filetype name
     * @param user FEI user name
     * @param password FEI user password
     */
    
    public void login(String servergroup, String filetype,
                       String user, String password) throws SessionException 
    {           
           //create client instance
           this._client = new Client(_domainFile);           
           
           if (servergroup != null)
           {
               if (!this._client.isGroupDefined(servergroup))
                   throw new SessionException("Group " + servergroup
                           + " not found in domain!", 
                           Constants.DOMAINLOOKUPERR);
               
//               LinkedList l = this._client.getGroupList();
//               boolean found = false;
//               Iterator it = l.iterator();
//               while (it.hasNext())
//               {
//                   String entry = it.next().toString();
//                   if (servergroup.equalsIgnoreCase(entry))
//                       found = true;
//               }           
//               if (!found)
//                   throw new SessionException("Group " + servergroup
//                                       + " not found in domain!", 
//                                       Constants.DOMAINLOOKUPERR);           
           }
           
           //login to client
           this._client.login(user, password, servergroup, filetype);           
     }
    
    //---------------------------------------------------------------------
    
    /**
     * Logs out of client
     */
    
    public void logout() throws SessionException 
    {
        if (this._client != null && this._client.isLoggedOn())
           this._client.logout();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Add an array of files to filetype with optional comment
     * @param files Array of files to be added
     * @param comment Comment associated with files, optional
     * @return boolean true of operation is successful, false otherwise
     * @throws SessionException when operation fails.
     */
    
    public boolean addFiles(String[] files, String comment) throws SessionException
    {
        
        if (this._client == null || !this._client.isLoggedOn())
            return false;
        
        int transId;
        
        if (files == null || files.length < 1) 
        {
            //this._logger.info("No file to be added");
            return true;
        } 
        
        
        transId = this._client.add(files, comment);        
        boolean success = true;
        NumberFormat fmt = NumberFormat.getInstance();
        String currentType = "";
        
        while (this._client.getTransactionCount() > 0) {
            Result result = this._client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() != Constants.OK) {
               //this._logger.error(
                System.err.println("Error: " + result.getMessage());
               //++this._errorCount;
               continue;
            }
 
            //this._logger.info(this._align(fmt, ++this._resultIdx, 6)
            //      + ". Added \"" + result.getName() + "\".");
            System.out.println("Added \""+result.getName() + "\".");
            
            if (result.getErrno() != Constants.OK) 
            {
                success = false;
            }
        }
        return success;
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Replace action
     * @param files Array of files to be replaced in filetype
     * @param comment Optional comment associated with file, can be null
     * @return boolean true of operation is successful, false otherwise
     * @throws SessionException when operation fails.
     */
    
    public boolean replace(String[] files, String comment) throws SessionException
    {        
        if (this._client == null || !this._client.isLoggedOn())
            return false;
        
        int transId = this._client.replace(files, comment);
        boolean success = true;
        String currentType = "";
        
        while (this._client.getTransactionCount() > 0) 
        {
            Result result = this._client.getResult();
            if (result == null) 
            {
                continue;
            }
            if (result.getErrno() != Constants.OK) 
            {
                //this._logger.error(ERROR_TAG + result.getMessage());
                System.err.println("Error: " + result.getMessage());
                //++this._errorCount;
                continue;
            }
            //if (!currentType.equals(result.getType()) && this._using) 
            //{
            //    currentType = result.getType();
            //    this._logger.info("\nReplacing file type \"" + currentType
            //                      + "\":");
            //}
            //this._logger.info(this._align(fmt, ++this._resultIdx, 6)
            //          + ". Replaced \"" + result.getName() + "\".");
            System.out.println("Replaced \""+result.getName() + "\".");
            if (result.getErrno() != Constants.OK) 
            {
                success = false;
            }
        }
        return success;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get Action. Note: At most one of {replace,version} can be set to true
     * @param files Array of filenames to be retrieved
     * @param destination Output directory path
     * @param replace Replace flag
     * @param version Version flag
     * @param crc Crc flag
     * @return boolean true when operation is successful, false otherwise
     * @throws SessionException when operation fails
     */
    
    private boolean showFile(String fileRegEx, String destination,
                        boolean replace, boolean version,
                        boolean crc) throws SessionException 
    {
        
        if (this._client == null || !this._client.isLoggedOn())
            return false;
        
        try {
            this._client.changeDir(destination);

            if (replace && version)
            {
                //this._logger.info(this._getUsage());
                return false;
            }
             
            if (replace) 
            {
                this._client.set(Client.OPTION_REPLACEFILE, true);
            }

            if (version) 
            {
                this._client.set(Client.OPTION_VERSIONFILE, true);
            }

            
            if (crc) 
            {
                this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
                this._client.set(Client.OPTION_RESTART, true);
                //this._logger.info("File resume transfer enabled");
            }


            int transId = this._client.show(fileRegEx);
            boolean success = true;
            //NumberFormat fmt = NumberFormat.getInstance();
            String currentType = "";
            
            while (this._client.getTransactionCount() > 0) 
            {
                Result result = this._client.getResult();
                if (result == null) 
                {
                    continue;
                }
                if (result.getErrno() == Constants.NO_FILES_MATCH) 
                {
                    System.err.println(result.getMessage());
                    //this._logger.info(result.getMessage());
                    continue;
                } 
                else if (result.getErrno() == Constants.FILE_EXISTS) 
                {
                    //this._logger.error(ERROR_TAG + result.getMessage());
                    System.err.println("Error: " + result.getMessage());
                    //++this._errorCount;
                    success = false;
                    if (crc)
                        result.commit();
                    continue;
                } 
                else if (result.getErrno() != Constants.OK) 
                {
                    //this._logger.error(ERROR_TAG + result.getMessage());
                    System.err.println("Error: " + result.getMessage());
                    //++this._errorCount;
                    success = false;
                    continue;
                }
             
                //if (!currentType.equals(result.getType()) && this._using) 
                // {
                //    currentType = result.getType();
                //    this._logger.info("\nGetting from file type \"" + currentType
                //            + "\":");
                //}
                //String msg = this._align(fmt, ++this._resultIdx, 6) + ". Got \""
                //   + result.getName() + "\".";

                //if (result.getChecksumStr() != null)
                //    msg += (" CRC:" + result.getChecksumStr());

                //this._logger.info(msg);
                System.out.println("Got file: " + result.getName());

                // if invoke is supported, this is the place to check and
                // execute it. If it fails, then it should commit the
                // restart cache info.
                if (crc)
                    result.commit();
            }
            return success;
        } catch (Exception e) {
            throw new SessionException(e.getMessage(), -1);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Get Action. Note: At most one of {replace,version} can be set to true
     * @param files Array of filenames to be retrieved
     * @param destination Output directory path
     * @param replace Replace flag
     * @param version Version flag
     * @param crc Crc flag
     * @return boolean true when operation is successful, false otherwise
     * @throws SessionException when operation fails
     */
    
    public boolean get(String[] files, String destination,
                        boolean replace, boolean version,
                        boolean crc) throws SessionException 
    {
        
        if (this._client == null || !this._client.isLoggedOn())
            return false;
        
        try {
            this._client.changeDir(destination);

            if (replace && version)
            {
                //this._logger.info(this._getUsage());
                return false;
            }
             
            if (replace) 
            {
                this._client.set(Client.OPTION_REPLACEFILE, true);
            }

            if (version) 
            {
                this._client.set(Client.OPTION_VERSIONFILE, true);
            }

            
            if (crc) 
            {
                this._client.set(Client.OPTION_COMPUTECHECKSUM, true);
                this._client.set(Client.OPTION_RESTART, true);
                //this._logger.info("File resume transfer enabled");
            }

            //if (this._argTable.get(CMD.SAFEREAD) != null) {
            //    this._client.set(Client.OPTION_SAFEREAD, true);
            // }

            //if (this._argTable.get(CMD.RECEIPT) != null) {
            //    this._client.set(Client.OPTION_RECEIPT, true);
            //}

            int transId = this._client.get(files);
            boolean success = true;
            //NumberFormat fmt = NumberFormat.getInstance();
            String currentType = "";
            
            while (this._client.getTransactionCount() > 0) 
            {
                Result result = this._client.getResult();
                if (result == null) 
                {
                    continue;
                }
                if (result.getErrno() == Constants.NO_FILES_MATCH) 
                {
                    System.err.println(result.getMessage());
                    //this._logger.info(result.getMessage());
                    continue;
                } 
                else if (result.getErrno() == Constants.FILE_EXISTS) 
                {
                    //this._logger.error(ERROR_TAG + result.getMessage());
                    System.err.println("Error: " + result.getMessage());
                    //++this._errorCount;
                    success = false;
                    if (crc)
                        result.commit();
                    continue;
                } 
                else if (result.getErrno() != Constants.OK) 
                {
                    //this._logger.error(ERROR_TAG + result.getMessage());
                    System.err.println("Error: " + result.getMessage());
                    //++this._errorCount;
                    success = false;
                    continue;
                }
             
                //if (!currentType.equals(result.getType()) && this._using) 
                // {
                //    currentType = result.getType();
                //    this._logger.info("\nGetting from file type \"" + currentType
                //            + "\":");
                //}
                //String msg = this._align(fmt, ++this._resultIdx, 6) + ". Got \""
                //   + result.getName() + "\".";

                //if (result.getChecksumStr() != null)
                //    msg += (" CRC:" + result.getChecksumStr());

                //this._logger.info(msg);
                System.out.println("Got file: " + result.getName());

                // if invoke is supported, this is the place to check and
                // execute it. If it fails, then it should commit the
                // restart cache info.
                if (crc)
                    result.commit();
            }
            return success;
        } catch (Exception e) {
            throw new SessionException(e.getMessage(), -1);
        }
    }

    
    //---------------------------------------------------------------------
    
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 4)
        {
            System.err.println("Usage: ...");
            System.exit(1);
        }
        
        String domainFile = args[0];
        String servergroup = args[1];
        String filetype = args[2];
        String fileToAdd = args[3];
        String dir = System.getProperty("user.dir");
        LoginFile loginFile = new LoginFile(new File(dir+File.separator+".komodo"+ File.separator + ".komodologin"));
        String user = loginFile.getUsername(servergroup);
        String password = loginFile.getPassword(servergroup);        

        File file = new File(fileToAdd);
        String filename = file.getName();
        
        SimpleClient client = new SimpleClient(domainFile);
        
        System.out.println("Logging in...");
        try {
            client.login(servergroup, filetype, user, password);
        } catch (SessionException sesEx) {
            sesEx.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("Replacing file...");
        try {
            client.replace(new String[] {fileToAdd}, "Test");
        } catch (SessionException sesEx) {
            sesEx.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("Getting file (putting it in "+dir+")...");
        try {
            client.get(new String[] {filename}, dir, true, false, false);
        } catch (SessionException sesEx) {
            sesEx.printStackTrace();
            System.exit(1);
        }   
        
        System.out.println("Logging out...");
        try {
            client.logout();
        }catch (SessionException sesEx) {
            sesEx.printStackTrace();
            System.exit(1);
        }
    }
    
    //---------------------------------------------------------------------
}
