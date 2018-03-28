/**
 * @copyright Copyright 2003, California Institute of Technology. ALL RIGHTS
 *            RESERVED. U.S. Government Sponsorship acknowledged. 29-6-2000.
 *            MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.FileService.komodo.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;

import jpl.mipl.mdms.FileService.komodo.xml.DomXmlParser;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Komodo XML parser for client Help information.
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: HelpXMLParser.java,v 1.7 2004/10/27 00:52:39 txh Exp $
 */
public class HelpXMLParser extends DomXmlParser {
   private static Stack _stack = new Stack();

   /**
    * Constructor, references super class constructor.
    */
   public HelpXMLParser() {
      super();
   }

   /**
    * Constructor, references super class constructor.
    * 
    * @param schema String pointing to KomodHelp XSD file
    */
   public HelpXMLParser(InputStream schema) {
      // disabling document validation.
      super(schema, false);
   }

   /**
    * Get the Help information in java.util.Properties form.
    * 
    * @param uri String points to KomodoHelp XML file to be parsed
    * @return Properties file containing Help Information
    * @throws IOException when I/O failure
    * @throws SAXException when XML parsing or validation failure
    */
   public Properties getProperties(String uri) throws IOException, SAXException {
      if (uri == null)
         throw new IOException("XML uri cannot be null.");

      return this.getProperties(new FileInputStream(new File(uri)));
   }

   /**
    * Get the Help information in java.util.Properties form.
    * 
    * @param is InputStream containing serialized XML KomodoHelp file
    * @return Properties file containing Help Information
    * @throws IOException when I/O failure
    * @throws SAXException when XML parsing or validation failure
    */
   public Properties getProperties(InputStream is) throws IOException,
         SAXException {

      if (is == null)
         throw new IOException("XML input stream cannot be null.");

      Document doc = null;
      try {
         doc = this.parse(is);
         doc.getDocumentElement().normalize();

      } catch (SAXException e) {
         throw new SAXException(e.getMessage());

      } catch (IOException e) {
         throw new IOException(e.getMessage());
      }

      Node node = doc.getDocumentElement();

      if (!node.getNodeName().equals("KomodoHelp")) {
         throw new IOException("Could not find required XML document "
               + "element: KomodoHelp");
      }

      HelpXMLParser._traverse(node);

      StringBuffer buf = new StringBuffer();
      Properties props = new Properties();

      while (!HelpXMLParser._stack.empty()) {
         String s = (String) HelpXMLParser._stack.pop();
         buf.insert(0, s);

         if (s.startsWith("help.cmd") && buf.length() != 0) {
            // Command Help Information:
            // Tokenizer will acquire cmd help tokens in the following order:
            // 1st token will be cmd name in form of help.cmd.xxxx
            // 2nd token will be cmd type in form type:xxxx
            // The rest of the tokens will be the stmt arguments (there might
            // not be any
            // arguments).
            StringTokenizer tokens = new StringTokenizer(buf.toString(), "|");

            // Throw exception if token count is less then expected.
            if (tokens.countTokens() < 2)
               throw new IOException("Invalid number of tokens in cmd help.");

            // First token is cmd help name (order is important)
            String key = tokens.nextToken().toLowerCase();
            String name = key.substring(key.lastIndexOf('.') + 1, key.length());

            // Create help Properties object to store all the props
            Properties help = new Properties();
            help.setProperty("name", name);

            // Parse through remaining tokens, add to Properties object
            int tokNum = tokens.countTokens();
            for (int i = 0; i < tokNum; i++) {
               String t = tokens.nextToken();
               if (t.startsWith("type:")) {
                  help.setProperty("type", t.substring(t.indexOf(':') + 1, t
                        .length()));

               } else if (t.startsWith("desc:")) {
                  help.setProperty("description", t.substring(
                        t.indexOf(':') + 1, t.length()));

               } else if (t.startsWith("usage:")) {
                  help.setProperty("usage", t.substring(t.indexOf(':') + 1, t
                        .length()));

               } else if (t.startsWith("shortcut:")) {
                  help.setProperty("shortcut", t.substring(
                        t.lastIndexOf(':') + 1, t.length()));

               } else if (t.startsWith("also:")) {
                  help.setProperty("also", t.substring(t.lastIndexOf(':') + 1,
                        t.length()));

               } else if (t.startsWith("alt:")) {
                  help.setProperty("alt", t.substring(t.lastIndexOf(':') + 1, t
                        .length()));
               }
            }

            // Put the help Properties object into another Properties object
            // which is indexed by cmd help name
            props.put(name, help);
            buf.delete(0, buf.length());
         }
      }

      return props;
   }

   /**
    * Private recursive method to visit nodes and their children and look for
    * particular bits of information (i.e. NodeName or AttributeName).
    * Information from the nodes is pushed onto a stack.
    * 
    * @param node DOM Document node
    */
   private static void _traverse(Node node) {
      NodeList children = node.getChildNodes();

      if (children != null) {
         for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeName().equals("cmd")) {
               String name = child.getAttributes().getNamedItem("name")
                     .getLastChild().getNodeValue();
               String type = child.getAttributes().getNamedItem("type")
                     .getLastChild().getNodeValue();
               /*
                * String shortcut = child .getAttributes()
                * .getNamedItem("shortcut") .getLastChild() .getNodeValue();
                * String alt = child .getAttributes() .getNamedItem("alt")
                * .getLastChild() .getNodeValue(); String see_also = child
                * .getAttributes() .getNamedItem("see_also") .getLastChild()
                * .getNodeValue();
                */

               HelpXMLParser._stack.push("help.cmd." + name + "|");
               HelpXMLParser._stack.push("type:" + type + "|");

               /*
                * if (shortcut != null) { HelpXMLParser._stack.push("shortcut:" +
                * shortcut); }
                * 
                * if (alt != null) { HelpXMLParser._stack.push("alt:" + alt); }
                * 
                * if (see_also != null) { HelpXMLParser._stack.push("see_also:" +
                * see_also); }
                */

               HelpXMLParser._traverse(child);

            } else if (child.getNodeName().equals("desc")) {
               HelpXMLParser._stack.push("desc:"
                     + child.getLastChild().getNodeValue() + "|");
            } else if (child.getNodeName().equals("usage")) {
               HelpXMLParser._stack.push("usage:"
                     + child.getLastChild().getNodeValue() + "|");
            } else if (child.getNodeName().equals("param")) {
               String name = child.getAttributes().getNamedItem("name")
                     .getLastChild().getNodeValue();
               HelpXMLParser._stack.push("param:" + name + "|");

            } else {
               HelpXMLParser._traverse(child);
            }
         }
      }
   }
}