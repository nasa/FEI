/******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights re served
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 *****************************************************************************/
package jpl.mipl.mdms.FileService.util;

/**
 * Implement class for handling Switches, booleans whose meanings
 * are "on" or "off". Used to map strings "on" and "off" to boolean
 * values true or false.
 *
 * @author J. Jacobson
 * @version $Id: Switch.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class Switch {
    private boolean _value;

    /**
     * Allocates a Switch object representing the value argument.
     *
     * @param value to turn on or off
     */
    public Switch(boolean value) {
        this._value = value;
    }

    /**
     * Returns a String representation of the value of the Switch object.
     *
     * @return "on" or "off".
     */
    public String toString() {
        return (this._value) ? "on" : "off";
    }

    /**
     * Returns a Switch object with a value represented by the
     * specified String.
     *
     * @param value "on" => true, null and all other strings, false.
     * @return a Swing object instance
     */
    public static Switch valueOf(String value) {
        if (value != null && value.equalsIgnoreCase("on"))
            return new Switch(true);
        return new Switch(false);
    }
}
