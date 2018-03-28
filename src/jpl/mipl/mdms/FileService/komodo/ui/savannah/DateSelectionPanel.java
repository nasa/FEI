package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * <B>Purpose:<B>
 * There are a few open source Swing based Calendar panels one could
 * use, but some have licensing issues.  So this is just a basic
 * implementation with a note that it could be replaced by incorporating
 * another date picker library.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DateSelectionPanel.java,v 1.1 2013/04/04 20:48:50 ntt Exp $
 *
 */
public class DateSelectionPanel extends JPanel
{

    private static final long serialVersionUID = -5714837225251309814L;

    protected Date currentDate;
    
    protected JLabel yearLabel, monthLabel, dayLabel;
    
    protected JComboBox    yearBox;
    protected JComboBox    monthBox;
    protected JComboBox    dayBox;
    protected ComboHandler handler;
    
    List<String> yearList;
    List<String> monthList;
    List<String> dayList;
    
    public static final int MIN_YEAR = 1970;
    public static int MAX_YEAR;
    
    List<DateChangeListener> dcListeners = new Vector<DateChangeListener>();
    
    public DateSelectionPanel()
    {
        this(new Date());
    }
    
    public DateSelectionPanel(Date date)
    {
        this.currentDate = date;
        
        init();
    }
    
    protected void init()
    {
        
        //get latest year (todays year)
        Calendar cal = Calendar.getInstance();
        int nowYear  = cal.get(Calendar.YEAR);
        nowYear++;  //lets include next year (handle's new years eve case)
        MAX_YEAR = nowYear;
        
        this.yearList  = new ArrayList<String>();
        this.monthList = new ArrayList<String>();
        this.dayList   = new ArrayList<String>();
        
        for (int i = MAX_YEAR; i >= MIN_YEAR; --i)
            this.yearList.add(i+"");
        
        for (int i = 1 ; i <= 31; ++i)
            this.dayList.add(i+"");
        
        this.monthList.add("January");
        this.monthList.add("February");
        this.monthList.add("March");
        this.monthList.add("April");
        this.monthList.add("May");
        this.monthList.add("June");
        this.monthList.add("July");
        this.monthList.add("August");
        this.monthList.add("September");
        this.monthList.add("October");
        this.monthList.add("November");
        this.monthList.add("December");
        
        
        buildUi();
        
        updateFields();
        
        this.addDateChangeListener(new InternalListener());
    }
   
    public void addDateChangeListener(DateChangeListener l)
    {
        synchronized (this.dcListeners)
        {
            if (!this.dcListeners.contains(l))
                dcListeners.add(l);
        }
        
    }
    public void removeDateChangeListener(DateChangeListener l)
    {
        synchronized (this.dcListeners)
        {
            if (this.dcListeners.contains(l))
                dcListeners.remove(l);
        }
    }
    
    protected void fireDateChange()
    {
        synchronized (this.dcListeners)
        {
            Iterator<DateChangeListener> it = dcListeners.iterator();
            while (it.hasNext())
            {
                it.next().dateChanged(this.currentDate);
            }
        }
    }
    
    protected void buildUi()
    {
        this.yearBox  = new JComboBox();
        this.monthBox = new JComboBox();
        this.dayBox   = new JComboBox();
        
        this.yearBox.setPrototypeDisplayValue("XXXXXX");
        this.monthBox.setPrototypeDisplayValue("XXXXXXXXXXX");
        this.dayBox.setPrototypeDisplayValue("XXXX");
        
        for (String year : yearList)
            this.yearBox.addItem(year);
        for (String month : monthList)
            this.monthBox.addItem(month);
        for (String day : dayList)
            this.dayBox.addItem(day);
        
        this.yearLabel  = new JLabel("Year:");
        this.monthLabel = new JLabel("Month:");
        this.dayLabel   = new JLabel("Day:");
        
        performLayout();
        
        attachListeners();
    }
    
    protected void performLayout()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        this.add(Box.createHorizontalStrut(5));
        this.add(this.monthBox);
        this.add(Box.createHorizontalStrut(5));
        this.add(this.dayBox);
        this.add(Box.createHorizontalStrut(5));
        this.add(this.yearBox);
        this.add(Box.createHorizontalStrut(5));
                
    }
    
    protected void attachListeners()
    {
        handler = new ComboHandler();
        this.dayBox.addActionListener(handler);
        this.monthBox.addActionListener(handler);
        this.yearBox.addActionListener(handler);
    }

    protected void updateDate()
    {
        int monthIdx = monthBox.getSelectedIndex();
        int dayIdx   = dayBox.getSelectedIndex();
        int yearIdx  = yearBox.getSelectedIndex();
        
        int monthVal = monthIdx; 
        int dayVal   = dayIdx + 1;
        int yearVal  = MAX_YEAR - yearIdx;
        
        updateDate(monthVal, dayVal, yearVal);
    }
    
    protected void updateDate(int month, int day, int year)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH,        month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.YEAR,         year);
        cal.set(Calendar.HOUR_OF_DAY,  0);
        cal.set(Calendar.MINUTE,       0);
        cal.set(Calendar.SECOND,       0);
        cal.set(Calendar.MILLISECOND,  0);
        
        Date newDate = cal.getTime();
        
        updateDate(newDate);
    }
    
    protected void updateDate(Date newDate)
    {
        if (!this.currentDate.equals(newDate))
        {
            this.currentDate = newDate;
            
            fireDateChange();

        }
    }
     
    public Date getDate()
    {
        return this.currentDate;
        
    }
    
    public void setDate(Date date)
    {
        updateDate(date);
    }
    
    protected void updateFields()
    {
        if (this.handler != null)
            this.handler.setEnabled(false);
        
        //get current state
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        int theYear = cal.get(Calendar.YEAR);
        int theMonth = cal.get(Calendar.MONTH);
        int theDay   = cal.get(Calendar.DAY_OF_MONTH);
        
        yearBox.setSelectedItem(theYear+"");
        monthBox.setSelectedIndex(theMonth);
        dayBox.setSelectedIndex(theDay-1);
        
        if (this.handler != null)
            this.handler.setEnabled(true);
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
                updateDate();
        }
    }
    
    
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Test");
        
        DateSelectionPanel panel = new DateSelectionPanel();
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        
        frame.pack();
        frame.setVisible(true);
    }
    
    interface DateChangeListener
    {
        public void dateChanged(Date date); 
    }
    class InternalListener implements DateChangeListener
    {
        public void dateChanged(Date date)
        {
            updateFields();
        }
    }
    
    
    public void setEnabled(boolean flag)
    {
        super.setEnabled(flag);
        this.dayBox.setEnabled(flag);
        this.yearBox.setEnabled(flag);
        this.monthBox.setEnabled(flag);
        
    }
}
