/*
 * Created on Jul 17, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.services.query.client.QueryClient;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * This class acts as the middle-man between the query client and a 
 * filetype-based partitioned Result registry.  The run method
 * will collect results and add to registry, allowing client of this
 * class to request those results by filetype.
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
 * @version $Id: QueryResultsCollector.java,v 1.5 2007/09/26 00:06:50 ntt Exp $
 *
 */

public class QueryResultsCollector implements Runnable
{
    String _serverGroup;
    ResultMap _resultMap;
    QueryClient _queryClient;

    private Logger _logger = Logger.getLogger(
                                  QueryResultsCollector.class.getName());

    //---------------------------------------------------------------------

    /**
     * Constructor.
     * @param queryClient Instance of query client from which Results will
     * be collected.
     */

    public QueryResultsCollector(QueryClient queryClient)
    {
        this._resultMap = new ResultMap();
        this._queryClient = queryClient;
        this._serverGroup = this._queryClient.getServerGroup();

        setup();
    }

    //---------------------------------------------------------------------

    protected void setup()
    {
        //do we need to do anything?
    }

    //---------------------------------------------------------------------

    /**
     * Passes control to the collect and partition loop of this class.
     */

    public void run()
    {
        collectResults();
//System.out.println("DEBUG::QResultsCollector: END OF THREAD " + Thread.currentThread().getName());
    }

    //---------------------------------------------------------------------

    protected void collectResults()
    {
        if (this._queryClient == null)
            return;
        if (!this._queryClient.isQueryActive())
            return;

        String msg;
        boolean shouldExit;
        boolean alive = true;

        while (alive)
        {
            while (this._queryClient.getTransactionCount() > 0)
            {
//System.out.println("DEBUG::QResultsCollector:: Transaction count > 0");            	
                Result result = null;

                try {
                    result = this._queryClient.result();
                } catch (SessionException sesEx) {
                    sesEx.printStackTrace();
                }

                if (result == null)
                {
                    continue;
                } 
                else if (result.getErrno() == Constants.NO_FILES_MATCH)
                {
                    // no new files at this time
                    continue;
                } 
                else if (result.getErrno() == Constants.OK)
                {
                    String filetype = result.getType();
                    if (filetype != null)
                    {
                        synchronized (this)
                        {
                            this._resultMap.addResult(result);
                        }
                    }
                    continue;
                } 
                else if (result.getErrno() == Constants.IO_ERROR)
                {
                    msg = "FEI5 Information on "
                            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                            + "FEI::ERROR: "
                            + "Lost connection to query service.";

                    this._logger.error(msg);
                    break;
                } 
                else
                {
                    msg = "FEI5 Information on "
                            + DateTimeUtil.getCurrentDateCCSDSAString() + "\n"
                            + "FEI::ERROR: " + result.getMessage() + "\n";
                    this._logger.error(msg);
                    this._logger.debug("ERRNO = " + result.getErrno());
                    continue;
                }
            }
            alive = this._queryClient.isQueryActive();
        }

        this._logger.trace("Done collecting results");
    }

    //---------------------------------------------------------------------

    public QueryClient getClient()
    {
        return this._queryClient;
    }

    //---------------------------------------------------------------------

