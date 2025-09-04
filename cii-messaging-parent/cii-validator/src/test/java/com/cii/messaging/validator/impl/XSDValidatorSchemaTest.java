package com.cii.messaging.validator.impl;

import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class XSDValidatorSchemaTest {

    static Stream<Arguments> provideTypes() {
        return Stream.of(MessageType.values()).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("provideTypes")
    void validatorLoadsOfficialSchemas(MessageType type) {
        XSDValidator validator = new XSDValidator();

        String xml = buildMinimalXml(type);
        ValidationResult result = validator.validate(xml);

        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals("XSD D23B", result.getValidatedAgainst());
    }

    private String buildMinimalXml(MessageType type) {
        String root = rootName(type);
        String rsmNs = rsmNamespace(type);
        String ramNs = "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:34";
        String udtNs = "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:34";
        String qdtNs = "urn:un:unece:uncefact:data:standard:QualifiedDataType:34";
        return String.format("<rsm:%s xmlns:rsm=\"%s\" xmlns:ram=\"%s\" xmlns:udt=\"%s\" xmlns:qdt=\"%s\"/>",
                root, rsmNs, ramNs, udtNs, qdtNs);
    }

    private String rootName(MessageType type) {
        switch (type) {
            case INVOICE:
                return "CrossIndustryInvoice";
            case ORDER:
                return "CrossIndustryOrder";
            case DESADV:
                return "CrossIndustryDespatchAdvice";
            case ORDERSP:
                return "CrossIndustryOrderResponse";
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private String rsmNamespace(MessageType type) {
        switch (type) {
            case INVOICE:
                return "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:26";
            case ORDER:
                return "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:25";
            case DESADV:
                return "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:25";
            case ORDERSP:
                return "urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:25";
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}

