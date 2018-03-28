/*
 * Created on Feb 17, 2005
 */
package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.handler;

import java.awt.Component;
import java.awt.Dialog;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.HandlerInfo;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.PluginLoader;

/**
 * <b>Purpose:</b>
 * Displays filenames in a dialog. Special attributes are:
 * <pre>
 *  (1) modal - "true" if dialog should be modal, "false" otherwise, 
 *               default false
 *  (2) title - Title to be displayed, default is "New Files Available"
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
 * 02/17/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: NotificationPanelHandler.java,v 1.4 2005/03/29 03:01:51 ntt Exp $
 *
 */

public class NotificationPanelHandler extends AbstractSubscriptionHandler
{
    private final String  __classname  = this.getClass().getName();
    protected String      _dialogTitle = null;
    protected boolean     _dialogModal = false;
    protected Component   _relativeComponent = null;
    
    /** Name of the title property */
    public final static String PROPERTY_TITLE = "title";
    
    /** Name of the modal property */
    public final static String PROPERTY_MODAL = "modal";
    
    protected final static String NO_FILETYPE   = "NULL";
    protected final static String DEFAULT_TITLE = "New Files Available";
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes this handler from handler info object
     * @param hInfo Handler info object used to initialize this handler
     */
    
    public void initialize(HandlerInfo hInfo) throws Exception
    {   
        super.initialize(hInfo);
        
        //check for modality (not required)
        String value = hInfo.getProperty(PROPERTY_MODAL);
        this._dialogModal = PluginLoader.getBooleanValue(value, false);
        
        //check for title (not required)
        value = hInfo.getProperty(PROPERTY_TITLE);
        this._dialogTitle = (value == null) ? DEFAULT_TITLE : value;
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
        String filetype = null;
        String[] filenames = new String[0];
        
        if (results.isEmpty())
            return;
        
        int numResults = results.size();
        filenames = new String[numResults];
        for (int i = 0; i < numResults; ++i)
        {
            Result result = (Result) results.get(i);

            filenames[i] = result.getName();
            
            if (i == 0)
            {
                String group, ft;
                group = result.getServerGroup();
                ft    = result.getType();

                if (group != null && ft != null)
                    filetype = group + ":" + ft;
                else
                    filetype = NO_FILETYPE;
            }
        }
        
        displayNewFiles(filetype, filenames);
    }
    
    //---------------------------------------------------------------------
    
    protected void displayNewFiles(String filetype, String[] filenames)
    {
        //if no files, return
        if (filenames.length == 0)
            return;
        
        //build the message
        
        StringBuffer buffer = new StringBuffer();
        
        if (filenames.length == 1)
        {
            buffer.append("New file available in filetype '").append(
                          filetype).append("':\n1)\t").append(
                          filenames[0]).append("\n"); 
        }
        else
        {
            buffer.append("New files available in filetype '").append(
                          filetype).append("':\n");  
            for (int i = 0; i < filenames.length; ++i)
            {
                buffer.append(i+1).append(")\t").
                       append(filenames[i]).append("\n");
            }
        }
        final String message = buffer.toString();
        
        //display message
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                JOptionPane pane = new JOptionPane(message, 
                                        JOptionPane.INFORMATION_MESSAGE);
                Dialog dialog = pane.createDialog(_relativeComponent, 
                                                  _dialogTitle);
                dialog.setModal(_dialogModal);
                dialog.setVisible(true);
            }    
        });
    }
    
    
    //---------------------------------------------------------------------
    
    public void setAppModel(SavannahModel model)
    {
        super.setAppModel(model);
        this._relativeComponent = (this._model == null) ? null : 
                                   this._model.getRelativeComponent();  
    }
    
    //---------------------------------------------------------------------

}
