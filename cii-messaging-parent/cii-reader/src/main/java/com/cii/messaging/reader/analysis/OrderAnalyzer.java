package com.cii.messaging.reader.analysis;

import com.cii.messaging.model.order.Order;
import com.cii.messaging.reader.CIIReaderException;
import com.cii.messaging.reader.OrderReader;
import com.cii.messaging.unece.order.AmountType;
import com.cii.messaging.unece.order.CurrencyCodeType;
import com.cii.messaging.unece.order.DateTimeType;
import com.cii.messaging.unece.order.DocumentLineDocumentType;
import com.cii.messaging.unece.order.HeaderTradeAgreementType;
import com.cii.messaging.unece.order.HeaderTradeDeliveryType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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
        OrderAnalysisResult.PartySummary invoicee = null;
        OrderAnalysisResult.PartySummary payer = null;
        OrderAnalysisResult.PartySummary seller = null;
        OrderAnalysisResult.PartySummary shipTo = null;
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

            HeaderTradeDeliveryType delivery = transaction.getApplicableHeaderTradeDelivery();
            if (delivery != null) {
                shipTo = mapParty(delivery.getShipToTradeParty());
                if (shipTo == null) {
                    shipTo = mapParty(delivery.getUltimateShipToTradeParty());
                }
            }

            HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
            if (settlement != null) {
                currency = safeCurrency(settlement.getOrderCurrencyCode());
                invoicee = mapParty(settlement.getInvoiceeTradeParty());
                payer = mapParty(settlement.getPayerTradeParty());
                if (payer == null) {
                    payer = invoicee;
                }
                if (invoicee == null) {
                    invoicee = payer;
                }

                TradeSettlementHeaderMonetarySummationType summary = settlement.getSpecifiedTradeSettlementHeaderMonetarySummation();
                if (summary != null) {
                    if (orderNetTotal == null) {
                        orderNetTotal = firstNonNullAmount(currency,
                                summary.getNetLineTotalAmount(),
                                summary.getLineTotalAmount(),
                                summary.getTaxBasisTotalAmount());
                    }
                    if (orderTaxTotal == null) {
                        orderTaxTotal = firstNonNullAmount(currency, summary.getTaxTotalAmount());
                    }
                    if (orderGrossTotal == null) {
                        orderGrossTotal = firstNonNullAmount(currency,
                                summary.getGrandTotalAmount(),
                                summary.getIncludingTaxesLineTotalAmount(),
                                summary.getDuePayableAmount());
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

        if (orderNetTotal == null) {
            orderNetTotal = sumLineAmounts(lines, OrderAnalysisResult.OrderLineSummary::getLineNetAmount, currency);
        }
        if (orderTaxTotal == null) {
            orderTaxTotal = sumLineAmounts(lines, OrderAnalysisResult.OrderLineSummary::getLineTaxAmount, currency);
        }
        if (orderTaxes.isEmpty()) {
            orderTaxes = new ArrayList<>(aggregateTaxesFromLines(lines, currency));
        }
        if (orderTaxTotal == null && !orderTaxes.isEmpty()) {
            orderTaxTotal = sumTaxAmounts(orderTaxes, currency);
        }
        if (orderGrossTotal == null) {
            orderGrossTotal = sumLineAmounts(lines, OrderAnalysisResult.OrderLineSummary::getLineGrossAmount, currency);
        }

        if (orderGrossTotal == null && orderNetTotal != null && orderTaxTotal != null
                && isCurrencyCompatible(orderNetTotal.getCurrency(), orderTaxTotal.getCurrency())) {
            orderGrossTotal = new OrderAnalysisResult.MonetaryAmount(
                    orderNetTotal.getAmount().add(orderTaxTotal.getAmount()),
                    chooseCurrency(orderNetTotal.getCurrency(), orderTaxTotal.getCurrency(), currency));
        }

        if (orderNetTotal == null && orderGrossTotal != null && orderTaxTotal != null
                && isCurrencyCompatible(orderGrossTotal.getCurrency(), orderTaxTotal.getCurrency())) {
            orderNetTotal = new OrderAnalysisResult.MonetaryAmount(
                    orderGrossTotal.getAmount().subtract(orderTaxTotal.getAmount()),
                    chooseCurrency(orderGrossTotal.getCurrency(), orderTaxTotal.getCurrency(), currency));
        }

        if (orderTaxTotal == null && orderGrossTotal != null && orderNetTotal != null
                && isCurrencyCompatible(orderGrossTotal.getCurrency(), orderNetTotal.getCurrency())) {
            orderTaxTotal = new OrderAnalysisResult.MonetaryAmount(
                    orderGrossTotal.getAmount().subtract(orderNetTotal.getAmount()),
                    chooseCurrency(orderGrossTotal.getCurrency(), orderNetTotal.getCurrency(), currency));
        }

        if (orderGrossTotal == null && orderNetTotal != null && orderNetTotal.getAmount() != null
                && !hasSignificantTax(orderTaxTotal, orderTaxes, lines)) {
            orderGrossTotal = new OrderAnalysisResult.MonetaryAmount(
                    orderNetTotal.getAmount(),
                    chooseCurrency(orderNetTotal.getCurrency(), currency));
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
                invoicee,
                payer,
                seller,
                shipTo,
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

    @SafeVarargs
    private static OrderAnalysisResult.MonetaryAmount firstNonNullAmount(String defaultCurrency, List<AmountType>... amountLists) {
        if (amountLists == null) {
            return null;
        }
        for (List<AmountType> amounts : amountLists) {
            OrderAnalysisResult.MonetaryAmount monetaryAmount = toMonetaryAmount(firstAmount(amounts), defaultCurrency);
            if (monetaryAmount != null) {
                return monetaryAmount;
            }
        }
        return null;
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

    private static List<OrderAnalysisResult.TaxSummary> aggregateTaxesFromLines(
            List<OrderAnalysisResult.OrderLineSummary> lines,
            String defaultCurrency) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        Map<TaxKey, TaxAccumulator> aggregated = new LinkedHashMap<>();
        for (OrderAnalysisResult.OrderLineSummary line : lines) {
            if (line == null) {
                continue;
            }
            for (OrderAnalysisResult.TaxSummary tax : line.getTaxes()) {
                if (tax == null) {
                    continue;
                }
                TaxKey key = new TaxKey(
                        tax.getTypeCode(),
                        tax.getCategoryCode(),
                        normalizeRate(tax.getRatePercent()),
                        tax.getExemptionReason());
                TaxAccumulator accumulator = aggregated.computeIfAbsent(key, unused -> new TaxAccumulator(key, defaultCurrency));
                accumulator.add(tax);
            }
        }
        if (aggregated.isEmpty()) {
            return List.of();
        }
        List<OrderAnalysisResult.TaxSummary> summaries = new ArrayList<>();
        for (TaxAccumulator accumulator : aggregated.values()) {
            OrderAnalysisResult.TaxSummary summary = accumulator.toSummary();
            if (summary != null) {
                summaries.add(summary);
            }
        }
        return summaries.isEmpty() ? List.of() : Collections.unmodifiableList(summaries);
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

    private static OrderAnalysisResult.MonetaryAmount sumLineAmounts(
            List<OrderAnalysisResult.OrderLineSummary> lines,
            Function<OrderAnalysisResult.OrderLineSummary, OrderAnalysisResult.MonetaryAmount> extractor,
            String defaultCurrency) {
        if (lines == null || lines.isEmpty() || extractor == null) {
            return null;
        }
        OrderAnalysisResult.MonetaryAmount total = null;
        for (OrderAnalysisResult.OrderLineSummary line : lines) {
            if (line == null) {
                continue;
            }
            OrderAnalysisResult.MonetaryAmount amount = extractor.apply(line);
            if (amount == null || amount.getAmount() == null) {
                continue;
            }
            if (total == null) {
                total = new OrderAnalysisResult.MonetaryAmount(
                        amount.getAmount(),
                        chooseCurrency(amount.getCurrency(), defaultCurrency));
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

    private static boolean hasSignificantTax(
            OrderAnalysisResult.MonetaryAmount orderTaxTotal,
            List<OrderAnalysisResult.TaxSummary> orderTaxes,
            List<OrderAnalysisResult.OrderLineSummary> lines) {
        if (orderTaxTotal != null && orderTaxTotal.getAmount() != null
                && orderTaxTotal.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            return true;
        }
        if (orderTaxes != null) {
            for (OrderAnalysisResult.TaxSummary tax : orderTaxes) {
                if (tax == null) {
                    continue;
                }
                OrderAnalysisResult.MonetaryAmount taxAmount = tax.getTaxAmount();
                if (taxAmount != null && taxAmount.getAmount() != null
                        && taxAmount.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    return true;
                }
            }
        }
        if (lines != null) {
            for (OrderAnalysisResult.OrderLineSummary line : lines) {
                if (line == null) {
                    continue;
                }
                OrderAnalysisResult.MonetaryAmount taxAmount = line.getLineTaxAmount();
                if (taxAmount != null && taxAmount.getAmount() != null
                        && taxAmount.getAmount().compareTo(BigDecimal.ZERO) != 0) {
                    return true;
                }
            }
        }
        return false;
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

    private static BigDecimal normalizeRate(BigDecimal rate) {
        return rate != null ? rate.stripTrailingZeros() : null;
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

    private record TaxKey(String typeCode, String categoryCode, BigDecimal rate, String exemptionReason) {
    }

    private static final class TaxAccumulator {
        private final TaxKey key;
        private final String defaultCurrency;
        private BigDecimal baseTotal;
        private String baseCurrency;
        private BigDecimal taxTotal;
        private String taxCurrency;

        private TaxAccumulator(TaxKey key, String defaultCurrency) {
            this.key = key;
            this.defaultCurrency = defaultCurrency;
        }

        private void add(OrderAnalysisResult.TaxSummary tax) {
            accumulateBase(tax.getBaseAmount());
            accumulateTax(tax.getTaxAmount());
        }

        private void accumulateBase(OrderAnalysisResult.MonetaryAmount amount) {
            if (amount == null || amount.getAmount() == null) {
                return;
            }
            if (baseTotal == null) {
                baseTotal = amount.getAmount();
                baseCurrency = chooseCurrency(amount.getCurrency(), defaultCurrency);
            } else if (isCurrencyCompatible(baseCurrency, amount.getCurrency())) {
                baseTotal = baseTotal.add(amount.getAmount());
                baseCurrency = chooseCurrency(baseCurrency, amount.getCurrency(), defaultCurrency);
            }
        }

        private void accumulateTax(OrderAnalysisResult.MonetaryAmount amount) {
            if (amount == null || amount.getAmount() == null) {
                return;
            }
            if (taxTotal == null) {
                taxTotal = amount.getAmount();
                taxCurrency = chooseCurrency(amount.getCurrency(), defaultCurrency);
            } else if (isCurrencyCompatible(taxCurrency, amount.getCurrency())) {
                taxTotal = taxTotal.add(amount.getAmount());
                taxCurrency = chooseCurrency(taxCurrency, amount.getCurrency(), defaultCurrency);
            }
        }

        private OrderAnalysisResult.TaxSummary toSummary() {
            OrderAnalysisResult.MonetaryAmount base = baseTotal != null
                    ? new OrderAnalysisResult.MonetaryAmount(baseTotal, baseCurrency)
                    : null;
            OrderAnalysisResult.MonetaryAmount tax = taxTotal != null
                    ? new OrderAnalysisResult.MonetaryAmount(taxTotal, taxCurrency)
                    : null;
            if (base == null && tax == null && key.rate() == null
                    && (key.typeCode() == null || key.typeCode().isBlank())
                    && (key.categoryCode() == null || key.categoryCode().isBlank())
                    && (key.exemptionReason() == null || key.exemptionReason().isBlank())) {
                return null;
            }
            return new OrderAnalysisResult.TaxSummary(
                    key.typeCode(),
                    key.categoryCode(),
                    key.rate(),
                    base,
                    tax,
                    key.exemptionReason());
        }
    }
}
