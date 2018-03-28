/*******************************************************************************
 * Copyright (C) 2001 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

public class ReceiptDateInfo {
    private String _receiptDate;  // The date is a string in this format  2014-06-11
    
    public ReceiptDateInfo(String receiptDate)
    {
        this._receiptDate = receiptDate;
    }

    public String getReceiptDate() {
        return _receiptDate;
    }

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("receiptDate: "    + this._receiptDate + "\n");
        return strBuff.toString();
    }
}
