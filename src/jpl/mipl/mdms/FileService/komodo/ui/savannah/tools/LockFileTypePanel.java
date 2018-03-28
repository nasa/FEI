package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;

public class LockFileTypePanel extends JPanel
{

    //---------------------------------------------------------------------
    
    public static final String MODE_GROUP = "group";
    public static final String MODE_OWNER = "owner";
    
    private javax.swing.JComboBox filetypeCombo;
    private javax.swing.JLabel actionLabel;
    private javax.swing.JLabel filetypeLabel;
    private javax.swing.JLabel lockModeLabel;
    private javax.swing.JRadioButton lockTypeButton;
    private javax.swing.JRadioButton unlockTypeButton;
    private javax.swing.JRadioButton noneModeButton;
    private javax.swing.JRadioButton ownerModeButton;
    private javax.swing.JRadioButton groupModeButton;
    
    
    protected List<String> filetypes;
    protected String       operationMode;
    protected String       filetype;
    protected String       operation;
    protected String initialGroup;
    protected String initialType;

    //---------------------------------------------------------------------
    
    public LockFileTypePanel(String[] inFiletypes)
    {
        this(inFiletypes, null, null, Constants.LOCKFILETYPE);
    }

    //---------------------------------------------------------------------
    
    public LockFileTypePanel(String[] inFiletypes, String initOp)
    {
        this(inFiletypes, null, null, initOp);
    }
    
    //---------------------------------------------------------------------
    
    public LockFileTypePanel(String[] inFiletypes, String initGroup,
                             String initType, String initOp)
    {
        super();

        //-------------------------
        //filetypes
        
        this.filetypes = new Vector<String>();
        for (String ft : inFiletypes)
            this.filetypes.add(ft);
                
        //-------------------------
        //operation
        
        if (initOp.equals(Constants.LOCKFILETYPE) ||
            initOp.equals(Constants.UNLOCKFILETYPE))
        {
            this.operation = initOp;
        }
        else
        {
            this.operation = Constants.LOCKFILETYPE;
        }
        
        
        //-------------------------
        //operation modifier
        
        this.operationMode = null;
               
        //-------------------------
        //initial state info

        this.initialGroup = initGroup;
        this.initialType = initType;
        
        //-------------------------
        
        buildUI();
    }
    
    //---------------------------------------------------------------------
    
    protected void buildUI()
    {
        initComponents();
        
        initValues();
        
        initListeners();
    }
    
