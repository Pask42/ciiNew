package com.cii.messaging.reader.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.reader.CIIReaderException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvoiceReader extends AbstractCIIReader {
    
    @Override
    protected void initializeJAXBContext() throws JAXBException {
        // Simple XML parsing without specific JAXB context
    }
    
    @Override
    public CIIMessage read(File xmlFile) throws CIIReaderException {
        try {
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            return parseInvoiceDocument(doc);
        } catch (Exception e) {
            throw new CIIReaderException("Failed to read invoice file", e);
        }
    }
    
    @Override
    public CIIMessage read(InputStream inputStream) throws CIIReaderException {
        try {
            DocumentBuilder builder = createSecureDocumentBuilder();
            Document doc = builder.parse(inputStream);
            return parseInvoiceDocument(doc);
        } catch (Exception e) {
            throw new CIIReaderException("Failed to read invoice from stream", e);
        }
    }

    private DocumentBuilder createSecureDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    @Override
    protected CIIMessage parseDocument(Object document) throws CIIReaderException {
        if (document instanceof Document) {
            return parseInvoiceDocument((Document) document);
        }
        throw new CIIReaderException("Invalid document type");
    }
    
    private CIIMessage parseInvoiceDocument(Document doc) {
        return CIIMessage.builder()
                .messageId(extractTextContent(doc, "ID"))
                .messageType(MessageType.INVOICE)
                .creationDateTime(LocalDateTime.now())
                .senderPartyId(extractSellerPartyId(doc))
                .receiverPartyId(extractBuyerPartyId(doc))
                .header(extractInvoiceHeader(doc))
                .lineItems(extractInvoiceLineItems(doc))
                .totals(extractInvoiceTotals(doc))
                .build();
    }
    
    private String extractSellerPartyId(Document doc) {
        NodeList sellerNodes = doc.getElementsByTagNameNS("*", "SellerTradeParty");
        if (sellerNodes.getLength() > 0) {
            Element seller = (Element) sellerNodes.item(0);
            return extractTextContent(seller, "ID");
        }
        return null;
    }
    
    private String extractBuyerPartyId(Document doc) {
        NodeList buyerNodes = doc.getElementsByTagNameNS("*", "BuyerTradeParty");
        if (buyerNodes.getLength() > 0) {
            Element buyer = (Element) buyerNodes.item(0);
            return extractTextContent(buyer, "ID");
        }
        return null;
    }
    
    private DocumentHeader extractInvoiceHeader(Document doc) {
        return DocumentHeader.builder()
                .documentNumber(extractTextContent(doc, "ID"))
                .documentDate(LocalDate.now()) // Simplified
                .currency(extractTextContent(doc, "InvoiceCurrencyCode"))
                .build();
    }
    
    private List<LineItem> extractInvoiceLineItems(Document doc) {
        List<LineItem> items = new ArrayList<>();
        NodeList lineNodes = doc.getElementsByTagNameNS("*", "IncludedSupplyChainTradeLineItem");
        
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element lineElement = (Element) lineNodes.item(i);
            items.add(extractLineItem(lineElement, i + 1));
        }
        
        return items;
    }
    
    private LineItem extractLineItem(Element lineElement, int lineNumber) {
        return LineItem.builder()
                .lineNumber(String.valueOf(lineNumber))
                .productId(extractTextContent(lineElement, "GlobalID"))
                .description(extractTextContent(lineElement, "Name"))
                .quantity(parseBigDecimal(extractTextContent(lineElement, "BilledQuantity")))
                .unitCode("EA") // Default
                .unitPrice(parseBigDecimal(extractTextContent(lineElement, "ChargeAmount")))
                .lineAmount(parseBigDecimal(extractTextContent(lineElement, "LineTotalAmount")))
                .build();
    }
    
    private TotalsInformation extractInvoiceTotals(Document doc) {
        return TotalsInformation.builder()
                .lineTotalAmount(parseBigDecimal(extractTextContent(doc, "LineTotalAmount")))
                .taxTotalAmount(parseBigDecimal(extractTextContent(doc, "TaxTotalAmount")))
                .grandTotalAmount(parseBigDecimal(extractTextContent(doc, "GrandTotalAmount")))
                .duePayableAmount(parseBigDecimal(extractTextContent(doc, "DuePayableAmount")))
                .build();
    }
    
    private String extractTextContent(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
    
    private String extractTextContent(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
}
