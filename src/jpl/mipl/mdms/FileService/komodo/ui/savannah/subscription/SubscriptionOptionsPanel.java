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
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * <b>Purpose:</b>
 * Panel containing components allowing user to specify options
 * for subscription.
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
 * 03/21/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SubscriptionOptionsPanel.java,v 1.13 2013/08/21 22:10:01 ntt Exp $
 */

public class SubscriptionOptionsPanel extends JPanel {
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected JPanel _bottomPanel;
    protected JCheckBox _checksumBox;
    protected JLabel _checksumLabel;
    protected JLabel _existFileLabel;
    protected JLabel _exitInkErrorLabel;
    protected JCheckBox _exitOnInvokeErrorBox;
    protected JLabel _filetypeLabel;
    protected JLabel _filetypeValueLabel;
    protected JTextField _invokeCmdField;
    protected JLabel _invokeCmdLabel;
    protected JButton _loadFromCacheButton;
    protected JPanel _logfilePanel;
    protected JButton _logfilePathButton;
    protected JTextField _logfilePathField;
    protected JLabel _logfilePathLabel;
    protected JComboBox _logfileRollingBox;
    protected JLabel _logfileRollingLabel;
    protected JTextField _mailMessageFromField;
    protected JLabel _mailMessageFromLabel;
    protected JTextField _mailMessageToField;
    protected JLabel _mailMessageToLabel;
    protected JTextField _mailReportAtField;
    protected JLabel _mailReportAtLabel;
    protected JTextField _mailReportToField;
    protected JLabel _mailReportToLabel;
    protected JTextField _mailSmtpHostField;
    protected JLabel _mailSmtpHostLabel;
    protected JPanel _midPanel;
    protected JRadioButton _noOptionButton;
    protected JLabel _outputLabel;
    protected JLabel _outputValueLabel;
    protected JLabel _purposeLabel;
    protected JCheckBox _receiptBox;
    protected JLabel _receiptLabel;
    protected JRadioButton _replaceButton;
    protected JPanel _replaceVersionPanel;
    protected JCheckBox _restartBox;
    protected JLabel _restartLabel;
    protected JCheckBox _safereadBox;
    protected JLabel _safereadLabel;
    protected JCheckBox _diffBox;
    protected JLabel _diffLabel;
    protected JSeparator _separator;
    protected JButton _storeToCacheButton;
    protected JPanel _subPanel;
    protected JPanel _topPanel;
    protected JRadioButton _versionButton;
    protected ButtonGroup _buttonGroup;
    protected JLabel _methodLabel;
    protected JPanel _methodOptionsPanel;
    protected ButtonGroup _methodButtonGroup;
    protected JRadioButton _methodPullButton;
    protected JRadioButton _methodPushButton;
    protected JLabel _sessionStayAliveLabel;
    protected JCheckBox _sessionStayAliveBox;
    protected JLabel _mailSilentReconnLabel;
    protected JCheckBox _mailSilentReconnBox;
    
    protected SubscriptionParameters _params;
    protected boolean _readOnly;
    //protected File _cacheFile = null;
    protected File _logfileFile = null;
    protected MetaParameterFileUtility _fileUtility;
    
    /** Creates new form newNotify */
    public SubscriptionOptionsPanel(SubscriptionParameters params) {
        this._params = params;
        initComponents();
        init();
    }
    
