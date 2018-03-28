/*
 * Created on Dec 16, 2004
 */
package jpl.mipl.mdms.FileService.komodo.client;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.mipl.mdms.FileService.io.BoundedBufferedReader;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.FileType;
import jpl.mipl.mdms.utils.GetOptLong;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose: </b> Parsing utility for the UtilClient command-line arguments.
 * This class takes an array of Strings and parses according to a set of general
 * and specific rules.
 * 
 * Some operations utilize options files which specify a set of arguments per
 * line. To allow iteration through these separate invocations, clients can call
 * <code>iterations()</code>,<code>hasNext()</code> and
 * <code>advance()</code> methods.
 * 
 *    <PRE>
 *    Copyright 2005, California Institute of Technology. ALL RIGHTS RESERVED. U.S.
 *    Government Sponsorship acknowledge. 2005.
 *    </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History : </B> 
 * ----------------------
 * 
 * <B>Date              Who          What </B>
 * ----------------------------------------------------------------------------
 * 12/16/2004        Nick          Initial Release
 * 05/26/2005        Nick          Added push,pull options
 * 09/16/2005        Nick          Added push,pull to notify
 * 03/09/2006        Nick          Added 'format' to add, checkfiles, get, 
 *                                 list, notify, replace, subscribe.
 * 05/20/2007        Nick          Added query option and checks
 * 05/15/2008        Nick          Added un/register option and checks
 * 06/24/2008        Nick          Added allowable filenames check
 * 05/24/2009        Nick          Added filetype lock and unlock
 * 06/14/2009        Nick          Added change password
 * 06/16/2009        Nick          Added invoke options for get command
 * ============================================================================
 * </PRE>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: UtilCmdParser.java,v 1.66 2013/10/14 17:12:49 ntt Exp $
 *  
 */

public class UtilCmdParser {
   /** Keyword used for files lookup */
   public static final String KEYWORD_FILES = "files";

   protected static final String ERROR_TAG = "FEI_ERROR::";

   protected static final String ERR_MSG_MISSING_ARGS = "Required arguments missing";
   public static final int CODE_MISSING_ARGUMENTS = -123;

   protected static final int MAX_FILENAME_LENGTH = 256;

   /** Command line arguments */
   protected Hashtable _cmdLineArguments;

   /**
    * List of arguments from options file. Each entry is a hashtable of options
    * from a line of the file.
    */
   protected List _argumentList;

   /** String name representing the action/operation */
   protected String _actionName = "invalid";

   /** Id representing the action/operation */
   protected String _actionId = Constants.NOOPERATION;

   /** Reference to the command line argument array */
   protected String[] _args = new String[0];

   /** Table containing mapping from actionId to allowable keywords */
   protected Hashtable _actionTable;
   
   /** Table containing list of actions that accept filenames as arg*/
   protected List _filenamesActionTable;

   /** Flag that is true iff actionId is of type notify/subscription */
   protected boolean _actionIsAuto = false;

   /** Interval reference to the current argument hash */
   protected Hashtable _currentArguments;
   protected int _currentIndex = -1;

   /** Flag indicating usage of options file */
   protected boolean _using = false;

   /** Name of the options file, null if none specified */
   protected String _optionsFile = null;

   /** Flag indicating whether or not help should be printed */
   protected boolean _printHelp = false;

   /** Flag indicating error occurred during parsing */
   protected boolean _error = false;

   /** Logger instance */
   private Logger _logger = Logger.getLogger(this.getClass().getName());

   /** Contains argument array for each line entry of options file */
   protected String[][] _optionsFileArgs;

   protected final String _printableRegex = "\\p{Graph}*";
   protected Pattern _printablePattern = null;
   protected Matcher _matcher = null;
   
   /** Flag indicating that parsed tokens should be trimmed as part of 
    * of the process.
    */
   protected boolean _shouldTrimTokens = true;

   //---------------------------------------------------------------------

   /**
    * Constructor.
    */

   public UtilCmdParser() {
      init();
   }

   //---------------------------------------------------------------------

   /**
    * Initializes internal lookup table.
    */

   protected void init() {
      //-------------------------

      //init the action table, containing a list of possible argument key
      //words associated with each action id.

      _actionTable = new Hashtable();

      //accept
      this._actionTable.put(Constants.ACCEPT, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.FOR, CMD.OUTPUT,
            CMD.SAFEREAD, CMD.CRC, CMD.AUTODELETE, CMD.REPLACE, CMD.VERSION,
            CMD.HELP, CMD.RECEIPT, CMD.DIFF, CMD.FILEHANDLER });
      //add
      this._actionTable.put(Constants.ADDFILE,
            new String[] { CMD.FILETYPE, CMD.SERVERGROUP, CMD.AFTER,
                  CMD.BEFORE, CMD.BETWEEN, CMD.AND, CMD.COMMENT, CMD.CRC,
                  CMD.AUTODELETE, CMD.HELP, CMD.USING, CMD.FORMAT, 
                  CMD.RECEIPT, CMD.FILEHANDLER });
      //check
      this._actionTable.put(Constants.CHECK, new String[] {
              CMD.HELP});
      
