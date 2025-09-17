package com.cii.messaging.validator;

import com.cii.messaging.validator.impl.XSDValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class XSDValidatorOrderTest {
    private static final String INVALID_ORDER = """
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

    @Test
    @DisplayName("valide un ORDER conforme au XSD UNECE")
    void validOrderReturnsValideMessage() throws IOException {
        XSDValidator validator = new XSDValidator();
        validator.setSchemaVersion(SchemaVersion.D23B);

        ValidationResult result;
        try (InputStream xml = getResource("order-valid.xml")) {
            assertNotNull(xml, "Le fichier ORDER d'exemple doit être présent dans les ressources de test");
            result = validator.validate(xml);
        }

        assertTrue(result.isValid(), () -> formatResult(result));
        assertEquals("valide", formatResult(result));
    }

    @Test
    @DisplayName("décrit les erreurs pour un ORDER non conforme")
    void invalidOrderReturnsExplicitErrors() {
        XSDValidator validator = new XSDValidator();
        validator.setSchemaVersion(SchemaVersion.D23B);

        ValidationResult result = validator.validate(INVALID_ORDER);

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty(), "La validation doit retourner au moins une erreur");

        String formatted = formatResult(result);
        assertNotEquals("valide", formatted);
        assertTrue(formatted.contains("SupplyChainTradeTransaction"),
                () -> "Le message d'erreur doit mentionner l'élément manquant : " + formatted);
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
