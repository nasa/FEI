package jpl.mipl.mdms.FileService.util;

import java.io.File;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;

public class DirectoryUtil
{
    
    public static boolean makeDirectory(File dir) throws SessionException
    {        
        if (dir.isDirectory())
            return true;        
        else if (dir.exists())
            return false;
        
        boolean success = true;
        try {
            success = dir.mkdirs();
        } catch (Exception ex) {
            success = false;
            throw new SessionException("Unabled to create directory: " + 
                    dir.getAbsolutePath() + ".  Reason: " +
                    ex.getMessage(), Constants.EXCEPTION);
        }
        
        return success;
    }
    
    public static boolean removeEmptyDirectories(File file) throws SessionException
    {   
        try {
            if (!file.exists())
                return false;
            
            if (!file.isDirectory())
                return false;
            
            String[] contents = file.list();
            if (contents == null || contents.length == 0)
            {
                file.delete();
                removeEmptyDirectories(file.getParentFile());
            }
        } catch (Exception ex) {
            throw new SessionException("Error occurred while removing " +
            		"directories: " + ex.getMessage(), Constants.EXCEPTION);
        }
        
        return true;
    }
}
