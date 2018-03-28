package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.logging.SavannahLogPanel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.DefaultMetaSubscriptionManager;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.ManagerControlPanel;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription.util.PersistedSessionsChecker;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.ChangePasswordAction;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.LockFileTypeAction;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.MakeDomainFileAction;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.SetDateTimeFilterAction;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.tools.SetDateTimeFormatAction;
import jpl.mipl.mdms.FileService.komodo.util.ConfigFileURLResolver;



/**
 * <b>Purpose:</b>
 *  Menu object to be used with MDMS FEI5 Savannah application.
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
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 06/02/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole	(Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahMenu.java,v 1.30 2013/04/04 20:48:50 ntt Exp $
 *
 */

public class SavannahMenu extends JMenuBar implements PropertyChangeListener
{
    private final String className_ = "SavannahMenu";

    /** Prepends component to the beginning of a menu */
    public static final int INSERT_AT_BEGINNING = 1;

    /** Appends component to the end of a menu */
    public static final int INSERT_AT_END = 2;

    /** Indicates action to performed on file menu */
    public static final int FILE_MENU = 0;

    /** Indicates action to performed on options menu */
    public static final int OPTIONS_MENU = 1;

    /** Indicates action to performed on window menu */
    public static final int TOOLS_MENU = 2;
    
    /** Indicates action to performed on help menu */
    public static final int HELP_MENU = 3;

    protected SavannahModel         _model;
    protected JPanel                _viewPanel;
    protected String                _aboutString;
    protected ImageIcon             _aboutIcon;
    protected SavannahTransferPanel _transferPanel;
    protected SavannahLogPanel      _logPanel;
    protected ManagerControlPanel   _subscriptionPanel;
    protected JDialog               _transferDialog;
    protected JDialog               _logDialog;
    protected JDialog               _msmDialog;
    
    //File
    protected JMenu     main_menu;
    protected JMenuItem main_exit_item;

    //Options
    protected JMenu     options_menu;
    protected JMenuItem set_date_time_format_item;
    protected JMenu     domain_file_options;
    protected JMenuItem new_domain_file_item;
    protected JMenuItem refresh_fei_group_item;
    protected JMenuItem make_domain_file_item;
    protected JMenu     session_options_item;
    protected JMenu     date_time_options_menu;
    protected JMenuItem set_date_filter_item;
    
    //Tools
    protected JMenu     tools_menu;
    protected JMenuItem tools_view_transfer_item;
    protected JMenuItem tools_view_log_item;
    protected JMenuItem tools_view_msm_item;
    protected JMenuItem tools_change_password;
    protected JMenuItem tools_lock_unlock_filetype_item;
    
    //Help
    protected JMenu     help_menu;
    protected JMenuItem help_help_item;
    protected JMenuItem help_about_item;

    //---------------------------------------------------------------------

    /**
     *  Constructs an instance of SavannahMenu for use with
     *  Savannah main panel components.
     *  @param model Instance of application model
     *  @param view_panel Reference to the main view panel to
     *                    which menu bar will be added
     *  @param about_string Text which will appear in the About 
     *                      box for application
     */
    
    public SavannahMenu(SavannahModel model,
                        JPanel view_panel,
                        String about_string)
    {
        this._model = model;
        this._viewPanel = view_panel;       
        this._aboutString = about_string;

        this._model.addPropertyChangeListener(this);

        buildMenu();
    }
                                                               
    //---------------------------------------------------------------------

