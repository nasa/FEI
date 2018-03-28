package jpl.mipl.mdms.test.FileService.komodo.client;

import java.io.File;

import jpl.mipl.mdms.FileService.komodo.api.Client;
import jpl.mipl.mdms.FileService.komodo.api.Constants;
import jpl.mipl.mdms.FileService.komodo.api.Result;
import jpl.mipl.mdms.FileService.komodo.util.LoginFile;

public class SimpleClientTest {
    
    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        
        // You'll find the domain file in $FEI5/domain.fei
        String domainFile = args[0];
        
        // You'll find the trustore in $FEI5/mdms-fei.keystore
        String trustStore = args[1];
        
        // Find in $HOME/.komodo/.komodologin
        String loginCacheFileName = args[2];
        
        // server group
        String sGroup = args[3];
        // a filetype
        String type = args[4];
        
        // test file
        String testFileName = args[5];
        
        // Read login file
        LoginFile loginCacheFile = new LoginFile(new File(loginCacheFileName));
        
        // Retrieve the username and password from the login cache file
        // that is associated with the server group.  If no username/password
        // is associated with server group, use default username/password
        // in the login cache file.
        String user = loginCacheFile.getUsername(sGroup,true);
        String password = loginCacheFile.getPassword(sGroup,true);
        
        //  Create a Client object
        Client client = new Client(domainFile, trustStore);
        
        //  Client login to server
        //  Provide username, password, servergroup, and filetype
        System.out.println("Client logging in to server...");
        client.login(user,password,sGroup,type);

        //  Perform a list operation
        System.out.println("\nClient performing a list operation...");
        client.show();        
        System.out.println("Results of List Operation");
        while (client.getTransactionCount()>0) {
            // Retrieve the show operation's results
            // from the results queue
            Result r = client.getResult();
            if (r == null) {
                continue;
            }
            if (r.getErrno() == Constants.OK) {
                // Get the return code for each operation
                System.out.println(r.getName());
            } else {
                System.out.println(r.getMessage());
            }
            
        }
        
        
        //  Add a file
        System.out.println("\nAttempting to add file...");
        client.add(testFileName);
        
        while (client.getTransactionCount() > 0) {
            // Get results from result queue
            Result result = client.getResult();
            if (result == null) {
               continue;
            }
            System.out.println(result.getMessage());
        }
        
        
        //  Perform another list operation
        System.out.println("\nClient performing a list operation...");
        // change filetype
        client.setType(sGroup,"type2");
        client.show();        
        System.out.println("Results of List Operation");
        while (client.getTransactionCount()>0) {
            Result r = client.getResult();
            if (r == null) {
                continue;
            }
            if (r.getErrno() == Constants.OK) {
                // Get the return code for each operation
                System.out.println(r.getName());
            } else {
                System.out.println("ERROR ("+r.getErrno()+"): "+r.getMessage());
            }
            
        }
        
        //  Perform a delete operation
        System.out.println("\nAttempting to delete file...");
        client.delete(testFileName);
        while (client.getTransactionCount() > 0) {
            Result result = client.getResult();
            if (result == null) {
               continue;
            }
            if (result.getErrno() != Constants.OK) {
               System.out.println(result.getMessage());
               continue;
            }
            System.out.println("Deleted " + result.getName() + ".");
         }
        
        //  Perform another list operation
        System.out.println("\nClient performing a list operation...");
        client.show();        
        System.out.println("Results of List Operation");
        while (client.getTransactionCount()>0) {
            Result r = client.getResult();
            if (r == null) {
                continue;
            }
            if (r.getErrno() == Constants.OK) {
                // Get the return code for each operation
                System.out.println(r.getName());
            } else {
                System.out.println(r.getMessage());
            }
            
        }
        
        // Client logout from the server
        System.out.println("\nClient logging out...");
        client.logout();
    }
}
