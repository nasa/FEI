
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

/**
 * <b>Purpose:</b>
 *  Default table model for Savannah JTable components.  Does not
 *  offer any sorting functionality.
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
 * @version $Id: DefaultSavannahTableModel.java,v 1.1 2004/09/16 19:50:09 ntt Exp $
 *
 */
public class DefaultSavannahTableModel extends AbstractTableModel 
                                                implements SavannahTableModel
{
    private final String __classname = "DefaultSavannahTableModel";
    
    protected Vector _elements;
    
    protected String[] _columnNames = new String[] {"Type", 
                                                    "Name", 
                                                    "Size", 
                                                    "Modified"};
    
    protected Class[] _columnClasses = new Class[] {Integer.class, 
                                                    String.class, 
                                                    Long.class, 
                                                    Long.class};
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    
    public DefaultSavannahTableModel()
    {
        _elements = new Vector();
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     *  Removes all elements from the model list. 
     */
    
    public void clear()
    {
        int size = this._elements.size();
       
        if (size > 0)
        {
            this._elements.clear();
            this.fireTableRowsDeleted(0, size-1);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Appends new element to end of element list 
     *  @param element New element to be added
     */
    
    public void addElement(Object element)
    {
        _elements.add(element);
        int index = _elements.size() - 1;
        this.fireTableRowsInserted(index, index);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Appends argument list of elements to model element list 
     *  @param elementList List of new elements to be added
     */
    
    public void addAll(java.util.List elementList)
    {
        int startIndex = _elements.size();
        _elements.addAll(elementList);
        int endIndex   = _elements.size() - 1;
        this.fireTableRowsInserted(startIndex, endIndex);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Appends elements from argument array to model element list 
     *  @param elementArray Array of elements to be added
     */
    
    public void addAll(Object[] elementArray)
    {
        List elementList = new ArrayList();
        for (int i = 0; i < elementArray.length; ++i)
            elementList.add(elementArray[i]);
        addAll(elementList);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the element located at the argument index of internal list
     *  @param index Index of the element to be retrieved
     *  @return Object found at index or null if not found  
     */
    
    public Object elementAt(int index)
    {
        if (index < 0 || index > _elements.size())
            return null;
        else
            return _elements.get(index);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the index of element argument if found in internal
     *  element list, or -1 if not found.
     *  @param element Element whose index is to be returned
     *  @return Index of element if found, -1 otherwise. 
     */
    
    public int indexOf(Object element)
    {
        return _elements.indexOf(element);
    }
    
    //---------------------------------------------------------------------
    

    /**
     *  Returns the number of rows (elements) in the model list
     *  @return Number of rows 
     */
    
    public int getRowCount()
    {
        return _elements.size();
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the class associated with the column at a specified 
     *  index.
     *  @param columnIndex Index of column whose class is to be returned
     *  @return Class associated with column specified by columnIndex
     *  @throws IllegalArgumentException if columnIndex is negative or 
     *          greater than the column count
     */
    
    public Class getColumnClass(int columnIndex)
    {
        if (columnIndex < 0 || columnIndex > _columnClasses.length)
            throw new IllegalArgumentException(__classname+
                    "::getColumnClass():: Invalid index: "+columnIndex);
        
        return _columnClasses[columnIndex];
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the index of column associated with the String identifier
     * @param columnName Identifier associated with column
     * @return Index of column matching identifier, -1 if not found.
     */
    
    public int findColumn(String columnName)
    {
        int index = -1;
        for (int i = 0; i < _columnNames.length && index == -1; ++i)
        {
            if (_columnNames[i].equalsIgnoreCase(columnName))
                index = i;
        }
        return index;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the column name associated with the index parameter
     * @param columnIndex Index of the column of interest
     * @return Name of the column at columnIndex, null if not found.
     */
    
    public String getColumnName(int columnIndex)
    {
        if (columnIndex >= 0 && columnIndex < _columnNames.length)
            return _columnNames[columnIndex];
        else
            return null;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the number of columns in the model
     * @return Column count.
     */
    
    public int getColumnCount()
    {
        return _columnNames.length;
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns value located at (rowIndex, columnIndex) within the
     * table model if values are legal, null otherwise.
     * @param rowIndex Index of the row where object resides
     * @param columnIndex Index of the column where object resides
     * @return Object located at coordinates if found, null otherwise.
     */
    
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        SavannahTableElement element = null;
        
        if (rowIndex < 0 || rowIndex >= _elements.size())
        {
            return null;
        }
        if (columnIndex < 0 || columnIndex >= _columnNames.length)
        {
            return null;
        }
        
        element = (SavannahTableElement) _elements.get(rowIndex);
        
        String columnName = _columnNames[columnIndex];
        if (columnName != null)
        {
            if (columnName.equalsIgnoreCase("Type"))
                return new Integer(element.getType());
            else if (columnName.equalsIgnoreCase("Name"))
                return element.getName();
            else if (columnName.equalsIgnoreCase("Size"))
                return new Long(element.getSize());
            else if (columnName.equalsIgnoreCase("Modified"))
                return new Long(element.getModificationDate());    
        }
        
        return null;
    }

    //---------------------------------------------------------------------
    
    /**
     * Alerts listeners to refresh themselves due to change in model. 
     */
    
    public void refresh()
    {
        this.fireTableDataChanged();
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
}
