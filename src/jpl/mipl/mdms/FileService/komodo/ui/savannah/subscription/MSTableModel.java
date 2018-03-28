package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

/**
 * <b>Purpose:</b>
 *  Table model for Savannah subscription manager components.  Does not
 *  offer any sorting functionality.
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
 * @version $Id: MSTableModel.java,v 1.1 2005/03/29 03:01:41 ntt Exp $
 *
 */
public class MSTableModel extends AbstractTableModel 
{
    private final String __classname = MSTableModel.class.getName();
    
    protected Vector _elements;
    
    public final static String COLUMN_ID        = "Id";
    public final static String COLUMN_TASK      = "Task";
    public final static String COLUMN_FILETYPE  = "Filetype";
    public final static String COLUMN_DIRECTORY = "Directory";
    public final static String COLUMN_STATE     = "State";

    protected String[] _columnNames = new String[] {
                COLUMN_ID, COLUMN_TASK, COLUMN_FILETYPE, 
                COLUMN_DIRECTORY, COLUMN_STATE};
    
    protected Class[] _columnClasses = new Class[] {Integer.class, 
                                                    String.class,
                                                    String.class,
                                                    String.class,
                                                    String.class};
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    
    public MSTableModel()
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
     *  Replace current entries with those from the argument list of 
     *  to model element list 
     *  @param elementList List of new elements to be added
     */
    
    public void replaceAll(java.util.List elementList)
    {
        clear();
        addAll(elementList);
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
     *  Replace current entries with those from the argument list of 
     *  to model element list 
     *  @param elementArray Array of new elements to be added
     */
    
    public void replaceAll(Object[] elementArray)
    {
        List elementList = new ArrayList();
        for (int i = 0; i < elementArray.length; ++i)
            elementList.add(elementArray[i]);
        replaceAll(elementList);
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
     * table model if values are legal, null otherwise.  If column
     * index is set to -1, then the element defining the entire
     * row will be returned.
     * @param rowIndex Index of the row where object resides
     * @param columnIndex Index of the column where object resides, -1
     *                    for object defining entire row.
     * @return Object located at coordinates if found, null otherwise.
     */
    
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        MetaSubscription element = null;
        
        if (rowIndex < 0 || rowIndex >= _elements.size())
        {
            return null;
        }
        
        //retrieve the object defining the row, convenience for this
        //type of table model
        if (columnIndex == -1)
            return _elements.get(rowIndex);
        else if (columnIndex < 0 || columnIndex >= _columnNames.length)
        {
            return null;
        }
        
        element = (MetaSubscription) _elements.get(rowIndex);
        
        String columnName = _columnNames[columnIndex];
        if (columnName != null)
        {
            if (columnName.equalsIgnoreCase(COLUMN_ID))
                return new Integer(element.getId());
            if (columnName.equalsIgnoreCase(COLUMN_TASK))
                return new Integer(element.getTaskType());
            else if (columnName.equalsIgnoreCase(COLUMN_FILETYPE))
                return (String) element.getSource();
            else if (columnName.equalsIgnoreCase(COLUMN_DIRECTORY))
                return (String) element.getTarget();
            else if (columnName.equalsIgnoreCase(COLUMN_STATE))
                return new Integer(element.getState());    
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
    
    /**
     * Returns the row index of subscription with id matching that
     * of the parameter
     * @param id Metasubscription id
     * @return Row index of matching subscription, -1 if not found
     */
    
    public int getRowWithId(int id)
    {
        int row = -1;
        int size = this._elements.size();
        MetaSubscription ms = null;
        for (int i = 0; i < size && row == -1; ++i)
        {
            ms = (MetaSubscription) _elements.get(i);
            if (ms.getId() == id)
                row = i;
        }
        
        return row;
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
}
