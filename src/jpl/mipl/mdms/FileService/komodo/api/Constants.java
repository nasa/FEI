/* ******************************************************************************
 * Copyright (C) 2012 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ***************************************************************************** */
package jpl.mipl.mdms.FileService.komodo.api;


/**
 * <b>Purpose:</b>
 * Interface to define constants and error codes for the Komodo API.
 *
 *   <PRE>
 *   Copyright 2012, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2012.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 01/01/1999        MDMS             Initial Release
 * 08/22/2005        Nick             Initial documentation.
 * 08/22/2005        Nick             Incremented API to 2.9, Client to 2.0.0b
 * 08/24/2005        Nick             Added KILLSUBSCRIPTION modifier
 * 02/27/2006        Nick             Added MODIFYFT admin command, with
 *                                    associated modifiers.
 * 08/11/2008        Adrian           Incremented server version to 3.0.2
 *                                    - fei5register "force" option
 *                                    - fixed connection count "negative" bug
 *                                      when logins fail and also from the use of the
 *                                      AUTHSERVERGROUPUSER command (i.e. connect button of the gui).
 *                                    - fixed showroles hanging bug
 *                                      when externalRoleName is null
 *                                    - fixed add file no disk space bug
 *                                    - fixed single quotes file filter bug
 *                                    - fixed changepassword bug that disregards
 *                                      supplied oldpassword for non admin users
 *                                    - using jgroups 2.6.3
 * 12/03/2008       Adrian            Incremented server version to 3.0.3
 *                                    - fixed AR-115110: renaming a file to a name of a logically
 *                                      deleted file
 *                                    - fixed AR-114873: bug when adding a file that causes
 *                                      filetype spaceUsed to exceed spaceReserved
 *                                    - fixed AR-115530: bug when replacing a registered file where a
 *                                      shadow directory does not exists
 *                                    - Now sends error messages from moveFile to client.  Before these
 *                                      messages were only stored in the log.
 *                                    - fixed AR-115523: file left in delete lock until interactive/gui client
 *                                      exits.
 *                                    - Added support for admin command modifyfiletype [spaceReserved|threshold]
 * 03/06/2009       Adrian            Incremented server version to 3.0.4
 *                                    - fixed AR-115760, added host info to the receipts table
 * 03/30/2009       Adrian            Incremented server version to 3.0.5
 *                                    - fixed a bug where the add file command doesn't properly cleanup when
 *                                      adding a file that already exists. AR-115853
 *                                    - fixed AR-115868: file deletes from filetypes with logdelrecord = 0
 *                                      subtracts from spaceUsed twice.  Stored procedure changes only.
 *                                    - fixed a bug where some files are not getting added when multiple processes
 *                                      are adding the same files to the same filetypes at the same time.
 * 06/26/2009       Nick              Added new operation, SHOWLOCKS, with value of '5'.
 *                                    Incremented Komodo API version to 2.9.7 to account for new operation.
 * 09/28/2009       Adrian            Incremented server version to 3.0.6
 *                                    - Added wildcard support for showfiletypes and showrolesforfiletype command
 *                                    - Fixed AR-115977: changepassword bug in fei5admin
 *                                    - Added external role name in the result of showroles/showrolesforfiletype admin command
 *                                    - Changed space units for showfiletypes from bytes to Mbytes.
 *                                    - Handles protocol version 3.0.0 (command string format)
 *                                    - Server changes to merge user capabilites in the showusers admin command.
 *                                    - Added support for showfiletypesforrole admin command.
 *                                    - Added support for showusersforrole admin command.
 *                                    - Added user's system level capabilities (admin, read, write, vft)
 *                                      in the output of showrolesforuser
 *                                    - Added support for showlocks admin command
 *                                    - Added support for setlock admin command
 *                                    - Added support for the diff option in fei5replace: replace file only if file is different.
 * 12/08/2009       Nick              Added new password handling using public/private key encryption
 *                                    Incremented Komodo API version to 3.0.1 to account for new operation.
 * 12/09/2009       Adrian            Incremented server version to 3.0.7
 *                                    - password handling using public/private key encryption
 * 04/06/2010       Nick              Incremented client version to 2.2.4
 *                                    - fixes to AR116324 (kinit create .komodo dir) and AR116318 (showcaps using login file)
 * 05/03/2010       Adrian            Incremented server version to 3.0.8
 *                                    - showcaps command now shows merged capabilities per filetype.
 *                                    - Added support for the diff option in get commands: get file only if file is different.
 * 08/05/2010       Nick              Incremented client version to 2.2.6
 *                                    - added new command QUERYTYPES ('qrytypes') for server filetype queries
 * 08/11/2010       Adrian            Incremented server version to 3.0.9
 *                                    - added support for the QUERYTYPES command to be used by the DynamicDomain capability.
 *                                    - added support for receipts on register command.
 *                                    - fixed AR-116074, special characters in filename.
 * 01/11/2011       Adrian            Incremented server version to 3.1.0
 *                                    - added retry logic in the dbms registries to retry transaction when a deadlock
 *                                      is detected.  AR-116615
 *                                    - fixed AR-116721, checksum value not cleared when fei5register-ing a file
 * 04/13/2011       Adrian            Incremented server version to 3.1.1
 *                                    - fixed AR-116327, client clock ahead of server.  Added a server parameter timesync.threshold
 *                                      that is used to determine whether to substitute the current time for a timestamp received
 *                                      from the client that is ahead of the server's clock.
 *                                    - Added a synchronized block when ServerMessageProcessor unmarshalls a message object.  This is
 *                                      to prevent the following type of error:
 *                                         ERROR [ServerMessageProcessor:138] {Incoming-2,JUNO_TEST,128.149.134.53:42144}
 * 02/28/2012       Adrian            Incremented server version to 3.1.2
 *                                    - Reclassified some ERROR level log messages to either INFO or WARN
 * 05/10/2012       Adrian            Incremented server version to 3.1.3
 *                                    - fixed AR-117246, connection metrics count goes to negative
 * 12/08/2012       Adrian            Incremented server version to 3.1.4 - protocol 3.0.4
 *                                    - Modified creation of JAXBContext instance
 *                                    - Better MySQLRegistery handling of deadlock and lockwait timeout
 *                                    - Added support for command GETAUTHTOKEN
 * 03/14/2013       Adrian            Incremented protocol version to 3.0.5
 *                                    - Server performing token based authentication
 *                                    - TFA AA registry
 *                                    - Added support for command GETAUTHTYPE
 * 06/18/2013       Adrian            Incremented server version to 3.1.5
 *                                    - Fixed a bug with the TFA LDAP registry code that calls the TFA authentication
 *                                      mechanism when it should call the token authentication function.  This bug has
 *                                      caused the TFA accounts getting locked for users with older clients.
 * 08/01/2013       Adrian            Incremented server version to 3.1.6
 *                                    - Fixed ISA-54557; deadlock transactions not properly retried.
 *                                    - Added switch to disable filetype spaceReserved checking; set spaceReserved = -1 to disable
 * 12/06/2013       Nick              Incremented client version to 2.3.11
 *                                    - Added 'Skip All' option in FEI5GUI to ignore remaining existing files
 * 05/21/2014       Adrian            Incremented server version to 3.1.7
 *                                    - Added support for user metaQuery flag/privs that allows user to perform FMQ queries, if enabled.
 * 07/09/2014       Adrian            Incremented server version to 3.1.8
 *                                    - Trigger cleanup of command if the server receives an unknown command...
 * 08/09/2016       Adrian            Incremented server version to 3.1.9, client version to 2.3.16
 *                                    - Using stronger (TLS) cipher suites;
 *                                        Configurable via komodo.net.ciphers and komodo.net.protocol system properties
 *                                    - Earlier handling of unknown commands
 *                                    - More efficient use of JAXBContext; instantiate once and reuse
 * 09/29/2016       Nick              Incremented server version to 3.1.10, client version to 2.3.17
 *                                    - Perform null-check to result of File.list() and File.listFiles()
 * 01/12/2017       Nick              Incremented client version to 2.3.18
 *                                    - Bounded buffered reader defaults were increased.  Added properties
 *                                    to override these values, even to disable to checks.  Also failed
 *                                    invocation error messages include the underlying errno message if
 *                                    available.
 * 07/27/2017       Adrian            Incremented server version to 3.1.11
 *                                    - Added server support for LDAP application accounts.  Application accounts
 *                                    are those in ou=applications,dc=dir,dc=jpl,dc=nasa,dc=gov branch.  Requested by InSight project
 * 11/15/2017       William           Incremented protocol version to 3.0.6 and server version to 3.1.12
 *                                    - Server will send getFiles by batch for MySQL and Sybase
 *                                    for getFiles, listFiles, pushSubscribe
 * 02/14/2018       Nick              Incremented  client version to 2.3.19
 *                                    - Client was updated to use the 3.0.6 protocol which
 *                                    batches file list results from the server.
 *                                    - secure client sockets are no longer enabling only specified cipher suites from config.
 *                                    So when server needs to update the cipher suites, clients are not required to update as well.
 *
 *
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: Constants.java,v 1.227 2018/02/15 02:25:08 ntt Exp $
 *
 */

