package jpl.mipl.mdms.FileService.komodo.api;

import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import jpl.mipl.mdms.FileService.io.BufferedStreamIO;
import jpl.mipl.mdms.FileService.io.MessagePkg;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * 
 * <B>Purpose:<B>
 * The filetype query client class that implements a query to
 * a server requesting filetype listings.  Current implementation
 * of result handling is synchronous.
 *
 * The call for retrieve filetypes requires a parameter of
 * type ServerInfo that provides the host and port information.
 * Subsequent calls to getFileTypes is supported using different
 * instances of the ServerInfo argument.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileTypeQueryClient.java,v 1.2 2010/08/11 16:08:36 awt Exp $
 *
 */

public class FileTypeQueryClient
{
    //For logging events
    private final Logger _logger = Logger.getLogger(FileTypeQueryClient.class.getName());  
    
    //Connection _conn;
    
    int _tcpStartPort  = 0;
    int _tcpEndPort    = 0;
    boolean _ssl       = false;
    int _securityModel = Constants.INSECURE;
    int _timeout       = Constants.TIMEOUT_NONE;
    
    //---------------------------------------------------------------------
    
    /**
     * Default constructor, defaults to security enabled.
     */
    
    public FileTypeQueryClient() throws SessionException
    {
        this(true);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param ssl True if ssl connection should be used, false for
     * insecure.
     */
    public FileTypeQueryClient(boolean ssl) throws SessionException
    {
        this._ssl = ssl;
        init();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes this instance
     */
    
    protected void init() throws SessionException
    {
        
        //----------------
        
        configure();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Configures the security model, time out, and port range for connection
     * creation.
     */
    
    protected void configure() throws SessionException
    {
        //-------------------------
        //determine security model
        
        if (this._ssl)
        {
            this._securityModel = Constants.SSL;
        }
        else
        {
            this._securityModel = Constants.INSECURE;
        }
        
        //-------------------------
        //get the timeout
        
        this._timeout = this._getInitialConnectionTimeout();
        
        //-------------------------
        //check for tcp range (not always set)
        
        String startTcpPortStr = System.getProperty("komodo.tcp.startPort");
        String endTcpPortStr   = System.getProperty("komodo.tcp.endPort");
        
        try {
            this._setTcpPortRange(Integer.parseInt(startTcpPortStr),
                              Integer.parseInt(endTcpPortStr));
        } catch (NumberFormatException nfe) {
            // TODO ignore
        }
      //-------------------------
        
    }
    
    //---------------------------------------------------------------------

    public void setTimeout(int timeoutMillis)
    {
        if (timeoutMillis < 0)
            timeoutMillis = 0;
        
        this._timeout = timeoutMillis;
    }
    
    //---------------------------------------------------------------------
    
   
    public int getTimeout()
    {
        return this._timeout;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that enforces order and bounds of tcp
     * port start and end values.
     */
    
    protected void _setTcpPortRange(int start, int end) throws SessionException 
    {
        if (start < Constants.MINTCPPORT && end > Constants.MAXTCPPORT) 
        {
           throw new SessionException("TCP port number not between "
                 + Constants.MINTCPPORT + " and " + Constants.MAXTCPPORT + ".",
                 Constants.TCP_PORT_RANGE);
        }
        if (start > end) 
        {
           throw new SessionException("TCP start port greater than end port.",
                 Constants.TCP_PORT_RANGE);
        }
        this._tcpStartPort = start;
        this._tcpEndPort = end;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Convenience method that determines initial connection timeout value.
     * System property associated with <code>Constants.PROPERTY_CLIENT_TIMEOUT</code>
     * is checked (NOTE: units are in milliseconds).  
     * @return Initial connection timeout value
     */
    
    protected int _getInitialConnectionTimeout()
    {
        int timeout;
        
        //check to see if property is set
        String timeoutStr = System.getProperty(Constants.PROPERTY_CLIENT_TIMEOUT);
        if (timeoutStr != null)
        {
            try {
                timeout = Integer.parseInt(timeoutStr);
            } catch (NumberFormatException nfEx) {
                timeout = Constants.TIMEOUT_NONE;
            }
        }
        else
        {
            timeout = Constants.TIMEOUT_DEFAULT;
        }
        
        if (timeout < 0)
            timeout = Constants.TIMEOUT_NONE;
        
        return timeout;
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    /**
     * Submit request for filetype information and return results.
     * This call is synchronous and will block while connection is
     * open.
     * @param serverInfo Server info data object
     * @return List of filetype names associated with server, or null
     * if error occurred.
     * @throws SessionException if session error occurs
     */
    
    public List getFileTypes(ServerInfo serverInfo) throws SessionException
    {
        return getFileTypes(serverInfo, null, null);
    }

    //---------------------------------------------------------------------
    
    /**
     * Submit request for filetype information and return results.
     * This call is synchronous and will block while connection is
     * open.  Includes user credentials for role based lookup and
     * filtering.
     * @param serverInfo Server info data object
     * @param user Username
     * @param pass User password (potentially encryped)
     * @return List of filetype names associated with server, or null
     * if error occurred.
     * @throws SessionException if session error occurs
     */
    
    public List getFileTypes(ServerInfo serverInfo, String user, 
                             String password)  throws SessionException
    {
        List types = null;
        Connection conn = null;
        
        try {
            
            //open connection to server
            conn = openConnection(serverInfo);
            
            //if null, then return null result
            if (conn == null)
                return null;
            
            //otherwise, request the types using the conn
            types = getFileTypes(conn, user, password);
            
        } catch (SessionException sesEx) {     
            
            types = null;
            throw sesEx;
            
        } finally {
            
            //close connection if necessary
            boolean sendQuitMesg = (types != null);
            
            if (conn != null)
                closeConnection(conn, sendQuitMesg);           
        }
        
        return types;
    }
        
    //---------------------------------------------------------------------
    
    /** 
     * Opens a connection to the server using information provided by
     * parameter.
     * @param serverInfo Server info data object
     * @return Connection to server, null if unable to connect
     * @throws SessionException If session error, or io error, occurs.
     */
    
    protected Connection openConnection(ServerInfo serverInfo) throws SessionException
    {
        Connection conn = null;
        
        String hostname = serverInfo.getHostName();
        
        // Now, establish the connection to the server.
        int port = serverInfo.getPort();
        boolean admin = false;  //assume we never use admin for now
        if (admin)
           port += 1;
        
        try {
            
            //create a connection object (similar to what ServerProxy does)
            conn = new Connection(hostname,  port, 
                                  this._securityModel,  
                                  this._tcpStartPort, 
                                  this._tcpEndPort,
                                  this._timeout);
            
        } catch (IOException ioEx) {    
            
            this._logger.trace("IO error occured while opening connection to "+
                               hostname+":"+port,  ioEx);
            
            //close connection if it was created (unlikely but hey)
            if (conn != null)
            {
                closeConnection(conn, false);
                conn = null;
            }
            
            throw new SessionException(ioEx.getMessage(), Constants.IO_ERROR);
        }
        
        return conn;
    }
        
    //---------------------------------------------------------------------
    
    /** 
     * Clones a connection to the server.  Attempts to write quit message
     * to server before closing.
     * @param Connection conn Connection to be closed.
     * @return Connection to server, null if unable to connect
     * @throws SessionException If session error, or io error, occurs.
     */
    
    protected void closeConnection(Connection conn, boolean quit) 
                                          throws SessionException
    {
        if (conn != null)
        {
            try {
                
                //send quit command
                if (quit)
                    conn.getIO().writeLine(Constants.QUIT);

                //close the actual connection
                conn.close();
                
                //set to null
                conn = null;
                
            } catch (IOException ioEx) {
                this._logger.trace("IO error occured while closing connection",
                                   ioEx);
                throw new SessionException(ioEx.getMessage(), Constants.IO_ERROR);
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    protected List getFileTypes(Connection conn, String user, String pass)
                                                   throws SessionException
    {
        List types = new Vector();
        
        String querycmd = Constants.PROTOCOLVERSION + " " + Constants.QUERYTYPES;
        
        //append username and password?
        if (user != null && pass != null && !user.equals("") && !pass.equals(""))
            querycmd += " " + user + " " + pass;
        
        this._logger.trace(this + " ftqueryutil.run () query: " + querycmd);

        try {
            BufferedStreamIO io = conn.getIO();
    
            io.writeLine(querycmd);
            MessagePkg srvReply = io.readMessage();
            
            if (srvReply.getErrno() != Constants.OK) 
            {
                if (srvReply.getErrno() == Constants.UNKNOWNCMD)
                {
                    this._logger.trace(this + " querycmd reply: " + srvReply.getMessage());
                    throw new SessionException("Server does not recognize the "+
                    		                   "query command: "+querycmd, 
                                               Constants.UNKNOWNCMD);
                }
                else
                {
                    this._logger.trace(this + " querycmd reply: " + srvReply.getMessage());
                    throw new SessionException("Invalid Query", 
                                               Constants.INVALID_COMMAND);
                }
            }
            
            
            String reply;
            do 
            {
               reply = io.readLine();
               if (reply.length() == 0)
                  throw new SessionException("Unexpected eof from server",
                                             Constants.UNEXPECTED_EOF);
    
               StringTokenizer st = new StringTokenizer(reply, "\t");
               st.nextToken();
               if (reply.charAt(0) == 'i') 
               {
                  // Strip of leading and trailing white-spaces and save filetype.
                  String tmp1 = st.nextToken().trim();
                  this._logger.trace(this + " komodo.filetype string = " + tmp1);
                  if (!types.contains(tmp1))
                      types.add(tmp1);
               } 
               
               this._logger.trace(this + " querytypes reply: " + reply);
            } while (!reply.startsWith("eol"));
            
        } catch (IOException ioEx) {
            throw new SessionException(ioEx.getMessage(), Constants.IO_ERROR);
        }
        
        return types;
    }
    
    //---------------------------------------------------------------------
}
