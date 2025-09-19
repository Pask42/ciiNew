package com.cii.messaging.reader.analysis;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderAnalyzerTest {

    @Test
    void shouldProduceDetailedSummary() throws Exception {
        Path samplePath = resourcePath("/order-detailed-sample.xml");

        OrderAnalysisResult result = OrderAnalyzer.analyserOrder(samplePath.toString());

        assertNotNull(result);
        assertEquals("ORD-DET-001", result.getOrderId());
        assertEquals("DET-REF-001", result.getBuyerReference());

        assertParty(result.getOrderingCustomer(), "Client Commandeur", "3300012399999");
        assertParty(result.getBuyer(), "Acheteur Détaillé SAS", "3300012300002");
        assertParty(result.getInvoicee(), "Société Facturée", "3300012300019");
        assertParty(result.getPayer(), "Société Facturée", "3300012300019");
        assertEquals(result.getInvoicee(), result.getPayer());
        assertParty(result.getSeller(), "Fournisseur Démonstration SA", "5401234500008");
        assertParty(result.getShipTo(), "Plateforme Logistique Client", "3300012390005");

        assertMonetary(result.getOrderNetTotal(), "EUR", new BigDecimal("1000.00"));
        assertMonetary(result.getOrderTaxTotal(), "EUR", new BigDecimal("200.00"));
        assertMonetary(result.getOrderGrossTotal(), "EUR", new BigDecimal("1200.00"));

        List<OrderAnalysisResult.OrderLineSummary> lines = result.getLines();
        assertEquals(1, lines.size());

        OrderAnalysisResult.OrderLineSummary line = lines.get(0);
        assertEquals("1", line.getLineId());
        assertEquals("Produit de démonstration", line.getProductName());
        assertEquals(new BigDecimal("10"), line.getQuantity());
        assertEquals("EA", line.getQuantityUnit());
        assertMonetary(line.getNetUnitPrice(), "EUR", new BigDecimal("100.00"));
        assertMonetary(line.getGrossUnitPrice(), "EUR", new BigDecimal("120.00"));
        assertMonetary(line.getLineNetAmount(), "EUR", new BigDecimal("1000.00"));
        assertMonetary(line.getLineTaxAmount(), "EUR", new BigDecimal("200.00"));
        assertMonetary(line.getLineGrossAmount(), "EUR", new BigDecimal("1200.00"));
        assertEquals(1, line.getTaxes().size());
        OrderAnalysisResult.TaxSummary tax = line.getTaxes().get(0);
        assertEquals("VAT", tax.getTypeCode());
        assertEquals("S", tax.getCategoryCode());
        assertEquals(new BigDecimal("20.00"), tax.getRatePercent());
        assertMonetary(tax.getBaseAmount(), "EUR", new BigDecimal("1000.00"));
        assertMonetary(tax.getTaxAmount(), "EUR", new BigDecimal("200.00"));

        String pretty = result.toPrettyString();
        assertTrue(pretty.contains("Client commandeur"));
        assertTrue(pretty.contains("GLN : 3300012399999"));
        assertTrue(pretty.contains("Facturé (invoicee)"));
        assertTrue(pretty.contains("Lieu de livraison"));
        assertTrue(pretty.contains("Prix unitaire HT : 100 EUR"));
        assertTrue(pretty.contains("Total ligne TTC : 1200 EUR"));
        assertTrue(pretty.contains("Taux 20%"));
    }

    @Test
    void shouldComputeTotalsFromLinesWhenHeaderMissing() throws Exception {
        Path samplePath = resourcePath("/order-missing-header-totals.xml");

        OrderAnalysisResult result = OrderAnalyzer.analyserOrder(samplePath.toString());

        assertNotNull(result);
        assertEquals(2, result.getLineCount());
        assertMonetary(result.getOrderNetTotal(), "EUR", new BigDecimal("250.00"));
        assertMonetary(result.getOrderTaxTotal(), "EUR", new BigDecimal("35.00"));
        assertMonetary(result.getOrderGrossTotal(), "EUR", new BigDecimal("285.00"));

        List<OrderAnalysisResult.TaxSummary> orderTaxes = result.getOrderTaxes();
        assertEquals(2, orderTaxes.size());

        OrderAnalysisResult.TaxSummary first = orderTaxes.get(0);
        assertEquals("VAT", first.getTypeCode());
        assertEquals("S", first.getCategoryCode());
        assertEquals(0, new BigDecimal("20").compareTo(first.getRatePercent()));
        assertMonetary(first.getBaseAmount(), "EUR", new BigDecimal("100.00"));
        assertMonetary(first.getTaxAmount(), "EUR", new BigDecimal("20.00"));

        OrderAnalysisResult.TaxSummary second = orderTaxes.get(1);
        assertEquals("VAT", second.getTypeCode());
        assertEquals("AA", second.getCategoryCode());
        assertEquals(0, new BigDecimal("10").compareTo(second.getRatePercent()));
        assertMonetary(second.getBaseAmount(), "EUR", new BigDecimal("150.00"));
        assertMonetary(second.getTaxAmount(), "EUR", new BigDecimal("15.00"));
    }

    private static void assertParty(OrderAnalysisResult.PartySummary party, String expectedName, String expectedGln) {
        assertNotNull(party);
        assertEquals(expectedName, party.getName());
        assertEquals(expectedGln, party.getGlobalIdentifier());
    }

    private static void assertMonetary(OrderAnalysisResult.MonetaryAmount amount, String expectedCurrency, BigDecimal expectedValue) {
        assertNotNull(amount, "Le montant ne doit pas être null");
        assertEquals(expectedCurrency, amount.getCurrency());
        assertEquals(0, expectedValue.compareTo(amount.getAmount()));
    }

    private static Path resourcePath(String resource) throws URISyntaxException {
        var url = OrderAnalyzerTest.class.getResource(resource);
        assertNotNull(url, "Ressource introuvable : " + resource);
        return Path.of(url.toURI());
    }
}
