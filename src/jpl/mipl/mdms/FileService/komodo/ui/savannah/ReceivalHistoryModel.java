/*
 * Created on Sep 28, 2004
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.util.HashSet;
import java.util.Set;

/**
 * <b>Purpose:</b>
 * Class encapsulates structures used to maintain transfer history
 * entries.
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
 * 09/28/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ReceivalHistoryModel.java,v 1.1 2004/09/28 23:19:25 ntt Exp $
 *
 */

public class ReceivalHistoryModel
{
    protected Set _feiSet;
    protected Set _fileSet;
    protected SavannahModel _model;
    
    //---------------------------------------------------------------------
    
    public ReceivalHistoryModel(SavannahModel model)
    {
        this._model = model;
        this._feiSet  = new HashSet();
        this._fileSet = new HashSet();
    }
    
    //---------------------------------------------------------------------
    
    public void resetAll()
    {
        resetFei();
        resetLocal();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a new entry to the FEI file set.
     * @param entry New entry to be added.
     */
    
    public void addToFei(String entry)
    {
        _feiSet.add(entry);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a new entry to the local file set.
     * @param entry New entry to be added.
     */
    
    public void addToLocal(String entry)
    {
        _fileSet.add(entry);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Resets the FEI file set.
     */
    
    public void resetFei()
    {
        this._feiSet.clear();
        this._model.requestRefresh(SavannahModel.TARGET_FEI);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Resets the local file set.
     */
    
    public void resetLocal()
    {
        this._fileSet.clear();
        this._model.requestRefresh(SavannahModel.TARGET_LOCAL);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Method returns a boolean value corresponding to the presence
     * of the entry parameter in the FEI receival history list.
     * @param entry Entry of form "/{filetype}/{filename}"
     * @return True if entry was found in history list, false otherwise
     */
    
    public boolean inFeiHistory(String entry)
    {
        if (entry == null)
            return false;
        
        return _feiSet.contains(entry);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Method returns a boolean value corresponding to the presence
     * of the entry parameter in the local receival history list.
     * @param entry Full path of file 
     * @return True if entry was found in history list, false otherwise
     */
    
    public boolean inLocalHistory(String entry)
    {
        if (entry == null)
            return false;
        
        return _fileSet.contains(entry);
    }
    
    //---------------------------------------------------------------------
    
}
