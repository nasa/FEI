/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.services.query.api;

import java.text.ParseException;
import java.util.Date;

import jpl.mipl.mdms.FileService.komodo.api.ClientRestartCache;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.FileService.util.DateTimeUtil;
import jpl.mipl.mdms.FileService.util.FileUtil;

/**
 * 
 * Query result class maintains information about individual files.
 * 
 * @author Nicholas T Toole (nicholas.t.toole@jpl.nasa.gov)
 * @version $Id: QResult.java,v 1.5 2015/01/30 02:05:45 awt Exp $
 */

public class QResult 
{
    
   private QRequest _request = null;
   private long _fileId = Constants.NOT_SET;
   private String _timeStamp;
   private Date _fileCreated;
   private String _fileContributor;
   private String _fileName;
   private String _location;
   private long _size = Constants.NOT_SET;
   private Date _fileModified;
   private int _ftId = Constants.NOT_SET;
   private int _receiptId = Constants.NOT_SET;
   private String _qaRelease;
   private byte[] _checksum;
   private byte[] _fileBuffer;
   private String _note;
   private String _message;
   private String _comment;
   private String _archiveNote;
   private ClientRestartCache _restartCache = null;
   //private RestartInfo _restartInfo;

   private String _fileType;
   private String _serverGroup;
   private int    _transactionId;
   
   
   /* Additional fields to support VFT reference reports */
   private String _vftName;
   private String _refName;
   private String _refLink;
   private String _updateFileType;
   private String _updateFileName;
   private boolean _updateRef;
   private String _refFileName;
   private String _refFileType;

   /* Additional fields to support VFT reports */
   private String _title;
   private String _createdBy;
   private String _created;
   private String _updatedBy;
   private String _updated;
   private int _errno = Constants.OK;
   private boolean _eot = false; // End Of Transaction.

   // Some commands do not want to bother the application with a final profile
   private boolean _doNotShowUser = false;

   /**
    * Constructor
    * 
    * @param name the file's name.
    * @param size the size of the file in bytes.
    */
   public QResult(String name, long size) {
      this(null, name, null, size, null);
   }

   /**
    * Constructor
    * 
    * @param request the request information, includes, command, type,
    *           transaction id.
    * @param name the file's name.
    */
   public QResult(QRequest request, String name) {
      this._request = request;
      this._fileName = name;
      this._timeStamp = DateTimeUtil.getTimestamp();
      if (this._request != null)
      {
          this._fileType      = "FILETYPE??";
          this._serverGroup   = this._request.getServerGroup();
          this._transactionId = this._request.getTransactionId();
      }
   }

   /**
    * Constructor
    * 
    * @param request the request information, includes type, transaction id.
    * @param name the file's name.
    * @param size the size of the file in bytes.
    */
   public QResult(QRequest request, String name, long size) {
      this(request, name, null, size, null);
   }

   /**
    * Constructor
    * 
    * @param request the request information, includes type, transaction id.
    * @param name the file's name.
    * @param filetype the file's FEI filetype.
    * @param size the size of the file in bytes.
    * @param datetime the file's modification date.
    */
   public QResult(QRequest request, String name, String filetype, long size, Date datetime) 
   {
      this(request, name);      
      this._size = size;
      this._fileType = filetype;
      this._fileModified = datetime;
   }

   /**
    * Constructor
    * 
    * @param request the request information, includes type, transaction id.
    * @param errno the error status for this file.
    * @param message an error message.
    */
   public QResult(QRequest request, int errno, String message) {
      this(request, null);
      this._errno = errno;
      this._message = message.trim();
   }

   /**
    * Constructor
    * 
    * @param ftId the internal DBMS id for the file type.
    * @param name the file's name.
    * @param contributor user name of the file's contributor.
    * @param size the file's size in bytes.
    * @param qaRelease the date the file becomes available to the public?
    * @param checksum the file's checksum.
    * @param note a note, or message associated with the file.
    * @param serverGroup
    * @param fileType file type 
    * @param transId transaction id
    */
   public QResult(int ftId, String name, String contributor, long size,
                 String qaRelease, byte[] checksum, String note, 
                 String serverGroup, String fileType, int transId) {
      this._ftId = ftId;
      this._fileName = name;
      this._fileContributor = contributor;
      this._size = size;
      this._qaRelease = qaRelease;
      this._checksum = checksum;
      this._note = note;
      this._fileModified = null;
      this._timeStamp = DateTimeUtil.getTimestamp();
      this._fileType = fileType;
      this._serverGroup = serverGroup;
      this._transactionId = transId;
   }
   
