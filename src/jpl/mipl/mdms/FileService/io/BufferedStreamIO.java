package jpl.mipl.mdms.FileService.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.net.SocketTimeoutException;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import jpl.mipl.mdms.FileService.util.FileUtil;
import jpl.mipl.mdms.utils.logging.Logger;

/**
 * <b>Purpose:</b> Hides the complexity of "attaching" an output stream to an
 * input stream for the purpose of file transfers and file transfers with
 * message digest. This class has been revised to allow partial file transfer,
 * which enables resume transfer in the case of network failure.
 * 
 * <PRE>
 * Copyright 2008, California Institute of Technology. 
 * ALL RIGHTS RESERVED. 
 * U.S. Government Sponsorship acknowledge (NASA contract NAS7-918). 2008.
 * </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 * 
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 01/01/1999        MDMS             Initial Release
 * 07/08/2005        Nick             Initial documentation.
 *                                    Swapped "rwd" for "rw" for RandomAccFile 
 *                                    for server file writing.
 * 07/15/2005        Nick             Set raf.setLength() to occur after file 
 *                                    fully transfers to truncate, not before.
 * 06/01/2008        Nick             CRC placed in a read loop. 
 * ============================================================================
 * </PRE>
 * 
 * @author Thomas Huang (Thomas.Huang@jpl.nasa.gov)
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: BufferedStreamIO.java,v 1.41 2010/09/08 19:23:05 ntt Exp $
 * 
 */

public class BufferedStreamIO 
{
   private static final int DEFINBUFSIZE = 512;

   private static final int DEFOUTBUFSIZE = 512;

   private static final String _LOCK = "";

   private static final String RAF_WRITE_MODE = "rw"; // "rwd";

   private static MessageDigest _digest = null;

   private BufferedInputStream _bis;

   private DigestInputStream _digestis; // Checksum file transfer filter

   private BufferedOutputStream _bos;

   private DigestOutputStream _digestos; // Checksum file transfer filter

   private int _inBufSize;

   private int _outBufSize;

   private Logger _logger = Logger.getLogger(BufferedStreamIO.class.getName());

   // ----------------------------------------------------------------------

   /**
    * Constructor using default buffer size for buffered I/O streams.
    * 
    * @param bis the BufferedInputStream object
    * @param bos the BufferedOutputStream object
    */

   public BufferedStreamIO(BufferedInputStream bis, BufferedOutputStream bos) 
   {
      this(bis, BufferedStreamIO.DEFINBUFSIZE, 
           bos, BufferedStreamIO.DEFOUTBUFSIZE);
   }

   // ----------------------------------------------------------------------

   /**
    * Constructor with specified buffer size for buffered I/O streams.
    * 
    * @param bis the BufferedInputStream object
    * @param bos the BufferedOutputStream object
    * @param bufSize input/output buffer size in unit of bytes
    */

   public BufferedStreamIO(BufferedInputStream bis, BufferedOutputStream bos,
                           int bufSize) 
   {
      this(bis, bufSize, bos, bufSize);
   }

   // ----------------------------------------------------------------------

   /**
    * Constructor with specified buffer sizes for each buffered stream object.
    * 
    * @param bis the BufferedInputStream object
    * @param inBufSize input buffer size in unit of bytes
    * @param bos the BufferedOutputStream object
    * @param outBufSize output buffer size in unit of bytes
    */

   public BufferedStreamIO(BufferedInputStream bis, int inBufSize,
                           BufferedOutputStream bos, int outBufSize) 
   {
      this._bis = bis;
      this._inBufSize = inBufSize;
      this._bos = bos;
      this._outBufSize = outBufSize;

      try {
         // If this is the first instance, create the message digest
         // for cloning.
         synchronized (BufferedStreamIO._LOCK) 
         {
            if (BufferedStreamIO._digest == null)
               BufferedStreamIO._digest = MessageDigest.getInstance("SHA");
         }
         this._digestis = new DigestInputStream(this._bis,
                                                BufferedStreamIO._digest);
         this._digestos = new DigestOutputStream(this._bos,
                                                 BufferedStreamIO._digest);
      } catch (java.security.NoSuchAlgorithmException nsa) {
         // SHA not implemented. Any checksum access attempt will fail.
         // This is not likely to happen anywhere.
         nsa.printStackTrace();
      }
   }

   // ----------------------------------------------------------------------

   /**
    * Reads from the (buffered) input stream and writes it to file
    * 
    * @param fileName absolute path of file to be written
    * @param fileSize file size in bytes
    * @throws IOException when file/stream I/O failed
    */

   public void readFileFromStream(String fileName, long fileSize)
                                               throws IOException 
   {
      this.readFileFromStream(fileName, 0, fileSize);
   }

   // ----------------------------------------------------------------------

   /**
    * Reads a block of binary data from stream and write to output file. If
    * <code>offset</code> is greater than zero, then the the binary data will
    * be append to the existing file.
    * 
    * @param fileName the output file name
    * @param offset the offset byte
    * @param length the lenght of byte stream
    * @throws IOException when network/file IO failure
    */

