package jpl.mipl.mdms.FileService.komodo.ui.savannah;

/**
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 *  
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahListException.java,v 1.2 2004/08/20 01:02:30 ntt Exp $
 */
public class SavannahListException extends Throwable 
{
    protected SavannahList _list;
    
    public SavannahListException(SavannahList list,
                                 String message)
    {
        super(message);
        _list = list;
    }
    
    public SavannahList getList()
    {
        return _list;
    }
    
}
