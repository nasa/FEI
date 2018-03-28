package jpl.mipl.mdms.FileService.komodo.services.query.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.api.ClientRestartCache;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.client.CMD;
import jpl.mipl.mdms.FileService.komodo.client.UtilCmdParser;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QConstants;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QLoginInfo;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QueryConstraints;
import jpl.mipl.mdms.FileService.komodo.services.query.api.QueryList;
import jpl.mipl.mdms.FileService.komodo.services.query.client.QueryClient;
import jpl.mipl.mdms.FileService.util.DateTimeFormatter;

/**
 * 
 * <b>Purpose:</b>
 * Utility class that creates instances of QueryClient. 
 * 
 * Using the options and operation parameters passed
 * into the constructor to build the login information and
 * location of the query file.  The query service location
 * if determined by inspecting the value of the 
 * service location system property.
 * 
 *   <PRE>
 *   Copyright 2007, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2007.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 07/18/2007        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QueryClientUtil.java,v 1.6 2009/08/07 01:00:48 ntt Exp $
 *
 */
public class QueryClientUtil
{
    Map options;
    String operation;

    //---------------------------------------------------------------------

    /**
     * Constructor
     * @param options Options map that should contain values for the
     * following: user, password, server group, query.
     * @param operation 
     */

    public QueryClientUtil(Map options, String operation)
    {
        this.options = options;
        this.operation = operation;
    }

    //---------------------------------------------------------------------

    /**
     * Returns a QLoginInfo instance with data populated from the
     * options map passed into the constructor.  As such, map
     * must contains values for the following keywords, otherwise
     * a SessionException will be thrown: <code>CMD.USER, CMD.PASSWORD,
     * CMD.SERVERGROUP</code>
     * @return QLoginInfo instance
     */

    public QLoginInfo getLoginInfo() throws SessionException
    {
        QLoginInfo loginInfo = null;
        String user = (String) options.get(CMD.USER);
        String pass = (String) options.get(CMD.PASSWORD);
        String srvg = (String) options.get(CMD.SERVERGROUP);

        if (user == null)
            throw new SessionException("Cannot create login info.  "
                    + "Missing user option", Constants.MISSINGARG);
        if (pass == null)
            throw new SessionException("Cannot create login info.  "
                    + "Missing password option", Constants.MISSINGARG);
        if (srvg == null)
            throw new SessionException("Cannot create login info.  "
                    + "Missing server group option", Constants.MISSINGARG);

        loginInfo = new QLoginInfo(user, pass, srvg, operation);

        return loginInfo;
    }

    //---------------------------------------------------------------------

    /**
     * Examines service location property and returns bound value.
     * Property is <code>QConstants.PROP_SERVICE_LOCATION</code>
     * @return FeiQ service location
     */

    public String getServiceLocation()
    {
        String service = null;

        service = System.getProperty(QConstants.PROP_SERVICE_LOCATION);

        return service;
    }

    //---------------------------------------------------------------------

    /**
     * Examines options map for value of keyword <code>CMD.QUERY</code>.
     * This should be the path to the query file.  This file will be loaded
     * into a new instance of QueryList and returned to the caller.
     * @return QueryList populated from query file
     * @throws SessionException if error occurs
     */

    public QueryList getUserQuery() throws SessionException
    {
        //load file specified as the query file

        String queryLocation = (String) this.options.get(CMD.QUERY);
        File queryFile = new File(queryLocation);

        if (!queryFile.canRead())
            throw new SessionException("Cannot access query file: "
                    + queryFile.getAbsolutePath(), Constants.NOTFOUND);

        //-------------------------
        //collect non-commented lines from file to list

        LineNumberReader lineReader;

        try {
            lineReader = new LineNumberReader(new FileReader(queryFile));
        } catch (FileNotFoundException fnfEx) {
            throw new SessionException("Cannot access query file: "
                    + queryFile.getAbsolutePath(), Constants.NOTFOUND);
        }

        List<String> lineList = new ArrayList<String>();
        String line;
        try {
            while ((line = lineReader.readLine()) != null)
            {
                if (line.startsWith("#"))
                    continue;
                else
                    lineList.add(line);
            }
        } catch (IOException ioEx) {
            throw new SessionException("Cannot load query file: "
                    + queryFile.getAbsolutePath() + "\nError message: "
                    + ioEx.getMessage(), Constants.IO_ERROR);
        }

        //-------------------------
        //build the query list

        QueryList query = new QueryList(lineList);

        //-------------------------

        return query;
    }

