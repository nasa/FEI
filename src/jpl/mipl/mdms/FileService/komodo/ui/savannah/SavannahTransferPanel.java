package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * <b>Purpose:</b>
 *  Panel for displaying entries in the transfer history.
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
 * 08/11/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahTransferPanel.java,v 1.12 2004/09/24 01:09:48 ntt Exp $
 *
 */

public class SavannahTransferPanel extends JPanel implements
                                    PropertyChangeListener, 
                                    ListSelectionListener
{
    protected SavannahTransferModel _model;
    protected JList _jlist;
    protected DefaultListModel _listModel;
    protected List _transferList;
    protected JButton _removeButton;
    protected JButton _clearButton;
    protected String _title;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor using empty string for panel title
     * @param model Instance of SavannahTransferModel
     */
    
    public SavannahTransferPanel(SavannahTransferModel model)
    {
        this(model, "");
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param model Instance of SavannahTransferModel
     * @param title The label of panel
     */
    
    public SavannahTransferPanel(SavannahTransferModel model, String title)
    {
        this._model = model;
        this._model.addPropertyChangeListener(this);
        this._title = ((title != null) ? title : " ");
        init();
    }

    //---------------------------------------------------------------------
    
    /**
     * Initialization code.  To be called by constructor only. 
     */
    
    protected void init()
    {
        this._transferList = new Vector();
        
        this.setLayout(new BorderLayout());
        
        //construct the list
        this._listModel = new DefaultListModel();
        this._jlist = new JList(_listModel);
        this._jlist.setSelectionMode(ListSelectionModel.
                                     MULTIPLE_INTERVAL_SELECTION);
        this._jlist.addListSelectionListener(this);
        this._jlist.addMouseListener(new TransferListMouseListener());
        this._jlist.setCellRenderer(new TransferListRenderer());
        JScrollPane scrollPane = new JScrollPane(_jlist);
        this.add(scrollPane, BorderLayout.CENTER);
        
        //construct the buttons
        JPanel buttonPanel = createButtons();
        this.add(buttonPanel, BorderLayout.SOUTH);
        
        //construct title label
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
        JLabel titleLabel = new JLabel(_title);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titlePanel.add(Box.createHorizontalGlue());
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createHorizontalGlue());
        this.add(titlePanel, BorderLayout.NORTH);
        
        //some spacing
        this.add(Box.createHorizontalStrut(8), BorderLayout.WEST);
        this.add(Box.createHorizontalStrut(8), BorderLayout.EAST);
        
        //set the list
        updateTransferList(_model.getTransferList());
    }
 
    //---------------------------------------------------------------------
    
    /*
     * Create the buttons used for this panel.
     */
    
    protected JPanel createButtons()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        
        //construct the buttons
        this._removeButton = new JButton("Remove");
        this._removeButton.setMnemonic(KeyEvent.VK_R);
        this._clearButton = new JButton("Clear");
        this._clearButton.setMnemonic(KeyEvent.VK_C);
        
        //add action listener
        //_removeButton.addActionListener(this);
        //_clearButton.addActionListener(this);
        
        //set disabled initially
        this._removeButton.setEnabled(false);
        this._clearButton.setEnabled(false);
        
        //lay em out
        panel.add(Box.createHorizontalGlue());
        panel.add(_removeButton);
        panel.add(Box.createHorizontalStrut(25));
        panel.add(_clearButton);
        panel.add(Box.createHorizontalGlue());
        
        return panel;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Updates the transfer list displayed to that of the parameter list
     * if different from current list.
     * @param newList A list of SavannahTransferRecord objects to be 
     *                displayed.
     */
    
    protected void updateTransferList(List newList)
    {
        if (newList == null)
            newList = new Vector();
        if (newList.equals(_transferList))
            return;
        
        //grab indices of selected items
        Object[] selected = _jlist.getSelectedValues();
        
        //set entries of new transfer list to jlist
        this._transferList = newList;
        int numEntries = this._transferList.size();
        this._listModel.clear();
        for (int i = 0; i < numEntries; ++i)
            this._listModel.addElement(_transferList.get(i));
        
        _jlist.clearSelection();
        
        //try to select that which was previously selected
        if (selected.length > 0)
        {
            List indexList = new ArrayList();
            for (int i = 0; i < selected.length; ++i)
            {
                int index = _listModel.indexOf(selected[i]);
                if (index != -1)
                    indexList.add(new Integer(index));
            }
            int numIndices = indexList.size();
            if (numIndices > 0)
            {
                int[] selectedIndices = new int[numIndices];
                for (int i = 0; i < numIndices; ++i)
                {
                    selectedIndices[i]=((Integer)indexList.get(i)).intValue();
                }
                this._jlist.setSelectedIndices(selectedIndices);
            }
        }    
        else if (numEntries > 0)
        {
            this._jlist.ensureIndexIsVisible(numEntries - 1);
        }
        
        updateButtons();
        this._jlist.invalidate();
        this._jlist.repaint();
        
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * This method gets called when a bound property is changed.
     * @param pce A PropertyChangeEvent object describing the event 
     *            source and the property that has changed.
     */
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        if (propName.equalsIgnoreCase("TRANSFER_LIST_CHANGED"))
        {
            List newList = this._model.getTransferList();
            updateTransferList(newList);
        }
        else if (propName.equalsIgnoreCase("RECORD_VALUE_CHANGED"))
        {
            this._jlist.invalidate();
            this._jlist.repaint();
        }
    } 
   
    //---------------------------------------------------------------------
    
    /**
     * Implementation of the ListSelectionListener interface
     */
    
    public void valueChanged(ListSelectionEvent lse)
    {
        if (lse.getValueIsAdjusting())
            return;
        updateButtons();
    }
    
    //---------------------------------------------------------------------
    
    /*
     * Updates buttons behavior and availablility based on state of list
     */
    
    protected void updateButtons()
    {   
        Object[] selected = this._jlist.getSelectedValues();
        boolean canRemove = (selected.length > 0);
        boolean canRemoveAll = (this._listModel.size() > 0);
        this._removeButton.setAction(new RemoveAction(selected));
        this._clearButton.setAction(new ClearAction());
        this._removeButton.setEnabled(canRemove);
        this._clearButton.setEnabled(canRemoveAll);
    }
    
    //---------------------------------------------------------------------

    /**
     *  Unregister self as property listener, clear internal list, nullify
     *  state.  To be called 
     */
    
    public void nullify()
    {
        if (this._model != null)
        {
            this._model.removePropertyChangeListener(this);
            this._model = null;
        }
        
        if (this._transferList != null)
        {
            this._transferList.clear();
            this._transferList = null;
        }
        
        if (this._jlist != null)   
        {
            this._jlist.removeListSelectionListener(this);
            this._jlist = null;
            this._listModel.clear();
            this._listModel = null;
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates panel containing details of record state.
     * @param record The SavannahTransferRecord instance whose details
     * are to be used to populate the panel display.
     * @return JPanel containing details of record state.
     */
    
    protected JPanel createDetailsPanel(SavannahTransferRecord record)
    {
        JPanel panel = new JPanel();
        
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel lPanel = new JPanel();
        JPanel rPanel = new JPanel();
        lPanel.setLayout(new BoxLayout(lPanel, BoxLayout.Y_AXIS));
        rPanel.setLayout(new BoxLayout(rPanel, BoxLayout.Y_AXIS));
        

        lPanel.add(new JLabel(" Filename "));
        lPanel.add(new JLabel(" Filesize "));
        lPanel.add(new JLabel(" Filetype "));
        lPanel.add(new JLabel(" Transaction Type "));
        lPanel.add(new JLabel(" Transfer Time "));
        lPanel.add(new JLabel(" State "));
        lPanel.add(new JLabel(" Transaction Id "));

        rPanel.add(new JLabel(" = "+record.getFilename()+" "));
        rPanel.add(new JLabel(" = "+record.getFileSizeString()));
        rPanel.add(new JLabel(" = "+record.getFiletype()+" "));
        rPanel.add(new JLabel(" = "+
                   SavannahTransferRecord.transactionTypeAsString(
                                       record.getTransactionType())+" "));
        rPanel.add(new JLabel(" = "+record.getTransferTimeString()+" "));
        rPanel.add(new JLabel(" = "+
                SavannahTransferRecord.stateAsString(
                        record.getState())+" "));
        rPanel.add(new JLabel(" = "+record.getTransactionId()+" ")); 
        
        lPanel.setOpaque(false);
        rPanel.setOpaque(false);
        
        panel.add(lPanel);
        panel.add(rPanel);
        panel.add(Box.createHorizontalGlue());
        
        return panel;
    }
    
    //---------------------------------------------------------------------
    
    //=====================================================================
    //=====================================================================
    
    /**
     *  Implementation of ListCellRenderer which displays FEI entries
     */
    
    class TransferListRenderer extends JPanel implements ListCellRenderer
    {            
        JLabel iconLabel, filenameLabel, timeLabel, stateLabel;
        Color  evenColor = new Color(230, 235, 235);
        ImageIcon importIcon = null, exportIcon = null;
        
        //-----------------------------------------------------------------
        
        /**
         * Constructor.
         */
       
        public TransferListRenderer()
        {
            //load this icons
            URL imageURL = SavannahTransferPanel.class.getResource(
                                    "resources/import24.gif"); 
            if (imageURL != null)
                importIcon = new ImageIcon(imageURL, "Import");

            imageURL = SavannahTransferPanel.class.getResource(
                                    "resources/export24.gif");      
            
            if (imageURL != null)
                exportIcon = new ImageIcon(imageURL, "Export");
            
            //build the panel
            this.setLayout(new BorderLayout());
            this.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            
            iconLabel     = new JLabel();
            filenameLabel = new JLabel();
            timeLabel     = new JLabel();
            stateLabel    = new JLabel();
            
            Font oldFont = timeLabel.getFont();
            Font newFont = oldFont.deriveFont((float) (oldFont.getSize() - 2.0));
            timeLabel.setFont(newFont);
            
            JPanel subPanel = new JPanel(new BorderLayout());
            subPanel.setOpaque(false);
            subPanel.add(filenameLabel, BorderLayout.NORTH);
            subPanel.add(timeLabel, BorderLayout.SOUTH);
            subPanel.add(Box.createHorizontalStrut(150), BorderLayout.CENTER);
            this.add(iconLabel, BorderLayout.WEST);
            this.add(subPanel, BorderLayout.CENTER);
            this.add(stateLabel, BorderLayout.EAST);
            
        }
        
        //-----------------------------------------------------------------
        
        /**
         *  Return a component that has been configured to display the 
         *  specified value. That component's paint method is then called 
         *  to "render" the cell. If it is necessary to compute the 
         *  dimensions of a list because the list cells do not have a fixed
         *  size, this method is called to generate a component on which 
         *  getPreferredSize can be invoked.
         *
         *  Displays only the filename given a filepath.
         *
         *  @param list - The JList we're painting.
         *  @param value - The value returned by 
         *                  list.getModel().getElementAt(index)
         *  @param index - The cells index.
         *  @param isSelected - True if the specified cell was selected.
         *  @param cellHasFocus - True if the specified cell has the focus. 
         *  @return A component whose paint() method will render the specified
         *          value.  
         *  
         */
        
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {

            JPanel panel = this;
            Color color = list.getForeground(); 
            SavannahTransferRecord record = (SavannahTransferRecord) value;
            String filename = record.getFilename();
            
            //icons
            if (record.getTransactionType() == 
                                SavannahTransferRecord.TRANSACTION_TYPE_GET)
            {
                iconLabel.setIcon(importIcon);  
            }
            else
            {
                iconLabel.setIcon(exportIcon);
                File tmpFile = new File(filename);
                filename = tmpFile.getName();
            }
            
            filenameLabel.setText(filename);
            timeLabel.setText(record.getTransferTimeString());
            stateLabel.setText(SavannahTransferRecord.stateAsString(
                                                record.getState())+"  ");
            
            
            panel.setFont(list.getFont());     
            panel.setOpaque(true);
            if (isSelected)
            {
                panel.setBackground(list.getSelectionBackground()); 
                panel.setForeground(list.getSelectionForeground());                                
            }
            else
            {
                if (index % 2 == 0)
                    panel.setBackground(evenColor);
                else
                    panel.setBackground(list.getBackground());  
                panel.setForeground(color);
            }

            //---------------------         

            return panel;
        }         
    } //end_class
    
    //=====================================================================
    
    /**
     *  Extension of MouseAdapater for handling mouse events with list
     */
    
    class TransferListMouseListener extends MouseAdapter
    {
        public void mouseClicked(MouseEvent me) 
        {
            JList list = (JList) me.getSource();
            
            if (SwingUtilities.isLeftMouseButton(me) &&  
                                                me.getClickCount() > 1)
            {
                int index = list.locationToIndex(me.getPoint());
                if (index == -1 || index >= _listModel.size())
                    return;
                
                Object entry = _listModel.elementAt(index);
                if (entry == null)
                    return;
                
                if (!(entry instanceof SavannahTransferRecord))
                    return;
                
                JPanel detailsPanel = createDetailsPanel(
                                            (SavannahTransferRecord) entry);
                
                JOptionPane.showMessageDialog(SavannahTransferPanel.this, 
                            detailsPanel, "Transfer Details",
                            JOptionPane.PLAIN_MESSAGE);
                me.consume();
            }
            else if (SwingUtilities.isRightMouseButton(me))
            {
                int index = list.locationToIndex(me.getPoint());
                if (index == -1 || index >= _listModel.size())
                    return;
                
                _jlist.setSelectedIndex(index);
                SavannahTransferRecord record = (SavannahTransferRecord)
                                                 _listModel.elementAt(index);
                if (record == null)
                    return;
                
                JPopupMenu menu = createPopupMenu(new Object[] {record}, 
                                                  me.getPoint());
                if (menu != null)
                    menu.show(_jlist, me.getX(), me.getY());  
                me.consume();
            }
        }
        
        //-----------------------------------------------------------------
        
        public JPopupMenu createPopupMenu(Object[] selected, Point point)
        {
            if (selected == null || selected.length == 0)
                return null;
            
            JPopupMenu menu = new JPopupMenu();
            
            AbstractAction removeAction = null;
            AbstractAction clearAction  = null;
            AbstractAction detailsAction  = null;
            
            removeAction = new RemoveAction(selected);
            clearAction  = new ClearAction();
            
            if (selected.length == 1)
                detailsAction = new DetailsAction((SavannahTransferRecord)
                                                            selected[0]);
            
            menu.add(removeAction);
            menu.addSeparator();
            //menu.add(clearAction);
            if (detailsAction != null)
                menu.add(detailsAction);
            
            return menu;
        }
        
        //-----------------------------------------------------------------
        
    }
    
    //=====================================================================
    
    class RemoveAction extends AbstractAction
    {
        Object[] selection;
        
        //-----------------------------------------------------------------
        
        public RemoveAction(Object[] selected)
        {
            super("Remove");
            selection = selected;
            
            if (selection == null || selection.length == 0)
                this.setEnabled(false);
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            String message;
            if (selection.length == 1)
                message = "Remove selected entry?";
            else
                message = "Remove selected entries?";
            
            int answer = JOptionPane.showConfirmDialog(
                             SavannahTransferPanel.this,
                             message,
                             "Confirm Remove",
                             JOptionPane.YES_NO_CANCEL_OPTION,
                             JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION)
            {
                SavannahTransferRecord current;
                for (int i = 0; i < selection.length; ++i)
                {
                    current = (SavannahTransferRecord) selection[i];
                    _model.removeTransferRecord(current);
                }
            }
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    class ClearAction extends AbstractAction
    {
        //-----------------------------------------------------------------
        
        public ClearAction()
        {
            super("Clear");
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            int answer = JOptionPane.showConfirmDialog(
                    SavannahTransferPanel.this,
                    "Clear all entries?",
                    "Confirm Remove",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION)
            {
                _model.resetTransferList();
            }
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    class DetailsAction extends AbstractAction
    {
        SavannahTransferRecord record;
    
        //-----------------------------------------------------------------
        
        public DetailsAction(SavannahTransferRecord rec)
        {
            super("Details");
            record=rec;
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            JPanel detailsPanel = createDetailsPanel(record);

            JOptionPane.showMessageDialog(SavannahTransferPanel.this, 
                            detailsPanel, "Transfer Details",
                            JOptionPane.PLAIN_MESSAGE);
        }
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    /* *
     * Testing main.
     * /
    protected static void main(String[] args)
    {
        final SavannahTransferModel model = new SavannahTransferModel();
        SavannahTransferRecord rec = new SavannahTransferRecord("filename1",
                                "filetype1", 1, 321890L, 
                                 SavannahTransferRecord.TRANSACTION_TYPE_ADD);
        rec.setStartTime(System.currentTimeMillis());
        rec.setEndTime(System.currentTimeMillis()+12000L);
        rec.setState(SavannahTransferRecord.STATE_COMPLETE);
        model.addTransferRecord(rec);
        rec = new SavannahTransferRecord("filename2",
                            "filetype2", 12, 343L, 
                            SavannahTransferRecord.TRANSACTION_TYPE_GET);
        model.addTransferRecord(rec);
        rec.setStartTime(System.currentTimeMillis());
        rec.setState(SavannahTransferRecord.STATE_TRANSFERRING);
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());
        
        pane.add(new SavannahTransferPanel(model,"Transfer Log"), 
                 BorderLayout.CENTER);
        
        JButton tstButton = new JButton("tst_add");
        tstButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               int c = (int) (System.currentTimeMillis() % 100000);
               model.addTransferRecord(new SavannahTransferRecord("filename"+c,
                       "filetype2"+c, c, 343L+c, 
                       SavannahTransferRecord.TRANSACTION_TYPE_REPLACE));
           }
        });
        pane.add(tstButton, BorderLayout.SOUTH);
        
        
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.show();
    }
    */
    
    //=====================================================================
}
