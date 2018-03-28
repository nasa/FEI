package jpl.mipl.mdms.FileService.komodo.client.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 
 * <B>Purpose:<B>
 * Handler utility is used to show discoverable handler plugin's and
 * can even be used to create plugin manifest files.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: HandlerUtility.java,v 1.2 2011/06/01 21:51:29 ntt Exp $
 *
 */

public class HandlerUtility
{
    FileEventHandlerRegistryLoader _loader;
    Map<String, FileEventHandlerInfo> _handlerInfoMap;
    OutputStream _os;
    FileEventHandlerBuilder _builder;
    
    boolean _verbose;
    boolean _help;
    //File _outputDir;
    
    //---------------------------------------------------------------------
    
    public HandlerUtility(String[] args)
    {
        init();
        
        try {
            parseArgs(args);
        } catch (IllegalArgumentException iaEx) {
            System.err.println(iaEx.getMessage());
            _help = true;            
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        _verbose = false;
        _help = false;
        
        loadPluginMetadata();
    }
    
    //---------------------------------------------------------------------
    
    protected void parseArgs(String[] args) throws IllegalArgumentException
    {
//        if (args.length == 0)
//        {
//            this._help = true;
//        }
//        else
//        {
        
            for (int i = 0; i < args.length; ++i)
            {
                String arg = args[i];
                if (arg.equalsIgnoreCase("-v") || arg.equalsIgnoreCase("-verbose"))
                {
                    this._verbose = true;
                }
    //            if (arg.equalsIgnoreCase("-o") || arg.equalsIgnoreCase("-out") ||
    //                arg.equalsIgnoreCase("-output"))
    //            {
    //                if (i == args.length - 1)
    //                    
    //                this._verbose = true;
    //                
    //                
    //            }
                else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help") ||
                    arg.equalsIgnoreCase("-?") || arg.equalsIgnoreCase("help"))
                {
                    this._help = true;
                }
                else
                {
                    throw new IllegalArgumentException("Unrecognized argument: "+arg);
                }
                
            }
        //}
    }
    
    //---------------------------------------------------------------------
    
    public void run()
    {
        initOutput();
        
        if (this._help)
            printUsage();
        else if (this._verbose)
            printVerbose();
        else
            printIds();
        
        closeOutput();            
    }
    
    //---------------------------------------------------------------------
    
    protected void initOutput()
    {
        this._os = System.out;
    }
    
    //---------------------------------------------------------------------
    
    protected void closeOutput()
    {
        try {
            this._os.close();
        } catch (IOException ioEx) {
            System.err.println("Error occurred while attempting to close output stream");
            ioEx.printStackTrace();
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void printIds()
    {
        StringBuffer sb = new StringBuffer();
        
        
        if (_handlerInfoMap.size() == 0)
        {
            System.err.println("No handler plugins found.");
        }
        else
        {
        
            Iterator<String> keys = _handlerInfoMap.keySet().iterator();
            while (keys.hasNext())
            {
                String key = keys.next();
                sb.append(key).append("\n");
            }
            
            try {
                this._os.write(sb.toString().getBytes());
            } catch (IOException ioEx) {
                System.err.println("Error occurred while attempting to write to output.");
                ioEx.printStackTrace();
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void printVerbose()
    {
        
        
        StringBuffer sb = new StringBuffer();
        
        if (_handlerInfoMap.size() == 0)
        {
            System.err.println("No handler plugins found.");
        }
        else
        {
            sb.append("\nHandler count = ").append(_handlerInfoMap.size()).append("\n\n");
        
            Iterator<String> keys = _handlerInfoMap.keySet().iterator();
            int count = 0;
            
            while (keys.hasNext())
            {
                String key = keys.next();
                FileEventHandlerInfo info = _handlerInfoMap.get(key);
                ++count;
                
                sb.append("Handler ").append(count).append("\n================================\n");
                sb.append("Id: ").append(key).append("\n");
                if (info.getName() != null)
                    sb.append("Name: ").append(info.getName()).append("\n");
                if (info.getDescription() != null)
                    sb.append("Description: ").append(info.getDescription()).append("\n");
                if (info.getVersion() != null)
                    sb.append("Version: ").append(info.getVersion()).append("\n");
                if (info.getOrganization() != null)
                    sb.append("Organization: ").append(info.getOrganization()).append("\n");
                if (info.getImplementation() != null)
                    sb.append("Implementation: ").append(info.getImplementation()).append("\n");
                sb.append("\n\n");
                
            }
            
            try {
                this._os.write(sb.toString().getBytes());
            } catch (IOException ioEx) {
                System.err.println("Error occurred while attempting to write to output.");
                ioEx.printStackTrace();
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    protected void printUsage()
    {
        System.err.println("Usage: java "+HandlerUtility.class.getName()+" [-v(erbose) | -h(elp) ]");
        System.err.println();
    }
    
    //---------------------------------------------------------------------
    
    protected void loadPluginMetadata()
    {
        this._handlerInfoMap = new HashMap<String, FileEventHandlerInfo>();
        
        //construct internal registry loader and hander builder
        this._loader = new FileEventHandlerRegistryLoader();
        this._builder = new FileEventHandlerBuilder();
        
        List<FileEventHandlerInfo> handlerInfos = this._loader.getHandlerInfos();
        
        //iterate over returned metadata, adding each to map using
        //its id as the key
        Iterator<FileEventHandlerInfo> it = handlerInfos.iterator();
        while (it.hasNext())
        {
            FileEventHandlerInfo metadata = it.next();
            
            this._handlerInfoMap.put(metadata.getId(), metadata);
        }        
        
    }
    
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        HandlerUtility utility = new HandlerUtility(args);
        
        utility.run();
    }
    
    //---------------------------------------------------------------------

}