    /** Creates new form newNotify */
    public SubscriptionOptionsPanel(SubscriptionParameters params, 
                                                 boolean readOnly) 
    {
        this._params = params;
        this._readOnly = readOnly;
        initComponents();
        init();
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    protected void initComponents() {//GEN-BEGIN:initComponents
        GridBagConstraints gridBagConstraints;

        _subPanel = new JPanel();
        _topPanel = new JPanel();
        _purposeLabel = new JLabel();
        _separator = new JSeparator();
        _midPanel = new JPanel();
        _logfilePathLabel = new JLabel();
        _logfilePanel = new JPanel();
        _logfilePathButton = new JButton();
        _logfileRollingLabel = new JLabel();
        _invokeCmdLabel = new JLabel();
        _logfileRollingBox = new JComboBox();
        _outputLabel = new JLabel();
        _filetypeLabel = new JLabel();
        _checksumLabel = new JLabel();
        _filetypeValueLabel = new JLabel();
        _outputValueLabel = new JLabel();
        _replaceVersionPanel = new JPanel();
        _methodLabel = new JLabel();
        _methodOptionsPanel = new JPanel();
        _noOptionButton = new JRadioButton();
        _replaceButton = new JRadioButton();
        _versionButton = new JRadioButton();
        _methodPullButton = new JRadioButton();
        _methodPushButton = new JRadioButton();
        _exitInkErrorLabel = new JLabel();
        _mailReportToLabel = new JLabel();
        _mailMessageToLabel = new JLabel();
        _mailMessageFromLabel = new JLabel();
        _exitOnInvokeErrorBox = new JCheckBox();
        _checksumBox = new JCheckBox();
        _mailReportAtLabel = new JLabel();
        _mailSmtpHostLabel = new JLabel();
        _mailMessageFromField = new JTextField();
        _mailMessageToField = new JTextField();
        _mailReportToField = new JTextField();
        _mailReportAtField = new JTextField();
        _mailSmtpHostField = new JTextField();
        _invokeCmdField = new JTextField();
        _restartLabel = new JLabel();
        _receiptLabel = new JLabel();
        _safereadLabel = new JLabel();
        _diffLabel = new JLabel();
        _existFileLabel = new JLabel();
        _restartBox = new JCheckBox();
        _receiptBox = new JCheckBox();
        _safereadBox = new JCheckBox();
        _diffBox = new JCheckBox();
        _bottomPanel = new JPanel();
        _loadFromCacheButton = new JButton();
        _storeToCacheButton = new JButton();
        _sessionStayAliveLabel = new JLabel();
        _mailSilentReconnLabel = new JLabel();
        _sessionStayAliveBox = new JCheckBox();
        _mailSilentReconnBox = new JCheckBox();
        _logfilePathField = new JTextField() {
            public void setText(String text)
            {
                super.setText(text);
                final String fText = text;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run()
                    {
                        setToolTipText(fText);
                    }
                });
            }
        };
        _buttonGroup = new ButtonGroup();
        _buttonGroup.add(_noOptionButton);
        _buttonGroup.add(_replaceButton);
        _buttonGroup.add(_versionButton);
        
        _methodButtonGroup = new ButtonGroup();
        _methodButtonGroup.add(_methodPullButton);
        _methodButtonGroup.add(_methodPushButton);
        
        setLayout(new BorderLayout());

        setMinimumSize(new Dimension(480, 520));
        setPreferredSize(new Dimension(600, 545));
        _subPanel.setLayout(new GridBagLayout());

        _topPanel.setLayout(new GridBagLayout());


        String purposeStr = (this._readOnly) ? "Review" : "Supply";
        purposeStr +=  " subscription options.  ";
        
        _purposeLabel.setText(purposeStr + "Select 'Done' to continue.");
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
        _subPanel.add(_topPanel, gridBagConstraints);

        _midPanel.setLayout(new GridBagLayout());

