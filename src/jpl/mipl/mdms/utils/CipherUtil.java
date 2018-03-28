package jpl.mipl.mdms.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import jpl.mipl.mdms.utils.logging.Logger;

/**
 * Utility to encrypt/decrypt a message using RSA public/private key pairs.
 * To generate the private key:
 * % openssl genrsa -out private.pem 2048
 * % openssl pkcs8 -topk8 -nocrypt -in private.pem -inform PEM -out private.der -outform DER
 * 
 * To create a public key
 * % openssl rsa -inform PEM -in private.pem -pubout -outform DER -out public.der
 * 
 * Use the DER keys with this utility.
 * 
 * 
 * @author awtinio
 * @version $Id: CipherUtil.java,v 1.6 2012/03/16 18:13:37 ntt Exp $
 */
public class CipherUtil {
    
    //private File _privateKeyFile;
    //private File _publicKeyFile;
    
    private InputStream _privateInputStream;
    private InputStream _publicInputStream;
    
    private PublicKey _publicKey;
    private PrivateKey _privateKey;

    private Cipher _cipher;
    private Cipher _dcipher;
    
    private final String _class = CipherUtil.class.getName();
    private final Logger _logger = Logger.getLogger(this._class);
    
    
    // Mapping byte to hex
    private static char[] HEX_CHAR = {
      '0' , '1' , '2' , '3' , 
      '4' , '5' , '6' , '7' , 
      '8' , '9' , 'a' , 'b' , 
      'c' , 'd' , 'e' , 'f' }; 

    // precomputed translation table for chars 0..f
    private static byte[] TRANSLATE_TABLE = new byte['f' + 1];   
    static {
        // only 0..9 A..F a..f have meaning. rest are errors.
        for ( int i = 0; i <= 'f'; i++ )
            TRANSLATE_TABLE[ i ] = -1;
        
        for ( int i = '0'; i <= '9'; i++ )
            TRANSLATE_TABLE[ i ] = ( byte ) ( i - '0' );
    
        for ( int i = 'A'; i <= 'F'; i++ )
            TRANSLATE_TABLE[ i ] = ( byte ) ( i - 'A' + 10 );
            
        for ( int i = 'a'; i <= 'f'; i++ )
            TRANSLATE_TABLE[ i ] = ( byte ) ( i - 'a' + 10 );

    }
    
    /**
     * Constructor. 
     * Either privKeyFile or pubKeyFile can be null but not both.
     * 
     * @param privKeyFile absolute file path of a private key in DER format.
     *        If null, this util will only be used for encryption
     * @param pubKeyFile absolute file path of corresponding public key in DER format
     *        If null, this util will only be used for decryption
     * @throws IOException
     */
    public CipherUtil(String privKeyFile, String pubKeyFile) throws IOException {
        
        if (privKeyFile == null && pubKeyFile == null)
            throw new IllegalArgumentException("Both keyfiles can not be null.");
        
        if (privKeyFile != null) {
            File f = new File(privKeyFile);
            this._privateInputStream = new FileInputStream(f);
            readPrivateKey();
            this._privateInputStream.close();
        }
        if (pubKeyFile != null) {
            File f = new File(pubKeyFile);
            this._publicInputStream = new FileInputStream(f);
            readPublicKey();
            this._publicInputStream.close();
        }
        
//        if (privKeyFile != null) {
//            this._privateKeyFile = new File(privKeyFile);            
//            readPrivateKey();
//        }
//        if (pubKeyFile != null) {
//            this._publicKeyFile = new File(pubKeyFile);
//            readPublicKey();
//        }
    }
    
    public CipherUtil(URL privKey, URL pubKey) throws IOException, URISyntaxException {
        
        if (privKey == null && pubKey == null)
            throw new IllegalArgumentException("Both keyfiles can not be null.");
        
//        if (privKey != null) {        
//            this._privateKeyFile = new File(privKey.toURI());
//            readPrivateKey();
//        }
//        if (pubKey != null) {
//            this._publicKeyFile = new File(pubKey.toURI());
//            readPublicKey();
//        }
        
        if (privKey != null) {
            this._privateInputStream = privKey.openStream();
            readPrivateKey();
            this._privateInputStream.close();
            
        }
        if (pubKey != null) {
            this._publicInputStream = pubKey.openStream();
            readPublicKey();
            this._publicInputStream.close();
        }
    }   
    
    /**
     * Reads content from inputstream and writes to a expandable
     * byte array object.  Returns primitive byte[].
     * @param is InputStream with content.  
     * @return byte[] of input stream contents
     * @throws IOException If IO error occurs.
     */
    