    public boolean isActive()
    {
        return (this._queryClient.isAlive() || isResultAvailable());
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    public boolean isResultAvailable()
    {
        return !this._resultMap.isEmpty();
    }

    //---------------------------------------------------------------------

    public List<String> getResultKeys()
    {
        return this._resultMap.getKeys();
    }

    //---------------------------------------------------------------------

    public List<Result> getResultsForFiletype(String filetype)
    {
        return this._resultMap.getResults(filetype);
    }

    //---------------------------------------------------------------------

    public List<Result> getAllResults()
    {
        return this._resultMap.getResults();
    }

    //---------------------------------------------------------------------

    public void remove(List<Result> results)
    {
        this._resultMap.removeResults(results);
    }

    //---------------------------------------------------------------------

    public void remove(Result result)
    {
        this._resultMap.removeResult(result);
    }

    //---------------------------------------------------------------------

    public void remove(String filetype, String filename)
    {
        Result result = this._resultMap.getResult(filetype, filename);
        if (result != null)
            this._resultMap.removeResult(result);
    }

    //---------------------------------------------------------------------

    public int getResultCount()
    {
        return this._resultMap.size();
    }

    //=====================================================================
    //=====================================================================

    class ResultMap
    {
        Map<String, List<Result>> map;
        Comparator resultComparator;

        //-----------------------------------------------------------------

        public ResultMap()
        {
            map = new Hashtable<String, List<Result>>();
            resultComparator = new ResultComparator();
        }

        //-----------------------------------------------------------------

        public List<String> getKeys()
        {
            List<String> list = new ArrayList<String>();
            synchronized (this)
            {
                list.addAll(this.map.keySet());
            }
            return list;
        }

        //-----------------------------------------------------------------

        public void addResult(Result result)
        {
            String filetype = result.getType();
            if (filetype != null)
            {
                synchronized (this)
                {
                    List<Result> list = this.map.get(filetype);
                    if (list == null)
                    {
                        list = new LinkedList<Result>();
                        this.map.put(filetype, list);
                    }
                    list.add(result);
                }
            }
        }

        //-----------------------------------------------------------------

        public void removeResults(List<Result> results)
        {
            for (Result result : results)
            {
                removeResult(result);
            }
        }

        //-----------------------------------------------------------------

        public void removeResult(Result result)
        {
            String filetype = result.getType();
            if (filetype != null)
            {
                synchronized (this)
                {
                    if (this.map.containsKey(filetype))
                    {
                        this.map.get(filetype).remove(result);
                    }
                }
            }
        }

        //-----------------------------------------------------------------

        public List<Result> getResults(String filetype)
        {
            List<Result> list = new Vector<Result>();
            synchronized (this)
            {
                if (this.map.containsKey(filetype))
                {
                    list.addAll(this.map.get(filetype));
                }
            }
            return list;
        }

        //-----------------------------------------------------------------

        public List<Result> getResults()
        {
            List<Result> list = new Vector<Result>();
            synchronized (this)
            {
                for (String filetype : this.map.keySet())
                {
                    list.addAll(getResults(filetype));

                }
            }

            //sort list according to mod times
            Collections.sort(list, this.resultComparator);

            return list;
        }

        //-----------------------------------------------------------------

        public Result getResult(String filetype, String filename)
        {
            List<Result> list;
            synchronized (this)
            {
                if (this.map.containsKey(filetype))
                {
                    list = this.map.get(filetype);
                    for (Result r : list)
                    {
                        if (r.getName().equals(filename))
                            return r;
                    }
                }
            }
            return null;
        }

        //-----------------------------------------------------------------

        public int size()
        {
            int size = 0;

            synchronized (this)
            {
                for (List list : this.map.values())
                {
                    size += list.size();
                }
            }

            return size;
        }

        //-----------------------------------------------------------------

        public boolean isEmpty()
        {
            boolean empty = true;

            synchronized (this)
            {
                Iterator<String> it = this.map.keySet().iterator();
                while (it.hasNext() && empty)
                {
                    if (!this.map.get(it.next()).isEmpty())
                        empty = false;
                }
            }

            return empty;
        }

        //-----------------------------------------------------------------
    }

    //=====================================================================
    //=====================================================================

    class ResultComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            if (o1 == null || o2 == null)
                return 0;
            if (!(o1 instanceof Result && o2 instanceof Result))
                return 0;
            Result r1 = (Result) o1;
            Result r2 = (Result) o2;
            Date d1 = r1.getFileModificationTime();
            Date d2 = r2.getFileModificationTime();
            if (d1 != null && d2 != null)
                return d1.compareTo(d2);
            else
                return 0;
        }

        public boolean equals(Object o)
        {
            return this == o;
        }
    }
    
    //=====================================================================
    //=====================================================================
}
