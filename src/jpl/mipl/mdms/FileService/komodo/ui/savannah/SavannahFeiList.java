package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahFileList.FileListToolbar;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahFilterModel.SavannahListFilterListener;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.dnd.SavannahTableTransferHandler;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 *  FEI File list for the Savannah application.
 *
 *   <PRE>
 *   Copyright 2004, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2004.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who                        What</B>
 * ----------------------------------------------------------------------------
 * 06/04/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole	(Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahFeiList.java,v 1.44 2013/03/30 00:06:21 ntt Exp $
 *
 */
 
public class SavannahFeiList extends JPanel implements SavannahList, 
                                            PropertyChangeListener, 
                                            SavannahListFilterListener
{
    private final String __classname = "SavannahFeiList";
    private Logger _logger = Logger.getLogger(SavannahFeiList.class.getName());
    
    protected String _currentFeiType;
    protected String _currentFeiGroup;
    protected String _parentDirectoryString = "[..]";
    protected java.util.List _showAfterCache;
    
    protected SavannahModel _model;
    protected JTable _fileTable;
    protected SavannahTableModel _tableModel;
    protected JLabel _noConnLabel;
    protected JScrollPane _scrollPane;
    protected FeiListToolbar _toolbar;
    protected SavannahTableCellRenderer _tableCellRenderer;
    protected SavannahTableTransferHandler _transferHandler; 
    protected String _transferIdentifier;
    protected MouseListener _mouseListener;
    
    public static final int ACT_NONE             = 0;
    public static final int ACT_REFRESH          = 1;
    public static final int ACT_CHANGE_DIRECTORY = 2;
    public static final int ACT_RENAME           = 4;
    public static final int ACT_DELETE           = 5;
    public static final int ACT_COMMENT          = 6;
//    public static final int ACT_LOCK             = 7;
//    public static final int ACT_UNLOCK           = 8;
         
    protected ImageIcon _refreshIcon, _refreshRollIcon;
    protected ImageIcon _cdIcon, _cdRollIcon;
    protected ImageIcon _renameIcon, _renameRollIcon;
    protected ImageIcon _deleteIcon, _deleteRollIcon;
    protected ImageIcon _commentIcon, _commentRollIcon;
//    protected ImageIcon _lockTypeIcon, _lockTypeRollIcon;
//    protected ImageIcon _unlockTypeIcon, _unlockTypeRollIcon;
    
    protected int _dndAction = DnDConstants.ACTION_COPY;   
    
    //---------------------------------------------------------------------
    
    /**
     *  Default constructor.  Uses default initial directory by searching
     *  for a valid directory in the following order: user.dir, user.home, 
     *  first root directory.  If none of these satisfy, an IOException 
     *  is thrown.
     *  @throws IOException if problem occurs with selected directory
     */
     
    public SavannahFeiList(SavannahModel model)  throws IOException
    {              
        init(model);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Initializes instance of object.  Builds list and sets
     *  initial directory.
     *  @throws IOException if error occurs with directory
     */
     
    protected void init(SavannahModel model)  throws IOException
    {                
        this._model = model;
        this._model.addPropertyChangeListener(this);
        this._model.getFilterModel().addFilterListener(this);
        this._showAfterCache = new Vector();
       
        this._tableModel = new SortableSavannahTableModel();
        this._fileTable  = new SavannahTable(this._model, this._tableModel);
        this._transferIdentifier = __classname + "::_fileTable::" + 
                                        this._fileTable.toString();
        this._fileTable.setName(_transferIdentifier);
        
        //selection and mouse attributes
        this._mouseListener = new TableMouseListener();
        this._fileTable.addMouseListener(this._mouseListener);
        
        //setup drag and drop
        this._fileTable.setDragEnabled(true);
        this._transferHandler = new SavannahTableTransferHandler(_model,this);
        this._fileTable.setTransferHandler(this._transferHandler);        
        this.setTransferHandler(this._transferHandler);    
        
        //init layout and initial component
        this.setLayout(new BorderLayout());
        _noConnLabel = new JLabel("Not connected to FEI server");
        _noConnLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        //build scrollpane
        this._scrollPane = new JScrollPane(_fileTable);
        this._scrollPane.setBorder(null);
        this._scrollPane.getViewport().setBackground(
                                this._fileTable.getBackground());
        this.add(_noConnLabel, BorderLayout.CENTER);
        
        //allow scrollpane to be droppable target
        this._scrollPane.setTransferHandler(this._transferHandler);
        this._scrollPane.setName(this._transferIdentifier);
        this._scrollPane.addMouseListener(this._mouseListener);

        initActionIcons(); 
        _toolbar = new FeiListToolbar(_model);
        _fileTable.getSelectionModel().addListSelectionListener(_toolbar);
        this.add(_toolbar, BorderLayout.NORTH);        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Adds a listener to the list that's notified each time a change 
     * to the selection occurs.
     * @param listener the ListSelectionListener to add
     */
       
    public void addListSelectionListener(ListSelectionListener listener) 
    {
        if (listener != null)
            this._fileTable.getSelectionModel().
                                        addListSelectionListener(listener);        
    }    
       
    //---------------------------------------------------------------------
       
    /**
     * Removes a listener from the list that's notified each time a change 
     * to the selection occurs. 
     * @param listener the ListSelectionListener to remove
     */
       
    public void removeListSelectionListener(ListSelectionListener listener) 
    {
        if (listener != null)
            this._fileTable.getSelectionModel().
                                        removeListSelectionListener(listener);          
    }
              
    //---------------------------------------------------------------------
    
    protected void initActionIcons()
    {
        URL imageURL;
        
        //refresh
        imageURL = FileListToolbar.class.getResource(
                                         "resources/refresh24.gif");            
        _refreshIcon = new ImageIcon(imageURL, "Refresh");
        imageURL = FileListToolbar.class.getResource(
                                         "resources/refresh24_ro.gif");            
        _refreshRollIcon = new ImageIcon(imageURL, "Refresh");
        
        //cd (change filetype)
        imageURL = FileListToolbar.class.getResource(
                                         "resources/chftype24.gif");            
        _cdIcon = new ImageIcon(imageURL, "Change Filetype");

        imageURL = FileListToolbar.class.getResource(
                                         "resources/chftype24_ro.gif");            
        _cdRollIcon = new ImageIcon(imageURL, "Change Filetype");
        
        //rename
        imageURL = FileListToolbar.class.getResource(
                                         "resources/rename24.gif");            
        _renameIcon = new ImageIcon(imageURL, "Rename");
        imageURL = FileListToolbar.class.getResource(
                                         "resources/rename24_ro.gif");            
        _renameRollIcon = new ImageIcon(imageURL, "Rename");
        
        //delete
        imageURL = FileListToolbar.class.getResource(
                                         "resources/delete24.gif");            
        _deleteIcon = new ImageIcon(imageURL, "Delete");
        imageURL = FileListToolbar.class.getResource(
                                         "resources/delete24_ro.gif"); 
        _deleteRollIcon = new ImageIcon(imageURL, "Delete");  
        
        //comment
        imageURL = FileListToolbar.class.getResource(
                                        "resources/comment24.gif");            
        _commentIcon = new ImageIcon(imageURL, "Comment");
        imageURL = FileListToolbar.class.getResource(
                                        "resources/comment24_ro.gif"); 
        _commentRollIcon = new ImageIcon(imageURL, "Comment"); 
        
//        //lock
//        imageURL = FileListToolbar.class.getResource(
//                                        "resources/locktype24.gif");            
//        _lockTypeIcon = new ImageIcon(imageURL, "Lock type");
//        imageURL = FileListToolbar.class.getResource(
//                                        "resources/locktype24_ro.gif"); 
//        _lockTypeRollIcon = new ImageIcon(imageURL, "Lock type"); 
//        
//        //unlock
//        imageURL = FileListToolbar.class.getResource(
//                                        "resources/unlocktype24.gif");            
//        _unlockTypeIcon = new ImageIcon(imageURL, "Unlock type");
//        imageURL = FileListToolbar.class.getResource(
//                                        "resources/unlocktype24_ro.gif"); 
//        _unlockTypeRollIcon = new ImageIcon(imageURL, "Unlock type"); 
    }
    
    //---------------------------------------------------------------------
    
    public boolean inFileTypeDirectory()
    {
        return (this._currentFeiType != null);
    }       
    
    //---------------------------------------------------------------------
    
    /** 
     *  Method called in response to having established a new
     *  (possibly null) connection to an FEI server.  
     *  If parameter is different from current, resets current 
     *  file type and displays server file types in list, if any.
     *  @param group String identifier of FEI server group
     */
     
    public void setCurrentFeiGroup(String group)
    {                       
        //return if both null or equal
        if (this._currentFeiGroup == null && group == null)
            return;
        if (this._currentFeiGroup != null &&
                    this._currentFeiGroup.equals(group))
            return;
        
        //reset server as null
        if (group == null)
        {           
           this._model.setStatusMessage("Disconnecting from FEI server group...");
           _currentFeiGroup = group; 
		   changeFileType(null);    
           this.remove(_scrollPane);
           this.add(_noConnLabel, BorderLayout.CENTER);
           this._model.setStatusMessage("Disconnected from FEI server group.");
           this.invalidate();
           this.repaint();
        }
        else //dispose of old data, get new data
        { 			
            this._model.setStatusMessage("Connecting to "+group+" server group...");
            _currentFeiGroup = group; 
			changeFileType(null);
			this.remove(_noConnLabel);
			this.add(_scrollPane, BorderLayout.CENTER);
            this._model.setStatusMessage("Connected to "+group+" server group.");
            this.invalidate();
            this.repaint();
        }                 
    }
    
    //---------------------------------------------------------------------
    
    /** 
     *  Returns the string identifier of the current FEI server,
     *  or null if none.
     *  @return Current FEI server name
     */
     
    public String getCurrentFeiServer()
    {
        return _currentFeiGroup;
    }
    
    //---------------------------------------------------------------------        
    
    /**
     *  Returns current FEI file type.
     */
     
    public String getCurrentFeiType()
    {
        return _currentFeiType;
    }

    //---------------------------------------------------------------------
    
    /**
     *  Sets the current FEI file type.  Refreshes listing according
     *  to new contents.  Note: current fei server MUST be set
     *  before setting type. If server is null and this method is
     *  called, an exception will be thrown.
     *  @param type The FEI file type of interest 
     *  @throws SessionException if error occurs with FEI client
     *  @throws IllegalArgumentException if server reference is null
     */
     
    public void setCurrentFeiType(String type) 
    {
        if (this._currentFeiGroup == null && type != null)
            throw new IllegalArgumentException(
                                "Cannot set FEI file type call "+
                                "without connection to FEI server");
        
        /*
        if (type == null && this._currentFeiType == null) 
            return;
        if (this._currentFeiType != null && _currentFeiType.equals(type))
            return;
        */
        
        this._currentFeiType = type;
        refreshListing();   
    }

    //---------------------------------------------------------------------
    
    /**
     *  Refreshes listing of current "directory" contents.
     */

    public void refresh()
    {            
        //this automatically calls refresh
        this._model.getReceivalModel().resetFei();
        //this.refreshListing();
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Refreshes the list contents of the current directory
     */
     
    protected void refreshListing()
    {             
        this._model.printDebug(__classname+"::refreshListing(): "+
                               "Entering...");
                
        java.util.List elements = new Vector();
        
        if (!inFileTypeDirectory())
        {
            this._model.printDebug(__classname+"::refreshListing(): "+
                                         "Getting FEI TYPES...");
            //get fei file type listing
            List types = this._model.getFeiTypes(); 
            this._model.printDebug(__classname+"::refreshListing(): "+
                                   "FEI TYPES size = "+types.size());
            
            //build list of elements from types
            int numTypes = types.size();
            for (int i = 0; i < numTypes; ++i)
            {
                elements.add(new SavannahFeiTableElement(
                                 this._currentFeiGroup,
                                 types.get(i).toString(),
                                 null,
                                 SavannahTableElement.SIZE_UNKNOWN,
                                 SavannahTableElement.DATE_UNKNOWN,
                                 null,
                                 false));
            }
        }
        else
        {
            this._model.printDebug(__classname+"::refreshListing(): "+
                    "Getting FEI FILES...");
            
            //get fei file type contents       
            List results = this._model.showFromFei();
            this._model.printDebug(__classname+"::refreshListing(): "+
                                   "FEI FILES size = "+results.size());
      
            Result result;
            SavannahTableElement element;
            int numResults = results.size();
            for (int i = 0; i < numResults; ++i)
            {
                result = (Result) results.get(i);
                try {
                    element = new SavannahFeiTableElement(
                                  this._currentFeiGroup,
                                  this._currentFeiType,
                                  result.getName(),
                                  result.getSize(),
                                  result.getFileModificationTime().getTime(),
                                  result.getComment(),
                                  false);
                    elements.add(element);
                    
                } catch (IllegalArgumentException iaEx) {
                    _logger.error(iaEx.getMessage(), iaEx);//iaEx.printStackTrace();
                    //TODO - handle error
                }
            }
            
            //insert 'parent dir' at top
            //TODO - handle parent directory string
            if (this._parentDirectoryString != null)
            {
                try {
                    element = new SavannahFeiTableElement(
                                  this._currentFeiGroup,
                                  this._currentFeiType,
                                  null,
                                  SavannahFeiTableElement.SIZE_UNKNOWN,
                                  SavannahFeiTableElement.DATE_UNKNOWN,
                                  null,
                                  true);
                    elements.add(0, element);
                } catch (IllegalArgumentException iaEx) {
                    _logger.error(iaEx.getMessage(), iaEx);//iaEx.printStackTrace();
                    //TODO - handle error
                }
            }
        }                             
        
        Object[] wereSelected = getSelectedElements();
        
        //clear entries
        _tableModel.clear();
        
        //add entries to list  
        _tableModel.addAll(elements);
        
        //select that which was once selected
        if (wereSelected != null && wereSelected.length > 0)
            selectItems(wereSelected);
        
        _fileTable.invalidate();
        _fileTable.repaint();
    }
    
    //---------------------------------------------------------------------
    
    protected void selectItems(Object[] items)
    {
        //try to select that which was previously selected
        if (items.length > 0)
        {
            List indexList = new ArrayList();
            for (int i = 0; i < items.length; ++i)
            {
                int index = _tableModel.indexOf(items[i]);
                if (index != -1)
                    indexList.add(new Integer(index));
            }
            int numIndices = indexList.size();
            if (numIndices > 0)
            {
                for (int i = 0; i < numIndices; ++i)
                {
                    int value = ((Integer)indexList.get(i)).intValue();
                    this._fileTable.addRowSelectionInterval(value, value);
                }
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  If entry is null or empty string, returns false.  Otherwise,
     *  if not in a FEI filetype, we assume all entries are filetype
     *  directories, and thus return true.  If in a FEI filetype, then
     *  the only directory is the parent directory, where we return true.
     *  Otherwise, return false.
     */
    
    protected boolean isDirectory(String entry)
    {
        if (entry == null || entry.equals(""))
            return false;
        else if (!inFileTypeDirectory())
            return true;
        else if (entry.equals(_parentDirectoryString))
            return true;
        else
            return false;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns all selected files from the FEI filetype file listing,  
     *  excluding parent directory.
     *  @return Array of files selected in listing
     */
     
    public String[] getSelectedFiles()
    {        
        Object[] values = getSelectedValues();
        String[] fileArray = new String[values.length];
        SavannahFileTableElement element;
        for (int i = 0; i < values.length; ++i)
        {
            fileArray[i] = values[i].toString();
        }
        return fileArray;
    }  
    
    //---------------------------------------------------------------------
    
    /**
     *  Copy file from current local direcotry to FEI filetype directory.
     *  @param filepaths Name of the file to copy
     *  @return True if trasfer was successful, false otherwise
     */
     
    protected boolean transferFiles(String[] filepaths) 
    {		        
        if (filepaths == null || filepaths.length ==0)
            return false;        
        
        for (int i = 0; i < filepaths.length; ++i)
        {
            String filepath = filepaths[i];
            File file = new File(filepath);
            if (!file.canRead())
            {
                JOptionPane.showMessageDialog(this,
                    "Cannot transfer '"+filepath+"'\nFile does "+
                    "not exist or cannot be read.\n\nAborting transfer...",
                    "File Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }            
        }
                            
        this._model.addToFei(filepaths);
                
        return true;
	}
    
    //---------------------------------------------------------------------
    
    /**
     *  Instructs instance to import object.
     *  @param obj Object to be imported.
     *  @return True if object was imported, false otherwise.
     */ 
    
    public boolean importEntry(Object obj)
    {
        String[] filenames = toStringArray(obj);

        if (filenames == null)
            return false;
            
        return transferFiles(filenames);                
    }

    //---------------------------------------------------------------------
    
    /**
     *  Returns whether component can export object for transfer.
     *  @param obj Object to be exported.
     *  @return True if object can be exported, false otherwise.
     *  @throws SavannahListException if import disallowed with
     *          explanation contained in exception getMessage string
     */ 
    
    public boolean canExport(Object obj) throws SavannahListException
    {        
        String[] filenames = toStringArray(obj);
        if (filenames == null)
            return false;    
        
        for (int i = 0; i < filenames.length; ++i)
        {
            if (isDirectory(filenames[i]))
            {
                throw new SavannahListException(this,
                        "Cannot transfer item '"+filenames[i]+"'\n"+
                        "because filetypes are not transferrable.");
            }
        }        
        return true;
    }

    //---------------------------------------------------------------------
    
    /**
     *  Returns whether component can accept object for import.
     *  @param obj Object to be imported.
     *  @return True if object can be imported, false otherwise.
     *  @throws SavannahListException if import disallowed with
     *          explanation contained in exception getMessage string
     */ 
    
    public boolean canImport(Object obj) throws SavannahListException
    {
        String[] filenames = toStringArray(obj);
        
        if (filenames == null)
            return false;
        
        boolean canImp = inFileTypeDirectory();
        if (!canImp)
        {
            throw new SavannahListException(this,
                  "FEI filetype not selected.  Cannot drop.");
        }
        
        return canImp; 
    }
    
    //---------------------------------------------------------------------
    
    protected String[] toStringArray(Object obj)
    {
        String[] filenames = null;
        if (obj instanceof String)
        {
            filenames = new String[] {(String) obj};
        }
        else if (obj instanceof Object[])
        {
            Object[] objArray = (Object[]) obj;
            filenames = new String[objArray.length];
            for (int i = 0; i < objArray.length; ++i)                
                filenames[i] = objArray[i].toString();
        }
        else if (obj instanceof List)
        {
            List list = (List) obj;
            int numEntries = list.size();
            filenames = new String[numEntries];
            for (int i = 0; i < numEntries; ++i)
            {
                filenames[i] = list.get(i).toString();
            }                        
        }
        return filenames;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Method determines whether an action is legal in current state.
     * @param actionId The id of the action whose legal execution 
     *                 is in question
     * @param values Object array containing selected SavannahFeiTableElement
     *               objects
     */
    
    public boolean canPerformAction(int actionId, Object[] values)
    {
        if (actionId == ACT_REFRESH)
        {
            if (_currentFeiGroup == null)
                return false;
            return true;
        }        
        else if (actionId == ACT_CHANGE_DIRECTORY)
        {
        	if (inFileTypeDirectory())
        		return true;
        	
            if (values == null || values.length != 1)
                return false;

            if (!(values[0] instanceof SavannahTableElement))
                return false;
            
            SavannahTableElement element = (SavannahTableElement) values[0];   
            if (element.getType() != SavannahTableElement.TYPE_FOLDER)
                return false;
            return true;
        }
        else if (actionId == ACT_RENAME)
        {
            if (values == null || values.length != 1)
                return false;
            if (!(values[0] instanceof SavannahFeiTableElement))
                return false;
            if (!inFileTypeDirectory())
                return false;
            
            SavannahTableElement element = (SavannahTableElement) values[0];
            if (element.getType() != SavannahTableElement.TYPE_FILE)
                return false;
            return true;
        }
        else if (actionId == ACT_DELETE)
        {
            if (!inFileTypeDirectory())
                return false;
            if (values == null || values.length == 0)
                return false;
            
            SavannahFeiTableElement element;
            for (int i = 0; i < values.length; ++i)
            {
                if (!(values[i] instanceof SavannahFeiTableElement))
                    return false;
                element = (SavannahFeiTableElement) values[i];
                if (element == null || 
                        element.getType() == SavannahTableElement.TYPE_FOLDER)
                    return false;                
            }            
            return true;
        }
        else if (actionId == ACT_COMMENT)
        {
            if (!inFileTypeDirectory())
                return false;
            if (values == null || values.length != 1)
                return false;
            if (!(values[0] instanceof SavannahFeiTableElement))
                return false;

            SavannahFeiTableElement element;
            element = (SavannahFeiTableElement) values[0];
            if (element.getType() == SavannahTableElement.TYPE_FOLDER)
                return false;
            return true;
        }
        
        return false;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns array of selected values within list.  If a parent
     *  folder is selected, then it will be replaced with the 
     *  parent dir string.
     *  @return Object array of values selected in list
     */ 
    public Object[] getSelectedValues()
    {
        SavannahTableElement[] elements = getSelectedElements();
        Object[] filenames = new String[elements.length];
        SavannahTableElement element;
        for (int i = 0; i < elements.length; ++i)
        {
            if (elements[i].isParent())
                filenames[i] = this._parentDirectoryString;
            else
                filenames[i] = elements[i].getName();
        }
        return filenames;
    }
    
    //---------------------------------------------------------------------
    
    protected SavannahTableElement[] getSelectedElements()
    {
        SavannahTableElement[] elements;
        int[] selectedRows = _fileTable.getSelectedRows();
        elements = new SavannahTableElement[selectedRows.length];
        SavannahTableElement element;
        for (int i = 0; i < selectedRows.length; ++i)
        {
            element = (SavannahTableElement) 
                                _tableModel.elementAt(selectedRows[i]);
            elements[i] = element;
        }
        return elements;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Delete filenames from current FEI filetype.
     *  @param filenames Names of the file to delete
     *  @return True if deletion was successful, false otherwise
     */
     
    protected boolean deleteFiles(String[] filenames) 
    {		
        boolean rValue = true;
        
        if (true)
        {       
            this._model.deleteFromFei(filenames);
        }
        
        return rValue;
	}
    
    //---------------------------------------------------------------------
    
    /**
     *  Rename file.
     *  @param origName Original filename
     *  @param newName New filename
     *  @return True by default
     */
     
    protected boolean renameFile(final String origName,
                                 final String newName) 
    {       
        boolean rValue = true;

        if (origName == null || origName.equals("") ||
            newName  == null || newName.equals(""))
            return false;

        if (origName.equals(newName))
            return false;
        
        this._model.renameInFei(origName, newName);        
        return rValue;
    }
 
    //---------------------------------------------------------------------
    
    /**
     * Attempts to change the model file type.
     * @param filetype New file type
     */
    
    protected void changeFileType(String filetype)
    {
        boolean cont = true, error = false, abort = false;
        int attemptCount = SavannahModel.LOGIN_ATTEMPT_COUNT;
                        
        String serverGroup = this._model.getCurrentFeiServer();
        if (serverGroup == null)
            serverGroup = FileType.extractServerGroup(filetype);
        
        while (cont && (attemptCount > 0))
        {  
            cont  = false;
            error = false;
            
            this._model.printDebug(__classname+"::changeFileType:: Changing to "+filetype);
            
            try {
                this._model.setCurrentFeiType(filetype);  
            } catch (SessionException sesEx) {
                _logger.error(sesEx.getMessage(), sesEx);//sesEx.printStackTrace();
                error = true;
                --attemptCount;
  
                //invalid login, get username, password, try again
                if (sesEx.getErrno() == Constants.INVALID_LOGIN)
                {
                    //attempt to set username password
                    if (attemptCount >= 0)
                    {
                        cont = true;  //enable loop to go again
                        this._model.setStatusMessage("Login failed.");
                        JOptionPane.showMessageDialog(this,
                                "Invalid login.  Please re-enter "+
                                "username and password in login window.",
                                "Login Error", JOptionPane.ERROR_MESSAGE);
                        
                        boolean loginSuccess = _model.getLoginInfo(serverGroup);
                        int rVal = (loginSuccess) ? JOptionPane.OK_OPTION : JOptionPane.CANCEL_OPTION;
                        //int rVal = getLoginInfo();
                        
                        if (rVal != JOptionPane.OK_OPTION)
                        {
                            error = false;
                            abort = true;
                            cont  = false;
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(this,
                                "Invalid login.  Max attempt count ("+
                                SavannahModel.LOGIN_ATTEMPT_COUNT+") reached!",
                                "Login Error", JOptionPane.ERROR_MESSAGE); 
                    }
                } 
                else //general error, don't try again...
                {
                    JOptionPane.showMessageDialog(this, 
                            "Error occurred while changing FEI filetype to '"+
                            filetype+"'\n\n"+
                            "ERROR DETAILS\n-MESSAGE: "+
                            sesEx.getMessage()+
                            "\n-CODE: "+sesEx.getErrno(), 
                            "Login Error", JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (Exception ex) {
                error = true;
                --attemptCount;
                _logger.error(ex.getMessage(), ex);//ex.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                        "Error occurred while changing FEI filetype to '"+
                        filetype+"'\n\n"+
                        "ERROR DETAILS\n-MESSAGE: "+
                        ex.getMessage()+
                        "\n-CODE: "+Constants.EXCEPTION, 
                        "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        //set status message
        if (error || abort)
        {
            this._model.setStatusMessage("Login aborted.");
        }
        else
        {
            this._model.setStatusMessage("Login successful.");
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Dispatches call to model for comment update.
     *  @param file The file which comment will be updated.
     *  @param comment The comment string, null resets.  
     */
    
    protected boolean commentFile(String file, String comment)
    {                
        this._model.commentInFei(file, comment);
        return true;
    }
    
    //---------------------------------------------------------------------
    
//    /**
//     *  Displays dialog for user to enter username and password
//     *  @return Return value of dialog
//     */
//     
//    public int getLoginInfo()
//    {
//        //create login dialog
//        LoginDialog loginDialog = new EncryptedLoginDialog("FEI Login", 
//                                      this._model.getUsername());
//        int rVal = loginDialog.showDialog(this);
//        
//        if (rVal == JOptionPane.OK_OPTION)
//        {
//            this._model.setUsername(loginDialog.getUsername());
//            this._model.setPassword(loginDialog.getPassword());
//        }       
//        return rVal;
//    }
        
    //---------------------------------------------------------------------

    /**
     * Creates pop-up menu based on selected entries parameter.
     * @param entries Array of selected objects.  NOTE: For all objects,
     * the value of obj.toString() must return the filename
     * of the entry.
     */
    
    protected JPopupMenu createPopupMenu(final Object[] entries, Point point)
    {        
        if (point == null)
            return null;
        
        JPopupMenu menu = new JPopupMenu();
        AbstractAction cdAction       = null;    
        AbstractAction renameAction   = null;
        AbstractAction deleteAction   = null;        
        AbstractAction refreshAction  = null;
        AbstractAction commentAction  = null;
        
        boolean canCd, canRename, canDelete, canRefresh, canComment;
        
        SavannahFeiTableElement element = null;
        
        //---------------
        //determine legal actions
        
        canCd      = canPerformAction(ACT_CHANGE_DIRECTORY, entries);
        canRename  = canPerformAction(ACT_RENAME, entries);
        canDelete  = canPerformAction(ACT_DELETE, entries);
        canRefresh = canPerformAction(ACT_REFRESH, entries);
        canComment = canPerformAction(ACT_COMMENT, entries);
        
        //---------------
        //construct the actions
        
        if (canCd)
        {        	
            String fileType = null;
            
            if (!inFileTypeDirectory() && entries != null && entries.length > 0)
            {
          	  element = (SavannahFeiTableElement) entries[0];                	 
          	  if (!element.isParent())
          		  fileType = entries[0].toString();
            }     
            String text = fileType == null ? "Close filetype" :
                                      "Open selected filetype";
            cdAction = new ChangeDirAction(text, fileType);
            
        }
        if (canRename)
        {
            renameAction = new RenameAction(entries[0].toString());
        }
        if (canDelete)
        {
            String[] files = new String[entries.length];
            for (int i = 0; i < entries.length; ++i)
                files[i] = entries[i].toString();
            
            deleteAction = new DeleteAction(files);
        }
        if (canComment)
        {
            String comment = null;
            if (entries[0] instanceof SavannahFeiTableElement)
                comment = ((SavannahFeiTableElement)entries[0]).getComment();
            commentAction = new CommentAction(entries[0].toString(),comment);
        }
        if (canRefresh)
        {
            refreshAction = new RefreshAction();
        }
        
        //---------------
        //add the actions to the menu
        
        if (cdAction != null)
        {
            menu.add(cdAction);
            menu.addSeparator();
        }
        if (renameAction != null)
        {
            menu.add(renameAction);
            menu.addSeparator();
        }    
        if (deleteAction != null)
        {
            menu.add(deleteAction);
            menu.addSeparator();
        }
        if (commentAction != null)
        {
            menu.add(commentAction);
            menu.addSeparator();
        }
        if (refreshAction != null)
        {
            menu.add(refreshAction);
        }       
        
        return menu;
    }
    
    //---------------------------------------------------------------------
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        //----------------------
        
        if (propName.equals("CURRENT_FEI_GROUP"))
        {
            String newGroup = (String) pce.getNewValue();
            setCurrentFeiGroup(newGroup);
        }
        
        //----------------------
        
        else if (propName.equals("CURRENT_FEI_TYPE"))
        {
            String newType = (String) pce.getNewValue();
            setCurrentFeiType(newType);
        }
        
        //----------------------
        
        else if (propName.equals("FEI_LISTING"))
        {
            this._model.printDebug(__classname+"::propListener:: FEI LISTING");
            refreshListing();
        }
        
        //----------------------
        
    }
        
    
    public void filterChange(SavannahListFilter filter)
    {
        if (filter.getName().equals("FEI_FILTER"))
        {
            refreshListing();
        }
        else if (filter.getName().equals("FEI_FT_FILTER"))
        {
            refreshListing();
        }
    }
    
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
    
    /**
     * Creates panel containing details of record state.
     * @param element The SavannahTransferRecord instance whose details
     * are to be used to populate the panel display.
     * @return JPanel containing details of record state.
     */
    
    protected JPanel createDetailsPanel(SavannahFeiTableElement element)
    {
        JPanel panel = new JPanel();
        
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel lPanel = new JPanel();
        JPanel rPanel = new JPanel();
        lPanel.setLayout(new BoxLayout(lPanel, BoxLayout.Y_AXIS));
        rPanel.setLayout(new BoxLayout(rPanel, BoxLayout.Y_AXIS));
        

        lPanel.add(new JLabel(" Filename "));
        lPanel.add(new JLabel(" Filesize "));
        lPanel.add(new JLabel(" Modified "));
        lPanel.add(new JLabel(" Filetype "));
        lPanel.add(new JLabel(" Comment  "));


        
        rPanel.add(new JLabel(" = "+element.getName()+" "));
        rPanel.add(new JLabel(" = "+element.getSize()+ " bytes"));
        rPanel.add(new JLabel(" = "+DateTimeUtil.getDateCCSDSAString(new Date(
                                    element.getModificationDate()))+ " "));
        rPanel.add(new JLabel(" = "+element.getFeiGroup() + ":"+
                                    element.getFeiType()+" "));
        String comment = element.getComment();
        comment = (comment == null) ? "\"\"" : comment;
        rPanel.add(new JLabel(" = "+comment+ " "));  
        
        
        lPanel.setOpaque(false);
        rPanel.setOpaque(false);
        
        panel.add(lPanel);
        panel.add(rPanel);
        panel.add(Box.createHorizontalGlue());
        
        return panel;
    }

    //---------------------------------------------------------------------
    
    //=====================================================================
    //=====================================================================
    //=====================================================================
    
    
    //=====================================================================
    
    class TableMouseListener extends MouseAdapter 
    {
        public void mouseClicked(MouseEvent me) 
        {
            if (!(me.getSource() instanceof JComponent))
                return;
            
            JComponent c = (JComponent) me.getSource();
            
            // if right mouse button clicked (or me.isPopupTrigger())
            if (SwingUtilities.isRightMouseButton(me))
            {                
                Object[] selected = null;
                if (!_fileTable.getSelectionModel().isSelectionEmpty())
                {
                    selected = getSelectedElements();
                }
                
                JPopupMenu menu = createPopupMenu(selected, me.getPoint());
                if (menu != null)
                    menu.show(c, me.getX(), me.getY());  
                me.consume();
            }        

            else if (me.getClickCount() == 2)  // Double-click 
            {       
                //Get item index               
                int row = _fileTable.rowAtPoint(me.getPoint());
                if (row == -1 || row >= _fileTable.getRowCount())
                    return;
                
                SavannahTableElement element;
                element = (SavannahTableElement) _tableModel.elementAt(row);
                if (element == null)
                    return;
                
                //we are in the parent directory, only feiTypes listed
                if (!inFileTypeDirectory())
                {                	                	
                    //enter selected filetype
                    _model.printDebug(__classname+"::MOUSELISTENER:"+
                              "entry = "+element+"(Setting file type)");
                    changeFileType(element.getName());                
                }
                else // we are in a filetype currently, can move up only
                {
                    if (element.isParent())
                    {
                        //leave current filetype
                        _model.printDebug(__classname+
                                "::MOUSELISTENER: "+
                        "entry = parentDir. (Unsetting file type)");
                        changeFileType(null);
                    }    
                    else
                    {
                        //popup window with file details
                        JPanel detailsPanel  = createDetailsPanel(
                                            (SavannahFeiTableElement) element);
                        JOptionPane.showMessageDialog(SavannahFeiList.this, 
                                            detailsPanel, "File Details",
                                            JOptionPane.PLAIN_MESSAGE);
                        me.consume();
                    }
                }      
                me.consume();
            } 
        }
    }
    
    //=====================================================================  
    //=====================================================================
    
    class DeleteAction extends AbstractAction
    {
        String[] _files;
        
        //-----------------------------------------------------------------
        
        public DeleteAction(String[] files)
        {
            super("Delete", _deleteIcon);
            
            if (files == null || files.length == 0)
                throw new IllegalArgumentException("DeleteAction::"+
                    "constructor:: Parameter cannot be null or empty.");
            
            _files = new String[files.length];
            for (int i = 0; i < files.length; ++i)
            {
                _files[i] = files[i];
                if (_files[i] == null || isDirectory(_files[i]))
                    this.setEnabled(false);
            }
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            String message;
            if (_files.length > 1)
                message = "Delete these "+_files.length+" items?";
            else
                message = "Delete '"+_files[0]+"'?";
            
            int answer =JOptionPane.showConfirmDialog(
                                SavannahFeiList.this,
                                message,
                                "Delete Confirm", 
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                     
            if (answer == JOptionPane.YES_OPTION)
            {
                deleteFiles(_files);
            }                    
            return;
        }
        
        //-----------------------------------------------------------------
    }    
    
    //=====================================================================

    class ChangeDirAction extends AbstractAction
    {
        String _dir;
        
        //-----------------------------------------------------------------
        
        /**
         * Change directory/folder action.
         * @param dir Name of folder to open, null to reset.
         */
        
        public ChangeDirAction(String dir)
        {
        	this("Change Filetype", dir);
        }
        public ChangeDirAction(String text, String dir)
        {
            super(text, _cdIcon);
            
            if (dir != null && !isDirectory(dir))
                throw new IllegalArgumentException("ChangeDirAction"+
                        "::constructor:: Parameter must be a valid "+
                        "filetype entry.");
                
            _dir = dir;
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {            
            changeFileType(_dir);
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    class RenameAction extends AbstractAction
    {
        String _origName;
        
        //-----------------------------------------------------------------
        
        public RenameAction(String origName)
        {
            super("Rename", _renameIcon);
            if (origName == null || isDirectory(origName))
                throw new IllegalArgumentException("RenameAction::"+
                        "constructor:: Parameter must non-null file");
            _origName = origName;
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {                    
            String newName = JOptionPane.showInputDialog(
                                        SavannahFeiList.this,
                                        "Rename "+_origName+
                                        " to: ",
                                        _origName);                    
            if (newName != null && !newName.equals("") &&
                                !newName.equals(_origName))
            {
                int numEntries = _tableModel.getRowCount();
                for (int i = 0; i < numEntries; ++i)
                {
                    String entry = _tableModel.elementAt(i).toString();
                    if (newName.equals(entry))
                    {
                        JOptionPane.showMessageDialog(SavannahFeiList.this,
                             "Filename '"+newName+"' already exists.\n"+
                             "Rename aborted.\n", "Rename Error",
                             JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                renameFile(_origName, newName);
            }                    
            return;
        }
        
        //-----------------------------------------------------------------
    }

    //=====================================================================
    
    class RefreshAction extends AbstractAction
    {        
        public RefreshAction()
        {
            super("Refresh", _refreshIcon);            
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            refresh();                 
            return;
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    class CommentAction extends AbstractAction
    {
        String _filename;
        String _comment;
        
        //-----------------------------------------------------------------
        
        public CommentAction(String filename, String comment)
        {
            super("Comment", _commentIcon);
            if (filename == null || isDirectory(filename))
                throw new IllegalArgumentException("CommentAction::"+
                        "constructor:: Filename must be non-null file");
            _filename = filename;
            _comment  = comment;
            if (_comment == null)
                _comment = "";
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {                    
            String newComment = JOptionPane.showInputDialog(
                                        SavannahFeiList.this,
                                        "Set comment for '"+_filename+
                                        "' to: ",
                                        _comment);                    
            if (newComment != null && !newComment.equals(_comment))
            {
                commentFile(_filename, newComment);
            }                    
            return;
        }
        
        //-----------------------------------------------------------------
    }

    //=====================================================================
    //=====================================================================
    
     class FeiListToolbar extends JPanel implements PropertyChangeListener,
                                                    ListSelectionListener
     {

         SavannahModel _model;
         JPanel _panel;
         JToolBar _toolbar;

         AbstractAction cdAction       = null;    
         AbstractAction renameAction   = null;
         AbstractAction deleteAction   = null;        
         AbstractAction refreshAction  = null;
         AbstractAction commentAction  = null;
         
         JButton _cdButton, _renameButton, _deleteButton,
                  _refreshButton, _commentButton;
          
         //--------------------------------------------------------------------


         public FeiListToolbar(SavannahModel model)
         {
             _model = model;
             _toolbar = new JToolBar();
             _toolbar.setFloatable(false);

             addButtons();
             this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
             this.add(_toolbar);
             this.add(Box.createHorizontalGlue());
             this.setBorder(BorderFactory.createLineBorder(Color.gray));
         }

         //--------------------------------------------------------------------

         protected void addButtons()
         {
              JButton button;

              //Change diretory
              //_cdButton = makeToolbarButton("resources/cd24.gif",
              _cdButton = makeToolbarButton("resources/chftype24.gif",
                                      "CHANGE_DIRECTORY",
                                      "Move to selected filetype",
                                      "Change Filetype");             
              _toolbar.add(_cdButton);
              _toolbar.addSeparator();
              
              //Rename     
              _renameButton = makeToolbarButton("resources/rename24.gif",
                                      "Rename Selection",
                                      "Rename file",
                                      "Rename");
              _renameButton.addActionListener(new ActionListener()
                      {public void actionPerformed(ActionEvent ae){;}});                           
              _toolbar.add(_renameButton);
     
              //Delete
              _deleteButton = makeToolbarButton("resources/delete24.gif",
                                      "Delete Selection",
                                      "Delete selected files",
                                      "Delete");
              _deleteButton.addActionListener(new ActionListener()
                      {public void actionPerformed(ActionEvent ae){;
                        }});
              _toolbar.add(_deleteButton);
              
              //Comment
              _commentButton = makeToolbarButton("resources/comment24.gif",
                                      "COMMENT_FILE",
                                      "Set comment for file",
                                      "Comment");
              _commentButton.addActionListener(new ActionListener()
                      {public void actionPerformed(ActionEvent ae){;}});
              _toolbar.add(_commentButton);
              
              
              _toolbar.addSeparator();
                            
              //------------
                          
              //Refresh
              _refreshButton = makeToolbarButton("resources/refresh24.gif",
                                      "REFESH_LISTING",
                                      "Refresh current listing",
                                      "Refresh");
              _refreshButton.addActionListener(new ActionListener()
                      {public void actionPerformed(ActionEvent ae){;}});
              _toolbar.add(_refreshButton);
          }

          //-----------------------------------------------------------------

          protected JButton makeToolbarButton(String imageLocation,
                                              String actionCommand,
                                              String toolTipText,
                                              String altText) 
          {
              //Look for the image.        
              URL imageURL = FileListToolbar.class.getResource(imageLocation);            
              
              //Create and initialize the button.
              JButton button = new JButton();
              button.setActionCommand(actionCommand);
              button.setToolTipText(toolTipText);
              
              if (imageURL != null) {                      //image found
                  button.setIcon(new ImageIcon(imageURL, altText));
              } else {                                     //no image found
                  button.setText(altText);
              }
              
              button.setEnabled(false); 
              button.setRolloverEnabled(true);
              return button;
          }

          //-----------------------------------------------------------------
          
          public void valueChanged(ListSelectionEvent event)
          {
              if (event.getValueIsAdjusting())
                  return;
                          
              Object[] values = getSelectedElements();
              boolean canCd, canRename, canDelete, canRefresh, canComment;
              
              canCd      = canPerformAction(ACT_CHANGE_DIRECTORY, values);
              canRename  = canPerformAction(ACT_RENAME, values);
              canDelete  = canPerformAction(ACT_DELETE, values);
              canRefresh = canPerformAction(ACT_REFRESH, values);
              canComment = canPerformAction(ACT_COMMENT, values);
              
              //cd
              if (canCd)
              {
                  SavannahTableElement element;
                  String fileType = null;
                  
                  if (!inFileTypeDirectory() && values != null && values.length > 0)
                  {
                	  element = (SavannahTableElement) values[0];                	 
                	  if (!element.isParent())
                		  fileType = values[0].toString();
                  }
                      
                  String text = fileType == null ? "Close filetype" :
     		                                    "Open selected filetype";
                  _cdButton.setAction(new ChangeDirAction(fileType));
                  _cdButton.setToolTipText(text);
                  _cdButton.setText(null);
                  _cdButton.setRolloverIcon(_cdRollIcon);
                  _cdButton.setEnabled(true);
              }
              else
              {
                  _cdButton.setEnabled(false);
              }
              
              //rename
              if (canRename)
              {
                  _renameButton.setAction(new RenameAction(values[0].toString()));
                  _renameButton.setToolTipText("Rename file");
                  _renameButton.setText(null);
                  _renameButton.setRolloverIcon(_renameRollIcon);
                  _renameButton.setEnabled(true);
              }
              else
              {
                  _renameButton.setEnabled(false);
              }
              
              //delete
              if (canDelete)
              {
                  //convert to File array
                  String[] files = new String[values.length];
                  for (int i = 0; i < values.length; ++i)
                      files[i] = values[i].toString();
                  
                  _deleteButton.setAction(new DeleteAction(files));
                  _deleteButton.setToolTipText("Remove selected file(s)");
                  _deleteButton.setText(null);
                  _deleteButton.setRolloverIcon(_deleteRollIcon);
                  _deleteButton.setEnabled(true);
              }
              else
              {
                  _deleteButton.setEnabled(false);
              }                        
              
              //refresh
              if (canRefresh)
              {                
                  _refreshButton.setAction(new RefreshAction());
                  _refreshButton.setToolTipText("Refresh current listing");
                  _refreshButton.setText(null);
                  _refreshButton.setRolloverIcon(_refreshRollIcon);
                  _refreshButton.setEnabled(true);
              }
              else
              {
                  _refreshButton.setEnabled(false);
              }
              
              //comment
              if (canComment)
              {           
                  String comment = null;
                  String filename = values[0].toString();
                  
                  if (values[0] instanceof SavannahFeiTableElement)
                  {
                      comment = ((SavannahFeiTableElement) values[0]).getComment();
                  }
                  
                  _commentButton.setAction(new CommentAction(filename, 
                                                             comment));
                  _commentButton.setToolTipText("Set comment of file");
                  _commentButton.setText(null);
                  _commentButton.setRolloverIcon(_commentRollIcon);
                  _commentButton.setEnabled(true);
              }
              else
              {
                  _commentButton.setEnabled(false);
              }
          }
          
          //-----------------------------------------------------------------

          /**
           *  Implementation of the PropertyChangeListener interface.  For
           *  interaction with the application model.  Method is called whenever
           *  a change is made to a model property.
           *  @param evt A PropertyChangeEvent object describing the event 
           *               source and the property that has changed.
           */

          public void propertyChange(PropertyChangeEvent pce)
          {
              String propName = pce.getPropertyName();

              //--------------------------
             

              //--------------------------      
          } 
      }
      
      //=====================================================================
      
}

