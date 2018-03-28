package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

/**
 * <b>Purpose:</b>
 *  Subclass of JTable for use with Savannah application.  Displays
 *  the type, name, size, and modification date of elements from
 *  the model.
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
 * 09/10/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahTable.java,v 1.8 2004/09/28 23:19:25 ntt Exp $
 *
 */

public class SavannahTable extends JTable
{
    protected SavannahModel              _appModel;
    protected SavannahTableModel         _tableModel;
    protected SavannahTableCellRenderer  _renderer;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param appModel Instance of the SavannahModel application model
     * @param tableModel Instance of SavannahTableModel for table
     */
    
    public SavannahTable(SavannahModel appModel, SavannahTableModel tableModel)
    {
        super(tableModel);
        this._appModel   = appModel;
        this._tableModel = tableModel;
        
        if (this._tableModel instanceof SortableSavannahTableModel)
        {
            ((SortableSavannahTableModel) this._tableModel).setTableHeader(
                                                    this.getTableHeader());
        }
        
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        //set renderer to use Savannah specific renderer
        this._renderer = new SavannahTableCellRenderer(_appModel, _tableModel);
        this.setDefaultRenderer(String.class,_renderer);
        this.setDefaultRenderer(Number.class,_renderer);

        //set the selection mode
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        //set column ordering rigid
        this.getTableHeader().setReorderingAllowed(false);

        //get rid of margin and grid lines
        this.setShowGrid(false);
        this.getColumnModel().setColumnMargin(0);
        
        //remove border
        this.setBorder(null);
        
        //set column widths
        TableColumn column = this.getColumn("Type");
        column.setPreferredWidth(30);
        column.setMinWidth(30);
        column.setMaxWidth(45);
        
        column = this.getColumn("Name");
        column.setMinWidth(20);
        
        column = this.getColumn("Size");
        column.setPreferredWidth(55);
        column.setMinWidth(40);
        column.setMaxWidth(70);
        
        column = this.getColumn("Modified");
        column.setPreferredWidth(70);
        column.setMinWidth(40);
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
        int rowIndex = this.rowAtPoint(point);
        
        SavannahTableElement element = (SavannahTableElement) 
                                        this._tableModel.elementAt(rowIndex);
        
        if (element != null)
            tip = element.getPath();
        
        return tip;
    }
    
    //---------------------------------------------------------------------
}
