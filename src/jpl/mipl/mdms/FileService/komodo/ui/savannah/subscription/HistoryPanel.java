package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;

/**
 * <b>Purpose:</b>
 * History panel containing information on records of files that have
 * been affected by a subscription task.  
 *
 *   <PRE>
 *   Copyright 2005, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2005.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 04/04/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: HistoryPanel.java,v 1.2 2005/04/08 01:14:54 ntt Exp $
 *
 */

public class HistoryPanel extends JPanel implements PropertyChangeListener 
{
    
    protected JLabel _filetypeKeyLabel;
    protected JLabel _filetypeValLabel;
    protected JLabel _outputKeyLabel;
    protected JLabel _outputValLabel;
    protected JLabel _taskKeyLabel;
    protected JLabel _taskValLabel;
    protected JLabel _countKeyLabel;
    protected JLabel _countValLabel;
    protected JList  _jList;
    protected JPanel _valuePanel;
    protected JPanel _headerPanel;
    protected JPanel _keywordPanel;
    protected JPanel _buttonPanel;
    protected JScrollPane _scrollPane;
    protected JButton _okButton;
    protected JButton _clearButton;
    
    protected MetaSubscription _ms;
    protected DefaultListModel _listModel;
    
    /** allow user to clear history from panel */
    boolean _canClear = true;
    
    //---------------------------------------------------------------------
    
