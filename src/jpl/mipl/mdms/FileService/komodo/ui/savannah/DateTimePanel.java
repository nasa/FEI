package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * <b>Purpose: </b> The DateTime Panel is a GUI component where the user can
 * specify a date/time value using either (1) YYYY-MM-ddThh:mm:ss format, or
 * using the number of milliseconds since the epoch of January 1, 1970, 00:00:00
 * GMT. Updating one field will update all others.
 * 
 * <PRE>
 * 
 * Copyright 2004, California Institute of Technology. ALL RIGHTS RESERVED. U.S.
 * Government Sponsorship acknowledge. 2004.
 * 
 * </PRE>
 * 
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who          What</B>
 * ----------------------------------------------------------------------------
 * 12/16/2003        Nick         Initial Release
 * 02/10/2004        Nick         Added TimeZone to options and data
 * 04/01/2004        Nick         Added custom timezones for PST and PDT, as 
 *                                GMT-8, GMT-7 respectively.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DateTimePanel.java,v 1.6 2004/10/08 00:22:26 ntt Exp $
 *  
 */

public class DateTimePanel extends JPanel {
    private static String className_ = "DateTimePanel";

    public static final int YEAR = 0;

    public static final int MONTH = 1;

    public static final int DAY = 2;

    public static final int HOUR = 3;

    public static final int MINUTE = 4;

    public static final int SECOND = 5;

    protected PropertyChangeSupport changes = new PropertyChangeSupport(this);

    protected JPanel _mainPanel;

    protected JPanel _calPanel;

    protected JPanel _milliPanel;

    protected JPanel _buttonPanel;

    protected JTextField _calYearField;

    protected JTextField _calMonField;

    protected JTextField _calDayField;

    protected JTextField _calHourField;

    protected JTextField _calMinField;

    protected JTextField _calSecField;

    protected JComboBox _calTimeZone;

    protected JTextField _calField;

    protected JTextField _milliField;

    protected JButton _nowButton;

    protected JButton _okButton;

    protected JButton _applyButton;

    protected JButton _cancelButton;

    protected ActionListener _calListener;

    protected ActionListener _tzListener;

    protected ActionListener _milliListener;

    protected FocusListener _focusListener;

    protected String _currentCal = "1970-01-01T00:00:00";

    protected long _currentDate = 0;

    protected long _previousDate = 0;

    protected boolean _initialized = false;

    protected String _calToolTipText = "Enter time in YYYY-MM-ddThh:mm:ss "
            + "format";

    protected Pattern _calPattern;

    protected Matcher _calMatcher;

    protected SimpleDateFormat _dateFormat;

    protected boolean _isEnabled;

    protected TimeZone _timeZone;

    protected TimeZone _previousTimeZone;

    protected Vector _timeZoneIds;

    //Playing god with space time continuum...
    protected TimeZone _pstTimeZone;

    protected TimeZone _pdtTimeZone;

    //---------------------------------------------------------------------

    /**
     * Constructor.
     */

    public DateTimePanel() {
        //create CAL format verification objects
        String patStr = "\\p{Digit}{4}-\\p{Digit}{2}" + "-\\p{Digit}{2}T"
                + "\\p{Digit}{2}:\\p{Digit}{2}:" + "\\p{Digit}{2}";
        _calPattern = Pattern.compile(patStr);
        _calMatcher = _calPattern.matcher("");
        _timeZone = TimeZone.getTimeZone("UTC");
        _dateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH:mm:ss");
        _dateFormat.setTimeZone(_timeZone);
        _pstTimeZone = new SimpleTimeZone(-28800000, "Pacific Standard Time");
        _pdtTimeZone = new SimpleTimeZone(-25200000, "Pacific Daylight Time");

        buildGui();
    }

    //---------------------------------------------------------------------

