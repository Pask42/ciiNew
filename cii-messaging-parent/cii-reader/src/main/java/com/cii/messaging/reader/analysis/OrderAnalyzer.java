package com.cii.messaging.reader.analysis;

import com.cii.messaging.model.order.Order;
import com.cii.messaging.reader.CIIReaderException;
import com.cii.messaging.reader.OrderReader;
import com.cii.messaging.unece.order.AmountType;
import com.cii.messaging.unece.order.CurrencyCodeType;
import com.cii.messaging.unece.order.DateTimeType;
import com.cii.messaging.unece.order.DocumentLineDocumentType;
import com.cii.messaging.unece.order.HeaderTradeAgreementType;
import com.cii.messaging.unece.order.HeaderTradeSettlementType;
import com.cii.messaging.unece.order.IDType;
import com.cii.messaging.unece.order.LineTradeAgreementType;
import com.cii.messaging.unece.order.LineTradeDeliveryType;
import com.cii.messaging.unece.order.LineTradeSettlementType;
import com.cii.messaging.unece.order.QuantityType;
import com.cii.messaging.unece.order.SupplyChainTradeLineItemType;
import com.cii.messaging.unece.order.SupplyChainTradeTransactionType;
import com.cii.messaging.unece.order.TextType;
import com.cii.messaging.unece.order.TradePartyType;
import com.cii.messaging.unece.order.TradePriceType;
import com.cii.messaging.unece.order.TradeProductType;
import com.cii.messaging.unece.order.TradeSettlementLineMonetarySummationType;
import com.cii.messaging.unece.order.TradeSettlementHeaderMonetarySummationType;
import com.cii.messaging.unece.order.TradeTaxType;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Utilitaire qui lit un fichier CrossIndustryOrder et en propose un résumé exploitable.
 */
public final class OrderAnalyzer {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private OrderAnalyzer() {
        // utilitaire
    }

    /**
     * Lit un fichier XML de commande et retourne un objet métier contenant les informations principales.
     *
     * @param cheminFichier chemin vers le fichier ORDER XML
     * @return les informations extraites
     * @throws IOException        si le fichier est introuvable ou inaccessible
     * @throws CIIReaderException si la désérialisation échoue
     */
    public static OrderAnalysisResult analyserOrder(String cheminFichier) throws IOException, CIIReaderException {
        Objects.requireNonNull(cheminFichier, "cheminFichier");
        Path path = Path.of(cheminFichier);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IOException("Fichier XML introuvable : " + cheminFichier);
        }

