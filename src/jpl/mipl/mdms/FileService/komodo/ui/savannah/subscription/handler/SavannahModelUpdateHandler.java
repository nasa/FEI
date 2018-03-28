/*
 * Created on Feb 18, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler;

import java.awt.Component;
import java.io.File;
import java.util.List;

import javax.swing.SwingUtilities;

import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.ReceivalHistoryModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahTransferModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahTransferRecord;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.HandlerInfo;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.PluginLoader;


/**
 * <b>Purpose:</b>
 * Handler that updates Savannah tranfer model and receival model
 * with files that were retrieved during a subscription.
 * Special attributes are:
 * <pre>
 *  (1) updateTransferModel - "true" if transfer model should be updated with new
 *                            file transfer, "false" otherwise, default true
 *  (2) updateReceivalModel - "true" if receival model should be updated with new
 *                            file transfer, "false" otherwise, default true
 * </pre>
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
 * 02/18/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahModelUpdateHandler.java,v 1.4 2005/03/29 03:01:52 ntt Exp $
 *
 */

public class SavannahModelUpdateHandler extends AbstractSubscriptionHandler
{
    private final String  __classname  = this.getClass().getName();
    
    /** Name of the updateTransferModel property */
    public final static String PROPERTY_TRANSFER = "updateTransferModel";
    
    /** Name of the updateReceivalModel property */
    public final static String PROPERTY_RECEIVAL = "updateReceivalModel";
    
    protected boolean                _syncWithModel = false;
    protected SavannahTransferModel  _transferModel = null;
    protected ReceivalHistoryModel   _receivalModel = null;
    protected Component _relativeComponent          = null;
    
    protected boolean _updateTransferModel = true;
    protected boolean _updateReceivalModel = true;

    
    //---------------------------------------------------------------------
    
    /**
     * Initializes this handler from handler info object
     * @param hInfo Handler info object used to initialize this handler
     */
    
    public void initialize(HandlerInfo hInfo) throws Exception
    {   
        super.initialize(hInfo);
        
        //check for update of transfer model (not required)
        String value = hInfo.getProperty(PROPERTY_TRANSFER);
        this._updateTransferModel = PluginLoader.getBooleanValue(value, true);
        
        //check for update of receival model (not required)
        value = hInfo.getProperty(PROPERTY_RECEIVAL);
        this._updateReceivalModel = PluginLoader.getBooleanValue(value, true);
    }

    //---------------------------------------------------------------------

    
    /**
     * Handle method performs operation of the handler.
     * @param taskType Type of task, one of 
     *            SubscriptionConstants.TASK_{SUBSCRIPTION,NOTIFICATION}
     * @param results List of Result objects 
     */
    
    public void handleEvent(int taskType, List results)
    {
        if (_syncWithModel)
            updateModel(results);
    }
    
    //---------------------------------------------------------------------
    
    protected void updateModel(List results)
    {
        Result result = null;
        
        int numResults = results.size();
        
        //iterate, create records and send them to the model
        for (int i = 0; i < numResults; ++i)
        {
            result = (Result) results.get(i);
            final File localDir = new File(_client.getDir());
            final String filename = result.getName();
            final SavannahTransferRecord record = 
                new SavannahTransferRecord(filename, 
                     result.getType(), result.getTransactionId(), 
                     result.getSize(), SavannahTransferRecord.
                     TRANSACTION_TYPE_GET);
            
            record.setState(SavannahTransferRecord.STATE_COMPLETE);
            record.setStartTime(System.currentTimeMillis());
            record.setEndTime(record.getStartTime());
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    if (_updateTransferModel)
                    {
                        _transferModel.addTransferRecord(record);
                    }
                    
                    if (_updateReceivalModel)
                    {
                        _receivalModel.addToLocal(
                                new File(localDir, filename).
                                getAbsolutePath());
                        
                        //if user viewer the recv'ing dir, then refresh it
                        if (localDir.equals(_model.getLocalDirectory()))
                        {
                            _model.requestRefresh(SavannahModel.TARGET_LOCAL);
                            _model.setStatusMessage("New file available in "+
                                                    "local directory");
                        }
                    }
                }
            });
        
        }
    }
    
    //---------------------------------------------------------------------
    
    public void setAppModel(SavannahModel model)
    {
        super.setAppModel(model);
        if (this._model == null)
        {
            this._relativeComponent = null;
            this._transferModel     = null;
            this._receivalModel     = null;
            this._syncWithModel     = false;
        }
        else
        {
            this._relativeComponent = this._model.getRelativeComponent();
            this._transferModel     = this._model.getTransferModel();
            this._receivalModel     = this._model.getReceivalModel();
            this._syncWithModel     = (this._transferModel != null &&
                                       this._receivalModel != null);
        }
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------

}
