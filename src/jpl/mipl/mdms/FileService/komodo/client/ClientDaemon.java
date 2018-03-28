package jpl.mipl.mdms.FileService.komodo.client;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.util.LoginFile;



public class ClientDaemon
{
    // Overkill--current architecture has one daemon per server/filetype

    private static HashMap<String, Client> clientMap = null;
    private static String fileType;
    private static String logFileName;
    private static PrintStream logStream = null;
    private static String serverGroup;
    private static final int shutdownTime = 2; // shutdown hook delay time in
                                               // seconds
    public static void main(String[] args)
    {
        int sleepMS = 3000; // number of milliseconds to sleep between checks
        logFileName = args[3] + "/FeiPublishDaemon_" + args[1] + "." +
        args[2] + ".log";
        try
        {
            logStream = new PrintStream(logFileName);
        }
        catch (Throwable t)
        {
            logStream = null;
            log("Can't create log file");
        }

        log("FeiClientDaemon started");
        log(logFileName);
        // System.err.println (getTimeStamp()+" This is output to StdErr...");
        serverGroup = args[1]; // get the server group
        fileType = args[2]; // get the file type

        if (System.getenv("FEI5_PUBLISH_TIMER") != null)
        { // pick up value (in seconds) from environment
            sleepMS = new Integer(System.getenv("FEI5_PUBLISH_TIMER"));
            sleepMS *= 1000; // convert to milliseconds
        }

        Thread runtimeHookThread = new Thread() {
            public void run()
            {
                shutdownHook();
            }
        };

        Runtime.getRuntime().addShutdownHook(runtimeHookThread);

        clientMap = new HashMap<String, Client>();

        daemonize();

        try
        {
            while (true)
            {
                checkStaging(args[0] + '/' + serverGroup + '/' + fileType);
                Thread.sleep(sleepMS);
                // log ("running");
            }
        }
        catch (Throwable t)
        {

            log("Exception: " + t.toString());
        }
    }

    private static void shutdownHook()
    {
        log("Shutdown tasks started");
        long t0 = System.currentTimeMillis();
        while (true)
        {
            try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
                log("Exception: " + e.toString());
                break;
            }

            if (System.currentTimeMillis() - t0 > shutdownTime * 1000)
                break;

            log("shutdown");
        }

