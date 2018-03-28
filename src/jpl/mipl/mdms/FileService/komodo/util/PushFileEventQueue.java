package jpl.mipl.mdms.FileService.komodo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 ** <b>Purpose:</b>
 * Implementation of a state-based queue structure with two states, 
 * 'new' and 'in-process'.  
 * 
 * When a file is added, it is set to 'new'.  Caller must perform 
 * advanceQueue() to move elements from 'new' state to 'in-process' 
 * state, and then call getFilesInProcess() to retrieve an array of 
 * those files.  
 * 
 * As each file is processed successfully, caller should perform a 
 * removeFile(entry).
 * 
 * Ordering in terms of sequence the items are added will be maintained.
 * 
 *   <PRE>
 *   Copyright 2008, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2008.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 07/14/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: PushFileEventQueue.java,v 1.2 2008/07/30 00:46:28 ntt Exp $
 * 
 */

public class PushFileEventQueue<E extends Object>
{
    protected final List<E> _newFilesList;
    protected final List<E> _pendingFilesList;
    protected final Object _lock = new Object();
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     */
    
    public PushFileEventQueue()
    {
        this._newFilesList = new Vector<E>();
        this._pendingFilesList = new Vector<E>();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Add a new entry to the queue.  If entry already exists with state
     * 'new', then duplicate will be added.  However, if an entry exist
     * with state 'in-process', then the new entry will be added. Check
     * the return value to see if entry was added.
     * @param entry New entry to be added
     * @return True if added, false otherwise
     */
    
    public boolean addItem(E entry)
    {
        boolean success = false;        
        
        synchronized(this._lock)
        {
            if (!this._newFilesList.contains(entry))
                success = this._newFilesList.add(entry);
        }        
        return success;        
    }
    
    //---------------------------------------------------------------------
   
    /**
     * Removes the first occurrence in this queue of item with the specified 
     * name. If this queue does not contain the item, it is unchanged.
     * The 'in-process' items will be examined first, and followed by
     * the 'new' items. 
     * @param entry Item to be removed
     * @return True if an item was removed, false otherwise
     */
    
    public boolean removeItem(E entry)
    {
        boolean success = false;        
        
        synchronized(this._lock)
        {
            if (this._pendingFilesList.contains(entry))
                success = this._pendingFilesList.remove(entry);
            else if (this._newFilesList.contains(entry))
                success = this._newFilesList.remove(entry);            
        }        
        return success;     
    }
    
    //---------------------------------------------------------------------

    /**
     * Returns true if and only if there are no entries in this
     * queue, regardless of state.  Returns false otherwise.
     * @return True if empty, false otherwise
     */
    
    public boolean isEmpty()
    {
        boolean empty = false;
        synchronized (this._lock)
        {
            empty = this._newFilesList.isEmpty() && this._pendingFilesList.isEmpty();
        }
        return empty;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Advances to next state of queue, moving 'new' files to the
     * 'in-process' state.  After calling this, there will be no
     * 'new' files until they are added.  'In-process' files will
     * stay in that state.
     */
    
    public void advanceQueue()
    {
        synchronized(this._lock)
        {
            //this._pendingFilesList.clear();
            for (E entry : this._newFilesList)
            {
                if (!this._pendingFilesList.contains(entry))
                    this._pendingFilesList.add(entry);
            }
            this._newFilesList.clear();
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Retrieve an array of file names in 'in-process' state.
     * @return Array of 'in-process' files
     */
    
    public List<E> getItemsInProcess()
    {        
        List<E> items = new ArrayList<E>();
        
        synchronized(this._lock)
        {
            items.addAll(this._pendingFilesList);            
        } 
        return items;        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * For testing purposes 
     */
    public static void main(String[] args)
    {
        final PushFileEventQueue<String> q = 
                                new PushFileEventQueue<String>();
        
        Thread t1 = new Thread(new Runnable() { 
            public void run() {
                
                int i = 0;
                while (true)
                {
                    i++;
                    String name = "File "+i;
                    q.addItem(name);
                    System.out.println(Thread.currentThread().getName()+": added: "+name);
                    
                    try { Thread.sleep((i%3)*1000); } catch(Exception e) {}
                    if (i % 10 == 0)
                        try { Thread.sleep(10*1000); } catch(Exception e) {}
                    else if (i % 7 == 0)
                    {
                        name = "File "+(i-1);
                        
                        boolean added = q.addItem(name);
                        if (added)
                            System.out.println(Thread.currentThread().getName()+": added: "+name);
                        else
                            System.out.println(Thread.currentThread().getName()+": cant add: "+name);
                    }
                }
                
        }});
        t1.setName("THREAD_PRODUCER");
        
        Thread t2 = new Thread(new Runnable() { 
            public void run() {
                while (true)
                {
                    q.advanceQueue();
                    List<String> itemList = q.getItemsInProcess();
                    String[] items = new String[0];
                    items = itemList.toArray(items);
                    
                    if (items.length == 0)
                    {
                        System.out.println(Thread.currentThread().getName()+": Nothing to process");
                        try { Thread.sleep(3*1000); } catch(Exception e) {}
                    }
                    else
                    {
                        for (int i = 0; i < items.length; ++i)
                        {
                            String name = items[i];
                            System.out.println(Thread.currentThread().getName()+": processing: "+name);
                            try { Thread.sleep(((i+1)%3)*1000); } catch(Exception e) {}
                            q.removeItem(name);                        
                        }
                    }
                }                
        }});
        t2.setName("THREAD_CONSUMER");     
        
        t2.start();
        t1.start();
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
}
