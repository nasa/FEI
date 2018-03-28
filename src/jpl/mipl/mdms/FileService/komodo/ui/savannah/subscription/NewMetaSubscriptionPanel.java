package jpl.mipl.mdms.FileService.komodo.ui.savannah.subscription;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * <b>Purpose:</b>
 * JPanel for entering the meta-subscription type, filetype, and 
 * output directory.  Subscription types and filetype options
 * are passed as a string array, while the initial output dir
 * is passed as a file.  Once the panel is dismissed, query
 * the methods get*() to retrieve selected parameters.
 *
 *   <PRE>
 *   Copyright 2005, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2005.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 03/17/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: NewMetaSubscriptionPanel.java,v 1.1 2005/03/23 03:45:10 ntt Exp $
 *
 */

public class NewMetaSubscriptionPanel extends JPanel {
    
    protected File _outputDir;
    protected String _filetype;
    protected String _subscriptionType;
    
    protected String[] _metaSubTypes;
    protected String[] _filetypes;    
    protected int _exitStatus;
    

    private JPanel _dirPanel;
    private JLabel _filetypeLabel;
    private JPanel _midPanel;
    private JLabel _outputLabel;
    private JPanel _topPanel;
    private JLabel _typeLabel;
    private JButton _dirButton;
    private JComboBox _typeComboBox;
    private JComboBox _filetypeComboBox;
    private JLabel _purposeLabel;
    private JPanel _mainPanel;
    private JSeparator _separator;
    private JTextField _dirField;
    
    //---------------------------------------------------------------------
    
    /** 
     * Creates new form NewMetaSubscriptionPanel 
     * @param types Array of metasubscription types
     * @param filetypes Array of filetypes
     */
    
    public NewMetaSubscriptionPanel(String[] types, String[] filetypes) 
    {
        this._metaSubTypes = types;
        this._filetypes    = filetypes;
        this._outputDir    = new File(System.getProperty("user.home"));
        
        this._subscriptionType = this._metaSubTypes[0];
        this._filetype         = this._filetypes[0]; 
        
        initComponents();
        init();
    }
    
    //---------------------------------------------------------------------
    
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        _mainPanel = new JPanel();
        _topPanel = new JPanel();
        _purposeLabel = new JLabel();
        _separator = new JSeparator();
        _midPanel = new JPanel();
        _typeLabel = new JLabel();
        _filetypeLabel = new JLabel();
        _outputLabel = new JLabel();
        _typeComboBox = new JComboBox();
        _filetypeComboBox = new JComboBox();
        _dirPanel = new JPanel();
        
        //anon subclass that sets text as tooltiptext as well
        _dirField = new JTextField() {
            public void setText(String text) {
                super.setText(text);
                final String fText = text; 
                SwingUtilities.invokeLater(new Runnable() {
                   public void run() {
                       _dirField.setToolTipText(fText);
                   }
                });
            }
        };
        _dirButton = new JButton();

        setLayout(new BorderLayout());

        setMinimumSize(new Dimension(480, 130));
        setPreferredSize(new Dimension(600, 150));
        _mainPanel.setLayout(new GridBagLayout());

        _topPanel.setLayout(new GridBagLayout());