        OrderReader reader = new OrderReader();
        Order order = reader.read(new File(cheminFichier));
        return analyse(order);
    }

    private static OrderAnalysisResult analyse(Order order) {
        String orderId = null;
        String issueDate = null;
        String currency = null;
        String buyerReference = null;
        OrderAnalysisResult.PartySummary orderingCustomer = null;
        OrderAnalysisResult.PartySummary buyer = null;
        OrderAnalysisResult.PartySummary payer = null;
        OrderAnalysisResult.PartySummary seller = null;
        OrderAnalysisResult.MonetaryAmount orderNetTotal = null;
        OrderAnalysisResult.MonetaryAmount orderTaxTotal = null;
        OrderAnalysisResult.MonetaryAmount orderGrossTotal = null;
        List<OrderAnalysisResult.TaxSummary> orderTaxes = new ArrayList<>();
        List<OrderAnalysisResult.OrderLineSummary> lines = new ArrayList<>();

        if (order.getExchangedDocument() != null) {
            if (order.getExchangedDocument().getID() != null) {
                orderId = safeValue(order.getExchangedDocument().getID());
            }
            issueDate = formatIssueDate(order.getExchangedDocument().getIssueDateTime());
        }

        SupplyChainTradeTransactionType transaction = order.getSupplyChainTradeTransaction();
        if (transaction != null) {
            HeaderTradeAgreementType agreement = transaction.getApplicableHeaderTradeAgreement();
            if (agreement != null) {
                buyerReference = safeValue(agreement.getBuyerReference());
                buyer = mapParty(agreement.getBuyerTradeParty());
                seller = mapParty(agreement.getSellerTradeParty());
                List<TradePartyType> requisitioners = agreement.getBuyerRequisitionerTradeParty();
                if (requisitioners != null && !requisitioners.isEmpty()) {
                    orderingCustomer = mapParty(requisitioners.get(0));
                }
                if (orderingCustomer == null) {
                    orderingCustomer = mapParty(agreement.getProductEndUserTradeParty());
                }
            }

            HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
            if (settlement != null) {
                currency = safeCurrency(settlement.getOrderCurrencyCode());
                payer = mapParty(settlement.getPayerTradeParty());

                TradeSettlementHeaderMonetarySummationType summary = settlement.getSpecifiedTradeSettlementHeaderMonetarySummation();
                if (summary != null) {
                    if (orderNetTotal == null) {
                        orderNetTotal = toMonetaryAmount(firstAmount(summary.getTaxBasisTotalAmount()), currency);
                    }
                    if (orderTaxTotal == null) {
                        orderTaxTotal = toMonetaryAmount(firstAmount(summary.getTaxTotalAmount()), currency);
                    }
                    if (orderGrossTotal == null) {
                        orderGrossTotal = toMonetaryAmount(firstAmount(summary.getGrandTotalAmount()), currency);
                    }
                    if (orderGrossTotal == null) {
                        orderGrossTotal = toMonetaryAmount(firstAmount(summary.getIncludingTaxesLineTotalAmount()), currency);
                    }
                    if (orderGrossTotal == null) {
                        orderGrossTotal = toMonetaryAmount(firstAmount(summary.getDuePayableAmount()), currency);
                    }
                }

                orderTaxes = new ArrayList<>(mapTaxes(settlement.getApplicableTradeTax(), currency));
                if (orderTaxTotal == null && !orderTaxes.isEmpty()) {
                    orderTaxTotal = sumTaxAmounts(orderTaxes, currency);
                }
            }

            List<SupplyChainTradeLineItemType> lineItems = transaction.getIncludedSupplyChainTradeLineItem();
            for (SupplyChainTradeLineItemType lineItem : lineItems) {
                lines.add(mapLine(lineItem, currency));
            }
        }

        int lineCount = lines.size();

        if (orderGrossTotal == null && orderNetTotal != null && orderTaxTotal != null
                && isCurrencyCompatible(orderNetTotal.getCurrency(), orderTaxTotal.getCurrency())) {
            orderGrossTotal = new OrderAnalysisResult.MonetaryAmount(
                    orderNetTotal.getAmount().add(orderTaxTotal.getAmount()),
                    chooseCurrency(orderNetTotal.getCurrency(), orderTaxTotal.getCurrency(), currency));
        }

        if (currency == null) {
            currency = chooseCurrency(
                    orderGrossTotal != null ? orderGrossTotal.getCurrency() : null,
                    orderNetTotal != null ? orderNetTotal.getCurrency() : null,
                    orderTaxTotal != null ? orderTaxTotal.getCurrency() : null);
        }

        return new OrderAnalysisResult(
                orderId,
                issueDate,
                buyerReference,
                orderingCustomer,
                buyer,
                payer,
                seller,
                currency,
                lineCount,
                orderNetTotal,
                orderTaxTotal,
                orderGrossTotal,
                orderTaxes,
                lines);
    }

    private static OrderAnalysisResult.OrderLineSummary mapLine(SupplyChainTradeLineItemType lineItem, String defaultCurrency) {
        String lineId = null;
        String productIdentifier = null;
        String productName = null;
        BigDecimal quantityValue = null;
        String quantityUnit = null;
        OrderAnalysisResult.MonetaryAmount netUnitPrice = null;
        OrderAnalysisResult.MonetaryAmount grossUnitPrice = null;
        OrderAnalysisResult.MonetaryAmount lineNetAmount = null;
        OrderAnalysisResult.MonetaryAmount lineGrossAmount = null;
        OrderAnalysisResult.MonetaryAmount lineTaxAmount = null;
        List<OrderAnalysisResult.TaxSummary> taxes = new ArrayList<>();

        if (lineItem.getAssociatedDocumentLineDocument() != null) {
            DocumentLineDocumentType lineDocument = lineItem.getAssociatedDocumentLineDocument();
            if (lineDocument.getLineID() != null) {
                lineId = safeValue(lineDocument.getLineID());
            }
        }

        TradeProductType product = lineItem.getSpecifiedTradeProduct();
        if (product != null) {
            productName = firstTextValue(product.getName());
            if (productName == null) {
                productName = safeValue(product.getTradeName());
            }
            if (product.getGlobalID() != null && !product.getGlobalID().isEmpty()) {
                productIdentifier = safeValue(product.getGlobalID().get(0));
            } else if (product.getID() != null) {
                productIdentifier = safeValue(product.getID());
            } else if (product.getSellerAssignedID() != null) {
                productIdentifier = safeValue(product.getSellerAssignedID());
            }
        }

        LineTradeDeliveryType delivery = lineItem.getSpecifiedLineTradeDelivery();
        if (delivery != null && delivery.getRequestedQuantity() != null) {
            QuantityType requestedQuantity = delivery.getRequestedQuantity();
            quantityValue = requestedQuantity.getValue();
            quantityUnit = requestedQuantity.getUnitCode();
        }

        LineTradeAgreementType lineAgreement = lineItem.getSpecifiedLineTradeAgreement();
        if (lineAgreement != null) {
            if (lineAgreement.getNetPriceProductTradePrice() != null) {
                TradePriceType price = lineAgreement.getNetPriceProductTradePrice();
                netUnitPrice = toMonetaryAmount(firstAmount(price.getChargeAmount()), defaultCurrency);
            }
            if (lineAgreement.getGrossPriceProductTradePrice() != null) {
                TradePriceType price = lineAgreement.getGrossPriceProductTradePrice();
                grossUnitPrice = toMonetaryAmount(firstAmount(price.getChargeAmount()), defaultCurrency);
            }
        }

        LineTradeSettlementType lineSettlement = lineItem.getSpecifiedLineTradeSettlement();
        if (lineSettlement != null) {
            TradeSettlementLineMonetarySummationType summary = lineSettlement.getSpecifiedTradeSettlementLineMonetarySummation();
            if (summary != null) {
                lineNetAmount = toMonetaryAmount(firstAmount(summary.getNetLineTotalAmount()), defaultCurrency);
                if (lineNetAmount == null) {
                    lineNetAmount = toMonetaryAmount(firstAmount(summary.getLineTotalAmount()), defaultCurrency);
                }
                lineTaxAmount = toMonetaryAmount(firstAmount(summary.getTaxTotalAmount()), defaultCurrency);
                lineGrossAmount = toMonetaryAmount(firstAmount(summary.getGrandTotalAmount()), defaultCurrency);
                if (lineGrossAmount == null) {
                    lineGrossAmount = toMonetaryAmount(firstAmount(summary.getIncludingTaxesLineTotalAmount()), defaultCurrency);
                }
            }
            taxes = new ArrayList<>(mapTaxes(lineSettlement.getApplicableTradeTax(), defaultCurrency));
        }

        if (lineTaxAmount == null && !taxes.isEmpty()) {
            lineTaxAmount = sumTaxAmounts(taxes, defaultCurrency);
        }

        if (lineGrossAmount == null && lineNetAmount != null && lineTaxAmount != null
                && isCurrencyCompatible(lineNetAmount.getCurrency(), lineTaxAmount.getCurrency())) {
            lineGrossAmount = new OrderAnalysisResult.MonetaryAmount(
                    lineNetAmount.getAmount().add(lineTaxAmount.getAmount()),
                    chooseCurrency(lineNetAmount.getCurrency(), lineTaxAmount.getCurrency(), defaultCurrency));
        }

        if (lineNetAmount == null && lineGrossAmount != null && lineTaxAmount != null
                && isCurrencyCompatible(lineGrossAmount.getCurrency(), lineTaxAmount.getCurrency())) {
            lineNetAmount = new OrderAnalysisResult.MonetaryAmount(
                    lineGrossAmount.getAmount().subtract(lineTaxAmount.getAmount()),
                    chooseCurrency(lineGrossAmount.getCurrency(), defaultCurrency));
        }

        return new OrderAnalysisResult.OrderLineSummary(
                lineId,
                productIdentifier,
                productName,
                quantityValue,
                quantityUnit,
                netUnitPrice,
                grossUnitPrice,
                lineNetAmount,
                lineGrossAmount,
                lineTaxAmount,
                taxes
        );
    }

    private static AmountType firstAmount(List<AmountType> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return null;
        }
        return amounts.get(0);
    }

    private static String firstTextValue(List<TextType> texts) {
        if (texts == null || texts.isEmpty()) {
            return null;
        }
        return safeValue(texts.get(0));
    }

    private static String firstIdValue(List<IDType> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return safeValue(ids.get(0));
    }

    private static OrderAnalysisResult.PartySummary mapParty(TradePartyType party) {
        if (party == null) {
            return null;
        }
        String name = safeValue(party.getName());
        String identifier = firstIdValue(party.getID());
        String globalIdentifier = findGlobalIdentifier(party);
        if ((name == null || name.isBlank())
                && (identifier == null || identifier.isBlank())
                && (globalIdentifier == null || globalIdentifier.isBlank())) {
            return null;
        }
        return new OrderAnalysisResult.PartySummary(name, identifier, globalIdentifier);
    }

    private static String findGlobalIdentifier(TradePartyType party) {
        if (party == null) {
            return null;
        }
        String fromGlobal = findIdByScheme(party.getGlobalID(), "0088", "GLN");
        if (fromGlobal != null && !fromGlobal.isBlank()) {
            return fromGlobal;
        }
        String fromId = findIdByScheme(party.getID(), "0088", "GLN");
        if (fromId != null && !fromId.isBlank()) {
            return fromId;
        }
        return firstIdValue(party.getGlobalID());
    }

    private static String findIdByScheme(List<IDType> ids, String... schemeCandidates) {
        if (ids == null || ids.isEmpty() || schemeCandidates == null || schemeCandidates.length == 0) {
            return null;
        }
        Set<String> normalized = new HashSet<>();
        for (String candidate : schemeCandidates) {
            if (candidate != null && !candidate.isBlank()) {
                normalized.add(candidate.trim().toUpperCase());
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        for (IDType id : ids) {
            if (id == null) {
                continue;
            }
            String scheme = id.getSchemeID();
            if (scheme != null && normalized.contains(scheme.trim().toUpperCase())) {
                String value = safeValue(id);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static List<OrderAnalysisResult.TaxSummary> mapTaxes(List<TradeTaxType> taxes, String defaultCurrency) {
        if (taxes == null || taxes.isEmpty()) {
            return List.of();
        }
        List<OrderAnalysisResult.TaxSummary> results = new ArrayList<>();
        for (TradeTaxType tax : taxes) {
            if (tax == null) {
                continue;
            }
            String typeCode = null;
            if (tax.getTypeCode() != null && tax.getTypeCode().getValue() != null) {
                typeCode = tax.getTypeCode().getValue().value();
            }
            String categoryCode = null;
            if (tax.getCategoryCode() != null && tax.getCategoryCode().getValue() != null) {
                categoryCode = tax.getCategoryCode().getValue().value();
            }
            BigDecimal rate = tax.getRateApplicablePercent() != null ? tax.getRateApplicablePercent().getValue() : null;
            OrderAnalysisResult.MonetaryAmount baseAmount = toMonetaryAmount(firstAmount(tax.getBasisAmount()), defaultCurrency);
            if (baseAmount == null) {
                baseAmount = toMonetaryAmount(firstAmount(tax.getLineTotalBasisAmount()), defaultCurrency);
            }
            if (baseAmount == null) {
                baseAmount = toMonetaryAmount(firstAmount(tax.getUnitBasisAmount()), defaultCurrency);
            }
            OrderAnalysisResult.MonetaryAmount taxAmount = toMonetaryAmount(firstAmount(tax.getCalculatedAmount()), defaultCurrency);
            if (taxAmount == null) {
                taxAmount = toMonetaryAmount(firstAmount(tax.getGrandTotalAmount()), defaultCurrency);
            }
            String exemptionReason = safeValue(tax.getExemptionReason());
            results.add(new OrderAnalysisResult.TaxSummary(typeCode, categoryCode, rate, baseAmount, taxAmount, exemptionReason));
        }
        return Collections.unmodifiableList(results);
    }

    private static OrderAnalysisResult.MonetaryAmount sumTaxAmounts(List<OrderAnalysisResult.TaxSummary> taxes, String defaultCurrency) {
        OrderAnalysisResult.MonetaryAmount total = null;
        for (OrderAnalysisResult.TaxSummary tax : taxes) {
            if (tax == null) {
                continue;
            }
            OrderAnalysisResult.MonetaryAmount amount = tax.getTaxAmount();
            if (amount == null || amount.getAmount() == null) {
                continue;
            }
            if (total == null) {
                total = new OrderAnalysisResult.MonetaryAmount(amount.getAmount(), chooseCurrency(amount.getCurrency(), defaultCurrency));
            } else if (isCurrencyCompatible(total.getCurrency(), amount.getCurrency())) {
                total = new OrderAnalysisResult.MonetaryAmount(
                        total.getAmount().add(amount.getAmount()),
                        chooseCurrency(total.getCurrency(), amount.getCurrency(), defaultCurrency));
            }
        }
        return total;
    }

    private static boolean isCurrencyCompatible(String left, String right) {
        if (left == null || left.isBlank() || right == null || right.isBlank()) {
            return true;
        }
        return left.equals(right);
    }

    private static String chooseCurrency(String... currencies) {
        if (currencies == null) {
            return null;
        }
        for (String currency : currencies) {
            if (currency != null && !currency.isBlank()) {
                return currency;
            }
        }
        return null;
    }

    private static OrderAnalysisResult.MonetaryAmount toMonetaryAmount(AmountType amount, String defaultCurrency) {
        if (amount == null || amount.getValue() == null) {
            return null;
        }
        return toMonetaryAmount(amount.getValue(), amount.getCurrencyID(), defaultCurrency);
    }

    private static OrderAnalysisResult.MonetaryAmount toMonetaryAmount(BigDecimal value, String currency, String defaultCurrency) {
        if (value == null) {
            return null;
        }
        String resolvedCurrency = chooseCurrency(currency, defaultCurrency);
        return new OrderAnalysisResult.MonetaryAmount(value, resolvedCurrency);
    }

    private static String safeValue(TextType textType) {
        return textType != null ? textType.getValue() : null;
    }

    private static String safeValue(IDType id) {
        return id != null ? id.getValue() : null;
    }

    private static String safeCurrency(CurrencyCodeType currencyCode) {
        if (currencyCode == null || currencyCode.getValue() == null) {
            return null;
        }
        return currencyCode.getValue().value();
    }

    private static String formatIssueDate(DateTimeType dateTime) {
        if (dateTime == null) {
            return null;
        }
        if (dateTime.getDateTime() != null) {
            return DATE_TIME_FORMAT.format(dateTime.getDateTime().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
        }
        if (dateTime.getDateTimeString() != null) {
            String rawValue = dateTime.getDateTimeString().getValue();
            String format = dateTime.getDateTimeString().getFormat();
            String formatted = parseDateTimeString(rawValue, format);
            if (formatted != null) {
                return formatted;
            }
            return rawValue;
        }
        return null;
    }

    private static String parseDateTimeString(String rawValue, String formatCode) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> candidates = new ArrayList<>();
        if ("102".equals(formatCode)) {
            candidates.add(DateTimeFormatter.ofPattern("yyyyMMdd"));
            candidates.add(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
        if (rawValue.length() == 8) {
            candidates.add(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if (rawValue.length() == 12) {
            candidates.add(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        }
        if (rawValue.length() == 14) {
            candidates.add(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }

        for (DateTimeFormatter formatter : candidates) {
            if (formatter == null) {
                continue;
            }
            try {
                LocalDateTime dateTime = LocalDateTime.parse(rawValue, formatter);
                return DATE_TIME_FORMAT.format(dateTime);
            } catch (DateTimeParseException ignored) {
                try {
                    LocalDate date = LocalDate.parse(rawValue, formatter);
                    return DATE_FORMAT.format(date);
                } catch (DateTimeParseException ignoredAgain) {
                    // on tente le formateur suivant
                }
            }
        }
        return null;
    }
}
