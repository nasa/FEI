package jpl.mipl.mdms.FileService.komodo.client.handlers;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.client.CMD;


/**
 * <B>Purpose:<B>
 * Implementation of FileEventHandlerAcceptStrategy that enables
 * handlers for a set of sub-handlers.  The options passed in via initialize
 * is examined for all necessary properties, (servergroup, filetype,
 * output dir) and passes if all of the underlying handlers pass.
 * Default for no handlers is true.
 * 
 * @see CMD for names of properties.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: JointAndAcceptStrategy.java,v 1.1 2010/01/27 19:47:58 ntt Exp $
 *
 */
public class JointAndAcceptStrategy implements FileEventHandlerAcceptStrategy
{       

    List<FileEventHandlerAcceptStrategy> strategies;
    
    //-----------------------------------------------------------------------
    
    public JointAndAcceptStrategy()
    {        
        this.strategies = new Vector<FileEventHandlerAcceptStrategy>();
    }
    
    //-----------------------------------------------------------------------
    
    public void addStrategy(FileEventHandlerAcceptStrategy strategy)
    {
        if (!this.strategies.contains(strategy))
        {
            this.strategies.add(strategy);
        }
    }
    
    //-----------------------------------------------------------------------
    
    public void removeStrategy(FileEventHandlerAcceptStrategy strategy)
    {
        if (this.strategies.contains(strategy))
        {
            this.strategies.remove(strategy);
        }
    }
    
    //-----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerAcceptStrategy#initialize(java.util.Map, int)
     */
    
    public void initialize(Map options, String actionId) throws IllegalArgumentException
    {        
        Iterator<FileEventHandlerAcceptStrategy> it = this.strategies.iterator();
        while (it.hasNext())
        {
            it.next().initialize(options, actionId);
        }       
    }
    
    //-----------------------------------------------------------------------
    
    /* (non-Javadoc)
     * @see jpl.mipl.mdms.FileService.komodo.client.handlers.FileEventHandlerAcceptStrategy#accept()
     */
    
    public boolean accept()
    {
        boolean returnValue = true;
        
        Iterator<FileEventHandlerAcceptStrategy> it = this.strategies.iterator();
        while (it.hasNext())
        {
            returnValue = returnValue && it.next().accept();
        }
        
        
        return returnValue;
        
    }
    
    //-----------------------------------------------------------------------
}
