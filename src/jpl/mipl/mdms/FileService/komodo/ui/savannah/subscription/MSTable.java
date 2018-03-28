package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

/**
 * <b>Purpose:</b>
 *  Subclass of JTable for use with Savannah subscrptions.  Displays
 *  the id, task type, filetype, dir, and state of elements from
 *  the model.
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
 * 03/25/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MSTable.java,v 1.3 2005/04/02 02:37:06 ntt Exp $
 *
 */

public class MSTable extends JTable
{
    protected MetaSubscriptionManager  _manager;
    protected MSTableModel             _tableModel;
    protected MSTableCellRenderer      _renderer;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param appModel Instance of the SavannahModel application model
     * @param tableModel Instance of SavannahTableModel for table
     */
    
    public MSTable(MetaSubscriptionManager manager, MSTableModel tableModel)
    {
        super(tableModel);
        this._manager    = manager;
        this._tableModel = tableModel;
        
        if (this._tableModel instanceof MSTableModel)
        {
            //((SortableMSTableModel) this._tableModel).setTableHeader(
            //                                this.getTableHeader());
        }
        
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        //set renderer to use Savannah specific renderer
        this._renderer = new MSTableCellRenderer(_manager, _tableModel);
        this.setDefaultRenderer(String.class,_renderer);
        this.setDefaultRenderer(Number.class,_renderer);

        //set the selection mode
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        //set column ordering rigid
        this.getTableHeader().setReorderingAllowed(false);
        //((DefaultTableCellRenderer) this.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        //get rid of margin and grid lines
        this.setShowGrid(false);
        this.setShowHorizontalLines(true);
        this.getColumnModel().setColumnMargin(0);
        
        //remove border
        this.setBorder(null);
        
        //set column widths
        TableColumn column = this.getColumn(MSTableModel.COLUMN_ID);
        column.setPreferredWidth(30);
        column.setMinWidth(15);
        column.setMaxWidth(40);
        
        column = this.getColumn(MSTableModel.COLUMN_TASK);
        column.setMinWidth(20);
        column.setPreferredWidth(40);
        
        column = this.getColumn(MSTableModel.COLUMN_FILETYPE);
        column.setPreferredWidth(60);
        column.setMinWidth(40);
        
        column = this.getColumn(MSTableModel.COLUMN_DIRECTORY);
        column.setPreferredWidth(90);
        column.setMinWidth(40);
        
        column = this.getColumn(MSTableModel.COLUMN_STATE);
        column.setPreferredWidth(40);
        //column.setMaxWidth(160);
        column.setMinWidth(20);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the string to be used as the tooltip for event. Currently
     * the method getPath() is called on the SavannahTableElement 
     * associated with point of mouse event me.
     * @param me Mouse event associated with cursor location
     * @return Path of savannah element at location.
     */
    
    public String getToolTipText(MouseEvent me)
    {
        String tip   = null;
        Point point  = me.getPoint();
        int row      = this.rowAtPoint(point);
        int column   = this.columnAtPoint(point);
        MetaSubscription element = (MetaSubscription) 
                                   this._tableModel.elementAt(row);
        Object value = this.getValueAt(row, column);
        String columnName = this.getColumnName(column);

        if (element != null && columnName != null && value != null)
        {
            //Object history = element.getHistory();
            
            //echo the value of the cell (which also uses translate())
            tip = MSTableCellRenderer.translate(columnName, value, element);        
        }
        
        return tip;
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
}