    public static byte[] readBytesFromStream(InputStream is) throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream(512);
        byte[] buf = new byte[512];
        while (true) {
            int len = is.read(buf);
            if (len == -1)
                break;
            os.write(buf, 0, len);
        }        
        os.close();        
        return os.toByteArray();
    }
    
    
    /**
     * Read the private key file and initialize cipher for 
     * decryption.
     * 
     * @throws IOException
     */
    private void readPrivateKey() throws IOException {
        
        //byte[] privateKeyByte = new byte[(int)this._privateKeyFile.length()];
        //new FileInputStream(this._privateKeyFile).read(privateKeyByte);
       
        
        byte[] privateKeyByte = readBytesFromStream(this._privateInputStream);
        
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyByte);
        
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this._privateKey = kf.generatePrivate(privateKeySpec);
            
            this._dcipher = Cipher.getInstance("RSA");
            this._dcipher.init(Cipher.DECRYPT_MODE, this._privateKey);
            
        } catch (Exception exc) {
            this._logger.error(exc.getMessage());
        }
        
    }
    
    
    /**
     * Read the public key file and initialize cipher for 
     * encryption.
     * 
     * @throws IOException
     */
    private void readPublicKey() throws IOException {   
        
        //byte[] publicKeyByte = new byte[(int)this._publicKeyFile.length()];        
        //new FileInputStream(this._publicKeyFile).read(publicKeyByte);
       
        byte[] publicKeyByte = readBytesFromStream(this._publicInputStream);
        
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByte);
        
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this._publicKey = kf.generatePublic(publicKeySpec);            
            this._cipher = Cipher.getInstance("RSA");
            
            this._cipher.init(Cipher.ENCRYPT_MODE,this._publicKey);
            
        } catch (Exception exc) {
            this._logger.error(exc.getMessage());
        }
        
    }
    /**
     * Encrypts the message into a byte array.
     * 
     * @param message
     * @return
     * @throws Exception
     */
    public byte[] encrypt(String message) throws Exception {
        if (this._cipher == null)
            throw new IllegalStateException("CipherUtil has not been initialized for encryption.");
        return this._cipher.doFinal(message.getBytes("UTF-8"));
    }
    
    /**
     * Encrypts the message and encodes it to a hexadecimal
     * string.
     * 
     * @param message
     * @return
     * @throws Exception
     */
    public String encryptToHex(String message) throws Exception {
        return CipherUtil.byteArrayToHexString(encrypt(message));
    }
    
    /**
     * Decrypt a hexadecimal string encoded cipher text.
     * 
     * @param hex
     * @return
     * @throws Exception
     */
    public String decryptFromHex(String hex) throws Exception {
        return decrypt(CipherUtil.hexStringToByteArray(hex));
    }
    
    /**
     * Decrypt a byte array cipher text.
     * 
     * @param cipherText
     * @return
     * @throws Exception
     */
    public String decrypt(byte[] cipherText) throws Exception {
        if (this._dcipher == null)
            throw new IllegalStateException("CipherUtil has not been initialized for decryption.");
        byte[] clearText = this._dcipher.doFinal(cipherText);
        return new String(clearText,"UTF-8");
    }
    
    
    
    /**
     * Converts a hex string into a byte array
     * @param hexStr
     * @return
     */
    public static byte[] hexStringToByteArray(String hexStr) {
        int strLen = hexStr.length();
        if ((strLen % 2) != 0)
            throw new IllegalArgumentException("Hex string needs to be an even number of characters");
        
        byte[] bytes = new byte[strLen / 2];
        for (int i = 0, j = 0; i< strLen; i += 2, j++) {
            int high = translateChar ( hexStr.charAt( i ));
            int low = translateChar( hexStr.charAt( i + 1));
            
            bytes[j] = (byte)((high << 4) | low);
            
        }
        return bytes;
    }
    
    /**
    * Fast convert a byte array to a hex string
    * with possible leading zero.
    * @param b array of bytes to convert to string
    * @return hex representation, two chars per byte.
    */
    public static String byteArrayToHexString ( byte[] b ) {
      StringBuffer sb = new StringBuffer( b.length * 2 );
      for ( int i=0; i<b.length; i++ ) {

         // look up high nibble char
         sb.append( HEX_CHAR [( b[i] & 0xf0 ) >>> 4] );

         // look up low nibble char
         sb.append( HEX_CHAR [b[i] & 0x0f] );
      }
      return sb.toString();
   }

    
    /**     
     * convert  a single char to corresponding nibble using a precalculated array.
     * Based on code by:
     * Brian Marquis
     * Orion Group Software Engineers http://www.ogse.com
     *      
     * @param c char to convert. must be 0-9 a-f A-F, no
     *          spaces, plus or minus signs.
     *
     * @return corresponding integer  0..15
     * @throws IllegalArgumentException on invalid c.
     */
    private static int translateChar( char c ) {
        
        if ( c > 'f' )
            {
            throw new IllegalArgumentException( "Invalid hex character: " + c );
            }
        int nibble = TRANSLATE_TABLE[ c ];
        if ( nibble < 0 )
            {
            throw new IllegalArgumentException( "Invalid hex character: " + c );
            } 
        return nibble;
    }

    
    /**
     * Test program
     * 
     * @param args
     */
    public static void main(String args[]) {
        
        CipherUtil cipherUtilClient = null,
                   cipherUtilServer = null;
        try {
            File pub = new File(args[1]);
        
            // Client only needs public key
            cipherUtilClient = new CipherUtil(null, pub.toURL());                    
            String cipherTextHex = cipherUtilClient.encryptToHex(args[2]);

            // Server only needs private key
            cipherUtilServer = new CipherUtil(args[0],null);
            String clearText = cipherUtilServer.decryptFromHex(cipherTextHex);
            assert args[2].compareTo(clearText) == 0;
            System.out.println(clearText);            
                        
            
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        
    }
}
