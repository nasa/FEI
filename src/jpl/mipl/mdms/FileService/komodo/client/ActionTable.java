/*
 * Created on Jan 7, 2005
 */
package jpl.mipl.mdms.FileService.komodo.client;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import jpl.mipl.mdms.FileService.komodo.api.Constants;

/**
 * <b>Purpose: </b> Static lookup table to map between action ids and names.
 * 
 * <PRE>
 *   int addId = Constants.ADDFILE; 
 *   String notifyName = "komodo.util.notify";
 *   String addName = ActionTable.toName(addId); 
 *   int notifyId = ActionTable.toId(notifyName);
 * </PRE>
 * 
 * <PRE>
 * Copyright 2005, California Institute of Technology. ALL RIGHTS RESERVED. U.S.
 * Government Sponsorship acknowledge. 2005.
 * </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History : </B> ----------------------
 * 
 * <B>Date             Who         What </B>
 * ----------------------------------------------------------------------------
 * 01/07/2005       Nick        Initial Release
 * ============================================================================
 * </PRE>
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: ActionTable.java,v 1.7 2009/08/07 01:00:47 ntt Exp $
 *  
 */

public class ActionTable {
   protected static final Map _idToNameTable = new Hashtable();
   protected static final Map _nameToIdTable = new Hashtable();

   //---------------------------------------------------------------------

   static {
      initTables();
   }

   //---------------------------------------------------------------------

   protected static void initTables() {
      //init the name to id table
      _nameToIdTable.put("komodo.util.add",           Constants.ADDFILE);
      _nameToIdTable.put("komodo.util.accept",        Constants.ACCEPT);
      _nameToIdTable.put("komodo.util.get",           Constants.GETFILES);
      _nameToIdTable.put("komodo.util.subscribe",     Constants.AUTOGETFILES);
      _nameToIdTable.put("komodo.util.notify",        Constants.AUTOSHOWFILES);
      _nameToIdTable.put("komodo.util.crc",           Constants.COMPUTECHECKSUM);
      _nameToIdTable.put("komodo.util.checkfiles",    Constants.CHECKFILES);
      _nameToIdTable.put("komodo.util.check",         Constants.CHECK);
      _nameToIdTable.put("komodo.util.delete",        Constants.DELETEFILE);
      _nameToIdTable.put("komodo.util.display",       Constants.DISPLAY);
      _nameToIdTable.put("komodo.util.makeclean",     Constants.MAKECLEAN);
      _nameToIdTable.put("komodo.util.list",          Constants.SHOWFILES);
      _nameToIdTable.put("komodo.util.rename",        Constants.RENAMEFILE);
      _nameToIdTable.put("komodo.util.replace",       Constants.REPLACEFILE);
      _nameToIdTable.put("komodo.util.reference",     Constants.SETREFERENCE);
      _nameToIdTable.put("komodo.util.comment",       Constants.COMMENTFILE);
      _nameToIdTable.put("komodo.util.showtypes",     Constants.SHOWTYPES);
      _nameToIdTable.put("komodo.util.login",         Constants.CREDLOGIN);
      _nameToIdTable.put("komodo.util.credlist",      Constants.CREDLIST);
      _nameToIdTable.put("komodo.util.logout",        Constants.CREDLOGOUT);
      _nameToIdTable.put("komodo.util.register",      Constants.REGISTERFILE);
      _nameToIdTable.put("komodo.util.unregister",    Constants.UNREGISTERFILE);
      _nameToIdTable.put("komodo.util.locktype",      Constants.LOCKFILETYPE);
      _nameToIdTable.put("komodo.util.unlocktype",    Constants.UNLOCKFILETYPE);
      _nameToIdTable.put("komodo.util.changepassword",Constants.CHANGEPASSWORD);
      
      //init id to name table
      Iterator it = _nameToIdTable.keySet().iterator();
      while (it.hasNext()) {
         String key = (String) it.next();
         _idToNameTable.put(_nameToIdTable.get(key), key);
      }
   }

   //---------------------------------------------------------------------

   /**
    * Returns action name associated with the action id parameter.
    * 
    * @param actionId Action id
    * @return Associated name of action, or null if not found.
    */

   public static String toName(String actionId) 
   {
      String name = null;
      if (_idToNameTable.containsKey(actionId))
         name = (String) _idToNameTable.get(actionId);
      return name;
   }

   //---------------------------------------------------------------------

   /**
    * Returns action id associated with the action name parameter.
    * 
    * @param name Action name
    * @return Associated id of action, or Constants.NOOPERATION if not
    *         found.
    */

   public static String toId(String name) {
      String actionId = Constants.NOOPERATION;
      
      if (_nameToIdTable.containsKey(name))
         actionId = (String) _nameToIdTable.get(name);

      return actionId;
   }

   //---------------------------------------------------------------------
}