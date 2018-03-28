package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ChangePasswordPanel extends JPanel
{
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPasswordField new2PassField;
    private javax.swing.JPasswordField newPassField;
    private javax.swing.JPasswordField oldPassField;
    private javax.swing.JComboBox sgComboBox;
    private javax.swing.JTextField usernameField;
    
    
    protected List<String> servergroups;
    protected String       username;
    protected String       oldPassword;
    protected String       newPassword;
    protected String       servergroup;
    
    public ChangePasswordPanel(String[] groups, String currentGroup, 
                               String username, String password)
    {
        super();
        
        this.servergroups = new Vector<String>();
        for (String groupname : groups)
            this.servergroups.add(groupname);
        
        this.servergroup = currentGroup;
        this.username    = username;
        this.oldPassword = password;
        
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

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        sgComboBox = new javax.swing.JComboBox();
        usernameField = new javax.swing.JTextField();
        oldPassField = new javax.swing.JPasswordField();
        newPassField = new javax.swing.JPasswordField();
        new2PassField = new javax.swing.JPasswordField();

        setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Server group:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(jLabel1, gridBagConstraints);

        jLabel2.setText("User name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(jLabel2, gridBagConstraints);

        jLabel3.setText("Old password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(jLabel3, gridBagConstraints);

        jLabel4.setText("New password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(jLabel4, gridBagConstraints);

        jLabel5.setText("Repeat password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(sgComboBox, gridBagConstraints);

        usernameField.setText("<username>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(usernameField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(oldPassField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 10);
        add(newPassField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 10);
        add(new2PassField, gridBagConstraints);

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
        this.usernameField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                usernameField.transferFocus();
            }
        });
        this.usernameField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent fe)
            {
                String txt = usernameField.getText();
                username = txt;
            }            
        });
        

        //hack to convert action events into focus events for the field
        this.oldPassField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                oldPassField.transferFocus();
            }
        });
        this.oldPassField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent fe)
            {
                char[] txt = oldPassField.getPassword();
                oldPassword = new String(txt);
            }            
        });


        //hack to convert action events into focus events for the field
        this.newPassField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                newPassField.transferFocus();
            }
        });
        this.newPassField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent fe)
            {
                char[] txt = newPassField.getPassword();
                newPassword = new String(txt);
            }            
        });
        
        
        //hack to convert action events into focus events for the field
        this.new2PassField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                new2PassField.transferFocus();
            }
        });
        this.new2PassField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent fe)
            {
                char[] txt = new2PassField.getPassword();
                String copy = new String(txt);
                if (copy != null && !copy.equals(newPassword))
                {
                    JOptionPane.showMessageDialog(ChangePasswordPanel.this, 
                            "New password fields do not match", 
                            "Password Error", JOptionPane.ERROR_MESSAGE);
                    newPassField.requestFocus();
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
        
        if (this.username == null)
            username = System.getProperty("user.name");
        if (this.username != null)
            this.usernameField.setText(this.username);
        
        if (this.oldPassword != null)
            this.oldPassField.setText(this.oldPassword);    
    }
    
    
    
    public String getUsername()
    {
        return this.username;
    }
    
    public String getOldPassword()
    {
        return this.oldPassword;
    }
    

    public String getNewPassword()
    {
        return this.newPassword;
    }
    
    public String getGroup()
    {
        return this.servergroup;
    }
    
    
    public static void main(String[] args)
    {
        String[] groups = { "my group", "group grope", "music group"};
        String username = "mr_peterman";
        
        
        ChangePasswordPanel panel = new ChangePasswordPanel(groups, null, username, null);
        
        JFrame frame = new JFrame("Test");
        frame.getContentPane().add(panel);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