public interface Constants extends jpl.mipl.mdms.utils.Constants
{

   //----------------------------------------------------------------------


   public static final String CLIENTVERSIONSTR = "FEI5 release 2.3.19, February 2018";    //updated 02/14/18
   public static final String SERVERVERSIONSTR = "FEI5 release 3.1.12, December 2017";    //updated 12/11/17

   /* Komodo API Version information. */
   public static final String APIVERSIONSTR   = "Komodo API release 3.0.6, November 2017 ";
   public static final float APIVERSION       = 3.06f; //updated 11/15/2017

   public static final int APIVERSIONINT      = (int) (APIVERSION * 100.0f);
   public static final int APIVERSIONINT3050  = 305;
   public static final int APIVERSIONINT3040  = 304;
   public static final int APIVERSIONINT3030  = 303;
   public static final int APIVERSIONINT3020  = 302;
   public static final int APIVERSIONINT3010  = 301;
   public static final int APIVERSIONINT3000  = 300;
   public static final int APIVERSIONINT2970  = 297;
   public static final int APIVERSIONINT2960  = 296;
   public static final int APIVERSIONINT2950  = 295;
   public static final int APIVERSIONINT2930  = 293;
   public static final int APIVERSIONINT2920  = 292;
   public static final int APIVERSIONINT2830  = 283;
   public static final int APIVERSIONINT2720  = 272;


