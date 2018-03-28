package jpl.mipl.mdms.FileService.komodo.api;

import java.sql.Timestamp;

public class ReceiptInfo {
    private int _receiptId;
    private String _userName;
    private String _host;
    private long _fileId;
    private int _ftId;
    private String _command; 
    private String _checksum;
    private Timestamp _recordedAt;
    
    public ReceiptInfo(int receiptId, String userName, String host, long fileId, int ftId, String command, String checksum, Timestamp recordedAt) {
        this._receiptId = receiptId;
        this._userName = userName;
        this._host = host;
        this._fileId = fileId;
        this._ftId = ftId;
        this._command = command;
        this._checksum = checksum;
        this._recordedAt = recordedAt;
        
    }
    
    public int getReceiptId() {
        return this._receiptId;
    }

    public String getUserName() {
        return _userName;
    }


    public String getHost() {
        return _host;
    }


    public long getFileId() {
        return _fileId;
    }


    public int getFtId() {
        return _ftId;
    }


    public String getCommand() {
        return _command;
    }


    public String getChecksum() {
        return _checksum;
    }


    public Timestamp getRecordedAt() {
        return _recordedAt;
    }    

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("id: "+this._receiptId+"\n");
        strBuff.append("user: "+this._userName+"\n");
        strBuff.append("host: "+this._host+"\n");
        strBuff.append("fileId: "+this._fileId+"\n");
        strBuff.append("ftId: "+this._ftId+"\n");
        strBuff.append("command: "+this._command+"\n");
        strBuff.append("checksum: "+this._checksum+"\n");
        strBuff.append("recordDate: "+this._recordedAt+"\n");
        
        return strBuff.toString();
    }

}