   public void readFileFromStream(String fileName, long offset, long length)
                                                          throws IOException 
   {
      byte[] data = new byte[this._inBufSize];
      int buffersize;
      long bytesToRead = length;
      long clock = 0;

      RandomAccessFile outFile = null;

      /**
       * For resume file write, we must first make sure the file is already
       * exists and we are starting at the correct position.
       */
      File file = new File(fileName);
      if (offset > 0) 
      {
         this._logger.debug("StreamIO: resume file write.");

         if (!file.exists() || (file.length() != offset))
            throw new IOException("Unable to resume file I/O due to " +
                       "either non-exist file or invalid offset value.");
      }

      try {
         this._logger.debug("StreamIO - Reading file, size "
                            + (bytesToRead + offset));
         this._logger.trace("StreamIO - Input buffer " + this._inBufSize);

         if (this._logger.isBenchEnabled()) 
         {
            clock = System.currentTimeMillis();
            this._logger.bench("Begin receiving and writting file '" + fileName
                  + "' size=" + length + " byte(s)");
         }

         // create new random access file
         outFile = new RandomAccessFile(file, RAF_WRITE_MODE);

         // setting the length truncates any data that existed before
         // outFile.setLength(length);
         // XXX THis was here before, not sure if it should be later
         outFile.seek(offset);
         while (bytesToRead > 0) 
         {
            /**
             * Casting of long to int here is OK. The if statement guarantees
             * that by the time we fall into the case where we're doing the
             * casting bytesToRead will be "castable"
             */
            buffersize = this._inBufSize;
            if (this._inBufSize >= bytesToRead)
               buffersize = (int) bytesToRead;

            int retVal;

            if ((retVal = this._bis.read(data, 0, buffersize)) < 1)
               throw new IOException("Unexpected EOF from network peer.");

            // trying to detect write failure such as insufficant disk
            // space,
            // etc.
            try {
               outFile.write(data, 0, retVal);
            } catch (IOException e) {
               this._logger.debug("File write failed.");
               bytesToRead -= retVal;
               throw e;
            }
            bytesToRead -= retVal;
         }
         outFile.setLength(length); // XXX This is the new position for call
         file.setLastModified(System.currentTimeMillis());
         
      } catch (SocketTimeoutException stoEx) {          
          this._logger.error("Received timeout error. Aborting read.");
          throw stoEx; // rethrow the exception.
       } catch (IOException e) {          
         this._logger.debug("Flush remaining data from peer");
         int retVal;
         while (bytesToRead > 0) {
            if (this._inBufSize < bytesToRead)
               retVal = this._bis.read(data, 0, this._inBufSize);
            else 
               retVal = this._bis.read(data, 0, (int) bytesToRead);
            
            // need to add this check here. If the IOException was thrown
            // due to lost of connection, then we need to stop pulling.
            if (retVal < 1)
               break;
            bytesToRead -= retVal;
         }
         _logger.debug("Done flushing");
         throw e; // rethrow the exception.
      } finally {
         if (outFile != null) {
            // Now sync the file. This call guarantees that dirty buffers
            // associated with the file are written to the physical medium.
            // Similar to the c function fsync.
            try {
              _logger.debug("Attempting to sync file");
               outFile.getFD().sync();
            } catch (SyncFailedException sfEx) {
               _logger.error("Error occurred while attempting to sync "
                     + fileName);
               _logger.debug(null, sfEx);
            }
            try {
               _logger.debug("Attempting to close file.");
               outFile.close();
            } catch (IOException ioEx) {
               _logger.error("Error occurred while attempting to close "
                     + fileName);
               _logger.debug(null, ioEx);
            }
            this._logger.bench("Time spent receiving and writting file '"
                  + fileName + "' size=" + length + " byte(s): "
                  + (System.currentTimeMillis() - clock) + "ms");
         }
      }
   }

   // ----------------------------------------------------------------------

   /**
    * Reads a block of binary data from stream and writes to output stream.
    * 
    * @param out the output file stream, left open on return.
    * @param offset the offset byte
    * @param length the lenght of byte stream
    * @throws IOException when network/file IO failure
    */

   public void readFileFromStream(OutputStream out, long offset, long length)
                                                          throws IOException 
   {
       readFileFromStream(out, offset, length, false);
   }
   
   // ----------------------------------------------------------------------
   
   /**
    * Reads a block of binary data from stream and writes to output stream.
    * 
    * @param out the output file stream, left open on return.
    * @param offset the offset byte
    * @param length the lenght of byte stream
    * @param checksum True if checksum follows file contents
    * @throws IOException when network/file IO failure
    */
   