    /**
     *  Builds menu GUI and sets up action listeners.
     */
    protected void buildMenu()
    {     
        //-----------------------------------------

        main_menu = new JMenu("File");        
        this.add(main_menu);
        main_menu.setMnemonic(KeyEvent.VK_F);

        //-------------------------
        //-------------------------
        //Exit
        
        main_exit_item = new JMenuItem("Exit");
        main_exit_item.setToolTipText("Quits application.");
        main_exit_item.setMnemonic(KeyEvent.VK_X);        
        main_exit_item.setAccelerator(KeyStroke.getKeyStroke(
                                  KeyEvent.VK_X, ActionEvent.ALT_MASK));
        main_exit_item.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               _model.setStatusMessage("Confirming application exit...");
               int n = JOptionPane.showConfirmDialog(_viewPanel,
                                    "Exit application?",
                                    "Quiting...",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
               
               if (n == JOptionPane.YES_OPTION)
               {
                   _model.setStatusMessage("Exiting...");
                   _model.destroy();
                   System.exit(0);
               }
               else
               {
                   _model.setStatusMessage("Cancelled application exit.");
               }
           }
        });
        main_menu.add(main_exit_item);

        //-------------------------------------
        //-------------------------------------
        //OPTIONS MENU

        options_menu = new JMenu("Options");
        options_menu.setMnemonic(KeyEvent.VK_O);
        this.add(options_menu);
               
        //-------------------------
        
        //Domain File Options
        domain_file_options = new JMenu("Domain File Options");
        domain_file_options.setMnemonic(KeyEvent.VK_D);
        options_menu.add(domain_file_options);
        
        
        //-------------------------
        //New Domain File
        new_domain_file_item = new JMenuItem("Select New Domain File");
        new_domain_file_item.setToolTipText(
                                   "Specify different FEI domain file");
        new_domain_file_item.setMnemonic(KeyEvent.VK_N);
        new_domain_file_item.addActionListener(new LoadNewDomainFileActListener());
        domain_file_options.add(new_domain_file_item);
        
        //-------------------------
        //Refresh Server List
        refresh_fei_group_item = new JMenuItem("Refresh Domain File");
        refresh_fei_group_item.setToolTipText(
                                "Refreshes list of FEI server groups "
                                + "from current domain file");
        refresh_fei_group_item.setMnemonic(KeyEvent.VK_H);

        refresh_fei_group_item.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               _model.setStatusMessage("Confirming domain file refresh...");
               int answer = JOptionPane.showConfirmDialog(
                                _viewPanel,
                                "Refresh from domain file?\n\nNOTE: This will "+
                                "disconnect you from current \nserver group.",
                                "Refresh Confirm", 
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
            
               if (answer == JOptionPane.YES_OPTION)
               {
                   _model.setStatusMessage("Refreshing from domain file...");
                   _model.refreshFeiServerGroups();
                   _model.setStatusMessage("Refresh complete.");
               }
               else
               {
                   _model.setStatusMessage("Domain file refresh cancelled.");
               }
           }
        });
        domain_file_options.add(refresh_fei_group_item);
        
        //-------------------------
        //Refresh Server List
        make_domain_file_item = new JMenuItem("Make Domain File");
        make_domain_file_item.setToolTipText(
                                "Create a new domain file based on current server group "
                                + "information");
        make_domain_file_item.setMnemonic(KeyEvent.VK_M);

        make_domain_file_item.addActionListener(new MakeDomainFileAction(
                                                this._viewPanel, this._model));       
        domain_file_options.add(make_domain_file_item);
        
        //-------------------------
        //Session Options List        
        
        options_menu.add(new SessionOptionsPanel(
                			_model.getSessionModel()));
        
        //-------------------------
        //Date time options
        date_time_options_menu = new JMenu("Date/Time Options");
        
 
        set_date_filter_item = new JMenuItem("Edit Date Filter(s)");
        set_date_filter_item.addActionListener(
                                    new SetDateTimeFilterAction(this._viewPanel,
                                                                this._model));
        
        set_date_time_format_item = new JMenuItem("Set Date/Time Format");
        set_date_time_format_item.addActionListener(
                                    new SetDateTimeFormatAction(this._viewPanel,
                                                                this._model));
        
        date_time_options_menu.add(set_date_filter_item);
        date_time_options_menu.add(set_date_time_format_item);
        
        options_menu.add(date_time_options_menu);
        
        
        //-------------------------------------
        //-------------------------------------
        //TOOLS MENU

        tools_menu = new JMenu("Tools");        
        tools_menu.setMnemonic(KeyEvent.VK_T);
        this.add(tools_menu);

        //-------------------------
        //Transfer 
        tools_view_transfer_item = new JMenuItem("Transfer History");
        tools_view_transfer_item.setToolTipText("Open transfer history " +
                                                 "window");
        tools_view_transfer_item.setMnemonic(KeyEvent.VK_T);
        tools_view_transfer_item.setAccelerator(KeyStroke.getKeyStroke(
                                    KeyEvent.VK_T, ActionEvent.ALT_MASK));
        tools_view_transfer_item.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               if (_transferPanel == null)
               {
                   _transferPanel = new SavannahTransferPanel(
                                       _model.getTransferModel(),
                                       "Transfer Log");                   
               }
               if (_transferDialog == null)
               {
                   _transferDialog = new JDialog();
                   _transferDialog.getContentPane().add(_transferPanel);
                   _transferDialog.setModal(false);
                   _transferDialog.pack();
                   _transferDialog.setTitle(SavannahModel.APPLICATION_TITLE);
                   _transferDialog.setLocationRelativeTo(_viewPanel);
                   _transferDialog.setDefaultCloseOperation(WindowConstants.
                                                            HIDE_ON_CLOSE);
                   _transferDialog.addComponentListener(new ComponentAdapter() {
                     public void componentHidden(ComponentEvent ce)
                     {
                       _model.setStatusMessage("Transfer Log window closed.");
                     }
                    });
               }
               
               if (!_transferDialog.isVisible())
               {
                   _transferDialog.setVisible(true);
                   _model.setStatusMessage("Transfer Log window opened.");
               }
               else
               {
                   _transferDialog.toFront();
               }
           }
        });
        tools_menu.add(tools_view_transfer_item);
        
        //-------------------------
        //Log Panel
        
        tools_view_log_item = new JMenuItem("Log Window");
        tools_view_log_item.setToolTipText("Open log window");
        tools_view_log_item.setMnemonic(KeyEvent.VK_L);
        tools_view_log_item.setAccelerator(KeyStroke.getKeyStroke(
                                    KeyEvent.VK_L, ActionEvent.ALT_MASK));
        tools_view_log_item.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               
               if (_logPanel == null)
               {
                   _logPanel = new SavannahLogPanel(
                                       _model.getLoggingModel(),
                                       "Log Window");
                   
               }
               
               if (_logDialog == null)
               {
                   _logDialog = new JDialog();
                   _logDialog.getContentPane().add(_logPanel);
                   _logDialog.addComponentListener(_logPanel);
                   _logDialog.setModal(false);
                   _logDialog.pack();
                   _logDialog.setTitle(SavannahModel.APPLICATION_TITLE);
                   _logDialog.setLocationRelativeTo(_viewPanel);
                   _logDialog.setDefaultCloseOperation(WindowConstants.
                                                            HIDE_ON_CLOSE);
                   _logDialog.addComponentListener(new ComponentAdapter() {
                     public void componentHidden(ComponentEvent ce)
                     {
                       _model.setStatusMessage("Log history window closed.");
                     }
                    });
               }
               
               if (!_logDialog.isVisible())
               {
                   _logDialog.setVisible(true);
                   _model.setStatusMessage("Log history window opened.");
               }
               else
               {
                   _logDialog.toFront();
               }               
           }
        });
        tools_menu.add(tools_view_log_item);
 
        //-------------------------
        //Change password
        
        Action changePasswordAction = new ChangePasswordAction(this._viewPanel,
                                                               this._model);
        tools_change_password = new JMenuItem(changePasswordAction);
        tools_change_password.setToolTipText("Change server group password");
        tools_change_password.setMnemonic(KeyEvent.VK_P);
        tools_change_password.setAccelerator(KeyStroke.getKeyStroke(
                                    KeyEvent.VK_P, ActionEvent.ALT_MASK));  
        tools_menu.add(new JSeparator());
        tools_menu.add(tools_change_password);
        
        //-------------------------
        //Lock/Unlock Filetype
        
        Action lockFiletypeAction = new LockFileTypeAction(this._viewPanel,
                                                           this._model);
        tools_lock_unlock_filetype_item = new JMenuItem(lockFiletypeAction);
        tools_lock_unlock_filetype_item.setToolTipText("Lock/Unlock Filetype");
        tools_lock_unlock_filetype_item.setMnemonic(KeyEvent.VK_F);
        tools_lock_unlock_filetype_item.setAccelerator(KeyStroke.getKeyStroke(
                                    KeyEvent.VK_F, ActionEvent.ALT_MASK));  
        tools_menu.add(new JSeparator());
        tools_menu.add(tools_lock_unlock_filetype_item);
        
        
        //-------------------------
        //Metasubscription Manager Panel
        
        tools_view_msm_item = new JMenuItem("Subscription Manager");
        tools_view_msm_item.setToolTipText("View subscription manager");
        tools_view_msm_item.setMnemonic(KeyEvent.VK_S);
        tools_view_msm_item.setAccelerator(KeyStroke.getKeyStroke(
                                    KeyEvent.VK_S, ActionEvent.ALT_MASK));
        tools_view_msm_item.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               boolean firstCall = false;
               
               if (_subscriptionPanel == null)
               {
                   _subscriptionPanel = new ManagerControlPanel(
                                       (DefaultMetaSubscriptionManager)
                                       _model.getSubscriptionManager());
                   firstCall = true;
                   
               }
               if (_msmDialog == null)
               {
                   _msmDialog = new JDialog();
                   _msmDialog.getContentPane().add(_subscriptionPanel);
                   _msmDialog.setModal(false);
                   _msmDialog.pack();
                   _msmDialog.setTitle(SavannahModel.APPLICATION_TITLE);
                   _msmDialog.setLocationRelativeTo(_viewPanel);
                   _msmDialog.setDefaultCloseOperation(WindowConstants.
                                                            HIDE_ON_CLOSE);
                   _msmDialog.addComponentListener(new ComponentAdapter() {
                     public void componentHidden(ComponentEvent ce)
                     {
                       _model.setStatusMessage("Subscription manager "
                                               + "window closed.");
                     }
                    });
               }
               
               if (!_msmDialog.isVisible())
               {
                   _msmDialog.setVisible(true);
                   _model.setStatusMessage("Subscription manager "
                                           + "window opened.");
               }
               else
               {
                   _msmDialog.toFront();
               }
               
               if (firstCall)
               {
                   PersistedSessionsChecker checker;
                   checker = new PersistedSessionsChecker(
                                       _model, 
                                       _msmDialog);
                   checker.run();
               }
           }
        });
        tools_menu.add(new JSeparator());
        tools_menu.add(tools_view_msm_item);
        

        
        
        //-------------------------------------
        //-------------------------------------
        //HELP MENU

        help_menu = new JMenu("Help");        
        help_menu.setMnemonic(KeyEvent.VK_H);
        this.add(Box.createHorizontalGlue());
        this.add(help_menu);
        this.add(Box.createHorizontalStrut(20));

        //-------------------------
        //About 
        help_about_item = new JMenuItem("About");
        help_about_item.setToolTipText("Viewer Info");
        help_about_item.setMnemonic(KeyEvent.VK_A);
        help_about_item.setAccelerator(KeyStroke.getKeyStroke(
                               KeyEvent.VK_A, ActionEvent.ALT_MASK));
        java.net.URL iconURL = SavannahMenu.class.getResource(
                                          SavannahModel.ABOUT_ICON_PATH);
                                          //SavannahModel.ICON_PATH);
        if (iconURL != null) 
        {
            _aboutIcon = new ImageIcon(iconURL);     
        }
        else
        {
            _aboutIcon = null;            
        }
        
        help_about_item.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               JOptionPane.showMessageDialog(_viewPanel, 
                          _aboutString,
                          "About...",
                          JOptionPane.INFORMATION_MESSAGE,
                          _aboutIcon);               
           }
        });
        help_menu.add(help_about_item);
    }

    //---------------------------------------------------------------------

    /**
     *  Inserts a custom menu component to a menu.
     *  @param menuId Menu to which component will be added, one of
     *                FILE_MENU, OPTIONS_MENU, HELP_MENU.
     *  @param newMenuItem The component to be added
     *  @param separate Flag indicating whether to add separators
     *  @param placement Where in menu to place component, one of
     *                   INSERT_AT_BEGINNING, INSERT_AT_END.
     */

    public void insertMenuItem(int menuId, Component newMenuItem, 
                               boolean separate, int placement)
    {       
        JMenu menu = null;
        int index = 0;
        boolean topSeparator = false;
        boolean botSeparator = false;
        
        switch (menuId)
        {
            case FILE_MENU: 
                menu = main_menu;
                break;
            case OPTIONS_MENU: 
                menu = options_menu;
                break;
            case TOOLS_MENU: 
                menu = tools_menu;
                break;
            case HELP_MENU: 
                menu = help_menu;
                break;
            default:
                throw new IllegalArgumentException(className_+
                        "::insertMenuItem(): Unrecognized menu id: "+menuId);
        }

        switch (placement)
        {
            case INSERT_AT_BEGINNING: 
                index = 0;
                botSeparator = separate;
                break;
            case INSERT_AT_END: 
                index = menu.getItemCount();
                topSeparator = separate;
                break;
            default:
                throw new IllegalArgumentException(className_+
                        "::insertMenuItem(): Unrecognized placement: "+
                        placement); 
        }

        if (topSeparator)
        {
            menu.insertSeparator(index);
            index++;
        }

        menu.add(newMenuItem, index);

        if (botSeparator)
        {
            index++;
            menu.insertSeparator(index);
        }        
    }
  
    //---------------------------------------------------------------------
    
    /**
     *  Implementation of the PropertyChangeListener interface.  For
     *  interaction with the model.  Method is called whenever
     *  a change is made to a model property.
     *  @param evt A PropertyChangeEvent object describing the event 
     *               source and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        //--------------------------------
        
        /*
        if (propName.equalsIgnoreCase(""))
        {
            
        } 
        */

        //--------------------------------
    }

    public static JPanel createMessagePanel(String message)
    {
        JPanel panel = new JPanel();
        JLabel label = new JLabel(message);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.setLayout(new BorderLayout());
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Implementation of ActionListener for the new domain file menu item.
     * 
     */
    
    class LoadNewDomainFileActListener implements ActionListener
    {   
        public void actionPerformed(ActionEvent ae)
        {
            _model.setStatusMessage("Selecting new domain file...");
            URL curDomain = _model.getDomainFile();
            File parent = null, child = null;
            File newFile;
            
                        
            try {
                URI uri = curDomain.toURI();
                child = new File(uri);
            } catch (Exception ex) {
                child = null;
            }
            
            
            //if child exists, then opem chooser in the same dir,
            //otherwise open up in working dir
            if (child != null && child.canRead())
                parent = child.getParentFile();
            else
                parent = new File(System.getProperty("user.dir"));
            
            JFileChooser fc = new JFileChooser(parent);
            if (child != null)
            {
                fc.setSelectedFile(child);
                fc.ensureFileIsVisible(child);
            }
            
            int rVal = fc.showOpenDialog(_viewPanel);
            if (rVal == JFileChooser.APPROVE_OPTION)
            {
                newFile = fc.getSelectedFile();
                
                ConfigFileURLResolver resolver = new ConfigFileURLResolver();
                URL newURL = null;
                try {
                    newURL = resolver.getFileURL(newFile.getAbsolutePath());
                } catch (SessionException sesEx) {
                    newURL = null;
                }
                
                if (newURL != null)
                    _model.setDomainFile(newURL, false);
            }
        }
    }
    
    
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

} //end_of_class

