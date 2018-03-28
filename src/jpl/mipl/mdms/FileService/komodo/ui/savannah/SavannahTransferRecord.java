package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * <b>Purpose:</b>
 *  Record class used to keep track of file transfers with Savannah 
 *  application.
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
 * 08/05/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahTransferRecord.java,v 1.13 2005/08/10 00:49:08 ntt Exp $
 *
 */

public class SavannahTransferRecord
{
    private final String __classname = "SavannahTransferRecord";
    
    //----------------------------
    //STATES
    
    /** State: initialized - transfer has not yet begun */
    public static final int STATE_INITIALIZED   = 0;
    
    /** State: Transferring - transfer is in progress */
    public static final int STATE_TRANSFERRING  = 1;
    
    /** State: Complete - transfer complete */
    public static final int STATE_COMPLETE      = 2;
    
    /** State: Abort - user aborted transfer */
    public static final int STATE_ABORTED       = 3;
    
    /** State: Error - transfer unsuccessful due to error */
    public static final int STATE_ERROR         = 4;
    
    //----------------------------
    //TRANSACTION TYPES
    
    /** Transcation type: ADD.  File added to FEI from local filesystem */
    public static final int TRANSACTION_TYPE_ADD     = 0;
    
    /** Transcation type: REPLACE.  File replaced in FEI from local 
     *  filesystem */
    public static final int TRANSACTION_TYPE_REPLACE = 1;
    
    /** Transaction type: GET.  File retrieved from FEI to local filesystem */
    public static final int TRANSACTION_TYPE_GET     = 2;
    
    //----------------------------
    //NULL VALUE TYPES
    
    /** Transaction id unspecified */
    public static final int NULL_TRANSACTION_ID = -1;
    
    /** Time value unspecified */
    public static final int NULL_TIME           = -1;
    
    /** File size unspecified */
    public static final int NULL_SIZE           = -1;
    
    //----------------------------
    
    /** enables bean property change event handling */
    protected PropertyChangeSupport _changes = new PropertyChangeSupport(this);
    
    protected String _filename;
    protected String _filetype;
    protected int    _state;
    protected long   _startTime;
    protected long   _endTime;
    protected int    _transactionId;
    protected long   _fileSize;
    protected int    _transactionType;
    
    //---------------------------------------------------------------------
   
    /** 
     * Constructor.
     * @param filename Name of the file being transferred.
     * @param filetype Filetype involved in transfer.
     * @param id Transaction identifier, can be NULL_TRANSACTION_ID
     * @param size File size, positive or NULL_SIZE.
     * @param type Transaction type.  One of
     *                                 TRANSACTION_TYPE_{GET,ADD,REPLACE}
     */
    
