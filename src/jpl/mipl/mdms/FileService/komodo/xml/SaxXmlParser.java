/**
 *  @copyright Copyright 2004, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledge. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.FileService.komodo.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Abstract base class for SAX XML parser. Currently we are using Apache
 * Xerces-J for our SAX parser, but the code should be portable enough to
 * plug-in other parsers. NOTE: To use this parser class, please pass in the
 * following System parameters:
 * <PRE>
 *    -Dorg.xml.sax.driver=org.apache.xerces.parsers.SAXParser
 *    -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl
 * </PRE>
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: SaxXmlParser.java,v 1.5 2006/12/13 23:52:53 ntt Exp $
 */
public abstract class SaxXmlParser {
    private boolean _validate = false;

    private boolean _isValid = false;

    private ContentHandler _handler;

    private String _schema = null;

    /**
     * Constructor, sets the SAX ContentHandler class to parameter input. XML
     * validation is disabled by default.
     * 
     * @param handler Must implement ContentHandler interface to catch messages
     *        when XML document is parsed
     */
    protected SaxXmlParser(ContentHandler handler) {
        this._handler = handler;
    }

    /**
     * Constructor, sets the SAX ContentHandler class to parameter input. XML
     * validation is disabled by default.
     * 
     * @param handler Must implement ContentHandler interface to catch messages
     *        when XML document is parsed
     * @param validate Boolean flag to enable or disable XML validation
     */
    protected SaxXmlParser(ContentHandler handler, boolean validate) {
        this._handler = handler;
        this._validate = validate;
    }

    /**
     * Constructor, sets the SAX ContentHandler class to parameter input. XML
     * validation is disabled by default.
     * 
     * @param handler Must implement ContentHandler interface to catch messages
     *        when XML document is parsed
     * @param schema String location of XML schema document
     */
    protected SaxXmlParser(ContentHandler handler, String schema) {
        this._handler = handler;
        this._schema = schema;
    }

    /**
     * Constructor, sets the SAX ContentHandler class to parameter input. XML
     * validation is disabled by default.
     * 
     * @param handler Must implement ContentHandler interface to catch messages
     *        when XML document is parsed
     * @param schema String location of XML schema document
     * @param validate Boolean flag to enable or disable XML validation
     */
    protected SaxXmlParser(ContentHandler handler, String schema,
            boolean validate) {
        this._handler = handler;
        this._schema = schema;
        this._validate = validate;
    }

    /**
     * Parses via SAX the XML document, use ContentHandler class to get the
     * data. This class will serialize the XML data into an InputStream and pass
     * it to the SAX parser.
     * 
     * @param uri points to XML file to be parsed
     * @throws SAXException when XML parsing validating failure
     * @throws IOException when I/O failure
     */
    public void parse(String uri) throws SAXException,
            ParserConfigurationException, IOException {
        if (uri == null)
            throw new IOException("XML uri cannot be null.");

        this._parse(new InputSource(new FileInputStream(new File(uri))));

    }

    /**
     * Parses and validates XML input stream, use ContentHandler class to get
     * the data. This method should be used when accessing XML docs within a jar
     * file.
     * 
     * @param is reference to input stream object
     * @throws SAXException when XML parsing validating failure
     * @throws IOException when I/O failure
     */
    public void parse(InputStream is) throws SAXException,
            ParserConfigurationException, IOException {
        if (is == null)
            throw new IOException("XML inputstream cannot be null.");
        this._parse(new InputSource(is));
    }

    /**
     * Private method to do the actual SAX parsing. I've converted the String
     * XML location and InputStream to InputSource to conform with the SAX api.
     * 
     * @param source Source XML file to be parsed
     * @throws SAXException when parse fails or document fails validation
     * @throws IOException when source XML stream cannot be read
     * @throws ParserConfigurationException when parser config error
     */
    private void _parse(InputSource source) throws SAXException, IOException,
            ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        if (this._validate) {
            factory.setValidating(true);
            // Validate the document and report validity errors.
            factory.setFeature("http://xml.org/sax/features/validation", true);
            // Turn on XML Schema validation by inserting XML Schema validator
            // in the pipeline. NOTE: Current SAX default validator uses DTDs
            factory.setFeature(
                    "http://apache.org/xml/features/validation/schema", true);
            // Enable full schema grammar constraint checking, including
            // checking which may be time-consuming or memory intensive.
            // Currently, particle unique attribution constraint checking and
            // particle derivation resriction checking are controlled by this
            // option.
            factory
                    .setFeature(
                            "http://apache.org/xml/features/validation/schema-full-checking",
                            true);
            // Report the original prefixed names and attributes used for
            // namespace declarations.
            factory.setFeature(
                    "http://xml.org/sax/features/namespace-prefixes", true);
            
        }
        DefaultHandler handler = (DefaultHandler) this._handler;
        SAXParser parser = factory.newSAXParser();
        if (this._schema != null)
            // The XML Schema Recommendation explicitly states that the
            // inclusion of schemaLocation/ noNamespaceSchemaLocation
            // attributes is only a hint; it does not mandate that these
            // attributes must be used to locate schemas. This property
            // allows the user to specify a list of schemas to use. If the
            // targetNamespace of a schema (specified using this property)
            // matches the targetNamespace of a schema occuring in the
            // instance document in schemaLocation attribute, the schema
            // specified by the user using this property will be used (i.e.,
            // the instance document's schemaLocation attribute will be
            // effectively ignored).
            parser
                    .setProperty(
                            "http://apache.org/xml/properties/schema/external-schemaLocation",
                            this._schema);
        
        parser.parse(source, handler);
    }

    /**
     * Accessor method to see if XML file validated successfully.
     * 
     * @return boolean true of XML file is valid, false if it is not.
     */
    public boolean isValid() {
        return this._isValid;
    }

    /**
     * Accessor method to get SAX ContentHandler object
     * 
     * @return SAX ContentHandler object
     */
    public ContentHandler getHandler() {
        return this._handler;
    }
}