    //---------------------------------------------------------------------
    
    
    protected void initComponents() 
    {
        java.awt.GridBagConstraints gridBagConstraints;

        filetypeCombo    = new javax.swing.JComboBox();
        lockTypeButton   = new javax.swing.JRadioButton();
        unlockTypeButton = new javax.swing.JRadioButton();
        filetypeLabel    = new javax.swing.JLabel();
        lockModeLabel    = new javax.swing.JLabel();
        noneModeButton   = new javax.swing.JRadioButton();
        ownerModeButton  = new javax.swing.JRadioButton();
        groupModeButton  = new javax.swing.JRadioButton();
        actionLabel      = new javax.swing.JLabel();

        setName("Form"); // NOI18N
        setLayout(new java.awt.GridBagLayout());

        
        filetypeCombo.setName("filetypeComboBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 4, 5);
        add(filetypeCombo, gridBagConstraints);
       
        lockTypeButton.setText("Lock Type");
        lockTypeButton.setName("lockTypeButton"); // NOI18N
        lockTypeButton.setToolTipText("Performs lock operation on filetype");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(lockTypeButton, gridBagConstraints);

        unlockTypeButton.setText("Unlock Type");
        unlockTypeButton.setName("unlockTypeButton"); // NOI18N
        unlockTypeButton.setToolTipText("Performs unlock operation on filetype");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(unlockTypeButton, gridBagConstraints);

        filetypeLabel.setText("Filetype: ");
        filetypeLabel.setName("filetypeLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 4, 20);
        add(filetypeLabel, gridBagConstraints);

        lockModeLabel.setText("Lock Mode: ");
        lockModeLabel.setName("lockModeLabel"); // NOI18N
        lockModeLabel.setToolTipText("Lock operation modifier");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 20);
        add(lockModeLabel, gridBagConstraints);

        noneModeButton.setText("None");
        noneModeButton.setName("noneModeButton"); // NOI18N
        noneModeButton.setToolTipText("Default write access removal.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(noneModeButton, gridBagConstraints);

        ownerModeButton.setText("Owner");
        ownerModeButton.setName("ownerModeButton"); // NOI18N
        ownerModeButton.setToolTipText("Removes write access from all.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(ownerModeButton, gridBagConstraints);

        groupModeButton.setText("Group");
        groupModeButton.setName("groupModeButton"); // NOI18N
        groupModeButton.setToolTipText("Removes write access from all but owner.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        add(groupModeButton, gridBagConstraints);

        actionLabel.setText("Operation");
        actionLabel.setName("actionLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 20);
        add(actionLabel, gridBagConstraints);
    }

    //---------------------------------------------------------------------
    
    protected void initListeners()
    {
        
        this.filetypeCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ie)
            {
                filetype = (String) filetypeCombo.getSelectedItem();
            }
        });
        
        //-------------------------
        
        ButtonGroup opButtonGroup = new ButtonGroup();
        opButtonGroup.add(lockTypeButton);
        opButtonGroup.add(unlockTypeButton);
        
        ActionListener operationListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) 
            {
                Object source = ae.getSource();
                String oldOp = operation;
                
                if (source == lockTypeButton && lockTypeButton.isSelected())
                {
                    operation = Constants.LOCKFILETYPE;
                }
                else if (source == unlockTypeButton && unlockTypeButton.isSelected())
                {
                    operation = Constants.UNLOCKFILETYPE;                    
                } 
                
                if (!oldOp.equals(operation))
                    enableBasedOnOperation();
            }
        };
        this.lockTypeButton.addActionListener(operationListener);
        this.unlockTypeButton.addActionListener(operationListener);
        
        //-------------------------
        
        ButtonGroup modeButtonGroup = new ButtonGroup();
        modeButtonGroup.add(noneModeButton);
        modeButtonGroup.add(groupModeButton);
        modeButtonGroup.add(ownerModeButton);
        
        ActionListener modeListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) 
            {
                Object source = ae.getSource();
                
                if (!(source instanceof JRadioButton))
                    return;
                
               JRadioButton button = (JRadioButton) source;
               
               //we are only interested in selection action events
               if (!button.isSelected())
                   return;
                
                if (button == noneModeButton)
                {
                    operationMode = null;
                }
                else if (button == groupModeButton)
                {
                    operationMode = MODE_GROUP;                    
                } 
                else if (button == ownerModeButton)
                {
                    operationMode = MODE_OWNER;
                }
                
            }
        };
        this.noneModeButton.addActionListener(modeListener);
        this.groupModeButton.addActionListener(modeListener);
        this.ownerModeButton.addActionListener(modeListener);
        
        //-------------------------
        
    }
    
    //---------------------------------------------------------------------
    
    protected void initValues()
    {
        
        //-------------------------
        
        //init the combo entries and initial selection
        
        for (String item : this.filetypes)
            this.filetypeCombo.addItem(item);
        
        if (this.initialGroup != null && this.initialType != null)
        {
            String initFullFt = FileType.toFullFiletype(initialGroup, initialType);
            this.filetypeCombo.setSelectedItem(initFullFt);            
        }
        else if (this.initialGroup != null)
        {
            int index = -1;
            int size = filetypes.size();
            for (int i = 0; i < size && index == -1; ++i)
            {
                String entry =filetypes.get(i);
                String entryGroup = FileType.extractServerGroup(entry);
                if (entryGroup != null && entryGroup.equalsIgnoreCase(initialGroup))
                    index = i;                    
            }            
            if (index != -1)
                this.filetypeCombo.setSelectedIndex(index);
        }
        else 
        {
            this.filetypeCombo.setSelectedIndex(0);
        }
        
        this.filetype = (String) this.filetypeCombo.getSelectedItem();
        
        
        //-------------------------
        
        //init operation buttons initial state
        
        if (this.operation.equals(Constants.LOCKFILETYPE))
        {
            this.lockTypeButton.setSelected(true);
            this.unlockTypeButton.setSelected(false);
        }
        else
        {
            this.lockTypeButton.setSelected(false);
            this.unlockTypeButton.setSelected(true);
        }
        
        //-------------------------
        
        //init lock mode including whether components are enabled
        
        if (this.operationMode == null)
        {
            this.noneModeButton.setSelected(true);
        }
        else if (this.operationMode.equalsIgnoreCase(MODE_GROUP))
        {
            this.groupModeButton.setSelected(true);
        }
        else if (this.operationMode.equalsIgnoreCase(MODE_OWNER))
        {
            this.ownerModeButton.setSelected(true);
        }
        
        enableBasedOnOperation();

        //-------------------------

    }
    
    //---------------------------------------------------------------------
        
    protected void enableBasedOnOperation()
    {
//        boolean enabled = (this.operation == Constants.LOCKFILETYPE);
//        
//        this.lockModeLabel.setEnabled(enabled);
//        this.noneModeButton.setEnabled(enabled);
//        this.groupModeButton.setEnabled(enabled);
//        this.ownerModeButton.setEnabled(enabled);        
    }
    
    //---------------------------------------------------------------------
    
    public String getOperation()
    {
        return this.operation;
    }
    
    //---------------------------------------------------------------------

    public String getOperationMode()
    {
        return this.operationMode;
    }
    
    //---------------------------------------------------------------------
    
    public String getFileType()
    {
        return this.filetype;
    }
    
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        String[] groups = { "dev:my group", "ops:group grope", "ops:music group"};
        
        
        
        LockFileTypePanel panel = new LockFileTypePanel(groups);
        
        JFrame frame = new JFrame("Test");
        frame.getContentPane().add(panel);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    //---------------------------------------------------------------------
}
