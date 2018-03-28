/**
 *  @copyright Copyright 2004, California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government Sponsorship acknowledged. 29-6-2000.
 *  MIPL Data Management System (MDMS).
 */

package jpl.mipl.mdms.FileService.komodo.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Abstract base class for DOM XML parser. Currently we are using Apache
 * Xerces-J for our DOM parser, but the code should be portable enough to
 * plug-in other parsers. NOTE: To use this parser class, please pass in the
 * following System parameter:
 * 
 * -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl
 * 
 * @author R. Pavlovsky, {Rich.Pavlovsky@jpl.nasa.gov}
 * @version $Id: DomXmlParser.java,v 1.1 2004/08/18 21:35:26 rap Exp $
 */
public abstract class DomXmlParser {
	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	private DocumentBuilderFactory _factory;

	private DocumentBuilder _parser;

	private ErrorCatcher _ec;

	private boolean _isValid = false;

	/**
	 * Constructor, creates XML parsing factory. XML schema location is not set
	 * but used implicitly from XML file reference. By default validation is
	 * disabled.
	 */
	protected DomXmlParser() {
		this(false);
	}

	/**
	 * Constructor, creates XML parsing factory. XML schema location is not set
	 * but used implicitly from XML file reference. The validate flag allows
	 * subclass to enable/disable document validation.
	 * 
	 * @param validate
	 *            Validation flag.
	 */
	protected DomXmlParser(boolean validate) {
		this._factory = DocumentBuilderFactory.newInstance();
		this._factory.setValidating(validate);
		this._factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
	}

	/**
	 * Constructor, creates XML parsing factory. XML schema location is set to
	 * the String input. XML validation is disabled by default.
	 * 
	 * @param schema
	 *            String pointing to KomodoDbStmts XSD file
	 */
	protected DomXmlParser(String schema) {
		this(false);
		this._factory.setAttribute(JAXP_SCHEMA_SOURCE, schema);
	}

	/**
	 * Constructor, creates XML parsing factory. XML schema location is set to
	 * the String input.
	 * 
	 * @param schema
	 *            schema String pointing to KomodoDbStmts XSD file
	 * @param validate
	 *            Boolean flag to enable or disable XML validation.
	 */
	protected DomXmlParser(String schema, boolean validate) {
		this(validate);
		this._factory.setAttribute(JAXP_SCHEMA_SOURCE, schema);
	}

	/**
	 * Constructor, creates XML parsing factory. XML schema is passed in via
	 * serialized inputstream. This method should be used when validating XML
	 * docs within a jar file. XML validation is disable by default.
	 * 
	 * @param schema
	 *            reference to input stream object containing XML schema
	 */
	protected DomXmlParser(InputStream schema) {
		this(false);
		this._factory.setAttribute(JAXP_SCHEMA_SOURCE, schema);
	}

	/**
	 * Constructor, creates XML parsing factory. XML schema is passed in via
	 * serialized input stream. This method should be used when validating XML
	 * docs within a jar file.
	 * 
	 * @param schema
	 *            reference to input stream object containing XML schema
	 * @param validate
	 *            Boolean flag to enable or disable XML validation
	 */
	protected DomXmlParser(InputStream schema, boolean validate) {
		this(validate);
		this._factory.setAttribute(JAXP_SCHEMA_SOURCE, schema);
	}

	/**
	 * Parses and validates (if enabled) XML file, returns DOM Document.
	 * 
	 * @param uri
	 *            points to XML file to be parsed
	 * @return DOM Document containing parsed XML structure
	 * @throws SAXException
	 *             when XML parsing validating failure
	 * @throws IOException
	 *             when I/O failure
	 */
	public Document parse(String uri) throws SAXException, IOException {
		if (uri == null)
			throw new IOException("XML uri cannot be null.");

		return this._parse(new FileInputStream(new File(uri)));
	}

	/**
	 * Parses and validates XML input stream, returns DOM Document. This method
	 * should be used when accessing XML docs with a jar file.
	 * 
	 * @param is
	 *            reference to input stream object
	 * @return Document containing parsed XML structure
	 * @throws SAXException
	 *             when XML parsing validating failure
	 * @throws IOException
	 *             when I/O failure
	 */
	public Document parse(InputStream is) throws SAXException, IOException {
		if (is == null)
			throw new IOException("XML input stream cannot be null.");
		return this._parse(is);
	}

	/**
	 * Private method to do the actual DOM parsing.
	 * 
	 * @param is
	 *            InputStream containing serialized XML content to be parsed
	 * @return DOM Document contain parsed XML tree
	 * @throws SAXException
	 *             when XML parsing validating fails
	 * @throws IOException
	 *             when general I/O failure
	 */
	private Document _parse(InputStream is) throws SAXException, IOException {
		Document doc = null;
		try {
			this._parser = this._factory.newDocumentBuilder();
			this._ec = new ErrorCatcher();
			this._parser.setErrorHandler(this._ec);
			doc = this._parser.parse(is);
			this._isValid = this._ec.isValid();
		} catch (ParserConfigurationException e) {
			throw new SAXException("XML Parser improperly configured: "
					+ e.getMessage());
		}

		return doc;
	}

	/**
	 * Accessor method to see if XML file validated successfully. NOTE: This
	 * method will return false if validation is disabled.
	 * 
	 * @return boolean true of XML file is valid, false if it is not.
	 */
	public boolean isValid() {
		return this._isValid;
	}

	/**
	 * Abstract method to get a Property list of parsed XML values.
	 * 
	 * @param uri
	 *            String points to Komodo DBMS stmts XML file to be parsed
	 * @return Properties list containing parsed XML values
	 * @throws SAXException
	 *             when XML parsing validating failure
	 * @throws IOException
	 *             when I/O failure
	 */
	public abstract Properties getProperties(String uri) throws IOException,
			SAXException;

	/**
	 * Abstract method to get a Property list of parsed XML values.
	 * 
	 * @param is
	 *            InputStream containing serialized XML KomodoDbStmts file
	 * @return Properties list containing parsed XML values
	 * @throws SAXException
	 *             when XML parsing validating failure
	 * @throws IOException
	 *             when I/O failure
	 */
	public abstract Properties getProperties(InputStream is)
			throws IOException, SAXException;
}