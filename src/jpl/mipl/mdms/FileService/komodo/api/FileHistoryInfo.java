/*******************************************************************************
 * Copyright (C) 2015 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

import java.sql.Timestamp;

public class FileHistoryInfo {

    private String  m_name;
    private long    m_fileId;
    private String  m_userName;
    private String  m_host;
    private String  m_command;
    private Timestamp m_recordedAt;

    public FileHistoryInfo(String  i_name,
                           long    i_fileId,
                           String  i_userName,
                           String  i_host,
                           String  i_command,
                           Timestamp i_recordedAt)
    {
        this.m_name       = i_name;
        this.m_fileId     = i_fileId;
        this.m_userName   = i_userName;
        this.m_host       = i_host;
        this.m_command    = i_command;
        this.m_recordedAt = i_recordedAt;
    }

    public String getName() {
        return m_name;
    }

    public long getFileId() {
        return m_fileId;
    }

    public String getUserName() {
        return m_userName;
    }

    public String getHost() {
        return m_host;
    }

    public String getCommand() {
        return m_command;
    }

    public Timestamp getRecordedAt() {
        return m_recordedAt;
    }

    public String toString() {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("m_name: "       + this.m_name       + "\n");
        strBuff.append("m_fileId: "     + this.m_fileId     + "\n");
        strBuff.append("m_userName: "   + this.m_userName   + "\n");
        strBuff.append("m_host: "       + this.m_host       + "\n");
        strBuff.append("m_command: "    + this.m_command    + "\n");
        strBuff.append("m_recordedAt: " + this.m_recordedAt + "\n");
        return strBuff.toString();
    }
}
