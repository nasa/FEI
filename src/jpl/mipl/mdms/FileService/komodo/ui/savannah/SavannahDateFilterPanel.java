package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.DateSelectionPanel.DateChangeListener;

public class SavannahDateFilterPanel extends JPanel
{

    private static final long serialVersionUID = -456833960508916631L;

    protected SavannahDateFilterModel model;
    protected boolean includeDescription;
    
    protected JRadioButton noneButton;
    protected JRadioButton presetButton;
    protected JRadioButton dateButton;
    protected ButtonGroup  buttonGroup;
    
//    protected JComboBox presetOffsetBox;
    
    protected SpinnerModel offsetSpinModel;
    protected JSpinner offsetSpinner;
    protected JComboBox dataConditionalBox;
    protected JLabel andLabel;
    
    protected DateSelectionPanel dateFieldA;
    protected DateSelectionPanel dateFieldB;
    
    protected JPanel descriptionPanel = null;
    
    protected String dateFormatStr = "yyyy-MM-dd";
    protected DateFormat dateFormat;
    
    //public static final String MODE_BEFORE  = "Before";
    public static final String MODE_AFTER   = "After";
    public static final String MODE_BETWEEN = "Between";
    
    public static final int DEFAULT_DAY = 7;
    public static final int MIN_DAY     = 1;
    public static final int MAX_DAY     = 90;
    public static final int STEP_DAY    = 1;
    
    
    
    public SavannahDateFilterPanel(SavannahDateFilterModel model)
    {
        this(model, true);
    }

    public SavannahDateFilterPanel(SavannahDateFilterModel model,
                                   boolean includeDescriptionPanel)
    {
        this.model = model;
        this.includeDescription = includeDescriptionPanel;
        
        init();
    }
    
    protected void init()
    {
        this.dateFormat = new SimpleDateFormat(dateFormatStr);
        
        buildUI();
        
        this.model.addFilterListener(new ModelListener());
    }
    
    protected void buildUI()
    {        
        initComponents();
        initValues();
        performLayout();      
        attachListeners();
    }
    
    protected void initComponents()
    {
        noneButton   = new JRadioButton("None");
        presetButton = new JRadioButton("Offset");
        dateButton   = new JRadioButton("Date");
        buttonGroup  = new ButtonGroup();
        buttonGroup.add(this.noneButton);
        buttonGroup.add(this.presetButton);
        buttonGroup.add(this.dateButton);
        
        offsetSpinModel = new SpinnerNumberModel(DEFAULT_DAY, MIN_DAY, MAX_DAY ,STEP_DAY); 
        offsetSpinner = new JSpinner(offsetSpinModel);
        
//        presetOffsetBox = new JComboBox();
        dataConditionalBox = new JComboBox();
        dataConditionalBox.addItem(MODE_AFTER);
        //dataConditionalBox.addItem(MODE_BEFORE);
        dataConditionalBox.addItem(MODE_BETWEEN);
        
        
        andLabel = new JLabel("and");
        
        dateFieldA = new DateSelectionPanel();
        dateFieldB = new DateSelectionPanel();
        
        if (this.includeDescription)
        {
            final String description = getDescriptionText();
            JLabel descriptionArea = new JLabel(description);
            
//            JTextArea textArea = new JTextArea(description);
//            textArea.setLineWrap(true);
//            textArea.setEditable(false);
//            textArea.setEnabled(false);
            descriptionPanel = new JPanel();
            descriptionPanel.setLayout(new BoxLayout(descriptionPanel, 
                                                    BoxLayout.X_AXIS));
//            descriptionPanel.add(textArea, BorderLayout.CENTER);
            descriptionPanel.add(Box.createHorizontalStrut(17));
            descriptionPanel.add(descriptionArea);
            descriptionPanel.add(Box.createHorizontalStrut(17));
            
        }
        
    }
    
    protected String getDescriptionText()
    {
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("<HTML>Select a date filter mode (None, Offset, Date).<BR>").
               append("<BR>").
               append("None: No date filtering will be applied<BR>").
               append("Offset: Filter to include most recent number of days<BR>").
               append("Date: Filter includes files either after a date or between two dates<BR>").
               append("</HTML>");
        		
        
        return buffer.toString();        
    }
    