    //---------------------------------------------------------------------

    /**
     * Convenience method that instantiates new QueryClient using
     * the other utility methods to collect login info, service location,
     * and user query.
     * @return New instance of QueryClient
     * @throws SessionException if error occurs
     */

    public QueryClient getQueryClient() throws SessionException
    {
        QueryClient qClient = null;

        QLoginInfo loginInfo = getLoginInfo();
        String serviceUrl = getServiceLocation();
        QueryList queryList = getUserQuery();

        qClient = new QueryClient(serviceUrl, loginInfo, options);
        qClient.setUserQuery(queryList);

        return qClient;
    }

    //---------------------------------------------------------------------

    /**
     * Creates a map of filetype name to associated restart cache for
     * the list of filetypes.
     * @param filetypes Array of accessible filetype names
     * @param outputDirectory Output directory
     * @return Map of String to ClientRestartCache
     * @throws SessionException If restart directory does not exist and 
     *         cannot be created, or is request does not contain servergroup
     *         or command parameters.
     */

    public static Map<String, ClientRestartCache> loadRestartCaches(
            String serverGroup, String command, List<String> filetypes,
            String outputDirectory) throws SessionException
    {
        if (serverGroup == null)
            throw new SessionException("Server group not se defined",
                    Constants.MISSINGARG);
        if (command == null)
            throw new SessionException("Command not set", 
                                        Constants.MISSINGARG);

        Map<String, ClientRestartCache> map = 
                                  new Hashtable<String, ClientRestartCache>();

        //get registry location
        String registry = System.getProperty(Constants.PROPERTY_RESTART_DIR);
        if (registry == null)
            registry = System.getProperty("user.home") + File.separator
                    + Constants.RESTARTDIR;
        else
            registry = registry + File.separator + Constants.RESTARTDIR;
        File f = new File(registry);
        boolean ok = true;

        if (!f.exists())
            ok = f.mkdir();

        if (!ok)
            throw new SessionException("Unable to create restart directory \""
                    + registry + "\".", Constants.RESTARTFILEERR);

        //------------------

        for (String filetype : filetypes)
        {
            ClientRestartCache crc = ClientRestartCache.restoreFromCache(
                    serverGroup, filetype, null, command, "*", registry,
                    outputDirectory);

            if (crc == null)
                throw new SessionException("Could not restore cache for "
                        + FileType.toFullFiletype(serverGroup, filetype),
                        Constants.RESTARTFILEERR);

            map.put(filetype, crc);
        }

        return map;
    }

    //---------------------------------------------------------------------

    /**
     * Constructs a new internal query blob built from options map.
     * @param options Map containing values used to populate blob.
     *        These optional keywords are <code>UtilCmdParser.KEYWORD_FILES, 
     *        CMD.BEFORE, CMD.AFTER, CMD.BETWEEN & CMD.AFTER, CMD.FORMAT
     *        </code>.  
     * @throws SessionException If dates could not be parsed according to 
     *         format
     */

    public static QueryConstraints createSystemQuery(Map options)
                                           throws SessionException
    {
        QueryConstraints query = new QueryConstraints();

        //--------------------------

        //filename expression

        String[] expressions = (String[]) options
                .get(UtilCmdParser.KEYWORD_FILES);
        query.setFilenameExpressions(expressions);

        //--------------------------

        String before = (String) options.get(CMD.BEFORE);
        String after = (String) options.get(CMD.AFTER);
        String between = (String) options.get(CMD.BETWEEN);
        String and = (String) options.get(CMD.AND);
        String format = (String) options.get(CMD.FORMAT);

        DateTimeFormatter dateFormatter = new DateTimeFormatter(format);

        try {
            if (before != null)
            {
                query.setBeforeDate(dateFormatter.parseDate(before));
            }
            else if (after != null)
            {
                query.setAfterDate(dateFormatter.parseDate(after));
            }
            else if (between != null && and != null)
            {
                query.setBetweenDates(dateFormatter.parseDate(between),
                        dateFormatter.parseDate(and));
            }
        } catch (ParseException pEx) {
            throw new SessionException(pEx.getMessage(), Constants.EXCEPTION);
        }

        //--------------------------

        return query;
    }

    //---------------------------------------------------------------------

}
