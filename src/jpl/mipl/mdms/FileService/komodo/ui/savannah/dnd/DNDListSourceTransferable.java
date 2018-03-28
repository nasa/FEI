package jpl.mipl.mdms.FileService.komodo.ui.savannah.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;
import java.util.Vector;



/**
 *
 * Implementation of Transferable that joins the transfer
 * data along with a source identifier.
 * The identifier is used to name the component responsible
 * for the DND initiation.
 *  
 * @author Nicholas Toole (Nicholas.Toole@jpl.nasa.gov)
 * @version $Id: DNDListSourceTransferable.java,v 1.2 2005/02/02 00:08:43 ntt Exp $
 */

public class DNDListSourceTransferable implements Transferable 
{    
    public static final String LOCAL_HUMAN_READABLE_NAME  = "java.util.List";
    public static final String LOCAL_LIST_TYPE  = 
                                    DataFlavor.javaJVMLocalObjectMimeType +
                                                    ";class=java.util.List";         
    public static final Class  SERIAL_CLASS_TYPE = java.util.List.class;
    public static final String SERIAL_HUMAN_READABLE_NAME = "List";
    
    //data
    protected java.util.List      _list;
    protected String              _listString;
    protected DNDSourceIdentifier _source;
        
    //data flava' flaves
    protected DataFlavor _sourceDataFlavor, _stringFlavor; 
    protected DataFlavor _localListFlavor, _serialListFlavor;
    protected DataFlavor[] _flavors = new DataFlavor[4];
         
    //---------------------------------------------------------------------
   
    public DNDListSourceTransferable(List list, 
                                     DNDSourceIdentifier source) 
    {
        _list       = new Vector(list);
        _source     = source;
        _listString = listToString(_list);
        init();
    }
    
    //---------------------------------------------------------------------
    
    public DNDListSourceTransferable(String string, 
                        DNDSourceIdentifier source) 
    {
        _listString = string;
        _list       = stringToList(_listString);
        _source     = source;
        init();
    }
    
    //---------------------------------------------------------------------
    
    public static String listToString(List list)
    {
        StringBuffer buffer = new StringBuffer();
        int listSize = list.size();
        if (listSize > 0)
            buffer.append((String) list.get(0));
        for (int i = 1; i < listSize; ++i)
        {
            buffer.append("\t"+list.get(i));
        }
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
    
    public static List stringToList(String string)
    {
        List list = new Vector();        
        String[] array = string.split("\t");
        
        if (array == null)
            return list;
        
        for (int i = 0; i < array.length; ++i)
        {
            if (array[i] != null && !array[i].equals(""))
            {
                list.add(array[i]);
            }
        }
        
        return list;
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        _stringFlavor = DataFlavor.stringFlavor;
        
        //build source data flavor
        _sourceDataFlavor = new DataFlavor(DNDSourceIdentifier.class,
                                           DNDSourceIdentifier.HUMAN_READABLE);
        
        // build local list flavor
        String localListType = DataFlavor.javaJVMLocalObjectMimeType +
                                               ";class=java.util.List";

        _localListFlavor = new DataFlavor(localListType, 
                                LOCAL_HUMAN_READABLE_NAME);
        
        //build serial list flavor
        _serialListFlavor = new DataFlavor(java.util.List.class,
                                SERIAL_HUMAN_READABLE_NAME);
        
        _flavors[0] = _sourceDataFlavor;
        _flavors[1] = _stringFlavor;
        _flavors[2] = _localListFlavor;
        _flavors[3] = _serialListFlavor;
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
                                        throws UnsupportedFlavorException
    {
        if (flavor.equals(_sourceDataFlavor))
            return _source;
        if (flavor.equals(_stringFlavor))
            return _listString;
        if (flavor.equals(_localListFlavor))
            return _list;
        if (flavor.equals(_serialListFlavor))
            return _list;
        return null;
    }     
}