   public static final String PROTOCOLVERSION = "KOMODO/3.06";//updated 12/11/17

   public static final String COPYRIGHT = "Copyright 2002-2018 Jet Propulsion Laboratory, Caltech, NASA";

   //---------------------------------------------------------------------

   /* Security types */
   public static final int INSECURE = 0;
   public static final int SSL = 1;
   public static final int KERBEROS = 2;

   // Length of SHA checksum (in bytes).
   public static final int DIGESTLENGTH = 20;

   //---------------------------------------------------------------------

   /*
    * Session errors
    */
   public static final int INVALID_LOGIN = -2001;
   public static final int INVALID_TYPE = -2002;
   public static final int TYPE_OPEN = -2003;
   public static final int CONN_FAILED = -2004;
   public static final int ALREADY_OPEN = -2005;
   public static final int TCP_PORT_RANGE = -2006;
   public static final int TIMEOUT_RANGE = -2007;
   public static final int NO_SERVERS = -2008;
   public static final int CHECKSUM_ERROR = -2009;

   //---------------------------------------------------------------------

   /*
    * Domain errors.
    */
   public static final int MALFORMEDENTRY   = -2050;
   public static final int REGEXPSYNTAXERR  = -2051;
   public static final int DOMAINIOERR      = -2052;
   public static final int DOMAINFTNOTFOUND = -2053;
   public static final int DOMAINDUMPERR    = -2054;
   public static final int DOMAINLOOKUPERR  = -2055;
   public static final int DOMAINPARSEERR   = -2056;

   //---------------------------------------------------------------------

   /*
    * Define the error codes associated with results.
    */
   public static final int OK = 0;

   //Used when server is saying it has more results
   public static final int MORE = 1315185;

   public static final String FILE_LIST_BATCH = "morefilesavailable";
   public static final String FILE_LIST_FINISH = "done";

   // Operation OK, but warning issued.
   public static final int WARNING = -1;

   // Feature is not implemented.
   public static final int NOT_IMPLEMENTED = -3001;

   // Operation rejected by client or server.
   public static final int NACKED = -3002;

   // File not found on disc.
   public static final int FILE_NOT_FOUND = -3003;

   // File was not added due to error.
   public static final int FILE_NOT_ADDED = -3004;

   // File was not delete due to error.
   public static final int FILE_NOT_DELETED = -3005;

   // A file that can't be shipped (eg. softlink)
   public static final int FILE_NOT_NORMAL = -3006;

   // File was not registered due to error.
   public static final int FILE_NOT_REGISTERED = -3032;

   // Regular expression found directory on add.
   public static final int DIRECTORY_IGNORED = -3007;

   // Read/write error on file or stream.
   public static final int IO_ERROR = -3008;

   // Invalid message traffic.
   public static final int PROTOCOL_ERROR = -3009;

   // File expression not well formed.
   public static final int INVALID_FILE_EXPR = -3010;

   // get <regexp> resulted in no files to recv.
   public static final int NO_FILES_MATCH = -3011;

   // Add command resulted in no files to xmit.
   public static final int NO_FILE_SPECIFIED = -3012;

   // Comment not set in DBMS.
   public static final int FILE_NOT_COMMENTED = -3013;

   // Archive note not set in DBMS.
   public static final int FILE_NOT_ARCHIVED = -3014;

   // No such server in domain.
   public static final int NO_SUCH_SERVER = -3015;

