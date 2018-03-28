package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.net.URL;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.table.TableCellRenderer;

import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.PrintfFormat;

/**
 * <b>Purpose:</b>
 *  Cell rendered for the Savannah application table using a 
 *  SavannahTabelModel instance as table model.  Renders
 *  cells of the table for Type, Name, Size, and Modified
 *  attributes.
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
 * 09/08/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahTableCellRenderer.java,v 1.7 2009/05/19 19:08:34 ntt Exp $
 *
 */

public class SavannahTableCellRenderer extends JLabel 
                                            implements TableCellRenderer
{
    public static final String PARENT_FOLDER_STRING = "..";
    protected static final int IMAGE_DIMENSION = 16;
    
    protected Icon _folderIcon               = null;
    protected Icon _fileIcon                 = null;
    protected Icon _filetypeIcon             = null;
    protected Icon _filetypeOutIcon          = null;
    protected Color _highlightColor          = Color.blue;
    protected SavannahTableModel _tableModel = null;
    protected SavannahModel _model           = null;
    protected String[] _units = new String[] {"B","K","M","G","T"};
    protected PrintfFormat _printfDecimalFormat;
    protected String _printfDecimalControl   = "%.1f";
    
    //---------------------------------------------------------------------
    
    public SavannahTableCellRenderer(SavannahModel model,
                                     SavannahTableModel tableModel)
    {
        this._model      = model;
        this._tableModel = tableModel;
        this._printfDecimalFormat = new PrintfFormat(_printfDecimalControl);
        this.setOpaque(true);
        this.setBorder(null);

        /*
        URL imageURL = FileListToolbar.class.getResource("resources/folder24.gif");            
        if (imageURL != null)
            folderIcon = new ImageIcon(imageURL);
        else
        */
        this._folderIcon = new MetalIconFactory.FolderIcon16();
        
        /*
        imageURL = FileListToolbar.class.getResource("resources/file24.gif");            
        if (imageURL != null)
            fileIcon = new ImageIcon(imageURL);
        else
        */
        this._fileIcon = new MetalIconFactory.FileIcon16();
        
        //filetype icon
        URL imageURL = SavannahTableCellRenderer.class.getResource(
                                       "resources/filetype24.gif");            
        if (imageURL != null)
        {
            ImageIcon tempIcon = new ImageIcon(imageURL);
            tempIcon.setImage(tempIcon.getImage().getScaledInstance(
                    IMAGE_DIMENSION, IMAGE_DIMENSION, Image.SCALE_SMOOTH));
            this._filetypeIcon = tempIcon;

        }
        else
            this._filetypeIcon = this._folderIcon;
        
        //Filetype up icon (leave filetype)
        imageURL = SavannahTableCellRenderer.class.getResource(
                                    "resources/ftypeup24.gif");            

        if (imageURL != null)
        {
            ImageIcon tempIcon = new ImageIcon(imageURL);
            tempIcon.setImage(tempIcon.getImage().getScaledInstance(
                    IMAGE_DIMENSION, IMAGE_DIMENSION, Image.SCALE_SMOOTH));
            this._filetypeOutIcon = tempIcon;

        }
        else
            this._filetypeOutIcon = this._folderIcon;

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
        SavannahTableElement element = (SavannahTableElement) 
                                       this._tableModel.elementAt(row);
        
        //handle label contents
        String columnName = table.getColumnName(column);
        
        if (columnName.equalsIgnoreCase("Type"))
        {
            Icon icon = null;
            int type = ((Integer) value).intValue();
            if (type == SavannahTableElement.TYPE_FILE)
                icon = _fileIcon;
            else if (type == SavannahTableElement.TYPE_FOLDER)
            {
                //how to hack code:
                if (element instanceof SavannahFeiTableElement)
                {
                    if (element.isParent())
                        icon = _filetypeOutIcon;
                    else
                        icon = _filetypeIcon;
                }
                else
                    icon = _folderIcon;
            }
            this.setText(null);
            this.setIcon(icon);
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }
        else if (columnName.equalsIgnoreCase("Name"))
        {
            String text = null;
            if (element.isParent())
                text = PARENT_FOLDER_STRING;
            else
                text = value.toString();
            this.setIcon(null);
            this.setText(text);
            this.setHorizontalAlignment(SwingConstants.LEFT);
        }
        else if (columnName.equalsIgnoreCase("Size"))
        {
            String text = null;
            long fileSize = ((Long) value).longValue();
            
            if (fileSize == SavannahTableElement.SIZE_UNKNOWN)
                text = null;
            else
            {
                double fileSizeD = (double) fileSize;
                int index = 0;
                boolean cont = true;
                while (cont)
                {
                    if (index == _units.length - 1 || fileSizeD <= 1000.0)
                        cont = false;
                    else
                    {
                        fileSizeD = fileSizeD / 1000.0;
                        index++;
                    }
                }
                
                if (index == 0)
                    text = fileSize+" "+_units[index]+"  ";
                else
                {
                    Object[] objArray = new Object[] {
                                            new Double(fileSizeD)};
                    String val = _printfDecimalFormat.sprintf(objArray);
                    text = val+" "+_units[index]+"  ";
                }
            }
            this.setIcon(null);
            this.setText(text);
            this.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        else if (columnName.equalsIgnoreCase("Modified"))
        {
            long modDate = ((Long) value).longValue();
            String modDateStr;
            
            if (modDate == SavannahTableElement.DATE_UNKNOWN)
                modDateStr = "";
            else
            {
                Date d = new Date(modDate);
                    
                //modDateStr = DateTimeUtil.getDateCCSDSAString(d);
                
                //TODO - this will be the proper way to do it (ntt: 051209)
                modDateStr = this._model.getDateTimeFormatter().formatDate(d);
            }
            this.setIcon(null);
            this.setText(modDateStr);
            this.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        else
        {
            this.setIcon(null);
            this.setText("Undefined");
        }

        
        //handle bolding directories
        if (element.getType() == SavannahTableElement.TYPE_FOLDER)
            this.setFont(table.getFont().deriveFont(Font.BOLD));
        else
            this.setFont(table.getFont().deriveFont(Font.PLAIN));

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
            
            if (element instanceof SavannahFileTableElement)
            {
                if (_model.getReceivalModel().inLocalHistory(element.getPath()))
                    this.setForeground(_highlightColor);
            }
            else if (element instanceof SavannahFeiTableElement)
            {
                if (_model.getReceivalModel().inFeiHistory(element.getPath()))
                    this.setForeground(_highlightColor);
            }      
        }

        //---------------------         

        return this;
    }         
    
    //---------------------------------------------------------------------
    
}
