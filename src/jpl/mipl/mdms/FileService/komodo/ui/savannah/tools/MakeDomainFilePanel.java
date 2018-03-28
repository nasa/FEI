package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MakeDomainFilePanel extends JPanel
{
    public static final String DEFAULT_FILENAME = "domain.fei";
    
    private javax.swing.JLabel serverGroupLabel;
    private javax.swing.JLabel outputFileLabel;

    
    JPanel domainfilePanel;
    
    
    private javax.swing.JComboBox sgComboBox;
    private javax.swing.JTextField domainfilePathField;
    private javax.swing.JButton domainfilePathButton;
    
    
    protected List<String> servergroups;
    protected File         directory;
    protected File         newDomainFile;
    protected String       servergroup;
    
    public MakeDomainFilePanel(String[] groups, String currentGroup,
                               String currentDir)
    {
        super();
        
        //-------------------------
        
        this.servergroups = new Vector<String>();
        for (String groupname : groups)
            this.servergroups.add(groupname);
        
        this.servergroup   = currentGroup;
        
        //-------------------------
        
        if (currentDir == null || !(new File(currentDir)).isDirectory())
            currentDir = System.getProperty("user.dir");
        
        this.directory = new File(currentDir);
        this.newDomainFile = new File(this.directory, DEFAULT_FILENAME);
        
        //-------------------------
        
        buildUI();
    }
    
    
    protected void buildUI()
    {
        initComponents();
        
        initValues();
        
        initListeners();
    }
    

    private void initComponents() 
    {
        java.awt.GridBagConstraints gridBagConstraints;

        serverGroupLabel = new javax.swing.JLabel();
        outputFileLabel = new javax.swing.JLabel();
       
        sgComboBox = new javax.swing.JComboBox();
        domainfilePathField = new javax.swing.JTextField();        

        setLayout(new java.awt.GridBagLayout());

        serverGroupLabel.setText("Server group:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(serverGroupLabel, gridBagConstraints);

        outputFileLabel.setText("File path:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(outputFileLabel, gridBagConstraints);

        
        domainfilePanel = new JPanel();
        domainfilePanel.setLayout(new GridBagLayout());

        domainfilePathField.setText(this.newDomainFile.getAbsolutePath());
        domainfilePathField.setToolTipText("Path of newly created domain file");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.95;
        domainfilePanel.add(domainfilePathField, gridBagConstraints);

        domainfilePathButton = new JButton("...");
        domainfilePathButton.setToolTipText("Browse using file dialog");
        domainfilePathButton.setPreferredSize(new Dimension(25, 23));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        domainfilePanel.add(domainfilePathButton, gridBagConstraints);
               
       

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(sgComboBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(domainfilePanel, gridBagConstraints);
       

    }
    
    protected void initListeners()
    {
        
        this.sgComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie)
            {
                servergroup = (String) sgComboBox.getSelectedItem();
            }
        });
        
        
        //hack to convert action events into focus events for the field
        this.domainfilePathField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                domainfilePathField.transferFocus();
            }
        });
        this.domainfilePathField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent fe)
            {
                String txt = domainfilePathField.getText();
                File f = new File(txt);
                if (f.isDirectory())
                {
                    MakeDomainFilePanel.this.directory = f;
                    MakeDomainFilePanel.this.newDomainFile = null;   
                }
                else if (f.getParentFile().isDirectory())
                {
                    MakeDomainFilePanel.this.directory = f.getParentFile();
                    MakeDomainFilePanel.this.newDomainFile = f;
                }             
            }            
        });
        
        this.domainfilePathButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                JFileChooser fc = new JFileChooser(directory);
                if (newDomainFile != null)
                {
                    fc.setSelectedFile(newDomainFile);
                    fc.ensureFileIsVisible(newDomainFile);
                }
                
                int rVal = fc.showSaveDialog(MakeDomainFilePanel.this);
                if (rVal == JFileChooser.APPROVE_OPTION)
                {
                    File newFile = fc.getSelectedFile();
                    domainfilePathField.setText(newFile.getAbsolutePath());
                    newDomainFile = newFile;
                    directory = newDomainFile.getParentFile();                    
                }                
            }
        });

    }
    
    protected void initValues()
    {
        for (String item : this.servergroups)
            this.sgComboBox.addItem(item);
        
        if (this.servergroup == null)
        {
            this.servergroup = (String) this.sgComboBox.getItemAt(0);
        }
        else
        {
            this.sgComboBox.setSelectedItem(this.servergroup);
        }
        
        if (this.newDomainFile != null)
        {
            this.domainfilePathField.setText(this.newDomainFile.getAbsolutePath());
        }         
    }
    
    
    
    public File getOutputFile()
    {
        return this.newDomainFile;
    }   
    
    public String getGroup()
    {
        return this.servergroup;
    }
    
    
    public static void main(String[] args)
    {
        String[] groups = { "my group", "group grope", "music group"};
        
        
        MakeDomainFilePanel panel = new MakeDomainFilePanel(groups, null, null);
        
        JFrame frame = new JFrame("Test");
        frame.getContentPane().add(panel);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
