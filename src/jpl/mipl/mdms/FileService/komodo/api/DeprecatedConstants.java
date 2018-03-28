package jpl.mipl.mdms.FileService.komodo.api;

/**
 * <B>Purpose:<B>
 * Class maintains deprecated constants and mechanism to convert
 * from old constants to their updated version.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: DeprecatedConstants.java,v 1.1 2009/08/07 01:00:47 ntt Exp $
 *
 */
public class DeprecatedConstants
{
    /* Virtual file type Commands. */
    public static final char ADDREF = ':';
    public static final char DELVFT = '!';
    public static final char DELREF = '@';
    public static final char GETREFFILE = '#';
    public static final char ADDVFT = '%';
    public static final char GETVFT = '&';
    public static final char SETREFERENCE = '(';
    public static final char CANCELREFERENCE = ')';
    public static final char SHOWREF = '`';
    public static final char SHOWREFAT = '*';
    public static final char SHOWVFT = '=';
    public static final char SHOWVFTAT = '<';
    public static final char ADDVFTREADER = 'V';
    public static final char SHOWVFTREADERS = 'W';
    public static final char DELVFTREADER = 'Y';
    public static final char UPDVFT = '?';

    public static final char UNUSED6 = '6';
    public static final char UNUSED7 = '7';
    public static final char UNUSED8 = '8';
    public static final char UNUSED9 = '9';
    

    /* Komodo File commands. */
    public static final char CHANGETYPE = 'b';
    public static final char SHOWFILES = 's';
    public static final char AUTOSHOWFILES = 'S';
    public static final char GETFILES = 'g';
    public static final char AUTOGETFILES = 'G';
    public static final char GETFILEFROMVFT = '_';
    public static final char GETFILEFROMFS = '>';
    public static final char GETFILEOUTPUTSTREAM = '}';
    public static final char COMMENTFILE = 'c';
    public static final char ARCHIVENOTE = 'v';
    public static final char ADDFILE = 'a';
    public static final char ADDFILEANDREF = '{';
    public static final char REPLACEFILE = 'r';
    public static final char DELETEFILE = 'd';
    public static final char MAKECLEAN = 'm';
    public static final char IGNOREFILE = 'i';
    public static final char RENAMEFILE = 'n';
    public static final char SHOWCAPS = 'o';
    public static final char COMPUTECHECKSUM = 'k';
    public static final char CHECKFILES = 'f';
    public static final char CHECK = 'h';
    public static final char DISPLAY = 'p';
    public static final char ACCEPT = 't';
    public static final char SUBSCRIBEPUSH = '[';
    public static final char PING = 'l';
    public static final char REGISTERFILE = '3';
    public static final char UNREGISTERFILE = '4';

    /* Credential commands. */
    public static final char CREDLOGIN = 'u';
    public static final char CREDLIST = 'j';
    public static final char CREDLOGOUT = 'w';
    
    /* Misc commands */
    public static final char AUTHSERVERGROUPUSER = '~';
    public static final char NOOPERATION = '0';
    public static final char EXCHANGEPROPERTY = '2';
    
    
    /* Komodo File type commands. */
    public static final char LOCKFILETYPE = '-';
    public static final char UNLOCKFILETYPE = '+';
    public static final char SHOWTYPES = 'T';


    /* Komodo Admin commands. */
    public static final char ADDFTTOROLE = 'A';
    public static final char ADDUSERTOROLE = 'B';
    public static final char DSYNC = 'C';
    public static final char FSYNC = 'D';
    public static final char MAKEDOMAIN = 'E';
    public static final char ADDFT = 'F';
    public static final char ADDROLE = 'G';
    public static final char ADDUSER = 'H';
    public static final char REMOVEROLE = 'I';
    public static final char REMOVEUSER = 'J';
    public static final char RMFTFROMROLE = 'K';
    public static final char RMUSERFROMROLE = 'L';
    public static final char SHOWCONN = 'M';
    public static final char SHOWFT = 'N';
    public static final char SHOWMEM = 'O';
    public static final char SHOWPARAMS = 'P';
    public static final char SHOWSERVERS = 'Q';
    public static final char SHOWROLES = 'R';
    public static final char SHOWUSERS = 'S';
    public static final char SHUTDOWN = 'T';
    public static final char SHOWROLESFORFT = 'U';
    public static final char CHANGEPASSWORD = ',';
    public static final char SHOWROLESFORUSER = 'X';
    public static final char REMOVEFT = 'Z';
    public static final char HOTBOOT = ']';
    public static final char MOVEFILES = '^';
    public static final char MODIFYFT = '1';
    public static final char SHOWLOCKS = '5';
    
    
    /* Subscription modifiers */
    public static final char KILLSUBSCRIPTION = 'k';
    
    /* Komodo connection maintenance commands. */
    public static final char LOGIN = 'h';
    public static final char READY = 'e';
    public static final char QUIT = 'q';
   
    
    /* File register (REGISTERFILES) modifiers */
    //reuse NOMODIFIER = ' ' 
    public static final char REREGISTER    = 'r';
    public static final char REREGISTERALL = 'R'; 
    
