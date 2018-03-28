/******************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights re served
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 *****************************************************************************/
package jpl.mipl.mdms.FileService.komodo.help;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import jpl.mipl.mdms.FileService.komodo.client.HelpXMLParser;
import jpl.mipl.mdms.FileService.util.TextFormatter;

import org.xml.sax.SAXException;

/**
 *  Simple help utility for Komodo to handle user help requests
 *
 *  @author G.Turek, R. Pavlovsky
 *  @version $Id: ClientHelp.java,v 1.10 2005/02/09 01:57:18 ntt Exp $
 */
public class ClientHelp {
    private HelpXMLParser _xmlParser;
    private static Properties _help = null;
    private static Properties _helpTypes = null;

    /**
     * Constructor
     *
     * @param xmlStream Stream of xml data to parse
     * @param schemaStream stream of xml schema data to validate the xml file
     */
    public ClientHelp(InputStream xmlStream, InputStream schemaStream) {
        try {
            this._xmlParser = new HelpXMLParser(schemaStream);
            ClientHelp._help = this._xmlParser.getProperties(xmlStream);

        } catch (IOException e) {
            System.err.println("XML parser exception: " + e.getMessage());
        } catch (SAXException e) {
            System.err.println("XML parser exception: " + e.getMessage());
        }

        ClientHelp._helpTypes = new Properties();
        Vector aft = new Vector();
        Vector aut = new Vector();
        Vector ast = new Vector();
        Vector apv = new Vector();
        Vector aad = new Vector();

        Enumeration tokens = ClientHelp._help.elements();
        //System.out.println("Help Elements: " + ClientHelp._help.size());
        while (tokens.hasMoreElements()) {
            Properties info = (Properties) tokens.nextElement();
            if (info.getProperty("type").equals("filetype")) {
                aft.add(info.getProperty("name"));
            } else if (info.getProperty("type").equals("utility")) {
                aut.add(info.getProperty("name"));
            } else if (info.getProperty("type").equals("admin")) {
                aad.add(info.getProperty("name"));
            } else if (info.getProperty("type").equals("settings")) {
                ast.add(info.getProperty("name"));
            } else if (info.getProperty("type").equals("vft")) {
                apv.add(info.getProperty("name"));
            }
        }

        //sort the Vectors now
        Collections.sort(aft);
        Collections.sort(aad);
        Collections.sort(ast);
        Collections.sort(apv);
        Collections.sort(aut);
        
        ClientHelp._helpTypes.put("filetype", TextFormatter.formatText(3, aft));
        ClientHelp._helpTypes.put("admin", TextFormatter.formatText(3, aad));
        ClientHelp._helpTypes.put("settings", TextFormatter.formatText(3, ast));
        ClientHelp._helpTypes.put("vft", TextFormatter.formatText(3, apv));
        ClientHelp._helpTypes.put("utility", TextFormatter.formatText(3, aut));

        String usrCmds =
            " * Settings commands *"
                + "\n"
                + ClientHelp._helpTypes.get("settings")
                + "\n"
                + " * Utility commands *"
                + "\n"
                + ClientHelp._helpTypes.get("utility")
                + "\n"
                + " * Filetype commands *"
                + "\n"
                + ClientHelp._helpTypes.get("filetype")
                + "\n"
                + " * VFT commands *"
                + "\n"
                + ClientHelp._helpTypes.get("vft");

        String admCmds =
            " * Settings commands *"
                + "\n"
                + ClientHelp._helpTypes.get("settings")
                + "\n"
                + " * Utility commands *"
                + "\n"
                + ClientHelp._helpTypes.get("utility")
                + "\n"
                + " * Admin commands *"
                + "\n"
                + ClientHelp._helpTypes.get("admin");

        ClientHelp._helpTypes.put("usr", usrCmds);
        ClientHelp._helpTypes.put("adm", admCmds);

        /*
        try {
         Document doc = XMLDomParser.parseXML(helpURI, false, schemaURI);
         NodeList cmds = doc.getElementsByTagName("Command");
         int nl = cmds.getLength();
        
         Vector aft = new Vector();
         Vector aut = new Vector();
         Vector ast = new Vector();
         Vector apv = new Vector();
         Vector aad = new Vector();
        
         Hashtable help;
         String name, type, desc, usage, alt, shortcut, also;
        
         ClientHelp._commands = new Hashtable(nl);
         ClientHelp._cmdTypes = new Hashtable(6);
        
         for (int i = 0; i < nl; i++) {
            Element el = (Element) cmds.item(i);
            name = el.getAttribute("name");
            type = el.getAttribute("type");
            desc = el.getAttribute("description");
            usage = el.getAttribute("usage");
            alt = el.getAttribute("alt");
            shortcut = el.getAttribute("shortcut");
            also = el.getAttribute("see_also");
        
            if (type.equals("filetype")) {
               aft.add(name);
            } else if (type.equals("utility")) {
               aut.add(name);
            } else if (type.equals("admin")) {
               aad.add(name);
            } else if (type.equals("settings")) {
               ast.add(name);
            } else if (type.equals("vft")) {
               apv.add(name);
            }
        
            Collections.sort(aft);
            Collections.sort(aut);
            Collections.sort(ast);
            Collections.sort(apv);
            Collections.sort(aad);
        
            help = new Hashtable();
            help.put("name", name);
            help.put("description", desc);
            help.put("usage", usage);
            help.put("type", type);
            if (shortcut != null && !shortcut.equals(""))
               help.put("shortcut", shortcut);
            if (alt != null && !alt.equals(""))
               help.put("alt", alt);
            if (also != null && !also.equals(""))
               help.put("also", also);
            ClientHelp._commands.put(name.toLowerCase(), help);
         }
        
         ClientHelp._cmdTypes.put("filetype", TextFormatter.formatText(3, aft));
         ClientHelp._cmdTypes.put("admin", TextFormatter.formatText(3, aad));
         ClientHelp._cmdTypes.put("settings", TextFormatter.formatText(3, ast));
         ClientHelp._cmdTypes.put("vft", TextFormatter.formatText(3, apv));
         ClientHelp._cmdTypes.put("utility", TextFormatter.formatText(3, aut));
        
         String usrCmds =
            " * Settings commands *"
               + "\n"
               + ClientHelp._cmdTypes.get("settings")
               + "\n"
               + " * Utility commands *"
               + "\n"
               + ClientHelp._cmdTypes.get("utility")
               + "\n"
               + " * Filetype commands *"
               + "\n"
               + ClientHelp._cmdTypes.get("filetype")
               + "\n"
               + " * VFT commands *"
               + "\n"
               + ClientHelp._cmdTypes.get("vft");
        
         String admCmds =
            " * Settings commands *"
               + "\n"
               + ClientHelp._cmdTypes.get("settings")
               + "\n"
               + " * Utility commands *"
               + "\n"
               + ClientHelp._cmdTypes.get("utility")
               + "\n"
               + " * Admin commands *"
               + "\n"
               + ClientHelp._cmdTypes.get("admin");
        
         ClientHelp._cmdTypes.put("usr", usrCmds);
         ClientHelp._cmdTypes.put("adm", admCmds);
        } catch (SAXException e) {
         System.err.println("XML parser exception: " + e.getMessage());
        } catch (IOException ioe) {
         System.err.println("XML parser exception: " + ioe.getMessage());
        } catch (ParserConfigurationException pce) {
         System.err.println("XML parser exception: " + pce.getMessage());
        }
        */
    }