   /**
    * Constructor
    * 
    * @param ftId the internal DBMS id for the file type.
    * @param name the file's name.
    * @param contributor user name of the file's contributor.
    * @param size the file's size in bytes.
    * @param qaRelease the date the file becomes available to the public?
    * @param checksum the file's checksum.
    * @param note a note, or message associated with the file.
    */
   public QResult(int ftId, String name, String contributor, long size,
                 String qaRelease, byte[] checksum, String note) {
       this(ftId, name, contributor, size, qaRelease,
            checksum, note, null, null, -1);      
   }

   /**
    * Constructor for vft reference information.
    * 
    * @param vft the virtual file type name
    * @param refName the reference name
    * @param refLink the reference link
    * @param refFileType the reference file type
    * @param refFileName the reference file name
    * @param comment the reference comment
    * @param updateFileType the update reference file type
    * @param updateFileName the update reference file name
    * @param updateRef the update reference flag. Is an update pending?
    */
   public QResult(String vft, String refName, String refLink,
                 String refFileType, String refFileName, String comment,
                 String updateFileType, String updateFileName, boolean updateRef) {

      this._vftName = vft;
      this._refName = refName;
      this._refLink = refLink;
      this._refFileType = refFileType;
      this._refFileName = refFileName;
      this._comment = comment;
      this._updateFileType = updateFileType;
      this._updateFileName = updateFileName;
      this._updateRef = updateRef;
      this._timeStamp = DateTimeUtil.getTimestamp();
   }

   /**
    * Constructor for vft information. Used for both current vfts and vft
    * history.
    * 
    * @param vft the virtual file type name.
    * @param title the title of the virtual file type.
    * @param comment the comment assoicated with the last update.
    * @param createdBy the agent that created the vft
    * @param created the time vft was created
    * @param updatedBy the agent that updated the vft.
    * @param updated the time of update.
    */
   public QResult(String vft, String title, String comment, String createdBy,
                 String created, String updatedBy, String updated) {
      this._vftName = vft;
      this._comment = comment;
      this._title = title;
      this._createdBy = createdBy;
      this._created = created;
      this._updatedBy = updatedBy;
      this._updated = updated;
      this._timeStamp = DateTimeUtil.getTimestamp();
   }

   /**
    * Need result to show eot, but don't show user. This is package protected,
    * for internal use only.
    */
   public void setDoNotShowUser() {
      this._doNotShowUser = true;
   }

   /**
    * Method to return the flag that determins if the application see this last
    * result on an end of transaction. This is package protected, for internal
    * use only.
    * 
    * @return true if to hide result
    */
   public final boolean isDoNotShowUser() {
      return this._doNotShowUser;
   }

   /**
    * Method to set message
    * 
    * @param message associated with this transaction
    */
   public void setMessage(String message) {
      this._message = message.trim();
   }

   /**
    * Method to set restart file
    * 
    * @param restartInfo the restart file name for commit.
    */
   //void setRestartInfo(RestartInfo restartInfo) {
   //    this._restartInfo = restartInfo;
   //}
   public void setClientRestartCache(ClientRestartCache restartCache) {
      this._restartCache = restartCache;
   }

   /**
    * Method to return message with time stamp
    * 
    * @return message associated with this transaction
    */
   public final String getMessage() {
      //return "[" + this._timeStamp + "]" + this._message;
      return this._message;
   }

   /**
    * Method to set comment
    * 
    * @param comment the comment associated with this file
    */
   public void setComment(String comment) {
      this._comment = comment;
   }

   /**
    * Method to return the comment
    * 
    * @return comment associated with this file
    */
   public final String getComment() {
      return this._comment;
   }

   /**
    * Method to set the title.
    * 
    * @param title the title associated with this file
    */
   public void setTitle(String title) {
      //this._title = _title.trim ();
      this._title = title.trim();
   }

