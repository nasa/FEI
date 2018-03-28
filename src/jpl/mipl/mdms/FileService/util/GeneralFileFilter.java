package jpl.mipl.mdms.FileService.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * <b>Purpose: </b> File filter for the local file system. Client supplies
 * a filesystem wildcard expression, which is then used to filter
 * files.  Allows special treatment for directories using a directory flag.
 * 
 * <PRE>
 * Copyright 2004, California Institute of Technology. 
 * ALL RIGHTS RESERVED. 
 * U.S. Government Sponsorship acknowledge. 2004.
 * </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History : </B> ----------------------
 * 
 * <B>Date          Who               What </B>
 * ----------------------------------------------------------------------------
 * 09/04/2004    Nick              Initial Release
 * ============================================================================
 * </PRE>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: GeneralFileFilter.java,v 1.3 2004/11/18 20:24:33 ntt Exp $
 *  
 */

public class GeneralFileFilter implements FileFilter, FilenameFilter {
   private final String __classname = GeneralFileFilter.class.getName();
    
   /** Flag to treat all files the same, even if directory */
   public final static int DIRECTORY_AS_FILE       = 0;
   
   /** Flag to always accept file if it is a directory */
   public final static int DIRECTORY_ALWAYS_ACCEPT = 1;
   
   /** Flag to always reject file if it is a directory */
   public final static int DIRECTORY_ALWAYS_REJECT = 2;
   

   /** reference to the wildcard expression */
   protected String  _expression;
   
   /** reference to generated regular expression */
   private   String  _regExpression;
   
   /** Pattern which takes regular expression */
   protected Pattern _pattern;

   /** Flag indicating how directories should be filtered */
   public int _directoryFlag;

   //---------------------------------------------------------------------

   /**
    * Constructor.  Default expression is null and directory flag is
    * DIRECTORY_AS_FILE.
    */

   public GeneralFileFilter() {
      setExpression(null);
      setDirectoryFlag(DIRECTORY_AS_FILE);
   }

   //---------------------------------------------------------------------

   /**
    * Constructor. Default directory flag is DIRECTORY_AS_FILE.
    * @param expression Initial expression used as file filter
    */

   public GeneralFileFilter(String expression) {
      this();
      setExpression(expression);
   }

   //---------------------------------------------------------------------

   /**
    * Constructor.
    * @param expression Initial expression used as file filter
    * @param directoryFlag Flag indicating treatment of directories,
    *        one of: DIRECTORY_ALWAYS_ACCEPT, DIRECTORY_ALWAYS_REJECT, 
    *        or DIRECTORY_AS_FILE.
    */

   public GeneralFileFilter(String expression, int directoryFlag) {
      this(expression);
      setDirectoryFlag(directoryFlag);
   }
   
   //---------------------------------------------------------------------

   /**
    * Sets the value of the filter expression.
    * 
    * @param expression New filter expression, can be null
    */

   public void setExpression(String expression) {
      if (this._expression == null && expression == null)
         return;
      if (this._expression != null && this._expression.equals(expression))
         return;

      this._expression    = expression;
      this._regExpression = regularize(this._expression);
      this._pattern       = Pattern.compile(this._regExpression);
   }

   //---------------------------------------------------------------------

   /**
    * Returns the value of the filter expression.
    * 
    * @return Filter expression.
    */

   public String getExpression() {
      return this._expression;
   }

   //---------------------------------------------------------------------
   
   /**
    * Returns the directory flag.  One of DIRECTORY_ALWAYS_ACCEPT,
    * DIRECTORY_ALWAYS_REJECT, or DIRECTORY_AS_FILE as defined in 
    * GeneralFileFilter.
    * @return Directory flag.
    */
   
   public int getDirectoryFlag()
   {
       return this._directoryFlag;
   }
   
   //---------------------------------------------------------------------
   
   /**
    * Set the directory flag for this filter.  Parameter must legal flag
    * from DIRECTORY_ALWAYS_ACCEPT, DIRECTORY_ALWAYS_REJECT, or 
    * DIRECTORY_AS_FILE as defined in GeneralFileFilter, otherwise
    * an IllegalArgumentException will be thrown.
    * @param dirFlag New directory flag value
    * @throws IllegalArgumentException if flag value is unrecognized.
    */
   
   public void setDirectoryFlag(int dirFlag) throws IllegalArgumentException
   {
       if (dirFlag == this._directoryFlag)
           return;
       
       if (dirFlag != DIRECTORY_ALWAYS_ACCEPT &&
           dirFlag != DIRECTORY_ALWAYS_REJECT &&    
           dirFlag != DIRECTORY_AS_FILE)
       {
           throw new IllegalArgumentException("Unrecognized flag: "+dirFlag);
       }
       
       this._directoryFlag = dirFlag;
   }
   
   //---------------------------------------------------------------------

   /**
    * Converts file system wildcard expression to regular expression. Periods
    * (.) are replaced with (\.). Stars (*) are replaced with (.*) Question
    * marks are replaced with (.) Null or empty string is replaced with .*
    * 
    * @param string Instance of wildcard expression, can be null
    * @return Associated regular expression.
    */

   protected String regularize(String string) {
      String regEx = string;

      if (string == null || string.equals("")) {
         regEx = ".*";
      } else {
         //replace . with \.
         regEx = regEx.replaceAll("\\.", "\\\\.");

         //replace * with .*
         regEx = regEx.replaceAll("\\*", "\\.\\*");

         //replace ? with .
         regEx = regEx.replaceAll("\\?", "\\.");
      }

      return regEx;
   }

   //---------------------------------------------------------------------

   /**
    * Tests whether or not the specified abstract pathname should be included in
    * a pathname list.
    * 
    * @param pathname The abstract pathname to be tested
    * @return True if and only if pathname should be included
    */

   public boolean accept(File pathname) { 
       
      if (pathname.isDirectory())
      {    
          if (this._directoryFlag == DIRECTORY_ALWAYS_ACCEPT)
              return true;
          else if (this._directoryFlag == DIRECTORY_ALWAYS_REJECT)
              return false;
      }

      if (this._expression == null)
         return false;

      if (this._pattern.matcher(pathname.getName()).matches())
         return true;

      return false;
   }

   //---------------------------------------------------------------------

   /**
    * Implements the required method for FilenameFilter to match file name
    * against the defined regular expression.
    * 
    * @param dir the directory in which the file was found
    * @param name the name of the file
    * @return true if and only if the name should be included in teh file list;
    *         false otherwise.
    */
   public boolean accept(File dir, String name) {
       return accept(new File(dir, name));
   }

   //---------------------------------------------------------------------

}