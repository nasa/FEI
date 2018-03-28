/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 *  Generic command interpreter.
 *
 *  @author J. Jacobson
 *  @version $Id: Interpreter.java,v 1.2 2003/09/09 00:32:35 rap Exp $
 */
public class Interpreter {
    private Hashtable _commandDict;
    private int _maxCharMatch;
    private int _oldMaxCharMatch = -1;

    /**
     * Constructor
     * @param maxCharMatch the maximum number of characters in
     * commands involved for match
     * @param commands the array of Commands.  May be null.
     */
    public Interpreter(int maxCharMatch, Command[] commands) {
        this._maxCharMatch = maxCharMatch;
        this._commandDict = new Hashtable();
        if (commands != null) {
            for (int i = 0; i < commands.length; i++) {
                this.loadCommand(commands[i]);
            }
        }
    }

    /**
     * Load command and aliases into hash.  The canonical form of the hash key
     * is a lowercase maxCharMatch sized string.  Made public so commands can
     * be added dynamically, say for creating command aliases.
     *
     * @param cmdInfo the Command information object with method bindings and
     * reg expressions.
     */
    public void loadCommand(Command cmdInfo) {
        String[] names;

        names = cmdInfo.getCommandNames();
        for (int i = 0; i < names.length; i++) {
            int hashSize =
                (this._maxCharMatch > names[i].length())
                    ? names[i].length()
                    : this._maxCharMatch;
            this._commandDict.put(
                names[i].substring(0, hashSize).toLowerCase(),
                cmdInfo);
        }
    }

    /**
     * Load an array of commands and aliases into hash.
     *
     * @param commands an array of Command objects
     */
    public void loadCommands(Command[] commands) {
        for (int i = 0; i < commands.length; i++) {
            this.loadCommand(commands[i]);
        }
    }

    /**
     * Parse arguments and execute a command bases on a string input.
     *
     * @param context the object to execute the input command.
     * @param command the input command string
     * @return the result object
     * @throws NoSuchMethodException when reflection fails
     * @throws IllegalAccessException when access fails
     * @throws InvocationTargetException when target fails
     * @throws IllegalArgumentException when argument failure
     */
    public Object exec(Object context, String command)
        throws
            NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException,
            IllegalArgumentException {
        Object[] callValues = null;
        StringTokenizer st = new StringTokenizer(command);
        int tokenCount = st.countTokens();
        if (tokenCount < 1)
            throw new IllegalArgumentException("No command specified");
        // Use up to maxCharMatch characters of the command only.
        String name = st.nextToken();
        String commandName = name;
        String cmdKey =
            commandName.length() <= this._maxCharMatch
                ? commandName.toLowerCase()
                : commandName.substring(0, this._maxCharMatch).toLowerCase();
        Command cmdInfo = (Command) this._commandDict.get(cmdKey);
        //Cludge work around for admin tool where maxCharMatch gets increased
        //but the util commands went in a shorter cmdKey
        if (cmdInfo == null) {
            if (this._oldMaxCharMatch > -1) {
                commandName = name;
                cmdKey =
                    commandName.length() <= this._oldMaxCharMatch
                        ? commandName.toLowerCase()
                        : commandName
                            .substring(0, this._oldMaxCharMatch)
                            .toLowerCase();
                cmdInfo = (Command) _commandDict.get(cmdKey);
                if (cmdInfo == null)
                    throw new IllegalArgumentException(
                        "Unrecognized command: " + commandName);
            } else
                throw new IllegalArgumentException(
                    "Unrecognized command: " + commandName);
        }
        // If there is a regular expression associated with the command, parse
        // the arguments.
        Matcher[] regExp = cmdInfo.getRegExp();
        boolean foundMatch = false;
        if (regExp != null) {
            /*
             * Note: try to match all the expressions in the array.  This will
             * reset previous commands, so the getParam(1) check will be effective.
             */
            for (int i = 0; i < regExp.length; i++) {
                regExp[i].reset(command);
                if (regExp[i].find() == true) {
                    // If function has args, then load them.  Do this for the
                    // first match only.
                    foundMatch = true;
                }
            }
            if (foundMatch == false) {
                throw new IllegalArgumentException(cmdInfo.getArgList());
            }
        }
        // Now, execute the method.  The method will de-reference arguments
        // from the compile regular expression, using re.getParend().
        return cmdInfo.executeMethod(context, callValues);
    }

    /**
     *  Set number of characters in commands to match against
     *
     *  @param nchar the number of characters in commands to match against
     */
    public final void setMaxCharMatch(int nchar) {
        this._oldMaxCharMatch = this._maxCharMatch;
        this._maxCharMatch = nchar;
    }
}
