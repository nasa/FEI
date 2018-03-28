package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.SetDateTimeFormatAction;

/**
 * <b>Purpose:</b>
 *  Panel containing session options.
 *
 *   <PRE>
 *   Copyright 2004, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2004.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 08/09/2004        Nick             Initial Release
 * 09/22/2009        Nick             Added diff option for file transfers
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SessionOptionsPanel.java,v 1.8 2009/09/22 21:58:19 ntt Exp $
 *
 */

public class SessionOptionsPanel extends JMenu  implements ItemListener,
                                                PropertyChangeListener
{
    protected JCheckBoxMenuItem _checksumBox;
    protected JCheckBoxMenuItem _restartBox;
    protected JCheckBoxMenuItem _resumeBox;
    
    protected JCheckBoxMenuItem _safereadBox;
    protected JCheckBoxMenuItem _receiptBox;
    protected JCheckBoxMenuItem _diffBox;

    //protected JCheckBoxMenuItem _fileVersionBox;
    
    protected SavannahSessionModel _sessionModel;
    
    //---------------------------------------------------------------------
    
    public SessionOptionsPanel(SavannahSessionModel model)
    {
        _sessionModel = model;        
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
    	this.setText("Session Options");
    	this.setToolTipText("View and set session options");
    	this.setMnemonic(KeyEvent.VK_S);
    	
        this._sessionModel.addPropertyChangeListener(this);
        
        //construct the option boxes
        _checksumBox = new JCheckBoxMenuItem("Compute Checksum");
        _restartBox  = new JCheckBoxMenuItem("Enable Restart");
        _resumeBox   = new JCheckBoxMenuItem("Enable Resume");
        
        _safereadBox  = new JCheckBoxMenuItem("Enable Saferead");
        _receiptBox   = new JCheckBoxMenuItem("Enable Receipts");
        _diffBox      = new JCheckBoxMenuItem("Enable Diff");
        
        //_checksumBox tool tips
        _checksumBox.setToolTipText("Enables checksum verification");
        _restartBox.setToolTipText("Enables transfer restart");
        _resumeBox.setToolTipText("Enables transfer resume "+
                                  "(checksum and restart)");
        _safereadBox.setToolTipText("Enables saferead for file downloads");
        _receiptBox.setToolTipText("Enables receipt for file downloads");
        _diffBox.setToolTipText("Enables file diff for file transfers");

        
        //mneumonics...
        _checksumBox.setMnemonic(KeyEvent.VK_C);
        _checksumBox.setDisplayedMnemonicIndex(8);
        _restartBox.setMnemonic(KeyEvent.VK_T);
        _resumeBox.setMnemonic(KeyEvent.VK_U); 
        _safereadBox.setMnemonic(KeyEvent.VK_C);
        _receiptBox.setMnemonic(KeyEvent.VK_P);
        _diffBox.setMnemonic(KeyEvent.VK_D);
        
        //add listeners
        _checksumBox.addItemListener(this);
        _restartBox.addItemListener(this);
        _resumeBox.addItemListener(this);
        _safereadBox.addItemListener(this);
        _receiptBox.addItemListener(this);
        _diffBox.addItemListener(this);

        
        //add to layout
//        JPanel panel = new JPanel();
//        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
//        panel.add(_checksumBox);
//        panel.add(_restartBox);
//        panel.add(_resumeBox);
//        this.add(panel);
        this.add(_checksumBox);
        this.add(_restartBox);
        this.add(_resumeBox);
        this.add(new JSeparator());
        this.add(_safereadBox);
        this.add(_receiptBox);
        this.add(_diffBox);
        
        //set according to model state
        _checksumBox.setSelected(_sessionModel.isComputeChecksumEnabled());
        _restartBox.setSelected(_sessionModel.isRestartTransferEnabled());
        _resumeBox.setSelected(_sessionModel.isResumeTransferEnabled());
        _safereadBox.setSelected(_sessionModel.isSafereadEnabled());
        _receiptBox.setSelected(_sessionModel.isReceiptEnabled());
        _diffBox.setSelected(_sessionModel.isDiffEnabled());
    }

    //---------------------------------------------------------------------
    
    public void itemStateChanged(ItemEvent ie) 
    {       
        Object source = ie.getItemSelectable();
        boolean isSelected = (ie.getStateChange() == ItemEvent.SELECTED);
        
        if (source == _checksumBox)
        {
            if (isSelected != _sessionModel.isComputeChecksumEnabled())
                _sessionModel.enableComputeChecksum(isSelected);
        }
        else if (source == _restartBox)
        {
            if (isSelected != _sessionModel.isRestartTransferEnabled())
                _sessionModel.enableRestartTransfer(isSelected);
        }
        else if (source == _resumeBox)
        {
            _checksumBox.setEnabled(!isSelected);
            _restartBox.setEnabled(!isSelected);
            
            if (isSelected != _sessionModel.isResumeTransferEnabled())
                _sessionModel.enableResumeTransfer(isSelected);
        }
        else if (source == _safereadBox)
        {
            if (isSelected != _sessionModel.isSafereadEnabled())
                _sessionModel.enableSaferead(isSelected);
        }
        else if (source == _receiptBox)
        {
            if (isSelected != _sessionModel.isReceiptEnabled())
                _sessionModel.enableReceipt(isSelected);
        }
        else if (source == _diffBox)
        {
            if (isSelected != _sessionModel.isDiffEnabled())
                _sessionModel.enableDiff(isSelected);
        }
    }
    
    //---------------------------------------------------------------------
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        if (propName.equalsIgnoreCase(SavannahSessionModel.COMPUTE_CHECKSUM_ENABLED))
        {
            boolean flag = ((Boolean) pce.getNewValue()).booleanValue();
            if (flag != _checksumBox.isSelected())
                _checksumBox.setSelected(flag);
        }
        else if (propName.equalsIgnoreCase(SavannahSessionModel.RESTART_TRANSACTION_ENABLED))
        {
            boolean flag = ((Boolean) pce.getNewValue()).booleanValue();
            if (flag != _restartBox.isSelected())
                _restartBox.setSelected(flag);
        }
        else if (propName.equalsIgnoreCase(SavannahSessionModel.RESUME_TRANSACTION_ENABLED))
        {
            boolean flag = ((Boolean) pce.getNewValue()).booleanValue();
            if (flag != _resumeBox.isSelected())
                _resumeBox.setSelected(flag);
        }
        else if (propName.equalsIgnoreCase(SavannahSessionModel.FILE_RECEIPT_ENABLED))
        {
            boolean flag = ((Boolean) pce.getNewValue()).booleanValue();
            if (flag != _receiptBox.isSelected())
                _receiptBox.setSelected(flag);
        }
        else if (propName.equalsIgnoreCase(SavannahSessionModel.FILE_SAFEREAD_ENABLED))
        {
            boolean flag = ((Boolean) pce.getNewValue()).booleanValue();
            if (flag != _safereadBox.isSelected())
                _safereadBox.setSelected(flag);
        }
        else if (propName.equalsIgnoreCase(SavannahSessionModel.FILE_DIFF_ENABLED))
        {
            boolean flag = ((Boolean) pce.getNewValue()).booleanValue();
            if (flag != _diffBox.isSelected())
                _diffBox.setSelected(flag);
        }
    }
    
    //---------------------------------------------------------------------
    
}
