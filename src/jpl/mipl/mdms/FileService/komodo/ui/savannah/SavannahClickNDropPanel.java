package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * <b>Purpose:</b>
 * Panel created to allow user to "Click and Drop" files
 * from one SavannahList to another.
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
 * <B>Date              Who                        What</B>
 * ----------------------------------------------------------------------------
 * 06/04/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahClickNDropPanel.java,v 1.10 2016/02/03 21:44:03 ntt Exp $
 *
 */

public class SavannahClickNDropPanel extends JPanel implements 
                                                PropertyChangeListener
{
    
    private final String __classname = "SavannahClickNDropPanel";
    
    protected JPanel        _mainPanel;
    protected SavannahModel _model;
    protected SavannahList  _fileList;
    protected SavannahList  _feiList;
    protected JButton _feiToFileButton;
    protected JButton _fileToFeiButton;
    protected String  _lIconPath = "resources/forward24.gif";
    protected String  _rIconPath = "resources/back24.gif";
    protected String  _lRollIconPath = "resources/forward24_ro.gif";
    protected String  _rRollIconPath = "resources/back24_ro.gif";
    protected ListSelectionListener _feiListener;
    protected ListSelectionListener _fileListener;
    protected String _fileToFeiStr = "FILE TO FEI";
    protected String _feiToFileStr = "FEI TO FILE";
    
    //---------------------------------------------------------------------
    
    public SavannahClickNDropPanel(SavannahModel model,
                                   SavannahList fileList,
                                   SavannahList feiList)
    {
        _model    = model;
        _fileList = fileList;
        _feiList  = feiList;
        
        _fileListener = new FileListListener();
        _feiListener  = new FeiListListener();
        _fileList.addListSelectionListener(_fileListener);
        _feiList.addListSelectionListener(_feiListener);
        
        _model.addPropertyChangeListener(this);
        
        init();        
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        this.setLayout(new BorderLayout());
        
        //set layout to y-box
        _mainPanel = new JPanel();
        _mainPanel.setLayout(new BoxLayout(_mainPanel, BoxLayout.Y_AXIS));
        //this.setBorder(BorderFactory.createEtchedBorder());
        
        //Look for the image.        
        URL lURL = SavannahClickNDropPanel.class.getResource(_lIconPath);
        URL rURL = SavannahClickNDropPanel.class.getResource(_rIconPath);
        
        URL lRollURL = SavannahClickNDropPanel.class.getResource(
                                                 _lRollIconPath);
        URL rRollURL = SavannahClickNDropPanel.class.getResource(
                                                 _rRollIconPath);
        
        _feiToFileButton = new JButton();
        _feiToFileButton.setToolTipText("Transfer files from FEI to "+
                                        "local system");
        _feiToFileButton.setEnabled(false);
        _feiToFileButton.addActionListener(new FeiToFileActionListener());
        
        _fileToFeiButton = new JButton();
        _fileToFeiButton.setToolTipText("Transfer files from local system "+
                                        "to FEI");
        _fileToFeiButton.setEnabled(false);
        _fileToFeiButton.addActionListener(new FileToFeiActionListener());
        
        if (lURL != null && rURL != null)
        {
            _fileToFeiButton.setIcon(new ImageIcon(lURL, _fileToFeiStr));
            _feiToFileButton.setIcon(new ImageIcon(rURL, _feiToFileStr));
        }
        else
        {
            _fileToFeiButton.setText(_fileToFeiStr);
            _feiToFileButton.setText(_feiToFileStr);            
        }
        
        if (lRollURL != null && rRollURL != null)
        {
            _fileToFeiButton.setRolloverEnabled(true);
            _feiToFileButton.setRolloverEnabled(true);
            _fileToFeiButton.setRolloverIcon(
                                new ImageIcon(lRollURL, _fileToFeiStr));
            _feiToFileButton.setRolloverIcon(
                                new ImageIcon(rRollURL, _feiToFileStr));
        }
        
        _mainPanel.add(Box.createVerticalGlue());
        _mainPanel.add(_fileToFeiButton);
        _mainPanel.add(Box.createVerticalStrut(25));
        _mainPanel.add(_feiToFileButton);
        _mainPanel.add(Box.createVerticalGlue());
        
        this.add(_mainPanel, BorderLayout.CENTER);
        this.add(new JSeparator(JSeparator.VERTICAL), BorderLayout.WEST);
        this.add(new JSeparator(JSeparator.VERTICAL), BorderLayout.EAST);
    }
    
    //---------------------------------------------------------------------
    
    class FeiListListener implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent event)
        {
            if (event.getValueIsAdjusting())
                return;
                        
            Object[] values = _feiList.getSelectedValues(); 
            
            boolean canExport = false;
            boolean canImport = false;            
            try {
                canExport = _feiList.canExport(values);
            } catch (SavannahListException slEx) {
                canExport = false;
            }            
            try {
                canImport = _fileList.canImport(values);
            } catch (SavannahListException slEx) {
                canImport = false;
            }
                        
            if (values != null && values.length > 0 && canExport && canImport)
            {
                if (!_feiToFileButton.isEnabled())
                {
                    _feiToFileButton.setEnabled(true);
                }
            }
            else
            {
                if (_feiToFileButton.isEnabled())
                {
                    _feiToFileButton.setEnabled(false);
                }
            }
            
            // File To Fei check
            if (_model.getCurrentFeiType() == null) 
            {
                if (_fileToFeiButton.isEnabled())           
                    _fileToFeiButton.setEnabled(false);
            }
            else
            {
                //might need to update file-to-fei if transfer is possible
                
                //when lots of local files are selected while deleting a bunch
                //of remote files, this takes a lot of useless time 
                //performing repeated file checks, so use app model for
                //is adjust parameter.  Listener will check that and 
                //skip if set to true
                
                //Original (2016.02.03):
                //_fileListener.valueChanged(
                //             new ListSelectionEvent(this, -1, -1, false));
                
                _fileListener.valueChanged(
                        new ListSelectionEvent(this, -1, -1, _model.isBusy()));
            }
        }        
    }
    
    //---------------------------------------------------------------------
    
    class FileListListener implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent event)
        {
            if (event.getValueIsAdjusting())
                return;
            
            Object[] values = _fileList.getSelectedValues(); 
            
            boolean canExport = false;
            boolean canImport = false;            
            try {
                canExport = _fileList.canExport(values);
            } catch (SavannahListException slEx) {
                canExport = false;
            }            
            try {
                canImport = _feiList.canImport(values);
            } catch (SavannahListException slEx) {
                canImport = false;
            }
                        
            if (values != null && values.length > 0 && canExport && canImport)
            {
                if (!_fileToFeiButton.isEnabled())
                {
                    _fileToFeiButton.setEnabled(true);
                    //_model.printDebug(__classname+"::FileListener:: "+
                    //        "Enabling FileToFeiButton");
                }
            }
            else
            {
                if (_fileToFeiButton.isEnabled())
                {
                    _fileToFeiButton.setEnabled(false);
                    //_model.printDebug(__classname+"::FileListener:: "+
                    //        "Disabling FileToFeiButton");
                }
            }
        }        
    }
    
    //---------------------------------------------------------------------
    
    class FeiToFileActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent ae)
        {
            Object[] selected = _feiList.getSelectedValues();
            String[] values = toStringArray(selected);
            
            if (values != null && values.length > 0)
                _fileList.importEntry(values);
        }        
    }
    
    //---------------------------------------------------------------------
    
    class FileToFeiActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent ae)
        {
            Object[] selected = _fileList.getSelectedValues();
            String[] values = toStringArray(selected);
            
            if (values != null && values.length > 0)
                _feiList.importEntry(values);
        }        
    }
    
    //---------------------------------------------------------------------
    
    protected String[] toStringArray(Object[] array)
    {
        if (array == null)
            return null;
        
        String[] stringArray = new String[array.length];
        for (int i = 0; i < array.length; ++i)
            stringArray[i] = array[i].toString();
        
        return stringArray;        
    }
    
    //---------------------------------------------------------------------
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        if (propName.equalsIgnoreCase("CURRENT_FEI_TYPE"))
        {
            Object newType = pce.getNewValue();
            if (newType == null)
                _fileToFeiButton.setEnabled(false);
        }
    }
    
    //---------------------------------------------------------------------
}