   /**
    * Method to return the title
    * 
    * @return comment associated with this file
    */
   public final String getTitle() {
      return this._title;
   }

   /**
    * Method to set receipt ID
    * 
    * @param receiptId the receipt id associated with this file
    */
   public void setReceiptId(int receiptId) {
      this._receiptId = receiptId;
   }

   /**
    * Method to return the receipt ID
    * 
    * @return receipt id associated with this file
    */
   public final int getReceiptId() {
      return this._receiptId;
   }

   /**
    * Method to set the archive note
    * 
    * @param archiveNote the archive note for this file.
    */
   public void setArchiveNote(String archiveNote) {
      this._archiveNote = archiveNote;
   }

   /**
    * Method to return the archive note
    * 
    * @return archive note for this file.
    */
   public final String getArchiveNote() {
      return this._archiveNote;
   }

   /**
    * Method to set result errno
    * 
    * @param errno the new errno. 0 is OK.
    */
   public final void setErrno(int errno) {
      this._errno = errno;
   }

   /**
    * Method to get errno for this result
    * 
    * @return the errno of a result.
    */
   public final int getErrno() {
      return this._errno;
   }

   /**
    * Method to enable end of transaction flag. This allows the Komodo to
    * decrement the transaction count.
    */
   public void setEoT() {
      this._eot = true;
   }

   /**
    * Method to return the request file type
    * 
    * @return file type associated with this transaction
    */
   public String getType() {
       return this._fileType;
   }

   
   /**
    * Method to set the request file type.
    * 
    * @param fileType filetype associated with this transaction
    */
   public void setType(String fileType) {
       this._fileType = fileType;
   }
   
   /**
    * Method to return the request server group
    * 
    * @return the server group associated with this transaction
    */
   public String getServerGroup() {
       return this._serverGroup;
   }

   /**
    * Is this the last profile record returned for the request. This allows the
    * Komodo to decrement the transaction count when the user dequeues a result.
    * Not for use by the application.
    * 
    * @return true if eof of transaction is enabled
    */
   public final boolean isEoT() {
      return this._eot;
   }

   /**
    * Method to get transaction id
    * 
    * @return the transaction id of the request. If this returns -1, and error
    *         has occurred.
    */
   public int getTransactionId() {
       return this._transactionId;
   }

   /**
    * Method to get vft name
    * 
    * @return the vft name
    */
   public final String getVFTName() {
      return this._vftName;
   }

   /**
    * Method to get vft the name of the agent that created the VFT.
    * 
    * @return the vft creator name
    */
   public final String getCreatedBy() {
      return this._createdBy;
   }

   /**
    * Method to get file/vft create time
    * 
    * @return the file/vft created time
    */
   public final String getCreated() {
      return this._created;
   }

   /**
    * Method to set file/vft createt time
    * 
    * @param created the file/vft create time
    */
   public final void setCreated(String created) {
      this._created = created;
   }

   /**
    * Method to get the name of the agent that updated the file/VFT.
    * 
    * @return the agent that updated the vft
    */
   public final String getUpdatedBy() {
      return this._updatedBy;
   }

   /**
    * Method to set the name of the agent that updated the file/vft
    * 
    * @param updatedBy the agent's name
    */
   public final void setUpdatedBy(String updatedBy) {
      this._updatedBy = updatedBy;
   }

   /**
    * Method to get vft update time
    * 
    * @return the vft created time
    */
   public final String getUpdated() {
      return this._updated;
   }

   /**
    * Method to get vft reference name
    * 
    * @return the vft reference name
    */
   public final String getRefName() {
      return this._refName;
   }

   /**
    * Method to set vft reference name
    * 
    * @param refName the reference name string.
    *  
    */
   public final void setRefName(String refName) {
      this._refName = refName;
   }

   /**
    * Method to get vft reference file name
    * 
    * @return the vft reference file name.
    */
   public final String getRefFileName() {
      return this._refFileName;
   }

   /**
    * Method to set reference file name
    * 
    * @param refFileName reference file name string.
    */
   public final void setRefFileName(String refFileName) {
      this._refFileName = refFileName;
   }