   public void readFileFromStream(OutputStream out, long offset, 
                                  long length, boolean checksum)
                                  throws IOException 
   {
      byte[] data = new byte[this._inBufSize];
      int buffersize;
      long bytesToRead = length;
      int retVal;
      
      try {

         this._logger.debug("StreamIO - Reading file, size "
                           + (bytesToRead + offset));
         this._logger.debug("StreamIO - Input buffer " + this._inBufSize);
         
         while (bytesToRead > 0) 
         {
            /**
             * Casting of long to int here is OK. The if statement guarantees
             * that by the time we fall into the case where we're doing the
             * casting bytesToRead will be "castable"
             */
            buffersize = this._inBufSize;
            if (this._inBufSize >= bytesToRead)
               buffersize = (int) bytesToRead;

            if ((retVal = this._bis.read(data, 0, buffersize)) < 1)
               throw new IOException("Unexpected EOF from network peer.");

            // trying to detect buffer write failure such as insufficant
            // memory,
            // etc.
            try {
               out.write(data, 0, retVal);
            } catch (IOException e) {
               this._logger.debug("Buffer write failed.");
               bytesToRead -= retVal;
               throw e;
            } finally {
               out.flush();
            }
            bytesToRead -= retVal;
         }
         
         //eat the checksum, we don't use it
         if (checksum)
         {
             readChecksum();                        
             //this._bis.read(data, 0, BufferedStreamIO._digest.getDigestLength());
         }
         
      } catch (SocketTimeoutException stoEx) {
          this._logger.error("Received socket timeout error.  Aborting read.");          
          throw stoEx; // rethrow the exception.
      } catch (IOException e) {
         this._logger.debug("Flush remaining data from peer");
         while (bytesToRead > 0) {
            if (this._inBufSize < bytesToRead)
               retVal = this._bis.read(data, 0, this._inBufSize);
            else
               retVal = this._bis.read(data, 0, (int) bytesToRead);

            // need to add this check here. If the IOException was thrown
            // due to lost of connection, then we need to stop pulling.
            if (retVal < 1)
               break;
            bytesToRead -= retVal;
         }
         
         //eat the checksum, we don't use it
         if (checksum)
         {
             readChecksum();             
             //this._bis.read(data, 0, BufferedStreamIO._digest.getDigestLength());
         }
                      
         throw e; // rethrow the exception.
      } finally {
         if (out != null) {
            // Now sync the file. This call guarantees that dirty buffers
            // associated with the file are written to the physical medium.
            // Similar to the c function fsync.
            // outFile.getFD().sync();
            out.flush();
            //out.close();            
         }
      }
   }

   // ----------------------------------------------------------------------

   /**
    * Reads from the (buffered) input stream and writes it to the specified byte
    * buffer. The method limits the input file size to be less than or equal to
    * Integer.MAX_VALUE (~2GB), since buffering such large file in memory is not
    * recommended.
    * 
    * @param buffer data buffer to store data read from input stream.
    * @param fileSize file size in unit of bytes
    * @throws IOException when network I/O error occurs
    */

   public void readBufferFromStream(byte[] buffer, long fileSize) throws IOException
   {
       readBufferFromStream(buffer, fileSize, false);
   }
   
   // ----------------------------------------------------------------------

   /**
    * Reads from the (buffered) input stream and writes it to the specified byte
    * buffer. The method limits the input file size to be less than or equal to
    * Integer.MAX_VALUE (~2GB), since buffering such large file in memory is not
    * recommended.
    * 
    * @param buffer data buffer to store data read from input stream.
    * @param fileSize file size in unit of bytes
    * @param flushChecksum Flag indicating if checksum needs to be flushed
    * @throws IOException when network I/O error occurs
    */
   
   public void readBufferFromStream(byte[] buffer, long fileSize, boolean flushChecksum)
                                                                     throws IOException 
   {
      this._logger.debug("StreamIO - Reading file, size " + fileSize);
      this._logger.debug("StreamIO - Input buffer " + this._inBufSize);

      if (buffer.length < fileSize)
         throw new IOException(
               "Insufficient buffer size for the incoming file size.");

      int retVal;
      int bytesRead = 0;

      while (bytesRead < fileSize) 
      {
         retVal = this._bis.read(buffer, bytesRead, (int) fileSize - bytesRead);
         if (retVal < 1)
            throw new IOException("Unexpected EOF from network peer.");
         bytesRead += retVal;
      }
      
      //eat the checksum, we don't use it
      if (flushChecksum)
      {
          readChecksum();
          //int length = BufferedStreamIO._digest.getDigestLength();
          //byte[] receivedChecksum = new byte[length];      
          //this._bis.read(receivedChecksum, 0, length);
      }
      
   }

   // ----------------------------------------------------------------------

   /**
    * Reads the checksum from the input buffer and returns it as byte array.
    * The new byte array will be created with length equal to  the digest 
    * length.
    * @return Checksum byte array
    * @throws IOException if IO error occurs
    */
   
   protected byte[] readChecksum() throws IOException
   {
       int bytesToRead = BufferedStreamIO._digest.getDigestLength();       
       byte[] data = new byte[bytesToRead];
       byte[] crc  = new byte[bytesToRead];
       int bytesRead = 0;
       int crcIndex  = 0;
       
       //as we learned, even a 20 byte read may not collect 
       //everything in one call, so make successive calls
       //until all crc data is read in
       while (bytesToRead > 0)
       {
           bytesRead = this._bis.read(data, 0, bytesToRead);
           
           if (bytesToRead < 1)
               throw new IOException("Unexpected EOF from network peer.");
           
           //copy from data to crc array
           for (int i = 0; i < bytesRead; ++i)
               crc[crcIndex+i] = data[i];
           
           crcIndex    += bytesRead;              
           bytesToRead -= bytesRead;
       }       
       
       return crc;
   }