    /** Creates new form HistoryPanel */
    public HistoryPanel(MetaSubscription ms) 
    {
        this._ms = ms;
        this._ms.addPropertyChangeListener(this);
        initComponents();
        init();
        refresh();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Queries the history of the subscription and updates view. 
     */
    
    protected void refresh()
    {
        Object obj = this._ms.getHistory();
        
        if (obj instanceof List)
        {
            List results = (List) obj;
            Result result; 
            _listModel.clear();
            
            //list of result objects?
            int count = results.size();
            for (int i = 0; i < count; ++i)
                _listModel.addElement(results.get(i));     
            
            this._countValLabel.setText(count+"");
            
            boolean gtZero = count > 0;
            if (gtZero)
                this._jList.ensureIndexIsVisible(count - 1);
            this._clearButton.setEnabled(gtZero && _canClear);
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void initComponents() {//GEN-BEGIN:initComponents
        GridBagConstraints gridBagConstraints;

        _scrollPane = new JScrollPane();
        _listModel  = new DefaultListModel();
        _jList      = new JList(_listModel);
        _jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _jList.setCellRenderer(new ResultCellRenderer());
        
        _headerPanel      = new JPanel();
        _buttonPanel      = new JPanel();
        _keywordPanel     = new JPanel();
        _taskKeyLabel     = new JLabel();
        _filetypeKeyLabel = new JLabel();
        _outputKeyLabel   = new JLabel();
        _valuePanel       = new JPanel();
        _taskValLabel     = new JLabel();
        _filetypeValLabel = new JLabel();
        _outputValLabel   = new JLabel();
        _countKeyLabel    = new JLabel();
        _countValLabel    = new JLabel();

        setLayout(new BorderLayout());

        _scrollPane.setViewportView(_jList);

        add(_scrollPane, BorderLayout.CENTER);

        _headerPanel.setLayout(new BoxLayout(_headerPanel, BoxLayout.X_AXIS));
        _headerPanel.add(Box.createHorizontalStrut(10));
        
        _headerPanel.setFocusable(false);
        _keywordPanel.setLayout(new GridLayout(4, 0));

        _keywordPanel.setMaximumSize(new Dimension(3000, 32767));
        _taskKeyLabel.setText("Task:");
        _keywordPanel.add(_taskKeyLabel);

        _filetypeKeyLabel.setText("Filetype:");
        _keywordPanel.add(_filetypeKeyLabel);

        _outputKeyLabel.setText("Directory:");
        _keywordPanel.add(_outputKeyLabel);

        _countKeyLabel.setText("File count:");
        _keywordPanel.add(_countKeyLabel);
        
        _headerPanel.add(_keywordPanel);
        _headerPanel.add(Box.createHorizontalStrut(12));

        _valuePanel.setLayout(new GridLayout(4, 0));

        _valuePanel.setAlignmentX(0.0F);
        
        String taskValue = SubscriptionConstants.taskTypeToString(
                                           this._ms.getTaskType());
        taskValue += "  [Id = "+this._ms.getId()+"] ";
        
        _taskValLabel.setText(taskValue);
        _valuePanel.add(_taskValLabel);

        _filetypeValLabel.setText((String) this._ms.getSource());
        _valuePanel.add(_filetypeValLabel);

        _outputValLabel.setText((String) this._ms.getTarget());
        _valuePanel.add(_outputValLabel);

        _countValLabel.setText(this._listModel.getSize()+"");
        _valuePanel.add(_countValLabel);
        
        _headerPanel.add(_valuePanel);
        _headerPanel.add(Box.createHorizontalStrut(10));

        add(_headerPanel, BorderLayout.NORTH);
        
        _buttonPanel = new JPanel();
        _okButton = new JButton("OK");
        _buttonPanel.add(_okButton);
        _clearButton = new JButton("Clear");
        _buttonPanel.add(_clearButton);
        add(_buttonPanel, BorderLayout.SOUTH);

    }//GEN-END:initComponents
    
    //---------------------------------------------------------------------
    
    /**
     * Add action listeners to components
     */
    protected void init()
    {
        this._okButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               Window w = SwingUtilities.windowForComponent(HistoryPanel.this);
               if (w instanceof JDialog)
               {
                   ((JDialog) w).setVisible(false);
               }
               else
                   w.setVisible(false);
           }            
        });
        
        this._clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                int rVal = JOptionPane.showConfirmDialog(HistoryPanel.this, 
                        "Clear all entries?", "Clear Confirm", 
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                
                if (rVal != JOptionPane.YES_OPTION)
                    return;
                
                _ms.clearHistory();
            }            
         });
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Implementation of the PropertyChangeListener interface.
     * Looks for changes in history property of the metasubscription
     * @param pce PropertyChangeEvent instance 
     */
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        if (propName.equalsIgnoreCase(MetaSubscription.PROPERTY_HISTORY))
        {
            refresh();
        }     
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Render cell for the history list.  Displays filename,
     * filesize, and modification time of the file.
     */
    
    class ResultCellRenderer extends JPanel implements ListCellRenderer
    {
        JLabel _filename, _date, _size, _units, _comma;
        
        public ResultCellRenderer()
        {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            _filename = new JLabel();
            _size     = new JLabel();
            _date     = new JLabel();
            _units    = new JLabel(" bytes,  ");
            _comma    = new JLabel(", ");
            add(Box.createHorizontalStrut(5));
            add(_filename);
            add(_comma);
            add(Box.createHorizontalStrut(5));
            add(Box.createHorizontalGlue());
            add(_size);
            add(_units);
            add(Box.createHorizontalStrut(2));
            add(_date);
            add(Box.createHorizontalStrut(5));            
        }
        
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            Result result = (Result) value;
            String name = result.getName();
            String date = DateTimeUtil.getDateCCSDSAString(
                                            result.getFileModificationTime());
            _filename.setText(name);
            _date.setText(date);
            _size.setText(result.getSize()+"");
            this.setToolTipText(name);
            
            return this;
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Remove this as property change listener of the metasubscription.
     * Clear references to the metasubscription and list model.
     * This should only be called when history panel will no longer
     * be used.
     */
    
    public void nullify()
    {
        this._ms.removePropertyChangeListener(this);
        this._ms = null;
        this._listModel.clear();
        this._listModel = null;
    }
    
    //---------------------------------------------------------------------
}
