package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.writer.CIIWriterException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class OrderResponseWriter extends AbstractCIIWriter {
    
    @Override
    protected void initializeJAXBContext() throws JAXBException {
        // Simple XML generation without specific JAXB context
    }
    
    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            namespaces.clear();
            namespaces.put("rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:16");
            namespaces.put("ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:16");
            namespaces.put("udt", "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16");

            // Create root element
            Element root = createElement(doc, "rsm:CrossIndustryOrderResponse");
            root.setAttribute("xmlns:rsm", namespaces.get("rsm"));
            root.setAttribute("xmlns:ram", namespaces.get("ram"));
            root.setAttribute("xmlns:udt", namespaces.get("udt"));
            doc.appendChild(root);

            // Add ExchangedDocument
            Element exchangedDoc = createElement(doc, "rsm:ExchangedDocument");
            root.appendChild(exchangedDoc);

            addElement(doc, exchangedDoc, "ram:ID", message.getMessageId());

            return doc;
            
        } catch (Exception e) {
            throw new CIIWriterException("Failed to create Order Response document", e);
        }
    }
}
