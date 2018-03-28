/*******************************************************************************
 * Copyright (C) 2001 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

import java.sql.Date;

public class LockInfo {
    private int _currentLock;
    private int  _ftId;
    private String _filetype;
    private String _name;
    private long _size;
    private String _contributor;
    private Date _created;
    private Date _modified;
    
    public LockInfo(int       currentLock,
                    int       ftId,
                    String    filetype,
                    String    name,
                    long      size,
                    String    contributor,
                    Date      created,
                   Date modified)
    {
        this._currentLock = currentLock;
        this._ftId        = ftId;
        this._filetype    = filetype;
        this._name        = name;
        this._size        = size;
        this._contributor = contributor;
        this._created     = created;
        this._modified    = modified;
    }

    public int getCurrentLock() {
        return _currentLock;
    }

    public String getFiletype() {
        return _filetype;
    }

    public String getFileName() {
        return _name;
    }

    public long getSize() {
        return _size;
    }

    public int getFtId() {
        return _ftId;
    }


    public String getContributor() {
        return _contributor;
    }


    public Date getCreated() {
        return _created;
    }


    public Date getModified() {
        return _modified;
    }    

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("currentLock: " + this._currentLock + "\n");
        strBuff.append("ftId: "        + this._ftId + "\n");
        strBuff.append("filetype: "       + this._filetype+ "\n");
        strBuff.append("name: "        + this._name + "\n");
        strBuff.append("size: "        + this._size + "\n");
        strBuff.append("contributor: " + this._contributor + "\n");
        strBuff.append("created: "     + this._created + "\n");
        strBuff.append("modified: "    + this._modified + "\n");
        return strBuff.toString();
    }
}