   // ----------------------------------------------------------------------

   
   /**
    * Method to flush an incoming file, and flush an incoming checksum, if
    * required.
    * 
    * @param fileSize Size of the file to be thrown away.
    * @param doChecksum Do we need to flush the checksum too?
    * @throws IOException when file I/O fail
    */

   public void skipFile(long fileSize, boolean doChecksum) throws IOException 
   {
      byte[] junk = new byte[this._inBufSize];
      long bytesToRead = fileSize;
      int retVal;

      // May need to flush a checksum too.
      if (doChecksum)
         bytesToRead += BufferedStreamIO._digest.getDigestLength();
      while (bytesToRead > 0) 
      {
         if (this._inBufSize < bytesToRead)
            retVal = this._bis.read(junk, 0, this._inBufSize);
         else
            retVal = this._bis.read(junk, 0, (int) bytesToRead);
         if (retVal < 1)
            throw new IOException("Unexpected EOF from network peer.");
         bytesToRead -= retVal;
      }
   }

   // ----------------------------------------------------------------------

   /**
    * Reads from the (buffered) input stream and writes it to file. A message
    * digest will be cloned and the reference <code>calculatedChecksum</code>
    * updated. See <code>MessageDigest</code> for methods to retrieve
    * "checksum": <code>toString()</code> and <code>digest()</code>.
    * 
    * @param fileName absolute path of file to be written
    * @param fileSize file size in unit of bytes
    * @return checksum value in byte array
    * @throws IOException when network/file I/O errors
    * @throws VerifyException when checksum unmatched
    * @see java.security.MessageDigest
    */

   public byte[] readAndVerifyFileFromStream(String fileName, long fileSize)
                                        throws IOException, VerifyException 
   {
      return this.readAndVerifyFileFromStream(fileName, 0, fileSize);
   }

   // ----------------------------------------------------------------------

   /**
    * Reads from the (buffered) input stream and writes it to file with the
    * specified offset. A message digest will be cloned and the reference
    * <code>calculatedChecksum</code> updated. If this is a partical file
    * transfer, then the message digest will be first calculated from byte 0 to
    * offset before resume transfer-time calculation of message digest. See
    * <code>MessageDigest</code> for methods to retrieve "checksum":
    * <code>toString()</code> and <code>digest()</code>.
    * 
    * @param fileName the name of the file
    * @param offset the offset value to begin the transfer
    * @param length the number of bytes to be transfered
    * @return the checksum byte array
    * @throws IOException when network/file IO failure
    * @throws VerifyException when checksum failed to verify.
    */
   
