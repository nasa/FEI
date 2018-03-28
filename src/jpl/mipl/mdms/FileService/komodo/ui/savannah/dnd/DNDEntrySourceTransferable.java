package jpl.mipl.mdms.FileService.komodo.ui.savannah.dnd;

import java.awt.datatransfer.*;
import java.io.IOException;



/**
 *
 * Implementation of Transferable that joins the transfer
 * data along with a source identifier.
 * The identifier is used to name the component responsible
 * for the DND initiation.
 *  
 * @author Nicholas Toole (Nicholas.Toole@jpl.nasa.gov)
 * @version $Id: DNDEntrySourceTransferable.java,v 1.1 2004/09/21 22:11:28 ntt Exp $
 */

public class DNDEntrySourceTransferable implements Transferable 
{    
    protected String _entry;
    protected DNDSourceIdentifier _source;
        
    protected DataFlavor _sourceDataFlavor; 
    protected DataFlavor _entryDataFlavor;
    protected DataFlavor[] _flavors = new DataFlavor[2];
    
     
    //---------------------------------------------------------------------
   
    public DNDEntrySourceTransferable(String entry, DNDSourceIdentifier source) 
    {
        _entry = entry;
        _source = source;
    
        _sourceDataFlavor = new DataFlavor(DNDSourceIdentifier.class,
                                           DNDSourceIdentifier.HUMAN_READABLE);
        
        _flavors[0] = _sourceDataFlavor;
        _flavors[1] = DataFlavor.stringFlavor;
    }
    
    //---------------------------------------------------------------------    
    
    // required methods for the interface
    public DataFlavor[] getTransferDataFlavors() 
    {            
        DataFlavor[] flavors = new DataFlavor[_flavors.length];
        for (int i = 0; i < _flavors.length; ++i)
        {
            flavors[i] = _flavors[i];
        }
            
        return flavors;
    }
    
    //---------------------------------------------------------------------
    
    public boolean isDataFlavorSupported(DataFlavor flavor) 
    {
        for (int i=0; i < _flavors.length; ++i) 
        {
            if (flavor == _flavors[i]) 
                return true;
        }
 
        return false;
    }

    //---------------------------------------------------------------------
    
    public Object getTransferData(DataFlavor flavor) 
                throws UnsupportedFlavorException, IOException 
    {
        if (flavor == _sourceDataFlavor)
            return _source;
        if (flavor == DataFlavor.stringFlavor)
            return _entry;
        return null;
    }     
}

