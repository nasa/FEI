/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *  Test driver for Interpreter class.
 *
 *  @author J. Jacobson
 *  @version $Id: InterpreterDriver.java,v 1.3 2003/09/09 00:32:35 rap Exp $
 */
public class InterpreterDriver {
    private String _prompt = "";

    //Processing variables
    private static String _ttyInput;
    private static Command[] _commands;
    private static Interpreter _interpreter;
    private static final int _CMDMATCHCOUNT = 4;
    // Max number of characters used for lookup.

    private Matcher _oneArg;
    private Matcher _oneArgOneString;
    private Matcher _twoArg;
    private Matcher _oneDate;
    private Matcher _twoDate;

    /**
     * Constructor to populate the list of command strings.
     *
     * @throws NoSuchMethodException when input method not found.
     */
    public InterpreterDriver() throws NoSuchMethodException {
        try {
            // Note: Be sure include the command name in the parse.  This
            // is useful for printing out usage using a command name or
            // a command alias.
            this._oneArg =
                Pattern.compile("^\\s*(\\w+)\\s+(\\w+)\\s*$").matcher("");
            this._oneArgOneString =
                Pattern.compile("^\\s*(\\w+)\\s+(.+)\"(.*)\"\\s*$").matcher("");
            this._twoArg =
                Pattern.compile(
                    "^\\s*(\\w+)\\s+(\\w+)\\s+(\\w+)\\s*$").matcher(
                    "");
            this._oneDate =
                Pattern.compile("^\\s*(\\w+)\\s+(.+)\\s*$").matcher("");
            this._twoDate =
                Pattern.compile(
                    "^\\s*(\\w+)\\s+(.+)\\s+and\\s+(.+)\\s*$").matcher(
                    "");
        } catch (PatternSyntaxException re) {
            System.err.println(
                "Programmer Error: fix your regular expressions.");
            re.printStackTrace();
            System.exit(2);
        }

        /*
         * Setup an array of commands. The arguments are:
         * Object      - Class instance for finding menthods.
         * Commands    - Array of names for the command.
         * Method name - Name of method to map.
         * argLisg     - String representing argument syntax for
         *             - Interpreter-generated IllegalArgumentException.
         * Classes     - Array of classes representing arguments, to
         *             - complete signature.
         *             - If null, then no arguments.
         * RegExp      - A compiled regular expression.  Use regexp.getParam(i) to
         *             - dereference any args.
         */
        InterpreterDriver._commands =
            new Command[] {
                new Command(
                    this,
                    new String[] { "cmd0", "cmd0a1", "cmd0a2" },
                    "processCmd0",
                    "",
                    (Class[]) null,
                    (Matcher[]) null),
                new Command(
                    this,
                    new String[] { "cmd1", "cmd1a1", "cmd1a2" },
                    "processCmd1",
                    "<arg1>",
                    (Class[]) null,
                    new Matcher[] { this._oneArg }),
                new Command(
                    this,
                    new String[] { "cmd2", "cmd2a1", "cmd2a2" },
                    "processCmd2",
                    "<arg1> <arg2>",
                    (Class[]) null,
                    new Matcher[] { this._twoArg }),
                new Command(
                    this,
                    new String[] { "cmd3", "cmd3a1", "cmd3a2" },
                    "processCmd3",
                    "<date>",
                    (Class[]) null,
                    new Matcher[] { this._oneDate }),
                new Command(
                    this,
                    new String[] { "cmd4", "cmd4a1", "cmd4a2" },
                    "processCmd4",
                    "<date> and <date>",
                    (Class[]) null,
                    new Matcher[] { this._twoDate }),
                new Command(
                    this,
                    new String[] { "cmd5" },
                    "processCmd5",
                    "<arg1> <arg2> | <date> and <date>",
                    (Class[]) null,
                    new Matcher[] { this._twoArg, this._twoDate }),
                new Command(
                    this,
                    new String[] { "cmd6" },
                    "processCmd6",
                    "\"<string>\"",
                    (Class[]) null,
                    new Matcher[] { this._oneArgOneString }),
                new Command(
                    this,
                    new String[] { "cmd7" },
                    "processCmd7",
                    "<\"string\">",
                    new Class[] { Integer.class, Integer.class },
                    new Matcher[] { this._twoArg }),
                new Command(
                    this,
                    new String[] { "bye", "quit", "exit" },
                    "processExit",
                    "",
                    (Class[]) null,
                    (Matcher[]) null),
                };

        /* Load these commands into the interpreter. */
        InterpreterDriver._interpreter =
            new Interpreter(
                InterpreterDriver._CMDMATCHCOUNT,
                InterpreterDriver._commands);
    }

