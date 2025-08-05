#!/bin/bash

# Script pour corriger les writers dans le projet CII

echo "🔧 Correction des writers CII..."

# Ajouter la dépendance Lombok au module cii-writer si elle n'y est pas
if ! grep -q "lombok" cii-writer/pom.xml; then
    echo "📦 Ajout de la dépendance Lombok au module cii-writer..."
    # Remplacer le pom.xml de cii-writer
    cat > cii-writer/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.cii.messaging</groupId>
        <artifactId>cii-messaging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>cii-writer</artifactId>
    <packaging>jar</packaging>

    <name>CII Writer</name>
    <description>Java to XML generation for CII messages</description>

    <dependencies>
        <dependency>
            <groupId>com.cii.messaging</groupId>
            <artifactId>cii-model</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>jakarta.xml.bind</groupId>
            <artifactId>jakarta.xml.bind-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.sun.xml.bind</groupId>
            <artifactId>jaxb-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mustangproject</groupId>
            <artifactId>library</artifactId>
        </dependency>
        <dependency>
            <groupId>com.helger.cii</groupId>
            <artifactId>ph-cii-d16b</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF
fi

# Corriger InvoiceWriter.java
cat > cii-writer/src/main/java/com/cii/messaging/writer/impl/InvoiceWriter.java << 'EOF'
package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.writer.CIIWriterException;
import jakarta.xml.bind.JAXBContext;
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
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class InvoiceWriter extends AbstractCIIWriter {
    
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
            throw new CIIWriterException("Failed to write invoice", e);
        }
    }
    
    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            // Create root element
            Element root = doc.createElementNS("urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100", "rsm:CrossIndustryInvoice");
            root.setAttribute("xmlns:rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100");
            root.setAttribute("xmlns:ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100");
            root.setAttribute("xmlns:udt", "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100");
            doc.appendChild(root);
            
            // Add ExchangedDocument
            Element exchangedDoc = doc.createElement("rsm:ExchangedDocument");
            root.appendChild(exchangedDoc);
            
            addElement(doc, exchangedDoc, "ram:ID", message.getMessageId());
            addElement(doc, exchangedDoc, "ram:TypeCode", "380"); // Invoice type code
            
            Element issueDateTime = doc.createElement("ram:IssueDateTime");
            Element dateTimeString = doc.createElement("udt:DateTimeString");
            dateTimeString.setAttribute("format", "102");
            dateTimeString.setTextContent(message.getCreationDateTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            issueDateTime.appendChild(dateTimeString);
            exchangedDoc.appendChild(issueDateTime);
            
            // Add SupplyChainTradeTransaction
            Element transaction = doc.createElement("rsm:SupplyChainTradeTransaction");
            root.appendChild(transaction);
            
            // Add line items
            if (message.getLineItems() != null) {
                for (LineItem lineItem : message.getLineItems()) {
                    Element lineElement = createLineItemElement(doc, lineItem);
                    transaction.appendChild(lineElement);
                }
            }
            
            // Add header trade agreement
            Element headerAgreement = doc.createElement("ram:ApplicableHeaderTradeAgreement");
            transaction.appendChild(headerAgreement);
            
            if (message.getHeader() != null && message.getHeader().getBuyerReference() != null) {
                addElement(doc, headerAgreement, "ram:BuyerReference", message.getHeader().getBuyerReference());
            }
            
            // Add seller party
            Element sellerParty = doc.createElement("ram:SellerTradeParty");
            addElement(doc, sellerParty, "ram:ID", message.getSenderPartyId());
            addElement(doc, sellerParty, "ram:Name", "Seller Company");
            headerAgreement.appendChild(sellerParty);
            
            // Add buyer party
            Element buyerParty = doc.createElement("ram:BuyerTradeParty");
            addElement(doc, buyerParty, "ram:ID", message.getReceiverPartyId());
            addElement(doc, buyerParty, "ram:Name", "Buyer Company");
            headerAgreement.appendChild(buyerParty);
            
            // Add header trade settlement
            Element headerSettlement = doc.createElement("ram:ApplicableHeaderTradeSettlement");
            transaction.appendChild(headerSettlement);
            
            if (message.getHeader() != null && message.getHeader().getCurrency() != null) {
                addElement(doc, headerSettlement, "ram:InvoiceCurrencyCode", message.getHeader().getCurrency());
            }
            
            // Add monetary summation
            if (message.getTotals() != null) {
                Element monetarySummation = doc.createElement("ram:SpecifiedTradeSettlementHeaderMonetarySummation");
                headerSettlement.appendChild(monetarySummation);
                
                addAmountElement(doc, monetarySummation, "ram:LineTotalAmount", message.getTotals().getLineTotalAmount());
                addAmountElement(doc, monetarySummation, "ram:TaxBasisTotalAmount", message.getTotals().getTaxBasisAmount());
                addAmountElement(doc, monetarySummation, "ram:TaxTotalAmount", message.getTotals().getTaxTotalAmount());
                addAmountElement(doc, monetarySummation, "ram:GrandTotalAmount", message.getTotals().getGrandTotalAmount());
                addAmountElement(doc, monetarySummation, "ram:DuePayableAmount", message.getTotals().getDuePayableAmount());
            }
            
            return doc;
            
        } catch (Exception e) {
            throw new CIIWriterException("Failed to create invoice document", e);
        }
    }
    
    private Element createLineItemElement(Document doc, LineItem lineItem) {
        Element lineElement = doc.createElement("ram:IncludedSupplyChainTradeLineItem");
        
        // Line document
        Element lineDoc = doc.createElement("ram:AssociatedDocumentLineDocument");
        addElement(doc, lineDoc, "ram:LineID", lineItem.getLineNumber());
        lineElement.appendChild(lineDoc);
        
        // Product
        Element product = doc.createElement("ram:SpecifiedTradeProduct");
        Element globalId = doc.createElement("ram:GlobalID");
        globalId.setAttribute("schemeID", "GTIN");
        globalId.setTextContent(lineItem.getProductId());
        product.appendChild(globalId);
        addElement(doc, product, "ram:Name", lineItem.getDescription());
        lineElement.appendChild(product);
        
        // Line agreement
        Element lineAgreement = doc.createElement("ram:SpecifiedLineTradeAgreement");
        Element priceDetails = doc.createElement("ram:NetPriceProductTradePrice");
        addAmountElement(doc, priceDetails, "ram:ChargeAmount", lineItem.getUnitPrice());
        lineAgreement.appendChild(priceDetails);
        lineElement.appendChild(lineAgreement);
        
        // Line delivery
        Element lineDelivery = doc.createElement("ram:SpecifiedLineTradeDelivery");
        Element quantity = doc.createElement("ram:BilledQuantity");
        quantity.setAttribute("unitCode", lineItem.getUnitCode());
        quantity.setTextContent(lineItem.getQuantity().toString());
        lineDelivery.appendChild(quantity);
        lineElement.appendChild(lineDelivery);
        
        // Line settlement
        Element lineSettlement = doc.createElement("ram:SpecifiedLineTradeSettlement");
        Element lineMonetarySummation = doc.createElement("ram:SpecifiedTradeSettlementLineMonetarySummation");
        addAmountElement(doc, lineMonetarySummation, "ram:LineTotalAmount", lineItem.getLineAmount());
        lineSettlement.appendChild(lineMonetarySummation);
        lineElement.appendChild(lineSettlement);
        
        return lineElement;
    }
    
    private void addElement(Document doc, Element parent, String name, String value) {
        if (value != null) {
            Element element = doc.createElement(name);
            element.setTextContent(value);
            parent.appendChild(element);
        }
    }
    
    private void addAmountElement(Document doc, Element parent, String name, BigDecimal amount) {
        if (amount != null) {
            Element element = doc.createElement(name);
            element.setTextContent(amount.toPlainString());
            parent.appendChild(element);
        }
    }
}
EOF

