package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * <b>Purpose:</b>
 *  Model for recording and maintaining transfer history.
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
 * @version $Id: SavannahTransferModel.java,v 1.6 2004/09/24 00:38:19 ntt Exp $
 *
 */

public class SavannahTransferModel implements PropertyChangeListener
{
    private final String __classname = "SavannahTransferModel";
    
    /** enables bean property change event handling */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(this);
    
    /** Reference to internal list which contains SavannahTransferRecord 
     *  instances */
    protected List _transferList;
    
    //---------------------------------------------------------------------
    
    /**
     *  Constructor.
     */
    
    public SavannahTransferModel()
    {
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes this class.  Called by constructor only. 
     */
    
    protected void init()
    {
        this._transferList = new Vector();
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Adds listener for property change of model.
     *  @param l Object implementing the PropertyChangeListener interface 
     *           to be added
     */
    
    public void addPropertyChangeListener(PropertyChangeListener l)
    {        
        this._changes.addPropertyChangeListener(l);  
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Removes listener for property change of model.
     *  @param l Object implementing the PropertyChangeListener interface
     *           to be removed
     */
    
    public void removePropertyChangeListener(PropertyChangeListener l)
    {     
        this._changes.removePropertyChangeListener(l);   
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     *  Clears all entries from the transfer list. 
     */
    
    public void resetTransferList()
    {
        int numEntries = this._transferList.size();
        if (numEntries > 0)
        {
            for (int i = 0; i < numEntries; ++i)
            {
                ((SavannahTransferRecord)this._transferList.get(i)).
                                 removePropertyChangeListener(this);
            }
            
            this._transferList.clear();
            this._changes.firePropertyChange("TRANSFER_LIST_CHANGED", 
                                             null, null);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns copy of list containing references to the current
     * SavannahTransferRecord instances.
     * @return Copy of transfer list.
     */
    
    public List getTransferList()
    {
        List copy = new Vector();
        int numEntries = this._transferList.size();
        for (int i = 0; i < numEntries; ++i)
        {
            copy.add(this._transferList.get(i));
        }
        return copy;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds the record parameter to internal list for tracking.
     * @param record Instance of SavannahTransferRecord to be added.
     * @return True if the record is added, false otherwise.
     */
    
    public boolean addTransferRecord(SavannahTransferRecord record)
    {
        boolean added = false;
        if (record != null)
        {
            record.addPropertyChangeListener(this);
            this._transferList.add(record);
            added = true;
            this._changes.firePropertyChange("TRANSFER_LIST_CHANGED", 
                                             null, null);
        }
        return added;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Removes the latest occurrance of an entry which returns true
     * for record.equals(entry). If record is null or not found, 
     * then no changes are made.
     * @param record Instance of SavannahTransferRecord to be removed.
     * @return True if the record is found and removed, false otherwise.
     */
    
    public boolean removeTransferRecord(SavannahTransferRecord record)
    {
        boolean removed = false;
        
        if (record != null)
        {
            int numEntries = this._transferList.size();
            Object current = null;

            for (int i = numEntries - 1; i >= 0 && !removed; i--)
            {
                current = this._transferList.get(i);
                if (record.equals(current))
                {
                    this._transferList.remove(i);
                    removed = true;
                }
                
            }
            
            //boolean removed = _transferList.remove(record);
            
            if (removed)
            {
                record.removePropertyChangeListener(this);
                this._changes.firePropertyChange("TRANSFER_LIST_CHANGED", 
                                                 null, null);
            }
        }
        
        return removed;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns latest entry from list that matches the parameters values.
     * @param filename Name of the transferred file
     * @param filetype Name of the filetype involved in transfer
     * @param transcationId The transcation numeric identifier
     * @return Latest matching entry if found, null otherwise.
     */
    
    public SavannahTransferRecord getTransferRecord(String filename, 
                                                    String filetype, 
                                                    int transactionId)
    {
        SavannahTransferRecord record  = null;
        SavannahTransferRecord current = null;
        int numEntries = this._transferList.size();
        boolean cont = true;
        for (int i = numEntries - 1; i >= 0 && cont; --i)
        {
            current = (SavannahTransferRecord) this._transferList.get(i);
            if (current.getTransactionId() == transactionId)
            {
                String curName = current.getFilename();
                String curType = current.getFiletype();
                if ( ((curName == null  && filename == null) ||
                      (filename != null && filename.equals(curName))) 
                                        &&
                     ((curType == null  && filetype == null) ||
                      (filetype != null && filetype.equals(curType))) )
                {
                    record = current;
                    cont = false;
                }
            }
        }
        return record;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns all entries from list that match filename parameter
     * @param filename Name of the transferred file
     * @return Array of all matching entries found, possibly zero length.
     */
    
    public SavannahTransferRecord[] getTransferRecords(String filename)
    {
        SavannahTransferRecord[] records = new SavannahTransferRecord[0];
        SavannahTransferRecord current;
        List matches = new ArrayList();
        
        if (filename != null)
        {
            //extract matches
            int numEntries = this._transferList.size();
            for (int i = 0; i < numEntries; ++i)
            {
                current = (SavannahTransferRecord) this._transferList.get(i);
                String curName = current.getFilename();
                if (filename.equals(curName))
                {
                    matches.add(current);
                }
            }    
            
            //convert list to array 
            numEntries = matches.size();
            records = new SavannahTransferRecord[numEntries];
            for (int i = 0; i < numEntries; ++i)
            {
                records[i] = (SavannahTransferRecord) matches.get(i);
            }
        }
        
        return records;
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
        
        //--------------------
        
        if (propName.equalsIgnoreCase("RECORD_VALUE_CHANGED"))
        {
            this._changes.firePropertyChange(pce);
        }
        
        //--------------------
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
}
