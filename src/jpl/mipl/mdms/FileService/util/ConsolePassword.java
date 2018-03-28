package jpl.mipl.mdms.FileService.util;

import java.io.Console;
import java.io.IOException;

/**
 * This is a utility class that has only one method to allow user to type 
 * in their password without being echoed back on the console.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: ConsolePassword.java,v 1.3 2013/03/30 00:06:21 ntt Exp $
 */
public class ConsolePassword {

    /**
     * Method to read in a user password.
     * 
     * @param prompt The command prompt string (e.g. 'Password:')
     * @return The user input password string
     * @throws IOException When failed to read user input.
     */
    public static String getPassword(String prompt) throws IOException 
    {
        
        String password = "";
        Console console = System.console();
        
        if (console == null)
        {
            password = getPasswordInternal(prompt);
        }

        char[] pass = console.readPassword("%s", prompt);
        if (pass != null)
        {
            password = new String(pass);
        }
        
      
        return password;
    }
    
    
    private static String getPasswordInternal(String prompt) throws IOException {
        String password = "";

        // creates the eraser thread
        Eraser eraser = new Eraser(prompt);
        Thread eraserThread = new Thread(eraser);
        eraserThread.start();

        while (true) {
            // this is a blocking call until the user press' enter.
            char c = (char) System.in.read();

            // Now the user is done inputing, we should stop the eraser
            eraser.quit();

            // a special case for platforms that reads \r\n for end of line.
            if (c == '\r') {
                c = (char) System.in.read();
                if (c == '\n')
                    break;
                else
                    continue;
            } else if (c == '\n')
                break;
            else
                password += c;
        }
        return password;
    }

    /**
     * Inner eraser class that continues to refresh the input line with
     * user prompt and blanks.
     * 
     * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
     * @version $Id: ConsolePassword.java,v 1.3 2013/03/30 00:06:21 ntt Exp $
     */
    private static class Eraser implements Runnable {

        private boolean _stop = false;
        private String _userPrompt;

        /**
         * Constructor to set the user prompt to blank.
         *
         */
        public Eraser() {
            this._userPrompt = "";
        }

        /**
         * Constructor to set the user prompt.
         * 
         * @param prompt The user prompt.
         */
        public Eraser(String prompt) {
            this._userPrompt = prompt;
        }

        /**
         * Method to keeps on erasing the input line until it is signaled to stop.
         */
        public void run() {
            while (!this._stop) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    //no-op;
                }

                if (!this._stop) {
                    System.out.print(
                        "\r" + this._userPrompt + " \r" + this._userPrompt);
                    System.out.flush();
                }
            }
        }

        /**
         * Method to set the stop flag.
         *
         */
        public void quit() {
            this._stop = true;
        }
    }
}
