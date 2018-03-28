/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

import jpl.mipl.mdms.FileService.komodo.api.ServerInfo;
import jpl.mipl.mdms.FileService.komodo.api.ServerProxy;
import jpl.mipl.mdms.FileService.komodo.api.Session;
import jpl.mipl.mdms.FileService.komodo.api.SessionException;
import jpl.mipl.mdms.utils.logging.Logger;
import java.util.Date;

/**
 * This class contains all actions that can be take on a VFT by a user.
 * 
 * @author J. Jacobson, G. Turek, T. Huang
 * @version $Id: VFT.java,v 1.10 2004/12/03 01:06:33 txh Exp $
 */
public class VFT {
   //implements Constants {
   private ServerInfo _serverInfo;
   private ServerProxy _proxy;
   private Session _session;
   private String _serverName = "vft";
   private final Logger _logger = Logger.getLogger(VFT.class.getName());

   /**
    * Constructor
    * 
    * @param session a transfer session, for maintining file types and
    *           connections.
    * @param mustBeAdmin if true, connect on admin port
    * @throws SessionException when session failure
    */
   public VFT(Session session, boolean mustBeAdmin) throws SessionException {
      this._session = session;

      this._serverInfo = session.getServerInfo(session.getDefaultGroup(),
            this._serverName);
      if (this._serverInfo == null) {
         throw new SessionException("Server \"" + this._serverName
               + "\" not found in domain.", Constants.NO_SUCH_SERVER);
      }

      // If there is no server proxy, create one.
      this._proxy = _serverInfo.getProxy();
      if (this._proxy == null) {
         this._logger.trace("No proxy, so make one.");
         // Blocks.
         this._serverInfo.setProxy(new ServerProxy(session, this._serverInfo,
               mustBeAdmin));
         this._proxy = this._serverInfo.getProxy();
      } else {
         // Increment the reference count on the server proxy. Note: the
         // server proxy refenence count is initialized to 1 in its
         // constructor.
         this._proxy.incrementRefCount();
      }

      // resume transfer for getVFT is not yet supported
   }

   /**
    * Accessor method to return the vft server name
    * 
    * @return the vft server name
    */
   public final String getServerName() {
      return this._serverName;
   }

   /**
    * Method to get all files associated with a VFT. Thes files are placed under
    * the current directory and take the name of the reference. If files already
    * exist, new files follow the file-replace policy set by Session options.
    * See Session.setOption().
    * 
    * @param vft the virtual file type
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int getVFT(String vft) throws SessionException {
      Request cmd = new Request(Constants.GETVFT, new String[] { vft });

      cmd.setDirectory(this._session.getDir());
      // Use the current session's director setting.
      cmd.setModifier(Constants.LATEST);
      // Just get the latest valid reference.
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get all files that were associated with a VFT at a particular
    * date. These files are placed under the current directory and take the name
    * of the reference. If files already exist, new files follow the
    * file-replace policy set by Session options. See Session.setOption().
    * 
    * @param vft the virtual file type
    * @param datetime the date these files were in effect.
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int getVFTAt(String vft, Date datetime) throws SessionException {
      Request cmd = new Request(Constants.GETVFT, new String[] { vft },
            new Date[] { datetime });
      cmd.setDirectory(this._session.getDir());
      // Use the current session's director setting.
      cmd.setModifier(Constants.ATTIMEOF); // This is a date command.
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get the file associated with a virtual file type reference.
    * 
    * @param vft the vitual file type
    * @param reference the reference name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int getReference(String vft, String reference)
         throws SessionException {
      Request cmd = new Request(Constants.GETREFFILE, new String[] { vft,
            reference });
      cmd.setModifier(Constants.LATEST);
      // Just get the latest valid reference.
      return (this._proxy.put(cmd));
   }

   /**
    * Method to get the file associated with a virtual file type reference at a
    * specified date.
    * 
    * @param vft the virtual file type
    * @param reference the reference name
    * @param datetime the date filter
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int getReferenceAt(String vft, String reference, Date datetime)
         throws SessionException {
      Request cmd = new Request(Constants.GETREFFILE, new String[] { vft,
            reference }, new Date[] { datetime });
      cmd.setModifier(Constants.ATTIMEOF); // This is a date command.
      return (this._proxy.put(cmd));
   }

   /**
    * Method to set a reference to point to a new file on next vftUpdate.
    * 
    * @param vft the virtual file type
    * @param reference the reference name
    * @param fileType the name of file type
    * @param fileName the name of file to reference
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int setReference(String vft, String reference, String fileType,
         String fileName) throws SessionException {
      Request cmd;
      if (fileType != null) {
         if (fileName == null)
            throw new SessionException("Must specify file name.",
                  Constants.MISSINGARG);
         cmd = new Request(Constants.SETREFERENCE, new String[] { vft,
               reference, fileType, fileName });
      } else {
         cmd = new Request(Constants.SETREFERENCE, new String[] { vft,
               reference });
      }
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show information about the supplied vft. If vft is null, show
    * information about all vfts. If date is not null, show information for the
    * supplied date time.
    * 
    * @param vft the virtual file type
    * @param datetime the optional date filter
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int showVFT(String vft, Date datetime) throws SessionException {
      Request cmd;
      if (datetime != null) {
         if (vft != null)
            cmd = new Request(Constants.SHOWVFTAT, new String[] { vft },
                  new Date[] { datetime });
         else
            cmd = new Request(Constants.SHOWVFTAT, null,
                  new Date[] { datetime });
      } else {
         if (vft != null)
            cmd = new Request(Constants.SHOWVFT, new String[] { vft });
         else
            cmd = new Request(Constants.SHOWVFT);
      }
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show information about the supplied vft references. If ref is
    * null, show information about all the vft's references. If date is not
    * null, show information for the supplied date time.
    * 
    * @param vft the virtual file type
    * @param ref the reference name
    * @param datetime the optional date filter
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int showReference(String vft, String ref, Date datetime)
         throws SessionException {
      Request cmd;
      if (datetime != null) {
         if (ref != null)
            cmd = new Request(Constants.SHOWREFAT, new String[] { vft, ref },
                  new Date[] { datetime });
         else
            cmd = new Request(Constants.SHOWREFAT, new String[] { vft },
                  new Date[] { datetime });
      } else {
         if (ref != null)
            cmd = new Request(Constants.SHOWREF, new String[] { vft, ref });
         else
            cmd = new Request(Constants.SHOWREF, new String[] { vft });
      }
      return (this._proxy.put(cmd));
   }

   /**
    * Method to cancel a reference change before doing a vft update.
    * 
    * @param vft the virtual file type
    * @param reference the reference name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int cancelReference(String vft, String reference)
         throws SessionException {
      Request cmd = new Request(Constants.CANCELREFERENCE, new String[] { vft,
            reference });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to add vft reader
    * 
    * @param vft the virtual file type
    * @param user the vft reader name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int addVFTReader(String vft, String user)
         throws SessionException {
      Request cmd = new Request(Constants.ADDVFTREADER, new String[] { vft,
            user });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to show readers allowed to access the supplied vft.
    * 
    * @param vft the virtual file type
    * @param user the vft reader name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int showVFTReaders(String vft, String user)
         throws SessionException {
      Request cmd;
      if (user != null)
         cmd = new Request(Constants.SHOWVFTREADERS, new String[] { vft, user });
      else
         cmd = new Request(Constants.SHOWVFTREADERS, new String[] { vft });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to remove a reader from the supplied vft
    * 
    * @param vft the virtual file type
    * @param user the vft reader.
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int delVFTReader(String vft, String user)
         throws SessionException {
      Request cmd = new Request(Constants.DELVFTREADER, new String[] { vft,
            user });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to close this vft channel. Do this by appending the close command
    * at the head of the requests queue. The ServerProxy will then remove any
    * admin requests for from the request queue. If all references, file types
    * and admin for this server have been closed, then the connection to the
    * server will be gracefully closed.
    * 
    * @return the transaction id for tracking this command.
    */
   public final int close() {
      Request cmd = new Request(Constants.QUIT, (String[]) null);
      this._logger.trace("Queing requested command " + cmd.getCommand());
      return (this._proxy.putExpedited(cmd));
   }