   // Unknown command.
   public static final int INVALID_COMMAND = -3017;

   // Connection to server dropped.
   public static final int UNEXPECTED_EOF = -3018;

   // Could not rename a file.
   public static final int FILE_NOT_RENAMED = -3019;

   // Checksum does not match.
   public static final int FILE_NOT_VERIFIED = -3020;

   // Could not get list of files to transfer.
   public static final int FILE_LIST_ERROR = -3021;

   // File already exists.
   public static final int FILE_EXISTS = -3022;

   // Lock mode not in group | owner.
   public static final int INVALID_LOCK_MODE = -3023;

   // Interrupt getting result from proxy thread.
   public static final int INTERRUPTED = -3024;

   // Invalid date range.
   public static final int DATE_RANGE = -3025;

   // Virtual file type not found.
   public static final int VFTNOTFOUND = -3026;

   // Could not local delete file on add/get.
   public static final int LOCAL_FILE_DEL_ERR = -3027;

   // Inconsistent file size
   public static final int INCONSISTENT_FILE_SIZE_ERR = -3028;

   //  File move failed
   public static final int FILE_NOT_MOVED = -3029;

   // File skipped, without error.
   public static final int FILE_SKIPPED = -3030;

   // File delivery failed due to error
   public static final int FILE_NOT_DELIVERED = -3031;

   // DBMS error while validating user.
   public static final int INVALID_FILE_NAME = -3102;

   // Server found login invalid.
   public static final int INVALIDLOGIN = -3101;

   // DBMS error while validating user.
   public static final int LOGINDBMSERROR = -3102;

   // BaseClient connecting not logged in.
   public static final int USERNOTLOGGED = -3103;

   // Komodo could not use DBMS.
   public static final int DBMSERROR = -3104;

   // Komodo could not find file type.
   public static final int FTNOTFOUND = -3105;

   // BaseClient did not "ct" before trying op.
   public static final int FTNOTSELECTED = -3107;

   // Could not get lock on file.
   public static final int LOCKEDERR = -3108;

   // Server could not write file.
   public static final int FILEWRITEERR = -3109;

   // Exception during op: see error text.
   public static final int EXCEPTION = -3110;

   // DBMS exception raised: see error text.
   public static final int SQLEXCEPTION = -3111;

   // Komodo could not find file.
   public static final int FILENOTFOUND = -3112;

   // File already exists. Can't add or replace.
   public static final int FILEALREADYEXISTS = -3113;

   // Unlock file type when not locked.
   public static final int FTNOTLOCKED = -3114;

   // Komodo could not mkdir for file type.
   public static final int MKDIRERROR = -3116;

   // Komodo could not create file type.
   public static final int MKFTFAILED = -3117;

   // Syntax error in regexp.
   public static final int REGEXPSYN = -3118;

   // Command sent to server is invalid.
   public static final int INVALIDCMD = -3119;

   // Malformed command.
   public static final int MALFORMED = -3120;

   // Unknown command modifier.
   public static final int INVALIDMOD = -3121;

   // Command not known to server.
   public static final int UNKNOWNCMD = -3121;

   // Komodo could not find VFT.
   public static final int NOSUCHVFT = -3130;

   // VFT op when VFT not set by client.
   public static final int VFTNOTSET = -3131;

   // Local reference (soft link) exists.
   public static final int REFLCLALREADYEXISTS = -3132;

   // VFT Reference to file not yet made.
   public static final int FILENOTREFERENCED = -3133;

   // Command rejected because transaction was pending.
   public static final int INTRANSACTION = -3134;

   // Password not at least 8 characters.
   public static final int PASSWORDTOOSHORT = -3135;
   public static final int PASSWORDTOSHORT  = PASSWORDTOOSHORT;

   // Komodo denied client access to operation.
   public static final int DENIED = -3150;

   // Command cancelled on Komodo shutdown.
   public static final int INSHUTDOWN = -3151;

   // Command was aborted on Komodo shutdown.
   public static final int SHUTDOWNABORT = -3152;

   // Shutdown timeout value out of range.
   public static final int INVALIDTIMEOUT = -3153;

   // Local reference make soft link failed.
   public static final int REFLOCERR = -3232;

   // Null arg found when arg was required.
   public static final int MISSINGARG = -3233;

   // Can't open restart file.
   public static final int RESTARTFILEERR = -3234;

   // No RFS users found.
   public static final int USERSNOTFOUND = -3235;

   // Parent file is not a directory.
   public static final int NOTADIRECTORY = -3236;

   // Server can't write file.
   public static final int SRVCANTWRITEFILE = -3237;

   // Misc. not found.
   public static final int NOTFOUND = -3238;

