package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.writer.CIIWriterException;
import jakarta.xml.bind.JAXBException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DesadvWriter extends AbstractCIIWriter {
    
    @Override
    protected void initializeJAXBContext() throws JAXBException {
        // Simple XML generation without specific JAXB context
    }
    
    @Override
    public void write(CIIMessage message, OutputStream outputStream) throws CIIWriterException {
        try {
            Document document = (Document) createDocument(message);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            if (formatOutput) {
                transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, encoding);
            
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
            
        } catch (Exception e) {
            throw new CIIWriterException("Échec de l'écriture du DESADV", e);
        }
    }
    
    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            namespaces.clear();
            namespaces.put("rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:12");
            namespaces.put("ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:20");
            namespaces.put("udt", "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:20");

            // Create root element
            Element root = createElement(doc, "rsm:CrossIndustryDespatchAdvice");
            root.setAttribute("xmlns:rsm", namespaces.get("rsm"));
            root.setAttribute("xmlns:ram", namespaces.get("ram"));
            root.setAttribute("xmlns:udt", namespaces.get("udt"));
            doc.appendChild(root);
            
            // Add ExchangedDocument
            Element exchangedDoc = createElement(doc, "rsm:ExchangedDocument");
            root.appendChild(exchangedDoc);

            addElement(doc, exchangedDoc, "ram:ID", message.getMessageId());
            addElement(doc, exchangedDoc, "ram:TypeCode", "351"); // Despatch advice type code

              Element issueDateTime = createElement(doc, "ram:IssueDateTime");
              Element dateTimeString = createElement(doc, "udt:DateTimeString");
              dateTimeString.setAttribute("format", "102");
              dateTimeString.setTextContent(OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
              issueDateTime.appendChild(dateTimeString);
            exchangedDoc.appendChild(issueDateTime);

            // Add SupplyChainTradeTransaction
            Element transaction = createElement(doc, "rsm:SupplyChainTradeTransaction");
            root.appendChild(transaction);

            // Add header trade delivery
            Element headerDelivery = createElement(doc, "ram:ApplicableHeaderTradeDelivery");
            transaction.appendChild(headerDelivery);

            // Ship from party
            Element shipFrom = createElement(doc, "ram:ShipFromTradeParty");
            addElement(doc, shipFrom, "ram:ID", message.getSenderPartyId());
            headerDelivery.appendChild(shipFrom);

            // Ship to party
            Element shipTo = createElement(doc, "ram:ShipToTradeParty");
            addElement(doc, shipTo, "ram:ID", message.getReceiverPartyId());
            headerDelivery.appendChild(shipTo);
            
            // Add line items
            if (message.getLineItems() != null) {
                for (LineItem lineItem : message.getLineItems()) {
                    Element lineElement = createTradeLineItem(doc, lineItem);
                    transaction.appendChild(lineElement);
                }
            }
            
            return doc;
            
        } catch (Exception e) {
            throw new CIIWriterException("Échec de la création du document DESADV", e);
        }
    }
    
    private Element createTradeLineItem(Document doc, LineItem lineItem) {
        Element tradeLine = createElement(doc, "ram:IncludedSupplyChainTradeLineItem");

        // Line document
        Element lineDoc = createElement(doc, "ram:AssociatedDocumentLineDocument");
        addElement(doc, lineDoc, "ram:LineID", lineItem.getLineNumber());
        tradeLine.appendChild(lineDoc);

        // Product
        Element product = createElement(doc, "ram:SpecifiedTradeProduct");
        Element productId = createElement(doc, "ram:GlobalID");
        productId.setTextContent(lineItem.getProductId());
        product.appendChild(productId);
        addElement(doc, product, "ram:Name", lineItem.getDescription());
        tradeLine.appendChild(product);

        // Line delivery
        Element lineDelivery = createElement(doc, "ram:SpecifiedLineTradeDelivery");
        Element quantity = createElement(doc, "ram:DespatchedQuantity");
        quantity.setAttribute("unitCode", lineItem.getUnitCode());
        quantity.setTextContent(lineItem.getQuantity().toString());
        lineDelivery.appendChild(quantity);
        tradeLine.appendChild(lineDelivery);

        return tradeLine;
    }
}