    /**
     * Method to begin the test.
     */
    public void run() {
        while (true) {
            try {
                this.readInput();
                InterpreterDriver._interpreter.exec(this, _ttyInput);
            } catch (IllegalArgumentException iarg) {
                System.err.println(iarg.getMessage());
            } catch (NoSuchMethodException nm) {
                System.err.println(nm);
                System.exit(1);
            } catch (IllegalAccessException ia) {
                System.err.println(ia);
                System.exit(1);
            } catch (InvocationTargetException it) {
                System.err.println(it);
                System.exit(1);
            }
        }
    }

    /**
     * Main test program
     * @param args optional command line arguments
     * @throws IOException when I/O failure
     * @throws NoSuchMethodException when reflection failure
     * @throws IllegalAccessException when access failure
     * @throws InvocationTargetException when invocation failure
     */
    public static void main(String[] args)
        throws
            IOException,
            NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException {
        InterpreterDriver driver = new InterpreterDriver();
        driver.run();
    }

    /**
     * The target commands to test out the Interpreter class.
     */
    public void processCmd0() {
        System.out.println("Executing cmd0");
    }

    /**
     * The target commands to test out the Interpreter class.
     */
    public void processCmd1() {
        System.out.println(
            "Executing "
                + this._oneArg.group(1)
                + ": arg = "
                + this._oneArg.group(2));
    }

    /**
     * The target commands to test out the Interpreter class.
     */
    public void processCmd2() {
        System.out.println(
            "Executing "
                + this._twoArg.group(1)
                + ": arg1, arg2 = "
                + this._twoArg.group(2)
                + ", "
                + this._twoArg.group(3));
    }

    /**
     * The target commands to test out the Interpreter class.
     */
    public void processCmd3() {
        System.out.println(
            "Executing "
                + this._oneDate.group(1)
                + ": date = "
                + this._oneDate.group(2));
    }

    /**
     * The target commands to test out the Interpreter class.
     */
    public void processCmd4() {
        System.out.println(
            "Executing "
                + this._twoDate.group(1)
                + " : date1, date2 = "
                + this._twoDate.group(2)
                + ", "
                + this._twoDate.group(3));
    }

    /**
     * Try a command that uses either two arguments, or two dates delimited
     * by the text "and".
     */
    public void processCmd5() {
        if (this._twoArg.group(1) != null) {
            System.out.println(
                "Executing "
                    + this._twoArg.group(1)
                    + " : arg1, arg2 = "
                    + this._twoArg.group(2)
                    + ", "
                    + this._twoArg.group(3));
        } else if (this._twoDate.group(1) != null) {
            System.out.println(
                "Executing "
                    + this._twoDate.group(1)
                    + " : date1, date2 = "
                    + this._twoDate.group(2)
                    + ", "
                    + this._twoDate.group(3));
        }
    }

    /**
     * Try a command that uses one argument as a quoted string.
     */
    public void processCmd6() {
        System.out.println(
            "Executing "
                + this._oneArgOneString.group(1)
                + " : arg, String = "
                + this._oneArgOneString.group(2)
                + ", "
                + this._oneArgOneString.group(3));
    }

    /**
     * The target commands to test out the Interpreter class.
     *
     * @param i integer input 1
     * @param j integer input 2
     */
    public void processCmd7(Integer i, Integer j) {
        System.out.println(
            "Executing "
                + this._twoDate.group(1)
                + " : getParen(2), getParen(3) = "
                + this._twoDate.group(2)
                + ", "
                + this._twoDate.group(3));
        System.out.println("Integer i = " + i);
        System.out.println("Integer j = " + j);
    }

    /**
     * The target commands to test out the Interpreter class.
     */
    public void processExit() {
        System.out.println("Exiting...");
        System.exit(0);
    }

    /**
     * Reads input, wither from command line or a batch file
     */
    private void readInput() {
        this.prompt();
        InterpreterDriver._ttyInput = this.readTTYLine();
    }

    /**
     * Prints a line to STDOUT
     * @param msg a string to be printed to STDOUT
     *
    private void writeTTYLine(String msg) {
        System.out.println(msg);
    }
    */

    /**
     * Command line prompt to STDOUT
     * Note it uses print rather than println because we don't want
     * an EOL. However then we must flush.
     */
    private void prompt() {
        System.out.print(this._prompt + ">> ");
        System.out.flush();
    }

    /**
     * Gets input from the command line
     * Note: must look for '\r' for it to work properly in DOS
     * @return tty input
     */
    private String readTTYLine() {
        StringBuffer buf = new StringBuffer(80);
        int c = 0;
        try {
            while ((c = System.in.read()) != -1) {
                char ch = (char) c;
                if (ch == '\n')
                    break; // Unix flavors.
                buf.append(ch);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return buf.toString();
    }
}
