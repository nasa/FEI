/*******************************************************************************
 * Copyright (C) 2001 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

public class ReceiptCountInfo {
    private long _countAdds;
    private long _countGets;
    
    public ReceiptCountInfo(long countAdds, long countGets)
    {
        this._countAdds = countAdds;
        this._countGets = countGets;
    }

    public long getCountAdds() {
        return _countAdds;
    }
    public long getCountGets() {
        return _countGets;
    }

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("countAdds: "    + this._countAdds + "\n");
        strBuff.append("countGets: "    + this._countGets + "\n");
        return strBuff.toString();
    }
}
