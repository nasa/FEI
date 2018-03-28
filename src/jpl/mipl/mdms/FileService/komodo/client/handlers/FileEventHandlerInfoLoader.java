package jpl.mipl.mdms.FileService.komodo.client.handlers;


import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import jpl.mipl.mdms.FileService.komodo.util.UrlInputStreamLoader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Loads and parses file event handler metadata from associated
 * descriptors.
 *  
 *   <PRE>
 *   Copyright 2008, California Institute of Technology.
 *   ALL RIGHTS RESERVED.
 *   U.S. Government Sponsorship acknowledge. 2008.
 *   </PRE>
 *
 * <PRE>
 * ============================================================================
 * <B>Modification History :</B>
 * ----------------------
 *
 * <B>Date              Who              What</B>
 * ----------------------------------------------------------------------------
 * 08/15/2008        Nick             Initial Release
 * ============================================================================
 * </PRE>
 *
 * @author Nicholas Toole   (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: FileEventHandlerInfoLoader.java,v 1.4 2012/03/15 23:05:55 ntt Exp $
 *
 */
public class FileEventHandlerInfoLoader
{
    protected URL descriptorUrl;
    protected FileEventHandlerInfo info;
    
    public final static String FEI_NAMESPACE  = "fei";
    
    /** Name of the name element */
    public final static String ELEMENT_NAME    = "name";

    /** Name of the id element */
    public final static String ELEMENT_ID    = "id";
    
    /** Name of the version element */
    public final static String ELEMENT_VERSION = "version";
    
    /** Name of the description element */
    public final static String ELEMENT_DESC    = "description";

    /** Name of the org element */
    public final static String ELEMENT_ORG = "org";
    
    /** Name of the element defining a handler plugin */
    public static final String ELEMENT_HANDLER    = "handler";
    
    
        /** Name of the class attribute */
        public static final String ELEMENT_IMPL   = "implementation";
        
        /** Name of the element defining a handler properties */
        public static final String ELEMENT_PROPERTIES_NAME = "properties";
    
    
            /** Name of the element defining a handler property */
            public static final String ELEMENT_PROPERTY_NAME = "property";
            
                /** Name of the attribute defining a handler property name */
                public static final String ATTR_KEYWORD = "name";
                
                /** Name of the attribute defining a handler property value */
                public static final String ATTR_VALUE = "value";
    
    
    public FileEventHandlerInfoLoader(URL descriptorUrl)
    {   
        this.descriptorUrl = descriptorUrl;

        init();
    }
    
    protected void init()
    {
        this.info =  new FileEventHandlerInfo();
        readDescriptor();
    }
    
    protected void readDescriptor()
    {
        Element root = getDescriptorRoot();
        parsePlugin(root);        
    }
    
    protected void parsePlugin(Element node)
    {
        NodeList children = node.getChildNodes();
        //List children = node.elements();
        int numChildren = children.getLength();
        
        for (int i = 0; i < numChildren; ++i)
        {
            Node chNode = children.item(i);
            
            if (!(chNode instanceof Element))
                continue;
            
            Element child = (Element) chNode;
            String elementName = child.getNodeName();            

            
            if (compareElementName(child, FEI_NAMESPACE, ELEMENT_NAME))
            {
                String value = child.getTextContent().trim();
                this.info.setName(value);
                //this.info.setName(child.getTextTrim());
            }
            else if (compareElementName(child, FEI_NAMESPACE, ELEMENT_ID))
            {
                String value = child.getTextContent().trim();
                this.info.setId(value);
                //this.info.setId(child.getTextTrim());
            }
            else if (compareElementName(child, FEI_NAMESPACE, ELEMENT_VERSION))
            {
                String value = child.getTextContent().trim();
                this.info.setVersion(value);
                //this.info.setVersion(child.getTextTrim());   
            } 
            else if (compareElementName(child, FEI_NAMESPACE, ELEMENT_DESC))
            {
                String value = child.getTextContent().trim();
                this.info.setDescription(value);
                //this.info.setDescription(child.getTextTrim());  
            } 
            else if (compareElementName(child, FEI_NAMESPACE, ELEMENT_ORG))
            {
                String value = child.getTextContent().trim();
                this.info.setOrganization(value);
                //this.info.setOrganization(child.getTextTrim());
            }
            else if (compareElementName(child, FEI_NAMESPACE, ELEMENT_HANDLER))
            {
                parseHandler(child);
            }
        }

    }
    
    protected void parseHandler(Element node)
    {
        //List children = node.elements();
        NodeList children = node.getChildNodes();
        int numChildren = children.getLength();
        
        for (int i = 0; i < numChildren; ++i)
        {
            Node chNode = children.item(i);
            if (!(chNode instanceof Element))
                continue;
            
            Element child = (Element) chNode;
            String elementName = child.getNodeName();  
            
            if (compareElementName(child, FEI_NAMESPACE, ELEMENT_IMPL))
            {
                String value = child.getTextContent().trim();
                info.setImplementation(value);
                //info.setImplementation(child.getTextTrim());   
            }
            else if (compareElementName(child, FEI_NAMESPACE, ELEMENT_PROPERTIES_NAME))
            {
                parseProperties(child);
            }
        }
    }
    
    protected void parseProperties(Element node)
    {
        //List children = node.elements();
        NodeList children = node.getChildNodes();
        
        int numChildren = children.getLength();
        
        for (int i = 0; i < numChildren; ++i)
        {
            Node chNode = children.item(i);
            
            if (!(chNode instanceof Element))
                continue;
            
            Element child = (Element) chNode;
            String elementName = child.getNodeName();
            
            if (compareElementName(child, FEI_NAMESPACE, ELEMENT_PROPERTY_NAME))
            {
                String k = child.getAttribute(ATTR_KEYWORD);
                String v = child.getAttribute(ATTR_VALUE);
                
                if (k != null && v != null)
                    info.setProperty(k, v);                
            }            
        }
    }
    
    public FileEventHandlerInfo getInfo()
    {
        return this.info;
    }
                
//    protected Element getDescriptorRoot()
//    {
//        Element root = null;
//
//        try {
//            InputStream is = descriptorUrl.openStream();
//            
//            SAXReader reader = new SAXReader();
//            Document doc = reader.read(is);
//            root = doc.getRootElement();
//            
//        } catch (Exception ex) {
//            root = null;
//        }
//        
//        return root;
//    }           
    
    
    /**
     * This is a hacky way of checking if the element name matches, and
     * considers the case that the prefix might or might not be part
     * of the fullname.  
     * @param element Element to check
     * @param prefix Prefix (from namespace)
     * @param local Actual node name
     * @return True if name matches, with some concept of prefix
     */
    
    protected boolean compareElementName(Element element, String prefix, String local)
    {

        if (element == null || local == null)
            return false;
        
        String elementName = element.getNodeName();
        
        if (elementName == null)
            return false;
        
        if (prefix != null)
        {
            if (elementName.equals(prefix+":"+local))
                return true;
            else
            {
                String elementPrefix = element.getPrefix();
                if (prefix.equals(elementPrefix) && elementName.equals(local))
                    return true;
            }
        }
        else
        {
            if (elementName.equals(local))
                return true;
            else
            {
                int indexOf = elementName.lastIndexOf(":");
                if (indexOf != -1 && indexOf != (elementName.length() - 1))
                {
                    String subname = elementName.substring(indexOf+1);
                    if (local.equals(subname))
                        return true;
                }   
            }
        }
        
        return false;
    }
    
    protected Element getDescriptorRoot()
    {
        Element root = null;

        try {
            //InputStream is = descriptorUrl.openStream();
            InputStream is = UrlInputStreamLoader.open(descriptorUrl);
  
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            
            Document document = null;
            try {
                document = builder.parse(is);
            } catch (Exception ex) {
                throw ex;
            }
            
            root = document.getDocumentElement();           
            
        } catch (Exception ex) {
            //ex.printStackTrace();
            root = null;
        }
        
        return root;
    }  
    
}
