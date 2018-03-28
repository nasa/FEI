/*
 * Created on Jun 25, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.api;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * <b>Purpose:</b> 
 * Data structure that holds the user meta-data query.
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
 * 08/08/2007        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QueryList.java,v 1.4 2007/09/26 00:06:50 ntt Exp $
 *
 */

public class QueryList
{
    protected List<String> queryList;
    //protected Map<String,String> queryKeyMap;
    
    //---------------------------------------------------------------------
    
    /** 
     * Default constructor
     */
    
    public QueryList()
    {
        this.queryList = new ArrayList<String>();
        //this.queryKeyMap = new Hashtable<String,String>();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param list List of query filter entries 
     */
    
    public QueryList(List<String> list)
    {
        this();
        for (String filter: list)
            addFilter(filter);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Copy constructor
     * @param original Source QueryList instance 
     */
    
    public QueryList(QueryList original)
    {
        this(original.toList());
    }
    
    //---------------------------------------------------------------------
    
    public static QueryList parse(String queryBlob) throws ParseException
    {
        QueryList qList = new QueryList();
        
        StringTokenizer st = new StringTokenizer(queryBlob, "(", false);
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            token = token.replace(")", "").trim();     
            qList.addFilter(token);
        }
        
        return qList;
    }

    //---------------------------------------------------------------------
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        synchronized(this)
        {
            //Iterator<String> it = this.queryKeyMap.values().iterator();
            Iterator<String> it = this.queryList.iterator();
            while(it.hasNext())
            {
                String filter = it.next();
                buffer.append("(").append(filter).append(") ");
            }
        }
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
    
    public boolean addFilter(String filter) 
    {
        boolean success = false;
        filter = filter.trim();
        String key = extractKeyword(filter);
        
        if (key != null)
        {
            synchronized(this)
            {
                //if (!this.queryKeyMap.containsKey(key))
                //{
                //    this.queryKeyMap.put(key, filter);
                //    success = true;
                //}
                if (!this.queryList.contains(filter))
                {
                    this.queryList.add(filter);
                    success = true;
                }
            }
        }
        return success;
    }
    
    //---------------------------------------------------------------------
    
//    public boolean replaceFilter(String filter) 
//    {
//        boolean success = false;
//        filter = filter.trim();
//        String key = extractKeyword(filter);
//        
//        if (key != null)
//        {
//            synchronized(this)
//            {
//                if (!this.queryKeyMap.containsKey(key))
//                {
//                    this.queryKeyMap.put(key, filter);
//                    success = true;
//                }
//            }
//        }
//        
//        return success;
//    }
    
    //---------------------------------------------------------------------
    
    protected String extractKeyword(String filter)
    {
        filter = filter.trim();
        String keyword = null;
        
        int index = filter.indexOf(" ");
        if (index != -1)
        {
            keyword = filter.substring(0, index);
        }
        
        return keyword;
    }
    
    //---------------------------------------------------------------------
    
    public List<String> toList()
    {
        List<String> copyList = new ArrayList<String>();
        copyList.addAll(this.queryList);
//        synchronized(this)
//        {
//            copyList.addAll(this.queryKeyMap.values());
//        }
        
        return copyList;
    }
    
    //---------------------------------------------------------------------
    
}
