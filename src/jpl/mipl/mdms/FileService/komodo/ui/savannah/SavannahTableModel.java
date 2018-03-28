
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import javax.swing.table.TableModel;

/**
 * <b>Purpose:</b>
 *  Interface for table model for Savannah JTable components.
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
 * 09/16/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahTableModel.java,v 1.3 2004/09/16 19:50:10 ntt Exp $
 *
 */
public interface SavannahTableModel extends TableModel
{   
    /**
     *  Removes all elements from the model list. 
     */
    
    public void clear();
    
    //---------------------------------------------------------------------
    
    /**
     *  Appends new element to end of element list 
     *  @param element New element to be added
     */
    
    public void addElement(Object element);
    
    //---------------------------------------------------------------------
    
    /**
     *  Appends argument list of elements to model element list 
     *  @param elementList List of new elements to be added
     */
    
    public void addAll(java.util.List elementList);
    
    //---------------------------------------------------------------------
    
    /**
     *  Appends elements from argument array to model element list 
     *  @param elementArray Array of elements to be added
     */
    
    public void addAll(Object[] elementArray);
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the element located at the argument index of internal list
     *  @param index Index of the element to be retrieved
     *  @return Object found at index or null if not found  
     */
    
    public Object elementAt(int index);
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the index of element argument if found in internal
     *  element list, or -1 if not found.
     *  @param element Element whose index is to be returned
     *  @return Index of element if found, -1 otherwise. 
     */
    
    public int indexOf(Object element);
    
    //---------------------------------------------------------------------
    

    /**
     *  Returns the number of rows (elements) in the model list
     *  @return Number of rows 
     */
    
    public int getRowCount();
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the class associated with the column at a specified 
     *  index.
     *  @param columnIndex Index of column whose class is to be returned
     *  @return Class associated with column specified by columnIndex
     *  @throws IllegalArgumentException if columnIndex is negative or 
     *          greater than the column count
     */
    
    public Class getColumnClass(int columnIndex);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the index of column associated with the String identifier
     * @param columnName Identifier associated with column
     * @return Index of column matching identifier, -1 if not found.
     */
    
    public int findColumn(String columnName);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the column name associated with the index parameter
     * @param columnIndex Index of the column of interest
     * @return Name of the column at columnIndex, null if not found.
     */
    
    public String getColumnName(int columnIndex);
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the number of columns in the model
     * @return Column count.
     */
    
    public int getColumnCount();
    
    //---------------------------------------------------------------------

    /**
     * Returns value located at (rowIndex, columnIndex) within the
     * table model if values are legal, null otherwise.
     * @param rowIndex Index of the row where object resides
     * @param columnIndex Index of the column where object resides
     * @return Object located at coordinates if found, null otherwise.
     */
    
    public Object getValueAt(int rowIndex, int columnIndex);

    //---------------------------------------------------------------------
    
    /**
     * Alerts listeners to refresh themselves due to change in model. 
     */
    
    public void refresh();
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
}
