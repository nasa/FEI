package jpl.mipl.mdms.FileService.komodo.client.handlers;

import jpl.mipl.mdms.FileService.komodo.api.Result;

/**
 * <B>Purpose:<B>
 * Data type encapsulating the operation and Komodo Result 
 * associated with a file event.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileResultEvent.java,v 1.3 2009/08/07 15:53:55 ntt Exp $
 *
 */
public class FileResultEvent
{
    protected Result _result;
    protected String _taskId;
    
    public FileResultEvent(String taskId, Result result)
    {
         this._result = result;
         this._taskId = taskId;        
    }
    
    public Result getResult()
    {
        return this._result;
    }
    
    public String getTaskId()
    {
        return this._taskId;
    }        
}
