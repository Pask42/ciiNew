package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.model.util.UneceSchemaLoader;
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
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class InvoiceWriter extends AbstractCIIWriter {

    public InvoiceWriter() {
        String version = UneceSchemaLoader.resolveVersion().substring(1);
        namespaces.put("rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:" + version);
        namespaces.put("ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:" + version);
        namespaces.put("udt", "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:" + version);
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
            throw new CIIWriterException("Échec de l'écriture de la facture", e);
        }
    }
    
    @Override
    protected Object createDocument(CIIMessage message) throws CIIWriterException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            // Create root element for configured namespace
            String version = UneceSchemaLoader.resolveVersion().substring(1);
            Element root = createElement(doc, "rsm:CrossIndustryInvoice");
            root.setAttribute("xmlns:rsm",
                    "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:" + version);
            root.setAttribute("xmlns:ram",
                    "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:" + version);
            root.setAttribute("xmlns:udt",
                    "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:" + version);
            doc.appendChild(root);
            DocumentHeader header = message.getHeader() != null ? message.getHeader() : DocumentHeader.builder().build();

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

            addElement(doc, exchangedDoc, "ram:ReferenceID", header.getDocumentNumber());
            
            Element issueDateTime = createElement(doc, "ram:IssueDateTime");
            Element dateTimeString = createElement(doc, "udt:DateTimeString");
            dateTimeString.setAttribute("format", "204");
            dateTimeString.setTextContent(message.getCreationDateTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            issueDateTime.appendChild(dateTimeString);
            exchangedDoc.appendChild(issueDateTime);
            
            // Add SupplyChainTradeTransaction
            Element transaction = createElement(doc, "rsm:SupplyChainTradeTransaction");
            root.appendChild(transaction);
            
            // Add line items
            if (message.getLineItems() != null) {
                for (LineItem lineItem : message.getLineItems()) {
                      Element lineElement = createLineItemElement(doc, lineItem, header.getCurrency() != null ? header.getCurrency().getCurrencyCode() : null);
                    transaction.appendChild(lineElement);
                }
            }

            // Add header trade agreement
            Element headerAgreement = createElement(doc, "ram:ApplicableHeaderTradeAgreement");
            transaction.appendChild(headerAgreement);
            addElement(doc, headerAgreement, "ram:BuyerReference", header.getBuyerReference());

            // Add seller party
            if (message.getSeller() != null) {
                headerAgreement.appendChild(createTradePartyElement(doc, "ram:SellerTradeParty", message.getSeller()));
            }

            // Add buyer party
            if (message.getBuyer() != null) {
                headerAgreement.appendChild(createTradePartyElement(doc, "ram:BuyerTradeParty", message.getBuyer()));
            }

            // Add header trade delivery
            if (header.getDelivery() != null) {
                DeliveryInformation delivery = header.getDelivery();
                Element headerDelivery = createElement(doc, "ram:ApplicableHeaderTradeDelivery");
                transaction.appendChild(headerDelivery);
                Element supplyChainEvent = createElement(doc, "ram:ActualDeliverySupplyChainEvent");
                headerDelivery.appendChild(supplyChainEvent);
                if (delivery.getDeliveryDate() != null) {
                    Element occurrenceDateTime = createElement(doc, "ram:OccurrenceDateTime");
                    Element dateTime = createElement(doc, "udt:DateTimeString");
                    dateTime.setAttribute("format", "102");
                    dateTime.setTextContent(delivery.getDeliveryDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                    occurrenceDateTime.appendChild(dateTime);
                    supplyChainEvent.appendChild(occurrenceDateTime);
                }
            }

            // Add header trade settlement
            Element headerSettlement = createElement(doc, "ram:ApplicableHeaderTradeSettlement");
            transaction.appendChild(headerSettlement);

              addElement(doc, headerSettlement, "ram:InvoiceCurrencyCode", header.getCurrency() != null ? header.getCurrency().getCurrencyCode() : null);

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

              addAmountElement(doc, monetarySummation, "ram:LineTotalAmount", message.getTotals().getLineTotalAmount(), header.getCurrency() != null ? header.getCurrency().getCurrencyCode() : null, false);
                  addAmountElement(doc, monetarySummation, "ram:TaxBasisTotalAmount", message.getTotals().getTaxBasisAmount(), header.getCurrency() != null ? header.getCurrency().getCurrencyCode() : null, false);
                  addAmountElement(doc, monetarySummation, "ram:TaxTotalAmount", message.getTotals().getTaxTotalAmount(), header.getCurrency() != null ? header.getCurrency().getCurrencyCode() : null, true);
                  addAmountElement(doc, monetarySummation, "ram:GrandTotalAmount", message.getTotals().getGrandTotalAmount(), header.getCurrency() != null ? header.getCurrency().getCurrencyCode() : null, false);
                  addAmountElement(doc, monetarySummation, "ram:DuePayableAmount", message.getTotals().getDuePayableAmount(), header.getCurrency() != null ? header.getCurrency().getCurrencyCode() : null, false);
            }
            
            return doc;
            
        } catch (Exception e) {
            throw new CIIWriterException("Échec de la création du document facture", e);
        }
    }

    private Element createTradePartyElement(Document doc, String tagName, TradeParty party) {
        Element partyElement = createElement(doc, tagName);
        addElement(doc, partyElement, "ram:ID", party.getId());
        addElement(doc, partyElement, "ram:Name", party.getName());

        if (party.getContact() != null) {
            Contact contact = party.getContact();
            Element contactEl = createElement(doc, "ram:DefinedTradeContact");
            addElement(doc, contactEl, "ram:PersonName", contact.getName());
            if (contact.getTelephone() != null) {
                Element phone = createElement(doc, "ram:TelephoneUniversalCommunication");
                addElement(doc, phone, "ram:CompleteNumber", contact.getTelephone());
                contactEl.appendChild(phone);
            }
            if (contact.getEmail() != null) {
                Element email = createElement(doc, "ram:EmailURIUniversalCommunication");
                addElement(doc, email, "ram:URIID", contact.getEmail());
                contactEl.appendChild(email);
            }
            partyElement.appendChild(contactEl);
        }

        if (party.getAddress() != null) {
            Address addr = party.getAddress();
            Element addrEl = createElement(doc, "ram:PostalTradeAddress");
            addElement(doc, addrEl, "ram:PostcodeCode", addr.getPostalCode());
            addElement(doc, addrEl, "ram:LineOne", addr.getStreet());
            addElement(doc, addrEl, "ram:CityName", addr.getCity());
            addElement(doc, addrEl, "ram:CountryID", addr.getCountryCode());
            partyElement.appendChild(addrEl);
        }

        if (party.getTaxRegistration() != null) {
            TaxRegistration reg = party.getTaxRegistration();
            Element regEl = createElement(doc, "ram:SpecifiedTaxRegistration");
            Element idEl = createElement(doc, "ram:ID");
            if (reg.getSchemeId() != null && !reg.getSchemeId().isBlank()) {
                idEl.setAttribute("schemeID", reg.getSchemeId());
            }
            idEl.setTextContent(reg.getId());
            regEl.appendChild(idEl);
            partyElement.appendChild(regEl);
        }

        return partyElement;
    }

    private Element createLineItemElement(Document doc, LineItem lineItem, String currency) {
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
        addAmountElement(doc, priceDetails, "ram:ChargeAmount", lineItem.getUnitPrice(), currency, false);
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
            addAmountElement(doc, tradeTax, "ram:RateApplicablePercent", lineItem.getTaxRate(), null, false);
            addElement(doc, tradeTax, "ram:CategoryCode", lineItem.getTaxCategory());
            lineSettlement.appendChild(tradeTax);
        }
        Element lineMonetarySummation = createElement(doc, "ram:SpecifiedTradeSettlementLineMonetarySummation");
        addAmountElement(doc, lineMonetarySummation, "ram:LineTotalAmount", lineItem.getLineAmount(), currency, false);
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
        addAmountElement(doc, parent, name, amount, null, false);
    }

    private void addAmountElement(Document doc, Element parent, String name, BigDecimal amount, String currency, boolean withCurrency) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
            Element element = createElement(doc, name);
            element.setTextContent(amount.toPlainString());
            if (withCurrency && currency != null && !currency.isBlank()) {
                element.setAttribute("currencyID", currency);
            }
            parent.appendChild(element);
        }
    }
}
