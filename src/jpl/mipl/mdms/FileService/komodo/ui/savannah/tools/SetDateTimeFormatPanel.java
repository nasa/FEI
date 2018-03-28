package jpl.mipl.mdms.FileService.komodo.ui.savannah.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.SimpleDateFormat;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import jpl.mipl.mdms.FileService.util.DateTimeFormatter;

/**
 * <b>Purpose:</b>
 *  User-inferface panel for setting the date/time format string.
 *
 *   <PRE>
 *   Copyright 2009, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2009.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 05/02/2009        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SetDateTimeFormatPanel.java,v 1.2 2009/05/27 22:43:51 ntt Exp $
 *
 */

public class SetDateTimeFormatPanel extends JPanel
{
    protected javax.swing.JRadioButton ccsdsaButton;
    private javax.swing.JTextField formatField;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JRadioButton otherButton;
    protected javax.swing.JRadioButton utcButton;
        
    
    protected String defaultManualFormat = "yyyy.MM.dd G 'at' HH:mm:ss z";   
    protected String format;
    
    //---------------------------------------------------------------------
    
    public SetDateTimeFormatPanel()
    {
        this(null);
    }
    
    //---------------------------------------------------------------------
    
    public SetDateTimeFormatPanel(String currentFormat)
    {
        super();
       
        this.format = currentFormat;
        
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
    
    private void initComponents() 
    {
        java.awt.GridBagConstraints gridBagConstraints;

        infoLabel = new javax.swing.JLabel();
        ccsdsaButton = new javax.swing.JRadioButton();
        utcButton = new javax.swing.JRadioButton();
        otherButton = new javax.swing.JRadioButton();
        formatField = new javax.swing.JTextField();
        helpLabel = new javax.swing.JLabel();

        setName("Form"); // NOI18N
        setLayout(new java.awt.GridBagLayout());
        
        infoLabel.setText("Select the date/time format.  "); // NOI18N
        infoLabel.setName("infoLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 10);
        add(infoLabel, gridBagConstraints);
               
        ccsdsaButton.setText("CCSDSA"); // NOI18N
        ccsdsaButton.setToolTipText("Consultative Committee for Space Data Systems (CCSDS) A. \n" +
        		                    "Format: "+DateTimeFormatter.FORMAT_CCSDSA_PATTERN);
        ccsdsaButton.setName("ccsdsaButton"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        add(ccsdsaButton, gridBagConstraints);
        
        utcButton.setText("UTC"); // NOI18N
        utcButton.setToolTipText("Coordinated Universal Time (UTC). \n" +
                                 "Format: "+DateTimeFormatter.FORMAT_UTC_PATTERN);
        utcButton.setName("utcButton"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        add(utcButton, gridBagConstraints);
        
        String otherTooltip = "User specified formatting.\n See API of " +
        		              "SimpleDateFormat.java for info.";
        otherButton.setText("Other: "); // NOI18N
        otherButton.setName("otherButton"); // NOI18N
        otherButton.setToolTipText(otherTooltip);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
        add(otherButton, gridBagConstraints);
        
        formatField.setText("yyyy.MM.dd G 'at' HH:mm:ss z"); // NOI18N
        formatField.setName("formatField"); // NOI18N
        formatField.setToolTipText(otherTooltip);
        formatField.setColumns(36);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
        add(formatField, gridBagConstraints);
        
        helpLabel.setText("For information on formats, please visit Javadoc for SimpleDateFormat.");
        helpLabel.setName("helpLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 10);
        add(helpLabel, gridBagConstraints);
        
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.ccsdsaButton);
        buttonGroup.add(this.utcButton);
        buttonGroup.add(this.otherButton);
    }
    
    //---------------------------------------------------------------------
    
    protected void initListeners()
    {
              
        ActionListener buttonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                JRadioButton button = (JRadioButton) ae.getSource();
                
                if (!button.isSelected())
                    return;
                
                if (button.equals(ccsdsaButton))
                {
                    format = DateTimeFormatter.FORMAT_CCSDSA_PATTERN;
                }
                else if (button.equals(utcButton))
                {
                    format = DateTimeFormatter.FORMAT_UTC_PATTERN;
                }
                else if (button.equals(otherButton))
                {                    
                    String txt = formatField.getText();  
                    txt = checkManual(txt);
                    if (txt != null)
                        format = txt;                    
                }                
            }
        };
        
        this.ccsdsaButton.addActionListener(buttonListener);
        this.utcButton.addActionListener(buttonListener);
        this.otherButton.addActionListener(buttonListener);
        
        //hack to convert action events into focus events for the field
        this.formatField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                formatField.transferFocus();
            }
        });
        this.formatField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent fe)
            {
                String txt = formatField.getText();
                txt = checkManual(txt);
                if (txt != null)
                    format = txt;                
            }            
        });                
    }

    //---------------------------------------------------------------------
    
    /**
     * Performs a well-formed-ness check on potential manual format string.
     * @param Format as retrieved from field
     * @return Potentially updated string, or null if string is invalid.
     */
    
    protected String checkManual(String input)
    {
        if (otherButton.isSelected())
        {
            if (input == null || input.equals(""))
            {
                JOptionPane.showMessageDialog(SetDateTimeFormatPanel.this, 
                               "Format field cannot be empty.\n" +
                               "Default value entered", "Format Error", 
                               JOptionPane.WARNING_MESSAGE);
                formatField.setText(defaultManualFormat);
                input = defaultManualFormat;
            }
            
            String trimmed = input.trim();
            if (!trimmed.equals(input))
            {
                formatField.setText(defaultManualFormat);
                input = trimmed;
            }
            
            try {
                new SimpleDateFormat(input);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(SetDateTimeFormatPanel.this, 
                        "Illegal format: "+input+"\nReason: "+ex.getMessage()+
                        "\nReplacing with default expression",
                        "Format Error", 
                        JOptionPane.WARNING_MESSAGE);
                formatField.setText(defaultManualFormat);
                input = defaultManualFormat;

            }
   
            return input;            
        }
        else return null;
        
        //return input;
    }
    
    //---------------------------------------------------------------------
    
    protected void initValues()
    {
        //-------------------------
        
        if (this.format == null)
        {
            this.format = DateTimeFormatter.FORMAT_CCSDSA_ID;
        }
        
        //-------------------------
        
        if (this.format.equals(DateTimeFormatter.FORMAT_CCSDSA_ID) || 
            this.format.equals(DateTimeFormatter.FORMAT_CCSDSA_PATTERN))
        {
            this.ccsdsaButton.setSelected(true);            
        }
        else if (this.format.equals(DateTimeFormatter.FORMAT_UTC_ID) || 
                 this.format.equals(DateTimeFormatter.FORMAT_UTC_PATTERN))
        {
            this.utcButton.setSelected(true);
        } 
        else
        {
            this.formatField.setText(this.format);
            this.otherButton.setSelected(true);
        }
        
        //-------------------------
    }
    
    //---------------------------------------------------------------------
    
    public String getFormat()
    {        
        return this.format;
    }
    
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        String[] groups = { "my group", "group grope", "music group"};
        String username = "mr_peterman";
        
        
        SetDateTimeFormatPanel panel = new SetDateTimeFormatPanel();
        
        JFrame frame = new JFrame("Test");
        frame.getContentPane().add(panel);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    //---------------------------------------------------------------------
}

