/*
 * Created on Dec 22, 2004
 */
package jpl.mipl.mdms.FileService.komodo.client;

/**
 * <b>Purpose:</b> Interface for the UtilClient argument keywords.
 * 
 * <PRE>
 * Copyright 2004, California Institute of Technology. 
 * ALL RIGHTS RESERVED. U.S.
 * Government Sponsorship acknowledge. 2004.
 * </PRE>
 * 
 * <PRE>
 * 
 * ============================================================================
 * <B>Modification History : </B> 
 * ----------------------
 * 
 * <B>Date              Who         What </B>
 * ----------------------------------------------------------------------------
 * 12/22/2004        Nick        Initial Release
 * 05/26/2005        Nick        Added push,pull options
 * 03/09/2006        Nick        Added 'format' option for date formatting
 * ============================================================================
 * 
 * </PRE>
 * 
 * @author Thomas Huang (Thomas.Huang@jpl.nasa.gov)
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: CMD.java,v 1.17 2013/08/20 16:45:17 ntt Exp $
 *  
 */

public interface CMD 
{
   String HELP = "help";
   String DEBUG = "debug";
   String LONG = "long";
   String VERYLONG = "verylong";
   String VERSION = "version";
   String RESTART = "restart";
   String REPLACE = "replace";
   String SAFEREAD = "saferead";
   String AUTODELETE = "autodelete";
   String CRC = "crc";
   String DISPLAYMESSAGES = "displaymessages";
   String LIMITMESSAGES = "limitmessages";
   String COMMENT = "comment";
   String OUTPUT = "output";
   String BEFORE = "before";
   String AFTER = "after";
   String BETWEEN = "between";
   String AND = "and";
   String VFT = "vft";
   String REFERENCE = "reference";
   String LOGFILE = "logfile";
   String LOGFILEROLLING = "logfilerolling";
   String MAILMESSAGEFROM = "mailmessagefrom";
   String MAILMESSAGETO = "mailmessageto";
   String MAILREPORTTO = "mailreportto";
   String MAILREPORTAT = "mailreportat";
   String MAILSMTPHOST = "mailsmtphost";
   String MAILSILENTRECONN = "mailsilentreconnect";
   String INVOKE = "invoke";
   String INVOKEEXITONERROR = "invokeexitonerror";
   String USING = "using";
   String USER = "user";
   String PASSWORD = "password";
   String FILETYPE = "filetype";
   String SERVERGROUP = "servergroup";
   String FOR = "for";
   String RECEIPT = "receipt";
   String SERVER = "server";
   String CLASSIC = "classic";
   String SERVERGROUPS = "srvgroups";
   String PUSH = "push";
   String PULL = "pull";
   String FORMAT = "format";
   String INVOKEASYNC = "invokeasync";
   String QUERY = "query";
   String REPLICATE = "replicate";
   String REPLICATEROOT = "replicateroot";
   String FORCE = "force";
   String OWNER = "owner";
   String GROUP = "group";
   String DIFF = "diff";   
   String FILEHANDLER = "filehandler";
   String EXPIRY = "expiry";
}