   public byte[] readAndVerifyFileFromStream(String fileName, long offset,
                          long length) throws IOException, VerifyException 
   {       
      byte[] data = new byte[this._inBufSize];
      byte[] receivedChecksum;
      int retVal;
      int buffersize;
      long bytesToRead = length - offset;
      RandomAccessFile outFile = null;
      MessageDigest calculatedDigest;
      byte[] calculatedChecksum = null;
      long clock = 0;

      if (offset > 0) 
      {
         this._logger.debug("StreamIO: resume file write.");
         File file = new File(fileName);

         if (!file.exists() || (file.length() != offset))
            throw new IOException("Unable to resume file I/O due to either "
                  + "non-existing file or invalid offset value.");
      }

      try {
         this._logger.debug("StreamIO - Reading file, size "
               + (bytesToRead + offset));
         this._logger.debug("StreamIO - Input buffer " + this._inBufSize);
         // Create a new message digest for this file, made available to the
         // caller via reference.
         calculatedDigest = (MessageDigest) BufferedStreamIO._digest.clone();
         if (offset > 0)
            FileUtil.updateChecksum(fileName, calculatedDigest, 0, offset);
         
         //receivedChecksum = new byte[calculatedDigest.getDigestLength()];
         this._digestis.setMessageDigest(calculatedDigest);

         if (this._logger.isBenchEnabled()) {
            clock = System.currentTimeMillis();
            this._logger.bench("Begin receiving and writting file '" + fileName
                  + "' size=" + length + " byte(s)");
         }

         //create the File instance
         File file = new File(fileName);
         
         // Digest for this file transfer.
         outFile = new RandomAccessFile(file, RAF_WRITE_MODE);

         // setting the length truncates any data that existed after length
         // outFile.setLength(length); (MOVED DOWN BELOW)

         if (offset > 0)
            outFile.seek(offset);
         while (bytesToRead > 0) {
            buffersize = this._inBufSize;

            if (this._inBufSize >= bytesToRead)
               buffersize = (int) bytesToRead;
           
            if ((retVal = this._digestis.read(data, 0, buffersize)) < 1)
               throw new IOException("Unexpected EOF from network peer.");

            // trying to detect write failure such as insufficant disk
            // space,
            // etc.
            try {         
               outFile.write(data, 0, retVal);
            } catch (IOException e) {
               this._logger.debug("File write failed.");
               bytesToRead = 0;
               throw e;
            }
            bytesToRead -= retVal;
         }
         // setting the length truncates any data that existed after length
         outFile.setLength(length);
         file.setLastModified(System.currentTimeMillis());

         // The file has now been transfered. Now read the client-calculated
         // checksum, and call verify.         
         receivedChecksum = readChecksum();         
         
         // Call to digest() method resets digest.
         calculatedChecksum = calculatedDigest.digest();
         
         // Uncomment the next line to test verify failure handling.
         // receivedChecksum[2] = 0;
         if (MessageDigest.isEqual(receivedChecksum, calculatedChecksum) == false)
            throw new VerifyException("Message digest comparison failed.");

         // checksum does not verify that data does not exist on the local
         // file beyond where we write, so check case that local file is longer
         // than server file, checksums would not match in the case
         if (outFile.getFilePointer() < outFile.length())
            throw new VerifyException("Local file is larger than " +
                                      "server file.");

      } catch (SocketTimeoutException stoEx) {
         // Dont treat this the same as general IOEx, as we cannot
          //read anymore
         this._logger.error("Received socket timeout error.  Aborting read.");
         throw stoEx;
      } catch (IOException e) {
          // Flush using buffered stream. We are no longer interested in the
          // digest.
          this._logger.debug("Flush remaining data from peer");
          while (bytesToRead > 0) {
             buffersize = this._inBufSize;
             if (this._inBufSize >= bytesToRead)
                buffersize = (int) bytesToRead;

             if ((retVal = this._digestis.read(data, 0, buffersize)) < 1)
                throw new IOException("Unexpected EOF from network peer.");
             bytesToRead -= retVal;
          }
          /* Need to flush checksum too. */
          readChecksum();
          //this._bis.read(data, 0, BufferedStreamIO._digest.getDigestLength());
          throw e;
       } catch (CloneNotSupportedException cnse) {
         // Flush using buffered input stream.
         this._logger.debug("Flush remaining data from peer");
         while (bytesToRead > 0) {
            buffersize = this._inBufSize;

            if (this._inBufSize >= bytesToRead)
               buffersize = (int) bytesToRead;

            if ((retVal = this._bis.read(data, 0, buffersize)) < 1)
               throw new IOException("Unexpected EOF network peer.");
            bytesToRead -= retVal;
         }
         /* Need to flush checksum too. */
         readChecksum();
         //this._bis.read(data, 0, BufferedStreamIO._digest.getDigestLength());
         throw new IOException(cnse.getMessage());
      } finally {
         if (outFile != null) {
            // Now sync the file. This call guarantees that dirty buffers
            // associated with the file are written to the physical medium.
            // Similar to the c function fsync.
            outFile.getFD().sync();
            outFile.close();
            this._logger.bench("Time spent receiving and writing file '"
                  + fileName + "' size=" + length + " byte(s): "
                  + (System.currentTimeMillis() - clock) + "ms");
         }
      }
      return calculatedChecksum;
   }

   // ----------------------------------------------------------------------

   /**
    * Reads from the (buffered) input stream an writes it to file. A message
    * digest will be cloned and the reference <code>calculatedChecksum</code>
    * updated. See <code>MessageDigest</code> for methods to retrieve
    * "checksum": <code>toString()</code> and <code>digest()</code>.
    * 
    * @param buffer message digest byte array.
    * @param fileSize file size
    * @return the received, verified checksum
    * @throws IOException when file I/O fail
    * @throws VerifyException when comparisons file fail
    * @see java.security.MessageDigest
    */

   public byte[] readAndVerifyBufferFromStream(byte[] buffer, long fileSize)
                                         throws IOException, VerifyException 
   {
      if (buffer.length < fileSize)
         throw new IOException("Insufficient buffer size for the incoming file size.");

      byte[] receivedChecksum;

      int retVal;
      int bytesRead = 0;

      MessageDigest calculatedDigest;
      byte[] calculatedChecksum = null;

      try {
         this._logger.debug("StreamIO - Reading file, size " + fileSize);
         this._logger.debug("StreamIO - Input buffer " + this._inBufSize);
         
         // Create a new message digest for this file, made available to the
         // caller via reference.
         calculatedDigest = (MessageDigest) BufferedStreamIO._digest.clone();
         receivedChecksum = new byte[calculatedDigest.getDigestLength()];
         this._digestis.setMessageDigest(calculatedDigest);
         
         // Digest for this file transfer.
         while (bytesRead < fileSize) 
         {
            retVal = this._digestis.read(buffer, bytesRead, 
                                         (int) fileSize - bytesRead);
            if (retVal < 1)
               throw new IOException("Unexpected EOF network peer.");
            bytesRead += retVal;
         }
         
         // The file has now been transfered. Now read the
         // client-calculated checksum, and call verify.
         this._logger.debug("Now receiving checksum.");
         receivedChecksum = readChecksum();
         //this._bis.read(receivedChecksum, 0, calculatedDigest.getDigestLength());
         
         // Call to digest() method resets digest.
         calculatedChecksum = calculatedDigest.digest();
         
         // Uncomment the next line to test verify failure handling.
         // receivedChecksum[2] = 0;
         if (MessageDigest.isEqual(receivedChecksum, calculatedChecksum) == false)
            throw new VerifyException("Message digest comparison failed.");

      } catch (SocketTimeoutException stoEx) {
          // print message and rethrow
          this._logger.error("Received timeout error.  Aborting read.");
          throw stoEx;
       } catch (CloneNotSupportedException cnse) {
         // Flush using buffered input stream.
         this._logger.debug("Flush remaining data from peer");
         while (bytesRead < fileSize) {
            retVal = this._digestis.read(buffer, bytesRead, 
                               (int) fileSize - bytesRead);
            if (retVal < 1)
               throw new IOException("Unexpected EOF network peer.");
            bytesRead += retVal;
         }
         //flush checksum
         readChecksum();         
         throw new IOException(cnse.getMessage());
      }
      return calculatedChecksum;
   }

