/*
 * Created on Jul 11, 2007
 */
package jpl.mipl.mdms.FileService.komodo.services.query.api;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;

/**
 * <b>Purpose:</b>
 * Handles and manages the post-query blob modifiers such as file name 
 * expression, global date filter, and filetype/modification time pairs.
 * 
 * This data is usually populated from command line options such as file
 * expressions, before/after/between-and time constraints, and restart
 * files for filetype modification times. 
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
 * 07/11/2007        Nick             Initial release.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: QueryConstraints.java,v 1.7 2007/09/26 00:06:50 ntt Exp $
 *
 */

public class QueryConstraints
{
    public static final String FEI_MODTIME  = "FEI_MODIFICATION_TIME";
    public static final String FEI_FILENAME = "FEI_FILENAME";
    public static final String FEI_FILETYPE = "FEI_FILETYPE";
    
    protected String[] globalFilenameExpressions;
    protected Date[] globalDateFilter;    
    protected Map<String, FileTypeTimePair> filetypeList;

    //---------------------------------------------------------------------
    
    public QueryConstraints()
    {
        globalFilenameExpressions = new String[] {"*"};
        globalDateFilter = new Date[2];
        filetypeList = new Hashtable<String, FileTypeTimePair>();
    }
    
    //---------------------------------------------------------------------    
    //---------------------------------------------------------------------
    
    public void setFilenameExpression(String expression)
    {
        if (expression == null)
            this.setFilenameExpressions(null);  
        else
            this.setFilenameExpressions(new String[] {expression});
    }
    
    //---------------------------------------------------------------------
    