# Corriger DesadvWriter.java
cat > cii-writer/src/main/java/com/cii/messaging/writer/impl/DesadvWriter.java << 'EOF'
package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.writer.CIIWriterException;
import jakarta.xml.bind.JAXBContext;
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
import java.time.LocalDateTime;
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
            throw new CIIWriterException("Failed to write DESADV", e);
        }
    }
    
    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            // Create root element
            Element root = doc.createElementNS("urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:16", "rsm:CrossIndustryDespatchAdvice");
            root.setAttribute("xmlns:rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:16");
            root.setAttribute("xmlns:ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:16");
            root.setAttribute("xmlns:udt", "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16");
            doc.appendChild(root);
            
            // Add ExchangedDocument
            Element exchangedDoc = doc.createElement("rsm:ExchangedDocument");
            root.appendChild(exchangedDoc);
            
            addElement(doc, exchangedDoc, "ram:ID", message.getMessageId());
            addElement(doc, exchangedDoc, "ram:TypeCode", "351"); // Despatch advice type code
            
            Element issueDateTime = doc.createElement("ram:IssueDateTime");
            Element dateTimeString = doc.createElement("udt:DateTimeString");
            dateTimeString.setAttribute("format", "102");
            dateTimeString.setTextContent(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            issueDateTime.appendChild(dateTimeString);
            exchangedDoc.appendChild(issueDateTime);
            
            // Add SupplyChainTradeTransaction
            Element transaction = doc.createElement("rsm:SupplyChainTradeTransaction");
            root.appendChild(transaction);
            
            // Add header trade delivery
            Element headerDelivery = doc.createElement("ram:ApplicableHeaderTradeDelivery");
            transaction.appendChild(headerDelivery);
            
            // Ship from party
            Element shipFrom = doc.createElement("ram:ShipFromTradeParty");
            addElement(doc, shipFrom, "ram:ID", message.getSenderPartyId());
            headerDelivery.appendChild(shipFrom);
            
            // Ship to party
            Element shipTo = doc.createElement("ram:ShipToTradeParty");
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
            throw new CIIWriterException("Failed to create DESADV document", e);
        }
    }
    
    private Element createTradeLineItem(Document doc, LineItem lineItem) {
        Element tradeLine = doc.createElement("ram:IncludedSupplyChainTradeLineItem");
        
        // Line document
        Element lineDoc = doc.createElement("ram:AssociatedDocumentLineDocument");
        addElement(doc, lineDoc, "ram:LineID", lineItem.getLineNumber());
        tradeLine.appendChild(lineDoc);
        
        // Product
        Element product = doc.createElement("ram:SpecifiedTradeProduct");
        Element productId = doc.createElement("ram:GlobalID");
        productId.setTextContent(lineItem.getProductId());
        product.appendChild(productId);
        addElement(doc, product, "ram:Name", lineItem.getDescription());
        tradeLine.appendChild(product);
        
        // Line delivery
        Element lineDelivery = doc.createElement("ram:SpecifiedLineTradeDelivery");
        Element quantity = doc.createElement("ram:DespatchedQuantity");
        quantity.setAttribute("unitCode", lineItem.getUnitCode());
        quantity.setTextContent(lineItem.getQuantity().toString());
        lineDelivery.appendChild(quantity);
        tradeLine.appendChild(lineDelivery);
        
        return tradeLine;
    }
    
    private void addElement(Document doc, Element parent, String name, String value) {
        if (value != null) {
            Element element = doc.createElement(name);
            element.setTextContent(value);
            parent.appendChild(element);
        }
    }
}
EOF

# Corriger OrderResponseWriter.java
cat > cii-writer/src/main/java/com/cii/messaging/writer/impl/OrderResponseWriter.java << 'EOF'
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
EOF

echo "✅ Writers corrigés avec succès!"