   // ----------------------------------------------------------------------

   /**
    * Reads a file and writes it to the (buffered) output stream.
    * 
    * @param fileName absolute path of file to be read
    * @param fileSize file size in unit of bytes
    * @throws IOException when network/file I/O error occurs
    */

   public void writeFileToStream(String fileName, long fileSize)
                                              throws IOException 
   {
      this.writeFileToStream(fileName, 0, fileSize);
   }

   // ----------------------------------------------------------------------

   /**
    * Method to read a data block from an input file and writes it to the
    * (buffered) output stream.
    * 
    * @param fileName the file name.
    * @param offset the offset set to begin the transfer
    * @param length the number of bytes to be transfered
    * @throws IOException when network/file IO failure.
    */

   public void writeFileToStream(String fileName, long offset, long length)
                                                         throws IOException 
   {

      RandomAccessFile inFile = null;
      long clock = 0;
      try {
         this._logger.debug("StreamIO - Writing file ");
         byte[] data = new byte[this._outBufSize];

         if (this._logger.isBenchEnabled()) 
         {
            clock = System.currentTimeMillis();
            this._logger.bench("Begin reading and sending file '" + fileName
                  + "' size=" + length + " byte(s)");
         }

         inFile = new RandomAccessFile(fileName, "r");
         inFile.seek(offset);

         long bytesToRead = length;
         while (bytesToRead > 0) 
         {
            int buffersize = this._inBufSize;

            if (this._inBufSize >= bytesToRead)
               buffersize = (int) bytesToRead;

            int retVal;
            if ((retVal = inFile.read(data, 0, buffersize)) == -1)
               throw new IOException("Unexpected EOF while reading file.");
            this._bos.write(data, 0, retVal);
            bytesToRead = bytesToRead - retVal;
            this._bos.flush();
         }
      } finally {
         if (inFile != null) 
         {
            inFile.close();
            this._logger.bench("Time spent reading and sending file '"
                  + fileName + "' size=" + length + " byte(s): "
                  + (System.currentTimeMillis() - clock) + "ms");
         }
      }
   }

   // ----------------------------------------------------------------------

   /**
    * Generate a checksum on a buffer. A message digest will be cloned and the
    * reference <code>calculatedChecksum</code> updated. See
    * <code>MessageDigest</code> for methods to retrieve "checksum":
    * <code>toString()</code> and <code>digest()</code>.
    * 
    * @param buffer input message digest byte array.
    * @return the checksum byte array.
    * @throws IOException since the buffer was retrieved from network/file.
    * @see java.security.MessageDigest
    */

   public byte[] genChecksum(byte[] buffer) throws IOException 
   {
      MessageDigest calculatedDigest;
      byte[] checksum;
      try {
         // Create a new message digest for this buffer.
         calculatedDigest = (MessageDigest) BufferedStreamIO._digest.clone();
         checksum = calculatedDigest.digest(buffer);
      } catch (CloneNotSupportedException cnse) {
         throw new IOException(cnse.getMessage());
      }
      return checksum;
   }

   // ----------------------------------------------------------------------

   /**
    * Writes the input data buffer to buffered output stream. Again, the method
    * limits the data buffer to be less than Integer.MAX_VALUE (~2GB), since
    * buffering such large file in memory is not recommended.
    * 
    * @param buffer in-memory buffer contains the file.
    * @param fileSize file size in unit of bytes.
    * @throws IOException when general I/O failure
    */

   public void writeFileToStream(byte[] buffer, long fileSize)
                                            throws IOException 
   {
      this._logger.debug("StreamIO - Writing file from memory");

      this._bos.write(buffer, 0, (int) fileSize);
      this._bos.flush();
   }

   // ----------------------------------------------------------------------

   /**
    * Writes the input data buffer onto buffered output stream followed by the
    * supplied checksum value.
    * 
    * @param fileName absolute path of file to be written
    * @param fileSize file size in unit of bytes
    * @param suppliedChecksum the input checksum value to be sent
    * @throws IOException when network/file I/O failure
    * @see java.security.MessageDigest
    */

