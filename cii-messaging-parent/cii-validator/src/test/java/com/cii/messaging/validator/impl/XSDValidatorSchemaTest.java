package com.cii.messaging.validator.impl;

import com.cii.messaging.model.MessageType;
import com.cii.messaging.validator.SchemaVersion;
import com.cii.messaging.validator.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class XSDValidatorSchemaTest {

    static Stream<Arguments> provideVersionsAndTypes() {
        return Stream.of(SchemaVersion.D16B, SchemaVersion.D23B)
                .flatMap(v -> Stream.of(MessageType.values()).map(t -> Arguments.of(v, t)));
    }

    @ParameterizedTest
    @MethodSource("provideVersionsAndTypes")
    void validatorLoadsOfficialSchemas(SchemaVersion version, MessageType type) {
        XSDValidator validator = new XSDValidator();
        validator.setSchemaVersion(version);

        String xml = buildMinimalXml(version, type);
        ValidationResult result = validator.validate(xml);

        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertEquals("XSD " + version.getVersion(), result.getValidatedAgainst());
    }

    private String buildMinimalXml(SchemaVersion version, MessageType type) {
        String root = rootName(type);
        String rsmNs = rsmNamespace(version, type);
        String ramNs = version == SchemaVersion.D23B
                ? "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:34"
                : "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:20";
        String udtNs = version == SchemaVersion.D23B
                ? "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:34"
                : "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:20";
        String qdtNs = version == SchemaVersion.D23B
                ? "urn:un:unece:uncefact:data:standard:QualifiedDataType:34"
                : "urn:un:unece:uncefact:data:Standard:QualifiedDataType:20";
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

    private String rsmNamespace(SchemaVersion version, MessageType type) {
        if (version == SchemaVersion.D23B) {
            switch (type) {
                case INVOICE:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:26";
                case ORDER:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:25";
                case DESADV:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:25";
                case ORDERSP:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:25";
            }
        } else {
            switch (type) {
                case INVOICE:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:13";
                case ORDER:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryOrder:12";
                case DESADV:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:12";
                case ORDERSP:
                    return "urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:12";
            }
        }
        throw new IllegalArgumentException("Unsupported combination: " + version + " " + type);
    }
}
