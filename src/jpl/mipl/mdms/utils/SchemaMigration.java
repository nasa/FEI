package jpl.mipl.mdms.utils;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

public class SchemaMigration {

    
    private static Connection _conn;
    private static HashMap _map = new HashMap();
    private static HashMap _ftIdLocMap = new HashMap();
    
    
    public static void populateStorageLocationsTable() throws SQLException {
        Statement stat = _conn.createStatement();
        Statement stat2 = _conn.createStatement();
        
        ResultSet rs = stat.executeQuery("select distinct location " +
                                         "from fileTypes");
        try {
            String loc = null;
            while (rs.next()) {
                loc = rs.getString("location");
                stat2.executeUpdate("exec addStorageLocation '"+loc+"'");
            }
        
            rs = stat.executeQuery("select slId, location "+
                               "from storageLocations");
            while (rs.next()) {
                
                _map.put(rs.getString("location"),new Integer(rs.getInt("slId")));
            }
            System.out.println("Successfully populated storageLocations table");
        } finally {
            rs.close();
            stat.close();
            stat2.close();
        }
        
    }
    
    
    public static void fillInFileTypesTable() throws SQLException {
        Statement stat = _conn.createStatement();
        try {
            String loc=null;
            String update = null;
            for (Iterator it = _map.keySet().iterator(); it.hasNext(); ){
                loc = (String)it.next();
                update = "update fileTypes " +
                         "set slId = "+((Integer)_map.get(loc)).intValue()+" "+
                         "where location = '"+loc+"'";
                stat.executeUpdate(update);
            }
            System.out.println("Successfully filled in fileTypes table");
        } finally {
            stat.close();
        }
    }
    
    
    public static void fillInFilesTable() throws SQLException {
         Statement stat = _conn.createStatement();
         ResultSet rs = null;
         try {
             rs = stat.executeQuery("select ftId, location "+
                                    "from fileTypes");

             while (rs.next()) {
                 _ftIdLocMap.put(new Integer(rs.getInt("ftId")), rs.getString("location"));
             }
         

             Integer ftId = null;
             for (Iterator it = _ftIdLocMap.keySet().iterator(); it.hasNext(); ) {
                 ftId = (Integer)it.next();
                 stat.executeUpdate("update files "+
                                    "set location='"+_ftIdLocMap.get(ftId)+"' "+
                                    "where ftId = "+ftId.intValue());
             }
             System.out.println("Successfully filled in files table");
         } finally {
             rs.close();
             stat.close();
         }
         
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: SchemaMigration configFile");
            System.exit(0);
        }
        
        Properties props = new Properties();
        props.load(new FileInputStream(args[0]));
        System.setProperty("jdbc.drivers",props.getProperty("db_driver"));
        try {
            _conn =  DriverManager.getConnection(props.getProperty("db_uri"),
                                            props.getProperty("db_user"),
                                            props.getProperty("db_password"));
        
            populateStorageLocationsTable();
            fillInFileTypesTable();
            fillInFilesTable();
        } finally {
            _conn.close();
        }

    }

}
