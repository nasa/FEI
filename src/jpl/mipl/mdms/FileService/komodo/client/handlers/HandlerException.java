package jpl.mipl.mdms.FileService.komodo.client.handlers;

/**
 * <B>Purpose:<B>
 * Exceptions caused by errors or issues within the handler framework.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: HandlerException.java,v 1.2 2009/08/07 15:53:55 ntt Exp $
 *
 */
public class HandlerException extends Exception
{
    public HandlerException(String message)
    {
        super(message);
    }
    
    public HandlerException(String message, Throwable cause)
    {
        super(message, cause);
    }    
}
