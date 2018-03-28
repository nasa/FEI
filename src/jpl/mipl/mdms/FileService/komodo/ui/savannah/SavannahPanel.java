package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.SetDateTimeFilterAction;

/**
 * <b>Purpose: </b>
 * Main panel for the Savannah application.
 * 
 * <PRE>
 * Copyright 2004, California Institute of Technology. 
 * ALL RIGHTS RESERVED. U.S.
 * Government Sponsorship acknowledge. 2004.
 * </PRE>
 * 
 * <PRE> 
 * ============================================================================
 * <B>Modification History : </B> ----------------------
 * 
 * <B>Date             Who             What </B>
 * ----------------------------------------------------------------------------
 * 06/02/2004       Nick            Initial Release
 * ============================================================================
 * </PRE>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahPanel.java,v 1.41 2013/04/08 15:31:47 ntt Exp $
 *  
 */

public class SavannahPanel extends JPanel implements PropertyChangeListener {
    
   private final String __classname = "SavannahPanel";

   protected JLabel _statusLabel;
   protected JMenuBar _menuBar;
   protected JPanel _centralPanel;
   protected JPanel _northPanel;
   protected JPanel _southPanel;
   protected JPanel _menuPanel;
   protected SavannahModel _model;
   protected SavannahFilterModel _filterModel;

   protected JBoundedComboBox _curDirectoryBox;
   protected JBoundedComboBox _localFilenameFilterBox;
   protected SavannahFileFilter _localFileFilter;
   protected JCheckBox _localFilterEnabledBox;
   protected JBoundedComboBox _feiFilenameFilterBox;
   protected JCheckBox _feiFilterEnabledBox;
   protected JCheckBox _feiFiletypesBox;
      
   protected JButton _feiDateFilterButton;
   protected ImageIcon dateFilterOnIcon, dateFilterOffIcon;
   
   protected JLabel _curServerLabel;
   protected JLabel _curTypeLabel;
   protected JLabel _feiTypeUserLabel;
   protected JComboBox _feiServerCombo;
   protected ActionListener _feiServerComboListener;
   protected JButton _connectFeiServerButton;
   protected JButton _disconnectFeiServerButton;

   protected Color _connColor = new Color(10, 200, 10);
   protected Color _noConnColor = new Color(200, 10, 10);

   protected String _iconPath = SavannahModel.ICON_PATH;
   protected String _notApplicString = "N/A";

   protected Cursor _hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
   protected Cursor _normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
   protected RootPaneContainer _rootPane = null;
   protected JProgressBar _busyBar;
   protected MouseAdapter _disableMouseAdapter = null;

   //---------------------------------------------------------------------

   public SavannahPanel() {
      //create new application model
      _model = new SavannahModel();
      _filterModel = _model.getFilterModel();

      //register this as a property listener to app model
      _model.addPropertyChangeListener(this);

      //set this as model's relative comp for error messages
      _model.setRelativeComponent(this);

      //create instance of file filter
      this._localFileFilter = new SavannahFileFilter(
                              _filterModel.getPattern("LOCAL_FILTER"));                    

      //construct the gui
      buildGui();
   }

   //---------------------------------------------------------------------

   protected void buildGui() {
      this.setLayout(new BorderLayout());

      //build component panels
      _northPanel = buildNorthPanel();
      _centralPanel = buildCentralPanel();
      _southPanel = buildSouthPanel();

      //add panels to this
      this.add(_northPanel, BorderLayout.NORTH);
      this.add(_centralPanel, BorderLayout.CENTER);
      this.add(_southPanel, BorderLayout.SOUTH);
   }

   //---------------------------------------------------------------------

   /*
    * Will contain control menu for local host info, current server info, server
    * list, connect button
    *  
    */

