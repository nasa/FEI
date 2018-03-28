/*******************************************************************************
 * Copyright (C) 2015 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

public class ReceiptUserInfo {
    private String _userName;
    
    public ReceiptUserInfo(String userName)
    {
        this._userName = userName;
    }

    public String getUserName() {
        return _userName;
    }

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("userName: "    + this._userName + "\n");
        return strBuff.toString();
    }
}
