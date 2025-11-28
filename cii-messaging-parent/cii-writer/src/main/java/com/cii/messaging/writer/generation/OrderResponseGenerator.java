package com.cii.messaging.writer.generation;

import com.cii.messaging.model.order.Order;
import com.cii.messaging.model.orderresponse.OrderResponse;
import com.cii.messaging.unece.orderresponse.AmountType;
import com.cii.messaging.unece.orderresponse.CodeType;
import com.cii.messaging.unece.orderresponse.CurrencyCodeType;
import com.cii.messaging.unece.orderresponse.DateTimeType;
import com.cii.messaging.unece.orderresponse.DocumentCodeType;
import com.cii.messaging.unece.orderresponse.DocumentContextParameterType;
import com.cii.messaging.unece.orderresponse.DocumentLineDocumentType;
import com.cii.messaging.unece.orderresponse.MessageFunctionCodeType;
import com.cii.messaging.unece.orderresponse.ExchangedDocumentContextType;
import com.cii.messaging.unece.orderresponse.ExchangedDocumentType;
import com.cii.messaging.unece.orderresponse.HeaderTradeAgreementType;
import com.cii.messaging.unece.orderresponse.HeaderTradeDeliveryType;
import com.cii.messaging.unece.orderresponse.HeaderTradeSettlementType;
import com.cii.messaging.unece.orderresponse.IDType;
import com.cii.messaging.unece.orderresponse.LineTradeDeliveryType;
import com.cii.messaging.unece.orderresponse.QuantityType;
import com.cii.messaging.unece.orderresponse.ReferencedDocumentType;
import com.cii.messaging.unece.orderresponse.SupplyChainTradeLineItemType;
import com.cii.messaging.unece.orderresponse.SupplyChainTradeTransactionType;
import com.cii.messaging.unece.orderresponse.TextType;
import com.cii.messaging.unece.orderresponse.TradeAddressType;
import com.cii.messaging.unece.orderresponse.TradePartyType;
import com.cii.messaging.unece.orderresponse.TradeProductType;
import com.cii.messaging.unece.orderresponse.TradeSettlementHeaderMonetarySummationType;
import com.cii.messaging.writer.CIIWriterException;
import com.cii.messaging.writer.OrderResponseWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Transforme un message ORDERS en ORDER_RESPONSE structuré conformément aux schémas UNECE.
 */
public final class OrderResponseGenerator {

    private static final DateTimeFormatter ISSUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderResponseGenerator() {
        // utilitaire
    }

    /**
     * Génère un ORDER_RESPONSE à partir de l'ORDER fourni en appliquant les options par défaut.
     *
     * @param order commande source
     * @return réponse construite
     */
    public static OrderResponse genererDepuisOrder(Order order) {
        return genererDepuisOrder(order, OrderResponseGenerationOptions.defaults());
    }

    /**
     * Génère un ORDER_RESPONSE à partir de l'ORDER fourni.
     *
     * @param order   commande source
     * @param options options de génération (peut être {@code null} pour les valeurs par défaut)
     * @return réponse structurée prête à être sérialisée
     */
    public static OrderResponse genererDepuisOrder(Order order, OrderResponseGenerationOptions options) {
        Objects.requireNonNull(order, "order");
        OrderResponseGenerationOptions resolved = options != null ? options : OrderResponseGenerationOptions.defaults();

        String orderId = extractOrderId(order);
        LocalDateTime issueDate = resolveIssueDate(resolved);
        String responseId = resolveResponseId(orderId, resolved, issueDate);

        Mapper mapper = new Mapper(order, orderId);
        return mapper.map(responseId, issueDate, resolved);
    }

    /**
     * Génère puis écrit un ORDER_RESPONSE au format XML sur le chemin souhaité.
     *
     * @param order        commande source
     * @param cheminSortie chemin du fichier ORDER_RESPONSE à produire
     * @param options      options de génération (peut être {@code null})
     * @return message de confirmation avec le chemin absolu du fichier produit
     * @throws IOException        si l'écriture échoue ou si le chemin est invalide
     * @throws CIIWriterException si la sérialisation JAXB échoue
     */
    public static String genererOrderResponse(Order order, String cheminSortie, OrderResponseGenerationOptions options)
            throws IOException, CIIWriterException {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(cheminSortie, "cheminSortie");

        Path outputPath = Path.of(cheminSortie);
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.exists(outputPath) && Files.isDirectory(outputPath)) {
            throw new IOException("Le chemin de sortie correspond à un répertoire : " + cheminSortie);
        }

