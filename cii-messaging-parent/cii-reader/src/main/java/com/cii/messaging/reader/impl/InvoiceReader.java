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
import java.time.format.DateTimeFormatter;
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
        TradeParty seller = extractTradeParty(doc, "SellerTradeParty");
        TradeParty buyer = extractTradeParty(doc, "BuyerTradeParty");
        return CIIMessage.builder()
                .messageId(extractDocumentId(doc))
                .messageType(MessageType.INVOICE)
                .creationDateTime(LocalDateTime.now())
                .senderPartyId(seller != null ? seller.getId() : null)
                .receiverPartyId(buyer != null ? buyer.getId() : null)
                .seller(seller)
                .buyer(buyer)
                .header(extractInvoiceHeader(doc))
                .lineItems(extractInvoiceLineItems(doc))
                .totals(extractInvoiceTotals(doc))
                .build();
    }

    private TradeParty extractTradeParty(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() > 0) {
            Element party = (Element) nodes.item(0);
            return TradeParty.builder()
                    .id(extractTextContent(party, "ID"))
                    .name(extractTextContent(party, "Name"))
                    .contact(extractContact(party))
                    .address(extractAddress(party))
                    .taxRegistration(extractTaxRegistration(party))
                    .build();
        }
        return null;
    }

    private Contact extractContact(Element party) {
        NodeList contactNodes = party.getElementsByTagNameNS("*", "DefinedTradeContact");
        if (contactNodes.getLength() > 0) {
            Element contact = (Element) contactNodes.item(0);
            return Contact.builder()
                    .name(extractTextContent(contact, "PersonName"))
                    .telephone(extractTextContent(contact, "CompleteNumber"))
                    .email(extractTextContent(contact, "URIID"))
                    .build();
        }
        return null;
    }

    private Address extractAddress(Element party) {
        NodeList addressNodes = party.getElementsByTagNameNS("*", "PostalTradeAddress");
        if (addressNodes.getLength() > 0) {
            Element addr = (Element) addressNodes.item(0);
            return Address.builder()
                    .street(extractTextContent(addr, "LineOne"))
                    .city(extractTextContent(addr, "CityName"))
                    .postalCode(extractTextContent(addr, "PostcodeCode"))
                    .countryCode(extractTextContent(addr, "CountryID"))
                    .build();
        }
        return null;
    }

    private TaxRegistration extractTaxRegistration(Element party) {
        NodeList taxNodes = party.getElementsByTagNameNS("*", "SpecifiedTaxRegistration");
        if (taxNodes.getLength() > 0) {
            Element reg = (Element) taxNodes.item(0);
            NodeList idNodes = reg.getElementsByTagNameNS("*", "ID");
            if (idNodes.getLength() > 0) {
                Element idElem = (Element) idNodes.item(0);
                String scheme = idElem.getAttribute("schemeID");
                String id = idElem.getTextContent().trim();
                return TaxRegistration.builder().schemeId(scheme).id(id).build();
            }
        }
        return null;
    }
    
    private DocumentHeader extractInvoiceHeader(Document doc) {
        return DocumentHeader.builder()
                .documentNumber(extractDocumentId(doc))
                .buyerReference(extractTextContent(doc, "BuyerReference"))
                .documentDate(extractIssueDate(doc))
                .currency(extractTextContent(doc, "InvoiceCurrencyCode"))
                .paymentTerms(extractPaymentTerms(doc))
                .delivery(extractDeliveryInformation(doc))
                .build();
    }

    private String extractDocumentId(Document doc) {
        NodeList exchangedDocs = doc.getElementsByTagNameNS("*", "ExchangedDocument");
        if (exchangedDocs.getLength() > 0) {
            Element exchangedDoc = (Element) exchangedDocs.item(0);
            NodeList idNodes = exchangedDoc.getElementsByTagNameNS("*", "ID");
            if (idNodes.getLength() > 0) {
                return idNodes.item(0).getTextContent().trim();
            }
        }
        return null;
    }

    private LocalDate extractIssueDate(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS("*", "IssueDateTime");
        if (nodes.getLength() > 0) {
            String text = extractTextContent((Element) nodes.item(0), "DateTimeString");
            if (text != null && text.length() >= 8) {
                return LocalDate.parse(text.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
        }
        return LocalDate.now();
    }

    private PaymentTerms extractPaymentTerms(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS("*", "SpecifiedTradeSettlementPaymentTerms");
        if (nodes.getLength() > 0) {
            Element terms = (Element) nodes.item(0);
            String description = extractTextContent(terms, "Description");
            String dateStr = extractTextContent(terms, "DateTimeString");
            LocalDate dueDate = null;
            if (dateStr != null && dateStr.length() >= 8) {
                dueDate = LocalDate.parse(dateStr.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            return PaymentTerms.builder()
                    .description(description)
                    .dueDate(dueDate)
                    .build();
        }
        return null;
    }

    private DeliveryInformation extractDeliveryInformation(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS("*", "ApplicableHeaderTradeDelivery");
        if (nodes.getLength() > 0) {
            Element delivery = (Element) nodes.item(0);
            String dateStr = extractTextContent(delivery, "DateTimeString");
            LocalDate deliveryDate = null;
            if (dateStr != null && dateStr.length() >= 8) {
                deliveryDate = LocalDate.parse(dateStr.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            return DeliveryInformation.builder()
                    .deliveryDate(deliveryDate)
                    .build();
        }
        return null;
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
                .taxCategory(extractTextContent(lineElement, "CategoryCode"))
                .taxRate(parseBigDecimal(extractTextContent(lineElement, "RateApplicablePercent")))
                .taxTypeCode(extractTextContent(lineElement, "TypeCode"))
                .build();
    }
    
    private TotalsInformation extractInvoiceTotals(Document doc) {
        return TotalsInformation.builder()
                .lineTotalAmount(parseBigDecimal(extractTextContent(doc, "LineTotalAmount")))
                .taxBasisAmount(parseBigDecimal(extractTextContent(doc, "TaxBasisTotalAmount")))
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
