package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

/**
 * <b>Purpose:</b>
 *  Panel displaying local directory structure as tree for navigation.
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
 * 09/07/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahDirectoryPanel.java,v 1.7 2009/09/04 17:05:15 ntt Exp $
 *
 */

public class SavannahDirectoryPanel extends JPanel 
                                                implements PropertyChangeListener
{
    private final String __classname = "SavannahDirectoryPanel";

    protected SavannahModel           _model;
    protected JTree                   _tree; 
    protected DefaultTreeModel        _treeModel;
    protected JScrollPane             _scrollPane;
    protected DefaultTreeCellRenderer _renderer;
    protected File                    _currentDirectory;
    protected DefaultMutableTreeNode  _root;
    protected DefaultMutableTreeNode  _leaf;
    protected FileFilter              _directoryFilter;
    protected MouseListener           _mouseListener;
    
    protected boolean _showChildDirectories;
    
    //---------------------------------------------------------------------
    
    public SavannahDirectoryPanel(SavannahModel model)
    {
        if (model == null)
            throw new IllegalArgumentException(__classname + 
                        "::constructor::Model cannot be null");
        
        this._model = model;

        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        buildGui();
        this._directoryFilter = new DirectoryFileFilter();
        this.setCurrentDirectory(_model.getLocalDirectory());
    }
    
    //---------------------------------------------------------------------
    
    protected void buildGui()
    {
        this._root      = new DefaultMutableTreeNode("Empty");
        this._leaf      = this._root;
        this._treeModel = new DefaultTreeModel(this._root);
        this._tree      = new JTree(this._treeModel);
        
        this._renderer = new DefaultTreeCellRenderer();
        this._renderer.setLeafIcon(this._renderer.getOpenIcon());
        this._showChildDirectories = false;

        this._mouseListener = new PanelMouseListener();
        
        this._tree.setCellRenderer(this._renderer);
        this._tree.setShowsRootHandles(false);
        this._tree.setScrollsOnExpand(true);
        this._tree.addTreeWillExpandListener(new WillExpandListener());
        this._tree.addMouseListener(this._mouseListener);
        
        this._scrollPane = new JScrollPane(this._tree);
        this._scrollPane.addMouseListener(this._mouseListener);
        this.setLayout(new BorderLayout());
        this.add(this._scrollPane, BorderLayout.CENTER);
    }
    
    //---------------------------------------------------------------------
    
    protected void setCurrentDirectory(File dir)
    {
        if (dir == null || !dir.isDirectory())
            return;
            
        if (this._currentDirectory != null && 
                                      this._currentDirectory.equals(dir))
            return;
        
        this._currentDirectory = dir;
        buildTreeFromDir(this._currentDirectory);
    }
    
    //---------------------------------------------------------------------
    
    public void setShowChildDirectories(boolean flag)
    {
        if (flag != this._showChildDirectories)
        {
            this._showChildDirectories = flag;
            Icon leafIcon = (this._showChildDirectories) ? 
                                    this._renderer.getClosedIcon() :
                                    this._renderer.getOpenIcon();
            this._renderer.setLeafIcon(leafIcon);
            buildTreeFromDir(this._currentDirectory);
        }
    }
    
    //---------------------------------------------------------------------
    
    public boolean getShowChildDirectories()
    {
        return this._showChildDirectories;
    }
    
    //---------------------------------------------------------------------
    
    protected void buildTreeFromDir(File dir)
    {   
        //split up directory into separate nodes
        String[] parts = splitPath(dir);
        
        //build tree branch
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(parts[0]);
        DefaultMutableTreeNode node = null;
        DefaultMutableTreeNode parent = root;
        for (int i = 1; i < parts.length; ++i)
        {
            node = new DefaultMutableTreeNode(parts[i]);
            parent.add(node);
            parent = node;
        }
        
        if (this._showChildDirectories)
        {
            File[] childDirs = _currentDirectory.listFiles(_directoryFilter);
            for (int i = 0; i < childDirs.length; ++i)
            {
                node = new DefaultMutableTreeNode(childDirs[i].getName());
                parent.add(node);
            }
        }
        
        this._root = root;
        this._leaf = parent;
        updateTreeModel();
        expandAll();
        
        TreePath leafPath = new TreePath(this._leaf.getPath());
        this._tree.scrollPathToVisible(leafPath);
        this._tree.setSelectionPath(leafPath);
    }
    
    //---------------------------------------------------------------------
    
    protected void updateTreeModel()
    {
        this._treeModel.setRoot(this._root);
        this._treeModel.nodeChanged(this._root);

    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Expands tree to display image types with full details.
     */
    
    public void expandAll()
    {
        for (int j = 0; j <_tree.getRowCount(); ++j)
        {
            _tree.expandRow(j);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Converts the absolute path of a file parameter to a string
     * array containing the path names for each directory.
     * @param file
     * @return String array of directory names forming the file path
     */
    
    protected String[] splitPath(File file)
    {
        File dir = file.getAbsoluteFile();
        
        String[] pathArray = new String[0];
        java.util.List dirList = new ArrayList();
        
        do {
            dirList.add(dir.getName());
        } while ((dir = dir.getParentFile()) != null);
        
        int numParts = dirList.size();
        pathArray = new String[numParts];
        String entry;
        
        for (int i = numParts - 1; i >= 0; --i)
        {
            entry = (String) dirList.get(i);
            pathArray[numParts - 1 - i] = entry;
        }
        
        if (pathArray[0].equals(""))
            pathArray[0] = File.separator;
        
        return pathArray;
    }
    
    //---------------------------------------------------------------------
    
    protected File treePathToFile(TreePath treePath)
    {
        Object[] path = treePath.getPath();
        
        StringBuffer fileBuffer = new StringBuffer(path[0].toString());
        for (int i = 1; i < path.length; ++i)
            fileBuffer.append(path[i].toString()).append(File.separator);
        
        File file = new File(fileBuffer.toString());
        return file;
    }
    
    
    //---------------------------------------------------------------------
    
    /**
     *  Implementation of the PropertyChangeListener interface.  For
     *  interaction with the model.  Method is called whenever
     *  a change is made to a model property.
     *  @param pce A PropertyChangeEvent object describing the event 
     *               source and the property that has changed.
     */
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        if (propName.equalsIgnoreCase("LOCAL_DIRECTORY"))
        {
            File curDir = (File) pce.getNewValue();
            setCurrentDirectory(curDir);
        }

    }
    
    //---------------------------------------------------------------------   
    
    protected void selectDirectoryPath(TreePath treepath)
    {
        File dir = treePathToFile(treepath);
        
         if (dir.exists() && dir.isDirectory())
             _model.setLocalDirectory(dir);
    }
    
    //=====================================================================
    //=====================================================================
    
    class DirectoryFileFilter implements FileFilter
    {
        public boolean accept(File pathname)
        {
            return (pathname.canRead() && pathname.isDirectory());
        }
    }
    
    //=====================================================================
    
    class WillExpandListener implements TreeWillExpandListener
    {
        public void treeWillExpand(TreeExpansionEvent ev)
                                                    throws ExpandVetoException
        {
            //do nothing
        }
        
        //-----------------------------------------------------------------  
        
        public void treeWillCollapse(TreeExpansionEvent ev) 
                                                    throws ExpandVetoException
        {
            //cancel collapse, instead, treat as folder selection
            throw new ExpandVetoException(ev);
        }
        
        
    }
    
    //=====================================================================
    
    class PanelMouseListener extends MouseAdapter
    {   
        //-----------------------------------------------------------------  
        
        public void mouseClicked(MouseEvent me) 
        {
            if (!(me.getSource() instanceof JComponent))
                return;
            
            JComponent c = (JComponent) me.getSource();
            
            // if right mouse button clicked (or me.isPopupTrigger())
            if (SwingUtilities.isRightMouseButton(me))           
            { 
                JPopupMenu menu = preparePopup();
                if (menu != null)
                    menu.show(c, me.getX(), me.getY()); 
            }        

            //----------------------
            
            else if (me.getClickCount() == 2)  // Double-click 
            {          
                TreePath nodePath = _tree.getClosestPathForLocation(
                                            me.getX(), me.getY());
                if (nodePath == null)
                    return;
                
                selectDirectoryPath(nodePath);
            } 
        }
        
        //-----------------------------------------------------------------  
        
        protected JPopupMenu preparePopup()
        {
            JPopupMenu popupMenu = new JPopupMenu();
            JCheckBoxMenuItem showChildMenuItem;
            showChildMenuItem = new JCheckBoxMenuItem(
                                "Show current directory children");
            showChildMenuItem.setSelected(_showChildDirectories);
            showChildMenuItem.setToolTipText("Enable/disable display of "+
                                             "current directory's child "+
                                             "directories");
            showChildMenuItem.addActionListener(new ActionListener(){      
                public void actionPerformed(ActionEvent ae)
                {
                    JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) 
                                                 ae.getSource();
                    boolean state = menuItem.isSelected();
                    setShowChildDirectories(state);
                }
            });
           
            JLabel titleLabel = new JLabel(" Options");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD | Font.ITALIC));
            popupMenu.add(titleLabel);
            popupMenu.addSeparator();
            popupMenu.add(showChildMenuItem);
            popupMenu.setSelected(null);
            popupMenu.invalidate();
            
            return popupMenu;
        }
    }
    
    
    //=====================================================================
        
    public static void main(String[] args)
    {
        SavannahModel model = new SavannahModel();
        
        SavannahDirectoryPanel panel = new SavannahDirectoryPanel(model);
        
        JFrame frame = new JFrame("Testing...");
        frame.getContentPane().add(panel);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        
        try {
            Thread.sleep(5000);
        } catch (Exception ex) {
            
            ex.printStackTrace();
        }
        
        File temp = model.getLocalDirectory();
        model.setLocalDirectory(temp.getParentFile());
        
        try {
            Thread.sleep(5000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        model.setLocalDirectory(temp);
    }
}