   public void writeAndVerifyFileToStream(String fileName, long fileSize,
                                          byte[] suppliedChecksum) 
                                          throws IOException 
   {
      this.writeAndVerifyFileToStream(fileName, 0, fileSize, suppliedChecksum);
   }

   // ----------------------------------------------------------------------

   /**
    * Writes the data block within the input data file onto buffered output
    * stream followed by the supplied checksum value.
    * 
    * @param fileName the file name
    * @param offset the offset to begin the transfer
    * @param length number of bytes to be transfered
    * @param suppliedChecksum the supplied checksum array.
    * @throws IOException when network/file IO failure
    */

   public void writeAndVerifyFileToStream(String fileName, long offset,
                                          long length, byte[] suppliedChecksum) 
                                          throws IOException 
   {

      this._logger.debug("Sending file to client");
      byte[] data = new byte[this._outBufSize];
      RandomAccessFile inFile = null;
      long clock = 0;

      try {

         if (this._logger.isBenchEnabled()) 
         {
            clock = System.currentTimeMillis();
            this._logger.bench("Begin reading and sending file '" + fileName
                  + "' size=" + length + " byte(s)");
         }

         inFile = new RandomAccessFile(fileName, "r");
         inFile.seek(offset);

         long bytesToRead = length;
         while (bytesToRead > 0) 
         {
            int buffersize = this._inBufSize;

            if (this._inBufSize >= bytesToRead)
               buffersize = (int) bytesToRead;

            int retVal;
            if ((retVal = inFile.read(data, 0, buffersize)) == -1)
               throw new IOException("Unexpected EOF while reading file.");
            this._bos.write(data, 0, retVal);
            bytesToRead = bytesToRead - retVal;
         }
         
         // Ship the checksum supplied.
         this._logger.debug("Now send checksum to client - length "
                           + suppliedChecksum.length);
         this._bos.write(suppliedChecksum, 0, suppliedChecksum.length);
         this._bos.flush();
      } finally {
         if (inFile != null) 
         {
            inFile.close();
            this._logger.bench("Time spent reading and sending file '"
                  + fileName + "' size=" + length + " byte(s): "
                  + (System.currentTimeMillis() - clock) + "ms");
         }
      }
   }

   // ----------------------------------------------------------------------

   /**
    * Writes the input file onto buffered output stream. A message digest will
    * be cloned and the reference <code>calculatedChecksum</code> updated. See
    * <code>MessageDigest</code> for methods to retrieve digest:
    * <code>toString()</code> and <code>digest()</code>.
    * 
    * @param fileName absolute path of file to be written
    * @param fileSize file size in unit of bytes
    * @return calculatedChecksum message digest byte array.
    * @throws IOException when network/file I/O failure
    * @see java.security.MessageDigest
    */

   public byte[] writeAndVerifyFileToStream(String fileName, long fileSize)
                                           throws IOException 
   {
      return this.writeAndVerifyFileToStream(fileName, 0, fileSize);
   }

   // ----------------------------------------------------------------------

   /**
    * Writes the specified byte range within the input data file to buffered
    * output stream and caclculates the checksum as the data is being
    * transfered. This is a partical block, the the bytes 0-offset checksum
    * value will be first calculated before continue on the reset of the
    * transfer.
    * 
    * @param fileName the file name
    * @param offset the offset to begin the transfer
    * @param length number of bytes to be transfered
    * @return the checksum byte array
    * @throws IOException when network/file IO failure
    */

   public byte[] writeAndVerifyFileToStream(String fileName, long offset,
                                          long length) throws IOException 
   {

      this._logger.debug("StreamIO - Writing file ");
      byte[] data = new byte[this._outBufSize];
      byte[] calculatedChecksum = null;
      RandomAccessFile inFile = null;
      long clock = 0;

      try {
         if (this._logger.isBenchEnabled()) 
         {
            clock = System.currentTimeMillis();
            this._logger.bench("Begin reading and sending file '" + fileName
                  + "' size=" + length + " byte(s)");
         }

         inFile = new RandomAccessFile(fileName, "r");

         if (offset > 0)
            inFile.seek(offset);

         long bytesToRead = length;
         MessageDigest calculatedDigest;

         try {
            // Create a new message digest for this file, made available to
            // the
            // caller via reference.
            calculatedDigest = (MessageDigest) BufferedStreamIO._digest.clone();
            if (offset > 0)
               FileUtil.updateChecksum(fileName, calculatedDigest, 0, offset);
            this._digestos.setMessageDigest(calculatedDigest);
            // Digest for this file transfer.
         } catch (CloneNotSupportedException cnse) {
            throw new IOException(cnse.getMessage());
         }

         while (bytesToRead > 0) {

            int buffersize = this._inBufSize;

            if (this._inBufSize >= bytesToRead)
               buffersize = (int) bytesToRead;

            int retVal;
            if ((retVal = inFile.read(data, 0, buffersize)) == -1)
               throw new IOException("Unexpected EOF while reading file.");
            this._digestos.write(data, 0, retVal);
            bytesToRead = bytesToRead - retVal;
         }
         this._digestos.flush();
         // Get the digest for this message.
         calculatedChecksum = calculatedDigest.digest();
         // Ship the checksum just calculated.
         this._bos.write(calculatedChecksum, 0, 
                         calculatedDigest.getDigestLength());
         this._bos.flush();
      } finally {
         if (inFile != null) {
            inFile.close();
            this._logger.bench("Time spent reading and sending file '"
                  + fileName + "' size=" + length + " byte(s): "
                  + (System.currentTimeMillis() - clock) + "ms");
         }
      }
      return calculatedChecksum;
   }