    /**
     * Method to return all available commands for the input type
     *
     * @param admin whether it's an admin client requesting help
     * @param name the command type or command name
     * @return a String with a list of all available commands for given type
     */
    public static String getInfo(boolean admin, String name) {
        //If name = null return list of all available commands
        if (name == null) {
            if (admin)
                return (String) ClientHelp._helpTypes.get("adm");
            return (String) ClientHelp._helpTypes.get("usr");
        }

        if (!admin && name.equals("admin"))
            return "Unrecognized command admin";
        if (name.equals("types")) {
            if (!admin)
                return "Command types: vft, utility, filetype, settings";
            return "Command types: utility, settings, admin";
        }

        String cname = name.toLowerCase();
        String help = (String) ClientHelp._helpTypes.get(cname);
        if (help == null) {
            Properties info = (Properties) ClientHelp._help.get(cname);
            if (info == null)
                return "Unrecognized command " + name;
            String type = (String) info.get("type");
            if (!admin && type.equals("admin"))
                return "Unrecognized command " + name;
            help =
                "Command:     "
                    + (String) info.get("name")
                    + "\n"
                    + "Description: "
                    + (String) info.get("description")
                    + "\n"
                    + "Usage:       "
                    + (String) info.get("usage")
                    + "\n"
                    + "Type:        "
                    + type;
            String s = (String) info.get("shortcut");
            if (s != null)
                help += "\n" + "Shortcut:    " + s;
            s = (String) info.get("alt");
            if (s != null)
                help += "\n" + "Alt:         " + s;
            s = (String) info.get("also");
            if (s != null)
                help += "\n" + "See also:    " + s;
        }
        return help;
    }

    /**
     * Method to return usage info for the given command
     *
     * @param name the command name
     * @return usage information for given command
     */
    public static String getUsage(String name) {
        String cname = name.toLowerCase();
        Properties info = (Properties) ClientHelp._help.get(cname);
        if (info == null)
            return "Unrecognized command " + name;
        return "Usage: " + (String) info.get("usage");
    }
}
