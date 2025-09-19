package com.cii.messaging.reader.analysis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Représente une vue synthétique d'une commande CII et de ses lignes.
 */
public final class OrderAnalysisResult {

    private final String orderId;
    private final String issueDate;
    private final String buyerReference;
    private final PartySummary orderingCustomer;
    private final PartySummary buyer;
    private final PartySummary invoicee;
    private final PartySummary payer;
    private final PartySummary seller;
    private final PartySummary shipTo;
    private final String currency;
    private final int lineCount;
    private final MonetaryAmount orderNetTotal;
    private final MonetaryAmount orderTaxTotal;
    private final MonetaryAmount orderGrossTotal;
    private final List<TaxSummary> orderTaxes;
    private final List<OrderLineSummary> lines;

    public OrderAnalysisResult(
            String orderId,
            String issueDate,
            String buyerReference,
            PartySummary orderingCustomer,
            PartySummary buyer,
            PartySummary invoicee,
            PartySummary payer,
            PartySummary seller,
            PartySummary shipTo,
            String currency,
            int lineCount,
            MonetaryAmount orderNetTotal,
            MonetaryAmount orderTaxTotal,
            MonetaryAmount orderGrossTotal,
            List<TaxSummary> orderTaxes,
            List<OrderLineSummary> lines) {
        this.orderId = orderId;
        this.issueDate = issueDate;
        this.buyerReference = buyerReference;
        this.orderingCustomer = orderingCustomer;
        this.buyer = buyer;
        this.invoicee = invoicee;
        this.payer = payer;
        this.seller = seller;
        this.shipTo = shipTo;
        this.currency = currency;
        this.lineCount = lineCount;
        this.orderNetTotal = orderNetTotal;
        this.orderTaxTotal = orderTaxTotal;
        this.orderGrossTotal = orderGrossTotal;
        List<TaxSummary> taxes = orderTaxes != null ? orderTaxes : List.of();
        this.orderTaxes = Collections.unmodifiableList(new ArrayList<>(taxes));
        this.lines = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(lines, "lines")));
    }

    public String getOrderId() {
        return orderId;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public String getBuyerReference() {
        return buyerReference;
    }

    public PartySummary getOrderingCustomer() {
        return orderingCustomer;
    }

    public PartySummary getBuyer() {
        return buyer;
    }
  
    public PartySummary getInvoicee() {
        return invoicee;
    }

    public PartySummary getPayer() {
        return payer;
    }

    public PartySummary getSeller() {
        return seller;
    }

    public PartySummary getShipTo() {
        return shipTo;
    }

    public String getBuyerName() {
        return buyer != null ? buyer.getName() : null;
    }

    public String getBuyerIdentifier() {
        return buyer != null ? buyer.getIdentifier() : null;
    }

    public String getSellerName() {
        return seller != null ? seller.getName() : null;
    }

    public String getCurrency() {
        return currency;
    }

    public int getLineCount() {
        return lineCount;
    }

    public MonetaryAmount getOrderNetTotal() {
        return orderNetTotal;
    }

    public MonetaryAmount getOrderTaxTotal() {
        return orderTaxTotal;
    }

    public MonetaryAmount getOrderGrossTotal() {
        return orderGrossTotal;
    }

    public List<TaxSummary> getOrderTaxes() {
        return orderTaxes;
    }

    public List<OrderLineSummary> getLines() {
        return lines;
    }

    /**
     * Retourne un message multi-lignes prêt à afficher décrivant la commande.
     */
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Commande");
        if (orderId != null && !orderId.isBlank()) {
            sb.append(' ').append(orderId);
        }
        sb.append('\n');

        if (issueDate != null && !issueDate.isBlank()) {
            sb.append("  Date d'émission : ").append(issueDate).append('\n');
        }
        if (buyerReference != null && !buyerReference.isBlank()) {
            sb.append("  Référence acheteur : ").append(buyerReference).append('\n');
        }
        appendParty(sb, "Client commandeur", orderingCustomer);
        appendParty(sb, "Acheteur", buyer);
        appendParty(sb, "Facturé (invoicee)", invoicee);
        if (!isSameParty(invoicee, payer)) {
            appendParty(sb, "Payeur", payer);
        }
        appendParty(sb, "Fournisseur", seller);
        appendParty(sb, "Lieu de livraison", shipTo);

        if (currency != null && !currency.isBlank()) {
            sb.append("  Devise : ").append(currency).append('\n');
        }

        boolean hasFinancialSummary = (orderNetTotal != null && orderNetTotal.getAmount() != null)
                || (orderTaxTotal != null && orderTaxTotal.getAmount() != null)
                || (orderGrossTotal != null && orderGrossTotal.getAmount() != null)
                || !orderTaxes.isEmpty();
        if (hasFinancialSummary) {
            sb.append("  Synthèse montants :\n");
            appendLabeledAmount(sb, "    • ", "Total HT", orderNetTotal);
            appendLabeledAmount(sb, "    • ", "Total TVA", orderTaxTotal);
            if (!orderTaxes.isEmpty()) {
                sb.append("    • Détail TVA :\n");
                appendTaxDetails(sb, "      ", orderTaxes);
            }
            appendLabeledAmount(sb, "    • ", "Total TTC", orderGrossTotal);
        }

        sb.append("  Nombre de lignes : ").append(lineCount).append('\n');

        for (OrderLineSummary line : lines) {
            if (line == null) {
                continue;
            }
            sb.append("    - Ligne ");
            if (line.getLineId() != null && !line.getLineId().isBlank()) {
                sb.append(line.getLineId());
            } else {
                sb.append('?');
            }
            sb.append(" : ");
            if (line.getProductName() != null && !line.getProductName().isBlank()) {
                sb.append(line.getProductName());
            } else if (line.getProductIdentifier() != null && !line.getProductIdentifier().isBlank()) {
                sb.append(line.getProductIdentifier());
            } else {
                sb.append("Article sans nom");
            }
            sb.append('\n');

            if (line.getQuantity() != null) {
                sb.append("      Quantité : ").append(formatNumber(line.getQuantity()));
                if (line.getQuantityUnit() != null && !line.getQuantityUnit().isBlank()) {
                    sb.append(' ').append(line.getQuantityUnit());
                }
                sb.append('\n');
            }
            appendLabeledAmount(sb, "      ", "Prix unitaire HT", line.getNetUnitPrice());
            appendLabeledAmount(sb, "      ", "Prix unitaire TTC", line.getGrossUnitPrice());
            appendLabeledAmount(sb, "      ", "Total ligne HT", line.getLineNetAmount());
            appendLabeledAmount(sb, "      ", "Total TVA", line.getLineTaxAmount());
            if (!line.getTaxes().isEmpty()) {
                sb.append("      TVA :\n");
                appendTaxDetails(sb, "        ", line.getTaxes());
            }
            appendLabeledAmount(sb, "      ", "Total ligne TTC", line.getLineGrossAmount());
        }
        return sb.toString();
    }

    private static void appendParty(StringBuilder sb, String label, PartySummary party) {
        if (party == null || party.isEmpty()) {
            return;
        }
        sb.append("  ").append(label).append(" : ");
        boolean hasName = party.getName() != null && !party.getName().isBlank();
        if (hasName) {
            sb.append(party.getName());
        }
        List<String> details = new ArrayList<>();
        if (party.getIdentifier() != null && !party.getIdentifier().isBlank()) {
            details.add("ID : " + party.getIdentifier());
        }
        if (party.getGlobalIdentifier() != null && !party.getGlobalIdentifier().isBlank()) {
            details.add("GLN : " + party.getGlobalIdentifier());
        }
        if (!details.isEmpty()) {
            if (hasName) {
                sb.append(" (").append(String.join(", ", details)).append(')');
            } else {
                sb.append(String.join(", ", details));
            }
        }
        sb.append('\n');
    }

    private static boolean isSameParty(PartySummary left, PartySummary right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getName(), right.getName())
                && Objects.equals(left.getIdentifier(), right.getIdentifier())
                && Objects.equals(left.getGlobalIdentifier(), right.getGlobalIdentifier());
    }

    private static void appendLabeledAmount(StringBuilder sb, String indent, String label, MonetaryAmount amount) {
        if (amount == null || amount.getAmount() == null) {
            return;
        }
        sb.append(indent).append(label).append(" : ").append(formatAmount(amount)).append('\n');
    }

    private static void appendTaxDetails(StringBuilder sb, String indent, List<TaxSummary> taxes) {
        for (TaxSummary tax : taxes) {
            if (tax == null) {
                continue;
            }
            List<String> parts = new ArrayList<>();
            if (tax.getRatePercent() != null) {
                parts.add("Taux " + formatRate(tax.getRatePercent()));
            }
            if (tax.getBaseAmount() != null && tax.getBaseAmount().getAmount() != null) {
                parts.add("Base : " + formatAmount(tax.getBaseAmount()));
            }
            if (tax.getTaxAmount() != null && tax.getTaxAmount().getAmount() != null) {
                parts.add("Montant : " + formatAmount(tax.getTaxAmount()));
            }
            StringJoiner metadata = new StringJoiner(", ");
            if (tax.getTypeCode() != null && !tax.getTypeCode().isBlank()) {
                metadata.add("Type : " + tax.getTypeCode());
            }
            if (tax.getCategoryCode() != null && !tax.getCategoryCode().isBlank()) {
                metadata.add("Catégorie : " + tax.getCategoryCode());
            }
            if (tax.getExemptionReason() != null && !tax.getExemptionReason().isBlank()) {
                metadata.add("Motif : " + tax.getExemptionReason());
            }
            sb.append(indent).append("- ");
            if (!parts.isEmpty()) {
                sb.append(String.join(" — ", parts));
            } else {
                sb.append("Détail TVA indisponible");
            }
            if (metadata.length() > 0) {
                sb.append(" [").append(metadata).append(']');
            }
            sb.append('\n');
        }
    }

    private static String formatAmount(MonetaryAmount amount) {
        if (amount == null || amount.getAmount() == null) {
            return "?";
        }
        BigDecimal value = amount.getAmount().stripTrailingZeros();
        String text = value.scale() <= 0 ? value.toPlainString() : value.toPlainString();
        if (amount.getCurrency() != null && !amount.getCurrency().isBlank()) {
            text += " " + amount.getCurrency();
        }
        return text;
    }

    private static String formatRate(BigDecimal rate) {
        BigDecimal normalized = rate.stripTrailingZeros();
        return (normalized.scale() <= 0 ? normalized.toPlainString() : normalized.toPlainString()) + "%";
    }

    private static String formatNumber(BigDecimal number) {
        BigDecimal normalized = number.stripTrailingZeros();
        return normalized.scale() <= 0 ? normalized.toPlainString() : normalized.toPlainString();
    }

    @Override
    public String toString() {
        return toPrettyString();
    }

    /**
     * Résumé d'une ligne de commande.
     */
    public static final class OrderLineSummary {
        private final String lineId;
        private final String productIdentifier;
        private final String productName;
        private final BigDecimal quantity;
        private final String quantityUnit;
        private final MonetaryAmount netUnitPrice;
        private final MonetaryAmount grossUnitPrice;
        private final MonetaryAmount lineNetAmount;
        private final MonetaryAmount lineGrossAmount;
        private final MonetaryAmount lineTaxAmount;
        private final List<TaxSummary> taxes;

        public OrderLineSummary(
                String lineId,
                String productIdentifier,
                String productName,
                BigDecimal quantity,
                String quantityUnit,
                MonetaryAmount netUnitPrice,
                MonetaryAmount grossUnitPrice,
                MonetaryAmount lineNetAmount,
                MonetaryAmount lineGrossAmount,
                MonetaryAmount lineTaxAmount,
                List<TaxSummary> taxes) {
            this.lineId = lineId;
            this.productIdentifier = productIdentifier;
            this.productName = productName;
            this.quantity = quantity;
            this.quantityUnit = quantityUnit;
            this.netUnitPrice = netUnitPrice;
            this.grossUnitPrice = grossUnitPrice;
            this.lineNetAmount = lineNetAmount;
            this.lineGrossAmount = lineGrossAmount;
            this.lineTaxAmount = lineTaxAmount;
            List<TaxSummary> taxDetails = taxes != null ? taxes : List.of();
            this.taxes = Collections.unmodifiableList(new ArrayList<>(taxDetails));
        }

        public String getLineId() {
            return lineId;
        }

        public String getProductIdentifier() {
            return productIdentifier;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public String getQuantityUnit() {
            return quantityUnit;
        }

        public MonetaryAmount getNetUnitPrice() {
            return netUnitPrice;
        }

        public MonetaryAmount getGrossUnitPrice() {
            return grossUnitPrice;
        }

        public MonetaryAmount getLineNetAmount() {
            return lineNetAmount;
        }

        public MonetaryAmount getLineGrossAmount() {
            return lineGrossAmount;
        }

        public MonetaryAmount getLineTaxAmount() {
            return lineTaxAmount;
        }

        public List<TaxSummary> getTaxes() {
            return taxes;
        }

        /**
         * @deprecated Utiliser {@link #getNetUnitPrice()}.
         */
        @Deprecated
        public BigDecimal getNetPrice() {
            return netUnitPrice != null ? netUnitPrice.getAmount() : null;
        }

        /**
         * @deprecated Utiliser {@link #getNetUnitPrice()}.
         */
        @Deprecated
        public String getNetPriceCurrency() {
            return netUnitPrice != null ? netUnitPrice.getCurrency() : null;
        }

        /**
         * @deprecated Utiliser {@link #getLineGrossAmount()} ou {@link #getLineNetAmount()}.
         */
        @Deprecated
        public BigDecimal getLineTotal() {
            return lineGrossAmount != null ? lineGrossAmount.getAmount() : null;
        }

        /**
         * @deprecated Utiliser {@link #getLineGrossAmount()}.
         */
        @Deprecated
        public String getLineTotalCurrency() {
            return lineGrossAmount != null ? lineGrossAmount.getCurrency() : null;
        }
    }

    /**
     * Représente un montant avec sa devise.
     */
    public static final class MonetaryAmount {
        private final BigDecimal amount;
        private final String currency;

        public MonetaryAmount(BigDecimal amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }
    }

    /**
     * Détail d'une taxe (TVA).
     */
    public static final class TaxSummary {
        private final String typeCode;
        private final String categoryCode;
        private final BigDecimal ratePercent;
        private final MonetaryAmount baseAmount;
        private final MonetaryAmount taxAmount;
        private final String exemptionReason;

        public TaxSummary(
                String typeCode,
                String categoryCode,
                BigDecimal ratePercent,
                MonetaryAmount baseAmount,
                MonetaryAmount taxAmount,
                String exemptionReason) {
            this.typeCode = typeCode;
            this.categoryCode = categoryCode;
            this.ratePercent = ratePercent;
            this.baseAmount = baseAmount;
            this.taxAmount = taxAmount;
            this.exemptionReason = exemptionReason;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public String getCategoryCode() {
            return categoryCode;
        }

        public BigDecimal getRatePercent() {
            return ratePercent;
        }

        public MonetaryAmount getBaseAmount() {
            return baseAmount;
        }

        public MonetaryAmount getTaxAmount() {
            return taxAmount;
        }

        public String getExemptionReason() {
            return exemptionReason;
        }
    }

    /**
     * Résumé d'une partie prenante (client, fournisseur, payeur...).
     */
    public static final class PartySummary {
        private final String name;
        private final String identifier;
        private final String globalIdentifier;

        public PartySummary(String name, String identifier, String globalIdentifier) {
            this.name = name;
            this.identifier = identifier;
            this.globalIdentifier = globalIdentifier;
        }

        public String getName() {
            return name;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getGlobalIdentifier() {
            return globalIdentifier;
        }

        public boolean isEmpty() {
            return (name == null || name.isBlank())
                    && (identifier == null || identifier.isBlank())
                    && (globalIdentifier == null || globalIdentifier.isBlank());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PartySummary)) {
                return false;
            }
            PartySummary that = (PartySummary) o;
            return Objects.equals(name, that.name)
                    && Objects.equals(identifier, that.identifier)
                    && Objects.equals(globalIdentifier, that.globalIdentifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, identifier, globalIdentifier);
        }
    }
}
