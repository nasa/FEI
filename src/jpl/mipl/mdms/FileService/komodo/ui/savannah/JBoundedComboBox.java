package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/**
 * <b>Purpose:</b>
 *  Extension of JComboBox with bounded size.  Added items are always added
 *  to the beginning of the underlying model list, and items from end are 
 *  discarded until model size is less than or equal to internal bound.
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
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 08/23/2004        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: 
 */

public class JBoundedComboBox extends JComboBox
{
    private String __classname = "JBoundedComboBox";
    
    public static final int DEFAULT_BOUND = 20;
    
    protected int _bound;
    
    //---------------------------------------------------------------------
    
    /**
     * Default constructor.  Uses DEFAULT_BOUND for initial bound size.
     */
    
    public JBoundedComboBox()
    {
        this(DEFAULT_BOUND);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  
     * @param bound The initial bound size.
     * @throws IllegalArgumentException if newBound is non-positive
     */
    
    public JBoundedComboBox(int bound) throws IllegalArgumentException
    {
        super();
        setBound(bound);
        init();
    }
    
    //---------------------------------------------------------------------
    
    protected void init()
    {
        this.setEditable(true);
        //this.setRenderer(new BoundedListCellRenderer());
        this.setUI(new ResizedComboBoxUI());
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Override of JComboBox.setSelectedItem().  Checks to see if
     * item is currently selected.  If not, item is add to list.
     * Finally, item is selected.
     * @param item Item to be selected and possibly added.
     */
    
    public void setSelectedItem(Object item)
    {
        if (getItemCount() == 0 || !getItemAt(0).equals(item))
        {
            addItem(item);
        }
        super.setSelectedItem(item);
        super.setToolTipText(item.toString());
        this.invalidate();
        this.repaint();
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Override of JComboBox.addItem().  Always places item at front
     * of list
     * @param item Item to be added.
     */
    
    public void addItem(Object item)
    {
        insertItemAt(item, 0);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Override of JComboBox.insertItemAt().  Checks list size after
     * and removes extra entries that go beyond the bound.
     * @param item Item to be added.
     * @param index Position to place the item
     */
    
    public void insertItemAt(Object item, int index)
    {
        super.insertItemAt(item, index);
        while (getItemCount() > this._bound)
        {
            removeItemAt(getItemCount()-1);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the bound value.
     * @param newBound A positive integer
     * @throws IllegalArgumentException if newBound is non-positive
     */
    
    public void setBound(int newBound) throws IllegalArgumentException
    {
        if (newBound < 1)
        {
            throw new IllegalArgumentException(__classname+
                    "::setBound():: Bound must be a positive integer."+
                    "Received: "+newBound);
        }
            
        if (newBound != this._bound)
        {
            
            this._bound = newBound;
            while (getItemCount() > this._bound)
            {
                removeItemAt(getItemCount()-1);
            } 
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns value of bound
     * @returns Bound value.
     */
    
    public int getBound()
    {
        return this._bound;
    }
    
    //---------------------------------------------------------------------
    //---------------------------------------------------------------------
    
    /*
     * Extension of BasicComboBoxUI to handle cases where entries
     * in JComboBox popup list have length greater than that of
     * the box's preferred length.
     * 
     * Based on code provided by members of Sun Java Forum:
     * LIRA, keeskiup
     * Dev note: This was a pain in the a**.
     */
    public class ResizedComboBoxUI extends BasicComboBoxUI 
    {
        protected ComboPopup createPopup()
        {
            BasicComboPopup popup = new BasicComboPopup(super.comboBox)
            {
                public void show()
                {
                    super.list.clearSelection();
                    if (super.list.getModel().getSize() > 0)
                    {
                        super.list.setSelectedIndex(0);
                        super.list.ensureIndexIsVisible(0);
                        /*
                        super.scroller.getViewport().setViewPosition(
                                                            new Point(0,0));
                        super.scroller.getVerticalScrollBar().setValue(0);
                        */
                    }
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run()
                        {
                            list.invalidate();
                            list.validate();
                            list.repaint();
                            scroller.invalidate();
                            scroller.repaint();
                            scroller.getVerticalScrollBar().invalidate();
                            scroller.getVerticalScrollBar().repaint();
                        }
                    });
                    
                    Dimension popSize = super.comboBox.getSize();
                    Dimension listSize = list.getPreferredSize();
    
                    int newWidth = listSize.width;
                    newWidth += 20; // for the size of the scrollbar !
                    if (popSize.width > newWidth)
                    {
                        newWidth = popSize.width;
                    }
    
                    popSize.setSize(newWidth,
                            getPopupHeightForRowCount(super.comboBox.
                                                      getMaximumRowCount()));
    
                    Rectangle popupBounds = this.computePopupBounds(0,
                                                comboBox.getBounds().height, 
                                                popSize.width, 
                                                popSize.height);
    
                    setLightWeightPopupEnabled(comboBox.
                                               isLightWeightPopupEnabled());
    
                    super.scroller.setPreferredSize(popupBounds.getSize());
    
                    show(super.comboBox, popupBounds.x, popupBounds.y);
                }
            };
    
            popup.getAccessibleContext().setAccessibleParent(super.comboBox);
            return popup;
        }
    }
}
