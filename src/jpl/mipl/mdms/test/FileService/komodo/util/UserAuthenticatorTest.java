/*
 * Created on Oct 12, 2005
 */
package jpl.mipl.mdms.test.FileService.komodo.util;

import java.io.File;

import javax.swing.JOptionPane;

import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.util.UserAuthenticator;
import junit.framework.TestCase;

/**
 * <b>Purpose:</b>
 * Test cases for UserAuthenticator class.
 * 
 *   <PRE>
 *   Copyright 2005, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2005.
 *   </PRE>
 * 
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 10/12/2005        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: UserAuthenticatorTest.java,v 1.2 2006/07/25 00:14:50 ntt Exp $
 *
 */

public class UserAuthenticatorTest extends TestCase
{
    protected String domainFilename;

    protected String servergroup;
    protected String filetype;
    protected String username;
    protected String password;
    
    protected static final String[] usernames = {"ntt", "ntt", "ttn", "ttn", "ntt", "ntt", null, "ntt"};
    protected static final String[] passwords = {"ntt388", "ntt388", "883ttn", "883ttn", "ntt388", "ntt388", "ntt388", null};
    protected static final String[] servergroups = {"dev", "dev", "dev", "dev", "ved", "dev", "dev", "dev"};
    protected static final String[] filetypes = {null, "type1", null, "type1", "type1", "typo1", "type1", "type1"};
    
    protected final static int ID_USERCONNSG     = 0;  
    protected final static int ID_USERCONNFT     = 1;
    protected final static int ID_USERNOCONNSG   = 2;
    protected final static int ID_USERNOCONNFT   = 3;
    protected final static int ID_BADSERVERGROUP = 4;
    protected final static int ID_BADFILETYPE    = 5;
    protected final static int ID_BADUSERNAME    = 6;
    protected final static int ID_BADPASSWORD    = 7;
    
    protected UserAuthenticator ua;
    
    public static void main(String[] args)
    {
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        ua = new UserAuthenticator(this.domainFilename);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        ua = null;
        super.tearDown();        
    }

    /**
     * Constructor for UserAuthenticatorTest.
     * @param arg0
     */
    public UserAuthenticatorTest(String arg0)
    {
        super(arg0);
        
        
        //get the domain file
        domainFilename = System.getProperty(Constants.PROPERTY_DOMAIN_FILE);
        if (domainFilename == null)
        {
            throw new RuntimeException("Could not find value for property: "+
                                       Constants.PROPERTY_DOMAIN_FILE);
        }
        if (!((new File(domainFilename))).canRead())
        {
            JOptionPane.showMessageDialog(null, "Domain file '"
                    + domainFilename
                    + "' \ndoes not exist or cannot be read.\n\n"
                    + "Use the Options Menu to load an exising domain file.",
                    "Session Error", JOptionPane.ERROR_MESSAGE);
        }        
    }
    
    public void testUserConnectSG() throws Exception
    {
        assignFields(ID_USERCONNSG);
        tryAuthenticate(true);
    }
    
    public void testUserConnectFt() throws Exception
    {
        assignFields(ID_USERCONNFT);
        tryAuthenticate(true);
    }
    
    
    public void testUserNoConnectSG() throws Exception
    {
        assignFields(ID_USERNOCONNSG);
        tryAuthenticate(false);
    }
    
    public void testUserNoConnectFt() throws Exception
    {
        assignFields(ID_USERNOCONNFT);
        tryAuthenticate(false);       
    }

    public void testUserNoConnectBadSg() throws Exception
    {        
        assignFields(ID_BADSERVERGROUP);
        tryAuthenticate(false);        
    }
    
    public void testUserNoConnectBadFt() throws Exception
    {
        assignFields(ID_BADFILETYPE);
        tryAuthenticate(false);       
    }
    
    public void testNoConnectNullUser() throws Exception
    {        
        assignFields(ID_BADUSERNAME);
        tryAuthenticate(false);        
    }
    
    public void testNoConnectNullPassword() throws Exception
    {
        assignFields(ID_BADPASSWORD);
        tryAuthenticate(false);       
    }
    
    protected void assignFields(int index)
    {
        if (index < 0 || index > usernames.length)
            index = 0;
        this.username    = usernames[index];
        this.password    = passwords[index];
        this.servergroup = servergroups[index];
        this.filetype    = filetypes[index];
    }
    
    protected void tryAuthenticate(boolean shouldFail)
    {
        boolean ok = false;
    
        try {
            ok = ua.authenticate(username, password, servergroup, filetype);
        } catch (Exception ex) {
            ok = false;
            ex.printStackTrace();
        }
        
        assertTrue(shouldFail == ok);        
    }
    
    
}
