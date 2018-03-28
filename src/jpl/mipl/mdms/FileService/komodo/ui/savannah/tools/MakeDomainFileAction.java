package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;

public class MakeDomainFileAction extends AbstractAction
{
    SavannahModel model;
    Component parent;
    
    //---------------------------------------------------------------------
    
    public MakeDomainFileAction(Component parent, SavannahModel model)
    {
        super("Make Domain File");
        this.parent = parent;
        this.model = model;
    }
    
    //---------------------------------------------------------------------
    
    
    public void run()
    {
        List groupList = model.getAvailableFeiServers();
        String[] groups = new String[groupList.size()];
        for (int i = 0; i < groups.length; ++i)
            groups[i] = (String) groupList.get(i);
        
        String curGroup = model.getCurrentFeiServer();
        File   curDir = model.getLocalDirectory();
        
        MakeDomainFilePanel mdfPanel = new MakeDomainFilePanel(groups, curGroup, 
                                                        curDir.getAbsolutePath());       
        
        Object[] options = new Object[] {"Apply", "Cancel"};
        int opt = JOptionPane.showOptionDialog(this.parent, mdfPanel, 
                                               "Make Domain File",
                                               JOptionPane.YES_NO_OPTION, 
                                               JOptionPane.PLAIN_MESSAGE,
                                               null, options, options[0]);
        if (opt == JOptionPane.CLOSED_OPTION || options[opt].equals("Cancel"))
        {
            return;
        }
               
        File   outputFile  = mdfPanel.getOutputFile();
        String servergroup = mdfPanel.getGroup();
        
        makeDomainFile(outputFile, servergroup);    
    }
    
    //---------------------------------------------------------------------
    
    protected void makeDomainFile(File outputFile, String serverGroup)
    {
        this.model.makeDomainFile(outputFile, serverGroup);
        
    }

    //---------------------------------------------------------------------

    public void actionPerformed(ActionEvent arg0)
    {
        run();        
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
}
