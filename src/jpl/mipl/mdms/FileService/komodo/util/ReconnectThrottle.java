package jpl.mipl.mdms.FileService.komodo.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <B>Purpose:<B>
 * Throttle mechanism designed to control the number of reconnection
 * attempts made by the client.  Without this, the client could
 * repeatedly and indefinitely attempt to reconnect with the server
 * 
 * When a reconnect requests first comes in, a probation period is
 * set.  If another reconnect attempt occurs during the probation period, 
 * the wait time for actual reconnection attempt increases exponentially 
 * until a maximum is reached.  The probation period is updated based
 * on the last wait time.  After the probation period is over,
 * the wait time is reset to the minimal value.  
 * 
 * A throttle window can be overridden using the 
 * <code>Constants.PROPERTY_THROTTLE_WINDOW</code> system property, where
 * units are seconds.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ReconnectThrottle.java,v 1.1 2016/03/30 21:10:45 ntt Exp $
 *
 */
public class ReconnectThrottle
{
    public static final long NULL_TIME = -1L;
    
    protected long timeProbationEnds;
    protected long timeForWakeup;
    protected int  currentFactorScale;
    protected long probationWindowMillis;
    protected boolean isActive;
    
    protected static final int MILLIS_PER_SECOND     = 1000;
    
    /** Initial wait time */
    protected static final int INITIAL_WAIT_SECONDS  = 60;
    
    /** Factor of wait time increase */
    protected static final int INCREASE_FACTOR       = 2;
    
    /** Maximum wait time */
    protected static final int MAX_WAIT_SECONDS      = 60 * 16;  //16 minutes
    
    /** Initial scale factor */
    protected static final int INITIAL_FACTOR_SCALE  = -1;  

    
    /** Default number of seconds to for probation period */ 
    public static final int DEFAULT_RESET_WINDOW_SEC  = 600;   //10 minutes
    
    //date formatting
    protected static final String dateFormatStr = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    protected static DateFormat dateFormat = null;
    
    private Logger _logger = Logger.getLogger(ReconnectThrottle.class.getName());
    
    protected List<Logger> externalLoggers;
    
    //---------------------------------------------------------------------
    
    /** 
     * Constructor
     */
    public ReconnectThrottle()
    {
        init();
    }
    
    //---------------------------------------------------------------------
    
    /*
     * Initializes this instance
     */
    protected void init()
    {
        this.timeProbationEnds         = NULL_TIME;
        this.timeForWakeup             = NULL_TIME;
        this.currentFactorScale        = INITIAL_FACTOR_SCALE;
        this.probationWindowMillis     = DEFAULT_RESET_WINDOW_SEC * MILLIS_PER_SECOND;
        this.isActive                  = true;
        
        this.externalLoggers           = new Vector<Logger>();
        
        checkOverrides();
    }
    //---------------------------------------------------------------------
    
    /**
     * Check for system override values
     */
    protected void checkOverrides()
    {
        String propValue = null;
        
        propValue = System.getProperty(Constants.PROPERTY_THROTTLE_WINDOW);
        if (propValue != null)
        {
            int throttleWindowSec = -1;
            
            try { 
                throttleWindowSec = Integer.parseInt(propValue);
            } catch (NumberFormatException nfEx) {
                throttleWindowSec = -1;
            }
            
            if (throttleWindowSec >= 0)
            {
                probationWindowMillis = throttleWindowSec * MILLIS_PER_SECOND;
                _logger.debug("Reconnect throttle window is set to " + 
                              throttleWindowSec+" seconds");
            }
        }
        
    }

    //---------------------------------------------------------------------

    public synchronized void addLogger(Logger externalLogger)
    {
        if (externalLogger == null)
            return;
        
        if (!this.externalLoggers.contains(externalLogger))
        {
            this.externalLoggers.add(externalLogger);
        }        
    }
    
    //---------------------------------------------------------------------
    
    public synchronized void removeLogger(Logger externalLogger)
    {
        if (externalLogger == null)
            return;
        
        if (this.externalLoggers.contains(externalLogger))
        {
            this.externalLoggers.remove(externalLogger);
        }
    }
    
    //---------------------------------------------------------------------
    
    
    //---------------------------------------------------------------------
    /**
     * Poll this throttle instance, and return only when a reconnection
     * is allowed.  The call will block until the throttle determines
     * it is appropriate to continue.  Calling reset() will not.
     * @return The number of milliseconds throttled iff  
     */
    public synchronized void pollWait() 
    {
        final long now = System.currentTimeMillis();
        
        long pollingPeriod = NULL_TIME;
        
        _logger.trace("Reconnection throttle being polled");
        
        final boolean inProbation = isInProbationPeriod(now);
        
        //If the current time is before the probation period ends,
        //then advance our time fields.  Otherwise, we can reset them.
        
        if (!inProbation)
        {
            resetFields();
        }
        
        advanceFields(); 
        
        
        //if non-null, then we have some potential waiting to do...
        if (timeForWakeup != NULL_TIME)
        {
            pollingPeriod =  sleepTil(timeForWakeup);           
        }
             
    }
    
    //---------------------------------------------------------------------
    
    protected boolean isInProbationPeriod(long timestamp)
    {
        return timeProbationEnds != NULL_TIME && timestamp < timeProbationEnds;
    }