        _midPanel.setPreferredSize(new Dimension(100, 53));
        _logfilePathLabel.setText("Logfile Path:");
        _logfilePathLabel.setToolTipText("Path of logfile to which session "
                                         + "info will be written");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_logfilePathLabel, gridBagConstraints);

        _logfilePanel.setLayout(new GridBagLayout());

        _logfilePathField.setText("Log file path");
        _logfilePathField.setToolTipText("Path of logfile to which session "
                                         + "info will be written");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.95;
        _logfilePanel.add(_logfilePathField, gridBagConstraints);

        _logfilePathButton.setText("...");
        _logfilePathButton.setToolTipText("Browse using file dialog");
        _logfilePathButton.setPreferredSize(new Dimension(25, 23));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        _logfilePanel.add(_logfilePathButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_logfilePanel, gridBagConstraints);

        _logfileRollingLabel.setText("Logfile Rolling:");
        _logfileRollingLabel.setToolTipText("Log file rolling option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_logfileRollingLabel, gridBagConstraints);

        _invokeCmdLabel.setText("Invoke Command:");
        _invokeCmdLabel.setToolTipText("Command to invoke when subscription "
                                       + "event occurs");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;//ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_invokeCmdLabel, gridBagConstraints);

        _logfileRollingBox.setModel(new DefaultComboBoxModel(
                SubscriptionConstants.LOG_ROLL_OPTIONS));
        _logfileRollingBox.setEditable(false);
        _logfileRollingBox.setToolTipText("Log file rolling option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;//ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_logfileRollingBox, gridBagConstraints);

        _outputLabel.setText("Output:");
        _outputLabel.setToolTipText("Destination directory for new files");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_outputLabel, gridBagConstraints);

        _filetypeLabel.setText("Filetype:");
        _filetypeLabel.setToolTipText("Subscription filetype");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(2, 15, 3, 0);
        _midPanel.add(_filetypeLabel, gridBagConstraints);

        _checksumLabel.setText("Checksum:");
        _checksumLabel.setToolTipText("Enable/disable checksum (CRC) option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_checksumLabel, gridBagConstraints);

        _filetypeValueLabel.setText("group:filetype");
        _filetypeValueLabel.setToolTipText("Subscription filetype");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(2, 3, 3, 15);
        _midPanel.add(_filetypeValueLabel, gridBagConstraints);

        _outputValueLabel.setText("/Output/directory/");
        _outputValueLabel.setToolTipText("Destination directory for new files");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 3, 3, 15);
        _midPanel.add(_outputValueLabel, gridBagConstraints);

        _replaceVersionPanel.setLayout(new GridBagLayout());

        _noOptionButton.setSelected(true);
        _noOptionButton.setText("None");
        _noOptionButton.setToolTipText("Preserve existing file");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        _replaceVersionPanel.add(_noOptionButton, gridBagConstraints);

        _replaceButton.setText("Replace");
        _replaceButton.setToolTipText("Overwrites existing file");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 20, 0, 0);
        _replaceVersionPanel.add(_replaceButton, gridBagConstraints);

        _versionButton.setText("Version");
        _versionButton.setToolTipText("Creates version of original file "
                                      + "using timestamp");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 20, 0, 0);
        _replaceVersionPanel.add(_versionButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_replaceVersionPanel, gridBagConstraints);

        _exitInkErrorLabel.setText("Exit on Invoke Error:");
        _exitInkErrorLabel.setToolTipText("Option to exit if invocation "
                                          + "error occurs");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_exitInkErrorLabel, gridBagConstraints);

        _mailReportToLabel.setText("Mail Report To:");
        _mailReportToLabel.setToolTipText("Comma-separated list of email "
              + "report recipients (e.g. name@domain.net,name2@domain.net)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_mailReportToLabel, gridBagConstraints);

        _mailMessageToLabel.setText("Mail Message To:");
        _mailMessageToLabel.setToolTipText("Comma-separated list of email "
             + "message recipients (e.g. name@domain.net,name2@domain.net)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_mailMessageToLabel, gridBagConstraints);

        _mailMessageFromLabel.setText("Mail Message From:");
        _mailMessageFromLabel.setToolTipText("Email address of sender "
                                             + "(e.g. name@domain.net)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_mailMessageFromLabel, gridBagConstraints);

        _exitOnInvokeErrorBox.setToolTipText("Option to exit if invocation "
                                             + "error occurs");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_exitOnInvokeErrorBox, gridBagConstraints);

        _checksumBox.setToolTipText("Enable/disable checksum (CRC) option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_checksumBox, gridBagConstraints);

        _mailReportAtLabel.setText("Mail Report At:");
        _mailReportAtLabel.setToolTipText("Comma-separated list of times to "
                                    + "send report (e.g. 12:00 AM, 6:30 PM)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_mailReportAtLabel, gridBagConstraints);

        _mailSmtpHostLabel.setText("Mail SMTP Host:");
        _mailSmtpHostLabel.setToolTipText("Hostname of SMTP mail server");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_mailSmtpHostLabel, gridBagConstraints);

        _mailMessageFromField.setText("from@someplace.com");
        _mailMessageFromField.setToolTipText("Email address of sender (e.g. "
                                             + "name@domain.net)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_mailMessageFromField, gridBagConstraints);

        _mailMessageToField.setText("to@someotherplace.com");
        _mailMessageToField.setToolTipText("Comma-separated list of email " +
                "message recipients (e.g. name@domain.net,name2@domain.net)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_mailMessageToField, gridBagConstraints);

        _mailReportToField.setText("him@there.com, her@there.com");
        _mailReportToField.setToolTipText("Comma-separated list of email " +
                "report recipients (e.g. name@domain.net,name2@domain.net)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_mailReportToField, gridBagConstraints);

        _mailReportAtField.setText("12:00 AM; 1:00 PM");
        _mailReportAtField.setToolTipText("Comma-separated list of times to " +
                "send report (e.g. 12:00 AM, 6:30 PM)");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_mailReportAtField, gridBagConstraints);

        _mailSmtpHostField.setText("mailhost.here.edu");
        _mailSmtpHostField.setToolTipText("Hostname of SMTP mail server");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 17; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_mailSmtpHostField, gridBagConstraints);

        _invokeCmdField.setText("command $filename");
        _invokeCmdField.setToolTipText("Command to invoke when subscription " +
                                       "event occurs");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11; //ntt
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 15);
        _midPanel.add(_invokeCmdField, gridBagConstraints);

        _restartLabel.setText("Restart:");
        _restartLabel.setToolTipText("Enable/disable restart option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_restartLabel, gridBagConstraints);

        _receiptLabel.setText("Receipt:");
        _receiptLabel.setToolTipText("Enable/disable receipt option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_receiptLabel, gridBagConstraints);

        _safereadLabel.setText("Saferead:");
        _safereadLabel.setToolTipText("Enable/disable saferead option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_safereadLabel, gridBagConstraints);
        
        _diffLabel.setText("Diff:");
        _diffLabel.setToolTipText("Enable/disable diff option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;//ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_diffLabel, gridBagConstraints);
        

        _existFileLabel.setText("Existing file handling:");
        _existFileLabel.setToolTipText("Select how to handle case when " +
                "file already exists");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;//ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_existFileLabel, gridBagConstraints);

        _restartBox.setToolTipText("Enable/disable restart option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_restartBox, gridBagConstraints);

        _receiptBox.setToolTipText("Enable/disable receipt option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_receiptBox, gridBagConstraints);

        _safereadBox.setToolTipText("Enable/disable saferead option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_safereadBox, gridBagConstraints);

        _diffBox.setToolTipText("Enable/disable diff option");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_diffBox, gridBagConstraints);
        
        _methodLabel.setText("Method:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 3, 0);
        _midPanel.add(_methodLabel, gridBagConstraints);

        _methodOptionsPanel.setLayout(new java.awt.GridBagLayout());

        _methodPullButton.setSelected(true);
        _methodPullButton.setText("Pull");
        _methodPullButton.setToolTipText("Periodic query for new files " +
                                         "(delayed response)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        _methodOptionsPanel.add(_methodPullButton, gridBagConstraints);

        _methodPushButton.setText("Push");
        _methodPushButton.setToolTipText("For immediate notification of new " +
                                         "files (uses more resources)");        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        _methodOptionsPanel.add(_methodPushButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 3, 15);
        _midPanel.add(_methodOptionsPanel, gridBagConstraints);
        
        
        _mailSilentReconnLabel.setText("Silent Reconnect:");
        _mailSilentReconnLabel.setToolTipText("No email message will be sent out for reconnection messages");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_mailSilentReconnLabel, gridBagConstraints);

        _mailSilentReconnBox.setToolTipText("No email message will be sent out for reconnection messages");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_mailSilentReconnBox, gridBagConstraints);
        
        
        _sessionStayAliveLabel.setText("Persist session:");
        _sessionStayAliveLabel.setToolTipText("Enable to persist session across applications");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 19; //ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 15, 3, 0);
        _midPanel.add(_sessionStayAliveLabel, gridBagConstraints);

        _sessionStayAliveBox.setToolTipText("Restart subscription in later application runs");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 19;//ntt
        gridBagConstraints.anchor = GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new Insets(0, 2, 3, 0);
        _midPanel.add(_sessionStayAliveBox, gridBagConstraints);
        
        
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        _subPanel.add(_midPanel, gridBagConstraints);

        _bottomPanel.setLayout(new GridBagLayout());

        _loadFromCacheButton.setText("Load Options");
        _loadFromCacheButton.setToolTipText("Load most recently stored " +
                "options");
        _loadFromCacheButton.setMnemonic(KeyEvent.VK_L);  
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10, 10, 10, 15);
        _bottomPanel.add(_loadFromCacheButton, gridBagConstraints);

        _storeToCacheButton.setText("Store Options");
        _storeToCacheButton.setToolTipText("Store current options for " +
                                           "future use");  
        _storeToCacheButton.setMnemonic(KeyEvent.VK_S);  
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10, 15, 10, 10);
        _bottomPanel.add(_storeToCacheButton, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.LAST_LINE_START;
        _subPanel.add(_bottomPanel, gridBagConstraints);

        add(_subPanel, BorderLayout.CENTER);

    }//GEN-END:initComponents


    //---------------------------------------------------------------------
    
    protected void init()
    {
        //create ref to cache file
//        String cacheRoot = System.getProperty("user.home");
//        cacheRoot += File.separator + Constants.RESTARTDIR +
//                     File.separator + SubscriptionConstants.CACHE_FILE_PREFIX +
//                     Constants.RESTARTEXTENSION;        
//        this._cacheFile = new File(cacheRoot);
        
        this._fileUtility = new MetaParameterFileUtility(this);
        setComponentValuesFromParameters();
        initComponentListeners();
        
        if (this._readOnly)
            makeReadOnly();
    }
    
    //---------------------------------------------------------------------
    
    protected void makeReadOnly()
    {
        _exitOnInvokeErrorBox.setEnabled(false);
        _restartBox.setEnabled(false);
        _mailMessageFromField.setEnabled(false);
        _mailMessageToField.setEnabled(false);
        _mailReportToField.setEnabled(false);
        _mailReportAtField.setEnabled(false);
        _mailSmtpHostField.setEnabled(false);
        _invokeCmdField.setEnabled(false);        
        _loadFromCacheButton.setEnabled(false);
        _storeToCacheButton.setEnabled(false);
        _methodPullButton.setEnabled(false);
        _methodPushButton.setEnabled(false);
        _logfilePathField.setEnabled(false);
        _logfileRollingBox.setEnabled(false);
        _noOptionButton.setEnabled(false);
        _replaceButton.setEnabled(false);
        _versionButton.setEnabled(false);       
        _checksumBox.setEnabled(false);       
        _restartBox.setEnabled(false);
        _receiptBox.setEnabled(false);
        _safereadBox.setEnabled(false);
        _diffBox.setEnabled(false);
        _sessionStayAliveBox.setEnabled(false);
        _mailSilentReconnBox.setEnabled(false);
        
        _logfilePanel.remove(_logfilePathButton);
        _subPanel.remove(_bottomPanel);
    }
    
    //---------------------------------------------------------------------
    
    protected void setComponentValuesFromParameters()
    {
        String value;
        File file;
        String filetype  = this._params.getFiletype();
        String outputDir = this._params.getOutputDirectory();
        this._filetypeValueLabel.setText(filetype);
        this._outputValueLabel.setText(outputDir);
        
        //-------------------------
        
        this._restartBox.setSelected(this._params.getRestart());
        this._checksumBox.setSelected(this._params.getCrc());
        this._receiptBox.setSelected(this._params.getReceipt());
        this._safereadBox.setSelected(this._params.getSaferead());
        this._diffBox.setSelected(this._params.getDiff());
        if (this._params.getVersion())
        {
            this._versionButton.setSelected(true);
            this._params.setReplace(false);
        }
        else if (this._params.getReplace())
        {
            this._replaceButton.setSelected(true);
        }
        else
        {
            this._noOptionButton.setSelected(true);
        }

        //method: pull / push
        if (this._params.getPush())
        {
            this._methodPushButton.setSelected(true);
        }
        else if (this._params.getPull())
        {
            this._methodPullButton.setSelected(true);
        }
        else
        {
            this._methodPullButton.setSelected(true);
        }
        
        //-------------------------
        
        //log file
        value = this._params.getLogFilename();
        this._logfilePathField.setText(value);
        if (value != null)
        {
            File outdirFile = new File(outputDir);
            file = (new File(value));
            //set relative to output dir if not absolute
            if (!file.isAbsolute())
                file = new File(outdirFile, value);

            this._logfileFile = file;
        }
        else
        {
            this._logfileFile = null;
        }
        
        //-------------------------
        
        value = this._params.getLogfileRolling();
        if (value != null)
            this._logfileRollingBox.setSelectedItem(value.toUpperCase());
        else
            this._logfileRollingBox.setSelectedItem(
                                    SubscriptionConstants.LOG_ROLL_NEVER);
        
        //-------------------------
        
        this._invokeCmdField.setText(this._params.getInvokeCommand());
        this._exitOnInvokeErrorBox.setSelected(this._params.getInvokeExitOnError());
        this._mailMessageFromField.setText(this._params.getMailMessageFrom());
        this._mailMessageToField.setText(this._params.getMailMessageTo());
        this._mailReportToField.setText(this._params.getMailReportTo());
        this._mailReportAtField.setText(this._params.getMailReportAt());
        this._mailSmtpHostField.setText(this._params.getMailSMTPHost());
        
        //-------------------------
            
        this._sessionStayAliveBox.setSelected(this._params.getSessionStayAlive());
        this._mailSilentReconnBox.setSelected(this._params.getMailSilentReconn());

        //-------------------------
        
        updateCacheFileButtons();
    }
    
    //---------------------------------------------------------------------
    
    protected void updateCacheFileButtons()
    {
//        boolean canStore = !this._cacheFile.isDirectory();
//        boolean canLoad = (this._cacheFile.canRead() && canStore);
        boolean canStore = true;
        boolean canLoad = true;
        
        //enable load button iff cache file exists
        this._loadFromCacheButton.setEnabled(canLoad);
        this._storeToCacheButton.setEnabled(canStore);
    }
    
    //---------------------------------------------------------------------
    
    protected void initComponentListeners()
    {
        //create action listener that causes focus loss event
        ActionListener actLoseFocus = new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                Object source = ae.getSource();
                if (source instanceof JComponent)
                    ((JComponent) source).transferFocus();
            }
        };
        
        //create Item listener for the check box components
        ItemListener cbListener = new ItemListener() {
            public void itemStateChanged(ItemEvent ie)
            {
                Object source = ie.getSource();
                boolean enabled = ie.getStateChange() == ItemEvent.SELECTED;
                
                if (source == _restartBox)
                {
                    _params.setRestart(enabled);
                }
                else if (source == _exitOnInvokeErrorBox)
                {
                    _params.setInvokeExitOnError(enabled);
                }
                else if (source == _checksumBox)
                {
                    _params.setCrc(enabled);
                }
                else if (source == _receiptBox)
                {
                    _params.setReceipt(enabled);
                }
                else if (source == _safereadBox)
                {
                    _params.setSaferead(enabled);
                }
                else if (source == _diffBox)
                {
                    _params.setDiff(enabled);
                }
                else if (source == _logfileRollingBox)
                {
                    String val = (String) _logfileRollingBox.getSelectedItem();
                    if (val.equalsIgnoreCase(SubscriptionConstants.LOG_ROLL_NEVER))
                        val = null;
                    _params.setLogfileRolling(val);    
                }
                else if (source == _sessionStayAliveBox)
                {
                    _params.setSessionStayAlive(enabled);    
                }
                else if (source == _mailSilentReconnBox)
                {
                    _params.setMailSilentReconn(enabled);    
                }
                
            }
        };
        
        //create action listener for radio buttons
        ActionListener rbActListener = new ActionListener() {
          public void actionPerformed(ActionEvent ae)
          {
              Object source = ae.getSource();
              
              if (source == _noOptionButton)
              {
                  _params.setReplace(false);
                  _params.setVersion(false);
              }
              else if (source == _versionButton)
              {
                  _params.setReplace(false);
                  _params.setVersion(true);
              }
              else if (source == _replaceButton)
              {
                  _params.setReplace(true);
                  _params.setVersion(false);
              }
              else if (source == _methodPullButton)
              {
                  _params.setPush(false);
                  _params.setPull(true);
              }
              else if (source == _methodPushButton)
              {
                  _params.setPull(false);
                  _params.setPush(true);
              }
          }
        };
        
        //create focus listener for the fields
        FocusListener fieldListener = new FocusAdapter() {
          public void focusLost(FocusEvent fe)
          {
              Object source = fe.getSource();
              JTextField field = (JTextField) source;
              String txt = field.getText();
              if (txt != null && txt.equals(""))
                  txt = null;
              
              if (field == _logfilePathField)
              {
                  //check that dir exists...?
                  if (txt != null)
                      _logfileFile = new File(txt);
                  else
                      _logfileFile = null;
                  
                  _params.setLogFilename(txt);
              }
              else if (field == _invokeCmdField)
              {
                  _params.setInvokeCommand(txt);
              }
              else if (field == _mailMessageFromField)
              {
                  _params.setMailMessageFrom(txt);
              }
              else if (field == _mailMessageToField)
              {
                  _params.setMailMessageTo(txt);
              }
              else if (field == _mailReportToField)
              {
                  _params.setMailReportTo(txt);
              }
              else if (field == _mailReportAtField)
              {
                  _params.setMailReportAt(txt);
              }
              else if (field == _mailSmtpHostField)
              {
                  _params.setMailSMTPHost(txt);
              }
          }
        };
        
        //-------------------------
        
        //check box /combo box components
        this._restartBox.addItemListener(cbListener);
        this._exitOnInvokeErrorBox.addItemListener(cbListener);
        this._logfileRollingBox.addItemListener(cbListener);
        this._checksumBox.addItemListener(cbListener);
        this._receiptBox.addItemListener(cbListener);
        this._safereadBox.addItemListener(cbListener);
        this._diffBox.addItemListener(cbListener);
        this._sessionStayAliveBox.addItemListener(cbListener);
        this._mailSilentReconnBox.addItemListener(cbListener);
        
        //-------------------------
        
        this._noOptionButton.addActionListener(rbActListener);
        this._replaceButton.addActionListener(rbActListener);
        this._versionButton.addActionListener(rbActListener);
        
        this._methodPullButton.addActionListener(rbActListener);
        this._methodPushButton.addActionListener(rbActListener);
        
        //-------------------------
        
        //text field components
        this._logfilePathField.addFocusListener(fieldListener);
        this._logfilePathField.addActionListener(actLoseFocus);
        this._invokeCmdField.addFocusListener(fieldListener);
        this._invokeCmdField.addActionListener(actLoseFocus);
        this._mailMessageFromField.addFocusListener(fieldListener);
        this._mailMessageFromField.addActionListener(actLoseFocus);
        this._mailMessageToField.addFocusListener(fieldListener);
        this._mailMessageToField.addActionListener(actLoseFocus);
        this._mailReportToField.addFocusListener(fieldListener);
        this._mailReportToField.addActionListener(actLoseFocus);
        this._mailReportAtField.addFocusListener(fieldListener);
        this._mailReportAtField.addActionListener(actLoseFocus);
        this._mailSmtpHostField.addFocusListener(fieldListener);
        this._mailSmtpHostField.addActionListener(actLoseFocus);
        
        //-------------------------
        
        //buttons (log file path, load from cache, store to cache)
        this._logfilePathButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               File file = (_logfileFile != null) ? 
                           _logfileFile.getParentFile() :
                           new File(_params.getOutputDirectory());
               JFileChooser fc = new JFileChooser(file);
               fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
               fc.setDialogTitle("Select Log Filename");
               int opt = fc.showDialog(SubscriptionOptionsPanel.this,"Select");
               if (opt == JFileChooser.APPROVE_OPTION)
               {
                   _logfileFile = fc.getSelectedFile();
                   _params.setLogFilename(_logfileFile.getAbsolutePath());
                   _logfilePathField.setText(_logfileFile.getAbsolutePath());
               }
           }
         });
        
        this._loadFromCacheButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                boolean success = _fileUtility.loadParametersFromFile(_params);
                
                if (success)
                {
                    setComponentValuesFromParameters();
                    updateCacheFileButtons();
                }
            }
         });
        
        this._storeToCacheButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae)
            {
                boolean success = _fileUtility.storeParametersToFile(_params);
                
                if (success)
                {
                    updateCacheFileButtons();
                }
            }
         });
        
