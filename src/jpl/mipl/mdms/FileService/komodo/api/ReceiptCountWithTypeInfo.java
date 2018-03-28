/*******************************************************************************
 * Copyright (C) 2015 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

public class ReceiptCountWithTypeInfo {
    private long _countAdds;
    private long _countGets;
    private String _ftName;
    
    public ReceiptCountWithTypeInfo(long countAdds, long countGets, String ftName)
    {
        this._countAdds = countAdds;
        this._countGets = countGets;
        this. _ftName   = ftName;
    }

    public long getCountAdds() {
        return _countAdds;
    }
    public long getCountGets() {
        return _countGets;
    }
    public String getFiletype() {
        return _ftName;
    }

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("ftName:    "    + this._ftName+ "\n");
        strBuff.append("countAdds: "    + this._countAdds + "\n");
        strBuff.append("countGets: "    + this._countGets + "\n");
        return strBuff.toString();
    }
}
