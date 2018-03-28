/**
 *  @copyright Copyright 2003, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.test.connection;

import jpl.mipl.mdms.connection.SocketConnector;
import jpl.mipl.mdms.connection.Reactor;
import jpl.mipl.mdms.utils.MDMS;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author T. Huang, R. Pavlovsky
 * @version $Id: TestClient.java,v 1.3 2003/09/10 19:33:37 rap Exp $
 */
public class TestClient {

    /**
     * Main method
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            MDMS.ERROR(
                "Usage: TestClient <hostname> <input file> <output file>");
            return;
        }
        try {
            InetSocketAddress addr =
                new InetSocketAddress(InetAddress.getByName(args[0]), 8123);
            Reactor reactor = new Reactor();
            TestConnectHandler handler =
                new TestConnectHandler(reactor, args[1], args[2]);
            SocketConnector connector = new SocketConnector(reactor);
            connector.connect(handler, addr);
            reactor.run();
        } catch (IOException ex) {
            MDMS.ERROR(ex.getMessage());
        }
    }
}