   /**
    * Method to get vft reference file type
    * 
    * @return the vft reference file type.
    */
   public final String getRefFileType() {
      return this._refFileType;
   }

   /**
    * Method to set ref file type
    * 
    * @param refFileType the reference file type string.
    *  
    */
   public final void setRefFileType(String refFileType) {
      this._refFileType = refFileType;
   }

   /**
    * Build a VFT header, using all ancillary information. If this header does
    * not fill the bill, use getVFTName(), getCreatedBy(), etc.
    * 
    * @return null if vft not defined in this result.
    */
   public String getVFTHeader() {
      StringBuffer sb = new StringBuffer();
      if (this._vftName == null)
         return null;
      sb.append("VFT: " + this._vftName);
      sb.append("\nCreated: " + this._created + " by " + this._createdBy);
      if (this._updated != null && this._updated.length() > 1) {
         sb.append("\nUpdated: " + this._updated + " by " + this._updatedBy);
      }
      if (this._title != null)
         sb.append("\nTitle: \"" + this._title + "\"");
      if (this._comment != null)
         sb.append("\nComment: \"" + this._comment + "\"");
      return sb.toString();
   }

   /**
    * Method to return true if an update is pending on this reference. If so,
    * then use getUpdateFileName () and getUpdateFileType () for the details.
    * 
    * @return update pending flag.
    */
   public final boolean updateRef() {
      return this._updateRef;
   }

   /**
    * Method to get vft update file name. If this is null, and an update is
    * pending, then the this reference will be set to null at the next update
    * vft. If not null, then the reference is to be set to this file at the next
    * updateVFT.
    * 
    * @return the references next file name.
    */
   public final String getUpdateFileName() {
      return this._updateFileName;
   }

   /**
    * Method to get the file type of the next reference.
    * 
    * @return file type name
    */
   public final String getUpdateFileType() {
      return this._updateFileType;
   }

   /**
    * Method to get vft reference location
    * 
    * @return the vft reference location.
    */
   public final String getRefLink() {
      return this._refLink;
   }

   /**
    * Method to get file name
    * 
    * @return the file name
    */
   public final String getName() {
      return this._fileName;
   }

   /**
    * Method to set file name string.
    * 
    * @param name the file name string.
    *  
    */
   public final void setName(String name) {
      this._fileName = name;
   }

   /**
    * Method to get file size
    * 
    * @return the file size
    */
   public final long getSize() {
      return this._size;
   }

   /**
    * Method to get file buffer size
    * 
    * @return the file buffer size, or zero if there is no in-memory file.
    */
   public long getFileBufferSize() {
      if (this._fileBuffer == null)
         return 0;
      else
         return this._size;
   }

   /**
    * Method to get file buffer
    * 
    * @return a byte array, the contents of a file.
    */
   public final byte[] getFileBuffer() {
      return this._fileBuffer;
   }

   /**
    * Method to set file buffer
    * 
    * @param fileBuffer the buffer containing file.
    */
   public final void setFileBuffer(byte[] fileBuffer) {
      this._fileBuffer = fileBuffer;
   }

   /**
    * Method to set file size
    * 
    * @param size the file size
    */
   public final void setSize(long size) {
      this._size = size;
   }

   /**
    * Method to get file's date
    * 
    * @return the file's date
    * @deprecated Use getFileModificationTime
    */
   public final Date getDate() {
      return this._fileModified;
   }

   /**
    * Method to get file's date
    * 
    * @return the file's date
    */
   public final Date getFileModificationTime() {
      return this._fileModified;
   }

   /**
    * Method to set date string.
    * 
    * @param date the date string.
    * @deprecated Use setFileModificationTimeString
    */
   public final void setDate(Date date) {
      this._fileModified = date;
   }

   /**
    * Method to set date string.
    * 
    * @param date the date string.
    */
   public final void setFileModificationTime(Date date) {
      this._fileModified = date;
   }

   /**
    * Method to get file's internal DBMS id for the file type.
    * 
    * @return file's internal DBMS id for the file type.
    */
   public final int getFtId() {
      return this._ftId;
   }

   /**
    * Method to set file's internal DBMS id for the file type.
    * 
    * @param ftId the file's internal DBMS id for the file type.
    */
   public final void setFtId(int ftId) {
      this._ftId = ftId;
   }

