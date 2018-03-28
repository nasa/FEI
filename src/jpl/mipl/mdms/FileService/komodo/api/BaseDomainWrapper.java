package jpl.mipl.mdms.FileService.komodo.api;

import java.util.LinkedList;

public class BaseDomainWrapper extends Domain
{
    Domain domain;
    
    public BaseDomainWrapper(Domain domain)
    {
        this.domain = domain;
    }
    
    @Override
    public String getDefaultGroup()
    {
        return domain.getDefaultGroup();
    }

    @Override
    public FileTypeInfo getFileType(String typeName) throws SessionException
    {
        return domain.getFileType(typeName);
    }

    @Override
    public FileTypeInfo getFileType(String groupName, String typeName)
            throws SessionException
    {
        return domain.getFileType(groupName, typeName);
    }

    @Override
    public LinkedList getFileTypeNames() throws SessionException
    {
        return domain.getFileTypeNames();
    }

    @Override
    public LinkedList getFileTypeNames(String groupName)
            throws SessionException
    {
        return domain.getFileTypeNames(groupName);
    }

    @Override
    public LinkedList getGroupNames()
    {
        return domain.getGroupNames();
    }

    @Override
    public ServerInfo getServerInfo(String serverName) throws SessionException
    {
        return domain.getServerInfo(serverName);
    }

    @Override
    public ServerInfo getServerInfo(String groupName, String serverName)
            throws SessionException
    {
        return domain.getServerInfo(groupName, serverName);
    }

    @Override
    public ServerInfo getServerInfoByIndex(int index)
    {
        return domain.getServerInfoByIndex(index);
    }

    @Override
    public LinkedList getServerInfoFromFileType(String groupName,
            String typeName) throws SessionException
    {
        return domain.getServerInfoFromFileType(groupName, typeName);
    }

    @Override
    public LinkedList getServerInfoFromGroup(String groupName)
            throws SessionException
    {
        return domain.getServerInfoFromGroup(groupName);
    }

    @Override
    public LinkedList getServerNames(String group)
    {
        return domain.getServerNames(group);
    }

    @Override
    public boolean isGroupDefined(String servergroup)
    {
        return domain.isGroupDefined(servergroup);
    }

    @Override
    public boolean isGroupDynamic(String groupName) throws SessionException
    {
        return domain.isGroupDynamic(groupName);
    }

    @Override
    public String toString()
    {
        return domain.toString();
    }

}