    public SavannahTransferRecord(String filename, String filetype, int id,
                                  long size, int type)
    {
        String errHeader = __classname+"::constructor: ";
        
        //check for legal values of parameters
        if (filename == null || filename.equals(""))
            throw new IllegalArgumentException(errHeader+
                    "Filename cannot be null or empty string");
        if (filetype == null || filetype.equals(""))
            throw new IllegalArgumentException(errHeader+
                    "Filetype cannot be null or empty string");
        if (size != NULL_SIZE && size < 0)
            throw new IllegalArgumentException(errHeader+
                    "Size must be non-negative or NULL_SIZE."+
                    "  Size: "+size);
        if (type != TRANSACTION_TYPE_ADD && 
            type != TRANSACTION_TYPE_GET &&
            type != TRANSACTION_TYPE_REPLACE)
            throw new IllegalArgumentException(errHeader+
                    "Type not a legal value: "+type);
        
        //assign state to data members
        this._filename         = filename;
        this._filetype         = filetype;
        this._transactionId    = id;
        this._fileSize         = size;
        this._transactionType  = type;
        this._state            = STATE_INITIALIZED;
        this._startTime        = -1;
        this._endTime          = -1;
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
     * Returns end time associated with record, NULL_TIME if not set.
     * @return Returns the end time.
     */
    
    public long getEndTime()
    {
        return this._endTime;
    }    

    //---------------------------------------------------------------------

    /**
     * Sets end time associated with record.
     * @param time The end time to set.
     */
    
    public void setEndTime(long time)
    {
        if ( time != this._endTime)
        {
            this._endTime = time;
            this._changes.firePropertyChange("RECORD_VALUE_CHANGED", 
                                             null, this);
        }
    }   

    //---------------------------------------------------------------------

    /**
     * Returns filename associated with transfer.
     * @return Returns the filename.
     */
    
    public String getFilename()
    {
        return this._filename;
    }   

    //---------------------------------------------------------------------

    /**
     * Sets the transfer filename.
     * @param filename The filename to set.
     */
    
    protected void setFilename(String filename)
    {
        if (filename == null)
            return;
        
        if (!filename.equalsIgnoreCase(this._filename))
        {
            this._filename = filename;
            this._changes.firePropertyChange("RECORD_VALUE_CHANGED", 
                                             null, this);
        }
    }   

    //---------------------------------------------------------------------

    /**
     * Returns the filetype name associated with transfer
     * @return Returns the filetype.
     */
    
    public String getFiletype()
    {
        return this._filetype;
    }   

    //---------------------------------------------------------------------

    /**
     * Sets the filetype name associated with transfer
     * @param filetype The filetype to set.
     */
    
    protected void setFiletype(String filetype)
    {
        if (filetype == null)
            return;
        
        if (!filetype.equalsIgnoreCase(this._filetype))
        {
            this._filetype = filetype;
            this._changes.firePropertyChange("RECORD_VALUE_CHANGED", 
                                             null, this);
        }
    }   

    //---------------------------------------------------------------------

    /**
     * Returns the start time of transfer, possibly NULL_TIME if not set.
     * @return Returns the startTime.
     */
    
    public long getStartTime()
    {
        return this._startTime;
    }   

    //---------------------------------------------------------------------

    /**
     * Sets the start time of transfer
     * @param time The startTime to set.
     */
    
    public void setStartTime(long time)
    {
        if (time != this._startTime)
        {
            this._startTime = time;
            this._changes.firePropertyChange("RECORD_VALUE_CHANGED", 
                                             null, this);
        }
    }   

    //---------------------------------------------------------------------

    /**
     * Returns the state of the transfer
     * @return Returns the state.
     */
    
    public int getState()
    {
        return this._state;
    }   

    //---------------------------------------------------------------------

    /**
     * Sets the state of the transfer.
     * @param state The state to set.
     */
    
    public void setState(int state)
    {
        if (state != _state)
        {
            String stateString = SavannahTransferRecord.stateAsString(state);
            if ( stateString == null || 
                                    stateString.equalsIgnoreCase("Undefined"))
            {
                throw new IllegalArgumentException(__classname+"::setState()"+
                                        ": Unrecognized state value: "+state); 
            }
        
            this._state = state;
            this._changes.firePropertyChange("RECORD_VALUE_CHANGED", 
                                             null, this);
        }
    }   

    //---------------------------------------------------------------------

    /**
     * Returns the transaction id of the transfer.
     * @return Returns the transactionId.
     */
    
    public int getTransactionId()
    {
        return this._transactionId;
    }   

    //---------------------------------------------------------------------

    /**
     * Sets the transaction id associated with the transfer.
     * @param id The transactionId to set.
     */
    
    protected void setTransactionId(int id)
    {
        if (id != this._transactionId)
        {
            _transactionId = id;
            this._changes.firePropertyChange("RECORD_VALUE_CHANGED", 
                                             null, this);
        }
    }   

    //---------------------------------------------------------------------

    /**
     * Returns the transfer transaction type, one of 
     * TRANSACTION_TYPE_{ADD|REPLACE|GET}.
     * @return Returns the transaction type
     */
    
    public int getTransactionType()
    {
        return this._transactionType;
    }   
    
    //---------------------------------------------------------------------

    /**
     * Returns the filesize of file being transferred, NULL_SIZE if not set.
     * @return Returns the file size.
     */
    
    public long getFileSize()
    {
        return this._fileSize;
    }   
    

    
    //---------------------------------------------------------------------

    /** 
     * Implements an equivalence relation  on non-null object references
     * @param object  the reference object with which to compare.
     * @return True if this object is the same as the object argument; 
     *         false otherwis
     */
    
    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        
        if (!(object instanceof SavannahTransferRecord))
        {
            return super.equals(object);
        }
        
        SavannahTransferRecord other = (SavannahTransferRecord) object;
        
        if (this._transactionId == other.getTransactionId() &&
            this._filename.equals(other.getFilename()) &&
            this._filetype.equals(other.getFiletype()))
            return true;
        
        return false;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns a hash code value for the object.  Currently
     *  transactionid, filename, and filetype are used as part of the
     *  hash value calculation.
     *  @return Hash value for this object
     */
    
    public int hashCode()
    {
        int hash = this._transactionId;
        hash = hash * 31 + this._filename.hashCode();
        hash = hash * 31 + this._filetype.hashCode();    
        
        return hash;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns the elapsed time or other status as a string.  If state
     *  is STATE_ERROR or STATE_ABORTED, "Cancelled" is returned.  If
     *  start time is NULL_TIME, "Pending", else if end time is NULL_TIME,
     *  returns "In progress".  Otherwise, the elapsed time is returned
     *  as xxx ms.
     *  @return Elapsed time status
     */
    
    public String getTransferTimeString()
    {
        if (this._state == STATE_ABORTED || this._state == STATE_ERROR)
            return "Cancelled";
        
        if (this._startTime == NULL_TIME)
            return "Pending";
        else if (this._endTime == NULL_TIME)
            return "In progress";
        else if (this._startTime < this._endTime)
        {
            long diff = this._endTime - this._startTime;
            //diff /= 1000;
            return diff+" ms.";
        }
        else
            return "Error";
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns string format of file size.  Can return 'Unspecified',
     * 'Error', or 'xxx bytes' depending on whether file size value
     * is defined.
     * @return String value of file size
     */
    
    public String getFileSizeString()
    {
        String sizeStr;
        
        if (_fileSize == NULL_SIZE)
            sizeStr = "Unspecified";
        else if (_fileSize < 0)        
            sizeStr = "Error";
        else
        {
                /*
                double size = (double) _fileSize;
                double KILO = 1000.0;
                String[] units = new String[] {"bytes", "KB", "MB", "GB", "TB"};
                boolean cont = true;
                int unitIndex = 0;
                for (int i = 0; cont && i < units.length; ++i)
                {
                    if (size > KILO)
                    {
                        unitIndex++;
                        size /= KILO;
                    }
                    else
                        cont = false;
                }
                
                _fileSizeString = size + " " + units[unitIndex];
                */
            sizeStr = _fileSize + " bytes";
        }
        
        return sizeStr;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Static method that returns the string name of the type parameter
     * for printing.
     * @param type One of TRANSACTION_TYPE_{ADD,GET,REPLACE}
     * @return String representation of type, 'Undefined' if type
     *         unrecognized.
     */
    
    public static String transactionTypeAsString(int type)
    {
        if (type == TRANSACTION_TYPE_ADD)
            return "ADD";
        else if (type == TRANSACTION_TYPE_REPLACE)
            return "REPLACE";
        else if (type == TRANSACTION_TYPE_GET)
            return "GET";
        else
            return "Undefined";
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Static method that returns the string name of the state parameter
     * for printing.
     * @param type One of STATE_{INITIALIZED,TRANSFERRING,COMPLETE,ERROR,
     *              ABORTED}
     * @return String representation of state, 'Undefined' if type
     *         unrecognized.
     */
    
    public static String stateAsString(int state)
    {
        if (state == STATE_INITIALIZED)
            return "Initialized";
        else if (state == STATE_TRANSFERRING)
            return "Transferring...";
        else if (state == STATE_COMPLETE)
            return "Complete";
        else if (state == STATE_ABORTED)
            return "Aborted";
        else if (state == STATE_ERROR)
            return "Error";
        else
            return "Undefined"; 
    }

    //---------------------------------------------------------------------
    
    public void setFileSize(long size)
    {
        if (size < 0 && size != NULL_SIZE)
            size = NULL_SIZE;
        if (size != this._fileSize)
        {
            this._fileSize = size;
            this._changes.firePropertyChange("RECORD_VALUE_CHANGED", 
                                             null, this);
        }
    }
    
    //---------------------------------------------------------------------
}
