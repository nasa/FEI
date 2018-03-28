/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.util;

import java.util.Hashtable;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.api.Constants;

/**
 *  Manipulation of capabilities
 *
 *  @author gt
 *  @version $Id: CapsUtil.java,v 1.13 2009/09/16 20:48:43 awt Exp $
 */
public class CapsUtil implements Constants
{
    
    //---------------------------------------------------------------------
    
    /**
     * Get the string representation of the capabilities.  For
     * example, "list,get,add".
     *
     * @param capabilities the capabilities mask
     * @return the capabilities string.
     */
    public static String getCapabilitiesAsString(int capabilities) {
        StringBuffer caps = new StringBuffer("");
        if (CapsUtil.isDefined(capabilities, Constants.ADD))
            caps.append("|add");
        if (CapsUtil.isDefined(capabilities, Constants.ARCHIVE))
            caps.append("|archive");
        if (CapsUtil.isDefined(capabilities, Constants.DELETE))
            caps.append("|delete");
        if (CapsUtil.isDefined(capabilities, Constants.GET))
            caps.append("|get");
        if (CapsUtil.isDefined(capabilities, Constants.LOCKTYPE))
            caps.append("|locktype");
        if (CapsUtil.isDefined(capabilities, Constants.OFFLINE))
            caps.append("|offline");
        if (CapsUtil.isDefined(capabilities, Constants.PUSHSUBSCRIBE))
            caps.append("|push-subscribe");
        if (CapsUtil.isDefined(capabilities, Constants.QAACCESS))
            caps.append("|qaaccess");
        if (CapsUtil.isDefined(capabilities, Constants.RECEIPT))
            caps.append("|receipt");
        if (CapsUtil.isDefined(capabilities, Constants.REGISTER))
            caps.append("|register");
        if (CapsUtil.isDefined(capabilities, Constants.RENAME))
            caps.append("|rename");
        if (CapsUtil.isDefined(capabilities, Constants.REPLACE))
            caps.append("|replace");
        if (CapsUtil.isDefined(capabilities, Constants.REPLICATE))
            caps.append("|replicate");
        if (CapsUtil.isDefined(capabilities, Constants.SUBTYPE))
            caps.append("|subtype");
        if (CapsUtil.isDefined(capabilities, Constants.VFT))
            caps.append("|vft");

        if (caps.length() > 0)
            return caps.substring(1);
        else
            return "No capabilities";
    }

    //---------------------------------------------------------------------
    
    /**
     * Method to convert the input capability list into a capability mask
     *
     * @param capabilities the capability list
     * @return the capability mask
     * @throws NoSuchFieldException when field not found
     */
    public static int getCapabilitiesAsInt(Vector capabilities)
        throws NoSuchFieldException {
        String tmp;
        Hashtable caps = new Hashtable();
        // Add also gives GET capabilities.
        caps.put("add", new Integer(Constants.ADD + Constants.GET));
        caps.put("archive", new Integer(Constants.ARCHIVE));
        caps.put("delete", new Integer(Constants.DELETE));
        caps.put("get", new Integer(Constants.GET));
        caps.put("locktype", new Integer(Constants.LOCKTYPE));
        caps.put("offline", new Integer(Constants.OFFLINE));
        caps.put("push-subscribe", new Integer(Constants.PUSHSUBSCRIBE));
        caps.put("qaaccess", new Integer(Constants.QAACCESS));
        caps.put("receipt", new Integer(Constants.RECEIPT));
        caps.put("register", new Integer(Constants.REGISTER));
        caps.put("rename", new Integer(Constants.RENAME));
        // Replace gives ADD and GET too.
        caps.put("replace", new Integer(Constants.REPLACE + Constants.ADD + 
                                        Constants.GET));
        caps.put("replicate", new Integer(Constants.REPLICATE));
        caps.put("subtype", new Integer(Constants.SUBTYPE));
        caps.put("vft", new Integer(Constants.VFT));

        int icaps = 0;
        for (int i = 0; i < capabilities.size(); i++) {
            tmp = ((String) capabilities.elementAt(i)).toLowerCase();
            if (!caps.containsKey(tmp))
                throw new NoSuchFieldException(
                    "Unrecognized capability " + tmp);
            else
                icaps |= ((Integer) caps.get(tmp)).intValue();
        }
        return icaps;
    }

    
    //---------------------------------------------------------------------

    
    /**
     * Method to convert the input capability string into a capability mask.
     * Same as calling <code>CapsUtil.getCapabilitiesAsInt(capList, '|')</code>
     *
     * @param capabilities string representation of the capability list,
     * separated by a delimiter
     * @return the capability mask
     * @throws NoSuchFieldException when field not found
     */
    public static int getCapabilitiesAsInt(String capList)
                                    throws NoSuchFieldException 
    {
        return getCapabilitiesAsInt(capList, '|');
    }

    //---------------------------------------------------------------------
    
    /**
     * Method to convert the input capability string into a capability mask
     *
     * @param capabilities string representation of the capability list,
     * separated by a delimiter
     * @param delim The delimitting character
     * @return the capability mask
     * @throws NoSuchFieldException when field not found
     */
    public static int getCapabilitiesAsInt(String capList, char delim)
                                           throws NoSuchFieldException 
    {
        Vector v = new Vector();
        if (capList.indexOf(delim) != -1)
        {
            String splitRegex = "\\s*" + delim + "\\s*";
            String[] caps = capList.split( splitRegex );
            for (int i = 0; i < caps.length; ++i)
            {
                if (caps[i] != null && !caps[i].equals(""))
                    v.add(caps[i]);
            }
        }
        else
        {
            v.add(capList);
        }
        return getCapabilitiesAsInt(v);
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true if the capability is defined
     *
     * @param capabilities - integer representation of capabilities
     * @param  type a specific capability
     * @return true if the capability is defined, false othewise.
     */
    public static boolean isDefined(int capabilities, int type) {
        return ((capabilities & type) > 0 ? true : false);
    }
    
    //---------------------------------------------------------------------
}
