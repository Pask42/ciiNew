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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderReader extends AbstractCIIReader {
    
    @Override
    protected void initializeJAXBContext() throws JAXBException {
        // Simple XML parsing without specific JAXB context
    }
    
    @Override
    public CIIMessage read(File xmlFile) throws CIIReaderException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            return parseOrderDocument(doc);
        } catch (Exception e) {
            throw new CIIReaderException("Failed to read order file", e);
        }
    }
    
    @Override
    public CIIMessage read(InputStream inputStream) throws CIIReaderException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            return parseOrderDocument(doc);
        } catch (Exception e) {
            throw new CIIReaderException("Failed to read order from stream", e);
        }
    }
    
    @Override
    protected CIIMessage parseDocument(Object document) throws CIIReaderException {
        if (document instanceof Document) {
            return parseOrderDocument((Document) document);
        }
        throw new CIIReaderException("Invalid document type");
    }
    
    private CIIMessage parseOrderDocument(Document doc) {
        return CIIMessage.builder()
                .messageId(extractMessageId(doc))
                .messageType(MessageType.ORDER)
                .creationDateTime(LocalDateTime.now())
                .senderPartyId(extractBuyerPartyId(doc))
                .receiverPartyId(extractSellerPartyId(doc))
                .header(extractOrderHeader(doc))
                .lineItems(extractOrderLineItems(doc))
                .totals(extractOrderTotals(doc))
                .build();
    }
    
    private String extractMessageId(Document doc) {
        NodeList idNodes = doc.getElementsByTagNameNS("*", "ID");
        if (idNodes.getLength() > 0) {
            return idNodes.item(0).getTextContent().trim();
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
    
    private String extractSellerPartyId(Document doc) {
        NodeList sellerNodes = doc.getElementsByTagNameNS("*", "SellerTradeParty");
        if (sellerNodes.getLength() > 0) {
            Element seller = (Element) sellerNodes.item(0);
            return extractTextContent(seller, "ID");
        }
        return null;
    }
    
    private DocumentHeader extractOrderHeader(Document doc) {
        return DocumentHeader.builder()
                .documentNumber(extractMessageId(doc))
                .buyerReference(extractTextContent(doc, "BuyerReference"))
                .currency(extractTextContent(doc, "InvoiceCurrencyCode"))
                .build();
    }
    
    private List<LineItem> extractOrderLineItems(Document doc) {
        List<LineItem> items = new ArrayList<>();
        NodeList lineNodes = doc.getElementsByTagNameNS("*", "IncludedSupplyChainTradeLineItem");
        
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element lineElement = (Element) lineNodes.item(i);
            items.add(extractLineItem(lineElement));
        }
        
        return items;
    }
    
    private LineItem extractLineItem(Element lineElement) {
        String lineId = extractTextContent(lineElement, "LineID");
        if (lineId == null) {
            NodeList lineIdNodes = lineElement.getElementsByTagNameNS("*", "LineID");
            if (lineIdNodes.getLength() > 0) {
                lineId = lineIdNodes.item(0).getTextContent().trim();
            }
        }
        
        return LineItem.builder()
                .lineNumber(lineId != null ? lineId : "1")
                .productId(extractTextContent(lineElement, "GlobalID"))
                .description(extractTextContent(lineElement, "Name"))
                .quantity(parseBigDecimal(extractTextContent(lineElement, "RequestedQuantity")))
                .unitCode("EA")
                .unitPrice(parseBigDecimal(extractTextContent(lineElement, "ChargeAmount")))
                .lineAmount(parseBigDecimal(extractTextContent(lineElement, "LineTotalAmount")))
                .build();
    }
    
    private TotalsInformation extractOrderTotals(Document doc) {
        BigDecimal lineTotal = parseBigDecimal(extractTextContent(doc, "LineTotalAmount"));
        if (lineTotal == null || lineTotal.equals(BigDecimal.ZERO)) {
            // Calculate from line items if not present
            lineTotal = calculateLineTotalFromItems(doc);
        }
        
        return TotalsInformation.builder()
                .lineTotalAmount(lineTotal)
                .grandTotalAmount(lineTotal)
                .build();
    }
    
    private BigDecimal calculateLineTotalFromItems(Document doc) {
        BigDecimal total = BigDecimal.ZERO;
        NodeList lineAmounts = doc.getElementsByTagNameNS("*", "LineTotalAmount");
        for (int i = 0; i < lineAmounts.getLength(); i++) {
            BigDecimal amount = parseBigDecimal(lineAmounts.item(i).getTextContent());
            if (amount != null) {
                total = total.add(amount);
            }
        }
        return total;
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
