package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.util.DateTimeFormatter;

/**
 * <b>Purpose:</b>
 *  Action implementation for setting the date/time format string.
 *
 *   <PRE>
 *   Copyright 2009, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2009.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 05/02/2009        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SetDateTimeFormatAction.java,v 1.3 2009/12/10 00:12:08 ntt Exp $
 *
 */

public class SetDateTimeFormatAction extends AbstractAction
{
    SavannahModel model;
    Component parent;
    
    //---------------------------------------------------------------------
    
    public SetDateTimeFormatAction(Component parent, SavannahModel model)
    {
        super("Modify Date/Time Format");
        this.parent = parent;
        this.model = model;
    }
    
    //---------------------------------------------------------------------
    
    
    public void run()
    {        
        DateTimeFormatter formatter = model.getDateTimeFormatter();
        String initFormat = formatter == null ? null :
                            formatter.getFormatString();
        
        SetDateTimeFormatPanel dtfPanel = new SetDateTimeFormatPanel(initFormat);       
        
        Object[] options = new Object[] {"Apply", "Cancel"};
        int opt = JOptionPane.showOptionDialog(this.parent, dtfPanel, 
                                               "Set Date/Time Format",
                                               JOptionPane.YES_NO_OPTION, 
                                               JOptionPane.PLAIN_MESSAGE,
                                               null, options, options[0]);
        if (opt == JOptionPane.CLOSED_OPTION || options[opt].equals("Cancel"))
        {
            return;
        }
        
        String format = dtfPanel.getFormat();
        
        try {
            new SimpleDateFormat(format);            
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this.parent, "BAD FORMAT: "+format, 
                    "Set Date/Time",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        setDateTimeFormat(format);    
    }
    
    //---------------------------------------------------------------------
    
    protected void setDateTimeFormat(String format)
    {
        this.model.setDateTimeFormat(format);     
    }

    //---------------------------------------------------------------------

    public void actionPerformed(ActionEvent arg0)
    {
        run();        
    }
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
}