    protected void initValues()
    {
        DateFilter filter = this.model.getActive();
        
        reactToFilter(filter);
        
    }
    
    protected void performLayout()
    {
        JPanel buttonPanel  = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1));
        buttonPanel.add(noneButton); //, 0, 0);
        buttonPanel.add(presetButton); //, 0, 1);
        buttonPanel.add(dateButton);//, 0, 2);
//        buttonPanel.add(andLabel);
        
        
        JPanel controlPanel = new JPanel();   
        controlPanel.setLayout(new GridLayout(4,1));
        
        JPanel controlPanelB = new JPanel();   
        controlPanelB.setLayout(new GridLayout(4,1));
        
        controlPanel.add(new JLabel(""));//,      0, 1); //1, 0);
        controlPanelB.add(new JLabel(""));//,      0, 1); //1, 0);
        controlPanel.add(offsetSpinner );//,      0, 1); //1, 0);
        controlPanelB.add(new JLabel("day(s)"));//,      0, 1); //1, 0);
        controlPanel.add(dataConditionalBox); //, 0, 2); //2, 0);
        controlPanelB.add(dateFieldA); //,         1, 2); //2, 1);
        controlPanel.add(andLabel );// ,           0, 3); //3, 0);
        
        controlPanelB.add(dateFieldB);//,         1, 3); //3, 1);
        
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));        
        mainPanel.add(Box.createHorizontalStrut(10));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createHorizontalStrut(5));
        mainPanel.add(controlPanel);
        mainPanel.add(Box.createHorizontalStrut(5));
        mainPanel.add(controlPanelB);
        mainPanel.add(Box.createHorizontalStrut(10));
        
        
        
        
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        if (descriptionPanel != null)
        {
            this.add(Box.createVerticalStrut(10));
            this.add(descriptionPanel);           
        }
        this.add(Box.createVerticalStrut(10));       
        this.add(mainPanel);
        this.add(Box.createVerticalStrut(10));
        
       
    }
    
    protected void attachListeners()
    {
        SpinnerListener spinListener = new SpinnerListener();
        this.offsetSpinner.addChangeListener(spinListener);
        
        ModeListener modeListener = new ModeListener();
        this.noneButton.addActionListener(modeListener);
        this.dateButton.addActionListener(modeListener);
        this.presetButton.addActionListener(modeListener);
        
        DatePanelListener dateListener = new DatePanelListener();
        this.dateFieldA.addDateChangeListener(dateListener);
        this.dateFieldB.addDateChangeListener(dateListener);
        
        ComboHandler comboListener = new ComboHandler();
        this.dataConditionalBox.addActionListener(comboListener);
        
        
    }
    
    protected void reactToFilter(DateFilter filter)
    {
        
        final int    mode     = filter.getMode();
        
        long dateMs    = filter.getDate();
        long endDateMs = filter.getEndDate();
        
        
        final Date   dateA    = dateMs == DateFilter.NO_VALUE ?
                                    null : new Date(dateMs);
        final Date   dateB    = endDateMs == DateFilter.NO_VALUE ?
                                    null : new Date(endDateMs);
        
        final long   offsetMs = filter.getOffset();        
        final int days = SavannahDateFilterModel.getDaysForOffset(offsetMs);
        
        this.andLabel.setEnabled(false);
        this.dateFieldB.setEnabled(false);
        
        if (mode == DateFilter.MODE_OFF)
        {
            this.noneButton.setSelected(true);
        }
        else if (mode == DateFilter.MODE_OFFSET)
        {
            this.presetButton.setSelected(true);
            this.offsetSpinModel.setValue(days);            
            
        }
        else if (mode == DateFilter.MODE_AFTER  ||
                 mode == DateFilter.MODE_BEFORE ||
                 mode == DateFilter.MODE_BETWEEN)
        {

            
            this.dateButton.setSelected(true);
            if (mode == DateFilter.MODE_BETWEEN)
            {
                this.dataConditionalBox.setSelectedItem(MODE_BETWEEN);

            }           
            else if (mode == DateFilter.MODE_AFTER)
            {
                this.dataConditionalBox.setSelectedItem(MODE_AFTER);
//                this.andLabel.setEnabled(false);
            }
//            else if (mode == DateFilter.MODE_BEFORE)
//            {
//                this.dataConditionalBox.setSelectedItem(MODE_BEFORE);
////                this.andLabel.setEnabled(false);
//            }
  
        }
        
        if (dateA != null)
        {
//            String dateAStr = dateFormat.format(dateA);
//            this.dateFieldA.setText(dateAStr);
            this.dateFieldA.setDate(dateA);
        }
        if (dateB != null)
        {
//            String dateBStr = dateFormat.format(dateB);
//            this.dateFieldB.setText(dateBStr);
            this.dateFieldB.setDate(dateB);
        }    
        
        if (MODE_BETWEEN.equals(dataConditionalBox.getSelectedItem()))
        {
            this.andLabel.setEnabled(true);     
            this.dateFieldB.setEnabled(true);
        }
    }
    
    

    
    protected DateFilter getDateFilter()
    {
        DateFilter filter= null;

        Date dateA =  this.dateFieldA.getDate();
        Date dateB =  this.dateFieldB.getDate();
        
        if (presetButton.isSelected())
        {
            int days = ((Integer) this.offsetSpinModel.getValue()).intValue();
            long offset = SavannahDateFilterModel.getOffsetForDays(days);
            filter = new DateFilter(offset);
        }
        else if (dateButton.isSelected())
        {
            Object cond = dataConditionalBox.getSelectedItem();
            if (MODE_AFTER.equals(cond))
            {               
                dateA = model.roundDate(dateA, false);
                filter = new DateFilter(DateFilter.MODE_AFTER, 
                                        dateA.getTime());
            }
//            else if (MODE_BEFORE.equals(cond))
//            {
//                dateA = model.roundDate(dateA, false);
//                filter = new DateFilter(DateFilter.MODE_BEFORE, 
//                                        dateA.getTime());
//            }
            else if (MODE_BETWEEN.equals(cond))
            {
               
                
                dateA = model.roundDate(dateA, false);
                dateB = model.roundDate(dateB, true);                
                filter = new DateFilter(dateA.getTime(),
                                        dateB.getTime());   
            }
        }
        else
        {
            filter = new DateFilter();
        }
        
        
        filter.setDateBounds(dateA.getTime(),
                             dateB.getTime());
        
        return filter;
    }
    
    protected void dateFilterChanged()
    {
        DateFilter newFilter = this.getDateFilter();
        this.reactToFilter(newFilter);
        this.model.setActive(newFilter);
    }
    
    
    
    class SpinnerListener implements ChangeListener
    {
        public void stateChanged(ChangeEvent ce) 
        {
            dateFilterChanged();
        }
    }
    

    
    class DatePanelListener implements DateChangeListener
    {
        public void dateChanged(Date date)
        {
            dateFilterChanged();
        }
    }
    
   
    
    class ModeListener implements ActionListener
    {
        public void actionPerformed(ActionEvent ae)
        {
            dateFilterChanged();            
        }        
    }
    
    class ModelListener implements SavannahDateFilterListener
    {
        public void filterChanged(DateFilter filter)
        {
            reactToFilter(filter);
        }        
    }
    
    class ComboHandler implements ActionListener
    {
        boolean enabled = true;
        
        public void setEnabled(boolean flag)
        {
            this.enabled = flag;
        }
        public boolean isEnabled()
        {
            return this.enabled;
        }
        public void actionPerformed(ActionEvent ae) 
        {
                    
            if (this.enabled)
                dateFilterChanged();
                            
                    
        }
    }
    
    public static void main(String[] args)
    {
        SavannahDateFilterModel model = new SavannahDateFilterModel();
        SavannahDateFilterPanel panel = new SavannahDateFilterPanel(model);
        
        JFrame frame = new JFrame("test date filter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        
        frame.pack();
        frame.setVisible(true);
        
        
        
    } 
}
