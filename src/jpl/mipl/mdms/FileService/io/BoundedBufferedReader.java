package jpl.mipl.mdms.FileService.io;

/**
 * The BoundedBufferedReader class
 *
 * A BufferedReader that prevents DoS attacks by providing 
 * bounds for line length and number of lines
 *
 * Copyright (c) 2011 - Sean Malone
 *
 * The BoundedBufferedReader is published by Sean Malone under 
 * the BSD license. You should read and accept the
 * LICENSE before you use, modify, and/or redistribute 
 * this software.
 *
 * Version 1.2 adds the option for the max line count and character
 * per line count to be overridden. Also introduced is the option
 * to disable bound checking.  These overrides should be used
 * judiciously as they can re-open the potential for DOS attacks.
 *
 * @author Sean Malone <sean@seantmalone.com>
 * @author Nicholas Toole <nttoole@jpl.nasa.gov>
 * @version 1.2
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import jpl.mipl.mdms.FileService.komodo.api.Constants;

public class BoundedBufferedReader extends BufferedReader 
{
    //doubled the max values for both (01.11.17 nttoole)
	private static final int DEFAULT_MAX_LINES       = 65536;	//Max lines per file
	private static final int DEFAULT_MAX_LINE_LENGTH = 8192;	//Max bytes per line
	
	private static final boolean DEFAULT_BOUND_CHECK_ENABLED = true; //Check bounds 
	
	
	private int readerMaxLines;
	private int readerMaxLineLen;
	private int currentLine = 1;
	
	private boolean checkBounds = DEFAULT_BOUND_CHECK_ENABLED;
	
   //---------------------------------------------------------------------

	//---------------------------------------------------------------------
	
	/**
	 * Constructor with explicit max values for line count and character per
	 * line count.  Calling this constructor explicitly does not check the
	 * environment for overrides.
	 * @param reader Wrapped reader instance
	 * @param maxLines Maximum number of lines allowed
	 * @param maxLineLen Maximum number of characters per line allowed
	 * @throws IllegalArgumentException if max values are illegal
	 */
	public BoundedBufferedReader(final Reader reader, final int maxLines, 
	                             final int maxLineLen) 
	{
		super(reader);
		if ((maxLines<=0) || (maxLineLen<=0)) 
		    throw new IllegalArgumentException("BoundedBufferedReader - " +
		    		"maxLines and maxLineLen must be greater than 0");
		
		this.readerMaxLines = maxLines;
		this.readerMaxLineLen = maxLineLen;
	}
	
	//---------------------------------------------------------------------
	
	/**
	 * Constructor that uses default values.  This constructor will
	 * check the system properties for value overrides. 
	 * @param reader Wrapped reader instance
	 */
	public BoundedBufferedReader(Reader reader) 
	{
		this(reader, DEFAULT_MAX_LINES, DEFAULT_MAX_LINE_LENGTH);
	    checkOverrides();
	}
	
	   //---------------------------------------------------------------------
	
	protected void checkOverrides()
	{
	    String value = null;
	    
	    //max lines
	    value = System.getProperty(Constants.PROPERTY_IO_BOUNDEDREADER_MAX_LINE);
	    if (value != null)
	    {
	        try {
    	        int maxLine = Integer.parseInt(value);
    	        if (maxLine > 1)
    	            this.readerMaxLines = maxLine;
	        } catch (NumberFormatException nfEx) {   }
	    }
	    
	    //max chars per line
	    value = System.getProperty(Constants.PROPERTY_IO_BOUNDEDREADER_MAX_CHAR);
        if (value != null)
        {
            try {
                int maxChar = Integer.parseInt(value);
                if (maxChar > 1)
                    this.readerMaxLineLen = maxChar;
            } catch (NumberFormatException nfEx) {  }            
        }
        
        //disable all checking
        value = System.getProperty(Constants.PROPERTY_IO_BOUNDEDREADER_UNBOUNDED);
        if (value != null)
        {
            boolean removeCheck = Boolean.parseBoolean(value);
            if (removeCheck)
                this.checkBounds = !removeCheck;
        }
        
	}
	
	//---------------------------------------------------------------------
	
	public String readLine() throws IOException 
	{
		//Check readerMaxLines limit
		if (checkBounds && currentLine > readerMaxLines) 
		    throw new IOException("BoundedBufferedReader - Line read " +
		    		              "limit has been reached.");
		currentLine++;
		
		int currentPos=0;
		char[] data=new char[readerMaxLineLen];
		final int CR = 13;
		final int LF = 10;
		int currentCharVal=super.read();
		
		//Read characters and add them to the data buffer until we 
		//hit the end of a line or the end of the file.
		while( (currentCharVal!=CR) && (currentCharVal!=LF) && (currentCharVal>=0)) 
		{
			data[currentPos++]=(char) currentCharVal;
			//Check readerMaxLineLen limit
			if (!checkBounds || currentPos<readerMaxLineLen) 
				currentCharVal = super.read();
			else
				break;
		}
		
		if (currentCharVal<0)
		{
			//End of file
			if (currentPos>0) 
				//Return last line
				return(new String(data,0,currentPos));
			else
				return null;
		}
		else
		{	
			//Remove newline characters from the buffer
			if(currentCharVal==CR) 
			{
				//Check for LF and remove from buffer
				super.mark(1);
				if (super.read() != LF) 
					super.reset();
			} 
			else if(currentCharVal!=LF)
			{
				//readerMaxLineLen has been hit, but we still need to remove newline characters.
				super.mark(1);
				int nextCharVal = super.read();
				if (nextCharVal==CR) 
				{
					super.mark(1);
					if (super.read() != LF) 
						super.reset();	
				} 
				else if (nextCharVal!=LF) 
					super.reset();
			}
			return(new String(data,0,currentPos));
		}
		
	}
	
    //---------------------------------------------------------------------
}