   // ----------------------------------------------------------------------

   /**
    * Writes a file from memory to to the (buffered) output stream A message
    * digest will be cloned and the reference <code>calculatedChecksum</code>
    * updated. See <code>MessageDigest</code> for methods to retrieve digest:
    * <code>toString()</code> and <code>digest()</code>.
    * 
    * @param buffer file in-memory buffer
    * @param fileSize file size in bytes
    * @return calculatedChecksum message digest byte array.
    * @throws IOException when network/file I/O failures
    * @see java.security.MessageDigest
    */

   public byte[] writeAndVerifyBufferToStream(byte[] buffer, long fileSize)
                                                         throws IOException 
   {
      this._logger.debug("StreamIO - Writing file ");
      MessageDigest calculatedDigest;
      byte[] calculatedChecksum = null;

      try {
         // Create a new message digest for this file, made available to the
         // caller via reference.
         calculatedDigest = (MessageDigest) BufferedStreamIO._digest.clone();
         this._digestos.setMessageDigest(calculatedDigest);
         // Digest for this file transfer.
      } catch (CloneNotSupportedException cnse) {
         throw new IOException(cnse.getMessage());
      }
      this._digestos.write(buffer, 0, (int) fileSize);
      this._digestos.flush();
      // Get the digest for this message.
      calculatedChecksum = calculatedDigest.digest();
      // Ship the checksum just calculated.
      this._bos.write(calculatedChecksum, 0, 
                      calculatedDigest.getDigestLength());
      this._bos.flush();
      return calculatedChecksum;
   }

   // ----------------------------------------------------------------------

   /**
    * Construct a message from a reply packet. See <code>writeMessage()</code>
    * for reply packed format.
    * 
    * @return a MessagePkg object that contains the message just read.
    * @throws IOException when general I/O failure
    */

   public MessagePkg readMessage() throws IOException 
   {
      // First, get the error number string.
      StringBuffer buf = new StringBuffer();
      int c = 0;
      while ((c = this._bis.read()) != -1) {
         char ch = (char) c;
         if (ch == ':')
            break;
         buf.append(ch);
      }
      if (c == -1)
         throw new IOException("Unexpected EOF from network peer.");

      int errno = 0;
      try {
          errno = Integer.parseInt(buf.toString());
      } catch (NumberFormatException nfEx) {
          throw new IOException("Expected numeric value.  Error: " +
                                nfEx.getMessage());
      }
      String msgtext = this.readLine();

      MessagePkg message = new MessagePkg(errno, msgtext);
      return message;
   }

   // ----------------------------------------------------------------------

   /**
    * Get a line from input stream
    * 
    * @return an <code>'\n'</code> or <code>'\r'</code> delimited line
    * @throws IOException when read failed
    */

   public String readLine() throws IOException 
   {
      StringBuffer buf = new StringBuffer();
      int c = 0;
      while ((c = this._bis.read()) != -1) 
      {
         char ch = (char) c;
         buf.append(ch);
         if (ch == '\n' || ch == '\r')
            break;
      }
      if (c == -1)
         throw new IOException("Unexpected EOF from network peer.");
      return buf.toString();
   }

   // ----------------------------------------------------------------------

   /**
    * Write a message to the command stream. A message consists of integer and a
    * message. The format of the reply is " <number>: <message>".
    * 
    * @param errorNum the input error value.
    * @param s the line to be written
    * @throws IOException when write failed
    */

   public void writeMessage(int errorNum, String s) throws IOException 
   {
      String errnumStr = errorNum + ":";
      byte[] b = errnumStr.getBytes();
      this._bos.write(b, 0, b.length);

      b = s.getBytes();
      this._bos.write(b, 0, b.length);
      if (s.length() == 0 || 
                      (s.length() > 0 && s.charAt(s.length() - 1) != '\n'))
         this._bos.write(10);
      this._bos.flush();
   }

   // ----------------------------------------------------------------------

   /**
    * Write line to output stream
    * 
    * @param s the line to be written
    * @throws IOException when write fails
    */

   public void writeLine(String s) throws IOException 
   {
      final byte[] b = s.getBytes();
      this._bos.write(b, 0, b.length);
      this._bos.write(10);
      this._bos.flush();
   }

   // ----------------------------------------------------------------------
   
   /**
    * Write string to output stream
    * 
    * @param s the string to be written
    * @throws IOException when write fails
    */
   public void write(String s) throws IOException 
   {
      final byte[] b = s.getBytes();
      this._bos.write(b, 0, b.length);
      this._bos.flush();
   }
   
   // ----------------------------------------------------------------------
}