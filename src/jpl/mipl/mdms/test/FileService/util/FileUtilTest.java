package jpl.mipl.mdms.test.FileService.util;

import jpl.mipl.mdms.FileService.util.FileUtil;

public class FileUtilTest {
    public static void main(String args[]) {
        System.out.println(FileUtil.encryptMessage("testing".getBytes(), "SHA2"));
        System.out.println(FileUtil.encryptMessage("testing".getBytes(),"MD5"));
    }

}
