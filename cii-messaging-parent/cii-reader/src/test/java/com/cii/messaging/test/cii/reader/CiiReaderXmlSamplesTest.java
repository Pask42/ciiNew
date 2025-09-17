package com.cii.messaging.test.cii.reader;

import com.cii.messaging.reader.OrderReader;
import com.cii.messaging.test.cii.support.CiiSampleResource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Skeleton test suite prepared for future reader-oriented unit tests.  The
 * structure mirrors the main {@code cii-reader} responsibilities (parsing
 * multiple Cross Industry message types and handling defensive XML parsing).
 */
@DisplayName("CII Reader – XML processing contract")
class CiiReaderXmlSamplesTest {

    @Nested
    @DisplayName("Order samples")
    class OrderSamples {

        @Test
        @DisplayName("lit et désérialise un ORDER CII depuis un chemin de fichier")
        void shouldParseOrderSample(@TempDir Path tempDir) throws Exception {
            var samples = new CiiSampleResource();
            Path orderXml = tempDir.resolve("order-sample.xml");
            try (var inputStream = samples.open("order-sample.xml")) {
                Files.copy(inputStream, orderXml, StandardCopyOption.REPLACE_EXISTING);
            }

            var reader = new OrderReader();
            var order = reader.read(orderXml.toFile());

            assertNotNull(order, "La commande doit être désérialisée");

            var exchangedDocument = order.getExchangedDocument();
            assertNotNull(exchangedDocument, "Le document échangé doit être présent");

            var id = exchangedDocument.getID();
            assertNotNull(id, "L'identifiant de commande doit être présent");
            assertEquals("ORD-2024-001", id.getValue(), "L'identifiant de commande doit correspondre au fichier d'exemple");

            var issueDateTime = exchangedDocument.getIssueDateTime();
            assertNotNull(issueDateTime, "La date d'émission doit être renseignée");
            var dateTimeString = issueDateTime.getDateTimeString();
            assertNotNull(dateTimeString, "La représentation textuelle de la date doit être fournie");
            assertEquals("20240115133000", dateTimeString.getValue(), "La date d'émission doit correspondre à l'exemple");

            var tradeTransaction = order.getSupplyChainTradeTransaction();
            assertNotNull(tradeTransaction, "La transaction commerciale doit être présente");

            var agreement = tradeTransaction.getApplicableHeaderTradeAgreement();
            assertNotNull(agreement, "L'entête d'accord commercial doit être présent");

            var buyer = agreement.getBuyerTradeParty();
            assertNotNull(buyer, "Les informations d'acheteur doivent être présentes");
            var buyerName = buyer.getName();
            assertNotNull(buyerName, "Le nom de l'acheteur doit être renseigné");
            assertEquals("Buyer Company SAS", buyerName.getValue(), "Le nom de l'acheteur doit correspondre à l'exemple");

            var lineItems = tradeTransaction.getIncludedSupplyChainTradeLineItem();
            assertNotNull(lineItems, "Les lignes de commande doivent être présentes");
            assertEquals(2, lineItems.size(), "La commande doit contenir deux lignes");

            var firstLine = lineItems.get(0);
            assertNotNull(firstLine.getAssociatedDocumentLineDocument(), "La première ligne doit avoir une référence de document");
            assertEquals("1", firstLine.getAssociatedDocumentLineDocument().getLineID().getValue(), "L'identifiant de la première ligne doit correspondre");
            assertNotNull(firstLine.getSpecifiedTradeProduct(), "La première ligne doit référencer un produit");
            assertFalse(firstLine.getSpecifiedTradeProduct().getName().isEmpty(), "Le produit de la première ligne doit être nommé");
            assertEquals("Industrial Widget Type A", firstLine.getSpecifiedTradeProduct().getName().get(0).getValue(), "Le produit de la première ligne doit correspondre");

            var secondLine = lineItems.get(1);
            assertNotNull(secondLine.getAssociatedDocumentLineDocument(), "La seconde ligne doit avoir une référence de document");
            assertEquals("2", secondLine.getAssociatedDocumentLineDocument().getLineID().getValue(), "L'identifiant de la seconde ligne doit correspondre");
            assertNotNull(secondLine.getSpecifiedTradeProduct(), "La seconde ligne doit référencer un produit");
            assertFalse(secondLine.getSpecifiedTradeProduct().getName().isEmpty(), "Le produit de la seconde ligne doit être nommé");
            assertEquals("Industrial Widget Type B", secondLine.getSpecifiedTradeProduct().getName().get(0).getValue(), "Le produit de la seconde ligne doit correspondre");
        }
    }

    @Nested
    @DisplayName("Invoice samples")
    class InvoiceSamples {

        @Test
        @Disabled("Reader implementation tests to be provided in a future iteration")
        void shouldParseInvoiceSample() {
            var samples = new CiiSampleResource();
            // Placeholder for future parsing assertion using samples.open("invoice-sample.xml")
        }
    }

    @Test
    @Disabled("Security hardening tests to be provided in a future iteration")
    @DisplayName("Rejects XML entities and dangerous constructs")
    void shouldProtectAgainstXxePayloads() {
        // Placeholder for future XXE protection check leveraging secure parser configuration
    }
}
