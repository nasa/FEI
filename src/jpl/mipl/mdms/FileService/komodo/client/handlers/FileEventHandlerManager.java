package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.client.CMD;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * Manages loading of file event handlers and presents a single entry
 * point for clients to acquire those handlers.
 * 
 * There are two forms of the constructor. The first checks the value of
 * a system property to determine if handling is enabled.  The second offers
 * a parameter to force handlers to be loaded.   
 */

public class FileEventHandlerManager implements FileEventHandlerRegistryIF
{
    FileEventsContext context;
    FileEventHandlerRegistryIF registry;
    
    /**
     * Strategy reference to determine if a particular 
     * type of handler should be loaded.
     */
    FileEventHandlerAcceptStrategy acceptStrategy;
    //FileEventHandlerLocateStrategy locateStrategy;

    boolean enabled = false;
    
    /**
     * Property indicating that handler framework is to be used, if true.
     * Otherwise, the manager will only load handlers if the forceEnabled
     * parameter is true.
     * Name: komodo.filehandling.enable
     * Values: true/false
     */
    public static final String PROPERTY_HANDLERS_ENABLED = Constants.PROPERTY_FILEHANDLING_ENABLE;
    
    public static final String PROPERTY_HANDLERS_CONTINUE = "komodo.filehandler.continue";
    
    //Loggers
    private Logger _logger = Logger.getLogger(FileEventHandlerManager.class.getName());
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * During initialization, the manager checks the environment to determine
     * if event handling is enabled.  If so, then it continues to load
     * the handler framework. Otherwise, the framework will not be loaded
     * and calls to getFileEventHandlers() will return an empty set.
     * To ensure that handling is enabled, call the other constructor.
     * Property name is 'jpl.mipl.mdms.fei.handling.enable'.
     * @param options Map of argument options
     * @param actionId Identifier for the operation
     */
    
    public FileEventHandlerManager(Map options, String actionId)
    {
        this(options, actionId, false);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * During initialization, the manager checks the environment to determine
     * if event handling is enabled if the forceEnabled parameter is false; 
     * otherwise handling will be enabled.
     * To rely on system environment to determine if handling should be enabled, 
     * call the other constructor.
     * @param options Map of argument options
     * @param actionId Identifier for the operation
     * @param forceEnabled True to force handlers to be enabled, false otherwise
     */
    
    public FileEventHandlerManager(Map options, String actionId, boolean forceEnabled)
    {        
        _logger.trace("FileEventHandlerManager constructor for action id "+actionId);
        
        if (forceEnabled)
            this.enabled = true;
        else
            loadEnabled();
        
        if (this.enabled)
        {
            loadAcceptStrategy();  
            init(options, actionId);
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void loadEnabled()
    {
        boolean shouldEnable = false;
        
//        //check command line options
//        if (options != null && options.containsKey(CMD.FILEHANDLER))
//        {
//            shouldEnable = ((Boolean)options.get(CMD.FILEHANDLER)).booleanValue();            
//        }
//        
        //check system property
        if (!shouldEnable)
        {
            String value = System.getProperty(PROPERTY_HANDLERS_ENABLED);
            if (value != null)
            {
                shouldEnable = Boolean.parseBoolean(value);         
            }   
        }
        
        this.enabled = shouldEnable;
    }
    
    //---------------------------------------------------------------------
    
    protected void loadAcceptStrategy()
    {        
        //for now, we will use 'replicate' in options to indicate
        //that handlers should be enabled, this could be generalized,
        //if the need ever arises
        JointAndAcceptStrategy jointStrat = new JointAndAcceptStrategy();
        //jointStrat.addStrategy(new ReplicationAcceptStrategy());
        jointStrat.addStrategy(new FileHandlerOptAcceptStrategy());        
        this.acceptStrategy = jointStrat;
        
    }
    
    //---------------------------------------------------------------------
    
    protected void init(Map options, String actionId)
    {      
        this.acceptStrategy.initialize(options, actionId);
         
        if (acceptStrategy.accept())
        {                      
            //---------------------
            
            //collect data required for handler context            
            String sg  = (String) options.get(CMD.SERVERGROUP);
            if (sg == null)
                throw new IllegalArgumentException("Options parameter does not " +
                        "contain value for '"+CMD.SERVERGROUP+"'");

            
            String ft  = (String) options.get(CMD.FILETYPE);
            if (ft == null)
                throw new IllegalArgumentException("Options parameter does not " +
                        "contain value for '"+CMD.FILETYPE+"'");
            
            
            String dir = (String) options.get(CMD.OUTPUT);        
            if (dir == null)
                dir = System.getProperty("user.dir");
            
            //---------------------
            
            //create the context for the registry
            context = new FileEventsContext(sg, ft, dir, actionId);
            

            //---------------------
            
            //create registry            
            this.registry = new FileEventHandlerRegistry(context);
        }

        //-------------------------        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if manager determined that handling was enabled, false 
     * otherwise.
     * @return Enabled state
     */
    
    public boolean isEnabled()
    {
        return this.enabled;
    }

    //---------------------------------------------------------------------
    
    /**
     * Returns the file event handling context built by manager.
     
     */
    public FileEventsContext getContext()
    {
        return this.context;
    }
    
    //---------------------------------------------------------------------
    
    public FileEventHandlerSet getFileEventHandlers()
    {
        if (this.registry == null)
            return new FileEventHandlerSet();
        else
            return this.registry.getFileEventHandlers();
    }
    
    //---------------------------------------------------------------------
    
}
