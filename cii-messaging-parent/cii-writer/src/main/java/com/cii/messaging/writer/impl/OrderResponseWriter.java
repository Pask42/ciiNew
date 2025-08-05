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
            
            // Create root element
            Element root = doc.createElementNS("urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:16", "rsm:CrossIndustryOrderResponse");
            doc.appendChild(root);
            
            // Add ExchangedDocument
            Element exchangedDoc = doc.createElement("rsm:ExchangedDocument");
            root.appendChild(exchangedDoc);
            
            Element docId = doc.createElement("ram:ID");
            docId.setTextContent(message.getMessageId());
            exchangedDoc.appendChild(docId);
            
            return doc;
            
        } catch (Exception e) {
            throw new CIIWriterException("Failed to create Order Response document", e);
        }
    }
}
