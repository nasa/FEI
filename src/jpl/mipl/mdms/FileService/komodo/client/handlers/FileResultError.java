package jpl.mipl.mdms.FileService.komodo.client.handlers;

import jpl.mipl.mdms.FileService.komodo.api.Result;

/**
 * <B>Purpose:<B>
 * Data type encapsulating the operation, Komodo Result, 
 * and Throwable/Exception associated with an error.
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileResultError.java,v 1.4 2009/08/07 15:53:55 ntt Exp $
 *
 */

public class FileResultError
{
    protected Throwable _throwable;
    protected Result    _result;
    protected String       _taskId;

    public FileResultError(String taskId, Throwable t)
    {
         this._throwable = t;
         this._taskId    = taskId;        
    }
    
    public FileResultError(String taskId, Result result)
    {
         this._result = result;
         this._taskId = taskId;        
    }
    
    public FileResultError(String taskId, Result result, Throwable t)
    {
         this._result = result;
         this._throwable = t;
         this._taskId = taskId;        
    }
    
    public Throwable getThrowable()
    {
        return this._throwable;
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