    //---------------------------------------------------------------------
    
    /**
     * Converts from pre-protocol 3.0 command character to 3.0 and
     * later command string.
     * Note: Due to shared values, some ambiguities exist.  These
     * are resolved by defaulting to the meaning the server assumed
     * (since the other meaning was used exclusively by the client):
     * <pre>
     * 'h' = LOGIN, CHECK.              Assumed: LOGIN.
     * 'S' = SHOWUSERS, AUTOSHOWFILES.  Assumed: SHOWUSERS
     * 'G' = ADDROLE, AUTOGETFILES.     Assumed: ADDROLE
     * 'T' = SHUTDOWN, SHOWTYPES.       Assumed: SHUTDOWN.
     * </pre>
     */
    
    public static String getCommandString(char commandChar)
    {
        return getCommandString(commandChar, false);
        
    }
    
    //---------------------------------------------------------------------

    /**
     * Converts from pre-protocol 3.0 command character to 3.0 and
     * later command string.
     * Note: Due to shared values, some ambiguities exist.  
     * The boolean parameter indicates whether we should decide
     * the value based on if requester is a client (true) 
     * or server (false).
     * These
     * are resolved by defaulting to the meaning the server assumed
     * (since the other meaning was used exclusively by the client):
     * <pre>
     * 'h' = LOGIN, CHECK.              Server: LOGIN.     Client: CHECK
     * 'S' = SHOWUSERS, AUTOSHOWFILES.  Server: SHOWUSERS. Client: AUTOSHOWFILES
     * 'G' = ADDROLE, AUTOGETFILES.     Server: ADDROLE.   Client: AUTOGETFILES
     * 'T' = SHUTDOWN, SHOWTYPES.       Server: SHUTDOWN.  Client: SHOWTYPES
     * </pre>
     */
    
