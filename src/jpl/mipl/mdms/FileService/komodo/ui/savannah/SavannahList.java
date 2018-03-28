package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import javax.swing.event.ListSelectionListener;

/**
 *
 * Interface defining behavior of Savannah list components for
 * DnD behavior.
 *  
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahList.java,v 1.5 2004/08/20 01:02:30 ntt Exp $
 */

public interface SavannahList 
{
    //---------------------------------------------------------------------
    
    /**
     *  Returns whether component can accept object for import.
     *  @param obj Object to be imported.
     *  @return True if object can be imported, false otherwise.
     *  @throws SavannahListException if obj cannot be import with
     *          explanation as part of exception's getMessage string
     */    
    
    public boolean canImport(Object obj) throws SavannahListException;
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns whether component can export object for transfer.
     *  @param obj Object to be exported.
     *  @return True if object can be exported, false otherwise.
     *  @throws SavannahListException if obj cannot be import with
     *          explanation as part of exception's getMessage string
     */    
    
    public boolean canExport(Object obj) throws SavannahListException;
    
    //---------------------------------------------------------------------
    
    /**
     *  Intructs instance to refresh its listing.
     */
    
    public void refresh();
    
    //---------------------------------------------------------------------
    
    /**
     *  Instructs instance to import object.
     *  @param obj Object to be imported.
     *  @return True if object was imported, false otherwise.
     */ 
    
    public boolean importEntry(Object obj);

    //---------------------------------------------------------------------
    
    /**
     *  Returns array of selected values within list.
     *  @return Obejct array of values selected in list
     */ 
    
    public Object[] getSelectedValues();
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the String representation of instance.
     *  @return String representation of this instance
     */ 
    
    public String toString();
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a listener to the list that's notified each time a change 
     * to the selection occurs.
     * @param listener the ListSelectionListener to add
     */
       
     public void addListSelectionListener(ListSelectionListener listener);    
       
     //---------------------------------------------------------------------
       
     /**
      * Removes a listener from the list that's notified each time a change 
      * to the selection occurs. 
      * @param listener the ListSelectionListener to remove
      */
       
     public void removeListSelectionListener(ListSelectionListener listener);
     
     //---------------------------------------------------------------------
     
     /**
      * Method determines whether an action is legal in current state.
      * @param actionId The id of the action whose legal execution 
      *                 is in question
      * @param values Object array containing selected contents of list
      */
     
     public boolean canPerformAction(int actionId, Object[] values);
     
     //---------------------------------------------------------------------
     
}

