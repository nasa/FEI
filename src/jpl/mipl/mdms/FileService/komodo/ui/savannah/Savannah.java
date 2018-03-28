package jpl.mipl.mdms.FileService.komodo.ui.savannah;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jpl.mipl.mdms.utils.logging.Logger;

public class Savannah
{    
    protected String _title      = SavannahModel.APPLICATION_TITLE;
    protected String _iconPath   = SavannahModel.ICON_PATH;
    protected String _imagePath  = "resources/savannah.jpg";
    protected int    _splashWait = 3000;
    protected int    _initWidth  = 800;
    protected int    _initHeight = 600;
    protected Logger _logger = Logger.getLogger(Savannah.class.getName());
    
    //---------------------------------------------------------------------
    
    /**
     *  Starts the Savannah application. 
     */
    
    public static void main(String[] args)
    {                
        try {
            
            new Savannah();
            
        } catch (Exception ex) {
            Logger.getLogger(Savannah.class.getName()).error(
                                            ex.getMessage(), ex);
            String errMsg = ex.getMessage();
            if (errMsg != null)
            {
                errMsg = "Error Message: "+errMsg;
            }
            else
            {             
                if (ex instanceof NullPointerException)
                    errMsg = "Cause: Null pointer error.";
                else 
                    errMsg = "Cause: "+ex.getClass().getName();
            }
            
            System.err.println("Error: Could not initialize application. " + errMsg);
                                      
            System.exit(1);
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Sets the look and feel of the application based upon the value
     * of the system property "komodo.ui.lookandfeel".  If the value is
     * "native", then the look and feel for class name returned by
     * <code>UIManager.getSystemLookAndFeelClassName()</code> is used.
     * Otherwise, the look and feel for class name returned by
     * <code>UIManager.getCrossPlatformLookAndFeelClassName()</code> 
     * is used.
     */
    
    protected void setLookAndFeel()
    {
        String lfValue = System.getProperty("komodo.ui.lookandfeel");
        
        if (lfValue == null || !lfValue.equalsIgnoreCase("native"))
        {
            //set cross platform look and feel
            try {
                UIManager.setLookAndFeel(  
                    UIManager.getCrossPlatformLookAndFeelClassName());
            } catch(Exception ex) {
                _logger.warn("Could not set cross platform look "+
                             "and feel. Using default...");
            }
        }
        else //set native look and feel
        {
            try {
                UIManager.setLookAndFeel(  
                    UIManager.getSystemLookAndFeelClassName());
            } catch(Exception ex) {
                _logger.warn("Could not set native look and feel. "+
                             "Using default...");
            }
        }
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Constructor.  Builds user interface and displays window. 
     */
    
    public Savannah()
    {
        setLookAndFeel();
        
        JFrame frame = new JFrame(_title);
        
        //create splash screen
        new SavannahSplashWindow(_imagePath, frame, _splashWait);
        
        //set close op to do nothing, SavannahPanel will handle it
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        //setup icon
        java.net.URL iconURL = Savannah.class.getResource(_iconPath);
        if (iconURL != null) 
        {               
            ImageIcon icon = new ImageIcon(iconURL);
            if (icon != null)
            {
                frame.setIconImage(icon.getImage());                
            }                
        }
        
        SavannahPanel panel = new SavannahPanel();
        frame.getContentPane().add(panel);
        frame.setJMenuBar(panel.getMenuBar());
        frame.addWindowListener(panel.getWindowCloseListener());
        frame.pack();
        frame.setSize(_initWidth, _initHeight);
        frame.setVisible(true);                
    }

    //---------------------------------------------------------------------
    
}

//-------------------------------------------------------------------------
//-------------------------------------------------------------------------

class SavannahSplashWindow extends JWindow
{
    private static final long serialVersionUID = 1409311637823208367L;
 
    private BufferedImage _bufImage;    
    private Rectangle _rect;
    private String _imgPath;
    private Frame  _daddy;
    private int   _waitTime;
    private Image _image;
    int _xOrigin, _yOrigin;
    
    //---------------------------------------------------------------------
    
    public SavannahSplashWindow(String imgPath, Frame f, 
                                int waitTime)
    {
        super(f);
        _imgPath = imgPath;
        _daddy = f;
        _waitTime = waitTime;
        run();
    }
    
    //---------------------------------------------------------------------
        
    public void run2()
    {
        java.net.URL iconURL = Savannah.class.getResource(_imgPath);
        if (iconURL != null) 
        {
            _image = new ImageIcon(iconURL).getImage();    
            int imageWidth =  _image.getWidth(this);    
            int imageHeight = _image.getHeight(this);    
            if (imageWidth > 0 && imageHeight > 0) 
            {      
                int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;      
                int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;      
                // a Rectangle centered on screen      
                _rect = new Rectangle((screenWidth - imageWidth) / 2, 
                                     (screenHeight - imageHeight) / 2,
                                     screenWidth, screenHeight);
                _rect = new Rectangle(0,0,
                        screenWidth, screenHeight);
                _xOrigin = (screenWidth - imageWidth) / 2;
                _yOrigin = (screenHeight - imageHeight) / 2;
                
                // the critical lines, create a screen shot      
                try {        
                    _bufImage = new Robot().createScreenCapture(_rect);      
                } catch (AWTException e) {        
                    e.printStackTrace();      
                }      
                
                // obtain the graphics context from the BufferedImage      
                Graphics2D g2D = _bufImage.createGraphics();      
                // Draw the image over the screen shot      
                g2D.drawImage(_image, 0, 0, this);      
                // draw the modified BufferedImage back into the same space      
                setBounds(_rect);                     
            }
            
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e)
                {
                    setVisible(false);
                    dispose();
                }
            });
            final int pause = _waitTime;
            final Runnable closerRunner = new Runnable() {
                public void run()
                {
                    setVisible(false);
                    dispose();
                }
            };
            Runnable waitRunner = new Runnable() {
                public void run()
                {
                    try {
                            Thread.sleep(pause);
                            SwingUtilities.invokeAndWait(closerRunner);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            
            setVisible(true);
            Thread splashThread = new Thread(waitRunner, "splashWaiterThread");
            splashThread.start();
        }
    }
    
    /*
    public void paint(Graphics g) 
    {    Graphics2D g2D = (Graphics2D) g;    
        //g2D.drawImage(_bufImage, 0, 0, this);
        g2D.drawImage(_bufImage, _xOrigin, _yOrigin, this); 
    }
    */
    
    //---------------------------------------------------------------------
    
    public void run()
    {
        java.net.URL iconURL = Savannah.class.getResource(_imgPath);
        if (iconURL != null) 
        {
            ImageIcon icon = new ImageIcon(iconURL);

            JLabel l = new JLabel(icon);
            l.setOpaque(false);
            getContentPane().add(l, BorderLayout.CENTER);
            pack();
            Dimension screenSize =
              Toolkit.getDefaultToolkit().getScreenSize();
            Dimension labelSize = l.getPreferredSize();
            setLocation(screenSize.width/2 - (labelSize.width/2),
                        screenSize.height/2 - (labelSize.height/2));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e)
                {
                    setVisible(false);
                    dispose();
                }
            });
            final int pause = _waitTime;
            final Runnable closerRunner = new Runnable() {
                public void run()
                {
                    setVisible(false);
                    dispose();
                }
            };
            Runnable waitRunner = new Runnable() {
                public void run()
                {
                    try {
                            Thread.sleep(pause);
                            SwingUtilities.invokeAndWait(closerRunner);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            setVisible(true);

            Thread splashThread = new Thread(waitRunner, "Savannah Loading..");
            splashThread.start();
        }
    }
    
    //---------------------------------------------------------------------
    
}