    /**
     * Adds listener for property change of model.
     * 
     * @param l Object implementing the PropertyChangeListener interface to be
     *        added
     */

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changes.addPropertyChangeListener(l);
    }

    //---------------------------------------------------------------------

    /**
     * Removes listener for property change of model.
     * 
     * @param l Object implementing the PropertyChangeListener interface to be
     *        removed
     */

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changes.removePropertyChangeListener(l);
    }

    //---------------------------------------------------------------------

    protected void buildGui() {
        //---------------------

        _isEnabled = true;
        _calListener = new CalActionListener();
        _milliListener = new MilliActionListener();
        _tzListener = new TimeZoneActionListener();
        _focusListener = new TextFieldFocusListener();

        _mainPanel = new JPanel();
        _mainPanel.setLayout(new BoxLayout(_mainPanel, BoxLayout.Y_AXIS));

        //---------------------
        //build cal panel

        _calPanel = new JPanel();
        _calPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Calendar Format"));
        _calPanel.setToolTipText(_calToolTipText + " (" + _timeZone.getID()
                + ")");
        _calPanel.setLayout(new BoxLayout(_calPanel, BoxLayout.Y_AXIS));

        //set up time zone box
        String[] ids = TimeZone.getAvailableIDs();
        _timeZoneIds = new Vector();
        for (int i = 0; i < ids.length; ++i)
            _timeZoneIds.add(ids[i]);
        Collections.sort(_timeZoneIds);
        _timeZoneIds.add(0, "UTC");
        _timeZoneIds.add(1, TimeZone.getDefault().getID());
        _timeZoneIds.add(2, _pstTimeZone.getID()); //HACK!!
        _timeZoneIds.add(3, _pdtTimeZone.getID()); //HACK!!
        _calTimeZone = new JComboBox(_timeZoneIds);
        _calTimeZone.setEditable(false);
        _calTimeZone.setToolTipText("Time Zone");
        _calTimeZone.setSelectedItem(_timeZone.getID());
        _calTimeZone.addActionListener(_tzListener);
        _calPanel.add(_calTimeZone);

        //set up time fields
        _calYearField = new JTextField("1970");//, 5);
        _calMonField = new JTextField("01");//,3);
        _calDayField = new JTextField("01");//,3);
        _calHourField = new JTextField("00");//,3);
        _calMinField = new JTextField("00");//,3);
        _calSecField = new JTextField("00");//,3);
        _calYearField.setToolTipText("Year (1970+)");
        _calMonField.setToolTipText("Month (01-12)");
        _calDayField.setToolTipText("Day (01-31)");
        _calHourField.setToolTipText("Hour (00-23)");
        _calMinField.setToolTipText("Minute (00-59)");
        _calSecField.setToolTipText("Second (00-59)");
        _calYearField.setName("CALENDAR_YEAR_FIELD");
        _calMonField.setName("CALENDAR_MONTH_FIELD");
        _calDayField.setName("CALENDAR_DAY_FIELD");
        _calHourField.setName("CALENDAR_HOUR_FIELD");
        _calMinField.setName("CALENDAR_MINUTE_FIELD");
        _calSecField.setName("CALENDAR_SECOND_FIELD");

        Font oldFont = _calYearField.getFont();
        Font newFont = new Font("Monospaced", Font.PLAIN, oldFont.getSize());//+1);
        _calYearField.setFont(newFont);
        _calMonField.setFont(newFont);
        _calDayField.setFont(newFont);
        _calHourField.setFont(newFont);
        _calMinField.setFont(newFont);
        _calSecField.setFont(newFont);

        JPanel tmpPanel = new JPanel();
        tmpPanel.add(_calYearField);
        tmpPanel.add(new JLabel("-"));
        tmpPanel.add(_calMonField);
        tmpPanel.add(new JLabel("-"));
        tmpPanel.add(_calDayField);
        tmpPanel.add(new JLabel("T"));
        tmpPanel.add(_calHourField);
        tmpPanel.add(new JLabel(":"));
        tmpPanel.add(_calMinField);
        tmpPanel.add(new JLabel(":"));
        tmpPanel.add(_calSecField);
        tmpPanel.add(Box.createHorizontalGlue());
        _calPanel.add(tmpPanel);
        _calPanel.add(Box.createVerticalStrut(5));

        tmpPanel = new JPanel();
        tmpPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEmptyBorder(), "Time Zone :"));
        tmpPanel.add(_calTimeZone);
        _calPanel.add(tmpPanel);
        _calPanel.add(Box.createVerticalStrut(3));

        _calYearField.addActionListener(_calListener);
        _calMonField.addActionListener(_calListener);
        _calDayField.addActionListener(_calListener);
        _calHourField.addActionListener(_calListener);
        _calMinField.addActionListener(_calListener);
        _calSecField.addActionListener(_calListener);

        _calYearField.addFocusListener(_focusListener);
        _calMonField.addFocusListener(_focusListener);
        _calDayField.addFocusListener(_focusListener);
        _calHourField.addFocusListener(_focusListener);
        _calMinField.addFocusListener(_focusListener);
        _calSecField.addFocusListener(_focusListener);

        //---------------------

        //build milli panel 1060544964000 - 13 spaces
        _milliPanel = new JPanel();
        _milliPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Millisecond Format"));
        _milliPanel.setToolTipText("Enter time in milliseconds since epoch "
                + "(00:00:00 GMT, Jan 1, 1970) format");
        _milliField = new JTextField("0000000000000");//, 13);
        _milliField.addActionListener(_milliListener);
        _milliField.addFocusListener(_focusListener);
        _milliField.setFont(newFont);
        _milliField.setName("MILLISECONDS_FIELD");
        _nowButton = new JButton("Now");
        _nowButton.setToolTipText("Select to set to current time");
        _nowButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setToCurrentTime();
            }
        });
        _milliPanel.add(_milliField);
        _milliPanel.add(new JLabel("ms"));
        _milliPanel.add(Box.createHorizontalStrut(5));
        _milliPanel.add(_nowButton);
        _milliPanel.add(Box.createHorizontalGlue());

        //---------------------
        //build button panel

        _buttonPanel = new JPanel();

        _buttonPanel.setLayout(new BoxLayout(_buttonPanel, BoxLayout.X_AXIS));

        _okButton = new JButton("OK");
        _okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                _calListener.actionPerformed(new ActionEvent(_applyButton,
                        ActionEvent.ACTION_PERFORMED, "APPLY_CLICKED"));
                _milliListener.actionPerformed(new ActionEvent(_applyButton,
                        ActionEvent.ACTION_PERFORMED, "APPLY_CLICKED"));
                changes.firePropertyChange("dateInMillis", new Long(
                        _previousDate), new Long(_currentDate));
                changes.firePropertyChange("dateTimeZone", _previousTimeZone,
                        _timeZone);
            }
        });
        _okButton.setToolTipText("Alerts listeners of updated date");

        _cancelButton = new JButton("Cancel");
        _cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateView();
            }
        });

        //_buttonPanel.add(_okButton);
        //_buttonPanel.add(_applyButton);
        //_buttonPanel.add(new JLabel(""));
        _buttonPanel.add(Box.createHorizontalGlue());
        _buttonPanel.add(_okButton);
        _buttonPanel.add(_cancelButton);
        _buttonPanel.add(Box.createHorizontalGlue());
        //_buttonPanel.add(new JLabel(""));
        //_buttonPanel.add(_cancelButton);

        //---------------------

        //bring it all together

        _mainPanel.add(_calPanel);
        _mainPanel.add(_milliPanel);
        //_mainPanel.add(tmpPanel);
        _mainPanel.add(_buttonPanel);

        setToCurrentTime();

        add(_mainPanel);
    }

    //---------------------------------------------------------------------

    protected String[] parseCal(String calStr) {
        String[] parts = null;

        if (calStr == null || calStr.length() != 19)
            return parts;

        _calMatcher.reset(calStr);
        if (!_calMatcher.matches())
            return parts;

        parts = new String[6];
        parts[0] = calStr.substring(0, 4); //yr
        parts[1] = calStr.substring(5, 7); //mon
        parts[2] = calStr.substring(8, 10); //day
        parts[3] = calStr.substring(11, 13); //hr
        parts[4] = calStr.substring(14, 16); //min
        parts[5] = calStr.substring(17, 19); //sec

        if (Integer.parseInt(parts[1]) > 12 || Integer.parseInt(parts[1]) == 0)
            return null;
        if (Integer.parseInt(parts[2]) > 31 || Integer.parseInt(parts[2]) == 0)
            return null;
        if (Integer.parseInt(parts[3]) > 23)
            return null;
        if (Integer.parseInt(parts[4]) > 59)
            return null;
        if (Integer.parseInt(parts[5]) > 59)
            return null;

        return parts;
    }

    //---------------------------------------------------------------------

    /**
     * Given a string array of length six, each index representing: (0) year,
     * (1) day of year, (2) hour of day, (3) minute, (4) second, (5)
     * millisecond; returns a string in CAL-format
     * 
     * @param calArray CAL components
     * @return CAL formatted string created from components
     */

    protected String createCal(String[] calArray) {
        StringBuffer buffer = new StringBuffer();
        String calStr;

        if (calArray == null || calArray.length != 6)
            return null;

        buffer.append(calArray[0]).append("-").append(calArray[1]).append("-")
                .append(calArray[2]).append("T").append(calArray[3])
                .append(":").append(calArray[4]).append(":")
                .append(calArray[5]);

        calStr = buffer.toString();

        _calMatcher.reset(calStr);
        if (!_calMatcher.matches())
            return null;
        else
            return calStr;
    }

    //---------------------------------------------------------------------

    /**
     * Converts the time from a milliseconds format long to the CAL-formatted
     * string.
     * 
     * @param millis Time in milliseconds
     * @return Same time in CAL-format
     */

    public String millisToCal(long millis) {
        if (millis < 0)
            return null;
        else
            return _dateFormat.format(new Date(millis));
    }

    //---------------------------------------------------------------------

    /**
     * Converts the time from a CAL-formatted string to the long milliseconds
     * format.
     * 
     * @param calStr Time in CAL-format
     * @return Same time in milliseconds
     */
    public long calToMillis(String calStr) {
        if (calStr == null || calStr.length() != 19)
            return (long) -1;

        Date date;
        long millis;

        try {
            date = _dateFormat.parse(calStr);
        } catch (ParseException pe) {
            return (long) -1;
        }

        return date.getTime();
    }

    //---------------------------------------------------------------------

    /**
     * Returns string form of an integer taking reqDigits places, filling in
     * zeros on the left.
     * 
     * @param number Primitive integer to be formatted.
     * @param reqDigits Number of significant digits in whole number
     * @return String of the formatted number.
     */

    protected String formatInteger(long number, int reqDigits) {
        StringBuffer pattern = new StringBuffer();
        DecimalFormat formatter;
        long l_number = (long) number;

        for (int i = 0; i < reqDigits; ++i) {
            pattern.append("0");
        }

        formatter = new DecimalFormat(pattern.toString());
        return formatter.format(l_number);
    }

    //---------------------------------------------------------------------

    /**
     * Sets date according to millisecond format
     * 
     * @param millis Time in millisecond long representation
     */

    public void setDate(long millis) {
        if (millis != _currentDate) {
            _previousDate = _currentDate;
            _currentDate = millis;
            _currentCal = millisToCal(_currentDate);

            /*
             * changes.firePropertyChange("dateInMillis", new Long(oldDate), new
             * Long(millis));
             */
        }

        //update GUI
        updateView();
    }

    //---------------------------------------------------------------------

    /**
     * Sets date according to CAL string format
     * 
     * @param millis Time in CAL format
     */

    //set date according to CAL format
    public void setDate(String cal) {
        long millis = calToMillis(cal);
        if (millis == -1L) {
            throw new IllegalArgumentException(className_
                    + "::setDate(): Could not parse CAL string " + "\'" + cal
                    + "\'");
        }

        setDate(millis);
    }

    //---------------------------------------------------------------------

    public void setToCurrentTime() {
        setDate(System.currentTimeMillis());
    }

    //---------------------------------------------------------------------

    public long getDate() {
        return _currentDate;
    }

    //---------------------------------------------------------------------

    public void setEnabled(boolean enableFlag) {
        _isEnabled = enableFlag;
        _calYearField.setEnabled(enableFlag);
        _calMonField.setEnabled(enableFlag);
        _calDayField.setEnabled(enableFlag);
        _calHourField.setEnabled(enableFlag);
        _calMinField.setEnabled(enableFlag);
        _calSecField.setEnabled(enableFlag);
        _milliField.setEnabled(enableFlag);
        _nowButton.setEnabled(enableFlag);
    }

    //---------------------------------------------------------------------

    public boolean isEnabled() {
        return _isEnabled;
    }

    //---------------------------------------------------------------------

    public String getCalDate() {
        return _currentCal + " (" + _timeZone.getID() + ")";
        // + " ("+_dateFormat.getTimeZone().getID()+")";
    }

    //---------------------------------------------------------------------

    public void setTimeZone(String timeZoneId) {
        if (timeZoneId == null || timeZoneId.equals(""))
            throw new IllegalArgumentException(className_ + "::setTimeZone(): "
                    + "Parameter must be non-null, non-empty string.");

        if (!_timeZoneIds.contains(timeZoneId))
            throw new IllegalArgumentException(className_ + "::setTimeZone(): "
                    + "Unrecognized time zone id: " + timeZoneId);

        //HACK!!!
        if (timeZoneId.equals(_pstTimeZone.getID()))
            setTimeZone(_pstTimeZone);
        else if (timeZoneId.equals(_pdtTimeZone.getID()))
            setTimeZone(_pdtTimeZone);
        else
            setTimeZone(TimeZone.getTimeZone(timeZoneId));
    }

    //---------------------------------------------------------------------

    public void setTimeZone(TimeZone timeZone) {
        if (timeZone == null)
            throw new IllegalArgumentException(className_ + "::setTimeZone(): "
                    + "Parameter cannot be null.");
        if (!timeZone.getID().equals(_timeZone.getID())) {
            Date date = null;
            _previousTimeZone = _timeZone;
            _timeZone = timeZone;
            _dateFormat.setTimeZone(_timeZone);

            //test to make sure of epoch boundary
            try {
                date = _dateFormat.parse(_currentCal);
                if (date.getTime() < 0L)
                    throw new Exception();
            } catch (Exception ex) {
                _timeZone = _previousTimeZone;
                _dateFormat.setTimeZone(_timeZone);
                _calTimeZone.setSelectedItem(_timeZone.getID());
                JOptionPane
                        .showMessageDialog(
                                DateTimePanel.this,
                                "Could not "
                                        + "parse the year date with new time zone\n"
                                        + "Date falls below the epoch Jan 01, 1970 00:00:00 GMT",
                                "Format Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            _calTimeZone.setSelectedItem(_timeZone.getID());
            _calPanel.setToolTipText(_calToolTipText + " (" + _timeZone.getID()
                    + ")");
            //_dateFormat.setTimeZone(_timeZone);
            setDate(_currentCal);
            /*
             * changes.firePropertyChange("dateTimeZone", _previousTimeZone,
             * _timeZone);
             */
        }
    }

    //---------------------------------------------------------------------

    public TimeZone getTimeZone() {
        return _timeZone;
    }

    //---------------------------------------------------------------------

    protected void updateView() {
        //millis
        _milliField.setText(formatInteger(_currentDate, 13));
        _milliField.setCaretPosition(0);

        //cal
        _currentCal = millisToCal(_currentDate);
        String[] calArray = parseCal(_currentCal);

        //time zone
        _calTimeZone.setSelectedItem(_timeZone.getID());

        if (calArray != null && calArray.length == 6) {
            _calYearField.setText(calArray[0]);
            _calMonField.setText(calArray[1]);
            _calDayField.setText(calArray[2]);
            _calHourField.setText(calArray[3]);
            _calMinField.setText(calArray[4]);
            _calSecField.setText(calArray[5]);

            _calYearField.setCaretPosition(0);
            _calMonField.setCaretPosition(0);
            _calDayField.setCaretPosition(0);
            _calHourField.setCaretPosition(0);
            _calMinField.setCaretPosition(0);
            _calSecField.setCaretPosition(0);
        } else {
            _calYearField.setText("????");
            _calMonField.setText("??");
            _calDayField.setText("??");
            _calHourField.setText("??");
            _calMinField.setText("??");
            _calSecField.setText("??");
        }
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        JPanel panel = new DateTimePanel();
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    //---------------------------------------------------------------------

    class CalActionListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            String strVal;
            StringBuffer buffer = new StringBuffer();
            int intVal;
            String year, doy, hr, min, s, ms;

            //verify input ---------------------------

            //year
            strVal = _calYearField.getText();
            if (strVal == null || strVal.length() != 4) {
                JOptionPane.showMessageDialog(DateTimePanel.this, "Could not "
                        + "parse the year entry: " + strVal + "\n"
                        + "Please verify that format is: YYYY (1970+)",
                        "Format Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                if (Integer.parseInt(strVal) < 1970)
                    throw new NumberFormatException("Must be greater "
                            + "than 1970");
            } catch (NumberFormatException nfEx) {
                JOptionPane.showMessageDialog(DateTimePanel.this,
                        "Year must be a 4 digit representation greater "
                                + "than 1970", "Format Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            buffer.append(strVal).append("-");
            //month
            strVal = _calMonField.getText();
            if (strVal == null || strVal.length() != 2) {
                if (strVal.matches("\\p{Digit}{1}"))
                    strVal = "0" + strVal;
                else
                    strVal = "01";
            }
            buffer.append(strVal).append("-");
            //day
            strVal = _calDayField.getText();
            if (strVal == null || strVal.length() != 2) {
                if (strVal.matches("\\p{Digit}{1}"))
                    strVal = "0" + strVal;
                else
                    strVal = "01";
            }
            buffer.append(strVal).append("T");
            //hour
            strVal = _calHourField.getText();
            if (strVal == null || strVal.length() != 2) {
                if (strVal.matches("\\p{Digit}{1}"))
                    strVal = "0" + strVal;
                else
                    strVal = "00";
            }
            buffer.append(strVal).append(":");
            //minute
            strVal = _calMinField.getText();
            if (strVal == null || strVal.length() != 2) {
                if (strVal.matches("\\p{Digit}{1}"))
                    strVal = "0" + strVal;
                else
                    strVal = "00";
            }
            buffer.append(strVal).append(":");
            //second
            strVal = _calSecField.getText();
            if (strVal == null || strVal.length() != 2) {
                if (strVal.matches("\\p{Digit}{1}"))
                    strVal = "0" + strVal;
                else
                    strVal = "00";
            }
            buffer.append(strVal);

            String calStr = buffer.toString();
            long millis = calToMillis(calStr);
            if (millis == -1L) {
                JOptionPane.showMessageDialog(DateTimePanel.this, "Could not "
                        + "parse the calendar entry: " + calStr + "\n"
                        + "Please verify that format is: YYYY-MM-ddThh:mm:ss",
                        "Format Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (millis < 0L) {
                JOptionPane.showMessageDialog(DateTimePanel.this, "Could not "
                        + "parse the calendar entry: " + calStr + "\n"
                        + "Please verify that date us greater than "
                        + "(00:00:00 GMT, January 1, 1970)", "Format Error",
                        JOptionPane.ERROR_MESSAGE);
                return;

            }

            //if (millis != _currentDate)
            setDate(millis);
        }
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    class TimeZoneActionListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            String tzId = (String) _calTimeZone.getSelectedItem();

            if (tzId == null)
                return;
            //<HACK>
            if (tzId.equals(_pstTimeZone.getID()))
                setTimeZone(_pstTimeZone);
            else if (tzId.equals(_pdtTimeZone.getID()))
                setTimeZone(_pdtTimeZone);
            //</HACK>
            else
                setTimeZone(TimeZone.getTimeZone(tzId));
        }
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    class MilliActionListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            String milliStr = _milliField.getText();
            long millis;
            try {
                millis = Long.parseLong(milliStr);
                if (millis < 0L)
                    throw new NumberFormatException("Must be non-negative");
            } catch (NumberFormatException nfEx) {

                JOptionPane.showMessageDialog(DateTimePanel.this, "Could not "
                        + "parse the millisecond entry: " + milliStr + "\n"
                        + "Please enter a non-negative integer value only. "
                        + "(ie 1075496071881)", "Format Error",
                        JOptionPane.ERROR_MESSAGE);
                updateView();
                return;
            }

            //if (millis != _currentDate)
            setDate(millis);
        }
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

    class TextFieldFocusListener implements FocusListener {
        public void focusGained(FocusEvent fe)
        {
            JTextField tf = (JTextField) fe.getSource();
            tf.selectAll();
        }

        public void focusLost(FocusEvent fe)
        {
            JTextField tf = (JTextField) fe.getSource();
            String compName = tf.getName();
            int len = tf.getText().length();
            tf.select(0, 0);
        }
    }

    //---------------------------------------------------------------------
    //---------------------------------------------------------------------

}