   /**
    * Method to add a vft to the database. This VFT will be created on the
    * server associated with this file type.
    * 
    * @param vft the virtual file type
    * @param title the optional title, may be null.
    * @param notify if true, notify a list of email addresses on update vft
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int addVFT(String vft, String title, boolean notify)
         throws SessionException {
      // Format notify token
      String notifyTok;
      if (notify)
         notifyTok = "notify";
      else
         notifyTok = "nonotify";

      // Just use single quote to delimit the start of a title. No problem
      // with embedded quotes. Works as long as commands only need one
      // free-form text attribute.
      Request cmd;
      if (title != null)
         cmd = new Request(Constants.ADDVFT, new String[] { vft, notifyTok,
               "\"" + title });
      else
         cmd = new Request(Constants.ADDVFT, new String[] { vft, notifyTok });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to delete a the current virutal file type
    * 
    * @param vft the virtual file type
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int deleteVFT(String vft) throws SessionException {
      Request cmd = new Request(Constants.DELVFT, new String[] { vft });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to create the a reference in the supplied vft.
    * 
    * @param vft the virtual file type
    * @param reference new reference name
    * @param link the optional link
    * @param comment the optional comment
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int addRef(String vft, String reference, String link,
         String comment) throws SessionException {
      Request cmd;
      String[] args;
      if (comment != null) {
         if (link != null)
            args = new String[] { vft, reference, link, "\"" + comment };
         else
            args = new String[] { vft, reference, "\"" + comment };
      } else {
         if (link != null)
            args = new String[] { vft, reference, link };
         else
            args = new String[] { vft, reference };
      }
      cmd = new Request(Constants.ADDREF, args);
      return (this._proxy.put(cmd));
   }

   /**
    * Method to delete the a reference within the supplied virtual file type
    * 
    * @param vft the virtual file type
    * @param ref the reference name
    * @return the transaction id for tracking this command.
    * @throws SessionException when session failure
    */
   public final int deleteRef(String vft, String ref) throws SessionException {
      Request cmd = new Request(Constants.DELREF, new String[] { vft, ref });
      return (this._proxy.put(cmd));
   }

   /**
    * Method to update the current VFT.
    * 
    * @param vft the virtual file type
    * @param comment the optional comment (may be null)
    * @return the transaction id for tracking this command.
    */
   public final int update(String vft, String comment) {
      Request cmd;
      if (comment != null)
         cmd = new Request(Constants.UPDVFT,
               new String[] { vft, "\"" + comment });
      else
         cmd = new Request(Constants.UPDVFT, new String[] { vft });
      return (this._proxy.put(cmd));
   }
}