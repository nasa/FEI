/*******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved US
 * Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ******************************************************************************/
package jpl.mipl.mdms.FileService.util;

import java.util.regex.Pattern;

/**
 * A utility class that supplies method/s that evaluate 
 * simple expressions containing only the '*' character
 * 
 * @author A. Tinio, {Adrian.Tinio@jpl.nasa.gov}
 * @version $Id: ExpressionEvaluator.java,v 1.3 2008/08/22 05:36:55 awt Exp $
 *
 */
public class ExpressionEvaluator {
    
    /**
     * Evaluates whether the input is a match for the given '*' expression
     * 
     * @param expression the expression containing any number of '*' chars.
     * @param input the input to evaluate the expression against.
     * @return true if the input matches the expression, false otherwise
     */
    public static boolean isMatch(String expression, String input) {
        //  Replace the '*' character with a regular expression
        //  token that indicates "zero or more" characters.
        expression = expression.replaceAll("\\*",".*");
        return Pattern.matches(expression,input); 
    }
    
    /**
     * Strips the string of enclosing single or double quotes.
     * 
     * @param input
     * @return
     */
    public static String stripEnclosingQuotes(String input) {
       if (input.startsWith("\"") || input.startsWith("'"))
           input = input.substring(1);
       if (input.endsWith("\"") || input.endsWith("'"))
           input = input.substring(0, input.length() - 1);
       
       return input;
    }
    

}
