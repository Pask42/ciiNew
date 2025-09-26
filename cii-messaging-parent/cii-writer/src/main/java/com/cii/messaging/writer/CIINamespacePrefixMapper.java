package com.cii.messaging.writer;

import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

import java.util.Map;

/**
 * Provides the standard CII namespace prefixes expected by trading partners.
 */
public class CIINamespacePrefixMapper extends NamespacePrefixMapper {

    private static final Map<String, String> PREFIXES = Map.ofEntries(
            Map.entry("urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100", "ram"),
            Map.entry("urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100", "udt"),
            Map.entry("urn:un:unece:uncefact:data:standard:QualifiedDataType:100", "qdt"),
            Map.entry("urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100", "rsm"),
            Map.entry("urn:un:unece:uncefact:data:standard:CrossIndustryOrder:100", "rsm"),
            Map.entry("urn:un:unece:uncefact:data:standard:CrossIndustryOrderResponse:100", "rsm"),
            Map.entry("urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:100", "rsm")
    );

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        return PREFIXES.getOrDefault(namespaceUri, suggestion);
    }
}
