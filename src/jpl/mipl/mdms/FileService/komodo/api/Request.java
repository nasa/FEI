/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/

package jpl.mipl.mdms.FileService.komodo.api;

import jpl.mipl.mdms.FileService.komodo.api.ClientRestartCache;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;

import java.io.OutputStream;
import java.util.Date;

/**
 * Request class maintains information about file transactions requests This is
 * an internal package class. Not part of the interface.
 * 
 * @author Jeff Jacobson, G. Turek
 * @version $Id: Request.java,v 1.15 2013/03/30 00:06:20 ntt Exp $
 */
class Request {

   //Request parameters
   private String _command;
   private char _modifier = Constants.NOMODIFIER;
   private String[] _fileNames;
   private String[] _cmdArgs;
   private Date[] _dates;
   private String _regexp;
   private String _comment;
   private String _fileType;
   private String _serverGroup;
   private String _directory;
   private String _replicationRoot;
   
   //private RestartInfo _restartInfo = null;
   // If we must persist last file info.
   private ClientRestartCache _restartCache = null;
   private byte[] _fileBuffer;
   private long _fileBufferLength;
   private String _linkdir;
   private String _vft;
   private OutputStream _out;

   //Transaction parameters
   private int _transactionId = -1;
   private int _options;
   private boolean _inMem = false;

   // = USER request constructors

   /**
    * Constructor used to close file type connection <br>
    * The command is internally set to QUIT.
    * 
    * @param _serverGroup the server group name
    * @param fileType the file's type
    */
   Request() {
      this._serverGroup = null;
      this._fileType = null;
      this._command = Constants.QUIT;
   }
   