    //---------------------------------------------------------------------
    
    public void reset()
    {
        resetFields();
  
    }
    
    //---------------------------------------------------------------------
    
    protected void resetFields()
    {
        this.timeProbationEnds  = NULL_TIME;
        this.timeForWakeup      = NULL_TIME;
        this.currentFactorScale = INITIAL_FACTOR_SCALE;
    }

    //---------------------------------------------------------------------
    
    protected void advanceFields()
    {

        this.currentFactorScale = this.currentFactorScale + 1;
        
        double factor = currentFactorScale < 1 ? 0 : 
                        Math.pow(INCREASE_FACTOR, 
                                 currentFactorScale-1);
        
        long waitTimeSec = (long) factor * INITIAL_WAIT_SECONDS;
        if (waitTimeSec > MAX_WAIT_SECONDS)
        {
            waitTimeSec = MAX_WAIT_SECONDS;
        }
        
        long waitTimeMillis = waitTimeSec * MILLIS_PER_SECOND;
        
        this.timeForWakeup      = System.currentTimeMillis() + waitTimeMillis;
        this.timeProbationEnds  = timeForWakeup + this.probationWindowMillis;
        
        
        //System.out.println("CurrentFactorScale is "+currentFactorScale);
        //System.out.println("Throttle probation ends at "+millisToTimestamp(timeProbationEnds));
        //System.out.println("Wakeup time set to "+millisToTimestamp(timeForWakeup));
        String dbgMsg = "FEI5 Information on " +
                        DateTimeUtil.getCurrentDateCCSDSAString() + "\n" +
                        "Reconnect throttle scaleFactor = "+currentFactorScale+"; "+
                        "Wait time is "+waitTimeSec+ " seconds; "+
                        "Probation ends "+millisToTimestamp(timeProbationEnds);
        _logger.debug(dbgMsg);        
    }
    
    //---------------------------------------------------------------------
    
    protected String millisToTimestamp(long millis)
    {
        if (dateFormat == null)
        {
            dateFormat = new SimpleDateFormat(dateFormatStr);
        }
        Date date = new Date(millis);
        return dateFormat.format(date);
    }
    
    //---------------------------------------------------------------------
    
    protected long sleepTil(long timeToWake)
    {
        long start = System.currentTimeMillis();
        
        while (timeToWake > System.currentTimeMillis())
        {
            //how many millis should we sleep for
            final long sleepTime = timeToWake - System.currentTimeMillis();
            
            try {          

                String infoMsg = "FEI5 Information on " +
                                 DateTimeUtil.getCurrentDateCCSDSAString() + "\n" +
                                 "Next reconnection attempt throttled for  " +
                                 (sleepTime/1000) + " seconds.";
                
                broadcastInfo(infoMsg);
                          
                Thread.sleep(sleepTime);          
            } catch (InterruptedException e) {
                break;                
            }
        }
        
        long end = System.currentTimeMillis();
        
        long timeDiff = end - start;
        
        return timeDiff;
    }
    

    //---------------------------------------------------------------------
    
    protected synchronized void broadcastInfo(final String mesg)
    {
        Iterator<Logger> it = this.externalLoggers.iterator();
        while (it.hasNext())
        {
            it.next().info(mesg);
        }
        
        _logger.info(mesg);
    }
    
    //---------------------------------------------------------------------
    
    protected synchronized void broadcastWarn(final String mesg)
    {
        Iterator<Logger> it = this.externalLoggers.iterator();
        while (it.hasNext())
        {
            final Logger logger = it.next();           
            logger.warn(mesg);               
        }
        
        _logger.warn(mesg);
    }
    
    //---------------------------------------------------------------------
    
    protected synchronized void broadcastError(final String mesg, 
                                               final Throwable t)
    {
        Iterator<Logger> it = this.externalLoggers.iterator();
        while (it.hasNext())
        {
            final Logger logger = it.next();           
            logger.error(mesg, t);               
        }
        
        _logger.error(mesg, t);
    }
    
    //---------------------------------------------------------------------
    
//    protected synchronized void broadcastDebug(final String mesg)
//    {
//        Iterator<Logger> it = this.externalLoggers.iterator();
//        while (it.hasNext())
//        {
//            final Logger logger = it.next();           
//            logger.debug(mesg);               
//        }
//    }
    
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        
        ReconnectThrottle throttle = new ReconnectThrottle();
        Random random = new Random(System.currentTimeMillis());
        int randomMax = 10;
        int randomCuttoff = 8;
        
        
        while (true)
        {
            int randInt = random.nextInt(randomMax);
            boolean specialCase = (randInt > randomCuttoff);
            
            long sleepTime = 0;
            if (specialCase)
            {
                sleepTime = 60000 * (randInt*2);
            }
            else
            {
                sleepTime = 60000 * randInt;
            }
            
            
            try {           
                System.out.println("a Main thread sleeping "+ (sleepTime / 1000) + " seconds...");
                Thread.sleep(sleepTime);
                System.out.println("a Main thread is awake!");
            } catch (InterruptedException e) {
                break;                
            }
            
            System.out.println("b Calling thottle at "+System.currentTimeMillis());
            throttle.pollWait();
            System.out.println("b Returned from thottle at "+System.currentTimeMillis());
        }
    }
 
}