        _purposeLabel.setText("Specify the subscription type, filetype, "
                              + "and output directory.");
                              //Select 'Next' to continue.");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(13, 18, 0, 0);
        _topPanel.add(_purposeLabel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(10, 9, 0, 9);
        _topPanel.add(_separator, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        _mainPanel.add(_topPanel, gridBagConstraints);

        _midPanel.setLayout(new GridBagLayout());

        _midPanel.setPreferredSize(new Dimension(100, 53));
        _typeLabel.setText("Type:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(5, 15, 3, 0);
        _midPanel.add(_typeLabel, gridBagConstraints);

        _filetypeLabel.setText("Filetype:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(0, 15, 2, 0);
        _midPanel.add(_filetypeLabel, gridBagConstraints);

        _outputLabel.setText("Output directory:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(0, 15, 0, 0);
        _midPanel.add(_outputLabel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new Insets(5, 2, 3, 15);
        _midPanel.add(_typeComboBox, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new Insets(0, 2, 2, 15);
        _midPanel.add(_filetypeComboBox, gridBagConstraints);

        _dirPanel.setLayout(new GridBagLayout());
        _dirField.setText("Output directory");
        _dirField.setEditable(true);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = .99;
        _dirPanel.add(_dirField, gridBagConstraints);
        
        _dirButton.setText("...");
        _dirButton.setPreferredSize(new Dimension(25, 25));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        _dirPanel.add(_dirButton, gridBagConstraints);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 0, 15);
        _midPanel.add(_dirPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new Insets(0,0,5,0);
        _mainPanel.add(_midPanel, gridBagConstraints);

        add(_mainPanel, BorderLayout.CENTER);

    }//GEN-END:initComponents
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        //-------------------------
        
        //add initial data to fields 
        for (int i = 0; i < this._metaSubTypes.length; ++i)
        {
            this._typeComboBox.addItem(this._metaSubTypes[i]);
        }
        for (int i = 0; i < this._filetypes.length; ++i)
        {
            this._filetypeComboBox.addItem(this._filetypes[i]);
        }
        this._dirField.setText(this._outputDir.getAbsolutePath());
        
        //-------------------------
        
        // add action listeners to components
        this._typeComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie)
            {
                _subscriptionType = (String) _typeComboBox.getSelectedItem();
            }
        });
        this._filetypeComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie)
            {
                _filetype = (String) _filetypeComboBox.getSelectedItem();
            }
        });
        
        //hack to convert action events into focus events for the field
        this._dirField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                _dirField.transferFocus();
            }
        });
        this._dirField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent fe)
            {
                String txt = _dirField.getText();
                File tmp = new File(txt);
                
                if (tmp.equals(_outputDir))
                    return;
                
                if (!tmp.exists() || !tmp.isDirectory())
                {
                    JOptionPane.showMessageDialog(NewMetaSubscriptionPanel.this, 
                            "'"+txt+"' is not a directory.\n"+
                            "Click 'OK' to revert value.", "Dir Warning", 
                            JOptionPane.WARNING_MESSAGE);
                    _dirField.setText(_outputDir.getAbsolutePath());
                    return;
                }
                _outputDir = tmp;
            }
        });
        
        //-------------------------
        
        this._dirButton.addActionListener(new ActionListener(){ 
            public void actionPerformed(ActionEvent ae)
            {
                JFileChooser fc = new JFileChooser(_outputDir);
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setDialogTitle("Select output dir...");
                
                int val = fc.showOpenDialog(NewMetaSubscriptionPanel.this);
                if (val == JFileChooser.APPROVE_OPTION)
                {
                    _outputDir = fc.getSelectedFile();
                    _dirField.setText(_outputDir.getAbsolutePath());
                }
            }
        });

        //-------------------------
    }
 
    //---------------------------------------------------------------------
    
    public void setSubscriptionType(String type)
    {
        this._typeComboBox.setSelectedItem(type);
    }
    
    //---------------------------------------------------------------------
    
    public void setFiletype(String filetype)
    {
        this._filetypeComboBox.setSelectedItem(filetype);
    }
    
    //---------------------------------------------------------------------
    
    public String getSubscriptionType()
    {
        return this._subscriptionType;
    }
    
    //---------------------------------------------------------------------
    
    public String getFiletype()
    {
        return this._filetype;
    }
    
    //---------------------------------------------------------------------
    
    public void setOutputDir(File output)
    {
        if (output == null || !output.isDirectory() ||
            output.equals(this._outputDir))
            return;
        
        this._outputDir = output;
        this._dirField.setText(this._outputDir.getAbsolutePath());
    }
    
    //---------------------------------------------------------------------
    
    public File getOutputDir()
    {
        return this._outputDir;
    }
  
    //---------------------------------------------------------------------
    
    public int getExitStatus()
    {
        return _exitStatus;
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        String[] types = new String[] {"Subscription", "Notification"};
        String[] fts = new String[] {"group:type1", "group:type2",
                                    "group:type3","group:type4"};
        String type = "Subscription"; 
        String ft = "group:type2";
        File file = new File(System.getProperty("user.home"));
        file = new File(file, "Development");
        JFrame frame = new JFrame("Test");
        NewMetaSubscriptionPanel panel = new NewMetaSubscriptionPanel(types, fts);
        frame.getContentPane().add(panel);
        panel.setFiletype(ft);
        panel.setSubscriptionType(type);
        panel.setOutputDir(file);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        
    }
    
    //---------------------------------------------------------------------
}
