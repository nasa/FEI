/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.FileService.komodo.fileMgmt;

import java.util.Properties;

import jpl.mipl.mdms.FileService.komodo.registry.Registry;
import jpl.mipl.mdms.FileService.komodo.transaction.FileTypeManager;
import jpl.mipl.mdms.FileService.komodo.util.Configuration;
import junit.framework.TestCase;

/**
 * JUnit test case for the Komodo File Manager DBMS access
 *
 * @author T. Huang, R. Pavlovsky
 * @version $Id: FileMgrDBMSTest.java,v 1.3 2003/09/26 00:12:11 txh Exp $
 */
public class FileMgrDBMSTest extends TestCase {
	private Properties _props = System.getProperties();
	private Registry _registry;
	private String _confFile;

	/**
	 * Constructor
	 *
	 * @param name the test suite name
	 */
	public FileMgrDBMSTest(String name) {
		super(name);
	}

	/**
	 * Override the TestCase setUp method to initialize test environment.
	 *
	 * @throws Exception when general failure
	 */
	public void setUp() throws Exception {
		this._confFile = this._props.getProperty("komodo.conffile");
		Configuration.loadConfiguration(this._confFile);

/*
		FileTypeManager.initialize(
			"RfsDB_Pool",
			"/tmp",
			"rfsSwRole",
			"mdms1",
			true);
			*/
		this._registry = FileTypeManager.getRegistry();
	}

	/**
	 * Override parent tearDown method to cleanup after testing.
	 */
	public void tearDown() {
		this._registry = null;
	}

	/**
	 * Simple test of FileMgsDBMS
	 * 
	 * @throws Exception when general failure
	 */
	public void testFileMgrDBMS() throws Exception {
		// no test for now, SybaseRegistry is rapidly changing
	}

	/*
	   public static void main(String[] args) {
	      try {
	         Configuration.loadConfiguration(
	            "/home/txh/Development/java/config/Komodo.cnf");
	
	         FileTypeManager.initialize(
	            "RfsDB_Pool",
	            "/tmp",
	            "rfsSwRole",
	            "mdms1",
	            true);
	
	         SybaseRegistry dbmsAccess = FileTypeManager.getDbmsAccess();
	         //            new SybaseRegistry("RfsDB_Pool", "/tmp", "rfsSwRole");
	         dbmsAccess.loadFileServerParams("defaultServerProperties", false);
	         dbmsAccess.loadFileServerParams("defaultServerProperties", false);
	         dbmsAccess.getFileServerConfig("mdms1");
	
	         Hashtable fileTypes = dbmsAccess.getFileTypeInfoTable("mdms1");
	         dbmsAccess.startServer();
	         System.out.println(dbmsAccess.showServerParameters("mdms1"));
	         Hashtable table = dbmsAccess.getFileTypeInfoTable("mdms1");
	
	         ((FileTypeInfo) table.get("image1")).dump(System.out);
	         ((FileTypeInfo) table.get("image2")).dump(System.out);
	         ((FileTypeInfo) table.get("image3")).dump(System.out);
	
	         UserProfile up = new UserProfile(dbmsAccess, "txh_reg", "txh_reg");
	         dbmsAccess.getUserVFTCapabilities("txh_reg", up);
	         Result r =
	            new Result(
	               1,
	               "thomas2.jpg",
	               "txh_reg",
	               9878,
	               "09/28/2002",
	               null,
	               "another dummy note");
	         int fileId = dbmsAccess.startAddFile(r);
	         System.out.println(
	            "file list: " + dbmsAccess.getFileList(1, null, null));
	
	         dbmsAccess.addFileComplete(fileId, null);
	
	         fileId = dbmsAccess.startRenameTrans(1, "thomas2.jpg", "foobar.jpg");
	         dbmsAccess.renameFile(fileId, "foobar.jpg");
	
	         fileId = dbmsAccess.lockFile("foobar.jpg", 1, 3);
	
	         Result r2 =
	            new Result(
	               1,
	               "foobar.jpg",
	               "rap_reg",
	               2048,
	               "09/30/2002",
	               null,
	               "rap replace");
	         dbmsAccess.replaceFile(fileId, r2);
	
	         fileId = dbmsAccess.startGetFile(r2);
	         dbmsAccess.releaseFileLock(fileId);
	
	         System.out.println(
	            "file list: " + dbmsAccess.getFileList(1, "*jpg", 'l'));
	
	         System.out.println(
	            "file names: " + dbmsAccess.getFileNames(1, "foo*jpg", 'l'));
	
	         System.out.println(
	            "file names: "
	               + dbmsAccess.getFileNames(1, "09/17/2002", "2002-09-18"));
	
	         dbmsAccess.checkForUser("txh_reg", "txh_reg", up);
	         System.out.println(up);
	         dbmsAccess.lockFileType(1);
	         System.out.println(
	            "locked file list: " + dbmsAccess.getFileList(1, "*jpg", 'l'));
	
	         if (dbmsAccess.isFileTypeLocked(1))
	            System.out.println("FileType 1 is locked");
	         dbmsAccess.unlockFileType(1);
	
	         int receipt =
	            dbmsAccess.transferReceipt(5, fileId, 1, 'g', "dsfsdwrwerew");
	
	         dbmsAccess.addVFT("adding pov", "txh_reg", "a dummy pov", true);
	         System.out.println(dbmsAccess.showVFT("adding pov", "09/20/2002"));
	         dbmsAccess.startDelVFT("adding pov");
	         dbmsAccess.delVFT("adding pov");
	         //dbmsAccess.deleteFile(fileId);
	
	         dbmsAccess.addFileType(
	            "image99",
	            "/project/mdms/txh/komodo/image99",
	            "Test java file type",
	            "null",
	            "null",
	            "null",
	            "f",
	            "null");
	
	         LinkedList filetypelist = dbmsAccess.getFileTypeNames("mdms1");
	         for (int i = 0;
	            i <= filetypelist.indexOf(filetypelist.getLast());
	            ++i)
	            System.out.println(filetypelist.get(i));
	
	         String password = "txh388";
	         System.out.println(
	            "encrypted txh_reg: "
	               + FileUtil.encryptMessage("txh_reg".getBytes()));
	         System.out.println(
	            "encrypted txh_read: "
	               + FileUtil.encryptMessage("txh_read".getBytes()));
	         System.out.println(
	            "encrypted txh_write: "
	               + FileUtil.encryptMessage("txh_write".getBytes()));
	         System.out.println(
	            "encrypted txh_admin: "
	               + FileUtil.encryptMessage("txh_admin".getBytes()));
	
	      } catch (Exception e) {
	         System.err.println(e.getMessage());
	         e.printStackTrace();
	      }
	   }
	   
	   */

}