   /**
    * Method to get contributor name
    * 
    * @return the contributor's name
    */
   public final String getFileContributor() {
      return this._fileContributor;
   }

   /**
    * Method to set the file contributor name
    * 
    * @param contributor the file contributor
    */
   public final void setFileContributor(String contributor) {
      this._fileContributor = contributor;
   }

   /**
    * Method to get file creation time
    * 
    * @return creation time string
    */
   public final Date getFileCreationTime() {
      return this._fileCreated;
   }

   /**
    * Method to set file creation time
    * 
    * @param created file creation time
    */
   public final void setFileCreationTime(Date created) {
      this._fileCreated = created;
   }

   /**
    * Method to get the qaDate.
    * 
    * @return the date the file becomes available to the public?
    */
   public final String getQaRelease() {
      return this._qaRelease;
   }

   /**
    * Method to get the file's checksum.
    * 
    * @return the file's checksum byte array
    */
   public final byte[] getChecksum() {
      return this._checksum;
   }

   /**
    * Was the file transfered is secure mode?
    * 
    * @return true if checksum was applied during add, replace or get.
    */
   public boolean secure() {
      if (this._checksum != null)
         return true;
      return false;
   }

   /**
    * Method to return the checksum string
    * 
    * @return the file's checksum, as a hex string. Returns null otherwise.
    */
   public String getChecksumStr() {
      if (this._checksum == null)
         return null;
      return FileUtil.checksumToString(this._checksum);
   }

   /**
    * Method to set checksum byte array
    * 
    * @param checksum the checksum value associated with this transaction
    */
   public final void setChecksum(byte[] checksum) {
      this._checksum = checksum;
   }

   /**
    * Method to set checksum value
    * 
    * @param checksum the checksum from hex ascii string input.
    */
   public final void setChecksum(String checksum) {
      if (checksum == null)
         this._checksum = null;
      else
         this._checksum = FileUtil.stringToChecksum(checksum);
   }

   /**
    * Method to return the associated note
    * 
    * @return note associated with the file.
    */
   public final String getNote() {
      return this._note;
   }

   /**
    * Method to set the associated note
    * 
    * @param note the note to associate with a file.
    */
   public final void setNote(String note) {
      this._note = note;
   }
   
   /**
    * Method to return file location
    * 
    * @return location of file
    */
   public final String getLocation() {
       return this._location;
   }

   /**
    * Method to set file location
    * 
    * @param location the location of the file
    */
   public final void setLocation(String location) {
       this._location = location;
   }
   
   /**
    * Method to return file id
    * 
    * @return file id
    */
   public final long getFileId() {
       return this._fileId;
   }
   
   /**
    * Method to set file id
    * 
    * @param fileId the id of the file
    */
   public final void setFileId(long fileId) {
       this._fileId = fileId;
   }
   
   
   /**
    * Method to commit a transaction into the restart file.  Compares
    * Result.getFileModificationTime() with cache's last query time.
    * If greater, then cache is update to reflect new value before
    * commiting.
    * @throws SessionException when session failure
    */
    public final void commit() throws SessionException
    {        
        if (this._restartCache != null)
        {
            //-----------------------

            //check to see if result time is greater than cache time,
            //if so, update it
            Date fileTime       = getFileModificationTime();
            Date cacheTime      = null;
            String cacheTimeStr = this._restartCache.getLastQueryTime();
           
            //parse CCSDSA string to date
            if (cacheTimeStr != null)
            {
                try {
                    cacheTime = DateTimeUtil.getCCSDSAWithLocalTimeZoneDate(cacheTimeStr);
                } catch (ParseException pEx) {
                    cacheTime = null;
                }
            }

            //if fileTime exists and is greater than cache time, update
            if (fileTime != null &&
                     (cacheTime == null || cacheTime.before(fileTime)))
            {
                this._restartCache.setLastQueryTime(fileTime.getTime());
            }

            //-----------------------

            //now commit cache object
            this._restartCache.commit();

            //-----------------------

        }
    }
  
    public ClientRestartCache getClientRestartCache() 
    {
    	return this._restartCache;
    }
}