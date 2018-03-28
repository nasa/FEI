package jpl.mipl.mdms.FileService.util;

/**
 * <B>Purpose:<B>
 * Utility class for conversion of wildcard expressions
 * to legal regular expressions.
 *
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: WildcardRegexUtil.java,v 1.1 2009/07/28 19:21:51 ntt Exp $
 */

public class WildcardRegexUtil
{

    public static final String DEFAULT_REGULAR_EXPRESSION = "^.*$";
    
    //---------------------------------------------------------------------
    
    /**
     * Returns true input parameter contains characters that
     * make up a wildcard expression.
     * @param wildcard Input expression
     * @return True if expression can be treated as a wildcard
     * expression, false otherwise.
     */
    
    public static boolean isWildCard(String wildcard) 
    {
        if (wildcard == null)
            return false;
        
        for (int i = 0, is = wildcard.length(); i < is; i++) 
        {
            char c = wildcard.charAt(i);
            switch(c) 
            {
                case '*':
                case '?':
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    return true;
            }
        }
        return false;
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Converts a wildcard expression to a regular expression. 
     * Null or empty inputs result in null being returned.
     * @param wildcard Wildcard expression
     * @return Regex build from wildcard
     */
    
    public static String wildCardToRegex(String wildcard) 
    {
        return wildCardToRegex(wildcard, false);        
    }
    
    //---------------------------------------------------------------------
    
    /**
     * Converts a wildcard expression to a regular expression. 
     * 
     * Handling of null or empty string wildcard can be controlled 
     * via the second parameter.  If useDefault is true, then such 
     * wildcard expressions will return the default regex (which
     * matches everything).  Otherwise, null would be returned.
     * 
     * @param wildcard Wildcard expression
     * @param useDefault Controls handling of null or empty expression
     * @return Regex built from wildcard
     */
    
    public static String wildCardToRegex(String wildcard, 
                                         boolean useDefault)
    {
        String regex = null;
        
        if (wildcard == null || wildcard.equals(""))
        {
            if (useDefault)
            {
                regex = DEFAULT_REGULAR_EXPRESSION;
            }            
        }
        else
        {
            StringBuffer s = new StringBuffer(wildcard.length());
            s.append('^');
            for (int i = 0, is = wildcard.length(); i < is; i++) 
            {
                char c = wildcard.charAt(i);
                switch(c) 
                {
                    case '*':
                        s.append(".*");
                        break;
                    case '?':
                        s.append(".");
                        break;
                        // escape special regexp-characters
                    case '(': case ')': case '[': case ']': case '$':
                    case '^': case '.': case '{': case '}': case '|':
                    case '\\':
                        s.append("\\");
                        s.append(c);
                        break;
                    default:
                        s.append(c);
                        break;
                }
            }
            s.append('$');
            regex = s.toString();
        }
        
        return regex;       
    }
    
    //---------------------------------------------------------------------
    
    public static void main(String[] args)
    {
        String[] tests = new String[] {
                "a*", "?a?", "a*.doc", "", "[ABC]a"
        };
        
        
        for (String test : tests)
        {
            System.out.println("Test: "+test);
            System.out.println(WildcardRegexUtil.wildCardToRegex(test));
            System.out.println(WildcardRegexUtil.wildCardToRegex(test, true));
        }
    }
    
    
    //---------------------------------------------------------------------
}