   // Illegal argument
   public static final int ILLEGAL_ARG = -3239;

   // Ping message received
   public static final int PING_RECVD = -3136;

   // Request checksum
   public static final int REQUESTCHECKSUM = -3137;



   //---------------------------------------------------------------------

   /*
    * Option parameters - "ON" | "OFF" types. Note: The options vector defaults
    * to 0. When defining options, keep in mind that all options are off until
    * set. For example, NOAUTOCOMMIT must be set in order to disable AUTOCOMMIT.
    */

   // Compute checksum on transfers.
   public static final int CHECKSUM = 1;

   // Version files on get.
   public static final int FILEVERSION = 2;

   // Replace files on get.
   public static final int FILEREPLACE = 4;

   // Get receipt on transfer.
   public static final int RECEIPTONXFR = 8;

   // Delete files on add or replace.
   public static final int AUTODEL = 16;

   // Persist last files date in $HOME/.Komodo
   public static final int RESTART = 32;

   // Get files in currdir/.shadow. Move when done.
   public static final int SAFEREAD = 64;

   // Abort all transactions on error.
   public static final int ABORTALLONERR = 128;

   // Automatically commit to restart files.
   public static final int AUTOCOMMIT = 256;

   // resume file transfer
   public static final int RESUME = 512;

   // replace only if different
   public static final int DIFF = 1024;

   // replication enabled (shared definition as REPLICATE Capability)
   public static final int REPLICATE = 65536;

   //---------------------------------------------------------------------

   /*
    * Capabilities. These fields are defined in the file rfsTables.sql,
    * which is the final authority.
    */
   public static final int GET = 1;
   public static final int ADD = 2;
   public static final int REPLACE = 4;
   public static final int DELETE = 8;
   public static final int OFFLINE = 16;
   public static final int RENAME = 32;
   public static final int NOTIFY = 64;
   public static final int CONFIRM = 128;
   public static final int QAACCESS = 256;
   public static final int ARCHIVE = 512;
   public static final int SUBTYPE = 1024;
   public static final int RECEIPT = 2048;
   public static final int LOCKTYPE = 4096;
   public static final int VFT = 8192;
   public static final int PUSHSUBSCRIBE = 16384;
   public static final int REGISTER = 32768;
   //public static final int REPLICATE = 65536; //same as param REPLICATE (65536)
   public static final int METAQUERY = 131072;

   //---------------------------------------------------------------------

   /* Special user access */
   public static final int ADMIN = 3;
   public static final int WRITE_ALL = 2;
   public static final int READ_ALL = 1;
   public static final int NO_ACCESS = 0;
   public static final int NOT_SET = -1;

   //---------------------------------------------------------------------

   /* Results vector capacity */
   public static final int RESULTCAPACITY = 128;
   public static final int RESULTCAPINCR = 64;

   //---------------------------------------------------------------------

   /* Results timeout range (in milliseconds) */
   public static final int RESULTNOTIMEOUT  = 0;
   public static final int RESULTMINTIMEOUT = 0;
   public static final int RESULTMAXTIMEOUT = 60000; //60;

   //---------------------------------------------------------------------

   /* Keep alive timeout range (in milliseconds) */
   public static final int KEEPALIVETIMEDEFAULT = 180000;  //3 minutes
   public static final int KEEPALIVETIMEMIN     =  30000;  //30 seconds
   public static final int KEEPALIVETIMEMAX     = 300000;  //5 minutes

   public static final int TIMEOUT_NONE         = 0;       //0 minutes
   public static final int TIMEOUT_DEFAULT      = 300000;  //5 minutes

   //---------------------------------------------------------------------

   /* Requests vector capacity */
   public static final int REQUESTCAPACITY = 16;
   public static final int REQUESTCAPINCR = 8;

   /* TCP Port range. */
   public static final int MINTCPPORT = 1024;
   public static final int MAXTCPPORT = 65535;

   /* Komodo Command dictionary */

   /* Virtual file type Commands. */
   public static final String ADDREF          = "vaddref_";  //':';
   public static final String DELVFT          = "vdelvref";  //'!';
   public static final String DELREF          = "vdelref_";  //'@';
   public static final String GETREFFILE      = "vgetref_";  //'#';
   public static final String ADDVFT          = "vaddvft_";  //'%';
   public static final String GETVFT          = "vgetvft_";  //'&';
   public static final String SETREFERENCE    = "vsetref_";  //'(';
   public static final String CANCELREFERENCE = "vcnclref";  //')';
   public static final String SHOWREF         = "vshowref";  //'`';
   public static final String SHOWREFAT       = "vshwrfat";  //'*';
   public static final String SHOWVFT         = "vshowvft";  //'=';
   public static final String SHOWVFTAT       = "vshwvfat";  //'<';
   public static final String ADDVFTREADER    = "vaddredr";  //'V';
   public static final String SHOWVFTREADERS  = "vshowrdr";  //'W';
   public static final String DELVFTREADER    = "vdelredr";  //'Y';
   public static final String UPDVFT          = "vupdtvft";  //'?';

