package com.cii.messaging.writer.impl;

import com.cii.messaging.model.*;
import com.cii.messaging.writer.CIIWriter;
import com.cii.messaging.writer.CIIWriterException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCIIWriter implements CIIWriter {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractCIIWriter.class);
    protected JAXBContext jaxbContext;
    protected boolean formatOutput = true;
    protected String encoding = "UTF-8";
    protected final Map<String, String> namespaces = new HashMap<>();
    
    protected AbstractCIIWriter() {
        try {
            initializeJAXBContext();
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to initialize JAXB context", e);
        }
    }
    
    protected abstract void initializeJAXBContext() throws JAXBException;
    protected abstract Object createDocument(CIIMessage message) throws CIIWriterException;

    @Override
    public void write(CIIMessage message, File outputFile) throws CIIWriterException {
        java.io.File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (OutputStream os = new FileOutputStream(outputFile)) {
            write(message, os);
        } catch (IOException e) {
            throw new CIIWriterException("Failed to write to file: " + outputFile.getName(), e);
        }
    }
    
    @Override
    public void write(CIIMessage message, OutputStream outputStream) throws CIIWriterException {
        try {
            Object document = createDocument(message);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatOutput);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            marshaller.marshal(document, outputStream);
        } catch (JAXBException e) {
            throw new CIIWriterException("Failed to marshal CII message", e);
        }
    }
    
    @Override
    public String writeToString(CIIMessage message) throws CIIWriterException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(message, baos);
        return baos.toString(StandardCharsets.UTF_8);
    }
    
    @Override
    public void setFormatOutput(boolean format) {
        this.formatOutput = format;
    }
    
    @Override
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    protected Element createElement(Document doc, String name) {
        int idx = name.indexOf(':');
        if (idx == -1) {
            return doc.createElement(name);
        }
        String prefix = name.substring(0, idx);
        String ns = namespaces.get(prefix);
        if (ns != null) {
            return doc.createElementNS(ns, name);
        }
        return doc.createElement(name);
    }

    protected void addElement(Document doc, Element parent, String name, String value) {
        if (value != null && !value.isEmpty()) {
            Element element = createElement(doc, name);
            element.setTextContent(value);
            parent.appendChild(element);
        }
    }

    protected void addAmountElement(Document doc, Element parent, String name, BigDecimal amount) {
        if (amount != null) {
            Element element = createElement(doc, name);
            element.setTextContent(amount.toPlainString());
            parent.appendChild(element);
        }
    }
}
