package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahFilterModel.SavannahListFilterListener;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.dnd.SavannahTableTransferHandler;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 *  File list for the Savannah application.
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
 * @version $Id: SavannahFileList.java,v 1.36 2016/09/29 23:30:03 ntt Exp $
 *
 */
 
public class SavannahFileList extends JPanel implements SavannahList, 
                                             PropertyChangeListener,
                                             SavannahListFilterListener
{
    private final String __classname = "SavannahFileList";
    
    protected JPanel _mainPanel;
    protected SavannahModel _model;
    protected File _currentDirectory;
    protected File _parentDirectory;    
    protected JTable _fileTable;
    protected SavannahTableModel _tableModel;
    protected JScrollPane _scrollPane;
    protected SavannahTableCellRenderer _tableCellRenderer;
    protected SavannahTableTransferHandler _transferHandler;
    protected String _transferIdentifier;
    protected MouseListener _mouseListener;
    protected SavannahDirectoryPanel _directoryPanel;
    protected JSplitPane _splitPane;
    protected SavannahFileFilter _fileFilter;
    
    protected String _nonWhitespacePatternStr = "\\S+";
    protected Pattern _nonWhitespacePattern;
    
    protected int _dndAction = DnDConstants.ACTION_COPY;
    protected FileListToolbar _toolbar;
    
    public static final int ACT_NONE             = 0;
    public static final int ACT_REFRESH          = 1;
    public static final int ACT_CHANGE_DIRECTORY = 2;
    public static final int ACT_RENAME           = 4;
    public static final int ACT_DELETE           = 5;
    public static final int ACT_MAKE_DIRECTORY   = 8;
    public static final int ACT_REMOVE_DIRECTORY = 9;
            
    protected ImageIcon _refreshIcon, _refreshRollIcon;
    protected ImageIcon _cdIcon, _cdRollIcon;
    protected ImageIcon _renameIcon, _renameRollIcon;
    protected ImageIcon _deleteIcon, _deleteRollIcon;
    protected ImageIcon _mkdirIcon, _mkdirRollIcon;
    protected ImageIcon _rmdirIcon, _rmdirRollIcon;

    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------
    
    /**
     *  Default constructor.  Uses default initial directory by searching
     *  for a valid directory in the following order: komodo.working.dir,
     *  user.dir, user.home, first root directory.  If none of these satisfy, 
     *  an IOException is thrown.
     *  @param model Reference to the Savannah application model
     *  @throws IOException if problem occurs with selected directory
     */
     
    public SavannahFileList(SavannahModel model)  throws IOException
    {
//        String kwd = System.getProperty("komodo.working.dir");
        String kwd = model.getLocalDirectory().getAbsolutePath();
        if (kwd == null)
            kwd = System.getProperty("komodo.working.dir");
        if (kwd == null)
            kwd = System.getProperty("user.dir");
        if (kwd == null)
            kwd = System.getProperty("user.home");
        if (kwd == null)
        {
            File[] roots = File.listRoots();
            if (roots.length == 0)
                throw new IOException(__classname+"::constructor: "+
                      "File system does not contain a root directory.");
            kwd = roots[0].getName();            
        }
        
        if (kwd == null)
            throw new IOException(__classname+"::constructor: "+
                      "Unable to assign initial current directory");

        File directory = new File(kwd);
        directory = directory.getCanonicalFile();        
        init(model, directory);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Constructs instance using directory parameter for initial
     *  directory.
     *  @param model Reference to the Savannah application model
     *  @param directory File of a valid, readable directory.
     *  @throws IllegalArgumentException if parameter is null
     *  @throws IOException if error occurs with directory
     */
     
    public SavannahFileList(SavannahModel model, File directory) 
                                                       throws IOException
    {   
        if (directory == null)
            throw new IllegalArgumentException(__classname+"::constructor: " +
                                               "directory cannot be null.");
                                                    
        init(model, directory);   
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Initializes instance of object.  Builds list and sets
     *  initial directory.
     *  @throws IOException if error occurs with directory
     */
     
    protected void init(SavannahModel model, File directory) 
                                                        throws IOException
    {          
        if (model == null)
            throw new IllegalArgumentException(__classname+
                "::init(): model parameter cannot be null.");
                
        this._model = model;
        this._model.addPropertyChangeListener(this);
        
        this._nonWhitespacePattern = Pattern.compile(this._nonWhitespacePatternStr);
        
        //file filtering setup
        //1) register as listener to filter model, (2) create filter
        this._model.getFilterModel().addFilterListener(this);
        this._fileFilter = new SavannahFileFilter();
        
        //create the main panel
        this._mainPanel = new JPanel(new BorderLayout());
        
        
        //construct the table
        this._tableModel = new SortableSavannahTableModel();
        this._fileTable = new SavannahTable(_model, _tableModel);
        this._transferIdentifier = __classname + "::_fileTable::" + 
                                    this._fileTable.toString();
        this._fileTable.setName(_transferIdentifier);
        
        //setup listeners
        this._mouseListener = new TableMouseListener();
        this._fileTable.addMouseListener(this._mouseListener);
       
        //setup drag and drop
        this._fileTable.setDragEnabled(true);
        this._transferHandler = new SavannahTableTransferHandler(_model, this);
        this._fileTable.setTransferHandler(this._transferHandler);
        this.setTransferHandler(this._transferHandler); //for empty list
        
        //layout details for table
        this.setLayout(new BorderLayout());        
        this._scrollPane = new JScrollPane(_fileTable);
        this._scrollPane.setBorder(null);
        this._scrollPane.getViewport().setBackground(
                                        this._fileTable.getBackground());
        
        this._mainPanel.add(_scrollPane, BorderLayout.CENTER);
        
        //allow scrollpane to be droppable target
        this._scrollPane.setName(_transferIdentifier);
        this._scrollPane.setTransferHandler(_transferHandler);
        this._scrollPane.addMouseListener(this._mouseListener);

        initActionIcons(); 
        
        //create and setup toolbar
        this._toolbar = new FileListToolbar(_model);
        this._fileTable.getSelectionModel().addListSelectionListener(_toolbar);               
        this._mainPanel.add(_toolbar, BorderLayout.NORTH);
        
        //init directory
        try {
            setCurrentDirectory(directory);   
        } catch (IOException ioEx) {
            throw new IOException(__classname+"::constructor: "+
                    "Error occurred during construction.\n"+
                    "Message: "+ioEx.getMessage());
        }  
        
        _fileTable.getSelectionModel().removeSelectionInterval(0,0);
        
        
        //---------------------
        

        _directoryPanel = new SavannahDirectoryPanel(this._model);
        _model.addPropertyChangeListener(_directoryPanel);
        _splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                              _directoryPanel,
                                              _mainPanel);
        _splitPane.setOneTouchExpandable(true);
        _splitPane.setDividerLocation(100);
        this.add(_splitPane, BorderLayout.CENTER);
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
            _fileTable.getSelectionModel().addListSelectionListener(listener);   
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
          _fileTable.getSelectionModel().
                                removeListSelectionListener(listener);     
    }
    
    //---------------------------------------------------------------------
    
    protected void initActionIcons()
    {
        URL imageURL;
        
        //Refresh
        imageURL = FileListToolbar.class.getResource(
                                         "resources/refresh24.gif");            
        _refreshIcon = new ImageIcon(imageURL, "Refresh");
        imageURL = FileListToolbar.class.getResource(
                                        "resources/refresh24_ro.gif");            
        _refreshRollIcon = new ImageIcon(imageURL, "Refresh");
        
        // Change Directory
        imageURL = FileListToolbar.class.getResource(
                                         "resources/cd24.gif");            
        _cdIcon = new ImageIcon(imageURL, "Change Directory");
        imageURL = FileListToolbar.class.getResource(
                                        "resources/cd24_ro.gif");            
        _cdRollIcon = new ImageIcon(imageURL, "Change Directory");

        //Rename
        imageURL = FileListToolbar.class.getResource(
                                         "resources/rename24.gif");            
        _renameIcon = new ImageIcon(imageURL, "Rename");
        imageURL = FileListToolbar.class.getResource(
                                        "resources/rename24_ro.gif");            
        _renameRollIcon = new ImageIcon(imageURL, "Rename");
                
        //Delete
        imageURL = FileListToolbar.class.getResource(
                                         "resources/delete24.gif"); 
        _deleteIcon = new ImageIcon(imageURL, "Delete");
        imageURL = FileListToolbar.class.getResource(
                                        "resources/delete24_ro.gif"); 
        _deleteRollIcon = new ImageIcon(imageURL, "Delete");    
        
        //Mkdir
        imageURL = FileListToolbar.class.getResource(
                                         "resources/mkdir24.gif");            
        _mkdirIcon = new ImageIcon(imageURL, "Create Directory");
        imageURL = FileListToolbar.class.getResource(
                                        "resources/mkdir24_ro.gif");            
        _mkdirRollIcon = new ImageIcon(imageURL, "Create Directory");
        
        //Rmdir
        imageURL = FileListToolbar.class.getResource(
                                         "resources/rmdir24.gif");            
        _rmdirIcon = new ImageIcon(imageURL, "Remove Directory");
        imageURL = FileListToolbar.class.getResource(
                                        "resources/rmdir24_ro.gif");            
        _rmdirRollIcon = new ImageIcon(imageURL, "Remove Directory");

    }
        
    //---------------------------------------------------------------------
    
    /**
     *  Returns current directory referenced by the file view.
     */
     
    public File getCurrentDirectory()
    {
        return _currentDirectory;
    }

    //---------------------------------------------------------------------
    
    /**
     *  Sets the current directory pointed by the file view.  Refreshes
     *  listing according to new contents.
     *  @param directory a valid, readable directory
     *  @throws IOException if error occurs from directory
     *  @throws IllegalArgumentException if parameter is null
     */
     
    public void setCurrentDirectory(File directory) throws IOException
    {
        if (directory == null)
        {
            throw new IllegalArgumentException(__classname+
                "::setCurrentDirectory(): directory cannot be null");
        }
        else if (!directory.isDirectory())
        {
            throw new IOException(__classname+
                "::setCurrentDirectory(): Argument '"+
                directory.getAbsolutePath()+"' is not a directory.");
        }
        else if (!directory.canRead())
        {
            throw new IOException(__classname+
                "::setCurrentDirectory(): Directory '"+
                directory.getAbsolutePath()+"' cannot be read");
        }
        
        if (_currentDirectory != null && _currentDirectory.equals(directory))
            return;
        
        this._currentDirectory = directory;
        this._parentDirectory = directory.getParentFile();
        _model.setLocalDirectory(this._currentDirectory);            
        refreshListing();
    }

    //---------------------------------------------------------------------
    
    /**
     *  Refreshes listing of current directory contents.
     */

    public void refresh()
    {            
        _model.getReceivalModel().resetLocal(); 
        refreshListing();
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Refreshes the list contents of the current directory
     */
     
    protected void refreshListing()
    {
        ArrayList files = new ArrayList();
        ArrayList dirs  = new ArrayList();
        File[] childFiles;
        File file;
        
        _model.setStatusMessage("Refreshing local files...");
        
        if (_model.getFilterModel().isEnabled("LOCAL_FILTER"))
            childFiles = _currentDirectory.listFiles(_fileFilter);
        else    
            childFiles = _currentDirectory.listFiles();
 
        //can be null, so check
        if (childFiles == null)
            childFiles = new File[0];
        
        //partition into directories and files
        for (int i = 0; i < childFiles.length; ++i)
        {
            file = childFiles[i];
            if (file.canRead())
            {
                if (file.isDirectory())
                    dirs.add(file);
                else
                    files.add(file);
            }
        }
        
        //sort em both
        Collections.sort(dirs);
        Collections.sort(files);
        
        //Should we capture selected items and reselect them?
        int[] selIndices = _fileTable.getSelectedRows();
        Object[] wereSelected = new Object[selIndices.length];
        for (int i = 0; i < selIndices.length; ++i)
        {
            wereSelected[i] = _tableModel.elementAt(selIndices[i]); 
        }
        
        //clear model entries
        _tableModel.clear();
                       
        //add directories first
        if (_parentDirectory != null)
            _tableModel.addElement(new SavannahFileTableElement(
                                   _parentDirectory, true));
        
        ArrayList elements = new ArrayList();
        int numDirs  = dirs.size();
        for (int i = 0; i < numDirs; ++i)
        {
            file = (File) dirs.get(i);
            elements.add(new SavannahFileTableElement(file));
        }
        _tableModel.addAll(elements);
        
        //add files next
        elements.clear();
        int numFiles = files.size();
        for (int i = 0; i < numFiles; ++i)
        {
            file = (File) files.get(i);
            elements.add(new SavannahFileTableElement(file));   
        } 
        _tableModel.addAll(elements);
        
        //select that which was once selected
        _fileTable.clearSelection();
        if (wereSelected != null && wereSelected.length > 0)
            selectItems(wereSelected);
        
        _fileTable.invalidate();
        _fileTable.repaint();
        _model.setStatusMessage("Refreshed local files.");
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
     *  Returns all selected files from the file listing.
     *  @return Array of files selected in listing
     */
     
    public File[] getSelectedFiles()
    {        
        int[] selectedRows = _fileTable.getSelectedRows();
        File[] fileArray = new File[selectedRows.length];
        SavannahFileTableElement element;
        for (int i = 0; i < selectedRows.length; ++i)
        {
            element = (SavannahFileTableElement) 
                                    _tableModel.elementAt(selectedRows[i]);
            fileArray[i] = element.getFile();
        }
        
        return fileArray;
    }    
    
    //---------------------------------------------------------------------
    
    /*
    private void copyFile(final String sourceName) 
    {
		File source = new File(sourceName);
        if (source == null || source.isDirectory() || !source.canRead())
        {   //error
            System.out.println("Cannot copy "+source+" to "+
                                _currentDirectory.getPath());
            return;
        }
        String base = source.getName();
        File dest = new File(_currentDirectory, base);
        if (dest.exists())
        {   int ans = JOptionPane.showConfirmDialog(_fileList,
                      dest.getName()+" already exists.\n\n"+
                      "Do you wish to overwrite?",
                      "File Exists", JOptionPane.YES_NO_CANCEL_OPTION,
                      JOptionPane.QUESTION_MESSAGE);
            if (ans != JOptionPane.YES_OPTION)
                return;
        }                
        try { 
            FileInputStream fis  = new FileInputStream(source);
            FileOutputStream fos = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int i = 0;
            while((i=fis.read(buf))!=-1) 
            {   fos.write(buf, 0, i);
            }
            fis.close();
            fos.close();
            refreshListing();
        } catch(FileNotFoundException fnfe) { 
            fnfe.printStackTrace();
        } catch(IOException ioe) { 
           ioe.printStackTrace();  
        } 
	}
    */
    
    //---------------------------------------------------------------------
    
    /**
     *  Copy file from FEI source to current directory
     *  @param filenames String array of names of the FEI files to transfer
     */
     
    protected boolean transferFiles(String[] filenames)
    {
        if (filenames == null || filenames.length ==0)
            return false;                      
        
        //want the ability to drag to a folder and put files in there
        //here, we get the index of the table, check if its a folder,
        //if so, use it.  Else use current dir as default. We ignore
        //folder if it is a parent folder.
        File destDir = this._currentDirectory;
        int count = this._fileTable.getSelectedRowCount();
        int index = this._fileTable.getSelectedRow();
        if (count == 1 && index != -1)
        {
            SavannahFileTableElement element = (SavannahFileTableElement) 
                                           _tableModel.elementAt(index);
            if (element != null)
            {
                if (element.getType() == SavannahTableElement.TYPE_FOLDER)
                {
                    if (element.isParent())
                    {
                        String[] options = {"Parent",
                                            "Current",
                                            "Cancel"};
                        
                        int val = JOptionPane.showOptionDialog(this._fileTable, 
                                "Are you sure you want to move file(s) to "+
                                "parent\ndirectory?\n\n"+
                                "Please verify by selecting target directory: ",
                                "Confirm Transfer", JOptionPane.YES_NO_OPTION, 
                                JOptionPane.QUESTION_MESSAGE, null, 
                                options, options[0]);
                        switch(val)
                        {
                            case 0:
                                destDir = new File(element.getPath());
                                break;
                            case 1:
                                break;
                            default:
                                _model.setStatusMessage("Transfer aborted.");
                                return false;
                        }
                    }
                    else
                        destDir = new File(element.getPath());
                }                
                
            }
        }
        
        _model.getFromFei(filenames, destDir);
                   
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
        
        return true;
    }
    
    //---------------------------------------------------------------------    
    
    /**
     *  Returns whether component can export object for transfer.
     *  @param obj Object to be exported.
     *  @return True if object can be exported, false otherwise.
     *  @throws SavannahListException if export disallowed with
     *          explanation contained in exception getMessage string
     */ 
    
    public boolean canExport(Object obj) throws SavannahListException
    {
        String[] filenames = toStringArray(obj);
        if (filenames == null)
            return false;
        
        for (int i = 0; i < filenames.length; ++i)
        {
            File file = new File(filenames[i]);
            if (file.isDirectory())
            {
                throw new SavannahListException(this,
                        "Cannot transfer '"+file.getName()+"'\n"+
                        "because directories are not transferrable.");
            }
            if (!file.canRead())
            {
                throw new SavannahListException(this,
                        "Cannot transfer '"+file.getName()+"'\n"+
                        "because file is unreadable.");
            }
            if (file.getName().indexOf(" ") != -1)
            {
                throw new SavannahListException(this,
                        "Cannot transfer '"+file.getName()+"'\n"+
                        "because filename contains whitespace.");
            }
            
            //handles general case of other whitespace chars
            Matcher m = this._nonWhitespacePattern.matcher(file.getName());
            if (m != null && !m.matches())
            {
                throw new SavannahListException(this,
                        "Cannot transfer '"+file.getName()+"'\n"+
                        "because filename contains whitespace.");
            }
        }
                
        return true;
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
        else if (obj instanceof File)
        {
            filenames = new String[] {((File) obj).getAbsolutePath()};
        }        
        return filenames;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Method determines whether an action is legal in current state.
     * @param actionId The id of the action whose legal execution 
     *                 is in question
     * @param values Object array containing selected File objects
     */
    
    public boolean canPerformAction(int actionId, Object[] values)
    {
        if (actionId == ACT_REFRESH)
        {
            return true;
        }        
        else if (actionId == ACT_CHANGE_DIRECTORY)
        {
            if (values == null || values.length != 1)
                return false;
            if (!(values[0] instanceof File))
                return false;
            File file = (File) values[0];
            if (!file.isDirectory())
                return false;
            return true;
        }
        else if (actionId == ACT_RENAME)
        {
            if (values == null || values.length != 1)
                return false;
            if (!(values[0] instanceof File))
                return false;
            File file = (File) values[0];
            if (!file.canWrite())
                return false;
            if (file.equals(_currentDirectory) || file.equals(_parentDirectory))
                return false;
            return true;
        }
        else if (actionId == ACT_DELETE)
        {
            if (values == null || values.length == 0)
                return false;
            File file;
            for (int i = 0; i < values.length; ++i)
            {
                if (!(values[i] instanceof File))
                    return false;
                file = (File) values[i];
                if (file == null || file.isDirectory() || !file.canWrite())
                    return false;                
            }            
            return true;
        }
        else if (actionId == ACT_MAKE_DIRECTORY)
        {
            if (!_currentDirectory.canWrite())
                return false;
            return true;
        }
        else if (actionId == ACT_REMOVE_DIRECTORY)
        {
            if (values == null || values.length != 1)
                return false;
            if (!(values[0] instanceof File))
                return false;
            File file = (File) values[0];
            if (!file.isDirectory() || !file.canWrite())
                return false;
            if (file.equals(_currentDirectory) || file.equals(_parentDirectory))
                return false;
            String[] filelist = file.list();
            if (filelist != null && filelist.length > 0)
                return false;
            return true;
        }

        return false;
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     *  Returns array of selected values within list.
     *  @return Object array of Strings denoting selected files 
     */
    
    public Object[] getSelectedValues()
    {
        int[] selectedRows = _fileTable.getSelectedRows();
        Object[] filenames = new String[selectedRows.length];
        SavannahTableElement element;
        for (int i = 0; i < selectedRows.length; ++i)
        {
            element = (SavannahTableElement) 
                                    _tableModel.elementAt(selectedRows[i]);
            filenames[i] = element.getPath();
        }
        return filenames;
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Delete file represented by file argument from current directory.
     *  Note, error message is displayed if file is a directory or has
     *  no write access, and deletion is aborted.
     *  @param files Files to be deleted
     */
     
    protected void deleteFiles(final File[] files)
    {        
       _model.deleteFromLocal(files);
    }
 
    protected void deleteFilesOld(final File[] files)
    {        
        if (files == null || files.length == 0)
            return;
        
        File file;
        String filename;
        
        for (int i = 0; i < files.length; ++i)
        {
            file = (File) files[i];
            filename = file.getName();
            
            if (file.isDirectory())
            {
               JOptionPane.showMessageDialog(this,
                      "Cannot remove "+filename+
                      "\nDirectories cannot be deleted",
                      "Delete Error", JOptionPane.ERROR_MESSAGE);
               _model.setStatusMessage("'Delete "+filename+"' aborted.");
            }
            else if (!file.canWrite())
            {
                JOptionPane.showMessageDialog(this,
                      "Cannot remove '"+filename+
                      "'\nWrite access was denied",
                      "Delete Error", JOptionPane.ERROR_MESSAGE);
                _model.setStatusMessage("'Delete "+filename+"' aborted.");  
            }
            else
            {
                _model.setStatusMessage("Deleting "+filename+"...");
            
                try {
                    file.delete();                
                    refreshListing();
                    _model.setStatusMessage("Deleted "+filename);
                } catch (Exception ex) {
                    _logger.error(ex.getMessage(), ex); //ex.printStackTrace();   
                    JOptionPane.showMessageDialog(this,
                      "Error occurred while attempting to delete "+filename+
                      "\nError message: "+ex.getMessage(),
                      "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } //end_for_loop
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Create directory in current directory
     */
    
    protected void createDirectory(String child)
    {
        File newDir = new File(_currentDirectory, child);
        if (newDir.exists())
        {
            JOptionPane.showMessageDialog(this,
                    "Sub-directory '"+child+"' already exist.",
                    "File Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean success = newDir.mkdir();
        if (!success)
        {
            JOptionPane.showMessageDialog(this,
                    "Unable to create '"+child+"' directory.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        refreshListing();
        _model.setStatusMessage("Created '"+child+"' directory");
    }
 
    //---------------------------------------------------------------------
    
    /**
     *  Create directory in current directory
     */
    
    protected void removeDirectory(File directory)
    {
        String dirName = directory.getName();
        if (!directory.exists())
        {
            JOptionPane.showMessageDialog(this,
                    "Directory '"+dirName+"' does not exist.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        boolean success = directory.delete();
        if (!success)
        {
            JOptionPane.showMessageDialog(this,
                    "Unable to delete '"+dirName+"' directory.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshListing();
        _model.setStatusMessage("Removed '"+dirName+"' directory");
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Rename file in current directory.
     *  @param origName Original filename
     *  @param newName New filename
     *  @return true if rename was successful, false otherwise
     */
    
    protected boolean renameFile(String origName, String newName)
    {
        if (origName == null || origName.equals("") ||
            newName  == null || newName.equals(""))
            return false;
    
        File oldFile = new File(_currentDirectory, origName);
        if (!oldFile.exists())
        {
            JOptionPane.showMessageDialog(this,
                    "Unable to rename '"+origName+"'.\n"+
                    "File does not exist.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        File newFile = new File(_currentDirectory, newName);
        if (newFile.exists())
        {
            JOptionPane.showMessageDialog(this,
                    "Unable to rename to '"+newName+"'.\n"+
                    "File already exists.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        boolean success = oldFile.renameTo(newFile);
        if (!success)
        {
            JOptionPane.showMessageDialog(this,
                    "Unable to rename '"+origName+
                    "' to\n'"+newName+"'.  Error occurred.",
                    "File Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        refreshListing();
        _model.setStatusMessage("Renamed '"+origName+"' to '"+newName+"'");
        return success;
    }
      
    //---------------------------------------------------------------------

    protected JPopupMenu createPopupMenu(final File[] files, Point point)
    {        
        if (point == null)
            return null;
        
        JPopupMenu menu = new JPopupMenu();

        AbstractAction cdAction       = null;    
        AbstractAction renameAction   = null;
        AbstractAction deleteAction   = null;        
        AbstractAction mkdirAction    = null;
        AbstractAction rmdirAction    = null;        
        AbstractAction refreshAction  = null;
        
        //---------------
        // Refresh
        refreshAction = new RefreshAction(); 
        
        if (files != null && files.length != 0)
        {
            //only one selection
            if (files.length == 1)
            {
                if (files[0].isDirectory())
                {
                    //-----------------------
                    // Change directory                    
                    cdAction = new ChangeDirAction(files[0]);                    
                    
                    //-----------------------
                    // Remove directory
                    rmdirAction = new RemoveDirAction(files[0]);
                }
                if (files[0].canWrite())
                {
                    //-----------------------
                    // Rename
                    
                    renameAction = new RenameAction(files[0]);                     
                }
            } //end_if_single_selection
            
            //-----------------------
            // Delete files
            
            boolean canDeleteAll = true;
            for (int i = 0; i < files.length; ++i)
                if (files[i] == null || files[i].isDirectory() ||
                                            !files[i].canWrite())
                    canDeleteAll = false;
                
            if (canDeleteAll)
            {
                deleteAction = new DeleteAction(files);
            }
        }                              
        
        //---------------
        // Create directory
        mkdirAction = new CreateDirAction(); 
        
        if (cdAction != null)
        {
            menu.add(cdAction);
            menu.addSeparator();
        }
        if (deleteAction != null)
        {
            menu.add(deleteAction);
            menu.addSeparator();
        }
        if (renameAction != null)
        {
            menu.add(renameAction);
            menu.addSeparator();
        }                
        if (mkdirAction != null)
        {
            menu.add(mkdirAction);
            if (rmdirAction == null)
                menu.addSeparator();
        }
        if (rmdirAction != null)
        {
            menu.add(rmdirAction);
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
        
        if (propName.equals("LOCAL_DIRECTORY"))
        {
            File newLocalDir = (File) pce.getNewValue();
            if (_currentDirectory.equals(newLocalDir))
                return;
            
            try {
                setCurrentDirectory(newLocalDir);   
            } catch (IOException ioEx) {
                _logger.error(ioEx.getMessage(), ioEx); //ioEx.printStackTrace();
            }
        }
        
        //----------------------
        
        else if (propName.equals("FILE_LISTING"))
        {
            refreshListing();
        }
        
        //----------------------
    }

    //---------------------------------------------------------------------
    
    /**
     * Implementation of the SavannahListFilterListener interface.
     * Care only about LOCAL_FILTER updates though.
     * @param filter Filter that has been updated
     */
    
    public void filterChange(SavannahListFilter filter)
    {
        if (filter.getName().equals("LOCAL_FILTER"))
        {
            String pattern = filter.getPattern();
            if (this._fileFilter.getExpression() == null ||
                !this._fileFilter.getExpression().equals(pattern))
                this._fileFilter.setExpression(pattern);
            refreshListing();
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Creates panel containing details of record state.
     * @param element The SavannahTransferRecord instance whose details
     * are to be used to populate the panel display.
     * @return JPanel containing details of record state.
     */
    
    protected JPanel createDetailsPanel(SavannahFileTableElement element)
    {
        JPanel panel = new JPanel();
        
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel lPanel = new JPanel();
        JPanel rPanel = new JPanel();
        lPanel.setLayout(new BoxLayout(lPanel, BoxLayout.Y_AXIS));
        rPanel.setLayout(new BoxLayout(rPanel, BoxLayout.Y_AXIS));
        

        lPanel.add(new JLabel(" Filename  "));
        lPanel.add(new JLabel(" Filesize  "));
        lPanel.add(new JLabel(" Modified  "));
        lPanel.add(new JLabel(" Directory "));
        
        rPanel.add(new JLabel(" = "+element.getName()+" "));
        rPanel.add(new JLabel(" = "+element.getSize()+ " bytes"));
        rPanel.add(new JLabel(" = "+DateTimeUtil.getDateCCSDSAString(new Date(
                                    element.getModificationDate()))+ " "));
        rPanel.add(new JLabel(" = "+element.getParent()+ " "));
        
        
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
               File[] files = null;
               if (!_fileTable.getSelectionModel().isSelectionEmpty())
               {
                   files = getSelectedFiles();
               }
                              
               JPopupMenu menu = createPopupMenu(files, me.getPoint());
               if (menu != null)
                   menu.show(c, me.getX(), me.getY());                  
            }        

            else if (me.getClickCount() == 2)  // Double-click 
            {          
                // Get item index
                int row = _fileTable.rowAtPoint(me.getPoint());
                if (row == -1 || row >= _fileTable.getRowCount())
                    return;
                
                SavannahFileTableElement element = (SavannahFileTableElement) 
                                                 _tableModel.elementAt(row);
                File file = element.getFile();
                if (file != null)
                {
                    if (file.isDirectory())
                    {
                        try {
                            setCurrentDirectory(file);
                        } catch (IOException ioEx) {
                            _logger.error(ioEx.getMessage(), ioEx); //ioEx.printStackTrace();   
                        }
                    }
                    else
                    {
                        //display file info
                        JPanel detailsPanel  = createDetailsPanel(element);
                        JOptionPane.showMessageDialog(
                                        SavannahFileList.this, 
                                        detailsPanel, "File Details",
                                        JOptionPane.PLAIN_MESSAGE);
                        me.consume();
                    }
                }
            } 
        }
    }
    
    //=====================================================================    
    //=====================================================================
    
    class ChangeDirAction extends AbstractAction
    {
        File _dir;
        
        //-----------------------------------------------------------------
        
        public ChangeDirAction(File dir)
        {
            super("Change Directory", _cdIcon);
            
            if (dir == null || !dir.isDirectory())
                throw new IllegalArgumentException("ChangeDirAction"+
                        "::constructor:: Parameter must be a valid "+
                        "directory.");                
            _dir = dir;
            if (!canPerformAction(ACT_CHANGE_DIRECTORY, new Object[] {_dir}))
                this.setEnabled(false);
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            try {
                setCurrentDirectory(_dir);
            } catch (IOException ioEx) {
                _logger.error(ioEx.getMessage(), ioEx); //ioEx.printStackTrace();   
            }                    
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    class RenameAction extends AbstractAction
    {
        String _origName;
        
        //-----------------------------------------------------------------
        
        public RenameAction(File origFile)
        {
            super("Rename", _renameIcon);
            if (origFile == null || !origFile.exists())
                throw new IllegalArgumentException("RenameAction::"+
                        "constructor:: Parameter must be an existing file");
            _origName = origFile.getName();
            
            if (!canPerformAction(ACT_RENAME, new Object[] {origFile}))
                this.setEnabled(false);   
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            String answer = JOptionPane.showInputDialog(
                                SavannahFileList.this,
                                "Rename "+_origName+
                                " to: ", _origName); 
            if (answer != null && !answer.equals("") &&
                    !answer.equals(_origName))
            {
                renameFile(_origName, answer);
            } 
            
            return;
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    class DeleteAction extends AbstractAction
    {
        File[] _files;
        
        //-----------------------------------------------------------------
        
        public DeleteAction(File[] files)
        {
            super("Delete", _deleteIcon);
            
            if (files == null || files.length == 0)
                throw new IllegalArgumentException("DeleteAction::"+
                    "constructor:: Parameter cannot be null or empty.");
            
            _files = new File[files.length];
            for (int i = 0; i < files.length; ++i)
                _files[i] = files[i];
            
            if (!canPerformAction(ACT_DELETE, _files))
                this.setEnabled(false);                       
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            String message;
            if (_files.length > 1)
                message = "Delete these "+_files.length+" items?";
            else
                message = "Delete '"+_files[0].getName()+"'?";
            
            int answer =JOptionPane.showConfirmDialog(
                                SavannahFileList.this,
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
    
    class RefreshAction extends AbstractAction
    {        
        public RefreshAction()
        {
            super("Refresh", _refreshIcon);  
            if (!canPerformAction(ACT_REFRESH, null))
                this.setEnabled(false);   
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
    
    class CreateDirAction extends AbstractAction
    {        
        public CreateDirAction()
        {
            super("Create Directory", _mkdirIcon);  
            if (!canPerformAction(ACT_MAKE_DIRECTORY, null))
                this.setEnabled(false);
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            String answer = JOptionPane.showInputDialog(
                                    SavannahFileList.this,
                                    "New directory name: ", 
                                    ""); 
            if (answer != null && !answer.equals(""))
            {   
                createDirectory(answer);
            }                
            return;
        }
        
        //-----------------------------------------------------------------
    }

    //=====================================================================
    
    class RemoveDirAction extends AbstractAction
    {        
        File _dir;
        
        public RemoveDirAction(File dir)
        {
            super("Remove Directory", _rmdirIcon);  
            if (dir == null || !dir.isDirectory())
                throw new IllegalArgumentException("RemoveDirAction"+
                        "::constructor:: Parameter must be a valid "+
                        "directory.");            
            _dir = dir;
            
            if (!canPerformAction(ACT_REMOVE_DIRECTORY, new Object[] {_dir}))
                this.setEnabled(false);            
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        { 
            int answer = JOptionPane.showConfirmDialog(
                                    SavannahFileList.this,
                                    "Delete directory '"+_dir.getName()+"'?",
                                    "Delete Confirm", 
                                    JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
         
            if (answer == JOptionPane.YES_OPTION)
            {   
                removeDirectory(_dir);
            }     
               
            return;
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
  
    class FileListToolbar extends JPanel implements PropertyChangeListener,
                                                    ListSelectionListener
    {
        SavannahModel _model;
        JPanel _panel;
        JToolBar _toolbar;

        AbstractAction cdAction       = null;    
        AbstractAction renameAction   = null;
        AbstractAction deleteAction   = null;        
        AbstractAction mkdirAction    = null;
        AbstractAction rmdirAction    = null;        
        AbstractAction refreshAction  = null;
        
        
        JButton _cdButton, _renameButton, _deleteButton,            
                _mkdirButton, _rmdirButton,
                _refreshButton;      

        //---------------------------------------------------------------------

        public FileListToolbar(SavannahModel model)
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

        //---------------------------------------------------------------------

        protected void addButtons()
        {
            JButton button;

            //Change diretory
            _cdButton = makeToolbarButton("resources/cd24.gif",
                                    "CHANGE_DIRECTORY",
                                    "Move to selected directory",
                                    "Change Directory");
            _cdButton.setToolTipText("Move to selected directory");
            //_cdButton.setAction(null);
            _toolbar.add(_cdButton);
            _toolbar.addSeparator();
            
            //Rename     
            _renameButton = makeToolbarButton("resources/rename24.gif",
                                    "Rename Selection",
                                    "Rename single file or directory",
                                    "Rename");
            _renameButton.setToolTipText("Rename file or directory");
            //_renameButton.setAction(null);
            _toolbar.add(_renameButton);
            
            //Delete
            _deleteButton = makeToolbarButton("resources/delete24.gif",
                                    "Delete Selection",
                                    "Delete selected files",
                                    "Delete");
            _deleteButton.setToolTipText("Delete file(s)");
            //_deleteButton.setAction(null);
            _toolbar.add(_deleteButton);
            
            _toolbar.addSeparator();
            
            //------------
            
            
            //Mkdir
            _mkdirButton = makeToolbarButton("resources/mkdir24.gif",
                                        "Create Directory",
                                        "Create new directory",
                                        "Create Directory");
            _mkdirButton.setToolTipText("Create new subdirectory");
            //_mkdirButton.setAction(null);
            _toolbar.add(_mkdirButton);
            

            //Rmdir
            _rmdirButton = makeToolbarButton("resources/rmdir24.gif",
                                "Remove Directory",
                                "Remove directory",
                                "Remove Directory");
            _rmdirButton.setToolTipText("Remove empty directory");
            //_rmdirButton.setAction(null);
            _toolbar.add(_rmdirButton);
                        
            _toolbar.addSeparator();
            
            //------------
                        
            //Refresh
            _refreshButton = makeToolbarButton("resources/refresh24.gif",
                                    "REFESH_LISTING",
                                    "Refresh current listing",
                                    "Refresh");
            _refreshButton.setToolTipText("Refresh current directory");
            //_refreshButton.setAction(null);
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
            
            button.setRolloverEnabled(true);          
            return button;
        }

        //-----------------------------------------------------------------
        
        public void valueChanged(ListSelectionEvent event)
        {
            if (event.getValueIsAdjusting())
                return;
            
            Object[] values = getSelectedFiles();
            boolean canCd, canRename, canDelete, canMkdir, 
                    canRmdir, canRefresh;
            
            canCd      = canPerformAction(ACT_CHANGE_DIRECTORY, values);
            canRename  = canPerformAction(ACT_RENAME, values);
            canDelete  = canPerformAction(ACT_DELETE, values);
            canMkdir   = canPerformAction(ACT_MAKE_DIRECTORY, values);
            canRmdir   = canPerformAction(ACT_REMOVE_DIRECTORY, values);
            canRefresh = canPerformAction(ACT_REFRESH, values);
            
            //cd
            if (canCd)
            {
                _cdButton.setAction(new ChangeDirAction((File) values[0]));
                _cdButton.setToolTipText("Move to selected directory");
                _cdButton.setIcon(_cdIcon);          
                _cdButton.setRolloverIcon(_cdRollIcon);
                _cdButton.setText(null);
                _cdButton.setEnabled(true);
            }
            else
            {
                _cdButton.setEnabled(false);
            }
            
            //rename
            if (canRename)
            {
                _renameButton.setAction(new RenameAction((File) values[0]));
                _renameButton.setToolTipText("Rename file or directory");
                _renameButton.setIcon(_renameIcon);
                _renameButton.setRolloverIcon(_renameRollIcon);
                _renameButton.setText(null);
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
                File[] files = new File[values.length];
                for (int i = 0; i < values.length; ++i)
                    files[i] = (File) values[i];
                
                _deleteButton.setAction(new DeleteAction(files));
                _deleteButton.setToolTipText("Remove selected file(s)");
                _deleteButton.setIcon(_deleteIcon);
                _deleteButton.setRolloverIcon(_deleteRollIcon);
                _deleteButton.setText(null);
                _deleteButton.setEnabled(true);
            }
            else
            {
                _deleteButton.setEnabled(false);
            }
            
            //mkdir
            if (canMkdir)
            {                
                _mkdirButton.setAction(new CreateDirAction());
                _mkdirButton.setToolTipText("Create new sub-directory");
                _mkdirButton.setIcon(_mkdirIcon);
                _mkdirButton.setRolloverIcon(_mkdirRollIcon);
                _mkdirButton.setText(null);
                _mkdirButton.setEnabled(true);
            }
            else
            {
                _mkdirButton.setEnabled(false);
            }
            
            //rmdir
            if (canRmdir)
            {                
                _rmdirButton.setAction(new RemoveDirAction((File) values[0]));
                _rmdirButton.setToolTipText("Remove empty directory");
                _rmdirButton.setIcon(_rmdirIcon);
                _rmdirButton.setRolloverIcon(_rmdirRollIcon);
                _rmdirButton.setText(null);
                _rmdirButton.setEnabled(true);
            }
            else
            {
                _rmdirButton.setEnabled(false);
            }
            
            //refresh
            if (canRefresh)
            {                
                _refreshButton.setAction(new RefreshAction());
                _refreshButton.setToolTipText("Refresh current directory");
                _refreshButton.setIcon(_refreshIcon);
                _refreshButton.setRolloverIcon(_refreshRollIcon);
                _refreshButton.setText(null);
                _refreshButton.setEnabled(true);
            }
            else
            {
                _refreshButton.setEnabled(false);
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
            
            if (propName.equals(""))
            {                
            }

            //--------------------------      
        } 
    }
    
    //=====================================================================    
}