   public static final char UNUSED6 = '6';
   public static final char UNUSED7 = '7';
   public static final char UNUSED8 = '8';
   public static final char UNUSED9 = '9';


   /* Komodo File commands. */
   public static final String CHANGETYPE      = "chngtype";  //'b';
   public static final String SHOWFILES       = "showfile";  //'s';
   public static final String AUTOSHOWFILES   = "notify_";  //'S';
   public static final String GETFILES        = "getfiles";  //'g';
   public static final String AUTOGETFILES    = "subscrib";  //'G';
   public static final String GETFILEFROMVFT  = "getfrvft";  //'_';
   public static final String GETFILEFROMFS   = "getfrmfs";  //'>';
   public static final String GETFILEOUTPUTSTREAM = "getfilos";  //'}';
   public static final String COMMENTFILE     = "comment_";  //'c';
   public static final String ARCHIVENOTE     = "archive_";  //'v';
   public static final String ADDFILE         = "addfile_";  //'a';
   public static final String ADDFILEANDREF   = "addfwref";  //'{';
   public static final String REPLACEFILE     = "replace_";  //'r';
   public static final String DELETEFILE      = "delete__";  //'d';
   public static final String MAKECLEAN       = "makclean";  //'m';
   public static final String IGNOREFILE      = "ignorefl";  //'i';
   public static final String RENAMEFILE      = "renamefl";  //'n';
   public static final String SHOWCAPS        = "showcaps";  //'o';
   public static final String COMPUTECHECKSUM = "cmpchksm";  //'k';
   public static final String CHECKFILES      = "checkfls";  //'f';
   public static final String CHECK           = "check___";  //'h';
   public static final String DISPLAY         = "display_";  //'p';
   public static final String ACCEPT          = "accept__";  //'t';
   public static final String SUBSCRIBEPUSH   = "pshsubsr";  //'[';
   public static final String PING            = "ping____";  //'l';
   public static final String REGISTERFILE    = "register";  //'3';
   public static final String UNREGISTERFILE  = "unregstr";  //'4';
   public static final String GETAUTHTOKEN    = "authtokn";
   public static final String GETAUTHTYPE     = "authtype";
   public static final String GET_MORE_FILES = "morefiles";
   /* Credential commands. */
   public static final String CREDLOGIN       = "credlgin";  //'u';
   public static final String CREDLIST        = "credlist";  //'j';
   public static final String CREDLOGOUT      = "credlgot";  //'w';

   /* Misc commands */
   public static final String AUTHSERVERGROUPUSER = "authuser";  //'~';
   public static final String NOOPERATION         = "no_op___";  //'0';
   public static final String EXCHANGEPROPERTY    = "exchprop";  //'2';
   public static final String QUERYTYPES          = "qrytypes";

   /* Session shared property modifiers */
   public static final char SETPROPERTY = 's';
   public static final char GETPROPERTY = 'g';

   /* Komodo File Access Modifiers */
   public static final char NOMODIFIER = ' ';
   public static final char REGEXP = 'x';
   public static final char LATEST = 'l';
   public static final char DATETIME = 'd';
   public static final char FILENAMES = 'f';
   public static final char MEMTRANSFER = 'm';
   public static final char FILESSINCE = 's';
   public static final char FILESBETWEEN = 'b';
   public static final char ATTIMEOF = 'a';

   /* Komodo File type commands. */
   public static final String LOCKFILETYPE   = "lckftype";  //'-';
   public static final String UNLOCKFILETYPE = "unlkftyp";  //'+';
   public static final String SHOWTYPES      = "shwtypes";  //'T';

   /* Lock file modifiers */
   public static final char NOOPMOD = 'n';
   public static final char GROUPMOD = 'g';
   public static final char OWNERMOD = 'o';

