package jpl.mipl.mdms.FileService.komodo.util;

import java.io.File;

import jpl.mipl.mdms.FileService.komodo.api.Result;

/**
 * <b>Purpose:</b>
 * Utility for invocation commands, primarily creates instances of command
 * after substituting command variables with values.
 * 
 * If a variable has not value, then it will be printed as 'NULL'.
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
 * 04/13/2006        Nick             Initial Release
 * 04/08/2008        Nick             Added $fileLocation for server-side file
 *                                    path
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: InvocationCommandUtil.java,v 1.4 2008/04/23 17:57:58 ntt Exp $
 *
 */

public class InvocationCommandUtil
{
    public static final String NULL_STR = "NULL";

    //---------------------------------------------------------------------
    
    //invocation variable regex's
    protected static final String REGEX_FILENAME_NO_PATH = "\\$(f|F)(i|I)(l|L)(e|E)(n|N)(a|A)(m|M)(e|E)(n|N)(o|O)(p|P)(a|A)(t|T)(h|H)";
    protected static final String REGEX_FILENAME = "\\$(f|F)(i|I)(l|L)(e|E)(n|N)(a|A)(m|M)(e|E)";
    protected static final String REGEX_FILEPATH = "\\$(f|F)(i|I)(l|L)(e|E)(p|P)(a|A)(t|T)(h|H)";
    protected static final String REGEX_FILETYPE = "\\$(f|F)(i|I)(l|L)(e|E)(t|T)(y|Y)(p|P)(e|E)";
    protected static final String REGEX_SERVER_GROUP = "\\$(s|S)(e|E)(r|R)(v|V)(e|E)(r|R)(g|G)(r|R)(o|O)(u|U)(p|P)";
    protected static final String REGEX_COMMENT = "\\$(c|C)(o|O)(m|M)(m|M)(e|E)(n|N)(t|T)";
    protected static final String REGEX_REMOTE_LOCATION = "\\$(r|R)(e|E)(m|M)(o|O)(t|T)(e|E)(l|L)(o|O)(c|C)(a|A)(t|T)(i|I)(o|O)(n|N)";    
    
    //---------------------------------------------------------------------
    
    /**
     * Given the invocation string and a set of values, replaces instances of
     * variable names in string with corresponding values if provided.
     * 
     * @param command Invocation command string
     * @param filenameNoPath Filename without path prefix
     * @param filename Complete path of the file
     * @param filepath Directory containing file
     * @param filetype Name of the FEi filetype
     * @param serverGroup Name of the FEI server group
     * @param comment Comment associated with file
     * @param remoteLocation Server-side filepath location of the file
     * @return command string with variable names replaced with values
     */

    public static final String buildCommand(String command, String filenameNoPath,
                                            String filename, String filepath, 
                                            String filetype, String serverGroup,
                                            String comment, String remoteLocation) 
    {
        //if null command, then can't do anything really, return empty string
        if (command == null)
            return "";
       
        //check for nulls, replace with null string
        if (filetype == null)
            filetype = NULL_STR;
        if (serverGroup == null)
            serverGroup = NULL_STR;
        if (comment == null)
            comment = NULL_STR;
        if (remoteLocation == null)
            remoteLocation = NULL_STR;
        
       
        //see if we can salvage file info from any of the bits
        if (filename == null)
        {
            if (filepath != null && filenameNoPath != null)
                filename = filepath + File.separator + filenameNoPath;                     
        }
        else if (filepath == null || filenameNoPath == null)
        {
            File tmpFile = new File(filename);
            if (filepath == null)
                filepath = tmpFile.getParentFile().getAbsolutePath();
            if (filenameNoPath == null)
                filenameNoPath = tmpFile.getName();
        }
       
        //if any fileparts are still null, then set to null string
        if (filename == null)
            filename = NULL_STR;
        if (filepath == null)
            filepath = NULL_STR;
        if (filenameNoPath == null)
            filenameNoPath = NULL_STR;
       
        //--------------------------
       
        //translate Windows separators to preserve em
        filename = filename.replaceAll("\\\\", "\\\\\\\\");
        filepath = filepath.replaceAll("\\\\", "\\\\\\\\");

       
        //perform the substitutions
        String cmdStr = command;
        cmdStr = cmdStr.replaceAll(REGEX_FILENAME_NO_PATH, filenameNoPath);
        cmdStr = cmdStr.replaceAll(REGEX_FILENAME, filename);
        cmdStr = cmdStr.replaceAll(REGEX_FILEPATH, filepath);
        cmdStr = cmdStr.replaceAll(REGEX_FILETYPE, filetype);
        cmdStr = cmdStr.replaceAll(REGEX_SERVER_GROUP, serverGroup);
        cmdStr = cmdStr.replaceAll(REGEX_COMMENT, comment);
        cmdStr = cmdStr.replaceAll(REGEX_REMOTE_LOCATION, remoteLocation);
        
        //return result
        return cmdStr;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Given the invocation string and a set of values, replaces instances of
     * variable names in string with corresponding values if provided.
     * @param command Invocation command string
     * @param directory Directory containing file (will be overriden if 
     *                  result has local location value)
     * @param result Result object associated with file
     * @return command string with variable names replaced with values
     */

    public static final String buildCommand(String command, String directory,
                                            Result result) 
    {
        String cmdStr;
        
        //if null command, then can't do anything really, return empty string
        if (command == null || directory == null || result == null)
            return "";
       
        String location = directory;
        if (result.getLocalLocation() != null)
            location = result.getLocalLocation();
        
        
        //call the other buildCommand with data from result 
        String name = result.getName();            
        cmdStr = buildCommand(command, name, 
                              location + File.separator + name,
                              location, result.getType(),
                              result.getServerGroup(), 
                              result.getComment(),
                              result.getRemoteLocation());
        
        //return result
        return cmdStr;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Given the invocation string and a set of values, replaces instances of
     * variable names in string with corresponding values if provided.
     * @param command Invocation command string
     * @param result Result object associated with file
     * @return command string with variable names replaced with values
     */

    private static final String buildCommand(String command, Result result) 
    {        
        return buildCommand(command, result.getLocalLocation(), result);
    }
    
    //---------------------------------------------------------------------
}
