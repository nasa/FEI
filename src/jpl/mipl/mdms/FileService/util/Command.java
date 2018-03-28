/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;

/**
 * This class mangages command information, use for hashing commands.
 * Keeps track of commands, help information, and methods for implementing
 * commands.
 *
 * @author J. Jacobson
 * @version $Id: Command.java,v 1.3 2003/09/09 00:32:34 rap Exp $
 */
public class Command {
    private String[] _cmdNames; // Array of names to map to a method.
    private String _methodName; // Java instance method to execute.
    private String _argList;
    private Method _method;
    private Class[] _params; // Array of classes to pass to method.
    private Matcher[] _regExp; // Compiled regular expressions for args.

    /**
     * Constructor creates a new command object, with a method map.
     *
     * @param obj object class containing method for lookup
     * @param cmdNames names of the commands to associate with a method
     * @param methodName actual name of the method
     * @param argList the input arguments to the methods text
     * @param params array of class instances representing parameters from left
     * to right.
     * @param regExp array of compiled regular expression matcher objects for
     * decoding arguments.
     * @throws NoSuchMethodException when reflection failed
     */
    public Command(
        Object obj,
        String[] cmdNames,
        String methodName,
        String argList,
        Class[] params,
        Matcher[] regExp)
        throws NoSuchMethodException {
        this._cmdNames = cmdNames;
        this._methodName = methodName;
        this._regExp = regExp;
        this._params = params;
        this._argList = argList;

        try {
            Class c = obj.getClass();
            // Now, get the method.
            this._method = c.getMethod(methodName, params);
        } catch (NoSuchMethodException e) {
            System.err.println("Cannot find method \"" + methodName + "\"");
            throw e;
        }
    }

    /**
     * Accessor method to get command names.
     *
     * @return command name array.
     */
    public String[] getCommandNames() {
        return this._cmdNames;
    }

    /**
     * Execute method with no input argument array.
     *
     * @param interpreter the object to execute the registered method.
     * @return the result of the method invocation.
     * @throws IllegalAccessException when access failure
     * @throws IllegalArgumentException when argument failure
     * @throws InvocationTargetException when target failure
     */
    Object executeMethod(Object interpreter)
        throws
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        return this._method.invoke(interpreter, null);
    }

    /**
     * Accessor method to get regular expression.  Allows interpreter to decode
     * command line to create aguments to pass to the execute function call.
     *
     * @return the internal regular expression matcher object array.
     */
    Matcher[] getRegExp() {
        return this._regExp;
    }

    /**
     * Accessor method to get params array.
     *
     * @return the parameter class object array reference.
     */
    Class[] getParams() {
        return this._params;
    }

    /**
     * Execute method with passed argument array.
     *
     * @param interpreter the object context for method invocation.
     * @param args the array of arguments
     * @return the result object reference
     * @throws IllegalAccessException when access failure
     * @throws IllegalArgumentException when argument failure
     * @throws InvocationTargetException when target failure
     */
    Object executeMethod(Object interpreter, Object[] args)
        throws
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        return this._method.invoke(interpreter, args);
    }

    /**
     * Accessor method to get arglist syntax.  Used for help info forming
     * syntax error messages.
     *
     * @return the method name.
     */
    final String getArgList() {
        return this._argList;
    }
}
