package jpl.mipl.mdms.FileService.komodo.api;

import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Handler that waits for invocation to begin a shutdown procedure.
 * Implementors must overwrite the <code>shutdown()</code> method with  
 * operation shutdown logic.  
 * 
 * If the method <code>expectsConfirmation()</code> returns true, then
 * this handler will wait until it receives a confirmation or is stopped.
 * Otherwise, handler will stop once shutdown procedure completes. 
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
 * 08/30/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: RequestTerminationHandler.java,v 1.1 2005/08/31 23:59:56 ntt Exp $
 *
 */

public abstract class RequestTerminationHandler implements Runnable
{
    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_WAITING = 4;
    public static final int STATE_SUCCESS = 2;
    public static final int STATE_ABORTED = 3;
    
    protected boolean[] _done    = {false};
    protected final int[] _state = {STATE_INITIALIZED}; 
    protected final boolean[] _expectsConfirmation = {false};
    
    private Logger _logger = Logger.getLogger(RequestTerminationHandler.class.getName());
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Defaults expectsConfirmation to false.
     */
    
    public RequestTerminationHandler()
    {
        this(false);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param expectsConfirmation Flag indicating whether handler expects
     * to receive confirmation from external caller before exiting.
     */
    
    public RequestTerminationHandler(boolean expectsConfirmation)
    {
        this._expectsConfirmation[0] = expectsConfirmation;
        initialize();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Implementation of the Runnable interface.  Initializes handler,
     * waits for call to invoke before performing shutdown.  May wait
     * for call to done for notification that shutdown is fully complete.
     * A call to abort() can dismiss waiting for these events, to exit
     * this method quickly.
     */
    
    public final void run()
    {
        //-------------------------   
    
        //wrap in try/final to ensure cleanup is called
        try {
            
            //set state to running if not aborted
            synchronized(this._state) {
                if (this._state[0] == STATE_ABORTED)
                    return;
                else
                    this._state[0] = STATE_RUNNING;
            }
            
            //-------------------------         
            
            //perform shutdown
            shutdown();
                
            //-------------------------    
            
            //wait until confirm()/abort() is called? 
            if (this.expectsConfirm())
            {
                try {   
                    
                    //set state to waiting if not aborted
                    synchronized(this._state) {
                        if (this._state[0] == STATE_ABORTED)
                            return;
                        else
                            this._state[0] = STATE_WAITING;
                    }
                    
                    //wait for done to be notified if not already set
                    synchronized(_done) {
                        if (!this._done[0])
                            this._done.wait();
                    }                
                } catch (InterruptedException iEx) {
                    _logger.error(iEx.getMessage());
                    _logger.debug(null, iEx);
                }
            }
            
            //-------------------------

            //set state to success if not aborted
            synchronized(this._state) {
                if (this._state[0] == STATE_ABORTED)
                    return;
                else
                    this._state[0] = STATE_SUCCESS;
            }

            //perform any post shutdown behavior that might be necessary
            postShutdown();                         
            
        } finally {
            //perform any cleanup that might be necessary
            cleanup();
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Instructs implementation to perform the necessary shutdown request
     * to the server.  The request is synchronous, so that once complete, 
     * it waits until shuwdownComplete() is called before returning.
     */
    
    public final synchronized void start()
    {
        boolean callRun = false;
        synchronized(this._state) {
            if (this._state[0] == STATE_INITIALIZED)
            {            
                callRun = true;
            }
        }
        
        if (callRun)
            run();
        else
            return;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Called to signify that shutdown is complete and successful,
     * so that the request can return.
     */
    
    public final void confirm()
    {        
        synchronized(this._done)
        {
            //if aleady done, dont continue
            if (this._done[0])
                return;
            else
                this._done[0] = true;
            
            //perform notify which will wakeup waiting thread if any
            this._done.notify();
        }        
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * Notifies that shutdown procedure should be aborted.  If invocation
     * has not yet occurred, then it is skipped.  If confirmation has
     * not yet occurred, that too is skipped.
     */
    
    protected final void abort()
    {
        synchronized(this._done) {
            
            //if aleady done, dont continue
            if (this._done[0])
                return;
            else
                this._done[0] = true;
            
            //set state if not already aborted or success
            synchronized(this._state) {
                if (_state[0] == STATE_ABORTED || _state[0] == STATE_SUCCESS)
                    return;
                else
                    this._state[0] = STATE_ABORTED;
            }
            
            //perform notify which will wakeup waiting thread if any
            this._done.notify();
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Stops the handler.  If start was never called, then stopping
     * handler puts it in a non-startable state.  If start() was
     * already invoked but not complete, then state is set to ABORT.
     * If handler has already completed, then state is unaffected.
     */
    
    public final synchronized void stop()
    {
        abort();
    }
    
    //---------------------------------------------------------------------
        
    /**
     * Returns state of this handler.  One of STATE_{INITIATED,RUNNING,
     * WAITING,ABORTED,SUCCESS} as defined by this class.
     * @return Handler state.
     */
    
    public final int getState()
    {
        synchronized(this._state) {
            return this._state[0];
        }
    }
        
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that returns true if state of handler is either 
     * running or waiting.
     * @return running state
     */
    
    public final boolean isRunning()
    {
        synchronized(this._state) 
        {
            return (this._state[0] == STATE_RUNNING || 
                    this._state[0] == STATE_WAITING);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that returns true if state of handler is complete
     * and successful.
     * @return Success state
     */
    
    public final boolean isSuccessful()
    {
        synchronized(this._state) 
        {
            return (this._state[0] == STATE_SUCCESS);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if this handler waits for external confirmation after
     * its been invoked, but before it completes successfully.  If true,
     * then shutdown logic will perform a wait until one of <code>confirm(),
     * abort(), stop()</code> is called.
     * @return True if handler expects external confirmation.
     */
    
    public final boolean expectsConfirm()
    {
        return this._expectsConfirmation[0];
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Contains all shutdown logic for this handler.  If the handler 
     * expects external notification of success, it will wait until
     * a call is made.  If no such notification is required, then 
     * simply add a call to done() at the end of this method. 
     */
    
    protected abstract void shutdown();
    
    //---------------------------------------------------------------------
    
    /**
     * Performs any initialization that might be required for
     * handler.
     */
    
    protected void initialize() {};
    
    //---------------------------------------------------------------------
    
    /**
     * Performs any initialization that might be required for
     * handler.
     */
    
    protected void cleanup() {};
    
    //---------------------------------------------------------------------
    
    /**
     * Performs any post shutdown logic like cleanup or status 
     * reporting.
     */
    
    protected void postShutdown() {};

    
    //---------------------------------------------------------------------
}
