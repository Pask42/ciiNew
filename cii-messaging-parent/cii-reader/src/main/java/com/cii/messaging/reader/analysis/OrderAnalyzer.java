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
import java.util.List;
import java.util.Objects;

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
        String buyerName = null;
        String buyerId = null;
        String buyerReference = null;
        String sellerName = null;
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
                TradePartyType buyer = agreement.getBuyerTradeParty();
                if (buyer != null) {
                    buyerName = safeValue(buyer.getName());
                    buyerId = firstIdValue(buyer.getID());
                }
                TradePartyType seller = agreement.getSellerTradeParty();
                if (seller != null) {
                    sellerName = safeValue(seller.getName());
                }
            }

            HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
            if (settlement != null) {
                currency = safeCurrency(settlement.getOrderCurrencyCode());
            }

            List<SupplyChainTradeLineItemType> lineItems = transaction.getIncludedSupplyChainTradeLineItem();
            for (SupplyChainTradeLineItemType lineItem : lineItems) {
                lines.add(mapLine(lineItem, currency));
            }
        }

        int lineCount = lines.size();
        return new OrderAnalysisResult(orderId, issueDate, buyerName, buyerId, buyerReference, sellerName, currency, lineCount, lines);
    }

    private static OrderAnalysisResult.OrderLineSummary mapLine(SupplyChainTradeLineItemType lineItem, String defaultCurrency) {
        String lineId = null;
        String productIdentifier = null;
        String productName = null;
        BigDecimal quantityValue = null;
        String quantityUnit = null;
        BigDecimal netPriceValue = null;
        String netPriceCurrency = null;
        BigDecimal lineTotalValue = null;
        String lineCurrency = null;

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
        if (lineAgreement != null && lineAgreement.getNetPriceProductTradePrice() != null) {
            TradePriceType price = lineAgreement.getNetPriceProductTradePrice();
            AmountType netAmount = firstAmount(price.getChargeAmount());
            if (netAmount != null) {
                netPriceValue = netAmount.getValue();
                netPriceCurrency = netAmount.getCurrencyID();
            }
        }

        LineTradeSettlementType lineSettlement = lineItem.getSpecifiedLineTradeSettlement();
        if (lineSettlement != null) {
            TradeSettlementLineMonetarySummationType summary = lineSettlement.getSpecifiedTradeSettlementLineMonetarySummation();
            if (summary != null) {
                AmountType totalAmount = firstAmount(summary.getLineTotalAmount());
                if (totalAmount == null) {
                    totalAmount = firstAmount(summary.getNetLineTotalAmount());
                }
                if (totalAmount != null) {
                    lineTotalValue = totalAmount.getValue();
                    lineCurrency = totalAmount.getCurrencyID();
                }
            }
        }

        if (lineCurrency == null) {
            lineCurrency = defaultCurrency;
        }

        return new OrderAnalysisResult.OrderLineSummary(
                lineId,
                productIdentifier,
                productName,
                quantityValue,
                quantityUnit,
                netPriceValue,
                netPriceCurrency,
                lineTotalValue,
                lineCurrency
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
