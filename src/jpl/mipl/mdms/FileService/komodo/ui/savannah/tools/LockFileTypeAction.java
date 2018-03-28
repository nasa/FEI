package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;

public class LockFileTypeAction extends AbstractAction
{
    SavannahModel model;
    Component parent;
    
    //---------------------------------------------------------------------
    
    public LockFileTypeAction(Component parent, SavannahModel model)
    {
        super("Lock/Unlock Filetype");
        this.parent = parent;
        this.model = model;
    }
    
    //---------------------------------------------------------------------
    
    
    public void run()
    {
        String[] allFiletypes = model.getAllFiletypes();
        
        String curGroup = model.getCurrentFeiServer();
        String curType  = model.getCurrentFeiType();
       
        
        LockFileTypePanel lftPanel = new LockFileTypePanel(
                                                allFiletypes, 
                                                curGroup, curType,
                                                Constants.LOCKFILETYPE);       
        
        Object[] options = new Object[] {"Apply", "Cancel"};
        int opt = JOptionPane.showOptionDialog(this.parent, lftPanel, 
                                               "Lock/Unlock Filetype",
                                               JOptionPane.YES_NO_OPTION, 
                                               JOptionPane.PLAIN_MESSAGE,
                                               null, options, options[0]);
        if (opt == JOptionPane.CLOSED_OPTION || options[opt].equals("Cancel"))
        {
            return;
        }
        
        String operation   = lftPanel.getOperation();
        String filetype    = lftPanel.getFileType();
        String option      = lftPanel.getOperationMode();
        
        if (operation.equals(Constants.LOCKFILETYPE))
            lockFiletype(filetype, option);
        else if (operation.equals(Constants.UNLOCKFILETYPE))
            unlockFiletype(filetype, option);
    }
    
    //---------------------------------------------------------------------
    
    protected void lockFiletype(String filetype, String option)
    {
        this.model.lockFiletype(filetype, option);
        
    }

    //---------------------------------------------------------------------
    
    protected void unlockFiletype(String filetype, String option)
    {
        this.model.unlockFiletype(filetype, option);       
    }
    
    //---------------------------------------------------------------------

    public void actionPerformed(ActionEvent arg0)
    {
        run();        
    }
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
}
