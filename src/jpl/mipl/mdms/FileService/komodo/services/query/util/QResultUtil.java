/*
 * Created on Aug 8, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.util;

import java.util.Date;
import java.util.StringTokenizer;

import jpl.mipl.mdms.FileService.komodo.services.query.api.QResult;

/**
 * <b>Purpose:</b> Utility class for QResult serialization/deserialization
 * according to the service protocol.
 * 
 *   <PRE>
 *   Copyright 2007, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2007.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 08/08/2007        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QResultUtil.java,v 1.1 2007/08/09 00:13:47 ntt Exp $
 *
 */
public class QResultUtil
{
    //---------------------------------------------------------------------
    
    /**
     * Creates a string representing the result.  This string is formatted
     * as part of the service protocol, which is currently:
     * "i {name}\t{type}\t{modTime}\t{size}"
     * @param result QResult to be serialized
     * @return Protocol formatted string representation of result
     */
    
    public static String resultToStringEntry(QResult result)
    {
        StringBuffer buffer = new StringBuffer("i ");
        buffer.append(result.getName()).append("\t").append(result.getType());
        buffer.append("\t").append(result.getFileModificationTime().getTime());
        buffer.append("\t").append(result.getSize());
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
    

    /**
     * Creates a QResult from the string representing the result.  
     * This string is expected to be formatted as part of the service 
     * protocol, which is currently: "i {name}\t{type}\t{modTime}\t{size}"
     * @param entry Line representing the result information
     * @return QResult built from information, false if unsuccessful
     */
    
    public static QResult stringEntryToResult(String entry)
    {
        String fileName, date, fileType;
        long fileSize;
        QResult result;
        
        if (!entry.startsWith("i "))
            return null;
        
        StringTokenizer st = new StringTokenizer(entry.substring(2), "\t");
        fileName = st.nextToken();
        fileType = st.nextToken();
        date = st.nextToken();
        fileSize = Long.parseLong(st.nextToken().trim());
        result = new QResult(fileName, fileSize);
        result.setType(fileType);
        result.setFileModificationTime(new Date(Long.parseLong(date)));
        
        return result;
    }
    
    //---------------------------------------------------------------------
}