   /* Komodo Admin commands. */
   public static final String ADDFTTOROLE      = "adft2rol";  //'A';
   public static final String ADDUSERTOROLE    = "adusr2rl";  //'B';
   public static final String DSYNC            = "dsync___";  //'C';
   public static final String FSYNC            = "fsync___";  //'D';
   public static final String MAKEDOMAIN       = "mkdomain";  //'E';
   public static final String ADDFT            = "addftype";  //'F';
   public static final String ADDROLE          = "addrole_";  //'G';
   public static final String ADDUSER          = "adduser_";  //'H';
   public static final String REMOVEROLE       = "remvrole";  //'I';
   public static final String REMOVEUSER       = "remvuser";  //'J';
   public static final String RMFTFROMROLE     = "rmftfrrl";  //'K';
   public static final String RMUSERFROMROLE   = "rmusfrrl";  //'L';
   public static final String SHOWCONN         = "showconn";  //'M';
   public static final String SHOWFT           = "shwftype";  //'N';
   public static final String SHOWMEM          = "shwmemry";  //'O';
   public static final String SHOWPARAMS       = "shwparms";  //'P';
   public static final String SHOWSERVERS      = "shwsrvrs";  //'Q';
   public static final String SHOWROLES        = "shwroles";  //'R';
   public static final String SHOWUSERS        = "shwusers";  //'S';
   public static final String SHUTDOWN         = "shutdown";  //'T';
   public static final String SHOWROLESFORFT   = "shrlfrft";  //'U';
   public static final String SHOWTYPESFORROLE = "shftfrrl";  //N/A
   public static final String CHANGEPASSWORD   = "chpasswd";  //',';
   public static final String SHOWROLESFORUSER = "shrlfrus";  //'X';
   public static final String SHOWUSERSFORROLE = "shusfrrl";  //N/A
   public static final String REMOVEFT         = "rmvftype";  //'Z';
   public static final String HOTBOOT          = "hotboot_";  //']';
   public static final String MOVEFILES        = "movfiles";  //'^';
   public static final String MODIFYFT         = "modifyft";  //'1';
   public static final String SHOWLOCKS        = "shwlocks";  //'5';
   public static final String LOGMESSAGE       = "logmessg";  //N/A
   public static final String MODIFYROLE       = "modrole_";  //N/A
   public static final String MODIFYUSERACCESS = "modusrac";  //N/A
   public static final String SETLOCKS         = "setlocks";  //N/A


   /* Admin command MOVEFILES modifiers */
   public static final char REPLACE_FILE = 'r';
   public static final char NO_PRESERVE = 'p';
   public static final char REPLACE_NO_PRESERVE = 'q';

   /* Admin command MODIFYFT modifiers */
   public static final char SETLOGDELRECORD  = 'D';  //value is 'on' or 'off'
   public static final char SETLOCATION      = 'L';  //value is a path
   public static final char SETCHECKSUM      = 'C';  //value is 'on' or 'off'
   public static final char SETRECEIPT       = 'R';  //value is 'on' or 'off'
   public static final char SETSPACERESERVED = 'S';  //value is number of megabytes
   public static final char SETTHRESHOLD     = 'T';  //value is number of megabytes

   /* Subscription modifiers */
   public static final char KILLSUBSCRIPTION = 'k';
   public static final char SUBSCRIBECHECKSUM  = 'c';

   /* Admin command MODIFYROLE modifiers */
   public static final char ADDCAPABILITIES    = 'A';  //add capabilities
   public static final char DELETECAPABILITIES = 'D';  //delete capabilities
   public static final char SETCAPABILITIES    = 'S';  //set capabilities

   /* Komodo connection maintenance commands. */
   public static final String LOGIN = "login___";  //'h';
   public static final String READY = "ready___";  //'e';
   public static final String QUIT  = "quit____";  //'q';

   /* Komodo no operation modifier. */
   public static final char QUIET = 'q';  //no result object should be created

   /* Inverse modifier for lookup.
    * show$XFor$Y with this modifier indicates that server should perform a
    * lookup of $Y's matching $X
    */
   //public static final char INVERSE = 'i';

   /* File register (REGISTERFILES) modifiers */
   //reuse NOMODIFIER = ' '
   public static final char REREGISTER    = 'r';
   public static final char REREGISTERALL = 'R';

   /* Misc. */
   public static final boolean NEEDTYPE = true;
   public static final boolean NEEDNOTYPE = false;

   /* file lock types and their string representation*/
   /* if new locks added, add in FileLocksUtil.java as well*/
   public static final int NOLOCK       = 0;
   public static final int GETLOCK      = 1;
   public static final int ADDLOCK      = 2;
   public static final int REPLACELOCK  = 3;
   public static final int DELETELOCK   = 4;
   public static final int RESERVEDLOCK = 5;
   public static final int LINKLOCK     = 6;
   public static final int RENAMELOCK   = 7;
   public static final int DELETEDLOCK  = 8;
   public static final int MOVELOCK     = 9;
   public static final int MOVEPRSTLOCK = 10;




   /* network communication string tags */
   public static final String ACK = "ACK";

   //---------------------------------------------------------------------