      //checkfiles
      this._actionTable.put(Constants.CHECKFILES, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.BEFORE, CMD.AFTER, CMD.BETWEEN,
            CMD.AND, CMD.LONG, CMD.VERYLONG, CMD.HELP, CMD.FORMAT, CMD.USING });
      //comment
      this._actionTable.put(Constants.COMMENTFILE, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.COMMENT, CMD.FILEHANDLER, 
            CMD.HELP });
      //crc
      this._actionTable.put(Constants.COMPUTECHECKSUM,
            new String[] { CMD.FILETYPE, CMD.HELP });
      //delete
      this._actionTable.put(Constants.DELETEFILE, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.USING, CMD.FILEHANDLER,
            CMD.HELP });
      //display
      this._actionTable.put(Constants.DISPLAY, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.HELP });
      //get
      this._actionTable.put(Constants.GETFILES, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.BEFORE, CMD.AFTER, CMD.BETWEEN,
            CMD.AND, CMD.CRC, CMD.SAFEREAD, CMD.RECEIPT, CMD.OUTPUT,
            CMD.REPLACE, CMD.VERSION, CMD.HELP, CMD.USING, CMD.FORMAT, 
            CMD.QUERY, CMD.REPLICATE, CMD.REPLICATEROOT, CMD.INVOKE, 
            CMD.INVOKEEXITONERROR, CMD.INVOKEASYNC, CMD.FILEHANDLER,
            CMD.DIFF});
      //kinit
      this._actionTable.put(Constants.CREDLOGIN, new String[] {
            /*CMD.FILETYPE,*/ CMD.HELP, CMD.USER, CMD.SERVERGROUP });
      //klist
      this._actionTable.put(Constants.CREDLIST,
            new String[] { CMD.HELP });
      //kdestroy
      this._actionTable.put(Constants.CREDLOGOUT,
            new String[] { CMD.HELP, CMD.SERVERGROUP });
      //list
      this._actionTable.put(Constants.SHOWFILES, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.BEFORE, CMD.AFTER, CMD.BETWEEN,
            CMD.AND, CMD.LONG, CMD.VERYLONG, CMD.HELP, CMD.FORMAT, CMD.QUERY,
            CMD.FILEHANDLER});
      //makeclean
      this._actionTable.put(Constants.MAKECLEAN, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.HELP });
      //notify
      this._actionTable.put(Constants.AUTOSHOWFILES, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.OUTPUT, CMD.RESTART, CMD.USING,
            CMD.CRC, CMD.INVOKE, CMD.INVOKEEXITONERROR, CMD.LOGFILE,
            CMD.LOGFILEROLLING, CMD.MAILMESSAGEFROM, CMD.MAILMESSAGETO,
            CMD.MAILREPORTAT, CMD.MAILREPORTTO, CMD.MAILSMTPHOST, 
            CMD.MAILSILENTRECONN, CMD.HELP,
            CMD.PULL, CMD.PUSH, CMD.FORMAT, CMD.INVOKEASYNC, CMD.QUERY,
            CMD.FILEHANDLER});
      //register
      this._actionTable.put(Constants.REGISTERFILE, new String[] {
          CMD.FILETYPE, CMD.SERVERGROUP, CMD.BEFORE, CMD.AFTER, CMD.BETWEEN,
          CMD.AND, CMD.OUTPUT, CMD.HELP, CMD.USING, CMD.FORMAT, CMD.COMMENT,
          CMD.REPLACE, CMD.FORCE, CMD.RECEIPT, CMD.FILEHANDLER});
      //unregister
      this._actionTable.put(Constants.UNREGISTERFILE, new String[] {
          CMD.FILETYPE, CMD.SERVERGROUP, CMD.USING, 
          CMD.FILEHANDLER, CMD.HELP});
      //lock
      this._actionTable.put(Constants.LOCKFILETYPE, new String[] {
          CMD.FILETYPE, CMD.SERVERGROUP, CMD.OWNER, CMD.GROUP, CMD.HELP});
      //unlock
      this._actionTable.put(Constants.UNLOCKFILETYPE, new String[] {
          CMD.FILETYPE, CMD.SERVERGROUP, CMD.OWNER, CMD.GROUP, CMD.HELP});
      //changepassword
      this._actionTable.put(Constants.CHANGEPASSWORD, new String[] {
          CMD.SERVERGROUP, CMD.HELP});
      //rename
      this._actionTable.put(Constants.RENAMEFILE, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.USING, CMD.HELP,
            CMD.FILEHANDLER});
      //replace
      this._actionTable.put(Constants.REPLACEFILE,
            new String[] { CMD.FILETYPE, CMD.SERVERGROUP, CMD.BEFORE,
                  CMD.AFTER, CMD.BETWEEN, CMD.AND, CMD.COMMENT, CMD.AUTODELETE,
                  CMD.CRC, CMD.HELP, CMD.USING, CMD.FORMAT, CMD.RECEIPT,
                  CMD.DIFF, CMD.FILEHANDLER});
      //reference
      this._actionTable.put(Constants.SETREFERENCE, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.VFT, CMD.REFERENCE, CMD.HELP });
      //showtypes
      this._actionTable.put(Constants.SHOWTYPES, new String[] {
            CMD.SERVERGROUP, CMD.FILETYPE, CMD.SERVERGROUPS, CMD.CLASSIC,
            CMD.HELP });

      //subscribe
      this._actionTable.put(Constants.AUTOGETFILES, new String[] {
            CMD.FILETYPE, CMD.SERVERGROUP, CMD.OUTPUT, CMD.RESTART, CMD.USING,
            CMD.CRC, CMD.INVOKE, CMD.INVOKEEXITONERROR, CMD.LOGFILE,
            CMD.LOGFILEROLLING, CMD.MAILMESSAGEFROM, CMD.MAILMESSAGETO,
            CMD.MAILREPORTAT, CMD.MAILREPORTTO, CMD.MAILSMTPHOST, 
            CMD.MAILSILENTRECONN, CMD.SAFEREAD,
            CMD.RECEIPT, CMD.REPLACE, CMD.VERSION, CMD.HELP, CMD.PULL,
            CMD.PUSH, CMD.FORMAT, CMD.INVOKEASYNC, CMD.QUERY, CMD.REPLICATE,
            CMD.REPLICATEROOT, CMD.FILEHANDLER, CMD.DIFF});

      //-------------------------
      //this list contains actions that accept filenames/expr at the command 
      //line.  It makes it easier to check for incorrect options for things
      //that don't accept filenames
      String[] args_with_filename_keyword = new String[] { 
              Constants.ACCEPT,          Constants.ADDFILE,
              Constants.CHECKFILES,      Constants.COMMENTFILE,
              Constants.COMPUTECHECKSUM, Constants.DELETEFILE,
              Constants.DISPLAY,         Constants.GETFILES,
              Constants.SHOWFILES,       Constants.REGISTERFILE,
              Constants.UNREGISTERFILE,  Constants.RENAMEFILE,
              Constants.REPLACEFILE,     Constants.SETREFERENCE};
      
      _filenamesActionTable = new Vector();
      for (String s : args_with_filename_keyword)
          _filenamesActionTable.add(s);
      
   }

   //---------------------------------------------------------------------

   /**
    * Performs the parsing of the arguments.
    * 
    * @param actionId Id associated with the operation to be performed.
    * @param args[] String array of arguments
    * @throws ParseException if parsing cannot complete successfully
    */

   public void parse(String actionId, String args[]) throws ParseException {
      this._args = args;

      this._actionId = actionId;
      this._actionName = ActionTable.toName(_actionId);
      this._actionIsAuto = (actionId.equals(Constants.AUTOSHOWFILES) || 
                            actionId.equals(Constants.AUTOGETFILES));

      this._cmdLineArguments = new Hashtable();
      this._argumentList = new ArrayList();
      this._optionsFile = null;

      parse();
   }

   //---------------------------------------------------------------------

   /**
    * Given a command id and argument array, parses the arguments according to
    * the command throwing Exception if (1) unrecognized argument (2) missing
    * value (3) missing required argument (4) ...
    * 
    * @throws ParseException if error occurs druring parsing
    */

   protected void parse() throws ParseException {
      //call the general parsing method
      generalParse();

      //init the arguments pointer
      reset();
   }

   //---------------------------------------------------------------------

   /**
    * Returns true if keyword was set in the current argument set. Use
    * <code>hasValue()</code> to check value of keyword.
    * 
    * @param keyword Keyword of the value to be checked
    * @return True if keyword is found in current argument set.
    */

   public boolean isSet(String keyword) {
      if (this._currentArguments != null)
         return this._currentArguments.keySet().contains(keyword);
      else
         return false;
   }

   //---------------------------------------------------------------------

   /**
    * Returns true if value associated with keyword from current argument 
    * set is defined and non-null.
    * 
    * @param keyword Keyword of the value to be checked
    * @return True if value associated with keyword is non-null, else false.
    */

   public boolean hasValue(String keyword) {
      Object value = null;
      if (this._currentArguments != null) {
         value = this._currentArguments.get(keyword);
      }
      return (value != null);
   }

   //---------------------------------------------------------------------

   /**
    * Returns value associated with keyword from current argument set.
    * 
    * @param keyword Keyword of the value to be returned
    * @return Value associated with keyword if found, else null.
    */

   public Object getValue(String keyword) {
      Object value = null;
      if (this._currentArguments != null) {
         value = this._currentArguments.get(keyword);
      }
      return value;
   }

   //---------------------------------------------------------------------

   /**
    * Returns true iff there are more iterations of operation invocations 
    * that have yet to be processed.
    * 
    * @return True if next() can be called again.
    */

   public boolean hasNext() {
      if (this._argumentList != null)
         return this._currentIndex < this._argumentList.size() - 1;
      else
         return false;
   }

   //---------------------------------------------------------------------

   /**
    * Advances to the next argument set. Should be called when return 
    * value from iterations() is greater than 1.
    */

   public void advance() {
      ++this._currentIndex;
      if (this._currentIndex < this._argumentList.size())
         this._currentArguments = (Hashtable) this._argumentList
               .get(this._currentIndex);
      else
         this._currentArguments = null;
   }

   //---------------------------------------------------------------------

   /**
    * Resets to beginning of arguments list.
    */

   public void reset() {
      this._currentIndex = 0;
      if (this._argumentList == null || this._argumentList.isEmpty())
         this._currentArguments = null;
      else
         this._currentArguments = (Hashtable) this._argumentList
               .get(this._currentIndex);
   }

   //---------------------------------------------------------------------

   /**
    * Returns reference to the current argument map.
    * 
    * @return Current argument map
    */

   public Map getCurrentArguments() {
      return this._currentArguments;
   }

   //---------------------------------------------------------------------

   /**
    * Returns the number of invocations of the operation. This will 
    * return a number greater than 1 if using a options list that 
    * contains one invocation per line.
    * 
    * @return Number of expected invocations of operation based on 
    *         separate argument sets per invocation.
    */

   public int iterations() {
      if (this._argumentList != null)
         return this._argumentList.size();
      else
         return 0;
   }

   //---------------------------------------------------------------------

   /**
    * Returns the filename of the options file if specified at the 
    * command line argument set. If no filename was specified, null 
    * is returned.
    * 
    * @return Options filename, null if not specified.
    */

   public String getOptionsFilename() {
      return this._optionsFile;
   }

   //---------------------------------------------------------------------

   /**
    * Returns true iff help should be printed for the operation, either 
    * if help keyword was found or parse error occurred.
    * 
    * @return True if help/usage should be printed, false otherwise.
    */

   public boolean printHelp() {
      return this._printHelp;
   }

   //---------------------------------------------------------------------

   /**
    * This method checks an option set for known conflicting options. (1)
    * TimeFilterSet = {BEFORE, AFTER, BETWEEN} (2) VerboseSet = {LONG, VERYLONG}
    * (3) DuplicateSet = {REPLACE, VERSION} (4) UsageSet = {USING, HELP} (5)
    * Between = Looks at uses of BETWEEN and pairs with AND (6) Using = Looks at
    * placement of USING and checks file. Verifies that action can have USING,
    * and in most cases, if it does, has no other args (7) MailSet = checks that
    * mail arguments are used consistently. These include {mailMessageFrom,
    * mailMessageTo, mailReportAt, mailReportTo, mailSMTPHost}
    * 
    * @param args Current hashtable of arguments being checked
    */

   protected void checkConsistency(Hashtable arguments, boolean fromFile)
         throws ParseException {
      int count = 0;

      //TimeFilterSet
      if (arguments.get(CMD.BEFORE) != null)
         ++count;
      if (arguments.get(CMD.AFTER) != null)
         ++count;
      if (arguments.get(CMD.BETWEEN) != null)
         ++count;
      if (count > 1)
         throw new ParseException("Conflicting time filters found.  "
               + "Choose at most one of {" + CMD.BEFORE + "," + CMD.AFTER + ","
               + CMD.BETWEEN + "}", setError(-1));

      //VerboseSet
      count = 0;
      if (arguments.get(CMD.LONG) != null)
         ++count;
      if (arguments.get(CMD.VERYLONG) != null)
         ++count;
      if (count > 1)
         throw new ParseException("Conflicting verbosity keywords found.  "
               + "Choose at most one of {" + CMD.LONG + "," + CMD.VERYLONG
               + "}", setError(-1));

      //DuplicateSet
      count = 0;
      if (arguments.get(CMD.REPLACE) != null)
         ++count;
      if (arguments.get(CMD.VERSION) != null)
         ++count;
      if (count > 1)
         throw new ParseException("Conflicting duplicate keywords found.  "
               + "Choose at most one of {" + CMD.REPLACE + "," + CMD.VERSION
               + "}", setError(-1));

      //UsageSet
      count = 0;
      if (arguments.get(CMD.HELP) != null)
         ++count;
      if (arguments.get(CMD.USING) != null)
         ++count;
      if (count > 1)
         throw new ParseException("Conflicting keywords found.  "
               + "Choose at most one of {" + CMD.USING + "," + CMD.HELP + "}",
               setError(-1));

      //check pairing of BETWEEN date1 AND date2
      if (arguments.get(CMD.BETWEEN) != null) {
         for (int i = 0; i < this._args.length; ++i) {
            String arg = this._args[i];
            if (arg.equalsIgnoreCase(CMD.BETWEEN)) {
               if (i + 3 >= this._args.length
                     || !this._args[i + 2].equalsIgnoreCase(CMD.AND))
                  throw new ParseException("'" + CMD.BETWEEN
                        + "' pair must be followed by '" + CMD.AND + "' pair",
                        setError(-1));
            }
         }
      }

      //check usage of USING
      if (arguments.get(CMD.USING) != null) {
         if (fromFile)
            throw new ParseException("Cannot supply " + CMD.USING
                  + "keyword within an options file", setError(-1));

         //check that action allows USING
         if (this._actionId.equals(Constants.ADDFILE)
             || this._actionId.equals(Constants.CHECKFILES)
             || this._actionId.equals(Constants.DELETEFILE)
             || this._actionId.equals(Constants.GETFILES)
             || this._actionId.equals(Constants.DELETEFILE)
             || this._actionId.equals(Constants.RENAMEFILE)
             || this._actionId.equals(Constants.REPLACEFILE)
             || this._actionId.equals(Constants.REGISTERFILE)
             || this._actionId.equals(Constants.UNREGISTERFILE))
         {
            //these should ONLY have "USING filename"
            if (arguments.size() > 1)
               throw new ParseException("Conflict of keywords.  "
                     + "When supplying '" + CMD.USING + "' keyword, "
                     + " no other keywords are allowed.", setError(-1));
         }
         //these can have USING with overriding options
         else if (this._actionIsAuto) {
            //can have overrides
         } else
            //nothing else should include USING
            throw new ParseException("Cannot use '" + CMD.USING
                  + "' keyword with the operation: " + this._actionName,
                  setError(-1));

         //if using, make sure that filename was included, and that they
         //were the first two arguments. Also check that file exists.
         if (this._args.length < 2)
            throw new ParseException("'" + CMD.USING + "' keyword "
                  + "requires an options filename", setError(-1));
         if (!this._actionIsAuto) {
            if (!this._args[0].equals(CMD.USING))
               throw new ParseException("'" + CMD.USING + "' keyword "
                     + "must be the first argument", setError(-1));
            File file = new File(this._args[1]);
            if (!file.exists())
               throw new ParseException("Options file '" + this._args[1]
                     + "' does not exist.", setError(-1));
         }
      }

      //(7) MailSet
      if (fromFile) {
         if ((arguments.get(CMD.MAILMESSAGETO) != null)) {
            if ((arguments.get(CMD.MAILMESSAGEFROM) == null)
                  || (arguments.get(CMD.MAILMESSAGETO) == null)
                  || (arguments.get(CMD.MAILSMTPHOST) == null))
               throw new ParseException(
                     "Missing one or more of mail message settings: "
                           + CMD.MAILMESSAGEFROM + ", " + CMD.MAILMESSAGETO
                           + ", " + CMD.MAILSMTPHOST, setError(-1));
         }

         if ((arguments.get(CMD.MAILREPORTAT) != null)
               || (arguments.get(CMD.MAILREPORTTO) != null)) {
            if ((arguments.get(CMD.MAILMESSAGEFROM) == null)
                  || (arguments.get(CMD.MAILREPORTAT) == null)
                  || (arguments.get(CMD.MAILREPORTTO) == null)
                  || (arguments.get(CMD.MAILSMTPHOST) == null))
               throw new ParseException(
                     "Missing one or more of mail report settings: "
                           + CMD.MAILMESSAGEFROM + ", " + CMD.MAILREPORTTO
                           + ", " + CMD.MAILREPORTAT + ", " + CMD.MAILSMTPHOST,
                     setError(-1));
         }
      }
      
      //(8) Invoke options
      if (arguments.get(CMD.INVOKE) != null)
      {
          if ((arguments.get(CMD.INVOKEEXITONERROR) != null) &&
              (arguments.get(CMD.INVOKEASYNC) != null))
              throw new ParseException("Conflicting invoke options found.  "
                      + "Choose at most one of {" + CMD.INVOKEEXITONERROR + 
                      "," + CMD.INVOKEASYNC + "}", setError(-1));          
      }

      //(9) Push/Pull/Query options
      if (arguments.get(CMD.PUSH) != null)
      {
          if (arguments.get(CMD.PULL) != null)
          {
              throw new ParseException("Conflicting subscription options found.  "
                      + "Choose at most one of {" + CMD.PULL + 
                      "," + CMD.PUSH + "}", setError(-1)); 
          }
          if (arguments.get(CMD.QUERY) != null)
          {
              throw new ParseException("Conflicting options found.  "
                      + "Choose at most one of {" + CMD.QUERY + 
                      "," + CMD.PUSH + "}", setError(-1)); 
              
          }
      }
          
      //(10) Lock/Unlock mode modifiers
      if (arguments.get(CMD.GROUP) != null && arguments.get(CMD.OWNER) != null)
      {
          throw new ParseException("Conflicting type locking modes found.  "
                      + "Choose at most one of {" + CMD.GROUP + 
                      "," + CMD.OWNER + "}", setError(-1));           
      }
      
      //end_consistency_check
   }

   //---------------------------------------------------------------------

   /**
    * This method performs a general parse of the command line arguments,
    * performs a consistency check, then applies more specific parsing according
    * to the action id. If an options file was specified, it will be loaded and
    * then finally operation-specific parsing will be applied.
    */

   protected void generalParse() throws ParseException {
      //1) parse the arguments naively
      this._cmdLineArguments = new Hashtable();
      parseArgs(this._args, _cmdLineArguments);

      //2) perform general consistency check
      checkConsistency(this._cmdLineArguments, false);

      //3) check that there are no extra arguments than expected
      checkAllowableArguments();
      
      //4) Load options file info if specified
      if (this._using) {
         if (this._optionsFile == null)
            throw new ParseException("'" + CMD.USING + "' keyword "
                  + "requires an options filename", setError(-1));

         parseOptionsFile(this._optionsFile);
      }

      //5) merge arguments
      mergeArguments();

      //6) Check if help should be printed
      if (!this._printHelp)
         this._printHelp = shouldPrintHelp();

      //7) Perform task-specific checks
      parseSpecific();

   }

   //---------------------------------------------------------------------
   
   protected void checkAllowableArguments() throws ParseException
   {
       String[] allowable = (String[]) _actionTable.get(_actionId);
       
       if (allowable.length == 0 && !_cmdLineArguments.keySet().isEmpty()) {
          if (this._actionId.equals(Constants.COMPUTECHECKSUM))
             throw new ParseException("Operation " + this._actionName
                   + " expects no arguments.", setError(-1));
       }
       
       List allowableList = Arrays.asList(allowable);
       Collections.sort(allowableList);
       
       TreeSet keys = new TreeSet();
       keys.addAll(_cmdLineArguments.keySet());
       Iterator it = keys.iterator();
       
       while (it.hasNext()) {
          String key = (String) it.next();
          String badKey = null;
          
          //if not FILENAME and not in allowable set, its an error
          if (!key.equals(KEYWORD_FILES))
          {
              if (!allowableList.contains(key))
                  badKey = key;
              

          }  //if FILENAME and cmd doesnt take files, then error
          else if (!_filenamesActionTable.contains(_actionId))
          {
              String[] values = (String[]) _cmdLineArguments.get(KEYWORD_FILES);

              if (values != null && values.length > 0)
                  badKey = values[0];
              else    //it should never happen, but if values is empty, use an expr
                  badKey = "<"+KEYWORD_FILES+">";
          }
          
          //if we determined that there was a bad key, throw exception
          if (badKey != null)
              throw new ParseException("Command line argument for '" + badKey
                                       + "' unrecognized", setError(-1));
                    
//          
//          if (!key.equals(KEYWORD_FILES) && !allowableList.contains(key)) {
//             throw new ParseException("Command line argument for '" + key
//                                    + "' unrecognized", setError(-1));
          
       }
   }
   
   //---------------------------------------------------------------------

   /**
    * Adds command line arguments to options file arguments, as a way of
    * overriding and supplementing arguments.
    */

   protected void mergeArguments() {
      //for each entry in the file arguments, add the command line
      //args as well

      //NOTE: listArguments should only be populated from an
      //options file prior to this point. Afterwards, listArguments
      //will contain the refernce list of invocation arguments.

      if (this._using) {
         Iterator it = this._argumentList.iterator();
         while (it.hasNext()) {
            Hashtable fileArgs = (Hashtable) it.next();
            fileArgs.putAll(this._cmdLineArguments);
         }
      } else {
         this._argumentList.add(this._cmdLineArguments);
      }
   }

   //---------------------------------------------------------------------

   /**
    * Protected method that determines if help should be printed based on a set
    * of known-criteria such as error conditions.
    * 
    * @return True if help should be printed, false otherwise.
    */

   protected boolean shouldPrintHelp() {
      //todo - add checks to infer that help needs printin'
      return false;
   }

   //---------------------------------------------------------------------

   /**
    * Wrapper method that dispatches specific parse checking based on action id.
    * If the printHelp member is set, then no specific parsing is performed.
    */

   protected void parseSpecific() throws ParseException 
   {
      if (this._printHelp)
         return;

      if (this._actionId.equals(Constants.ACCEPT))
      {
          parseAccept();
      }
      else if (this._actionId.equals(Constants.ADDFILE))
      {
          parseAdd();
      }
      else if (this._actionId.equals(Constants.CHECK))
      {
          parseCheck();
      }
      else if (this._actionId.equals(Constants.GETFILES))
      {
          parseGet();
      }
      else if (this._actionId.equals(Constants.AUTOGETFILES))
      {
          parseSubscribe();
      }
      else if (this._actionId.equals(Constants.AUTOSHOWFILES))
      {
          parseNotify();
      }
      else if (this._actionId.equals(Constants.COMPUTECHECKSUM))
      {
          parseCrc();
      }
      else if (this._actionId.equals(Constants.CHECKFILES))
      {
          parseCheckfiles();
      }
      else if (this._actionId.equals(Constants.DELETEFILE))
      {
          parseDelete();
      }
      else if (this._actionId.equals(Constants.MAKECLEAN))
      {
          parseMakeClean();
      }
      else if (this._actionId.equals(Constants.DISPLAY))
      {
          parseDisplay();
      }
      else if (this._actionId.equals(Constants.SHOWFILES))
      {
          parseList();
      }
      else if (this._actionId.equals(Constants.SETREFERENCE))
      {
          parseReference();
      }
      else if (this._actionId.equals(Constants.RENAMEFILE))
      {
          parseRename();
      }
      else if (this._actionId.equals(Constants.REPLACEFILE))
      {
          parseReplace();
      }
      else if (this._actionId.equals(Constants.COMMENTFILE))
      {
          parseComment();
      }
      else if (this._actionId.equals(Constants.SHOWTYPES))
      {
          parseShowtypes();
      }
      else if (this._actionId.equals(Constants.CREDLIST))
      {
          parseKlist();
      }
      else if (this._actionId.equals(Constants.CREDLOGIN))
      {
          parseKinit();
      }
      else if (this._actionId.equals(Constants.CREDLOGOUT))
      {
          parseKdestroy();
      }
      else if (this._actionId.equals(Constants.REGISTERFILE))
      {
          parseRegister();
      }
      else if (this._actionId.equals(Constants.UNREGISTERFILE))
      {
          parseUnregister();
      }
      else if (this._actionId.equals(Constants.LOCKFILETYPE))
      {
          parseLocktype();
      }
      else if (this._actionId.equals(Constants.UNLOCKFILETYPE))
      {
          parseUnlocktype();
      }
      else if (this._actionId.equals(Constants.CHANGEPASSWORD))
      {
          parseChangePassword();
      }
      else
      {
          throw new ParseException("Invalid operation.", setError(-1));
      }

   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5accept fei5accept &lt;filetype&gt; for 
    * &lt;add|replace|get|delete&gt; {[output &lt;path&gt;] [crc] [restart] 
    * [saferead] [autodelete] [replace|version]}
    *  
    */

   protected void parseAccept() throws ParseException {
      if (this._args.length < 3)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));

      String arg1 = this._args[1];
      String arg2 = this._args[2];

      //arg 1 must be "for"
      if (!arg1.equalsIgnoreCase(CMD.FOR))
         throw new ParseException("Argument 2 must be '" + CMD.FOR
               + "' instead of " + arg1, setError(-1));

      //arg 2 must be "add|replace|get|delete" (silently, we accept "remove")
      if (!(arg2.equalsIgnoreCase("add") || arg2.equalsIgnoreCase("replace")
            || arg2.equalsIgnoreCase("get") || arg2.equalsIgnoreCase("delete") 
            || arg2.equalsIgnoreCase("remove"))) {
         throw new ParseException("Argument 3 must be one of {"
               + "add,replace,get,delete}, instead of '" + arg2+"'", 
               setError(-1));
      }

      //grab files from standard in
      String[] files = this.getFileListFromSTDIN();
      if (files == null)
         throw new ParseException("Could not read file list from "
               + "standard input.", setError(-1));

      for (int i = 0; i < this._argumentList.size(); ++i) {
         Map map = (Map) this._argumentList.get(i);
         map.put(KEYWORD_FILES, files);
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5add fei5add [ &lt;server group&gt;:] &lt;file type&gt; 
    * &lt;file name expression&gt;... {[before|after &lt;date-time&gt;] | 
    * [between &lt;date-time1&gt;and &lt;date-time2&gt;] [comment " 
    * &lt;comment text&gt;"] [crc] [autodelete] [help]}
    * 
    * fei5add using &lt;option file&gt; Option File Format (per line): 
    * [ &lt;server group&gt;:] &lt;file type&gt; &lt;file name&gt;... 
    * {[before|after &lt;date-time&gt;] | [between
    * &lt;date-time1&gt;and &lt;date-time2&gt;] [comment " 
    * &lt;comment text&gt;"] [autodelete] [crc]}
    *  
    */

   protected void parseAdd() throws ParseException {
      if (this._args.length < 2)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));

      //check options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length < 2)
               throw new ParseException("Required arguments missing "
                     + "from options file at line " + (i + 1), setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5check fei5check
    */

   protected void parseCheck() throws ParseException {
      if (this._args.length > 1)
          throw new ParseException("At most one argument expected",
                                setError(-1));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5checkfiles fei5checkfiles [ &lt;server group&gt;:] 
    * &lt;file type&gt; ["&lt;file name expression&gt;"] {[before|after 
    * &lt;date-time&gt;] | [between &lt;date-time1&gt;and &lt;date-time2&gt;] 
    * [long | verylong] [help]}
    * 
    * fei5checkfiles using &lt;option file&gt; Option File Format 
    * (per line): [
    * &lt;server group&gt;:] &lt;file type&gt; " &lt;file name expression&gt;" 
    * {[before|after &lt;date-time&gt;] | [between &lt;date-time1&gt;and 
    * &lt;date-time2&gt;] [long | verylong]}
    */

   protected void parseCheckfiles() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));

      //check options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length < 1)
               throw new ParseException("Required arguments missing "
                     + "from options file at line " + (i + 1), setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5comment fei5comment [ &lt;server group&gt;:] &lt;file
    * type&gt; &lt;file name&gt; comment " &lt;comment text&gt;" [help]
    */

   protected void parseComment() throws ParseException {
      if (this._args.length < 3)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));

      String arg2 = this._args[2];

      //arg 2 must be "comment"
      if (!arg2.equalsIgnoreCase(CMD.COMMENT)) {
         throw new ParseException("Argument 3 must be '" + CMD.COMMENT
               + "' instead of " + arg2, setError(-1));
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5crc fei5crc &lt;file name expression&gt;
    */

   protected void parseCrc() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
                      setError(CODE_MISSING_ARGUMENTS));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5delete fei5delete [ &lt;server group&gt;:]
    *  &lt;file type&gt; " &lt;file name expression&gt;" [filehandler] [help]
    * 
    * fei5delete using &lt;option file&gt; Option File Format (per line): 
    * [ &lt;server group&gt;:] &lt;file type&gt; &lt;file name&gt;
    */

   protected void parseDelete() throws ParseException {
      if (this._args.length != 2 && this._args.length != 3) {
         throw new ParseException("Exactly two or three arguments expected",
               setError(CODE_MISSING_ARGUMENTS));
      }
      //check options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length != 2 && this._optionsFileArgs[i].length != 3)
               throw new ParseException("Exactly two or three  arguments expected "
                     + " from options file at line " + (i + 1), setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------
   
   protected void parseMakeClean() throws ParseException {
      if (this._args.length < 1) {
         throw new ParseException(ERR_MSG_MISSING_ARGS, this
               .setError(CODE_MISSING_ARGUMENTS));
      }

   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5display fei5display [ &lt;server group&gt;:] 
    * &lt;file type&gt; &lt;file name&gt; [help]
    */

   protected void parseDisplay() throws ParseException {
      if (this._args.length != 2)
         throw new ParseException("Exactly two arguments expected",
               setError(CODE_MISSING_ARGUMENTS));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5get 
    * 
    * fei5get [ &lt;server group&gt;:] &lt;file type&gt;
    * [" &lt;file name expression&gt;"] {[output &lt;path&gt;] [before|after 
    * &lt;datetime&gt;] | [between &lt;datetime1&gt;and &lt;datetime2&gt;] 
    * [crc] [saferead] [receipt] [replace|version] [replicate] [replicateroot] 
    * [help]}
    * 
    * fei5get using &lt;option file&gt; Option File Format (per line): [ 
    * &lt;server group&gt;:] &lt;file type&gt; [" &lt;file name expression&gt;"]
    * {[output &lt;path&gt;] [before|after &lt;date-time&gt;] | [between 
    * &lt;date-time1&gt;and &lt;date-time2&gt;] [crc] [saferead] [receipt] 
    * [replace|version]} invoke &lt;command&gt; invokeExitOnError invokeAsync
    */

   protected void parseGet() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));

      //check options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length < 1)
               throw new ParseException("Required arguments missing "
                     + "from options file at line " + (i + 1), setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5kdestroy fei5kdestroy
    */

   protected void parseKdestroy() throws ParseException {
      if (this._args.length > 1)
         throw new ParseException("At most one argument expected",
               setError(CODE_MISSING_ARGUMENTS));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5kinit fei5kinit
    */

   protected void parseKinit() throws ParseException {
      if (this._args.length > 2)
         throw new ParseException("At most two arguments expected",
               setError(CODE_MISSING_ARGUMENTS));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5klist fei5klist
    */

   protected void parseKlist() throws ParseException {
      if (this._args.length != 0)
         throw new ParseException("No arguments expected",
               setError(CODE_MISSING_ARGUMENTS));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5list 
    * fei5list [ &lt;server group&gt;:] &lt;file type&gt;
    * [" &lt;file name expression&gt;"] {[before|after &lt;date-time&gt;] | 
    * [between &lt;date-time1&gt;and &lt;date-time2&gt;] [long | verylong] [help]}
    */

   protected void parseList() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5changepassword 
    * fei5changepassword [ &lt;server group&gt;:] &lt;file type&gt;
    *  [help]}
    */

   protected void parseChangePassword() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));
   }
   
   //---------------------------------------------------------------------

   /**
    * Parsing for fei5locktype 
    * fei5locktype [ owner | group ]
    */

   protected void parseLocktype() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));
   }
   
   //---------------------------------------------------------------------

   /**
    * Parsing for fei5unlocktype 
    * fei5unlocktype [ owner | group ]
    */

   protected void parseUnlocktype() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));
   }
   
   //---------------------------------------------------------------------

