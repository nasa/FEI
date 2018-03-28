/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.api;

/**
 * This class contains user's capabilities for a file type.
 * @version $Id: Capability.java,v 1.8 2009/08/07 18:46:22 ntt Exp $
 */
public class Capability 
{
   protected String _name = "";
   protected long _capabilities;

   /**
    * Constructor
    *
    * @param name the string object name
    * @param capabilities the long capabilities bit vector.
    */
   public Capability(String name, long capabilities) {
      this._name = name;
      this._capabilities = capabilities;
   }

   /**
    * Accessor method to get the long representation of the capabilities. 
    *
    * @return the long representation of capabilities
    */
   public long getCapabilitiesLong() {
       return this._capabilities;
   }
   
   /**
    * Accessor method to get the name of the object associated with this set of
    * capabilities.
    *
    * @return the object name
    */
   public final String getName() {
      return this._name;
   }

   /**
    * Accessor method to get the string representation of the capabilities.  For
    * example, "list,get,add".
    *
    * @return the string contains the list of capabilities
    */
   public String getCapabilities() {
      StringBuffer capDisplay = new StringBuffer("");
      if (this.isDefined(Constants.GET))
         capDisplay.append("get, ");
      if (this.isDefined(Constants.ADD))
         capDisplay.append("add, ");
      if (this.isDefined(Constants.REPLACE))
         capDisplay.append("replace, ");
      if (this.isDefined(Constants.DELETE))
         capDisplay.append("delete, ");
      if (this.isDefined(Constants.RENAME))
         capDisplay.append("rename, ");
      if (this.isDefined(Constants.OFFLINE))
         capDisplay.append("offline, ");
      if (this.isDefined(Constants.PUSHSUBSCRIBE))
          capDisplay.append("push-subscribe, ");
      if (this.isDefined(Constants.QAACCESS))
         capDisplay.append("qaaccess, ");
      if (this.isDefined(Constants.ARCHIVE))
         capDisplay.append("archive, ");
      if (this.isDefined(Constants.RECEIPT))
         capDisplay.append("receipt, ");
      if (this.isDefined(Constants.REGISTER))
          capDisplay.append("register, ");
      if (this.isDefined(Constants.REPLICATE))
          capDisplay.append("replicate, ");
      if (this.isDefined(Constants.SUBTYPE))
         capDisplay.append("subtype, ");
      if (this.isDefined(Constants.LOCKTYPE))
         capDisplay.append("locktype, ");
      if (this.isDefined(Constants.VFT))
         capDisplay.append("vft, ");

      // Trim off final ", ".
      if (capDisplay.length() > 2)
          capDisplay.delete(capDisplay.length() - 3, capDisplay.length());
      
      return capDisplay.toString();
      //return capDisplay.substring(0, capDisplay.length() - 2);
   }

   /**
    * Method to return true if the capability is defined for this session and
    * file type.
    *
    * @param cmdType the command type
    * @return true if the capability is defined, false otherwise.
    */
   public boolean isDefined(long cmdType) {
      return ((this._capabilities & cmdType) > 0 ? true : false);
   }
   
   /**
    * Method to return a string representation of this instance
    *
    * @return String representation of Capability.
    */
   public String toString()
   {
       return this._name + ": " + this.getCapabilities();
   }
}