//        //load options from the cache file
//        this._loadFromCacheButton.addActionListener(new ActionListener() {
//           public void actionPerformed(ActionEvent ae)
//           {
//               //this should never happen, but just in case
//               if (!_cacheFile.canRead() || _cacheFile.isDirectory())
//               {
//                   JOptionPane.showMessageDialog(SubscriptionOptionsPanel.this,
//                                   "Cache file does not exist.\n"
//                                   + "Select 'OK' to abort loading.",
//                                   "Cache Error", JOptionPane.ERROR_MESSAGE);
//                   _loadFromCacheButton.setEnabled(false);
//                   return;
//               }
//               
//               //retain ft, outdir, and domain values 
//               String filetype   = _params.getFiletype();
//               URL    domainFile = _params.getDomainFile();
//               String outdir     = _params.getOutputDirectory();
//               String username   = _params.getUsername();
//               String password   = _params.getPassword();
//               
//               //open input stream to file cache
//               InputStream inStream = null;
//               try {
//                   inStream = new FileInputStream(_cacheFile);
//                   MetaParameterIO mpIo = new DefaultMetaParameterIO();
//                   mpIo.read(_params, inStream, MetaParameterIO.FORMAT_PLAIN);
//               } catch (IOException ioEx) {
//                   JOptionPane.showMessageDialog(SubscriptionOptionsPanel.this,
//                           "Could not load options from cache file.\n"
//                           +"Reason: "+ioEx.getMessage(), "Cache Error", 
//                           JOptionPane.ERROR_MESSAGE);
//               } finally {
//                   if (inStream != null)
//                       try {  inStream.close(); } catch (IOException ioEx) {}
//                   _params.setFiletype(filetype);
//                   _params.setDomainFile(domainFile);
//                   _params.setOutputDirectory(outdir);
//                   _params.setUsername(username);
//                   _params.setPassword(password);
//                   setComponentValuesFromParameters();
//               }
//               updateCacheFileButtons();
//           }
//        });
//        
//        //store current options to the cache file
//        this._storeToCacheButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent ae)
//            {                
//                boolean success = false;
//                OutputStream outStream = null;
//                
//                try {
//                    outStream = new FileOutputStream(_cacheFile);
//                    MetaParameterIO mpIo = new DefaultMetaParameterIO();
//                    mpIo.write(_params, outStream, MetaParameterIO.FORMAT_PLAIN);
//                    success = true;
//                } catch (IOException ioEx) {
//                    JOptionPane.showMessageDialog(SubscriptionOptionsPanel.this,
//                            "Could not write options to cache file.\n"
//                            +"Reason: "+ioEx.getMessage(), "Cache Error", 
//                            JOptionPane.ERROR_MESSAGE);
//                } finally {
//                    if (outStream != null)
//                    {
//                        try {  outStream.flush();
//                               outStream.close(); } catch (IOException ioEx) {} 
//                    }
//                }
//                updateCacheFileButtons();
//            }
//         });
    }
    
    //---------------------------------------------------------------------
   

    // End of variables declaration//GEN-END:variables
    
}
