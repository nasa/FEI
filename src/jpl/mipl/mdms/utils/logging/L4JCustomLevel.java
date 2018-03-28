/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.utils.logging;

import org.apache.log4j.Level;

/**
 * This is a custom logging level class derived from the log4j Level class to
 * introduce applcation trace level
 * 
 * @see http://logging.apache.org/log4j
 * 
 * @author T. Huang {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: L4JCustomLevel.java,v 1.5 2007/02/12 19:21:00 ntt Exp $
 */
public class L4JCustomLevel extends Level {

   static public final int BENCH_INT = Level.DEBUG_INT + 100;

   public static final String BENCH_STR = "BENCH";

   public static final L4JCustomLevel BENCH = new L4JCustomLevel(BENCH_INT,
                                                             BENCH_STR, 7);

   protected L4JCustomLevel(int level, String strLevel, int syslogEquiv) 
   {
      super(level, strLevel, syslogEquiv);
   }

   /**
    * Convert the string passed as argument to a level. If the conversion fails,
    * then this method returns {@link #TRACE}.
    */
     public static Level toLevel(final String sArg) 
     {
         return toLevel(sArg, TRACE);
     }

   public static Level toLevel(final String sArg, final Level defaultValue) 
   {

      if (sArg == null) 
      {
         return defaultValue;
      }
      String stringVal = sArg.toUpperCase();
 

      if (stringVal.equals(BENCH_STR)) 
      {
         return L4JCustomLevel.BENCH;
      }
      return Level.toLevel(sArg, defaultValue);
   }

   public static Level toLevel(int i) throws IllegalArgumentException {
      return toLevel(i, TRACE);
   }

   /**
    * Convert an integer passed as argument to a level. If the conversion 
    * fails, then this method returns the specified default.
    * 
    * @return the Level object for i if one exists, defaultLevel otherwize.
    */
   public static Level toLevel(final int i, final Level defaultLevel) {
      Level p;

      if (i == BENCH_INT)
         p = BENCH;
      else
         p = Level.toLevel(i);
      return p;
   }
}