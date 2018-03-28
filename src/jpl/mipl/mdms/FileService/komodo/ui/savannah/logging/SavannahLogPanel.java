package jpl.mipl.mdms.FileService.komodo.ui.savannah.logging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;

import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b>
 * Panel for displaying entries in the log history.  This panel acts
 * as a component listener that can be added to a window widget for 
 * notification as to when this panel is viewable.  When in a viewable
 * state, all entries received will be displayed.  When not viewable, 
 * however, no updates are performed until the panel becomes visible,
 * during which the list of entries is reprinted.
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
 * <B>Date              Who             What</B>
 * ----------------------------------------------------------------------------
 * 11/30/2004        Nick             Initial Release
 * 04/13/2005        Nick             Added search capability
 * 04/14/2005        Nick             Added printing capability
 * 09/27/2005        Nick             Added benchmark level
 * 10/05/2005        Nick             Updates only when viewable using the
 *                                    ComponentListener interface.
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahLogPanel.java,v 1.12 2009/09/04 17:05:15 ntt Exp $
 *
 */

public class SavannahLogPanel extends JPanel implements ComponentListener,
                                        PropertyChangeListener, Printable
{
    protected int MAX_DOC_OFFSET = 50000;
    protected double REMOVAL_FACTOR = .35;
    
    protected SavannahLogModel _model;
    protected StyledDocument _doc;
    protected final List    _logList;
    protected JButton _searchButton;
    protected JButton _clearButton;
    protected JButton _printButton;
    protected String  _title;
    protected JTextPane _textPane;
    protected JScrollPane _scrollPane;
    protected JLabel _titleLabel;
    
    //style and icon info
    Color  evenColor = new Color(230, 235, 235);
    ImageIcon _debugIcon = null, _warnIcon  = null;
    ImageIcon _infoIcon  = null, _errorIcon = null;
    ImageIcon _traceIcon = null, _unkIcon   = null;
    ImageIcon _fatalIcon = null, _benchIcon = null;;
    Style _messageStyle, _boldStyle, _iconStyle, _italicStyle;
    
    protected final int ICON_STYLE    = 3;
    protected final int ITALIC_STYLE  = 2;
    protected final int BOLD_STYLE    = 1;
    protected final int MESSAGE_STYLE = 0;
    
    protected final String[] _styleNames = new String[] {"message", "bold",
                                                         "italic",  "icon"};
    
    //logger
    private Logger _logger = Logger.getLogger(SavannahLogPanel.class.getName());
    
    //other data
    SearchDialog _searchDialog = null;
    PrintView _printView = null;
    boolean _notifySuccess = false;
    protected final Object _syncLock = new Object();
    protected final List _messageSizeList;
    
    protected boolean _canUpdate = false;
    
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor using empty string for panel title
     * @param model Instance of SavannahTransferModel
     */
    
    public SavannahLogPanel(SavannahLogModel model)
    {
        this(model, "");
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor
     * @param model Instance of SavannahTransferModel
     * @param title The label of panel
     */
    
    public SavannahLogPanel(SavannahLogModel model, String title)
    {
        this._model = model;
        this._model.addPropertyChangeListener(this);
        this._title = ((title != null) ? title : " ");
        this._logList    = new Vector();
        this._messageSizeList = new Vector();
        init();
    }

    //---------------------------------------------------------------------
    
    /**
     * Initialization code.  To be called by constructor only. 
     */
    
    protected void init()
    {      
        this.setLayout(new BorderLayout());
        
        loadIcons();
        
        //construct the pane
        _textPane = new JTextPane();
        _textPane.setMaximumSize(new Dimension(250, _textPane.getMaximumSize().height));
        _textPane.setPreferredSize(new Dimension(500, 300));
        _textPane.setEditable(false);
        _doc = _textPane.getStyledDocument();
        
        initStyle(_doc);        
        
        _scrollPane = new JScrollPane(_textPane);
        this.add(_scrollPane, BorderLayout.CENTER);
        
        //construct the buttons
        JPanel buttonPanel = createButtons();
        this.add(buttonPanel, BorderLayout.SOUTH);
        
        //construct title label
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
        _titleLabel = new JLabel(_title+" XXXXXXXXXXX");
        _titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titlePanel.add(Box.createHorizontalGlue());
        titlePanel.add(_titleLabel);
        titlePanel.add(Box.createHorizontalGlue());
        this.add(titlePanel, BorderLayout.NORTH);
        _titleLabel.setText(_title);
        
        //some spacing
        this.add(Box.createHorizontalStrut(8), BorderLayout.WEST);
        this.add(Box.createHorizontalStrut(8), BorderLayout.EAST);
   
        //set the list
        //updateLogList(_model.getLogEntries());        
    }
 
    //---------------------------------------------------------------------
    
    /**
     * Loads the icon images from resources.
     */
    
    protected void loadIcons()
    {
        int ICON_WIDTH  = 16;
        int ICON_HEIGHT = 16;
        int HINTS = Image.SCALE_SMOOTH;
        
        //FATAL
        URL imageURL = SavannahLogPanel.class.getResource(
                                "resources/fatal.png"); 
        if (imageURL != null)
            _fatalIcon = new ImageIcon(imageURL, "Fatal");
        _fatalIcon.setImage(_fatalIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));
        
        //ERROR
        imageURL = SavannahLogPanel.class.getResource(
                                "resources/error.png");      
        if (imageURL != null)
            _errorIcon = new ImageIcon(imageURL, "Error");
        _errorIcon.setImage(_errorIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));
        
        //WARN
        imageURL = SavannahLogPanel.class.getResource(
                                "resources/warn.png");      
        if (imageURL != null)
            _warnIcon = new ImageIcon(imageURL, "Warning");
        _warnIcon.setImage(_warnIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));

        //INFO
        imageURL = SavannahLogPanel.class.getResource(
                                "resources/info.png");      
        if (imageURL != null)
            _infoIcon = new ImageIcon(imageURL, "Info");
        _infoIcon.setImage(_infoIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));
        
        //BENCHMARK
        imageURL = SavannahLogPanel.class.getResource(
                                "resources/bench.png");      
        if (imageURL != null)
            _benchIcon = new ImageIcon(imageURL, "Benchmark");
        _benchIcon.setImage(_benchIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));
        
        //DEBUG
        imageURL = SavannahLogPanel.class.getResource(
                                "resources/debug.png");      
        if (imageURL != null)
            _debugIcon = new ImageIcon(imageURL, "Debug");
        _debugIcon.setImage(_debugIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));
        
        //TRACE
        imageURL = SavannahLogPanel.class.getResource(
                                "resources/trace.png");      
        if (imageURL != null)
            _traceIcon = new ImageIcon(imageURL, "Trace");
        _traceIcon.setImage(_traceIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));
        
        //UNKNOWN
        imageURL = SavannahLogPanel.class.getResource(
                                "resources/unknown.png");      
        if (imageURL != null)
            _unkIcon = new ImageIcon(imageURL, "Unknown");
        _unkIcon.setImage(_unkIcon.getImage().getScaledInstance(
                ICON_WIDTH, ICON_HEIGHT, HINTS));
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Initializes the style information for the text pane document.
     * @param doc Instance of StyleDocument
     */
    
    protected void initStyle(StyledDocument doc)
    {   
        Style def = StyleContext.getDefaultStyleContext().getStyle(
                                            StyleContext.DEFAULT_STYLE);
        _messageStyle = doc.addStyle(_styleNames[MESSAGE_STYLE], def);
        StyleConstants.setFontFamily(def, "SansSerif");
        //StyleConstants.setFirstLineIndent(_messageStyle, 2.0f);
        StyleConstants.setLeftIndent(_messageStyle, 2.0f);
        
        _boldStyle = doc.addStyle(_styleNames[BOLD_STYLE], _messageStyle);
        StyleConstants.setBold(_boldStyle, true);
        
        _italicStyle = doc.addStyle(_styleNames[ITALIC_STYLE], _messageStyle);
        StyleConstants.setItalic(_italicStyle, true);
        //StyleConstants.setFirstLineIndent(_messageStyle, 2.0f);
        StyleConstants.setLeftIndent(_messageStyle, .10f);
        StyleConstants.setBackground(_messageStyle, Color.WHITE);
        
        _iconStyle = doc.addStyle(_styleNames[ICON_STYLE], _messageStyle);
        StyleConstants.setAlignment(_iconStyle, StyleConstants.ALIGN_CENTER);
        StyleConstants.setIcon(_iconStyle, this._unkIcon);
        
        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Create the button panel used for this panel.
     */
    
    protected JPanel createButtons()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        
        //construct the buttons
        this._searchButton = new JButton("Search");
        this._searchButton.setMnemonic(KeyEvent.VK_S);
        this._clearButton = new JButton("Clear");
        this._clearButton.setMnemonic(KeyEvent.VK_C);
        this._printButton = new JButton("Print");
        this._printButton.setMnemonic(KeyEvent.VK_P);
        
        //set disabled initially
        this._searchButton.setEnabled(true);
        this._clearButton.setEnabled(false);
        this._printButton.setEnabled(false);
        
        //lay em out
        panel.add(Box.createHorizontalGlue());
        panel.add(_searchButton);
        panel.add(Box.createHorizontalStrut(25));
        panel.add(_printButton);
        panel.add(Box.createHorizontalStrut(25));
        panel.add(_clearButton);
        panel.add(Box.createHorizontalGlue());
        
        return panel;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Enqueues a new queue event for each log entry in the List.  Also 
     * adds a clear event, and boundary event, so that previous entries
     * can be erased and waiting entries can be discarded.
     * @param newList List of LogEntry instances to be printed 
     */
    
    private void updateLogList(final List newList)
    {
        appendToTitle("(Refreshing)");
        
        //add new entries
        Iterator it = newList.iterator();
        while (it.hasNext())
        {
            insertEntry((LogEntry) it.next());
        }          
        
        appendToTitle(null); 
    }
    
    //---------------------------------------------------------------------
  
    protected void appendToTitle(String text)
    {
        final String newTitle = (text == null) ? _title :
                                                 _title + " " + text + " ";
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            _titleLabel.setText(newTitle); }});
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Inserts new LogEntry into log list.  Called by GUI-thread
     * @param entry Instance of LogEntry
     */
    
    protected void insertEntry(LogEntry entry)
    {
        String msg      = entry.getMessage();
        int level       = entry.getLevel();
        ImageIcon icon  = toIcon(level);
        String levelStr = entry.getLevelString();
        String ts = DateTimeUtil.getDateCCSDSAString(new Date(entry.getTimestamp()));
        Color bgColor = Color.WHITE;
        
        //in general, don't report these
        String location    = null;
        Throwable throwable = null;
        
        if (/*_logger.isDebugEnabled() ||*/ _logger.isTraceEnabled())
        {
            location  = entry.getLocation();
            throwable = entry.getThrowable();
        }

        _logList.add(entry);
        printMessage(msg, levelStr, ts, icon, location, throwable, bgColor);        
        updateButtons();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Prints a log message to the text pane.
     * @param mesg Log message
     * @param title Log level or title
     * @param ts Timestamp of message
     * @param icon Icon associated with log level
     * @param location Location associated with log message, can be null
     * @param throwable Associated throwable, can be null
     */
    
    protected  void printMessage(String mesg, String title, String ts, 
                                 ImageIcon icon, String location,
                                 Throwable throwable, Color bgColor)
    {
        try {
            Position startPosition, endPosition;
            int totalOffset = _doc.getLength();
            startPosition = _doc.getEndPosition();
            
            StyleConstants.setIcon(_iconStyle, icon);
            _doc.insertString(_doc.getLength(), " ", _iconStyle);
            _doc.insertString(_doc.getLength(), " "+title, _boldStyle);
            _doc.insertString(_doc.getLength(), "  ("+ts+")", _italicStyle);

            _doc.insertString(_doc.getLength(), "\n"+mesg, _messageStyle);
            if (throwable != null)
            {
                String throwString = stackTraceToString(throwable.getStackTrace());
                _doc.insertString(_doc.getLength(), "\nStack trace: ", _italicStyle);
                _doc.insertString(_doc.getLength(), throwString, _messageStyle);              
            }
            
            if (location != null && !location.equals(""))
            {
                _doc.insertString(_doc.getLength(), "\nLocation:", _italicStyle);
                _doc.insertString(_doc.getLength(), location, _messageStyle);
            }
            
            _doc.insertString(_doc.getLength(), "\n\n", _messageStyle);
            
            endPosition = _doc.getEndPosition();            
            //totalOffset = endPosition.getOffset() - startPosition.getOffset();
            totalOffset = _doc.getLength() - totalOffset;
            Integer mesgSize = new Integer(totalOffset);
            //System.out.println("MESSAGE SIZE = "+totalOffset);
            this._messageSizeList.add(mesgSize);
            
            ensureLimit();
            
        } catch (BadLocationException blEx) {
            blEx.printStackTrace();
        }

        ensureBottomView();
    }

    //---------------------------------------------------------------------
    
    
    /** 
     * Called after adding new message, removes older messages until
     * we are at the appropriate size...
     */
    
    protected void ensureLimit()
    {
        int docSize = _doc.getLength();
        int amountToRemove = 0;

        
        if (docSize > MAX_DOC_OFFSET)
        {
            int minToRemove = (int) (REMOVAL_FACTOR * MAX_DOC_OFFSET);
            while (minToRemove > 0 && !_messageSizeList.isEmpty()) 
            {
                Integer nextMesgItem = (Integer) _messageSizeList.remove(0);
                int nextMesgSize = nextMesgItem.intValue();
                amountToRemove += nextMesgSize;
                minToRemove -= nextMesgSize;   
                
                //remove entry from internal log list too!
                if (!_logList.isEmpty())
                    _logList.remove(0);
            }
        
            if (amountToRemove > 0)
            {
                appendToTitle("(Cleaning)");
                //System.out.println("Removing "+amountToRemove+" offset values.");
                try {
                    _doc.remove(0, amountToRemove);
                } catch (BadLocationException blEx) {
                    blEx.printStackTrace();
                }
                appendToTitle(null);
            }
        }
        
    }
    
    //---------------------------------------------------------------------
    
    private void clearPanel()
    {
        _textPane.setText("");               
        _logList.clear();
        updateButtons();        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Converts an array of StackTraceElements to a String of the stack
     * trace.
     * @param elems StackTraceElement array from Throwable.getStackTrace(),
     * where the entry at index 0 is the closest call to the source of the
     * exception.
     * @return String representation of stack trace
     */
    protected static String stackTraceToString(final StackTraceElement[] elems)
    {
        StringBuffer buffer = new StringBuffer();
        
        if (elems.length > 0)
            buffer.append(elems[0].toString()+"\n");
        
        for (int i = 1; i < elems.length; ++i)
            buffer.append("\tat  "+elems[i].toString()+"\n");
        
        return buffer.toString();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Forces bottom of message pane to be displayed.
     */
    
    protected void ensureBottomView()
    {
        _textPane.setCaretPosition(_doc.getLength());
    }
    //---------------------------------------------------------------------
    
    /**
     * Returns icon associated with log level.  If unrecognized level,
     * then a default "unknown" icon will be returned.
     * @param level Log level
     * @return Icon associated with log level
     */
    
    protected ImageIcon toIcon(int level)
    {
        ImageIcon icon = this._unkIcon;
        switch (level)
        {
            case LogEntry.LEVEL_FATAL:
                icon = this._fatalIcon;
                break;
            case LogEntry.LEVEL_ERROR:
                icon = this._errorIcon;
                break;
            case LogEntry.LEVEL_WARN:
                icon = this._warnIcon;
                break;
            case LogEntry.LEVEL_INFO:
                icon = this._infoIcon;
                break;
            case LogEntry.LEVEL_BENCHMARK:
                icon = this._benchIcon;
                break;
            case LogEntry.LEVEL_DEBUG:
                icon = this._debugIcon;
                break;
            case LogEntry.LEVEL_TRACE:
                icon = this._traceIcon;
                break;      
        }
        return icon;
    }
    
    //---------------------------------------------------------------------
    
    /** 
     * This method gets called when a bound property is changed.
     * @param pce A PropertyChangeEvent object describing the event 
     *            source and the property that has changed.
     */
    
    public void propertyChange(PropertyChangeEvent pce)
    {
        String propName = pce.getPropertyName();
        
        //if (propName.equalsIgnoreCase(SavannahLogModel.LOG_QUEUE_PROPERTY))
        //{
        //    List newList = this._model.getLogEntries();
        //    updateLogList(newList);
        //    
        //}
        //if (propName.equalsIgnoreCase(SavannahLogModel.LOG_QUEUE_PROPERTY))
       // {
        //    List newList = this._model.getLogEntries();
        //    _updateLogList(newList);
        //}
        //else 
        if (propName.equalsIgnoreCase(SavannahLogModel.LOG_NEW_ENTRY))
        {
            if (_canUpdate)
            {
                LogEntry entry = (LogEntry) pce.getNewValue();
                if (entry != null)
                {
                    insertEntry(entry);
                }
            }
        }
    } 
    
    //---------------------------------------------------------------------
    
    public int selectMatch(String text, boolean cont, boolean caseSensitive)
    {
        int currentIndex, startIndex, endIndex, nextIndex;
        String docText;
        Document doc;
        
        doc = this._textPane.getDocument();        
        currentIndex = cont ? this._textPane.getSelectionEnd() : 0;       
        startIndex   = doc.getStartPosition().getOffset();
        endIndex     = doc.getLength() - 1;
        
        if (currentIndex > endIndex)
            currentIndex = startIndex;
        
        try {
            docText = doc.getText(startIndex, endIndex);
        } catch (BadLocationException blEx) {
            docText = "";
        }
        
        if (!caseSensitive)
        {
            text    = text.toLowerCase();
            docText = docText.toLowerCase();
        }
        
        nextIndex = docText.indexOf(text, currentIndex);
        if (nextIndex == -1)
        {
            return - 1;
        }
        
        this._textPane.select(nextIndex, nextIndex+text.length());
        this._textPane.grabFocus();
        
        return nextIndex;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Updates buttons behavior and availablility based on state of list
     */
    
    protected void updateButtons()
    {   
        //this._removeButton.setAction(new RemoveAction(selected));
        this._clearButton.setAction(new ClearAction());
        this._searchButton.setAction(new SearchAction());
        this._printButton.setAction(new PrintAction());
        //this._removeButton.setEnabled(canRemove);
        this._clearButton.setEnabled(!_logList.isEmpty());
        this._searchButton.setEnabled(!_logList.isEmpty());
    }
    
    //---------------------------------------------------------------------

    /**
     *  Unregister self as property listener, clear internal list, nullify
     *  state.  To be called 
     */
    
    public void nullify()
    {        
        if (this._model != null)
        {
            this._model.removePropertyChangeListener(this);
            this._model = null;
        }
        
        if (this._logList != null)
        {
            this._logList.clear();
        }
        
        if (this._textPane != null)   
        {
            //this._jlist.removeListSelectionListener(this);
            this._textPane = null;
            this._doc = null; 
        }
    }
    
    //---------------------------------------------------------------------
    
    
    //---------------------------------------------------------------------
    
    
    //=====================================================================
    //=====================================================================

    /**
     * Action associated with clear button.
     */
    
    class ClearAction extends AbstractAction
    {
        //-----------------------------------------------------------------
        
        public ClearAction()
        {
            super("Clear");
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            int answer = JOptionPane.showConfirmDialog(
                    SavannahLogPanel.this,
                    "Clear all entries?",
                    "Confirm Remove",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION)
            {
                _model.resetLogQueue();
                clearPanel();
            }
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    /**
     * Action associated with print button.
     */
    
    class PrintAction extends AbstractAction
    {
        //-----------------------------------------------------------------
        
        public PrintAction()
        {
            super("Print");
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {           
            Thread runner = new Thread() {
                public void run() 
                {
                    printData();
                }
            };
            runner.start();
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    /**
     * Action associated with search button.
     */
    class SearchAction extends AbstractAction
    {
        //-----------------------------------------------------------------
        
        public SearchAction()
        {
            super("Search");
        }
        
        //-----------------------------------------------------------------
        
        public void actionPerformed(ActionEvent ae)
        {
            if (_searchDialog == null)
            {
                _searchDialog = new SearchDialog();
            }
            _searchDialog.setLocationRelativeTo(SavannahLogPanel.this);
            _searchDialog.setVisible(true);
        }
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    
    /**
     * Dialog used during search.
     */
    
    class SearchDialog extends JDialog
    {
        JPanel _mainPanel;
        JButton _searchBtn;
        JButton _cancelBtn;
        JTextField _textField;

        //-----------------------------------------------------------------

        public SearchDialog()
        {
            super((Frame) null, "Search Dialog", false);
            buildGui();
        }

        //-----------------------------------------------------------------

        protected void buildGui()
        {
            _mainPanel = new JPanel();
            _mainPanel.setLayout(new BoxLayout(_mainPanel, BoxLayout.Y_AXIS));

            //-----------------------

            JPanel textPanel = new JPanel();
            textPanel.setBorder(BorderFactory.createTitledBorder(
               BorderFactory.createEtchedBorder() ,"Find String"));           
            _textField = new JTextField(20);
            textPanel.add(_textField);
            _textField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae)
                {
                    String text = _textField.getText();
                    if (text == null || text.equals(""))
                        return;
                    _searchBtn.doClick();
                }
            });

            //-----------------------
            // Button Panel
            
            JPanel buttonPanel = new JPanel();
            //buttonPanel.setLayout(new GridLayout(1,3));
            buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));
                        
            _searchBtn = new JButton("Find");
            _searchBtn.setToolTipText("Find next instance of string");
            _searchBtn.setMnemonic(KeyEvent.VK_F);
            _searchBtn.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae)
                {
                    String text = _textField.getText();
                    if (text == null || text.equals(""))
                    {
                        JOptionPane.showMessageDialog(SearchDialog.this, 
                                   "Text field is empty.",
                                   "Search Error", 
                                   JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    searchForText(text,true);
                }
            });

            _cancelBtn = new JButton("Close");
            _cancelBtn.setToolTipText("Closes search dialog");
            _cancelBtn.setMnemonic(KeyEvent.VK_C);
            _cancelBtn.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae)
                {
                    SearchDialog.this.hide();
                }
            });

            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(_searchBtn);
            buttonPanel.add(Box.createHorizontalStrut(20));
            buttonPanel.add(_cancelBtn);
            buttonPanel.add(Box.createHorizontalGlue());

            //-----------------------

            _mainPanel.add(textPanel);
            _mainPanel.add(buttonPanel);            

            this.setContentPane(_mainPanel);
            this.pack();
        }
        
        //-----------------------------------------------------------------

        protected void searchForText(String text,boolean cont)
        {
            int index = selectMatch(text, cont, false);
            
            if (index == -1)
            {
                JPanel msgPanel = new JPanel();
                msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.Y_AXIS));
                msgPanel.add(new JLabel("No more matches for \'" + text 
                                        + "\' could be found."));
                msgPanel.add(new JLabel("Start search from beginning?"));

                int rv = JOptionPane.showConfirmDialog(SearchDialog.this,
                                                 msgPanel, "Search Result", 
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE);
                if (rv == JOptionPane.YES_OPTION)
                    searchForText(text, false);
                 return;
            }
        }

        //-----------------------------------------------------------------
        //-----------------------------------------------------------------
        //-----------------------------------------------------------------

    }
    
    //=====================================================================
    //== Printing code from the book "Swing, Second Edition" by Robinson,
    //== and Vorobiev from Manning Publications, 2003. Chapter 22.
    //=====================================================================
    
    public void printData()
    {
        try {
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPrintable(this);
            if (!pj.printDialog())
                return;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            pj.print();
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (_notifySuccess)
                JOptionPane.showMessageDialog(SavannahLogPanel.this, 
                        "Printing completed successfully.", 
                        "Print Status", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (PrinterException pEx) {
            JOptionPane.showMessageDialog(SavannahLogPanel.this, 
                    "Unable to print log.\nReason: "+pEx.getMessage(), 
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }        
        
    }

    //---------------------------------------------------------------------
    
    public int print(Graphics pg, PageFormat pageFormat, int pageIndex) 
                                                throws PrinterException
    {
        pg.translate((int) pageFormat.getImageableX(), 
                     (int) pageFormat.getImageableY());
        int wPage  = (int) pageFormat.getImageableWidth();
        int hPage  = (int) pageFormat.getImageableHeight();
        pg.setClip(0,0,wPage,hPage);
        
        if (this._printView == null)
        {
            BasicTextUI btui = (BasicTextUI)_textPane.getUI();
            View root = btui.getRootView(_textPane);
            _printView = new PrintView(_textPane.getDocument().
                                       getDefaultRootElement(), 
                                       root, wPage, hPage); 
        }
        
        boolean bCont = _printView.printPage(pg, hPage, pageIndex);
        System.gc();
        
        if (bCont)
        {
            return PAGE_EXISTS;
        }
        else
        {
            _printView = null;
            return NO_SUCH_PAGE;
        }
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    class PrintView extends BoxView
    {
        protected int _firstOnPage = 0;
        protected int _lastOnPage  = 0;
        protected int _pageIndex   = 0;
        
        //-----------------------------------------------------------------
        
        public PrintView(Element elem, View root, int w, int h)
        {
            super(elem, Y_AXIS);
            setParent(root);
            setSize(w,h);
            layout(w,h);
        }
        
        //-----------------------------------------------------------------
        
        public boolean printPage(Graphics g, int hPage, int pageIndex)
        {
            if (pageIndex > _pageIndex)
            {
                _firstOnPage = _lastOnPage + 1;
                if (_firstOnPage >= getViewCount())
                    return false;
                _pageIndex = pageIndex;
            }
            int yMin = getOffset(Y_AXIS, _firstOnPage);
            int yMax = yMin + hPage;
            Rectangle rc = new Rectangle();
            
            for (int k = _firstOnPage; k < getViewCount(); ++k)
            {
                rc.x = getOffset(X_AXIS, k);
                rc.y = getOffset(Y_AXIS, k);
                rc.width  = getSpan(X_AXIS, k);
                rc.height = getSpan(Y_AXIS, k);
                if (rc.y + rc.height > yMax)
                    break;
                _lastOnPage = k;
                rc.y -= yMin;
                paintChild(g, rc, k);
            }
            return true;
        }        
        
        //-----------------------------------------------------------------
    }
    
    //=====================================================================
    //=====================================================================
    //=====================================================================
    
    /* *
     * Testing main.
     */
    private static void main(String[] args)
    {
        final SavannahLogModel model = new SavannahLogModel();
        LogEntry log = new LogEntry("Video-game characters denounce randomly placed swinging blades",
                                     System.currentTimeMillis(), LogEntry.LEVEL_INFO);
        model.newLogEntry(log);
        log = new LogEntry("Video-game characters denounce randomly placed swinging blades.  "+
                            "1,500 Dead in AT&T Cost-Cutting Measure\n"+
                            "Suborbital ballistic-propulsion Engineer not exactly a rocket-scientist",
                System.currentTimeMillis(), LogEntry.LEVEL_FATAL);
        model.newLogEntry(log);
        log = new LogEntry("Video-game characters denounce randomly placed swinging blades",
        System.currentTimeMillis(), LogEntry.LEVEL_WARN);
        model.newLogEntry(log);
        log = new LogEntry("Video-game characters denounce randomly placed swinging blades",
                            System.currentTimeMillis(), LogEntry.LEVEL_TRACE);
        model.newLogEntry(log);
        log = new LogEntry("Video-game characters denounce randomly placed swinging blades",
                 System.currentTimeMillis(), LogEntry.LEVEL_ERROR);
        model.newLogEntry(log);
        log = new LogEntry("Video-game characters denounce randomly placed swinging blades",
                System.currentTimeMillis(), LogEntry.LEVEL_UNKNOWN);
        model.newLogEntry(log); 
         
        JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());
        
        pane.add(new SavannahLogPanel(model,"- Test Log Window -"), 
                 BorderLayout.CENTER);
        
        JButton tstButton = new JButton("tst_add");
        tstButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent ae)
           {
               int c = (int) (System.currentTimeMillis() % 100);
               int random = c % 3;
               int level = LogEntry.LEVEL_WARN;
               if (random == 1)
                   level = LogEntry.LEVEL_INFO;
               if (random == 2)
                   level = LogEntry.LEVEL_DEBUG;
               LogEntry log = new LogEntry("Bush on Economy: We must invade Iraq! - "+c,
                       System.currentTimeMillis(), level);
               model.newLogEntry(log);
           }
        });
        pane.add(tstButton, BorderLayout.SOUTH);
        
        
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.show();
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    public void componentShown(ComponentEvent ce)
    {
        _canUpdate = true;
        updateLogList(_model.getLogEntries());
    }
        
    public void componentHidden(ComponentEvent ce)
    {
        //no longer need to worry about updating...
        _canUpdate = false;
    }
    
    public void componentMoved(ComponentEvent ce) {}
    public void componentResized(ComponentEvent ce) {}
    
    
    //---------------------------------------------------------------------
    
    //=====================================================================
}