    public void setFilenameExpressions(String[] expressions)
    {
        if (expressions == null)
        {
            this.globalFilenameExpressions = new String[] {"*"};
        }
        else
        {
            this.globalFilenameExpressions = new String[expressions.length];
            for (int i = 0; i < expressions.length; ++i)
                this.globalFilenameExpressions[i] = expressions[i];
        }
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    /**
     * Sets the before date filter.  Results will have a modification time
     * less than date parameter
     * @param date Maximum date value
     */
    
    public void setBeforeDate(Date date)
    {
        setGlobalTime(null, date);
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the after date filter.  Results will have a modification time
     * greater than date parameter
     * @param date Minimum date value
     */
    
    public void setAfterDate(Date date)
    {
        setGlobalTime(date, null);        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the range date filter.  Results will have a modification time
     * that falls between the two parameters.
     * @param dateMin Minimum date value
     * @param dateMax Maximum date value
     */
    
    public void setBetweenDates(Date dateMin, Date dateMax)
    {
        setGlobalTime(dateMin, dateMax);
    }

    //---------------------------------------------------------------------
    
    /**
     * Resets date filters.
     */
    
    public void resetDates()
    {
        setGlobalTime(null, null);
    }
    
    //---------------------------------------------------------------------
    
    protected void setGlobalTime(Date date1, Date date2)
    {
        this.globalDateFilter[0] = date1;
        this.globalDateFilter[1] = date2;        
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    /**
     * Adds a new filetype filter with no minimum modification time value
     * @param filetype Full filetype {severgroup:filetype} to add
     */
    
    public void addFiletype(String filetype)
    {
        FileTypeTimePair pair = new FileTypeTimePair(filetype);
        this.filetypeList.put(pair.getName(), pair);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the modification time associated with a filetype filter.  If no
     * filter exists, then it is created and added. 
     * @param filetype Full filetype {severgroup:filetype} 
     * @param time Time filter, in epoch milliseconds 
     */
    
    public void setFiletypeTime(String filetype, Long time)
    {
        FileTypeTimePair pair = this.filetypeList.get(filetype);
        
        if (pair != null)
        {
            pair.setTime(time);
        }
        else
        {
            pair = new FileTypeTimePair(filetype, time);
            this.filetypeList.put(pair.getName(), pair);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the modification time associated with a filetype filter.  
     * @param filetype Full filetype {severgroup:filetype} 
     * return Time associated with filetype, possibly null 
     */
    
    public Long getFiletypeTime(String filetype)
    {
        FileTypeTimePair pair = this.filetypeList.get(filetype);
        return (pair == null) ? null : pair.getTime();
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    /**
     * Returns a SQL-formatted string representation of the contents
     * of this constraints instance.
     * @return SQL-formatted constraints
     */
    
    public String toSqlString()
    {
        StringBuffer buffer = new StringBuffer();
        List<String> andList = new ArrayList<String>();
        
        //-------------------------
        
        //global file expression
        if (this.globalFilenameExpressions != null)
        {
            if (this.globalFilenameExpressions.length == 1)
            {
                String sqlFileExpr = sqlize(this.globalFilenameExpressions[0]);
                andList.add("( "+ FEI_FILENAME +" LIKE '" + sqlFileExpr + "' )");    
            }
            else
            {       
                
                List<String> orList = new ArrayList<String>();                
                String entry;                
                for (String expr : this.globalFilenameExpressions)
                {
                    expr = sqlize(expr);
                    entry = "( "+ FEI_FILENAME +" LIKE '" + expr + "' )";
                    orList.add(entry);                     
                }    
                
                //---------------------
                
                if (!orList.isEmpty())
                {
                    StringBuffer ftBuffer = new StringBuffer();
                    ftBuffer.append("( ");
                    for (String orEntry : orList)
                    {
                        ftBuffer.append(orEntry).append(" OR ");
                    }
                    //remove last ' OR '
                    int len = ftBuffer.length();
                    ftBuffer.delete(len - 4, len);
                    
                    ftBuffer.append(" )");
                    
                    andList.add(ftBuffer.toString());
                }        
            }
        }
        
        //-------------------------
        
        //global time filter
        if (this.globalDateFilter[0] != null || this.globalDateFilter[1] != null)
        {
            String entry;
            if (this.globalDateFilter[0] != null && 
                this.globalDateFilter[1] != null)
            {
                entry = "( ( " + FEI_MODTIME +" >= " + 
                            this.globalDateFilter[0].getTime() + " ) AND ( " +
                            FEI_MODTIME + " <= " +
                            this.globalDateFilter[1].getTime() + " ) )";
            }
            else if (this.globalDateFilter[0] != null)
            {
                entry = "( " + FEI_MODTIME +" > " + 
                            this.globalDateFilter[0].getTime() + " )";
            }
            else
            {
                entry = "( " + FEI_MODTIME +" < " + 
                            this.globalDateFilter[1].getTime() + " )";
            }
            andList.add(entry);
        }
        
        //-------------------------
        
        //filetypes and possible mod times
        if (!this.filetypeList.isEmpty())
        {
            List<String> orList = new ArrayList<String>();
            
            Iterator<String> it = this.filetypeList.keySet().iterator();
            while (it.hasNext())
            {
                String filetype = it.next();
                FileTypeTimePair pair = this.filetypeList.get(filetype);
            
                String entry;
                if (pair.getTime() != null)
                {
                    entry = "( ( " + FEI_FILETYPE + " = '" + filetype + 
                                 "' ) AND ( " + FEI_MODTIME +" > " + 
                                 pair.getTime() + " ) )";
                }
                else
                {
                    entry = "( " + FEI_FILETYPE +" = '" + filetype + "' )";                    
                }
                orList.add(entry);                    
            }
            
            //---------------------
            
            if (!orList.isEmpty())
            {
                StringBuffer ftBuffer = new StringBuffer();
                ftBuffer.append("( ");
                for (String orEntry : orList)
                {
                    ftBuffer.append(orEntry).append(" OR ");
                }
                //remove last ' OR '
                int len = ftBuffer.length();
                ftBuffer.delete(len - 4, len);
                
                ftBuffer.append(" )");
                
                andList.add(ftBuffer.toString());
            }            
        }

        //-------------------------
        
        if (!andList.isEmpty())
        {
            buffer.append(" ( ");
            for (String andEntry : andList)
            {
                buffer.append(andEntry).append(" AND ");
            }
            //remove last ' AND '
            int len = buffer.length();
            buffer.delete(len - 5, len);
            
            buffer.append(" ) ");
        } 
        
        
        //-------------------------
        
        return buffer.toString();
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    protected String sqlize(String text)
    {
        text = text.replaceAll("\\*", "%");
        return text;
    }
    
    
    //---------------------------------------------------------------------
    
    public void sanityCheck() throws SessionException
    {
    	Long maxTime = (this.globalDateFilter[1] == null) ? null :
                       this.globalDateFilter[1].getTime();
    	
    	if (maxTime != null)
    	{
	        for (FileTypeTimePair pair : this.filetypeList.values())
	        {
	            Long modTime = pair.getTime();
	            if (modTime == null)
	                continue;
	            
	            if (modTime > maxTime)
	            {
	                throw new SessionException("Modification time for '"+
	                        pair.getName() + "' is greater than global max" +
	                        "time", Constants.DATE_RANGE);
	            }
	        }
    	}
    }

    //---------------------------------------------------------------------
    
    public static QueryConstraints parse(String entry) throws ParseException
    {
        QueryConstraints constraints = new QueryConstraints();
        
        if (!entry.startsWith("[") || !entry.endsWith("]"))
            throw new ParseException("entry must be enclosed with brackets", 0);            
        
        entry = entry.substring(1, entry.length()-1);
        
        String[] entries = entry.split(";");
        if (entries.length < 2)
            throw new ParseException("entry missing a section", 0); 
        
        String exprStr = entries[0];
        String dateStr = entries[1];
        String typeStr = (entries.length == 3) ? typeStr = entries[2] : null;
        
        try {
            //filename exprs
            String[] exprs = exprStr.split(",");
            if (exprs != null && exprs.length > 0)
                constraints.setFilenameExpressions(exprs);
            
            //dates            
            String[] dates = dateStr.split(",");
            if (dates != null && dates.length > 0)
            {
	            long time1 = -1, time2 = -1;
	            Date date1, date2;
	            if (dates.length == 1)
	            {
	            	if (dateStr.startsWith(","))
	            		time2 = Long.parseLong(dates[0]);
	            	else
	            		time1 = Long.parseLong(dates[0]);
	            }
	            else
	            {	            
	            	time1 = Long.parseLong(dates[0]);
	            	time2 = Long.parseLong(dates[1]);
	            }
	            date1 = (time1 == -1) ? null : new Date(time1);
	            date2 = (time2 == -1) ? null : new Date(time2);	            
	            constraints.setGlobalTime(date1, date2);
            }
            
            //filetype strs
            if (typeStr != null)
            {
              String[] types = typeStr.split(",");
              if (types != null && types.length > 0)
              {
                for (String typeEntry : types)
                {
                    String[] parts = typeEntry.split("=");
                    if (parts != null)
                    {
                    	if (parts.length == 2)                    
                    	{
                    		constraints.setFiletypeTime(parts[0], 
                                       Long.parseLong(parts[1]));
                    	}
                    	else if (parts.length == 1)
                    	{
                    		constraints.setFiletypeTime(parts[0], 
                                    					   null);
                    	}
                    }
                }
              }
            }
            
        } catch (Exception ex) {
        	ex.printStackTrace();
            throw new ParseException("Could not parse entry: " + entry +
            		                 "\nReason: "+ex.getMessage(), 0); 
        }
        
        return constraints;
    }
    
    //---------------------------------------------------------------------
    
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        
        //[name;date1;date2;ft1=time1,ft2=time2]
        buffer.append("[");
        
        //filename expressions
        if (this.globalFilenameExpressions != null && 
                this.globalFilenameExpressions.length > 0)
        {
            for (String expr : this.globalFilenameExpressions)
                buffer.append(expr).append(",");
            int length = buffer.length();
            buffer.delete(length - 1, length);
        }
        buffer.append(";");
        
        //global dates
        if (this.globalDateFilter != null &&
                this.globalDateFilter.length == 2)
        {
            if (this.globalDateFilter[0] != null)
                buffer.append(this.globalDateFilter[0].getTime());
            buffer.append(",");
            if (this.globalDateFilter[1] != null)
                buffer.append(this.globalDateFilter[1].getTime());            
        }
        buffer.append(";");
        
        //filetype modification times
        if (this.filetypeList != null && !this.filetypeList.isEmpty())
        {
            Iterator<String> it = this.filetypeList.keySet().iterator();
            while (it.hasNext())
            {
                String type = it.next();
                FileTypeTimePair pair = this.filetypeList.get(type);
                buffer.append(pair.getName()).append("=");
                if (pair.getTime() != null)
                	buffer.append(pair.getTime());
                buffer.append(",");
            }
            int length = buffer.length();
            buffer.delete(length - 1, length);
        }
        
        
        buffer.append("]");
        
        return buffer.toString();        
    }
    
    //---------------------------------------------------------------------
       
    class FileTypeTimePair
    {
        protected String filetype;
        protected Long modTime;
        public FileTypeTimePair(String filetype)
        {
            this(filetype, null);
        }
        public FileTypeTimePair(String filetype, Long date)
        {
            this.filetype = filetype;
            this.modTime = date;
        }
        public void setTime(long date)
        {
            this.modTime = date;
        }
        public Long getTime()
        {
            return this.modTime;
        }
        public String getName()
        {
            return this.filetype;
        }
    }

    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        QueryConstraints blob = new QueryConstraints();        
        Date date = new Date();
        
        //0
        System.out.println(blob.toString());
        
        //1
        blob.setAfterDate(date);
        System.out.println(blob.toString());
        
        //2
        blob.setFilenameExpression("*.EFF");
        blob.setBetweenDates(date, new Date());
        blob.setFiletypeTime("sg:type1", System.currentTimeMillis());
        blob.setFiletypeTime("sg:type2", System.currentTimeMillis()-1000424);
        blob.setFiletypeTime("sg:type3", System.currentTimeMillis()+1000424);
        System.out.println(blob.toString());     
        
        //3
        String[] fileexprs = new String[] { "*.EFF" , "*.FFL" };
        blob.setFilenameExpressions(fileexprs);
        blob.setBetweenDates(date, new Date());
        blob.setFiletypeTime("sg:type1", System.currentTimeMillis());
        blob.setFiletypeTime("sg:type2", System.currentTimeMillis()-1000424);
        blob.setFiletypeTime("sg:type3", System.currentTimeMillis()+1000424);
        System.out.println(blob.toString()); 
                
        //4               
        blob.setFiletypeTime("sg:type4", null);
        System.out.println(blob.toString()); 
        
        
        System.out.println(blob.toSqlString());
        String blobStr = blob.toString();
        QueryConstraints blobRecovered = null;
        try {
            blobRecovered = QueryConstraints.parse(blobStr);
        } catch (ParseException pEx) {
            pEx.printStackTrace();
        }
        if (blobRecovered != null)
            System.out.println(blobRecovered.toSqlString());   
        
        blob = new QueryConstraints();
        System.out.println(blob.toSqlString());
        blobStr = blob.toString();
        blobRecovered = null;
        try {
            blobRecovered = QueryConstraints.parse(blobStr);
        } catch (ParseException pEx) {
            pEx.printStackTrace();
        }
        if (blobRecovered != null)
            System.out.println(blobRecovered.toSqlString());
    }
    

}