        OrderResponse response = genererDepuisOrder(order, options);
        new OrderResponseWriter().write(response, outputPath.toFile());
        return "Fichier ORDER_RESPONSE généré avec succès : " + outputPath.toAbsolutePath();
    }

    private static String extractOrderId(Order order) {
        if (order.getExchangedDocument() != null && order.getExchangedDocument().getID() != null) {
            return order.getExchangedDocument().getID().getValue();
        }
        return null;
    }

    private static LocalDateTime resolveIssueDate(OrderResponseGenerationOptions options) {
        return options.getIssueDateTime() != null ? options.getIssueDateTime() : LocalDateTime.now(options.getClock());
    }

    private static String resolveResponseId(String orderId, OrderResponseGenerationOptions options, LocalDateTime issueDate) {
        if (options.getResponseId() != null && !options.getResponseId().isBlank()) {
            return options.getResponseId().trim();
        }
        if (orderId != null && !orderId.isBlank()) {
            return options.getResponseIdPrefix() + orderId.trim();
        }
        return options.getResponseIdPrefix() + ISSUE_DATE_FORMATTER.format(issueDate);
    }

    /**
     * Réalise l'intégralité du mapping entre les structures ORDER et ORDER_RESPONSE.
     */
    private static final class Mapper {
        private final Order source;
        private final String orderId;

        private Mapper(Order source, String orderId) {
            this.source = source;
            this.orderId = orderId;
        }

        private OrderResponse map(String responseId, LocalDateTime issueDate, OrderResponseGenerationOptions options) {
            OrderResponse response = new OrderResponse();
            response.setExchangedDocumentContext(mapContext());
            response.setExchangedDocument(mapDocument(responseId, issueDate, options));
            response.setSupplyChainTradeTransaction(mapTransaction());
            return response;
        }

        private ExchangedDocumentContextType mapContext() {
            ExchangedDocumentContextType target = new ExchangedDocumentContextType();
            com.cii.messaging.unece.order.ExchangedDocumentContextType sourceContext = source.getExchangedDocumentContext();
            if (sourceContext == null) {
                return target;
            }

            target.setSpecifiedTransactionID(copyId(sourceContext.getSpecifiedTransactionID()));
            target.setTestIndicator(copyIndicator(sourceContext.getTestIndicator()));
            target.setProcessingTransactionDateTime(copyDateTime(sourceContext.getProcessingTransactionDateTime()));

            copyDocumentContextParameters(sourceContext.getBusinessProcessSpecifiedDocumentContextParameter(),
                    target.getBusinessProcessSpecifiedDocumentContextParameter());
            copyDocumentContextParameters(sourceContext.getBIMSpecifiedDocumentContextParameter(),
                    target.getBIMSpecifiedDocumentContextParameter());
            copyDocumentContextParameters(sourceContext.getScenarioSpecifiedDocumentContextParameter(),
                    target.getScenarioSpecifiedDocumentContextParameter());
            copyDocumentContextParameters(sourceContext.getApplicationSpecifiedDocumentContextParameter(),
                    target.getApplicationSpecifiedDocumentContextParameter());
            copyDocumentContextParameters(sourceContext.getGuidelineSpecifiedDocumentContextParameter(),
                    target.getGuidelineSpecifiedDocumentContextParameter());
            copyDocumentContextParameters(sourceContext.getSubsetSpecifiedDocumentContextParameter(),
                    target.getSubsetSpecifiedDocumentContextParameter());
            if (sourceContext.getMessageStandardSpecifiedDocumentContextParameter() != null) {
                target.setMessageStandardSpecifiedDocumentContextParameter(
                        copyDocumentContextParameter(sourceContext.getMessageStandardSpecifiedDocumentContextParameter()));
            }
            copyDocumentContextParameters(sourceContext.getUserSpecifiedDocumentContextParameter(),
                    target.getUserSpecifiedDocumentContextParameter());
            return target;
        }

        private ExchangedDocumentType mapDocument(String responseId, LocalDateTime issueDate,
                                                  OrderResponseGenerationOptions options) {
            ExchangedDocumentType document = new ExchangedDocumentType();
            document.setID(createId(responseId));

            DocumentCodeType typeCode = new DocumentCodeType();
            typeCode.setValue(options.getDocumentTypeCode());
            typeCode.setListAgencyID("6");
            document.setTypeCode(typeCode);

            MessageFunctionCodeType purposeCode = new MessageFunctionCodeType();
            purposeCode.setValue(options.getAcknowledgementCode());
            purposeCode.setListAgencyID("6");
            document.setPurposeCode(purposeCode);

            DateTimeType dateTime = new DateTimeType();
            DateTimeType.DateTimeString dateTimeString = new DateTimeType.DateTimeString();
            dateTimeString.setFormat("102");
            dateTimeString.setValue(ISSUE_DATE_FORMATTER.format(issueDate));
            dateTime.setDateTimeString(dateTimeString);
            document.setIssueDateTime(dateTime);

            com.cii.messaging.unece.order.ExchangedDocumentType orderDocument = source.getExchangedDocument();
            if (orderDocument != null) {
                copyTexts(orderDocument.getName(), document.getName());
                document.setPurpose(copyText(orderDocument.getPurpose()));
                copyIds(orderDocument.getLanguageID(), document.getLanguageID());
            }

            return document;
        }

        private SupplyChainTradeTransactionType mapTransaction() {
            SupplyChainTradeTransactionType transaction = new SupplyChainTradeTransactionType();
            com.cii.messaging.unece.order.SupplyChainTradeTransactionType orderTransaction =
                    source.getSupplyChainTradeTransaction();
            if (orderTransaction == null) {
                transaction.setApplicableHeaderTradeAgreement(mapHeaderTradeAgreement(null));
                transaction.setApplicableHeaderTradeDelivery(mapHeaderTradeDelivery(null));
                transaction.setApplicableHeaderTradeSettlement(mapHeaderTradeSettlement(null));
                return transaction;
            }

            transaction.setApplicableHeaderTradeAgreement(mapHeaderTradeAgreement(
                    orderTransaction.getApplicableHeaderTradeAgreement()));
            transaction.setApplicableHeaderTradeDelivery(mapHeaderTradeDelivery(
                    orderTransaction.getApplicableHeaderTradeDelivery()));
            transaction.setApplicableHeaderTradeSettlement(mapHeaderTradeSettlement(
                    orderTransaction.getApplicableHeaderTradeSettlement()));

            List<com.cii.messaging.unece.order.SupplyChainTradeLineItemType> lines =
                    orderTransaction.getIncludedSupplyChainTradeLineItem();
            if (lines != null) {
                for (com.cii.messaging.unece.order.SupplyChainTradeLineItemType line : lines) {
                    transaction.getIncludedSupplyChainTradeLineItem().add(mapLineItem(line));
                }
            }
            return transaction;
        }

        private HeaderTradeAgreementType mapHeaderTradeAgreement(
                com.cii.messaging.unece.order.HeaderTradeAgreementType sourceAgreement) {
            HeaderTradeAgreementType agreement = new HeaderTradeAgreementType();
            if (sourceAgreement != null) {
                agreement.setBuyerReference(copyText(sourceAgreement.getBuyerReference()));
                agreement.setSellerTradeParty(copyTradeParty(sourceAgreement.getSellerTradeParty()));
                agreement.setBuyerTradeParty(copyTradeParty(sourceAgreement.getBuyerTradeParty()));
            }
            if (orderId != null && !orderId.isBlank()) {
                agreement.setSellerOrderReferencedDocument(createOrderReference(orderId));
            }
            return agreement;
        }

        private HeaderTradeDeliveryType mapHeaderTradeDelivery(
                com.cii.messaging.unece.order.HeaderTradeDeliveryType sourceDelivery) {
            HeaderTradeDeliveryType delivery = new HeaderTradeDeliveryType();
            if (sourceDelivery != null) {
                delivery.setShipToTradeParty(copyTradeParty(sourceDelivery.getShipToTradeParty()));
                delivery.setUltimateShipToTradeParty(copyTradeParty(sourceDelivery.getUltimateShipToTradeParty()));
                delivery.setShipFromTradeParty(copyTradeParty(sourceDelivery.getShipFromTradeParty()));
            }
            return delivery;
        }

        private HeaderTradeSettlementType mapHeaderTradeSettlement(
                com.cii.messaging.unece.order.HeaderTradeSettlementType sourceSettlement) {
            HeaderTradeSettlementType settlement = new HeaderTradeSettlementType();
            if (sourceSettlement != null) {
                settlement.setTaxCurrencyCode(copyCurrency(sourceSettlement.getTaxCurrencyCode()));
                settlement.setOrderCurrencyCode(copyCurrency(sourceSettlement.getOrderCurrencyCode()));
                settlement.setInvoiceCurrencyCode(copyCurrency(sourceSettlement.getInvoiceCurrencyCode()));
                settlement.setPriceCurrencyCode(copyCurrency(sourceSettlement.getPriceCurrencyCode()));
                copyAmounts(sourceSettlement.getDuePayableAmount(), settlement.getDuePayableAmount());
                if (sourceSettlement.getSpecifiedTradeSettlementHeaderMonetarySummation() != null) {
                    settlement.setSpecifiedTradeSettlementHeaderMonetarySummation(
                            copyMonetarySummation(sourceSettlement.getSpecifiedTradeSettlementHeaderMonetarySummation()));
                }
            }
            if (settlement.getSpecifiedTradeSettlementHeaderMonetarySummation() == null) {
                settlement.setSpecifiedTradeSettlementHeaderMonetarySummation(new TradeSettlementHeaderMonetarySummationType());
            }
            return settlement;
        }

        private SupplyChainTradeLineItemType mapLineItem(
                com.cii.messaging.unece.order.SupplyChainTradeLineItemType sourceLine) {
            SupplyChainTradeLineItemType line = new SupplyChainTradeLineItemType();
            line.setAssociatedDocumentLineDocument(copyLineDocument(sourceLine.getAssociatedDocumentLineDocument()));
            line.setSpecifiedTradeProduct(copyTradeProduct(sourceLine.getSpecifiedTradeProduct()));
            line.setSpecifiedLineTradeAgreement(copyLineAgreement(sourceLine.getSpecifiedLineTradeAgreement()));
            line.setSpecifiedLineTradeDelivery(copyLineDelivery(sourceLine.getSpecifiedLineTradeDelivery()));
            line.setSpecifiedLineTradeSettlement(copyLineSettlement(sourceLine.getSpecifiedLineTradeSettlement()));
            return line;
        }

        private DocumentLineDocumentType copyLineDocument(
                com.cii.messaging.unece.order.DocumentLineDocumentType sourceLineDoc) {
            DocumentLineDocumentType target = new DocumentLineDocumentType();
            if (sourceLineDoc != null) {
                target.setLineID(copyId(sourceLineDoc.getLineID()));
            }
            return target;
        }

        private TradeProductType copyTradeProduct(com.cii.messaging.unece.order.TradeProductType sourceProduct) {
            if (sourceProduct == null) {
                return null;
            }
            TradeProductType product = new TradeProductType();
            product.setID(copyId(sourceProduct.getID()));
            copyIds(sourceProduct.getGlobalID(), product.getGlobalID());
            product.setSellerAssignedID(copyId(sourceProduct.getSellerAssignedID()));
            product.setBuyerAssignedID(copyId(sourceProduct.getBuyerAssignedID()));
            copyTexts(sourceProduct.getName(), product.getName());
            product.setTradeName(copyText(sourceProduct.getTradeName()));
            return product;
        }

        private LineTradeDeliveryType copyLineDelivery(
                com.cii.messaging.unece.order.LineTradeDeliveryType sourceDelivery) {
            LineTradeDeliveryType delivery = new LineTradeDeliveryType();
            if (sourceDelivery != null) {
                delivery.setRequestedQuantity(copyQuantity(sourceDelivery.getRequestedQuantity()));
                QuantityType agreed = copyQuantity(sourceDelivery.getAgreedQuantity());
                if (agreed == null) {
                    agreed = copyQuantity(sourceDelivery.getRequestedQuantity());
                }
                delivery.setAgreedQuantity(agreed);
                delivery.setShipToTradeParty(copyTradeParty(sourceDelivery.getShipToTradeParty()));
                delivery.setShipFromTradeParty(copyTradeParty(sourceDelivery.getShipFromTradeParty()));
            }
            return delivery;
        }

        private ReferencedDocumentType createOrderReference(String orderIdentifier) {
            ReferencedDocumentType reference = new ReferencedDocumentType();
            IDType issuer = createId(orderIdentifier);
            reference.setIssuerAssignedID(issuer);
            return reference;
        }

        private TradePartyType copyTradeParty(com.cii.messaging.unece.order.TradePartyType sourceParty) {
            if (sourceParty == null) {
                return null;
            }
            TradePartyType party = new TradePartyType();
            copyIds(sourceParty.getID(), party.getID());
            copyIds(sourceParty.getGlobalID(), party.getGlobalID());
            party.setName(copyText(sourceParty.getName()));
            party.setPostalTradeAddress(copyTradeAddress(sourceParty.getPostalTradeAddress()));
            return party;
        }

        private TradeAddressType copyTradeAddress(com.cii.messaging.unece.order.TradeAddressType sourceAddress) {
            if (sourceAddress == null) {
                return null;
            }
            TradeAddressType address = new TradeAddressType();
            address.setID(copyId(sourceAddress.getID()));
            address.setPostcodeCode(copyCode(sourceAddress.getPostcodeCode()));
            address.setLineOne(copyText(sourceAddress.getLineOne()));
            address.setLineTwo(copyText(sourceAddress.getLineTwo()));
            address.setLineThree(copyText(sourceAddress.getLineThree()));
            address.setCityName(copyText(sourceAddress.getCityName()));
            address.setCountryID(copyCountryId(sourceAddress.getCountryID()));
            address.setCountrySubDivisionID(copyId(sourceAddress.getCountrySubDivisionID()));
            copyTexts(sourceAddress.getCountryName(), address.getCountryName());
            copyTexts(sourceAddress.getCountrySubDivisionName(), address.getCountrySubDivisionName());
            return address;
        }

        private TradeSettlementHeaderMonetarySummationType copyMonetarySummation(
                com.cii.messaging.unece.order.TradeSettlementHeaderMonetarySummationType sourceSummation) {
            TradeSettlementHeaderMonetarySummationType target = new TradeSettlementHeaderMonetarySummationType();
            if (sourceSummation == null) {
                return target;
            }
            copyAmounts(sourceSummation.getLineTotalAmount(), target.getLineTotalAmount());
            copyAmounts(sourceSummation.getChargeTotalAmount(), target.getChargeTotalAmount());
            copyAmounts(sourceSummation.getAllowanceTotalAmount(), target.getAllowanceTotalAmount());
            copyAmounts(sourceSummation.getTaxBasisTotalAmount(), target.getTaxBasisTotalAmount());
            copyAmounts(sourceSummation.getTaxTotalAmount(), target.getTaxTotalAmount());
            copyAmounts(sourceSummation.getRoundingAmount(), target.getRoundingAmount());
            copyAmounts(sourceSummation.getGrandTotalAmount(), target.getGrandTotalAmount());
            copyAmounts(sourceSummation.getTotalPrepaidAmount(), target.getTotalPrepaidAmount());
            copyAmounts(sourceSummation.getDuePayableAmount(), target.getDuePayableAmount());
            copyAmounts(sourceSummation.getNetLineTotalAmount(), target.getNetLineTotalAmount());
            copyAmounts(sourceSummation.getIncludingTaxesLineTotalAmount(), target.getIncludingTaxesLineTotalAmount());
            return target;
        }

        private void copyAmounts(List<com.cii.messaging.unece.order.AmountType> sources, List<AmountType> targets) {
            if (sources == null) {
                return;
            }
            for (com.cii.messaging.unece.order.AmountType amount : sources) {
                AmountType converted = copyAmount(amount);
                if (converted != null) {
                    targets.add(converted);
                }
            }
        }

        private void copyIds(List<com.cii.messaging.unece.order.IDType> sources, List<IDType> targets) {
            if (sources == null) {
                return;
            }
            for (com.cii.messaging.unece.order.IDType id : sources) {
                IDType converted = copyId(id);
                if (converted != null) {
                    targets.add(converted);
                }
            }
        }

        private void copyTexts(List<com.cii.messaging.unece.order.TextType> sources, List<TextType> targets) {
            if (sources == null) {
                return;
            }
            for (com.cii.messaging.unece.order.TextType text : sources) {
                TextType converted = copyText(text);
                if (converted != null) {
                    targets.add(converted);
                }
            }
        }

        private void copyDocumentContextParameters(
                List<com.cii.messaging.unece.order.DocumentContextParameterType> sources,
                List<DocumentContextParameterType> targets) {
            if (sources == null) {
                return;
            }
            for (com.cii.messaging.unece.order.DocumentContextParameterType parameter : sources) {
                DocumentContextParameterType converted = copyDocumentContextParameter(parameter);
                if (converted != null) {
                    targets.add(converted);
                }
            }
        }

        private DocumentContextParameterType copyDocumentContextParameter(
                com.cii.messaging.unece.order.DocumentContextParameterType sourceParameter) {
            if (sourceParameter == null) {
                return null;
            }
            DocumentContextParameterType parameter = new DocumentContextParameterType();
            parameter.setID(copyId(sourceParameter.getID()));
            parameter.setValue(copyText(sourceParameter.getValue()));
            if (sourceParameter.getSpecifiedDocumentVersion() != null) {
                parameter.setSpecifiedDocumentVersion(copyDocumentVersion(sourceParameter.getSpecifiedDocumentVersion()));
            }
            return parameter;
        }

        private com.cii.messaging.unece.orderresponse.DocumentVersionType copyDocumentVersion(
                com.cii.messaging.unece.order.DocumentVersionType sourceVersion) {
            if (sourceVersion == null) {
                return null;
            }
            com.cii.messaging.unece.orderresponse.DocumentVersionType version =
                    new com.cii.messaging.unece.orderresponse.DocumentVersionType();
            version.setID(copyId(sourceVersion.getID()));
            version.setName(copyText(sourceVersion.getName()));
            version.setIssueDateTime(copyDateTime(sourceVersion.getIssueDateTime()));
            return version;
        }

        private IDType copyId(com.cii.messaging.unece.order.IDType sourceId) {
            if (sourceId == null) {
                return null;
            }
            IDType id = new IDType();
            id.setValue(sourceId.getValue());
            id.setSchemeID(sourceId.getSchemeID());
            id.setSchemeName(sourceId.getSchemeName());
            id.setSchemeAgencyID(sourceId.getSchemeAgencyID());
            id.setSchemeAgencyName(sourceId.getSchemeAgencyName());
            id.setSchemeVersionID(sourceId.getSchemeVersionID());
            id.setSchemeDataURI(sourceId.getSchemeDataURI());
            id.setSchemeURI(sourceId.getSchemeURI());
            return id;
        }

        private IDType createId(String value) {
            if (value == null) {
                return null;
            }
            IDType id = new IDType();
            id.setValue(value);
            return id;
        }

        private TextType copyText(com.cii.messaging.unece.order.TextType sourceText) {
            if (sourceText == null) {
                return null;
            }
            TextType text = new TextType();
            text.setValue(sourceText.getValue());
            text.setLanguageID(sourceText.getLanguageID());
            text.setLanguageLocaleID(sourceText.getLanguageLocaleID());
            return text;
        }

        private CodeType copyCode(com.cii.messaging.unece.order.CodeType sourceCode) {
            if (sourceCode == null) {
                return null;
            }
            CodeType code = new CodeType();
            code.setValue(sourceCode.getValue());
            code.setListID(sourceCode.getListID());
            code.setListAgencyID(sourceCode.getListAgencyID());
            code.setListAgencyName(sourceCode.getListAgencyName());
            code.setListVersionID(sourceCode.getListVersionID());
            code.setName(sourceCode.getName());
            code.setListName(sourceCode.getListName());
            code.setLanguageID(sourceCode.getLanguageID());
            code.setListURI(sourceCode.getListURI());
            code.setListSchemeURI(sourceCode.getListSchemeURI());
            return code;
        }

        private QuantityType copyQuantity(com.cii.messaging.unece.order.QuantityType sourceQuantity) {
            if (sourceQuantity == null) {
                return null;
            }
            QuantityType quantity = new QuantityType();
            quantity.setValue(sourceQuantity.getValue());
            quantity.setUnitCode(sourceQuantity.getUnitCode());
            quantity.setUnitCodeListID(sourceQuantity.getUnitCodeListID());
            quantity.setUnitCodeListAgencyID(sourceQuantity.getUnitCodeListAgencyID());
            quantity.setUnitCodeListAgencyName(sourceQuantity.getUnitCodeListAgencyName());
            return quantity;
        }

        private AmountType copyAmount(com.cii.messaging.unece.order.AmountType sourceAmount) {
            if (sourceAmount == null) {
                return null;
            }
            AmountType amount = new AmountType();
            amount.setValue(sourceAmount.getValue());
            amount.setCurrencyID(sourceAmount.getCurrencyID());
            amount.setCurrencyCodeListVersionID(sourceAmount.getCurrencyCodeListVersionID());
            return amount;
        }

        private CurrencyCodeType copyCurrency(com.cii.messaging.unece.order.CurrencyCodeType sourceCurrency) {
            if (sourceCurrency == null) {
                return null;
            }
            CurrencyCodeType currency = new CurrencyCodeType();
            if (sourceCurrency.getValue() != null) {
                currency.setValue(com.cii.messaging.unece.orderresponse.ISO3AlphaCurrencyCodeContentType
                        .valueOf(sourceCurrency.getValue().name()));
            }
            currency.setListAgencyID(sourceCurrency.getListAgencyID());
            return currency;
        }

        private com.cii.messaging.unece.orderresponse.CountryIDType copyCountryId(
                com.cii.messaging.unece.order.CountryIDType sourceCountry) {
            if (sourceCountry == null) {
                return null;
            }
            com.cii.messaging.unece.orderresponse.CountryIDType country =
                    new com.cii.messaging.unece.orderresponse.CountryIDType();
            if (sourceCountry.getValue() != null) {
                country.setValue(com.cii.messaging.unece.orderresponse.ISOTwoletterCountryCodeContentType
                        .valueOf(sourceCountry.getValue().name()));
            }
            country.setSchemeAgencyID(sourceCountry.getSchemeAgencyID());
            return country;
        }

        private DateTimeType copyDateTime(com.cii.messaging.unece.order.DateTimeType sourceDate) {
            if (sourceDate == null) {
                return null;
            }
            DateTimeType dateTime = new DateTimeType();
            if (sourceDate.getDateTimeString() != null) {
                DateTimeType.DateTimeString targetString = new DateTimeType.DateTimeString();
                targetString.setFormat(sourceDate.getDateTimeString().getFormat());
                targetString.setValue(sourceDate.getDateTimeString().getValue());
                dateTime.setDateTimeString(targetString);
            }
            if (sourceDate.getDateTime() != null) {
                dateTime.setDateTime(sourceDate.getDateTime());
            }
            return dateTime;
        }

        private com.cii.messaging.unece.orderresponse.IndicatorType copyIndicator(
                com.cii.messaging.unece.order.IndicatorType sourceIndicator) {
            if (sourceIndicator == null) {
                return null;
            }
            com.cii.messaging.unece.orderresponse.IndicatorType indicator =
                    new com.cii.messaging.unece.orderresponse.IndicatorType();
            if (sourceIndicator.getIndicatorString() != null) {
                com.cii.messaging.unece.orderresponse.IndicatorType.IndicatorString string =
                        new com.cii.messaging.unece.orderresponse.IndicatorType.IndicatorString();
                string.setFormat(sourceIndicator.getIndicatorString().getFormat());
                string.setValue(sourceIndicator.getIndicatorString().getValue());
                indicator.setIndicatorString(string);
            }
            indicator.setIndicator(sourceIndicator.isIndicator());
            return indicator;
        }

        private com.cii.messaging.unece.orderresponse.LineTradeAgreementType copyLineAgreement(
                com.cii.messaging.unece.order.LineTradeAgreementType sourceAgreement) {
            return convertStructure(sourceAgreement,
                    com.cii.messaging.unece.orderresponse.LineTradeAgreementType.class);
        }

        private com.cii.messaging.unece.orderresponse.LineTradeSettlementType copyLineSettlement(
                com.cii.messaging.unece.order.LineTradeSettlementType sourceSettlement) {
            return convertStructure(sourceSettlement,
                    com.cii.messaging.unece.orderresponse.LineTradeSettlementType.class);
        }

        private <S, T> T convertStructure(S source, Class<T> targetType) {
            if (source == null) {
                return null;
            }
            Object converted = convertValue(source, targetType);
            return targetType.cast(converted);
        }

        private void copyOrderBean(Object source, Object target) {
            Class<?> sourceClass = source.getClass();
            Class<?> targetClass = target.getClass();
            for (Method getter : sourceClass.getMethods()) {
                if (!isGetter(getter)) {
                    continue;
                }
                try {
                    Object value = getter.invoke(source);
                    if (value == null) {
                        continue;
                    }
                    if (List.class.isAssignableFrom(getter.getReturnType())) {
                        @SuppressWarnings("unchecked")
                        List<Object> sourceList = (List<Object>) value;
                        Method targetGetter = targetClass.getMethod(getter.getName());
                        @SuppressWarnings("unchecked")
                        List<Object> targetList = (List<Object>) targetGetter.invoke(target);
                        if (targetList == null) {
                            Method setter = findSetter(targetClass, getter.getName().startsWith("is")
                                    ? "set" + getter.getName().substring(2)
                                    : "set" + getter.getName().substring(3));
                            if (setter == null) {
                                throw new IllegalStateException(
                                        "Impossible d'initialiser la collection " + getter.getName()
                                                + " sur " + targetClass.getName());
                            }
                            targetList = new ArrayList<>();
                            setter.invoke(target, targetList);
                        } else {
                            targetList.clear();
                        }
                        for (Object element : sourceList) {
                            Object convertedElement = convertListItem(element);
                            if (convertedElement != null) {
                                targetList.add(convertedElement);
                            }
                        }
                    } else {
                        String setterName = getter.getName().startsWith("is")
                                ? "set" + getter.getName().substring(2)
                                : "set" + getter.getName().substring(3);
                        Method setter = findSetter(targetClass, setterName);
                        if (setter == null) {
                            continue;
                        }
                        Class<?> parameterType = setter.getParameterTypes()[0];
                        Object convertedValue = convertValue(value, parameterType);
                        if (convertedValue != null || !parameterType.isPrimitive()) {
                            setter.invoke(target, convertedValue);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalStateException("Impossible de copier la structure " + sourceClass.getName(), e);
                }
            }
        }

        private boolean isOrderPackage(Class<?> clazz) {
            Package pkg = clazz.getPackage();
            return pkg != null && pkg.getName().startsWith("com.cii.messaging.unece.order");
        }

        private Method findSetter(Class<?> targetClass, String name) {
            for (Method method : targetClass.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 1) {
                    return method;
                }
            }
            return null;
        }

        private boolean isGetter(Method method) {
            if (method.getParameterCount() != 0) {
                return false;
            }
            String name = method.getName();
            return (name.startsWith("get") && name.length() > 3 && !name.equals("getClass"))
                    || (name.startsWith("is") && name.length() > 2);
        }

        private Object convertValue(Object value, Class<?> targetType) {
            if (value == null) {
                return null;
            }
            Class<?> sourceClass = value.getClass();
            if (targetType.isPrimitive()) {
                targetType = primitiveToWrapper(targetType);
            }
            if (targetType.isAssignableFrom(sourceClass)) {
                return value;
            }
            if (targetType.isEnum() && value instanceof Enum<?>) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum> enumClass = (Class<? extends Enum>) targetType;
                return Enum.valueOf(enumClass, ((Enum<?>) value).name());
            }
            if (isOrderPackage(sourceClass)) {
                Class<?> resolvedTarget = targetType == Object.class || !isOrderPackage(targetType)
                        ? resolveOrderResponseClass(sourceClass)
                        : targetType;
                Object targetInstance = instantiate(resolvedTarget);
                copyOrderBean(value, targetInstance);
                return targetInstance;
            }
            return value;
        }

        private Object convertListItem(Object element) {
            if (element == null) {
                return null;
            }
            Class<?> elementClass = element.getClass();
            if (element instanceof Enum<?>) {
                Class<?> targetEnum = resolveOrderResponseClass(elementClass);
                @SuppressWarnings("unchecked")
                Class<? extends Enum> enumClass = (Class<? extends Enum>) targetEnum;
                return Enum.valueOf(enumClass, ((Enum<?>) element).name());
            }
            if (isOrderPackage(elementClass)) {
                Class<?> targetClass = resolveOrderResponseClass(elementClass);
                Object targetInstance = instantiate(targetClass);
                copyOrderBean(element, targetInstance);
                return targetInstance;
            }
            return element;
        }

        private Class<?> resolveOrderResponseClass(Class<?> orderClass) {
            String targetName = orderClass.getName().replace("com.cii.messaging.unece.order",
                    "com.cii.messaging.unece.orderresponse");
            try {
                return Class.forName(targetName);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Classe cible introuvable pour la conversion : " + targetName, e);
            }
        }

        private Object instantiate(Class<?> clazz) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Impossible d'instancier la classe " + clazz.getName(), e);
            }
        }

        private Class<?> primitiveToWrapper(Class<?> primitive) {
            if (primitive == boolean.class) {
                return Boolean.class;
            }
            if (primitive == byte.class) {
                return Byte.class;
            }
            if (primitive == short.class) {
                return Short.class;
            }
            if (primitive == int.class) {
                return Integer.class;
            }
            if (primitive == long.class) {
                return Long.class;
            }
            if (primitive == float.class) {
                return Float.class;
            }
            if (primitive == double.class) {
                return Double.class;
            }
            if (primitive == char.class) {
                return Character.class;
            }
            return primitive;
        }
    }
}
