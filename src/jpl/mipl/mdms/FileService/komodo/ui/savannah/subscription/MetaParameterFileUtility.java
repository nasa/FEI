package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.DefaultMetaParameterIO;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.MetaParameterIO;

/**
 * <b>Purpose:</b>
 * Utility to handler user selection of files to load and store 
 * metasubscription parameters.
 *
 *   <PRE>
 *   Copyright 2008, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2008.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 09/02/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: MetaParameterFileUtility.java,v 1.1 2008/09/04 17:32:30 ntt Exp $
 *
 */
public class MetaParameterFileUtility
{
    protected File _currentDirectory;
    protected Component _parentComponent;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param parentComponent Parent component for any UI dialogs,
     * can be null
     */
    
    public MetaParameterFileUtility(Component parentComponent)
    {
        this._parentComponent = parentComponent;
        
        String pwd = System.getProperty("user.dir");
        if (pwd == null)
            pwd = System.getProperty("user.home");
        
        this._currentDirectory = new File(pwd);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Pops up file chooser and stores parameters to selected file.
     * @param params Meta parameters to be stored.
     * @return True if successful, false if error or abort.
     */
    
    public boolean storeParametersToFile(DefaultMetaParameters params)
    {
        boolean success = false;
        OutputStream outStream = null;
        File selectedFile = null;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(this._currentDirectory);
        chooser.setMultiSelectionEnabled(false);
        int returnVal = chooser.showSaveDialog(this._parentComponent);
        
        if(returnVal != JFileChooser.APPROVE_OPTION) 
        {
            return false;
        }
        
        selectedFile = chooser.getSelectedFile();
        this._currentDirectory = selectedFile.getParentFile();
        
           
        try {
            outStream = new FileOutputStream(selectedFile);
            MetaParameterIO mpIo = new DefaultMetaParameterIO();
            mpIo.write(params, outStream, MetaParameterIO.FORMAT_PLAIN);
            success = true;
        } catch (IOException ioEx) {
            JOptionPane.showMessageDialog(this._parentComponent,
                    "Could not write options to cache file.\n"
                    +"Reason: "+ioEx.getMessage(), "Cache Error", 
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            if (outStream != null)
            {
                try {  outStream.flush();
                       outStream.close(); } catch (IOException ioEx) {} 
            }
        }
        
        return success;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Pops up file chooser and loads parameters from selected file.
     * Parameters for user login, filetype, domain URL, and output
     * directory are not affected from original parameters.
     * @param params Meta parameters to be loaded.
     * @return True if successful, false if error or abort.
     */
    
    public boolean loadParametersFromFile(DefaultMetaParameters params)
    {
        boolean success = false;
        
        File selectedFile = null;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(this._currentDirectory);
        chooser.setMultiSelectionEnabled(false);
        int returnVal = chooser.showOpenDialog(this._parentComponent);
        
        if(returnVal != JFileChooser.APPROVE_OPTION) 
        {
            return false;
        }
        
        selectedFile = chooser.getSelectedFile();
        this._currentDirectory = selectedFile.getParentFile();
        
        
        //this should never happen, but just in case
        if (!selectedFile.canRead() || selectedFile.isDirectory())
        {
            JOptionPane.showMessageDialog(this._parentComponent, 
                            "Cache file does not exist.\n"
                            + "Select 'OK' to abort loading.",
                            "Cache Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        //retain ft, outdir, and domain values 
        String filetype   = params.getFiletype();
        URL domainFile    = params.getDomainFile();
        String outdir     = params.getOutputDirectory();
        String username   = params.getUsername();
        String password   = params.getPassword();
        
        //open input stream to file cache
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(selectedFile);
            MetaParameterIO mpIo = new DefaultMetaParameterIO();
            mpIo.read(params, inStream, MetaParameterIO.FORMAT_PLAIN);
            success = true;
        } catch (IOException ioEx) {
            JOptionPane.showMessageDialog(this._parentComponent,
                    "Could not load options from cache file.\n"
                    +"Reason: "+ioEx.getMessage(), "Cache Error", 
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            if (inStream != null)
                try {  inStream.close(); } catch (IOException ioEx) {}
            params.setFiletype(filetype);
            params.setDomainFile(domainFile);
            params.setOutputDirectory(outdir);
            params.setUsername(username);
            params.setPassword(password);            
        }
        
        return success;
    }
    
    //---------------------------------------------------------------------
    
}