   protected JPanel buildNorthPanel() {
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BorderLayout());
      this._menuPanel = buildMenuPanel();
      JPanel controlPanel = buildNorthControlPanel();
      mainPanel.add(this._menuPanel, BorderLayout.NORTH);
      mainPanel.add(controlPanel, BorderLayout.CENTER);
      mainPanel.add(new JSeparator(), BorderLayout.SOUTH);
      return mainPanel;
   }

   //---------------------------------------------------------------------

   protected JPanel buildMenuPanel() {
      JPanel menuPanel = new JPanel();
      menuPanel.setLayout(new BorderLayout());

      String aboutString = SavannahModel.APPLICATION_TITLE + " v"
            + SavannahModel.VERSION_ID + "\n" + SavannahModel.KOMODO_VERSION
            + "\nMission Data Management Service (MDMS)" + "\n\n"
            + SavannahModel.COPYRIGHT + "\n\n";

      this._menuBar = new SavannahMenu(_model, SavannahPanel.this, aboutString);

      menuPanel.add(this._menuBar, BorderLayout.CENTER);
      return menuPanel;
   }

   //---------------------------------------------------------------------

   protected JPanel buildNorthControlPanel() {
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
      mainPanel.setBorder(BorderFactory.createEmptyBorder());

      //------------------------

      //Local host information
      JPanel localHostPanel = buildLocalPanel();

      TitledBorder titleBorder = BorderFactory.createTitledBorder(
              BorderFactory.createEmptyBorder(), "Local", 
              TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION);
      setTitledBorderBold(titleBorder);
      //titleBorder.setTitleFont(titleBorder.getTitleFont().deriveFont(Font.BOLD));
      localHostPanel.setBorder(titleBorder);
      mainPanel.add(localHostPanel);
      mainPanel.add(new JSeparator(SwingConstants.VERTICAL));

      //------------------------
      //------------------------
      //------------------------

      //Server selection/display

      JPanel feiServerPanel = buildServerPanel();

      titleBorder = BorderFactory.createTitledBorder(
                      BorderFactory.createEmptyBorder(), 
                      "FEI Service", TitledBorder.CENTER,
                      TitledBorder.DEFAULT_POSITION);
      //titleBorder.setTitleFont(titleBorder.getTitleFont().deriveFont(Font.BOLD));
      setTitledBorderBold(titleBorder);
      feiServerPanel.setBorder(titleBorder);

      mainPanel.add(feiServerPanel);
      //mainPanel.add(Box.createHorizontalGlue());
      mainPanel.add(new JSeparator(SwingConstants.VERTICAL));

      //------------------------
      //ICON

      java.net.URL iconURL = SavannahPanel.class.getResource(_iconPath);
      if (iconURL != null) {
         ImageIcon icon = new ImageIcon(iconURL);
         if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            //mainPanel.add(Box.createHorizontalGlue());
            mainPanel.add(iconLabel);
            mainPanel.add(Box.createHorizontalStrut(5));
         }
      }

      //------------------------

      return mainPanel;
   }
   
   //---------------------------------------------------------------------
   
   protected void setTitledBorderBold(TitledBorder titledBorder)
   {
       if (titledBorder == null)
           return;
           
       Font origfont = titledBorder.getTitleFont();
       if (origfont == null)
       {
           origfont = UIManager.getDefaults().getFont("TitledBorder.font");
       }
       
       if (origfont != null)
       {
           Font boldfont = origfont.deriveFont(Font.BOLD);
           titledBorder.setTitleFont(boldfont);
       }
   }

   //---------------------------------------------------------------------

   protected JPanel buildLocalPanel() {
      JPanel panel = new JPanel();

      GridBagConstraints gridBagConstraints;

      JLabel userLabel = new JLabel("User: ");
      JLabel curUserLabel = new JLabel();
      JLabel directoryLabel = new JLabel("Directory: ");
      _curDirectoryBox = new JBoundedComboBox();
      _localFilterEnabledBox = new JCheckBox();
      _localFilenameFilterBox = new JBoundedComboBox();
      _feiTypeUserLabel = new JLabel();

      panel.setLayout(new java.awt.GridBagLayout());

      //User: label
      userLabel.setHorizontalTextPosition(SwingConstants.LEFT);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
      gridBagConstraints.weighty = .5;
      gridBagConstraints.insets = new Insets(3, 17, 0, 0);
      panel.add(userLabel, gridBagConstraints);

      //Current user label
      curUserLabel.setText(System.getProperty("user.name", _notApplicString));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.insets = new Insets(3, 0, 0, 0);
      panel.add(curUserLabel, gridBagConstraints);

      int secondLevelTopInset = 16;
      int secondLevelBottomInset = 11;
      
      //Directory: label
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      //gridBagConstraints.weighty = .5;
      gridBagConstraints.anchor = GridBagConstraints.WEST;
      gridBagConstraints.insets = new Insets(secondLevelTopInset, 17, secondLevelBottomInset, 0);
      panel.add(directoryLabel, gridBagConstraints);

      //Current directory Box:
      _curDirectoryBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            String txt = _curDirectoryBox.getSelectedItem().toString();
            File file = new File(txt);
            if (!_model.getLocalDirectory().equals(file)) {
               if (file.isDirectory() && file.canRead()) {
                  _model.setLocalDirectory(file);
                  _curDirectoryBox.setSelectedItem(_model.getLocalDirectory()
                        .getAbsolutePath());
               } else {
                  JOptionPane.showMessageDialog(SavannahPanel.this,
                        "Directory '" + txt + "' does not exist.",
                        "Directory Warning", JOptionPane.WARNING_MESSAGE);
                  _curDirectoryBox.setSelectedItem(_model.getLocalDirectory()
                        .getAbsolutePath());
               }
            }
         }
      });
      _curDirectoryBox.setPreferredSize(new Dimension(60, _curDirectoryBox
            .getPreferredSize().height));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.gridwidth = 3;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 0.5;
      //gridBagConstraints.weighty = .5;
      gridBagConstraints.insets = new Insets(secondLevelTopInset, 0, secondLevelBottomInset, 12);
      panel.add(_curDirectoryBox, gridBagConstraints);

      //File Filter Check Box:
      _localFilterEnabledBox.setText("File Filter: ");
      _localFilterEnabledBox.setToolTipText("Enable/disable file filtering");
      _localFilterEnabledBox.setSelected(_filterModel.isEnabled("LOCAL_FILTER"));
      _localFilterEnabledBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            JCheckBox cb = (JCheckBox) ae.getSource();
            boolean isSelected = cb.isSelected();
            boolean wasSelected = _filterModel.isEnabled("LOCAL_FILTER");
            if (isSelected != wasSelected)
                _model.getFilterModel().setEnabled("LOCAL_FILTER", isSelected);
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = GridBagConstraints.SOUTH;
      gridBagConstraints.weighty = .9;
      gridBagConstraints.insets = new Insets(0, 13, 3, 0);
      panel.add(_localFilterEnabledBox, gridBagConstraints);

      //Local filename filter box:
      _localFilenameFilterBox.setSelectedItem(_filterModel.getPattern("LOCAL_FILTER"));
      _localFilenameFilterBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            String txt = _localFilenameFilterBox.getSelectedItem().toString();
            if (!txt.equals(_filterModel.getPattern("LOCAL_FILTER"))) {
               _filterModel.setPattern("LOCAL_FILTER", txt);
               _localFilenameFilterBox.setSelectedItem(
                                       _filterModel.getPattern("LOCAL_FILTER"));
            }
         }
      });
      _localFilenameFilterBox.setPreferredSize(new Dimension(20,
            _localFilenameFilterBox.getPreferredSize().height));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.gridwidth = 3;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.anchor = GridBagConstraints.SOUTH;
      gridBagConstraints.weighty = .5;
      gridBagConstraints.insets = new Insets(0, 0, 3, 12);
      panel.add(_localFilenameFilterBox, gridBagConstraints);

      //User logged in as label:
      _feiTypeUserLabel.setText("");
      _feiTypeUserLabel.setEnabled(false);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
      gridBagConstraints.anchor = GridBagConstraints.WEST;
      gridBagConstraints.insets = new Insets(3, 14, 0, 5);
      panel.add(_feiTypeUserLabel, gridBagConstraints);

      return panel;
   }

   //---------------------------------------------------------------------

   protected JPanel buildServerPanel() 
   {
      JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      GridBagConstraints gridBagConstraints;
      
      _feiServerCombo = new JComboBox();
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
      URL imageURL = SavannahPanel.class.getResource(
                                          "resources/connect24.png");            
      ImageIcon connectIcon = new ImageIcon(imageURL, "Connect");
      imageURL = SavannahPanel.class.getResource(
                                          "resources/disconnect24.png");            
      ImageIcon disconnectIcon = new ImageIcon(imageURL, "Disconnect");
      
      _connectFeiServerButton = new JButton(connectIcon);
      _connectFeiServerButton.setMargin(null);
      _disconnectFeiServerButton = new JButton( disconnectIcon);

      buttonPanel.add(_connectFeiServerButton);
      buttonPanel.add(Box.createHorizontalStrut(20));
      buttonPanel.add(_disconnectFeiServerButton);
      
      
//      _connectFeiServerButton = new JButton("Connect");
//      _disconnectFeiServerButton = new JButton("Disconnect");
      _disconnectFeiServerButton.setEnabled(false);
      JLabel connectedToLabel = new JLabel("Connected to: ");
      JLabel filetypeLabel = new JLabel("Filetype: ");

      _curServerLabel = new JLabel();
      _curTypeLabel = new JLabel();
      _feiFilterEnabledBox = new JCheckBox();
      _feiFilenameFilterBox = new JBoundedComboBox();
      _feiFiletypesBox = new JCheckBox();

      //---------------------------
      //fei date filter button
      
      _feiDateFilterButton = new JButton("Date Filter");
      URL iconUrl = SavannahPanel.class.getResource(
              "resources/enabled_box.png");            
      this.dateFilterOnIcon = new ImageIcon(iconUrl, "Enabled");
      iconUrl = SavannahPanel.class.getResource(
              "resources/disabled_box.png");            
      this.dateFilterOffIcon = new ImageIcon(iconUrl, "Disabled");
      _feiDateFilterButton.setIcon(dateFilterOffIcon);
          
      //---------------------------
      
      //FEI Server Combo Box
      _feiServerCombo.setEditable(false);
      java.util.List feiServers = _model.getAvailableFeiServers();
      int numServers = feiServers.size();
      for (int i = 0; i < numServers; ++i) {
         _feiServerCombo.addItem(feiServers.get(i));
      }
      _feiServerComboListener = new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            String newServer = (String) _feiServerCombo.getSelectedItem();
            String curServer = _model.getCurrentFeiServer();
            if (newServer == null
                  || (curServer != null && _model.getCurrentFeiServer().equals(
                        newServer))) {
               _connectFeiServerButton.setEnabled(false);
               _disconnectFeiServerButton.setEnabled(true);
            } else {
               _connectFeiServerButton.setEnabled(true);
               _disconnectFeiServerButton.setEnabled(false);
            }
         }
      };
      _feiServerCombo.addActionListener(_feiServerComboListener);
      //_feiServerCombo.setPreferredSize(new java.awt.Dimension(140, 19));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.gridwidth = 2;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.insets = new Insets(1, 4, 7, 6);
      panel.add(_feiServerCombo, gridBagConstraints);

      //---------------------------
      
      // Connect to Fei Server Button
      _connectFeiServerButton.setToolTipText("Connect to server group");
      _connectFeiServerButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            String newServer = (String) _feiServerCombo.getSelectedItem();
            String curServer = _model.getCurrentFeiServer();
            if (newServer == null
                  || (curServer != null && curServer.equals(newServer))) {
               //no server selected or its the same server, ignore?
               return;
            } else {                
               _model.setCurrentFeiGroup(newServer);
            }
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.gridwidth = 2;
      gridBagConstraints.fill = GridBagConstraints.NONE;
      gridBagConstraints.weightx = 0.5;
      gridBagConstraints.insets = new Insets(0, 5, 7, 5);
      panel.add(buttonPanel, gridBagConstraints);
      //panel.add(_connectFeiServerButton, gridBagConstraints);
      
      //-----------------------------
      
      //  Disconnect to FEI server button
      _disconnectFeiServerButton.setToolTipText("Disconnect from current server group");
      _disconnectFeiServerButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
              String sg = _model.getCurrentFeiServer();
              if (sg != null)
              {
                  int answer = JOptionPane.showConfirmDialog(
                              SavannahPanel.this, 
                              "Disconnecting from servergroup '"+sg+"' will " +
                              "close connection\nand clear password.  Proceed?", 
                              "Confirm Disconnect", 
                              JOptionPane.OK_CANCEL_OPTION, 
                              JOptionPane.QUESTION_MESSAGE);
                  if (answer == JOptionPane.CANCEL_OPTION)
                      return;
              }
              
              _model.unsetCurrentFeiGroup();
              //_model.setCurrentFeiGroup(null);
          }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 4;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.gridwidth = 2;
      gridBagConstraints.fill = GridBagConstraints.NONE;
      gridBagConstraints.weightx = 0.5;
      gridBagConstraints.insets = new Insets(0, 5, 7, 5);
      //panel.add(_disconnectFeiServerButton, gridBagConstraints);
      
      //---------------------------
      int secondLevelTopInset = 10;
      int secondLevelBottomInset = 0;
      
      JPanel panel2 = new JPanel();
      panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
      
      
      //Connected to Label
      connectedToLabel.setHorizontalAlignment(SwingConstants.CENTER);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 0.0;//0.5;
      gridBagConstraints.insets = new Insets(secondLevelTopInset, 6, secondLevelBottomInset, 0);//(1, 0, 0, 0);
      //panel.add(connectedToLabel, gridBagConstraints);      

      //---------------------------
            
      //Filetype label
      filetypeLabel.setHorizontalAlignment(SwingConstants.CENTER);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 0.0;      
      //gridBagConstraints.weightx = 0.5;
      gridBagConstraints.insets = new Insets(secondLevelTopInset, 6, secondLevelBottomInset, 0);
      //panel.add(filetypeLabel, gridBagConstraints);
      
      //---------------------------
      
      //Current server label
      _curServerLabel.setText(_notApplicString);
      _curServerLabel.setForeground(_noConnColor);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 0.5;
      gridBagConstraints.insets = new Insets(secondLevelTopInset, 0, secondLevelBottomInset, 4);
      //panel.add(_curServerLabel, gridBagConstraints);


      //---------------------------
      
      //Current FEI type label
      _curTypeLabel.setText(_notApplicString);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 3;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 0.0;//      gridBagConstraints.weightx = 0.5;
      gridBagConstraints.insets = new Insets(secondLevelTopInset, 0, secondLevelBottomInset, 0);
      //panel.add(_curTypeLabel, gridBagConstraints);

      
      panel2.add(connectedToLabel);
      panel2.add(_curServerLabel);
      panel2.add(Box.createHorizontalStrut(35));
      panel2.add(filetypeLabel);
      panel2.add(_curTypeLabel);
      panel2.add(Box.createHorizontalGlue());
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.gridwidth = 4;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.weightx = 0.5;//      gridBagConstraints.weightx = 0.5;
      gridBagConstraints.insets = new Insets(1, 0, 0, 0);
      panel.add(panel2, gridBagConstraints);
      
      //---------------------------
      
      //File filter enabled box
      _feiFilterEnabledBox.setText("File Filter");
      _feiFilterEnabledBox.setToolTipText("Enable/disable file filtering");
      _feiFilterEnabledBox.setSelected(_filterModel.isEnabled("FEI_FILTER"));      
      _feiFilterEnabledBox.setEnabled(this._model.getCurrentFeiType()!=null);
      _feiFilterEnabledBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            JCheckBox cb = (JCheckBox) ae.getSource();
            boolean isSelected = cb.isSelected();
            boolean wasSelected = _filterModel.isEnabled("FEI_FILTER");
            if (isSelected != wasSelected)
                _filterModel.setEnabled("FEI_FILTER", isSelected);
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = GridBagConstraints.SOUTH;
      gridBagConstraints.weighty = .5;
      gridBagConstraints.insets = new Insets(7, 0, 1, 0);
      panel.add(_feiFilterEnabledBox, gridBagConstraints);

      //---------------------------

      //Filetype filter enabled box      
      _feiFiletypesBox.setText("Filetype Filter:");
      _feiFiletypesBox.setToolTipText("Enable/disable filetype filtering");
      _feiFiletypesBox.setSelected(_filterModel.isEnabled("FEI_FT_FILTER"));
      _feiFiletypesBox.setEnabled(this._model.getCurrentFeiType()==null);
      _feiFiletypesBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            JCheckBox cb = (JCheckBox) ae.getSource();
            boolean isSelected = cb.isSelected();
            boolean wasSelected = _filterModel.isEnabled("FEI_FT_FILTER");
            if (isSelected != wasSelected)
                _filterModel.setEnabled("FEI_FT_FILTER", isSelected);
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = GridBagConstraints.SOUTH;
      gridBagConstraints.weighty = .5;
      gridBagConstraints.insets = new Insets(7, 0, 1, 0);
      panel.add(_feiFiletypesBox, gridBagConstraints);
      
      //---------------------------
      
      
      //FEI filename filter bounded box:
      _feiFilenameFilterBox.setSelectedItem(_filterModel.getPattern("FEI_FILTER"));
      _feiFilenameFilterBox.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
             
            String boxText = _feiFilenameFilterBox.getSelectedItem().toString();
            
            if (_feiFilterEnabledBox.isEnabled())
            {
                if (!boxText.equals(_filterModel.getPattern("FEI_FILTER")))
                {
                    _filterModel.setPattern("FEI_FILTER", boxText);
                    _feiFilenameFilterBox.setSelectedItem(
                            _filterModel.getPattern("FEI_FILTER"));
                }
            }
            if (_feiFiletypesBox.isEnabled())
            {
                if (!boxText.equals(_filterModel.getPattern("FEI_FT_FILTER")))
                {
                    _filterModel.setPattern("FEI_FT_FILTER", boxText);
                    _feiFilenameFilterBox.setSelectedItem(
                            _filterModel.getPattern("FEI_FT_FILTER"));
                }
            }         
         }
      });
      _feiFilenameFilterBox.setPreferredSize(new Dimension(20,
            _feiFilenameFilterBox.getPreferredSize().height));
      gridBagConstraints = new GridBagConstraints();
      //gridBagConstraints.gridx = 1;      
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
      gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
      gridBagConstraints.anchor = GridBagConstraints.CENTER;
      gridBagConstraints.weightx = 0.5;
      gridBagConstraints.anchor = GridBagConstraints.SOUTH;
      gridBagConstraints.weighty = .5;
      gridBagConstraints.insets = new Insets(7, 0, 1, 1);
      panel.add(_feiFilenameFilterBox, gridBagConstraints);
      
      
      //---------------------------
      
    
              
      _feiDateFilterButton.addActionListener(
             new SetDateTimeFilterAction(this, this._model));
      SavannahDateFilterModel dateModel = _model.getDateFilterModel();
      dateModel.addFilterListener(new SavannahDateFilterListener() {
          public void filterChanged(DateFilter filter)
          {
              boolean enabled = filter == null ? true :
                                filter.getMode() != DateFilter.MODE_OFF;
              Icon newIcon = enabled ? dateFilterOnIcon : dateFilterOffIcon;              
              _feiDateFilterButton.setIcon(newIcon);                          
          }          
      });
              
        
