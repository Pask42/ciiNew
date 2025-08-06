package com.cii.messaging.reader;

import com.cii.messaging.model.MessageType;
import com.cii.messaging.reader.impl.DesadvReader;
import com.cii.messaging.reader.impl.InvoiceReader;
import com.cii.messaging.reader.impl.OrderResponseReader;
import java.io.InputStream;
import java.io.StringReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class CIIReaderFactory {

    public static CIIReader createReader(MessageType messageType) {
        switch (messageType) {
            case ORDER:
                return new com.cii.messaging.reader.impl.OrderReader();
            case INVOICE:
                return new InvoiceReader();
            case DESADV:
                return new DesadvReader();
            case ORDERSP:
                return new OrderResponseReader();
            default:
                throw new IllegalArgumentException("Unsupported message type: " + messageType);
        }
    }

    public static CIIReader createReader(String xmlContent) throws CIIReaderException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        XMLStreamReader reader = null;
        try {
            reader = factory.createXMLStreamReader(new StringReader(xmlContent));
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    switch (localName) {
                        case "CrossIndustryOrder":
                            return new com.cii.messaging.reader.impl.OrderReader();
                        case "CrossIndustryInvoice":
                            return new InvoiceReader();
                        case "CrossIndustryDespatchAdvice":
                            return new DesadvReader();
                        case "CrossIndustryOrderResponse":
                            return new OrderResponseReader();
                        default:
                            throw new CIIReaderException("Unsupported message type: " + localName);
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new CIIReaderException("Invalid XML content", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
        }
        throw new CIIReaderException("Unable to detect message type from XML content");
    }

    public static CIIReader createReader(Path xmlFile) throws CIIReaderException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        XMLStreamReader reader = null;
        try (InputStream inputStream = Files.newInputStream(xmlFile)) {
            reader = factory.createXMLStreamReader(inputStream);
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    switch (localName) {
                        case "CrossIndustryOrder":
                            return new com.cii.messaging.reader.impl.OrderReader();
                        case "CrossIndustryInvoice":
                            return new InvoiceReader();
                        case "CrossIndustryDespatchAdvice":
                            return new DesadvReader();
                        case "CrossIndustryOrderResponse":
                            return new OrderResponseReader();
                        default:
                            throw new CIIReaderException("Unsupported message type: " + localName);
                    }
                }
            }
        } catch (IOException | XMLStreamException e) {
            throw new CIIReaderException("Invalid XML file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
        }
        throw new CIIReaderException("Unable to detect message type from XML file");
    }
}
