package jpl.mipl.mdms.test.FileService.komodo.fileMgmt;

import jpl.mipl.mdms.FileService.komodo.fileMgmt.FileTypeMgr;
import jpl.mipl.mdms.FileService.komodo.fileMgmt.FileMgrDBMS;
import jpl.mipl.mdms.FileService.komodo.util.Configuration;
import jpl.mipl.mdms.FileService.komodo.fileMgmt.FileTypeInfo;
import jpl.mipl.mdms.FileService.komodo.fileMgmt.UserProfile;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.util.FileUtil;

import java.util.Hashtable;
import java.util.LinkedList;

/**
 * @author txh
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class testFileMgrDBMS {

   public static void main(String[] args) {
      try {
         Configuration.loadConfiguration(
            "/home/txh/Development/java/config/Komodo.cnf");

         FileTypeMgr.initialize(
            "RfsDB_Pool",
            "/tmp",
            "rfsSwRole",
            "mdms1",
            true);

         FileMgrDBMS dbmsAccess = FileTypeMgr.getDbmsAccess();
         //            new FileMgrDBMS("RfsDB_Pool", "/tmp", "rfsSwRole");
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

}