//      _feiDateFilterButton.setPreferredSize(new Dimension(20,
//              _feiDateFilterButton.getPreferredSize().height));
//      _feiDateFilterButton.setPreferredSize(_feiFilterEnabledBox.getPreferredSize());
//      
      gridBagConstraints = new GridBagConstraints();
      //gridBagConstraints.gridx = 1;      
      gridBagConstraints.gridx = 4;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
      gridBagConstraints.fill = GridBagConstraints.NONE;
      gridBagConstraints.anchor = GridBagConstraints.CENTER;
      gridBagConstraints.weightx = 0.0;
      //gridBagConstraints.anchor = GridBagConstraints.SOUTH;
      gridBagConstraints.weighty = .0;
      gridBagConstraints.insets = new Insets(8, 7, 1, 1);
      panel.add(_feiDateFilterButton, gridBagConstraints);
      
   
              
      //---------------------------
      
      return panel;
   }

   //---------------------------------------------------------------------

   protected JPanel buildSouthPanel() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(null);
      panel.add(new JSeparator(), BorderLayout.NORTH);

      _statusLabel = new JLabel(" Status");
      panel.add(_statusLabel, BorderLayout.WEST);
      panel.add(Box.createHorizontalGlue());

      JPanel busyPanel = new JPanel(new BorderLayout());
      busyPanel.setBorder(null);
      _busyBar = new JProgressBar();
      _busyBar.setIndeterminate(_model.isBusy());
      _busyBar.setToolTipText("Transfer Indicator");
      _busyBar.setMaximumSize(new Dimension(25, (int) _busyBar.getMaximumSize()
            .getHeight()));
      busyPanel.add(_busyBar, BorderLayout.WEST);
      busyPanel.add(Box.createHorizontalStrut(15), BorderLayout.EAST);
      panel.add(busyPanel, BorderLayout.EAST);
      return panel;
   }

   //---------------------------------------------------------------------

   protected JPanel buildCentralPanel() {
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      SavannahFileList localHostPanel = null;

      try {
         localHostPanel = new SavannahFileList(_model);
         _curDirectoryBox.setSelectedItem(localHostPanel.getCurrentDirectory()
               .getAbsolutePath());
      } catch (IOException ioEx) {
         ioEx.printStackTrace();
         System.exit(1);
      }

      SavannahFeiList feiTypePanel = null;
      try {
         feiTypePanel = new SavannahFeiList(_model);
      } catch (IOException ioEx) {
         ioEx.printStackTrace();
         System.exit(1);
      }

      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
            .createEmptyBorder(), "Local Filesystem"));
      leftPanel.add(localHostPanel, BorderLayout.CENTER);

      //ClickNDrop Panel goes here
      JPanel lefterPanel = new JPanel(new BorderLayout());
      SavannahClickNDropPanel cndPanel = new SavannahClickNDropPanel(_model,
            localHostPanel, feiTypePanel);
      lefterPanel.add(leftPanel, BorderLayout.CENTER);
      lefterPanel.add(cndPanel, BorderLayout.EAST);

      JPanel rightPanel = new JPanel(new BorderLayout());
      rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
            .createEmptyBorder(), "FEI Filesystem"));
      rightPanel.add(feiTypePanel, BorderLayout.CENTER);

      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            lefterPanel, rightPanel);
      splitPane.setOneTouchExpandable(false);
      splitPane.setDividerLocation(400);
      splitPane.setBorder(null);
      splitPane.setResizeWeight(.5);

      //  Provide minimum sizes for the two components in the split pane
      Dimension minimumSize = new Dimension(100, 50);
      localHostPanel.setMinimumSize(minimumSize);
      feiTypePanel.setMinimumSize(minimumSize);
      panel.add(splitPane, BorderLayout.CENTER);

      return panel;
   }

   //---------------------------------------------------------------------

   /**
    * Implementation of the PropertyChangeListener interface. For interaction
    * with the model. Method is called whenever a change is made to a model
    * property.
    * 
    * @param evt A PropertyChangeEvent object describing the event source and
    *           the property that has changed.
    */
   public void propertyChange(PropertyChangeEvent pce) {
      String propName = pce.getPropertyName();

      //--------------------------------

      if (propName.equalsIgnoreCase("STATUS_MESSAGE")) {
         String newMesg = (String) pce.getNewValue();
         if (newMesg == null)
            newMesg = "";
         if (_statusLabel != null && !_statusLabel.getText().equals(newMesg)) 
         {
            _statusLabel.setText(" " + newMesg);
            Dimension dim = _statusLabel.getSize();
            _statusLabel.paintImmediately(0, 0, dim.width, dim.height);
         }
      }

      //--------------------------------

      else if (propName.equalsIgnoreCase("CURRENT_FEI_GROUP")) {
         String newGroup = (String) pce.getNewValue();
         Color fgColor = _connColor;
         boolean enableConnButton = true;

         if (newGroup == null || newGroup.equals("")) {
            newGroup = _notApplicString;
            fgColor = _noConnColor;
         }
         if (newGroup.equals(_feiServerCombo.getSelectedItem())) {
            enableConnButton = false;
         }
         if (!_curServerLabel.getText().equals(newGroup)) {
            _curServerLabel.setText(newGroup);
            _curServerLabel.setForeground(fgColor);
            _connectFeiServerButton.setEnabled(enableConnButton);
            _disconnectFeiServerButton.setEnabled(!enableConnButton);
         }
      }

      //--------------------------------

      else if (propName.equalsIgnoreCase("CURRENT_FEI_TYPE")) {
         String newType = (String) pce.getNewValue();
         String userLoginInfo = "";

         if (newType == null || newType.equals("")) {
            newType = _notApplicString;
         }
         if (!_curTypeLabel.getText().equals(newType)) {
            _curTypeLabel.setText(newType);            
         }

         //update parts of the panel based on cur filetype state
         if (_model.getCurrentFeiType() != null) {
             
             //print login user info thats connected
            userLoginInfo = "(logged in as " + _model.getUsername() + ")";
            
            //enable file filtering, disable ft filtering, update filter pat
            _feiFilterEnabledBox.setEnabled(true);
            _feiFiletypesBox.setEnabled(false);
            _feiFilenameFilterBox.setSelectedItem(_filterModel.getPattern("FEI_FILTER"));
         }
         else
         {
             //disable file filtering, enable ft filtering, update filter pat
             _feiFilterEnabledBox.setEnabled(false);
             _feiFiletypesBox.setEnabled(true);
             _feiFilenameFilterBox.setSelectedItem(_filterModel.getPattern("FEI_FT_FILTER"));
         }
         _feiTypeUserLabel.setText(userLoginInfo);
      }

      //--------------------------------

      else if (propName.equalsIgnoreCase("LOCAL_DIRECTORY")) {
         File newDir = (File) pce.getNewValue();
         String dirName;
         if (newDir != null)
            dirName = newDir.getAbsolutePath();
         else
            dirName = "N/A";

         if (_curDirectoryBox != null) {
            Object entry = _curDirectoryBox.getSelectedItem();
            if (entry != null) {
               if (!dirName.equals(entry.toString())) {
                  _curDirectoryBox.setSelectedItem(dirName);
                  _model.setStatusMessage("Moved to " + dirName);
               }
            }
         }
      }

      //--------------------------------

      else if (propName.equalsIgnoreCase("AVAILABLE_FEI_SERVERS")) {
         List feiServers = (List) pce.getNewValue();

         int numServers = feiServers.size();
         _feiServerCombo.setEnabled(false);
         _feiServerCombo.removeActionListener(_feiServerComboListener);
         _feiServerCombo.removeAllItems();
         for (int i = 0; i < numServers; ++i) {
            _feiServerCombo.addItem(feiServers.get(i));
         }
         _feiServerCombo.addActionListener(_feiServerComboListener);
         _feiServerCombo.setEnabled(true);
      }

      //--------------------------------

      else if (propName.equalsIgnoreCase("IS_BUSY")) {
         boolean isBusy = ((Boolean) pce.getNewValue()).booleanValue();
         if (_busyBar.isIndeterminate() != isBusy) {
            _busyBar.setIndeterminate(isBusy);

            if (_rootPane == null)
               _rootPane = (RootPaneContainer) this.getTopLevelAncestor();
            if (_disableMouseAdapter == null)
               _disableMouseAdapter = new MouseAdapter() {
               };

            if (isBusy) {
               _rootPane.getGlassPane().setCursor(_hourglassCursor);
               _rootPane.getGlassPane().addMouseListener(_disableMouseAdapter);
               _rootPane.getGlassPane().setVisible(true);
            } else {
               _rootPane.getGlassPane().setCursor(_normalCursor);
               _rootPane.getGlassPane().removeMouseListener(
                     _disableMouseAdapter);
               _rootPane.getGlassPane().setVisible(false);
            }
         }
      }

      //--------------------------------

      else if (propName.equalsIgnoreCase("FILENAME_FILTER")) {
         String newFilter = (String) pce.getNewValue();
         String oldFilter = this._localFileFilter.getExpression();

         if (newFilter == null)
            newFilter = "";
         if (oldFilter == null)
            oldFilter = "";

         if (_localFilenameFilterBox != null) {
            Object entry = _localFilenameFilterBox.getSelectedItem();
            if (entry != null) {
               if (!newFilter.equals(entry.toString())) {
                  _localFilenameFilterBox.setSelectedItem(newFilter);
                  _model.setStatusMessage("Filter changed to " + newFilter);
               }
            }
         }
      }

      //--------------------------------

      else if (propName.equalsIgnoreCase("FILENAME_FILTER_ENABLED")) {
         boolean flag = ((Boolean) pce.getNewValue()).booleanValue();
         this._localFilterEnabledBox.setSelected(flag);
      }

      //--------------------------------
   }

   //---------------------------------------------------------------------

   /**
    * Returns a WindowListener to handle control of window closing events from
    * the Parent component to this component.
    * 
    * @return WindowListener To handle window closing events.
    */

   public WindowListener getWindowCloseListener() {
      WindowListener listener = new WindowAdapter() {
         public void windowClosing(WindowEvent we) {
            _model.setStatusMessage("Confirming application exit...");
            int n = JOptionPane.showConfirmDialog(SavannahPanel.this,
                  "Exit application?", "Quiting...", JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE);

            if (n == JOptionPane.YES_OPTION) {
               _model.setStatusMessage("Exiting...");
               _model.destroy();
               System.exit(0);
            } else {
               _model.setStatusMessage("Cancelled application exit.");
            }
         }
      };
      return listener;
   }

   //---------------------------------------------------------------------

   /**
    * Removes menu from this panel and returns reference to caller. This method
    * is used if panel is to be placed in a window that can display a menu bar.
    * By default, the menu is placed within this panel and only removed when
    * this method is called.
    * 
    * @return Application menu bar.
    */

   public JMenuBar getMenuBar() {
      this._menuPanel.removeAll();
      this._menuPanel.invalidate();
      return this._menuBar;
   }

   //---------------------------------------------------------------------

}