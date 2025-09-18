package com.cii.messaging.validator;

import com.cii.messaging.validator.impl.XSDValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class XSDValidatorMessagesTest {

    private static final SchemaVersion DEFAULT_VERSION = SchemaVersion.D23B;

    private static final String INVALID_ORDERS = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <rsm:CrossIndustryOrder xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryOrder:100\"
                                    xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\"
                                    xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\">
                <rsm:ExchangedDocumentContext/>
                <rsm:ExchangedDocument>
                    <ram:ID>ORD-ERROR</ram:ID>
                    <ram:IssueDateTime>
                        <udt:DateTimeString format=\"102\">20240130</udt:DateTimeString>
                    </ram:IssueDateTime>
                </rsm:ExchangedDocument>
            </rsm:CrossIndustryOrder>
            """;

    private static final String INVALID_DESADV = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <rsm:CrossIndustryDespatchAdvice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryDespatchAdvice:100\"
                                             xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\"
                                             xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\">
                <rsm:ExchangedDocumentContext/>
                <rsm:ExchangedDocument>
                    <ram:ID>DESADV-ERROR</ram:ID>
                    <ram:IssueDateTime>
                        <udt:DateTimeString format=\"102\">20240130</udt:DateTimeString>
                    </ram:IssueDateTime>
                </rsm:ExchangedDocument>
                <rsm:SupplyChainTradeTransaction>
                    <ram:ApplicableHeaderTradeAgreement/>
                </rsm:SupplyChainTradeTransaction>
            </rsm:CrossIndustryDespatchAdvice>
            """;

    private static final String INVALID_INVOICE = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100\"
                                      xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\"
                                      xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\">
                <rsm:ExchangedDocumentContext/>
                <rsm:ExchangedDocument>
                    <ram:ID>INV-ERROR</ram:ID>
                    <ram:IssueDateTime>
                        <udt:DateTimeString format=\"102\">20240130</udt:DateTimeString>
                    </ram:IssueDateTime>
                </rsm:ExchangedDocument>
                <rsm:SupplyChainTradeTransaction>
                    <ram:ApplicableHeaderTradeAgreement/>
                    <ram:ApplicableHeaderTradeDelivery/>
                </rsm:SupplyChainTradeTransaction>
            </rsm:CrossIndustryInvoice>
            """;

    @Nested
    @DisplayName("Validation des messages ORDERS")
    class OrdersValidation {

        @Test
        @DisplayName("valide un ORDERS conforme au XSD UNECE")
        void ordersValideRetourneMessageValide() throws IOException {
            ValidationResult result = validateResource("order-valid.xml");

            assertTrue(result.isValid(), () -> formatResult(result));
            assertEquals("valide", formatResult(result));
        }

        @Test
        @DisplayName("décrit les erreurs pour un ORDERS non conforme")
        void ordersInvalideRetourneErreursExplicites() {
            ValidationResult result = validateContent(INVALID_ORDERS);

            assertFalse(result.isValid());
            assertFalse(result.getErrors().isEmpty(), "La validation doit retourner au moins une erreur");

            String formatted = formatResult(result);
            assertNotEquals("valide", formatted);
            assertTrue(formatted.contains("SupplyChainTradeTransaction"),
                    () -> "Le message d'erreur doit mentionner l'élément manquant : " + formatted);
        }
    }

    @Nested
    @DisplayName("Validation des messages DESADV")
    class DesadvValidation {

        @Test
        @DisplayName("valide un DESADV conforme au XSD UNECE")
        void desadvValideRetourneMessageValide() throws IOException {
            ValidationResult result = validateResource("desadv-valid.xml");

            assertTrue(result.isValid(), () -> formatResult(result));
            assertEquals("valide", formatResult(result));
        }

        @Test
        @DisplayName("décrit les erreurs pour un DESADV non conforme")
        void desadvInvalideRetourneErreursExplicites() {
            ValidationResult result = validateContent(INVALID_DESADV);

            assertFalse(result.isValid());
            assertFalse(result.getErrors().isEmpty(), "La validation doit retourner au moins une erreur");

            String formatted = formatResult(result);
            assertNotEquals("valide", formatted);
            assertTrue(formatted.contains("ApplicableHeaderTradeDelivery"),
                    () -> "Le message d'erreur doit mentionner l'élément manquant : " + formatted);
        }
    }

    @Nested
    @DisplayName("Validation des messages INVOICE")
    class InvoiceValidation {

        @Test
        @DisplayName("valide une INVOICE conforme au XSD UNECE")
        void invoiceValideRetourneMessageValide() throws IOException {
            ValidationResult result = validateResource("invoice-valid.xml");

            assertTrue(result.isValid(), () -> formatResult(result));
            assertEquals("valide", formatResult(result));
        }

        @Test
        @DisplayName("décrit les erreurs pour une INVOICE non conforme")
        void invoiceInvalideRetourneErreursExplicites() {
            ValidationResult result = validateContent(INVALID_INVOICE);

            assertFalse(result.isValid());
            assertFalse(result.getErrors().isEmpty(), "La validation doit retourner au moins une erreur");

            String formatted = formatResult(result);
            assertNotEquals("valide", formatted);
            assertTrue(formatted.contains("ApplicableHeaderTradeSettlement"),
                    () -> "Le message d'erreur doit mentionner l'élément manquant : " + formatted);
        }
    }

    private ValidationResult validateResource(String resourceName) throws IOException {
        XSDValidator validator = buildValidator();
        ValidationResult result;
        try (InputStream xml = getResource(resourceName)) {
            assertNotNull(xml, "Le fichier " + resourceName + " doit être présent dans les ressources de test");
            result = validator.validate(xml);
        }
        return result;
    }

    private ValidationResult validateContent(String xmlContent) {
        XSDValidator validator = buildValidator();
        return validator.validate(xmlContent);
    }

    private XSDValidator buildValidator() {
        XSDValidator validator = new XSDValidator();
        validator.setSchemaVersion(DEFAULT_VERSION);
        return validator;
    }

    private InputStream getResource(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    private String formatResult(ValidationResult result) {
        if (result.isValid()) {
            return "valide";
        }

        return result.getErrors().stream()
                .map(error -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append(error.getSeverity() != null ? error.getSeverity().name() : "ERREUR");
                    builder.append(" : ");
                    builder.append(error.getMessage());
                    if (error.getLineNumber() > 0 || error.getColumnNumber() > 0) {
                        builder.append(" (ligne ")
                                .append(error.getLineNumber())
                                .append(", colonne ")
                                .append(error.getColumnNumber())
                                .append(")");
                    }
                    return builder.toString();
                })
                .collect(Collectors.joining("\n"));
    }
}
