/*******************************************************************************
 * Copyright (C) 2015 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

public class ProductCountWithTypeInfo {
    private long   _countFiles;
    private String _ftName;
    
    public ProductCountWithTypeInfo(long countFiles, String ftName)
    {
        this._countFiles = countFiles;
        this._ftName     = ftName;
    }

    public long getCountFiles() {
        return _countFiles;
    }

    public String getFiletype() {
        return _ftName;
    }

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("ftName:     " + this._ftName+ "\n");
        strBuff.append("countFiles: " + this._countFiles + "\n");
        return strBuff.toString();
    }
}