   /**
    * Constructor used to close file type connection <br>
    * The command is internally set to QUIT.
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    */
   Request(String serverGroup, String fileType) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._command = Constants.QUIT;
   }

   /**
    * Constructor for regular expression requests <br>
    * The command modifier is internally set to REGEXP.
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param directory the client's current working directory
    * @param command the command's shortcut (a, g, s, n,....)
    * @param regexp a regular expression
    */
   Request(String serverGroup, String fileType, String directory, String command,
         String regexp) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._directory = directory;
      this._command = command;
      this._regexp = regexp;
      this._comment = null;
      this._modifier = Constants.REGEXP;
   }

   /**
    * Constructor for regular expression requests, where dates are used to
    * further discriminate the selected set. Note: An empty Data array implies
    * latest. Otherwise one Date means "since", and two means "between". <br>
    * the command modifier is internally set to REGEXP.
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param directory the client's current working directory
    * @param command the command's shortcut (a, g, s, n,....)
    * @param modifier the command modifier (e.g. regular expression, latest)
    * @param regexp a regular expression
    * @param dates the list of date filters
    */
   Request(String serverGroup, String fileType, String directory, String command,
         char modifier, String regexp, Date[] dates) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._directory = directory;
      this._command = command;
      this._regexp = regexp;
      this._comment = null;
      this._dates = dates;
      //if (dates.length > 0)
      //   this._modifier = Constants.REGEXP;
      //else
      this._modifier = modifier;
   }

   /**
    * Constructor for regular expression requests where comments are sent to the
    * server. The command modifier is internally set to REGEXP;
    * 
    * @param serverGroup - the server group name
    * @param type - the file's type
    * @param directory - the client's current working directory
    * @param command - the command's shortcut (a, g, s, n,....)
    * @param regexp - a regular expression
    * @param comment - a comment to associate with a file set
    */
   Request(String serverGroup, String type, String directory, String command,
         String regexp, String comment) {

      this._serverGroup = serverGroup;
      this._fileType = type;
      this._directory = directory;
      this._command = command;
      this._regexp = regexp;
      this._comment = comment;
      this._modifier = Constants.REGEXP;
   }

   /**
    * Constructor for since/between dates requests <br>
    * The command modifier is internally set to DATETIME depending on size of
    * dates array
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param directory the client's current working directory
    * @param command the command's shortcut (a, d, n,....)
    * @param dates array of Date objects (size = 1 for 'since', 2 for 'between'
    *           cases)
    */
   Request(String serverGroup, String fileType, String directory, String command,
         Date[] dates) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._directory = directory;
      this._command = command;
      this._dates = dates;
      this._modifier = Constants.DATETIME;
   }

   /**
    * Constructor for by name requests, if fileNames is null, the command is
    * equivalent to imply "all" for given type <br>
    * The command modifier is internally set to FILENAMES.
    * 
    * @param serverGroup the server group
    * @param fileType the file's type
    * @param directory the client's current working directory
    * @param command the command's shortcut (a, d, n,....)
    * @param fileNames a array of file names
    */
   Request(String serverGroup, String fileType, String directory, String command,
         String[] fileNames) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._directory = directory;
      this._command = command;
      this._fileNames = fileNames;
      this._comment = null;
      this._modifier = Constants.FILENAMES;
   }

   /**
    * Constructor for by name requests, if fileNames is null, the command is
    * equivalent to imply "all" for given type <br>
    * The command modifier is internally set to FILENAMES.
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param out the output stream to return the data on
    * @param command the command's shortcut (a, d, n,....)
    * @param fileNames a array of file names
    */
   Request(String serverGroup, String fileType, OutputStream out, String command,
         String[] fileNames) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._out = out;
      this._command = command;
      this._fileNames = fileNames;
      this._comment = null;
      this._modifier = Constants.FILENAMES;
   }

   /**
    * Constructor for by name requests, if fileNames is null, the command is
    * equivalent to imply "all" for given type The command modifier is
    * internally set to FILENAMES
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param directory the client's current working directory
    * @param command the command's shortcut (a, d, n,....)
    * @param fileNames a array of file names
    * @param comment a comment to associate with a file set
    */
   Request(String serverGroup, String fileType, String directory, String command,
         String[] fileNames, String comment) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._directory = directory;
      this._command = command;
      this._fileNames = fileNames;
      this._comment = comment;
      this._modifier = Constants.FILENAMES;
   }

   /**
    * Constructor for by in-memory add/replace requests
    * 
    * @param serverGroup the server group name
    * @param fileType the file's type
    * @param command the command's string
    * @param fileNames the file names for Komodo registration
    * @param fileBuffer in-memory file contents.
    * @param fileBufferLength length of file in fileBuffer
    * @param comment an optional comment to associate with a file set
    */
   Request(String serverGroup, String fileType, String command,
         String[] fileNames, byte[] fileBuffer, long fileBufferLength,
         String comment) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._command = command;
      this._fileNames = fileNames;
      this._fileBuffer = fileBuffer;
      this._fileBufferLength = fileBufferLength;
      this._comment = comment;
      this._modifier = Constants.MEMTRANSFER;
   }

   /**
    * Constructor for file type requests with no arguments. Command modifer can
    * be set.
    * 
    * @param serverGroup the server group name
    * @param fileType the file type
    * @param command the request command
    * @param modifier the modifier value
    */
   Request(String serverGroup, String fileType, String command, char modifier) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._command = command;
      this._modifier = modifier;
   }

   /**
    * Constructor for add a file and ref requests
    * 
    * @param serverGroup the server group name
    * @param fileType the file type
    * @param directory the file directory
    * @param command the request command
    * @param regexp the regular expression
    * @param vft the virtual file type
    * @param linkdir the link directory
    */
   Request(String serverGroup, String fileType, String directory, String command,
         String regexp, String vft, String linkdir) {
      this._serverGroup = serverGroup;
      this._fileType = fileType;
      this._directory = directory;
      this._command = command;
      this._regexp = regexp;
      this._vft = vft;
      this._linkdir = linkdir;
   }

   // = ADMIN request constructors

   /**
    * Constructor for admin create file type request.
    * 
    * @param command the command's shortcut
    * @param cmdArgs array of command arguments
    */
   Request(String command, String[] cmdArgs) {

      this._command = command;
      this._cmdArgs = cmdArgs;
   }

   /**
    * Constructor for sync admin requests
    * 
    * @param command the command's shortcut
    * @param cmdArgs the array of command arguments
    * @param dates the array of date
    */
   Request(String command, String[] cmdArgs, Date[] dates) {

      this._command = command;
      this._cmdArgs = cmdArgs;
      this._dates = dates;
   }

   /**
    * Constructor for admin requests
    * 
    * @param command the command shortcut
    */
   Request(String command) {

      this._command = command;
      this._modifier = Constants.FILENAMES;
   }

   // = METHODS

   /**
    * Method to set in-memory flag for a get request.
    * 
    * @param value the flag value
    * @return the old in-memory flag value
    */
   boolean setInMem(boolean value) {

      boolean oldValue = this._inMem;
      this._inMem = value;
      return oldValue;
   }

   /**
    * Accessor method to get in-memory flag.
    * 
    * @return the current in-memroy flag value
    */
   final boolean getInMem() {

      return this._inMem;
   }

   /**
    * Accessor method to get command
    * 
    * @return command shortcut (a, l, n,...)
    */
   final String getCommand() {

      return this._command;
   }

   /**
    * Method to get command modifier
    * 
    * @return command modifier (0 = none, x = regexp, s = since, b = between, l =
    *         latest for regexp, a = at)
    */
   final char getModifier() {

      return this._modifier;
   }

   /**
    * Method to set command modifier
    * 
    * @param modifier the command modifier.
    */
   final void setModifier(char modifier) {

      this._modifier = modifier;
   }

   /**
    * Method to get client's working directory (for add/get), also used for
    * create file type.
    * 
    * @return client's current working directory
    */
   final String getDirectory() {

      return this._directory;
   }

   /**
    * Method to set directory
    * 
    * @param directory the directory a command should for getting files.
    */
   final void setDirectory(String directory) {

      this._directory = directory;
   }

   /**
    * Method to get client's comment (for add/replace/comment)
    * 
    * @return client's comment to associate with files.
    */
   final String getComment() {

      return this._comment;
   }

   /**
    * Method to get regular expression
    * 
    * @return string representation of regular expression
    */
   final String getRegExp() {

      return this._regexp;
   }

   /**
    * Method to get dates for 'since' and 'between' modified commands
    * 
    * @return array of Dates objects for 'since' and 'between' modified commands
    */
   final Date[] getDates() {

      return this._dates;
   }

   /**
    * Method to get array of file names command is operating on
    * 
    * @return array of file names command is operating on
    */
   final String[] getFileNames() {

      return this._fileNames;
   }

   /**
    * Method to get array of command arguments
    * 
    * @return array of command arguments
    */
   final String[] getCommandArgs() {

      return this._cmdArgs;
   }

   /**
    * Method to get file buffer.
    * 
    * @return byte array containing file.
    */
   final byte[] getFileBuffer() {

      return this._fileBuffer;
   }

   /**
    * Method to get the length of the file buffer.
    * 
    * @return amount of data in file.
    */
   final long getFileBufferLength() {

      return this._fileBufferLength;
   }

   /**
    * Method to get array of file names command is operating on
    * 
    * @param fileNames the list of file names
    */
   final void setFileNames(String[] fileNames) {

      this._fileNames = fileNames;
   }

   /**
    * Method to set transaction id.
    * 
    * @param transactionId the transaction Id
    */
   final void setTransactionId(int transactionId) {

      this._transactionId = transactionId;
   }

   final void setClientRestartCache(ClientRestartCache restartCache) {
      this._restartCache = restartCache;
   }

   /**
    * Method to perform client restart cache object checkout. Once the object is
    * returned, the internal cache object reference is set to null. The caller
    * must call setClientRestartCache to return the object to this class.
    * 
    * @return the client restart cache object reference.
    */
   final synchronized ClientRestartCache getClientRestartCache() {
      return this._restartCache;
      /**
       * ClientRestartCache restartCache = this._restartCache;
       * this._restartCache = null; return restartCache;
       */
   }

   /**
    * Method to tell api to keep track of last file found.
    * 
    * @param restartInfo The restart info object
    * 
    * final void setRestartInfo(RestartInfo restartInfo) {
    * 
    * this._restartInfo = restartInfo; }
    */

   /**
    * Method to get the restart file.
    * 
    * @return the restart file name final RestartInfo getRestartInfo() {
    * 
    * return this._restartInfo; }
    */

   /**
    * Method to get the link for an addFileAndRef command.
    * 
    * @return the link directory
    */
   final String getLinkDir() {

      return this._linkdir;
   }

   /**
    * Method to get the vft name for an addFileAndRef command.
    * 
    * @return the vft name
    */
   final String getVFT() {

      return this._vft;
   }

   /**
    * Method to get transaction id
    * 
    * @return the transaction id of the command. If this returns -1, and error
    *         has occurred.
    */
   final int getTransactionId() {

      return this._transactionId;
   }

   /**
    * Method to set transaction option.
    * 
    * @param options the options mask
    */
   final void setOptions(int options) {

      this._options = options;
   }

   /**
    * Method to return true if the input option is enabled in the options mask
    * 
    * @param option the option value
    * @return true if the option is enabled in the options mask
    */
   boolean getOption(int option) {

      return (this._options & option) > 0 ? true : false;
   }

   /**
    * Method to get the options mask
    * 
    * @return all currently set options.
    */
   int getOptions() {

      return (this._options);
   }

   /**
    * Method to set the file type
    * 
    * @param fileType file type associated with this transaction
    */
   final void setType(String fileType) {

      this._fileType = fileType;
   }

   /**
    * Method to get the file type name
    * 
    * @return file type associated with this transaction
    */
   final String getType() {

      return this._fileType;
   }

   /**
    * Method to set server group name
    * 
    * @param serverGroup the server group name
    */
   final void setServerGroup(String serverGroup) {
      this._serverGroup = serverGroup;
   }

   /**
    * Method to return the server grou pname
    * 
    * @return the server group name
    */
   final String getServerGroup() {
      return this._serverGroup;
   }

   /**
    * Method to set replication root directory path
    * 
    * @param rootPath the replication root path
    */
   final void setReplicationRoot(String rootPath) {
      this._replicationRoot = rootPath;
   }
   
   /**
    * Method to return the replication root directory path
    * 
    * @return the replication root directory path
    */
   final String getReplicationRoot() {
      return this._replicationRoot;
   }
   
   /**
    * Accessor method to get the output stream
    * 
    * @return the output stream
    */
   final OutputStream getOutputStream() {

      return this._out;
   }

   /**
    * General command marshalling method.
    * 
    * @return entire command string ready to ship to server
    */
   final String getCommandString() {

      StringBuffer sb = new StringBuffer();
      sb.append(this._command);
      if (this._modifier != Constants.NOMODIFIER)
         sb.append(this._modifier);
      if (this._cmdArgs != null) {
         for (int i = 0; i < this._cmdArgs.length; i++) {
            if (this._cmdArgs[i] == null)
               sb.append(" null");
            else
               sb.append(" " + this._cmdArgs[i]);
         }
      }
      if (this._dates != null) {
         for (int i = 0; i < this._dates.length; i++) {
            if (this._dates[i] != null)
               sb.append(" " + DateTimeUtil.getDateDBString(this._dates[i]));
         }
      }
      return sb.toString();
   }
}