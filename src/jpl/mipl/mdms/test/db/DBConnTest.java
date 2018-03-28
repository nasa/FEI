package jpl.mipl.mdms.test.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnTest {
    public static void main(String args[]) throws Exception {
        //Class.forName("com.mysql.jdbc.Driver").newInstance();
        Class.forName("com.sybase.jdbc2.jdbc.SybDriver").newInstance();
//        Connection con = DriverManager.getConnection(
//                "jdbc:mysql://mdms-dev.jpl.nasa.gov:3306/komodo_build",
//                "awt",
//                "49Labskita");
        
        Connection con = DriverManager.getConnection(
                "jdbc:sybase:Tds:mipldb-dev.jpl.nasa.gov:1040/komodo_dev",
                "sa",
                "**put-password-here**");
        con.setAutoCommit(false);
        Statement stmt = con.createStatement();
        //ResultSet rs = stmt.executeQuery("SELECT * FROM serverGroups");
        try {
            System.out.println("Will execute query");
            ResultSet rs = stmt.executeQuery("UPDATE serverGroups SET comment = 'MDMS DEV server group' WHERE sgId = 1");
            System.out.println("here");
            while (rs.next()) {
                String sg  = rs.getString("name");
                System.out.println(sg);
            }
            con.commit();
        } catch (SQLException sqle) {
            System.err.println("rolling back");
            con.rollback();
            sqle.printStackTrace();
        }
    }
}