//   /**
//    * Parsing for fei5makeclean fei5makeclean [ &lt;server group&gt;:] 
//    * &lt;file type&gt; "&lt;file name expression&gt;" [help]
//    *  
//    */
//
//   protected void parseMakeclean() throws ParseException {
//      if (this._args.length < 1)
//         throw new ParseException(ERR_MSG_MISSING_ARGS,
//               setError(CODE_MISSING_ARGUMENTS));
//   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5reference fei5reference [ &lt;server group&gt;:] 
    * &lt;file type&gt; &lt;file name&gt; vft &lt;VFT name&gt; reference 
    * &lt;ref name&gt; [help]
    */

   protected void parseReference() throws ParseException {
      if (this._args.length != 6)
         throw new ParseException("Exactly six arguments expected",
               setError(CODE_MISSING_ARGUMENTS));

      String arg2 = this._args[2];
      String arg4 = this._args[4];

      if (!arg2.equalsIgnoreCase(CMD.VFT))
         throw new ParseException("Argument 3 expected to be '" + CMD.VFT
               + "' instead of " + arg2, setError(-1));
      if (!arg4.equalsIgnoreCase(CMD.REFERENCE))
         throw new ParseException("Argument 5 expected to be '" + CMD.REFERENCE
               + "' instead of " + arg4, setError(-1));
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5register: fei5register [ &lt;server group&gt;:] &lt;file type&gt; 
    * &lt;file name expression&gt;... {[before|after &lt;date-time&gt;] | 
    * [between &lt;date-time1&gt;and &lt;date-time2&gt;] [comment " 
    * &lt;comment text&gt;"] [help]}
    * 
    * fei5register using &lt;option file&gt; Option File Format (per line): 
    * [ &lt;server group&gt;:] &lt;file type&gt; &lt;file name&gt;... 
    * {[before|after &lt;date-time&gt;] | [between
    * &lt;date-time1&gt;and &lt;date-time2&gt;] [comment " 
    * &lt;comment text&gt;"]}
    *  
    */

   protected void parseRegister() throws ParseException {
      if (this._args.length < 2)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));

      //check options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length < 2)
               throw new ParseException("Required arguments missing "
                     + "from options file at line " + (i + 1), setError(-1));
         }
      }
   }
   
   //---------------------------------------------------------------------

   /**
    * Parsing for fei5unregister: fei5unregister [ &lt;server group&gt;:]
    *  &lt;file type&gt; " &lt;file name expression&gt;" [help]
    * 
    * fei5unregister using &lt;option file&gt; Option File Format (per line): 
    * [ &lt;server group&gt;:] &lt;file type&gt; &lt;file name&gt;
    */

   protected void parseUnregister() throws ParseException {
      if (this._args.length != 2 && this._args.length != 3 ) {
         throw new ParseException("Exactly two or three arguments expected",
               setError(CODE_MISSING_ARGUMENTS));
      }
      //check options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length != 2 &&
                this._optionsFileArgs[i].length != 3)
               throw new ParseException("Exactly two or three arguments expected "
                     + " from options file at line " + (i + 1), setError(-1));
         }
      }
   }
   
   //---------------------------------------------------------------------

   /**
    * Parsing for fei5rename fei5rename [ &lt;server group&gt;:] 
    * &lt;file type&gt; &lt;old file name&gt; &lt;new file name&gt; [help]
    * 
    * fei5rename using &lt;option file&gt; Option File Format (per line): [ 
    * &lt;server group&gt;:] &lt;file type&gt; &lt;old file name&gt; 
    * &lt;new file name&gt;
    *  
    */

   protected void parseRename() throws ParseException {
      if (!this._using && this._args.length != 3 && this._args.length != 4)
         throw new ParseException("Exactly three or four arguments expected",
               setError(CODE_MISSING_ARGUMENTS));

      if (this._using && this._args.length != 2)
          throw new ParseException(
                "Exactly two arguments expected when options file used",
                setError(CODE_MISSING_ARGUMENTS));

      
      //parse options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length != 3 && 
                                this._optionsFileArgs[i].length != 4)
               throw new ParseException("Exactly three or four arguments "
                     + "expected from options file at line " + (i + 1),
                     setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5replace fei5replace [ &lt;server group&gt;:] 
    * &lt;file type&gt; &lt;file name expression&gt;... {[before|after 
    * &lt;date-time&gt;] | [between &lt;date-time1&gt;
    * and &lt;date-time2&gt;] [comment " &lt;comment text&gt;"] [crc] 
    * [autodelete] [help]}
    * 
    * fei5replace using &lt;option file&gt; Option File Format (per line): 
    * [ &lt;server group&gt;:] &lt;file type&gt; &lt;file name&gt;... 
    * {[before|after &lt;date-time&gt;] | [between &lt;date-time1&gt;and 
    * &lt;date-time2&gt;] [comment " &lt;comment text&gt;"] [autodelete]
    * [crc]}
    */

   protected void parseReplace() throws ParseException {
      if (this._args.length < 2)
         throw new ParseException(ERR_MSG_MISSING_ARGS,
               setError(CODE_MISSING_ARGUMENTS));

      //parse options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length < 2)
               throw new ParseException("Required arguments missing "
                     + "from options file at line " + (i + 1), setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5showtypes
    *  
    */
   protected void parseShowtypes() throws ParseException {
      //  a servergroup:filetype parameter can NOT be used
      //  in conjunction with the srvgroups option.
      if (this._cmdLineArguments.get(CMD.SERVERGROUPS) != null
            && (this._cmdLineArguments.get(CMD.SERVERGROUP) != null || this._cmdLineArguments
                  .get(CMD.FILETYPE) != null))
         throw new ParseException("Option \"" + CMD.SERVERGROUPS
               + "\" can not be " + "used in conjunction with a server group\n"
               + "or a filetype expression parameter.", setError(-1));

      if (this._cmdLineArguments.get(KEYWORD_FILES) != null) {
         throw new ParseException("Either there were too many arguments or "
               + "the \"classic\" option was\n" + "\t   supplied before the "
               + "<servergroup:filetype expression> specification.",
               setError(-1));
      }

   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5notify fei5notify [ &lt;server group:] &lt;file type&gt; 
    * {[output &lt;path&gt;] [restart] [using &lt;option file&gt;]} 
    * Option File Format: 
    * invoke &lt;command&gt; invokeExitOnError invokeAsync logFile &lt;file name&gt; 
    * mailMessageFrom &lt;email address&gt; mailMessageTo &lt;email address,
    * email address, ...&gt; mailReportAt &lt;hh:mm am|pm, hh:mm am|pm, ...&gt; 
    * mailReportTo &lt;email address, email address, ...&gt; mailSMTPHost &lt;host name&gt;
    */

   protected void parseNotify() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException("Missing filetype argument",
               setError(CODE_MISSING_ARGUMENTS));

      //parse options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length > 2)
               throw new ParseException("No more than two arguments expected "
                     + "from options file at line " + (i + 1), setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parsing for fei5subscribe fei5subscribe [ &lt;server group:] &lt;file type&gt;
    * {[output &lt;path&gt;] [restart] [using &lt;option file&gt;]} 
    * 
    * Option File Format: crc invoke &lt;command&gt;invokeExitOnError 
    * logFile &lt;file name&gt; mailMessageFrom
    * &lt;email address&gt; mailMessageTo &lt;email address, email address, ...&gt;
    * mailReportAt &lt;hh:mm am|pm, hh:mm am|pm, ...&gt; mailReportTo &lt;email address,
    * email address, ...&gt; mailSMTPHost &lt;host name&gt; receipt replace saferead
    * version
    */

   protected void parseSubscribe() throws ParseException {
      if (this._args.length < 1)
         throw new ParseException("Missing filetype argument",
               setError(CODE_MISSING_ARGUMENTS));
      
      //parse options file
      if (this._using) {
         for (int i = 0; i < this._optionsFileArgs.length; ++i) {
            if (this._optionsFileArgs[i].length > 2)
               throw new ParseException("No more than two arguments expected "
                     + "from options file at line " + (i + 1), setError(-1));
         }
      }
   }

   //---------------------------------------------------------------------

   /**
    * Parse the command line arguments.
    * 
    * @param args The command line arguments
    * @param argTable the argument lookup hashtable
    * @return boolean true if parsing was successful, false otherwise
    * @throws ParseException when parsing fails
    */

   protected boolean parseArgs(String[] args, Hashtable argTable)
         throws ParseException {
      GetOptLong getOpt = new GetOptLong(args, CMD.HELP + "|" + CMD.LONG + "|"
            + CMD.VERYLONG + "|" + CMD.VERSION + "|" + CMD.RESTART + "|"
            + CMD.REPLACE + "|" + CMD.SAFEREAD + "|" + CMD.AUTODELETE + "|"
            + CMD.CRC + "|" + CMD.DISPLAYMESSAGES + "|" + CMD.CLASSIC + "|"
            + CMD.SERVERGROUPS + "|" + CMD.LIMITMESSAGES + "|"
            + CMD.INVOKEEXITONERROR + "|" + CMD.RECEIPT + "|" 
            + CMD.PULL + "|" + CMD.PUSH + "|" + CMD.COMMENT
            + ":" + CMD.BEFORE + ":" + CMD.AFTER + ":" + CMD.BETWEEN + ":"
            + CMD.AND + ":" + CMD.USING + ":" + CMD.VFT + ":" + CMD.REFERENCE
            + ":" + CMD.OUTPUT + ":" + CMD.USER + ":" + CMD.PASSWORD + ":"
            + CMD.LOGFILE + ":" + CMD.LOGFILEROLLING + ":"
            + CMD.MAILMESSAGEFROM + ":" + CMD.MAILMESSAGETO + ":"
            + CMD.MAILSMTPHOST + ":" + CMD.MAILREPORTTO + ":"
            + CMD.MAILREPORTAT + ":" + CMD.MAILSILENTRECONN + "|" 
            + CMD.INVOKE + ":" + CMD.FOR + ":"
            + CMD.SERVERGROUP + ":" + CMD.SERVER + ":"+ CMD.FORMAT + ":"
            + CMD.INVOKEASYNC + "|" + CMD.QUERY + ":" 
            + CMD.REPLICATE + "|" + CMD.REPLICATEROOT + ":" + CMD.FORCE + "|"
            + CMD.OWNER + "|" + CMD.GROUP + "|" + CMD.DIFF + "|"  
            + CMD.FILEHANDLER + "|", 
            true, "");
      
      LinkedList files = new LinkedList();
      String str;

      try {

         while ((str = getOpt.nextArg()) != null) {             
            if (str.equalsIgnoreCase(CMD.HELP)) {
               this._printHelp = true;
               argTable.put(CMD.HELP, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.CLASSIC)) {
               argTable.put(CMD.CLASSIC, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.SERVERGROUPS)) {
               argTable.put(CMD.SERVERGROUPS, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.LONG)) {
               argTable.put(CMD.LONG, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.VERYLONG)) {
               argTable.put(CMD.VERYLONG, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.CRC)) {                
               argTable.put(CMD.CRC, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.VERSION)) {
               argTable.put(CMD.VERSION, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.RESTART)) {
               argTable.put(CMD.RESTART, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.DISPLAYMESSAGES)) {
               argTable.put(CMD.DISPLAYMESSAGES, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.LIMITMESSAGES)) {
               argTable.put(CMD.LIMITMESSAGES, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.SAFEREAD)) {
               argTable.put(CMD.SAFEREAD, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.AUTODELETE)) {
               argTable.put(CMD.AUTODELETE, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.INVOKEEXITONERROR)) {
               argTable.put(CMD.INVOKEEXITONERROR, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.INVOKEASYNC)) {
                argTable.put(CMD.INVOKEASYNC, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.RECEIPT)) {
               argTable.put(CMD.RECEIPT, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.PULL)) {
                argTable.put(CMD.PULL, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.PUSH)) {
                argTable.put(CMD.PUSH, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.USING)) {
               if (this._using) {
                  throw new ParseException("Illegal keyword '" + str
                        + "' in option file.", setError(-1));
               }
               this._using = true;
               this._optionsFile = getOpt.getArgValue();
               argTable.put(CMD.USING, this._optionsFile);
            } else if (str.equalsIgnoreCase(CMD.VFT)) {
               argTable.put(CMD.VFT, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.REFERENCE)) {
               argTable.put(CMD.REFERENCE, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.COMMENT)) {
               argTable.put(CMD.COMMENT, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.USER)) {
               argTable.put(CMD.USER, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.PASSWORD)) {
               argTable.put(CMD.PASSWORD, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.BEFORE)) {
               argTable.put(CMD.BEFORE, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.AFTER)) {
               argTable.put(CMD.AFTER, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.BETWEEN)) {
               argTable.put(CMD.BETWEEN, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.AND)) {
               argTable.put(CMD.AND, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.REPLACE)) {
               argTable.put(CMD.REPLACE, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.OUTPUT)) {
               String output = getOpt.getArgValue();
               argTable.put(CMD.OUTPUT, new File(output).getAbsolutePath());
            } else if (str.equalsIgnoreCase(CMD.LOGFILE)) {
               argTable.put(CMD.LOGFILE, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.LOGFILEROLLING)) {
               argTable.put(CMD.LOGFILEROLLING, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.MAILMESSAGEFROM)) {
               argTable.put(CMD.MAILMESSAGEFROM, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.MAILMESSAGETO)) {
               argTable.put(CMD.MAILMESSAGETO, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.MAILREPORTTO)) {
               argTable.put(CMD.MAILREPORTTO, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.MAILREPORTAT)) {
               argTable.put(CMD.MAILREPORTAT, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.MAILSMTPHOST)) {
               argTable.put(CMD.MAILSMTPHOST, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.MAILSILENTRECONN)) {
               argTable.put(CMD.MAILSILENTRECONN, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.INVOKE)) {
               argTable.put(CMD.INVOKE, getOpt.getArgValue());              
            } else if (str.equalsIgnoreCase(CMD.FOR)) {
               argTable.put(CMD.FOR, getOpt.getArgValue().toLowerCase());
            } else if (str.equalsIgnoreCase(CMD.SERVERGROUP)) {
               argTable.put(CMD.SERVERGROUP, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.SERVER)) {
               argTable.put(CMD.SERVER, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.FORMAT)) {
                argTable.put(CMD.FORMAT, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.QUERY)) {
                argTable.put(CMD.QUERY, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.REPLICATE)) {
                argTable.put(CMD.REPLICATE, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.REPLICATEROOT)) {
               argTable.put(CMD.REPLICATEROOT, getOpt.getArgValue());
            } else if (str.equalsIgnoreCase(CMD.FORCE)) {
                argTable.put(CMD.FORCE, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.OWNER)) {
                argTable.put(CMD.OWNER, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.GROUP)) {
                argTable.put(CMD.GROUP, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.DIFF)) {
                argTable.put(CMD.DIFF, Boolean.TRUE);
            } else if (str.equalsIgnoreCase(CMD.FILEHANDLER)) {
                argTable.put(CMD.FILEHANDLER, Boolean.TRUE);
            } else {
                
                if (_actionId.equals(Constants.CREDLOGIN))
                {
                    if (str.equalsIgnoreCase(args[0]))
                    {
                        argTable.put(CMD.USER, str);    
                    }
                    else if (args.length > 1 && str.equalsIgnoreCase(args[1])) 
                    {
                        argTable.put(CMD.SERVERGROUP, str);                            
                    }
                }
                else if (_actionId.equals(Constants.CREDLOGOUT))
                {
                    if (str.equalsIgnoreCase(args[0]))
                    {
                        argTable.put(CMD.SERVERGROUP, str);    
                    }
                }
                else if (str.equalsIgnoreCase(args[0])
                    && _actionId.equals(Constants.CHANGEPASSWORD))
                {
                    //if full filetype, then extract only the servergroup
                    String arg = args[0];
                    if (arg.indexOf(":") != -1)
                    {
                        String sg = FileType.extractServerGroup(arg);
                        //String ft = FileType.extractFiletype(arg);
                        
                        if (sg != null)
                            argTable.put(CMD.SERVERGROUP, sg);
                    }
                    else
                    {
                        argTable.put(CMD.SERVERGROUP,    arg);
                    }
                }  
                else if (str.equalsIgnoreCase(args[0])
                         && _actionId != Constants.COMPUTECHECKSUM) 
                {
                  // file type argument
                  // check to see if server group is specified
                  String arg = args[0];
                  if (arg.indexOf(":") != -1)
                  {
	                  String sg = FileType.extractServerGroup(arg);
	                  String ft = FileType.extractFiletype(arg);
	                  
	                  if (sg != null)
	                	  argTable.put(CMD.SERVERGROUP, sg);
	                  if (ft != null)
	                	  argTable.put(CMD.FILETYPE,    ft);
                  }
                  else
                  {
                	  argTable.put(CMD.FILETYPE,    arg);
                  }
                  
//                  // file type argument
//                  // check to see if server group is specified
//                  String arg = args[0];
//                  int index = arg.indexOf(':');
//                  if (index > 0) {
//                     String group = arg.substring(0, index);                     
//                     argTable.put(CMD.SERVERGROUP, group);
//                     
//                     //we support 'sg:' for all filetypes
//                     if (index < arg.length() - 1)
//                     {
//                         String type = arg.substring(arg.indexOf(':') + 1);
//                         argTable.put(CMD.FILETYPE, type);
//                     }
//                  } else {
//                     argTable.put(CMD.FILETYPE, arg);
//                  }
               } 
               else 
               {
                  files.add(str);
                  if (!files.isEmpty()) {
                     String[] filelist = new String[files.size()];
                     for (int i = 0; i < files.size(); ++i) {
                        filelist[i] = (String) files.get(i);
                     }
                     argTable.put(KEYWORD_FILES, filelist);
                  }
               } //end_else
            } //end_else
         } //end_while
      } catch (ParseException pEx) {
         throw pEx;
      } //end_try_catch

      return true;
   }

   //---------------------------------------------------------------------

   /**
    * Parses entries from an options file and populates the fileArguments list.
    * If actionId is subscription or notify, then contents of file are for a
    * single operation invocation, and thus the list will contain a single
    * entry. For all other operations, each line represents a separate
    * invocation, and so the list size should be equal to the number of
    * non-empty lines of an options file.
    * 
    * @param optionsFilename Path of the options file to be parsed
    * @throws ParseException if a parse-related error occurs
    */

   protected void parseOptionsFile(String optionsFilename)
         throws ParseException {
      File optionsFile = new File(optionsFilename);
      if (!optionsFile.canRead())
         throw new ParseException("Cannot read options file '"
               + optionsFilename + "'", setError(-1));

      List fileArgumentList = new ArrayList();

      Hashtable curArgTable = new Hashtable();

      //if action is auto, then there is only one iteration, so add now
      if (this._actionIsAuto) {
         this._logger.debug("subscription action");
         this._argumentList.add(curArgTable);
      }

      LineNumberReader reader = null;
      String line;

      try {
         //Security code review prompted this change to prevent DOS attack (nttoole 08.27.2013)
         //reader = new LineNumberReader(new FileReader(optionsFile));
         reader = new LineNumberReader(new BoundedBufferedReader(
                                    new FileReader(optionsFile)));

         while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.equals(""))
               continue;

            String[] args = buildArgs(line, reader.getLineNumber(),
                                      this._actionIsAuto);

            if (args == null)
               continue;
            else
               fileArgumentList.add(args);

            //parse the args            
            parseArgs(args, curArgTable);

            //not auto, so add and create new map
            if (!this._actionIsAuto) {
               this._argumentList.add(curArgTable);
               curArgTable = new Hashtable();
            }
         }
         reader.close();
      } catch (ParseException e) {
         int currentLine = (reader == null ? -1 : reader.getLineNumber());
         throw new ParseException(e.getMessage() + " Line " + currentLine,
               setError(currentLine));
      } catch (IOException ioEx) {
         throw new ParseException(ioEx.getMessage(), setError(-1));
      }

      //perform consistency checks
      for (int i = 0; i < this._argumentList.size(); ++i) {
         curArgTable = (Hashtable) this._argumentList.get(i);
         checkConsistency(curArgTable, true);
      }

      //create argument line record (stores per-line args)
      int numLines = fileArgumentList.size();
      this._optionsFileArgs = new String[numLines][];
      for (int i = 0; i < numLines; ++i)
         this._optionsFileArgs[i] = (String[]) fileArgumentList.get(i);
   }

   //---------------------------------------------------------------------

   /**
    * Method to parse a line read in from the option file to create a tokenized
    * array of strings.
    * 
    * @param line the input line string
    * @return the tokenized array of strings
    * @throws ParseException when invalid syntax is encountered
    */

   protected String[] buildArgs(String line) throws ParseException {
      return this.buildArgs(line, -1, false);
   }

   //---------------------------------------------------------------------

   /**
    * Method to parse a line read in from the option file to create a tokenized
    * array of strings.
    * 
    * @param line the input line string
    * @param lineNo Line number of string, -1 if from command line
    * @param subscription subscription flag, since subscription option file has
    *           a different way of parsing
    * @return the tokenized array of strings
    * @throws ParseException when invalid syntax is encountered
    */

   private String[] buildArgs(String line, int lineNo, boolean subscription)
         throws ParseException {
      //if subscription, then wrap value with quotes if not already
      if (subscription) {
         StringBuffer sb = new StringBuffer(line.trim());
         if (line.indexOf("\"") == -1 && line.indexOf("'") == -1
               && sb.indexOf(" ") > -1) {
            sb.insert(line.indexOf(' ') + 1, '\"');
            sb.append("\"");
            line = sb.toString();
            this._logger.debug("got here |" + line + "|");
         }
      }

      //-------------------------

      //construct/initialize tokenizer
      StreamTokenizer tokens = new StreamTokenizer(new StringReader(line));
      
      tokens.resetSyntax();
      tokens.wordChars('a', 'z');
      tokens.wordChars('A', 'Z');
      tokens.wordChars('0', '9');
      tokens.wordChars('!', '~');
      tokens.wordChars(128+32, 255);
      tokens.whitespaceChars(0, ' ');
      tokens.commentChar('#');
      tokens.quoteChar('"');
      tokens.quoteChar('\'');

      List list = new LinkedList();
      String[] returnVal;

      //go through the tokens, adding to the list
      try {
         while (tokens.nextToken() != StreamTokenizer.TT_EOF
               && tokens.ttype != StreamTokenizer.TT_EOL) {
            String token = tokens.sval;             
            if (token != null)
            {
                if (_shouldTrimTokens)
                    token = token.trim();
                list.add(token);
            }
         }
      } catch (IOException ioEx) {
         throw new ParseException("Could not parse: " + line, setError(lineNo));
      }

      //convert list to string array
      int size = list.size();
      returnVal = (size > 0) ? new String[size] : null;    
      for (int i = 0; i < size; ++i) {
         returnVal[i] = (String) list.get(i);
      }

      //return array
      return returnVal;
   }

   //---------------------------------------------------------------------

   /**
    * Translates from string representation of time to Date object.
    * 
    * @param time Time as string, format hh:mm
    * @param pm flag indicating time is in PM
    * @return Date object represented by parameters
    */

   public static Date getDate(String time, boolean pm) {
      Date now = new Date();

      String[] tlist = time.split(":");
      int hour = Integer.parseInt(tlist[0].trim());
      int minute = Integer.parseInt(tlist[1].trim());

      //if 12 am, convert hour to 0
      if (hour == 12 && !pm)
      {
          hour = 0;
      }
      else if (pm)
      {
          //if pm, then add 12 hours
         hour += 12;
      }
      //if (pm)
      //   hour += 12;

      Calendar cal = Calendar.getInstance();
      cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal
            .get(Calendar.DAY_OF_MONTH), hour, minute);

      Date then = cal.getTime();

      if (then.before(now)) {
         long ll = then.getTime() + 86400000;
         then = new Date(ll);
      }
      return then;
   }

   //---------------------------------------------------------------------

   /**
    * Returns an array of Date objects created from the timelist parameter.
    * 
    * @param timelist String of date list delimited by comma (,), where each
    *           entry is of the form 'hh:mm [ap]m'
    * @return Array of Date corresponding to the timelist
    */

   public static Date[] parseTimeList(String timelist) {
      timelist = timelist.toLowerCase();

      StringTokenizer st = new StringTokenizer(timelist, ",");
      Date[] dateList = new Date[st.countTokens()];
      int i = 0;
      while (st.hasMoreTokens()) {
         String time = st.nextToken();
         boolean pm = false;
         if (time.endsWith("pm"))
            pm = true;
         if (time.endsWith("am") || pm) {
            time = time.substring(0, time.indexOf('m') - 1).trim();
            Date d = getDate(time, pm);
            if (d != null) {
               dateList[i++] = d;
            }
         }
      }
      return dateList;
   }

   //---------------------------------------------------------------------

   /**
    * Returns array of filenames retrieved from standard input. Filenames are
    * expected to be placed one per line. If unable to retrieve this
    * information, the null is returned.
    * 
    * @return String array of filenames if successful, else null.  If no
    * filenames were available, a String array of length 0 is returned.
    */

   private String[] getFileListFromSTDIN() {
      String[] list = null;

      try {
         
         ArrayList alist = new ArrayList();

         //Security code review prompted this change to prevent DOS attack (nttoole 08.27.2013)
         //LineNumberReader reader = new LineNumberReader(new InputStreamReader(
         //                                                 System.in));
         LineNumberReader reader = new LineNumberReader(
                                     new BoundedBufferedReader(
                                        new InputStreamReader(System.in)));
         
         String line = null;
         while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0)
               continue;

            //check for spaces
            if (line.indexOf(" ") != -1 || line.indexOf("\t") != -1) {
               this._logger.error(ERROR_TAG + "Invalid input stream format \""
                     + line + "\"");
               return null;
            }

            //check length
            if (line.length() > MAX_FILENAME_LENGTH) {
               this._logger.error(ERROR_TAG + "Filename too long.  Max = "
                     + MAX_FILENAME_LENGTH + ", Length = " + line.length()
                     + ": \"" + line + "\"");
               return null;
            }

            //if pattern not constructed, then construct it
            if (this._printablePattern == null) {
               this._printablePattern = Pattern.compile(_printableRegex);
            }

            //check for illegal characters
            if (!this._printablePattern.matcher(line).matches()) {
               this._logger.error(ERROR_TAG + "Invalid input stream format, "
                     + "illegal characters in filename: \"" + line + "\"");
               return null;
            }

            alist.add(line);
         }

         if (alist.size() == 0)
         {
            return new String[0];
         }
         list = new String[alist.size()];
         for (int i = 0; i < alist.size(); ++i) {
            list[i] = (String) alist.get(i);
         }
      } catch (IOException e) {
         this._logger.error(ERROR_TAG + "Unable to read from standard-in.");
         return null;
      }
      return list;
   }

   //---------------------------------------------------------------------

   /**
    * Testing main method. TODO - make private for distributions.
    */

   private static void main(String[] args) {
      String opId = Constants.ADDFILE;

      UtilCmdParser parser = new UtilCmdParser();
      try {
         parser.parse(opId, args);
         System.out.println(parser.toString());
      } catch (ParseException pEx) {
         pEx.printStackTrace();
      }
   }

   //---------------------------------------------------------------------

   /**
    * Override of toString method. Prints the super.toString() followed by the
    * contents of the parse mapping.
    * 
    * @return String representation of this instance
    */

   public String toString() {
      StringBuffer buffer = new StringBuffer(super.toString());

      int iterations = iterations();
      String message;
      int count = 0;

      while (true) {
         count++;
         Map map = this.getCurrentArguments();
         if (map == null) {
            buffer.append("\nNull mapping.");
            break;
         }

         Iterator it = map.keySet().iterator();
         buffer.append("\n> Argument list #" + count);
         while (it.hasNext()) {
            String key = (String) it.next();
            message = ">> " + key;
            Object value = this.getValue(key);
            if (value != null) {
               if (!(value instanceof Object[]))
                  message += " = " + value;
               else {
                  message += " = ";
                  Object[] array = (Object[]) value;
                  for (int i = 0; i < array.length; ++i)
                     message += "(" + i + "," + array[i] + ") ";
               }
            }
            buffer.append("\n").append(message);
         }
         if (this.hasNext())
            this.advance();
         else
            break;
      }

      buffer.append("\n");
      return buffer.toString();
   }

   //---------------------------------------------------------------------

   /**
    * Helper method that sets the error flag and returns parameter value back.
    * 
    * @param lineNumber Line number associated with error
    * @return lineNumber value
    */

   protected int setError(int lineNumber) {
      this._error = true;
      this._printHelp = true;
      return lineNumber;
   }

   //---------------------------------------------------------------------

}