package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * <b>Purpose:</b>
 *  Generic login dialog with username and password fields.
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
 * 06/02/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole	(Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: LoginDialog.java,v 1.9 2013/03/30 00:06:21 ntt Exp $
 *
 */
 
public class LoginDialog
{
    protected String _username;
    protected String _password;
    protected String _title;
    protected String _message;
    protected JPanel _panel;
    protected JTextField _userField;
    protected JPasswordField _passField;
    protected JLabel _msgLabel;
    
    protected JLabel _passwordLabel;
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.
     * @param title Optional title to be displayed with dialog.
     */
    
    public LoginDialog(String title)
    {
        this(title, null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Constructs a login dialog with username and password
     *  fields.
     *  @param title Optional title to be displayed with dialog.
     *  @param username Initial username to enter in field
     */
    public LoginDialog(String title, String username)
    {            
        this(title, username, null);
    }
    
    //---------------------------------------------------------------------
    
    
    /**
     * Constructs a login dialog with username and password
     * fields.
     * @param title Optional title to be displayed with dialog.
     * @param username Initial username to enter in field
     * @param labelValue Optional message value for panel 
     */
    public LoginDialog(String title, String username, String message)
    {            
        _title = title;
        _message = message;
        
        _panel = new JPanel();
        _panel.setLayout(new BoxLayout(_panel, BoxLayout.Y_AXIS));
        _userField = new JTextField(username, 12);
        _passField = new JPasswordField(12);                          
             
        JPanel linePanel;
        JLabel label;
        
        if (this._message != null)
        {
            linePanel = new JPanel();
            this._msgLabel = new JLabel(this._message);
            this._msgLabel.setEnabled(false);
            linePanel.add(this._msgLabel);
            linePanel.setAlignmentX(0.0f);
            this._panel.add(linePanel);
        }
        
        linePanel = new JPanel();
        label = new JLabel("Username: ");
        linePanel.add(label);
        linePanel.add(_userField);
        linePanel.setAlignmentX(0.0f);
        _panel.add(linePanel);
        linePanel = new JPanel();
        label = new JLabel("Password: ");
        linePanel.add(label);
        _passwordLabel = label;
        linePanel.add(_passField);
        linePanel.setAlignmentX(0.0f);
        _panel.add(linePanel);   
        
        _userField.requestFocusInWindow();
    }
    
    //---------------------------------------------------------------------
    
    public void setPasswordPrompt(String prompt)
    {
        if (prompt == null)
            return;
        
        String formattedPrompt = prompt + ": ";
        this._passwordLabel.setText(formattedPrompt);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Displays the username/password dialog for view.
     *  
     *  @return JOptionPane.OK_OPTION if user entered values
     *          and select OK, otherwise JOptionPane.OK_CANCEL
     */
    
    public int showDialog()
    {
        return showDialog(null);
    }
    
    //---------------------------------------------------------------------
    
    /**
     *  Displays the username/password dialog for view.
     *  @param relativeComponent Component used to place dialog
     *  @return JOptionPane.OK_OPTION if user entered values
     *          and select OK, otherwise JOptionPane.OK_CANCEL
     */
    
    public int showDialog(Component relativeComponent)
    {
        int returnValue = JOptionPane.CANCEL_OPTION;
        int reply;
        boolean cont = true;
        
        while (cont)
        {
            //hackish way to get focus on the userfield...
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    if (_userField != null && _passField != null)
                    {
                        if (_userField.getText().equals(""))
                        {
                            _userField.requestFocus();
                        }
                        else
                        {
                            _passField.requestFocus();
                        }
                    }
                };
            });
            reply = JOptionPane.showConfirmDialog(relativeComponent, 
                    _panel, _title, JOptionPane.OK_CANCEL_OPTION,  
                    JOptionPane.PLAIN_MESSAGE);
            
            if (reply == JOptionPane.OK_OPTION)
            {
//                _username = _userField.getText();                
//                _password = new String(_passField.getPassword());
                
                readFields();
                
                boolean fieldsOk = checkFields();
                if (fieldsOk)
                {
                    returnValue = JOptionPane.OK_OPTION;                    
                    cont = false;
                }
               
                
//                if (this._username == null || this._username.equals(""))
//                {
//                    JOptionPane.showMessageDialog(null, "Username "+
//                        "field cannot be empty", "Error",
//                        JOptionPane.ERROR_MESSAGE);
//                }
//                else if (_password == null || _password.equals(""))
//                {
//                    JOptionPane.showMessageDialog(null, "Password "+
//                        "field cannot be empty", "Error",
//                        JOptionPane.ERROR_MESSAGE);
//                }
//                else
//                {                                       
//                    returnValue = JOptionPane.OK_OPTION;                    
//                    cont = false;
//                }
            }
            else
            {
                cont = false;   
            }
        } //end_while

        return returnValue;                        
    }

    //---------------------------------------------------------------------
    
    protected void readFields()
    {
        readUsernameField();
        readPasswordField();        
    }
    
    protected boolean checkFields()
    {
        boolean fieldsOK = true;
        
        if (this._username == null || this._username.equals(""))
        {
            JOptionPane.showMessageDialog(null, "Username "+
                "field cannot be empty", "Error",
                JOptionPane.ERROR_MESSAGE);
            fieldsOK = false;
        }
        else if (_password == null || _password.equals(""))
        {
            JOptionPane.showMessageDialog(null, "Password "+
                "field cannot be empty", "Error",
                JOptionPane.ERROR_MESSAGE);
            fieldsOK = false;
        }
        
        return fieldsOK;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the username from dialog.
     * @return username
     */
    
    public String getUsername()
    {
        return _username;
    }


    //---------------------------------------------------------------------
    
    protected void readUsernameField()
    {
        this._username = this._userField.getText();
    }
    
    //---------------------------------------------------------------------
    
    protected void readPasswordField()
    {
        this._password = new String(this._passField.getPassword());
    }
    
    //---------------------------------------------------------------------
    
    /**
     * If message was used during construction of this panel,
     * then it was disabled (greyed out) by default.  Use this
     * method to activate message label.
     * @param enabled Flag indicating whether or not message label
     *        will be enabled or disabled. 
     */
    
    public void enableMessageLabel(boolean enabled)
    {
        this._msgLabel.setEnabled(enabled);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns the password from dialog.
     * @return User password
     */
    
    public String getPassword()
    {
        return _password;   
    }
    
    //---------------------------------------------------------------------
    
    /*
    public  static void main(String[] args)
    {
        String username = System.getProperties().getProperty("user.name");
        LoginDialog dialog = new LoginDialog("Test", username);
        
        int rval = dialog.showDialog();
        if (rval == JOptionPane.OK_OPTION)
        {
            System.out.println("Username: "+dialog.getUsername());
            System.out.println("Password: "+dialog.getPassword());
        }
        else
        {
            System.out.println("Dialog cancelled.");
        }                                            
    }
    */
    
    //---------------------------------------------------------------------
  
}

