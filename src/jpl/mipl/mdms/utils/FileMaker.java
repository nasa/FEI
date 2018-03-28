/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.utils;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;

/**
 * Makes a test file of specified size.  This is useful when created 3+ Gb files to 
 * test FEI.
 *
 * @author T. Huang, {Thomas.Huang@jpl.nasa.gov}
 * @version $Id: FileMaker.java,v 1.7 2004/12/23 23:24:57 ntt Exp $
 */
public class FileMaker {
    private ByteBuffer _buffer = ByteBuffer.allocate(1024 * 1024);
    private long _numWrite = 0;
    private String _filename;

    /**
     * Constructor
     * 
     * @param args the command line args
     */
    public FileMaker(String[] args) {
        if (!this._parseArgs(args))
            System.exit(0);
    }

    /**
     * Run method
     */
    public void run() {
        try {
            FileChannel filechannel =
                new FileOutputStream(this._filename).getChannel();

            this._buffer.clear();

            for (int i = 0; i < this._buffer.capacity(); ++i)
                this._buffer.put((byte) 0);

            for (int i = 0; i < this._numWrite; ++i) {
                this._buffer.flip();
                filechannel.write(this._buffer);
            }
            filechannel.close();
        } catch (Exception e) {
            MDMS.ERROR(e.getMessage());
        }

    }

    /**
     * Parse the command line args
     * 
     * @param args the command line args
     * @return boolean true if parsed successfully, false otherwise
     */
    private boolean _parseArgs(String[] args) {
        GetOpt getOpt = new GetOpt(args, "n:s:H");

        String str;
        try {
            while ((str = getOpt.nextArg()) != null) {
                // it's so happened that all the test arguments begin with different
                // letter, so we can just check by that.  In general, we can also
                // use String.equals() method to do the comparison.
                switch (str.charAt(0)) {
                    case 'H' :
                        MDMS.DEBUG(
                            "Usage: "
                                + this.getClass().getName()
                                + " -n <filename> -s <size in MB> -H");
                        return false;
                    case 'n' :
                        this._filename = getOpt.getArgValue();
                        break;
                    case 's' :
                        this._numWrite = Integer.parseInt(getOpt.getArgValue());
                        break;
                    default :
                        return false;
                }
            }
        } catch (ParseException pEx) {
            return false;
        }
        return true;
    }

    /**
     * Main method
     * 
     * @param args the command line args
     */
    public static void main(String[] args) {
        FileMaker fm = new FileMaker(args);
        fm.run();
    }
}