        removeClient(serverGroup, fileType);
        log("Shutdown Tasks completed");
        logStream.close();
    }

    private static void checkStaging(String stageDirName)
    {
        Client theClient;
        // int i, j, k;
        int k;
        File stageDir = new File(stageDirName);

        if (stageDir.isDirectory())
        { // legitimate directory, now check for contents
            // File[] serverDirs = stageDir.listFiles();
            // for (i = 0; i < serverDirs.length; i++)
            // { // go through the files in stage directory, looking for
            // serversubdirs
            // if (serverDirs[i].isDirectory())
            // {
            // File[] typeDirs = serverDirs[i].listFiles();
            // for (j = 0; j < typeDirs.length; j++)
            // {
            // if (typeDirs[j].isDirectory())
            // { // now finally check for files to be transferred

            String[] transFiles = stageDir.list();
            if (transFiles != null && transFiles.length > 0)
            {
                String prefix = stageDir.getAbsolutePath() + "/";

                // theClient = getClient(serverDirs[i].getName(),
                // typeDirs[j].getName());

                theClient = getClient(serverGroup, fileType);

                if (theClient == null)
                    // continue; // try again next time
                    return; // try again next time

                thisClient:
                for (k = 0; k < transFiles.length; k++)
                {
                    String replFileName = prefix + transFiles[k];
                    log(replFileName);
                    try
                    {
                        theClient.replace(replFileName, null);
                        while (theClient.getTransactionCount() > 0)
                        {
                            // Get results from result queue
                            Result result = theClient.getResult(60000);
                            if (result != null)
                                log(result.getMessage());
                            else
                            { // we may have timed out--force new connection
                                theClient.logout();
                                // removeClient(serverDirs[i].getName(),
                                // typeDirs[j].getName());
                                removeClient(serverGroup, fileType);
                                break thisClient;
                            }
                        }

                        File replFile = new File(replFileName);
                        replFile.delete();
                    }
                    catch (SessionException e)
                    {
                        log("Client replace error: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // }
                    // }
                    // }
                }
            }
        }
    }

    /**
     *
     * Allow program to keep running by closing stdout and stderr.
     */

    public static void daemonize()
    {
        System.out.close();
        System.err.close();
    }

    /**
     *
     * Get or create a client for this serverGroup, fileType pair.
     *
     * Call the client's login routine so it is ready to go.
     *
     * @param serverGroup
     *            the server group for the client
     *
     * @param fileType
     *            the file type for this request
     *
     * @return SimpleClient the simple client that is either created or
     *         retrieved
     */

    private static Client getClient(String serverGroup, String fileType)
    {
        String clientKey;
        String domainFileName;
        String loginCacheFileName;
        String password;
        // SimpleClient theClient;
        String userName;

        clientKey = serverGroup + ":" + fileType;
        if (clientMap.containsKey(clientKey))
            return clientMap.get(clientKey); // use existing client
        else
        { // create a new client, login, and save it in the map

            domainFileName = System.getenv("FEI5") + "/" + "domain.fei";
            // You'll find the trustore in $FEI5/mdms-fei.keystore
            String trustStore = System.getenv("FEI5") + "/mdms-fei.keystore";
            // theClient = new SimpleClient( domainFileName );
            Client client;
            try
            {
                client = new Client(domainFileName, trustStore);
            }
            catch (SessionException e1)
            {
                log("Client creation error:  " + e1.getMessage());
                e1.printStackTrace();
                return null;
            }

            loginCacheFileName = System.getenv("HOME") + "/.komodo/.komodologin";

            LoginFile loginCacheFile;
            try
            {
                loginCacheFile = new LoginFile(new File(loginCacheFileName));
            }
            catch (IOException e)
            {
                log("Login file creation exception: " + e.getMessage());
                e.printStackTrace();
                return null;
            }

            userName = loginCacheFile.getUsername(serverGroup, true);
            password = loginCacheFile.getPassword(serverGroup, true);

            try
            {
                // theClient.login(serverGroup, fileType, userName, password);
                // log ("username = " + userName);
                // log ("password = " + password);
                // theClient.login(serverGroup, fileType, userName, password);
                client.login(userName, password, serverGroup, fileType);
            }
            catch (SessionException e)
            {
                log("Login exception: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
            clientMap.put(clientKey, client);
            return client;
        }
    }

    private static String getTimeStamp()
    {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return f.format(new Date());
    }

    /**
     *
     * Log to stdout and log file.
     *
     * @param msg
     *            the log message
     */

    private static void log(String msg)
    {
        String datedMsg = getTimeStamp() + " " + msg;
        System.out.println(datedMsg);
        if (logStream != null)
            logStream.println(datedMsg);
    }

    /**
     *
     * Remove the client for this serverGroup, fileType pair, from the map.
     *
     * This forces a new client to be created next time.
     *
     * @param serverGroup
     *            the server group for the client
     *
     * @param fileType
     *            the file type for this request
     */

    private static void removeClient(String serverGroup, String fileType)
    {
        File dirFile;
        String clientKey;
        int i;
        String lockFileNameStart;
        File[] tmpFiles;
        String tmpFileName;
        clientKey = serverGroup + ":" + fileType;
        clientMap.remove(clientKey);
        lockFileNameStart = ".FeiDaemon." + serverGroup + "." + fileType;
        dirFile = new File("/tmp");

        tmpFiles = dirFile.listFiles();
        if (tmpFiles == null)
            tmpFiles = new File[0];

        for (i = 0; i < tmpFiles.length; i++)
        {
            tmpFileName = tmpFiles[i].getName();
            if (tmpFileName.startsWith(lockFileNameStart))
            {
                tmpFiles[i].delete(); // delete the lock file
                log("Deleted lock file " + tmpFileName);
            }
        }
    }

} // end class ClientDaemon


