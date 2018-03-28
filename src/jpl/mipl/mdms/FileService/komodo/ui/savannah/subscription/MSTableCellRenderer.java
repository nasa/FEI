package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

/**
 * <b>Purpose:</b>
 *  Cell rendered for the Savannah subscription table using a 
 *  MSTabelModel instance as table model.  Renders
 *  cells of the table for id, type, filetype, directory, and state
 *  attributes.
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
 * 03/28/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MSTableCellRenderer.java,v 1.3 2005/04/08 01:14:55 ntt Exp $
 *
 */

public class MSTableCellRenderer extends JLabel implements TableCellRenderer
{
    protected Color _highlightColor              = Color.BLUE;
    protected Color _errorColor                  = Color.RED;
    protected MSTableModel _tableModel           = null;
    protected MetaSubscriptionManager _manager   = null;
    
    //---------------------------------------------------------------------
    
    public MSTableCellRenderer(MetaSubscriptionManager manager,
                               MSTableModel tableModel)
    {
        this._manager    = manager;
        this._tableModel = tableModel;
        this.setOpaque(true);
        this.setBorder(null);
    }
    
    //---------------------------------------------------------------------
    
    public Color getHighlightColor()
    {
        return _highlightColor;
    }
    
    //---------------------------------------------------------------------
    
    public void setHighlightColor(Color newColor)
    {
        if (!_highlightColor.equals(newColor))
            _highlightColor = newColor;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the component used for drawing the cell. This method
     * is used to configure the renderer appropriately before drawing. 
     * @param table The JTable that is asking the renderer to draw; 
     *              can be null
     * @param value The value of the cell to be rendered. It is up 
     *              to the specific renderer to interpret and draw the 
     *              value. For example, if value is the string "true", 
     *              it could be rendered as astring or it could be 
     *              rendered as a check box that is checked. null is 
     *              a valid value
     * @param isSelected True if the cell is to be rendered with the 
     *              selection highlighted; otherwise false
     * @param hasFocus If true, render cell appropriately. For example, 
     *              put a special border on the cell, if the cell can 
     *              be edited, render in the color used to indicate 
     *              editing
     * @param row The row index of the cell being drawn. When drawing 
     *             the header, the value of row is -1
     * @param column The column index of the cell being drawn
     */

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column)
    {
        //need to know about element to set some attributes
        MetaSubscription element = (MetaSubscription) 
                                       this._tableModel.elementAt(row);
        
        //handle label contents
        String columnName = table.getColumnName(column);
        String newValue = translate(columnName, value, element);
        
        if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_ID))
        {
            this.setText(newValue);
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_TASK))
        {
            this.setText(newValue);
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_FILETYPE))
        {
            this.setText(newValue);
            this.setHorizontalAlignment(SwingConstants.LEFT);
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_DIRECTORY))
        {
            this.setText(newValue);
            this.setHorizontalAlignment(SwingConstants.LEFT);
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_STATE))
        {
            this.setText(newValue);
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }
        
        //handle label color
        if (isSelected)
        {
            this.setBackground(table.getSelectionBackground()); 
            this.setForeground(table.getSelectionForeground());                                
        }
        else
        {
            this.setBackground(table.getBackground());  
            this.setForeground(table.getForeground());    
        }
        
        //set foreground color to error color if in error state
        if (element.getState() == SubscriptionConstants.STATE_ERROR)
        {
            this.setForeground(_errorColor);
        }
        //set foreground color of state to error color if in repair state
        if (element.getState() == SubscriptionConstants.STATE_REPAIR &&
            columnName.equalsIgnoreCase(MSTableModel.COLUMN_STATE))
        {
            this.setForeground(_errorColor);
        }

        //---------------------         

        return this;
    }         
    
    //---------------------------------------------------------------------
    
    public static String translate(String columnName, Object value, 
                            MetaSubscription element)
    {
        String newValue = null;
        if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_ID))
        {
            newValue = value.toString();
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_TASK))
        {
            Integer val = (Integer) value;
            newValue = SubscriptionConstants.taskTypeToString(val.intValue());
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_FILETYPE))
        {
            newValue = value.toString();
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_DIRECTORY))
        {
            newValue = value.toString();
        }
        else if (columnName.equalsIgnoreCase(MSTableModel.COLUMN_STATE))
        {
            int val = ((Integer) value).intValue();
            if (val != SubscriptionConstants.STATE_TERMINATED &&
                val != SubscriptionConstants.STATE_ERROR      
                && element.isInterrupted())
                val = SubscriptionConstants.STATE_PAUSED;
            newValue = SubscriptionConstants.STATE_STRING[val];
        }
        
        return newValue;
    }
    
    //---------------------------------------------------------------------
    
}
