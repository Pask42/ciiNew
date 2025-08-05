package com.cii.messaging.validator;

import com.cii.messaging.model.CIIMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Performs XSD and Schematron validation on CII messages.
 */
public class MessageValidator {

    private final Schema schema;
    private final XsltExecutable schematronXslt;

    public MessageValidator(File xsdFile, File schematronXsltFile) throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.schema = schemaFactory.newSchema(xsdFile);

        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        this.schematronXslt = compiler.compile(new StreamSource(schematronXsltFile));
    }

    public void validate(String xml) throws Exception {
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));

        Processor processor = new Processor(false);
        XsltTransformer transformer = schematronXslt.load();
        transformer.setSource(new StreamSource(new StringReader(xml)));
        transformer.setDestination(new Serializer(new NullWriter()));
        transformer.transform();
    }

    public void validate(CIIMessage message) throws Exception {
        JAXBContext context = JAXBContext.newInstance(CIIMessage.class);
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(message, writer);
        validate(writer.toString());
    }

    private static class NullWriter extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) { }
        @Override
        public void flush() { }
        @Override
        public void close() { }
    }
}
