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
import java.time.format.DateTimeFormatter;

public class OrderWriter extends AbstractCIIWriter {

    public OrderWriter() {
        namespaces.put("rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:12");
        namespaces.put("ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:20");
        namespaces.put("udt", "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:20");
    }

    @Override
    protected void initializeJAXBContext() throws JAXBException {
        // Simple DOM generation without specific JAXB context
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
            throw new CIIWriterException("Failed to write order", e);
        }
    }

    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Root element
            Element root = createElement(doc, "rsm:CrossIndustryOrder");
            root.setAttribute("xmlns:rsm", namespaces.get("rsm"));
            root.setAttribute("xmlns:ram", namespaces.get("ram"));
            root.setAttribute("xmlns:udt", namespaces.get("udt"));
            doc.appendChild(root);

            // ExchangedDocument
            DocumentHeader header = message.getHeader() != null ? message.getHeader() : DocumentHeader.builder().build();
            Element exchangedDoc = createElement(doc, "rsm:ExchangedDocument");
            addElement(doc, exchangedDoc, "ram:ID", message.getMessageId());
            addElement(doc, exchangedDoc, "ram:TypeCode", "220");
            addElement(doc, exchangedDoc, "ram:ReferenceID", header.getDocumentNumber());
            if (message.getCreationDateTime() != null) {
                Element issueDateTime = createElement(doc, "ram:IssueDateTime");
                Element dateTimeString = createElement(doc, "udt:DateTimeString");
                dateTimeString.setAttribute("format", "102");
                dateTimeString.setTextContent(message.getCreationDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                issueDateTime.appendChild(dateTimeString);
                exchangedDoc.appendChild(issueDateTime);
            }
            root.appendChild(exchangedDoc);

            // Transaction
            Element transaction = createElement(doc, "rsm:SupplyChainTradeTransaction");
            root.appendChild(transaction);

            // Header agreement with parties
            Element headerAgreement = createElement(doc, "ram:ApplicableHeaderTradeAgreement");
            addElement(doc, headerAgreement, "ram:BuyerReference", header.getBuyerReference());
            if (message.getSeller() != null) {
                headerAgreement.appendChild(createTradePartyElement(doc, "ram:SellerTradeParty", message.getSeller()));
            }
            if (message.getBuyer() != null) {
                headerAgreement.appendChild(createTradePartyElement(doc, "ram:BuyerTradeParty", message.getBuyer()));
            }
            transaction.appendChild(headerAgreement);

            // Line items
            if (message.getLineItems() != null) {
                for (LineItem line : message.getLineItems()) {
                    transaction.appendChild(createLineItemElement(doc, line));
                }
            }

            return doc;
        } catch (Exception e) {
            throw new CIIWriterException("Failed to create Order document", e);
        }
    }

    private Element createTradePartyElement(Document doc, String tagName, TradeParty party) {
        Element partyElement = createElement(doc, tagName);
        addElement(doc, partyElement, "ram:ID", party.getId());
        addElement(doc, partyElement, "ram:Name", party.getName());
        return partyElement;
    }

    private Element createLineItemElement(Document doc, LineItem lineItem) {
        Element line = createElement(doc, "ram:IncludedSupplyChainTradeLineItem");

        Element lineDoc = createElement(doc, "ram:AssociatedDocumentLineDocument");
        addElement(doc, lineDoc, "ram:LineID", lineItem.getLineNumber());
        line.appendChild(lineDoc);

        Element product = createElement(doc, "ram:SpecifiedTradeProduct");
        addElement(doc, product, "ram:GlobalID", lineItem.getProductId());
        addElement(doc, product, "ram:Name", lineItem.getDescription());
        line.appendChild(product);

        Element agreement = createElement(doc, "ram:SpecifiedLineTradeAgreement");
        Element price = createElement(doc, "ram:GrossPriceProductTradePrice");
        addAmountElement(doc, price, "ram:ChargeAmount", lineItem.getUnitPrice());
        agreement.appendChild(price);
        line.appendChild(agreement);

        Element delivery = createElement(doc, "ram:SpecifiedLineTradeDelivery");
        Element qty = createElement(doc, "ram:BilledQuantity");
        if (lineItem.getUnitCode() != null) {
            qty.setAttribute("unitCode", lineItem.getUnitCode());
        }
        if (lineItem.getQuantity() != null) {
            qty.setTextContent(lineItem.getQuantity().toPlainString());
        }
        delivery.appendChild(qty);
        line.appendChild(delivery);

        Element settlement = createElement(doc, "ram:SpecifiedLineTradeSettlement");
        if (lineItem.getTaxRate() != null || lineItem.getTaxCategory() != null || lineItem.getTaxTypeCode() != null) {
            Element tax = createElement(doc, "ram:ApplicableTradeTax");
            addElement(doc, tax, "ram:TypeCode", lineItem.getTaxTypeCode());
            addElement(doc, tax, "ram:CategoryCode", lineItem.getTaxCategory());
            addAmountElement(doc, tax, "ram:RateApplicablePercent", lineItem.getTaxRate());
            settlement.appendChild(tax);
        }
        Element sum = createElement(doc, "ram:SpecifiedTradeSettlementLineMonetarySummation");
        addAmountElement(doc, sum, "ram:LineTotalAmount", lineItem.getLineAmount());
        settlement.appendChild(sum);
        line.appendChild(settlement);

        return line;
    }
}