    public static String getCommandString(char commandChar, boolean isClient)
    {
        String cmdStr = null;
    
        switch(commandChar)
        {
            
            case ADDREF:  cmdStr = Constants.ADDREF + "";
            break;
            case DELVFT:  cmdStr = Constants.DELVFT + "";
            break;
            case DELREF:  cmdStr = Constants.DELREF + "";
            break;
            case GETREFFILE:  cmdStr = Constants.GETREFFILE + "";
            break;
            case ADDVFT:  cmdStr = Constants.ADDVFT + "";
            break;
            case GETVFT:  cmdStr = Constants.GETVFT + "";
            break;
            case SETREFERENCE:  cmdStr = Constants.SETREFERENCE + "";
            break;
            case CANCELREFERENCE:  cmdStr = Constants.CANCELREFERENCE + "";
            break;
            case SHOWREF:  cmdStr = Constants.SHOWREF + "";
            break;
            case SHOWREFAT:  cmdStr = Constants.SHOWREFAT + "";
            break;
            case ADDVFTREADER:  cmdStr = Constants.ADDVFTREADER + "";
            break;
            case SHOWVFTREADERS:  cmdStr = Constants.SHOWVFTREADERS + "";
            break;
            case DELVFTREADER:  cmdStr = Constants.DELVFTREADER + "";
            break;
            case UPDVFT:  cmdStr = Constants.UPDVFT + "";
            break;
                          
            //---------------------    
            //Filetype Commands
                          
            case CHANGETYPE:  cmdStr = Constants.CHANGETYPE + "";
            break;
            case SHOWFILES:  cmdStr = Constants.SHOWFILES + "";
            break;
            //case AUTOSHOWFILES:  cmdStr = Constants.AUTOSHOWFILES + "";
            //                break;
            case GETFILES:  cmdStr = Constants.GETFILES + "";
            break;
            //case AUTOGETFILES:  cmdStr = Constants.AUTOGETFILES + "";
            //              break;
            case GETFILEFROMVFT:  cmdStr = Constants.GETFILEFROMVFT + "";
            break;
            case GETFILEFROMFS:  cmdStr = Constants.GETFILEFROMFS + "";
            break;
            case GETFILEOUTPUTSTREAM:  cmdStr = Constants.GETFILEOUTPUTSTREAM + "";
            break;
            case COMMENTFILE:  cmdStr = Constants.COMMENTFILE + "";
            break;
            case ARCHIVENOTE:  cmdStr = Constants.ARCHIVENOTE + "";
            break;
            case ADDFILE:  cmdStr = Constants.ADDFILE + "";
            break;
            case ADDFILEANDREF:  cmdStr = Constants.ADDFILEANDREF + "";
            break;
            case REPLACEFILE:  cmdStr = Constants.REPLACEFILE + "";
            break;
            case DELETEFILE:  cmdStr = Constants.DELETEFILE + "";
            break;                          
            case MAKECLEAN:  cmdStr = Constants.MAKECLEAN + "";
            break;  
            case IGNOREFILE:  cmdStr = Constants.IGNOREFILE + "";
            break;
            case RENAMEFILE:  cmdStr = Constants.RENAMEFILE + "";
            break;
            case SHOWCAPS:  cmdStr = Constants.SHOWCAPS + "";
            break;
            case COMPUTECHECKSUM:  cmdStr = Constants.COMPUTECHECKSUM + "";
            break;
            case CHECKFILES:  cmdStr = Constants.CHECKFILES + "";
            break;
            //case CHECK:  cmdStr = Constants.CHECK + "";
            //break;
            case DISPLAY:  cmdStr = Constants.DISPLAY + "";
            break;
            case ACCEPT:  cmdStr = Constants.ACCEPT + "";
            break;
            case SUBSCRIBEPUSH:  cmdStr = Constants.SUBSCRIBEPUSH + "";
            break;
            case PING:  cmdStr = Constants.PING + "";
            break;
            case REGISTERFILE:  cmdStr = Constants.REGISTERFILE + "";
            break;
            case UNREGISTERFILE:  cmdStr = Constants.REPLACEFILE + "";
            break;
  
            //---------------------
            
            case CREDLOGIN:  cmdStr = Constants.CREDLOGIN + "";
            break;  
            case CREDLIST:  cmdStr = Constants.CREDLIST + "";
            break;
            case CREDLOGOUT:  cmdStr = Constants.CREDLOGOUT + "";
            break;
            case AUTHSERVERGROUPUSER:  cmdStr = Constants.AUTHSERVERGROUPUSER + "";
            break;
            case NOOPERATION:  cmdStr = Constants.NOOPERATION + "";
            break;
            case EXCHANGEPROPERTY:  cmdStr = Constants.EXCHANGEPROPERTY + "";
            break;
            case LOCKFILETYPE:  cmdStr = Constants.LOCKFILETYPE + "";
            break;
            case UNLOCKFILETYPE:  cmdStr = Constants.UNLOCKFILETYPE + "";
            break;
            //case SHOWTYPES:  cmdStr = Constants.SHOWTYPES + "";
            //break;
            
            //---------------------
            //Admin commands
            
            case ADDFTTOROLE:  cmdStr = Constants.ADDFTTOROLE + "";
            break;  
            case ADDUSERTOROLE:  cmdStr = Constants.ADDUSERTOROLE + "";
            break;
            case DSYNC:  cmdStr = Constants.DSYNC + "";
            break;
            case FSYNC:  cmdStr = Constants.FSYNC + "";
            break;
            case MAKEDOMAIN:  cmdStr = Constants.MAKEDOMAIN + "";                        
            break;
            case ADDFT:  cmdStr = Constants.ADDFT + "";
            break;
            case ADDROLE:  cmdStr = (isClient) ? Constants.AUTOGETFILES : 
                                                 Constants.ADDROLE;
            break;
            case ADDUSER:  cmdStr = Constants.ADDUSER + "";
            break;
            case REMOVEROLE:  cmdStr = Constants.REMOVEROLE + "";
            break;
            case REMOVEUSER:  cmdStr = Constants.REMOVEUSER + "";
            break;
            case RMFTFROMROLE:  cmdStr = Constants.RMFTFROMROLE + "";
            break;  
            case RMUSERFROMROLE:  cmdStr = Constants.RMUSERFROMROLE + "";
            break;
            case SHOWCONN:  cmdStr = Constants.SHOWCONN + "";
            break;
            case SHOWFT:  cmdStr = Constants.SHOWFT + "";
            break;
            case SHOWMEM:  cmdStr = Constants.SHOWMEM + "";
            break;
            case SHOWPARAMS:  cmdStr = Constants.SHOWPARAMS + "";
            break;
            case SHOWSERVERS:  cmdStr = Constants.SHOWSERVERS + "";
            break;
            case SHOWROLES:  cmdStr = Constants.SHOWROLES + "";
            break;
            case SHOWUSERS:  cmdStr = (isClient) ? Constants.AUTOSHOWFILES :
                                                   Constants.SHOWUSERS;
            break;
            case SHUTDOWN:  cmdStr = (isClient) ? Constants.SHOWTYPES : 
                                                  Constants.SHUTDOWN ;
            break;  
            case SHOWROLESFORFT:  cmdStr = Constants.SHOWROLESFORFT + "";
            break;
            case CHANGEPASSWORD:  cmdStr = Constants.CHANGEPASSWORD + "";
            break;
            case SHOWROLESFORUSER:  cmdStr = Constants.SHOWROLESFORUSER + "";
            break;
            case REMOVEFT:  cmdStr = Constants.REMOVEFT + "";
            break;
            case HOTBOOT:  cmdStr = Constants.HOTBOOT + "";
            break;
            case MOVEFILES:  cmdStr = Constants.MOVEFILES + "";
            break;
            case MODIFYFT:  cmdStr = Constants.MODIFYFT + "";
            break;
            case SHOWLOCKS:  cmdStr = Constants.SHOWLOCKS + "";
            break;

            //---------------------
            
            case LOGIN:  cmdStr = (isClient) ? Constants.CHECK :
                                               Constants.LOGIN;
            break;
            case READY:  cmdStr = Constants.READY + "";
            break;
            case QUIT:  cmdStr = Constants.QUIT + "";
            break;

        }
        
        return cmdStr;
    }
    
    //---------------------------------------------------------------------
    
}
