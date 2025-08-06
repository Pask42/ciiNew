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

    public InvoiceWriter() {
        namespaces.put("rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:16B");
        namespaces.put("ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:16B");
        namespaces.put("udt", "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16B");
    }

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
            
            // Create root element for D16B namespace
            Element root = createElement(doc, "rsm:CrossIndustryInvoice");
            root.setAttribute("xmlns:rsm",
                    "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:16B");
            root.setAttribute("xmlns:ram",
                    "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:16B");
            root.setAttribute("xmlns:udt",
                    "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16B");
            doc.appendChild(root);

            // Add ExchangedDocumentContext
            Element docContext = createElement(doc, "rsm:ExchangedDocumentContext");
            Element guideline = createElement(doc, "ram:GuidelineSpecifiedDocumentContextParameter");
            addElement(doc, guideline, "ram:ID", "urn:cen.eu:en16931:2017");
            docContext.appendChild(guideline);
            root.appendChild(docContext);

            // Add ExchangedDocument
            Element exchangedDoc = createElement(doc, "rsm:ExchangedDocument");
            root.appendChild(exchangedDoc);
            
            addElement(doc, exchangedDoc, "ram:ID", message.getMessageId());
            addElement(doc, exchangedDoc, "ram:TypeCode", "380"); // Invoice type code
            
            Element issueDateTime = createElement(doc, "ram:IssueDateTime");
            Element dateTimeString = createElement(doc, "udt:DateTimeString");
            dateTimeString.setAttribute("format", "102");
            dateTimeString.setTextContent(message.getCreationDateTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            issueDateTime.appendChild(dateTimeString);
            exchangedDoc.appendChild(issueDateTime);
            
            // Add SupplyChainTradeTransaction
            Element transaction = createElement(doc, "rsm:SupplyChainTradeTransaction");
            root.appendChild(transaction);
            
            // Add line items
            if (message.getLineItems() != null) {
                for (LineItem lineItem : message.getLineItems()) {
                    Element lineElement = createLineItemElement(doc, lineItem);
                    transaction.appendChild(lineElement);
                }
            }
            
            // Add header trade agreement
            Element headerAgreement = createElement(doc, "ram:ApplicableHeaderTradeAgreement");
            transaction.appendChild(headerAgreement);

            DocumentHeader header = message.getHeader() != null ? message.getHeader() : DocumentHeader.builder().build();
            addElement(doc, headerAgreement, "ram:BuyerReference", header.getBuyerReference());
            
            // Add seller party
            Element sellerParty = createElement(doc, "ram:SellerTradeParty");
            addElement(doc, sellerParty, "ram:ID", message.getSenderPartyId());
            addElement(doc, sellerParty, "ram:Name", "Seller Company");
            headerAgreement.appendChild(sellerParty);
            
            // Add buyer party
            Element buyerParty = createElement(doc, "ram:BuyerTradeParty");
            addElement(doc, buyerParty, "ram:ID", message.getReceiverPartyId());
            addElement(doc, buyerParty, "ram:Name", "Buyer Company");
            headerAgreement.appendChild(buyerParty);
            
            // Add header trade settlement
            Element headerSettlement = createElement(doc, "ram:ApplicableHeaderTradeSettlement");
            transaction.appendChild(headerSettlement);

            addElement(doc, headerSettlement, "ram:InvoiceCurrencyCode", header.getCurrency());

            if (header.getPaymentTerms() != null) {
                PaymentTerms terms = header.getPaymentTerms();
                Element paymentTerms = createElement(doc, "ram:SpecifiedTradeSettlementPaymentTerms");
                addElement(doc, paymentTerms, "ram:Description", terms.getDescription());
                if (terms.getDueDate() != null) {
                    Element dueDate = createElement(doc, "ram:DueDateDateTime");
                    Element dateTime = createElement(doc, "udt:DateTimeString");
                    dateTime.setAttribute("format", "102");
                    dateTime.setTextContent(terms.getDueDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                    dueDate.appendChild(dateTime);
                    paymentTerms.appendChild(dueDate);
                }
                headerSettlement.appendChild(paymentTerms);
            }

            // Add monetary summation
            if (message.getTotals() != null) {
                Element monetarySummation = createElement(doc, "ram:SpecifiedTradeSettlementHeaderMonetarySummation");
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
        Element lineElement = createElement(doc, "ram:IncludedSupplyChainTradeLineItem");
        
        // Line document
        Element lineDoc = createElement(doc, "ram:AssociatedDocumentLineDocument");
        addElement(doc, lineDoc, "ram:LineID", lineItem.getLineNumber());
        lineElement.appendChild(lineDoc);
        
        // Product
        Element product = createElement(doc, "ram:SpecifiedTradeProduct");
        Element globalId = createElement(doc, "ram:GlobalID");
        globalId.setAttribute("schemeID", "GTIN");
        globalId.setTextContent(lineItem.getProductId());
        product.appendChild(globalId);
        addElement(doc, product, "ram:Name", lineItem.getDescription());
        lineElement.appendChild(product);
        
        // Line agreement
        Element lineAgreement = createElement(doc, "ram:SpecifiedLineTradeAgreement");
        Element priceDetails = createElement(doc, "ram:NetPriceProductTradePrice");
        addAmountElement(doc, priceDetails, "ram:ChargeAmount", lineItem.getUnitPrice());
        lineAgreement.appendChild(priceDetails);
        lineElement.appendChild(lineAgreement);
        
        // Line delivery
        Element lineDelivery = createElement(doc, "ram:SpecifiedLineTradeDelivery");
        Element quantity = createElement(doc, "ram:BilledQuantity");
        quantity.setAttribute("unitCode", lineItem.getUnitCode());
        quantity.setTextContent(lineItem.getQuantity().toString());
        lineDelivery.appendChild(quantity);
        lineElement.appendChild(lineDelivery);
        
        // Line settlement
        Element lineSettlement = createElement(doc, "ram:SpecifiedLineTradeSettlement");
        if (lineItem.getTaxRate() != null || lineItem.getTaxCategory() != null || lineItem.getTaxTypeCode() != null) {
            Element tradeTax = createElement(doc, "ram:ApplicableTradeTax");
            addElement(doc, tradeTax, "ram:TypeCode", lineItem.getTaxTypeCode());
            addAmountElement(doc, tradeTax, "ram:RateApplicablePercent", lineItem.getTaxRate());
            addElement(doc, tradeTax, "ram:CategoryCode", lineItem.getTaxCategory());
            lineSettlement.appendChild(tradeTax);
        }
        Element lineMonetarySummation = createElement(doc, "ram:SpecifiedTradeSettlementLineMonetarySummation");
        addAmountElement(doc, lineMonetarySummation, "ram:LineTotalAmount", lineItem.getLineAmount());
        lineSettlement.appendChild(lineMonetarySummation);
        lineElement.appendChild(lineSettlement);
        
        return lineElement;
    }
    
    @Override
    protected void addElement(Document doc, Element parent, String name, String value) {
        if (value != null && !value.isBlank()) {
            super.addElement(doc, parent, name, value);
        }
    }

    @Override
    protected void addAmountElement(Document doc, Element parent, String name, BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
            super.addAmountElement(doc, parent, name, amount);
        }
    }
}