   /* Directory names */
   public static final String SHADOWDIR = ".shadow";
   public static final String RESTARTDIR = ".komodo";
   public static final String LOGINFILE = ".komodologin";
   public static final String RESTARTEXTENSION = ".restart";
   public static final String LEGACY_RESTART_SUFFIX = "restart";
   public static final String SETTINGDIR = "settings";
   public static final String NOTIFYEXTENSION = ".notify";
   public static final String PLUGINSDIR = "plugins";
   public static final String BACKUPEXTENSION = "~";

   //---------------------------------------------------------------------

   /* thread group names */
   public static final String ADMIN_THREAD = "adm";
   public static final String USER_THREAD = "usr";

   /* server configuration property names */
   public static final String MAX_ADMIN = "maxconn.admin";
   public static final String MAX_USER = "maxconn.usr";
   public static final String DB_ROLE = "db.role";
   public static final String DB_REGISTRY = "db.registry";
   public static final String TIME_SYNC_THRESHOLD = "time.sync.threshold";

   //---------------------------------------------------------------------

   /* server sigevent property names */
   public static final String SIGEVENT_DS = "db.SigEventsDS";
   public static final String SIGEVENT_SECURITY = "db.SigEvents.SECURITY";
   public static final String SIGEVENT_USER_SECURITY = "db.SigEvents.USERSECURITY";
   public static final String SIGEVENT_USER = "db.SigEvents.USER";
   public static final String SIGEVENT_ALERT = "db.SigEvents.ALERT";
   public static final String SIGEVENT_DBMS = "db.SigEvents.DBMS";
   public static final String SIGEVENT_SERVER_STATE = "db.SigEvents.SERVERSTATE";
   public static final String SIGEVENT_VFT = "db.SigEvents.VFT";

   /* Komodo property names  */
   public static final String PROPERTY_CLIENT_TIMEOUT      = "komodo.client.timeout";
   public static final String PROPERTY_RESTART_DIR         = "komodo.restart.dir";
   public static final String PROPERTY_CLIENT_PULSE        = "komodo.client.pulse";
   public static final String PROPERTY_KEEP_ALIVE          = "komodo.keep.alive";
   public static final String PROPERTY_CONFIG_DIR          = "komodo.config.dir";
   public static final String PROPERTY_USER_HOME           = "komodo.user.home";
   public static final String PROPERTY_DOMAIN_FILE         = "komodo.domain.file";
   public static final String PROPERTY_QUERY_INTERVAL      = "komodo.query.interval";
   public static final String PROPERTY_PRIVATE_KEY         = "komodo.private.key";
   public static final String PROPERTY_PUBLIC_KEY          = "komodo.public.key";
   public static final String PROPERTY_URL_READ_TIMEOUT    = "komodo.url.read.timeout"; //milliseconds
   public static final String PROPERTY_URL_CONN_TIMEOUT    = "komodo.url.conn.timeout"; //milliseonds
   public static final String PROPERTY_UNK_CMD_CLEANUP     = "komodo.unkcmd.cleanup";
   public static final String PROPERTY_DB_ROW_LIMIT     = "komodo.db.row.limit";

   public static final String PROPERTY_FILEHANDLING_ENABLE = "komodo.filehandling.enable";


   /** Number of seconds to use for the reconnect throttle window */
   public static final String PROPERTY_THROTTLE_WINDOW     = "komodo.throttle.window";



   /**
    * int property, specifies the maximum number of lines the
    * bounded reader will accept before throwing an IOException.
    */
   public static final String PROPERTY_IO_BOUNDEDREADER_MAX_LINE  = "komodo.io.boundedreader.maxlinecount";

   /**
    * int property, specifies the maximum number of characters per line
    * the bounded reader will accept before returning what it has collected so
    * far.
    */
   public static final String PROPERTY_IO_BOUNDEDREADER_MAX_CHAR = "komodo.io.boundedreader.maxlinelength";

   /**
    * boolean property, that when set to true, requests that no bounds
    * check be performed by the bounded reader.  Note: This can be dangerous.
    */
   public static final String PROPERTY_IO_BOUNDEDREADER_UNBOUNDED   = "komodo.io.boundedreader.unbounded";



   /*  AA service OP codes */
   public static final String AA_SERVICE_OP_GET   = "GET";
   public static final String AA_SERVICE_OP_ADD   = "ADD";


   /* Authentication methods */
   public static final String AUTH_INTERNAL_PASSW        = "internal/password";
   public static final String AUTH_INTERNAL_PASSC        = "internal/passcode";
   public static final String AUTH_INSTITUTIONAL_PASSW   = "institutional/password";
   public static final String AUTH_INSTITUTIONAL_PASSC   = "institutional/passcode";

   public static final long   NO_EXPIRATION              = -1L;

}
